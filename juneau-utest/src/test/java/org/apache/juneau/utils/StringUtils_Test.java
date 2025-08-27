// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.junit.jupiter.api.*;

class StringUtils_Test extends SimpleTestBase {

	//====================================================================================================
	// isNumeric(String,Class)
	// parseNumber(String,Class)
	//====================================================================================================
	@Test void a01_testParser() {

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

		assertThrows(NumberFormatException.class, ()->parseNumber("x", Number.class));
		assertThrows(NumberFormatException.class, ()->parseNumber("x", null));
		assertThrowsWithMessage(NumberFormatException.class, "Unsupported Number type", ()->parseNumber("x", BadNumber.class));
	}

	@SuppressWarnings("serial")
	private abstract static class BadNumber extends Number {}

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test void a02_numberRanges() {
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
		assertThrows(NumberFormatException.class, ()->parseNumber("214748364x", Number.class));

		s = String.valueOf("2147483640x");
		assertFalse(isNumeric(s));
		assertThrows(NumberFormatException.class, ()->parseNumber("2147483640x", Long.class));
	}

	//====================================================================================================
	// testReplaceVars
	//====================================================================================================
	@Test void a03_replaceVars() throws Exception {
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
	@Test void a04_isFloat() {
		var valid = a(
			"+1.0",
			"-1.0",
			".0",
			"NaN",
			"Infinity",
			"1e1",
			"-1e-1",
			"+1e+1",
			"-1.1e-1",
			"+1.1e+1",
			"1.1f",
			"1.1F",
			"1.1d",
			"1.1D",
			"0x1.fffffffffffffp1023",
			"0x1.FFFFFFFFFFFFFP1023"
		);
		for (var s : valid)
			assertTrue(isFloat(s));

		var invalid = a(
			null,
			"",
			"a",
			"+",
			"-",
			".",
			"a",
			"+a",
			"11a"
		);
		for (var s : invalid)
			assertFalse(isFloat(s));
	}

	//====================================================================================================
	// isDecimal(String)
	//====================================================================================================
	@Test void a05_isDecimal() {
		var valid = a(
			"+1",
			"-1",
			"0x123",
			"0X123",
			"0xdef",
			"0XDEF",
			"#def",
			"#DEF",
			"0123"
		);
		for (var s : valid)
			assertTrue(isDecimal(s));

		var invalid = a(
			null,
			"",
			"a",
			"+",
			"-",
			".",
			"0xdeg",
			"0XDEG",
			"#deg",
			"#DEG",
			"0128",
			"012A"
		);
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
	@Test void a01_join() {
		assertNull(Utils.join((Object[])null, ","));
		assertEquals("1", Utils.join(new Object[]{1}, ","));
		assertEquals("1,2", Utils.join(new Object[]{1,2}, ","));

		assertNull(Utils.join((Collection<?>)null, ","));
		assertEquals("1", Utils.join(Arrays.asList(new Integer[]{1}), ","));
		assertEquals("1,2", Utils.join(Arrays.asList(new Integer[]{1,2}), ","));

		assertNull(Utils.join((Object[])null, ','));
		assertEquals("x,y,z", Utils.join(new Object[]{"x,y","z"}, ','));

		assertNull(Utils.join((int[])null, ','));
		assertEquals("1", Utils.join(new int[]{1}, ','));
		assertEquals("1,2", Utils.join(new int[]{1,2}, ','));

		assertNull(Utils.join((Collection<?>)null, ','));
		assertEquals("1", Utils.join(Arrays.asList(new Integer[]{1}), ','));
		assertEquals("1,2", Utils.join(Arrays.asList(new Integer[]{1,2}), ','));

		assertNull(StringUtils.joine((List<?>)null, ','));
		assertEquals("x\\,y,z", StringUtils.joine(Arrays.asList(new String[]{"x,y","z"}), ','));
	}

	//====================================================================================================
	// split(String,char)
	//====================================================================================================
	@Test void a07_split() {
		assertNull(Utils.splita((String)null));
		assertArray(Utils.splita(""));
		assertArray(Utils.splita("1"), "1");
		assertArray(Utils.splita("1,2"), "1", "2");
		assertArray(Utils.splita("1\\,2"), ">1,2");
		assertArray(Utils.splita("1\\\\,2"), "1\\", "2");
		assertArray(Utils.splita("1\\\\\\,2"), ">1\\,2");
		assertArray(Utils.splita("1,2\\"), "1", "2\\");
		assertArray(Utils.splita("1,2\\\\"), "1", "2\\");
		assertArray(Utils.splita("1,2\\,"), "1", "2,");
		assertArray(Utils.splita("1,2\\\\,"), "1", "2\\", "");
	}

	@Test void a08_split2() {
		assertEmpty(split2test(null));
		assertString("[]", split2test(""));
		assertString("[1]", split2test("1"));
		assertString("[1,2]", split2test("1,2"));
		assertList(split2test("1\\,2"), ">1,2");
		assertList(split2test("1\\\\,2"), "1\\", "2");
		assertList(split2test("1\\\\\\,2"), ">1\\,2");
		assertList(split2test("1,2\\"), "1", "2\\");
		assertList(split2test("1,2\\\\"), "1", "2\\");
		assertList(split2test("1,2\\,"), "1", "2,");
		assertList(split2test("1,2\\\\,"), "1", "2\\", "");
	}

	private List<String> split2test(String s) {
		var l = new ArrayList<String>();
		Utils.split(s, l::add);
		return l;
	}

	//====================================================================================================
	// split(String,char,int)
	//====================================================================================================
	@Test void a09_splitWithLimit() {
		assertString("[boo,and,foo]", Utils.splita("boo:and:foo", ':', 10));
		assertString("[boo,and:foo]", Utils.splita("boo:and:foo", ':', 2));
		assertString("[boo:and:foo]", Utils.splita("boo:and:foo", ':', 1));
		assertString("[boo:and:foo]", Utils.splita("boo:and:foo", ':', 0));
		assertString("[boo:and:foo]", Utils.splita("boo:and:foo", ':', -1));
		assertString("[boo,and,foo]", Utils.splita("boo : and : foo", ':', 10));
		assertString("[boo,and : foo]", Utils.splita("boo : and : foo", ':', 2));
	}

	//====================================================================================================
	// nullIfEmpty(String)
	//====================================================================================================
	@Test void a10_nullIfEmpty() {
		assertNull(nullIfEmpty(null));
		assertNull(nullIfEmpty(""));
		assertNotNull(nullIfEmpty("x"));
	}

	//====================================================================================================
	// unescapeChars(String,char[],char)
	//====================================================================================================
	@Test void a11_unescapeChars() {
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
	@Test void a12_decodeHex() {
		assertNull(decodeHex(null));
		assertEquals("19azAZ", decodeHex("19azAZ"));
		assertEquals("[0][1][ffff]", decodeHex("\u0000\u0001\uFFFF"));
	}

	//====================================================================================================
	// startsWith(String,char)
	//====================================================================================================
	@Test void a13_startsWith() {
		assertFalse(startsWith(null, 'a'));
		assertFalse(startsWith("", 'a'));
		assertTrue(startsWith("a", 'a'));
		assertTrue(startsWith("ab", 'a'));
	}

	//====================================================================================================
	// endsWith(String,char)
	//====================================================================================================
	@Test void a14_endsWith() {
		assertFalse(endsWith(null, 'a'));
		assertFalse(endsWith("", 'a'));
		assertTrue(endsWith("a", 'a'));
		assertTrue(endsWith("ba", 'a'));
	}

	//====================================================================================================
	// base64EncodeToString(String)
	// base64DecodeToString(String)
	//====================================================================================================
	@Test void a15_base64EncodeToString() {
		assertNull(base64DecodeToString(base64EncodeToString(null)));
		assertEquals("", base64DecodeToString(base64EncodeToString("")));
		assertEquals("foobar", base64DecodeToString(base64EncodeToString("foobar")));
		assertEquals("\u0000\uffff", base64DecodeToString(base64EncodeToString("\u0000\uffff")));
		assertThrowsWithMessage(IllegalArgumentException.class, "Invalid BASE64 string length.  Must be multiple of 4.", ()->base64Decode("a"));
		assertThrows(IllegalArgumentException.class, ()->base64Decode("aaa"));
	}

	//====================================================================================================
	// generateUUID(String)
	//====================================================================================================
	@Test void a16_generateUUID() {
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
	@Test void a17_trim() {
		assertNull(trim(null));
		assertEquals("", trim(""));
		assertEquals("", trim("  "));
	}

	//====================================================================================================
	// parseISO8601Date(String)
	//====================================================================================================
	@Test void a18_parseISO8601Date() throws Exception {
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
	@Test void a19_splitMap() {
		assertString("{a=1}", Utils.splitMap("a=1", true));
		assertString("{a=1,b=2}", Utils.splitMap("a=1,b=2", true));
		assertString("{a=1,b=2}", Utils.splitMap(" a = 1 , b = 2 ", true));
		assertString("{ a = 1 , b = 2 }", Utils.splitMap(" a = 1 , b = 2 ", false));
		assertString("{a=}", Utils.splitMap("a", true));
		assertString("{a=,b=}", Utils.splitMap("a,b", true));
		assertString("{a=1,b=}", Utils.splitMap("a=1,b", true));
		assertString("{a=,b=1}", Utils.splitMap("a,b=1", true));
		assertString("{a==1}", Utils.splitMap("a\\==1", true));
		assertString("{a\\=1}", Utils.splitMap("a\\\\=1", true));
	}

	//====================================================================================================
	// isAbsoluteUri(String)
	//====================================================================================================
	@Test void a10_isAbsoluteUri() {
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
	@Test void a21_getAuthorityUri() {
		assertEquals("http://foo", getAuthorityUri("http://foo"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123/"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123/bar"));
	}

	//====================================================================================================
	// splitQuoted(String)
	//====================================================================================================
	@Test void a22_splitQuoted() {
		assertNull(Utils.splitQuoted(null));
		assertArray(Utils.splitQuoted(""));
		assertArray(Utils.splitQuoted(" \t "));
		assertArray(Utils.splitQuoted("foo"), "foo");
		assertArray(Utils.splitQuoted("foo  bar baz"), "foo", "bar", "baz");
		assertArray(Utils.splitQuoted("'foo'"), "foo");
		assertArray(Utils.splitQuoted(" ' foo ' "), " foo ");
		assertArray(Utils.splitQuoted("'foo' 'bar'"), "foo", "bar");
		assertArray(Utils.splitQuoted("\"foo\""), "foo");
		assertArray(Utils.splitQuoted(" \" foo \" "), " foo ");
		assertArray(Utils.splitQuoted("\"foo\" \"bar\""), "foo", "bar");
		assertArray(Utils.splitQuoted("'foo\\'bar'"), "foo'bar");
		assertArray(Utils.splitQuoted("'foo\\\"bar'"), "foo\"bar");
		assertArray(Utils.splitQuoted("'\\'foo\\'bar\\''"), "'foo'bar'");
		assertArray(Utils.splitQuoted("'\\\"foo\\\"bar\\\"'"), "\"foo\"bar\"");
		assertArray(Utils.splitQuoted("'\\'foo\\''"), "'foo'");
		assertArray(Utils.splitQuoted("\"\\\"foo\\\"\""), "\"foo\"");
		assertArray(Utils.splitQuoted("'\"foo\"'"), "\"foo\"");
		assertArray(Utils.splitQuoted("\"'foo'\""), "'foo'");
	}

	//====================================================================================================
	// firstNonWhitespaceChar(String)
	//====================================================================================================
	@Test void a23_firstNonWhitespaceChar() {
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
	@Test void a24_lastNonWhitespaceChar() {
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
	@Test void a25_isJsonObject() {
		assertTrue(isJsonObject("{foo:'bar'}", true));
		assertTrue(isJsonObject(" { foo:'bar' } ", true));
		assertFalse(isJsonObject(" { foo:'bar'  ", true));
		assertFalse(isJsonObject("  foo:'bar' } ", true));
		assertTrue(isJsonObject("/*foo*/ { foo:'bar' } /*foo*/", true));
	}

	//====================================================================================================
	// isJsonArray(Object)
	//====================================================================================================
	@Test void a26_isJsonArray() {
		assertTrue(isJsonArray("[123,'bar']", true));
		assertTrue(isJsonArray(" [ 123,'bar' ] ", true));
		assertFalse(isJsonArray(" [ 123,'bar'  ", true));
		assertFalse(isJsonArray("  123,'bar' ] ", true));
		assertTrue(isJsonArray("/*foo*/ [ 123,'bar' ] /*foo*/", true));
	}

	//====================================================================================================
	// addLineNumbers(String)
	//====================================================================================================
	@Test void a27_addLineNumbers() {
		assertNull(getNumberedLines(null));
		assertEquals("1: \n", getNumberedLines(""));
		assertEquals("1: foo\n", getNumberedLines("foo"));
		assertEquals("1: foo\n2: bar\n", getNumberedLines("foo\nbar"));
	}

	//====================================================================================================
	// compare(String,String)
	//====================================================================================================
	@Test void a28_compare() {
		assertTrue(compare("a","b") < 0);
		assertTrue(compare("b","a") > 0);
		assertTrue(compare(null,"b") < 0);
		assertTrue(compare("b",null) > 0);
		assertEquals(0, compare(null,null));
	}

	//====================================================================================================
	// matchPattern(String)
	//====================================================================================================
	@Test void a29_getMatchPattern() {
		assertTrue(Utils.getMatchPattern3("a").matcher("a").matches());
		assertTrue(Utils.getMatchPattern3("*a*").matcher("aaa").matches());
		assertFalse(Utils.getMatchPattern3("*b*").matcher("aaa").matches());
	}

	//====================================================================================================
	// getDuration(String)
	//====================================================================================================
	@Test void a30_getDuration() {
		assertEquals(-1, getDuration(null));
		assertEquals(-1, getDuration(""));
		assertEquals(-1, getDuration(" "));
		assertEquals(1, getDuration("1"));
		assertEquals(10, getDuration("10"));
		assertEquals(10, getDuration("10"));

		long
			s = 1000,
			m = s * 60,
			h = m * 60,
			d = h * 24,
			w = d * 7;

		assertEquals(10*s, getDuration("10s"));
		assertEquals(10*s, getDuration("10 s"));
		assertEquals(10*s, getDuration("  10  s  "));
		assertEquals(10*s, getDuration("10sec"));
		assertEquals(10*s, getDuration("10 sec"));
		assertEquals(10*s, getDuration("  10  sec  "));
		assertEquals(10*s, getDuration("10seconds"));
		assertEquals(10*s, getDuration("10 seconds"));
		assertEquals(10*s, getDuration("  10  seconds  "));
		assertEquals(10*s, getDuration("10S"));
		assertEquals(10*s, getDuration("10 S"));
		assertEquals(10*s, getDuration("  10  S  "));

		assertEquals(10*m, getDuration("10m"));
		assertEquals(10*m, getDuration("10 m"));
		assertEquals(10*m, getDuration("  10  m  "));
		assertEquals(10*m, getDuration("10min"));
		assertEquals(10*m, getDuration("10 min"));
		assertEquals(10*m, getDuration("  10  min  "));
		assertEquals(10*m, getDuration("10minutes"));
		assertEquals(10*m, getDuration("10 minutes"));
		assertEquals(10*m, getDuration("  10  minutes  "));
		assertEquals(10*m, getDuration("10M"));
		assertEquals(10*m, getDuration("10 M"));
		assertEquals(10*m, getDuration("  10  M  "));

		assertEquals(10*h, getDuration("10h"));
		assertEquals(10*h, getDuration("10 h"));
		assertEquals(10*h, getDuration("  10  h  "));
		assertEquals(10*h, getDuration("10hour"));
		assertEquals(10*h, getDuration("10 hour"));
		assertEquals(10*h, getDuration("  10  hour  "));
		assertEquals(10*h, getDuration("10hours"));
		assertEquals(10*h, getDuration("10 hours"));
		assertEquals(10*h, getDuration("  10  hours  "));
		assertEquals(10*h, getDuration("10H"));
		assertEquals(10*h, getDuration("10 H"));
		assertEquals(10*h, getDuration("  10  H  "));

		assertEquals(10*d, getDuration("10d"));
		assertEquals(10*d, getDuration("10 d"));
		assertEquals(10*d, getDuration("  10  d  "));
		assertEquals(10*d, getDuration("10day"));
		assertEquals(10*d, getDuration("10 day"));
		assertEquals(10*d, getDuration("  10  day  "));
		assertEquals(10*d, getDuration("10days"));
		assertEquals(10*d, getDuration("10 days"));
		assertEquals(10*d, getDuration("  10  days  "));
		assertEquals(10*d, getDuration("10D"));
		assertEquals(10*d, getDuration("10 D"));
		assertEquals(10*d, getDuration("  10  D  "));

		assertEquals(10*w, getDuration("10w"));
		assertEquals(10*w, getDuration("10 w"));
		assertEquals(10*w, getDuration("  10  w  "));
		assertEquals(10*w, getDuration("10week"));
		assertEquals(10*w, getDuration("10 week"));
		assertEquals(10*w, getDuration("  10  week  "));
		assertEquals(10*w, getDuration("10weeks"));
		assertEquals(10*w, getDuration("10 weeks"));
		assertEquals(10*w, getDuration("  10  weeks  "));
		assertEquals(10*w, getDuration("10W"));
		assertEquals(10*w, getDuration("10 W"));
		assertEquals(10*w, getDuration("  10  W  "));
	}

	//====================================================================================================
	// getDuration(String)
	//====================================================================================================
	@Test void a31_stripInvalidHttpHeaderChars() {
		assertEquals("xxx", stripInvalidHttpHeaderChars("xxx"));
		assertEquals("\t []^x", stripInvalidHttpHeaderChars("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u0020\\[]^x"));
	}

	//====================================================================================================
	// abbreviate(String,int)
	//====================================================================================================
	@Test void a32_abbrevate() {
		assertNull(abbreviate(null, 0));
		assertEquals("foo", abbreviate("foo", 3));
		assertEquals("...", abbreviate("fooo", 3));
		assertEquals("f...", abbreviate("foooo", 4));
		assertEquals("foo", abbreviate("foo", 2));
	}

	//====================================================================================================
	// splitMethodArgs(String)
	//====================================================================================================
	@Test void a33_splitMethodArgs() {
		assertArray(Utils.splitMethodArgs("java.lang.String"), "java.lang.String");
		assertArray(Utils.splitMethodArgs("java.lang.String,java.lang.Integer"), "java.lang.String", "java.lang.Integer");
		assertArray(Utils.splitMethodArgs("x,y"), "x","y");
		assertArray(Utils.splitMethodArgs("x,y<a,b>,z"), "x", "y<a,b>", "z");
		assertArray(Utils.splitMethodArgs("x,y<a<b,c>,d<e,f>>,z"), "x", "y<a<b,c>,d<e,f>>", "z");
	}

	//====================================================================================================
	// fixUrl(String)
	//====================================================================================================
	@Test void a34_fixUrl() {
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
	@Test void a35_diffPosition() {
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
	@Test void a36_diffPositionIc() {
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
	@Test void a37_splitNested() {
		assertNull(Utils.splitNested(null));
		assertList(Utils.splitNested(""));
		assertList(Utils.splitNested("a"), "a");
		assertList(Utils.splitNested("a,b,c"), "a", "b", "c");
		assertList(Utils.splitNested("a{b,c},d"), "a{b,c}", "d");
		assertList(Utils.splitNested("a,b{c,d}"), "a", "b{c,d}");
		assertList(Utils.splitNested("a,b{c,d{e,f}}"), "a", "b{c,d{e,f}}");
		assertList(Utils.splitNested("a { b , c } , d "), "a { b , c }", "d");
		assertList(Utils.splitNested("a\\,b"), ">a,b");
		assertList(Utils.splitNested("a\\\\,b"), "a\\", "b");
	}

	//====================================================================================================
	// splitNestedInner(String)
	//====================================================================================================
	@Test void a38_splitNestedInner() {
		assertThrowsWithMessage(IllegalArgumentException.class, "String was null.", ()->Utils.splitNestedInner(null));
		assertThrowsWithMessage(IllegalArgumentException.class, "String was empty.", ()->Utils.splitNestedInner(""));
		assertList(Utils.splitNestedInner("a{b}"), "b");
		assertList(Utils.splitNestedInner(" a { b } "), "b");
		assertList(Utils.splitNestedInner("a{b,c}"), "b", "c");
		assertList(Utils.splitNestedInner("a{b{c,d},e{f,g}}"), "b{c,d}", "e{f,g}");
	}
}