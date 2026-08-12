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
package org.apache.juneau.rest.client.classic.remote;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.http.remote.RemoteUtils.*;
import static org.apache.juneau.marshall.Constants.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.common.utils.*;

/**
 * Contains the meta-data about a Java method on a REST proxy class.
 *
 * <p>
 * Captures the information in {@link RemoteOp @RemoteOp} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClient">juneau-rest-client Basics</a>
 * </ul>
 */
public class RemoteOperationMeta {

	private static class Builder {
		String httpMethod;
		String fullPath;
		String path;
		List<RemoteOperationArg> pathArgs = ll();
		List<RemoteOperationArg> queryArgs = ll();
		List<RemoteOperationArg> headerArgs = ll();
		List<RemoteOperationArg> formDataArgs = ll();
		List<RemoteOperationBeanArg> requestArgs = ll();
		RemoteOperationArg bodyArg;
		RemoteOperationReturn methodReturn;
		Map<String,String> pathDefaults = m();
		Map<String,String> queryDefaults = m();
		Map<String,String> headerDefaults = m();
		Map<String,String> formDataDefaults = m();
		String contentDefault = null;
		// Method-level members honored for classic/NG parity (B-client-1).
		String baseUrl = "";
		String accept = "";
		String contentType = "";
		List<Map.Entry<String,String>> constantHeaders = ll();
		List<Map.Entry<String,String>> constantQueryData = ll();
		List<Map.Entry<String,String>> constantFormData = ll();
		String timeout = "";
		int retries = 0;
		boolean retryNonIdempotent = false;
		boolean throwOnError = false;
		int urlParamIndex = -1;
		static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

