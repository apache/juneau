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

import static org.apache.juneau.rest.server.filter.CsrfBoundary_Test.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.filter.CsrfBoundary.*;
import org.apache.juneau.rest.server.filter.CsrfBoundary_Test.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for {@link RequestBoundaryFilter} applying a {@link CsrfBoundary}: what an allowed request publishes for the
 * page renderer, when a session is created so the page can carry a session-scoped token, and what a refusal looks
 * like.
 *
 * <p>
 * Every test drives {@link RequestBoundaryFilter#doFilter(ServletRequest, ServletResponse, FilterChain)} itself, with
 * Mockito servlet mocks and a mocked {@link FilterChain}.  A mock REST client would not do: it dispatches straight to
 * the REST context and never runs a servlet filter, so a test built on it passes with no filter installed.
 *
 * <p>
 * Requests are built with {@link CsrfBoundary_Test}'s fixture, which stubs {@code getHeaders(name)} alongside
 * {@code getHeader(name)}.  A request built with {@link R#noSession()} answers {@code getSession(true)} with a new
 * session, so a test can observe whether one was created.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5976" // Each minting rule is its own named test so a failure names the rule it broke.
})
class CsrfBoundaryFilter_Test extends TestBase {

	private static final byte[] KEY = new byte[32];
	static {
		for (var i = 0; i < KEY.length; i++)
			KEY[i] = (byte)(0x61 + i);
	}

	private static final CsrfTokens TOKENS = CsrfTokens.perSession().key(KEY);

	private static final String HOST = "app.example:8080";
	private static final String ORIGIN = "http://app.example:8080";
	private static final String JSON = "application/json";
	private static final String EVIL = "http://evil.example";

	private static CsrfBoundary.Builder boundary() {
		return CsrfBoundary.create().tokens(TOKENS);
	}

	private static RequestBoundaryFilter filter() {
		return new RequestBoundaryFilter(boundary().build());
	}

	/** A safe {@code GET} from a client that sent no hint of what it is loading, with no session. */
	private static R get() {
		return r("GET", "http", HOST, null).noSession();
	}

	/** A same-origin JSON write carrying a valid token for {@code b} in the fixture's session. */
	private static R write(CsrfBoundary b) {
		var r = r("POST", "http", HOST, ORIGIN, "Content-Type", JSON);
		return r.h(b.csrfHeader(), b.tokenFor(new R().build()));
	}

	/** Builds the request, keeping the attributes the filter sets on it so a test can read them back. */
	private static HttpServletRequest build(R r) {
		var req = r.build();
		var attrs = new HashMap<String,Object>();
		doAnswer(x -> attrs.put(x.getArgument(0), x.getArgument(1))).when(req).setAttribute(anyString(), any());
		when(req.getAttribute(anyString())).thenAnswer(x -> attrs.get(x.getArgument(0)));
		return req;
	}

	/** Captures what the filter wrote to the response body. */
	private static final class CapturingOutputStream extends ServletOutputStream {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		@Override public void write(int b) { baos.write(b); }
		@Override public boolean isReady() { return true; }
		@Override public void setWriteListener(WriteListener l) { /* not used by this filter */ }
		String captured() { return baos.toString(StandardCharsets.UTF_8); }
	}

	/** A response paired with the body the filter wrote to it and the headers it ended up with. */
	private record Capture(HttpServletResponse res, CapturingOutputStream out, Map<String,List<String>> headers) {
		String body() { return out.captured(); }
		List<String> header(String name) { return headers.getOrDefault(name, List.of()); }
	}

