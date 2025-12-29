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

class Consumer3_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedA = new StringBuilder();
		var receivedB = new StringBuilder();
		var receivedC = new StringBuilder();

		Consumer3<String,Integer,Boolean> x = (a, b, c) -> {
			callCount.incrementAndGet();
			receivedA.append(a);
			receivedB.append(b);
			receivedC.append(c);
		};

		x.apply("foo", 42, true);
		assertEquals(1, callCount.get());
		assertEquals("foo", receivedA.toString());
		assertEquals("42", receivedB.toString());
		assertEquals("true", receivedC.toString());
	}

	@Test void a02_withNullValues() {
		var callCount = new AtomicInteger();
		var receivedA = new Object[1];
		var receivedB = new Object[1];
		var receivedC = new Object[1];

		Consumer3<String,Integer,Boolean> x = (a, b, c) -> {
			callCount.incrementAndGet();
			receivedA[0] = a;
			receivedB[0] = b;
			receivedC[0] = c;
		};

		x.apply(null, null, null);
		assertEquals(1, callCount.get());
		assertNull(receivedA[0]);
		assertNull(receivedB[0]);
		assertNull(receivedC[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_bothCalled() {
		var firstCallCount = new AtomicInteger();
		var secondCallCount = new AtomicInteger();

		Consumer3<String,Integer,Boolean> first = (a, b, c) -> firstCallCount.incrementAndGet();
		Consumer3<String,Integer,Boolean> second = (a, b, c) -> secondCallCount.incrementAndGet();

		var composed = first.andThen(second);
		composed.apply("test", 123, false);

		assertEquals(1, firstCallCount.get());
		assertEquals(1, secondCallCount.get());
	}

	@Test void b02_andThen_calledInOrder() {
		var callOrder = new StringBuilder();

		Consumer3<String,Integer,Boolean> first = (a, b, c) -> callOrder.append("first");
		Consumer3<String,Integer,Boolean> second = (a, b, c) -> callOrder.append("second");

		var composed = first.andThen(second);
		composed.apply("test", 123, false);

		assertEquals("firstsecond", callOrder.toString());
	}

	@Test void b03_andThen_sameArgumentsPassed() {
		var firstArgs = new Object[3];
		var secondArgs = new Object[3];

		Consumer3<String,Integer,Boolean> first = (a, b, c) -> {
			firstArgs[0] = a;
			firstArgs[1] = b;
			firstArgs[2] = c;
		};
		Consumer3<String,Integer,Boolean> second = (a, b, c) -> {
			secondArgs[0] = a;
			secondArgs[1] = b;
			secondArgs[2] = c;
		};

		var composed = first.andThen(second);
		composed.apply("foo", 42, true);

		assertEquals("foo", firstArgs[0]);
		assertEquals(42, firstArgs[1]);
		assertEquals(true, firstArgs[2]);
		assertEquals("foo", secondArgs[0]);
		assertEquals(42, secondArgs[1]);
		assertEquals(true, secondArgs[2]);
	}

	@Test void b04_andThen_chaining() {
		var callCount = new AtomicInteger();

		Consumer3<String,Integer,Boolean> first = (a, b, c) -> callCount.addAndGet(1);
		Consumer3<String,Integer,Boolean> second = (a, b, c) -> callCount.addAndGet(10);
		Consumer3<String,Integer,Boolean> third = (a, b, c) -> callCount.addAndGet(100);

		var composed = first.andThen(second).andThen(third);
		composed.apply("test", 123, false);

		assertEquals(111, callCount.get());
	}
}

