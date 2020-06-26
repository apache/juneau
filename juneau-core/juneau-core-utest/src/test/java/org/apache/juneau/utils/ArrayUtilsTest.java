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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.assertions.ThrowableAssertion.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ArrayUtilsTest {

	//====================================================================================================
	// iterator(T[])
	//====================================================================================================
	@Test
	public void testArrayIterator() throws Exception {
		assertObject(iterator(new Object[]{1,2,3})).json().is("[1,2,3]");
		assertObject(iterator(new int[]{1,2,3})).json().is("[1,2,3]");
		assertObject(iterator(null)).json().is("[]");
	}

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test
	public void testAppendArrayToArray() throws Exception {
		String[] s = new String[0];

		s = append(s, "a", "b");
		assertObject(s).json().is("['a','b']");

		s = append(s, "c");
		assertObject(s).json().is("['a','b','c']");

		s = append(s);
		assertObject(s).json().is("['a','b','c']");

		Object[] o = append((Object[])null);
		assertObject(o).json().is("[]");

		s = append((String[])null, "a", "b");
		assertObject(s).json().is("['a','b']");
	}

	//====================================================================================================
	// append(T[], Collection)
	//====================================================================================================
	@Test
	public void testAppendCollectionToArray() throws Exception {
		String[] s = new String[0];

		s = append(s, Arrays.asList(new String[]{"a","b"}));
		assertObject(s).json().is("['a','b']");

		s = append(s, Arrays.asList(new String[]{"c"}));
		assertObject(s).json().is("['a','b','c']");

		s = append(s, Arrays.asList(new String[0]));
		assertObject(s).json().is("['a','b','c']");

		assertThrown(()->{append((Object[])null, Collections.emptyList());}).isType(IllegalArgumentException.class);
	}

	//====================================================================================================
	// reverse(T[] array)
	//====================================================================================================
	@Test
	public void testReverse() throws Exception {
		String[] s = null;

		assertNull(reverse(s));

		s = new String[]{};
		assertObject(reverse(s)).json().is("[]");

		s = new String[]{"a"};
		assertObject(reverse(s)).json().is("['a']");

		s = new String[]{"a","b"};
		assertObject(reverse(s)).json().is("['b','a']");

		s = new String[]{"a","b","c"};
		assertObject(reverse(s)).json().is("['c','b','a']");
}

	//====================================================================================================
	// reverseInline(T[] array)
	//====================================================================================================
	@Test
	public void testReverseInline() throws Exception {
		String[] s = null;

		assertNull(reverseInline(s));

		s = new String[]{};
		assertObject(reverseInline(s)).json().is("[]");

		s = new String[]{"a"};
		assertObject(reverseInline(s)).json().is("['a']");

		s = new String[]{"a","b"};
		assertObject(reverseInline(s)).json().is("['b','a']");

		s = new String[]{"a","b","c"};
		assertObject(reverseInline(s)).json().is("['c','b','a']");
	}

	//====================================================================================================
	// reverseInline(T[] array)
	//====================================================================================================
	@Test
	public void testToReverseArray() throws Exception {
		String[] s = null;

		assertNull(toReverseArray(String.class, null));

		s = new String[]{};
		assertObject(toReverseArray(String.class, Arrays.asList(s))).json().is("[]");

		s = new String[]{"a"};
		assertObject(toReverseArray(String.class, Arrays.asList(s))).json().is("['a']");

		s = new String[]{"a","b"};
		assertObject(toReverseArray(String.class, Arrays.asList(s))).json().is("['b','a']");

		s = new String[]{"a","b","c"};
		assertObject(toReverseArray(String.class, Arrays.asList(s))).json().is("['c','b','a']");
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test
	public void testAsSet() throws Exception {
		String[] s = null;

		assertThrown(()->{asSet((String[])null);}).isType(IllegalArgumentException.class);

		s = new String[]{"a"};
		Iterator<String> i = asSet(s).iterator();
		assertEquals("a", i.next());

		assertThrown(()->{i.remove();}).isType(UnsupportedOperationException.class);
		assertThrown(()->{i.next();}).isType(NoSuchElementException.class);
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

		assertThrown(()->{i.remove();}).isType(UnsupportedOperationException.class);
		assertThrown(()->{i.next();}).isType(NoSuchElementException.class);
	}

	//====================================================================================================
	// combine(T[]...)
	//====================================================================================================
	@Test
	public void testCombine() throws Exception {
		String[] s1 = new String[]{"a"}, s2 = new String[]{"b"};

		assertObject(combine(s1, s2)).json().is("['a','b']");
		assertObject(combine(s1)).json().is("['a']");
		assertObject(combine(s2)).json().is("['b']");
		assertObject(combine(s1,null)).json().is("['a']");
		assertObject(combine(null,s2)).json().is("['b']");
		assertNull(combine(null,null));
		assertNull(combine());
	}
}
