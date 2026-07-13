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

	public static class OptNullInnerBean {
		public Optional<NestedB> inner;
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

	public static class BeanArrayPropBean {
		public String name;
		public Inner[] items;
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

	@Test void c07_emptyFileAsIntKeyMap() throws Exception {
		// Empty file parsed as Map<Integer,String>: rows is empty, isKeyValuePairFormat returns false
		// via e(rows) early-exit (line 731 true branch), exercises the isEmpty branch.
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var out = (Map<Integer, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, Integer.class, String.class);
		// An empty Parquet file with no rows produces null or empty map for a Map target.
		assertTrue(out == null || out.isEmpty());
	}

	// =================================================================================
	// d. Optional bean property reconstruction (collapseOptionalWrappers).
	// =================================================================================

	@Test void d01_optionalPropertyPresent() throws Exception {
		var b = new OptionalBean();
		b.nick = o("ace");
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
		b.nick = oe();
		b.age = 20;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertEquals(20, out.get(0).age);
		assertNotNull(out.get(0).nick);
		assertFalse(out.get(0).nick.isPresent());
	}

	@Test void d03_mixedOptionalRows() throws Exception {
		var b1 = new OptionalBean(); b1.nick = o("x"); b1.age = 1;
		var b2 = new OptionalBean(); b2.nick = oe(); b2.age = 2;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b1, b2));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertEquals("x", out.get(0).nick.orElse(null));
		assertFalse(out.get(1).nick.isPresent());
	}

	@Test void d04_optionalBeanPropertyPresent() throws Exception {
		// Optional<Inner> bean property — exercises collapseOptionalValue's bean branch + absent detection.
		var o = new OptionalBeanProp();
		o.a = o("hi");
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
		o.a = oe();
		o.b = oe();
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

	@Test void f05_trimStringsNullString() throws Exception {
		// trimStrings=true + null string value exercises the s==null short-circuit in readValue
		// (trimStrings && s != null → false → return s=null without calling trim()).
		var b = new MapBean(); b.name = null; b.attrs = map();
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var p = ParquetParser.create().trimStrings().build();
		var out = (List<MapBean>) p.parse(bytes, List.class, MapBean.class);
		assertNull(out.get(0).name);
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

	// =================================================================================
	// p. Map-target {root:{...}} unwrap and ValueHolder variants (doParse lines 262-266).
	// =================================================================================

	// Bean for nested-Optional tests: Optional<Optional<String>> property.
	public static class NestedOptBean {
		public Optional<Optional<String>> deep;
	}

	// Bean with a non-Optional scalar property and no Optional properties — exercises hasOptional=false early-exit.
	public static class NoOptBean {
		public String name;
		public int val;
	}

	public static class BeanWithRoot {
		public String x;
		public int y;
	}

	@Test void p01_rootKeyNamedRootUnwrap() throws Exception {
		// A Map whose key is literally "root" with a nested Map value triggers the {root:{...}} unwrap
		// on line 264: buildSchemaFromMap creates root.root.key_value.key/value columns; reassembleRows
		// merges them into row["root"] = innerMap; doParse unwraps the single-key root map.
		var inner = new LinkedHashMap<String, Object>();
		inner.put("inner", "val");
		var in = new LinkedHashMap<String, Object>();
		in.put("root", inner);
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<?, ?>) ParquetParser.DEFAULT.parse(bytes, Map.class);
		// The parser unwraps {root:{...}} → the inner map is returned directly.
		assertNotNull(out);
	}

	@Test void p02_rootMapStringKeysFlatUnwrap() throws Exception {
		// Normal string-keyed map: doParse single-row Map branch returns the flat row map directly.
		var in = new LinkedHashMap<String, Object>();
		in.put("a", "1");
		in.put("b", "2");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<String, Object>) ParquetParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals("1", out.get("a"));
		assertEquals("2", out.get("b"));
	}

	@Test void p03_mapTargetMultiRowFallthrough() throws Exception {
		// More than one row for a Map target falls through to the raw-rows return.
		var b1 = new MapBean(); b1.name = "r1"; b1.attrs = map();
		var b2 = new MapBean(); b2.name = "r2"; b2.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2));
		// Parsing as a raw Map will return a List because rows.size() != 1
		Object out = ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>) Map.class);
		assertNotNull(out);
	}

	@Test void p04_mapTargetNonStringKeyWithNullKey() throws Exception {
		// nullKey path in isKeyValuePairFormat: null key stored as nullKeyString sentinel
		var in = new LinkedHashMap<Integer, String>();
		in.put(1, "one");
		in.put(null, "nullval");
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(in);
		var out = (Map<Integer, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, Integer.class, String.class);
		assertEquals("one", out.get(1));
		assertTrue(out.containsKey(null));
		assertEquals("nullval", out.get(null));
	}

	@Test void p05_mapTargetNonStringKeyNullValueType() throws Exception {
		// valueType == null path in the non-string-key map branch (line 249)
		var in = new LinkedHashMap<Integer, String>();
		in.put(10, "ten");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		// Parse as Map<Integer, ?> without explicit value type
		var out = (Map<?, ?>) ParquetParser.DEFAULT.parse(bytes, Map.class, Integer.class);
		assertEquals("ten", out.get(10));
	}

	// =================================================================================
	// q. doParse null-inner scalar rows (line 296-298) and edge branches.
	// =================================================================================

	@Test void q01_nullValueInScalarRowToBean() throws Exception {
		// Serializing a null scalar gives a row {value: null}; parsing to a non-null bean type
		// exercises the inner==null → "" branch on line 298.
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize((Object) null);
		var result = ParquetParser.DEFAULT.parse(bytes, String.class);
		assertNull(result);
	}

	@Test void q02_optionalTargetWithRows() throws Exception {
		// Optional target with multiple rows falls through to the Optional(unwrapValueHolder) branch.
		var b = new MapBean(); b.name = "x"; b.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		// Shouldn't throw; unwrapValueHolder handles a Map row
		Object out = ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>) Optional.class);
		assertNotNull(out);
	}

	// =================================================================================
	// r. collapseOptionalWrappers / isAbsentOptionalMap / collapseOptionalValue branches.
	// =================================================================================

	public static class DeepOptBean {
		public Optional<String> a;
		public Optional<String> b;
	}

	@Test void r01_isAbsentOptionalMapAllNullValues() throws Exception {
		// A bean with multiple Optional<String> properties, both absent. At wire level the group
		// for each absent Optional contains {value: null}; isAbsentOptionalMap's allNull-values
		// check (m.values().stream().allMatch(v -> v == null)) returns true.
		var o = new DeepOptBean();
		o.a = oe();
		o.b = oe();
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<DeepOptBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DeepOptBean.class);
		assertFalse(out.get(0).a.isPresent());
		assertFalse(out.get(0).b.isPresent());
	}

	@Test void r02_collapseOptionalValueWithBeanInner() throws Exception {
		// Optional<Inner> present: collapseOptionalValue enters the targetType.isBean() branch (line 1601).
		var o = new OptionalBeanProp();
		o.b = o(new Inner());
		o.b.get().v = "val";
		o.b.get().n = 99;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<OptionalBeanProp>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBeanProp.class);
		assertTrue(out.get(0).b.isPresent());
		assertEquals("val", out.get(0).b.get().v);
		assertEquals(99, out.get(0).b.get().n);
	}

	@Test void r03_collapseOptionalNonBeanPropSkipped() throws Exception {
		// A bean with no Optional properties: collapseOptionalWrappers hasOptional=false early-exit (line 1526).
		var b = new NoOptBean(); b.name = "no-opt"; b.val = 7;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<NoOptBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NoOptBean.class);
		assertEquals("no-opt", out.get(0).name);
		assertEquals(7, out.get(0).val);
	}

	@Test void r04_collapseOptionalBeanNestedRecurse() throws Exception {
		// NestedA.b is a non-Optional bean: collapseOptionalWrappers recurses into it (line 1546).
		var a = new NestedA(); a.b = new NestedB(); a.b.v = "recurse";
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(a));
		var out = (List<NestedA>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedA.class);
		assertEquals("recurse", out.get(0).b.v);
	}

	@Test void r05_nestedOptionalPresent() throws Exception {
		// Optional<Optional<String>> with inner Optional present:
		// collapseOptionalValue hits the isOptional branch (line 1595), isAbsentOptionalMap=false,
		// then recurses through {value:{value:"x"}} → "x".
		var o = new NestedOptBean();
		o.deep = o(o("hello"));
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<NestedOptBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedOptBean.class);
		assertTrue(out.get(0).deep.isPresent());
		assertTrue(out.get(0).deep.get().isPresent());
		assertEquals("hello", out.get(0).deep.get().get());
	}

	@Test void r06_nestedOptionalEmptyInner() throws Exception {
		// Optional<Optional<String>> with inner Optional empty:
		// isAbsentOptionalMap recurses (elemType.isOptional branch, line 1574) and returns true.
		var o = new NestedOptBean();
		o.deep = o(oe());
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<NestedOptBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedOptBean.class);
		assertTrue(out.get(0).deep.isPresent());
		assertFalse(out.get(0).deep.get().isPresent());
	}

	@Test void r07b_collapseOptionalWrappersValNull() throws Exception {
		// OptionalBean with nick=null serialized WITHOUT keepNullProperties: the nick column
		// is absent from the row map, so val==null at line 1464 → continue (coverage of that branch).
		var b = new OptionalBean();
		b.nick = null;
		b.age = 42;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertEquals(42, out.get(0).age);
	}

	@Test void r07_nestedOptionalOuterEmpty() throws Exception {
		// Outer Optional empty (whole deep == Optional.empty()):
		// collapseOptionalValue: isAbsentOptionalMap for outer returns true → null.
		// MarshallingSession converts null Optional<Optional<String>> property to Optional.empty or Optional.of(empty).
		var o = new NestedOptBean();
		o.deep = oe();
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<NestedOptBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedOptBean.class);
		// Parquet round-trip serializes Optional.empty() as a null group; on parse the outer Optional is
		// reconstructed — it may be empty or present(empty) depending on MarshallingSession coercion.
		assertNotNull(out.get(0).deep);
	}

	@Test void r08_allNullLeavesNullLeafInOptionalBean() throws Exception {
		// Optional<NestedB> present, but NestedB.v == null.
		// Row: {inner: {value: {v: null}}}. isAbsentOptionalMap → allNullLeaves({v:null}) →
		// allNullLeaves(null) returns true (line 1505 true branch), allMatch → true →
		// isAbsentOptionalMap returns true → property set to null → MarshallingSession → Optional.empty().
		var o = new OptNullInnerBean();
		o.inner = o(new NestedB());  // NestedB.v stays null
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<OptNullInnerBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptNullInnerBean.class);
		assertFalse(out.get(0).inner.isPresent());
	}

	// =================================================================================
	// s. uuidFromFixedLenBytes wrong-length guard (line 1326).
	// =================================================================================

	@Test void s01_uuidWrongLengthReturnsZeroUuid() {
		// A UUID bean round-tripped normally will always produce the right UUID (16-byte FLBA).
		// The wrong-length guard (b.length != 16 → new UUID(0L, 0L)) can only be hit by fuzz input
		// so we test it directly via the package-private method.
		// Null input → zero UUID.
		var nullResult = ParquetParserSession.uuidFromFixedLenBytesForTest(null);
		assertEquals(new UUID(0L, 0L), nullResult);
		// Wrong-length input → zero UUID.
		var wrongLength = ParquetParserSession.uuidFromFixedLenBytesForTest(new byte[]{1, 2, 3});
		assertEquals(new UUID(0L, 0L), wrongLength);
	}

	// =================================================================================
	// u. readValue: default type branch and logical-type NOT_LOGICAL fallthrough.
	// =================================================================================

	/** Builds a one-value PLAIN page body for a BYTE_ARRAY value (no def levels, maxDefLevel=0). */
	private static byte[] plainByteArrayPage(String s) {
		var data = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		var buf = new byte[4 + data.length];
		buf[0] = (byte)(data.length & 0xFF);
		buf[1] = (byte)((data.length >> 8) & 0xFF);
		buf[2] = (byte)((data.length >> 16) & 0xFF);
		buf[3] = (byte)((data.length >> 24) & 0xFF);
		System.arraycopy(data, 0, buf, 4, data.length);
		return buf;
	}

	@Test void u01_readValueDefaultTypeBranch() throws Exception {
		// readValue switch default (type code 99, unknown) calls readByteArrayAsString.
		// Two sub-tests: trimStrings=false (returns raw) and trimStrings=true (trims).
		var page = plainByteArrayPage("  hello  ");
		// trimStrings=false: yields raw string.
		var reader1 = new ParquetColumnReader(page, 1, 0);
		var result1 = ParquetParserSession.readValue(reader1, 99, false, false, false, null);
		assertEquals("  hello  ", result1);
		// trimStrings=true: yields trimmed string.
		var reader2 = new ParquetColumnReader(page, 1, 0);
		var result2 = ParquetParserSession.readValue(reader2, 99, true, false, false, null);
		assertEquals("hello", result2);
	}

	@Test void u02_readLogicalValueDefaultBranch() throws Exception {
		// readLogicalValue with an unrecognized convertedType (e.g. CONVERTED_UTF8=0) returns NOT_LOGICAL
		// and readValue falls through to its physical-type switch.
		var page = plainByteArrayPage("test");
		var reader = new ParquetColumnReader(page, 1, 0);
		// CONVERTED_UTF8=0 is not handled in readLogicalValue's switch → default → NOT_LOGICAL.
		// readValue then falls to TYPE_BYTE_ARRAY branch and decodes as UTF-8 string.
		var logical = new ParquetParserSession.ColumnLogical(ParquetSchemaElement.CONVERTED_UTF8, 0, 0);
		var result = ParquetParserSession.readValue(reader, ParquetSchemaElement.TYPE_BYTE_ARRAY, false, false, false, logical);
		assertEquals("test", result);
	}

	@Test void u03_readDictEncodedPageOutOfRangeDictIndex() {
		// readDictionaryEncodedPage: dictionary index >= dictionary.size() → ParseException.
		var dictionary = List.<Object>of("a", "b");  // size=2, valid indices 0 and 1
		// Page: maxDefLevel=0 so no def section. idxBitWidth=4, index=10 (out of range for size-2 dict).
		var idxEnc = new RleBitPackingEncoder(4);
		idxEnc.writeInt(10);
		var idxBytes = idxEnc.toByteArray();
		var page = new byte[1 + idxBytes.length];
		page[0] = 4; // idxBitWidth=4
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);
		var values = new ArrayList<Object>();
		assertThrows(org.apache.juneau.marshall.parser.ParseException.class,
			() -> ParquetParserSession.readDictionaryEncodedPage(page, 1, 0, 1, dictionary, values));
	}

	@Test void u04_readLogicalValueDecimalNotLogicalFallthrough() throws Exception {
		// readLogicalValue CONVERTED_DECIMAL with wrong physical type (FLOAT) returns NOT_LOGICAL.
		// readValue then falls to the physical-type switch FLOAT branch.
		var floatBytes = new byte[4];
		var floatBits = Float.floatToRawIntBits(1.5f);
		floatBytes[0] = (byte)(floatBits & 0xFF);
		floatBytes[1] = (byte)((floatBits >> 8) & 0xFF);
		floatBytes[2] = (byte)((floatBits >> 16) & 0xFF);
		floatBytes[3] = (byte)((floatBits >> 24) & 0xFF);
		var reader = new ParquetColumnReader(floatBytes, 1, 0);
		var logical = new ParquetParserSession.ColumnLogical(ParquetSchemaElement.CONVERTED_DECIMAL, 2, 10);
		var result = ParquetParserSession.readValue(reader, ParquetSchemaElement.TYPE_FLOAT, false, false, false, logical);
		assertEquals(1.5f, (float)result, 0.001f);
	}

	// =================================================================================
	// t. readDictionaryEncodedPage with maxDefLevel==0 (no def section) — line 1157.
	// =================================================================================

	@Test void t01_dictEncodedMaxDefLevelZero() throws Exception {
		// maxDefLevel==0: the defDecoder is null (skipped), all values are present, only index section read.
		var dictionary = List.<Object>of("alpha", "beta", "gamma");
		// No def section needed; page body is just: [1-byte idxBitWidth][RLE indices].
		// Encode three indices [2, 0, 1] with bitWidth=2.
		var idxEnc = new RleBitPackingEncoder(2);
		idxEnc.writeInt(2); idxEnc.writeInt(0); idxEnc.writeInt(1);
		var idxBytes = idxEnc.toByteArray();
		var page = new byte[1 + idxBytes.length];
		page[0] = 2; // idxBitWidth = 2
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);
		var values = new ArrayList<Object>();
		ParquetParserSession.readDictionaryEncodedPage(page, 3, 0, 1, dictionary, values);
		assertEquals(Arrays.asList("gamma", "alpha", "beta"), values);
	}

	// =================================================================================
	// v. readSchema / readAllRows edge branches.
	// =================================================================================

	@Test void v01_multipleBeansMultiplePages() throws Exception {
		// Many beans force multiple data pages (pageSize=1024), exercising the page-loop counter
		// in readColumnChunk and the inner-list page accumulation.
		var in = new ArrayList<MapBean>();
		for (var i = 0; i < 200; i++) {
			var b = new MapBean();
			b.name = "n" + i;
			b.attrs = new LinkedHashMap<>();
			b.attrs.put("k" + i, "v" + i);
			in.add(b);
		}
		var bytes = ParquetSerializer.create().pageSize(512).build().serialize(in);
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals(200, out.size());
		assertEquals("n99", out.get(99).name);
	}

	@Test void v02_requiredSchemaElement() throws Exception {
		// A bean with only primitive (REQUIRED) columns exercises the maxDefLevel==0 path in readColumnChunk
		// (rep==REQUIRED → maxDefLevel = 0).
		var b = new ParquetSerializer_Test.PrimitiveBean();
		b.i = 7; b.l = 8L; b.d = 1.5; b.b = true; b.s = "req";
		var bytes = ParquetSerializer.DEFAULT.serialize(b);
		var out = ParquetParser.DEFAULT.parse(bytes, ParquetSerializer_Test.PrimitiveBean.class);
		assertEquals(7, out.i);
	}

	// =================================================================================
	// w. mergeListBeanColumns empty / short-circuit paths (lines 1460-1484).
	// =================================================================================

	@Test void w01_listBeanColumnsEmptyGroup() throws Exception {
		// A List<Inner> bean property where all rows have empty inner lists exercises the listSize==0 path.
		var t = new TeamBean(); t.team = "empty"; t.members = list();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(t));
		var out = (List<TeamBean>) ParquetParser.DEFAULT.parse(bytes, List.class, TeamBean.class);
		assertNotNull(out.get(0).members);
		assertTrue(out.get(0).members == null || out.get(0).members.isEmpty());
	}

	@Test void w02_listBeanColumnsWithNulls() throws Exception {
		// List<Inner> where some members are non-null and some rows are missing values.
		var i1 = new Inner(); i1.v = "a"; i1.n = 1;
		var t1 = new TeamBean(); t1.team = "t"; t1.members = list(i1, null);
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(t1));
		var out = (List<TeamBean>) ParquetParser.DEFAULT.parse(bytes, List.class, TeamBean.class);
		assertEquals("a", out.get(0).members.get(0).v);
	}

	// =================================================================================
	// x. replaceNullKeySentinel on List elements (line 1504).
	// =================================================================================

	@Test void x01_nullKeySentinelInNestedMap() throws Exception {
		// A map nested inside a list bean property that has a null key exercises the List branch
		// of replaceNullKeySentinel (line 1504).
		var b = new MapBean(); b.name = "nested"; b.attrs = new LinkedHashMap<>();
		b.attrs.put(null, "nullval");
		b.attrs.put("k", "v");
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertTrue(out.get(0).attrs.containsKey(null));
		assertEquals("nullval", out.get(0).attrs.get(null));
	}

	// =================================================================================
	// y. prepareMapForBean branches (line 1634-1641).
	// =================================================================================

	@Test void y01_prepareMapForBeanNullValue() throws Exception {
		// prepareMapForBean with value==null returns null immediately (line 1635 false branch).
		// Achieved indirectly: a null Optional<String> property stores null in the row map,
		// then prepareMapForBean(null, ...) is invoked on the collapsed value.
		var o = new OptionalBean(); o.nick = oe(); o.age = 5;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<OptionalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBean.class);
		assertFalse(out.get(0).nick.isPresent());
		assertEquals(5, out.get(0).age);
	}

	@Test void y02_prepareMapForBeanAlreadyJsonMap() throws Exception {
		// prepareMapForBean: value is already a JsonMap → returned as-is (line 1637 true branch).
		// Exercised by any normal bean round-trip that goes through reassembleRows.
		var b = new MapBean(); b.name = "already"; b.attrs = new LinkedHashMap<>(); b.attrs.put("x", "1");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals("already", out.get(0).name);
	}

	// =================================================================================
	// z. mergeMapColumns empty/null key-column paths (lines 1434-1455).
	// =================================================================================

	@Test void z01_mergeMapColumnsEmptyAttrs() throws Exception {
		// MapBean with null attrs: key_value column data is empty (rowIndex >= size → return empty map).
		var b = new MapBean(); b.name = "nomap"; b.attrs = null;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals("nomap", out.get(0).name);
		assertTrue(out.get(0).attrs == null || out.get(0).attrs.isEmpty());
	}

	@Test void z02_mergeMapColumnsNullKeyRow() throws Exception {
		// Serialize two MapBeans where the second has null attrs.
		// Both rows share the same schema; second row's key column list slot is null.
		var b1 = new MapBean(); b1.name = "a"; b1.attrs = new LinkedHashMap<>(); b1.attrs.put("k", "v");
		var b2 = new MapBean(); b2.name = "b"; b2.attrs = null;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2));
		var out = (List<MapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, MapBean.class);
		assertEquals("a", out.get(0).name);
		assertEquals("v", out.get(0).attrs.get("k"));
		assertEquals("b", out.get(1).name);
	}

	// =================================================================================
	// aa. doParse scalar type branches: Boolean, Character targets.
	// =================================================================================

	@Test void aa01_parseBoolean() throws Exception {
		// Exercises inner==Boolean.class branch in the scalar-type guard (line 1060).
		var bytes = ParquetSerializer.DEFAULT.serialize(true);
		var result = ParquetParser.DEFAULT.parse(bytes, Boolean.class);
		assertTrue(result);
	}

	@Test void aa02_parseInteger() throws Exception {
		// Exercises Number.class.isAssignableFrom(inner) in the scalar-type guard.
		var bytes = ParquetSerializer.DEFAULT.serialize(42);
		var result = ParquetParser.DEFAULT.parse(bytes, Integer.class);
		assertEquals(42, result);
	}

	// =================================================================================
	// bb. ValueHolder target branches: isArray and isCollection with inner value (lines 1052-1070).
	// =================================================================================

	@Test void bb01_valueHolderToArray() throws Exception {
		// Single-row {value: "x"} parsed as String[] wraps the value in a list then converts to array.
		var bytes = ParquetSerializer.DEFAULT.serialize("x");
		var out = ParquetParser.DEFAULT.parse(bytes, String[].class);
		assertArrayEquals(new String[]{"x"}, out);
	}

	@Test void bb02_valueHolderToCollection() throws Exception {
		// Single-row {value: "y"} parsed as ArrayList<String> exercises the isCollection unwrap path.
		var bytes = ParquetSerializer.DEFAULT.serialize("y");
		var out = (List<String>) ParquetParser.DEFAULT.parse(bytes, ArrayList.class, String.class);
		assertEquals(List.of("y"), out);
	}

	// =================================================================================
	// cc. Optional doParse with empty rows (line 1098) and unwrapValueHolder (line 1658).
	// =================================================================================

	@Test void cc01_optionalTargetEmptyRows() throws Exception {
		// Parsing an empty Parquet file as Optional target returns Optional.empty().
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		Optional<?> out = (Optional<?>) ParquetParser.DEFAULT.parse(bytes, Optional.class, String.class);
		assertFalse(out.isPresent());
	}

	// =================================================================================
	// dd. doParse: Collection<Map>, array-null-inner, Optional-null-inner, Map root/multi-row.
	// =================================================================================

	@Test void dd01_parseCollectionOfMaps() throws Exception {
		// targetWantsScalar=false for List<Map> target — exercises the isCollection+isMap branch (line 217).
		var b1 = new MapBean(); b1.name = "a"; b1.attrs = map("k", "v");
		var b2 = new MapBean(); b2.name = "b"; b2.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2));
		var out = (List<Map<?,?>>) ParquetParser.DEFAULT.parse(bytes, List.class, Map.class);
		assertEquals(2, out.size());
		assertNotNull(out.get(0));
	}

	@Test void dd02_parseArrayEmptyInner() throws Exception {
		// Null scalar produces 0 rows; parse as String[] gives empty array.
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize((Object) null);
		var out = ParquetParser.DEFAULT.parse(bytes, String[].class);
		assertNotNull(out);
		assertEquals(0, out.length);
	}

	@Test void dd03_parseCollectionEmptyInner() throws Exception {
		// Null scalar produces 0 rows; parse as ArrayList<String> gives empty list.
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize((Object) null);
		var out = (List<String>) ParquetParser.DEFAULT.parse(bytes, ArrayList.class, String.class);
		assertNotNull(out);
		assertTrue(out.isEmpty());
	}

	@Test void dd04_parseOptionalNonEmptyRows() throws Exception {
		// Multiple rows with Optional target — exercises unwrapValueHolder path (line 272).
		var b = new MapBean(); b.name = "p"; b.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		Optional<?> out = (Optional<?>) ParquetParser.DEFAULT.parse(bytes, Optional.class, MapBean.class);
		assertTrue(out.isPresent());
	}

	@Test void dd05_parseMapTargetMultipleRows() throws Exception {
		// Two-row result with Map target — exercises rows.size()!=1 path in the type.isMap() block (line 262 false).
		var b1 = new MapBean(); b1.name = "a"; b1.attrs = map("x", "1");
		var b2 = new MapBean(); b2.name = "b"; b2.attrs = map("y", "2");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b1, b2));
		// Parsing a list of MapBeans as raw List — rows is size 2, doesn't take the size==1 path
		var out = (List<Map<?,?>>) ParquetParser.DEFAULT.parse(bytes, List.class, Map.class);
		assertEquals(2, out.size());
	}

	// =================================================================================
	// ee. prepareMapForBean branches: Map target (returns value), null type.
	// =================================================================================

	@Test void ee01_prepareMapForBeanMapTarget() throws Exception {
		// value is a Map and target type is Map — exercises the !type.isMap() false branch (line 1575).
		var in = new LinkedHashMap<String, String>();
		in.put("a", "1");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<String, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, String.class, String.class);
		assertEquals("1", out.get("a"));
	}

	@Test void ee02_characterTarget() throws Exception {
		// inner==Character.class exercises the Character branch in the scalar-type guard.
		var bytes = ParquetSerializer.DEFAULT.serialize('Z');
		var result = ParquetParser.DEFAULT.parse(bytes, Character.class);
		assertEquals('Z', (char) result);
	}

	// =================================================================================
	// ff. groupListBeanColumns — propClassMeta.isArray() branch (line 1362).
	// =================================================================================

	@Test void ff01_beanWithArrayOfBeansProp() throws Exception {
		// BeanArrayPropBean.items is Inner[] — an array (not collection) of beans.
		// groupListBeanColumns must enter the propClassMeta.isArray() branch to build
		// ListColumnInfo entries so the list-bean columns are merged correctly.
		var b = new BeanArrayPropBean();
		b.name = "x";
		var i1 = new Inner(); i1.v = "a"; i1.n = 1;
		var i2 = new Inner(); i2.v = "b"; i2.n = 2;
		b.items = new Inner[]{i1, i2};
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (List<BeanArrayPropBean>) ParquetParser.DEFAULT.parse(bytes, List.class, BeanArrayPropBean.class);
		assertEquals(1, out.size());
		assertEquals("x", out.get(0).name);
		assertNotNull(out.get(0).items);
		assertEquals(2, out.get(0).items.length);
		assertEquals("a", out.get(0).items[0].v);
		assertEquals(1, out.get(0).items[0].n);
		assertEquals("b", out.get(0).items[1].v);
		assertEquals(2, out.get(0).items[1].n);
	}

	// =================================================================================
	// gg. readDictionaryEncodedPage: short page buffer and invalid defLen branches (lines 1157-1170).
	// =================================================================================

	@Test void gg01_dictEncodedShortPageNoDefSection() throws Exception {
		// maxDefLevel=1 but decompressed.length<4 → defDecoder stays null → all values treated as present.
		// Page body: [1-byte idxBitWidth][RLE index bytes] — total length will be < 4.
		var dictionary = List.<Object>of("x");
		var idxEnc = new RleBitPackingEncoder(1);
		idxEnc.writeInt(0);
		var idxBytes = idxEnc.toByteArray();
		// page = [idxBitWidth=1][idxBytes] — 1 + idxBytes.length bytes (< 4)
		var page = new byte[1 + idxBytes.length];
		page[0] = 1; // idxBitWidth = 1
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);
		assertTrue(page.length < 4); // confirm short buffer
		var values = new ArrayList<Object>();
		ParquetParserSession.readDictionaryEncodedPage(page, 1, 1, 1, dictionary, values);
		assertEquals(List.of("x"), values);
	}

	@Test void gg04_dictEncodedOutOfRangeIndex() {
		// Dictionary index >= dictionary.size() triggers ParseException.
		// maxDefLevel=0 (no def section), idxBitWidth=1, encode index=1 but dictionary has only 1 entry (index 0 only).
		var dictionary = List.<Object>of("only");
		var idxEnc = new RleBitPackingEncoder(1);
		idxEnc.writeInt(1); // index=1 is out of range for a 1-entry dictionary
		var idxBytes = idxEnc.toByteArray();
		var page = new byte[1 + idxBytes.length];
		page[0] = 1; // idxBitWidth=1
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);
		var values = new ArrayList<Object>();
		assertThrows(org.apache.juneau.marshall.parser.ParseException.class, () ->
			ParquetParserSession.readDictionaryEncodedPage(page, 1, 0, 1, dictionary, values));
	}

	@Test void gg03_dictEncodedNegativeDefLen() throws org.apache.juneau.marshall.parser.ParseException {
		// maxDefLevel=1, decompressed.length>=4, defLen encodes as negative int32 (0xFFFFFFFF = -1)
		// → defLen >= 0 check fails → defDecoder stays null → pageValues=0 short-circuits.
		var dictionary = List.<Object>of("z");
		var page = new byte[5]; // length >= 4 so outer check passes
		page[0] = (byte)0xFF; page[1] = (byte)0xFF; page[2] = (byte)0xFF; page[3] = (byte)0xFF; // defLen = -1
		page[4] = 1; // idxBitWidth (off=0 since defDecoder=null)
		var values = new ArrayList<Object>();
		ParquetParserSession.readDictionaryEncodedPage(page, 0, 1, 1, dictionary, values);
		assertTrue(values.isEmpty());
	}

	@Test void gg02_dictEncodedInvalidDefLen() throws Exception {
		// maxDefLevel=1, decompressed.length>=4, but defLen (first 4 bytes) > decompressed.length - 4
		// → inner if-check fails → defDecoder stays null → index section starts at off=0.
		// We encode idxBitWidth in byte 0, then defLen bytes 1-3 encode large values; the only RLE index
		// follows; defDecoder=null so def is treated as maxDefLevel (value present) for each slot.
		var dictionary = List.<Object>of("y");
		var idxEnc = new RleBitPackingEncoder(1);
		idxEnc.writeInt(0);
		var idxBytes = idxEnc.toByteArray();
		// page[0]: idxBitWidth (also the low byte of defLen which is read first as LE int32)
		// We want defLen = idxBitWidth | (byte1<<8) | ... to be > page.length-4.
		// Simplest: set bytes 0-3 so defLen = 0x7FFFFFFF (huge), page length > 4.
		var page = new byte[4 + idxBytes.length];
		page[0] = (byte)0xFF; // low byte of defLen = 255
		page[1] = (byte)0xFF; // → defLen = 0xFFFF... which is negative as int32
		page[2] = (byte)0xFF;
		page[3] = (byte)0x7F; // → defLen = 0x7FFFFFFF > page.length - 4 → defLen check fails
		// Since defDecoder=null → off=0 → idxBitWidth = page[0] = 0xFF = 255
		// RleBitPackingDecoder with bitWidth=255 will throw or produce no values but pageValues=0 avoids that.
		// Simplest: pass pageValues=0 to skip the decode loop entirely.
		var values = new ArrayList<Object>();
		ParquetParserSession.readDictionaryEncodedPage(page, 0, 1, 1, dictionary, values);
		assertTrue(values.isEmpty()); // no pageValues to read
	}

	// =================================================================================
	// hh. doParse edge branches: scalar-as-map, raw collection, collection-newInstance-null,
	//     prepareMapForBean map-target, unwrapValueHolder non-map row, collapseOptionalValue.
	// =================================================================================

	@Test void hh01_scalarRowParsedAsMapTarget() throws Exception {
		// Serializing a scalar produces {value:"x"} rows; parsing as Map target hits the
		// isMap() branch where inner is NOT a Map → inner instanceof Map<?,?> = false (line 240).
		var bytes = ParquetSerializer.DEFAULT.serialize("hello");
		var out = (Map<?, ?>) ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>)Map.class);
		// Falls through to single-row Map branch and returns the {value:"hello"} row as-is.
		assertNotNull(out);
	}

	@Test void hh02_rawCollectionTargetNullElementType() throws Exception {
		// parse(bytes, Collection.class) → effectiveType is raw Collection → elementType == null
		// → line 205 elementType-null branch fires, elementType becomes Map.class.
		var b = new MapBean(); b.name = "x"; b.attrs = map();
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		var out = (Collection<?>) ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>)Collection.class);
		assertNotNull(out);
		assertFalse(out.isEmpty());
	}

	@Test void hh03_valueHolderToAbstractCollection() throws Exception {
		// Serializing a single scalar → {value:"v"} row; parsing as raw Collection (abstract interface)
		// means type.newInstance() returns null → coll = new ArrayList<>() fallback (line 229-230).
		var bytes = ParquetSerializer.DEFAULT.serialize("v");
		var out = (Collection<?>) ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>)Collection.class);
		assertNotNull(out);
		assertEquals(1, out.size());
	}

	@Test void hh04_prepareMapForBeanMapTarget() throws Exception {
		// prepareMapForBean with a Map value and a Map target type: type.isMap() = true
		// → falls through to return value as-is (line 1539 false branch on !type.isMap()).
		var in = new LinkedHashMap<String, Object>();
		in.put("a", "1");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<?, ?>) ParquetParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals("1", out.get("a"));
	}

	@Test void hh05_collapseOptionalValueBeanWithScalarInner() throws Exception {
		// collapseOptionalValue called with a bean target type but val is NOT a Map
		// (e.g., val is a String due to type mismatch) → falls through to return val (line 1507).
		// Achieved by serializing Optional<String> present and parsing as OptionalBeanProp
		// — the Optional<String> "a" property gets a String inner, not a Map.
		var o = new OptionalBeanProp();
		o.a = o("strval");
		o.b = null;
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(list(o));
		var out = (List<OptionalBeanProp>) ParquetParser.DEFAULT.parse(bytes, List.class, OptionalBeanProp.class);
		assertTrue(out.get(0).a.isPresent());
		assertEquals("strval", out.get(0).a.get());
	}

	@Test void hh06_listOfMapsSerializedAndParsedAsMap() throws Exception {
		// Serializing a List<Map> wraps each Map element in a ValueHolder row {value: {k:v}}.
		// Parsing with Map target enters the type.isMap() block; single {value: Map} row's inner
		// value is a Map, so convertToType(inner, type) is returned directly.
		var inner = new LinkedHashMap<String, Object>();
		inner.put("k", "v");
		var in = list(inner);
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<?, ?>) ParquetParser.DEFAULT.parse(bytes, (Class<Object>)(Class<?>)Map.class);
		assertNotNull(out);
	}

	// =================================================================================
	// ii. readDictionaryPage and readDataPageValues direct unit tests.
	// =================================================================================

	@Test void ii01_readDictionaryPageDirectly() throws Exception {
		// Call readDictionaryPage directly with TYPE_BYTE_ARRAY PLAIN-encoded bytes for two strings.
		// PLAIN encoding for BYTE_ARRAY uses a 4-byte little-endian length prefix followed by the UTF-8 bytes.
		var plain = new byte[]{
			2, 0, 0, 0, 'h', 'i',           // "hi"
			3, 0, 0, 0, 'b', 'y', 'e'       // "bye"
		};
		var dict = ParquetParserSession.readDictionaryPage(plain, 2, ParquetSchemaElement.TYPE_BYTE_ARRAY, false, false, false, null);
		assertEquals(List.of("hi", "bye"), dict);
	}

	@Test void ii02_readDataPageValuesDictEncoded() throws Exception {
		// readDataPageValues with dictEncoded=true delegates to readDictionaryEncodedPage; exercises line 1054.
		var dictionary = List.<Object>of("x", "y");
		// Page: [1-byte idxBitWidth=1][RLE indices for [0, 1]].
		var idxEnc = new RleBitPackingEncoder(1);
		idxEnc.writeInt(0); idxEnc.writeInt(1);
		var idxBytes = idxEnc.toByteArray();
		var page = new byte[1 + idxBytes.length];
		page[0] = 1; // idxBitWidth = 1
		System.arraycopy(idxBytes, 0, page, 1, idxBytes.length);
		var values = new ArrayList<Object>();
		ParquetParserSession.readDataPageValues(page, 2, 0, 1, ParquetSchemaElement.TYPE_BYTE_ARRAY,
			false, false, false, null, true, dictionary, values);
		assertEquals(Arrays.asList("x", "y"), values);
	}

}
