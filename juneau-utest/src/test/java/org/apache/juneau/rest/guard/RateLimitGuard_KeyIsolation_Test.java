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
package org.apache.juneau.rest.guard;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

class RateLimitGuard_KeyIsolation_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> req.getHeader("X-Tenant"))
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/a")
		public String a() { return "ok"; }
	}

	@Test void a01_differentKeysHaveIndependentBuckets() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").header("X-Tenant", "alpha").run().assertStatus(200);
		c.get("/a").header("X-Tenant", "alpha").run().assertStatus(429);
		c.get("/a").header("X-Tenant", "beta").run().assertStatus(200);
		c.get("/a").header("X-Tenant", "beta").run().assertStatus(429);
		c.get("/a").header("X-Tenant", "gamma").run().assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Storage SPI directly: same key shares state, different keys are isolated.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_storageIsolatesKeys() {
		var storage = RateLimitGuard.Storage.inMemory();
		assertTrue(storage.tryAcquire("k1", 1, 1.0).allowed());
		assertFalse(storage.tryAcquire("k1", 1, 1.0).allowed());
		assertTrue(storage.tryAcquire("k2", 1, 1.0).allowed());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Null key from resolver is normalized to empty string.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> null)
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/b")
		public String b() { return "ok"; }
	}

	@Test void b02_nullKeyNormalizedToEmptyStringSharedBucket() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/b").run().assertStatus(200);
		c.get("/b").run().assertStatus(429);
	}
}
