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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;
import org.opentest4j.*;

/**
 * Unit tests for the {@link Assertions2} class.
 *
 * <p>This test class focuses on testing the assertion methods' behavior, error handling,
 * and argument passing. The underlying BeanConverter functionality is tested separately
 * in BasicBeanConverter_Test.</p>
 */
class BctAssertions_Test extends TestBase {

	// ====================================================================================================
	// AssertionArgs Tests
	// ====================================================================================================

	@Nested
	class a_AssertionArgs {

		@Test
		void a01_args() {
			var args = args();
			assertNotNull(args);
			assertTrue(args.getBeanConverter().isEmpty());
			assertNull(args.getMessage());
		}
	}

	// ====================================================================================================
	// Bean Property Tests
	// ====================================================================================================

	@Nested
	class b_AssertBean {

		@Test
		void b01_basicAssertion() {
			var person = new TestPerson("Alice", 25);

			// Test successful assertion
			assertDoesNotThrow(() -> assertBean(person, "name", "Alice"));
		}

		@Test
		void b02_withCustomArgs() {
			var person = new TestPerson("Bob", 30);
			var args = args().setMessage("Custom message");

			assertDoesNotThrow(() -> assertBean(args, person, "name", "Bob"));
		}

		@Test
		void b03_nullObject() {
			var e = assertThrows(AssertionFailedError.class, () -> assertBean(null, "name", "test"));
			assertContains("Actual was null", e.getMessage());
		}

		@Test
		void b04_assertionFailure() {
			var person = new TestPerson("Eve", 28);

			var e = assertThrows(AssertionFailedError.class, () -> assertBean(person, "name", "Wrong"));
			assertContains("expected: <Wrong>", e.getMessage());
			assertContains("but was: <Eve>", e.getMessage());
		}

		@Test
		void b05_customMessage() {
			var person = new TestPerson("Charlie", 35);
			var args = args().setMessage("Custom error message");

			var e = assertThrows(AssertionFailedError.class, () -> assertBean(args, person, "name", "Wrong"));
			assertContains("Custom error message", e.getMessage());
		}
	}

	// ====================================================================================================
	// Multiple Beans Tests
	// ====================================================================================================

	@Nested
	class c_AssertBeans {

		@Test
		void c01_basicBeansAssertion() {
			var people = List.of(new TestPerson("Alice", 25), new TestPerson("Bob", 30));

			assertDoesNotThrow(() -> assertBeans(people, "name", "Alice", "Bob"));
		}

		@Test
		void c02_withCustomArgs() {
			var people = List.of(new TestPerson("Charlie", 35));
			var args = args().setMessage("Custom beans message");

			assertDoesNotThrow(() -> assertBeans(args, people, "name", "Charlie"));
		}

		@Test
		void c03_emptyCollection() {
			assertDoesNotThrow(() -> assertBeans(List.of(), "name"));
		}

		@Test
		void c04_sizeMismatch() {
			var people = List.of(new TestPerson("Dave", 40));

			var e = assertThrows(AssertionFailedError.class, () -> assertBeans(people, "name", "Dave", "Extra"));
			assertContains("Wrong number of beans", e.getMessage());
		}

		@Test
		void c05_contentMismatch() {
			var people = List.of(new TestPerson("Eve", 28));

			var e = assertThrows(AssertionFailedError.class, () -> assertBeans(people, "name", "Wrong"));
			assertContains("Bean at row", e.getMessage());
		}

		@Test
		void c06_nullCollection() {
			var e = assertThrows(AssertionFailedError.class, () -> assertBeans((Object)null, "name", "test"));
			assertContains("Value was null", e.getMessage());
		}
	}

	// ====================================================================================================
	// Mapped Property Tests
	// ====================================================================================================

	@Nested
	class d_AssertMapped {

		@Test
		void d01_basicMapping() {
			var person = new TestPerson("Grace", 27);
			BiFunction<TestPerson, String, Object> mapper = (p, prop) -> p.getName().toUpperCase();

			assertDoesNotThrow(() -> assertMapped(person, mapper, "upperName", "GRACE"));
		}

