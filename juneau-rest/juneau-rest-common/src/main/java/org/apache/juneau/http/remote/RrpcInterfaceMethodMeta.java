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
package org.apache.juneau.http.remote;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.Constants.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.httppart.*;

/**
 * Holds resolved metadata for a single method on an interface annotated with
 * {@link Remote}.
 *
 * <p>
 * Extracted at interface-discovery time (see {@link RrpcInterfaceMeta}) and cached for
 * efficient proxy invocation.
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
public final class RrpcInterfaceMethodMeta {

	// Per-class cache of resolved part serializers, mirroring the classic
	// RestClient.getPartSerializer(Class) per-class cache.  The next-gen client has no bean store, so resolved
	// instances are independent of any client and safe to share statically across interfaces/methods.
	private static final Map<Class<? extends HttpPartSerializer>, HttpPartSerializer> PART_SERIALIZERS = new ConcurrentHashMap<>();

	/**
	 * Resolves an {@link HttpPartSerializer} class declared on an {@link HttpPartMarshalling} annotation to a cached
	 * instance, or returns <jk>null</jk> for the {@link org.apache.juneau.marshall.httppart.HttpPartSerializer.Void} sentinel (or a <jk>null</jk> class).
	 *
	 * <p>
	 * Instances are created through {@link BeanInstantiator} (bean-store-less in the next-gen engine) and cached per
	 * class.  Mirrors the classic {@code RestClient.getPartSerializer(Class)} per-class cache.
	 *
	 * @param c The part serializer class. May be <jk>null</jk>.
	 * @return The resolved (cached) part serializer, or <jk>null</jk> if none was specified.
	 */
	public static HttpPartSerializer resolvePartSerializer(Class<? extends HttpPartSerializer> c) {
		if (c == null || c == HttpPartSerializer.Void.class)
			return null;
		return PART_SERIALIZERS.computeIfAbsent(c, k -> BeanInstantiator.of(k).run());
	}

	/**
	 * Method-level cross-cutting call policy: interceptors, per-call timeout, retries, and the
	 * generic {@code throwOnError} flag, sourced from {@code @RemoteOp}/verb annotation members.
	 *
	 * <p>
	 * Kept as a single carrier so the (already busy) method-meta constructor isn't flooded with scalar arguments.
	 * The {@code interceptors} are stored as raw {@code Class<?>[]} (rather than {@code Class<? extends
	 * RestCallInterceptor>[]}) because {@code RestCallInterceptor} lives in the {@code juneau-rest-client} module, which
	 * this module must not depend on; they are resolved to interceptor instances by the next-gen {@code RemoteClient}.
	 *
	 * @param interceptors The interceptor classes to apply (each must implement the client-side interceptor SPI).
	 * @param timeout The per-call response timeout as a duration string (empty = none).
	 * @param retries The max retry attempts (0 = disabled).
	 * @param retryNonIdempotent Whether to allow retrying non-idempotent verbs (POST/PATCH).
	 * @param throwOnError Whether to throw a generic exception on an unmatched error response.
	 */
	public record Policy(Class<?>[] interceptors, String timeout, int retries, boolean retryNonIdempotent, boolean throwOnError) {

		/** The empty (no-op) policy used for methods that declare none of the cross-cutting policy members. */
		public static final Policy NONE = new Policy(new Class<?>[0], "", 0, false, false);

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Policy other)) return false;
			return Arrays.equals(interceptors, other.interceptors)
				&& timeout.equals(other.timeout)
				&& retries == other.retries
				&& retryNonIdempotent == other.retryNonIdempotent
				&& throwOnError == other.throwOnError;
		}

		@Override
		public int hashCode() {
			return h(Arrays.hashCode(interceptors), timeout, retries, retryNonIdempotent, throwOnError);
		}

		@Override
		public String toString() {
			return "Policy[interceptors=" + Arrays.toString(interceptors)
				+ ", timeout=" + timeout
				+ ", retries=" + retries
				+ ", retryNonIdempotent=" + retryNonIdempotent
				+ ", throwOnError=" + throwOnError + "]";
		}
	}

	/**
	 * Method-level content negotiation: the {@code accept} and {@code contentType} media-type
	 * overrides sourced from {@code @RemoteOp}/verb-annotation members.
	 *
	 * <p>
	 * Kept as a single carrier so the (already busy) method-meta constructor isn't flooded with scalar arguments.
	 * Both values are plain media-type strings (e.g. {@code "application/xml"}); an empty string means "not set".
	 *
	 * @param accept The {@code Accept} media type (empty = none).
	 * @param contentType The {@code Content-Type} / request-serializer media type (empty = none).
	 */
	public record ContentNegotiation(String accept, String contentType) {

		/** The empty content-negotiation carrier used for methods that declare neither member. */
		public static final ContentNegotiation NONE = new ContentNegotiation("", "");
	}

	private final Method method;
	private final String httpMethod;
	private final String path;
	private final RemoteReturn returnType;
	// Method-level custom part serializer: from @HttpPartMarshalling(serializer=...) on the method.
	private final HttpPartSerializer partSerializer;
	// Method-level cross-cutting call policy.
	private final Policy policy;
	// Method-level content negotiation: accept + contentType media-type overrides.
	private final ContentNegotiation neg;
	// Method-level part defaults: a default that fills a null argument for the matching part name.
	private final Map<String,String> headerDefaults;
	private final Map<String,String> queryDefaults;
	private final Map<String,String> formDataDefaults;
	private final Map<String,String> pathDefaults;
	// Method-level @Content(def): default/constant body applied when the body arg is null or absent.
	private final String contentDefault;
	// Method-level base/host override: an annotation-declared substitute for the client root URL.
	private final String baseUrl;
	// Index of the single @Url parameter, or -1 if the method declares none.
	private final int urlParamIndex;
	// Whether this method assembles a multipart/form-data body from its @Part parameters.
	private final boolean multipart;
	// Method-level always-applied constants: emitted on every call, no parameter required.
	private final List<Map.Entry<String,String>> constantHeaders;
	private final List<Map.Entry<String,String>> constantQueryData;
	private final List<Map.Entry<String,String>> constantFormData;

	RrpcInterfaceMethodMeta(Method method, String httpMethod, String path, RemoteReturn returnType) {
		this(method, httpMethod, path, returnType, List.of(), List.of(), List.of(), "", Policy.NONE, ContentNegotiation.NONE);
	}

	@SuppressWarnings({
		"java:S107" // The constructor mirrors the verb-annotation surface (verb/path/return + 3 constant-part lists + the baseUrl override + the cross-cutting policy carrier + the content-negotiation carrier); wider holders would add indirection without improving this internal metadata builder.
	})
	RrpcInterfaceMethodMeta(Method method, String httpMethod, String path, RemoteReturn returnType,
			List<Map.Entry<String,String>> constantHeaders, List<Map.Entry<String,String>> constantQueryData,
			List<Map.Entry<String,String>> constantFormData, String baseUrl, Policy policy, ContentNegotiation neg) {
		this.method = method;
		this.httpMethod = httpMethod;
		this.path = path;
		this.returnType = returnType;
		this.constantHeaders = constantHeaders;
		this.constantQueryData = constantQueryData;
		this.constantFormData = constantFormData;
		this.baseUrl = baseUrl;
		this.urlParamIndex = findUrlParamIndex(method);
		this.multipart = method.getAnnotation(Multipart.class) != null;
		validateMultipart(method, multipart);
		this.policy = policy;
		this.neg = neg;

		var h = method.getAnnotation(Header.class);
		this.headerDefaults = h == null ? Map.of() : partDefault(h.name(), h.value(), h.def(), false);
		var q = method.getAnnotation(Query.class);
		this.queryDefaults = q == null ? Map.of() : partDefault(q.name(), q.value(), q.def(), false);
		var f = method.getAnnotation(FormData.class);
		this.formDataDefaults = f == null ? Map.of() : partDefault(f.name(), f.value(), f.def(), false);
		var p = method.getAnnotation(Path.class);
		this.pathDefaults = p == null ? Map.of() : partDefault(p.name(), p.value(), p.def(), true);
		var c = method.getAnnotation(Content.class);
		this.contentDefault = (c == null || c.def().isEmpty()) ? null : c.def();
		var hpm = method.getAnnotation(HttpPartMarshalling.class);
		this.partSerializer = hpm == null ? null : resolvePartSerializer(hpm.serializer());
	}

	/**
	 * Builds a single-entry method-level default map from a method-level part annotation.
	 *
	 * <p>
	 * The entry is keyed by the part name ({@code name()} falling back to {@code value()}).  The default is recorded
	 * only when both a name and a "set" default are present.  For {@code @Path} (where {@code def()} defaults to the
	 * {@code _NONE_} sentinel) any value other than the sentinel counts as set; for the other part types a non-empty
	 * default counts as set.
	 */
	private static Map<String,String> partDefault(String name, String value, String def, boolean pathSemantics) {
		var key = name.isEmpty() ? value : name;
		var hasDef = pathSemantics ? ! NONE.equals(def) : ! def.isEmpty();
		return (key.isEmpty() || ! hasDef) ? Map.of() : Map.of(key, def);
	}

	/**
	 * Locates the single {@link Url @Url} parameter on the method.
	 *
	 * <p>
	 * Validated at proxy-build time: at most one {@code @Url} parameter is permitted per method.
	 *
	 * @param method The method to scan.
	 * @return The zero-based index of the {@code @Url} parameter, or {@code -1} if the method declares none.
	 * @throws IllegalArgumentException If the method declares more than one {@code @Url} parameter.
	 */
	private static int findUrlParamIndex(Method method) {
		var params = method.getParameters();
		var index = -1;
		for (var i = 0; i < params.length; i++) {
			if (params[i].getAnnotation(Url.class) != null) {
				if (index != -1)
					throw iaex("Method %s.%s declares more than one @Url parameter; at most one is allowed",
						method.getDeclaringClass().getName(), method.getName());
				index = i;
			}
		}
		return index;
	}

	/**
	 * Validates the multipart body-mode declaration on the method.
	 *
	 * <p>
	 * Enforces body-mode exclusivity and detects misuse at proxy-build time:
	 * <ul>
	 * 	<li>A {@code @Multipart} method must declare at least one {@link Part @Part} parameter.
	 * 	<li>A {@code @Multipart} method must not also declare a single {@link Content @Content}
	 * 		body (parameter-level or method-level) &mdash; a method is either multipart or single-body, not both.
	 * 	<li>A {@link Part @Part} parameter is only valid on a {@code @Multipart} method.
	 * </ul>
	 *
	 * @param method The method to validate.
	 * @param multipart Whether the method is annotated {@link Multipart}.
	 * @throws IllegalArgumentException If the multipart declaration is invalid.
	 */
	private static void validateMultipart(Method method, boolean multipart) {
		var partCount = 0;
		var hasContentParam = method.getAnnotation(Content.class) != null;
		for (var p : method.getParameters()) {
			if (p.getAnnotation(Part.class) != null)
				partCount++;
			if (p.getAnnotation(Content.class) != null)
				hasContentParam = true;
		}
		var cn = method.getDeclaringClass().getName();
		var mn = method.getName();
		if (multipart) {
			if (hasContentParam)
				throw iaex("Method %s.%s declares both @Multipart and @Content; a method is either multipart or single-@Content, not both", cn, mn);
			if (partCount == 0)
				throw iaex("Method %s.%s is annotated @Multipart but declares no @Part parameters", cn, mn);
		} else if (partCount > 0) {
			throw iaex("Method %s.%s declares @Part parameter(s) but is not annotated @Multipart", cn, mn);
		}
	}

	/**
	 * Returns the Java method this metadata is for.
	 *
	 * @return The method. Never <jk>null</jk>.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Returns the Java method this metadata is for.
	 *
	 * <p>
	 * Alias for {@link #getMethod()} provided for backwards compatibility with the classic API.
	 *
	 * @return The Java method. Never <jk>null</jk>.
	 */
	public Method getJavaMethod() {
		return getMethod();
	}

	/**
	 * Returns the HTTP method (e.g. {@code "GET"}, {@code "POST"}).
	 *
	 * @return The HTTP method. Never <jk>null</jk>.
	 */
	public String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the resolved path for this operation (relative to the interface base path).
	 *
	 * @return The path. Never <jk>null</jk>, but may be empty.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns what the proxy method should return.
	 *
	 * @return The return type. Never <jk>null</jk>.
	 */
	public RemoteReturn getReturnType() {
		return returnType;
	}

	/**
	 * Returns the method-level default for a {@link Header @Header} part, or <jk>null</jk> if none.
	 *
	 * @param name The header name.
	 * @return The default value, or <jk>null</jk>.
	 */
	public String getHeaderDefault(String name) {
		return headerDefaults.get(name);
	}

	/**
	 * Returns the method-level default for a {@link Query @Query} part, or <jk>null</jk> if none.
	 *
	 * @param name The query parameter name.
	 * @return The default value, or <jk>null</jk>.
	 */
	public String getQueryDefault(String name) {
		return queryDefaults.get(name);
	}

	/**
	 * Returns the method-level default for a {@link FormData @FormData} part, or <jk>null</jk> if none.
	 *
	 * @param name The form-data parameter name.
	 * @return The default value, or <jk>null</jk>.
	 */
	public String getFormDataDefault(String name) {
		return formDataDefaults.get(name);
	}

	/**
	 * Returns the method-level default for a {@link Path @Path} part, or <jk>null</jk> if none.
	 *
	 * @param name The path variable name.
	 * @return The default value, or <jk>null</jk>.
	 */
	public String getPathDefault(String name) {
		return pathDefaults.get(name);
	}

	/**
	 * Returns the method-level {@link Content @Content} default body, or <jk>null</jk> if none.
	 *
	 * <p>
	 * Sourced from {@code @Content(def=...)} declared on the method.  Used as the body when a {@code @Content}
	 * parameter is <jk>null</jk>, or as a constant body when the method declares no {@code @Content} parameter.
	 *
	 * @return The default body value, or <jk>null</jk>.
	 */
	public String getContentDefault() {
		return contentDefault;
	}

	/**
	 * Returns the method-level base/host override, or an empty string if none.
	 *
	 * <p>
	 * Sourced from {@code @RemoteOp(baseUrl=...)} / the verb annotations' {@code baseUrl} (resolved through
	 * {@link org.apache.juneau.commons.svl.VarResolver#DEFAULT}).  Takes precedence over the interface-level
	 * {@link RrpcInterfaceMeta#getBaseUrl()} but is itself overridden by an {@link Url @Url}
	 * parameter; preserves the interface base path + method path + templating.
	 *
	 * @return The base/host override. Never <jk>null</jk>, but may be empty.
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Returns the method-level {@code contentType} media-type override, or an empty string if none.
	 *
	 * <p>
	 * Sourced from {@code @RemoteOp(contentType=...)} / the verb annotations' {@code contentType}.  Selects the
	 * matching request serializer and sets the {@code Content-Type} header; takes precedence over the interface-level
	 * {@link RrpcInterfaceMeta#getContentType()}.
	 *
	 * @return The {@code contentType} media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getContentType() {
		return neg.contentType();
	}

	/**
	 * Returns the method-level {@code accept} media-type override, or an empty string if none.
	 *
	 * <p>
	 * Sourced from {@code @RemoteOp(accept=...)} / the verb annotations' {@code accept}.  Sets the {@code Accept}
	 * header and acts as the response parser fallback; takes precedence over the interface-level
	 * {@link RrpcInterfaceMeta#getAccept()}.
	 *
	 * @return The {@code accept} media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getAccept() {
		return neg.accept();
	}

	/**
	 * Returns the zero-based index of the method's single {@link Url @Url} parameter,
	 * or {@code -1} if the method declares none.
	 *
	 * <p>
	 * The presence and structural position of the {@code @Url} parameter is fixed per method (validated at proxy-build
	 * time: at most one is allowed); the per-call value is read from the invocation arguments by the next-gen
	 * {@code RemoteClient} and never cached here.
	 *
	 * @return The {@code @Url} parameter index, or {@code -1}.
	 */
	public int getUrlParamIndex() {
		return urlParamIndex;
	}

	/**
	 * Returns whether this method assembles a {@code multipart/form-data} request body from its
	 * {@link Part @Part} parameters.
	 *
	 * <p>
	 * Sourced from the presence of a method-level {@link Multipart @Multipart} annotation.  Validated at proxy-build
	 * time: a multipart method must declare at least one {@code @Part} parameter and must not also declare a single
	 * {@link Content @Content} body.
	 *
	 * @return <jk>true</jk> if this is a multipart method.
	 */
	public boolean isMultipart() {
		return multipart;
	}

	/**
	 * Returns the method-level custom {@link HttpPartSerializer}, or <jk>null</jk> if none.
	 *
	 * <p>
	 * Sourced from {@code @HttpPartMarshalling(serializer=...)} declared on the method.  Applied to a
	 * parameter's parts when the parameter itself declares no {@code @HttpPartMarshalling} serializer; the
	 * interface-level default ({@link RrpcInterfaceMeta#getPartSerializer()}) applies when neither is present.
	 *
	 * @return The resolved part serializer, or <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the method-level interceptor classes, or an empty array if none.
	 *
	 * <p>
	 * Each class implements the next-gen client-side interceptor SPI and is resolved to an instance by the next-gen
	 * {@code RemoteClient} (the raw {@code Class<?>[]} type avoids a module dependency on {@code juneau-rest-client}).
	 *
	 * @return The interceptor classes. Never <jk>null</jk>, but may be empty.
	 */
	public Class<?>[] getInterceptorClasses() {
		return policy.interceptors();
	}

	/**
	 * Returns the method-level per-call timeout as a duration string, or an empty string if none.
	 *
	 * @return The timeout duration string. Never <jk>null</jk>, but may be empty.
	 */
	public String getTimeout() {
		return policy.timeout();
	}

	/**
	 * Returns the method-level max retry attempts, or {@code 0} if retries are disabled.
	 *
	 * @return The retry count.
	 */
	public int getRetries() {
		return policy.retries();
	}

	/**
	 * Returns whether this method opts non-idempotent verbs (POST/PATCH) into automatic retries.
	 *
	 * @return <jk>true</jk> if non-idempotent retries are opted in at the method level.
	 */
	public boolean isRetryNonIdempotent() {
		return policy.retryNonIdempotent();
	}

	/**
	 * Returns whether this method throws a generic exception on an unmatched error response.
	 *
	 * @return <jk>true</jk> if {@code throwOnError} is set at the method level.
	 */
	public boolean isThrowOnError() {
		return policy.throwOnError();
	}

	/**
	 * Returns the method-level always-applied constant headers ({@code "Name: value"} form), resolved via
	 * {@link org.apache.juneau.commons.svl.VarResolver#DEFAULT}.
	 *
	 * @return An unmodifiable list of name/value header entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getConstantHeaders() {
		return constantHeaders;
	}

	/**
	 * Returns the method-level always-applied constant query parameters ({@code "name=value"} form), resolved via
	 * {@link org.apache.juneau.commons.svl.VarResolver#DEFAULT}.
	 *
	 * @return An unmodifiable list of name/value query entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getConstantQueryData() {
		return constantQueryData;
	}

	/**
	 * Returns the method-level always-applied constant form-data parameters ({@code "name=value"} form), resolved via
	 * {@link org.apache.juneau.commons.svl.VarResolver#DEFAULT}.
	 *
	 * @return An unmodifiable list of name/value form-data entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getConstantFormData() {
		return constantFormData;
	}

	@Override /* Object */
	public String toString() {
		return httpMethod + " " + path + " → " + method.getName() + "()";
	}
}