		@SuppressWarnings({
			"java:S3776" // Cognitive complexity acceptable for operation metadata construction
		})
		Builder(String parentPath, Method m, String defaultMethod) {

			var mi = MethodInfo.of(m);

			var al = rstream(AP.find(mi)).filter(REMOTE_OP_GROUP).toList();
			if (al.isEmpty())
				al = rstream(AP.find(mi.getReturnType().unwrap(Holder.class, Optional.class))).filter(REMOTE_OP_GROUP).toList();

		var httpMethodValue = Holder.<String>empty();
		var pathValue = Holder.<String>empty();
		al.stream().map(x -> ucr(x.getNameSimple().substring(6))).filter(x -> ! x.equals("OP")).forEach(httpMethodValue::set);
		al.forEach(ai -> ai.getValue(String.class, "method").filter(NOT_EMPTY).ifPresent(x -> httpMethodValue.set(ucr(x.trim()))));
		al.forEach(ai -> ai.getValue(String.class, "path").filter(NOT_EMPTY).ifPresent(x -> pathValue.set(x.trim())));
		httpMethod = httpMethodValue.orElse("").trim();
		path = pathValue.orElse("").trim();

			Holder<String> value = Holder.empty();
			al.stream().filter(x -> x.isType(RemoteOp.class) && ine(((RemoteOp)x.inner()).value().trim())).forEach(x -> value.set(((RemoteOp)x.inner()).value().trim()));

			if (value.isPresent()) {
				var v = value.get();
				var i = v.indexOf(' ');
				if (i == -1) {
					httpMethod = v;
				} else {
					httpMethod = v.substring(0, i).trim();
					path = v.substring(i).trim();
				}
			} else {
				al.stream().filter(x -> ! x.isType(RemoteOp.class) && ine(x.getValue(String.class, "value").filter(NOT_EMPTY).orElse("").trim()))
					.forEach(x -> value.set(x.getValue(String.class, "value").filter(NOT_EMPTY).get().trim()));
				if (value.isPresent())
					path = value.get();
			}

			if (path.isEmpty()) {
				path = HttpUtils.detectHttpPath(m, nie(httpMethod));
			}
			if (httpMethod.isEmpty())
				httpMethod = HttpUtils.detectHttpMethod(m, true, defaultMethod);

			path = trimSlashes(path);

			if (! isOneOf(httpMethod, "DELETE", "GET", "POST", "PUT", "OPTIONS", "HEAD", "CONNECT", "TRACE", "PATCH"))
				throw new RemoteMetadataException(m,
					"Invalid value specified for @RemoteOp(httpMethod) annotation: '" + httpMethod + "'.  Valid values are [DELETE,GET,POST,PUT,OPTIONS,HEAD,CONNECT,TRACE,PATCH].");

			methodReturn = new RemoteOperationReturn(mi);

			String fullPathValue;
			if (path.indexOf("://") != -1) {
				fullPathValue = path;
			} else if (parentPath.isEmpty()) {
				fullPathValue = urlEncodePath(path);
			} else {
				fullPathValue = trimSlashes(parentPath) + '/' + urlEncodePath(path);
			}
			fullPath = fullPathValue;

			mi.getParameters().forEach(x -> {
				// @Url parameter (call-time URL override) — captured for URL resolution, not bound as a part/body.
				if (AP.has(Url.class, x))
					urlParamIndex = x.getIndex();
				var rma = RemoteOperationArg.create(x);
				if (nn(rma)) {
					var pt = rma.getPartType();
					if (pt == HEADER)
						headerArgs.add(rma);
					else if (pt == QUERY)
						queryArgs.add(rma);
					else if (pt == FORMDATA)
						formDataArgs.add(rma);
					else if (pt == PATH)
						pathArgs.add(rma);
					else
						bodyArg = rma;
				}
				var rmba = RequestBeanMeta.create(x, AnnotationWorkList.create());
				if (nn(rmba)) {
					requestArgs.add(new RemoteOperationBeanArg(x.getIndex(), rmba));
				}
			});

			// Process method-level annotations for defaults (9.2.0)
			// Note: We need to handle both individual annotations and repeated annotation arrays
			processHeaderDefaults(mi, headerDefaults);
			processQueryDefaults(mi, queryDefaults);
			processFormDataDefaults(mi, formDataDefaults);
			processPathDefaults(mi, pathDefaults);
			processContentDefaults(mi);

			// Method-level parity members (B-client-1) — read generically across the @RemoteOp annotation group
			// (@RemoteOp/@RemoteGet/@RemotePost/...), mirroring the next-generation RrpcInterfaceMeta parsing.
			for (var ai : al) {
				ai.getValue(String.class, "baseUrl").filter(NOT_EMPTY).ifPresent(x -> baseUrl = resolve(x.trim()));
				ai.getValue(String.class, "accept").filter(NOT_EMPTY).ifPresent(x -> accept = resolve(x.trim()));
				ai.getValue(String.class, "contentType").filter(NOT_EMPTY).ifPresent(x -> contentType = resolve(x.trim()));
				ai.getValue(String.class, "timeout").filter(NOT_EMPTY).ifPresent(x -> timeout = x.trim());
				ai.getValue(int.class, "retries").filter(x -> x > 0).ifPresent(x -> retries = x);
				ai.getValue(boolean.class, "retryNonIdempotent").ifPresent(x -> retryNonIdempotent |= x);
				ai.getValue(boolean.class, "throwOnError").ifPresent(x -> throwOnError |= x);
				ai.getValue(String[].class, "headers").ifPresent(x -> constantHeaders.addAll(RemoteProxyUtils.parseConstantParts(x, ':')));
				ai.getValue(String[].class, "queryData").ifPresent(x -> constantQueryData.addAll(RemoteProxyUtils.parseConstantParts(x, '=')));
				ai.getValue(String[].class, "formData").ifPresent(x -> constantFormData.addAll(RemoteProxyUtils.parseConstantParts(x, '=')));
				// Genuinely engine-specific: classic cannot honor NG interceptors.  Warn once per interface.
				ai.getValue(Class[].class, "interceptors").filter(x -> x.length > 0).ifPresent(x ->
					RemoteProxyUtils.warnUnsupportedMember(m.getDeclaringClass(), "interceptors",
						"classic and next-generation RestCallInterceptor SPI types are nominally incompatible."));
			}
		}

		private static String resolve(String s) {
			return VarResolver.DEFAULT.resolve(s);
		}

		// Helper methods to process method-level annotations with defaults (9.2.0)
		// These handle both individual annotations and repeated annotation arrays

		private void processContentDefaults(MethodInfo mi) {
			// @formatter:off
			AP.find(Content.class, mi)
				.stream()
				.map(x -> x.inner().def())
				.filter(StringUtils::isNotBlank)
				.findFirst()
				.ifPresent(x -> contentDefault = x);
			// @formatter:on
		}

		private static void processFormDataDefaults(MethodInfo mi, Map<String,String> defaults) {
			// @formatter:off
			rstream(AP.find(FormData.class, mi))
				.map(AnnotationInfo::inner)
				.filter(x -> isAnyNotEmpty(x.name(), x.value()) && ine(x.def()))
				.forEach(x -> defaults.put(firstNonEmpty(x.name(), x.value()), x.def()));
			// @formatter:on
		}

		private static void processHeaderDefaults(MethodInfo mi, Map<String,String> defaults) {
			// @formatter:off
			rstream(AP.find(Header.class, mi))
				.map(AnnotationInfo::inner)
				.filter(x -> isAnyNotEmpty(x.name(), x.value()) && ine(x.def()))
				.forEach(x -> defaults.put(firstNonEmpty(x.name(), x.value()), x.def()));
			// @formatter:on
		}

