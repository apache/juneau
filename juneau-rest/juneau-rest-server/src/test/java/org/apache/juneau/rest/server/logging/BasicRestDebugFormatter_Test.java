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
package org.apache.juneau.rest.server.logging;

import static java.util.Collections.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.rest.server.logging.BasicRestDebugFormatter.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Unit tests for {@link BasicRestDebugFormatter} — secure-by-default body handling (env-gated no-dump), header
 * redaction (all values, widened formatter-local set, separator folding), sanitization/flood bounds, and the status
 * line.
 *
 * @since 10.0.0
 */
class BasicRestDebugFormatter_Test {

	private final BasicRestDebugFormatter f = new BasicRestDebugFormatter();

	private RestRequest req;
	private RestResponse res;
	private HttpServletRequest sreq;
	private HttpServletResponse sres;

	@BeforeEach void setUp() {
		req = mock(RestRequest.class);
		res = mock(RestResponse.class);
		sreq = mock(HttpServletRequest.class);
		sres = mock(HttpServletResponse.class);
		when(req.getHttpServletRequest()).thenReturn(sreq);
		when(res.getHttpServletResponse()).thenReturn(sres);
		when(sres.getStatus()).thenReturn(200);
		when(sreq.getMethod()).thenReturn("GET");
		when(sreq.getRequestURI()).thenReturn("/foo");
	}

	@AfterEach void tearDown() {
		// Never leak forced gate state between tests; next resolution re-reads the environment once.
		resetAllowDumpBodiesForTest(null);
	}

	// -----------------------------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------------------------

	/** Asserts the planted secret is absent in plaintext, UTF-8 spaced-hex, Base64, and escaped code-unit forms. */
	private static void assertAllEncodingsAbsent(String out, String secret) {
		assertFalse(out.contains(secret), () -> "plaintext secret present in: " + out);
		var bytes = secret.getBytes(UTF8);
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
		assertEquals(-1, out.indexOf('\u2028'), () -> "raw LS present in: " + out);
		assertEquals(-1, out.indexOf('\u2029'), () -> "raw PS present in: " + out);
		assertEquals(-1, out.indexOf('\u001B'), () -> "raw ESC present in: " + out);
		for (var c = '\u202A'; c <= '\u202E'; c++)
			assertEquals(-1, out.indexOf(c), "raw bidi control present");
	}

	private void stubRequestHeaders(LinkedHashMap<String,List<String>> h) {
		when(sreq.getHeaderNames()).thenReturn(enumeration(new ArrayList<>(h.keySet())));
		h.forEach((k, v) -> when(sreq.getHeaders(k)).thenReturn(enumeration(v)));
	}

	private void stubResponseHeaders(LinkedHashMap<String,List<String>> h) {
		when(sres.getHeaderNames()).thenReturn(new ArrayList<>(h.keySet()));
		h.forEach((k, v) -> when(sres.getHeaders(k)).thenReturn(new ArrayList<>(v)));
	}

	private void requestBody(String body, String contentType) {
		var bytes = body.getBytes(UTF8);
		when(req.getCachedContent()).thenReturn(bytes);
		when(req.getCachedContentLength()).thenReturn((long) bytes.length);
		when(sreq.getContentType()).thenReturn(contentType);
		when(res.getCachedContent()).thenReturn(new byte[0]);
		when(res.getCachedContentLength()).thenReturn(0L);
	}

	// -----------------------------------------------------------------------------------------
	// a — formatBasic / statusLine (sanitization + maxUriLength)
	// -----------------------------------------------------------------------------------------

	@Test void a01_formatBasic_statusLine() {
		assertEquals("[200] HTTP GET /foo", f.formatBasic(req, res));
	}

	@Test void a02_statusLine_sanitizesMethodAndUri() {
		when(sreq.getMethod()).thenReturn("GET");
		when(sreq.getRequestURI()).thenReturn("/x\r\n[200] HTTP GET /admin");
		var s = f.formatBasic(req, res);
		assertNoRawControlChars(s);
		assertTrue(s.contains("\\r\\n"), s);
		assertFalse(s.contains("\n[200] HTTP GET /admin"), "URI must not forge a physical line: " + s);
	}

