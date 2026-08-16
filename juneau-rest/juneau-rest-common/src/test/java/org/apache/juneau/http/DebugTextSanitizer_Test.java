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
package org.apache.juneau.http;

import static org.apache.juneau.http.DebugTextSanitizer.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link DebugTextSanitizer} — control-char escaping, cap/truncation, and the original-reference fast path.
 *
 * @since 10.0.0
 */
class DebugTextSanitizer_Test {

	// -----------------------------------------------------------------------------------------
	// a — CR/LF escaping (the core log-forging defense)
	// -----------------------------------------------------------------------------------------

	@Test void a01_crlf_renderedAsSingleInertLine() {
		var s = sanitize("a\r\nb");
		assertEquals("a\\r\\nb", s);
		// The escaped output can never produce a second physical log line.
		assertFalse(s.contains("\r"));
		assertFalse(s.contains("\n"));
	}

	@Test void a02_forgedBanner_cannotStartNewLine() {
		var s = sanitize("GET /x\r\n[200] HTTP GET /admin");
		assertEquals("GET /x\\r\\n[200] HTTP GET /admin", s);
		assertEquals(-1, s.indexOf('\n'));
	}

	// -----------------------------------------------------------------------------------------
	// b — control-char ranges
	// -----------------------------------------------------------------------------------------

	@Test void b01_c0Controls_escapedExceptTab() {
		assertEquals("\\u0000", sanitize("\u0000"));
		assertEquals("\\u0001", sanitize("\u0001"));
		assertEquals("\\u001B", sanitize("\u001B"));  // ANSI ESC
		assertEquals("\\u001F", sanitize("\u001F"));
	}

	@Test void b02_tab_preserved() {
		assertEquals("a\tb", sanitize("a\tb"));
	}

	@Test void b03_delAndC1Controls_escaped() {
		assertEquals("\\u007F", sanitize("\u007F"));  // DEL
		assertEquals("\\u0085", sanitize("\u0085"));  // NEL
		assertEquals("\\u0080", sanitize("\u0080"));
		assertEquals("\\u009F", sanitize("\u009F"));
	}

	@Test void b04_lineAndParagraphSeparators_escaped() {
		assertEquals("\\u2028", sanitize("\u2028"));
		assertEquals("\\u2029", sanitize("\u2029"));
	}

	@Test void b05_bidiControls_escaped() {
		assertEquals("\\u202A", sanitize("\u202A"));
		assertEquals("\\u202E", sanitize("\u202E"));
		assertEquals("\\u2066", sanitize("\u2066"));
		assertEquals("\\u2069", sanitize("\u2069"));
	}

	@Test void b06_ordinaryPrintableUntouched() {
		assertEquals("Hello, world! 123", sanitize("Hello, world! 123"));
		assertEquals("caf\u00e9", sanitize("caf\u00e9"));  // é is printable, not escaped
	}

	// -----------------------------------------------------------------------------------------
	// c — surrogate handling
	// -----------------------------------------------------------------------------------------

	@Test void c01_surrogatePair_preservedIntact() {
		var emoji = "\uD83D\uDE00";  // U+1F600
		assertEquals(emoji, sanitize(emoji));
	}

	@Test void c02_surrogatePair_notSplitByCap() {
		var s = "ab\uD83D\uDE00cd";
		// Cap of 3 characters: "ab" fits (2), the surrogate pair is 2 chars and would exceed 3 → truncate before it.
		var out = sanitize(s, 3);
		assertEquals("ab" + TRUNCATED_MARKER, out);
		// Cap of 4: "ab" + pair (2) = 4 fits exactly.
		assertEquals("ab\uD83D\uDE00" + TRUNCATED_MARKER, sanitize(s, 4));
	}

	// -----------------------------------------------------------------------------------------
	// d — cap / truncation
	// -----------------------------------------------------------------------------------------

	@Test void d01_cap_appendsMarkerAndCountsSanitizedChars() {
		var out = sanitize("abcdef", 3);
		assertEquals("abc" + TRUNCATED_MARKER, out);
	}

	@Test void d02_cap_neverSplitsEscapeSequence() {
		// "\u001B" escapes to 6 chars. With cap 3 it cannot fit, so nothing of it is emitted.
		var out = sanitize("\u001B", 3);
		assertEquals(TRUNCATED_MARKER, out);
		// With cap 6 it fits exactly.
		assertEquals("\\u001B", sanitize("\u001B", 6));
		// With cap 5 it cannot fit (would split), so it is dropped whole.
		assertEquals(TRUNCATED_MARKER, sanitize("\u001B", 5));
	}

	@Test void d03_cap_crlfEscapeNotSplit() {
		// "a" + "\r"(→2) with cap 2 → "a" fits, "\r" needs 2 more (total 3) → truncate.
		assertEquals("a" + TRUNCATED_MARKER, sanitize("a\r", 2));
		// cap 3 → "a\\r" fits.
		assertEquals("a\\r" + TRUNCATED_MARKER, sanitize("a\rb", 3));
	}

	@Test void d04_zeroCap() {
		assertEquals("", sanitize("", 0));
		assertEquals(TRUNCATED_MARKER, sanitize("a", 0));
	}

	@Test void d05_withinCap_noMarker() {
		assertEquals("abc", sanitize("abc", 3));
		assertEquals("abc", sanitize("abc", 100));
	}

	// -----------------------------------------------------------------------------------------
	// e — null / empty / fast path
	// -----------------------------------------------------------------------------------------

	@Test void e01_null() {
		assertNull(sanitize(null));
		assertNull(sanitize(null, 10));
	}

	@Test void e02_empty() {
		assertEquals("", sanitize(""));
		assertSame("", sanitize(""));
	}

	@Test void e03_fastPath_returnsOriginalReference() {
		var s = "no controls here";
		assertSame(s, sanitize(s));
		assertSame(s, sanitize(s, s.length()));
		assertSame(s, sanitize(s, 1000));
	}

	@Test void e04_fastPath_notTakenWhenOverCap() {
		var s = "abcdef";
		assertNotSame(s, sanitize(s, 3));
	}
}
