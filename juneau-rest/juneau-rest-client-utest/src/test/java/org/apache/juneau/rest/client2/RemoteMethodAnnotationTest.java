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

import java.io.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the @RemoteResource annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteMethodAnnotationTest {

	//=================================================================================================================
	// Inferred methods/paths
	//=================================================================================================================

	@Rest
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

	@Remote
	public static interface A01 {
		public String doGet();
		public String doGET();
		public String doFoo();
		public String getA01();
		public String postA02();
	}

	@Remote
	public static interface A02 {
		public Future<String> doGet();
		public Future<String> doGET();
		public Future<String> doFoo();
		public Future<String> getA01();
		public Future<String> postA02();
	}

	@Remote
	public static interface A03 {
		public CompletableFuture<String> doGet();
		public CompletableFuture<String> doGET();
		public CompletableFuture<String> doFoo();
		public CompletableFuture<String> getA01();
		public CompletableFuture<String> postA02();
	}

	@Test
	public void a01_inferredMethodsAndPaths() throws Exception {
		A01 t = MockRemote.build(A01.class, A.class, null);
		assertEquals("foo", t.doGet());
		assertEquals("foo", t.doGET());
		assertEquals("qux", t.doFoo());
		assertEquals("bar", t.getA01());
		assertEquals("baz", t.postA02());
	}

	@Test
	public void a02_inferredMethodsAndPaths_futures() throws Exception {
		A02 t = MockRemote.build(A02.class, A.class, null);
		assertEquals("foo", t.doGet().get());
		assertEquals("foo", t.doGET().get());
		assertEquals("qux", t.doFoo().get());
		assertEquals("bar", t.getA01().get());
		assertEquals("baz", t.postA02().get());
	}

	@Test
	public void a03_inferredMethodsAndPaths_completableFutures() throws Exception {
		A03 t = MockRemote.build(A03.class, A.class, null);
		assertEquals("foo", t.doGet().get());
		assertEquals("foo", t.doGET().get());
		assertEquals("qux", t.doFoo().get());
		assertEquals("bar", t.getA01().get());
		assertEquals("baz", t.postA02().get());
	}

	//=================================================================================================================
	// Return types
	//=================================================================================================================

	@Rest
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

	@Remote
	public static interface B01 {
		public void b01();
		public String b02();
		public HttpResponse b02a();
		public Reader b02b();
		public InputStream b02c();
	}

	@Remote
	public static interface B02 {
		public Future<Void> b01();
		public Future<String> b02();
		public Future<HttpResponse> b02a();
		public Future<Reader> b02b();
		public Future<InputStream> b02c();
	}

	@Remote
	public static interface B03 {
		public CompletableFuture<Void> b01();
		public CompletableFuture<String> b02();
		public CompletableFuture<HttpResponse> b02a();
		public CompletableFuture<Reader> b02b();
		public CompletableFuture<InputStream> b02c();
	}

	@Test
	public void b01_returnTypes() throws Exception {
		B01 t = MockRemote.build(B01.class, B.class, null);
		t.b01();
		assertEquals("foo", t.b02());
		assertEquals("bar", IOUtils.read(t.b02a().getEntity().getContent()));
		assertEquals("baz", IOUtils.read(t.b02b()));
		assertEquals("qux", IOUtils.read(t.b02c()));
	}

	@Test
	public void b02_returnTypes_futures() throws Exception {
		B02 t = MockRemote.build(B02.class, B.class, null);
		t.b01().get();
		assertEquals("foo", t.b02().get());
		assertEquals("bar", IOUtils.read(t.b02a().get().getEntity().getContent()));
		assertEquals("baz", IOUtils.read(t.b02b().get()));
		assertEquals("qux", IOUtils.read(t.b02c().get()));
	}

	@Test
	public void b03_returnTypes_completableFutures() throws Exception {
		B03 t = MockRemote.build(B03.class, B.class, null);
		t.b01().get();
		assertEquals("foo", t.b02().get());
		assertEquals("bar", IOUtils.read(t.b02a().get().getEntity().getContent()));
		assertEquals("baz", IOUtils.read(t.b02b().get()));
		assertEquals("qux", IOUtils.read(t.b02c().get()));
	}

	//=================================================================================================================
	// Return types, JSON
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class, parsers=JsonParser.class)
	public static class C {

		@RestMethod(name="POST")
		public String c01(@Body String body) {
			return body;
		}
	}

	@Remote
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

	@Remote
	public static interface C02 {

		@RemoteMethod(method="POST",path="c01")
		public Future<String> c01a(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public Future<HttpResponse> c01b(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public Future<Reader> c01c(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public Future<InputStream> c01d(@Body String foo);
	}

	@Remote
	public static interface C03 {

		@RemoteMethod(method="POST",path="c01")
		public CompletableFuture<String> c01a(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public CompletableFuture<HttpResponse> c01b(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public CompletableFuture<Reader> c01c(@Body String foo);

		@RemoteMethod(method="POST",path="c01")
		public CompletableFuture<InputStream> c01d(@Body String foo);
	}

	@Test
	public void c01_returnTypes_json() throws Exception {
		C01 t = MockRemote.build(C01.class, C.class, Json.DEFAULT);
		assertEquals("foo", t.c01a("foo"));
		assertEquals("'foo'", IOUtils.read(t.c01b("foo").getEntity().getContent()));
		assertEquals("'foo'", IOUtils.read(t.c01c("foo")));
		assertEquals("'foo'", IOUtils.read(t.c01d("foo")));
	}

	@Test
	public void c02_returnTypes_json_futures() throws Exception {
		C02 t = MockRemote.build(C02.class, C.class, Json.DEFAULT);
		assertEquals("foo", t.c01a("foo").get());
		assertEquals("'foo'", IOUtils.read(t.c01b("foo").get().getEntity().getContent()));
		assertEquals("'foo'", IOUtils.read(t.c01c("foo").get()));
		assertEquals("'foo'", IOUtils.read(t.c01d("foo").get()));
	}

	@Test
	public void c03_returnTypes_json_completableFutures() throws Exception {
		C03 t = MockRemote.build(C03.class, C.class, Json.DEFAULT);
		assertEquals("foo", t.c01a("foo").get());
		assertEquals("'foo'", IOUtils.read(t.c01b("foo").get().getEntity().getContent()));
		assertEquals("'foo'", IOUtils.read(t.c01c("foo").get()));
		assertEquals("'foo'", IOUtils.read(t.c01d("foo").get()));
	}

	//=================================================================================================================
	// Return types, part serialization
	//=================================================================================================================

	@Rest(serializers=OpenApiSerializer.class, parsers=OpenApiParser.class, defaultAccept="text/openapi")
	public static class D {

		@RestMethod(name="POST")
		@Response
		public String d01(@Body String body) {
			return body;
		}
	}

	@Remote
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

	@Remote
	public static interface D02 {

		@RemoteMethod(method="POST",path="d01")
		public Future<String> d01a(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public Future<HttpResponse> d01b(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public Future<Reader> d01c(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public Future<InputStream> d01d(@Body String foo);
	}

	@Remote
	public static interface D03 {

		@RemoteMethod(method="POST",path="d01")
		public CompletableFuture<String> d01a(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public CompletableFuture<HttpResponse> d01b(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public CompletableFuture<Reader> d01c(@Body String foo);

		@RemoteMethod(method="POST",path="d01")
		public CompletableFuture<InputStream> d01d(@Body String foo);
	}

	@Test
	public void d01_returnTypes_partSerialization() throws Exception {
		D01 t = MockRemote.build(D01.class, D.class, OpenApi.DEFAULT);
		assertEquals("foo", t.d01a("foo"));
		assertEquals("foo", IOUtils.read(t.d01b("foo").getEntity().getContent()));
		assertEquals("foo", IOUtils.read(t.d01c("foo")));
		assertEquals("foo", IOUtils.read(t.d01d("foo")));
	}

	@Test
	public void d02_returnTypes_partSerialization_futures() throws Exception {
		D02 t = MockRemote.build(D02.class, D.class, OpenApi.DEFAULT);
		assertEquals("foo", t.d01a("foo").get());
		assertEquals("foo", IOUtils.read(t.d01b("foo").get().getEntity().getContent()));
		assertEquals("foo", IOUtils.read(t.d01c("foo").get()));
		assertEquals("foo", IOUtils.read(t.d01d("foo").get()));
	}

	@Test
	public void d03_returnTypes_partSerialization_completableutures() throws Exception {
		D03 t = MockRemote.build(D03.class, D.class, OpenApi.DEFAULT);
		assertEquals("foo", t.d01a("foo").get());
		assertEquals("foo", IOUtils.read(t.d01b("foo").get().getEntity().getContent()));
		assertEquals("foo", IOUtils.read(t.d01c("foo").get()));
		assertEquals("foo", IOUtils.read(t.d01d("foo").get()));
	}
}
