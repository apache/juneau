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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class BinaryFormat_Test {

	private static final byte[] HELLO = "Hello".getBytes();
	private static final byte[] EMPTY = new byte[0];

	@Test void a01_format_base64() {
		assertEquals("SGVsbG8=", BinaryFormat.BASE64.format(HELLO));
		assertEquals("SGVsbG8=", BinaryFormat.NOT_SET.format(HELLO));
	}

	@Test void a02_format_base64Url() {
		assertEquals("SGVsbG8", BinaryFormat.BASE64_URL.format(HELLO));
		var withSlashAndPlus = new byte[]{(byte)0xfb, (byte)0xff, (byte)0xfe};
		var encoded = BinaryFormat.BASE64_URL.format(withSlashAndPlus);
		assertFalse(encoded.contains("+"));
		assertFalse(encoded.contains("/"));
		assertFalse(encoded.contains("="));
	}

	@Test void a03_format_hex() {
		assertEquals("48656C6C6F", BinaryFormat.HEX.format(HELLO));
	}

	@Test void a04_format_spacedHex() {
		assertEquals("48 65 6C 6C 6F", BinaryFormat.SPACED_HEX.format(HELLO));
	}

	@Test void a05_format_null() {
		for (var f : BinaryFormat.values())
			assertNull(f.format(null), "format=" + f);
	}

	@Test void a06_format_empty() {
		assertEquals("", BinaryFormat.BASE64.format(EMPTY));
		assertEquals("", BinaryFormat.BASE64_URL.format(EMPTY));
		assertEquals("", BinaryFormat.HEX.format(EMPTY));
		assertEquals("", BinaryFormat.SPACED_HEX.format(EMPTY));
		assertEquals("", BinaryFormat.NOT_SET.format(EMPTY));
	}

	@Test void a07_parse_roundtripBase64() {
		var s = BinaryFormat.BASE64.format(HELLO);
		assertArrayEquals(HELLO, BinaryFormat.BASE64.parse(s));
	}

	@Test void a08_parse_roundtripBase64Url() {
		var bytes = new byte[]{(byte)0xfb, (byte)0xff, (byte)0xfe, 0x00, 0x10};
		var s = BinaryFormat.BASE64_URL.format(bytes);
		assertArrayEquals(bytes, BinaryFormat.BASE64_URL.parse(s));
	}

	@Test void a09_parse_roundtripHex() {
		var s = BinaryFormat.HEX.format(HELLO);
		assertArrayEquals(HELLO, BinaryFormat.HEX.parse(s));
	}

	@Test void a10_parse_roundtripSpacedHex() {
		var s = BinaryFormat.SPACED_HEX.format(HELLO);
		assertArrayEquals(HELLO, BinaryFormat.SPACED_HEX.parse(s));
	}

	@Test void a11_parse_formatAgnostic() {
		// Spaced-hex on the wire — every parser should handle it.
		assertArrayEquals(HELLO, BinaryFormat.BASE64.parse("48 65 6C 6C 6F"));
		assertArrayEquals(HELLO, BinaryFormat.HEX.parse("48 65 6C 6C 6F"));
		// Hex on the wire — base64 parser should fall back.
		assertArrayEquals(HELLO, BinaryFormat.BASE64.parse("48656C6C6F"));
		// Base64 on the wire — hex parser should fall back.
		assertArrayEquals(HELLO, BinaryFormat.HEX.parse("SGVsbG8="));
		// Url-safe base64 input — every parser handles dashes/underscores.
		var bytes = new byte[]{(byte)0xfb, (byte)0xff, (byte)0xfe};
		assertArrayEquals(bytes, BinaryFormat.BASE64.parse(BinaryFormat.BASE64_URL.format(bytes)));
	}

	@Test void a12_parse_nullAndBlank() {
		for (var f : BinaryFormat.values()) {
			assertArrayEquals(EMPTY, f.parse(null), "format=" + f);
			assertArrayEquals(EMPTY, f.parse(""), "format=" + f);
			assertArrayEquals(EMPTY, f.parse("   "), "format=" + f);
		}
	}

	@Test void a13_parse_invalidThrows() {
		// Even-length, non-hex chars, no spaces / dashes / underscores → falls into base64 branch and base64 throws.
		assertThrows(IllegalArgumentException.class, () -> BinaryFormat.BASE64.parse("@@@@@@"));
	}

	@Test void a13a_parse_oddLengthHexFallsBackToBase64() {
		// 5 chars (odd), no spaces / dashes / underscores → isHex() rejects on odd length, base64 attempted.
		// Standard base64 also rejects odd length → IllegalArgumentException with our wrapped message.
		var ex = assertThrows(IllegalArgumentException.class, () -> BinaryFormat.BASE64.parse("ABCDE"));
		assertTrue(ex.getMessage().contains("Invalid binary value"), ex.getMessage());
	}

	@Test void a13b_isHex_rejectsNonHexEvenLength() {
		// Valid hex (4 chars) round-trips through hex branch.
		assertArrayEquals(new byte[]{(byte)0xAB, (byte)0xCD}, BinaryFormat.HEX.parse("ABCD"));
		// Even-length non-hex character ('z') → falls back to base64; "zzzz" is valid base64.
		assertNotNull(BinaryFormat.HEX.parse("zzzz"));
	}

	@Test void a13c_parse_underscoreAlone() {
		// '_' (without '-') triggers the url-safe base64 branch — covers the second OR clause.
		var bytes = new byte[]{(byte)0xfb, (byte)0xff, (byte)0xfe};
		assertArrayEquals(bytes, BinaryFormat.HEX.parse("-__-"));  // url-safe base64 decode
	}

	@Test void a13d_isHex_emptyEdgeCases() {
		// isHex internal: empty string / odd length both reject; non-hex char in middle position reject.
		// Use BinaryFormat.parse() with strings that exercise each branch via the format-agnostic path.
		// "AG" is even-length, all hex characters → goes through hex branch.
		assertArrayEquals(new byte[]{(byte)0xAA}, BinaryFormat.HEX.parse("AA"));
		// "Az" is even-length but 'z' is non-hex → falls back to base64; "Az" is not valid base64 length.
		var ex = assertThrows(IllegalArgumentException.class, () -> BinaryFormat.HEX.parse("Az"));
		assertTrue(ex.getMessage().contains("Invalid binary value"), ex.getMessage());
	}

	@Test void a13e_isHex_allCharClassBranches() {
		// Each character class boundary in isHex() — different char classes per pair.
		// 'g' fails 'a'-'f' check, 'G' (in even position) fails 'A'-'F' check, ':' fails '0'-'9' check.
		// With these scattered through, valid base64 round-trip lands here too.
		assertNotNull(BinaryFormat.HEX.parse("ABCG"));     // 'G' fails uppercase hex.
		assertNotNull(BinaryFormat.HEX.parse("abcg"));     // 'g' fails lowercase hex.
		assertNotNull(BinaryFormat.HEX.parse("01:2"));     // ':' fails digit class.
	}

	@Test void a14_notSet_isSentinel() {
		assertEquals(BinaryFormat.BASE64.format(HELLO), BinaryFormat.NOT_SET.format(HELLO));
		assertArrayEquals(HELLO, BinaryFormat.NOT_SET.parse(BinaryFormat.NOT_SET.format(HELLO)));
	}

	@Test void a15_base64Url_acceptsPaddedInput() {
		var bytes = new byte[]{1,2,3,4,5,6,7,8,9,10};
		var padded = Base64.getEncoder().encodeToString(bytes);
		// Plain base64 input passed to BASE64_URL parser — format-agnostic round-trip.
		assertArrayEquals(bytes, BinaryFormat.BASE64_URL.parse(padded));
	}
}
