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
package org.apache.juneau.rest.client.classic;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;

/**
 * The settings of an Apache {@link HttpClientBuilder} that it applies only to the connection manager it builds itself.
 *
 * <p>
 * {@link RestClient} always gives the {@code HttpClientBuilder} a connection manager of its own, so the builder ignores
 * these settings.  {@code HttpClientBuilder} has no getters, so they are read from its fields, which are stable across
 * the HttpClient 4.5.x line.
 *
 * @param sslContext The SSL context, or <jk>null</jk>.
 * @param sslSocketFactory The SSL socket factory, or <jk>null</jk>.
 * @param hostnameVerifier The hostname verifier, or <jk>null</jk>.
 * @param publicSuffixMatcher The public suffix matcher, or <jk>null</jk>.
 * @param dnsResolver The DNS resolver, or <jk>null</jk>.
 * @param socketConfig The default socket configuration, or <jk>null</jk>.
 * @param connectionConfig The default connection configuration, or <jk>null</jk>.
 * @param maxConnTotal The maximum total connections, or {@code 0} if not set.
 * @param maxConnPerRoute The maximum connections per route, or {@code 0} if not set.
 * @param connTimeToLive The connection time-to-live, or a value {@code <= 0} if not set.
 * @param connTimeToLiveTimeUnit The time unit of {@code connTimeToLive}.
 * @param systemProperties Whether {@link HttpClientBuilder#useSystemProperties()} was called.
 */
