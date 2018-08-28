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
import static org.junit.Assert.assertEquals;

import java.util.*;

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
}
