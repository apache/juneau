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

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.markdown.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarkdownParser} and {@link MarkdownParserSession} (fragment mode).
 */
class MarkdownParser_Test {

	//====================================================================================================
	// a - Parse key/value table to bean
	//====================================================================================================

	@Test void a01_readKeyValueTable_toBean() {
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| age | 30 |";
		var r = MarkdownParser.DEFAULT.read(md, A.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	@Test void a02_readKeyValueTable_toMap() {
		var md = "| Property | Value |\n|---|---|\n| city | Boston |\n| state | MA |";
		var r = MarkdownParser.DEFAULT.read(md, Map.class);
		assertEquals("Boston", r.get("city"));
		assertEquals("MA", r.get("state"));
	}

	@Test
	void a03_readKeyValueTable_toObject() {
		var md = "| Key | Value |\n|---|---|\n| foo | bar |";
		var r = (MarshalledMap) MarkdownParser.DEFAULT.read(md, Object.class);
		assertEquals("bar", r.get("foo"));
	}

	public static class A {
		public String name;
		public int age;
	}

	//====================================================================================================
	// b - Parse multi-column table to list of beans
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void b01_readMultiColumnTable_toBeanList() {
		var md = "| name | age |\n|---|---|\n| Alice | 30 |\n| Bob | 25 |";
		var r = (List<B>) MarkdownParser.DEFAULT.read(md, List.class, B.class);
		assertEquals(2, r.size());
		assertEquals("Alice", r.get(0).name);
		assertEquals(30, r.get(0).age);
		assertEquals("Bob", r.get(1).name);
		assertEquals(25, r.get(1).age);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void b02_readMultiColumnTable_toMapList() {
		var md = "| key1 | key2 |\n|---|---|\n| v1 | v2 |";
		var r = (List<Map<?,?>>) MarkdownParser.DEFAULT.read(md, List.class, Map.class);
		assertEquals(1, r.size());
		assertEquals("v1", r.get(0).get("key1"));
		assertEquals("v2", r.get(0).get("key2"));
	}

	@Test void b03_readMultiColumnTable_toArray() {
		var md = "| name | age |\n|---|---|\n| Alice | 30 |";
		var r = MarkdownParser.DEFAULT.read(md, B[].class);
		assertEquals(1, r.length);
		assertEquals("Alice", r[0].name);
		assertEquals(30, r[0].age);
	}

	public static class B {
		public String name;
		public int age;
	}

	//====================================================================================================
	// c - Parse bulleted list
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void c01_readBulletList_toStringList() {
		var md = "- alpha\n- beta\n- gamma";
		var r = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(List.of("alpha", "beta", "gamma"), r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void c02_readBulletList_toIntList() {
		var md = "- 1\n- 2\n- 3";
		var r = (List<Integer>) MarkdownParser.DEFAULT.read(md, List.class, Integer.class);
		assertEquals(List.of(1, 2, 3), r);
	}

	@Test void c03_readBulletList_toStringArray() {
		var md = "- foo\n- bar";
		var r = MarkdownParser.DEFAULT.read(md, String[].class);
		assertArrayEquals(new String[]{"foo", "bar"}, r);
	}

	//====================================================================================================
	// d - Null value handling
	//====================================================================================================

	@Test void d01_readNullValue_defaultMarker() {
		var md = "| Property | Value |\n|---|---|\n| name | *null* |\n| age | 30 |";
		var r = MarkdownParser.DEFAULT.read(md, C.class);
		assertNull(r.name);
		assertEquals(30, r.age);
	}

	@Test void d02_readNullValue_customMarker() {
		var p = MarkdownParser.create().nullValue("N/A").build();
		var md = "| Property | Value |\n|---|---|\n| name | N/A |\n| age | 30 |";
		var r = p.read(md, C.class);
		assertNull(r.name);
		assertEquals(30, r.age);
	}

	@Test void d03_readEmptyInput() {
		var r = MarkdownParser.DEFAULT.read("", A.class);
		assertNull(r);
	}

	public static class C {
		public String name;
		public Integer age;
	}

	//====================================================================================================
	// e - Inline JSON5 cell values
	//====================================================================================================

	@Test void e01_readInlineJson5_nestedBean() {
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| address | `{city:'Boston',state:'MA'}` |";
		var r = MarkdownParser.DEFAULT.read(md, D.class);
		assertEquals("Alice", r.name);
		assertNotNull(r.address);
		assertEquals("Boston", r.address.get("city"));
		assertEquals("MA", r.address.get("state"));
	}

	public static class D {
		public String name;
		public JsonMap address;
	}

	//====================================================================================================
	// f - Type auto-detection (Object target)
	//====================================================================================================

	@Test void f01_autoDetect_integer() {
		var md = "| Property | Value |\n|---|---|\n| count | 42 |";
		var r = (MarshalledMap) MarkdownParser.DEFAULT.read(md, Object.class);
		assertInstanceOf(Integer.class, r.get("count"));
		assertEquals(42, r.get("count"));
	}

	@Test void f02_autoDetect_boolean() {
		var md = "| Property | Value |\n|---|---|\n| flag | true |";
		var r = (MarshalledMap) MarkdownParser.DEFAULT.read(md, Object.class);
		assertInstanceOf(Boolean.class, r.get("flag"));
		assertEquals(Boolean.TRUE, r.get("flag"));
	}

	@Test void f03_autoDetect_string() {
		var md = "| Property | Value |\n|---|---|\n| name | Alice |";
		var r = (MarshalledMap) MarkdownParser.DEFAULT.read(md, Object.class);
		assertInstanceOf(String.class, r.get("name"));
		assertEquals("Alice", r.get("name"));
	}

	//====================================================================================================
	// g - Enum values
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void g01_readEnumValues() {
		var md = "| name | status |\n|---|---|\n| Task1 | PENDING |\n| Task2 | COMPLETED |";
		var r = (List<E>) MarkdownParser.DEFAULT.read(md, List.class, E.class);
		assertEquals(2, r.size());
		assertEquals("Task1", r.get(0).name);
		assertEquals(Status.PENDING, r.get(0).status);
		assertEquals("Task2", r.get(1).name);
		assertEquals(Status.COMPLETED, r.get(1).status);
	}

	public static class E {
		public String name;
		public Status status;
	}

	public enum Status { PENDING, IN_PROGRESS, COMPLETED }

	//====================================================================================================
	// h - Bean property annotations
	//====================================================================================================

	@Test void h01_readMarshalledAnnotations() {
		var md = "| Property | Value |\n|---|---|\n| full_name | Alice |\n| years | 30 |";
		var r = MarkdownParser.DEFAULT.read(md, F.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	public static class F {
		@BeanProp(name="full_name")
		public String name;

		@BeanProp(name="years")
		public int age;
	}

	//====================================================================================================
	// i - Pipe character escaping
	//====================================================================================================

	@Test void i01_readPipeEscaping() {
		var md = "| Property | Value |\n|---|---|\n| desc | hello \\| world |";
		var r = MarkdownParser.DEFAULT.read(md, Map.class);
		assertEquals("hello | world", r.get("desc"));
	}

	//====================================================================================================
	// j - Round-trip: serialize then parse
	//====================================================================================================

	@Test void j01_roundTripBean() {
		var original = new G("Alice", 30, true);
		var md = MarkdownSerializer.DEFAULT.write(original);
		var parsed = MarkdownParser.DEFAULT.read(md, G.class);
		assertEquals("Alice", parsed.name);
		assertEquals(30, parsed.age);
		assertTrue(parsed.active);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void j02_roundTripBeanList() {
		var original = List.of(new G("Alice", 30, true), new G("Bob", 25, false));
		var md = MarkdownSerializer.DEFAULT.write(original);
		var parsed = (List<G>) MarkdownParser.DEFAULT.read(md, List.class, G.class);
		assertEquals(2, parsed.size());
		assertEquals("Alice", parsed.get(0).name);
		assertEquals(30, parsed.get(0).age);
		assertTrue(parsed.get(0).active);
		assertEquals("Bob", parsed.get(1).name);
		assertEquals(25, parsed.get(1).age);
		assertFalse(parsed.get(1).active);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void j03_roundTripStringList() {
		var original = List.of("alpha", "beta", "gamma");
		var md = MarkdownSerializer.DEFAULT.write(original);
		var parsed = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(original, parsed);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void j04_roundTripStringToStringMap() {
		var original = new LinkedHashMap<String,String>();
		original.put("k1", "v1");
		original.put("k2", "v2");
		var md = MarkdownSerializer.DEFAULT.write(original);
		var parsed = (Map<String,String>) MarkdownParser.DEFAULT.read(md, Map.class, String.class, String.class);
		assertEquals(original, parsed);
	}

	public static class G {
		public String name;
		public int age;
		public boolean active;
		public G() {}
		public G(String name, int age, boolean active) {
			this.name = name;
			this.age = age;
			this.active = active;
		}
	}

	//====================================================================================================
	// k - Separator row variants
	//====================================================================================================

	@Test void k01_readSeparatorWithColons() {
		// Some Markdown flavors use |:---|:---| for alignment
		var md = "| Property | Value |\n|:---|:---|\n| name | Alice |";
		var r = MarkdownParser.DEFAULT.read(md, Map.class);
		assertEquals("Alice", r.get("name"));
	}

	//====================================================================================================
	// l - Whitespace handling
	//====================================================================================================

	@Test void l01_readWithExtraWhitespace() {
		var md = "  | Property | Value |\n  |---|---|\n  | name |   Alice   |";
		var r = MarkdownParser.DEFAULT.read(md, A.class);
		assertEquals("Alice", r.name);
	}

	//====================================================================================================
	// m - Shorter rows than header
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void m01_fewerCellsThanHeaders() {
		var md = "| name | age |\n|---|---|\n| Alice |";
		var r = (List<B>) MarkdownParser.DEFAULT.read(md, List.class, B.class);
		assertEquals(1, r.size());
		assertEquals("Alice", r.get(0).name);
		assertEquals(0, r.get(0).age);
	}
}
