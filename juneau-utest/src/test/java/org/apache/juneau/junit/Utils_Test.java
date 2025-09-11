// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.junit;

import static org.apache.juneau.junit.Utils.*;
import static org.apache.juneau.junit.Assertions2.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.junit.jupiter.api.*;

/**
 * Comprehensive unit tests for the {@link Utils} utility class.
 *
 * <p>This test class validates all utility methods for correct behavior, edge cases,
 * and error conditions. Tests are organized by functional groups matching the
 * utility method categories.
 */
@DisplayName("Utility Methods")
class Utils_Test extends TestBase {

	// ====================================================================================================
	// Array Conversion Tests
	// ====================================================================================================

	@Test
	@DisplayName("arrayToList() - Basic array conversion")
	void a01_arrayToListBasicConversion() {
		// Test with integer array
		int[] intArray = {1, 2, 3, 4, 5};
		var result = arrayToList(intArray);
		assertEquals(5, result.size());
		assertEquals(1, result.get(0));
		assertEquals(2, result.get(1));
		assertEquals(3, result.get(2));
		assertEquals(4, result.get(3));
		assertEquals(5, result.get(4));

		// Test with string array
		String[] stringArray = {"hello", "world", "test"};
		result = arrayToList(stringArray);
		assertEquals(3, result.size());
		assertEquals("hello", result.get(0));
		assertEquals("world", result.get(1));
		assertEquals("test", result.get(2));
	}

	@Test
	@DisplayName("arrayToList() - Empty and single element arrays")
	void a02_arrayToListEdgeCases() {
		// Empty array
		int[] emptyArray = {};
		var result = arrayToList(emptyArray);
		assertEmpty(result);

		// Single element
		String[] singleElement = {"single"};
		result = arrayToList(singleElement);
		assertList(result, "single");
	}

	@Test
	@DisplayName("arrayToList() - Null values in array")
	void a03_arrayToListWithNulls() {
		String[] arrayWithNulls = {"first", null, "third", null};
		var result = arrayToList(arrayWithNulls);
		assertEquals(4, result.size());
		assertEquals("first", result.get(0));
		assertNull(result.get(1));
		assertEquals("third", result.get(2));
		assertNull(result.get(3));
	}

	@Test
	@DisplayName("arrayToList() - Different primitive types")
	void a04_arrayToListPrimitiveTypes() {
		// Boolean array
		boolean[] boolArray = {true, false, true};
		var result = arrayToList(boolArray);
		assertList(result, "true","false","true");

		// Double array
		double[] doubleArray = {1.5, 2.7, 3.14};
		result = arrayToList(doubleArray);
		assertList(result, "1.5","2.7","3.14");
	}

	// ====================================================================================================
	// Argument Validation Tests
	// ====================================================================================================

	@Test
	@DisplayName("assertArg() - Valid expressions")
	void b01_assertArgValidExpressions() {
		// Should not throw
		assertDoesNotThrow(() -> assertArg(true, "Should not fail"));
		assertDoesNotThrow(() -> assertArg(5 > 3, "Math should work"));
		assertDoesNotThrow(() -> assertArg("test".length() == 4, "Length check"));
	}

	@Test
	@DisplayName("assertArg() - Invalid expressions")
	void b02_assertArgInvalidExpressions() {
		// Simple false expression
		var e = assertThrows(IllegalArgumentException.class, () -> assertArg(false, "This should fail"));
		assertEquals("This should fail", e.getMessage());

		// False expression with parameters
		e = assertThrows(IllegalArgumentException.class, () -> assertArg(5 < 3, "Value {0} should be greater than {1}", 5, 3));
		assertEquals("Value 5 should be greater than 3", e.getMessage());
	}

	@Test
	@DisplayName("assertArg() - Message formatting")
	void b03_assertArgMessageFormatting() {
		var e = assertThrows(IllegalArgumentException.class, () -> assertArg(false, "User {0} has invalid age {1}", "Alice", -5));
		assertEquals("User Alice has invalid age -5", e.getMessage());
	}

