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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ArrayUtilsTest {

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test
	public void testAppendArrayToArray() throws Exception {
		String[] s = {};

		s = append(s, "a", "b");
		assertObject(s).asJson().is("['a','b']");

		s = append(s, "c");
		assertObject(s).asJson().is("['a','b','c']");

		s = append(s);
		assertObject(s).asJson().is("['a','b','c']");

		Object[] o = append((Object[])null);
		assertObject(o).asJson().is("[]");

		s = append((String[])null, "a", "b");
		assertObject(s).asJson().is("['a','b']");
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test
	public void testAsSet() throws Exception {
		String[] s = null;

		assertThrown(()->asSet((String[])null)).isType(IllegalArgumentException.class);

		s = new String[]{"a"};
		Iterator<String> i = asSet(s).iterator();
		assertEquals("a", i.next());

		assertThrown(()->i.remove()).isType(UnsupportedOperationException.class);
		assertThrown(()->i.next()).isType(NoSuchElementException.class);
	}

	//====================================================================================================
	// combine(T[]...)
	//====================================================================================================
	@Test
	public void testCombine() throws Exception {
		String[] s1 = {"a"}, s2 = {"b"};

		assertObject(combine(s1, s2)).asJson().is("['a','b']");
		assertObject(combine(s1)).asJson().is("['a']");
		assertObject(combine(s2)).asJson().is("['b']");
		assertObject(combine(s1,null)).asJson().is("['a']");
		assertObject(combine(null,s2)).asJson().is("['b']");
		assertNull(combine(null,null));
		assertNull(combine());
	}
}