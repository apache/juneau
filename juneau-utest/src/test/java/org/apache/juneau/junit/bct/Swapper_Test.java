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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link Swapper} functional interface.
 *
 * <p>This test class verifies functional interface compliance, lambda compatibility,
 * BiFunction inheritance, and integration with BasicBeanConverter.</p>
 */
class Swapper_Test extends TestBase {

	// ====================================================================================================
	// Functional Interface Compliance Tests
	// ====================================================================================================

	@Nested
	class A_functionalInterfaceCompliance extends TestBase {

		@SuppressWarnings("cast")
		@Test
		void a01_functionalInterfaceContract() {
			// Verify it's a proper functional interface
			Swapper<String> swapper = (converter, str) -> "SWAPPED:" + str;

			assertNotNull(swapper);
			assertTrue(swapper instanceof BiFunction);
			assertTrue(swapper instanceof Swapper);
		}

		@Test
		void a02_lambdaExpressionCompatibility() {
			// Test lambda expression usage
			Swapper<Integer> lambda = (converter, num) -> num * 2;

			var converter = BasicBeanConverter.DEFAULT;
			var result = lambda.apply(converter, 21);

			assertEquals(42, result);
		}

		@Test
		void a03_methodReferenceCompatibility() {
			// Test method reference usage
			Swapper<String> methodRef = SwapperMethods::addPrefix;

			var converter = BasicBeanConverter.DEFAULT;
			var result = methodRef.apply(converter, "test");

			assertEquals("PREFIX:test", result);
		}

		@Test
		void a04_biFunctionInheritance() {
			// Verify BiFunction methods are inherited
			Swapper<String> swapper = (converter, str) -> str.toUpperCase();

			// Test BiFunction.apply method
			var converter = BasicBeanConverter.DEFAULT;
			var result = swapper.apply(converter, "test");
			assertEquals("TEST", result);
		}
	}

	// ====================================================================================================
	// Lambda Composition Tests
	// ====================================================================================================

	@Nested
	class B_lambdaComposition extends TestBase {

		@Test
		void b01_andThenComposition() {
			Swapper<String> base = (converter, str) -> str.toLowerCase();
			Function<Object, String> postProcessor = obj -> "[" + obj.toString() + "]";

			var composed = base.andThen(postProcessor);

			var converter = BasicBeanConverter.DEFAULT;
			var result = composed.apply(converter, "TEST");

			assertEquals("[test]", result);
		}

