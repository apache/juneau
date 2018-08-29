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

import org.apache.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @RemoteResource annotation.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteMethodAnnotationTest {

	//=================================================================================================================
	// Inferred methods/paths
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod
		public String get() {
			return "foo";
		}

		@RestMethod
		public String a01() {
			return "bar";
		}

		@RestMethod
		public String postA02() {
			return "baz";
		}

		@RestMethod(path="/doFoo")
		public String doFoo() {
			return "qux";
		}
	}
	private static MockRest a = MockRest.create(A.class);
	private static RestClient ra = RestClient.create().mockHttpConnection(a).build();

	@RemoteResource
	public static interface A01 {
		public String doGet();
		public String doGET();
		public String doFoo();
		public String getA01();
		public String postA02();
	}

	@Test
	public void a01_inferredMethodsAndPaths() throws Exception {
		A01 t = ra.getRemoteResource(A01.class);
		assertEquals("foo", t.doGet());
		assertEquals("foo", t.doGET());
		assertEquals("qux", t.doFoo());
		assertEquals("bar", t.getA01());
		assertEquals("baz", t.postA02());
	}

	//=================================================================================================================
	// Return types
	//=================================================================================================================

	@RestResource
	public static class B {

		@RestMethod
		public void b01() {
		}

		@RestMethod
		public String b02() {
			return "foo";
		}
		@RestMethod
		public String b02a() {
			return "bar";
		}
		@RestMethod
		public String b02b() {
			return "baz";
		}
		@RestMethod
		public String b02c() {
			return "qux";
		}
	}
	private static MockRest b = MockRest.create(B.class);
	private static RestClient rb = RestClient.create().mockHttpConnection(b).build();

	@RemoteResource
	public static interface B01 {
		public void b01();

		public String b02();
		public HttpResponse b02a();
		public Reader b02b();
		public InputStream b02c();
	}

	@Test
	public void b01_returnTypes() throws Exception {
		B01 t = rb.getRemoteResource(B01.class);
		t.b01();
		assertEquals("foo", t.b02());
		assertEquals("bar", IOUtils.read(t.b02a().getEntity().getContent()));
		assertEquals("baz", IOUtils.read(t.b02b()));
		assertEquals("qux", IOUtils.read(t.b02c()));
	}

	//=================================================================================================================
	// Return types, JSON
	//=================================================================================================================

	@RestResource(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class)
	public static class C {

		@RestMethod(name="POST")
		public String c01(@Body String body) {
			return body;
		}
	}
	private static MockRest c = MockRest.create(C.class);
	private static RestClient rc = RestClient.create().mockHttpConnection(c).simpleJson().build();

	@RemoteResource
	public static interface C01 {

		@RemoteMethod(method="POST",path="c01")
		public String c01a(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public HttpResponse c01b(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public Reader c01c(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public InputStream c01d(@Body String foo);
	}

	@Test
	public void c01_returnTypes_json() throws Exception {
		C01 t = rc.getRemoteResource(C01.class);
		assertEquals("foo", t.c01a("foo"));
		assertEquals("'foo'", IOUtils.read(t.c01b("foo").getEntity().getContent()));
		assertEquals("'foo'", IOUtils.read(t.c01c("foo")));
		assertEquals("'foo'", IOUtils.read(t.c01d("foo")));
	}

	//=================================================================================================================
	// Return types, part serialization
	//=================================================================================================================

	@RestResource(serializers=OpenApiSerializer.class, parsers=OpenApiParser.class, defaultAccept="text/openapi")
	public static class D {

		@RestMethod(name="POST")
		@Response
		public String d01(@Body String body) {
			return body;
		}
	}
	private static MockRest d = MockRest.create(D.class, true);
	private static RestClient rd = RestClient.create().debug().mockHttpConnection(d).build();

	@RemoteResource
	public static interface D01 {

		@RemoteMethod(method="POST",path="d01")
		public String d01a(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public HttpResponse d01b(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public Reader d01c(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public InputStream d01d(@Body String foo);
	}

	@Test
	public void d01_returnTypes_partSerialization() throws Exception {
		D01 t = rd.getRemoteResource(D01.class);
		assertEquals("foo", t.d01a("foo"));
		assertEquals("foo", IOUtils.read(t.d01b("foo").getEntity().getContent()));
		assertEquals("foo", IOUtils.read(t.d01c("foo")));
		assertEquals("foo", IOUtils.read(t.d01d("foo")));
	}
}
