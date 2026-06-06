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
package org.apache.juneau.bson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.parser.ParserPipe;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonInputStream}.
 */
@SuppressWarnings({"resource"})
class BsonInputStream_Test extends TestBase {

	// ====================================================================
	// Helpers
	// ====================================================================

	/** Creates a {@link BsonInputStream} backed by the given byte array. */
	private static BsonInputStream openIs(byte[] bytes) throws IOException {
		var pipe = new ParserPipe(new ByteArrayInputStream(bytes));
		return new BsonInputStream(pipe);
	}

	private static byte[] le4(int v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
	}

	private static byte[] le8(long v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24),
			(byte) (v >> 32), (byte) (v >> 40), (byte) (v >> 48), (byte) (v >> 56)};
	}

	private static byte[] cat(byte[]... arrs) {
		var total = 0;
		for (var a : arrs)
			total += a.length;
		var out = new byte[total];
		var pos = 0;
		for (var a : arrs) {
			System.arraycopy(a, 0, out, pos, a.length);
			pos += a.length;
		}
		return out;
	}

	private static byte[] bsonString(String s) {
		var b = s.getBytes(StandardCharsets.UTF_8);
		var out = new byte[4 + b.length + 1];
		var len = b.length + 1; // includes terminator
		out[0] = (byte) len;
		out[1] = (byte) (len >> 8);
		out[2] = (byte) (len >> 16);
		out[3] = (byte) (len >> 24);
		System.arraycopy(b, 0, out, 4, b.length);
		out[4 + b.length] = 0x00;
		return out;
	}

	private static byte[] cstring(String s) {
		var b = s.getBytes(StandardCharsets.UTF_8);
		var out = new byte[b.length + 1];
		System.arraycopy(b, 0, out, 0, b.length);
		out[b.length] = 0x00;
		return out;
	}

	// ====================================================================
	// Round-trip via parser/serializer (existing tests preserved)
	// ====================================================================

	@Test
	void a01_readDocumentViaParser() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("x", 1, "y", "foo"));
		try (var pipe = new ParserPipe(bytes)) {
			try (var is = new BsonInputStream(pipe)) {
				var size = is.readDocumentSize();
				assertTrue(size > 0);
				assertTrue(size <= bytes.length);
			}
		}
	}

	@Test
	void a02_parseSimpleMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("a", 42, "b", "hello"));
		var p = BsonParser.create().build();
		var result = p.parse(bytes, JsonMap.class);
		assertNotNull(result);
		assertEquals(42, result.get("a"));
		assertEquals("hello", result.get("b"));
	}

	@Test
	void a03_parseArray() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(java.util.List.of(1, 2, 3));
		var p = BsonParser.create().build();
		var result = p.parse(bytes, java.util.List.class);
		assertNotNull(result);
		assertEquals(java.util.List.of(1, 2, 3), result);
	}

	// ====================================================================
	// Primitive readers - happy path
	// ====================================================================

	@Test
	void b01_readLE4_basic() throws Exception {
		try (var is = openIs(le4(0x12345678))) {
			assertEquals(0x12345678, is.readLE4());
		}
	}

	@Test
	void b02_readLE4_negative() throws Exception {
		try (var is = openIs(le4(-1))) {
			assertEquals(-1, is.readLE4());
		}
	}

	@Test
	void b03_readLE8_basic() throws Exception {
		try (var is = openIs(le8(0x0123456789ABCDEFL))) {
			assertEquals(0x0123456789ABCDEFL, is.readLE8());
		}
	}

	@Test
	void b04_readLE8_minusOne() throws Exception {
		try (var is = openIs(le8(-1L))) {
			assertEquals(-1L, is.readLE8());
		}
	}

	@Test
	void b05_readDocumentSize() throws Exception {
		try (var is = openIs(le4(42))) {
			assertEquals(42, is.readDocumentSize());
		}
	}

	@Test
	void b06_readElementType() throws Exception {
		try (var is = openIs(new byte[]{0x10})) {
			assertEquals(0x10, is.readElementType());
		}
	}

	@Test
	void b07_readElementType_unsignedHigh() throws Exception {
		try (var is = openIs(new byte[]{(byte) 0xFF})) {
			assertEquals(0xFF, is.readElementType());
		}
	}

	@Test
	void b08_readElementName() throws Exception {
		try (var is = openIs(cstring("abc"))) {
			assertEquals("abc", is.readElementName());
		}
	}

	@Test
	void b09_readCString_empty() throws Exception {
		try (var is = openIs(new byte[]{0x00})) {
			assertEquals("", is.readCString());
		}
	}

	@Test
	void b10_readCString_unicode() throws Exception {
		// "héllo" -> e9 in latin1 but utf8 multi-byte
		var name = "héllo";
		try (var is = openIs(cstring(name))) {
			assertEquals(name, is.readCString());
		}
	}

	@Test
	void b11_isDocumentEnd_true() throws Exception {
		try (var is = openIs(new byte[]{0x00})) {
			assertTrue(is.isDocumentEnd());
			// Pushback should make subsequent read return the same byte
			is.readDocumentTerminator();
		}
	}

	@Test
	void b12_isDocumentEnd_false() throws Exception {
		try (var is = openIs(new byte[]{0x10, 0x00})) {
			assertFalse(is.isDocumentEnd());
			// Pushback should make read return the 0x10
			assertEquals(0x10, is.readElementType());
			assertTrue(is.isDocumentEnd());
		}
	}

	@Test
	void b13_isDocumentEnd_eof() throws Exception {
		try (var is = openIs(new byte[]{})) {
			assertFalse(is.isDocumentEnd());
		}
	}

	@Test
	void b14_readDocumentTerminator_ok() throws Exception {
		try (var is = openIs(new byte[]{0x00})) {
			is.readDocumentTerminator();
		}
	}

	@Test
	void b15_readDocumentTerminator_bad() throws Exception {
		try (var is = openIs(new byte[]{0x01})) {
			var ex = assertThrows(IOException.class, is::readDocumentTerminator);
			assertTrue(ex.getMessage().contains("Expected document terminator"));
		}
	}

	@Test
	void b16_readDouble() throws Exception {
		try (var is = openIs(le8(Double.doubleToLongBits(3.14159)))) {
			assertEquals(3.14159, is.readDouble(), 0.0);
		}
	}

	@Test
	void b17_readInt32() throws Exception {
		try (var is = openIs(le4(-12345))) {
			assertEquals(-12345, is.readInt32());
		}
	}

	@Test
	void b18_readInt64() throws Exception {
		try (var is = openIs(le8(0x7FFFFFFFFFFFFFFFL))) {
			assertEquals(Long.MAX_VALUE, is.readInt64());
		}
	}

	@Test
	void b19_readBoolean_true() throws Exception {
		try (var is = openIs(new byte[]{0x01})) {
			assertTrue(is.readBoolean());
		}
	}

	@Test
	void b20_readBoolean_false() throws Exception {
		try (var is = openIs(new byte[]{0x00})) {
			assertFalse(is.readBoolean());
		}
	}

	@Test
	void b21_readBoolean_nonZero() throws Exception {
		// Any non-zero byte is treated as true.
		try (var is = openIs(new byte[]{0x42})) {
			assertTrue(is.readBoolean());
		}
	}

	@Test
	void b22_readString() throws Exception {
		try (var is = openIs(bsonString("hello"))) {
			assertEquals("hello", is.readString());
		}
	}

	@Test
	void b23_readString_empty() throws Exception {
		try (var is = openIs(bsonString(""))) {
			assertEquals("", is.readString());
		}
	}

	@Test
	void b24_readBinary() throws Exception {
		var data = new byte[]{0x01, 0x02, 0x03, 0x04};
		try (var is = openIs(cat(le4(4), new byte[]{0x00}, data))) {
			assertArrayEquals(data, is.readBinary());
		}
	}

	@Test
	void b25_readBinary_empty() throws Exception {
		try (var is = openIs(cat(le4(0), new byte[]{0x00}))) {
			assertArrayEquals(new byte[0], is.readBinary());
		}
	}

	@Test
	void b26_readDateTime() throws Exception {
		try (var is = openIs(le8(1700000000000L))) {
			assertEquals(1700000000000L, is.readDateTime());
		}
	}

	@Test
	void b27_readObjectId() throws Exception {
		var oid = new byte[]{0x50, 0x7F, 0x1F, 0x77, (byte) 0xBC, (byte) 0xF8, 0x6C, (byte) 0xD7,
			(byte) 0x99, 0x43, (byte) 0x90, 0x11};
		try (var is = openIs(oid)) {
			assertEquals("507f1f77bcf86cd799439011", is.readObjectId());
		}
	}

	@Test
	void b28_readObjectId_zeros() throws Exception {
		try (var is = openIs(new byte[12])) {
			assertEquals("000000000000000000000000", is.readObjectId());
		}
	}

	@Test
	void b29_readDecimal128_zero() throws Exception {
		// Round-trip via BsonOutputStream to get correct bytes
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.writeDecimal128(BigDecimal.ZERO);
		bson.flush();
		try (var is = openIs(out.toByteArray())) {
			var dec = is.readDecimal128();
			assertEquals(0, dec.compareTo(BigDecimal.ZERO));
		}
	}

	@Test
	void b30_readDecimal128_value() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.writeDecimal128(new BigDecimal("3.14"));
		bson.flush();
		try (var is = openIs(out.toByteArray())) {
			var dec = is.readDecimal128();
			assertEquals(0, dec.compareTo(new BigDecimal("3.14")));
		}
	}

	// ====================================================================
	// Pushback mechanism (read after pushback)
	// ====================================================================

	@Test
	void c01_pushbackReadPath() throws Exception {
		try (var is = openIs(new byte[]{0x10, 0x20, 0x30, 0x40})) {
			// isDocumentEnd reads a byte, sees non-zero, pushes back
			assertFalse(is.isDocumentEnd());
			// readLE4 should now read all four bytes including the pushed-back one
			assertEquals(0x40302010, is.readLE4());
		}
	}

	// ====================================================================
	// EOF / malformed paths
	// ====================================================================

	@Test
	void d01_readLE4_eof() throws Exception {
		try (var is = openIs(new byte[]{0x01, 0x02, 0x03})) {
			var ex = assertThrows(IOException.class, is::readLE4);
			assertTrue(ex.getMessage().contains("Unexpected end of BSON stream"));
		}
	}

	@Test
	void d02_readElementType_eof() throws Exception {
		try (var is = openIs(new byte[0])) {
			assertThrows(IOException.class, is::readElementType);
		}
	}

	@Test
	void d03_readCString_eof() throws Exception {
		// No terminator
		try (var is = openIs(new byte[]{'a', 'b', 'c'})) {
			assertThrows(IOException.class, is::readCString);
		}
	}

	@Test
	void d04_readBoolean_eof() throws Exception {
		try (var is = openIs(new byte[0])) {
			assertThrows(IOException.class, is::readBoolean);
		}
	}

	@Test
	void d05_readString_invalidLengthZero() throws Exception {
		try (var is = openIs(le4(0))) {
			var ex = assertThrows(IOException.class, is::readString);
			assertTrue(ex.getMessage().contains("Invalid BSON string length"));
		}
	}

	@Test
	void d06_readString_invalidLengthNegative() throws Exception {
		try (var is = openIs(le4(-5))) {
			assertThrows(IOException.class, is::readString);
		}
	}

	@Test
	void d07_readString_eofMidPayload() throws Exception {
		// Length says 5 (4 chars + null), but we only supply 2 bytes
		try (var is = openIs(cat(le4(5), new byte[]{'a', 'b'}))) {
			assertThrows(IOException.class, is::readString);
		}
	}

	@Test
	void d08_readString_missingTerminator() throws Exception {
		// Length says 4 (3 chars + null), supply 4 bytes but last is non-zero
		try (var is = openIs(cat(le4(4), new byte[]{'a', 'b', 'c', 0x42}))) {
			var ex = assertThrows(IOException.class, is::readString);
			assertTrue(ex.getMessage().contains("Expected string terminator"));
		}
	}

	@Test
	void d09_readBinary_eofSubtype() throws Exception {
		try (var is = openIs(le4(4))) {
			assertThrows(IOException.class, is::readBinary);
		}
	}

	@Test
	void d10_readBinary_eofPayload() throws Exception {
		try (var is = openIs(cat(le4(4), new byte[]{0x00, 0x01}))) {
			assertThrows(IOException.class, is::readBinary);
		}
	}

	@Test
	void d11_readObjectId_eof() throws Exception {
		try (var is = openIs(new byte[]{0x01, 0x02, 0x03})) {
			assertThrows(IOException.class, is::readObjectId);
		}
	}

	@Test
	void d12_readDecimal128_eof() throws Exception {
		try (var is = openIs(new byte[]{0x01, 0x02, 0x03})) {
			assertThrows(IOException.class, is::readDecimal128);
		}
	}

	// ====================================================================
	// skipValue - all BSON types
	// ====================================================================

	@Test
	void e01_skipValue_double() throws Exception {
		try (var is = openIs(cat(le8(0L), new byte[]{0x42}))) {
			is.skipValue(0x01);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e02_skipValue_string() throws Exception {
		// 0x02 reads int32 length, then skip(len) bytes
		try (var is = openIs(cat(bsonString("foo"), new byte[]{0x42}))) {
			is.skipValue(0x02);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e03_skipValue_embeddedDocument() throws Exception {
		// 0x03: reads size (int32), skips size-4 bytes
		// Build a tiny doc: size=5 (just terminator)
		try (var is = openIs(cat(le4(5), new byte[]{0x00, 0x42}))) {
			is.skipValue(0x03);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e04_skipValue_array() throws Exception {
		try (var is = openIs(cat(le4(5), new byte[]{0x00, 0x42}))) {
			is.skipValue(0x04);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e05_skipValue_binary() throws Exception {
		// 0x05: reads len, then subtype byte, then skip(len)
		try (var is = openIs(cat(le4(3), new byte[]{0x00, 0x01, 0x02, 0x03, 0x42}))) {
			is.skipValue(0x05);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e06_skipValue_undefined() throws Exception {
		// 0x06: zero bytes consumed
		try (var is = openIs(new byte[]{0x42})) {
			is.skipValue(0x06);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e07_skipValue_objectId() throws Exception {
		// 0x07: skip 12 bytes
		try (var is = openIs(cat(new byte[12], new byte[]{0x42}))) {
			is.skipValue(0x07);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e08_skipValue_boolean() throws Exception {
		// 0x08: read 1 byte
		try (var is = openIs(new byte[]{0x01, 0x42})) {
			is.skipValue(0x08);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e09_skipValue_dateTime() throws Exception {
		// 0x09: 8 bytes
		try (var is = openIs(cat(le8(0L), new byte[]{0x42}))) {
			is.skipValue(0x09);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e10_skipValue_null() throws Exception {
		// 0x0A: zero bytes
		try (var is = openIs(new byte[]{0x42})) {
			is.skipValue(0x0A);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e11_skipValue_regex() throws Exception {
		// 0x0B: two cstrings
		try (var is = openIs(cat(cstring("pattern"), cstring("i"), new byte[]{0x42}))) {
			is.skipValue(0x0B);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e12_skipValue_dbpointer() throws Exception {
		// 0x0C: string + 12 bytes
		try (var is = openIs(cat(bsonString("ns"), new byte[12], new byte[]{0x42}))) {
			is.skipValue(0x0C);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e13_skipValue_javascript() throws Exception {
		// 0x0D: string
		try (var is = openIs(cat(bsonString("var x;"), new byte[]{0x42}))) {
			is.skipValue(0x0D);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e14_skipValue_symbol() throws Exception {
		// 0x0E: string
		try (var is = openIs(cat(bsonString("sym"), new byte[]{0x42}))) {
			is.skipValue(0x0E);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e15_skipValue_codeWithScope() throws Exception {
		// 0x0F: read int32 len, then skip(len-4) bytes
		try (var is = openIs(cat(le4(8), new byte[]{0x01, 0x02, 0x03, 0x04, 0x42}))) {
			is.skipValue(0x0F);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e16_skipValue_int32() throws Exception {
		try (var is = openIs(cat(le4(0), new byte[]{0x42}))) {
			is.skipValue(0x10);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e17_skipValue_timestamp() throws Exception {
		try (var is = openIs(cat(le8(0L), new byte[]{0x42}))) {
			is.skipValue(0x11);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e18_skipValue_int64() throws Exception {
		try (var is = openIs(cat(le8(0L), new byte[]{0x42}))) {
			is.skipValue(0x12);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e19_skipValue_decimal128() throws Exception {
		try (var is = openIs(cat(new byte[16], new byte[]{0x42}))) {
			is.skipValue(0x13);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e20_skipValue_minKey() throws Exception {
		// 0x7F: zero bytes
		try (var is = openIs(new byte[]{0x42})) {
			is.skipValue(0x7F);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e21_skipValue_maxKey() throws Exception {
		// 0xFF: zero bytes
		try (var is = openIs(new byte[]{0x42})) {
			is.skipValue(0xFF);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void e22_skipValue_unknown() throws Exception {
		try (var is = openIs(new byte[0])) {
			var ex = assertThrows(IOException.class, () -> is.skipValue(0x55));
			assertTrue(ex.getMessage().contains("Unknown BSON type"));
		}
	}

	@Test
	void e23_skipValue_eofInPayload() throws Exception {
		// skip(8) on empty stream should fail
		try (var is = openIs(new byte[0])) {
			assertThrows(IOException.class, () -> is.skipValue(0x01));
		}
	}

	// ====================================================================
	// Element-name boundary cases
	// ====================================================================

	@Test
	void f01_readElementName_empty() throws Exception {
		// Empty cstring: just a 0x00 byte
		try (var is = openIs(new byte[]{0x00})) {
			assertEquals("", is.readElementName());
		}
	}

	@Test
	void f02_readElementName_long() throws Exception {
		var name = "a".repeat(100);
		try (var is = openIs(cstring(name))) {
			assertEquals(name, is.readElementName());
		}
	}

	// ====================================================================
	// Composite scenario: parse a hand-built document
	// ====================================================================

	@Test
	void g01_parseEmptyDocumentBytes() throws Exception {
		// Empty doc: size(5) + terminator(0x00)
		var bytes = cat(le4(5), new byte[]{0x00});
		try (var is = openIs(bytes)) {
			assertEquals(5, is.readDocumentSize());
			assertTrue(is.isDocumentEnd());
			is.readDocumentTerminator();
		}
	}

	@Test
	void g02_parseSingleElementDoc() throws Exception {
		// {"x": 42}
		var elem = cat(new byte[]{0x10}, cstring("x"), le4(42));
		var docBody = cat(elem, new byte[]{0x00});
		var size = docBody.length + 4;
		var doc = cat(le4(size), docBody);
		try (var is = openIs(doc)) {
			assertEquals(size, is.readDocumentSize());
			assertFalse(is.isDocumentEnd());
			assertEquals(0x10, is.readElementType());
			assertEquals("x", is.readElementName());
			assertEquals(42, is.readInt32());
			assertTrue(is.isDocumentEnd());
		}
	}

	@Test
	void g03_deeplyNestedSkip() throws Exception {
		// Use serializer to produce a real nested doc, then verify skipValue can traverse it.
		// Treat the entire serialized doc (which starts with its own int32 size) as an embedded
		// document type (0x03) to verify skipValue can consume the whole thing.
		var s = BsonSerializer.create().keepNullProperties().build();
		var nested = JsonMap.of("level", JsonMap.of("level", JsonMap.of("level", JsonMap.of("v", 1))));
		var docBytes = s.serialize(nested);
		// Append a sentinel after the doc to verify skipValue stops at the right offset
		var withSentinel = cat(docBytes, new byte[]{0x42});
		try (var is = openIs(withSentinel)) {
			is.skipValue(0x03);
			assertEquals(0x42, is.readElementType());
		}
	}

	@Test
	void g04_largeDocFullParse() throws Exception {
		// Build a doc with several elements to exercise multiple branches
		var elements = new ArrayList<byte[]>();
		// "i": int32(123)
		elements.add(cat(new byte[]{0x10}, cstring("i"), le4(123)));
		// "d": double(2.5)
		elements.add(cat(new byte[]{0x01}, cstring("d"), le8(Double.doubleToLongBits(2.5))));
		// "s": string("hi")
		elements.add(cat(new byte[]{0x02}, cstring("s"), bsonString("hi")));
		// "b": bool(true)
		elements.add(cat(new byte[]{0x08}, cstring("b"), new byte[]{0x01}));
		// "n": null
		elements.add(cat(new byte[]{0x0A}, cstring("n")));
		// "l": int64(99999999999L)
		elements.add(cat(new byte[]{0x12}, cstring("l"), le8(99999999999L)));

		var bodyParts = new ArrayList<byte[]>(elements);
		bodyParts.add(new byte[]{0x00}); // terminator
		var body = cat(bodyParts.toArray(new byte[0][]));
		var doc = cat(le4(body.length + 4), body);

		try (var is = openIs(doc)) {
			is.readDocumentSize();
			// Walk and skip each element
			int count = 0;
			while (!is.isDocumentEnd()) {
				var t = is.readElementType();
				is.readElementName();
				is.skipValue(t);
				count++;
			}
			assertEquals(6, count);
			is.readDocumentTerminator();
		}
	}
}
