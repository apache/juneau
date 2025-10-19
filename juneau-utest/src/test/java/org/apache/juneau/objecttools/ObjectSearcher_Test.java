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
package org.apache.juneau.objecttools;

import static org.apache.juneau.TestUtils.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

/**
 * Tests the PojoSearcher class.
 */
public class ObjectSearcher_Test extends TestBase {

	private static BeanSession bs = BeanContext.DEFAULT_SESSION;
	private static ObjectSearcher os = ObjectSearcher.DEFAULT;
	private static WriterSerializer ws = JsonSerializer.create().json5().swaps(TemporalCalendarSwap.IsoLocalDateTime.class).build();

	//-----------------------------------------------------------------------------------------------------------------
	// Utility
	//-----------------------------------------------------------------------------------------------------------------

	static SearchArgs[] create(String...search) {
		var sa = new SearchArgs[search.length];
		for (var i = 0; i < search.length; i++)
			sa[i] = SearchArgs.create(search[i]);
		return sa;
	}

	static SearchArgs create(String search) {
		return SearchArgs.create(search);
	}

	static Object run(Object in, String search) {
		return os.run(bs, in, create(search));
	}

	static Object run(Object in, SearchArgs sa) {
		return os.run(bs, in, sa);
	}

	static String[] a(String...s) {
		return s;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// String search
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public String f;

		public static A create(String f) {
			var a = new A();
			a.f = f;
			return a;
		}
	}

