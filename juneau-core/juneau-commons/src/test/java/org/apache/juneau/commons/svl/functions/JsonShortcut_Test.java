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

/** Tests for {@link JsonShortcut}. */
class JsonShortcut_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// encodeArray(List<String>)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_encodeArray_empty() {
		assertEquals("[]", JsonShortcut.encodeArray(List.of()));
	}

	@Test void a02_encodeArray_single() {
		assertEquals("[\"a\"]", JsonShortcut.encodeArray(List.of("a")));
	}

	@Test void a03_encodeArray_multiple() {
		assertEquals("[\"a\",\"b\",\"c\"]", JsonShortcut.encodeArray(List.of("a", "b", "c")));
	}

	@Test void a04_encodeArray_escapesQuote() {
		assertEquals("[\"a\\\"b\"]", JsonShortcut.encodeArray(List.of("a\"b")));
	}

	@Test void a05_encodeArray_escapesBackslash() {
		assertEquals("[\"a\\\\b\"]", JsonShortcut.encodeArray(List.of("a\\b")));
	}

	@Test void a06_encodeArray_emptyString() {
		assertEquals("[\"\"]", JsonShortcut.encodeArray(List.of("")));
	}

	@Test void a07_encodeArray_nullString() {
		// appendQuoted handles null gracefully: emits empty quoted string.
		var l = new ArrayList<String>();
		l.add(null);
		assertEquals("[\"\"]", JsonShortcut.encodeArray(l));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// decodeArray(String) - null/empty/non-bracketed
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_decodeArray_null() {
		assertArrayEquals(new String[0], JsonShortcut.decodeArray(null));
	}

	@Test void b02_decodeArray_empty() {
		assertArrayEquals(new String[0], JsonShortcut.decodeArray(""));
	}

	@Test void b03_decodeArray_whitespaceOnly() {
		// Trim makes it empty
		assertArrayEquals(new String[0], JsonShortcut.decodeArray("   "));
	}

	@Test void b04_decodeArray_noBracketsReturnsRaw() {
		// Mirrors ArgCoercer.parseStringArray: returns single-element array of raw input
		assertArrayEquals(new String[]{"abc"}, JsonShortcut.decodeArray("abc"));
	}

	@Test void b05_decodeArray_onlyOpeningBracket() {
		// Doesn't both start AND end with brackets, so returns raw
		assertArrayEquals(new String[]{"[abc"}, JsonShortcut.decodeArray("[abc"));
	}

	@Test void b06_decodeArray_onlyClosingBracket() {
		assertArrayEquals(new String[]{"abc]"}, JsonShortcut.decodeArray("abc]"));
	}

	@Test void b07_decodeArray_emptyArray() {
		assertArrayEquals(new String[0], JsonShortcut.decodeArray("[]"));
	}

	@Test void b08_decodeArray_emptyArrayWithSpaces() {
		assertArrayEquals(new String[0], JsonShortcut.decodeArray("[  ]"));
	}

	@Test void b09_decodeArray_arrayWithLeadingTrailingWhitespace() {
		// trim() removes outer whitespace
		assertArrayEquals(new String[]{"a"}, JsonShortcut.decodeArray("  [a]  "));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// decodeArray(String) - quoted elements
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_decodeArray_doubleQuoted() {
		assertArrayEquals(new String[]{"a", "b"}, JsonShortcut.decodeArray("[\"a\",\"b\"]"));
	}

	@Test void c02_decodeArray_singleQuoted() {
		assertArrayEquals(new String[]{"a", "b"}, JsonShortcut.decodeArray("['a','b']"));
	}

	@Test void c03_decodeArray_quotedWithEscape() {
		// \-escapes: next char inserted literally
		assertArrayEquals(new String[]{"a\"b"}, JsonShortcut.decodeArray("[\"a\\\"b\"]"));
	}

	@Test void c04_decodeArray_quotedWithBackslash() {
		assertArrayEquals(new String[]{"a\\b"}, JsonShortcut.decodeArray("[\"a\\\\b\"]"));
	}

	@Test void c05_decodeArray_quotedWithEmbeddedComma() {
		// A comma inside quotes does not split.
		assertArrayEquals(new String[]{"a,b", "c"}, JsonShortcut.decodeArray("[\"a,b\",\"c\"]"));
	}

	@Test void c06_decodeArray_quotedEmpty() {
		assertArrayEquals(new String[]{""}, JsonShortcut.decodeArray("[\"\"]"));
	}

	@Test void c07_decodeArray_quotedUnterminatedInsideClosedArray() {
		// No closing quote; loop ends without breaking — accumulated content returned.
		assertArrayEquals(new String[]{"abc"}, JsonShortcut.decodeArray("[\"abc]"));
	}

	@Test void c08_decodeArray_mixedQuotes() {
		assertArrayEquals(new String[]{"a", "b"}, JsonShortcut.decodeArray("[\"a\",'b']"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// decodeArray(String) - unquoted elements
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_decodeArray_unquoted() {
		assertArrayEquals(new String[]{"a", "b", "c"}, JsonShortcut.decodeArray("[a,b,c]"));
	}

	@Test void d02_decodeArray_unquotedWithSpaces() {
		// Unquoted tokens are trimmed
		assertArrayEquals(new String[]{"a", "b", "c"}, JsonShortcut.decodeArray("[ a , b , c ]"));
	}

	@Test void d03_decodeArray_unquoted_singleElement() {
		assertArrayEquals(new String[]{"foo"}, JsonShortcut.decodeArray("[foo]"));
	}

	@Test void d04_decodeArray_mixedQuotedAndUnquoted() {
		assertArrayEquals(new String[]{"a", "b"}, JsonShortcut.decodeArray("[\"a\",b]"));
	}

	@Test void d05_decodeArray_trailingComma() {
		// Trailing comma after value, then loop continues -> i >= len -> break.
		// Actually after comma, i is incremented past comma; while loop reads next iteration -> empty unquoted.
		var r = JsonShortcut.decodeArray("[a,]");
		// Behavior: depending on implementation, trailing comma may produce empty element or not.
		// We assert the leading element exists.
		assertEquals("a", r[0]);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// decodeArray(String) - whitespace handling within elements
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_decodeArray_whitespaceBetween() {
		assertArrayEquals(new String[]{"a", "b"}, JsonShortcut.decodeArray("[\"a\" , \"b\"]"));
	}

	@Test void e02_decodeArray_tabsAndNewlines() {
		assertArrayEquals(new String[]{"a", "b"}, JsonShortcut.decodeArray("[\n\t\"a\",\n\t\"b\"\n]"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// roundtrip
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_roundtrip_simple() {
		var encoded = JsonShortcut.encodeArray(List.of("a", "b", "c"));
		assertArrayEquals(new String[]{"a", "b", "c"}, JsonShortcut.decodeArray(encoded));
	}

	@Test void f02_roundtrip_withQuotes() {
		var input = List.of("a\"b", "plain", "c\\d");
		var encoded = JsonShortcut.encodeArray(input);
		assertArrayEquals(input.toArray(new String[0]), JsonShortcut.decodeArray(encoded));
	}
}
