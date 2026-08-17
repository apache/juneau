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
import java.nio.charset.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.junit.jupiter.api.*;

/**
 * Client-side {@code X-Request-Id} correlation: auto-send interceptor, echo capture, the {@code sendRequestId(boolean)}
 * opt-out, the pre-call {@link RestRequest#requestId(String)} setter, the debug-emit stamp-from-field, and the
 * rendered-message prefix.
 *
 * <p>
 * Uses inline {@link HttpTransport} lambdas (not {@code MockHttpTransport}, which lives in the downstream
 * {@code juneau-rest-mock} module): each lambda captures the sent {@code X-Request-Id} into a holder and controls what
 * the response echoes.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource"  // Clients/responses in tests are closed with try-with-resources.
})
class RestClient_RequestId_Test extends TestBase {

	private static final String HDR = "X-Request-Id";

	private static TransportResponse body(TransportResponse.Builder b) {
		return b.body(new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8))).build();
	}

	private static String sentId(TransportRequest req) {
		var h = req.getFirstHeader(HDR);
		return h != null ? h.value() : null;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Phase 6 — auto-send + no-overwrite + opt-out + pre-call setter.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_autoSendMintsAndCaptures() throws Exception {
		var sent = new AtomicReference<String>();
		HttpTransport transport = req -> {
			sent.set(sentId(req));
			return body(TransportResponse.builder().statusCode(200).reasonPhrase("OK").header(HDR, sentId(req)));
		};
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var req = client.get("/");
			try (var res = req.run()) {
				assertNotNull(sent.get(), "auto-send should set X-Request-Id");
				assertFalse(sent.get().isEmpty());
				assertEquals(sent.get(), req.getRequestId());
				assertEquals(sent.get(), res.getRequestId());
			}
		}
	}

	@Test void a02_doesNotOverwriteCallerHeader() throws Exception {
		var sent = new AtomicReference<String>();
		HttpTransport transport = req -> {
			sent.set(sentId(req));
			return body(TransportResponse.builder().statusCode(200).reasonPhrase("OK").header(HDR, sentId(req)));
		};
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			// Differing-case header name must still be honored (case-insensitive no-overwrite).
			var req = client.get("/").header("x-request-id", "caller-id");
			try (var res = req.run()) {
				assertEquals("caller-id", sent.get());
				assertEquals("caller-id", req.getRequestId());
				assertEquals("caller-id", res.getRequestId());
			}
		}
	}

	@Test void a03_preCallSetter() throws Exception {
		var sent = new AtomicReference<String>();
		HttpTransport transport = req -> {
			sent.set(sentId(req));
			return body(TransportResponse.builder().statusCode(200).reasonPhrase("OK").header(HDR, sentId(req)));
		};
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var req = client.get("/").requestId("preset-id");
			try (var res = req.run()) {
				assertEquals("preset-id", sent.get());
				assertEquals("preset-id", req.getRequestId());
				assertEquals("preset-id", res.getRequestId());
			}
		}
	}

	@Test void a04_optOut_noHeaderSent() throws Exception {
		var sent = new AtomicReference<String>();
		HttpTransport transport = req -> {
			sent.set(sentId(req));
			return body(TransportResponse.builder().statusCode(200).reasonPhrase("OK"));
		};
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").sendRequestId(false).build()) {
			var req = client.get("/");
			try (var res = req.run()) {
				assertNull(sent.get(), "opt-out must send no X-Request-Id");
				assertNull(req.getRequestId());
				assertNull(res.getRequestId());
			}
		}
	}

	@Test void a05_optOut_laterInterceptorsDoesNotResurrect() throws Exception {
		var sent = new AtomicReference<String>();
		HttpTransport transport = req -> {
			sent.set(sentId(req));
			return body(TransportResponse.builder().statusCode(200).reasonPhrase("OK"));
		};
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com")
				.sendRequestId(false)
				.interceptors(new RestCallInterceptor() { /* no-op user interceptor added after opt-out */ })
				.build()) {
			var req = client.get("/");
			try (var res = req.run()) {
				assertNull(sent.get());
				assertNull(res.getRequestId());
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Phase 7 — echo capture: effective id prefers the server-echoed value; no echo returns null (never the sent id).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_echoDiffersFromSent() throws Exception {
		var sent = new AtomicReference<String>();
		HttpTransport transport = req -> {
			sent.set(sentId(req));
			return body(TransportResponse.builder().statusCode(200).reasonPhrase("OK").header(HDR, "server-sanitized-id"));
		};
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var req = client.get("/");
			try (var res = req.run()) {
				assertEquals("server-sanitized-id", res.getRequestId());  // echoed (effective)
				assertEquals(sent.get(), req.getRequestId());             // sent (minted)
				assertNotEquals(sent.get(), res.getRequestId());
			}
		}
	}

	@Test void b02_noEchoReturnsNull_neverFallsBackToSent() throws Exception {
		// Server responds without echoing X-Request-Id.
		HttpTransport transport = req -> body(TransportResponse.builder().statusCode(200).reasonPhrase("OK"));
		try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var req = client.get("/");
			try (var res = req.run()) {
				assertNotNull(req.getRequestId(), "sent id was still minted");
				assertNull(res.getRequestId(), "no echo -> null, never falls back to the sent id");
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Phase 8/9 — debug emit stamps the structured field and the rendered message carries the prefix.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_emitStampsStructuredFieldAndPrefix() throws Exception {
		var loggerName = getClass().getName() + ".c01";
		var logger = RichLogger.getLogger(loggerName);
		var prev = logger.getLevel();
		logger.setLevel(Level.INFO);
		try (var cap = logger.captureEvents(Level.INFO)) {
			HttpTransport transport = req -> body(TransportResponse.builder().statusCode(200).reasonPhrase("OK").header(HDR, "echoed-777"));
			try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com").debugLoggerName(loggerName).build();
				var res = client.get("/").run()) {
				res.body().asString();
			}
			var record = cap.last();
			assertNotNull(record);
			// Structured field carries the effective (echoed) id...
			assertEquals("echoed-777", LogRecordContext.of(record).get("requestId"));
			// ...and the rendered message carries the matching prefix.
			assertTrue(record.getMessage().contains("[requestId=echoed-777] "), record.getMessage());
		} finally {
			logger.setLevel(prev);
		}
	}

	@Test void c02_optOut_debugEnabled_doesNotThrow_noField() throws Exception {
		var loggerName = getClass().getName() + ".c02";
		var logger = RichLogger.getLogger(loggerName);
		var prev = logger.getLevel();
		logger.setLevel(Level.INFO);
		try (var cap = logger.captureEvents(Level.INFO)) {
			HttpTransport transport = req -> body(TransportResponse.builder().statusCode(200).reasonPhrase("OK"));
			try (var client = RestClient.builder().transport(transport).rootUrl("http://x.com")
					.sendRequestId(false).debugLoggerName(loggerName).build();
				var res = client.get("/").run()) {
				res.body().asString();
			}
			var record = cap.last();
			assertNotNull(record);
			// No NPE from a null-valued Map.of, and no requestId field present.
			assertFalse(LogRecordContext.of(record).containsKey("requestId"));
			assertFalse(record.getMessage().contains("[requestId="));
		} finally {
			logger.setLevel(prev);
		}
	}
}
