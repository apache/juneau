/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.assertions;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link FluentRequestLineAssertion}, {@link FluentRequestHeaderAssertion},
 * {@link FluentRequestQueryParamAssertion}, {@link FluentRequestFormParamAssertion},
 * {@link FluentRequestContentAssertion}, and {@link FluentProtocolVersionAssertion}.
 */
class RestAssertions_Test extends TestBase {

	// =================================================================================================================
	// A - FluentRequestLineAssertion
	// =================================================================================================================

	@Rest
	public static class A {
		@RestGet("/")
		public String get(RestRequest req) {
			req.assertRequestLine().asMethod().is("GET");
			req.assertRequestLine().asUri().isContains("/");
			req.assertRequestLine().asProtocolVersion().asMajor().is(1);
			req.assertRequestLine().asProtocolVersion().asMinor().is(1);
			req.assertRequestLine().asProtocolVersion().asProtocol().is("HTTP");
			return "ok";
		}
	}

	@Test
	void a01_requestLine_method() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void a02_requestLine_protocolVersion() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/").run().assertStatus(200).assertContent().is("ok");
	}

	// =================================================================================================================
	// B - FluentRequestHeaderAssertion
	// =================================================================================================================

	@Rest
	public static class B {
		@RestGet("/")
		public String get(RestRequest req) {
			req.assertHeader("X-Foo").is("bar");
			req.assertHeader("X-Foo").isNot("baz");
			req.assertHeader("X-Foo").isContains("ba");
			req.assertHeader("X-Foo").isNotEmpty();
			return "ok";
		}

		@RestGet("/integer")
		public String integer(RestRequest req) {
			req.assertHeader("X-Num").asInteger().is(123);
			return "ok";
		}

		@RestGet("/long")
		public String longVal(RestRequest req) {
			req.assertHeader("X-Long").asLong().is(9999999999L);
			return "ok";
		}

		@RestGet("/boolean")
		public String booleanVal(RestRequest req) {
			req.assertHeader("X-Bool").asBoolean().isTrue();
			return "ok";
		}

		@RestGet("/as-type")
		public String asType(RestRequest req) {
			req.assertHeader("X-Num").as(Integer.class).is(42);
			return "ok";
		}
	}

	@Test
	void b01_header_stringAssertions() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/").header("X-Foo", "bar").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void b02_header_asInteger() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/integer").header("X-Num", "123").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void b03_header_asLong() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/long").header("X-Long", "9999999999").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void b04_header_asBoolean() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/boolean").header("X-Bool", "true").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void b05_header_asType() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/as-type").header("X-Num", "42").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void b06_header_failingAssertion_throwsError() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/").header("X-Foo", "wrong").run().assertStatus(500);
	}

	// =================================================================================================================
	// C - FluentRequestQueryParamAssertion
	// =================================================================================================================

	@Rest
	public static class C {
		@RestGet("/")
		public String get(RestRequest req) {
			req.assertQueryParam("foo").is("bar");
			req.assertQueryParam("foo").isNot("baz");
			req.assertQueryParam("foo").isContains("ba");
			req.assertQueryParam("foo").isNotEmpty();
			return "ok";
		}

		@RestGet("/integer")
		public String integer(RestRequest req) {
			req.assertQueryParam("num").asInteger().is(42);
			return "ok";
		}

		@RestGet("/long")
		public String longVal(RestRequest req) {
			req.assertQueryParam("num").asLong().is(100L);
			return "ok";
		}

		@RestGet("/boolean")
		public String booleanVal(RestRequest req) {
			req.assertQueryParam("flag").asBoolean().isTrue();
			return "ok";
		}

		@RestGet("/as-type")
		public String asType(RestRequest req) {
			req.assertQueryParam("num").as(Integer.class).is(7);
			return "ok";
		}
	}

	@Test
	void c01_queryParam_stringAssertions() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/").queryData("foo", "bar").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void c02_queryParam_asInteger() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/integer").queryData("num", "42").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void c03_queryParam_asLong() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/long").queryData("num", "100").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void c04_queryParam_asBoolean() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/boolean").queryData("flag", "true").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void c05_queryParam_asType() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/as-type").queryData("num", "7").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void c06_queryParam_failingAssertion_throwsError() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/").queryData("foo", "wrong").run().assertStatus(500);
	}

	// =================================================================================================================
	// D - FluentRequestFormParamAssertion
	// =================================================================================================================

	@Rest
	public static class D {
		@RestPost("/")
		public String post(RestRequest req) {
			req.assertFormParam("foo").is("bar");
			req.assertFormParam("foo").isNot("baz");
			req.assertFormParam("foo").isContains("ba");
			req.assertFormParam("foo").isNotEmpty();
			return "ok";
		}

		@RestPost("/integer")
		public String integer(RestRequest req) {
			req.assertFormParam("num").asInteger().is(42);
			return "ok";
		}

		@RestPost("/long")
		public String longVal(RestRequest req) {
			req.assertFormParam("num").asLong().is(100L);
			return "ok";
		}

		@RestPost("/boolean")
		public String booleanVal(RestRequest req) {
			req.assertFormParam("flag").asBoolean().isTrue();
			return "ok";
		}

		@RestPost("/as-type")
		public String asType(RestRequest req) {
			req.assertFormParam("num").as(Integer.class).is(7);
			return "ok";
		}
	}

	@Test
	void d01_formParam_stringAssertions() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/").contentType("application/x-www-form-urlencoded").formData("foo", "bar").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void d02_formParam_asInteger() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/integer").contentType("application/x-www-form-urlencoded").formData("num", "42").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void d03_formParam_asLong() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/long").contentType("application/x-www-form-urlencoded").formData("num", "100").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void d04_formParam_asBoolean() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/boolean").contentType("application/x-www-form-urlencoded").formData("flag", "true").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void d05_formParam_asType() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/as-type").contentType("application/x-www-form-urlencoded").formData("num", "7").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void d06_formParam_failingAssertion_throwsError() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.post("/").contentType("application/x-www-form-urlencoded").formData("foo", "wrong").run().assertStatus(500);
	}

	// =================================================================================================================
	// E - FluentRequestContentAssertion
	// =================================================================================================================

	@Rest
	public static class E {
		@RestPost("/")
		public String post(RestRequest req) {
			req.assertContent().is("hello");
			req.assertContent().isNotEmpty();
			req.assertContent().isContains("ell");
			req.assertContent().isNotContains("xyz");
			return "ok";
		}

		@RestPost("/bytes")
		public String bytes(RestRequest req) {
			req.assertContent().asBytes().isNotNull();
			return "ok";
		}

		@RestPost("/empty")
		public String empty(RestRequest req) {
			req.assertContent().isEmpty();
			return "ok";
		}
	}

	@Test
	void e01_content_stringAssertions() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.post("/").contentType("text/plain").content("hello").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void e02_content_asBytes() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.post("/bytes").contentType("text/plain").content("data").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void e03_content_isEmpty() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.post("/empty").contentType("text/plain").content("").run().assertStatus(200).assertContent().is("ok");
	}

	@Test
	void e04_content_failingAssertion_throwsError() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.post("/").contentType("text/plain").content("wrong").run().assertStatus(500);
	}

	// =================================================================================================================
	// F - FluentProtocolVersionAssertion (standalone)
	// =================================================================================================================

	@Rest
	public static class F {
		@RestGet("/")
		public String get(RestRequest req) {
			req.assertRequestLine().asProtocolVersion().asProtocol().is("HTTP");
			req.assertRequestLine().asProtocolVersion().asMajor().is(1);
			req.assertRequestLine().asProtocolVersion().asMinor().is(1);
			req.assertRequestLine().asProtocolVersion().isNotNull();
			return "ok";
		}
	}

	@Test
	void f01_protocolVersion_assertions() throws Exception {
		var f = MockRestClient.buildLax(F.class);
		f.get("/").run().assertStatus(200).assertContent().is("ok");
	}
}
