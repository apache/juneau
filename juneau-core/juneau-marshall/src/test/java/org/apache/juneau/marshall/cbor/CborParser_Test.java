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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CborParser}.
 */
class CborParser_Test extends TestBase {

	@Test
	void d01_readSimpleBean() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(JsonMap.of("name", "Alice", "age", 30));
		var bean = CborParser.DEFAULT.read(bytes, Person.class);
		assertEquals("Alice", bean.name);
		assertEquals(30, bean.age);
	}

	@Test
	void d02_readNestedBean() throws Exception {
		var outer = JsonMap.of("name", "outer", "child", JsonMap.of("s", "inner", "i", 1, "b", false));
		var bytes = CborSerializer.DEFAULT.write(outer);
		var bean = CborParser.DEFAULT.read(bytes, CborSerializer_Test.Bean2.class);
		assertEquals("outer", bean.name);
		assertEquals("inner", bean.child.s);
		assertEquals(1, bean.child.i);
		assertFalse(bean.child.b);
	}

	@Test
	void d03_readCollectionOfBeans() throws Exception {
		var list = list(
			JsonMap.of("s", "a", "i", 1, "b", true),
			JsonMap.of("s", "b", "i", 2, "b", false));
		var bytes = CborSerializer.DEFAULT.write(list);
		var parsed = CborParser.DEFAULT.read(bytes, JsonList.class);
		assertEquals(2, parsed.size());
		assertEquals("a", parsed.getMap(0).getString("s"));
		assertEquals("b", parsed.getMap(1).getString("s"));
	}

	@Test
	void d04_readMapProperty() throws Exception {
		var m = JsonMap.of("data", JsonMap.of("x", 1, "y", 2));
		var bytes = CborSerializer.DEFAULT.write(m);
		var parsed = CborParser.DEFAULT.read(bytes, JsonMap.class);
		var data = parsed.getMap("data");
		assertEquals(1, data.getInt("x"));
		assertEquals(2, data.getInt("y"));
	}

	@Test
	void d05_readNullValues() throws Exception {
		var m = JsonMap.of("a", 1, "b", null);
		var bytes = CborSerializer.DEFAULT.write(m);
		var parsed = CborParser.DEFAULT.read(bytes, JsonMap.class);
		assertNull(parsed.get("b"));
	}

	@Test
	void d06_readBooleans() throws Exception {
		assertEquals(false, CborParser.DEFAULT.read(fromHex("F4"), Boolean.class));
		assertEquals(true, CborParser.DEFAULT.read(fromHex("F5"), Boolean.class));
	}

	@Test
	void d07_readIntegers() throws Exception {
		assertEquals(100, CborParser.DEFAULT.read(CborSerializer.DEFAULT.write(100), Integer.class));
		assertEquals(1000000L, CborParser.DEFAULT.read(CborSerializer.DEFAULT.write(1000000), Long.class));
	}

	@Test
	void d08_readNegativeIntegers() throws Exception {
		assertEquals(-1, CborParser.DEFAULT.read(fromHex("20"), Integer.class));
		assertEquals(-100, CborParser.DEFAULT.read(fromHex("3863"), Integer.class));
	}

	@Test
	void d09_readFloats() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(1.5);
		assertEquals(1.5, CborParser.DEFAULT.read(bytes, Double.class), 0.001);
	}

	@Test
	void d10_readStrings() throws Exception {
		assertEquals("hello", CborParser.DEFAULT.read(CborSerializer.DEFAULT.write("hello"), String.class));
	}

	@Test
	void d11_readBinary() throws Exception {
		var data = new byte[] { 1, 2, 3 };
		var bytes = CborSerializer.DEFAULT.write(data);
		assertArrayEquals(data, CborParser.DEFAULT.read(bytes, byte[].class));
	}

	@Test
	void d12_readEnums() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(CborSerializer_Test.Size.LARGE);
		assertEquals(CborSerializer_Test.Size.LARGE, CborParser.DEFAULT.read(bytes, CborSerializer_Test.Size.class));
	}

	@Test
	void d14_readEmptyStructures() throws Exception {
		var emptyList = CborParser.DEFAULT.read(fromHex("80"), JsonList.class);
		assertTrue(emptyList.isEmpty());
		var emptyMap = CborParser.DEFAULT.read(fromHex("A0"), JsonMap.class);
		assertTrue(emptyMap.isEmpty());
	}

	@Test
	void d16_spacedHexInput() throws Exception {
		assertEquals("v", CborParser.DEFAULT_SPACED_HEX.read(
			CborSerializer.DEFAULT_SPACED_HEX.write(JsonMap.of("k", "v")), JsonMap.class).getString("k"));
	}

	@Test
	void d17_base64Input() throws Exception {
		assertEquals("v", CborParser.DEFAULT_BASE64.read(
			CborSerializer.DEFAULT_BASE64.write(JsonMap.of("k", "v")), JsonMap.class).getString("k"));
	}

	@Test
	void d18_readTag() throws Exception {
		var tagged = fromHex("C074323032332D30312D30315431323A30303A30305A");
		var parsed = CborParser.DEFAULT.read(tagged, String.class);
		assertEquals("2023-01-01T12:00:00Z", parsed);
	}

	@Test
	void d20_readFromInputStream() throws Exception {
		var bytes = CborSerializer.DEFAULT.write(JsonMap.of("x", 1));
		try (var is = new ByteArrayInputStream(bytes)) {
			var m = CborParser.DEFAULT.read(is, JsonMap.class);
			assertEquals(1, m.getInt("x"));
		}
	}

	public static class Person {
		public String name;
		public int age;
	}
}