		@Test
		void b02_chainedSwapping() {
			// Compose multiple swapping steps
			Swapper<String> upperCase = (converter, str) -> str.toUpperCase();
			Swapper<String> prefixed = (converter, str) -> "PROCESSED:" + str;

			var converter = BasicBeanConverter.DEFAULT;

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
	class C_edgeCases extends TestBase {

		@Test
		void c01_nullInputHandling() {
			Swapper<String> nullSafe = (converter, str) -> {
				if (str == null) return "NULL_INPUT";
				return str;
			};

			var converter = BasicBeanConverter.DEFAULT;
			var result = nullSafe.apply(converter, null);

			assertEquals("NULL_INPUT", result);
		}

		@Test
		void c02_nullOutputHandling() {
			Swapper<String> nullReturning = (converter, str) -> {
				if ("return_null".equals(str)) return null;
				return str;
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertNull(nullReturning.apply(converter, "return_null"));
			assertEquals("keep", nullReturning.apply(converter, "keep"));
		}

		@Test
		void c03_exceptionHandling() {
			Swapper<String> throwing = (converter, str) -> {
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
		void c04_typeTransformation() {
			Swapper<Number> numberSwapper = (converter, num) -> {
				if (num instanceof Integer) return num.doubleValue();
				if (num instanceof Double) return num.intValue();
				return num;
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals(42.0, numberSwapper.apply(converter, 42));
			assertEquals(3, numberSwapper.apply(converter, 3.14));
			assertEquals(123L, numberSwapper.apply(converter, 123L));
		}
	}

	// ====================================================================================================
	// Type-Specific Tests
	// ====================================================================================================

	@Nested
	class D_typeSpecific extends TestBase {

		@Test
		void d01_optionalSwapping() {
			Swapper<Optional<String>> optionalSwapper = (converter, opt) -> opt.orElse("EMPTY");

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("value", optionalSwapper.apply(converter, Optional.of("value")));
			assertEquals("EMPTY", optionalSwapper.apply(converter, Optional.empty()));
		}

		@Test
		void d02_collectionSwapping() {
			Swapper<Collection<?>> collectionSwapper = (converter, coll) -> {
				if (coll.isEmpty()) return "EMPTY_COLLECTION";
				return "COLLECTION[" + coll.size() + "]:" + coll.iterator().next();
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("EMPTY_COLLECTION", collectionSwapper.apply(converter, Arrays.asList()));
			assertEquals("COLLECTION[3]:a", collectionSwapper.apply(converter, Arrays.asList("a", "b", "c")));
		}

		@Test
		void d03_supplierSwapping() {
			Swapper<Supplier<?>> supplierSwapper = (converter, supplier) -> {
				try {
					return supplier.get();
				} catch (Exception e) {
					return "SUPPLIER_ERROR:" + e.getMessage();
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			assertEquals("success", supplierSwapper.apply(converter, () -> "success"));
			assertEquals("SUPPLIER_ERROR:test error",
				supplierSwapper.apply(converter, () -> { throw new RuntimeException("test error"); }));
		}

		@Test
		void d04_futureSwapping() {
			Swapper<CompletableFuture<?>> futureSwapper = (converter, future) -> {
				try {
					if (future.isDone()) {
						return future.get();
					} else {
						return "<pending>";
					}
				} catch (Exception e) {
					return "<error:" + e.getMessage() + ">";
				}
			};

			var converter = BasicBeanConverter.DEFAULT;

			// Test completed future
			var completedFuture = CompletableFuture.completedFuture("done");
			assertEquals("done", futureSwapper.apply(converter, completedFuture));

			// Test pending future
			var pendingFuture = new CompletableFuture<String>();
			assertEquals("<pending>", futureSwapper.apply(converter, pendingFuture));

			// Test failed future
			var failedFuture = new CompletableFuture<String>();
			failedFuture.completeExceptionally(new RuntimeException("failed"));
			var result = (String) futureSwapper.apply(converter, failedFuture);
			assertTrue(result.startsWith("<error:"));
			assertTrue(result.contains("failed"));
		}

		@Test
		void d05_customObjectSwapping() {
			Swapper<TestWrapper> wrapperSwapper = (converter, wrapper) -> wrapper.getValue();

			var converter = BasicBeanConverter.DEFAULT;
			var wrapper = new TestWrapper("inner_value");
			var result = wrapperSwapper.apply(converter, wrapper);

			assertEquals("inner_value", result);
		}
	}

	// ====================================================================================================
	// Integration Tests
	// ====================================================================================================

	@Nested
	class E_integration extends TestBase {

		@Test
		void e01_converterIntegration() {
			// Test integration with custom converter
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSwapper(TestWrapper.class, (converter, wrapper) -> wrapper.getValue())
				.build();

			var wrapper = new TestWrapper("swapped_content");
			var result = customConverter.stringify(wrapper);

			assertEquals("swapped_content", result);
		}

		@Test
		void e02_multipleSwapperRegistration() {
			var customConverter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSwapper(TestWrapper.class, (converter, wrapper) -> "WRAPPER:" + wrapper.getValue())
				.addSwapper(TestContainer.class, (converter, container) -> "CONTAINER:" + container.content)
				.build();

			// Test wrapper swapper
			var wrapper = new TestWrapper("test");
			assertEquals("WRAPPER:test", customConverter.stringify(wrapper));

			// Test container swapper
			var container = new TestContainer("content");
			assertEquals("CONTAINER:content", customConverter.stringify(container));
		}

		@Test
		void e03_converterPassthrough() {
			// Test that converter parameter is properly passed
			Swapper<TestContainer> containerSwapper = (converter, container) -> {
				// Use the converter parameter to stringify the content
				return "CONTAINER[" + converter.stringify(container.content) + "]";
			};

			var converter = BasicBeanConverter.DEFAULT;
			var container = new TestContainer("inner_content");
			var result = containerSwapper.apply(converter, container);

			assertEquals("CONTAINER[inner_content]", result);
		}

		@Test
		void e04_nestedConverterCalls() {
			// Test swapper that makes nested converter calls
			Swapper<TestComplexObject> complexSwapper = (converter, obj) -> {
				var itemsStr = converter.stringify(obj.items);
				return "COMPLEX{items=" + itemsStr + ", count=" + obj.items.size() + "}";
			};

			var converter = BasicBeanConverter.DEFAULT;
			var complex = new TestComplexObject(Arrays.asList("x", "y", "z"));
			var result = complexSwapper.apply(converter, complex);

			assertTrue(result.toString().contains("COMPLEX{items="));
			assertTrue(result.toString().contains("count=3"));
		}

		@Test
		void e05_swapperChaining() {
			// Test that swapped objects can be swapped again
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSwapper(TestWrapper.class, (conv, wrapper) -> new TestContainer(wrapper.getValue()))
				.addSwapper(TestContainer.class, (conv, container) -> "CHAINED:" + container.content)
				.build();

			var wrapper = new TestWrapper("hello");
			var result = converter.stringify(wrapper);

			// Should unwrap the wrapper, create a container, then process the container
			assertEquals("CHAINED:hello", result);
		}

		@Test
		void e06_swapperWithListification() {
			// Test swapper integration with listification
			var converter = BasicBeanConverter.builder()
				.defaultSettings()
				.addSwapper(TestContainer.class, (conv, container) -> Arrays.asList(container.content, "extra"))
				.build();

			var container = new TestContainer("test");
			var list = converter.listify(container);

			assertEquals(2, list.size());
			assertEquals("test", list.get(0));
			assertEquals("extra", list.get(1));
		}
	}

	// ====================================================================================================
	// Performance Tests
	// ====================================================================================================

	@Nested
	class F_performance extends TestBase {

		@Test
		void f01_simpleSwapperPerformance() {
			Swapper<String> simpleSwapper = (converter, str) -> str.toUpperCase();
			var converter = BasicBeanConverter.DEFAULT;

			// Test that simple swappers execute quickly
			var start = System.nanoTime();
			for (int i = 0; i < 1000; i++) {
				simpleSwapper.apply(converter, "test" + i);
			}
			var end = System.nanoTime();

			var durationMs = (end - start) / 1_000_000;
			assertTrue(durationMs < 100, "Simple swapper should execute quickly, took: " + durationMs + "ms");
		}

		@Test
		void f02_complexSwapperPerformance() {
			Swapper<List<String>> complexSwapper = (converter, list) -> {
				return list.stream()
					.map(String::toUpperCase)
					.sorted()
					.toList();
			};

			var converter = BasicBeanConverter.DEFAULT;
			var testList = Arrays.asList("z", "a", "m", "c", "x");

			var start = System.nanoTime();
			for (int i = 0; i < 100; i++) {
				complexSwapper.apply(converter, testList);
			}
			var end = System.nanoTime();

			var durationMs = (end - start) / 1_000_000;
			assertTrue(durationMs < 1000, "Complex swapper should complete reasonably quickly, took: " + durationMs + "ms");
		}
	}

	// ====================================================================================================
	// Helper Classes and Methods
	// ====================================================================================================

	static class SwapperMethods {
		static String addPrefix(BeanConverter converter, String str) {
			return "PREFIX:" + str;
		}
	}

	static class TestWrapper {
		final String value;

		TestWrapper(String value) {
			this.value = value;
		}

		String getValue() { return value; }
	}

	static class TestContainer {
		final String content;

		TestContainer(String content) {
			this.content = content;
		}
	}

	static class TestComplexObject {
		final List<String> items;

		TestComplexObject(List<String> items) {
			this.items = items;
		}
	}
}
