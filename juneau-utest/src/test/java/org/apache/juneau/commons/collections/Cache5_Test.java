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

import static org.junit.jupiter.api.Assertions.*;
import static org.apache.juneau.commons.collections.CacheMode.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Cache5_Test extends TestBase {

	//====================================================================================================
	// a - Basic cache operations
	//====================================================================================================

	@Test
	void a01_defaultSupplier_basic() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			})
			.build();

		var result1 = x.get("en", "US", "west", "formal", 1);
		var result2 = x.get("en", "US", "west", "formal", 1); // Cache hit

		assertEquals("en:US:west:formal:1", result1);
		assertEquals("en:US:west:formal:1", result2);
		assertEquals(1, callCount.get());
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a02_overrideSupplier() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4, k5) -> "DEFAULT")
			.build();

		var result = x.get("en", "US", "west", "formal", 1, () -> "OVERRIDE");

		assertEquals("OVERRIDE", result);
	}

	@Test
	void a03_nullKeys() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4, k5) -> "value-" + k1 + "-" + k2 + "-" + k3 + "-" + k4 + "-" + k5)
			.build();

		// Null keys are now allowed
		assertEquals("value-null-US-west-formal-1", x.get(null, "US", "west", "formal", 1));
		assertEquals("value-en-null-west-formal-1", x.get("en", null, "west", "formal", 1));
		assertEquals("value-en-US-null-formal-1", x.get("en", "US", null, "formal", 1));
		assertEquals("value-en-US-west-null-1", x.get("en", "US", "west", null, 1));
		assertEquals("value-en-US-west-formal-null", x.get("en", "US", "west", "formal", null));
		assertEquals("value-null-null-null-null-null", x.get(null, null, null, null, null));

		// Cached values should be returned on subsequent calls
		assertEquals("value-null-US-west-formal-1", x.get(null, "US", "west", "formal", 1));
		assertEquals("value-en-null-west-formal-1", x.get("en", null, "west", "formal", 1));
	}

	@Test
	void a04_disableCaching() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.cacheMode(NONE)
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return "value";
			})
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("en", "US", "west", "formal", 1);

		assertEquals(2, callCount.get()); // Called twice
		assertEmpty(x);
	}

	@Test
	void a04b_weakMode_basicCaching() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("en", "US", "west", "formal", 1);

		// Second call - cache hit
		var result2 = x.get("en", "US", "west", "formal", 1);

		assertEquals("en:US:west:formal:1", result1);
		assertEquals("en:US:west:formal:1", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a04c_weakMode_multipleKeys() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2, k3, k4, k5) -> k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5)
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "north", "formal", 2);
		x.get("de", "DE", "south", "formal", 3);

		assertSize(3, x);
		assertEquals(0, x.getCacheHits());

		// Verify all cached
		assertEquals("en:US:west:formal:1", x.get("en", "US", "west", "formal", 1));
		assertEquals("fr:FR:north:formal:2", x.get("fr", "FR", "north", "formal", 2));
		assertEquals("de:DE:south:formal:3", x.get("de", "DE", "south", "formal", 3));
		assertEquals(3, x.getCacheHits());
	}

	@Test
	void a04d_weakMode_clear() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "north", "formal", 2);
		assertSize(2, x);

		x.clear();
		assertEmpty(x);
	}

	@Test
	void a04e_weakMode_maxSize() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.maxSize(2)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "north", "formal", 2);
		assertSize(2, x);

		// 3rd item doesn't trigger eviction yet
		x.get("de", "DE", "south", "formal", 3);
		assertSize(3, x);

		// 4th item triggers eviction
		x.get("es", "ES", "east", "formal", 4);
		assertSize(1, x);
	}

	@Test
	void a04f_weakMethod_basicCaching() {
		// Test the weak() convenience method
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.weak()
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("en", "US", "west", "formal", 1);

		// Second call - cache hit
		var result2 = x.get("en", "US", "west", "formal", 1);

		assertEquals("en:US:west:formal:1", result1);
		assertEquals("en:US:west:formal:1", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a04g_weakMethod_chaining() {
		// Test that weak() can be chained with other builder methods
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.weak()
			.maxSize(100)
			.supplier((k1, k2, k3, k4, k5) -> k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5)
			.build();

		var result = x.get("en", "US", "west", "formal", 1);
		assertEquals("en:US:west:formal:1", result);
		assertSize(1, x);
	}

	@Test
	void a05_maxSize() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.maxSize(2)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "north", "formal", 2);
		assertSize(2, x);

		x.get("de", "DE", "south", "formal", 3); // Doesn't exceed yet
		assertSize(3, x);

		x.get("es", "ES", "east", "formal", 4); // Exceeds max (3 > 2), triggers clear
		assertSize(1, x); // Cleared
	}

	@Test
	void a06_cacheHitsTracking() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1); // Miss
		assertEquals(0, x.getCacheHits());

		x.get("en", "US", "west", "formal", 1); // Hit
		x.get("en", "US", "west", "formal", 1); // Hit
		assertEquals(2, x.getCacheHits());
	}

	//====================================================================================================
	// b - put(), isEmpty(), containsKey()
	//====================================================================================================

	@Test
	void b01_put() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		var previous = x.put("en", "US", "west", "formal", 1, "value");
		assertNull(previous);
		assertEquals("value", x.get("en", "US", "west", "formal", 1, () -> "should not be called"));
	}

	@Test
	void b01b_put_withNullValue() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		x.put("en", "US", "west", "formal", 1, "value1");
		var previous = x.put("en", "US", "west", "formal", 1, null);
		assertEquals("value1", previous);
		assertFalse(x.containsKey("en", "US", "west", "formal", 1));
	}

	@Test
	void b02_isEmpty() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		assertTrue(x.isEmpty());
		x.put("en", "US", "west", "formal", 1, "value");
		assertFalse(x.isEmpty());
		x.clear();
		assertTrue(x.isEmpty());
	}

	@Test
	void b03_containsKey() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		assertFalse(x.containsKey("en", "US", "west", "formal", 1));
		x.put("en", "US", "west", "formal", 1, "value");
		assertTrue(x.containsKey("en", "US", "west", "formal", 1));
	}

	//====================================================================================================
	// c - create(), disableCaching(), null values
	//====================================================================================================

	@Test
	void c01_create() {
		var x = Cache5.<String, String, String, String, Integer, String>create()
			.supplier((k1, k2, k3, k4, k5) -> k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5)
			.build();
		var result = x.get("en", "US", "west", "formal", 1);
		assertEquals("en:US:west:formal:1", result);
	}

	@Test
	void c02_disableCaching() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.cacheMode(NONE)
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return "value";
			})
			.build();
		x.get("en", "US", "west", "formal", 1);
		x.get("en", "US", "west", "formal", 1);
		assertEquals(2, callCount.get());
		assertTrue(x.isEmpty());
	}

	@Test
	void c03_nullValue_notCached() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		x.get("en", "US", "west", "formal", 1, () -> {
			callCount.incrementAndGet();
			return null;
		});
		x.get("en", "US", "west", "formal", 1, () -> {
			callCount.incrementAndGet();
			return null;
		});
		assertEquals(2, callCount.get());
		assertTrue(x.isEmpty());
	}

	//====================================================================================================
	// d - remove() and containsValue()
	//====================================================================================================

	@Test
	void d01_remove() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		x.put("en", "US", "west", "formal", 1, "value1");
		var removed = x.remove("en", "US", "west", "formal", 1);
		assertEquals("value1", removed);
		assertFalse(x.containsKey("en", "US", "west", "formal", 1));
	}

	@Test
	void d02_containsValue() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		x.put("en", "US", "west", "formal", 1, "value1");
		assertTrue(x.containsValue("value1"));
		assertFalse(x.containsValue("value2"));
	}

	@Test
	void d03_containsValue_nullValue() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		// Null values can't be cached, so containsValue(null) should return false
		x.get("en", "US", "west", "formal", 1, () -> null);
		assertFalse(x.containsValue(null));
		// Also test with empty cache
		var x2 = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class).build();
		assertFalse(x2.containsValue(null));
	}

	//====================================================================================================
	// e - logOnExit() builder methods
	//====================================================================================================

	@Test
	void e01_logOnExit_withStringId() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.logOnExit("TestCache5")
			.supplier((k1, k2, k3, k4, k5) -> k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5)
			.build();
		x.get("en", "US", "west", "formal", 1);
		assertSize(1, x);
	}

	@Test
	void e02_logOnExit_withBoolean() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.logOnExit(true, "MyCache5")
			.supplier((k1, k2, k3, k4, k5) -> k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5)
			.build();
		x.get("en", "US", "west", "formal", 1);
		assertSize(1, x);
	}

	//====================================================================================================
	// f - Thread-local cache mode
	//====================================================================================================

	@Test
	void f01_threadLocal_basicCaching() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("en", "US", "west", "formal", 1);

		// Second call - cache hit
		var result2 = x.get("en", "US", "west", "formal", 1);

		assertEquals("en:US:west:formal:1", result1);
		assertEquals("en:US:west:formal:1", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void f02_threadLocal_eachThreadHasOwnCache() throws Exception {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.build();
		var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		var threadValues = new java.util.concurrent.ConcurrentHashMap<Thread, String>();

		// Each thread caches ("en", "US", "west", "formal", 1) with its own value
		var future1 = java.util.concurrent.CompletableFuture.runAsync(() -> {
			var value = x.get("en", "US", "west", "formal", 1, () -> "thread1-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		var future2 = java.util.concurrent.CompletableFuture.runAsync(() -> {
			var value = x.get("en", "US", "west", "formal", 1, () -> "thread2-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		java.util.concurrent.CompletableFuture.allOf(future1, future2).get(5, java.util.concurrent.TimeUnit.SECONDS);

		// Verify both threads cached their own values
		assertEquals(2, threadValues.size());
		assertTrue(threadValues.containsValue("thread1-value"));
		assertTrue(threadValues.containsValue("thread2-value"));

		executor.shutdown();
	}

	@Test
	void f03_threadLocal_multipleKeys() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.supplier((k1, k2, k3, k4, k5) -> k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5)
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "east", "informal", 2);
		x.get("de", "DE", "north", "formal", 3);

		assertSize(3, x);
		assertEquals(0, x.getCacheHits());

		// Verify all cached
		assertEquals("en:US:west:formal:1", x.get("en", "US", "west", "formal", 1));
		assertEquals("fr:FR:east:informal:2", x.get("fr", "FR", "east", "informal", 2));
		assertEquals("de:DE:north:formal:3", x.get("de", "DE", "north", "formal", 3));
		assertEquals(3, x.getCacheHits());
	}

	@Test
	void f04_threadLocal_clear() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "east", "informal", 2);
		assertSize(2, x);

		x.clear();
		assertEmpty(x);
	}

	@Test
	void f05_threadLocal_maxSize() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.maxSize(2)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "east", "informal", 2);
		assertSize(2, x);

		// 3rd item doesn't trigger eviction yet
		x.get("de", "DE", "north", "formal", 3);
		assertSize(3, x);

		// 4th item triggers eviction
		x.get("es", "ES", "south", "informal", 4);
		assertSize(1, x);
	}

	//====================================================================================================
	// g - Thread-local + weak mode combination
	//====================================================================================================

	@Test
	void g01_threadLocal_weakMode_basicCaching() {
		var callCount = new AtomicInteger();
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.cacheMode(WEAK)
			.supplier((k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("en", "US", "west", "formal", 1);

		// Second call - cache hit
		var result2 = x.get("en", "US", "west", "formal", 1);

		assertEquals("en:US:west:formal:1", result1);
		assertEquals("en:US:west:formal:1", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void g02_threadLocal_weakMode_eachThreadHasOwnCache() throws Exception {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.cacheMode(WEAK)
			.build();
		var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
		var threadValues = new java.util.concurrent.ConcurrentHashMap<Thread, String>();

		// Each thread caches ("en", "US", "west", "formal", 1) with its own value
		var future1 = java.util.concurrent.CompletableFuture.runAsync(() -> {
			var value = x.get("en", "US", "west", "formal", 1, () -> "thread1-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		var future2 = java.util.concurrent.CompletableFuture.runAsync(() -> {
			var value = x.get("en", "US", "west", "formal", 1, () -> "thread2-value");
			threadValues.put(Thread.currentThread(), value);
		}, executor);

		java.util.concurrent.CompletableFuture.allOf(future1, future2).get(5, java.util.concurrent.TimeUnit.SECONDS);

		// Verify both threads cached their own values
		assertEquals(2, threadValues.size());
		assertTrue(threadValues.containsValue("thread1-value"));
		assertTrue(threadValues.containsValue("thread2-value"));

		executor.shutdown();
	}

	@Test
	void g03_threadLocal_weakMode_clear() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.cacheMode(WEAK)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "east", "informal", 2);
		assertSize(2, x);

		x.clear();
		assertEmpty(x);
	}

	@Test
	void g04_threadLocal_weakMode_maxSize() {
		var x = Cache5.of(String.class, String.class, String.class, String.class, Integer.class, String.class)
			.threadLocal()
			.cacheMode(WEAK)
			.maxSize(2)
			.supplier((k1, k2, k3, k4, k5) -> "value")
			.build();

		x.get("en", "US", "west", "formal", 1);
		x.get("fr", "FR", "east", "informal", 2);
		assertSize(2, x);

		// 3rd item doesn't trigger eviction yet
		x.get("de", "DE", "north", "formal", 3);
		assertSize(3, x);

		// 4th item triggers eviction
		x.get("es", "ES", "south", "informal", 4);
		assertSize(1, x);
	}
}

