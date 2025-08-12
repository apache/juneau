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
import static org.junit.Assert.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ArrayUtilsTest extends SimpleTestBase {

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test void testAppendArrayToArray() {
		String[] s = {};

		s = append(s, "a", "b");
		assertArray(s, "a,b");

		s = append(s, "c");
		assertArray(s, "a,b,c");

		s = append(s);
		assertArray(s, "a,b,c");

		Object[] o = append((Object[])null);
		assertArray(o);

		s = append((String[])null, "a", "b");
		assertArray(s, "a,b");
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test void testAsSet() {
		String[] s = null;

		assertThrows(IllegalArgumentException.class, ()->asSet((String[])null));

		s = new String[]{"a"};
		Iterator<String> i = asSet(s).iterator();
		assertEquals("a", i.next());

		assertThrows(UnsupportedOperationException.class, i::remove);
		assertThrows(NoSuchElementException.class, i::next);
	}

	//====================================================================================================
	// combine(T[]...)
	//====================================================================================================
	@Test void testCombine() {
		String[] s1 = {"a"}, s2 = {"b"};

		assertArray(combine(s1, s2), "a,b");
		assertArray(combine(s1), "a");
		assertArray(combine(s2), "b");
		assertArray(combine(s1,null), "a");
		assertArray(combine(null,s2), "b");
		assertNull(combine(null,null));
		assertNull(combine());
	}
}