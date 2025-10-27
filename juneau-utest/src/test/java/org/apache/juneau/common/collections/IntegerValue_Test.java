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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class IntegerValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var a = IntegerValue.create();
		assertEquals(0, a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a02_of() {
		var a = IntegerValue.of(42);
		assertEquals(42, a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a03_of_null() {
		var a = IntegerValue.of(null);
		assertEquals(0, a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a04_constructor_default() {
		var a = new IntegerValue();
		assertEquals(0, a.get());
	}

	@Test
	void a05_constructor_withValue() {
		var a = new IntegerValue(100);
		assertEquals(100, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAndIncrement() tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getAndIncrement_basic() {
		var a = IntegerValue.of(5);
		assertEquals(5, a.getAndIncrement());
		assertEquals(6, a.get());
	}

	@Test
	void b02_getAndIncrement_fromZero() {
		var a = IntegerValue.create();
		assertEquals(0, a.getAndIncrement());
		assertEquals(1, a.get());
	}

	@Test
	void b03_getAndIncrement_multiple() {
		var a = IntegerValue.of(10);
		assertEquals(10, a.getAndIncrement());
		assertEquals(11, a.getAndIncrement());
		assertEquals(12, a.getAndIncrement());
		assertEquals(13, a.get());
	}

	@Test
	void b04_getAndIncrement_withNull() {
		var a = new IntegerValue();
		a.set(null);
		assertEquals(0, a.getAndIncrement());
		assertEquals(1, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// increment/decrement tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b05_increment() {
		var a = IntegerValue.of(5);
		a.increment();
		assertEquals(6, a.get());
	}

	@Test
	void b06_decrement() {
		var a = IntegerValue.of(5);
		a.decrement();
		assertEquals(4, a.get());
	}

	@Test
	void b07_incrementAndGet() {
		var a = IntegerValue.of(5);
		assertEquals(6, a.incrementAndGet());
		assertEquals(6, a.get());
	}

	@Test
	void b08_decrementAndGet() {
		var a = IntegerValue.of(5);
		assertEquals(4, a.decrementAndGet());
		assertEquals(4, a.get());
	}

	@Test
	void b09_add() {
		var a = IntegerValue.of(10);
		a.add(5);
		assertEquals(15, a.get());
	}

	@Test
	void b10_addAndGet() {
		var a = IntegerValue.of(10);
		assertEquals(15, a.addAndGet(5));
		assertEquals(15, a.get());
	}

	@Test
	void b11_is() {
		var a = IntegerValue.of(42);
		assertTrue(a.is(42));
		assertFalse(a.is(43));
	}

	@Test
	void b12_isAny() {
		var a = IntegerValue.of(5);
		assertTrue(a.isAny(3, 5, 7));
		assertFalse(a.isAny(1, 2));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Integer> functionality tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_set() {
		var a = IntegerValue.create();
		a.set(99);
		assertEquals(99, a.get());
	}

	@Test
	void c02_setIfEmpty() {
		var a = IntegerValue.of(5);
		a.setIfEmpty(10);
		assertEquals(5, a.get());  // Should not change

		a.set(null);
		a.setIfEmpty(10);
		assertEquals(10, a.get());  // Should change
	}

	@Test
	void c03_getAndUnset() {
		var a = IntegerValue.of(42);
		var b = a.getAndUnset();
		assertEquals(42, b);
		assertNull(a.get());
		assertFalse(a.isPresent());
	}

	@Test
	void c04_isPresent_isEmpty() {
		var a = IntegerValue.of(5);
		assertTrue(a.isPresent());
		assertFalse(a.isEmpty());

		a.set(null);
		assertFalse(a.isPresent());
		assertTrue(a.isEmpty());
	}

	@Test
	void c05_orElse() {
		var a = IntegerValue.of(10);
		assertEquals(10, a.orElse(999));

		a.set(null);
		assertEquals(999, a.orElse(999));
	}

	@Test
	void c06_orElseGet() {
		var a = IntegerValue.of(10);
		assertEquals(10, a.orElseGet(() -> 999));

		a.set(null);
		assertEquals(999, a.orElseGet(() -> 999));
	}

	@Test
	void c07_orElseThrow() {
		var a = IntegerValue.of(10);
		assertEquals(10, a.orElseThrow(() -> new RuntimeException("error")));

		a.set(null);
		assertThrows(RuntimeException.class, () -> a.orElseThrow(() -> new RuntimeException("error")));
	}

	@Test
	void c08_ifPresent() {
		var a = IntegerValue.of(10);
		var b = Flag.create();

		a.ifPresent(x -> b.set());
		assertTrue(b.isSet());

		var c = Flag.create();
		a.set(null);
		a.ifPresent(x -> c.set());
		assertFalse(c.isSet());
	}

	@Test
	void c09_map() {
		var a = IntegerValue.of(10);
		var b = a.map(x -> x * 2);
		assertEquals(20, b.get());

		a.set(null);
		var c = a.map(x -> x * 2);
		assertNull(c.get());
		assertFalse(c.isPresent());
	}

	@Test
	void c10_setIf() {
		var a = IntegerValue.of(10);
		a.setIf(true, 20);
		assertEquals(20, a.get());

		a.setIf(false, 30);
		assertEquals(20, a.get());  // Should not change
	}

	@Test
	void c11_update() {
		var a = IntegerValue.of(10);
		a.update(x -> x * 2);
		assertEquals(20, a.get());

		a.set(null);
		a.update(x -> x * 2);  // Should be no-op
		assertNull(a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_counterInLambda() {
		var a = IntegerValue.create();

		var list = list("a", "b", "c", "d", "e");
		list.forEach(x -> a.getAndIncrement());

		assertEquals(5, a.get());
	}

	@Test
	void d02_conditionalCounting() {
		var a = IntegerValue.create();

		list(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).forEach(x -> {
			if (x % 2 == 0) {
				a.getAndIncrement();
			}
		});

		assertEquals(5, a.get());  // 5 even numbers
	}

	@Test
	void d03_multipleCounters() {
		var a = IntegerValue.create();
		var b = IntegerValue.create();

		list(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).forEach(x -> {
			if (x % 2 == 0) {
				a.getAndIncrement();
			} else {
				b.getAndIncrement();
			}
		});

		assertEquals(5, a.get());  // even count
		assertEquals(5, b.get());  // odd count
	}

	@Test
	void d04_resetAndReuse() {
		var a = IntegerValue.of(100);
		assertEquals(100, a.get());

		a.set(0);
		assertEquals(0, a.get());

		for (int i = 0; i < 10; i++) {
			a.getAndIncrement();
		}
		assertEquals(10, a.get());
	}
}