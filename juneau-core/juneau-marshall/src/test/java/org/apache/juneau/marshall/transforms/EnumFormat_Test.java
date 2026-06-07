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

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class EnumFormat_Test {

	enum E {
		ALPHA_BETA, GAMMA;

		@Override
		public String toString() {
			// Override to validate that TO_STRING vs NAME differ.
			return name().toLowerCase().replace('_', ' ');
		}
	}

	enum NoOverride { FOO, BAR_BAZ }

	@Test void a01_format_toString_isDefault() {
		assertEquals("alpha beta", EnumFormat.NOT_SET.format(E.ALPHA_BETA));
		assertEquals("alpha beta", EnumFormat.TO_STRING.format(E.ALPHA_BETA));
	}

	@Test void a02_format_name() {
		assertEquals("ALPHA_BETA", EnumFormat.NAME.format(E.ALPHA_BETA));
		assertEquals("GAMMA", EnumFormat.NAME.format(E.GAMMA));
	}

	@Test void a03_format_lowerHyphen() {
		assertEquals("alpha-beta", EnumFormat.LOWER_HYPHEN.format(E.ALPHA_BETA));
	}

	@Test void a04_format_upperHyphen() {
		assertEquals("ALPHA-BETA", EnumFormat.UPPER_HYPHEN.format(E.ALPHA_BETA));
	}

	@Test void a05_format_lowerUnderscore() {
		assertEquals("alpha_beta", EnumFormat.LOWER_UNDERSCORE.format(E.ALPHA_BETA));
	}

	@Test void a06_format_lower() {
		assertEquals("alpha_beta", EnumFormat.LOWER.format(E.ALPHA_BETA));
	}

	@Test void a07_format_upper() {
		assertEquals("ALPHA_BETA", EnumFormat.UPPER.format(E.ALPHA_BETA));
	}

	@Test void a08_format_ordinal() {
		assertEquals("0", EnumFormat.ORDINAL.format(E.ALPHA_BETA));
		assertEquals("1", EnumFormat.ORDINAL.format(E.GAMMA));
	}

	@Test void a09_format_null() {
		for (var f : EnumFormat.values())
			assertNull(f.format(null), "format=" + f);
	}

	@Test void a10_isNumeric() {
		assertTrue(EnumFormat.ORDINAL.isNumeric());
		assertFalse(EnumFormat.NOT_SET.isNumeric());
		assertFalse(EnumFormat.TO_STRING.isNumeric());
		assertFalse(EnumFormat.NAME.isNumeric());
		assertFalse(EnumFormat.LOWER_HYPHEN.isNumeric());
		assertFalse(EnumFormat.UPPER_HYPHEN.isNumeric());
		assertFalse(EnumFormat.LOWER_UNDERSCORE.isNumeric());
		assertFalse(EnumFormat.LOWER.isNumeric());
		assertFalse(EnumFormat.UPPER.isNumeric());
	}

	@Test void a11_parse_byName() {
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("ALPHA_BETA", E.class));
	}

	@Test void a12_parse_byToString() {
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("alpha beta", E.class));
	}

	@Test void a13_parse_byOrdinal() {
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("0", E.class));
		assertEquals(E.GAMMA, EnumFormat.parse("1", E.class));
	}

	@Test void a14_parse_byHyphenForms() {
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("alpha-beta", E.class));
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("ALPHA-BETA", E.class));
	}

	@Test void a15_parse_caseInsensitive() {
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("alpha_beta", E.class));
	}

	@Test void a16_parse_formatAgnostic() {
		// Every constant should accept every wire shape produced by another constant.
		for (var fmt : EnumFormat.values()) {
			if (fmt == EnumFormat.NOT_SET)
				continue;
			for (var producer : EnumFormat.values()) {
				if (producer == EnumFormat.NOT_SET)
					continue;
				var s = producer.format(E.ALPHA_BETA);
				assertEquals(E.ALPHA_BETA, EnumFormat.parse(s, E.class), "fmt=" + fmt + " producer=" + producer);
			}
		}
	}

	@Test void a17_parse_nullAndBlank() {
		for (var f : EnumFormat.values()) {
			assertNull(EnumFormat.parse(null, E.class), "format=" + f);
			assertNull(EnumFormat.parse("", E.class), "format=" + f);
			assertNull(EnumFormat.parse("   ", E.class), "format=" + f);
		}
	}

	@Test void a18_parse_invalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> EnumFormat.parse("DOES_NOT_EXIST", E.class));
	}

	@Test void a19_parse_outOfRangeOrdinalFallsThrough() {
		assertThrows(IllegalArgumentException.class, () -> EnumFormat.parse("99", E.class));
	}

	@Test void a20_parse_negativeOrdinalFallsThrough() {
		assertThrows(IllegalArgumentException.class, () -> EnumFormat.parse("-1", E.class));
	}

	@Test void a21_parse_noToStringOverride() {
		// Enum without toString() override — name() and toString() are the same.
		assertEquals(NoOverride.BAR_BAZ, EnumFormat.parse("BAR_BAZ", NoOverride.class));
		assertEquals(NoOverride.BAR_BAZ, EnumFormat.parse("bar-baz", NoOverride.class));
		assertEquals(NoOverride.BAR_BAZ, EnumFormat.parse("bar_baz", NoOverride.class));
		assertEquals(NoOverride.BAR_BAZ, EnumFormat.parse("1", NoOverride.class));
	}

	@Test void a22_format_noToStringOverride() {
		// On enums without toString() override, TO_STRING and NAME emit the same value.
		assertEquals("BAR_BAZ", EnumFormat.TO_STRING.format(NoOverride.BAR_BAZ));
		assertEquals("BAR_BAZ", EnumFormat.NAME.format(NoOverride.BAR_BAZ));
	}

	@Test void a23_parse_isAllDigitsBranches() {
		// Isolates the '+' prefix branch of isAllDigits — must still match a valid ordinal.
		assertEquals(NoOverride.BAR_BAZ, EnumFormat.parse("+1", NoOverride.class));
	}

	@Test void a24_parse_signOnly() {
		// "+" / "-" alone fail isAllDigits → fall through to name match (no match → throw).
		assertThrows(IllegalArgumentException.class, () -> EnumFormat.parse("+", E.class));
		assertThrows(IllegalArgumentException.class, () -> EnumFormat.parse("-", E.class));
	}

	@Test void a25_parse_caseInsensitiveToStringFallback() {
		// Case-insensitive toString() fallback (loop at line 148): "ALPHA BETA" doesn't match name() exact,
		// doesn't match toString() exact, doesn't match snake-form, but matches case-insensitively.
		assertEquals(E.ALPHA_BETA, EnumFormat.parse("ALPHA BETA", E.class));
	}

	@Test void a26_parse_isAllDigits_charBelowZero() {
		// Char below '0' (e.g. '/') exercises the isAllDigits c < '0' short-circuit.
		assertThrows(IllegalArgumentException.class, () -> EnumFormat.parse("/123", E.class));
	}
}
