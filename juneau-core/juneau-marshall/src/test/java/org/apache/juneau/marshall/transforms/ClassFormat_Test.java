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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class ClassFormat_Test {

	//------------------------------------------------------------------------------------------------------------------
	// format
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_format_fqcn_topLevel() {
		assertEquals("java.lang.String", ClassFormat.format(String.class, ClassFormat.FQCN));
		assertEquals("java.util.Map", ClassFormat.format(Map.class, ClassFormat.FQCN));
	}

	@Test void a02_format_fqcn_nested() {
		// Map.Entry is nested — FQCN uses canonical dotted form.
		assertEquals("java.util.Map.Entry", ClassFormat.format(Map.Entry.class, ClassFormat.FQCN));
	}

	@Test void a03_format_fqcn_primitive() {
		assertEquals("int", ClassFormat.format(int.class, ClassFormat.FQCN));
	}

	@Test void a04_format_fqcn_array() {
		assertEquals("int[]", ClassFormat.format(int[].class, ClassFormat.FQCN));
		assertEquals("java.lang.String[]", ClassFormat.format(String[].class, ClassFormat.FQCN));
	}

	@Test void a05_format_notSetFallsThroughToFqcn() {
		assertEquals("java.lang.String", ClassFormat.format(String.class, ClassFormat.NOT_SET));
	}

	@Test void a06_format_nullFormatTreatedAsFqcn() {
		assertEquals("java.lang.String", ClassFormat.format(String.class, null));
	}

	@Test void a07_format_binaryName_nested() {
		// Map.Entry binary name uses $ separator.
		assertEquals("java.util.Map$Entry", ClassFormat.format(Map.Entry.class, ClassFormat.BINARY_NAME));
	}

	@Test void a08_format_binaryName_array() {
		assertEquals("[I", ClassFormat.format(int[].class, ClassFormat.BINARY_NAME));
		assertEquals("[Ljava.lang.String;", ClassFormat.format(String[].class, ClassFormat.BINARY_NAME));
	}

	@Test void a09_format_simpleName() {
		assertEquals("String", ClassFormat.format(String.class, ClassFormat.SIMPLE_NAME));
		assertEquals("Entry", ClassFormat.format(Map.Entry.class, ClassFormat.SIMPLE_NAME));
		assertEquals("int", ClassFormat.format(int.class, ClassFormat.SIMPLE_NAME));
	}

	@Test void a10_format_null() {
		for (var f : ClassFormat.values())
			assertNull(ClassFormat.format(null, f), "format=" + f);
	}

	@Test void a11_isNumeric() {
		for (var f : ClassFormat.values())
			assertFalse(f.isNumeric(), "format=" + f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parse
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_parse_fqcn_topLevel() {
		assertEquals(String.class, ClassFormat.parse("java.lang.String", ClassFormat.FQCN, null));
	}

	@Test void b02_parse_fqcn_nested_dottedFormFallback() {
		// FQCN uses dotted nested form — Class.forName won't find it directly, but the fallback retries with $.
		assertEquals(Map.Entry.class, ClassFormat.parse("java.util.Map.Entry", ClassFormat.FQCN, null));
	}

	@Test void b03_parse_binaryName_nested() {
		assertEquals(Map.Entry.class, ClassFormat.parse("java.util.Map$Entry", ClassFormat.BINARY_NAME, null));
	}

	@Test void b04_parse_binaryName_array() {
		assertEquals(int[].class, ClassFormat.parse("[I", ClassFormat.BINARY_NAME, null));
		assertEquals(String[].class, ClassFormat.parse("[Ljava.lang.String;", ClassFormat.BINARY_NAME, null));
	}

	@Test void b05_parse_simpleName_unsupportedThrows() {
		assertThrows(UnsupportedOperationException.class, () -> ClassFormat.parse("String", ClassFormat.SIMPLE_NAME, null));
	}

	@Test void b06_parse_nullAndBlank() {
		for (var f : ClassFormat.values()) {
			assertNull(ClassFormat.parse(null, f, null), "format=" + f);
			assertNull(ClassFormat.parse("", f, null), "format=" + f);
			assertNull(ClassFormat.parse("   ", f, null), "format=" + f);
		}
	}

	@Test void b07_parse_explicitClassLoader() {
		var cl = String.class.getClassLoader();
		assertEquals(String.class, ClassFormat.parse("java.lang.String", ClassFormat.FQCN, cl));
	}

	@Test void b07b_parse_nullFormatHint() {
		assertEquals(String.class, ClassFormat.parse("java.lang.String", null, null));
	}

	@Test void b08_parse_invalidClassThrows() {
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("no.such.Class", ClassFormat.FQCN, null));
	}

	@Test void b09_parse_lenient_dottedNestedAcrossFormats() {
		// Lenient parsing across non-SIMPLE_NAME hints should handle both shapes.
		for (var hint : new ClassFormat[] { ClassFormat.NOT_SET, ClassFormat.FQCN, ClassFormat.BINARY_NAME }) {
			assertEquals(Map.Entry.class, ClassFormat.parse("java.util.Map$Entry", hint, null), "hint=" + hint);
			assertEquals(Map.Entry.class, ClassFormat.parse("java.util.Map.Entry", hint, null), "hint=" + hint);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Round-trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_roundTrip_fqcn() {
		for (var c : new Class<?>[] { String.class, Integer.class, Map.class, Map.Entry.class }) {
			var s = ClassFormat.format(c, ClassFormat.FQCN);
			assertEquals(c, ClassFormat.parse(s, ClassFormat.FQCN, null), "class=" + c);
		}
	}

	@Test void c02_roundTrip_binaryName() {
		for (var c : new Class<?>[] { String.class, Map.Entry.class, int[].class, String[].class }) {
			var s = ClassFormat.format(c, ClassFormat.BINARY_NAME);
			assertEquals(c, ClassFormat.parse(s, ClassFormat.BINARY_NAME, null), "class=" + c);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Nested-type fallback heuristics
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parse_dottedSegmentStartingWithLowerCase_noFallback() {
		// "java.util" — lastDot at index 4, trailing 'u' is lowercase, so heuristic skips and original parse fails.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("java.util.notaclass", ClassFormat.FQCN, null));
	}

	@Test void d02_parse_noDotAtAll() {
		// No dot — nested heuristic returns null immediately.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("NoSuchClass", ClassFormat.FQCN, null));
	}

	@Test void d03_parse_leadingDot_noFallback() {
		// lastDot == 0 — heuristic returns null.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse(".LeadingDot", ClassFormat.FQCN, null));
	}

	@Test void d04_parse_trailingDot_noFallback() {
		// lastDot == s.length() - 1 — heuristic returns null.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("java.util.", ClassFormat.FQCN, null));
	}

	@Test void d05_parse_fallbackToBinaryButStillNotFound() {
		// "no.such.Nested" — heuristic kicks in (Nested starts with uppercase, fallback = "no.such$Nested") but
		// the fallback also fails to resolve, so the original error is thrown.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("no.such.Nested", ClassFormat.FQCN, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// format edge cases
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_format_localAnonymousFallback() {
		// Anonymous inner class — getCanonicalName() returns null, format falls back to getName().
		Runnable r = () -> {};
		var cls = r.getClass();
		assertNull(cls.getCanonicalName());
		assertNotNull(ClassFormat.format(cls, ClassFormat.FQCN));
		assertEquals(cls.getName(), ClassFormat.format(cls, ClassFormat.FQCN));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Array-suffix resolution (resolveWithArraySuffix branch coverage)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_parse_fqcnArray_primitive() {
		// "int[]" — dims=1, leaf="int" routes through primitiveByName.
		assertEquals(int[].class, ClassFormat.parse("int[]", ClassFormat.FQCN, null));
	}

	@Test void f02_parse_fqcnArray_object() {
		// "java.lang.String[]" — dims=1, leaf="java.lang.String" routes through Class.forName.
		assertEquals(String[].class, ClassFormat.parse("java.lang.String[]", ClassFormat.FQCN, null));
	}

	@Test void f03_parse_fqcnArray_multiDim() {
		// "java.lang.String[][]" — dims=2, exercises the array-dim rebuild loop more than once.
		assertEquals(String[][].class, ClassFormat.parse("java.lang.String[][]", ClassFormat.FQCN, null));
		assertEquals(int[][].class, ClassFormat.parse("int[][]", ClassFormat.FQCN, null));
	}

	@Test void f04_parse_fqcnArray_nestedType() {
		// "java.util.Map.Entry[]" — dims=1, leaf "java.util.Map.Entry" goes through nested-type fallback.
		assertEquals(Map.Entry[].class, ClassFormat.parse("java.util.Map.Entry[]", ClassFormat.FQCN, null));
	}

	@Test void f05_parse_fqcnArray_emptyLeafThrows() {
		// "[]" — dims=1, leaf="" — should throw because leaf is empty after stripping.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("[]", ClassFormat.FQCN, null));
	}

	@Test void f06_parse_fqcnArray_unknownLeafThrows() {
		// "no.such.Class[]" — array form with unresolvable leaf; should surface IllegalArgumentException.
		assertThrows(IllegalArgumentException.class, () -> ClassFormat.parse("no.such.Class[]", ClassFormat.FQCN, null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Primitive-name resolution (primitiveByName branch coverage)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_parse_fqcnPrimitive_allLeafs_topLevel() {
		// resolveLeaf consults primitiveByName before falling through to Class.forName, so every
		// primitive-name top-level leaf must resolve through the table.
		assertEquals(boolean.class, ClassFormat.parse("boolean", ClassFormat.FQCN, null));
		assertEquals(byte.class, ClassFormat.parse("byte", ClassFormat.FQCN, null));
		assertEquals(char.class, ClassFormat.parse("char", ClassFormat.FQCN, null));
		assertEquals(short.class, ClassFormat.parse("short", ClassFormat.FQCN, null));
		assertEquals(int.class, ClassFormat.parse("int", ClassFormat.FQCN, null));
		assertEquals(long.class, ClassFormat.parse("long", ClassFormat.FQCN, null));
		assertEquals(float.class, ClassFormat.parse("float", ClassFormat.FQCN, null));
		assertEquals(double.class, ClassFormat.parse("double", ClassFormat.FQCN, null));
		assertEquals(void.class, ClassFormat.parse("void", ClassFormat.FQCN, null));
	}

	@Test void g02_parse_fqcnPrimitive_allLeafs_array() {
		// Array form also routes the leaf through primitiveByName — confirms the array-suffix path
		// composes with the primitive table for every leaf type.  void[] is intentionally excluded
		// because Class.arrayType() is unsupported on void.class.
		assertEquals(boolean[].class, ClassFormat.parse("boolean[]", ClassFormat.FQCN, null));
		assertEquals(byte[].class, ClassFormat.parse("byte[]", ClassFormat.FQCN, null));
		assertEquals(char[].class, ClassFormat.parse("char[]", ClassFormat.FQCN, null));
		assertEquals(short[].class, ClassFormat.parse("short[]", ClassFormat.FQCN, null));
		assertEquals(int[].class, ClassFormat.parse("int[]", ClassFormat.FQCN, null));
		assertEquals(long[].class, ClassFormat.parse("long[]", ClassFormat.FQCN, null));
		assertEquals(float[].class, ClassFormat.parse("float[]", ClassFormat.FQCN, null));
		assertEquals(double[].class, ClassFormat.parse("double[]", ClassFormat.FQCN, null));
	}

	@Test void g03_parse_fqcnPrimitive_defaultArmReturnsNull() {
		// Non-primitive leaf — exercises the 'default -> null' arm of the primitiveByName switch; // NOSONAR
		// resolveLeaf then falls through to Class.forName which resolves the class normally.
		assertEquals(String.class, ClassFormat.parse("java.lang.String", ClassFormat.FQCN, null));
	}
}
