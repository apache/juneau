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
 * Unit tests for the {@link Stringifier} functional interface.
 *
 * <p>This test class verifies functional interface compliance, lambda compatibility,
 * and edge case handling for Stringifier implementations.</p>
 */
class Stringifier_Test extends TestBase {

	// ====================================================================================================
	// Functional Interface Compliance Tests
	// ====================================================================================================

	@Nested
	class a_FunctionalInterfaceCompliance {

		@SuppressWarnings("cast")
		@Test
		void a01_functionalInterfaceContract() {
			// Verify it's a proper functional interface
			Stringifier<String> stringifier = (converter, obj) -> "STRINGIFIED:" + obj;

			assertNotNull(stringifier);
			assertTrue(stringifier instanceof BiFunction);
			assertTrue(stringifier instanceof Stringifier);
		}

		@Test
		void a02_lambdaExpressionCompatibility() {
			// Test lambda expression usage
			Stringifier<Integer> lambda = (converter, num) -> "NUMBER:" + num;

			var converter = BasicBeanConverter.DEFAULT;
			var result = lambda.apply(converter, 42);

			assertEquals("NUMBER:42", result);
		}

		@Test
		void a03_methodReferenceCompatibility() {
			// Test method reference usage
			Stringifier<String> methodRef = StringifierMethods::addPrefix;

			var converter = BasicBeanConverter.DEFAULT;
			var result = methodRef.apply(converter, "test");

			assertEquals("PREFIX:test", result);
		}

		@Test
		void a04_biFunctionInheritance() {
			// Verify BiFunction methods are inherited
			Stringifier<String> stringifier = (converter, str) -> str.toUpperCase();

			// Test BiFunction.apply method
			var converter = BasicBeanConverter.DEFAULT;
			var result = stringifier.apply(converter, "test");
			assertEquals("TEST", result);
		}
	}

	// ====================================================================================================
	// Lambda Composition Tests
	// ====================================================================================================

	@Nested
	class b_LambdaComposition {

		@Test
		void b01_andThenComposition() {
			Stringifier<String> base = (converter, str) -> str.toLowerCase();
			Function<String, String> postProcessor = s -> "[" + s + "]";

			BiFunction<BeanConverter, String, String> composed = base.andThen(postProcessor);

			var converter = BasicBeanConverter.DEFAULT;
			var result = composed.apply(converter, "TEST");

			assertEquals("[test]", result);
		}

		@Test
		void b02_functionalComposition() {
			// Compose multiple stringification steps
			Stringifier<String> upperCase = (converter, str) -> str.toUpperCase();
			Stringifier<String> prefixed = (converter, str) -> "PROCESSED:" + str;

			BeanConverter converter = BasicBeanConverter.DEFAULT;

			var upperResult = upperCase.apply(converter, "test");
			assertEquals("TEST", upperResult);

			var prefixedResult = prefixed.apply(converter, "value");
			assertEquals("PROCESSED:value", prefixedResult);
		}
	}

	// ====================================================================================================
	// Edge Case Tests
	// ====================================================================================================

	@Nested
	class c_EdgeCases {

