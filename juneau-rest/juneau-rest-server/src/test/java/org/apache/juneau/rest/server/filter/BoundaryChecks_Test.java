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
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link BoundaryChecks}, the request predicates shared by the {@link RequestBoundary} implementations.
 *
 * @since 10.0.0
 */
class BoundaryChecks_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a) isJson: the base type is exactly application/json
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_json() {
		assertTrue(BoundaryChecks.isJson("application/json"));
	}

	@Test void a02_jsonWithParameters() {
		assertTrue(BoundaryChecks.isJson("application/json;charset=utf-8"));
		assertTrue(BoundaryChecks.isJson(" APPLICATION/JSON ; charset=UTF-8"));
	}

	@Test void a03_formEncodableTypesAreNotJson() {
		assertFalse(BoundaryChecks.isJson("application/x-www-form-urlencoded"));
		assertFalse(BoundaryChecks.isJson("multipart/form-data; boundary=x"));
		assertFalse(BoundaryChecks.isJson("text/plain"));
	}

	@Test void a04_suffixedJsonIsNotJson() {
		assertFalse(BoundaryChecks.isJson("application/problem+json"));
	}

	@Test void a05_absentIsNotJson() {
		assertFalse(BoundaryChecks.isJson(null));
		assertFalse(BoundaryChecks.isJson(""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) isSafelistedType: the content types a cross-origin request may send without a preflight
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_isSafelistedType() {
		assertTrue(BoundaryChecks.isSafelistedType("application/x-www-form-urlencoded"));
		assertTrue(BoundaryChecks.isSafelistedType("multipart/form-data"));
		assertTrue(BoundaryChecks.isSafelistedType("text/plain"));
		assertTrue(BoundaryChecks.isSafelistedType("Text/Plain"));
		assertTrue(BoundaryChecks.isSafelistedType("MULTIPART/FORM-DATA; boundary=----x"));
		assertTrue(BoundaryChecks.isSafelistedType(" application/x-www-form-urlencoded ; charset=utf-8"));
		assertFalse(BoundaryChecks.isSafelistedType("application/json"));
		assertFalse(BoundaryChecks.isSafelistedType("text/plain+xml"));
		assertFalse(BoundaryChecks.isSafelistedType("text/html"));
		assertFalse(BoundaryChecks.isSafelistedType(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// p01) parseOrigin: exactly scheme "://" host [":" port], normalized, nothing else
	//-----------------------------------------------------------------------------------------------------------------

	/** Rows of name, input, expected normalized origin as {@code scheme|host|port} (or {@code null} when refused). */
	static Stream<Arguments> p01_rows() {
		return Stream.of(
			Arguments.of("http, default port", "http://h", "http|h|80"),
			Arguments.of("https, default port", "https://h", "https|h|443"),
			Arguments.of("explicit default port", "http://h:80", "http|h|80"),
			Arguments.of("explicit port", "http://app.example:8080", "http|app.example|8080"),
			Arguments.of("upper-case scheme and host", "HTTP://App.EXAMPLE:8080", "http|app.example|8080"),
			Arguments.of("bracketed ipv6", "http://[::1]:8080", "http|[::1]|8080"),
			Arguments.of("ipv4-mapped ipv6 kept as text", "http://[::FFFF:127.0.0.1]", "http|[::ffff:127.0.0.1]|80"),
			Arguments.of("ipv4 short form kept as text", "http://127.1", "http|127.1|80"),
			Arguments.of("trailing dot kept", "http://a.example.", "http|a.example.|80"),
			Arguments.of("punycode kept as sent", "http://xn--pp-uia.example", "http|xn--pp-uia.example|80"),
			Arguments.of("trailing slash", "http://h/", null),
			Arguments.of("path", "http://h/x", null),
			Arguments.of("userinfo", "http://u@h", null),
			Arguments.of("query", "http://h?x", null),
			Arguments.of("fragment", "http://h#x", null),
			Arguments.of("leading space", " http://h", null),
			Arguments.of("trailing space", "http://h ", null),
			Arguments.of("internal space", "http:// h", null),
			Arguments.of("blank", "", null),
			Arguments.of("whitespace only", "   ", null),
			Arguments.of("null literal", "null", null),
			Arguments.of("file scheme", "file:///etc/passwd", null),
			Arguments.of("extension scheme", "chrome-extension://abcdef", null),
			Arguments.of("ftp scheme", "ftp://h", null),
			Arguments.of("one slash", "http:/h", null),
			Arguments.of("no colon", "http//h", null),
			Arguments.of("no scheme", "://h", null),
			Arguments.of("no host", "http://", null),
			Arguments.of("non-ascii host", "http://\u00e4pp.example", null),
			Arguments.of("unbracketed ipv6", "http://::1:8080", null),
			Arguments.of("zone id", "http://[fe80::1%25en0]", null),
			Arguments.of("zero-padded port", "http://h:080", null),
			Arguments.of("port zero", "http://h:0", null),
			Arguments.of("port too large", "http://h:65536", null),
			Arguments.of("signed port", "http://h:+80", null),
			Arguments.of("negative port", "http://h:-1", null),
			Arguments.of("empty port", "http://h:", null)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("p01_rows")
	void p01_parseOrigin(String name, String input, String expected) {
		var o = BoundaryChecks.parseOrigin(input);
		assertEquals(expected, o == null ? null : o.scheme() + "|" + o.host() + "|" + o.port(), name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// p02) parseHost: a Host header value, a reg-name / IPv4 / bracketed IPv6 with an optional port
	//-----------------------------------------------------------------------------------------------------------------

	/** Rows of name, input, expected {@code host|port} (port -1 when absent), or {@code null} when malformed. */
	static Stream<Arguments> p02_rows() {
		return Stream.of(
			Arguments.of("name", "h", "h|-1"),
			Arguments.of("name and port", "app.example:8080", "app.example|8080"),
			Arguments.of("upper-case", "APP.Example:8080", "app.example|8080"),
			Arguments.of("ipv4", "127.0.0.1:8790", "127.0.0.1|8790"),
			Arguments.of("ipv6", "[::1]:8080", "[::1]|8080"),
			Arguments.of("ipv6 no port", "[::1]", "[::1]|-1"),
			Arguments.of("ipv6 upper-case", "[FE80::1]", "[fe80::1]|-1"),
			Arguments.of("ipv4-mapped ipv6", "[::ffff:127.0.0.1]", "[::ffff:127.0.0.1]|-1"),
			Arguments.of("trailing dot", "a.example.", "a.example.|-1"),
			Arguments.of("max port", "h:65535", "h|65535"),
			Arguments.of("blank", "", null),
			Arguments.of("leading space", " h", null),
			Arguments.of("trailing space", "h ", null),
			Arguments.of("tab", "h\t", null),
			Arguments.of("comma list", "a, b", null),
			Arguments.of("comma list no space", "a,b", null),
			Arguments.of("userinfo", "evil@h", null),
			Arguments.of("zone id", "[fe80::1%25en0]:8080", null),
			Arguments.of("raw zone id", "[fe80::1%en0]", null),
			Arguments.of("non-ascii", "\u00e4pp.example", null),
			Arguments.of("slash", "h/x", null),
			Arguments.of("backslash", "h\\x", null),
			Arguments.of("control character", "h\u0007", null),
			Arguments.of("nul", "h\u0000", null),
			Arguments.of("unbracketed ipv6", "::1", null),
			Arguments.of("unbracketed ipv6 with port", "::1:8080", null),
			Arguments.of("unclosed bracket", "[::1", null),
			Arguments.of("junk after bracket", "[::1]x", null),
			Arguments.of("empty brackets", "[]", null),
			Arguments.of("ipv6 without colon", "[abc]", null),
			Arguments.of("two ports", "h:8080:9090", null),
			Arguments.of("empty port", "h:", null),
			Arguments.of("zero-padded port", "h:080", null),
			Arguments.of("port zero", "h:0", null),
			Arguments.of("port too large", "h:65536", null),
			Arguments.of("very large port", "h:99999999999", null),
			Arguments.of("signed port", "h:+80", null),
			Arguments.of("negative port", "h:-1", null),
			Arguments.of("port only", ":80", null)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("p02_rows")
	void p02_parseHost(String name, String input, String expected) {
		var a = BoundaryChecks.parseHost(input);
		assertEquals(expected, a == null ? null : a.host() + "|" + a.port(), name);
	}

	@Test void p03_parseNull() {
		assertNull(BoundaryChecks.parseHost(null));
		assertNull(BoundaryChecks.parseOrigin(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// p04) headerValues: every value of a header, so a duplicate can be counted
	//-----------------------------------------------------------------------------------------------------------------

	@Test void p04_headerValues() {
		var r = mock(HttpServletRequest.class);
		when(r.getHeaders("Origin")).thenAnswer(i -> Collections.enumeration(List.of("a", "b")));
		when(r.getHeader("Origin")).thenReturn("a");
		assertEquals(List.of("a", "b"), BoundaryChecks.headerValues(r, "Origin"));
		// A container that will not enumerate a header returns null; the single value is then all there is.
		when(r.getHeaders("Host")).thenReturn(null);
		when(r.getHeader("Host")).thenReturn("h");
		assertEquals(List.of("h"), BoundaryChecks.headerValues(r, "Host"));
		assertEquals(List.of(), BoundaryChecks.headerValues(r, "Absent"));
	}

	@Test void p05_isSameOriginFetch() {
		assertTrue(BoundaryChecks.isSameOriginFetch("same-origin"));
		assertFalse(BoundaryChecks.isSameOriginFetch("same-site"));
		assertFalse(BoundaryChecks.isSameOriginFetch("Same-Origin"));
		assertFalse(BoundaryChecks.isSameOriginFetch(" same-origin"));
		assertFalse(BoundaryChecks.isSameOriginFetch(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// p06) hostValues / isSynthesisedHost: HTTP/2 :authority stands in for a missing Host
	//-----------------------------------------------------------------------------------------------------------------

	private static HttpServletRequest h(String protocol, String name, int port, String...hosts) {
		var r = mock(HttpServletRequest.class);
		when(r.getProtocol()).thenReturn(protocol);
		when(r.getServerName()).thenReturn(name);
		when(r.getServerPort()).thenReturn(port);
		when(r.getHeaders("Host")).thenAnswer(i -> Collections.enumeration(List.of(hosts)));
		when(r.getHeader("Host")).thenReturn(hosts.length == 0 ? null : hosts[0]);
		return r;
	}

	@Test void p06_http2WithoutHostSynthesises() {
		var r = h("HTTP/2.0", "app.example", 8080);
		assertEquals(List.of("app.example:8080"), BoundaryChecks.hostValues(r));
		assertTrue(BoundaryChecks.isSynthesisedHost(r));
	}

	@Test void p07_otherProtocolsDoNotSynthesise() {
		for (var protocol : new String[]{"HTTP/1.1", "HTTP/1.0", "HTTP/2", "http/2.0", "HTTP/3.0", "", null}) {
			var r = h(protocol, "app.example", 8080);
			assertEquals(List.of(), BoundaryChecks.hostValues(r), String.valueOf(protocol));
			assertFalse(BoundaryChecks.isSynthesisedHost(r), String.valueOf(protocol));
		}
	}

	@Test void p08_realHostWinsAndStaysRepeated() {
		var r = h("HTTP/2.0", "app.example", 8080, "other.example");
		assertEquals(List.of("other.example"), BoundaryChecks.hostValues(r));
		assertFalse(BoundaryChecks.isSynthesisedHost(r));
		var two = h("HTTP/2.0", "app.example", 8080, "a", "b");
		assertEquals(List.of("a", "b"), BoundaryChecks.hostValues(two));
		assertFalse(BoundaryChecks.isSynthesisedHost(two));
	}

	@Test void p09_portsAndNames() {
		assertEquals(List.of("app.example"), BoundaryChecks.hostValues(h("HTTP/2.0", "app.example", 0)));
		assertEquals(List.of("app.example"), BoundaryChecks.hostValues(h("HTTP/2.0", "app.example", -1)));
		assertEquals(List.of("[::1]:80"), BoundaryChecks.hostValues(h("HTTP/2.0", "::1", 80)));
		assertEquals(List.of("[::1]:80"), BoundaryChecks.hostValues(h("HTTP/2.0", "[::1]", 80)));
		assertEquals(List.of(), BoundaryChecks.hostValues(h("HTTP/2.0", null, 80)));
		assertEquals(List.of(), BoundaryChecks.hostValues(h("HTTP/2.0", "", 80)));
		assertEquals(List.of(), BoundaryChecks.hostValues(h("HTTP/2.0", "  ", 80)));
		assertFalse(BoundaryChecks.isSynthesisedHost(h("HTTP/2.0", null, 80)));
	}
}
