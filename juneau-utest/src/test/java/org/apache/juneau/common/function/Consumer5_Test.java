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

class Consumer5_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var callCount = new AtomicInteger();
		var receivedA = new StringBuilder();
		var receivedB = new StringBuilder();
		var receivedC = new StringBuilder();
		var receivedD = new StringBuilder();
		var receivedE = new StringBuilder();

		Consumer5<String,Integer,Boolean,Double,Character> x = (a, b, c, d, e) -> {
			callCount.incrementAndGet();
			receivedA.append(a);
			receivedB.append(b);
			receivedC.append(c);
			receivedD.append(d);
			receivedE.append(e);
		};

		x.apply("foo", 42, true, 3.14, 'X');
		assertEquals(1, callCount.get());
		assertEquals("foo", receivedA.toString());
		assertEquals("42", receivedB.toString());
		assertEquals("true", receivedC.toString());
		assertEquals("3.14", receivedD.toString());
		assertEquals("X", receivedE.toString());
	}

	@Test void a02_withNullValues() {
		var callCount = new AtomicInteger();
		var receivedA = new Object[1];
		var receivedB = new Object[1];
		var receivedC = new Object[1];
		var receivedD = new Object[1];
		var receivedE = new Object[1];

		Consumer5<String,Integer,Boolean,Double,Character> x = (a, b, c, d, e) -> {
			callCount.incrementAndGet();
			receivedA[0] = a;
			receivedB[0] = b;
			receivedC[0] = c;
			receivedD[0] = d;
			receivedE[0] = e;
		};

		x.apply(null, null, null, null, null);
		assertEquals(1, callCount.get());
		assertNull(receivedA[0]);
		assertNull(receivedB[0]);
		assertNull(receivedC[0]);
		assertNull(receivedD[0]);
		assertNull(receivedE[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_bothCalled() {
		var firstCallCount = new AtomicInteger();
		var secondCallCount = new AtomicInteger();

		Consumer5<String,Integer,Boolean,Double,Character> first = (a, b, c, d, e) -> firstCallCount.incrementAndGet();
		Consumer5<String,Integer,Boolean,Double,Character> second = (a, b, c, d, e) -> secondCallCount.incrementAndGet();

		var composed = first.andThen(second);
		composed.apply("test", 123, false, 2.5, 'Y');

		assertEquals(1, firstCallCount.get());
		assertEquals(1, secondCallCount.get());
	}

	@Test void b02_andThen_calledInOrder() {
		var callOrder = new StringBuilder();

		Consumer5<String,Integer,Boolean,Double,Character> first = (a, b, c, d, e) -> callOrder.append("first");
		Consumer5<String,Integer,Boolean,Double,Character> second = (a, b, c, d, e) -> callOrder.append("second");

		var composed = first.andThen(second);
		composed.apply("test", 123, false, 2.5, 'Y');

		assertEquals("firstsecond", callOrder.toString());
	}

	@Test void b03_andThen_sameArgumentsPassed() {
		var firstArgs = new Object[5];
		var secondArgs = new Object[5];

		Consumer5<String,Integer,Boolean,Double,Character> first = (a, b, c, d, e) -> {
			firstArgs[0] = a;
			firstArgs[1] = b;
			firstArgs[2] = c;
			firstArgs[3] = d;
			firstArgs[4] = e;
		};
		Consumer5<String,Integer,Boolean,Double,Character> second = (a, b, c, d, e) -> {
			secondArgs[0] = a;
			secondArgs[1] = b;
			secondArgs[2] = c;
			secondArgs[3] = d;
			secondArgs[4] = e;
		};

		var composed = first.andThen(second);
		composed.apply("foo", 42, true, 3.14, 'X');

		assertEquals("foo", firstArgs[0]);
		assertEquals(42, firstArgs[1]);
		assertEquals(true, firstArgs[2]);
		assertEquals(3.14, firstArgs[3]);
		assertEquals('X', firstArgs[4]);
		assertEquals("foo", secondArgs[0]);
		assertEquals(42, secondArgs[1]);
		assertEquals(true, secondArgs[2]);
		assertEquals(3.14, secondArgs[3]);
		assertEquals('X', secondArgs[4]);
	}

	@Test void b04_andThen_chaining() {
		var callCount = new AtomicInteger();

		Consumer5<String,Integer,Boolean,Double,Character> first = (a, b, c, d, e) -> callCount.addAndGet(1);
		Consumer5<String,Integer,Boolean,Double,Character> second = (a, b, c, d, e) -> callCount.addAndGet(10);
		Consumer5<String,Integer,Boolean,Double,Character> third = (a, b, c, d, e) -> callCount.addAndGet(100);

		var composed = first.andThen(second).andThen(third);
		composed.apply("test", 123, false, 2.5, 'Y');

		assertEquals(111, callCount.get());
	}
}

