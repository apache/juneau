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

class RateLimitGuard_AdvisoryHeaders_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(5)
					.keyBy(req -> "static-headers-success")
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/a")
		public String a() { return "ok"; }
	}

	@Test void a01_advisoryHeadersPopulatedOnSuccess() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").run()
			.assertStatus(200)
			.assertHeader("X-RateLimit-Limit").is("5")
			.assertHeader("X-RateLimit-Remaining").isExists()
			.assertHeader("X-RateLimit-Reset").isExists();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Separate resource (and therefore separate RestContext + storage) for the rejection path
	// so the per-class shared MockRestClient context cache doesn't leak state between tests.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(5)
					.keyBy(req -> "static-headers-rejection")
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/b")
		public String b() { return "ok"; }
	}

	@Test void b01_advisoryHeadersOnRejection() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		for (var i = 0; i < 5; i++)
			c.get("/b").run().assertStatus(200);
		c.get("/b").run()
			.assertStatus(429)
			.assertHeader("X-RateLimit-Limit").is("5")
			.assertHeader("X-RateLimit-Remaining").is("0")
			.assertHeader("X-RateLimit-Reset").isExists()
			.assertHeader("Retry-After").asInteger().isExists();
	}
}
