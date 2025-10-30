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
package org.apache.juneau.common.collections;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Cache_Test extends TestBase {

	//====================================================================================================
	// Basic cache operations - get, hit, miss
	//====================================================================================================

	@Test void a01_basicGet_cacheMiss() {
		var cache = Cache.of(String.class, String.class).build();
		var callCount = new AtomicInteger();

		var result = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "value1";
		});

		assertEquals("value1", result);
		assertEquals(1, callCount.get());
		assertSize(1, cache);
		assertEquals(0, cache.getCacheHits());
	}

	@Test void a02_basicGet_cacheHit() {
		var cache = Cache.of(String.class, String.class).build();
		var callCount = new AtomicInteger();

		// First call - cache miss
		var result1 = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "value1";
		});

		// Second call - cache hit
		var result2 = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "should not be called";
		});

		assertEquals("value1", result1);
		assertEquals("value1", result2);
		assertSame(result1, result2); // Same instance
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a03_multipleKeys() {
		var cache = Cache.of(String.class, Integer.class).build();

		var v1 = cache.get("one", () -> 1);
		var v2 = cache.get("two", () -> 2);
		var v3 = cache.get("three", () -> 3);

		assertEquals(1, v1);
		assertEquals(2, v2);
		assertEquals(3, v3);
		assertSize(3, cache);
		assertEquals(0, cache.getCacheHits());

		// Verify all cached
		var v1Again = cache.get("one", () -> 999);
		var v2Again = cache.get("two", () -> 999);
		var v3Again = cache.get("three", () -> 999);

		assertEquals(1, v1Again);
		assertEquals(2, v2Again);
		assertEquals(3, v3Again);
		assertEquals(3, cache.getCacheHits());
	}

	//====================================================================================================
	// Null key handling
	//====================================================================================================

	@Test void a04_nullKey_throwsException() {
		var cache = Cache.of(String.class, String.class).build();

		// Null keys are not allowed and throw IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> cache.get(null, () -> "value"));
		assertThrows(IllegalArgumentException.class, () -> cache.get(null));
	}

	//====================================================================================================
	// Size and clear operations
	//====================================================================================================

	@Test void a06_size() {
		var cache = Cache.of(String.class, Integer.class).build();

		assertEmpty(cache);

		cache.get("one", () -> 1);
		assertSize(1, cache);

		cache.get("two", () -> 2);
		assertSize(2, cache);

		cache.get("three", () -> 3);
		assertSize(3, cache);

		// Accessing existing key doesn't change size
		cache.get("one", () -> 999);
		assertSize(3, cache);
	}

	@Test void a07_clear() {
		var cache = Cache.of(String.class, Integer.class).build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("one", () -> 999); // Cache hit

		assertSize(2, cache);
		assertEquals(1, cache.getCacheHits());

		cache.clear();

		assertEmpty(cache);
		assertEquals(1, cache.getCacheHits()); // Hits not cleared

		// Values must be recomputed after clear
		var v1 = cache.get("one", () -> 10);
		assertEquals(10, v1);
		assertSize(1, cache);
	}

	//====================================================================================================
	// Max size and eviction
	//====================================================================================================

	@Test void a08_maxSize_eviction() {
		var cache = Cache.of(String.class, Integer.class)
			.maxSize(3)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);
		assertSize(3, cache);

		// 4th item doesn't trigger eviction (size > maxSize means 4 > 3)
		cache.get("four", () -> 4);
		assertSize(4, cache);

		// 5th item triggers eviction (size=4, 4 > 3, so clear then add)
		cache.get("five", () -> 5);
		assertSize(1, cache); // Only the new item
	}

	@Test void a09_maxSize_custom() {
		var cache = Cache.of(String.class, String.class)
			.maxSize(5)
			.build();

		for (int i = 1; i <= 5; i++) {
			final int index = i;
			cache.get("key" + index, () -> "value" + index);
		}
		assertSize(5, cache);

		// 6th item doesn't trigger clear yet (5 > 5 is false)
		cache.get("key6", () -> "value6");
		assertSize(6, cache);

		// 7th item triggers clear (6 > 5 is true)
		cache.get("key7", () -> "value7");
		assertSize(1, cache);
	}

	//====================================================================================================
	// Disabled cache
	//====================================================================================================

	@Test void a10_disabled_neverCaches() {
		var cache = Cache.of(String.class, String.class)
			.disableCaching()
			.build();
		var callCount = new AtomicInteger();

		var result1 = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "value" + callCount.get();
		});

		var result2 = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "value" + callCount.get();
		});

		assertEquals("value1", result1);
		assertEquals("value2", result2);
		assertEquals(2, callCount.get()); // Always calls supplier
		assertEmpty(cache);
		assertEquals(0, cache.getCacheHits());
	}

	@Test void a11_disabled_sizeAlwaysZero() {
		var cache = Cache.of(String.class, Integer.class)
			.disableCaching()
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);

		assertEmpty(cache);
	}

	@Test void a12_disabled_clearHasNoEffect() {
		var cache = Cache.of(String.class, Integer.class)
			.disableCaching()
			.build();

		cache.clear(); // Should not throw
		assertEmpty(cache);
	}

	//====================================================================================================
	// Builder configuration
	//====================================================================================================

	@Test void a13_builder_defaults() {
		var cache = Cache.of(String.class, String.class).build();

		// Should work with defaults
		cache.get("key1", () -> "value1");
		assertSize(1, cache);
	}

	@Test void a14_builder_chaining() {
		var cache = Cache.of(String.class, String.class)
			.maxSize(100)
			.disableCaching()
			.build();

		// Disabled takes precedence
		cache.get("key1", () -> "value1");
		assertEmpty(cache);
	}

	//====================================================================================================
	// Cache hits statistics
	//====================================================================================================

	@Test void a15_cacheHits_countsCorrectly() {
		var cache = Cache.of(String.class, Integer.class).build();

		assertEquals(0, cache.getCacheHits());

		cache.get("one", () -> 1); // Miss
		assertEquals(0, cache.getCacheHits());

		cache.get("one", () -> 999); // Hit
		assertEquals(1, cache.getCacheHits());

		cache.get("two", () -> 2); // Miss
		assertEquals(1, cache.getCacheHits());

		cache.get("one", () -> 999); // Hit
		cache.get("two", () -> 999); // Hit
		assertEquals(3, cache.getCacheHits());
	}

	@Test void a16_cacheHits_persistsAfterClear() {
		var cache = Cache.of(String.class, Integer.class).build();

		cache.get("one", () -> 1);
		cache.get("one", () -> 999); // Hit
		assertEquals(1, cache.getCacheHits());

		cache.clear();
		assertEquals(1, cache.getCacheHits()); // Still 1

		cache.get("one", () -> 1); // Miss (recomputed)
		cache.get("one", () -> 999); // Hit
		assertEquals(2, cache.getCacheHits()); // Incremented
	}

	//====================================================================================================
	// Thread safety and concurrency
	//====================================================================================================

	@Test void a17_concurrentAccess() throws Exception {
		var cache = Cache.of(Integer.class, String.class).build();
		var executor = Executors.newFixedThreadPool(10);
		var callCount = new AtomicInteger();

		// Submit 100 tasks that all try to cache the same key
		var futures = new CompletableFuture[100];
		for (int i = 0; i < 100; i++) {
			futures[i] = CompletableFuture.runAsync(() -> {
				cache.get(1, () -> {
					callCount.incrementAndGet();
					return "value";
				});
			}, executor);
		}

		// Wait for all tasks to complete
		CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

		// Supplier might be called multiple times due to putIfAbsent semantics,
		// but should be much less than 100
		assertTrue(callCount.get() < 10, "Supplier called " + callCount.get() + " times");
		assertSize(1, cache);

		executor.shutdown();
	}

	@Test void a18_concurrentDifferentKeys() throws Exception {
		var cache = Cache.of(Integer.class, String.class).build();
		var executor = Executors.newFixedThreadPool(10);

		// Submit tasks for different keys
		var futures = new CompletableFuture[10];
		for (int i = 0; i < 10; i++) {
			final int key = i;
			futures[i] = CompletableFuture.runAsync(() -> {
				cache.get(key, () -> "value" + key);
			}, executor);
		}

		// Wait for all tasks to complete
		CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

		assertSize(10, cache);

		executor.shutdown();
	}

	//====================================================================================================
	// Different key/value types
	//====================================================================================================

	@Test void a19_integerKeys() {
		var cache = Cache.of(Integer.class, String.class).build();

		cache.get(1, () -> "one");
		cache.get(2, () -> "two");

		assertEquals("one", cache.get(1, () -> "should not call"));
		assertSize(2, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a20_classKeys() {
		var cache = Cache.of(Class.class, String.class).build();

		cache.get(String.class, () -> "String");
		cache.get(Integer.class, () -> "Integer");

		assertEquals("String", cache.get(String.class, () -> "should not call"));
		assertSize(2, cache);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test void a21_sameKeyDifferentValues_returnsFirstCached() {
		var cache = Cache.of(String.class, String.class).build();

		var result1 = cache.get("key", () -> "first");
		var result2 = cache.get("key", () -> "second");

		assertEquals("first", result1);
		assertEquals("first", result2); // Returns cached, not "second"
		assertSize(1, cache);
	}

	@Test void a22_emptyCache_operations() {
		var cache = Cache.of(String.class, String.class).build();

		assertEmpty(cache);
		assertEquals(0, cache.getCacheHits());
		cache.clear(); // Should not throw on empty cache
		assertEmpty(cache);
	}

	@Test void a23_maxSize_exactBoundary() {
		var cache = Cache.of(Integer.class, String.class)
			.maxSize(3)
			.build();

		cache.get(1, () -> "one");
		cache.get(2, () -> "two");
		cache.get(3, () -> "three");

		assertSize(3, cache);

		// Accessing existing keys shouldn't trigger eviction
		cache.get(1, () -> "should not call");
		cache.get(2, () -> "should not call");
		assertSize(3, cache);
		assertEquals(2, cache.getCacheHits());
	}
}

