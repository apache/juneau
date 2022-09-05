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
package org.apache.juneau.collections;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ControlledArrayList_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// test - Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_constructors() throws Exception {
		ControlledArrayList<Integer> x;

		x = new ControlledArrayList<>(false);
		assertTrue(x.isModifiable());

		x = new ControlledArrayList<>(true);
		assertFalse(x.isModifiable());

		x = new ControlledArrayList<>(false, Arrays.asList(1));
		assertTrue(x.isModifiable());

		x = new ControlledArrayList<>(true, Arrays.asList(1));
		assertFalse(x.isModifiable());
	}

	@Test
	public void a02_basicMethods() throws Exception {
		ControlledArrayList<Integer> x1 = new ControlledArrayList<Integer>(false, Arrays.asList(1));
		ControlledArrayList<Integer> x2 = new ControlledArrayList<Integer>(true, Arrays.asList(1));

		x1.set(0, 2);
		assertThrown(() -> x2.set(0, 2)).isType(UnsupportedOperationException.class);
		x2.overrideSet(0, 2);
		assertList(x1).is(x2);

		x1.add(0, 2);
		assertThrown(() -> x2.add(0, 2)).isType(UnsupportedOperationException.class);
		x2.overrideAdd(0, 2);
		assertList(x1).is(x2);

		x1.remove(0);
		assertThrown(() -> x2.remove(0)).isType(UnsupportedOperationException.class);
		x2.overrideRemove(0);
		assertList(x1).is(x2);

		x1.addAll(0, Arrays.asList(3));
		assertThrown(() -> x2.addAll(0, Arrays.asList(3))).isType(UnsupportedOperationException.class);
		x2.overrideAddAll(0, Arrays.asList(3));
		assertList(x1).is(x2);

		x1.replaceAll(x -> x);
		assertThrown(() -> x2.replaceAll(x -> x)).isType(UnsupportedOperationException.class);
		x2.overrideReplaceAll(x -> x);
		assertList(x1).is(x2);

		x1.sort(null);
		assertThrown(() -> x2.sort(null)).isType(UnsupportedOperationException.class);
		x2.overrideSort(null);
		assertList(x1).is(x2);

		x1.add(1);
		assertThrown(() -> x2.add(1)).isType(UnsupportedOperationException.class);
		x2.overrideAdd(1);
		assertList(x1).is(x2);

		x1.remove((Integer)1);
		assertThrown(() -> x2.remove((Integer)1)).isType(UnsupportedOperationException.class);
		x2.overrideRemove((Integer)1);
		assertList(x1).is(x2);

		x1.addAll(Arrays.asList(3));
		assertThrown(() -> x2.addAll(Arrays.asList(3))).isType(UnsupportedOperationException.class);
		x2.overrideAddAll(Arrays.asList(3));
		assertList(x1).is(x2);

		x1.removeAll(Arrays.asList(3));
		assertThrown(() -> x2.removeAll(Arrays.asList(3))).isType(UnsupportedOperationException.class);
		x2.overrideRemoveAll(Arrays.asList(3));
		assertList(x1).is(x2);

		x1.retainAll(Arrays.asList(2));
		assertThrown(() -> x2.retainAll(Arrays.asList(2))).isType(UnsupportedOperationException.class);
		x2.overrideRetainAll(Arrays.asList(2));
		assertList(x1).is(x2);

		x1.clear();
		assertThrown(() -> x2.clear()).isType(UnsupportedOperationException.class);
		x2.overrideClear();
		assertList(x1).is(x2);

		x1.add(1);
		x2.overrideAdd(1);

		x1.removeIf(x -> x == 1);
		assertThrown(() -> x2.removeIf(x -> x == 1)).isType(UnsupportedOperationException.class);
		x2.overrideRemoveIf(x -> x == 1);
		assertList(x1).is(x2);

		x1.add(1);
		x2.overrideAdd(1);

		ControlledArrayList<Integer> x1a = (ControlledArrayList<Integer>) x1.subList(0, 0);
		ControlledArrayList<Integer> x2a = (ControlledArrayList<Integer>) x2.subList(0, 0);
		assertTrue(x1a.isModifiable());
		assertFalse(x2a.isModifiable());
	}

	@Test
	public void a03_iterator() throws Exception {
		ControlledArrayList<Integer> x1 = new ControlledArrayList<Integer>(false, Arrays.asList(1));
		ControlledArrayList<Integer> x2 = new ControlledArrayList<Integer>(true, Arrays.asList(1));

		Iterator<Integer> i1 = x1.iterator();
		Iterator<Integer> i2 = x2.iterator();

		assertTrue(i1.hasNext());
		assertTrue(i2.hasNext());

		assertEquals(1, i1.next().intValue());
		assertEquals(1, i2.next().intValue());

		i1.remove();
		assertThrown(() -> i2.remove()).isType(UnsupportedOperationException.class);

		i1.forEachRemaining(x -> {});
		i2.forEachRemaining(x -> {});
	}

	@Test
	public void a04_listIterator() throws Exception {
		ControlledArrayList<Integer> x1 = new ControlledArrayList<Integer>(false, Arrays.asList(1));
		ControlledArrayList<Integer> x2 = new ControlledArrayList<Integer>(true, Arrays.asList(1));

		ListIterator<Integer> i1a = x1.listIterator();
		ListIterator<Integer> i2a = x2.listIterator();

		assertTrue(i1a.hasNext());
		assertTrue(i2a.hasNext());

		assertEquals(1, i1a.next().intValue());
		assertEquals(1, i2a.next().intValue());

		assertTrue(i1a.hasPrevious());
		assertTrue(i2a.hasPrevious());

		assertEquals(1, i1a.nextIndex());
		assertEquals(1, i2a.nextIndex());

		assertEquals(0, i1a.previousIndex());
		assertEquals(0, i2a.previousIndex());

		i1a.previous();
		i2a.previous();

		i1a.set(1);
		assertThrown(() -> i2a.set(1)).isType(UnsupportedOperationException.class);

		i1a.add(1);
		assertThrown(() -> i2a.add(1)).isType(UnsupportedOperationException.class);

		i1a.next();
		i2a.next();

		i1a.remove();
		assertThrown(() -> i2a.remove()).isType(UnsupportedOperationException.class);

		i1a.forEachRemaining(x -> {});
		i2a.forEachRemaining(x -> {});
	}
}
