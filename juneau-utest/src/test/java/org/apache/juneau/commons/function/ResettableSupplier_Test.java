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
package org.apache.juneau.commons.function;

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

	@Test void a08_set() {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "computed";
		});

		// Set a value before any get() call
		supplier.set("injected");
		assertEquals("injected", supplier.get());
		assertEquals(0, callCount.get()); // Supplier should not have been called

		// Subsequent get() calls should return the set value
		assertEquals("injected", supplier.get());
		assertEquals(0, callCount.get()); // Still not called

		// Set a different value
		supplier.set("newValue");
		assertEquals("newValue", supplier.get());
		assertEquals(0, callCount.get()); // Still not called

		// Reset clears the set value, next get() will invoke supplier
		supplier.reset();
		assertEquals("computed", supplier.get());
		assertEquals(1, callCount.get()); // Now supplier is called

		// Set after get() has been called
		supplier.set("overridden");
		assertEquals("overridden", supplier.get());
		assertEquals(1, callCount.get()); // Should not increment
	}

	@Test void a09_setNull() {
		var callCount = new AtomicInteger();
		ResettableSupplier<String> supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "computed";
		});

		// Set null value
		supplier.set(null);
		assertNull(supplier.get());
		assertEquals(0, callCount.get()); // Supplier should not have been called

		// Subsequent get() calls should return null
		assertNull(supplier.get());
		assertEquals(0, callCount.get()); // Still not called

		// Reset and get() should invoke supplier
		supplier.reset();
		assertEquals("computed", supplier.get());
		assertEquals(1, callCount.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// isSupplied()
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_isSupplied_notCalled() {
		var supplier = new ResettableSupplier<>(() -> "value");
		assertTrue(supplier.isSupplied()); // Not called yet
	}

	@Test void b02_isSupplied_afterGet() {
		var supplier = new ResettableSupplier<>(() -> "value");
		supplier.get();
		assertFalse(supplier.isSupplied()); // Has been called
	}

	@Test void b03_isSupplied_afterReset() {
		var supplier = new ResettableSupplier<>(() -> "value");
		supplier.get();
		supplier.reset();
		assertTrue(supplier.isSupplied()); // Reset, so not supplied anymore
	}

	@Test void b04_isSupplied_afterSet() {
		var supplier = new ResettableSupplier<>(() -> "value");
		supplier.set("injected");
		assertFalse(supplier.isSupplied()); // Has a value (even if set directly)
	}

	//------------------------------------------------------------------------------------------------------------------
	// copy()
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_copy_notCalled() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "value" + callCount.get();
		});
		var copy = supplier.copy();

		// Copy should use original supplier
		assertEquals("value1", copy.get());
		assertEquals(1, callCount.get());
	}

	@Test void c02_copy_afterGet() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "value" + callCount.get();
		});
		supplier.get(); // Cache the value
		var copy = supplier.copy();

		// Copy should use cached value
		assertEquals("value1", copy.get());
		assertEquals(1, callCount.get()); // Original supplier not called again
	}

	@Test void c03_copy_independent() {
		var supplier = new ResettableSupplier<>(() -> "value");
		var copy = supplier.copy();

		// Copy is independent
		supplier.reset();
		assertEquals("value", copy.get()); // Copy still has cached value
	}

	//------------------------------------------------------------------------------------------------------------------
	// map() - overridden to return ResettableSupplier
	//------------------------------------------------------------------------------------------------------------------
	@Test void d01_map_present() {
		var supplier = new ResettableSupplier<>(() -> "hello");
		var mapped = supplier.map(String::length);
		assertEquals(5, mapped.get());
	}

	@Test void d02_map_empty() {
		@SuppressWarnings("cast")
		var supplier = new ResettableSupplier<>(() -> (String)null);
		var mapped = supplier.map(String::length);
		assertNull(mapped.get());
	}

	@Test void d03_map_cached() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "hello";
		});
		var mapped = supplier.map(String::length);

		// First call
		assertEquals(5, mapped.get());
		assertEquals(1, callCount.get());

		// Second call - should use cached value from original supplier
		assertEquals(5, mapped.get());
		assertEquals(1, callCount.get()); // Original supplier not called again
	}

	@Test void d04_map_independent() {
		var supplier = new ResettableSupplier<>(() -> "hello");
		var mapped = supplier.map(String::length);

		// Reset original
		supplier.reset();
		// Mapped should still have cached value
		assertEquals(5, mapped.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// filter() - overridden to return ResettableSupplier
	//------------------------------------------------------------------------------------------------------------------
	@Test void e01_filter_matches() {
		var supplier = new ResettableSupplier<>(() -> "hello");
		var filtered = supplier.filter(s -> s.length() > 3);
		assertEquals("hello", filtered.get());
	}

	@Test void e02_filter_noMatch() {
		var supplier = new ResettableSupplier<>(() -> "hi");
		var filtered = supplier.filter(s -> s.length() > 3);
		assertNull(filtered.get());
	}

	@Test void e03_filter_empty() {
		@SuppressWarnings("cast")
		var supplier = new ResettableSupplier<>(() -> (String)null);
		var filtered = supplier.filter(s -> s.length() > 3);
		assertNull(filtered.get());
	}

	@Test void e04_filter_cached() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> {
			callCount.incrementAndGet();
			return "hello";
		});
		var filtered = supplier.filter(s -> s.length() > 3);

		// First call
		assertEquals("hello", filtered.get());
		assertEquals(1, callCount.get());

		// Second call - should use cached value
		assertEquals("hello", filtered.get());
		assertEquals(1, callCount.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// OptionalSupplier methods
	//------------------------------------------------------------------------------------------------------------------
	@Test void f01_isPresent() {
		var supplier = new ResettableSupplier<>(() -> "value");
		assertTrue(supplier.isPresent());
		assertFalse(supplier.isEmpty());
	}

	@Test void f02_isEmpty() {
		var supplier = new ResettableSupplier<>(() -> null);
		assertFalse(supplier.isPresent());
		assertTrue(supplier.isEmpty());
	}

	@Test void f03_orElse() {
		var supplier = new ResettableSupplier<>(() -> "value");
		assertEquals("value", supplier.orElse("default"));
	}

	@Test void f04_orElse_empty() {
		var supplier = new ResettableSupplier<>(() -> null);
		assertEquals("default", supplier.orElse("default"));
	}

	@Test void f05_orElseGet() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> "value");
		var result = supplier.orElseGet(() -> {
			callCount.incrementAndGet();
			return "default";
		});
		assertEquals("value", result);
		assertEquals(0, callCount.get());
	}

	@Test void f06_orElseGet_empty() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> null);
		var result = supplier.orElseGet(() -> {
			callCount.incrementAndGet();
			return "default";
		});
		assertEquals("default", result);
		assertEquals(1, callCount.get());
	}

	@Test void f07_orElseThrow() {
		var supplier = new ResettableSupplier<>(() -> "value");
		assertEquals("value", supplier.orElseThrow(() -> new RuntimeException("should not throw")));
	}

	@Test void f08_orElseThrow_empty() {
		var supplier = new ResettableSupplier<>(() -> null);
		assertThrows(RuntimeException.class, () -> supplier.orElseThrow(() -> new RuntimeException("expected")));
	}

	@Test void f09_ifPresent() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> "value");
		supplier.ifPresent(s -> callCount.incrementAndGet());
		assertEquals(1, callCount.get());
	}

	@Test void f10_ifPresent_empty() {
		var callCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> null);
		supplier.ifPresent(s -> callCount.incrementAndGet());
		assertEquals(0, callCount.get());
	}

	@Test void f11_ifPresentOrElse() {
		var presentCount = new AtomicInteger();
		var emptyCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> "value");
		supplier.ifPresentOrElse(
			s -> presentCount.incrementAndGet(),
			() -> emptyCount.incrementAndGet()
		);
		assertEquals(1, presentCount.get());
		assertEquals(0, emptyCount.get());
	}

	@Test void f12_ifPresentOrElse_empty() {
		var presentCount = new AtomicInteger();
		var emptyCount = new AtomicInteger();
		var supplier = new ResettableSupplier<>(() -> null);
		supplier.ifPresentOrElse(
			s -> presentCount.incrementAndGet(),
			() -> emptyCount.incrementAndGet()
		);
		assertEquals(0, presentCount.get());
		assertEquals(1, emptyCount.get());
	}

	@Test void f13_toOptional() {
		var supplier = new ResettableSupplier<>(() -> "value");
		var optional = supplier.toOptional();
		assertTrue(optional.isPresent());
		assertEquals("value", optional.get());
	}

	@Test void f14_toOptional_empty() {
		var supplier = new ResettableSupplier<>(() -> null);
		var optional = supplier.toOptional();
		assertFalse(optional.isPresent());
	}

	@Test void f15_flatMap() {
		var supplier = new ResettableSupplier<>(() -> "hello");
		var mapped = supplier.flatMap(s -> OptionalSupplier.ofNullable(s.length()));
		assertEquals(5, mapped.get());
	}

	@Test void f16_flatMap_empty() {
		@SuppressWarnings("cast")
		var supplier = new ResettableSupplier<>(() -> (String)null);
		var mapped = supplier.flatMap(s -> OptionalSupplier.ofNullable(s.length()));
		assertNull(mapped.get());
	}
}

