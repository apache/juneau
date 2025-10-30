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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.junit.bct.BctAssertions.*;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link Listifier} functional interface.
 *
 * <p>This test class verifies functional interface compliance, lambda compatibility,
 * and edge case handling for Listifier implementations.</p>
 */
class Listifier_Test extends TestBase {

	// ====================================================================================================
	// Functional Interface Compliance Tests
	// ====================================================================================================

	@Nested
	class A_functionalInterfaceCompliance extends TestBase {

		@SuppressWarnings("cast")
		@Test
		void a01_functionalInterfaceContract() {
			// Verify it's a proper functional interface
			Listifier<String> listifier = (converter, obj) -> l(obj, obj.toUpperCase());

			assertNotNull(listifier);
			assertTrue(listifier instanceof BiFunction);
			assertTrue(listifier instanceof Listifier);
		}

		@Test
		void a02_lambdaExpressionCompatibility() {
			// Test lambda expression usage
			Listifier<String> lambda = (converter, str) -> l(str.toLowerCase(), str.toUpperCase());

			var converter = BasicBeanConverter.DEFAULT;
			var result = lambda.apply(converter, "Test");

			assertSize(2, result);
			assertEquals("test", result.get(0));
			assertEquals("TEST", result.get(1));
		}

		@Test
		void a03_methodReferenceCompatibility() {
			// Test method reference usage
			Listifier<String> methodRef = ListifierMethods::splitToChars;

			var converter = BasicBeanConverter.DEFAULT;
			var result = methodRef.apply(converter, "abc");

			assertSize(3, result);
			assertEquals("a", result.get(0));
			assertEquals("b", result.get(1));
			assertEquals("c", result.get(2));
		}

		@Test
		void a04_biFunctionInheritance() {
			// Verify BiFunction methods are inherited
			Listifier<String> listifier = (converter, str) -> l((Object[])str.split(""));

			// Test BiFunction.apply method
			var converter = BasicBeanConverter.DEFAULT;
			var result = listifier.apply(converter, "xy");
			assertSize(2, result);
		}
	}

	// ====================================================================================================
	// Lambda Composition Tests
	// ====================================================================================================

	@Nested
	class B_lambdaComposition extends TestBase {

		@Test
		void b01_andThenComposition() {
			Listifier<String> base = (converter, str) -> l(str.toLowerCase());
			Function<List<Object>, List<Object>> mapper = list -> {
				List<Object> result = new ArrayList<>(list);
				result.add("ADDED");
				return result;
			};

			BiFunction<BeanConverter, String, List<Object>> composed = base.andThen(mapper);

			var converter = BasicBeanConverter.DEFAULT;
			var result = composed.apply(converter, "TEST");

			assertSize(2, result);
			assertEquals("test", result.get(0));
			assertEquals("ADDED", result.get(1));
		}

		@Test
		void b02_functionalComposition() {
			// Compose multiple listifiers
			Listifier<String> splitter = (converter, str) -> l((Object[])str.split(","));
			Listifier<String> trimmer = (converter, str) -> l((Object[])str.trim().split("\\s+"));

			var converter = BasicBeanConverter.DEFAULT;

			var splitResult = splitter.apply(converter, "a,b,c");
			assertSize(3, splitResult);

			var trimResult = trimmer.apply(converter, "  hello   world  ");
			assertSize(2, trimResult);
			assertEquals("hello", trimResult.get(0));
			assertEquals("world", trimResult.get(1));
		}
	}

	// ====================================================================================================
	// Edge Case Tests
	// ====================================================================================================

	@Nested
	class C_edgeCases extends TestBase {

