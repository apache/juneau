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
package org.apache.juneau.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for the marshaller-neutral {@link MarshalledList} base class.
 *
 * <p>
 * These tests exercise the neutral base directly to confirm that:
 * <ul>
 * 	<li>{@link MarshalledList#toString()} produces the inherited {@link LinkedList} form (not JSON).
 * 	<li>{@link MarshalledList#getMap} / {@link MarshalledList#getList} return the neutral types.
 * 	<li>Construction via the parser-required ctors works.
 * </ul>
 */
class MarshalledList_Test extends TestBase {

	@Test void a01_emptyToString() {
		var l = new MarshalledList();
		assertEquals("[]", l.toString());
	}

	@Test void a02_toStringIsLinkedListForm() {
		var l = new MarshalledList(1, "two", true);
		// LinkedList-style output, NOT JSON5.
		assertEquals("[1, two, true]", l.toString());
	}

	@Test void a03_factoryOf() {
		var l = MarshalledList.of(1, 2, 3);
		assertEquals(3, l.size());
		assertEquals(1, l.getInt(0));
		assertEquals(2, l.getInt(1));
		assertEquals(3, l.getInt(2));
	}

	@Test void a04_factoryCreateAndAppend() {
		var l = MarshalledList.create().append("a").append("b");
		assertEquals(2, l.size());
		assertEquals("a", l.getString(0));
	}

	@Test void a05_parseViaOfString() throws Exception {
		var l = MarshalledList.ofString("[1,2,3]", Json5Parser.DEFAULT);
		assertNotNull(l);
		assertEquals(3, l.size());
		assertEquals(2, l.getInt(1));
	}

	@Test void a06_parseViaOfStringNullInput() throws Exception {
		var l = MarshalledList.ofString((CharSequence)null, Json5Parser.DEFAULT);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(MarshalledList.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a06c_parseViaOfStringNullReader() throws Exception {
		var l = MarshalledList.ofString((java.io.Reader)null, Json5Parser.DEFAULT);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(MarshalledList.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a06b_ofTextAliasStillWorks() throws Exception {
		var l = MarshalledList.ofString("[1,2,3]", Json5Parser.DEFAULT);
		assertNotNull(l);
		assertEquals(3, l.size());
	}

	@Test void a07_getMapReturnsNeutralType() {
		var nested = new MarshalledMap("x", 1);
		var l = MarshalledList.of(nested);
		var m = l.getMap(0);
		assertTrue(m instanceof MarshalledMap, "Expected MarshalledMap, got " + m.getClass().getName());
		assertFalse(m instanceof JsonMap, "Stored MarshalledMap should not be returned as a JsonMap");
		assertEquals(1, m.getInt("x"));
	}

	@Test void a08_getListReturnsNeutralType() {
		var inner = MarshalledList.of(1, 2);
		// Use add() (inherited from LinkedList) to add inner as a single element rather than flattening it.
		var l = new MarshalledList();
		l.add(inner);
		var nested = l.getList(0);
		assertTrue(nested instanceof MarshalledList, "Expected MarshalledList, got " + nested.getClass().getName());
		assertFalse(nested instanceof JsonList, "Stored MarshalledList should not be returned as a JsonList");
		assertEquals(2, nested.size());
	}

	@Test void a09_typedAccessors() {
		var l = MarshalledList.of("42", "true", "9999999999", 123);
		assertEquals(42, l.getInt(0));
		assertTrue(l.getBoolean(1));
		assertEquals(9999999999L, l.getLong(2));
		assertEquals("123", l.getString(3));
	}

	@Test void a10_appendReverse() {
		var l = MarshalledList.create().appendReverse("a", "b", "c");
		assertEquals("c", l.getString(0));
		assertEquals("a", l.getString(2));
	}

	@Test void a11_unmodifiable_state() {
		var l = MarshalledList.of("a", "b", "c").unmodifiable();
		assertTrue(l.isUnmodifiable());
		assertEquals(3, l.size());
	}

	@Test void a11b_unmodifiable_listMutators() {
		var l = MarshalledList.of("a", "b", "c").unmodifiable();
		var listX = List.of("x");
		var listA = List.of("a");
		assertThrows(UnsupportedOperationException.class, () -> l.add(0, "x"));
		assertThrows(UnsupportedOperationException.class, () -> l.add("x"));
		assertThrows(UnsupportedOperationException.class, () -> l.remove(0));
		assertThrows(UnsupportedOperationException.class, () -> l.remove("a"));
		assertThrows(UnsupportedOperationException.class, () -> l.set(0, "x"));
		assertThrows(UnsupportedOperationException.class, () -> l.addAll(listX));
		assertThrows(UnsupportedOperationException.class, () -> l.addAll(0, listX));
		assertThrows(UnsupportedOperationException.class, () -> l.removeAll(listA));
		assertThrows(UnsupportedOperationException.class, () -> l.retainAll(listA));
		assertThrows(UnsupportedOperationException.class, l::clear);
	}

	@Test void a11c_unmodifiable_dequeMutators() {
		var l = MarshalledList.of("a", "b", "c").unmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> l.addFirst("x"));
		assertThrows(UnsupportedOperationException.class, () -> l.addLast("x"));
		assertThrows(UnsupportedOperationException.class, l::removeFirst);
		assertThrows(UnsupportedOperationException.class, l::removeLast);
		assertThrows(UnsupportedOperationException.class, () -> l.removeFirstOccurrence("a"));
		assertThrows(UnsupportedOperationException.class, () -> l.removeLastOccurrence("a"));
		assertThrows(UnsupportedOperationException.class, () -> l.offer("x"));
		assertThrows(UnsupportedOperationException.class, () -> l.offerFirst("x"));
		assertThrows(UnsupportedOperationException.class, () -> l.offerLast("x"));
		assertThrows(UnsupportedOperationException.class, l::poll);
		assertThrows(UnsupportedOperationException.class, l::pollFirst);
		assertThrows(UnsupportedOperationException.class, l::pollLast);
		assertThrows(UnsupportedOperationException.class, l::pop);
		assertThrows(UnsupportedOperationException.class, () -> l.push("x"));
	}

	@Test void a11d_unmodifiable_functionalMutators() {
		var l = MarshalledList.of("a", "b", "c").unmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> l.removeIf(o -> true));
		assertThrows(UnsupportedOperationException.class, () -> l.replaceAll(o -> "x"));
		assertThrows(UnsupportedOperationException.class, () -> l.sort((a, b) -> 0));
	}

	@Test void a11e_unmodifiable_iteratorMutators() {
		var l = MarshalledList.of("a", "b", "c").unmodifiable();
		var it = l.iterator();
		it.next();
		assertThrows(UnsupportedOperationException.class, it::remove);
		var lit = l.listIterator();
		lit.next();
		assertThrows(UnsupportedOperationException.class, lit::remove);
		assertThrows(UnsupportedOperationException.class, () -> lit.add("x"));
		assertThrows(UnsupportedOperationException.class, () -> lit.set("x"));
	}

	@Test void a12_modifiable() {
		var ro = MarshalledList.of("a").unmodifiable();
		var mod = ro.modifiable();
		assertFalse(mod.isUnmodifiable());
		// modifiable() returns a fresh copy that is itself modifiable.
		mod.add("b");
		assertEquals(2, mod.size());
	}

	@Test void a13_elementsIterableConvertsTypes() {
		var l = MarshalledList.of("1", "2", "3");
		var sum = 0;
		for (Integer i : l.elements(Integer.class))
			sum += i;
		assertEquals(6, sum);
	}

	@Test void a14_emptyListConstant() {
		assertTrue(MarshalledList.EMPTY_LIST.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> MarshalledList.EMPTY_LIST.add(0, "x"));
	}

	@Test void a15_ofCollection() {
		var l = MarshalledList.of(Arrays.asList("a", "b"));
		assertEquals(2, l.size());
		assertEquals("b", l.getString(1));
	}

	@Test void a16_ofArrays() {
		var l = MarshalledList.ofArrays(new Object[]{1, 2}, new Object[]{3, 4});
		assertEquals(2, l.size());
	}

	@Test void a17_appendIf() {
		var l = MarshalledList.create()
			.appendIf(true, "kept")
			.appendIf(false, "skipped");
		assertEquals(1, l.size());
		assertEquals("kept", l.getString(0));
	}
}
