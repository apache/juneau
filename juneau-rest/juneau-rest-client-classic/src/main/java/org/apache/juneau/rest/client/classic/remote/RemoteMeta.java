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

import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.remote.*;

/**
 * Contains the meta-data about a REST proxy class.
 *
 * <p>
 * Captures the information in {@link Remote @Remote} and {@link RemoteOp @RemoteOp} annotations for
 * caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClient">juneau-rest-client Basics</a>
 * </ul>
 */
public class RemoteMeta {

	private static String resolve(String s) {
		return VarResolver.DEFAULT.resolve(s);
	}

	private final Map<Method,RemoteOperationMeta> operations;

	private final HeaderList headers;

	// Interface-level members honored for classic/NG parity (B-client-1).
	private final String baseUrl;
	private final String accept;
	private final String contentType;
	private final List<Map.Entry<String,String>> queryData;
	private final List<Map.Entry<String,String>> formData;
	private final String timeout;
	private final int retries;
	private final boolean retryNonIdempotent;
	private final boolean throwOnError;

	/**
	 * Constructor.
	 *
	 * @param c The interface class annotated with a {@link Remote @Remote} annotation (optional).
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for remote interface metadata construction dispatch
	})
	public RemoteMeta(Class<?> c) {
		var path = "";

		var ci = ClassInfo.of(c);
		var remotes = rstream(ci.getAnnotations(Remote.class).toList()).map(AnnotationInfo::inner).toList();

		var versionHeader = "Client-Version";
		String clientVersion = null;
		var headers2 = HeaderList.create().resolving();

		var baseUrl2 = "";
		var accept2 = "";
		var contentType2 = "";
		var queryData2 = new ArrayList<Map.Entry<String,String>>();
		var formData2 = new ArrayList<Map.Entry<String,String>>();
		var timeout2 = "";
		var retries2 = 0;
		var retryNonIdempotent2 = false;
		var throwOnError2 = false;

		for (var r : remotes) {
			if (ine(r.path()))
				path = trimSlashes(resolve(r.path()));
			else if (ine(r.value()))
				path = trimSlashes(resolve(r.value()));
			for (var h : r.headers())
				headers2.append(stringHeader(resolve(h)));
			if (ine(r.version()))
				clientVersion = resolve(r.version());
			if (ine(r.versionHeader()))
				versionHeader = resolve(r.versionHeader());
			if (isNotVoid(r.headerList()) && HeaderList.class.isAssignableFrom(r.headerList())) {
				try {
					headers2.append(((HeaderList) r.headerList().getDeclaredConstructor().newInstance()).getAll());
				} catch (Exception e) {
					throw rex(e, "Could not instantiate HeaderSupplier class");
				}
			}
			if (ine(r.baseUrl()))
				baseUrl2 = resolve(r.baseUrl());
			if (ine(r.accept()))
				accept2 = resolve(r.accept());
			if (ine(r.contentType()))
				contentType2 = resolve(r.contentType());
			queryData2.addAll(RemoteProxyUtils.parseConstantParts(r.queryData(), '='));
			formData2.addAll(RemoteProxyUtils.parseConstantParts(r.formData(), '='));
			if (ine(r.timeout()))
				timeout2 = r.timeout();
			if (r.retries() > 0)
				retries2 = r.retries();
			retryNonIdempotent2 |= r.retryNonIdempotent();
			throwOnError2 |= r.throwOnError();
			// Genuinely engine-specific: classic and NG RestCallInterceptor SPI types are nominally
			// incompatible, so the classic engine cannot honor interface-level interceptors.  Warn once.
			if (r.interceptors().length > 0)
				RemoteProxyUtils.warnUnsupportedMember(c, "interceptors",
					"classic and next-generation RestCallInterceptor SPI types are nominally incompatible.");
		}

		if (nn(clientVersion))
			headers2.append(stringHeader(versionHeader, clientVersion));

		Map<Method,RemoteOperationMeta> operations2 = map();
		var path2 = path;
		ci.getPublicMethods().forEach(x -> operations2.put(x.inner(), new RemoteOperationMeta(path2, x.inner(), "GET")));

		this.operations = u(operations2);
		this.headers = headers2.unmodifiable();
		this.baseUrl = baseUrl2;
		this.accept = accept2;
		this.contentType = contentType2;
		this.queryData = u(queryData2);
		this.formData = u(formData2);
		this.timeout = timeout2;
		this.retries = retries2;
		this.retryNonIdempotent = retryNonIdempotent2;
		this.throwOnError = throwOnError2;
	}

	/**
	 * Returns the headers to set on all requests.
	 *
	 * @return The headers to set on all requests.
	 */
	public HeaderList getHeaders() { return headers; }

	/**
	 * Returns the interface-level base/host override from {@link Remote#baseUrl()}.
	 *
	 * @return The base/host override. Never <jk>null</jk>, but may be empty.
	 */
	public String getBaseUrl() { return baseUrl; }

	/**
	 * Returns the interface-level default {@code Accept} media type from {@link Remote#accept()}.
	 *
	 * @return The accept media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getAccept() { return accept; }

	/**
	 * Returns the interface-level default {@code Content-Type} media type from {@link Remote#contentType()}.
	 *
	 * @return The content-type media type. Never <jk>null</jk>, but may be empty.
	 */
	public String getContentType() { return contentType; }

	/**
	 * Returns the interface-level constant query parameters from {@link Remote#queryData()}.
	 *
	 * @return An unmodifiable list of name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getQueryData() { return queryData; }

	/**
	 * Returns the interface-level constant form-data parameters from {@link Remote#formData()}.
	 *
	 * @return An unmodifiable list of name/value entries. Never <jk>null</jk>, but may be empty.
	 */
	public List<Map.Entry<String,String>> getFormData() { return formData; }

	/**
	 * Returns the interface-level default per-call timeout duration string from {@link Remote#timeout()}.
	 *
	 * @return The timeout duration string. Never <jk>null</jk>, but may be empty.
	 */
	public String getTimeout() { return timeout; }

	/**
	 * Returns the interface-level default maximum retry attempts from {@link Remote#retries()}.
	 *
	 * @return The retry count.
	 */
	public int getRetries() { return retries; }

	/**
	 * Returns whether the interface opts non-idempotent verbs into automatic retries ({@link Remote#retryNonIdempotent()}).
	 *
	 * @return <jk>true</jk> if non-idempotent retries are opted in at the interface level.
	 */
	public boolean isRetryNonIdempotent() { return retryNonIdempotent; }

	/**
	 * Returns whether the interface throws a generic exception on an unmatched error response ({@link Remote#throwOnError()}).
	 *
	 * @return <jk>true</jk> if {@code throwOnError} is set at the interface level.
	 */
	public boolean isThrowOnError() { return throwOnError; }

	/**
	 * Returns the metadata about the specified operation on this resource proxy.
	 *
	 * @param m The method to look up.
	 * @return Metadata about the method or <jk>null</jk> if no metadata was found.
	 */
	public RemoteOperationMeta getOperationMeta(Method m) {
		return operations.get(m);
	}
}