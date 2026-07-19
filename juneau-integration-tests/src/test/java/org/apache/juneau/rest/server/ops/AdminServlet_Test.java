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
package org.apache.juneau.rest.server.ops;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.guard.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link AdminServlet} servlet flavor mounted directly as a top-level servlet.
 *
 * <p>
 * The servlet pins its ops at the bare endpoint names ({@code /threads}, {@code /heap},
 * {@code /cache/flush}, {@code /ratelimit}) and delegates to a shared {@link AdminMixin} worker, so
 * this mirrors {@link AdminMixin_AsMixin_Test} but exercises the standalone-servlet deployment.
 * Cases:
 * <ul>
 * 	<li>Default-deny: the inherited {@link org.apache.juneau.rest.server.server.guard.DenyAllGuard DenyAllGuard}
 * 		returns {@code 403 Forbidden} on every admin path until an allow-all
 * 		{@code @Bean RestGuardList} replaces it.
 * 	<li>With the guard replaced and a builder-configured delegate, {@code /threads} / {@code /heap}
 * 		serve JSON and {@code /cache/flush} runs the registered hooks.
 * 	<li>{@code /ratelimit} returns {@code 404} when no {@link RateLimitGuard} bean is registered.
 * </ul>
 *
 * @since 10.0.0
 */
class AdminServlet_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Default-deny posture (the inherited DenyAllGuard is still in effect).
	// -----------------------------------------------------------------------------------------

	private static final MockRestClient ca = MockRestClient.buildLax(AdminServlet.class);

	@Test void a01_threadsDeniedByDefault() throws Exception {
		ca.get("/threads").run().assertStatus(403);
	}

	@Test void a02_heapDeniedByDefault() throws Exception {
		ca.get("/heap").run().assertStatus(403);
	}

	@Test void a03_cacheFlushDeniedByDefault() throws Exception {
		ca.post("/cache/flush", "").run().assertStatus(403);
	}

	// -----------------------------------------------------------------------------------------
	// Allow-all RestGuardList override + builder-configured delegate.
	// -----------------------------------------------------------------------------------------

	static final AtomicInteger PRIMARY_INVOCATIONS = new AtomicInteger();
	static final Runnable FLUSH_PRIMARY = PRIMARY_INVOCATIONS::incrementAndGet;

	/** Servlet subclass: allow-all guard chain + a configured admin worker. */
	public static class B extends AdminServlet {
		private static final long serialVersionUID = 1L;
		public B() {
			super(AdminProvider.create()
				.cacheFlush("primary", FLUSH_PRIMARY)
				.threadNamePrefixExclude()  // disable filtering to make /threads deterministic
				.build());
		}
		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@BeforeEach void resetCounters() {
		PRIMARY_INVOCATIONS.set(0);
	}

	@Test void b01_threadsServesJsonList() throws Exception {
		var body = cb.get("/threads")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.read(body, List.class);
		Assertions.assertFalse(parsed.isEmpty(), "thread list should not be empty");
	}

	@Test void b02_heapServesJsonMap() throws Exception {
		var body = cb.get("/heap")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.read(body, Map.class);
		var heap = (Map<?,?>) parsed.get("heap");
		Assertions.assertNotNull(heap.get("used"));
	}

	@Test void b03_cacheFlushRunsHook() throws Exception {
		cb.post("/cache/flush", "")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"executed\"");
		Assertions.assertEquals(1, PRIMARY_INVOCATIONS.get(), "primary should run once");
	}

	@Test void b04_rateLimit404WhenNoGuardRegistered() throws Exception {
		cb.get("/ratelimit").run().assertStatus(404);
	}
}
