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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CborSerializer}.
 */
class CborSerializer_Test extends TestBase {

	private static void test(Object input, String expected) throws Exception {
		var b = CborSerializer.DEFAULT.serialize(input);
		assertEquals(expected, toSpacedHex(b));
	}

	@Test
	void a01_rfc8949Compliance() throws Exception {
		test(null, "F6");
		test(false, "F4");
		test(true, "F5");
		test(0, "00");
		test(1, "01");
		test(23, "17");
		test(24, "18 18");
		test(100, "18 64");
		test(1000, "19 03 E8");
		test(1000000, "1A 00 0F 42 40");
		test(-1, "20");
		test(-100, "38 63");
		test(0.0f, "FA 00 00 00 00");
		test(1.1, "FB 3F F1 99 99 99 99 99 9A");
		test("", "60");
		test("a", "61 61");
		test(ints(), "80");
		test(ints(1, 2, 3), "83 01 02 03");
		test(JsonMap.ofJson("{}"), "A0");
	}

	public static class Person {
		public String name = "John Smith";
		public int age = 21;
		public Person() {}
		public Person(String name, int age) { this.name = name; this.age = age; }
	}

	@Test
	void a02_simpleBean() throws Exception {
		var a = new Person();
		var bytes = CborSerializer.DEFAULT.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		var parsed = CborParser.DEFAULT.parse(bytes, Person.class);
		assertEquals(a.name, parsed.name);
		assertEquals(a.age, parsed.age);
	}

	@Test
	void a03_roundTrip() throws Exception {
		var a = new Person("Alice", 30);
		var bytes = CborSerializer.DEFAULT.serialize(a);
		var b = CborParser.DEFAULT.parse(bytes, Person.class);
		assertEquals("Alice", b.name);
		assertEquals(30, b.age);
	}

	@Test
	void c01_simpleBean() throws Exception {
		var a = new Bean1("x", 42, true);
		var bytes = CborSerializer.DEFAULT.serialize(a);
		var b = CborParser.DEFAULT.parse(bytes, Bean1.class);
		assertEquals("x", b.s);
		assertEquals(42, b.i);
		assertTrue(b.b);
	}

	@Test
	void c02_nestedBean() throws Exception {
		var a = new Bean2("outer", new Bean1("inner", 1, false));
		var bytes = CborSerializer.DEFAULT.serialize(a);
		var b = CborParser.DEFAULT.parse(bytes, Bean2.class);
		assertEquals("outer", b.name);
		assertEquals("inner", b.child.s);
		assertEquals(1, b.child.i);
		assertFalse(b.child.b);
	}

