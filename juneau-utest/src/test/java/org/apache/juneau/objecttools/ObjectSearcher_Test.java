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
package org.apache.juneau.objecttools;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swaps.*;
import org.junit.*;

/**
 * Tests the PojoSearcher class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ObjectSearcher_Test {

	private static BeanSession bs = BeanContext.DEFAULT_SESSION;
	private static ObjectSearcher os = ObjectSearcher.DEFAULT;
	private static WriterSerializer ws = JsonSerializer.create().ssq().swaps(TemporalCalendarSwap.IsoLocalDateTime.class).build();

	//-----------------------------------------------------------------------------------------------------------------
	// Utility
	//-----------------------------------------------------------------------------------------------------------------

	static SearchArgs[] create(String...search) {
		SearchArgs[] sa = new SearchArgs[search.length];
		for (int i = 0; i < search.length; i++)
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
			A a = new A();
			a.f = f;
			return a;
		}
	}

	public static List<A> A_LIST = list(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static Set<A> A_SET = set(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static A[] A_ARRAY = new A[]{A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null)};

	@Test
	public void a01_stringSearch_singleWord() throws Exception {
		assertObject(run(A_LIST, "f=foo")).asJson().is("[{f:'foo'}]");
		assertObject(run(A_SET, "f=foo")).asJson().is("[{f:'foo'}]");
		assertObject(run(A_ARRAY, "f=foo")).asJson().is("[{f:'foo'}]");
		assertObject(os.run(A_LIST, "f=foo")).asJson().is("[{f:'foo'}]");
		assertObject(os.run(A_SET, "f=foo")).asJson().is("[{f:'foo'}]");
		assertObject(os.run(A_ARRAY, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void a02_stringSearch_pattern1() throws Exception {
		assertObject(run(A_LIST, "f=fo*")).asJson().is("[{f:'foo'}]");
		assertObject(run(A_SET, "f=fo*")).asJson().is("[{f:'foo'}]");
		assertObject(run(A_ARRAY, "f=fo*")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void a03_stringSearch_pattern2() throws Exception {
		assertObject(run(A_LIST, "f=*ar")).asJson().is("[{f:'bar'}]");
		assertObject(run(A_SET, "f=*ar")).asJson().is("[{f:'bar'}]");
		assertObject(run(A_ARRAY, "f=*ar")).asJson().is("[{f:'bar'}]");
	}

	@Test
	public void a04_stringSearch_pattern3() throws Exception {
		assertObject(run(A_LIST, "f=?ar")).asJson().is("[{f:'bar'}]");
		assertObject(run(A_SET, "f=?ar")).asJson().is("[{f:'bar'}]");
		assertObject(run(A_ARRAY, "f=?ar")).asJson().is("[{f:'bar'}]");
	}

	@Test
	public void a05_stringSearch_multiple() throws Exception {
		assertObject(run(A_LIST, "f=foo bar q ux")).asJson().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(A_SET, "f=foo bar q ux")).asJson().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(A_ARRAY, "f=foo bar q ux")).asJson().is("[{f:'foo'},{f:'bar'}]");
	}

	@Test
	public void a06_stringSearch_quoted() throws Exception {
		assertObject(run(A_LIST, "f='q ux'")).asJson().is("[{f:'q ux'}]");
		assertObject(run(A_SET, "f='q ux'")).asJson().is("[{f:'q ux'}]");
		assertObject(run(A_ARRAY, "f='q ux'")).asJson().is("[{f:'q ux'}]");
	}

	@Test
	public void a07_stringSearch_quotedWithPattern() throws Exception {
		assertObject(run(A_LIST, "f='q *x'")).asJson().is("[{f:'q ux'}]");
		assertObject(run(A_SET, "f='q *x'")).asJson().is("[{f:'q ux'}]");
		assertObject(run(A_ARRAY, "f='q *x'")).asJson().is("[{f:'q ux'}]");
	}

	@Test
	public void a08_stringSearch_unquotedContainingQuote() throws Exception {
		assertObject(run(A_LIST, "f=qu'ux")).asJson().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_SET, "f=qu'ux")).asJson().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_ARRAY, "f=qu'ux")).asJson().is("[{f:'qu\\'ux'}]");
	}

	@Test
	public void a09_stringSearch_quotedContainingQuote() throws Exception {
		assertObject(run(A_LIST, "f='qu\\'ux'")).asJson().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_SET, "f='qu\\'ux'")).asJson().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_ARRAY, "f='qu\\'ux'")).asJson().is("[{f:'qu\\'ux'}]");
	}

	@Test
	public void a10_stringSearch_regExp() throws Exception {
		assertObject(run(A_LIST, "f=/q\\sux/")).asJson().is("[{f:'q ux'}]");
		assertObject(run(A_SET, "f=/q\\sux/")).asJson().is("[{f:'q ux'}]");
		assertObject(run(A_ARRAY, "f=/q\\sux/")).asJson().is("[{f:'q ux'}]");
	}

	@Test
	public void a11_stringSearch_regExp_noEndSlash() throws Exception {
		Object in = list(A.create("/foo"), A.create("bar"));
		for (String s : a("f=/foo","f='/foo'"))
			assertObject(run(in, s)).asJson().is("[{f:'/foo'}]");
	}

	@Test
	public void a12_stringSearch_regExp_onlySlash() throws Exception {
		Object in = list(A.create("/"), A.create("bar"));
		for (String s : a("f=/", "f='/'"))
			assertObject(run(in, s)).asJson().is("[{f:'/'}]");
	}

	@Test
	public void a13_stringSearch_or_pattern() throws Exception {
		Object in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=f* *r")).asJson().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(in, "f='f* *r'")).asJson().is("[]");
		assertObject(run(in, "f='f*oo'")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void a14_stringSearch_explicit_or_pattern() throws Exception {
		Object in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=^f* ^*r")).asJson().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(in, "f=^'f* *r'")).asJson().is("[]");
		assertObject(run(in, "f=^'f*oo'")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void a15_stringSearch_and_pattern() throws Exception {
		Object in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=+b* +*r")).asJson().is("[{f:'bar'}]");
		assertObject(run(in, "f=+'b*' +'*r'")).asJson().is("[{f:'bar'}]");
	}

	@Test
	public void a16_stringSearch_not_pattern() throws Exception {
		Object in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=b* -*r")).asJson().is("[{f:'baz'}]");
		assertObject(run(in, "f=+'b*' -'*r'")).asJson().is("[{f:'baz'}]");
	}

	@Test
	public void a17_stringSearch_caseSensitive() throws Exception {
		Object in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=F*")).asJson().is("[]");
		assertObject(run(in, "f=\"F*\"")).asJson().is("[]");
		assertObject(run(in, "f='F*'")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void a18_stringSearch_malformedQuotes() throws Exception {
		Object in = list(A.create("'foo"), A.create("\"bar"), A.create("baz"));

		assertThrown(()->run(in, "f='*")).asMessage().isContains("Unmatched string quotes");

		assertThrown(()->run(in, "f=\"*")).asMessage().isContains("Unmatched string quotes");

		assertObject(run(in, "f='\\'*'")).asJson().is("[{f:'\\'foo'}]");
		assertObject(run(in, "f='\"*'")).asJson().is("[{f:'\"bar'}]");
		assertObject(run(in, "f=\"\\\"*\"")).asJson().is("[{f:'\"bar'}]");
	}

	@Test
	public void a19_stringSearch_regexChars() throws Exception {
		Object in = list(A.create("+\\[]{}()^$."), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=*+*")).asJson().is("[{f:'+\\\\[]{}()^$.'}]");
		assertObject(run(in, "f='+\\\\[]{}()^$.'")).asJson().is("[{f:'+\\\\[]{}()^$.'}]");
		assertObject(run(in, "f=++\\\\[]{}()^$.")).asJson().is("[{f:'+\\\\[]{}()^$.'}]");
	}

	@Test
	public void a20_stringSearch_metaChars() throws Exception {
		Object in = list(A.create("*?\\'\""), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f='\\*\\?\\\\\\'\"'")).asJson().is("[{f:'*?\\\\\\'\"'}]");
	}

	@Test
	public void a21_stringSearch_metaChars_escapedQuotes() throws Exception {
		Object in = list(A.create("'"), A.create("\""), A.create("baz"));
		assertObject(run(in, "f=\\'")).asJson().is("[{f:'\\''}]");
		assertObject(run(in, "f=\\\"")).asJson().is("[{f:'\"'}]");
	}

	@Test
	public void a22_stringSearch_metaChars_falseEscape() throws Exception {
		Object in = list(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=\\f\\o\\o")).asJson().is("[{f:'foo'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Number search
	//-----------------------------------------------------------------------------------------------------------------

	public static class C {
		public int f;

		static C create(int f) {
			C c = new C();
			c.f = f;
			return c;
		}
	}

	C[] INT_BEAN_ARRAY = new C[]{C.create(-2), C.create(-1), C.create(0), C.create(1), C.create(2), C.create(3)};

	@Test
	public void b01_intSearch_oneNumber() throws Exception {
		for (String s : a("f=1", "f = 1"))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:1}]");
	}

	@Test
	public void b02_intSearch_twoNumbers() throws Exception {
		for (String s : a("f=1 2", "f = 1  2 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:1},{f:2}]");
	}

	@Test
	public void b03_intSearch_oneNegativeNumber() throws Exception {
		for (String s : a("f=-1", "f = -1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-1}]");
	}

	@Test
	public void b04_intSearch_twoNegativeNumbers() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f=-1 -2")).asJson().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void b05_intSearch_simpleRange() throws Exception {
		for (String s : a("f=1-2", "f = 1 - 2 ", "f = 1- 2 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:1},{f:2}]");
	}

	@Test
	public void b06_intSearch_simpleRange_invalid() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f=2-1")).asJson().is("[]");
	}

	@Test
	public void b07_intSearch_twoNumbersThatLookLikeRange() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = 1 -2 ")).asJson().is("[{f:-2},{f:1}]");
	}

	@Test
	public void b08_intSearch_rangeWithNegativeNumbers() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = -2--1 ")).asJson().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void b09_intSearch_rangeWithNegativeNumbers_invalidRange() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = -1--2 ")).asJson().is("[]");
	}

	@Test
	public void b10_intSearch_multipleRanges() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = 0-1 3-4")).asJson().is("[{f:0},{f:1},{f:3}]");
	}

	@Test
	public void b11_intSearch_overlappingRanges() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = 0-0 2-2")).asJson().is("[{f:0},{f:2}]");
	}

	@Test
	public void b12_intSearch_LT() throws Exception {
		for (String s : a("f = <0", "f<0", "f = < 0 ", "f < 0 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void b13_intSearch_LT_negativeNumber() throws Exception {
		for (String s : a("f = <-1", "f<-1", "f = < -1 ", "f < -1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-2}]");
	}

	@Test
	public void b14_intSearch_GT() throws Exception {
		for (String s : a("f = >1", "f>1", "f = > 1 ", "f > 1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:2},{f:3}]");
	}

	@Test
	public void b15_intSearch_GT_negativeNumber() throws Exception {
		for (String s : a("f = >-1", "f>-1", "f = > -1 ", "f > -1 ", "f =  >  -1  ", "f >  -1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void b16_intSearch_LTE() throws Exception {
		for (String s : a("f = <=0", "f<=0", "f = <= 0 ", "f <= 0 ", "f =  <=  0  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-2},{f:-1},{f:0}]");
	}

	@Test
	public void b17_intSearch_LTE_negativeNumber() throws Exception {
		for (String s : a("f = <=-1", "f <=-1", "f = <= -1 ", "f =  <=  -1  ", "f <=  -1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void b18_intSearch_GTE() throws Exception {
		for (String s : a("f = >=1", "f >=1", "f = >= 1 ", "f >= 1 ", "f =  >=  1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:1},{f:2},{f:3}]");
	}

	@Test
	public void b19_intSearch_GTE_negativeNumber() throws Exception {
		for (String s : a("f = >=-1", "f >=-1", "f = >= -1 ", "f >= -1 ", "f =  >=  -1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-1},{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void b20_intSearch_not_singleNumber() throws Exception {
		for (String s : a("f = !1", "f = ! 1 ", "f =  !  1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-2},{f:-1},{f:0},{f:2},{f:3}]");
	}

	@Test
	public void b21_intSearch_not_range() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = !1-2")).asJson().is("[{f:-2},{f:-1},{f:0},{f:3}]");
	}

	@Test
	public void b22_intSearch_not_range_negativeNumbers() throws Exception {
		for (String s : a("f = !-2--1", "f = ! -2 - -1", "f =  !  -2  -  -1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void b23_intSearch_not_looksLikeRange() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = ! -2 -2")).asJson().is("[{f:-2},{f:-1},{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void b24_intSearch_empty() throws Exception {
		for (String s : a("f=", "f = ", "f =  "))
			assertObject(run(INT_BEAN_ARRAY, s)).asJson().is("[{f:-2},{f:-1},{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void b25_intSearch_badSearches() throws Exception {
		String[] ss = new String[] {
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
			"f=123 -","(ES09)",
		};

		for (int i = 0; i < ss.length; i+=2) {
			final int i2 = i;
			assertThrown(()->run(INT_BEAN_ARRAY, ss[i2])).asMessage().isContains(ss[i+1]);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Date search
	//-----------------------------------------------------------------------------------------------------------------

	public static class B {
		public Calendar f;

		static B[] create(String...dates) {
			B[] bb = new B[dates.length];
			for (int i = 0; i < dates.length; i++) {
				bb[i] = new B();
				bb[i].f = DateUtils.parseISO8601Calendar(dates[i]);
			}
			return bb;
		}
	}

	@Test
	public void c01_dateSearch_singleDate_y() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		for (String s : a(
				"f=2011",
				"f = 2011 ",
				"f = '2011' ",
				"f = \"2011\" "
			))
			assertObject(run(in, s)).asString(ws).is("[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]");
	}

	@Test
	public void c02_dateSearch_singleDate_ym() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		for (String s : a(
				"f=2011-01",
				"f = 2011-01 ",
				"f='2011-01'",
				"f=\"2011-01\""
			))
			assertObject(run(in, s)).asString(ws).is("[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]");
	}

	@Test
	public void c03_dateSearch_singleDate_ymd() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		assertObject(run(in, "f=2011-01-01")).asString(ws).is("[{f:'2011-01-01T00:00:00'}]");
	}


	@Test
	public void c04_dateSearch_singleDate_ymdh() throws Exception {
		B[] in = B.create("2011-01-01T11:15:59", "2011-01-01T12:00:00", "2011-01-01T12:59:59", "2011-01-01T13:00:00");
		assertObject(run(in, "f=2011-01-01T12")).asString(ws).is("[{f:'2011-01-01T12:00:00'},{f:'2011-01-01T12:59:59'}]");
	}

	@Test
	public void c05_dateSearch_singleDate_ymdhm() throws Exception {
		B[] in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00", "2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertObject(run(in, "f=2011-01-01T12:30")).asString(ws).is("[{f:'2011-01-01T12:30:00'},{f:'2011-01-01T12:30:59'}]");
	}

	@Test
	public void c06_dateSearch_singleDate_ymdhms() throws Exception {
		B[] in = B.create("2011-01-01T12:30:29", "2011-01-01T12:30:30", "2011-01-01T12:30:31");
		assertObject(run(in, "f=2011-01-01T12:30:30")).asString(ws).is("[{f:'2011-01-01T12:30:30'}]");
	}

	@Test
	public void c07_dateSearch_openEndedRanges_y() throws Exception {
		B[] in = B.create("2000-12-31", "2001-01-01");
		for (String s : a(
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
			assertObject(run(in, s)).asString(ws).is("[{f:'2001-01-01T00:00:00'}]");
		for (String s : a(
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
			assertObject(run(in, s)).asString(ws).is("[{f:'2000-12-31T00:00:00'}]");
	}

	@Test
	public void c08_dateSearch_openEndedRanges_toMinute() throws Exception {
		B[] in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00");
		assertObject(run(in, "f>=2011-01-01T12:30")).asString(ws).is("[{f:'2011-01-01T12:30:00'}]");
		assertObject(run(in, "f<2011-01-01T12:30")).asString(ws).is("[{f:'2011-01-01T12:29:59'}]");
	}

	@Test
	public void c09_dateSearch_openEndedRanges_toSecond() throws Exception {
		B[] in = B.create("2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertObject(run(in, "f>2011-01-01T12:30")).asString(ws).is("[{f:'2011-01-01T12:31:00'}]");
		assertObject(run(in, "f<=2011-01-01T12:30")).asString(ws).is("[{f:'2011-01-01T12:30:59'}]");
	}

	@Test
	public void c10_dateSearch_closedRanges() throws Exception {
		B[] in = B.create("2000-12-31T23:59:59", "2001-01-01T00:00:00", "2003-06-30T23:59:59", "2003-07-01T00:00:00");

		for (String s : a(
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
			assertObject(run(in, s)).asString(ws).is("[{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]");

		for (String s : a(
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
			assertObject(run(in, s)).asString(ws).is("[{f:'2000-12-31T23:59:59'},{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]");
	}

	@Test
	public void c11_dateSearch_or1() throws Exception {
		B[] in = B.create("2000-12-31", "2001-01-01", "2001-12-31", "2002-01-01");
		for (String s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertObject(run(in, s)).asString(ws).is("[{f:'2001-01-01T00:00:00'},{f:'2001-12-31T00:00:00'}]");
	}

	@Test
	public void c12_dateSearch_or2() throws Exception {
		B[] in = B.create("2002-12-31", "2003-01-01", "2003-12-31", "2004-01-01");
		for (String s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertObject(run(in, s)).asString(ws).is("[{f:'2003-01-01T00:00:00'},{f:'2003-12-31T00:00:00'}]");
	}

	@Test
	public void c13_dateSearch_or3() throws Exception {
		B[] in = B.create("2004-12-31", "2005-01-01", "2005-12-31", "2006-01-01");
		for (String s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertObject(run(in, s)).asString(ws).is("[{f:'2005-01-01T00:00:00'},{f:'2005-12-31T00:00:00'}]");
	}

	@Test
	public void c14_dateSearch_or_singleAndRange() throws Exception {
		B[] in = B.create("2000-12-31", "2001-01-01", "2002-12-31", "2003-01-01");
		for (String s : a(
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
			assertObject(run(in, s)).asString(ws).is("[{f:'2001-01-01T00:00:00'},{f:'2003-01-01T00:00:00'}]");
		for (String s : a(
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
			assertObject(run(in, s)).asString(ws).is("[{f:'2000-12-31T00:00:00'},{f:'2003-01-01T00:00:00'}]");
	}

	@Test
	@Ignore /* TODO - Fix me */
	public void c15_dateSearch_badSearches() throws Exception {
		B[] in = B.create("2000-12-31");
		String[] ss = new String[] {
			"f=X","(S01)",
			"f=>X","(S02)",
			"f=<X","(S03)",
			"f=>=X","(S04)",
			"f='1'X","(S07)",
			"f=2000 X","(S09)",
			"f=2000-X","(S10)",
			"f=>","(ES02)",
			"f=<","(ES03)",
			"f=>=","(ES04)",
			"f='","(ES05)",
			"f=\"","(ES06)",
			"f=2000-","(ES10)",
			"f=2000-'","(ES11)",
			"f=2000-\"","(ES12)"
		};

		for (int i = 0; i < ss.length; i+=2) {
			final int i2 = i;
			assertThrown(()->run(in, ss[i2])).asMessage().isContains(ss[i+1]);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other data structures.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_d2ListOfMaps() throws Exception {
		List<Map<?,?>> in = list(
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null)
		);
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void d02_d2SetOfMaps() throws Exception {
		Set<Map<?,?>> in = set(
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null)
		);
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}


	@Test
	public void d03_d2ArrayOfMaps() throws Exception {
		Map<?,?>[] in = new Map[]{
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null)
		};
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void d04_d2ListOfObjects() throws Exception {
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
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void d05_d2SetOfObjects() throws Exception {
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
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void d06_d2ArrayOfObjects() throws Exception {
		Object[] in = new Object[]{
			map("f","foo"),
			map("f","bar"),
			null,
			map(null,"qux"),
			map("quux",null),
			map(null,null),
			"xxx",
			123
		};
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void d07_d2ListOfMapsWithLists() throws Exception {
		List<Map<?,?>> in = list(
			map("f",list("foo")),
			map("f",list("bar")),
			null,
			map(null,list("qux")),
			map("quux",list((Object)null)),
			map(null,list((Object)null))
		);
		assertObject(run(in, "f=foo")).asJson().is("[{f:['foo']}]");
	}

	@Test
	public void d08_d2SetOfMapsWithSets() throws Exception {
		Set<Map<?,?>> in = set(
			map("f",set("foo")),
			map("f",set("bar")),
			null,
			map(null,set("qux")),
			map("quux",set((Object)null)),
			map(null,set((Object)null))
		);
		assertObject(run(in, "f=foo")).asJson().is("[{f:['foo']}]");
	}

	@Test
	public void d09_d2ArrayOfMapsWithArrays() throws Exception {
		Map<?,?>[] in = new Map[]{
			map("f",new Object[]{"foo"}),
			map("f",new Object[]{"bar"}),
			null,
			map(null,new Object[]{"qux"}),
			map("quux",new Object[]{null}),
			map(null,new Object[]{null})
		};
		assertObject(run(in, "f=foo")).asJson().is("[{f:['foo']}]");
	}

	@Test
	public void d10_d2ListOfBeans() throws Exception {
		List<A> in = list(
			A.create("foo"),
			A.create("bar"),
			null,
			A.create(null)
		);
		assertObject(run(in, "f=foo")).asJson().is("[{f:'foo'}]");
	}

	@Test
	public void d11_d3ListOfListOfMaps() throws Exception {
		List<List<Map<?,?>>> in = list(
			list(map("f","foo")),
			list(map("f","bar")),
			list((Map<?,?>)null),
			list(map(null,"qux")),
			list(map("quux",null)),
			list(map(null,null)),
			null
		);
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	@Test
	public void d12_d3SetOfSetOfMaps() throws Exception {
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
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	@Test
	public void d13_d3ArrayOfArrayOfMaps() throws Exception {
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
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	@Test
	public void d14_d3ListOfListOfObjects() throws Exception {
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
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	@Test
	public void d15_d3SetOfSetOfObjects() throws Exception {
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
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	@Test
	public void d16_d3ArrayOfArrayOfObjects() throws Exception {
		Object[][] in = new Object[][]{
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
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	@Test
	public void d17_d3ListOfListOfMapsWithCollections() throws Exception {
		List<List<Map<?,?>>> in = list(
			list(map("f",list("foo"))),
			list(map("f",list("bar"))),
			list((Map<?,?>)null),
			list(map(null,list("qux"))),
			list(map("quux",list((Object)null))),
			list(map(null,list((Object)null))),
			null
		);
		assertObject(run(in, "f=foo")).asJson().is("[[{f:['foo']}]]");
	}

	@Test
	public void d18_d3SetOfSetOfMapsWithCollections() throws Exception {
		Set<Set<Map<?,?>>> in = set(
			set(map("f",set("foo"))),
			set(map("f",set("bar"))),
			set((Map<?,?>)null),
			set(map(null,set("qux"))),
			set(map("quux",set((Object)null))),
			set(map(null,set((Object)null))),
			null
		);
		assertObject(run(in, "f=foo")).asJson().is("[[{f:['foo']}]]");
	}

	@Test
	public void d19_d3ArrayOfArrayOfMapsWithCollections() throws Exception {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{map("f",new Object[]{"foo"})},
			new Map[]{map("f",new Object[]{"bar"})},
			new Map[]{null},
			new Map[]{map(null,new Object[]{"qux"})},
			new Map[]{map("quux",new Object[]{null})},
			new Map[]{map(null,new Object[]{null})},
			null
		};
		assertObject(run(in, "f=foo")).asJson().is("[[{f:['foo']}]]");
	}

	@Test
	public void d20_d3ArrayOfArrayOfBeans() throws Exception {
		A[][] in = new A[][]{
			new A[]{A.create("foo")},
			new A[]{A.create("bar")},
			new A[]{null},
			new A[]{A.create(null)},
			null
		};
		assertObject(run(in, "f=foo")).asJson().is("[[{f:'foo'}]]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other tests
	//-----------------------------------------------------------------------------------------------------------------

//	@Test
//	public void noSearchArgs() {
//		SearchArgs sa = SearchArgs.create();
//		assertObjectEquals("'foo'", run("foo", sa));
//	}
//
//	@Test
//	public void invalidSearchArgs() {
//		for (String s : a("", "x")) {
//			try {
//				run("foo", s);
//				fail();
//			} catch (PatternException e) {
//				assertTrue(e.getLocalizedMessage().contains("Invalid search terms"));
//			}
//		}
//		SearchArgs sa = SearchArgs.create();
//		assertObjectEquals("'foo'", run("foo", sa));
//	}
//
//	@Test
//	public void not2dPojo() {
//		assertObjectEquals("'foo'", run("foo", "x=y"));
//	}
//
//	@Test
//	public void nullInput() {
//		assertObjectEquals("null", run(null, "x=y"));
//	}
//
//	@Test
//	public void searchArgsEmptyKey() {
//		SearchArgs sa = SearchArgs.create().append(null, "foo");
//		assertObjectEquals("'foo'", run("foo", sa));
//	}
//
//	@Test
//	public void searchArgsEmptyValue() {
//		SearchArgs sa = SearchArgs.create().append("foo", null);
//		assertObjectEquals("'foo'", run("foo", sa));
//	}
}