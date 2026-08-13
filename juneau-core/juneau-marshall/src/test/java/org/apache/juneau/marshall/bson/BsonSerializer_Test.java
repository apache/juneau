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
package org.apache.juneau.marshall.bson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonSerializer}.
 */
class BsonSerializer_Test extends TestBase {

	@Test
	void a01_writeMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.write(JsonMap.of("a", 1, "b", "foo"));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a02_writeList() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.write(List.of(1, 2, 3));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a03_writeToOutputStream() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var out = new ByteArrayOutputStream();
		s.write(JsonMap.of("x", 42), out);
		var bytes = out.toByteArray();
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a04_writeBigDecimalAsDecimal128() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var value = new BigDecimal("123.45");
		var bytes = s.write(value);
		var result = p.read(bytes, BigDecimal.class);
		assertNotNull(result);
		assertEquals(0, value.compareTo(result), "BigDecimal round-trip via Decimal128");
	}

	@Test
	void a05_writeScalarWrapsInValue() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.write("hello");
		assertNotNull(bytes);
		var p = BsonParser.create().build();
		var parsed = p.read(bytes, JsonMap.class);
		assertTrue(parsed.containsKey("value"));
		assertEquals("hello", parsed.get("value"));
	}

	@Test
	void a06_writeDateAsDatetime() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var instant = Instant.ofEpochMilli(1700000000000L);
		var bytes = s.write(JsonMap.of("ts", instant));
		var p = BsonParser.create().build();
		var parsed = p.read(bytes, JsonMap.class);
		assertEquals(1700000000000L, parsed.get("ts"));
	}

	@Test
	void a07_writeByteArray() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var data = new byte[] { 0x01, 0x02, 0x03 };
		var bytes = s.write(JsonMap.of("data", data));
		var p = BsonParser.create().build();
		var parsed = p.read(bytes, JsonMap.class);
		assertArrayEquals(data, (byte[])parsed.get("data"));
	}

	@Test
	void a08_writeEnum() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.write(JsonMap.of("size", Size.LARGE));
		var p = BsonParser.create().build();
		var parsed = p.read(bytes, JsonMap.class);
		assertEquals("LARGE", parsed.get("size"));
	}

	@Test
	void a09_writeByteArraySpacedHexHonorsBinaryFormat() throws Exception {
		var bean = new BeanWithBytes();
		var defaultOut = BsonSerializer.DEFAULT.write(bean);
		var spacedHexOut = BsonSerializer.DEFAULT_SPACED_HEX.write(bean);
		assertFalse(Arrays.equals(defaultOut, spacedHexOut), "SpacedHex output should differ from the native binary output");
		var parsed = BsonParser.DEFAULT_SPACED_HEX.read(spacedHexOut, BeanWithBytes.class);
		assertArrayEquals(bean.data, parsed.data);
	}

	@Test
	void a10_writeByteArrayBase64HonorsBinaryFormat() throws Exception {
		var bean = new BeanWithBytes();
		var defaultOut = BsonSerializer.DEFAULT.write(bean);
		var base64Out = BsonSerializer.DEFAULT_BASE64.write(bean);
		assertFalse(Arrays.equals(defaultOut, base64Out), "Base64 output should differ from the native binary output");
		var parsed = BsonParser.DEFAULT_BASE64.read(base64Out, BeanWithBytes.class);
		assertArrayEquals(bean.data, parsed.data);
	}

	@Test
	void a11_writeByteArrayNotSetKeepsNativeBinary() throws Exception {
		// NOT_SET (the default on BsonSerializer.DEFAULT) is unaffected by the SpacedHex/Base64 fix -- byte[]
		// values still use BSON's native binary element (subtype 0x05) rather than a string encoding.
		var bean = new BeanWithBytes();
		var s = BsonSerializer.create().keepNullProperties().binaryFormat(BinaryFormat.NOT_SET).build();
		var bytes = s.write(bean);
		var p = BsonParser.create().build();
		var parsed = p.read(bytes, JsonMap.class);
		assertArrayEquals(bean.data, (byte[])parsed.get("data"));
	}

	/** Bean with a single byte[] property, used to exercise the MarshalledPropertyPostProcessor bean-property path. */
	public static class BeanWithBytes {
		public byte[] data = { 0x01, 0x02, 0x03 };
	}

	enum Size { SMALL, MEDIUM, LARGE }
}
