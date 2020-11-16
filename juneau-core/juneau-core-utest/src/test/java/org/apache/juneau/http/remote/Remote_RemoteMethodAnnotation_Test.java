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
package org.apache.juneau.http.remote;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_RemoteMethodAnnotation_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Inferred methods/paths
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestMethod
		public String get() {
			return "foo";
		}
		@RestMethod
		public String x1() {
			return "bar";
		}
		@RestMethod
		public String postX2() {
			return "baz";
		}
		@RestMethod(path="/doFoo")
		public String doFoo() {
			return "qux";
		}
	}

	@Remote
	public static interface A1 {
		String doGet();
		String doGET();
		String doFoo();
		String getX1();
		String postX2();
	}

	@Test
	public void a01_inferredMethodsAndPaths() throws Exception {
		A1 t = remote(A.class,A1.class);
		assertEquals("foo",t.doGet());
		assertEquals("foo",t.doGET());
		assertEquals("qux",t.doFoo());
		assertEquals("bar",t.getX1());
		assertEquals("baz",t.postX2());
	}

	@Remote
	public static interface A2 {
		Future<String> doGet();
		Future<String> doGET();
		Future<String> doFoo();
		Future<String> getX1();
		Future<String> postX2();
	}

	@Test
	public void a02_inferredMethodsAndPaths_futures() throws Exception {
		A2 t = remote(A.class,A2.class);
		assertEquals("foo",t.doGet().get());
		assertEquals("foo",t.doGET().get());
		assertEquals("qux",t.doFoo().get());
		assertEquals("bar",t.getX1().get());
		assertEquals("baz",t.postX2().get());
	}

	@Remote
	public static interface A3 {
		CompletableFuture<String> doGet();
		CompletableFuture<String> doGET();
		CompletableFuture<String> doFoo();
		CompletableFuture<String> getX1();
		CompletableFuture<String> postX2();
	}

	@Test
	public void a03_inferredMethodsAndPaths_completableFutures() throws Exception {
		A3 t = remote(A.class,A3.class);
		assertEquals("foo",t.doGet().get());
		assertEquals("foo",t.doGET().get());
		assertEquals("qux",t.doFoo().get());
		assertEquals("bar",t.getX1().get());
		assertEquals("baz",t.postX2().get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Return types
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestMethod(path="/*")
		public String get() {
			return "foo";
		}
	}

	@Remote
	public static interface B1 {
		void x1();
		String x2();
		HttpResponse x3();
		Reader x4();
		InputStream x5();
		Future<Void> x6();
		Future<String> x7();
		Future<HttpResponse> x8();
		Future<Reader> x9();
		Future<InputStream> x10();
		CompletableFuture<Void> x11();
		CompletableFuture<String> x12();
		CompletableFuture<HttpResponse> x13();
		CompletableFuture<Reader> x14();
		CompletableFuture<InputStream> x15();
	}

	@Test
	public void b01_returnTypes() throws Exception {
		B1 x = remote(B.class,B1.class);
		x.x1();
		assertEquals("foo",x.x2());
		assertEquals("foo",IOUtils.read(x.x3().getEntity().getContent()));
		assertEquals("foo",IOUtils.read(x.x4()));
		assertEquals("foo",IOUtils.read(x.x5()));
		x.x6().get();
		assertEquals("foo",x.x7().get());
		assertEquals("foo",IOUtils.read(x.x8().get().getEntity().getContent()));
		assertEquals("foo",IOUtils.read(x.x9().get()));
		assertEquals("foo",IOUtils.read(x.x10().get()));
		x.x11().get();
		assertEquals("foo",x.x12().get());
		assertEquals("foo",IOUtils.read(x.x13().get().getEntity().getContent()));
		assertEquals("foo",IOUtils.read(x.x14().get()));
		assertEquals("foo",IOUtils.read(x.x15().get()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Return types, JSON
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C implements BasicSimpleJsonRest {
		@RestMethod(path="/*")
		public String post(@Body String body) {
			return body;
		}
	}

	@Remote
	public static interface C1 {
		String postX1(@Body String foo);
		HttpResponse postX2(@Body String foo);
		Reader postX3(@Body String foo);
		InputStream postX4(@Body String foo);
		Future<String> postX5(@Body String foo);
		Future<HttpResponse> postX6(@Body String foo);
		Future<Reader> postX7(@Body String foo);
		Future<InputStream> postX8(@Body String foo);
		CompletableFuture<String> postX9(@Body String foo);
		CompletableFuture<HttpResponse> postX10(@Body String foo);
		CompletableFuture<Reader> postX11(@Body String foo);
		CompletableFuture<InputStream> postX12(@Body String foo);
	}

	@Test
	public void c01_returnTypes_json() throws Exception {
		C1 x = MockRestClient.buildJson(C.class).getRemote(C1.class);
		assertEquals("foo",x.postX1("foo"));
		assertEquals("'foo'",IOUtils.read(x.postX2("foo").getEntity().getContent()));
		assertEquals("'foo'",IOUtils.read(x.postX3("foo")));
		assertEquals("'foo'",IOUtils.read(x.postX4("foo")));
		assertEquals("foo",x.postX5("foo").get());
		assertEquals("'foo'",IOUtils.read(x.postX6("foo").get().getEntity().getContent()));
		assertEquals("'foo'",IOUtils.read(x.postX7("foo").get()));
		assertEquals("'foo'",IOUtils.read(x.postX8("foo").get()));
		assertEquals("foo",x.postX9("foo").get());
		assertEquals("'foo'",IOUtils.read(x.postX10("foo").get().getEntity().getContent()));
		assertEquals("'foo'",IOUtils.read(x.postX11("foo").get()));
		assertEquals("'foo'",IOUtils.read(x.postX12("foo").get()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Return types, part serialization
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D implements BasicOpenApiRest {
		@RestMethod(path="/*") @Response
		public String postX1(@Body String body) {
			return body;
		}
	}

	@Remote
	public static interface D1 {
		String postX1(@Body String foo);
		HttpResponse postX2(@Body String foo);
		Reader postX3(@Body String foo);
		InputStream postX4(@Body String foo);
		Future<String> postX5(@Body String foo);
		Future<HttpResponse> postX6(@Body String foo);
		Future<Reader> postX7(@Body String foo);
		Future<InputStream> postX8(@Body String foo);
		CompletableFuture<String> postX9(@Body String foo);
		CompletableFuture<HttpResponse> postX10(@Body String foo);
		CompletableFuture<Reader> postX11(@Body String foo);
		CompletableFuture<InputStream> postX12(@Body String foo);
	}

	@Test
	public void d01_returnTypes_partSerialization() throws Exception {
		D1 x = MockRestClient.create(D.class).openApi().build().getRemote(D1.class);
		assertEquals("foo",x.postX1("foo"));
		assertEquals("foo",IOUtils.read(x.postX2("foo").getEntity().getContent()));
		assertEquals("foo",IOUtils.read(x.postX3("foo")));
		assertEquals("foo",IOUtils.read(x.postX4("foo")));
		assertEquals("foo",x.postX5("foo").get());
		assertEquals("foo",IOUtils.read(x.postX6("foo").get().getEntity().getContent()));
		assertEquals("foo",IOUtils.read(x.postX7("foo").get()));
		assertEquals("foo",IOUtils.read(x.postX8("foo").get()));
		assertEquals("foo",x.postX9("foo").get());
		assertEquals("foo",IOUtils.read(x.postX10("foo").get().getEntity().getContent()));
		assertEquals("foo",IOUtils.read(x.postX11("foo").get()));
		assertEquals("foo",IOUtils.read(x.postX12("foo").get()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static <T> T remote(Class<?> rest, Class<T> t) {
		return MockRestClient.build(rest).getRemote(t);
	}
}
