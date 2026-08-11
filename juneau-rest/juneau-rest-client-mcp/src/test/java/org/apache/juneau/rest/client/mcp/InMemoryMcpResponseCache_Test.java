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
package org.apache.juneau.rest.client.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link InMemoryMcpResponseCache}.
 */
class InMemoryMcpResponseCache_Test extends TestBase {

	// ==========================================================================
	// a — basic get/put
	// ==========================================================================

	@Test
	void a01_get_missingKey_returnsEmpty() {
		var cache = new InMemoryMcpResponseCache();
		assertTrue(cache.get("scope1", "key1").isEmpty());
	}

	@Test
	void a02_put_thenGet_returnsValue() {
		var cache = new InMemoryMcpResponseCache();
		cache.put("scope1", "key1", "value1", 0);
		assertEquals("value1", cache.get("scope1", "key1").get());
	}

	@Test
	void a03_clear_removesAllEntries() {
		var cache = new InMemoryMcpResponseCache();
		cache.put("scope1", "key1", "value1", 0);
		cache.put("scope2", "key2", "value2", 0);
		cache.clear();
		assertTrue(cache.get("scope1", "key1").isEmpty());
		assertTrue(cache.get("scope2", "key2").isEmpty());
	}

	// ==========================================================================
	// b — TTL expiry (deterministic clock)
	// ==========================================================================

	@Test
	void b01_ttl_beforeExpiry_returnsValue() {
		var now = new AtomicLong(1_000L);
		var cache = new InMemoryMcpResponseCache(now::get);
		cache.put("scope1", "key1", "value1", 500);
		now.set(1_499L);
		assertEquals("value1", cache.get("scope1", "key1").get());
	}

	@Test
	void b02_ttl_atOrAfterExpiry_returnsEmpty() {
		var now = new AtomicLong(1_000L);
		var cache = new InMemoryMcpResponseCache(now::get);
		cache.put("scope1", "key1", "value1", 500);
		now.set(1_500L);
		assertTrue(cache.get("scope1", "key1").isEmpty());
	}

	@Test
	void b03_zeroTtl_neverExpires() {
		var now = new AtomicLong(1_000L);
		var cache = new InMemoryMcpResponseCache(now::get);
		cache.put("scope1", "key1", "value1", 0);
		now.set(Long.MAX_VALUE - 1);
		assertEquals("value1", cache.get("scope1", "key1").get());
	}

	@Test
	void b04_negativeTtl_neverExpires() {
		var now = new AtomicLong(1_000L);
		var cache = new InMemoryMcpResponseCache(now::get);
		cache.put("scope1", "key1", "value1", -1);
		now.set(Long.MAX_VALUE - 1);
		assertEquals("value1", cache.get("scope1", "key1").get());
	}

	// ==========================================================================
	// c — scope partitioning
	// ==========================================================================

	@Test
	void c01_sameKey_differentScopes_doNotShareEntries() {
		var cache = new InMemoryMcpResponseCache();
		cache.put("private", "resources/read:file:///a", "valueA", 0);
		assertTrue(cache.get("public", "resources/read:file:///a").isEmpty());
		assertEquals("valueA", cache.get("private", "resources/read:file:///a").get());
	}

	@Test
	void c02_sameScope_sharesEntries() {
		var cache = new InMemoryMcpResponseCache();
		cache.put("public", "keyA", "valueA", 0);
		cache.put("public", "keyB", "valueB", 0);
		assertEquals("valueA", cache.get("public", "keyA").get());
		assertEquals("valueB", cache.get("public", "keyB").get());
	}

	// ==========================================================================
	// d — putResult (never caches errors)
	// ==========================================================================

	@Test
	void d01_putResult_successResponse_isCached() {
		var cache = new InMemoryMcpResponseCache();
		var res = JsonRpcResponse.ok("1", "the-result");
		cache.putResult("public", "tools/list", res, 0);
		assertEquals("the-result", cache.get("public", "tools/list").get());
	}

