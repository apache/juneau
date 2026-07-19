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
package org.apache.juneau.marshall.json5l;

import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the public JSON5L token-streaming surface.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class Json5lTokenStream_Test extends TestBase {

	// =================================================================================
	// A. Reader — JSON5L flat-sequence semantics with JSON5 dialect
	// =================================================================================

	@Nested class A_reader extends TestBase {

		@Test void a01_singleLine() throws Exception {
			try (var r = Json5lParser.DEFAULT.readTokens("{a:1}\n")) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME,
					TokenType.VALUE_NUMBER,
					TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a02_multipleLinesFlat() throws Exception {
			try (var r = Json5lParser.DEFAULT.readTokens("{a:1}\n{b:2}\n")) {
				assertSequence(r,
					TokenType.START_OBJECT, TokenType.FIELD_NAME, TokenType.VALUE_NUMBER, TokenType.END_OBJECT,
					TokenType.START_OBJECT, TokenType.FIELD_NAME, TokenType.VALUE_NUMBER, TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a03_capability() throws Exception {
			assertInstanceOf(TokenReadable.class, Json5lParser.DEFAULT);
			try (var r = Json5lParser.DEFAULT.readTokens("1\n")) {
				assertReaderStreaming(r);
			}
		}
	}

	// =================================================================================
	// B. Writer — strict default and json5Sugar output, newline per top-level value
	// =================================================================================

	@Nested class B_writer extends TestBase {

		@Test void b01_strictDefault() throws Exception {
			var sb = new StringWriter();
			try (var w = Json5lSerializer.DEFAULT.writeTokens(sb)) {
				w.startObject(); w.fieldName("a"); w.number(1); w.endObject();
				w.startObject(); w.fieldName("b"); w.number(2); w.endObject();
			}
			assertEquals("{\"a\":1}\n{\"b\":2}\n", sb.toString());
		}

		@Test void b02_sugarUnquotedKeysSingleQuotes() throws Exception {
			var s = Json5lSerializer.create().json5Sugar().build();
			var sb = new StringWriter();
			try (var w = s.writeTokens(sb)) {
				w.startObject(); w.fieldName("a"); w.string("x"); w.endObject();
				w.startObject(); w.fieldName("b"); w.string("y"); w.endObject();
			}
			assertEquals("{a:'x'}\n{b:'y'}\n", sb.toString());
		}

		@Test void b03_capability() throws Exception {
			assertInstanceOf(TokenWritable.class, Json5lSerializer.DEFAULT);
			var sb = new StringWriter();
			try (var w = Json5lSerializer.DEFAULT.writeTokens(sb)) {
				assertWriterStreaming(w);
			}
		}
	}

	// =================================================================================
	// C. Array-record stream — JSON5L aliases its line record stream (no surrounding [...])
	// =================================================================================

	@Nested class C_arrayRecords extends TestBase {

		@Test void c01_capability() {
			assertInstanceOf(ArrayRecordReadable.class, Json5lParser.DEFAULT);
			assertInstanceOf(ArrayRecordWritable.class, Json5lSerializer.DEFAULT);
			assertTrue(((ArrayRecordReadable) Json5lParser.DEFAULT).isArrayRecordStreaming());
			assertTrue(((ArrayRecordWritable) Json5lSerializer.DEFAULT).isArrayRecordStreaming());
		}

		@Test void c02_roundTrip() throws Exception {
			var sb = new StringWriter();
			try (RecordWriter w = Json5lSerializer.DEFAULT.writeArrayRecords(sb)) {
				assertTrue(w.isStreaming());
				w.write(java.util.Map.of("x", 1));
				w.write(java.util.Map.of("x", 2));
			}
			assertString("{\"x\":1}\n{\"x\":2}\n", sb);

			var records = new java.util.ArrayList<java.util.Map<?, ?>>();
			try (RecordReader r = Json5lParser.DEFAULT.readArrayRecords(sb.toString())) {
				assertTrue(r.isStreaming());
				while (r.canRead())
					records.add(r.read(java.util.Map.class));
			}
			assertEquals(2, records.size());
			assertBean(records.get(0), "x", "1");
			assertBean(records.get(1), "x", "2");
		}
	}
}
