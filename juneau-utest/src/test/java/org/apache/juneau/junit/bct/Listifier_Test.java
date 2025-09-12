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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

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
	class a_FunctionalInterfaceCompliance {

		@SuppressWarnings("cast")
		@Test
		void a01_functionalInterfaceContract() {
			// Verify it's a proper functional interface
			Listifier<String> listifier = (converter, obj) -> Arrays.asList(obj, obj.toUpperCase());

			assertNotNull(listifier);
			assertTrue(listifier instanceof BiFunction);
			assertTrue(listifier instanceof Listifier);
		}

		@Test
		void a02_lambdaExpressionCompatibility() {
			// Test lambda expression usage
			Listifier<String> lambda = (converter, str) -> Arrays.asList(str.toLowerCase(), str.toUpperCase());

			var converter = BasicBeanConverter.DEFAULT;
			var result = lambda.apply(converter, "Test");

			assertEquals(2, result.size());
			assertEquals("test", result.get(0));
			assertEquals("TEST", result.get(1));
		}

		@Test
		void a03_methodReferenceCompatibility() {
			// Test method reference usage
			Listifier<String> methodRef = ListifierMethods::splitToChars;

			var converter = BasicBeanConverter.DEFAULT;
			var result = methodRef.apply(converter, "abc");

			assertEquals(3, result.size());
			assertEquals("a", result.get(0));
			assertEquals("b", result.get(1));
			assertEquals("c", result.get(2));
		}

		@Test
		void a04_biFunctionInheritance() {
			// Verify BiFunction methods are inherited
			Listifier<String> listifier = (converter, str) -> Arrays.asList((Object[])str.split(""));

			// Test BiFunction.apply method
			var converter = BasicBeanConverter.DEFAULT;
			var result = listifier.apply(converter, "xy");
			assertEquals(2, result.size());
		}
	}

	// ====================================================================================================
	// Lambda Composition Tests
	// ====================================================================================================

	@Nested
	class b_LambdaComposition {

		@Test
		void b01_andThenComposition() {
			Listifier<String> base = (converter, str) -> Arrays.asList(str.toLowerCase());
			Function<List<Object>, List<Object>> mapper = list -> {
				List<Object> result = new ArrayList<>(list);
				result.add("ADDED");
				return result;
			};

			BiFunction<BeanConverter, String, List<Object>> composed = base.andThen(mapper);

			var converter = BasicBeanConverter.DEFAULT;
			var result = composed.apply(converter, "TEST");

			assertEquals(2, result.size());
			assertEquals("test", result.get(0));
			assertEquals("ADDED", result.get(1));
		}

		@Test
		void b02_functionalComposition() {
			// Compose multiple listifiers
			Listifier<String> splitter = (converter, str) -> Arrays.asList((Object[])str.split(","));
			Listifier<String> trimmer = (converter, str) -> Arrays.asList((Object[])str.trim().split("\\s+"));

			var converter = BasicBeanConverter.DEFAULT;

			var splitResult = splitter.apply(converter, "a,b,c");
			assertEquals(3, splitResult.size());

			var trimResult = trimmer.apply(converter, "  hello   world  ");
			assertEquals(2, trimResult.size());
			assertEquals("hello", trimResult.get(0));
			assertEquals("world", trimResult.get(1));
		}
	}

	// ====================================================================================================
	// Edge Case Tests
	// ====================================================================================================

	@Nested
	class c_EdgeCases {

		@Test
		void c01_nullInputHandling() {
			Listifier<String> nullSafe = (converter, str) -> {
				if (str == null) return Arrays.asList("NULL_INPUT");
				return Arrays.asList(str);
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = nullSafe.apply(converter, null);

			assertEquals(1, result.size());
			assertEquals("NULL_INPUT", result.get(0));
		}

		@Test
		void c02_emptyResultHandling() {
			Listifier<String> emptyReturner = (converter, str) -> new ArrayList<>();

			var converter = BasicBeanConverter.DEFAULT;
			var result = emptyReturner.apply(converter, "anything");

			assertNotNull(result);
			assertEquals(0, result.size());
		}

		@Test
		void c03_exceptionHandling() {
			Listifier<String> throwing = (converter, str) -> {
				if ("ERROR".equals(str)) {
					throw new RuntimeException("Intentional test exception");
				}
				return Arrays.asList(str);
			};

			var converter = BasicBeanConverter.DEFAULT;

			// Normal case should work
			var normalResult = throwing.apply(converter, "normal");
			assertEquals(1, normalResult.size());
			assertEquals("normal", normalResult.get(0));

			// Exception case should throw
			assertThrows(RuntimeException.class, () -> throwing.apply(converter, "ERROR"));
		}

		@Test
		void c04_largeListHandling() {
			Listifier<Integer> largeListGenerator = (converter, count) -> {
				List<Object> result = new ArrayList<>();
				for (int i = 0; i < count; i++) {
					result.add("item_" + i);
				}
				return result;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = largeListGenerator.apply(converter, 1000);

			assertEquals(1000, result.size());
			assertEquals("item_0", result.get(0));
			assertEquals("item_999", result.get(999));
		}
	}

	// ====================================================================================================
	// Integration Tests
	// ====================================================================================================

	@Nested
	class d_Integration {

		@Test
		void d01_converterIntegration() {
			// Test integration with custom converter
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addListifier(TestObject.class, (converter, obj) ->
				Arrays.asList(obj.name, obj.value, "LISTIFIED"))
				.build();

			var test = new TestObject("test", 42);
			var result = customConverter.listify(test);

			assertEquals(3, result.size());
			assertEquals("test", result.get(0));
			assertEquals(42, result.get(1));
			assertEquals("LISTIFIED", result.get(2));
		}

		@Test
		void d02_multipleListifierRegistration() {
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addListifier(String.class, (converter, str) -> Arrays.asList(str.toLowerCase()))
				.addListifier(Integer.class, (converter, num) -> Arrays.asList(num, num * 2))
				.build();

			// Test string listifier
			var stringResult = customConverter.listify("TEST");
			assertEquals(1, stringResult.size());
			assertEquals("test", stringResult.get(0));

			// Test integer listifier
			var intResult = customConverter.listify(5);
			assertEquals(2, intResult.size());
			assertEquals(5, intResult.get(0));
			assertEquals(10, intResult.get(1));
		}

		@Test
		void d03_converterPassthrough() {
			// Test that converter parameter is properly passed
			Listifier<String> converterUser = (converter, str) -> {
				// Use the converter parameter to stringify something
				String stringified = converter.stringify(Arrays.asList("nested", "call"));
				return Arrays.asList(str, stringified);
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = converterUser.apply(converter, "test");

			assertEquals(2, result.size());
			assertEquals("test", result.get(0));
			assertTrue(result.get(1).toString().contains("nested"));
		}
	}

	// ====================================================================================================
	// Performance Tests
	// ====================================================================================================

	@Nested
	class e_Performance {

		@Test
		void e01_performanceWithLargeLists() {
			Listifier<Integer> rangeGenerator = (converter, count) -> {
				List<Object> result = new ArrayList<>();
				for (int i = 0; i < count; i++) {
					result.add(i);
				}
				return result;
			};

			var converter = BasicBeanConverter.DEFAULT;

			var start = System.currentTimeMillis();
			var result = rangeGenerator.apply(converter, 10000);
			var end = System.currentTimeMillis();

			assertEquals(10000, result.size());
			assertTrue(end - start < 1000, "Should complete within 1 second");
		}

		@Test
		void e02_memoryEfficiency() {
			Listifier<String> memoryTest = (converter, str) -> {
				// Create a reasonably sized list
				List<Object> result = new ArrayList<>();
				for (int i = 0; i < 1000; i++) {
					result.add(str + "_" + i);
				}
				return result;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = memoryTest.apply(converter, "test");

			assertEquals(1000, result.size());
			assertEquals("test_0", result.get(0));
			assertEquals("test_999", result.get(999));
		}
	}

	// ====================================================================================================
	// Helper Classes and Methods
	// ====================================================================================================

	static class ListifierMethods {
		static List<Object> splitToChars(BeanConverter converter, String str) {
			return Arrays.asList((Object[])str.split(""));
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
