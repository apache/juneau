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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class AssertionUtils_Test extends TestBase {

	//====================================================================================================
	// Constructor (line 56)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 56: class instantiation
		// AssertionUtils has an implicit public no-arg constructor
		var instance = new AssertionUtils();
		assertNotNull(instance);
	}

	//====================================================================================================
	// assertArg(boolean, String, Object...)
	//====================================================================================================
	@Test
	void a001_assertArg() {
		// Should not throw when expression is true
		assertArg(true, "Should not throw");
		assertArg(true, "Message with {0}", "arg");
		assertArg(true, "Test {0} {1} {2}", "a", "b", "c");
		
		// Should throw when expression is false
		assertThrowsWithMessage(IllegalArgumentException.class, "Test message", () -> {
			assertArg(false, "Test message");
		});
		
		assertThrowsWithMessage(IllegalArgumentException.class, l("Test message", "arg1"), () -> {
			assertArg(false, "Test message {0}", "arg1");
		});
		
		assertThrowsWithMessage(IllegalArgumentException.class, "Test", () -> {
			assertArg(false, "Test {0} {1} {2}", "a", "b", "c");
		});
	}

	//====================================================================================================
	// assertArgNotNull(String, T)
	//====================================================================================================
	@Test
	void a002_assertArgNotNull() {
		// Should not throw when value is not null
		var value = "test";
		var result = assertArgNotNull("arg", value);
		assertSame(value, result);
		
		var obj = new Object();
		var result2 = assertArgNotNull("arg", obj);
		assertSame(obj, result2);
		
		var i = 123;
		var result3 = assertArgNotNull("int", i);
		assertEquals(i, result3);
		
		var d = 45.6;
		var result4 = assertArgNotNull("double", d);
		assertEquals(d, result4);
		
		// Should throw when value is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be null"), () -> {
			assertArgNotNull("arg", null);
		});
	}

	//====================================================================================================
	// assertArgNotNullOrBlank(String, String)
	//====================================================================================================
	@Test
	void a003_assertArgNotNullOrBlank() {
		// Should not throw when value is valid
		var value = "test";
		var result = assertArgNotNullOrBlank("arg", value);
		assertSame(value, result);
		
		// Should throw when value is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be null"), () -> {
			assertArgNotNullOrBlank("arg", null);
		});
		
		// Should throw when value is empty
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be blank"), () -> {
			assertArgNotNullOrBlank("arg", "");
		});
		
		// Should throw when value is whitespace
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be blank"), () -> {
			assertArgNotNullOrBlank("arg", "   ");
		});
		
		assertThrowsWithMessage(IllegalArgumentException.class, "cannot be blank", () -> {
			assertArgNotNullOrBlank("arg", "\t");
		});
		
		assertThrowsWithMessage(IllegalArgumentException.class, "cannot be blank", () -> {
			assertArgNotNullOrBlank("arg", "\n");
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object) - 2 args
	//====================================================================================================
	@Test
	void a004_assertArgsNotNull_2args() {
		// Should not throw when both are not null
		assertArgsNotNull("arg1", "value1", "arg2", "value2");
		
		// Should throw when first is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg1", "cannot be null"), () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2");
		});
		
		// Should throw when second is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg2", "cannot be null"), () -> {
			assertArgsNotNull("arg1", "value1", "arg2", null);
		});
		
		// Should fail on first null when both are null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", null);
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object, String, Object) - 3 args
	//====================================================================================================
	@Test
	void a005_assertArgsNotNull_3args() {
		// Should not throw when all are not null
		assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3");
		
		// Should throw when first is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2", "arg3", "value3");
		});
		
		// Should throw when second is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg2", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", null, "arg3", "value3");
		});
		
		// Should throw when third is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg3", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", null);
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object, String, Object, String, Object) - 4 args
	//====================================================================================================
	@Test
	void a006_assertArgsNotNull_4args() {
		// Should not throw when all are not null
		assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", "value4");
		
		// Should throw when first is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2", "arg3", "value3", "arg4", "value4");
		});
		
		// Should throw when fourth is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg4", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", null);
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object, String, Object, String, Object, String, Object) - 5 args
	//====================================================================================================
	@Test
	void a007_assertArgsNotNull_5args() {
		// Should not throw when all are not null
		assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", "value4", "arg5", "value5");
		
		// Should throw when first is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2", "arg3", "value3", "arg4", "value4", "arg5", "value5");
		});
		
		// Should throw when fifth is null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg5", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", "value4", "arg5", null);
		});
	}

	//====================================================================================================
	// assertClassArrayArgIsType(String, Class<E>, Class<?>[])
	//====================================================================================================
	@Test
	void a008_assertClassArrayArgIsType() {
		// Should not throw when all classes are assignable
		var classes = a(String.class, Object.class);
		var result = assertClassArrayArgIsType("arg", Object.class, classes);
		assertSame(classes, result);
		
		// Should not throw with empty array
		var emptyClasses = new Class<?>[0];
		var result2 = assertClassArrayArgIsType("arg", Object.class, emptyClasses);
		assertSame(emptyClasses, result2);
		
		// Should not throw with subclasses
		var subclasses = a(Integer.class, Double.class);
		var result3 = assertClassArrayArgIsType("arg", Number.class, subclasses);
		assertSame(subclasses, result3);
		
		// Should not throw with same class
		var sameClasses = a(String.class);
		var result4 = assertClassArrayArgIsType("arg", String.class, sameClasses);
		assertSame(sameClasses, result4);
		
		// Should throw when class is not assignable
		var invalidClasses = a(String.class, Integer.class);
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "String"), () -> {
			assertClassArrayArgIsType("arg", Number.class, invalidClasses);
		});
		
		// Should throw with index information
		var invalidClasses2 = a(String.class, Integer.class, Double.class);
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "index", "0"), () -> {
			assertClassArrayArgIsType("arg", Number.class, invalidClasses2);
		});
	}

	//====================================================================================================
	// assertOneOf(T, T...)
	//====================================================================================================
	@Test
	void a009_assertOneOf() {
		// Should return actual when it matches
		assertEquals("test", assertOneOf("test", "test", "other"));
		assertEquals(123, assertOneOf(123, 123, 456));
		assertEquals("a", assertOneOf("a", "a", "b", "c"));
		
		// Exact match
		assertEquals("test", assertOneOf("test", "test"));
		assertEquals(1, assertOneOf(1, 1, 2, 3));
		
		// Match in middle
		assertEquals(2, assertOneOf(2, 1, 2, 3));
		
		// Match at end
		assertEquals(3, assertOneOf(3, 1, 2, 3));
		
		// Should handle nulls
		assertNull(assertOneOf(null, null, "test"));
		assertNull(assertOneOf(null, "test", null));
		
		// Should return same instance
		var value = "test";
		var result = assertOneOf(value, "test", "other");
		assertSame(value, result);
		
		// Should work with objects
		var obj1 = new Object();
		var obj2 = new Object();
		var result2 = assertOneOf(obj1, obj1, obj2);
		assertSame(obj1, result2);
		
		// Should throw when value doesn't match
		assertThrowsWithMessage(AssertionError.class, l("Invalid value specified", "test"), () -> {
			assertOneOf("test", "other");
		});
		
		assertThrowsWithMessage(AssertionError.class, l("Invalid value specified", "test"), () -> {
			assertOneOf("test", "a", "b", "c");
		});
		
		assertThrows(AssertionError.class, () -> assertOneOf(10, 1, 2, 3, 4, 5));
		
		// Should throw with empty expected
		assertThrowsWithMessage(AssertionError.class, "Invalid value specified", () -> {
			assertOneOf("test");
		});
	}

	//====================================================================================================
	// assertType(Class<T>, Object)
	//====================================================================================================
	@Test
	void a010_assertType() {
		// Should return object when it's an instance of type
		String value = "test";
		String result = assertType(String.class, value);
		assertSame(value, result);
		
		// Should work with subclasses
		Integer intValue = 123;
		Number numberResult = assertType(Number.class, intValue);
		assertSame(intValue, numberResult);
		
		// Should work with same class
		Object obj = new Object();
		Object result2 = assertType(Object.class, obj);
		assertSame(obj, result2);
		
		// Should work with primitive wrappers
		Integer intValue2 = 42;
		Integer result3 = assertType(Integer.class, intValue2);
		assertSame(intValue2, result3);
		
		// Should throw when type is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("type", "cannot be null"), () -> {
			assertType(null, "test");
		});
		
		// Should throw when object is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("o", "cannot be null"), () -> {
			assertType(String.class, null);
		});
		
		// Should throw when object is not an instance
		assertThrowsWithMessage(IllegalArgumentException.class, l("Object is not an instance of", "String", "Integer"), () -> {
			assertType(String.class, 123);
		});
		
		assertThrowsWithMessage(IllegalArgumentException.class, "Object is not an instance of", () -> {
			assertType(Integer.class, "test");
		});
	}

	//====================================================================================================
	// assertType(Class<T>, Object, Supplier<? extends RuntimeException>)
	//====================================================================================================
	@Test
	void a011_assertType_withSupplier() {
		// Should return object when it's an instance of type
		String value = "test";
		String result = assertType(String.class, value, () -> new IllegalStateException("Should not throw"));
		assertSame(value, result);
		
		// Should throw when type is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("type", "cannot be null"), () -> {
			assertType(null, "test", () -> new IllegalStateException("Custom"));
		});
		
		// Should throw when object is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("o", "cannot be null"), () -> {
			assertType(String.class, null, () -> new IllegalStateException("Custom"));
		});
		
		// Should throw custom exception when object is not an instance
		IllegalStateException customException = new IllegalStateException("Custom exception");
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			assertType(String.class, 123, () -> customException);
		});
		assertSame(customException, thrown);
		
		// Should work with different exception types
		RuntimeException runtimeException = new RuntimeException("Custom runtime exception");
		RuntimeException thrown2 = assertThrows(RuntimeException.class, () -> {
			assertType(Integer.class, "test", () -> runtimeException);
		});
		assertSame(runtimeException, thrown2);
		
		// Should work with supplier that creates new exception
		IllegalStateException thrown3 = assertThrows(IllegalStateException.class, () -> {
			assertType(String.class, 123, () -> new IllegalStateException("Not a string"));
		});
		assertEquals("Not a string", thrown3.getMessage());
	}

	//====================================================================================================
	// assertVarargsNotNull(String, T[])
	//====================================================================================================
	@Test
	void a012_assertVarargsNotNull() {
		// Should not throw when array and elements are not null
		var array = a("a", "b", "c");
		var result = assertVarargsNotNull("arg", array);
		assertSame(array, result);
		
		// Should not throw with empty array
		var emptyArray = new String[0];
		var result2 = assertVarargsNotNull("arg", emptyArray);
		assertSame(emptyArray, result2);
		
		// Should work with integer array
		var intArray = a(1, 2, 3);
		var result3 = assertVarargsNotNull("arg", intArray);
		assertSame(intArray, result3);
		
		// Should work with object array
		var objArray = a(new Object(), new Object());
		var result4 = assertVarargsNotNull("arg", objArray);
		assertSame(objArray, result4);
		
		// Should throw when array is null
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be null"), () -> {
			assertVarargsNotNull("arg", (String[])null);
		});
		
		// Should throw when element is null
		var nullElementArray = a("a", null, "c");
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "parameter", "1"), () -> {
			assertVarargsNotNull("arg", nullElementArray);
		});
		
		// Should fail on first null when multiple elements are null
		var multipleNullArray = a("a", null, null, "d");
		assertThrowsWithMessage(IllegalArgumentException.class, "1", () -> {
			assertVarargsNotNull("arg", multipleNullArray);
		});
	}
}

