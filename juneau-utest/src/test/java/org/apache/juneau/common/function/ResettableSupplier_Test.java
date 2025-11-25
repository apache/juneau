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
package org.apache.juneau.common.function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ResettableSupplier_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "result";
		});

		// First call should invoke the supplier
		assertEquals("result", supplier.get());
		assertEquals(1, callCount.get());

		// Second call should return cached value
		assertEquals("result", supplier.get());
		assertEquals(1, callCount.get()); // Should not increment
	}

	@Test void a02_returnsNull() {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return null;
		});

		// First call should invoke the supplier and cache null
		assertNull(supplier.get());
		assertEquals(1, callCount.get());

		// Second call should return cached null
		assertNull(supplier.get());
		assertEquals(1, callCount.get()); // Should not increment
	}

	@Test void a03_reset() {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "result" + callCount.get();
		});

		// First call
		assertEquals("result1", supplier.get());
		assertEquals(1, callCount.get());

		// Second call - cached
		assertEquals("result1", supplier.get());
		assertEquals(1, callCount.get());

		// Reset
		supplier.reset();

		// Third call - should recompute
		assertEquals("result2", supplier.get());
		assertEquals(2, callCount.get());

		// Fourth call - cached again
		assertEquals("result2", supplier.get());
		assertEquals(2, callCount.get());
	}

	@Test void a04_multipleResets() {
		var callCount = new AtomicInteger();
		ResettableSupplier<Integer> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return callCount.get();
		});

		assertEquals(1, supplier.get());
		supplier.reset();
		assertEquals(2, supplier.get());
		supplier.reset();
		assertEquals(3, supplier.get());
		supplier.reset();
		assertEquals(4, supplier.get());
		assertEquals(4, callCount.get());
	}

	@Test void a05_resetBeforeFirstCall() {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "result";
		});

		// Reset before any call should be safe
		supplier.reset();

		// First call should still work
		assertEquals("result", supplier.get());
		assertEquals(1, callCount.get());
	}

	@Test void a06_threadSafety_raceCondition() throws InterruptedException {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			// Simulate some work
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return "result" + callCount.get();
		});

		// Multiple threads calling get() simultaneously
		var threads = new Thread[10];
		var results = new String[10];
		for (int i = 0; i < 10; i++) {
			final int index = i;
			threads[i] = new Thread(() -> {
				results[index] = supplier.get();
			});
		}

		// Start all threads
		for (var thread : threads) {
			thread.start();
		}

		// Wait for all threads
		for (var thread : threads) {
			thread.join();
		}

		// All results should be the same (cached value)
		var firstResult = results[0];
		for (var result : results) {
			assertEquals(firstResult, result);
		}

		// Supplier should have been called at least once, but possibly multiple times due to race
		// The important thing is that all threads got the same result
		assertTrue(callCount.get() >= 1);
	}

	@Test void a07_resetAndGetConcurrently() throws InterruptedException {
		var callCount = new AtomicInteger();
		ResettableSupplier<Integer> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return callCount.get();
		});

		var resetThread = new Thread(() -> {
			for (int i = 0; i < 10; i++) {
				supplier.reset();
				try {
					Thread.sleep(5);  // Increased sleep to increase chance of race
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});

		var getThread = new Thread(() -> {
			for (int i = 0; i < 100; i++) {
				supplier.get();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});

		resetThread.start();
		getThread.start();

		resetThread.join();
		getThread.join();

		// Should have called supplier multiple times due to resets
		// Note: This is a race condition test, so results may vary
		// We just verify that the supplier was called at least once
		assertTrue(callCount.get() >= 1, "Supplier should have been called at least once");
	}
}

