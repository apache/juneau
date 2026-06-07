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
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.remote.*;

/**
 * Next-generation transport-agnostic REST client.
 *
 * <p>
 * Create instances via {@link #create()} or {@link #builder()}:
 * <p class='bjava'>
 * 	<jc>// Minimal — uses the built-in JDK HttpClient transport</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>create</jsm>();
 *
 * 	<jc>// Explicit transport (e.g. for tests)</jc>
 * 	RestClient <jv>client</jv> = RestClient.<jsm>builder</jsm>()
 * 		.transport(<jv>mockTransport</jv>)
 * 		.header(<js>"Accept"</js>, <js>"application/json"</js>)
 * 		.build();
 * </p>
 *
 * <p>
 * By default, {@code RestClient} uses {@link JavaHttpTransport} (backed by the JDK's
 * {@link java.net.http.HttpClient}), which ships built into the {@code juneau-rest-client} artifact and
 * requires no extra dependencies on Java 11+.  Pulling in one of the optional transport modules
 * ({@code juneau-rest-client-apache-httpclient-45}, {@code -apache-httpclient-50}, {@code -okhttp},
 * {@code -jetty}) registers a higher-priority provider via {@link java.util.ServiceLoader} that takes
 * over automatically.  An explicit {@link Builder#transport(HttpTransport)} call always wins.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // transport is closed in RestClient.close(); this class owns it
})
public final class RestClient implements Closeable {

	/**
	 * Default body converters applied in order when {@link RestRequest#body(Object)} is called.
	 *
	 * <p>
	 * Custom converters registered via {@link Builder#bodyConverter(BodyConverter[])} are prepended to this list.
	 * Call {@link Builder#bodyConverters(BodyConverter[])} to replace all defaults.
	 */
	public static final List<BodyConverter<?>> DEFAULT_BODY_CONVERTERS = List.of(
		BodyConverter.of(HttpBody.class, TransportBody::of),
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

	private RestClient(Builder builder) {
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
		if (providers.isEmpty())
			return JavaHttpTransport.create();  // defensive fallback if META-INF/services was stripped (e.g. uber-jar shading)
		providers.sort(Comparator.comparingInt(HttpTransportProvider::getPriority));
		return providers.get(0).create();
	}

	/**
	 * Creates a new {@link RestClient} using auto-discovered transport.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static RestClient create() {
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
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest get(String url) {
		return request("GET", url);
	}

	/**
	 * Creates a POST request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest post(String url) {
		return request("POST", url);
	}

	/**
	 * Creates a PUT request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest put(String url) {
		return request("PUT", url);
	}

	/**
	 * Creates a PATCH request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest patch(String url) {
		return request("PATCH", url);
	}

	/**
	 * Creates a DELETE request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest delete(String url) {
		return request("DELETE", url);
	}

	/**
	 * Creates a HEAD request to the given URL.
	 *
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest head(String url) {
		return request("HEAD", url);
	}

	/**
	 * Creates a request with the given HTTP method and URL.
	 *
	 * @param method The HTTP method. Must not be <jk>null</jk>.
	 * @param url The target URL. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest request(String method, String url) {
		var resolvedUrl = rootUrl != null && !url.contains("://") ? rootUrl + url : url;
		return new RestRequest(this, method, resolvedUrl);
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
	 * Creates a Java proxy for the given {@link Remote}-annotated interface.
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
	 * @param iface The interface class. Must be annotated with {@link Remote}. Must not be <jk>null</jk>.
	 * @return A proxy instance backed by this client. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code iface} is not an interface or not annotated with {@code @Remote}.
	 */
	public <T> T remote(Class<T> iface) {
		return new RemoteClient(this).create(iface);
	}

	@Override /* Closeable */
	public void close() throws IOException {
		transport.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link RestClient}.
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
		 * If not set, {@link RestClient} discovers a transport via {@link java.util.ServiceLoader}.  When no
		 * sibling transport module is on the classpath, the built-in {@link JavaHttpTransport} (backed by the JDK's
		 * {@link java.net.http.HttpClient}) is used as the default.
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
			defaultHeaders.add(HttpHeaderBean.of(name, value));
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
			defaultHeaders.add(HttpHeaderBean.of(name, value));
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
		 * Custom converters are checked before the defaults when {@link RestRequest#body(Object)} is called.
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
			bodyConverters = list(value);
			return this;
		}

		/**
		 * Builds and returns the {@link RestClient}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public RestClient build() {
			return new RestClient(this);
		}
	}
}
