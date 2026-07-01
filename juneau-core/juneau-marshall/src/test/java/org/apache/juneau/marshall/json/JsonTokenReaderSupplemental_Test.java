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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Supplemental branch coverage for {@link JsonTokenReader} surface not reached by
 * {@link JsonTokenStream_Test} or {@link JsonTokenReaderCoverage_Test} — the public {@code (pipe,
 * settings)} constructor, string trimming, number-lexeme validation edges, comment scanning,
 * deep-nesting overflow stack, and the {@code read()} exception funnel.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT false-positive leak reports over chained factory calls.
})
class JsonTokenReaderSupplemental_Test extends TestBase {

	private static void drain(String json) throws Exception {
		try (var r = JsonParser.DEFAULT.parseTokens(json)) {
			while (r.next() != TokenType.END_OF_STREAM) { /* drain */ }
		}
	}

	@Nested class A_constructorAndTrim extends TestBase {

		@Test void a01_pipeSettingsConstructorTrimsStrings() throws Exception {
			// Exercises the public (pipe, settings) constructor and the trimStrings path of maybeTrim.
			try (var r = new JsonTokenReader(new ParserPipe("\"  hi  \""), new JsonTokenReader.Settings(true))) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hi", r.getString());
			}
		}

		@Test void a02_trimAppliesToFieldNames() throws Exception {
			try (var r = new JsonTokenReader(new ParserPipe("{\"  k  \":1}"), new JsonTokenReader.Settings(true))) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("k", r.getFieldName());
			}
		}
	}

	@Nested class B_eofAndNumbers extends TestBase {

		@Test void b01_eofInsideContainerThrows() {
			// EOF at depth>0 (state still S00_expectValue but depth != 0) is an error.
			assertThrowsWithMessage(ParseException.class, "Unexpected end of input", () -> drain("["));
		}

		@Test void b02_leadingZeroNumbersRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("01"));
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("-01"));
		}

		@Test void b03_zeroWithFractionOrExponentAccepted() {
			// '0' followed by '.' / 'e' / 'E' is the valid branch of the leading-zero guard.
			assertDoesNotThrow(() -> drain("0.5"));
			assertDoesNotThrow(() -> drain("0e1"));
			assertDoesNotThrow(() -> drain("0E1"));
		}

		@Test void b04_danglingDecimalPointRejected() {
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("1."));
		}

		@Test void b05_nonDigitAfterDecimalRejected() {
			// Reaches the isDigit(...) false branch in validateNumberLexeme.
			assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> drain("1.e0"));
		}

		@Test void b06_validDecimal() {
			assertDoesNotThrow(() -> drain("1.5"));
		}
	}

	@Nested class C_comments extends TestBase {

		@Test void c01_blockComment() {
			// '*' that does not immediately precede '/' stays inside the block.
			assertDoesNotThrow(() -> drain("/* a * b */ 1"));
		}

		@Test void c02_lineCommentTerminatedByNewline() {
			assertDoesNotThrow(() -> drain("// comment\n1"));
		}

		@Test void c03_lineCommentTerminatedByEof() {
			// A whole-document line comment is scanned through to EOF (the c == -1 branch of the
			// line-comment loop) and the document is then a valid empty input.
			assertDoesNotThrow(() -> drain("// just a comment, no newline"));
		}

		@Test void c04_unterminatedBlockComment() {
			assertThrowsWithMessage(ParseException.class, "Unterminated block comment", () -> drain("/* x"));
		}

		@Test void c05_openEndedComment() {
			assertThrowsWithMessage(ParseException.class, "Open ended comment", () -> drain("/x"));
		}
	}

	@Nested class D_deepNesting extends TestBase {

		@Test void d01_nestingBeyondOverflowGrowsTwice() throws Exception {
			// 85 levels: the bit-packed stack covers 0-63, the first overflow array (16 slots) covers
			// 64-79, and crossing 80 forces growOverflow() to double the overflow array.
			var depth = 85;
			var sb = new StringBuilder();
			for (var i = 0; i < depth; i++) sb.append('[');
			for (var i = 0; i < depth; i++) sb.append(']');
			try (var r = JsonParser.DEFAULT.parseTokens(sb.toString())) {
				for (var i = 0; i < depth; i++)
					assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(depth, r.getDepth());
			}
		}
	}

	@Nested class E_readFunnel extends TestBase {

		@Test void e01_readParseExceptionWrapped() throws Exception {
			// A string scalar bound to int funnels through read()'s catch block (generic wrap).
			try (var r = JsonParser.DEFAULT.parseTokens("\"abc\"")) {
				assertThrows(ParseException.class, () -> r.read(int.class));
			}
		}

		@Test void e02_readSurfacesParserParseException() throws Exception {
			// Malformed element content makes parseAnything throw a ParseException that the read()
			// funnel rethrows as-is (the 'instanceof ParseException' branch).
			try (var r = JsonParser.DEFAULT.parseTokens("[@]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertThrows(ParseException.class, () -> r.read(Object.class));
			}
		}
	}

	@Nested class F_canRead extends TestBase {

		@Test void f01_emptyArrayCanReadFalse() throws Exception {
			// canRead() peeks ']' at S00_expectValue and reports no value to read.
			try (var r = JsonParser.DEFAULT.parseTokens("[]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertFalse(r.canRead());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}
	}

	@Nested class G_canReadAtEof extends TestBase {

		@Test void g01_eofAfterCommaInArray() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("[1,")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertFalse(r.canRead());  // S04: comma then EOF
			}
		}

		@Test void g02_eofInObjectCommaOrEndState() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertFalse(r.canRead());  // S03: EOF where ',' or '}' expected
			}
		}

		@Test void g03_eofAfterCommaInObject() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1,")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertFalse(r.canRead());  // S03: comma then EOF
			}
		}

		@Test void g04_eofAfterFieldNameAwaitingColon() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\"")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertFalse(r.canRead());  // S02: EOF where ':' expected
			}
		}

		@Test void g05_trailingCommaRejectedViaCanRead() throws Exception {
			try (var r = JsonParser.DEFAULT.parseTokens("{\"a\":1,}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrowsWithMessage(ParseException.class, "Trailing comma not allowed", r::canRead);
			}
		}
	}
}
