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

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

class JsonList_Test extends TestBase {

	@Test void a01_create() {
		var l = JsonList.create();
		assertNotNull(l);
		assertTrue(l.isEmpty());
	}

	@Test void a02_ofValues() {
		var l = JsonList.of("a", 1, true);
		assertEquals(3, l.size());
		assertEquals("a", l.getString(0));
		assertEquals(1, l.getInt(1));
		assertTrue(l.getBoolean(2));
	}

	@Test void a03_ofCollection() {
		var src = Arrays.asList(1, 2, 3);
		var l = JsonList.of(src);
		assertEquals(3, l.size());
		assertEquals(1, l.getInt(0));
	}

	@Test void a04_ofCollectionNull() {
		assertNull(JsonList.of((Collection<?>) null));
	}

	@Test void a05_ofStringCharSequence() throws Exception {
		var l = JsonList.ofString("[1,2,3]");
		assertEquals(3, l.size());
		assertEquals(1, l.getInt(0));
		assertEquals(2, l.getInt(1));
		assertEquals(3, l.getInt(2));
	}

	@Test void a06_ofStringCharSequenceNull() throws Exception {
		var l = JsonList.ofString((CharSequence) null);
		assertNotNull(l);
		assertTrue(l.isEmpty());
	}

	@Test void a07_ofStringReader() throws Exception {
		var l = JsonList.ofString(new StringReader("[10,20]"));
		assertEquals(2, l.size());
		assertEquals(10, l.getInt(0));
	}

	@Test void a08_ofStringReaderNull() throws Exception {
		var l = JsonList.ofString((Reader) null);
		assertNotNull(l);
		assertTrue(l.isEmpty());
	}

	@Test void a09_ofStringWithParser() throws Exception {
		var l = JsonList.ofString("[1,2]", JsonParser.DEFAULT);
		assertEquals(2, l.size());
	}

	@Test void a10_toStringProducesJson() {
		var l = JsonList.of(1, 2, 3);
		assertEquals("[1,2,3]", l.toString());
	}

	@Test void a11_toJson() {
		var l = JsonList.of("a", "b");
		assertEquals("[\"a\",\"b\"]", l.toJson());
	}

	@Test void a12_toJson5() {
		var l = JsonList.of("a", "b");
		assertEquals("['a','b']", l.toJson5());
	}

	@Test void a13_appendFluent() {
		var l = JsonList.create().append("a").append(1).append(true);
		assertEquals(3, l.size());
		assertEquals("a", l.getString(0));
		assertEquals(1, l.getInt(1));
		assertTrue(l.getBoolean(2));
	}

	@Test void a14_appendVarargs() {
		var l = JsonList.create().append("a", "b", "c");
		assertEquals(3, l.size());
	}

	@Test void a15_appendIfTrue() {
		var l = JsonList.create().appendIf(true, "yes").appendIf(false, "no");
		assertEquals(1, l.size());
		assertEquals("yes", l.getString(0));
	}

	@Test void a16_appendReverse() {
		var l = JsonList.create().appendReverse("a", "b", "c");
		assertEquals(3, l.size());
		assertEquals("c", l.getString(0));
		assertEquals("b", l.getString(1));
		assertEquals("a", l.getString(2));
	}

	@Test void a17_appendReverseList() {
		var l = JsonList.create().appendReverse(Arrays.asList("x", "y"));
		assertEquals(2, l.size());
		assertEquals("y", l.getString(0));
		assertEquals("x", l.getString(1));
	}

	@Test void a18_appendCollection() {
		var l = JsonList.create().append(Arrays.asList(1, 2, 3));
		assertEquals(3, l.size());
	}

	@Test void a19_typedAccessors() {
		var l = JsonList.of("42", "true", "9999999999", 123);
		assertEquals(42, l.getInt(0));
		assertTrue(l.getBoolean(1));
		assertEquals(9999999999L, l.getLong(2));
		assertEquals("123", l.getString(3));
	}

	@Test void a20_getMap() {
		var map = JsonMap.of("a", 1);
		var l = JsonList.of(map);
		var got = l.getMap(0);
		assertTrue(got instanceof JsonMap);
		assertEquals(1, got.getInt("a"));
	}

	@Test void a21_getList() {
		var inner = JsonList.of(1, 2);
		var l = JsonList.create().append((Object) inner);
		var got = l.getList(0);
		assertTrue(got instanceof JsonList);
		assertEquals(2, got.size());
	}

