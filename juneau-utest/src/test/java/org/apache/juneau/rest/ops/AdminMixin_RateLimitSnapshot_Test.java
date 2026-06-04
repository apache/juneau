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
package org.apache.juneau.rest.ops;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates that {@link AdminMixin#getRateLimit} on the {@code /admin/ratelimit} endpoint
 * surfaces both static {@code config} and live {@code snapshot} bucket state when a
 * {@link RateLimitGuard} is registered.
 *
 * <p>
 * Builds on {@code AdminMixin_AsMixin_Test}'s {@code c01_rateLimitListsRegisteredGuard}
 * pattern; the snapshot coverage is the v1 follow-up that closes the {@code "buckets": []}
 * placeholder noted in the FINISHED-77 archive.
 *
 * @since 10.0.0
 */
class AdminMixin_RateLimitSnapshot_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Single RateLimitGuard bean — snapshot reports per-key bucket state after activity.
	//
	// Stash the guard in a static field so the instance the GuardList enforces on /items is the
	// same instance AdminMixin resolves from the bean store. The @Bean RateLimitGuard
	// factory method below is invoked once by the bean store; both the guard chain (via
	// bs.getBean(...)) and the admin endpoint pick up that single cached instance.
	// -----------------------------------------------------------------------------------------

	static final RateLimitGuard A_GUARD = RateLimitGuard.create()
		.permitsPerSecond(1)
		.burst(5)
		.keyBy(req -> req.getHeader("X-Key"))
		.exemptPaths("/admin/ratelimit", "/admin/threads", "/admin/heap", "/admin/cache/flush")
		.build();

	@Rest(mixins=AdminMixin.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(A_GUARD).build();
		}

		@Bean public RateLimitGuard rateLimit() { return A_GUARD; }

		@RestGet(path="/items") public String items() { return "ok"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_snapshotPopulatesAfterRequests() throws Exception {
		ca.get("/items").header("X-Key", "client-a").run().assertStatus(200);
		ca.get("/items").header("X-Key", "client-a").run().assertStatus(200);
		ca.get("/items").header("X-Key", "client-a").run().assertStatus(200);

		var body = ca.get("/admin/ratelimit").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var guards = (Map<?,?>) parsed.get("guards");
		Assertions.assertEquals(1, guards.size(), "expected exactly one guard entry");
		var entry = (Map<?,?>) guards.values().iterator().next();

		var config = (Map<?,?>) entry.get("config");
		Assertions.assertNotNull(config);
		Assertions.assertEquals(RateLimitGuard.class.getName(), config.get("class"));
		Assertions.assertEquals(5, ((Number) config.get("limit")).intValue());
		Assertions.assertEquals(1.0, ((Number) config.get("permitsPerSecond")).doubleValue(), 1e-9);
		Assertions.assertEquals(Boolean.FALSE, config.get("xForwardedForAware"));
		Assertions.assertNotNull(config.get("exemptPaths"));

		var snapshot = (List<?>) entry.get("snapshot");
		Assertions.assertNotNull(snapshot);
		Assertions.assertFalse(snapshot.isEmpty(), "snapshot should contain at least one bucket");
		// Find the client-a entry — there may also be a null-key entry from the admin call's
		// missing X-Key header if it hit the guard before our exempt list took effect.
		Map<?,?> clientBucket = null;
		for (var b : snapshot) {
			var m = (Map<?,?>) b;
			if ("client-a".equals(m.get("key"))) {
				clientBucket = m;
				break;
			}
		}
		Assertions.assertNotNull(clientBucket, "expected a bucket for key 'client-a' in snapshot: " + snapshot);
		var remaining = ((Number) clientBucket.get("remaining")).intValue();
		Assertions.assertTrue(remaining >= 1 && remaining <= 2,
			"expected remaining in [1, 2] after 3 acquisitions on capacity-5 bucket; got " + remaining);
		Assertions.assertEquals(Boolean.FALSE, clientBucket.get("throttled"),
			"bucket should not be throttled after only 3 of 5 tokens consumed");
		Assertions.assertNotNull(clientBucket.get("lastRequest"));
	}

	// -----------------------------------------------------------------------------------------
	// Exhaust the bucket to flip throttled=true / remaining=0 in the snapshot.
	// -----------------------------------------------------------------------------------------

	static final RateLimitGuard B_GUARD = RateLimitGuard.create()
		.permitsPerMinute(1)
		.burst(1)
		.keyBy(req -> "static")
		.exemptPaths("/admin/ratelimit", "/admin/threads", "/admin/heap", "/admin/cache/flush")
		.build();

	@Rest(mixins=AdminMixin.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(B_GUARD).build();
		}

		@Bean public RateLimitGuard rateLimit() { return B_GUARD; }

		@RestGet(path="/items") public String items() { return "ok"; }
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_exhaustedBucketHasThrottledTrueInSnapshot() throws Exception {
		cb.get("/items").run().assertStatus(200);
		cb.get("/items").run().assertStatus(429);

		var body = cb.get("/admin/ratelimit").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var entry = (Map<?,?>) ((Map<?,?>) parsed.get("guards")).values().iterator().next();
		var snapshot = (List<?>) entry.get("snapshot");
		Assertions.assertFalse(snapshot.isEmpty());
		var bucket = (Map<?,?>) snapshot.get(0);
		Assertions.assertEquals("static", bucket.get("key"));
		Assertions.assertEquals(0, ((Number) bucket.get("remaining")).intValue());
		Assertions.assertEquals(Boolean.TRUE, bucket.get("throttled"));
	}

	// -----------------------------------------------------------------------------------------
	// Multiple keys → snapshot lists every key sorted ascending.
	// -----------------------------------------------------------------------------------------

	static final RateLimitGuard C_GUARD = RateLimitGuard.create()
		.permitsPerSecond(1)
		.burst(5)
		.xForwardedForAware(true)
		.exemptPaths("/admin/ratelimit", "/admin/threads", "/admin/heap", "/admin/cache/flush")
		.build();

	@Rest(mixins=AdminMixin.class)
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(C_GUARD).build();
		}

		@Bean public RateLimitGuard rateLimit() { return C_GUARD; }

		@RestGet(path="/items") public String items() { return "ok"; }
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_multiKeySnapshotIsSortedAscending() throws Exception {
		cc.get("/items").header("X-Forwarded-For", "10.0.0.30").run().assertStatus(200);
		cc.get("/items").header("X-Forwarded-For", "10.0.0.10").run().assertStatus(200);
		cc.get("/items").header("X-Forwarded-For", "10.0.0.20").run().assertStatus(200);

		var body = cc.get("/admin/ratelimit").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var entry = (Map<?,?>) ((Map<?,?>) parsed.get("guards")).values().iterator().next();
		var snapshot = (List<?>) entry.get("snapshot");
		Assertions.assertTrue(snapshot.size() >= 3, "expected at least 3 entries; got " + snapshot.size());
		// Verify the three tenant keys are present and that the overall list is sorted ascending.
		var keys = new ArrayList<String>();
		for (var b : snapshot)
			keys.add((String) ((Map<?,?>) b).get("key"));
		Assertions.assertTrue(keys.contains("10.0.0.10"));
		Assertions.assertTrue(keys.contains("10.0.0.20"));
		Assertions.assertTrue(keys.contains("10.0.0.30"));
		var sorted = new ArrayList<>(keys);
		Collections.sort(sorted);
		Assertions.assertEquals(sorted, keys, "snapshot entries should be sorted ascending by key");
	}

	@Test void c02_configReflectsXForwardedForAndExemptPaths() throws Exception {
		var body = cc.get("/admin/ratelimit").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var entry = (Map<?,?>) ((Map<?,?>) parsed.get("guards")).values().iterator().next();
		var config = (Map<?,?>) entry.get("config");
		Assertions.assertEquals(Boolean.TRUE, config.get("xForwardedForAware"));
		var exempt = (List<?>) config.get("exemptPaths");
		Assertions.assertTrue(exempt.contains("/admin/ratelimit"));
	}

	// -----------------------------------------------------------------------------------------
	// Custom Storage that doesn't override snapshot() → response has snapshot == [], no error.
	// -----------------------------------------------------------------------------------------

	/** No-op storage that always admits and never overrides snapshot(). */
	static final class E01_NoSnapshotStorage implements RateLimitGuard.Storage {
		@Override public RateLimitGuard.Storage.AcquireResult tryAcquire(String key, int capacity, double permitsPerSecond) {
			return new RateLimitGuard.Storage.AcquireResult(true, capacity, 0L);
		}
		@Override public void evict(Duration ttl) { /* intentionally empty */ }
	}

	static final RateLimitGuard E_GUARD = RateLimitGuard.create()
		.permitsPerSecond(1)
		.burst(1)
		.storage(new E01_NoSnapshotStorage())
		.build();

	@Rest(mixins=AdminMixin.class)
	public static class E extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}

		@Bean public RateLimitGuard rateLimit() { return E_GUARD; }
	}

	private static final MockRestClient ce = MockRestClient.buildLax(E.class);

	@Test void e01_customStorageWithoutSnapshotOverrideEmitsEmptyArray() throws Exception {
		var body = ce.get("/admin/ratelimit").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var entry = (Map<?,?>) ((Map<?,?>) parsed.get("guards")).values().iterator().next();
		Assertions.assertNotNull(entry.get("config"));
		var snapshot = (List<?>) entry.get("snapshot");
		Assertions.assertNotNull(snapshot);
		Assertions.assertTrue(snapshot.isEmpty(), "snapshot should be empty for a storage that doesn't override snapshot()");
	}
}
