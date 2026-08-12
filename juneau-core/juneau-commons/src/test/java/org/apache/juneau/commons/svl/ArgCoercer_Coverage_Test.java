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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Direct-call coverage tests for {@link ArgCoercer}'s package-private {@code coerce()} /
 * {@code coerceOne()} entry points, filling gaps not exercised by {@link ArgCoercer_Test}'s
 * end-to-end {@code VarResolver} integration path (wrapper-type variants, null-arg handling,
 * variadic arity edges, and the inline JSON-array-shortcut parser's branch coverage).
 */
class ArgCoercer_Coverage_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// coerce() - arity checks (lines 95/98)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_coerce_variadic_tooFewFixedArgs_throws() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> ArgCoercer.coerce("fn", new Class<?>[] {String.class, String[].class}, List.of()));
		assertTrue(ex.getMessage().contains("expected at least"), ex.getMessage());
	}

	@Test
	void a02_coerce_variadic_excessArgs_allowed() {
		var out = ArgCoercer.coerce("fn", new Class<?>[] {String.class, String[].class}, List.of("a", "b", "c", "d"));
		assertEquals(2, out.length);
		assertEquals("a", out[0]);
		assertArrayEquals(new String[] {"b", "c", "d"}, (String[])out[1]);
	}

	@Test
	void a03_coerce_nonVariadic_exactArgCount_ok() {
		var out = ArgCoercer.coerce("fn", new Class<?>[] {String.class, String.class}, List.of("a", "b"));
		assertEquals(2, out.length);
	}

	@Test
	void a04_coerce_variadic_nullElement_becomesEmptyString() {
		var out = ArgCoercer.coerce("fn", new Class<?>[] {String[].class}, Arrays.asList((Object)null));
		assertArrayEquals(new String[] {""}, (String[])out[0]);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// coerceOne() - String target null handling (lines 133/134)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_coerceOne_stringTarget_nullRaw_becomesEmptyString() {
		assertEquals("", ArgCoercer.coerceOne("fn", 0, String.class, null));
	}

	@Test
	void b02_coerceOne_stringTarget_nonNullRaw_returnsToString() {
		assertEquals("42", ArgCoercer.coerceOne("fn", 0, String.class, 42));
	}

	@Test
	void b03_coerceOne_nonStringTarget_nullRaw_treatedAsEmptyString() {
		// null -> "" -> Integer.parseInt("") throws NumberFormatException -> wrapped.
		var ex = assertThrows(IllegalArgumentException.class, () -> ArgCoercer.coerceOne("fn", 0, int.class, null));
		assertTrue(ex.getMessage().contains("cannot coerce"), ex.getMessage());
	}

	@Test
	void b04_coerceOne_nonStringTarget_nonStringRawObject_usesToString() {
		assertEquals(42, ArgCoercer.coerceOne("fn", 0, int.class, 42));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// coerceOne() - wrapper-type variants of each numeric/boolean branch (lines 136/138/140/142)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_coerceOne_integerWrapperTarget() {
		assertEquals(5, ArgCoercer.coerceOne("fn", 0, Integer.class, "5"));
	}

	@Test
	void c02_coerceOne_longWrapperTarget() {
		assertEquals(5L, ArgCoercer.coerceOne("fn", 0, Long.class, "5"));
	}

	@Test
	void c03_coerceOne_doubleWrapperTarget() {
		assertEquals(5.5, ArgCoercer.coerceOne("fn", 0, Double.class, "5.5"));
	}

	@Test
	void c04_coerceOne_booleanWrapperTarget() {
		assertEquals(Boolean.TRUE, ArgCoercer.coerceOne("fn", 0, Boolean.class, "true"));
	}

	@Test
	void c05_coerceOne_objectTarget_returnsRawString() {
		assertEquals("raw", ArgCoercer.coerceOne("fn", 0, Object.class, "raw"));
	}

	@Test
	void c06_coerceOne_unrecognizedTargetType_passesThroughAsString() {
		assertEquals("42", ArgCoercer.coerceOne("fn", 0, java.util.Date.class, "42"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// coerceOne(String[].class, ...) -> parseStringArray() branch coverage (lines 189-233)
	//-----------------------------------------------------------------------------------------------------------------

	private static String[] parseArr(String raw) {
		return (String[])ArgCoercer.coerceOne("fn", 0, String[].class, raw);
	}

	@Test
	void d01_parseStringArray_emptyInput_returnsEmptyArray() {
		assertArrayEquals(new String[0], parseArr(""));
	}

	@Test
	void d02_parseStringArray_notBracketed_wrapsAsSingleElement() {
		assertArrayEquals(new String[] {"hello"}, parseArr("hello"));
	}

	@Test
	void d03_parseStringArray_startsWithBracketButDoesNotEnd_wrapsAsSingleElement() {
		// Exercises the "startsWith true, endsWith false" combination of the bracket check.
		assertArrayEquals(new String[] {"[abc"}, parseArr("[abc"));
	}

	@Test
	void d04_parseStringArray_endsWithBracketButDoesNotStart_wrapsAsSingleElement() {
		// Exercises the "startsWith false, endsWith true" combination of the bracket check.
		assertArrayEquals(new String[] {"abc]"}, parseArr("abc]"));
	}

	@Test
	void d05_parseStringArray_emptyBracketBody_returnsEmptyArray() {
		assertArrayEquals(new String[0], parseArr("[]"));
	}

	@Test
	void d06_parseStringArray_emptyBracketBodyWithWhitespace_returnsEmptyArray() {
		assertArrayEquals(new String[0], parseArr("[   ]"));
	}

	@Test
	void d07_parseStringArray_doubleQuotedElements() {
		assertArrayEquals(new String[] {"a", "b"}, parseArr("[\"a\",\"b\"]"));
	}

	@Test
	void d08_parseStringArray_singleQuotedElements() {
		assertArrayEquals(new String[] {"a", "b"}, parseArr("['a','b']"));
	}

	@Test
	void d09_parseStringArray_escapedQuoteInsideQuotedElement() {
		assertArrayEquals(new String[] {"a\"b"}, parseArr("[\"a\\\"b\"]"));
	}

	@Test
	void d10_parseStringArray_unquotedElements_splitOnComma() {
		assertArrayEquals(new String[] {"a", "b", "c"}, parseArr("[a,b,c]"));
	}

	@Test
	void d11_parseStringArray_whitespaceBetweenElementsTrimmed() {
		assertArrayEquals(new String[] {"a", "b"}, parseArr("[ a , b ]"));
	}

	@Test
	void d12_parseStringArray_mixedQuotedAndUnquotedElements() {
		assertArrayEquals(new String[] {"a", "b", "c"}, parseArr("[\"a\", b, 'c']"));
	}

	@Test
	void d13_parseStringArray_singleUnquotedElement_noTrailingComma() {
		assertArrayEquals(new String[] {"a"}, parseArr("[a]"));
	}

	@Test
	void d14_parseStringArray_malformedMissingComma_throws() {
		// After the closing quote, the next non-whitespace char ('b') is neither ',' nor end-of-input.
		var ex = assertThrows(IllegalArgumentException.class, () -> parseArr("[\"a\"b]"));
		assertTrue(ex.getMessage().contains("malformed JSON array"), ex.getMessage());
	}
}
