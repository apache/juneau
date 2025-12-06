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

class ThrowingFunction4_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var function = (ThrowingFunction4<String, Integer, Boolean, Double, String>)(a, b, c, d) -> {
			return a + "-" + b + "-" + c + "-" + d;
		};

		assertEquals("test-42-true-3.14", function.apply("test", 42, true, 3.14));
	}

	@Test void a02_returnsNull() {
		ThrowingFunction4<String, Integer, Boolean, Double, String> function = (a, b, c, d) -> null;

		assertNull(function.apply("test", 42, true, 3.14));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var function = (ThrowingFunction4<String, Integer, Boolean, Double, String>)(a, b, c, d) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42, true, 3.14);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var function = (ThrowingFunction4<String, Integer, Boolean, Double, String>)(a, b, c, d) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42, true, 3.14);
		});

		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsFunction4() {
		var function = (Function4<String, Integer, Boolean, Double, String>)(a, b, c, d) -> a + "-" + b;

		assertEquals("test-42", function.apply("test", 42, true, 3.14));
	}

	@Test void c02_lambdaExpression() {
		ThrowingFunction4<Integer, Integer, Integer, Integer, Integer> function = (a, b, c, d) -> a + b + c + d;

		assertEquals(10, function.apply(1, 2, 3, 4));
	}

	@Test void c03_andThen() {
		var add = (ThrowingFunction4<Integer, Integer, Integer, Integer, Integer>)(a, b, c, d) -> a + b + c + d;
		var toString = (java.util.function.Function<Integer, String>)Object::toString;

		var composed = add.andThen(toString);
		assertEquals("10", composed.apply(1, 2, 3, 4));
	}
}

