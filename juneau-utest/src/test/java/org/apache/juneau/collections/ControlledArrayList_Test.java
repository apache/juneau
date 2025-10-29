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
package org.apache.juneau.collections;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ControlledArrayList_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// test - Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_constructors() {
		var x = new ControlledArrayList<>(false);
		assertTrue(x.isModifiable());

		x = new ControlledArrayList<>(true);
		assertFalse(x.isModifiable());

		x = new ControlledArrayList<>(false, l(1));
		assertTrue(x.isModifiable());

		x = new ControlledArrayList<>(true, l(1));
		assertFalse(x.isModifiable());
	}

	@Test void a02_basicMethods() {
		var x1 = new ControlledArrayList<>(false, l(1));
		var x2 = new ControlledArrayList<>(true, l(1));

		x1.set(0, 2);
		assertThrows(UnsupportedOperationException.class, () -> x2.set(0, 2));
		x2.overrideSet(0, 2);
		assertEquals(x2, x1);

		x1.add(0, 2);
		assertThrows(UnsupportedOperationException.class, () -> x2.add(0, 2));
		x2.overrideAdd(0, 2);
		assertEquals(x2, x1);

		x1.remove(0);
		assertThrows(UnsupportedOperationException.class, () -> x2.remove(0));
		x2.overrideRemove(0);
		assertEquals(x2, x1);

		x1.addAll(0, l(3));
		assertThrows(UnsupportedOperationException.class, () -> x2.addAll(0, l(3)));
		x2.overrideAddAll(0, l(3));
		assertEquals(x2, x1);

		x1.replaceAll(x -> x);
		assertThrows(UnsupportedOperationException.class, () -> x2.replaceAll(x -> x));
		x2.overrideReplaceAll(x -> x);
		assertEquals(x2, x1);

		x1.sort(null);
		assertThrows(UnsupportedOperationException.class, () -> x2.sort(null));
		x2.overrideSort(null);
		assertEquals(x2, x1);

		x1.add(1);
		assertThrows(UnsupportedOperationException.class, () -> x2.add(1));
		x2.overrideAdd(1);
		assertEquals(x2, x1);

		x1.remove((Integer)1);
		assertThrows(UnsupportedOperationException.class, () -> x2.remove((Integer)1));
		x2.overrideRemove((Integer)1);
		assertEquals(x2, x1);

		x1.addAll(l(3));
		assertThrows(UnsupportedOperationException.class, () -> x2.addAll(l(3)));
		x2.overrideAddAll(l(3));
		assertEquals(x2, x1);

		x1.removeAll(l(3));
		assertThrows(UnsupportedOperationException.class, () -> x2.removeAll(l(3)));
		x2.overrideRemoveAll(l(3));
		assertEquals(x2, x1);

		x1.retainAll(l(2));
		assertThrows(UnsupportedOperationException.class, () -> x2.retainAll(l(2)));
		x2.overrideRetainAll(l(2));
		assertEquals(x2, x1);

		x1.clear();
		assertThrows(UnsupportedOperationException.class, x2::clear);
		x2.overrideClear();
		assertEquals(x2, x1);

		x1.add(1);
		x2.overrideAdd(1);

		x1.removeIf(x -> x == 1);
		assertThrows(UnsupportedOperationException.class, () -> x2.removeIf(x -> x == 1));
		x2.overrideRemoveIf(x -> x == 1);
		assertEquals(x2, x1);

		x1.add(1);
		x2.overrideAdd(1);

		var x1a = (ControlledArrayList<Integer>) x1.subList(0, 0);
		var x2a = (ControlledArrayList<Integer>) x2.subList(0, 0);
		assertTrue(x1a.isModifiable());
		assertFalse(x2a.isModifiable());
	}

	@Test void a03_iterator() {
		var x1 = new ControlledArrayList<>(false, l(1));
		var x2 = new ControlledArrayList<>(true, l(1));

		var i1 = x1.iterator();
		var i2 = x2.iterator();

		assertTrue(i1.hasNext());
		assertTrue(i2.hasNext());

		assertEquals(1, i1.next().intValue());
		assertEquals(1, i2.next().intValue());

		i1.remove();
		assertThrows(UnsupportedOperationException.class, i2::remove);

		i1.forEachRemaining(x -> {});
		i2.forEachRemaining(x -> {});
	}

	@Test void a04_listIterator() {
		var x1 = new ControlledArrayList<>(false, l(1));
		var x2 = new ControlledArrayList<>(true, l(1));

		var i1a = x1.listIterator();
		var i2a = x2.listIterator();

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
		assertThrows(UnsupportedOperationException.class, () -> i2a.set(1));

		i1a.add(1);
		assertThrows(UnsupportedOperationException.class, () -> i2a.add(1));

		i1a.next();
		i2a.next();

		i1a.remove();
		assertThrows(UnsupportedOperationException.class, i2a::remove);

		i1a.forEachRemaining(x -> {});
		i2a.forEachRemaining(x -> {});
	}
}