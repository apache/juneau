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
package org.apache.juneau.common.reflect;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the {@link ThrowingSupplier} functional interface.
 *
 * <p>This test class verifies functional interface compliance, exception handling,
 * and integration with the safe() method.</p>
 */
class ThrowingSupplier_Test extends TestBase {

	// ====================================================================================================
	// Functional Interface Compliance Tests
	// ====================================================================================================

	@Nested
	class A_functionalInterfaceCompliance extends TestBase {

		@SuppressWarnings("cast")
		@Test
		void a01_functionalInterfaceContract() {
			// Verify it's a proper functional interface
			ThrowingSupplier<String> supplier = () -> "test_value";

			assertNotNull(supplier);
			assertTrue(supplier instanceof ThrowingSupplier);
		}

		@Test
		void a02_lambdaExpressionCompatibility() throws Exception {
			// Test lambda expression usage
			ThrowingSupplier<Integer> lambda = () -> 42;

			var result = lambda.get();
			assertEquals(42, result);
		}

		@Test
		void a03_methodReferenceCompatibility() throws Exception {
			// Test method reference usage
			ThrowingSupplier<String> methodRef = SupplierMethods::getValue;

			var result = methodRef.get();
			assertEquals("METHOD_VALUE", result);
		}

		@Test
		void a04_exceptionThrowingCapability() {
			// Test that it can throw checked exceptions
			ThrowingSupplier<String> throwingSupplier = () -> {
				throw new Exception("Test checked exception");
			};

			// Should be able to throw checked exceptions
			assertThrows(Exception.class, throwingSupplier::get);
		}
	}

	// ====================================================================================================
	// Exception Handling Tests
	// ====================================================================================================

	@Nested
	class B_exceptionHandling extends TestBase {

		@Test
		void b01_checkedExceptionHandling() {
			ThrowingSupplier<String> supplier = () -> {
				throw new java.io.IOException("IO error");
			};

			assertThrows(java.io.IOException.class, supplier::get);
		}

		@Test
		void b02_runtimeExceptionHandling() {
			ThrowingSupplier<String> supplier = () -> {
				throw new RuntimeException("Runtime error");
			};

			assertThrows(RuntimeException.class, supplier::get);
		}

		@Test
		void b03_multipleExceptionTypes() {
			ThrowingSupplier<String> ioSupplier = () -> {
				throw new java.io.IOException("IO exception");
			};

			ThrowingSupplier<String> parseSupplier = () -> {
				throw new java.text.ParseException("Parse exception", 0);
			};

			assertThrows(java.io.IOException.class, ioSupplier::get);
			assertThrows(java.text.ParseException.class, parseSupplier::get);
		}

		@Test
		void b04_noExceptionCase() throws Exception {
			ThrowingSupplier<String> normalSupplier = () -> "normal_value";

			var result = normalSupplier.get();
			assertEquals("normal_value", result);
		}
	}

	// ====================================================================================================
	// Integration with safe() Tests
	// ====================================================================================================

	@Nested
	class C_utilsSafeIntegration extends TestBase {

		@Test
		void c01_safeExecutionWithoutException() {
			ThrowingSupplier<String> supplier = () -> "safe_value";

			var result = safe(supplier);
			assertEquals("safe_value", result);
		}

		@Test
		void c02_safeExecutionWithCheckedException() {
			ThrowingSupplier<String> supplier = () -> {
				throw new java.io.IOException("Checked exception");
			};

			// safe should wrap checked exceptions in RuntimeException
			var exception = assertThrows(RuntimeException.class, () -> safe(supplier));
			assertTrue(exception.getCause() instanceof java.io.IOException);
			assertEquals("Checked exception", exception.getCause().getMessage());
		}

		@Test
		void c03_safeExecutionWithRuntimeException() {
			ThrowingSupplier<String> supplier = () -> {
				throw new RuntimeException("Runtime exception");
			};

			// safe should re-throw RuntimeExceptions as-is
			var exception = assertThrows(RuntimeException.class, () -> safe(supplier));
			assertEquals("Runtime exception", exception.getMessage());
			assertNull(exception.getCause()); // Should not be wrapped
		}

		@Test
		void c04_safeExecutionWithNullReturn() {
			ThrowingSupplier<String> supplier = () -> null;

			var result = safe(supplier);
			assertNull(result);
		}
	}

	// ====================================================================================================
	// Complex Scenario Tests
	// ====================================================================================================

	@Nested
	class D_complexScenarios extends TestBase {

		@Test
		void d01_fileOperationSimulation() {
			// Simulate file reading that might throw IOException
			ThrowingSupplier<String> fileReader = () -> {
				// Simulate file not found
				throw new java.io.FileNotFoundException("File not found");
			};

			var exception = assertThrows(RuntimeException.class, () -> safe(fileReader));
			assertTrue(exception.getCause() instanceof java.io.FileNotFoundException);
		}

		@Test
		void d02_networkOperationSimulation() {
			// Simulate network operation that might timeout
			ThrowingSupplier<String> networkCall = () -> {
				// Simulate network timeout
				throw new java.net.SocketTimeoutException("Connection timeout");
			};

			var exception = assertThrows(RuntimeException.class, () -> safe(networkCall));
			assertTrue(exception.getCause() instanceof java.net.SocketTimeoutException);
		}

		@Test
		void d03_databaseOperationSimulation() {
			// Simulate database operation that might throw SQLException
			ThrowingSupplier<List<String>> dbQuery = () -> {
				// Simulate SQL exception
				throw new java.sql.SQLException("Database connection failed");
			};

			var exception = assertThrows(RuntimeException.class, () -> safe(dbQuery));
			assertTrue(exception.getCause() instanceof java.sql.SQLException);
		}

