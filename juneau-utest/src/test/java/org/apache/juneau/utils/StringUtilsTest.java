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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utest.utils.Utils2;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StringUtilsTest {

	//====================================================================================================
	// isNumeric(String,Class)
	// parseNumber(String,Class)
	//====================================================================================================
	@Test
	public void testParseNumber() {

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

		assertThrown(()->parseNumber("x", Number.class)).isExists();
		assertThrown(()->parseNumber("x", null)).isExists();
		assertThrown(()->parseNumber("x", BadNumber.class)).asMessage().isContains("Unsupported Number type");
	}

	@SuppressWarnings("serial")
	private abstract static class BadNumber extends Number {}

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void testNumberRanges() {
		String s;

		// An integer range is -2,147,483,648 to 2,147,483,647

		assertFalse(isNumeric(null));
		assertFalse(isNumeric(""));
		assertFalse(isNumeric("x"));

		s = "-2147483648";
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
		assertThrown(()->parseNumber(String.valueOf("214748364x"), Number.class)).isType(NumberFormatException.class);

		s = String.valueOf("2147483640x");
		assertFalse(isNumeric(s));
		assertThrown(()->parseNumber(String.valueOf("2147483640x"), Long.class)).isType(NumberFormatException.class);
	}

	//====================================================================================================
	// testReplaceVars
	//====================================================================================================
	@Test
	public void testReplaceVars() throws Exception {
		JsonMap m = JsonMap.ofJson("{a:'A',b:1,c:true,d:'{e}',e:'E{f}E',f:'F',g:'{a}',h:'a',i:null}");

		String s = "xxx";
		assertEquals("xxx", replaceVars(s, m));

		s = "{a}";
		assertEquals("A", replaceVars(s, m));
		s = "{a}{a}";
		assertEquals("AA", replaceVars(s, m));
		s = "x{a}x";
		assertEquals("xAx", replaceVars(s, m));
		s = "x{a}x{a}x";
		assertEquals("xAxAx", replaceVars(s, m));

		s = "{b}";
		assertEquals("1", replaceVars(s, m));
		s = "{b}{b}";
		assertEquals("11", replaceVars(s, m));
		s = "x{b}x";
		assertEquals("x1x", replaceVars(s, m));
		s = "x{b}x{b}x";
		assertEquals("x1x1x", replaceVars(s, m));

		s = "{c}";
		assertEquals("true", replaceVars(s, m));
		s = "{c}{c}";
		assertEquals("truetrue", replaceVars(s, m));
		s = "x{c}x{c}x";
		assertEquals("xtruextruex", replaceVars(s, m));

		s = "{d}";
		assertEquals("EFE", replaceVars(s, m));
		s = "{d}{d}";
		assertEquals("EFEEFE", replaceVars(s, m));
		s = "x{d}x";
		assertEquals("xEFEx", replaceVars(s, m));
		s = "x{d}x{d}x";
		assertEquals("xEFExEFEx", replaceVars(s, m));

		s = "{g}";
		assertEquals("A", replaceVars(s, m));
		s = "{g}{g}";
		assertEquals("AA", replaceVars(s, m));
		s = "x{g}x";
		assertEquals("xAx", replaceVars(s, m));
		s = "x{g}x{g}x";
		assertEquals("xAxAx", replaceVars(s, m));

		s = "{x}";
		assertEquals("{x}", replaceVars(s, m));
		s = "{x}{x}";
		assertEquals("{x}{x}", replaceVars(s, m));
		s = "x{x}x{x}x";
		assertEquals("x{x}x{x}x", replaceVars(s, m));

		s = "{{g}}";
		assertEquals("{A}", replaceVars(s, m));
		s = "{{h}}";
		assertEquals("A", replaceVars(s, m));

		s = "{{i}}";
		assertEquals("{}", replaceVars(s, m));
		s = "{}";
		assertEquals("{}", replaceVars(s, m));
	}

	//====================================================================================================
	// isFloat(String)
	//====================================================================================================
	@Test
	public void testisFloat() {
		String[] valid = {
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
			"0x1.FFFFFFFFFFFFFP1023",
		};
		for (String s : valid)
			assertTrue(isFloat(s));

		String[] invalid = {
			null,
			"",
			"a",
			"+",
			"-",
			".",
			"a",
			"+a",
			"11a",
		};
		for (String s : invalid)
			assertFalse(isFloat(s));
	}

	//====================================================================================================
	// isDecimal(String)
	//====================================================================================================
	@Test
	public void testisDecimal() {
		String[] valid = {
			"+1",
			"-1",
			"0x123",
			"0X123",
			"0xdef",
			"0XDEF",
			"#def",
			"#DEF",
			"0123",
		};
		for (String s : valid)
			assertTrue(isDecimal(s));

		String[] invalid = {
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
			"012A",
		};
		for (String s : invalid)
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
	public void testJoin() {
		assertNull(join((Object[])null, ","));
		assertEquals("1", join(new Object[]{1}, ","));
		assertEquals("1,2", join(new Object[]{1,2}, ","));

		assertNull(join((Collection<?>)null, ","));
		assertEquals("1", join(Arrays.asList(new Integer[]{1}), ","));
		assertEquals("1,2", join(Arrays.asList(new Integer[]{1,2}), ","));

		assertNull(join((Object[])null, ','));
		assertEquals("x,y,z", join(new Object[]{"x,y","z"}, ','));

		assertNull(join((int[])null, ','));
		assertEquals("1", join(new int[]{1}, ','));
		assertEquals("1,2", join(new int[]{1,2}, ','));

		assertNull(join((Collection<?>)null, ','));
		assertEquals("1", join(Arrays.asList(new Integer[]{1}), ','));
		assertEquals("1,2", join(Arrays.asList(new Integer[]{1,2}), ','));

		assertNull(joine((List<?>)null, ','));
		assertEquals("x\\,y,z", joine(Arrays.asList(new String[]{"x,y","z"}), ','));
	}

	//====================================================================================================
	// split(String,char)
	//====================================================================================================
	@Test
	public void testSplit() {
		String[] r;

		assertNull(split((String)null));
		assertObject(split("")).asJson().is("[]");
		assertObject(split("1")).asJson().is("['1']");
		assertObject(split("1,2")).asJson().is("['1','2']");
		assertObject(split("1\\,2")).asJson().is("['1,2']");

		r = split("1\\\\,2");
		assertEquals("1\\", r[0]);
		assertEquals("2", r[1]);

		r = split("1\\\\\\,2");
		assertEquals(1, r.length);
		assertEquals("1\\,2", r[0]);

		r = split("1,2\\");
		assertEquals("2\\", r[1]);

		r = split("1,2\\\\");
		assertEquals("2\\", r[1]);

		r = split("1,2\\,");
		assertEquals("2,", r[1]);

		r = split("1,2\\\\,");
		assertEquals("2\\", r[1]);
		assertEquals("", r[2]);
	}

	@Test
	public void testSplit2() {
		List<String> l1 = list();
		split((String)null, l1::add);
		assertList(l1).isEmpty();

		List<String> l2 = list();
		split("", l2::add);
		assertObject(l2).asJson().is("[]");

		List<String> l3 = list();
		split("1", l3::add);
		assertObject(l3).asJson().is("['1']");

		List<String> l4 = list();
		split("1,2", l4::add);
		assertObject(l4).asJson().is("['1','2']");

		List<String> l5 = list();
		split("1\\,2", l5::add);
		assertObject(l5).asJson().is("['1,2']");

		List<String> l6 = list();
		split("1\\\\,2", l6::add);
		assertEquals("1\\", l6.get(0));
		assertEquals("2", l6.get(1));

		List<String> l7 = list();
		split("1\\\\\\,2", l7::add);
		assertEquals(1, l7.size());
		assertEquals("1\\,2", l7.get(0));

		List<String> l8 = list();
		split("1,2\\", l8::add);
		assertEquals("2\\", l8.get(1));

		List<String> l9 = list();
		split("1,2\\\\", l9::add);
		assertEquals("2\\", l9.get(1));

		List<String> l10 = list();
		split("1,2\\,", l10::add);
		assertEquals("2,", l10.get(1));

		List<String> l11 = list();
		split("1,2\\\\,", l11::add);
		assertEquals("2\\", l11.get(1));
		assertEquals("", l11.get(2));
	}

	//====================================================================================================
	// split(String,char,int)
	//====================================================================================================
	@Test
	public void testSplitWithLimit() {
		String[] r;

		r = split("boo:and:foo", ':', 10);
		assertObject(r).asJson().is("['boo','and','foo']");

		r = split("boo:and:foo", ':', 2);
		assertObject(r).asJson().is("['boo','and:foo']");

		r = split("boo:and:foo", ':', 1);
		assertObject(r).asJson().is("['boo:and:foo']");

		r = split("boo:and:foo", ':', 0);
		assertObject(r).asJson().is("['boo:and:foo']");

		r = split("boo:and:foo", ':', -1);
		assertObject(r).asJson().is("['boo:and:foo']");

		r = split("boo : and : foo", ':', 10);
		assertObject(r).asJson().is("['boo','and','foo']");

		r = split("boo : and : foo", ':', 2);
		assertObject(r).asJson().is("['boo','and : foo']");
	}

	//====================================================================================================
	// nullIfEmpty(String)
	//====================================================================================================
	@Test
	public void testNullIfEmpty() {
		assertNull(nullIfEmpty(null));
		assertNull(nullIfEmpty(""));
		assertNotNull(nullIfEmpty("x"));
	}

	//====================================================================================================
	// unescapeChars(String,char[],char)
	//====================================================================================================
	@Test
	public void testUnescapeChars() {
		AsciiSet escape = AsciiSet.create("\\,|");

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

		escape = AsciiSet.create(",|");
		assertEquals("x\\\\xx", unEscapeChars("x\\\\xx", escape));
	}

	//====================================================================================================
	// decodeHex(String)
	//====================================================================================================
	@Test
	public void testDecodeHex() {
		assertNull(decodeHex(null));
		assertEquals("19azAZ", decodeHex("19azAZ"));
		assertEquals("[0][1][ffff]", decodeHex("\u0000\u0001\uFFFF"));
	}

	//====================================================================================================
	// startsWith(String,char)
	//====================================================================================================
	@Test
	public void testStartsWith() {
		assertFalse(startsWith(null, 'a'));
		assertFalse(startsWith("", 'a'));
		assertTrue(startsWith("a", 'a'));
		assertTrue(startsWith("ab", 'a'));
	}

	//====================================================================================================
	// endsWith(String,char)
	//====================================================================================================
	@Test
	public void testEndsWith() {
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
	public void testBase64EncodeToString() {
		String s = null;

		assertEquals(s, base64DecodeToString(base64EncodeToString(s)));
		s = "";
		assertEquals(s, base64DecodeToString(base64EncodeToString(s)));
		s = "foobar";
		assertEquals(s, base64DecodeToString(base64EncodeToString(s)));
		s = "\u0000\uffff";
		assertEquals(s, base64DecodeToString(base64EncodeToString(s)));

		assertThrown(()->base64Decode("a")).asMessage().is("Invalid BASE64 string length.  Must be multiple of 4.");
		assertThrown(()->base64Decode("aaa")).isType(IllegalArgumentException.class);
	}

	//====================================================================================================
	// generateUUID(String)
	//====================================================================================================
	@Test
	public void testGenerateUUID() {
		for (int i = 0; i < 10; i++) {
			String s = random(i);
			assertEquals(i, s.length());
			for (char c : s.toCharArray())
				assertTrue(Character.isLowerCase(c) || Character.isDigit(c));
		}
	}

	//====================================================================================================
	// trim(String)
	//====================================================================================================
	@Test
	public void testTrim() {
		assertNull(trim(null));
		assertEquals("", trim(""));
		assertEquals("", trim("  "));
	}

	//====================================================================================================
	// parseISO8601Date(String)
	//====================================================================================================
	@Test
	public void testParseISO8601Date() throws Exception {
		WriterSerializer s = JsonSerializer.create().json5().build();

		assertNull(parseIsoDate(null));
		assertNull(parseIsoDate(""));

		Utils2.setTimeZone("GMT");
		try {
			assertEquals("'2000-01-01T00:00:00'", s.serialize(parseIsoDate("2000")));
			assertEquals("'2000-02-01T00:00:00'", s.serialize(parseIsoDate("2000-02")));
			assertEquals("'2000-02-03T00:00:00'", s.serialize(parseIsoDate("2000-02-03")));
			assertEquals("'2000-02-03T04:00:00'", s.serialize(parseIsoDate("2000-02-03T04")));
			assertEquals("'2000-02-03T04:05:00'", s.serialize(parseIsoDate("2000-02-03T04:05")));
			assertEquals("'2000-02-03T04:05:06'", s.serialize(parseIsoDate("2000-02-03T04:05:06")));
			assertEquals("'2000-02-03T04:00:00'", s.serialize(parseIsoDate("2000-02-03 04")));
			assertEquals("'2000-02-03T04:05:00'", s.serialize(parseIsoDate("2000-02-03 04:05")));
			assertEquals("'2000-02-03T04:05:06'", s.serialize(parseIsoDate("2000-02-03 04:05:06")));

			// ISO8601 doesn't support milliseconds, so it gets trimmed.
			assertEquals("'2000-02-03T04:05:06'", s.serialize(parseIsoDate("2000-02-03 04:05:06,789")));
		} finally {
			Utils2.unsetTimeZone();
		}
	}

	//====================================================================================================
	// parseMap(String,char,char,boolean)
	//====================================================================================================
	@Test
	public void testSplitMap() {
		assertObject(splitMap("a=1", true)).asJson().is("{a:'1'}");
		assertObject(splitMap("a=1,b=2", true)).asJson().is("{a:'1',b:'2'}");
		assertObject(splitMap(" a = 1 , b = 2 ", true)).asJson().is("{a:'1',b:'2'}");
		assertObject(splitMap(" a = 1 , b = 2 ", false)).asJson().is("{' a ':' 1 ',' b ':' 2 '}");
		assertObject(splitMap("a", true)).asJson().is("{a:''}");
		assertObject(splitMap("a,b", true)).asJson().is("{a:'',b:''}");
		assertObject(splitMap("a=1,b", true)).asJson().is("{a:'1',b:''}");
		assertObject(splitMap("a,b=1", true)).asJson().is("{a:'',b:'1'}");
		assertObject(splitMap("a\\==1", true)).asJson().is("{'a=':'1'}");
		assertObject(splitMap("a\\\\=1", true)).asJson().is("{'a\\\\':'1'}");
	}

	//====================================================================================================
	// isAbsoluteUri(String)
	//====================================================================================================
	@Test
	public void testIsAbsoluteUri() {
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
	public void testGetAuthorityUri() {
		assertEquals("http://foo", getAuthorityUri("http://foo"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123/"));
		assertEquals("http://foo:123", getAuthorityUri("http://foo:123/bar"));
	}

	//====================================================================================================
	// splitQuoted(String)
	//====================================================================================================
	@Test
	public void getSplitQuoted() {
		assertObject(splitQuoted(null)).asJson().is("null");
		assertObject(splitQuoted("")).asJson().is("[]");
		assertObject(splitQuoted(" \t ")).asJson().is("[]");
		assertObject(splitQuoted("foo")).asJson().is("['foo']");
		assertObject(splitQuoted("foo  bar baz")).asJson().is("['foo','bar','baz']");
		assertObject(splitQuoted("'foo'")).asJson().is("['foo']");
		assertObject(splitQuoted(" ' foo ' ")).asJson().is("[' foo ']");
		assertObject(splitQuoted("'foo' 'bar'")).asJson().is("['foo','bar']");
		assertObject(splitQuoted("\"foo\"")).asJson().is("['foo']");
		assertObject(splitQuoted(" \" foo \" ")).asJson().is("[' foo ']");
		assertObject(splitQuoted("\"foo\" \"bar\"")).asJson().is("['foo','bar']");
		assertObject(splitQuoted("'foo\\'bar'")).asJson().is("['foo\\'bar']");
		assertObject(splitQuoted("'foo\\\"bar'")).asJson().is("['foo\"bar']");
		assertObject(splitQuoted("'\\'foo\\'bar\\''")).asJson().is("['\\'foo\\'bar\\'']");
		assertObject(splitQuoted("'\\\"foo\\\"bar\\\"'")).asJson().is("['\"foo\"bar\"']");
		assertObject(splitQuoted("'\\'foo\\''")).asJson().is("['\\'foo\\'']");
		assertObject(splitQuoted("\"\\\"foo\\\"\"")).asJson().is("['\"foo\"']");
		assertObject(splitQuoted("'\"foo\"'")).asJson().is("['\"foo\"']");
		assertObject(splitQuoted("\"'foo'\"")).asJson().is("['\\'foo\\'']");
	}

	//====================================================================================================
	// firstNonWhitespaceChar(String)
	//====================================================================================================
	@Test
	public void testFirstNonWhitespaceChar() {
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
	public void testLastNonWhitespaceChar() {
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
	public void testIsJsonObject() {
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
	public void testIsJsonArray() {
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
	public void testAddLineNumbers() {
		assertNull(getNumberedLines(null));
		assertEquals("1: \n", getNumberedLines(""));
		assertEquals("1: foo\n", getNumberedLines("foo"));
		assertEquals("1: foo\n2: bar\n", getNumberedLines("foo\nbar"));
	}

	//====================================================================================================
	// compare(String,String)
	//====================================================================================================
	@Test
	public void testCompare() {
		assertTrue(compare("a","b") < 0);
		assertTrue(compare("b","a") > 0);
		assertTrue(compare(null,"b") < 0);
		assertTrue(compare("b",null) > 0);
		assertEquals(0, compare(null,null));
	}

	//====================================================================================================
	// matchPattern(String)
	//====================================================================================================
	@Test
	public void testGetMatchPattern() {
		assertTrue(getMatchPattern("a").matcher("a").matches());
		assertTrue(getMatchPattern("*a*").matcher("aaa").matches());
		assertFalse(getMatchPattern("*b*").matcher("aaa").matches());
	}

	//====================================================================================================
	// getDuration(String)
	//====================================================================================================
	@Test
	public void testGetDuration() {
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
	@Test
	public void testStripInvalidHttpHeaderChars() {
		assertEquals("xxx", stripInvalidHttpHeaderChars("xxx"));
		assertEquals("\t []^x", stripInvalidHttpHeaderChars("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u0020\\[]^x"));
	}

	//====================================================================================================
	// abbreviate(String,int)
	//====================================================================================================
	@Test
	public void testAbbrevate() {
		assertNull("xxx", abbreviate(null, 0));
		assertEquals("foo", abbreviate("foo", 3));
		assertEquals("...", abbreviate("fooo", 3));
		assertEquals("f...", abbreviate("foooo", 4));
		assertEquals("foo", abbreviate("foo", 2));
	}

	//====================================================================================================
	// splitMethodArgs(String)
	//====================================================================================================
	@Test
	public void testSplitMethodArgs() {
		assertObject(splitMethodArgs("java.lang.String")).asJson().is("['java.lang.String']");
		assertObject(splitMethodArgs("java.lang.String,java.lang.Integer")).asJson().is("['java.lang.String','java.lang.Integer']");
		assertObject(splitMethodArgs("x,y")).asJson().is("['x','y']");
		assertObject(splitMethodArgs("x,y<a,b>,z")).asJson().is("['x','y<a,b>','z']");
		assertObject(splitMethodArgs("x,y<a<b,c>,d<e,f>>,z")).asJson().is("['x','y<a<b,c>,d<e,f>>','z']");
	}

	//====================================================================================================
	// fixUrl(String)
	//====================================================================================================
	@Test
	public void testFixUrl() {
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
	public void testDiffPosition() {
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
	public void testDiffPositionIc() {
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
}