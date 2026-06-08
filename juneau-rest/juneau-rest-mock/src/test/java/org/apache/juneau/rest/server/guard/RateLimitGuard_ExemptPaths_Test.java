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
package org.apache.juneau.rest.server.guard;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

// Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
@SuppressWarnings({
	"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class RateLimitGuard_ExemptPaths_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Default exempt paths (/healthz, /readyz, /livez) bypass throttling.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> "static-exempt")
					.build()
			).build();
		}
		@RestGet(path="/healthz")
		public String healthz() { return "ok"; }
		@RestGet(path="/readyz")
		public String readyz() { return "ok"; }
		@RestGet(path="/livez")
		public String livez() { return "ok"; }
		@RestGet(path="/a")
		public String a() { return "ok"; }
	}

	@Test void a01_defaultExemptPathsBypassThrottling() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		for (var i = 0; i < 5; i++) {
			c.get("/healthz").run().assertStatus(200);
			c.get("/readyz").run().assertStatus(200);
			c.get("/livez").run().assertStatus(200);
		}
		c.get("/a").run().assertStatus(200);
		c.get("/a").run().assertStatus(429);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Custom exempt paths replace the defaults.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> "static-custom-exempt")
					.exemptPaths("/special")
					.build()
			).build();
		}
		@RestGet(path="/special")
		public String special() { return "ok"; }
		@RestGet(path="/healthz")
		public String healthz() { return "ok"; }
	}

	@Test void b01_customExemptPathsReplaceDefaults() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		for (var i = 0; i < 3; i++)
			c.get("/special").run().assertStatus(200);
		c.get("/healthz").run().assertStatus(200);
		c.get("/healthz").run().assertStatus(429);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Empty exempt-paths list throttles even the probe paths.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> "static-no-exempt")
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/healthz")
		public String healthz() { return "ok"; }
	}

	@Test void c01_emptyExemptPathsThrottlesEveryPath() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/healthz").run().assertStatus(200);
		c.get("/healthz").run().assertStatus(429);
	}
}
