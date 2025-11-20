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
import static org.apache.juneau.junit.bct.BctAssertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Cache4_Test extends TestBase {

	//====================================================================================================
	// a - Basic cache operations
	//====================================================================================================

	@Test
	void a01_defaultSupplier_basic() {
		var callCount = new AtomicInteger();
		var x = Cache4.of(String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4;
			})
			.build();

		var result1 = x.get("en", "US", "formal", 1);
		var result2 = x.get("en", "US", "formal", 1); // Cache hit

		assertEquals("en:US:formal:1", result1);
		assertEquals("en:US:formal:1", result2);
		assertEquals(1, callCount.get());
		assertEquals(1, x.getCacheHits());
	}

	@Test
	void a02_overrideSupplier() {
		var x = Cache4.of(String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4) -> "DEFAULT")
			.build();

		var result = x.get("en", "US", "formal", 1, () -> "OVERRIDE");

		assertEquals("OVERRIDE", result);
	}

	@Test
	void a03_nullKeys() {
		var x = Cache4.of(String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4) -> "value")
			.build();

		assertThrows(IllegalArgumentException.class, () -> x.get(null, "US", "formal", 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", null, "formal", 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", "US", null, 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", "US", "formal", null));
	}

	@Test
	void a04_disableCaching() {
		var callCount = new AtomicInteger();
		var x = Cache4.of(String.class, String.class, String.class, Integer.class, String.class)
			.disableCaching()
			.supplier((k1, k2, k3, k4) -> {
				callCount.incrementAndGet();
				return "value";
			})
			.build();

		x.get("en", "US", "formal", 1);
		x.get("en", "US", "formal", 1);

		assertEquals(2, callCount.get()); // Called twice
		assertEmpty(x);
	}

	@Test
	void a05_maxSize() {
		var x = Cache4.of(String.class, String.class, String.class, Integer.class, String.class)
			.maxSize(2)
			.supplier((k1, k2, k3, k4) -> "value")
			.build();

		x.get("en", "US", "formal", 1);
		x.get("fr", "FR", "formal", 2);
		assertSize(2, x);

		x.get("de", "DE", "formal", 3); // Doesn't exceed yet
		assertSize(3, x);

		x.get("es", "ES", "formal", 4); // Exceeds max (3 > 2), triggers clear
		assertSize(1, x); // Cleared
	}

	@Test
	void a06_cacheHitsTracking() {
		var x = Cache4.of(String.class, String.class, String.class, Integer.class, String.class)
			.supplier((k1, k2, k3, k4) -> "value")
			.build();

		x.get("en", "US", "formal", 1); // Miss
		assertEquals(0, x.getCacheHits());

		x.get("en", "US", "formal", 1); // Hit
		x.get("en", "US", "formal", 1); // Hit
		assertEquals(2, x.getCacheHits());
	}
}