	@Test void a03_statusLine_appliesMaxUriLength() {
		when(sreq.getRequestURI()).thenReturn("/".repeat(5000));
		f.maxUriLength(32);
		var s = f.formatBasic(req, res);
		assertTrue(s.contains("[truncated]"), s);
		assertTrue(s.length() < 100, "URI must be capped: " + s.length());
	}

	@Test void a04_statusLine_doesNotAppendQueryString() {
		when(sreq.getRequestURI()).thenReturn("/foo");
		when(sreq.getQueryString()).thenReturn("token=secret");
		var s = f.formatBasic(req, res);
		assertFalse(s.contains("token=secret"), s);
	}

	// -----------------------------------------------------------------------------------------
	// b — formatHeaders (redaction, all values, folding, bounds, injection)
	// -----------------------------------------------------------------------------------------

	@Test void b01_formatHeaders_lengthsAndExecTime() {
		when(req.getCachedContentLength()).thenReturn(5L);
		when(res.getCachedContentLength()).thenReturn(10L);
		when(req.getExecTime()).thenReturn(42L);
		when(sreq.getHeaderNames()).thenReturn(emptyEnumeration());
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertTrue(s.contains("Request length: 5 bytes"), s);
		assertTrue(s.contains("Response code: 200"), s);
		assertTrue(s.contains("Response length: 10 bytes"), s);
		assertTrue(s.contains("Exec time: 42ms"), s);
	}

