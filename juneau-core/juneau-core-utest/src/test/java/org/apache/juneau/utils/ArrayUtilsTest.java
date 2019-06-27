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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class ArrayUtilsTest {

	//====================================================================================================
	// iterator(T[])
	//====================================================================================================
	@Test
	public void testArrayIterator() throws Exception {
		assertObjectEquals("[1,2,3]", iterator(new Object[]{1,2,3}));
		assertObjectEquals("[1,2,3]", iterator(new int[]{1,2,3}));
		assertObjectEquals("[]", iterator(null));
	}

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test
	public void testAppendArrayToArray() throws Exception {
		String[] s = new String[0];

		s = append(s, "a", "b");
		assertObjectEquals("['a','b']", s);

		s = append(s, "c");
		assertObjectEquals("['a','b','c']", s);

		s = append(s);
		assertObjectEquals("['a','b','c']", s);

		Object[] o = append((Object[])null);
		assertObjectEquals("[]", o);

		s = append((String[])null, "a", "b");
		assertObjectEquals("['a','b']", s);
	}

	//====================================================================================================
	// append(T[], Collection)
	//====================================================================================================
	@Test
	public void testAppendCollectionToArray() throws Exception {
		String[] s = new String[0];

		s = append(s, Arrays.asList(new String[]{"a","b"}));
		assertObjectEquals("['a','b']", s);

		s = append(s, Arrays.asList(new String[]{"c"}));
		assertObjectEquals("['a','b','c']", s);

		s = append(s, Arrays.asList(new String[0]));
		assertObjectEquals("['a','b','c']", s);

		try {
			append((Object[])null, Collections.emptyList());
			fail();
		} catch (IllegalArgumentException e) {}
	}

	//====================================================================================================
	// reverse(T[] array)
	//====================================================================================================
	@Test
	public void testReverse() throws Exception {
		String[] s = null;

		assertNull(reverse(s));

		s = new String[]{};
		assertObjectEquals("[]", reverse(s));

		s = new String[]{"a"};
		assertObjectEquals("['a']", reverse(s));

		s = new String[]{"a","b"};
		assertObjectEquals("['b','a']", reverse(s));

		s = new String[]{"a","b","c"};
		assertObjectEquals("['c','b','a']", reverse(s));
}

	//====================================================================================================
	// reverseInline(T[] array)
	//====================================================================================================
	@Test
	public void testReverseInline() throws Exception {
		String[] s = null;

		assertNull(reverseInline(s));

		s = new String[]{};
		assertObjectEquals("[]", reverseInline(s));

		s = new String[]{"a"};
		assertObjectEquals("['a']", reverseInline(s));

		s = new String[]{"a","b"};
		assertObjectEquals("['b','a']", reverseInline(s));

		s = new String[]{"a","b","c"};
		assertObjectEquals("['c','b','a']", reverseInline(s));
	}

	//====================================================================================================
	// reverseInline(T[] array)
	//====================================================================================================
	@Test
	public void testToReverseArray() throws Exception {
		String[] s = null;

		assertNull(toReverseArray(String.class, null));

		s = new String[]{};
		assertObjectEquals("[]", toReverseArray(String.class, Arrays.asList(s)));

		s = new String[]{"a"};
		assertObjectEquals("['a']", toReverseArray(String.class, Arrays.asList(s)));

		s = new String[]{"a","b"};
		assertObjectEquals("['b','a']", toReverseArray(String.class, Arrays.asList(s)));

		s = new String[]{"a","b","c"};
		assertObjectEquals("['c','b','a']", toReverseArray(String.class, Arrays.asList(s)));
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test
	public void testAsSet() throws Exception {
		String[] s = null;

		try {
			asSet(s);
			fail();
		} catch (IllegalArgumentException e) {}

		s = new String[]{"a"};
		Iterator<String> i = asSet(s).iterator();
		assertEquals("a", i.next());

		try {
			i.remove();
			fail();
		} catch (UnsupportedOperationException e) {}

		try {
			i.next();
			fail();
		} catch (NoSuchElementException e) {}
	}

	//====================================================================================================
	// iterator(T[])
	//====================================================================================================
	@Test
	public void testIterator() throws Exception {
		String[] s = null;

		s = new String[]{"a"};
		Iterator<Object> i = iterator(s);
		assertEquals("a", i.next());

		try {
			i.remove();
			fail();
		} catch (UnsupportedOperationException e) {}

		try {
			i.next();
			fail();
		} catch (NoSuchElementException e) {}
	}

	//====================================================================================================
	// combine(T[]...)
	//====================================================================================================
	@Test
	public void testCombine() throws Exception {
		String[] s1 = new String[]{"a"}, s2 = new String[]{"b"};

		assertObjectEquals("['a','b']", combine(s1, s2));
		assertObjectEquals("['a']", combine(s1));
		assertObjectEquals("['b']", combine(s2));
		assertObjectEquals("['a']", combine(s1,null));
		assertObjectEquals("['b']", combine(null,s2));
		assertNull(combine(null,null));
		assertNull(combine());
	}
}