		@Test
		void c01_nullInputHandling() {
			Listifier<String> nullSafe = (converter, str) -> {
				if (str == null) return l("NULL_INPUT");
				return l(str);
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = nullSafe.apply(converter, null);

			assertSize(1, result);
			assertEquals("NULL_INPUT", result.get(0));
		}

		@Test
		void c02_emptyResultHandling() {
			Listifier<String> emptyReturner = (converter, str) -> list();

			var converter = BasicBeanConverter.DEFAULT;
			var result = emptyReturner.apply(converter, "anything");

			assertNotNull(result);
			assertEmpty(result);
		}

		@Test
		void c03_exceptionHandling() {
			Listifier<String> throwing = (converter, str) -> {
				if ("ERROR".equals(str)) {
					throw new RuntimeException("Intentional test exception");
				}
				return l(str);
			};

			var converter = BasicBeanConverter.DEFAULT;

			// Normal case should work
			var normalResult = throwing.apply(converter, "normal");
			assertSize(1, normalResult);
			assertEquals("normal", normalResult.get(0));

			// Exception case should throw
			assertThrows(RuntimeException.class, () -> throwing.apply(converter, "ERROR"));
		}

		@Test
		void c04_largeListHandling() {
			Listifier<Integer> largeListGenerator = (converter, count) -> {
				List<Object> result = list();
				for (int i = 0; i < count; i++) {
					result.add("item_" + i);
				}
				return result;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = largeListGenerator.apply(converter, 1000);

			assertSize(1000, result);
			assertEquals("item_0", result.get(0));
			assertEquals("item_999", result.get(999));
		}
	}

	// ====================================================================================================
	// Integration Tests
	// ====================================================================================================

	@Nested
	class D_integration extends TestBase {

		@Test
		void d01_converterIntegration() {
			// Test integration with custom converter
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addListifier(TestObject.class, (converter, obj) ->
				l(obj.name, obj.value, "LISTIFIED"))
				.build();

			var test = new TestObject("test", 42);
			var result = customConverter.listify(test);

			assertSize(3, result);
			assertEquals("test", result.get(0));
			assertEquals(42, result.get(1));
			assertEquals("LISTIFIED", result.get(2));
		}

		@Test
		void d02_multipleListifierRegistration() {
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addListifier(String.class, (converter, str) -> l(str.toLowerCase()))
				.addListifier(Integer.class, (converter, num) -> l(num, num * 2))
				.build();

			// Test string listifier
			var stringResult = customConverter.listify("TEST");
			assertSize(1, stringResult);
			assertEquals("test", stringResult.get(0));

			// Test integer listifier
			var intResult = customConverter.listify(5);
			assertSize(2, intResult);
			assertEquals(5, intResult.get(0));
			assertEquals(10, intResult.get(1));
		}

		@Test
		void d03_converterPassthrough() {
			// Test that converter parameter is properly passed
			Listifier<String> converterUser = (converter, str) -> {
				// Use the converter parameter to stringify something
				String stringified = converter.stringify(l("nested", "call"));
				return l(str, stringified);
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = converterUser.apply(converter, "test");

			assertSize(2, result);
			assertEquals("test", result.get(0));
			assertTrue(result.get(1).toString().contains("nested"));
		}
	}

	// ====================================================================================================
	// Performance Tests
	// ====================================================================================================

	@Nested
	class E_performance extends TestBase {

		@Test
		void e01_performanceWithLargeLists() {
			Listifier<Integer> rangeGenerator = (converter, count) -> {
				List<Object> result = list();
				for (int i = 0; i < count; i++) {
					result.add(i);
				}
				return result;
			};

			var converter = BasicBeanConverter.DEFAULT;

			var start = System.currentTimeMillis();
			var result = rangeGenerator.apply(converter, 10000);
			var end = System.currentTimeMillis();

			assertSize(10000, result);
			assertTrue(end - start < 1000, "Should complete within 1 second");
		}

		@Test
		void e02_memoryEfficiency() {
			Listifier<String> memoryTest = (converter, str) -> {
				// Create a reasonably sized list
				List<Object> result = list();
				for (int i = 0; i < 1000; i++) {
					result.add(str + "_" + i);
				}
				return result;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = memoryTest.apply(converter, "test");

			assertSize(1000, result);
			assertEquals("test_0", result.get(0));
			assertEquals("test_999", result.get(999));
		}
	}

	// ====================================================================================================
	// Helper Classes and Methods
	// ====================================================================================================

	static class ListifierMethods {
		static List<Object> splitToChars(BeanConverter converter, String str) {
			return l((Object[])str.split(""));
		}
	}

	static class TestObject {
		final String name;
		final int value;

		TestObject(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}
}