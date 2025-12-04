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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.junit.jupiter.api.*;

class CollectionUtils_Test extends TestBase {

	//====================================================================================================
	// Constructor (line 126)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 126: class instantiation
		// CollectionUtils has a private constructor, so it cannot be instantiated.
		// Line 126 (class declaration) is covered by using the class's static methods.
	}

	//====================================================================================================
	// a(T...)
	//====================================================================================================
	@Test
	void a001_a() {
		String[] result = a("a", "b", "c");
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals("a", result[0]);
		assertEquals("b", result[1]);
		assertEquals("c", result[2]);

		// Empty array
		String[] empty = a();
		assertNotNull(empty);
		assertEquals(0, empty.length);
	}

	//====================================================================================================
	// a2(E[]...)
	//====================================================================================================
	@Test
	void a002_a2() {
		String[][] result = a2(a("a", "b"), a("c", "d"));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals(2, result[0].length);
		assertEquals("a", result[0][0]);
		assertEquals("b", result[0][1]);
		assertEquals("c", result[1][0]);
		assertEquals("d", result[1][1]);

		// Single row
		String[][] single = a2(a("x", "y", "z"));
		assertEquals(1, single.length);
		assertEquals(3, single[0].length);

		// Empty
		String[][] empty = a2();
		assertNotNull(empty);
		assertEquals(0, empty.length);
	}

	//====================================================================================================
	// accumulate(Object)
	//====================================================================================================
	@Test
	void a003_accumulate() {
		List<String> list = list("a", "b", "c");
		List<?> result = accumulate(list);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertTrue(result.contains("c"));
	}

	//====================================================================================================
	// addAll(List<E>, E...)
	//====================================================================================================
	@Test
	void a004_addAll_listVarargs() {
		// Null value creates new list
		var result1 = addAll((List<String>)null, "a", "b");
		assertNotNull(result1);
		assertEquals(list("a", "b"), result1);

		// Existing list adds to it
		var list = list("x");
		var result2 = addAll(list, "a", "b");
		assertSame(list, result2);
		assertEquals(list("x", "a", "b"), result2);

		// Null entries returns original
		var list2 = list("a", "b");
		var result3 = addAll(list2, (String[])null);
		assertSame(list2, result3);
	}

	//====================================================================================================
	// addAll(List<E>, List<E>)
	//====================================================================================================
	@Test
	void a005_addAll_listToList() {
		// Null entries returns original
		var list = list("a", "b");
		var result1 = addAll(list, (List<String>)null);
		assertSame(list, result1);
		assertEquals(list("a", "b"), result1);

		// Null value creates copy
		var entries = list("x", "y", "z");
		var result2 = addAll(null, entries);
		assertNotNull(result2);
		assertEquals(list("x", "y", "z"), result2);
		assertNotSame(entries, result2);

		// Both not null adds to existing
		var list2 = list("a", "b");
		var entries2 = list("c", "d");
		var result3 = addAll(list2, entries2);
		assertSame(list2, result3);
		assertEquals(list("a", "b", "c", "d"), result3);
	}

	//====================================================================================================
	// addAll(Set<E>, E...)
	//====================================================================================================
	@Test
	void a006_addAll_setVarargs() {
		// Null entries returns original
		var set = set("a", "b");
		var result1 = addAll(set, (String[])null);
		assertSame(set, result1);
		assertEquals(set("a", "b"), result1);

		// Null value creates new set
		var result2 = addAll((Set<String>)null, "x", "y", "z");
		assertNotNull(result2);
		assertEquals(set("x", "y", "z"), result2);

		// Both not null adds to existing
		var set2 = set("a", "b");
		var result3 = addAll(set2, "c", "d");
		assertSame(set2, result3);
		assertEquals(set("a", "b", "c", "d"), result3);
	}

	//====================================================================================================
	// addAll(SortedSet<E>, E...)
	//====================================================================================================
	@Test
	void a007_addAll_sortedSetVarargs() {
		// Null entries returns original
		var set = sortedSet("a", "b");
		var result1 = addAll(set, (String[])null);
		assertSame(set, result1);
		assertEquals(sortedSet("a", "b"), result1);

		// Null value creates new sorted set
		var result2 = addAll((SortedSet<String>)null, "x", "y", "z");
		assertNotNull(result2);
		assertEquals(sortedSet("x", "y", "z"), result2);

		// Both not null adds to existing
		var set2 = sortedSet("a", "b");
		var result3 = addAll(set2, "c", "d");
		assertSame(set2, result3);
		assertEquals(sortedSet("a", "b", "c", "d"), result3);
	}

	//====================================================================================================
	// addAll(T[], T...)
	//====================================================================================================
	@Test
	void a008_addAll_array() {
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
	// ao(Object...)
	//====================================================================================================
	@Test
	void a009_ao() {
		Object[] result = ao("string", 42, true, 3.14, null);
		assertNotNull(result);
		assertEquals(5, result.length);
		assertEquals("string", result[0]);
		assertEquals(42, result[1]);
		assertEquals(true, result[2]);
		assertEquals(3.14, result[3]);
		assertNull(result[4]);

		// Empty
		Object[] empty = ao();
		assertNotNull(empty);
		assertEquals(0, empty.length);
	}

	//====================================================================================================
	// array(Class<E>, int)
	//====================================================================================================
	@Test
	void a010_array_classLength() {
		String[] result = array(String.class, 5);
		assertNotNull(result);
		assertEquals(5, result.length);
		for (String s : result) {
			assertNull(s);
		}
	}

	//====================================================================================================
	// array(Collection<E>, Class<E>)
	//====================================================================================================
	@Test
	void a011_array_collection() {
		List<String> list = list("a", "b", "c");
		String[] result = array(list, String.class);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals("a", result[0]);
		assertEquals("b", result[1]);
		assertEquals("c", result[2]);
	}

	//====================================================================================================
	// arrayToList(Object)
	//====================================================================================================
	@Test
	void a012_arrayToList() {
		String[] arr = {"a", "b", "c"};
		List<Object> result = arrayToList(arr);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
		assertEquals("b", result.get(1));
		assertEquals("c", result.get(2));

		// Primitive array
		int[] intArr = {1, 2, 3};
		List<Object> intResult = arrayToList(intArr);
		assertEquals(3, intResult.size());
		assertEquals(1, intResult.get(0));
		assertEquals(2, intResult.get(1));
		assertEquals(3, intResult.get(2));

		// Test lines 431-433: char array
		char[] charArr = {'a', 'b', 'c'};
		List<Object> charResult = arrayToList(charArr);
		assertEquals(3, charResult.size());
		assertEquals('a', charResult.get(0));
		assertEquals('b', charResult.get(1));
		assertEquals('c', charResult.get(2));
	}

	//====================================================================================================
	// booleans(boolean...)
	//====================================================================================================
	@Test
	void a013_booleans() {
		boolean[] result = booleans(true, false, true);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertTrue(result[0]);
		assertFalse(result[1]);
		assertTrue(result[2]);
	}

	//====================================================================================================
	// bytes(int...)
	//====================================================================================================
	@Test
	void a014_bytes() {
		byte[] result = bytes(1, 2, 3);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	//====================================================================================================
	// chars(char...)
	//====================================================================================================
	@Test
	void a015_chars() {
		char[] result = chars('a', 'b', 'c');
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals('a', result[0]);
		assertEquals('b', result[1]);
		assertEquals('c', result[2]);
	}

	//====================================================================================================
	// combine(E[]...)
	//====================================================================================================
	@Test
	void a016_combine() {
		var s1 = a("a");
		var s2 = a("b");

		assertList(combine(s1, s2), "a", "b");
		assertList(combine(s1), "a");
		assertList(combine(s2), "b");
		assertList(combine(s1, null), "a");
		assertList(combine(null, s2), "b");
		assertNull(combine(null, null));
		assertNull(combine());
	}

	//====================================================================================================
	// contains(T, T[])
	//====================================================================================================
	@Test
	void a017_contains() {
		String[] arr = {"a", "b", "c"};
		assertTrue(contains("a", arr));
		assertTrue(contains("b", arr));
		assertFalse(contains("d", arr));
		assertFalse(contains(null, arr));
		assertFalse(contains("a", null));
	}

	//====================================================================================================
	// copyArrayToList(Object, List)
	//====================================================================================================
	@Test
	void a018_copyArrayToList() {
		String[] arr = {"a", "b", "c"};
		List<Object> list = new ArrayList<>();
		var result = copyArrayToList(arr, list);
		assertSame(list, result);
		assertEquals(3, list.size());
		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));

		// Null array
		List<Object> list2 = new ArrayList<>();
		copyArrayToList(null, list2);
		assertTrue(list2.isEmpty());
	}

	//====================================================================================================
	// copyOf(Collection<E>)
	//====================================================================================================
	@Test
	void a019_copyOf_collection() {
		Collection<String> col = list("a", "b", "c");
		Collection<String> result = copyOf(col);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertTrue(result.contains("c"));
		assertNotSame(col, result);

		assertNull(copyOf((Collection<String>)null));
	}

	//====================================================================================================
	// copyOf(List<E>)
	//====================================================================================================
	@Test
	void a020_copyOf_list() {
		List<String> list = list("a", "b", "c");
		ArrayList<String> result = copyOf(list);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
		assertEquals("b", result.get(1));
		assertEquals("c", result.get(2));
		assertNotSame(list, result);

		assertNull(copyOf((List<String>)null));
	}

	//====================================================================================================
	// copyOf(List<E>, Function)
	//====================================================================================================
	@Test
	void a021_copyOf_listFunction() {
		List<String> list = list("a", "b", "c");
		List<String> result = copyOf(list, s -> s.toUpperCase());
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("A", result.get(0));
		assertEquals("B", result.get(1));
		assertEquals("C", result.get(2));

		assertNull(copyOf((List<String>)null, s -> s));
	}

	//====================================================================================================
	// copyOf(List<E>, Function, Supplier)
	//====================================================================================================
	@Test
	void a022_copyOf_listFunctionSupplier() {
		List<String> list = list("a", "b");
		List<String> result = copyOf(list, s -> s.toUpperCase(), LinkedList::new);
		assertNotNull(result);
		assertTrue(result instanceof LinkedList);
		assertEquals(2, result.size());
		assertEquals("A", result.get(0));
		assertEquals("B", result.get(1));
	}

	//====================================================================================================
	// copyOf(Map<K,V>)
	//====================================================================================================
	@Test
	void a023_copyOf_map() {
		Map<String, Integer> map = map("a", 1, "b", 2);
		Map<String, Integer> result = copyOf(map);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
		assertNotSame(map, result);

		assertNull(copyOf((Map<String, Integer>)null));
	}

	//====================================================================================================
	// copyOf(Map<K,V>, Function)
	//====================================================================================================
	@Test
	void a024_copyOf_mapFunction() {
		Map<String, Integer> map = map("a", 1, "b", 2);
		Map<String, Integer> result = copyOf(map, v -> v * 2);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(2, result.get("a"));
		assertEquals(4, result.get("b"));
	}

	//====================================================================================================
	// copyOf(Map<K,V>, Function, Supplier)
	//====================================================================================================
	@Test
	void a025_copyOf_mapFunctionSupplier() {
		Map<String, Integer> map = map("a", 1);
		Map<String, Integer> result = copyOf(map, v -> v, TreeMap::new);
		assertNotNull(result);
		assertTrue(result instanceof TreeMap);
		assertEquals(1, result.size());

		// Test line 647: null map returns null
		assertNull(copyOf((Map<String, Integer>)null, v -> v, TreeMap::new));
	}

	//====================================================================================================
	// copyOf(Set<E>)
	//====================================================================================================
	@Test
	void a026_copyOf_set() {
		Set<String> set = set("a", "b", "c");
		Set<String> result = copyOf(set);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertNotSame(set, result);

		assertNull(copyOf((Set<String>)null));
	}

	//====================================================================================================
	// copyOf(Set<E>, Function)
	//====================================================================================================
	@Test
	void a027_copyOf_setFunction() {
		Set<String> set = set("a", "b");
		Set<String> result = copyOf(set, s -> s.toUpperCase());
		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.contains("A"));
		assertTrue(result.contains("B"));
	}

	//====================================================================================================
	// copyOf(Set<E>, Function, Supplier)
	//====================================================================================================
	@Test
	void a028_copyOf_setFunctionSupplier() {
		Set<String> set = set("a", "b");
		Set<String> result = copyOf(set, s -> s, TreeSet::new);
		assertNotNull(result);
		assertTrue(result instanceof TreeSet);
		assertEquals(2, result.size());
	}

	//====================================================================================================
	// copyOf(T[])
	//====================================================================================================
	@Test
	void a029_copyOf_array() {
		String[] arr = {"a", "b", "c"};
		String[] result = copyOf(arr);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals("a", result[0]);
		assertNotSame(arr, result);

		assertNull(copyOf((String[])null));
	}

	//====================================================================================================
	// doubles(double...)
	//====================================================================================================
	@Test
	void a030_doubles() {
		double[] result = doubles(1.0, 2.0, 3.0);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals(1.0, result[0]);
		assertEquals(2.0, result[1]);
		assertEquals(3.0, result[2]);
	}

	//====================================================================================================
	// first(List<E>)
	//====================================================================================================
	@Test
	void a031_first() {
		List<String> list = list("a", "b", "c");
		assertEquals("a", first(list));

		assertNull(first(null));
		assertNull(first(list()));
	}

	//====================================================================================================
	// floats(float...)
	//====================================================================================================
	@Test
	void a032_floats() {
		float[] result = floats(1.0f, 2.0f, 3.0f);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals(1.0f, result[0]);
		assertEquals(2.0f, result[1]);
		assertEquals(3.0f, result[2]);
	}

	//====================================================================================================
	// forEachReverse(E[], Consumer)
	//====================================================================================================
	@Test
	void a033_forEachReverse_array() {
		String[] arr = {"a", "b", "c"};
		List<String> result = new ArrayList<>();
		forEachReverse(arr, result::add);
		assertEquals(list("c", "b", "a"), result);
	}

	//====================================================================================================
	// forEachReverse(List<E>, Consumer)
	//====================================================================================================
	@Test
	void a034_forEachReverse_list() {
		List<String> list = list("a", "b", "c");
		List<String> result = new ArrayList<>();
		forEachReverse(list, result::add);
		assertEquals(list("c", "b", "a"), result);

		// Test lines 762-764: non-ArrayList List uses ListIterator
		LinkedList<String> linkedList = new LinkedList<>(list("x", "y", "z"));
		List<String> result2 = new ArrayList<>();
		forEachReverse(linkedList, result2::add);
		assertEquals(list("z", "y", "x"), result2);
	}

	//====================================================================================================
	// indexOf(T, T[])
	//====================================================================================================
	@Test
	void a035_indexOf() {
		String[] arr = {"a", "b", "c"};
		assertEquals(0, indexOf("a", arr));
		assertEquals(1, indexOf("b", arr));
		assertEquals(2, indexOf("c", arr));
		assertEquals(-1, indexOf("d", arr));
		assertEquals(-1, indexOf(null, arr));
		assertEquals(-1, indexOf("a", null));
	}

	//====================================================================================================
	// ints(int...)
	//====================================================================================================
	@Test
	void a036_ints() {
		int[] result = ints(1, 2, 3);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	//====================================================================================================
	// isEmptyArray(Object[])
	//====================================================================================================
	@Test
	void a037_isEmptyArray() {
		assertTrue(isEmptyArray(null));
		assertTrue(isEmptyArray(new String[0]));
		assertFalse(isEmptyArray(new String[]{"a"}));
	}

	//====================================================================================================
	// isNotEmptyArray(Object[])
	//====================================================================================================
	@Test
	void a038_isNotEmptyArray() {
		assertFalse(isNotEmptyArray(null));
		assertFalse(isNotEmptyArray(new String[0]));
		assertTrue(isNotEmptyArray(new String[]{"a"}));
	}

	//====================================================================================================
	// l(T...)
	//====================================================================================================
	@Test
	void a039_l() {
		List<String> result = l("a", "b", "c");
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
		assertThrows(UnsupportedOperationException.class, () -> result.add("d"));

		assertNull(l((String[])null));
	}

	//====================================================================================================
	// last(E[])
	//====================================================================================================
	@Test
	void a040_last_array() {
		String[] arr = {"a", "b", "c"};
		assertEquals("c", last(arr));
		String[] nullArr = null;
		assertNull(last(nullArr));
		assertNull(last(new String[0]));
	}

	//====================================================================================================
	// last(List<E>)
	//====================================================================================================
	@Test
	void a041_last_list() {
		List<String> list = list("a", "b", "c");
		assertEquals("c", last(list));
		List<String> nullList = null;
		assertNull(last(nullList));
		assertNull(last(list()));
	}

	//====================================================================================================
	// length(Object)
	//====================================================================================================
	@Test
	void a042_length() {
		String[] arr = {"a", "b", "c"};
		assertEquals(3, length(arr));
		assertEquals(0, length(null));
		int[] intArr = {1, 2, 3, 4};
		assertEquals(4, length(intArr));
	}

	//====================================================================================================
	// list(T...)
	//====================================================================================================
	@Test
	void a043_list() {
		List<String> result = list("a", "b", "c");
		assertNotNull(result);
		assertTrue(result instanceof ArrayList);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
		result.add("d"); // Modifiable
		assertEquals(4, result.size());
	}

	//====================================================================================================
	// listb(Class<E>, Converter...)
	//====================================================================================================
	@Test
	void a044_listb() {
		ListBuilder<String> builder = listb(String.class);
		assertNotNull(builder);
		List<String> result = builder.add("a").add("b").build();
		assertEquals(2, result.size());
		assertEquals("a", result.get(0));
		assertEquals("b", result.get(1));
	}

	//====================================================================================================
	// liste()
	//====================================================================================================
	@Test
	void a045_liste() {
		List<String> result = liste();
		assertNotNull(result);
		assertTrue(result.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> result.add("a"));
	}

	//====================================================================================================
	// liste(Class<T>)
	//====================================================================================================
	@Test
	void a046_liste_class() {
		List<String> result = liste(String.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> result.add("a"));
	}

	//====================================================================================================
	// listn(Class<T>)
	//====================================================================================================
	@Test
	void a047_listn() {
		assertNull(listn(String.class));
	}

	//====================================================================================================
	// listOf(Class<E>, E...)
	//====================================================================================================
	@Test
	void a048_listOf() {
		List<String> result = listOf(String.class, "a", "b", "c");
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
	}

	//====================================================================================================
	// listOfSize(int)
	//====================================================================================================
	@Test
	void a049_listOfSize() {
		ArrayList<String> result = listOfSize(5);
		assertNotNull(result);
		assertEquals(0, result.size());
		result.add("a");
		assertEquals(1, result.size());
	}

	//====================================================================================================
	// longs(long...)
	//====================================================================================================
	@Test
	void a050_longs() {
		long[] result = longs(1L, 2L, 3L);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals(1L, result[0]);
		assertEquals(2L, result[1]);
		assertEquals(3L, result[2]);
	}

	//====================================================================================================
	// m() - all overloads
	//====================================================================================================
	@Test
	void a051_m() {
		// Empty
		Map<String, Integer> empty = m();
		assertNotNull(empty);
		assertTrue(empty.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> empty.put("x", 1));

		// 1 pair
		Map<String, Integer> m1 = m("a", 1);
		assertEquals(1, m1.size());
		assertEquals(1, m1.get("a"));

		// 2 pairs
		Map<String, Integer> m2 = m("a", 1, "b", 2);
		assertEquals(2, m2.size());
		assertEquals(1, m2.get("a"));
		assertEquals(2, m2.get("b"));

		// 3 pairs
		Map<String, Integer> m3 = m("a", 1, "b", 2, "c", 3);
		assertEquals(3, m3.size());

		// 4 pairs
		Map<String, Integer> m4 = m("a", 1, "b", 2, "c", 3, "d", 4);
		assertEquals(4, m4.size());

		// 5 pairs
		Map<String, Integer> m5 = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
		assertEquals(5, m5.size());

		// 6 pairs
		Map<String, Integer> m6 = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
		assertEquals(6, m6.size());

		// 7 pairs
		Map<String, Integer> m7 = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
		assertEquals(7, m7.size());

		// 8 pairs
		Map<String, Integer> m8 = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
		assertEquals(8, m8.size());

		// 9 pairs
		Map<String, Integer> m9 = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
		assertEquals(9, m9.size());

		// 10 pairs
		Map<String, Integer> m10 = m("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);
		assertEquals(10, m10.size());

		// Null handling
		Map<String, Integer> nullKey = m(null, 1);
		assertEquals(1, nullKey.get(null));

		Map<String, Integer> nullValue = m("a", null);
		assertNull(nullValue.get("a"));
		assertTrue(nullValue.containsKey("a"));

		// Duplicate key detection
		assertThrowsWithMessage(IllegalArgumentException.class, "Duplicate key found: key1", () -> {
			m("key1", "value1", "key2", "value2", "key1", "value3");
		});

		// Insertion order preservation
		Map<String, Integer> ordered = m("z", 1, "a", 2, "m", 3, "b", 4);
		var keys = new ArrayList<>(ordered.keySet());
		assertEquals(list("z", "a", "m", "b"), keys);

		// Immutability
		assertThrows(UnsupportedOperationException.class, () -> m1.put("x", 2));
		assertThrows(UnsupportedOperationException.class, () -> m1.remove("a"));
		assertThrows(UnsupportedOperationException.class, () -> m1.clear());
	}

	//====================================================================================================
	// map() - all overloads
	//====================================================================================================
	@Test
	void a052_map() {
		// Empty
		LinkedHashMap<String, Integer> empty = map();
		assertNotNull(empty);
		assertTrue(empty.isEmpty());
		empty.put("x", 1); // Modifiable
		assertEquals(1, empty.size());

		// 1 pair
		LinkedHashMap<String, Integer> m1 = map("a", 1);
		assertEquals(1, m1.size());
		assertEquals(1, m1.get("a"));

		// 2 pairs
		LinkedHashMap<String, Integer> m2 = map("a", 1, "b", 2);
		assertEquals(2, m2.size());

		// 3 pairs
		LinkedHashMap<String, Integer> m3 = map("a", 1, "b", 2, "c", 3);
		assertEquals(3, m3.size());

		// 4 pairs
		LinkedHashMap<String, Integer> m4 = map("a", 1, "b", 2, "c", 3, "d", 4);
		assertEquals(4, m4.size());
		var keys = new ArrayList<>(m4.keySet());
		assertEquals(l("a", "b", "c", "d"), keys);

		// 5 pairs
		LinkedHashMap<String, Integer> m5 = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
		assertEquals(5, m5.size());

		// 6 pairs
		LinkedHashMap<String, Integer> m6 = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
		assertEquals(6, m6.size());

		// 7 pairs - test lines 1400-1408
		LinkedHashMap<String, Integer> m7 = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
		assertEquals(7, m7.size());
		assertEquals(7, m7.get("g"));

		// 8 pairs - test lines 1435-1444
		LinkedHashMap<String, Integer> m8 = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
		assertEquals(8, m8.size());
		assertEquals(8, m8.get("h"));

		// 9 pairs - test lines 1473-1483
		LinkedHashMap<String, Integer> m9 = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
		assertEquals(9, m9.size());
		assertEquals(9, m9.get("i"));

		// 10 pairs
		LinkedHashMap<String, Integer> m10 = map("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);
		assertEquals(10, m10.size());
		assertEquals(l("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), new ArrayList<>(m10.keySet()));

		// Null values
		LinkedHashMap<String, String> nullVals = map("a", "val1", "b", null, "c", "val3", "d", null);
		assertEquals(4, nullVals.size());
		assertNull(nullVals.get("b"));
	}

	//====================================================================================================
	// mapb()
	//====================================================================================================
	@Test
	void a053_mapb() {
		MapBuilder<String, Object> builder = mapb();
		assertNotNull(builder);
		Map<String, Object> result = builder.add("a", 1).add("b", 2).build();
		assertEquals(2, result.size());
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
	}

	//====================================================================================================
	// mapb(Class<K>, Class<V>, Converter...)
	//====================================================================================================
	@Test
	void a054_mapb_class() {
		MapBuilder<String, Integer> builder = mapb(String.class, Integer.class);
		assertNotNull(builder);
		Map<String, Integer> result = builder.add("a", 1).build();
		assertEquals(1, result.size());
	}

	//====================================================================================================
	// mape(Class<K>, Class<V>)
	//====================================================================================================
	@Test
	void a055_mape() {
		Map<String, Integer> result = mape(String.class, Integer.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> result.put("a", 1));
	}

	//====================================================================================================
	// mapn(Class<K>, Class<V>)
	//====================================================================================================
	@Test
	void a056_mapn() {
		assertNull(mapn(String.class, Integer.class));
	}

	//====================================================================================================
	// mapOf(Class<K>, Class<V>)
	//====================================================================================================
	@Test
	void a057_mapOf() {
		LinkedHashMap<String, Integer> result = mapOf(String.class, Integer.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
		result.put("a", 1); // Modifiable
		assertEquals(1, result.size());
	}

	//====================================================================================================
	// na(Class<T>)
	//====================================================================================================
	@Test
	void a058_na() {
		assertNull(na(String.class));
	}

	//====================================================================================================
	// prependAll(List<E>, E...)
	//====================================================================================================
	@Test
	void a059_prependAll() {
		// Null value creates new list
		var result1 = prependAll((List<String>)null, "a", "b");
		assertNotNull(result1);
		assertEquals(list("a", "b"), result1);

		// Existing list prepends to it
		var list = list("x");
		var result2 = prependAll(list, "a", "b");
		assertSame(list, result2);
		assertEquals(list("a", "b", "x"), result2);

		// Null entries returns original
		var list2 = list("a", "b");
		var result3 = prependAll(list2, (String[])null);
		assertSame(list2, result3);
	}

	//====================================================================================================
	// reverse(E[])
	//====================================================================================================
	@Test
	void a060_reverse_array() {
		String[] arr = {"a", "b", "c"};
		String[] result = reverse(arr);
		assertSame(arr, result);
		assertEquals("c", arr[0]);
		assertEquals("b", arr[1]);
		assertEquals("a", arr[2]);
	}

	//====================================================================================================
	// reverse(List<E>)
	//====================================================================================================
	@Test
	void a061_reverse_list() {
		List<String> list = list("a", "b", "c");
		List<String> result = reverse(list);
		assertNotNull(result);
		assertEquals("c", result.get(0));
		assertEquals("b", result.get(1));
		assertEquals("a", result.get(2));
		// View reflects changes - adding to original list adds to end, which appears at index 0 in reversed view
		list.add("d");
		assertEquals(4, result.size());
		assertEquals("d", result.get(0)); // New element appears at start of reversed view
		assertEquals("a", result.get(3)); // Original first element now at end
	}

	//====================================================================================================
	// rstream(List<T>)
	//====================================================================================================
	@Test
	void a062_rstream() {
		List<String> list = list("a", "b", "c");
		List<String> result = rstream(list).toList();
		assertEquals(list("c", "b", "a"), result);

		assertTrue(rstream(null).toList().isEmpty());
		assertTrue(rstream(list()).toList().isEmpty());
	}

	//====================================================================================================
	// set(T...)
	//====================================================================================================
	@Test
	void a063_set() {
		LinkedHashSet<String> result = set("a", "b", "c");
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertTrue(result.contains("c"));
		result.add("d"); // Modifiable
		assertEquals(4, result.size());
	}

	//====================================================================================================
	// setb(Class<E>, Converter...)
	//====================================================================================================
	@Test
	void a064_setb() {
		SetBuilder<String> builder = setb(String.class);
		assertNotNull(builder);
		Set<String> result = builder.add("a").add("b").build();
		assertEquals(2, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
	}

	//====================================================================================================
	// setOf(Class<E>, E...)
	//====================================================================================================
	@Test
	void a065_setOf() {
		LinkedHashSet<String> result = setOf(String.class, "a", "b", "c");
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
	}

	//====================================================================================================
	// shorts(int...)
	//====================================================================================================
	@Test
	void a066_shorts() {
		short[] result = shorts(1, 2, 3);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	//====================================================================================================
	// sortedList(Comparator<E>, Collection<E>)
	//====================================================================================================
	@Test
	void a067_sortedList_comparatorCollection() {
		List<String> list = list("c", "a", "b");
		List<String> result = sortedList(Comparator.naturalOrder(), list);
		assertNotNull(result);
		assertEquals(list("a", "b", "c"), result);
	}

	//====================================================================================================
	// sortedList(Comparator<E>, E[])
	//====================================================================================================
	@Test
	void a068_sortedList_comparatorArray() {
		String[] arr = {"c", "a", "b"};
		List<String> result = sortedList(Comparator.naturalOrder(), arr);
		assertNotNull(result);
		assertEquals(list("a", "b", "c"), result);
	}

	//====================================================================================================
	// sortedList(E...)
	//====================================================================================================
	@Test
	void a069_sortedList() {
		List<String> result = sortedList("c", "a", "b");
		assertNotNull(result);
		assertEquals(list("a", "b", "c"), result);
	}

	//====================================================================================================
	// sortedMap()
	//====================================================================================================
	@Test
	void a070_sortedMap() {
		TreeMap<String, Integer> result = sortedMap();
		assertNotNull(result);
		assertTrue(result.isEmpty());
		result.put("a", 1);
		assertEquals(1, result.size());
	}

	//====================================================================================================
	// sortedSet(E...)
	//====================================================================================================
	@Test
	void a071_sortedSet() {
		TreeSet<String> result = sortedSet("c", "a", "b");
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals(list("a", "b", "c"), new ArrayList<>(result));

		// Empty
		TreeSet<String> empty = sortedSet();
		assertNotNull(empty);
		assertTrue(empty.isEmpty());

		// Single
		TreeSet<String> single = sortedSet("a");
		assertEquals(1, single.size());

		// Numbers
		TreeSet<Integer> numbers = sortedSet(3, 1, 2, 5, 4);
		assertEquals(list(1, 2, 3, 4, 5), new ArrayList<>(numbers));
	}

	//====================================================================================================
	// stream(T[])
	//====================================================================================================
	@Test
	void a072_stream() {
		String[] arr = {"a", "b", "c"};
		List<String> result = stream(arr).toList();
		assertEquals(list("a", "b", "c"), result);

		assertTrue(stream(null).toList().isEmpty());
	}

	//====================================================================================================
	// synced(List<E>)
	//====================================================================================================
	@Test
	void a073_synced_list() {
		List<String> list = list("a", "b");
		List<String> result = synced(list);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertNull(synced((List<String>)null));
	}

	//====================================================================================================
	// synced(Map<K,V>)
	//====================================================================================================
	@Test
	void a074_synced_map() {
		Map<String, Integer> map = map("a", 1);
		Map<String, Integer> result = synced(map);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(1, result.get("a"));
		assertNull(synced((Map<String, Integer>)null));
	}

	//====================================================================================================
	// synced(Set<E>)
	//====================================================================================================
	@Test
	void a075_synced_set() {
		Set<String> set = set("a", "b");
		Set<String> result = synced(set);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertNull(synced((Set<String>)null));
	}

	//====================================================================================================
	// toArray(Collection<?>, Class<E>)
	//====================================================================================================
	@Test
	void a076_toArray() {
		List<String> list = list("a", "b", "c");
		Object result = toArray(list, String.class);
		assertNotNull(result);
		assertTrue(result instanceof String[]);
		String[] arr = (String[])result;
		assertEquals(3, arr.length);
		assertEquals("a", arr[0]);
	}

	//====================================================================================================
	// toList(Collection<E>)
	//====================================================================================================
	@Test
	void a077_toList_collection() {
		Collection<String> col = set("a", "b", "c");
		ArrayList<String> result = toList(col);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
	}

	//====================================================================================================
	// toList(Collection<E>, boolean)
	//====================================================================================================
	@Test
	void a078_toList_collectionBoolean() {
		Collection<String> col = list("a", "b");
		ArrayList<String> result1 = toList(col, false);
		assertNotNull(result1);
		assertEquals(2, result1.size());

		ArrayList<String> result2 = toList(col, true);
		assertNotNull(result2);
		assertEquals(2, result2.size());

		Collection<String> empty = list();
		assertNull(toList(empty, true));
		assertNotNull(toList(empty, false));
		assertNull(toList(null, false));
	}

	//====================================================================================================
	// toList(Object)
	//====================================================================================================
	@Test
	void a079_toList_object() {
		// List returns as-is
		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add("a");
		arrayList.add("b");
		List<?> result1 = toList((Object)arrayList);
		assertSame(arrayList, result1);

		// Set converts to list
		Set<String> set = set("a", "b", "c");
		List<?> result2 = toList(set);
		assertNotNull(result2);
		assertEquals(3, result2.size());
		assertTrue(result2.contains("a"));
		assertTrue(result2.contains("b"));
		assertTrue(result2.contains("c"));

		// Array converts to list
		String[] arr = {"a", "b", "c"};
		List<?> result3 = toList(arr);
		assertEquals(3, result3.size());
		assertEquals("a", result3.get(0));

		// Test lines 2001-2006: Stream, Map, Optional
		// Stream
		List<?> result4 = toList(java.util.stream.Stream.of("x", "y", "z"));
		assertEquals(3, result4.size());
		assertEquals("x", result4.get(0));

		// Map
		Map<String, Integer> map = map("a", 1, "b", 2);
		List<?> result5 = toList(map);
		assertEquals(2, result5.size());

		// Optional - empty
		List<?> result6 = toList(Optional.empty());
		assertTrue(result6.isEmpty());

		// Optional - present
		List<?> result7 = toList(Optional.of("test"));
		assertEquals(1, result7.size());
		assertEquals("test", result7.get(0));

		// Test line 2009: unsupported type throws exception
		assertThrows(RuntimeException.class, () -> {
			toList(new Object() {}); // Unsupported type
		});
	}

	//====================================================================================================
	// toSet(E[])
	//====================================================================================================
	@Test
	void a080_toSet() {
		String[] arr = {"a", "b", "c"};
		Set<String> result = toSet(arr);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertThrows(UnsupportedOperationException.class, () -> result.add("d"));

		assertThrows(IllegalArgumentException.class, () -> toSet((String[])null));

		// Test lines 2085, 2093: Iterator behavior
		Iterator<String> it = result.iterator();
		assertTrue(it.hasNext());
		assertEquals("a", it.next());
		assertEquals("b", it.next());
		assertEquals("c", it.next());
		assertFalse(it.hasNext());

		// Test line 2085: NoSuchElementException when calling next() after exhausted
		assertThrows(java.util.NoSuchElementException.class, () -> {
			it.next();
		});

		// Test line 2093: UnsupportedOperationException when calling remove()
		Iterator<String> it2 = result.iterator();
		it2.next(); // Move to first element
		assertThrows(UnsupportedOperationException.class, () -> {
			it2.remove();
		});
	}

	//====================================================================================================
	// toSortedSet(Set<E>)
	//====================================================================================================
	@Test
	void a081_toSortedSet() {
		LinkedHashSet<String> input = new LinkedHashSet<>(l("c", "a", "b"));
		TreeSet<String> result = toSortedSet(input);
		assertNotNull(result);
		assertEquals(l("a", "b", "c"), new ArrayList<>(result));

		LinkedHashSet<Integer> input2 = new LinkedHashSet<>(l(3, 1, 2));
		TreeSet<Integer> result2 = toSortedSet(input2);
		assertEquals(l(1, 2, 3), new ArrayList<>(result2));

		assertNull(toSortedSet((Set<String>)null));
	}

	//====================================================================================================
	// traverse(Object, Consumer<T>)
	//====================================================================================================
	@Test
	void a082_traverse() {
		List<String> list = list("a", "b", "c");
		List<Object> result = new ArrayList<>();
		traverse(list, result::add);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertTrue(result.contains("b"));
		assertTrue(result.contains("c"));

		// Test line 2189: null object returns early
		List<Object> result2 = new ArrayList<>();
		traverse(null, result2::add);
		assertTrue(result2.isEmpty());

		// Test line 2194: Stream handling
		List<Object> result3 = new ArrayList<>();
		traverse(java.util.stream.Stream.of("x", "y", "z"), result3::add);
		assertEquals(3, result3.size());
		assertTrue(result3.contains("x"));
		assertTrue(result3.contains("y"));
		assertTrue(result3.contains("z"));
	}

	//====================================================================================================
	// toList(Object, Class<E>)
	//====================================================================================================
	@Test
	void a083_toList_objectClass() {
		String[] arr = {"a", "b", "c"};
		List<String> result = toList(arr, String.class);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
	}

	//====================================================================================================
	// toObjectList(Object)
	//====================================================================================================
	@Test
	void a084_toObjectList() {
		String[] arr = {"a", "b", "c"};

		// Test line 2041: nested arrays (recursive call)
		String[][] nestedArr = {{"a", "b"}, {"c", "d"}};
		List<Object> nestedResult = toObjectList(nestedArr);
		assertEquals(2, nestedResult.size());
		assertTrue(nestedResult.get(0) instanceof List);
		assertTrue(nestedResult.get(1) instanceof List);

		String[] arr2 = {"a", "b", "c"};
		List<Object> result = toObjectList(arr);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
	}

	//====================================================================================================
	// toSet(Collection<E>)
	//====================================================================================================
	@Test
	void a085_toSet_collection() {
		Collection<String> col = list("a", "b", "c");
		Set<String> result = toSet(col);
		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.contains("a"));
		assertNull(toSet((Collection<String>)null));
	}

	//====================================================================================================
	// toSortedSet(Collection<E>)
	//====================================================================================================
	@Test
	void a085b_toSortedSet_collection() {
		// Test lines 2117-2121: toSortedSet(Collection<E>) - different from toSortedSet(Set<E>)
		Collection<String> col = list("c", "a", "b");
		TreeSet<String> result = toSortedSet(col);
		assertNotNull(result);
		assertEquals(l("a", "b", "c"), new ArrayList<>(result));

		Collection<Integer> col2 = list(3, 1, 2);
		TreeSet<Integer> result2 = toSortedSet(col2);
		assertEquals(l(1, 2, 3), new ArrayList<>(result2));

		// Test line 2117: null returns null
		assertNull(toSortedSet((Collection<String>)null));
	}

	//====================================================================================================
	// toSortedSet(Collection<E>, boolean)
	//====================================================================================================
	@Test
	void a086_toSortedSet_collectionBoolean() {
		Collection<String> col = list("c", "a", "b");
		TreeSet<String> result1 = toSortedSet(col, false);
		assertNotNull(result1);
		assertEquals(3, result1.size());

		Collection<String> empty = list();
		assertNull(toSortedSet(empty, true));
		assertNotNull(toSortedSet(empty, false));
		assertNull(toSortedSet((Collection<String>)null, false));
	}

	//====================================================================================================
	// toStream(Object)
	//====================================================================================================
	@Test
	void a087_toStream() {
		String[] arr = {"a", "b", "c"};
		List<Object> result = toStream(arr).toList();
		assertEquals(3, result.size());
		assertEquals("a", result.get(0));
	}

	//====================================================================================================
	// toStringArray(Collection<?>)
	//====================================================================================================
	@Test
	void a088_toStringArray() {
		Collection<Integer> col = list(1, 2, 3);
		String[] result = toStringArray(col);
		assertNotNull(result);
		assertEquals(3, result.length);
		assertEquals("1", result[0]);
		assertEquals("2", result[1]);
		assertEquals("3", result[2]);
	}

	//====================================================================================================
	// u(List<? extends T>)
	//====================================================================================================
	@Test
	void a089_u_list() {
		List<String> list = list("a", "b");
		List<String> result = u(list);
		assertNotNull(result);
		assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
		assertNull(u((List<String>)null));
	}

	//====================================================================================================
	// u(Map<? extends K,? extends V>)
	//====================================================================================================
	@Test
	void a090_u_map() {
		Map<String, Integer> map = map("a", 1);
		Map<String, Integer> result = u(map);
		assertNotNull(result);
		assertThrows(UnsupportedOperationException.class, () -> result.put("b", 2));
		assertNull(u((Map<String, Integer>)null));
	}

	//====================================================================================================
	// u(Set<? extends T>)
	//====================================================================================================
	@Test
	void a091_u_set() {
		Set<String> set = set("a", "b");
		Set<String> result = u(set);
		assertNotNull(result);
		assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
		assertNull(u((Set<String>)null));
	}
}
