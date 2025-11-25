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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ThrowingFunction_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void a01_basic() {
		ThrowingFunction<String,Integer> function = (t) -> t.length();

		assertEquals(5, function.apply("hello"));
		assertEquals(0, function.apply(""));
	}

	@Test void a02_withNullValue() {
		ThrowingFunction<String,Integer> function = (t) -> t == null ? 0 : t.length();

		assertEquals(0, function.apply(null));
		assertEquals(5, function.apply("hello"));
	}

	@Test void a03_returnsNull() {
		ThrowingFunction<String,String> function = (t) -> null;

		assertNull(function.apply("test"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Exception handling tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void b01_throwsCheckedException() {
		ThrowingFunction<String,String> function = (t) -> {
			throw new Exception("Test exception");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test");
		});

		assertTrue(ex.getCause() instanceof Exception);
		assertEquals("Test exception", ex.getCause().getMessage());
	}

	@Test void b02_throwsRuntimeException() {
		ThrowingFunction<String,String> function = (t) -> {
			throw new RuntimeException("Test runtime exception");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test");
		});

		// RuntimeExceptions should be re-thrown as-is (not wrapped)
		assertEquals("Test runtime exception", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test void b03_throwsError() {
		ThrowingFunction<String,String> function = (t) -> {
			throw new Error("Test error");
		};

		// Error is not an Exception, so it's not caught and propagates directly
		Error ex = assertThrows(Error.class, () -> {
			function.apply("test");
		});

		assertEquals("Test error", ex.getMessage());
	}

	@Test void b04_throwsNullPointerException() {
		ThrowingFunction<String,String> function = (t) -> {
			throw new NullPointerException("NPE");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test");
		});

		// NullPointerException is a RuntimeException, so it should be re-thrown as-is
		assertTrue(ex instanceof NullPointerException);
		assertEquals("NPE", ex.getMessage());
	}

	@Test void b05_throwsIllegalArgumentException() {
		ThrowingFunction<String,String> function = (t) -> {
			throw new IllegalArgumentException("IAE");
		};

		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			function.apply("test");
		});

		// IllegalArgumentException is a RuntimeException, so it should be re-thrown as-is
		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("IAE", ex.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Functional interface tests.
	//------------------------------------------------------------------------------------------------------------------
	@Test void c01_usedAsFunction() {
		// ThrowingFunction extends Function, so it can be used where Function is expected
		java.util.function.Function<String,Integer> function = (t) -> t.length();

		assertEquals(5, function.apply("hello"));
	}

	@Test void c02_lambdaExpression() {
		ThrowingFunction<Integer,Integer> function = (x) -> x * 2;

		assertEquals(10, function.apply(5));
		assertEquals(0, function.apply(0));
		assertEquals(-6, function.apply(-3));
	}

	@Test void c03_methodReference() {
		ThrowingFunction<String,Integer> function = String::length;

		assertEquals(5, function.apply("hello"));
		assertEquals(0, function.apply(""));
	}

	@Test void c04_compose() {
		ThrowingFunction<Integer,Integer> multiply = (x) -> x * 2;
		java.util.function.Function<Integer,Integer> add = (x) -> x + 1;

		// Compose: multiply.apply(add.apply(5)) = multiply.apply(6) = 12
		// compose applies the argument function first, then this function
		var composed = multiply.compose(add);
		assertEquals(12, composed.apply(5));
	}

	@Test void c05_andThen() {
		ThrowingFunction<Integer,Integer> multiply = (x) -> x * 2;
		java.util.function.Function<Integer,Integer> add = (x) -> x + 1;

		// AndThen: add.apply(multiply.apply(5)) = add.apply(10) = 11
		var composed = multiply.andThen(add);
		assertEquals(11, composed.apply(5));
	}

	@Test void c06_identity() {
		ThrowingFunction<String,String> identity = (t) -> t;

		assertEquals("test", identity.apply("test"));
		assertNull(identity.apply(null));
	}
}

