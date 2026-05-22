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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class BigNumberFormat_Test {

	private static final BigInteger SMALL = BigInteger.valueOf(42L);
	private static final BigInteger JS_MAX_SAFE = BigInteger.valueOf(9007199254740991L); // 2^53 - 1
	private static final BigInteger JS_MAX_SAFE_PLUS_ONE = JS_MAX_SAFE.add(BigInteger.ONE);
	private static final BigInteger JS_MIN_SAFE = JS_MAX_SAFE.negate();
	private static final BigInteger JS_MIN_SAFE_MINUS_ONE = JS_MIN_SAFE.subtract(BigInteger.ONE);
	private static final BigInteger HUGE = new BigInteger("123456789012345678901234567890");

	private static final BigDecimal D_SMALL = new BigDecimal("42");
	private static final BigDecimal D_FRAC = new BigDecimal("3.14");
	private static final BigDecimal D_HUGE = new BigDecimal("123456789012345678901234567890");

	//------------------------------------------------------------------------------------------------------------------
	// format(BigInteger,...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_format_int_number() {
		assertEquals(SMALL, BigNumberFormat.format(SMALL, BigNumberFormat.NUMBER));
		assertEquals(HUGE, BigNumberFormat.format(HUGE, BigNumberFormat.NUMBER));
	}

	@Test void a02_format_int_notSetFallsThrough() {
		assertEquals(SMALL, BigNumberFormat.format(SMALL, BigNumberFormat.NOT_SET));
	}

	@Test void a03_format_int_nullFormatTreatedAsNumber() {
		assertEquals(SMALL, BigNumberFormat.format(SMALL, null));
	}

	@Test void a04_format_int_string() {
		assertEquals("42", BigNumberFormat.format(SMALL, BigNumberFormat.STRING));
		assertEquals(HUGE.toString(), BigNumberFormat.format(HUGE, BigNumberFormat.STRING));
	}

	@Test void a05_format_int_auto_safe() {
		assertEquals(SMALL, BigNumberFormat.format(SMALL, BigNumberFormat.AUTO));
	}

	@Test void a06_format_int_auto_jsMaxSafeBoundaryInclusive() {
		// 2^53 - 1 is INCLUDED in safe range.
		assertEquals(JS_MAX_SAFE, BigNumberFormat.format(JS_MAX_SAFE, BigNumberFormat.AUTO));
		assertEquals(JS_MIN_SAFE, BigNumberFormat.format(JS_MIN_SAFE, BigNumberFormat.AUTO));
	}

	@Test void a07_format_int_auto_jsMaxSafeBoundaryExclusive() {
		// 2^53 (one more than max safe) flips to STRING.
		assertEquals(JS_MAX_SAFE_PLUS_ONE.toString(), BigNumberFormat.format(JS_MAX_SAFE_PLUS_ONE, BigNumberFormat.AUTO));
		assertEquals(JS_MIN_SAFE_MINUS_ONE.toString(), BigNumberFormat.format(JS_MIN_SAFE_MINUS_ONE, BigNumberFormat.AUTO));
	}

	@Test void a08_format_int_auto_huge() {
		assertEquals(HUGE.toString(), BigNumberFormat.format(HUGE, BigNumberFormat.AUTO));
	}

	@Test void a09_format_int_null() {
		for (var f : BigNumberFormat.values())
			assertNull(BigNumberFormat.format((BigInteger) null, f), "format=" + f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// format(BigDecimal,...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_format_dec_number() {
		assertEquals(D_SMALL, BigNumberFormat.format(D_SMALL, BigNumberFormat.NUMBER));
		assertEquals(D_FRAC, BigNumberFormat.format(D_FRAC, BigNumberFormat.NUMBER));
	}

	@Test void b02_format_dec_string() {
		assertEquals("42", BigNumberFormat.format(D_SMALL, BigNumberFormat.STRING));
		assertEquals("3.14", BigNumberFormat.format(D_FRAC, BigNumberFormat.STRING));
	}

	@Test void b03_format_dec_auto_zeroScaleSafe() {
		assertEquals(D_SMALL, BigNumberFormat.format(D_SMALL, BigNumberFormat.AUTO));
	}

	@Test void b04_format_dec_auto_zeroScaleHuge() {
		assertEquals(D_HUGE.toPlainString(), BigNumberFormat.format(D_HUGE, BigNumberFormat.AUTO));
	}

	@Test void b05_format_dec_auto_nonZeroScaleAlwaysString() {
		// Even small fractional values cannot round-trip through JS doubles → AUTO always uses STRING.
		assertEquals("3.14", BigNumberFormat.format(D_FRAC, BigNumberFormat.AUTO));
		assertEquals("0.1", BigNumberFormat.format(new BigDecimal("0.1"), BigNumberFormat.AUTO));
	}

	@Test void b06_format_dec_notSetFallsThrough() {
		assertEquals(D_SMALL, BigNumberFormat.format(D_SMALL, BigNumberFormat.NOT_SET));
	}

	@Test void b07_format_dec_nullFormat() {
		assertEquals(D_SMALL, BigNumberFormat.format(D_SMALL, null));
	}

	@Test void b08_format_dec_null() {
		for (var f : BigNumberFormat.values())
			assertNull(BigNumberFormat.format((BigDecimal) null, f), "format=" + f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parse — lenient
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_parse_int_bareNumeric() {
		assertEquals(SMALL, BigNumberFormat.parse("42", BigNumberFormat.NUMBER, BigInteger.class));
	}

	@Test void c02_parse_int_quotedDouble() {
		assertEquals(SMALL, BigNumberFormat.parse("\"42\"", BigNumberFormat.STRING, BigInteger.class));
	}

	@Test void c03_parse_int_quotedSingle() {
		assertEquals(SMALL, BigNumberFormat.parse("'42'", BigNumberFormat.STRING, BigInteger.class));
	}

	@Test void c04_parse_int_lenient_everyHintAcceptsBoth() {
		for (var hint : BigNumberFormat.values()) {
			assertEquals(SMALL, BigNumberFormat.parse("42", hint, BigInteger.class), "hint=" + hint);
			assertEquals(SMALL, BigNumberFormat.parse("\"42\"", hint, BigInteger.class), "hint=" + hint);
		}
	}

	@Test void c05_parse_dec_bareNumeric() {
		assertEquals(D_FRAC, BigNumberFormat.parse("3.14", BigNumberFormat.NUMBER, BigDecimal.class));
	}

	@Test void c06_parse_dec_quoted() {
		assertEquals(D_FRAC, BigNumberFormat.parse("\"3.14\"", BigNumberFormat.STRING, BigDecimal.class));
	}

	@Test void c07_parse_huge_bothShapes() {
		assertEquals(HUGE, BigNumberFormat.parse(HUGE.toString(), BigNumberFormat.AUTO, BigInteger.class));
		assertEquals(HUGE, BigNumberFormat.parse("\"" + HUGE + "\"", BigNumberFormat.AUTO, BigInteger.class));
	}

	@Test void c08_parse_nullAndBlank() {
		for (var f : BigNumberFormat.values()) {
			assertNull(BigNumberFormat.parse(null, f, BigInteger.class), "format=" + f);
			assertNull(BigNumberFormat.parse("", f, BigInteger.class), "format=" + f);
			assertNull(BigNumberFormat.parse("   ", f, BigInteger.class), "format=" + f);
		}
	}

	@Test void c09_parse_invalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> BigNumberFormat.parse("not-a-number", BigNumberFormat.NUMBER, BigInteger.class));
	}

	@Test void c10_parse_unsupportedTargetThrows() {
		assertThrows(IllegalArgumentException.class, () -> BigNumberFormat.parse("42", BigNumberFormat.NUMBER, Long.class));
	}

	@Test void c11_parse_nullFormatHint() {
		assertEquals(SMALL, BigNumberFormat.parse("42", null, BigInteger.class));
	}

	@Test void c12_parse_singleCharNotQuoted() {
		// Single char is not stripped as a quoted value (would produce empty string).
		assertEquals(BigInteger.valueOf(7), BigNumberFormat.parse("7", BigNumberFormat.NUMBER, BigInteger.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// isNumeric
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_isNumeric() {
		assertTrue(BigNumberFormat.NUMBER.isNumeric());
		assertFalse(BigNumberFormat.NOT_SET.isNumeric());
		assertFalse(BigNumberFormat.STRING.isNumeric());
		assertFalse(BigNumberFormat.AUTO.isNumeric());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Round-trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_roundTrip_int() {
		for (var f : BigNumberFormat.values()) {
			var formatted = BigNumberFormat.format(HUGE, f);
			var s = formatted instanceof BigInteger bi ? bi.toString() : formatted.toString();
			assertEquals(HUGE, BigNumberFormat.parse(s, f, BigInteger.class), "format=" + f);
		}
	}

	@Test void e02_roundTrip_dec() {
		for (var f : BigNumberFormat.values()) {
			var formatted = BigNumberFormat.format(D_HUGE, f);
			var s = formatted instanceof BigDecimal bd ? bd.toPlainString() : formatted.toString();
			assertEquals(D_HUGE, BigNumberFormat.parse(s, f, BigDecimal.class), "format=" + f);
		}
	}
}
