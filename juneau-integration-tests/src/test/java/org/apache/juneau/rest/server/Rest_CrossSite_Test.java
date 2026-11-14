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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Tests the default refusal of cross-site requests that use unsafe methods.
 */
class Rest_CrossSite_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		@RestPost(path="/x") public String post() { return "ok"; }
		@RestGet(path="/x") public String get() { return "ok"; }
	}

	@Rest(allowCrossSite="true")
	public static class B extends BasicRestServlet {
		@RestPost(path="/x") public String post() { return "ok"; }
	}

	@Rest(allowedOrigins="https://app.example,http://pub.example")
	public static class C extends BasicRestServlet {
		@RestPost(path="/x") public String post() { return "ok"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(C.class);
	private static final MockRestClient a = MockRestClient.buildLax(A.class);
	private static final MockRestClient b = MockRestClient.buildLax(B.class);

	@Test void a01_noFetchHeaders_passes() throws Exception {
		a.post("/x", "").run().assertStatus(200);
	}

	@Test void a02_secFetchSiteCrossSite_refused() throws Exception {
		a.post("/x", "").header("Sec-Fetch-Site", "cross-site").run().assertStatus(403);
	}

	@Test void a03_secFetchSiteSameOrigin_passes() throws Exception {
		a.post("/x", "").header("Sec-Fetch-Site", "same-origin").run().assertStatus(200);
	}

	@Test void a04_originMismatch_refused() throws Exception {
		a.post("/x", "").header("Host", "example.com").header("Origin", "https://other.example").run().assertStatus(403);
	}

	@Test void a05_originMatchesHost_passes() throws Exception {
		a.post("/x", "").header("Host", "example.com").header("Origin", "https://example.com").run().assertStatus(200);
		a.post("/x", "").header("Host", "example.com:8080").header("Origin", "http://example.com:8080").run().assertStatus(200);
	}

	@Test void a06_safeMethod_notChecked() throws Exception {
		a.get("/x").header("Sec-Fetch-Site", "cross-site").run().assertStatus(200);
	}

	@Test void a07_optOut_allowsCrossSite() throws Exception {
		b.post("/x", "").header("Sec-Fetch-Site", "cross-site").run().assertStatus(200);
	}

	@Test void a08_listedOrigin_passes() throws Exception {
		c.post("/x", "").header("Host", "internal").header("Origin", "https://app.example").run().assertStatus(200);
	}

	@Test void a09_unlistedOrigin_refused() throws Exception {
		c.post("/x", "").header("Host", "internal").header("Origin", "https://evil.example").run().assertStatus(403);
	}

	@Test void a10_unlistedForwardedHost_refused() throws Exception {
		c.post("/x", "").header("Host", "internal").header("X-Forwarded-Host", "evil.example").header("Origin", "https://evil.example").run().assertStatus(403);
		a.post("/x", "").header("Host", "internal").header("X-Forwarded-Host", "evil.example").header("Origin", "https://evil.example").run().assertStatus(403);
	}

	@Test void a11_listedForwardedHost_passes() throws Exception {
		c.post("/x", "").header("Host", "internal").header("X-Forwarded-Host", "pub.example").header("Origin", "https://pub.example").run().assertStatus(200);
	}
}
