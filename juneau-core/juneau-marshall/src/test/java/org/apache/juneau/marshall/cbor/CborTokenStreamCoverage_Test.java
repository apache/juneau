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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * White-box branch coverage for {@link CborTokenReader} / {@link CborTokenWriter} structural
 * surface not reached by the higher-level {@link CborTokenStream_Test} round-trips.
 */
@SuppressWarnings({
	"resource" // Token readers/writers are closed via try-with-resources; JDT false-positive leak reports over chained factory calls.
})
class CborTokenStreamCoverage_Test extends TestBase {

	// =================================================================================
	// R. Reader edge branches
	// =================================================================================

	@Nested class R_reader extends TestBase {

		@Test void r01_floatRoundTripsAsNumber() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.number(3.5d);
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(3.5d, r.getNumber().doubleValue());
				assertEquals("3.5", r.getNumberLexeme());
			}
		}

		@Test void r02_tagUnwrapsToWrappedValue() throws Exception {
			// 0xC1 = tag 1 (epoch time), wrapping uint 1.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xC1, 0x01})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(1L, r.getNumber().longValue());
			}
		}

		@Test void r03_simpleAndUndefinedAreNull() throws Exception {
			// 0xF8 0xFF = simple value 255; 0xF7 = undefined.  Both normalize to VALUE_NULL.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xF8, (byte) 0xFF})) {
				assertEquals(TokenType.VALUE_NULL, r.next());
			}
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xF7})) {
				assertEquals(TokenType.VALUE_NULL, r.next());
			}
		}

		@Test void r04_nonStringMapKeysCoerced() throws Exception {
			// {1:2} integer key.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, 0x01, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("1", r.getFieldName());
			}
			// {-1:2} negative-integer key.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, 0x20, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("-1", r.getFieldName());
			}
			// {true:2} boolean key.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, (byte) 0xF5, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("true", r.getFieldName());
			}
			// {1.0:2} single-precision float key (0xFA + 4 bytes = 1.0f).
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, (byte) 0xFA, 0x3F, (byte) 0x80, 0x00, 0x00, 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("1.0", r.getFieldName());
			}
			// {h'78':2} binary key decodes as UTF-8 "x".
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, 0x41, 'x', 0x02})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("x", r.getFieldName());
			}
		}

		@Test void r05_invalidMapKeyTypeThrows() throws Exception {
			// {[]:...} — array as a map key is rejected.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, (byte) 0x80})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertThrows(ParseException.class, r::next);
			}
		}

		@Test void r06_breakAtRootThrows() throws Exception {
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xFF})) {
				assertThrows(ParseException.class, r::next);
			}
		}

		@Test void r07_breakInsideDefiniteContainerThrows() throws Exception {
			// 0x81 = definite array length 1, but a BREAK appears where the element should be.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x81, (byte) 0xFF})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertThrows(ParseException.class, r::next);
			}
		}

		@Test void r08_endOfStreamRepeatsAndCanReadFalse() throws Exception {
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{0x01})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());
				assertEquals(TokenType.END_OF_STREAM, r.next());  // ended branch repeats
				assertFalse(r.canRead());
			}
		}

		@Test void r09_truncatedDefiniteContainerRethrowsIo() throws Exception {
			// 0x82 = array length 2, only one element present.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x82, 0x01})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertThrows(IOException.class, r::next);
			}
		}

		@Test void r10_getStringViews() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startArray().number(5).bool(true).nil().string("hi").endArray();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next());  assertEquals("5", r.getString());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertEquals("true", r.getString());
				assertEquals(TokenType.VALUE_NULL, r.next());    assertNull(r.getString());
				assertEquals(TokenType.VALUE_STRING, r.next());  assertEquals("hi", r.getString());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void r11_accessorsRejectWrongToken() throws Exception {
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x80})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertThrows(IllegalStateException.class, r::getFieldName);
				assertThrows(IllegalStateException.class, r::getNumber);
				assertThrows(IllegalStateException.class, r::getNumberLexeme);
				assertThrows(IllegalStateException.class, r::getBool);
				assertThrows(IllegalStateException.class, r::getBinary);
				assertThrows(IllegalStateException.class, r::getString);
			}
		}

		@Test void r12_currentTokenAndDepth() throws Exception {
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x81, (byte) 0x80})) {
				assertEquals(TokenType.NOT_AVAILABLE, r.getCurrentToken());
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.START_ARRAY, r.getCurrentToken());
				assertEquals(1, r.getDepth());
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(2, r.getDepth());
			}
		}

		@Test void r13_skipChildren() throws Exception {
			// no-op when positioned at a scalar.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{0x01})) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertDoesNotThrow(r::skipChildren);
			}
			// skips an entire object subtree.
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("a").number(1);
				w.fieldName("b").startArray().number(1).number(2).endArray();
				w.endObject();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_OBJECT, r.next());
				r.skipChildren();
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void r14_readNonValueStateThrows() throws Exception {
			// [1] — after reading the single element the cursor is at an exhausted container.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x81, 0x01})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(Integer.valueOf(1), r.read(Integer.class));
				assertThrows(IllegalStateException.class, () -> r.read(Integer.class));
			}
		}

		@Test void r15_readTypeOverload() throws Exception {
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{0x01})) {
				Integer v = r.read((Type) Integer.class);
				assertEquals(Integer.valueOf(1), v);
			}
		}

		@Test void r16_sessionlessReadThrows() throws Exception {
			try (var r = new CborTokenReader(new ParserPipe(new byte[]{0x01}))) {
				assertThrows(UnsupportedOperationException.class, () -> r.read(Integer.class));
			}
		}

		@Test void r17_getStringOnFieldName() throws Exception {
			// {"a":1} — getString() on a FIELD_NAME token returns the field name view.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA1, 0x61, 'a', 0x01})) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next());
				assertEquals("a", r.getString());
			}
		}

		@Test void r18_skipChildrenOverArray() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startArray().number(1).startArray().number(2).endArray().endArray();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				r.skipChildren();
				assertEquals(0, r.getDepth());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void r19_readParseExceptionWrapped() throws Exception {
			// A string scalar bound to an incompatible numeric type funnels through the read() catch
			// block and surfaces as a ParseException.
			var bytes = CborSerializer.DEFAULT.serialize("not-a-number");
			try (var r = CborParser.DEFAULT.parseTokens((byte[]) bytes)) {
				assertThrows(ParseException.class, () -> r.read(int.class));
			}
		}

		@Test void r20_readIoExceptionRethrown() throws Exception {
			// 0x82 = array length 2, only one element present.  Binding the whole truncated array
			// drives parseAnything to EOF, which the read() funnel rethrows as IOException.
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x82, 0x01})) {
				assertThrows(IOException.class, () -> r.read(java.util.List.class));
			}
		}

		@Test void r22_skipChildrenOverTruncatedIndefiniteContainerRethrowsIo() throws Exception {
			// 0x9F = indefinite-length array, one element, no BREAK, then EOF.  skipChildren() drives
			// next() inside the open container; the underlying stream raises IOException at EOF before
			// END_OF_STREAM can ever surface mid-container (so the skipChildren EOF guard is defensive).
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x9F, 0x01})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertThrows(IOException.class, r::skipChildren);
			}
		}

		@Test void r21_readBeanFromScalarSurfacesParseException() throws Exception {
			// A bare integer scalar bound to a bean type drives parseAnything to throw a ParseException
			// that the read() funnel rethrows as-is (the 'instanceof ParseException' branch).
			var bytes = CborSerializer.DEFAULT.serialize(1);
			try (var r = CborParser.DEFAULT.parseTokens((byte[]) bytes)) {
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
			try (var w = new CborTokenWriter(bos)) {
				w.startArray().number(1).endArray();
			}
			assertTrue(bos.toByteArray().length > 0);

			var bos2 = new ByteArrayOutputStream();
			try (var w = new CborTokenWriter(bos2, CborTokenWriter.Settings.DEFAULT)) {
				w.startArray().number(1).endArray();
			}
			assertArrayEquals(bos.toByteArray(), bos2.toByteArray());
		}

		@Test void w02_forOutput() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborTokenWriter.forOutput(bos, CborTokenWriter.Settings.DEFAULT)) {
				w.number(1);
			}
			assertTrue(bos.toByteArray().length > 0);

			var f = File.createTempFile("cbortok", ".cbor");
			f.deleteOnExit();
			try (var w = CborTokenWriter.forOutput(f, CborTokenWriter.Settings.DEFAULT)) {
				w.startArray().number(1).number(2).endArray();
			}
			assertTrue(f.length() > 0);

			assertThrowsWithMessage(IOException.class, "Output cannot be null.",
				() -> CborTokenWriter.forOutput(null, CborTokenWriter.Settings.DEFAULT));
			assertThrowsWithMessage(IOException.class, "Cannot convert object of type",
				() -> CborTokenWriter.forOutput("not-a-stream", CborTokenWriter.Settings.DEFAULT));
		}

		@Test void w03_numberOverloadsRoundTrip() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startArray();
				w.number((Number) Integer.valueOf(5));
				w.number(2.5d);
				w.number(new BigDecimal("3.5"));
				w.number(new BigInteger("7"));
				w.endArray();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
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
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startArray();
				w.number((Number) null);
				w.number((BigDecimal) null);
				w.number((BigInteger) null);
				w.string(null);
				w.binary(null);
				w.endArray();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_ARRAY, r.next());
				for (var i = 0; i < 5; i++)
					assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void w05_fieldNameOutsideObjectThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				assertThrows(IllegalStateException.class, () -> w.fieldName("a"));  // depth 0
				w.startArray();
				assertThrows(IllegalStateException.class, () -> w.fieldName("a"));  // inside array
				w.endArray();
			}
		}

		@Test void w06_valueAtKeyPositionThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				assertThrows(IllegalStateException.class, () -> w.number(1));
			}
		}

		@Test void w07_deepNestingGrowsStack() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				for (var i = 0; i < 20; i++)
					w.startArray();
				for (var i = 0; i < 20; i++)
					w.endArray();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				for (var i = 0; i < 20; i++)
					assertEquals(TokenType.START_ARRAY, r.next());
			}
		}

		@Test void w08_endContainerWithNoStartThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			var w = new CborTokenWriter(bos);
			assertThrows(IllegalStateException.class, w::endObject);
		}

		@Test void w09_endContainerKindMismatchBothDirections() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				assertThrows(IllegalStateException.class, w::endArray);   // object open, array close
				w.endObject();
				w.startArray();
				assertThrows(IllegalStateException.class, w::endObject);  // array open, object close
				w.endArray();
			}
		}

		@Test void w10_isStreaming() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				assertTrue(w.isStreaming());
			}
		}
	}
}
