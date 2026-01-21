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
package org.apache.juneau.commons.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link OptionalReference}.
 */
class OptionalReference_Test extends TestBase {

	//====================================================================================================
	// Static factory methods
	//====================================================================================================

	@Test
	void a01_empty() {
		var ref = OptionalReference.<String>empty();
		assertNotNull(ref);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
		assertFalse(ref.isPresent());
	}

	@Test
	void a02_of_withValue() {
		var ref = OptionalReference.of("test");
		assertNotNull(ref);
		assertEquals("test", ref.get());
		assertTrue(ref.isPresent());
		assertFalse(ref.isEmpty());
	}

	@Test
	void a03_of_withNull() {
		var ref = OptionalReference.of(null);
		assertNotNull(ref);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
		assertFalse(ref.isPresent());
	}

	@Test
	void a04_ofNullable_withValue() {
		var ref = OptionalReference.ofNullable("test");
		assertNotNull(ref);
		assertEquals("test", ref.get());
		assertTrue(ref.isPresent());
	}

	@Test
	void a05_ofNullable_withNull() {
		var ref = OptionalReference.ofNullable(null);
		assertNotNull(ref);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
	}

	//====================================================================================================
	// Constructors
	//====================================================================================================

	@Test
	void b01_constructor_default() {
		var ref = new OptionalReference<String>();
		assertNotNull(ref);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
	}

	@Test
	void b02_constructor_withInitialValue() {
		var ref = new OptionalReference<>("initial");
		assertNotNull(ref);
		assertEquals("initial", ref.get());
		assertTrue(ref.isPresent());
	}

	@Test
	void b03_constructor_withNull() {
		var ref = new OptionalReference<String>(null);
		assertNotNull(ref);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
	}

	//====================================================================================================
	// isPresent() and isEmpty()
	//====================================================================================================

	@Test
	void c01_isPresent_withValue() {
		var ref = OptionalReference.of("test");
		assertTrue(ref.isPresent());
	}

	@Test
	void c02_isPresent_withNull() {
		var ref = OptionalReference.<String>empty();
		assertFalse(ref.isPresent());
	}

	@Test
	void c03_isEmpty_withValue() {
		var ref = OptionalReference.of("test");
		assertFalse(ref.isEmpty());
	}

	@Test
	void c04_isEmpty_withNull() {
		var ref = OptionalReference.<String>empty();
		assertTrue(ref.isEmpty());
	}

	//====================================================================================================
	// map()
	//====================================================================================================

	@Test
	void d01_map_withValue() {
		var ref = OptionalReference.of("test");
		var mapped = ref.map(String::toUpperCase);
		assertNotNull(mapped);
		assertEquals("TEST", mapped.get());
		assertTrue(mapped.isPresent());
	}

	@Test
	void d02_map_withNull() {
		var ref = OptionalReference.<String>empty();
		var mapped = ref.map(String::toUpperCase);
		assertNotNull(mapped);
		assertTrue(mapped.isEmpty());
		assertNull(mapped.get());
	}

	@Test
	void d03_map_returnsNull() {
		var ref = OptionalReference.of("test");
		var mapped = ref.map(s -> (String)null);
		assertNotNull(mapped);
		assertTrue(mapped.isEmpty());
		assertNull(mapped.get());
	}

	@Test
	void d04_map_chaining() {
		var ref = OptionalReference.of(5);
		var result = ref.map(i -> i * 2).map(i -> i + 1).map(Object::toString);
		assertEquals("11", result.get());
		assertTrue(result.isPresent());
	}

	//====================================================================================================
	// flatMap()
	//====================================================================================================

	@Test
	void e01_flatMap_withValue() {
		var ref = OptionalReference.of("test");
		var flatMapped = ref.flatMap(s -> OptionalReference.of(s.toUpperCase()));
		assertNotNull(flatMapped);
		assertEquals("TEST", flatMapped.get());
		assertTrue(flatMapped.isPresent());
	}

