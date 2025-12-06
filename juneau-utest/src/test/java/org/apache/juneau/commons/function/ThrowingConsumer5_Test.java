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

class ThrowingConsumer5_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedValues = new Object[5];

		var consumer = (ThrowingConsumer5<String, Integer, Boolean, Double, Long>)(a, b, c, d, e) -> {
			callCount.incrementAndGet();
			receivedValues[0] = a;
			receivedValues[1] = b;
			receivedValues[2] = c;
			receivedValues[3] = d;
			receivedValues[4] = e;
		};

		consumer.apply("test", 42, true, 3.14, 100L);
		assertEquals(1, callCount.get());
		assertEquals("test", receivedValues[0]);
		assertEquals(42, receivedValues[1]);
		assertEquals(true, receivedValues[2]);
		assertEquals(3.14, receivedValues[3]);
		assertEquals(100L, receivedValues[4]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var consumer = (ThrowingConsumer5<String, Integer, Boolean, Double, Long>)(a, b, c, d, e) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			consumer.apply("test", 42, true, 3.14, 100L);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var consumer = (ThrowingConsumer5<String, Integer, Boolean, Double, Long>)(a, b, c, d, e) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			consumer.apply("test", 42, true, 3.14, 100L);
		});

		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsConsumer5() {
		var callCount = new AtomicInteger();

		var consumer = (Consumer5<String, Integer, Boolean, Double, Long>)(a, b, c, d, e) -> {
			callCount.incrementAndGet();
		};

		consumer.apply("test", 42, true, 3.14, 100L);
		assertEquals(1, callCount.get());
	}

	@Test void c02_lambdaExpression() {
		var sum = new AtomicInteger(0);

		ThrowingConsumer5<Integer, Integer, Integer, Integer, Integer> consumer = (a, b, c, d, e) -> {
			sum.addAndGet(a + b + c + d + e);
		};

		consumer.apply(1, 2, 3, 4, 5);
		assertEquals(15, sum.get());
	}

	@Test void c03_andThen() {
		var callCount1 = new AtomicInteger();
		var callCount2 = new AtomicInteger();

		ThrowingConsumer5<String, Integer, Boolean, Double, Long> consumer1 = (a, b, c, d, e) -> callCount1.incrementAndGet();
		ThrowingConsumer5<String, Integer, Boolean, Double, Long> consumer2 = (a, b, c, d, e) -> callCount2.incrementAndGet();

		var composed = consumer1.andThen(consumer2);
		composed.apply("test", 42, true, 3.14, 100L);

		assertEquals(1, callCount1.get());
		assertEquals(1, callCount2.get());
	}
}

