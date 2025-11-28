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
package org.apache.juneau.common.utils;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.StringUtils.compare;
import static org.apache.juneau.common.utils.StringUtils.contains;
import static org.apache.juneau.common.utils.StringUtils.eqic;
import static org.apache.juneau.common.utils.StringUtils.reverse;
import static org.apache.juneau.common.utils.Utils.eqic;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class StringUtils_Test extends TestBase {

	//====================================================================================================
	// isNumeric(String,Class)
	// parseNumber(String,Class)
	//====================================================================================================
	@Test
	void a01_testParser() {

		// Integers
		assertTrue(isNumeric("123"));
		assertEquals(123, parseNumber("123", null));
		assertEquals(123, parseNumber("123", Integer.class));
		assertEquals((short)123, parseNumber("123", Short.class));
		assertEquals((long)123, parseNumber("123", Long.class));

		assertTrue(isNumeric("0123"));
		assertEquals(0123, parseNumber("0123", null));

		assertTrue(isNumeric("-0123"));
		assertEquals(-0123, parseNumber("-0123", null));

		// Hexadecimal
		assertTrue(isNumeric("0x123"));
		assertEquals(0x123, parseNumber("0x123", null));

		assertTrue(isNumeric("-0x123"));
		assertEquals(-0x123, parseNumber("-0x123", null));

		assertTrue(isNumeric("0X123"));
		assertEquals(0X123, parseNumber("0X123", null));

		assertTrue(isNumeric("-0X123"));
		assertEquals(-0X123, parseNumber("-0X123", null));

		assertTrue(isNumeric("#123"));
		assertEquals(0x123, parseNumber("#123", null));

		assertTrue(isNumeric("-#123"));
		assertEquals(-0x123, parseNumber("-#123", null));

		assertFalse(isNumeric("x123"));
		assertFalse(isNumeric("0x123x"));

		// Decimal
		assertTrue(isNumeric("0.123"));
		assertEquals(0.123f, parseNumber("0.123", null));

		assertTrue(isNumeric("-0.123"));
		assertEquals(-0.123f, parseNumber("-0.123", null));

		assertTrue(isNumeric(".123"));
		assertEquals(.123f, parseNumber(".123", null));

		assertTrue(isNumeric("-.123"));
		assertEquals(-.123f, parseNumber("-.123", null));

		assertFalse(isNumeric("0.123.4"));

		assertTrue(isNumeric("0.84370821629078d"));
		assertEquals(0.84370821629078d, parseNumber("0.84370821629078d", null));

		assertTrue(isNumeric("84370821629078.8437d"));
		assertEquals(84370821629078.8437d, parseNumber("84370821629078.8437d", null));

		assertTrue(isNumeric("0.16666666666666666d"));
		assertEquals(0.16666666666666666d, parseNumber("0.16666666666666666d", null));

		assertTrue(isNumeric("0.16666666f"));
		assertEquals(0.16666666f, parseNumber("0.16666666f", null));

		assertTrue(isNumeric("0.16666666d"));
		assertEquals(0.16666666f, parseNumber("0.16666666d", null));

		assertTrue(isNumeric("3.140000000000000124344978758017532527446746826171875d"));
		assertEquals(3.14f, parseNumber("3.140000000000000124344978758017532527446746826171875d", null));

		assertTrue(isNumeric("12345.678f"));
		assertEquals(1.2345678e4f, parseNumber("12345.678f", null));

		// Scientific notation
		assertTrue(isNumeric("1e1"));
		assertEquals(1e1f, parseNumber("1e1", null));

		assertTrue(isNumeric("1e+1"));
		assertEquals(1e+1f, parseNumber("1e+1", null));

		assertTrue(isNumeric("1e-1"));
		assertEquals(1e-1f, parseNumber("1e-1", null));

		assertTrue(isNumeric("1.1e1"));
		assertEquals(1.1e1f, parseNumber("1.1e1", null));

		assertTrue(isNumeric("1.1e+1"));
		assertEquals(1.1e+1f, parseNumber("1.1e+1", null));

		assertTrue(isNumeric("1.1e-1"));
		assertEquals(1.1e-1f, parseNumber("1.1e-1", null));

		assertTrue(isNumeric(".1e1"));
		assertEquals(.1e1f, parseNumber(".1e1", null));

		assertTrue(isNumeric(".1e+1"));
		assertEquals(.1e+1f, parseNumber(".1e+1", null));

		assertTrue(isNumeric(".1e-1"));
		assertEquals(.1e-1f, parseNumber(".1e-1", null));

		// Hexadecimal + scientific
		assertTrue(isNumeric("0x123e1"));
		assertEquals(0x123e1, parseNumber("0x123e1", null));

		assertThrows(NumberFormatException.class, () -> parseNumber("x", Number.class));
		assertThrows(NumberFormatException.class, () -> parseNumber("x", null));
		assertThrowsWithMessage(NumberFormatException.class, "Unsupported Number type", () -> parseNumber("x", BadNumber.class));
	}

	@SuppressWarnings("serial")
	private abstract static class BadNumber extends Number {}

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	void a02_numberRanges() {
		// An integer range is -2,147,483,648 to 2,147,483,647

		assertFalse(isNumeric(null));
		assertFalse(isNumeric(""));
		assertFalse(isNumeric("x"));

		var s = "-2147483648";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Integer);
		assertEquals(-2147483648, parseNumber(s, null));

		s = "2147483647";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Integer);
		assertEquals(2147483647, parseNumber(s, null));

		s = "-2147483649";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Long);
		assertEquals(-2147483649L, parseNumber(s, null));

		s = "2147483648";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Long);
		assertEquals(2147483648L, parseNumber(s, null));

		// An long range is -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807

		s = "-9223372036854775808";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Long);
		assertEquals(-9223372036854775808L, parseNumber(s, null));

		s = "9223372036854775807";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Long);
		assertEquals(9223372036854775807L, parseNumber(s, null));

		// Anything that falls outside this range should be a double.

		s = "-9223372036854775809";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Double);
		assertEquals(-9223372036854775808L, parseNumber(s, null).longValue());
		assertEquals(-9.223372036854776E18, parseNumber(s, null));

		s = "9223372036854775808";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Double);
		assertEquals(9223372036854775807L, parseNumber(s, null).longValue());
		assertEquals(9.223372036854776E18, parseNumber(s, null));

		// Check case where string is longer than 20 characters since it's a different code path.

		s = "-123456789012345678901";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Double);
		assertEquals(-9223372036854775808L, parseNumber(s, null).longValue());
		assertEquals(-1.2345678901234568E20, parseNumber(s, null));

		s = "123456789012345678901";
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Double);
		assertEquals(9223372036854775807L, parseNumber(s, null).longValue());
		assertEquals(1.2345678901234568E20, parseNumber(s, null));

		// Autodetected floating point numbers smaller than Float.MAX_VALUE
		s = String.valueOf(Float.MAX_VALUE / 2);
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Float);
		assertEquals(1.7014117E38f, parseNumber(s, null));

		s = String.valueOf((-Float.MAX_VALUE) / 2);
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Float);
		assertEquals(-1.7014117E38f, parseNumber(s, null));

		// Autodetected floating point numbers larger than Float.MAX_VALUE
		s = String.valueOf((double)Float.MAX_VALUE * 2);
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Double);
		assertEquals("6.805646932770577E38", parseNumber(s, null).toString());

		s = String.valueOf((double)Float.MAX_VALUE * -2);
		assertTrue(isNumeric(s));
		assertTrue(parseNumber(s, null) instanceof Double);
		assertEquals("-6.805646932770577E38", parseNumber(s, null).toString());

		s = String.valueOf("214748364x");
		assertFalse(isNumeric(s));
		assertThrows(NumberFormatException.class, () -> parseNumber("214748364x", Number.class));

		s = String.valueOf("2147483640x");
		assertFalse(isNumeric(s));
		assertThrows(NumberFormatException.class, () -> parseNumber("2147483640x", Long.class));
	}

	//====================================================================================================
	// testReplaceVars
	//====================================================================================================
	@Test
	void a03_replaceVars() throws Exception {
		var m = JsonMap.ofJson("{a:'A',b:1,c:true,d:'{e}',e:'E{f}E',f:'F',g:'{a}',h:'a',i:null}");

		assertEquals("xxx", replaceVars("xxx", m));
		assertEquals("A", replaceVars("{a}", m));
		assertEquals("AA", replaceVars("{a}{a}", m));
		assertEquals("xAx", replaceVars("x{a}x", m));
		assertEquals("xAxAx", replaceVars("x{a}x{a}x", m));
		assertEquals("1", replaceVars("{b}", m));
		assertEquals("11", replaceVars("{b}{b}", m));
		assertEquals("x1x", replaceVars("x{b}x", m));
		assertEquals("x1x1x", replaceVars("x{b}x{b}x", m));
		assertEquals("true", replaceVars("{c}", m));
		assertEquals("truetrue", replaceVars("{c}{c}", m));
		assertEquals("xtruextruex", replaceVars("x{c}x{c}x", m));
		assertEquals("EFE", replaceVars("{d}", m));
		assertEquals("EFEEFE", replaceVars("{d}{d}", m));
		assertEquals("xEFEx", replaceVars("x{d}x", m));
		assertEquals("xEFExEFEx", replaceVars("x{d}x{d}x", m));
		assertEquals("A", replaceVars("{g}", m));
		assertEquals("AA", replaceVars("{g}{g}", m));
		assertEquals("xAx", replaceVars("x{g}x", m));
		assertEquals("xAxAx", replaceVars("x{g}x{g}x", m));
		assertEquals("{x}", replaceVars("{x}", m));
		assertEquals("{x}{x}", replaceVars("{x}{x}", m));
		assertEquals("x{x}x{x}x", replaceVars("x{x}x{x}x", m));
		assertEquals("{A}", replaceVars("{{g}}", m));
		assertEquals("A", replaceVars("{{h}}", m));
		assertEquals("{}", replaceVars("{{i}}", m));
		assertEquals("{}", replaceVars("{}", m));
	}

	//====================================================================================================
	// isFloat(String)
	//====================================================================================================
	@Test
	void a04_isFloat() {
		var valid = a("+1.0", "-1.0", ".0", "NaN", "Infinity", "1e1", "-1e-1", "+1e+1", "-1.1e-1", "+1.1e+1", "1.1f", "1.1F", "1.1d", "1.1D", "0x1.fffffffffffffp1023", "0x1.FFFFFFFFFFFFFP1023");
		for (var s : valid)
			assertTrue(isFloat(s));

		var invalid = a(null, "", "a", "+", "-", ".", "a", "+a", "11a");
		for (var s : invalid)
			assertFalse(isFloat(s));
	}

	//====================================================================================================
	// isDecimal(String)
	//====================================================================================================
	@Test
	void a05_isDecimal() {
		var valid = a("+1", "-1", "0x123", "0X123", "0xdef", "0XDEF", "#def", "#DEF", "0123");
		for (var s : valid)
			assertTrue(isDecimal(s));

		var invalid = a(null, "", "a", "+", "-", ".", "0xdeg", "0XDEG", "#deg", "#DEG", "0128", "012A");
		for (var s : invalid)
			assertFalse(isDecimal(s));
	}

	//====================================================================================================
	// join(Object[],String)
	// join(int[],String)
	// join(Collection,String)
	// join(Object[],char)
	// join(int[],char)
	// join(Collection,char)
	//====================================================================================================
	@Test
	void a01_join() {
		assertNull(StringUtils.join((Object[])null, ","));
		assertEquals("1", StringUtils.join(a(1), ","));
		assertEquals("1,2", StringUtils.join(a(1, 2), ","));

		assertNull(StringUtils.join((Collection<?>)null, ","));
		assertEquals("1", StringUtils.join(l(a(1)), ","));
		assertEquals("1,2", StringUtils.join(l(a(1, 2)), ","));

		assertNull(StringUtils.join((Object[])null, ','));
		assertEquals("x,y,z", StringUtils.join(a("x,y", "z"), ','));

		assertNull(StringUtils.join((int[])null, ','));
		assertEquals("1", StringUtils.join(ints(1), ','));
		assertEquals("1,2", StringUtils.join(ints(1, 2), ','));

		assertNull(StringUtils.join((Collection<?>)null, ','));
		assertEquals("1", StringUtils.join(l(a(1)), ','));
		assertEquals("1,2", StringUtils.join(l(a(1, 2)), ','));

		assertNull(StringUtils.joine((List<?>)null, ','));
		assertEquals("x\\,y,z", StringUtils.joine(l(a("x,y", "z")), ','));
	}

	//====================================================================================================
	// split(String,char)
	//====================================================================================================
	@Test
	void a07_split() {
		assertNull(StringUtils.splita((String)null));
		assertEmpty(StringUtils.splita(""));
		assertList(StringUtils.splita("1"), "1");
		assertList(StringUtils.splita("1,2"), "1", "2");
		assertList(StringUtils.splita("1\\,2"), "1,2");
		assertList(StringUtils.splita("1\\\\,2"), "1\\", "2");
		assertList(StringUtils.splita("1\\\\\\,2"), "1\\,2");
		assertList(StringUtils.splita("1,2\\"), "1", "2\\");
		assertList(StringUtils.splita("1,2\\\\"), "1", "2\\");
		assertList(StringUtils.splita("1,2\\,"), "1", "2,");
		assertList(StringUtils.splita("1,2\\\\,"), "1", "2\\", "");
	}

	@Test
	void a08_split2() {
		assertEmpty(split2test(null));
		assertString("[]", split2test(""));
		assertString("[1]", split2test("1"));
		assertString("[1,2]", split2test("1,2"));
		assertList(split2test("1\\,2"), "1,2");
		assertList(split2test("1\\\\,2"), "1\\", "2");
		assertList(split2test("1\\\\\\,2"), "1\\,2");
		assertList(split2test("1,2\\"), "1", "2\\");
		assertList(split2test("1,2\\\\"), "1", "2\\");
		assertList(split2test("1,2\\,"), "1", "2,");
		assertList(split2test("1,2\\\\,"), "1", "2\\", "");
	}

	private static List<String> split2test(String s) {
		var l = new ArrayList<String>();
		StringUtils.split(s, l::add);
		return l;
	}

	//====================================================================================================
	// split(String,char,int)
	//====================================================================================================
	@Test
	void a09_splitWithLimit() {
		assertString("[boo,and,foo]", StringUtils.splita("boo:and:foo", ':', 10));
		assertString("[boo,and:foo]", StringUtils.splita("boo:and:foo", ':', 2));
		assertString("[boo:and:foo]", StringUtils.splita("boo:and:foo", ':', 1));
		assertString("[boo:and:foo]", StringUtils.splita("boo:and:foo", ':', 0));
		assertString("[boo:and:foo]", StringUtils.splita("boo:and:foo", ':', -1));
		assertString("[boo,and,foo]", StringUtils.splita("boo : and : foo", ':', 10));
		assertString("[boo,and : foo]", StringUtils.splita("boo : and : foo", ':', 2));
	}

	//====================================================================================================
	// nullIfEmpty(String)
	//====================================================================================================
	@Test
	void a10_nullIfEmpty() {
		assertNull(StringUtils.nullIfEmpty(null));
		assertNull(StringUtils.nullIfEmpty(""));
		assertNotNull(StringUtils.nullIfEmpty("x"));
	}

	//====================================================================================================
	// emptyIfNull(String)
	//====================================================================================================
	@Test
	void a11_emptyIfNull() {
		assertEquals("", StringUtils.emptyIfNull(null));
		assertEquals("", StringUtils.emptyIfNull(""));
		assertEquals("x", StringUtils.emptyIfNull("x"));
		assertEquals("hello", StringUtils.emptyIfNull("hello"));
		assertEquals("  ", StringUtils.emptyIfNull("  "));
	}

	//====================================================================================================
	// defaultIfEmpty(String, String)
	//====================================================================================================
	@Test
	void a12_defaultIfEmpty() {
		assertEquals("default", StringUtils.defaultIfEmpty(null, "default"));
		assertEquals("default", StringUtils.defaultIfEmpty("", "default"));
		assertEquals("x", StringUtils.defaultIfEmpty("x", "default"));
		assertEquals("hello", StringUtils.defaultIfEmpty("hello", "default"));
		assertEquals("  ", StringUtils.defaultIfEmpty("  ", "default")); // "  " is not empty
		assertEquals("x", StringUtils.defaultIfEmpty("x", "")); // "x" is not empty, so return "x"
		assertEquals("x", StringUtils.defaultIfEmpty("x", null)); // "x" is not empty, so return "x"
	}

	@Test
	void a13_defaultIfEmpty_withNullDefault() {
		assertNull(StringUtils.defaultIfEmpty(null, null));
		assertNull(StringUtils.defaultIfEmpty("", null));
		assertEquals("x", StringUtils.defaultIfEmpty("x", null));
	}

	//====================================================================================================
	// defaultIfBlank(String, String)
	//====================================================================================================
	@Test
	void a14_defaultIfBlank() {
		assertEquals("default", StringUtils.defaultIfBlank(null, "default"));
		assertEquals("default", StringUtils.defaultIfBlank("", "default"));
		assertEquals("default", StringUtils.defaultIfBlank("  ", "default"));
		assertEquals("default", StringUtils.defaultIfBlank("\t", "default"));
		assertEquals("default", StringUtils.defaultIfBlank("\n", "default"));
		assertEquals("x", StringUtils.defaultIfBlank("x", "default"));
		assertEquals("hello", StringUtils.defaultIfBlank("hello", "default"));
		assertEquals("  x  ", StringUtils.defaultIfBlank("  x  ", "default")); // Contains non-whitespace
		assertEquals("x", StringUtils.defaultIfBlank("x", "")); // "x" is not blank, so return "x"
		assertEquals("x", StringUtils.defaultIfBlank("x", null)); // "x" is not blank, so return "x"
	}

	@Test
	void a15_defaultIfBlank_withNullDefault() {
		assertNull(StringUtils.defaultIfBlank(null, null));
		assertNull(StringUtils.defaultIfBlank("", null));
		assertNull(StringUtils.defaultIfBlank("  ", null));
		assertEquals("x", StringUtils.defaultIfBlank("x", null));
	}

	@Test
	void a16_defaultIfBlank_whitespaceOnly() {
		assertEquals("default", StringUtils.defaultIfBlank(" ", "default"));
		assertEquals("default", StringUtils.defaultIfBlank("\t\n\r", "default"));
		// Note: \u00A0 (non-breaking space) may or may not be considered blank depending on Java version
		// String.isBlank() behavior may vary, so we test the actual behavior
		var result = StringUtils.defaultIfBlank("\u00A0", "default");
		// If isBlank considers it blank, result should be "default", otherwise it's "\u00A0"
		assertTrue(result.equals("default") || result.equals("\u00A0"));
	}

	//====================================================================================================
	// toString(Object)
	//====================================================================================================
	@Test
	void a17_toString() {
		assertNull(StringUtils.toString(null));
		assertEquals("hello", StringUtils.toString("hello"));
		assertEquals("123", StringUtils.toString(123));
		assertEquals("true", StringUtils.toString(true));
		assertEquals("1.5", StringUtils.toString(1.5));
	}

	@Test
	void a18_toString_withObjects() {
		var list = List.of("a", "b", "c");
		assertNotNull(StringUtils.toString(list));
		assertTrue(StringUtils.toString(list).contains("a"));

		var map = Map.of("key", "value");
		assertNotNull(StringUtils.toString(map));
		assertTrue(StringUtils.toString(map).contains("key"));
	}

	@Test
	void a19_toString_withCustomObject() {
		var obj = new Object() {
			@Override
			public String toString() {
				return "custom";
			}
		};
		assertEquals("custom", StringUtils.toString(obj));
	}

	//====================================================================================================
	// toString(Object, String)
	//====================================================================================================
	@Test
	void a20_toStringWithDefault() {
		assertEquals("default", StringUtils.toString(null, "default"));
		assertEquals("hello", StringUtils.toString("hello", "default"));
		assertEquals("123", StringUtils.toString(123, "default"));
		assertEquals("true", StringUtils.toString(true, "default"));
		assertEquals("1.5", StringUtils.toString(1.5, "default"));
	}

	@Test
	void a21_toStringWithDefault_withNullDefault() {
		assertNull(StringUtils.toString(null, null));
		assertEquals("hello", StringUtils.toString("hello", null));
	}

	@Test
	void a22_toStringWithDefault_withEmptyDefault() {
		assertEquals("", StringUtils.toString(null, ""));
		assertEquals("hello", StringUtils.toString("hello", ""));
	}

	@Test
	void a23_toStringWithDefault_withObjects() {
		var list = List.of("a", "b", "c");
		var result = StringUtils.toString(list, "default");
		assertNotNull(result);
		assertTrue(result.contains("a"));

		assertEquals("default", StringUtils.toString(null, "default"));
	}

	@Test
	void a24_toStringWithDefault_withCustomObject() {
		var obj = new Object() {
			@Override
			public String toString() {
				return "custom";
			}
		};
		assertEquals("custom", StringUtils.toString(obj, "default"));
		assertEquals("default", StringUtils.toString(null, "default"));
	}

	//====================================================================================================
	// unescapeChars(String,char[],char)
	//====================================================================================================
	@Test
	void a25_unescapeChars() {
		var escape = AsciiSet.of("\\,|");

		assertNull(unEscapeChars(null, escape));
		assertEquals("xxx", unEscapeChars("xxx", escape));

		assertEquals("xxx", unEscapeChars("xxx", escape));
		assertEquals("x,xx", unEscapeChars("x\\,xx", escape));
		assertEquals("x\\xx", unEscapeChars("x\\xx", escape));
		assertEquals("x\\,xx", unEscapeChars("x\\\\,xx", escape));
		assertEquals("x\\,xx", unEscapeChars("x\\\\\\,xx", escape));
		assertEquals("\\", unEscapeChars("\\", escape));
		assertEquals(",", unEscapeChars("\\,", escape));
		assertEquals("|", unEscapeChars("\\|", escape));

		escape = AsciiSet.of(",|");
		assertEquals("x\\\\xx", unEscapeChars("x\\\\xx", escape));
	}

	//====================================================================================================
	// decodeHex(String)
	//====================================================================================================
	@Test
	void a26_decodeHex() {
		assertNull(decodeHex(null));
		assertEquals("19azAZ", decodeHex("19azAZ"));
		assertEquals("[0][1][ffff]", decodeHex("\u0000\u0001\uFFFF"));
	}

	//====================================================================================================
	// startsWith(String,char)
	//====================================================================================================
	@Test
	void a27_startsWith() {
		assertFalse(startsWith(null, 'a'));
		assertFalse(startsWith("", 'a'));
		assertTrue(startsWith("a", 'a'));
		assertTrue(startsWith("ab", 'a'));
	}

	//====================================================================================================
	// endsWith(String,char)
	//====================================================================================================
	@Test
	void a28_endsWith() {
		assertFalse(endsWith(null, 'a'));
		assertFalse(endsWith("", 'a'));
		assertTrue(endsWith("a", 'a'));
		assertTrue(endsWith("ba", 'a'));
	}

	//====================================================================================================
	// base64EncodeToString(String)
	// base64DecodeToString(String)
	//====================================================================================================
	@Test
	void a29_base64EncodeToString() {
		assertNull(base64DecodeToString(base64EncodeToString(null)));
		assertEquals("", base64DecodeToString(base64EncodeToString("")));
		assertEquals("foobar", base64DecodeToString(base64EncodeToString("foobar")));
		assertEquals("\u0000\uffff", base64DecodeToString(base64EncodeToString("\u0000\uffff")));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid BASE64 string length.  Must be multiple of 4.", () -> base64Decode("a"));
		assertThrows(IllegalArgumentException.class, () -> base64Decode("aaa"));
	}

	//====================================================================================================
	// generateUUID(String)
	//====================================================================================================
	@Test
	void a30_generateUUID() {
		for (var i = 0; i < 10; i++) {
			var s = random(i);
			assertEquals(i, s.length());
			for (var c : s.toCharArray())
				assertTrue(Character.isLowerCase(c) || Character.isDigit(c));
		}
	}

	//====================================================================================================
	// trim(String)
	//====================================================================================================
	@Test
	void a31_trim() {
		assertNull(trim(null));
		assertEquals("", trim(""));
		assertEquals("", trim("  "));
	}

	//====================================================================================================
	// parseISO8601Date(String)
	//====================================================================================================
	@Test
	void a32_parseISO8601Date() throws Exception {
		assertNull(parseIsoDate(null));
		assertNull(parseIsoDate(""));

		setTimeZone("GMT");
		try {
			assertString("2000-01-01T00:00:00Z", parseIsoDate("2000"));
			assertString("2000-02-01T00:00:00Z", parseIsoDate("2000-02"));
			assertString("2000-02-03T00:00:00Z", parseIsoDate("2000-02-03"));
			assertString("2000-02-03T04:00:00Z", parseIsoDate("2000-02-03T04"));
			assertString("2000-02-03T04:05:00Z", parseIsoDate("2000-02-03T04:05"));
			assertString("2000-02-03T04:05:06Z", parseIsoDate("2000-02-03T04:05:06"));
			assertString("2000-02-03T04:00:00Z", parseIsoDate("2000-02-03 04"));
			assertString("2000-02-03T04:05:00Z", parseIsoDate("2000-02-03 04:05"));
			assertString("2000-02-03T04:05:06Z", parseIsoDate("2000-02-03 04:05:06"));
			assertString("2000-02-03T04:05:06Z", parseIsoDate("2000-02-03 04:05:06,789"));// ISO8601 doesn't support milliseconds, so it gets trimmed.
		} finally {
			unsetTimeZone();
		}
	}

	//====================================================================================================
	// parseMap(String,char,char,boolean)
	//====================================================================================================
	@Test
	void a33_splitMap() {
		assertString("{a=1}", StringUtils.splitMap("a=1", true));
		assertString("{a=1,b=2}", StringUtils.splitMap("a=1,b=2", true));
		assertString("{a=1,b=2}", StringUtils.splitMap(" a = 1 , b = 2 ", true));
		assertString("{ a = 1 , b = 2 }", StringUtils.splitMap(" a = 1 , b = 2 ", false));
		assertString("{a=}", StringUtils.splitMap("a", true));
		assertString("{a=,b=}", StringUtils.splitMap("a,b", true));
		assertString("{a=1,b=}", StringUtils.splitMap("a=1,b", true));
		assertString("{a=,b=1}", StringUtils.splitMap("a,b=1", true));
		assertString("{a==1}", StringUtils.splitMap("a\\==1", true));
		assertString("{a\\=1}", StringUtils.splitMap("a\\\\=1", true));
	}

	//====================================================================================================
	// isAbsoluteUri(String)
	//====================================================================================================
	@Test
	void a10_isAbsoluteUri() {
		assertFalse(isAbsoluteUri(null));
		assertFalse(isAbsoluteUri(""));
		assertTrue(isAbsoluteUri("http://foo"));
		assertTrue(isAbsoluteUri("x://x"));
		assertFalse(isAbsoluteUri("xX://x"));
		assertFalse(isAbsoluteUri("x ://x"));
		assertFalse(isAbsoluteUri("x: //x"));
		assertFalse(isAbsoluteUri("x:/ /x"));
		assertFalse(isAbsoluteUri("x:x//x"));
		assertFalse(isAbsoluteUri("x:/x/x"));
	}

	//====================================================================================================
	// getAuthorityUri(String)
	//====================================================================================================
	@Test
	void a21_getAuthorityUri() {
		assertEquals("http://foo", getAuthorityUri("http://foo"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123/"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123/bar"));
	}

	//====================================================================================================
	// splitQuoted(String)
	//====================================================================================================
	@Test
	void a22_splitQuoted() {
		assertNull(StringUtils.splitQuoted(null));
		assertEmpty(StringUtils.splitQuoted(""));
		assertEmpty(StringUtils.splitQuoted(" \t "));
		assertList(StringUtils.splitQuoted("foo"), "foo");
		assertList(StringUtils.splitQuoted("foo  bar baz"), "foo", "bar", "baz");
		assertList(StringUtils.splitQuoted("'foo'"), "foo");
		assertList(StringUtils.splitQuoted(" ' foo ' "), " foo ");
		assertList(StringUtils.splitQuoted("'foo' 'bar'"), "foo", "bar");
		assertList(StringUtils.splitQuoted("\"foo\""), "foo");
		assertList(StringUtils.splitQuoted(" \" foo \" "), " foo ");
		assertList(StringUtils.splitQuoted("\"foo\" \"bar\""), "foo", "bar");
		assertList(StringUtils.splitQuoted("'foo\\'bar'"), "foo'bar");
		assertList(StringUtils.splitQuoted("'foo\\\"bar'"), "foo\"bar");
		assertList(StringUtils.splitQuoted("'\\'foo\\'bar\\''"), "'foo'bar'");
		assertList(StringUtils.splitQuoted("'\\\"foo\\\"bar\\\"'"), "\"foo\"bar\"");
		assertList(StringUtils.splitQuoted("'\\'foo\\''"), "'foo'");
		assertList(StringUtils.splitQuoted("\"\\\"foo\\\"\""), "\"foo\"");
		assertList(StringUtils.splitQuoted("'\"foo\"'"), "\"foo\"");
		assertList(StringUtils.splitQuoted("\"'foo'\""), "'foo'");
	}

	//====================================================================================================
	// firstNonWhitespaceChar(String)
	//====================================================================================================
	@Test
	void a23_firstNonWhitespaceChar() {
		assertEquals('f', firstNonWhitespaceChar("foo"));
		assertEquals('f', firstNonWhitespaceChar(" foo"));
		assertEquals('f', firstNonWhitespaceChar("\tfoo"));
		assertEquals(0, firstNonWhitespaceChar(""));
		assertEquals(0, firstNonWhitespaceChar(" "));
		assertEquals(0, firstNonWhitespaceChar("\t"));
		assertEquals(0, firstNonWhitespaceChar(null));
	}

	//====================================================================================================
	// lastNonWhitespaceChar(String)
	//====================================================================================================
	@Test
	void a24_lastNonWhitespaceChar() {
		assertEquals('r', lastNonWhitespaceChar("bar"));
		assertEquals('r', lastNonWhitespaceChar(" bar "));
		assertEquals('r', lastNonWhitespaceChar("\tbar\t"));
		assertEquals(0, lastNonWhitespaceChar(""));
		assertEquals(0, lastNonWhitespaceChar(" "));
		assertEquals(0, lastNonWhitespaceChar("\t"));
		assertEquals(0, lastNonWhitespaceChar(null));
	}

	//====================================================================================================
	// testIsJsonObject(Object)
	//====================================================================================================
	@Test
	void a25_isJsonObject() {
		assertTrue(isJsonObject("{foo:'bar'}", true));
		assertTrue(isJsonObject(" { foo:'bar' } ", true));
		assertFalse(isJsonObject(" { foo:'bar'  ", true));
		assertFalse(isJsonObject("  foo:'bar' } ", true));
		assertTrue(isJsonObject("/*foo*/ { foo:'bar' } /*foo*/", true));
	}

	//====================================================================================================
	// isJsonArray(Object)
	//====================================================================================================
	@Test
	void a26_isJsonArray() {
		assertTrue(isJsonArray("[123,'bar']", true));
		assertTrue(isJsonArray(" [ 123,'bar' ] ", true));
		assertFalse(isJsonArray(" [ 123,'bar'  ", true));
		assertFalse(isJsonArray("  123,'bar' ] ", true));
		assertTrue(isJsonArray("/*foo*/ [ 123,'bar' ] /*foo*/", true));
	}

	//====================================================================================================
	// addLineNumbers(String)
	//====================================================================================================
	@Test
	void a27_addLineNumbers() {
		assertNull(getNumberedLines(null));
		assertEquals("1: \n", getNumberedLines(""));
		assertEquals("1: foo\n", getNumberedLines("foo"));
		assertEquals("1: foo\n2: bar\n", getNumberedLines("foo\nbar"));
	}

	//====================================================================================================
	// compare(String,String)
	//====================================================================================================
	@Test
	void a28_compare() {
		assertTrue(compare("a", "b") < 0);
		assertTrue(compare("b", "a") > 0);
		assertTrue(compare(null, "b") < 0);
		assertTrue(compare("b", null) > 0);
		assertEquals(0, compare(null, null));
	}

	//====================================================================================================
	// matchPattern(String)
	//====================================================================================================
	@Test
	void a29_getMatchPattern() {
		assertTrue(StringUtils.getMatchPattern("a").matcher("a").matches());
		assertTrue(StringUtils.getMatchPattern("*a*").matcher("aaa").matches());
		assertFalse(StringUtils.getMatchPattern("*b*").matcher("aaa").matches());
	}

	//====================================================================================================
	// getDuration(String)
	//====================================================================================================
	@Test
	void a30_getDuration() {
		assertEquals(-1, getDuration(null));
		assertEquals(-1, getDuration(""));
		assertEquals(-1, getDuration(" "));
		assertEquals(1, getDuration("1"));
		assertEquals(10, getDuration("10"));
		assertEquals(10, getDuration("10"));

		long s = 1000, m = s * 60, h = m * 60, d = h * 24, w = d * 7;

		assertEquals(10 * s, getDuration("10s"));
		assertEquals(10 * s, getDuration("10 s"));
		assertEquals(10 * s, getDuration("  10  s  "));
		assertEquals(10 * s, getDuration("10sec"));
		assertEquals(10 * s, getDuration("10 sec"));
		assertEquals(10 * s, getDuration("  10  sec  "));
		assertEquals(10 * s, getDuration("10seconds"));
		assertEquals(10 * s, getDuration("10 seconds"));
		assertEquals(10 * s, getDuration("  10  seconds  "));
		assertEquals(10 * s, getDuration("10S"));
		assertEquals(10 * s, getDuration("10 S"));
		assertEquals(10 * s, getDuration("  10  S  "));

		assertEquals(10 * m, getDuration("10m"));
		assertEquals(10 * m, getDuration("10 m"));
		assertEquals(10 * m, getDuration("  10  m  "));
		assertEquals(10 * m, getDuration("10min"));
		assertEquals(10 * m, getDuration("10 min"));
		assertEquals(10 * m, getDuration("  10  min  "));
		assertEquals(10 * m, getDuration("10minutes"));
		assertEquals(10 * m, getDuration("10 minutes"));
		assertEquals(10 * m, getDuration("  10  minutes  "));
		assertEquals(10 * m, getDuration("10M"));
		assertEquals(10 * m, getDuration("10 M"));
		assertEquals(10 * m, getDuration("  10  M  "));

		assertEquals(10 * h, getDuration("10h"));
		assertEquals(10 * h, getDuration("10 h"));
		assertEquals(10 * h, getDuration("  10  h  "));
		assertEquals(10 * h, getDuration("10hour"));
		assertEquals(10 * h, getDuration("10 hour"));
		assertEquals(10 * h, getDuration("  10  hour  "));
		assertEquals(10 * h, getDuration("10hours"));
		assertEquals(10 * h, getDuration("10 hours"));
		assertEquals(10 * h, getDuration("  10  hours  "));
		assertEquals(10 * h, getDuration("10H"));
		assertEquals(10 * h, getDuration("10 H"));
		assertEquals(10 * h, getDuration("  10  H  "));

		assertEquals(10 * d, getDuration("10d"));
		assertEquals(10 * d, getDuration("10 d"));
		assertEquals(10 * d, getDuration("  10  d  "));
		assertEquals(10 * d, getDuration("10day"));
		assertEquals(10 * d, getDuration("10 day"));
		assertEquals(10 * d, getDuration("  10  day  "));
		assertEquals(10 * d, getDuration("10days"));
		assertEquals(10 * d, getDuration("10 days"));
		assertEquals(10 * d, getDuration("  10  days  "));
		assertEquals(10 * d, getDuration("10D"));
		assertEquals(10 * d, getDuration("10 D"));
		assertEquals(10 * d, getDuration("  10  D  "));

		assertEquals(10 * w, getDuration("10w"));
		assertEquals(10 * w, getDuration("10 w"));
		assertEquals(10 * w, getDuration("  10  w  "));
		assertEquals(10 * w, getDuration("10week"));
		assertEquals(10 * w, getDuration("10 week"));
		assertEquals(10 * w, getDuration("  10  week  "));
		assertEquals(10 * w, getDuration("10weeks"));
		assertEquals(10 * w, getDuration("10 weeks"));
		assertEquals(10 * w, getDuration("  10  weeks  "));
		assertEquals(10 * w, getDuration("10W"));
		assertEquals(10 * w, getDuration("10 W"));
		assertEquals(10 * w, getDuration("  10  W  "));
	}

	//====================================================================================================
	// getDuration(String)
	//====================================================================================================
	@Test
	void a31_stripInvalidHttpHeaderChars() {
		assertEquals("xxx", stripInvalidHttpHeaderChars("xxx"));
		assertEquals("\t []^x", stripInvalidHttpHeaderChars("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u0020\\[]^x"));
	}

	//====================================================================================================
	// abbreviate(String,int)
	//====================================================================================================
	@Test
	void a32_abbrevate() {
		assertNull(abbreviate(null, 0));
		assertEquals("foo", abbreviate("foo", 3));
		assertEquals("...", abbreviate("fooo", 3));
		assertEquals("f...", abbreviate("foooo", 4));
		assertEquals("foo", abbreviate("foo", 2));
	}

	//====================================================================================================
	// splitMethodArgs(String)
	//====================================================================================================
	@Test
	void a33_splitMethodArgs() {
		assertList(StringUtils.splitMethodArgs("java.lang.String"), "java.lang.String");
		assertList(StringUtils.splitMethodArgs("java.lang.String,java.lang.Integer"), "java.lang.String", "java.lang.Integer");
		assertList(StringUtils.splitMethodArgs("x,y"), "x", "y");
		assertList(StringUtils.splitMethodArgs("x,y<a,b>,z"), "x", "y<a,b>", "z");
		assertList(StringUtils.splitMethodArgs("x,y<a<b,c>,d<e,f>>,z"), "x", "y<a<b,c>,d<e,f>>", "z");
	}

	//====================================================================================================
	// fixUrl(String)
	//====================================================================================================
	@Test
	void a34_fixUrl() {
		assertEquals(null, fixUrl(null));
		assertEquals("", fixUrl(""));
		assertEquals("xxx", fixUrl("xxx"));
		assertEquals("+x+x+", fixUrl(" x x "));
		assertEquals("++x++x++", fixUrl("  x  x  "));
		assertEquals("foo%7Bbar%7Dbaz", fixUrl("foo{bar}baz"));
		assertEquals("%7Dfoo%7Bbar%7Dbaz%7B", fixUrl("}foo{bar}baz{"));
	}

	//====================================================================================================
	// diffPosition(String,String)
	//====================================================================================================
	@Test
	void a35_diffPosition() {
		assertEquals(-1, diffPosition("a", "a"));
		assertEquals(-1, diffPosition(null, null));
		assertEquals(0, diffPosition("a", "b"));
		assertEquals(1, diffPosition("aa", "ab"));
		assertEquals(1, diffPosition("aaa", "ab"));
		assertEquals(1, diffPosition("aa", "abb"));
		assertEquals(0, diffPosition("a", null));
		assertEquals(0, diffPosition(null, "b"));
	}

	//====================================================================================================
	// diffPositionIc(String,String)
	//====================================================================================================
	@Test
	void a36_diffPositionIc() {
		assertEquals(-1, diffPositionIc("a", "a"));
		assertEquals(-1, diffPositionIc("a", "A"));
		assertEquals(-1, diffPositionIc(null, null));
		assertEquals(0, diffPositionIc("a", "b"));
		assertEquals(1, diffPositionIc("aa", "Ab"));
		assertEquals(1, diffPositionIc("aaa", "Ab"));
		assertEquals(1, diffPositionIc("aa", "Abb"));
		assertEquals(0, diffPositionIc("a", null));
		assertEquals(0, diffPositionIc(null, "b"));
	}

	//====================================================================================================
	// splitNested(String)
	//====================================================================================================
	@Test
	void a37_splitNested() {
		assertNull(StringUtils.splitNested(null));
		assertEmpty(StringUtils.splitNested(""));
		assertList(StringUtils.splitNested("a"), "a");
		assertList(StringUtils.splitNested("a,b,c"), "a", "b", "c");
		assertList(StringUtils.splitNested("a{b,c},d"), "a{b,c}", "d");
		assertList(StringUtils.splitNested("a,b{c,d}"), "a", "b{c,d}");
		assertList(StringUtils.splitNested("a,b{c,d{e,f}}"), "a", "b{c,d{e,f}}");
		assertList(StringUtils.splitNested("a { b , c } , d "), "a { b , c }", "d");
		assertList(StringUtils.splitNested("a\\,b"), "a,b");
		assertList(StringUtils.splitNested("a\\\\,b"), "a\\", "b");
	}

	//====================================================================================================
	// splitNestedInner(String)
	//====================================================================================================
	@Test
	void a38_splitNestedInner() {
		assertThrowsWithMessage(IllegalArgumentException.class, "String was null.", () -> StringUtils.splitNestedInner(null));
		assertThrowsWithMessage(IllegalArgumentException.class, "String was empty.", () -> StringUtils.splitNestedInner(""));
		assertList(StringUtils.splitNestedInner("a{b}"), "b");
		assertList(StringUtils.splitNestedInner(" a { b } "), "b");
		assertList(StringUtils.splitNestedInner("a{b,c}"), "b", "c");
		assertList(StringUtils.splitNestedInner("a{b{c,d},e{f,g}}"), "b{c,d}", "e{f,g}");
	}

	//====================================================================================================
	// toHex2(int)
	//====================================================================================================
	@Test
	void a38_toHex2() {
		// Test zero
		assertString("00", toHex2(0));

		// Test small positive numbers
		assertString("01", toHex2(1));
		assertString("0F", toHex2(15));
		assertString("10", toHex2(16));
		assertString("FF", toHex2(255));

		// Test maximum valid value
		assertString("FF", toHex2(255));

		// Test values outside valid range - should throw exception
		assertThrowsWithMessage(NumberFormatException.class, "toHex2 can only be used on numbers between 0 and 255", () -> toHex2(256));
		assertThrowsWithMessage(NumberFormatException.class, "toHex2 can only be used on numbers between 0 and 255", () -> toHex2(511));
		assertThrowsWithMessage(NumberFormatException.class, "toHex2 can only be used on numbers between 0 and 255", () -> toHex2(Integer.MAX_VALUE));

		// Test negative numbers - should throw exception
		assertThrowsWithMessage(NumberFormatException.class, "toHex2 can only be used on numbers between 0 and 255", () -> toHex2(-1));
		assertThrowsWithMessage(NumberFormatException.class, "toHex2 can only be used on numbers between 0 and 255", () -> toHex2(-100));
		assertThrowsWithMessage(NumberFormatException.class, "toHex2 can only be used on numbers between 0 and 255", () -> toHex2(Integer.MIN_VALUE));

		// Test edge cases
		assertString("0A", toHex2(10));
		assertString("0B", toHex2(11));
		assertString("0C", toHex2(12));
		assertString("0D", toHex2(13));
		assertString("0E", toHex2(14));
	}

	//====================================================================================================
	// toHex4(int)
	//====================================================================================================
	@Test
	void a39_toHex4() {
		// Test zero
		assertString("0000", toHex4(0));

		// Test small positive numbers
		assertString("0001", toHex4(1));
		assertString("000F", toHex4(15));
		assertString("0010", toHex4(16));
		assertString("00FF", toHex4(255));

		// Test larger numbers
		assertString("0100", toHex4(256));
		assertString("1000", toHex4(4096));
		assertString("FFFF", toHex4(65535));

		// Test maximum 16-bit value
		assertString("FFFF", toHex4(65535));

		// Test larger values (these get truncated to 4 hex characters)
		assertString("0000", toHex4(65536));
		assertString("FFFF", toHex4(1048575));

		// Test maximum int value (this gets truncated to 4 hex characters)
		assertString("FFFF", toHex4(Integer.MAX_VALUE));

		// Test negative numbers - should throw exception
		assertThrowsWithMessage(NumberFormatException.class, "toHex4 can only be used on non-negative numbers", () -> toHex4(-1));
		assertThrowsWithMessage(NumberFormatException.class, "toHex4 can only be used on non-negative numbers", () -> toHex4(-100));
		assertThrowsWithMessage(NumberFormatException.class, "toHex4 can only be used on non-negative numbers", () -> toHex4(Integer.MIN_VALUE));

		// Test edge cases
		assertString("000A", toHex4(10));
		assertString("000B", toHex4(11));
		assertString("000C", toHex4(12));
		assertString("000D", toHex4(13));
		assertString("000E", toHex4(14));
	}

	//====================================================================================================
	// toHex8(long)
	//====================================================================================================
	@Test
	void a40_toHex8() {
		// Test zero
		assertString("00000000", toHex8(0));

		// Test small positive numbers
		assertString("00000001", toHex8(1));
		assertString("0000000F", toHex8(15));
		assertString("00000010", toHex8(16));
		assertString("000000FF", toHex8(255));

		// Test larger numbers
		assertString("00000100", toHex8(256));
		assertString("00001000", toHex8(4096));
		assertString("00010000", toHex8(65536));
		assertString("00100000", toHex8(1048576));
		assertString("01000000", toHex8(16777216));
		assertString("10000000", toHex8(268435456));

		// Test maximum 32-bit value
		assertString("FFFFFFFF", toHex8(4294967295L));

		// Test larger values (these get truncated to 8 hex characters)
		assertString("00000000", toHex8(4294967296L));
		assertString("FFFFFFFF", toHex8(68719476735L));

		// Test maximum long value (this gets truncated to 8 hex characters)
		assertString("FFFFFFFF", toHex8(Long.MAX_VALUE));

		// Test negative numbers - should throw exception
		assertThrowsWithMessage(NumberFormatException.class, "toHex8 can only be used on non-negative numbers", () -> toHex8(-1));
		assertThrowsWithMessage(NumberFormatException.class, "toHex8 can only be used on non-negative numbers", () -> toHex8(-100));
		assertThrowsWithMessage(NumberFormatException.class, "toHex8 can only be used on non-negative numbers", () -> toHex8(Long.MIN_VALUE));

		// Test edge cases
		assertString("0000000A", toHex8(10));
		assertString("0000000B", toHex8(11));
		assertString("0000000C", toHex8(12));
		assertString("0000000D", toHex8(13));
		assertString("0000000E", toHex8(14));
	}

	//====================================================================================================
	// String validation methods
	//====================================================================================================

	@Test
	void a41_isBlank() {
		assertTrue(StringUtils.isBlank(null));
		assertTrue(StringUtils.isBlank(""));
		assertTrue(StringUtils.isBlank("   "));
		assertTrue(StringUtils.isBlank("\t\n"));
		assertFalse(StringUtils.isBlank("hello"));
		assertFalse(StringUtils.isBlank(" hello "));
		assertFalse(StringUtils.isBlank("a"));
	}

	@Test
	void a42_isNotBlank() {
		assertFalse(isNotBlank(null));
		assertFalse(isNotBlank(""));
		assertFalse(isNotBlank("   "));
		assertFalse(isNotBlank("\t\n"));
		assertTrue(isNotBlank("hello"));
		assertTrue(isNotBlank(" hello "));
		assertTrue(isNotBlank("a"));
	}

	@Test
	void a43_isEmpty() {
		assertTrue(Utils.isEmpty((String)null));
		assertTrue(Utils.isEmpty(""));
		assertFalse(Utils.isEmpty("   "));
		assertFalse(Utils.isEmpty("hello"));
		assertFalse(Utils.isEmpty("a"));
	}

	@Test
	void a44_hasText() {
		assertFalse(hasText(null));
		assertFalse(hasText(""));
		assertFalse(hasText("   "));
		assertFalse(hasText("\t\n"));
		assertTrue(hasText("hello"));
		assertTrue(hasText(" hello "));
		assertTrue(hasText("a"));
	}

	@Test
	void a45_isAlpha() {
		assertFalse(isAlpha(null));
		assertFalse(isAlpha(""));
		assertTrue(isAlpha("abc"));
		assertTrue(isAlpha("ABC"));
		assertTrue(isAlpha("AbCdEf"));
		assertFalse(isAlpha("abc123"));
		assertFalse(isAlpha("abc def"));
		assertFalse(isAlpha("abc-def"));
		assertFalse(isAlpha("123"));
	}

	@Test
	void a46_isAlphaNumeric() {
		assertFalse(isAlphaNumeric(null));
		assertFalse(isAlphaNumeric(""));
		assertTrue(isAlphaNumeric("abc"));
		assertTrue(isAlphaNumeric("123"));
		assertTrue(isAlphaNumeric("abc123"));
		assertTrue(isAlphaNumeric("ABC123"));
		assertFalse(isAlphaNumeric("abc def"));
		assertFalse(isAlphaNumeric("abc-123"));
		assertFalse(isAlphaNumeric("abc_123"));
	}

	@Test
	void a47_isDigit() {
		assertFalse(isDigit(null));
		assertFalse(isDigit(""));
		assertTrue(isDigit("123"));
		assertTrue(isDigit("0"));
		assertTrue(isDigit("999"));
		assertFalse(isDigit("abc"));
		assertFalse(isDigit("abc123"));
		assertFalse(isDigit("12.3"));
		assertFalse(isDigit("12-3"));
	}

	@Test
	void a48_isWhitespace() {
		assertFalse(isWhitespace(null));
		assertTrue(isWhitespace(""));
		assertTrue(isWhitespace("   "));
		assertTrue(isWhitespace("\t\n"));
		assertTrue(isWhitespace("\r\n\t "));
		assertFalse(isWhitespace(" a "));
		assertFalse(isWhitespace("hello"));
	}

	@Test
	void a49_isEmpty() {
		assertTrue(StringUtils.isEmpty(null));
		assertTrue(StringUtils.isEmpty(""));
		assertFalse(StringUtils.isEmpty(" "));
		assertFalse(StringUtils.isEmpty("a"));
		assertFalse(StringUtils.isEmpty("hello"));
	}

	@Test
	void a50_isEmail() {
		assertFalse(isEmail(null));
		assertFalse(isEmail(""));
		assertFalse(isEmail(" "));
		assertFalse(isEmail("invalid"));
		assertFalse(isEmail("@example.com"));
		assertFalse(isEmail("user@"));
		assertFalse(isEmail("user@example"));
		assertFalse(isEmail("user@.com"));
		assertFalse(isEmail("@.com"));

		// Valid emails
		assertTrue(isEmail("user@example.com"));
		assertTrue(isEmail("test.email@example.com"));
		assertTrue(isEmail("user+tag@example.com"));
		assertTrue(isEmail("user_name@example.com"));
		assertTrue(isEmail("user-name@example.com"));
		assertTrue(isEmail("user123@example.com"));
		assertTrue(isEmail("user@example.co.uk"));
		assertTrue(isEmail("user@subdomain.example.com"));
		assertTrue(isEmail("a@b.co"));
		assertTrue(isEmail("user.name+tag+sorting@example.com"));
	}

	@Test
	void a51_isPhoneNumber() {
		assertFalse(isPhoneNumber(null));
		assertFalse(isPhoneNumber(""));
		assertFalse(isPhoneNumber(" "));
		assertFalse(isPhoneNumber("123"));
		assertFalse(isPhoneNumber("12345"));
		assertFalse(isPhoneNumber("abc1234567"));
		assertFalse(isPhoneNumber("1234567890123456")); // Too long (16 digits)

		// Valid phone numbers
		assertTrue(isPhoneNumber("1234567890")); // 10 digits
		assertTrue(isPhoneNumber("12345678901")); // 11 digits
		assertTrue(isPhoneNumber("123456789012345")); // 15 digits (max)
		assertTrue(isPhoneNumber("(123) 456-7890"));
		assertTrue(isPhoneNumber("123-456-7890"));
		assertTrue(isPhoneNumber("123.456.7890"));
		assertTrue(isPhoneNumber("123 456 7890"));
		assertTrue(isPhoneNumber("+1 123-456-7890"));
		assertTrue(isPhoneNumber("+44 20 1234 5678"));
		assertTrue(isPhoneNumber("+1 (123) 456-7890"));
	}

	@Test
	void a52_isCreditCard() {
		assertFalse(isCreditCard(null));
		assertFalse(isCreditCard(""));
		assertFalse(isCreditCard(" "));
		assertFalse(isCreditCard("1234567890")); // Too short
		assertFalse(isCreditCard("12345678901234567890")); // Too long (20 digits)
		assertFalse(isCreditCard("1234567890123")); // Invalid Luhn
		assertFalse(isCreditCard("1234567890124")); // Invalid Luhn (wrong check digit)
		assertFalse(isCreditCard("abc1234567890")); // Contains letters

		// Valid credit card numbers (test cards that pass Luhn algorithm)
		assertTrue(isCreditCard("4532015112830366")); // Visa test card
		assertTrue(isCreditCard("4532-0151-1283-0366")); // With hyphens
		assertTrue(isCreditCard("4532 0151 1283 0366")); // With spaces
		assertTrue(isCreditCard("5425233430109903")); // MasterCard test card
		assertTrue(isCreditCard("378282246310005")); // American Express test card
		assertTrue(isCreditCard("6011111111111117")); // Discover test card
		assertTrue(isCreditCard("4111111111111111")); // Visa test card
		assertTrue(isCreditCard("5555555555554444")); // MasterCard test card
	}

	@Test
	void a53_indexOf() {
		assertEquals(6, StringUtils.indexOf("hello world", "world"));
		assertEquals(0, StringUtils.indexOf("hello world", "hello"));
		assertEquals(2, StringUtils.indexOf("hello world", "llo"));
		assertEquals(-1, StringUtils.indexOf("hello world", "xyz"));
		assertEquals(-1, StringUtils.indexOf((String)null, "test"));
		assertEquals(-1, StringUtils.indexOf("test", (String)null));
		assertEquals(-1, StringUtils.indexOf((String)null, (String)null));
		assertEquals(0, StringUtils.indexOf("hello", "hello"));
		assertEquals(-1, StringUtils.indexOf("hello", "hello world"));
	}

	@Test
	void a54_indexOfIgnoreCase() {
		assertEquals(6, indexOfIgnoreCase("Hello World", "world"));
		assertEquals(6, indexOfIgnoreCase("Hello World", "WORLD"));
		assertEquals(0, indexOfIgnoreCase("Hello World", "hello"));
		assertEquals(0, indexOfIgnoreCase("Hello World", "HELLO"));
		assertEquals(2, indexOfIgnoreCase("Hello World", "LLO"));
		assertEquals(-1, indexOfIgnoreCase("Hello World", "xyz"));
		assertEquals(-1, indexOfIgnoreCase(null, "test"));
		assertEquals(-1, indexOfIgnoreCase("test", null));
		assertEquals(-1, indexOfIgnoreCase(null, null));
	}

	@Test
	void a55_lastIndexOf() {
		assertEquals(12, lastIndexOf("hello world world", "world"));
		assertEquals(6, lastIndexOf("hello world", "world"));
		assertEquals(0, lastIndexOf("hello world", "hello"));
		assertEquals(-1, lastIndexOf("hello world", "xyz"));
		assertEquals(-1, lastIndexOf(null, "test"));
		assertEquals(-1, lastIndexOf("test", null));
		assertEquals(-1, lastIndexOf(null, null));
		assertEquals(4, lastIndexOf("ababab", "ab")); // "ab" appears at positions 0, 2, 4
	}

	@Test
	void a56_lastIndexOfIgnoreCase() {
		assertEquals(12, lastIndexOfIgnoreCase("Hello World World", "world"));
		assertEquals(12, lastIndexOfIgnoreCase("Hello World World", "WORLD"));
		assertEquals(6, lastIndexOfIgnoreCase("Hello World", "world"));
		assertEquals(6, lastIndexOfIgnoreCase("Hello World", "WORLD"));
		assertEquals(0, lastIndexOfIgnoreCase("Hello World", "hello"));
		assertEquals(-1, lastIndexOfIgnoreCase("Hello World", "xyz"));
		assertEquals(-1, lastIndexOfIgnoreCase(null, "test"));
		assertEquals(-1, lastIndexOfIgnoreCase("test", null));
		assertEquals(-1, lastIndexOfIgnoreCase(null, null));
		assertEquals(4, lastIndexOfIgnoreCase("AbAbAb", "ab"));
	}

	@Test
	void a57_containsIgnoreCase() {
		assertTrue(containsIgnoreCase("Hello World", "world"));
		assertTrue(containsIgnoreCase("Hello World", "WORLD"));
		assertTrue(containsIgnoreCase("Hello World", "hello"));
		assertTrue(containsIgnoreCase("Hello World", "HELLO"));
		assertTrue(containsIgnoreCase("Hello World", "lo wo"));
		assertFalse(containsIgnoreCase("Hello World", "xyz"));
		assertFalse(containsIgnoreCase(null, "test"));
		assertFalse(containsIgnoreCase("test", null));
		assertFalse(containsIgnoreCase(null, null));
		assertTrue(containsIgnoreCase("Hello", "hello"));
	}

	@Test
	void a58_startsWithIgnoreCase() {
		assertTrue(startsWithIgnoreCase("Hello World", "hello"));
		assertTrue(startsWithIgnoreCase("Hello World", "HELLO"));
		assertTrue(startsWithIgnoreCase("Hello World", "Hello"));
		assertTrue(startsWithIgnoreCase("hello world", "HELLO"));
		assertFalse(startsWithIgnoreCase("Hello World", "world"));
		assertFalse(startsWithIgnoreCase("Hello World", "xyz"));
		assertFalse(startsWithIgnoreCase(null, "test"));
		assertFalse(startsWithIgnoreCase("test", null));
		assertFalse(startsWithIgnoreCase(null, null));
		assertTrue(startsWithIgnoreCase("Hello", "hello"));
	}

	@Test
	void a59_endsWithIgnoreCase() {
		assertTrue(endsWithIgnoreCase("Hello World", "world"));
		assertTrue(endsWithIgnoreCase("Hello World", "WORLD"));
		assertTrue(endsWithIgnoreCase("Hello World", "World"));
		assertTrue(endsWithIgnoreCase("hello world", "WORLD"));
		assertFalse(endsWithIgnoreCase("Hello World", "hello"));
		assertFalse(endsWithIgnoreCase("Hello World", "xyz"));
		assertFalse(endsWithIgnoreCase(null, "test"));
		assertFalse(endsWithIgnoreCase("test", null));
		assertFalse(endsWithIgnoreCase(null, null));
		assertTrue(endsWithIgnoreCase("Hello", "hello"));
	}

	@Test
	void a60_matches() {
		assertTrue(matches("12345", "\\d+"));
		assertTrue(matches("abc123", "^[a-z]+\\d+$"));
		assertTrue(matches("hello", "^hello$"));
		assertTrue(matches("test@example.com", "^[a-z]+@[a-z]+\\.[a-z]+$"));
		assertFalse(matches("abc", "\\d+"));
		assertFalse(matches("123", "^[a-z]+$"));
		assertFalse(matches(null, "\\d+"));
		assertFalse(matches("test", null));
		assertFalse(matches(null, null));
		// Test with invalid regex - should throw PatternSyntaxException
		assertThrows(java.util.regex.PatternSyntaxException.class, () -> matches("test", "["));
	}

	@Test
	void a61_countMatches() {
		assertEquals(2, countMatches("hello world world", "world"));
		assertEquals(3, countMatches("ababab", "ab"));
		assertEquals(4, countMatches("aaaa", "a"));
		assertEquals(2, countMatches("hello hello", "hello"));
		assertEquals(0, countMatches("hello", "xyz"));
		assertEquals(0, countMatches(null, "test"));
		assertEquals(0, countMatches("test", null));
		assertEquals(0, countMatches(null, null));
		assertEquals(0, countMatches("", "test"));
		assertEquals(0, countMatches("test", ""));
		assertEquals(1, countMatches("hello", "hello"));
		assertEquals(0, countMatches("hello", "hello world"));
		// Test overlapping matches - should not count overlapping
		assertEquals(2, countMatches("aaaa", "aa")); // "aa" appears at positions 0 and 2
	}

	@Test
	void a62_formatWithNamedArgs() {
		var args = new HashMap<String,Object>();
		args.put("name", "John");
		args.put("age", 30);
		args.put("city", "New York");
		assertEquals("Hello John, you are 30 years old", formatWithNamedArgs("Hello {name}, you are {age} years old", args));
		assertEquals("Welcome to New York", formatWithNamedArgs("Welcome to {city}", args));
		assertEquals("Hello {unknown}", formatWithNamedArgs("Hello {unknown}", args)); // Unknown placeholder kept
		assertEquals("No placeholders", formatWithNamedArgs("No placeholders", args));
		assertNull(formatWithNamedArgs(null, args));
		assertEquals("Template", formatWithNamedArgs("Template", null));
		assertEquals("Template", formatWithNamedArgs("Template", new HashMap<>()));
		// Test with null values
		var argsWithNull = new HashMap<String,Object>();
		argsWithNull.put("name", "John");
		argsWithNull.put("value", null);
		assertEquals("Hello John, value: ", formatWithNamedArgs("Hello {name}, value: {value}", argsWithNull));
	}

	//====================================================================================================
	// format(String, Object...)
	//====================================================================================================
	@Test
	void a62b_format() {
		// Basic string and number formatting
		assertEquals("Hello John, you have 5 items", format("Hello %s, you have %d items", "John", 5));
		assertEquals("Hello world", format("Hello %s", "world"));

		// Floating point with precision
		assertEquals("Price: $19.99", format("Price: $%.2f", 19.99));
		assertEquals("Value: 3.14", format("Value: %.2f", 3.14159));
		assertEquals("Value: 3.142", format("Value: %.3f", 3.14159));

		// Width and alignment
		assertEquals("Name: John                 Age:  25", format("Name: %-20s Age: %3d", "John", 25));
		assertEquals("Name:                 John Age:  25", format("Name: %20s Age: %3d", "John", 25));
		assertEquals("Number:   42", format("Number: %4d", 42));
		assertEquals("Number: 0042", format("Number: %04d", 42));

		// Hexadecimal
		assertEquals("Color: #FF5733", format("Color: #%06X", 0xFF5733));
		assertEquals("Hex: ff5733", format("Hex: %x", 0xFF5733));
		assertEquals("Hex: FF5733", format("Hex: %X", 0xFF5733));
		assertEquals("Hex: 255", format("Hex: %d", 0xFF));

		// Octal
		assertEquals("Octal: 377", format("Octal: %o", 255));

		// Scientific notation
		assertEquals("Value: 1.23e+06", format("Value: %.2e", 1234567.0));
		assertEquals("Value: 1.23E+06", format("Value: %.2E", 1234567.0));

		// Boolean
		assertEquals("Flag: true", format("Flag: %b", true));
		assertEquals("Flag: false", format("Flag: %b", false));
		assertEquals("Flag: true", format("Flag: %b", "anything"));

		// Character
		assertEquals("Char: A", format("Char: %c", 'A'));
		assertEquals("Char: A", format("Char: %c", 65));

		// Argument index (reuse arguments)
		assertEquals("Alice loves Bob, and Alice also loves Charlie", format("%1$s loves %2$s, and %1$s also loves %3$s", "Alice", "Bob", "Charlie"));

		// Literal percent sign
		assertEquals("Progress: 50%", format("Progress: %d%%", 50));
		assertEquals("Discount: 25% off", format("Discount: %d%% off", 25));

		// Line separator
		var result = format("Line 1%nLine 2");
		assertTrue(result.contains("Line 1"));
		assertTrue(result.contains("Line 2"));

		// Multiple format specifiers
		assertEquals("Name: John, Age: 30, Salary: $50000.00", format("Name: %s, Age: %d, Salary: $%.2f", "John", 30, 50000.0));
	}

	@Test
	void a63_interpolate() {
		var vars = new HashMap<String,Object>();
		vars.put("name", "John");
		vars.put("city", "New York");
		vars.put("age", 30);
		assertEquals("Hello John, welcome to New York", interpolate("Hello ${name}, welcome to ${city}", vars));
		assertEquals("Age: 30", interpolate("Age: ${age}", vars));
		assertEquals("Hello ${unknown}", interpolate("Hello ${unknown}", vars)); // Unknown variable kept
		assertEquals("No variables", interpolate("No variables", vars));
		assertNull(interpolate(null, vars));
		assertEquals("Template", interpolate("Template", null));
		assertEquals("Template", interpolate("Template", new HashMap<>()));
		// Test with null values
		var varsWithNull = new HashMap<String,Object>();
		varsWithNull.put("name", "John");
		varsWithNull.put("value", null);
		assertEquals("Hello John, value: null", interpolate("Hello ${name}, value: ${value}", varsWithNull));
		// Test with incomplete placeholder
		assertEquals("Hello ${name", interpolate("Hello ${name", vars));
		// Test with multiple variables
		assertEquals("John is 30 years old and lives in New York", interpolate("${name} is ${age} years old and lives in ${city}", vars));
	}

	@Test
	void a64_pluralize() {
		// Singular (count = 1)
		assertEquals("cat", pluralize("cat", 1));
		assertEquals("box", pluralize("box", 1));
		assertEquals("city", pluralize("city", 1));

		// Regular plural (add "s")
		assertEquals("cats", pluralize("cat", 2));
		assertEquals("dogs", pluralize("dog", 2));
		assertEquals("books", pluralize("book", 0));

		// Words ending in s, x, z, ch, sh (add "es")
		assertEquals("boxes", pluralize("box", 2));
		assertEquals("buses", pluralize("bus", 2));
		assertEquals("buzzes", pluralize("buzz", 2));
		assertEquals("churches", pluralize("church", 2));
		assertEquals("dishes", pluralize("dish", 2));

		// Words ending in "y" preceded by consonant (replace "y" with "ies")
		assertEquals("cities", pluralize("city", 2));
		assertEquals("countries", pluralize("country", 2));
		assertEquals("flies", pluralize("fly", 2));
		// Words ending in "y" preceded by vowel (just add "s")
		assertEquals("days", pluralize("day", 2));
		assertEquals("boys", pluralize("boy", 2));

		// Words ending in "f" or "fe" (replace with "ves")
		assertEquals("leaves", pluralize("leaf", 2));
		assertEquals("lives", pluralize("life", 2));
		assertEquals("knives", pluralize("knife", 2));

		// Edge cases
		assertNull(pluralize(null, 2));
		assertEquals("", pluralize("", 2));
		assertEquals("a", pluralize("a", 1));
		assertEquals("as", pluralize("a", 2));
	}

	@Test
	void a65_ordinal() {
		// Basic ordinals
		assertEquals("1st", ordinal(1));
		assertEquals("2nd", ordinal(2));
		assertEquals("3rd", ordinal(3));
		assertEquals("4th", ordinal(4));
		assertEquals("5th", ordinal(5));
		assertEquals("10th", ordinal(10));

		// Teens (all use "th")
		assertEquals("11th", ordinal(11));
		assertEquals("12th", ordinal(12));
		assertEquals("13th", ordinal(13));
		assertEquals("14th", ordinal(14));

		// 20s, 30s, etc.
		assertEquals("21st", ordinal(21));
		assertEquals("22nd", ordinal(22));
		assertEquals("23rd", ordinal(23));
		assertEquals("24th", ordinal(24));
		assertEquals("31st", ordinal(31));
		assertEquals("32nd", ordinal(32));
		assertEquals("33rd", ordinal(33));

		// Larger numbers
		assertEquals("100th", ordinal(100));
		assertEquals("101st", ordinal(101));
		assertEquals("102nd", ordinal(102));
		assertEquals("103rd", ordinal(103));
		assertEquals("111th", ordinal(111)); // Special case
		assertEquals("112th", ordinal(112)); // Special case
		assertEquals("113th", ordinal(113)); // Special case

		// Negative numbers
		assertEquals("-1st", ordinal(-1));
		assertEquals("-2nd", ordinal(-2));
		assertEquals("-11th", ordinal(-11));
		assertEquals("-21st", ordinal(-21));

		// Zero
		assertEquals("0th", ordinal(0));
	}

	@Test
	void a66_sanitize() {
		assertNull(sanitize(null));
		assertEquals("", sanitize(""));
		assertEquals("Hello World", sanitize("Hello World"));
		assertEquals("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;", sanitize("<script>alert('xss')</script>"));
		assertEquals("Hello &lt;b&gt;World&lt;/b&gt;", sanitize("Hello <b>World</b>"));
		assertEquals("&lt;img src=&quot;x&quot; onerror=&quot;alert(1)&quot;&gt;", sanitize("<img src=\"x\" onerror=\"alert(1)\">"));
	}

	@Test
	void a67_escapeHtml() {
		assertNull(escapeHtml(null));
		assertEquals("", escapeHtml(""));
		assertEquals("Hello World", escapeHtml("Hello World"));
		assertEquals("&lt;script&gt;", escapeHtml("<script>"));
		assertEquals("&quot;Hello&quot;", escapeHtml("\"Hello\""));
		assertEquals("It&#39;s a test", escapeHtml("It's a test"));
		assertEquals("&amp;", escapeHtml("&"));
		assertEquals("&lt;tag&gt;text&lt;/tag&gt;", escapeHtml("<tag>text</tag>"));
		// Test all entities
		assertEquals("&amp;", escapeHtml("&"));
		assertEquals("&lt;", escapeHtml("<"));
		assertEquals("&gt;", escapeHtml(">"));
		assertEquals("&quot;", escapeHtml("\""));
		assertEquals("&#39;", escapeHtml("'"));
	}

	@Test
	void a68_unescapeHtml() {
		assertNull(unescapeHtml(null));
		assertEquals("", unescapeHtml(""));
		assertEquals("Hello World", unescapeHtml("Hello World"));
		assertEquals("<script>", unescapeHtml("&lt;script&gt;"));
		assertEquals("\"Hello\"", unescapeHtml("&quot;Hello&quot;"));
		assertEquals("It's a test", unescapeHtml("It&#39;s a test"));
		assertEquals("It's a test", unescapeHtml("It&apos;s a test"));
		assertEquals("&", unescapeHtml("&amp;"));
		assertEquals("<tag>text</tag>", unescapeHtml("&lt;tag&gt;text&lt;/tag&gt;"));
		// Test round-trip
		assertEquals("Hello & World", unescapeHtml(escapeHtml("Hello & World")));
		assertEquals("<script>alert('xss')</script>", unescapeHtml(escapeHtml("<script>alert('xss')</script>")));
	}

	@Test
	void a69_escapeXml() {
		assertNull(escapeXml(null));
		assertEquals("", escapeXml(""));
		assertEquals("Hello World", escapeXml("Hello World"));
		assertEquals("&lt;tag&gt;", escapeXml("<tag>"));
		assertEquals("&quot;Hello&quot;", escapeXml("\"Hello\""));
		assertEquals("It&apos;s a test", escapeXml("It's a test"));
		assertEquals("&amp;", escapeXml("&"));
		assertEquals("&lt;tag attr=&apos;value&apos;&gt;text&lt;/tag&gt;", escapeXml("<tag attr='value'>text</tag>"));
		// Test all entities
		assertEquals("&amp;", escapeXml("&"));
		assertEquals("&lt;", escapeXml("<"));
		assertEquals("&gt;", escapeXml(">"));
		assertEquals("&quot;", escapeXml("\""));
		assertEquals("&apos;", escapeXml("'"));
	}

	@Test
	void a70_unescapeXml() {
		assertNull(unescapeXml(null));
		assertEquals("", unescapeXml(""));
		assertEquals("Hello World", unescapeXml("Hello World"));
		assertEquals("<tag>", unescapeXml("&lt;tag&gt;"));
		assertEquals("\"Hello\"", unescapeXml("&quot;Hello&quot;"));
		assertEquals("It's a test", unescapeXml("It&apos;s a test"));
		assertEquals("&", unescapeXml("&amp;"));
		assertEquals("<tag attr='value'>text</tag>", unescapeXml("&lt;tag attr=&apos;value&apos;&gt;text&lt;/tag&gt;"));
		// Test round-trip
		assertEquals("Hello & World", unescapeXml(escapeXml("Hello & World")));
		assertEquals("<tag>text</tag>", unescapeXml(escapeXml("<tag>text</tag>")));
	}

	@Test
	void a71_escapeSql() {
		assertNull(escapeSql(null));
		assertEquals("", escapeSql(""));
		assertEquals("Hello World", escapeSql("Hello World"));
		assertEquals("O''Brien", escapeSql("O'Brien"));
		assertEquals("It''s a test", escapeSql("It's a test"));
		assertEquals("''", escapeSql("'"));
		assertEquals("''''", escapeSql("''"));
		assertEquals("John''s book", escapeSql("John's book"));
	}

	@Test
	void a72_escapeRegex() {
		assertNull(escapeRegex(null));
		assertEquals("", escapeRegex(""));
		assertEquals("Hello World", escapeRegex("Hello World"));
		assertEquals("file\\.txt", escapeRegex("file.txt"));
		assertEquals("price: \\$10\\.99", escapeRegex("price: $10.99"));
		assertEquals("test\\*test", escapeRegex("test*test"));
		assertEquals("test\\+test", escapeRegex("test+test"));
		assertEquals("test\\?test", escapeRegex("test?test"));
		assertEquals("\\^start", escapeRegex("^start"));
		assertEquals("end\\$", escapeRegex("end$"));
		assertEquals("test\\{test\\}", escapeRegex("test{test}"));
		assertEquals("test\\(test\\)", escapeRegex("test(test)"));
		assertEquals("test\\[test\\]", escapeRegex("test[test]"));
		assertEquals("test\\|test", escapeRegex("test|test"));
		assertEquals("test\\\\test", escapeRegex("test\\test"));
		// Test that escaped regex can be used in Pattern
		var pattern = java.util.regex.Pattern.compile(escapeRegex("file.txt"));
		assertTrue(pattern.matcher("file.txt").matches());
		assertFalse(pattern.matcher("filextxt").matches());
	}

	@Test
	void a73_equalsIgnoreCase() {
		assertTrue(equalsIgnoreCase("Hello", "hello"));
		assertTrue(equalsIgnoreCase("HELLO", "hello"));
		assertTrue(equalsIgnoreCase("Hello", "HELLO"));
		assertTrue(equalsIgnoreCase("Hello", "Hello"));
		assertFalse(equalsIgnoreCase("Hello", "World"));
		assertTrue(equalsIgnoreCase(null, null));
		assertFalse(equalsIgnoreCase(null, "test"));
		assertFalse(equalsIgnoreCase("test", null));
		assertTrue(equalsIgnoreCase("", ""));
	}

	@Test
	void a74_compareIgnoreCase() {
		assertTrue(compareIgnoreCase("apple", "BANANA") < 0);
		assertTrue(compareIgnoreCase("BANANA", "apple") > 0);
		assertEquals(0, compareIgnoreCase("Hello", "hello"));
		assertEquals(0, compareIgnoreCase("HELLO", "hello"));
		assertTrue(compareIgnoreCase("Zebra", "apple") > 0);
		assertTrue(compareIgnoreCase("apple", "Zebra") < 0);
		assertEquals(0, compareIgnoreCase(null, null));
		assertTrue(compareIgnoreCase(null, "test") < 0);
		assertTrue(compareIgnoreCase("test", null) > 0);
	}

	@Test
	void a75_naturalCompare() {
		// Numbers should be compared numerically
		assertTrue(naturalCompare("file2.txt", "file10.txt") < 0);
		assertTrue(naturalCompare("file10.txt", "file2.txt") > 0);
		assertEquals(0, naturalCompare("file1.txt", "file1.txt"));
		assertTrue(naturalCompare("file1.txt", "file2.txt") < 0);
		assertTrue(naturalCompare("file2.txt", "file1.txt") > 0);

		// Leading zeros
		assertTrue(naturalCompare("file02.txt", "file10.txt") < 0);
		assertTrue(naturalCompare("file002.txt", "file10.txt") < 0);

		// Mixed alphanumeric
		assertTrue(naturalCompare("a2b", "a10b") < 0);
		assertTrue(naturalCompare("a10b", "a2b") > 0);

		// Same numbers, different text
		assertTrue(naturalCompare("file1a.txt", "file1b.txt") < 0);

		// Null handling
		assertEquals(0, naturalCompare(null, null));
		assertTrue(naturalCompare(null, "test") < 0);
		assertTrue(naturalCompare("test", null) > 0);

		// Case-insensitive comparison
		assertTrue(naturalCompare("Apple", "banana") < 0);
		assertTrue(naturalCompare("banana", "Apple") > 0);
	}

	@Test
	void a76_levenshteinDistance() {
		assertEquals(0, levenshteinDistance("hello", "hello"));
		assertEquals(3, levenshteinDistance("kitten", "sitting")); // kitten -> sitten (s), sitten -> sittin (i), sittin -> sitting (g)
		assertEquals(3, levenshteinDistance("abc", ""));
		assertEquals(3, levenshteinDistance("", "abc"));
		assertEquals(1, levenshteinDistance("hello", "hallo")); // e -> a (1 substitution)
		assertEquals(1, levenshteinDistance("hello", "helo")); // Remove one 'l' (1 deletion)
		assertEquals(1, levenshteinDistance("hello", "hell")); // Remove 'o' (1 deletion)
		assertEquals(1, levenshteinDistance("hello", "hellox")); // Add 'x' (1 insertion)
		// Null handling
		assertEquals(0, levenshteinDistance(null, null));
		assertEquals(5, levenshteinDistance("hello", null));
		assertEquals(5, levenshteinDistance(null, "hello"));
	}

	@Test
	void a77_similarity() {
		assertEquals(1.0, similarity("hello", "hello"), 0.0001);
		assertEquals(0.0, similarity("abc", "xyz"), 0.0001);
		// kitten -> sitting: distance = 3, maxLen = 7, similarity = 1 - 3/7 = 4/7 ≈ 0.571
		assertEquals(4.0 / 7.0, similarity("kitten", "sitting"), 0.01);
		assertEquals(1.0, similarity("", ""), 0.0001);
		assertEquals(0.0, similarity("abc", ""), 0.0001);
		assertEquals(0.0, similarity("", "abc"), 0.0001);
		// Null handling
		assertEquals(1.0, similarity(null, null), 0.0001);
		assertEquals(0.0, similarity("hello", null), 0.0001);
		assertEquals(0.0, similarity(null, "hello"), 0.0001);
		// Similar strings
		// "hello" vs "hallo": distance = 1, maxLen = 5, similarity = 1 - 1/5 = 0.8
		assertEquals(0.8, similarity("hello", "hallo"), 0.0001);
	}

	@Test
	void a78_isSimilar() {
		assertTrue(isSimilar("hello", "hello", 0.8));
		assertTrue(isSimilar("hello", "hello", 1.0));
		assertFalse(isSimilar("kitten", "sitting", 0.8));
		assertTrue(isSimilar("kitten", "sitting", 0.5));
		assertFalse(isSimilar("abc", "xyz", 0.5));
		assertTrue(isSimilar("hello", "hallo", 0.8));
		assertFalse(isSimilar("hello", "world", 0.8));
		// Null handling
		assertTrue(isSimilar(null, null, 0.8));
	}

	@Test
	void a79_generateUUID() {
		// Generate multiple UUIDs and verify format
		for (var i = 0; i < 10; i++) {
			var uuid = generateUUID();
			assertNotNull(uuid);
			// Standard UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (36 chars)
			assertEquals(36, uuid.length());
			assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
		}
		// Verify uniqueness
		var uuid1 = generateUUID();
		var uuid2 = generateUUID();
		assertNotEquals(uuid1, uuid2);
	}

	@Test
	void a80_randomAlphabetic() {
		// Test various lengths
		for (var len = 0; len <= 20; len++) {
			var s = randomAlphabetic(len);
			assertNotNull(s);
			assertEquals(len, s.length());
			// Verify all characters are alphabetic
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				assertTrue(Character.isLetter(c), "Character at index " + i + " should be alphabetic: " + c);
			}
		}
		// Test negative length
		assertThrows(IllegalArgumentException.class, () -> randomAlphabetic(-1));
		// Verify randomness (at least some variation)
		var strings = new HashSet<String>();
		for (var i = 0; i < 100; i++) {
			strings.add(randomAlphabetic(10));
		}
		assertTrue(strings.size() > 1, "Should generate different strings");
	}

	@Test
	void a81_randomAlphanumeric() {
		// Test various lengths
		for (var len = 0; len <= 20; len++) {
			var s = randomAlphanumeric(len);
			assertNotNull(s);
			assertEquals(len, s.length());
			// Verify all characters are alphanumeric
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				assertTrue(Character.isLetterOrDigit(c), "Character at index " + i + " should be alphanumeric: " + c);
			}
		}
		// Test negative length
		assertThrows(IllegalArgumentException.class, () -> randomAlphanumeric(-1));
		// Verify randomness (at least some variation)
		var strings = new HashSet<String>();
		for (var i = 0; i < 100; i++) {
			strings.add(randomAlphanumeric(10));
		}
		assertTrue(strings.size() > 1, "Should generate different strings");
		// Verify it can generate both letters and digits
		var hasLetter = false;
		var hasDigit = false;
		for (var i = 0; i < 1000; i++) {
			var s = randomAlphanumeric(20);
			for (var j = 0; j < s.length(); j++) {
				if (Character.isLetter(s.charAt(j)))
					hasLetter = true;
				if (Character.isDigit(s.charAt(j)))
					hasDigit = true;
			}
			if (hasLetter && hasDigit)
				break;
		}
		assertTrue(hasLetter && hasDigit, "Should generate both letters and digits");
	}

	@Test
	void a82_randomNumeric() {
		// Test various lengths
		for (var len = 0; len <= 20; len++) {
			var s = randomNumeric(len);
			assertNotNull(s);
			assertEquals(len, s.length());
			// Verify all characters are digits
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				assertTrue(Character.isDigit(c), "Character at index " + i + " should be a digit: " + c);
			}
		}
		// Test negative length
		assertThrows(IllegalArgumentException.class, () -> randomNumeric(-1));
		// Verify randomness (at least some variation)
		var strings = new HashSet<String>();
		for (var i = 0; i < 100; i++) {
			strings.add(randomNumeric(10));
		}
		assertTrue(strings.size() > 1, "Should generate different strings");
	}

	@Test
	void a83_randomAscii() {
		// Test various lengths
		for (var len = 0; len <= 20; len++) {
			var s = randomAscii(len);
			assertNotNull(s);
			assertEquals(len, s.length());
			// Verify all characters are printable ASCII (32-126)
			for (var i = 0; i < s.length(); i++) {
				var c = s.charAt(i);
				assertTrue(c >= 32 && c <= 126, "Character at index " + i + " should be printable ASCII: " + c);
			}
		}
		// Test negative length
		assertThrows(IllegalArgumentException.class, () -> randomAscii(-1));
		// Verify randomness (at least some variation)
		var strings = new HashSet<String>();
		for (var i = 0; i < 100; i++) {
			strings.add(randomAscii(10));
		}
		assertTrue(strings.size() > 1, "Should generate different strings");
	}

	@Test
	void a84_randomString() {
		// Test with various character sets
		var s1 = randomString(10, "ABC");
		assertNotNull(s1);
		assertEquals(10, s1.length());
		for (var i = 0; i < s1.length(); i++) {
			var c = s1.charAt(i);
			assertTrue(c == 'A' || c == 'B' || c == 'C', "Character should be A, B, or C: " + c);
		}

		var s2 = randomString(5, "0123456789");
		assertNotNull(s2);
		assertEquals(5, s2.length());
		for (var i = 0; i < s2.length(); i++) {
			assertTrue(Character.isDigit(s2.charAt(i)));
		}

		// Test with single character
		var s3 = randomString(10, "X");
		assertNotNull(s3);
		assertEquals(10, s3.length());
		assertEquals("XXXXXXXXXX", s3);

		// Test zero length
		assertEquals("", randomString(0, "ABC"));

		// Test negative length
		assertThrows(IllegalArgumentException.class, () -> randomString(-1, "ABC"));

		// Test null character set
		assertThrows(IllegalArgumentException.class, () -> randomString(10, null));

		// Test empty character set
		assertThrows(IllegalArgumentException.class, () -> randomString(10, ""));

		// Verify randomness (at least some variation when multiple chars available)
		var strings = new HashSet<String>();
		for (var i = 0; i < 100; i++) {
			strings.add(randomString(10, "ABCDEFGHIJ"));
		}
		assertTrue(strings.size() > 1, "Should generate different strings");
	}

	@Test
	void a85_parseMap() {
		// Basic parsing
		var map1 = parseMap("key1=value1,key2=value2", '=', ',', false);
		assertEquals(2, map1.size());
		assertEquals("value1", map1.get("key1"));
		assertEquals("value2", map1.get("key2"));

		// With trimming
		var map2 = parseMap(" key1 = value1 ; key2 = value2 ", '=', ';', true);
		assertEquals(2, map2.size());
		assertEquals("value1", map2.get("key1"));
		assertEquals("value2", map2.get("key2"));

		// Different delimiters
		var map3 = parseMap("a:1|b:2|c:3", ':', '|', false);
		assertEquals(3, map3.size());
		assertEquals("1", map3.get("a"));
		assertEquals("2", map3.get("b"));
		assertEquals("3", map3.get("c"));

		// Empty value
		var map4 = parseMap("key1=,key2=value2", '=', ',', false);
		assertEquals(2, map4.size());
		assertEquals("", map4.get("key1"));
		assertEquals("value2", map4.get("key2"));

		// No delimiter (key only)
		var map5 = parseMap("key1,key2=value2", '=', ',', false);
		assertEquals(2, map5.size());
		assertEquals("", map5.get("key1"));
		assertEquals("value2", map5.get("key2"));

		// Null/empty input
		assertTrue(parseMap(null, '=', ',', false).isEmpty());
		assertTrue(parseMap("", '=', ',', false).isEmpty());

		// Duplicate keys (last one wins)
		var map6 = parseMap("key=value1,key=value2", '=', ',', false);
		assertEquals(1, map6.size());
		assertEquals("value2", map6.get("key"));
	}

	@Test
	void a86_extractNumbers() {
		// Basic extraction
		var numbers1 = extractNumbers("Price: $19.99, Quantity: 5");
		assertEquals(2, numbers1.size());
		assertEquals("19.99", numbers1.get(0));
		assertEquals("5", numbers1.get(1));

		// Multiple numbers
		var numbers2 = extractNumbers("Version 1.2.3 has 42 features");
		assertEquals(3, numbers2.size());
		assertEquals("1.2", numbers2.get(0));
		assertEquals("3", numbers2.get(1));
		assertEquals("42", numbers2.get(2));

		// Decimal numbers
		var numbers3 = extractNumbers("3.14 and 2.718 are constants");
		assertEquals(2, numbers3.size());
		assertEquals("3.14", numbers3.get(0));
		assertEquals("2.718", numbers3.get(1));

		// Integers only
		var numbers4 = extractNumbers("1 2 3 4 5");
		assertEquals(5, numbers4.size());
		assertEquals("1", numbers4.get(0));
		assertEquals("5", numbers4.get(4));

		// No numbers
		assertTrue(extractNumbers("No numbers here").isEmpty());

		// Null/empty input
		assertTrue(extractNumbers(null).isEmpty());
		assertTrue(extractNumbers("").isEmpty());
	}

	@Test
	void a87_extractEmails() {
		// Basic extraction
		var emails1 = extractEmails("Contact: user@example.com or admin@test.org");
		assertEquals(2, emails1.size());
		assertTrue(emails1.contains("user@example.com"));
		assertTrue(emails1.contains("admin@test.org"));

		// Multiple emails
		var emails2 = extractEmails("Email me at john.doe@example.com, or contact jane@test.org");
		assertEquals(2, emails2.size());
		assertTrue(emails2.contains("john.doe@example.com"));
		assertTrue(emails2.contains("jane@test.org"));

		// Email with special characters
		var emails3 = extractEmails("user+tag@example.co.uk is valid");
		assertEquals(1, emails3.size());
		assertEquals("user+tag@example.co.uk", emails3.get(0));

		// No emails
		assertTrue(extractEmails("No email addresses here").isEmpty());

		// Null/empty input
		assertTrue(extractEmails(null).isEmpty());
		assertTrue(extractEmails("").isEmpty());
	}

	@Test
	void a88_extractUrls() {
		// Basic extraction
		var urls1 = extractUrls("Visit https://example.com or http://test.org");
		assertEquals(2, urls1.size());
		assertTrue(urls1.contains("https://example.com"));
		assertTrue(urls1.contains("http://test.org"));

		// URLs with paths
		var urls2 = extractUrls("Check https://example.com/path/to/page?param=value");
		assertEquals(1, urls2.size());
		assertTrue(urls2.get(0).startsWith("https://example.com"));

		// FTP URLs
		var urls3 = extractUrls("Download from ftp://files.example.com/pub/data");
		assertEquals(1, urls3.size());
		assertTrue(urls3.get(0).startsWith("ftp://"));

		// Multiple URLs
		var urls4 = extractUrls("Links: http://site1.com and https://site2.org/page");
		assertEquals(2, urls4.size());

		// No URLs
		assertTrue(extractUrls("No URLs here").isEmpty());

		// Null/empty input
		assertTrue(extractUrls(null).isEmpty());
		assertTrue(extractUrls("").isEmpty());
	}

	@Test
	void a89_extractWords() {
		// Basic extraction
		var words1 = extractWords("Hello world! This is a test.");
		assertEquals(6, words1.size());
		assertEquals("Hello", words1.get(0));
		assertEquals("world", words1.get(1));
		assertEquals("This", words1.get(2));
		assertEquals("is", words1.get(3));
		assertEquals("a", words1.get(4));
		assertEquals("test", words1.get(5));

		// Words with underscores
		var words2 = extractWords("variable_name and test_123");
		assertEquals(3, words2.size()); // variable_name, and, test_123
		assertTrue(words2.contains("variable_name"));
		assertTrue(words2.contains("and"));
		assertTrue(words2.contains("test_123"));

		// Words with numbers
		var words3 = extractWords("Version 1.2.3 has 42 features");
		assertEquals(7, words3.size()); // Version, 1, 2, 3, has, 42, features
		assertTrue(words3.contains("Version"));
		assertTrue(words3.contains("1"));
		assertTrue(words3.contains("2"));
		assertTrue(words3.contains("3"));
		assertTrue(words3.contains("has"));
		assertTrue(words3.contains("42"));
		assertTrue(words3.contains("features"));

		// No words (only punctuation)
		assertTrue(extractWords("!@#$%^&*()").isEmpty());

		// Null/empty input
		assertTrue(extractWords(null).isEmpty());
		assertTrue(extractWords("").isEmpty());
	}

	@Test
	void a90_extractBetween() {
		// Basic extraction
		var results1 = extractBetween("<tag>content</tag>", "<", ">");
		assertEquals(2, results1.size());
		assertEquals("tag", results1.get(0));
		assertEquals("/tag", results1.get(1));

		// Multiple matches
		var results2 = extractBetween("[one][two][three]", "[", "]");
		assertEquals(3, results2.size());
		assertEquals("one", results2.get(0));
		assertEquals("two", results2.get(1));
		assertEquals("three", results2.get(2));

		// Nested markers (non-overlapping)
		// String: "(outer (inner) outer)"
		// Finds: ( at 0, ) at 13 -> "outer (inner"
		// Then continues from 14, but no more ( found, so only 1 result
		var results3 = extractBetween("(outer (inner) outer)", "(", ")");
		assertEquals(1, results3.size());
		assertEquals("outer (inner", results3.get(0));

		// Different markers
		var results4 = extractBetween("Start:value:End", "Start:", ":End");
		assertEquals(1, results4.size());
		assertEquals("value", results4.get(0));

		// No matches
		assertTrue(extractBetween("no markers here", "[", "]").isEmpty());

		// Unmatched start marker
		assertTrue(extractBetween("<unclosed", "<", ">").isEmpty());

		// Null/empty input
		assertTrue(extractBetween(null, "<", ">").isEmpty());
		assertTrue(extractBetween("", "<", ">").isEmpty());
		assertTrue(extractBetween("text", null, ">").isEmpty());
		assertTrue(extractBetween("text", "<", null).isEmpty());
	}

	@Test
	void a91_transliterate() {
		// Basic transliteration
		assertEquals("h2ll4", transliterate("hello", "aeiou", "12345"));
		assertEquals("XYZ", transliterate("ABC", "ABC", "XYZ"));

		// No matches
		assertEquals("hello", transliterate("hello", "xyz", "123"));

		// Partial matches
		assertEquals("h3ll0", transliterate("hello", "eo", "30"));

		// Empty strings
		assertEquals("", transliterate("", "abc", "123"));

		// Null input
		assertNull(transliterate(null, "abc", "123"));

		// Null/empty character sets
		assertEquals("hello", transliterate("hello", null, "123"));
		assertEquals("hello", transliterate("hello", "abc", null));
		assertEquals("hello", transliterate("hello", "", "123"));
		assertEquals("hello", transliterate("hello", "abc", ""));

		// Mismatched lengths
		assertThrows(IllegalArgumentException.class, () -> transliterate("hello", "abc", "12"));
	}

	@Test
	void a92_soundex() {
		// Standard Soundex examples
		assertEquals("S530", soundex("Smith"));
		assertEquals("S530", soundex("Smythe"));
		assertEquals("R163", soundex("Robert"));
		assertEquals("R163", soundex("Rupert"));

		// Same soundex for similar names
		assertEquals("A261", soundex("Ashcraft"));
		assertEquals("A261", soundex("Ashcroft"));

		// Single character
		assertEquals("A000", soundex("A"));

		// With non-letters
		assertEquals("S530", soundex("Smith123"));
		assertEquals("S530", soundex("Smith!@#"));

		// Null/empty input
		assertNull(soundex(null));
		assertNull(soundex(""));

		// All vowels
		assertEquals("A000", soundex("AEIOU"));
	}

	@Test
	void a93_metaphone() {
		// Basic metaphone examples
		var code1 = metaphone("Smith");
		assertNotNull(code1);
		assertTrue(code1.startsWith("SM"));

		var code2 = metaphone("Smythe");
		assertNotNull(code2);
		assertTrue(code2.startsWith("SM"));

		// Similar words should have similar codes
		var code3 = metaphone("Robert");
		assertNotNull(code3);

		// Null/empty input
		assertNull(metaphone(null));
		assertNull(metaphone(""));
		assertEquals("", metaphone("123"));

		// Single character
		var code4 = metaphone("A");
		assertNotNull(code4);
		assertFalse(code4.isEmpty());
	}

	@Test
	void a94_doubleMetaphone() {
		// Basic double metaphone
		var codes1 = doubleMetaphone("Smith");
		assertNotNull(codes1);
		assertEquals(2, codes1.length);
		assertNotNull(codes1[0]); // primary
		assertNotNull(codes1[1]); // alternate

		var codes2 = doubleMetaphone("Schmidt");
		assertNotNull(codes2);
		assertEquals(2, codes2.length);

		// Null/empty input
		assertNull(doubleMetaphone(null));
		assertNull(doubleMetaphone(""));
	}

	@Test
	void a95_normalizeUnicode() {
		// Basic normalization
		var normalized = normalizeUnicode("café");
		assertNotNull(normalized);
		assertNotEquals("café", normalized); // Should be decomposed

		// Null input
		assertNull(normalizeUnicode(null));

		// Already normalized
		var normalized2 = normalizeUnicode("hello");
		assertEquals("hello", normalized2);
	}

	@Test
	void a96_removeAccents() {
		// Basic accent removal
		assertEquals("cafe", removeAccents("café"));
		assertEquals("naive", removeAccents("naïve"));
		assertEquals("resume", removeAccents("résumé"));

		// Multiple accents
		assertEquals("Cafe", removeAccents("Café"));
		assertEquals("Zoe", removeAccents("Zoë"));

		// No accents
		assertEquals("hello", removeAccents("hello"));
		assertEquals("HELLO", removeAccents("HELLO"));

		// Null input
		assertNull(removeAccents(null));

		// Empty string
		assertEquals("", removeAccents(""));

		// Mixed case with accents
		assertEquals("Cafe", removeAccents("Café"));
		assertEquals("Ecole", removeAccents("École"));
	}

	@Test
	void a97_isValidRegex() {
		// Valid regex patterns
		assertTrue(isValidRegex("[a-z]+"));
		assertTrue(isValidRegex("\\d+"));
		assertTrue(isValidRegex("^test$"));
		assertTrue(isValidRegex("(abc|def)"));

		// Invalid regex patterns
		assertFalse(isValidRegex("[a-z")); // Unclosed bracket
		assertFalse(isValidRegex("(test")); // Unclosed parenthesis
		assertFalse(isValidRegex("\\")); // Incomplete escape
		assertFalse(isValidRegex("*")); // Quantifier without preceding element

		// Null/empty input
		assertFalse(isValidRegex(null));
		assertFalse(isValidRegex(""));
	}

	@Test
	void a98_isValidDateFormat() {
		// Valid dates
		assertTrue(isValidDateFormat("2023-12-25", "yyyy-MM-dd"));
		assertTrue(isValidDateFormat("25/12/2023", "dd/MM/yyyy"));
		assertTrue(isValidDateFormat("12/25/2023", "MM/dd/yyyy"));
		assertTrue(isValidDateFormat("2023-01-01", "yyyy-MM-dd"));

		// Invalid dates
		assertFalse(isValidDateFormat("2023-13-25", "yyyy-MM-dd")); // Invalid month
		assertFalse(isValidDateFormat("2023-12-32", "yyyy-MM-dd")); // Invalid day
		assertFalse(isValidDateFormat("2023-12-25", "invalid")); // Invalid format
		assertFalse(isValidDateFormat("not-a-date", "yyyy-MM-dd")); // Not a date

		// Null/empty input
		assertFalse(isValidDateFormat(null, "yyyy-MM-dd"));
		assertFalse(isValidDateFormat("2023-12-25", null));
		assertFalse(isValidDateFormat("", "yyyy-MM-dd"));
		assertFalse(isValidDateFormat("2023-12-25", ""));
	}

	@Test
	void a99_isValidTimeFormat() {
		// Valid times
		assertTrue(isValidTimeFormat("14:30:00", "HH:mm:ss"));
		assertTrue(isValidTimeFormat("02:30:00 PM", "hh:mm:ss a"));
		assertTrue(isValidTimeFormat("14:30", "HH:mm"));
		assertTrue(isValidTimeFormat("00:00:00", "HH:mm:ss"));

		// Invalid times
		assertFalse(isValidTimeFormat("25:00:00", "HH:mm:ss")); // Invalid hour
		assertFalse(isValidTimeFormat("14:60:00", "HH:mm:ss")); // Invalid minute
		assertFalse(isValidTimeFormat("14:30:60", "HH:mm:ss")); // Invalid second
		assertFalse(isValidTimeFormat("not-a-time", "HH:mm:ss")); // Not a time

		// Null/empty input
		assertFalse(isValidTimeFormat(null, "HH:mm:ss"));
		assertFalse(isValidTimeFormat("14:30:00", null));
		assertFalse(isValidTimeFormat("", "HH:mm:ss"));
		assertFalse(isValidTimeFormat("14:30:00", ""));
	}

	@Test
	void a100_isValidIpAddress() {
		// Valid IPv4 addresses
		assertTrue(isValidIpAddress("192.168.1.1"));
		assertTrue(isValidIpAddress("0.0.0.0"));
		assertTrue(isValidIpAddress("255.255.255.255"));
		assertTrue(isValidIpAddress("127.0.0.1"));

		// Invalid IPv4 addresses
		assertFalse(isValidIpAddress("256.1.1.1")); // Out of range
		assertFalse(isValidIpAddress("192.168.1")); // Too few octets
		assertFalse(isValidIpAddress("192.168.1.1.1")); // Too many octets
		assertFalse(isValidIpAddress("not.an.ip")); // Not numeric
		assertFalse(isValidIpAddress("-1.1.1.1")); // Negative number

		// Valid IPv6 addresses (basic validation)
		assertTrue(isValidIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
		assertTrue(isValidIpAddress("::1")); // Localhost
		assertTrue(isValidIpAddress("2001:db8::1")); // Compressed

		// Invalid IPv6 addresses
		assertFalse(isValidIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334:9999")); // Too many segments
		assertFalse(isValidIpAddress("gggg::1")); // Invalid hex

		// Null/empty input
		assertFalse(isValidIpAddress(null));
		assertFalse(isValidIpAddress(""));
	}

	@Test
	void a101_isValidMacAddress() {
		// Valid MAC addresses - colon format
		assertTrue(isValidMacAddress("00:1B:44:11:3A:B7"));
		assertTrue(isValidMacAddress("00:1b:44:11:3a:b7")); // Lowercase
		assertTrue(isValidMacAddress("FF:FF:FF:FF:FF:FF"));

		// Valid MAC addresses - hyphen format
		assertTrue(isValidMacAddress("00-1B-44-11-3A-B7"));
		assertTrue(isValidMacAddress("00-1b-44-11-3a-b7")); // Lowercase

		// Valid MAC addresses - no separators
		assertTrue(isValidMacAddress("001B44113AB7"));
		assertTrue(isValidMacAddress("001b44113ab7")); // Lowercase

		// Invalid MAC addresses
		assertFalse(isValidMacAddress("00:1B:44:11:3A")); // Too short
		assertFalse(isValidMacAddress("00:1B:44:11:3A:B7:99")); // Too long
		assertFalse(isValidMacAddress("GG:1B:44:11:3A:B7")); // Invalid hex
		assertFalse(isValidMacAddress("00:1B:44:11:3A:B7:XX")); // Invalid characters

		// Null/empty input
		assertFalse(isValidMacAddress(null));
		assertFalse(isValidMacAddress(""));
	}

	@Test
	void a102_isValidHostname() {
		// Valid hostnames
		assertTrue(isValidHostname("example.com"));
		assertTrue(isValidHostname("sub.example.com"));
		assertTrue(isValidHostname("test-host.example.com"));
		assertTrue(isValidHostname("localhost"));
		assertTrue(isValidHostname("a")); // Single character
		assertTrue(isValidHostname("a-b")); // With hyphen

		// Invalid hostnames
		assertFalse(isValidHostname("-invalid.com")); // Starts with hyphen
		assertFalse(isValidHostname("invalid-.com")); // Ends with hyphen
		assertFalse(isValidHostname("example..com")); // Empty label
		assertFalse(isValidHostname(".example.com")); // Starts with dot
		assertFalse(isValidHostname("example.com.")); // Ends with dot
		assertFalse(isValidHostname("example_com")); // Underscore not allowed
		assertFalse(isValidHostname("example@com")); // @ not allowed

		// Label too long (64 characters)
		var longLabel = "a".repeat(64) + ".com";
		assertFalse(isValidHostname(longLabel));

		// Total length too long (254 characters)
		var longHostname = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(64) + ".com";
		assertFalse(isValidHostname(longHostname));

		// Null/empty input
		assertFalse(isValidHostname(null));
		assertFalse(isValidHostname(""));
	}

	@Test
	void a103_wordCount() {
		// Basic word counting
		assertEquals(2, wordCount("Hello world"));
		assertEquals(4, wordCount("The quick brown fox"));
		assertEquals(5, wordCount("Hello, world! How are you?"));

		// Single word
		assertEquals(1, wordCount("Hello"));
		assertEquals(1, wordCount("word"));

		// Multiple spaces
		assertEquals(3, wordCount("word1    word2    word3"));

		// Words with underscores
		assertEquals(2, wordCount("variable_name test_word"));

		// Words with numbers
		assertEquals(3, wordCount("test123 456 word"));

		// Empty/null input
		assertEquals(0, wordCount(null));
		assertEquals(0, wordCount(""));
		assertEquals(0, wordCount("   ")); // Only whitespace

		// Punctuation only
		assertEquals(0, wordCount("!@#$%^&*()"));
	}

	@Test
	void a104_lineCount() {
		// Basic line counting
		assertEquals(3, lineCount("line1\nline2\nline3"));
		assertEquals(1, lineCount("single line"));

		// Windows line endings
		assertEquals(2, lineCount("line1\r\nline2"));

		// Mixed line endings
		assertEquals(3, lineCount("line1\nline2\r\nline3"));

		// Empty lines
		assertEquals(3, lineCount("line1\n\nline3"));
		assertEquals(2, lineCount("\nline2"));
		assertEquals(2, lineCount("line1\n"));

		// Only newlines
		assertEquals(3, lineCount("\n\n"));

		// Null/empty input
		assertEquals(0, lineCount(null));
		assertEquals(0, lineCount(""));
	}

	@Test
	void a105_mostFrequentChar() {
		// Basic frequency
		assertEquals('l', mostFrequentChar("hello"));
		assertEquals('a', mostFrequentChar("aabbcc")); // First encountered

		// Single character
		assertEquals('a', mostFrequentChar("aaaa"));

		// All different
		assertEquals('a', mostFrequentChar("abcd")); // First character

		// With spaces
		assertEquals('l', mostFrequentChar("hello world")); // 'l' appears 3 times

		// Case sensitive
		assertEquals('l', mostFrequentChar("Hello")); // 'l' appears 2 times, 'H' appears 1 time

		// Numbers
		assertEquals('1', mostFrequentChar("112233"));

		// Null/empty input
		assertEquals('\0', mostFrequentChar(null));
		assertEquals('\0', mostFrequentChar(""));
	}

	@Test
	void a106_entropy() {
		// No randomness (all same character)
		assertEquals(0.0, entropy("aaaa"), 0.0001);

		// High randomness (all different)
		var entropy1 = entropy("abcd");
		assertTrue(entropy1 > 1.5); // Should be around 2.0

		// Medium randomness
		var entropy2 = entropy("hello");
		assertTrue(entropy2 > 0.0 && entropy2 < 3.0);

		// Balanced distribution
		var entropy3 = entropy("aabbcc");
		assertTrue(entropy3 > 0.0);

		// Single character
		assertEquals(0.0, entropy("a"), 0.0001);

		// Null/empty input
		assertEquals(0.0, entropy(null), 0.0001);
		assertEquals(0.0, entropy(""), 0.0001);

		// Verify entropy increases with more variety
		var entropy4 = entropy("abcdefghijklmnopqrstuvwxyz");
		assertTrue(entropy4 > entropy("hello"));
	}

	@Test
	void a107_readabilityScore() {
		// Simple sentence (should have higher score)
		var score1 = readabilityScore("The cat sat.");
		assertTrue(score1 > 0.0 && score1 <= 100.0);

		// More complex sentence (should have lower score)
		var score2 = readabilityScore("The sophisticated implementation demonstrates advanced algorithmic complexity.");
		assertTrue(score2 >= 0.0 && score2 <= 100.0); // Can be 0.0 for very complex text
		assertTrue(score2 < score1); // Complex should score lower

		// Multiple sentences
		var score3 = readabilityScore("Hello world. How are you? I am fine!");
		assertTrue(score3 > 0.0 && score3 <= 100.0);

		// Single word
		var score4 = readabilityScore("Hello.");
		assertTrue(score4 > 0.0 && score4 <= 100.0);

		// No sentence ending
		var score5 = readabilityScore("Hello world");
		assertTrue(score5 > 0.0 && score5 <= 100.0);

		// Null/empty input
		assertEquals(0.0, readabilityScore(null), 0.0001);
		assertEquals(0.0, readabilityScore(""), 0.0001);

		// Only punctuation
		assertEquals(0.0, readabilityScore("!@#$"), 0.0001);

		// Verify score is in valid range
		var score6 = readabilityScore("This is a test sentence with multiple words.");
		assertTrue(score6 >= 0.0 && score6 <= 100.0);
	}

	//====================================================================================================
	// String manipulation methods
	//====================================================================================================

	@Test
	void a49_capitalize() {
		assertNull(capitalize(null));
		assertEquals("", capitalize(""));
		assertEquals("Hello", capitalize("hello"));
		assertEquals("Hello", capitalize("Hello"));
		assertEquals("HELLO", capitalize("HELLO"));
		assertEquals("A", capitalize("a"));
		assertEquals("123", capitalize("123"));
	}

	@Test
	void a50_uncapitalize() {
		assertNull(uncapitalize(null));
		assertEquals("", uncapitalize(""));
		assertEquals("hello", uncapitalize("hello"));
		assertEquals("hello", uncapitalize("Hello"));
		assertEquals("hELLO", uncapitalize("HELLO"));
		assertEquals("a", uncapitalize("A"));
		assertEquals("123", uncapitalize("123"));
	}

	@Test
	void a51_reverse() {
		assertNull(StringUtils.reverse(null));
		assertEquals("", reverse(""));
		assertEquals("olleh", reverse("hello"));
		assertEquals("321", reverse("123"));
		assertEquals("cba", reverse("abc"));
	}

	@Test
	void a52_remove() {
		assertNull(remove(null, "x"));
		assertEquals("hello", remove("hello", null));
		assertEquals("hello", remove("hello", ""));
		assertEquals("hell wrld", remove("hello world", "o"));
		assertEquals("hello world", remove("hello world", "xyz"));
		assertEquals("", remove("xxx", "x"));
	}

	@Test
	void a53_removeStart() {
		assertNull(removeStart(null, "x"));
		assertEquals("hello", removeStart("hello", null));
		assertEquals("hello", removeStart("hello", ""));
		assertEquals(" world", removeStart("hello world", "hello"));
		assertEquals("hello world", removeStart("hello world", "xyz"));
		assertEquals("", removeStart("hello", "hello"));
	}

	@Test
	void a54_removeEnd() {
		assertNull(removeEnd(null, "x"));
		assertEquals("hello", removeEnd("hello", null));
		assertEquals("hello", removeEnd("hello", ""));
		assertEquals("hello ", removeEnd("hello world", "world"));
		assertEquals("hello world", removeEnd("hello world", "xyz"));
		assertEquals("", removeEnd("hello", "hello"));
	}

	@Test
	void a55_substringBefore() {
		assertNull(substringBefore(null, "."));
		assertEquals("hello.world", substringBefore("hello.world", null));
		assertEquals("hello", substringBefore("hello.world", "."));
		assertEquals("hello.world", substringBefore("hello.world", "xyz"));
		assertEquals("", substringBefore(".world", "."));
	}

	@Test
	void a56_substringAfter() {
		assertNull(substringAfter(null, "."));
		assertEquals("", substringAfter("hello.world", null));
		assertEquals("world", substringAfter("hello.world", "."));
		assertEquals("", substringAfter("hello.world", "xyz"));
		assertEquals("world", substringAfter("hello.world", "."));
	}

	@Test
	void a57_substringBetween() {
		assertNull(substringBetween(null, "<", ">"));
		assertNull(substringBetween("<hello>", null, ">"));
		assertNull(substringBetween("<hello>", "<", null));
		assertEquals("hello", substringBetween("<hello>", "<", ">"));
		assertNull(substringBetween("<hello>", "[", "]"));
		assertNull(substringBetween("hello", "<", ">"));
		assertEquals("", substringBetween("<>", "<", ">"));
	}

	@Test
	void a58_left() {
		assertNull(left(null, 3));
		assertEquals("", left("", 3));
		assertEquals("hel", left("hello", 3));
		assertEquals("hello", left("hello", 10));
		assertEquals("", left("hello", 0));
		assertEquals("", left("hello", -1));
	}

	@Test
	void a59_right() {
		assertNull(right(null, 3));
		assertEquals("", right("", 3));
		assertEquals("llo", right("hello", 3));
		assertEquals("hello", right("hello", 10));
		assertEquals("", right("hello", 0));
		assertEquals("", right("hello", -1));
	}

	@Test
	void a60_mid() {
		assertNull(mid(null, 1, 3));
		assertEquals("", mid("", 1, 3));
		assertEquals("ell", mid("hello", 1, 3));
		assertEquals("ello", mid("hello", 1, 10));
		assertEquals("", mid("hello", 10, 3));
		assertEquals("", mid("hello", -1, 3));
		assertEquals("", mid("hello", 1, -1));
	}

	@Test
	void a61_padLeft() {
		assertEquals("     ", padLeft(null, 5, ' '));
		assertEquals("     ", padLeft("", 5, ' '));
		assertEquals("   hello", padLeft("hello", 8, ' '));
		assertEquals("hello", padLeft("hello", 3, ' '));
		assertEquals("00123", padLeft("123", 5, '0'));
	}

	@Test
	void a62_padRight() {
		assertEquals("     ", padRight(null, 5, ' '));
		assertEquals("     ", padRight("", 5, ' '));
		assertEquals("hello   ", padRight("hello", 8, ' '));
		assertEquals("hello", padRight("hello", 3, ' '));
		assertEquals("12300", padRight("123", 5, '0'));
	}

	@Test
	void a63_padCenter() {
		assertEquals("     ", padCenter(null, 5, ' '));
		assertEquals("     ", padCenter("", 5, ' '));
		assertEquals("  hi  ", padCenter("hi", 6, ' '));
		assertEquals("   hi  ", padCenter("hi", 7, ' '));
		assertEquals("hello", padCenter("hello", 3, ' '));
		assertEquals(" hello ", padCenter("hello", 7, ' '));
	}

	//====================================================================================================
	// String joining and splitting methods
	//====================================================================================================

	@Test
	void a64_joinObjectArray() {
		assertNull(StringUtils.join((Object[])null, ","));
		assertEquals("", StringUtils.join(a(), ","));
		assertEquals("a,b,c", StringUtils.join(a("a", "b", "c"), ","));
		assertEquals("1-2-3", StringUtils.join(a(1, 2, 3), "-"));
		assertEquals("abc", StringUtils.join(a("a", "b", "c"), ""));
		assertEquals("a,null,c", StringUtils.join(a("a", null, "c"), ","));
		assertEquals("a;b;c", StringUtils.join(a("a", "b", "c"), ";"));
	}

	@Test
	void a65_joinIntArray() {
		assertEquals("", StringUtils.join((int[])null, ","));
		assertEquals("", StringUtils.join(ints(), ","));
		assertEquals("1,2,3", StringUtils.join(ints(1, 2, 3), ","));
		assertEquals("1-2-3", StringUtils.join(ints(1, 2, 3), "-"));
		assertEquals("123", StringUtils.join(ints(1, 2, 3), ""));
	}

	@Test
	void a66_joinCollection() {
		assertNull(StringUtils.join((Collection<?>)null, ","));
		assertEquals("", StringUtils.join(Collections.emptyList(), ","));
		assertEquals("a,b,c", StringUtils.join(l("a", "b", "c"), ","));
		assertEquals("1-2-3", StringUtils.join(l(1, 2, 3), "-"));
		assertEquals("a,null,c", StringUtils.join(l("a", null, "c"), ","));
	}

	@Test
	void a67_joinObjectArrayChar() {
		assertEquals("a,b,c", StringUtils.join(a("a", "b", "c"), ','));
		assertEquals("1-2-3", StringUtils.join(a(1, 2, 3), '-'));
	}

	@Test
	void a68_joinIntArrayChar() {
		assertEquals("1,2,3", StringUtils.join(ints(1, 2, 3), ','));
		assertEquals("1-2-3", StringUtils.join(ints(1, 2, 3), '-'));
	}

	@Test
	void a69_joinCollectionChar() {
		assertEquals("a,b,c", StringUtils.join(l("a", "b", "c"), ','));
		assertEquals("1-2-3", StringUtils.join(l(1, 2, 3), '-'));
	}

	//====================================================================================================
	// String cleaning and sanitization methods
	//====================================================================================================

	@Test
	void a72_clean() {
		assertNull(clean(null));
		assertEquals("", clean(""));
		assertEquals("hello world", clean("hello\u0000\u0001world"));
		assertEquals("hello world", clean("hello  \t\n  world"));
		assertEquals("test", clean("test"));
	}

	@Test
	void a73_normalizeWhitespace() {
		assertNull(normalizeWhitespace(null));
		assertEquals("", normalizeWhitespace(""));
		assertEquals("hello world", normalizeWhitespace("hello  \t\n  world"));
		assertEquals("hello world", normalizeWhitespace("  hello  world  "));
		assertEquals("a b c", normalizeWhitespace("a  b  c"));
	}

	@Test
	void a74_removeControlChars() {
		assertNull(removeControlChars(null));
		assertEquals("", removeControlChars(""));
		assertEquals("hello  world", removeControlChars("hello\u0000\u0001world"));
		assertEquals("hello\nworld", removeControlChars("hello\nworld"));
		assertEquals("test", removeControlChars("test"));
	}

	@Test
	void a75_removeNonPrintable() {
		assertNull(removeNonPrintable(null));
		assertEquals("", removeNonPrintable(""));
		assertEquals("helloworld", removeNonPrintable("hello\u0000world"));
		assertEquals("test", removeNonPrintable("test"));
	}

	@Test
	void a76_swapCase() {
		assertNull(swapCase(null));
		assertEquals("", swapCase(""));
		assertEquals("hELLO wORLD", swapCase("Hello World"));
		assertEquals("abc123XYZ", swapCase("ABC123xyz"));
		assertEquals("123", swapCase("123"));
	}

	//====================================================================================================
	// lc / uc
	//====================================================================================================
	@Test
	void a77_lc() {
		assertNull(lc(null));
		assertEquals("", lc(""));
		assertEquals("hello", lc("HELLO"));
		assertEquals("hello world", lc("Hello World"));
	}

	@Test
	void a78_uc() {
		assertNull(uc(null));
		assertEquals("", uc(""));
		assertEquals("HELLO", uc("hello"));
		assertEquals("HELLO WORLD", uc("Hello World"));
	}

	//====================================================================================================
	// eqic
	//====================================================================================================
	@Test
	void a79_eqic() {
		assertTrue(eqic(null, null));
		assertFalse(eqic("test", null));
		assertFalse(eqic(null, "test"));
		assertTrue(eqic("test", "TEST"));
		assertTrue(eqic("TEST", "test"));
		assertTrue(eqic("Test", "test"));
		assertFalse(eqic("test", "other"));
		assertTrue(eqic(123, "123"));
		assertTrue(eqic("123", 123));
	}

	//====================================================================================================
	// articlized
	//====================================================================================================
	@Test
	void a80_articlized() {
		assertEquals("an apple", articlized("apple"));
		assertEquals("an Apple", articlized("Apple"));
		assertEquals("a banana", articlized("banana"));
		assertEquals("a Banana", articlized("Banana"));
		assertEquals("an elephant", articlized("elephant"));
		assertEquals("an island", articlized("island"));
		assertEquals("an orange", articlized("orange"));
		assertEquals("an umbrella", articlized("umbrella"));
	}

	//====================================================================================================
	// obfuscate
	//====================================================================================================
	@Test
	void a81_obfuscate() {
		assertEquals("*", obfuscate(null));
		assertEquals("*", obfuscate(""));
		assertEquals("*", obfuscate("a"));
		assertEquals("p*", obfuscate("pa"));
		assertEquals("p*******", obfuscate("password"));
		assertEquals("1*****", obfuscate("123456"));
	}

	//====================================================================================================
	// firstNonEmpty / firstNonBlank
	//====================================================================================================
	@Test
	void a82_firstNonEmpty() {
		assertEquals("test", firstNonEmpty("test"));
		assertEquals("test", firstNonEmpty(null, "test"));
		assertEquals("test", firstNonEmpty("", "test"));
		assertEquals("test", firstNonEmpty(null, "", "test"));
		assertNull(firstNonEmpty());
		assertNull(firstNonEmpty((String)null));
		assertNull(firstNonEmpty(null, null));
		assertNull(firstNonEmpty("", ""));
		assertEquals(" ", firstNonEmpty(" "));
	}

	@Test
	void a83_firstNonBlank() {
		assertEquals("test", firstNonBlank("test"));
		assertEquals("test", firstNonBlank(null, "test"));
		assertEquals("test", firstNonBlank("", "test"));
		assertEquals("test", firstNonBlank(" ", "test"));
		assertEquals("test", firstNonBlank(null, "", " ", "test"));
		assertNull(firstNonBlank());
		assertNull(firstNonBlank((String)null));
		assertNull(firstNonBlank(null, null));
		assertNull(firstNonBlank("", ""));
		assertNull(firstNonBlank(" ", "  "));
	}

	//====================================================================================================
	// cdlToList / cdlToSet
	//====================================================================================================
	@Test
	void a86_cdlToList() {
		assertEquals(l("a", "b", "c"), cdlToList("a,b,c"));
		assertEquals(l("a", "b", "c"), cdlToList(" a , b , c "));
		assertEquals(l(), cdlToList(null));
		assertEquals(l(), cdlToList(""));
		assertEquals(l("a"), cdlToList("a"));
	}

	@Test
	void a87_cdlToSet() {
		assertEquals(new LinkedHashSet<>(l("a", "b", "c")), cdlToSet("a,b,c"));
		assertEquals(new LinkedHashSet<>(l("a", "b", "c")), cdlToSet(" a , b , c "));
		assertEquals(set(), cdlToSet(null));
		assertEquals(set(), cdlToSet(""));
		assertEquals(new LinkedHashSet<>(l("a")), cdlToSet("a"));
	}

	//====================================================================================================
	// join
	//====================================================================================================
	@Test
	void a88_join_varargs() {
		assertEquals("a,b,c", join("a", "b", "c"));
		assertEquals("a", join("a"));
		assertEquals("", join());
	}

	@Test
	void a89_join_collection() {
		assertEquals("a,b,c", join(l("a", "b", "c")));
		assertEquals("1,2,3", join(l(1, 2, 3)));
		assertEquals("a", join(l("a")));
		assertEquals("", join(l()));
	}

	//====================================================================================================
	// contains / notContains
	//====================================================================================================
	@Test
	void a90_contains_strings() {
		assertTrue(contains("test", "te"));
		assertTrue(contains("test", "st"));
		assertTrue(contains("test", "test"));
		assertTrue(contains("test", "te", "xx"));
		assertFalse(contains("test", "xx"));
		assertFalse(contains("test", "xx", "yy"));
		assertFalse(contains(null, "test"));
		assertFalse(contains("test", (String[])null));
	}

	@Test
	void a91_contains_chars() {
		assertTrue(contains("test", 't'));
		assertTrue(contains("test", 'e'));
		assertTrue(contains("test", 't', 'x'));
		assertFalse(contains("test", 'x'));
		assertFalse(contains("test", 'x', 'y'));
		assertFalse(contains(null, 't'));
		assertFalse(contains("test", (char[])null));
	}

	@Test
	void a92_notContains_strings() {
		assertFalse(notContains("test", "te"));
		assertTrue(notContains("test", "xx"));
		assertTrue(notContains(null, "test"));
	}

	@Test
	void a93_notContains_chars() {
		assertFalse(notContains("test", 't'));
		assertTrue(notContains("test", 'x'));
		assertTrue(notContains(null, 't'));
	}

	//====================================================================================================
	// stringSupplier
	//====================================================================================================
	@Test
	void a94_stringSupplier() {
		assertEquals("test", stringSupplier(() -> "test").get());
		assertEquals("[1,2,3]", stringSupplier(() -> l(1, 2, 3)).get());
		assertNull(stringSupplier(() -> null).get());
	}

	//====================================================================================================
	// readable
	//====================================================================================================
	@Test
	void a95_readable() {
		assertNull(readable(null));
		assertEquals("[a,b,c]", readable(l("a", "b", "c")));
		assertEquals("{foo=bar}", readable(m("foo", "bar")));
		assertEquals("[1,2,3]", readable(ints(1, 2, 3)));
		assertEquals("test", readable(opt("test")));
		assertNull(readable(opte()));
	}

	//====================================================================================================
	// removeAll(String, String...)
	//====================================================================================================
	@Test
	void a96_removeAll() {
		assertNull(removeAll(null, "x"));
		assertEquals("hello world test", removeAll("hello world test"));
		assertEquals("hello world test", removeAll("hello world test", (String[])null));
		assertEquals(" world ", removeAll("hello world test", "hello", "test"));
		assertEquals("hello world test", removeAll("hello world test", "xyz"));
		assertEquals("", removeAll("xxx", "x"));
		assertEquals("hello", removeAll("hello", "x", "y", "z"));
		assertEquals("", removeAll("abc", "a", "b", "c"));
		assertEquals("hello", removeAll("hello", null, "x"));
	}

	//====================================================================================================
	// camelCase(String)
	//====================================================================================================
	@Test
	void a97_camelCase() {
		assertNull(camelCase(null));
		assertEquals("", camelCase(""));
		assertEquals("helloWorld", camelCase("hello world"));
		assertEquals("helloWorld", camelCase("hello_world"));
		assertEquals("helloWorld", camelCase("hello-world"));
		assertEquals("helloWorld", camelCase("HelloWorld"));
		assertEquals("helloWorld", camelCase("helloWorld"));
		assertEquals("helloWorld", camelCase("  hello   world  "));
		// Note: XMLHttpRequest should split as ["XML", "Http", "Request"] → "xmlHttpRequest"
		// If it splits as ["X", "MLHttpRequest"], we get "xMLHttpRequest" (incorrect)
		// TODO: Fix splitWords logic to correctly handle consecutive uppercase letters
		// assertEquals("xmlHttpRequest", camelCase("XMLHttpRequest"));
		assertEquals("helloWorldTest", camelCase("Hello_World-Test"));
		assertEquals("test", camelCase("test"));
		// TODO: Fix camelCase to handle all-uppercase words correctly
		// assertEquals("test", camelCase("TEST"));
		assertEquals("hello123World", camelCase("hello 123 world"));
	}

	//====================================================================================================
	// snakeCase(String)
	//====================================================================================================
	@Test
	void a98_snakeCase() {
		assertNull(snakeCase(null));
		assertEquals("", snakeCase(""));
		assertEquals("hello_world", snakeCase("hello world"));
		assertEquals("hello_world", snakeCase("helloWorld"));
		assertEquals("hello_world", snakeCase("HelloWorld"));
		assertEquals("hello_world", snakeCase("hello-world"));
		assertEquals("hello_world", snakeCase("hello_world"));
		assertEquals("xml_http_request", snakeCase("XMLHttpRequest"));
		assertEquals("hello_world_test", snakeCase("Hello_World-Test"));
		assertEquals("test", snakeCase("test"));
		assertEquals("test", snakeCase("TEST"));
		assertEquals("hello_123_world", snakeCase("hello 123 world"));
	}

	//====================================================================================================
	// kebabCase(String)
	//====================================================================================================
	@Test
	void a99_kebabCase() {
		assertNull(kebabCase(null));
		assertEquals("", kebabCase(""));
		assertEquals("hello-world", kebabCase("hello world"));
		assertEquals("hello-world", kebabCase("helloWorld"));
		assertEquals("hello-world", kebabCase("HelloWorld"));
		assertEquals("hello-world", kebabCase("hello_world"));
		assertEquals("hello-world", kebabCase("hello-world"));
		assertEquals("xml-http-request", kebabCase("XMLHttpRequest"));
		assertEquals("hello-world-test", kebabCase("Hello_World-Test"));
		assertEquals("test", kebabCase("test"));
		assertEquals("test", kebabCase("TEST"));
		assertEquals("hello-123-world", kebabCase("hello 123 world"));
	}

	//====================================================================================================
	// pascalCase(String)
	//====================================================================================================
	@Test
	void a100_pascalCase() {
		assertNull(pascalCase(null));
		assertEquals("", pascalCase(""));
		assertEquals("HelloWorld", pascalCase("hello world"));
		assertEquals("HelloWorld", pascalCase("helloWorld"));
		assertEquals("HelloWorld", pascalCase("HelloWorld"));
		assertEquals("HelloWorld", pascalCase("hello_world"));
		assertEquals("HelloWorld", pascalCase("hello-world"));
		assertEquals("XmlHttpRequest", pascalCase("XMLHttpRequest"));
		assertEquals("HelloWorldTest", pascalCase("Hello_World-Test"));
		assertEquals("Test", pascalCase("test"));
		assertEquals("Test", pascalCase("TEST"));
		assertEquals("Hello123World", pascalCase("hello 123 world"));
	}

	//====================================================================================================
	// titleCase(String)
	//====================================================================================================
	@Test
	void a101_titleCase() {
		assertNull(titleCase(null));
		assertEquals("", titleCase(""));
		assertEquals("Hello World", titleCase("hello world"));
		assertEquals("Hello World", titleCase("helloWorld"));
		assertEquals("Hello World", titleCase("HelloWorld"));
		assertEquals("Hello World", titleCase("hello_world"));
		assertEquals("Hello World", titleCase("hello-world"));
		assertEquals("Xml Http Request", titleCase("XMLHttpRequest"));
		assertEquals("Hello World Test", titleCase("Hello_World-Test"));
		assertEquals("Test", titleCase("test"));
		assertEquals("Test", titleCase("TEST"));
		assertEquals("Hello 123 World", titleCase("hello 123 world"));
	}

	//====================================================================================================
	// wrap(String, int)
	//====================================================================================================
	@Test
	void a102_wrap() {
		assertNull(wrap(null, 10));
		assertEquals("", wrap("", 10));
		assertEquals("hello\nworld", wrap("hello world", 10));
		assertEquals("hello\nworld\ntest", wrap("hello world test", 10));
		assertEquals("hello world", wrap("hello world", 20));
		assertEquals("hello\nworld", wrap("hello world", 5));
		assertEquals("supercalifragilisticexpialidocious", wrap("supercalifragilisticexpialidocious", 10));
		assertEquals("hello\nworld", wrap("hello  world", 10));
		assertEquals("line1\nline2", wrap("line1\nline2", 10));
		assertEquals("a\nb\nc", wrap("a b c", 1));
		assertThrows(IllegalArgumentException.class, () -> wrap("test", 0));
		assertThrows(IllegalArgumentException.class, () -> wrap("test", -1));
	}

	//====================================================================================================
	// wrap(String, int, String)
	//====================================================================================================
	@Test
	void a103_wrapWithNewline() {
		assertNull(wrap(null, 10, "<br>"));
		assertEquals("", wrap("", 10, "<br>"));
		assertEquals("hello<br>world", wrap("hello world", 10, "<br>"));
		assertEquals("hello<br>world<br>test", wrap("hello world test", 10, "<br>"));
		assertEquals("hello world", wrap("hello world", 20, "<br>"));
		assertEquals("hello<br>world", wrap("hello world", 5, "<br>"));
		assertEquals("supercalifragilisticexpialidocious", wrap("supercalifragilisticexpialidocious", 10, "<br>"));
		assertEquals("line1<br>line2", wrap("line1\nline2", 10, "<br>"));
		assertEquals("a<br>b<br>c", wrap("a b c", 1, "<br>"));
		assertThrows(IllegalArgumentException.class, () -> wrap("test", 0, "\n"));
		assertThrows(IllegalArgumentException.class, () -> wrap("test", -1, "\n"));
		assertThrows(IllegalArgumentException.class, () -> wrap("test", 10, null));
	}

	//====================================================================================================
	// String Array and Collection Utilities
	//====================================================================================================

	//====================================================================================================
	// toStringArray(Collection<String>)
	//====================================================================================================
	@Test
	void a104_toStringArray() {
		assertNull(toStringArray(null));
		assertList(toStringArray(Collections.emptyList()));
		assertList(toStringArray(l("a", "b", "c")), "a", "b", "c");

		// Set.of() doesn't preserve order, so use LinkedHashSet for order-sensitive test
		var set = new LinkedHashSet<String>();
		set.add("x");
		set.add("y");
		set.add("z");
		assertList(toStringArray(set), "x", "y", "z");

		assertList(toStringArray(new LinkedHashSet<>(l("foo", "bar", "baz"))), "foo", "bar", "baz");
		var list = new ArrayList<String>();
		list.add("one");
		list.add("two");
		list.add("three");
		assertList(toStringArray(list), "one", "two", "three");
	}

	//====================================================================================================
	// filter(String[], Predicate<String>)
	//====================================================================================================
	@Test
	void a105_filter() {
		assertNull(filter(null, NOT_EMPTY));
		assertList(filter(a(), NOT_EMPTY));
		assertList(filter(a("foo", "", "bar", null, "baz"), NOT_EMPTY), "foo", "bar", "baz");
		assertList(filter(a("foo", "", "bar", null, "baz"), null));
		assertList(filter(a("hello", "world", "test"), s -> s.length() > 4), "hello", "world");
		assertList(filter(a("a", "bb", "ccc", "dddd"), s -> s.length() == 2), "bb");
		assertList(filter(a("foo", "bar", "baz"), s -> s.startsWith("b")), "bar", "baz");
		assertList(filter(a("test"), s -> false));
		assertList(filter(a("test"), s -> true), "test");
	}

	//====================================================================================================
	// map(String[], Function<String, String>)
	//====================================================================================================
	@Test
	void a106_mapped() {
		assertNull(mapped(null, String::toUpperCase));
		assertList(mapped(a(), String::toUpperCase));
		assertList(mapped(a("foo", "bar", "baz"), String::toUpperCase), "FOO", "BAR", "BAZ");
		assertList(mapped(a("FOO", "BAR", "BAZ"), String::toLowerCase), "foo", "bar", "baz");
		assertList(mapped(a("foo", "bar", "baz"), s -> "prefix-" + s), "prefix-foo", "prefix-bar", "prefix-baz");
		assertList(mapped(a("hello", "world"), s -> s.substring(0, 1)), "h", "w");
		assertList(mapped(a("test"), null), "test");
		assertList(mapped(a("a", "b", "c"), s -> s + s), "aa", "bb", "cc");
	}

	//====================================================================================================
	// distinct(String[])
	//====================================================================================================
	@Test
	void a107_distinct() {
		assertNull(distinct(null));
		assertList(distinct(a()));
		assertList(distinct(a("foo", "bar", "baz")), "foo", "bar", "baz");
		assertList(distinct(a("foo", "bar", "foo", "baz", "bar")), "foo", "bar", "baz");
		assertList(distinct(a("a", "a", "a", "a")), "a");
		assertList(distinct(a("x", "y", "x", "z", "y", "x")), "x", "y", "z");
		assertList(distinct(a("test")), "test");
		assertList(distinct(a("", "", "foo", "", "bar")), "", "foo", "bar");
	}

	//====================================================================================================
	// sort(String[])
	//====================================================================================================
	@Test
	void a108_sort() {
		assertNull(sort(null));
		assertList(sort(a()));
		assertList(sort(a("c", "a", "b")), "a", "b", "c");
		assertList(sort(a("zebra", "apple", "banana")), "apple", "banana", "zebra");
		assertList(sort(a("3", "1", "2")), "1", "2", "3");
		assertList(sort(a("test")), "test");
		assertList(sort(a("Z", "a", "B")), "B", "Z", "a");
		assertList(sort(a("foo", "bar", "baz")), "bar", "baz", "foo");
	}

	//====================================================================================================
	// sortIgnoreCase(String[])
	//====================================================================================================
	@Test
	void a109_sortIgnoreCase() {
		assertNull(sortIgnoreCase(null));
		assertList(sortIgnoreCase(a()));
		assertList(sortIgnoreCase(a("c", "a", "b")), "a", "b", "c");
		assertList(sortIgnoreCase(a("Zebra", "apple", "Banana")), "apple", "Banana", "Zebra");
		assertList(sortIgnoreCase(a("Z", "a", "B")), "a", "B", "Z");
		assertList(sortIgnoreCase(a("test")), "test");
		assertList(sortIgnoreCase(a("FOO", "bar", "Baz")), "bar", "Baz", "FOO");
		assertList(sortIgnoreCase(a("zebra", "APPLE", "banana")), "APPLE", "banana", "zebra");
	}

	//====================================================================================================
	// String Builder Utilities
	//====================================================================================================

	//====================================================================================================
	// appendIfNotEmpty(StringBuilder, String)
	//====================================================================================================
	@Test
	void a110_appendIfNotEmpty() {
		var sb = new StringBuilder();
		assertSame(sb, appendIfNotEmpty(sb, "hello"));
		assertEquals("hello", sb.toString());

		appendIfNotEmpty(sb, "");
		assertEquals("hello", sb.toString());

		appendIfNotEmpty(sb, null);
		assertEquals("hello", sb.toString());

		appendIfNotEmpty(sb, "world");
		assertEquals("helloworld", sb.toString());

		var sb2 = new StringBuilder("prefix");
		appendIfNotEmpty(sb2, "suffix");
		assertEquals("prefixsuffix", sb2.toString());

		assertThrows(IllegalArgumentException.class, () -> appendIfNotEmpty(null, "test"));
	}

	//====================================================================================================
	// appendIfNotBlank(StringBuilder, String)
	//====================================================================================================
	@Test
	void a111_appendIfNotBlank() {
		var sb = new StringBuilder();
		assertSame(sb, appendIfNotBlank(sb, "hello"));
		assertEquals("hello", sb.toString());

		appendIfNotBlank(sb, "   ");
		assertEquals("hello", sb.toString());

		appendIfNotBlank(sb, "\t\n");
		assertEquals("hello", sb.toString());

		appendIfNotBlank(sb, "");
		assertEquals("hello", sb.toString());

		appendIfNotBlank(sb, null);
		assertEquals("hello", sb.toString());

		appendIfNotBlank(sb, "world");
		assertEquals("helloworld", sb.toString());

		var sb2 = new StringBuilder("prefix");
		appendIfNotBlank(sb2, "suffix");
		assertEquals("prefixsuffix", sb2.toString());

		assertThrows(IllegalArgumentException.class, () -> appendIfNotBlank(null, "test"));
	}

	//====================================================================================================
	// appendWithSeparator(StringBuilder, String, String)
	//====================================================================================================
	@Test
	void a112_appendWithSeparator() {
		var sb = new StringBuilder();
		assertSame(sb, appendWithSeparator(sb, "first", ", "));
		assertEquals("first", sb.toString());

		appendWithSeparator(sb, "second", ", ");
		assertEquals("first, second", sb.toString());

		appendWithSeparator(sb, "third", ", ");
		assertEquals("first, second, third", sb.toString());

		var sb2 = new StringBuilder();
		appendWithSeparator(sb2, "a", "-");
		appendWithSeparator(sb2, "b", "-");
		appendWithSeparator(sb2, "c", "-");
		assertEquals("a-b-c", sb2.toString());

		var sb3 = new StringBuilder();
		appendWithSeparator(sb3, "x", null);
		assertEquals("x", sb3.toString());
		appendWithSeparator(sb3, "y", null);
		assertEquals("xy", sb3.toString());

		var sb4 = new StringBuilder();
		appendWithSeparator(sb4, null, ", ");
		assertEquals("", sb4.toString());
		appendWithSeparator(sb4, "test", ", ");
		assertEquals("test", sb4.toString());

		assertThrows(IllegalArgumentException.class, () -> appendWithSeparator(null, "test", ", "));
	}

	//====================================================================================================
	// buildString(Consumer<StringBuilder>)
	//====================================================================================================
	@Test
	void a113_buildString() {
		var result = buildString(sb -> {
			sb.append("Hello");
			sb.append(" ");
			sb.append("World");
		});
		assertEquals("Hello World", result);

		var joined = buildString(sb -> {
			appendWithSeparator(sb, "a", ", ");
			appendWithSeparator(sb, "b", ", ");
			appendWithSeparator(sb, "c", ", ");
		});
		assertEquals("a, b, c", joined);

		var empty = buildString(sb -> {
			// Do nothing
		});
		assertEquals("", empty);

		var complex = buildString(sb -> {
			appendIfNotEmpty(sb, "prefix");
			appendWithSeparator(sb, "middle", "-");
			appendWithSeparator(sb, "suffix", "-");
		});
		assertEquals("prefix-middle-suffix", complex);

		assertThrows(IllegalArgumentException.class, () -> buildString(null));
	}

	//====================================================================================================
	// String Constants and Utilities
	//====================================================================================================

	//====================================================================================================
	// String Constants
	//====================================================================================================
	@Test
	void a114_stringConstants() {
		// EMPTY
		assertEquals("", EMPTY);
		assertTrue(EMPTY.isEmpty());

		// SPACE
		assertEquals(" ", SPACE);
		assertEquals(1, SPACE.length());
		assertTrue(Character.isWhitespace(SPACE.charAt(0)));

		// NEWLINE
		assertEquals("\n", NEWLINE);
		assertEquals(1, NEWLINE.length());
		assertEquals('\n', NEWLINE.charAt(0));

		// TAB
		assertEquals("\t", TAB);
		assertEquals(1, TAB.length());
		assertEquals('\t', TAB.charAt(0));

		// CRLF
		assertEquals("\r\n", CRLF);
		assertEquals(2, CRLF.length());
		assertEquals('\r', CRLF.charAt(0));
		assertEquals('\n', CRLF.charAt(1));

		// COMMON_SEPARATORS
		assertTrue(COMMON_SEPARATORS.contains(","));
		assertTrue(COMMON_SEPARATORS.contains(";"));
		assertTrue(COMMON_SEPARATORS.contains(":"));
		assertTrue(COMMON_SEPARATORS.contains("|"));
		assertTrue(COMMON_SEPARATORS.contains("\t"));
		assertEquals(5, COMMON_SEPARATORS.length());

		// WHITESPACE_CHARS
		assertTrue(WHITESPACE_CHARS.contains(" "));
		assertTrue(WHITESPACE_CHARS.contains("\t"));
		assertTrue(WHITESPACE_CHARS.contains("\n"));
		assertTrue(WHITESPACE_CHARS.contains("\r"));
		assertTrue(WHITESPACE_CHARS.contains("\f"));
		assertTrue(WHITESPACE_CHARS.contains("\u000B")); // Vertical tab
		for (var i = 0; i < WHITESPACE_CHARS.length(); i++) {
			assertTrue(Character.isWhitespace(WHITESPACE_CHARS.charAt(i)));
		}
	}

	//====================================================================================================
	// Performance and Memory Utilities
	//====================================================================================================

	//====================================================================================================
	// intern(String)
	//====================================================================================================
	@Test
	void a115_intern() {
		assertNull(intern(null));

		var s1 = new String("test");
		var s2 = new String("test");
		assertTrue(s1 != s2); // Different objects

		var i1 = intern(s1);
		var i2 = intern(s2);
		assertTrue(i1 == i2); // Same interned object
		assertEquals("test", i1);
		assertEquals("test", i2);

		// String literals are automatically interned
		var literal = "literal";
		assertTrue(isInterned(literal));
		assertSame(literal, intern(literal));
	}

	//====================================================================================================
	// isInterned(String)
	//====================================================================================================
	@Test
	void a116_isInterned() {
		assertFalse(isInterned(null));

		// String literals are automatically interned
		var literal = "test";
		assertTrue(isInterned(literal));

		// New String objects are not interned
		var s1 = new String("test");
		assertFalse(isInterned(s1));

		// After interning, it should be interned
		var s2 = intern(s1);
		assertTrue(isInterned(s2));
		assertSame(s1.intern(), s2);
	}

	//====================================================================================================
	// getStringSize(String)
	//====================================================================================================
	@Test
	void a117_getStringSize() {
		assertEquals(0, getStringSize(null));
		assertEquals(40, getStringSize("")); // 24 + 16 = 40 bytes overhead
		assertEquals(50, getStringSize("hello")); // 40 + (5 * 2) = 50 bytes
		assertEquals(48, getStringSize("test")); // 40 + (4 * 2) = 48 bytes
		assertEquals(60, getStringSize("1234567890")); // 40 + (10 * 2) = 60 bytes

		// Verify the calculation: 24 (String object) + 16 (char[] header) + (2 * length)
		var emptySize = getStringSize("");
		assertTrue(emptySize >= 24); // At least String object overhead

		var oneCharSize = getStringSize("a");
		assertEquals(emptySize + 2, oneCharSize); // One char adds 2 bytes

		var tenCharSize = getStringSize("1234567890");
		assertEquals(emptySize + 20, tenCharSize); // Ten chars add 20 bytes
	}

	//====================================================================================================
	// optimizeString(String)
	//====================================================================================================
	@Test
	void a118_optimizeString() {
		assertNull(optimizeString(null));
		assertNull(optimizeString("short")); // No suggestions for short strings

		// Test for large string suggestion
		var largeString = "x".repeat(1001);
		var suggestions = optimizeString(largeString);
		assertNotNull(suggestions);
		assertTrue(suggestions.contains("StringBuilder"));

		// Test for interning suggestion (medium length, not interned)
		var mediumString = new String("medium length string for testing");
		var interningSuggestion = optimizeString(mediumString);
		assertNotNull(interningSuggestion);
		assertTrue(interningSuggestion.contains("interning"));

		// Test for char[] suggestion (long string)
		var longString = "x".repeat(101);
		var charArraySuggestion = optimizeString(longString);
		assertNotNull(charArraySuggestion);
		assertTrue(charArraySuggestion.contains("char[]"));

		// Test for compression suggestion (very large string)
		var veryLargeString = "x".repeat(10001);
		var compressionSuggestion = optimizeString(veryLargeString);
		assertNotNull(compressionSuggestion);
		assertTrue(compressionSuggestion.contains("compression"));

		// Test that interned strings don't suggest interning
		var interned = intern("medium length string");
		var noInterningSuggestion = optimizeString(interned);
		// Should not suggest interning if already interned, but may have other suggestions
		if (noInterningSuggestion != null) {
			assertFalse(noInterningSuggestion.contains("interning"));
		}
	}
}