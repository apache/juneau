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

import static org.apache.juneau.common.collections.CacheMode.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Cache2_Test extends TestBase {

	//====================================================================================================
	// a - Basic cache operations with default supplier
	//====================================================================================================

	@Test
	void a01_defaultSupplier_cacheMiss() {
		var callCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2;
			})
			.build();

		var result = x.get("user", 123);

		assertEquals("user:123", result);
		assertEquals(1, callCount.get());
		assertSize(1, x);
		assertEquals(0, x.getCacheHits());
	}

	@Test
	void a02_defaultSupplier_cacheHit() {
		var callCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("user", 123);

		// Second call - cache hit
		var result2 = x.get("user", 123);

		assertEquals("user:123", result1);
		assertEquals("user:123", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a03_multipleKeys() {
		var callCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2;
			})
			.build();

		var v1 = x.get("user", 123);
		var v2 = x.get("admin", 456);
		var v3 = x.get("guest", 789);

		assertEquals("user:123", v1);
		assertEquals("admin:456", v2);
		assertEquals("guest:789", v3);
		assertEquals(3, callCount.get());
		assertSize(3, x);
		assertEquals(0, x.getCacheHits());
	}

	//====================================================================================================
	// b - Override supplier
	//====================================================================================================

	@Test
	void b01_overrideSupplier() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> k1 + ":" + k2) // Default
			.build();

		// Use override supplier
		var result = x.get("user", 123, () -> "OVERRIDE");

		assertEquals("OVERRIDE", result);
		assertSize(1, x);
	}

	@Test
	void b02_overrideSupplier_cachesResult() {
		var defaultCalls = new AtomicInteger();
		var overrideCalls = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> {
				defaultCalls.incrementAndGet();
				return "DEFAULT";
			})
			.build();

		// First call with override
		var result1 = x.get("user", 123, () -> {
			overrideCalls.incrementAndGet();
			return "OVERRIDE";
		});

		// Second call with default supplier - should use cached value
		var result2 = x.get("user", 123);

		assertEquals("OVERRIDE", result1);
		assertEquals("OVERRIDE", result2); // Cached value, not default
		assertEquals(0, defaultCalls.get());
		assertEquals(1, overrideCalls.get());
		assertEquals(1, x.getCacheHits());
	}

	//====================================================================================================
	// c - Null key validation
	//====================================================================================================

	@Test
	void c01_nullKeys_defaultSupplier() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> "value-" + k1 + "-" + k2)
			.build();

		// Null keys are now allowed
		assertEquals("value-null-123", x.get(null, 123));
		assertEquals("value-user-null", x.get("user", null));
		assertEquals("value-null-null", x.get(null, null));

		// Cached values should be returned on subsequent calls
		assertEquals("value-null-123", x.get(null, 123));
		assertEquals("value-user-null", x.get("user", null));
	}

	@Test
	void c02_nullKeys_overrideSupplier() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();

		// Null keys are now allowed
		assertEquals("value", x.get(null, 123, () -> "value"));
		assertEquals("value", x.get("user", null, () -> "value"));
		assertEquals("value", x.get(null, null, () -> "value"));

		// Cached values should be returned on subsequent calls
		assertEquals("value", x.get(null, 123, () -> "should-not-be-called"));
		assertEquals("value", x.get("user", null, () -> "should-not-be-called"));
	}

	//====================================================================================================
	// d - Disabled caching
	//====================================================================================================

	@Test
	void d01_disableCaching_alwaysCallsSupplier() {
		var defaultCallCount = new AtomicInteger();
		var overrideCallCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.cacheMode(NONE)
			.supplier((k1, k2) -> {
				defaultCallCount.incrementAndGet();
				return k1 + ":" + k2;
			})
			.build();

		// First call with override supplier
		assertEquals("user:123", x.get("user", 123, () -> {
			overrideCallCount.incrementAndGet();
			return "user:123";
		}));
		assertEquals(0, defaultCallCount.get());
		assertEquals(1, overrideCallCount.get());

		// Second call with override - supplier called again (no caching)
		assertEquals("user:123", x.get("user", 123, () -> {
			overrideCallCount.incrementAndGet();
			return "user:123";
		}));
		assertEquals(0, defaultCallCount.get());
		assertEquals(2, overrideCallCount.get());

		// Using default supplier
		assertEquals("user:456", x.get("user", 456));
		assertEquals(1, defaultCallCount.get());

		assertEquals("user:456", x.get("user", 456));
		assertEquals(2, defaultCallCount.get()); // Called again

		assertEmpty(x); // Nothing cached
	}

	//====================================================================================================
	// d2 - Weak cache mode
	//====================================================================================================

	@Test
	void d02_weakMode_basicCaching() {
		var callCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("user", 123);

		// Second call - cache hit
		var result2 = x.get("user", 123);

		assertEquals("user:123", result1);
		assertEquals("user:123", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void d03_weakMode_multipleKeys() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		x.get("user", 123);
		x.get("admin", 456);
		x.get("guest", 789);

		assertSize(3, x);
		assertEquals(0, x.getCacheHits());

		// Verify all cached
		assertEquals("user:123", x.get("user", 123));
		assertEquals("admin:456", x.get("admin", 456));
		assertEquals("guest:789", x.get("guest", 789));
		assertEquals(3, x.getCacheHits());
	}

	@Test
	void d04_weakMode_clear() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		x.get("user", 123);
		x.get("admin", 456);
		assertSize(2, x);

		x.clear();
		assertEmpty(x);
	}

	@Test
	void d05_weakMode_maxSize() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.maxSize(2)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		x.get("k1", 1);
		x.get("k2", 2);
		assertSize(2, x);

		// 3rd item doesn't trigger eviction yet
		x.get("k3", 3);
		assertSize(3, x);

		// 4th item triggers eviction
		x.get("k4", 4);
		assertSize(1, x);
	}

	//====================================================================================================
	// e - Max size and eviction
	//====================================================================================================

	@Test
	void e01_maxSize_clearsWhenExceeded() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.maxSize(2)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		x.get("k1", 1);
		x.get("k2", 2);
		assertSize(2, x);

		// Adding a third entry doesn't exceed maxSize (2 > 2 is false)
		x.get("k3", 3);
		assertSize(3, x);

		// Fourth entry triggers clear (3 > 2 is true)
		x.get("k4", 4);
		assertSize(1, x); // Only the new entry
	}

	//====================================================================================================
	// f - Cache hits tracking
	//====================================================================================================

	@Test
	void f01_cacheHitsTracking() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		assertEquals(0, x.getCacheHits());

		x.get("user", 123); // Miss
		assertEquals(0, x.getCacheHits());

		x.get("user", 123); // Hit
		assertEquals(1, x.getCacheHits());

		x.get("admin", 456); // Miss
		assertEquals(1, x.getCacheHits());

		x.get("user", 123); // Hit
		x.get("admin", 456); // Hit
		assertEquals(3, x.getCacheHits());
	}

	//====================================================================================================
	// g - Clear operation
	//====================================================================================================

	@Test
	void g01_clear() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		x.get("user", 123);
		x.get("admin", 456);
		assertSize(2, x);

		x.clear();

		assertEmpty(x);
	}

	//====================================================================================================
	// h - No default supplier
	//====================================================================================================

	@Test
	void h01_noDefaultSupplier_requiresOverride() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();

		assertThrows(NullPointerException.class, () -> x.get("user", 123));
	}

	@Test
	void h02_noDefaultSupplier_worksWithOverride() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();

		var result = x.get("user", 123, () -> "CUSTOM");

		assertEquals("CUSTOM", result);
		assertSize(1, x);
	}

	//====================================================================================================
	// i - put() method
	//====================================================================================================

	@Test
	void i01_put_directInsertion() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		var previous = x.put("user", 123, "value1");
		assertNull(previous);
		assertEquals("value1", x.get("user", 123, () -> "should not be called"));
		assertSize(1, x);
	}

	@Test
	void i02_put_overwritesExisting() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value1");
		var previous = x.put("user", 123, "value2");
		assertEquals("value1", previous);
		assertEquals("value2", x.get("user", 123, () -> "should not be called"));
	}

	@Test
	void i03_put_withNullValue() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value1");
		var previous = x.put("user", 123, null);
		assertEquals("value1", previous);
		assertFalse(x.containsKey("user", 123));
	}

	@Test
	void i04_put_withNullValue_newKey() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		var previous = x.put("user", 123, null);
		assertNull(previous);
		assertFalse(x.containsKey("user", 123));
		assertTrue(x.isEmpty());
	}

	//====================================================================================================
	// j - isEmpty() method
	//====================================================================================================

	@Test
	void j01_isEmpty_newCache() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		assertTrue(x.isEmpty());
	}

	@Test
	void j02_isEmpty_afterPut() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value");
		assertFalse(x.isEmpty());
	}

	@Test
	void j03_isEmpty_afterClear() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value");
		x.clear();
		assertTrue(x.isEmpty());
	}

	//====================================================================================================
	// k - containsKey() method
	//====================================================================================================

	@Test
	void k01_containsKey_notPresent() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		assertFalse(x.containsKey("user", 123));
	}

	@Test
	void k02_containsKey_afterPut() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value");
		assertTrue(x.containsKey("user", 123));
		assertFalse(x.containsKey("user", 456));
	}

	@Test
	void k03_containsKey_afterGet() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();
		x.get("user", 123);
		assertTrue(x.containsKey("user", 123));
	}

	//====================================================================================================
	// l - remove() method
	//====================================================================================================

	@Test
	void l02_remove_existingKey() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value1");
		var removed = x.remove("user", 123);
		assertEquals("value1", removed);
		assertFalse(x.containsKey("user", 123));
	}

	@Test
	void l03_remove_nonExistentKey() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		var removed = x.remove("user", 123);
		assertNull(removed);
	}

	//====================================================================================================
	// m - containsValue() method
	//====================================================================================================

	@Test
	void m02_containsValue_present() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		x.put("user", 123, "value1");
		x.put("admin", 456, "value2");
		assertTrue(x.containsValue("value1"));
		assertTrue(x.containsValue("value2"));
		assertFalse(x.containsValue("value3"));
	}

	@Test
	void m03_containsValue_notPresent() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();
		assertFalse(x.containsValue("value1"));
	}

	//====================================================================================================
	// n - logOnExit() builder methods
	//====================================================================================================

	@Test
	void n02_logOnExit_withStringId() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.logOnExit("TestCache2")
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();
		x.get("user", 123);
		assertSize(1, x);
	}

	@Test
	void n03_logOnExit_withBooleanTrue() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.logOnExit(true, "MyCache2")
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();
		x.get("user", 123);
		assertSize(1, x);
	}

	@Test
	void n04_logOnExit_withBooleanFalse() {
		var x = Cache2.of(String.class, Integer.class, String.class)
			.logOnExit(false, "DisabledCache2")
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();
		x.get("user", 123);
		assertSize(1, x);
	}

	//====================================================================================================
	// l - create() static method
	//====================================================================================================

	@Test
	void l01_create_basic() {
		var x = Cache2.<String, Integer, String>create()
			.supplier((k1, k2) -> k1 + ":" + k2)
			.build();

		var result = x.get("user", 123);
		assertEquals("user:123", result);
		assertSize(1, x);
	}

	//====================================================================================================
	// m - disableCaching() builder method
	//====================================================================================================

	@Test
	void m01_disableCaching() {
		var callCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.cacheMode(NONE)
			.supplier((k1, k2) -> {
				callCount.incrementAndGet();
				return "value";
			})
			.build();

		x.get("user", 123);
		x.get("user", 123);
		assertEquals(2, callCount.get());
		assertTrue(x.isEmpty());
	}

	//====================================================================================================
	// n - Null value handling
	//====================================================================================================

	@Test
	void n01_nullValue_notCached() {
		var callCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class).build();

		var result1 = x.get("user", 123, () -> {
			callCount.incrementAndGet();
			return null;
		});
		assertNull(result1);

		var result2 = x.get("user", 123, () -> {
			callCount.incrementAndGet();
			return null;
		});
		assertNull(result2);
		assertEquals(2, callCount.get());
		assertTrue(x.isEmpty());
	}
}

