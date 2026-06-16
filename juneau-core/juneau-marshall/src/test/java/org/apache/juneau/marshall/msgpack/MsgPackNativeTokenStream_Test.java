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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the MsgPack opt-in binary-native token surface (175ad).  Verifies that
 * {@link MsgPackParserSession#parseNativeTokens(Object) parseNativeTokens} surfaces the {@code ext}
 * type byte as token-level metadata, and that the default
 * {@link MsgPackParserSession#parseTokens(Object) parseTokens} path still drops it.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class MsgPackNativeTokenStream_Test extends TestBase {

	private static byte[] ext(int type, byte... payload) throws IOException {
		var bos = new ByteArrayOutputStream();
		try (var os = new MsgPackOutputStream(bos)) {
			os.writeExt(type, payload);
		}
		return bos.toByteArray();
	}

	// =================================================================================
	// A. Reader native-mode (parseNativeTokens)
	// =================================================================================

	@Test void a01_ext() throws Exception {
		// fixext4 with type 5 and 4-byte payload.
		var data = ext(5, (byte) 1, (byte) 2, (byte) 3, (byte) 4);
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(BinaryNativeKind.MSGPACK_EXT, r.getNativeKind());
			assertEquals(5, r.getExtType());
			assertArrayEquals(new byte[]{1, 2, 3, 4}, r.getBinary());
		}
	}

	@Test void a02_negativeExtType() throws Exception {
		// Negative ext type (-1) — must round-trip as signed int8.
		var data = ext(-1, (byte) 0x7F);
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(-1, r.getExtType());
		}
	}

	@Test void a03_defaultStillNormalizes() throws Exception {
		// Same input via plain parseTokens: VALUE_BINARY with no native metadata, type byte dropped.
		var data = ext(5, (byte) 1, (byte) 2, (byte) 3, (byte) 4);
		try (var r = MsgPackParser.DEFAULT.parseTokens(data)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertArrayEquals(new byte[]{1, 2, 3, 4}, r.getBinary());
		}
	}

	@Test void a04_tagAccessOnMsgPack() throws Exception {
		var data = ext(5, (byte) 0);
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			// MsgPack has no tags.
			assertEquals(0, r.getTagCount());
			assertThrows(IndexOutOfBoundsException.class, () -> r.getTag(0));
		}
	}

	@Test void a05_simpleValueOnMsgPack() throws Exception {
		var data = ext(5, (byte) 0);
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			// MsgPack has no CBOR simple values.
			assertThrows(IllegalStateException.class, r::getSimpleValue);
		}
	}

	@Test void a06_extTypeOutOfStateThrows() throws Exception {
		// A plain string token has no ext: getExtType must throw.
		var data = new byte[]{(byte) 0xA1, (byte) 'a'};  // fixstr len 1, "a"
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertThrows(IllegalStateException.class, r::getExtType);
		}
	}

	@Test void a08_largeExt() throws Exception {
		// 257 bytes uses ext16 framing internally; native-mode must surface the type/payload.
		var p = new byte[257];
		for (var i = 0; i < 257; i++) p[i] = (byte) i;
		var data = ext(127, p);
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(127, r.getExtType());
			assertArrayEquals(p, r.getBinary());
		}
	}

	// =================================================================================
	// B. Writer native-mode (serializeTokens) — round-trip through reader
	// =================================================================================

	@Test void b01_writeExt() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.writeExt(5, new byte[]{1, 2, 3, 4});
		}
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(BinaryNativeKind.MSGPACK_EXT, r.getNativeKind());
			assertEquals(5, r.getExtType());
			assertArrayEquals(new byte[]{1, 2, 3, 4}, r.getBinary());
		}
	}

	@Test void b02_extInsideArray() throws Exception {
		// [ ext(5, [1,2,3,4]) ]
		var bos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.startArray();
			w.writeExt(5, new byte[]{1, 2, 3, 4});
			w.endArray();
		}
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.START_ARRAY, r.next());
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(5, r.getExtType());
			assertArrayEquals(new byte[]{1, 2, 3, 4}, r.getBinary());
			assertEquals(TokenType.END_ARRAY, r.next());
		}
	}

	@Test void b03_extInsideMap() throws Exception {
		// { "k": ext(5, [9]) }
		var bos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.startObject();
			w.fieldName("k");
			w.writeExt(5, new byte[]{9});
			w.endObject();
		}
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.START_OBJECT, r.next());
			assertEquals(TokenType.FIELD_NAME, r.next());
			assertEquals("k", r.getFieldName());
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(5, r.getExtType());
			assertArrayEquals(new byte[]{9}, r.getBinary());
			assertEquals(TokenType.END_OBJECT, r.next());
		}
	}

	@Test void b04_negativeType() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.writeExt(-1, new byte[]{0});
		}
		try (var r = MsgPackParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.VALUE_BINARY, r.next());
			assertEquals(-1, r.getExtType());
		}
	}

	@Test void b05_writeTagUnsupported() throws Exception {
		// MsgPack doesn't support CBOR tags; the inherited default-throwing writeTag/writeSimple
		// should fire.
		var bos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			assertThrows(UnsupportedOperationException.class, () -> w.writeTag(0));
			assertThrows(UnsupportedOperationException.class, () -> w.writeSimple(16));
		}
	}
}
