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
package org.apache.juneau.rest.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

class RequestIdFilter_Malformed_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create().build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
		}
		@RestGet(path="/a")
		public String a(RestRequest req) {
			return req.getAttribute(RestServerConstants.REQUEST_ID).asString().orElse("");
		}
	}

	@Test void a01_malformedIdWithSpaceIsRejectedAndReminted() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var res = c.get("/a").header("X-Request-Id", "abc xyz").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertNotEquals("abc xyz", echoed);
		res.assertContent().asString().isContains(echoed);
	}

	@Test void a02_oversizeIdIsRejectedAndReminted() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var oversize = "a".repeat(200);
		var res = c.get("/a").header("X-Request-Id", oversize).run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertNotEquals(oversize, echoed);
	}

	@Test void a03_emptyIdIsRejectedAndReminted() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().json().build();
		var res = c.get("/a").header("X-Request-Id", "").run().assertStatus(200);
		var echoed = res.getHeader("X-Request-Id").asString().orElseThrow();
		assertFalse(echoed.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Custom validator rejects everything → always mints.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.validator(s -> false)
			.idSupplier(() -> "always-minted")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
		}
		@RestGet(path="/b")
		public String b() { return "ok"; }
	}

	@Test void b01_customValidatorAlwaysRejectingAlwaysMints() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/b").header("X-Request-Id", "550e8400-e29b-41d4-a716-446655440000").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("always-minted");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Custom attribute key is honored and observed in re-entry (existing attribute is reused).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.attributeKey("customReqId")
			.idSupplier(() -> "minted-once")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			FILTER.apply(req, res);
			FILTER.apply(req, res);
		}
		@RestGet(path="/c")
		public String c() { return "ok"; }
	}

	@Test void c01_reentryHonorsExistingAttribute() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/c").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("minted-once");
	}

	//------------------------------------------------------------------------------------------------------------------
	// An empty-string attribute already on the request is treated as missing and reminted.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.idSupplier(() -> "fresh-after-empty")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			req.setAttribute(RestServerConstants.REQUEST_ID, "");
			FILTER.apply(req, res);
		}
		@RestGet(path="/d")
		public String d() { return "ok"; }
	}

	@Test void d01_emptyStringAttributeRemints() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		c.get("/d").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("fresh-after-empty");
	}

	//------------------------------------------------------------------------------------------------------------------
	// A non-String attribute already on the request is treated as missing and reminted.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		private static final RequestIdFilter FILTER = RequestIdFilter.create()
			.idSupplier(() -> "fresh-after-non-string")
			.build();
		@RestStartCall
		public void stamp(HttpServletRequest req, HttpServletResponse res) {
			req.setAttribute(RestServerConstants.REQUEST_ID, Integer.valueOf(42));
			FILTER.apply(req, res);
		}
		@RestGet(path="/e")
		public String e() { return "ok"; }
	}

	@Test void e01_nonStringAttributeRemints() throws Exception {
		var c = MockRestClient.buildLax(E.class);
		c.get("/e").run()
			.assertStatus(200)
			.assertHeader("X-Request-Id").is("fresh-after-non-string");
	}
}
