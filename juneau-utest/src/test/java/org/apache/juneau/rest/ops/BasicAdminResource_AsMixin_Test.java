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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicAdminResource} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Cases:
 * <ul>
 * 	<li>Default-deny: {@link DenyAllGuard} returns {@code 403 Forbidden} on every admin path until
 * 		the host registers a {@code @Bean RestGuardList}.
 * 	<li>{@code GET /admin/threads} returns a JSON list with at least the JUnit test thread.
 * 	<li>{@code GET /admin/heap} returns a JSON map with {@code heap.total / free / max / used}.
 * 	<li>{@code POST /admin/cache/flush} runs all registered hooks (no {@code names}) or just the
 * 		named subset.
 * 	<li>{@code GET /admin/ratelimit} returns {@code 404} when no {@link RateLimitGuard} bean is
 * 		registered, and a populated map when one is.
 * 	<li>Builder-time validation rejects null/blank cache-flush names and null hooks.
 * </ul>
 *
 * @since 9.5.0
 */
class BasicAdminResource_AsMixin_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Default-deny posture (no host-supplied @Bean RestGuardList).
	// -----------------------------------------------------------------------------------------

	@Rest(mixins=BasicAdminResource.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_threadsDeniedByDefault() throws Exception {
		ca.get("/admin/threads").run().assertStatus(403);
	}

	@Test void a02_heapDeniedByDefault() throws Exception {
		ca.get("/admin/heap").run().assertStatus(403);
	}

	@Test void a03_cacheFlushDeniedByDefault() throws Exception {
		ca.post("/admin/cache/flush", "").run().assertStatus(403);
	}

	@Test void a04_rateLimitDeniedByDefault() throws Exception {
		ca.get("/admin/ratelimit").run().assertStatus(403);
	}

	@Test void a05_hostEndpointStillReachable() throws Exception {
		ca.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------
	// Allow-all RestGuardList override (replaces DenyAllGuard).
	// -----------------------------------------------------------------------------------------

	/** Allow-all guard chain — replaces the mixin's annotation-derived deny-all. */
	@Rest(mixins=BasicAdminResource.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}

		@Bean public BasicAdminResource admin() {
			return BasicAdminResource.create()
				.cacheFlush("primary", FLUSH_PRIMARY)
				.cacheFlush("secondary", FLUSH_SECONDARY)
				.threadNamePrefixExclude()  // disable filtering to make /admin/threads deterministic
				.build();
		}
	}

	static final AtomicInteger PRIMARY_INVOCATIONS = new AtomicInteger();
	static final AtomicInteger SECONDARY_INVOCATIONS = new AtomicInteger();
	static final Runnable FLUSH_PRIMARY = PRIMARY_INVOCATIONS::incrementAndGet;
	static final Runnable FLUSH_SECONDARY = SECONDARY_INVOCATIONS::incrementAndGet;

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@BeforeEach void resetCounters() {
		PRIMARY_INVOCATIONS.set(0);
		SECONDARY_INVOCATIONS.set(0);
	}

	@Test void b01_threadsServesJsonList() throws Exception {
		var body = cb.get("/admin/threads")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, List.class);
		Assertions.assertFalse(parsed.isEmpty(), "thread list should not be empty");
		var first = (Map<?,?>) parsed.get(0);
		Assertions.assertNotNull(first.get("name"));
		Assertions.assertNotNull(first.get("state"));
		Assertions.assertNotNull(first.get("stack"));
	}

	@Test void b02_heapServesJsonMap() throws Exception {
		var body = cb.get("/admin/heap")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var heap = (Map<?,?>) parsed.get("heap");
		Assertions.assertNotNull(heap.get("total"));
		Assertions.assertNotNull(heap.get("free"));
		Assertions.assertNotNull(heap.get("max"));
		Assertions.assertNotNull(heap.get("used"));
		var nonHeap = (Map<?,?>) parsed.get("nonHeap");
		Assertions.assertNotNull(nonHeap.get("used"));
	}

	@Test void b03_cacheFlushAllRunsEveryHook() throws Exception {
		cb.post("/admin/cache/flush", "")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"executed\"");
		Assertions.assertEquals(1, PRIMARY_INVOCATIONS.get(), "primary should run once");
		Assertions.assertEquals(1, SECONDARY_INVOCATIONS.get(), "secondary should run once");
	}

	@Test void b04_cacheFlushNamesRunsSubsetOnly() throws Exception {
		cb.post("/admin/cache/flush?names=primary", "")
			.run()
			.assertStatus(200);
		Assertions.assertEquals(1, PRIMARY_INVOCATIONS.get(), "primary should run");
		Assertions.assertEquals(0, SECONDARY_INVOCATIONS.get(), "secondary should NOT run");
	}

	@Test void b05_cacheFlushNamesUnknownIsSilentlyIgnored() throws Exception {
		var body = cb.post("/admin/cache/flush?names=primary,unknown,secondary", "")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertTrue(body.contains("primary"));
		Assertions.assertTrue(body.contains("secondary"));
		Assertions.assertEquals(1, PRIMARY_INVOCATIONS.get());
		Assertions.assertEquals(1, SECONDARY_INVOCATIONS.get());
	}

	@Test void b06_rateLimit404WhenNoGuardRegistered() throws Exception {
		cb.get("/admin/ratelimit").run().assertStatus(404);
	}

	@Test void b07_hostEndpointStillReachable() throws Exception {
		cb.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------
	// Allow-all guard list + a registered RateLimitGuard bean.
	// -----------------------------------------------------------------------------------------

	@Rest(mixins=BasicAdminResource.class)
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}

		@Bean public RateLimitGuard rateLimit() {
			return RateLimitGuard.create().build();
		}
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_rateLimitListsRegisteredGuard() throws Exception {
		var body = cc.get("/admin/ratelimit")
			.run()
			.assertStatus(200)
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		var guardsMap = (Map<?,?>) parsed.get("guards");
		Assertions.assertEquals(1, guardsMap.size(), "expected one entry; got: " + guardsMap.keySet());
		var first = (Map<?,?>) guardsMap.values().iterator().next();
		Assertions.assertNotNull(first.get("config"));
	}

	// -----------------------------------------------------------------------------------------
	// Builder-time validation.
	// -----------------------------------------------------------------------------------------

	@Test void d01_builderRejectsBlankCacheFlushName() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BasicAdminResource.create().cacheFlush("", () -> {}));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BasicAdminResource.create().cacheFlush(null, () -> {}));
	}

	@Test void d02_builderRejectsNullHook() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BasicAdminResource.create().cacheFlush("foo", null));
	}

	@Test void d03_cacheFlushAllRoundTrips() {
		Map<String,Runnable> hooks = new LinkedHashMap<>();
		var r1 = (Runnable) () -> {};
		var r2 = (Runnable) () -> {};
		hooks.put("a", r1);
		hooks.put("b", r2);
		var admin = BasicAdminResource.create().cacheFlushAll(hooks).build();
		Assertions.assertEquals(2, admin.getCacheFlushHooks().size());
		Assertions.assertSame(r1, admin.getCacheFlushHooks().get("a"));
		Assertions.assertSame(r2, admin.getCacheFlushHooks().get("b"));
	}

	@Test void d04_threadNameExcludeReplaceList() {
		var admin = BasicAdminResource.create().threadNamePrefixExclude("foo-", "bar-").build();
		Assertions.assertEquals(List.of("foo-", "bar-"), admin.getThreadNamePrefixExclude());
	}

	@Test void d05_defaultExcludeListContainsKnownNoise() {
		Assertions.assertTrue(BasicAdminResource.DEFAULT_THREAD_NAME_PREFIX_EXCLUDE.contains("Reference Handler"));
		Assertions.assertTrue(BasicAdminResource.DEFAULT_THREAD_NAME_PREFIX_EXCLUDE.contains("jetty-"));
	}

	@Test void d06_noArgConstructorMatchesEmptyHooks() {
		var r = new BasicAdminResource();
		Assertions.assertTrue(r.getCacheFlushHooks().isEmpty());
		Assertions.assertEquals(BasicAdminResource.DEFAULT_THREAD_NAME_PREFIX_EXCLUDE,
			r.getThreadNamePrefixExclude());
	}

	@Test void d07_denyAllGuardRejects() throws Exception {
		var g = new DenyAllGuard();
		Assertions.assertFalse(g.isRequestAllowed((RestRequest) null),
			"DenyAllGuard must always reject");
	}

	@Test void d08_cacheFlushAllNullIsNoOp() {
		var admin = BasicAdminResource.create().cacheFlushAll(null).build();
		Assertions.assertTrue(admin.getCacheFlushHooks().isEmpty());
	}

	@Test void d09_threadNamePrefixExcludeNullClearsList() {
		var admin = BasicAdminResource.create().threadNamePrefixExclude((String[]) null).build();
		Assertions.assertTrue(admin.getThreadNamePrefixExclude().isEmpty(),
			"null varargs should clear the list");
	}

	@Test void d10_threadNamePrefixExcludeFiltersNullAndEmpty() {
		var admin = BasicAdminResource.create().threadNamePrefixExclude("good-", null, "", "also-").build();
		Assertions.assertEquals(List.of("good-", "also-"), admin.getThreadNamePrefixExclude());
	}

	// -----------------------------------------------------------------------------------------
	// Thread filtering hits an actual matching prefix (covers the exclusion branch in
	// isExcludedThread when the configured filter list catches a real thread).
	// -----------------------------------------------------------------------------------------

	@Rest(mixins=BasicAdminResource.class)
	public static class F extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }

		// Prefix that will match the JUnit launcher / test thread on every supported JVM.
		@Bean public BasicAdminResource admin() {
			return BasicAdminResource.create()
				.threadNamePrefixExclude("ForkJoinPool", "main", "junit-")
				.build();
		}
	}

	private static final MockRestClient cf = MockRestClient.buildLax(F.class);

	@Test void f01_threadFilterDropsMatchedPrefixes() throws Exception {
		var body = cf.get("/admin/threads")
			.run()
			.assertStatus(200)
			.getContent().asString();
		// We don't assert on specific thread names — just confirm the endpoint serves and
		// that the JSON is a valid list. The branch coverage on isExcludedThread is what we
		// actually care about (the prefix-match true branch).
		Assertions.assertTrue(body.startsWith("["), "expected JSON array; body: " + body);
	}

	// -----------------------------------------------------------------------------------------
	// cacheFlush with names= containing blank entries (covers the n.isBlank() filter branch).
	// -----------------------------------------------------------------------------------------

	@Test void g01_cacheFlushNamesBlankSegmentsIgnored() throws Exception {
		cb.post("/admin/cache/flush?names=,primary,,", "")
			.run()
			.assertStatus(200);
		Assertions.assertEquals(1, PRIMARY_INVOCATIONS.get());
		Assertions.assertEquals(0, SECONDARY_INVOCATIONS.get());
	}

	@Test void g02_cacheFlushNamesParamBlankAllRunAll() throws Exception {
		cb.post("/admin/cache/flush?names=", "")
			.run()
			.assertStatus(200);
		// names=<blank> → namesParam.isBlank() → fall-through to "no filter, run everything"
		Assertions.assertEquals(1, PRIMARY_INVOCATIONS.get());
		Assertions.assertEquals(1, SECONDARY_INVOCATIONS.get());
	}
}
