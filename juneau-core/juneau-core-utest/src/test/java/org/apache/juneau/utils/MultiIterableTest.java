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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MultiIterableTest {

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test() throws Exception {
		List
			l1 = new OList(),
			l2 = new OList("['a','b']"),
			l3 = new OList("['c']");

		MultiIterable mi = new MultiIterable(l1.iterator(),l2.iterator());
		mi.append(l3.iterator());

		assertObject(mi.iterator()).json().is("['a','b','c']");

		mi = new MultiIterable(l1.iterator());
		assertObject(mi.iterator()).json().is("[]");

		mi = new MultiIterable(l2.iterator(), l1.iterator());
		assertObject(mi.iterator()).json().is("['a','b']");

		mi = new MultiIterable(l2.iterator(), l1.iterator(), l3.iterator());
		assertObject(mi.iterator()).json().is("['a','b','c']");

		mi = new MultiIterable();
		assertObject(mi.iterator()).json().is("[]");

		assertThrown(()->{return new MultiIterable().append(null);}).isType(IllegalArgumentException.class);

		mi = new MultiIterable(l1.iterator());
		assertThrown(()->{return new MultiIterable(l1.iterator()).iterator().next();}).isType(NoSuchElementException.class);

		mi = new MultiIterable(l1.iterator());
		final Iterator i = mi.iterator();
		assertFalse(i.hasNext());
		assertThrown(()->{i.remove(); return null;}).isType(NoSuchElementException.class);

		mi = new MultiIterable(l2.iterator());
		Iterator i2 = mi.iterator();
		assertTrue(i2.hasNext());
		assertEquals("a", i2.next());
		i2.remove();
	}
}
