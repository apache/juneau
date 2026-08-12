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

import static org.apache.juneau.BasicTestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link JsonParserSession} targeting the low-level state-machine error paths in
 * {@code readIntoBeanMap2}, {@code readIntoCollection2}, {@code readIntoMap2}, {@code readNumber}, {@code readString},
 * {@code skipCommentsAndSpace}, {@code readFieldName}, {@code readBoolean}, and {@code validateEnd} that aren't
 * already exercised by {@code Json_Test} / {@code JsonParser_Test}.
 *
 * <p>
 * Note: the post-loop "fallback" error branches in the {@code readIntoBeanMap2}/{@code readIntoMap2}
 * state machines (the S2/"Could not find attribute name" and S4/"Expected one of the following characters" arms),
 * the EOF branch of {@code readString}'s "Could not find expected end character" check, and {@code readKeyword}'s
 * {@code IndexOutOfBoundsException} catch were confirmed structurally unreachable (nested calls always throw a more
 * specific exception first) and have been deleted rather than tested here.
 */
class JsonParserSession_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a. readFieldName / readBoolean top-level errors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_unquotedAttributeName() {
		assertThrowsWithMessage(ParseException.class, "Unquoted attribute detected", () -> JsonParser.DEFAULT.read("{foo:1}", JsonMap.class));
	}

	@Test void a02_invalidBooleanSyntax() {
		assertThrowsWithMessage(ParseException.class, "Unrecognized syntax", () -> JsonParser.DEFAULT.read("x", Boolean.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. readIntoBeanMap2 state machine (concrete bean target)
	//-----------------------------------------------------------------------------------------------------------------

	public static class BBean {
		public String name;
	}

	@Test void b01_missingOpeningBrace() {
		assertThrowsWithMessage(ParseException.class, "Expected '{' at beginning of JSON object", () -> JsonParser.DEFAULT.read("x", BBean.class));
	}

	@Test void b03_missingColon() {
		// Quoted attribute name (so readFieldName succeeds) followed immediately by EOF, before ':' is found.
		assertThrowsWithMessage(ParseException.class, "Could not find ':'", () -> JsonParser.DEFAULT.read("{\"name\"", BBean.class));
	}

	@Test void b05_missingClosingBrace() {
		assertThrowsWithMessage(ParseException.class, "Could not find '}'", () -> JsonParser.DEFAULT.read("{\"name\":\"x\"", BBean.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. readIntoCollection2 state machine
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_missingOpeningBracket() {
		assertThrowsWithMessage(ParseException.class, "Expected '[' at beginning of JSON array", () -> JsonParser.DEFAULT.read("x", List.class, Object.class));
	}

	@Test void c02_invalidCharacterAfterOpeningBracket() {
		assertThrowsWithMessage(ParseException.class, "Expected one of the following characters", () -> JsonParser.DEFAULT.read("[", List.class, Object.class));
	}

	@Test void c03_missingCommaOrClosingBracket() {
		assertThrowsWithMessage(ParseException.class, "Expected ',' or ']'", () -> JsonParser.DEFAULT.read("[1", List.class, Object.class));
	}

	@Test void c04_trailingComma() {
		assertThrowsWithMessage(ParseException.class, "Unexpected trailing comma", () -> JsonParser.DEFAULT.read("[1,", List.class, Object.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. readIntoMap2 state machine (generic Object/Map target)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d02_missingColon() {
		// Quoted attribute name followed immediately by EOF, before ':' is found.
		assertThrowsWithMessage(ParseException.class, "Could not find ':'", () -> JsonParser.DEFAULT.read("{\"a\"", JsonMap.class));
	}

	@Test void d03_missingClosingBrace() {
		assertThrowsWithMessage(ParseException.class, "Could not find '}'", () -> JsonParser.DEFAULT.read("{\"a\":1", JsonMap.class));
	}

	@Test void d04_unexpectedClosingBraceAfterComma() {
		assertThrowsWithMessage(ParseException.class, "Unexpected '}' found", () -> JsonParser.DEFAULT.read("{\"a\":1,}", JsonMap.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e. readNumber invalid-format errors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_leadingZeroFollowedByDigit() {
		assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> JsonParser.DEFAULT.read("00", Integer.class));
	}

	@Test void e02_trailingDotNoFraction() {
		assertThrowsWithMessage(ParseException.class, "Invalid JSON number", () -> JsonParser.DEFAULT.read("1.", Integer.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f. readString errors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_invalidQuoteCharacter() {
		assertThrowsWithMessage(ParseException.class, "Invalid quote character", () -> JsonParser.DEFAULT.read("'x", String.class));
	}

	@Test void f03_invalidEscapeSequence() {
		assertThrowsWithMessage(ParseException.class, "Invalid escape sequence", () -> JsonParser.DEFAULT.read("\"\\x\"", String.class));
	}

	@Test void f04_invalidUnicodeEscapeSequence() {
		assertThrowsWithMessage(ParseException.class, "Invalid Unicode escape sequence", () -> JsonParser.DEFAULT.read("\"\\uZZZZ\"", String.class));
	}

	@Test void f05_stringConcatenationDetected() {
		assertThrowsWithMessage(ParseException.class, "String concatenation detected", () -> JsonParser.DEFAULT.read("\"foo\"+\"bar\"", String.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g. skipCommentsAndSpace / validateEnd
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g02_javascriptCommentDetected() {
		var p = JsonParser.create().validateEnd(true).build();
		assertThrowsWithMessage(ParseException.class, "Javascript comment detected", () -> p.read("{\"a\":1} /x", JsonMap.class));
	}

	@Test void g03_remainderAfterParse() {
		var p = JsonParser.create().validateEnd(true).build();
		assertThrowsWithMessage(ParseException.class, "Remainder after parse", () -> p.read("{\"a\":1} x", JsonMap.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h. readKeyword mismatch
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h02_keywordMismatch() {
		assertThrowsWithMessage(ParseException.class, "Unrecognized syntax", () -> JsonParser.DEFAULT.read("txyz", Boolean.class));
	}
}
