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
package org.apache.juneau.ng.http.remote;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.StringUtils.isEmpty;
import static org.apache.juneau.commons.utils.StringUtils.isOneOf;
import static org.apache.juneau.commons.utils.StringUtils.trimSlashes;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.http.remote.Remote;
import org.apache.juneau.http.remote.RemoteDelete;
import org.apache.juneau.http.remote.RemoteGet;
import org.apache.juneau.http.remote.RemoteOp;
import org.apache.juneau.http.remote.RemotePatch;
import org.apache.juneau.http.remote.RemotePost;
import org.apache.juneau.http.remote.RemotePut;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.rest.common.utils.HttpUtils;

/**
 * Holds resolved metadata for an interface annotated with {@link org.apache.juneau.http.remote.Remote @Remote}.
 *
 * <p>
 * Built once per interface class and cached for efficient proxy invocation.
 * Use {@link #of(Class)} to obtain instances.
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
public final class RrpcInterfaceMeta {

	private static final Map<Class<?>, RrpcInterfaceMeta> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private final Class<?> iface;
	private final String basePath;
	private final Map<Method, RrpcInterfaceMethodMeta> methodMetas;

	private RrpcInterfaceMeta(Class<?> iface) {
		if (!iface.isInterface())
			throw illegalArg("Class {0} is not an interface", iface.getName());
		var remote = iface.getAnnotation(Remote.class);
		if (remote == null)
			throw illegalArg("Interface {0} is not annotated with @Remote", iface.getName());

		this.iface = iface;
		var path = remote.path().isEmpty() ? remote.value() : remote.path();
		this.basePath = path;

		var metas = new LinkedHashMap<Method, RrpcInterfaceMethodMeta>();
		for (var m : iface.getMethods())
			buildMethodMeta(m).ifPresent(meta -> metas.put(m, meta));
		this.methodMetas = Collections.unmodifiableMap(metas);
	}

	/**
	 * Returns the {@link RrpcInterfaceMeta} for the given interface, creating and caching it if necessary.
	 *
	 * @param iface The interface class. Must be annotated with {@link org.apache.juneau.http.remote.Remote}. Must not be <jk>null</jk>.
	 * @return The metadata. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the class is not an interface or not annotated with {@link org.apache.juneau.http.remote.Remote}.
	 */
	public static RrpcInterfaceMeta of(Class<?> iface) {
		assertArgNotNull("iface", iface);
		return CACHE.computeIfAbsent(iface, RrpcInterfaceMeta::new);
	}

	/**
	 * Returns the interface class.
	 *
	 * @return The interface. Never <jk>null</jk>.
	 */
	public Class<?> getInterface() {
		return iface;
	}

	/**
	 * Returns the base path from the {@link org.apache.juneau.http.remote.Remote#path() Remote} annotation.
	 *
	 * @return The base path. Never <jk>null</jk>, but may be empty.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Returns the method metadata for all annotated methods.
	 *
	 * @return An unmodifiable map from {@link Method} to {@link RrpcInterfaceMethodMeta}. Never <jk>null</jk>.
	 */
	public Map<Method, RrpcInterfaceMethodMeta> getMethodMetas() {
		return methodMetas;
	}

	/**
	 * Returns the metadata for the given method, or {@code null} if the method has no remote annotation.
	 *
	 * @param method The method. Must not be <jk>null</jk>.
	 * @return The metadata, or <jk>null</jk>.
	 */
	public RrpcInterfaceMethodMeta getMethodMeta(Method method) {
		return methodMetas.get(method);
	}

	private static Optional<RrpcInterfaceMethodMeta> buildMethodMeta(Method m) {
		String httpMethod = null;
		String path = "";
		var returnType = RemoteReturn.BODY;

		if (m.isAnnotationPresent(RemoteGet.class)) {
			var a = m.getAnnotation(RemoteGet.class);
			httpMethod = "GET";
			path = a.path().isEmpty() ? a.value() : a.path();
			returnType = a.returns();
		} else if (m.isAnnotationPresent(RemotePost.class)) {
			var a = m.getAnnotation(RemotePost.class);
			httpMethod = "POST";
			path = a.path().isEmpty() ? a.value() : a.path();
			returnType = a.returns();
		} else if (m.isAnnotationPresent(RemotePut.class)) {
			var a = m.getAnnotation(RemotePut.class);
			httpMethod = "PUT";
			path = a.path().isEmpty() ? a.value() : a.path();
			returnType = a.returns();
		} else if (m.isAnnotationPresent(RemotePatch.class)) {
			var a = m.getAnnotation(RemotePatch.class);
			httpMethod = "PATCH";
			path = a.path().isEmpty() ? a.value() : a.path();
			returnType = a.returns();
		} else if (m.isAnnotationPresent(RemoteDelete.class)) {
			var a = m.getAnnotation(RemoteDelete.class);
			httpMethod = "DELETE";
			path = a.path().isEmpty() ? a.value() : a.path();
			returnType = a.returns();
		} else if (m.isAnnotationPresent(RemoteOp.class)) {
			var a = m.getAnnotation(RemoteOp.class);
			String method = a.method().trim();
			path = a.path().trim();
			returnType = a.returns();
			var v = a.value().trim();
			if (!v.isEmpty()) {
				var i = v.indexOf(' ');
				if (i == -1) {
					method = v.toUpperCase();
				} else {
					method = v.substring(0, i).trim().toUpperCase();
					path = v.substring(i).trim();
				}
			}
			if (path.isEmpty())
				path = HttpUtils.detectHttpPath(m, isEmpty(method) ? null : method);
			if (method.isEmpty())
				method = HttpUtils.detectHttpMethod(m, true, "GET");
			if (!isOneOf(method, "DELETE", "GET", "POST", "PUT", "OPTIONS", "HEAD", "CONNECT", "TRACE", "PATCH"))
				throw illegalArg("Invalid @RemoteOp method ''{0}'' on {1}.{2}", method, m.getDeclaringClass().getName(), m.getName());
			httpMethod = method;
			path = trimSlashes(path);
		}

		if (httpMethod == null)
			return Optional.empty();

		return Optional.of(new RrpcInterfaceMethodMeta(m, httpMethod, path, returnType));
	}

	@Override /* Object */
	public String toString() {
		return "@Remote " + iface.getSimpleName() + " (basePath=" + basePath + ", methods=" + methodMetas.size() + ")";
	}
}