	/**
	 * A response that keeps its headers the way a container does: {@code setHeader} replaces, {@code addHeader}
	 * appends, {@code reset} clears.  Its writer is unusable, so a refusal must be written as bytes.
	 */
	@SuppressWarnings({
		"resource" // The capture wraps an in-memory buffer and the mock's getOutputStream() acquires nothing; there is no resource to release.
	})
	private static Capture capturing() throws IOException {
		var out = new CapturingOutputStream();
		var r = mock(HttpServletResponse.class);
		when(r.getOutputStream()).thenReturn(out);
		when(r.getWriter()).thenThrow(new IllegalStateException("No writer: a refusal must not depend on one."));
		var headers = new TreeMap<String,List<String>>(String.CASE_INSENSITIVE_ORDER);
		doAnswer(x -> headers.put(x.getArgument(0), new ArrayList<>(List.of((String)x.getArgument(1))))).when(r).setHeader(anyString(), anyString());
		doAnswer(x -> headers.computeIfAbsent(x.getArgument(0), k -> new ArrayList<>()).add(x.getArgument(1))).when(r).addHeader(anyString(), anyString());
		doAnswer(x -> { headers.clear(); return null; }).when(r).reset();
		return new Capture(r, out, headers);
	}

	/** Runs the filter over a request and returns the response it wrote. */
	private static Capture run(RequestBoundaryFilter f, HttpServletRequest req, FilterChain chain) throws Exception {
		var c = capturing();
		f.doFilter(req, c.res(), chain);
		return c;
	}

	private static boolean verifies(String masked, HttpServletRequest req) {
		return TOKENS.verify(masked, TOKENS.expected(req, false));
	}

	private static void assertMinted(CsrfBoundary b, R r) throws Exception {
		var req = build(r);
		var chain = mock(FilterChain.class);
		run(new RequestBoundaryFilter(b), req, chain);
		verify(chain).doFilter(eq(req), any());
		verify(req).getSession(true);
	}

	private static void assertNotMinted(CsrfBoundary b, R r) throws Exception {
		var req = build(r);
		run(new RequestBoundaryFilter(b), req, mock(FilterChain.class));
		verify(req, never()).getSession(true);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Publishing, minting and refusals
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_publishesSupplierAndStringAttributes() throws Exception {
		var req = build(r("GET", "http", HOST, null));
		var chain = mock(FilterChain.class);
		run(filter(), req, chain);
		verify(chain).doFilter(eq(req), any());

		var token = req.getAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE);
		assertInstanceOf(String.class, token);
		assertTrue(verifies((String)token, req), () -> "published token does not verify: " + token);

		var supplier = req.getAttribute(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE);
		assertInstanceOf(Supplier.class, supplier);
		var supplied = ((Supplier<?>)supplier).get();
		assertInstanceOf(String.class, supplied);
		assertTrue(verifies((String)supplied, req), () -> "supplied token does not verify: " + supplied);

		assertEquals(LoopbackBoundary.DEFAULT_CSRF_HEADER, req.getAttribute(RequestBoundaryFilter.HEADER_ATTRIBUTE));
	}

