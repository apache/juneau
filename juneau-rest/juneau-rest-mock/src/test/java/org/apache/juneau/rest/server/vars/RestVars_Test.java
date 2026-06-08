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
package org.apache.juneau.rest.server.vars;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests for REST SVL variable resolvers: {@link RequestHeaderVar}, {@link RequestQueryVar},
 * {@link RequestAttributeVar}, {@link RequestVar}, and {@link UrlVar}.
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RestVars_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - RequestHeaderVar ($RH{key})
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$RH{X-Test}");
		}
	}

	@Test void a01_requestHeaderVar_present() throws Exception {
		var client = MockRestClient.build(A.class);
		client.get("/")
			.header("X-Test", "headerValue")
			.run()
			.assertStatus(200)
			.assertContent("headerValue");
	}

	@Test void a02_requestHeaderVar_missing() throws Exception {
		var client = MockRestClient.build(A.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - RequestQueryVar ($RQ{key})
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$RQ{foo}");
		}
	}

	@Test void b01_requestQueryVar_present() throws Exception {
		var client = MockRestClient.build(B.class);
		client.get("/?foo=bar")
			.run()
			.assertStatus(200)
			.assertContent("bar");
	}

	@Test void b02_requestQueryVar_missing() throws Exception {
		var client = MockRestClient.build(B.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - RequestAttributeVar ($RA{key})
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet
		public String get(RestRequest req) {
			req.setAttribute("myAttr", "attrValue");
			return req.getVarResolverSession().resolve("$RA{myAttr}");
		}
	}

	@Test void c01_requestAttributeVar_present() throws Exception {
		var client = MockRestClient.build(C.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent("attrValue");
	}

	@Rest
	public static class C2 {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$RA{noSuchAttr}");
		}
	}

	@Test void c02_requestAttributeVar_missing() throws Exception {
		var client = MockRestClient.build(C2.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - RequestVar ($R{key}) - request properties
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestGet
		public String method(RestRequest req) {
			return req.getVarResolverSession().resolve("$R{method}");
		}
	}

	@Test void d01_requestVar_method() throws Exception {
		var client = MockRestClient.build(D.class);
		client.get("/method")
			.run()
			.assertStatus(200)
			.assertContent("GET");
	}

	@Rest
	public static class D2 {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$R{servletPath}");
		}
	}

	@Test void d02_requestVar_servletPath() throws Exception {
		var client = MockRestClient.build(D2.class);
		client.get("/")
			.run()
			.assertStatus(200);
		// servletPath is available; we just verify no error occurs
	}

	@Rest
	public static class D3 {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$R{requestURI}");
		}
	}

	@Test void d03_requestVar_requestURI() throws Exception {
		var client = MockRestClient.build(D3.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("/");
	}

	@Rest
	public static class D4 {
		@RestGet
		public String get(RestRequest req) {
			// Unknown key falls back to request attributes
			return req.getVarResolverSession().resolve("$R{unknownKey}");
		}
	}

	@Test void d04_requestVar_unknownKey_returnsEmpty() throws Exception {
		var client = MockRestClient.build(D4.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent("");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - UrlVar ($U{uri})
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$U{servlet:/foo}");
		}
	}

	@Test void e01_urlVar_servletRelative() throws Exception {
		var client = MockRestClient.build(E.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("/foo");
	}

	@Rest
	public static class E2 {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$U{http://example.com/bar}");
		}
	}

	@Test void e02_urlVar_absolute() throws Exception {
		var client = MockRestClient.build(E2.class);
		client.get("/")
			.run()
			.assertStatus(200)
			.assertContent("http://example.com/bar");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - RequestHeaderVar multipart ($RH{key1,key2}) - first non-null wins
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$RH{X-Missing,X-Fallback}");
		}
	}

	@Test void f01_requestHeaderVar_multipart_fallback() throws Exception {
		var client = MockRestClient.build(F.class);
		client.get("/")
			.header("X-Fallback", "fallbackValue")
			.run()
			.assertStatus(200)
			.assertContent("fallbackValue");
	}

	@Test void f02_requestHeaderVar_multipart_firstWins() throws Exception {
		var client = MockRestClient.build(F.class);
		client.get("/")
			.header("X-Missing", "firstValue")
			.header("X-Fallback", "fallbackValue")
			.run()
			.assertStatus(200)
			.assertContent("firstValue");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g - RequestQueryVar multipart ($RQ{key1,key2}) - first non-null wins
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@RestGet
		public String get(RestRequest req) {
			return req.getVarResolverSession().resolve("$RQ{a,b}");
		}
	}

	@Test void g01_requestQueryVar_multipart_fallback() throws Exception {
		var client = MockRestClient.build(G.class);
		client.get("/?b=bValue")
			.run()
			.assertStatus(200)
			.assertContent("bValue");
	}

	@Test void g02_requestQueryVar_multipart_firstWins() throws Exception {
		var client = MockRestClient.build(G.class);
		client.get("/?a=aValue&b=bValue")
			.run()
			.assertStatus(200)
			.assertContent("aValue");
	}
}