	@Test
	@DisplayName("assertArgNotNull() - Valid arguments")
	void b04_assertArgNotNullValid() {
		var result = assertArgNotNull("testParam", "validValue");
		assertEquals("validValue", result);

		var intResult = assertArgNotNull("number", 42);
		assertEquals(Integer.valueOf(42), intResult);

		var list = new ArrayList<>();
		var listResult = assertArgNotNull("list", list);
		assertSame(list, listResult);
	}

	@Test
	@DisplayName("assertArgNotNull() - Null arguments")
	void b05_assertArgNotNullInvalid() {
		var e = assertThrows(IllegalArgumentException.class, () -> assertArgNotNull("username", null));
		assertEquals("Argument 'username' cannot be null.", e.getMessage());

		e = assertThrows(IllegalArgumentException.class, () -> assertArgNotNull("data", null));
		assertEquals("Argument 'data' cannot be null.", e.getMessage());
	}

	// ====================================================================================================
	// Assertion Error Creation Tests
	// ====================================================================================================

	@Test
	@DisplayName("assertEqualsFailed() - Basic error creation")
	void c01_assertEqualsFailedBasic() {
		var error = assertEqualsFailed("expected", "actual", null);
		assertContains("expected: <expected>", error.getMessage());
		assertContains("but was: <actual>", error.getMessage());
		assertEquals("expected", error.getExpected().getValue());
		assertEquals("actual", error.getActual().getValue());
	}

	@Test
	@DisplayName("assertEqualsFailed() - With custom message")
	void c02_assertEqualsFailedWithMessage() {
		var msgSupplier = fs("Custom context message");
		var error = assertEqualsFailed(100, 200, msgSupplier);
		assertContains("Custom context message", error.getMessage());
	}

	@Test
	@DisplayName("assertEqualsFailed() - Null values")
	void c03_assertEqualsFailedNullValues() {
		var error = assertEqualsFailed(null, "actual", null);
		assertContains("expected: <null>", error.getMessage());
		assertContains("but was: <actual>", error.getMessage());

		error = assertEqualsFailed("expected", null, null);
		assertContains("expected: <expected>", error.getMessage());
		assertContains("but was: <null>", error.getMessage());
	}

	// ====================================================================================================
	// Equality Testing Tests
	// ====================================================================================================

	@Test
	@DisplayName("eq() - Basic equality with Objects.equals()")
	void d01_eqBasicEquality() {
		assertTrue(eq("hello", "hello"));
		assertTrue(eq(null, null));
		assertTrue(eq(42, 42));

		assertFalse(eq("hello", "world"));
		assertFalse(eq(null, "test"));
		assertFalse(eq("test", null));
		assertFalse(eq(42, 43));
	}

	@Test
	@DisplayName("eq() - Custom predicate equality")
	void d02_eqCustomPredicate() {
		// Case-insensitive string comparison
		assertTrue(eq("HELLO", "hello", (s1, s2) -> s1.equalsIgnoreCase(s2)));
		assertFalse(eq("HELLO", "world", (s1, s2) -> s1.equalsIgnoreCase(s2)));

		// Custom object comparison by ID
		var bean1 = new TestBean("Alice", 25, true);
		var bean2 = new TestBean("Bob", 25, false);
		assertTrue(eq(bean1, bean2, (b1, b2) -> b1.getAge() == b2.getAge()));
		assertFalse(eq(bean1, bean2, (b1, b2) -> b1.getName().equals(b2.getName())));
	}

	@Test
	@DisplayName("eq() - Null handling with custom predicate")
	void d03_eqCustomPredicateNulls() {
		// Both null
		assertTrue(eq(null, null, (s1, s2) -> s1.equals(s2)));

		// One null
		assertFalse(eq(null, "test", (s1, s2) -> s1.equals(s2)));
		assertFalse(eq("test", null, (s1, s2) -> s1.equals(s2)));
	}

	@Test
	@DisplayName("eq() - Reference equality optimization")
	void d04_eqReferenceEquality() {
		var same = "test";
		assertTrue(eq(same, same, (s1, s2) -> { throw new RuntimeException("Should not be called"); }));
	}

