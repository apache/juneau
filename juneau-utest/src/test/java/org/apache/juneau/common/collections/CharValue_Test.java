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
		
		for (char c = 'a'; c <= 'z'; c++) {
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
}

