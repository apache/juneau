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
import java.util.*;

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
		@RemoteMethod(path="a") String postA04b(@FormData(name="x",format="uon") Bean[] b);
		@RemoteMethod(path="a") String postA05a(@FormData("x") List<Bean> b);
		@RemoteMethod(path="a") String postA05b(@FormData(name="x",format="uon") List<Bean> b);
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
	// @FormData(_default)
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
}