	@Test void a02_supplierYieldsFreshMaskEachCall_bothVerify() throws Exception {
		var req = build(r("GET", "http", HOST, null));
		run(filter(), req, mock(FilterChain.class));
		var supplier = (Supplier<?>)req.getAttribute(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE);
		var first = (String)supplier.get();
		var second = (String)supplier.get();
		assertNotEquals(first, second);
		assertNotEquals(req.getAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE), first);
		assertTrue(verifies(first, req));
		assertTrue(verifies(second, req));
	}

	@ParameterizedTest
	@CsvSource({
		"Sec-Fetch-Mode, navigate",
		"Sec-Fetch-Dest, document",
		"Sec-Fetch-Dest, iframe"
	})
	void a03_looksLikePageLoadMintsSession(String header, String value) throws Exception {
		assertMinted(boundary().build(), get().h(header, value));
	}

	@Test void a03b_fetchMetadataThatIsNotAPageLoadNeverMints() throws Exception {
		// A script's fetch names its own mode and destination; Accept is not consulted once fetch metadata is present.
		assertNotMinted(boundary().build(),
			get().h("Sec-Fetch-Mode", "cors").h("Sec-Fetch-Dest", "empty").h("Accept", "text/html"));
	}

	@Test void a04_acceptHtmlWithoutFetchMetadataMints() throws Exception {
		assertMinted(boundary().build(), get().h("Accept", "text/html,application/xhtml+xml"));
	}

	@Test void a05_acceptStarStarNeverMints() throws Exception {
		assertNotMinted(boundary().build(), get().h("Accept", "*/*"));
		assertNotMinted(boundary().build(), get());
	}

	@Test void a06_acceptHtmlCurlMints_documented() throws Exception {
		// A command-line client that asks for HTML is indistinguishable from a page load, and gets a session.
		assertMinted(boundary().build(), get().h("User-Agent", "curl/8.5.0").h("Accept", "text/html"));
	}

	@Test void a06b_crossSiteNavigationMintsSession_lockedPendingDecision() throws Exception {
		// A cross-site top-level navigation currently creates a session, like any page load.  Not
		// changed: refusing to mint would leave a first-time visitor arriving from an external link (mail, chat) on a
		// page with no token, so every write from it fails until reload.  Locked until that trade-off is decided.
		assertMinted(boundary().build(), get().h("Sec-Fetch-Site", "cross-site").h("Sec-Fetch-Mode", "navigate"));
	}

	@Test void a07_createSessionsFalseNeverMints() throws Exception {
		var b = boundary().createSessions(false).build();
		assertNotMinted(b, get().h("Sec-Fetch-Mode", "navigate"));
		assertNotMinted(b, get().h("Accept", "text/html"));
	}

	@Test void a08_unsafeRequestNeverMints() throws Exception {
		var b = boundary().build();
		var pageLoad = new String[] { "Sec-Fetch-Mode", "navigate", "Accept", "text/html" };

		// Allowed: a write from the app's own page, and a non-browser write.
		var req = build(write(b).h(pageLoad[0], pageLoad[1]).h(pageLoad[2], pageLoad[3]));
		var chain = mock(FilterChain.class);
		run(new RequestBoundaryFilter(b), req, chain);
		verify(chain).doFilter(eq(req), any());
		verify(req, never()).getSession(true);
		assertNotMinted(b, r("DELETE", "http", HOST, null, "Accept", "text/html").noSession());

		// Refused: a cross-site write, and a safe request line claiming a write.
		assertNotMinted(b, r("POST", "http", HOST, EVIL, "Content-Type", JSON).noSession().h(pageLoad[0], pageLoad[1]));
		assertNotMinted(b, get().query("method=DELETE").h("Accept", "text/html"));
	}

	@Test void a09_perProcessNeverMints() throws Exception {
		var b = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		assertNotMinted(b, get().h("Sec-Fetch-Mode", "navigate"));
		assertNotMinted(b, get().h("Accept", "text/html"));
	}

	/** A boundary and a request it refuses for exactly the given reason. */
	private record Refusal(CsrfBoundary boundary, R request, int status) {}

	private static Refusal refusedFor(BoundaryReason reason) {
		var b = boundary().build();
		return switch (reason) {
			case HOST_MISSING -> new Refusal(b, r("POST", "http", null, ORIGIN, "Content-Type", JSON), 400);
			case HOST_MALFORMED -> new Refusal(b, r("POST", "http", "app.example:8080/x", ORIGIN, "Content-Type", JSON), 400);
			case HOST_NOT_ALLOWED -> new Refusal(boundary().allowedHosts("other.example").build(), r("GET", "http", HOST, null), 421);
			case ORIGIN_MISSING -> new Refusal(boundary().nonBrowser(NonBrowser.REJECT).build(), r("DELETE", "http", HOST, null), 403);
			case ORIGIN_NULL -> new Refusal(b, r("POST", "http", HOST, "null", "Content-Type", JSON), 403);
			case ORIGIN_MISMATCH -> new Refusal(b, r("POST", "http", HOST, EVIL, "Content-Type", JSON), 403);
			case FETCH_SITE_NOT_SAME_ORIGIN -> new Refusal(b, write(b).h("Sec-Fetch-Site", "cross-site"), 403);
			case SIMPLE_REQUEST_WITHOUT_ORIGIN -> new Refusal(b,
				r("POST", "http", HOST, null, "Content-Type", "application/x-www-form-urlencoded"), 403);
			case CONTENT_TYPE_NOT_JSON -> {
				var j = boundary().requireJson(true).build();
				yield new Refusal(j, r("POST", "http", HOST, ORIGIN, "Content-Type", "text/plain")
					.h(j.csrfHeader(), j.tokenFor(new R().build())), 415);
			}
			case CSRF_TOKEN_MISSING -> new Refusal(b, r("POST", "http", HOST, ORIGIN, "Content-Type", JSON), 403);
			case CSRF_TOKEN_MISMATCH -> new Refusal(b, r("POST", "http", HOST, ORIGIN, "Content-Type", JSON)
				.h(b.csrfHeader(), "0".repeat(128)), 403);
			case FORWARDED_HOST_MISSING -> forwarded(x -> x, r("POST", "http", HOST, ORIGIN, "Content-Type", JSON));
			case FORWARDED_PROTO_MISSING -> forwarded(x -> x,
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "app.example"));
			case FORWARDED_HOST_MALFORMED -> forwarded(x -> x,
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "a@b").h("X-Forwarded-Proto", "http"));
			case FORWARDED_PROTO_MALFORMED -> forwarded(x -> x,
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "ws"));
			case FORWARDED_HOPS_INSUFFICIENT -> forwarded(x -> x.trustedHops(2),
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "http"));
			case FORWARDED_HOPS_MISMATCH -> forwarded(x -> x,
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "a, b").h("X-Forwarded-Proto", "http"));
			case FORWARDED_CONTAINER_MISMATCH -> forwarded(x -> x.originSource(OriginSource.CONTAINER),
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https"));
			case FORWARDED_PEER_NOT_TRUSTED -> forwarded(x -> x.trustedProxies("10.0.0.0/8"),
				r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h("X-Forwarded-Host", "app.example").h("X-Forwarded-Proto", "https"));
			default -> throw new IllegalArgumentException("CsrfBoundary does not emit " + reason);
		};
	}

	/** A trusting boundary (after {@code tweak}) and the request it refuses; every forwarded refusal is a 403. */
	private static Refusal forwarded(java.util.function.UnaryOperator<CsrfBoundary.Builder> tweak, R request) {
		var builder = boundary().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED);
		return new Refusal(tweak.apply(builder).build(), request, 403);
	}

	@ParameterizedTest
	@EnumSource(value=BoundaryReason.class, names={
		"HOST_MISSING", "HOST_MALFORMED", "HOST_NOT_ALLOWED", "ORIGIN_MISSING", "ORIGIN_NULL", "ORIGIN_MISMATCH",
		"FETCH_SITE_NOT_SAME_ORIGIN", "SIMPLE_REQUEST_WITHOUT_ORIGIN", "CONTENT_TYPE_NOT_JSON", "CSRF_TOKEN_MISSING",
		"CSRF_TOKEN_MISMATCH", "FORWARDED_HOST_MISSING", "FORWARDED_PROTO_MISSING", "FORWARDED_HOST_MALFORMED",
		"FORWARDED_PROTO_MALFORMED", "FORWARDED_HOPS_INSUFFICIENT", "FORWARDED_HOPS_MISMATCH", "FORWARDED_CONTAINER_MISMATCH",
		"FORWARDED_PEER_NOT_TRUSTED"
	})
	void a10_rejectionNoStoreAndNoCacheOnAllReasons(BoundaryReason reason) throws Exception {
		var x = refusedFor(reason);
		var req = build(x.request().h("Sec-Fetch-Mode", "navigate"));
		var c = run(new RequestBoundaryFilter(x.boundary()), req, mock(FilterChain.class));
		verify(c.res()).setStatus(x.status());
		assertEquals(List.of(reason.name()), c.header(RequestBoundaryFilter.REJECTION_HEADER));
		assertEquals(List.of("no-store"), c.header("Cache-Control"));
		for (var h : List.of("Expires", "ETag", "Last-Modified", "Set-Cookie"))
			assertEquals(List.of(), c.header(h), h);
		verify(req, never()).getSession(true);
	}

	@Test void a11_rejectedRequestNeverReachesChain() throws Exception {
		var req = build(r("POST", "http", HOST, EVIL, "Content-Type", JSON));
		var chain = mock(FilterChain.class);
		run(filter(), req, chain);
		verifyNoInteractions(chain);
		verify(req, never()).setAttribute(eq(RequestBoundaryFilter.TOKEN_ATTRIBUTE), any());
		verify(req, never()).setAttribute(eq(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE), any());
	}

	@Test void a12_rejectionProducedWithBrokenSerializers() throws Exception {
		// No REST context, serializer or writer exists here: the refusal is built and written as bytes by the filter.
		var c = run(filter(), build(r("POST", "http", HOST, EVIL, "Content-Type", JSON)), mock(FilterChain.class));
		var body = c.body();
		assertTrue(body.startsWith("{\"reason\":\"ORIGIN_MISMATCH\",\"message\":\""), body);
		assertTrue(body.endsWith("\"}"), body);
		verify(c.res()).setContentType("application/json;charset=utf-8");
		verify(c.res()).setContentLength(body.getBytes(StandardCharsets.UTF_8).length);
		verify(c.res(), never()).getWriter();
	}

	@ParameterizedTest
	@EnumSource(DispatcherType.class)
	void a13_filterAppliesToWhateverDispatchItIsGiven(DispatcherType type) throws Exception {
		var refused = build(r("POST", "http", HOST, EVIL, "Content-Type", JSON));
		when(refused.getDispatcherType()).thenReturn(type);
		var chain = mock(FilterChain.class);
		var c = run(filter(), refused, chain);
		verifyNoInteractions(chain);
		assertEquals(List.of("ORIGIN_MISMATCH"), c.header(RequestBoundaryFilter.REJECTION_HEADER));

		var allowed = build(r("GET", "http", HOST, null));
		when(allowed.getDispatcherType()).thenReturn(type);
		var chain2 = mock(FilterChain.class);
		run(filter(), allowed, chain2);
		verify(chain2).doFilter(eq(allowed), any());
	}

	@Test void a14_optionsAddsNoCorsHeaders() throws Exception {
		// The chain adds nothing, so any Access-Control-* header on the response came from the filter.
		var allowed = build(r("OPTIONS", "http", HOST, EVIL, "Access-Control-Request-Method", "DELETE"));
		var chain = mock(FilterChain.class);
		var c1 = run(filter(), allowed, chain);
		verify(chain).doFilter(eq(allowed), any());

		var refused = build(r("OPTIONS", "http", HOST, EVIL, "Access-Control-Request-Method", "DELETE").query("method=DELETE"));
		var c2 = run(filter(), refused, mock(FilterChain.class));
		assertEquals(List.of("ORIGIN_MISMATCH"), c2.header(RequestBoundaryFilter.REJECTION_HEADER));

		for (var c : List.of(c1, c2)) {
			c.headers().keySet().forEach(k ->
				assertFalse(k.regionMatches(true, 0, "Access-Control-", 0, 15), () -> "CORS header added: " + k));
			verify(c.res(), never()).setHeader(startsWith("Access-Control-"), anyString());
			verify(c.res(), never()).addHeader(startsWith("Access-Control-"), anyString());
		}
	}
}
