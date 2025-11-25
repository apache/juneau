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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SimpleMap_Test extends TestBase {

	//====================================================================================================
	// Null key support
	//====================================================================================================
	@Test
	void a01_nullKey_singleEntry() {
		var keys = a((String)null);
		var values = a("value1");

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		assertSize(1, map);
		assertEquals("value1", map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a02_nullKey_withOtherKeys() {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key3"));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a03_nullKey_updateValue() {
		var keys = a("key1", null);
		var values = a("value1", "value2");

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		String oldValue = map.put(null, "newValue");

		assertEquals("value2", oldValue);
		assertEquals("newValue", map.get(null));
		assertSize(2, map);
	}

	@Test
	void a04_nullKey_withNullValue() {
		var keys = a((String)null);
		var values = a((String)null);

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		assertSize(1, map);
		assertNull(map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a05_nullKey_entrySet() {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		var foundNullKey = false;
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
		String[] keys = { "key1", null, "key3" };
		String[] values = { "value1", "value2", "value3" };

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		assertTrue(map.keySet().contains(null), "Null key not found in keySet");
		assertSize(3, map.keySet());
	}

	//====================================================================================================
	// Duplicate key detection
	//====================================================================================================
	@Test
	void b01_duplicateKey_nonNullKeys() {
		String[] keys = { "key1", "key2", "key1" };
		String[] values = { "value1", "value2", "value3" };

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b02_duplicateKey_nullKeys() {
		String[] keys = { null, "key2", null };
		String[] values = { "value1", "value2", "value3" };

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: null"));
	}

	@Test
	void b03_duplicateKey_mixedNullAndNonNull() {
		String[] keys = { "key1", null, "key2", "key1" };
		String[] values = { "value1", "value2", "value3", "value4" };

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b04_noDuplicateKeys_success() {
		String[] keys = { "key1", null, "key2", "key3" };
		String[] values = { "value1", "value2", "value3", "value4" };

		SimpleMap<String,String> map = assertDoesNotThrow(() -> new SimpleMap<>(keys, values));

		assertSize(4, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key2"));
		assertEquals("value4", map.get("key3"));
	}

	//====================================================================================================
	// Array length mismatch
	//====================================================================================================
	@Test
	void c01_arrayLengthMismatch_keysLonger() {
		var keys = a("key1", "key2", "key3");
		var values = a("value1", "value2");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
		assertTrue(ex.getMessage().contains("3")); // keys length
		assertTrue(ex.getMessage().contains("2")); // values length
	}

	@Test
	void c02_arrayLengthMismatch_valuesLonger() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2", "value3", "value4");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
		assertTrue(ex.getMessage().contains("2")); // keys length
		assertTrue(ex.getMessage().contains("4")); // values length
	}

	@Test
	void c03_arrayLengthMismatch_emptyKeys() {
		var keys = new String[0];
		var values = a("value1");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
	}

	@Test
	void c04_arrayLengthMismatch_emptyValues() {
		var keys = a("key1", "key2");
		var values = new String[0];

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================
	@Test
	void c05_emptyMap_noNullKeys() {
		String[] keys = {};
		String[] values = {};

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		assertEmpty(map);
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void c06_getLookup_nullKeyNotInMap() {
		String[] keys = { "key1", "key2" };
		String[] values = { "value1", "value2" };

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void c07_putOperation_cannotAddNewNullKey() {
		String[] keys = { "key1" };
		String[] values = { "value1" };

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			map.put(null, "newValue");
		});

		assertTrue(ex.getMessage().contains("No key 'null' defined in map"));
	}

	@Test
	void c08_complexTypes_nullKey() {
		var keys = a(1, null, 3);
		var values = a("one", "null-key", "three");

		SimpleMap<Integer,String> map = new SimpleMap<>(keys, values);

		assertEquals("one", map.get(1));
		assertEquals("null-key", map.get(null));
		assertEquals("three", map.get(3));
	}

	//====================================================================================================
	// Entry setValue
	//====================================================================================================

	@Test
	void d01_entrySetValue() {
		String[] keys = { "key1", "key2", "key3" };
		String[] values = { "value1", "value2", "value3" };

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		// Get an entry and modify its value
		var entry = map.entrySet().iterator().next();
		String oldValue = entry.setValue("newValue");

		// Verify the value was updated in the map
		assertEquals("newValue", map.get(entry.getKey()));
		assertEquals("newValue", entry.getValue());
		assertTrue(oldValue.equals("value1") || oldValue.equals("value2") || oldValue.equals("value3"));
	}

	@Test
	void d02_entrySetValue_updatesUnderlyingArray() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2");

		var map = new SimpleMap<>(keys, values);

		// Find the entry for "key1"
		Map.Entry<String,String> entry = null;
		for (var e : map.entrySet()) {
			if ("key1".equals(e.getKey())) {
				entry = e;
				break;
			}
		}

		assertNotNull(entry);
		var oldValue = entry.setValue("updated");
		assertEquals("value1", oldValue);
		assertEquals("updated", map.get("key1"));
		assertEquals("updated", entry.getValue());
	}

	@Test
	void d03_entrySetValue_nullValue() {
		String[] keys = { "key1" };
		String[] values = { "value1" };

		SimpleMap<String,String> map = new SimpleMap<>(keys, values);

		var entry = map.entrySet().iterator().next();
		String oldValue = entry.setValue(null);

		assertEquals("value1", oldValue);
		assertNull(map.get("key1"));
		assertNull(entry.getValue());
	}
}