		@Test
		void d02_withCustomArgs() {
			var person = new TestPerson("Henry", 45);
			var args = args().setMessage("Custom mapped message");
			BiFunction<TestPerson, String, Object> mapper = (p, prop) -> p.getName();

			assertDoesNotThrow(() -> assertMapped(args, person, mapper, "name", "Henry"));
		}

		@Test
		void d03_mappingMismatch() {
			var person = new TestPerson("Jack", 50);
			BiFunction<TestPerson, String, Object> mapper = (p, prop) -> p.getName();

			var e = assertThrows(AssertionFailedError.class, () -> assertMapped(person, mapper, "name", "Wrong"));
			assertContains("expected: <Wrong>", e.getMessage());
			assertContains("but was: <Jack>", e.getMessage());
		}
	}

	// ====================================================================================================
	// String Contains Tests
	// ====================================================================================================

	@Nested
	class e_AssertContains {

		@Test
		void e01_basicContains() {
			assertDoesNotThrow(() -> assertContains("Hello", "Hello World"));
		}

		@Test
		void e02_withCustomArgs() {
			var args = args().setMessage("Custom contains message");

			assertDoesNotThrow(() -> assertContains(args, "Test", "Test String"));
		}

		@Test
		void e03_doesNotContain() {
			var e = assertThrows(AssertionFailedError.class, () -> assertContains("Missing", "Hello World"));
			assertContains("String did not contain expected substring", e.getMessage());
		}

		@Test
		void e04_nullValue() {
			var e = assertThrows(IllegalArgumentException.class, () -> assertContains("test", null));
			assertContains("cannot be null", e.getMessage());
		}
	}

	// ====================================================================================================
	// Contains All Tests
	// ====================================================================================================

	@Nested
	class f_AssertContainsAll {

		@Test
		void f01_basicContainsAll() {
			assertDoesNotThrow(() -> assertContainsAll("Hello World", "Hello", "World"));
		}

		@Test
		void f02_withCustomArgs() {
			var args = args().setMessage("Custom contains all message");

			assertDoesNotThrow(() -> assertContainsAll(args, (Object)"Testing", "Test"));
		}

		@Test
		void f03_missingSubstring() {
			var e = assertThrows(AssertionFailedError.class, () -> assertContainsAll("Hello World", "Hello", "Missing"));
			assertContains("String did not contain expected substring", e.getMessage());
			assertContains("Missing", e.getMessage());
		}

		@Test
		void f04_nullValue() {
			var e = assertThrows(AssertionFailedError.class, () -> assertContainsAll((Object)null, "test"));
			assertContains("Value was null", e.getMessage());
		}

		@Test
		void f05_multipleErrors() {
			// Test that multiple missing substrings are collected and reported together
			var text = "Hello World Testing";
			var expected = new String[]{"Hello", "Missing1", "World", "Missing2", "Testing"};

			var e = assertThrows(AssertionFailedError.class, () -> assertContainsAll(text, expected));

			// Should report multiple errors in a single assertion failure
			var message = e.getMessage();
			assertContains("2 substring assertions failed", message);
			assertContains("String did not contain expected substring", message);

			// Should mention both missing substrings
			assertContains("Missing1", message);
			assertContains("Missing2", message);

			// Should include the actual text
			assertContains("Hello World Testing", message);
		}

		@Test
		void f06_singleError() {
			// Test that single errors are still reported as single assertion failures
			var text = "Hello World";
			var expected = new String[]{"Hello", "Missing", "World"};

			var e = assertThrows(AssertionFailedError.class, () -> assertContainsAll(text, expected));

			// Should report single error normally (not as "1 substring assertions failed")
			var message = e.getMessage();
			assertDoesNotThrow(() -> assertTrue(!message.contains("1 substring assertions failed")));
			assertContains("String did not contain expected substring", message);
			assertContains("Missing", message);
		}
	}

	// ====================================================================================================
	// Empty Tests
	// ====================================================================================================

	@Nested
	class g_AssertEmpty {

