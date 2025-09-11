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
package org.apache.juneau.junit;

import static org.apache.juneau.junit.Assertions2.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.junit.jupiter.api.*;
import org.opentest4j.*;

/**
 * Unit tests for the {@link Assertions2} class.
 *
 * <p>This test class focuses on testing the assertion methods' behavior, error handling,
 * and argument passing. The underlying BeanConverter functionality is tested separately
 * in BasicBeanConverter_Test.</p>
 */
class Assertions2_Test extends TestBase {

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
			var e = assertThrows(AssertionFailedError.class, () -> Assertions2.assertBeans((Object)null, "name", "test"));
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

			assertDoesNotThrow(() -> Assertions2.assertContainsAll(args, (Object)"Testing", "Test"));
		}

		@Test
		void f03_missingSubstring() {
			var e = assertThrows(AssertionFailedError.class, () -> Assertions2.assertContainsAll("Hello World", "Hello", "Missing"));
			assertContains("String did not contain expected substring", e.getMessage());
			assertContains("Missing", e.getMessage());
		}

		@Test
		void f04_nullValue() {
			var e = assertThrows(AssertionFailedError.class, () -> Assertions2.assertContainsAll((Object)null, "test"));
			assertContains("Value was null", e.getMessage());
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
		private final TestAddress address;

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
}
