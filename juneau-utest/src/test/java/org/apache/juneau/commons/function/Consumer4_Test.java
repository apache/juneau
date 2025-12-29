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

class Consumer4_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedA = new StringBuilder();
		var receivedB = new StringBuilder();
		var receivedC = new StringBuilder();
		var receivedD = new StringBuilder();

		Consumer4<String,Integer,Boolean,Double> x = (a, b, c, d) -> {
			callCount.incrementAndGet();
			receivedA.append(a);
			receivedB.append(b);
			receivedC.append(c);
			receivedD.append(d);
		};

		x.apply("foo", 42, true, 3.14);
		assertEquals(1, callCount.get());
		assertEquals("foo", receivedA.toString());
		assertEquals("42", receivedB.toString());
		assertEquals("true", receivedC.toString());
		assertEquals("3.14", receivedD.toString());
	}

	@Test void a02_withNullValues() {
		var callCount = new AtomicInteger();
		var receivedA = new Object[1];
		var receivedB = new Object[1];
		var receivedC = new Object[1];
		var receivedD = new Object[1];

		Consumer4<String,Integer,Boolean,Double> x = (a, b, c, d) -> {
			callCount.incrementAndGet();
			receivedA[0] = a;
			receivedB[0] = b;
			receivedC[0] = c;
			receivedD[0] = d;
		};

		x.apply(null, null, null, null);
		assertEquals(1, callCount.get());
		assertNull(receivedA[0]);
		assertNull(receivedB[0]);
		assertNull(receivedC[0]);
		assertNull(receivedD[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_bothCalled() {
		var firstCallCount = new AtomicInteger();
		var secondCallCount = new AtomicInteger();

		Consumer4<String,Integer,Boolean,Double> first = (a, b, c, d) -> firstCallCount.incrementAndGet();
		Consumer4<String,Integer,Boolean,Double> second = (a, b, c, d) -> secondCallCount.incrementAndGet();

		var composed = first.andThen(second);
		composed.apply("test", 123, false, 2.5);

		assertEquals(1, firstCallCount.get());
		assertEquals(1, secondCallCount.get());
	}

	@Test void b02_andThen_calledInOrder() {
		var callOrder = new StringBuilder();

		Consumer4<String,Integer,Boolean,Double> first = (a, b, c, d) -> callOrder.append("first");
		Consumer4<String,Integer,Boolean,Double> second = (a, b, c, d) -> callOrder.append("second");

		var composed = first.andThen(second);
		composed.apply("test", 123, false, 2.5);

		assertEquals("firstsecond", callOrder.toString());
	}

	@Test void b03_andThen_sameArgumentsPassed() {
		var firstArgs = new Object[4];
		var secondArgs = new Object[4];

		Consumer4<String,Integer,Boolean,Double> first = (a, b, c, d) -> {
			firstArgs[0] = a;
			firstArgs[1] = b;
			firstArgs[2] = c;
			firstArgs[3] = d;
		};
		Consumer4<String,Integer,Boolean,Double> second = (a, b, c, d) -> {
			secondArgs[0] = a;
			secondArgs[1] = b;
			secondArgs[2] = c;
			secondArgs[3] = d;
		};

		var composed = first.andThen(second);
		composed.apply("foo", 42, true, 3.14);

		assertEquals("foo", firstArgs[0]);
		assertEquals(42, firstArgs[1]);
		assertEquals(true, firstArgs[2]);
		assertEquals(3.14, firstArgs[3]);
		assertEquals("foo", secondArgs[0]);
		assertEquals(42, secondArgs[1]);
		assertEquals(true, secondArgs[2]);
		assertEquals(3.14, secondArgs[3]);
	}

	@Test void b04_andThen_chaining() {
		var callCount = new AtomicInteger();

		Consumer4<String,Integer,Boolean,Double> first = (a, b, c, d) -> callCount.addAndGet(1);
		Consumer4<String,Integer,Boolean,Double> second = (a, b, c, d) -> callCount.addAndGet(10);
		Consumer4<String,Integer,Boolean,Double> third = (a, b, c, d) -> callCount.addAndGet(100);

		var composed = first.andThen(second).andThen(third);
		composed.apply("test", 123, false, 2.5);

		assertEquals(111, callCount.get());
	}
}

