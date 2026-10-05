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
package org.apache.juneau.rest.client.apachehttpclient50;

import static org.apache.juneau.test.bct.BctAssertions.*;

import java.util.*;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.*;
import org.apache.hc.core5.http.protocol.*;
import org.apache.hc.core5.io.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link ApacheHc5Transport}'s policy-covered redirect loop does not forward caller-set credential
 * headers to a different origin, while still forwarding them on a same-origin redirect.
 *
 * <p>
 * Uses a stub {@link CloseableHttpClient} that records each outgoing request and answers by path, so no socket is
 * opened.
 */
@SuppressWarnings({
	"resource" // Transport, stub client and responses are short-lived in-memory test fixtures.
})
class ApacheHc5Transport_RedirectCredentials_Test {

	/** A CloseableHttpClient that never touches the network: 302s {@code /cross} and {@code /same}, 200s anything else. */
	private static final class RecordingHttpClient extends CloseableHttpClient {
		final List<ClassicHttpRequest> sent = new ArrayList<>();

		@Override
		protected CloseableHttpResponse doExecute(HttpHost target, ClassicHttpRequest request, HttpContext context) {
			sent.add(request);
			String path;
			try {
				path = request.getUri().getPath();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			var location = switch (path) {
				case "/cross" -> "http://b.example.com/echo";
				case "/same" -> "/echo";
				default -> null;
			};
			var response = new BasicClassicHttpResponse(location == null ? 200 : 302);
			if (location != null)
				response.addHeader("Location", location);
			return CloseableHttpResponse.adapt(response);
		}

		@Override public void close() { /* Nothing to release. */ }
		@Override public void close(CloseMode closeMode) { /* Nothing to release. */ }
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

	private static List<String> headerNames(ClassicHttpRequest r) {
		return Arrays.stream(r.getHeaders()).map(Header::getName).toList();
	}

	private static List<String> uris(RecordingHttpClient client) {
		return client.sent.stream().map(r -> {
			try {
				return r.getUri().toString();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).toList();
	}

	private static RecordingHttpClient run(String uri) throws Exception {
		var client = new RecordingHttpClient();
		ApacheHc5Transport.builder().httpClient(client).build().execute(request(uri));
		return client;
	}

	@Test void a01_crossOriginRedirect_dropsCredentialHeaders() throws Exception {
		var client = run("http://a.example.com/cross");
		assertList(uris(client), "http://a.example.com/cross", "http://b.example.com/echo");
		assertList(headerNames(client.sent.get(0)), "Authorization", "Cookie", "X-Trace");
		assertList(headerNames(client.sent.get(1)), "X-Trace");
	}

	@Test void a02_sameOriginRedirect_keepsCredentialHeaders() throws Exception {
		var client = run("http://a.example.com/same");
		assertList(uris(client), "http://a.example.com/same", "http://a.example.com/echo");
		assertList(headerNames(client.sent.get(1)), "Authorization", "Cookie", "X-Trace");
	}

	private static RecordingHttpClient runWith(String uri, String... nameValuePairs) throws Exception {
		var b = TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false);
		for (var i = 0; i < nameValuePairs.length; i += 2)
			b.header(nameValuePairs[i], nameValuePairs[i + 1]);
		var client = new RecordingHttpClient();
		ApacheHc5Transport.builder().httpClient(client).build().execute(b.build());
		return client;
	}

	private static List<String> sensitiveNames(ClassicHttpRequest r) {
		return Arrays.stream(r.getHeaders()).filter(Header::isSensitive).map(Header::getName).toList();
	}

	@Test void a03_apiKeyAndProxyAuthorizationOnly_flaggedSensitive() throws Exception {
		var client = runWith("http://a.example.com/cross", "X-API-Key", "k-1", "Proxy-Authorization", "Basic abc", "X-Trace", "t-1");
		assertList(sensitiveNames(client.sent.get(0)), "X-API-Key", "Proxy-Authorization");
	}

	@Test void a04_apiKeyAndProxyAuthorizationOnly_crossOriginDropped() throws Exception {
		var client = runWith("http://a.example.com/cross", "X-API-Key", "k-1", "Proxy-Authorization", "Basic abc", "X-Trace", "t-1");
		assertList(uris(client), "http://a.example.com/cross", "http://b.example.com/echo");
		assertList(headerNames(client.sent.get(1)), "X-Trace");
	}

	@Test void a05_mixedCaseNames_flaggedSensitiveAndDropped() throws Exception {
		var client = runWith("http://a.example.com/cross", "x-api-key", "k-1", "PROXY-authorization", "Basic abc", "cOoKiE", "s=1", "X-Trace", "t-1");
		assertList(sensitiveNames(client.sent.get(0)), "x-api-key", "PROXY-authorization", "cOoKiE");
		assertList(headerNames(client.sent.get(1)), "X-Trace");
	}

	@Test void a06_apiKeyAndProxyAuthorizationOnly_sameOriginKept() throws Exception {
		var client = runWith("http://a.example.com/same", "X-API-Key", "k-1", "Proxy-Authorization", "Basic abc");
		assertList(headerNames(client.sent.get(1)), "X-API-Key", "Proxy-Authorization");
	}
}