	@Test
	@DisplayName("ne() - Negation of equality")
	void d05_neNegation() {
		assertTrue(ne("hello", "world"));
		assertTrue(ne(null, "test"));
		assertTrue(ne("test", null));

		assertFalse(ne("hello", "hello"));
		assertFalse(ne(null, null));
		assertFalse(ne(42, 42));
	}

	// ====================================================================================================
	// String Escaping Tests
	// ====================================================================================================

	@Test
	@DisplayName("escapeForJava() - Basic escape sequences")
	void e01_escapeForJavaBasicEscapes() {
		assertEquals("\\\"", escapeForJava("\""));
		assertEquals("\\\\", escapeForJava("\\"));
		assertEquals("\\n", escapeForJava("\n"));
		assertEquals("\\r", escapeForJava("\r"));
		assertEquals("\\t", escapeForJava("\t"));
		assertEquals("\\f", escapeForJava("\f"));
		assertEquals("\\b", escapeForJava("\b"));
	}

	@Test
	@DisplayName("escapeForJava() - Combined sequences")
	void e02_escapeForJavaCombined() {
		var input = "Hello\nWorld\"Test\"";
		var expected = "Hello\\nWorld\\\"Test\\\"";
		assertEquals(expected, escapeForJava(input));

		input = "Line1\r\nLine2\tTabbed";
		expected = "Line1\\r\\nLine2\\tTabbed";
		assertEquals(expected, escapeForJava(input));
	}

	@Test
	@DisplayName("escapeForJava() - Unicode escapes")
	void e03_escapeForJavaUnicode() {
		// Control characters
		assertEquals("\\u0001", escapeForJava("\u0001"));
		assertEquals("\\u001f", escapeForJava("\u001f"));

		// Non-ASCII characters
		assertEquals("\\u007f", escapeForJava("\u007f"));
		assertEquals("\\u0080", escapeForJava("\u0080"));
		assertEquals("\\u00ff", escapeForJava("\u00ff"));
	}

	@Test
	@DisplayName("escapeForJava() - Normal characters unchanged")
	void e04_escapeForJavaNormalChars() {
		var normal = "ABCabc123!@#$%^&*()_+-=[]{}|;':,.<>?";
		assertEquals(normal, escapeForJava(normal));
	}

	@Test
	@DisplayName("escapeForJava() - Empty string")
	void e05_escapeForJavaEmpty() {
		assertEquals("", escapeForJava(""));
	}

	// ====================================================================================================
	// Message Formatting Tests
	// ====================================================================================================

	@Test
	@DisplayName("f() - No parameters")
	void f01_fNoParameters() {
		assertEquals("Simple message", f("Simple message"));
		assertEquals("", f(""));
	}

	@Test
	@DisplayName("f() - With parameters")
	void f02_fWithParameters() {
		assertEquals("User Alice has 5 items", f("User {0} has {1} items", "Alice", 5));
		assertEquals("Value: 42", f("Value: {0}", 42));
		assertEquals("a, b, c", f("{0}, {1}, {2}", "a", "b", "c"));
	}

	@Test
	@DisplayName("f() - Complex parameter types")
	void f03_fComplexParameters() {
		// Test with Boolean
		assertString(f("Flag: {0}", true), "Flag: true");
		assertString(f("Flag: {0}", false), "Flag: false");

		// Test with List
		var list = Arrays.asList("a", "b", "c");
		assertString(f("List: {0}", list), "List: [a, b, c]");

		// Test with mixed types
		assertString(f("User {0} (age {1}) is active: {2}", "Alice", 25, true), "User Alice (age 25) is active: true");
	}

	@Test
	@DisplayName("f() - Edge cases and special characters")
	void f04_fEdgeCases() {
		// Test with special characters (avoid braces in parameters for MessageFormat compatibility)
		assertEquals("Pattern: .*items=\\[a,b\\].*", f("Pattern: {0}", ".*items=\\[a,b\\].*"));
		assertEquals("Simple: no braces here", f("Simple: {0}", "no braces here"));
		assertEquals("Mixed: string with data and 42", f("Mixed: {0} and {1}", "string with data", 42));

		// Test with non-string arguments
		assertEquals("Number: 42", f("Number: {0}", 42));
		assertEquals("Boolean: true", f("Boolean: {0}", true));

		// Test that single quotes in input are properly handled (need to be doubled in MessageFormat)
		assertEquals("Text with 'single quotes'", f("Text with {0}", "'single quotes'"));
	}