		@Test
		void g01_basicEmpty() {
			assertDoesNotThrow(() -> assertEmpty(List.of()));
			assertDoesNotThrow(() -> assertEmpty(new String[0]));
		}

		@Test
		void g02_withCustomArgs() {
			var args = args().setMessage("Custom empty message");

			assertDoesNotThrow(() -> assertEmpty(args, List.of()));
		}

		@Test
		void g03_notEmpty() {
			var e = assertThrows(AssertionFailedError.class, () -> assertEmpty(List.of("item")));
			assertContains("Value was not empty", e.getMessage());
		}

		@Test
		void g04_nullValue() {
			var e = assertThrows(AssertionError.class, () -> assertEmpty(null));
			assertContains("Value was null", e.getMessage());
		}
	}

	// ====================================================================================================
	// List Tests
	// ====================================================================================================

	@Nested
	class h_AssertList {

		@Test
		void h01_basicList() {
			assertDoesNotThrow(() -> assertList(List.of("a", "b", "c"), "a", "b", "c"));
		}

		@Test
		void h02_withCustomArgs() {
			var args = args().setMessage("Custom list message");

			assertDoesNotThrow(() -> assertList(args, List.of(1, 2), 1, 2));
		}

		@Test
		void h03_sizeMismatch() {
			var e = assertThrows(AssertionFailedError.class, () -> assertList(List.of("a", "b"), "a", "b", "c"));
			assertContains("Wrong list length", e.getMessage());
		}

		@Test
		void h04_elementMismatch() {
			var e = assertThrows(AssertionFailedError.class, () -> assertList(List.of("a", "b"), "a", "wrong"));
			assertContains("Element at index 1 did not match", e.getMessage());
		}

		@Test
		void h05_nullValue() {
			var e = assertThrows(IllegalArgumentException.class, () -> assertList(null, "test"));
			assertContains("cannot be null", e.getMessage());
		}

		@Test
		void h06_predicateValidation() {
			// Test lines 765-766: Predicate-based element validation
			var args = args().setMessage("Custom predicate message");
			var numbers = Arrays.asList(1, 2, 3, 4, 5);

			// Test successful predicate validation
			assertDoesNotThrow(() -> assertList(args, numbers, 
				(Predicate<Integer>) x -> x == 1,   // First element should equal 1
				(Predicate<Integer>) x -> x > 1,    // Second element should be > 1
				"3",                                // Third element as string
				(Predicate<Integer>) x -> x % 2 == 0, // Fourth element should be even
				(Predicate<Integer>) x -> x == 5     // Fifth element should equal 5
			));

			// Test failed predicate validation - use single element list to avoid length mismatch
			var singleNumber = Arrays.asList(1);
			var e = assertThrows(AssertionFailedError.class, () -> 
				assertList(args, singleNumber, (Predicate<Integer>) x -> x == 99)); // Should fail
			assertContains("Element at index 0 did not pass predicate", e.getMessage());
			assertContains("actual: <1>", e.getMessage());
		}

		@Test
		void h07_multipleErrors() {
			// Test that multiple assertion errors are collected and reported together
			var list = Arrays.asList("a", "wrong1", "c", "wrong2", "e");
			var expected = new Object[]{"a", "b", "c", "d", "e"};

			var e = assertThrows(AssertionFailedError.class, () -> assertList(list, expected));

			// Should report multiple errors in a single assertion failure
			var message = e.getMessage();
			assertContains("2 list assertions failed", message);
			assertContains("Element at index 1 did not match", message);
			assertContains("Element at index 3 did not match", message);

			// Should include both expected and actual values
			assertContains("expected: <b>", message);
			assertContains("but was: <wrong1>", message);
			assertContains("expected: <d>", message);
			assertContains("but was: <wrong2>", message);
		}

		@Test
		void h08_singleError() {
			// Test that single errors are still reported as single assertion failures
			var list = Arrays.asList("a", "wrong", "c");
			var expected = new Object[]{"a", "b", "c"};

			var e = assertThrows(AssertionFailedError.class, () -> assertList(list, expected));

			// Should report single error normally (not as "1 list assertions failed")
			var message = e.getMessage();
			assertDoesNotThrow(() -> assertTrue(!message.contains("1 list assertions failed")));
			assertContains("Element at index 1 did not match", message);
		}
	}

