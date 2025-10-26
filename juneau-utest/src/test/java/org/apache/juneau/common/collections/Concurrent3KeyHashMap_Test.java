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

class Concurrent3KeyHashMap_Test extends TestBase {

	@Test void a01_basicPutAndGet() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>();

		map.put("lang", "en", 1, "Hello");
		map.put("lang", "fr", 1, "Bonjour");
		map.put("lang", "de", 1, "Guten Tag");
		map.put("greeting", "en", 1, "Greetings");

		assertEquals("Hello", map.get("lang", "en", 1));
		assertEquals("Bonjour", map.get("lang", "fr", 1));
		assertEquals("Guten Tag", map.get("lang", "de", 1));
		assertEquals("Greetings", map.get("greeting", "en", 1));
	}

	@Test void a02_getWithNonExistentKey() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>();

		map.put("lang", "en", 1, "Hello");

		assertNull(map.get("lang", "es", 1));
		assertNull(map.get("lang", "en", 2));
		assertNull(map.get("other", "en", 1));
	}

	@Test void a03_updateExistingKey() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>();

		assertNull(map.put("lang", "en", 1, "Hello"));
		assertEquals("Hello", map.put("lang", "en", 1, "Hi"));
		assertEquals("Hi", map.get("lang", "en", 1));
	}

	@Test void a04_nullKeys() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>();

		map.put(null, null, null, "All null");
		assertEquals("All null", map.get(null, null, null));

		map.put(null, "en", 1, "First null");
		assertEquals("First null", map.get(null, "en", 1));

		map.put("lang", null, 1, "Second null");
		assertEquals("Second null", map.get("lang", null, 1));

		map.put("lang", "en", null, "Third null");
		assertEquals("Third null", map.get("lang", "en", null));
	}

	@Test void a05_supplierComputesMissingValue() {
		var callCount = new AtomicInteger();
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>(false,
			(k1, k2, k3) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3;
			}
		);

		var result1 = map.get("a", "b", 1);
		assertEquals("a:b:1", result1);
		assertEquals(1, callCount.get());

		var result2 = map.get("a", "b", 1);
		assertEquals("a:b:1", result2);
		assertEquals(1, callCount.get());

		var result3 = map.get("a", "b", 2);
		assertEquals("a:b:2", result3);
		assertEquals(2, callCount.get());
	}

	@Test void a06_disabledMode_neverCaches() {
		var callCount = new AtomicInteger();
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>(true,
			(k1, k2, k3) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3;
			}
		);

		assertEquals("a:b:1", map.get("a", "b", 1));
		assertEquals(1, callCount.get());

		assertEquals("a:b:1", map.get("a", "b", 1));
		assertEquals(2, callCount.get());
	}

	@Test void a07_disabledMode_putDoesNothing() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>(true, null);

		assertNull(map.put("a", "b", 1, "value"));
		assertNull(map.get("a", "b", 1));
		assertEquals(0, map.size());
	}

	@Test void a08_keyEquality_differentKeyParts() {
		var map = new Concurrent3KeyHashMap<String,String,String,String>();

		map.put("a", "b", "c", "ABC");
		map.put("b", "c", "a", "BCA");
		map.put("c", "a", "b", "CAB");

		assertEquals("ABC", map.get("a", "b", "c"));
		assertEquals("BCA", map.get("b", "c", "a"));
		assertEquals("CAB", map.get("c", "a", "b"));
	}

	@Test void a09_sizeTracking() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>();

		assertEquals(0, map.size());

		map.put("a", "b", 1, "AB1");
		assertEquals(1, map.size());

		map.put("a", "b", 2, "AB2");
		assertEquals(2, map.size());

		map.put("a", "b", 1, "AB1Updated");
		assertEquals(2, map.size());
	}

	@Test void a10_clear() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>();

		map.put("a", "b", 1, "AB1");
		map.put("a", "b", 2, "AB2");

		assertEquals(2, map.size());

		map.clear();

		assertEquals(0, map.size());
		assertNull(map.get("a", "b", 1));
		assertNull(map.get("a", "b", 2));
	}

	@Test void a11_supplierReturnsNull() {
		var map = new Concurrent3KeyHashMap<String,String,Integer,String>(false,
			(k1, k2, k3) -> null
		);

		assertThrows(NullPointerException.class, () -> map.get("a", "b", 1));
	}
}

