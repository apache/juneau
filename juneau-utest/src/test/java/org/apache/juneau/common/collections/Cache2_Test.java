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
		assertEquals(1, x.size());
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
		assertEquals(1, x.size());
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
		assertEquals(3, x.size());
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
		assertEquals(1, x.size());
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
			.supplier((k1, k2) -> "value")
			.build();

		assertThrows(IllegalArgumentException.class, () -> x.get(null, 123));
		assertThrows(IllegalArgumentException.class, () -> x.get("user", null));
		assertThrows(IllegalArgumentException.class, () -> x.get(null, null));
	}

	@Test
	void c02_nullKeys_overrideSupplier() {
		var x = Cache2.of(String.class, Integer.class, String.class).build();

		assertThrows(IllegalArgumentException.class, () -> x.get(null, 123, () -> "value"));
		assertThrows(IllegalArgumentException.class, () -> x.get("user", null, () -> "value"));
		assertThrows(IllegalArgumentException.class, () -> x.get(null, null, () -> "value"));
	}

	//====================================================================================================
	// d - Disabled caching
	//====================================================================================================

	@Test
	void d01_disableCaching_alwaysCallsSupplier() {
		var defaultCallCount = new AtomicInteger();
		var overrideCallCount = new AtomicInteger();
		var x = Cache2.of(String.class, Integer.class, String.class)
			.disableCaching()
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

		assertTrue(x.isEmpty()); // Nothing cached
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
		assertEquals(2, x.size());

		// Adding a third entry doesn't exceed maxSize (2 > 2 is false)
		x.get("k3", 3);
		assertEquals(3, x.size());

		// Fourth entry triggers clear (3 > 2 is true)
		x.get("k4", 4);
		assertEquals(1, x.size()); // Only the new entry
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
		assertEquals(2, x.size());

		x.clear();

		assertTrue(x.isEmpty());
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
		assertEquals(1, x.size());
	}
}

