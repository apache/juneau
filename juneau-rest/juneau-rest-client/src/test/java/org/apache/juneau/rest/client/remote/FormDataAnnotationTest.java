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

import static org.junit.Assert.*;
import static org.apache.juneau.testutils.TestUtils.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @FormData annotation.
 */
@SuppressWarnings({"javadoc","resource"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FormDataAnnotationTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}
	}

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod
		public String postA(@FormData("*") ObjectMap m, @Header("Content-Type") String ct) {
			assertEquals(ct, "application/x-www-form-urlencoded");
			return m.toString();
		}
	}
	private static MockRest a = MockRest.create(A.class);

	@RemoteResource
	public static interface AR {
		@RemoteMethod(path="a") String postA01(@FormData("x") int b);
		@RemoteMethod(path="a") String postA02(@FormData("x") float b);
		@RemoteMethod(path="a") String postA03a(@FormData("x") Bean b);
		@RemoteMethod(path="a") String postA03b(@FormData("*") Bean b);
		@RemoteMethod(path="a") String postA03c(@FormData Bean b);
		@RemoteMethod(path="a") String postA04a(@FormData("x") Bean[] b);
		@RemoteMethod(path="a") String postA04b(@FormData(name="x",collectionFormat="uon") Bean[] b);
		@RemoteMethod(path="a") String postA05a(@FormData("x") List<Bean> b);
		@RemoteMethod(path="a") String postA05b(@FormData(name="x",collectionFormat="uon") List<Bean> b);
		@RemoteMethod(path="a") String postA06a(@FormData("x") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06b(@FormData("*") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06c(@FormData Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06d(@FormData(name="x",format="uon") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06e(@FormData(format="uon") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA07a(@FormData("*") Reader b);
		@RemoteMethod(path="a") String postA07b(@FormData Reader b);
		@RemoteMethod(path="a") String postA08a(@FormData("*") InputStream b);
		@RemoteMethod(path="a") String postA08b(@FormData InputStream b);
		@RemoteMethod(path="a") String postA09a(@FormData("*") NameValuePairs b);
		@RemoteMethod(path="a") String postA09b(@FormData NameValuePairs b);
	}

	private static AR ar = RestClient.create().mockHttpConnection(a).build().getRemoteResource(AR.class);

	@Test
	public void a01_int() throws Exception {
		assertEquals("{x:'1'}", ar.postA01(1));
	}
	@Test
	public void a02_float() throws Exception {
		assertEquals("{x:'1.0'}", ar.postA02(1));
	}
	@Test
	public void a03a_Bean() throws Exception {
		assertEquals("{x:'(f=1)'}", ar.postA03a(Bean.create()));
	}
	@Test
	public void a03b_Bean() throws Exception {
		assertEquals("{f:'1'}", ar.postA03b(Bean.create()));
	}
	@Test
	public void a03c_Bean() throws Exception {
		assertEquals("{f:'1'}", ar.postA03c(Bean.create()));
	}
	@Test
	public void a04a_BeanArray() throws Exception {
		assertEquals("{x:'(f=1),(f=1)'}", ar.postA04a(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a04b_BeanArray() throws Exception {
		assertEquals("{x:'@((f=1),(f=1))'}", ar.postA04b(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a05a_ListOfBeans() throws Exception {
		assertEquals("{x:'(f=1),(f=1)'}", ar.postA05a(AList.create(Bean.create(),Bean.create())));
	}
	@Test
	public void a05b_ListOfBeans() throws Exception {
		assertEquals("{x:'@((f=1),(f=1))'}", ar.postA05b(AList.create(Bean.create(),Bean.create())));
	}
	@Test
	public void a06a_MapOfBeans() throws Exception {
		assertEquals("{x:'(k1=(f=1))'}", ar.postA06a(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06b_MapOfBeans() throws Exception {
		assertEquals("{k1:'(f=1)'}", ar.postA06b(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06c_MapOfBeans() throws Exception {
		assertEquals("{k1:'(f=1)'}", ar.postA06c(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06d_MapOfBeans() throws Exception {
		assertEquals("{x:'(k1=(f=1))'}", ar.postA06d(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06e_MapOfBeans() throws Exception {
		assertEquals("{k1:'(f=1)'}", ar.postA06e(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a07a_Reader() throws Exception {
		assertEquals("{x:'1'}", ar.postA07a(new StringReader("x=1")));
	}
	@Test
	public void a07b_Reader() throws Exception {
		assertEquals("{x:'1'}", ar.postA07b(new StringReader("x=1")));
	}
	@Test
	public void a08a_InputStream() throws Exception {
		assertEquals("{x:'1'}", ar.postA08a(new StringInputStream("x=1")));
	}
	@Test
	public void a08b_InputStream() throws Exception {
		assertEquals("{x:'1'}", ar.postA08b(new StringInputStream("x=1")));
	}
	@Test
	public void a09a_NameValuePairs() throws Exception {
		assertEquals("{foo:'bar'}", ar.postA09a(new NameValuePairs().append("foo", "bar")));
	}
	@Test
	public void a09b_NameValuePairs() throws Exception {
		assertEquals("{foo:'bar'}", ar.postA09b(new NameValuePairs().append("foo", "bar")));
	}

	//=================================================================================================================
	// @FormData(_default/allowEmptyValue)
	//=================================================================================================================

	@RestResource
	public static class B {
		@RestMethod
		public String post(@FormData("*") ObjectMap m) {
			return m.toString();
		}
	}
	private static MockRest b = MockRest.create(B.class);

	@RemoteResource
	public static interface BR {
		@RemoteMethod(path="/") String postB01(@FormData(name="x",_default="foo") String b);
		@RemoteMethod(path="/") String postB02(@FormData(name="x",_default="foo",allowEmptyValue=true) String b);
		@RemoteMethod(path="/") String postB03(@FormData(name="x",_default="") String b);
		@RemoteMethod(path="/") String postB04(@FormData(name="x",_default="",allowEmptyValue=true) String b);
	}

	private static BR br = RestClient.create().mockHttpConnection(b).build().getRemoteResource(BR.class);

	@Test
	public void b01a_default() throws Exception {
		assertEquals("{x:'foo'}", br.postB01(null));
	}
	@Test
	public void b01b_default_emptyString() throws Exception {
		try {
			br.postB01("");
		} catch (Exception e) {
			assertContains(e, "Empty value not allowed");
		}
	}
	@Test
	public void b02a_default_allowEmptyValue() throws Exception {
		assertEquals("{x:'foo'}", br.postB02(null));
	}
	@Test
	public void b02b_default_allowEmptyValue_emptyString() throws Exception {
		assertEquals("{x:''}", br.postB02(""));
	}
	@Test
	public void b03a_defaultIsBlank() throws Exception {
		assertEquals("{x:''}", br.postB03(null));
	}
	@Test
	public void b03b_defaultIsBlank_emptyString() throws Exception {
		try {
			br.postB03("");
		} catch (Exception e) {
			assertContains(e, "Empty value not allowed");
		}
	}
	@Test
	public void b04a_defaultIsBlank_allowEmptyValue() throws Exception {
		assertEquals("{x:''}", br.postB04(null));
	}
	@Test
	public void b04b_defaultIsBlank_allowEmptyValue_emptyString() throws Exception {
		assertEquals("{x:''}", br.postB04(""));
	}

	//=================================================================================================================
	// @FormData(collectionFormat)
	//=================================================================================================================

	@RestResource
	public static class C {
		@RestMethod
		public String postA(@FormData("*") ObjectMap m) {
			return m.toString();
		}
		@RestMethod
		public Reader postB(@Body Reader b) {
			return b;
		}
	}
	private static MockRest c = MockRest.create(C.class);

	@RemoteResource
	public static interface CR {
		@RemoteMethod(path="/a") String postC01a(@FormData(name="x") String...b);
		@RemoteMethod(path="/b") String postC01b(@FormData(name="x") String...b);
		@RemoteMethod(path="/a") String postC02a(@FormData(name="x",collectionFormat="csv") String...b);
		@RemoteMethod(path="/b") String postC02b(@FormData(name="x",collectionFormat="csv") String...b);
		@RemoteMethod(path="/a") String postC03a(@FormData(name="x",collectionFormat="ssv") String...b);
		@RemoteMethod(path="/b") String postC03b(@FormData(name="x",collectionFormat="ssv") String...b);
		@RemoteMethod(path="/a") String postC04a(@FormData(name="x",collectionFormat="tsv") String...b);
		@RemoteMethod(path="/b") String postC04b(@FormData(name="x",collectionFormat="tsv") String...b);
		@RemoteMethod(path="/a") String postC05a(@FormData(name="x",collectionFormat="pipes") String...b);
		@RemoteMethod(path="/b") String postC05b(@FormData(name="x",collectionFormat="pipes") String...b);
		@RemoteMethod(path="/a") String postC06a(@FormData(name="x",collectionFormat="multi") String...b);
		@RemoteMethod(path="/b") String postC06b(@FormData(name="x",collectionFormat="multi") String...b);
		@RemoteMethod(path="/a") String postC07a(@FormData(name="x",collectionFormat="uon") String...b);
		@RemoteMethod(path="/b") String postC07b(@FormData(name="x",collectionFormat="uon") String...b);
	}

	private static CR cr = RestClient.create().mockHttpConnection(c).build().getRemoteResource(CR.class);

	@Test
	public void c01a_default() throws Exception {
		assertEquals("{x:'foo,bar'}", cr.postC01a("foo","bar"));
	}
	@Test
	public void c01b_default_raw() throws Exception {
		assertEquals("x=foo%2Cbar", cr.postC01b("foo","bar"));
	}
	@Test
	public void c02a_csv() throws Exception {
		assertEquals("{x:'foo,bar'}", cr.postC02a("foo","bar"));
	}
	@Test
	public void c02b_csv_raw() throws Exception {
		assertEquals("x=foo%2Cbar", cr.postC02b("foo","bar"));
	}
	@Test
	public void c03a_ssv() throws Exception {
		assertEquals("{x:'foo bar'}", cr.postC03a("foo","bar"));
	}
	@Test
	public void c03b_ssv_raw() throws Exception {
		assertEquals("x=foo+bar", cr.postC03b("foo","bar"));
	}
	@Test
	public void c04a_tsv() throws Exception {
		assertEquals("{x:'foo\\tbar'}", cr.postC04a("foo","bar"));
	}
	@Test
	public void c04b_tsv_raw() throws Exception {
		assertEquals("x=foo%09bar", cr.postC04b("foo","bar"));
	}
	@Test
	public void c05a_pipes() throws Exception {
		assertEquals("{x:'foo|bar'}", cr.postC05a("foo","bar"));
	}
	@Test
	public void c05b_pipes_raw() throws Exception {
		assertEquals("x=foo%7Cbar", cr.postC05b("foo","bar"));
	}
	@Test
	public void c06a_multi() throws Exception {
		// Not supported, but should be treated as csv.
		assertEquals("{x:'foo,bar'}", cr.postC06a("foo","bar"));
	}
	@Test
	public void c06b_multi_raw() throws Exception {
		// Not supported, but should be treated as csv.
		assertEquals("x=foo%2Cbar", cr.postC06b("foo","bar"));
	}
	@Test
	public void c07a_uon() throws Exception {
		assertEquals("{x:'@(foo,bar)'}", cr.postC07a("foo","bar"));
	}
	@Test
	public void c07b_uon_raw() throws Exception {
		assertEquals("x=%40%28foo%2Cbar%29", cr.postC07b("foo","bar"));
	}

	//=================================================================================================================
	// @FormData(maximum,exclusiveMaximum,minimum,exclusiveMinimum)
	//=================================================================================================================

	@RestResource
	public static class D {
		@RestMethod
		public String post(@FormData("*") ObjectMap m) {
			return m.toString();
		}
	}
	private static MockRest d = MockRest.create(D.class);

	@RemoteResource
	public static interface DR {
		@RemoteMethod(path="/") String postC01a(@FormData(name="x",minimum="1",maximum="10") int b);
		@RemoteMethod(path="/") String postC01b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) int b);
		@RemoteMethod(path="/") String postC01c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) int b);
		@RemoteMethod(path="/") String postC02a(@FormData(name="x",minimum="1",maximum="10") short b);
		@RemoteMethod(path="/") String postC02b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) short b);
		@RemoteMethod(path="/") String postC02c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) short b);
		@RemoteMethod(path="/") String postC03a(@FormData(name="x",minimum="1",maximum="10") long b);
		@RemoteMethod(path="/") String postC03b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) long b);
		@RemoteMethod(path="/") String postC03c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) long b);
		@RemoteMethod(path="/") String postC04a(@FormData(name="x",minimum="1",maximum="10") float b);
		@RemoteMethod(path="/") String postC04b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) float b);
		@RemoteMethod(path="/") String postC04c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) float b);
		@RemoteMethod(path="/") String postC05a(@FormData(name="x",minimum="1",maximum="10") double b);
		@RemoteMethod(path="/") String postC05b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) double b);
		@RemoteMethod(path="/") String postC05c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) double b);
		@RemoteMethod(path="/") String postC06a(@FormData(name="x",minimum="1",maximum="10") byte b);
		@RemoteMethod(path="/") String postC06b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) byte b);
		@RemoteMethod(path="/") String postC06c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) byte b);
		@RemoteMethod(path="/") String postC07a(@FormData(name="x",minimum="1",maximum="10") AtomicInteger b);
		@RemoteMethod(path="/") String postC07b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) AtomicInteger b);
		@RemoteMethod(path="/") String postC07c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) AtomicInteger b);
		@RemoteMethod(path="/") String postC08a(@FormData(name="x",minimum="1",maximum="10") BigDecimal b);
		@RemoteMethod(path="/") String postC08b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) BigDecimal b);
		@RemoteMethod(path="/") String postC08c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) BigDecimal b);
		@RemoteMethod(path="/") String postC11a(@FormData(name="x",minimum="1",maximum="10") Integer b);
		@RemoteMethod(path="/") String postC11b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Integer b);
		@RemoteMethod(path="/") String postC11c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Integer b);
		@RemoteMethod(path="/") String postC12a(@FormData(name="x",minimum="1",maximum="10") Short b);
		@RemoteMethod(path="/") String postC12b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Short b);
		@RemoteMethod(path="/") String postC12c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Short b);
		@RemoteMethod(path="/") String postC13a(@FormData(name="x",minimum="1",maximum="10") Long b);
		@RemoteMethod(path="/") String postC13b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Long b);
		@RemoteMethod(path="/") String postC13c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Long b);
		@RemoteMethod(path="/") String postC14a(@FormData(name="x",minimum="1",maximum="10") Float b);
		@RemoteMethod(path="/") String postC14b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Float b);
		@RemoteMethod(path="/") String postC14c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Float b);
		@RemoteMethod(path="/") String postC15a(@FormData(name="x",minimum="1",maximum="10") Double b);
		@RemoteMethod(path="/") String postC15b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Double b);
		@RemoteMethod(path="/") String postC15c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Double b);
		@RemoteMethod(path="/") String postC16a(@FormData(name="x",minimum="1",maximum="10") Byte b);
		@RemoteMethod(path="/") String postC16b(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=false,exclusiveMaximum=false) Byte b);
		@RemoteMethod(path="/") String postC16c(@FormData(name="x",minimum="1",maximum="10",exclusiveMinimum=true,exclusiveMaximum=true) Byte b);
	}

	private static DR dr = RestClient.create().mockHttpConnection(d).build().getRemoteResource(DR.class);

	@Test
	public void d01a_int_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC01a(1));
		assertEquals("{x:'10'}", dr.postC01a(10));
		try { dr.postC01a(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC01a(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01b_int_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC01b(1));
		assertEquals("{x:'10'}", dr.postC01b(10));
		try { dr.postC01b(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC01b(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01c_int_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC01c(2));
		assertEquals("{x:'9'}", dr.postC01c(9));
		try { dr.postC01c(1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC01c(10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02a_short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC02a((short)1));
		assertEquals("{x:'10'}", dr.postC02a((short)10));
		try { dr.postC02a((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC02a((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02b_short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC02b((short)1));
		assertEquals("{x:'10'}", dr.postC02b((short)10));
		try { dr.postC02b((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC02b((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02c_short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC02c((short)2));
		assertEquals("{x:'9'}", dr.postC02c((short)9));
		try { dr.postC02c((short)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC02c((short)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03a_long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC03a(1l));
		assertEquals("{x:'10'}", dr.postC03a(10l));
		try { dr.postC03a(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC03a(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03b_long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC03b(1l));
		assertEquals("{x:'10'}", dr.postC03b(10l));
		try { dr.postC03b(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC03b(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03c_long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC03c(2l));
		assertEquals("{x:'9'}", dr.postC03c(9l));
		try { dr.postC03c(1l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC03c(10l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04a_float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC04a(1f));
		assertEquals("{x:'10.0'}", dr.postC04a(10f));
		try { dr.postC04a(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC04a(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04b_float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC04b(1f));
		assertEquals("{x:'10.0'}", dr.postC04b(10f));
		try { dr.postC04b(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC04b(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04c_float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC04c(1.1f));
		assertEquals("{x:'9.9'}", dr.postC04c(9.9f));
		try { dr.postC04c(1f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC04c(10f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05a_double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC05a(1d));
		assertEquals("{x:'10.0'}", dr.postC05a(10d));
		try { dr.postC05a(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC05a(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05b_double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC05b(1d));
		assertEquals("{x:'10.0'}", dr.postC05b(10d));
		try { dr.postC05b(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC05b(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05c_double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC05c(1.1d));
		assertEquals("{x:'9.9'}", dr.postC05c(9.9d));
		try { dr.postC05c(1d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC05c(10d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06a_byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC06a((byte)1));
		assertEquals("{x:'10'}", dr.postC06a((byte)10));
		try { dr.postC06a((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC06a((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06b_byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC06b((byte)1));
		assertEquals("{x:'10'}", dr.postC06b((byte)10));
		try { dr.postC06b((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC06b((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06c_byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC06c((byte)2));
		assertEquals("{x:'9'}", dr.postC06c((byte)9));
		try { dr.postC06c((byte)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC06c((byte)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07a_AtomicInteger_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC07a(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.postC07a(new AtomicInteger(10)));
		try { dr.postC07a(new AtomicInteger(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC07a(new AtomicInteger(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07b_AtomicInteger_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC07b(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.postC07b(new AtomicInteger(10)));
		try { dr.postC07b(new AtomicInteger(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC07b(new AtomicInteger(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07c_AtomicInteger_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC07c(new AtomicInteger(2)));
		assertEquals("{x:'9'}", dr.postC07c(new AtomicInteger(9)));
		try { dr.postC07c(new AtomicInteger(1)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC07c(new AtomicInteger(10)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08a_BigDecimal_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC08a(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.postC08a(new BigDecimal(10)));
		try { dr.postC08a(new BigDecimal(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC08a(new BigDecimal(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08b_BigDecimal_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC08b(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.postC08b(new BigDecimal(10)));
		try { dr.postC08b(new BigDecimal(0)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC08b(new BigDecimal(11)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08cBigDecimal_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC08c(new BigDecimal(2)));
		assertEquals("{x:'9'}", dr.postC08c(new BigDecimal(9)));
		try { dr.postC08c(new BigDecimal(1)); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC08c(new BigDecimal(10)); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d11a_Integer_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC11a(1));
		assertEquals("{x:'10'}", dr.postC11a(10));
		try { dr.postC11a(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC11a(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC11a(null));
	}
	@Test
	public void d11b_Integer_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC11b(1));
		assertEquals("{x:'10'}", dr.postC11b(10));
		try { dr.postC11b(0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC11b(11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC11b(null));
	}
	@Test
	public void d11c_Integer_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC11c(2));
		assertEquals("{x:'9'}", dr.postC11c(9));
		try { dr.postC11c(1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC11c(10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC11c(null));
	}
	@Test
	public void d12a_Short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC12a((short)1));
		assertEquals("{x:'10'}", dr.postC12a((short)10));
		try { dr.postC12a((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC12a((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC12a(null));
	}
	@Test
	public void d12b_Short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC12b((short)1));
		assertEquals("{x:'10'}", dr.postC12b((short)10));
		try { dr.postC12b((short)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC12b((short)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC12b(null));
	}
	@Test
	public void d12c_Short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC12c((short)2));
		assertEquals("{x:'9'}", dr.postC12c((short)9));
		try { dr.postC12c((short)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC12c((short)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC12c(null));
	}
	@Test
	public void d13a_Long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC13a(1l));
		assertEquals("{x:'10'}", dr.postC13a(10l));
		try { dr.postC13a(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC13a(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC13a(null));
	}
	@Test
	public void d13b_Long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC13b(1l));
		assertEquals("{x:'10'}", dr.postC13b(10l));
		try { dr.postC13b(0l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC13b(11l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC13b(null));
	}
	@Test
	public void d13c_Long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC13c(2l));
		assertEquals("{x:'9'}", dr.postC13c(9l));
		try { dr.postC13c(1l); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC13c(10l); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC13c(null));
	}
	@Test
	public void d14a_Float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC14a(1f));
		assertEquals("{x:'10.0'}", dr.postC14a(10f));
		try { dr.postC14a(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC14a(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC14a(null));
	}
	@Test
	public void d14b_Float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC14b(1f));
		assertEquals("{x:'10.0'}", dr.postC14b(10f));
		try { dr.postC14b(0.9f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC14b(10.1f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC14b(null));
	}
	@Test
	public void d14c_Float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC14c(1.1f));
		assertEquals("{x:'9.9'}", dr.postC14c(9.9f));
		try { dr.postC14c(1f); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC14c(10f); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC14c(null));
	}
	@Test
	public void d15a_Double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC15a(1d));
		assertEquals("{x:'10.0'}", dr.postC15a(10d));
		try { dr.postC15a(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC15a(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC15a(null));
	}
	@Test
	public void d15b_Double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC15b(1d));
		assertEquals("{x:'10.0'}", dr.postC15b(10d));
		try { dr.postC15b(0.9d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC15b(10.1d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC15b(null));
	}
	@Test
	public void d15c_Double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC15c(1.1d));
		assertEquals("{x:'9.9'}", dr.postC15c(9.9d));
		try { dr.postC15c(1d); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC15c(10d); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC15c(null));
	}
	@Test
	public void d16a_Byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC16a((byte)1));
		assertEquals("{x:'10'}", dr.postC16a((byte)10));
		try { dr.postC16a((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC16a((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC16a(null));
	}
	@Test
	public void d16b_Byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC16b((byte)1));
		assertEquals("{x:'10'}", dr.postC16b((byte)10));
		try { dr.postC16b((byte)0); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC16b((byte)11); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC16b(null));
	}
	@Test
	public void d16c_Byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC16c((byte)2));
		assertEquals("{x:'9'}", dr.postC16c((byte)9));
		try { dr.postC16c((byte)1); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC16c((byte)10); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC16c(null));
	}
}