	@Test
	void d02_putResult_errorResponse_neverCached() {
		var cache = new InMemoryMcpResponseCache();
		var res = JsonRpcResponse.errorResponse("1", -32000, "boom");
		cache.putResult("public", "tools/list", res, 0);
		assertTrue(cache.get("public", "tools/list").isEmpty());
	}

	@Test
	void d03_putResult_nullResponse_neverCached() {
		var cache = new InMemoryMcpResponseCache();
		cache.putResult("public", "tools/list", null, 0);
		assertTrue(cache.get("public", "tools/list").isEmpty());
	}

	@Test
	void d04_putResult_nullResult_neverCached() {
		var cache = new InMemoryMcpResponseCache();
		var res = JsonRpcResponse.ok("1", null);
		cache.putResult("public", "tools/list", res, 0);
		assertTrue(cache.get("public", "tools/list").isEmpty());
	}

	// ==========================================================================
	// e — argument validation
	// ==========================================================================

	@Test
	void e01_put_nullScope_throwsIllegalArgumentException() {
		var cache = new InMemoryMcpResponseCache();
		assertThrows(IllegalArgumentException.class, () -> cache.put(null, "key1", "value1", 0));
	}

	@Test
	void e02_put_nullKey_throwsIllegalArgumentException() {
		var cache = new InMemoryMcpResponseCache();
		assertThrows(IllegalArgumentException.class, () -> cache.put("scope1", null, "value1", 0));
	}

	@Test
	void e03_put_nullValue_throwsIllegalArgumentException() {
		var cache = new InMemoryMcpResponseCache();
		assertThrows(IllegalArgumentException.class, () -> cache.put("scope1", "key1", null, 0));
	}

	@Test
	void e04_get_nullScope_throwsIllegalArgumentException() {
		var cache = new InMemoryMcpResponseCache();
		assertThrows(IllegalArgumentException.class, () -> cache.get(null, "key1"));
	}

	@Test
	void e05_get_nullKey_throwsIllegalArgumentException() {
		var cache = new InMemoryMcpResponseCache();
		assertThrows(IllegalArgumentException.class, () -> cache.get("scope1", null));
	}

	// ==========================================================================
	// f — bounded size / LRU eviction
	// ==========================================================================

	@Test
	void f01_boundedSize_evictsLeastRecentlyUsedEntry() {
		var cache = new InMemoryMcpResponseCache(2);
		cache.put("s", "k1", "v1", 0);
		cache.put("s", "k2", "v2", 0);
		cache.put("s", "k3", "v3", 0);
		assertTrue(cache.get("s", "k1").isEmpty());
		assertEquals("v2", cache.get("s", "k2").get());
		assertEquals("v3", cache.get("s", "k3").get());
	}

	@Test
	void f02_accessRefreshesRecency_evictsTrulyLeastRecentlyUsed() {
		var cache = new InMemoryMcpResponseCache(2);
		cache.put("s", "k1", "v1", 0);
		cache.put("s", "k2", "v2", 0);
		cache.get("s", "k1");
		cache.put("s", "k3", "v3", 0);
		assertEquals("v1", cache.get("s", "k1").get());
		assertTrue(cache.get("s", "k2").isEmpty());
		assertEquals("v3", cache.get("s", "k3").get());
	}

	@Test
	void f03_boundedSize_evictionSpansAllScopes() {
		var cache = new InMemoryMcpResponseCache(2);
		cache.put("a", "k", "v1", 0);
		cache.put("b", "k", "v2", 0);
		cache.put("c", "k", "v3", 0);
		assertTrue(cache.get("a", "k").isEmpty());
		assertEquals("v2", cache.get("b", "k").get());
		assertEquals("v3", cache.get("c", "k").get());
	}

	@Test
	void f04_zeroMaxEntries_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryMcpResponseCache(0));
	}

	@Test
	void f05_negativeMaxEntries_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> new InMemoryMcpResponseCache(-1));
	}

	@Test
	void f06_defaultMaxEntries_isPositive() {
		assertTrue(InMemoryMcpResponseCache.DEFAULT_MAX_ENTRIES > 0);
	}
}