	@SuppressWarnings({"java:S5961", // Test comprehensiveness requires more than 25 assertions.
		"java:S5778" // assertThrows lambdas contain multiple calls; only the collection-mutating call throws in practice.
	})
	@Test void a22_unmodifiable() {
		var l = JsonList.of(1, 2, 3).unmodifiable();
		assertTrue(l.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> l.add(0, 4));
		assertThrows(UnsupportedOperationException.class, () -> l.add(4));
		assertThrows(UnsupportedOperationException.class, () -> l.remove(0));
		assertThrows(UnsupportedOperationException.class, () -> l.remove((Object) Integer.valueOf(1)));
		assertThrows(UnsupportedOperationException.class, () -> l.set(0, 9));
		assertThrows(UnsupportedOperationException.class, () -> l.addAll(Arrays.asList(4, 5)));
		assertThrows(UnsupportedOperationException.class, () -> l.addAll(0, Arrays.asList(4, 5)));
		assertThrows(UnsupportedOperationException.class, () -> l.removeAll(Arrays.asList(1)));
		assertThrows(UnsupportedOperationException.class, () -> l.retainAll(Arrays.asList(1)));
		assertThrows(UnsupportedOperationException.class, l::clear);
		assertThrows(UnsupportedOperationException.class, () -> l.addFirst(0));
		assertThrows(UnsupportedOperationException.class, () -> l.addLast(4));
		assertThrows(UnsupportedOperationException.class, l::removeFirst);
		assertThrows(UnsupportedOperationException.class, l::removeLast);
		assertThrows(UnsupportedOperationException.class, () -> l.removeFirstOccurrence(1));
		assertThrows(UnsupportedOperationException.class, () -> l.removeLastOccurrence(3));
		assertThrows(UnsupportedOperationException.class, () -> l.offer(4));
		assertThrows(UnsupportedOperationException.class, () -> l.offerFirst(0));
		assertThrows(UnsupportedOperationException.class, () -> l.offerLast(4));
		assertThrows(UnsupportedOperationException.class, l::poll);
		assertThrows(UnsupportedOperationException.class, l::pollFirst);
		assertThrows(UnsupportedOperationException.class, l::pollLast);
		assertThrows(UnsupportedOperationException.class, l::pop);
		assertThrows(UnsupportedOperationException.class, () -> l.push(0));
		assertThrows(UnsupportedOperationException.class, () -> l.removeIf(x -> true));
		assertThrows(UnsupportedOperationException.class, () -> l.replaceAll(UnaryOperator.identity()));
		assertThrows(UnsupportedOperationException.class, () -> l.sort(null));
		assertThrows(UnsupportedOperationException.class, l::removeFirst);
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = l.iterator();
			it.next();
			it.remove();
		});
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = l.listIterator();
			it.next();
			it.remove();
		});
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = l.listIterator();
			it.add(99);
		});
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = l.listIterator();
			it.next();
			it.set(99);
		});
		assertEquals(3, l.size());
	}

	@Test void a23_modifiable() {
		var l = JsonList.of(1, 2).unmodifiable().modifiable();
		assertFalse(l.isUnmodifiable());
		l.add(3);
		assertEquals(3, l.size());
	}

	@Test void a24_emptyListConstant() {
		assertTrue(JsonList.EMPTY_LIST.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> JsonList.EMPTY_LIST.add(0, "x"));
	}

	@Test void a25_ofArrays() {
		var l = JsonList.ofArrays(new Object[]{1, 2}, new Object[]{3, 4});
		assertEquals(2, l.size());
	}

	@Test void a26_ofCollections() {
		var l = JsonList.ofCollections(Arrays.asList(1, 2), Arrays.asList(3, 4));
		assertEquals(2, l.size());
	}

	@Test void a27_ofJsonOrCdl_json() throws Exception {
		var l = JsonList.ofJsonOrCdl("[1,2,3]");
		assertEquals(3, l.size());
		assertEquals(1, l.getInt(0));
	}

	@Test void a28_ofJsonOrCdl_cdl() throws Exception {
		var l = JsonList.ofJsonOrCdl("a,b,c");
		assertEquals(3, l.size());
		assertEquals("a", l.getString(0));
		assertEquals("b", l.getString(1));
		assertEquals("c", l.getString(2));
	}

	@Test void a29_ofJsonOrCdl_null() throws Exception {
		var l = JsonList.ofJsonOrCdl(null);
		assertNotNull(l);
		assertTrue(l.isEmpty());
	}

	@Test void a30_ofJsonOrCdl_empty() throws Exception {
		var l = JsonList.ofJsonOrCdl("");
		assertNotNull(l);
		assertTrue(l.isEmpty());
	}

	@Test void a31_elements() {
		var l = JsonList.of("1", "2", "3");
		var count = 0;
		for (var s : l.elements(String.class)) {
			assertNotNull(s);
			count++;
		}
		assertEquals(3, count);
	}

	@Test void a32_writeTo() throws Exception {
		var l = JsonList.of(1, 2);
		var sw = new StringWriter();
		l.writeTo(sw);
		assertEquals("[1,2]", sw.toString());
	}

	@Test void a33_toStringWithSerializer() {
		var l = JsonList.of(1, 2);
		var s = l.toString(JsonSerializer.DEFAULT);
		assertEquals("[1,2]", s);
	}
}
