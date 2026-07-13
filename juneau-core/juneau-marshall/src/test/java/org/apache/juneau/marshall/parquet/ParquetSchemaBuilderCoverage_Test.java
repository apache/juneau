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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Data-driven coverage of {@link ParquetSchemaBuilder} — serializes a wide range of bean/value shapes
 * to drive the schema-mapping branches (every scalar type, nested beans, optionals, nested lists,
 * arrays, string-keyed and non-string-keyed maps, recursive types) under both the default and
 * {@code nativeLogicalTypes} writers.
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types
})
class ParquetSchemaBuilderCoverage_Test extends TestBase {

	public enum Color { RED, GREEN }

	public static class AllScalars {
		public boolean bool;
		public byte b;
		public short sh;
		public int i;
		public long l;
		public float f;
		public double d;
		public String s;
		public Color color;
		public byte[] bytes;
		public UUID uuid;
		public BigDecimal bd;
		public BigInteger bi;
		public Number num;
		public LocalDate date;
		public LocalTime time;
		public Instant ts;
		public Duration dur;

		public static AllScalars sample() {
			var a = new AllScalars();
			a.bool = true; a.b = 1; a.sh = 2; a.i = 3; a.l = 4L; a.f = 1.5f; a.d = 2.5;
			a.s = "x"; a.color = Color.GREEN; a.bytes = new byte[]{1, 2};
			a.uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
			a.bd = new BigDecimal("1.25"); a.bi = BigInteger.TEN; a.num = 7;
			a.date = LocalDate.parse("2026-06-17"); a.time = LocalTime.parse("01:02:03");
			a.ts = Instant.parse("2026-06-17T00:00:00Z"); a.dur = Duration.ofSeconds(5);
			return a;
		}
	}

	public static class Nested {
		public String name;
		public AllScalars inner;
		public Optional<String> maybe = oe();
		public List<String> tags;
		public List<List<Integer>> matrix;
		public int[] nums;
		public Map<String,Integer> counts;
	}

	@Test
	void a01_allScalarsDefault() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(AllScalars.sample()));
		var out = (List<AllScalars>) ParquetParser.DEFAULT.parse(bytes, List.class, AllScalars.class);
		assertEquals(1, out.size());
		assertEquals(3, out.get(0).i);
	}

	@Test
	void a02_allScalarsNative() throws Exception {
		var s = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var bytes = s.serialize(list(AllScalars.sample()));
		var out = (List<AllScalars>) ParquetParser.DEFAULT.parse(bytes, List.class, AllScalars.class);
		assertEquals(LocalDate.parse("2026-06-17"), out.get(0).date);
	}

	@Test
	void a03_nestedStructures() throws Exception {
		var n = new Nested();
		n.name = "n";
		n.inner = AllScalars.sample();
		n.maybe = o("y");
		n.tags = list("a", "b");
		n.matrix = list(list(1, 2), list(3));
		n.nums = new int[]{9, 8};
		n.counts = map("x", 1, "y", 2);
		var bytes = ParquetSerializer.DEFAULT.serialize(list(n));
		var out = (List<Nested>) ParquetParser.DEFAULT.parse(bytes, List.class, Nested.class);
		assertEquals("n", out.get(0).name);
	}

	@Test
	void a04_stringKeyedMapRoot() throws Exception {
		// Drives buildSchemaFromMap.
		var bytes = ParquetSerializer.DEFAULT.serialize(JsonMap.of("a", 1, "b", "two"));
		assertNotEquals(0, bytes.length);
	}

	@Test
	void a05_emptyCollection() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var out = (List<Object>) ParquetParser.DEFAULT.parse(bytes, List.class, Object.class);
		assertTrue(out.isEmpty());
	}

	public static class Node {
		public int val;
		public Node next;
	}

	@Test
	void a06_recursiveTypeWithinDepth() throws Exception {
		var head = new Node();
		head.val = 1;
		head.next = new Node();
		head.next.val = 2;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(head));
		var out = (List<Node>) ParquetParser.DEFAULT.parse(bytes, List.class, Node.class);
		assertEquals(1, out.get(0).val);
	}

	public static class Boxed {
		public Boolean bool;
		public Byte b;
		public Short sh;
		public Integer i;
		public Long l;
		public Float f;
		public Double d;
	}

	@Test
	void a08_boxedWrapperTypes() throws Exception {
		var x = new Boxed();
		x.bool = true; x.b = 1; x.sh = 2; x.i = 3; x.l = 4L; x.f = 1.5f; x.d = 2.5;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(x));
		var out = (List<Boxed>) ParquetParser.DEFAULT.parse(bytes, List.class, Boxed.class);
		assertEquals(3, out.get(0).i);
	}

	public static class Temporals {
		public LocalDate date;
		public Instant ts;
	}

	@Test
	void a09_datesAsStringWhenTimestampDisabled() throws Exception {
		// With writeDatesAsTimestamp disabled, temporals go to the UTF-8 string branch (non-native path).
		var t = new Temporals();
		t.date = LocalDate.parse("2026-06-17");
		t.ts = Instant.parse("2026-06-17T00:00:00Z");
		var s = ParquetSerializer.create().writeDatesAsTimestamp(false).build();
		var bytes = s.serialize(list(t));
		var out = (List<Temporals>) ParquetParser.DEFAULT.parse(bytes, List.class, Temporals.class);
		assertEquals(LocalDate.parse("2026-06-17"), out.get(0).date);
	}

	public static class HasNodeList {
		public String name;
		public List<Node> nodes;
	}

	@Test
	void a11_recursiveTypeThroughCollectionPlaceholder() throws Exception {
		// A self-referential type reached indirectly through a collection, under NULL cycle handling, at the
		// depth limit emits the String back-reference placeholder (addBeanSchema indirect-recursion branch).
		var h = new HasNodeList();
		h.name = "h";
		var n = new Node(); n.val = 1;
		var deep = n;
		for (var i = 0; i < 8; i++) { deep.next = new Node(); deep.next.val = i; deep = deep.next; }
		h.nodes = list(n);
		var s = ParquetSerializer.create().maxRecursionDepth(2).cycleHandling(ParquetCycleHandling.NULL).build();
		// Beyond the depth limit the recursive type collapses to a String placeholder (documented-lossy);
		// the point here is that serialize succeeds and exercises the indirect-recursion schema branch.
		var bytes = s.serialize(list(h));
		assertNotEquals(0, bytes.length);
	}

	@Test
	void a12_mapWithObjectValueType() throws Exception {
		// A Map property whose value type erases to Object exercises the addMapSchema vt-null fallback.
		var n = new Nested();
		n.name = "m";
		n.counts = map("x", 1);
		var bytes = ParquetSerializer.DEFAULT.serialize(list(n));
		assertNotEquals(0, bytes.length);
	}

	@Test
	void a13_recursiveCycleThrowsWhenConfigured() {
		var head = new Node();
		head.val = 1;
		var deep = head;
		for (var i = 0; i < 10; i++) { deep.next = new Node(); deep = deep.next; }
		var s = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).maxRecursionDepth(3).build();
		assertThrows(Exception.class, () -> s.serialize(list(head)));
	}
}
