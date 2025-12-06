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

class ThrowingConsumer3_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedValues = new Object[3];

		var consumer = (ThrowingConsumer3<String, Integer, Boolean>)(a, b, c) -> {
			callCount.incrementAndGet();
			receivedValues[0] = a;
			receivedValues[1] = b;
			receivedValues[2] = c;
		};

		consumer.apply("test", 42, true);
		assertEquals(1, callCount.get());
		assertEquals("test", receivedValues[0]);
		assertEquals(42, receivedValues[1]);
		assertEquals(true, receivedValues[2]);
	}

	@Test void a02_withNullValues() {
		var callCount = new AtomicInteger();
		var receivedValues = new Object[3];

		var consumer = (ThrowingConsumer3<String, Integer, Boolean>)(a, b, c) -> {
			callCount.incrementAndGet();
			receivedValues[0] = a;
			receivedValues[1] = b;
			receivedValues[2] = c;
		};

		consumer.apply(null, null, null);
		assertEquals(1, callCount.get());
		assertNull(receivedValues[0]);
		assertNull(receivedValues[1]);
		assertNull(receivedValues[2]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var consumer = (ThrowingConsumer3<String, Integer, Boolean>)(a, b, c) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			consumer.apply("test", 42, true);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var consumer = (ThrowingConsumer3<String, Integer, Boolean>)(a, b, c) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			consumer.apply("test", 42, true);
		});

		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsConsumer3() {
		var callCount = new AtomicInteger();

		var consumer = (Consumer3<String, Integer, Boolean>)(a, b, c) -> {
			callCount.incrementAndGet();
		};

		consumer.apply("test", 42, true);
		assertEquals(1, callCount.get());
	}

	@Test void c02_lambdaExpression() {
		var sum = new AtomicInteger(0);

		ThrowingConsumer3<Integer, Integer, Integer> consumer = (a, b, c) -> {
			sum.addAndGet(a + b + c);
		};

		consumer.apply(1, 2, 3);
		consumer.apply(4, 5, 6);
		assertEquals(21, sum.get());
	}

	@Test void c03_andThen() {
		var callCount1 = new AtomicInteger();
		var callCount2 = new AtomicInteger();

		ThrowingConsumer3<String, Integer, Boolean> consumer1 = (a, b, c) -> callCount1.incrementAndGet();
		ThrowingConsumer3<String, Integer, Boolean> consumer2 = (a, b, c) -> callCount2.incrementAndGet();

		var composed = consumer1.andThen(consumer2);
		composed.apply("test", 42, true);

		assertEquals(1, callCount1.get());
		assertEquals(1, callCount2.get());
	}
}

