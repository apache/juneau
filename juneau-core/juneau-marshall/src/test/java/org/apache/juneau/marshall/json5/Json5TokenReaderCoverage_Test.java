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
package org.apache.juneau.marshall.json5;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link Json5TokenReader} &mdash; the JSON5 dialect tokenizer behind
 * {@link Json5Parser#parseTokens(Object)}.
 *
 * <p>
 * Targets the JSON5-specific relaxations layered on top of {@link JsonTokenReader}: single-quoted
 * strings (with the full escape matrix), bare/unquoted identifiers as both values and field names,
 * trailing commas, missing values emitted as {@code VALUE_NULL}, and the dialect-specific
 * comma/end dispatch overrides.  Also pins which JSON5-spec number forms are intentionally NOT
 * implemented by this cursor.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class Json5TokenReaderCoverage_Test extends TestBase {

	private static void drain(String json) throws Exception {
		try (var r = Json5Parser.DEFAULT.parseTokens(json)) {
			while (r.next() != TokenType.END_OF_STREAM) {
				// drain
			}
		}
	}

	// =================================================================================
	// A. Single-quoted strings + escapes
	// =================================================================================

	@Nested class A_singleQuoted extends TestBase {

		@Test void a01_singleQuotedValues() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("['a','b']")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("a", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("b", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
			try (var r = Json5Parser.DEFAULT.parseTokens("'hi'")) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hi", r.getString());
			}
		}

		@Test void a02_singleQuotedAllEscapes() throws Exception {
			// Exercises every case in the single-quoted escape state machine (newline, carriage-return,
			// tab, form-feed, backspace, backslash, solidus, single-quote, double-quote, unicode).
			var input = "['\\n','\\r','\\t','\\f','\\b','\\\\','\\/','\\'','\\\"','\\u0041']";
			var expected = new String[]{"\n", "\r", "\t", "\f", "\b", "\\", "/", "'", "\"", "A"};
			try (var r = Json5Parser.DEFAULT.parseTokens(input)) {
				assertEquals(TokenType.START_ARRAY, r.next());
				for (var e : expected) {
					assertEquals(TokenType.VALUE_STRING, r.next());
					assertEquals(e, r.getString());
				}
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a03_singleQuotedInvalidEscapeRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid escape sequence in string", () -> drain("['\\x']"));
		}

		@Test void a04_singleQuotedInvalidUnicodeRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid Unicode escape sequence in string", () -> drain("['\\uZZZZ']"));
		}

		@Test void a05_singleQuotedUnterminatedRejected() {
			assertThrowsWithMessage(ParseException.class, "Unterminated string", () -> drain("'abc"));
		}

		@Test void a06_singleQuotedFieldName() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("{'foo':1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("foo", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}
	}

	// =================================================================================
	// B. Bare / unquoted identifiers
	// =================================================================================

	@Nested class B_bareIdentifiers extends TestBase {

		@Test void b01_bareValueStrings() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("[foo, bar, baz]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("foo", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("bar", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("baz", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void b02_bareStartVariants() throws Exception {
			// isBareStart accepts letters, '_' and '$'; isBareCont additionally accepts digits.
			try (var r = Json5Parser.DEFAULT.parseTokens("[$foo, _bar, a1b2_$]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("$foo", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("_bar", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("a1b2_$", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void b03_bareKeywordsResolveToTokens() throws Exception {
			// true/false/null bare-words must resolve to boolean/null tokens, not strings.
			try (var r = Json5Parser.DEFAULT.parseTokens("[true, false, null]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertTrue(r.getBool());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertFalse(r.getBool());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void b04_bareFieldNames() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("{foo: 1, bar: 2}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("foo", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("bar", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void b05_bareWordAtEndOfInput() throws Exception {
			// readBareWord terminates on EOF (the c==-1 branch) rather than a delimiter.
			try (var r = Json5Parser.DEFAULT.parseTokens("hello")) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hello", r.getString());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}

	// =================================================================================
	// C. Trailing commas + missing values
	// =================================================================================

	@Nested class C_trailingAndMissing extends TestBase {

		@Test void c01_trailingCommaInArray() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("[1,2,3,]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void c02_trailingCommaInObject() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("{a:1, b:2,}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void c03_missingValuesInArrayAsNull() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("[,1,,3,]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(3L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void c04_missingValueInObjectAsNull() throws Exception {
			// `{a:,b:1}` -> a is the missing-value VALUE_NULL, b is 1.
			try (var r = Json5Parser.DEFAULT.parseTokens("{a:,b:1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("b", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void c05_trailingCommaStreamingWithCanRead() throws Exception {
			var seen = new ArrayList<Integer>();
			try (var r = Json5Parser.DEFAULT.parseTokens("[1,2,3,]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				while (r.canRead())
					seen.add(r.read(Integer.class));
				assertEquals(TokenType.END_ARRAY, r.next());
			}
			assertList(seen, 1, 2, 3);
		}
	}

	// =================================================================================
	// D. Delegation to the JSON parent (double-quoted strings, numbers, structural)
	// =================================================================================

	@Nested class D_parentDelegation extends TestBase {

		@Test void d01_doubleQuotedStringsAndNumbers() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("[\"x\", 1, 2.5, true]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("x", r.getString());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals("1", r.getNumberLexeme());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2.5d, r.getNumber().doubleValue());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertTrue(r.getBool());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void d02_doubleQuotedFieldName() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("{\"a\":1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void d03_mixedQuotingStyles() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("{foo:1, 'bar':2, \"baz\":3}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("foo", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("bar", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("baz", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void d04_comments() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("/* lead */ [1, /* mid */ 2 // trail\n]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}
	}

	// =================================================================================
	// E. JSON5-dialect separator errors
	// =================================================================================

	@Nested class E_separatorErrors extends TestBase {

		@Test void e01_badSeparatorInArray() {
			assertThrowsWithMessage(ParseException.class, "Expected , or ]", () -> drain("[1 2]"));
		}

		@Test void e02_badSeparatorInObject() {
			assertThrowsWithMessage(ParseException.class, "Expected , or }", () -> drain("{a:1 b:2}"));
		}

		@Test void e03_endOfInputAfterArrayComma() {
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input after ','", () -> drain("[1,"));
		}

		@Test void e04_endOfInputAfterObjectComma() {
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input after ','", () -> drain("{a:1,"));
		}
	}

	// =================================================================================
	// F. Intentionally-unsupported JSON5 number forms
	// =================================================================================

	@Nested class F_unsupportedNumberForms extends TestBase {

		@Test void f01_infinityAndNaNAreBareStrings() throws Exception {
			// This cursor does not implement JSON5 numeric Infinity/NaN; bare-words become strings.
			try (var r = Json5Parser.DEFAULT.parseTokens("[Infinity, NaN]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("Infinity", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("NaN", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void f02_leadingPlusRejected() {
			// '+' is neither a bare-start nor a JSON number-start, so it is an unexpected char.
			assertThrowsWithMessage(ParseException.class, "Unexpected character", () -> drain("[+1]"));
		}

		@Test void f03_leadingDecimalPointRejected() {
			assertThrowsWithMessage(ParseException.class, "Unexpected character", () -> drain("[.5]"));
		}

		@Test void f04_hexNumberRejected() {
			// '0' starts a number lexeme, the 'x' fails JSON number validation.
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("[0x1F]"));
		}
	}

	// =================================================================================
	// G. read() bridge + trimStrings
	// =================================================================================

	@Nested class G_readAndSettings extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void g01_readBeanFromBareKeys() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("{name:'alice', age:30}")) {
				var b = r.read(Bean.class);
				assertBean(b, "name,age", "alice,30");
			}
		}

		@Test void g02_streamArrayOfBeansWithTrailingComma() throws Exception {
			var seen = new ArrayList<Bean>();
			try (var r = Json5Parser.DEFAULT.parseTokens("[{name:'a',age:1},{name:'b',age:2},]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				while (r.canRead())
					seen.add(r.read(Bean.class));
				assertEquals(TokenType.END_ARRAY, r.next());
			}
			assertEquals(2, seen.size());
			assertBean(seen.get(0), "name,age", "a,1");
			assertBean(seen.get(1), "name,age", "b,2");
		}

		@Test void g03_trimStringsOnSingleQuotedAndBare() throws Exception {
			var p = Json5Parser.create().trimStrings().build();
			try (var r = p.parseTokens("{ '  k  ' : '  v  ' }")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("k", r.getFieldName());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("v", r.getString());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void g04_defaultDoesNotTrim() throws Exception {
			try (var r = Json5Parser.DEFAULT.parseTokens("['  v  ']")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("  v  ", r.getString());
			}
		}

		@Test void g05_readWithoutSessionThrows() throws Exception {
			try (var r = new Json5TokenReader(new ParserPipe("{a:1}"))) {
				assertThrows(UnsupportedOperationException.class, () -> r.read(Bean.class));
			}
		}

		@Test void g06_directConstructionStillTokenizes() throws Exception {
			try (var r = new Json5TokenReader(new ParserPipe("{a:1}"))) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void g07_capabilityIsStreaming() throws Exception {
			assertInstanceOf(TokenReadable.class, Json5Parser.DEFAULT);
			try (var r = Json5Parser.DEFAULT.parseTokens("null")) {
				assertReaderStreaming(r);
			}
		}
	}
}
