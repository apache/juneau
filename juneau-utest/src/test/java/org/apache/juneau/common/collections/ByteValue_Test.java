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

class ByteValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var a = ByteValue.create();
		assertEquals((byte)0, a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a02_of() {
		var a = ByteValue.of((byte)42);
		assertEquals((byte)42, a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a03_of_null() {
		var a = ByteValue.of(null);
		assertEquals((byte)0, a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a04_constructor_default() {
		var a = new ByteValue();
		assertEquals((byte)0, a.get());
	}

	@Test
	void a05_constructor_withValue() {
		var a = new ByteValue((byte)100);
		assertEquals((byte)100, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// increment/decrement tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_increment() {
		var a = ByteValue.of((byte)5);
		a.increment();
		assertEquals((byte)6, a.get());
	}

	@Test
	void b02_decrement() {
		var a = ByteValue.of((byte)5);
		a.decrement();
		assertEquals((byte)4, a.get());
	}

	@Test
	void b03_incrementAndGet() {
		var a = ByteValue.of((byte)5);
		assertEquals((byte)6, a.incrementAndGet());
		assertEquals((byte)6, a.get());
	}

	@Test
	void b04_decrementAndGet() {
		var a = ByteValue.of((byte)5);
		assertEquals((byte)4, a.decrementAndGet());
		assertEquals((byte)4, a.get());
	}

	@Test
	void b05_increment_chain() {
		var a = ByteValue.of((byte)0);
		a.increment().increment().increment();
		assertEquals((byte)3, a.get());
	}

	@Test
	void b06_decrement_chain() {
		var a = ByteValue.of((byte)10);
		a.decrement().decrement().decrement();
		assertEquals((byte)7, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// add/addAndGet tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_add() {
		var a = ByteValue.of((byte)10);
		a.add((byte)5);
		assertEquals((byte)15, a.get());
	}

	@Test
	void c02_addAndGet() {
		var a = ByteValue.of((byte)10);
		assertEquals((byte)15, a.addAndGet((byte)5));
		assertEquals((byte)15, a.get());
	}

	@Test
	void c03_add_null() {
		var a = ByteValue.of((byte)10);
		a.add(null);
		assertEquals((byte)10, a.get());
	}

	@Test
	void c04_add_chain() {
		var a = ByteValue.of((byte)0);
		a.add((byte)5).add((byte)10).add((byte)15);
		assertEquals((byte)30, a.get());
	}

	@Test
	void c05_add_negative() {
		var a = ByteValue.of((byte)10);
		a.add((byte)-3);
		assertEquals((byte)7, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is/isAny tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_is_match() {
		var a = ByteValue.of((byte)42);
		assertTrue(a.is((byte)42));
	}

	@Test
	void d02_is_noMatch() {
		var a = ByteValue.of((byte)42);
		assertFalse(a.is((byte)43));
	}

	@Test
	void d03_isAny_match() {
		var a = ByteValue.of((byte)5);
		assertTrue(a.isAny((byte)3, (byte)5, (byte)7));
	}

	@Test
	void d04_isAny_noMatch() {
		var a = ByteValue.of((byte)5);
		assertFalse(a.isAny((byte)1, (byte)2));
	}

	@Test
	void d05_isAny_empty() {
		var a = ByteValue.of((byte)5);
		assertFalse(a.isAny());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setIf/update tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_setIf_true() {
		var a = ByteValue.of((byte)10);
		a.setIf(true, (byte)20);
		assertEquals((byte)20, a.get());
	}

	@Test
	void e02_setIf_false() {
		var a = ByteValue.of((byte)10);
		a.setIf(false, (byte)20);
		assertEquals((byte)10, a.get());
	}

	@Test
	void e03_update() {
		var a = ByteValue.of((byte)10);
		a.update(x -> (byte)(x * 2));
		assertEquals((byte)20, a.get());
	}

	@Test
	void e04_update_chain() {
		var a = ByteValue.of((byte)2);
		a.update(x -> (byte)(x * 2)).update(x -> (byte)(x + 1));
		assertEquals((byte)5, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Byte> functionality tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_orElse() {
		var a = ByteValue.of((byte)10);
		assertEquals((byte)10, a.orElse((byte)99));

		a.set(null);
		assertEquals((byte)99, a.orElse((byte)99));
	}

	@Test
	void f02_getAndSet() {
		var a = ByteValue.of((byte)5);
		var b = a.getAndSet((byte)10);
		assertEquals((byte)5, b);
		assertEquals((byte)10, a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_counter() {
		var a = ByteValue.create();
		for (var i = 0; i < 10; i++) {
			a.increment();
		}
		assertEquals((byte)10, a.get());
	}

	@Test
	void g02_accumulator() {
		var a = ByteValue.create();
		var list = l((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
		list.forEach(a::add);
		assertEquals((byte)15, a.get());
	}

	@Test
	void g03_conditionalCounter() {
		var a = ByteValue.create();
		for (var i = 0; i < 20; i++) {
			a.setIf(i % 2 == 0, (byte)(a.get() + 1));
		}
		assertEquals((byte)10, a.get());  // 10 even numbers
	}
}

