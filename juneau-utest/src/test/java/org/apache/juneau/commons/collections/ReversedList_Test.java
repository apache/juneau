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

class ReversedList_Test extends TestBase {

	//====================================================================================================
	// Basic functionality
	//====================================================================================================

	@Test
	void a01_basicGet() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertEquals("C", reversed.get(0));
		assertEquals("B", reversed.get(1));
		assertEquals("A", reversed.get(2));
		assertSize(3, reversed);
	}

	@Test
	void a02_basicIteration() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var result = new ArrayList<String>();
		for (String s : reversed) {
			result.add(s);
		}

		assertEquals(List.of("C", "B", "A"), result);
	}

	@Test
	void a03_emptyList() {
		var original = List.<String>of();
		var reversed = new ReversedList<>(original);

		assertEmpty(reversed);
		assertFalse(reversed.iterator().hasNext());
	}

	@Test
	void a04_singleElement() {
		var original = List.of("A");
		var reversed = new ReversedList<>(original);

		assertSize(1, reversed);
		assertEquals("A", reversed.get(0));
	}

	//====================================================================================================
	// Null handling
	//====================================================================================================

	@Test
	void b01_nullList_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> new ReversedList<>(null));
	}

	@Test
	void b02_listWithNulls() {
		var original = Arrays.asList("A", null, "C");
		var reversed = new ReversedList<>(original);

		assertEquals("C", reversed.get(0));
		assertNull(reversed.get(1));
		assertEquals("A", reversed.get(2));
	}

	//====================================================================================================
	// Index bounds
	//====================================================================================================

	@Test
	void c01_outOfBounds_negative() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(IndexOutOfBoundsException.class, () -> reversed.get(-1));
	}

	@Test
	void c02_outOfBounds_tooLarge() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(IndexOutOfBoundsException.class, () -> reversed.get(3));
	}

	//====================================================================================================
	// Reflection of underlying list changes
	//====================================================================================================

	@Test
	void d01_reflectsUnderlyingChanges() {
		var original = new ArrayList<>(Arrays.asList("A", "B", "C"));
		var reversed = new ReversedList<>(original);

		assertEquals("C", reversed.get(0));

		original.add("D");

		assertSize(4, reversed);
		assertEquals("D", reversed.get(0));
		assertEquals("C", reversed.get(1));
	}

	@Test
	void d02_reflectsUnderlyingRemoval() {
		var original = new ArrayList<>(Arrays.asList("A", "B", "C"));
		var reversed = new ReversedList<>(original);

		original.remove(2); // Remove "C"

		assertSize(2, reversed);
		assertEquals("B", reversed.get(0));
		assertEquals("A", reversed.get(1));
	}

	//====================================================================================================
	// Read-only enforcement
	//====================================================================================================

	@Test
	void e01_add_throwsException() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(UnsupportedOperationException.class, () -> reversed.add("D"));
	}

	@Test
	void e02_addAtIndex_throwsException() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(UnsupportedOperationException.class, () -> reversed.add(0, "D"));
	}

	@Test
	void e03_remove_throwsException() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(UnsupportedOperationException.class, () -> reversed.remove(0));
	}

	@Test
	void e04_set_throwsException() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(UnsupportedOperationException.class, () -> reversed.set(0, "D"));
	}

	@Test
	void e05_clear_throwsException() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(UnsupportedOperationException.class, () -> reversed.clear());
	}

	@Test
	void e06_iteratorRemove_throwsException() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var it = reversed.iterator();
		it.next();
		assertThrows(UnsupportedOperationException.class, () -> it.remove());
	}

	//====================================================================================================
	// Iterator functionality
	//====================================================================================================

	@Test
	void f01_iterator_traversal() {
		var original = List.of("A", "B", "C", "D");
		var reversed = new ReversedList<>(original);

		var it = reversed.iterator();
		assertTrue(it.hasNext());
		assertEquals("D", it.next());
		assertTrue(it.hasNext());
		assertEquals("C", it.next());
		assertTrue(it.hasNext());
		assertEquals("B", it.next());
		assertTrue(it.hasNext());
		assertEquals("A", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void f02_listIterator_forward() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator();
		assertEquals("C", it.next());
		assertEquals("B", it.next());
		assertEquals("A", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void f03_listIterator_backward() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(3);
		assertEquals("A", it.previous());
		assertEquals("B", it.previous());
		assertEquals("C", it.previous());
		assertFalse(it.hasPrevious());
	}

	@Test
	void f04_listIterator_bidirectional() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(1);
		assertEquals("B", it.next());
		assertEquals("B", it.previous());
		assertEquals("C", it.previous());
		assertEquals("C", it.next());
	}

	@Test
	void f05_listIterator_indices() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator();
		assertEquals(-1, it.previousIndex());
		assertEquals(0, it.nextIndex());

		it.next();
		assertEquals(0, it.previousIndex());
		assertEquals(1, it.nextIndex());

		it.next();
		assertEquals(1, it.previousIndex());
		assertEquals(2, it.nextIndex());

		it.next();
		assertEquals(2, it.previousIndex());
		assertEquals(3, it.nextIndex());
	}

	@Test
	void f06_listIterator_modificationThrows() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator();
		it.next();

		assertThrows(UnsupportedOperationException.class, () -> it.remove());
		assertThrows(UnsupportedOperationException.class, () -> it.set("X"));
		assertThrows(UnsupportedOperationException.class, () -> it.add("X"));
	}

	//====================================================================================================
	// SubList functionality
	//====================================================================================================

	@Test
	void g01_subList_basic() {
		var original = List.of("A", "B", "C", "D", "E");
		var reversed = new ReversedList<>(original);

		var subList = reversed.subList(1, 4);

		assertSize(3, subList);
		assertEquals("D", subList.get(0));
		assertEquals("C", subList.get(1));
		assertEquals("B", subList.get(2));
	}

	@Test
	void g02_subList_empty() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var subList = reversed.subList(1, 1);

		assertEmpty(subList);
	}

	@Test
	void g03_subList_full() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var subList = reversed.subList(0, 3);

		assertSize(3, subList);
		assertEquals("C", subList.get(0));
		assertEquals("B", subList.get(1));
		assertEquals("A", subList.get(2));
	}

	@Test
	void g04_subList_outOfBounds() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(-1, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(0, 4));
		assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(2, 1));
	}

	@Test
	void g05_subList_reflectsChanges() {
		var original = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
		var reversed = new ReversedList<>(original);
		var subList = reversed.subList(1, 4);

		original.set(3, "X"); // Changes "D" to "X" in original

		assertEquals("X", subList.get(0));
	}

	//====================================================================================================
	// Contains and indexOf
	//====================================================================================================

	@Test
	void h01_contains() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertTrue(reversed.contains("A"));
		assertTrue(reversed.contains("B"));
		assertTrue(reversed.contains("C"));
		assertFalse(reversed.contains("D"));
	}

	@Test
	void h02_indexOf() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertEquals(2, reversed.indexOf("A"));
		assertEquals(1, reversed.indexOf("B"));
		assertEquals(0, reversed.indexOf("C"));
		assertEquals(-1, reversed.indexOf("D"));
	}

	@Test
	void h03_lastIndexOf() {
		var original = List.of("A", "B", "A", "C");
		var reversed = new ReversedList<>(original);

		// Original: ["A", "B", "A", "C"]
		// Reversed: ["C", "A", "B", "A"]
		// First "A" in reversed is at index 1, last "A" is at index 3
		assertEquals(3, reversed.lastIndexOf("A"));
		assertEquals(0, reversed.lastIndexOf("C"));
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void i01_largeList() {
		var original = new ArrayList<Integer>();
		for (var i = 0; i < 1000; i++) {
			original.add(i);
		}

		var reversed = new ReversedList<>(original);

		assertSize(1000, reversed);
		assertEquals(999, reversed.get(0));
		assertEquals(0, reversed.get(999));
	}

	@Test
	void i02_toArray() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var array = reversed.toArray();

		assertEquals(3, array.length);
		assertEquals("C", array[0]);
		assertEquals("B", array[1]);
		assertEquals("A", array[2]);
	}

	@Test
	void i03_toArrayTyped() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		var array = reversed.toArray(new String[0]);

		assertEquals(3, array.length);
		assertEquals("C", array[0]);
		assertEquals("B", array[1]);
		assertEquals("A", array[2]);
	}

	//====================================================================================================
	// listIterator with index parameter
	//====================================================================================================

	@Test
	void j01_listIterator_withIndex_start() {
		var original = List.of("A", "B", "C", "D");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(0);
		assertEquals("D", it.next());
		assertEquals("C", it.next());
		assertEquals("B", it.next());
		assertEquals("A", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void j02_listIterator_withIndex_middle() {
		var original = List.of("A", "B", "C", "D");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(2);
		assertEquals("B", it.next());
		assertEquals("A", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void j03_listIterator_withIndex_end() {
		var original = List.of("A", "B", "C", "D");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(4);
		assertFalse(it.hasNext());
		assertTrue(it.hasPrevious());
		assertEquals("A", it.previous());
	}

	@Test
	void j04_listIterator_withIndex_bidirectional() {
		var original = List.of("A", "B", "C", "D");
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(2);
		assertEquals(2, it.nextIndex());
		assertEquals(1, it.previousIndex());
		assertEquals("B", it.next());
		assertEquals(3, it.nextIndex());
		assertEquals(2, it.previousIndex());
		assertEquals("B", it.previous());
		assertEquals("C", it.previous());
	}

	@Test
	void j05_listIterator_withIndex_outOfBounds_negative() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(IndexOutOfBoundsException.class, () -> reversed.listIterator(-1));
	}

	@Test
	void j06_listIterator_withIndex_outOfBounds_tooLarge() {
		var original = List.of("A", "B", "C");
		var reversed = new ReversedList<>(original);

		assertThrows(IndexOutOfBoundsException.class, () -> reversed.listIterator(4));
	}

	@Test
	void j07_listIterator_withIndex_emptyList() {
		var original = List.<String>of();
		var reversed = new ReversedList<>(original);

		var it = reversed.listIterator(0);
		assertFalse(it.hasNext());
		assertFalse(it.hasPrevious());
	}
}

