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

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Function5_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		Function5<Integer,Integer,Integer,Integer,Integer,Integer> x = (a,b,c,d,e)->a+b+c+d+e;
		assertEquals(15, x.apply(1,2,3,4,5));
	}

	@Test void a02_withNullValues() {
		Function5<String,Integer,Boolean,Double,Character,String> x = (a, b, c, d, e) -> a + b + c + d + e;
		assertEquals("null42true3.14X", x.apply(null, 42, true, 3.14, 'X'));
		assertEquals("foo0nullnullnull", x.apply("foo", 0, null, null, null));
		assertEquals("nullnullnullnullnull", x.apply(null, null, null, null, null));
	}

	@Test void a03_returnsNull() {
		Function5<String,Integer,Boolean,Double,Character,String> x = (a, b, c, d, e) -> null;
		assertNull(x.apply("foo", 42, true, 3.14, 'X'));
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_basic() {
		Function5<Integer,Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d, e) -> a + b + c + d + e;
		Function<Integer,String> after = x -> "result: " + x;
		Function5<Integer,Integer,Integer,Integer,Integer,String> composed = first.andThen(after);

		assertEquals("result: 15", composed.apply(1, 2, 3, 4, 5));
	}

	@Test void b02_andThen_differentReturnType() {
		Function5<String,Integer,Boolean,Double,Character,Integer> first = (a, b, c, d, e) ->
			a.length() + b + (c ? 1 : 0) + (int)d.doubleValue() + (e != null ? 1 : 0);
		Function<Integer,Boolean> after = x -> x > 15;
		Function5<String,Integer,Boolean,Double,Character,Boolean> composed = first.andThen(after);

		// 5 + 1 + 1 + 5 + 1 = 13 <= 15
		assertFalse(composed.apply("hello", 1, true, 5.0, 'X'));
		// 2 + 1 + 0 + 1 + 0 = 4 <= 15
		assertFalse(composed.apply("hi", 1, false, 1.0, null));
		// 10 + 5 + 1 + 10 + 1 = 27 > 15
		assertTrue(composed.apply("helloworld", 5, true, 10.0, 'Y'));
	}

	@Test void b03_andThen_chaining() {
		Function5<Integer,Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d, e) -> a + b + c + d + e;
		Function<Integer,Integer> second = x -> x * 2;
		Function<Integer,String> third = x -> "value: " + x;
		Function5<Integer,Integer,Integer,Integer,Integer,String> composed = first.andThen(second).andThen(third);

		assertEquals("value: 30", composed.apply(1, 2, 3, 4, 5));  // (1+2+3+4+5)*2 = 30
	}

	@Test void b04_andThen_withNullAfter() {
		Function5<Integer,Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d, e) -> a + b + c + d + e;
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'after' cannot be null", () -> {
			first.andThen(null);
		});
	}

	@Test void b05_andThen_afterReturnsNull() {
		Function5<Integer,Integer,Integer,Integer,Integer,Integer> first = (a, b, c, d, e) -> a + b + c + d + e;
		Function<Integer,String> after = x -> null;
		Function5<Integer,Integer,Integer,Integer,Integer,String> composed = first.andThen(after);

		assertNull(composed.apply(1, 2, 3, 4, 5));
	}
}