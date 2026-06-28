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
import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the public CBOR token-streaming surface.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class CborTokenStream_Test extends TestBase {

	@Nested class A_reader extends TestBase {

		@Test void a01_emptyMap() throws Exception {
			// 0xA0 = definite-length map, length=0
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xA0})) {
				assertSequence(r, TokenType.START_OBJECT, TokenType.END_OBJECT, TokenType.END_OF_STREAM);
			}
		}

		@Test void a02_emptyArray() throws Exception {
			// 0x80 = definite-length array, length=0
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x80})) {
				assertSequence(r, TokenType.START_ARRAY, TokenType.END_ARRAY, TokenType.END_OF_STREAM);
			}
		}

		@Test void a03_definiteLengthArray() throws Exception {
			// [1,2,3] = 0x83 0x01 0x02 0x03
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0x83, 0x01, 0x02, 0x03})) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(3L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a04_definiteLengthMap() throws Exception {
			// {"a":1,"b":2} = 0xA2 0x61 'a' 0x01 0x61 'b' 0x02
			var bytes = new byte[]{(byte) 0xA2, 0x61, 'a', 0x01, 0x61, 'b', 0x02};
			try (var r = CborParser.DEFAULT.parseTokens(bytes)) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("b", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2L, r.getNumber().longValue());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void a05_indefiniteLengthArray() throws Exception {
			// 0x9F 0x01 0x02 0xFF = indefinite [1,2]
			var bytes = new byte[]{(byte) 0x9F, 0x01, 0x02, (byte) 0xFF};
			try (var r = CborParser.DEFAULT.parseTokens(bytes)) {
				assertSequence(r,
					TokenType.START_ARRAY,
					TokenType.VALUE_NUMBER,
					TokenType.VALUE_NUMBER,
					TokenType.END_ARRAY);
			}
		}

		@Test void a06_indefiniteLengthMap() throws Exception {
			// 0xBF 0x61 'a' 0x01 0xFF = indefinite {"a":1}
			var bytes = new byte[]{(byte) 0xBF, 0x61, 'a', 0x01, (byte) 0xFF};
			try (var r = CborParser.DEFAULT.parseTokens(bytes)) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void a07_binaryValue() throws Exception {
			// 0x44 0x01 0x02 0x03 0x04 = 4-byte binary
			var bytes = new byte[]{0x44, 0x01, 0x02, 0x03, 0x04};
			try (var r = CborParser.DEFAULT.parseTokens(bytes)) {
				assertEquals(TokenType.VALUE_BINARY, r.next());
				assertArrayEquals(new byte[]{1, 2, 3, 4}, r.getBinary());
			}
		}

		@Test void a08_booleanAndNull() throws Exception {
			// 0x83 0xF5 0xF4 0xF6 = [true, false, null]
			var bytes = new byte[]{(byte) 0x83, (byte) 0xF5, (byte) 0xF4, (byte) 0xF6};
			try (var r = CborParser.DEFAULT.parseTokens(bytes)) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertTrue(r.getBool());
				assertEquals(TokenType.VALUE_BOOLEAN, r.next()); assertFalse(r.getBool());
				assertEquals(TokenType.VALUE_NULL, r.next());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a09_capability() throws Exception {
			assertInstanceOf(TokenReadable.class, CborParser.DEFAULT);
			try (var r = CborParser.DEFAULT.parseTokens(new byte[]{(byte) 0xF6})) {
				assertReaderStreaming(r);
			}
		}
	}

	@Nested class B_writer extends TestBase {

		@Test void b01_emptyMap() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject().endObject();
			}
			// 0xBF 0xFF = indefinite-length empty map
			assertArrayEquals(new byte[]{(byte) 0xBF, (byte) 0xFF}, bos.toByteArray());
		}

		@Test void b02_emptyArray() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startArray().endArray();
			}
			// 0x9F 0xFF = indefinite-length empty array
			assertArrayEquals(new byte[]{(byte) 0x9F, (byte) 0xFF}, bos.toByteArray());
		}

		@Test void b03_simpleMap() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.endObject();
			}
			// 0xBF 0x61 'a' 0x01 0xFF
			assertArrayEquals(new byte[]{(byte) 0xBF, 0x61, 'a', 0x01, (byte) 0xFF}, bos.toByteArray());
		}

		@Test void b04_binaryNative() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.binary(new byte[]{1, 2, 3, 4});
			}
			// 0x44 = byte string length 4, then the bytes
			assertArrayEquals(new byte[]{0x44, 0x01, 0x02, 0x03, 0x04}, bos.toByteArray());
		}

		@Test void b05_capability() {
			assertInstanceOf(TokenWritable.class, CborSerializer.DEFAULT);
		}

		@Test void b06_doubleFieldNameRejected() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("a");
				assertThrows(IllegalStateException.class, () -> w.fieldName("b"));
			}
		}

		@Test void b07_endKindMismatch() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				assertThrows(IllegalStateException.class, w::endArray);
			}
		}

		@Test void b08_writeAfterCloseThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			var w = CborSerializer.DEFAULT.serializeTokens(bos);
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
		}
	}

	@Nested class C_roundTrip extends TestBase {

		@Test void c01_simpleStructure() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("greeting"); w.string("hello");
				w.fieldName("count"); w.number(3);
				w.fieldName("flags"); w.startArray();
				w.bool(true); w.bool(false); w.nil();
				w.endArray();
				w.endObject();
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME, TokenType.VALUE_STRING,
					TokenType.FIELD_NAME, TokenType.VALUE_NUMBER,
					TokenType.FIELD_NAME, TokenType.START_ARRAY,
					TokenType.VALUE_BOOLEAN, TokenType.VALUE_BOOLEAN, TokenType.VALUE_NULL,
					TokenType.END_ARRAY,
					TokenType.END_OBJECT);
			}
		}

		@Test void c02_binaryRoundTripsAsBinary() throws Exception {
			// Binary value emitted via binary() should round-trip back as VALUE_BINARY (no base64).
			var bytes = new byte[]{1, 2, 3, 4, 5};
			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.binary(bytes);
			}
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.VALUE_BINARY, r.next());
				assertArrayEquals(bytes, r.getBinary());
			}
		}
	}

	@Nested class D_read extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void d01_readBean() throws Exception {
			// {"age":30,"name":"alice"} via the canonical serializer, then bind via read.
			var b = new Bean();
			b.name = "alice";
			b.age = 30;
			var bytes = CborSerializer.DEFAULT.serialize(b);

			try (var r = CborParser.DEFAULT.parseTokens((byte[]) bytes)) {
				var got = r.read(Bean.class);
				assertEquals("alice", got.name);
				assertEquals(30, got.age);
			}
		}

		@Test void d02_streamArrayOfBeans() throws Exception {
			var b1 = new Bean(); b1.name = "alice"; b1.age = 30;
			var b2 = new Bean(); b2.name = "bob";   b2.age = 40;
			var list = java.util.List.of(b1, b2);
			var bytes = CborSerializer.DEFAULT.serialize(list);

			var seen = new java.util.ArrayList<Bean>();
			try (var r = CborParser.DEFAULT.parseTokens((byte[]) bytes)) {
				assertEquals(TokenType.START_ARRAY, r.next());
				while (r.canRead())
					seen.add(r.read(Bean.class));
				assertEquals(TokenType.END_ARRAY, r.next());
			}
			assertEquals(2, seen.size());
			assertEquals("alice", seen.get(0).name);
			assertEquals("bob", seen.get(1).name);
		}
	}

	@Nested class E_object extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void e01_objectMatchesCanonicalSerializer() throws Exception {
			var b = new Bean();
			b.name = "alice";
			b.age = 30;

			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.object(b);
			}

			// The walker emits indefinite-length containers, while the canonical serializer
			// emits definite-length.  Both decode to the same logical value via the same
			// CborParser, so re-decode and compare.
			var fromWalker = CborParser.DEFAULT.parse(bos.toByteArray(), Bean.class);
			var fromCanonical = CborParser.DEFAULT.parse((byte[]) CborSerializer.DEFAULT.serialize(b), Bean.class);
			assertEquals(fromCanonical.name, fromWalker.name);
			assertEquals(fromCanonical.age, fromWalker.age);
		}

		@Test void e02_objectWithBinary() throws Exception {
			// byte[] inside a Map should emit as VALUE_BINARY native, not base64.
			var m = new java.util.LinkedHashMap<String, Object>();
			m.put("name", "alice");
			m.put("data", new byte[]{1, 2, 3});

			var bos = new ByteArrayOutputStream();
			try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
				w.object(m);
			}

			// Decode via the cursor and verify VALUE_BINARY for the data field.
			try (var r = CborParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.START_OBJECT, r.next());
				while (r.next() != TokenType.END_OBJECT) {
					var key = r.getFieldName();
					var t = r.next();
					if ("data".equals(key))
						assertEquals(TokenType.VALUE_BINARY, t);
				}
			}
		}
	}

	@Nested class F_tagNestingGuard extends TestBase {

		// Builds <count> nested CBOR semantic tags (0xC0 = tag(0)) wrapping the integer 0 (0x00).
		private static byte[] nestedTags(int count) {
			var b = new byte[count + 1];
			for (var i = 0; i < count; i++)
				b[i] = (byte) 0xC0;
			b[count] = 0x00;  // uint 0
			return b;
		}

		@Test void f01_atDefaultLimitParses() throws Exception {
			// Exactly at the default limit (64 tags) — must still unwrap and emit the value.
			try (var r = CborParser.DEFAULT.parseTokens(nestedTags(CborTokenReader.DEFAULT_MAX_TAG_NESTING_DEPTH))) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(0L, r.getNumber().longValue());
			}
		}

		@Test void f02_exceedingDefaultLimitThrows() throws Exception {
			// One past the default limit — must throw a ParseException, not a StackOverflowError.
			try (var r = CborParser.DEFAULT.parseTokens(nestedTags(CborTokenReader.DEFAULT_MAX_TAG_NESTING_DEPTH + 1))) {
				assertThrowsWithMessage(ParseException.class, "CBOR tag nesting depth exceeded the maximum of 64", r::next);
			}
		}

		@Test void f03_configurableLimitAtBoundaryParses() throws Exception {
			// A custom (lower) limit is honored: depth == limit still parses.
			try (var r = (CborTokenReader) CborParser.DEFAULT.parseTokens(nestedTags(4))) {
				r.setMaxTagNestingDepth(4);
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(0L, r.getNumber().longValue());
			}
		}

		@Test void f04_configurableLimitExceededThrows() throws Exception {
			// A custom (lower) limit is honored: depth > limit throws with the configured bound.
			try (var r = (CborTokenReader) CborParser.DEFAULT.parseTokens(nestedTags(5))) {
				r.setMaxTagNestingDepth(4);
				assertThrowsWithMessage(ParseException.class, "CBOR tag nesting depth exceeded the maximum of 4", r::next);
			}
		}

		@Test void f05_singleTagStillUnwraps() throws Exception {
			// Baseline: a single tag around an integer unwraps to the value (regression guard for
			// the tag counter reset between independent values).
			try (var r = CborParser.DEFAULT.parseTokens(nestedTags(1))) {
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(0L, r.getNumber().longValue());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}
}
