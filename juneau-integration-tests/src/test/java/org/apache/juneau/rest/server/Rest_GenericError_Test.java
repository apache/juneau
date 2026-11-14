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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.ops.*;
import org.junit.jupiter.api.*;

/**
 * Tests that unplanned exceptions are reported generically, that stats omit messages, and related defaults.
 */
class Rest_GenericError_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/boom") public String boom() { throw new IllegalStateException("secret-detail"); }
	}

	@Rest(renderResponseStackTraces="true")
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/boom") public String boom() { throw new IllegalStateException("secret-detail"); }
	}

	@Rest(mixins={StatsMixin.class})
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/boom") public String boom() { throw new IllegalStateException("secret-detail"); }
	}

	@Test void a01_unplannedExceptionIsGeneric500() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		var body = c.get("/boom").run().assertStatus(500).getContent().asString();
		assertFalse(body.contains("secret-detail"), body);
		assertFalse(body.contains("IllegalStateException"), body);
	}

	@Test void a02_stackTracesOptInShowsDetail() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		var body = c.get("/boom").run().assertStatus(500).getContent().asString();
		assertTrue(body.contains("secret-detail"), body);
	}

	@Test void b01_statsNotMountedByDefault() throws Exception {
		MockRestClient.buildLax(A.class).get("/stats").run().assertStatus(404);
	}

	@Test void b02_statsOmitMessagesAndStacks() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/boom").run().assertStatus(500);
		var body = c.get("/stats").accept("application/json").run().getContent().asString();
		assertFalse(body.contains("secret-detail"), body);
		assertFalse(body.contains("firstMessage"), body);
		assertFalse(body.contains("stackTrace"), body);
	}

	@Rest(maxInput="0")
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestPost(path="/in") public String in(@Content String b) { return "len=" + b.length(); }
	}

	@Test void c01_zeroMaxInputMeansDefault() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		// Zero behaves like the default ceiling, not like "unlimited".
		var big = "x".repeat(2_000_000);
		var d = MockRestClient.buildLax(E.class).post("/in", big).run().getStatusCode();
		assertNotEquals(200, d);
		c.post("/in", big).run().assertStatus(d);
		c.post("/in", "abc").run().assertStatus(200);
	}

	@Rest
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestPost(path="/in") public String in(@Content String b) { return "len=" + b.length(); }
	}
}
