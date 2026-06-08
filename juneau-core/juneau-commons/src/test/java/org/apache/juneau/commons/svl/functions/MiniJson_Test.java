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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/** Tests for {@link MiniJson}. */
class MiniJson_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// parse(String) - basic values
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_parse_null() {
		assertNull(MiniJson.parse(null));
	}

	@Test void a02_parse_empty() {
		assertNull(MiniJson.parse(""));
	}

	@Test void a03_parse_whitespaceOnly() {
		assertNull(MiniJson.parse("   \t\n\r  "));
	}

	@Test void a04_parse_nullLiteral() {
		assertNull(MiniJson.parse("null"));
	}

	@Test void a05_parse_trueLiteral() {
		assertEquals(Boolean.TRUE, MiniJson.parse("true"));
	}

	@Test void a06_parse_falseLiteral() {
		assertEquals(Boolean.FALSE, MiniJson.parse("false"));
	}

	@Test void a07_parse_doubleQuotedString() {
		assertEquals("hello", MiniJson.parse("\"hello\""));
	}

	@Test void a08_parse_singleQuotedString() {
		assertEquals("hello", MiniJson.parse("'hello'"));
	}

	@Test void a09_parse_emptyString() {
		assertEquals("", MiniJson.parse("\"\""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parse(String) - numbers
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_parse_positiveInt() {
		assertEquals(42L, MiniJson.parse("42"));
	}

	@Test void b02_parse_negativeInt() {
		assertEquals(-7L, MiniJson.parse("-7"));
	}

	@Test void b03_parse_zero() {
		assertEquals(0L, MiniJson.parse("0"));
	}

	@Test void b04_parse_floatingPoint() {
		assertEquals(3.14, MiniJson.parse("3.14"));
	}

	@Test void b05_parse_negativeFloating() {
		assertEquals(-2.5, MiniJson.parse("-2.5"));
	}

	@Test void b06_parse_exponentLower() {
		assertEquals(1.5e3, MiniJson.parse("1.5e3"));
	}

	@Test void b07_parse_exponentUpper() {
		assertEquals(1.5e3, MiniJson.parse("1.5E3"));
	}

	@Test void b08_parse_exponentPositiveSign() {
		assertEquals(1.5e3, MiniJson.parse("1.5e+3"));
	}

	@Test void b09_parse_exponentNegativeSign() {
		assertEquals(1.5e-3, MiniJson.parse("1.5e-3"));
	}

	@Test void b10_parse_largeNumber_fallsBackToDouble() {
		// Number too large for Long forces fallback to Double
		var r = MiniJson.parse("99999999999999999999");
		assertTrue(r instanceof Double);
	}

	@Test void b11_parse_integerExponent_isFloating() {
		// Even without decimal point, 'e' marks as floating
		assertEquals(2000.0, MiniJson.parse("2e3"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parse(String) - strings with escapes
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_parse_escape_n() {
		assertEquals("a\nb", MiniJson.parse("\"a\\nb\""));
	}

	@Test void c02_parse_escape_t() {
		assertEquals("a\tb", MiniJson.parse("\"a\\tb\""));
	}

	@Test void c03_parse_escape_r() {
		assertEquals("a\rb", MiniJson.parse("\"a\\rb\""));
	}

	@Test void c04_parse_escape_b() {
		assertEquals("a\bb", MiniJson.parse("\"a\\bb\""));
	}

	@Test void c05_parse_escape_f() {
		assertEquals("a\fb", MiniJson.parse("\"a\\fb\""));
	}

	@Test void c06_parse_escape_slash() {
		assertEquals("a/b", MiniJson.parse("\"a\\/b\""));
	}

	@Test void c07_parse_escape_u() {
		assertEquals("A", MiniJson.parse("\"\\u0041\""));
	}

	@Test void c08_parse_escape_default_quote() {
		// Default branch: \" produces "
		assertEquals("a\"b", MiniJson.parse("\"a\\\"b\""));
	}

	@Test void c09_parse_escape_default_backslash() {
		// Default branch: \\ produces \
		assertEquals("a\\b", MiniJson.parse("\"a\\\\b\""));
	}

	@Test void c10_parse_escape_unicodeTruncated() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("\"\\u01\""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parse(String) - objects
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_parse_emptyObject() {
		assertEquals(new LinkedHashMap<>(), MiniJson.parse("{}"));
	}

	@Test void d02_parse_emptyObjectWithWhitespace() {
		assertEquals(new LinkedHashMap<>(), MiniJson.parse("{ }"));
	}

	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	@Test void d03_parse_simpleObject() {
		var m = (Map<String, Object>) MiniJson.parse("{\"a\":1}");
		assertEquals(1L, m.get("a"));
		assertEquals(1, m.size());
	}

	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	@Test void d04_parse_multipleEntries() {
		var m = (Map<String, Object>) MiniJson.parse("{\"a\":1,\"b\":2,\"c\":3}");
		assertEquals(3, m.size());
		assertEquals(1L, m.get("a"));
		assertEquals(2L, m.get("b"));
		assertEquals(3L, m.get("c"));
	}

	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	@Test void d05_parse_nestedObject() {
		var m = (Map<String, Object>) MiniJson.parse("{\"a\":{\"b\":\"c\"}}");
		var inner = (Map<String, Object>) m.get("a");
		assertEquals("c", inner.get("b"));
	}

	@Test void d06_parse_object_missingColon() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\" 1}"));
	}

	@Test void d07_parse_object_missingCommaOrBrace() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\":1 \"b\":2}"));
	}

	@Test void d08_parse_objectInsertionOrderPreserved() {
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var m = (Map<String, Object>) MiniJson.parse("{\"z\":1,\"a\":2,\"m\":3}");
		assertEquals(List.of("z", "a", "m"), new ArrayList<>(m.keySet()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parse(String) - arrays
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_parse_emptyArray() {
		assertEquals(List.of(), MiniJson.parse("[]"));
	}

	@Test void e02_parse_emptyArrayWithWhitespace() {
		assertEquals(List.of(), MiniJson.parse("[ ]"));
	}

	@Test void e03_parse_array() {
		assertEquals(List.of(1L, 2L, 3L), MiniJson.parse("[1,2,3]"));
	}

	@Test void e04_parse_arrayMixedTypes() {
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var l = (List<Object>) MiniJson.parse("[1,\"two\",true,null]");
		assertEquals(4, l.size());
		assertEquals(1L, l.get(0));
		assertEquals("two", l.get(1));
		assertEquals(Boolean.TRUE, l.get(2));
		assertNull(l.get(3));
	}

	@Test void e05_parse_array_missingCommaOrBracket() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("[1 2]"));
	}

	@Test void e06_parse_nestedArray() {
		assertEquals(List.of(List.of(1L, 2L), List.of(3L)), MiniJson.parse("[[1,2],[3]]"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// parse(String) - error cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_parse_trailingChars() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("1 garbage"));
	}

	@Test void f02_parse_unexpectedCharacter() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("@"));
	}

	@Test void f03_parse_unterminatedString() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("\"unterminated"));
	}

	@Test void f04_parse_invalidTrue() {
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("trueX"));
	}

	@Test void f05_parse_invalidFalseStartingWithF() {
		// 'f' but not "false"
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("foo"));
	}

	@Test void f06_parse_invalidNullLiteral() {
		// 'n' but not "null"
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("nope"));
	}

	@Test void f07_parse_unexpectedEOFInValue() {
		// Object expecting value after colon, but EOF immediately
		assertThrows(IllegalArgumentException.class, () -> MiniJson.parse("{\"a\":"));
	}

	@Test void f08_parse_whitespaceTabNewlineCR() {
		// Exercise all whitespace branches: space, tab, newline, CR
		assertEquals(1L, MiniJson.parse(" \t\n\r 1 \t\n\r "));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// render(Object) - basics
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_render_null() {
		assertEquals("", MiniJson.render(null));
	}

	@Test void g02_render_string() {
		// Strings rendered without surrounding quotes
		assertEquals("hello", MiniJson.render("hello"));
	}

	@Test void g03_render_number() {
		assertEquals("42", MiniJson.render(42L));
	}

	@Test void g04_render_boolean() {
		assertEquals("true", MiniJson.render(Boolean.TRUE));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// render(Object) - maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_render_emptyMap() {
		assertEquals("{}", MiniJson.render(new LinkedHashMap<String, Object>()));
	}

	@Test void h02_render_simpleMap() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", 1L);
		assertEquals("{\"a\":1}", MiniJson.render(m));
	}

	@Test void h03_render_mapMultipleEntries() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", 1L);
		m.put("b", "two");
		assertEquals("{\"a\":1,\"b\":\"two\"}", MiniJson.render(m));
	}

	@Test void h04_render_mapWithNullValue() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", null);
		assertEquals("{\"a\":null}", MiniJson.render(m));
	}

	@Test void h05_render_mapWithNestedMap() {
		var inner = new LinkedHashMap<String, Object>();
		inner.put("b", "c");
		var outer = new LinkedHashMap<String, Object>();
		outer.put("a", inner);
		assertEquals("{\"a\":{\"b\":\"c\"}}", MiniJson.render(outer));
	}

	@Test void h06_render_mapWithNestedList() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", List.of(1L, 2L));
		assertEquals("{\"a\":[1,2]}", MiniJson.render(m));
	}

	@Test void h07_render_mapWithBooleanAndNumber() {
		var m = new LinkedHashMap<String, Object>();
		m.put("flag", Boolean.TRUE);
		m.put("count", 5L);
		assertEquals("{\"flag\":true,\"count\":5}", MiniJson.render(m));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// render(Object) - lists
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_render_emptyList() {
		assertEquals("[]", MiniJson.render(List.of()));
	}

	@Test void i02_render_listOfStrings() {
		assertEquals("[\"a\",\"b\"]", MiniJson.render(List.of("a", "b")));
	}

	@Test void i03_render_listOfNumbers() {
		assertEquals("[1,2,3]", MiniJson.render(List.of(1L, 2L, 3L)));
	}

	@Test void i04_render_listWithNull() {
		var l = new ArrayList<>();
		l.add("a");
		l.add(null);
		l.add("b");
		assertEquals("[\"a\",null,\"b\"]", MiniJson.render(l));
	}

	@Test void i05_render_listOfBooleans() {
		assertEquals("[true,false]", MiniJson.render(List.of(Boolean.TRUE, Boolean.FALSE)));
	}

	@Test void i06_render_listOfMaps() {
		var m = new LinkedHashMap<String, Object>();
		m.put("k", "v");
		assertEquals("[{\"k\":\"v\"}]", MiniJson.render(List.of(m)));
	}

	@Test void i07_render_listWithNonStandardType() {
		// Force the fallback branch in renderInto: not null/String/Boolean/Number/Map/List.
		// e.g. an Object that is none of the above renders as String.valueOf and gets quoted.
		var l = new ArrayList<Object>();
		l.add(new StringBuilder("x"));  // not in any of the special-case types
		var s = MiniJson.render(l);
		assertEquals("[\"x\"]", s);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// render(Object) - non-standard top-level
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_render_nonStandardTopLevel() {
		// render() top-level fallback: not String/Map/List, returns String.valueOf(value).
		// e.g. a StringBuilder.
		assertEquals("foo", MiniJson.render(new StringBuilder("foo")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// render(Object) - string escaping
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_render_mapKeyWithQuote() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a\"b", 1L);
		// appendString escapes " and \
		assertEquals("{\"a\\\"b\":1}", MiniJson.render(m));
	}

	@Test void k02_render_mapKeyWithBackslash() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a\\b", 1L);
		assertEquals("{\"a\\\\b\":1}", MiniJson.render(m));
	}

	@Test void k03_render_listValueWithQuote() {
		assertEquals("[\"a\\\"b\"]", MiniJson.render(List.of("a\"b")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// roundtrip
	//-----------------------------------------------------------------------------------------------------------------

	@Test void m01_roundtrip_object() {
		var src = "{\"name\":\"alice\",\"age\":30,\"tags\":[\"a\",\"b\"]}";
		var parsed = MiniJson.parse(src);
		assertEquals(src, MiniJson.render(parsed));
	}
}
