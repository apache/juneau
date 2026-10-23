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
import static org.apache.juneau.rest.server.filter.BoundaryReason.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link LoopbackBoundary}.
 *
 * <p>
 * The negative cases carry the weight here.  A boundary with only positive tests passes just as happily when a
 * check has been accidentally removed, so every rule below is asserted from both directions: the legitimate
 * request must succeed, <i>and</i> a request violating exactly that one rule must be refused with the specific
 * reason and status that rule is supposed to produce.
 *
 * @since 10.0.0
 */
class LoopbackBoundary_Test extends TestBase {

	private static final String AUTHORITY = "127.0.0.1:8790";
	private static final String ORIGIN = "http://127.0.0.1:8790";
	private static final SynchronizerToken TOKEN = SynchronizerToken.of("the-real-token");

	private static final LoopbackBoundary BOUNDARY = LoopbackBoundary.create()
		.authority(AUTHORITY)
		.token(TOKEN)
		.build();

	/**
	 * A fully legitimate write request: correct {@code Host}, correct {@code Origin}, same-origin fetch metadata,
	 * JSON content type, and this server's token.  Each negative test below is this request with exactly one
	 * header spoiled, so a failure isolates the rule that broke.
	 */
	private static Map<String,String> goodWriteHeaders() {
		Map<String,String> m = map("Host", AUTHORITY, "Origin", ORIGIN, "Sec-Fetch-Site", "same-origin", "X-Csrf-Token", TOKEN.value());
		return m;
	}

	private static HttpServletRequest req(String method, Map<String,String> headers) {
		return req(method, headers, "application/json");
	}

	private static HttpServletRequest req(String method, Map<String,String> headers, String contentType) {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn(method);
		when(r.getContentType()).thenReturn(contentType);
		headers.forEach((k, v) -> when(r.getHeader(k)).thenReturn(v));
		return r;
	}

	/** The legitimate write request, optionally with one header replaced or (on a null value) removed. */
	private static HttpServletRequest write(String spoiledHeader, String value) {
		var h = goodWriteHeaders();
		if (spoiledHeader != null) {
			if (value == null)
				h.remove(spoiledHeader);
			else
				h.put(spoiledHeader, value);
		}
		return req("POST", h);
	}

	private static void assertRejected(BoundaryResult r, BoundaryReason expectedReason, int expectedStatus) {
		assertFalse(r.isAllowed(), () -> "expected a rejection, got: " + r);
		assertEquals(expectedReason, r.reason());
		assertEquals(expectedStatus, r.status());
		assertNotNull(r.message(), "a rejection must carry a message; a silent refusal is not diagnosable");
		assertFalse(r.message().isBlank());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) The legitimate path still works
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_legitimateWrite_allowed() {
		assertTrue(BOUNDARY.check(write(null, null)).isAllowed());
	}

	@Test void a02_legitimateWrite_withCharsetParameterOnContentType_allowed() {
		assertTrue(BOUNDARY.check(req("POST", goodWriteHeaders(), "application/json;charset=utf-8")).isAllowed());
	}

	@Test void a03_legitimateWrite_withoutSecFetchSite_allowed() {
		// Absence is tolerated so a non-browser client used during development is not broken.
		assertTrue(BOUNDARY.check(write("Sec-Fetch-Site", null)).isAllowed());
	}

	@Test void a04b_selfCallHeaders_satisfyTheBoundary() {
		// The headers handed to an in-process loopback caller must actually pass, or the "no path exemptions"
		// stance is unimplementable and the next person adds one.  Host comes from the request URI, not the map.
		var h = new LinkedHashMap<String,String>();
		h.put("Host", AUTHORITY);
		h.putAll(BOUNDARY.selfCallHeaders());
		assertTrue(BOUNDARY.check(req("POST", h)).isAllowed());
	}

	@Test void a04c_selfCallHeaders_omitHostAndCarryNoSurprises() {
		var h = BOUNDARY.selfCallHeaders();
		assertEquals(Set.of("Origin", "X-Csrf-Token"), h.keySet());
		assertEquals(ORIGIN, h.get("Origin"));
		assertEquals(TOKEN.value(), h.get("X-Csrf-Token"));
	}

