// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.marshall.prototext;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for repeated fields (lists, arrays, nested messages) in compact and whitespace modes.
 */
class PrototextRepeatedFields_Test {

	private static final PrototextSerializer COMPACT = PrototextSerializer.DEFAULT;
	private static final PrototextSerializer WS = PrototextSerializer.create().ws().build();
	private static final PrototextParser P = PrototextParser.DEFAULT;

	public static class Inner {
		public String name;
		public int n;
		public Inner() {}
		public Inner(String name, int n) { this.name = name; this.n = n; }
	}

	public static class Scalars { public List<Integer> nums; }
	public static class Beans { public List<Inner> ins; }
	public static class BeanArray { public Inner[] ins; }
	public static class Maps { public Map<String,Integer> m; }
	public static class MapList { public List<Map<String,Object>> ms; }
	public static class Nested { public List<List<Integer>> grid; }
	public static class Empty { public List<Integer> a; public List<Inner> b; public String s; }
	public static class Mid { public String title; public List<Beans> parts; }
	public static class Untyped { public Object items; public Map<String,List<Inner>> byKey; public Inner[][] grid; }
	public static class Mixed { public Beans first; public List<Integer> nums; public List<Inner> ins; public String last; }

	private static void forEachMode(java.util.function.Consumer<PrototextSerializer> c) {
		c.accept(COMPACT);
		c.accept(WS);
	}

	@Test void a01_scalarList() {
		forEachMode(s -> {
			try {
				var b = new Scalars();
				b.nums = List.of(1, 2, 3);
				var r = P.read(s.write(b), Scalars.class);
				assertBean(r, "nums", "[1,2,3]");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a02_beanList() {
		forEachMode(s -> {
			try {
				var b = new Beans();
				b.ins = List.of(new Inner("a", 1), new Inner("b", 2));
				var r = P.read(s.write(b), Beans.class);
				assertSize(2, r.ins);
				assertBean(r.ins.get(0), "name,n", "a,1");
				assertBean(r.ins.get(1), "name,n", "b,2");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a03_beanListSingleElement() {
		forEachMode(s -> {
			try {
				var b = new Beans();
				b.ins = List.of(new Inner("a", 1));
				var r = P.read(s.write(b), Beans.class);
				assertSize(1, r.ins);
				assertBean(r.ins.get(0), "name,n", "a,1");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a04_beanArray() {
		forEachMode(s -> {
			try {
				var b = new BeanArray();
				b.ins = new Inner[]{new Inner("a", 1), new Inner("b", 2)};
				var r = P.read(s.write(b), BeanArray.class);
				assertEquals(2, r.ins.length);
				assertBean(r.ins[1], "name,n", "b,2");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a05_map() {
		forEachMode(s -> {
			try {
				var b = new Maps();
				b.m = new LinkedHashMap<>();
				b.m.put("x", 1);
				b.m.put("y", 2);
				var r = P.read(s.write(b), Maps.class);
				assertEquals(Map.of("x", 1, "y", 2), r.m);
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a06_mapList() {
		forEachMode(s -> {
			try {
				var b = new MapList();
				b.ms = List.of(JsonMap.of("k", "v"), JsonMap.of("k", "w"));
				var r = P.read(s.write(b), MapList.class);
				assertSize(2, r.ms);
				assertEquals("v", r.ms.get(0).get("k"));
				assertEquals("w", r.ms.get(1).get("k"));
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a07_nestedLists() {
		forEachMode(s -> {
			try {
				var b = new Nested();
				b.grid = List.of(List.of(1), List.of(2, 3), List.of());
				var proto = s.write(b);
				assertFalse(proto.contains("\"["), proto);
				var r = P.read(proto, Nested.class);
				assertBean(r, "grid", "[[1],[2,3],[]]");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a08_emptyLists() {
		forEachMode(s -> {
			try {
				var b = new Empty();
				b.a = List.of();
				b.b = List.of();
				b.s = "x";
				var r = P.read(s.write(b), Empty.class);
				assertEquals("x", r.s);
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a09_nestedMessagesInsideRepeatedMessages() {
		forEachMode(s -> {
			try {
				var b = new Mid();
				b.title = "t";
				var b1 = new Beans();
				b1.ins = List.of(new Inner("a", 1), new Inner("b", 2));
				var b2 = new Beans();
				b2.ins = List.of(new Inner("c", 3));
				b.parts = List.of(b1, b2);
				var r = P.read(s.write(b), Mid.class);
				assertEquals("t", r.title);
				assertSize(2, r.parts);
				assertSize(2, r.parts.get(0).ins);
				assertBean(r.parts.get(0).ins.get(1), "name,n", "b,2");
				assertBean(r.parts.get(1).ins.get(0), "name,n", "c,3");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a10_mixedFieldOrdering() {
		forEachMode(s -> {
			try {
				var b = new Mixed();
				b.first = new Beans();
				b.first.ins = List.of(new Inner("z", 9));
				b.nums = List.of(4, 5);
				b.ins = List.of(new Inner("a", 1), new Inner("b", 2));
				b.last = "end";
				var r = P.read(s.write(b), Mixed.class);
				assertBean(r.first.ins.get(0), "name,n", "z,9");
				assertBean(r, "nums,last", "[4,5],end");
				assertSize(2, r.ins);
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void a11_erasedAndMapValueLists() {
		forEachMode(s -> {
			try {
				var b = new Untyped();
				b.items = List.of(new Inner("a", 1), new Inner("b", 2));
				b.byKey = new LinkedHashMap<>();
				b.byKey.put("k", List.of(new Inner("c", 3), new Inner("d", 4)));
				b.byKey.put("j", List.of(new Inner("e", 5)));
				var proto = s.write(b);
				var r = P.read(proto, Untyped.class);
				assertSize(2, (List<?>) r.items);
				assertSize(2, r.byKey.get("k"));
				assertBean(r.byKey.get("j").get(0), "name,n", "e,5");
			} catch (Exception e) { throw new AssertionError(e); }
		});
	}

	@Test void b01_compactHasSeparatorAfterClosingBrace() throws Exception {
		var b = new Mixed();
		b.first = new Beans();
		b.first.ins = List.of(new Inner("z", 9));
		b.nums = List.of(4, 5);
		var proto = COMPACT.write(b);
		assertFalse(proto.matches("(?s).*\\}[A-Za-z\"].*"), proto);
	}

	@Test void b02_exactOutput() throws Exception {
		var b = new Mixed();
		b.ins = List.of(new Inner("a", 1), new Inner("b", 2));
		b.nums = List.of(4);
		assertEquals("ins {n: 1\nname: \"a\"\n}\nins {n: 2\nname: \"b\"\n}\nnums: [4]\n", COMPACT.write(b));
	}

	@Test void b03_nestedListOutput() throws Exception {
		var b = new Nested();
		b.grid = List.of(List.of(1), List.of(2, 3), List.of());
		assertEquals("grid {_value: [1]\n}\ngrid {_value: [2, 3]\n}\ngrid {_value: []\n}\n", COMPACT.write(b));
	}
}
