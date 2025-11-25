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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SimpleUnmodifiableMap_Test extends TestBase {

	//====================================================================================================
	// Null key support
	//====================================================================================================
	@Test
	void a01_nullKey_singleEntry() {
		var keys = a((String)null);
		var values = a("value1");

		var map = new SimpleUnmodifiableMap<>(keys, values);

		assertSize(1, map);
		assertEquals("value1", map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a02_nullKey_withOtherKeys() {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		var map = new SimpleUnmodifiableMap<>(keys, values);

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

		var map = new SimpleUnmodifiableMap<>(keys, values);

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

		SimpleUnmodifiableMap<String,String> map = new SimpleUnmodifiableMap<>(keys, values);

		assertSize(1, map);
		assertNull(map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void a05_nullKey_entrySet() {
		var keys = a("key1", null, "key3");
		var values = a("value1", "value2", "value3");

		var map = new SimpleUnmodifiableMap<>(keys, values);

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

		var map = new SimpleUnmodifiableMap<>(keys, values);

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

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b02_duplicateKey_nullKeys() {
		var keys = a(null, "key2", null);
		var values = a("value1", "value2", "value3");

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: null"));
	}

	@Test
	void b03_duplicateKey_mixedNullAndNonNull() {
		var keys = a("key1", null, "key2", "key1");
		var values = a("value1", "value2", "value3", "value4");

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void b04_noDuplicateKeys_success() {
		var keys = a("key1", null, "key2", "key3");
		var values = a("value1", "value2", "value3", "value4");

		var map = assertDoesNotThrow(() -> new SimpleUnmodifiableMap<>(keys, values));

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

		var map = new SimpleUnmodifiableMap<>(keys, values);

		assertThrows(UnsupportedOperationException.class, () -> {
			map.put("key1", "newValue");
		});

		assertEquals("value1", map.get("key1"));
	}

	@Test
	void c02_entrySetValues_cannotModify() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2");

		var map = new SimpleUnmodifiableMap<>(keys, values);

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

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
		assertTrue(ex.getMessage().contains("3")); // keys length
		assertTrue(ex.getMessage().contains("2")); // values length
	}

	@Test
	void d02_arrayLengthMismatch_valuesLonger() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2", "value3", "value4");

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
		assertTrue(ex.getMessage().contains("2")); // keys length
		assertTrue(ex.getMessage().contains("4")); // values length
	}

	@Test
	void d03_arrayLengthMismatch_emptyKeys() {
		var keys = new String[0];
		var values = a("value1");

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
	}

	@Test
	void d04_arrayLengthMismatch_emptyValues() {
		var keys = a("key1", "key2");
		var values = new String[0];

		var ex = assertThrows(IllegalArgumentException.class, () -> {
			new SimpleUnmodifiableMap<>(keys, values);
		});

		assertTrue(ex.getMessage().contains("array lengths differ"));
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================
	@Test
	void e01_emptyMap_noNullKeys() {
		var keys = new String[0];
		var values = new String[0];

		var map = new SimpleUnmodifiableMap<>(keys, values);

		assertEmpty(map);
		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void e02_getLookup_nullKeyNotInMap() {
		var keys = a("key1", "key2");
		var values = a("value1", "value2");

		var map = new SimpleUnmodifiableMap<>(keys, values);

		assertNull(map.get(null));
		assertFalse(map.containsKey(null));
	}

	@Test
	void e03_complexTypes_nullKey() {
		var keys = a(1, null, 3);
		var values = a("one", "null-key", "three");

		var map = new SimpleUnmodifiableMap<>(keys, values);

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

		var map = new SimpleUnmodifiableMap<>(keys, values);

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
}
