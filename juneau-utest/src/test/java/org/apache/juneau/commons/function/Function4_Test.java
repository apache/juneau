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

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Function4_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		Function4<Integer,Integer,Integer,Integer,Integer> x = (a,b,c,d)->a+b+c+d;
		assertEquals(10, x.apply(1,2,3,4));
	}

	@Test void a02_withNullValues() {
		Function4<String,Integer,Boolean,Double,String> x = (a, b, c, d) -> a + b + c + d;
		assertEquals("null42true3.14", x.apply(null, 42, true, 3.14));
		assertEquals("foo0nullnull", x.apply("foo", 0, null, null));
		assertEquals("nullnullnullnull", x.apply(null, null, null, null));
	}

	@Test void a03_returnsNull() {
		Function4<String,Integer,Boolean,Double,String> x = (a, b, c, d) -> null;
		assertNull(x.apply("foo", 42, true, 3.14));
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_basic() {
		Function4<Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d) -> a + b + c + d;
		Function<Integer,String> after = x -> "result: " + x;
		Function4<Integer,Integer,Integer,Integer,String> composed = first.andThen(after);

		assertEquals("result: 10", composed.apply(1, 2, 3, 4));
	}

	@Test void b02_andThen_differentReturnType() {
		Function4<String,Integer,Boolean,Double,Integer> first = (a, b, c, d) -> a.length() + b + (c ? 1 : 0) + (int)d.doubleValue();
		Function<Integer,Boolean> after = x -> x > 10;
		Function4<String,Integer,Boolean,Double,Boolean> composed = first.andThen(after);

		assertTrue(composed.apply("hello", 1, true, 5.0));   // 5 + 1 + 1 + 5 = 12 > 10
		assertFalse(composed.apply("hi", 1, false, 1.0));    // 2 + 1 + 0 + 1 = 4 <= 10
	}

	@Test void b03_andThen_chaining() {
		Function4<Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d) -> a + b + c + d;
		Function<Integer,Integer> second = x -> x * 2;
		Function<Integer,String> third = x -> "value: " + x;
		Function4<Integer,Integer,Integer,Integer,String> composed = first.andThen(second).andThen(third);

		assertEquals("value: 20", composed.apply(1, 2, 3, 4));  // (1+2+3+4)*2 = 20
	}

	@Test void b04_andThen_withNullAfter() {
		Function4<Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d) -> a + b + c + d;
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'after' cannot be null", () -> {
			first.andThen(null);
		});
	}

	@Test void b05_andThen_afterReturnsNull() {
		Function4<Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d) -> a + b + c + d;
		Function<Integer,String> after = x -> null;
		Function4<Integer,Integer,Integer,Integer,String> composed = first.andThen(after);

		assertNull(composed.apply(1, 2, 3, 4));
	}
}