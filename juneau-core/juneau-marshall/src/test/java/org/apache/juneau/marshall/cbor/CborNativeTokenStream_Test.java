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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the CBOR opt-in binary-native token surface (175ad).  Verifies that
 * {@link CborParserSession#parseNativeTokens(Object) parseNativeTokens} surfaces semantic tags
 * and simple values as token-level metadata, and that the default
 * {@link CborParserSession#parseTokens(Object) parseTokens} path still normalizes them away.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class CborNativeTokenStream_Test extends TestBase {

	private static byte[] bytes(int... b) {
		var r = new byte[b.length];
		for (var i = 0; i < b.length; i++) r[i] = (byte) b[i];
		return r;
	}

	// =================================================================================
	// A. Reader native-mode (parseNativeTokens)
	// =================================================================================

	@Test void a01_singleTag() throws Exception {
		// 0xC0 = tag(0); 0x61 = string len 1; 'a' = 0x61 — tag(0) wraps "a".
		var data = bytes(0xC0, 0x61, 0x61);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertEquals(BinaryNativeKind.CBOR_TAG, r.getNativeKind());
			assertEquals(1, r.getTagCount());
			assertEquals(0L, r.getTag(0));
			assertEquals("a", r.getString());
			assertEquals(TokenType.END_OF_STREAM, r.next());
		}
	}

	@Test void a02_nestedTags() throws Exception {
		// tag(1)(tag(2)(uint 5)).  tag-1 = 0xC1, tag-2 = 0xC2, uint 5 = 0x05.
		var data = bytes(0xC1, 0xC2, 0x05);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_NUMBER, r.next());
			assertEquals(BinaryNativeKind.CBOR_TAG, r.getNativeKind());
			assertEquals(2, r.getTagCount());
			assertEquals(1L, r.getTag(0)); // outermost first
			assertEquals(2L, r.getTag(1));
			assertEquals(5L, r.getNumber().longValue());
		}
	}

	@Test void a03_taggedContainer() throws Exception {
		// tag(4) wrapping an empty array: 0xC4 0x80
		var data = bytes(0xC4, 0x80);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.START_ARRAY, r.next());
			assertEquals(BinaryNativeKind.CBOR_TAG, r.getNativeKind());
			assertEquals(1, r.getTagCount());
			assertEquals(4L, r.getTag(0));
			// Subsequent END_ARRAY is not tagged.
			assertEquals(TokenType.END_ARRAY, r.next());
			assertEquals(BinaryNativeKind.NONE, r.getNativeKind());
			assertEquals(0, r.getTagCount());
		}
	}

	@Test void a04_simpleValue() throws Exception {
		// Major-7 simple value 16: 0xF0 (initial byte = 0xE0 | 16).
		var data = bytes(0xF0);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_NULL, r.next());
			assertEquals(BinaryNativeKind.CBOR_SIMPLE, r.getNativeKind());
			assertEquals(16, r.getSimpleValue());
		}
	}

	@Test void a05_defaultStillNormalizes() throws Exception {
		// Same inputs via plain parseTokens: tags are silently unwrapped, simple → VALUE_NULL.
		var taggedData = bytes(0xC1, 0xC2, 0x05);
		try (var r = CborParser.DEFAULT.parseTokens(taggedData)) {
			assertEquals(TokenType.VALUE_NUMBER, r.next());
			assertEquals(5L, r.getNumber().longValue());
		}
		var simpleData = bytes(0xF0);
		try (var r = CborParser.DEFAULT.parseTokens(simpleData)) {
			assertEquals(TokenType.VALUE_NULL, r.next());
		}
	}

	@Test void a06_extTypeAccessThrowsOnCbor() throws Exception {
		var data = bytes(0xC0, 0x61, 0x61);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertThrows(IllegalStateException.class, r::getExtType);
		}
	}

	@Test void a07_simpleValueOutOfStateThrows() throws Exception {
		// A plain string token has no simple value: getSimpleValue must throw.
		var data = bytes(0x61, 0x61);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertThrows(IllegalStateException.class, r::getSimpleValue);
		}
	}

	@Test void a08_getTagOutOfBounds() throws Exception {
		var data = bytes(0xC0, 0x61, 0x61);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertThrows(IndexOutOfBoundsException.class, () -> r.getTag(1));
			assertThrows(IndexOutOfBoundsException.class, () -> r.getTag(-1));
		}
	}

	@Test void a10_tagStackGrows() throws Exception {
		// Five nested tags exceeds the initial tags[] array (size 4) and must trigger growth.
		// 0xC0 0xC0 0xC0 0xC0 0xC0 0x05 — 5 tags wrapping uint 5.
		var data = bytes(0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0x05);
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(data)) {
			assertEquals(TokenType.VALUE_NUMBER, r.next());
			assertEquals(5, r.getTagCount());
			for (var i = 0; i < 5; i++)
				assertEquals(0L, r.getTag(i));
		}
	}

	// =================================================================================
	// B. Writer native-mode (serializeTokens) — round-trip through reader
	// =================================================================================

	@Test void b01_writeTaggedString() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.writeTag(0);
			w.string("hello");
		}
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertEquals(BinaryNativeKind.CBOR_TAG, r.getNativeKind());
			assertEquals(1, r.getTagCount());
			assertEquals(0L, r.getTag(0));
			assertEquals("hello", r.getString());
		}
	}

	@Test void b02_writeNestedTags() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.writeTag(1);
			w.writeTag(2);
			w.number(5L);
		}
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.VALUE_NUMBER, r.next());
			assertEquals(2, r.getTagCount());
			assertEquals(1L, r.getTag(0));
			assertEquals(2L, r.getTag(1));
			assertEquals(5L, r.getNumber().longValue());
		}
	}

	@Test void b03_writeSimple() throws Exception {
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.writeSimple(16);
		}
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.VALUE_NULL, r.next());
			assertEquals(BinaryNativeKind.CBOR_SIMPLE, r.getNativeKind());
			assertEquals(16, r.getSimpleValue());
		}
	}

	@Test void b04_taggedInsideMap() throws Exception {
		// Tag a map value: { "k": tag(0)("v") }.
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.startObject();
			w.fieldName("k");
			w.writeTag(0);
			w.string("v");
			w.endObject();
		}
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.START_OBJECT, r.next());
			assertEquals(TokenType.FIELD_NAME, r.next());
			assertEquals("k", r.getFieldName());
			assertEquals(TokenType.VALUE_STRING, r.next());
			assertEquals(BinaryNativeKind.CBOR_TAG, r.getNativeKind());
			assertEquals(1, r.getTagCount());
			assertEquals(0L, r.getTag(0));
			assertEquals("v", r.getString());
			assertEquals(TokenType.END_OBJECT, r.next());
		}
	}

	@Test void b05_taggedInsideArray() throws Exception {
		// Tag an array element: [ tag(7)(42) ].
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			w.startArray();
			w.writeTag(7);
			w.number(42L);
			w.endArray();
		}
		try (var r = CborParser.DEFAULT_NATIVE.getSession().parseTokens(bos.toByteArray())) {
			assertEquals(TokenType.START_ARRAY, r.next());
			assertEquals(TokenType.VALUE_NUMBER, r.next());
			assertEquals(7L, r.getTag(0));
			assertEquals(42L, r.getNumber().longValue());
			assertEquals(TokenType.END_ARRAY, r.next());
		}
	}

	@Test void b06_extWriteUnsupported() throws Exception {
		// CBOR doesn't support MsgPack ext; the inherited default-throwing writeExt should fire.
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.getSession().serializeTokens(bos)) {
			assertThrows(UnsupportedOperationException.class, () -> w.writeExt(0, new byte[]{1}));
		}
	}

	// =================================================================================
	// C. Convenience-class delegation (CborParser/CborSerializer DEFAULT)
	// =================================================================================

	@Test void c01_convenienceDelegation() throws Exception {
		// CborParser.DEFAULT_NATIVE.parseTokens(...) must yield the same shape as session-level.
		var data = bytes(0xC0, 0x05);
		try (var r = CborParser.DEFAULT_NATIVE.parseTokens(data)) {
			assertEquals(TokenType.VALUE_NUMBER, r.next());
			assertEquals(BinaryNativeKind.CBOR_TAG, r.getNativeKind());
			assertEquals(1, r.getTagCount());
			assertEquals(0L, r.getTag(0));
		}
		// And the writer side: serializeTokens via DEFAULT.
		var bos = new ByteArrayOutputStream();
		try (var w = CborSerializer.DEFAULT.serializeTokens(bos)) {
			w.writeTag(0);
			w.number(5L);
		}
		assertArrayEquals(data, bos.toByteArray());
	}
}
