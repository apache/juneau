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
package org.apache.juneau.cbor;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CborParser}.
 */
class CborParser_Test extends TestBase {

	@Test
	void d01_parseSimpleBean() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(JsonMap.of("name", "Alice", "age", 30));
		var bean = CborParser.DEFAULT.parse(bytes, Person.class);
		assertEquals("Alice", bean.name);
		assertEquals(30, bean.age);
	}

	@Test
	void d02_parseNestedBean() throws Exception {
		var outer = JsonMap.of("name", "outer", "child", JsonMap.of("s", "inner", "i", 1, "b", false));
		var bytes = CborSerializer.DEFAULT.serialize(outer);
		var bean = CborParser.DEFAULT.parse(bytes, CborSerializer_Test.Bean2.class);
		assertEquals("outer", bean.name);
		assertEquals("inner", bean.child.s);
		assertEquals(1, bean.child.i);
		assertFalse(bean.child.b);
	}

	@Test
	void d03_parseCollectionOfBeans() throws Exception {
		var list = list(
			JsonMap.of("s", "a", "i", 1, "b", true),
			JsonMap.of("s", "b", "i", 2, "b", false));
		var bytes = CborSerializer.DEFAULT.serialize(list);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonList.class);
		assertEquals(2, parsed.size());
		assertEquals("a", parsed.getMap(0).getString("s"));
		assertEquals("b", parsed.getMap(1).getString("s"));
	}

	@Test
	void d04_parseMapProperty() throws Exception {
		var m = JsonMap.of("data", JsonMap.of("x", 1, "y", 2));
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		var data = parsed.getMap("data");
		assertEquals(1, data.getInt("x"));
		assertEquals(2, data.getInt("y"));
	}

	@Test
	void d05_parseNullValues() throws Exception {
		var m = JsonMap.of("a", 1, "b", null);
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertNull(parsed.get("b"));
	}

	@Test
	void d06_parseBooleans() throws Exception {
		assertEquals(false, CborParser.DEFAULT.parse(fromHex("F4"), Boolean.class));
		assertEquals(true, CborParser.DEFAULT.parse(fromHex("F5"), Boolean.class));
	}

	@Test
	void d07_parseIntegers() throws Exception {
		assertEquals(100, CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize(100), Integer.class));
		assertEquals(1000000L, CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize(1000000), Long.class));
	}

	@Test
	void d08_parseNegativeIntegers() throws Exception {
		assertEquals(-1, CborParser.DEFAULT.parse(fromHex("20"), Integer.class));
		assertEquals(-100, CborParser.DEFAULT.parse(fromHex("3863"), Integer.class));
	}

	@Test
	void d09_parseFloats() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(1.5);
		assertEquals(1.5, CborParser.DEFAULT.parse(bytes, Double.class), 0.001);
	}

	@Test
	void d10_parseStrings() throws Exception {
		assertEquals("hello", CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize("hello"), String.class));
	}

	@Test
	void d11_parseBinary() throws Exception {
		var data = new byte[] { 1, 2, 3 };
		var bytes = CborSerializer.DEFAULT.serialize(data);
		assertArrayEquals(data, CborParser.DEFAULT.parse(bytes, byte[].class));
	}

	@Test
	void d12_parseEnums() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(CborSerializer_Test.Size.LARGE);
		assertEquals(CborSerializer_Test.Size.LARGE, CborParser.DEFAULT.parse(bytes, CborSerializer_Test.Size.class));
	}

	@Test
	void d14_parseEmptyStructures() throws Exception {
		var emptyList = CborParser.DEFAULT.parse(fromHex("80"), JsonList.class);
		assertTrue(emptyList.isEmpty());
		var emptyMap = CborParser.DEFAULT.parse(fromHex("A0"), JsonMap.class);
		assertTrue(emptyMap.isEmpty());
	}

	@Test
	void d16_spacedHexInput() throws Exception {
		assertEquals("v", CborParser.DEFAULT_SPACED_HEX.parse(
			CborSerializer.DEFAULT_SPACED_HEX.serialize(JsonMap.of("k", "v")), JsonMap.class).getString("k"));
	}

	@Test
	void d17_base64Input() throws Exception {
		assertEquals("v", CborParser.DEFAULT_BASE64.parse(
			CborSerializer.DEFAULT_BASE64.serialize(JsonMap.of("k", "v")), JsonMap.class).getString("k"));
	}

	@Test
	void d18_parseTag() throws Exception {
		var tagged = fromHex("C074323032332D30312D30315431323A30303A30305A");
		var parsed = CborParser.DEFAULT.parse(tagged, String.class);
		assertEquals("2023-01-01T12:00:00Z", parsed);
	}

	@Test
	void d20_parseFromInputStream() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(JsonMap.of("x", 1));
		try (var is = new ByteArrayInputStream(bytes)) {
			var m = CborParser.DEFAULT.parse(is, JsonMap.class);
			assertEquals(1, m.getInt("x"));
		}
	}

	public static class Person {
		public String name;
		public int age;
	}
}
