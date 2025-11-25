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

class ThrowingConsumer_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedValue = new Object[1];

		ThrowingConsumer<String> consumer = (t) -> {
			callCount.incrementAndGet();
			receivedValue[0] = t;
		};

		consumer.accept("test");
		assertEquals(1, callCount.get());
		assertEquals("test", receivedValue[0]);
	}

	@Test void a02_withNullValue() {
		var callCount = new AtomicInteger();
		var receivedValue = new Object[1];

		ThrowingConsumer<String> consumer = (t) -> {
			callCount.incrementAndGet();
			receivedValue[0] = t;
		};

		consumer.accept(null);
		assertEquals(1, callCount.get());
		assertNull(receivedValue[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		ThrowingConsumer<String> consumer = (t) -> {
			throw new Exception("Test exception");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			consumer.accept("test");
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		ThrowingConsumer<String> consumer = (t) -> {
			throw new RuntimeException("Test runtime exception");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			consumer.accept("test");
		});

		// RuntimeExceptions should be re-thrown as-is (not wrapped)
		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test void b03_throwsError() {
		ThrowingConsumer<String> consumer = (t) -> {
			throw new Error("Test error");
		};

		// Error is not an Exception, so it's not caught and propagates directly
		Error ex = assertThrows(Error.class, () -> {
			consumer.accept("test");
		});

		assertEquals("Test error", ex.getMessage());
	}

	@Test void b04_throwsNullPointerException() {
		ThrowingConsumer<String> consumer = (t) -> {
			throw new NullPointerException("NPE");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			consumer.accept("test");
		});

		// NullPointerException is a RuntimeException, so it should be re-thrown as-is
		assertTrue(ex instanceof NullPointerException);
		assertEquals("NPE", ex.getMessage());
	}

	@Test void b05_throwsIllegalArgumentException() {
		ThrowingConsumer<String> consumer = (t) -> {
			throw new IllegalArgumentException("IAE");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			consumer.accept("test");
		});

		// IllegalArgumentException is a RuntimeException, so it should be re-thrown as-is
		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("IAE", ex.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsConsumer() {
		var callCount = new AtomicInteger();
		var receivedValue = new Object[1];

		// ThrowingConsumer extends Consumer, so it can be used where Consumer is expected
		java.util.function.Consumer<String> consumer = (t) -> {
			callCount.incrementAndGet();
			receivedValue[0] = t;
		};

		consumer.accept("test");
		assertEquals(1, callCount.get());
		assertEquals("test", receivedValue[0]);
	}

	@Test void c02_lambdaExpression() {
		var callCount = new AtomicInteger();

		ThrowingConsumer<Integer> consumer = (x) -> {
			callCount.addAndGet(x);
		};

		consumer.accept(5);
		consumer.accept(10);
		assertEquals(15, callCount.get());
	}

	@Test void c03_methodReference() {
		var callCount = new AtomicInteger();

		class Counter {
			void increment(int value) {
				callCount.addAndGet(value);
			}
		}

		Counter counter = new Counter();
		ThrowingConsumer<Integer> consumer = counter::increment;

		consumer.accept(7);
		assertEquals(7, callCount.get());
	}
}

