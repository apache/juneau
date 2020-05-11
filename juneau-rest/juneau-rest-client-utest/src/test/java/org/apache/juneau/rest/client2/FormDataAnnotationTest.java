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

import static org.junit.Assert.*;
import static org.apache.juneau.testutils.TestUtils.*;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @FormData annotation.
 */
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

	@Rest
	public static class A {
		@RestMethod
		public String postA(@FormData("*") OMap m, @Header("Content-Type") String ct) {
			assertEquals(ct, "application/x-www-form-urlencoded");
			return m.toString();
		}
	}

	@Remote
	public static interface AR {
		@RemoteMethod(path="a") String postA01(@FormData("x") int b);
		@RemoteMethod(path="a") String postA02(@FormData("x") float b);
		@RemoteMethod(path="a") String postA03a(@FormData("x") Bean b);
		@RemoteMethod(path="a") String postA03b(@FormData("*") Bean b);
		@RemoteMethod(path="a") String postA03c(@FormData Bean b);
		@RemoteMethod(path="a") String postA04a(@FormData("x") Bean[] b);
		@RemoteMethod(path="a") String postA04b(@FormData(n="x",cf="uon") Bean[] b);
		@RemoteMethod(path="a") String postA05a(@FormData("x") List<Bean> b);
		@RemoteMethod(path="a") String postA05b(@FormData(n="x",cf="uon") List<Bean> b);
		@RemoteMethod(path="a") String postA06a(@FormData("x") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06b(@FormData("*") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06c(@FormData Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06d(@FormData(n="x",f="uon") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA06e(@FormData(f="uon") Map<String,Bean> b);
		@RemoteMethod(path="a") String postA07a(@FormData("*") Reader b);
		@RemoteMethod(path="a") String postA07b(@FormData Reader b);
		@RemoteMethod(path="a") String postA08a(@FormData("*") InputStream b);
		@RemoteMethod(path="a") String postA08b(@FormData InputStream b);
		@RemoteMethod(path="a") String postA09a(@FormData("*") NameValuePairs b);
		@RemoteMethod(path="a") String postA09b(@FormData NameValuePairs b);
	}

	private static AR ar = MockRemote.build(AR.class, A.class);

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
		assertEquals("{x:'f=1'}", ar.postA03a(Bean.create()));
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
		assertEquals("{x:'f=1,f=1'}", ar.postA04a(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a04b_BeanArray() throws Exception {
		assertEquals("{x:'@((f=1),(f=1))'}", ar.postA04b(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a05a_ListOfBeans() throws Exception {
		assertEquals("{x:'f=1,f=1'}", ar.postA05a(AList.of(Bean.create(),Bean.create())));
	}
	@Test
	public void a05b_ListOfBeans() throws Exception {
		assertEquals("{x:'@((f=1),(f=1))'}", ar.postA05b(AList.of(Bean.create(),Bean.create())));
	}
	@Test
	public void a06a_MapOfBeans() throws Exception {
		assertEquals("{x:'k1=f\\\\=1'}", ar.postA06a(AMap.of("k1",Bean.create())));
	}
	@Test
	public void a06b_MapOfBeans() throws Exception {
		assertEquals("{k1:'f=1'}", ar.postA06b(AMap.of("k1",Bean.create())));
	}
	@Test
	public void a06c_MapOfBeans() throws Exception {
		assertEquals("{k1:'f=1'}", ar.postA06c(AMap.of("k1",Bean.create())));
	}
	@Test
	public void a06d_MapOfBeans() throws Exception {
		assertEquals("{x:'k1=f\\\\=1'}", ar.postA06d(AMap.of("k1",Bean.create())));
	}
	@Test
	public void a06e_MapOfBeans() throws Exception {
		assertEquals("{k1:'f=1'}", ar.postA06e(AMap.of("k1",Bean.create())));
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

	@Rest
	public static class B {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface BR {
		@RemoteMethod(path="/") String postB01(@FormData(n="x",df="foo") String b);
		@RemoteMethod(path="/") String postB02(@FormData(n="x",df="foo",aev=true) String b);
		@RemoteMethod(path="/") String postB03(@FormData(n="x",df="") String b);
		@RemoteMethod(path="/") String postB04(@FormData(n="x",df="",aev=true) String b);
	}

	private static BR br = MockRemote.build(BR.class, B.class);

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

	@Rest
	public static class C {
		@RestMethod
		public String postA(@FormData("*") OMap m) {
			return m.toString();
		}
		@RestMethod
		public Reader postB(@Body Reader b) {
			return b;
		}
	}

	@Remote
	public static interface CR {
		@RemoteMethod(path="/a") String postC01a(@FormData(n="x") String...b);
		@RemoteMethod(path="/b") String postC01b(@FormData(n="x") String...b);
		@RemoteMethod(path="/a") String postC02a(@FormData(n="x",cf="csv") String...b);
		@RemoteMethod(path="/b") String postC02b(@FormData(n="x",cf="csv") String...b);
		@RemoteMethod(path="/a") String postC03a(@FormData(n="x",cf="ssv") String...b);
		@RemoteMethod(path="/b") String postC03b(@FormData(n="x",cf="ssv") String...b);
		@RemoteMethod(path="/a") String postC04a(@FormData(n="x",cf="tsv") String...b);
		@RemoteMethod(path="/b") String postC04b(@FormData(n="x",cf="tsv") String...b);
		@RemoteMethod(path="/a") String postC05a(@FormData(n="x",cf="pipes") String...b);
		@RemoteMethod(path="/b") String postC05b(@FormData(n="x",cf="pipes") String...b);
		@RemoteMethod(path="/a") String postC06a(@FormData(n="x",cf="multi") String...b);
		@RemoteMethod(path="/b") String postC06b(@FormData(n="x",cf="multi") String...b);
		@RemoteMethod(path="/a") String postC07a(@FormData(n="x",cf="uon") String...b);
		@RemoteMethod(path="/b") String postC07b(@FormData(n="x",cf="uon") String...b);
	}

	private static CR cr = MockRemote.build(CR.class, C.class);

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

	@Rest
	public static class D {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface DR {
		@RemoteMethod(path="/") String postC01a(@FormData(n="x",min="1",max="10") int b);
		@RemoteMethod(path="/") String postC01b(@FormData(n="x",min="1",max="10",emin=false,emax=false) int b);
		@RemoteMethod(path="/") String postC01c(@FormData(n="x",min="1",max="10",emin=true,emax=true) int b);
		@RemoteMethod(path="/") String postC01d(@FormData(n="x",min="1",max="10",emin=true,emax=true) int b);
		@RemoteMethod(path="/") String postC02a(@FormData(n="x",min="1",max="10") short b);
		@RemoteMethod(path="/") String postC02b(@FormData(n="x",min="1",max="10",emin=false,emax=false) short b);
		@RemoteMethod(path="/") String postC02c(@FormData(n="x",min="1",max="10",emin=true,emax=true) short b);
		@RemoteMethod(path="/") String postC03a(@FormData(n="x",min="1",max="10") long b);
		@RemoteMethod(path="/") String postC03b(@FormData(n="x",min="1",max="10",emin=false,emax=false) long b);
		@RemoteMethod(path="/") String postC03c(@FormData(n="x",min="1",max="10",emin=true,emax=true) long b);
		@RemoteMethod(path="/") String postC04a(@FormData(n="x",min="1",max="10") float b);
		@RemoteMethod(path="/") String postC04b(@FormData(n="x",min="1",max="10",emin=false,emax=false) float b);
		@RemoteMethod(path="/") String postC04c(@FormData(n="x",min="1",max="10",emin=true,emax=true) float b);
		@RemoteMethod(path="/") String postC05a(@FormData(n="x",min="1",max="10") double b);
		@RemoteMethod(path="/") String postC05b(@FormData(n="x",min="1",max="10",emin=false,emax=false) double b);
		@RemoteMethod(path="/") String postC05c(@FormData(n="x",min="1",max="10",emin=true,emax=true) double b);
		@RemoteMethod(path="/") String postC06a(@FormData(n="x",min="1",max="10") byte b);
		@RemoteMethod(path="/") String postC06b(@FormData(n="x",min="1",max="10",emin=false,emax=false) byte b);
		@RemoteMethod(path="/") String postC06c(@FormData(n="x",min="1",max="10",emin=true,emax=true) byte b);
		@RemoteMethod(path="/") String postC07a(@FormData(n="x",min="1",max="10") AtomicInteger b);
		@RemoteMethod(path="/") String postC07b(@FormData(n="x",min="1",max="10",emin=false,emax=false) AtomicInteger b);
		@RemoteMethod(path="/") String postC07c(@FormData(n="x",min="1",max="10",emin=true,emax=true) AtomicInteger b);
		@RemoteMethod(path="/") String postC08a(@FormData(n="x",min="1",max="10") BigDecimal b);
		@RemoteMethod(path="/") String postC08b(@FormData(n="x",min="1",max="10",emin=false,emax=false) BigDecimal b);
		@RemoteMethod(path="/") String postC08c(@FormData(n="x",min="1",max="10",emin=true,emax=true) BigDecimal b);
		@RemoteMethod(path="/") String postC11a(@FormData(n="x",min="1",max="10") Integer b);
		@RemoteMethod(path="/") String postC11b(@FormData(n="x",min="1",max="10",emin=false,emax=false) Integer b);
		@RemoteMethod(path="/") String postC11c(@FormData(n="x",min="1",max="10",emin=true,emax=true) Integer b);
		@RemoteMethod(path="/") String postC12a(@FormData(n="x",min="1",max="10") Short b);
		@RemoteMethod(path="/") String postC12b(@FormData(n="x",min="1",max="10",emin=false,emax=false) Short b);
		@RemoteMethod(path="/") String postC12c(@FormData(n="x",min="1",max="10",emin=true,emax=true) Short b);
		@RemoteMethod(path="/") String postC13a(@FormData(n="x",min="1",max="10") Long b);
		@RemoteMethod(path="/") String postC13b(@FormData(n="x",min="1",max="10",emin=false,emax=false) Long b);
		@RemoteMethod(path="/") String postC13c(@FormData(n="x",min="1",max="10",emin=true,emax=true) Long b);
		@RemoteMethod(path="/") String postC14a(@FormData(n="x",min="1",max="10") Float b);
		@RemoteMethod(path="/") String postC14b(@FormData(n="x",min="1",max="10",emin=false,emax=false) Float b);
		@RemoteMethod(path="/") String postC14c(@FormData(n="x",min="1",max="10",emin=true,emax=true) Float b);
		@RemoteMethod(path="/") String postC15a(@FormData(n="x",min="1",max="10") Double b);
		@RemoteMethod(path="/") String postC15b(@FormData(n="x",min="1",max="10",emin=false,emax=false) Double b);
		@RemoteMethod(path="/") String postC15c(@FormData(n="x",min="1",max="10",emin=true,emax=true) Double b);
		@RemoteMethod(path="/") String postC16a(@FormData(n="x",min="1",max="10") Byte b);
		@RemoteMethod(path="/") String postC16b(@FormData(n="x",min="1",max="10",emin=false,emax=false) Byte b);
		@RemoteMethod(path="/") String postC16c(@FormData(n="x",min="1",max="10",emin=true,emax=true) Byte b);
	}

	private static DR dr = MockRemote.build(DR.class, D.class);

	@Test
	public void d01a_int_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC01a(1));
		assertEquals("{x:'10'}", dr.postC01a(10));
		try { dr.postC01a(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC01a(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01b_int_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC01b(1));
		assertEquals("{x:'10'}", dr.postC01b(10));
		try { dr.postC01b(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC01b(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d01c_int_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC01c(2));
		assertEquals("{x:'9'}", dr.postC01c(9));
		try { dr.postC01c(1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC01c(10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02a_short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC02a((short)1));
		assertEquals("{x:'10'}", dr.postC02a((short)10));
		try { dr.postC02a((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC02a((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02b_short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC02b((short)1));
		assertEquals("{x:'10'}", dr.postC02b((short)10));
		try { dr.postC02b((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC02b((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d02c_short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC02c((short)2));
		assertEquals("{x:'9'}", dr.postC02c((short)9));
		try { dr.postC02c((short)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC02c((short)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03a_long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC03a(1l));
		assertEquals("{x:'10'}", dr.postC03a(10l));
		try { dr.postC03a(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC03a(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03b_long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC03b(1l));
		assertEquals("{x:'10'}", dr.postC03b(10l));
		try { dr.postC03b(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC03b(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d03c_long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC03c(2l));
		assertEquals("{x:'9'}", dr.postC03c(9l));
		try { dr.postC03c(1l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC03c(10l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04a_float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC04a(1f));
		assertEquals("{x:'10.0'}", dr.postC04a(10f));
		try { dr.postC04a(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC04a(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04b_float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC04b(1f));
		assertEquals("{x:'10.0'}", dr.postC04b(10f));
		try { dr.postC04b(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC04b(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d04c_float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC04c(1.1f));
		assertEquals("{x:'9.9'}", dr.postC04c(9.9f));
		try { dr.postC04c(1f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC04c(10f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05a_double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC05a(1d));
		assertEquals("{x:'10.0'}", dr.postC05a(10d));
		try { dr.postC05a(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC05a(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05b_double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC05b(1d));
		assertEquals("{x:'10.0'}", dr.postC05b(10d));
		try { dr.postC05b(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC05b(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d05c_double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC05c(1.1d));
		assertEquals("{x:'9.9'}", dr.postC05c(9.9d));
		try { dr.postC05c(1d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC05c(10d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06a_byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC06a((byte)1));
		assertEquals("{x:'10'}", dr.postC06a((byte)10));
		try { dr.postC06a((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC06a((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06b_byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC06b((byte)1));
		assertEquals("{x:'10'}", dr.postC06b((byte)10));
		try { dr.postC06b((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC06b((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d06c_byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC06c((byte)2));
		assertEquals("{x:'9'}", dr.postC06c((byte)9));
		try { dr.postC06c((byte)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC06c((byte)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07a_AtomicInteger_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC07a(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.postC07a(new AtomicInteger(10)));
		try { dr.postC07a(new AtomicInteger(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC07a(new AtomicInteger(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07b_AtomicInteger_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC07b(new AtomicInteger(1)));
		assertEquals("{x:'10'}", dr.postC07b(new AtomicInteger(10)));
		try { dr.postC07b(new AtomicInteger(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC07b(new AtomicInteger(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d07c_AtomicInteger_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC07c(new AtomicInteger(2)));
		assertEquals("{x:'9'}", dr.postC07c(new AtomicInteger(9)));
		try { dr.postC07c(new AtomicInteger(1)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC07c(new AtomicInteger(10)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08a_BigDecimal_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC08a(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.postC08a(new BigDecimal(10)));
		try { dr.postC08a(new BigDecimal(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC08a(new BigDecimal(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08b_BigDecimal_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC08b(new BigDecimal(1)));
		assertEquals("{x:'10'}", dr.postC08b(new BigDecimal(10)));
		try { dr.postC08b(new BigDecimal(0)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC08b(new BigDecimal(11)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d08cBigDecimal_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC08c(new BigDecimal(2)));
		assertEquals("{x:'9'}", dr.postC08c(new BigDecimal(9)));
		try { dr.postC08c(new BigDecimal(1)); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC08c(new BigDecimal(10)); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
	}
	@Test
	public void d11a_Integer_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC11a(1));
		assertEquals("{x:'10'}", dr.postC11a(10));
		try { dr.postC11a(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC11a(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC11a(null));
	}
	@Test
	public void d11b_Integer_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC11b(1));
		assertEquals("{x:'10'}", dr.postC11b(10));
		try { dr.postC11b(0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC11b(11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC11b(null));
	}
	@Test
	public void d11c_Integer_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC11c(2));
		assertEquals("{x:'9'}", dr.postC11c(9));
		try { dr.postC11c(1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC11c(10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC11c(null));
	}
	@Test
	public void d12a_Short_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC12a((short)1));
		assertEquals("{x:'10'}", dr.postC12a((short)10));
		try { dr.postC12a((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC12a((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC12a(null));
	}
	@Test
	public void d12b_Short_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC12b((short)1));
		assertEquals("{x:'10'}", dr.postC12b((short)10));
		try { dr.postC12b((short)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC12b((short)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC12b(null));
	}
	@Test
	public void d12c_Short_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC12c((short)2));
		assertEquals("{x:'9'}", dr.postC12c((short)9));
		try { dr.postC12c((short)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC12c((short)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC12c(null));
	}
	@Test
	public void d13a_Long_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC13a(1l));
		assertEquals("{x:'10'}", dr.postC13a(10l));
		try { dr.postC13a(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC13a(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC13a(null));
	}
	@Test
	public void d13b_Long_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC13b(1l));
		assertEquals("{x:'10'}", dr.postC13b(10l));
		try { dr.postC13b(0l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC13b(11l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC13b(null));
	}
	@Test
	public void d13c_Long_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC13c(2l));
		assertEquals("{x:'9'}", dr.postC13c(9l));
		try { dr.postC13c(1l); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC13c(10l); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC13c(null));
	}
	@Test
	public void d14a_Float_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC14a(1f));
		assertEquals("{x:'10.0'}", dr.postC14a(10f));
		try { dr.postC14a(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC14a(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC14a(null));
	}
	@Test
	public void d14b_Float_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC14b(1f));
		assertEquals("{x:'10.0'}", dr.postC14b(10f));
		try { dr.postC14b(0.9f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC14b(10.1f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC14b(null));
	}
	@Test
	public void d14c_Float_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC14c(1.1f));
		assertEquals("{x:'9.9'}", dr.postC14c(9.9f));
		try { dr.postC14c(1f); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC14c(10f); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC14c(null));
	}
	@Test
	public void d15a_Double_defaultExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC15a(1d));
		assertEquals("{x:'10.0'}", dr.postC15a(10d));
		try { dr.postC15a(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC15a(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC15a(null));
	}
	@Test
	public void d15b_Double_notExclusive() throws Exception {
		assertEquals("{x:'1.0'}", dr.postC15b(1d));
		assertEquals("{x:'10.0'}", dr.postC15b(10d));
		try { dr.postC15b(0.9d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC15b(10.1d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC15b(null));
	}
	@Test
	public void d15c_Double_exclusive() throws Exception {
		assertEquals("{x:'1.1'}", dr.postC15c(1.1d));
		assertEquals("{x:'9.9'}", dr.postC15c(9.9d));
		try { dr.postC15c(1d); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC15c(10d); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC15c(null));
	}
	@Test
	public void d16a_Byte_defaultExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC16a((byte)1));
		assertEquals("{x:'10'}", dr.postC16a((byte)10));
		try { dr.postC16a((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC16a((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC16a(null));
	}
	@Test
	public void d16b_Byte_notExclusive() throws Exception {
		assertEquals("{x:'1'}", dr.postC16b((byte)1));
		assertEquals("{x:'10'}", dr.postC16b((byte)10));
		try { dr.postC16b((byte)0); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC16b((byte)11); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC16b(null));
	}
	@Test
	public void d16c_Byte_exclusive() throws Exception {
		assertEquals("{x:'2'}", dr.postC16c((byte)2));
		assertEquals("{x:'9'}", dr.postC16c((byte)9));
		try { dr.postC16c((byte)1); fail(); } catch (Exception e) { assertContains(e, "Minimum value not met"); }
		try { dr.postC16c((byte)10); fail(); } catch (Exception e) { assertContains(e, "Maximum value exceeded"); }
		assertEquals("{}", dr.postC16c(null));
	}

	//=================================================================================================================
	// @FormData(maxItems,minItems,uniqueItems)
	//=================================================================================================================

	@Rest
	public static class E {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface ER {
		@RemoteMethod(path="/") String postE01(@FormData(n="x",cf="pipes",mini=1,maxi=2) String...b);
		@RemoteMethod(path="/") String postE02(@FormData(n="x",items=@Items(cf="pipes",mini=1,maxi=2)) String[]...b);
		@RemoteMethod(path="/") String postE03(@FormData(n="x",cf="pipes",ui=false) String...b);
		@RemoteMethod(path="/") String postE04(@FormData(n="x",items=@Items(cf="pipes",ui=false)) String[]...b);
		@RemoteMethod(path="/") String postE05(@FormData(n="x",cf="pipes",ui=true) String...b);
		@RemoteMethod(path="/") String postE06(@FormData(n="x",items=@Items(cf="pipes",ui=true)) String[]...b);
	}

	private static ER er = MockRemote.build(ER.class, E.class);

	@Test
	public void e01_minMax() throws Exception {
		assertEquals("{x:'1'}", er.postE01("1"));
		assertEquals("{x:'1|2'}", er.postE01("1","2"));
		try { er.postE01(); fail(); } catch (Exception e) { assertContains(e, "Minimum number of items not met"); }
		try { er.postE01("1","2","3"); fail(); } catch (Exception e) { assertContains(e, "Maximum number of items exceeded"); }
		assertEquals("{x:null}", er.postE01((String)null));
	}
	@Test
	public void e02_minMax_items() throws Exception {
		assertEquals("{x:'1'}", er.postE02(new String[]{"1"}));
		assertEquals("{x:'1|2'}", er.postE02(new String[]{"1","2"}));
		try { er.postE02(new String[]{}); fail(); } catch (Exception e) { assertContains(e, "Minimum number of items not met"); }
		try { er.postE02(new String[]{"1","2","3"}); fail(); } catch (Exception e) { assertContains(e, "Maximum number of items exceeded"); }
		assertEquals("{x:null}", er.postE02(new String[]{null}));
	}
	@Test
	public void e03_uniqueItems_false() throws Exception {
		assertEquals("{x:'1|1'}", er.postE03("1","1"));
	}
	@Test
	public void e04_uniqueItems_items_false() throws Exception {
		assertEquals("{x:'1|1'}", er.postE04(new String[]{"1","1"}));
	}
	@Test
	public void e05_uniqueItems_true() throws Exception {
		assertEquals("{x:'1|2'}", er.postE05("1","2"));
		try { er.postE05("1","1"); fail(); } catch (Exception e) { assertContains(e, "Duplicate items not allowed"); }
	}
	@Test
	public void e06_uniqueItems_items_true() throws Exception {
		assertEquals("{x:'1|2'}", er.postE06(new String[]{"1","2"}));
		try { er.postE06(new String[]{"1","1"}); fail(); } catch (Exception e) { assertContains(e, "Duplicate items not allowed"); }
	}

	//=================================================================================================================
	// @FormData(maxLength,minLength,enum,pattern)
	//=================================================================================================================

	@Rest
	public static class F {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface FR {
		@RemoteMethod(path="/") String postF01(@FormData(n="x",minl=2,maxl=3) String b);
		@RemoteMethod(path="/") String postF02(@FormData(n="x",cf="pipes",items=@Items(minl=2,maxl=3)) String...b);
		@RemoteMethod(path="/") String postF03(@FormData(n="x",e={"foo"}) String b);
		@RemoteMethod(path="/") String postF04(@FormData(n="x",cf="pipes",items=@Items(e={"foo"})) String...b);
		@RemoteMethod(path="/") String postF05(@FormData(n="x",p="foo\\d{1,3}") String b);
		@RemoteMethod(path="/") String postF06(@FormData(n="x",cf="pipes",items=@Items(p="foo\\d{1,3}")) String...b);
	}

	private static FR fr = MockRemote.build(FR.class, F.class);

	@Test
	public void f01_minMaxLength() throws Exception {
		assertEquals("{x:'12'}", fr.postF01("12"));
		assertEquals("{x:'123'}", fr.postF01("123"));
		try { fr.postF01("1"); fail(); } catch (Exception e) { assertContains(e, "Minimum length of value not met"); }
		try { fr.postF01("1234"); fail(); } catch (Exception e) { assertContains(e, "Maximum length of value exceeded"); }
		assertEquals("{}", fr.postF01(null));
	}
	@Test
	public void f02_minMaxLength_items() throws Exception {
		assertEquals("{x:'12|34'}", fr.postF02("12","34"));
		assertEquals("{x:'123|456'}", fr.postF02("123","456"));
		try { fr.postF02("1","2"); fail(); } catch (Exception e) { assertContains(e, "Minimum length of value not met"); }
		try { fr.postF02("1234","5678"); fail(); } catch (Exception e) { assertContains(e, "Maximum length of value exceeded"); }
		assertEquals("{x:'12|null'}", fr.postF02("12",null));
	}
	@Test
	public void f03_enum() throws Exception {
		assertEquals("{x:'foo'}", fr.postF03("foo"));
		try { fr.postF03("bar"); fail(); } catch (Exception e) { assertContains(e, "Value does not match one of the expected values"); }
		assertEquals("{}", fr.postF03(null));
	}
	@Test
	public void f04_enum_items() throws Exception {
		assertEquals("{x:'foo'}", fr.postF04("foo"));
		try { fr.postF04("bar"); fail(); } catch (Exception e) { assertContains(e, "Value does not match one of the expected values"); }
		assertEquals("{x:null}", fr.postF04((String)null));
	}
	@Test
	public void f05_pattern() throws Exception {
		assertEquals("{x:'foo123'}", fr.postF05("foo123"));
		try { fr.postF05("bar"); fail(); } catch (Exception e) { assertContains(e, "Value does not match expected pattern"); }
		assertEquals("{}", fr.postF05(null));
	}
	@Test
	public void f06_pattern_items() throws Exception {
		assertEquals("{x:'foo123'}", fr.postF06("foo123"));
		try { fr.postF06("foo"); fail(); } catch (Exception e) { assertContains(e, "Value does not match expected pattern"); }
		assertEquals("{x:null}", fr.postF06((String)null));
	}

	//=================================================================================================================
	// @FormData(multipleOf)
	//=================================================================================================================

	@Rest
	public static class G {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface GR {
		@RemoteMethod(path="/") String postG01(@FormData(n="x",mo="2") int b);
		@RemoteMethod(path="/") String postG02(@FormData(n="x",mo="2") short b);
		@RemoteMethod(path="/") String postG03(@FormData(n="x",mo="2") long b);
		@RemoteMethod(path="/") String postG04(@FormData(n="x",mo="2") float b);
		@RemoteMethod(path="/") String postG05(@FormData(n="x",mo="2") double b);
		@RemoteMethod(path="/") String postG06(@FormData(n="x",mo="2") byte b);
		@RemoteMethod(path="/") String postG07(@FormData(n="x",mo="2") AtomicInteger b);
		@RemoteMethod(path="/") String postG08(@FormData(n="x",mo="2") BigDecimal b);
		@RemoteMethod(path="/") String postG11(@FormData(n="x",mo="2") Integer b);
		@RemoteMethod(path="/") String postG12(@FormData(n="x",mo="2") Short b);
		@RemoteMethod(path="/") String postG13(@FormData(n="x",mo="2") Long b);
		@RemoteMethod(path="/") String postG14(@FormData(n="x",mo="2") Float b);
		@RemoteMethod(path="/") String postG15(@FormData(n="x",mo="2") Double b);
		@RemoteMethod(path="/") String postG16(@FormData(n="x",mo="2") Byte b);
	}

	private static GR gr = MockRemote.build(GR.class, G.class);

	@Test
	public void g01_multipleOf_int() throws Exception {
		assertEquals("{x:'4'}", gr.postG01(4));
		try { gr.postG01(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g02_multipleOf_short() throws Exception {
		assertEquals("{x:'4'}", gr.postG02((short)4));
		try { gr.postG02((short)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g03_multipleOf_long() throws Exception {
		assertEquals("{x:'4'}", gr.postG03(4));
		try { gr.postG03(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g04_multipleOf_float() throws Exception {
		assertEquals("{x:'4.0'}", gr.postG04(4));
		try { gr.postG04(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g05_multipleOf_double() throws Exception {
		assertEquals("{x:'4.0'}", gr.postG05(4));
		try { gr.postG05(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g06_multipleOf_byte() throws Exception {
		assertEquals("{x:'4'}", gr.postG06((byte)4));
		try { gr.postG06((byte)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g07_multipleOf_AtomicInteger() throws Exception {
		assertEquals("{x:'4'}", gr.postG07(new AtomicInteger(4)));
		try { gr.postG07(new AtomicInteger(5)); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g08_multipleOf_BigDecimal() throws Exception {
		assertEquals("{x:'4'}", gr.postG08(new BigDecimal(4)));
		try { gr.postG08(new BigDecimal(5)); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g11_multipleOf_Integer() throws Exception {
		assertEquals("{x:'4'}", gr.postG11(4));
		try { gr.postG11(5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g12_multipleOf_Short() throws Exception {
		assertEquals("{x:'4'}", gr.postG12((short)4));
		try { gr.postG12((short)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g13_multipleOf_Long() throws Exception {
		assertEquals("{x:'4'}", gr.postG13(4l));
		try { gr.postG13(5l); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g14_multipleOf_Float() throws Exception {
		assertEquals("{x:'4.0'}", gr.postG14(4f));
		try { gr.postG14(5f); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g15_multipleOf_Double() throws Exception {
		assertEquals("{x:'4.0'}", gr.postG15(4d));
		try { gr.postG15(5d); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}
	@Test
	public void g16_multipleOf_Byte() throws Exception {
		assertEquals("{x:'4'}", gr.postG16((byte)4));
		try { gr.postG16((byte)5); fail(); } catch (Exception e) { assertContains(e, "Multiple-of not met"); }
	}

	//=================================================================================================================
	// @FormData(required)
	//=================================================================================================================

	@Rest
	public static class H {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface HR {
		@RemoteMethod(path="/") String postH01(@FormData(n="x") String b);
		@RemoteMethod(path="/") String postH02(@FormData(n="x",r=false) String b);
		@RemoteMethod(path="/") String postH03(@FormData(n="x",r=true) String b);
	}

	private static HR hr = MockRemote.build(HR.class, H.class);

	@Test
	public void h01_required_default() throws Exception {
		assertEquals("{}", hr.postH01(null));
	}
	@Test
	public void h02_required_false() throws Exception {
		assertEquals("{}", hr.postH02(null));
	}
	@Test
	public void h03_required_true() throws Exception {
		assertEquals("{x:'1'}", hr.postH03("1"));
		try { hr.postH03(null); fail(); } catch (Exception e) { assertContains(e, "Required value not provided."); }
	}

	//=================================================================================================================
	// @FormData(skipIfEmpty)
	//=================================================================================================================

	@Rest
	public static class I {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface IR {
		@RemoteMethod(path="/") String postI01(@FormData(n="x",aev=true) String b);
		@RemoteMethod(path="/") String postI02(@FormData(n="x",aev=true,sie=false) String b);
		@RemoteMethod(path="/") String postI03(@FormData(n="x",sie=true) String b);
	}

	private static IR ir = MockRemote.build(IR.class, I.class);

	@Test
	public void h01_skipIfEmpty_default() throws Exception {
		assertEquals("{x:''}", ir.postI01(""));
	}
	@Test
	public void h02_skipIfEmpty_false() throws Exception {
		assertEquals("{x:''}", ir.postI02(""));
	}
	@Test
	public void h03_skipIfEmpty_true() throws Exception {
		assertEquals("{}", ir.postI03(""));
	}

	//=================================================================================================================
	// @FormData(serializer)
	//=================================================================================================================

	@Rest
	public static class J {
		@RestMethod
		public String post(@FormData("*") OMap m) {
			return m.toString();
		}
	}

	@Remote
	public static interface JR {
		@RemoteMethod(path="/") String postJ01(@FormData(n="x",serializer=XPartSerializer.class) String b);
	}

	private static JR jr = MockRemote.build(JR.class, J.class);

	@Test
	public void j01_serializer() throws Exception {
		assertEquals("{x:'xXx'}", jr.postJ01("X"));
	}
}
