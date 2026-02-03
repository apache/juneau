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

class FluentSet_Test extends TestBase {

	//====================================================================================================
	// Basic functionality - a(E element)
	//====================================================================================================

	@Test
	void a01_addSingleElement() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		assertSize(3, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	@Test
	void a02_addSingleElement_returnsThis() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		var result = set.a("item1");

		assertSame(set, result);
	}

	@Test
	void a03_addSingleElement_nullValue() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a(null).a("item3");

		assertSize(3, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains(null));
		assertTrue(set.contains("item3"));
	}

	@Test
	void a04_addSingleElement_duplicateIgnored() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item1");

		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
	}

	//====================================================================================================
	// a(Collection) method
	//====================================================================================================

	@Test
	void b01_addCollection() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		var other = List.of("item1", "item2", "item3");
		set.aa(other);

		assertSize(3, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	@Test
	void b02_addCollection_returnsThis() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		var other = List.of("item1", "item2");
		var result = set.aa(other);

		assertSame(set, result);
	}

	@Test
	void b03_addCollection_nullCollection() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1");
		set.aa((Collection<String>)null);
		set.a("item2");

		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
	}

	@Test
	void b04_addCollection_emptyCollection() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1");
		set.aa(List.of());
		set.a("item2");

		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
	}

	@Test
	void b05_addCollection_duplicatesIgnored() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.aa(List.of("item1", "item2"));
		set.aa(List.of("item2", "item3"));  // item2 is duplicate

		assertSize(3, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	//====================================================================================================
	// ai(boolean, E) method
	//====================================================================================================

	@Test
	void c01_ai_conditionTrue() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").ai(true, "item2").a("item3");

		assertSize(3, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	@Test
	void c02_ai_conditionFalse() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").ai(false, "item2").a("item3");

		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertFalse(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	@Test
	void c03_ai_returnsThis() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		var result1 = set.ai(true, "item1");
		var result2 = set.ai(false, "item2");

		assertSame(set, result1);
		assertSame(set, result2);
	}

	@Test
	void c04_ai_conditionalBuilding() {
		boolean includeDebug = true;
		boolean includeTest = false;

		var set = new FluentSet<>(new LinkedHashSet<String>())
			.a("basic")
			.ai(includeDebug, "debug")
			.ai(includeTest, "test");

		assertSize(2, set);
		assertTrue(set.contains("basic"));
		assertTrue(set.contains("debug"));
		assertFalse(set.contains("test"));
	}

	//====================================================================================================
	// Method chaining
	//====================================================================================================

	@Test
	void d01_methodChaining() {
		var set = new FluentSet<>(new LinkedHashSet<String>())
			.a("item1")
			.a("item2")
			.ai(true, "item3")
			.ai(false, "item4")
			.aa(List.of("item5", "item6"));

		assertSize(5, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item3"));
		assertTrue(set.contains("item5"));
		assertTrue(set.contains("item6"));
		assertFalse(set.contains("item4"));
	}

	//====================================================================================================
	// Set interface methods
	//====================================================================================================

	@Test
	void e01_setInterface_size() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		assertEquals(0, set.size());

		set.a("item1");
		assertEquals(1, set.size());

		set.a("item2");
		assertEquals(2, set.size());
	}

	@Test
	void e02_setInterface_add() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		assertTrue(set.add("item1"));
		assertTrue(set.add("item2"));
		assertFalse(set.add("item1"));  // Duplicate, returns false

		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
	}

	@Test
	void e03_setInterface_addAll() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1");
		assertTrue(set.addAll(List.of("item2", "item3")));

		assertSize(3, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	@Test
	void e04_setInterface_remove() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		assertTrue(set.remove("item2"));
		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertFalse(set.contains("item2"));
		assertTrue(set.contains("item3"));
	}

	@Test
	void e05_setInterface_contains() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2");

		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item2"));
		assertFalse(set.contains("item3"));
	}

	@Test
	void e06_setInterface_containsAll() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		assertTrue(set.containsAll(List.of("item1", "item2")));
		assertTrue(set.containsAll(List.of("item1", "item2", "item3")));
		assertFalse(set.containsAll(List.of("item1", "item4")));
	}

	@Test
	void e07_setInterface_isEmpty() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		assertTrue(set.isEmpty());

		set.a("item1");
		assertFalse(set.isEmpty());
	}

	@Test
	void e08_setInterface_clear() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		set.clear();
		assertTrue(set.isEmpty());
		assertSize(0, set);
	}

	@Test
	void e09_setInterface_iterator() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		var found = new LinkedHashSet<String>();
		for (var item : set) {
			found.add(item);
		}

		assertSize(3, found);
		assertTrue(found.contains("item1"));
		assertTrue(found.contains("item2"));
		assertTrue(found.contains("item3"));
	}

	@Test
	void e10_setInterface_toArray() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		var array = set.toArray();
		assertEquals(3, array.length);
		// Order may vary, so just check all items are present
		var arrayList = Arrays.asList(array);
		assertTrue(arrayList.contains("item1"));
		assertTrue(arrayList.contains("item2"));
		assertTrue(arrayList.contains("item3"));
	}

	@Test
	void e11_setInterface_toArrayTyped() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		var array = set.toArray(new String[0]);
		assertEquals(3, array.length);
		// Order may vary, so just check all items are present
		var arrayList = Arrays.asList(array);
		assertTrue(arrayList.contains("item1"));
		assertTrue(arrayList.contains("item2"));
		assertTrue(arrayList.contains("item3"));
	}

	//====================================================================================================
	// Different set implementations
	//====================================================================================================

	@Test
	void f01_hashSet() {
		var set = new FluentSet<>(new HashSet<String>());
		set.a("item1").a("item2").a("item3");

		assertSize(3, set);
	}

	@Test
	void f02_treeSet() {
		var set = new FluentSet<>(new TreeSet<String>());
		set.a("zebra").a("apple").a("banana");

		assertSize(3, set);
		// TreeSet maintains sorted order
		var iterator = set.iterator();
		assertEquals("apple", iterator.next());
		assertEquals("banana", iterator.next());
		assertEquals("zebra", iterator.next());
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void g01_emptySet() {
		var set = new FluentSet<>(new LinkedHashSet<String>());

		assertTrue(set.isEmpty());
		assertSize(0, set);
		assertFalse(set.contains("anything"));
	}

	@Test
	void g02_removeAll() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3").a("item4");

		assertTrue(set.removeAll(List.of("item2", "item4")));
		assertSize(2, set);
		assertTrue(set.contains("item1"));
		assertTrue(set.contains("item3"));
		assertFalse(set.contains("item2"));
		assertFalse(set.contains("item4"));
	}

	@Test
	void g03_retainAll() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3").a("item4");

		assertTrue(set.retainAll(List.of("item2", "item4")));
		assertSize(2, set);
		assertTrue(set.contains("item2"));
		assertTrue(set.contains("item4"));
		assertFalse(set.contains("item1"));
		assertFalse(set.contains("item3"));
	}

	@Test
	void g04_duplicateHandling() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item1").a("item1");

		assertSize(1, set);
		assertTrue(set.contains("item1"));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void w01_toString_delegatesToUnderlyingSet() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		var underlyingSet = new LinkedHashSet<String>();
		underlyingSet.add("item1");
		underlyingSet.add("item2");
		underlyingSet.add("item3");

		assertEquals(underlyingSet.toString(), set.toString());
	}

	@Test
	void w02_equals_delegatesToUnderlyingSet() {
		var set1 = new FluentSet<>(new LinkedHashSet<String>());
		set1.a("item1").a("item2").a("item3");

		var set2 = new LinkedHashSet<String>();
		set2.add("item1");
		set2.add("item2");
		set2.add("item3");

		assertEquals(set1, set2);
		assertEquals(set2, set1);
	}

	@Test
	void w03_equals_differentContents_returnsFalse() {
		var set1 = new FluentSet<>(new LinkedHashSet<String>());
		set1.a("item1").a("item2");

		var set2 = new LinkedHashSet<String>();
		set2.add("item1");
		set2.add("item3");

		assertNotEquals(set1, set2);
		assertNotEquals(set2, set1);
	}

	@Test
	void w04_hashCode_delegatesToUnderlyingSet() {
		var set = new FluentSet<>(new LinkedHashSet<String>());
		set.a("item1").a("item2").a("item3");

		var underlyingSet = new LinkedHashSet<String>();
		underlyingSet.add("item1");
		underlyingSet.add("item2");
		underlyingSet.add("item3");

		assertEquals(underlyingSet.hashCode(), set.hashCode());
	}
}