	@Test
	@DisplayName("fs() - Supplier creation")
	void f05_fsSupplierCreation() {
		var supplier = fs("User {0} has {1} items", "Bob", 3);
		assertEquals("User Bob has 3 items", supplier.get());

		// Multiple calls should produce same result
		assertEquals("User Bob has 3 items", supplier.get());
	}

	@Test
	@DisplayName("fs() - No parameters supplier")
	void f06_fsNoParameters() {
		assertEquals("Static message", fs("Static message").get());
	}

	// ====================================================================================================
	// Pattern Matching Tests
	// ====================================================================================================

	@Test
	@DisplayName("getGlobMatchPattern() - Basic wildcard patterns")
	void g01_getGlobMatchPatternBasicWildcards() {
		var pattern = getGlobMatchPattern("user_*_temp");
		assertTrue(pattern.matcher("user_alice_temp").matches());
		assertTrue(pattern.matcher("user_bob_temp").matches());
		assertTrue(pattern.matcher("user__temp").matches());  // Empty match
		assertFalse(pattern.matcher("admin_alice_temp").matches());
		assertFalse(pattern.matcher("user_alice_data").matches());
	}

	@Test
	@DisplayName("getGlobMatchPattern() - Question mark patterns")
	void g02_getGlobMatchPatternQuestionMark() {
		var pattern = getGlobMatchPattern("file?.txt");
		assertTrue(pattern.matcher("file1.txt").matches());
		assertTrue(pattern.matcher("fileA.txt").matches());
		assertTrue(pattern.matcher("file_.txt").matches());
		assertFalse(pattern.matcher("file.txt").matches());    // Missing character
		assertFalse(pattern.matcher("file12.txt").matches());  // Too many characters
	}

	@Test
	@DisplayName("getGlobMatchPattern() - Combined wildcards")
	void g03_getGlobMatchPatternCombined() {
		var pattern = getGlobMatchPattern("test_*.?");
		assertTrue(pattern.matcher("test_data.1").matches());
		assertTrue(pattern.matcher("test_long_filename.x").matches());
		assertTrue(pattern.matcher("test_.a").matches());
		assertFalse(pattern.matcher("test_data").matches());   // Missing single char
		assertFalse(pattern.matcher("test_data.ab").matches()); // Too many chars at end
	}

	@Test
	@DisplayName("getGlobMatchPattern() - Special regex characters")
	void g04_getGlobMatchPatternSpecialChars() {
		// Test that regex special characters are properly escaped
		var pattern = getGlobMatchPattern("file[1].txt");
		assertTrue(pattern.matcher("file[1].txt").matches());
		assertFalse(pattern.matcher("file1.txt").matches());   // Should not match as character class

		pattern = getGlobMatchPattern("amount$100");
		assertTrue(pattern.matcher("amount$100").matches());
		assertFalse(pattern.matcher("amount100").matches());   // Should require the $ character
	}

	@Test
	@DisplayName("getGlobMatchPattern() - With flags")
	void g05_getGlobMatchPatternWithFlags() {
		var pattern = getGlobMatchPattern("USER_*", Pattern.CASE_INSENSITIVE);
		assertTrue(pattern.matcher("user_alice").matches());
		assertTrue(pattern.matcher("USER_BOB").matches());
		assertTrue(pattern.matcher("User_Charlie").matches());
	}

	@Test
	@DisplayName("getGlobMatchPattern() - Null input")
	void g06_getGlobMatchPatternNull() {
		assertNull(getGlobMatchPattern(null));
		assertNull(getGlobMatchPattern(null, Pattern.CASE_INSENSITIVE));
	}

