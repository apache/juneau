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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Regression tests for the Phase-2 CBOR conformance fixes (175fc).
 *
 * <p>
 * Each category guards one audited gap:
 * <ul>
 * 	<li><b>a</b> — GAP-1: 64-bit unsigned integers / negatives beyond signed {@code long}.
 * 	<li><b>b</b> — GAP-2: lossless {@link BigInteger} / {@link BigDecimal} round-trips.
 * 	<li><b>c</b> — GAP-3: indefinite-length text/byte strings (RFC 8949 §3.2.3).
 * 	<li><b>d</b> — GAP-4: half-precision (float16) subnormal decode.
 * 	<li><b>e</b> — GAP-8: unpaired UTF-16 surrogate on encode.
 * </ul>
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class CborConformanceFixes_Test extends TestBase {

	private static String enc(Object input) throws Exception {
		return toSpacedHex(CborSerializer.DEFAULT.serialize(input));
	}

	private static <T> T rt(Object input, Class<T> type) throws Exception {
		return CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize(input), type);
	}

	//------------------------------------------------------------------------------------------------
	// GAP-1 — uint64 / negatives beyond signed long.
	//------------------------------------------------------------------------------------------------

	@Test
	void a01_uint64MaxDecodesToBigInteger() throws Exception {
		// 1B FF FF FF FF FF FF FF FF == CBOR 18446744073709551615 (uint64 max).
		var b = fromHex("1BFFFFFFFFFFFFFFFF");
		assertEquals(new BigInteger("18446744073709551615"), CborParser.DEFAULT.parse(b, BigInteger.class));
	}

	@Test
	void a02_uint64MaxIntoLongKeepsRawBits() throws Exception {
		// Per the type-driven contract a long field keeps the raw 64-bit bits (uint64 max -> -1).
		var b = fromHex("1BFFFFFFFFFFFFFFFF");
		assertEquals(-1L, CborParser.DEFAULT.parse(b, Long.class));
	}

	@Test
	void a03_nintMinDecodesToBigInteger() throws Exception {
		// 3B FF FF FF FF FF FF FF FF == CBOR -18446744073709551616 (nint min).
		var b = fromHex("3BFFFFFFFFFFFFFFFF");
		assertEquals(new BigInteger("-18446744073709551616"), CborParser.DEFAULT.parse(b, BigInteger.class));
	}

	@Test
	void a04_uint64MaxEncodesWithoutTruncation() throws Exception {
		// Previously truncated to CBOR -1 (0x20); must now emit the full 8-byte magnitude.
		assertEquals("1B FF FF FF FF FF FF FF FF", enc(new BigInteger("18446744073709551615")));
	}

	@Test
	void a05_pow63EncodesAsNativeUint() throws Exception {
		// 2^63 exceeds signed long but fits unsigned 64; emit major type 0 with the raw magnitude.
		assertEquals("1B 80 00 00 00 00 00 00 00", enc(new BigInteger("9223372036854775808")));
	}

	@Test
	void a06_nintMinEncodesAsNativeNint() throws Exception {
		// -2^64 is the smallest CBOR negative integer; emit major type 1 with argument 2^64-1.
		assertEquals("3B FF FF FF FF FF FF FF FF", enc(new BigInteger("-18446744073709551616")));
	}

	@Test
	void a07_uint64RoundTrip() throws Exception {
		var v = new BigInteger("18446744073709551615");
		assertEquals(v, rt(v, BigInteger.class));
	}

	//------------------------------------------------------------------------------------------------
	// GAP-2 — BigInteger / BigDecimal losslessness.
	//------------------------------------------------------------------------------------------------

	@Test
	void b01_bigDecimalEncodesAsLosslessString() throws Exception {
		// 0.1 has no exact double; must serialize as the text string "0.1" (63 = major 3, len 3).
		assertEquals("63 30 2E 31", enc(new BigDecimal("0.1")));
	}

	@Test
	void b02_bigDecimalRoundTripLossless() throws Exception {
		var v = new BigDecimal("0.1");
		assertEquals(v, rt(v, BigDecimal.class));
	}

	@Test
	void b03_bigDecimalHighPrecisionRoundTrip() throws Exception {
		var v = new BigDecimal("3.141592653589793238462643383279502884197");
		assertEquals(v, rt(v, BigDecimal.class));
	}

	@Test
	void b04_bigIntegerInLongRangeRoundTrip() throws Exception {
		var v = new BigInteger("7");
		assertEquals("07", enc(v));
		assertEquals(v, rt(v, BigInteger.class));
	}

	@ParameterizedTest
	@ValueSource(strings = {"18446744073709551616", "-5", "-18446744073709551617"})
	void b05_bigIntegerRoundTrip(String value) throws Exception {
		var v = new BigInteger(value);
		assertEquals(v, rt(v, BigInteger.class));
	}

	//------------------------------------------------------------------------------------------------
	// GAP-3 — indefinite-length text / byte strings (RFC 8949 §3.2.3).
	//------------------------------------------------------------------------------------------------

	@Test
	void c01_indefiniteByteStringDecodes() throws Exception {
		// 5F 42 01 02 43 03 04 05 FF == indefinite byte string of chunks {01 02} + {03 04 05}.
		var b = fromHex("5F42010243030405FF");
		assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, CborParser.DEFAULT.parse(b, byte[].class));
	}

	@Test
	void c02_indefiniteTextStringDecodes() throws Exception {
		// 7F 61 61 61 62 FF == indefinite text string of chunks "a" + "b".
		var b = fromHex("7F61616162FF");
		assertEquals("ab", CborParser.DEFAULT.parse(b, String.class));
	}

	@Test
	void c03_indefiniteEmptyTextStringDecodes() throws Exception {
		// 7F FF == indefinite text string with zero chunks -> empty string.
		var b = fromHex("7FFF");
		assertEquals("", CborParser.DEFAULT.parse(b, String.class));
	}

	@Test
	void c04_indefiniteByteStringViaTokenReader() throws Exception {
		var b = fromHex("5F42010243030405FF");
		try (var r = CborParser.DEFAULT.parseTokens(b)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, r.getBinary());
		}
	}

	@Test
	void c05_nestedIndefiniteChunkRejectedCleanly() throws Exception {
		// 5F 5F FF == a byte-string chunk that is itself indefinite; rejected per §3.2.3 with a clean
		// IOException rather than a NegativeArraySizeException.
		var b = fromHex("5F5FFF");
		try (var r = CborParser.DEFAULT.parseTokens(b)) {
			assertThrowsWithMessage(IOException.class, "Nested indefinite-length string chunk not allowed", r::next);
		}
	}

	@Test
	void c06_mismatchedChunkMajorTypeRejectedCleanly() throws Exception {
		// 5F 61 61 FF == a text-string chunk inside an indefinite byte string; rejected cleanly.
		var b = fromHex("5F6161FF");
		try (var r = CborParser.DEFAULT.parseTokens(b)) {
			assertThrowsWithMessage(IOException.class, "Invalid chunk major type", r::next);
		}
	}

	@Test
	void c07_truncatedIndefiniteStringRejectedCleanly() throws Exception {
		// 5F 42 01 02 (no BREAK, EOF mid-stream) must not loop or crash with a runtime exception.
		var b = fromHex("5F42010 2".replace(" ", ""));
		try (var r = CborParser.DEFAULT.parseTokens(b)) {
			assertThrowsWithMessage(IOException.class, "Unexpected end of CBOR input in indefinite-length string", r::next);
		}
	}

	//------------------------------------------------------------------------------------------------
	// GAP-4 — half-precision (float16) subnormal decode.
	//------------------------------------------------------------------------------------------------

	@Test
	void d01_smallestPositiveSubnormal() throws Exception {
		// F9 00 01 == smallest positive half subnormal == 2^-24 (~5.96e-8), previously decoded ~0.
		var b = fromHex("F90001");
		assertEquals(1.0 / 16777216.0, CborParser.DEFAULT.parse(b, Double.class), 0.0);
	}

	@Test
	void d02_largestSubnormal() throws Exception {
		// F9 03 FF == largest half subnormal == 1023 * 2^-24.
		var b = fromHex("F903FF");
		assertEquals(1023.0 / 16777216.0, CborParser.DEFAULT.parse(b, Double.class), 0.0);
	}

	@Test
	void d03_negativeSubnormalKeepsSign() throws Exception {
		// F9 80 01 == negative smallest subnormal.
		var b = fromHex("F98001");
		assertEquals(-1.0 / 16777216.0, CborParser.DEFAULT.parse(b, Double.class), 0.0);
	}

	@Test
	void d04_zeroStillDecodesToZero() throws Exception {
		// F9 00 00 == +0.0 (mant == 0 branch must still be exact zero).
		var b = fromHex("F90000");
		assertEquals(0.0, CborParser.DEFAULT.parse(b, Double.class), 0.0);
	}

	//------------------------------------------------------------------------------------------------
	// GAP-8 — unpaired UTF-16 surrogate on encode.
	//------------------------------------------------------------------------------------------------

	@Test
	void e01_loneHighSurrogateEmitsReplacement() throws Exception {
		// A trailing/unpaired high surrogate must emit U+FFFD (EF BF BD) rather than throwing.
		assertEquals("63 EF BF BD", enc("\uD83D"));
		assertEquals("\uFFFD", rt("\uD83D", String.class));
	}

	@Test
	void e02_loneLowSurrogateEmitsReplacement() throws Exception {
		assertEquals("63 EF BF BD", enc("\uDC00"));
		assertEquals("\uFFFD", rt("\uDC00", String.class));
	}

	@Test
	void e03_highSurrogateFollowedByNonLowSurrogate() throws Exception {
		// High surrogate followed by 'a' (not a low surrogate): replacement char then 'a'.
		assertEquals("\uFFFDa", rt("\uD83Da", String.class));
	}

	@Test
	void e04_validSurrogatePairStillRoundTrips() throws Exception {
		// Regression guard: a proper astral character (U+1F600) must still encode as a 4-byte
		// sequence and round-trip unchanged.
		assertEquals("64 F0 9F 98 80", enc("\uD83D\uDE00"));
		assertEquals("\uD83D\uDE00", rt("\uD83D\uDE00", String.class));
	}

	@Test
	void e05_threeByteBmpCharStillRoundTrips() throws Exception {
		// Regression guard for the non-surrogate branch: a 3-byte BMP char (U+4E2D) must encode as
		// E4 B8 AD and round-trip unchanged (no replacement applied).
		assertEquals("63 E4 B8 AD", enc("\u4E2D"));
		assertEquals("\u4E2D", rt("\u4E2D", String.class));
	}
}
