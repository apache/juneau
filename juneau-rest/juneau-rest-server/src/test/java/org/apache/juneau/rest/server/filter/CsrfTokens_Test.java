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

import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link CsrfTokens}.
 *
 * <p>
 * The negative cases carry the weight: a token check that only has positive tests passes just as happily when it
 * accepts everything.
 *
 * @since 10.0.0
 */
class CsrfTokens_Test extends TestBase {

	private static final HexFormat HEX = HexFormat.of();

	private static final Path SOURCE = Path.of("src/main/java/org/apache/juneau/rest/server/filter/CsrfTokens.java");

	/** A request that has a session with the given id; {@code null} means a request with no session. */
	private static HttpServletRequest req(String sessionId) {
		var r = mock(HttpServletRequest.class);
		if (sessionId != null) {
			var s = mock(HttpSession.class);
			when(s.getId()).thenReturn(sessionId);
			when(r.getSession()).thenReturn(s);
			when(r.getSession(anyBoolean())).thenReturn(s);
		}
		return r;
	}

	private static byte[] key(int length, int seed) {
		var k = new byte[length];
		for (var i = 0; i < length; i++)
			k[i] = (byte)(seed + i);
		return k;
	}

	private static byte[] unmaskByHand(String masked) {
		var pad = HEX.parseHex(masked.substring(0, 64));
		var body = HEX.parseHex(masked.substring(64));
		var out = new byte[32];
		for (var i = 0; i < 32; i++)
			out[i] = (byte)(pad[i] ^ body[i]);
		return out;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Masking, binding and verification
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_maskedFormat() {
		var t = CsrfTokens.perSession();
		var masked = t.mask(t.expected(req("s1"), false));
		assertEquals(128, masked.length(), masked);
		assertTrue(masked.matches("[0-9a-f]{128}"), masked);
	}

	@Test void a02_maskIndependentPerCall() {
		var t = CsrfTokens.perSession();
		var expected = t.expected(req("s1"), false);
		var m1 = t.mask(expected);
		var m2 = t.mask(expected);
		assertNotEquals(m1, m2, "two embeds of one token must not repeat on the wire");
		assertNotEquals(m1.substring(0, 64), m2.substring(0, 64), "the pad must be fresh per call");
		assertTrue(t.verify(m1, expected));
		assertTrue(t.verify(m2, expected));
	}

	@Test void a03_maskedHalvesXorToToken() {
		var t = CsrfTokens.perSession();
		var expected = t.expected(req("s1"), false);
		assertArrayEquals(expected, unmaskByHand(t.mask(expected)));
	}

	@Test void a04_perSessionBoundToSessionId() {
		var t = CsrfTokens.perSession();
		var forS1 = t.mask(t.expected(req("s1"), false));
		assertTrue(t.verify(forS1, t.expected(req("s1"), false)));
		assertFalse(t.verify(forS1, t.expected(req("s2"), false)));
	}

	@Test void a05_perSessionDeterministicForSameId() {
		var t = CsrfTokens.perSession();
		var e1 = t.expected(req("s1"), false);
		var e2 = t.expected(req("s1"), false);
		assertEquals(32, e1.length);
		assertArrayEquals(e1, e2);
		assertNotSame(e1, e2, "callers get their own copy");
	}

	@Test void a06_noSessionUsesDummyHmac() {
		var t = CsrfTokens.perSession();
		var dummy = t.expected(req(null), false);
		assertNotNull(dummy);
		assertEquals(32, dummy.length);
		assertArrayEquals(dummy, t.expected(req(null), false), "the placeholder is a fixed id");
		for (var id : List.of("", "no-session", "null", "0", "s1", "node0abc123.node0"))
			assertFalse(Arrays.equals(dummy, t.expected(req(id), false)), () -> "dummy HMAC equals the HMAC of '" + id + "'");
		for (var id : List.of("", "no-session", "s1"))
			assertFalse(t.verify(t.mask(t.expected(req(id), false)), dummy), () -> "a token for '" + id + "' verified against no session");
	}

	@Test void a06b_placeholderCannotCollideWithRealSessionId() throws Exception {
		var f = CsrfTokens.class.getDeclaredField("NO_SESSION_ID");
		f.setAccessible(true);
		var placeholder = (String)f.get(null);
		assertTrue(placeholder.indexOf('\u0000') >= 0, "the placeholder must contain a NUL");
		// A session id reaches the server as a cookie value or URL parameter; RFC 6265 cookie-octets exclude every
		// control character, so a NUL can never be part of an id a container hands out.
		assertTrue(placeholder.chars().anyMatch(c -> c < 0x21 || c > 0x7e), placeholder);
	}

	@Test void a07_noSessionAndWrongTokenSameOutcome() {
		var t = CsrfTokens.perSession();
		var presented = t.mask(t.expected(req("attacker"), false));
		var noSession = t.expected(req(null), false);
		var victim = t.expected(req("victim"), false);
		assertFalse(t.verify(presented, noSession));
		assertFalse(t.verify(presented, victim));
		assertEquals(victim.length, noSession.length);
	}

	@Test void a08_formatChecksRejectBadShape() {
		var t = CsrfTokens.perSession();
		var expected = t.expected(req("s1"), false);
		var good = t.mask(expected);
		assertTrue(t.verify(good, expected));
		var bad = new ArrayList<String>();
		bad.add(null);
		bad.add("");
		bad.add(HEX.formatHex(expected));
		bad.add(good.substring(1));
		bad.add(good + "0");
		bad.add(good.toUpperCase());
		bad.add("g" + good.substring(1));
		bad.add(good.substring(0, 127) + "g");
		bad.add(" " + good.substring(1));
		bad.add(good.substring(0, 127) + " ");
		bad.add(" " + good);
		bad.add(good + " ");
		for (var b : bad)
			assertFalse(assertDoesNotThrow(() -> t.verify(b, expected)), () -> "accepted: '" + b + "'");
	}

	@Test void a09_keyLengthEnforced() {
		var t = CsrfTokens.perSession();
		assertThrows(IllegalArgumentException.class, () -> t.key(new byte[31]));
		assertThrows(IllegalArgumentException.class, () -> t.key(new byte[0]));
		assertThrows(IllegalArgumentException.class, () -> t.key(null));
		assertNotNull(t.key(key(32, 1)));
		assertNotNull(t.key(key(64, 1)));
	}

	@Test void a10_keyIsCopied() {
		var k = key(32, 7);
		var t = CsrfTokens.perSession().key(k);
		var before = t.expected(req("s1"), false);
		Arrays.fill(k, (byte)0);
		assertArrayEquals(before, t.expected(req("s1"), false));
	}

	@Test void a11_twoInstancesDifferentKeys() {
		var t1 = CsrfTokens.perSession();
		var t2 = CsrfTokens.perSession();
		assertFalse(t2.verify(t1.mask(t1.expected(req("s1"), false)), t2.expected(req("s1"), false)));

		var c1 = CsrfTokens.perSession().key(key(32, 3));
		var c2 = CsrfTokens.perSession().key(key(32, 3));
		assertTrue(c2.verify(c1.mask(c1.expected(req("s1"), false)), c2.expected(req("s1"), false)),
			"nodes sharing one key must accept each other's tokens");
	}

	@Test void a12_perProcessTwoInstancesDoNotCrossVerify() {
		var p1 = CsrfTokens.perProcess();
		var p2 = CsrfTokens.perProcess();
		var r = req("s1");
		assertTrue(p1.verify(p1.mask(p1.expected(r, false)), p1.expected(r, false)));
		assertFalse(p2.verify(p1.mask(p1.expected(r, false)), p2.expected(r, false)));
	}

	@Test void a13_toStringRedacts() throws Exception {
		var k = key(32, 11);
		var session = CsrfTokens.perSession().key(k);
		var secret = SynchronizerToken.generate();
		var process = CsrfTokens.perProcess(secret);
		var r = req("s1");
		var leaks = List.of(
			HEX.formatHex(k), HEX.withUpperCase().formatHex(k), Base64.getEncoder().encodeToString(k),
			Base64.getUrlEncoder().withoutPadding().encodeToString(k), Arrays.toString(k),
			HEX.formatHex(session.expected(r, false)), secret.value(), HEX.formatHex(process.expected(r, false)));
		for (var t : List.of(session, process, CsrfTokens.perSession(), CsrfTokens.perProcess())) {
			var s = t.toString();
			assertTrue(s.contains("redacted"), s);
			assertFalse(s.contains("javax.crypto"), s);
			for (var leak : leaks)
				assertFalse(s.contains(leak), () -> "toString() leaks a secret: " + s);
			for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass())
				for (var f : c.getDeclaredFields())
					assertFalse(f.getType().getName().startsWith("javax.crypto"), () -> "field holds a crypto object: " + f);
		}
	}

