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

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class MultiIterableTest {

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test() throws Exception {
		List
			l1 = new ObjectList(),
			l2 = new ObjectList("['a','b']"),
			l3 = new ObjectList("['c']");

		MultiIterable mi = new MultiIterable(l1.iterator(),l2.iterator());
		mi.append(l3.iterator());

		assertObjectEquals("['a','b','c']", mi.iterator());

		mi = new MultiIterable(l1.iterator());
		assertObjectEquals("[]", mi.iterator());

		mi = new MultiIterable(l2.iterator(), l1.iterator());
		assertObjectEquals("['a','b']", mi.iterator());

		mi = new MultiIterable(l2.iterator(), l1.iterator(), l3.iterator());
		assertObjectEquals("['a','b','c']", mi.iterator());

		mi = new MultiIterable();
		assertObjectEquals("[]", mi.iterator());

		try { mi.append(null); fail(); } catch (IllegalArgumentException e) {}

		mi = new MultiIterable(l1.iterator());
		try { mi.iterator().next(); fail(); } catch (NoSuchElementException e) {}

		mi = new MultiIterable(l1.iterator());
		Iterator i = mi.iterator();
		assertFalse(i.hasNext());
		try { i.remove(); fail(); } catch (NoSuchElementException e) {}

		mi = new MultiIterable(l2.iterator());
		i = mi.iterator();
		assertTrue(i.hasNext());
		assertEquals("a", i.next());
		i.remove();
	}
}
