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
package org.apache.juneau.rest.client.apachehttpclient45;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ApacheHc45Transport}'s SSRF-guard (policy-covered) execution path and its
 * {@link PolicyPinningDnsResolver}, plus the per-request timeout base-config selection.
 *
 * <p>
 * Driven with a fake {@link CloseableHttpClient} so the Juneau-controlled redirect loop can be observed without needing
 * a public network target.  These tests drive transport internals through a caller-supplied client, a path
 * {@code RestClient} would not route guarded requests through in production.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
class ApacheHc45Transport_Policy_Test {

	@FunctionalInterface
	private interface Executor {
		CloseableHttpResponse execute(HttpRequest request) throws IOException;
	}

	private static class FakeHttpClient extends CloseableHttpClient {
		private final Executor executor;
		FakeHttpClient(Executor executor) { this.executor = executor; }
		@Override protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			return executor.execute(request);
		}
		@Override public void close() { /* no real connections to release */ }
		@Override @Deprecated public HttpParams getParams() { return null; }
		@Override @Deprecated public ClientConnectionManager getConnectionManager() { return null; }
	}

	private static final class ConfigurableFakeHttpClient extends FakeHttpClient implements Configurable {
		private final RequestConfig config;
		ConfigurableFakeHttpClient(RequestConfig config, Executor executor) {
			super(executor);
			this.config = config;
		}
		@Override public RequestConfig getConfig() { return config; }
	}

	private static final class FakeResponse extends BasicHttpResponse implements CloseableHttpResponse {
		FakeResponse(int statusCode) { super(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "X")); }
		@Override public void close() { /* nothing to release */ }
	}

	private static TransportRequest guarded(String uri) {
		return TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false).build();
	}

	// =================================================================================================================
	// A — policy-covered execution: pinning resolver, Juneau-controlled redirects
	// =================================================================================================================

	@Test
	void a01_resolver_pinsPublicAddress() throws Exception {
		var pinned = PolicyPinningDnsResolver.INSTANCE.resolve("192.0.2.1");
		assertEquals(1, pinned.length);
		assertEquals("192.0.2.1", pinned[0].getHostAddress());
	}

	@Test
	void a02_resolver_rejectsLoopbackTarget() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PolicyPinningDnsResolver.INSTANCE.resolve("127.0.0.1"));
		assertTrue(ex.getMessage().contains("No allowed address"), ex.getMessage());
	}

	@Test
	void a03_guardedRequest_redirectsAreFollowedByJuneauAndRevalidated() throws Exception {
		var uris = new ArrayList<String>();
		var httpClient = new FakeHttpClient(req -> {
			uris.add(((HttpUriRequest)req).getURI().toString());
			if (uris.size() == 1) {
				var redirect = new FakeResponse(302);
				redirect.addHeader("Location", "http://192.0.2.2/next");
				return redirect;
			}
			return new FakeResponse(200);
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			try (var resp = transport.execute(guarded("http://192.0.2.1/x"))) {
				assertEquals(200, resp.getStatusCode());
			}
		}
		assertEquals(List.of("http://192.0.2.1/x", "http://192.0.2.2/next"), uris);
	}

	@Test
	void a04_guardedRequest_redirectToLoopbackIsRejected() throws Exception {
		var httpClient = new FakeHttpClient(req -> {
			var redirect = new FakeResponse(302);
			redirect.addHeader("Location", "http://127.0.0.1/secret");
			return redirect;
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(guarded("http://192.0.2.1/x")));
			assertTrue(ex.getMessage().contains("127.0.0.1"), ex.getMessage());
		}
	}

	@Test
	void a05_guardedRequest_staleConnectionIsNotReplayed() throws Exception {
		var attempts = new int[1];
		var httpClient = new FakeHttpClient(req -> {
			attempts[0]++;
			throw new NoHttpResponseException("The target server failed to respond");
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(guarded("http://192.0.2.1/x")));
			assertTrue(ex.getMessage().contains("HTTP transport error"), ex.getMessage());
		}
		assertEquals(1, attempts[0], "guarded requests are never replayed on a stale connection");
	}

	// =================================================================================================================
	// B — supportsUrlPolicy
	// =================================================================================================================

	@Test
	void b01_supportsUrlPolicy_onlyForTheTransportsOwnClient() throws Exception {
		try (var transport = ApacheHc45Transport.create()) {
			assertTrue(transport.supportsUrlPolicy());
		}
		try (var transport = ApacheHc45Transport.builder().httpClient(new FakeHttpClient(req -> new FakeResponse(200))).build()) {
			assertFalse(transport.supportsUrlPolicy(), "a caller-supplied client cannot be given pin-on-connect / redirect control");
		}
	}

	// =================================================================================================================
	// C — per-request timeout layered on the right base config
	// =================================================================================================================

	@Test
	void c01_timeout_preservesClientDefaultConfig() throws Exception {
		var seen = new RequestConfig[1];
		var httpClient = new ConfigurableFakeHttpClient(RequestConfig.custom().setRedirectsEnabled(false).build(), req -> {
			seen[0] = ((Configurable)req).getConfig();
			return new FakeResponse(200);
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			transport.execute(TransportRequest.builder().method("GET").uri("http://192.0.2.1/x").timeout(Duration.ofSeconds(5)).build()).close();
		}
		assertEquals(5000, seen[0].getSocketTimeout());
		assertEquals(5000, seen[0].getConnectTimeout());
		assertFalse(seen[0].isRedirectsEnabled(), "client-level settings must survive the per-request timeout override");
	}

	@Test
	void c02_timeout_clientWithoutDefaultConfig_usesHttpClientDefaults() throws Exception {
		var seen = new RequestConfig[1];
		var httpClient = new ConfigurableFakeHttpClient(null, req -> {
			seen[0] = ((Configurable)req).getConfig();
			return new FakeResponse(200);
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			transport.execute(TransportRequest.builder().method("GET").uri("http://192.0.2.1/x").timeout(Duration.ofSeconds(5)).build()).close();
		}
		assertEquals(5000, seen[0].getSocketTimeout());
		assertTrue(seen[0].isRedirectsEnabled());
	}

	@Test
	void c03_timeout_nonConfigurableClient_usesHttpClientDefaults() throws Exception {
		var seen = new RequestConfig[1];
		var httpClient = new FakeHttpClient(req -> {
			seen[0] = ((Configurable)req).getConfig();
			return new FakeResponse(200);
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			transport.execute(TransportRequest.builder().method("GET").uri("http://192.0.2.1/x").timeout(Duration.ofSeconds(5)).build()).close();
		}
		assertEquals(5000, seen[0].getSocketTimeout());
		assertTrue(seen[0].isRedirectsEnabled());
	}
}
