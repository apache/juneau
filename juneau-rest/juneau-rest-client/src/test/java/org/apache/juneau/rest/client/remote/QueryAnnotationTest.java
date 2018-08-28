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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @Query annotation.
 */
@SuppressWarnings({"javadoc","resource"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class QueryAnnotationTest {

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
		public String getA(@Query("*") ObjectMap m) {
			return m.toString();
		}
	}
	private static MockRest a = MockRest.create(A.class);

	@RemoteResource
	public static interface A01 {
		@RemoteMethod(path="a") String getA01(@Query("x") int b);
		@RemoteMethod(path="a") String getA02(@Query("x") float b);
		@RemoteMethod(path="a") String getA03a(@Query("x") Bean b);
		@RemoteMethod(path="a") String getA03b(@Query("*") Bean b);
		@RemoteMethod(path="a") String getA03c(@Query Bean b);
		@RemoteMethod(path="a") String getA04a(@Query("x") Bean[] b);
		@RemoteMethod(path="a") String getA04b(@Query(name="x",collectionFormat="uon") Bean[] b);
		@RemoteMethod(path="a") String getA05a(@Query("x") List<Bean> b);
		@RemoteMethod(path="a") String getA05b(@Query(name="x",collectionFormat="uon") List<Bean> b);
		@RemoteMethod(path="a") String getA06a(@Query("x") Map<String,Bean> b);
		@RemoteMethod(path="a") String getA06b(@Query("*") Map<String,Bean> b);
		@RemoteMethod(path="a") String getA06c(@Query Map<String,Bean> b);
		@RemoteMethod(path="a") String getA06d(@Query(name="x",format="uon") Map<String,Bean> b);
		@RemoteMethod(path="a") String getA06e(@Query(format="uon") Map<String,Bean> b);
		@RemoteMethod(path="a") String getA07a(@Query("*") Reader b);
		@RemoteMethod(path="a") String getA07b(@Query Reader b);
		@RemoteMethod(path="a") String getA08a(@Query("*") InputStream b);
		@RemoteMethod(path="a") String getA08b(@Query InputStream b);
		@RemoteMethod(path="a") String getA09a(@Query("*") NameValuePairs b);
		@RemoteMethod(path="a") String getA09b(@Query NameValuePairs b);
	}

	private static A01 a01 = RestClient.create().mockHttpConnection(a).build().getRemoteResource(A01.class);

	@Test
	public void a01_int() throws Exception {
		assertEquals("{x:'1'}", a01.getA01(1));
	}
	@Test
	public void a02_float() throws Exception {
		assertEquals("{x:'1.0'}", a01.getA02(1));
	}
	@Test
	public void a03a_Bean() throws Exception {
		assertEquals("{x:'(f=1)'}", a01.getA03a(Bean.create()));
	}
	@Test
	public void a03b_Bean() throws Exception {
		assertEquals("{f:'1'}", a01.getA03b(Bean.create()));
	}
	@Test
	public void a03c_Bean() throws Exception {
		assertEquals("{f:'1'}", a01.getA03c(Bean.create()));
	}
	@Test
	public void a04a_BeanArray() throws Exception {
		assertEquals("{x:'(f=1),(f=1)'}", a01.getA04a(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a04b_BeanArray() throws Exception {
		assertEquals("{x:'@((f=1),(f=1))'}", a01.getA04b(new Bean[]{Bean.create(),Bean.create()}));
	}
	@Test
	public void a05a_ListOfBeans() throws Exception {
		assertEquals("{x:'(f=1),(f=1)'}", a01.getA05a(AList.create(Bean.create(),Bean.create())));
	}
	@Test
	public void a05b_ListOfBeans() throws Exception {
		assertEquals("{x:'@((f=1),(f=1))'}", a01.getA05b(AList.create(Bean.create(),Bean.create())));
	}
	@Test
	public void a06a_MapOfBeans() throws Exception {
		assertEquals("{x:'(k1=(f=1))'}", a01.getA06a(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06b_MapOfBeans() throws Exception {
		assertEquals("{k1:'(f=1)'}", a01.getA06b(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06c_MapOfBeans() throws Exception {
		assertEquals("{k1:'(f=1)'}", a01.getA06c(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06d_MapOfBeans() throws Exception {
		assertEquals("{x:'(k1=(f=1))'}", a01.getA06d(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a06e_MapOfBeans() throws Exception {
		assertEquals("{k1:'(f=1)'}", a01.getA06e(AMap.create("k1",Bean.create())));
	}
	@Test
	public void a07a_Reader() throws Exception {
		assertEquals("{x:'1'}", a01.getA07a(new StringReader("x=1")));
	}
	@Test
	public void a07b_Reader() throws Exception {
		assertEquals("{x:'1'}", a01.getA07b(new StringReader("x=1")));
	}
	@Test
	public void a08a_InputStream() throws Exception {
		assertEquals("{x:'1'}", a01.getA08a(new StringInputStream("x=1")));
	}
	@Test
	public void a08b_InputStream() throws Exception {
		assertEquals("{x:'1'}", a01.getA08b(new StringInputStream("x=1")));
	}
	@Test
	public void a09a_NameValuePairs() throws Exception {
		assertEquals("{foo:'bar'}", a01.getA09a(new NameValuePairs().append("foo", "bar")));
	}
	@Test
	public void a09b_NameValuePairs() throws Exception {
		assertEquals("{foo:'bar'}", a01.getA09b(new NameValuePairs().append("foo", "bar")));
	}

	//=================================================================================================================
	// @Query(_default/allowEmptyValue)
	//=================================================================================================================

	@RestResource
	public static class B {
		@RestMethod
		public String get(@Query("*") ObjectMap m) {
			return m.toString();
		}
	}
	private static MockRest b = MockRest.create(B.class);

	@RemoteResource
	public static interface BR {
		@RemoteMethod(path="/") String getB01(@Query(name="x",_default="foo") String b);
		@RemoteMethod(path="/") String getB02(@Query(name="x",_default="foo",allowEmptyValue=true) String b);
		@RemoteMethod(path="/") String getB03(@Query(name="x",_default="") String b);
		@RemoteMethod(path="/") String getB04(@Query(name="x",_default="",allowEmptyValue=true) String b);
	}

	private static BR br = RestClient.create().mockHttpConnection(b).build().getRemoteResource(BR.class);

	@Test
	public void b01a_default() throws Exception {
		assertEquals("{x:'foo'}", br.getB01(null));
	}
	@Test
	public void b01b_default_emptyString() throws Exception {
		try {
			br.getB01("");
		} catch (Exception e) {
			assertContains(e, "Empty value not allowed");
		}
	}
	@Test
	public void b02a_default_allowEmptyValue() throws Exception {
		assertEquals("{x:'foo'}", br.getB02(null));
	}
	@Test
	public void b02b_default_allowEmptyValue_emptyString() throws Exception {
		assertEquals("{x:''}", br.getB02(""));
	}
	@Test
	public void b03a_defaultIsBlank() throws Exception {
		assertEquals("{x:''}", br.getB03(null));
	}
	@Test
	public void b03b_defaultIsBlank_emptyString() throws Exception {
		try {
			br.getB03("");
		} catch (Exception e) {
			assertContains(e, "Empty value not allowed");
		}
	}
	@Test
	public void b04a_defaultIsBlank_allowEmptyValue() throws Exception {
		assertEquals("{x:''}", br.getB04(null));
	}
	@Test
	public void b04b_defaultIsBlank_allowEmptyValue_emptyString() throws Exception {
		assertEquals("{x:''}", br.getB04(""));
	}

	//=================================================================================================================
	// @Query(collectionFormat)
	//=================================================================================================================

	@RestResource
	public static class C {
		@RestMethod
		public String getA(@Query("*") ObjectMap m) {
			return m.toString();
		}
		@RestMethod
		public Reader getB(RestRequest req) {
			return new StringReader(req.getQueryString());
		}
	}
	private static MockRest c = MockRest.create(C.class);

	@RemoteResource
	public static interface CR {
		@RemoteMethod(path="/a") String getC01a(@Query(name="x") String...b);
		@RemoteMethod(path="/b") String getC01b(@Query(name="x") String...b);
		@RemoteMethod(path="/a") String getC02a(@Query(name="x",collectionFormat="csv") String...b);
		@RemoteMethod(path="/b") String getC02b(@Query(name="x",collectionFormat="csv") String...b);
		@RemoteMethod(path="/a") String getC03a(@Query(name="x",collectionFormat="ssv") String...b);
		@RemoteMethod(path="/b") String getC03b(@Query(name="x",collectionFormat="ssv") String...b);
		@RemoteMethod(path="/a") String getC04a(@Query(name="x",collectionFormat="tsv") String...b);
		@RemoteMethod(path="/b") String getC04b(@Query(name="x",collectionFormat="tsv") String...b);
		@RemoteMethod(path="/a") String getC05a(@Query(name="x",collectionFormat="pipes") String...b);
		@RemoteMethod(path="/b") String getC05b(@Query(name="x",collectionFormat="pipes") String...b);
		@RemoteMethod(path="/a") String getC06a(@Query(name="x",collectionFormat="multi") String...b);
		@RemoteMethod(path="/b") String getC06b(@Query(name="x",collectionFormat="multi") String...b);
		@RemoteMethod(path="/a") String getC07a(@Query(name="x",collectionFormat="uon") String...b);
		@RemoteMethod(path="/b") String getC07b(@Query(name="x",collectionFormat="uon") String...b);
	}

	private static CR cr = RestClient.create().mockHttpConnection(c).build().getRemoteResource(CR.class);

	@Test
	public void c01a_default() throws Exception {
		assertEquals("{x:'foo,bar'}", cr.getC01a("foo","bar"));
	}
	@Test
	public void c01b_default_raw() throws Exception {
		assertEquals("x=foo%2Cbar", cr.getC01b("foo","bar"));
	}
	@Test
	public void c02a_csv() throws Exception {
		assertEquals("{x:'foo,bar'}", cr.getC02a("foo","bar"));
	}
	@Test
	public void c02b_csv_raw() throws Exception {
		assertEquals("x=foo%2Cbar", cr.getC02b("foo","bar"));
	}
	@Test
	public void c03a_ssv() throws Exception {
		assertEquals("{x:'foo bar'}", cr.getC03a("foo","bar"));
	}
	@Test
	public void c03b_ssv_raw() throws Exception {
		assertEquals("x=foo+bar", cr.getC03b("foo","bar"));
	}
	@Test
	public void c04a_tsv() throws Exception {
		assertEquals("{x:'foo\\tbar'}", cr.getC04a("foo","bar"));
	}
	@Test
	public void c04b_tsv_raw() throws Exception {
		assertEquals("x=foo%09bar", cr.getC04b("foo","bar"));
	}
	@Test
	public void c05a_pipes() throws Exception {
		assertEquals("{x:'foo|bar'}", cr.getC05a("foo","bar"));
	}
	@Test
	public void c05b_pipes_raw() throws Exception {
		assertEquals("x=foo%7Cbar", cr.getC05b("foo","bar"));
	}
	@Test
	public void c06a_multi() throws Exception {
		// Not supported, but should be treated as csv.
		assertEquals("{x:'foo,bar'}", cr.getC06a("foo","bar"));
	}
	@Test
	public void c06b_multi_raw() throws Exception {
		// Not supported, but should be treated as csv.
		assertEquals("x=foo%2Cbar", cr.getC06b("foo","bar"));
	}
	@Test
	public void c07a_uon() throws Exception {
		assertEquals("{x:'@(foo,bar)'}", cr.getC07a("foo","bar"));
	}
	@Test
	public void c07b_uon_raw() throws Exception {
		assertEquals("x=%40%28foo%2Cbar%29", cr.getC07b("foo","bar"));
	}
}
