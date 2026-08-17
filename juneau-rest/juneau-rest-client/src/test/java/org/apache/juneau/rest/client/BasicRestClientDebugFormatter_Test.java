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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link BasicRestClientDebugFormatter} — the widened, formatter-local header redaction set
 * (with separator folding), status-line/header sanitization against log forging (Phase 2), and the
 * secure-by-default body-dump master gate / renderability / scrubber pipeline (Phase 3).
 *
 * <p>
 * Exercised end-to-end through a real {@link RestClient} with a stub {@link HttpTransport} (rather than
 * mocking {@link RestRequest}/{@link RestResponse} directly — both are {@code final}, and the module has no
 * Mockito dependency), mirroring {@code RestClient_DebugLogging_Test}'s existing pattern.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Clients/responses in tests are closed with try-with-resources.
})
class BasicRestClientDebugFormatter_Test extends TestBase {

	@AfterEach
	void tearDown() {
		// Never leak forced gate state between tests; next resolution re-reads the environment once.
		// Belt-and-suspenders alongside each test's own try/finally (Risk 6).
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
	}

	// -----------------------------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------------------------

	/** Asserts the planted secret is absent in plaintext, UTF-8 spaced-hex, Base64, and escaped code-unit forms. */
	private static void assertAllEncodingsAbsent(String out, String secret) {
		assertFalse(out.contains(secret), () -> "plaintext secret present in: " + out);
		var bytes = secret.getBytes(StandardCharsets.UTF_8);
		assertFalse(out.contains(toSpacedHex(bytes)), () -> "spaced-hex secret present in: " + out);
		assertFalse(out.contains(Base64.getEncoder().encodeToString(bytes)), () -> "base64 secret present in: " + out);
		var esc = new StringBuilder();
		for (var i = 0; i < secret.length(); i++)
			esc.append(String.format("\\u%04X", (int) secret.charAt(i)));
		assertFalse(out.contains(esc.toString()), () -> "escaped-code-unit secret present in: " + out);
	}

	/** Asserts no client-controllable raw control character survived — only formatter-owned {@code \n} separators. */
	private static void assertNoRawControlChars(String out) {
		assertEquals(-1, out.indexOf('\r'), () -> "raw CR present in: " + out);
		assertEquals(-1, out.indexOf('\u0085'), () -> "raw NEL present in: " + out);
	}

