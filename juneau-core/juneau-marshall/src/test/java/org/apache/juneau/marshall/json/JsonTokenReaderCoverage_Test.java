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
package org.apache.juneau.marshall.json;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link JsonTokenReader} &mdash; the structural JSON tokenizer behind
 * {@link JsonParser#parseTokens(Object)}.
 *
 * <p>
 * These tests deliberately drive every tokenizer branch (scalar shapes, every escape sequence,
 * the number-lexeme validator, whitespace/comment skipping, the container stack overflow path,
 * the {@code read}/{@code canRead}/{@code skipChildren} bridges, and the malformed-input error
 * paths) to raise JaCoCo line+branch coverage toward 100%.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class JsonTokenReaderCoverage_Test extends TestBase {

	private static void drain(String json) throws Exception {
		try (var r = JsonParser.DEFAULT.parseTokens(json)) {
			while (r.next() != TokenType.END_OF_STREAM) {
				// drain
			}
		}
	}

	// =================================================================================
	// A. Scalar accessors and getString() views
	// =================================================================================

	@Nested class A_accessors extends TestBase {

		@Test void a01_getStringForEveryTokenView() throws Exception {
			// getString() is defined for VALUE_STRING, FIELD_NAME, VALUE_NUMBER (lexeme),
			// VALUE_BOOLEAN ("true"/"false") and VALUE_NULL (null).
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":\"s\",\"b\":12,\"c\":true,\"d\":false,\"e\":null}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("a", r.getString());           // FIELD_NAME view
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("s", r.getString());           // VALUE_STRING view
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("12", r.getString());          // VALUE_NUMBER lexeme view
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next());
				assertEquals("true", r.getString());        // VALUE_BOOLEAN view
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next());
				assertEquals("false", r.getString());       // VALUE_BOOLEAN view
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertNull(r.getString());                  // VALUE_NULL view
			}
		}

		@Test void a02_getStringOnStructuralTokenThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertThrows(IllegalStateException.class, r::getString);
			}
		}

		@Test void a03_getNumberIsLazyAndCached() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[7]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				var n1 = r.getNumber();
				var n2 = r.getNumber();   // second call hits the cached-Number branch
				assertSame(n1, n2);
				assertEquals(7L, n1.longValue());
			}
		}

		@Test void a04_getBinaryAlwaysThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("\"hi\"")) {
				r.next();
				assertThrowsWithMessage(IllegalStateException.class, "JSON does not produce VALUE_BINARY tokens", r::getBinary);
			}
		}

		@Test void a05_getFieldNameWrongStateThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1]")) {
				r.next();  // START_ARRAY
				assertThrows(IllegalStateException.class, r::getFieldName);
			}
		}

		@Test void a06_nextAfterEndOfStreamStaysEndOfStream() throws Exception {
			// Hits the early-return state==S05_end branch in next().
			try (var r = JsonParser.DEFAULT.parseTokens("1")) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}

	// =================================================================================
	// B. Number lexeme matrix + validator
	// =================================================================================

	@Nested class B_numbers extends TestBase {

		@Test void b01_validNumberMatrix() throws Exception {
			var input = "[0, -0, 12, -7, 3.14, -2.5, 0.5, 100, 1e10, 1E10, 1e+10, 1e-10, 2.5E-3, 123456789012345]";
			var expectedLexemes = new String[]{
				"0", "-0", "12", "-7", "3.14", "-2.5", "0.5", "100",
				"1e10", "1E10", "1e+10", "1e-10", "2.5E-3", "123456789012345"
			};
			try (var r = JsonParser.DEFAULT.parseTokens(input)) {
				assertEquals(TokenType.START_ARRAY, r.next());
				for (var lex : expectedLexemes) {
					assertEquals(TokenType.VALUE_NUMBER, r.next());
					assertEquals(lex, r.getNumberLexeme());
				}
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void b02_numberValuesParse() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[0, 12, 3.14, 1e3]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  assertEquals(0L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  assertEquals(12L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  assertEquals(3.14d, r.getNumber().doubleValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  assertEquals(1000d, r.getNumber().doubleValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void b03_topLevelNegativeOnlyLexeme() throws Exception {
			// "-" survives lexeme validation (length-1 minus branch) even though it is not a real number.
			try (var r = JsonParser.DEFAULT.parseTokens("-")) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("-", r.getNumberLexeme());
			}
		}

		@Test void b04_leadingZeroRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("01"));
		}

		@Test void b05_doubleZeroRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("00"));
		}

		@Test void b06_trailingDecimalPointRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("[1.]"));
		}

		@Test void b07_decimalPointThenNonDigitRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("1.e5"));
		}

		@Test void b08_leadingDecimalAfterMinusRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("-.5"));
		}
	}

	// =================================================================================
	// C. Strings + escapes
	// =================================================================================

	@Nested class C_strings extends TestBase {

		@Test void c01_allEscapeSequences() throws Exception {
			// \" \\ \/ \b \f \n \r \t and \' (the parent accepts \' even in double-quoted strings).
			try (var r = JsonParser.DEFAULT.parseTokens("[\"\\\"\", \"\\\\\", \"\\/\", \"\\b\", \"\\f\", \"\\n\", \"\\r\", \"\\t\", \"\\'\"]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\"", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\\", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("/", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\b", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\f", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\n", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\r", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\t", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("'", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void c02_unicodeEscape() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[\"\\u00e9\", \"\\u0041BC\"]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("\u00e9", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("ABC", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void c03_invalidUnicodeEscapeRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid Unicode escape sequence in string", () -> drain("\"\\uZZZZ\""));
		}

		@Test void c04_invalidEscapeRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid escape sequence in string", () -> drain("\"\\x\""));
		}

		@Test void c05_unterminatedStringRejected() {
			assertThrowsWithMessage(ParseException.class, "Unterminated string", () -> drain("\"abc"));
		}

		@Test void c06_unescapedControlCharRejected() {
			// A raw control character (U+0001) inside a string is illegal.
			assertThrowsWithMessage(ParseException.class, "Unescaped control character", () -> drain("[\"a\u0001b\"]"));
		}

		@Test void c07_trimStringsTrimsValuesAndFieldNames() throws Exception {
			var p = JsonParser.create().trimStrings().build();
			try (var r = p.parseTokens("{\"  k  \":\"  v  \"}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("k", r.getFieldName());
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("v", r.getString());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void c08_defaultPreservesWhitespace() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[\"  v  \"]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("  v  ", r.getString());
			}
		}

		@Test void c09_largeStringForcesBufferRefill() throws Exception {
			// A long string + many array elements exercises any internal buffer growth/refill path.
			var big = "x".repeat(10000);
			var sb = new StringBuilder("[");
			for (var i = 0; i < 200; i++) {
				if (i > 0) sb.append(',');
				sb.append('"').append(big).append('"');
			}
			sb.append(']');
			try (var r = JsonParser.DEFAULT.parseTokens(sb.toString())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				for (var i = 0; i < 200; i++) {
					assertEquals(TokenType.VALUE_STRING, r.next());
					assertEquals(big, r.getString());
				}
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}

	// =================================================================================
	// D. Keyword literals
	// =================================================================================

	@Nested class D_keywords extends TestBase {

		@Test void d01_trueFalseNull() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[true,false,null]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertTrue(r.getBool());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertFalse(r.getBool());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void d02_malformedKeywordsRejected() {
			assertThrows(ParseException.class, () -> drain("trve"));
			assertThrows(ParseException.class, () -> drain("fals"));
			assertThrows(ParseException.class, () -> drain("nul"));
		}

		@Test void d03_getBoolWrongStateThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("\"hi\"")) {
				r.next();
				assertThrows(IllegalStateException.class, r::getBool);
			}
		}
	}

	// =================================================================================
	// E. Whitespace + comments
	// =================================================================================

	@Nested class E_whitespaceAndComments extends TestBase {

		@Test void e01_whitespaceBetweenTokens() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{ \n\t\"a\" :\r 1 ,\n\"b\" : 2 }")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("b", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2L, r.getNumber().longValue());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void e02_blockComments() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("/* lead */ [1, /* mid */ 2 /* trail */]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void e03_lineComments() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("// lead\n[1,// mid\n2]// trail")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void e04_unterminatedBlockCommentRejected() {
			assertThrowsWithMessage(ParseException.class, "Unterminated block comment", () -> drain("[1 /* unterminated"));
		}

		@Test void e05_openEndedCommentRejected() {
			assertThrowsWithMessage(ParseException.class, "Open ended comment", () -> drain("/x"));
		}
	}

	// =================================================================================
	// F. Structural errors
	// =================================================================================

	@Nested class F_structuralErrors extends TestBase {

		@Test void f01_truncatedObject() {
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input", () -> drain("{\"a\":1"));
		}

		@Test void f02_unmatchedCloser() {
			assertThrows(ParseException.class, () -> drain("]"));
		}

		@Test void f03_missingColon() {
			assertThrows(ParseException.class, () -> drain("{\"a\" 1}"));
		}

		@Test void f04_endOfInputAfterColon() {
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input after ':'", () -> drain("{\"a\":"));
		}

		@Test void f05_endOfInputAfterObjectComma() {
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input after ','", () -> drain("{\"a\":1,"));
		}

		@Test void f06_endOfInputAfterArrayComma() {
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input after ','", () -> drain("[1,"));
		}

		@Test void f07_missingCommaInObject() {
			assertThrowsWithMessage(ParseException.class, "Expected , or }", () -> drain("{\"a\":1 \"b\":2}"));
		}

		@Test void f08_missingCommaInArray() {
			assertThrowsWithMessage(ParseException.class, "Expected , or ]", () -> drain("[1 2]"));
		}

		@Test void f09_nonStringFieldNameRejected() {
			assertThrowsWithMessage(ParseException.class, "Expected \" for field name", () -> drain("{a:1}"));
		}

		@Test void f10_unexpectedCharAtValuePosition() {
			assertThrowsWithMessage(ParseException.class, "Unexpected character", () -> drain("@"));
			assertThrowsWithMessage(ParseException.class, "Unexpected character", () -> drain("[@]"));
		}

		@Test void f11_closeBracketAtObjectValuePosition() {
			// ']' at a value position that is not inside an array is an unexpected char.
			assertThrowsWithMessage(ParseException.class, "Unexpected character", () -> drain("{\"a\":]}"));
		}

		@Test void f12_getNumberWrongStateThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("\"hi\"")) {
				r.next();
				assertThrows(IllegalStateException.class, r::getNumber);
				assertThrows(IllegalStateException.class, r::getNumberLexeme);
			}
		}
	}

	// =================================================================================
	// G. Nesting, depth, container overflow
	// =================================================================================

	@Nested class G_nesting extends TestBase {

		@Test void g01_deeplyNestedArraysBeyond64Levels() throws Exception {
			// >64 levels of nesting forces the heap-allocated container-overflow array (and growth).
			var depth = 80;
			var input = "[".repeat(depth) + "1" + "]".repeat(depth);
			try (var r = JsonParser.DEFAULT.parseTokens(input)) {
				var max = 0;
				for (var i = 0; i < depth; i++) {
					assertEquals(TokenType.START_ARRAY, r.next());
					max = Math.max(max, r.getDepth());
				}
				assertEquals(depth, max);
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(depth, r.getDepth());
				for (var i = 0; i < depth; i++)
					assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void g02_deeplyNestedObjectsBeyond64Levels() throws Exception {
			var depth = 70;
			var sb = new StringBuilder();
			for (var i = 0; i < depth; i++)
				sb.append("{\"a\":");
			sb.append("1");
			sb.append("}".repeat(depth));
			try (var r = JsonParser.DEFAULT.parseTokens(sb.toString())) {
				while (r.next() != TokenType.END_OF_STREAM) {
					// drain; overflow object container handling is exercised
				}
			}
		}

		@Test void g03_nestedMixedDocument() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":[1,[2,3],{\"b\":4}]}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}

	// =================================================================================
	// H. skipChildren
	// =================================================================================

	@Nested class H_skipChildren extends TestBase {

		@Test void h01_skipObjectSubtree() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[{\"a\":1,\"b\":[2,3]},42]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.START_OBJECT, r.next());
				r.skipChildren();
				assertEquals(TokenType.END_OBJECT, r.getCurrentToken());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(42L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void h02_skipArraySubtree() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[[1,2,[3]],99]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.START_ARRAY, r.next());
				r.skipChildren();
				assertEquals(TokenType.END_ARRAY, r.getCurrentToken());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(99L, r.getNumber().longValue());
			}
		}

		@Test void h03_skipChildrenOnScalarIsNoop() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1,2]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				r.skipChildren();
				assertEquals(TokenType.VALUE_NUMBER, r.getCurrentToken());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(2L, r.getNumber().longValue());
			}
		}
	}

	// =================================================================================
	// I. canRead() advance-to-value-state branches
	// =================================================================================

	@Nested class I_canRead extends TestBase {

		@Test void i01_emptyContainersCanReadFalse() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertFalse(r.canRead());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void i02_canReadConsumesArrayComma() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1,2]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(1, r.read(Integer.class));
				assertTrue(r.canRead());   // S04 comma path -> S00 -> peek value
				assertEquals(2, r.read(Integer.class));
				assertFalse(r.canRead());  // S04 end-of-array path
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void i03_canReadFalseAtObjectFieldName() throws Exception {
			// After reading a value inside an object, the cursor is at S03; canRead consumes the
			// comma, lands at a field name, and reports false (not a value position).
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1,\"b\":2}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  // value -> S03
				assertFalse(r.canRead());
			}
		}

		@Test void i04_canReadFalseAtObjectEnd() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  // value -> S03
				assertFalse(r.canRead());  // S03 sees '}' -> end-of-container path
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void i05_canReadAtEndOfInputInArray() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  // S04, input now exhausted
				assertFalse(r.canRead());  // S04 c==-1 path
			}
		}

		@Test void i06_trailingCommaRejectedInArrayViaCanRead() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1,]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrowsWithMessage(ParseException.class, "Trailing comma not allowed", r::canRead);
			}
		}

		@Test void i07_trailingCommaRejectedInObjectViaCanRead() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1,}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrowsWithMessage(ParseException.class, "Trailing comma not allowed", r::canRead);
			}
		}

		@Test void i08_canReadAfterFieldNameConsumesColon() throws Exception {
			// From S02 (just emitted FIELD_NAME), canRead consumes the ':' and reports true.
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());  // S02
				assertTrue(r.canRead());
				assertEquals(1, r.read(Integer.class));
			}
		}

		@Test void i09_canReadBadSeparatorInArray() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1 2]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrowsWithMessage(ParseException.class, "Expected , or ]", r::canRead);
			}
		}

		@Test void i10_canReadBadSeparatorInObject() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1 \"b\":2}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrowsWithMessage(ParseException.class, "Expected , or }", r::canRead);
			}
		}

		@Test void i11_canReadBadCharAfterFieldName() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\" 1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());  // S02
				assertThrowsWithMessage(ParseException.class, "Expected : after field name", r::canRead);
			}
		}
	}

	// =================================================================================
	// J. read() POJO bridge
	// =================================================================================

	@Nested class J_read extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void j01_readScalar() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("42")) {
				assertEquals(42, r.read(Integer.class));
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void j02_readBean() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"name\":\"alice\",\"age\":30}")) {
				var b = r.read(Bean.class);
				assertBean(b, "name,age", "alice,30");
			}
		}

		@Test void j03_readParameterizedTypeOverload() throws Exception {
			// Two-argument call resolves to read(Type, Type...) rather than read(Class).
			try (var r = JsonParser.DEFAULT.parseTokens("[1,2,3]")) {
				List<Integer> v = r.read(List.class, Integer.class);
				assertList(v, 1, 2, 3);
			}
		}

		@Test void j04_readClassMetaOverload() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[\"a\",\"b\"]")) {
				var cm = JsonParser.DEFAULT.getSession().getClassMeta(List.class);
				List<?> v = r.read(cm);
				assertList(v, "a", "b");
			}
		}

		@Test void j05_streamArrayOfBeans() throws Exception {
			var seen = new ArrayList<Bean>();
			try (var r = JsonParser.DEFAULT.parseTokens("[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				while (r.canRead())
					seen.add(r.read(Bean.class));
				assertEquals(TokenType.END_ARRAY, r.next());
			}
			assertEquals(2, seen.size());
			assertBean(seen.get(0), "name,age", "a,1");
			assertBean(seen.get(1), "name,age", "b,2");
		}

		@Test void j06_readAfterStartObjectThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertThrows(IllegalStateException.class, () -> r.read(Bean.class));
			}
		}

		@Test void j07_readAtEndOfArrayThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(1, r.read(Integer.class));
				assertFalse(r.canRead());
				assertThrows(IllegalStateException.class, () -> r.read(Integer.class));
			}
		}

		@Test void j08_readWithoutSessionThrows() throws Exception {
			// Direct construction (no session) leaves read disabled across all three overloads.
			try (var r = new JsonTokenReader(new ParserPipe("{\"a\":1}"))) {
				assertThrows(UnsupportedOperationException.class, () -> r.read(Bean.class));
				assertThrows(UnsupportedOperationException.class, () -> r.read((Type) Bean.class));
			}
		}

		@Test void j09_directConstructionStillTokenizes() throws Exception {
			// The no-session constructor path still produces a fully functional tokenizer.
			try (var r = new JsonTokenReader(new ParserPipe("{\"a\":1}"))) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}

	// =================================================================================
	// K. Top-level scalars + empties
	// =================================================================================

	@Nested class K_topLevel extends TestBase {

		@Test void k01_emptyObject() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void k02_emptyArray() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void k03_topLevelScalars() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("\"hi\"")) {
				assertEquals(TokenType.VALUE_STRING, r.next()); assertEquals("hi", r.getString());
			}
			try (var r = JsonParser.DEFAULT.parseTokens("true")) {
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertTrue(r.getBool());
			}
			try (var r = JsonParser.DEFAULT.parseTokens("null")) {
				assertEquals(TokenType.VALUE_NULL, r.next());
			}
			try (var r = JsonParser.DEFAULT.parseTokens("123")) {
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(123L, r.getNumber().longValue());
			}
		}

		@Test void k04_initialTokenIsNotAvailableAndStreaming() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[]")) {
				assertEquals(TokenType.NOT_AVAILABLE, r.getCurrentToken());
				assertTrue(r.isStreaming());
			}
		}
	}
}
