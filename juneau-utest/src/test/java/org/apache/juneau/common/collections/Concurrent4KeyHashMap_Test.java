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

class Concurrent4KeyHashMap_Test extends TestBase {

	@Test void a01_basicPutAndGet() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,Boolean>();

		map.put("tenant1", "user1", "doc", "read", true);
		map.put("tenant1", "user1", "doc", "write", false);
		map.put("tenant1", "user2", "doc", "read", false);
		map.put("tenant2", "user1", "doc", "read", true);

		assertEquals(true, map.get("tenant1", "user1", "doc", "read"));
		assertEquals(false, map.get("tenant1", "user1", "doc", "write"));
		assertEquals(false, map.get("tenant1", "user2", "doc", "read"));
		assertEquals(true, map.get("tenant2", "user1", "doc", "read"));
	}

	@Test void a02_getWithNonExistentKey() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,Boolean>();

		map.put("tenant1", "user1", "doc", "read", true);

		assertNull(map.get("tenant1", "user1", "doc", "delete"));
		assertNull(map.get("tenant1", "user1", "file", "read"));
		assertNull(map.get("tenant1", "user2", "doc", "read"));
		assertNull(map.get("tenant2", "user1", "doc", "read"));
	}

	@Test void a03_updateExistingKey() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,Boolean>();

		assertNull(map.put("tenant1", "user1", "doc", "read", true));
		assertEquals(true, map.put("tenant1", "user1", "doc", "read", false));
		assertEquals(false, map.get("tenant1", "user1", "doc", "read"));
	}

	@Test void a04_nullKeys() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>();

		map.put(null, null, null, null, "All null");
		assertEquals("All null", map.get(null, null, null, null));

		map.put(null, "b", "c", "d", "First null");
		assertEquals("First null", map.get(null, "b", "c", "d"));

		map.put("a", null, "c", "d", "Second null");
		assertEquals("Second null", map.get("a", null, "c", "d"));

		map.put("a", "b", null, "d", "Third null");
		assertEquals("Third null", map.get("a", "b", null, "d"));

		map.put("a", "b", "c", null, "Fourth null");
		assertEquals("Fourth null", map.get("a", "b", "c", null));
	}

	@Test void a05_supplierComputesMissingValue() {
		var callCount = new AtomicInteger();
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>(false,
			(k1, k2, k3, k4) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4;
			}
		);

		var result1 = map.get("a", "b", "c", "d");
		assertEquals("a:b:c:d", result1);
		assertEquals(1, callCount.get());

		var result2 = map.get("a", "b", "c", "d");
		assertEquals("a:b:c:d", result2);
		assertEquals(1, callCount.get());

		var result3 = map.get("a", "b", "c", "e");
		assertEquals("a:b:c:e", result3);
		assertEquals(2, callCount.get());
	}

	@Test void a06_disabledMode_neverCaches() {
		var callCount = new AtomicInteger();
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>(true,
			(k1, k2, k3, k4) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4;
			}
		);

		assertEquals("a:b:c:d", map.get("a", "b", "c", "d"));
		assertEquals(1, callCount.get());

		assertEquals("a:b:c:d", map.get("a", "b", "c", "d"));
		assertEquals(2, callCount.get());
	}

	@Test void a07_disabledMode_putDoesNothing() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>(true, null);

		assertNull(map.put("a", "b", "c", "d", "value"));
		assertNull(map.get("a", "b", "c", "d"));
		assertEquals(0, map.size());
	}

	@Test void a08_keyEquality_differentKeyParts() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>();

		map.put("a", "b", "c", "d", "ABCD");
		map.put("d", "c", "b", "a", "DCBA");

		assertEquals("ABCD", map.get("a", "b", "c", "d"));
		assertEquals("DCBA", map.get("d", "c", "b", "a"));
	}

	@Test void a09_sizeTracking() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>();

		assertEquals(0, map.size());

		map.put("a", "b", "c", "d", "ABCD");
		assertEquals(1, map.size());

		map.put("a", "b", "c", "e", "ABCE");
		assertEquals(2, map.size());

		map.put("a", "b", "c", "d", "ABCD_Updated");
		assertEquals(2, map.size());
	}

	@Test void a10_clear() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>();

		map.put("a", "b", "c", "d", "ABCD");
		map.put("a", "b", "c", "e", "ABCE");

		assertEquals(2, map.size());

		map.clear();

		assertEquals(0, map.size());
		assertNull(map.get("a", "b", "c", "d"));
		assertNull(map.get("a", "b", "c", "e"));
	}

	@Test void a11_supplierReturnsNull() {
		var map = new Concurrent4KeyHashMap<String,String,String,String,String>(false,
			(k1, k2, k3, k4) -> null
		);

		assertThrows(NullPointerException.class, () -> map.get("a", "b", "c", "d"));
	}
}

