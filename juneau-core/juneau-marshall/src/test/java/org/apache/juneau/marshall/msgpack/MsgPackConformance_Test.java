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
package org.apache.juneau.marshall.msgpack;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Conformance regression tests guarding the Phase-2 MessagePack codec fixes (175fd audit).
 *
 * <p>
 * Each test pins a specific spec-corner / lossless-round-trip gap so that a future regression is caught:
 * <ul>
 * 	<li>G1 &mdash; {@code ext} payload consumption in the databind parser (stream desync).
 * 	<li>G3 &mdash; {@code uint64} above {@code Long.MAX_VALUE} surfaced by target Java type.
 * 	<li>G4 &mdash; {@link BigInteger} exact serialization (no silent truncation).
 * 	<li>G8 &mdash; 32-bit length above {@code Integer.MAX_VALUE} rejected (no negative-size truncation).
 * 	<li>G9 &mdash; lone/unpaired UTF-16 surrogate emitted as U+FFFD.
 * 	<li>Recursion-depth guard on the databind parse path.
 * </ul>
 */
class MsgPackConformance_Test extends TestBase {

	private static byte[] mp(Object o) throws Exception {
		return MsgPackSerializer.DEFAULT.serialize(o);
	}

	private static <T> T parse(String spacedHex, Class<T> type) throws Exception {
		return MsgPackParser.DEFAULT.parse(fromSpacedHex(spacedHex), type);
	}

	//====================================================================================================
	// G1 - ext family handled in the databind parser (payload consumed, no desync).
	//====================================================================================================

	@Test void a01_ext_singleValueSurfacesAsBytes() throws Exception {
		// fixext1, type=5, payload=0x01 -> surfaced as byte[]{0x01}.
		var a = parse("D4 05 01", byte[].class);
		assertArrayEquals(new byte[]{0x01}, a);
	}

	@Test void a02_ext_intoObjectSurfacesAsBytes() throws Exception {
		var a = parse("D4 05 01", Object.class);
		assertInstanceOf(byte[].class, a);
		assertArrayEquals(new byte[]{0x01}, (byte[])a);
	}

	@Test void a03_ext_nestedDoesNotDesyncFollowingElement() throws Exception {
		// [ext(type=5, 0x01), 7] -> the ext payload must be consumed so the trailing 7 is read correctly.
		var a = (List<?>) parse("92 D4 05 01 07", Object.class);
		assertEquals(2, a.size());
		assertArrayEquals(new byte[]{0x01}, (byte[])a.get(0));
		assertEquals(7, ((Number)a.get(1)).intValue());
	}

	@Test void a04_ext8_multiBytePayloadConsumed() throws Exception {
		// ext8, len=3, type=9, payload=0x0A0B0C followed by int 0x07.
		var a = (List<?>) parse("92 C7 03 09 0A 0B 0C 07", Object.class);
		assertEquals(2, a.size());
		assertArrayEquals(new byte[]{0x0A, 0x0B, 0x0C}, (byte[])a.get(0));
		assertEquals(7, ((Number)a.get(1)).intValue());
	}

	//====================================================================================================
	// G3 - uint64 above Long.MAX_VALUE surfaced by target Java type.
	//====================================================================================================

	@Test void b01_uint64_maxIntoBigIntegerKeepsMagnitude() throws Exception {
		// CF FF FF FF FF FF FF FF FF = 2^64 - 1.
		var a = parse("CF FF FF FF FF FF FF FF FF", BigInteger.class);
		assertEquals(new BigInteger("18446744073709551615"), a);
	}

	@Test void b02_uint64_2pow63IntoBigInteger() throws Exception {
		// CF 80 00 00 00 00 00 00 00 = 2^63.
		var a = parse("CF 80 00 00 00 00 00 00 00", BigInteger.class);
		assertEquals(new BigInteger("9223372036854775808"), a);
	}

	@Test void b03_uint64_intoLongKeepsRawBits() throws Exception {
		// Default (long target / generic Object) keeps the raw signed bits: 2^64-1 -> -1L.
		var a = parse("CF FF FF FF FF FF FF FF FF", long.class);
		assertEquals(-1L, a.longValue());
		var b = parse("CF FF FF FF FF FF FF FF FF", Object.class);
		assertEquals(-1L, ((Number)b).longValue());
	}

	@Test void b04_uint64_withinLongRangeIntoBigInteger() throws Exception {
		// High bit clear -> value fits a signed long; BigInteger target still receives the right magnitude.
		var a = parse("CF 00 00 00 00 00 00 00 2A", BigInteger.class);
		assertEquals(BigInteger.valueOf(42), a);
	}

	//====================================================================================================
	// G4 - BigInteger exact serialization (no silent longValue() truncation).
	//====================================================================================================

	@Test void c01_bigInteger_longRangeEmitsInt64() throws Exception {
		assertEquals("D3 7F FF FF FF FF FF FF FF", toSpacedHex(mp(new BigInteger("9223372036854775807"))));
	}

	@Test void c02_bigInteger_2pow63EmitsUint64() throws Exception {
		assertEquals("CF 80 00 00 00 00 00 00 00", toSpacedHex(mp(new BigInteger("9223372036854775808"))));
	}

