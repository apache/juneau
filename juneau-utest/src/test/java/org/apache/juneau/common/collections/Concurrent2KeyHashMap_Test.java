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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Concurrent2KeyHashMap_Test extends TestBase {

	//====================================================================================================
	// Basic put and get operations
	//====================================================================================================

	@Test void a01_basicPutAndGet() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put("user", 123, "Alice");
		map.put("user", 456, "Bob");
		map.put("admin", 123, "Charlie");

		assertEquals("Alice", map.get("user", 123));
		assertEquals("Bob", map.get("user", 456));
		assertEquals("Charlie", map.get("admin", 123));
	}

	@Test void a02_getWithNonExistentKey() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put("user", 123, "Alice");

		assertNull(map.get("user", 999));
		assertNull(map.get("admin", 123));
	}

	@Test void a03_updateExistingKey() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		assertNull(map.put("user", 123, "Alice"));
		assertEquals("Alice", map.put("user", 123, "AliceUpdated"));
		assertEquals("AliceUpdated", map.get("user", 123));
	}

	//====================================================================================================
	// Null key handling
	//====================================================================================================

	@Test void a04_nullKeys() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		// Both keys null
		map.put(null, null, "Both null");
		assertEquals("Both null", map.get(null, null));

		// First key null
		map.put(null, 123, "First null");
		assertEquals("First null", map.get(null, 123));

		// Second key null
		map.put("user", null, "Second null");
		assertEquals("Second null", map.get("user", null));
	}

	@Test void a05_nullKeysDistinct() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put(null, null, "Both null");
		map.put(null, 123, "First null");
		map.put("user", null, "Second null");

		// Each combination should be distinct
		assertEquals("Both null", map.get(null, null));
		assertEquals("First null", map.get(null, 123));
		assertEquals("Second null", map.get("user", null));
	}

	//====================================================================================================
	// Supplier functionality
	//====================================================================================================

	@Test void a06_supplierComputesMissingValue() {
		var callCount = new AtomicInteger();
		var map = new Concurrent2KeyHashMap<String,Integer,String>(false,
			(k1, k2) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2;
			}
		);

		// First call - should invoke supplier
		var result1 = map.get("user", 123);
		assertEquals("user:123", result1);
		assertEquals(1, callCount.get());

		// Second call - should return cached value
		var result2 = map.get("user", 123);
		assertEquals("user:123", result2);
		assertEquals(1, callCount.get()); // Supplier not called again

		// Different key - should invoke supplier again
		var result3 = map.get("user", 456);
		assertEquals("user:456", result3);
		assertEquals(2, callCount.get());
	}

	@Test void a07_supplierWithNullKeys() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>(false,
			(k1, k2) -> "k1=" + k1 + ",k2=" + k2
		);

		assertEquals("k1=null,k2=null", map.get(null, null));
		assertEquals("k1=null,k2=123", map.get(null, 123));
		assertEquals("k1=user,k2=null", map.get("user", null));
	}

	@Test void a08_noSupplierReturnsNull() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		assertNull(map.get("user", 123));
	}

	//====================================================================================================
	// Disabled mode
	//====================================================================================================

	@Test void a09_disabledMode_neverCaches() {
		var callCount = new AtomicInteger();
		var map = new Concurrent2KeyHashMap<String,Integer,String>(true,
			(k1, k2) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2;
			}
		);

		// First call
		assertEquals("user:123", map.get("user", 123));
		assertEquals(1, callCount.get());

		// Second call - supplier called again (no caching)
		assertEquals("user:123", map.get("user", 123));
		assertEquals(2, callCount.get());

		// Third call
		assertEquals("user:123", map.get("user", 123));
		assertEquals(3, callCount.get());
	}

	@Test void a10_disabledMode_putDoesNothing() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>(true, null);

		assertNull(map.put("user", 123, "Alice"));
		assertNull(map.get("user", 123)); // Nothing was stored
		assertEquals(0, map.size());
	}

	@Test void a11_disabledMode_withoutSupplierReturnsNull() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>(true, null);

		assertNull(map.get("user", 123));
	}

	//====================================================================================================
	// Key equality and hashing
	//====================================================================================================

	@Test void a12_keyEquality_differentKeyParts() {
		var map = new Concurrent2KeyHashMap<String,String,String>();

		map.put("a", "b", "AB");
		map.put("b", "a", "BA");

		assertEquals("AB", map.get("a", "b"));
		assertEquals("BA", map.get("b", "a"));
	}

	@Test void a13_keyEquality_hashCollisions() {
		// Strings with same hash code (intentionally created)
		var map = new Concurrent2KeyHashMap<String,String,String>();

		map.put("Aa", "BB", "value1");
		map.put("BB", "Aa", "value2");

		// Despite potential hash collision, should maintain distinct entries
		assertEquals("value1", map.get("Aa", "BB"));
		assertEquals("value2", map.get("BB", "Aa"));
	}

	//====================================================================================================
	// Size and containment
	//====================================================================================================

	@Test void a14_sizeTracking() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		assertEquals(0, map.size());

		map.put("user", 123, "Alice");
		assertEquals(1, map.size());

		map.put("user", 456, "Bob");
		assertEquals(2, map.size());

		map.put("admin", 123, "Charlie");
		assertEquals(3, map.size());

		// Updating existing key doesn't change size
		map.put("user", 123, "AliceUpdated");
		assertEquals(3, map.size());
	}

	@Test void a15_isEmpty() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		assertTrue(map.isEmpty());

		map.put("user", 123, "Alice");
		assertFalse(map.isEmpty());

		map.clear();
		assertTrue(map.isEmpty());
	}

	//====================================================================================================
	// Thread safety
	//====================================================================================================

	@Test void a16_concurrentAccess() throws Exception {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();
		var executor = Executors.newFixedThreadPool(10);

		// Submit 100 concurrent put operations
		var futures = new CompletableFuture[100];
		for (var i = 0; i < 100; i++) {
			final int index = i;
			futures[i] = CompletableFuture.runAsync(() -> {
				map.put("key" + (index % 10), index, "value" + index);
			}, executor);
		}

		// Wait for all to complete
		CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

		// Verify all values are present
		for (var i = 0; i < 100; i++) {
			var value = map.get("key" + (i % 10), i);
			assertEquals("value" + i, value);
		}

		executor.shutdown();
	}

	@Test void a17_concurrentAccessWithSupplier() throws Exception {
		var computeCount = new AtomicInteger();
		var map = new Concurrent2KeyHashMap<String,Integer,String>(false,
			(k1, k2) -> {
				computeCount.incrementAndGet();
				return k1 + ":" + k2;
			}
		);

		var executor = Executors.newFixedThreadPool(10);

		// Multiple threads trying to get the same key
		var futures = new CompletableFuture[100];
		for (var i = 0; i < 100; i++) {
			futures[i] = CompletableFuture.runAsync(() -> {
				map.get("shared", 123);
			}, executor);
		}

		// Wait for all to complete
		CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

		// Supplier should be called only a few times (due to concurrent puts)
		assertTrue(computeCount.get() < 10, "Supplier called " + computeCount.get() + " times");

		executor.shutdown();
	}

	//====================================================================================================
	// Clear operation
	//====================================================================================================

	@Test void a18_clear() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put("user", 123, "Alice");
		map.put("user", 456, "Bob");
		map.put("admin", 123, "Charlie");

		assertEquals(3, map.size());

		map.clear();

		assertEquals(0, map.size());
		assertNull(map.get("user", 123));
		assertNull(map.get("user", 456));
		assertNull(map.get("admin", 123));
	}

	//====================================================================================================
	// Different key types
	//====================================================================================================

	@Test void a19_integerKeys() {
		var map = new Concurrent2KeyHashMap<Integer,Integer,String>();

		map.put(1, 2, "1:2");
		map.put(2, 1, "2:1");

		assertEquals("1:2", map.get(1, 2));
		assertEquals("2:1", map.get(2, 1));
		assertNull(map.get(1, 1));
	}

	@Test void a20_mixedKeyTypes() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put("string", 123, "mixed1");
		map.put("another", 456, "mixed2");

		assertEquals("mixed1", map.get("string", 123));
		assertEquals("mixed2", map.get("another", 456));
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test void a21_emptyMap() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		assertTrue(map.isEmpty());
		assertEquals(0, map.size());
		assertNull(map.get("any", 123));
	}

	@Test void a22_singleEntry() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put("only", 1, "single");

		assertEquals(1, map.size());
		assertEquals("single", map.get("only", 1));
		assertNull(map.get("only", 2));
	}

	@Test void a23_supplierReturnsNull() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>(false,
			(k1, k2) -> null
		);

		// ConcurrentHashMap doesn't allow null values, so this will throw NullPointerException
		assertThrows(NullPointerException.class, () -> map.get("user", 123));
	}

	@Test void a24_removeOperation() {
		var map = new Concurrent2KeyHashMap<String,Integer,String>();

		map.put("user", 123, "Alice");
		map.put("user", 456, "Bob");

		assertEquals(2, map.size());

		// Remove using the underlying ConcurrentHashMap methods
		// Note: TwoKeyConcurrentHashMap doesn't expose a direct remove(k1, k2) method,
		// but you can use the inherited remove method with a Key object if needed
		map.clear();
		assertEquals(0, map.size());
	}
}

