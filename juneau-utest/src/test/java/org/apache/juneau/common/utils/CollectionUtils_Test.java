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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CollectionUtils_Test extends TestBase {

	//====================================================================================================
	// treeSet(Set)
	//====================================================================================================
	@Test
	void a01_treeSet_fromSet() {
		LinkedHashSet<String> input = new LinkedHashSet<>(l("c", "a", "b"));
		TreeSet<String> result = toSortedSet(input);

		assertNotNull(result);
		assertEquals(l("a", "b", "c"), new ArrayList<>(result));

		// Null input
		assertNull(toSortedSet((Set<String>)null));
	}

	@Test
	void a02_treeSet_fromSet_numbers() {
		LinkedHashSet<Integer> input = new LinkedHashSet<>(l(3, 1, 2));
		TreeSet<Integer> result = toSortedSet(input);

		assertNotNull(result);
		assertEquals(l(1, 2, 3), new ArrayList<>(result));
	}

	//====================================================================================================
	// treeSet(T...)
	//====================================================================================================
	@Test
	void a03_treeSet_varargs() {
		TreeSet<String> result = sortedSet("c", "a", "b");

		assertNotNull(result);
		assertEquals(l("a", "b", "c"), new ArrayList<>(result));
	}

	@Test
	void a04_treeSet_varargs_empty() {
		TreeSet<String> result = sortedSet();

		assertNotNull(result);
		assertEmpty(result);
	}

	@Test
	void a05_treeSet_varargs_single() {
		TreeSet<String> result = sortedSet("a");

		assertNotNull(result);
		assertEquals(l("a"), new ArrayList<>(result));
	}

	@Test
	void a06_treeSet_varargs_numbers() {
		TreeSet<Integer> result = sortedSet(3, 1, 2, 5, 4);

		assertNotNull(result);
		assertEquals(l(1, 2, 3, 4, 5), new ArrayList<>(result));
	}

	//====================================================================================================
	// map(K,V,K,V,K,V,K,V)
	//====================================================================================================
	@Test
	void a07_map_4pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4);

		assertNotNull(result);
		assertSize(4, result);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertEquals(3, result.get("c"));
		assertEquals(4, result.get("d"));

		// Verify order (LinkedHashMap maintains insertion order)
		var keys = new ArrayList<>(result.keySet());
		assertEquals(l("a", "b", "c", "d"), keys);
	}

	//====================================================================================================
	// map(K,V,K,V,K,V,K,V,K,V)
	//====================================================================================================
	@Test
	void a08_map_5pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);

		assertNotNull(result);
		assertSize(5, result);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertEquals(3, result.get("c"));
		assertEquals(4, result.get("d"));
		assertEquals(5, result.get("e"));

		// Verify order (LinkedHashMap maintains insertion order)
		var keys = new ArrayList<>(result.keySet());
		assertEquals(l("a", "b", "c", "d", "e"), keys);
	}

	@Test
	void a09_map_4pairs_nullValues() {
		LinkedHashMap<String, String> result = map("a", "val1", "b", null, "c", "val3", "d", null);

		assertNotNull(result);
		assertSize(4, result);
		assertEquals("val1", result.get("a"));
		assertNull(result.get("b"));
		assertEquals("val3", result.get("c"));
		assertNull(result.get("d"));
	}

	@Test
	void a10_map_5pairs_nullValues() {
		LinkedHashMap<String, String> result = map("a", "val1", "b", null, "c", "val3", "d", null, "e", "val5");

		assertNotNull(result);
		assertSize(5, result);
		assertEquals("val1", result.get("a"));
		assertNull(result.get("b"));
		assertEquals("val3", result.get("c"));
		assertNull(result.get("d"));
		assertEquals("val5", result.get("e"));
	}

	//====================================================================================================
	// m() - unmodifiable maps
	//====================================================================================================
	@Test
	void a11_m_empty() {
		Map<String, Integer> result = m();

		assertNotNull(result);
		assertEmpty(result);
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 1));
	}

	@Test
	void a12_m_1pair() {
		Map<String, Integer> result = m("a", 1);

		assertNotNull(result);
		assertSize(1, result);
		assertEquals(1, result.get("a"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 2));
	}

	@Test
	void a13_m_2pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2);

		assertNotNull(result);
		assertSize(2, result);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 3));
	}

	@Test
	void a14_m_3pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3);

		assertNotNull(result);
		assertSize(3, result);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertEquals(3, result.get("c"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 4));
	}

	@Test
	void a15_m_4pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4);

		assertNotNull(result);
		assertSize(4, result);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertEquals(3, result.get("c"));
		assertEquals(4, result.get("d"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 5));
	}

	@Test
	void a16_m_5pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);

		assertNotNull(result);
		assertSize(5, result);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertEquals(3, result.get("c"));
		assertEquals(4, result.get("d"));
		assertEquals(5, result.get("e"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 6));
	}

	@Test
	void a17_m_mixedTypes() {
		Map<String, Object> result = m("name", "Alice", "age", 30, "active", true);

		assertNotNull(result);
		assertSize(3, result);
		assertEquals("Alice", result.get("name"));
		assertEquals(30, result.get("age"));
		assertEquals(true, result.get("active"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
	}

	//====================================================================================================
	// map() and m() - 6 to 10 pairs
	//====================================================================================================
	@Test
	void a18_map_6pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);

		assertNotNull(result);
		assertSize(6, result);
		assertEquals(6, result.get("f"));
		assertEquals(l("a", "b", "c", "d", "e", "f"), new ArrayList<>(result.keySet()));
	}

	@Test
	void a19_map_7pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);

		assertNotNull(result);
		assertSize(7, result);
		assertEquals(7, result.get("g"));
	}

	@Test
	void a20_map_8pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);

		assertNotNull(result);
		assertSize(8, result);
		assertEquals(8, result.get("h"));
	}

	@Test
	void a21_map_9pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);

		assertNotNull(result);
		assertSize(9, result);
		assertEquals(9, result.get("i"));
	}

	@Test
	void a22_map_10pairs() {
		LinkedHashMap<String, Integer> result = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);

		assertNotNull(result);
		assertSize(10, result);
		assertEquals(10, result.get("j"));
		assertEquals(l("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), new ArrayList<>(result.keySet()));
	}

	@Test
	void a23_m_6pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);

		assertNotNull(result);
		assertSize(6, result);
		assertEquals(6, result.get("f"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 7));
	}

	@Test
	void a24_m_7pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);

		assertNotNull(result);
		assertSize(7, result);
		assertEquals(7, result.get("g"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 8));
	}

	@Test
	void a25_m_8pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);

		assertNotNull(result);
		assertSize(8, result);
		assertEquals(8, result.get("h"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 9));
	}

	@Test
	void a26_m_9pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);

		assertNotNull(result);
		assertSize(9, result);
		assertEquals(9, result.get("i"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 10));
	}

	@Test
	void a27_m_10pairs() {
		Map<String, Integer> result = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);

		assertNotNull(result);
		assertSize(10, result);
		assertEquals(10, result.get("j"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 11));
	}

	//====================================================================================================
	// m() - null handling
	//====================================================================================================
	@Test
	void a28_m_nullKey() {
		Map<String, Integer> result = m(null, 1);

		assertNotNull(result);
		assertSize(1, result);
		assertEquals(1, result.get(null));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 2));
	}

	@Test
	void a29_m_nullValue() {
		Map<String, Integer> result = m("a", null);

		assertNotNull(result);
		assertSize(1, result);
		assertNull(result.get("a"));
		assertTrue(result.containsKey("a"));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", 2));
	}

	@Test
	void a30_m_multipleNulls() {
		Map<String, String> result = m("a", "val1", "b", null, "c", "val3", null, "val4");

		assertNotNull(result);
		assertSize(4, result);
		assertEquals("val1", result.get("a"));
		assertNull(result.get("b"));
		assertEquals("val3", result.get("c"));
		assertEquals("val4", result.get(null));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
	}

	@Test
	void a31_m_allNulls_duplicateKeys() {
		// This should throw because we have duplicate null keys
		assertThrows(IllegalArgumentException.class, () -> {
			m(null, null, null, null);
		});
	}

	@Test
	void a32_m_singleNullKeyValue() {
		Map<String, String> result = m(null, null);

		assertNotNull(result);
		assertSize(1, result);
		assertNull(result.get(null));
		assertTrue(result.containsKey(null));
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
	}

	//====================================================================================================
	// m() - insertion order preservation
	//====================================================================================================
	@Test
	void a33_m_insertionOrder_simpleKeys() {
		Map<String, Integer> result = m("z", 1, "a", 2, "m", 3, "b", 4);

		assertNotNull(result);
		assertSize(4, result);

		// Verify insertion order is preserved
		var keys = new ArrayList<>(result.keySet());
		assertEquals(list("z", "a", "m", "b"), keys);
	}

	@Test
	void a34_m_insertionOrder_withNullKey() {
		Map<String, Integer> result = m("first", 1, null, 2, "last", 3);

		assertNotNull(result);
		assertSize(3, result);

		// Verify insertion order is preserved, including null
		var keys = new ArrayList<>(result.keySet());
		assertEquals(list("first", null, "last"), keys);
	}

	@Test
	void a35_m_insertionOrder_allPairs() {
		// Test with 10 pairs to ensure order is preserved for larger maps
		Map<Integer, String> result = m(10, "ten", 9, "nine", 8, "eight", 7, "seven",
		                                 6, "six", 5, "five", 4, "four", 3, "three",
		                                 2, "two", 1, "one");

		assertNotNull(result);
		assertSize(10, result);

		// Verify insertion order
		var keys = new ArrayList<>(result.keySet());
		assertEquals(l(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), keys);
	}

	@Test
	void a36_m_insertionOrder_entrySet() {
		Map<String, String> result = m("key1", "val1", "key2", "val2", "key3", "val3");

		// Verify order in entrySet
		List<String> orderedKeys = list();
		for (Map.Entry<String, String> entry : result.entrySet()) {
			orderedKeys.add(entry.getKey());
		}

		assertEquals(l("key1", "key2", "key3"), orderedKeys);
	}

	//====================================================================================================
	// m() - duplicate key detection
	//====================================================================================================
	@Test
	void a37_m_duplicateKey_nonNull() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			m("key1", "value1", "key2", "value2", "key1", "value3");
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: key1"));
	}

	@Test
	void a38_m_duplicateKey_nullKeys() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			m(null, "value1", "key2", "value2", null, "value3");
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: null"));
	}

	@Test
	void a39_m_duplicateKey_inLargerMap() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "a", 6);
		});

		assertTrue(ex.getMessage().contains("Duplicate key found: a"));
	}

	@Test
	void a40_m_noDuplicates_success() {
		Map<String, Integer> result = assertDoesNotThrow(() ->
			m("key1", 1, null, 2, "key3", 3, "key4", 4)
		);

		assertSize(4, result);
		assertEquals(1, result.get("key1"));
		assertEquals(2, result.get(null));
		assertEquals(3, result.get("key3"));
		assertEquals(4, result.get("key4"));
	}

	//====================================================================================================
	// m() - comprehensive integration tests
	//====================================================================================================
	@Test
	void a41_m_nullKey_preservesOrder_immutable() {
		Map<String, String> result = m("first", "1st", null, "null-value", "last", "final");

		// Check null key works
		assertEquals("null-value", result.get(null));

		// Check insertion order
		var keys = new ArrayList<>(result.keySet());
		assertEquals(list("first", null, "last"), keys);

		// Check immutability
		assertThrows(UnsupportedOperationException.class, () -> result.put("new", "value"));
		assertThrows(UnsupportedOperationException.class, () -> result.remove("first"));
		assertThrows(UnsupportedOperationException.class, () -> result.clear());
	}

	@Test
	void a42_m_complexScenario() {
		// Test with various types, nulls, and verify all properties
		Map<Object, Object> result = m(
			"string", "value",
			null, "null-key",
			42, "number",
			true, "boolean"
		);

		// Verify all entries
		assertSize(4, result);
		assertEquals("value", result.get("string"));
		assertEquals("null-key", result.get(null));
		assertEquals("number", result.get(42));
		assertEquals("boolean", result.get(true));

		// Verify order
		var keys = new ArrayList<>(result.keySet());
		assertEquals(list("string", null, 42, true), keys);

		// Verify immutability
		assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
	}

	//====================================================================================================
	// addAll(T[], T...) - Array utilities from deprecated ArrayUtils
	//====================================================================================================
	@Test
	void a43_addAll_arrayToArray() {
		String[] s = {};

		s = addAll(s, "a", "b");
		assertList(s, "a", "b");

		s = addAll(s, "c");
		assertList(s, "a", "b", "c");

		s = addAll(s);
		assertList(s, "a", "b", "c");

		var o = addAll((Object[])null);
		assertEmpty(o);

		s = addAll((String[])null, "a", "b");
		assertList(s, "a", "b");
	}

	//====================================================================================================
	// toSet(T[]) - Array utilities from deprecated ArrayUtils
	//====================================================================================================
	@Test
	void a44_toSet_fromArray() {
		assertThrows(IllegalArgumentException.class, ()->toSet((String[])null));

		var s = a("a");
		var i = toSet(s).iterator();
		assertEquals("a", i.next());

		assertThrows(UnsupportedOperationException.class, i::remove);
		assertThrows(NoSuchElementException.class, i::next);
	}

	//====================================================================================================
	// combine(T[]...) - Array utilities from deprecated ArrayUtils
	//====================================================================================================
	@Test
	void a45_combine_arrays() {
		var s1 = a("a");
		var s2 = a("b");

		assertList(combine(s1, s2), "a", "b");
		assertList(combine(s1), "a");
		assertList(combine(s2), "b");
		assertList(combine(s1,null), "a");
		assertList(combine(null,s2), "b");
		assertNull(combine(null,null));
		assertNull(combine());
	}
}


