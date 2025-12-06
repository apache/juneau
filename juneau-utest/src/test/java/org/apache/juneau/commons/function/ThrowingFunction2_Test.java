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

class ThrowingFunction2_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		var function = (ThrowingFunction2<String, Integer, String>)(a, b) -> a + "-" + b;

		assertEquals("test-42", function.apply("test", 42));
		assertEquals("hello-0", function.apply("hello", 0));
	}

	@Test void a02_withNullValues() {
		var function = (ThrowingFunction2<String, Integer, String>)(a, b) -> {
			String aStr = a == null ? "null" : a;
			String bStr = b == null ? "null" : String.valueOf(b);
			return aStr + "-" + bStr;
		};

		assertEquals("null-42", function.apply(null, 42));
		assertEquals("test-null", function.apply("test", null));
	}

	@Test void a03_returnsNull() {
		var function = (ThrowingFunction2<String, Integer, String>)(a, b) -> null;

		assertNull(function.apply("test", 42));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		var function = (ThrowingFunction2<String, Integer, String>)(a, b) -> {
			throw new Exception("Test exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42);
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		var function = (ThrowingFunction2<String, Integer, String>)(a, b) -> {
			throw new RuntimeException("Test runtime exception");
		};

		var ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test", 42);
		});

		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test void b03_throwsError() {
		var function = (ThrowingFunction2<String, Integer, String>)(a, b) -> {
			throw new Error("Test error");
		};

		var ex = assertThrows(Error.class, () -> {
			function.apply("test", 42);
		});

		assertEquals("Test error", ex.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsFunction2() {
		var function = (Function2<String, Integer, String>)(a, b) -> a + "-" + b;

		assertEquals("test-42", function.apply("test", 42));
	}

	@Test void c02_lambdaExpression() {
		var function = (ThrowingFunction2<Integer, Integer, Integer>)(a, b) -> a + b;

		assertEquals(5, function.apply(2, 3));
		assertEquals(0, function.apply(0, 0));
		assertEquals(-1, function.apply(-3, 2));
	}

	@Test void c03_methodReference() {
		var function = (ThrowingFunction2<String, String, Boolean>)String::equals;

		assertTrue(function.apply("test", "test"));
		assertFalse(function.apply("test", "other"));
	}

	@Test void c04_andThen() {
		var add = (ThrowingFunction2<Integer, Integer, Integer>)(a, b) -> a + b;
		var toString = (java.util.function.Function<Integer, String>)Object::toString;

		var composed = add.andThen(toString);
		assertEquals("5", composed.apply(2, 3));
	}
}