		private static void processPathDefaults(MethodInfo mi, Map<String,String> defaults) {
			// @formatter:off
			rstream(AP.find(Path.class, mi))
				.map(AnnotationInfo::inner)
				.filter(x -> isAnyNotEmpty(x.name(), x.value()) && neq(NONE, x.def()))
				.forEach(x -> defaults.put(firstNonEmpty(x.name(), x.value()), x.def()));
			// @formatter:on
		}

		private static void processQueryDefaults(MethodInfo mi, Map<String,String> defaults) {
			// @formatter:off
			rstream(AP.find(Query.class, mi))
				.map(AnnotationInfo::inner)
				.filter(x -> isAnyNotEmpty(x.name(), x.value()) && ine(x.def()))
				.forEach(x -> defaults.put(firstNonEmpty(x.name(), x.value()), x.def()));
			// @formatter:on
		}
	}

	private final String httpMethod;
	private final String fullPath;
	private final RemoteOperationArg[] pathArgs;
	private final RemoteOperationArg[] queryArgs;
	private final RemoteOperationArg[] headerArgs;
	private final RemoteOperationArg[] formDataArgs;
	private final RemoteOperationBeanArg[] requestArgs;
	private final RemoteOperationArg contentArg;
	private final RemoteOperationReturn methodReturn;

	private final Class<?>[] exceptions;
	// Method-level annotations with defaults (9.2.0)
	private final Map<String,String> pathDefaults;
	private final Map<String,String> queryDefaults;
	private final Map<String,String> headerDefaults;
	private final Map<String,String> formDataDefaults;

	private final String contentDefault;

	// Method-level members honored for classic/NG parity (B-client-1).
	private final String baseUrl;
	private final String accept;
	private final String contentType;
	private final List<Map.Entry<String,String>> constantHeaders;
	private final List<Map.Entry<String,String>> constantQueryData;
	private final List<Map.Entry<String,String>> constantFormData;
	private final String timeout;
	private final int retries;
	private final boolean retryNonIdempotent;
	private final boolean throwOnError;
	private final int urlParamIndex;

	/**
	 * Constructor.
	 *
	 * @param parentPath The absolute URI of the REST interface backing the interface proxy.
	 * @param m The Java method.
	 * @param defaultMethod The default HTTP method if not specified through annotation.
	 */
	public RemoteOperationMeta(String parentPath, Method m, String defaultMethod) {
		var b = new Builder(parentPath, m, defaultMethod);
		httpMethod = b.httpMethod;
		fullPath = b.fullPath;
		pathArgs = b.pathArgs.toArray(new RemoteOperationArg[b.pathArgs.size()]);
		queryArgs = b.queryArgs.toArray(new RemoteOperationArg[b.queryArgs.size()]);
		formDataArgs = b.formDataArgs.toArray(new RemoteOperationArg[b.formDataArgs.size()]);
		headerArgs = b.headerArgs.toArray(new RemoteOperationArg[b.headerArgs.size()]);
		requestArgs = b.requestArgs.toArray(new RemoteOperationBeanArg[b.requestArgs.size()]);
		contentArg = b.bodyArg;
		methodReturn = b.methodReturn;
		exceptions = m.getExceptionTypes();
		pathDefaults = u(b.pathDefaults);
		queryDefaults = u(b.queryDefaults);
		headerDefaults = u(b.headerDefaults);
		formDataDefaults = u(b.formDataDefaults);
		contentDefault = b.contentDefault;
		baseUrl = b.baseUrl;
		accept = b.accept;
		contentType = b.contentType;
		constantHeaders = u(b.constantHeaders);
		constantQueryData = u(b.constantQueryData);
		constantFormData = u(b.constantFormData);
		timeout = b.timeout;
		retries = b.retries;
		retryNonIdempotent = b.retryNonIdempotent;
		throwOnError = b.throwOnError;
		urlParamIndex = b.urlParamIndex;
	}

	/**
	 * Performs an action on the exceptions thrown by this method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachException(Consumer<Class<?>> action) {
		for (var e : exceptions)
			action.accept(e);
		return this;
	}

	/**
	 * Performs an action on the {@link FormData @FormData} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachFormDataArg(Consumer<RemoteOperationArg> action) {
		for (var a : formDataArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Header @Header} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachHeaderArg(Consumer<RemoteOperationArg> action) {
		for (var a : headerArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Path @Path} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachPathArg(Consumer<RemoteOperationArg> action) {
		for (var a : pathArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Query @Query} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachQueryArg(Consumer<RemoteOperationArg> action) {
		for (var a : queryArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Performs an action on the {@link Request @Request} annotated arguments on this Java method.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public RemoteOperationMeta forEachRequestArg(Consumer<RemoteOperationBeanArg> action) {
		for (var a : requestArgs)
			action.accept(a);
		return this;
	}

	/**
	 * Returns the argument annotated with {@link Content @Content}.
	 *
	 * @return A index of the argument with the {@link Content @Content} annotation, or <jk>null</jk> if no argument exists.
	 */
	public RemoteOperationArg getContentArg() { return contentArg; }

