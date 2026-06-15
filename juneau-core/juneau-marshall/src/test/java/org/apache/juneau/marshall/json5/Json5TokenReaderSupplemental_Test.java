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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Supplemental branch coverage for {@link Json5TokenReader} surface not reached by
 * {@link Json5TokenStream_Test} or {@link Json5TokenReaderCoverage_Test} — the missing-value
 * detection for {@code ]} / } at value position, the empty-object field dispatch, and the
 * single-quoted string trim path.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT false-positive leak reports over chained factory calls.
})
class Json5TokenReaderSupplemental_Test extends TestBase {

	@Nested class A_missingValues extends TestBase {

		@Test void a01_emptyArrayEmitsMissingNull() throws Exception {
			// Documents current behavior: at value position a ']' is treated as a missing value
			// (VALUE_NULL) before the array closes.  Covers the ']' arm of the missing-value guard.
			try (var r = Json5Parser.DEFAULT.parseTokens("[]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a02_missingObjectValueEmitsNull() throws Exception {
			// {a:} — value position sees '}' and emits a missing VALUE_NULL.  Covers the '}' arm.
			try (var r = Json5Parser.DEFAULT.parseTokens("{a:}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void a03_emptyObjectClosesImmediately() throws Exception {
			// {} — field-name position sees '}' and closes the object via the JSON5 override.
			try (var r = Json5Parser.DEFAULT.parseTokens("{}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}
	}

	@Nested class B_canRead extends TestBase {

		@Test void b01_trailingCommaObjectCanReadReturnsFalse() throws Exception {
			// JSON5 allows trailing commas, so canRead() over {a:1,} consumes the comma, sees '}',
			// and (allowsTrailingComma() == true) returns without error — reporting no further value.
			try (var r = Json5Parser.DEFAULT.parseTokens("{a:1,}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertFalse(r.canRead());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}
	}

	@Nested class C_trim extends TestBase {

		@Test void b01_singleQuotedStringTrimmed() throws Exception {
			// Json5TokenReader(pipe, settings, session) with trimStrings=true exercises maybeTrimString's
			// trim branch on a single-quoted value.
			try (var r = new Json5TokenReader(new ParserPipe("'  hi  '"), new JsonTokenReader.Settings(true), null)) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hi", r.getString());
			}
		}
	}
}
