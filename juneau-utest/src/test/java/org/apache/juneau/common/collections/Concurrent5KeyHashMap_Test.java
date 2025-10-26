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

class Concurrent5KeyHashMap_Test extends TestBase {

	@Test void a01_basicPutAndGet() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,Integer>();

		map.put("env1", "db1", "schema1", "table1", "select", 100);
		map.put("env1", "db1", "schema1", "table1", "insert", 50);
		map.put("env1", "db1", "schema1", "table2", "select", 75);
		map.put("env2", "db1", "schema1", "table1", "select", 200);

		assertEquals(100, map.get("env1", "db1", "schema1", "table1", "select"));
		assertEquals(50, map.get("env1", "db1", "schema1", "table1", "insert"));
		assertEquals(75, map.get("env1", "db1", "schema1", "table2", "select"));
		assertEquals(200, map.get("env2", "db1", "schema1", "table1", "select"));
	}

	@Test void a02_getWithNonExistentKey() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,Integer>();

		map.put("env1", "db1", "schema1", "table1", "select", 100);

		assertNull(map.get("env1", "db1", "schema1", "table1", "delete"));
		assertNull(map.get("env1", "db1", "schema1", "table2", "select"));
		assertNull(map.get("env1", "db1", "schema2", "table1", "select"));
		assertNull(map.get("env1", "db2", "schema1", "table1", "select"));
		assertNull(map.get("env2", "db1", "schema1", "table1", "select"));
	}

	@Test void a03_updateExistingKey() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,Integer>();

		assertNull(map.put("env1", "db1", "schema1", "table1", "select", 100));
		assertEquals(100, map.put("env1", "db1", "schema1", "table1", "select", 200));
		assertEquals(200, map.get("env1", "db1", "schema1", "table1", "select"));
	}

	@Test void a04_nullKeys() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>();

		map.put(null, null, null, null, null, "All null");
		assertEquals("All null", map.get(null, null, null, null, null));

		map.put(null, "b", "c", "d", "e", "First null");
		assertEquals("First null", map.get(null, "b", "c", "d", "e"));

		map.put("a", null, "c", "d", "e", "Second null");
		assertEquals("Second null", map.get("a", null, "c", "d", "e"));

		map.put("a", "b", null, "d", "e", "Third null");
		assertEquals("Third null", map.get("a", "b", null, "d", "e"));

		map.put("a", "b", "c", null, "e", "Fourth null");
		assertEquals("Fourth null", map.get("a", "b", "c", null, "e"));

		map.put("a", "b", "c", "d", null, "Fifth null");
		assertEquals("Fifth null", map.get("a", "b", "c", "d", null));
	}

	@Test void a05_supplierComputesMissingValue() {
		var callCount = new AtomicInteger();
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>(false,
			(k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			}
		);

		var result1 = map.get("a", "b", "c", "d", "e");
		assertEquals("a:b:c:d:e", result1);
		assertEquals(1, callCount.get());

		var result2 = map.get("a", "b", "c", "d", "e");
		assertEquals("a:b:c:d:e", result2);
		assertEquals(1, callCount.get());

		var result3 = map.get("a", "b", "c", "d", "f");
		assertEquals("a:b:c:d:f", result3);
		assertEquals(2, callCount.get());
	}

	@Test void a06_disabledMode_neverCaches() {
		var callCount = new AtomicInteger();
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>(true,
			(k1, k2, k3, k4, k5) -> {
				callCount.incrementAndGet();
				return k1 + ":" + k2 + ":" + k3 + ":" + k4 + ":" + k5;
			}
		);

		assertEquals("a:b:c:d:e", map.get("a", "b", "c", "d", "e"));
		assertEquals(1, callCount.get());

		assertEquals("a:b:c:d:e", map.get("a", "b", "c", "d", "e"));
		assertEquals(2, callCount.get());
	}

	@Test void a07_disabledMode_putDoesNothing() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>(true, null);

		assertNull(map.put("a", "b", "c", "d", "e", "value"));
		assertNull(map.get("a", "b", "c", "d", "e"));
		assertEquals(0, map.size());
	}

	@Test void a08_keyEquality_differentKeyParts() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>();

		map.put("a", "b", "c", "d", "e", "ABCDE");
		map.put("e", "d", "c", "b", "a", "EDCBA");

		assertEquals("ABCDE", map.get("a", "b", "c", "d", "e"));
		assertEquals("EDCBA", map.get("e", "d", "c", "b", "a"));
	}

	@Test void a09_sizeTracking() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>();

		assertEquals(0, map.size());

		map.put("a", "b", "c", "d", "e", "ABCDE");
		assertEquals(1, map.size());

		map.put("a", "b", "c", "d", "f", "ABCDF");
		assertEquals(2, map.size());

		map.put("a", "b", "c", "d", "e", "ABCDE_Updated");
		assertEquals(2, map.size());
	}

	@Test void a10_clear() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>();

		map.put("a", "b", "c", "d", "e", "ABCDE");
		map.put("a", "b", "c", "d", "f", "ABCDF");

		assertEquals(2, map.size());

		map.clear();

		assertEquals(0, map.size());
		assertNull(map.get("a", "b", "c", "d", "e"));
		assertNull(map.get("a", "b", "c", "d", "f"));
	}

	@Test void a11_supplierReturnsNull() {
		var map = new Concurrent5KeyHashMap<String,String,String,String,String,String>(false,
			(k1, k2, k3, k4, k5) -> null
		);

		assertThrows(NullPointerException.class, () -> map.get("a", "b", "c", "d", "e"));
	}
}

