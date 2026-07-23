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
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CsvCellParser}.
 */
class CsvCellParser_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// null / empty inputs (line 60)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_null_input() throws ParseException {
		assertNull(CsvCellParser.read(null, null));
	}

	@Test void a02_empty_input() throws ParseException {
		assertNull(CsvCellParser.read("", null));
	}

	@Test void a03_whitespace_only() throws ParseException {
		assertNull(CsvCellParser.read("   ", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Simple scalar values (line 191-216)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_string_value() throws ParseException {
		assertEquals("hello", CsvCellParser.read("hello", null));
	}

	@Test void b02_true_value() throws ParseException {
		assertEquals(Boolean.TRUE, CsvCellParser.read("true", null));
	}

	@Test void b03_false_value() throws ParseException {
		assertEquals(Boolean.FALSE, CsvCellParser.read("false", null));
	}

	@Test void b04_null_marker_default() throws ParseException {
		assertNull(CsvCellParser.read("null", null));
	}

	@Test void b05_null_marker_custom() throws ParseException {
		assertNull(CsvCellParser.read("<NULL>", "<NULL>"));
	}

	@Test void b06_null_marker_caseInsensitive() throws ParseException {
		assertNull(CsvCellParser.read("NULL", null));
	}

	@Test void b07_integer_value() throws ParseException {
		assertEquals(42L, CsvCellParser.read("42", null));
	}

	@Test void b08_negative_integer() throws ParseException {
		assertEquals(-5L, CsvCellParser.read("-5", null));
	}

	@Test void b09_double_value() throws ParseException {
		assertEquals(3.14, CsvCellParser.read("3.14", null));
	}

	@Test void b10_double_that_is_whole() throws ParseException {
		// 2.0 — has decimal point, equals (long), returns Long
		assertEquals(2L, CsvCellParser.read("2.0", null));
	}

	@Test void b11_non_numeric_string() throws ParseException {
		assertEquals("hello world", CsvCellParser.read("\"hello world\"", null));
	}

	@Test void b12_string_with_spaces_unquoted() throws ParseException {
		// unquoted identifier parsing
		assertEquals("abc", CsvCellParser.read("abc", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Quoted string (lines 164-178)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_quoted_string() throws ParseException {
		assertEquals("hello", CsvCellParser.read("\"hello\"", null));
	}

	@Test void c02_quoted_empty() throws ParseException {
		assertEquals("", CsvCellParser.read("\"\"", null));
	}

	@Test void c03_quoted_with_escape() throws ParseException {
		assertEquals("a\"b", CsvCellParser.read("\"a\\\"b\"", null));
	}

	@Test void c04_quoted_with_backslash_escape() throws ParseException {
		assertEquals("a\\b", CsvCellParser.read("\"a\\\\b\"", null));
	}

	@Test void c05_unterminated_quoted_throws() {
		assertThrows(ParseException.class, () -> CsvCellParser.read("\"unclosed", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Object parsing (lines 102-129)
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Test void d01_empty_object() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{}", null);
		assertNotNull(r);
		assertTrue(r.isEmpty());
	}

	@SuppressWarnings("unchecked")
	@Test void d02_single_pair() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{a:1}", null);
		assertEquals(1L, r.get("a"));
	}

	@SuppressWarnings("unchecked")
	@Test void d03_multiple_pairs() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{a:1;b:2;c:3}", null);
		assertEquals(1L, r.get("a"));
		assertEquals(2L, r.get("b"));
		assertEquals(3L, r.get("c"));
	}

	@SuppressWarnings("unchecked")
	@Test void d04_object_with_string_value() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{name:Alice}", null);
		assertEquals("Alice", r.get("name"));
	}

	@SuppressWarnings("unchecked")
	@Test void d05_object_with_quoted_key() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{\"my key\":val}", null);
		assertEquals("val", r.get("my key"));
	}

	@SuppressWarnings("unchecked")
	@Test void d06_object_with_null_value() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{a:null}", null);
		assertNull(r.get("a"));
	}

	@Test void d07_object_missing_colon_throws() {
		assertThrows(ParseException.class, () -> CsvCellParser.read("{a}", null));
	}

	@Test void d08_object_invalid_separator_throws() {
		assertThrows(ParseException.class, () -> CsvCellParser.read("{a:1 b:2}", null));
	}

	@SuppressWarnings("unchecked")
	@Test void d09_nested_object() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{a:{b:1}}", null);
		assertInstanceOf(Map.class, r.get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Array parsing (lines 140-162)
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Test void e01_empty_array() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[]", null);
		assertNotNull(r);
		assertTrue(r.isEmpty());
	}

	@SuppressWarnings("unchecked")
	@Test void e02_single_element() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[1]", null);
		assertEquals(1L, r.get(0));
	}

	@SuppressWarnings("unchecked")
	@Test void e03_multiple_elements() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[1;2;3]", null);
		assertEquals(3, r.size());
		assertEquals(1L, r.get(0));
		assertEquals(2L, r.get(1));
		assertEquals(3L, r.get(2));
	}

	@SuppressWarnings("unchecked")
	@Test void e04_array_of_strings() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[a;b;c]", null);
		assertEquals("a", r.get(0));
		assertEquals("b", r.get(1));
		assertEquals("c", r.get(2));
	}

	@SuppressWarnings("unchecked")
	@Test void e05_array_with_null_elements() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[null;1;null]", null);
		assertNull(r.get(0));
		assertEquals(1L, r.get(1));
		assertNull(r.get(2));
	}

	@Test void e06_array_invalid_separator_throws() {
		assertThrows(ParseException.class, () -> CsvCellParser.read("[1 2]", null));
	}

	@SuppressWarnings("unchecked")
	@Test void e07_array_with_objects() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[{a:1};{b:2}]", null);
		assertEquals(2, r.size());
		assertInstanceOf(Map.class, r.get(0));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Whitespace handling
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Test void f01_object_with_spaces() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{ a : 1 ; b : 2 }", null);
		assertEquals(1L, r.get("a"));
		assertEquals(2L, r.get("b"));
	}

	@SuppressWarnings("unchecked")
	@Test void f02_array_with_spaces() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[ 1 ; 2 ; 3 ]", null);
		assertEquals(3, r.size());
	}

	@Test void f03_leading_trailing_spaces() throws ParseException {
		assertEquals(42L, CsvCellParser.read("  42  ", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Edge cases for line 193: readSimpleValue when next char is ; or } or ]
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Test void h01_object_empty_value_before_semicolon() throws ParseException {
		// In {a:;b:2}, the value of 'a' is parsed via readSimpleValue where peek() == ';'
		var r = (Map<String,Object>) CsvCellParser.read("{a:;b:2}", null);
		assertEquals("", r.get("a"));
		assertEquals(2L, r.get("b"));
	}

	@SuppressWarnings("unchecked")
	@Test void h02_object_empty_value_before_close() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{a:}", null);
		assertEquals("", r.get("a"));
	}

	@SuppressWarnings("unchecked")
	@Test void h03_array_empty_element_before_semicolon() throws ParseException {
		// [;1] — first element is "" (empty value before ;)
		var r = (List<Object>) CsvCellParser.read("[;1]", null);
		assertEquals("", r.get(0));
		assertEquals(1L, r.get(1));
	}

	@SuppressWarnings("unchecked")
	@Test void h04_array_empty_element_before_close() throws ParseException {
		var r = (List<Object>) CsvCellParser.read("[1;]", null);
		assertEquals("", r.get(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Identifier with various stop characters (line 184)
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	@Test void i01_identifier_stops_at_colon() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{key:val}", null);
		assertEquals("val", r.get("key"));
	}

	@SuppressWarnings("unchecked")
	@Test void i02_identifier_stops_at_semicolon() throws ParseException {
		var r = (Map<String,Object>) CsvCellParser.read("{a:x;b:y}", null);
		assertEquals("x", r.get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error paths
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_invalid_cell_throws_readException() {
		assertThrows(ParseException.class, () -> CsvCellParser.read("{incomplete", null));
	}

	@SuppressWarnings("unchecked")
	@Test void g02_empty_key_reads_to_empty_string() throws ParseException {
		// parser reads empty identifier as empty string key
		var r = (Map<String,Object>) CsvCellParser.read("{:val}", null);
		assertTrue(r.containsKey(""));
		assertEquals("val", r.get(""));
	}
}
