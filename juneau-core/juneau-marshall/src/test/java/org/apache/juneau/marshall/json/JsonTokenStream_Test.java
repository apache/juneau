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
import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the public JSON token-streaming surface ({@link JsonTokenReader} +
 * {@link JsonTokenWriter}) introduced by the format-neutral {@code marshall.stream} package.
 */
@SuppressWarnings({
	"resource" // Token readers/writers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class JsonTokenStream_Test extends TestBase {

	@Nested class A_reader extends TestBase {

		@Test void a01_emptyObject() throws Exception {
			try (var r = fromJsonTokens("{}")) {
				assertSequence(r, TokenType.START_OBJECT, TokenType.END_OBJECT, TokenType.END_OF_STREAM);
			}
		}

		@Test void a02_emptyArray() throws Exception {
			try (var r = fromJsonTokens("[]")) {
				assertSequence(r, TokenType.START_ARRAY, TokenType.END_ARRAY, TokenType.END_OF_STREAM);
			}
		}

		@Test void a03_simpleObject() throws Exception {
			try (var r = fromJsonTokens("{\"a\":1,\"b\":\"hi\",\"c\":true,\"d\":null}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(0, r.getDepth() - 1); // sanity: now inside object at depth 1
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(1L, r.getNumber().longValue());
				assertEquals("1", r.getNumberLexeme());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("b", r.getFieldName());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hi", r.getString());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("c", r.getFieldName());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next());
				assertTrue(r.getBool());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("d", r.getFieldName());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void a04_nestedDocument() throws Exception {
			try (var r = fromJsonTokens("{\"a\":[1,[2,3],{\"b\":4}]}")) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME,    // a
					TokenType.START_ARRAY,
					TokenType.VALUE_NUMBER,  // 1
					TokenType.START_ARRAY,
					TokenType.VALUE_NUMBER,  // 2
					TokenType.VALUE_NUMBER,  // 3
					TokenType.END_ARRAY,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME,    // b
					TokenType.VALUE_NUMBER,  // 4
					TokenType.END_OBJECT,
					TokenType.END_ARRAY,
					TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a05_skipChildrenOnObject() throws Exception {
			try (var r = fromJsonTokens("[{\"a\":1,\"b\":[2,3]},42]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.START_OBJECT, r.next());
				r.skipChildren();
				// After skipChildren the cursor is positioned on the matching END_OBJECT.
				assertEquals(TokenType.END_OBJECT, r.getCurrentToken());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(42L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void a06_skipChildrenOnScalarIsNoop() throws Exception {
			try (var r = fromJsonTokens("[1,2]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				r.skipChildren();
				assertEquals(TokenType.VALUE_NUMBER, r.getCurrentToken());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(2L, r.getNumber().longValue());
			}
		}

		@Test void a07_numberLexemePreserved() throws Exception {
			try (var r = fromJsonTokens("[1, 1.5, -3, 1.0e10]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("1", r.getNumberLexeme());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("1.5", r.getNumberLexeme());
				assertEquals(1.5d, r.getNumber().doubleValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("-3", r.getNumberLexeme());
				assertEquals(-3L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("1.0e10", r.getNumberLexeme());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a08_stringEscapes() throws Exception {
			try (var r = fromJsonTokens("[\"a\\nb\",\"\\u00e9\",\"\\\"q\\\"\"]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("a\nb", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("é", r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("\"q\"", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a09_depthTracking() throws Exception {
			try (var r = fromJsonTokens("{\"a\":[1,{\"b\":2}]}")) {
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(1, r.getDepth());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(1, r.getDepth());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(2, r.getDepth());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(2, r.getDepth());
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(3, r.getDepth());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(2, r.getDepth());
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(1, r.getDepth());
				assertEquals(TokenType.END_OBJECT, r.next());
				assertEquals(0, r.getDepth());
			}
		}

		@Test void a10_capability() throws Exception {
			try (var r = fromJsonTokens("null")) {
				assertTrue(r.isStreaming());
			}
		}

		@Test void a11_initialTokenIsNotAvailable() throws Exception {
			try (var r = fromJsonTokens("[]")) {
				assertEquals(TokenType.NOT_AVAILABLE, r.getCurrentToken());
			}
		}

		@Test void a12_topLevelScalar() throws Exception {
			try (var r = fromJsonTokens("42")) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(42L, r.getNumber().longValue());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
			try (var r = fromJsonTokens("\"hi\"")) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("hi", r.getString());
			}
			try (var r = fromJsonTokens("true")) {
				assertEquals(TokenType.VALUE_BOOLEAN, r.next());
				assertTrue(r.getBool());
			}
			try (var r = fromJsonTokens("null")) {
				assertEquals(TokenType.VALUE_NULL, r.next());
			}
		}
	}

	@Nested class B_readerErrors extends TestBase {

		@Test void b01_truncatedObject() {
			assertThrows(ParseException.class, () -> drain("{\"a\":1"));
		}

		@Test void b02_unmatchedCloser() {
			assertThrows(ParseException.class, () -> drain("]"));
		}

		@Test void b03_missingColon() {
			assertThrows(ParseException.class, () -> drain("{\"a\" 1}"));
		}

		@Test void b04_missingComma() {
			assertThrows(ParseException.class, () -> drain("{\"a\":1 \"b\":2}"));
		}

		@Test void b05_invalidKeyword() {
			assertThrows(ParseException.class, () -> drain("trve"));
		}

		@Test void b06_invalidNumber() {
			assertThrows(ParseException.class, () -> drain("01"));
		}

		@Test void b07_unterminatedString() {
			assertThrows(ParseException.class, () -> drain("\"abc"));
		}

		@Test void b08_getFieldNameWrongState() throws Exception {
			try (var r = fromJsonTokens("[1]")) {
				r.next(); // START_ARRAY
				assertThrows(IllegalStateException.class, r::getFieldName);
			}
		}

		@Test void b09_getNumberWrongState() throws Exception {
			try (var r = fromJsonTokens("\"hi\"")) {
				r.next();
				assertThrows(IllegalStateException.class, r::getNumber);
				assertThrows(IllegalStateException.class, r::getNumberLexeme);
			}
		}

		@Test void b10_getBooleanWrongState() throws Exception {
			try (var r = fromJsonTokens("\"hi\"")) {
				r.next();
				assertThrows(IllegalStateException.class, r::getBool);
			}
		}

		@Test void b11_getBinaryAlwaysThrowsForJson() throws Exception {
			try (var r = fromJsonTokens("\"hi\"")) {
				r.next();
				assertThrows(IllegalStateException.class, r::getBinary);
			}
		}

		private void drain(String json) throws Exception {
			try (var r = fromJsonTokens(json)) {
				while (r.next() != TokenType.END_OF_STREAM) {
					// drain
				}
			}
		}
	}

	@Nested class C_writer extends TestBase {

		@Test void c01_emptyObject() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject().endObject();
			}
			assertEquals("{}", sb.toString());
		}

		@Test void c02_emptyArray() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startArray().endArray();
			}
			assertEquals("[]", sb.toString());
		}

		@Test void c03_simpleObject() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.fieldName("b"); w.string("hi");
				w.fieldName("c"); w.bool(true);
				w.fieldName("d"); w.nil();
				w.endObject();
			}
			assertEquals("{\"a\":1,\"b\":\"hi\",\"c\":true,\"d\":null}", sb.toString());
		}

		@Test void c04_nested() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject();
				w.fieldName("a");
				w.startArray();
				w.number(1);
				w.startArray();
				w.number(2);
				w.number(3);
				w.endArray();
				w.startObject();
				w.fieldName("b"); w.number(4);
				w.endObject();
				w.endArray();
				w.endObject();
			}
			assertEquals("{\"a\":[1,[2,3],{\"b\":4}]}", sb.toString());
		}

		@Test void c05_stringEscaping() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startArray();
				w.string("a\nb");
				w.string("\"q\"");
				w.string("ok");
				w.endArray();
			}
			assertEquals("[\"a\\nb\",\"\\\"q\\\"\",\"ok\"]", sb.toString());
		}

		@Test void c06_numberOverloads() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startArray();
				w.number(7L);
				w.number(2.5d);
				w.number(new BigDecimal("123.4567890123456789"));
				w.number(new BigInteger("12345678901234567890"));
				w.number((Number) null);
				w.endArray();
			}
			assertEquals("[7,2.5,123.4567890123456789,12345678901234567890,null]", sb.toString());
		}

		@Test void c07_stringNullEmitsNull() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startArray();
				w.string(null);
				w.endArray();
			}
			assertEquals("[null]", sb.toString());
		}

		@Test void c08_binaryAsBase64String() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startArray();
				w.binary(new byte[]{1, 2, 3, 4});
				w.endArray();
			}
			assertEquals("[\"" + Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4}) + "\"]", sb.toString());
		}

		@Test void c09_capability() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				assertTrue(w.isStreaming());
			}
		}

		@Test void c10_doubleFieldNameRejected() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject();
				w.fieldName("a");
				assertThrows(IllegalStateException.class, () -> w.fieldName("b"));
			}
		}

		@Test void c11_fieldNameOutsideObjectRejected() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				assertThrows(IllegalStateException.class, () -> w.fieldName("a"));
				w.startArray();
				assertThrows(IllegalStateException.class, () -> w.fieldName("a"));
			}
		}

		@Test void c12_writeEndKindMismatch() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject();
				assertThrows(IllegalStateException.class, w::endArray);
			}
		}

		@Test void c13_nonFiniteDoubleRejected() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startArray();
				assertThrows(IOException.class, () -> w.number(Double.NaN));
			}
		}

		@Test void c14_writerOverWriter() throws Exception {
			var sw = new StringWriter();
			try (var w = toJsonTokens(sw)) {
				w.startArray();
				w.number(1);
				w.endArray();
			}
			assertEquals("[1]", sw.toString());
		}

		@Test void c15_writerOverOutputStream() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = toJsonTokens(bos)) {
				w.startArray();
				w.bool(false);
				w.endArray();
			}
			assertEquals("[false]", bos.toString("UTF-8"));
		}

		@Test void c16_writeAfterCloseThrows() throws Exception {
			var sb = new StringWriter();
			var w = toJsonTokens(sb);
			w.startArray().endArray();
			w.close();
			// Every mutating method must reject writes after close() with the closed-writer message.
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::startObject);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::startArray);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::endObject);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::endArray);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.fieldName("a"));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.string("x"));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.number(1));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.bool(true));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::nil);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.binary(new byte[]{1}));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.object(42));
			// close() stays idempotent and flush() after close stays a safe no-op.
			assertDoesNotThrow(w::close);
			assertDoesNotThrow(w::flush);
			// The pre-close content was flushed (BCT string-shape assertion on the produced output).
			assertString("[]", sb);
		}
	}

	@Nested class D_roundTrip extends TestBase {

		@Test void d01_basicRoundTrip() throws Exception {
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject();
				w.fieldName("greeting"); w.string("hello");
				w.fieldName("count");    w.number(3);
				w.fieldName("flags");
				w.startArray();
				w.bool(true);
				w.bool(false);
				w.nil();
				w.endArray();
				w.endObject();
			}
			var produced = sb.toString();
			assertEquals("{\"greeting\":\"hello\",\"count\":3,\"flags\":[true,false,null]}", produced);

			try (var r = fromJsonTokens(produced)) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME, TokenType.VALUE_STRING,
					TokenType.FIELD_NAME, TokenType.VALUE_NUMBER,
					TokenType.FIELD_NAME, TokenType.START_ARRAY,
					TokenType.VALUE_BOOLEAN, TokenType.VALUE_BOOLEAN, TokenType.VALUE_NULL,
					TokenType.END_ARRAY,
					TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void d02_byteForByteAgainstDefaultSerializer() throws Exception {
			// Build the same value twice: once via the token writer, once via JsonSerializer.DEFAULT.
			// Outputs should be byte-for-byte equal because both produce compact RFC-8259 JSON with
			// double-quoted keys and no whitespace.
			var sb = new StringWriter();
			try (var w = toJsonTokens(sb)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.fieldName("b"); w.startArray();
				w.string("x");
				w.string("y");
				w.endArray();
				w.endObject();
			}
			var streamed = sb.toString();

			var pojo = new LinkedHashMap<String, Object>();
			pojo.put("a", 1);
			pojo.put("b", List.of("x", "y"));
			var databind = JsonSerializer.DEFAULT.writeToString(pojo);
			assertEquals(databind, streamed);
		}
	}

	@Nested class E_capabilities extends TestBase {

		@Test void e01_jsonParserDeclaresFull() throws Exception {
			assertInstanceOf(TokenReadable.class, JsonParser.DEFAULT);
			try (var r = JsonParser.DEFAULT.readTokens("null")) {
				assertReaderStreaming(r);
			}
		}

		@Test void e02_jsonSerializerDeclaresFull() throws Exception {
			assertInstanceOf(TokenWritable.class, JsonSerializer.DEFAULT);
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				assertWriterStreaming(w);
			}
		}
	}

	@Nested class F_settingsPropagation extends TestBase {

		@Test void f01_useWhitespacePrettyPrints() throws Exception {
			var ser = JsonSerializer.create().useWhitespace().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.fieldName("b");
				w.startArray();
				w.number(2);
				w.number(3);
				w.endArray();
				w.endObject();
			}
			// Pretty-printed: newlines + 2-space indent per level, ' ' after the colon.
			var expected = """
				{
				  "a": 1,
				  "b": [
				    2,
				    3
				  ]
				}""";
			assertEquals(expected, sb.toString());
		}

		@Test void f02_useWhitespaceEmptyContainersStayCompact() throws Exception {
			var ser = JsonSerializer.create().useWhitespace().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startObject();
				w.fieldName("empties");
				w.startArray();
				w.startObject(); w.endObject();
				w.startArray();  w.endArray();
				w.endArray();
				w.endObject();
			}
			var expected = """
				{
				  "empties": [
				    {},
				    []
				  ]
				}""";
			assertEquals(expected, sb.toString());
		}

		@Test void f03_quoteCharOverride() throws Exception {
			// Single-quote output (e.g. for embedding in HTML attributes).
			var ser = JsonSerializer.create().quoteChar('\'').build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startObject();
				w.fieldName("greeting"); w.string("o'reilly");
				w.endObject();
			}
			assertEquals("{'greeting':'o\\'reilly'}", sb.toString());
		}

		@Test void f04_escapeSolidus() throws Exception {
			var ser = JsonSerializer.create().escapeSolidus().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startArray();
				w.string("path/to/thing");
				w.endArray();
			}
			assertEquals("[\"path\\/to\\/thing\"]", sb.toString());
		}

		@Test void f05_trimStringsOnWriter() throws Exception {
			var ser = JsonSerializer.create().trimStrings().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startObject();
				w.fieldName("  padded  "); w.string("  value  ");
				w.endObject();
			}
			// Both the field name and the value get trimmed.
			assertEquals("{\"padded\":\"value\"}", sb.toString());
		}

		@Test void f06_defaultSerializerStaysCompact() throws Exception {
			// JsonSerializer.DEFAULT should still produce the canonical compact form.
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.endObject();
			}
			assertEquals("{\"a\":1}", sb.toString());
		}

		@Test void f07_trimStringsOnReader() throws Exception {
			var p = JsonParser.create().trimStrings().build();
			try (var r = p.readTokens("{\"k\":\"  v  \"}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("k", r.getFieldName());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("v", r.getString());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void f08_defaultReaderPreservesWhitespace() throws Exception {
			try (var r = JsonParser.DEFAULT.readTokens("[\"  v  \"]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("  v  ", r.getString());
			}
		}

		@Test void f09_streamCharsetHonored() throws Exception {
			// Latin-1 byte sequence containing a character that decodes differently under UTF-8.
			// 0xE9 is "é" in Latin-1; in UTF-8 it would be the start of a 2-byte sequence and
			// without a continuation byte would either fail or be replaced.
			var bytes = new byte[]{'"', (byte) 0xE9, '"'};
			var p = JsonParser.create().streamCharset(java.nio.charset.StandardCharsets.ISO_8859_1).build();
			try (var r = p.readTokens(bytes)) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("é", r.getString());
			}
		}

		@Test void f10_maxIndentClampsDeepLevels() throws Exception {
			// useWhitespace + maxIndent=2 means levels 0..2 indent (0/2/4 spaces), and levels >= 2
			// emit no further per-level indent.  The structural newlines themselves still appear.
			var ser = JsonSerializer.create().useWhitespace().maxIndent(2).build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startObject();
				w.fieldName("a");
				w.startArray();
				w.startArray();
				w.number(1);
				w.endArray();
				w.endArray();
				w.endObject();
			}
			// depth 1 at "a" => 2 spaces; depth 2 at outer array members => 4 spaces;
			// depth 3 inner array members and the inner ']' clamp to 4 spaces.
			var expected = """
				{
				  "a": [
				    [
				    1
				    ]
				  ]
				}""";
			assertEquals(expected, sb.toString());
		}

		@Test void f11_autoCloseStreamsOnReader() throws Exception {
			var p = JsonParser.create().autoCloseStreams().build();
			var underlying = new StringReader("[1,2]");
			try (var r = p.readTokens(underlying)) {
				while (r.next() != TokenType.END_OF_STREAM) {
					// drain
				}
			}
			// With autoCloseStreams the underlying reader should be closed.  StringReader.read()
			// throws IOException("Stream closed") after close, which is the cleanest probe.
			assertThrows(IOException.class, underlying::read);
		}

		@Test void f12_unbufferedOnReader() throws Exception {
			// unbuffered() doesn't change observable token output; assert it parses correctly so
			// the flag at least flows through without breaking the cursor.
			var p = JsonParser.create().unbuffered().build();
			try (var r = p.readTokens("[1,2,3]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(3L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void f13_streamCharsetHonored() throws Exception {
			var bytes = new byte[]{'"', (byte) 0xE9, '"'};  // ISO_8859_1 'é'
			var p = JsonParser.create().streamCharset(java.nio.charset.StandardCharsets.ISO_8859_1).build();
			try (var r = p.readTokens(new ByteArrayInputStream(bytes))) {
				assertEquals(TokenType.VALUE_STRING, r.next());
				assertEquals("é", r.getString());
			}
		}

		@Test void f14_ignoredSettingsAreSilentlySkipped() throws Exception {
			// Spot-check: settings on the always-ignored list must not crash and must not influence
			// the token output.  We pick one writer-side flag (addBeanTypesJson + sortMaps) and one
			// reader-side flag (debugOutputLines + listener=null).
			var ser = JsonSerializer.create().addBeanTypesJson().sortMaps().keepNullProperties().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.startObject();
				w.fieldName("z"); w.number(1);
				w.fieldName("a"); w.number(2);
				w.endObject();
			}
			// addBeanTypesJson does not inject _type; sortMaps does not reorder z/a; keepNullProperties is moot.
			assertEquals("{\"z\":1,\"a\":2}", sb.toString());

			var p = JsonParser.create().debugOutputLines(99).build();
			try (var r = p.readTokens("[true]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next());
				assertTrue(r.getBool());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}
	}

	@Nested class G_read extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void g01_readScalar() throws Exception {
			try (var r = JsonParser.DEFAULT.readTokens("42")) {
				assertEquals(0, r.getDepth());
				Integer v = r.read(Integer.class);
				assertEquals(42, v);
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void g02_readObject() throws Exception {
			try (var r = JsonParser.DEFAULT.readTokens("{\"name\":\"alice\",\"age\":30}")) {
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void g03_readObjectViaJsonMap() throws Exception {
			try (var r = JsonParser.DEFAULT.readTokens("{\"a\":1,\"b\":2}")) {
				var m = r.read(Map.class);
				assertEquals(1L, ((Number) m.get("a")).longValue());
				assertEquals(2L, ((Number) m.get("b")).longValue());
			}
		}

		@Test void g04_streamArrayOfBeans() throws Exception {
			// Idiomatic streaming-records pattern: the canonical loop uses canRead() as the predicate.
			var input = "[{\"name\":\"alice\",\"age\":30},{\"name\":\"bob\",\"age\":40}]";
			var seen = new ArrayList<Bean>();
			try (var r = JsonParser.DEFAULT.readTokens(input)) {
				assertEquals(TokenType.START_ARRAY, r.next());
				while (r.canRead())
					seen.add(r.read(Bean.class));
				assertEquals(TokenType.END_ARRAY, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
			assertEquals(2, seen.size());
			assertEquals("alice", seen.get(0).name);
			assertEquals(30, seen.get(0).age);
			assertEquals("bob", seen.get(1).name);
			assertEquals(40, seen.get(1).age);
		}

		@Test void g04b_emptyArrayCanReadValueIsFalse() throws Exception {
			try (var r = JsonParser.DEFAULT.readTokens("[]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertFalse(r.canRead());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void g05_readAfterFieldName() throws Exception {
			// Walk to a particular field, then bind its value to a POJO.
			try (var r = JsonParser.DEFAULT.readTokens("{\"meta\":{\"v\":1},\"payload\":{\"name\":\"alice\",\"age\":30}}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());  // "meta"
				assertEquals("meta", r.getFieldName());
				r.read(Object.class);  // skip via bind
				assertEquals(TokenType.FIELD_NAME, r.next());  // "payload"
				assertEquals("payload", r.getFieldName());
				var b = r.read(Bean.class);
				assertEquals("alice", b.name);
				assertEquals(30, b.age);
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void g06_readAfterValueAutoAdvancesPastComma() throws Exception {
			// read() auto-advances through structural separators (commas inside arrays,
			// colons after field names) so the natural array-streaming loop works without manual
			// next() calls between elements.
			try (var r = JsonParser.DEFAULT.readTokens("[1,2]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(1L, r.getNumber().longValue());
				// Cursor is positioned right after the first value; read auto-advances past
				// the comma and consumes the next element.
				assertEquals(2, r.read(Integer.class));
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void g06b_readAtEndOfArrayThrows() throws Exception {
			try (var r = JsonParser.DEFAULT.readTokens("[1]")) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(1, r.read(Integer.class));
				// Cursor is now positioned at the end of the array (no more values).
				assertFalse(r.canRead());
				assertThrows(IllegalStateException.class, () -> r.read(Integer.class));
			}
		}

		@Test void g07_readAfterStartObjectThrows() throws Exception {
			// Documents the "instead-of-next, not after-next" contract.
			try (var r = JsonParser.DEFAULT.readTokens("{\"a\":1}")) {
				assertEquals(TokenType.START_OBJECT, r.next());
				// We've already consumed the {; read cannot rebind.
				assertThrows(IllegalStateException.class, () -> r.read(Bean.class));
			}
		}

		@Test void g08_readWithoutSessionThrows() throws Exception {
			// Direct construction (without a JsonParserSession) leaves read disabled.
			try (var r = new JsonTokenReader(new ParserPipe("{\"a\":1}"))) {
				assertThrows(UnsupportedOperationException.class, () -> r.read(Bean.class));
			}
		}
	}

	@Nested class H_object extends TestBase {

		public static class HBean {
			public String name;
			public int age;
			public List<String> tags;
			public Map<String, Integer> scores;
		}

		@Test void h01_simpleScalar() throws Exception {
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(42);
			}
			assertEquals("42", sb.toString());
		}

		@Test void h02_nullEmitsNil() throws Exception {
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(null);
			}
			assertEquals("null", sb.toString());
		}

		@Test void h03_simpleBean() throws Exception {
			var b = new HBean();
			b.name = "alice";
			b.age = 30;
			b.tags = List.of("x", "y");
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(b);
			}
			// Compare against the canonical serializer's output for the same bean.
			assertEquals(JsonSerializer.DEFAULT.writeToString(b), sb.toString());
		}

		@Test void h04_listOfBeans() throws Exception {
			var b1 = new HBean(); b1.name = "a"; b1.age = 1;
			var b2 = new HBean(); b2.name = "b"; b2.age = 2;
			var beans = List.of(b1, b2);
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(beans);
			}
			assertEquals(JsonSerializer.DEFAULT.writeToString(beans), sb.toString());
		}

		@Test void h05_compositeWithMapsAndArrays() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", List.of("x", "y"));
			m.put("c", new int[]{10, 20});
			m.put("d", Map.of("nested", true));
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals(JsonSerializer.DEFAULT.writeToString(m), sb.toString());
		}

		@Test void h06_byteArrayEmitsBinary() throws Exception {
			var bytes = new byte[]{1, 2, 3, 4};
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(bytes);
			}
			// JsonTokenWriter's binary() base64-encodes (already covered by C_writer); object()
			// should route a byte[] through binary(), not through walk-as-array.
			assertEquals("\"" + Base64.getEncoder().encodeToString(bytes) + "\"", sb.toString());
		}

		@Test void h07_objectInsideTokenStream() throws Exception {
			// Mix raw token emits with object(): emit a wrapping envelope structurally, then
			// object() the payload.
			var payload = new HBean();
			payload.name = "alice";
			payload.age = 30;
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.startObject();
				w.fieldName("envelopeVersion"); w.number(1);
				w.fieldName("payload"); w.object(payload);
				w.endObject();
			}
			var expectedPayload = JsonSerializer.DEFAULT.writeToString(payload);
			assertEquals("{\"envelopeVersion\":1,\"payload\":" + expectedPayload + "}", sb.toString());
		}

		@Test void h08_sortMapsHonored() throws Exception {
			var m = new LinkedHashMap<String, Integer>();
			m.put("z", 1);
			m.put("a", 2);
			m.put("m", 3);
			// With sortMaps, keys come out in alphabetical order.
			var ser = JsonSerializer.create().sortMaps().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals("{\"a\":2,\"m\":3,\"z\":1}", sb.toString());
		}

		@Test void h09_sortMapsDefaultOff() throws Exception {
			// Default: insertion order preserved.
			var m = new LinkedHashMap<String, Integer>();
			m.put("z", 1);
			m.put("a", 2);
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals("{\"z\":1,\"a\":2}", sb.toString());
		}

		@Test void h10_keepNullPropertiesOff() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", null);
			m.put("c", 3);
			// Default keepNullProperties=true would produce {"a":1,"b":null,"c":3}.
			// But the actual default on JsonSerializer.DEFAULT is to skip nulls during a databind
			// emit; verify that the walker honors the configured setting.
			var skipNullsSer = JsonSerializer.create().keepNullProperties(false).build();
			var sb = new StringWriter();
			try (var w = skipNullsSer.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals("{\"a\":1,\"c\":3}", sb.toString());
		}

		@Test void h11_keepNullPropertiesOn() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", null);
			var ser = JsonSerializer.create().keepNullProperties().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals("{\"a\":1,\"b\":null}", sb.toString());
		}

		@Test void h12_trimEmptyMapsHonored() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", Map.of());
			var ser = JsonSerializer.create().trimEmptyMaps().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals("{\"a\":1}", sb.toString());
		}

		@Test void h13_trimEmptyCollectionsHonored() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", List.of());
			var ser = JsonSerializer.create().trimEmptyCollections().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.object(m);
			}
			assertEquals("{\"a\":1}", sb.toString());
		}

		@Test void h14_sortCollectionsHonored() throws Exception {
			var l = List.of("c", "a", "b");
			var ser = JsonSerializer.create().sortCollections().build();
			var sb = new StringWriter();
			try (var w = ser.writeTokens(sb)) {
				w.object(l);
			}
			assertEquals("[\"a\",\"b\",\"c\"]", sb.toString());
		}

		@Test void h15_localDateGoesThroughSwap() throws Exception {
			// java.time.LocalDate has a swap registered in DefaultSwaps; object() should emit
			// it as the swapped string form (ISO-8601), not as a bean walk over its public methods.
			var date = java.time.LocalDate.of(2026, java.time.Month.JUNE, 12);
			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(date);
			}
			// Match what the databind path produces.
			assertEquals(JsonSerializer.DEFAULT.writeToString(date), sb.toString());
		}

		@Test void h16_objectMatchesSerializerByteForByte() throws Exception {
			// Round-trip equivalence: for any non-trivial value, object() output must equal
			// JsonSerializer.DEFAULT.writeToString() output.
			var b = new HBean();
			b.name = "alice";
			b.age = 30;
			b.tags = List.of("a", "b");
			b.scores = new LinkedHashMap<>();
			b.scores.put("math", 95);
			b.scores.put("english", 87);

			var sb = new StringWriter();
			try (var w = JsonSerializer.DEFAULT.writeTokens(sb)) {
				w.object(b);
			}
			assertEquals(JsonSerializer.DEFAULT.writeToString(b), sb.toString());
		}
	}

	// Helpers keep the marshaller reference fully-qualified (the simple name Json is shadowed by the @Json annotation in this package).
	// Note: the token direction is inverted relative to of/to — the reader (toTokens) is the parse/"from" side and the writer (ofTokens) is the serialize/"to" side.

	private static TokenReader fromJsonTokens(Object input) throws IOException {
		return org.apache.juneau.marshall.marshaller.Json.toTokens(input);
	}

	private static TokenWriter toJsonTokens(Object output) throws IOException {
		return org.apache.juneau.marshall.marshaller.Json.ofTokens(output);
	}

}
