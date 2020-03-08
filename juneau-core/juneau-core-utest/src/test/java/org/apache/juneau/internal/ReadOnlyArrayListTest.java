// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

@SuppressWarnings("unchecked")
public class ReadOnlyArrayListTest {

	private static <T> UnmodifiableArray<T> create(T...t) {
		return new UnmodifiableArray<>(t);
	}

	private static <T> UnmodifiableArray<T> createReversed(T...t) {
		return new UnmodifiableArray<>(t, true);
	}

	@Test
	public void testBasic() {
		List<String> l = create("a","b","c");
		assertEquals("a", l.get(0));
		assertEquals("b", l.get(1));
		assertEquals("c", l.get(2));
	}

	@Test
	public void testBasicReversed() {
		List<String> l = createReversed("a","b","c");
		assertEquals("c", l.get(0));
		assertEquals("b", l.get(1));
		assertEquals("a", l.get(2));
	}

	@Test
	public void testIterator() {
		List<String> l = create("a","b","c");
		Iterator<String> i = l.iterator();
		assertTrue(i.hasNext());
		assertEquals("a", i.next());
		assertTrue(i.hasNext());
		assertEquals("b", i.next());
		assertTrue(i.hasNext());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());

		l = create();
		assertFalse(l.iterator().hasNext());
	}

	@Test
	public void testIteratorReversed() {
		List<String> l = createReversed("a","b","c");
		Iterator<String> i = l.iterator();
		assertTrue(i.hasNext());
		assertEquals("c", i.next());
		assertTrue(i.hasNext());
		assertEquals("b", i.next());
		assertTrue(i.hasNext());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());

		l = createReversed();
		assertFalse(l.iterator().hasNext());
	}

	@Test
	public void testSize() {
		assertEquals(1, create("a").size());
	}

	@Test
	public void testIsEmpty() {
		assertFalse(create("a").isEmpty());
		assertTrue(create().isEmpty());
	}

	@Test
	public void testContains() {
		assertTrue(create("a").contains("a"));
		assertFalse(create("a").contains("b"));
		assertFalse(create("a").contains(null));
		assertTrue(create("a", null).contains(null));
	}

	@Test
	public void testToArray() {
		String[] s = new String[]{"a"};
		List<String> l = new UnmodifiableArray<>(s);
		String[] s2 = (String[])l.toArray();
		assertEquals("a", s2[0]);
		s2[0] = "b";
		assertEquals("a", s[0]);
	}

	@Test
	public void testToArray2() {
		String[] s = new String[]{"a"};
		List<String> l = new UnmodifiableArray<>(s);
		String[] s2 = l.toArray(new String[l.size()]);
		assertEquals("a", s2[0]);
		s2[0] = "b";
		assertEquals("a", s[0]);
	}

	@Test
	public void testContainsAll() {
		assertTrue(create("a").containsAll(Arrays.asList("a")));
		assertFalse(create("a").containsAll(Arrays.asList("a","b")));
		assertFalse(create("a").containsAll(Arrays.asList("b")));
		assertFalse(create("a").containsAll(Arrays.asList((String)null)));
		assertTrue(create("a", null).containsAll(Arrays.asList((String)null)));
	}

	@Test
	public void testIndexOf() {
		List<String> l = create("a","b","a","c");
		assertEquals(0, l.indexOf("a"));
		assertEquals(2, l.lastIndexOf("a"));
		assertEquals(1, l.indexOf("b"));
		assertEquals(1, l.lastIndexOf("b"));
		assertEquals(3, l.indexOf("c"));
		assertEquals(3, l.lastIndexOf("c"));
		assertEquals(-1, l.indexOf("d"));
		assertEquals(-1, l.lastIndexOf("d"));
		assertEquals(-1, l.indexOf(null));
		assertEquals(-1, l.lastIndexOf(null));
	}
}
