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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.internal.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class MultiSetTest {

	@Test
	public void doTest() throws Exception {
		List<String> l1, l2;
		MultiSet<String> ms;

		l1 = Arrays.asList(new String[]{"1","2"});
		l2 = Arrays.asList(new String[]{"3","4"});
		ms = new MultiSet<>(l1, l2);
		Iterator<String> i1 = ms.iterator();
		assertTrue(i1.hasNext());
		assertEquals("1", i1.next());
		assertTrue(i1.hasNext());
		assertEquals("2", i1.next());
		assertTrue(i1.hasNext());
		assertEquals("3", i1.next());
		assertTrue(i1.hasNext());
		assertEquals("4", i1.next());
		assertFalse(i1.hasNext());
		assertThrown(()->i1.next()).isType(NoSuchElementException.class);

		l1 = Arrays.asList(new String[]{"1","2"});
		l2 = Arrays.asList(new String[]{});
		ms = new MultiSet<>(l1, l2);
		Iterator<String> i2 = ms.iterator();
		assertTrue(i2.hasNext());
		assertEquals("1", i2.next());
		assertTrue(i2.hasNext());
		assertEquals("2", i2.next());
		assertFalse(i2.hasNext());
		assertThrown(()->i2.next()).isType(NoSuchElementException.class);

		l1 = Arrays.asList(new String[]{});
		l2 = Arrays.asList(new String[]{"3","4"});
		ms = new MultiSet<>(l1, l2);
		Iterator<String> i3 = ms.iterator();
		assertTrue(i3.hasNext());
		assertEquals("3", i3.next());
		assertTrue(i3.hasNext());
		assertEquals("4", i3.next());
		assertFalse(i3.hasNext());
		assertThrown(()->i3.next()).isType(NoSuchElementException.class);

		l1 = Arrays.asList(new String[]{});
		l2 = Arrays.asList(new String[]{});
		ms = new MultiSet<>(l1, l2);
		Iterator<String> i4 = ms.iterator();
		assertFalse(i4.hasNext());
		assertThrown(()->i4.next()).isType(NoSuchElementException.class);

		l1 = Arrays.asList(new String[]{"1","2"});
		ms = new MultiSet<>(l1);
		Iterator<String> i5 = ms.iterator();
		assertTrue(i5.hasNext());
		assertEquals("1", i5.next());
		assertTrue(i5.hasNext());
		assertEquals("2", i5.next());
		assertFalse(i5.hasNext());
		assertThrown(()->i5.next()).isType(NoSuchElementException.class);

		l1 = new LinkedList<>(Arrays.asList(new String[]{"1","2"}));
		l2 = new LinkedList<>(Arrays.asList(new String[]{"3","4"}));
		ms = new MultiSet<>(l1).append(l2);
		assertObject(ms).json().is("['1','2','3','4']");
		assertObject(ms.enumerator()).json().is("['1','2','3','4']");
		assertEquals(4, ms.size());

		Iterator<String> t = ms.iterator();
		t.next();
		t.remove();
		assertObject(ms.enumerator()).json().is("['2','3','4']");

		t = ms.iterator();
		t.next();
		t.remove();
		assertObject(ms.enumerator()).json().is("['3','4']");

		t = ms.iterator();
		t.next();
		t.remove();
		assertObject(ms.enumerator()).json().is("['4']");

		t = ms.iterator();
		t.next();
		t.remove();
		assertObject(ms.enumerator()).json().is("[]");
		assertEquals(0, ms.size());

		ms = new MultiSet<>();
		assertObject(ms).json().is("[]");
		assertEquals(0, ms.size());

		assertThrown(()->new MultiSet<>((Collection<String>)null)).isType(IllegalArgumentException.class);
		assertThrown(()->new MultiSet<String>().iterator().next()).isType(NoSuchElementException.class);
		assertThrown(()->new MultiSet<String>().iterator().remove()).isType(NoSuchElementException.class);

	}
}