	@Test
	void e02_flatMap_withNull() {
		var ref = OptionalReference.<String>empty();
		var flatMapped = ref.flatMap(s -> OptionalReference.of(s.toUpperCase()));
		assertNotNull(flatMapped);
		assertTrue(flatMapped.isEmpty());
	}

	@Test
	void e03_flatMap_returnsEmpty() {
		var ref = OptionalReference.of("test");
		var flatMapped = ref.flatMap(s -> OptionalReference.<String>empty());
		assertNotNull(flatMapped);
		assertTrue(flatMapped.isEmpty());
	}

	@Test
	void e04_flatMap_returnsNull() {
		var ref = OptionalReference.of("test");
		var flatMapped = ref.flatMap(s -> null);
		assertNotNull(flatMapped);
		assertTrue(flatMapped.isEmpty());
	}

	//====================================================================================================
	// filter()
	//====================================================================================================

	@Test
	void f01_filter_matches() {
		var ref = OptionalReference.of(5);
		var filtered = ref.filter(i -> i > 0);
		assertNotNull(filtered);
		assertEquals(5, filtered.get());
		assertTrue(filtered.isPresent());
	}

	@Test
	void f02_filter_doesNotMatch() {
		var ref = OptionalReference.of(5);
		var filtered = ref.filter(i -> i < 0);
		assertNotNull(filtered);
		assertTrue(filtered.isEmpty());
	}

	@Test
	void f03_filter_withNull() {
		var ref = OptionalReference.<Integer>empty();
		var filtered = ref.filter(i -> i > 0);
		assertNotNull(filtered);
		assertTrue(filtered.isEmpty());
	}

	//====================================================================================================
	// orElse()
	//====================================================================================================

	@Test
	void g01_orElse_withValue() {
		var ref = OptionalReference.of("test");
		var result = ref.orElse("default");
		assertEquals("test", result);
	}

	@Test
	void g02_orElse_withNull() {
		var ref = OptionalReference.<String>empty();
		var result = ref.orElse("default");
		assertEquals("default", result);
	}

	@Test
	void g03_orElse_withNullDefault() {
		var ref = OptionalReference.<String>empty();
		var result = ref.orElse(null);
		assertNull(result);
	}

	//====================================================================================================
	// orElseGet()
	//====================================================================================================

	@Test
	void h01_orElseGet_withValue() {
		var ref = OptionalReference.of("test");
		var result = ref.orElseGet(() -> "default");
		assertEquals("test", result);
	}

	@Test
	void h02_orElseGet_withNull() {
		var ref = OptionalReference.<String>empty();
		var result = ref.orElseGet(() -> "default");
		assertEquals("default", result);
	}

	@Test
	void h03_orElseGet_supplierNotCalledWhenPresent() {
		var ref = OptionalReference.of("test");
		var callCount = new AtomicInteger(0);
		var result = ref.orElseGet(() -> {
			callCount.incrementAndGet();
			return "default";
		});
		assertEquals("test", result);
		assertEquals(0, callCount.get());
	}

	//====================================================================================================
	// orElseThrow()
	//====================================================================================================

	@Test
	void i01_orElseThrow_withValue() {
		var ref = OptionalReference.of("test");
		var result = ref.orElseThrow(() -> new RuntimeException("should not throw"));
		assertEquals("test", result);
	}

	@Test
	void i02_orElseThrow_withNull() {
		var ref = OptionalReference.<String>empty();
		assertThrows(RuntimeException.class, () -> ref.orElseThrow(() -> new RuntimeException("expected")));
	}

	@Test
	void i03_orElseThrow_withNull_correctMessage() {
		var ref = OptionalReference.<String>empty();
		var exception = assertThrows(RuntimeException.class, () -> ref.orElseThrow(() -> new RuntimeException("expected message")));
		assertEquals("expected message", exception.getMessage());
	}

	//====================================================================================================
	// ifPresent()
	//====================================================================================================

	@Test
	void j01_ifPresent_withValue() {
		var ref = OptionalReference.of("test");
		var called = new boolean[]{false};
		ref.ifPresent(v -> called[0] = true);
		assertTrue(called[0]);
	}

