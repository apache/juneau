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

class ThrowingFunction3_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var function = (ThrowingFunction3<String, Integer, Boolean, String>)(a, b, c) -> {
			return a + "-" + b + "-" + (c ? "Y" : "N");
		};

		assertEquals("test-42-Y", function.apply("test", 42, true));
		assertEquals("hello-0-N", function.apply("hello", 0, false));
	}

	@Test void a02_returnsNull() {
		ThrowingFunction3<String, Integer, Boolean, String> function = (a, b, c) -> null;

		assertNull(function.apply("test", 42, true));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var function = (ThrowingFunction3<String, Integer, Boolean, String>)(a, b, c) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42, true);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var function = (ThrowingFunction3<String, Integer, Boolean, String>)(a, b, c) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42, true);
		});

		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsFunction3() {
		var function = (Function3<String, Integer, Boolean, String>)(a, b, c) -> a + "-" + b + "-" + c;

		assertEquals("test-42-true", function.apply("test", 42, true));
	}

	@Test void c02_lambdaExpression() {
		ThrowingFunction3<Integer, Integer, Integer, Integer> function = (a, b, c) -> a + b + c;

		assertEquals(6, function.apply(1, 2, 3));
		assertEquals(0, function.apply(0, 0, 0));
	}

	@Test void c03_andThen() {
		var add = (ThrowingFunction3<Integer, Integer, Integer, Integer>)(a, b, c) -> a + b + c;
		var toString = (java.util.function.Function<Integer, String>)Object::toString;

		var composed = add.andThen(toString);
		assertEquals("6", composed.apply(1, 2, 3));
	}
}