	// ====================================================================================================
	// Not Empty Tests
	// ====================================================================================================

	@Nested
	class i_AssertNotEmpty {

		@Test
		void i01_basicNotEmpty() {
			assertDoesNotThrow(() -> assertNotEmpty(List.of("item")));
			assertDoesNotThrow(() -> assertNotEmpty(new String[]{"item"}));
		}

		@Test
		void i02_withCustomArgs() {
			var args = args().setMessage("Custom not empty message");

			assertDoesNotThrow(() -> assertNotEmpty(args, List.of("content")));
		}

		@Test
		void i03_actuallyEmpty() {
			var e = assertThrows(AssertionFailedError.class, () -> assertNotEmpty(List.of()));
			assertContains("Value was empty", e.getMessage());
		}

		@Test
		void i04_nullValue() {
			var e = assertThrows(AssertionError.class, () -> assertNotEmpty(null));
			assertContains("Value was null", e.getMessage());
		}
	}

	// ====================================================================================================
	// Size Tests
	// ====================================================================================================

	@Nested
	class j_AssertSize {

		@Test
		void j01_basicSizes() {
			assertDoesNotThrow(() -> assertSize(3, List.of("a", "b", "c")));
			assertDoesNotThrow(() -> assertSize(5, "hello"));
		}

		@Test
		void j02_withCustomArgs() {
			var args = args().setMessage("Custom size message");

			assertDoesNotThrow(() -> assertSize(args, 2, List.of("a", "b")));
		}

		@Test
		void j03_wrongSize() {
			var e = assertThrows(AssertionFailedError.class, () -> assertSize(5, List.of("a", "b", "c")));
			assertContains("Value not expected size", e.getMessage());
		}

		@Test
		void j04_nullValue() {
			var e = assertThrows(AssertionError.class, () -> assertSize(0, null));
			assertContains("Value was null", e.getMessage());
		}
	}

	// ====================================================================================================
	// String Tests
	// ====================================================================================================

	@Nested
	class k_AssertString {

		@Test
		void k01_basicString() {
			assertDoesNotThrow(() -> assertString("hello", "hello"));
		}

		@Test
		void k02_withCustomArgs() {
			var args = args().setMessage("Custom string message");

			assertDoesNotThrow(() -> assertString(args, "test", "test"));
		}

		@Test
		void k03_stringMismatch() {
			var e = assertThrows(AssertionFailedError.class, () -> assertString("expected", "actual"));
			assertContains("expected: <expected>", e.getMessage());
			assertContains("but was: <actual>", e.getMessage());
		}

		@Test
		void k04_nullValue() {
			var e = assertThrows(AssertionError.class, () -> assertString("test", null));
			assertContains("Value was null", e.getMessage());
		}
	}

	// ====================================================================================================
	// Pattern Matching Tests
	// ====================================================================================================

	@Nested
	class l_AssertMatchesGlob {

		@Test
		void l01_basicGlobPatterns() {
			assertDoesNotThrow(() -> assertMatchesGlob("hello*", "hello world"));
			assertDoesNotThrow(() -> assertMatchesGlob("h?llo", "hello"));
		}

		@Test
		void l02_withCustomArgs() {
			var args = args().setMessage("Custom glob message");

			assertDoesNotThrow(() -> assertMatchesGlob(args, "test*", "testing"));
		}

		@Test
		void l03_patternMismatch() {
			var e = assertThrows(AssertionFailedError.class, () -> assertMatchesGlob("hello*", "goodbye"));
			assertContains("Pattern didn't match", e.getMessage());
			assertContains("pattern: <hello*>", e.getMessage());
			assertContains("but was: <goodbye>", e.getMessage());
		}

		@Test
		void l04_nullValue() {
			var e = assertThrows(AssertionError.class, () -> assertMatchesGlob("*", null));
			assertContains("Value was null", e.getMessage());
		}

