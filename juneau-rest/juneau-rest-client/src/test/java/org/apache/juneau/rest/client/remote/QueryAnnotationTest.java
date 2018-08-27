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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
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
		@RemoteMethod(path="a") String getA04b(@Query(name="x",format="uon") Bean[] b);
		@RemoteMethod(path="a") String getA05a(@Query("x") List<Bean> b);
		@RemoteMethod(path="a") String getA05b(@Query(name="x",format="uon") List<Bean> b);
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
}
