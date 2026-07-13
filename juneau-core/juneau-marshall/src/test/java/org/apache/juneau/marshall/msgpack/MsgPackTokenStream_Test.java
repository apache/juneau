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
import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the public MessagePack token-streaming surface.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class MsgPackTokenStream_Test extends TestBase {

	@Nested class A_reader extends TestBase {

		@Test void a01_emptyMap() throws Exception {
			// 0x80 = fixmap with 0 entries
			try (var r = MsgPackParser.DEFAULT.parseTokens(new byte[]{(byte) 0x80})) {
				assertSequence(r, TokenType.START_OBJECT, TokenType.END_OBJECT, TokenType.END_OF_STREAM);
			}
		}

		@Test void a02_emptyArray() throws Exception {
			// 0x90 = fixarray with 0 entries
			try (var r = MsgPackParser.DEFAULT.parseTokens(new byte[]{(byte) 0x90})) {
				assertSequence(r, TokenType.START_ARRAY, TokenType.END_ARRAY, TokenType.END_OF_STREAM);
			}
		}

		@Test void a03_simpleArray() throws Exception {
			// 0x93 0x01 0x02 0x03 = fixarray[3] of 1,2,3
			var bytes = new byte[]{(byte) 0x93, 0x01, 0x02, 0x03};
			try (var r = MsgPackParser.DEFAULT.parseTokens(bytes)) {
				assertEquals(TokenType.START_ARRAY, r.next());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(3L, r.getNumber().longValue());
				assertEquals(TokenType.END_ARRAY, r.next());
			}
		}

		@Test void a04_simpleMap() throws Exception {
			// 0x82 0xA1 'a' 0x01 0xA1 'b' 0x02 = {"a":1,"b":2}
			var bytes = new byte[]{(byte) 0x82, (byte) 0xA1, 'a', 0x01, (byte) 0xA1, 'b', 0x02};
			try (var r = MsgPackParser.DEFAULT.parseTokens(bytes)) {
				assertEquals(TokenType.START_OBJECT, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("a", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.FIELD_NAME, r.next()); assertEquals("b", r.getFieldName());
				assertEquals(TokenType.VALUE_NUMBER, r.next());
				assertEquals(TokenType.END_OBJECT, r.next());
			}
		}

		@Test void a05_capability() {
			assertInstanceOf(TokenReadable.class, MsgPackParser.DEFAULT);
		}
	}

	@Nested class B_writer extends TestBase {

		@Test void b01_emptyMap() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject().endObject();
			}
			// 0x80 = fixmap[0]
			assertArrayEquals(new byte[]{(byte) 0x80}, bos.toByteArray());
		}

		@Test void b02_emptyArray() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.startArray().endArray();
			}
			// 0x90 = fixarray[0]
			assertArrayEquals(new byte[]{(byte) 0x90}, bos.toByteArray());
		}

		@Test void b03_simpleMapBufferedAndCounted() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.fieldName("b"); w.number(2);
				w.endObject();
			}
			// 0x82 0xA1 'a' 0x01 0xA1 'b' 0x02
			assertArrayEquals(
				new byte[]{(byte) 0x82, (byte) 0xA1, 'a', 0x01, (byte) 0xA1, 'b', 0x02},
				bos.toByteArray());
		}

		@Test void b04_nestedContainersBufferIndependently() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("a");
				w.startArray();
				w.number(1);
				w.number(2);
				w.endArray();
				w.endObject();
			}
			// 0x81 0xA1 'a' 0x92 0x01 0x02
			assertArrayEquals(
				new byte[]{(byte) 0x81, (byte) 0xA1, 'a', (byte) 0x92, 0x01, 0x02},
				bos.toByteArray());
		}

		@Test void b05_binaryNative() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.binary(new byte[]{1, 2, 3});
			}
			// 0xC4 0x03 0x01 0x02 0x03 = bin8 length 3
			assertArrayEquals(new byte[]{(byte) 0xC4, 0x03, 0x01, 0x02, 0x03}, bos.toByteArray());
		}

		@Test void b06_capability() {
			assertInstanceOf(TokenWritable.class, MsgPackSerializer.DEFAULT);
		}

		@Test void b07_writeAfterCloseThrows() throws Exception {
			var bos = new ByteArrayOutputStream();
			var w = MsgPackSerializer.DEFAULT.serializeTokens(bos);
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

		@Test void c01_writeThenRead() throws Exception {
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.startObject();
				w.fieldName("name"); w.string("alice");
				w.fieldName("age"); w.number(30);
				w.endObject();
			}
			try (var r = MsgPackParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME, TokenType.VALUE_STRING,
					TokenType.FIELD_NAME, TokenType.VALUE_NUMBER,
					TokenType.END_OBJECT);
			}
		}

		@Test void c02_binaryRoundTripsAsBinary() throws Exception {
			var bytes = new byte[]{1, 2, 3, 4};
			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.binary(bytes);
			}
			try (var r = MsgPackParser.DEFAULT.parseTokens(bos.toByteArray())) {
				assertEquals(TokenType.VALUE_BINARY, r.next());
				assertArrayEquals(bytes, r.getBinary());
			}
		}
	}

	@Nested class D_bridges extends TestBase {

		public static class Bean {
			public String name;
			public int age;
		}

		@Test void d01_readBean() throws Exception {
			var b = new Bean();
			b.name = "alice";
			b.age = 30;
			var bytes = MsgPackSerializer.DEFAULT.serialize(b);

			try (var r = MsgPackParser.DEFAULT.parseTokens((byte[]) bytes)) {
				var got = r.read(Bean.class);
				assertEquals("alice", got.name);
				assertEquals(30, got.age);
			}
		}

		@Test void d02_objectMatchesCanonicalSerializer() throws Exception {
			var b = new Bean();
			b.name = "alice";
			b.age = 30;

			var bos = new ByteArrayOutputStream();
			try (var w = MsgPackSerializer.DEFAULT.serializeTokens(bos)) {
				w.object(b);
			}

			// Round-trip via MsgPackParser (the cursor walks the bean alphabetically by BeanMap;
			// canonical MsgPackSerializer also alphabetical).  Compare the parsed Bean.
			var fromWalker = MsgPackParser.DEFAULT.parse(bos.toByteArray(), Bean.class);
			assertEquals("alice", fromWalker.name);
			assertEquals(30, fromWalker.age);
		}

		@Test void d03_streamArrayOfBeans() throws Exception {
			var b1 = new Bean(); b1.name = "alice"; b1.age = 30;
			var b2 = new Bean(); b2.name = "bob";   b2.age = 40;
			var list = java.util.List.of(b1, b2);
			var bytes = MsgPackSerializer.DEFAULT.serialize(list);

			var seen = new java.util.ArrayList<Bean>();
			try (var r = MsgPackParser.DEFAULT.parseTokens((byte[]) bytes)) {
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
}
