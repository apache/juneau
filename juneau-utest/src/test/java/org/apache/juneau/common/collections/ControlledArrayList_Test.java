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

class ControlledArrayList_Test extends TestBase {

	@Nested
	class A_ConstructorTests extends TestBase {

		@Test
		void a01_emptyModifiable() {
			var x = new ControlledArrayList<>(false);
			assertTrue(x.isModifiable());
			assertEmpty(x);
		}

		@Test
		void a02_emptyUnmodifiable() {
			var x = new ControlledArrayList<>(true);
			assertFalse(x.isModifiable());
			assertEmpty(x);
		}

		@Test
		void a03_withInitialListModifiable() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3));
			assertTrue(x.isModifiable());
			assertList(x, 1, 2, 3);
		}

		@Test
		void a04_withInitialListUnmodifiable() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));
			assertFalse(x.isModifiable());
			assertList(x, 1, 2, 3);
		}

		@Test
		void a05_withEmptyList() {
			var x1 = new ControlledArrayList<>(false, l());
			var x2 = new ControlledArrayList<>(true, l());
			assertTrue(x1.isModifiable());
			assertFalse(x2.isModifiable());
			assertEmpty(x1);
			assertEmpty(x2);
		}
	}

	@Nested
	class B_ModificationTests extends TestBase {

		@Test
		void b01_set() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			assertEquals(2, x1.set(1, 99));
			assertThrows(UnsupportedOperationException.class, () -> x2.set(1, 99));
			x2.overrideSet(1, 99);
			assertList(x1, 1, 99, 3);
			assertList(x2, 1, 99, 3);
		}

		@Test
		void b02_add() {
			var x1 = new ControlledArrayList<>(false, l(1, 2));
			var x2 = new ControlledArrayList<>(true, l(1, 2));

			assertTrue(x1.add(3));
			assertThrows(UnsupportedOperationException.class, () -> x2.add(3));
			x2.overrideAdd(3);
			assertList(x1, 1, 2, 3);
			assertList(x2, 1, 2, 3);
		}

		@Test
		void b03_addAtIndex() {
			var x1 = new ControlledArrayList<>(false, l(1, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 3));

			x1.add(1, 2);
			assertThrows(UnsupportedOperationException.class, () -> x2.add(1, 2));
			x2.overrideAdd(1, 2);
			assertList(x1, 1, 2, 3);
			assertList(x2, 1, 2, 3);
		}

		@Test
		void b04_addAll() {
			var x1 = new ControlledArrayList<>(false, l(1));
			var x2 = new ControlledArrayList<>(true, l(1));

			assertTrue(x1.addAll(l(2, 3)));
			assertThrows(UnsupportedOperationException.class, () -> x2.addAll(l(2, 3)));
			x2.overrideAddAll(l(2, 3));
			assertList(x1, 1, 2, 3);
			assertList(x2, 1, 2, 3);
		}

		@Test
		void b05_addAllAtIndex() {
			var x1 = new ControlledArrayList<>(false, l(1, 4));
			var x2 = new ControlledArrayList<>(true, l(1, 4));

			assertTrue(x1.addAll(1, l(2, 3)));
			assertThrows(UnsupportedOperationException.class, () -> x2.addAll(1, l(2, 3)));
			x2.overrideAddAll(1, l(2, 3));
			assertList(x1, 1, 2, 3, 4);
			assertList(x2, 1, 2, 3, 4);
		}

		@Test
		void b06_removeByIndex() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			assertEquals(2, x1.remove(1));
			assertThrows(UnsupportedOperationException.class, () -> x2.remove(1));
			x2.overrideRemove(1);
			assertList(x1, 1, 3);
			assertList(x2, 1, 3);
		}

		@Test
		void b07_removeByObject() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			assertTrue(x1.remove((Integer)2));
			assertThrows(UnsupportedOperationException.class, () -> x2.remove((Integer)2));
			x2.overrideRemove((Integer)2);
			assertList(x1, 1, 3);
			assertList(x2, 1, 3);
		}

		@Test
		void b08_removeAll() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3, 4));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3, 4));

			assertTrue(x1.removeAll(l(2, 4)));
			assertThrows(UnsupportedOperationException.class, () -> x2.removeAll(l(2, 4)));
			x2.overrideRemoveAll(l(2, 4));
			assertList(x1, 1, 3);
			assertList(x2, 1, 3);
		}

		@Test
		void b09_retainAll() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3, 4));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3, 4));

			assertTrue(x1.retainAll(l(2, 4)));
			assertThrows(UnsupportedOperationException.class, () -> x2.retainAll(l(2, 4)));
			x2.overrideRetainAll(l(2, 4));
			assertList(x1, 2, 4);
			assertList(x2, 2, 4);
		}

		@Test
		void b10_clear() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			x1.clear();
			assertThrows(UnsupportedOperationException.class, x2::clear);
			x2.overrideClear();
			assertEmpty(x1);
			assertEmpty(x2);
		}

		@Test
		void b11_replaceAll() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			x1.replaceAll(x -> x * 2);
			assertThrows(UnsupportedOperationException.class, () -> x2.replaceAll(x -> x * 2));
			x2.overrideReplaceAll(x -> x * 2);
			assertList(x1, 2, 4, 6);
			assertList(x2, 2, 4, 6);
		}

		@Test
		void b12_removeIf() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3, 4));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3, 4));

			assertTrue(x1.removeIf(x -> x % 2 == 0));
			assertThrows(UnsupportedOperationException.class, () -> x2.removeIf(x -> x % 2 == 0));
			x2.overrideRemoveIf(x -> x % 2 == 0);
			assertList(x1, 1, 3);
			assertList(x2, 1, 3);
		}

		@Test
		void b13_sort() {
			var x1 = new ControlledArrayList<>(false, l(3, 1, 4, 2));
			var x2 = new ControlledArrayList<>(true, l(3, 1, 4, 2));

			x1.sort(null);
			assertThrows(UnsupportedOperationException.class, () -> x2.sort(null));
			x2.overrideSort(null);
			assertList(x1, 1, 2, 3, 4);
			assertList(x2, 1, 2, 3, 4);
		}

		@Test
		void b14_sortWithComparator() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3, 4));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3, 4));

			x1.sort((a, b) -> b.compareTo(a));
			assertThrows(UnsupportedOperationException.class, () -> x2.sort((a, b) -> b.compareTo(a)));
			x2.overrideSort((a, b) -> b.compareTo(a));
			assertList(x1, 4, 3, 2, 1);
			assertList(x2, 4, 3, 2, 1);
		}
	}

	@Nested
	class C_IteratorTests extends TestBase {

		@Test
		void c01_modifiableIterator() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3));
			var it = x.iterator();

			assertTrue(it.hasNext());
			assertEquals(1, it.next());
			it.remove();
			assertList(x, 2, 3);

			assertTrue(it.hasNext());
			assertEquals(2, it.next());
			assertTrue(it.hasNext());
			assertEquals(3, it.next());
			assertFalse(it.hasNext());
		}

		@Test
		void c02_unmodifiableIterator() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));
			var it = x.iterator();

			assertTrue(it.hasNext());
			assertEquals(1, it.next());
			assertThrows(UnsupportedOperationException.class, it::remove);

			assertTrue(it.hasNext());
			assertEquals(2, it.next());
			assertTrue(it.hasNext());
			assertEquals(3, it.next());
			assertFalse(it.hasNext());
		}

		@Test
		void c03_iteratorForEachRemaining() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));
			var list1 = new java.util.ArrayList<Integer>();
			var list2 = new java.util.ArrayList<Integer>();

			x1.iterator().forEachRemaining(list1::add);
			x2.iterator().forEachRemaining(list2::add);

			assertList(list1, 1, 2, 3);
			assertList(list2, 1, 2, 3);
		}

		@Test
		void c04_overrideIterator() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));
			var it = x.overrideIterator();

			assertTrue(it.hasNext());
			assertEquals(1, it.next());
			// Note: overrideIterator() returns the underlying iterator, but iterator.remove()
			// still goes through the list's remove() method which checks modifiability.
			// So we can only test that it returns a readable iterator.
			var list = new java.util.ArrayList<Integer>();
			it.forEachRemaining(list::add);
			assertList(list, 2, 3);
		}

		@Test
		void c05_emptyIterator() {
			var x1 = new ControlledArrayList<>(false);
			var x2 = new ControlledArrayList<>(true);

			assertFalse(x1.iterator().hasNext());
			assertFalse(x2.iterator().hasNext());
		}
	}

	@Nested
	class D_ListIteratorTests extends TestBase {

		@Test
		void d01_modifiableListIterator() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3));
			var it = x.listIterator();

			assertTrue(it.hasNext());
			assertFalse(it.hasPrevious());
			assertEquals(0, it.nextIndex());
			assertEquals(-1, it.previousIndex());

			assertEquals(1, it.next());
			assertTrue(it.hasPrevious());
			assertEquals(1, it.nextIndex());
			assertEquals(0, it.previousIndex());

			it.set(99);
			assertList(x, 99, 2, 3);

			it.add(100);
			assertList(x, 99, 100, 2, 3);

			assertEquals(2, it.nextIndex());
			assertEquals(1, it.previousIndex());
		}

		@Test
		void d02_unmodifiableListIterator() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));
			var it = x.listIterator();

			assertTrue(it.hasNext());
			assertFalse(it.hasPrevious());
			assertEquals(0, it.nextIndex());
			assertEquals(-1, it.previousIndex());

			assertEquals(1, it.next());
			assertTrue(it.hasPrevious());
			assertEquals(1, it.nextIndex());
			assertEquals(0, it.previousIndex());

			assertThrows(UnsupportedOperationException.class, () -> it.set(99));
			assertThrows(UnsupportedOperationException.class, () -> it.add(100));
			assertThrows(UnsupportedOperationException.class, it::remove);
		}

		@Test
		void d03_listIteratorWithIndex() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3, 4));
			var it = x.listIterator(2);

			assertTrue(it.hasNext());
			assertTrue(it.hasPrevious());
			assertEquals(2, it.nextIndex());
			assertEquals(1, it.previousIndex());

			assertEquals(3, it.next());
			assertEquals(2, it.previousIndex());
			assertEquals(3, it.previous());
			assertEquals(1, it.previousIndex());
		}

		@Test
		void d04_listIteratorWithIndexUnmodifiable() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3, 4));
			var it = x.listIterator(2);

			assertTrue(it.hasNext());
			assertTrue(it.hasPrevious());
			assertEquals(2, it.nextIndex());
			assertEquals(1, it.previousIndex());

			assertEquals(3, it.next());
			assertThrows(UnsupportedOperationException.class, () -> it.set(99));
			assertThrows(UnsupportedOperationException.class, () -> it.add(100));
			assertThrows(UnsupportedOperationException.class, it::remove);
		}

		@Test
		void d05_listIteratorForEachRemaining() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));
			var list1 = new java.util.ArrayList<Integer>();
			var list2 = new java.util.ArrayList<Integer>();

			x1.listIterator().forEachRemaining(list1::add);
			x2.listIterator().forEachRemaining(list2::add);

			assertList(list1, 1, 2, 3);
			assertList(list2, 1, 2, 3);
		}

		@Test
		void d06_overrideListIterator() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));
			var it = x.overrideListIterator(1);

			assertTrue(it.hasNext());
			assertEquals(2, it.next());
			// Note: overrideListIterator() returns the underlying iterator, but iterator.set()
			// still goes through the list's set() method which checks modifiability.
			// So we can only test that it returns a readable iterator at the correct position.
			assertTrue(it.hasPrevious());
			assertEquals(1, it.previousIndex());
			assertEquals(2, it.nextIndex());
		}

		@Test
		void d07_listIteratorBidirectional() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3));
			var it = x.listIterator();

			assertEquals(1, it.next());
			assertEquals(2, it.next());
			assertEquals(2, it.previous());
			assertEquals(1, it.previous());
			assertFalse(it.hasPrevious());
		}
	}

	@Nested
	class E_SubListTests extends TestBase {

		@Test
		void e01_subListModifiable() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3, 4, 5));
			var sub = (ControlledArrayList<Integer>) x.subList(1, 4);

			assertTrue(sub.isModifiable());
			assertList(sub, 2, 3, 4);

			// Note: subList creates a copy, not a view (because the constructor copies elements)
			// So modifications to subList don't affect the original list
			sub.set(0, 99);
			assertEquals(99, sub.get(0));
			assertList(sub, 99, 3, 4);
			// Original list is unchanged
			assertList(x, 1, 2, 3, 4, 5);
		}

		@Test
		void e02_subListUnmodifiable() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3, 4, 5));
			var sub = (ControlledArrayList<Integer>) x.subList(1, 4);

			assertFalse(sub.isModifiable());
			assertList(sub, 2, 3, 4);

			assertThrows(UnsupportedOperationException.class, () -> sub.set(0, 99));
		}

		@Test
		void e03_subListEmpty() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			var sub1 = (ControlledArrayList<Integer>) x1.subList(1, 1);
			var sub2 = (ControlledArrayList<Integer>) x2.subList(1, 1);

			assertTrue(sub1.isModifiable());
			assertFalse(sub2.isModifiable());
			assertEmpty(sub1);
			assertEmpty(sub2);
		}

		@Test
		void e04_subListFullRange() {
			var x1 = new ControlledArrayList<>(false, l(1, 2, 3));
			var x2 = new ControlledArrayList<>(true, l(1, 2, 3));

			var sub1 = (ControlledArrayList<Integer>) x1.subList(0, 3);
			var sub2 = (ControlledArrayList<Integer>) x2.subList(0, 3);

			assertTrue(sub1.isModifiable());
			assertFalse(sub2.isModifiable());
			assertList(sub1, 1, 2, 3);
			assertList(sub2, 1, 2, 3);
		}
	}

	@Nested
	class F_SetUnmodifiableTests extends TestBase {

		@Test
		void f01_setUnmodifiable() {
			var x = new ControlledArrayList<>(false, l(1, 2, 3));

			assertTrue(x.isModifiable());
			x.set(0, 99); // Should work

			var result = x.setUnmodifiable();
			assertSame(x, result); // Should return this
			assertFalse(x.isModifiable());

			assertThrows(UnsupportedOperationException.class, () -> x.set(0, 100));
			assertThrows(UnsupportedOperationException.class, () -> x.add(4));
			assertThrows(UnsupportedOperationException.class, () -> x.remove(0));
		}

		@Test
		void f02_setUnmodifiableAlreadyUnmodifiable() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));

			assertFalse(x.isModifiable());
			x.setUnmodifiable();
			assertFalse(x.isModifiable());
		}
	}

	@Nested
	class G_EdgeCaseTests extends TestBase {

		@Test
		void g01_emptyListOperations() {
			var x1 = new ControlledArrayList<>(false);
			var x2 = new ControlledArrayList<>(true);

			assertFalse(x1.addAll(l()));
			assertThrows(UnsupportedOperationException.class, () -> x2.addAll(l()));

			assertFalse(x1.removeAll(l()));
			assertThrows(UnsupportedOperationException.class, () -> x2.removeAll(l()));

			assertFalse(x1.retainAll(l()));
			assertThrows(UnsupportedOperationException.class, () -> x2.retainAll(l()));

			assertFalse(x1.remove((Integer)1));
			assertThrows(UnsupportedOperationException.class, () -> x2.remove((Integer)1));
		}

		@Test
		void g02_singleElementList() {
			var x1 = new ControlledArrayList<>(false, l(42));
			var x2 = new ControlledArrayList<>(true, l(42));

			assertEquals(42, x1.get(0));
			assertEquals(42, x2.get(0));

			x1.set(0, 99);
			assertThrows(UnsupportedOperationException.class, () -> x2.set(0, 99));
			x2.overrideSet(0, 99);

			assertEquals(99, x1.remove(0));
			assertThrows(UnsupportedOperationException.class, () -> x2.remove(0));
			x2.overrideRemove(0);

			assertEmpty(x1);
			assertEmpty(x2);
		}

		@Test
		void g03_overrideMethodsWorkWhenUnmodifiable() {
			var x = new ControlledArrayList<>(true, l(1, 2, 3));

			x.overrideAdd(4);
			x.overrideAdd(0, 0);
			x.overrideSet(2, 99);
			x.overrideRemove(3);
			x.overrideRemove((Integer)1);
			x.overrideAddAll(l(5, 6));
			x.overrideAddAll(0, l(-1));
			x.overrideRemoveAll(l(0));
			x.overrideRetainAll(l(2, 99, 5, 6));
			x.overrideReplaceAll(a -> a * 2);
			x.overrideRemoveIf(a -> a == 4);
			x.overrideSort(null);
			x.overrideClear();

			assertEmpty(x);
		}

		@Test
		void g04_isModifiable() {
			var x1 = new ControlledArrayList<>(false);
			var x2 = new ControlledArrayList<>(true);

			assertTrue(x1.isModifiable());
			assertFalse(x2.isModifiable());

			x1.setUnmodifiable();
			assertFalse(x1.isModifiable());
		}
	}
}