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

class ThrowingConsumer2_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedValues = new Object[2];

		var consumer = (ThrowingConsumer2<String, Integer>)(a, b) -> {
			callCount.incrementAndGet();
			receivedValues[0] = a;
			receivedValues[1] = b;
		};

		consumer.apply("test", 42);
		assertEquals(1, callCount.get());
		assertEquals("test", receivedValues[0]);
		assertEquals(42, receivedValues[1]);
	}

	@Test void a02_withNullValues() {
		var callCount = new AtomicInteger();
		var receivedValues = new Object[2];

		var consumer = (ThrowingConsumer2<String, Integer>)(a, b) -> {
			callCount.incrementAndGet();
			receivedValues[0] = a;
			receivedValues[1] = b;
		};

		consumer.apply(null, null);
		assertEquals(1, callCount.get());
		assertNull(receivedValues[0]);
		assertNull(receivedValues[1]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var consumer = (ThrowingConsumer2<String, Integer>)(a, b) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			consumer.apply("test", 42);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var consumer = (ThrowingConsumer2<String, Integer>)(a, b) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			consumer.apply("test", 42);
		});

		// RuntimeExceptions should be re-thrown as-is (not wrapped)
		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test void b03_throwsError() {
		var consumer = (ThrowingConsumer2<String, Integer>)(a, b) -> {
			throw new Error("Test error");
		};

		// Error is not an Exception, so it's not caught and propagates directly
		var ex = assertThrows(Error.class, () -> {
			consumer.apply("test", 42);
		});

		assertEquals("Test error", ex.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsConsumer2() {
		var callCount = new AtomicInteger();

		// ThrowingConsumer2 extends Consumer2, so it can be used where Consumer2 is expected
		var consumer = (Consumer2<String, Integer>)(a, b) -> {
			callCount.incrementAndGet();
		};

		consumer.apply("test", 42);
		assertEquals(1, callCount.get());
	}

	@Test void c02_lambdaExpression() {
		var sum = new AtomicInteger(0);

		var consumer = (ThrowingConsumer2<Integer, Integer>)(a, b) -> {
			sum.addAndGet(a + b);
		};

		consumer.apply(5, 10);
		consumer.apply(3, 7);
		assertEquals(25, sum.get());
	}

	@Test void c03_methodReference() {
		var map = new java.util.HashMap<String, Integer>();

		var consumer = (ThrowingConsumer2<String, Integer>)map::put;

		consumer.apply("key1", 1);
		consumer.apply("key2", 2);
		assertEquals(2, map.size());
		assertEquals(1, map.get("key1"));
		assertEquals(2, map.get("key2"));
	}

	@Test void c04_andThen() {
		var callCount1 = new AtomicInteger();
		var callCount2 = new AtomicInteger();

		var consumer1 = (ThrowingConsumer2<String, Integer>)(a, b) -> callCount1.incrementAndGet();
		var consumer2 = (ThrowingConsumer2<String, Integer>)(a, b) -> callCount2.incrementAndGet();

		var composed = consumer1.andThen(consumer2);
		composed.apply("test", 42);

		assertEquals(1, callCount1.get());
		assertEquals(1, callCount2.get());
	}

	@Test void c05_andThen_withException() {
		var callCount = new AtomicInteger();

		var consumer1 = (ThrowingConsumer2<String, Integer>)(a, b) -> {
			throw new RuntimeException("First consumer failed");
		};
		var consumer2 = (ThrowingConsumer2<String, Integer>)(a, b) -> callCount.incrementAndGet();

		var composed = consumer1.andThen(consumer2);

		var ex = assertThrows(RuntimeException.class, () -> {
			composed.apply("test", 42);
		});

		assertEquals("First consumer failed", ex.getMessage());
		assertEquals(0, callCount.get()); // Second consumer should not be called
	}
}

