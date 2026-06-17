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
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-completion tests for {@link ParquetParserSession} targeting the parse-back branches not already
 * exercised by the other parquet test classes: nested-list reconstruction (depth&ge;2), null/empty inner
 * lists on read, map-key-value column chunks (string + non-string keys, empty/null maps), Optional bean
 * property reconstruction (collapseOptionalWrappers), GZIP-compressed round-trips, trim-strings on read,
 * raw-byte[]/UUID/enum columns, native logical types (INT96/DECIMAL/DATE/TIME), and multi-page list columns.
 */
@SuppressWarnings({
	"unchecked",   // Parser returns raw types; explicit casts required for typed assertions
	"java:S5961",  // High assertion count is acceptable in comprehensive data-driven test methods.
	"java:S1192"   // Repeated schema-key string literals mirror production constants.
})
class ParquetParserSessionFull_Test extends TestBase {

	// =================================================================================
	// Beans
	// =================================================================================

	public static class ListBean {
		public String name;
		public List<String> tags;
	}

	public static class NestedListBean {
		public String name;
		public List<List<String>> matrix;
	}

	public static class IntMatrixBean {
		public int[][] grid;
	}

	public static class MapBean {
		public String name;
		public Map<String, String> attrs;
	}

	public static class IntKeyMapBean {
		public Map<Integer, String> codes;
	}

	public static class OptionalBean {
		public Optional<String> nick;
		public int age;
	}

	public static class Inner {
		public String v;
		public int n;
	}

	public static class OptionalBeanProp {
		public Optional<String> a;
		public Optional<Inner> b;
	}

	public static class TeamBean {
		public String team;
		public List<Inner> members;
	}

	public static class NestedB {
		public String v;
	}

	public static class NestedA {
		public NestedB b;
	}

	public static class RawBytesBean {
		public byte[] data;
	}

	public static class UuidBean {
		public UUID id;
	}

	private enum Color { RED, GREEN, BLUE }

	public static class EnumBean {
		public Color color;
	}

	@BeanType(properties = "amount")
	public static class DecBean {
		public BigDecimal amount;
		public DecBean() {}
		public DecBean(BigDecimal amount) { this.amount = amount; }
	}

	@BeanType(properties = "d,t")
	public static class DateTimeBean {
		public LocalDate d;
		public LocalTime t;
		public DateTimeBean() {}
		public DateTimeBean(LocalDate d, LocalTime t) { this.d = d; this.t = t; }
	}

	// =================================================================================
	// a. Single-level lists with null / empty elements (reconstructRowsFromListColumn).
	// =================================================================================

