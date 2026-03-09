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
import java.time.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonSerializer}.
 */
class BsonSerializer_Test extends TestBase {

	@Test
	void a01_serializeMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("a", 1, "b", "foo"));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a02_serializeList() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(List.of(1, 2, 3));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a03_serializeToOutputStream() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var out = new ByteArrayOutputStream();
		s.serialize(JsonMap.of("x", 42), out);
		var bytes = out.toByteArray();
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a04_serializeBigDecimalAsDecimal128() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var value = new BigDecimal("123.45");
		var bytes = s.serialize(value);
		var result = p.parse(bytes, BigDecimal.class);
		assertNotNull(result);
		assertEquals(0, value.compareTo(result), "BigDecimal round-trip via Decimal128");
	}

	@Test
	void a05_serializeScalarWrapsInValue() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize("hello");
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertTrue(parsed.containsKey("value"));
		assertEquals("hello", parsed.get("value"));
	}

	@Test
	void a06_serializeDateAsDatetime() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var instant = Instant.ofEpochMilli(1700000000000L);
		var bytes = s.serialize(JsonMap.of("ts", instant));
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals(1700000000000L, parsed.get("ts"));
	}

	@Test
	void a07_serializeByteArray() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var data = new byte[] { 0x01, 0x02, 0x03 };
		var bytes = s.serialize(JsonMap.of("data", data));
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertArrayEquals(data, (byte[])parsed.get("data"));
	}

	@Test
	void a08_serializeEnum() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("size", Size.LARGE));
		var p = BsonParser.create().build();
		var parsed = p.parse(bytes, JsonMap.class);
		assertEquals("LARGE", parsed.get("size"));
	}

	enum Size { SMALL, MEDIUM, LARGE }
}
