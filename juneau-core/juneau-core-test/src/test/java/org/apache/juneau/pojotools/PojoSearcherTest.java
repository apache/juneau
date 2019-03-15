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
package org.apache.juneau.pojotools;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the PojoSearcher class.
 */
public class PojoSearcherTest {

	private static BeanSession bs = BeanContext.DEFAULT.createSession();
	private static PojoSearcher ps = PojoSearcher.DEFAULT;
	private static WriterSerializer ws = JsonSerializer.create().ssq().pojoSwaps(CalendarSwap.DateTimeSimple.class).build();

	//-----------------------------------------------------------------------------------------------------------------
	// Utility
	//-----------------------------------------------------------------------------------------------------------------

	static SearchArgs[] create(String...search) {
		SearchArgs[] sa = new SearchArgs[search.length];
		for (int i = 0; i < search.length; i++)
			sa[i] = new SearchArgs(search[i]);
		return sa;
	}

	static SearchArgs create(String search) {
		return new SearchArgs(search);
	}

	static Object run(Object in, String search) {
		return ps.run(bs, in, create(search));
	}

	static Object run(Object in, SearchArgs sa) {
		return ps.run(bs, in, sa);
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

	public static List<A> A_LIST = AList.create(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static Set<A> A_SET = ASet.create(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static A[] A_ARRAY = new A[]{A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null)};

	@Test
	public void stringSearch_singleWord() throws Exception {
		assertObjectEquals("[{f:'foo'}]", run(A_LIST, "f=foo"));
		assertObjectEquals("[{f:'foo'}]", run(A_SET, "f=foo"));
		assertObjectEquals("[{f:'foo'}]", run(A_ARRAY, "f=foo"));
	}

	@Test
	public void stringSearch_pattern1() throws Exception {
		assertObjectEquals("[{f:'foo'}]", run(A_LIST, "f=fo*"));
		assertObjectEquals("[{f:'foo'}]", run(A_SET, "f=fo*"));
		assertObjectEquals("[{f:'foo'}]", run(A_ARRAY, "f=fo*"));
	}

	@Test
	public void stringSearch_pattern2() throws Exception {
		assertObjectEquals("[{f:'bar'}]", run(A_LIST, "f=*ar"));
		assertObjectEquals("[{f:'bar'}]", run(A_SET, "f=*ar"));
		assertObjectEquals("[{f:'bar'}]", run(A_ARRAY, "f=*ar"));
	}

	@Test
	public void stringSearch_pattern3() throws Exception {
		assertObjectEquals("[{f:'bar'}]", run(A_LIST, "f=?ar"));
		assertObjectEquals("[{f:'bar'}]", run(A_SET, "f=?ar"));
		assertObjectEquals("[{f:'bar'}]", run(A_ARRAY, "f=?ar"));
	}

	@Test
	public void stringSearch_multiple() throws Exception {
		assertObjectEquals("[{f:'foo'},{f:'bar'}]", run(A_LIST, "f=foo bar q ux"));
		assertObjectEquals("[{f:'foo'},{f:'bar'}]", run(A_SET, "f=foo bar q ux"));
		assertObjectEquals("[{f:'foo'},{f:'bar'}]", run(A_ARRAY, "f=foo bar q ux"));
	}

	@Test
	public void stringSearch_quoted() throws Exception {
		assertObjectEquals("[{f:'q ux'}]", run(A_LIST, "f='q ux'"));
		assertObjectEquals("[{f:'q ux'}]", run(A_SET, "f='q ux'"));
		assertObjectEquals("[{f:'q ux'}]", run(A_ARRAY, "f='q ux'"));
	}

	@Test
	public void stringSearch_quotedWithPattern() throws Exception {
		assertObjectEquals("[{f:'q ux'}]", run(A_LIST, "f='q *x'"));
		assertObjectEquals("[{f:'q ux'}]", run(A_SET, "f='q *x'"));
		assertObjectEquals("[{f:'q ux'}]", run(A_ARRAY, "f='q *x'"));
	}

	@Test
	public void stringSearch_unquotedContainingQuote() throws Exception {
		assertObjectEquals("[{f:'qu\\'ux'}]", run(A_LIST, "f=qu'ux"));
		assertObjectEquals("[{f:'qu\\'ux'}]", run(A_SET, "f=qu'ux"));
		assertObjectEquals("[{f:'qu\\'ux'}]", run(A_ARRAY, "f=qu'ux"));
	}

	@Test
	public void stringSearch_quotedContainingQuote() throws Exception {
		assertObjectEquals("[{f:'qu\\'ux'}]", run(A_LIST, "f='qu\\'ux'"));
		assertObjectEquals("[{f:'qu\\'ux'}]", run(A_SET, "f='qu\\'ux'"));
		assertObjectEquals("[{f:'qu\\'ux'}]", run(A_ARRAY, "f='qu\\'ux'"));
	}

	@Test
	public void stringSearch_regExp() throws Exception {
		assertObjectEquals("[{f:'q ux'}]", run(A_LIST, "f=/q\\sux/"));
		assertObjectEquals("[{f:'q ux'}]", run(A_SET, "f=/q\\sux/"));
		assertObjectEquals("[{f:'q ux'}]", run(A_ARRAY, "f=/q\\sux/"));
	}

	@Test
	public void stringSearch_regExp_noEndSlash() throws Exception {
		Object in = AList.create(A.create("/foo"), A.create("bar"));
		for (String s : a("f=/foo","f='/foo'")) // Not a regex.
			assertObjectEquals("[{f:'/foo'}]", run(in, s));
	}

	@Test
	public void stringSearch_regExp_onlySlash() throws Exception {
		Object in = AList.create(A.create("/"), A.create("bar"));
		for (String s : a("f=/", "f='/'")) // Not a regex.
			assertObjectEquals("[{f:'/'}]", run(in, s));
	}

	@Test
	public void stringSearch_or_pattern() throws Exception {
		Object in = AList.create(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'foo'},{f:'bar'}]", run(in, "f=f* *r"));
		assertObjectEquals("[]", run(in, "f='f* *r'"));
		assertObjectEquals("[{f:'foo'}]", run(in, "f='f*oo'"));
	}

	@Test
	public void stringSearch_explicit_or_pattern() throws Exception {
		Object in = AList.create(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'foo'},{f:'bar'}]", run(in, "f=^f* ^*r"));
		assertObjectEquals("[]", run(in, "f=^'f* *r'"));
		assertObjectEquals("[{f:'foo'}]", run(in, "f=^'f*oo'"));
	}

