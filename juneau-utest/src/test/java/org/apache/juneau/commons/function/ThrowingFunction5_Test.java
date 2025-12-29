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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ThrowingFunction5_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var function = (ThrowingFunction5<String, Integer, Boolean, Double, Long, String>)(a, b, c, d, e) -> {
			return a + "-" + b + "-" + c + "-" + d + "-" + e;
		};

		assertEquals("test-42-true-3.14-100", function.apply("test", 42, true, 3.14, 100L));
	}

	@Test void a02_returnsNull() {
		ThrowingFunction5<String, Integer, Boolean, Double, Long, String> function = (a, b, c, d, e) -> null;

		assertNull(function.apply("test", 42, true, 3.14, 100L));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var function = (ThrowingFunction5<String, Integer, Boolean, Double, Long, String>)(a, b, c, d, e) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42, true, 3.14, 100L);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var function = (ThrowingFunction5<String, Integer, Boolean, Double, Long, String>)(a, b, c, d, e) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42, true, 3.14, 100L);
		});

		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsFunction5() {
		var function = (Function5<String, Integer, Boolean, Double, Long, String>)(a, b, c, d, e) -> a + "-" + b;

		assertEquals("test-42", function.apply("test", 42, true, 3.14, 100L));
	}

	@Test void c02_lambdaExpression() {
		ThrowingFunction5<Integer, Integer, Integer, Integer, Integer, Integer> function = (a, b, c, d, e) -> a + b + c + d + e;

		assertEquals(15, function.apply(1, 2, 3, 4, 5));
	}

	@Test void c03_andThen() {
		var add = (ThrowingFunction5<Integer, Integer, Integer, Integer, Integer, Integer>)(a, b, c, d, e) -> a + b + c + d + e;
		var toString = (java.util.function.Function<Integer, String>)Object::toString;

		var composed = add.andThen(toString);
		assertEquals("15", composed.apply(1, 2, 3, 4, 5));
	}
}

