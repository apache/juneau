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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.PredicateUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.*;

import org.junit.jupiter.api.*;

class PredicateUtils_Test {

	//====================================================================================================
	// Constructor (line 24)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 24: class declaration
		// PredicateUtils has a private constructor, so it cannot be instantiated.
		// Line 24 (class declaration) is covered by using the class's static methods.
	}

	//====================================================================================================
	// consumeIf(Predicate<T>, Consumer<T>, T)
	//====================================================================================================
	@Test
	void a001_consumeIf() {
		List<String> consumed = new ArrayList<>();
		Consumer<String> consumer = consumed::add;
		
		// When predicate is null, should consume
		consumeIf(null, consumer, "test");
		assertEquals(1, consumed.size());
		assertEquals("test", consumed.get(0));
		
		// When predicate matches, should consume
		consumed.clear();
		Predicate<String> matches = s -> s.equals("match");
		consumeIf(matches, consumer, "match");
		assertEquals(1, consumed.size());
		assertEquals("match", consumed.get(0));
		
		// When predicate doesn't match, should not consume
		consumed.clear();
		Predicate<String> noMatch = s -> s.equals("match");
		consumeIf(noMatch, consumer, "nomatch");
		assertTrue(consumed.isEmpty());
		
		// Test with different types
		List<Integer> intConsumed = new ArrayList<>();
		Consumer<Integer> intConsumer = intConsumed::add;
		Predicate<Integer> even = i -> i % 2 == 0;
		consumeIf(even, intConsumer, 2);
		assertEquals(1, intConsumed.size());
		assertEquals(2, intConsumed.get(0));
		
		consumeIf(even, intConsumer, 3);
		assertEquals(1, intConsumed.size()); // Should not add 3
	}

	//====================================================================================================
	// peek()
	//====================================================================================================
	@Test
	void a002_peek() {
		// Capture stderr output
		PrintStream originalErr = System.err;
		ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
		System.setErr(new PrintStream(errCapture));
		
		try {
			// Test peek() function
			Function<String, String> peekFunc = peek();
			String result = peekFunc.apply("test value");
			
			// Should return the value unchanged
			assertEquals("test value", result);
			
			// Should have printed to stderr
			String output = errCapture.toString();
			assertTrue(output.contains("test value"), "Output should contain 'test value', but was: " + output);
			
			// Test with null
			errCapture.reset();
			Function<Object, Object> peekFunc2 = peek();
			Object result2 = peekFunc2.apply(null);
			assertNull(result2);
			String output2 = errCapture.toString();
			assertTrue(output2.contains("null"), "Output should contain 'null', but was: " + output2);
			
			// Test with different types
			errCapture.reset();
			Function<Integer, Integer> peekInt = peek();
			Integer result3 = peekInt.apply(123);
			assertEquals(123, result3);
			String output3 = errCapture.toString();
			assertTrue(output3.contains("123"), "Output should contain '123', but was: " + output3);
		} finally {
			System.setErr(originalErr);
		}
	}

	//====================================================================================================
	// peek(String, Function<T,?>)
	//====================================================================================================
	@Test
	void a003_peek_withMessage() {
		// Capture stderr output
		PrintStream originalErr = System.err;
		ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
		System.setErr(new PrintStream(errCapture));
		
		try {
			// Test peek() with message and formatter
			Function<String, String> peekFunc = peek("Processing: {0}", s -> s.toUpperCase());
			String result = peekFunc.apply("test");
			
			// Should return the value unchanged
			assertEquals("test", result);
			
			// Should have printed formatted message to stderr
			String output = errCapture.toString();
			assertTrue(output.contains("Processing: TEST"), "Output should contain 'Processing: TEST', but was: " + output);
			
			// Test with different formatter
			errCapture.reset();
			Function<Integer, Integer> peekInt = peek("Value: {0}", i -> i * 2);
			Integer result2 = peekInt.apply(5);
			assertEquals(5, result2);
			String output2 = errCapture.toString();
			assertTrue(output2.contains("Value: 10"), "Output should contain 'Value: 10', but was: " + output2);
			
			// Test with null value
			errCapture.reset();
			Function<String, String> peekNull = peek("Null value: {0}", s -> s == null ? "null" : s);
			String result3 = peekNull.apply(null);
			assertNull(result3);
			String output3 = errCapture.toString();
			assertTrue(output3.contains("Null value: null"), "Output should contain 'Null value: null', but was: " + output3);
			
			// Test with complex formatter
			errCapture.reset();
			class Person {
				String name;
				Person(String name) { this.name = name; }
			}
			Function<Person, Person> peekPerson = peek("Person: {0}", p -> p.name);
			Person person = new Person("John");
			Person result4 = peekPerson.apply(person);
			assertSame(person, result4);
			String output4 = errCapture.toString();
			assertTrue(output4.contains("Person: John"), "Output should contain 'Person: John', but was: " + output4);
		} finally {
			System.setErr(originalErr);
		}
	}

	//====================================================================================================
	// test(Predicate<T>, T)
	//====================================================================================================
	@Test
	void a004_test() {
		// When predicate is null, should return true
		assertTrue(test(null, "any value"));
		assertTrue(test(null, null));
		assertTrue(test(null, 123));
		
		// When predicate matches, should return true
		Predicate<String> matches = s -> s.equals("match");
		assertTrue(test(matches, "match"));
		
		// When predicate doesn't match, should return false
		assertFalse(test(matches, "nomatch"));
		
		// Test with different types
		Predicate<Integer> even = i -> i % 2 == 0;
		assertTrue(test(even, 2));
		assertTrue(test(even, 4));
		assertFalse(test(even, 3));
		assertFalse(test(even, 5));
		
		// Test with null value
		Predicate<String> notNull = s -> s != null;
		assertTrue(test(notNull, "test"));
		assertFalse(test(notNull, null));
		
		// Test with always true predicate
		Predicate<Object> alwaysTrue = o -> true;
		assertTrue(test(alwaysTrue, "anything"));
		assertTrue(test(alwaysTrue, null));
		assertTrue(test(alwaysTrue, 123));
		
		// Test with always false predicate
		Predicate<Object> alwaysFalse = o -> false;
		assertFalse(test(alwaysFalse, "anything"));
		assertFalse(test(alwaysFalse, null));
		assertFalse(test(alwaysFalse, 123));
	}
}
