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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.client5.http.protocol.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.http.message.*;
import org.apache.hc.core5.http.protocol.*;
import org.apache.hc.core5.io.*;
import org.apache.hc.core5.util.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.rest.client.apachehttpclient50.*;
import org.junit.jupiter.api.Test;

/**
 * Exercises the edge paths of {@link ApacheHc5Transport} (per-request timeout base config, request-build failure,
 * response-wiring failure cleanup) using a caller-supplied stub client, so no network is involved.
 */
@SuppressWarnings({
	"resource" // Transports and stub clients are short-lived test fixtures.
})
class ApacheHc5Transport_Edge_Test {

	/** A client that is not {@link Configurable}; records the context it was invoked with and returns a canned response. */
	private static class StubClient extends CloseableHttpClient {
		final AtomicReference<HttpContext> context = new AtomicReference<>();
		final ClassicHttpResponse response;

		StubClient(ClassicHttpResponse response) {
			this.response = response;
		}

		@Override /* CloseableHttpClient */
		public ClassicHttpResponse executeOpen(HttpHost target, ClassicHttpRequest request, HttpContext context) {
			this.context.set(context);
			return response;
		}

		@Override /* CloseableHttpClient */
		protected CloseableHttpResponse doExecute(HttpHost target, ClassicHttpRequest request, HttpContext context) {
			throw new UnsupportedOperationException();
		}

		@Override /* ModalCloseable */
		public void close(CloseMode closeMode) {}

		@Override /* Closeable */
		public void close() {}
	}

	/** Same stub, but {@link Configurable} with a configurable (possibly null) default config. */
	private static final class ConfigurableStubClient extends StubClient implements Configurable {
		private final RequestConfig config;

		ConfigurableStubClient(ClassicHttpResponse response, RequestConfig config) {
			super(response);
			this.config = config;
		}

		@Override /* Configurable */
		public RequestConfig getConfig() {
			return config;
		}
	}

	/** A response whose entity cannot supply its content stream, and whose close() optionally fails. */
	private static final class BrokenResponse extends BasicClassicHttpResponse {
		private static final long serialVersionUID = 1L;
		final AtomicBoolean closed = new AtomicBoolean();

		private final boolean closeFails;

		BrokenResponse(boolean closeFails) {
			super(200);
			this.closeFails = closeFails;
			setEntity(new AbstractHttpEntity((String)null, null, false) {
				@Override /* HttpEntity */
				public InputStream getContent() throws IOException {
					throw new IOException("content unavailable");
				}

				@Override /* HttpEntity */
				public boolean isRepeatable() {
					return false;
				}

				@Override /* EntityDetails */
				public long getContentLength() {
					return -1;
				}

				@Override /* HttpEntity */
				public boolean isStreaming() {
					return false;
				}

				@Override /* HttpEntity */
				public void writeTo(OutputStream out) {}

				@Override /* Closeable */
				public void close() {}
			});
		}

		@Override /* BasicClassicHttpResponse */
		public void close() throws IOException {
			closed.set(true);
			if (closeFails)
				throw new IOException("close failed");
		}
	}

	private static ApacheHc5Transport transport(CloseableHttpClient client) {
		return ApacheHc5Transport.builder().httpClient(client).build();
	}

	private static TransportRequest request(String method, Duration timeout) {
		return TransportRequest.builder().method(method).uri("http://127.0.0.1:1/x").timeout(timeout).build();
	}

	// =================================================================================================================
	// A: per-request timeout is layered on the client's default config
	// =================================================================================================================

	@Test
	void a01_timeout_nonConfigurableClient_usesDefaultConfigBase() throws Exception {
		var client = new StubClient(new BasicClassicHttpResponse(204));
		try (var response = transport(client).execute(request("GET", Duration.ofMillis(1500)))) {
			assertEquals(204, response.getStatusCode());
		}
		var config = ((HttpClientContext)client.context.get()).getRequestConfig();
		assertEquals(Timeout.ofMilliseconds(1500), config.getResponseTimeout());
		assertEquals(RequestConfig.DEFAULT.getConnectionRequestTimeout(), config.getConnectionRequestTimeout());
	}

