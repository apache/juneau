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

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Function3_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		Function3<Integer,Integer,Integer,Integer> x = (a,b,c)->a+b+c;
		assertEquals(6, x.apply(1,2,3));
	}

	@Test void a02_withNullValues() {
		Function3<String,Integer,Boolean,String> x = (a, b, c) -> a + b + c;
		assertEquals("null42true", x.apply(null, 42, true));
		assertEquals("foo0null", x.apply("foo", 0, null));
		assertEquals("nullnullnull", x.apply(null, null, null));
	}

	@Test void a03_returnsNull() {
		Function3<String,Integer,Boolean,String> x = (a, b, c) -> null;
		assertNull(x.apply("foo", 42, true));
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_basic() {
		Function3<Integer,Integer,Integer,Integer> first = (a, b, c) -> a + b + c;
		Function<Integer,String> after = x -> "result: " + x;
		Function3<Integer,Integer,Integer,String> composed = first.andThen(after);

		assertEquals("result: 6", composed.apply(1, 2, 3));
	}

	@Test void b02_andThen_differentReturnType() {
		Function3<String,Integer,Boolean,Integer> first = (a, b, c) -> a.length() + b + (c ? 1 : 0);
		Function<Integer,Boolean> after = x -> x > 5;
		Function3<String,Integer,Boolean,Boolean> composed = first.andThen(after);

		assertTrue(composed.apply("hello", 1, true));   // 5 + 1 + 1 = 7 > 5
		assertFalse(composed.apply("hi", 1, false));    // 2 + 1 + 0 = 3 <= 5
	}

	@Test void b03_andThen_chaining() {
		Function3<Integer,Integer,Integer,Integer> first = (a, b, c) -> a + b + c;
		Function<Integer,Integer> second = x -> x * 2;
		Function<Integer,String> third = x -> "value: " + x;
		Function3<Integer,Integer,Integer,String> composed = first.andThen(second).andThen(third);

		assertEquals("value: 12", composed.apply(1, 2, 3));  // (1+2+3)*2 = 12
	}

	@Test void b04_andThen_withNullAfter() {
		Function3<Integer,Integer,Integer,Integer> first = (a, b, c) -> a + b + c;
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			first.andThen(null);
		});
		assertTrue(ex.getMessage().contains("Argument 'after' cannot be null"));
	}

	@Test void b05_andThen_afterReturnsNull() {
		Function3<Integer,Integer,Integer,Integer> first = (a, b, c) -> a + b + c;
		Function<Integer,String> after = x -> null;
		Function3<Integer,Integer,Integer,String> composed = first.andThen(after);

		assertNull(composed.apply(1, 2, 3));
	}
}