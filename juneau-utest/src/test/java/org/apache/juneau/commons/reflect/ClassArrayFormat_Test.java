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

import static org.apache.juneau.commons.reflect.ClassArrayFormat.*;
import static org.apache.juneau.commons.reflect.ClassNameFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ClassArrayFormat}.
 */
class ClassArrayFormat_Test extends TestBase {

	//====================================================================================================
	// Enum values
	//====================================================================================================

	@Test
	void a01_enumValues_exist() {
		assertNotNull(JVM);
		assertNotNull(BRACKETS);
		assertNotNull(WORD);
		assertEquals(3, ClassArrayFormat.values().length);
	}

	@Test
	void a02_enumValueOf_works() {
		assertEquals(JVM, ClassArrayFormat.valueOf("JVM"));
		assertEquals(BRACKETS, ClassArrayFormat.valueOf("BRACKETS"));
		assertEquals(WORD, ClassArrayFormat.valueOf("WORD"));
	}

	//====================================================================================================
	// BRACKETS format - single dimension arrays
	//====================================================================================================

	@Test
	void b01_brackets_singleDimensionStringArray() {
		var ci = ClassInfo.of(String[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("String[]", result);
	}

	@Test
	void b02_brackets_singleDimensionIntArray() {
		var ci = ClassInfo.of(int[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("int[]", result);
	}

	@Test
	void b03_brackets_singleDimensionBooleanArray() {
		var ci = ClassInfo.of(boolean[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("boolean[]", result);
	}

	//====================================================================================================
	// BRACKETS format - multi-dimensional arrays
	//====================================================================================================

	@Test
	void c01_brackets_twoDimensionalStringArray() {
		var ci = ClassInfo.of(String[][].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("String[][]", result);
	}

	@Test
	void c02_brackets_threeDimensionalIntArray() {
		var ci = ClassInfo.of(int[][][].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("int[][][]", result);
	}

	@Test
	void c03_brackets_twoDimensionalBooleanArray() {
		var ci = ClassInfo.of(boolean[][].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("boolean[][]", result);
	}

	//====================================================================================================
	// WORD format - single dimension arrays
	//====================================================================================================

	@Test
	void d01_word_singleDimensionStringArray() {
		var ci = ClassInfo.of(String[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("StringArray", result);
	}

	@Test
	void d02_word_singleDimensionIntArray() {
		var ci = ClassInfo.of(int[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("intArray", result);
	}

	@Test
	void d03_word_singleDimensionBooleanArray() {
		var ci = ClassInfo.of(boolean[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("booleanArray", result);
	}

	//====================================================================================================
	// WORD format - multi-dimensional arrays
	//====================================================================================================

	@Test
	void e01_word_twoDimensionalStringArray() {
		var ci = ClassInfo.of(String[][].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("StringArrayArray", result);
	}

	@Test
	void e02_word_threeDimensionalIntArray() {
		var ci = ClassInfo.of(int[][][].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("intArrayArrayArray", result);
	}

	@Test
	void e03_word_twoDimensionalBooleanArray() {
		var ci = ClassInfo.of(boolean[][].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("booleanArrayArray", result);
	}

	//====================================================================================================
	// JVM format - uses Class.getName() which already has JVM format
	//====================================================================================================

	@Test
	void f01_jvm_singleDimensionStringArray() {
		var ci = ClassInfo.of(String[].class);
		// JVM format is already in Class.getName()
		var jvmName = String[].class.getName();
		assertEquals("[Ljava.lang.String;", jvmName);
		// When using JVM format, it should use the class name directly
		var result = ci.getNameFormatted(SIMPLE, false, '$', JVM);
		// For JVM format, it uses the inner class name which is already in JVM format
		assertTrue(result.contains("String") || result.equals(jvmName));
	}

	@Test
	void f02_jvm_singleDimensionIntArray() {
		var ci = ClassInfo.of(int[].class);
		var jvmName = int[].class.getName();
		assertEquals("[I", jvmName);
		var result = ci.getNameFormatted(SIMPLE, false, '$', JVM);
		assertTrue(result.contains("int") || result.equals(jvmName));
	}

	@Test
	void f03_jvm_twoDimensionalStringArray() {
		var ci = ClassInfo.of(String[][].class);
		var jvmName = String[][].class.getName();
		assertEquals("[[Ljava.lang.String;", jvmName);
		var result = ci.getNameFormatted(SIMPLE, false, '$', JVM);
		assertTrue(result.contains("String") || result.equals(jvmName));
	}

	//====================================================================================================
	// appendNameFormatted with different formats
	//====================================================================================================

	@Test
	void g01_appendNameFormatted_brackets_appendsCorrectly() {
		var ci = ClassInfo.of(String[][].class);
		var sb = new StringBuilder();
		ci.appendNameFormatted(sb, SIMPLE, false, '$', BRACKETS);
		assertEquals("String[][]", sb.toString());
	}

	@Test
	void g02_appendNameFormatted_word_appendsCorrectly() {
		var ci = ClassInfo.of(int[][].class);
		var sb = new StringBuilder();
		ci.appendNameFormatted(sb, SIMPLE, false, '$', WORD);
		assertEquals("intArrayArray", sb.toString());
	}

	@Test
	void g03_appendNameFormatted_brackets_withFullName() {
		var ci = ClassInfo.of(String[][].class);
		var sb = new StringBuilder();
		ci.appendNameFormatted(sb, FULL, false, '$', BRACKETS);
		assertTrue(sb.toString().contains("String[][]"));
		assertTrue(sb.toString().contains("java.lang"));
	}

	@Test
	void g04_appendNameFormatted_word_withFullName() {
		var ci = ClassInfo.of(Integer[][].class);
		var sb = new StringBuilder();
		ci.appendNameFormatted(sb, FULL, false, '$', WORD);
		assertTrue(sb.toString().contains("ArrayArray"));
		assertTrue(sb.toString().contains("Integer"));
	}

	//====================================================================================================
	// Non-array classes (should not be affected by array format)
	//====================================================================================================

	@Test
	void h01_nonArrayClass_brackets_returnsNormalName() {
		var ci = ClassInfo.of(String.class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("String", result);
	}

	@Test
	void h02_nonArrayClass_word_returnsNormalName() {
		var ci = ClassInfo.of(Integer.class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("Integer", result);
	}

	@Test
	void h03_nonArrayClass_jvm_returnsNormalName() {
		var ci = ClassInfo.of(String.class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', JVM);
		assertEquals("String", result);
	}

	//====================================================================================================
	// Primitive arrays with different formats
	//====================================================================================================

	@Test
	void i01_primitiveArrays_brackets() {
		assertEquals("byte[]", ClassInfo.of(byte[].class).getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("short[]", ClassInfo.of(short[].class).getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("char[]", ClassInfo.of(char[].class).getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("long[]", ClassInfo.of(long[].class).getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("float[]", ClassInfo.of(float[].class).getNameFormatted(SIMPLE, false, '$', BRACKETS));
		assertEquals("double[]", ClassInfo.of(double[].class).getNameFormatted(SIMPLE, false, '$', BRACKETS));
	}

	@Test
	void i02_primitiveArrays_word() {
		assertEquals("byteArray", ClassInfo.of(byte[].class).getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("shortArray", ClassInfo.of(short[].class).getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("charArray", ClassInfo.of(char[].class).getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("longArray", ClassInfo.of(long[].class).getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("floatArray", ClassInfo.of(float[].class).getNameFormatted(SIMPLE, false, '$', WORD));
		assertEquals("doubleArray", ClassInfo.of(double[].class).getNameFormatted(SIMPLE, false, '$', WORD));
	}

	//====================================================================================================
	// Complex array types
	//====================================================================================================

	@Test
	void j01_complexArray_brackets() {
		var ci = ClassInfo.of(java.util.List[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		assertEquals("List[]", result);
	}

	@Test
	void j02_complexArray_word() {
		var ci = ClassInfo.of(java.util.Map[].class);
		var result = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("MapArray", result);
	}

	@Test
	void j03_complexArray_multiDimensional() {
		var ci = ClassInfo.of(java.util.List[][].class);
		var bracketsResult = ci.getNameFormatted(SIMPLE, false, '$', BRACKETS);
		var wordResult = ci.getNameFormatted(SIMPLE, false, '$', WORD);
		assertEquals("List[][]", bracketsResult);
		assertEquals("ListArrayArray", wordResult);
	}
}

