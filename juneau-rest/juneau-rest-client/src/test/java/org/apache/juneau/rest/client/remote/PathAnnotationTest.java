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
package org.apache.juneau.rest.client.remote;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Path annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PathAnnotationTest {

	public static class Bean {
		public int x;

		public static Bean create() {
			Bean b = new Bean();
			b.x = 1;
			return b;
		}
	}

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod(path="/a/{x}")
		public String getA(@Path("x") Object x) {
			return x.toString();
		}
	}
	private static MockRest a = MockRest.create(A.class);

	@RemoteResource
	public static interface A01 {
		@RemoteMethod(path="a/{x}") String getA01(@Path("x") int b);
		@RemoteMethod(path="a/{x}") String getA02(@Path("x") float b);
		@RemoteMethod(path="a/{x}") String getA03a(@Path("x") Bean b);
		@RemoteMethod(path="a/{x}") String getA03b(@Path("*") Bean b);
		@RemoteMethod(path="a/{x}") String getA03c(@Path Bean b);
		@RemoteMethod(path="a/{x}") String getA04a(@Path("x") Bean[] b);
		@RemoteMethod(path="a/{x}") String getA04b(@Path(name="x",collectionFormat="uon") Bean[] b);
		@RemoteMethod(path="a/{x}") String getA05a(@Path("x") List<Bean> b);
		@RemoteMethod(path="a/{x}") String getA05b(@Path(name="x",collectionFormat="uon") List<Bean> b);
		@RemoteMethod(path="a/{x}") String getA06a(@Path("x") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06b(@Path("*") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06c(@Path Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06d(@Path(name="x",format="uon") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06e(@Path(format="uon") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA09a(@Path("*") NameValuePairs b);
		@RemoteMethod(path="a/{x}") String getA09b(@Path NameValuePairs b);
	}

	private static A01 a01 = RestClient.create().mockHttpConnection(a).build().getRemoteResource(A01.class);

	@Test
	public void a01_int() throws Exception {
		assertEquals("1", a01.getA01(1));
	}
	@Test
	public void a02_float() throws Exception {
		assertEquals("1.0", a01.getA02(1));
	}
	@Test
	public void a03a_Bean() throws Exception {
		assertEquals("(x=1)", a01.getA03a(Bean.create()));
	}
	@Test
	public void a03b_Bean() throws Exception {
		assertEquals("1", a01.getA03b(Bean.create()));
	}
	@Test
	public void a03c_Bean() throws Exception {
		assertEquals("1", a01.getA03c(Bean.create()));
	}
	@Test
	public void a04a_BeanArray() throws Exception {
		assertEquals("(x=1),(x=1)", a01.getA04a(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a04b_BeanArray() throws Exception {
		assertEquals("@((x=1),(x=1))", a01.getA04b(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a05a_ListOfBeans() throws Exception {
		assertEquals("(x=1),(x=1)", a01.getA05a(AList.create(Bean.create(),Bean.create())));
	}
	@Test
	public void a05b_ListOfBeans() throws Exception {
		assertEquals("@((x=1),(x=1))", a01.getA05b(AList.create(Bean.create(),Bean.create())));
	}
	@Test
	public void a06a_MapOfBeans() throws Exception {
		assertEquals("(x=(x=1))", a01.getA06a(AMap.create("x",Bean.create())));
	}
	@Test
	public void a06b_MapOfBeans() throws Exception {
		assertEquals("(x=1)", a01.getA06b(AMap.create("x",Bean.create())));
	}
	@Test
	public void a06c_MapOfBeans() throws Exception {
		assertEquals("(x=1)", a01.getA06c(AMap.create("x",Bean.create())));
	}
	@Test
	public void a06d_MapOfBeans() throws Exception {
		assertEquals("(x=(x=1))", a01.getA06d(AMap.create("x",Bean.create())));
	}
	@Test
	public void a06e_MapOfBeans() throws Exception {
		assertEquals("(x=1)", a01.getA06e(AMap.create("x",Bean.create())));
	}
	@Test
	public void a09a_NameValuePairs() throws Exception {
		assertEquals("bar", a01.getA09a(new NameValuePairs().append("x", "bar")));
	}
	@Test
	public void a09b_NameValuePairs() throws Exception {
		assertEquals("bar", a01.getA09b(new NameValuePairs().append("x", "bar")));
	}


	//=================================================================================================================
	// @Query(collectionFormat)
	//=================================================================================================================

	@RestResource
	public static class C {
		@RestMethod(path="/a/{x}")
		public String getA(@Path("x") Object x) {
			return x.toString();
		}
	}
	private static MockRest c = MockRest.create(C.class);

	@RemoteResource
	public static interface CR {
		@RemoteMethod(path="/a/{x}") String getC01(@Path(name="x") String...b);
		@RemoteMethod(path="/a/{x}") String getC02(@Path(name="x",collectionFormat="csv") String...b);
		@RemoteMethod(path="/a/{x}") String getC03(@Path(name="x",collectionFormat="ssv") String...b);
		@RemoteMethod(path="/a/{x}") String getC04(@Path(name="x",collectionFormat="tsv") String...b);
		@RemoteMethod(path="/a/{x}") String getC05(@Path(name="x",collectionFormat="pipes") String...b);
		@RemoteMethod(path="/a/{x}") String getC06(@Path(name="x",collectionFormat="multi") String...b);
		@RemoteMethod(path="/a/{x}") String getC07(@Path(name="x",collectionFormat="uon") String...b);
	}

	private static CR cr = RestClient.create().mockHttpConnection(c).build().getRemoteResource(CR.class);

	@Test
	public void c01a_default() throws Exception {
		assertEquals("foo,bar", cr.getC01("foo","bar"));
	}
	@Test
	public void c02a_csv() throws Exception {
		assertEquals("foo,bar", cr.getC02("foo","bar"));
	}
	@Test
	public void c03a_ssv() throws Exception {
		assertEquals("foo bar", cr.getC03("foo","bar"));
	}
	@Test
	public void c04a_tsv() throws Exception {
		assertEquals("foo\tbar", cr.getC04("foo","bar"));
	}
	@Test
	public void c05a_pipes() throws Exception {
		assertEquals("foo|bar", cr.getC05("foo","bar"));
	}
	@Test
	public void c06a_multi() throws Exception {
		// Not supported, but should be treated as csv.
		assertEquals("foo,bar", cr.getC06("foo","bar"));
	}
	@Test
	public void c07a_uon() throws Exception {
		assertEquals("@(foo,bar)", cr.getC07("foo","bar"));
	}

	//=================================================================================================================
	// @Path(maximum,exclusiveMaximum,minimum,exclusiveMinimum)
	//=================================================================================================================

	@RestResource
	public static class D {
		@RestMethod(path="/a/{x}")
		public String get(@Path("*") ObjectMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}
	private static MockRest d = MockRest.create(D.class);

	@RemoteResource
	public static interface DR {
		@RemoteMethod(path="/a/{x}") String getC01a(@Path(name="x",minimum="1",maximum="10") int b);
		@RemoteMethod(path="/a/{x}") String getC01b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) int b);
		@RemoteMethod(path="/a/{x}") String getC01c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) int b);
		@RemoteMethod(path="/a/{x}") String getC02a(@Path(name="x",minimum="1",maximum="10") short b);
		@RemoteMethod(path="/a/{x}") String getC02b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) short b);
		@RemoteMethod(path="/a/{x}") String getC02c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) short b);
		@RemoteMethod(path="/a/{x}") String getC03a(@Path(name="x",minimum="1",maximum="10") long b);
		@RemoteMethod(path="/a/{x}") String getC03b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) long b);
		@RemoteMethod(path="/a/{x}") String getC03c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) long b);
		@RemoteMethod(path="/a/{x}") String getC04a(@Path(name="x",minimum="1",maximum="10") float b);
		@RemoteMethod(path="/a/{x}") String getC04b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) float b);
		@RemoteMethod(path="/a/{x}") String getC04c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) float b);
		@RemoteMethod(path="/a/{x}") String getC05a(@Path(name="x",minimum="1",maximum="10") double b);
		@RemoteMethod(path="/a/{x}") String getC05b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) double b);
		@RemoteMethod(path="/a/{x}") String getC05c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) double b);
		@RemoteMethod(path="/a/{x}") String getC06a(@Path(name="x",minimum="1",maximum="10") byte b);
		@RemoteMethod(path="/a/{x}") String getC06b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) byte b);
		@RemoteMethod(path="/a/{x}") String getC06c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) byte b);
		@RemoteMethod(path="/a/{x}") String getC07a(@Path(name="x",minimum="1",maximum="10") AtomicInteger b);
		@RemoteMethod(path="/a/{x}") String getC07b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) AtomicInteger b);
		@RemoteMethod(path="/a/{x}") String getC07c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) AtomicInteger b);
		@RemoteMethod(path="/a/{x}") String getC08a(@Path(name="x",minimum="1",maximum="10") BigDecimal b);
		@RemoteMethod(path="/a/{x}") String getC08b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) BigDecimal b);
		@RemoteMethod(path="/a/{x}") String getC08c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) BigDecimal b);
		@RemoteMethod(path="/a/{x}") String getC11a(@Path(name="x",minimum="1",maximum="10") Integer b);
		@RemoteMethod(path="/a/{x}") String getC11b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Integer b);
		@RemoteMethod(path="/a/{x}") String getC11c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Integer b);
		@RemoteMethod(path="/a/{x}") String getC12a(@Path(name="x",minimum="1",maximum="10") Short b);
		@RemoteMethod(path="/a/{x}") String getC12b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Short b);
		@RemoteMethod(path="/a/{x}") String getC12c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Short b);
		@RemoteMethod(path="/a/{x}") String getC13a(@Path(name="x",minimum="1",maximum="10") Long b);
		@RemoteMethod(path="/a/{x}") String getC13b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Long b);
		@RemoteMethod(path="/a/{x}") String getC13c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Long b);
		@RemoteMethod(path="/a/{x}") String getC14a(@Path(name="x",minimum="1",maximum="10") Float b);
		@RemoteMethod(path="/a/{x}") String getC14b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Float b);
		@RemoteMethod(path="/a/{x}") String getC14c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Float b);
		@RemoteMethod(path="/a/{x}") String getC15a(@Path(name="x",minimum="1",maximum="10") Double b);
		@RemoteMethod(path="/a/{x}") String getC15b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Double b);
		@RemoteMethod(path="/a/{x}") String getC15c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Double b);
		@RemoteMethod(path="/a/{x}") String getC16a(@Path(name="x",minimum="1",maximum="10") Byte b);
		@RemoteMethod(path="/a/{x}") String getC16b(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Byte b);
		@RemoteMethod(path="/a/{x}") String getC16c(@Path(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Byte b);
	}

	private static DR dr = RestClient.create().mockHttpConnection(d).build().getRemoteResource(DR.class);

	@Test
	public void d01a_int_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC01a(1));
		assertEquals("{x:'10'}", dr.getC01a(10));
		try { dr.getC01a(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC01a(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01b_int_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC01b(1));
		assertEquals("{x:'10'}", dr.getC01b(10));
		try { dr.getC01b(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC01b(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01c_int_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC01c(2));
		assertEquals("{x:'9'}", dr.getC01c(9));
		try { dr.getC01c(1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC01c(10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02a_short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC02a((short)1));
		assertEquals("{x:'10'}", dr.getC02a((short)10));
		try { dr.getC02a((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC02a((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02b_short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC02b((short)1));
		assertEquals("{x:'10'}", dr.getC02b((short)10));
		try { dr.getC02b((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC02b((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02c_short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC02c((short)2));
		assertEquals("{x:'9'}", dr.getC02c((short)9));
		try { dr.getC02c((short)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC02c((short)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03a_long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC03a(1l));
		assertEquals("{x:'10'}", dr.getC03a(10l));
		try { dr.getC03a(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC03a(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03b_long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC03b(1l));
		assertEquals("{x:'10'}", dr.getC03b(10l));
		try { dr.getC03b(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC03b(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03c_long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC03c(2l));
		assertEquals("{x:'9'}", dr.getC03c(9l));
		try { dr.getC03c(1l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC03c(10l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04a_float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC04a(1f));
		assertEquals("{x:'10.0'}", dr.getC04a(10f));
		try { dr.getC04a(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC04a(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04b_float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC04b(1f));
		assertEquals("{x:'10.0'}", dr.getC04b(10f));
		try { dr.getC04b(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC04b(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04c_float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC04c(1.1f));
		assertEquals("{x:'9.9'}", dr.getC04c(9.9f));
		try { dr.getC04c(1f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC04c(10f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05a_double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC05a(1d));
		assertEquals("{x:'10.0'}", dr.getC05a(10d));
		try { dr.getC05a(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC05a(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05b_double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC05b(1d));
		assertEquals("{x:'10.0'}", dr.getC05b(10d));
		try { dr.getC05b(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC05b(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05c_double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC05c(1.1d));
		assertEquals("{x:'9.9'}", dr.getC05c(9.9d));
		try { dr.getC05c(1d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC05c(10d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06a_byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC06a((byte)1));
		assertEquals("{x:'10'}", dr.getC06a((byte)10));
		try { dr.getC06a((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC06a((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06b_byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC06b((byte)1));
		assertEquals("{x:'10'}", dr.getC06b((byte)10));
		try { dr.getC06b((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC06b((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06c_byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC06c((byte)2));
		assertEquals("{x:'9'}", dr.getC06c((byte)9));
		try { dr.getC06c((byte)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC06c((byte)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07a_AtomicInteger_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC07a(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.getC07a(new AtomicInteger(10)));
		try { dr.getC07a(new AtomicInteger(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC07a(new AtomicInteger(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07b_AtomicInteger_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC07b(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.getC07b(new AtomicInteger(10)));
		try { dr.getC07b(new AtomicInteger(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC07b(new AtomicInteger(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07c_AtomicInteger_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC07c(new AtomicInteger(2)));
		assertEquals("{x:'9'}", dr.getC07c(new AtomicInteger(9)));
		try { dr.getC07c(new AtomicInteger(1)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC07c(new AtomicInteger(10)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08a_BigDecimal_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC08a(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.getC08a(new BigDecimal(10)));
		try { dr.getC08a(new BigDecimal(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC08a(new BigDecimal(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08b_BigDecimal_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC08b(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.getC08b(new BigDecimal(10)));
		try { dr.getC08b(new BigDecimal(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC08b(new BigDecimal(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08cBigDecimal_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC08c(new BigDecimal(2)));
		assertEquals("{x:'9'}", dr.getC08c(new BigDecimal(9)));
		try { dr.getC08c(new BigDecimal(1)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC08c(new BigDecimal(10)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d11a_Integer_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC11a(1));
		assertEquals("{x:'10'}", dr.getC11a(10));
		try { dr.getC11a(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC11a(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC11a(null));
	}
	@Test
	public void d11b_Integer_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC11b(1));
		assertEquals("{x:'10'}", dr.getC11b(10));
		try { dr.getC11b(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC11b(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC11b(null));
	}
	@Test
	public void d11c_Integer_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC11c(2));
		assertEquals("{x:'9'}", dr.getC11c(9));
		try { dr.getC11c(1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC11c(10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC11c(null));
	}
	@Test
	public void d12a_Short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC12a((short)1));
		assertEquals("{x:'10'}", dr.getC12a((short)10));
		try { dr.getC12a((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC12a((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC12a(null));
	}
	@Test
	public void d12b_Short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC12b((short)1));
		assertEquals("{x:'10'}", dr.getC12b((short)10));
		try { dr.getC12b((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC12b((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC12b(null));
	}
	@Test
	public void d12c_Short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC12c((short)2));
		assertEquals("{x:'9'}", dr.getC12c((short)9));
		try { dr.getC12c((short)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC12c((short)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC12c(null));
	}
	@Test
	public void d13a_Long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC13a(1l));
		assertEquals("{x:'10'}", dr.getC13a(10l));
		try { dr.getC13a(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC13a(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC13a(null));
	}
	@Test
	public void d13b_Long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC13b(1l));
		assertEquals("{x:'10'}", dr.getC13b(10l));
		try { dr.getC13b(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC13b(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC13b(null));
	}
	@Test
	public void d13c_Long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC13c(2l));
		assertEquals("{x:'9'}", dr.getC13c(9l));
		try { dr.getC13c(1l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC13c(10l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC13c(null));
	}
	@Test
	public void d14a_Float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC14a(1f));
		assertEquals("{x:'10.0'}", dr.getC14a(10f));
		try { dr.getC14a(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC14a(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC14a(null));
	}
	@Test
	public void d14b_Float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC14b(1f));
		assertEquals("{x:'10.0'}", dr.getC14b(10f));
		try { dr.getC14b(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC14b(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC14b(null));
	}
	@Test
	public void d14c_Float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC14c(1.1f));
		assertEquals("{x:'9.9'}", dr.getC14c(9.9f));
		try { dr.getC14c(1f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC14c(10f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC14c(null));
	}
	@Test
	public void d15a_Double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC15a(1d));
		assertEquals("{x:'10.0'}", dr.getC15a(10d));
		try { dr.getC15a(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC15a(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC15a(null));
	}
	@Test
	public void d15b_Double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC15b(1d));
		assertEquals("{x:'10.0'}", dr.getC15b(10d));
		try { dr.getC15b(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC15b(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC15b(null));
	}
	@Test
	public void d15c_Double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC15c(1.1d));
		assertEquals("{x:'9.9'}", dr.getC15c(9.9d));
		try { dr.getC15c(1d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC15c(10d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC15c(null));
	}
	@Test
	public void d16a_Byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC16a((byte)1));
		assertEquals("{x:'10'}", dr.getC16a((byte)10));
		try { dr.getC16a((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC16a((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC16a(null));
	}
	@Test
	public void d16b_Byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC16b((byte)1));
		assertEquals("{x:'10'}", dr.getC16b((byte)10));
		try { dr.getC16b((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC16b((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC16b(null));
	}
	@Test
	public void d16c_Byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC16c((byte)2));
		assertEquals("{x:'9'}", dr.getC16c((byte)9));
		try { dr.getC16c((byte)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC16c((byte)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{x:'null'}", dr.getC16c(null));
	}

	//=================================================================================================================
	// @Path(maxItems,minItems,uniqueItems)
	//=================================================================================================================

	@RestResource
	public static class E {
		@RestMethod(path="/{x}")
		public String get(@Path("*") ObjectMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}
	private static MockRest e = MockRest.create(E.class);

	@RemoteResource
	public static interface ER {
		@RemoteMethod(path="/{x}") String getE01(@Path(name="x",collectionFormat="pipes",minItems=1,maxItems=2) String...b);
		@RemoteMethod(path="/{x}") String getE02(@Path(name="x",items=@Items(collectionFormat="pipes",minItems=1,maxItems=2)) String[]...b);
		@RemoteMethod(path="/{x}") String getE03(@Path(name="x",collectionFormat="pipes",uniqueItems=false) String...b);
		@RemoteMethod(path="/{x}") String getE04(@Path(name="x",items=@Items(collectionFormat="pipes",uniqueItems=false)) String[]...b);
		@RemoteMethod(path="/{x}") String getE05(@Path(name="x",collectionFormat="pipes",uniqueItems=true) String...b);
		@RemoteMethod(path="/{x}") String getE06(@Path(name="x",items=@Items(collectionFormat="pipes",uniqueItems=true)) String[]...b);
	}

	private static ER er = RestClient.create().mockHttpConnection(e).build().getRemoteResource(ER.class);

	@Test
	public void e01_minMax() throws Exception {
		assertEquals("{x:'1'}", er.getE01("1"));
		assertEquals("{x:'1|2'}", er.getE01("1","2"));
		try { er.getE01(); } catch (Exception e) { assertContains(e, "Minimum number of items not met"); }
		try { er.getE01("1","2","3"); } catch (Exception e) { assertContains(e, "Maximum number of items exceeded"); }
		assertEquals("{x:'null'}", er.getE01((String)null));
	}
	@Test
	public void e02_minMax_items() throws Exception {
		assertEquals("{x:'1'}", er.getE02(new String[]{"1"}));
		assertEquals("{x:'1|2'}", er.getE02(new String[]{"1","2"}));
		try { er.getE02(new String[]{}); } catch (Exception e) { assertContains(e, "Minimum number of items not met"); }
		try { er.getE02(new String[]{"1","2","3"}); } catch (Exception e) { assertContains(e, "Maximum number of items exceeded"); }
		assertEquals("{x:'null'}", er.getE02(new String[]{null}));
	}
	@Test
	public void e03_uniqueItems_false() throws Exception {
		assertEquals("{x:'1|1'}", er.getE03("1","1"));
	}
	@Test
	public void e04_uniqueItems_items_false() throws Exception {
		assertEquals("{x:'1|1'}", er.getE04(new String[]{"1","1"}));
	}
	@Test
	public void e05_uniqueItems_true() throws Exception {
		assertEquals("{x:'1|2'}", er.getE05("1","2"));
		try { assertEquals("{x:'1|1'}", er.getE05("1","1")); } catch (Exception e) { assertContains(e, "Duplicate items not allowed"); }
	}
	@Test
	public void e06_uniqueItems_items_true() throws Exception {
		assertEquals("{x:'1|2'}", er.getE06(new String[]{"1","2"}));
		try { assertEquals("{x:'1|1'}", er.getE06(new String[]{"1","1"})); } catch (Exception e) { assertContains(e, "Duplicate items not allowed"); }
	}
}
