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

class MultiSet_Test extends TestBase {

	@Test void doTest() {
		List<String> l1, l2;
		MultiSet<String> ms;

		l1 = l(a("1","2"));
		l2 = l(a("3","4"));
		ms = new MultiSet<>(l1, l2);
		var i1 = ms.iterator();
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

		l1 = l(a("1","2"));
		l2 = l(a());
		ms = new MultiSet<>(l1, l2);
		var i2 = ms.iterator();
		assertTrue(i2.hasNext());
		assertEquals("1", i2.next());
		assertTrue(i2.hasNext());
		assertEquals("2", i2.next());
		assertFalse(i2.hasNext());
		assertThrows(NoSuchElementException.class, i2::next);

		l1 = l(a());
		l2 = l(a("3","4"));
		ms = new MultiSet<>(l1, l2);
		var i3 = ms.iterator();
		assertTrue(i3.hasNext());
		assertEquals("3", i3.next());
		assertTrue(i3.hasNext());
		assertEquals("4", i3.next());
		assertFalse(i3.hasNext());
		assertThrows(NoSuchElementException.class, i3::next);

		l1 = l(a());
		l2 = l(a());
		ms = new MultiSet<>(l1, l2);
		var i4 = ms.iterator();
		assertFalse(i4.hasNext());
		assertThrows(NoSuchElementException.class, i4::next);

		l1 = l(a("1","2"));
		ms = new MultiSet<>(l1);
		var i5 = ms.iterator();
		assertTrue(i5.hasNext());
		assertEquals("1", i5.next());
		assertTrue(i5.hasNext());
		assertEquals("2", i5.next());
		assertFalse(i5.hasNext());
		assertThrows(NoSuchElementException.class, i5::next);

		l1 = new LinkedList<>(l(a("1","2")));
		l2 = new LinkedList<>(l(a("3","4")));
		ms = new MultiSet<>(l1, l2);
		assertList(ms, "1", "2", "3", "4");
		assertList(ms.enumerator(), "1", "2", "3", "4");
		assertSize(4, ms);

		var t = ms.iterator();
		t.next();
		t.remove();
		assertList(ms.enumerator(), "2", "3", "4");

		t = ms.iterator();
		t.next();
		t.remove();
		assertList(ms.enumerator(), "3", "4");

		t = ms.iterator();
		t.next();
		t.remove();
		assertList(ms.enumerator(), "4");

		t = ms.iterator();
		t.next();
		t.remove();
		assertEmpty(ms.enumerator());
		assertEmpty(ms);

		ms = new MultiSet<>();
		assertEmpty(ms);
		assertEmpty(ms);

		assertThrows(IllegalArgumentException.class, ()->new MultiSet<>((Collection<String>)null));
		assertThrows(NoSuchElementException.class, ()->new MultiSet<String>().iterator().next());
		assertThrows(NoSuchElementException.class, ()->new MultiSet<String>().iterator().remove());
	}

	@Test
	void hasNext_whenCurrentIteratorExhausted_butMoreCollectionsHaveElements() {
		// Test the hasNext() logic when current iterator is exhausted but remaining collections have elements
		var l1 = l(a("1", "2"));
		var l2 = l(a("3", "4"));
		var l3 = l(a("5", "6"));
		var ms = new MultiSet<>(l1, l2, l3);
		var it = ms.iterator();

		// Exhaust the first collection's iterator
		assertTrue(it.hasNext());
		assertEquals("1", it.next());
		assertTrue(it.hasNext());
		assertEquals("2", it.next());

		// Now i2.hasNext() should be false, but hasNext() should return true
		// because there are more collections with elements (testing lines 214-216)
		assertTrue(it.hasNext()); // Should check remaining collections
		assertEquals("3", it.next());

		// Continue to exhaust second collection
		assertTrue(it.hasNext());
		assertEquals("4", it.next());

		// Now should check third collection
		assertTrue(it.hasNext());
		assertEquals("5", it.next());
		assertTrue(it.hasNext());
		assertEquals("6", it.next());
		assertFalse(it.hasNext());
	}

	@Test
	void hasNext_withEmptyCollectionsInBetween() {
		// Test hasNext() when there are empty collections between non-empty ones
		var l1 = l(a("1"));
		var l2 = l(new String[0]);
		var l3 = l(a("2"));
		var l4 = l(new String[0]);
		var l5 = l(a("3"));
		var ms = new MultiSet<>(l1, l2, l3, l4, l5);
		var it = ms.iterator();

		// Exhaust first collection
		assertTrue(it.hasNext());
		assertEquals("1", it.next());

		// Now hasNext() should skip empty collections and find l3
		assertTrue(it.hasNext()); // Should skip l2 (empty) and find l3
		assertEquals("2", it.next());

		// Should skip l4 (empty) and find l5
		assertTrue(it.hasNext());
		assertEquals("3", it.next());
		assertFalse(it.hasNext());
	}
}

