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
package org.apache.juneau.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarkdownSerializer} and {@link MarkdownSerializerSession} (fragment mode).
 */
class MarkdownSerializer_Test {

	//====================================================================================================
	// a - Serialize bean as key/value table
	//====================================================================================================

	@Test void a01_serializeFlatBean() throws Exception {
		var bean = new A();
		bean.name = "Alice";
		bean.age = 30;
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("| Property | Value |"), "Expected header row: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name row: " + md);
		assertTrue(md.contains("| age | 30 |"), "Expected age row: " + md);
	}

	@Test void a02_serializeBeanWithNullValue() throws Exception {
		var bean = new A();
		bean.name = null;
		bean.age = 30;
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("*null*"), "Expected null marker in: " + md);
	}

	@Test void a03_serializeBeanWithCustomNullValue() throws Exception {
		var s = MarkdownSerializer.create().nullValue("N/A").build();
		var bean = new A();
		bean.name = null;
		bean.age = 30;
		var md = s.serialize(bean);
		assertTrue(md.contains("N/A"), "Expected custom null marker in: " + md);
	}

	public static class A {
		public String name;
		public int age;
	}

	//====================================================================================================
	// b - Serialize map as key/value table
	//====================================================================================================

	@Test void b01_serializeStringMap() throws Exception {
		var m = new LinkedHashMap<String, String>();
		m.put("k1", "v1");
		m.put("k2", "v2");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertTrue(md.contains("| Key | Value |"), "Expected header row: " + md);
		assertTrue(md.contains("| k1 | v1 |"), "Expected k1 row: " + md);
		assertTrue(md.contains("| k2 | v2 |"), "Expected k2 row: " + md);
	}

	//====================================================================================================
	// c - Serialize uniform collection as multi-column table
	//====================================================================================================

	@Test void c01_serializeBeanListAsMultiColumnTable() throws Exception {
		var list = List.of(new B("Alice", 30), new B("Bob", 25));
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		// Multi-column table with bean properties as headers
		assertTrue(md.contains("|---"), "Expected separator row: " + md);
		// Alice and Bob should appear as rows
		assertTrue(md.contains("Alice"), "Expected Alice in table: " + md);
		assertTrue(md.contains("Bob"), "Expected Bob in table: " + md);
	}

	public static class B {
		public String name;
		public int age;
		public B() {}
		public B(String name, int age) { this.name = name; this.age = age; }
	}

	//====================================================================================================
	// d - Serialize simple-value collection as bulleted list
	//====================================================================================================

	@Test void d01_serializeStringListAsBullets() throws Exception {
		var list = List.of("alpha", "beta", "gamma");
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		assertTrue(md.contains("- alpha"), "Expected bullet alpha: " + md);
		assertTrue(md.contains("- beta"), "Expected bullet beta: " + md);
		assertTrue(md.contains("- gamma"), "Expected bullet gamma: " + md);
	}

	@Test void d02_serializeIntListAsBullets() throws Exception {
		var list = List.of(1, 2, 3);
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		assertTrue(md.contains("- 1"), "Expected bullet 1: " + md);
		assertTrue(md.contains("- 2"), "Expected bullet 2: " + md);
		assertTrue(md.contains("- 3"), "Expected bullet 3: " + md);
	}

	//====================================================================================================
	// e - Nested complex values rendered as inline JSON5
	//====================================================================================================

	@Test void e01_serializeNestedBeanAsInlineJson5() throws Exception {
		var bean = new C();
		bean.name = "Alice";
		bean.nested = new A();
		bean.nested.name = "inner";
		bean.nested.age = 1;
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("`"), "Expected backtick wrapping for nested value: " + md);
	}

	public static class C {
		public String name;
		public A nested;
	}

	//====================================================================================================
	// f - Pipe character in values is escaped
	//====================================================================================================

	@Test void f01_pipeCharacterIsEscaped() throws Exception {
		var m = Map.of("desc", "hello | world");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertTrue(md.contains("\\|"), "Expected escaped pipe: " + md);
	}

	//====================================================================================================
	// g - showHeaders = false
	//====================================================================================================

	@Test void g01_noHeaders() throws Exception {
		var s = MarkdownSerializer.create().showHeaders(false).build();
		var bean = new A();
		bean.name = "Alice";
		bean.age = 30;
		var md = s.serialize(bean);
		assertFalse(md.contains("| Property | Value |"), "Did not expect header row: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name row: " + md);
	}

	//====================================================================================================
	// h - Boolean and numeric values
	//====================================================================================================

	@Test void h01_serializeBooleanAndNumericProperties() throws Exception {
		var bean = new D();
		bean.count = 42;
		bean.ratio = 3.14;
		bean.flag = true;
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("| count | 42 |"), "Expected count row: " + md);
		assertTrue(md.contains("| ratio | 3.14 |"), "Expected ratio row: " + md);
		assertTrue(md.contains("| flag | true |"), "Expected flag row: " + md);
	}

	public static class D {
		public int count;
		public double ratio;
		public boolean flag;
	}

	//====================================================================================================
	// i - Round-trip assertions (serialize then parse back)
	//====================================================================================================

	@Test void i01_roundTripFlatBean() throws Exception {
		var original = new B("Alice", 30);
		var md = MarkdownSerializer.DEFAULT.serialize(original);
		var parsed = MarkdownParser.DEFAULT.parse(md, B.class);
		assertEquals("Alice", parsed.name);
		assertEquals(30, parsed.age);
	}

	@Test
	@SuppressWarnings("unchecked")
	void i02_roundTripBeanList() throws Exception {
		var original = List.of(new B("Alice", 30), new B("Bob", 25));
		var md = MarkdownSerializer.DEFAULT.serialize(original);
		var parsed = (List<B>) MarkdownParser.DEFAULT.parse(md, List.class, B.class);
		assertEquals(2, parsed.size());
		assertEquals("Alice", parsed.get(0).name);
		assertEquals(30, parsed.get(0).age);
		assertEquals("Bob", parsed.get(1).name);
		assertEquals(25, parsed.get(1).age);
	}

	@Test
	@SuppressWarnings("unchecked")
	void i03_roundTripStringList() throws Exception {
		var original = List.of("foo", "bar", "baz");
		var md = MarkdownSerializer.DEFAULT.serialize(original);
		var parsed = (List<String>) MarkdownParser.DEFAULT.parse(md, List.class, String.class);
		assertEquals(original, parsed);
	}

	@Test
	@SuppressWarnings("unchecked")
	void i04_roundTripMap() throws Exception {
		var original = new LinkedHashMap<String, String>();
		original.put("k1", "v1");
		original.put("k2", "v2");
		var md = MarkdownSerializer.DEFAULT.serialize(original);
		var parsed = (Map<String, String>) MarkdownParser.DEFAULT.parse(md, Map.class, String.class, String.class);
		assertEquals(original, parsed);
	}
}