	private static TransportResponse.Builder responseBuilder(int statusCode, String body) {
		return TransportResponse.builder()
			.statusCode(statusCode)
			.reasonPhrase("OK")
			.body(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
	}

	/** Runs a request/response pair through a real client at the given level and returns the captured message. */
	private static String runAndCapture(String loggerName, Level level, RestClient.Builder clientBuilder,
			java.util.function.Function<RestRequest,RestRequest> requestFn) throws Exception {
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(level);
		try (var c = logger.captureEvents(level)) {
			try (var client = clientBuilder.debugLoggerName(loggerName).build();
				var res = requestFn.apply(client.get("http://example.com/x")).run()) {
				res.body().asString();
			}
			assertNotNull(c.last());
			return c.last().getMessage();
		} finally {
			logger.setLevel(prevLevel);
		}
	}

	// -----------------------------------------------------------------------------------------
	// b — widened redaction set + separator folding + CR/LF sanitization (Phase 2)
	// -----------------------------------------------------------------------------------------

	@Test
	void b01_widenedSet_redactsXAuthToken() throws Exception {
		var msg = runAndCapture(getClass().getName() + ".b01", Level.FINE,
			RestClient.builder().transport(req -> responseBuilder(200, "ok").build()),
			req -> req.header("X-Auth-Token", "widened-secret"));
		assertTrue(msg.contains("[REDACTED]"), msg);
		assertFalse(msg.contains("widened-secret"), msg);
	}

	@Test
	void b02_separatorFolding_underscoreVariant_stillRedacted() throws Exception {
		var msg = runAndCapture(getClass().getName() + ".b02", Level.FINE,
			RestClient.builder().transport(req -> responseBuilder(200, "ok").build()),
			req -> req.header("X_Auth_Token", "folded-secret"));
		assertTrue(msg.contains("[REDACTED]"), msg);
		assertFalse(msg.contains("folded-secret"), msg);
	}

	@Test
	void b03_crlfInjection_headerValue_escapedNotForged() throws Exception {
		var msg = runAndCapture(getClass().getName() + ".b03", Level.FINE,
			RestClient.builder().transport(req -> responseBuilder(200, "ok").build()),
			req -> req.header("X-Evil", "a\r\nInjected: yes"));
		assertNoRawControlChars(msg);
		assertTrue(msg.contains("\\r\\n"), msg);
		assertFalse(msg.contains("\nInjected: yes"), "header value must not forge a physical line: " + msg);
	}

	// -----------------------------------------------------------------------------------------
	// c — secure-by-default body-dump master gate, renderability, scrubber (Phase 3)
	// -----------------------------------------------------------------------------------------

	/**
	 * Runs a POST (with the given request body/content-type) against a stub transport that returns the given
	 * response body/content-type/content-encoding, at {@code FINEST}, through a client using {@code formatter},
	 * and returns the captured message. The request side always carries a body so both {@code appendBody} call
	 * sites run.
	 */
	private static String runBodyTest(String loggerName, BasicRestClientDebugFormatter formatter,
			String reqBody, String reqContentType, String resBody, String resContentType, String resContentEncoding)
			throws Exception {
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.FINEST);
		try (var c = logger.captureEvents(Level.FINEST)) {
			try (var client = RestClient.builder()
				.transport(req -> {
					if (req.getBody() != null) {
						try {
							req.getBody().writeTo(new ByteArrayOutputStream());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					var b = TransportResponse.builder().statusCode(200).reasonPhrase("OK");
					if (resContentType != null)
						b.header("Content-Type", resContentType);
					if (resContentEncoding != null)
						b.header("Content-Encoding", resContentEncoding);
					return b.body(new ByteArrayInputStream(resBody.getBytes(StandardCharsets.UTF_8))).build();
				})
				.debugLoggerName(loggerName)
				.debugFormatter(formatter)
				.build();
				var res = client.post("http://example.com/x").header("Content-Type", reqContentType)
					.bodyString(reqBody).run()) {
				res.body().asString();
			}
			assertNotNull(c.last());
			return c.last().getMessage();
		} finally {
			logger.setLevel(prevLevel);
		}
	}

	/**
	 * The direct "{@code d02}-equivalent inversion test" (closes B2). Locks all four RED-proof preconditions:
	 * {@code FINEST} capture, a non-empty captured body, the placeholder actually present (not just raw-text
	 * absent), and every encoded form of the secret absent (catches a broken fix that removes the plaintext dump
	 * but leaves the pre-existing {@code toSpacedHex} block intact).
	 */
	@Test
	void c01_bodyGateOff_default_suppressesPlaceholder_allEncodingsAbsent() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.FALSE);
		var secret = "hunter2-PASSWORD";
		try {
			var msg = runBodyTest(getClass().getName() + ".c01", new BasicRestClientDebugFormatter(),
				"ignored-request-body", "text/plain", "{\"password\":\"" + secret + "\"}", "application/json", null);
			assertTrue(msg.contains("[body suppressed"), msg);
			assertTrue(msg.contains("set JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES to enable"), msg);
			assertFalse(msg.contains("UTF-8---"), msg);
			assertFalse(msg.contains("Content Hex"), msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void c02_bodyGateOff_scrubberNeverInvoked() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.FALSE);
		var invoked = new AtomicBoolean(false);
		var secret = "do-not-leak";
		try {
			var formatter = new BasicRestClientDebugFormatter().bodyScrubber((ct, body) -> {
				invoked.set(true);
				return "SCRUBBED";
			});
			var msg = runBodyTest(getClass().getName() + ".c02", formatter,
				"ignored", "text/plain", "{\"p\":\"" + secret + "\"}", "application/json", null);
			assertFalse(invoked.get(), "scrubber must not run while the gate is off");
			assertTrue(msg.contains("[body suppressed"), msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void c03_bodyGateOn_nonRenderableContentType_placeholder() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var secret = "PLAINTEXT-BINARY-SECRET";
		try {
			var msg = runBodyTest(getClass().getName() + ".c03", new BasicRestClientDebugFormatter(),
				"prefix-" + secret + "-suffix", "application/octet-stream",
				"prefix-" + secret + "-suffix", "multipart/form-data", null);
			assertTrue(msg.contains("[body not rendered"), msg);
			assertTrue(msg.contains("binary/non-renderable content"), msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	/**
	 * Proves the identity-{@code Content-Encoding} check is wired in (S3 in the plan review): gate on + a
	 * renderable {@code Content-Type} + a non-identity {@code Content-Encoding} must still suppress. Forgetting
	 * this check would pass every other Phase 3 test.
	 */
	@Test
	void c04_bodyGateOn_nonIdentityEncoding_placeholder() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var secret = "COMPRESSED-SECRET";
		try {
			var msg = runBodyTest(getClass().getName() + ".c04", new BasicRestClientDebugFormatter(),
				"ignored", "text/plain", "prefix-" + secret, "application/json", "gzip");
			assertTrue(msg.contains("[body not rendered"), msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void c05_bodyGateOn_scrubberThrows_failsClosed() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var secret = "throwing-secret";
		try {
			var formatter = new BasicRestClientDebugFormatter().bodyScrubber((ct, body) -> {
				throw new RuntimeException(body);  // must not re-leak the body through the exception message
			});
			var msg = runBodyTest(getClass().getName() + ".c05", formatter,
				"ignored", "text/plain", "{\"p\":\"" + secret + "\"}", "application/json", null);
			assertTrue(msg.contains("scrubber failed"), msg);
			assertFalse(msg.contains("set JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES"),
				"scrubber-failed placeholder must not use gate-off wording: " + msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void c06_bodyGateOn_scrubberReturnsNull_failsClosed() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var secret = "null-scrubber-secret";
		try {
			var formatter = new BasicRestClientDebugFormatter().bodyScrubber((ct, body) -> null);
			var msg = runBodyTest(getClass().getName() + ".c06", formatter,
				"ignored", "text/plain", "{\"p\":\"" + secret + "\"}", "application/json", null);
			assertTrue(msg.contains("scrubber failed"), msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void c07_bodyGateOn_scrubberTransformsText_rawAbsent() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var secret = "raw-secret-value";
		try {
			var formatter = new BasicRestClientDebugFormatter().bodyScrubber((ct, body) -> "SCRUBBED-OUTPUT");
			var msg = runBodyTest(getClass().getName() + ".c07", formatter,
				"ignored", "text/plain", "{\"p\":\"" + secret + "\"}", "application/json", null);
			assertTrue(msg.contains("SCRUBBED-OUTPUT"), msg);
			assertAllEncodingsAbsent(msg, secret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void c08_bodyGateOn_noScrubber_dumpsRawSanitized_labelsCorrect() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var secret = "plain-visible-secret";
		try {
			var msg = runBodyTest(getClass().getName() + ".c08", new BasicRestClientDebugFormatter(),
				"ignored", "text/plain", "body-with-" + secret, "text/plain", null);
			assertTrue(msg.contains(secret), msg);
			assertTrue(msg.contains("---Response Content---"), msg);
			assertFalse(msg.contains("UTF-8---"), msg);
			assertFalse(msg.contains("Content Hex"), msg);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	/**
	 * Request/response content-type independence (Risk 5): different content types on each side (one renderable,
	 * one not) catches a copy/paste bug that resolves the wrong side's {@code Content-Type}. A test using the same
	 * content type on both sides could not catch this.
	 */
	@Test
	void c09_requestResponseContentTypeIndependence() throws Exception {
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		var reqSecret = "request-side-secret";
		var resSecret = "response-side-secret";
		try {
			var msg = runBodyTest(getClass().getName() + ".c09", new BasicRestClientDebugFormatter(),
				reqSecret, "text/plain", "prefix-" + resSecret, "application/octet-stream", null);
			// Request side (text/plain) is renderable → dumps raw.
			assertTrue(msg.contains(reqSecret), msg);
			// Response side (application/octet-stream) is not → placeholder, response secret absent.
			assertTrue(msg.contains("[body not rendered"), msg);
			assertAllEncodingsAbsent(msg, resSecret);
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	// -----------------------------------------------------------------------------------------
	// d — env-var master gate parsing (pin-tests ported from the server's truthy table so the
	// duplicated resolution logic can't silently drift from BasicRestDebugFormatter's)
	// -----------------------------------------------------------------------------------------

	@Test
	void d01_truthyTable_disable() {
		for (var v : Arrays.asList(null, "", "   ", "false", "FALSE", "0", " false ", " 0 "))
			assertFalse(BasicRestClientDebugFormatter.parseAllowDumpBodies(v), () -> "should disable: [" + v + "]");
	}

	@Test
	void d02_truthyTable_enable() {
		for (var v : List.of("1", "true", "yes", "on", "no", "off", "please", " 1 "))
			assertTrue(BasicRestClientDebugFormatter.parseAllowDumpBodies(v), () -> "should enable: [" + v + "]");
	}

	@Test
	void d03_seam_forcesBothStates_andReadOnce() {
		try {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(true);
			assertTrue(BasicRestClientDebugFormatter.isAllowDumpBodies());
			assertTrue(BasicRestClientDebugFormatter.isAllowDumpBodies());  // cached; stable across calls
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(false);
			assertFalse(BasicRestClientDebugFormatter.isAllowDumpBodies());
		} finally {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void d04_noSystemPropertyFallback() {
		Assumptions.assumeTrue(System.getenv(BasicRestClientDebugFormatter.ENV_ALLOW_DUMP_BODIES) == null);
		var prev = System.getProperty(BasicRestClientDebugFormatter.ENV_ALLOW_DUMP_BODIES);
		System.setProperty(BasicRestClientDebugFormatter.ENV_ALLOW_DUMP_BODIES, "true");
		try {
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);  // re-resolve from env only
			assertFalse(BasicRestClientDebugFormatter.isAllowDumpBodies(), "a system property must NEVER enable the env-only gate");
		} finally {
			if (prev == null)
				System.clearProperty(BasicRestClientDebugFormatter.ENV_ALLOW_DUMP_BODIES);
			else
				System.setProperty(BasicRestClientDebugFormatter.ENV_ALLOW_DUMP_BODIES, prev);
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	// -----------------------------------------------------------------------------------------
	// e — isBodyRenderable predicate (pin-tests ported from the server's table, per decision 1 —
	// the duplicated default method must stay byte-identical to RestDebugFormatter's)
	// -----------------------------------------------------------------------------------------

	private final BasicRestClientDebugFormatter f = new BasicRestClientDebugFormatter();

	@Test
	void e01_renderable_textAndStructured() {
		assertTrue(f.isBodyRenderable("text/plain"));
		assertTrue(f.isBodyRenderable("text/html; charset=utf-8"));
		assertTrue(f.isBodyRenderable("application/json"));
		assertTrue(f.isBodyRenderable("application/json; charset=utf-8"));
		assertTrue(f.isBodyRenderable("application/xml"));
		assertTrue(f.isBodyRenderable("application/hal+json"));
		assertTrue(f.isBodyRenderable("application/atom+xml"));
		assertTrue(f.isBodyRenderable("application/x-www-form-urlencoded"));
		assertTrue(f.isBodyRenderable("APPLICATION/JSON"));
	}

	@Test
	void e02_nonRenderable_binaryAndMultipartAndAbsent() {
		assertFalse(f.isBodyRenderable("application/octet-stream"));
		assertFalse(f.isBodyRenderable("multipart/form-data"));
		assertFalse(f.isBodyRenderable("image/png"));
		assertFalse(f.isBodyRenderable(null));
		assertFalse(f.isBodyRenderable(""));
		assertFalse(f.isBodyRenderable("   "));
		assertFalse(f.isBodyRenderable("; charset=utf-8"));
	}
}
