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

import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ConfigurableConverter}.
 */
public class ConfigurableConverter_Test {

	// =================================================================================================================
	// a - ConversionFinder integration
	// =================================================================================================================

	public static class A01_Value {
		public final String raw;
		private A01_Value(String raw) { this.raw = raw; }
	}

	@Test void a01_finderConversionIsUsed() {
		var c = new ConfigurableConverter(
			(in, out) -> in == String.class && out == A01_Value.class
				? (s, memberOf, session, args) -> new A01_Value((String) s)
				: null
		);
		var result = c.to("hello", A01_Value.class);
		assertNotNull(result);
		assertEquals("hello", result.raw);
	}

	@Test void a02_finderTakesPriorityOverBuiltIn() {
		var c = new ConfigurableConverter(
			(in, out) -> in == Integer.class && out == String.class
				? (i, memberOf, session, args) -> "[" + i + "]"
				: null
		);
		assertEquals("[42]", c.to(42, String.class));
	}

	@Test void a03_builtInConversionUsedWhenNoFinderMatch() {
		var c = new ConfigurableConverter();
		assertEquals(Integer.valueOf(42), c.to("42", Integer.class));
	}

	@Test void a04_nullInputReturnsNull() {
		var c = new ConfigurableConverter(
			(in, out) -> in == String.class && out == A01_Value.class
				? (s, memberOf, session, args) -> new A01_Value((String) s)
				: null
		);
		assertNull(c.to(null, A01_Value.class));
	}

	@Test void a05_hasCustomConversionReturnsTrueForFinderMatch() {
		var c = new ConfigurableConverter(
			(in, out) -> in == String.class && out == A01_Value.class
				? (s, memberOf, session, args) -> new A01_Value((String) s)
				: null
		);
		assertTrue(c.hasCustomConversion(String.class, A01_Value.class));
		assertFalse(c.hasCustomConversion(Integer.class, A01_Value.class));
	}

	@Test void a06_canConvertReturnsTrueForBuiltInType() {
		var c = new ConfigurableConverter();
		assertTrue(c.canConvert(String.class, Integer.class));
	}

	@Test void a07_canConvertReturnsFalseForUnknownType() {
		var c = new ConfigurableConverter();
		assertFalse(c.canConvert(A01_Value.class, ConcurrentLinkedQueue.class));
	}

	@Test void a08_multipleFindersConsultedInOrder() {
		// First finder handles String→A01_Value, second handles Integer→A01_Value
		var c = new ConfigurableConverter(
			(in, out) -> in == String.class && out == A01_Value.class
				? (s, memberOf, session, args) -> new A01_Value("string:" + s)
				: null,
			(in, out) -> in == Integer.class && out == A01_Value.class
				? (i, memberOf, session, args) -> new A01_Value("int:" + i)
				: null
		);
		assertEquals("string:hello", c.to("hello", A01_Value.class).raw);
		assertEquals("int:42", c.to(42, A01_Value.class).raw);
	}

	@Test void a09_findersDoNotAffectOtherInstances() {
		var c1 = new ConfigurableConverter(
			(in, out) -> in == String.class && out == A01_Value.class
				? (s, memberOf, session, args) -> new A01_Value((String) s)
				: null
		);
		var c2 = new ConfigurableConverter();
		assertNotNull(c1.to("x", A01_Value.class));
		assertFalse(c2.canConvert(String.class, A01_Value.class));
	}

	// =================================================================================================================
	// b - memberOf parameter is forwarded to finder conversion
	// =================================================================================================================

	@Test void b01_memberOfIsForwardedToFinderConversion() {
		var memberOf = new Object();
		var captured = new java.util.concurrent.atomic.AtomicReference<Object>();
		var c = new ConfigurableConverter(
			(in, out) -> in == String.class && out == Integer.class
				? (s, m, session, args) -> { captured.set(m); return Integer.parseInt((String) s); }
				: null
		);
		c.to("42", memberOf, (ConverterSession) null, Integer.class);
		assertSame(memberOf, captured.get());
	}

	// =================================================================================================================
	// c - Thread safety
	// =================================================================================================================

	@Test void c01_concurrentConvertsAreThreadSafe() throws Exception {
		var c = new ConfigurableConverter(
			(in, out) -> in == String.class && out == Integer.class
				? (s, m, session, args) -> Integer.parseInt((String) s) * 2
				: null
		);
		var threads = 32;
		var latch = new CountDownLatch(1);
		var errors = new java.util.concurrent.atomic.AtomicInteger(0);
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
