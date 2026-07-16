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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.rest.common.utils.*;

/**
 * Holds resolved metadata for an interface annotated with {@link Remote @Remote}.
 *
 * <p>
 * Built once per interface class and cached for efficient proxy invocation.
 * Use {@link #of(Class)} to obtain instances.
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
public final class RrpcInterfaceMeta {

	private static final Map<Class<?>, RrpcInterfaceMeta> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private final Class<?> iface;
	private final String basePath;
	// Interface-level base/host override: an annotation-declared substitute for the client root URL.
	private final String baseUrl;
	// Interface-level content negotiation: default accept + contentType media-type overrides.
	private final String accept;
	private final String contentType;
	private final List<Map.Entry<String,String>> headers;
	private final List<Map.Entry<String,String>> queryData;
	private final List<Map.Entry<String,String>> formData;
	// Interface-level custom part serializer: from @HttpPartMarshalling(serializer=...) on the interface.
	private final HttpPartSerializer partSerializer;
	// Interface-level cross-cutting call policy: defaults inherited by every method.
	private final Class<?>[] interceptorClasses;
	private final String timeout;
	private final int retries;
	private final boolean retryNonIdempotent;
	private final boolean throwOnError;
	private final Map<Method, RrpcInterfaceMethodMeta> methodMetas;
	private final Map<String, RrpcInterfaceMethodMeta> methodMetasByPath;

	private RrpcInterfaceMeta(Class<?> iface) {
		this(iface, false);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Provides backwards compatibility with the classic {@code RrpcInterfaceMeta} signature; the {@code uri}
	 * argument is accepted for API parity and is currently ignored by this implementation.
	 *
	 * <p>
	 * Unlike {@link #of(Class)}, this constructor includes <i>all</i> public interface methods (matching the
	 * classic behavior), not just those annotated with the {@code @Remote*} annotations.
	 *
	 * @param iface The interface class. Must not be <jk>null</jk>.
	 * @param uri Reserved for compatibility with the classic constructor. May be <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // 'uri' is part of the public constructor signature retained for API parity with the classic RrpcInterfaceMeta; intentionally ignored
	})
	public RrpcInterfaceMeta(Class<?> iface, String uri) {
		this(iface, true);
	}

	private RrpcInterfaceMeta(Class<?> iface, boolean includeUnannotated) {
		if (!iface.isInterface())
			throw iaex("Class %s is not an interface", iface.getName());
		var remote = iface.getAnnotation(Remote.class);

		this.iface = iface;
		this.basePath = buildBasePath(remote);
		this.baseUrl = remote == null ? "" : VarResolver.DEFAULT.resolve(remote.baseUrl());
		this.accept = remote == null ? "" : VarResolver.DEFAULT.resolve(remote.accept());
		this.contentType = remote == null ? "" : VarResolver.DEFAULT.resolve(remote.contentType());
		this.headers = buildHeaders(remote);
		this.queryData = remote == null ? List.of() : parseConstantParts(remote.queryData(), '=');
		this.formData = remote == null ? List.of() : parseConstantParts(remote.formData(), '=');
		var hpm = iface.getAnnotation(HttpPartMarshalling.class);
		this.partSerializer = hpm == null ? null : RrpcInterfaceMethodMeta.resolvePartSerializer(hpm.serializer());
		// Interface-level cross-cutting policy defaults.
		this.interceptorClasses = remote == null ? new Class<?>[0] : remote.interceptors();
		this.timeout = remote == null ? "" : remote.timeout();
		this.retries = remote == null ? 0 : remote.retries();
		this.retryNonIdempotent = remote != null && remote.retryNonIdempotent();
		this.throwOnError = remote != null && remote.throwOnError();

		var metas = new LinkedHashMap<Method, RrpcInterfaceMethodMeta>();
		for (var m : iface.getMethods()) {
			var opt = buildMethodMeta(m);
			if (opt.isPresent()) {
				metas.put(m, opt.get());
			} else if (includeUnannotated) {
				metas.put(m, new RrpcInterfaceMethodMeta(m, "POST", buildSignaturePath(m), RemoteReturn.BODY));
			}
		}
		this.methodMetas = Collections.unmodifiableMap(metas);

		var byPath = new LinkedHashMap<String, RrpcInterfaceMethodMeta>();
		for (var v : metas.values())
			byPath.put(v.getPath(), v);
		this.methodMetasByPath = Collections.unmodifiableMap(byPath);
	}

	private static String buildBasePath(Remote remote) {
		if (remote == null)
			return "";
		return remote.path().isEmpty() ? remote.value() : remote.path();
	}

	/**
	 * Resolves the interface-level constant headers declared via {@link Remote#headers()} and the client-version
	 * header derived from {@link Remote#version()}/{@link Remote#versionHeader()}.
	 *
	 * <p>
	 * Each {@code headers()} entry is a {@code "Name: value"} string (the colon-separated form), resolved through
	 * {@link VarResolver#DEFAULT} so values such as {@code "$S{sysprop}"} expand.  A non-empty {@code version()}
	 * appends a {@code Client-Version} header (or the name given by {@code versionHeader()}).
	 *
	 * <p>
	 * <b>Note:</b> {@link Remote#headerList()} is intentionally <i>not</i> honored by the next-gen engine — its value
	 * type is a classic {@code org.apache.juneau.http.classic.header.HeaderList} subclass that is specific to the
	 * Apache-HttpClient transport.  Use {@code headers()} for transport-agnostic constant headers.
	 */
	private static List<Map.Entry<String,String>> buildHeaders(Remote remote) {
		if (remote == null)
			return List.of();
		var l = new ArrayList<>(parseConstantParts(remote.headers(), ':'));
		var version = VarResolver.DEFAULT.resolve(remote.version());
		if (! version.isEmpty()) {
			var vh = remote.versionHeader().isEmpty() ? "Client-Version" : VarResolver.DEFAULT.resolve(remote.versionHeader());
			l.add(Map.entry(vh, version));
		}
		return Collections.unmodifiableList(l);
	}

	/**
	 * Parses an array of constant {@code "name<delim>value"} strings into resolved name/value entries.
	 *
	 * <p>
	 * Each entry is resolved through {@link VarResolver#DEFAULT} (so values such as {@code "$S{sysprop}"} expand), then
	 * split on the first occurrence of {@code delim} ({@code ':'} for the {@code "Name: value"} header form, {@code '='}
	 * for the {@code "name=value"} query/form-data form).  Entries with no delimiter are skipped.  Both the name and the
	 * value are trimmed.
	 *
	 * @param entries The raw annotation strings (may be empty).
	 * @param delim The name/value delimiter character.
	 * @return An unmodifiable list of resolved name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	static List<Map.Entry<String,String>> parseConstantParts(String[] entries, char delim) {
		if (entries.length == 0)
			return List.of();
		var l = new ArrayList<Map.Entry<String,String>>();
		for (var e : entries) {
			var s = VarResolver.DEFAULT.resolve(e);
			var i = s.indexOf(delim);
			if (i != -1)
				l.add(Map.entry(s.substring(0, i).trim(), s.substring(i + 1).trim()));
		}
		return Collections.unmodifiableList(l);
	}

	private static String buildSignaturePath(Method m) {
		var sb = new StringBuilder(128);
		sb.append(m.getName()).append('/');
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length == 0)
			return sb.toString();
		sb.append('(');
		for (var i = 0; i < pt.length; i++) {
			if (i > 0)
				sb.append(',');
			appendTypeName(sb, pt[i]);
		}
		sb.append(')');
		return sb.toString();
	}

	private static void appendTypeName(StringBuilder sb, Class<?> c) {
		if (c.isArray()) {
			appendTypeName(sb, c.getComponentType());
			sb.append("[]");
		} else {
			sb.append(c.getName());
		}
	}

	/**
	 * Returns the {@link RrpcInterfaceMeta} for the given interface, creating and caching it if necessary.
	 *
	 * @param iface The interface class. Must be annotated with {@link Remote}. Must not be <jk>null</jk>.
	 * @return The metadata. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the class is not an interface or not annotated with {@link Remote}.
	 */
	public static RrpcInterfaceMeta of(Class<?> iface) {
		assertArgNotNull("iface", iface);
		if (!iface.isInterface())
			throw iaex("Class %s is not an interface", iface.getName());
		if (iface.getAnnotation(Remote.class) == null)
			throw iaex("Class %s is not annotated with @Remote", iface.getName());
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
	 * Returns the Java class of this interface.
	 *
	 * <p>
	 * Alias for {@link #getInterface()} provided for backwards compatibility with the classic API.
	 *
	 * @return The Java class. Never <jk>null</jk>.
	 */
	public Class<?> getJavaClass() {
		return getInterface();
	}

	/**
	 * Returns the method metadata keyed by the resolved method path.
	 *
	 * @return An unmodifiable map from path to {@link RrpcInterfaceMethodMeta}. Never <jk>null</jk>.
	 */
	public Map<String, RrpcInterfaceMethodMeta> getMethodsByPath() {
		return methodMetasByPath;
	}

	/**
	 * Returns the method metadata for the given path.
	 *
	 * @param path The path. May be <jk>null</jk>.
	 * @return The matching method metadata, or <jk>null</jk> if no match was found.
	 */
	public RrpcInterfaceMethodMeta getMethodMetaByPath(String path) {
		return path == null ? null : methodMetasByPath.get(path);
	}

	/**
	 * Returns the base path from the {@link Remote#path() Remote} annotation.
	 *
	 * @return The base path. Never <jk>null</jk>, but may be empty.
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Returns the interface-level base/host override, or an empty string if none.
	 *
	 * <p>
	 * Sourced from {@link Remote#baseUrl()} (resolved through {@link VarResolver#DEFAULT}).  Used as a substitute for
	 * the client root URL when neither an {@link org.apache.juneau.http.Url @Url} parameter nor a method-level
	 * {@code baseUrl} is in effect; preserves the interface base path + method path + templating.
	 *
	 * @return The base/host override. Never <jk>null</jk>, but may be empty.
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Returns the interface-level default {@code contentType} media-type override, or an empty string.
	 *
	 * <p>
	 * Sourced from {@link Remote#contentType()} (resolved through {@link VarResolver#DEFAULT}).  Used as the default
	 * request-serializer/{@code Content-Type} selector for methods that declare no method-level
	 * {@link RrpcInterfaceMethodMeta#getContentType()}.
	 *
	 * @return The {@code contentType} media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Returns the interface-level default {@code accept} media-type override, or an empty string.
	 *
	 * <p>
	 * Sourced from {@link Remote#accept()} (resolved through {@link VarResolver#DEFAULT}).  Used as the default
	 * {@code Accept} header / response-parser fallback for methods that declare no method-level
	 * {@link RrpcInterfaceMethodMeta#getAccept()}.
	 *
	 * @return The {@code accept} media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getAccept() {
		return accept;
	}

	/**
	 * Returns the interface-level constant headers to set on every request.
	 *
	 * <p>
	 * Includes the headers declared via {@link Remote#headers()} plus the client-version header derived from
	 * {@link Remote#version()}/{@link Remote#versionHeader()} (if a version was specified).
	 *
	 * @return An unmodifiable list of name/value header entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getHeaders() {
		return headers;
	}

	/**
	 * Returns the interface-level constant query parameters to apply to every request.
	 *
	 * <p>
	 * Resolved from {@link Remote#queryData()} (the {@code "name=value"} form, with {@link VarResolver#DEFAULT}
	 * expansion).
	 *
	 * @return An unmodifiable list of name/value query entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getQueryData() {
		return queryData;
	}

	/**
	 * Returns the interface-level constant form-data parameters to apply to every request.
	 *
	 * <p>
	 * Resolved from {@link Remote#formData()} (the {@code "name=value"} form, with {@link VarResolver#DEFAULT}
	 * expansion).
	 *
	 * @return An unmodifiable list of name/value form-data entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getFormData() {
		return formData;
	}

	/**
	 * Returns the interface-level custom {@link HttpPartSerializer}, or <jk>null</jk> if none.
	 *
	 * <p>
	 * Sourced from {@code @HttpPartMarshalling(serializer=...)} declared on the interface.  Used as
	 * the lowest-precedence default for part serialization: a parameter-level or method-level
	 * {@code @HttpPartMarshalling} serializer takes precedence over it.
	 *
	 * @return The resolved part serializer, or <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the interface-level interceptor classes, or an empty array if none.
	 *
	 * <p>
	 * Each class implements the next-gen client-side interceptor SPI and is resolved to an instance by the next-gen
	 * {@code RemoteClient} (the raw {@code Class<?>[]} type avoids a module dependency on {@code juneau-rest-client}).
	 *
	 * @return The interceptor classes. Never <jk>null</jk>, but may be empty.
	 */
	public Class<?>[] getInterceptorClasses() {
		return interceptorClasses;
	}

	/**
	 * Returns the interface-level default per-call timeout as a duration string, or an empty string.
	 *
	 * @return The timeout duration string. Never <jk>null</jk>, but may be empty.
	 */
	public String getTimeout() {
		return timeout;
	}

	/**
	 * Returns the interface-level default max retry attempts, or {@code 0} if retries are disabled.
	 *
	 * @return The retry count.
	 */
	public int getRetries() {
		return retries;
	}

	/**
	 * Returns whether the interface opts non-idempotent verbs (POST/PATCH) into automatic retries.
	 *
	 * @return <jk>true</jk> if non-idempotent retries are opted in at the interface level.
	 */
	public boolean isRetryNonIdempotent() {
		return retryNonIdempotent;
	}

	/**
	 * Returns whether the interface throws a generic exception on an unmatched error response.
	 *
	 * @return <jk>true</jk> if {@code throwOnError} is set at the interface level.
	 */
	public boolean isThrowOnError() {
		return throwOnError;
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

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity inherent to per-verb interface-metadata parsing; refactoring would reduce readability without benefit.
	})
	private static Optional<RrpcInterfaceMethodMeta> buildMethodMeta(Method m) {
		if (m.isAnnotationPresent(RemoteGet.class)) {
			var a = m.getAnnotation(RemoteGet.class);
			return o(simpleMeta(m, "GET", a.path(), a.value(), a.returns(), a.headers(), a.queryData(), a.formData(), a.baseUrl(),
				new RrpcInterfaceMethodMeta.Policy(a.interceptors(), a.timeout(), a.retries(), a.retryNonIdempotent(), a.throwOnError()),
				neg(a.accept(), a.contentType())));
		}
		if (m.isAnnotationPresent(RemotePost.class)) {
			var a = m.getAnnotation(RemotePost.class);
			return o(simpleMeta(m, "POST", a.path(), a.value(), a.returns(), a.headers(), a.queryData(), a.formData(), a.baseUrl(),
				new RrpcInterfaceMethodMeta.Policy(a.interceptors(), a.timeout(), a.retries(), a.retryNonIdempotent(), a.throwOnError()),
				neg(a.accept(), a.contentType())));
		}
		if (m.isAnnotationPresent(RemotePut.class)) {
			var a = m.getAnnotation(RemotePut.class);
			return o(simpleMeta(m, "PUT", a.path(), a.value(), a.returns(), a.headers(), a.queryData(), a.formData(), a.baseUrl(),
				new RrpcInterfaceMethodMeta.Policy(a.interceptors(), a.timeout(), a.retries(), a.retryNonIdempotent(), a.throwOnError()),
				neg(a.accept(), a.contentType())));
		}
		if (m.isAnnotationPresent(RemotePatch.class)) {
			var a = m.getAnnotation(RemotePatch.class);
			return o(simpleMeta(m, "PATCH", a.path(), a.value(), a.returns(), a.headers(), a.queryData(), a.formData(), a.baseUrl(),
				new RrpcInterfaceMethodMeta.Policy(a.interceptors(), a.timeout(), a.retries(), a.retryNonIdempotent(), a.throwOnError()),
				neg(a.accept(), a.contentType())));
		}
		if (m.isAnnotationPresent(RemoteDelete.class)) {
			var a = m.getAnnotation(RemoteDelete.class);
			return o(simpleMeta(m, "DELETE", a.path(), a.value(), a.returns(), a.headers(), a.queryData(), a.formData(), a.baseUrl(),
				new RrpcInterfaceMethodMeta.Policy(a.interceptors(), a.timeout(), a.retries(), a.retryNonIdempotent(), a.throwOnError()),
				neg(a.accept(), a.contentType())));
		}
		if (m.isAnnotationPresent(RemoteOp.class))
			return o(buildRemoteOpMeta(m, m.getAnnotation(RemoteOp.class)));

		return oe();
	}

	/**
	 * Builds a resolved {@link RrpcInterfaceMethodMeta.ContentNegotiation} carrier from the raw
	 * {@code accept}/{@code contentType} annotation members, expanding {@link VarResolver#DEFAULT} variables.
	 */
	private static RrpcInterfaceMethodMeta.ContentNegotiation neg(String accept, String contentType) {
		return new RrpcInterfaceMethodMeta.ContentNegotiation(VarResolver.DEFAULT.resolve(accept), VarResolver.DEFAULT.resolve(contentType));
	}

	@SuppressWarnings({
		"java:S107" // The 11 parameters mirror the verb-annotation surface (verb/path/value/return + the 3 constant-part arrays + the baseUrl override + the cross-cutting policy carrier + the content-negotiation carrier) threaded into the method-meta; holder types would add indirection without improving this internal builder.
	})
	private static RrpcInterfaceMethodMeta simpleMeta(Method m, String httpMethod, String pathAttr, String valueAttr, RemoteReturn returnType, String[] headers, String[] queryData, String[] formData, String baseUrl, RrpcInterfaceMethodMeta.Policy policy, RrpcInterfaceMethodMeta.ContentNegotiation neg) {
		var path = pathAttr.isEmpty() ? valueAttr : pathAttr;
		return new RrpcInterfaceMethodMeta(m, httpMethod, path, returnType,
			parseConstantParts(headers, ':'),
			parseConstantParts(queryData, '='),
			parseConstantParts(formData, '='),
			VarResolver.DEFAULT.resolve(baseUrl),
			policy,
			neg);
	}

	private static RrpcInterfaceMethodMeta buildRemoteOpMeta(Method m, RemoteOp a) {
		var method = a.method().trim();
		var path = a.path().trim();
		var returnType = a.returns();
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
			throw iaex("Invalid @RemoteOp method '%s' on %s.%s", method, m.getDeclaringClass().getName(), m.getName());
		return new RrpcInterfaceMethodMeta(m, method, trimSlashes(path), returnType,
			parseConstantParts(a.headers(), ':'),
			parseConstantParts(a.queryData(), '='),
			parseConstantParts(a.formData(), '='),
			VarResolver.DEFAULT.resolve(a.baseUrl()),
			new RrpcInterfaceMethodMeta.Policy(a.interceptors(), a.timeout(), a.retries(), a.retryNonIdempotent(), a.throwOnError()),
			neg(a.accept(), a.contentType()));
	}

	@Override /* Object */
	public String toString() {
		return "@Remote " + iface.getSimpleName() + " (basePath=" + basePath + ", methods=" + methodMetas.size() + ")";
	}
}