	/**
	 * Returns the default value for a {@link Content @Content} annotation on the method.
	 *
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getContentDefault() { return contentDefault; }

	/**
	 * Returns the default value for a {@link FormData @FormData} annotation on the method.
	 *
	 * @param name The form data parameter name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getFormDataDefault(String name) {
		return formDataDefaults.get(name);
	}

	/**
	 * Returns the absolute URI of the REST interface invoked by this Java method.
	 *
	 * @return The absolute URI of the REST interface, never <jk>null</jk>.
	 */
	public String getFullPath() { return fullPath; }

	/**
	 * Returns the default value for a {@link Header @Header} annotation on the method.
	 *
	 * @param name The header name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getHeaderDefault(String name) {
		return headerDefaults.get(name);
	}

	/**
	 * Returns the value of the {@link RemoteOp#method() @RemoteOp(method)} annotation on this Java method.
	 *
	 * @return The value of the annotation, never <jk>null</jk>.
	 */
	public String getHttpMethod() { return httpMethod; }

	/**
	 * Returns the default value for a {@link Path @Path} annotation on the method.
	 *
	 * @param name The path parameter name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getPathDefault(String name) {
		return pathDefaults.get(name);
	}

	/**
	 * Returns the default value for a {@link Query @Query} annotation on the method.
	 *
	 * @param name The query parameter name.
	 * @return The default value, or <jk>null</jk> if not specified.
	 * @since 9.2.0
	 */
	public String getQueryDefault(String name) {
		return queryDefaults.get(name);
	}

	/**
	 * Returns whether the method returns the HTTP response body or status code.
	 *
	 * @return Whether the method returns the HTTP response body or status code.
	 */
	public RemoteOperationReturn getReturns() { return methodReturn; }

	/**
	 * Returns the method-level base/host override from the {@code baseUrl} annotation member.
	 *
	 * @return The base/host override. Never <jk>null</jk>, but may be empty.
	 */
	public String getBaseUrl() { return baseUrl; }

	/**
	 * Returns the method-level default {@code Accept} media type from the {@code accept} annotation member.
	 *
	 * @return The accept media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getAccept() { return accept; }

	/**
	 * Returns the method-level default {@code Content-Type} media type from the {@code contentType} annotation member.
	 *
	 * @return The content-type media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getContentType() { return contentType; }

	/**
	 * Returns the method-level constant headers from the {@code headers} annotation member.
	 *
	 * @return An unmodifiable list of name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getConstantHeaders() { return constantHeaders; }

	/**
	 * Returns the method-level constant query parameters from the {@code queryData} annotation member.
	 *
	 * @return An unmodifiable list of name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getConstantQueryData() { return constantQueryData; }

	/**
	 * Returns the method-level constant form-data parameters from the {@code formData} annotation member.
	 *
	 * @return An unmodifiable list of name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getConstantFormData() { return constantFormData; }

	/**
	 * Returns the method-level per-call timeout duration string from the {@code timeout} annotation member.
	 *
	 * @return The timeout duration string. Never <jk>null</jk>, but may be empty.
	 */
	public String getTimeout() { return timeout; }

	/**
	 * Returns the method-level maximum retry attempts from the {@code retries} annotation member.
	 *
	 * @return The retry count.
	 */
	public int getRetries() { return retries; }

	/**
	 * Returns whether the method opts non-idempotent verbs into automatic retries ({@code retryNonIdempotent} member).
	 *
	 * @return <jk>true</jk> if non-idempotent retries are opted in at the method level.
	 */
	public boolean isRetryNonIdempotent() { return retryNonIdempotent; }

	/**
	 * Returns whether the method throws a generic exception on an unmatched error response ({@code throwOnError} member).
	 *
	 * @return <jk>true</jk> if {@code throwOnError} is set at the method level.
	 */
	public boolean isThrowOnError() { return throwOnError; }

	/**
	 * Returns the index of the {@link org.apache.juneau.http.Url @Url} parameter, or {@code -1} if none.
	 *
	 * @return The {@code @Url} parameter index, or {@code -1}.
	 */
	public int getUrlParamIndex() { return urlParamIndex; }
}