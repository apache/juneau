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

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
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

		var map = new SimpleMap<>(keys, values);

		assertSize(1, map);
		assertEquals("value1", map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a02_nullKey_withOtherKeys() {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		var map = new SimpleMap<>(keys, values);

		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key3"));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a03_nullKey_cannotModify() {
		var keys = a("key1", null);
		var values = a("value1", "value2");

		var map = new SimpleMap<>(keys, values);

		assertThrows(UnsupportedOperationException.class, () -> {
			map.put(null, "newValue");
		});

		// Verify original value unchanged
		assertEquals("value2", map.get(null));
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

		var map = new SimpleMap<>(keys, values);

		var foundNullKey = false;
		for (var entry : map.entrySet()) {
			if (entry.getKey() == null) {
				foundNullKey = true;
				assertEquals("value2", entry.getValue());

				// Verify entry is also unmodifiable
				assertThrows(UnsupportedOperationException.class, () -> {
					entry.setValue("modified");
				});
			}
		}
		assertTrue(foundNullKey, "Null key not found in entrySet");
	}

	@Test
	void a06_nullKey_keySet() {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		var map = new SimpleMap<>(keys, values);

		assertTrue(map.keySet().contains(null), "Null key not found in keySet");
		assertSize(3, map.keySet());
	}

	//====================================================================================================
	// Duplicate key detection
	//====================================================================================================
	@Test
	void b01_duplicateKey_nonNullKeys() {
		var keys = a("key1", "key2", "key1");
		var values = a("value1", "value2", "value3");

		assertThrowsWithMessage(IllegalArgumentException.class, "Duplicate key found: key1", () -> {
			new SimpleMap<>(keys, values);
		});
	}

	@Test
	void b02_duplicateKey_nullKeys() {
		var keys = a(null, "key2", null);
		var values = a("value1", "value2", "value3");

		assertThrowsWithMessage(IllegalArgumentException.class, "Duplicate key found: null", () -> {
			new SimpleMap<>(keys, values);
		});
	}

	@Test
	void b03_duplicateKey_mixedNullAndNonNull() {
		var keys = a("key1", null, "key2", "key1");
		var values = a("value1", "value2", "value3", "value4");

		assertThrowsWithMessage(IllegalArgumentException.class, "Duplicate key found: key1", () -> {
			new SimpleMap<>(keys, values);
		});
	}

	@Test
	void b04_noDuplicateKeys_success() {
		var keys = a("key1", null, "key2", "key3");
		var values = a("value1", "value2", "value3", "value4");

		var map = assertDoesNotThrow(() -> new SimpleMap<>(keys, values));

		assertSize(4, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key2"));
		assertEquals("value4", map.get("key3"));
	}

	//====================================================================================================
	// Immutability verification
	//====================================================================================================
	@Test
	void c01_put_throwsException() {
		var keys = a("key1");
		var values = a("value1");

		var map = new SimpleMap<>(keys, values);

		assertThrows(UnsupportedOperationException.class, () -> {
			map.put("key1", "newValue");
		});

		assertEquals("value1", map.get("key1"));
	}

	@Test
	void c02_entrySetValues_cannotModify() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2");

		var map = new SimpleMap<>(keys, values);

		for (var entry : map.entrySet()) {
			assertThrows(UnsupportedOperationException.class, () -> {
				entry.setValue("modified");
			});
		}
	}

	//====================================================================================================
	// Array length mismatch
	//====================================================================================================
	@Test
	void d01_arrayLengthMismatch_keysLonger() {
		var keys = a("key1", "key2", "key3");
		var values = a("value1", "value2");

		assertThrowsWithMessage(IllegalArgumentException.class, java.util.List.of("array lengths differ", "3", "2"), () -> {
			new SimpleMap<>(keys, values);
		});
	}

	@Test
	void d02_arrayLengthMismatch_valuesLonger() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2", "value3", "value4");

		assertThrowsWithMessage(IllegalArgumentException.class, java.util.List.of("array lengths differ", "2", "4"), () -> {
			new SimpleMap<>(keys, values);
		});
	}

	@Test
	void d03_arrayLengthMismatch_emptyKeys() {
		var keys = new String[0];
		var values = a("value1");

		assertThrowsWithMessage(IllegalArgumentException.class, "array lengths differ", () -> {
			new SimpleMap<>(keys, values);
		});
	}

	@Test
	void d04_arrayLengthMismatch_emptyValues() {
		var keys = a("key1", "key2");
		var values = new String[0];

		assertThrowsWithMessage(IllegalArgumentException.class, "array lengths differ", () -> {
			new SimpleMap<>(keys, values);
		});
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================
	@Test
	void e01_emptyMap_noNullKeys() {
		var keys = new String[0];
		var values = new String[0];

		var map = new SimpleMap<>(keys, values);

		assertEmpty(map);
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void e02_getLookup_nullKeyNotInMap() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2");

		var map = new SimpleMap<>(keys, values);

		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void e03_complexTypes_nullKey() {
		var keys = a(1, null, 3);
		var values = a("one", "null-key", "three");

		var map = new SimpleMap<>(keys, values);

		assertEquals("one", map.get(1));
		assertEquals("null-key", map.get(null));
		assertEquals("three", map.get(3));
	}

	//====================================================================================================
	// Thread safety verification
	//====================================================================================================
	@Test
	void f01_concurrentAccess_safe() throws InterruptedException {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		var map = new SimpleMap<>(keys, values);

		// Create multiple threads reading from the map
		var threads = new Thread[10];
		for (var i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				for (var j = 0; j < 1000; j++) {
					assertEquals("value1", map.get("key1"));
					assertEquals("value2", map.get(null));
					assertEquals("value3", map.get("key3"));
				}
			});
			threads[i].start();
		}

		// Wait for all threads to complete
		for (var thread : threads) {
			thread.join();
		}

		// Verify map is still intact
		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key3"));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void e01_toString_standardFormat() {
		String[] keys = { "key1", "key2", "key3" };
		Object[] values = { "value1", "value2", "value3" };
		SimpleMap<String,Object> map = new SimpleMap<>(keys, values);

		var result = map.toString();
		assertTrue(result.startsWith("{"));
		assertTrue(result.endsWith("}"));
		assertTrue(result.contains("key1=value1"));
		assertTrue(result.contains("key2=value2"));
		assertTrue(result.contains("key3=value3"));
	}

	@Test
	void e02_toString_emptyMap() {
		String[] keys = {};
		Object[] values = {};
		SimpleMap<String,Object> map = new SimpleMap<>(keys, values);

		assertEquals("{}", map.toString());
	}

	@Test
	void e03_equals_sameContents() {
		String[] keys1 = { "key1", "key2" };
		Object[] values1 = { "value1", "value2" };
		SimpleMap<String,Object> map1 = new SimpleMap<>(keys1, values1);

		String[] keys2 = { "key1", "key2" };
		Object[] values2 = { "value1", "value2" };
		SimpleMap<String,Object> map2 = new SimpleMap<>(keys2, values2);

		assertTrue(map1.equals(map2));
		assertTrue(map2.equals(map1));
	}

	@Test
	void e04_equals_differentContents() {
		String[] keys1 = { "key1", "key2" };
		Object[] values1 = { "value1", "value2" };
		SimpleMap<String,Object> map1 = new SimpleMap<>(keys1, values1);

		String[] keys2 = { "key1", "key2" };
		Object[] values2 = { "value1", "value3" };
		SimpleMap<String,Object> map2 = new SimpleMap<>(keys2, values2);

		assertFalse(map1.equals(map2));
		assertFalse(map2.equals(map1));
	}

	@Test
	void e05_equals_regularMap() {
		String[] keys = { "key1", "key2" };
		Object[] values = { "value1", "value2" };
		SimpleMap<String,Object> map = new SimpleMap<>(keys, values);

		var regularMap = new LinkedHashMap<String, Object>();
		regularMap.put("key1", "value1");
		regularMap.put("key2", "value2");

		assertTrue(map.equals(regularMap));
		assertTrue(regularMap.equals(map));
	}

	@Test
	void e06_equals_notAMap() {
		String[] keys = { "key1" };
		Object[] values = { "value1" };
		SimpleMap<String,Object> map = new SimpleMap<>(keys, values);

		assertFalse(map.equals("not a map"));
		assertFalse(map.equals(null));
	}

	@Test
	void e07_hashCode_sameContents() {
		String[] keys1 = { "key1", "key2" };
		Object[] values1 = { "value1", "value2" };
		SimpleMap<String,Object> map1 = new SimpleMap<>(keys1, values1);

		String[] keys2 = { "key1", "key2" };
		Object[] values2 = { "value1", "value2" };
		SimpleMap<String,Object> map2 = new SimpleMap<>(keys2, values2);

		assertEquals(map1.hashCode(), map2.hashCode());
	}

	@Test
	void e08_hashCode_regularMap() {
		String[] keys = { "key1", "key2" };
		Object[] values = { "value1", "value2" };
		SimpleMap<String,Object> map = new SimpleMap<>(keys, values);

		var regularMap = new LinkedHashMap<String, Object>();
		regularMap.put("key1", "value1");
		regularMap.put("key2", "value2");

		assertEquals(map.hashCode(), regularMap.hashCode());
	}
}
