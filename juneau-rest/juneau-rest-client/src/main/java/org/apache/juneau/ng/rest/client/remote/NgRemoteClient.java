/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ng.rest.client.remote;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.StringUtils.firstNonEmpty;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.remote.Remote;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.ng.http.HttpBody;
import org.apache.juneau.ng.http.remote.RrpcInterfaceMeta;
import org.apache.juneau.ng.rest.client.*;

/**
 * Creates Java proxy instances for {@link Remote}-annotated interfaces, backed by an {@link NgRestClient}.
 *
 * <p>
 * Each interface method call translates to an HTTP request via the client.
 *
 * <p>
 * Obtain instances via {@link NgRestClient#remote(Class)}.
 *
 * <p class='bjava'>
 * 	<ja>@Remote</ja>(path=<js>"/api/users"</js>)
 * 	<jk>public interface</jk> UserService {{
 * 		<ja>@RemoteGet</ja>(<js>"/{id}"</js>)
 * 		String getUser(<ja>@Path</ja>(<js>"id"</js>) String <jv>id</jv>);
 * 	}}
 *
 * 	UserService <jv>svc</jv> = client.remote(UserService.<jk>class</jk>);
 * 	String <jv>user</jv> = <jv>svc</jv>.getUser(<js>"42"</js>);
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class NgRemoteClient {

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: client is caller-owned, not closed by this holder
	})
	private final NgRestClient client;

	/**
	 * Constructor.
	 *
	 * @param client The underlying REST client. Must not be <jk>null</jk>.
	 */
	@SuppressWarnings({
		"resource" // NgRestClient is owned by caller; this holder must not close it
	})
	public NgRemoteClient(NgRestClient client) {
		this.client = assertArgNotNull("client", client);
	}

	/**
	 * Creates a Java proxy for the given {@link Remote}-annotated interface.
	 *
	 * @param <T> The interface type.
	 * @param iface The interface class. Must be annotated with {@link Remote}. Must not be <jk>null</jk>.
	 * @return A proxy instance. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code iface} is not an interface or not annotated with {@link Remote}.
	 */
	@SuppressWarnings("unchecked")
	public <T> T create(Class<T> iface) {
		assertArgNotNull("iface", iface);
		var meta = RrpcInterfaceMeta.of(iface);
		return (T) Proxy.newProxyInstance(
			iface.getClassLoader(),
			new Class<?>[]{iface},
			new RemoteInvocationHandler(client, meta)
		);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// InvocationHandler
	// ------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"resource" // resp is closed within this method or returned to caller (RESPONSE mode)
	})
	private static final class RemoteInvocationHandler implements InvocationHandler {

		private final NgRestClient client;
		private final RrpcInterfaceMeta meta;

		RemoteInvocationHandler(NgRestClient client, RrpcInterfaceMeta meta) {
			this.client = client;
			this.meta = meta;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Handle Object methods directly
			if (method.getDeclaringClass() == Object.class)
				return method.invoke(this, args);

			var methodMeta = meta.getMethodMeta(method);
			if (methodMeta == null)
				throw new UnsupportedOperationException("Method '" + method.getName() + "' has no @Remote* annotation");

			// Build the full path: basePath + methodPath
			var basePath = meta.getBasePath();
			var methodPath = methodMeta.getPath();
			var fullPath = combinePaths(basePath, methodPath);

			// Build the request
			var req = buildRequest(methodMeta.getHttpMethod(), fullPath, method, args);

			// Execute and process the return value
			return processReturn(req, methodMeta.getReturnType(), method.getReturnType());
		}

		private NgRestRequest buildRequest(String httpMethod, String path, Method method, Object[] args) {
			var req = switch (httpMethod) {
				case "GET" -> client.get(path);
				case "POST" -> client.post(path);
				case "PUT" -> client.put(path);
				case "PATCH" -> client.patch(path);
				case "DELETE" -> client.delete(path);
				default -> throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod); // HTT
			};

			if (args != null) {
				var params = method.getParameters();
				for (int i = 0; i < params.length; i++) {
					var param = params[i];
					var arg = args[i];
					if (arg == null)
						continue;

					var pathAnnotation = param.getAnnotation(org.apache.juneau.http.annotation.Path.class);
					if (pathAnnotation != null) {
						var name = firstNonEmpty(pathAnnotation.value(), pathAnnotation.name(), param.getName());
						req = req.pathData(name, String.valueOf(arg));
						continue;
					}

					var queryAnnotation = param.getAnnotation(org.apache.juneau.http.annotation.Query.class);
					if (queryAnnotation != null) {
						var name = firstNonEmpty(queryAnnotation.value(), queryAnnotation.name(), param.getName());
						req = req.queryData(name, String.valueOf(arg));
						continue;
					}

					var headerAnnotation = param.getAnnotation(org.apache.juneau.http.annotation.Header.class);
					if (headerAnnotation != null) {
						var name = firstNonEmpty(headerAnnotation.value(), headerAnnotation.name(), param.getName());
						req = req.header(name, String.valueOf(arg));
						continue;
					}

					var contentAnnotation = param.getAnnotation(Content.class);
					if (contentAnnotation != null) {
						if (arg instanceof HttpBody b)
							req = req.body(b);
						else
							req = req.bodyString(String.valueOf(arg));
						continue;
					}

					if (params.length == 1) {
						if (arg instanceof HttpBody b)
							req = req.body(b);
						else
							req = req.bodyString(String.valueOf(arg));
					}
				}
			}

			return req;
		}

		private Object processReturn(NgRestRequest req, RemoteReturn returnMode, Class<?> returnType) throws Exception {
			return switch (returnMode) {
			case BODY -> {
				try (var resp = req.run()) { // HTT - exception during close() branch
					if (returnType == void.class || returnType == Void.class)
						yield null;
					if (returnType == String.class)
						yield resp.getBodyAsString();
					if (returnType == InputStream.class)
						yield resp.getBodyStream();
					if (returnType == byte[].class)
						yield resp.body().asBytes();
					yield resp.getBodyAsString();
				}
			}
			case STATUS -> {
				try (var resp = req.run()) { // HTT - exception during close() branch
					var sc = resp.getStatusCode();
					if (returnType == int.class || returnType == Integer.class)
						yield sc;
					if (returnType == boolean.class || returnType == Boolean.class)
						yield sc < 400;
					yield sc;
				}
			}
			case RESPONSE -> req.run(); // caller must close
			case BEAN, NONE -> throw new UnsupportedOperationException(
				"NgRestClient remote proxies support RemoteReturn.BODY, STATUS, and RESPONSE only; got " + returnMode);
			};
		}

		private static String combinePaths(String base, String method) {
			if (base.isEmpty())
				return method.isEmpty() ? "" : method;
			if (method.isEmpty())
				return base;
			// Avoid double slashes
			if (base.endsWith("/") && method.startsWith("/"))
				return base + method.substring(1);
			if (!base.endsWith("/") && !method.startsWith("/"))
				return base + "/" + method;
			return base + method;
		}
	}
}
