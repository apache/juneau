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

class Function2_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		Function2<Integer,Integer,Integer> x = (a,b)->a+b;
		assertEquals(3, x.apply(1,2));
	}

	@Test void a02_withNullValues() {
		Function2<String,Integer,String> x = (a, b) -> a + b;
		assertEquals("null42", x.apply(null, 42));
		assertEquals("foonull", x.apply("foo", null));  // String concatenation: "foo" + null = "foonull"
		assertEquals("nullnull", x.apply(null, null));
	}

	@Test void a03_returnsNull() {
		Function2<String,Integer,String> x = (a, b) -> null;
		assertNull(x.apply("foo", 42));
	}

	//------------------------------------------------------------------------------------------------------------------
	// andThen tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_andThen_basic() {
		Function2<Integer,Integer,Integer> first = (a, b) -> a + b;
		Function<Integer,String> after = x -> "result: " + x;
		Function2<Integer,Integer,String> composed = first.andThen(after);

		assertEquals("result: 3", composed.apply(1, 2));
	}

	@Test void b02_andThen_differentReturnType() {
		Function2<String,Integer,Integer> first = (a, b) -> a.length() + b;
		Function<Integer,Boolean> after = x -> x > 5;
		Function2<String,Integer,Boolean> composed = first.andThen(after);

		assertTrue(composed.apply("hello", 1));  // 5 + 1 = 6 > 5
		assertFalse(composed.apply("hi", 1));    // 2 + 1 = 3 <= 5
	}

	@Test void b03_andThen_chaining() {
		Function2<Integer,Integer,Integer> first = (a, b) -> a + b;
		Function<Integer,Integer> second = x -> x * 2;
		Function<Integer,String> third = x -> "value: " + x;
		Function2<Integer,Integer,String> composed = first.andThen(second).andThen(third);

		assertEquals("value: 6", composed.apply(1, 2));  // (1+2)*2 = 6
	}

	@Test void b04_andThen_withNullAfter() {
		Function2<Integer,Integer,Integer> first = (a, b) -> a + b;
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			first.andThen(null);
		});
		assertTrue(ex.getMessage().contains("Argument 'after' cannot be null"));
	}

	@Test void b05_andThen_afterReturnsNull() {
		Function2<Integer,Integer,Integer> first = (a, b) -> a + b;
		Function<Integer,String> after = x -> null;
		Function2<Integer,Integer,String> composed = first.andThen(after);

		assertNull(composed.apply(1, 2));
	}
}