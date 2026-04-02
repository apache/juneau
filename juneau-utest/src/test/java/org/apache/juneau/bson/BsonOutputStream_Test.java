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

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.JsonMap;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonOutputStream}.
 */
@SuppressWarnings({
	"resource" // BsonOutputStream is intentionally not closed in unit tests
})
class BsonOutputStream_Test extends TestBase {

	@Test
	void a01_emptyDocument() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		assertTrue(bytes.length >= 5); // 4 bytes size + at least 1 terminator
		assertEquals(0x00, bytes[4]); // Document terminator
	}

	@Test
	void a02_simpleKeyValueDocument() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x02, "name"); // STRING type
		bson.writeString("foo");
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		assertTrue(bytes.length > 10);
	}

	@Test
	void a03_writePrimitives() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x10, "i"); // INT32
		bson.writeInt32(42);
		bson.writeElement(0x12, "l"); // INT64
		bson.writeInt64(12345L);
		bson.writeElement(0x01, "d"); // DOUBLE
		bson.writeDouble(3.14);
		bson.writeElement(0x08, "b"); // BOOLEAN
		bson.writeBoolean(true);
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		assertTrue(bytes.length > 20);
	}

	@Test
	void a04_writeDecimal128() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x13, "dec"); // DECIMAL128
		bson.writeDecimal128(new BigDecimal("123.45"));
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		assertTrue(bytes.length >= 26); // size(4) + type(1) + "dec\0"(4) + 16 bytes + terminator(1)
	}

	@Test
	void a05_roundTripViaSerializer() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var map = JsonMap.of("a", 1, "b", "foo", "c", true);
		var bytes = s.serialize(map);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(1, parsed.getInt("a"));
		assertEquals("foo", parsed.getString("b"));
		assertEquals(true, parsed.getBoolean("c"));
	}

	@Test
	void a06_writeInt64Boundary() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x12, "max");
		bson.writeInt64(Long.MAX_VALUE);
		bson.writeElement(0x12, "min");
		bson.writeInt64(Long.MIN_VALUE);
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(Long.MAX_VALUE, parsed.get("max"));
		assertEquals(Long.MIN_VALUE, parsed.get("min"));
	}

	@Test
	void a07_writeBooleanFalse() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x08, "flag");
		bson.writeBoolean(false);
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(false, parsed.get("flag"));
	}

	@Test
	void a08_writeBinary() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x05, "data");
		bson.writeBinary(new byte[] { 0x01, 0x02, 0x03 });
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertArrayEquals(new byte[] { 0x01, 0x02, 0x03 }, (byte[])parsed.get("data"));
	}

	@Test
	void a09_writeDateTime() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x09, "ts");
		bson.writeDateTime(1700000000000L);
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(1700000000000L, parsed.get("ts"));
	}

	@Test
	void a10_nestedDocument() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x02, "name");
		bson.writeString("outer");
		bson.writeElement(0x03, "inner");
		var child = bson.createChild();
		child.startDocument();
		child.writeElement(0x10, "x");
		child.writeInt32(99);
		bson.writeChildDocument(child);
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals("outer", parsed.get("name"));
		var inner = (java.util.Map<?,?>)parsed.get("inner");
		assertNotNull(inner);
		assertEquals(99, inner.get("x"));
	}

	@Test
	void a11_emptyString() throws Exception {
		var out = new ByteArrayOutputStream();
		var bson = new BsonOutputStream(out);
		bson.startDocument();
		bson.writeElement(0x02, "s");
		bson.writeString("");
		var bytes = bson.endDocument();
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals("", parsed.get("s"));
	}
}
