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
package org.apache.juneau.commons.conversion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ConfigurableConverter}.
 */
public class ConfigurableConverter_Test {

	// =================================================================================================================
	// a - Registration and basic conversion
	// =================================================================================================================

	/**
	 * Custom value type used in registration tests.
	 */
	public static class A01_Value {
		public final String raw;
		public A01_Value(String raw) { this.raw = raw; }
	}

	@Test void a01_registeredConversionIsUsed() {
		var c = new ConfigurableConverter()
			.add(String.class, A01_Value.class, (in, memberOf, args) -> new A01_Value(in));
		var result = c.to("hello", A01_Value.class);
		assertNotNull(result);
		assertEquals("hello", result.raw);
	}

	@Test void a02_registeredConversionTakesPriorityOverBuiltIn() {
		// Integer→String has a built-in conversion; register a custom one that wraps with brackets
		var c = new ConfigurableConverter()
			.add(Integer.class, String.class, (in, memberOf, args) -> "[" + in + "]");
		assertEquals("[42]", c.to(42, String.class));
	}

	@Test void a03_builtInConversionUsedWhenNoRegistration() {
		var c = new ConfigurableConverter();
		// String→Integer has a built-in conversion
		assertEquals(Integer.valueOf(42), c.to("42", Integer.class));
	}

	@Test void a04_nullInputReturnsNull() {
		var c = new ConfigurableConverter()
			.add(String.class, A01_Value.class, (in, memberOf, args) -> new A01_Value(in));
		assertNull(c.to(null, A01_Value.class));
	}

	@Test void a05_canConvertReturnsTrueForRegisteredType() {
		var c = new ConfigurableConverter()
			.add(String.class, A01_Value.class, (in, memberOf, args) -> new A01_Value(in));
		assertTrue(c.canConvert(String.class, A01_Value.class));
	}

	@Test void a06_canConvertReturnsTrueForBuiltInType() {
		var c = new ConfigurableConverter();
		assertTrue(c.canConvert(String.class, Integer.class));
	}

	@Test void a07_canConvertReturnsFalseForUnregisteredUnknownType() {
		var c = new ConfigurableConverter();
		assertFalse(c.canConvert(A01_Value.class, ConcurrentLinkedQueue.class));
	}

	@Test void a08_multipleRegistrationsOnSameConverter() {
		var c = new ConfigurableConverter()
			.add(String.class, A01_Value.class, (in, memberOf, args) -> new A01_Value(in))
			.add(Integer.class, A01_Value.class, (in, memberOf, args) -> new A01_Value(String.valueOf(in)));
		assertEquals("hello", c.to("hello", A01_Value.class).raw);
		assertEquals("42", c.to(42, A01_Value.class).raw);
	}

	/**
	 * A type with no public single-arg String constructor (so BasicConverter cannot convert it without registration).
	 */
	public static class A09_Value {
		public final String raw;
		private A09_Value(String raw) { this.raw = raw; }  // private - not discoverable by BasicConverter
	}

	@Test void a09_registrationDoesNotAffectOtherInstances() {
		var c1 = new ConfigurableConverter()
			.add(String.class, A09_Value.class, (in, memberOf, args) -> new A09_Value(in));
		var c2 = new ConfigurableConverter();
		assertNotNull(c1.to("x", A09_Value.class));
		assertFalse(c2.canConvert(String.class, A09_Value.class));
	}

	// =================================================================================================================
	// b - memberOf parameter is forwarded to registered conversion
	// =================================================================================================================

	@Test void b01_memberOfIsForwardedToRegisteredConversion() {
		// Use String→Integer (different types) so the identity-check shortcut is not triggered
		var memberOf = new Object();
		var captured = new AtomicReference<Object>();
		var c = new ConfigurableConverter()
			.add(String.class, Integer.class, (in, m, args) -> { captured.set(m); return Integer.parseInt(in); });
		c.to("42", memberOf, Integer.class);
		assertSame(memberOf, captured.get());
	}

	// =================================================================================================================
	// c - Thread safety
	// =================================================================================================================

	/**
	 * Type with private constructor used to verify concurrent registration without built-in conflict.
	 */
	public static class C01_Value {
		public final String raw;
		private C01_Value(String raw) { this.raw = raw; }
	}

	@Test void c01_concurrentRegistrationsAreThreadSafe() throws Exception {
		// Register String→C01_Value (not convertible by BasicConverter due to private constructor)
		var c = new ConfigurableConverter();
		var threads = 16;
		var latch = new CountDownLatch(1);
		var errors = new AtomicInteger(0);
		var pool = Executors.newFixedThreadPool(threads);
		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				try {
					latch.await();
					c.add(String.class, C01_Value.class, (in, m, args) -> new C01_Value(in.toUpperCase()));
				} catch (Exception e) {
					errors.incrementAndGet();
				}
			});
		}
		latch.countDown();
		pool.shutdown();
		pool.awaitTermination(5, TimeUnit.SECONDS);
		assertEquals(0, errors.get());
		var result = c.to("hello", C01_Value.class);
		assertNotNull(result);
		assertEquals("HELLO", result.raw);
	}

	@Test void c02_concurrentConvertsAreThreadSafe() throws Exception {
		var c = new ConfigurableConverter()
			.add(String.class, Integer.class, (in, m, args) -> Integer.parseInt(in) * 2);
		var threads = 32;
		var latch = new CountDownLatch(1);
		var errors = new AtomicInteger(0);
		var pool = Executors.newFixedThreadPool(threads);
		for (int i = 0; i < threads; i++) {
			pool.submit(() -> {
				try {
					latch.await();
					for (int j = 0; j < 100; j++) {
						var result = c.to("21", Integer.class);
						if (result != 42)
							errors.incrementAndGet();
					}
				} catch (Exception e) {
					errors.incrementAndGet();
				}
			});
		}
		latch.countDown();
		pool.shutdown();
		pool.awaitTermination(5, TimeUnit.SECONDS);
		assertEquals(0, errors.get());
	}
}