	@Test
	void j02_ifPresent_withNull() {
		var ref = OptionalReference.<String>empty();
		var called = new boolean[]{false};
		ref.ifPresent(v -> called[0] = true);
		assertFalse(called[0]);
	}

	@Test
	void j03_ifPresent_receivesCorrectValue() {
		var ref = OptionalReference.of("test");
		var received = new String[1];
		ref.ifPresent(v -> received[0] = v);
		assertEquals("test", received[0]);
	}

	//====================================================================================================
	// ifPresentOrElse()
	//====================================================================================================

	@Test
	void k01_ifPresentOrElse_withValue() {
		var ref = OptionalReference.of("test");
		var presentCalled = new boolean[]{false};
		var emptyCalled = new boolean[]{false};
		ref.ifPresentOrElse(
			v -> presentCalled[0] = true,
			() -> emptyCalled[0] = true
		);
		assertTrue(presentCalled[0]);
		assertFalse(emptyCalled[0]);
	}

	@Test
	void k02_ifPresentOrElse_withNull() {
		var ref = OptionalReference.<String>empty();
		var presentCalled = new boolean[]{false};
		var emptyCalled = new boolean[]{false};
		ref.ifPresentOrElse(
			v -> presentCalled[0] = true,
			() -> emptyCalled[0] = true
		);
		assertFalse(presentCalled[0]);
		assertTrue(emptyCalled[0]);
	}

	//====================================================================================================
	// toOptional()
	//====================================================================================================

	@Test
	void l01_toOptional_withValue() {
		var ref = OptionalReference.of("test");
		var optional = ref.toOptional();
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals("test", optional.get());
	}

	@Test
	void l02_toOptional_withNull() {
		var ref = OptionalReference.<String>empty();
		var optional = ref.toOptional();
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}

	//====================================================================================================
	// AtomicReference operations
	//====================================================================================================

	@Test
	void m01_set() {
		var ref = OptionalReference.<String>empty();
		ref.set("new");
		assertEquals("new", ref.get());
		assertTrue(ref.isPresent());
	}

	@Test
	void m02_set_null() {
		var ref = OptionalReference.of("test");
		ref.set(null);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
	}

	@Test
	void m03_getAndSet() {
		var ref = OptionalReference.of("old");
		var oldValue = ref.getAndSet("new");
		assertEquals("old", oldValue);
		assertEquals("new", ref.get());
	}

	@Test
	void m04_compareAndSet_success() {
		var ref = OptionalReference.of("old");
		var success = ref.compareAndSet("old", "new");
		assertTrue(success);
		assertEquals("new", ref.get());
	}

	@Test
	void m05_compareAndSet_failure() {
		var ref = OptionalReference.of("old");
		var success = ref.compareAndSet("wrong", "new");
		assertFalse(success);
		assertEquals("old", ref.get());
	}

	@Test
	void m06_compareAndSet_null() {
		var ref = OptionalReference.of("old");
		var success = ref.compareAndSet("old", null);
		assertTrue(success);
		assertNull(ref.get());
		assertTrue(ref.isEmpty());
	}

	@Test
	void m07_compareAndSet_fromNull() {
		var ref = OptionalReference.<String>empty();
		var success = ref.compareAndSet(null, "new");
		assertTrue(success);
		assertEquals("new", ref.get());
		assertTrue(ref.isPresent());
	}

	@Test
	void m08_updateAndGet() {
		var ref = OptionalReference.of(5);
		var result = ref.updateAndGet(i -> i * 2);
		assertEquals(10, result);
		assertEquals(10, ref.get());
	}

	@Test
	void m09_getAndUpdate() {
		var ref = OptionalReference.of(5);
		var result = ref.getAndUpdate(i -> i * 2);
		assertEquals(5, result);
		assertEquals(10, ref.get());
	}

	@Test
	void m10_accumulateAndGet() {
		var ref = OptionalReference.of(5);
		var result = ref.accumulateAndGet(3, (x, y) -> x + y);
		assertEquals(8, result);
		assertEquals(8, ref.get());
	}

