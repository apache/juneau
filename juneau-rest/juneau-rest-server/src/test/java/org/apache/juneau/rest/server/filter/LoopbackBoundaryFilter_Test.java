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
package org.apache.juneau.rest.server.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for {@link LoopbackBoundaryFilter} — that a refused request never reaches the chain, that the refusal is
 * rendered explicitly rather than as a silent no-op, and that an allowed request passes through with the token
 * available to the page renderer.
 *
 * @since 10.0.0
 */
class LoopbackBoundaryFilter_Test extends TestBase {

	private static final String AUTHORITY = "127.0.0.1:8790";
	private static final SynchronizerToken TOKEN = SynchronizerToken.of("the-real-token");

	private static LoopbackBoundaryFilter filter() {
		return new LoopbackBoundaryFilter(LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).build());
	}

	/** Captures what the filter wrote to the response body. */
	private static final class CapturingOutputStream extends ServletOutputStream {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		@Override public void write(int b) { baos.write(b); }
		@Override public boolean isReady() { return true; }
		@Override public void setWriteListener(WriteListener l) { /* not used by this filter */ }
		String captured() { return baos.toString(StandardCharsets.UTF_8); }
	}

	private static HttpServletRequest req(String method, Map<String,String> headers, String contentType) {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn(method);
		when(r.getContentType()).thenReturn(contentType);
		headers.forEach((k, v) -> when(r.getHeader(k)).thenReturn(v));
		return r;
	}

	private static Map<String,String> goodWriteHeaders() {
		var m = new LinkedHashMap<String,String>();
		m.put("Host", AUTHORITY);
		m.put("Origin", "http://" + AUTHORITY);
		m.put("Sec-Fetch-Site", "same-origin");
		m.put("X-Csrf-Token", TOKEN.value());
		return m;
	}

	/** A response paired with the body the filter wrote to it. */
	private record Capture(HttpServletResponse res, CapturingOutputStream out) {
		String body() { return out.captured(); }
	}

	/**
	 * A response whose output stream is captured, since the filter writes a body on every refusal.
	 *
	 * <p>
	 * Every refusal test funnels through here so the stubbing lives in one place.
	 */
	@SuppressWarnings({
		"resource" // The capture wraps an in-memory buffer and the mock's getOutputStream() acquires nothing; there is no resource to release.
	})
	private static Capture capturing() throws IOException {
		var out = new CapturingOutputStream();
		var r = mock(HttpServletResponse.class);
		when(r.getOutputStream()).thenReturn(out);
		return new Capture(r, out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Allowed requests pass through
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_allowedWrite_reachesTheChain() throws Exception {
		var req = req("POST", goodWriteHeaders(), "application/json");
		var res = mock(HttpServletResponse.class);
		var chain = mock(FilterChain.class);
		filter().doFilter(req, res, chain);
		verify(chain).doFilter(req, res);
		verify(res, never()).setStatus(anyInt());
	}

	@Test void a02_allowedRequest_exposesTheTokenToThePageRenderer() throws Exception {
		var req = req("GET", Map.of("Host", AUTHORITY), null);
		filter().doFilter(req, mock(HttpServletResponse.class), mock(FilterChain.class));
		verify(req).setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN.value());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Refused requests never reach application code
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_foreignOrigin_chainIsNeverInvoked() throws Exception {
		var h = goodWriteHeaders();
		h.put("Origin", "http://evil.example");
		var chain = mock(FilterChain.class);
		filter().doFilter(req("POST", h, "application/json"), capturing().res(), chain);
		verifyNoInteractions(chain);
	}

	@Test void b02_mismatchedHost_chainIsNeverInvoked() throws Exception {
		var chain = mock(FilterChain.class);
		filter().doFilter(req("GET", Map.of("Host", "evil.example"), null), capturing().res(), chain);
		verifyNoInteractions(chain);
	}

	@Test void b03_missingToken_chainIsNeverInvoked() throws Exception {
		var h = goodWriteHeaders();
		h.remove("X-Csrf-Token");
		var chain = mock(FilterChain.class);
		filter().doFilter(req("POST", h, "application/json"), capturing().res(), chain);
		verifyNoInteractions(chain);
	}

	@Test void b04_formEncodedWrite_chainIsNeverInvoked() throws Exception {
		var chain = mock(FilterChain.class);
		filter().doFilter(req("POST", goodWriteHeaders(), "application/x-www-form-urlencoded"),
			capturing().res(), chain);
		verifyNoInteractions(chain);
	}

	@Test void b05_refusedRequestDoesNotExposeTheToken() throws Exception {
		var req = req("GET", Map.of("Host", "evil.example"), null);
		filter().doFilter(req, capturing().res(), mock(FilterChain.class));
		verify(req, never()).setAttribute(eq(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE), any());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) A refusal is rendered explicitly — never as an empty result or a silent no-op
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_refusalCarriesTheStatusReasonHeaderAndAJsonBody() throws Exception {
		var h = goodWriteHeaders();
		h.put("Origin", "http://evil.example");
		var capture = capturing();

		filter().doFilter(req("POST", h, "application/json"), capture.res(), mock(FilterChain.class));

		verify(capture.res()).reset();
		verify(capture.res()).setStatus(403);
		verify(capture.res()).setHeader(LoopbackBoundaryFilter.REJECTION_HEADER, "ORIGIN_MISMATCH");
		verify(capture.res()).setContentType("application/json;charset=utf-8");
		var body = capture.body();
		assertTrue(body.contains("\"reason\":\"ORIGIN_MISMATCH\""), body);
		assertTrue(body.contains("\"message\":\""), body);
		assertFalse(body.isBlank(), "a refusal must not render as an empty body");
	}

	@Test void c02_hostMismatchAnswers421() throws Exception {
		var capture = capturing();
		filter().doFilter(req("GET", Map.of("Host", "evil.example"), null), capture.res(), mock(FilterChain.class));
		verify(capture.res()).setStatus(421);
		verify(capture.res()).setHeader(LoopbackBoundaryFilter.REJECTION_HEADER, "HOST_MISMATCH");
	}

	@Test void c03_formEncodedWriteAnswers415() throws Exception {
		var capture = capturing();
		filter().doFilter(req("POST", goodWriteHeaders(), "application/x-www-form-urlencoded"), capture.res(),
			mock(FilterChain.class));
		verify(capture.res()).setStatus(415);
		verify(capture.res()).setHeader(LoopbackBoundaryFilter.REJECTION_HEADER, "CONTENT_TYPE_NOT_JSON");
	}

	@Test void c04_refusalBodyDoesNotLeakTheServersToken() throws Exception {
		var h = goodWriteHeaders();
		h.put("X-Csrf-Token", "wrong");
		var capture = capturing();
		filter().doFilter(req("POST", h, "application/json"), capture.res(), mock(FilterChain.class));
		assertFalse(capture.body().contains(TOKEN.value()), capture::body);
	}

	@Test void c05_refusalBodyIsWellFormedJsonWhenTheMessageCarriesQuotableCharacters() throws Exception {
		// The configured header name reaches the message, so a name carrying a quote or backslash must not
		// produce a malformed body.
		var b = LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).csrfHeader("X-\"Odd\"\\Header").build();
		var capture = capturing();
		var h = goodWriteHeaders();
		h.remove("X-Csrf-Token");
		new LoopbackBoundaryFilter(b).doFilter(req("POST", h, "application/json"), capture.res(),
			mock(FilterChain.class));
		var body = capture.body();
		assertTrue(body.contains("\\\"Odd\\\""), () -> "expected escaped quotes: " + body);
		assertTrue(body.contains("\\\\Header"), () -> "expected escaped backslash: " + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Construction
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_boundaryIsRequired() {
		assertThrows(IllegalArgumentException.class, () -> new LoopbackBoundaryFilter(null));
	}

	@Test void d02_boundaryAccessor() {
		var b = LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).build();
		assertSame(b, new LoopbackBoundaryFilter(b).boundary());
	}
}
