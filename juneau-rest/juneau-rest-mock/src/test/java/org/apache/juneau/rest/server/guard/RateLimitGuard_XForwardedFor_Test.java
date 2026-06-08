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

/**
 * Tests for the {@code xForwardedForAware} flag.
 *
 * <p>
 * <b>Trusted-proxy assumption.</b>  These tests deliberately exercise the spoofable path to confirm the guard
 * keys on the first {@code X-Forwarded-For} hop when opted in.  Production deployments must only enable the flag
 * behind a reverse proxy that strips and rewrites the header.
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RateLimitGuard_XForwardedFor_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.xForwardedForAware(true)
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/a")
		public String a() { return "ok"; }
	}

	@Test void a01_keyResolvesFromFirstXForwardedForHop() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").header("X-Forwarded-For", "203.0.113.10, 10.0.0.1").run().assertStatus(200);
		c.get("/a").header("X-Forwarded-For", "203.0.113.10, 10.0.0.2").run().assertStatus(429);
		c.get("/a").header("X-Forwarded-For", "203.0.113.11").run().assertStatus(200);
	}

	@Test void a02_xForwardedForAbsentFallsBackToDefaultKey() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").remoteAddr("198.51.100.1").run().assertStatus(200);
		c.get("/a").remoteAddr("198.51.100.1").run().assertStatus(429);
		c.get("/a").remoteAddr("198.51.100.2").run().assertStatus(200);
	}

	@Test void a03_xForwardedForBlankFallsBackToDefaultKey() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").header("X-Forwarded-For", "   ").remoteAddr("198.51.100.5").run().assertStatus(200);
		c.get("/a").header("X-Forwarded-For", "   ").remoteAddr("198.51.100.5").run().assertStatus(429);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Default behavior: X-Forwarded-For ignored when xForwardedForAware not enabled.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/b")
		public String b() { return "ok"; }
	}

	@Test void b01_xForwardedForIgnoredWhenAwarenessOff() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/b").remoteAddr("198.51.100.20").header("X-Forwarded-For", "203.0.113.99").run().assertStatus(200);
		c.get("/b").remoteAddr("198.51.100.20").header("X-Forwarded-For", "203.0.113.99").run().assertStatus(429);
		c.get("/b").remoteAddr("198.51.100.21").header("X-Forwarded-For", "203.0.113.99").run().assertStatus(200);
	}
}
