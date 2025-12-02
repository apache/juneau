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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
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
		var v = ShortValue.create();
		assertEquals((short)0, v.get());
	}

	@Test
	void a02_of() {
		var v = ShortValue.of((short)42);
		assertEquals((short)42, v.get());
	}

	@Test
	void a03_constructor() {
		var v = new ShortValue((short)100);
		assertEquals((short)100, v.get());
	}

	@Test
	void a04_constructor_withNull() {
		var v = new ShortValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAndIncrement()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getAndIncrement_basic() {
		var v = ShortValue.of((short)5);
		assertEquals((short)5, v.getAndIncrement());
		assertEquals((short)6, v.get());
	}

	@Test
	void b02_getAndIncrement_fromZero() {
		var v = ShortValue.create();
		assertEquals((short)0, v.getAndIncrement());
		assertEquals((short)1, v.get());
	}

	@Test
	void b03_getAndIncrement_multiple() {
		var v = ShortValue.of((short)1);
		assertEquals((short)1, v.getAndIncrement());
		assertEquals((short)2, v.getAndIncrement());
		assertEquals((short)3, v.getAndIncrement());
		assertEquals((short)4, v.get());
	}

	@Test
	void b04_getAndIncrement_withNull() {
		var v = new ShortValue(null);
		assertEquals((short)0, v.getAndIncrement());
		assertEquals((short)1, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Short> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_set() {
		var v = ShortValue.create();
		v.set((short)50);
		assertEquals((short)50, v.get());
	}

	@Test
	void c02_setIfEmpty() {
		var v = new ShortValue(null);
		v.setIfEmpty((short)10);
		assertEquals((short)10, v.get());

		v.setIfEmpty((short)20);
		assertEquals((short)10, v.get()); // Should not change
	}

	@Test
	void c03_orElse() {
		var v = new ShortValue(null);
		assertEquals((short)99, v.orElse((short)99));

		v.set((short)42);
		assertEquals((short)42, v.orElse((short)99));
	}

	@Test
	void c04_map() {
		var v = ShortValue.of((short)5);
		Value<Short> v2 = v.map(x -> (short)(x * 2));
		assertEquals((short)10, v2.get());
	}

	@Test
	void c05_ifPresent() {
		var v = ShortValue.of((short)7);
		var sb = new StringBuilder();
		v.ifPresent(x -> sb.append(x));
		assertEquals("7", sb.toString());
	}

	@Test
	void c06_isPresent() {
		var v = new ShortValue(null);
		assertFalse(v.isPresent());

		v.set((short)1);
		assertTrue(v.isPresent());
	}

	@Test
	void c07_isEmpty() {
		var v = new ShortValue(null);
		assertEmpty(v);

		v.set((short)1);
		assertNotEmpty(v);
	}

	@Test
	void c08_getAndSet() {
		var v = ShortValue.of((short)10);
		assertEquals((short)10, v.getAndSet((short)20));
		assertEquals((short)20, v.get());
	}

	@Test
	void c09_getAndUnset() {
		var v = ShortValue.of((short)15);
		assertEquals((short)15, v.getAndUnset());
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_countingInLambda() {
		var counter = ShortValue.create();

		l(1, 2, 3, 4, 5).forEach(x -> {
			if (x % 2 == 0) {
				counter.getAndIncrement();
			}
		});

		assertEquals((short)2, counter.get());
	}

	@Test
	void d02_trackingMaxValue() {
		var max = ShortValue.of((short)0);

		l((short)5, (short)12, (short)3, (short)8, (short)20, (short)1).forEach(x -> {
			if (x > max.get()) {
				max.set(x);
			}
		});

		assertEquals((short)20, max.get());
	}

	@Test
	void d03_accumulatingValues() {
		var sum = ShortValue.create();

		l((short)1, (short)2, (short)3, (short)4, (short)5).forEach(x -> {
			sum.set((short)(sum.get() + x));
		});

		assertEquals((short)15, sum.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Increment/Decrement operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_increment() {
		var v = ShortValue.of((short)5);
		v.increment();
		assertEquals((short)6, v.get());
	}

	@Test
	void e02_increment_fromNull() {
		var v = new ShortValue(null);
		v.increment();
		assertEquals((short)1, v.get());  // null treated as 0, so 0+1 = 1
	}

	@Test
	void e03_decrement() {
		var v = ShortValue.of((short)5);
		v.decrement();
		assertEquals((short)4, v.get());
	}

	@Test
	void e04_decrement_fromNull() {
		var v = new ShortValue(null);
		v.decrement();
		assertEquals((short)-1, v.get());  // null treated as 0, so 0-1 = -1
	}

	@Test
	void e05_incrementAndGet() {
		var v = ShortValue.of((short)5);
		var result = v.incrementAndGet();
		assertEquals((short)6, result);
		assertEquals((short)6, v.get());
	}

	@Test
	void e06_incrementAndGet_fromNull() {
		var v = new ShortValue(null);
		var result = v.incrementAndGet();
		assertEquals((short)1, result);
		assertEquals((short)1, v.get());
	}

	@Test
	void e07_decrementAndGet() {
		var v = ShortValue.of((short)5);
		var result = v.decrementAndGet();
		assertEquals((short)4, result);
		assertEquals((short)4, v.get());
	}

	@Test
	void e08_decrementAndGet_fromNull() {
		var v = new ShortValue(null);
		var result = v.decrementAndGet();
		assertEquals((short)-1, result);
		assertEquals((short)-1, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_add() {
		var v = ShortValue.of((short)10);
		v.add((short)5);
		assertEquals((short)15, v.get());
	}

	@Test
	void f02_add_withNullValue() {
		var v = ShortValue.of((short)10);
		v.add(null);
		assertEquals((short)10, v.get());  // null treated as 0, so 10+0 = 10
	}

	@Test
	void f03_add_toNullValue() {
		var v = new ShortValue(null);
		v.add((short)5);
		assertEquals((short)5, v.get());  // null treated as 0, so 0+5 = 5
	}

	@Test
	void f04_add_bothNull() {
		var v = new ShortValue(null);
		v.add(null);
		assertEquals((short)0, v.get());  // null+null = 0+0 = 0
	}

	@Test
	void f05_addAndGet() {
		var v = ShortValue.of((short)10);
		var result = v.addAndGet((short)5);
		assertEquals((short)15, result);
		assertEquals((short)15, v.get());
	}

	@Test
	void f06_addAndGet_withNullValue() {
		var v = ShortValue.of((short)10);
		var result = v.addAndGet(null);
		assertEquals((short)10, result);
		assertEquals((short)10, v.get());
	}

	@Test
	void f07_addAndGet_toNullValue() {
		var v = new ShortValue(null);
		var result = v.addAndGet((short)5);
		assertEquals((short)5, result);
		assertEquals((short)5, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Comparison operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_is() {
		var v = ShortValue.of((short)42);
		assertTrue(v.is((short)42));
		assertFalse(v.is((short)43));
		assertFalse(v.is(null));
	}

	@Test
	void g02_is_withNullValue() {
		var v = new ShortValue(null);
		assertFalse(v.is((short)42));
		assertTrue(v.is(null));
	}

	@Test
	void g03_isAny() {
		var v = ShortValue.of((short)5);
		assertTrue(v.isAny((short)3, (short)5, (short)7));
		assertTrue(v.isAny((short)5));
		assertFalse(v.isAny((short)1, (short)2));
		assertFalse(v.isAny());
	}

	@Test
	void g04_isAny_nullValue() {
		var v = new ShortValue(null);
		assertFalse(v.isAny((short)1, (short)2, (short)3));
		assertTrue(v.isAny((Short)null));
		assertTrue(v.isAny((short)1, null, (short)2));
	}
}

