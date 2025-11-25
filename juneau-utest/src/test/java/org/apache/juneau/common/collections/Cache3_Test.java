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

class Cache3_Test extends TestBase {

	//====================================================================================================
	// a - Basic cache operations
	//====================================================================================================

	@Test
	void a01_defaultSupplier_basic() {
		var callCount = new AtomicInteger();
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3;
			})
			.build();

		var result1 = x.get("en", "US", 1);
		var result2 = x.get("en", "US", 1); // Cache hit

		assertEquals("en:US:1", result1);
		assertEquals("en:US:1", result2);
		assertEquals(1, callCount.get());
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a02_overrideSupplier() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3) -> "DEFAULT")
			.build();

		var result = x.get("en", "US", 1, () -> "OVERRIDE");

		assertEquals("OVERRIDE", result);
	}

	@Test
	void a03_nullKeys() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3) -> "value-" + k1 + "-" + k2 + "-" + k3)
			.build();

		// Null keys are now allowed
		assertEquals("value-null-US-1", x.get(null, "US", 1));
		assertEquals("value-en-null-1", x.get("en", null, 1));
		assertEquals("value-en-US-null", x.get("en", "US", null));
		assertEquals("value-null-null-null", x.get(null, null, null));

		// Cached values should be returned on subsequent calls
		assertEquals("value-null-US-1", x.get(null, "US", 1));
		assertEquals("value-en-null-1", x.get("en", null, 1));
	}

	@Test
	void a04_disableCaching() {
		var callCount = new AtomicInteger();
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.cacheMode(NONE)
			.supplier((k1, k2, k3) -> {
				callCount.incrementAndGet();
				return "value";
			})
			.build();

		x.get("en", "US", 1);
		x.get("en", "US", 1);

		assertEquals(2, callCount.get()); // Called twice
		assertEmpty(x);
	}

	@Test
	void a04b_weakMode_basicCaching() {
		var callCount = new AtomicInteger();
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2, k3) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3;
			})
			.build();

		// First call - cache miss
		var result1 = x.get("en", "US", 1);

		// Second call - cache hit
		var result2 = x.get("en", "US", 1);

		assertEquals("en:US:1", result1);
		assertEquals("en:US:1", result2);
		assertSame(result1, result2);
		assertEquals(1, callCount.get()); // Supplier only called once
		assertSize(1, x);
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a04c_weakMode_multipleKeys() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2, k3) -> k1 + ":" + k2 + ":" + k3)
			.build();

		x.get("en", "US", 1);
		x.get("fr", "FR", 2);
		x.get("de", "DE", 3);

		assertSize(3, x);
		assertEquals(0, x.getCacheHits());

		// Verify all cached
		assertEquals("en:US:1", x.get("en", "US", 1));
		assertEquals("fr:FR:2", x.get("fr", "FR", 2));
		assertEquals("de:DE:3", x.get("de", "DE", 3));
		assertEquals(3, x.getCacheHits());
	}

	@Test
	void a04d_weakMode_clear() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.supplier((k1, k2, k3) -> "value")
			.build();

		x.get("en", "US", 1);
		x.get("fr", "FR", 2);
		assertSize(2, x);

		x.clear();
		assertEmpty(x);
	}

	@Test
	void a04e_weakMode_maxSize() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.cacheMode(WEAK)
			.maxSize(2)
			.supplier((k1, k2, k3) -> "value")
			.build();

		x.get("en", "US", 1);
		x.get("fr", "FR", 2);
		assertSize(2, x);

		// 3rd item doesn't trigger eviction yet
		x.get("de", "DE", 3);
		assertSize(3, x);

		// 4th item triggers eviction
		x.get("es", "ES", 4);
		assertSize(1, x);
	}

	@Test
	void a05_maxSize() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.maxSize(2)
			.supplier((k1, k2, k3) -> "value")
			.build();

		x.get("en", "US", 1);
		x.get("fr", "FR", 2);
		assertSize(2, x);

		x.get("de", "DE", 3);
		assertSize(3, x);

		x.get("es", "ES", 4); // This exceeds max (3 > 2)
		assertSize(1, x); // Cleared
	}

	@Test
	void a06_cacheHitsTracking() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3) -> "value")
			.build();

		x.get("en", "US", 1); // Miss
		assertEquals(0, x.getCacheHits());

		x.get("en", "US", 1); // Hit
		x.get("en", "US", 1); // Hit
		assertEquals(2, x.getCacheHits());
	}

	//====================================================================================================
	// b - put(), isEmpty(), containsKey()
	//====================================================================================================

	@Test
	void b01_put() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		var previous = x.put("en", "US", 1, "value");
		assertNull(previous);
		assertEquals("value", x.get("en", "US", 1, () -> "should not be called"));
	}

	@Test
	void b01b_put_withNullValue() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		x.put("en", "US", 1, "value1");
		var previous = x.put("en", "US", 1, null);
		assertEquals("value1", previous);
		assertFalse(x.containsKey("en", "US", 1));
	}

	@Test
	void b02_isEmpty() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		assertTrue(x.isEmpty());
		x.put("en", "US", 1, "value");
		assertFalse(x.isEmpty());
		x.clear();
		assertTrue(x.isEmpty());
	}

	@Test
	void b03_containsKey() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		assertFalse(x.containsKey("en", "US", 1));
		x.put("en", "US", 1, "value");
		assertTrue(x.containsKey("en", "US", 1));
		assertFalse(x.containsKey("fr", "FR", 2));
	}

	//====================================================================================================
	// c - create(), disableCaching(), null values
	//====================================================================================================

	@Test
	void c01_create() {
		var x = Cache3.<String, String, Integer, String>create()
			.supplier((k1, k2, k3) -> k1 + ":" + k2 + ":" + k3)
			.build();
		var result = x.get("en", "US", 1);
		assertEquals("en:US:1", result);
	}

	@Test
	void c02_disableCaching() {
		var callCount = new AtomicInteger();
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.cacheMode(NONE)
			.supplier((k1, k2, k3) -> {
				callCount.incrementAndGet();
				return "value";
			})
			.build();
		x.get("en", "US", 1);
		x.get("en", "US", 1);
		assertEquals(2, callCount.get());
		assertTrue(x.isEmpty());
	}

	@Test
	void c03_nullValue_notCached() {
		var callCount = new AtomicInteger();
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		x.get("en", "US", 1, () -> {
			callCount.incrementAndGet();
			return null;
		});
		x.get("en", "US", 1, () -> {
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
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		x.put("en", "US", 1, "value1");
		var removed = x.remove("en", "US", 1);
		assertEquals("value1", removed);
		assertFalse(x.containsKey("en", "US", 1));
	}

	@Test
	void d02_containsValue() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class).build();
		x.put("en", "US", 1, "value1");
		assertTrue(x.containsValue("value1"));
		assertFalse(x.containsValue("value2"));
	}

	//====================================================================================================
	// e - logOnExit() builder methods
	//====================================================================================================

	@Test
	void e01_logOnExit_withStringId() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.logOnExit("TestCache3")
			.supplier((k1, k2, k3) -> k1 + ":" + k2 + ":" + k3)
			.build();
		x.get("en", "US", 1);
		assertSize(1, x);
	}

	@Test
	void e02_logOnExit_withBoolean() {
		var x = Cache3.of(String.class, String.class, Integer.class, String.class)
			.logOnExit(true, "MyCache3")
			.supplier((k1, k2, k3) -> k1 + ":" + k2 + ":" + k3)
			.build();
		x.get("en", "US", 1);
		assertSize(1, x);
	}
}