	@Test void a14_compareIsConstantTimeShape() throws Exception {
		var src = Files.readString(SOURCE, StandardCharsets.UTF_8);
		var verify = body(src, "boolean verify(");
		var unmask = body(src, "byte[] unmask(");
		assertEquals(1, count(verify, "MessageDigest.isEqual("), () -> "verify must compare with MessageDigest.isEqual exactly once:\n" + verify);
		for (var b : List.of(verify, unmask)) {
			for (var banned : List.of("Arrays.equals", "Arrays.mismatch", ".equals(", ".compareTo(", "==", "!="))
				assertFalse(b.contains(banned), () -> "'" + banned + "' on secret bytes in:\n" + b);
		}
	}

	@Test void a15_sessionIdChangeInvalidatesToken() {
		var t = CsrfTokens.perSession();
		var s = mock(HttpSession.class);
		when(s.getId()).thenReturn("before-login", "after-login");
		var r = mock(HttpServletRequest.class);
		when(r.getSession(anyBoolean())).thenReturn(s);
		var minted = t.mask(t.expected(r, false));
		assertFalse(t.verify(minted, t.expected(r, false)), "a token minted before changeSessionId() must not survive it");
	}

	@Test void a16_perProcessRejectsNonSixtyFourHex() {
		var hex64 = SynchronizerToken.generate().value();
		for (var v : List.of("the-real-token", hex64.substring(1), hex64 + "a", hex64.toUpperCase())) {
			var bad = SynchronizerToken.of(v);
			assertThrows(IllegalArgumentException.class, () -> CsrfTokens.perProcess(bad), v);
		}
		assertThrows(IllegalArgumentException.class, () -> CsrfTokens.perProcess(null));

		var secret = SynchronizerToken.generate();
		var p = CsrfTokens.perProcess(secret);
		var r = req(null);
		var expected = p.expected(r, false);
		assertArrayEquals(HEX.parseHex(secret.value()), expected);
		var masked = p.mask(expected);
		assertTrue(masked.matches("[0-9a-f]{128}"), masked);
		assertArrayEquals(HEX.parseHex(secret.value()), unmaskByHand(masked));
		assertTrue(p.verify(masked, p.expected(r, false)));
	}

	@Test void a17_sessionCreationIsTheCallersChoice() {
		var r = req("s1");
		CsrfTokens.perSession().expected(r, true);
		verify(r).getSession(true);
		var r2 = req("s1");
		CsrfTokens.perSession().expected(r2, false);
		verify(r2).getSession(false);
		verify(r2, never()).getSession(true);
		verify(r2, never()).getSession();

		var r3 = req("s1");
		CsrfTokens.perProcess().expected(r3, true);
		verifyNoInteractions(r3);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Source-shape helpers
	//-----------------------------------------------------------------------------------------------------------------

	/** The body of the first method whose declaration contains {@code signature}, braces included. */
	private static String body(String src, String signature) {
		var at = src.indexOf(signature);
		assertTrue(at >= 0, () -> "method not found: " + signature);
		var open = src.indexOf('{', at);
		var depth = 0;
		for (var i = open; i < src.length(); i++) {
			var c = src.charAt(i);
			if (c == '{')
				depth++;
			else if (c == '}' && --depth == 0)
				return src.substring(open, i + 1);
		}
		return fail("unbalanced braces after " + signature);
	}

	private static int count(String s, String needle) {
		return (int)Pattern.compile(Pattern.quote(needle)).matcher(s).results().count();
	}
}
