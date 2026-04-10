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
package org.apache.juneau.ng.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.ng.http.*;
import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.part.*;
import org.apache.juneau.ng.rest.client.remote.*;

/**
 * Next-generation transport-agnostic REST client.
 *
 * <p>
 * Create instances via {@link #create()} or {@link #builder()}:
 * <p class='bjava'>
 * 	<jc>// Minimal — auto-discovers a transport via ServiceLoader</jc>
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>create</jsm>();
 *
 * 	<jc>// Explicit transport (e.g. for tests)</jc>
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>mockTransport</jv>)
 * 		.header(<js>"Accept"</js>, <js>"application/json"</js>)
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // transport is closed in NgRestClient.close(); this class owns it
})
public final class NgRestClient implements Closeable {

	/**
	 * Default body converters applied in order when {@link NgRestRequest#body(Object)} is called.
	 *
	 * <p>
	 * Custom converters registered via {@link Builder#bodyConverter(BodyConverter[])} are prepended to this list.
	 * Call {@link Builder#bodyConverters(BodyConverter[])} to replace all defaults.
	 */
	public static final List<BodyConverter<?>> DEFAULT_BODY_CONVERTERS = List.of(
		BodyConverter.of(HttpBody.class, body -> TransportBody.of(body)),
		BodyConverter.of(InputStream.class, is -> TransportBody.of(StreamBody.of(is))),
		BodyConverter.of(byte[].class, bytes -> TransportBody.of(ByteArrayBody.of(bytes))),
		BodyConverter.of(java.io.File.class, file -> TransportBody.of(FileBody.of(file)))
	);

	final HttpTransport transport;
	final List<HttpHeader> defaultHeaders;
	final List<HttpPart> defaultQueryData;
	final String rootUrl;
	final List<RestCallInterceptor> interceptors;
	final RestLogger logger;
	final List<BodyConverter<?>> bodyConverters;

	private NgRestClient(Builder builder) {
		this.transport = assertArgNotNull("transport",
			builder.transport != null ? builder.transport : discoverTransport());
		this.defaultHeaders = List.copyOf(builder.defaultHeaders);
		this.defaultQueryData = List.copyOf(builder.defaultQueryData);
		this.rootUrl = builder.rootUrl;
		this.interceptors = List.copyOf(builder.interceptors);
		this.logger = builder.logger;
		this.bodyConverters = List.copyOf(builder.bodyConverters);
	}

	private static HttpTransport discoverTransport() {
		var providers = new ArrayList<HttpTransportProvider>();
		for (var p : ServiceLoader.load(HttpTransportProvider.class))
			if (p.isAvailable())
				providers.add(p);
		if (providers.isEmpty()) // HTT: requires test classpath with no transport modules present
			return null;
		providers.sort(Comparator.comparingInt(HttpTransportProvider::getPriority));
		return providers.get(0).create();
	}

	/**
	 * Creates a new {@link NgRestClient} using auto-discovered transport.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static NgRestClient create() {
		return builder().build();
	}

	/** Returns a new {@link Builder}. */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a GET request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest get(String url) {
		return request("GET", url);
	}

	/**
	 * Creates a POST request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest post(String url) {
		return request("POST", url);
	}

	/**
	 * Creates a PUT request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest put(String url) {
		return request("PUT", url);
	}

	/**
	 * Creates a PATCH request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest patch(String url) {
		return request("PATCH", url);
	}

	/**
	 * Creates a DELETE request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest delete(String url) {
		return request("DELETE", url);
	}

	/**
	 * Creates a HEAD request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest head(String url) {
		return request("HEAD", url);
	}

	/**
	 * Creates a request with the given HTTP method and URL.
	 *
	 * @param method The HTTP method. Must not be <jk>null</jk>.
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link NgRestRequest}. Never <jk>null</jk>.
	 */
	public NgRestRequest request(String method, String url) {
		var resolvedUrl = rootUrl != null && !url.contains("://") ? rootUrl + url : url;
		return new NgRestRequest(this, method, resolvedUrl);
	}

	/**
	 * Returns the underlying transport used by this client.
	 *
	 * @return The transport. Never <jk>null</jk>.
	 */
	public HttpTransport getTransport() {
		return transport;
	}

	/**
	 * Creates a Java proxy for the given {@link org.apache.juneau.ng.http.remote.Remote}-annotated interface.
	 *
	 * <p>
	 * Each method call on the returned proxy will be translated into an HTTP request using this client.
	 *
	 * <p class='bjava'>
	 * 	UserService <jv>svc</jv> = client.remote(UserService.<jk>class</jk>);
	 * 	String <jv>user</jv> = <jv>svc</jv>.getUser(<js>"42"</js>);
	 * </p>
	 *
	 * @param <T> The interface type.
	 * @param iface The interface class. Must be annotated with {@link org.apache.juneau.ng.http.remote.Remote}. Must not be <jk>null</jk>.
	 * @return A proxy instance backed by this client. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code iface} is not an interface or not annotated with {@code @Remote}.
	 */
	public <T> T remote(Class<T> iface) {
		return new NgRemoteClient(this).create(iface);
	}

	@Override /* Closeable */
	public void close() throws IOException {
		transport.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link NgRestClient}.
	 *
	 * <p>
	 * <b>Beta — API subject to change.</b>
	 *
	 * @since 9.2.1
	 */
	public static final class Builder {

		HttpTransport transport;
		final List<HttpHeader> defaultHeaders = new ArrayList<>();
		final List<HttpPart> defaultQueryData = new ArrayList<>();
		String rootUrl;
		final List<RestCallInterceptor> interceptors = new ArrayList<>();
		RestLogger logger;
		List<BodyConverter<?>> bodyConverters = new ArrayList<>(DEFAULT_BODY_CONVERTERS);

		private Builder() {}

		/**
		 * Sets the HTTP transport to use.
		 *
		 * <p>
		 * If not set, {@link NgRestClient} will discover a transport via {@link java.util.ServiceLoader}.
		 *
		 * @param value The transport. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder transport(HttpTransport value) {
			transport = value;
			return this;
		}

		/**
		 * Sets a root URL prefix applied to all relative request URLs (those without {@code ://}).
		 *
		 * @param value The root URL (e.g. {@code "https://api.example.com/v1"}). May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder rootUrl(String value) {
			rootUrl = value;
			return this;
		}

		/**
		 * Adds default headers sent with every request.
		 *
		 * @param value The headers to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder headers(HttpHeader... value) {
			defaultHeaders.addAll(Arrays.asList(value));
			return this;
		}

		/**
		 * Adds a default header sent with every request.
		 *
		 * @param name The header name. Must not be <jk>null</jk>.
		 * @param value The header value (eager). May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder header(String name, String value) {
			defaultHeaders.add(org.apache.juneau.ng.http.header.HttpHeaderBean.of(name, value));
			return this;
		}

		/**
		 * Adds a default header with a lazy value, evaluated at each request.
		 *
		 * @param name The header name. Must not be <jk>null</jk>.
		 * @param value Supplier for the header value. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder header(String name, Supplier<String> value) {
			defaultHeaders.add(org.apache.juneau.ng.http.header.HttpHeaderBean.of(name, value));
			return this;
		}

		/**
		 * Adds default query parameters appended to every request URL.
		 *
		 * @param value The parts to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder queryData(HttpPart... value) {
			defaultQueryData.addAll(Arrays.asList(value));
			return this;
		}

		/**
		 * Adds a default query parameter appended to every request URL.
		 *
		 * @param name The parameter name. Must not be <jk>null</jk>.
		 * @param value The parameter value (eager). May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder queryData(String name, String value) {
			defaultQueryData.add(HttpPartBean.of(name, value));
			return this;
		}

		/**
		 * Adds one or more lifecycle interceptors called before/after each request.
		 *
		 * @param value The interceptors to add. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder interceptors(RestCallInterceptor... value) {
			interceptors.addAll(Arrays.asList(value));
			return this;
		}

		/**
		 * Sets the logger called at the end of every request (success or failure).
		 *
		 * @param value The logger. May be <jk>null</jk> to disable logging.
		 * @return This object.
		 */
		public Builder logger(RestLogger value) {
			logger = value;
			return this;
		}

		/**
		 * Prepends custom body converters to the default converter list.
		 *
		 * <p>
		 * Custom converters are checked before the defaults when {@link NgRestRequest#body(Object)} is called.
		 *
		 * @param value The converters to prepend. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder bodyConverter(BodyConverter<?>... value) {
			var prepended = new ArrayList<BodyConverter<?>>(Arrays.asList(value));
			prepended.addAll(bodyConverters);
			bodyConverters = prepended;
			return this;
		}

		/**
		 * Replaces the entire body converter list (including defaults).
		 *
		 * <p>
		 * Use this when you want full control over body conversion, including disabling the built-in defaults.
		 *
		 * @param value The complete replacement converter list. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder bodyConverters(BodyConverter<?>... value) {
			bodyConverters = new ArrayList<>(Arrays.asList(value));
			return this;
		}

		/**
		 * Builds and returns the {@link NgRestClient}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public NgRestClient build() {
			return new NgRestClient(this);
		}
	}
}
