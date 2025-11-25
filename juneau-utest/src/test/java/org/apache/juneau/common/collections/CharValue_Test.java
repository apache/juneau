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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CharValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var v = CharValue.create();
		assertEquals('\0', v.get());
	}

	@Test
	void a02_of() {
		var v = CharValue.of('A');
		assertEquals('A', v.get());
	}

	@Test
	void a03_constructor() {
		var v = new CharValue('Z');
		assertEquals('Z', v.get());
	}

	@Test
	void a04_constructor_withNull() {
		var v = new CharValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Character> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_set() {
		var v = CharValue.create();
		v.set('B');
		assertEquals('B', v.get());
	}

	@Test
	void b02_setIfEmpty() {
		var v = new CharValue(null);
		v.setIfEmpty('X');
		assertEquals('X', v.get());

		v.setIfEmpty('Y');
		assertEquals('X', v.get()); // Should not change
	}

	@Test
	void b03_orElse() {
		var v = new CharValue(null);
		assertEquals('?', v.orElse('?'));

		v.set('!');
		assertEquals('!', v.orElse('?'));
	}

	@Test
	void b04_map() {
		var v = CharValue.of('a');
		Value<Character> v2 = v.map(Character::toUpperCase);
		assertEquals('A', v2.get());
	}

	@Test
	void b05_ifPresent() {
		var v = CharValue.of('C');
		var sb = new StringBuilder();
		v.ifPresent(sb::append);
		assertEquals("C", sb.toString());
	}

	@Test
	void b06_isPresent() {
		var v = new CharValue(null);
		assertFalse(v.isPresent());

		v.set('D');
		assertTrue(v.isPresent());
	}

	@Test
	void b07_isEmpty() {
		var v = new CharValue(null);
		assertEmpty(v);

		v.set('E');
		assertNotEmpty(v);
	}

	@Test
	void b08_getAndSet() {
		var v = CharValue.of('F');
		assertEquals('F', v.getAndSet('G'));
		assertEquals('G', v.get());
	}

	@Test
	void b09_getAndUnset() {
		var v = CharValue.of('H');
		assertEquals('H', v.getAndUnset());
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_trackingLastCharacter() {
		var lastChar = CharValue.create();

		"Hello World".chars().mapToObj(c -> (char)c).forEach(ch -> {
			lastChar.set(ch);
		});

		assertEquals('d', lastChar.get());
	}

	@Test
	void c02_findingFirstUppercase() {
		var firstUpper = CharValue.of('\0');

		"helloWorld".chars().mapToObj(c -> (char)c).forEach(ch -> {
			if (Character.isUpperCase(ch) && firstUpper.get() == '\0') {
				firstUpper.set(ch);
			}
		});

		assertEquals('W', firstUpper.get());
	}

	@Test
	void c03_toggleCase() {
		var ch = CharValue.of('a');

		// Toggle to uppercase
		ch.set(Character.toUpperCase(ch.get()));
		assertEquals('A', ch.get());

		// Toggle back to lowercase
		ch.set(Character.toLowerCase(ch.get()));
		assertEquals('a', ch.get());
	}

	@Test
	void c04_trackingMostFrequentChar() {
		String text = "aabbccccdd";
		var mostFrequent = CharValue.of('\0');
		var maxCount = IntegerValue.create();

		for (var c = 'a'; c <= 'z'; c++) {
			final char current = c;
			var count = IntegerValue.create();
			text.chars().mapToObj(ch -> (char)ch).forEach(ch -> {
				if (ch == current) {
					count.getAndIncrement();
				}
			});

			if (count.get() > maxCount.get()) {
				maxCount.set(count.get());
				mostFrequent.set(current);
			}
		}

		assertEquals('c', mostFrequent.get());
		assertEquals(4, maxCount.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Increment/Decrement operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_increment() {
		var v = CharValue.of('A');
		v.increment();
		assertEquals('B', v.get());
	}

	@Test
	void d02_increment_fromNull() {
		var v = new CharValue(null);
		v.increment();
		assertEquals('\u0001', v.get());  // null treated as 0, so 0+1 = 1
	}

	@Test
	void d03_increment_wrapsAround() {
		var v = CharValue.of((char)65535);  // Max char value
		v.increment();
		assertEquals('\u0000', v.get());  // Wraps to 0
	}

	@Test
	void d04_decrement() {
		var v = CharValue.of('B');
		v.decrement();
		assertEquals('A', v.get());
	}

	@Test
	void d05_decrement_fromNull() {
		var v = new CharValue(null);
		v.decrement();
		assertEquals('\uFFFF', v.get());  // null treated as 0, so 0-1 = 65535 (wraps)
	}

	@Test
	void d06_decrement_wrapsAround() {
		var v = CharValue.of('\u0000');  // Min char value
		v.decrement();
		assertEquals('\uFFFF', v.get());  // Wraps to max
	}

	@Test
	void d07_incrementAndGet() {
		var v = CharValue.of('A');
		var result = v.incrementAndGet();
		assertEquals('B', result);
		assertEquals('B', v.get());
	}

	@Test
	void d08_incrementAndGet_fromNull() {
		var v = new CharValue(null);
		var result = v.incrementAndGet();
		assertEquals('\u0001', result);
		assertEquals('\u0001', v.get());
	}

	@Test
	void d09_decrementAndGet() {
		var v = CharValue.of('B');
		var result = v.decrementAndGet();
		assertEquals('A', result);
		assertEquals('A', v.get());
	}

	@Test
	void d10_decrementAndGet_fromNull() {
		var v = new CharValue(null);
		var result = v.decrementAndGet();
		assertEquals('\uFFFF', result);
		assertEquals('\uFFFF', v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_add() {
		var v = CharValue.of('A');
		v.add((char)5);
		assertEquals('F', v.get());
	}

	@Test
	void e02_add_withNullValue() {
		var v = CharValue.of('A');
		v.add(null);
		assertEquals('A', v.get());  // null treated as 0, so A+0 = A
	}

	@Test
	void e03_add_toNullValue() {
		var v = new CharValue(null);
		v.add('B');
		assertEquals('B', v.get());  // null treated as 0, so 0+B = B
	}

	@Test
	void e04_add_bothNull() {
		var v = new CharValue(null);
		v.add(null);
		assertEquals('\u0000', v.get());  // null+null = 0+0 = 0
	}

	@Test
	void e05_add_wrapsAround() {
		var v = CharValue.of((char)65534);
		v.add((char)2);
		assertEquals('\u0000', v.get());  // Wraps to 0
	}

	@Test
	void e06_addAndGet() {
		var v = CharValue.of('A');
		var result = v.addAndGet((char)5);
		assertEquals('F', result);
		assertEquals('F', v.get());
	}

	@Test
	void e07_addAndGet_withNullValue() {
		var v = CharValue.of('A');
		var result = v.addAndGet(null);
		assertEquals('A', result);
		assertEquals('A', v.get());
	}

	@Test
	void e08_addAndGet_toNullValue() {
		var v = new CharValue(null);
		var result = v.addAndGet('B');
		assertEquals('B', result);
		assertEquals('B', v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Comparison operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_is() {
		var v = CharValue.of('A');
		assertTrue(v.is('A'));
		assertFalse(v.is('B'));
		assertFalse(v.is(null));
	}

	@Test
	void f02_is_withNullValue() {
		var v = new CharValue(null);
		assertFalse(v.is('A'));
		assertTrue(v.is(null));
	}

	@Test
	void f03_isAny_withVarargs() {
		var v = CharValue.of('B');
		assertTrue(v.isAny('A', 'B', 'C'));
		assertTrue(v.isAny('B'));
		assertFalse(v.isAny('X', 'Y', 'Z'));
		assertFalse(v.isAny());
	}

	@Test
	void f04_isAny_withVarargs_nullValue() {
		var v = new CharValue(null);
		assertFalse(v.isAny('A', 'B', 'C'));
		assertTrue(v.isAny((Character)null));
		assertTrue(v.isAny('A', null, 'B'));
	}

	@Test
	void f05_isAny_withString() {
		var v = CharValue.of('B');
		assertTrue(v.isAny("ABC"));
		assertTrue(v.isAny("XYZB"));
		assertFalse(v.isAny("XYZ"));
		assertFalse(v.isAny(""));
		assertFalse(v.isAny((String)null));
	}

	@Test
	void f06_isAny_withString_nullValue() {
		var v = new CharValue(null);
		assertFalse(v.isAny("ABC"));
		assertFalse(v.isAny(""));
		assertFalse(v.isAny((String)null));
	}
}

