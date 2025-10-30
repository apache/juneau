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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SimpleMap_Test extends TestBase {

	//====================================================================================================
	// Null key support
	//====================================================================================================
	@Test
	void a01_nullKey_singleEntry() {
		String[] keys = {null};
		String[] values = {"value1"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		assertSize(1, map);
		assertEquals("value1", map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a02_nullKey_withOtherKeys() {
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key3"));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a03_nullKey_updateValue() {
		String[] keys = {"key1", null};
		String[] values = {"value1", "value2"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		String oldValue = map.put(null, "newValue");
		
		assertEquals("value2", oldValue);
		assertEquals("newValue", map.get(null));
		assertSize(2, map);
	}

	@Test
	void a04_nullKey_withNullValue() {
		String[] keys = {null};
		String[] values = {null};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		assertSize(1, map);
		assertNull(map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a05_nullKey_entrySet() {
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		boolean foundNullKey = false;
		for (var entry : map.entrySet()) {
			if (entry.getKey() == null) {
				foundNullKey = true;
				assertEquals("value2", entry.getValue());
			}
		}
		assertTrue(foundNullKey, "Null key not found in entrySet");
	}

	@Test
	void a06_nullKey_keySet() {
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		assertTrue(map.keySet().contains(null), "Null key not found in keySet");
		assertSize(3, map.keySet());
	}

	//====================================================================================================
	// Duplicate key detection
	//====================================================================================================
	@Test
	void b01_duplicateKey_nonNullKeys() {
		String[] keys = {"key1", "key2", "key1"};
		String[] values = {"value1", "value2", "value3"};
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});
		
		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b02_duplicateKey_nullKeys() {
		String[] keys = {null, "key2", null};
		String[] values = {"value1", "value2", "value3"};
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});
		
		assertTrue(ex.getMessage().contains("Duplicate key found: null"));
	}

	@Test
	void b03_duplicateKey_mixedNullAndNonNull() {
		String[] keys = {"key1", null, "key2", "key1"};
		String[] values = {"value1", "value2", "value3", "value4"};
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});
		
		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b04_noDuplicateKeys_success() {
		String[] keys = {"key1", null, "key2", "key3"};
		String[] values = {"value1", "value2", "value3", "value4"};
		
		SimpleMap<String, String> map = assertDoesNotThrow(() -> new SimpleMap<>(keys, values));
		
		assertSize(4, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key2"));
		assertEquals("value4", map.get("key3"));
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================
	@Test
	void c01_emptyMap_noNullKeys() {
		String[] keys = {};
		String[] values = {};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		assertEmpty(map);
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void c02_getLookup_nullKeyNotInMap() {
		String[] keys = {"key1", "key2"};
		String[] values = {"value1", "value2"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void c03_putOperation_cannotAddNewNullKey() {
		String[] keys = {"key1"};
		String[] values = {"value1"};
		
		SimpleMap<String, String> map = new SimpleMap<>(keys, values);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			map.put(null, "newValue");
		});
		
		assertTrue(ex.getMessage().contains("No key 'null' defined in map"));
	}

	@Test
	void c04_complexTypes_nullKey() {
		Integer[] keys = {1, null, 3};
		String[] values = {"one", "null-key", "three"};
		
		SimpleMap<Integer, String> map = new SimpleMap<>(keys, values);
		
		assertEquals("one", map.get(1));
		assertEquals("null-key", map.get(null));
		assertEquals("three", map.get(3));
	}
}
