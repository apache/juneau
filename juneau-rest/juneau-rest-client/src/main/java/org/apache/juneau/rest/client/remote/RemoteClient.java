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
package org.apache.juneau.rest.client.remote;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.client.*;

/**
 * Creates Java proxy instances for {@link Remote}-annotated interfaces, backed by an {@link RestClient}.
 *
 * <p>
 * Each interface method call translates to an HTTP request via the client.
 *
 * <p>
 * Obtain instances via {@link RestClient#remote(Class)}.
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
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class RemoteClient {

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: client is caller-owned, not closed by this holder
	})
	private final RestClient client;

	/**
	 * Constructor.
	 *
	 * @param client The underlying REST client. Must not be <jk>null</jk>.
	 */
	@SuppressWarnings({
		"resource" // RestClient is owned by caller; this holder must not close it
	})
	public RemoteClient(RestClient client) {
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
	@SuppressWarnings({
		"unchecked" // Type erasure on reflective/generic cast; element type is verified at call site
	})
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
		"resource", // resp is closed within this method or returned to caller (RESPONSE mode)
		"java:S112" // Reflective remote-proxy dispatch intentionally propagates arbitrary exceptions (target-method, parse, transport, and user-declared @Remote exception types) to the caller; narrowing the throws clauses would break that contract.
	})
	private static final class RemoteInvocationHandler implements InvocationHandler {

		private final RestClient client;
		private final RrpcInterfaceMeta meta;

		RemoteInvocationHandler(RestClient client, RrpcInterfaceMeta meta) {
			this.client = client;
			this.meta = meta;
		}

		@Override
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for remote proxy invocation dispatch
		})
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
			return processReturn(req, methodMeta.getReturnType(), method);
		}

		private RestRequest buildRequest(String httpMethod, String path, Method method, Object[] args) throws IOException {
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
				for (int i = 0; i < params.length; i++)
					bindParam(req, params[i], args[i], params.length == 1);
			}

			return req;
		}

		/**
		 * Binds a single method argument to the outgoing request based on its HTTP-part annotation.
		 *
		 * <p>
		 * Supports {@code @Path}, {@code @Query}, {@code @Header}, {@code @FormData}, {@code @Content}, and
		 * {@code @Request}.  {@code @Query}/{@code @Header}/{@code @FormData} honor dynamic name/value expansion
		 * (Map / {@code "*"} / part-list / bean) and parameter-level {@code def()} defaults for {@code null} args.
		 */
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for multi-part-type parameter binding dispatch
		})
		private void bindParam(RestRequest req, Parameter param, Object arg, boolean soleParam) throws IOException {
			var path = param.getAnnotation(Path.class);
			if (path != null) {
				if (arg != null)
					req.pathData(firstNonEmpty(path.value(), path.name(), param.getName()),
						serializePart(HttpPartType.PATH, HttpPartSchema.create(path, null), arg));
				return;
			}

			var query = param.getAnnotation(Query.class);
			if (query != null) {
				bindParts(HttpPartType.QUERY, HttpPartSchema.create(query, null), query.value(), query.name(), query.def(), arg, param.getName(), req::queryData);
				return;
			}

			var header = param.getAnnotation(Header.class);
			if (header != null) {
				bindParts(HttpPartType.HEADER, HttpPartSchema.create(header, null), header.value(), header.name(), header.def(), arg, param.getName(), req::header);
				return;
			}

			var formData = param.getAnnotation(FormData.class);
			if (formData != null) {
				bindParts(HttpPartType.FORMDATA, HttpPartSchema.create(formData, null), formData.value(), formData.name(), formData.def(), arg, param.getName(), req::formData);
				return;
			}

			var request = param.getAnnotation(Request.class);
			if (request != null) {
				if (arg != null)
					bindRequestBean(req, arg);
				return;
			}

			// G12: @PathRemainder appends the (part-serialized) value as the trailing path remainder ("/*").
			var pathRemainder = param.getAnnotation(PathRemainder.class);
			if (pathRemainder != null) {
				if (arg != null)
					req.pathData("/*", serializePart(HttpPartType.PATH, HttpPartSchema.create(pathRemainder, null), arg));
				return;
			}

			if (param.getAnnotation(Content.class) != null) {
				if (arg != null)
					withContentBody(req, arg);
				return;
			}

			if (soleParam && arg != null)
				withContentBody(req, arg);
		}

		/**
		 * Binds a {@code @Query}/{@code @Header}/{@code @FormData} argument as one or more name/value parts.
		 *
		 * <p>
		 * Single values are serialized via the configured {@link HttpPartSerializer} honoring the resolved
		 * {@link HttpPartSchema} ({@code @Schema} format / collection-format / {@code skipIfEmpty}); dynamic
		 * {@code "*"}/blank-name arguments expand into multiple parts (G8).
		 *
		 * @param partType The HTTP part category (query/header/form-data).
		 * @param schema The resolved part schema (from the annotation + {@code @Schema}).
		 * @param annValue The annotation {@code value()} (may be blank, or {@code "*"} for dynamic expansion).
		 * @param annName The annotation {@code name()} (may be blank).
		 * @param def The parameter-level default applied when {@code arg} is <jk>null</jk>.
		 * @param arg The argument value.
		 * @param fallbackName The name to use when the annotation specifies none (param or bean-property name).
		 * @param adder Sink that records a single name/value part on the request.
		 */
		@SuppressWarnings({
			"java:S107" // The 8 parameters form one cohesive HTTP-part binding descriptor (type/schema/name/default/value/sink) threaded through internal dispatch; a holder object would add indirection without improving this beta reflective-proxy code.
		})
		private static void bindParts(HttpPartType partType, HttpPartSchema schema, String annValue, String annName, String def, Object arg, String fallbackName, BiConsumer<String,String> adder) {
			var explicit = firstNonEmpty(annValue, annName);
			if (arg == null) {
				if (def != null && ! def.isEmpty())
					adder.accept("*".equals(explicit) ? fallbackName : firstNonEmpty(explicit, fallbackName), def);
				return;
			}
			if ("*".equals(explicit) || (explicit == null && isExpandable(arg))) {
				expandPairs(partType, arg, adder);
				return;
			}
			if (schema != null && schema.isSkipIfEmpty() && isEmptyArg(arg))
				return;
			var serialized = serializePart(partType, schema, arg);
			if (serialized == null)
				return;
			if (serialized.isEmpty() && schema != null && schema.isSkipIfEmpty())
				return;
			adder.accept(firstNonEmpty(explicit, fallbackName), serialized);
		}

		/** Returns <jk>true</jk> if the argument is an empty string/collection/array/map (for {@code skipIfEmpty}). */
		private static boolean isEmptyArg(Object arg) {
			if (arg == null)
				return true;
			if (arg instanceof CharSequence cs)
				return cs.isEmpty();
			if (arg instanceof Map<?,?> m)
				return m.isEmpty();
			if (arg instanceof Collection<?> c)
				return c.isEmpty();
			if (arg.getClass().isArray())
				return Array.getLength(arg) == 0;
			return false;
		}

		/**
		 * Expands a dynamic argument (Map / {@link PartList} / {@link HttpHeaderList} / bean) into discrete
		 * name/value parts.  Map/bean values are part-serialized; {@link PartList}/{@link HttpHeaderList} entries
		 * are already string-valued and passed through.
		 */
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for multi-type argument expansion dispatch
		})
		private static void expandPairs(HttpPartType partType, Object arg, BiConsumer<String,String> adder) {
			if (arg instanceof Map<?,?> m) {
				m.forEach((k, v) -> { if (k != null && v != null) adder.accept(String.valueOf(k), serializePart(partType, null, v)); });
			} else if (arg instanceof PartList pl) {
				for (var p : pl)
					if (p.getValue() != null)
						adder.accept(p.getName(), p.getValue());
			} else if (arg instanceof HttpHeaderList hl) {
				for (var h : hl)
					if (h.getValue() != null)
						adder.accept(h.getName(), h.getValue());
			} else {
				for (var e : MarshallingContext.DEFAULT.toBeanMap(arg).entrySet())
					if (e.getValue() != null)
						adder.accept(e.getKey(), serializePart(partType, null, e.getValue()));
			}
		}

		/**
		 * Serializes an HTTP-part value via the OpenAPI part serializer, honoring the supplied schema
		 * (collection format, value format, enums, etc.).  Returns the non-URL-encoded string form (G6).
		 */
		private static String serializePart(HttpPartType partType, HttpPartSchema schema, Object value) {
			try {
				return OpenApiSerializer.DEFAULT.getPartSession().serialize(partType, schema, value);
			} catch (Exception e) {
				throw rex(e, "Could not serialize HTTP {0} part value of type {1}", partType, value == null ? "null" : value.getClass().getName());
			}
		}

		/**
		 * Binds the HTTP-part-annotated getters of a {@code @Request} bean as discrete request parts.
		 */
		private static void bindRequestBean(RestRequest req, Object bean) {
			for (var m : bean.getClass().getMethods()) {
				if (m.getParameterCount() != 0 || m.getDeclaringClass() == Object.class)
					continue;
				var q = m.getAnnotation(Query.class);
				var h = m.getAnnotation(Header.class);
				var f = m.getAnnotation(FormData.class);
				var p = m.getAnnotation(Path.class);
				if (q != null || h != null || f != null || p != null) {
					Object value;
					try {
						value = m.invoke(bean);
					} catch (ReflectiveOperationException e) {
						throw rex(e, "Could not read @Request bean property via {0}", m.getName());
					}
					var prop = propertyName(m.getName());
					if (q != null)
						bindParts(HttpPartType.QUERY, HttpPartSchema.create(q, null), q.value(), q.name(), q.def(), value, prop, req::queryData);
					else if (h != null)
						bindParts(HttpPartType.HEADER, HttpPartSchema.create(h, null), h.value(), h.name(), h.def(), value, prop, req::header);
					else if (f != null)
						bindParts(HttpPartType.FORMDATA, HttpPartSchema.create(f, null), f.value(), f.name(), f.def(), value, prop, req::formData);
					else if (value != null)
						req.pathData(firstNonEmpty(p.value(), p.name(), prop), serializePart(HttpPartType.PATH, HttpPartSchema.create(p, null), value));
				}
			}
		}

		/**
		 * Returns <jk>true</jk> if a blank-named part argument should be expanded into multiple name/value parts
		 * (Map / {@link PartList} / {@link HttpHeaderList} / bean) rather than serialized as a single value.
		 */
		private static boolean isExpandable(Object arg) {
			return arg instanceof Map || arg instanceof PartList || arg instanceof HttpHeaderList || isBean(arg);
		}

		private static boolean isBean(Object arg) {
			return ! (arg instanceof CharSequence || arg instanceof Number || arg instanceof Boolean
				|| arg instanceof Character || arg instanceof Enum || arg instanceof Date
				|| arg instanceof Collection || arg.getClass().isArray());
		}

		/** Derives a bean-property name from a getter name (e.g. {@code getFoo} &rarr; {@code foo}). */
		private static String propertyName(String methodName) {
			if (methodName.startsWith("get") && methodName.length() > 3)
				return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
			if (methodName.startsWith("is") && methodName.length() > 2)
				return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
			return methodName;
		}

		private Object processReturn(RestRequest req, RemoteReturn returnMode, Method method) throws Exception {
			var returnType = method.getReturnType();
			var genericReturnType = method.getGenericReturnType();
			return switch (returnMode) {
			case BODY -> processBody(req, returnType, genericReturnType, method);
			case BEAN -> {
				// HTTP-response bean (e.g. Ok / NotFound): materialize from status + body rather than parsing.
				try (var resp = req.run()) { // HTT - exception during close() branch
					throwIfError(resp, method);
					yield instantiateHttpType(returnType, resp.getBodyAsString());
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
			case NONE -> {
				try (var resp = req.run()) { // HTT - exception during close() branch
					yield null;
				}
			}
			};
		}

		/**
		 * Handles {@link RemoteReturn#BODY} returns, including {@link Optional} and {@link CompletableFuture}/{@link Future}
		 * wrappers (G11) and HTTP-response beans (G2/G3).
		 */
		private Object processBody(RestRequest req, Class<?> returnType, Type genericReturnType, Method method) throws Exception {
			if (returnType == Optional.class) {
				var inner = innerType(genericReturnType);
				return opt(processBodyValue(req, rawClass(inner), inner, method));
			}
			if (returnType == CompletableFuture.class || returnType == Future.class) {
				var inner = innerType(genericReturnType);
				return CompletableFuture.completedFuture(processBodyValue(req, rawClass(inner), inner, method));
			}
			return processBodyValue(req, returnType, genericReturnType, method);
		}

		/** Runs the request and materializes a single (unwrapped) body value. */
		private Object processBodyValue(RestRequest req, Class<?> returnType, Type genericReturnType, Method method) throws Exception {
			try (var resp = req.run()) { // HTT - exception during close() branch
				throwIfError(resp, method);
				if (returnType == void.class || returnType == Void.class)
					return null;
				if (returnType == String.class)
					return resp.getBodyAsString();
				if (returnType == InputStream.class)
					return resp.getBodyStream();
				if (returnType == byte[].class)
					return resp.body().asBytes();
				if (BasicHttpResponse.class.isAssignableFrom(returnType) || BasicHttpException.class.isAssignableFrom(returnType))
					return instantiateHttpType(returnType, resp.getBodyAsString());
				return parseBody(resp, genericReturnType);
			}
		}

		/**
		 * Maps an error HTTP status (&ge;400) to a typed exception declared in the method's {@code throws} clause (G4).
		 *
		 * <p>
		 * A declared exception type matches when it extends {@link BasicHttpException} and its static {@code STATUS_CODE}
		 * field equals the response status code (e.g. {@code 404} &rarr; {@link org.apache.juneau.http.response.NotFound}).
		 * If no declared type matches, the response is left for normal body handling.
		 */
		private static void throwIfError(RestResponse resp, Method method) throws Exception {
			var sc = resp.getStatusCode();
			if (sc < 400)
				return;
			for (var et : method.getExceptionTypes()) {
				if (BasicHttpException.class.isAssignableFrom(et) && httpStatusCode(et) == sc)
					throw (BasicHttpException) instantiateHttpType(et, resp.getBodyAsString());
			}
		}

		/** Returns the static {@code STATUS_CODE} field of an HTTP response/exception type, or {@code -1} if absent. */
		private static int httpStatusCode(Class<?> c) {
			try {
				return c.getField("STATUS_CODE").getInt(null);
			} catch (ReflectiveOperationException e) {
				return -1;
			}
		}

		/**
		 * Instantiates an HTTP response/exception bean, preferring a {@code (String body)} constructor and falling back
		 * to the no-arg constructor (status code is intrinsic to the type).
		 */
		private static Object instantiateHttpType(Class<?> c, String body) {
			try {
				return newHttpTypeInstance(c, body);
			} catch (ReflectiveOperationException e) {
				throw rex(e, "Could not instantiate HTTP response/exception type {0}", c.getName());
			}
		}

		/** Instantiates {@code c} preferring a {@code (String body)} constructor, falling back to the no-arg constructor. */
		private static Object newHttpTypeInstance(Class<?> c, String body) throws ReflectiveOperationException {
			try {
				return c.getConstructor(String.class).newInstance(body);
			} catch (NoSuchMethodException e) {
				return c.getConstructor().newInstance();
			}
		}

		/** Returns the raw {@link Class} of a possibly-parameterized type. */
		private static Class<?> rawClass(Type t) {
			if (t instanceof Class<?> c)
				return c;
			if (t instanceof ParameterizedType p)
				return (Class<?>) p.getRawType();
			return Object.class;
		}

		/** Returns the first type argument of a parameterized wrapper (e.g. {@code Optional<T>} &rarr; {@code T}). */
		private static Type innerType(Type t) {
			if (t instanceof ParameterizedType p) {
				var args = p.getActualTypeArguments();
				if (args.length > 0)
					return args[0];
			}
			return Object.class;
		}

		private static RestRequest withContentBody(RestRequest req, Object arg) throws IOException {
			if (arg instanceof HttpBody b)
				return req.body(b);
			if (arg instanceof String s)
				return req.bodyString(s);
			if (arg instanceof Reader r)
				return req.bodyString(readReader(r));
			return req.bodyString(JsonSerializer.DEFAULT.serialize(arg));
		}

		private static Object parseBody(RestResponse resp, Type returnType) throws Exception {
			var body = resp.getBodyAsString();
			if (body == null)
				return null;
			try {
				return JsonParser.DEFAULT.parse(body, returnType);
			} catch (ParseException e) {
				if (returnType == Object.class)
					return body;
				throw e;
			}
		}

		private static String readReader(Reader reader) throws IOException {
			var sb = new StringBuilder();
			var buffer = new char[4096];
			int len;
			while ((len = reader.read(buffer)) != -1)
				sb.append(buffer, 0, len);
			return sb.toString();
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
