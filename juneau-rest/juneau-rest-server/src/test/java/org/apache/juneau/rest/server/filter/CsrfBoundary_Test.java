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

import static org.apache.juneau.rest.server.filter.BoundaryReason.*;
import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.filter.CsrfBoundary.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link CsrfBoundary}.
 *
 * <p>
 * As with {@link LoopbackBoundary_Test}, the refusals carry the weight: each rule is asserted from both directions,
 * the legitimate request passing <i>and</i> a request breaking exactly that rule being refused with that rule's
 * reason and status.
 *
 * <p>
 * The request mocks stub {@code getHeaders(name)} alongside {@code getHeader(name)} for every header they carry, and
 * return <jk>null</jk> from {@code getHeaders} only for a header that is absent.  The boundary counts headers through
 * {@code getHeaders}, so a mock that stubbed only {@code getHeader} would make every happy-path request look like it
 * had no headers at all.
 *
 * @since 10.0.0
 */
class CsrfBoundary_Test extends TestBase {

	private static final byte[] KEY = new byte[32];
	static {
		for (var i = 0; i < KEY.length; i++)
			KEY[i] = (byte)(0x41 + i);
	}

	private static final CsrfTokens TOKENS = CsrfTokens.perSession().key(KEY);

	private static final CsrfBoundary B = CsrfBoundary.create().tokens(TOKENS).build();

	private static final String HOST = "app.example:8080";
	private static final String ORIGIN = "http://app.example:8080";
	private static final String SESSION = "s1";
	private static final String JSON = "application/json";

	//-----------------------------------------------------------------------------------------------------------------
	// Request fixtures
	//-----------------------------------------------------------------------------------------------------------------

	/** A mocked request, built up header by header; a header added twice is two headers. */
	static final class R {
		String method = "POST", scheme = "http", query, sessionId = SESSION, uri = "/api/pets", remote = "203.0.113.7";
		String serverName = "server.internal";
		// Left null (Mockito's default) so every existing HOST_MISSING row stays on the HTTP/1 path.
		String protocol;
		int serverPort = 8080;
		final Map<String,List<String>> headers = new LinkedHashMap<>();
		final Map<String,String> params = new LinkedHashMap<>();

		R method(String v) { method = v; return this; }
		R scheme(String v) { scheme = v; return this; }
		R query(String v) { query = v; return this; }
		R session(String v) { sessionId = v; return this; }
		R noSession() { sessionId = null; return this; }
		R uri(String v) { uri = v; return this; }
		R container(String name, int port) { serverName = name; serverPort = port; return this; }
		R protocol(String v) { protocol = v; return this; }
		R remote(String v) { remote = v; return this; }
		R param(String n, String v) { params.put(n, v); return this; }

		R h(String name, String value) {
			if (value != null)
				headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(value);
			return this;
		}

		R remove(String name) { headers.remove(name.toLowerCase(Locale.ROOT)); return this; }

		R token() { return h(B.csrfHeader(), CsrfBoundary_Test.token(sessionId)); }

		private String first(String name) {
			var v = headers.get(name.toLowerCase(Locale.ROOT));
			return v == null ? null : v.get(0);
		}

		HttpServletRequest build() {
			var r = mock(HttpServletRequest.class);
			when(r.getMethod()).thenReturn(method);
			when(r.getScheme()).thenReturn(scheme);
			when(r.getServerName()).thenReturn(serverName);
			when(r.getProtocol()).thenReturn(protocol);
			when(r.getServerPort()).thenReturn(serverPort);
			when(r.getQueryString()).thenReturn(query);
			when(r.getRequestURI()).thenReturn(uri);
			when(r.getRemoteAddr()).thenReturn(remote);
			when(r.getHeader(anyString())).thenAnswer(i -> first(i.getArgument(0)));
			when(r.getHeaders(anyString())).thenAnswer(i -> {
				var v = headers.get(((String)i.getArgument(0)).toLowerCase(Locale.ROOT));
				return v == null ? null : Collections.enumeration(v);
			});
			when(r.getHeaderNames()).thenAnswer(i -> Collections.enumeration(new ArrayList<>(headers.keySet())));
			when(r.getContentType()).thenAnswer(i -> first("Content-Type"));
			when(r.getParameter(anyString())).thenAnswer(i -> params.get(i.getArgument(0)));
			if (sessionId != null) {
				var s = mock(HttpSession.class);
				when(s.getId()).thenReturn(sessionId);
				when(r.getSession()).thenReturn(s);
				when(r.getSession(anyBoolean())).thenReturn(s);
			} else {
				when(r.getSession(false)).thenReturn(null);
				when(r.getSession(true)).thenAnswer(i -> mock(HttpSession.class));
				when(r.getSession()).thenAnswer(i -> mock(HttpSession.class));
			}
			return r;
		}
	}

	/** A request with the given method, scheme, {@code Host} and {@code Origin} (null means absent), plus name/value pairs. */
	static R r(String method, String scheme, String host, String origin, String...extra) {
		var r = new R().method(method).scheme(scheme).h("Host", host).h("Origin", origin);
		for (var i = 0; i < extra.length; i += 2)
			r.h(extra[i], extra[i + 1]);
		return r;
	}

	static HttpServletRequest req(String method, String scheme, String host, String origin, String...extra) {
		return r(method, scheme, host, origin, extra).build();
	}

