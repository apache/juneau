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
package org.apache.juneau.commons.reflect;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BeanRuntimeException}.
 */
class BeanRuntimeException_Test extends TestBase {

	//====================================================================================================
	// Constructor with Class and message
	//====================================================================================================

	@Test
	void a01_constructor_withClassAndMessage_formatsMessage() {
		var ex = new BeanRuntimeException(String.class, "Test message");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("String"));
		assertTrue(ex.getMessage().contains("Test message"));
		assertNull(ex.getCause());
	}

	@Test
	void a02_constructor_withClassAndMessageWithArgs_formatsMessage() {
		var ex = new BeanRuntimeException(Integer.class, "Value is {0}", 42);
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Integer"));
		assertTrue(ex.getMessage().contains("42"));
		assertNull(ex.getCause());
	}

	@Test
	void a03_constructor_withClassAndNullMessage_handlesNull() {
		String nullMessage = null;
		var ex = new BeanRuntimeException(String.class, nullMessage);
		assertNotNull(ex);
		assertNull(ex.getMessage());
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// Constructor with message only
	//====================================================================================================

	@Test
	void b01_constructor_withMessage_createsException() {
		var ex = new BeanRuntimeException("Simple message");
		assertNotNull(ex);
		assertEquals("Simple message", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void b02_constructor_withMessageAndArgs_formatsMessage() {
		var ex = new BeanRuntimeException("Value is {0}", 123);
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("123"));
		assertNull(ex.getCause());
	}

	@Test
	void b03_constructor_withNullMessage_handlesNull() {
		String nullMessage = null;
		var ex = new BeanRuntimeException(nullMessage);
		assertNotNull(ex);
		assertNull(ex.getMessage());
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// Constructor with Throwable only
	//====================================================================================================

	@Test
	void c01_constructor_withThrowable_wrapsException() {
		var cause = new IllegalArgumentException("Original error");
		var ex = new BeanRuntimeException(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Original error"));
	}

	@Test
	void c02_constructor_withNullThrowable_handlesNull() {
		Throwable nullCause = null;
		var ex = new BeanRuntimeException(nullCause);
		assertNotNull(ex);
		assertNull(ex.getCause());
		assertNull(ex.getMessage());
	}

	//====================================================================================================
	// Constructor with Throwable, Class, and message
	//====================================================================================================

	@Test
	void d01_constructor_withCauseClassAndMessage_formatsMessage() {
		var cause = new RuntimeException("Underlying error");
		var ex = new BeanRuntimeException(cause, String.class, "Wrapper message");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("String"));
		assertTrue(ex.getMessage().contains("Wrapper message"));
	}

	@Test
	void d02_constructor_withCauseClassAndMessageWithArgs_formatsMessage() {
		var cause = new RuntimeException("Underlying error");
		var ex = new BeanRuntimeException(cause, Integer.class, "Value {0} failed", 99);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Integer"));
		assertTrue(ex.getMessage().contains("99"));
	}

	@Test
	void d03_constructor_withCauseClassAndNullMessage_usesCauseMessage() {
		var cause = new RuntimeException("Underlying error");
		var ex = new BeanRuntimeException(cause, String.class, null);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("String"));
		assertTrue(ex.getMessage().contains("Underlying error"));
	}

	@Test
	void d04_constructor_withCauseNullClassAndMessage_formatsMessage() {
		var cause = new RuntimeException("Underlying error");
		var ex = new BeanRuntimeException(cause, null, "Wrapper message");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Wrapper message"));
		assertFalse(ex.getMessage().contains(": "), "Should not have class prefix when class is null");
	}

	@Test
	void d05_constructor_withNullCauseClassAndMessage_formatsMessage() {
		Throwable nullCause = null;
		var ex = new BeanRuntimeException(nullCause, String.class, "Test message");
		assertNotNull(ex);
		assertNull(ex.getCause());
		assertTrue(ex.getMessage().contains("String"));
		assertTrue(ex.getMessage().contains("Test message"));
	}

	@Test
	void d06_constructor_withNullCauseNullClassAndNullMessage_handlesNulls() {
		var ex = new BeanRuntimeException((Class<?>)null, null, (Object)null);
		assertNotNull(ex);
		assertNull(ex.getCause());
		assertNull(ex.getMessage());
	}

	//====================================================================================================
	// Exception chaining and stack traces
	//====================================================================================================

	@Test
	void e01_constructor_preservesStackTrace() {
		var cause = new RuntimeException("Original");
		var ex = new BeanRuntimeException(cause, String.class, "Wrapped");
		assertSame(cause, ex.getCause());
		assertNotNull(ex.getStackTrace());
		assertTrue(ex.getStackTrace().length > 0);
	}

	@Test
	void e02_constructor_withReflectionException_wrapsCorrectly() {
		var cause = new IllegalAccessException("Access denied");
		var ex = new BeanRuntimeException(cause, TestClass.class, "Failed to access");
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("TestClass"));
		assertTrue(ex.getMessage().contains("Failed to access"));
	}

	//====================================================================================================
	// Test class for class name formatting
	//====================================================================================================

	public static class TestClass {
		// Test class for exception testing
	}
}

