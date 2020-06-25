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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

/**
 * Tests the PojoSearcher class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class PojoSearcherTest {

	private static BeanSession bs = BeanContext.DEFAULT.createSession();
	private static PojoSearcher ps = PojoSearcher.DEFAULT;
	private static WriterSerializer ws = JsonSerializer.create().ssq().swaps(TemporalCalendarSwap.IsoLocalDateTime.class).build();

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

	public static List<A> A_LIST = AList.of(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static Set<A> A_SET = ASet.of(A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null));
	public static A[] A_ARRAY = new A[]{A.create("foo"), A.create("bar"), A.create("baz"), A.create("q ux"), A.create("qu'ux"), null, A.create(null)};

	@Test
	public void stringSearch_singleWord() throws Exception {
		assertObject(run(A_LIST, "f=foo")).json().is("[{f:'foo'}]");
		assertObject(run(A_SET, "f=foo")).json().is("[{f:'foo'}]");
		assertObject(run(A_ARRAY, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void stringSearch_pattern1() throws Exception {
		assertObject(run(A_LIST, "f=fo*")).json().is("[{f:'foo'}]");
		assertObject(run(A_SET, "f=fo*")).json().is("[{f:'foo'}]");
		assertObject(run(A_ARRAY, "f=fo*")).json().is("[{f:'foo'}]");
	}

	@Test
	public void stringSearch_pattern2() throws Exception {
		assertObject(run(A_LIST, "f=*ar")).json().is("[{f:'bar'}]");
		assertObject(run(A_SET, "f=*ar")).json().is("[{f:'bar'}]");
		assertObject(run(A_ARRAY, "f=*ar")).json().is("[{f:'bar'}]");
	}

	@Test
	public void stringSearch_pattern3() throws Exception {
		assertObject(run(A_LIST, "f=?ar")).json().is("[{f:'bar'}]");
		assertObject(run(A_SET, "f=?ar")).json().is("[{f:'bar'}]");
		assertObject(run(A_ARRAY, "f=?ar")).json().is("[{f:'bar'}]");
	}

	@Test
	public void stringSearch_multiple() throws Exception {
		assertObject(run(A_LIST, "f=foo bar q ux")).json().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(A_SET, "f=foo bar q ux")).json().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(A_ARRAY, "f=foo bar q ux")).json().is("[{f:'foo'},{f:'bar'}]");
	}

	@Test
	public void stringSearch_quoted() throws Exception {
		assertObject(run(A_LIST, "f='q ux'")).json().is("[{f:'q ux'}]");
		assertObject(run(A_SET, "f='q ux'")).json().is("[{f:'q ux'}]");
		assertObject(run(A_ARRAY, "f='q ux'")).json().is("[{f:'q ux'}]");
	}

	@Test
	public void stringSearch_quotedWithPattern() throws Exception {
		assertObject(run(A_LIST, "f='q *x'")).json().is("[{f:'q ux'}]");
		assertObject(run(A_SET, "f='q *x'")).json().is("[{f:'q ux'}]");
		assertObject(run(A_ARRAY, "f='q *x'")).json().is("[{f:'q ux'}]");
	}

	@Test
	public void stringSearch_unquotedContainingQuote() throws Exception {
		assertObject(run(A_LIST, "f=qu'ux")).json().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_SET, "f=qu'ux")).json().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_ARRAY, "f=qu'ux")).json().is("[{f:'qu\\'ux'}]");
	}

	@Test
	public void stringSearch_quotedContainingQuote() throws Exception {
		assertObject(run(A_LIST, "f='qu\\'ux'")).json().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_SET, "f='qu\\'ux'")).json().is("[{f:'qu\\'ux'}]");
		assertObject(run(A_ARRAY, "f='qu\\'ux'")).json().is("[{f:'qu\\'ux'}]");
	}

	@Test
	public void stringSearch_regExp() throws Exception {
		assertObject(run(A_LIST, "f=/q\\sux/")).json().is("[{f:'q ux'}]");
		assertObject(run(A_SET, "f=/q\\sux/")).json().is("[{f:'q ux'}]");
		assertObject(run(A_ARRAY, "f=/q\\sux/")).json().is("[{f:'q ux'}]");
	}

	@Test
	public void stringSearch_regExp_noEndSlash() throws Exception {
		Object in = AList.of(A.create("/foo"), A.create("bar"));
		for (String s : a("f=/foo","f='/foo'"))
			assertObject(run(in, s)).json().is("[{f:'/foo'}]");
	}

	@Test
	public void stringSearch_regExp_onlySlash() throws Exception {
		Object in = AList.of(A.create("/"), A.create("bar"));
		for (String s : a("f=/", "f='/'"))
			assertObject(run(in, s)).json().is("[{f:'/'}]");
	}

	@Test
	public void stringSearch_or_pattern() throws Exception {
		Object in = AList.of(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=f* *r")).json().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(in, "f='f* *r'")).json().is("[]");
		assertObject(run(in, "f='f*oo'")).json().is("[{f:'foo'}]");
	}

	@Test
	public void stringSearch_explicit_or_pattern() throws Exception {
		Object in = AList.of(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=^f* ^*r")).json().is("[{f:'foo'},{f:'bar'}]");
		assertObject(run(in, "f=^'f* *r'")).json().is("[]");
		assertObject(run(in, "f=^'f*oo'")).json().is("[{f:'foo'}]");
	}

	@Test
	public void stringSearch_and_pattern() throws Exception {
		Object in = AList.of(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=+b* +*r")).json().is("[{f:'bar'}]");
		assertObject(run(in, "f=+'b*' +'*r'")).json().is("[{f:'bar'}]");
	}

	@Test
	public void stringSearch_not_pattern() throws Exception {
		Object in = AList.of(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=b* -*r")).json().is("[{f:'baz'}]");
		assertObject(run(in, "f=+'b*' -'*r'")).json().is("[{f:'baz'}]");
	}

	@Test
	public void stringSearch_caseSensitive() throws Exception {
		Object in = AList.of(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=F*")).json().is("[]");
		assertObject(run(in, "f=\"F*\"")).json().is("[]");
		assertObject(run(in, "f='F*'")).json().is("[{f:'foo'}]");
	}

	@Test
	public void stringSearch_malformedQuotes() throws Exception {
		Object in = AList.of(A.create("'foo"), A.create("\"bar"), A.create("baz"));

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

		assertObject(run(in, "f='\\'*'")).json().is("[{f:'\\'foo'}]");
		assertObject(run(in, "f='\"*'")).json().is("[{f:'\"bar'}]");
		assertObject(run(in, "f=\"\\\"*\"")).json().is("[{f:'\"bar'}]");
	}

	@Test
	public void stringSearch_regexChars() throws Exception {
		Object in = AList.of(A.create("+\\[]{}()^$."), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=*+*")).json().is("[{f:'+\\\\[]{}()^$.'}]");
		assertObject(run(in, "f='+\\\\[]{}()^$.'")).json().is("[{f:'+\\\\[]{}()^$.'}]");
		assertObject(run(in, "f=++\\\\[]{}()^$.")).json().is("[{f:'+\\\\[]{}()^$.'}]");
	}

	@Test
	public void stringSearch_metaChars() throws Exception {
		Object in = AList.of(A.create("*?\\'\""), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f='\\*\\?\\\\\\'\"'")).json().is("[{f:'*?\\\\\\'\"'}]");
	}

	@Test
	public void stringSearch_metaChars_escapedQuotes() throws Exception {
		Object in = AList.of(A.create("'"), A.create("\""), A.create("baz"));
		assertObject(run(in, "f=\\'")).json().is("[{f:'\\''}]");
		assertObject(run(in, "f=\\\"")).json().is("[{f:'\"'}]");
	}

	@Test
	public void stringSearch_metaChars_falseEscape() throws Exception {
		Object in = AList.of(A.create("foo"), A.create("bar"), A.create("baz"));
		assertObject(run(in, "f=\\f\\o\\o")).json().is("[{f:'foo'}]");
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
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:1}]");
	}

	@Test
	public void intSearch_twoNumbers() throws Exception {
		for (String s : a("f=1 2", "f = 1  2 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:1},{f:2}]");
	}

	@Test
	public void intSearch_oneNegativeNumber() throws Exception {
		for (String s : a("f=-1", "f = -1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-1}]");
	}

	@Test
	public void intSearch_twoNegativeNumbers() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f=-1 -2")).json().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void intSearch_simpleRange() throws Exception {
		for (String s : a("f=1-2", "f = 1 - 2 ", "f = 1- 2 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:1},{f:2}]");
	}

	@Test
	public void intSearch_simpleRange_invalid() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f=2-1")).json().is("[]");
	}

	@Test
	public void intSearch_twoNumbersThatLookLikeRange() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = 1 -2 ")).json().is("[{f:-2},{f:1}]");
	}

	@Test
	public void intSearch_rangeWithNegativeNumbers() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = -2--1 ")).json().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void intSearch_rangeWithNegativeNumbers_invalidRange() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = -1--2 ")).json().is("[]");
	}

	@Test
	public void intSearch_multipleRanges() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = 0-1 3-4")).json().is("[{f:0},{f:1},{f:3}]");
	}

	@Test
	public void intSearch_overlappingRanges() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = 0-0 2-2")).json().is("[{f:0},{f:2}]");
	}

	@Test
	public void intSearch_LT() throws Exception {
		for (String s : a("f = <0", "f<0", "f = < 0 ", "f < 0 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void intSearch_LT_negativeNumber() throws Exception {
		for (String s : a("f = <-1", "f<-1", "f = < -1 ", "f < -1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-2}]");
	}

	@Test
	public void intSearch_GT() throws Exception {
		for (String s : a("f = >1", "f>1", "f = > 1 ", "f > 1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:2},{f:3}]");
	}

	@Test
	public void intSearch_GT_negativeNumber() throws Exception {
		for (String s : a("f = >-1", "f>-1", "f = > -1 ", "f > -1 ", "f =  >  -1  ", "f >  -1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void intSearch_LTE() throws Exception {
		for (String s : a("f = <=0", "f<=0", "f = <= 0 ", "f <= 0 ", "f =  <=  0  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-2},{f:-1},{f:0}]");
	}

	@Test
	public void intSearch_LTE_negativeNumber() throws Exception {
		for (String s : a("f = <=-1", "f <=-1", "f = <= -1 ", "f =  <=  -1  ", "f <=  -1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-2},{f:-1}]");
	}

	@Test
	public void intSearch_GTE() throws Exception {
		for (String s : a("f = >=1", "f >=1", "f = >= 1 ", "f >= 1 ", "f =  >=  1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:1},{f:2},{f:3}]");
	}

	@Test
	public void intSearch_GTE_negativeNumber() throws Exception {
		for (String s : a("f = >=-1", "f >=-1", "f = >= -1 ", "f >= -1 ", "f =  >=  -1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-1},{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void intSearch_not_singleNumber() throws Exception {
		for (String s : a("f = !1", "f = ! 1 ", "f =  !  1  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-2},{f:-1},{f:0},{f:2},{f:3}]");
	}

	@Test
	public void intSearch_not_range() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = !1-2")).json().is("[{f:-2},{f:-1},{f:0},{f:3}]");
	}

	@Test
	public void intSearch_not_range_negativeNumbers() throws Exception {
		for (String s : a("f = !-2--1", "f = ! -2 - -1", "f =  !  -2  -  -1 "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void intSearch_not_looksLikeRange() throws Exception {
		assertObject(run(INT_BEAN_ARRAY, "f = ! -2 -2")).json().is("[{f:-2},{f:-1},{f:0},{f:1},{f:2},{f:3}]");
	}

	@Test
	public void intSearch_empty() throws Exception {
		for (String s : a("f=", "f = ", "f =  "))
			assertObject(run(INT_BEAN_ARRAY, s)).json().is("[{f:-2},{f:-1},{f:0},{f:1},{f:2},{f:3}]");
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
			assertObjectEquals("[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2011-01-01T00:00:00'},{f:'2011-01-31T00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_singleDate_ymd() throws Exception {
		B[] in = B.create("2010-01-01", "2011-01-01", "2011-01-31", "2012-01-01");
		assertObjectEquals("[{f:'2011-01-01T00:00:00'}]", run(in, "f=2011-01-01"), ws);
	}


	@Test
	public void dateSearch_singleDate_ymdh() throws Exception {
		B[] in = B.create("2011-01-01T11:15:59", "2011-01-01T12:00:00", "2011-01-01T12:59:59", "2011-01-01T13:00:00");
		assertObjectEquals("[{f:'2011-01-01T12:00:00'},{f:'2011-01-01T12:59:59'}]", run(in, "f=2011-01-01T12"), ws);
	}

	@Test
	public void dateSearch_singleDate_ymdhm() throws Exception {
		B[] in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00", "2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertObjectEquals("[{f:'2011-01-01T12:30:00'},{f:'2011-01-01T12:30:59'}]", run(in, "f=2011-01-01T12:30"), ws);
	}

	@Test
	public void dateSearch_singleDate_ymdhms() throws Exception {
		B[] in = B.create("2011-01-01T12:30:29", "2011-01-01T12:30:30", "2011-01-01T12:30:31");
		assertObjectEquals("[{f:'2011-01-01T12:30:30'}]", run(in, "f=2011-01-01T12:30:30"), ws);
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
			assertObjectEquals("[{f:'2001-01-01T00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2000-12-31T00:00:00'}]", run(in, s), ws);
	}

	@Test
	public void dateSearch_openEndedRanges_toMinute() throws Exception {
		B[] in = B.create("2011-01-01T12:29:59", "2011-01-01T12:30:00");
		assertObjectEquals("[{f:'2011-01-01T12:30:00'}]", run(in, "f>=2011-01-01T12:30"), ws);
		assertObjectEquals("[{f:'2011-01-01T12:29:59'}]", run(in, "f<2011-01-01T12:30"), ws);
	}

	@Test
	public void dateSearch_openEndedRanges_toSecond() throws Exception {
		B[] in = B.create("2011-01-01T12:30:59", "2011-01-01T12:31:00");
		assertObjectEquals("[{f:'2011-01-01T12:31:00'}]", run(in, "f>2011-01-01T12:30"), ws);
		assertObjectEquals("[{f:'2011-01-01T12:30:59'}]", run(in, "f<=2011-01-01T12:30"), ws);
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
			assertObjectEquals("[{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]", run(in, s), ws);

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
		assertObjectEquals("[{f:'2000-12-31T23:59:59'},{f:'2001-01-01T00:00:00'},{f:'2003-06-30T23:59:59'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2001-01-01T00:00:00'},{f:'2001-12-31T00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2003-01-01T00:00:00'},{f:'2003-12-31T00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2005-01-01T00:00:00'},{f:'2005-12-31T00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2001-01-01T00:00:00'},{f:'2003-01-01T00:00:00'}]", run(in, s), ws);
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
			assertObjectEquals("[{f:'2000-12-31T00:00:00'},{f:'2003-01-01T00:00:00'}]", run(in, s), ws);
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
		List<Map<?,?>> in = AList.of(
			AMap.of("f","foo"),
			AMap.of("f","bar"),
			null,
			AMap.of(null,"qux"),
			AMap.of("quux",null),
			AMap.of(null,null)
		);
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void d2SetOfMaps() throws Exception {
		Set<Map<?,?>> in = ASet.of(
			AMap.of("f","foo"),
			AMap.of("f","bar"),
			null,
			AMap.of(null,"qux"),
			AMap.of("quux",null),
			AMap.of(null,null)
		);
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}


	@Test
	public void d2ArrayOfMaps() throws Exception {
		Map<?,?>[] in = new Map[]{
			AMap.of("f","foo"),
			AMap.of("f","bar"),
			null,
			AMap.of(null,"qux"),
			AMap.of("quux",null),
			AMap.of(null,null)
		};
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void d2ListOfObjects() throws Exception {
		List<Object> in = AList.of(
			AMap.of("f","foo"),
			AMap.of("f","bar"),
			null,
			AMap.of(null,"qux"),
			AMap.of("quux",null),
			AMap.of(null,null),
			"xxx",
			123
		);
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void d2SetOfObjects() throws Exception {
		Set<Object> in = ASet.of(
			AMap.of("f","foo"),
			AMap.of("f","bar"),
			null,
			AMap.of(null,"qux"),
			AMap.of("quux",null),
			AMap.of(null,null),
			"xxx",
			123
		);
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void d2ArrayOfObjects() throws Exception {
		Object[] in = new Object[]{
			AMap.of("f","foo"),
			AMap.of("f","bar"),
			null,
			AMap.of(null,"qux"),
			AMap.of("quux",null),
			AMap.of(null,null),
			"xxx",
			123
		};
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void d2ListOfMapsWithLists() throws Exception {
		List<Map<?,?>> in = AList.of(
			AMap.of("f",AList.of("foo")),
			AMap.of("f",AList.of("bar")),
			null,
			AMap.of(null,AList.of("qux")),
			AMap.of("quux",AList.of((Object)null)),
			AMap.of(null,AList.of((Object)null))
		);
		assertObject(run(in, "f=foo")).json().is("[{f:['foo']}]");
	}

	@Test
	public void d2SetOfMapsWithSets() throws Exception {
		Set<Map<?,?>> in = ASet.of(
			AMap.of("f",ASet.of("foo")),
			AMap.of("f",ASet.of("bar")),
			null,
			AMap.of(null,ASet.of("qux")),
			AMap.of("quux",ASet.of((Object)null)),
			AMap.of(null,ASet.of((Object)null))
		);
		assertObject(run(in, "f=foo")).json().is("[{f:['foo']}]");
	}

	@Test
	public void d2ArrayOfMapsWithArrays() throws Exception {
		Map<?,?>[] in = new Map[]{
			AMap.of("f",new Object[]{"foo"}),
			AMap.of("f",new Object[]{"bar"}),
			null,
			AMap.of(null,new Object[]{"qux"}),
			AMap.of("quux",new Object[]{null}),
			AMap.of(null,new Object[]{null})
		};
		assertObject(run(in, "f=foo")).json().is("[{f:['foo']}]");
	}

	@Test
	public void d2ListOfBeans() throws Exception {
		List<A> in = AList.of(
			A.create("foo"),
			A.create("bar"),
			null,
			A.create(null)
		);
		assertObject(run(in, "f=foo")).json().is("[{f:'foo'}]");
	}

	@Test
	public void d3ListOfListOfMaps() throws Exception {
		List<List<Map<?,?>>> in = AList.of(
			AList.of(AMap.of("f","foo")),
			AList.of(AMap.of("f","bar")),
			AList.of((Map<?,?>)null),
			AList.of(AMap.of(null,"qux")),
			AList.of(AMap.of("quux",null)),
			AList.of(AMap.of(null,null)),
			null
		);
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
	}

	@Test
	public void d3SetOfSetOfMaps() throws Exception {
		Set<Set<Map<?,?>>> in = ASet.of(
			ASet.of(AMap.of("f","foo")),
			ASet.of(AMap.of("f","bar")),
			ASet.of(AMap.of("f","baz")),
			ASet.of((Map<?,?>)null),
			ASet.of(AMap.of(null,"qux")),
			ASet.of(AMap.of("quux",null)),
			ASet.of(AMap.of(null,null)),
			null
		);
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
	}

	@Test
	public void d3ArrayOfArrayOfMaps() throws Exception {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{AMap.of("f","foo")},
			new Map[]{AMap.of("f","bar")},
			new Map[]{AMap.of("f","baz")},
			new Map[]{null},
			new Map[]{AMap.of(null,"qux")},
			new Map[]{AMap.of("quux",null)},
			new Map[]{AMap.of(null,null)},
			null
		};
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
	}

	@Test
	public void d3ListOfListOfObjects() throws Exception {
		List<List<Object>> in = AList.of(
			AList.of(AMap.of("f","foo")),
			AList.of(AMap.of("f","bar")),
			AList.of((Object)null),
			AList.of(AMap.of(null,"qux")),
			AList.of(AMap.of("quux",null)),
			AList.of(AMap.of(null,null)),
			AList.of("xxx"),
			AList.of(123),
			null
		);
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
	}

	@Test
	public void d3SetOfSetOfObjects() throws Exception {
		Set<Set<Object>> in = ASet.of(
			ASet.of(AMap.of("f","foo")),
			ASet.of(AMap.of("f","bar")),
			ASet.of((Map<?,?>)null),
			ASet.of(AMap.of(null,"qux")),
			ASet.of(AMap.of("quux",null)),
			ASet.of(AMap.of(null,null)),
			ASet.of("xxx"),
			ASet.of(123),
			null
		);
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
	}

	@Test
	public void d3ArrayOfArrayOfObjects() throws Exception {
		Object[][] in = new Object[][]{
			new Object[]{AMap.of("f","foo")},
			new Object[]{AMap.of("f","bar")},
			new Object[]{null},
			new Object[]{AMap.of(null,"qux")},
			new Object[]{AMap.of("quux",null)},
			new Object[]{AMap.of(null,null)},
			new Object[]{"xxx"},
			new Object[]{123},
			null
		};
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
	}

	@Test
	public void d3ListOfListOfMapsWithCollections() throws Exception {
		List<List<Map<?,?>>> in = AList.of(
			AList.of(AMap.of("f",AList.of("foo"))),
			AList.of(AMap.of("f",AList.of("bar"))),
			AList.of((Map<?,?>)null),
			AList.of(AMap.of(null,AList.of("qux"))),
			AList.of(AMap.of("quux",AList.of((Object)null))),
			AList.of(AMap.of(null,AList.of((Object)null))),
			null
		);
		assertObject(run(in, "f=foo")).json().is("[[{f:['foo']}]]");
	}

	@Test
	public void d3SetOfSetOfMapsWithCollections() throws Exception {
		Set<Set<Map<?,?>>> in = ASet.of(
			ASet.of(AMap.of("f",ASet.of("foo"))),
			ASet.of(AMap.of("f",ASet.of("bar"))),
			ASet.of((Map<?,?>)null),
			ASet.of(AMap.of(null,ASet.of("qux"))),
			ASet.of(AMap.of("quux",ASet.of((Object)null))),
			ASet.of(AMap.of(null,ASet.of((Object)null))),
			null
		);
		assertObject(run(in, "f=foo")).json().is("[[{f:['foo']}]]");
	}

	@Test
	public void d3ArrayOfArrayOfMapsWithCollections() throws Exception {
		Map<?,?>[][] in = new Map[][]{
			new Map[]{AMap.of("f",new Object[]{"foo"})},
			new Map[]{AMap.of("f",new Object[]{"bar"})},
			new Map[]{null},
			new Map[]{AMap.of(null,new Object[]{"qux"})},
			new Map[]{AMap.of("quux",new Object[]{null})},
			new Map[]{AMap.of(null,new Object[]{null})},
			null
		};
		assertObject(run(in, "f=foo")).json().is("[[{f:['foo']}]]");
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
		assertObject(run(in, "f=foo")).json().is("[[{f:'foo'}]]");
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