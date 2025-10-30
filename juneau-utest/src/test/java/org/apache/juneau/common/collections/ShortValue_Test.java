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
package org.apache.juneau.common.collections;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ShortValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		ShortValue v = ShortValue.create();
		assertEquals((short)0, v.get());
	}

	@Test
	void a02_of() {
		ShortValue v = ShortValue.of((short)42);
		assertEquals((short)42, v.get());
	}

	@Test
	void a03_constructor() {
		ShortValue v = new ShortValue((short)100);
		assertEquals((short)100, v.get());
	}

	@Test
	void a04_constructor_withNull() {
		ShortValue v = new ShortValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAndIncrement()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getAndIncrement_basic() {
		ShortValue v = ShortValue.of((short)5);
		assertEquals((short)5, v.getAndIncrement());
		assertEquals((short)6, v.get());
	}

	@Test
	void b02_getAndIncrement_fromZero() {
		ShortValue v = ShortValue.create();
		assertEquals((short)0, v.getAndIncrement());
		assertEquals((short)1, v.get());
	}

	@Test
	void b03_getAndIncrement_multiple() {
		ShortValue v = ShortValue.of((short)1);
		assertEquals((short)1, v.getAndIncrement());
		assertEquals((short)2, v.getAndIncrement());
		assertEquals((short)3, v.getAndIncrement());
		assertEquals((short)4, v.get());
	}

	@Test
	void b04_getAndIncrement_withNull() {
		ShortValue v = new ShortValue(null);
		assertEquals((short)0, v.getAndIncrement());
		assertEquals((short)1, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Short> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_set() {
		ShortValue v = ShortValue.create();
		v.set((short)50);
		assertEquals((short)50, v.get());
	}

	@Test
	void c02_setIfEmpty() {
		ShortValue v = new ShortValue(null);
		v.setIfEmpty((short)10);
		assertEquals((short)10, v.get());

		v.setIfEmpty((short)20);
		assertEquals((short)10, v.get()); // Should not change
	}

	@Test
	void c03_orElse() {
		ShortValue v = new ShortValue(null);
		assertEquals((short)99, v.orElse((short)99));

		v.set((short)42);
		assertEquals((short)42, v.orElse((short)99));
	}

	@Test
	void c04_map() {
		ShortValue v = ShortValue.of((short)5);
		Value<Short> v2 = v.map(x -> (short)(x * 2));
		assertEquals((short)10, v2.get());
	}

	@Test
	void c05_ifPresent() {
		ShortValue v = ShortValue.of((short)7);
		StringBuilder sb = new StringBuilder();
		v.ifPresent(x -> sb.append(x));
		assertEquals("7", sb.toString());
	}

	@Test
	void c06_isPresent() {
		ShortValue v = new ShortValue(null);
		assertFalse(v.isPresent());

		v.set((short)1);
		assertTrue(v.isPresent());
	}

	@Test
	void c07_isEmpty() {
		ShortValue v = new ShortValue(null);
		assertEmpty(v);

		v.set((short)1);
		assertNotEmpty(v);
	}

	@Test
	void c08_getAndSet() {
		ShortValue v = ShortValue.of((short)10);
		assertEquals((short)10, v.getAndSet((short)20));
		assertEquals((short)20, v.get());
	}

	@Test
	void c09_getAndUnset() {
		ShortValue v = ShortValue.of((short)15);
		assertEquals((short)15, v.getAndUnset());
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_countingInLambda() {
		ShortValue counter = ShortValue.create();

		l(1, 2, 3, 4, 5).forEach(x -> {
			if (x % 2 == 0) {
				counter.getAndIncrement();
			}
		});

		assertEquals((short)2, counter.get());
	}

	@Test
	void d02_trackingMaxValue() {
		ShortValue max = ShortValue.of((short)0);

		l((short)5, (short)12, (short)3, (short)8, (short)20, (short)1).forEach(x -> {
			if (x > max.get()) {
				max.set(x);
			}
		});

		assertEquals((short)20, max.get());
	}

	@Test
	void d03_accumulatingValues() {
		ShortValue sum = ShortValue.create();

		l((short)1, (short)2, (short)3, (short)4, (short)5).forEach(x -> {
			sum.set((short)(sum.get() + x));
		});

		assertEquals((short)15, sum.get());
	}
}

