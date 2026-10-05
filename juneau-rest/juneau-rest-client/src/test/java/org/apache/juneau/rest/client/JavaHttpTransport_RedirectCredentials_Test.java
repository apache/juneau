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

import static org.apache.juneau.test.bct.BctAssertions.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

import org.junit.jupiter.api.*;

/**
 * Verifies that {@link JavaHttpTransport}'s policy-covered redirect loop does not forward caller-set credential
 * headers to a different origin, while still forwarding them on a same-origin redirect.
 *
 * <p>
 * Uses a stub JDK {@link HttpClient} that records each outgoing {@link HttpRequest} and answers by path, plus public
 * IP-literal hosts (which pass pin-on-connect without a DNS lookup), so no socket is opened.
 */
class JavaHttpTransport_RedirectCredentials_Test {

	private static final String ORIGIN_A = "http://93.184.216.34";
	private static final String ORIGIN_B = "http://8.8.8.8";

	/** A JDK HttpClient that never touches the network: 302s {@code /cross} and {@code /same}, 200s anything else. */
	private static final class RecordingHttpClient extends HttpClient {
		final List<HttpRequest> sent = new ArrayList<>();

		@Override
		public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
			sent.add(request);
			var path = request.uri().getPath();
			var location = switch (path) {
				case "/cross" -> ORIGIN_B + "/echo";
				case "/same" -> "/echo";
				default -> null;
			};
			var headers = location == null ? Map.<String,List<String>>of() : Map.of("Location", List.of(location));
			return response(request, location == null ? 200 : 302, HttpHeaders.of(headers, (k, v) -> true));
		}

		@SuppressWarnings({
			"unchecked" // Stub HttpResponse<InputStream> is cast to the generic HttpResponse<T> the fake client must return (body type is not inspected)
		})
		private static <T> HttpResponse<T> response(HttpRequest request, int status, HttpHeaders headers) {
			return (HttpResponse<T>) new HttpResponse<InputStream>() {
				@Override public int statusCode() { return status; }
				@Override public HttpRequest request() { return request; }
				@Override public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
				@Override public HttpHeaders headers() { return headers; }
				@Override public InputStream body() { return new ByteArrayInputStream(new byte[0]); }
				@Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
				@Override public URI uri() { return request.uri(); }
				@Override public Version version() { return Version.HTTP_1_1; }
			};
		}

		@Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
		@Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { throw new UnsupportedOperationException(); }
		@Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
		@Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
		@Override public Redirect followRedirects() { return Redirect.NEVER; }
		@Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
		@Override public SSLContext sslContext() { return null; }
		@Override public SSLParameters sslParameters() { return new SSLParameters(); }
		@Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
		@Override public Version version() { return Version.HTTP_1_1; }
		@Override public Optional<Executor> executor() { return Optional.empty(); }
	}

	private static TransportRequest request(String uri) {
		return TransportRequest.builder()
			.method("GET")
			.uri(uri)
			.remoteUrlPolicy(true, false)
			.header("Authorization", "Bearer secret-token")
			.header("Cookie", "session=abc123")
			.header("X-Trace", "t-1")
			.build();
	}

	private static List<String> headerNames(HttpRequest r) {
		return List.copyOf(r.headers().map().keySet());
	}

	@SuppressWarnings({
		"resource" // Transport and the final TransportResponse are short-lived test fixtures over an in-memory stub client.
	})
	private static RecordingHttpClient run(String uri) throws Exception {
		var client = new RecordingHttpClient();
		var transport = JavaHttpTransport.builder().httpClient(client).build();
		transport.execute(request(uri));
		return client;
	}

	@Test void a01_crossOriginRedirect_dropsCredentialHeaders() throws Exception {
		var client = run(ORIGIN_A + "/cross");
		assertList(client.sent.stream().map(HttpRequest::uri).toList(), ORIGIN_A + "/cross", ORIGIN_B + "/echo");
		assertList(headerNames(client.sent.get(0)), "Authorization", "Cookie", "Host", "X-Trace");
		assertList(headerNames(client.sent.get(1)), "Host", "X-Trace");
	}

	@Test void a02_sameOriginRedirect_keepsCredentialHeaders() throws Exception {
		var client = run(ORIGIN_A + "/same");
		assertList(client.sent.stream().map(HttpRequest::uri).toList(), ORIGIN_A + "/same", ORIGIN_A + "/echo");
		assertList(headerNames(client.sent.get(1)), "Authorization", "Cookie", "Host", "X-Trace");
	}
}