	/** A legitimate same-origin JSON write from the app's own page in session {@link #SESSION}, as a builder. */
	static R write() {
		return r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).token();
	}

	/** A masked token for a page served in the given session. */
	static String token(String sessionId) {
		return B.tokenFor(new R().session(sessionId).build());
	}

	static void assertAllowed(CsrfBoundary b, R r) {
		var x = b.check(r.build());
		assertTrue(x.isAllowed(), () -> "expected allowed, got: " + x);
	}

	static void assertAllowed(R r) {
		assertAllowed(B, r);
	}

	static void assertRefused(CsrfBoundary b, R r, BoundaryReason reason, int status) {
		var x = b.check(r.build());
		assertFalse(x.isAllowed(), () -> "expected " + reason + ", got allowed");
		assertEquals(reason, x.reason(), x::toString);
		assertEquals(status, x.status(), x::toString);
		assertNotNull(x.message());
		assertFalse(x.message().isBlank());
	}

	static void assertRefused(R r, BoundaryReason reason, int status) {
		assertRefused(B, r, reason, status);
	}

	static void assertRefused(R r, BoundaryReason reason) {
		assertRefused(B, r, reason, 403);
	}

	/** Runs {@code r} with every record of the boundary's logger captured, at every level. */
	static List<LogRecord> captureLog(Runnable r) {
		var log = Logger.getLogger(CsrfBoundary.class.getName());
		var records = new ArrayList<LogRecord>();
		var h = new Handler() {
			@Override public void publish(LogRecord x) { records.add(x); }
			@Override public void flush() { /* Nothing buffered. */ }
			@Override public void close() { /* Nothing held. */ }
		};
		h.setLevel(Level.ALL);
		var oldLevel = log.getLevel();
		var oldParent = log.getUseParentHandlers();
		log.setLevel(Level.ALL);
		log.setUseParentHandlers(false);
		log.addHandler(h);
		try {
			r.run();
		} finally {
			log.removeHandler(h);
			log.setLevel(oldLevel);
			log.setUseParentHandlers(oldParent);
		}
		return records;
	}

	static long count(List<LogRecord> records, Level level) {
		return records.stream().filter(x -> x.getLevel() == level).count();
	}

	static String text(LogRecord x) {
		return x.getMessage();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Requests that pass
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_sameOriginPostHttpWithToken() {
		assertAllowed(write());
	}

	@Test void a02_sameOriginPostHttpsWithToken() {
		assertAllowed(r("POST", "https", "app.example", "https://app.example", "Content-Type", JSON).token());
	}

	@Test void a03_defaultPortEquivalence() {
		assertAllowed(r("POST", "https", "h:443", "https://h", "Content-Type", JSON).token());
		assertAllowed(r("POST", "https", "h", "https://h:443", "Content-Type", JSON).token());
		assertAllowed(r("POST", "http", "h", "http://h:80", "Content-Type", JSON).token());
		assertAllowed(r("POST", "http", "h:80", "http://h", "Content-Type", JSON).token());
	}

	@Test void a04_ipv6() {
		assertAllowed(r("POST", "http", "[::1]:8080", "http://[::1]:8080", "Content-Type", JSON).token());
	}

	@Test void a05_upperCaseHostAndScheme() {
		assertAllowed(r("POST", "HTTP", "APP.Example:8080", "HTTP://App.EXAMPLE:8080", "Content-Type", JSON).token());
	}

	@Test void a06_sameOriginFetchSiteNoOriginWithToken() {
		assertAllowed(r("POST", "http", HOST, null, "Sec-Fetch-Site", "same-origin", "Content-Type", JSON).token());
	}

	@Test void a07_nonBrowserDeleteNoOrigin() {
		assertAllowed(r("DELETE", "http", HOST, null).noSession());
	}

	@Test void a08_nonBrowserJsonPostNoOrigin() {
		assertAllowed(r("POST", "http", HOST, null, "Content-Type", JSON).noSession());
		assertAllowed(r("POST", "http", HOST, null, "Content-Type", "Application/JSON; charset=utf-8").noSession());
	}

	@Test void a09_nonBrowserXmlPutNoOrigin() {
		assertAllowed(r("PUT", "http", HOST, null, "Content-Type", "application/xml").noSession());
	}

	@Test void a10_safeMethodsAllowed() {
		// Not judged at all beyond allowedHosts: no Host, a cross-site Origin and no token still pass.
		for (var m : List.of("GET", "HEAD", "OPTIONS", "TRACE")) {
			assertAllowed(r(m, "http", null, "https://evil.example", "Sec-Fetch-Site", "cross-site").noSession());
			assertAllowed(r(m, "http", HOST, null));
		}
	}

	@Test void a12_requireJsonMixedCaseContentType() {
		var b = CsrfBoundary.create().tokens(TOKENS).requireJson(true).build();
		assertAllowed(b, write().remove("Content-Type").h("Content-Type", "Application/JSON; charset=utf-8"));
	}

	@Test void a13_tokenForNeverMints() {
		var withSession = new R().build();
		var t1 = B.tokenFor(withSession);
		assertTrue(t1.matches("[0-9a-f]{128}"), t1);
		verify(withSession, never()).getSession(true);
		verify(withSession, never()).getSession();

		var noSession = new R().noSession().build();
		var t2 = B.tokenFor(noSession);
		assertTrue(t2.matches("[0-9a-f]{128}"), t2);
		verify(noSession, never()).getSession(true);
		verify(noSession, never()).getSession();
	}

	@Test void a14_tokenForFreshPadEachCall() {
		var r = new R().build();
		var t1 = B.tokenFor(r);
		var t2 = B.tokenFor(r);
		assertNotEquals(t1, t2);
		assertAllowed(write().remove(B.csrfHeader()).h(B.csrfHeader(), t1));
		assertAllowed(write().remove(B.csrfHeader()).h(B.csrfHeader(), t2));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Origin matrix
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_crossOriginFormPostEachSafelistedType() {
		for (var ct : List.of("application/x-www-form-urlencoded", "multipart/form-data; boundary=x", "text/plain"))
			assertRefused(r("POST", "http", HOST, "https://evil.example", "Content-Type", ct).token(), ORIGIN_MISMATCH);
	}

	@Test void b02_crossOriginJsonPost() {
		assertRefused(r("POST", "http", HOST, "https://evil.example", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b03_siblingSubdomain() {
		assertRefused(r("POST", "http", HOST, "http://api.app.example:8080", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
		assertRefused(r("POST", "http", "a.example.com", "http://b.example.com", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b04_schemeDowngrade() {
		// Explicit equal ports, so only the scheme differs: dropping the scheme from the match would let this pass.
		assertRefused(r("POST", "https", "app.example:8443", "http://app.example:8443", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
		assertRefused(r("POST", "https", "app.example", "http://app.example", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b05_portMismatch() {
		assertRefused(r("POST", "http", HOST, "http://app.example:9090", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
		assertRefused(r("POST", "http", HOST, "http://app.example", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b06_originTrailingSlash() {
		assertRefused(r("POST", "http", HOST, ORIGIN + "/", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b07_originWithPath() {
		assertRefused(r("POST", "http", HOST, ORIGIN + "/api", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b08_originWithUserinfo() {
		assertRefused(r("POST", "http", HOST, "http://u@app.example:8080", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b09_originWithWhitespace() {
		for (var o : List.of(" " + ORIGIN, ORIGIN + " ", "http:// app.example:8080", ORIGIN + "\t"))
			assertRefused(r("POST", "http", HOST, o, "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b10_originWithQuery() {
		assertRefused(r("POST", "http", HOST, ORIGIN + "?x=1", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b11_blankOriginIsPresent() {
		// A JSON write with a valid token: treated as absent, this would be admitted as a non-browser request.
		assertRefused(r("POST", "http", HOST, "", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b12_whitespaceOnlyOriginIsPresent() {
		assertRefused(r("POST", "http", HOST, "   ", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b13_twoOriginHeaders() {
		// Both values are the right origin; two of them is still not one origin.
		assertRefused(write().h("Origin", ORIGIN), ORIGIN_MISMATCH);
		assertRefused(write().h("Origin", "https://evil.example"), ORIGIN_MISMATCH);
	}

	@Test void b14_originNullAndNULL() {
		for (var o : List.of("null", "NULL", "Null"))
			assertRefused(r("POST", "http", HOST, o, "Content-Type", JSON).token(), ORIGIN_NULL);
	}

	@Test void b15_fileAndChromeExtensionOrigins() {
		for (var o : List.of("file://", "file:///etc/passwd", "chrome-extension://abcdefghijklmnop"))
			assertRefused(r("POST", "http", HOST, o, "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b16_nonAsciiOriginAndIdnPunycode() {
		assertRefused(r("POST", "http", "xn--pp-uia.example", "http://äpp.example", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
		assertAllowed(r("POST", "http", "xn--pp-uia.example", "http://xn--pp-uia.example", "Content-Type", JSON).token());
	}

	@Test void b17_trailingDotHost() {
		assertRefused(r("POST", "http", "a.example.", "http://a.example", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
		assertRefused(r("POST", "http", "a.example", "http://a.example.", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void b18_zeroPaddedAndOutOfRangePorts() {
		for (var p : List.of(":080", ":0", ":65536", ":+80", ":-1")) {
			assertRefused(r("POST", "http", "h:80", "http://h" + p, "Content-Type", JSON).token(), ORIGIN_MISMATCH);
			assertRefused(r("POST", "http", "h" + p, "http://h", "Content-Type", JSON).token(), HOST_MALFORMED, 400);
		}
	}

	@Test void b19_originWithoutOriginHeaderButSecFetchCrossSite() {
		assertRefused(r("POST", "http", HOST, null, "Sec-Fetch-Site", "cross-site", "Content-Type", JSON).token(), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Sec-Fetch-Site matrix
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_crossSiteWithMatchingOrigin() {
		assertRefused(write().h("Sec-Fetch-Site", "cross-site"), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c02_sameSiteWithMatchingOrigin() {
		assertRefused(write().h("Sec-Fetch-Site", "same-site"), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c03_noneWithMatchingOrigin() {
		assertRefused(write().h("Sec-Fetch-Site", "none"), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c04_mixedCaseSameOrigin() {
		assertRefused(write().h("Sec-Fetch-Site", "Same-Origin"), FETCH_SITE_NOT_SAME_ORIGIN);
		assertAllowed(write().h("Sec-Fetch-Site", "same-origin"));
	}

	@Test void c05_internalSpaces() {
		assertRefused(write().h("Sec-Fetch-Site", "same- origin"), FETCH_SITE_NOT_SAME_ORIGIN);
		assertRefused(write().h("Sec-Fetch-Site", "same origin"), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c06_twoFetchSiteHeaders() {
		assertRefused(write().h("Sec-Fetch-Site", "same-origin").h("Sec-Fetch-Site", "same-origin"), FETCH_SITE_NOT_SAME_ORIGIN);
		assertRefused(r("POST", "http", HOST, null, "Content-Type", JSON, "Sec-Fetch-Site", "same-origin", "Sec-Fetch-Site", "cross-site").token(),
			FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c07_crossSiteNoOrigin() {
		assertRefused(r("POST", "http", HOST, null, "Sec-Fetch-Site", "cross-site", "Content-Type", JSON).token(), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c08_sameSiteNoOrigin() {
		assertRefused(r("POST", "http", HOST, null, "Sec-Fetch-Site", "same-site", "Content-Type", JSON).token(), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c09_noneNoOrigin() {
		assertRefused(r("DELETE", "http", HOST, null, "Sec-Fetch-Site", "none").token(), FETCH_SITE_NOT_SAME_ORIGIN);
	}

	@Test void c10_sameOriginNoOriginNeedsToken() {
		assertRefused(r("POST", "http", HOST, null, "Sec-Fetch-Site", "same-origin", "Content-Type", JSON), CSRF_TOKEN_MISSING);
		// A non-simple method is not admitted around the token once fetch metadata says it is a browser.
		assertRefused(r("DELETE", "http", HOST, null, "Sec-Fetch-Site", "same-origin"), CSRF_TOKEN_MISSING);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Host parsing
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_hostMissing() {
		assertRefused(write().remove("Host"), HOST_MISSING, 400);
		assertRefused(r("DELETE", "http", null, null), HOST_MISSING, 400);
	}

	@Test void d02_twoHostHeaders() {
		assertRefused(write().h("Host", HOST), HOST_MALFORMED, 400);
		assertRefused(write().h("Host", "evil.example"), HOST_MALFORMED, 400);
	}

	@Test void d03_hostCommaList() {
		assertRefused(write().remove("Host").h("Host", HOST + ", evil.example"), HOST_MALFORMED, 400);
	}

	@Test void d04_hostWithAt() {
		assertRefused(write().remove("Host").h("Host", "evil@" + HOST), HOST_MALFORMED, 400);
	}

	@Test void d05_hostWithZoneId() {
		assertRefused(r("POST", "http", "[fe80::1%25en0]:8080", "http://[fe80::1]:8080", "Content-Type", JSON).token(), HOST_MALFORMED, 400);
		assertRefused(r("POST", "http", "[fe80::1%en0]:8080", "http://[fe80::1]:8080", "Content-Type", JSON).token(), HOST_MALFORMED, 400);
	}

	@Test void d06_hostNonAscii() {
		assertRefused(r("POST", "http", "äpp.example", "http://xn--pp-uia.example", "Content-Type", JSON).token(), HOST_MALFORMED, 400);
	}

	@Test void d07_hostWithSlash() {
		assertRefused(write().remove("Host").h("Host", HOST + "/x"), HOST_MALFORMED, 400);
	}

	@Test void d08_hostControlCharacter() {
		assertRefused(write().remove("Host").h("Host", "app.example\u0007:8080"), HOST_MALFORMED, 400);
		assertRefused(write().remove("Host").h("Host", HOST + "\r\n"), HOST_MALFORMED, 400);
	}

	@Test void d09_ipv4MappedIpv6VsDotted() {
		assertRefused(r("POST", "http", "[::ffff:127.0.0.1]:8080", "http://127.0.0.1:8080", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
		assertRefused(r("POST", "http", "127.0.0.1:8080", "http://[::ffff:127.0.0.1]:8080", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void d10_ipv4ShortForm() {
		assertRefused(r("POST", "http", "127.1:8080", "http://127.0.0.1:8080", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	@Test void d11_unbracketedIpv6() {
		assertRefused(r("POST", "http", "::1:8080", "http://[::1]:8080", "Content-Type", JSON).token(), HOST_MALFORMED, 400);
		assertRefused(r("POST", "http", "[::1]:8080", "http://::1:8080", "Content-Type", JSON).token(), ORIGIN_MISMATCH);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) Method overrides
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_getMethodParamDeleteNoOrigin() {
		assertRefused(r("GET", "http", HOST, null).query("method=DELETE"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void e02_overrideHeaderPatchCrossSite() {
		assertRefused(r("GET", "http", HOST, null, "X-HTTP-Method-Override", "PATCH", "Sec-Fetch-Site", "cross-site").token(), FETCH_SITE_NOT_SAME_ORIGIN);
		assertRefused(r("GET", "http", HOST, "https://evil.example", "X-HTTP-Method-Override", "PATCH", "Sec-Fetch-Site", "cross-site").token(),
			ORIGIN_MISMATCH);
	}

	@Test void e03_methodParamMixedCase() {
		assertRefused(r("GET", "http", HOST, null).query("method=dElEtE"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
		assertRefused(r("GET", "http", HOST, null).query("MeThOd=delete"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void e04_malformedPercentEscape() {
		assertRefused(r("GET", "http", HOST, null).query("method=%ZZ"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
		assertRefused(r("GET", "http", HOST, null).query("method=DEL%"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void e05_getMethodDeleteNoOriginNoFetchMetadataRefused() {
		// The request a browser sends after a cross-origin redirect lands on ?method=DELETE: GET, no Origin, no fetch metadata.
		assertRefused(r("GET", "http", HOST, null).query("x=1&method=DELETE"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void e06_sameOriginGetMethodDeleteFromOwnPage() {
		assertAllowed(r("GET", "http", HOST, null, "Sec-Fetch-Site", "same-origin").query("method=DELETE").token());
		assertRefused(r("GET", "http", HOST, null, "Sec-Fetch-Site", "same-origin").query("method=DELETE"), CSRF_TOKEN_MISSING);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f) Non-browser policy
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_urlencodedPostNoOriginRefused() {
		assertRefused(r("POST", "http", HOST, null, "Content-Type", "application/x-www-form-urlencoded").token(), SIMPLE_REQUEST_WITHOUT_ORIGIN);
		assertRefused(r("POST", "http", HOST, null, "Content-Type", "multipart/form-data; boundary=x").token(), SIMPLE_REQUEST_WITHOUT_ORIGIN);
		assertRefused(r("post", "http", HOST, null, "Content-Type", "application/x-www-form-urlencoded").token(), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void f02_postNoContentTypeRefused() {
		assertRefused(r("POST", "http", HOST, null), SIMPLE_REQUEST_WITHOUT_ORIGIN);
		assertRefused(r("POST", "http", HOST, null, "Content-Type", ""), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void f03_twoContentTypeHeadersRefused() {
		// getHeader and getContentType report the first, application/json; only counting the headers sees the second.
		assertRefused(r("POST", "http", HOST, null, "Content-Type", JSON, "Content-Type", "text/plain"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void f04_textPlainCarryingJsonRefused() {
		assertRefused(r("POST", "http", HOST, null, "Content-Type", "text/plain;charset=UTF-8"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void f05_headOrGetNoOriginWithOverrideRefused() {
		assertRefused(r("GET", "http", HOST, null, "X-HTTP-Method-Override", "DELETE"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
		assertRefused(r("HEAD", "http", HOST, null, "X-Method-Override", "PUT"), SIMPLE_REQUEST_WITHOUT_ORIGIN);
	}

	@Test void f06_sendBeaconJsonCrossSiteOriginRefused() {
		assertRefused(r("POST", "http", HOST, "https://evil.example", "Sec-Fetch-Site", "cross-site", "Content-Type", JSON), ORIGIN_MISMATCH);
	}

	@Test void f07_sendBeaconJsonNoOriginNoFetchMetadataAdmitted_documentedResidualRisk() {
		assertAllowed(r("POST", "http", HOST, null, "Content-Type", JSON).noSession());
	}

	@Test void f12_getMethodDeleteJsonNoOriginNoFetchMetadataAdmitted_documentedResidualRisk() {
		// Same residual as f07: a browser cannot send this shape without Origin, because a custom Content-Type preflights.
		assertAllowed(r("GET", "http", HOST, null, "Content-Type", JSON).query("method=DELETE").noSession());
	}

	@Test void f08_policyReject() {
		var b = CsrfBoundary.create().tokens(TOKENS).nonBrowser(NonBrowser.REJECT).build();
		assertRefused(b, r("DELETE", "http", HOST, null).token(), ORIGIN_MISSING, 403);
		assertRefused(b, r("POST", "http", HOST, null, "Content-Type", JSON).token(), ORIGIN_MISSING, 403);
		assertRefused(b, r("POST", "http", HOST, null, "Content-Type", "text/plain").token(), ORIGIN_MISSING, 403);
		assertAllowed(b, write());
	}

	@Test void f09_policyRequireToken() {
		var b = CsrfBoundary.create().tokens(TOKENS).nonBrowser(NonBrowser.REQUIRE_TOKEN).build();
		assertAllowed(b, r("DELETE", "http", HOST, null).token());
		assertAllowed(b, r("POST", "http", HOST, null, "Content-Type", "text/plain").token());
		assertRefused(b, r("DELETE", "http", HOST, null), CSRF_TOKEN_MISSING, 403);
		assertRefused(b, r("DELETE", "http", HOST, null).h(B.csrfHeader(), token("other")), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void f10_policyAdmitNonSimpleDefault() {
		var b = CsrfBoundary.create().build();
		assertEquals(NonBrowser.ADMIT_NON_SIMPLE, b.nonBrowser());
		assertAllowed(b, r("DELETE", "http", HOST, null).noSession());
		assertRefused(b, r("POST", "http", HOST, null, "Content-Type", "text/plain").noSession(), SIMPLE_REQUEST_WITHOUT_ORIGIN, 403);
	}

	@Test void f11_requireJsonAppliesToAdmittedNonBrowser() {
		var b = CsrfBoundary.create().tokens(TOKENS).requireJson(true).build();
		assertRefused(b, r("PUT", "http", HOST, null, "Content-Type", "application/xml").noSession(), CONTENT_TYPE_NOT_JSON, 415);
		assertRefused(b, r("DELETE", "http", HOST, null).noSession(), CONTENT_TYPE_NOT_JSON, 415);
		assertAllowed(b, r("PUT", "http", HOST, null, "Content-Type", JSON).noSession());
		assertAllowed(B, r("PUT", "http", HOST, null, "Content-Type", "application/xml").noSession());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g) Token
	//-----------------------------------------------------------------------------------------------------------------

	private static R withToken(String value) {
		return write().remove(B.csrfHeader()).h(B.csrfHeader(), value);
	}

	@Test void g01_missingHeader() {
		assertRefused(write().remove(B.csrfHeader()), CSRF_TOKEN_MISSING);
		assertRefused(withToken(""), CSRF_TOKEN_MISSING);
	}

	@Test void g02_wrongValue() {
		assertRefused(withToken("0".repeat(128)), CSRF_TOKEN_MISMATCH);
		assertRefused(withToken("not-a-token"), CSRF_TOKEN_MISMATCH);
	}

	@Test void g03_anotherSessionsToken() {
		assertRefused(withToken(token("s2")), CSRF_TOKEN_MISMATCH);
	}

	@Test void g04_unmasked64Hex() {
		assertRefused(withToken(token(SESSION).substring(64)), CSRF_TOKEN_MISMATCH);
		assertRefused(withToken("a".repeat(64)), CSRF_TOKEN_MISMATCH);
	}

	@Test void g05_127And129Hex() {
		var t = token(SESSION);
		assertRefused(withToken(t.substring(1)), CSRF_TOKEN_MISMATCH);
		assertRefused(withToken(t + "0"), CSRF_TOKEN_MISMATCH);
	}

	@Test void g06_upperCaseHex() {
		var t = token(SESSION);
		assertNotEquals(t, t.toUpperCase(Locale.ROOT), "the fixture token must contain a hex letter");
		assertRefused(withToken(t.toUpperCase(Locale.ROOT)), CSRF_TOKEN_MISMATCH);
	}

	@Test void g07_twoTokenHeaders() {
		assertRefused(write().h(B.csrfHeader(), token(SESSION)), CSRF_TOKEN_MISMATCH);
	}

	@Test void g08_preLoginTokenAfterChangeSessionIdRefused() {
		var preLogin = token("before-login");
		assertRefused(withToken(preLogin).session("after-login"), CSRF_TOKEN_MISMATCH);
	}

	@Test void g09_preLoginTokenAfterLoginWithoutChangeSessionIdAccepted_documentsAdopterObligation() {
		var preLogin = token("same-id");
		assertAllowed(withToken(preLogin).session("same-id"));
	}

	@Test void g10_noSessionVsWrongTokenSameReason() {
		var noSession = B.check(withToken(token(SESSION)).noSession().build());
		var wrong = B.check(withToken(token("s2")).build());
		assertEquals(CSRF_TOKEN_MISMATCH, noSession.reason());
		assertEquals(CSRF_TOKEN_MISMATCH, wrong.reason());
		assertEquals(wrong.status(), noSession.status());
		assertEquals(wrong.message(), noSession.message(), "the message must not reveal whether a session exists");
		assertRefused(write().remove(B.csrfHeader()).noSession(), CSRF_TOKEN_MISSING);
	}

	@Test void g11_perProcessTwoInstancesDoNotCrossVerify() {
		var b1 = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		var b2 = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		var t1 = b1.tokenFor(new R().noSession().build());
		assertAllowed(b1, withToken(t1).noSession());
		assertRefused(b2, withToken(t1).noSession(), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void g12_tokenInQueryOrBodyParamIgnored() {
		var t = token(SESSION);
		assertRefused(write().remove(B.csrfHeader()).query("_csrf=" + t).param("_csrf", t), CSRF_TOKEN_MISSING);
		assertRefused(write().remove(B.csrfHeader()).query(B.csrfHeader() + "=" + t).param(B.csrfHeader(), t), CSRF_TOKEN_MISSING);
		assertRefused(r("POST", "http", HOST, ORIGIN, "Content-Type", "application/x-www-form-urlencoded").param("_csrf", t), CSRF_TOKEN_MISSING);
	}

	@Test void g13_sameOriginHtmlFormPost() {
		assertRefused(r("POST", "http", HOST, ORIGIN, "Content-Type", "application/x-www-form-urlencoded"), CSRF_TOKEN_MISSING);
		var b = CsrfBoundary.create().tokens(TOKENS).requireJson(true).build();
		assertRefused(b, r("POST", "http", HOST, ORIGIN, "Content-Type", "application/x-www-form-urlencoded"), CONTENT_TYPE_NOT_JSON, 415);
	}

	@Test void g14_sameOriginMultipartFormPost() {
		assertRefused(r("POST", "http", HOST, ORIGIN, "Content-Type", "multipart/form-data; boundary=x"), CSRF_TOKEN_MISSING);
		var b = CsrfBoundary.create().tokens(TOKENS).requireJson(true).build();
		assertRefused(b, r("POST", "http", HOST, ORIGIN, "Content-Type", "multipart/form-data; boundary=x"), CONTENT_TYPE_NOT_JSON, 415);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h) Spoofing and OriginSource
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_xForwardedHostIgnoredUnderHostHeader() {
		assertRefused(r("POST", "http", HOST, "https://evil.example", "X-Forwarded-Host", "evil.example", "X-Forwarded-Proto", "https",
			"Content-Type", JSON).token(), ORIGIN_MISMATCH);
		// The scheme already agrees here, so only reading X-Forwarded-Host for the host could make this pass.
		assertRefused(r("POST", "http", HOST, "http://evil.example", "X-Forwarded-Host", "evil.example", "Content-Type", JSON).token(),
			ORIGIN_MISMATCH);
		assertAllowed(write().h("X-Forwarded-Host", "evil.example").h("X-Forwarded-Proto", "https"));
	}

	@Test void h02_forwardedHeaderIgnoredUnderHostHeader() {
		assertRefused(r("POST", "http", HOST, "http://evil.example", "Forwarded", "host=evil.example;proto=http", "Content-Type", JSON).token(),
			ORIGIN_MISMATCH);
		assertAllowed(write().h("Forwarded", "host=evil.example;proto=https"));
	}

	private static final CsrfBoundary CONTAINER = CsrfBoundary.create().tokens(TOKENS).originSource(OriginSource.CONTAINER).build();

	@Test void h03_containerUsesGetSchemeServerNameServerPort() {
		assertAllowed(CONTAINER, r("POST", "https", "internal:8080", "https://app.example", "Content-Type", JSON).container("app.example", 443).token());
		assertAllowed(CONTAINER, r("POST", "https", "internal:8080", "https://app.example:8443", "Content-Type", JSON).container("APP.example", 8443).token());
		assertRefused(CONTAINER, r("POST", "https", "internal:8080", "http://internal:8080", "Content-Type", JSON).container("app.example", 443).token(),
			ORIGIN_MISMATCH, 403);
	}

	@Test void h04_containerStillRequiresHostHeader() {
		assertRefused(CONTAINER, r("POST", "https", null, "https://app.example", "Content-Type", JSON).container("app.example", 443).token(),
			HOST_MISSING, 400);
		assertRefused(CONTAINER, r("POST", "https", "a, b", "https://app.example", "Content-Type", JSON).container("app.example", 443).token(),
			HOST_MALFORMED, 400);
	}

	@Test void h05_containerBehindTlsTerminationRewritten() {
		assertAllowed(CONTAINER, r("POST", "https", "h", "https://h", "Content-Type", JSON).container("h", 443).token());
	}

	@Test void h06_hostHeaderBehindTlsTerminationSchemeMismatch() {
		CsrfBoundary.resetWarnings();
		var log = captureLog(() -> assertRefused(r("POST", "http", "h", "https://h", "Content-Type", JSON).token(), ORIGIN_MISMATCH));
		var warnings = log.stream().filter(x -> x.getLevel() == Level.WARNING).toList();
		assertSize(() -> log.stream().map(CsrfBoundary_Test::text).toList().toString(), 1, warnings);
		assertTrue(text(warnings.get(0)).contains("CONTAINER"), text(warnings.get(0)));
	}

	@Test void h07_containerBracketsUnbracketedIpv6ServerName() {
		// Tomcat reports an IPv6 server name without brackets.
		assertAllowed(CONTAINER, r("POST", "http", "[::1]:8080", "http://[::1]:8080", "Content-Type", JSON).container("::1", 8080).token());
		assertRefused(CONTAINER, r("POST", "http", "[::1]:8080", "http://[::2]:8080", "Content-Type", JSON).container("::1", 8080).token(),
			ORIGIN_MISMATCH, 403);
	}

	@Test void h08_containerIgnoresXForwardedHost() {
		assertAllowed(CONTAINER, r("POST", "https", "internal:8080", "https://app.example", "X-Forwarded-Host", "evil.example",
			"Content-Type", JSON).container("app.example", 443).token());
		assertRefused(CONTAINER, r("POST", "https", "internal:8080", "http://evil.example", "X-Forwarded-Host", "evil.example",
			"Content-Type", JSON).container("app.example", 443).token(), ORIGIN_MISMATCH, 403);
	}

	@Test void h09_containerKeepsAlreadyBracketedIpv6ServerName() {
		// Jetty reports the bracketed form; it must not be wrapped a second time.
		assertAllowed(CONTAINER, r("POST", "http", "[::1]:8080", "http://[::1]:8080", "Content-Type", JSON).container("[::1]", 8080).token());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i) allowedHosts
	//-----------------------------------------------------------------------------------------------------------------

	private static final CsrfBoundary LISTED = CsrfBoundary.create().tokens(TOKENS).allowedHosts("App.Example", "[::1]").build();

	@Test void i01_unlistedHostSafeGetIs421() {
		assertRefused(LISTED, r("GET", "http", "evil.example", null).noSession(), HOST_NOT_ALLOWED, 421);
		assertRefused(LISTED, r("HEAD", "http", "evil.example:8080", null).noSession(), HOST_NOT_ALLOWED, 421);
		assertAllowed(LISTED, r("GET", "http", HOST, null).noSession());
	}

	@Test void i02_unlistedHostUnsafeIs421() {
		assertRefused(LISTED, r("POST", "http", "evil.example", "https://evil.example", "Content-Type", "text/plain"), HOST_NOT_ALLOWED, 421);
	}

	@Test void i03_listedHostCaseInsensitive() {
		assertAllowed(LISTED, r("GET", "http", "APP.EXAMPLE", null));
		assertAllowed(LISTED, r("POST", "http", "APP.example:8080", "http://app.EXAMPLE:8080", "Content-Type", JSON).token());
		assertAllowed(LISTED, r("GET", "http", "[::1]:8080", null));
	}

	@Test void i04_portsIgnored() {
		assertAllowed(LISTED, r("GET", "http", "app.example:8080", null));
		assertAllowed(LISTED, r("GET", "http", "app.example:9090", null));
		assertAllowed(LISTED, r("GET", "http", "app.example", null));
	}

	@Test void i05_trailingDotNotEquated() {
		assertRefused(LISTED, r("GET", "http", "app.example.", null), HOST_NOT_ALLOWED, 421);
		var dotted = CsrfBoundary.create().tokens(TOKENS).allowedHosts("app.example.").build();
		assertRefused(dotted, r("GET", "http", "app.example", null), HOST_NOT_ALLOWED, 421);
	}

	@Test void i06_offByDefault() {
		assertAllowed(r("GET", "http", "evil.example", null));
		assertTrue(B.allowedHosts().isEmpty());
	}

	@Test void i07_rebindingScenario() {
		// After rebinding, the attacker's page is same-origin with itself: Origin equals Host, and it can read the token.
		var rebound = "rebind.attacker.example:8080";
		assertRefused(LISTED, r("GET", "http", rebound, null), HOST_NOT_ALLOWED, 421);
		assertRefused(LISTED, r("POST", "http", rebound, "http://" + rebound, "Content-Type", JSON).token(), HOST_NOT_ALLOWED, 421);
		assertAllowed(B, r("POST", "http", rebound, "http://" + rebound, "Content-Type", JSON).token());
	}

	@Test void i08_listWithMissingOrMalformedHostFailsClosed() {
		assertRefused(LISTED, r("GET", "http", null, null), HOST_MISSING, 400);
		assertRefused(LISTED, r("GET", "http", "app.example, evil.example", null), HOST_MALFORMED, 400);
	}

	@Test void i09_containerListUsesServerName() {
		var b = CsrfBoundary.create().tokens(TOKENS).originSource(OriginSource.CONTAINER).allowedHosts("app.example").build();
		assertAllowed(b, r("GET", "https", "internal:8080", null).container("app.example", 443));
		assertRefused(b, r("GET", "https", "app.example", null).container("rebind.attacker.example", 443), HOST_NOT_ALLOWED, 421);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j) Output and logging
	//-----------------------------------------------------------------------------------------------------------------

	private static final List<String> ATTACKER = List.of("\"quoted\"", "<script>zqxjmarkup</script>", "zqxj".repeat(100), "zqxjplain");

	@Test void j01_rejectionMessagesEchoNothing() {
		var refusals = new ArrayList<BoundaryResult>();
		for (var a : ATTACKER) {
			var attackHost = "zqxj" + Math.abs(a.hashCode()) + ".example";
			refusals.add(B.check(r("POST", "http", a, ORIGIN, "Content-Type", JSON).token().build()));
			refusals.add(B.check(r("POST", "http", HOST, a, "Content-Type", JSON).token().build()));
			refusals.add(B.check(r("POST", "http", HOST, "http://" + attackHost, "Content-Type", JSON).token().build()));
			refusals.add(B.check(r("POST", "http", HOST, "http://" + a, "Content-Type", JSON).token().build()));
			refusals.add(B.check(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON, "Sec-Fetch-Site", a).token().build()));
			refusals.add(B.check(r("POST", "http", HOST, null, "Sec-Fetch-Site", a, "Content-Type", JSON).token().build()));
			refusals.add(B.check(withToken(a).build()));
			refusals.add(B.check(withToken(a).noSession().build()));
			refusals.add(B.check(r("POST", "http", HOST, null, "Content-Type", "text/plain; charset=" + a).build()));
			refusals.add(B.check(r("POST", "http", HOST, ORIGIN, "Content-Type", JSON).h(B.csrfHeader(), a).h(B.csrfHeader(), a).build()));
			refusals.add(LISTED.check(r("GET", "http", attackHost, null).build()));
			refusals.add(CsrfBoundary.create().tokens(TOKENS).requireJson(true).build().check(write().remove("Content-Type").h("Content-Type", a).build()));
			refusals.add(CONTAINER.check(r("POST", "http", HOST, "http://" + attackHost + ":81", "Content-Type", JSON).container(attackHost, 80).token().build()));
			for (var x : refusals) {
				assertFalse(x.isAllowed(), x::toString);
				var m = x.message();
				assertFalse(m.contains("zqxj"), m);
				assertFalse(m.contains("app.example"), () -> "the self origin must not be printed: " + m);
				assertFalse(m.contains("8080"), m);
				assertFalse(m.contains(token(SESSION).substring(0, 16)), m);
			}
		}
		// Every reason this boundary can produce was exercised above.
		var seen = EnumSet.noneOf(BoundaryReason.class);
		refusals.forEach(x -> seen.add(x.reason()));
		seen.add(ORIGIN_NULL);  // Fixed value, covered by the loop below.
		assertTrue(seen.containsAll(EnumSet.of(HOST_MALFORMED, ORIGIN_MISMATCH, FETCH_SITE_NOT_SAME_ORIGIN, CSRF_TOKEN_MISMATCH,
			SIMPLE_REQUEST_WITHOUT_ORIGIN, HOST_NOT_ALLOWED, CONTENT_TYPE_NOT_JSON)), seen::toString);
		for (var x : List.of(B.check(r("POST", "http", HOST, "null", "Content-Type", JSON).build()),
				B.check(write().remove("Host").build()), B.check(write().remove(B.csrfHeader()).build()),
				CsrfBoundary.create().nonBrowser(NonBrowser.REJECT).build().check(r("DELETE", "http", HOST, null).build()))) {
			assertFalse(x.isAllowed());
			assertFalse(x.message().contains("app.example"), x.message());
		}
	}

	@Test void j02_toStringRedactsKeyAndToken() {
		var s = B.toString();
		assertTrue(s.contains("redacted"), s);
		assertFalse(s.toLowerCase(Locale.ROOT).contains(HexFormat.of().formatHex(KEY).substring(0, 16)), s);
		assertFalse(s.contains(Base64.getEncoder().encodeToString(KEY).substring(0, 12)), s);
		assertFalse(s.contains(new String(KEY, java.nio.charset.StandardCharsets.ISO_8859_1).substring(0, 8)), s);
		var t = token(SESSION);
		assertFalse(s.contains(t.substring(0, 16)), s);
		assertFalse(s.contains(t.substring(64, 80)), s);
		var p = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		var pt = p.tokenFor(new R().build());
		assertFalse(p.toString().contains(pt.substring(0, 16)), p::toString);
		assertTrue(p.toString().contains("redacted"), p::toString);
	}

	@Test void j03_fineLogOmitsQueryAndSecrets() {
		var log = captureLog(() -> B.check(withToken("SENTINEL-TOKEN").method("PUT").uri("/api/pets/7").query("secret=SENTINEL-QUERY")
			.h("Cookie", "JSESSIONID=SENTINEL-COOKIE").h("Authorization", "Basic SENTINEL-AUTH")
			.h("X-HTTP-Method-Override", "DELETE").build()));
		var fine = log.stream().filter(x -> x.getLevel() == Level.FINE).toList();
		assertSize(log::toString, 1, fine);
		var m = text(fine.get(0));
		assertTrue(m.contains("CSRF_TOKEN_MISMATCH"), m);
		assertTrue(m.contains("PUT"), m);
		assertTrue(m.contains("/api/pets/7"), m);
		assertTrue(m.contains("203.0.113.7"), m);
		for (var x : log)
			for (var s : List.of("SENTINEL", "secret="))
				assertFalse(text(x).contains(s), text(x));

		// The effective method is logged alongside the request-line one.
		var log2 = captureLog(() -> B.check(r("GET", "http", HOST, null).query("method=DELETE&q=SENTINEL-QUERY").build()));
		var m2 = text(log2.stream().filter(x -> x.getLevel() == Level.FINE).findFirst().orElseThrow());
		assertTrue(m2.contains("DELETE") && m2.contains("GET"), m2);
		assertFalse(m2.contains("SENTINEL") || m2.contains("q="), m2);

		// Path parameters, where a container puts a rewritten session id, are not logged either.
		var log3 = captureLog(() -> B.check(withToken("x").uri("/api;jsessionid=SENTINEL-SESSION").build()));
		for (var x : log3)
			assertFalse(text(x).contains("SENTINEL"), text(x));
	}

	@Test void j04_finestLogSanitizesAndTruncates() {
		var longOrigin = "http://evil.example/" + "a".repeat(500);
		var log = captureLog(() -> B.check(r("POST", "http", HOST, "http://evil\u0001.exa\u001bmple\u007f", "Content-Type", JSON).build()));
		var finest = log.stream().filter(x -> x.getLevel() == Level.FINEST).toList();
		assertFalse(finest.isEmpty(), log::toString);
		for (var x : finest) {
			var m = text(x);
			assertTrue(m.contains("evil.example"), m);
			assertTrue(m.chars().noneMatch(c -> c < 0x20 || c == 0x7f), m);
		}
		var log2 = captureLog(() -> B.check(r("POST", "http", HOST, longOrigin, "Content-Type", JSON).build()));
		var m2 = text(log2.stream().filter(x -> x.getLevel() == Level.FINEST).findFirst().orElseThrow());
		assertTrue(m2.contains("a".repeat(150)), m2);
		assertFalse(m2.contains("a".repeat(201)), m2);
		// Nothing below FINEST carries the raw Origin.
		for (var x : log2)
			if (x.getLevel() != Level.FINEST)
				assertFalse(text(x).contains("evil.example"), text(x));
	}

	@Test void j05_warnOncePerProcessPerPattern() {
		CsrfBoundary.resetWarnings();
		Supplier<R> schemeOnly = () -> r("POST", "http", "app.example:8443", "https://app.example:8443", "Content-Type", JSON).token();
		Supplier<R> portOnly = () -> r("POST", "http", "app.example:8080", "http://app.example:9090", "Content-Type", JSON).token();
		Supplier<R> missing = () -> write().remove(B.csrfHeader());
		var log = captureLog(() -> {
			for (var i = 0; i < 3; i++) {
				B.check(schemeOnly.get().build());
				B.check(portOnly.get().build());
			}
		});
		assertEquals(2, count(log, Level.WARNING), log.stream().map(CsrfBoundary_Test::text).toList()::toString);

		var log2 = captureLog(() -> {
			for (var i = 0; i < 9; i++)
				B.check(missing.get().build());
		});
		assertEquals(0, count(log2, Level.WARNING), "no warning before the tenth refusal");
		var log3 = captureLog(() -> {
			for (var i = 0; i < 15; i++)
				B.check(missing.get().build());
			B.check(schemeOnly.get().build());
			B.check(portOnly.get().build());
		});
		assertEquals(1, count(log3, Level.WARNING), log3.stream().map(CsrfBoundary_Test::text).toList()::toString);

		// Token-missing refusals whose Origin did not match, or that had none, do not count toward that warning.
		CsrfBoundary.resetWarnings();
		var log4 = captureLog(() -> {
			for (var i = 0; i < 12; i++) {
				B.check(r("POST", "http", HOST, null, "Sec-Fetch-Site", "same-origin", "Content-Type", JSON).build());
				B.check(r("POST", "http", HOST, "https://evil.example", "Content-Type", JSON).build());
			}
		});
		assertEquals(0, count(log4, Level.WARNING), log4.stream().map(CsrfBoundary_Test::text).toList()::toString);
	}

	@Test void j06_attackShapedMismatchStaysFine() {
		CsrfBoundary.resetWarnings();
		var log = captureLog(() -> {
			B.check(r("POST", "http", HOST, "https://evil.example", "Content-Type", JSON).token().build());
			B.check(r("POST", "http", HOST, "http://evil.example:8080", "Content-Type", JSON).token().build());
			B.check(r("POST", "http", HOST, "null", "Content-Type", JSON).token().build());
		});
		assertEquals(0, count(log, Level.WARNING), log.stream().map(CsrfBoundary_Test::text).toList()::toString);
		assertEquals(3, count(log, Level.FINE));
	}

	@Test void j07_noConfigDefaults() {
		var b = CsrfBoundary.create().build();
		assertInstanceOf(CsrfTokens.PerSession.class, b.tokens());
		assertEquals(NonBrowser.ADMIT_NON_SIMPLE, b.nonBrowser());
		assertEquals(OriginSource.HOST_HEADER, b.originSource());
		assertFalse(b.requireJson());
		assertEquals("X-Csrf-Token", b.csrfHeader());
		assertEquals(LoopbackBoundary.DEFAULT_CSRF_HEADER, b.csrfHeader());
		assertTrue(b.createSessions());
		assertTrue(b.allowedHosts().isEmpty());
		assertNotSame(b.tokens(), CsrfBoundary.create().build().tokens(), "each boundary gets its own key by default");
	}

	@Test void j08_builderRejectsBadInput() {
		var bad = CsrfBoundary.create();
		assertThrows(IllegalArgumentException.class, () -> bad.tokens(null));
		assertThrows(IllegalArgumentException.class, () -> bad.nonBrowser(null));
		assertThrows(IllegalArgumentException.class, () -> bad.originSource(null));
		assertThrows(IllegalArgumentException.class, () -> bad.csrfHeader(null));
		assertThrows(IllegalArgumentException.class, () -> bad.csrfHeader(""));
		assertThrows(IllegalArgumentException.class, () -> bad.csrfHeader("  "));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts((String[])null));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts((String)null));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts(""));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts("  "));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts("app.example", " "));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts("app.example:8080"));
		assertThrows(IllegalArgumentException.class, () -> bad.allowedHosts("a, b"));
		assertThrows(IllegalArgumentException.class, () -> B.check(null));
		assertThrows(IllegalArgumentException.class, () -> B.tokenFor(null));
		var b = CsrfBoundary.create().csrfHeader(" X-My-Token ").createSessions(false).build();
		assertEquals("X-My-Token", b.csrfHeader());
		assertFalse(b.createSessions());
	}

	@Test void j09_customCsrfHeader() {
		var b = CsrfBoundary.create().tokens(TOKENS).csrfHeader("X-My-Token").build();
		assertAllowed(b, write().remove(B.csrfHeader()).h("X-My-Token", token(SESSION)));
		assertRefused(b, write(), CSRF_TOKEN_MISSING, 403);
	}

	@Test void j09b_forwardedMessageUsesCorrectPlaceholder() {
		// Verify that forwardedMessage formats the BoundaryReason with %s, not {0}.
		// Test the default branch (invalid reason) to catch the format bug.
		var ex = assertThrows(IllegalArgumentException.class, () -> CsrfBoundary.forwardedMessage(CSRF_TOKEN_MISSING));
		assertEquals("Not a forwarded-host reason: CSRF_TOKEN_MISSING", ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// k) HTTP/2: no Host header, so :authority (the container's server name and port) stands in
	//-----------------------------------------------------------------------------------------------------------------

	private static final String H2 = "HTTP/2.0";

	/** An HTTP/2 write with no {@code Host}, addressed to {@code app.example:80} as the container reports it. */
	private static R h2(String origin) {
		return r("POST", "http", null, origin, "Content-Type", JSON).protocol(H2).container("app.example", 80).token();
	}

	@Test void k01_http2WithoutHostUsesAuthority() {
		assertAllowed(h2("http://app.example"));
		assertAllowed(CONTAINER, h2("http://app.example"));
		assertRefused(h2("http://evil.example"), ORIGIN_MISMATCH);
		assertRefused(CONTAINER, h2("http://evil.example"), ORIGIN_MISMATCH, 403);
	}

	@Test void k02_http11WithoutHostIsStill400() {
		// The mutation row: removing the exact-protocol guard in hostValues must fail this, with a matching server name.
		assertRefused(h2("http://app.example").protocol("HTTP/1.1"), HOST_MISSING, 400);
		assertRefused(CONTAINER, h2("http://app.example").protocol("HTTP/1.1"), HOST_MISSING, 400);
		assertRefused(h2("http://app.example").protocol("HTTP/1.0"), HOST_MISSING, 400);
	}

	@Test void k03_http2WithHostUsesHost() {
		// A real Host wins under HOST_HEADER; CONTAINER still judges the server name, exactly as on HTTP/1.
		assertAllowed(h2("http://other.example").h("Host", "other.example"));
		assertRefused(h2("http://app.example").h("Host", "other.example"), ORIGIN_MISMATCH);
		assertAllowed(CONTAINER, h2("http://app.example").h("Host", "other.example"));
		assertRefused(CONTAINER, h2("http://other.example").h("Host", "other.example"), ORIGIN_MISMATCH, 403);
	}

	@Test void k04_http2AllowedHostsAppliesToAuthority() {
		assertRefused(LISTED, r("GET", "http", null, null).protocol(H2).container("evil.example", 80), HOST_NOT_ALLOWED, 421);
		assertRefused(LISTED, h2("http://evil.example").container("evil.example", 80), HOST_NOT_ALLOWED, 421);
		assertAllowed(LISTED, r("GET", "http", null, null).protocol(H2).container("app.example", 80));
		assertAllowed(LISTED, h2("http://app.example"));
	}

	@Test void k05_http2BracketsBareIpv6ServerName() {
		// Tomcat reports an IPv6 server name without brackets.
		assertAllowed(h2("http://[::1]:8080").container("::1", 8080));
		assertAllowed(h2("http://[::1]:8080").container("[::1]", 8080));
		assertRefused(h2("http://[::2]:8080").container("::1", 8080), ORIGIN_MISMATCH);
	}

	@Test void k06_http2WithoutAnyAuthorityIsHostMissing() {
		for (var name : new String[]{null, "", "  "}) {
			var x = B.check(h2("http://app.example").container(name, 80).build());
			assertEquals(HOST_MISSING, x.reason(), String.valueOf(name));
			assertEquals(400, x.status());
			assertTrue(x.message().contains(":authority"), x.message());
		}
	}

	@Test void k07_http2WithTwoHostsIsMalformed() {
		assertRefused(h2("http://app.example").h("Host", "a.example").h("Host", "b.example"), HOST_MALFORMED, 400);
	}

	@Test void k08_onlyTheExactProtocolStringFallsBack() {
		for (var protocol : new String[]{"HTTP/2", "http/2.0", "HTTP/2.0 ", "HTTP/3.0", null})
			assertRefused(h2("http://app.example").protocol(protocol), HOST_MISSING, 400);
	}

	@Test void k09_http2PortFollowsTheContainer() {
		assertAllowed(h2("http://app.example:8080").container("app.example", 8080));
		assertRefused(h2("http://app.example:8080").container("app.example", 9090), ORIGIN_MISMATCH);
		assertAllowed(h2("https://app.example").scheme("https").container("app.example", 443));
		// A port the container does not report: the name alone is judged, and the scheme default applies.
		assertAllowed(h2("http://app.example").container("app.example", 0));
	}

	@Test void k10_refusalLogMarksASynthesisedHost() {
		var log = captureLog(() -> B.check(h2("http://evil.example").build()));
		var finest = log.stream().filter(x -> x.getLevel() == Level.FINEST).map(CsrfBoundary_Test::text).toList();
		assertSize(finest::toString, 1, finest);
		assertTrue(finest.get(0).contains("Host=(h2 :authority) app.example:80"), finest.get(0));
		var real = captureLog(() -> B.check(h2("http://evil.example").h("Host", "app.example:80").build()));
		assertFalse(real.stream().map(CsrfBoundary_Test::text).anyMatch(t -> t.contains("(h2 :authority)")));
	}
}