	public static List<A> A_LIST = list(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static Set<A> A_SET = set(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static A[] A_ARRAY = {A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null)};

	@Test void a01_stringSearch_singleWord() {
		assertBeans(run(A_LIST, "f=foo"), "f", "foo");
		assertBeans(run(A_SET, "f=foo"), "f", "foo");
		assertBeans(run(A_ARRAY, "f=foo"), "f", "foo");
		assertBeans(os.run(A_LIST, "f=foo"), "f", "foo");
		assertBeans(os.run(A_SET, "f=foo"), "f", "foo");
		assertBeans(os.run(A_ARRAY, "f=foo"), "f", "foo");
	}

	@Test void a02_stringSearch_pattern1() {
		assertBeans(run(A_LIST, "f=fo*"), "f", "foo");
		assertBeans(run(A_SET, "f=fo*"), "f", "foo");
		assertBeans(run(A_ARRAY, "f=fo*"), "f", "foo");
	}

	@Test void a03_stringSearch_pattern2() {
		assertBeans(run(A_LIST, "f=*ar"), "f", "bar");
		assertBeans(run(A_SET, "f=*ar"), "f", "bar");
		assertBeans(run(A_ARRAY, "f=*ar"), "f", "bar");
	}

	@Test void a04_stringSearch_pattern3() {
		assertBeans(run(A_LIST, "f=?ar"), "f", "bar");
		assertBeans(run(A_SET, "f=?ar"), "f", "bar");
		assertBeans(run(A_ARRAY, "f=?ar"), "f", "bar");
	}

	@Test void a05_stringSearch_multiple() {
		assertBeans(run(A_LIST, "f=foo bar q ux"), "f", "foo", "bar");
		assertBeans(run(A_SET, "f=foo bar q ux"), "f", "foo", "bar");
		assertBeans(run(A_ARRAY, "f=foo bar q ux"), "f", "foo", "bar");
	}

	@Test void a06_stringSearch_quoted() {
		assertBeans(run(A_LIST, "f='q ux'"), "f", "q ux");
		assertBeans(run(A_SET, "f='q ux'"), "f", "q ux");
		assertBeans(run(A_ARRAY, "f='q ux'"), "f", "q ux");
	}

	@Test void a07_stringSearch_quotedWithPattern() {
		assertBeans(run(A_LIST, "f='q *x'"), "f", "q ux");
		assertBeans(run(A_SET, "f='q *x'"), "f", "q ux");
		assertBeans(run(A_ARRAY, "f='q *x'"), "f", "q ux");
	}

	@Test void a08_stringSearch_unquotedContainingQuote() {
		assertBeans(run(A_LIST, "f=qu'ux"), "f", "qu'ux");
		assertBeans(run(A_SET, "f=qu'ux"), "f", "qu'ux");
		assertBeans(run(A_ARRAY, "f=qu'ux"), "f", "qu'ux");
	}

	@Test void a09_stringSearch_quotedContainingQuote() {
		assertBeans(run(A_LIST, "f='qu\\'ux'"), "f", "qu'ux");
		assertBeans(run(A_SET, "f='qu\\'ux'"), "f", "qu'ux");
		assertBeans(run(A_ARRAY, "f='qu\\'ux'"), "f", "qu'ux");
	}

	@Test void a10_stringSearch_regExp() {
		assertBeans(run(A_LIST, "f=/q\\sux/"), "f", "q ux");
		assertBeans(run(A_SET, "f=/q\\sux/"), "f", "q ux");
		assertBeans(run(A_ARRAY, "f=/q\\sux/"), "f", "q ux");
	}

	@Test void a11_stringSearch_regExp_noEndSlash() {
		var in = list(A.create("/foo"), A.create("bar"));
		for (var s : a("f=/foo","f='/foo'"))
			assertBeans(run(in, s), "f", "/foo");
	}

	@Test void a12_stringSearch_regExp_onlySlash() {
		var in = list(A.create("/"), A.create("bar"));
		for (var s : a("f=/", "f='/'"))
			assertBeans(run(in, s), "f", "/");
	}

	@Test void a13_stringSearch_or_pattern() {
		var in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f=f* *r"), "f", "foo", "bar");
		assertEmpty(run(in, "f='f* *r'"));
		assertBeans(run(in, "f='f*oo'"), "f", "foo");
	}

	@Test void a14_stringSearch_explicit_or_pattern() {
		var in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f=^f* ^*r"), "f", "foo", "bar");
		assertEmpty(run(in, "f=^'f* *r'"));
		assertBeans(run(in, "f=^'f*oo'"), "f", "foo");
	}

	@Test void a15_stringSearch_and_pattern() {
		var in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f=+b* +*r"), "f", "bar");
		assertBeans(run(in, "f=+'b*' +'*r'"), "f", "bar");
	}

	@Test void a16_stringSearch_not_pattern() {
		var in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f=b* -*r"), "f", "baz");
		assertBeans(run(in, "f=+'b*' -'*r'"), "f", "baz");
	}

	@Test void a17_stringSearch_caseSensitive() {
		var in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertEmpty(run(in, "f=F*"));
		assertEmpty(run(in, "f=\"F*\""));
		assertBeans(run(in, "f='F*'"), "f", "foo");
	}

	@Test void a18_stringSearch_malformedQuotes() {
		var in = list(A.create("'foo"), A.create("\"bar"), A.create("baz"));

		assertThrowsWithMessage(Exception.class, "Unmatched string quotes", ()->run(in, "f='*"));

		assertThrowsWithMessage(Exception.class, "Unmatched string quotes", ()->run(in, "f=\"*"));

		assertBeans(run(in, "f='\\'*'"), "f", "'foo");
		assertBeans(run(in, "f='\"*'"), "f", "\"bar");
		assertBeans(run(in, "f=\"\\\"*\""), "f", "\"bar");
	}

	@Test void a19_stringSearch_regexChars() {
		var in = list(A.create("+\\[]{}()^$."), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f=*+*"), "f", "+\\[]{}()^$.");
		assertBeans(run(in, "f='+\\\\[]{}()^$.'"), "f", "+\\[]{}()^$.");
		assertBeans(run(in, "f=++\\\\[]{}()^$."), "f", "+\\[]{}()^$.");
	}

	@Test void a20_stringSearch_metaChars() {
		var in = list(A.create("*?\\'\""), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f='\\*\\?\\\\\\'\"'"), "f", "*?\\'\"");
	}

	@Test void a21_stringSearch_metaChars_escapedQuotes() {
		var in = list(A.create("'"), A.create("\""), A.create("baz"));
		assertBeans(run(in, "f=\\'"), "f", "'");
		assertBeans(run(in, "f=\\\""), "f", "\"");
	}

	@Test void a22_stringSearch_metaChars_falseEscape() {
		var in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertBeans(run(in, "f=\\f\\o\\o"), "f", "foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Number search
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public int f;

		static C create(int f) {
			var c = new C();
			c.f = f;
			return c;
		}
	}

	C[] INT_BEAN_ARRAY = {C.create(-2), C.create(-1), C.create(0), C.create(1), C.create(2), C.create(3)};

	@Test void b01_intSearch_oneNumber() {
		for (var s : a("f=1", "f = 1"))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", "1");
	}

	@Test void b02_intSearch_twoNumbers() {
		for (var s : a("f=1 2", "f = 1  2 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("1,2"));
	}

	@Test void b03_intSearch_oneNegativeNumber() {
		for (var s : a("f=-1", "f = -1 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", "-1");
	}

	@Test void b04_intSearch_twoNegativeNumbers() {
		assertBeans(run(INT_BEAN_ARRAY, "f=-1 -2"), "f", StringUtils.splita("-2,-1"));
	}

	@Test void b05_intSearch_simpleRange() {
		for (var s : a("f=1-2", "f = 1 - 2 ", "f = 1- 2 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("1,2"));
	}

	@Test void b06_intSearch_simpleRange_invalid() {
		assertEmpty(run(INT_BEAN_ARRAY, "f=2-1"));
	}

	@Test void b07_intSearch_twoNumbersThatLookLikeRange() {
		assertBeans(run(INT_BEAN_ARRAY, "f = 1 -2 "), "f", StringUtils.splita("-2,1"));
	}

	@Test void b08_intSearch_rangeWithNegativeNumbers() {
		assertBeans(run(INT_BEAN_ARRAY, "f = -2--1 "), "f", StringUtils.splita("-2,-1"));
	}

	@Test void b09_intSearch_rangeWithNegativeNumbers_invalidRange() {
		assertEmpty(run(INT_BEAN_ARRAY, "f = -1--2 "));
	}

	@Test void b10_intSearch_multipleRanges() {
		assertBeans(run(INT_BEAN_ARRAY, "f = 0-1 3-4"), "f", StringUtils.splita("0,1,3"));
	}

	@Test void b11_intSearch_overlappingRanges() {
		assertBeans(run(INT_BEAN_ARRAY, "f = 0-0 2-2"), "f", StringUtils.splita("0,2"));
	}

	@Test void b12_intSearch_LT() {
		for (var s : a("f = <0", "f<0", "f = < 0 ", "f < 0 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("-2,-1"));
	}

	@Test void b13_intSearch_LT_negativeNumber() {
		for (var s : a("f = <-1", "f<-1", "f = < -1 ", "f < -1 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", "-2");
	}

	@Test void b14_intSearch_GT() {
		for (var s : a("f = >1", "f>1", "f = > 1 ", "f > 1 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("2,3"));
	}

	@Test void b15_intSearch_GT_negativeNumber() {
		for (var s : a("f = >-1", "f>-1", "f = > -1 ", "f > -1 ", "f =  >  -1  ", "f >  -1  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("0,1,2,3"));
	}

	@Test void b16_intSearch_LTE() {
		for (var s : a("f = <=0", "f<=0", "f = <= 0 ", "f <= 0 ", "f =  <=  0  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("-2,-1,0"));
	}

	@Test void b17_intSearch_LTE_negativeNumber() {
		for (var s : a("f = <=-1", "f <=-1", "f = <= -1 ", "f =  <=  -1  ", "f <=  -1  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("-2,-1"));
	}

	@Test void b18_intSearch_GTE() {
		for (var s : a("f = >=1", "f >=1", "f = >= 1 ", "f >= 1 ", "f =  >=  1  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("1,2,3"));
	}

	@Test void b19_intSearch_GTE_negativeNumber() {
		for (var s : a("f = >=-1", "f >=-1", "f = >= -1 ", "f >= -1 ", "f =  >=  -1  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("-1,0,1,2,3"));
	}

	@Test void b20_intSearch_not_singleNumber() {
		for (var s : a("f = !1", "f = ! 1 ", "f =  !  1  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("-2,-1,0,2,3"));
	}

	@Test void b21_intSearch_not_range() {
		assertBeans(run(INT_BEAN_ARRAY, "f = !1-2"), "f", StringUtils.splita("-2,-1,0,3"));
	}

	@Test void b22_intSearch_not_range_negativeNumbers() {
		for (var s : a("f = !-2--1", "f = ! -2 - -1", "f =  !  -2  -  -1 "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("0,1,2,3"));
	}

	@Test void b23_intSearch_not_looksLikeRange() {
		assertBeans(run(INT_BEAN_ARRAY, "f = ! -2 -2"), "f", StringUtils.splita("-2,-1,0,1,2,3"));
	}

	@Test void b24_intSearch_empty() {
		for (var s : a("f=", "f = ", "f =  "))
			assertBeans(run(INT_BEAN_ARRAY, s), "f", StringUtils.splita("-2,-1,0,1,2,3"));
	}

	@Test void b25_intSearch_badSearches() {
		var ss = a(
			"f=x","(S01)",
			"f=>x","(S02)",
			"f=<x","(S03)",
			"f=>=x","(S04)",
			"f=>= x","(S05)",
			"f=1x","(S06)",
			"f=1 x","(S07)",
			"f=1-x","(S08)",
			"f=1 -x","(S09)",
			"f=1 - x","(S10)",
			"f=1 - 1x","(S11)",
			"f=>","(ES02)",
			"f=<","(ES03)",
			"f=>=","(ES04)",
			"f=123-","(ES08)",
			"f=123 -","(ES09)"
		);

		for (var i = 0; i < ss.length; i+=2) {
			final int i2 = i;
			assertThrowsWithMessage(Exception.class, ss[i+1], ()->run(INT_BEAN_ARRAY, ss[i2]));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Date search
	//-----------------------------------------------------------------------------------------------------------------

	public static class B {
		public Calendar f;

		static B[] create(String...dates) {
			var bb = new B[dates.length];
			for (var i = 0; i < dates.length; i++) {
				bb[i] = new B();
				bb[i].f = DateUtils.parseISO8601Calendar(dates[i]);
			}
			return bb;
		}
	}

	@Test void c01_dateSearch_singleDate_y() {
		var in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		for (var s : a(
				"f=2011",
				"f = 2011 ",
				"f = '2011' ",
				"f = \"2011\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]");
	}

	@Test void c02_dateSearch_singleDate_ym() {
		var in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		for (var s : a(
				"f=2011-01",
				"f = 2011-01 ",
				"f='2011-01'",
				"f=\"2011-01\""
			))
			assertSerialized(run(in, s), ws, "[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]");
	}

	@Test void c03_dateSearch_singleDate_ymd() {
		var in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		assertSerialized(run(in, "f=2011-01-01"), ws, "[{f:'2011-01-01T00:00:00'}]");
	}

	@Test void c04_dateSearch_singleDate_ymdh() {
		var in = B.create("2011-01-01T11:15:59", "2011-01-01T12:00:00", "2011-01-01T12:59:59", "2011-01-01T13:00:00");
		assertSerialized(run(in, "f=2011-01-01T12"), ws, "[{f:'2011-01-01T12:00:00'},{f:'2011-01-01T12:59:59'}]");
	}

	@Test void c05_dateSearch_singleDate_ymdhm() {
		var in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00", "2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertSerialized(run(in, "f=2011-01-01T12:30"), ws, "[{f:'2011-01-01T12:30:00'},{f:'2011-01-01T12:30:59'}]");
	}

	@Test void c06_dateSearch_singleDate_ymdhms() {
		var in = B.create("2011-01-01T12:30:29", "2011-01-01T12:30:30", "2011-01-01T12:30:31");
		assertSerialized(run(in, "f=2011-01-01T12:30:30"), ws, "[{f:'2011-01-01T12:30:30'}]");
	}

	@Test void c07_dateSearch_openEndedRanges_y() {
		var in = B.create("2000-12-31", "2001-01-01");
		for (var s : a(
				"f>2000",
				"f > 2000 ",
				"f>'2000'",
				"f > '2000' ",
				"f>\"2000\"",
				"f > \"2000\" ",
				"f>=2001",
				"f >= 2001 ",
				"f>='2001'",
				"f >= '2001' ",
				"f>=\"2001\"",
				"f >= \"2001\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2001-01-01T00:00:00'}]");
		for (var s : a(
				"f<2001",
				"f < 2001 ",
				"f<'2001'",
				"f < '2001'",
				"f<\"2001\"",
				"f < \"2001\" ",
				"f<=2000",
				"f <= 2000 ",
				"f<='2000'",
				"f <= '2000'",
				"f<=\"2000\"",
				"f <= \"2000\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2000-12-31T00:00:00'}]");
	}

	@Test void c08_dateSearch_openEndedRanges_toMinute() {
		var in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00");
		assertSerialized(run(in, "f>=2011-01-01T12:30"), ws, "[{f:'2011-01-01T12:30:00'}]");
		assertSerialized(run(in, "f<2011-01-01T12:30"), ws, "[{f:'2011-01-01T12:29:59'}]");
	}

	@Test void c09_dateSearch_openEndedRanges_toSecond() {
		var in = B.create("2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertSerialized(run(in, "f>2011-01-01T12:30"), ws, "[{f:'2011-01-01T12:31:00'}]");
		assertSerialized(run(in, "f<=2011-01-01T12:30"), ws, "[{f:'2011-01-01T12:30:59'}]");
	}

	@Test void c10_dateSearch_closedRanges() {
		var in = B.create("2000-12-31T23:59:59", "2001-01-01T00:00:00", "2003-06-30T23:59:59", "2003-07-01T00:00:00");

		for (var s : a(
				"f= 2001 - 2003-06-30 ",
				"f= 2001 - 2003-06-30",
				"f='2001'-'2003-06-30'",
				"f= '2001' - '2003-06-30' ",
				"f=\"2001\"-\"2003-06-30\"",
				"f= \"2001\" - \"2003-06-30\" ",
				"f=2001 -'2003-06-30'",
				"f= 2001 - '2003-06-30' ",
				"f=2001 -\"2003-06-30\"",
				"f= 2001 - \"2003-06-30\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]");

		for (var s : a(
			"f= 2001 - 2003-06-30 2000",
			"f= 2001 - 2003-06-30 '2000'",
			"f= 2001 - 2003-06-30 \"2000\"",
			"f='2001'-'2003-06-30' 2000",
			"f='2001'-'2003-06-30' '2000'",
			"f='2001'-'2003-06-30' \"2000\"",
			"f= '2001' - '2003-06-30'  2000",
			"f= '2001' - '2003-06-30'  '2000'",
			"f= '2001' - '2003-06-30'  \"2000\"",
			"f=\"2001\"-\"2003-06-30\" 2000",
			"f=\"2001\"-\"2003-06-30\" '2000'",
			"f=\"2001\"-\"2003-06-30\" \"2000\"",
			"f= \"2001\" - \"2003-06-30\"  2000",
			"f= \"2001\" - \"2003-06-30\"  '2000'",
			"f= \"2001\" - \"2003-06-30\"  \"2000\"",
			"f= 2001 - '2003-06-30'  2000",
			"f= 2001 - '2003-06-30'  '2000'",
			"f= 2001 - '2003-06-30'  \"2000\"",
			"f= 2001 - \"2003-06-30\"  2000",
			"f= 2001 - \"2003-06-30\"  '2000'",
			"f= 2001 - \"2003-06-30\"  \"2000\""
		))
			assertSerialized(run(in, s), ws, "[{f:'2000-12-31T23:59:59'},{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]");
	}

	@Test void c11_dateSearch_or1() {
		var in = B.create("2000-12-31", "2001-01-01", "2001-12-31", "2002-01-01");
		for (var s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2001-01-01T00:00:00'},{f:'2001-12-31T00:00:00'}]");
	}

	@Test void c12_dateSearch_or2() {
		var in = B.create("2002-12-31", "2003-01-01", "2003-12-31", "2004-01-01");
		for (var s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2003-01-01T00:00:00'},{f:'2003-12-31T00:00:00'}]");
	}

	@Test void c13_dateSearch_or3() {
		var in = B.create("2004-12-31", "2005-01-01", "2005-12-31", "2006-01-01");
		for (var s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2005-01-01T00:00:00'},{f:'2005-12-31T00:00:00'}]");
	}

	@Test void c14_dateSearch_or_singleAndRange() {
		var in = B.create("2000-12-31", "2001-01-01", "2002-12-31", "2003-01-01");
		for (var s : a(
				"f=2001 >2002",
				"f= 2001   >2002 ",
				"f='2001' >'2002'",
				"f= '2001'  >'2002' ",
				"f=\"2001\" >\"2002\"",
				"f= \"2001\"  >\"2002\" ",
				"f=>2002 2001",
				"f= >2002  2001 ",
				"f=>'2002' '2001'",
				"f= >'2002'  '2001' ",
				"f=>\"2002\" \"2001\"",
				"f= >\"2002\"  \"2001\" ",
				"f=2001 >=2003",
				"f= 2001  >=2003 ",
				"f='2001' >='2003'",
				"f= '2001'  >='2003' ",
				"f=\"2001\" >=\"2003\"",
				"f= \"2001\"  >=\"2003\" ",
				"f=>=2003 2001",
				"f= >=2003  2001 ",
				"f=>='2003' '2001'",
				"f= >='2003'  '2001' ",
				"f=>=\"2003\" \"2001\"",
				"f= >=\"2003\"  \"2001\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2001-01-01T00:00:00'},{f:'2003-01-01T00:00:00'}]");
		for (var s : a(
				"f=<2001 2003",
				"f= <2001  2003 ",
				"f=<'2001' '2003'",
				"f= <'2001'  '2003' ",
				"f=<\"2001\" \"2003\"",
				"f= <\"2001\"  \"2003\" ",
				"f=2003 <2001",
				"f= 2003  <2001 ",
				"f='2003' <'2001'",
				"f= '2003'  <'2001' ",
				"f=\"2003\" <\"2001\"",
				"f= \"2003\"  <\"2001\" ",
				"f=<=2000 2003",
				"f= <=2000  2003 ",
				"f=<='2000' '2003'",
				"f= <='2000'  '2003' ",
				"f=<=\"2000\" \"2003\"",
				"f= <=\"2000\"  \"2003\" ",
				"f=2003 <=2000",
				"f= 2003  <=2000 ",
				"f='2003' <='2000'",
				"f= '2003'  <='2000' ",
				"f=\"2003\" <=\"2000\"",
				"f= \"2003\"  <=\"2000\" "
			))
			assertSerialized(run(in, s), ws, "[{f:'2000-12-31T00:00:00'},{f:'2003-01-01T00:00:00'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other data structures.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_d2ListOfMaps() {
		List<Map<?,?>> in = list(
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null)
		);
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d02_d2SetOfMaps() {
		Set<Map<?,?>> in = set(
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null)
		);
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d03_d2ArrayOfMaps() {
		Map<?,?>[] in = new Map[]{
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null)
		};
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d04_d2ListOfObjects() {
		List<Object> in = list(
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null),
			"xxx",
			123
		);
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d05_d2SetOfObjects() {
		Set<Object> in = set(
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null),
			"xxx",
			123
		);
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d06_d2ArrayOfObjects() {
		Object[] in = {
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null),
			"xxx",
			123
		};
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d07_d2ListOfMapsWithLists() {
		List<Map<?,?>> in = list(
			map("f",list("foo")),
			map("f",list("bar")),
			null,
			map(null,list("qux")),
			map("quux",list((Object)null)),
			map(null,list((Object)null))
		);
		assertBeans(run(in, "f=foo"), "f", "[foo]");
	}

	@Test void d08_d2SetOfMapsWithSets() {
		Set<Map<?,?>> in = set(
			map("f",set("foo")),
			map("f",set("bar")),
			null,
			map(null,set("qux")),
			map("quux",set((Object)null)),
			map(null,set((Object)null))
		);
		assertBeans(run(in, "f=foo"), "f", "[foo]");
	}

	@Test void d09_d2ArrayOfMapsWithArrays() {
		Map<?,?>[] in = new Map[]{
			map("f",new Object[]{"foo"}),
			map("f",new Object[]{"bar"}),
			null,
			map(null,new Object[]{"qux"}),
			map("quux",new Object[]{null}),
			map(null,new Object[]{null})
		};
		assertBeans(run(in, "f=foo"), "f", "[foo]");
	}

	@Test void d10_d2ListOfBeans() {
		List<A> in = list(
			A.create("foo"),
			A.create("bar"),
			null,
			A.create(null)
		);
		assertBeans(run(in, "f=foo"), "f", "foo");
	}

	@Test void d11_d3ListOfListOfMaps() {
		List<List<Map<?,?>>> in = list(
			list(map("f","foo")),
			list(map("f","bar")),
			list((Map<?,?>)null),
			list(map(null,"qux")),
			list(map("quux",null)),
			list(map(null,null)),
			null
		);
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}

	@Test void d12_d3SetOfSetOfMaps() {
		Set<Set<Map<?,?>>> in = set(
			set(map("f","foo")),
			set(map("f","bar")),
			set(map("f","baz")),
			set((Map<?,?>)null),
			set(map(null,"qux")),
			set(map("quux",null)),
			set(map(null,null)),
			null
		);
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}

	@Test void d13_d3ArrayOfArrayOfMaps() {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{map("f","foo")},
			new Map[]{map("f","bar")},
			new Map[]{map("f","baz")},
			new Map[]{null},
			new Map[]{map(null,"qux")},
			new Map[]{map("quux",null)},
			new Map[]{map(null,null)},
			null
		};
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}

	@Test void d14_d3ListOfListOfObjects() {
		List<List<Object>> in = list(
			list(map("f","foo")),
			list(map("f","bar")),
			list((Object)null),
			list(map(null,"qux")),
			list(map("quux",null)),
			list(map(null,null)),
			list("xxx"),
			null
		);
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}

	@Test void d15_d3SetOfSetOfObjects() {
		Set<Set<Object>> in = set(
			set(map("f","foo")),
			set(map("f","bar")),
			set((Map<?,?>)null),
			set(map(null,"qux")),
			set(map("quux",null)),
			set(map(null,null)),
			set("xxx"),
			set(123),
			null
		);
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}

	@Test void d16_d3ArrayOfArrayOfObjects() {
		Object[][] in = {
			new Object[]{map("f","foo")},
			new Object[]{map("f","bar")},
			new Object[]{null},
			new Object[]{map(null,"qux")},
			new Object[]{map("quux",null)},
			new Object[]{map(null,null)},
			new Object[]{"xxx"},
			new Object[]{123},
			null
		};
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}

	@Test void d17_d3ListOfListOfMapsWithCollections() {
		List<List<Map<?,?>>> in = list(
			list(map("f",list("foo"))),
			list(map("f",list("bar"))),
			list((Map<?,?>)null),
			list(map(null,list("qux"))),
			list(map("quux",list((Object)null))),
			list(map(null,list((Object)null))),
			null
		);
		assertBeans(run(in, "f=foo"), "#{f}", "[{[foo]}]");
	}

	@Test void d18_d3SetOfSetOfMapsWithCollections() {
		Set<Set<Map<?,?>>> in = set(
			set(map("f",set("foo"))),
			set(map("f",set("bar"))),
			set((Map<?,?>)null),
			set(map(null,set("qux"))),
			set(map("quux",set((Object)null))),
			set(map(null,set((Object)null))),
			null
		);
		assertBeans(run(in, "f=foo"), "#{f}", "[{[foo]}]");
	}

	@Test void d19_d3ArrayOfArrayOfMapsWithCollections() {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{map("f",new Object[]{"foo"})},
			new Map[]{map("f",new Object[]{"bar"})},
			new Map[]{null},
			new Map[]{map(null,new Object[]{"qux"})},
			new Map[]{map("quux",new Object[]{null})},
			new Map[]{map(null,new Object[]{null})},
			null
		};
		assertBeans(run(in, "f=foo"), "#{f}", "[{[foo]}]");
	}

	@Test void d20_d3ArrayOfArrayOfBeans() {
		A[][] in = {
			new A[]{A.create("foo")},
			new A[]{A.create("bar")},
			new A[]{null},
			new A[]{A.create(null)},
			null
		};
		assertBeans(run(in, "f=foo"), "#{f}", "[{foo}]");
	}
}