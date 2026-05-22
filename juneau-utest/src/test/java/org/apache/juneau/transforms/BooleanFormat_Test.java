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

class BooleanFormat_Test {

	@Test void a01_format_trueFalse() {
		assertEquals(Boolean.TRUE, BooleanFormat.format(true, BooleanFormat.TRUE_FALSE));
		assertEquals(Boolean.FALSE, BooleanFormat.format(false, BooleanFormat.TRUE_FALSE));
	}

	@Test void a02_format_notSetFallsThroughToTrueFalse() {
		assertEquals(Boolean.TRUE, BooleanFormat.format(true, BooleanFormat.NOT_SET));
		assertEquals(Boolean.FALSE, BooleanFormat.format(false, BooleanFormat.NOT_SET));
	}

	@Test void a03_format_nullFormatTreatedAsTrueFalse() {
		assertEquals(Boolean.TRUE, BooleanFormat.format(true, null));
		assertEquals(Boolean.FALSE, BooleanFormat.format(false, null));
	}

	@Test void a04_format_zeroOne() {
		assertEquals(Integer.valueOf(1), BooleanFormat.format(true, BooleanFormat.ZERO_ONE));
		assertEquals(Integer.valueOf(0), BooleanFormat.format(false, BooleanFormat.ZERO_ONE));
	}

	@Test void a05_format_yesNo() {
		assertEquals("yes", BooleanFormat.format(true, BooleanFormat.YES_NO));
		assertEquals("no", BooleanFormat.format(false, BooleanFormat.YES_NO));
	}

	@Test void a06_format_yN() {
		assertEquals("Y", BooleanFormat.format(true, BooleanFormat.Y_N));
		assertEquals("N", BooleanFormat.format(false, BooleanFormat.Y_N));
	}

	@Test void a07_format_onOff() {
		assertEquals("on", BooleanFormat.format(true, BooleanFormat.ON_OFF));
		assertEquals("off", BooleanFormat.format(false, BooleanFormat.ON_OFF));
	}

	@Test void a08_isNumeric() {
		assertTrue(BooleanFormat.ZERO_ONE.isNumeric());
		assertFalse(BooleanFormat.NOT_SET.isNumeric());
		assertFalse(BooleanFormat.TRUE_FALSE.isNumeric());
		assertFalse(BooleanFormat.YES_NO.isNumeric());
		assertFalse(BooleanFormat.Y_N.isNumeric());
		assertFalse(BooleanFormat.ON_OFF.isNumeric());
	}

	@Test void b01_parse_true_allShapes() {
		for (var f : BooleanFormat.values()) {
			assertEquals(Boolean.TRUE, BooleanFormat.parse("true", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("TRUE", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("1", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("yes", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("YES", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("y", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("Y", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("on", f), "format=" + f);
			assertEquals(Boolean.TRUE, BooleanFormat.parse("ON", f), "format=" + f);
		}
	}

	@Test void b02_parse_false_allShapes() {
		for (var f : BooleanFormat.values()) {
			assertEquals(Boolean.FALSE, BooleanFormat.parse("false", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("FALSE", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("0", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("no", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("NO", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("n", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("N", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("off", f), "format=" + f);
			assertEquals(Boolean.FALSE, BooleanFormat.parse("OFF", f), "format=" + f);
		}
	}

	@Test void b03_parse_nullAndBlank() {
		for (var f : BooleanFormat.values()) {
			assertNull(BooleanFormat.parse(null, f), "format=" + f);
			assertNull(BooleanFormat.parse("", f), "format=" + f);
			assertNull(BooleanFormat.parse("   ", f), "format=" + f);
		}
	}

	@Test void b04_parse_nullFormatHint() {
		assertEquals(Boolean.TRUE, BooleanFormat.parse("yes", null));
		assertEquals(Boolean.FALSE, BooleanFormat.parse("0", null));
	}

	@Test void b05_parse_invalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> BooleanFormat.parse("maybe", BooleanFormat.TRUE_FALSE));
		assertThrows(IllegalArgumentException.class, () -> BooleanFormat.parse("2", BooleanFormat.ZERO_ONE));
	}

	@Test void b06_parse_trimsWhitespace() {
		assertEquals(Boolean.TRUE, BooleanFormat.parse("  yes  ", BooleanFormat.YES_NO));
	}

	@Test void c01_roundTrip_everyFormat() {
		for (var f : BooleanFormat.values()) {
			for (var v : new boolean[] { true, false }) {
				var formatted = BooleanFormat.format(v, f);
				var s = formatted.toString();
				assertEquals(Boolean.valueOf(v), BooleanFormat.parse(s, f), "format=" + f + " value=" + v);
			}
		}
	}
}
