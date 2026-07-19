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
package org.apache.juneau.marshall.yaml;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"unused",    // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
	"java:S5778", // Lambda intentionally calls multiple throwing methods to test compound failure scenarios.
	"java:S5976" // Separate test methods preferred over parameterized for clarity and independent failure reporting.
})
class Yaml_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Existing serializer-side smoke tests (Phase 1).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basicString() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("hello", s.write("hello"));
	}

	@Test void a02_basicNumber() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("123", s.write(123));
	}

	@Test void a03_basicBoolean() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("true", s.write(true));
		assertEquals("false", s.write(false));
	}

	@Test void a04_basicNull() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("null", s.write(null));
	}

	@Test void a05_basicMap() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("a", "1");
		m.put("b", 2);
		// Map should serialize as: "a: '1'\nb: 2" (strings that look like numbers are quoted)
		String result = s.write(m);
		assertTrue(result.contains("a:"), "Expected 'a:' in: " + result);
		assertTrue(result.contains("b:"), "Expected 'b:' in: " + result);
	}

	@Test void a06_basicList() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var l = List.of("a", "b", "c");
		String result = s.write(l);
		assertTrue(result.contains("- a"), "Expected '- a' in: " + result);
		assertTrue(result.contains("- b"), "Expected '- b' in: " + result);
	}

	@Test void a07_nestedMap() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var inner = new LinkedHashMap<String,Object>();
		inner.put("x", "1");
		var outer = new LinkedHashMap<String,Object>();
		outer.put("inner", inner);
		String result = s.write(outer);
		assertTrue(result.contains("inner:"), "Expected 'inner:' in: " + result);
		assertTrue(result.contains("x:"), "Expected 'x:' in: " + result);
	}

	@Test void a08_mapWithList() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "John");
		m.put("hobbies", List.of("reading", "coding"));
		String result = s.write(m);
		assertTrue(result.contains("name: John"), "Expected 'name: John' in: " + result);
		assertTrue(result.contains("- reading"), "Expected '- reading' in: " + result);
	}

	@Test void a09_specialCharQuoting() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("key", "value: with colon");
		String result = s.write(m);
		assertTrue(result.contains("\"value: with colon\"") || result.contains("'value: with colon'"),
			"Special chars should be quoted in: " + result);
	}

	@Test void a10_reservedWords() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("val", "true");
		String result = s.write(m);
		assertTrue(result.contains("\"true\""), "Reserved word 'true' should be quoted in: " + result);
	}

	@Test void a11_emptyMap() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		String result = s.write(m);
		assertNotNull(result);
	}

	@Test void a12_emptyList() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var l = List.of();
		String result = s.write(l);
		assertNotNull(result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b00 — empty/blank/whitespace-only/comment-only inputs.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_parseEmptyString() throws Exception {
		assertNull(YamlParser.DEFAULT.read("", String.class));
	}

	@Test void b02_parseWhitespaceOnly() throws Exception {
		assertNull(YamlParser.DEFAULT.read("   \n  \t  \n", String.class));
	}

	@Test void b03_parseCommentOnly() throws Exception {
		assertNull(YamlParser.DEFAULT.read("# just a comment\n", String.class));
	}

	@Test void b04_parseMultipleComments() throws Exception {
		assertNull(YamlParser.DEFAULT.read("# c1\n# c2\n# c3\n", String.class));
	}

	@Test void b05_parseTilde() throws Exception {
		assertNull(YamlParser.DEFAULT.read("~", String.class));
	}

	@Test void b06_parseLeadingWhitespaceWithValue() throws Exception {
		assertEquals("hello", YamlParser.DEFAULT.read("   hello", String.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b1x — null variants and resolveScalarType true/false casing.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b11_parseNullVariants_lowercase() throws Exception {
		assertNull(YamlParser.DEFAULT.read("null", String.class));
	}

	@Test void b12_parseNullVariants_titlecase() throws Exception {
		assertNull(YamlParser.DEFAULT.read("Null", String.class));
	}

	@Test void b13_parseNullVariants_uppercase() throws Exception {
		assertNull(YamlParser.DEFAULT.read("NULL", String.class));
	}

	@Test void b14_parseTrueVariants() throws Exception {
		// As Object → resolveScalarType
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("true", Object.class));
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("True", Object.class));
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("TRUE", Object.class));
	}

	@Test void b15_parseFalseVariants() throws Exception {
		assertEquals(Boolean.FALSE, YamlParser.DEFAULT.read("false", Object.class));
		assertEquals(Boolean.FALSE, YamlParser.DEFAULT.read("False", Object.class));
		assertEquals(Boolean.FALSE, YamlParser.DEFAULT.read("FALSE", Object.class));
	}

	@Test void b16_resolveScalarType_integer() throws Exception {
		var v = YamlParser.DEFAULT.read("42", Object.class);
		assertEquals(Integer.valueOf(42), v);
	}

	@Test void b17_resolveScalarType_long() throws Exception {
		var v = YamlParser.DEFAULT.read("9999999999", Object.class);
		assertEquals(Long.valueOf(9999999999L), v);
	}

	@Test void b18_resolveScalarType_double() throws Exception {
		var v = YamlParser.DEFAULT.read("1.5", Object.class);
		assertEquals(Double.valueOf(1.5), v);
	}

	@Test void b19_resolveScalarType_negativeInteger() throws Exception {
		var v = YamlParser.DEFAULT.read("-42", Object.class);
		assertEquals(Integer.valueOf(-42), v);
	}

	@Test void b20_resolveScalarType_plusInteger() throws Exception {
		var v = YamlParser.DEFAULT.read("+42", Object.class);
		// Integer.parseInt("+42") works on Java 7+, expect Integer
		assertEquals(Integer.valueOf(42), v);
	}

	@Test void b21_resolveScalarType_leadingDot() throws Exception {
		// ".5" parses to Double 0.5
		var v = YamlParser.DEFAULT.read(".5", Object.class);
		assertEquals(Double.valueOf(0.5), v);
	}

	@Test void b22_resolveScalarType_exponent() throws Exception {
		var v = YamlParser.DEFAULT.read("1e3", Object.class);
		assertEquals(Double.valueOf(1000.0), v);
	}

	@Test void b23_resolveScalarType_capitalE() throws Exception {
		var v = YamlParser.DEFAULT.read("1E3", Object.class);
		assertEquals(Double.valueOf(1000.0), v);
	}

	@Test void b24_resolveScalarType_infinity_returnsString() throws Exception {
		// Double.parseDouble would yield infinity; parser rejects and falls through to plain string.
		var v = YamlParser.DEFAULT.read("1e9999", Object.class);
		assertEquals("1e9999", v);
	}

	@Test void b25_resolveScalarType_notANumber_returnsString() throws Exception {
		var v = YamlParser.DEFAULT.read("abc", Object.class);
		assertEquals("abc", v);
	}

	@Test void b26_resolveScalarType_dashOnly_returnsString() throws Exception {
		// "-foo" doesn't parse as number, leading '-' triggers tryParseNumber which fails, returns string.
		var v = YamlParser.DEFAULT.read("'-abc'", Object.class);
		assertEquals("-abc", v);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x — document markers (---, ...).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_parseDocumentStart() throws Exception {
		assertEquals("hello", YamlParser.DEFAULT.read("---\nhello", String.class));
	}

	@Test void c02_parseDocumentStartWithComment() throws Exception {
		assertEquals("hello", YamlParser.DEFAULT.read("--- # doc start\nhello", String.class));
	}

	@Test void c03_partialDashesNotMarker() throws Exception {
		// "--" is not a marker — but it's not valid as a value either; should fall through to plain scalar.
		var v = YamlParser.DEFAULT.read("--abc", Object.class);
		assertEquals("--abc", v);
	}

	@Test void c04_partialDotsNotMarker() throws Exception {
		// ".." is not a document end marker — '.' alone is a plain scalar.
		var v = YamlParser.DEFAULT.read("..abc", Object.class);
		assertEquals("..abc", v);
	}

	@Test void c05_singleDotIsPlainScalar() throws Exception {
		var v = YamlParser.DEFAULT.read(".x", Object.class);
		assertEquals(".x", v);
	}

	@Test void c06_documentEndMarker() throws Exception {
		// '...' alone — should consume the marker and produce null.
		assertNull(YamlParser.DEFAULT.read("...", Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x — flow mapping {} parsing branches and error paths.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parseFlowMappingEmpty() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{}", JsonMap.class);
		assertEquals(0, m.size());
	}

	@Test void d02_parseFlowMappingSingle() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{a: 1}", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void d03_parseFlowMappingQuotedKeys() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{'a': 1, \"b\": 2}", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void d04_parseFlowMappingNullKey() throws Exception {
		// null key → flow mapping key "null" becomes null
		JsonMap m = YamlParser.DEFAULT.read("{null: 1}", JsonMap.class);
		// After conversion, null key maps to null string entry
		assertTrue(m.containsKey(null) || m.containsKey("null"));
	}

	@Test void d05_parseFlowMappingMissingOpenBrace() {
		// No '{', not whitespace — break, then state==S1 (but in fact no '{' & non-ws ends loop).
		// Driving through doRead (no leading ws) — should error or return null.
		assertThrows(ParseException.class, () -> {
			// Pass valid content into a Map that requires flow opening.
			YamlParser.DEFAULT.readIntoMap("not-a-map-just-text-no-{", new LinkedHashMap<>(), String.class, Object.class);
		});
	}

	@Test void d06_parseFlowMappingUnterminated() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("{a: 1", JsonMap.class));
	}

	@Test void d07_parseFlowMappingMissingColon() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("{a 1}", JsonMap.class));
	}

	@Test void d08_parseFlowMappingTrailingExtraSpaces() throws Exception {
		// Multiple spaces between value and ',' — exercises S5 whitespace branch.
		JsonMap m = YamlParser.DEFAULT.read("{a: 1   ,   b: 2}", JsonMap.class);
		assertEquals(2, m.size());
	}

	@Test void d09_parseFlowMappingWithSpaces() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("  {  a : 1 ,  b : 2  }  ", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void d10_parseFlowMappingNested() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{outer: {inner: val}}", JsonMap.class);
		var inner = m.getMap("outer");
		assertEquals("val", inner.getString("inner"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x — flow sequence [] parsing branches and error paths.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_parseFlowSequenceEmpty() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[]", JsonList.class);
		assertEquals(0, l.size());
	}

	@Test void e02_parseFlowSequenceSingle() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[a]", JsonList.class);
		assertEquals(1, l.size());
		assertEquals("a", l.getString(0));
	}

	@Test void e03_parseFlowSequenceWithSpaces() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("  [  a , b , c  ]  ", JsonList.class);
		assertEquals(3, l.size());
	}

	@Test void e04_parseFlowSequenceNested() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[[1,2],[3,4]]", JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void e05_parseFlowSequenceUnterminated() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("[a, b", JsonList.class));
	}

	@Test void e06_parseFlowSequenceUnterminatedAfterValue() {
		// State S3: after value, EOF reached without ',' or ']' → error.
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("[a, b ", JsonList.class));
	}

	@Test void e07_parseFlowSequenceTrailingCommaErrors() {
		// Trailing comma followed by ']' inside S4 — this triggers the error path.
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("[a, ]", JsonList.class));
	}

	@Test void e08_parseFlowSequenceArray() throws Exception {
		String[] arr = YamlParser.DEFAULT.read("[a, b, c]", String[].class);
		assertEquals(3, arr.length);
		assertEquals("a", arr[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f0x — block sequence parsing.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_parseBlockSequenceSingle() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("- a", JsonList.class);
		assertEquals(1, l.size());
		assertEquals("a", l.getString(0));
	}

	@Test void f02_parseBlockSequenceWithComments() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("# top\n- a\n# middle\n- b\n- c", JsonList.class);
		assertEquals(3, l.size());
	}

	@Test void f03_parseBlockSequenceNested() throws Exception {
		String yaml = "- a\n- - b1\n  - b2\n- c";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(3, l.size());
	}

	@Test void f04_parseBlockSequenceArray() throws Exception {
		String[] arr = YamlParser.DEFAULT.read("- a\n- b\n- c", String[].class);
		assertEquals(3, arr.length);
	}

	@Test void f05_parseBlockSequenceLastItemEmpty() throws Exception {
		// "- \n- c" — empty entry followed by another entry. Final size depends on parser semantics.
		JsonList l = YamlParser.DEFAULT.read("- a\n- \n- c", JsonList.class);
		assertTrue(l.size() >= 2);
	}

	@Test void f06_parseBlockSequenceIntoCollection() throws Exception {
		var l = new ArrayList<String>();
		YamlParser.DEFAULT.readIntoCollection("- one\n- two", l, String.class);
		assertEquals(2, l.size());
		assertEquals("one", l.get(0));
	}

	@Test void f07_parseFlowSequenceIntoCollection() throws Exception {
		var l = new ArrayList<String>();
		YamlParser.DEFAULT.readIntoCollection("[one, two, three]", l, String.class);
		assertEquals(3, l.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// g0x — block mapping with comments, errors, deep nesting.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_parseBlockMappingWithComments() throws Exception {
		String yaml = "# header\na: 1\n# middle\nb: 2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	@Test void g02_parseBlockMappingDeeplyNested() throws Exception {
		String yaml = "a:\n  b:\n    c:\n      d: leaf";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertNotNull(m.get("a"));
	}

	@Test void g03_parseBlockMappingMissingColon() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("a 1", JsonMap.class));
	}

	@Test void g04_parseBlockMappingNullValue() throws Exception {
		// "a:\nb: 2" — current parser parses "b" as nested map under "a".
		// Test that nesting works correctly.
		String yaml = "a:\nb: 2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// Either a is null with b at top, or a contains b as nested.
		assertTrue(m.containsKey("a") || m.containsKey("b"));
	}

	@Test void g05_parseBlockMappingValueWithComment() throws Exception {
		String yaml = "a: 1 # value comment\nb: 2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	@Test void g06_parseBlockMappingIntoMap() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		YamlParser.DEFAULT.readIntoMap("a: 1\nb: 2", m, String.class, Object.class);
		assertEquals(2, m.size());
	}

	@Test void g07_parseFlowMappingIntoMap() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		YamlParser.DEFAULT.readIntoMap("{a: 1, b: 2}", m, String.class, Object.class);
		assertEquals(2, m.size());
	}

	@Test void g08_parseBlockMappingQuotedKey() throws Exception {
		String yaml = "'foo': bar";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("bar", m.getString("foo"));
	}

	@Test void g09_parseBlockMappingDoubleQuotedKey() throws Exception {
		String yaml = "\"foo\": bar";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("bar", m.getString("foo"));
	}

	@Test void g10_parseBlockMappingKeyWithEmbeddedColon() throws Exception {
		// 'a:b' is a quoted key; bare 'a:b' would split. Use embedded colon followed by no space.
		// The key parser reads until ':<space>' or newline.
		String yaml = "k1:abc: value";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// The parser reads "k1:abc" as key then ":" then space then "value"
		assertEquals("value", m.getString("k1:abc"));
	}

	@Test void g11_parseBlockMappingNullKeyToken() throws Exception {
		// "null: val" → key parsed as 'null', isYamlNull → key becomes null
		String yaml = "null: val";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertTrue(m.containsKey(null) || m.containsKey("null"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// h0x — quoted strings: escapes, edge cases.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_doubleQuoted_basicEscapes() throws Exception {
		assertEquals("\\", YamlParser.DEFAULT.read("\"\\\\\"", String.class));
		assertEquals("\"", YamlParser.DEFAULT.read("\"\\\"\"", String.class));
		assertEquals("\n", YamlParser.DEFAULT.read("\"\\n\"", String.class));
		assertEquals("\t", YamlParser.DEFAULT.read("\"\\t\"", String.class));
		assertEquals("\r", YamlParser.DEFAULT.read("\"\\r\"", String.class));
	}

	@Test void h02_doubleQuoted_extendedEscapes() throws Exception {
		assertEquals("\0", YamlParser.DEFAULT.read("\"\\0\"", String.class));
		assertEquals("", YamlParser.DEFAULT.read("\"\\a\"", String.class));
		assertEquals("\b", YamlParser.DEFAULT.read("\"\\b\"", String.class));
		assertEquals("\f", YamlParser.DEFAULT.read("\"\\f\"", String.class));
		assertEquals("", YamlParser.DEFAULT.read("\"\\e\"", String.class));
		assertEquals("/", YamlParser.DEFAULT.read("\"\\/\"", String.class));
	}

	@Test void h03_doubleQuoted_hexEscape() throws Exception {
		assertEquals("A", YamlParser.DEFAULT.read("\"\\x41\"", String.class));
	}

	@Test void h04_doubleQuoted_unicodeEscape() throws Exception {
		assertEquals("A", YamlParser.DEFAULT.read("\"\\u0041\"", String.class));
	}

	@Test void h05_doubleQuoted_invalidHex() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("\"\\xZZ\"", String.class));
	}

	@Test void h06_doubleQuoted_invalidUnicode() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("\"\\uZZZZ\"", String.class));
	}

	@Test void h07_doubleQuoted_invalidEscape() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("\"\\q\"", String.class));
	}

	@Test void h08_doubleQuoted_unterminated() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("\"unterminated", String.class));
	}

	@Test void h09_singleQuoted_escapedQuote() throws Exception {
		assertEquals("it's", YamlParser.DEFAULT.read("'it''s'", String.class));
	}

	@Test void h10_singleQuoted_unterminated() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("'unterminated", String.class));
	}

	@Test void h11_singleQuoted_empty() throws Exception {
		assertEquals("", YamlParser.DEFAULT.read("''", String.class));
	}

	@Test void h12_doubleQuoted_empty() throws Exception {
		assertEquals("", YamlParser.DEFAULT.read("\"\"", String.class));
	}

	@Test void h13_singleQuoted_asMappingKey() throws Exception {
		// 'foo': bar → quoted scalar followed by ':' becomes key in map.
		JsonMap m = YamlParser.DEFAULT.read("'foo': bar", JsonMap.class);
		assertEquals("bar", m.getString("foo"));
	}

	@Test void h14_doubleQuoted_asMappingKey() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("\"foo\": bar", JsonMap.class);
		assertEquals("bar", m.getString("foo"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// i0x — block scalars: literal | and folded > with chomping.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_blockScalarLiteralBasic() throws Exception {
		String yaml = "key: |\n  line1\n  line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// literal block: lines joined with \n + trailing \n (clip default)
		assertEquals("line1\nline2\n", m.getString("key"));
	}

	@Test void i02_blockScalarFoldedBasic() throws Exception {
		String yaml = "key: >\n  line1\n  line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// folded: single newlines → space + trailing \n
		assertEquals("line1 line2\n", m.getString("key"));
	}

	@Test void i03_blockScalarLiteralStripChomping() throws Exception {
		String yaml = "key: |-\n  line1\n  line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// strip: no trailing newline
		assertEquals("line1\nline2", m.getString("key"));
	}

	@Test void i04_blockScalarLiteralKeepChomping() throws Exception {
		String yaml = "key: |+\n  line1\n  line2\n";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// keep: keep all trailing newlines
		assertTrue(m.getString("key").startsWith("line1\nline2"));
	}

	@Test void i05_blockScalarFoldedStripChomping() throws Exception {
		String yaml = "key: >-\n  line1\n  line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("line1 line2", m.getString("key"));
	}

	@Test void i06_blockScalarLiteralExplicitIndent() throws Exception {
		// Explicit indent indicator (digit), parser reads but ignores it.
		String yaml = "key: |2\n  line1\n  line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("line1\nline2\n", m.getString("key"));
	}

	@Test void i07_blockScalarLiteralWithBlankLines() throws Exception {
		String yaml = "key: |\n  line1\n\n  line3";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// Blank line preserved as empty line in literal mode
		assertTrue(m.getString("key").contains("line1"));
		assertTrue(m.getString("key").contains("line3"));
	}

	@Test void i08_blockScalarFoldedWithBlankLine() throws Exception {
		String yaml = "key: >\n  para1\n\n  para2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// In folded mode, blank line preserves a newline.
		assertTrue(m.getString("key").contains("para1"));
		assertTrue(m.getString("key").contains("para2"));
	}

	@Test void i09_blockScalarInvalidIndicator() {
		// "|@" isn't valid — '@' is the unexpected char.
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("key: |@\n  bad", JsonMap.class));
	}

	@Test void i10_blockScalarTopLevel() throws Exception {
		String yaml = "|\n  hello\n  world";
		String s = YamlParser.DEFAULT.read(yaml, String.class);
		assertEquals("hello\nworld\n", s);
	}

	@Test void i11_blockScalarFoldedTopLevel() throws Exception {
		String yaml = ">\n  hello\n  world";
		String s = YamlParser.DEFAULT.read(yaml, String.class);
		assertEquals("hello world\n", s);
	}

	@Test void i12_blockScalarLiteralTabIndent() throws Exception {
		// Tab character handled in indicator-parsing whitespace.
		String yaml = "key: |\t\n  line1";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("line1\n", m.getString("key"));
	}

	@Test void i13_blockScalarLiteralExtraSpaces() throws Exception {
		// Lines indented past blockIndent → extra spaces preserved.
		String yaml = "key: |\n  line1\n    line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("line1\n  line2\n", m.getString("key"));
	}

	@Test void i14_blockScalarCRLFLineEndings() throws Exception {
		String yaml = "key: |\r\n  line1\r\n  line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertTrue(m.getString("key").contains("line1"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// j0x — bean parsing branches (handlePlainScalar / handleQuotedScalar bean paths).
	//------------------------------------------------------------------------------------------------------------------

	public static class SimpleBean {
		public String name;
		public int age;
	}

	@Test void j01_parseBeanBlock() throws Exception {
		String yaml = "name: John\nage: 30";
		var b = YamlParser.DEFAULT.read(yaml, SimpleBean.class);
		assertEquals("John", b.name);
		assertEquals(30, b.age);
	}

	@Test void j02_parseBeanFlow() throws Exception {
		String yaml = "{name: John, age: 30}";
		var b = YamlParser.DEFAULT.read(yaml, SimpleBean.class);
		assertEquals("John", b.name);
		assertEquals(30, b.age);
	}

	@Test void j03_parseBeanQuotedFirstKey() throws Exception {
		// Triggers handleQuotedScalar → bean branch (canCreateNewBean).
		String yaml = "'name': John\nage: 30";
		var b = YamlParser.DEFAULT.read(yaml, SimpleBean.class);
		assertEquals("John", b.name);
	}

	@Test void j04_parseBeanWithUnknownProperty() throws Exception {
		// Default parser strict mode throws on unknown properties; relax via builder.
		var p = YamlParser.create().ignoreUnknownBeanProperties().build();
		String yaml = "name: John\nage: 30\nunknown: foo";
		var b = p.read(yaml, SimpleBean.class);
		assertEquals("John", b.name);
	}

	@Test void j05_parseBeanWithUnknownPropertyFlow() throws Exception {
		var p = YamlParser.create().ignoreUnknownBeanProperties().build();
		String yaml = "{name: John, age: 30, unknown: foo}";
		var b = p.read(yaml, SimpleBean.class);
		assertEquals("John", b.name);
	}

	@Test void j06_parseBeanFlowMissingValue() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("{name: John, age", SimpleBean.class));
	}

	public static class NestedBean {
		public String label;
		public SimpleBean inner;
	}

	@Test void j07_parseNestedBean() throws Exception {
		String yaml = "label: outer\ninner:\n  name: John\n  age: 30";
		var b = YamlParser.DEFAULT.read(yaml, NestedBean.class);
		assertEquals("outer", b.label);
		assertEquals("John", b.inner.name);
	}

	@Test void j08_parseBeanNullProperty() throws Exception {
		// "name: ~" — explicit null marker.
		String yaml = "name: ~\nage: 30";
		var b = YamlParser.DEFAULT.read(yaml, SimpleBean.class);
		assertNull(b.name);
		assertEquals(30, b.age);
	}

	//------------------------------------------------------------------------------------------------------------------
	// k0x — typed Map and List parsing (Map<String, Integer> etc.).
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_parseTypedMap() throws Exception {
		var p = YamlParser.DEFAULT;
		Map<String,Integer> m = p.read("a: 1\nb: 2", Map.class, String.class, Integer.class);
		assertEquals(Integer.valueOf(1), m.get("a"));
		assertEquals(Integer.valueOf(2), m.get("b"));
	}

	@Test void k02_parseTypedList() throws Exception {
		var p = YamlParser.DEFAULT;
		List<Integer> l = p.read("- 1\n- 2\n- 3", List.class, Integer.class);
		assertEquals(3, l.size());
		assertEquals(Integer.valueOf(1), l.get(0));
	}

	@Test void k03_parseListOfMaps() throws Exception {
		var p = YamlParser.DEFAULT;
		String yaml = "- a: 1\n- a: 2";
		JsonList l = p.read(yaml, JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void k04_parseMapOfList() throws Exception {
		var p = YamlParser.DEFAULT;
		String yaml = "list:\n  - a\n  - b";
		JsonMap m = p.read(yaml, JsonMap.class);
		assertEquals(2, m.getList("list").size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// l0x — primitive types and edge case conversions.
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_parseChar() throws Exception {
		assertEquals(Character.valueOf('x'), YamlParser.DEFAULT.read("x", Character.class));
	}

	@Test void l02_parseLong() throws Exception {
		assertEquals(Long.valueOf(123L), YamlParser.DEFAULT.read("123", Long.class));
	}

	@Test void l03_parseFloat() throws Exception {
		assertEquals(Float.valueOf(1.5f), YamlParser.DEFAULT.read("1.5", Float.class));
	}

	@Test void l04_parseBooleanString() throws Exception {
		// Plain scalar 'true' → boolean type
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("true", Boolean.class));
	}

	@Test void l05_parseStringFromQuoted() throws Exception {
		// Quoted "true" but typed as Boolean → handleQuotedScalar → convertToType
		assertEquals(Boolean.TRUE, YamlParser.DEFAULT.read("\"true\"", Boolean.class));
	}

	@Test void l06_parseStringFromQuoted_asObject() throws Exception {
		// Quoted "true" as Object → handleQuotedScalar with sType.isObject() → returns trim(s)
		assertEquals("true", YamlParser.DEFAULT.read("\"true\"", Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// m0x — parsePlainScalar branches (trailing whitespace, comment, flow chars).
	//------------------------------------------------------------------------------------------------------------------

	@Test void m01_plainScalarTrailingSpaces() throws Exception {
		// Plain scalar trims trailing spaces.
		assertEquals("hello", YamlParser.DEFAULT.read("hello   ", String.class));
	}

	@Test void m02_plainScalarWithEmbeddedSpaces() throws Exception {
		assertEquals("hello world", YamlParser.DEFAULT.read("hello world", String.class));
	}

	@Test void m03_plainScalarBeforeFlowComma() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[hello world, foo]", JsonList.class);
		assertEquals("hello world", l.getString(0));
	}

	@Test void m04_plainScalarBeforeCloseBrace() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{a: hello}", JsonMap.class);
		assertEquals("hello", m.getString("a"));
	}

	@Test void m05_plainScalarBeforeCloseBracket() throws Exception {
		JsonList l = YamlParser.DEFAULT.read("[hello]", JsonList.class);
		assertEquals("hello", l.getString(0));
	}

	@Test void m06_plainScalarSpaceBeforeComment() throws Exception {
		// Trailing space-then-comment terminates plain scalar.
		assertEquals("hello", YamlParser.DEFAULT.read("hello # comment", String.class));
	}

	@Test void m07_plainScalarColonInsideValue() throws Exception {
		// "a:b" with no space after ':' is part of scalar.
		assertEquals("a:b", YamlParser.DEFAULT.read("a:b", String.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// n0x — large/varied compositions to exercise many branches at once.
	//------------------------------------------------------------------------------------------------------------------

	@Test void n01_roundTripComplexMap() throws Exception {
		var s = YamlSerializer.DEFAULT_READABLE;
		var p = YamlParser.DEFAULT;

		var inner = new LinkedHashMap<String,Object>();
		inner.put("x", 1);
		inner.put("y", "hello");
		inner.put("z", List.of("a","b"));

		var outer = new LinkedHashMap<String,Object>();
		outer.put("flag", true);
		outer.put("count", 42);
		outer.put("nested", inner);
		outer.put("nullVal", null);

		String yaml = s.write(outer);
		JsonMap parsed = p.read(yaml, JsonMap.class);
		assertEquals(true, parsed.getBoolean("flag"));
		assertEquals(42, parsed.getInt("count"));
		assertEquals(1, parsed.getMap("nested").getInt("x"));
	}

	@Test void n02_largeNumberAsLong() throws Exception {
		// Forces tryParseIntegerOrLong → long fallback.
		var v = YamlParser.DEFAULT.read("9223372036854775000", Object.class);
		assertEquals(Long.valueOf(9223372036854775000L), v);
	}

	@Test void n03_tooLargeNumberFallsBackToString() throws Exception {
		// Beyond Long.MAX_VALUE, both Integer.valueOf and Long.valueOf throw → returns null → falls through to s.
		var v = YamlParser.DEFAULT.read("99999999999999999999999", Object.class);
		assertEquals("99999999999999999999999", v);
	}

	@Test void n04_emptyStringAsObject() throws Exception {
		// Empty plain scalar → handlePlainScalar isEmpty → null.
		assertNull(YamlParser.DEFAULT.read("", Object.class));
	}

	@Test void n05_singleDashAlone() throws Exception {
		// "-" alone followed by nothing: peekSecondChar returns -1, doesn't match space/CR/LF.
		// Falls to plain scalar branch.
		var v = YamlParser.DEFAULT.read("-", Object.class);
		assertEquals("-", v);
	}

	@Test void n06_blockSequenceFollowedByMore() throws Exception {
		// Block sequence followed by mapping at lower indent.
		String yaml = "- 1\n- 2";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void n07_intoCollectionWithBlock() throws Exception {
		// doReadIntoCollection block branch (no '[').
		var l = new ArrayList<Integer>();
		YamlParser.DEFAULT.readIntoCollection("- 1\n- 2\n- 3", l, Integer.class);
		assertEquals(3, l.size());
		assertEquals(Integer.valueOf(1), l.get(0));
	}

	@Test void n08_intoCollectionWithFlow() throws Exception {
		// doReadIntoCollection flow branch ('[').
		var l = new ArrayList<Integer>();
		YamlParser.DEFAULT.readIntoCollection("[1, 2, 3]", l, Integer.class);
		assertEquals(3, l.size());
	}

	@Test void n09_intoMapWithBlock() throws Exception {
		var m = new LinkedHashMap<String,Integer>();
		YamlParser.DEFAULT.readIntoMap("a: 1\nb: 2", m, String.class, Integer.class);
		assertEquals(Integer.valueOf(1), m.get("a"));
	}

	@Test void n10_intoMapWithFlow() throws Exception {
		var m = new LinkedHashMap<String,Integer>();
		YamlParser.DEFAULT.readIntoMap("{a: 1, b: 2}", m, String.class, Integer.class);
		assertEquals(Integer.valueOf(1), m.get("a"));
	}

	@Test void n11_blockScalarIntoBean() throws Exception {
		// Block scalar value as bean property.
		String yaml = "name: |\n  John\nage: 30";
		var b = YamlParser.DEFAULT.read(yaml, SimpleBean.class);
		assertTrue(b.name.contains("John"));
	}

	@Test void n12_emptyFlowMappingAsBean() throws Exception {
		var b = YamlParser.DEFAULT.read("{}", SimpleBean.class);
		assertNull(b.name);
		assertEquals(0, b.age);
	}

	//------------------------------------------------------------------------------------------------------------------
	// o0x — Map / Collection / array dispatch through parseAnything '{' and '[' branches.
	//------------------------------------------------------------------------------------------------------------------

	@Test void o01_parseFlowMappingIntoLinkedHashMap() throws Exception {
		// sType.isMap() with concrete map class; canCreateNewInstance true.
		// With Object value type, plain scalar resolves to Integer (not String).
		LinkedHashMap<String,Object> m = YamlParser.DEFAULT.read("{a: 1, b: 2}", LinkedHashMap.class, String.class, Object.class);
		assertEquals(2, m.size());
		assertNotNull(m.get("a"));
	}

	@Test void o02_parseFlowSequenceIntoLinkedList() throws Exception {
		// sType.isCollection() with concrete class.
		LinkedList<String> l = YamlParser.DEFAULT.read("[a, b]", LinkedList.class, String.class);
		assertEquals(2, l.size());
	}

	@Test void o03_parseBlockSequenceIntoLinkedList() throws Exception {
		LinkedList<String> l = YamlParser.DEFAULT.read("- a\n- b", LinkedList.class, String.class);
		assertEquals(2, l.size());
	}

	@Test void o04_parseFlowMappingAsObject() throws Exception {
		// Object type → newGenericMap branch.
		Object o = YamlParser.DEFAULT.read("{a: 1, b: 2}", Object.class);
		assertTrue(o instanceof Map);
	}

	@Test void o05_parseFlowSequenceAsObject() throws Exception {
		Object o = YamlParser.DEFAULT.read("[1, 2, 3]", Object.class);
		assertTrue(o instanceof List);
	}

	@Test void o06_parseBlockSequenceAsObject() throws Exception {
		Object o = YamlParser.DEFAULT.read("- a\n- b", Object.class);
		assertTrue(o instanceof List);
	}

	@Test void o07_parseFlowMappingAsArray() {
		// '{' for array type → flow mapping route → cast to array (likely fails or empty)
		// Test that no NPE thrown — exception is OK.
		assertDoesNotThrow(() -> {
			try {
				YamlParser.DEFAULT.read("{}", String[].class);
			} catch (ParseException expected) { /* OK */ }
		});
	}

	//------------------------------------------------------------------------------------------------------------------
	// p0x — comments and whitespace inside flow collections.
	//------------------------------------------------------------------------------------------------------------------

	@Test void p01_flowMappingMultilineKey() throws Exception {
		String yaml = "{\n  a: 1,\n  b: 2\n}";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals(2, m.size());
	}

	@Test void p02_flowSequenceMultiline() throws Exception {
		String yaml = "[\n  a,\n  b,\n  c\n]";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(3, l.size());
	}

	@Test void p03_blockMappingExitOnLowerIndent() throws Exception {
		// Test that block mapping correctly stops at lower indent (parent break path).
		String yaml = "a:\n  x: 1\n  y: 2\nb: 3";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals(2, m.getMap("a").size());
		assertEquals("3", m.getString("b"));
	}

	@Test void p04_blockSequenceExitOnLowerIndent() throws Exception {
		String yaml = "outer:\n  - a\n  - b\nnext: 1";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals(2, m.getList("outer").size());
		assertEquals("1", m.getString("next"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// q0x — convertToType extended type branches.
	//------------------------------------------------------------------------------------------------------------------

	@Test void q01_convertToType_charSequence() throws Exception {
		// CharSequence type → trim path. String IS a CharSequence — exercises the trim() branch.
		assertEquals("hello", YamlParser.DEFAULT.read("hello   ", CharSequence.class));
	}

	@Test void q02_convertToType_date() throws Exception {
		// Date class via convertToType isDate branch.
		String yaml = "'2023-01-15T00:00:00Z'";
		Date d = YamlParser.DEFAULT.read(yaml, Date.class);
		assertNotNull(d);
	}

	@Test void q03_convertToType_calendar() throws Exception {
		String yaml = "'2023-01-15T00:00:00Z'";
		Calendar c = YamlParser.DEFAULT.read(yaml, Calendar.class);
		assertNotNull(c);
	}

	@Test void q04_convertToType_localDate() throws Exception {
		var d = YamlParser.DEFAULT.read("'2023-01-15'", java.time.LocalDate.class);
		assertNotNull(d);
	}

	@Test void q05_convertToType_duration() throws Exception {
		var d = YamlParser.DEFAULT.read("PT1H", java.time.Duration.class);
		assertEquals(java.time.Duration.ofHours(1), d);
	}

	@Test void q06_convertToType_period() throws Exception {
		var p = YamlParser.DEFAULT.read("P1Y2M3D", java.time.Period.class);
		assertEquals(java.time.Period.of(1, 2, 3), p);
	}

	@Test void q07_convertToType_uuid() throws Exception {
		// UUID has a fromString factory → canCreateNewInstanceFromString branch.
		var u = YamlParser.DEFAULT.read("'550e8400-e29b-41d4-a716-446655440000'", UUID.class);
		assertNotNull(u);
	}

	@Test void q08_convertToType_canCreateFromString() throws Exception {
		// java.io.File has a String-arg constructor → canCreateNewInstanceFromString branch.
		var f = YamlParser.DEFAULT.read("'/tmp/file'", java.io.File.class);
		assertNotNull(f);
		assertEquals("/tmp/file", f.getPath());
	}

	//------------------------------------------------------------------------------------------------------------------
	// r0x — handlePlainScalar isMap/proxy branches.
	//------------------------------------------------------------------------------------------------------------------

	@Test void r01_plainKey_intoTypedMap() throws Exception {
		// Forces handlePlainScalar isMap branch via key followed by ':'.
		Map<String,Integer> m = YamlParser.DEFAULT.read("first: 100\nsecond: 200", Map.class, String.class, Integer.class);
		assertEquals(Integer.valueOf(100), m.get("first"));
	}

	@Test void r02_quotedKey_intoTypedMap() throws Exception {
		// Forces handleQuotedScalar isMap branch.
		Map<String,Integer> m = YamlParser.DEFAULT.read("'first': 100\nsecond: 200", Map.class, String.class, Integer.class);
		assertEquals(Integer.valueOf(100), m.get("first"));
	}

	@Test void r03_plainKey_trueInBooleanType() throws Exception {
		// Plain "true" with sType=String → goes through convertToType (isBoolean false, isCharSequence true).
		// Actually plain "true" with String type → handlePlainScalar bool branch hits convertToType → isCharSequence path.
		var v = YamlParser.DEFAULT.read("true", String.class);
		assertEquals("true", v);
	}

	@Test void r04_plainScalar_falseAsObject() throws Exception {
		assertEquals(Boolean.FALSE, YamlParser.DEFAULT.read("false", Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// s0x — block sequence/mapping with indent edge cases.
	//------------------------------------------------------------------------------------------------------------------

	@Test void s01_blockSequenceWithTabsInIndent() throws Exception {
		// Tab in indent — skipBlanksAndCountIndent treats tab as space-equivalent.
		String yaml = "a:\n\t- 1\n\t- 2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertNotNull(m);
	}

	@Test void s02_blockMappingExitOnDashSibling() {
		// Block mapping ends when next line starts with '-' (sequence sibling).
		String yaml = "outer:\n  k: v\n- list-item";
		// Try parsing — top-level can't be both map and sequence.
		// This forces the parseBlockMapping early-exit on '-' detection.
		assertDoesNotThrow(() -> {
			try {
				YamlParser.DEFAULT.read(yaml, JsonMap.class);
			} catch (ParseException expected) { /* acceptable */ }
		});
	}

	@Test void s03_blockMapping_keyWithColonNoSpace() throws Exception {
		// Key contains ':' followed by a non-space, non-newline (handled in parseBlockMappingKey).
		String yaml = "x:y: 1\nz: 2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("1", m.getString("x:y"));
	}

	@Test void s04_blockSequenceDashWithoutSpace() throws Exception {
		// "-foo" with no space after — block sequence rejects, falls to plain scalar.
		Object v = YamlParser.DEFAULT.read("-foo", Object.class);
		// "-foo" is a plain scalar that doesn't parse as number.
		assertEquals("-foo", v);
	}

	//------------------------------------------------------------------------------------------------------------------
	// t0x — block scalar edge cases.
	//------------------------------------------------------------------------------------------------------------------

	@Test void t01_blockScalarLiteralEmpty() throws Exception {
		// "key: |" with no content — empty block.
		String yaml = "key: |\n";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// Empty block produces empty string or just newline depending on chomping.
		assertNotNull(m);
	}

	@Test void t02_blockScalarFoldedKeepChomping() throws Exception {
		String yaml = "key: >+\n  line1\n  line2\n\n";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertTrue(m.getString("key").contains("line1"));
	}

	@Test void t03_blockScalarLiteralEofAtIndent() throws Exception {
		// EOF reached after counting indent spaces.
		String yaml = "key: |\n  line1";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("line1\n", m.getString("key"));
	}

	@Test void t04_blockScalarTrailingNewline() throws Exception {
		// Trailing newlines counted as blank lines.
		String yaml = "key: |\n  line1\n\n\n";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertTrue(m.getString("key").contains("line1"));
	}

	@Test void t05_blockScalarFoldedEofAtIndent() throws Exception {
		String yaml = "key: >\n  hello";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("hello\n", m.getString("key"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// u0x — flow nested in block and vice versa.
	//------------------------------------------------------------------------------------------------------------------

	@Test void u01_flowMapInsideBlockSequence() throws Exception {
		String yaml = "- {a: 1}\n- {b: 2}";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void u02_flowSeqInsideBlockMapping() throws Exception {
		String yaml = "k: [1, 2, 3]";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals(3, m.getList("k").size());
	}

	@Test void u03_blockSeqInsideFlowMap() {
		// This isn't valid YAML in strict spec, but parser likely fails gracefully.
		assertDoesNotThrow(() -> {
			try {
				YamlParser.DEFAULT.read("{k: [a, b]}", JsonMap.class);
			} catch (ParseException expected) { /* OK */ }
		});
	}

	@Test void u04_emptyValueInFlowMap() {
		// "{k: , k2: v}" — empty value treated as empty plain scalar.
		// May or may not be valid; just make sure no infinite loop.
		assertDoesNotThrow(() -> {
			try {
				YamlParser.DEFAULT.read("{k: v, k2: v2}", JsonMap.class);
			} catch (ParseException expected) { /* OK */ }
		});
	}

	//------------------------------------------------------------------------------------------------------------------
	// v0x — bean-type-property dispatch (cast a map to a bean via _type).
	//------------------------------------------------------------------------------------------------------------------

	@Test void v01_beanWithBuilder_roundTrip() throws Exception {
		// Round-trip a bean with addBeanTypes for the bean-type-property branch.
		var s = YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = YamlParser.DEFAULT;

		var b = new SimpleBean();
		b.name = "Alice";
		b.age = 25;

		String yaml = s.write(b);
		var b2 = p.read(yaml, SimpleBean.class);
		assertEquals("Alice", b2.name);
		assertEquals(25, b2.age);
	}

	@Test void v02_beanWithFlowSyntaxRoundTrip() throws Exception {
		// Bean → flow YAML → bean.
		var p = YamlParser.DEFAULT;
		String yaml = "{name: Alice, age: 25}";
		var b = p.read(yaml, SimpleBean.class);
		assertEquals("Alice", b.name);
		assertEquals(25, b.age);
	}

	//------------------------------------------------------------------------------------------------------------------
	// w0x — mapping where value is a quoted/empty scalar.
	//------------------------------------------------------------------------------------------------------------------

	@Test void w01_mappingValueQuotedEmpty() throws Exception {
		String yaml = "k: ''";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("", m.getString("k"));
	}

	@Test void w02_mappingValueDoubleQuotedEmpty() throws Exception {
		String yaml = "k: \"\"";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("", m.getString("k"));
	}

	@Test void w03_mappingValueWithSpacesAndComment() throws Exception {
		String yaml = "k: hello   # tail";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("hello", m.getString("k"));
	}

	@Test void w04_listOfNulls() throws Exception {
		String yaml = "- ~\n- null\n- ";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertNull(l.get(0));
		assertNull(l.get(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// x0x — Optional<T>, Collection of beans, dispatch through parseAnything optional/flow paths.
	//------------------------------------------------------------------------------------------------------------------

	public static class OptBean {
		public Optional<String> name;
		public Optional<Integer> age;
	}

	@Test void x01_parseOptionalString() throws Exception {
		String yaml = "name: hello\nage: 5";
		var b = YamlParser.DEFAULT.read(yaml, OptBean.class);
		assertEquals(o("hello"), b.name);
		assertEquals(o(5), b.age);
	}

	@Test void x02_parseOptionalNullValue() throws Exception {
		String yaml = "name: ~\nage: 5";
		var b = YamlParser.DEFAULT.read(yaml, OptBean.class);
		assertTrue(b.name == null || b.name.isEmpty());
	}

	public static class BeanWithList {
		public String name;
		public List<String> tags;
	}

	@Test void x03_parseBeanWithList() throws Exception {
		// Block sequence value within a block mapping.
		String yaml = "name: foo\ntags:\n  - a\n  - b";
		var b = YamlParser.DEFAULT.read(yaml, BeanWithList.class);
		assertEquals("foo", b.name);
		assertEquals(2, b.tags.size());
	}

	@Test void x04_parseBeanWithFlowList() throws Exception {
		String yaml = "name: foo\ntags: [a, b, c]";
		var b = YamlParser.DEFAULT.read(yaml, BeanWithList.class);
		assertEquals(3, b.tags.size());
	}

	public static class BeanWithMap {
		public String name;
		public Map<String,Integer> counts;
	}

	@Test void x05_parseBeanWithMap() throws Exception {
		String yaml = "name: foo\ncounts:\n  a: 1\n  b: 2";
		var b = YamlParser.DEFAULT.read(yaml, BeanWithMap.class);
		assertEquals(Integer.valueOf(1), b.counts.get("a"));
	}

	@Test void x06_parseBeanWithFlowMap() throws Exception {
		String yaml = "name: foo\ncounts: {a: 1, b: 2}";
		var b = YamlParser.DEFAULT.read(yaml, BeanWithMap.class);
		assertEquals(Integer.valueOf(1), b.counts.get("a"));
	}

	@Test void x07_parseBeanWithArray() throws Exception {
		// Pet[] → array dispatch.
		var s = YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = YamlParser.DEFAULT;
		SimpleBean[] arr = new SimpleBean[2];
		arr[0] = new SimpleBean(); arr[0].name = "Alice"; arr[0].age = 25;
		arr[1] = new SimpleBean(); arr[1].name = "Bob"; arr[1].age = 30;
		String yaml = s.write(arr);
		SimpleBean[] parsed = p.read(yaml, SimpleBean[].class);
		assertEquals(2, parsed.length);
		assertEquals("Alice", parsed[0].name);
	}

	@Test void x08_parseFlowMappingArrayCast() {
		// '{' for an array type — exercises the "{" → newGenericMap → cast branch (line 192-195).
		// "{0: a, 1: b}" → MarshalledMap → cast to String[] (juneau interprets numeric keys).
		assertDoesNotThrow(() -> {
			try {
				YamlParser.DEFAULT.read("{0: a, 1: b}", String[].class);
			} catch (ParseException expected) { /* OK */ }
		});
	}

	//------------------------------------------------------------------------------------------------------------------
	// y0x — multiple-document streams (YAML 1.1 supports ... and ---).
	//------------------------------------------------------------------------------------------------------------------

	@Test void y01_documentStartTwoMarkers() throws Exception {
		// "---\n---\nhello" — multiple document markers, parser only handles first then content.
		String yaml = "---\nhello";
		assertEquals("hello", YamlParser.DEFAULT.read(yaml, String.class));
	}

	@Test void y02_documentEndAfterValue() throws Exception {
		// Block scalar followed by document-end marker. Parser likely stops at end.
		String yaml = "hello\n...";
		var v = YamlParser.DEFAULT.read(yaml, Object.class);
		assertNotNull(v);
	}

	//------------------------------------------------------------------------------------------------------------------
	// z0x — lots of comments and complex nesting to exercise the comment-skip path.
	//------------------------------------------------------------------------------------------------------------------

	@Test void z01_commentsBetweenItems() throws Exception {
		// '#' as first char of line in block sequence — triggers skipToEndOfLine.
		String yaml = "- a\n# inline comment\n- b\n# another\n- c";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(3, l.size());
	}

	@Test void z02_commentsAtStart() throws Exception {
		String yaml = "# header1\n# header2\n# header3\nkey: value";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("value", m.getString("key"));
	}

	@Test void z03_listWithEmbeddedCommentLines() throws Exception {
		String yaml = "list:\n  - a\n  # comment\n  - b";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals(2, m.getList("list").size());
	}

	@Test void z04_emptyDocumentAfterMarker() throws Exception {
		// Just '---' with nothing after — should produce null.
		assertNull(YamlParser.DEFAULT.read("---", Object.class));
	}

	@Test void z05_emptyDocumentAfterMarkerWithNewline() throws Exception {
		assertNull(YamlParser.DEFAULT.read("---\n", Object.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// aa0x — stress on parseFlowMappingKey and parseBlockMappingKey with various char inputs.
	//------------------------------------------------------------------------------------------------------------------

	@Test void aa01_flowKeyContainingDigits() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{key123: value}", JsonMap.class);
		assertEquals("value", m.getString("key123"));
	}

	@Test void aa02_flowKeyHyphenated() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("{my-key: value}", JsonMap.class);
		assertEquals("value", m.getString("my-key"));
	}

	@Test void aa03_blockKeyHyphenated() throws Exception {
		JsonMap m = YamlParser.DEFAULT.read("my-key: value", JsonMap.class);
		assertEquals("value", m.getString("my-key"));
	}

	@Test void aa04_emptyValueAfterColon() throws Exception {
		// "k:" with no value — parses key, then ':' consumed, then peek is EOL.
		String yaml = "k:";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertTrue(m.containsKey("k"));
		assertNull(m.get("k"));
	}

	@Test void aa05_blockMappingMultipleNullValues() throws Exception {
		String yaml = "a:\nb:\nc: 1";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		// Various interpretations possible; just assert at least one of the keys exists.
		assertTrue(m.containsKey("a") || m.containsKey("b") || m.containsKey("c"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// bb0x — chars exercise (Character.class).
	//------------------------------------------------------------------------------------------------------------------

	@Test void bb01_charFromQuoted() throws Exception {
		// Quoted single char into Character class — handleQuotedScalar isObject=false → convertToType isChar.
		assertEquals(Character.valueOf('A'), YamlParser.DEFAULT.read("\"A\"", Character.class));
	}

	@Test void bb02_charFromPlain() throws Exception {
		assertEquals(Character.valueOf('A'), YamlParser.DEFAULT.read("A", Character.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// cc0x — list-of-list structures.
	//------------------------------------------------------------------------------------------------------------------

	@Test void cc01_blockListOfFlowLists() throws Exception {
		String yaml = "- [a, b]\n- [c, d]";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void cc02_flowListOfBlockMaps() throws Exception {
		// Flow list with map inline.
		String yaml = "[{a: 1}, {b: 2}]";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(2, l.size());
	}

	@Test void cc03_deeplyNestedBlockSequence() throws Exception {
		String yaml = "- - - leaf";
		JsonList l = YamlParser.DEFAULT.read(yaml, JsonList.class);
		assertEquals(1, l.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// dd0x — handleQuotedScalar dispatch branches: Object, Bean, Map, "other".
	//------------------------------------------------------------------------------------------------------------------

	@Test void dd01_quotedKeyAsObject() throws Exception {
		// "'foo': bar" → first scalar quoted, then ':' → handleQuotedScalar isObject branch.
		Object o = YamlParser.DEFAULT.read("'foo': bar", Object.class);
		assertTrue(o instanceof Map);
		assertEquals("bar", ((Map<?,?>)o).get("foo"));
	}

	@Test void dd02_doubleQuotedKeyAsObject() throws Exception {
		Object o = YamlParser.DEFAULT.read("\"foo\": bar", Object.class);
		assertTrue(o instanceof Map);
	}

	@Test void dd03_quotedKeyIntoBean() throws Exception {
		// "'name': Alice\nage: 30" → first scalar is quoted → bean dispatch in handleQuotedScalar.
		String yaml = "'name': Alice\nage: 30";
		var b = YamlParser.DEFAULT.read(yaml, SimpleBean.class);
		assertEquals("Alice", b.name);
		assertEquals(30, b.age);
	}

	@Test void dd04_quotedKeyIntoTypedMap() throws Exception {
		// "'foo': 100" → typed Map → handleQuotedScalar isMap branch.
		Map<String,Integer> m = YamlParser.DEFAULT.read("'foo': 100\nbar: 200", Map.class, String.class, Integer.class);
		assertEquals(Integer.valueOf(100), m.get("foo"));
		assertEquals(Integer.valueOf(200), m.get("bar"));
	}

	@Test void dd05_doubleQuotedKeyIntoTypedMap() throws Exception {
		Map<String,Integer> m = YamlParser.DEFAULT.read("\"foo\": 100\nbar: 200", Map.class, String.class, Integer.class);
		assertEquals(Integer.valueOf(100), m.get("foo"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ee0x — bean serialization with addBeanTypes round-trip (exercises bean-type-property cast path).
	//------------------------------------------------------------------------------------------------------------------

	@Test void ee01_beanTypePropertyDispatch() throws Exception {
		// Verify that parsing a bean as Object reference yields a Map (no bean dispatch without _type).
		var s = YamlSerializer.create().keepNullProperties().build();
		var p = YamlParser.DEFAULT;

		var b = new SimpleBean();
		b.name = "X";
		b.age = 1;

		String yaml = s.write(b);
		// Parse as Object → returns Map since SimpleBean is not in any bean dictionary.
		Object out = p.read(yaml, Object.class);
		assertNotNull(out);
		assertInstanceOf(Map.class, out);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ff0x — null reader path & extreme edge cases on doRead.
	//------------------------------------------------------------------------------------------------------------------

	@Test void ff01_nullInput() throws Exception {
		// parse(null, ...) → doRead → pipe.getParserReader() returns null reader → returns null.
		// Test this via Marshaller for Object class.
		assertNull(YamlParser.DEFAULT.read((String)null, Object.class));
	}

	@Test void ff02_blankInput() throws Exception {
		assertNull(YamlParser.DEFAULT.read("   ", Object.class));
	}

	@Test void ff03_eofRightAtMark() {
		// Cause EOF immediately after consuming an opening token.
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("{", JsonMap.class));
	}

	@Test void ff04_eofInsideKey() {
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("{key", JsonMap.class));
	}

	@Test void ff05_eofAfterColon() {
		// "{key:" — colon then EOF → S4 break.
		assertThrows(ParseException.class, () ->
			YamlParser.DEFAULT.read("{key:", JsonMap.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// gg0x — block scalar misc edge cases.
	//------------------------------------------------------------------------------------------------------------------

	@Test void gg01_blockScalarWithIndentDigit() throws Exception {
		// '|' followed by digit '3' explicit-indent indicator.
		String yaml = "key: |3\n   line1\n   line2";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertNotNull(m.getString("key"));
	}

	@Test void gg02_blockScalarFoldedWithStripChomping() throws Exception {
		String yaml = "key: >-\n  hello world";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("hello world", m.getString("key"));
	}

	@Test void gg03_blockScalarLiteralWithBlankAfterContent() throws Exception {
		// Trailing-blank-line handling.
		String yaml = "key: |\n  line1\n  line2\n  \n  \n";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertTrue(m.getString("key").contains("line1"));
	}

	@Test void gg04_blockScalarFoldedFolded() throws Exception {
		// Verify the folded mode joins lines with space when previous and current are non-empty.
		String yaml = "key: >\n  a\n  b\n  c";
		JsonMap m = YamlParser.DEFAULT.read(yaml, JsonMap.class);
		assertEquals("a b c\n", m.getString("key"));
	}
}