	@Test void c03_bigInteger_maxUint64EmitsUint64() throws Exception {
		assertEquals("CF FF FF FF FF FF FF FF FF", toSpacedHex(mp(new BigInteger("18446744073709551615"))));
	}

	@Test void c04_bigInteger_aboveUint64Throws() {
		// 2^64 cannot be represented by any MessagePack integer type.
		assertThrowsWithMessage(SerializeException.class, "outside the range supported by MessagePack",
			() -> mp(new BigInteger("18446744073709551616")));
	}

	@Test void c05_bigInteger_negativeBeyondLongThrows() {
		assertThrowsWithMessage(SerializeException.class, "outside the range supported by MessagePack",
			() -> mp(new BigInteger("-9223372036854775809")));
	}

	@Test void c06_bigInteger_roundTrip2pow63() throws Exception {
		var a = new BigInteger("9223372036854775808");
		assertEquals(a, MsgPackParser.DEFAULT.parse(mp(a), BigInteger.class));
	}

	@Test void c07_bigInteger_roundTripMaxUint64() throws Exception {
		var a = new BigInteger("18446744073709551615");
		assertEquals(a, MsgPackParser.DEFAULT.parse(mp(a), BigInteger.class));
	}

	//====================================================================================================
	// G8 - 32-bit length above Integer.MAX_VALUE rejected.
	//====================================================================================================

	@Test void d01_bin32_lengthAbove2pow31Rejected() {
		// bin32 with declared length 0x80000000 (2^31) -> would truncate to a negative int.
		assertThrowsWithMessage(ParseException.class, "exceeds the maximum supported size",
			() -> parse("C6 80 00 00 00", Object.class));
	}

	@Test void d02_array32_lengthAbove2pow31Rejected() {
		assertThrowsWithMessage(ParseException.class, "exceeds the maximum supported size",
			() -> parse("DD 80 00 00 00", Object.class));
	}

	@Test void d03_str32_lengthAbove2pow31Rejected() {
		assertThrowsWithMessage(ParseException.class, "exceeds the maximum supported size",
			() -> parse("DB FF FF FF FF", Object.class));
	}

	@SuppressWarnings({
		"resource" // The token cursor's pipe is closed by try-with-resources; JDT mis-flags the chained factory call.
	})
	@Test void d04_tokenCursorBinLengthAbove2pow31Rejected() throws Exception {
		// The token-cursor BIN path reads through readBinary(), which carries its own length guard.
		try (var r = MsgPackParser.DEFAULT.parseTokens(fromSpacedHex("C6 80 00 00 00"))) {
			assertThrowsWithMessage(IOException.class, "exceeds the maximum supported size", r::next);
		}
	}

	//====================================================================================================
	// G9 - lone/unpaired UTF-16 surrogate emitted as U+FFFD (matches String.getBytes(UTF_8)).
	//====================================================================================================

	@Test void e01_loneLowSurrogateEmitsReplacementChar() throws Exception {
		assertEquals("A3 EF BF BD", toSpacedHex(mp("\uDC00")));
	}

	@Test void e02_loneHighSurrogateAtEndEmitsReplacementChar() throws Exception {
		assertEquals("A3 EF BF BD", toSpacedHex(mp("\uD800")));
	}

	@Test void e03_validSurrogatePairStillEmits4Bytes() throws Exception {
		// U+1F600 GRINNING FACE -> F0 9F 98 80.
		assertEquals("A4 F0 9F 98 80", toSpacedHex(mp("\uD83D\uDE00")));
	}

	@Test void e04_loneSurrogateRoundTripsToReplacementChar() throws Exception {
		var a = MsgPackParser.DEFAULT.parse(mp("\uDC00"), String.class);
		assertEquals("\uFFFD", a);
	}

	//====================================================================================================
	// Recursion-depth guard on the databind parse path.
	//====================================================================================================

	@Test void f01_deeplyNestedArraysFailWithParseException() {
		// 1100 nested fixarray(1) headers then a terminal int 0 -> exceeds MAX_PARSE_DEPTH (1000).
		var sb = new StringBuilder();
		for (var i = 0; i < 1100; i++)
			sb.append("91 ");
		sb.append("00");
		// The contract is graceful failure (a ParseException, NOT a raw StackOverflowError). Which guard
		// trips first is stack-size dependent: on a roomy stack the soft MAX_PARSE_DEPTH guard fires
		// ("Maximum parse depth exceeded"); on a constrained CI thread stack a real StackOverflowError is
		// caught and rewrapped ("Depth too deep.  Stack overflow occurred.") below the soft limit. Accept either.
		var input = sb.toString();
		var e = assertThrows(ParseException.class, () -> parse(input, Object.class));
		var msg = String.valueOf(e.getMessage());
		assertTrue(
			msg.contains("Maximum parse depth exceeded") || msg.contains("Depth too deep"),
			"Expected a graceful depth-failure ParseException.  Actual:\n" + msg);
	}

	@Test void f02_moderateNestingStillParses() throws Exception {
		// 10 nested arrays then int 5 -> well within the depth budget.
		var sb = new StringBuilder();
		for (var i = 0; i < 10; i++)
			sb.append("91 ");
		sb.append("05");
		var a = parse(sb.toString(), Object.class);
		for (var i = 0; i < 10; i++)
			a = ((List<?>)a).get(0);
		assertEquals(5, ((Number)a).intValue());
	}
}