	@Test
	@DisplayName("getGlobMatchPattern() - Empty and edge cases")
	void g07_getGlobMatchPatternEdgeCases() {
		// Empty string
		var pattern = getGlobMatchPattern("");
		assertTrue(pattern.matcher("").matches());
		assertFalse(pattern.matcher("a").matches());

		// Only wildcards
		pattern = getGlobMatchPattern("*");
		assertTrue(pattern.matcher("").matches());
		assertTrue(pattern.matcher("anything").matches());

		pattern = getGlobMatchPattern("?");
		assertTrue(pattern.matcher("a").matches());
		assertFalse(pattern.matcher("").matches());
		assertFalse(pattern.matcher("ab").matches());
	}

	// ====================================================================================================
	// Safe Execution Tests
	// ====================================================================================================

	@Test
	@DisplayName("safe() - Successful execution")
	void h01_safeSuccessfulExecution() {
		assertEquals("success", safe(() -> "success"));
		assertEquals(Integer.valueOf(42), safe(() -> 42));
	}

	@Test
	@DisplayName("safe() - Runtime exception passthrough")
	void h02_safeRuntimeExceptionPassthrough() {
		var original = new RuntimeException("Test runtime exception");
		var thrown = assertThrows(RuntimeException.class, () -> safe(() -> { throw original; }));
		assertSame(original, thrown);

		var argException = new IllegalArgumentException("Invalid arg");
		var thrownArg = assertThrows(IllegalArgumentException.class, () -> safe(() -> { throw argException; }));
		assertSame(argException, thrownArg);
	}

	@Test
	@DisplayName("safe() - Checked exception wrapping")
	void h03_safeCheckedExceptionWrapping() {
		var checkedException = new Exception("Checked exception");
		var thrown = assertThrows(RuntimeException.class, () -> safe(() -> { throw checkedException; }));
		assertSame(checkedException, thrown.getCause());
		assertEquals(RuntimeException.class, thrown.getClass());
	}

	// ====================================================================================================
	// Type Name Tests
	// ====================================================================================================

	@Test
	@DisplayName("t() - Various object types")
	void i01_tVariousTypes() {
		assertString(t("hello"), "String");
		assertString(t(42), "Integer");
		assertString(t(new ArrayList<>()), "ArrayList");
		assertString(t(new HashMap<>()), "HashMap");
		assertString(t(Pattern.compile("test")), "Pattern");
	}

	@Test
	@DisplayName("t() - Null input")
	void i02_tNullInput() {
		assertNull(t(null));
	}

	@Test
	@DisplayName("t() - Array types")
	void i03_tArrayTypes() {
		assertString(t(new String[0]), "String[]");
		assertString(t(new int[0]), "int[]");
		assertString(t(new Object[0]), "Object[]");
	}

	// ====================================================================================================
	// Tokenization Tests
	// ====================================================================================================

	@Test
	@DisplayName("tokenize() - Basic tokenization")
	void j01_tokenizeBasic() {
		// This test assumes NestedTokenizer.tokenize() works correctly
		// We're just testing the delegation
		assertNotNull(tokenize("name,age"));
		// The actual behavior depends on NestedTokenizer implementation
	}

	@Test
	@DisplayName("tokenize() - Nested fields")
	void j02_tokenizeNested() {
		// Test with nested structure
		assertNotNull(tokenize("name,address{street,city}"));
		// The actual behavior depends on NestedTokenizer implementation
	}

	// ====================================================================================================
	// Test Helper Classes
	// ====================================================================================================

	/**
	 * Simple test bean for testing purposes.
	 */
	public static class TestBean {
		private final String name;
		private final int age;
		private final boolean active;

		public TestBean(String name, int age, boolean active) {
			this.name = name;
			this.age = age;
			this.active = active;
		}

		public String getName() { return name; }
		public int getAge() { return age; }
		public boolean isActive() { return active; }

		@Override
		public boolean equals(Object obj) {
		return (obj instanceof TestBean other) && eq(this, other, (x,y) ->
			x.age == y.age && x.active == y.active && eq(x.name, y.name));
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, age, active);
		}

		@Override
		public String toString() {
			return f("TestBean{name=''{0}'', age=''{1}'', active=''{2}''}", name, age, active);
		}
	}
}
