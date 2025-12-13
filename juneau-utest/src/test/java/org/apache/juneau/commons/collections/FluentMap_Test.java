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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FluentMap_Test extends TestBase {

	//====================================================================================================
	// Basic functionality - a(K key, V value)
	//====================================================================================================

	@Test
	void a01_addSingleEntry() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void a02_addSingleEntry_returnsThis() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		var result = map.a("key1", "value1");

		assertSame(map, result);
	}

	@Test
	void a03_addSingleEntry_nullValue() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", null).a("key3", "value3");

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertNull(map.get("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void a04_addSingleEntry_nullKey() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a(null, "value1").a("key2", "value2");

		assertSize(2, map);
		assertEquals("value1", map.get(null));
		assertEquals("value2", map.get("key2"));
	}

	@Test
	void a05_addSingleEntry_updateExisting() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1");
		map.a("key1", "value1-updated");

		assertSize(1, map);
		assertEquals("value1-updated", map.get("key1"));
	}

	//====================================================================================================
	// a(Map) method
	//====================================================================================================

	@Test
	void b01_addMap() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		var other = map("key1", "value1", "key2", "value2", "key3", "value3");
		map.aa(other);

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void b02_addMap_returnsThis() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		var other = map("key1", "value1", "key2", "value2");
		var result = map.aa(other);

		assertSame(map, result);
	}

	@Test
	void b03_addMap_nullMap() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1");
		map.aa((Map<String, String>)null);
		map.a("key2", "value2");

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
	}

	@Test
	void b04_addMap_emptyMap() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1");
		map.aa(map());
		map.a("key2", "value2");

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
	}

	@Test
	void b05_addMap_multipleCalls() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.aa(map("key1", "value1", "key2", "value2"));
		map.aa(map("key3", "value3", "key4", "value4"));

		assertSize(4, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
		assertEquals("value4", map.get("key4"));
	}

	@Test
	void b06_addMap_overwritesExisting() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1");
		map.aa(map("key1", "value1-updated", "key2", "value2"));

		assertSize(2, map);
		assertEquals("value1-updated", map.get("key1"));
		assertEquals("value2", map.get("key2"));
	}

	//====================================================================================================
	// ai(boolean, K, V) method
	//====================================================================================================

	@Test
	void c01_ai_conditionTrue() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").ai(true, "key2", "value2").a("key3", "value3");

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void c02_ai_conditionFalse() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").ai(false, "key2", "value2").a("key3", "value3");

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertFalse(map.containsKey("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void c03_ai_returnsThis() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		var result1 = map.ai(true, "key1", "value1");
		var result2 = map.ai(false, "key2", "value2");

		assertSame(map, result1);
		assertSame(map, result2);
	}

	@Test
	void c04_ai_conditionalBuilding() {
		boolean includeDebug = true;
		boolean includeTest = false;

		var map = new FluentMap<>(new LinkedHashMap<String, String>())
			.a("host", "localhost")
			.a("port", "8080")
			.ai(includeDebug, "debug", "true")
			.ai(includeTest, "test", "true");

		assertSize(3, map);
		assertEquals("localhost", map.get("host"));
		assertEquals("8080", map.get("port"));
		assertEquals("true", map.get("debug"));
		assertFalse(map.containsKey("test"));
	}

	//====================================================================================================
	// Method chaining
	//====================================================================================================

	@Test
	void d01_methodChaining() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>())
			.a("key1", "value1")
			.a("key2", "value2")
			.ai(true, "key3", "value3")
			.ai(false, "key4", "value4")
			.aa(map("key5", "value5", "key6", "value6"));

		assertSize(5, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
		assertEquals("value5", map.get("key5"));
		assertEquals("value6", map.get("key6"));
		assertFalse(map.containsKey("key4"));
	}

	//====================================================================================================
	// Map interface methods
	//====================================================================================================

	@Test
	void e01_mapInterface_get() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2");

		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertNull(map.get("key3"));
	}

	@Test
	void e02_mapInterface_put() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		assertNull(map.put("key1", "value1"));
		assertEquals("value1", map.put("key1", "value1-updated"));

		assertSize(1, map);
		assertEquals("value1-updated", map.get("key1"));
	}

	@Test
	void e03_mapInterface_putAll() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1");
		map.putAll(map("key2", "value2", "key3", "value3"));

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void e04_mapInterface_remove() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		assertEquals("value2", map.remove("key2"));
		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertFalse(map.containsKey("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void e05_mapInterface_containsKey() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2");

		assertTrue(map.containsKey("key1"));
		assertTrue(map.containsKey("key2"));
		assertFalse(map.containsKey("key3"));
	}

	@Test
	void e06_mapInterface_containsValue() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2");

		assertTrue(map.containsValue("value1"));
		assertTrue(map.containsValue("value2"));
		assertFalse(map.containsValue("value3"));
	}

	@Test
	void e07_mapInterface_size() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		assertEquals(0, map.size());

		map.a("key1", "value1");
		assertEquals(1, map.size());

		map.a("key2", "value2");
		assertEquals(2, map.size());
	}

	@Test
	void e08_mapInterface_isEmpty() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		assertTrue(map.isEmpty());

		map.a("key1", "value1");
		assertFalse(map.isEmpty());
	}

	@Test
	void e09_mapInterface_clear() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		map.clear();
		assertTrue(map.isEmpty());
		assertSize(0, map);
	}

	@Test
	void e10_mapInterface_keySet() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		var keySet = map.keySet();
		assertSize(3, keySet);
		assertTrue(keySet.contains("key1"));
		assertTrue(keySet.contains("key2"));
		assertTrue(keySet.contains("key3"));
	}

	@Test
	void e11_mapInterface_values() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		var values = map.values();
		assertSize(3, values);
		assertTrue(values.contains("value1"));
		assertTrue(values.contains("value2"));
		assertTrue(values.contains("value3"));
	}

	@Test
	void e12_mapInterface_entrySet() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		var entrySet = map.entrySet();
		assertSize(3, entrySet);

		var found = new LinkedHashSet<String>();
		for (var entry : entrySet) {
			found.add(entry.getKey() + "=" + entry.getValue());
		}
		assertTrue(found.contains("key1=value1"));
		assertTrue(found.contains("key2=value2"));
		assertTrue(found.contains("key3=value3"));
	}

	//====================================================================================================
	// Different map implementations
	//====================================================================================================

	@Test
	void f01_hashMap() {
		var map = new FluentMap<>(new HashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		assertSize(3, map);
	}

	@Test
	void f02_treeMap() {
		var map = new FluentMap<>(new TreeMap<String, String>());
		map.a("zebra", "value3").a("apple", "value1").a("banana", "value2");

		assertSize(3, map);
		// TreeMap maintains sorted order
		var keys = new ArrayList<>(map.keySet());
		assertEquals(List.of("apple", "banana", "zebra"), keys);
	}

	@Test
	void f03_concurrentHashMap() {
		var map = new FluentMap<>(new ConcurrentHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2").a("key3", "value3");

		assertSize(3, map);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void g01_emptyMap() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());

		assertTrue(map.isEmpty());
		assertSize(0, map);
		assertFalse(map.containsKey("anything"));
		assertNull(map.get("anything"));
	}

	@Test
	void g02_nullKeyAndValue() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a(null, null).a("key1", "value1");

		assertSize(2, map);
		assertNull(map.get(null));
		assertTrue(map.containsKey(null));
		assertEquals("value1", map.get("key1"));
	}

	@Test
	void g03_updateWithNullValue() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1");
		map.a("key1", null);

		assertSize(1, map);
		assertTrue(map.containsKey("key1"));
		assertNull(map.get("key1"));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void w01_toString_delegatesToUnderlyingMap() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2");

		var underlyingMap = new LinkedHashMap<String, String>();
		underlyingMap.put("key1", "value1");
		underlyingMap.put("key2", "value2");

		assertEquals(underlyingMap.toString(), map.toString());
	}

	@Test
	void w02_equals_delegatesToUnderlyingMap() {
		var map1 = new FluentMap<>(new LinkedHashMap<String, String>());
		map1.a("key1", "value1").a("key2", "value2");

		var map2 = new LinkedHashMap<String, String>();
		map2.put("key1", "value1");
		map2.put("key2", "value2");

		assertTrue(map1.equals(map2));
		assertTrue(map2.equals(map1));
	}

	@Test
	void w03_equals_differentContents_returnsFalse() {
		var map1 = new FluentMap<>(new LinkedHashMap<String, String>());
		map1.a("key1", "value1");

		var map2 = new LinkedHashMap<String, String>();
		map2.put("key1", "value2");

		assertFalse(map1.equals(map2));
		assertFalse(map2.equals(map1));
	}

	@Test
	void w04_hashCode_delegatesToUnderlyingMap() {
		var map = new FluentMap<>(new LinkedHashMap<String, String>());
		map.a("key1", "value1").a("key2", "value2");

		var underlyingMap = new LinkedHashMap<String, String>();
		underlyingMap.put("key1", "value1");
		underlyingMap.put("key2", "value2");

		assertEquals(underlyingMap.hashCode(), map.hashCode());
	}
}

