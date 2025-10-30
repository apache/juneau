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

class SimpleUnmodifiableMap_Test extends TestBase {

	//====================================================================================================
	// Null key support
	//====================================================================================================
	@Test
	void a01_nullKey_singleEntry() {
		String[] keys = {null};
		String[] values = {"value1"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertSize(1, map);
		assertEquals("value1", map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a02_nullKey_withOtherKeys() {
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertSize(3, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get(null));
		assertEquals("value3", map.get("key3"));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a03_nullKey_cannotModify() {
		String[] keys = {"key1", null};
		String[] values = {"value1", "value2"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertThrows(UnsupportedOperationException.class, () -> {
			map.put(null, "newValue");
		});
		
		// Verify original value unchanged
		assertEquals("value2", map.get(null));
	}

	@Test
	void a04_nullKey_withNullValue() {
		String[] keys = {null};
		String[] values = {null};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertSize(1, map);
		assertNull(map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a05_nullKey_entrySet() {
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		boolean foundNullKey = false;
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
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
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
			new SimpleUnmodifiableMap<>(keys, values);
		});
		
		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b02_duplicateKey_nullKeys() {
		String[] keys = {null, "key2", null};
		String[] values = {"value1", "value2", "value3"};
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});
		
		assertTrue(ex.getMessage().contains("Duplicate key found: null"));
	}

	@Test
	void b03_duplicateKey_mixedNullAndNonNull() {
		String[] keys = {"key1", null, "key2", "key1"};
		String[] values = {"value1", "value2", "value3", "value4"};
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});
		
		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b04_noDuplicateKeys_success() {
		String[] keys = {"key1", null, "key2", "key3"};
		String[] values = {"value1", "value2", "value3", "value4"};
		
		SimpleUnmodifiableMap<String, String> map = assertDoesNotThrow(() -> 
			new SimpleUnmodifiableMap<>(keys, values)
		);
		
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
		String[] keys = {"key1"};
		String[] values = {"value1"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertThrows(UnsupportedOperationException.class, () -> {
			map.put("key1", "newValue");
		});
		
		assertEquals("value1", map.get("key1"));
	}

	@Test
	void c02_entrySetValues_cannotModify() {
		String[] keys = {"key1", "key2"};
		String[] values = {"value1", "value2"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		for (var entry : map.entrySet()) {
			assertThrows(UnsupportedOperationException.class, () -> {
				entry.setValue("modified");
			});
		}
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================
	@Test
	void d01_emptyMap_noNullKeys() {
		String[] keys = {};
		String[] values = {};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertEmpty(map);
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void d02_getLookup_nullKeyNotInMap() {
		String[] keys = {"key1", "key2"};
		String[] values = {"value1", "value2"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void d03_complexTypes_nullKey() {
		Integer[] keys = {1, null, 3};
		String[] values = {"one", "null-key", "three"};
		
		SimpleUnmodifiableMap<Integer, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		assertEquals("one", map.get(1));
		assertEquals("null-key", map.get(null));
		assertEquals("three", map.get(3));
	}

	//====================================================================================================
	// Thread safety verification
	//====================================================================================================
	@Test
	void e01_concurrentAccess_safe() throws InterruptedException {
		String[] keys = {"key1", null, "key3"};
		String[] values = {"value1", "value2", "value3"};
		
		SimpleUnmodifiableMap<String, String> map = new SimpleUnmodifiableMap<>(keys, values);
		
		// Create multiple threads reading from the map
		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < 1000; j++) {
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
}

