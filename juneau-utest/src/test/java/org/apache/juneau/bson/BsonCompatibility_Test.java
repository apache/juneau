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

import java.math.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Compatibility tests for BSON: verifies parsing of BSON bytes and round-trip
 * with standard BSON structure.
 */
class BsonCompatibility_Test extends TestBase {

	@Test
	void a01_parseMinimalDocument() throws Exception {
		// BSON: { } - size(4)=5, type+name+terminator, doc terminator
		// 05 00 00 00 00
		var bytes = new byte[] { 0x05, 0x00, 0x00, 0x00, 0x00 };
		var p = BsonParser.create().build();
		var result = p.parse(bytes, JsonMap.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void a02_parseDocumentWithString() throws Exception {
		// BSON: { "a": "foo" } - size includes the 4-byte size field
		// Content: 1(type) + 2("a\0") + 4(len) + 4("foo\0") + 1(doc end) = 12
		// Size = 4 + 12 = 16
		var bytes = new byte[] {
			0x10, 0x00, 0x00, 0x00,  // size 16
			0x02, 0x61, 0x00,        // 0x02 + "a\0"
			0x04, 0x00, 0x00, 0x00,  // string length 4 (incl null)
			0x66, 0x6f, 0x6f, 0x00,  // "foo\0"
			0x00                    // doc terminator
		};
		var p = BsonParser.create().build();
		var result = p.parse(bytes, JsonMap.class);
		assertNotNull(result);
		assertEquals("foo", result.get("a"));
	}

	@Test
	void a03_roundTripBigDecimalViaDecimal128() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var value = new BigDecimal("1.234567890123456789012345678901234");
		var bytes = s.serialize(JsonMap.of("d", value));
		var parsed = p.parse(bytes, JsonMap.class);
		var roundTrip = (BigDecimal) parsed.get("d");
		assertNotNull(roundTrip);
		assertEquals(0, value.compareTo(roundTrip));
	}

	@Test
	void a03b_roundTripBigIntegerViaDecimal128() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var value = new BigInteger("12345678901234567890");
		var bytes = s.serialize(value);
		var roundTrip = p.parse(bytes, BigInteger.class);
		assertNotNull(roundTrip);
		assertEquals(value, roundTrip);
	}

	@Test
	void a04_serializeParseProduceConsistentStructure() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var original = JsonMap.of("x", 1, "y", "text", "z", true);
		var bytes = s.serialize(original);
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(original.get("x"), parsed.get("x"));
		assertEquals(original.get("y"), parsed.get("y"));
		assertEquals(original.get("z"), parsed.get("z"));
	}

	@Test
	void j01_emptyDocumentBytes() throws Exception {
		var expected = new byte[] { 0x05, 0x00, 0x00, 0x00, 0x00 };
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of());
		assertArrayEquals(expected, bytes);
	}

	@Test
	void j02_helloWorldBytes() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("hello", "world"));
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals("world", parsed.get("hello"));
		assertTrue(bytes.length >= 22, "BSON {hello:world} should be at least 22 bytes");
	}

	@Test
	void j03_int32Bytes() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("n", 42));
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(42, parsed.get("n"));
	}
}
