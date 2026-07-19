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
package org.apache.juneau.marshall.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@link CsvCellSerializer}.
 *
 * <p>
 * Exercises the inline-notation serialization paths used when {@code allowNestedStructures(true)} is set.
 * Covers all primitive array variants, Map/Collection nesting, escape rules, byte-array formats, and
 * null/default constructor fall-back behavior.
 */
class CsvCellSerializer_Test extends TestBase {

	private static CsvSerializerSession session() {
		return CsvSerializer.create().allowNestedStructures(true).build().getSession();
	}

	private static CsvSerializerSession sessionSemicolonBytes() {
		return CsvSerializer.create()
			.allowNestedStructures(true)
			.byteArrayFormat(CsvByteArrayCellFormat.SEMICOLON_DELIMITED)
			.build()
			.getSession();
	}

	//------------------------------------------------------------------------------------------------
	// Constructor edge cases (line 42, 43)
	//------------------------------------------------------------------------------------------------

	@Test void a01_ctorNullByteArrayFormat() {
		var cs = new CsvCellSerializer(null, null);
		assertEquals("null", cs.write(null, session()));
	}

	@Test void a02_ctorCustomNullMarker() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "<NIL>");
		assertEquals("<NIL>", cs.write(null, session()));
	}

	//------------------------------------------------------------------------------------------------
	// Null handling (line 61, 62)
	//------------------------------------------------------------------------------------------------

	@Test void b01_writeNull() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("null", cs.write(null, session()));
	}

	//------------------------------------------------------------------------------------------------
	// Map serialization (line 86-101)
	//------------------------------------------------------------------------------------------------

	@Test void c01_writeMapEmpty() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("{}", cs.write(new LinkedHashMap<>(), session()));
	}

	@Test void c02_writeMapSingleEntry() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "v");
		assertEquals("{k:v}", cs.write(m, session()));
	}

	@Test void c03_writeMapMultipleEntries() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		var m = new LinkedHashMap<String,Object>();
		m.put("a", 1);
		m.put("b", 2);
		assertEquals("{a:1;b:2}", cs.write(m, session()));
	}

	@Test void c04_writeMapNullKey() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "NIL");
		var m = new LinkedHashMap<String,Object>();
		m.put(null, "v");
		assertEquals("{NIL:v}", cs.write(m, session()));
	}

	@Test void c05_writeMapNullValue() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "NIL");
		var m = new LinkedHashMap<String,Object>();
		m.put("k", null);
		assertEquals("{k:NIL}", cs.write(m, session()));
	}

	//------------------------------------------------------------------------------------------------
	// Collection serialization (line 103-114)
	//------------------------------------------------------------------------------------------------

	@Test void d01_writeCollectionEmpty() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[]", cs.write(new ArrayList<>(), session()));
	}

	@Test void d02_writeCollectionSingle() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[a]", cs.write(List.of("a"), session()));
	}

	@Test void d03_writeCollectionMultiple() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[a;b;c]", cs.write(List.of("a", "b", "c"), session()));
	}

	@Test void d04_writeCollectionNested() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[[a;b];[c;d]]", cs.write(List.of(List.of("a", "b"), List.of("c", "d")), session()));
	}

	//------------------------------------------------------------------------------------------------
	// Object[] array serialization (line 116-125)
	//------------------------------------------------------------------------------------------------

	@Test void e01_writeObjectArrayEmpty() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[]", cs.write(new Object[0], session()));
	}

	@Test void e02_writeObjectArrayMultiple() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[1;2;3]", cs.write(new Object[] { 1, 2, 3 }, session()));
	}

	//------------------------------------------------------------------------------------------------
	// Primitive array serialization (line 127-202)
	//------------------------------------------------------------------------------------------------

	@Test void f01_intArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[1;2;3]", cs.write(new int[] { 1, 2, 3 }, session()));
	}

	@Test void f02_intArrayEmpty() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[]", cs.write(new int[0], session()));
	}

	@Test void f03_longArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[1;2;3]", cs.write(new long[] { 1L, 2L, 3L }, session()));
	}

	@Test void f04_doubleArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[1.5;2.5]", cs.write(new double[] { 1.5, 2.5 }, session()));
	}

	@Test void f05_floatArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[1.5;2.5]", cs.write(new float[] { 1.5f, 2.5f }, session()));
	}

	@Test void f06_shortArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[1;2;3]", cs.write(new short[] { (short) 1, (short) 2, (short) 3 }, session()));
	}

	@Test void f07_booleanArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[true;false;true]", cs.write(new boolean[] { true, false, true }, session()));
	}

	@Test void f08_charArray() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		// chars are emitted as int code points
		assertEquals("[65;66;67]", cs.write(new char[] { 'A', 'B', 'C' }, session()));
	}

	@Test void f09_charArrayEmpty() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[]", cs.write(new char[0], session()));
	}

	//------------------------------------------------------------------------------------------------
	// byte[] array serialization (line 70, 204-211)
	//------------------------------------------------------------------------------------------------

	@Test void g01_byteArrayBase64() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		var bytes = new byte[] { 'a', 'b', 'c' };
		var result = cs.write(bytes, session());
		// base64-encoded
		assertEquals("YWJj", result);
	}

	@Test void g02_byteArraySemicolon() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.SEMICOLON_DELIMITED, "null");
		var bytes = new byte[] { 1, 2, 3 };
		assertEquals("1;2;3", cs.write(bytes, sessionSemicolonBytes()));
	}

	@Test void g03_byteArraySemicolonNegative() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.SEMICOLON_DELIMITED, "null");
		var bytes = new byte[] { (byte) 0xFF, (byte) 0x80 };
		assertEquals("255;128", cs.write(bytes, sessionSemicolonBytes()));
	}

	@Test void g04_byteArrayEmpty() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.SEMICOLON_DELIMITED, "null");
		assertEquals("", cs.write(new byte[0], sessionSemicolonBytes()));
	}

	//------------------------------------------------------------------------------------------------
	// escapeIfNeeded / quoted - escape handling (line 213-235)
	//------------------------------------------------------------------------------------------------

	@Test void h01_escapeNoSpecialChars() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[hello]", cs.write(List.of("hello"), session()));
	}

	@Test void h02_escapeContainsSemicolon() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[\"a;b\"]", cs.write(List.of("a;b"), session()));
	}

	@Test void h03_escapeContainsColon() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[\"a:b\"]", cs.write(List.of("a:b"), session()));
	}

	@Test void h04_escapeContainsBraces() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[\"a{b}c\"]", cs.write(List.of("a{b}c"), session()));
	}

	@Test void h05_escapeContainsBrackets() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("[\"a[b]c\"]", cs.write(List.of("a[b]c"), session()));
	}

	@Test void h06_escapeContainsQuote() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		// Quote inside should be backslash-escaped within quoted form.
		assertEquals("[\"a\\\"b\"]", cs.write(List.of("a\"b"), session()));
	}

	@Test void h07_escapeContainsBackslash() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		// Backslash inside should be backslash-escaped.
		assertEquals("[\"a\\\\b\"]", cs.write(List.of("a\\b"), session()));
	}

	@Test void h08_escapeMapValueWithSpecialChar() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "a;b");
		assertEquals("{k:\"a;b\"}", cs.write(m, session()));
	}

	@Test void h09_escapeMapKeyWithSpecialChar() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		var m = new LinkedHashMap<String,Object>();
		m.put("a:b", "v");
		assertEquals("{\"a:b\":v}", cs.write(m, session()));
	}

	//------------------------------------------------------------------------------------------------
	// Bean / scalar fall-through via prepareForInlineValue (line 79-83)
	//------------------------------------------------------------------------------------------------

	public static class Bean1 {
		public String x;
		public int y;
		public Bean1() {}
		public Bean1(String x, int y) { this.x = x; this.y = y; }
	}

	@Test void i01_beanSerialization() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		var s = cs.write(new Bean1("hi", 42), session());
		// Bean should be serialized as map-like notation with x and y entries.
		assertTrue(s.contains("x:hi"), s);
		assertTrue(s.contains("y:42"), s);
		assertTrue(s.startsWith("{") && s.endsWith("}"), s);
	}

	@Test void i02_simpleStringFallthrough() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		// Plain string with no special chars takes the simple .toString() branch.
		assertEquals("hello", cs.write("hello", session()));
	}

	@Test void i03_simpleIntegerFallthrough() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		assertEquals("42", cs.write(42, session()));
	}

	@Test void i04_simpleStringWithSpecialChar() {
		var cs = new CsvCellSerializer(CsvByteArrayCellFormat.BASE64, "null");
		// Top-level string with separators should be quoted.
		assertEquals("\"a;b\"", cs.write("a;b", session()));
	}
}
