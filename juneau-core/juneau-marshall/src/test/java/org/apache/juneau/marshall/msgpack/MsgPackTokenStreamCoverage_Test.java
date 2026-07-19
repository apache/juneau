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
package org.apache.juneau.marshall.msgpack;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * White-box branch coverage for {@link MsgPackTokenReader} / {@link MsgPackTokenWriter} structural
 * surface not reached by the higher-level {@link MsgPackTokenStream_Test} round-trips.
 */
@SuppressWarnings({
	"resource" // Token readers/writers are closed via try-with-resources; JDT false-positive leak reports over chained factory calls.
})
class MsgPackTokenStreamCoverage_Test extends TestBase {

	// =================================================================================
	// R. Reader edge branches
	// =================================================================================

	@Nested class R_reader extends TestBase {

		@Test void r01_longFloatDoubleNumbers() throws Exception {
			// 0xCE uint32 = 65536 (LONG branch).
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0xCE, 0x00, 0x01, 0x00, 0x00})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(65536L, r.getNumber().longValue());
			}
			// 0xCA float32 = 1.0f (FLOAT branch).
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0xCA, 0x3F, (byte) 0x80, 0x00, 0x00})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(1.0d, r.getNumber().doubleValue());
			}
			// 0xCB float64 = 2.5 (DOUBLE branch).
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0xCB, 0x40, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(2.5d, r.getNumber().doubleValue());
			}
		}

		@Test void r02_extEmitsBinary() throws Exception {
			// 0xD4 = fixext1, type byte 0x01, one data byte.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0xD4, 0x01, 0x7F})) {
				assertEquals(TokenType.VALUE_BINARY, r.next());
				assertNotNull(r.getBinary());
			}
		}

		@Test void r03_nonStringMapKeysCoerced() throws Exception {
			// {1:2} int key.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, 0x01, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("1", r.getFieldName());
			}
			// {65536:2} long key.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0xCE, 0x00, 0x01, 0x00, 0x00, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("65536", r.getFieldName());
			}
			// {true:2} boolean key.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0xC3, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("true", r.getFieldName());
			}
			// {1.0f:2} float key.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0xCA, 0x3F, (byte) 0x80, 0x00, 0x00, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("1.0", r.getFieldName());
			}
			// {2.5:2} double key.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0xCB, 0x40, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("2.5", r.getFieldName());
			}
			// {bin "x":2} binary key decodes as UTF-8.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0xC4, 0x01, 'x', 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("x", r.getFieldName());
			}
		}

		@Test void r04_invalidMapKeyTypeThrows() throws Exception {
			// {[]:...} — array as a map key is rejected.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0x90, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertThrows(ParseException.class, r::next);
			}
		}

		@Test void r05_endOfStreamRepeatsAndCanReadFalse() throws Exception {
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{0x01})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
				assertFalse(r.canRead());
			}
		}

		@Test void r06_truncatedContainerRethrowsIo() throws Exception {
			// 0x92 = fixarray[2], only one element present.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x92, 0x01})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrows(IOException.class, r::next);
			}
		}

		@Test void r07_getStringViews() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				w.startArray().number(5).bool(true).nil().string("hi").endArray();
			}
			try (var r = MsgPackParser.DEFAULT.readTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  assertEquals("5", r.getString());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertEquals("true", r.getString());
				assertEquals(TokenType.VALUE_NULL, r.next());    assertNull(r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next());  assertEquals("hi", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void r08_accessorsRejectWrongToken() throws Exception {
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x90})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertThrows(IllegalStateException.class, r::getFieldName);
				assertThrows(IllegalStateException.class, r::getNumber);
				assertThrows(IllegalStateException.class, r::getNumberLexeme);
				assertThrows(IllegalStateException.class, r::getBool);
				assertThrows(IllegalStateException.class, r::getBinary);
				assertThrows(IllegalStateException.class, r::getString);
			}
		}

		@Test void r09_currentTokenAndDepth() throws Exception {
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x91, (byte) 0x90})) {
				assertEquals(TokenType.NOT_AVAILABLE, r.getCurrentToken());
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.START_ARRAY, r.getCurrentToken());
				assertEquals(1, r.getDepth());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(2, r.getDepth());
			}
		}

		@Test void r10_skipChildren() throws Exception {
			// no-op at a scalar.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{0x01})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertDoesNotThrow(r::skipChildren);
			}
			// skips a whole object subtree.
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				w.startObject();
				w.fieldName("a").number(1);
				w.fieldName("b").startArray().number(1).number(2).endArray();
				w.endObject();
			}
			try (var r = MsgPackParser.DEFAULT.readTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_OBJECT, r.next());
				r.skipChildren();
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void r11_readNonValueStateThrows() throws Exception {
			// 0x91 = fixarray[1]; after reading its single element the cursor is exhausted.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x91, 0x01})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(Integer.valueOf(1), r.read(Integer.class));
				assertThrows(IllegalStateException.class, () -> r.read(Integer.class));
			}
		}

		@Test void r12_readTypeOverload() throws Exception {
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{0x01})) {
				Integer v = r.read((Type) Integer.class);
				assertEquals(Integer.valueOf(1), v);
			}
		}

		@Test void r13_sessionlessReadThrows() throws Exception {
			try (var r = new MsgPackTokenReader(new ParserPipe(new byte[]{0x01}))) {
				assertThrows(UnsupportedOperationException.class, () -> r.read(Integer.class));
			}
		}

		@Test void r14_deepNestingGrowsReaderStack() throws Exception {
			// 17 nested fixarray[1] containers force the reader's parallel stacks (capacity 16) to grow.
			var bytes = new byte[18];
			for (var i = 0; i < 17; i++)
				bytes[i] = (byte) 0x91;  // fixarray length 1
			bytes[17] = 0x01;           // innermost scalar
			try (var r = MsgPackParser.DEFAULT.readTokens(bytes)) {
				for (var i = 0; i < 17; i++)
					assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(17, r.getDepth());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
			}
		}

		@Test void r15_getStringOnFieldNameAndScalarAccessors() throws Exception {
			// {"a":1} — getString() on FIELD_NAME returns the field name.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x81, (byte) 0xA1, 'a', 0x01})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("a", r.getString());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals("1", r.getNumberLexeme());
			}
			// getBool() on an actual boolean value.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0xC3})) {
				assertEquals(TokenType.VALUE_BOOLEAN, r.next());
				assertTrue(r.getBool());
			}
		}

		@Test void r16_skipChildrenOverArrayAndStreamingFlag() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				w.startArray().number(1).startArray().number(2).endArray().endArray();
			}
			try (var r = MsgPackParser.DEFAULT.readTokens(bos.toByteArray())) {
				assertTrue(r.isStreaming());
				assertEquals(TokenType.START_ARRAY, r.next());
				r.skipChildren();
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void r17_readParseExceptionWrapped() throws Exception {
			var bytes = MsgPackSerializer.DEFAULT.write("not-a-number");
			try (var r = MsgPackParser.DEFAULT.readTokens((byte[]) bytes)) {
				assertThrows(ParseException.class, () -> r.read(int.class));
			}
		}

		@Test void r18_readIoExceptionRethrown() throws Exception {
			// 0x92 = fixarray[2], only one element present; binding the truncated array hits EOF.
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x92, 0x01})) {
				assertThrows(IOException.class, () -> r.read(java.util.List.class));
			}
		}

		@Test void r19_eofReadingElementInsideContainerRethrowsIo() throws Exception {
			// 0x91 = fixarray[1] header with no element bytes.  The next() call inside the container
			// hits EOF while reading the element's data-type tag, which is rethrown (depth>0).
			try (var r = MsgPackParser.DEFAULT.readTokens(new byte[]{(byte) 0x91})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertThrows(IOException.class, r::next);
			}
		}

		@Test void r20_readBeanFromScalarSurfacesParseException() throws Exception {
			// A bare integer scalar bound to a bean type drives parseAnything to throw a ParseException
			// that the read() funnel rethrows as-is (the 'instanceof ParseException' branch).
			var bytes = MsgPackSerializer.DEFAULT.write(1);
			try (var r = MsgPackParser.DEFAULT.readTokens((byte[]) bytes)) {
				assertThrows(ParseException.class, () -> r.read(ABean.class));
			}
		}
	}

	public static class ABean {
		public int a;
	}

	// =================================================================================
	// W. Writer edge branches
	// =================================================================================

	@Nested class W_writer extends TestBase {

		@Test void w01_publicConstructors() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = new MsgPackTokenWriter(bos)) {
				w.startArray().number(1).endArray();
			}
			var bos2 = new ByteArrayOutputStream();
			try (var w = new MsgPackTokenWriter(bos2, MsgPackTokenWriter.Settings.DEFAULT)) {
				w.startArray().number(1).endArray();
			}
			assertArrayEquals(bos.toByteArray(), bos2.toByteArray());
		}

		@Test void w02_constructorAndSessionTypeRejection() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = new MsgPackTokenWriter(bos, MsgPackTokenWriter.Settings.DEFAULT)) {
				w.number(1);
			}
			assertTrue(bos.toByteArray().length > 0);

			// The Object-to-OutputStream narrowing (and null/illegal-type rejection) lives on the
			// session (the Object interface boundary); the writer takes a statically-typed OutputStream.
			assertThrowsWithMessage(IOException.class, "Output cannot be null.",
				() -> MsgPackSerializer.DEFAULT.writeTokens(null));
			assertThrowsWithMessage(IOException.class, "Cannot convert object of type",
				() -> MsgPackSerializer.DEFAULT.writeTokens("not-a-stream"));
		}

		@Test void w03_numberOverloadsRoundTrip() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				w.startArray();
				w.number((Number) Integer.valueOf(5));
				w.number(2.5d);
				w.number(new BigDecimal("3.5"));
				w.number(new BigInteger("7"));
				w.endArray();
			}
			try (var r = MsgPackParser.DEFAULT.readTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(5L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2.5d, r.getNumber().doubleValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(3.5d, r.getNumber().doubleValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(7L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void w04_nullValueOverloadsEmitNull() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				w.startArray();
				w.number((Number) null);
				w.number((BigDecimal) null);
				w.number((BigInteger) null);
				w.string(null);
				w.binary(null);
				w.endArray();
			}
			try (var r = MsgPackParser.DEFAULT.readTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				for (var i = 0; i < 5; i++)
					assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void w05_fieldNameOutsideObjectThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				assertThrows(IllegalStateException.class, () -> w.fieldName("a"));  // depth 0
				w.startArray();
				assertThrows(IllegalStateException.class, () -> w.fieldName("a"));  // inside array
				w.endArray();
			}
		}

		@Test void w06_valueAtKeyPositionThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				w.startObject();
				assertThrows(IllegalStateException.class, () -> w.number(1));
			}
		}

		@Test void w07_endContainerMismatchOrNoStartThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			var w = new MsgPackTokenWriter(bos);
			assertThrows(IllegalStateException.class, w::endObject);  // no start
			assertThrows(IllegalStateException.class, w::endArray);   // no start
			w.startObject();
			assertThrows(IllegalStateException.class, w::endArray);   // kind mismatch
			w.endObject();
			w.startArray();
			assertThrows(IllegalStateException.class, w::endObject);  // kind mismatch
			w.endArray();
		}

		@Test void w08_fieldNameTwiceThrowsAndStreamingFlag() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.writeTokens(bos)) {
				assertTrue(w.isStreaming());
				w.startObject();
				w.fieldName("a");
				assertThrows(IllegalStateException.class, () -> w.fieldName("b"));  // field called twice
			}
		}
	}
}