	@Test
	void m11_getAndAccumulate() {
		var ref = OptionalReference.of(5);
		var result = ref.getAndAccumulate(3, (x, y) -> x + y);
		assertEquals(5, result);
		assertEquals(8, ref.get());
	}

	//====================================================================================================
	// Thread safety tests
	//====================================================================================================

	@Test
	void n01_concurrentSet() throws InterruptedException {
		var ref = OptionalReference.<Integer>empty();
		var threadCount = 10;
		var iterations = 1000;
		var threads = new Thread[threadCount];
		var exceptions = new ConcurrentLinkedQueue<Exception>();

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			threads[i] = new Thread(() -> {
				try {
					for (int j = 0; j < iterations; j++) {
						ref.set(threadId * iterations + j);
						var value = ref.get();
						assertNotNull(value);
					}
				} catch (Exception e) {
					exceptions.add(e);
				}
			});
		}

		for (var thread : threads) {
			thread.start();
		}

		for (var thread : threads) {
			thread.join();
		}

		assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
		assertNotNull(ref.get());
	}

	@Test
	void n02_concurrentCompareAndSet() throws InterruptedException {
		// Test compareAndSet with concurrent access
		// AtomicReference.compareAndSet uses reference equality (==), not value equality
		// This test verifies that compareAndSet works correctly when multiple threads try to update
		var ref = OptionalReference.of("initial");
		var threadCount = 10;
		var iterations = 100;
		var threads = new Thread[threadCount];
		var successCount = new AtomicInteger(0);
		var targetValue = "target";

		for (int i = 0; i < threadCount; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < iterations; j++) {
					// Try to change from "initial" to "target"
					if (ref.compareAndSet("initial", targetValue)) {
						successCount.incrementAndGet();
					}
					// Try to change back from "target" to "initial"
					if (ref.compareAndSet(targetValue, "initial")) {
						successCount.incrementAndGet();
					}
				}
			});
		}

		for (var thread : threads) {
			thread.start();
		}

		for (var thread : threads) {
			thread.join();
		}

		// Verify that compareAndSet was called successfully many times
		// (exact count depends on timing, but should be significant)
		assertTrue(successCount.get() > 0, "compareAndSet should have succeeded at least once");
		// Final value should be either "initial" or "target"
		var finalValue = ref.get();
		assertTrue("initial".equals(finalValue) || targetValue.equals(finalValue),
			"Final value should be 'initial' or 'target', but was: " + finalValue);
	}

	@Test
	void n03_concurrentMap() throws InterruptedException {
		var ref = OptionalReference.of(0);
		var threadCount = 5;
		var iterations = 100;
		var threads = new Thread[threadCount];

		for (int i = 0; i < threadCount; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < iterations; j++) {
					ref.getAndUpdate(x -> x + 1);
				}
			});
		}

		for (var thread : threads) {
			thread.start();
		}

		for (var thread : threads) {
			thread.join();
		}

		assertTrue(ref.get() == 500);
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================

	@Test
	void o01_chainingOperations() {
		var ref = OptionalReference.of(5);
		var result = ref
			.map(i -> i * 2)
			.filter(i -> i > 5)
			.map(Object::toString)
			.orElse("default");
		assertEquals("10", result);
	}

	@Test
	void o02_chainingOperations_empty() {
		var ref = OptionalReference.of(5);
		var result = ref
			.map(i -> i * 2)
			.filter(i -> i > 20)
			.map(Object::toString)
			.orElse("default");
		assertEquals("default", result);
	}

	@Test
	void o03_optionalLikeUsage() {
		var ref = OptionalReference.<String>empty();
		var result = ref
			.map(String::toUpperCase)
			.orElseGet(() -> "DEFAULT");
		assertEquals("DEFAULT", result);
	}

	@Test
	void o04_atomicUpdateWithOptionalMethods() {
		var ref = OptionalReference.of(10);
		ref.updateAndGet(i -> i * 2);
		var result = ref
			.filter(i -> i > 15)
			.map(Object::toString)
			.orElse("too small");
		assertEquals("20", result);
	}
}
