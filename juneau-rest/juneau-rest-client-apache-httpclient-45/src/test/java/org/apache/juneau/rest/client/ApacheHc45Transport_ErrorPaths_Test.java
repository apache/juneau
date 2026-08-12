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
import java.net.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ApacheHc45Transport}'s error/retry paths, driven with a fake
 * {@link CloseableHttpClient} so every {@code IOException} subtype/message combination that
 * {@code isStaleConnectionFailure(...)} branches on &mdash; and the response-wiring-failure /
 * {@code closeQuietly(...)} cleanup path &mdash; can be exercised deterministically, without depending on
 * real socket-reset timing (see {@link ApacheHc45Transport_StaleConnectionRetry_Test} for the
 * real-socket end-to-end retry proof).
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
class ApacheHc45Transport_ErrorPaths_Test {

	@FunctionalInterface
	private interface Executor {
		CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException;
	}

	private static final class FakeHttpClient extends CloseableHttpClient {
		private final Executor executor;
		FakeHttpClient(Executor executor) { this.executor = executor; }
		@Override protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			return executor.execute(target, request, context);
		}
		@Override public void close() { /* no real connections to release */ }
		@Override @Deprecated public HttpParams getParams() { return null; }
		@Override @Deprecated public ClientConnectionManager getConnectionManager() { return null; }
	}

	private static final class FakeResponse extends BasicHttpResponse implements CloseableHttpResponse {
		boolean closeThrows;
		boolean closed;
		FakeResponse(int statusCode) { super(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, "OK")); }
		@Override public void close() throws IOException {
			closed = true;
			if (closeThrows)
				throw new IOException("close-boom");
		}
	}

	/** An entity whose {@code getContent()} always fails &mdash; simulates a body-read wiring failure. */
	private static final class ThrowingEntity extends AbstractHttpEntity {
		@Override public boolean isRepeatable() { return true; }
		@Override public long getContentLength() { return -1; }
		@Override public InputStream getContent() throws IOException { throw new IOException("content-boom"); }
		@Override public void writeTo(OutputStream out) { /* never reached by these tests */ }
		@Override public boolean isStreaming() { return false; }
	}

	private static TransportRequest request(String method) {
		return TransportRequest.builder().method(method).uri(URI.create("http://example.invalid/x")).build();
	}

	// =================================================================================================================
	// A — Stale-connection retry: both attempts fail
	// =================================================================================================================

	@Test
	void a01_doubleStaleConnectionFailure_bothAttemptsFail_throwsTransportException() throws Exception {
		var attempts = new int[1];
		var httpClient = new FakeHttpClient((target, req, ctx) -> {
			attempts[0]++;
			throw new NoHttpResponseException("The target server failed to respond");
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(request("GET")));
			assertTrue(ex.getMessage().contains("HTTP transport error"), ex.getMessage());
		}
		assertEquals(2, attempts[0], "a GET must be retried exactly once after a stale-connection failure, then give up");
	}

	@Test
	void a02_socketExceptionResetMessage_caseInsensitive_isRetriedOnceThenSucceeds() throws Exception {
		var attempts = new int[1];
		var httpClient = new FakeHttpClient((target, req, ctx) -> {
			attempts[0]++;
			if (attempts[0] == 1)
				throw new SocketException("Connection RESET by peer");
			var resp = new FakeResponse(200);
			resp.setEntity(new StringEntity("OK", ContentType.create("text/plain", "UTF-8")));
			return resp;
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			try (var resp = transport.execute(request("GET"))) {
				assertEquals(200, resp.getStatusCode());
			}
		}
		assertEquals(2, attempts[0], "a SocketException whose message contains \"reset\" (any case) must be treated as a stale-connection failure and retried");
	}

	// =================================================================================================================
	// B — Failures that are NOT stale-connection signals must not be retried
	// =================================================================================================================

	@Test
	void b01_plainIOException_isNotStaleConnectionFailure_notRetried() throws Exception {
		var attempts = new int[1];
		var httpClient = new FakeHttpClient((target, req, ctx) -> {
			attempts[0]++;
			throw new IOException("connection refused");
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(request("GET")));
			assertTrue(ex.getMessage().contains("connection refused"), ex.getMessage());
		}
		assertEquals(1, attempts[0], "a plain IOException (neither NoHttpResponseException nor a reset SocketException) must not be retried");
	}

	@Test
	void b02_socketExceptionNullMessage_isNotStaleConnectionFailure_notRetried() throws Exception {
		var attempts = new int[1];
		var httpClient = new FakeHttpClient((target, req, ctx) -> {
			attempts[0]++;
			throw new SocketException();
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			assertThrows(TransportException.class, () -> transport.execute(request("POST")));
		}
		assertEquals(1, attempts[0], "a SocketException with a null message must not be treated as a stale-connection failure");
	}

	@Test
	void b03_socketExceptionMessageWithoutReset_isNotStaleConnectionFailure_notRetried() throws Exception {
		var attempts = new int[1];
		var httpClient = new FakeHttpClient((target, req, ctx) -> {
			attempts[0]++;
			throw new SocketException("Network is unreachable");
		});
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			assertThrows(TransportException.class, () -> transport.execute(request("POST")));
		}
		assertEquals(1, attempts[0], "a SocketException whose message doesn't mention \"reset\" must not be treated as a stale-connection failure");
	}

	// =================================================================================================================
	// C/D — Response-wiring failure (buildTransportResponse) and its closeQuietly(...) cleanup
	// =================================================================================================================

	@Test
	void c01_entityGetContentThrows_wrappedAsTransportExceptionAndResponseClosed() throws Exception {
		var response = new FakeResponse(200);
		response.setEntity(new ThrowingEntity());
		var httpClient = new FakeHttpClient((target, req, ctx) -> response);
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(request("GET")));
			assertTrue(ex.getMessage().contains("Failed to read response body"), ex.getMessage());
		}
		assertTrue(response.closed, "the response must be closed on the wiring-failure path since the caller never receives it to close");
	}

	@Test
	void d01_closeQuietlyCloseAlsoThrows_originalWiringExceptionStillPropagates() throws Exception {
		var response = new FakeResponse(200);
		response.setEntity(new ThrowingEntity());
		response.closeThrows = true;
		var httpClient = new FakeHttpClient((target, req, ctx) -> response);
		try (var transport = ApacheHc45Transport.builder().httpClient(httpClient).build()) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(request("GET")));
			assertTrue(ex.getMessage().contains("Failed to read response body"),
				() -> "close()'s IOException must be swallowed rather than surfacing in place of the original: " + ex.getMessage());
		}
	}
}
