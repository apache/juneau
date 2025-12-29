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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HashKey_Test extends TestBase {

	//====================================================================================================
	// Static factory method - of(Object...)
	//====================================================================================================

	@Test
	void a01_of_emptyArray() {
		HashKey key = HashKey.of();
		assertNotNull(key);
		// Arrays.hashCode(new Object[0]) == 1
		assertEquals(1, key.hashCode());
	}

	@Test
	void a02_of_singleValue() {
		HashKey key1 = HashKey.of("test");
		HashKey key2 = HashKey.of("test");
		assertNotNull(key1);
		// Equal keys should have equal hash codes
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	void a03_of_multipleValues() {
		HashKey key = HashKey.of("a", "b", 42, true);
		assertNotNull(key);
	}

	@Test
	void a04_of_withNullValues() {
		HashKey key = HashKey.of("a", null, "b", null);
		assertNotNull(key);
	}

	@Test
	void a05_of_allNullValues() {
		HashKey key = HashKey.of((Object)null, null, null);
		assertNotNull(key);
	}

	@Test
	void a06_of_variousTypes() {
		HashKey key = HashKey.of(
			"string",
			42,
			true,
			3.14,
			'c',
			Integer.valueOf(100),
			new ArrayList<>(),
			new HashMap<>()
		);
		assertNotNull(key);
	}

	//====================================================================================================
	// equals(Object) method
	//====================================================================================================

	@Test
	void b01_equals_sameValues() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		assertEquals(key1, key2);
		assertTrue(key1.equals(key2));
		assertTrue(key2.equals(key1));
	}

	@Test
	void b02_equals_differentValues() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 43);
		assertNotEquals(key1, key2);
		assertFalse(key1.equals(key2));
	}

	@Test
	void b03_equals_differentOrder() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("b", "a", 42);
		assertNotEquals(key1, key2);
		assertFalse(key1.equals(key2));
	}

	@Test
	void b04_equals_differentLengths() {
		HashKey key1 = HashKey.of("a", "b");
		HashKey key2 = HashKey.of("a", "b", "c");
		assertNotEquals(key1, key2);
		assertFalse(key1.equals(key2));
	}

	@Test
	void b05_equals_withNullValues() {
		HashKey key1 = HashKey.of("a", null, "b");
		HashKey key2 = HashKey.of("a", null, "b");
		assertEquals(key1, key2);
	}

	@Test
	void b06_equals_nullVsNonNull() {
		HashKey key1 = HashKey.of("a", null, "b");
		HashKey key2 = HashKey.of("a", "notnull", "b");
		assertNotEquals(key1, key2);
	}

	@Test
	void b07_equals_emptyKeys() {
		HashKey key1 = HashKey.of();
		HashKey key2 = HashKey.of();
		assertEquals(key1, key2);
	}

	@Test
	void b08_equals_singleValue() {
		HashKey key1 = HashKey.of("test");
		HashKey key2 = HashKey.of("test");
		assertEquals(key1, key2);
	}

	@Test
	void b09_equals_differentSingleValues() {
		HashKey key1 = HashKey.of("test1");
		HashKey key2 = HashKey.of("test2");
		assertNotEquals(key1, key2);
	}

	@Test
	void b10_equals_withNullObject() {
		HashKey key = HashKey.of("a", "b");
		// equals() now checks for null and returns false
		assertFalse(key.equals(null));
		assertNotEquals(key, null);
	}

	@Test
	void b11_equals_withNonHashKeyObject() {
		HashKey key = HashKey.of("a", "b");
		// equals() now checks type and returns false for non-HashKey objects
		assertFalse(key.equals("not a HashKey"));
		assertNotEquals(key, "not a HashKey");
	}

	@Test
	void b12_equals_reflexive() {
		HashKey key = HashKey.of("a", "b", 42);
		assertEquals(key, key);
		assertTrue(key.equals(key));
	}

	@Test
	void b13_equals_symmetric() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		assertEquals(key1, key2);
		assertEquals(key2, key1);
	}

	@Test
	void b14_equals_transitive() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		HashKey key3 = HashKey.of("a", "b", 42);
		assertEquals(key1, key2);
		assertEquals(key2, key3);
		assertEquals(key1, key3);
	}

	@Test
	void b15_equals_differentTypes() {
		HashKey key1 = HashKey.of("42");
		HashKey key2 = HashKey.of(42);
		assertNotEquals(key1, key2);
	}

	@Test
	void b16_equals_arrays() {
		String[] arr1 = {"a", "b"};
		String[] arr2 = {"a", "b"};
		HashKey key1 = HashKey.of((Object)arr1);
		HashKey key2 = HashKey.of((Object)arr2);
		// Arrays with same contents should be equal (ne() uses deep equality)
		assertTrue(key1.equals(key2), "Arrays with same contents should be equal");
		// And should have same hash code (using Arrays.deepHashCode())
		assertEquals(key1.hashCode(), key2.hashCode(),
			"Equal HashKeys must have equal hash codes");
	}

	//====================================================================================================
	// hashCode() method
	//====================================================================================================

	@Test
	void c01_hashCode_equalKeysHaveEqualHashCodes() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	void c02_hashCode_differentKeysMayHaveDifferentHashCodes() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 43);
		// Different keys should have different hash codes (though collisions are possible)
		assertNotEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	void c03_hashCode_consistent() {
		HashKey key = HashKey.of("a", "b", 42);
		int hashCode1 = key.hashCode();
		int hashCode2 = key.hashCode();
		assertEquals(hashCode1, hashCode2);
	}

	@Test
	void c04_hashCode_emptyKey() {
		HashKey key1 = HashKey.of();
		HashKey key2 = HashKey.of();
		// Empty keys should have the same hash code
		assertEquals(key1.hashCode(), key2.hashCode());
		// And should equal Arrays.hashCode(new Object[0])
		assertEquals(java.util.Arrays.hashCode(new Object[0]), key1.hashCode());
	}

	@Test
	void c05_hashCode_withNullValues() {
		HashKey key1 = HashKey.of("a", null, "b");
		HashKey key2 = HashKey.of("a", null, "b");
		assertEquals(key1.hashCode(), key2.hashCode());
	}

	@Test
	void c06_hashCode_orderMatters() {
		HashKey key1 = HashKey.of("a", "b");
		HashKey key2 = HashKey.of("b", "a");
		// Different order should produce different hash codes
		assertNotEquals(key1.hashCode(), key2.hashCode());
	}

	//====================================================================================================
	// Usage in HashMap
	//====================================================================================================

	@Test
	void d01_hashMap_putAndGet() {
		Map<HashKey, String> map = new HashMap<>();
		HashKey key = HashKey.of("a", "b", 42);
		map.put(key, "value");
		assertEquals("value", map.get(key));
	}

	@Test
	void d02_hashMap_equivalentKeys() {
		Map<HashKey, String> map = new HashMap<>();
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		map.put(key1, "value");
		assertEquals("value", map.get(key2));
	}

	@Test
	void d03_hashMap_differentKeys() {
		Map<HashKey, String> map = new HashMap<>();
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 43);
		map.put(key1, "value1");
		map.put(key2, "value2");
		assertEquals("value1", map.get(key1));
		assertEquals("value2", map.get(key2));
	}

	@Test
	void d04_hashMap_multipleEntries() {
		Map<HashKey, Integer> map = new HashMap<>();
		map.put(HashKey.of("a"), 1);
		map.put(HashKey.of("b"), 2);
		map.put(HashKey.of("c"), 3);
		assertEquals(3, map.size());
		assertEquals(1, map.get(HashKey.of("a")));
		assertEquals(2, map.get(HashKey.of("b")));
		assertEquals(3, map.get(HashKey.of("c")));
	}

	@Test
	void d05_hashMap_withNullValues() {
		Map<HashKey, String> map = new HashMap<>();
		HashKey key = HashKey.of("a", null, "b");
		map.put(key, "value");
		assertEquals("value", map.get(HashKey.of("a", null, "b")));
	}

	@Test
	void d06_hashMap_emptyKey() {
		Map<HashKey, String> map = new HashMap<>();
		HashKey key = HashKey.of();
		map.put(key, "value");
		assertEquals("value", map.get(HashKey.of()));
	}

	//====================================================================================================
	// Usage in HashSet
	//====================================================================================================

	@Test
	void e01_hashSet_add() {
		Set<HashKey> set = new HashSet<>();
		HashKey key = HashKey.of("a", "b", 42);
		assertTrue(set.add(key));
		assertTrue(set.contains(key));
	}

	@Test
	void e02_hashSet_equivalentKeys() {
		Set<HashKey> set = new HashSet<>();
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		assertTrue(set.add(key1));
		assertFalse(set.add(key2)); // Should not add duplicate
		assertEquals(1, set.size());
		assertTrue(set.contains(key2));
	}

	@Test
	void e03_hashSet_differentKeys() {
		Set<HashKey> set = new HashSet<>();
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 43);
		assertTrue(set.add(key1));
		assertTrue(set.add(key2));
		assertEquals(2, set.size());
	}

	//====================================================================================================
	// toString() method
	//====================================================================================================

	@Test
	void f01_toString_notNull() {
		HashKey key = HashKey.of("a", "b", 42);
		String str = key.toString();
		assertNotNull(str);
		assertFalse(str.isEmpty());
	}

	@Test
	void f02_toString_containsHashCode() {
		HashKey key = HashKey.of("a", "b", 42);
		String str = key.toString();
		assertTrue(str.contains("hashCode"));
	}

	@Test
	void f03_toString_containsArray() {
		HashKey key = HashKey.of("a", "b", 42);
		String str = key.toString();
		assertTrue(str.contains("array"));
	}

	@Test
	void f04_toString_emptyKey() {
		HashKey key = HashKey.of();
		String str = key.toString();
		assertNotNull(str);
	}

	//====================================================================================================
	// Immutability
	//====================================================================================================

	@Test
	void g01_immutability_hashCodeConsistent() {
		HashKey key = HashKey.of("a", "b", 42);
		int hashCode1 = key.hashCode();
		int hashCode2 = key.hashCode();
		assertEquals(hashCode1, hashCode2);
	}

	@Test
	void g02_immutability_equalsConsistent() {
		HashKey key1 = HashKey.of("a", "b", 42);
		HashKey key2 = HashKey.of("a", "b", 42);
		boolean equals1 = key1.equals(key2);
		boolean equals2 = key1.equals(key2);
		assertEquals(equals1, equals2);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void h01_edgeCase_veryLongArray() {
		Object[] values = new Object[1000];
		for (int i = 0; i < 1000; i++) {
			values[i] = i;
		}
		HashKey key = HashKey.of(values);
		assertNotNull(key);
		assertEquals(key, HashKey.of(values));
	}

	@Test
	void h02_edgeCase_nestedHashKeys() {
		HashKey innerKey = HashKey.of("inner");
		HashKey outerKey = HashKey.of("outer", innerKey);
		assertNotNull(outerKey);
	}

	@Test
	void h03_edgeCase_mixedPrimitives() {
		HashKey key = HashKey.of(
			(byte)1,
			(short)2,
			3,
			4L,
			5.0f,
			6.0d,
			true,
			'c'
		);
		assertNotNull(key);
		assertEquals(key, HashKey.of((byte)1, (short)2, 3, 4L, 5.0f, 6.0d, true, 'c'));
	}

	@Test
	void h04_edgeCase_collections() {
		List<String> list = Arrays.asList("a", "b");
		Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
		HashKey key = HashKey.of(list, set);
		assertNotNull(key);
	}
}

