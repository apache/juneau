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
package org.apache.juneau.common.utils;

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link AssertionUtils}.
 */
class AssertionUtils_Test extends TestBase {

	//====================================================================================================
	// assertArg(boolean, String, Object...)
	//====================================================================================================
	@Test
	void a01_assertArg_true() {
		// Should not throw
		assertArg(true, "Should not throw");
		assertArg(true, "Message with {0}", "arg");
	}

	@Test
	void a02_assertArg_false() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Test message", () -> {
			assertArg(false, "Test message");
		});
	}

	@Test
	void a03_assertArg_false_withArgs() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("Test message", "arg1"), () -> {
			assertArg(false, "Test message {0}", "arg1");
		});
	}

	@Test
	void a04_assertArg_false_withMultipleArgs() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Test", () -> {
			assertArg(false, "Test {0} {1} {2}", "a", "b", "c");
		});
	}

	//====================================================================================================
	// assertArgNotNull(String, T)
	//====================================================================================================
	@Test
	void b01_assertArgNotNull_notNull() {
		var value = "test";
		var result = assertArgNotNull("arg", value);
		assertSame(value, result);
	}

	@Test
	void b02_assertArgNotNull_null() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be null"), () -> {
			assertArgNotNull("arg", null);
		});
	}

	@Test
	void b03_assertArgNotNull_returnsValue() {
		var obj = new Object();
		var result = assertArgNotNull("arg", obj);
		assertSame(obj, result);
	}

	@Test
	void b04_assertArgNotNull_differentTypes() {
		var i = 123;
		var result = assertArgNotNull("int", i);
		assertEquals(i, result);

		var d = 45.6;
		var result2 = assertArgNotNull("double", d);
		assertEquals(d, result2);
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object) - 2 args
	//====================================================================================================
	@Test
	void c01_assertArgsNotNull2_bothNotNull() {
		// Should not throw
		assertArgsNotNull("arg1", "value1", "arg2", "value2");
	}

	@Test
	void c02_assertArgsNotNull2_firstNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg1", "cannot be null"), () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2");
		});
	}

	@Test
	void c03_assertArgsNotNull2_secondNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg2", "cannot be null"), () -> {
			assertArgsNotNull("arg1", "value1", "arg2", null);
		});
	}

	@Test
	void c04_assertArgsNotNull2_bothNull() {
		// Should fail on first null
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", null);
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object, String, Object) - 3 args
	//====================================================================================================
	@Test
	void d01_assertArgsNotNull3_allNotNull() {
		// Should not throw
		assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3");
	}

	@Test
	void d02_assertArgsNotNull3_firstNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2", "arg3", "value3");
		});
	}

	@Test
	void d03_assertArgsNotNull3_secondNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg2", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", null, "arg3", "value3");
		});
	}

	@Test
	void d04_assertArgsNotNull3_thirdNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg3", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", null);
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object, String, Object, String, Object) - 4 args
	//====================================================================================================
	@Test
	void e01_assertArgsNotNull4_allNotNull() {
		// Should not throw
		assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", "value4");
	}

	@Test
	void e02_assertArgsNotNull4_firstNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2", "arg3", "value3", "arg4", "value4");
		});
	}

	@Test
	void e03_assertArgsNotNull4_fourthNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg4", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", null);
		});
	}

	//====================================================================================================
	// assertArgsNotNull(String, Object, String, Object, String, Object, String, Object, String, Object) - 5 args
	//====================================================================================================
	@Test
	void f01_assertArgsNotNull5_allNotNull() {
		// Should not throw
		assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", "value4", "arg5", "value5");
	}

	@Test
	void f02_assertArgsNotNull5_firstNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg1", () -> {
			assertArgsNotNull("arg1", null, "arg2", "value2", "arg3", "value3", "arg4", "value4", "arg5", "value5");
		});
	}

	@Test
	void f03_assertArgsNotNull5_fifthNull() {
		assertThrowsWithMessage(IllegalArgumentException.class, "arg5", () -> {
			assertArgsNotNull("arg1", "value1", "arg2", "value2", "arg3", "value3", "arg4", "value4", "arg5", null);
		});
	}

	//====================================================================================================
	// assertArgNotNullOrBlank(String, String)
	//====================================================================================================
	@Test
	void g01_assertArgNotNullOrBlank_valid() {
		var value = "test";
		var result = assertArgNotNullOrBlank("arg", value);
		assertSame(value, result);
	}

	@Test
	void g02_assertArgNotNullOrBlank_null() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be null"), () -> {
			assertArgNotNullOrBlank("arg", null);
		});
	}

	@Test
	void g03_assertArgNotNullOrBlank_empty() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be blank"), () -> {
			assertArgNotNullOrBlank("arg", "");
		});
	}

	@Test
	void g04_assertArgNotNullOrBlank_whitespace() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be blank"), () -> {
			assertArgNotNullOrBlank("arg", "   ");
		});
	}

	@Test
	void g05_assertArgNotNullOrBlank_tab() {
		assertThrowsWithMessage(IllegalArgumentException.class, "cannot be blank", () -> {
			assertArgNotNullOrBlank("arg", "\t");
		});
	}

	@Test
	void g06_assertArgNotNullOrBlank_newline() {
		assertThrowsWithMessage(IllegalArgumentException.class, "cannot be blank", () -> {
			assertArgNotNullOrBlank("arg", "\n");
		});
	}

	@Test
	void g07_assertArgNotNullOrBlank_returnsValue() {
		var value = "test";
		var result = assertArgNotNullOrBlank("arg", value);
		assertSame(value, result);
	}

	//====================================================================================================
	// assertClassArrayArgIsType(String, Class<E>, Class<?>[])
	//====================================================================================================
	@Test
	void h01_assertClassArrayArgIsType_valid() {
		var classes = a(String.class, Object.class);
		var result = assertClassArrayArgIsType("arg", Object.class, classes);
		assertSame(classes, result);
	}

	@Test
	void h02_assertClassArrayArgIsType_emptyArray() {
		var classes = new Class<?>[0];
		var result = assertClassArrayArgIsType("arg", Object.class, classes);
		assertSame(classes, result);
	}

	@Test
	void h03_assertClassArrayArgIsType_invalidType() {
		var classes = a(String.class, Integer.class);
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "String"), () -> {
			assertClassArrayArgIsType("arg", Number.class, classes);
		});
	}

	@Test
	void h04_assertClassArrayArgIsType_invalidTypeAtIndex() {
		var classes = a(String.class, Integer.class, Double.class);
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "index", "0"), () -> {
			assertClassArrayArgIsType("arg", Number.class, classes);
		});
	}

	@Test
	void h05_assertClassArrayArgIsType_subclass() {
		var classes = a(Integer.class, Double.class);
		var result = assertClassArrayArgIsType("arg", Number.class, classes);
		assertSame(classes, result);
	}

	@Test
	void h06_assertClassArrayArgIsType_sameClass() {
		var classes = a(String.class);
		var result = assertClassArrayArgIsType("arg", String.class, classes);
		assertSame(classes, result);
	}

	//====================================================================================================
	// assertVarargsNotNull(String, T[])
	//====================================================================================================
	@Test
	void i01_assertVarargsNotNull_valid() {
		var array = a("a", "b", "c");
		var result = assertVarargsNotNull("arg", array);
		assertSame(array, result);
	}

	@Test
	void i02_assertVarargsNotNull_emptyArray() {
		var array = new String[0];
		var result = assertVarargsNotNull("arg", array);
		assertSame(array, result);
	}

	@Test
	void i03_assertVarargsNotNull_nullArray() {
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "cannot be null"), () -> {
			assertVarargsNotNull("arg", (String[])null);
		});
	}

	@Test
	void i04_assertVarargsNotNull_nullElement() {
		var array = a("a", null, "c");
		assertThrowsWithMessage(IllegalArgumentException.class, l("arg", "parameter", "1"), () -> {
			assertVarargsNotNull("arg", array);
		});
	}

	@Test
	void i05_assertVarargsNotNull_multipleNullElements() {
		var array = a("a", null, null, "d");
		// Should fail on first null
		assertThrowsWithMessage(IllegalArgumentException.class, "1", () -> {
			assertVarargsNotNull("arg", array);
		});
	}

	@Test
	void i06_assertVarargsNotNull_returnsArray() {
		var array = a(1, 2, 3);
		var result = assertVarargsNotNull("arg", array);
		assertSame(array, result);
	}

	@Test
	void i07_assertVarargsNotNull_objectArray() {
		var array = a(new Object(), new Object());
		var result = assertVarargsNotNull("arg", array);
		assertSame(array, result);
	}

	//====================================================================================================
	// assertOneOf(T, T...)
	//====================================================================================================
	@Test
	void j01_assertOneOf() {
		assertEquals("test", assertOneOf("test", "test", "other"));
		assertEquals(123, assertOneOf(123, 123, 456));
		assertEquals("a", assertOneOf("a", "a", "b", "c"));
	}

	@Test
	void j02_assertOneOf_matches() {
		// Exact match
		assertEquals("test", assertOneOf("test", "test"));
		assertEquals(1, assertOneOf(1, 1, 2, 3));

		// Match in middle
		assertEquals(2, assertOneOf(2, 1, 2, 3));

		// Match at end
		assertEquals(3, assertOneOf(3, 1, 2, 3));
	}

	@Test
	void j03_assertOneOf_nulls() {
		assertNull(assertOneOf(null, null, "test"));
		assertNull(assertOneOf(null, "test", null));
	}

	@Test
	void j04_assertOneOf_fails() {
		assertThrowsWithMessage(AssertionError.class, l("Invalid value specified", "test"), () -> assertOneOf("test", "other"));
	}

	@Test
	void j05_assertOneOf_fails_multiple() {
		assertThrowsWithMessage(AssertionError.class, l("Invalid value specified", "test"), () -> assertOneOf("test", "a", "b", "c"));
	}

	@Test
	void j06_assertOneOf_numbers() {
		assertEquals(5, assertOneOf(5, 1, 2, 3, 4, 5));
		assertThrows(AssertionError.class, () -> assertOneOf(10, 1, 2, 3, 4, 5));
	}

	@Test
	void j07_assertOneOf_emptyExpected() {
		assertThrowsWithMessage(AssertionError.class, "Invalid value specified", () -> assertOneOf("test"));
	}

	@Test
	void j08_assertOneOf_objects() {
		var obj1 = new Object();
		var obj2 = new Object();
		var result = assertOneOf(obj1, obj1, obj2);
		assertSame(obj1, result);
	}

	@Test
	void j09_assertOneOf_returnsActual() {
		var value = "test";
		var result = assertOneOf(value, "test", "other");
		assertSame(value, result);
	}
}