	@Test void b02_formatHeaders_redactsSensitive() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("Authorization", List.of("Bearer secret"));
		reqH.put("User-Agent", List.of("curl/8.0"));
		stubRequestHeaders(reqH);
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertTrue(s.contains("Authorization: [REDACTED]"), s);
		assertFalse(s.contains("Bearer secret"), s);
		assertTrue(s.contains("User-Agent: curl/8.0"), s);
	}

	@Test void b03_formatHeaders_allValuesRendered_eachMasked() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("Cookie", List.of("a=1", "b=2"));
		stubRequestHeaders(reqH);
		var resH = new LinkedHashMap<String,List<String>>();
		resH.put("Set-Cookie", List.of("s1=x", "s2=y", "s3=z"));
		stubResponseHeaders(resH);

		var s = f.formatHeaders(req, res);
		// Every value rendered, each sensitive value independently masked.
		assertEquals(5, s.split("\\[REDACTED\\]", -1).length - 1, s);
		assertFalse(s.contains("a=1"), s);
		assertFalse(s.contains("s3=z"), s);
	}

	@Test void b04_formatHeaders_separatorFolding_customSet() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		f.redactedHeaders(List.of("X-Auth-Token"));
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("X-Auth-Token", List.of("v1"));
		reqH.put("X_Auth_Token", List.of("v2"));
		reqH.put("XAuthToken", List.of("v3"));
		stubRequestHeaders(reqH);
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertFalse(s.contains("v1"), s);
		assertFalse(s.contains("v2"), s);
		assertFalse(s.contains("v3"), s);
		assertEquals(3, s.split("\\[REDACTED\\]", -1).length - 1, s);
	}

	@Test void b05_formatHeaders_builtinWidenedSet() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("X-Authorization", List.of("secretA"));
		reqH.put("WWW-Authenticate", List.of("secretB"));
		reqH.put("Referer", List.of("http://secretC"));
		reqH.put("Authorization\t", List.of("Bearer secretD"));  // trailing tab must not dodge the match
		stubRequestHeaders(reqH);
		var resH = new LinkedHashMap<String,List<String>>();
		resH.put("Location", List.of("http://secretE"));
		stubResponseHeaders(resH);

		var s = f.formatHeaders(req, res);
		assertFalse(s.contains("secretA"), s);
		assertFalse(s.contains("secretB"), s);
		assertFalse(s.contains("secretC"), s);
		assertFalse(s.contains("Bearer secretD"), s);
		assertFalse(s.contains("secretE"), s);
	}

	@Test void b06_formatHeaders_addRedactedHeaders_extends_redactedHeaders_replaces() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);

		// addRedactedHeaders extends: built-in Authorization still masked, plus the new one.
		f.addRedactedHeaders(List.of("X-Custom-Secret"));
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("Authorization", List.of("Bearer keep-secret"));
		reqH.put("X-Custom-Secret", List.of("custom-value"));
		stubRequestHeaders(reqH);
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertFalse(s.contains("Bearer keep-secret"), s);
		assertFalse(s.contains("custom-value"), s);

		// redactedHeaders replaces: now Authorization is NOT masked.
		var f2 = new BasicRestDebugFormatter().redactedHeaders(List.of("X-Only-This"));
		var reqH2 = new LinkedHashMap<String,List<String>>();
		reqH2.put("Authorization", List.of("Bearer now-visible"));
		when(sreq.getHeaderNames()).thenReturn(enumeration(new ArrayList<>(reqH2.keySet())));
		reqH2.forEach((k, v) -> when(sreq.getHeaders(k)).thenReturn(enumeration(v)));
		var s2 = f2.formatHeaders(req, res);
		assertTrue(s2.contains("Bearer now-visible"), s2);
	}

	@Test void b07_formatHeaders_maxFieldLength_capsValue() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		f.maxFieldLength(16);
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("X-Big", List.of("v".repeat(5000)));
		stubRequestHeaders(reqH);
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertTrue(s.contains("[truncated]"), s);
		assertFalse(s.contains("v".repeat(100)), "oversized value must be capped: " + s.length());
	}

	@Test void b08_formatHeaders_maxHeaders_and_scan_bounds() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		f.maxHeaders(3).maxHeaderScan(6);
		var reqH = new LinkedHashMap<String,List<String>>();
		for (var i = 0; i < 20; i++)
			reqH.put("H" + i, List.of("val" + i));
		stubRequestHeaders(reqH);
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		// Only 3 emitted; scan stops at 6 so the approximate marker is used (not full enumeration of 20).
		assertTrue(s.contains("H0: val0"), s);
		assertTrue(s.contains("H2: val2"), s);
		assertFalse(s.contains("H3: val3"), s);
		assertTrue(s.contains("more headers omitted"), s);
		assertFalse(s.contains("H19"), s);
	}

	@Test void b09_formatHeaders_injectionInNameAndValue() {
		when(req.getCachedContentLength()).thenReturn(-1L);
		when(res.getCachedContentLength()).thenReturn(-1L);
		var reqH = new LinkedHashMap<String,List<String>>();
		reqH.put("X-Evil\r\n=== HTTP Call ===", List.of("v\r\n[200] HTTP GET /admin"));
		stubRequestHeaders(reqH);
		when(sres.getHeaderNames()).thenReturn(emptyList());

		var s = f.formatHeaders(req, res);
		assertNoRawControlChars(s);
		assertFalse(s.contains("\n=== HTTP Call ==="), "header name must not forge a line: " + s);
		assertFalse(s.contains("\n[200] HTTP GET /admin"), "header value must not forge a line: " + s);
	}

	@Test void b10_redactedHeadersDefault_unchangedRegressionGuard() {
		// Formatter-local widening must NOT mutate the shared default.
		assertEquals(5, RedactedHeaders.DEFAULT.size());
		assertTrue(RedactedHeaders.DEFAULT.containsAll(
			List.of("Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization", "X-API-Key")));
		// The formatter-local set is wider and still includes the inherited defaults.
		assertTrue(DEFAULT_REDACTED_HEADERS.contains("Cookie"));
		assertTrue(DEFAULT_REDACTED_HEADERS.contains("X-Auth-Token"));
	}

	// -----------------------------------------------------------------------------------------
	// c — formatBody (secure-by-default no-dump)
	// -----------------------------------------------------------------------------------------

	@Test void c01_bodyGate_offVsOn_pairedContrast() {
		var secret = "hunter2-PASSWORD";
		requestBody("{\"password\":\"" + secret + "\"}", "application/json");

		resetAllowDumpBodiesForTest(false);
		var off = f.formatBody(req, res);
		assertTrue(off.contains("[body suppressed"), off);
		assertTrue(off.contains("set " + ENV_ALLOW_DUMP_BODIES + " to enable"), off);
		assertAllEncodingsAbsent(off, secret);

		resetAllowDumpBodiesForTest(true);
		var on = f.formatBody(req, res);
		assertTrue(on.contains(secret), on);
		assertFalse(on.contains("Content Hex"), on);
		assertFalse(on.contains("---Request Content UTF-8---"), on);
		assertFalse(on.contains(toSpacedHex(secret.getBytes(UTF8))), on);

		assertNotEquals(off, on);
	}

	@Test void c02_bodyGateOff_scrubberNeverInvoked() {
		var invoked = new AtomicBoolean(false);
		f.bodyScrubber((ct, body) -> {
			invoked.set(true);
			return "SCRUBBED";
		});
		var secret = "do-not-leak";
		requestBody("{\"p\":\"" + secret + "\"}", "application/json");

		resetAllowDumpBodiesForTest(false);
		var s = f.formatBody(req, res);
		assertFalse(invoked.get(), "scrubber must not run while the gate is off");
		assertTrue(s.contains("[body suppressed"), s);
		assertAllEncodingsAbsent(s, secret);
	}

	@Test void c03_bodyGateOn_scrubberSelected() {
		f.bodyScrubber((ct, body) -> "SCRUBBED-OUTPUT");
		var secret = "raw-secret-value";
		requestBody("{\"p\":\"" + secret + "\"}", "application/json");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("SCRUBBED-OUTPUT"), s);
		assertAllEncodingsAbsent(s, secret);
	}

	@Test void c04_bodyGateOn_scrubberThrows_failsClosed() {
		f.bodyScrubber((ct, body) -> {
			throw new RuntimeException(body);  // must not re-leak the body through the exception
		});
		var secret = "throwing-secret";
		requestBody("{\"p\":\"" + secret + "\"}", "application/json");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("scrubber failed"), s);
		assertFalse(s.contains("set " + ENV_ALLOW_DUMP_BODIES), "scrubber-failed placeholder must not use gate-off wording: " + s);
		assertAllEncodingsAbsent(s, secret);
	}

	@Test void c05_bodyGateOn_scrubberReturnsNull_failsClosed() {
		f.bodyScrubber((ct, body) -> null);
		var secret = "null-scrubber-secret";
		requestBody("{\"p\":\"" + secret + "\"}", "application/json");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("scrubber failed"), s);
		assertAllEncodingsAbsent(s, secret);
	}

	@Test void c06_bodyGateOn_noScrubber_dumpsRawSanitized() {
		var secret = "plain-visible-secret";
		requestBody("body-with-" + secret, "text/plain");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains(secret), s);
	}

	@Test void c07_bodyGateOn_nonRenderable_octetStream_placeholder() {
		var secret = "PLAINTEXT-BINARY-SECRET";
		requestBody("prefix-" + secret + "-suffix", "application/octet-stream");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("[body not rendered"), s);
		assertTrue(s.contains("binary/non-renderable content"), s);
		assertAllEncodingsAbsent(s, secret);
	}

	@Test void c08_bodyGateOn_nonIdentityEncoding_placeholder() {
		var secret = "COMPRESSED-SECRET";
		var bytes = ("prefix-" + secret).getBytes(UTF8);
		when(req.getCachedContent()).thenReturn(bytes);
		when(req.getCachedContentLength()).thenReturn((long) bytes.length);
		when(sreq.getContentType()).thenReturn("application/json");
		when(sreq.getHeader("Content-Encoding")).thenReturn("gzip");
		when(res.getCachedContent()).thenReturn(new byte[0]);

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("[body not rendered"), s);
		assertAllEncodingsAbsent(s, secret);
	}

	@Test void c09_bodyGateOn_bodyCapTruncation_noHex() {
		when(req.getCachedContent()).thenReturn("0123".getBytes(UTF8));
		when(req.getCachedContentLength()).thenReturn(10L);
		when(sreq.getContentType()).thenReturn("text/plain");
		when(res.getCachedContent()).thenReturn(new byte[0]);

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("0123"), s);
		assertTrue(s.contains("truncated 6 bytes"), s);
		assertFalse(s.contains("Content Hex"), s);
		assertFalse(s.contains(toSpacedHex("0123".getBytes(UTF8))), s);
	}

	@Test void c10_bodyGateOn_charCap_boundsHugeScrubberOutput() {
		f.bodyCap(10);  // charCap = 60
		f.bodyScrubber((ct, body) -> "X".repeat(5000));
		requestBody("seed", "text/plain");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertTrue(s.contains("\u2026[truncated]"), s);
		assertFalse(s.contains("X".repeat(200)), "scrubber output must be char-capped: " + s.length());
	}

	@Test void c11_bodyGateOff_placeholder_sanitizesContentTypeToken() {
		var bytes = "body".getBytes(UTF8);
		when(req.getCachedContent()).thenReturn(bytes);
		when(req.getCachedContentLength()).thenReturn((long) bytes.length);
		when(sreq.getContentType()).thenReturn("application/json\r\n=== HTTP Call ===");
		when(res.getCachedContent()).thenReturn(new byte[0]);

		resetAllowDumpBodiesForTest(false);
		var s = f.formatBody(req, res);
		assertNoRawControlChars(s);
		assertFalse(s.contains("\n=== HTTP Call ==="), "content-type must not forge a line in the placeholder: " + s);
	}

	@Test void c12_bodyGateOn_injectionInBody_noForgedLine() {
		requestBody("safe\r\n=== HTTP Call ===\r\nmore", "text/plain");

		resetAllowDumpBodiesForTest(true);
		var s = f.formatBody(req, res);
		assertNoRawControlChars(s);
		assertFalse(s.contains("\n=== HTTP Call ==="), "body must not forge a line: " + s);
		assertTrue(s.contains("\\r\\n"), s);
	}

	@Test void c13_formatBody_emptyWhenNoContent() {
		when(req.getCachedContent()).thenReturn(null);
		when(res.getCachedContent()).thenReturn(null);
		assertEquals("", f.formatBody(req, res));
	}

	// -----------------------------------------------------------------------------------------
	// d — env-var master gate parsing / seam
	// -----------------------------------------------------------------------------------------

	@Test void d01_truthyTable_disable() {
		for (var v : Arrays.asList(null, "", "   ", "false", "FALSE", "0", " false ", " 0 "))
			assertFalse(parseAllowDumpBodies(v), () -> "should disable: [" + v + "]");
	}

	@Test void d02_truthyTable_enable() {
		for (var v : List.of("1", "true", "yes", "on", "no", "off", "please", " 1 "))
			assertTrue(parseAllowDumpBodies(v), () -> "should enable: [" + v + "]");
	}

	@Test void d03_seam_forcesBothStates_andReadOnce() {
		resetAllowDumpBodiesForTest(true);
		assertTrue(isAllowDumpBodies());
		assertTrue(isAllowDumpBodies());  // cached; stable across calls
		resetAllowDumpBodiesForTest(false);
		assertFalse(isAllowDumpBodies());
	}

	@Test void d04_noSystemPropertyFallback() {
		// Guard: only meaningful when the ambient env var is unset (the seam neutralizes it for other tests).
		Assumptions.assumeTrue(System.getenv(ENV_ALLOW_DUMP_BODIES) == null);
		var prev = System.getProperty(ENV_ALLOW_DUMP_BODIES);
		System.setProperty(ENV_ALLOW_DUMP_BODIES, "true");
		try {
			resetAllowDumpBodiesForTest(null);  // re-resolve from env only
			assertFalse(isAllowDumpBodies(), "a system property must NEVER enable the env-only gate");
		} finally {
			if (prev == null)
				System.clearProperty(ENV_ALLOW_DUMP_BODIES);
			else
				System.setProperty(ENV_ALLOW_DUMP_BODIES, prev);
			resetAllowDumpBodiesForTest(null);
		}
	}

	// -----------------------------------------------------------------------------------------
	// e — isBodyRenderable predicate
	// -----------------------------------------------------------------------------------------

	@Test void e01_renderable_textAndStructured() {
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

	@Test void e02_nonRenderable_binaryAndMultipartAndAbsent() {
		assertFalse(f.isBodyRenderable("application/octet-stream"));
		assertFalse(f.isBodyRenderable("multipart/form-data"));
		assertFalse(f.isBodyRenderable("image/png"));
		assertFalse(f.isBodyRenderable(null));
		assertFalse(f.isBodyRenderable(""));
		assertFalse(f.isBodyRenderable("   "));
		assertFalse(f.isBodyRenderable("; charset=utf-8"));
	}
}
