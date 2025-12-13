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

class FluentList_Test extends TestBase {

	//====================================================================================================
	// Basic functionality - a(E element)
	//====================================================================================================

	@Test
	void a01_addSingleElement() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		assertSize(3, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
	}

	@Test
	void a02_addSingleElement_returnsThis() {
		var list = new FluentList<>(new ArrayList<String>());
		var result = list.a("item1");

		assertSame(list, result);
	}

	@Test
	void a03_addSingleElement_nullValue() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a(null).a("item3");

		assertSize(3, list);
		assertEquals("item1", list.get(0));
		assertNull(list.get(1));
		assertEquals("item3", list.get(2));
	}

	//====================================================================================================
	// a(Collection) method
	//====================================================================================================

	@Test
	void b01_addCollection() {
		var list = new FluentList<>(new ArrayList<String>());
		var other = List.of("item1", "item2", "item3");
		list.aa(other);

		assertSize(3, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
	}

	@Test
	void b02_addCollection_returnsThis() {
		var list = new FluentList<>(new ArrayList<String>());
		var other = List.of("item1", "item2");
		var result = list.aa(other);

		assertSame(list, result);
	}

	@Test
	void b03_addCollection_nullCollection() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1");
		list.aa((Collection<String>)null);
		list.a("item2");

		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
	}

	@Test
	void b04_addCollection_emptyCollection() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1");
		list.aa(List.of());
		list.a("item2");

		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
	}

	@Test
	void b05_addCollection_multipleCalls() {
		var list = new FluentList<>(new ArrayList<String>());
		list.aa(List.of("item1", "item2"));
		list.aa(List.of("item3", "item4"));

		assertSize(4, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
		assertEquals("item4", list.get(3));
	}

	//====================================================================================================
	// ai(boolean, E) method
	//====================================================================================================

	@Test
	void c01_ai_conditionTrue() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").ai(true, "item2").a("item3");

		assertSize(3, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
	}

	@Test
	void c02_ai_conditionFalse() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").ai(false, "item2").a("item3");

		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item3", list.get(1));
	}

	@Test
	void c03_ai_returnsThis() {
		var list = new FluentList<>(new ArrayList<String>());
		var result1 = list.ai(true, "item1");
		var result2 = list.ai(false, "item2");

		assertSame(list, result1);
		assertSame(list, result2);
	}

	@Test
	void c04_ai_conditionalBuilding() {
		boolean includeDebug = true;
		boolean includeTest = false;

		var list = new FluentList<>(new ArrayList<String>())
			.a("basic")
			.ai(includeDebug, "debug")
			.ai(includeTest, "test");

		assertSize(2, list);
		assertTrue(list.contains("basic"));
		assertTrue(list.contains("debug"));
		assertFalse(list.contains("test"));
	}

	//====================================================================================================
	// Method chaining
	//====================================================================================================

	@Test
	void d01_methodChaining() {
		var list = new FluentList<>(new ArrayList<String>())
			.a("item1")
			.a("item2")
			.ai(true, "item3")
			.ai(false, "item4")
			.aa(List.of("item5", "item6"));

		assertSize(5, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
		assertEquals("item5", list.get(3));
		assertEquals("item6", list.get(4));
	}

	//====================================================================================================
	// List interface methods
	//====================================================================================================

	@Test
	void e01_listInterface_get() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
	}

	@Test
	void e02_listInterface_size() {
		var list = new FluentList<>(new ArrayList<String>());
		assertEquals(0, list.size());

		list.a("item1");
		assertEquals(1, list.size());

		list.a("item2");
		assertEquals(2, list.size());
	}

	@Test
	void e03_listInterface_add() {
		var list = new FluentList<>(new ArrayList<String>());
		assertTrue(list.add("item1"));
		assertTrue(list.add("item2"));

		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
	}

	@Test
	void e04_listInterface_addAtIndex() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item3");
		list.add(1, "item2");

		assertSize(3, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
	}

	@Test
	void e05_listInterface_addAll() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1");
		assertTrue(list.addAll(List.of("item2", "item3")));

		assertSize(3, list);
		assertEquals("item1", list.get(0));
		assertEquals("item2", list.get(1));
		assertEquals("item3", list.get(2));
	}

	@Test
	void e06_listInterface_remove() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		assertTrue(list.remove("item2"));
		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item3", list.get(1));
	}

	@Test
	void e07_listInterface_removeAtIndex() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		assertEquals("item2", list.remove(1));
		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item3", list.get(1));
	}

	@Test
	void e08_listInterface_set() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		assertEquals("item2", list.set(1, "item2-updated"));
		assertEquals("item2-updated", list.get(1));
	}

	@Test
	void e09_listInterface_indexOf() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item1");

		assertEquals(0, list.indexOf("item1"));
		assertEquals(1, list.indexOf("item2"));
		assertEquals(-1, list.indexOf("item3"));
	}

	@Test
	void e10_listInterface_lastIndexOf() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item1");

		assertEquals(2, list.lastIndexOf("item1"));
		assertEquals(1, list.lastIndexOf("item2"));
		assertEquals(-1, list.lastIndexOf("item3"));
	}

	@Test
	void e11_listInterface_contains() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2");

		assertTrue(list.contains("item1"));
		assertTrue(list.contains("item2"));
		assertFalse(list.contains("item3"));
	}

	@Test
	void e12_listInterface_containsAll() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		assertTrue(list.containsAll(List.of("item1", "item2")));
		assertTrue(list.containsAll(List.of("item1", "item2", "item3")));
		assertFalse(list.containsAll(List.of("item1", "item4")));
	}

	@Test
	void e13_listInterface_isEmpty() {
		var list = new FluentList<>(new ArrayList<String>());
		assertTrue(list.isEmpty());

		list.a("item1");
		assertFalse(list.isEmpty());
	}

	@Test
	void e14_listInterface_clear() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		list.clear();
		assertTrue(list.isEmpty());
		assertSize(0, list);
	}

	@Test
	void e15_listInterface_iterator() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		var found = new ArrayList<String>();
		for (var item : list) {
			found.add(item);
		}

		assertEquals(List.of("item1", "item2", "item3"), found);
	}

	@Test
	void e16_listInterface_listIterator() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		var it = list.listIterator();
		assertTrue(it.hasNext());
		assertEquals("item1", it.next());
		assertEquals("item2", it.next());
		assertTrue(it.hasPrevious());
		assertEquals("item2", it.previous());
	}

	@Test
	void e17_listInterface_subList() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3").a("item4");

		var subList = list.subList(1, 3);
		assertSize(2, subList);
		assertEquals("item2", subList.get(0));
		assertEquals("item3", subList.get(1));
	}

	@Test
	void e18_listInterface_toArray() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		var array = list.toArray();
		assertEquals(3, array.length);
		assertEquals("item1", array[0]);
		assertEquals("item2", array[1]);
		assertEquals("item3", array[2]);
	}

	@Test
	void e19_listInterface_toArrayTyped() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		var array = list.toArray(new String[0]);
		assertEquals(3, array.length);
		assertEquals("item1", array[0]);
		assertEquals("item2", array[1]);
		assertEquals("item3", array[2]);
	}

	//====================================================================================================
	// Different list implementations
	//====================================================================================================

	@Test
	void f01_linkedList() {
		var list = new FluentList<>(new LinkedList<String>());
		list.a("item1").a("item2").a("item3");

		assertSize(3, list);
	}

	@Test
	void f02_vector() {
		var list = new FluentList<>(new Vector<String>());
		list.a("item1").a("item2").a("item3");

		assertSize(3, list);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void g01_emptyList() {
		var list = new FluentList<>(new ArrayList<String>());

		assertTrue(list.isEmpty());
		assertSize(0, list);
		assertFalse(list.contains("anything"));
	}

	@Test
	void g02_removeAll() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3").a("item4");

		assertTrue(list.removeAll(List.of("item2", "item4")));
		assertSize(2, list);
		assertEquals("item1", list.get(0));
		assertEquals("item3", list.get(1));
	}

	@Test
	void g03_retainAll() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3").a("item4");

		assertTrue(list.retainAll(List.of("item2", "item4")));
		assertSize(2, list);
		assertEquals("item2", list.get(0));
		assertEquals("item4", list.get(1));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void w01_toString_delegatesToUnderlyingList() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		var underlyingList = new ArrayList<String>();
		underlyingList.add("item1");
		underlyingList.add("item2");
		underlyingList.add("item3");

		assertEquals(underlyingList.toString(), list.toString());
	}

	@Test
	void w02_equals_delegatesToUnderlyingList() {
		var list1 = new FluentList<>(new ArrayList<String>());
		list1.a("item1").a("item2").a("item3");

		var list2 = new ArrayList<String>();
		list2.add("item1");
		list2.add("item2");
		list2.add("item3");

		assertTrue(list1.equals(list2));
		assertTrue(list2.equals(list1));
	}

	@Test
	void w03_equals_differentContents_returnsFalse() {
		var list1 = new FluentList<>(new ArrayList<String>());
		list1.a("item1").a("item2");

		var list2 = new ArrayList<String>();
		list2.add("item1");
		list2.add("item3");

		assertFalse(list1.equals(list2));
		assertFalse(list2.equals(list1));
	}

	@Test
	void w04_hashCode_delegatesToUnderlyingList() {
		var list = new FluentList<>(new ArrayList<String>());
		list.a("item1").a("item2").a("item3");

		var underlyingList = new ArrayList<String>();
		underlyingList.add("item1");
		underlyingList.add("item2");
		underlyingList.add("item3");

		assertEquals(underlyingList.hashCode(), list.hashCode());
	}
}

