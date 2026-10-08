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
package org.apache.juneau.marshall.toml;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests native TOML arrays and arrays-of-tables for non-scalar lists.
 */
class TomlArrays_Test {

	private static final TomlSerializer S = TomlSerializer.create().sortKeys(true).build();
	private static final TomlParser P = TomlParser.DEFAULT;

	public static class Inner {
		public String name;
		public int n;
		public Inner() {}
		public Inner(String name, int n) { this.name = name; this.n = n; }
	}

	public static class Scalars {
		public List<Integer> nums;
	}

	public static class Beans {
		public List<Inner> ins;
	}

	public static class Maps {
		public List<Map<String,Object>> ms;
	}

	public static class Nested {
		public List<List<Integer>> grid;
	}

	public static class Empty {
		public List<Integer> a;
		public List<Inner> b;
	}

	public static class Outer {
		public String title;
		public Beans child;
	}

	public static class Mixed {
		public List<Object> items;
	}

	@Test void a01_scalarList() throws Exception {
		var b = new Scalars();
		b.nums = List.of(1, 2);
		var toml = S.write(b);
		assertEquals("nums = [1, 2]\n", toml);
		var r = P.read(toml, Scalars.class);
		assertBean(r, "nums", "[1,2]");
	}

	@Test void a02_beanList() throws Exception {
		var b = new Beans();
		b.ins = List.of(new Inner("a", 1), new Inner("b", 2));
		var toml = S.write(b);
		assertTrue(toml.contains("[[ins]]\nn = 1\nname = \"a\""), toml);
		var r = P.read(toml, Beans.class);
		assertSize(2, r.ins);
		assertBean(r.ins.get(0), "name,n", "a,1");
		assertBean(r.ins.get(1), "name,n", "b,2");
	}

	@Test void a03_mapList() throws Exception {
		var b = new Maps();
		b.ms = List.of(JsonMap.of("k", "v"), JsonMap.of("k", "w"));
		var toml = S.write(b);
		assertTrue(toml.contains("[[ms]]\nk = \"v\"\n\n[[ms]]\nk = \"w\""), toml);
		var r = P.read(toml, Maps.class);
		assertEquals(2, r.ms.size());
		assertEquals("v", r.ms.get(0).get("k"));
		assertEquals("w", r.ms.get(1).get("k"));
	}

	@Test void a04_nestedArrays() throws Exception {
		var b = new Nested();
		b.grid = List.of(List.of(1), List.of(2, 3));
		var toml = S.write(b);
		assertEquals("grid = [[1], [2, 3]]\n", toml);
		var r = P.read(toml, Nested.class);
		assertBean(r, "grid", "[[1],[2,3]]");
	}

	@Test void a05_emptyLists() throws Exception {
		var b = new Empty();
		b.a = List.of();
		b.b = List.of();
		var toml = S.write(b);
		assertEquals("a = []\nb = []\n", toml);
		var r = P.read(toml, Empty.class);
		assertEmpty(r.a);
		assertEmpty(r.b);
	}

	@Test void a06_listInNestedTable() throws Exception {
		var o = new Outer();
		o.title = "t";
		o.child = new Beans();
		o.child.ins = List.of(new Inner("a", 1), new Inner("b", 2));
		var toml = S.write(o);
		assertTrue(toml.contains("[[child.ins]]"), toml);
		var r = P.read(toml, Outer.class);
		assertEquals("t", r.title);
		assertSize(2, r.child.ins);
		assertBean(r.child.ins.get(0), "name,n", "a,1");
		assertBean(r.child.ins.get(1), "name,n", "b,2");
	}

	@Test void a07_mixedList() throws Exception {
		var b = new Mixed();
		b.items = List.of(1, "two", List.of(3));
		var toml = S.write(b);
		assertEquals("items = [1, \"two\", [3]]\n", toml);
		var r = P.read(toml, Mixed.class);
		assertEquals(3, r.items.size());
		assertEquals(1, ((Number)r.items.get(0)).intValue());
		assertEquals("two", r.items.get(1));
		assertEquals(List.of(3L), r.items.get(2));
	}

	@Test void a08_rawMapListOfLists() throws Exception {
		var m = JsonMap.of("nums", List.of(1, 2), "pairs", List.of(List.of("a", "b"), List.of("c")));
		var toml = S.write(m);
		assertTrue(toml.contains("nums = [1, 2]"), toml);
		assertTrue(toml.contains("pairs = [[\"a\", \"b\"], [\"c\"]]"), toml);
		var r = P.read(toml, JsonMap.class);
		assertList(r.getList("nums"), 1L, 2L);
		assertList(r.getList("pairs"), List.of("a", "b"), List.of("c"));
	}

	@Test void a09_rawMapListOfMaps() throws Exception {
		var m = JsonMap.of("ins", List.of(JsonMap.of("n", 1), JsonMap.of("n", 2)));
		var toml = S.write(m);
		assertTrue(toml.contains("[[ins]]\nn = 1\n\n[[ins]]\nn = 2"), toml);
		var r = P.read(toml, JsonMap.class);
		assertEquals(2, r.getList("ins").size());
	}

	@Test void a10_beanArray() throws Exception {
		var toml = S.write(Map.of("ins", new Inner[]{new Inner("a", 1)}));
		var r = P.read(toml, JsonMap.class);
		assertEquals(1, r.getList("ins").size());
	}
}
