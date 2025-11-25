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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class BidiMap_Test extends TestBase {

	//====================================================================================================
	// Basic forward and reverse lookups
	//====================================================================================================

	@Test void a01_basicForwardLookup() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.add("three", 3)
			.build();

		assertEquals(1, map.get("one"));
		assertEquals(2, map.get("two"));
		assertEquals(3, map.get("three"));
		assertNull(map.get("four"));
	}

	@Test void a02_basicReverseLookup() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.add("three", 3)
			.build();

		assertEquals("one", map.getKey(1));
		assertEquals("two", map.getKey(2));
		assertEquals("three", map.getKey(3));
		assertNull(map.getKey(4));
	}

	//====================================================================================================
	// Null handling
	//====================================================================================================

	@Test void a03_nullKeysAndValuesFilteredOut() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add(null, 2)
			.add("three", null)
			.add(null, null)
			.build();

		assertSize(1, map);
		assertEquals(1, map.get("one"));
		assertNull(map.get(null));
		assertNull(map.getKey(null));
	}

	@Test void a04_nullLookups() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		assertNull(map.get(null));
		assertNull(map.getKey(null));
	}

	//====================================================================================================
	// Builder operations
	//====================================================================================================

	@Test void a05_emptyMap() {
		var map = BidiMap.<String,Integer>create().build();

		assertEmpty(map);
		assertEmpty(map);
		assertNull(map.get("anything"));
		assertNull(map.getKey(123));
	}

	@Test void a06_builderChaining() {
		var map = BidiMap.<String,Integer>create()
			.add("a", 1)
			.add("b", 2)
			.add("c", 3)
			.build();

		assertSize(3, map);
		assertEquals(1, map.get("a"));
		assertEquals("b", map.getKey(2));
	}

	//====================================================================================================
	// Map interface - containsKey, containsValue
	//====================================================================================================

	@Test void a07_containsKey() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.build();

		assertTrue(map.containsKey("one"));
		assertTrue(map.containsKey("two"));
		assertFalse(map.containsKey("three"));
		assertFalse(map.containsKey(null));
	}

	@Test void a08_containsValue() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.build();

		assertTrue(map.containsValue(1));
		assertTrue(map.containsValue(2));
		assertFalse(map.containsValue(3));
		assertFalse(map.containsValue(null));
	}

	//====================================================================================================
	// Map interface - put, putAll, remove, clear
	//====================================================================================================

	@Test void a09_put() {
		var map = BidiMap.<String,Integer>create().build();

		assertNull(map.put("one", 1));
		assertEquals(1, map.get("one"));
		assertEquals("one", map.getKey(1));

		assertEquals(1, map.put("one", 10));
		assertEquals(10, map.get("one"));
		assertEquals("one", map.getKey(10));
	}

	@Test void a10_putAll() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		var toAdd = new HashMap<String,Integer>();
		toAdd.put("two", 2);
		toAdd.put("three", 3);

		map.putAll(toAdd);

		assertSize(3, map);
		assertEquals(2, map.get("two"));
		assertEquals("three", map.getKey(3));
	}

	@Test void a11_remove() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.build();

		assertEquals(1, map.remove("one"));
		assertFalse(map.containsKey("one"));
		assertFalse(map.containsValue(1));
		assertNull(map.getKey(1));

		assertNull(map.remove("three"));
	}

	@Test void a12_clear() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.build();

		map.clear();

		assertEmpty(map);
		assertEmpty(map);
		assertNull(map.get("one"));
		assertNull(map.getKey(1));
	}

	//====================================================================================================
	// Map interface - keySet, values, entrySet
	//====================================================================================================

	@Test void a13_keySet() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.add("three", 3)
			.build();

		var keys = map.keySet();

		assertSize(3, keys);
		assertTrue(keys.contains("one"));
		assertTrue(keys.contains("two"));
		assertTrue(keys.contains("three"));
	}

	@Test void a14_values() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.add("three", 3)
			.build();

		var values = map.values();

		assertSize(3, values);
		assertTrue(values.contains(1));
		assertTrue(values.contains(2));
		assertTrue(values.contains(3));
	}

	@Test void a15_entrySet() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.build();

		var entries = map.entrySet();

		assertSize(2, entries);

		boolean foundOne = false;
		boolean foundTwo = false;

		for (var entry : entries) {
			if ("one".equals(entry.getKey()) && Integer.valueOf(1).equals(entry.getValue())) {
				foundOne = true;
			}
			if ("two".equals(entry.getKey()) && Integer.valueOf(2).equals(entry.getValue())) {
				foundTwo = true;
			}
		}

		assertTrue(foundOne);
		assertTrue(foundTwo);
	}

	//====================================================================================================
	// Unmodifiable maps
	//====================================================================================================

	@Test void a16_unmodifiable_put() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.unmodifiable()
			.build();

		assertThrows(UnsupportedOperationException.class, () -> map.put("two", 2));
	}

	@Test void a17_unmodifiable_putAll() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.unmodifiable()
			.build();

		var toAdd = new HashMap<String,Integer>();
		toAdd.put("two", 2);

		assertThrows(UnsupportedOperationException.class, () -> map.putAll(toAdd));
	}

	@Test void a18_unmodifiable_remove() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.unmodifiable()
			.build();

		assertThrows(UnsupportedOperationException.class, () -> map.remove("one"));
	}

	@Test void a19_unmodifiable_clear() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.unmodifiable()
			.build();

		assertThrows(UnsupportedOperationException.class, () -> map.clear());
	}

	@Test void a20_unmodifiable_readOperationsWork() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.unmodifiable()
			.build();

		assertEquals(1, map.get("one"));
		assertEquals("two", map.getKey(2));
		assertSize(2, map);
		assertTrue(map.containsKey("one"));
		assertTrue(map.containsValue(2));
	}

	//====================================================================================================
	// Insertion order preservation
	//====================================================================================================

	@Test void a21_insertionOrderPreserved() {
		var map = BidiMap.<String,Integer>create()
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.build();

		var keys = new ArrayList<>(map.keySet());
		assertEquals("c", keys.get(0));
		assertEquals("a", keys.get(1));
		assertEquals("b", keys.get(2));
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test void a22_overwriteInBuilder() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("one", 10)
			.build();

		assertSize(1, map);
		assertEquals(10, map.get("one"));
		assertEquals("one", map.getKey(10));
		assertNull(map.getKey(1));
	}

	@Test void a23_duplicateValuesInBuilder() {
		// Duplicate values are not allowed
		assertThrows(IllegalArgumentException.class, () ->
			BidiMap.<String,Integer>create()
				.add("one", 1)
				.add("uno", 1)
				.build()
		);
	}

	@Test void a24_sizeAfterOperations() {
		var map = BidiMap.<String,Integer>create().build();

		assertEmpty(map);

		map.put("one", 1);
		assertSize(1, map);

		map.put("two", 2);
		assertSize(2, map);

		map.remove("one");
		assertSize(1, map);

		map.clear();
		assertEmpty(map);
	}

	@Test void a25_differentKeyValueTypes() {
		var map = BidiMap.<Integer,String>create()
			.add(1, "one")
			.add(2, "two")
			.build();

		assertEquals("one", map.get(1));
		assertEquals(Integer.valueOf(2), map.getKey("two"));
	}

	@Test void a26_put_duplicateValueThrowsException() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		// Trying to add a different key with the same value should throw
		assertThrows(IllegalArgumentException.class, () -> map.put("uno", 1));
	}

	@Test void a27_put_sameKeyDifferentValueAllowed() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		// Updating the same key with a different value is allowed
		assertEquals(1, map.put("one", 10));
		assertEquals(10, map.get("one"));
		assertEquals("one", map.getKey(10));
		assertNull(map.getKey(1));
	}

	@Test void a28_putAll_duplicateValueThrowsException() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		var toAdd = new HashMap<String,Integer>();
		toAdd.put("uno", 1);

		// Trying to add entries with duplicate values should throw
		assertThrows(IllegalArgumentException.class, () -> map.putAll(toAdd));
	}

	//====================================================================================================
	// isEmpty
	//====================================================================================================

	@Test void a29_isEmpty_emptyMap() {
		var map = BidiMap.<String,Integer>create().build();

		assertTrue(map.isEmpty());
		assertSize(0, map);
	}

	@Test void a30_isEmpty_nonEmptyMap() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		assertFalse(map.isEmpty());
		assertSize(1, map);
	}

	@Test void a31_isEmpty_afterClear() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.add("two", 2)
			.build();

		assertFalse(map.isEmpty());
		map.clear();
		assertTrue(map.isEmpty());
	}

	@Test void a32_isEmpty_afterRemove() {
		var map = BidiMap.<String,Integer>create()
			.add("one", 1)
			.build();

		assertFalse(map.isEmpty());
		map.remove("one");
		assertTrue(map.isEmpty());
	}
}