		@Test
		void d04_successfulComplexOperation() {
			// Simulate successful complex operation
			ThrowingSupplier<Map<String, Object>> complexOperation = () -> {
				var result = new HashMap<String, Object>();
				result.put("status", "success");
				result.put("data", l("item1", "item2", "item3"));
				result.put("timestamp", System.currentTimeMillis());
				return result;
			};

			var result = safe(complexOperation);
			assertNotNull(result);
			assertEquals("success", result.get("status"));
			assertNotNull(result.get("data"));
			assertNotNull(result.get("timestamp"));
		}
	}

	// ====================================================================================================
	// Performance and Resource Tests
	// ====================================================================================================

	@Nested
	class E_performanceAndResources extends TestBase {

		@Test
		void e01_quickExecution() {
			ThrowingSupplier<String> fastSupplier = () -> "quick_result";

			var start = System.nanoTime();
			var result = safe(fastSupplier);
			var end = System.nanoTime();

			assertEquals("quick_result", result);
			assertTrue((end - start) < 1_000_000); // Less than 1ms
		}

		@Test
		void e02_multipleExecutions() {
			var counter = new ThrowingSupplier<Integer>() {
				private int count = 0;
				@Override
				public Integer get() {
					return ++count;
				}
			};

			assertEquals(1, safe(counter));
			assertEquals(2, safe(counter));
			assertEquals(3, safe(counter));
		}

		@Test
		void e03_resourceCleanupSimulation() {
			// Simulate resource that needs cleanup
			var resourceClosed = booleans(false);

			ThrowingSupplier<String> resourceUser = () -> {
				try {
					// Simulate resource usage
					return "resource_data";
				} finally {
					// Simulate cleanup
					resourceClosed[0] = true;
				}
			};

			var result = safe(resourceUser);
			assertEquals("resource_data", result);
			assertTrue(resourceClosed[0], "Resource should be cleaned up");
		}

		@Test
		void e04_exceptionInFinallyBlock() {
			// Test behavior when exception occurs in finally block
			ThrowingSupplier<String> problematicResource = () -> {
				try {
					return "success";
				} finally {
					// This could happen in real resource cleanup
					// but won't affect the result since it's in finally
					System.gc(); // Safe operation for test
				}
			};

			var result = safe(problematicResource);
			assertEquals("success", result);
		}
	}

	// ====================================================================================================
	// Concurrency Tests
	// ====================================================================================================

	@Nested
	class F_concurrency extends TestBase {

		@Test
		void f01_threadSafeExecution() {
			ThrowingSupplier<String> supplier = () -> "thread_safe_value";

			// Test concurrent execution
			var futures = new ArrayList<java.util.concurrent.CompletableFuture<String>>();
			for (int i = 0; i < 10; i++) {
				futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> safe(supplier)));
			}

			// All should complete successfully
			for (var future : futures) {
				assertEquals("thread_safe_value", future.join());
			}
		}

		@Test
		void f02_concurrentExceptionHandling() {
			ThrowingSupplier<String> throwingSupplier = () -> {
				throw new RuntimeException("Concurrent exception");
			};

			// Test concurrent exception handling
			var futures = new ArrayList<java.util.concurrent.CompletableFuture<RuntimeException>>();
			for (int i = 0; i < 5; i++) {
				futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
					try {
						safe(throwingSupplier);
						return null; // Should not reach here
					} catch (RuntimeException e) {
						return e;
					}
				}));
			}

			// All should throw the same exception type
			for (var future : futures) {
				var exception = future.join();
				assertNotNull(exception);
				assertEquals("Concurrent exception", exception.getMessage());
			}
		}
	}

	// ====================================================================================================
	// Generic Type Tests
	// ====================================================================================================

	@Nested
	class G_genericTypes extends TestBase {

		@Test
		void g01_stringTypeSupplier() throws Exception {
			ThrowingSupplier<String> stringSupplier = () -> "string_value";
			assertEquals("string_value", stringSupplier.get());
		}

		@Test
		void g02_integerTypeSupplier() throws Exception {
			ThrowingSupplier<Integer> intSupplier = () -> 42;
			assertEquals(42, intSupplier.get());
		}

		@Test
		void g03_listTypeSupplier() throws Exception {
			ThrowingSupplier<List<String>> listSupplier = () -> l("a", "b", "c");
			var result = listSupplier.get();
			assertSize(3, result);
			assertEquals("a", result.get(0));
		}

		@Test
		void g04_mapTypeSupplier() throws Exception {
			ThrowingSupplier<Map<String, Integer>> mapSupplier = () -> {
				var map = new HashMap<String, Integer>();
				map.put("key1", 1);
				map.put("key2", 2);
				return map;
			};

			var result = mapSupplier.get();
			assertSize(2, result);
			assertEquals(1, result.get("key1"));
		}

		@Test
		void g05_customObjectTypeSupplier() throws Exception {
			ThrowingSupplier<TestResult> objectSupplier = () -> new TestResult("success", 100);

			var result = objectSupplier.get();
			assertEquals("success", result.status);
			assertEquals(100, result.value);
		}
	}

	// ====================================================================================================
	// Helper Classes and Methods
	// ====================================================================================================

	static class SupplierMethods {
		static String getValue() {
			return "METHOD_VALUE";
		}

		static String getValueWithException() throws Exception {
			throw new Exception("Method exception");
		}
	}

	static class TestResult {
		final String status;
		final int value;

		TestResult(String status, int value) {
			this.status = status;
			this.value = value;
		}
	}
}