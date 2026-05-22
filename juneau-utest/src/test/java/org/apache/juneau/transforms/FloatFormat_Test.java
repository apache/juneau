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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FloatFormat_Test {

	//------------------------------------------------------------------------------------------------------------------
	// format(double, ...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_format_finiteDouble_isBoxed() {
		assertEquals(Double.valueOf(3.14), FloatFormat.format(3.14, FloatFormat.NaN_AS_NULL));
		assertEquals(Double.valueOf(-0.5), FloatFormat.format(-0.5, FloatFormat.NaN_AS_STRING));
		assertEquals(Double.valueOf(0.0), FloatFormat.format(0.0, FloatFormat.NaN_AS_NUMBER));
		assertEquals(Double.valueOf(1.0e10), FloatFormat.format(1.0e10, FloatFormat.NaN_AS_ERROR));
	}

	@Test void a02_format_double_nan_asNull() {
		assertNull(FloatFormat.format(Double.NaN, FloatFormat.NaN_AS_NULL));
		assertNull(FloatFormat.format(Double.POSITIVE_INFINITY, FloatFormat.NaN_AS_NULL));
		assertNull(FloatFormat.format(Double.NEGATIVE_INFINITY, FloatFormat.NaN_AS_NULL));
	}

	@Test void a03_format_double_nan_asNotSet() {
		// NOT_SET falls through to NaN_AS_NULL.
		assertNull(FloatFormat.format(Double.NaN, FloatFormat.NOT_SET));
	}

	@Test void a04_format_double_nan_nullFormatTreatedAsNull() {
		assertNull(FloatFormat.format(Double.NaN, null));
	}

	@Test void a05_format_double_nan_asString() {
		assertEquals("NaN", FloatFormat.format(Double.NaN, FloatFormat.NaN_AS_STRING));
		assertEquals("Infinity", FloatFormat.format(Double.POSITIVE_INFINITY, FloatFormat.NaN_AS_STRING));
		assertEquals("-Infinity", FloatFormat.format(Double.NEGATIVE_INFINITY, FloatFormat.NaN_AS_STRING));
	}

	@Test void a06_format_double_nan_asNumber() {
		// Non-finite preserved as boxed Double.
		var r = FloatFormat.format(Double.NaN, FloatFormat.NaN_AS_NUMBER);
		assertTrue(r instanceof Double);
		assertTrue(Double.isNaN((Double) r));
		assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), FloatFormat.format(Double.POSITIVE_INFINITY, FloatFormat.NaN_AS_NUMBER));
	}

	@Test void a07_format_double_nan_asErrorThrows() {
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.format(Double.NaN, FloatFormat.NaN_AS_ERROR));
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.format(Double.POSITIVE_INFINITY, FloatFormat.NaN_AS_ERROR));
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.format(Double.NEGATIVE_INFINITY, FloatFormat.NaN_AS_ERROR));
	}

	//------------------------------------------------------------------------------------------------------------------
	// format(float, ...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_format_finiteFloat_isBoxed() {
		assertEquals(Float.valueOf(3.14f), FloatFormat.format(3.14f, FloatFormat.NaN_AS_NULL));
	}

	@Test void b02_format_float_nan_asNull() {
		assertNull(FloatFormat.format(Float.NaN, FloatFormat.NaN_AS_NULL));
		assertNull(FloatFormat.format(Float.NaN, FloatFormat.NOT_SET));
		assertNull(FloatFormat.format(Float.NaN, null));
	}

	@Test void b03_format_float_nan_asString() {
		assertEquals("NaN", FloatFormat.format(Float.NaN, FloatFormat.NaN_AS_STRING));
		assertEquals("Infinity", FloatFormat.format(Float.POSITIVE_INFINITY, FloatFormat.NaN_AS_STRING));
		assertEquals("-Infinity", FloatFormat.format(Float.NEGATIVE_INFINITY, FloatFormat.NaN_AS_STRING));
	}

	@Test void b04_format_float_nan_asNumber() {
		var r = FloatFormat.format(Float.NaN, FloatFormat.NaN_AS_NUMBER);
		assertTrue(r instanceof Float);
		assertTrue(Float.isNaN((Float) r));
	}

	@Test void b05_format_float_nan_asErrorThrows() {
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.format(Float.NaN, FloatFormat.NaN_AS_ERROR));
	}

	@Test void b06_isNumeric() {
		assertTrue(FloatFormat.NaN_AS_NUMBER.isNumeric());
		assertFalse(FloatFormat.NOT_SET.isNumeric());
		assertFalse(FloatFormat.NaN_AS_NULL.isNumeric());
		assertFalse(FloatFormat.NaN_AS_STRING.isNumeric());
		assertFalse(FloatFormat.NaN_AS_ERROR.isNumeric());
	}

	//------------------------------------------------------------------------------------------------------------------
	// parse — lenient
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_parse_double_bareNumeric() {
		assertEquals(Double.valueOf(3.14), FloatFormat.parse("3.14", FloatFormat.NaN_AS_NULL, Double.class));
	}

	@Test void c02_parse_double_nan_quoted() {
		var r = FloatFormat.parse("\"NaN\"", FloatFormat.NaN_AS_STRING, Double.class);
		assertTrue(Double.isNaN(r));
	}

	@Test void c03_parse_double_nan_bare() {
		var r = FloatFormat.parse("NaN", FloatFormat.NaN_AS_NUMBER, Double.class);
		assertTrue(Double.isNaN(r));
	}

	@Test void c04_parse_double_infinity_caseInsensitive() {
		assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), FloatFormat.parse("Infinity", FloatFormat.NaN_AS_NUMBER, Double.class));
		assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), FloatFormat.parse("INFINITY", FloatFormat.NaN_AS_NUMBER, Double.class));
		assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), FloatFormat.parse("-Infinity", FloatFormat.NaN_AS_NUMBER, Double.class));
		assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), FloatFormat.parse("-INF", FloatFormat.NaN_AS_NUMBER, Double.class));
		assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), FloatFormat.parse("+Inf", FloatFormat.NaN_AS_NUMBER, Double.class));
	}

	@Test void c05_parse_float_target() {
		assertEquals(Float.valueOf(3.14f), FloatFormat.parse("3.14", FloatFormat.NaN_AS_NULL, Float.class));
		var r = FloatFormat.parse("NaN", FloatFormat.NaN_AS_STRING, Float.class);
		assertTrue(Float.isNaN(r));
	}

	@Test void c06_parse_lenient_everyHintAccepts() {
		for (var hint : FloatFormat.values()) {
			assertEquals(Double.valueOf(3.14), FloatFormat.parse("3.14", hint, Double.class), "hint=" + hint);
			assertEquals(Double.valueOf(3.14), FloatFormat.parse("\"3.14\"", hint, Double.class), "hint=" + hint);
		}
	}

	@Test void c07_parse_nullAndBlank() {
		for (var f : FloatFormat.values()) {
			assertNull(FloatFormat.parse(null, f, Double.class), "format=" + f);
			assertNull(FloatFormat.parse("", f, Double.class), "format=" + f);
			assertNull(FloatFormat.parse("   ", f, Double.class), "format=" + f);
		}
	}

	@Test void c08_parse_invalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.parse("not-a-float", FloatFormat.NaN_AS_NULL, Double.class));
	}

	@Test void c09_parse_unsupportedTargetThrows() {
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.parse("3.14", FloatFormat.NaN_AS_NULL, Long.class));
	}

	@Test void c10_parse_nullFormatHint() {
		assertEquals(Double.valueOf(3.14), FloatFormat.parse("3.14", null, Double.class));
	}

	@Test void c11_parse_primitiveTargets() {
		assertEquals(Double.valueOf(3.14), FloatFormat.parse("3.14", FloatFormat.NaN_AS_NULL, double.class));
		assertEquals(Float.valueOf(3.14f), FloatFormat.parse("3.14", FloatFormat.NaN_AS_NULL, float.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Round-trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_roundTrip_finite_double() {
		for (var f : FloatFormat.values()) {
			var formatted = FloatFormat.format(3.14, f);
			assertEquals(Double.valueOf(3.14), FloatFormat.parse(formatted.toString(), f, Double.class), "format=" + f);
		}
	}

	@Test void d02_roundTrip_nanString() {
		// NaN_AS_STRING and NaN_AS_NUMBER preserve NaN through round-trip.
		var s1 = FloatFormat.format(Double.NaN, FloatFormat.NaN_AS_STRING);
		assertTrue(Double.isNaN(FloatFormat.parse(s1.toString(), FloatFormat.NaN_AS_STRING, Double.class)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Quote-stripping and special-token coverage for parseFloat(...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_parse_singleQuotedStrings() {
		assertEquals(Double.valueOf(3.14), FloatFormat.parse("'3.14'", FloatFormat.NaN_AS_NULL, Double.class));
		assertTrue(Double.isNaN(FloatFormat.parse("'NaN'", FloatFormat.NaN_AS_NULL, Double.class)));
	}

	@Test void e02_parse_mismatchedQuotesNotStripped() {
		// Leading quote without matching trailing quote — not stripped, parse fails.
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.parse("\"3.14", FloatFormat.NaN_AS_NULL, Double.class));
	}

	@Test void e03_parse_singleCharNotStripped() {
		// Length 1 — quote-stripping branch is skipped.
		assertThrows(IllegalArgumentException.class, () -> FloatFormat.parse("\"", FloatFormat.NaN_AS_NULL, Double.class));
	}

	@Test void e04_parse_float_specialTokens() {
		// Cover parseFloat's NaN / +Inf / -Inf / +Infinity / -Infinity branches.
		assertTrue(Float.isNaN(FloatFormat.parse("NaN", FloatFormat.NaN_AS_NUMBER, Float.class)));
		assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), FloatFormat.parse("Infinity", FloatFormat.NaN_AS_NUMBER, Float.class));
		assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), FloatFormat.parse("+Infinity", FloatFormat.NaN_AS_NUMBER, Float.class));
		assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), FloatFormat.parse("Inf", FloatFormat.NaN_AS_NUMBER, Float.class));
		assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), FloatFormat.parse("+Inf", FloatFormat.NaN_AS_NUMBER, Float.class));
		assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), FloatFormat.parse("-Infinity", FloatFormat.NaN_AS_NUMBER, Float.class));
		assertEquals(Float.valueOf(Float.NEGATIVE_INFINITY), FloatFormat.parse("-Inf", FloatFormat.NaN_AS_NUMBER, Float.class));
	}
}