	@Test
	public void stringSearch_and_pattern() throws Exception {
		Object in = AList.create(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'bar'}]", run(in, "f=+b* +*r"));
		assertObjectEquals("[{f:'bar'}]", run(in, "f=+'b*' +'*r'"));
	}

	@Test
	public void stringSearch_not_pattern() throws Exception {
		Object in = AList.create(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'baz'}]", run(in, "f=b* -*r"));
		assertObjectEquals("[{f:'baz'}]", run(in, "f=+'b*' -'*r'"));
	}

	@Test
	public void stringSearch_caseSensitive() throws Exception {
		Object in = AList.create(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObjectEquals("[]", run(in, "f=F*"));
		assertObjectEquals("[]", run(in, "f=\"F*\""));
		assertObjectEquals("[{f:'foo'}]", run(in, "f='F*'"));
	}

	@Test
	public void stringSearch_malformedQuotes() throws Exception {
		Object in = AList.create(A.create("'foo"), A.create("\"bar"), A.create("baz"));

		try {
			run(in, "f='*");
			fail();
		} catch (Exception e) {
			assertTrue(e.getLocalizedMessage().contains("Unmatched string quotes"));
		}

		try {
			run(in, "f=\"*");
			fail();
		} catch (Exception e) {
			assertTrue(e.getLocalizedMessage().contains("Unmatched string quotes"));
		}

		assertObjectEquals("[{f:'\\'foo'}]", run(in, "f='\\'*'"));
		assertObjectEquals("[{f:'\"bar'}]", run(in, "f='\"*'"));
		assertObjectEquals("[{f:'\"bar'}]", run(in, "f=\"\\\"*\""));
	}

	@Test
	public void stringSearch_regexChars() throws Exception {
		Object in = AList.create(A.create("+\\[]{}()^$."), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'+\\\\[]{}()^$.'}]", run(in, "f=*+*"));
		assertObjectEquals("[{f:'+\\\\[]{}()^$.'}]", run(in, "f='+\\\\[]{}()^$.'"));
		assertObjectEquals("[{f:'+\\\\[]{}()^$.'}]", run(in, "f=++\\\\[]{}()^$."));
	}

	@Test
	public void stringSearch_metaChars() throws Exception {
		Object in = AList.create(A.create("*?\\'\""), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'*?\\\\\\'\"'}]", run(in, "f='\\*\\?\\\\\\'\"'"));
	}

	@Test
	public void stringSearch_metaChars_escapedQuotes() throws Exception {
		Object in = AList.create(A.create("'"), A.create("\""), A.create("baz"));
		assertObjectEquals("[{f:'\\''}]", run(in, "f=\\'"));
		assertObjectEquals("[{f:'\"'}]", run(in, "f=\\\""));
	}

	@Test
	public void stringSearch_metaChars_falseEscape() throws Exception {
		Object in = AList.create(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObjectEquals("[{f:'foo'}]", run(in, "f=\\f\\o\\o"));
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
	public void intSearch_oneNumber() throws Exception {
		for (String s : a("f=1", "f = 1"))
			assertObjectEquals("[{f:1}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_twoNumbers() throws Exception {
		for (String s : a("f=1 2", "f = 1  2 "))
			assertObjectEquals("[{f:1},{f:2}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_oneNegativeNumber() throws Exception {
		for (String s : a("f=-1", "f = -1 "))
			assertObjectEquals("[{f:-1}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_twoNegativeNumbers() throws Exception {
		assertObjectEquals("[{f:-2},{f:-1}]", run(INT_BEAN_ARRAY, "f=-1 -2"));
	}

	@Test
	public void intSearch_simpleRange() throws Exception {
		for (String s : a("f=1-2", "f = 1 - 2 ", "f = 1- 2 "))
			assertObjectEquals("[{f:1},{f:2}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_simpleRange_invalid() throws Exception {
		assertObjectEquals("[]", run(INT_BEAN_ARRAY, "f=2-1"));
	}

	@Test
	public void intSearch_twoNumbersThatLookLikeRange() throws Exception {
		assertObjectEquals("[{f:-2},{f:1}]", run(INT_BEAN_ARRAY, "f = 1 -2 "));
	}

	@Test
	public void intSearch_rangeWithNegativeNumbers() throws Exception {
		assertObjectEquals("[{f:-2},{f:-1}]", run(INT_BEAN_ARRAY, "f = -2--1 "));
	}

	@Test
	public void intSearch_rangeWithNegativeNumbers_invalidRange() throws Exception {
		assertObjectEquals("[]", run(INT_BEAN_ARRAY, "f = -1--2 "));
	}

	@Test
	public void intSearch_multipleRanges() throws Exception {
		assertObjectEquals("[{f:0},{f:1},{f:3}]", run(INT_BEAN_ARRAY, "f = 0-1 3-4"));
	}

	@Test
	public void intSearch_overlappingRanges() throws Exception {
		assertObjectEquals("[{f:0},{f:2}]", run(INT_BEAN_ARRAY, "f = 0-0 2-2"));
	}

	@Test
	public void intSearch_LT() throws Exception {
		for (String s : a("f = <0", "f<0", "f = < 0 ", "f < 0 "))
			assertObjectEquals("[{f:-2},{f:-1}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_LT_negativeNumber() throws Exception {
		for (String s : a("f = <-1", "f<-1", "f = < -1 ", "f < -1 "))
			assertObjectEquals("[{f:-2}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_GT() throws Exception {
		for (String s : a("f = >1", "f>1", "f = > 1 ", "f > 1 "))
			assertObjectEquals("[{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_GT_negativeNumber() throws Exception {
		for (String s : a("f = >-1", "f>-1", "f = > -1 ", "f > -1 ", "f =  >  -1  ", "f >  -1  "))
			assertObjectEquals("[{f:0},{f:1},{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_LTE() throws Exception {
		for (String s : a("f = <=0", "f<=0", "f = <= 0 ", "f <= 0 ", "f =  <=  0  "))
			assertObjectEquals("[{f:-2},{f:-1},{f:0}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_LTE_negativeNumber() throws Exception {
		for (String s : a("f = <=-1", "f <=-1", "f = <= -1 ", "f =  <=  -1  ", "f <=  -1  "))
			assertObjectEquals("[{f:-2},{f:-1}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_GTE() throws Exception {
		for (String s : a("f = >=1", "f >=1", "f = >= 1 ", "f >= 1 ", "f =  >=  1  "))
			assertObjectEquals("[{f:1},{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_GTE_negativeNumber() throws Exception {
		for (String s : a("f = >=-1", "f >=-1", "f = >= -1 ", "f >= -1 ", "f =  >=  -1  "))
			assertObjectEquals("[{f:-1},{f:0},{f:1},{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_not_singleNumber() throws Exception {
		for (String s : a("f = !1", "f = ! 1 ", "f =  !  1  "))
			assertObjectEquals("[{f:-2},{f:-1},{f:0},{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_not_range() throws Exception {
		assertObjectEquals("[{f:-2},{f:-1},{f:0},{f:3}]", run(INT_BEAN_ARRAY, "f = !1-2"));
	}

	@Test
	public void intSearch_not_range_negativeNumbers() throws Exception {
		for (String s : a("f = !-2--1", "f = ! -2 - -1", "f =  !  -2  -  -1 "))
			assertObjectEquals("[{f:0},{f:1},{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_not_looksLikeRange() throws Exception {
		assertObjectEquals("[{f:-2},{f:-1},{f:0},{f:1},{f:2},{f:3}]", run(INT_BEAN_ARRAY, "f = ! -2 -2"));
	}

	@Test
	public void intSearch_empty() throws Exception {
		for (String s : a("f=", "f = ", "f =  "))
			assertObjectEquals("[{f:-2},{f:-1},{f:0},{f:1},{f:2},{f:3}]", run(INT_BEAN_ARRAY, s));
	}

	@Test
	public void intSearch_badSearches() throws Exception {
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
			try {
				run(INT_BEAN_ARRAY, ss[i]);
				fail("i=" + i);
			} catch (PatternException e) {
				assertTrue(e.getLocalizedMessage().contains(ss[i+1]));
			}
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
	public void dateSearch_singleDate_y() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		for (String s : a(
				"f=2011",
				"f = 2011 ",
				"f = '2011' ",
				"f = \"2011\" "
			))
			assertObjectEquals("[{f:'2011/01/01 00:00:00'},{f:'2011/01/31 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_singleDate_ym() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		for (String s : a(
				"f=2011-01",
				"f = 2011-01 ",
				"f='2011-01'",
				"f=\"2011-01\""
			))
			assertObjectEquals("[{f:'2011/01/01 00:00:00'},{f:'2011/01/31 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_singleDate_ymd() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		assertObjectEquals("[{f:'2011/01/01 00:00:00'}]", run(in, "f=2011-01-01"), ws);
	}


	@Test
	public void dateSearch_singleDate_ymdh() throws Exception {
		B[] in = B.create("2011-01-01T11:15:59", "2011-01-01T12:00:00", "2011-01-01T12:59:59", "2011-01-01T13:00:00");
		assertObjectEquals("[{f:'2011/01/01 12:00:00'},{f:'2011/01/01 12:59:59'}]", run(in, "f=2011-01-01T12"), ws);
	}

	@Test
	public void dateSearch_singleDate_ymdhm() throws Exception {
		B[] in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00", "2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertObjectEquals("[{f:'2011/01/01 12:30:00'},{f:'2011/01/01 12:30:59'}]", run(in, "f=2011-01-01T12:30"), ws);
	}

	@Test
	public void dateSearch_singleDate_ymdhms() throws Exception {
		B[] in = B.create("2011-01-01T12:30:29", "2011-01-01T12:30:30", "2011-01-01T12:30:31");
		assertObjectEquals("[{f:'2011/01/01 12:30:30'}]", run(in, "f=2011-01-01T12:30:30"), ws);
	}

	@Test
	public void dateSearch_openEndedRanges_y() throws Exception {
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
			assertObjectEquals("[{f:'2001/01/01 00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2000/12/31 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_openEndedRanges_toMinute() throws Exception {
		B[] in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00");
		assertObjectEquals("[{f:'2011/01/01 12:30:00'}]", run(in, "f>=2011-01-01T12:30"), ws);
		assertObjectEquals("[{f:'2011/01/01 12:29:59'}]", run(in, "f<2011-01-01T12:30"), ws);
	}

	@Test
	public void dateSearch_openEndedRanges_toSecond() throws Exception {
		B[] in = B.create("2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertObjectEquals("[{f:'2011/01/01 12:31:00'}]", run(in, "f>2011-01-01T12:30"), ws);
		assertObjectEquals("[{f:'2011/01/01 12:30:59'}]", run(in, "f<=2011-01-01T12:30"), ws);
	}

	@Test
	public void dateSearch_closedRanges() throws Exception {
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
			assertObjectEquals("[{f:'2001/01/01 00:00:00'},{f:'2003/06/30 23:59:59'}]", run(in, s), ws);

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
		assertObjectEquals("[{f:'2000/12/31 23:59:59'},{f:'2001/01/01 00:00:00'},{f:'2003/06/30 23:59:59'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_or1() throws Exception {
		B[] in = B.create("2000-12-31", "2001-01-01", "2001-12-31", "2002-01-01");
		for (String s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertObjectEquals("[{f:'2001/01/01 00:00:00'},{f:'2001/12/31 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_or2() throws Exception {
		B[] in = B.create("2002-12-31", "2003-01-01", "2003-12-31", "2004-01-01");
		for (String s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertObjectEquals("[{f:'2003/01/01 00:00:00'},{f:'2003/12/31 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_or3() throws Exception {
		B[] in = B.create("2004-12-31", "2005-01-01", "2005-12-31", "2006-01-01");
		for (String s : a(
				"f=2001 2003 2005",
				"f= 2001  2003  2005 ",
				"f='2001' '2003' '2005'",
				"f= '2001'  '2003'  '2005' ",
				"f=\"2001\" \"2003\" \"2005\"",
				"f= \"2001\"  \"2003\"  \"2005\" "
			))
			assertObjectEquals("[{f:'2005/01/01 00:00:00'},{f:'2005/12/31 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_or_singleAndRange() throws Exception {
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
			assertObjectEquals("[{f:'2001/01/01 00:00:00'},{f:'2003/01/01 00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2000/12/31 00:00:00'},{f:'2003/01/01 00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_badSearches() throws Exception {
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
			try {
				run(in, ss[i]);
			} catch (PatternException e) {
				assertTrue("["+e.getLocalizedMessage()+"]!=["+ss[i]+"]", e.getLocalizedMessage().contains(ss[i+1]));
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other data structures.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d2ListOfMaps() throws Exception {
		List<Map<?,?>> in = AList.create(
			AMap.create("f","foo"),
			AMap.create("f","bar"),
			null,
			AMap.create(null,"qux"),
			AMap.create("quux",null),
			AMap.create(null,null)
		);
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}

	@Test
	public void d2SetOfMaps() throws Exception {
		Set<Map<?,?>> in = ASet.create(
			AMap.create("f","foo"),
			AMap.create("f","bar"),
			null,
			AMap.create(null,"qux"),
			AMap.create("quux",null),
			AMap.create(null,null)
		);
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}


	@Test
	public void d2ArrayOfMaps() throws Exception {
		Map<?,?>[] in = new Map[]{
			AMap.create("f","foo"),
			AMap.create("f","bar"),
			null,
			AMap.create(null,"qux"),
			AMap.create("quux",null),
			AMap.create(null,null)
		};
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}

	@Test
	public void d2ListOfObjects() throws Exception {
		List<Object> in = AList.create(
			AMap.create("f","foo"),
			AMap.create("f","bar"),
			null,
			AMap.create(null,"qux"),
			AMap.create("quux",null),
			AMap.create(null,null),
			"xxx",
			123
		);
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}

	@Test
	public void d2SetOfObjects() throws Exception {
		Set<Object> in = ASet.create(
			AMap.create("f","foo"),
			AMap.create("f","bar"),
			null,
			AMap.create(null,"qux"),
			AMap.create("quux",null),
			AMap.create(null,null),
			"xxx",
			123
		);
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}

	@Test
	public void d2ArrayOfObjects() throws Exception {
		Object[] in = new Object[]{
			AMap.create("f","foo"),
			AMap.create("f","bar"),
			null,
			AMap.create(null,"qux"),
			AMap.create("quux",null),
			AMap.create(null,null),
			"xxx",
			123
		};
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}

	@Test
	public void d2ListOfMapsWithLists() throws Exception {
		List<Map<?,?>> in = AList.create(
			AMap.create("f",AList.create("foo")),
			AMap.create("f",AList.create("bar")),
			null,
			AMap.create(null,AList.create("qux")),
			AMap.create("quux",AList.create((Object)null)),
			AMap.create(null,AList.create((Object)null))
		);
		assertObjectEquals("[{f:['foo']}]", run(in, "f=foo"));
	}

	@Test
	public void d2SetOfMapsWithSets() throws Exception {
		Set<Map<?,?>> in = ASet.create(
			AMap.create("f",ASet.create("foo")),
			AMap.create("f",ASet.create("bar")),
			null,
			AMap.create(null,ASet.create("qux")),
			AMap.create("quux",ASet.create((Object)null)),
			AMap.create(null,ASet.create((Object)null))
		);
		assertObjectEquals("[{f:['foo']}]", run(in, "f=foo"));
	}

	@Test
	public void d2ArrayOfMapsWithArrays() throws Exception {
		Map<?,?>[] in = new Map[]{
			AMap.create("f",new Object[]{"foo"}),
			AMap.create("f",new Object[]{"bar"}),
			null,
			AMap.create(null,new Object[]{"qux"}),
			AMap.create("quux",new Object[]{null}),
			AMap.create(null,new Object[]{null})
		};
		assertObjectEquals("[{f:['foo']}]", run(in, "f=foo"));
	}

	@Test
	public void d2ListOfBeans() throws Exception {
		List<A> in = AList.create(
			A.create("foo"),
			A.create("bar"),
			null,
			A.create(null)
		);
		assertObjectEquals("[{f:'foo'}]", run(in, "f=foo"));
	}

	@Test
	public void d3ListOfListOfMaps() throws Exception {
		List<List<Map<?,?>>> in = AList.create(
			AList.create(AMap.create("f","foo")),
			AList.create(AMap.create("f","bar")),
			AList.create((Map<?,?>)null),
			AList.create(AMap.create(null,"qux")),
			AList.create(AMap.create("quux",null)),
			AList.create(AMap.create(null,null)),
			null
		);
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	@Test
	public void d3SetOfSetOfMaps() throws Exception {
		Set<Set<Map<?,?>>> in = ASet.create(
			ASet.create(AMap.create("f","foo")),
			ASet.create(AMap.create("f","bar")),
			ASet.create(AMap.create("f","baz")),
			ASet.create((Map<?,?>)null),
			ASet.create(AMap.create(null,"qux")),
			ASet.create(AMap.create("quux",null)),
			ASet.create(AMap.create(null,null)),
			null
		);
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	@Test
	public void d3ArrayOfArrayOfMaps() throws Exception {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{AMap.create("f","foo")},
			new Map[]{AMap.create("f","bar")},
			new Map[]{AMap.create("f","baz")},
			new Map[]{null},
			new Map[]{AMap.create(null,"qux")},
			new Map[]{AMap.create("quux",null)},
			new Map[]{AMap.create(null,null)},
			null
		};
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	@Test
	public void d3ListOfListOfObjects() throws Exception {
		List<List<Object>> in = AList.create(
			AList.create(AMap.create("f","foo")),
			AList.create(AMap.create("f","bar")),
			AList.create((Object)null),
			AList.create(AMap.create(null,"qux")),
			AList.create(AMap.create("quux",null)),
			AList.create(AMap.create(null,null)),
			AList.create("xxx"),
			AList.create(123),
			null
		);
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	@Test
	public void d3SetOfSetOfObjects() throws Exception {
		Set<Set<Object>> in = ASet.create(
			ASet.create(AMap.create("f","foo")),
			ASet.create(AMap.create("f","bar")),
			ASet.create((Map<?,?>)null),
			ASet.create(AMap.create(null,"qux")),
			ASet.create(AMap.create("quux",null)),
			ASet.create(AMap.create(null,null)),
			ASet.create("xxx"),
			ASet.create(123),
			null
		);
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	@Test
	public void d3ArrayOfArrayOfObjects() throws Exception {
		Object[][] in = new Object[][]{
			new Object[]{AMap.create("f","foo")},
			new Object[]{AMap.create("f","bar")},
			new Object[]{null},
			new Object[]{AMap.create(null,"qux")},
			new Object[]{AMap.create("quux",null)},
			new Object[]{AMap.create(null,null)},
			new Object[]{"xxx"},
			new Object[]{123},
			null
		};
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	@Test
	public void d3ListOfListOfMapsWithCollections() throws Exception {
		List<List<Map<?,?>>> in = AList.create(
			AList.create(AMap.create("f",AList.create("foo"))),
			AList.create(AMap.create("f",AList.create("bar"))),
			AList.create((Map<?,?>)null),
			AList.create(AMap.create(null,AList.create("qux"))),
			AList.create(AMap.create("quux",AList.create((Object)null))),
			AList.create(AMap.create(null,AList.create((Object)null))),
			null
		);
		assertObjectEquals("[[{f:['foo']}]]", run(in, "f=foo"));
	}

	@Test
	public void d3SetOfSetOfMapsWithCollections() throws Exception {
		Set<Set<Map<?,?>>> in = ASet.create(
			ASet.create(AMap.create("f",ASet.create("foo"))),
			ASet.create(AMap.create("f",ASet.create("bar"))),
			ASet.create((Map<?,?>)null),
			ASet.create(AMap.create(null,ASet.create("qux"))),
			ASet.create(AMap.create("quux",ASet.create((Object)null))),
			ASet.create(AMap.create(null,ASet.create((Object)null))),
			null
		);
		assertObjectEquals("[[{f:['foo']}]]", run(in, "f=foo"));
	}

	@Test
	public void d3ArrayOfArrayOfMapsWithCollections() throws Exception {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{AMap.create("f",new Object[]{"foo"})},
			new Map[]{AMap.create("f",new Object[]{"bar"})},
			new Map[]{null},
			new Map[]{AMap.create(null,new Object[]{"qux"})},
			new Map[]{AMap.create("quux",new Object[]{null})},
			new Map[]{AMap.create(null,new Object[]{null})},
			null
		};
		assertObjectEquals("[[{f:['foo']}]]", run(in, "f=foo"));
	}

	@Test
	public void d3ArrayOfArrayOfBeans() throws Exception {
		A[][] in = new A[][]{
			new A[]{A.create("foo")},
			new A[]{A.create("bar")},
			new A[]{null},
			new A[]{A.create(null)},
			null
		};
		assertObjectEquals("[[{f:'foo'}]]", run(in, "f=foo"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other tests
	//-----------------------------------------------------------------------------------------------------------------

//	@Test
//	public void noSearchArgs() {
//		SearchArgs sa = new SearchArgs();
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
//		SearchArgs sa = new SearchArgs();
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
//		SearchArgs sa = new SearchArgs().append(null, "foo");
//		assertObjectEquals("'foo'", run("foo", sa));
//	}
//
//	@Test
//	public void searchArgsEmptyValue() {
//		SearchArgs sa = new SearchArgs().append("foo", null);
//		assertObjectEquals("'foo'", run("foo", sa));
//	}
}