		@Test
		void c01_nullInputHandling() {
			Stringifier<String> nullSafe = (converter, str) -> {
				if (str == null) return "NULL_INPUT";
				return str;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = nullSafe.apply(converter, null);

			assertEquals("NULL_INPUT", result);
		}

		@Test
		void c02_emptyStringHandling() {
			Stringifier<String> emptyHandler = (converter, str) -> {
				if (str.isEmpty()) return "EMPTY_STRING";
				return str;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = emptyHandler.apply(converter, "");

			assertEquals("EMPTY_STRING", result);
		}

		@Test
		void c03_exceptionHandling() {
			Stringifier<String> throwing = (converter, str) -> {
				if ("ERROR".equals(str)) {
					throw new RuntimeException("Intentional test exception");
				}
				return str;
			};

			var converter = BasicBeanConverter.DEFAULT;

			// Normal case should work
			var normalResult = throwing.apply(converter, "normal");
			assertEquals("normal", normalResult);

			// Exception case should throw
			assertThrows(RuntimeException.class, () -> throwing.apply(converter, "ERROR"));
		}

		@Test
		void c04_specialCharacterHandling() {
			Stringifier<String> specialHandler = (converter, str) -> {
				return str.replace("\n", "\\n")
					.replace("\t", "\\t")
					.replace("\r", "\\r");
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = specialHandler.apply(converter, "line1\nline2\tcolumn");

			assertEquals("line1\\nline2\\tcolumn", result);
		}

		@Test
		void c05_unicodeHandling() {
			Stringifier<String> unicodeHandler = (converter, str) -> "UNICODE:" + str;

			var converter = BasicBeanConverter.DEFAULT;
			var result = unicodeHandler.apply(converter, "测试 🎉 ñoël");

			assertEquals("UNICODE:测试 🎉 ñoël", result);
		}
	}

	// ====================================================================================================
	// Type-Specific Tests
	// ====================================================================================================

	@Nested
	class d_TypeSpecific {

		@Test
		void d01_numberStringification() {
			Stringifier<Number> numberFormatter = (converter, num) -> {
				if (num instanceof Integer) return "INT:" + num;
				if (num instanceof Double) return "DOUBLE:" + String.format("%.2f", num);
				return "NUMBER:" + num;
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("INT:42", numberFormatter.apply(converter, 42));
			assertEquals("DOUBLE:3.14", numberFormatter.apply(converter, 3.14159));
			assertEquals("NUMBER:123", numberFormatter.apply(converter, 123L));
		}

		@Test
		void d02_collectionStringification() {
			Stringifier<Collection<?>> collectionFormatter = (converter, coll) -> {
				if (coll.isEmpty()) return "EMPTY_COLLECTION";
				return "COLLECTION[" + coll.size() + "]:" +
				coll.stream().map(Object::toString).reduce("", (a, b) -> a + "," + b);
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("EMPTY_COLLECTION", collectionFormatter.apply(converter, Arrays.asList()));
			assertEquals("COLLECTION[3]:,a,b,c", collectionFormatter.apply(converter, Arrays.asList("a", "b", "c")));
		}

		@Test
		void d03_booleanStringification() {
			Stringifier<Boolean> booleanFormatter = (converter, bool) -> bool ? "YES" : "NO";

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("YES", booleanFormatter.apply(converter, true));
			assertEquals("NO", booleanFormatter.apply(converter, false));
		}

		@Test
		void d04_customObjectStringification() {
			Stringifier<TestPerson> personFormatter = (converter, person) -> String.format("Person{name='%s', age=%d}", person.name, person.age);

			var converter = BasicBeanConverter.DEFAULT;
			var person = new TestPerson("Alice", 30);
			var result = personFormatter.apply(converter, person);

			assertEquals("Person{name='Alice', age=30}", result);
		}
	}

	// ====================================================================================================
	// Integration Tests
	// ====================================================================================================

	@Nested
	class e_Integration {

		@Test
		void e01_converterIntegration() {
			// Test integration with custom converter
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addStringifier(TestPerson.class, (converter, person) ->
				"CUSTOM:" + person.name + ":" + person.age)
				.build();

			var person = new TestPerson("Bob", 25);
			var result = customConverter.stringify(person);

			assertEquals("CUSTOM:Bob:25", result);
		}

		@Test
		void e02_multipleStringifierRegistration() {
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addStringifier(String.class, (converter, str) -> "STR:" + str)
				.addStringifier(Integer.class, (converter, num) -> "INT:" + num)
				.build();

			// Test string stringifier
			assertEquals("STR:test", customConverter.stringify("test"));

			// Test integer stringifier
			assertEquals("INT:42", customConverter.stringify(42));
		}

		@Test
		void e03_converterPassthrough() {
			// Test that converter parameter is properly passed
			Stringifier<List<?>> listStringifier = (converter, list) -> {
				// Use the converter parameter to stringify elements
				StringBuilder sb = new StringBuilder("[");
				for (int i = 0; i < list.size(); i++) {
					if (i > 0) sb.append(",");
					sb.append(converter.stringify(list.get(i)));
				}
				sb.append("]");
				return sb.toString();
			};

			var converter = BasicBeanConverter.DEFAULT;
			var testList = Arrays.asList("a", 42, true);
			var result = listStringifier.apply(converter, testList);

			assertEquals("[a,42,true]", result);
		}

		@Test
		void e04_nestedConverterCalls() {
			// Test stringifier that makes nested converter calls
			Stringifier<TestContainer> containerStringifier = (converter, container) -> {
				String itemsStr = converter.stringify(container.items);
				return "Container{items=" + itemsStr + ", count=" + container.items.size() + "}";
			};

			var converter = BasicBeanConverter.DEFAULT;
			var container = new TestContainer(Arrays.asList("x", "y", "z"));
			var result = containerStringifier.apply(converter, container);

			assertTrue(result.contains("Container{items="));
			assertTrue(result.contains("count=3"));
		}
	}

	// ====================================================================================================
	// Performance Tests
	// ====================================================================================================

	@Nested
	class f_Performance {

		@Test
		void f01_performanceWithLargeStrings() {
			Stringifier<String> largeStringHandler = (converter, str) -> {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < 1000; i++) {
					sb.append(str).append("_").append(i);
					if (i < 999) sb.append(",");
				}
				return sb.toString();
			};

			var converter = BasicBeanConverter.DEFAULT;

			var start = System.currentTimeMillis();
			var result = largeStringHandler.apply(converter, "test");
			var end = System.currentTimeMillis();

			assertTrue(result.length() > 8000, "Should generate a large string (actual: " + result.length() + ")");
			assertTrue(end - start < 1000, "Should complete within 1 second");
		}

		@Test
		void f02_memoryEfficiency() {
			Stringifier<Integer> memoryTest = (converter, num) -> {
				// Create a reasonably large string
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < num; i++) {
					sb.append("item_").append(i);
					if (i < num - 1) sb.append(",");
				}
				return sb.toString();
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = memoryTest.apply(converter, 100);

			assertTrue(result.startsWith("item_0"));
			assertTrue(result.endsWith("item_99"));
			assertTrue(result.contains(","));
		}
	}

	// ====================================================================================================
	// Helper Classes and Methods
	// ====================================================================================================

	static class StringifierMethods {
		static String addPrefix(BeanConverter converter, String str) {
			return "PREFIX:" + str;
		}
	}

	static class TestPerson {
		final String name;
		final int age;

		TestPerson(String name, int age) {
			this.name = name;
			this.age = age;
		}
	}

	static class TestContainer {
		final List<String> items;

		TestContainer(List<String> items) {
			this.items = items;
		}
	}
}
