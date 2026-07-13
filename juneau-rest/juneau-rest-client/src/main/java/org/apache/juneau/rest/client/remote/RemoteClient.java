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
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.marshall.Constants.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;
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
@SuppressWarnings({
	"resource" // Eclipse resource analysis: client is caller-owned, not closed by this holder
})
public final class RemoteClient {

	private final RestClient client;

	/**
	 * Constructor.
	 *
	 * @param client The underlying REST client. Must not be <jk>null</jk>.
	 */
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
		"java:S112", // Reflective remote-proxy dispatch intentionally propagates arbitrary exceptions (target-method, parse, transport, and user-declared @Remote exception types) to the caller; narrowing the throws clauses would break that contract.
		"java:S2143" // java.util.Date 'instanceof' check classifies simple-typed @Request args (a type test, not date arithmetic); no behavior-preserving java.time equivalent.
	})
	private static final class RemoteInvocationHandler implements InvocationHandler {

		/**
		 * Per-class cache of resolved interceptor instances, mirroring the bean-store-less
		 * {@link RrpcInterfaceMethodMeta#resolvePartSerializer} pattern.  Located here (not on the {@code Rrpc*Meta}
		 * objects) because {@link RestCallInterceptor} lives in this module and the metadata module must not depend on
		 * it.  Resolved instances are independent of any client, so the cache is safely shared statically.
		 */
		private static final Map<Class<?>, RestCallInterceptor> INTERCEPTORS = new ConcurrentHashMap<>();
		private static final String HEADER_ACCEPT = "Accept";
		private static final String MEDIA_OCTET_STREAM = "application/octet-stream";

		/** Builds a fresh {@link RestRequest} for an invocation; re-invoked per retry attempt. */
		@FunctionalInterface
		private interface RequestSupplier {
			RestRequest get() throws IOException;
		}

		/**
		 * The resolved per-call request body format: the serializer selected for the {@code contentType}
		 * media type and the {@code Content-Type} label to send.  {@link #NONE} means no {@code contentType} attribute
		 * is in effect (use the default body serialization).
		 *
		 * @param serializer The serializer to write the body with (the matched serializer, or the default on no-match).
		 * @param contentType The {@code Content-Type} label to send (always the attribute's media type when set).
		 */
		private record BodyFormat(Serializer serializer, String contentType) {

			/** The empty body format used when no {@code contentType} attribute is in effect. */
			static final BodyFormat NONE = new BodyFormat(null, null);

			/** Returns <jk>true</jk> if a {@code contentType} attribute is in effect for this call. */
			boolean isSet() {
				return contentType != null;
			}
		}

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

			// Apply the call-time @Url parameter / declarative baseUrl override (computed per call;
			// never cached on the shared meta objects).  {var} substitution still runs at request time so @Path params
			// can fill tokens inside the resolved URL.
			var effectivePath = resolveEffectiveUrl(methodMeta, method, args, fullPath);

			// throwOnError is enabled if set true at either the method or interface level.
			var throwOnError = meta.isThrowOnError() || methodMeta.isThrowOnError();

			// The effective accept media type (method-level overrides interface-level) is the response
			// parser FALLBACK — consulted only when the response Content-Type matched no registered parser.
			var acceptFallback = firstNonEmpty(methodMeta.getAccept(), meta.getAccept());

			// Execute and process the return value.  The request is rebuilt per attempt so a (gated) retry resends a
			// freshly-bound request.
			return processReturn(() -> buildRequest(methodMeta, effectivePath, method, args), methodMeta, method, throwOnError, acceptFallback);
		}

		private RestRequest buildRequest(RrpcInterfaceMethodMeta methodMeta, String path, Method method, Object[] args) throws IOException {
			var httpMethod = methodMeta.getHttpMethod();
			var req = switch (httpMethod) {
				case "GET" -> client.get(path);
				case "POST" -> client.post(path);
				case "PUT" -> client.put(path);
				case "PATCH" -> client.patch(path);
				case "DELETE" -> client.delete(path);
				default -> throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod); // HTT
			};

			// Resolve the effective content-negotiation overrides (method-level over interface-level).
			// contentType SELECTS the request serializer (and sets the Content-Type label); accept sets the Accept
			// header.  Both win over a constant header of the same name (filtered out below), but a genuinely
			// caller-supplied @Header param still takes effect (applied after binding).
			var bodyFormat = resolveBodyFormat(methodMeta);
			var effectiveAccept = firstNonEmpty(methodMeta.getAccept(), meta.getAccept());

			// Apply always-applied constant parts (interface-level + method-level) to every request.
			// Within each part type, method-level constants take precedence over interface-level constants of the
			// same name; a caller-supplied parameter value (bound below) still composes with the constant.  A
			// constant Content-Type/Accept is dropped when the dedicated contentType/accept attribute is in effect.
			applyConstantHeaders(req, meta.getHeaders(), methodMeta.getConstantHeaders(), bodyFormat.isSet(), isNotEmpty(effectiveAccept));
			applyConstants(req::queryData, meta.getQueryData(), methodMeta.getConstantQueryData());
			applyConstants(req::formData, meta.getFormData(), methodMeta.getConstantFormData());

			if (args != null) {
				var params = method.getParameters();
				for (int i = 0; i < params.length; i++)
					bindParam(req, methodMeta, params[i], args[i], params.length == 1, bodyFormat);
			}

			// A @Multipart method assembles its body from the @Part parameters (a thin adapter onto the
			// existing streaming MultipartBody); otherwise apply the param-less constant body.
			if (methodMeta.isMultipart()) {
				bindMultipartBody(req, method, args);
			} else if (! hasBodyParam(method)) {
				// Param-less constant body — method-level @Content(def) with no body parameter at all.
				var contentDefault = methodMeta.getContentDefault();
				if (contentDefault != null)
					withContentBody(req, contentDefault, bodyFormat);
			}

			// Set the Accept header from the dedicated attribute unless a caller-supplied @Header
			// param already provided one (caller wins per existing semantics); a single Accept header is emitted.
			if (isNotEmpty(effectiveAccept) && ! req.hasHeader(HEADER_ACCEPT))
				req.header(HEADER_ACCEPT, effectiveAccept);

			// Apply annotation-declared interceptors + per-call timeout.
			applyPolicy(req, methodMeta);

			return req;
		}

		/**
		 * Resolves the effective request body format from the {@code contentType} attribute
		 * (method-level overriding interface-level).
		 *
		 * <p>
		 * When a {@code contentType} media type is in effect, the matching registered request serializer is selected
		 * from the client's serializer set ({@link RestClient#getSerializerForMediaType(String)}).  If no registered
		 * serializer matches, the client's explicitly-configured default serializer is used to write the body but the
		 * overridden media type is still sent as the {@code Content-Type} label (supporting vendor media types such as
		 * {@code application/vnd.foo+json} whose bytes are really the default format).  If neither a match nor a default
		 * serializer is available, an {@link IllegalStateException} is raised (there is no implicit JSON fallback).
		 *
		 * @param methodMeta The method metadata.
		 * @return The resolved body format; {@link BodyFormat#NONE} when no {@code contentType} attribute is in effect.
		 * @throws IllegalStateException If no serializer matches and no default serializer is configured on the client.
		 */
		private BodyFormat resolveBodyFormat(RrpcInterfaceMethodMeta methodMeta) {
			var contentType = firstNonEmpty(methodMeta.getContentType(), meta.getContentType());
			if (isEmpty(contentType))
				return BodyFormat.NONE;
			var serializer = client.getSerializerForMediaType(contentType).or(client::getDefaultSerializer).orElseThrow(() ->
				new IllegalStateException("No serializer matched Content-Type '" + contentType + "' and no default serializer is configured on the client.  Configure one via RestClient.Builder.defaultSerializer(...)."));
			return new BodyFormat(serializer, contentType);
		}

		/**
		 * Computes the effective request URL for a call, applying the dynamic-URL overrides over the
		 * statically-computed {@code basePath + methodPath}.
		 *
		 * <p>
		 * Precedence (most-specific first):
		 * <ol>
		 * 	<li><b>{@code @Url} parameter</b> (call-time, Option A) — its value becomes the effective URL, replacing the
		 * 		whole computed path.  A value containing a scheme is absolute (and bypasses the client root URL via the
		 * 		existing {@code ://} seam in {@link RestClient#request(String, String)}); a scheme-less value is relative
		 * 		and resolved against the client root URL only (Retrofit endpoint replacement — <i>not</i> against the
		 * 		interface path).  {@code {var}} tokens are preserved for {@code @Path} substitution at request time.
		 * 	<li><b>Method-level {@code baseUrl}</b> (Option B) — substitutes the authority+root, preserving the full path.
		 * 	<li><b>Interface-level {@code baseUrl}</b> (Option B) — same, at lower precedence.
		 * 	<li><b>Client root URL</b> (default, unchanged behavior — no override applied).
		 * </ol>
		 *
		 * <p>
		 * SSRF guardrail (v1): when an override is in effect and yields a value carrying a URI scheme, the scheme must be
		 * {@code http} or {@code https}; any other scheme is rejected.  The default (no-override) path is left untouched
		 * so pre-existing absolute-{@code @RemoteOp(path)} / templating behavior does not regress.
		 *
		 * @param methodMeta The method metadata (carries the {@code @Url} param index + the method-level {@code baseUrl}).
		 * @param method The invoked method (for error messages).
		 * @param args The invocation arguments (source of the {@code @Url} value).
		 * @param fullPath The statically-computed {@code basePath + methodPath}.
		 * @return The effective URL/path to hand to {@code client.<verb>(...)}.
		 */
		private String resolveEffectiveUrl(RrpcInterfaceMethodMeta methodMeta, Method method, Object[] args, String fullPath) {
			// Option A: a call-time @Url parameter wins over everything.
			var urlParamIndex = methodMeta.getUrlParamIndex();
			if (urlParamIndex >= 0) {
				var arg = (args == null || urlParamIndex >= args.length) ? null : args[urlParamIndex];
				var value = arg == null ? null : arg.toString();
				assertArg(! isBlank(value), "@Url parameter on {0}.{1} must not be null or blank",
					method.getDeclaringClass().getName(), method.getName());
				return requireHttpScheme(value.trim());
			}
			// Option B: a declarative base/host override (method-level wins over interface-level).
			var baseUrl = firstNonEmpty(methodMeta.getBaseUrl(), meta.getBaseUrl());
			if (isNotEmpty(baseUrl))
				return requireHttpScheme(combinePaths(baseUrl, fullPath));
			// Default: unchanged behavior (no override, no scheme restriction — no regression).
			return fullPath;
		}

		/**
		 * Enforces the SSRF guardrail: when {@code url} carries a URI scheme it must be {@code http} or
		 * {@code https}; otherwise an {@link IllegalArgumentException} is thrown.  Scheme-less (relative) values pass
		 * through unchanged.
		 */
		private static String requireHttpScheme(String url) {
			var scheme = schemeOf(url);
			assertArg(scheme == null || scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"),
				"Unsupported URL scheme ''{0}'' in @Remote URL override; only http/https are allowed: {1}", scheme, url);
			return url;
		}

		/**
		 * Returns the URI scheme of a URL (the token before the first {@code :} when it precedes any {@code /}, {@code ?}
		 * or {@code #}), or <jk>null</jk> if the value has no scheme (e.g. a relative path or a value beginning with a
		 * {@code {var}} token).
		 */
		private static String schemeOf(String url) {
			if (url.isEmpty() || ! Character.isLetter(url.charAt(0)))
				return null;
			for (var i = 0; i < url.length(); i++) {
				var c = url.charAt(i);
				if (c == ':')
					return url.substring(0, i);
				if (c == '/' || c == '?' || c == '#')
					return null;
				if (! (Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.'))
					return null;
			}
			return null;
		}

		/**
		 * Applies the cross-cutting call policy to a freshly-built request: annotation-declared
		 * interceptors (interface-level then method-level, unioned <i>after</i> the builder-level interceptors in
		 * {@link RestRequest#run()}) and the per-call response timeout (method-level overrides interface-level).
		 */
		private void applyPolicy(RestRequest req, RrpcInterfaceMethodMeta methodMeta) {
			var interceptorClasses = new ArrayList<Class<?>>();
			interceptorClasses.addAll(Arrays.asList(meta.getInterceptorClasses()));
			interceptorClasses.addAll(Arrays.asList(methodMeta.getInterceptorClasses()));
			if (! interceptorClasses.isEmpty()) {
				var resolved = new RestCallInterceptor[interceptorClasses.size()];
				for (var i = 0; i < resolved.length; i++)
					resolved[i] = resolveInterceptor(interceptorClasses.get(i));
				req.interceptors(resolved);
			}
			var timeout = firstNonEmpty(methodMeta.getTimeout(), meta.getTimeout());
			if (timeout != null && ! timeout.isEmpty()) {
				var ms = getDuration(timeout);
				if (ms > 0)
					req.timeout(Duration.ofMillis(ms));
			}
		}

		/**
		 * Resolves an interceptor class to a cached instance via {@link BeanInstantiator} (bean-store-less, mirroring
		 * the per-part-serializer resolver).  The class must implement {@link RestCallInterceptor} and provide a
		 * public no-arg constructor.
		 */
		private static RestCallInterceptor resolveInterceptor(Class<?> c) {
			return INTERCEPTORS.computeIfAbsent(c, k -> (RestCallInterceptor) BeanInstantiator.of(k).run());
		}

		/**
		 * Returns <jk>true</jk> if the method has a parameter that produces the request body (a {@code @Content}
		 * parameter, or a single unannotated parameter treated as the body), in which case a method-level
		 * {@code @Content(def)} constant body must not be applied independently.
		 */
		private static boolean hasBodyParam(Method method) {
			var params = method.getParameters();
			for (var p : params)
				if (p.getAnnotation(Content.class) != null)
					return true;
			return params.length == 1 && ! hasPartAnnotation(params[0]);
		}

		/** Returns <jk>true</jk> if the parameter carries a recognized HTTP-part annotation (non-body). */
		private static boolean hasPartAnnotation(Parameter param) {
			return param.getAnnotation(Path.class) != null
				|| param.getAnnotation(Query.class) != null
				|| param.getAnnotation(Header.class) != null
				|| param.getAnnotation(FormData.class) != null
				|| param.getAnnotation(Request.class) != null
				|| param.getAnnotation(PathRemainder.class) != null
				|| param.getAnnotation(Url.class) != null
				|| param.getAnnotation(Part.class) != null;
		}

		/**
		 * Applies always-applied constant parts (header/query/form-data) to the request, layering method-level
		 * constants on top of interface-level constants.
		 *
		 * <p>
		 * Both scopes are merged into a single name-keyed map (preserving declaration order) so that, when the same
		 * name is declared at both scopes, the method-level value wins (most-specific).  The merged constants are
		 * emitted before parameter binding so that a caller-supplied parameter value of the same name composes with
		 * (rather than erases) the constant.
		 *
		 * @param adder Sink that records a single name/value part on the request.
		 * @param interfaceLevel Interface-level constants (applied first; lower precedence).
		 * @param methodLevel Method-level constants (override interface-level entries of the same name).
		 */
		private static void applyConstants(BiConsumer<String,String> adder, List<Map.Entry<String,String>> interfaceLevel, List<Map.Entry<String,String>> methodLevel) {
			if (interfaceLevel.isEmpty() && methodLevel.isEmpty())
				return;
			var merged = new LinkedHashMap<String,String>();
			for (var e : interfaceLevel)
				merged.put(e.getKey(), e.getValue());
			for (var e : methodLevel)
				merged.put(e.getKey(), e.getValue());
			merged.forEach(adder);
		}

		/**
		 * Applies the always-applied constant <i>headers</i> (interface-level then method-level), with the
		 * precedence rule that a dedicated {@code contentType}/{@code accept} attribute wins over a constant
		 * {@code Content-Type}/{@code Accept} header of the same name.
		 *
		 * <p>
		 * The two scopes are merged (method-level wins on name collisions) and emitted in declaration order, except a
		 * constant {@code Content-Type} is dropped when {@code skipContentType} is set (the dedicated {@code contentType}
		 * attribute supplies it via the serialized body) and a constant {@code Accept} is dropped when {@code skipAccept}
		 * is set (the dedicated {@code accept} attribute supplies it after binding).  Dropping rather than emitting the
		 * constant guarantees a single header with the attribute's value.
		 */
		private void applyConstantHeaders(RestRequest req, List<Map.Entry<String,String>> interfaceLevel, List<Map.Entry<String,String>> methodLevel, boolean skipContentType, boolean skipAccept) {
			if (interfaceLevel.isEmpty() && methodLevel.isEmpty())
				return;
			var merged = new LinkedHashMap<String,String>();
			for (var e : interfaceLevel)
				merged.put(e.getKey(), e.getValue());
			for (var e : methodLevel)
				merged.put(e.getKey(), e.getValue());
			merged.forEach((k, v) -> {
				if (skipContentType && "Content-Type".equalsIgnoreCase(k))
					return;
				if (skipAccept && HEADER_ACCEPT.equalsIgnoreCase(k))
					return;
				req.header(k, v);
			});
		}

		/**
		 * Resolves the effective custom {@link HttpPartSerializer} for a parameter.
		 *
		 * <p>
		 * Precedence (most-specific first): parameter-level {@code @HttpPartMarshalling(serializer=...)} on the
		 * parameter itself, then the method-level default ({@link RrpcInterfaceMethodMeta#getPartSerializer()}), then
		 * the interface-level default ({@link RrpcInterfaceMeta#getPartSerializer()}).  Returns <jk>null</jk> when no
		 * {@code @HttpPartMarshalling} serializer is in effect, in which case the default OpenAPI part serializer is
		 * used (no-regression behavior).
		 *
		 * @param methodMeta The method metadata (method/interface-level defaults).
		 * @param param The method parameter (parameter-level annotation).
		 * @return The resolved part serializer, or <jk>null</jk> to use the default.
		 */
		private HttpPartSerializer partSerializer(RrpcInterfaceMethodMeta methodMeta, Parameter param) {
			var pm = param.getAnnotation(HttpPartMarshalling.class);
			var s = pm == null ? null : RrpcInterfaceMethodMeta.resolvePartSerializer(pm.serializer());
			if (s == null)
				s = methodMeta.getPartSerializer();
			if (s == null)
				s = meta.getPartSerializer();
			return s;
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
		private void bindParam(RestRequest req, RrpcInterfaceMethodMeta methodMeta, Parameter param, Object arg, boolean soleParam, BodyFormat bodyFormat) throws IOException {
			// @Url parameters supply the request URL (handled in resolveEffectiveUrl), not a part/body.
			if (param.getAnnotation(Url.class) != null)
				return;

			// @Part parameters are assembled into the multipart body (see bindMultipartBody), not bound here.
			if (param.getAnnotation(Part.class) != null)
				return;

			// Resolve the effective part serializer (param-level › method-level › interface-level),
			// falling back to the default OpenAPI part serializer when no @HttpPartMarshalling is in effect.
			var serializer = partSerializer(methodMeta, param);

			var path = param.getAnnotation(Path.class);
			if (path != null) {
				var name = firstNonEmpty(path.value(), path.name(), param.getName());
				// Parameter-level def (non-_NONE_) wins, else method-level @Path(def=...).
				var pathDef = NONE.equals(path.def()) ? null : path.def();
				var value = arg != null ? arg : firstNonEmpty(pathDef, methodMeta.getPathDefault(name));
				if (value != null)
					req.pathData(name, serializePart(HttpPartType.PATH, HttpPartSchema.create(path, null), value, serializer));
				return;
			}

			var query = param.getAnnotation(Query.class);
			if (query != null) {
				var def = firstNonEmpty(query.def(), methodMeta.getQueryDefault(firstNonEmpty(query.value(), query.name(), param.getName())));
				bindParts(HttpPartType.QUERY, HttpPartSchema.create(query, null), query.value(), query.name(), def, arg, param.getName(), serializer, req::queryData);
				return;
			}

			var header = param.getAnnotation(Header.class);
			if (header != null) {
				var def = firstNonEmpty(header.def(), methodMeta.getHeaderDefault(firstNonEmpty(header.value(), header.name(), param.getName())));
				bindParts(HttpPartType.HEADER, HttpPartSchema.create(header, null), header.value(), header.name(), def, arg, param.getName(), serializer, req::header);
				return;
			}

			var formData = param.getAnnotation(FormData.class);
			if (formData != null) {
				var def = firstNonEmpty(formData.def(), methodMeta.getFormDataDefault(firstNonEmpty(formData.value(), formData.name(), param.getName())));
				bindParts(HttpPartType.FORMDATA, HttpPartSchema.create(formData, null), formData.value(), formData.name(), def, arg, param.getName(), serializer, req::formData);
				return;
			}

			var request = param.getAnnotation(Request.class);
			if (request != null) {
				if (arg != null)
					bindRequestBean(req, arg);
				return;
			}

			// @PathRemainder appends the (part-serialized) value as the trailing path remainder ("/*").
			var pathRemainder = param.getAnnotation(PathRemainder.class);
			if (pathRemainder != null) {
				if (arg != null)
					req.pathData("/*", serializePart(HttpPartType.PATH, HttpPartSchema.create(pathRemainder, null), arg, serializer));
				return;
			}

			if (param.getAnnotation(Content.class) != null) {
				// A null body arg falls back to parameter-level @Content(def), then method-level @Content(def).
				var content = param.getAnnotation(Content.class);
				Object body = arg != null ? arg : firstNonEmpty(content.def(), methodMeta.getContentDefault());
				if (body != null)
					withContentBody(req, body, bodyFormat);
				return;
			}

			if (soleParam && arg != null)
				withContentBody(req, arg, bodyFormat);
		}

		/**
		 * Binds a {@code @Query}/{@code @Header}/{@code @FormData} argument as one or more name/value parts.
		 *
		 * <p>
		 * Single values are serialized via the configured {@link HttpPartSerializer} honoring the resolved
		 * {@link HttpPartSchema} ({@code @Schema} format / collection-format / {@code skipIfEmpty}); dynamic
		 * {@code "*"}/blank-name arguments expand into multiple parts.
		 *
		 * @param partType The HTTP part category (query/header/form-data).
		 * @param schema The resolved part schema (from the annotation + {@code @Schema}).
		 * @param annValue The annotation {@code value()} (may be blank, or {@code "*"} for dynamic expansion).
		 * @param annName The annotation {@code name()} (may be blank).
		 * @param def The parameter-level default applied when {@code arg} is <jk>null</jk>.
		 * @param arg The argument value.
		 * @param fallbackName The name to use when the annotation specifies none (param or bean-property name).
		 * @param serializer The custom {@link HttpPartSerializer} to use, or <jk>null</jk> for the default.
		 * @param adder Sink that records a single name/value part on the request.
		 */
		@SuppressWarnings({
			"java:S107" // The 9 parameters form one cohesive HTTP-part binding descriptor (type/schema/name/default/value/serializer/sink) threaded through internal dispatch; a holder object would add indirection without improving this beta reflective-proxy code.
		})
		private static void bindParts(HttpPartType partType, HttpPartSchema schema, String annValue, String annName, String def, Object arg, String fallbackName, HttpPartSerializer serializer, BiConsumer<String,String> adder) {
			var explicit = firstNonEmpty(annValue, annName);
			if (arg == null) {
				if (def != null && ! def.isEmpty())
					adder.accept("*".equals(explicit) ? fallbackName : firstNonEmpty(explicit, fallbackName), def);
				return;
			}
			if ("*".equals(explicit) || (explicit == null && isExpandable(arg))) {
				expandPairs(partType, arg, serializer, adder);
				return;
			}
			if (schema != null && schema.isSkipIfEmpty() && isEmptyArg(arg))
				return;
			var serialized = serializePart(partType, schema, arg, serializer);
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
		private static void expandPairs(HttpPartType partType, Object arg, HttpPartSerializer serializer, BiConsumer<String,String> adder) {
			if (arg instanceof Map<?,?> m) {
				m.forEach((k, v) -> { if (k != null && v != null) adder.accept(String.valueOf(k), serializePart(partType, null, v, serializer)); });
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
						adder.accept(e.getKey(), serializePart(partType, null, e.getValue(), serializer));
			}
		}

		/**
		 * Serializes an HTTP-part value, honoring the supplied schema (collection format, value format, enums, etc.).
		 * Returns the non-URL-encoded string form.
		 *
		 * <p>
		 * When {@code serializer} is non-<jk>null</jk> (a custom {@code @HttpPartMarshalling} serializer), it is
		 * used; otherwise the default OpenAPI part serializer is used (no-regression behavior).
		 */
		private static String serializePart(HttpPartType partType, HttpPartSchema schema, Object value, HttpPartSerializer serializer) {
			try {
				var session = serializer != null ? serializer.getPartSession() : OpenApiSerializer.DEFAULT.getPartSession();
				return session.serialize(partType, schema, value);
			} catch (Exception e) {
				throw rex(e, "Could not serialize HTTP {0} part value of type {1}", partType, value == null ? "null" : cn(value));
			}
		}

		/**
		 * Binds the HTTP-part-annotated getters of a {@code @Request} bean as discrete request parts.
		 */
		private static void bindRequestBean(RestRequest req, Object bean) {
			for (var m : bean.getClass().getMethods()) {
				if (m.getParameterCount() != 0 || m.getDeclaringClass() == Object.class)
					continue;
				bindRequestBeanMethod(req, bean, m);
			}
		}

		/**
		 * Binds a single HTTP-part-annotated getter of a {@code @Request} bean as a discrete request part.
		 */
		private static void bindRequestBeanMethod(RestRequest req, Object bean, Method m) {
			var q = m.getAnnotation(Query.class);
			var h = m.getAnnotation(Header.class);
			var f = m.getAnnotation(FormData.class);
			var p = m.getAnnotation(Path.class);
			if (q == null && h == null && f == null && p == null)
				return;
			Object value;
			try {
				value = m.invoke(bean);
			} catch (ReflectiveOperationException e) {
				throw rex(e, "Could not read @Request bean property via {0}", m.getName());
			}
			var prop = propertyName(m.getName());
			// @Request bean part serialization keeps the default OpenAPI serializer (custom per-part serializers are
			// scoped to direct method parameters; passing null preserves byte-for-byte behavior).
			if (q != null)
				bindParts(HttpPartType.QUERY, HttpPartSchema.create(q, null), q.value(), q.name(), q.def(), value, prop, null, req::queryData);
			else if (h != null)
				bindParts(HttpPartType.HEADER, HttpPartSchema.create(h, null), h.value(), h.name(), h.def(), value, prop, null, req::header);
			else if (f != null)
				bindParts(HttpPartType.FORMDATA, HttpPartSchema.create(f, null), f.value(), f.name(), f.def(), value, prop, null, req::formData);
			else if (value != null)
				req.pathData(firstNonEmpty(p.value(), p.name(), prop), serializePart(HttpPartType.PATH, HttpPartSchema.create(p, null), value, null));
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

		/**
		 * Executes the request and processes the return value, applying safe automatic retries when
		 * the method opts in and all safety gates pass.
		 *
		 * <p>
		 * <b>Retry policy.</b> When a positive retry count is in effect and the return mode + verb + body permit it,
		 * each attempt rebuilds the request via {@code reqSupplier} (so a fresh, re-bound request is resent) and runs
		 * it; a connection failure ({@link TransportException}) or a retryable status ({@code 429}/{@code 5xx}) triggers
		 * another attempt (after a short exponential backoff) until the cap is reached.  Safety gates (all hard):
		 * <ul>
		 * 	<li>Only idempotent verbs (GET/PUT/DELETE/HEAD) auto-retry; POST/PATCH require {@code retryNonIdempotent}.
		 * 	<li>A non-repeatable body ({@link RestRequest#isBodyRepeatable()} == <jk>false</jk>) is never retried.
		 * 	<li>Streaming return modes ({@link RemoteReturn#RESPONSE}, raw {@link InputStream}/{@link Reader}, cursors)
		 * 		and wrapper returns ({@link Optional}/{@link Future}/{@link CompletableFuture}) are never retried.
		 * </ul>
		 */
		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for the gated retry loop (verb/body/status safety gates + backoff).
		})
		private Object processReturn(RequestSupplier reqSupplier, RrpcInterfaceMethodMeta methodMeta, Method method, boolean throwOnError, String acceptFallback) throws Exception {
			var returnMode = methodMeta.getReturnType();
			var returnType = method.getReturnType();
			var genericReturnType = method.getGenericReturnType();

			var maxRetries = (isRetryableMode(returnMode, returnType) && isRetryableVerb(methodMeta)) ? effectiveRetries(methodMeta) : 0;
			if (maxRetries == 0)
				return processReturnOnce(reqSupplier.get(), returnMode, method, throwOnError, acceptFallback);

			var attempt = 0;
			while (true) {
				var req = reqSupplier.get();
				// Non-repeatable body: refuse to retry (resending a consumed stream would corrupt the call).
				if (attempt == 0 && ! req.isBodyRepeatable())
					return processReturnOnce(req, returnMode, method, throwOnError, acceptFallback);
				var shouldRetry = false;
				RestResponse resp = null;
				try {
					resp = req.run();
				} catch (TransportException e) {
					if (attempt++ >= maxRetries)
						throw e;
					backoff(attempt);
					shouldRetry = true;
				}
				if (! shouldRetry && resp != null && attempt < maxRetries && isRetryableStatus(resp.getStatusCode())) {
					resp.close();
					attempt++;
					backoff(attempt);
					shouldRetry = true;
				}
				if (shouldRetry)
					continue;
				try (var r = resp) { // HTT - exception during close() branch
					return materializeResponse(r, returnMode, returnType, genericReturnType, method, throwOnError, acceptFallback);
				}
			}
		}

		/** Executes a single (non-retried) attempt, including streaming / cursor / future / RESPONSE return modes. */
		private Object processReturnOnce(RestRequest req, RemoteReturn returnMode, Method method, boolean throwOnError, String acceptFallback) throws Exception {
			var returnType = method.getReturnType();
			var genericReturnType = method.getGenericReturnType();
			return switch (returnMode) {
			case BODY -> processBody(req, returnType, genericReturnType, method, throwOnError, acceptFallback);
			case RESPONSE -> req.run(); // caller must close
			default -> {
				try (var resp = req.run()) { // HTT - exception during close() branch
					yield materializeResponse(resp, returnMode, returnType, genericReturnType, method, throwOnError, acceptFallback);
				}
			}
			};
		}

		/** Materializes the terminal value for the buffered return modes (BODY/BEAN/STATUS/NONE) from a run response. */
		private Object materializeResponse(RestResponse resp, RemoteReturn returnMode, Class<?> returnType, Type genericReturnType, Method method, boolean throwOnError, String acceptFallback) throws Exception {
			return switch (returnMode) {
			case BODY -> materializeBufferedBody(resp, returnType, genericReturnType, method, throwOnError, acceptFallback);
			case BEAN -> {
				// HTTP-response bean (e.g. Ok / NotFound): materialize from status + body rather than parsing.
				throwIfError(resp, method, throwOnError);
				yield instantiateHttpType(returnType, resp.getBodyAsString());
			}
			case STATUS -> {
				var sc = resp.getStatusCode();
				if (returnType == int.class || returnType == Integer.class)
					yield sc;
				if (returnType == boolean.class || returnType == Boolean.class)
					yield sc < 400;
				yield sc;
			}
			case NONE -> null;
			case RESPONSE -> throw new IllegalStateException("RESPONSE mode is not a buffered return mode"); // HTT
			};
		}

		/** Returns the effective retry count: a positive method-level value overrides the interface-level default. */
		private int effectiveRetries(RrpcInterfaceMethodMeta methodMeta) {
			return methodMeta.getRetries() > 0 ? methodMeta.getRetries() : meta.getRetries();
		}

		/** Returns <jk>true</jk> if the verb may auto-retry: idempotent verbs always, POST/PATCH only when opted in. */
		private boolean isRetryableVerb(RrpcInterfaceMethodMeta methodMeta) {
			if (isOneOf(methodMeta.getHttpMethod(), "GET", "PUT", "DELETE", "HEAD"))
				return true;
			return methodMeta.isRetryNonIdempotent() || meta.isRetryNonIdempotent();
		}

		/** Returns <jk>true</jk> if the return mode/type is a buffered (retryable) shape, not a streaming/wrapper one. */
		private static boolean isRetryableMode(RemoteReturn returnMode, Class<?> returnType) {
			if (returnMode == RemoteReturn.RESPONSE)
				return false;
			if (returnType == InputStream.class || returnType == Reader.class || returnType == Optional.class
					|| returnType == CompletableFuture.class || returnType == Future.class)
				return false;
			return ! RecordReader.class.isAssignableFrom(returnType);
		}

		/** Retryable HTTP statuses: {@code 429} (Too Many Requests) and all {@code 5xx} server errors. */
		private static boolean isRetryableStatus(int statusCode) {
			return statusCode == 429 || statusCode >= 500;
		}

		/** Sleeps a short exponential backoff ({@code 50ms * 2^(attempt-1)}, capped at {@code 1s}) before a retry. */
		private static void backoff(int attempt) {
			try {
				Thread.sleep(Math.min(50L << (attempt - 1), 1000L));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * Handles {@link RemoteReturn#BODY} returns, including {@link Optional} and {@link CompletableFuture}/{@link Future}
		 * wrappers and HTTP-response beans.
		 */
		private Object processBody(RestRequest req, Class<?> returnType, Type genericReturnType, Method method, boolean throwOnError, String acceptFallback) throws Exception {
			if (returnType == Optional.class) {
				var inner = innerType(genericReturnType);
				return o(processBodyValue(req, rawClass(inner), inner, method, throwOnError, acceptFallback));
			}
			if (returnType == CompletableFuture.class || returnType == Future.class) {
				var inner = innerType(genericReturnType);
				return CompletableFuture.completedFuture(processBodyValue(req, rawClass(inner), inner, method, throwOnError, acceptFallback));
			}
			return processBodyValue(req, returnType, genericReturnType, method, throwOnError, acceptFallback);
		}

		/** Runs the request and materializes a single (unwrapped) body value. */
		private Object processBodyValue(RestRequest req, Class<?> returnType, Type genericReturnType, Method method, boolean throwOnError, String acceptFallback) throws Exception {
			// Cursor return types stream over the live response body; the caller owns the cursor (and the response it holds).
			if (RecordReader.class.isAssignableFrom(returnType))
				return processCursor(req, returnType, method, throwOnError);
			// A raw-stream return hands the caller the LIVE response stream — the response must NOT be
			// closed on success here (the old code returned it from inside try-with-resources, closing it before the
			// caller could read).  The caller owns the returned stream; closing it releases the connection.
			if (returnType == InputStream.class)
				return processStreamReturn(req, method, throwOnError);
			// A Reader return is a lazy reader over the same live response stream (same lifecycle).
			if (returnType == Reader.class)
				return processReaderReturn(req, method, throwOnError);
			try (var resp = req.run()) { // HTT - exception during close() branch
				return materializeBufferedBody(resp, returnType, genericReturnType, method, throwOnError, acceptFallback);
			}
		}

		/** Materializes a buffered (non-streaming) body value from an already-run response. */
		private Object materializeBufferedBody(RestResponse resp, Class<?> returnType, Type genericReturnType, Method method, boolean throwOnError, String acceptFallback) throws Exception {
			throwIfError(resp, method, throwOnError);
			if (returnType == void.class || returnType == Void.class)
				return null;
			if (returnType == String.class)
				return resp.getBodyAsString();
			if (returnType == byte[].class)
				return resp.body().asBytes();
			if (BasicHttpResponse.class.isAssignableFrom(returnType) || BasicHttpException.class.isAssignableFrom(returnType))
				return instantiateHttpType(returnType, resp.getBodyAsString());
			return parseBody(resp, genericReturnType, acceptFallback);
		}

		/**
		 * Runs the request and hands back the live response body as an {@link InputStream} the caller owns.
		 *
		 * <p>
		 * Mirrors the cursor lifecycle ({@link #processCursor}): on success the response is <b>not</b> closed &mdash; the
		 * returned stream wraps the live response body and closing it releases the connection (closes the body stream and
		 * the response, firing the transport close callback).  On any failure before the stream is handed back, the
		 * response is closed.  A body-less response ({@code null} stream) is released immediately and returns <jk>null</jk>.
		 */
		@SuppressWarnings({
			"resource" // On success the live response is owned by the returned stream (caller closes it); on failure it is closed in the finally block.
		})
		private InputStream processStreamReturn(RestRequest req, Method method, boolean throwOnError) throws Exception {
			var resp = req.run();
			var ok = false;
			try {
				throwIfError(resp, method, throwOnError);
				var stream = resp.getBodyStream();
				if (stream == null) {
					ok = true;
					resp.close();
					return null;
				}
				var result = new ResponseClosingInputStream(stream, resp);
				ok = true;
				return result;
			} finally {
				if (! ok)
					resp.close();
			}
		}

		/**
		 * Runs the request and hands back the live response body as a {@link Reader} the caller owns.
		 *
		 * <p>
		 * Same lifecycle as {@link #processStreamReturn}: the response is not closed on success; the returned reader wraps
		 * the live response stream (decoded as UTF-8) and closing it releases the connection.
		 */
		@SuppressWarnings({
			"resource" // On success the live response is owned by the returned reader (caller closes it); on failure it is closed in the finally block.
		})
		private Reader processReaderReturn(RestRequest req, Method method, boolean throwOnError) throws Exception {
			var resp = req.run();
			var ok = false;
			try {
				throwIfError(resp, method, throwOnError);
				var stream = resp.getBodyStream();
				if (stream == null) {
					ok = true;
					resp.close();
					return null;
				}
				var result = new ResponseClosingReader(new InputStreamReader(stream, StandardCharsets.UTF_8), resp);
				ok = true;
				return result;
			} finally {
				if (! ok)
					resp.close();
			}
		}

		/**
		 * A response-body {@link InputStream} that releases the owning {@link RestResponse} when closed.
		 *
		 * <p>
		 * Closing this stream closes the underlying body stream (returning the connection to the pool) and then closes the
		 * {@link RestResponse} (firing the transport close callback), so the connection is released regardless of whether
		 * the transport relies on the body stream or the close callback.
		 */
		private static final class ResponseClosingInputStream extends FilterInputStream {
			private final RestResponse response;

			ResponseClosingInputStream(InputStream in, RestResponse response) {
				super(in);
				this.response = response;
			}

			@Override /* Closeable */
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					response.close();
				}
			}

			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				if (len == 0) return 0;
				int b = read();
				if (b == -1) return -1;
				buf[off] = (byte) b;
				return 1;
			}
		}

		/** A response-body {@link Reader} that releases the owning {@link RestResponse} when closed. */
		private static final class ResponseClosingReader extends FilterReader {
			private final RestResponse response;

			ResponseClosingReader(Reader in, RestResponse response) {
				super(in);
				this.response = response;
			}

			@Override /* Closeable */
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					response.close();
				}
			}
		}

		/**
		 * Maps an error HTTP status (&ge;400) to a typed exception declared in the method's {@code throws} clause.
		 *
		 * <p>
		 * A declared exception type matches when it extends {@link BasicHttpException} and its static {@code STATUS_CODE}
		 * field equals the response status code (e.g. {@code 404} &rarr; {@link org.apache.juneau.http.response.NotFound}).
		 * If no declared type matches, the response is left for normal body handling.
		 */
		private static void throwIfError(RestResponse resp, Method method, boolean throwOnError) throws Exception {
			var sc = resp.getStatusCode();
			if (sc < 400)
				return;
			for (var et : method.getExceptionTypes()) {
				if (BasicHttpException.class.isAssignableFrom(et) && httpStatusCode(et) == sc)
					throw (BasicHttpException) instantiateHttpType(et, resp.getBodyAsString());
			}
			// No declared exception type matched — if throwOnError is enabled, raise a generic
			// HTTP exception carrying the status code and response body rather than returning an error payload.
			if (throwOnError)
				throw new BasicHttpException(sc, resp.getBodyAsString());
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

		private static RestRequest withContentBody(RestRequest req, Object arg, BodyFormat bodyFormat) throws IOException {
			if (arg instanceof HttpBody b)
				return req.body(b);
			if (arg instanceof String s)
				return req.bodyString(s);
			// Stream the Reader straight to the wire instead of draining it into a String first.
			if (arg instanceof Reader r)
				return req.body(ReaderBody.of(r));
			// When a contentType attribute is in effect, serialize the POJO with the selected serializer
			// and send the overridden Content-Type label (replacing the serializer's default — exactly one header).
			if (bodyFormat.isSet())
				return req.body(SerializerBody.of(bodyFormat.serializer(), arg, bodyFormat.contentType()));
			return req.body(arg);
		}

		/**
		 * Assembles a {@code multipart/form-data} request body from the method's {@link Part @Part} parameters.
		 *
		 * <p>
		 * A thin adapter onto the existing streaming {@link MultipartBody}: each non-<jk>null</jk> {@code @Part}
		 * argument becomes one part whose name/filename/content-type come from the annotation and whose body is mapped
		 * from the argument type by {@link #toPartBody(Object, String)} (file/stream/reader/byte[] parts stream, reusing
		 * the streaming bodies; beans are serialized).  The {@code multipart/form-data} {@code Content-Type} (with
		 * its boundary) is supplied by {@link MultipartBody#getContentType()} when the request is built.
		 */
		private void bindMultipartBody(RestRequest req, Method method, Object[] args) {
			var builder = MultipartBody.builder();
			var params = method.getParameters();
			for (var i = 0; i < params.length; i++) {
				var part = params[i].getAnnotation(Part.class);
				if (part == null)
					continue;
				var arg = (args == null || i >= args.length) ? null : args[i];
				if (arg != null) {
					var name = firstNonEmpty(part.name(), part.value(), params[i].getName());
					var contentType = part.contentType().isEmpty() ? null : part.contentType();
					var fileName = firstNonEmpty(part.fileName().isEmpty() ? null : part.fileName(), defaultFileName(arg));
					builder.part(MultipartBody.MultipartPart.of(name, fileName, contentType, toPartBody(arg, contentType)));
				}
			}
			req.body(builder.build());
		}

		/**
		 * Maps a {@code @Part} argument to a streaming-or-buffered {@link HttpBody} based on its runtime type.
		 *
		 * <p>
		 * {@link File}/{@link InputStream}/{@link Reader} and {@code byte[]} parts reuse the streaming bodies
		 * (never pre-buffered); scalar values become a text {@link StringBody}; an {@link HttpBody} is used directly;
		 * any other object (a bean) is streamed through the client's default {@link org.apache.juneau.marshall.serializer.Serializer}.
		 */
		private HttpBody toPartBody(Object arg, String contentType) {
			if (arg instanceof HttpBody b)
				return b;
			if (arg instanceof byte[] bytes)
				return ByteArrayBody.of(bytes, firstNonEmpty(contentType, MEDIA_OCTET_STREAM));
			if (arg instanceof File f)
				return FileBody.of(f, firstNonEmpty(contentType, MEDIA_OCTET_STREAM));
			if (arg instanceof InputStream in)
				return StreamBody.of(in, firstNonEmpty(contentType, MEDIA_OCTET_STREAM));
			if (arg instanceof Reader r)
				return ReaderBody.of(r, firstNonEmpty(contentType, "text/plain; charset=UTF-8"));
			if (isScalarPart(arg))
				return StringBody.of(arg.toString(), firstNonEmpty(contentType, "text/plain; charset=UTF-8"));
			// Bean part: stream it through the client's default serializer (no full in-memory materialization).
			var s = client.getDefaultSerializer().orElseThrow(() -> new IllegalStateException(
				"No default serializer is configured on the client.  Configure one via RestClient.Builder.defaultSerializer(...)."));
			return contentType != null ? SerializerBody.of(s, arg, contentType) : SerializerBody.of(s, arg);
		}

		/** Returns the default filename for a {@code @Part} argument: a {@link File}'s own name, else <jk>null</jk>. */
		private static String defaultFileName(Object arg) {
			return arg instanceof File f ? f.getName() : null;
		}

		/** Returns <jk>true</jk> if the {@code @Part} argument is a scalar that should be sent as a text field. */
		private static boolean isScalarPart(Object arg) {
			return arg instanceof CharSequence || arg instanceof Number || arg instanceof Boolean
				|| arg instanceof Character || arg instanceof Enum || arg instanceof Date;
		}

		/**
		 * Runs the request and opens a token/record-streaming cursor over the (live) response body.
		 *
		 * <p>
		 * Unlike the buffered body paths, the response is <b>not</b> closed here on success: the returned cursor reads
		 * directly from the response stream and the caller owns it (close the cursor when done).  On any failure before
		 * the cursor is handed back, the response is closed.
		 */
		@SuppressWarnings({
			"resource" // On success the response is owned by the returned cursor (caller closes it); on failure it is closed in the finally block.
		})
		private Object processCursor(RestRequest req, Class<?> returnType, Method method, boolean throwOnError) throws Exception {
			var resp = req.run();
			var ok = false;
			try {
				throwIfError(resp, method, throwOnError);
				var cursor = resp.body().asCursor(returnType);
				ok = true;
				return cursor;
			} finally {
				if (! ok)
					resp.close();
			}
		}

		/**
		 * Parses a response body for an {@code @Remote}-proxy method return value.
		 *
		 * <p>
		 * Unlike {@link org.apache.juneau.rest.client.ResponseBody#as(org.apache.juneau.marshall.parser.Parser, Class)},
		 * which always surfaces a parse failure as an {@code IOException}, this method applies a deliberate proxy-only
		 * leniency: when the declared return type is {@code Object}, a {@link ParseException} is swallowed and the raw
		 * response body string is returned as-is. For any other declared return type the {@code ParseException}
		 * propagates unchanged.
		 *
		 * <p>
		 * Response parsing remains driven by the response {@code Content-Type} (the server is
		 * authoritative about what it actually sent).  The {@code acceptFallback} media type is consulted only when the
		 * response is unlabeled or its {@code Content-Type} matches no registered parser (see {@link #selectParser}).
		 */
		private static Object parseBody(RestResponse resp, Type returnType, String acceptFallback) throws Exception {
			var body = resp.getBodyAsString();
			if (body == null)
				return null;
			var h = resp.getFirstHeader("Content-Type");
			var parser = selectParser(resp, h == null ? null : h.value(), acceptFallback);
			try {
				return parser.parse(body, returnType);
			} catch (ParseException e) {
				if (returnType == Object.class)
					return body;
				throw e;
			}
		}

		/**
		 * Selects the response parser honoring the locked precedence: the response {@code Content-Type}
		 * is authoritative; the {@code accept} media type is only a fallback.
		 *
		 * <ol>
		 * 	<li>If the response {@code Content-Type} matches a registered parser, use it.
		 * 	<li>Otherwise (response unlabeled or its type matched nothing), use the parser matching the {@code accept}
		 * 		media type, if one is registered.
		 * 	<li>Otherwise fall back to the client's explicitly-configured default parser
		 * 		({@link RestClient#getMatchingParser(String)}); if none is configured this throws
		 * 		<c>415 Unsupported Media Type</c> (there is no implicit JSON fallback).
		 * </ol>
		 *
		 * @param resp The response (source of the client + parsers).
		 * @param responseContentType The response {@code Content-Type} header value. May be <jk>null</jk>.
		 * @param acceptFallback The {@code accept} media type fallback. May be <jk>null</jk>/empty.
		 * @return The parser to use. Never <jk>null</jk>.
		 * @throws UnsupportedMediaType If no parser matches and no default parser is configured on the client.
		 */
		private static Parser selectParser(RestResponse resp, String responseContentType, String acceptFallback) {
			var c = resp.getClient();
			var p = c.getParserForMediaType(responseContentType);
			if (p.isPresent())
				return p.get();
			if (isNotEmpty(acceptFallback)) {
				var ap = c.getParserForMediaType(acceptFallback);
				if (ap.isPresent())
					return ap.get();
			}
			return c.getMatchingParser(responseContentType).orElseThrow(() -> new UnsupportedMediaType(
				"No parser matched the response Content-Type ''{0}'' and no default parser is configured on the client.", responseContentType));
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
