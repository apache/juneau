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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

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
		return map(
			"Host", AUTHORITY,
			"Origin", "http://" + AUTHORITY,
			"Sec-Fetch-Site", "same-origin",
			"X-Csrf-Token", TOKEN.value()
		);
	}

	/** A response paired with the body the filter wrote to it and the headers it ended up with. */
	private record Capture(HttpServletResponse res, CapturingOutputStream out, Map<String,List<String>> headers) {
		String body() { return out.captured(); }
		List<String> header(String name) { return headers.getOrDefault(name, List.of()); }
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
		// The response keeps its headers the way a container does, so a test can assert the values a header ends up
		// with rather than only which calls were made: setHeader replaces, addHeader appends, reset clears.
		var headers = new TreeMap<String,List<String>>(String.CASE_INSENSITIVE_ORDER);
		doAnswer(x -> headers.put(x.getArgument(0), new ArrayList<>(List.of((String)x.getArgument(1))))).when(r).setHeader(anyString(), anyString());
		doAnswer(x -> headers.computeIfAbsent(x.getArgument(0), k -> new ArrayList<>()).add(x.getArgument(1))).when(r).addHeader(anyString(), anyString());
		doAnswer(x -> { headers.clear(); return null; }).when(r).reset();
		return new Capture(r, out, headers);
	}

	/** A legitimate write with one header replaced, or removed when {@code value} is <jk>null</jk>. */
	private static HttpServletRequest write(String header, String value, String contentType) {
		var h = goodWriteHeaders();
		if (value == null)
			h.remove(header);
		else
			h.put(header, value);
		return req("POST", h, contentType);
	}

	/** A request that {@link LoopbackBoundary} refuses for exactly {@code reason}. */
	private static HttpServletRequest refusedFor(BoundaryReason reason) {
		return switch (reason) {
			case HOST_MISMATCH -> write("Host", "evil.example", "application/json");
			case ORIGIN_MISSING -> write("Origin", null, "application/json");
			case ORIGIN_MISMATCH -> write("Origin", "http://evil.example", "application/json");
			case FETCH_SITE_NOT_SAME_ORIGIN -> write("Sec-Fetch-Site", "cross-site", "application/json");
			case CONTENT_TYPE_NOT_JSON -> write("Sec-Fetch-Site", "same-origin", "text/plain");
			case CSRF_TOKEN_MISSING -> write("X-Csrf-Token", null, "application/json");
			case CSRF_TOKEN_MISMATCH -> write("X-Csrf-Token", "wrong", "application/json");
			default -> throw new IllegalArgumentException("LoopbackBoundary does not emit " + reason);
		};
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
		// The header name the token must be echoed back under is published as a sibling attribute so the page
		// renderer's document shell can emit it (data-juneau-csrf-header); default is DEFAULT_CSRF_HEADER.
		verify(req).setAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE, LoopbackBoundary.DEFAULT_CSRF_HEADER);
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

	@Test void f01_rejectionHeaderIsRequestBoundary() throws Exception {
		assertEquals("X-Request-Boundary", LoopbackBoundaryFilter.REJECTION_HEADER);
		var h = goodWriteHeaders();
		h.put("Origin", "http://evil.example");
		var capture = capturing();

		filter().doFilter(req("POST", h, "application/json"), capture.res(), mock(FilterChain.class));

		verify(capture.res()).setHeader("X-Request-Boundary", "ORIGIN_MISMATCH");
		verify(capture.res(), never()).setHeader(eq("X-Loopback-Boundary"), anyString());
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
	// h) Caching, the shared filter, and what a refusal may say
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_rejectionHasNoStore() throws Exception {
		var capture = capturing();
		filter().doFilter(refusedFor(BoundaryReason.ORIGIN_MISMATCH), capture.res(), mock(FilterChain.class));
		assertEquals(List.of("no-store"), capture.header("Cache-Control"));
	}

	@ParameterizedTest
	@EnumSource(value=BoundaryReason.class, names={
		"HOST_MISMATCH", "ORIGIN_MISSING", "ORIGIN_MISMATCH", "FETCH_SITE_NOT_SAME_ORIGIN", "CONTENT_TYPE_NOT_JSON",
		"CSRF_TOKEN_MISSING", "CSRF_TOKEN_MISMATCH"
	})
	void h02_rejectionHasNoStoreOnEveryReason(BoundaryReason reason) throws Exception {
		var capture = capturing();
		filter().doFilter(refusedFor(reason), capture.res(), mock(FilterChain.class));
		assertEquals(List.of(reason.name()), capture.header(RequestBoundaryFilter.REJECTION_HEADER));
		assertEquals(List.of("no-store"), capture.header("Cache-Control"));
	}

	@Test void h03_aliasConstantsResolve() {
		assertEquals(RequestBoundaryFilter.TOKEN_ATTRIBUTE, LoopbackBoundaryFilter.TOKEN_ATTRIBUTE);
		assertEquals(RequestBoundaryFilter.HEADER_ATTRIBUTE, LoopbackBoundaryFilter.HEADER_ATTRIBUTE);
		assertEquals(RequestBoundaryFilter.REJECTION_HEADER, LoopbackBoundaryFilter.REJECTION_HEADER);
	}

	@Test void h04_filterAcceptsAnyRequestBoundary() throws Exception {
		var stub = new RequestBoundary() {
			@Override public BoundaryResult check(HttpServletRequest req) { return BoundaryResult.ALLOWED; }
			@Override public String csrfHeader() { return "X-Stub-Csrf"; }
			@Override public String tokenFor(HttpServletRequest req) { return "token-for-" + req.getRequestURI(); }
		};
		var req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/page");
		var res = mock(HttpServletResponse.class);
		var chain = mock(FilterChain.class);
		new RequestBoundaryFilter(stub).doFilter(req, res, chain);
		verify(req).setAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE, "token-for-/page");
		verify(req).setAttribute(RequestBoundaryFilter.HEADER_ATTRIBUTE, "X-Stub-Csrf");
		verify(chain).doFilter(req, res);
	}

	@Test void h05_rejectionBodyEchoesNothing() throws Exception {
		var hostile = "http://evil.example\"'<script>\u0001\r\n";
		var capture = capturing();
		filter().doFilter(write("Origin", hostile, "application/json"), capture.res(), mock(FilterChain.class));
		var body = capture.body();
		for (var fragment : List.of("evil.example", "<script>", "\"'", "\u0001"))
			assertFalse(body.contains(fragment), () -> "the rejection body echoes caller input: " + body);
		capture.headers().forEach((k, v) -> v.forEach(x ->
			assertFalse(x.contains("evil.example") || x.contains("<script>"), () -> "header " + k + " echoes caller input: " + x)));
	}

	@Test void h06_markTokenBearingReplacesExistingCacheControl() throws Exception {
		var capture = capturing();
		capture.res().setHeader("Cache-Control", "public, max-age=60");
		RequestBoundaryFilter.markTokenBearing(capture.res());
		assertEquals(List.of("private, no-store"), capture.header("Cache-Control"));
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
