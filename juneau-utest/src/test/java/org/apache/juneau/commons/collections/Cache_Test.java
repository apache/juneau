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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.collections.CacheMode.*;
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

	@Test void a04_nullKey_allowed() {
		var cache = Cache.of(String.class, String.class)
			.supplier(k -> "value-" + k)
			.build();

		// Null keys are now allowed
		assertEquals("value-null", cache.get(null, () -> "value-null"));
		
		// Verify caching works with null keys
		assertEquals("value-null", cache.get(null)); // Cached (hit #1)
		assertEquals(1, cache.getCacheHits());
		
		assertEquals("value-null", cache.get(null)); // Cached (hit #2)
		assertEquals(2, cache.getCacheHits());
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

		for (var i = 1; i <= 5; i++) {
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
			.cacheMode(NONE)
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
			.cacheMode(NONE)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);

		assertEmpty(cache);
	}

	@Test void a12_disabled_clearHasNoEffect() {
		var cache = Cache.of(String.class, Integer.class)
			.cacheMode(NONE)
			.build();

		cache.clear(); // Should not throw
		assertEmpty(cache);
	}

	//====================================================================================================
	// Weak cache mode
	//====================================================================================================

	@Test void a13_weakMode_basicCaching() {
		var cache = Cache.of(String.class, String.class)
			.cacheMode(WEAK)
			.build();
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
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a14_weakMode_multipleKeys() {
		var cache = Cache.of(String.class, Integer.class)
			.cacheMode(WEAK)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);

		assertSize(3, cache);
		assertEquals(0, cache.getCacheHits());

		// Verify all cached
		assertEquals(1, cache.get("one", () -> 999));
		assertEquals(2, cache.get("two", () -> 999));
		assertEquals(3, cache.get("three", () -> 999));
		assertEquals(3, cache.getCacheHits());
	}

	@Test void a15_weakMode_clear() {
		var cache = Cache.of(String.class, Integer.class)
			.cacheMode(WEAK)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		assertSize(2, cache);

		cache.clear();
		assertEmpty(cache);
	}

	@Test void a16_weakMode_maxSize() {
		var cache = Cache.of(String.class, Integer.class)
			.cacheMode(WEAK)
			.maxSize(3)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);
		assertSize(3, cache);

		// 4th item doesn't trigger eviction yet
		cache.get("four", () -> 4);
		assertSize(4, cache);

		// 5th item triggers eviction
		cache.get("five", () -> 5);
		assertSize(1, cache);
	}

	@Test void a16b_weakMethod_basicCaching() {
		// Test the weak() convenience method
		var cache = Cache.of(String.class, String.class)
			.weak()
			.build();
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
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a16c_weakMethod_chaining() {
		// Test that weak() can be chained with other builder methods
		var cache = Cache.of(String.class, Integer.class)
			.weak()
			.maxSize(100)
			.supplier(k -> k.length())
			.build();

		var result = cache.get("hello");
		assertEquals(5, result);
		assertSize(1, cache);
	}

	//====================================================================================================
	// Builder configuration
	//====================================================================================================

	@Test void a17_builder_defaults() {
		var cache = Cache.of(String.class, String.class).build();

		// Should work with defaults
		cache.get("key1", () -> "value1");
		assertSize(1, cache);
	}

	@Test void a18_builder_chaining() {
		var cache = Cache.of(String.class, String.class)
			.maxSize(100)
			.cacheMode(NONE)
			.build();

		// Disabled takes precedence
		cache.get("key1", () -> "value1");
		assertEmpty(cache);
	}

	//====================================================================================================
	// Cache hits statistics
	//====================================================================================================

	@Test void a19_cacheHits_countsCorrectly() {
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

	@Test void a20_cacheHits_persistsAfterClear() {
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

	@Test void a21_concurrentAccess() throws Exception {
		var cache = Cache.of(Integer.class, String.class).build();
		var executor = Executors.newFixedThreadPool(10);
		var callCount = new AtomicInteger();

		// Submit 100 tasks that all try to cache the same key
		var futures = new CompletableFuture[100];
		for (var i = 0; i < 100; i++) {
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

	@Test void a22_concurrentDifferentKeys() throws Exception {
		var cache = Cache.of(Integer.class, String.class).build();
		var executor = Executors.newFixedThreadPool(10);

		// Submit tasks for different keys
		var futures = new CompletableFuture[10];
		for (var i = 0; i < 10; i++) {
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

	@Test void a23_integerKeys() {
		var cache = Cache.of(Integer.class, String.class).build();

		cache.get(1, () -> "one");
		cache.get(2, () -> "two");

		assertEquals("one", cache.get(1, () -> "should not call"));
		assertSize(2, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a24_classKeys() {
		var cache = Cache.of(Class.class, String.class).build();

		cache.get(String.class, () -> "String");
		cache.get(Integer.class, () -> "Integer");

		assertEquals("String", cache.get(String.class, () -> "should not call"));
		assertSize(2, cache);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test void a25_sameKeyDifferentValues_returnsFirstCached() {
		var cache = Cache.of(String.class, String.class).build();

		var result1 = cache.get("key", () -> "first");
		var result2 = cache.get("key", () -> "second");

		assertEquals("first", result1);
		assertEquals("first", result2); // Returns cached, not "second"
		assertSize(1, cache);
	}

	@Test void a26_emptyCache_operations() {
		var cache = Cache.of(String.class, String.class).build();

		assertEmpty(cache);
		assertEquals(0, cache.getCacheHits());
		cache.clear(); // Should not throw on empty cache
		assertEmpty(cache);
	}

	@Test void a27_maxSize_exactBoundary() {
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

	//====================================================================================================
	// logOnExit configuration
	//====================================================================================================

	@Test void a28_logOnExit_withStringId() {
		// Test that logOnExit(String) enables logging and sets the id
		var cache = Cache.of(String.class, String.class)
			.logOnExit("TestCache")
			.build();

		// Use the cache to generate some statistics
		cache.get("key1", () -> "value1");
		cache.get("key1", () -> "should not be called"); // Cache hit
		cache.get("key2", () -> "value2");

		// Verify cache works normally
		assertSize(2, cache);
		assertEquals(1, cache.getCacheHits());

		// Note: We can't easily test that the shutdown hook was actually registered
		// without triggering JVM shutdown, but we can verify the cache was created
		// and works correctly with logOnExit enabled
	}

	@Test void a29_logOnExit_withBooleanTrue() {
		// Test that logOnExit(boolean, String) with true enables logging
		var cache = Cache.of(String.class, Integer.class)
			.logOnExit(true, "MyCache")
			.build();

		cache.get("one", () -> 1);
		cache.get("one", () -> 999); // Cache hit

		assertSize(1, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a30_logOnExit_withBooleanFalse() {
		// Test that logOnExit(boolean, String) with false disables logging
		var cache = Cache.of(String.class, Integer.class)
			.logOnExit(false, "DisabledCache")
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);

		assertSize(2, cache);
		assertEquals(0, cache.getCacheHits());
	}

	@Test void a31_logOnExit_chaining() {
		// Test that logOnExit can be chained with other builder methods
		var cache = Cache.of(String.class, String.class)
			.maxSize(100)
			.logOnExit("ChainedCache")
			.supplier(k -> "value-" + k)
			.build();

		var result = cache.get("test");
		assertEquals("value-test", result);
		assertSize(1, cache);
	}

	@Test void a32_logOnExit_multipleCalls_lastWins() {
		// Test that calling logOnExit multiple times updates the id
		var cache = Cache.of(String.class, String.class)
			.logOnExit("FirstId")
			.logOnExit("SecondId")
			.logOnExit(true, "FinalId")
			.build();

		cache.get("key", () -> "value");
		assertSize(1, cache);
		// The final id should be "FinalId" (though we can't easily verify this without
		// checking the shutdown hook, the cache should still work correctly)
	}

	//====================================================================================================
	// put() method
	//====================================================================================================

	@Test void a33_put_directInsertion() {
		var cache = Cache.of(String.class, String.class).build();

		// Put a value directly
		var previous = cache.put("key1", "value1");
		assertNull(previous, "Should return null for new key");
		assertEquals("value1", cache.get("key1", () -> "should not be called"));
		assertSize(1, cache);
	}

	@Test void a34_put_overwritesExisting() {
		var cache = Cache.of(String.class, String.class).build();

		cache.put("key1", "value1");
		var previous = cache.put("key1", "value2");
		assertEquals("value1", previous, "Should return previous value");
		assertEquals("value2", cache.get("key1", () -> "should not be called"));
		assertSize(1, cache);
	}

	@Test void a35_put_withNullValue() {
		var cache = Cache.of(String.class, String.class).build();

		cache.put("key1", "value1");
		var previous = cache.put("key1", null);
		assertEquals("value1", previous);
		// Null values are not cached, so key should be removed
		assertFalse(cache.containsKey("key1"), "Key should be removed when null value is put");
		// Null values are not cached, so get() will call supplier
		var callCount = new AtomicInteger();
		var result = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "supplied";
		});
		assertEquals("supplied", result);
		assertEquals(1, callCount.get());
		// After get() with non-null supplier, key is now in cache again
		assertTrue(cache.containsKey("key1"), "Key should be in cache after get() with non-null supplier");
	}

	@Test void a35b_put_withNullValue_newKey() {
		var cache = Cache.of(String.class, String.class).build();
		// Putting null for a new key should return null and not add anything
		var previous = cache.put("key1", null);
		assertNull(previous);
		assertFalse(cache.containsKey("key1"));
		assertTrue(cache.isEmpty());
	}

	//====================================================================================================
	// isEmpty() method
	//====================================================================================================

	@Test void a36_isEmpty_newCache() {
		var cache = Cache.of(String.class, String.class).build();
		assertTrue(cache.isEmpty());
	}

	@Test void a37_isEmpty_afterPut() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		assertFalse(cache.isEmpty());
	}

	@Test void a38_isEmpty_afterGet() {
		var cache = Cache.of(String.class, String.class).build();
		cache.get("key1", () -> "value1");
		assertFalse(cache.isEmpty());
	}

	@Test void a39_isEmpty_afterClear() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		cache.put("key2", "value2");
		assertFalse(cache.isEmpty());
		cache.clear();
		assertTrue(cache.isEmpty());
	}

	@Test void a40_isEmpty_disabledCache() {
		var cache = Cache.of(String.class, String.class)
			.cacheMode(NONE)
			.build();
		cache.get("key1", () -> "value1");
		assertTrue(cache.isEmpty(), "Disabled cache should always be empty");
	}

	//====================================================================================================
	// containsKey() method
	//====================================================================================================

	@Test void a41_containsKey_notPresent() {
		var cache = Cache.of(String.class, String.class).build();
		assertFalse(cache.containsKey("key1"));
	}

	@Test void a42_containsKey_afterPut() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		assertTrue(cache.containsKey("key1"));
		assertFalse(cache.containsKey("key2"));
	}

	@Test void a43_containsKey_afterGet() {
		var cache = Cache.of(String.class, String.class).build();
		cache.get("key1", () -> "value1");
		assertTrue(cache.containsKey("key1"));
	}

	@Test void a44_containsKey_afterClear() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		assertTrue(cache.containsKey("key1"));
		cache.clear();
		assertFalse(cache.containsKey("key1"));
	}

	@Test void a45_containsKey_nullKey() {
		var cache = Cache.of(String.class, String.class).build();
		// Null keys are now cached, so containsKey should return true after get()
		cache.get(null, () -> "value");
		assertTrue(cache.containsKey(null));
	}

	//====================================================================================================
	// remove() method
	//====================================================================================================

	@Test void a46_remove_existingKey() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		var removed = cache.remove("key1");
		assertEquals("value1", removed);
		assertFalse(cache.containsKey("key1"));
		assertTrue(cache.isEmpty());
	}

	@Test void a47_remove_nonExistentKey() {
		var cache = Cache.of(String.class, String.class).build();
		var removed = cache.remove("key1");
		assertNull(removed);
	}

	@Test void a48_remove_afterGet() {
		var cache = Cache.of(String.class, String.class).build();
		cache.get("key1", () -> "value1");
		var removed = cache.remove("key1");
		assertEquals("value1", removed);
		assertFalse(cache.containsKey("key1"));
	}

	@Test void a49_remove_nullKey() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put(null, "value1");
		var removed = cache.remove(null);
		assertEquals("value1", removed);
		assertFalse(cache.containsKey(null));
	}

	//====================================================================================================
	// containsValue() method
	//====================================================================================================

	@Test void a50_containsValue_present() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		cache.put("key2", "value2");
		assertTrue(cache.containsValue("value1"));
		assertTrue(cache.containsValue("value2"));
		assertFalse(cache.containsValue("value3"));
	}

	@Test void a51_containsValue_notPresent() {
		var cache = Cache.of(String.class, String.class).build();
		assertFalse(cache.containsValue("value1"));
	}

	@Test void a52_containsValue_afterRemove() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		assertTrue(cache.containsValue("value1"));
		cache.remove("key1");
		assertFalse(cache.containsValue("value1"));
	}

	@Test void a53_containsValue_afterClear() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		cache.put("key2", "value2");
		assertTrue(cache.containsValue("value1"));
		cache.clear();
		assertFalse(cache.containsValue("value1"));
		assertFalse(cache.containsValue("value2"));
	}

	@Test void a54_containsValue_nullValue() {
		var cache = Cache.of(String.class, String.class).build();
		// Null values can't be cached, so containsValue(null) should return false
		cache.get("key1", () -> null);
		assertFalse(cache.containsValue(null));
	}

	//====================================================================================================
	// Array key support
	//====================================================================================================

	@Test void a46_arrayKeys_contentBasedEquality() {
		var cache = Cache.of(String[].class, String.class).build();

		var key1 = new String[]{"a", "b", "c"};
		var key2 = new String[]{"a", "b", "c"}; // Same content, different instance

		cache.get(key1, () -> "value1");
		// Should be a cache hit even though it's a different array instance
		var result = cache.get(key2, () -> "should not be called");
		assertEquals("value1", result);
		assertEquals(1, cache.getCacheHits());
		assertSize(1, cache);
	}

	@Test void a47_arrayKeys_differentContent() {
		var cache = Cache.of(String[].class, String.class).build();

		var key1 = new String[]{"a", "b", "c"};
		var key2 = new String[]{"a", "b", "d"}; // Different content

		cache.get(key1, () -> "value1");
		var result = cache.get(key2, () -> "value2");
		assertEquals("value2", result);
		assertSize(2, cache);
	}

	@Test void a48_arrayKeys_put() {
		var cache = Cache.of(String[].class, String.class).build();

		var key1 = new String[]{"a", "b"};
		var key2 = new String[]{"a", "b"}; // Same content

		cache.put(key1, "value1");
		assertTrue(cache.containsKey(key2));
		assertEquals("value1", cache.get(key2, () -> "should not be called"));
	}

	//====================================================================================================
	// Null value handling
	//====================================================================================================

	@Test void a49_nullValue_notCached() {
		var cache = Cache.of(String.class, String.class).build();
		var callCount = new AtomicInteger();

		// Supplier returns null - should not be cached
		var result1 = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return null;
		});
		assertNull(result1);
		assertEquals(1, callCount.get());

		// Second call should invoke supplier again (not cached)
		var result2 = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return null;
		});
		assertNull(result2);
		assertEquals(2, callCount.get());
		assertTrue(cache.isEmpty(), "Null values should not be cached");
	}

	@Test void a50_nullValue_afterPut() {
		var cache = Cache.of(String.class, String.class).build();
		cache.put("key1", "value1");
		cache.put("key1", null);
		// After putting null, the key should be removed
		var callCount = new AtomicInteger();
		var result = cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "supplied";
		});
		assertEquals("supplied", result);
		assertEquals(1, callCount.get());
	}

	//====================================================================================================
	// create() static method
	//====================================================================================================

	@Test void a51_create_basic() {
		var cache = Cache.<String, String>create()
			.supplier(k -> "value-" + k)
			.build();

		var result = cache.get("test");
		assertEquals("value-test", result);
		assertSize(1, cache);
	}

	@Test void a52_create_withConfiguration() {
		var cache = Cache.<String, Integer>create()
			.maxSize(50)
			.cacheMode(WEAK)
			.supplier(k -> k.length())
			.build();

		var result = cache.get("hello");
		assertEquals(5, result);
		assertSize(1, cache);
	}

	//====================================================================================================
	// disableCaching() builder method
	//====================================================================================================

	@Test void a53_disableCaching() {
		var cache = Cache.of(String.class, String.class)
			.cacheMode(NONE)
			.build();

		var callCount = new AtomicInteger();
		cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "value1";
		});
		cache.get("key1", () -> {
			callCount.incrementAndGet();
			return "value1";
		});

		assertEquals(2, callCount.get(), "Supplier should be called every time when disabled");
		assertTrue(cache.isEmpty());
		assertEquals(0, cache.getCacheHits());
	}

	//====================================================================================================
	// Thread-local cache mode
	//====================================================================================================

	@Test void a54_threadLocal_basicCaching() throws Exception {
		var cache = Cache.of(String.class, String.class)
			.threadLocal()
			.build();
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
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a55_threadLocal_eachThreadHasOwnCache() throws Exception {
		var cache = Cache.of(String.class, String.class)
			.threadLocal()
			.build();
		var executor = Executors.newFixedThreadPool(2);
		var threadValues = new ConcurrentHashMap<Thread, String>();

		// Each thread caches "key1" with its own value
		var future1 = CompletableFuture.runAsync(() -> {
			var value = cache.get("key1", () -> "thread1-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		var future2 = CompletableFuture.runAsync(() -> {
			var value = cache.get("key1", () -> "thread2-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

		// Verify both threads cached their own values
		assertEquals(2, threadValues.size());
		assertTrue(threadValues.containsValue("thread1-value"));
		assertTrue(threadValues.containsValue("thread2-value"));

		// Verify each thread's cache is independent - same thread should get same cached value
		var threadValues2 = new ConcurrentHashMap<Thread, String>();
		var threads = new java.util.ArrayList<Thread>(threadValues.keySet());

		future1 = CompletableFuture.runAsync(() -> {
			var value = cache.get("key1", () -> "should-not-be-called");
			threadValues2.put(Thread.currentThread(), value);
		}, executor);

		future2 = CompletableFuture.runAsync(() -> {
			var value = cache.get("key1", () -> "should-not-be-called");
			threadValues2.put(Thread.currentThread(), value);
		}, executor);

		CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

		// Each thread should get its own cached value (same as what it cached before)
		for (var thread : threads) {
			if (threadValues2.containsKey(thread)) {
				assertEquals(threadValues.get(thread), threadValues2.get(thread),
					"Thread " + thread + " should get its own cached value");
			}
		}

		executor.shutdown();
	}

	@Test void a56_threadLocal_multipleKeys() {
		var cache = Cache.of(String.class, Integer.class)
			.threadLocal()
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);

		assertSize(3, cache);
		assertEquals(0, cache.getCacheHits());

		// Verify all cached
		assertEquals(1, cache.get("one", () -> 999));
		assertEquals(2, cache.get("two", () -> 999));
		assertEquals(3, cache.get("three", () -> 999));
		assertEquals(3, cache.getCacheHits());
	}

	@Test void a57_threadLocal_clear() {
		var cache = Cache.of(String.class, Integer.class)
			.threadLocal()
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		assertSize(2, cache);

		cache.clear();
		assertEmpty(cache);
	}

	@Test void a58_threadLocal_maxSize() {
		var cache = Cache.of(String.class, Integer.class)
			.threadLocal()
			.maxSize(3)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);
		assertSize(3, cache);

		// 4th item doesn't trigger eviction yet
		cache.get("four", () -> 4);
		assertSize(4, cache);

		// 5th item triggers eviction
		cache.get("five", () -> 5);
		assertSize(1, cache);
	}

	@Test void a59_threadLocal_cacheHits() {
		var cache = Cache.of(String.class, Integer.class)
			.threadLocal()
			.build();

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

	//====================================================================================================
	// Thread-local + weak mode combination
	//====================================================================================================

	@Test void a60_threadLocal_weakMode_basicCaching() {
		var cache = Cache.of(String.class, String.class)
			.threadLocal()
			.cacheMode(WEAK)
			.build();
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
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, cache);
		assertEquals(1, cache.getCacheHits());
	}

	@Test void a61_threadLocal_weakMode_eachThreadHasOwnCache() throws Exception {
		var cache = Cache.of(String.class, String.class)
			.threadLocal()
			.cacheMode(WEAK)
			.build();
		var executor = Executors.newFixedThreadPool(2);
		var threadValues = new ConcurrentHashMap<Thread, String>();

		// Each thread caches "key1" with its own value
		var future1 = CompletableFuture.runAsync(() -> {
			var value = cache.get("key1", () -> "thread1-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		var future2 = CompletableFuture.runAsync(() -> {
			var value = cache.get("key1", () -> "thread2-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS);

		// Verify both threads cached their own values
		assertEquals(2, threadValues.size());
		assertTrue(threadValues.containsValue("thread1-value"));
		assertTrue(threadValues.containsValue("thread2-value"));

		executor.shutdown();
	}

	@Test void a62_threadLocal_weakMode_clear() {
		var cache = Cache.of(String.class, Integer.class)
			.threadLocal()
			.cacheMode(WEAK)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		assertSize(2, cache);

		cache.clear();
		assertEmpty(cache);
	}

	@Test void a63_threadLocal_weakMode_maxSize() {
		var cache = Cache.of(String.class, Integer.class)
			.threadLocal()
			.cacheMode(WEAK)
			.maxSize(3)
			.build();

		cache.get("one", () -> 1);
		cache.get("two", () -> 2);
		cache.get("three", () -> 3);
		assertSize(3, cache);

		// 4th item doesn't trigger eviction yet
		cache.get("four", () -> 4);
		assertSize(4, cache);

		// 5th item triggers eviction
		cache.get("five", () -> 5);
		assertSize(1, cache);
	}
}