record HttpClientBuilderSettings(
	SSLContext sslContext,
	LayeredConnectionSocketFactory sslSocketFactory,
	HostnameVerifier hostnameVerifier,
	PublicSuffixMatcher publicSuffixMatcher,
	DnsResolver dnsResolver,
	SocketConfig socketConfig,
	ConnectionConfig connectionConfig,
	int maxConnTotal,
	int maxConnPerRoute,
	long connTimeToLive,
	TimeUnit connTimeToLiveTimeUnit,
	boolean systemProperties
) {

	private static final String[] FIELD_NAMES = { "sslContext", "sslSocketFactory", "hostnameVerifier", "publicSuffixMatcher",
		"dnsResolver", "defaultSocketConfig", "defaultConnectionConfig", "maxConnTotal", "maxConnPerRoute", "connTimeToLive",
		"connTimeToLiveTimeUnit", "systemProperties", "requestLast", "requestExec", "httpprocessor",
		"redirectStrategy" };

	// The fields of HttpClientBuilder, or null if they cannot be read (FIELD_ERROR then says why).
	private static final Map<String,Field> FIELDS;
	private static final Exception FIELD_ERROR;

	static {
		Map<String,Field> fields = null;
		Exception error = null;
		try {
			var m = new HashMap<String,Field>();
			for (var name : FIELD_NAMES) {
				var f = HttpClientBuilder.class.getDeclaredField(name);
				f.setAccessible(true);
				m.put(name, f);
			}
			fields = u(m);
		} catch (NoSuchFieldException | RuntimeException e) {
			// HTT: only an HttpClient release without these fields, or a JVM that denies reflective access to them, gets here.
			error = e;
		}
		FIELDS = fields;
		FIELD_ERROR = error;
	}

	/**
	 * Reads the settings of the given builder.
	 *
	 * @param b The builder.
	 * @return Its settings.
	 * @throws IllegalStateException If the fields of {@link HttpClientBuilder} cannot be read: the settings would then be
	 * 	silently ignored, which for TLS settings could widen the trust a caller asked for.
	 */
	static HttpClientBuilderSettings of(HttpClientBuilder b) {
		if (FIELDS == null)
			// HTT: see the static initializer.  A new exception each time, so that each carries the caller's stack.
			throw isex(FIELD_ERROR, "Cannot read the TLS and connection settings of %s (%s), so they cannot be applied to the "
				+ "connection manager RestClient builds.  Use an HttpClient 4.5.x release whose HttpClientBuilder has these "
				+ "fields, or supply the connection manager yourself with connectionManager(...).", HttpClientBuilder.class.getName(), FIELD_ERROR);
		return new HttpClientBuilderSettings(
			get(b, "sslContext"),
			get(b, "sslSocketFactory"),
			get(b, "hostnameVerifier"),
			get(b, "publicSuffixMatcher"),
			get(b, "dnsResolver"),
			get(b, "defaultSocketConfig"),
			get(b, "defaultConnectionConfig"),
			(Integer)get(b, "maxConnTotal"),
			(Integer)get(b, "maxConnPerRoute"),
			(Long)get(b, "connTimeToLive"),
			get(b, "connTimeToLiveTimeUnit"),
			(Boolean)get(b, "systemProperties"));
	}

	/**
	 * Returns whether the builder already has a request interceptor of the given type (added with
	 * {@link HttpClientBuilder#addInterceptorLast(org.apache.http.HttpRequestInterceptor) addInterceptorLast}).
	 *
	 * @param b The builder.
	 * @param type The interceptor type.
	 * @return <jk>true</jk> if it has one, <jk>false</jk> if not or if the builder's fields cannot be read.
	 */
	static boolean hasLastRequestInterceptor(HttpClientBuilder b, Class<?> type) {
		if (FIELDS == null)
			return false;
		List<?> interceptors = get(b, "requestLast");
		return nn(interceptors) && interceptors.stream().anyMatch(type::isInstance);
	}

	/**
	 * Returns whether the fields of {@link HttpClientBuilder} can be read.
	 *
	 * @return <jk>false</jk> for an HttpClient release without them, or a JVM that denies reflective access to them.
	 */
	static boolean isReadable() {
		return FIELDS != null;
	}

	/**
	 * Returns the request executor set on the builder with
	 * {@link HttpClientBuilder#setRequestExecutor(HttpRequestExecutor) setRequestExecutor}.
	 *
	 * @param b The builder.
	 * @return The request executor, or <jk>null</jk> if none is set.
	 * @throws IllegalStateException If the builder's fields cannot be read (see {@link #isReadable()}): replacing an
	 * 	executor that could not be read would silently drop the caller's.
	 */
	static HttpRequestExecutor requestExecutor(HttpClientBuilder b) {
		return get(requireReadable(b), "requestExec");
	}

	/**
	 * Returns whether an {@link HttpProcessor} was set on the builder with
	 * {@link HttpClientBuilder#setHttpProcessor(HttpProcessor) setHttpProcessor}, which replaces all of the client's
	 * request and response interceptors.
	 *
	 * @param b The builder.
	 * @return <jk>true</jk> if one is set.
	 * @throws IllegalStateException If the builder's fields cannot be read (see {@link #isReadable()}).
	 */
	static boolean hasHttpProcessor(HttpClientBuilder b) {
		return nn(get(requireReadable(b), "httpprocessor"));
	}

	/**
	 * Returns the request interceptors added to the builder with
	 * {@link HttpClientBuilder#addInterceptorLast(org.apache.http.HttpRequestInterceptor) addInterceptorLast}, in order.
	 *
	 * @param b The builder.
	 * @return The interceptors (empty if none were added).
	 * @throws IllegalStateException If the builder's fields cannot be read (see {@link #isReadable()}).
	 */
	static List<?> lastRequestInterceptors(HttpClientBuilder b) {
		List<?> interceptors = get(requireReadable(b), "requestLast");
		return interceptors == null ? List.of() : interceptors;
	}

	/**
	 * Returns the redirect strategy set on the builder with
	 * {@link HttpClientBuilder#setRedirectStrategy(org.apache.http.client.RedirectStrategy) setRedirectStrategy}.
	 *
	 * @param b The builder.
	 * @return The redirect strategy, or <jk>null</jk> if none is set.
	 * @throws IllegalStateException If the builder's fields cannot be read (see {@link #isReadable()}).
	 */
	static Object redirectStrategy(HttpClientBuilder b) {
		return get(requireReadable(b), "redirectStrategy");
	}

	private static HttpClientBuilder requireReadable(HttpClientBuilder b) {
		if (FIELDS == null)
			// HTT: see the static initializer.
			throw isex(FIELD_ERROR, "Cannot read the fields of %s (%s).", HttpClientBuilder.class.getName(), FIELD_ERROR);
		return b;
	}

	@SuppressWarnings({
		"unchecked" // The caller names the field, and so its type.
	})
	private static <T> T get(HttpClientBuilder b, String name) {
		try {
			return (T)FIELDS.get(name).get(b);
		} catch (IllegalAccessException e) {
			// HTT: unreachable, since every field was made accessible when it was looked up.
			throw isex(e, "Cannot read %s.%s", HttpClientBuilder.class.getName(), name);
		}
	}
}