	@Test
	void a02_timeout_configurableClientWithNullConfig_usesDefaultConfigBase() throws Exception {
		var client = new ConfigurableStubClient(new BasicClassicHttpResponse(204), null);
		try (var response = transport(client).execute(request("GET", Duration.ofMillis(2500)))) {
			assertEquals(204, response.getStatusCode());
		}
		var config = ((HttpClientContext)client.context.get()).getRequestConfig();
		assertEquals(Timeout.ofMilliseconds(2500), config.getResponseTimeout());
		assertEquals(RequestConfig.DEFAULT.getConnectionRequestTimeout(), config.getConnectionRequestTimeout());
	}

	@Test
	void a03_timeout_configurableClientWithConfig_copiesClientConfig() throws Exception {
		var base = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(7)).build();
		var client = new ConfigurableStubClient(new BasicClassicHttpResponse(204), base);
		try (var response = transport(client).execute(request("GET", Duration.ofMillis(3500)))) {
			assertEquals(204, response.getStatusCode());
		}
		var config = ((HttpClientContext)client.context.get()).getRequestConfig();
		assertEquals(Timeout.ofMilliseconds(3500), config.getResponseTimeout());
		assertEquals(Timeout.ofSeconds(7), config.getConnectionRequestTimeout());
	}

	@Test
	void a04_noTimeout_contextCarriesNoRequestConfig() throws Exception {
		// The caller's client gets a per-request context (the redirect credential guard), but with no RequestConfig in it,
		// so the client's own default config (proxy, authentication, cookie spec, timeouts) still applies.
		var client = new StubClient(new BasicClassicHttpResponse(204));
		try (var response = transport(client).execute(request("GET", null))) {
			assertEquals(204, response.getStatusCode());
		}
		var context = client.context.get();
		// The guard is package-private, so it is identified by name.
		assertEquals("CredentialGuardingContext", context.getClass().getSimpleName());
		assertNull(((HttpClientContext)context).getRequestConfig());
	}

	// =================================================================================================================
	// B: failures building the request / wiring the response
	// =================================================================================================================

	@Test
	void b01_traceWithBody_failsToBuildRequest() {
		var client = new StubClient(new BasicClassicHttpResponse(204));
		var request = TransportRequest.builder().method("TRACE").uri("http://127.0.0.1:1/x").body(TransportBody.of(StringBody.of("x", "text/plain"))).build();
		var ex = assertThrows(TransportException.class, () -> transport(client).execute(request));
		assertTrue(ex.getMessage().startsWith("Failed to build HTTP request: TRACE"), ex.getMessage());
		assertInstanceOf(IllegalStateException.class, ex.getCause());
		assertNull(client.context.get());
	}

	@Test
	void b02_unreadableEntity_throwsAndClosesResponseEvenIfCloseFails() {
		var broken = new BrokenResponse(true);
		var ex = assertThrows(TransportException.class, () -> transport(new StubClient(broken)).execute(request("GET", null)));
		assertEquals("Failed to read response body: content unavailable", ex.getMessage());
		assertInstanceOf(IOException.class, ex.getCause());
		assertTrue(broken.closed.get(), "the leased connection must be released when response wiring fails");
	}

	@Test
	void b03_unreadableEntity_throwsAndClosesResponse() {
		var broken = new BrokenResponse(false);
		var ex = assertThrows(TransportException.class, () -> transport(new StubClient(broken)).execute(request("GET", null)));
		assertEquals("Failed to read response body: content unavailable", ex.getMessage());
		assertTrue(broken.closed.get(), "the leased connection must be released when response wiring fails");
	}
}
