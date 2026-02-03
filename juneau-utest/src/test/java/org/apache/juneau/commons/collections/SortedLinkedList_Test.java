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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SortedLinkedList_Test extends TestBase {

	//====================================================================================================
	// Basic operations - natural ordering
	//====================================================================================================

	@Test
	void a01_naturalOrdering_basicAdd() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");

		assertList(list, "a", "b", "c");
	}

	@Test
	void a02_naturalOrdering_integers() {
		var list = new SortedLinkedList<Integer>();
		list.add(5);
		list.add(2);
		list.add(8);
		list.add(1);
		list.add(9);

		assertList(list, 1, 2, 5, 8, 9);
	}

	@Test
	void a03_naturalOrdering_duplicates() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");
		list.add("a");  // Duplicate
		list.add("c");  // Duplicate

		assertList(list, "a", "a", "b", "c", "c");
	}

	//====================================================================================================
	// Custom comparator
	//====================================================================================================

	@Test
	void b01_customComparator_stringLength() {
		var list = new SortedLinkedList<>(Comparator.comparing(String::length));
		list.add("ccc");
		list.add("a");
		list.add("bb");

		assertList(list, "a", "bb", "ccc");
	}

	@Test
	void b02_customComparator_reverseOrder() {
		List<Integer> list = new SortedLinkedList<>(Comparator.reverseOrder());
		list.add(1);
		list.add(5);
		list.add(3);

		assertList(list, 5, 3, 1);
	}

	@Test
	void b03_customComparator_duplicates() {
		var list = new SortedLinkedList<>(Comparator.comparing(String::length));
		list.add("ccc");
		list.add("a");
		list.add("bb");
		list.add("dd");  // Same length as "bb"
		list.add("a");   // Duplicate

		assertList(list, "a", "a", "bb", "dd", "ccc");
	}

	//====================================================================================================
	// Constructors
	//====================================================================================================

	@Test
	void c01_constructor_default() {
		var list = new SortedLinkedList<String>();
		assertTrue(list.isEmpty());
		assertNull(list.comparator());
	}

	@Test
	void c02_constructor_withComparator() {
		var comparator = Comparator.comparing(String::length);
		var list = new SortedLinkedList<>(comparator);
		assertTrue(list.isEmpty());
		assertSame(comparator, list.comparator());
	}

	@Test
	void c03_constructor_withCollection() {
		var source = List.of("c", "a", "b");
		var list = new SortedLinkedList<>(source);

		assertList(list, "a", "b", "c");
	}

	@Test
	void c04_constructor_withCollectionAndComparator() {
		var source = List.of("ccc", "a", "bb");
		var list = new SortedLinkedList<>(Comparator.comparing(String::length), source);

		assertList(list, "a", "bb", "ccc");
	}

	//====================================================================================================
	// add(int, E) - index is ignored
	//====================================================================================================

	@Test
	void d01_addAtIndex_ignoresIndex() {
		var list = new SortedLinkedList<String>();
		list.add(0, "c");
		list.add(0, "a");  // Index ignored, inserted in sorted order
		list.add(1, "b");  // Index ignored, inserted in sorted order

		assertList(list, "a", "b", "c");
	}

	//====================================================================================================
	// addAll
	//====================================================================================================

	@Test
	void e01_addAll_maintainsSortOrder() {
		var list = new SortedLinkedList<String>();
		list.add("d");
		list.addAll(List.of("c", "a", "b"));

		assertList(list, "a", "b", "c", "d");
	}

	@Test
	void e02_addAll_emptyCollection() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		boolean modified = list.addAll(Collections.emptyList());

		assertFalse(modified);
		assertList(list, "a");
	}

	@Test
	void e03_addAll_atIndex_ignoresIndex() {
		var list = new SortedLinkedList<String>();
		list.add("d");
		list.addAll(0, List.of("c", "a", "b"));  // Index ignored

		assertList(list, "a", "b", "c", "d");
	}

	//====================================================================================================
	// set(int, E) - may move element
	//====================================================================================================

	@Test
	void f01_set_mayMoveElement() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("c");

		String oldValue = list.set(1, "z");  // Replace "b" with "z"
		assertEquals("b", oldValue);
		assertList(list, "a", "c", "z");  // "z" moved to end
	}

	@Test
	void f02_set_sameOrder() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("c");

		String oldValue = list.set(1, "b2");  // Replace "b" with "b2"
		assertEquals("b", oldValue);
		assertList(list, "a", "b2", "c");  // "b2" stays in same position
	}

	//====================================================================================================
	// List interface methods
	//====================================================================================================

	@Test
	void g01_get() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");

		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));
	}

	@Test
	void g02_get_indexOutOfBounds() {
		var list = new SortedLinkedList<String>();
		list.add("a");

		assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
	}

	@Test
	void g03_remove() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("c");

		String removed = list.remove(1);
		assertEquals("b", removed);
		assertList(list, "a", "c");
	}

	@Test
	void g04_size() {
		var list = new SortedLinkedList<String>();
		assertEquals(0, list.size());

		list.add("a");
		assertEquals(1, list.size());

		list.add("b");
		assertEquals(2, list.size());
	}

	@Test
	void g05_isEmpty() {
		var list = new SortedLinkedList<String>();
		assertTrue(list.isEmpty());

		list.add("a");
		assertFalse(list.isEmpty());
	}

	@Test
	void g06_contains() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("c");

		assertTrue(list.contains("b"));
		assertFalse(list.contains("z"));
	}

	@Test
	void g07_indexOf() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("a");  // Duplicate

		// After adding "a", "b", "a" to a sorted list, we get ["a", "a", "b"]
		assertEquals(0, list.indexOf("a"));
		assertEquals(1, list.lastIndexOf("a"));  // Second "a" is at index 1
		assertEquals(2, list.indexOf("b"));  // "b" is at index 2
	}

	@Test
	void g08_clear() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");

		list.clear();
		assertTrue(list.isEmpty());
	}

	//====================================================================================================
	// Iterator
	//====================================================================================================

	@Test
	void h01_iterator() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");

		var iterator = list.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("a", iterator.next());
		assertEquals("b", iterator.next());
		assertEquals("c", iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	void h02_listIterator() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");

		var iterator = list.listIterator();
		assertTrue(iterator.hasNext());
		assertEquals("a", iterator.next());
		assertEquals("b", iterator.next());
		assertTrue(iterator.hasPrevious());
		assertEquals("b", iterator.previous());
	}

	//====================================================================================================
	// SubList
	//====================================================================================================

	@Test
	void i01_subList() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		list.add("d");
		list.add("e");

		var subList = list.subList(1, 4);
		assertList(subList, "b", "c", "d");
	}

	//====================================================================================================
	// toArray
	//====================================================================================================

	@Test
	void j01_toArray() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");

		Object[] array = list.toArray();
		assertArrayEquals(new Object[]{"a", "b", "c"}, array);
	}

	@Test
	void j02_toArray_withType() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");

		String[] array = list.toArray(new String[0]);
		assertArrayEquals(new String[]{"a", "b", "c"}, array);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void k01_singleElement() {
		var list = new SortedLinkedList<String>();
		list.add("a");

		assertSize(1, list);
		assertEquals("a", list.get(0));
	}

	@Test
	void k02_emptyList() {
		var list = new SortedLinkedList<String>();

		assertTrue(list.isEmpty());
		assertSize(0, list);
	}

	@Test
	void k03_allEqualElements() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("a");
		list.add("a");

		assertList(list, "a", "a", "a");
	}

	@Test
	void k04_alreadySortedInput() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		list.add("b");
		list.add("c");

		assertList(list, "a", "b", "c");
	}

	@Test
	void k05_reverseSortedInput() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("b");
		list.add("a");

		assertList(list, "a", "b", "c");
	}

	//====================================================================================================
	// Not RandomAccess
	//====================================================================================================

	@Test
	void l01_doesNotImplementRandomAccess() {
		var list = new SortedLinkedList<String>();
		assertFalse(list instanceof RandomAccess);
	}

	//====================================================================================================
	// toString()
	//====================================================================================================

	@Test
	void m01_toString_empty() {
		var list = new SortedLinkedList<String>();
		assertEquals("[]", list.toString());
	}

	@Test
	void m02_toString_singleElement() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		assertEquals("[a]", list.toString());
	}

	@Test
	void m03_toString_multipleElements() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");
		assertEquals("[a, b, c]", list.toString());
	}

	@Test
	void m04_toString_withDuplicates() {
		var list = new SortedLinkedList<String>();
		list.add("c");
		list.add("a");
		list.add("b");
		list.add("a");
		assertEquals("[a, a, b, c]", list.toString());
	}

	@Test
	void m05_toString_withNull() {
		List<String> list = new SortedLinkedList<>(Comparator.nullsFirst(Comparator.naturalOrder()));
		list.add("b");
		list.add(null);
		list.add("a");
		assertEquals("[null, a, b]", list.toString());
	}

	//====================================================================================================
	// hashCode()
	//====================================================================================================

	@Test
	void n01_hashCode_empty() {
		List<String> list1 = new SortedLinkedList<>();
		List<String> list2 = new SortedLinkedList<>();
		assertEquals(list1.hashCode(), list2.hashCode());
	}

	@Test
	void n02_hashCode_sameElements() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("b");
		list1.add("c");

		var list2 = new SortedLinkedList<String>();
		list2.add("c");
		list2.add("a");
		list2.add("b");

		// Same elements in same order should have same hash code
		assertEquals(list1.hashCode(), list2.hashCode());
	}

	@Test
	void n03_hashCode_differentElements() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("b");

		var list2 = new SortedLinkedList<String>();
		list2.add("a");
		list2.add("c");

		// Different elements should have different hash codes (usually)
		assertNotEquals(list1.hashCode(), list2.hashCode());
	}

	@Test
	void n04_hashCode_equalsContract() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("b");
		list1.add("c");

		var list2 = new SortedLinkedList<String>();
		list2.add("c");
		list2.add("a");
		list2.add("b");

		// If two lists are equal, they must have the same hash code
		assertEquals(list1, list2);
		assertEquals(list1.hashCode(), list2.hashCode());
	}

	@Test
	void n05_hashCode_withDuplicates() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("a");
		list1.add("b");

		var list2 = new SortedLinkedList<String>();
		list2.add("a");
		list2.add("a");
		list2.add("b");

		assertEquals(list1.hashCode(), list2.hashCode());
	}

	//====================================================================================================
	// equals()
	//====================================================================================================

	@Test
	void o01_equals_sameInstance() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		assertEquals(list, list);
	}

	@Test
	void o02_equals_emptyLists() {
		var list1 = new SortedLinkedList<String>();
		var list2 = new SortedLinkedList<String>();
		assertEquals(list1, list2);
	}

	@Test
	void o03_equals_sameElements() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("b");
		list1.add("c");

		var list2 = new SortedLinkedList<String>();
		list2.add("c");
		list2.add("a");
		list2.add("b");

		// Same elements in same order should be equal
		assertEquals(list1, list2);
	}

	@Test
	void o04_equals_differentElements() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("b");

		var list2 = new SortedLinkedList<String>();
		list2.add("a");
		list2.add("c");

		assertNotEquals(list1, list2);
	}

	@Test
	void o05_equals_differentSizes() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("b");

		var list2 = new SortedLinkedList<String>();
		list2.add("a");

		assertNotEquals(list1, list2);
	}

	@Test
	void o06_equals_withDuplicates() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("a");
		list1.add("b");

		var list2 = new SortedLinkedList<String>();
		list2.add("a");
		list2.add("a");
		list2.add("b");

		assertEquals(list1, list2);
	}

	@Test
	void o07_equals_differentDuplicateCounts() {
		var list1 = new SortedLinkedList<String>();
		list1.add("a");
		list1.add("a");
		list1.add("b");

		var list2 = new SortedLinkedList<String>();
		list2.add("a");
		list2.add("b");

		assertNotEquals(list1, list2);
	}

	@Test
	void o08_equals_withArrayList() {
		var sortedList = new SortedLinkedList<String>();
		sortedList.add("a");
		sortedList.add("b");
		sortedList.add("c");

		var arrayList = new ArrayList<String>();
		arrayList.add("a");
		arrayList.add("b");
		arrayList.add("c");

		// Should be equal if they contain the same elements in the same order
		assertEquals(sortedList, arrayList);
		assertEquals(arrayList, sortedList);
	}

	@Test
	void o09_equals_withLinkedList() {
		var sortedList = new SortedLinkedList<String>();
		sortedList.add("a");
		sortedList.add("b");
		sortedList.add("c");

		var linkedList = new LinkedList<String>();
		linkedList.add("a");
		linkedList.add("b");
		linkedList.add("c");

		// Should be equal if they contain the same elements in the same order
		assertEquals(sortedList, linkedList);
		assertEquals(linkedList, sortedList);
	}

	@Test
	void o10_equals_notAList() {
		var list = new SortedLinkedList<String>();
		list.add("a");
		assertNotEquals("a", list);
		assertNotEquals(null, list);
	}

	@Test
	void o11_equals_differentComparators() {
		// Two lists with different comparators will have different element orders
		List<String> list1 = new SortedLinkedList<>(Comparator.naturalOrder());
		list1.add("a");
		list1.add("b");
		list1.add("c");
		// list1 is ["a", "b", "c"]

		List<String> list2 = new SortedLinkedList<>(Comparator.reverseOrder());
		list2.add("c");
		list2.add("b");
		list2.add("a");
		// list2 is ["c", "b", "a"] (reverse sorted)

		// They have the same elements but in different orders, so they should NOT be equal
		assertNotEquals(list1, list2);
	}

	@Test
	void o12_equals_crossType() {
		// SortedArrayList and SortedLinkedList with same elements should be equal
		var arrayList = new SortedArrayList<String>();
		arrayList.add("a");
		arrayList.add("b");
		arrayList.add("c");

		var linkedList = new SortedLinkedList<String>();
		linkedList.add("c");
		linkedList.add("a");
		linkedList.add("b");

		// Should be equal if they contain the same elements in the same order
		assertEquals(arrayList, linkedList);
		assertEquals(linkedList, arrayList);
	}
}