	@Test void a04d_selfCallHeaders_useTheConfiguredCsrfHeaderName() {
		var b = LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).csrfHeader("X-App-Token").build();
		assertEquals(Set.of("Origin", "X-App-Token"), b.selfCallHeaders().keySet());
	}

	@Test void a04_legitimateRead_allowed_andNeedsNoOriginContentTypeOrToken() {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getHeader("Host")).thenReturn(AUTHORITY);
		assertTrue(BOUNDARY.check(r).isAllowed());
	}

	@Test void a05_everyWriteMethodTakesTheWriteChecks() {
		for (var m : List.of("POST", "PUT", "PATCH", "DELETE")) {
			var h = goodWriteHeaders();
			h.remove("X-Csrf-Token");
			assertRejected(BOUNDARY.check(req(m, h)), CSRF_TOKEN_MISSING, 403);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Host — enforced on every request, including reads.  This is the DNS-rebinding check.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_mismatchedHostOnWrite_rejected() {
		assertRejected(BOUNDARY.check(write("Host", "evil.example")), HOST_MISMATCH, 421);
	}

	@Test void b02_mismatchedHostOnRead_rejected() {
		// The whole reason Host is not scoped to writes: a rebound page reading a data table is already an
		// exfiltration problem.
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getHeader("Host")).thenReturn("evil.example");
		assertRejected(BOUNDARY.check(r), HOST_MISMATCH, 421);
	}

	@Test void b03_absentHost_rejected() {
		assertRejected(BOUNDARY.check(write("Host", null)), HOST_MISMATCH, 421);
	}

	@Test void b04_reboundHostWithCorrectOriginAndValidToken_rejected() {
		// The DNS-rebinding shape specifically: after rebinding the attacker's page is same-origin with itself,
		// so it can scrape the real token out of this application's own HTML and present consistent fetch
		// metadata.  Only Host still gives it away.
		var h = goodWriteHeaders();
		h.put("Host", "evil.example");
		h.put("Origin", "http://evil.example");
		h.put("Sec-Fetch-Site", "same-origin");
		h.put("X-Csrf-Token", TOKEN.value());
		assertRejected(BOUNDARY.check(req("POST", h)), HOST_MISMATCH, 421);
	}

	@Test void b05_localhostSpellingOfTheSamePort_rejected() {
		// One canonical spelling by decision; localhost:8790 is not 127.0.0.1:8790.
		assertRejected(BOUNDARY.check(write("Host", "localhost:8790")), HOST_MISMATCH, 421);
	}

	@Test void b06_hostPrefixingOursOnAnAttackerDomain_rejected() {
		// Guards against a prefix/startsWith comparison creeping in: 127.0.0.1:8790.evil.example is a name an
		// attacker can register and resolve to anything, and it starts with our authority.
		assertRejected(BOUNDARY.check(write("Host", AUTHORITY + ".evil.example")), HOST_MISMATCH, 421);
	}

	@Test void b07_hostWithOurAuthorityAsASuffix_rejected() {
		assertRejected(BOUNDARY.check(write("Host", "evil.example." + AUTHORITY)), HOST_MISMATCH, 421);
	}

	@Test void b08_hostWithTrailingWhitespace_rejected() {
		assertRejected(BOUNDARY.check(write("Host", AUTHORITY + " ")), HOST_MISMATCH, 421);
	}

	@Test void b09_hostMissingThePort_rejected() {
		assertRejected(BOUNDARY.check(write("Host", "127.0.0.1")), HOST_MISMATCH, 421);
	}

	@Test void b10_hostOnADifferentPort_rejected() {
		// Ports matter: another loopback application on a different port is a different application.
		assertRejected(BOUNDARY.check(write("Host", "127.0.0.1:3000")), HOST_MISMATCH, 421);
	}

	@Test void b11_hostCaseIsInsignificant() {
		var b = LoopbackBoundary.create().authority("LocalHost:8790").token(TOKEN).build();
		var h = goodWriteHeaders();
		h.put("Host", "localhost:8790");
		h.put("Origin", "http://LocalHost:8790");
		assertTrue(b.check(req("POST", h)).isAllowed());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Origin — required on writes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_foreignOrigin_rejected() {
		assertRejected(BOUNDARY.check(write("Origin", "http://evil.example")), ORIGIN_MISMATCH, 403);
	}

	@Test void c02_absentOrigin_rejected() {
		// Absent must not mean "skip the check" — otherwise omitting the header is the bypass.
		assertRejected(BOUNDARY.check(write("Origin", null)), ORIGIN_MISSING, 403);
	}

	@Test void c03_blankOrigin_rejected() {
		assertRejected(BOUNDARY.check(write("Origin", "   ")), ORIGIN_MISSING, 403);
	}

	@Test void c04_nullOriginLiteral_rejected() {
		// Browsers send the literal string "null" for some opaque origins (e.g. a sandboxed iframe).
		assertRejected(BOUNDARY.check(write("Origin", "null")), ORIGIN_MISMATCH, 403);
	}

	@Test void c05_localhostSpellingOfOrigin_rejected() {
		assertRejected(BOUNDARY.check(write("Origin", "http://localhost:8790")), ORIGIN_MISMATCH, 403);
	}

	@Test void c06_httpsSpellingOfOrigin_rejected() {
		assertRejected(BOUNDARY.check(write("Origin", "https://127.0.0.1:8790")), ORIGIN_MISMATCH, 403);
	}

	@Test void c07_originWithTrailingSlash_rejected() {
		assertRejected(BOUNDARY.check(write("Origin", ORIGIN + "/")), ORIGIN_MISMATCH, 403);
	}

	@Test void c08_originPrefixingOursOnAnAttackerDomain_rejected() {
		// Guards against a substring/startsWith comparison creeping in.
		assertRejected(BOUNDARY.check(write("Origin", ORIGIN + ".evil.example")), ORIGIN_MISMATCH, 403);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) Sec-Fetch-Site — absent or same-origin
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_crossSiteFetchMetadata_rejected() {
		assertRejected(BOUNDARY.check(write("Sec-Fetch-Site", "cross-site")), FETCH_SITE_NOT_SAME_ORIGIN, 403);
	}

	@Test void d02_sameSiteButNotSameOrigin_rejected() {
		assertRejected(BOUNDARY.check(write("Sec-Fetch-Site", "same-site")), FETCH_SITE_NOT_SAME_ORIGIN, 403);
	}

	@Test void d03_userInitiatedNavigation_rejected() {
		assertRejected(BOUNDARY.check(write("Sec-Fetch-Site", "none")), FETCH_SITE_NOT_SAME_ORIGIN, 403);
	}

	@Test void d04_rejectionMessageDoesNotEchoTheCallerSuppliedValue() {
		var r = BOUNDARY.check(write("Sec-Fetch-Site", "<script>alert(1)</script>"));
		assertFalse(r.message().contains("<script>"),
			() -> "a caller-controlled value must not be echoed into a rejection message: " + r.message());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) Content type — JSON only, which is what excludes the no-preflight cross-origin form POST
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_formUrlEncoded_rejected() {
		// The important one: this content type is what makes a plain cross-origin <form> POST possible with no
		// preflight and no JavaScript at all.
		assertRejected(BOUNDARY.check(req("POST", goodWriteHeaders(), "application/x-www-form-urlencoded")),
			CONTENT_TYPE_NOT_JSON, 415);
	}

	@Test void e02_multipartFormData_rejected() {
		assertRejected(BOUNDARY.check(req("POST", goodWriteHeaders(), "multipart/form-data; boundary=x")),
			CONTENT_TYPE_NOT_JSON, 415);
	}

	@Test void e03_textPlain_rejected() {
		assertRejected(BOUNDARY.check(req("POST", goodWriteHeaders(), "text/plain")), CONTENT_TYPE_NOT_JSON, 415);
	}

	@Test void e04_absentContentType_rejected() {
		assertRejected(BOUNDARY.check(req("POST", goodWriteHeaders(), null)), CONTENT_TYPE_NOT_JSON, 415);
	}

	@Test void e05_jsonSuffixedType_rejected() {
		assertRejected(BOUNDARY.check(req("POST", goodWriteHeaders(), "application/problem+json")),
			CONTENT_TYPE_NOT_JSON, 415);
	}

	@Test void e06_contentTypeCaseAndWhitespaceAreInsignificant() {
		assertTrue(BOUNDARY.check(req("POST", goodWriteHeaders(), " APPLICATION/JSON ; charset=UTF-8")).isAllowed());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f) CSRF token
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_missingToken_rejected() {
		assertRejected(BOUNDARY.check(write("X-Csrf-Token", null)), CSRF_TOKEN_MISSING, 403);
	}

	@Test void f02_blankToken_rejected() {
		assertRejected(BOUNDARY.check(write("X-Csrf-Token", "")), CSRF_TOKEN_MISSING, 403);
	}

	@Test void f03_wrongToken_rejected() {
		assertRejected(BOUNDARY.check(write("X-Csrf-Token", "not-the-token")), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void f04_tokenFromADifferentServerInstance_rejected() {
		// "A token from a different session": each boundary instance holds an independent secret, so a token
		// minted alongside another instance is not this one's.
		var other = LoopbackBoundary.create().authority(AUTHORITY).token(SynchronizerToken.generate()).build();
		assertRejected(BOUNDARY.check(write("X-Csrf-Token", other.token().value())), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void f05_staleTokenAfterARestart_rejected() {
		// Simulates a page held open across a restart: the value it embedded was minted by the previous process.
		var beforeRestart = SynchronizerToken.generate();
		var afterRestart = LoopbackBoundary.create().authority(AUTHORITY).token(SynchronizerToken.generate()).build();
		var h = goodWriteHeaders();
		h.put("X-Csrf-Token", beforeRestart.value());
		assertRejected(afterRestart.check(req("POST", h)), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void f06_tokenTruncatedByOneCharacter_rejected() {
		var v = TOKEN.value();
		assertRejected(BOUNDARY.check(write("X-Csrf-Token", v.substring(0, v.length() - 1))), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void f07_tokenWithTrailingWhitespace_rejected() {
		assertRejected(BOUNDARY.check(write("X-Csrf-Token", TOKEN.value() + " ")), CSRF_TOKEN_MISMATCH, 403);
	}

	@Test void f08_rejectionMessageDoesNotRevealTheServersToken() {
		var r = BOUNDARY.check(write("X-Csrf-Token", "not-the-token"));
		assertFalse(r.message().contains(TOKEN.value()),
			() -> "a rejection message must not leak the server's token: " + r.message());
	}

	@Test void f09_customCsrfHeaderName_isTheOneRequired() {
		var b = LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).csrfHeader("X-Console-Csrf").build();
		var h = goodWriteHeaders();  // carries the default header name, not the configured one
		assertRejected(b.check(req("POST", h)), CSRF_TOKEN_MISSING, 403);
		h.put("X-Console-Csrf", TOKEN.value());
		assertTrue(b.check(req("POST", h)).isAllowed());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g) Check ordering — the first failure reported is the one whose refusal is most informative
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_hostIsCheckedBeforeAnyWriteRule() {
		var h = goodWriteHeaders();
		h.put("Host", "evil.example");
		h.remove("Origin");
		h.remove("X-Csrf-Token");
		assertRejected(BOUNDARY.check(req("POST", h, "text/plain")), HOST_MISMATCH, 421);
	}

	@Test void g02_originIsCheckedBeforeContentTypeAndToken() {
		var h = goodWriteHeaders();
		h.put("Origin", "http://evil.example");
		h.remove("X-Csrf-Token");
		assertRejected(BOUNDARY.check(req("POST", h, "text/plain")), ORIGIN_MISMATCH, 403);
	}

	@Test void g03_contentTypeIsCheckedBeforeToken() {
		var h = goodWriteHeaders();
		h.remove("X-Csrf-Token");
		assertRejected(BOUNDARY.check(req("POST", h, "text/plain")), CONTENT_TYPE_NOT_JSON, 415);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g, continued) The effective method: a safe request line claiming a write through an override is judged as that write
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_getWithMethodParamNeedsToken() {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getQueryString()).thenReturn("method=DELETE");
		when(r.getHeader("Host")).thenReturn(AUTHORITY);
		assertRejected(BOUNDARY.check(r), ORIGIN_MISSING, 403);
	}

	@Test void g02_getWithOverrideHeaderNeedsToken() {
		var h = goodWriteHeaders();
		h.remove("X-Csrf-Token");
		h.put("X-HTTP-Method-Override", "PUT");
		assertRejected(BOUNDARY.check(req("GET", h)), CSRF_TOKEN_MISSING, 403);
	}

	@Test void g03_plainGetStillAllowedAndPublishes() {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getQueryString()).thenReturn("page=2&sort=name");
		when(r.getHeader("Host")).thenReturn(AUTHORITY);
		assertTrue(BOUNDARY.check(r).isAllowed());
		assertEquals(TOKEN.value(), BOUNDARY.tokenFor(r));
		assertEquals(LoopbackBoundary.DEFAULT_CSRF_HEADER, BOUNDARY.csrfHeader());
	}

	@Test void g04_nullQueryStringStillJudged() {
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("GET");
		when(r.getHeader("Host")).thenReturn(AUTHORITY);
		when(r.getQueryString()).thenReturn(null);
		assertTrue(assertDoesNotThrow(() -> BOUNDARY.check(r)).isAllowed());
	}

	@Test void g05_getWithMethodParamAndGoodWriteHeadersIsAllowed() {
		var r = req("GET", goodWriteHeaders());
		when(r.getQueryString()).thenReturn("method=DELETE");
		assertTrue(BOUNDARY.check(r).isAllowed());
	}

	@Test void g06_getWithOverrideHeaderAndGoodWriteHeadersIsAllowed() {
		var h = goodWriteHeaders();
		h.put("X-HTTP-Method-Override", "PUT");
		assertTrue(BOUNDARY.check(req("GET", h)).isAllowed());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h) Method classification — fails closed on anything unrecognized
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_safeMethods() {
		for (var m : List.of("GET", "HEAD", "OPTIONS", "TRACE", "get", "head"))
			assertFalse(LoopbackBoundary.isStateChanging(m), m);
	}

	@Test void h02_writeMethods() {
		for (var m : List.of("POST", "PUT", "PATCH", "DELETE", "post", "LOCK", "PROPPATCH"))
			assertTrue(LoopbackBoundary.isStateChanging(m), m);
	}

	@Test void h03_unknownAndNullMethodsAreTreatedAsWrites() {
		// Fail closed: a method this framework does not recognize must not skip the write checks.
		assertTrue(LoopbackBoundary.isStateChanging(null));
		assertTrue(LoopbackBoundary.isStateChanging("SOMETHING-NEW"));
	}

	@Test void h04_optionsPreflightTakesOnlyTheHostCheck() {
		// A preflight carries a cross-origin Origin by definition and must not be refused for it; the browser
		// blocks the real request because this application answers no CORS headers.
		var r = mock(HttpServletRequest.class);
		when(r.getMethod()).thenReturn("OPTIONS");
		when(r.getHeader("Host")).thenReturn(AUTHORITY);
		when(r.getHeader("Origin")).thenReturn("http://evil.example");
		assertTrue(BOUNDARY.check(r).isAllowed());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i) Configuration and accessors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_originIsDerivedFromAuthority() {
		assertEquals(AUTHORITY, BOUNDARY.authority());
		assertEquals(ORIGIN, BOUNDARY.origin());
		assertEquals(LoopbackBoundary.DEFAULT_CSRF_HEADER, BOUNDARY.csrfHeader());
		assertSame(TOKEN, BOUNDARY.token());
	}

	@Test void i02_hostAndPortConvenienceForm() {
		var b = LoopbackBoundary.create().authority("127.0.0.1", 8877).token(TOKEN).build();
		assertEquals("127.0.0.1:8877", b.authority());
		assertEquals("http://127.0.0.1:8877", b.origin());
	}

	@Test void i03_authorityRejectsASchemeOrPath() {
		var b = LoopbackBoundary.create();
		assertThrows(IllegalArgumentException.class, () -> b.authority("http://127.0.0.1:8790"));
		assertThrows(IllegalArgumentException.class, () -> b.authority("127.0.0.1:8790/rest"));
	}

	@Test void i03b_authorityRejectsSchemeOrPathWithCorrectMessageFormat() {
		var b = LoopbackBoundary.create();
		var ex = assertThrows(IllegalArgumentException.class, () -> b.authority("http://127.0.0.1:8790"));
		assertEquals("Argument 'value' must be a bare host and port with no scheme or path: 'http://127.0.0.1:8790'.", ex.getMessage());
	}

	@Test void i04_authorityRejectsNullAndBlank() {
		var b = LoopbackBoundary.create();
		assertThrows(IllegalArgumentException.class, () -> b.authority(null));
		assertThrows(IllegalArgumentException.class, () -> b.authority("  "));
		assertThrows(IllegalArgumentException.class, () -> b.authority(null, 8790));
		assertThrows(IllegalArgumentException.class, () -> b.authority("127.0.0.1", 0));
	}

	@Test void i05_csrfHeaderRejectsNullAndBlank() {
		var b = LoopbackBoundary.create();
		assertThrows(IllegalArgumentException.class, () -> b.csrfHeader(null));
		assertThrows(IllegalArgumentException.class, () -> b.csrfHeader(" "));
	}

	@Test void i06_buildRequiresAnAuthorityAndAToken() {
		var withTokenOnly = LoopbackBoundary.create().token(TOKEN);
		assertThrows(IllegalArgumentException.class, withTokenOnly::build);
		var withAuthorityOnly = LoopbackBoundary.create().authority(AUTHORITY);
		assertThrows(IllegalArgumentException.class, withAuthorityOnly::build);
		var b = LoopbackBoundary.create();
		assertThrows(IllegalArgumentException.class, () -> b.token(null));
	}

	@Test void i07_checkRejectsANullRequest() {
		assertThrows(IllegalArgumentException.class, () -> BOUNDARY.check(null));
	}

	@Test void i08_resultToString() {
		assertEquals("ALLOWED", BoundaryResult.ALLOWED.toString());
		assertTrue(BOUNDARY.check(write("Host", "evil.example")).toString().startsWith("HOST_MISMATCH(421): "));
	}

	@Test void i09_allowedResultCarriesNoRejectionDetail() {
		var r = BOUNDARY.check(write(null, null));
		assertTrue(r.isAllowed());
		assertNull(r.reason());
		assertEquals(0, r.status());
		assertNull(r.message());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j) HTTP/2: no Host header, so :authority (the container's server name and port) stands in
	//-----------------------------------------------------------------------------------------------------------------

	/** The legitimate write with no {@code Host}, as the container reports an HTTP/2 request to the given authority. */
	private static HttpServletRequest h2(String protocol, String name, int port) {
		var h = goodWriteHeaders();
		h.remove("Host");
		var r = req("POST", h);
		when(r.getProtocol()).thenReturn(protocol);
		when(r.getServerName()).thenReturn(name);
		when(r.getServerPort()).thenReturn(port);
		return r;
	}

	@Test void j01_http2WithoutHostMatchesTheAuthority() {
		assertTrue(BOUNDARY.check(h2("HTTP/2.0", "127.0.0.1", 8790)).isAllowed());
	}

	@Test void j02_http2WithAnotherNameOrPortIs421() {
		assertRejected(BOUNDARY.check(h2("HTTP/2.0", "localhost", 8790)), HOST_MISMATCH, 421);
		assertRejected(BOUNDARY.check(h2("HTTP/2.0", "127.0.0.1", 8791)), HOST_MISMATCH, 421);
		assertRejected(BOUNDARY.check(h2("HTTP/2.0", "evil.example", 80)), HOST_MISMATCH, 421);
		assertRejected(BOUNDARY.check(h2("HTTP/2.0", null, 8790)), HOST_MISMATCH, 421);
	}

	@Test void j03_http11WithoutHostIsStill421() {
		// The mutation row: the server name and port equal the authority, so only the protocol guard refuses it.
		assertRejected(BOUNDARY.check(h2("HTTP/1.1", "127.0.0.1", 8790)), HOST_MISMATCH, 421);
		assertRejected(BOUNDARY.check(h2("HTTP/1.0", "127.0.0.1", 8790)), HOST_MISMATCH, 421);
		assertRejected(BOUNDARY.check(h2(null, "127.0.0.1", 8790)), HOST_MISMATCH, 421);
		assertRejected(BOUNDARY.check(h2("HTTP/2", "127.0.0.1", 8790)), HOST_MISMATCH, 421);
	}

	@Test void j04_http2WithARealHostUsesIt() {
		var h = goodWriteHeaders();
		h.put("Host", "evil.example");
		var r = req("POST", h);
		when(r.getProtocol()).thenReturn("HTTP/2.0");
		when(r.getServerName()).thenReturn("127.0.0.1");
		when(r.getServerPort()).thenReturn(8790);
		assertRejected(BOUNDARY.check(r), HOST_MISMATCH, 421);
	}

	@Test void j05_defaultPortIsFilledForTheSynthesisedValue() {
		var noPort = LoopbackBoundary.create().authority("127.0.0.1").token(TOKEN).build();
		var port80 = LoopbackBoundary.create().authority("127.0.0.1", 80).token(TOKEN).build();
		for (var b : List.of(noPort, port80)) {
			var h = new LinkedHashMap<String,String>(b.selfCallHeaders());
			var r = req("POST", h);
			when(r.getProtocol()).thenReturn("HTTP/2.0");
			when(r.getServerName()).thenReturn("127.0.0.1");
			when(r.getServerPort()).thenReturn(80);
			assertTrue(b.check(r).isAllowed(), b.authority());
			var other = req("POST", h);
			when(other.getProtocol()).thenReturn("HTTP/2.0");
			when(other.getServerName()).thenReturn("127.0.0.1");
			when(other.getServerPort()).thenReturn(81);
			assertRejected(b.check(other), HOST_MISMATCH, 421);
		}
	}

	@Test void j06_aRealHostKeepsTheStringCompare() {
		// Deliberate asymmetry: a real "127.0.0.1:80" is not widened to match authority "127.0.0.1".
		var noPort = LoopbackBoundary.create().authority("127.0.0.1").token(TOKEN).build();
		var h = new LinkedHashMap<String,String>(noPort.selfCallHeaders());
		h.put("Host", "127.0.0.1:80");
		assertRejected(noPort.check(req("POST", h)), HOST_MISMATCH, 421);
	}

	@Test void j07_aUnparseableAuthorityFailsClosedOnHttp2() {
		var odd = LoopbackBoundary.create().authority("not a host").token(TOKEN).build();
		var r = req("POST", new LinkedHashMap<String,String>(odd.selfCallHeaders()));
		when(r.getProtocol()).thenReturn("HTTP/2.0");
		when(r.getServerName()).thenReturn("not a host");
		when(r.getServerPort()).thenReturn(80);
		assertRejected(odd.check(r), HOST_MISMATCH, 421);
	}

	@Test void j08_ipv6ServerNameIsBracketed() {
		var v6 = LoopbackBoundary.create().authority("[::1]:8790").token(TOKEN).build();
		var r = req("POST", new LinkedHashMap<String,String>(v6.selfCallHeaders()));
		when(r.getProtocol()).thenReturn("HTTP/2.0");
		when(r.getServerName()).thenReturn("::1");
		when(r.getServerPort()).thenReturn(8790);
		assertTrue(v6.check(r).isAllowed());
	}

	@Test void j09_handshakeHost() {
		var uri = "ws";
		// A real Host: compared as written, first value, ignoring case.
		assertTrue(BOUNDARY.acceptsHandshakeHost(new HandshakeHosts(List.of(AUTHORITY.toUpperCase()), false), uri));
		assertFalse(BOUNDARY.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1"), false), uri));
		assertFalse(BOUNDARY.acceptsHandshakeHost(new HandshakeHosts(List.of(), false), uri));
		var noPort = LoopbackBoundary.create().authority("127.0.0.1").token(TOKEN).build();
		assertFalse(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1:80"), false), uri));
		// A synthesised value: parsed host and port, defaults filled from the handshake scheme and from http.
		assertTrue(BOUNDARY.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1:8790"), true), "http"));
		assertFalse(BOUNDARY.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1:8791"), true), "http"));
		assertTrue(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1"), true), "ws"));
		assertTrue(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1:80"), true), "ws"));
		assertFalse(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1"), true), "wss"));
		assertFalse(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1"), true), null));
		assertFalse(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1"), true), "ftp"));
		assertTrue(noPort.acceptsHandshakeHost(new HandshakeHosts(List.of("127.0.0.1:80"), true), null));
		assertThrows(IllegalArgumentException.class, () -> BOUNDARY.acceptsHandshakeHost(null, uri));
	}
}
