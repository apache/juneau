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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
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

	@Rest
	public static class A {
		@RestMethod(path="/a/{x}")
		public String getA(@Path("x") Object x) {
			return x.toString();
		}
	}

	@Remote
	public static interface A01 {
		@RemoteMethod(path="a/{x}") String getA01(@Path("x") int b);
		@RemoteMethod(path="a/{x}") String getA02(@Path("x") float b);
		@RemoteMethod(path="a/{x}") String getA03a(@Path("x") Bean b);
		@RemoteMethod(path="a/{x}") String getA03b(@Path("*") Bean b);
		@RemoteMethod(path="a/{x}") String getA03c(@Path Bean b);
		@RemoteMethod(path="a/{x}") String getA04a(@Path("x") Bean[] b);
		@RemoteMethod(path="a/{x}") String getA04b(@Path(n="x",cf="uon") Bean[] b);
		@RemoteMethod(path="a/{x}") String getA05a(@Path("x") List<Bean> b);
		@RemoteMethod(path="a/{x}") String getA05b(@Path(n="x",cf="uon") List<Bean> b);
		@RemoteMethod(path="a/{x}") String getA06a(@Path("x") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06b(@Path("*") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06c(@Path Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06d(@Path(n="x",cf="uon") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA06e(@Path(f="uon") Map<String,Bean> b);
		@RemoteMethod(path="a/{x}") String getA09a(@Path("*") NameValuePairs b);
		@RemoteMethod(path="a/{x}") String getA09b(@Path NameValuePairs b);
	}

	private static A01 a01 = MockRestClient.build(A.class).getRemote(A01.class);

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
		assertEquals("x=1", a01.getA03a(Bean.create()));
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
		assertEquals("x=1,x=1", a01.getA04a(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a04b_BeanArray() throws Exception {
		assertEquals("@((x=1),(x=1))", a01.getA04b(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a05a_ListOfBeans() throws Exception {
		assertEquals("x=1,x=1", a01.getA05a(AList.of(Bean.create(),Bean.create())));
	}
	@Test
	public void a05b_ListOfBeans() throws Exception {
		assertEquals("@((x=1),(x=1))", a01.getA05b(AList.of(Bean.create(),Bean.create())));
	}
	@Test
	public void a06a_MapOfBeans() throws Exception {
		assertEquals("x=x\\=1", a01.getA06a(AMap.of("x",Bean.create())));
	}
	@Test
	public void a06b_MapOfBeans() throws Exception {
		assertEquals("x=1", a01.getA06b(AMap.of("x",Bean.create())));
	}
	@Test
	public void a06c_MapOfBeans() throws Exception {
		assertEquals("x=1", a01.getA06c(AMap.of("x",Bean.create())));
	}
	@Test
	public void a06d_MapOfBeans() throws Exception {
		assertEquals("(x=(x=1))", a01.getA06d(AMap.of("x",Bean.create())));
	}
	@Test
	public void a06e_MapOfBeans() throws Exception {
		assertEquals("x=1", a01.getA06e(AMap.of("x",Bean.create())));
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

	@Rest
	public static class C {
		@RestMethod(path="/a/{x}")
		public String getA(@Path("x") Object x) {
			return x.toString();
		}
	}

	@Remote
	public static interface CR {
		@RemoteMethod(path="/a/{x}") String getC01(@Path(n="x") String...b);
		@RemoteMethod(path="/a/{x}") String getC02(@Path(n="x",cf="csv") String...b);
		@RemoteMethod(path="/a/{x}") String getC03(@Path(n="x",cf="ssv") String...b);
		@RemoteMethod(path="/a/{x}") String getC04(@Path(n="x",cf="tsv") String...b);
		@RemoteMethod(path="/a/{x}") String getC05(@Path(n="x",cf="pipes") String...b);
		@RemoteMethod(path="/a/{x}") String getC06(@Path(n="x",cf="multi") String...b);
		@RemoteMethod(path="/a/{x}") String getC07(@Path(n="x",cf="uon") String...b);
	}

	private static CR cr = MockRestClient.build(C.class).getRemote(CR.class);

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

	@Rest
	public static class D {
		@RestMethod(path="/a/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface DR {
		@RemoteMethod(path="/a/{x}") String getC01a(@Path(n="x",min="1",max="10") int b);
		@RemoteMethod(path="/a/{x}") String getC01b(@Path(n="x",min="1",max="10",emin=false,emax=false) int b);
		@RemoteMethod(path="/a/{x}") String getC01c(@Path(n="x",min="1",max="10",emin=true,emax=true) int b);
		@RemoteMethod(path="/a/{x}") String getC02a(@Path(n="x",min="1",max="10") short b);
		@RemoteMethod(path="/a/{x}") String getC02b(@Path(n="x",min="1",max="10",emin=false,emax=false) short b);
		@RemoteMethod(path="/a/{x}") String getC02c(@Path(n="x",min="1",max="10",emin=true,emax=true) short b);
		@RemoteMethod(path="/a/{x}") String getC03a(@Path(n="x",min="1",max="10") long b);
		@RemoteMethod(path="/a/{x}") String getC03b(@Path(n="x",min="1",max="10",emin=false,emax=false) long b);
		@RemoteMethod(path="/a/{x}") String getC03c(@Path(n="x",min="1",max="10",emin=true,emax=true) long b);
		@RemoteMethod(path="/a/{x}") String getC04a(@Path(n="x",min="1",max="10") float b);
		@RemoteMethod(path="/a/{x}") String getC04b(@Path(n="x",min="1",max="10",emin=false,emax=false) float b);
		@RemoteMethod(path="/a/{x}") String getC04c(@Path(n="x",min="1",max="10",emin=true,emax=true) float b);
		@RemoteMethod(path="/a/{x}") String getC05a(@Path(n="x",min="1",max="10") double b);
		@RemoteMethod(path="/a/{x}") String getC05b(@Path(n="x",min="1",max="10",emin=false,emax=false) double b);
		@RemoteMethod(path="/a/{x}") String getC05c(@Path(n="x",min="1",max="10",emin=true,emax=true) double b);
		@RemoteMethod(path="/a/{x}") String getC06a(@Path(n="x",min="1",max="10") byte b);
		@RemoteMethod(path="/a/{x}") String getC06b(@Path(n="x",min="1",max="10",emin=false,emax=false) byte b);
		@RemoteMethod(path="/a/{x}") String getC06c(@Path(n="x",min="1",max="10",emin=true,emax=true) byte b);
		@RemoteMethod(path="/a/{x}") String getC07a(@Path(n="x",min="1",max="10") AtomicInteger b);
		@RemoteMethod(path="/a/{x}") String getC07b(@Path(n="x",min="1",max="10",emin=false,emax=false) AtomicInteger b);
		@RemoteMethod(path="/a/{x}") String getC07c(@Path(n="x",min="1",max="10",emin=true,emax=true) AtomicInteger b);
		@RemoteMethod(path="/a/{x}") String getC08a(@Path(n="x",min="1",max="10") BigDecimal b);
		@RemoteMethod(path="/a/{x}") String getC08b(@Path(n="x",min="1",max="10",emin=false,emax=false) BigDecimal b);
		@RemoteMethod(path="/a/{x}") String getC08c(@Path(n="x",min="1",max="10",emin=true,emax=true) BigDecimal b);
		@RemoteMethod(path="/a/{x}") String getC11a(@Path(n="x",min="1",max="10") Integer b);
		@RemoteMethod(path="/a/{x}") String getC11b(@Path(n="x",min="1",max="10",emin=false,emax=false) Integer b);
		@RemoteMethod(path="/a/{x}") String getC11c(@Path(n="x",min="1",max="10",emin=true,emax=true) Integer b);
		@RemoteMethod(path="/a/{x}") String getC12a(@Path(n="x",min="1",max="10") Short b);
		@RemoteMethod(path="/a/{x}") String getC12b(@Path(n="x",min="1",max="10",emin=false,emax=false) Short b);
		@RemoteMethod(path="/a/{x}") String getC12c(@Path(n="x",min="1",max="10",emin=true,emax=true) Short b);
		@RemoteMethod(path="/a/{x}") String getC13a(@Path(n="x",min="1",max="10") Long b);
		@RemoteMethod(path="/a/{x}") String getC13b(@Path(n="x",min="1",max="10",emin=false,emax=false) Long b);
		@RemoteMethod(path="/a/{x}") String getC13c(@Path(n="x",min="1",max="10",emin=true,emax=true) Long b);
		@RemoteMethod(path="/a/{x}") String getC14a(@Path(n="x",min="1",max="10") Float b);
		@RemoteMethod(path="/a/{x}") String getC14b(@Path(n="x",min="1",max="10",emin=false,emax=false) Float b);
		@RemoteMethod(path="/a/{x}") String getC14c(@Path(n="x",min="1",max="10",emin=true,emax=true) Float b);
		@RemoteMethod(path="/a/{x}") String getC15a(@Path(n="x",min="1",max="10") Double b);
		@RemoteMethod(path="/a/{x}") String getC15b(@Path(n="x",min="1",max="10",emin=false,emax=false) Double b);
		@RemoteMethod(path="/a/{x}") String getC15c(@Path(n="x",min="1",max="10",emin=true,emax=true) Double b);
		@RemoteMethod(path="/a/{x}") String getC16a(@Path(n="x",min="1",max="10") Byte b);
		@RemoteMethod(path="/a/{x}") String getC16b(@Path(n="x",min="1",max="10",emin=false,emax=false) Byte b);
		@RemoteMethod(path="/a/{x}") String getC16c(@Path(n="x",min="1",max="10",emin=true,emax=true) Byte b);
	}

	private static DR dr = MockRestClient.build(D.class).getRemote(DR.class);

	@Test
	public void d01a_int_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC01a(1));
		assertEquals("{x:'10'}", dr.getC01a(10));
		try { dr.getC01a(0); fail(); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC01a(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01b_int_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC01b(1));
		assertEquals("{x:'10'}", dr.getC01b(10));
		try { dr.getC01b(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC01b(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01c_int_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC01c(2));
		assertEquals("{x:'9'}", dr.getC01c(9));
		try { dr.getC01c(1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC01c(10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02a_short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC02a((short)1));
		assertEquals("{x:'10'}", dr.getC02a((short)10));
		try { dr.getC02a((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC02a((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02b_short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC02b((short)1));
		assertEquals("{x:'10'}", dr.getC02b((short)10));
		try { dr.getC02b((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC02b((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02c_short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC02c((short)2));
		assertEquals("{x:'9'}", dr.getC02c((short)9));
		try { dr.getC02c((short)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC02c((short)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03a_long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC03a(1l));
		assertEquals("{x:'10'}", dr.getC03a(10l));
		try { dr.getC03a(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC03a(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03b_long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC03b(1l));
		assertEquals("{x:'10'}", dr.getC03b(10l));
		try { dr.getC03b(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC03b(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03c_long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC03c(2l));
		assertEquals("{x:'9'}", dr.getC03c(9l));
		try { dr.getC03c(1l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC03c(10l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04a_float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC04a(1f));
		assertEquals("{x:'10.0'}", dr.getC04a(10f));
		try { dr.getC04a(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC04a(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04b_float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC04b(1f));
		assertEquals("{x:'10.0'}", dr.getC04b(10f));
		try { dr.getC04b(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC04b(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04c_float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC04c(1.1f));
		assertEquals("{x:'9.9'}", dr.getC04c(9.9f));
		try { dr.getC04c(1f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC04c(10f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05a_double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC05a(1d));
		assertEquals("{x:'10.0'}", dr.getC05a(10d));
		try { dr.getC05a(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC05a(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05b_double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC05b(1d));
		assertEquals("{x:'10.0'}", dr.getC05b(10d));
		try { dr.getC05b(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC05b(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05c_double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC05c(1.1d));
		assertEquals("{x:'9.9'}", dr.getC05c(9.9d));
		try { dr.getC05c(1d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC05c(10d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06a_byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC06a((byte)1));
		assertEquals("{x:'10'}", dr.getC06a((byte)10));
		try { dr.getC06a((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC06a((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06b_byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC06b((byte)1));
		assertEquals("{x:'10'}", dr.getC06b((byte)10));
		try { dr.getC06b((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC06b((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06c_byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC06c((byte)2));
		assertEquals("{x:'9'}", dr.getC06c((byte)9));
		try { dr.getC06c((byte)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC06c((byte)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07a_AtomicInteger_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC07a(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.getC07a(new AtomicInteger(10)));
		try { dr.getC07a(new AtomicInteger(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC07a(new AtomicInteger(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07b_AtomicInteger_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC07b(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.getC07b(new AtomicInteger(10)));
		try { dr.getC07b(new AtomicInteger(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC07b(new AtomicInteger(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07c_AtomicInteger_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC07c(new AtomicInteger(2)));
		assertEquals("{x:'9'}", dr.getC07c(new AtomicInteger(9)));
		try { dr.getC07c(new AtomicInteger(1)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC07c(new AtomicInteger(10)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08a_BigDecimal_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC08a(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.getC08a(new BigDecimal(10)));
		try { dr.getC08a(new BigDecimal(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC08a(new BigDecimal(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08b_BigDecimal_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC08b(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.getC08b(new BigDecimal(10)));
		try { dr.getC08b(new BigDecimal(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC08b(new BigDecimal(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08cBigDecimal_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC08c(new BigDecimal(2)));
		assertEquals("{x:'9'}", dr.getC08c(new BigDecimal(9)));
		try { dr.getC08c(new BigDecimal(1)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC08c(new BigDecimal(10)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d11a_Integer_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC11a(1));
		assertEquals("{x:'10'}", dr.getC11a(10));
		try { dr.getC11a(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC11a(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d11b_Integer_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC11b(1));
		assertEquals("{x:'10'}", dr.getC11b(10));
		try { dr.getC11b(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC11b(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d11c_Integer_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC11c(2));
		assertEquals("{x:'9'}", dr.getC11c(9));
		try { dr.getC11c(1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC11c(10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d12a_Short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC12a((short)1));
		assertEquals("{x:'10'}", dr.getC12a((short)10));
		try { dr.getC12a((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC12a((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d12b_Short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC12b((short)1));
		assertEquals("{x:'10'}", dr.getC12b((short)10));
		try { dr.getC12b((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC12b((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d12c_Short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC12c((short)2));
		assertEquals("{x:'9'}", dr.getC12c((short)9));
		try { dr.getC12c((short)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC12c((short)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d13a_Long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC13a(1l));
		assertEquals("{x:'10'}", dr.getC13a(10l));
		try { dr.getC13a(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC13a(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d13b_Long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC13b(1l));
		assertEquals("{x:'10'}", dr.getC13b(10l));
		try { dr.getC13b(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC13b(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d13c_Long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC13c(2l));
		assertEquals("{x:'9'}", dr.getC13c(9l));
		try { dr.getC13c(1l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC13c(10l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d14a_Float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC14a(1f));
		assertEquals("{x:'10.0'}", dr.getC14a(10f));
		try { dr.getC14a(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC14a(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d14b_Float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC14b(1f));
		assertEquals("{x:'10.0'}", dr.getC14b(10f));
		try { dr.getC14b(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC14b(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d14c_Float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC14c(1.1f));
		assertEquals("{x:'9.9'}", dr.getC14c(9.9f));
		try { dr.getC14c(1f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC14c(10f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d15a_Double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC15a(1d));
		assertEquals("{x:'10.0'}", dr.getC15a(10d));
		try { dr.getC15a(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC15a(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d15b_Double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.getC15b(1d));
		assertEquals("{x:'10.0'}", dr.getC15b(10d));
		try { dr.getC15b(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC15b(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d15c_Double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.getC15c(1.1d));
		assertEquals("{x:'9.9'}", dr.getC15c(9.9d));
		try { dr.getC15c(1d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC15c(10d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d16a_Byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC16a((byte)1));
		assertEquals("{x:'10'}", dr.getC16a((byte)10));
		try { dr.getC16a((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC16a((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d16b_Byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.getC16b((byte)1));
		assertEquals("{x:'10'}", dr.getC16b((byte)10));
		try { dr.getC16b((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC16b((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d16c_Byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.getC16c((byte)2));
		assertEquals("{x:'9'}", dr.getC16c((byte)9));
		try { dr.getC16c((byte)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.getC16c((byte)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}

	//=================================================================================================================
	// @Path(maxItems,minItems,uniqueItems)
	//=================================================================================================================

	@Rest
	public static class E {
		@RestMethod(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface ER {
		@RemoteMethod(path="/{x}") String getE01(@Path(n="x",cf="pipes",mini=1,maxi=2) String...b);
		@RemoteMethod(path="/{x}") String getE02(@Path(n="x",items=@Items(cf="pipes",mini=1,maxi=2)) String[]...b);
		@RemoteMethod(path="/{x}") String getE03(@Path(n="x",cf="pipes",ui=false) String...b);
		@RemoteMethod(path="/{x}") String getE04(@Path(n="x",items=@Items(cf="pipes",ui=false)) String[]...b);
		@RemoteMethod(path="/{x}") String getE05(@Path(n="x",cf="pipes",ui=true) String...b);
		@RemoteMethod(path="/{x}") String getE06(@Path(n="x",items=@Items(cf="pipes",ui=true)) String[]...b);
	}

	private static ER er = MockRestClient.build(E.class).getRemote(ER.class);

	@Test
	public void e01_minMax() throws Exception {
		assertEquals("{x:'1'}", er.getE01("1"));
		assertEquals("{x:'1|2'}", er.getE01("1","2"));
		try { er.getE01(); fail(); } catch (Exception e) { assertContains(e, "Minimum number of items not met"); }
		try { er.getE01("1","2","3"); fail(); } catch (Exception e) { assertContains(e, "Maximum number of items exceeded"); }
		assertEquals("{x:null}", er.getE01((String)null));
	}
	@Test
	public void e02_minMax_items() throws Exception {
		assertEquals("{x:'1'}", er.getE02(new String[]{"1"}));
		assertEquals("{x:'1|2'}", er.getE02(new String[]{"1","2"}));
		try { er.getE02(new String[]{}); fail(); } catch (Exception e) { assertContains(e, "Minimum number of items not met"); }
		try { er.getE02(new String[]{"1","2","3"}); fail(); } catch (Exception e) { assertContains(e, "Maximum number of items exceeded"); }
		assertEquals("{x:null}", er.getE02(new String[]{null}));
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
		try { assertEquals("{x:'1|1'}", er.getE05("1","1")); fail(); } catch (Exception e) { assertContains(e, "Duplicate items not allowed"); }
	}
	@Test
	public void e06_uniqueItems_items_true() throws Exception {
		assertEquals("{x:'1|2'}", er.getE06(new String[]{"1","2"}));
		try { assertEquals("{x:'1|1'}", er.getE06(new String[]{"1","1"})); fail(); } catch (Exception e) { assertContains(e, "Duplicate items not allowed"); }
	}

	//=================================================================================================================
	// @Path(maxLength,minLength,enum)
	//=================================================================================================================

	@Rest
	public static class F {
		@RestMethod(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface FR {
		@RemoteMethod(path="/{x}") String getF01(@Path(n="x",minl=2,maxl=3) String b);
		@RemoteMethod(path="/{x}") String getF02(@Path(n="x",cf="pipes",items=@Items(minl=2,maxl=3)) String...b);
		@RemoteMethod(path="/{x}") String getF03(@Path(n="x",e={"foo"}) String b);
		@RemoteMethod(path="/{x}") String getF04(@Path(n="x",cf="pipes",items=@Items(e={"foo"})) String...b);
		@RemoteMethod(path="/{x}") String getF05(@Path(n="x",p="foo\\d{1,3}") String b);
		@RemoteMethod(path="/{x}") String getF06(@Path(n="x",cf="pipes",items=@Items(p="foo\\d{1,3}")) String...b);
	}

	private static FR fr = MockRestClient.build(F.class).getRemote(FR.class);

	@Test
	public void f01_minMaxLength() throws Exception {
		assertEquals("{x:'12'}", fr.getF01("12"));
		assertEquals("{x:'123'}", fr.getF01("123"));
		try { fr.getF01("1"); fail(); } catch (Exception e) { assertContains(e, "Minimum length of value not met"); }
		try { fr.getF01("1234"); fail(); } catch (Exception e) { assertContains(e, "Maximum length of value exceeded"); }
	}
	@Test
	public void f02_minMaxLength_items() throws Exception {
		assertEquals("{x:'12|34'}", fr.getF02("12","34"));
		assertEquals("{x:'123|456'}", fr.getF02("123","456"));
		try { fr.getF02("1","2"); fail(); } catch (Exception e) { assertContains(e, "Minimum length of value not met"); }
		try { fr.getF02("1234","5678"); fail(); } catch (Exception e) { assertContains(e, "Maximum length of value exceeded"); }
		assertEquals("{x:'12|null'}", fr.getF02("12",null));
	}
	@Test
	public void f03_enum() throws Exception {
		assertEquals("{x:'foo'}", fr.getF03("foo"));
		try { fr.getF03("bar"); fail(); } catch (Exception e) { assertContains(e, "Value does not match one of the expected values.  Must be one of the following: ['foo']"); }
	}
	@Test
	public void f04_enum_items() throws Exception {
		assertEquals("{x:'foo'}", fr.getF04("foo"));
		try { fr.getF04("bar"); fail(); } catch (Exception e) { assertContains(e, "Value does not match one of the expected values.  Must be one of the following: ['foo']"); }
		assertEquals("{x:null}", fr.getF04((String)null));
	}
	@Test
	public void f05_pattern() throws Exception {
		assertEquals("{x:'foo123'}", fr.getF05("foo123"));
		try { fr.getF05("bar"); fail(); } catch (Exception e) { assertContains(e, "Value does not match expected pattern"); }
	}
	@Test
	public void f06_pattern_items() throws Exception {
		assertEquals("{x:'foo123'}", fr.getF06("foo123"));
		try { fr.getF06("foo"); fail(); } catch (Exception e) { assertContains(e, "Value does not match expected pattern"); }
		assertEquals("{x:null}", fr.getF06((String)null));
	}

	//=================================================================================================================
	// @Path(multipleOf)
	//=================================================================================================================

	@Rest
	public static class G {
		@RestMethod(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface GR {
		@RemoteMethod(path="/{x}") String getG01(@Path(n="x",mo="2") int b);
		@RemoteMethod(path="/{x}") String getG02(@Path(n="x",mo="2") short b);
		@RemoteMethod(path="/{x}") String getG03(@Path(n="x",mo="2") long b);
		@RemoteMethod(path="/{x}") String getG04(@Path(n="x",mo="2") float b);
		@RemoteMethod(path="/{x}") String getG05(@Path(n="x",mo="2") double b);
		@RemoteMethod(path="/{x}") String getG06(@Path(n="x",mo="2") byte b);
		@RemoteMethod(path="/{x}") String getG07(@Path(n="x",mo="2") AtomicInteger b);
		@RemoteMethod(path="/{x}") String getG08(@Path(n="x",mo="2") BigDecimal b);
		@RemoteMethod(path="/{x}") String getG11(@Path(n="x",mo="2") Integer b);
		@RemoteMethod(path="/{x}") String getG12(@Path(n="x",mo="2") Short b);
		@RemoteMethod(path="/{x}") String getG13(@Path(n="x",mo="2") Long b);
		@RemoteMethod(path="/{x}") String getG14(@Path(n="x",mo="2") Float b);
		@RemoteMethod(path="/{x}") String getG15(@Path(n="x",mo="2") Double b);
		@RemoteMethod(path="/{x}") String getG16(@Path(n="x",mo="2") Byte b);
	}

	private static GR gr = MockRestClient.build(G.class).getRemote(GR.class);

	@Test
	public void g01_multipleOf_int() throws Exception {
		assertEquals("{x:'4'}", gr.getG01(4));
		try { gr.getG01(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g02_multipleOf_short() throws Exception {
		assertEquals("{x:'4'}", gr.getG02((short)4));
		try { gr.getG02((short)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g03_multipleOf_long() throws Exception {
		assertEquals("{x:'4'}", gr.getG03(4));
		try { gr.getG03(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g04_multipleOf_float() throws Exception {
		assertEquals("{x:'4.0'}", gr.getG04(4));
		try { gr.getG04(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g05_multipleOf_double() throws Exception {
		assertEquals("{x:'4.0'}", gr.getG05(4));
		try { gr.getG05(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g06_multipleOf_byte() throws Exception {
		assertEquals("{x:'4'}", gr.getG06((byte)4));
		try { gr.getG06((byte)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g07_multipleOf_AtomicInteger() throws Exception {
		assertEquals("{x:'4'}", gr.getG07(new AtomicInteger(4)));
		try { gr.getG07(new AtomicInteger(5)); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g08_multipleOf_BigDecimal() throws Exception {
		assertEquals("{x:'4'}", gr.getG08(new BigDecimal(4)));
		try { gr.getG08(new BigDecimal(5)); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g11_multipleOf_Integer() throws Exception {
		assertEquals("{x:'4'}", gr.getG11(4));
		try { gr.getG11(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g12_multipleOf_Short() throws Exception {
		assertEquals("{x:'4'}", gr.getG12((short)4));
		try { gr.getG12((short)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g13_multipleOf_Long() throws Exception {
		assertEquals("{x:'4'}", gr.getG13(4l));
		try { gr.getG13(5l); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g14_multipleOf_Float() throws Exception {
		assertEquals("{x:'4.0'}", gr.getG14(4f));
		try { gr.getG14(5f); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g15_multipleOf_Double() throws Exception {
		assertEquals("{x:'4.0'}", gr.getG15(4d));
		try { gr.getG15(5d); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g16_multipleOf_Byte() throws Exception {
		assertEquals("{x:'4'}", gr.getG16((byte)4));
		try { gr.getG16((byte)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}

	//=================================================================================================================
	// @Path(required)
	//=================================================================================================================

	@Rest
	public static class H {
		@RestMethod(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface HR {
		@RemoteMethod(path="/{x}") String getH01(@Path("x") String b);
	}

	private static HR hr = MockRestClient.build(H.class).getRemote(HR.class);

	@Test
	public void h01_required_default() throws Exception {
		try { hr.getH01(null); fail(); } catch (Exception e) { assertContains(e, "Required value not provided."); }
	}

	//=================================================================================================================
	// @Path(serializer)
	//=================================================================================================================

	@Rest
	public static class J {
		@RestMethod(path="/{x}")
		public String get(@Path("*") OMap m) {
			m.removeAll("/*","/**");
			return m.toString();
		}
	}

	@Remote
	public static interface JR {
		@RemoteMethod(path="/{x}") String getJ01(@Path(n="x",serializer=XPartSerializer.class) String b);
	}

	private static JR jr = MockRestClient.build(J.class).getRemote(JR.class);

	@Test
	public void j01_serializer() throws Exception {
		assertEquals("{x:'xXx'}", jr.getJ01("X"));
	}
}
