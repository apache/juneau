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
package org.apache.juneau.commons.http;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link SafePathTemplate} &mdash; the shared same-origin / non-templated path predicates, the
 * poll-interval floor, and the calendar path-template / document-URL helpers layered on the same core.
 */
class SafePathTemplate_Test extends TestBase {

	@Test void a01_minPollInterval_is5000() {
		assertEquals(5_000L, SafePathTemplate.MIN_POLL_INTERVAL_MS);
	}

	@Test void a02_sameOrigin_accepts() {
		assertTrue(SafePathTemplate.isSameOriginPath("/data/alerts"));
		assertTrue(SafePathTemplate.isSameOriginPath("data/alerts"));
		assertTrue(SafePathTemplate.isSameOriginPath("/data/alerts/{id}"));   // templated is still same-origin
		assertTrue(SafePathTemplate.isSameOriginPath("/events/{year}/{month}"));
		assertTrue(SafePathTemplate.isSameOriginPath("/events/123?x=1#frag"));
	}

	@Test void a03_sameOrigin_rejects() {
		assertFalse(SafePathTemplate.isSameOriginPath(null));
		assertFalse(SafePathTemplate.isSameOriginPath(""));
		assertFalse(SafePathTemplate.isSameOriginPath("   "));
		assertFalse(SafePathTemplate.isSameOriginPath("https://other/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("//other/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("servlet:/data/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("javascript:alert(1)"));
		assertFalse(SafePathTemplate.isSameOriginPath("/data/../x"));
		assertFalse(SafePathTemplate.isSameOriginPath("a:b"));
		assertFalse(SafePathTemplate.isSameOriginPath(".."));
	}

	@Test void a04_nonTemplated_acceptsFixedPathOnly() {
		assertTrue(SafePathTemplate.isNonTemplatedPath("/data/alerts"));
		assertTrue(SafePathTemplate.isNonTemplatedPath("data/alerts"));
	}

	@Test void a05_nonTemplated_rejectsTemplateAndUnsafe() {
		assertFalse(SafePathTemplate.isNonTemplatedPath("/data/alerts/{id}"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("/cards/{cardId}"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("/{anything}"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("https://other/x"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("//other/x"));
		assertFalse(SafePathTemplate.isNonTemplatedPath("/data/../x"));
		assertFalse(SafePathTemplate.isNonTemplatedPath(null));
	}

	@Test void a06_clampPollInterval_floors() {
		assertEquals(5_000L, SafePathTemplate.clampPollInterval(0L));
		assertEquals(5_000L, SafePathTemplate.clampPollInterval(1_000L));
		assertEquals(5_000L, SafePathTemplate.clampPollInterval(5_000L));
		assertEquals(30_000L, SafePathTemplate.clampPollInterval(30_000L));
	}

	@Test void a07_safeTemplate_requiresTokens() {
		assertTrue(SafePathTemplate.isSafeTemplate("/events/{year}/{month}", "{year}", "{month}"));
		assertTrue(SafePathTemplate.isSafeTemplate("/e?y={year}&m={month}", "{year}", "{month}"));
		assertTrue(SafePathTemplate.isSafeTemplate("/events/all"));
		assertFalse(SafePathTemplate.isSafeTemplate("/events/{year}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("/events/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("/events/all", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("http://other/{year}/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("//other/{year}/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate("../{year}/{month}", "{year}", "{month}"));
		assertFalse(SafePathTemplate.isSafeTemplate(null, "{year}"));
	}

	@Test void a08_safeDocumentUrl() {
		assertTrue(SafePathTemplate.isSafeDocumentUrl("/events/123"));
		assertTrue(SafePathTemplate.isSafeDocumentUrl("/events/123?x=1"));
		assertTrue(SafePathTemplate.isSafeDocumentUrl("/events/123#frag"));
		assertTrue(SafePathTemplate.isSafeDocumentUrl("events/123"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("https://other/1"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("//other/1"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("javascript:alert(1)"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl("/x/../../etc/passwd"));
		assertFalse(SafePathTemplate.isSafeDocumentUrl(null));
	}

	@Test void a09_sameOrigin_rejectsBackslashAndEdgeCharacters() {
		// A browser reads "\\" as "/", and strips a leading or trailing C0 control or space, so each of these resolves cross-host.
		assertFalse(SafePathTemplate.isSameOriginPath("\\\\other.test/x"));
		assertFalse(SafePathTemplate.isSameOriginPath("/\\other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("\\/other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath(" //other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("\t//other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("\n//other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("\r//other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("\u0000//other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("\u001f//other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("/\t/other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("/\n/other.test"));
		assertFalse(SafePathTemplate.isSameOriginPath("//other.test "));
		assertFalse(SafePathTemplate.isSameOriginPath("/foo "));
		assertFalse(SafePathTemplate.isSameOriginPath("/foo?q=C:\\x"));
		assertFalse(SafePathTemplate.isSameOriginPath("/a\u007fb"));
	}

	@Test void a10_sameOrigin_keepsInternalSpaceEncodedAndNonAscii() {
		assertTrue(SafePathTemplate.isSameOriginPath("/a b"));
		assertTrue(SafePathTemplate.isSameOriginPath("/%5Cevil.test"));
		assertTrue(SafePathTemplate.isSameOriginPath("%2F%2Fevil.test"));
		assertTrue(SafePathTemplate.isSameOriginPath("/.//other.test"));
		assertTrue(SafePathTemplate.isSameOriginPath("\u00a0//other.test"));
		assertTrue(SafePathTemplate.isSameOriginPath("/caf\u00e9"));
	}

	//====================================================================================================
	// isSafeRedirect(String, String...)
	//====================================================================================================

	@Test void b01_redirect_relativePath_isSafe() {
		assertTrue(SafePathTemplate.isSafeRedirect("/foo/bar"));
		assertTrue(SafePathTemplate.isSafeRedirect("foo/bar"));
	}

	@Test void b02_redirect_nullOrEmpty_isSafe() {
		assertTrue(SafePathTemplate.isSafeRedirect(null));
		assertTrue(SafePathTemplate.isSafeRedirect(""));
	}

	@Test void b03_redirect_absoluteUri_allowedHost_isSafe() {
		assertTrue(SafePathTemplate.isSafeRedirect("https://example.com/foo", "example.com"));
	}

	@Test void b04_redirect_absoluteUri_otherHost_isUnsafe() {
		assertFalse(SafePathTemplate.isSafeRedirect("https://other.example/foo", "example.com"));
	}

	@Test void b05_redirect_absoluteUri_noAllowedHosts_isUnsafe() {
		assertFalse(SafePathTemplate.isSafeRedirect("https://example.com/foo"));
		assertFalse(SafePathTemplate.isSafeRedirect("https://example.com/foo", (String[])null));
	}

	@Test void b06_redirect_protocolRelativeUri_otherHost_isUnsafe() {
		assertFalse(SafePathTemplate.isSafeRedirect("//other.example/foo", "example.com"));
	}

	@Test void b07_redirect_protocolRelativeUri_allowedHost_isSafe() {
		assertTrue(SafePathTemplate.isSafeRedirect("//example.com/foo", "example.com"));
	}

	@Test void b08_redirect_allowedHostMatch_isCaseInsensitive() {
		assertTrue(SafePathTemplate.isSafeRedirect("https://Example.COM/foo", "example.com"));
	}

	@Test void b09_redirect_malformedUri_isUnsafe() {
		assertFalse(SafePathTemplate.isSafeRedirect("https://exa mple.com/foo", "example.com"));
		assertFalse(SafePathTemplate.isSafeRedirect("https://[bad/foo", "example.com"));
	}

	@Test void b10_redirect_backslashAndControlChars_areUnsafe() {
		assertFalse(SafePathTemplate.isSafeRedirect("/\\example.com/foo", "example.com"));
		assertFalse(SafePathTemplate.isSafeRedirect("/foo\\bar"));
		assertFalse(SafePathTemplate.isSafeRedirect("/foo\tbar"));
		assertFalse(SafePathTemplate.isSafeRedirect("/foo\nbar"));
		assertFalse(SafePathTemplate.isSafeRedirect("/foo bar"));
		assertFalse(SafePathTemplate.isSafeRedirect("/foo\u007fbar"));
	}

	@Test void b11_redirect_schemeWithoutAuthority_isUnsafe() {
		assertFalse(SafePathTemplate.isSafeRedirect("javascript:alert(1)", "example.com"));
		assertFalse(SafePathTemplate.isSafeRedirect("data:text/html,x"));
		assertFalse(SafePathTemplate.isSafeRedirect("http:other.example"));
		assertTrue(SafePathTemplate.isSafeRedirect("foo/bar:baz"));
		assertTrue(SafePathTemplate.isSafeRedirect("?a=b:c"));
	}
}
