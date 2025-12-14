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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class MultiList_Test extends TestBase {

	@Test
	void a01_basicIteration() {
		List<String> l1, l2;
		MultiList<String> ml;

		l1 = l(a("1", "2"));
		l2 = l(a("3", "4"));
		ml = new MultiList<>(l1, l2);
		var i1 = ml.iterator();
		assertTrue(i1.hasNext());
		assertEquals("1", i1.next());
		assertTrue(i1.hasNext());
		assertEquals("2", i1.next());
		assertTrue(i1.hasNext());
		assertEquals("3", i1.next());
		assertTrue(i1.hasNext());
		assertEquals("4", i1.next());
		assertFalse(i1.hasNext());
		assertThrows(NoSuchElementException.class, i1::next);
	}

	@Test
	void a02_emptySecondList() {
		List<String> l1 = l(a("1", "2"));
		List<String> l2 = l(a());
		MultiList<String> ml = new MultiList<>(l1, l2);
		var i2 = ml.iterator();
		assertTrue(i2.hasNext());
		assertEquals("1", i2.next());
		assertTrue(i2.hasNext());
		assertEquals("2", i2.next());
		assertFalse(i2.hasNext());
		assertThrows(NoSuchElementException.class, i2::next);
	}

	@Test
	void a03_emptyFirstList() {
		List<String> l1 = l(a());
		List<String> l2 = l(a("3", "4"));
		MultiList<String> ml = new MultiList<>(l1, l2);
		var i3 = ml.iterator();
		assertTrue(i3.hasNext());
		assertEquals("3", i3.next());
		assertTrue(i3.hasNext());
		assertEquals("4", i3.next());
		assertFalse(i3.hasNext());
		assertThrows(NoSuchElementException.class, i3::next);
	}

	@Test
	void a04_bothEmptyLists() {
		List<String> l1 = l(a());
		List<String> l2 = l(a());
		MultiList<String> ml = new MultiList<>(l1, l2);
		var i4 = ml.iterator();
		assertFalse(i4.hasNext());
		assertThrows(NoSuchElementException.class, i4::next);
	}

	@Test
	void a05_singleList() {
		List<String> l1 = l(a("1", "2"));
		MultiList<String> ml = new MultiList<>(l1);
		var i5 = ml.iterator();
		assertTrue(i5.hasNext());
		assertEquals("1", i5.next());
		assertTrue(i5.hasNext());
		assertEquals("2", i5.next());
		assertFalse(i5.hasNext());
		assertThrows(NoSuchElementException.class, i5::next);
	}

	@Test
	void a06_assertListAndEnumerator() {
		List<String> l1 = new LinkedList<>(l(a("1", "2")));
		List<String> l2 = new LinkedList<>(l(a("3", "4")));
		MultiList<String> ml = new MultiList<>(l1, l2);
		assertList(ml, "1", "2", "3", "4");
		assertList(ml.enumerator(), "1", "2", "3", "4");
		assertSize(4, ml);
	}

	@Test
	void a07_iteratorRemove() {
		List<String> l1 = new LinkedList<>(l(a("1", "2")));
		List<String> l2 = new LinkedList<>(l(a("3", "4")));
		MultiList<String> ml = new MultiList<>(l1, l2);

		var t = ml.iterator();
		t.next();
		t.remove();
		assertList(ml.enumerator(), "2", "3", "4");

		t = ml.iterator();
		t.next();
		t.remove();
		assertList(ml.enumerator(), "3", "4");

		t = ml.iterator();
		t.next();
		t.remove();
		assertList(ml.enumerator(), "4");

		t = ml.iterator();
		t.next();
		t.remove();
		assertEmpty(ml.enumerator());
		assertEmpty(ml);
	}

	@Test
	void a08_emptyMultiList() {
		MultiList<String> ml = new MultiList<>();
		assertEmpty(ml);
		assertThrows(NoSuchElementException.class, () -> new MultiList<String>().iterator().next());
		assertThrows(IllegalStateException.class, () -> new MultiList<String>().iterator().remove());
	}

	@Test
	void a09_nullListThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> new MultiList<>((List<String>)null));
	}

	@Test
	void a10_hasNext_whenCurrentIteratorExhausted_butMoreListsHaveElements() {
		// Test the hasNext() logic when current iterator is exhausted but remaining lists have elements
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var l3 = l(a("5", "6"));
		var ml = new MultiList<>(l1, l2, l3);
		var it = ml.iterator();

		// Exhaust the first list's iterator
		assertTrue(it.hasNext());
		assertEquals("1", it.next());
		assertTrue(it.hasNext());
		assertEquals("2", it.next());

		// Now i2.hasNext() should be false, but hasNext() should return true
		// because there are more lists with elements
		assertTrue(it.hasNext()); // Should check remaining lists
		assertEquals("3", it.next());

		// Continue to exhaust second list
		assertTrue(it.hasNext());
		assertEquals("4", it.next());

		// Now should check third list
		assertTrue(it.hasNext());
		assertEquals("5", it.next());
		assertTrue(it.hasNext());
		assertEquals("6", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void a11_hasNext_withEmptyListsInBetween() {
		// Test hasNext() when there are empty lists between non-empty ones
		var l1 = l(a("1"));
		var l2 = l(new String[0]);
		var l3 = l(a("2"));
		var l4 = l(new String[0]);
		var l5 = l(a("3"));
		var ml = new MultiList<>(l1, l2, l3, l4, l5);
		var it = ml.iterator();

		// Exhaust first list
		assertTrue(it.hasNext());
		assertEquals("1", it.next());

		// Now hasNext() should skip empty lists and find l3
		assertTrue(it.hasNext()); // Should skip l2 (empty) and find l3
		assertEquals("2", it.next());

		// Should skip l4 (empty) and find l5
		assertTrue(it.hasNext());
		assertEquals("3", it.next());
		assertFalse(it.hasNext());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// get(int index) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getByIndex() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4", "5"));
		var ml = new MultiList<>(l1, l2);

		assertEquals("1", ml.get(0));
		assertEquals("2", ml.get(1));
		assertEquals("3", ml.get(2));
		assertEquals("4", ml.get(3));
		assertEquals("5", ml.get(4));
	}

	@Test
	void b02_getByIndex_withEmptyLists() {
		var l1 = l(a("1"));
		var l2 = l(new String[0]);
		var l3 = l(a("2", "3"));
		var ml = new MultiList<>(l1, l2, l3);

		assertEquals("1", ml.get(0));
		assertEquals("2", ml.get(1));
		assertEquals("3", ml.get(2));
	}

	@Test
	void b03_getByIndex_outOfBounds() {
		var l1 = l(a("1", "2"));
		var ml = new MultiList<>(l1);

		assertThrows(IndexOutOfBoundsException.class, () -> ml.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> ml.get(2));
	}

	@Test
	void b04_getByIndex_singleElementLists() {
		var l1 = l(a("1"));
		var l2 = l(a("2"));
		var l3 = l(a("3"));
		var ml = new MultiList<>(l1, l2, l3);

		assertEquals("1", ml.get(0));
		assertEquals("2", ml.get(1));
		assertEquals("3", ml.get(2));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// listIterator() tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_listIterator_forward() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator();

		assertTrue(li.hasNext());
		assertEquals(0, li.nextIndex());
		assertEquals("1", li.next());
		assertEquals(1, li.nextIndex());
		assertEquals("2", li.next());
		assertEquals(2, li.nextIndex());
		assertEquals("3", li.next());
		assertEquals(3, li.nextIndex());
		assertEquals("4", li.next());
		assertEquals(4, li.nextIndex());
		assertFalse(li.hasNext());
	}

	@Test
	void c02_listIterator_backward() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator(ml.size());

		assertTrue(li.hasPrevious());
		assertEquals(3, li.previousIndex());
		assertEquals("4", li.previous());
		assertEquals(2, li.previousIndex());
		assertEquals("3", li.previous());
		assertEquals(1, li.previousIndex());
		assertEquals("2", li.previous());
		assertEquals(0, li.previousIndex());
		assertEquals("1", li.previous());
		assertEquals(-1, li.previousIndex());
		assertFalse(li.hasPrevious());
	}

	@Test
	void c03_listIterator_bidirectional() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator();

		// Forward
		assertEquals("1", li.next());
		assertEquals("2", li.next());

		// Backward
		assertEquals("2", li.previous());
		assertEquals("1", li.previous());

		// Forward again
		assertEquals("1", li.next());
		assertEquals("2", li.next());
		assertEquals("3", li.next());
	}

	@Test
	void c04_listIterator_remove() {
		var l1 = new LinkedList<>(l(a("1", "2")));
		var l2 = new LinkedList<>(l(a("3", "4")));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator();

		li.next(); // "1"
		li.next(); // "2"
		li.remove(); // Remove "2"
		assertList(ml, "1", "3", "4");

		li.next(); // "3"
		li.remove(); // Remove "3"
		assertList(ml, "1", "4");
	}

	@Test
	void c05_listIterator_set() {
		var l1 = new ArrayList<>(l(a("1", "2")));
		var l2 = new ArrayList<>(l(a("3", "4")));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator();

		li.next(); // "1"
		li.set("10");
		assertEquals("10", ml.get(0));

		li.next(); // "2"
		li.next(); // "3"
		li.set("30");
		assertEquals("30", ml.get(2));
	}

	@Test
	void c06_listIterator_addThrowsException() {
		var l1 = l(a("1", "2"));
		var ml = new MultiList<>(l1);
		var li = ml.listIterator();

		li.next();
		assertThrows(UnsupportedOperationException.class, () -> li.add("x"));
	}

	@Test
	void c07_listIterator_startAtIndex() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator(2);

		assertTrue(li.hasNext());
		assertEquals(2, li.nextIndex());
		assertEquals("3", li.next());
		assertEquals("4", li.next());
		assertFalse(li.hasNext());
	}

	@Test
	void c08_listIterator_startAtEnd() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);
		var li = ml.listIterator(ml.size());

		assertFalse(li.hasNext());
		assertTrue(li.hasPrevious());
		assertEquals("4", li.previous());
	}

	@Test
	void c09_listIterator_outOfBounds() {
		var l1 = l(a("1", "2"));
		var ml = new MultiList<>(l1);

		assertThrows(IndexOutOfBoundsException.class, () -> ml.listIterator(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> ml.listIterator(3));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// size() tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_size() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4", "5"));
		var ml = new MultiList<>(l1, l2);

		assertEquals(5, ml.size());
	}

	@Test
	void d02_size_withEmptyLists() {
		var l1 = l(new String[0]);
		var l2 = l(a("1"));
		var l3 = l(new String[0]);
		var ml = new MultiList<>(l1, l2, l3);

		assertEquals(1, ml.size());
	}

	@Test
	void d03_size_emptyMultiList() {
		var ml = new MultiList<String>();
		assertEquals(0, ml.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Integration tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_forEach() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);
		var result = new ArrayList<String>();

		ml.forEach(result::add);
		assertList(result, "1", "2", "3", "4");
	}

	@Test
	void e02_stream() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);

		var result = ml.stream().toList();
		assertList(result, "1", "2", "3", "4");
	}

	@Test
	void e03_indexOf() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "2", "4"));
		var ml = new MultiList<>(l1, l2);

		assertEquals(1, ml.indexOf("2")); // First occurrence at index 1
		assertEquals(3, ml.lastIndexOf("2")); // Last occurrence at index 3 (not 4, which is "4")
	}

	@Test
	void e04_contains() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);

		assertTrue(ml.contains("2"));
		assertTrue(ml.contains("3"));
		assertFalse(ml.contains("5"));
	}

	@Test
	void e05_toArray() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var ml = new MultiList<>(l1, l2);

		var array = ml.toArray();
		assertEquals(4, array.length);
		assertEquals("1", array[0]);
		assertEquals("2", array[1]);
		assertEquals("3", array[2]);
		assertEquals("4", array[3]);
	}

	//====================================================================================================
	// toString()
	//====================================================================================================

	@Test
	void f01_toString_singleList() {
		var l1 = l(a("1", "2"));
		var ml = new MultiList<>(l1);

		var expected = "[" + l1.toString() + "]";
		assertEquals(expected, ml.toString());
	}

	@Test
	void f02_toString_multipleLists() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var l3 = l(a("5", "6"));
		var ml = new MultiList<>(l1, l2, l3);

		var expected = "[" + l1.toString() + ", " + l2.toString() + ", " + l3.toString() + "]";
		assertEquals(expected, ml.toString());
	}

	@Test
	void f03_toString_emptyLists() {
		var l1 = l(a());
		var l2 = l(a());
		var ml = new MultiList<>(l1, l2);

		var expected = "[" + l1.toString() + ", " + l2.toString() + "]";
		assertEquals(expected, ml.toString());
	}

	@Test
	void f04_toString_mixedEmptyAndNonEmpty() {
		List<String> l1 = l(a());
		var l2 = l(a("1", "2"));
		List<String> l3 = l(a());
		var ml = new MultiList<>(l1, l2, l3);

		var expected = "[" + l1.toString() + ", " + l2.toString() + ", " + l3.toString() + "]";
		assertEquals(expected, ml.toString());
	}

	//====================================================================================================
	// equals() and hashCode()
	//====================================================================================================

	@Test
	void g01_equals_sameContents() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var multiList1 = new MultiList<>(l1, l2);

		var l3 = l(a("1", "2"));
		var l4 = l(a("3", "4"));
		var multiList2 = new MultiList<>(l3, l4);

		assertTrue(multiList1.equals(multiList2));
		assertTrue(multiList2.equals(multiList1));
	}

	@Test
	void g02_equals_differentContents() {
		var l1 = l(a("1", "2"));
		var multiList1 = new MultiList<>(l1);

		var l2 = l(a("1", "3"));
		var multiList2 = new MultiList<>(l2);

		assertFalse(multiList1.equals(multiList2));
		assertFalse(multiList2.equals(multiList1));
	}

	@Test
	void g03_equals_differentOrder() {
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var multiList1 = new MultiList<>(l1, l2);

		var l3 = l(a("3", "4"));
		var l4 = l(a("1", "2"));
		var multiList2 = new MultiList<>(l3, l4);

		assertFalse(multiList1.equals(multiList2)); // Order matters for lists
	}

	@Test
	void g04_equals_regularList() {
		var l1 = l(a("1", "2", "3"));
		var multiList = new MultiList<>(l1);

		var regularList = new ArrayList<>(l(a("1", "2", "3")));

		assertTrue(multiList.equals(regularList));
		assertTrue(regularList.equals(multiList));
	}

	@Test
	void g05_equals_notAList() {
		var l1 = l(a("1", "2"));
		var multiList = new MultiList<>(l1);
		assertFalse(multiList.equals(null));
	}

	@Test
	void g06_hashCode_sameContents() {
		var l1 = l(a("1", "2", "3"));
		var multiList1 = new MultiList<>(l1);

		var l2 = l(a("1", "2", "3"));
		var multiList2 = new MultiList<>(l2);

		assertEquals(multiList1.hashCode(), multiList2.hashCode());
	}

	@Test
	void g07_hashCode_regularList() {
		var l1 = l(a("1", "2", "3"));
		var multiList = new MultiList<>(l1);

		var regularList = new ArrayList<>(l(a("1", "2", "3")));

		assertEquals(multiList.hashCode(), regularList.hashCode());
	}
}