		@Test
		void l05_nullPattern() {
			var e = assertThrows(IllegalArgumentException.class, () -> assertMatchesGlob(null, "test"));
			assertContains("cannot be null", e.getMessage());
		}
	}

	// ====================================================================================================
	// Test Helper Classes
	// ====================================================================================================

	static class TestPerson {
		private final String name;
		private final int age;
		private TestAddress address;

		TestPerson(String name, int age) {
			this(name, age, null);
		}

		TestPerson(String name, int age, TestAddress address) {
			this.name = name;
			this.age = age;
			this.address = address;
		}

		String getName() { return name; }
		int getAge() { return age; }
		TestAddress getAddress() { return address; }
		void setAddress(TestAddress address) { this.address = address; }

		@Override
		public String toString() {
			return "TestPerson{name='" + name + "', age=" + age + "}";
		}
	}

	static class TestAddress {
		private final String street;
		private final String city;

		TestAddress(String street, String city) {
			this.street = street;
			this.city = city;
		}

		String getStreet() { return street; }
		String getCity() { return city; }

		@Override
		public String toString() {
			return "TestAddress{street='" + street + "', city='" + city + "'}";
		}
	}

	// ====================================================================================================
	// Enhanced Edge Case Tests
	// ====================================================================================================

	@Nested
	class h_EnhancedEdgeCases {

		@Test
		void h01_assertListWithMixedTypes() {
			// Test list with mixed data types
			var mixedList = Arrays.asList("string", 42, true, 3.14, null);

			assertList(mixedList, "string", 42, true, 3.14, null);
			assertSize(5, mixedList);
			assertContains("42", mixedList); // Number stringified
			assertContains("true", mixedList); // Boolean stringified
		}

		@Test
		void h02_assertMatchesGlobWithComplexPatterns() {
			// Test glob matching with various patterns
			var testStrings = Arrays.asList(
				"hello.txt", "test_file.log", "document.pdf", 
				"IMG_001.jpg", "data.xml", "script.js"
			);

			assertMatchesGlob("*.txt", testStrings.get(0));
			assertMatchesGlob("test_*", testStrings.get(1));
			assertMatchesGlob("*.pdf", testStrings.get(2));
			assertMatchesGlob("IMG_???.jpg", testStrings.get(3));
			assertMatchesGlob("*.xml", testStrings.get(4));
			assertMatchesGlob("script.*", testStrings.get(5));
		}

		@Test
		void h03_assertEmptyWithVariousEmptyTypes() {
			// Test empty assertions with different empty types
			assertEmpty(Arrays.asList());
			assertEmpty(new HashMap<>());
			assertEmpty(new HashSet<>());
			assertEmpty(Optional.empty());
		}

		@Test
		void h04_assertNotEmptyWithVariousNonEmptyTypes() {
			// Test non-empty assertions
			assertNotEmpty(Arrays.asList("item"));
			assertNotEmpty(Map.of("key", "value"));
			assertNotEmpty(Set.of("element"));
			assertNotEmpty(Optional.of("value"));
		}

		@Test
		void h05_assertContainsAllWithPartialMatches() {
			// Test containsAll with partial string matches
			var text = "The quick brown fox jumps over the lazy dog";

			assertContainsAll(text, "quick", "fox", "lazy");
			assertContainsAll(text, "The", "dog");

			// Test with object that stringifies to contain multiple values
			var complexObj = Map.of(
				"description", "This is a test with multiple keywords: alpha, beta, gamma"
			);
			assertContainsAll(complexObj, "alpha", "beta", "gamma", "keywords");
		}

		@Test
		void h06_assertSizeWithCustomListifiableObjects() {
			// Test size assertions with objects that can be converted to lists
			var stringArray = new String[]{"a", "b", "c"};
			var intArray = new int[]{1, 2, 3, 4, 5};

			assertSize(3, stringArray);
			assertSize(5, intArray);

			// Test with Stream (gets converted to list)
			assertSize(4, Stream.of("w", "x", "y", "z"));
		}
	}
}
