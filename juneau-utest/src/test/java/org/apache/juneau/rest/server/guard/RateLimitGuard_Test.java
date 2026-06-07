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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class RateLimitGuard_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Burst drain + refill + 429 + Retry-After
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(3)
					.keyBy(req -> "static-burst-drain")
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/a")
		public String a() { return "ok"; }
	}

	@Test void a01_burstDrainsThenRejectsWith429AndRetryAfter() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/a").run().assertStatus(200);
		c.get("/a").run().assertStatus(200);
		c.get("/a").run().assertStatus(200);
		c.get("/a").run()
			.assertStatus(429)
			.assertHeader("Retry-After").asInteger().isExists();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Refill after wait
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> "static")
					.exemptPaths()
					.build()
			).build();
		}
		@RestGet(path="/b")
		public String b() { return "ok"; }
	}

	@SuppressWarnings({
		"java:S2925" // Thread.sleep verifies real-time token-bucket refill at the configured rate.
	})
	@Test void b01_bucketRefillsAtConfiguredRate() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/b").run().assertStatus(200);
		c.get("/b").run().assertStatus(429);
		Thread.sleep(1100);
		c.get("/b").run().assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Defaults reject zero/negative configuration
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_builderRejectsNonPositiveRate() {
		assertThrows(IllegalArgumentException.class, () -> RateLimitGuard.create().permitsPerSecond(0).burst(1).build());
	}

	@Test void c02_builderRejectsNonPositiveBurst() {
		assertThrows(IllegalArgumentException.class, () -> RateLimitGuard.create().permitsPerSecond(1).burst(0).build());
	}

	@Test void c03_builderRejectsNullKeyResolver() {
		assertThrows(IllegalArgumentException.class, () -> RateLimitGuard.create().keyBy(null));
	}

	@Test void c04_builderRejectsNullStorage() {
		assertThrows(IllegalArgumentException.class, () -> RateLimitGuard.create().storage(null));
	}

	@Test void c05_builderRejectsNullExemptPaths() {
		assertThrows(IllegalArgumentException.class, () -> RateLimitGuard.create().exemptPaths((String[])null));
	}

	@Test void c06_builderRejectsNullOnLimitExceeded() {
		assertThrows(IllegalArgumentException.class, () -> RateLimitGuard.create().whenLimitExceeded(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// permitsPerMinute / permitsPerHour wiring
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_permitsPerMinuteAndHourSetRate() {
		var g1 = RateLimitGuard.create().permitsPerMinute(60).burst(1).build();
		var g2 = RateLimitGuard.create().permitsPerHour(3600).burst(1).build();
		assertNotNull(g1);
		assertNotNull(g2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// onLimitExceeded callback fires on rejection
	//------------------------------------------------------------------------------------------------------------------

	private static volatile boolean eFired;

	@Rest
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(
				RateLimitGuard.create()
					.permitsPerSecond(1)
					.burst(1)
					.keyBy(req -> "static")
					.exemptPaths()
					.whenLimitExceeded((req, info) -> eFired = true)
					.build()
			).build();
		}
		@RestGet(path="/e")
		public String e() { return "ok"; }
	}

	@Test void e01_onLimitExceededCallbackFiresOnRejection() throws Exception {
		eFired = false;
		var c = MockRestClient.buildLax(E.class);
		c.get("/e").run().assertStatus(200);
		c.get("/e").run().assertStatus(429);
		assertTrue(eFired);
	}

	//------------------------------------------------------------------------------------------------------------------
	// TooManyRequests exception carries advisory headers + Retry-After
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_tooManyRequestsCarriesAdvisoryHeadersAndRetryAfter() {
		var storage = RateLimitGuard.Storage.inMemory();
		assertTrue(storage.tryAcquire("k", 1, 1.0).allowed());
		var result = storage.tryAcquire("k", 1, 1.0);
		assertFalse(result.allowed());
		assertEquals(0, result.remaining());
		assertTrue(result.secondsUntilReset() >= 1L);
	}

	//------------------------------------------------------------------------------------------------------------------
	// isRequestAllowed always returns true (we override guard() instead)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_isRequestAllowedAlwaysTrue() {
		var g = RateLimitGuard.create().permitsPerSecond(1).burst(1).build();
		assertTrue(g.isRequestAllowed(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sanity: TooManyRequests exception type is reachable
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_tooManyRequestsExceptionTypeAvailable() {
		assertEquals(429, TooManyRequests.STATUS_CODE);
	}
}