	@Test void a01_listWithEmptyAndNonEmpty() throws Exception {
		var b1 = new ListBean(); b1.name = "a"; b1.tags = list("x", "y");
		var b2 = new ListBean(); b2.name = "b"; b2.tags = list();
		var b3 = new ListBean(); b3.name = "c"; b3.tags = null;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2, b3));
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(3, out.size());
		assertEquals(list("x", "y"), out.get(0).tags);
		assertTrue(out.get(1).tags == null || out.get(1).tags.isEmpty());
	}

	@Test void a02_listOfStringsWithNullElement() throws Exception {
		var b = new ListBean(); b.name = "n"; b.tags = list("a", null, "c");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(3, out.get(0).tags.size());
		assertEquals("a", out.get(0).tags.get(0));
		assertNull(out.get(0).tags.get(1));
		assertEquals("c", out.get(0).tags.get(2));
	}

	@Test void a03_topLevelListOfLists() throws Exception {
		var in = list(list("a", "b"), list("c"), list());
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<List<String>>) ParquetParser.DEFAULT.parse(bytes, List.class, List.class);
		assertEquals(3, out.size());
	}

	// =================================================================================
	// b. Nested (depth>=2) list reconstruction (reconstructNestedListColumn).
	// =================================================================================

	@Test void b01_nestedListBeanRoundTrip() throws Exception {
		var b = new NestedListBean();
		b.name = "grid";
		b.matrix = list(list("a", "b"), list("c"), list());
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedListBean.class);
		assertEquals(1, out.size());
		assertEquals("grid", out.get(0).name);
		assertNotNull(out.get(0).matrix);
		assertEquals(list("a", "b"), out.get(0).matrix.get(0));
		assertEquals(list("c"), out.get(0).matrix.get(1));
	}

	@Test void b02_intMatrixRoundTrip() throws Exception {
		var b = new IntMatrixBean();
		b.grid = new int[][]{{1, 2, 3}, {4, 5}, {}};
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<IntMatrixBean>) ParquetParser.DEFAULT.parse(bytes, List.class, IntMatrixBean.class);
		assertEquals(1, out.size());
		assertArrayEquals(new int[]{1, 2, 3}, out.get(0).grid[0]);
		assertArrayEquals(new int[]{4, 5}, out.get(0).grid[1]);
	}

	@Test void b03_nestedListWithNullInnerList() throws Exception {
		var b = new NestedListBean();
		b.name = "withNull";
		b.matrix = new ArrayList<>();
		b.matrix.add(list("a"));
		b.matrix.add(null);
		b.matrix.add(list("b", "c"));
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedListBean.class);
		assertEquals(3, out.get(0).matrix.size());
		assertEquals(list("a"), out.get(0).matrix.get(0));
	}

	@Test void b04_multipleRowsNestedLists() throws Exception {
		var b1 = new NestedListBean(); b1.name = "r1"; b1.matrix = list(list("a"), list("b", "c"));
		var b2 = new NestedListBean(); b2.name = "r2"; b2.matrix = list(list("d", "e", "f"));
		var b3 = new NestedListBean(); b3.name = "r3"; b3.matrix = list();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2, b3));
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedListBean.class);
		assertEquals(3, out.size());
		assertEquals(list("d", "e", "f"), out.get(1).matrix.get(0));
	}

	@Test void b05_nestedListNullWholeField() throws Exception {
		// One row with matrix==null exercises reconstructNestedListColumn's whole-field-null branch (def==0,rep==0).
		var b1 = new NestedListBean(); b1.name = "has"; b1.matrix = list(list("a", "b"));
		var b2 = new NestedListBean(); b2.name = "none"; b2.matrix = null;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b1, b2));
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedListBean.class);
		assertEquals(2, out.size());
		assertEquals(list("a", "b"), out.get(0).matrix.get(0));
		assertTrue(out.get(1).matrix == null || out.get(1).matrix.isEmpty());
	}

	@Test void b06_nestedListEmptyInnerLists() throws Exception {
		// Inner empty lists at level 2 exercise the null/empty-list-at-level branch (def <= 2*(depth-1)+1).
		var b = new NestedListBean();
		b.name = "emptyInner";
		b.matrix = list(list(), list("x"), list());
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedListBean.class);
		assertEquals(3, out.get(0).matrix.size());
		assertTrue(out.get(0).matrix.get(0).isEmpty());
		assertEquals(list("x"), out.get(0).matrix.get(1));
		assertTrue(out.get(0).matrix.get(2).isEmpty());
	}

	// =================================================================================
	// c. Maps inside beans (mergeMapColumns) — string + non-string keys, empty/null.
	// =================================================================================

	@Test void c01_stringKeyMapInBean() throws Exception {
		var b = new MapBean();
		b.name = "m";
		b.attrs = new LinkedHashMap<>();
		b.attrs.put("k1", "v1");
		b.attrs.put("k2", "v2");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals(2, out.get(0).attrs.size());
		assertEquals("v1", out.get(0).attrs.get("k1"));
	}

	@Test void c02_emptyMapInBean() throws Exception {
		var b = new MapBean(); b.name = "empty"; b.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertNotNull(out.get(0).attrs);
		assertTrue(out.get(0).attrs.isEmpty());
	}

	@Test void c03_intKeyMapInBean() throws Exception {
		var b = new IntKeyMapBean();
		b.codes = new LinkedHashMap<>();
		b.codes.put(1, "one");
		b.codes.put(2, "two");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<IntKeyMapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, IntKeyMapBean.class);
		assertEquals(2, out.get(0).codes.size());
		assertEquals("one", out.get(0).codes.get(1));
	}

	@Test void c03b_intKeyMapWithNullKey() throws Exception {
		// Non-string-keyed bean Map with a null key — exercises mergeMapColumns null-key sentinel (line 1451).
		var b = new IntKeyMapBean();
		b.codes = new LinkedHashMap<>();
		b.codes.put(1, "one");
		b.codes.put(null, "none");
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var out = (List<IntKeyMapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, IntKeyMapBean.class);
		assertEquals("one", out.get(0).codes.get(1));
		assertTrue(out.get(0).codes.containsKey(null));
	}

	@Test void c04_multipleRowsWithMaps() throws Exception {
		var b1 = new MapBean(); b1.name = "r1"; b1.attrs = new LinkedHashMap<>(); b1.attrs.put("a", "1");
		var b2 = new MapBean(); b2.name = "r2"; b2.attrs = new LinkedHashMap<>(); b2.attrs.put("b", "2"); b2.attrs.put("c", "3");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals(2, out.size());
		assertEquals("1", out.get(0).attrs.get("a"));
		assertEquals("2", out.get(1).attrs.get("b"));
		assertEquals("3", out.get(1).attrs.get("c"));
	}

	@Test void c06_mapWithNullKey() throws Exception {
		// Bean Map<String,String> with a null key — serialized as the "<NULL>" sentinel and restored by
		// replaceNullKeySentinel on read.
		var b = new MapBean();
		b.name = "n";
		b.attrs = new LinkedHashMap<>();
		b.attrs.put(null, "nullval");
		b.attrs.put("k", "v");
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals("nullval", out.get(0).attrs.get(null));
		assertEquals("v", out.get(0).attrs.get("k"));
	}

	@Test void c05_mapWithNullValue() throws Exception {
		var s = ParquetSerializer.create().keepNullProperties().build();
		var b = new MapBean();
		b.name = "n";
		b.attrs = new LinkedHashMap<>();
		b.attrs.put("k1", "v1");
		b.attrs.put("k2", null);
		var bytes = s.serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals("v1", out.get(0).attrs.get("k1"));
	}

	// =================================================================================
	// d. Optional bean property reconstruction (collapseOptionalWrappers).
	// =================================================================================

	@Test void d01_optionalPropertyPresent() throws Exception {
		var b = new OptionalBean();
		b.nick = Optional.of("ace");
		b.age = 10;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertEquals(1, out.size());
		assertEquals(10, out.get(0).age);
		assertNotNull(out.get(0).nick);
		assertEquals("ace", out.get(0).nick.orElse(null));
	}

	@Test void d02_optionalPropertyEmpty() throws Exception {
		var b = new OptionalBean();
		b.nick = Optional.empty();
		b.age = 20;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertEquals(20, out.get(0).age);
		assertNotNull(out.get(0).nick);
		assertFalse(out.get(0).nick.isPresent());
	}

	@Test void d03_mixedOptionalRows() throws Exception {
		var b1 = new OptionalBean(); b1.nick = Optional.of("x"); b1.age = 1;
		var b2 = new OptionalBean(); b2.nick = Optional.empty(); b2.age = 2;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b1, b2));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertEquals("x", out.get(0).nick.orElse(null));
		assertFalse(out.get(1).nick.isPresent());
	}

	@Test void d04_optionalBeanPropertyPresent() throws Exception {
		// Optional<Inner> bean property — exercises collapseOptionalValue's bean branch + absent detection.
		var o = new OptionalBeanProp();
		o.a = Optional.of("hi");
		var inner = new Inner(); inner.v = "x"; inner.n = 5;
		o.b = Optional.of(inner);
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<OptionalBeanProp>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBeanProp.class);
		assertEquals("hi", out.get(0).a.orElse(null));
		assertTrue(out.get(0).b.isPresent());
		assertEquals("x", out.get(0).b.get().v);
		assertEquals(5, out.get(0).b.get().n);
	}

	@Test void d05_optionalBeanPropertyEmpty() throws Exception {
		// Empty Optional<Inner> — exercises allNullLeaves / isAbsentOptionalMap bean-group branch.
		var o = new OptionalBeanProp();
		o.a = Optional.empty();
		o.b = Optional.empty();
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<OptionalBeanProp>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBeanProp.class);
		assertFalse(out.get(0).a.isPresent());
		assertFalse(out.get(0).b.isPresent());
	}

	// =================================================================================
	// e. GZIP-compressed round-trips (CompressionCodec.GZIP decompress path).
	// =================================================================================

	@Test void e01_gzipScalarRoundTrip() throws Exception {
		var ser = ParquetSerializer.create().compressionCodec(CompressionCodec.GZIP).build();
		var bytes = ser.serialize("hello-gzip");
		assertEquals("hello-gzip", ParquetParser.DEFAULT.parse(bytes, String.class));
	}

	@Test void e02_gzipBeanListRoundTrip() throws Exception {
		var ser = ParquetSerializer.create().compressionCodec(CompressionCodec.GZIP).build();
		var b1 = new MapBean(); b1.name = "a"; b1.attrs = new LinkedHashMap<>(); b1.attrs.put("k", "v");
		var bytes = ser.serialize(list(b1));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals("v", out.get(0).attrs.get("k"));
	}

	@Test void e03_gzipListColumnRoundTrip() throws Exception {
		var ser = ParquetSerializer.create().compressionCodec(CompressionCodec.GZIP).build();
		var b = new ListBean(); b.name = "z"; b.tags = list("p", "q", "r");
		var bytes = ser.serialize(list(b));
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(list("p", "q", "r"), out.get(0).tags);
	}

	@Test void e04_gzipMultiPage() throws Exception {
		var ser = ParquetSerializer.create().compressionCodec(CompressionCodec.GZIP).pageSize(1024).build();
		var in = new ArrayList<ListBean>();
		for (var i = 0; i < 100; i++) {
			var b = new ListBean(); b.name = "n" + i; b.tags = list("t" + i); in.add(b);
		}
		var bytes = ser.serialize(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(100, out.size());
		assertEquals("n99", out.get(99).name);
	}

	// =================================================================================
	// f. trim-strings on read.
	// =================================================================================

	@Test void f01_trimStringsScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize("  padded  ");
		var p = ParquetParser.create().trimStrings().build();
		assertEquals("padded", p.parse(bytes, String.class));
	}

	@Test void f02_trimStringsBean() throws Exception {
		var b = new MapBean(); b.name = "  trimmed  "; b.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var p = ParquetParser.create().trimStrings().build();
		var out = (List<MapBean>) p.parse(bytes, List.class, MapBean.class);
		assertEquals("trimmed", out.get(0).name);
	}

	@Test void f04_trimStringsWithNullIntermediateGroup() throws Exception {
		// trim-strings + a null intermediate OPTIONAL group exercises the GroupNull prefix-trim path
		// in reassembleRows (lines 1356-1357).
		var nullGroup = new NestedA();           // b == null -> GroupNull on read
		var present = new NestedA(); present.b = new NestedB(); present.b.v = "  z  ";
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(nullGroup, present));
		var p = ParquetParser.create().trimStrings().build();
		var out = (List<NestedA>) p.parse(bytes, List.class, NestedA.class);
		assertNull(out.get(0).b);
		assertEquals("z", out.get(1).b.v);
	}

	@Test void f03_trimStringsListElements() throws Exception {
		var b = new ListBean(); b.name = "x"; b.tags = list(" a ", " b ");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var p = ParquetParser.create().trimStrings().build();
		var out = (List<ListBean>) p.parse(bytes, List.class, ListBean.class);
		assertEquals("a", out.get(0).tags.get(0));
		assertEquals("b", out.get(0).tags.get(1));
	}

	// =================================================================================
	// g. Raw byte[], UUID, enum columns.
	// =================================================================================

	@Test void g01_rawByteArrayRoundTrip() throws Exception {
		var b = new RawBytesBean();
		b.data = new byte[]{1, 2, 3, 4, 5};
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<RawBytesBean>) ParquetParser.DEFAULT.parse(bytes, List.class, RawBytesBean.class);
		assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, out.get(0).data);
	}

	@Test void g02_uuidRoundTrip() throws Exception {
		var b = new UuidBean();
		b.id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<UuidBean>) ParquetParser.DEFAULT.parse(bytes, List.class, UuidBean.class);
		assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), out.get(0).id);
	}

	@Test void g02b_uuidNullRoundTrip() throws Exception {
		// Null UUID field exercises the FIXED_LEN_BYTE_ARRAY null-bytes guard (readValue line 1242-1243).
		var b = new UuidBean();
		b.id = null;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var out = (List<UuidBean>) ParquetParser.DEFAULT.parse(bytes, List.class, UuidBean.class);
		assertEquals(1, out.size());
		assertNull(out.get(0).id);
	}

	@Test void g03_enumRoundTrip() throws Exception {
		var b = new EnumBean();
		b.color = Color.GREEN;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<EnumBean>) ParquetParser.DEFAULT.parse(bytes, List.class, EnumBean.class);
		assertEquals(Color.GREEN, out.get(0).color);
	}

	// =================================================================================
	// h. Native logical types (readLogicalValue + readValue physical switch).
	// =================================================================================

	@Test void h01_decimalInt32NativeRoundTrip() throws Exception {
		// Small-precision DECIMAL fits in INT32 backing — exercises CONVERTED_DECIMAL INT32 branch.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new DecBean(new BigDecimal("12.34")));
		var bytes = ser.serialize(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DecBean.class);
		assertEquals(0, new BigDecimal("12.34").compareTo(out.get(0).amount));
	}

	@Test void h02_decimalInt64NativeRoundTrip() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new DecBean(new BigDecimal("12345.678901")));
		var bytes = ser.serialize(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DecBean.class);
		assertEquals(0, new BigDecimal("12345.678901").compareTo(out.get(0).amount));
	}

	@Test void h03_dateAndTimeMillisNativeRoundTrip() throws Exception {
		// LocalTime with millisecond precision exercises CONVERTED_TIME_MILLIS / CONVERTED_DATE.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new DateTimeBean(LocalDate.parse("2020-02-29"), LocalTime.parse("08:09:10.123")));
		var bytes = ser.serialize(in);
		var out = (List<DateTimeBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DateTimeBean.class);
		assertEquals(LocalDate.parse("2020-02-29"), out.get(0).d);
		assertEquals(LocalTime.parse("08:09:10.123"), out.get(0).t);
	}

	// =================================================================================
	// i. Multi-page list column (page loop in list reader is single-page; covered via map page-skip).
	// =================================================================================

	@Test void i01_dictionaryEncodedColumnRoundTrip() throws Exception {
		// Many repeated string values encourage dictionary encoding on the value column.
		var in = new ArrayList<MapBean>();
		for (var i = 0; i < 50; i++) {
			var b = new MapBean();
			b.name = "repeated-name";
			b.attrs = map();
			in.add(b);
		}
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals(50, out.size());
		assertEquals("repeated-name", out.get(25).name);
	}

	// =================================================================================
	// j. Map target with non-string keys at root + ValueHolder unwrap to Map.
	// =================================================================================

	@Test void j01_rootMapStringKeys() throws Exception {
		var in = new LinkedHashMap<String, String>();
		in.put("a", "1");
		in.put("b", "2");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<String, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, String.class, String.class);
		assertEquals("1", out.get("a"));
		assertEquals("2", out.get("b"));
	}

	@Test void j02_rootMapNonStringKeysWithNullKeyValue() throws Exception {
		var s = ParquetSerializer.create().keepNullProperties().build();
		var in = new LinkedHashMap<Integer, String>();
		in.put(10, "ten");
		in.put(20, "twenty");
		var bytes = s.serialize(in);
		var out = (Map<Integer, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, Integer.class, String.class);
		assertEquals("ten", out.get(10));
		assertEquals("twenty", out.get(20));
	}

	// =================================================================================
	// k. Empty results across target shapes.
	// =================================================================================

	@Test void k01_emptyCollectionTarget() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var c = (Collection<MapBean>) ParquetParser.DEFAULT.parse(bytes, Collection.class, MapBean.class);
		assertNotNull(c);
		assertTrue(c.isEmpty());
	}

	@Test void k02_emptyArrayTargetStrings() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var arr = ParquetParser.DEFAULT.parse(bytes, String[].class);
		assertEquals(0, arr.length);
	}

	@Test void k03_multiRowToSingleBeanTargetReturnsList() throws Exception {
		// Multiple rows parsed against a non-collection bean target falls through to the raw-rows return
		// (doParse line 303-305).
		var b1 = new MapBean(); b1.name = "a"; b1.attrs = map();
		var b2 = new MapBean(); b2.name = "b"; b2.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2));
		Object out = ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>) MapBean.class);
		assertNotNull(out);
		assertTrue(out instanceof List);
		assertEquals(2, ((List<?>) out).size());
	}

	@Test void k04_nullScalarSerializesToEmpty() throws Exception {
		// Serializing a null value yields a zero-row file; parsing back gives null / empty per target shape.
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize((Object) null);
		assertNull(ParquetParser.DEFAULT.parse(bytes, String.class));
		assertEquals(0, ParquetParser.DEFAULT.parse(bytes, String[].class).length);
	}

	// =================================================================================
	// n. Bean with List<Bean> property across multiple rows incl. empty inner list.
	// =================================================================================

	@Test void n01_listOfBeansMultiRow() throws Exception {
		var i1 = new Inner(); i1.v = "a"; i1.n = 1;
		var i2 = new Inner(); i2.v = "b"; i2.n = 2;
		var t1 = new TeamBean(); t1.team = "t1"; t1.members = list(i1, i2);
		var t2 = new TeamBean(); t2.team = "t2"; t2.members = list();
		var i3 = new Inner(); i3.v = "c"; i3.n = 3;
		var t3 = new TeamBean(); t3.team = "t3"; t3.members = list(i3);
		var bytes = ParquetSerializer.create().addBeanTypes().build().serialize(list(t1, t2, t3));
		var out = (List<TeamBean>) ParquetParser.DEFAULT.parse(bytes, List.class, TeamBean.class);
		assertEquals(3, out.size());
		assertEquals(2, out.get(0).members.size());
		assertEquals("a", out.get(0).members.get(0).v);
		assertEquals(1, out.get(0).members.get(0).n);
		assertTrue(out.get(1).members == null || out.get(1).members.isEmpty());
		assertEquals("c", out.get(2).members.get(0).v);
	}

	// =================================================================================
	// m. Single-row {value:X} unwrap to array / collection / optional targets (doParse 222-235).
	// =================================================================================

	@Test void m01_singleScalarToArray() throws Exception {
		// A single scalar serializes to one {value:X} row; parsing to an array wraps it (line 223-227).
		var bytes = ParquetSerializer.DEFAULT.serialize(42);
		var arr = ParquetParser.DEFAULT.parse(bytes, Integer[].class);
		assertEquals(1, arr.length);
		assertEquals(42, arr[0]);
	}

	@Test void m02_singleScalarToCollection() throws Exception {
		// Single scalar -> Collection target (line 229-235).
		var bytes = ParquetSerializer.DEFAULT.serialize("solo");
		var c = (Collection<String>) ParquetParser.DEFAULT.parse(bytes, ArrayList.class, String.class);
		assertEquals(1, c.size());
		assertEquals("solo", c.iterator().next());
	}

	@Test void m03_singleScalarToList() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(7);
		var l = (List<Integer>) ParquetParser.DEFAULT.parse(bytes, List.class, Integer.class);
		assertEquals(1, l.size());
		assertEquals(7, l.get(0));
	}

	@Test void m04_rawCollectionNoElementType() throws Exception {
		// Raw ArrayList target (no element type) with multiple rows — exercises toCollection's
		// elemType==null fallback to Object (line 1673-1674).
		var bytes = ParquetSerializer.DEFAULT.serialize(list("a", "b", "c"));
		var c = (Collection<Object>) ParquetParser.DEFAULT.parse(bytes, ArrayList.class);
		assertEquals(3, c.size());
	}

	// =================================================================================
	// l. Dictionary-encoded data page WITH definition levels (readDictionaryEncodedPage,
	//    maxDefLevel>0 — optional column nulls + GroupNull intermediate-group sentinels).
	// =================================================================================

	/** Builds a dict-encoded page body: [4-byte LE defLen][RLE def levels][1-byte idxBitWidth][RLE indices]. */
	private static byte[] dictPage(int defBitWidth, int[] defLevels, int idxBitWidth, int[] indices) {
		var defEnc = new RleBitPackingEncoder(defBitWidth);
		for (var d : defLevels)
			defEnc.writeInt(d);
		var defSection = defEnc.toByteArrayWithLength();
		var idxEnc = new RleBitPackingEncoder(idxBitWidth);
		for (var idx : indices)
			idxEnc.writeInt(idx);
		var idxBytes = idxEnc.toByteArray();
		var page = new byte[defSection.length + 1 + idxBytes.length];
		System.arraycopy(defSection, 0, page, 0, defSection.length);
		page[defSection.length] = (byte) idxBitWidth;
		System.arraycopy(idxBytes, 0, page, defSection.length + 1, idxBytes.length);
		return page;
	}

	@Test void l01_dictEncodedWithOptionalNulls() throws Exception {
		// maxDefLevel=1: def<1 means null (no index consumed); def==1 means present (index consumed).
		var dictionary = List.<Object>of("a", "b", "c");
		int[] defLevels = {1, 0, 1, 1, 0};   // present, null, present, present, null
		int[] indices = {0, 1, 2};           // for the three present slots
		var page = dictPage(1, defLevels, 2, indices);
		var values = new ArrayList<Object>();
		ParquetParserSession.readDictionaryEncodedPage(page, 5, 1, 1, dictionary, values);
		assertEquals(Arrays.asList("a", null, "b", "c", null), values);
	}

	@Test void l02_dictEncodedWithGroupNullSentinel() throws Exception {
		// maxDefLevel=3: def < maxDefLevel-1 (i.e. def<2) yields a GroupNull(def) intermediate-group sentinel.
		var dictionary = List.<Object>of("x", "y");
		int[] defLevels = {3, 1, 2, 3};      // present, GroupNull(1), plain-null, present
		int[] indices = {0, 1};              // two present slots
		var page = dictPage(2, defLevels, 1, indices);
		var values = new ArrayList<Object>();
		ParquetParserSession.readDictionaryEncodedPage(page, 4, 3, 2, dictionary, values);
		assertEquals(4, values.size());
		assertEquals("x", values.get(0));
		// def=1 < maxDefLevel-1(=2) -> GroupNull sentinel.
		assertEquals("GroupNull[defLevel=1]", values.get(1).toString());
		// def=2 == maxDefLevel-1 -> plain null.
		assertNull(values.get(2));
		assertEquals("y", values.get(3));
	}

	@Test void l03_dictEncodedNullDictionaryThrows() {
		var page = new byte[]{1, 0};
		var values = new ArrayList<Object>();
		var ex = assertThrows(org.apache.juneau.marshall.parser.ParseException.class,
			() -> ParquetParserSession.readDictionaryEncodedPage(page, 1, 0, 1, null, values));
		assertTrue(ex.getMessage().contains("no preceding dictionary page"));
	}
}