	@Test
	void c03_collectionOfBeans() throws Exception {
		var list = list(new Bean1("a", 1, true), new Bean1("b", 2, false));
		var bytes = CborSerializer.DEFAULT.serialize(list);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonList.class);
		assertEquals(2, parsed.size());
		assertEquals("a", parsed.getMap(0).getString("s"));
		assertEquals("b", parsed.getMap(1).getString("s"));
	}

	@Test
	void c04_mapProperty() throws Exception {
		var m = JsonMap.of("x", 1, "y", "z");
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertEquals(1, parsed.getInt("x"));
		assertEquals("z", parsed.getString("y"));
	}

	@Test
	void c05_nullValues() throws Exception {
		var m = JsonMap.of("a", 1, "b", null, "c", "x");
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertEquals(1, parsed.getInt("a"));
		assertNull(parsed.get("b"));
		assertEquals("x", parsed.getString("c"));
	}

	@Test
	void c06_booleanValues() throws Exception {
		test(false, "F4");
		test(true, "F5");
	}

	@Test
	void c07_integerValues() throws Exception {
		test(0, "00");
		test(255, "18 FF");
		test(256, "19 01 00");
		test(-1, "20");
		test(-255, "38 FE");
	}

	@Test
	void c08_floatValues() throws Exception {
		test(0.0f, "FA 00 00 00 00");
		test(1.5, "FB 3F F8 00 00 00 00 00 00");
	}

	@Test
	void c09_stringValues() throws Exception {
		test("", "60");
		test("hello", "65 68 65 6C 6C 6F");
	}

	@Test
	void c10_binaryValues() throws Exception {
		var data = new byte[] { 1, 2, 3 };
		var bytes = CborSerializer.DEFAULT.serialize(data);
		assertEquals("43 01 02 03", toSpacedHex(bytes));
		var parsed = CborParser.DEFAULT.parse(bytes, byte[].class);
		assertArrayEquals(data, parsed);
	}

	@Test
	void c11_enumValues() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(Size.LARGE);
		assertEquals("65 4C 41 52 47 45", toSpacedHex(bytes));
		var parsed = CborParser.DEFAULT.parse(bytes, Size.class);
		assertEquals(Size.LARGE, parsed);
	}

	@Test
	void c12_objectSwaps() throws Exception {
		var s = CborSerializer.create().swaps(LowercaseStringSwap.class).build();
		var bytes = s.serialize("SWAPPED");
		var p = CborParser.create().swaps(LowercaseStringSwap.class).build();
		var parsed = p.parse(bytes, String.class);
		assertEquals("swapped", parsed);
	}

	@Test
	void c13_emptyBean() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(JsonMap.ofJson("{}"));
		assertEquals("A0", toSpacedHex(bytes));
		var parsed = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertTrue(parsed.isEmpty());
	}

	@Test
	void c14_emptyCollections() throws Exception {
		test(ints(), "80");
		test(JsonMap.ofJson("{}"), "A0");
	}

	@Test
	void c15_typeName() throws Exception {
		var s = CborSerializer.create().addBeanTypesCbor().addRootType().keepNullProperties().build();
		var bytes = s.serialize(new Bean1("x", 1, true));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		var parsed = CborParser.DEFAULT.parse(bytes, Bean1.class);
		assertEquals("x", parsed.s);
		assertEquals(1, parsed.i);
	}

	@Test
	void c16_deeplyNestedBean() throws Exception {
		var a = new NestedBean("a", new NestedBean("b", new NestedBean("c", null)));
		var bytes = CborSerializer.DEFAULT.serialize(a);
		var b = CborParser.DEFAULT.parse(bytes, NestedBean.class);
		assertEquals("a", b.name);
		assertEquals("b", b.child.name);
		assertEquals("c", b.child.child.name);
		assertNull(b.child.child.child);
	}

	@Test
	void c17_collectionOfStrings() throws Exception {
		test(list("a", "b", "c"), "83 61 61 61 62 61 63");
	}

	@Test
	void c18_collectionOfNumbers() throws Exception {
		var list = list(1, 2.5, 3);
		var bytes = CborSerializer.DEFAULT.serialize(list);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonList.class);
		assertEquals(3, parsed.size());
		assertEquals(1, parsed.getInt(0));
		assertEquals(2.5, ((Number)parsed.get(1)).doubleValue(), 0.001);
		assertEquals(3, parsed.getInt(2));
	}

	@Test
	void c19_spacedHexOutput() throws Exception {
		var s = CborSerializer.SpacedHex.DEFAULT;
		var out = s.serialize(JsonMap.of("a", 1));
		assertNotNull(out);
		assertTrue(out.length > 0);
		var p = CborParser.SpacedHex.DEFAULT;
		var parsed = p.parse(out, JsonMap.class);
		assertEquals(1, parsed.getInt("a"));
	}

	@Test
	void c20_base64Output() throws Exception {
		var s = CborSerializer.Base64.DEFAULT;
		var out = s.serialize(JsonMap.of("a", 1));
		assertNotNull(out);
		assertTrue(out.length > 0);
		var p = CborParser.Base64.DEFAULT;
		var parsed = p.parse(out, JsonMap.class);
		assertEquals(1, parsed.getInt("a"));
	}

	public static class Bean1 {
		public String s;
		public int i;
		public boolean b;
		public Bean1() {}
		public Bean1(String s, int i, boolean b) { this.s = s; this.i = i; this.b = b; }
	}

	public static class Bean2 {
		public String name;
		public Bean1 child;
		public Bean2() {}
		public Bean2(String name, Bean1 child) { this.name = name; this.child = child; }
	}

	public static class NestedBean {
		public String name;
		public NestedBean child;
		public NestedBean() {}
		public NestedBean(String name, NestedBean child) { this.name = name; this.child = child; }
	}


	enum Size { SMALL, MEDIUM, LARGE }

	public static class LowercaseStringSwap extends ObjectSwap<String,String> {
		@Override
		public String swap(BeanSession session, String o) { return o == null ? null : o.toLowerCase(); }
		@Override
		public String unswap(BeanSession session, String o, ClassMeta<?> hint, String attrName) { return o; }
	}
}
