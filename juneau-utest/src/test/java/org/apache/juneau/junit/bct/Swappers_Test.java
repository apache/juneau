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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link Swappers}.
 */
class Swappers_Test extends TestBase {

	@Nested
	class A_optionalSwapper extends TestBase {

		@Test
		void a01_swapPresentOptional() {
			var swapper = Swappers.optionalSwapper();
			var result = swapper.apply(null, Optional.of("Hello"));
			assertEquals("Hello", result);
		}

		@Test
		void a02_swapEmptyOptional() {
			var swapper = Swappers.optionalSwapper();
			var result = swapper.apply(null, Optional.empty());
			assertNull(result);
		}

		@Test
		void a03_swapOptionalWithNull() {
			var swapper = Swappers.optionalSwapper();
			var result = swapper.apply(null, Optional.ofNullable(null));
			assertNull(result);
		}

	@Test
	void a04_swapOptionalWithComplexObject() {
		var swapper = Swappers.optionalSwapper();
		var list = l("a", "b", "c");
		var result = swapper.apply(null, Optional.of(list));
		assertSame(list, result);
		}

		@Test
		void a05_swapOptionalWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var swapper = Swappers.optionalSwapper();
			var result = swapper.apply(converter, Optional.of(42));
			assertEquals(42, result);
		}
	}

	@Nested
	class B_supplierSwapper extends TestBase {

		@Test
		void b01_swapSupplierWithValue() {
			var swapper = Swappers.supplierSwapper();
			Supplier<String> supplier = () -> "Hello World";
			var result = swapper.apply(null, supplier);
			assertEquals("Hello World", result);
		}

		@Test
		void b02_swapSupplierWithNull() {
			var swapper = Swappers.supplierSwapper();
			Supplier<String> supplier = () -> null;
			var result = swapper.apply(null, supplier);
			assertNull(result);
		}

	@Test
	void b03_swapSupplierWithComplexObject() {
		var swapper = Swappers.supplierSwapper();
		var list = l("x", "y", "z");
		Supplier<List<String>> supplier = () -> list;
		var result = swapper.apply(null, supplier);
			assertSame(list, result);
		}

		@Test
		void b04_swapSupplierWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var swapper = Swappers.supplierSwapper();
			Supplier<Integer> supplier = () -> 123;
			var result = swapper.apply(converter, supplier);
			assertEquals(123, result);
		}

		@Test
		void b05_swapSupplierWithException() {
			var swapper = Swappers.supplierSwapper();
			Supplier<String> supplier = () -> {
				throw new RuntimeException("Test exception");
			};
			assertThrows(RuntimeException.class, () -> swapper.apply(null, supplier));
		}
	}

	@Nested
	class C_futureSwapper extends TestBase {

		@Test
		void c01_swapCompletedFuture() {
			var swapper = Swappers.futureSwapper();
			var future = CompletableFuture.completedFuture("Result");
			var result = swapper.apply(null, future);
			assertEquals("Result", result);
		}

		@Test
		void c02_swapPendingFuture() {
			var swapper = Swappers.futureSwapper();
			var future = new CompletableFuture<String>();
			var result = swapper.apply(null, future);
			assertEquals("<pending>", result);
		}

		@Test
		void c03_swapCancelledFuture() {
			var swapper = Swappers.futureSwapper();
			var future = new CompletableFuture<String>();
			future.cancel(true);
			var result = swapper.apply(null, future);
			assertEquals("<cancelled>", result);
		}

		@Test
		void c04_swapFailedFuture() {
			var swapper = Swappers.futureSwapper();
			var future = new CompletableFuture<String>();
			future.completeExceptionally(new RuntimeException("Test error"));
			var result = swapper.apply(null, future);
			assertEquals("<error: java.lang.RuntimeException: Test error>", result);
		}

		@Test
		void c05_swapCompletedFutureWithNull() {
			var swapper = Swappers.futureSwapper();
			var future = CompletableFuture.completedFuture(null);
			var result = swapper.apply(null, future);
			assertNull(result);
		}

	@Test
	void c06_swapFutureWithComplexObject() {
		var swapper = Swappers.futureSwapper();
		var list = l("a", "b", "c");
		var future = CompletableFuture.completedFuture(list);
		var result = swapper.apply(null, future);
			assertSame(list, result);
		}

		@Test
		void c07_swapFutureWithConverter() {
			var converter = BasicBeanConverter.builder().defaultSettings().build();
			var swapper = Swappers.futureSwapper();
			var future = CompletableFuture.completedFuture(999);
			var result = swapper.apply(converter, future);
			assertEquals(999, result);
		}

		@Test
		void c08_swapFutureWithNestedCause() {
			var swapper = Swappers.futureSwapper();
			var future = new CompletableFuture<String>();
			var rootCause = new IllegalArgumentException("Root cause");
			var wrappedException = new RuntimeException("Wrapper", rootCause);
			future.completeExceptionally(wrappedException);
			var result = swapper.apply(null, future);
			assertEquals("<error: java.lang.RuntimeException: Wrapper>", result);
		}
	}

	@Nested
	class D_integration extends TestBase {

		@Test
		void d01_useInBasicBeanConverter() {
			// Test Optional swapping
			assertBean(Optional.of("test"), "<self>", "test");
			assertBean(Optional.empty(), "<self>", "<null>");

			// Test Supplier swapping
			Supplier<String> supplier = () -> "supplied";
			assertBean(supplier, "<self>", "supplied");

			// Test Future swapping
			var future = CompletableFuture.completedFuture("future-result");
			assertBean(future, "<self>", "future-result");
		}

		@Test
		void d02_customSwapperRegistration() {
			// Test that custom registration works
			assertBean(Optional.of("custom"), "<self>", "custom");
		}

		@Test
		void d03_nestedSwapping() {
			// Test nested Optional in Supplier
			Supplier<Optional<String>> nestedSupplier = () -> Optional.of("nested");
			assertBean(nestedSupplier, "<self>", "nested");

			// Test Optional containing Supplier
			Supplier<String> supplier = () -> "inner";
			assertBean(Optional.of(supplier), "<self>", "inner");
		}
	}
}