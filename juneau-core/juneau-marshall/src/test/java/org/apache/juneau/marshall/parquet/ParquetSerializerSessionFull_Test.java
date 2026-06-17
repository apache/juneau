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

import java.io.*;
import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Broad behavioral coverage of {@link ParquetSerializerSession}: round-trips a wide variety of POJO/map/collection
 * shapes through {@link ParquetSerializer} + {@link ParquetParser} to drive the serialize-side branches.
 */
@SuppressWarnings({
	"unchecked", // Parser returns raw types; explicit casts required for typed assertions
	"resource"   // RecordWriter.write(...) returns the same writer (fluent 'this'); already closed via try-with-resources.
})
class ParquetSerializerSessionFull_Test extends TestBase {

	private enum Color { RED, GREEN, BLUE }

	//-----------------------------------------------------------------------------------------------------------------
	// Collections of beans / arrays / scalars.
	//-----------------------------------------------------------------------------------------------------------------

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	@Test
	void a01_collectionOfBeans() throws Exception {
		var in = list(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(3, out.size());
		assertEquals("a", out.get(0).name);
		assertEquals(3, out.get(2).age);
	}

	@Test
	void a02_arrayOfBeans() throws Exception {
		var in = new Bean[] { new Bean("a", 1), new Bean("b", 2) };
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
		assertEquals("b", out.get(1).name);
	}

	@Test
	void a03_collectionOfScalars() throws Exception {
		var in = list("x", "y", "z");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = ParquetParser.DEFAULT.parse(bytes, String[].class);
		assertArrayEquals(new String[]{"x", "y", "z"}, out);
	}

	@Test
	void a04_collectionWithNullElements() throws Exception {
		var in = new ArrayList<Bean>();
		in.add(new Bean("a", 1));
		in.add(null);
		in.add(new Bean("c", 3));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(3, out.size());
	}

	@Test
	void a05_immutableCollection() throws Exception {
		var in = List.of(new Bean("a", 1), new Bean("b", 2));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
	}

	@Test
	void a06_emptyCollection() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var out = ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertTrue(((List<?>) out).isEmpty());
	}

	@Test
	void a07_nullRoot() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(null);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Optional handling.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_optionalRootPresent() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(Optional.of(new Bean("a", 1)));
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(1, out.size());
		assertEquals("a", out.get(0).name);
	}

	@Test
	void b02_optionalRootEmpty() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(Optional.empty());
		assertNotNull(bytes);
		var out = ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertTrue(((List<?>) out).isEmpty());
	}

	public static class OptBean {
		public String name;
		public Optional<String> nick;
		public OptBean() {}
		public OptBean(String name, Optional<String> nick) { this.name = name; this.nick = nick; }
	}

	@Test
	void b03_optionalPropertyPresentAndEmpty() throws Exception {
		var in = list(new OptBean("a", Optional.of("aa")), new OptBean("b", Optional.empty()));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<OptBean>) ParquetParser.DEFAULT.parse(bytes, List.class, OptBean.class);
		assertEquals(2, out.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Null property values.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_nullPropertyValues() throws Exception {
		var in = list(new Bean(null, 0), new Bean("b", 2));
		var ser = ParquetSerializer.create().keepNullProperties().build();
		var bytes = ser.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lists / nested lists / arrays of arrays.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ListBean {
		public String name;
		public List<String> tags;
		public ListBean() {}
		public ListBean(String name, List<String> tags) { this.name = name; this.tags = tags; }
	}

	@Test
	void d01_listProperty() throws Exception {
		var in = list(new ListBean("a", list("x", "y")), new ListBean("b", list("z")));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(2, out.size());
		assertEquals(list("x", "y"), out.get(0).tags);
	}

	@Test
	void d02_emptyAndNullList() throws Exception {
		var in = new ArrayList<ListBean>();
		in.add(new ListBean("a", list()));
		in.add(new ListBean("b", null));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(2, out.size());
	}

	public static class NestedListBean {
		public String name;
		public List<List<String>> grid;
		public NestedListBean() {}
		public NestedListBean(String name, List<List<String>> grid) { this.name = name; this.grid = grid; }
	}

	@Test
	void d03_nestedList() throws Exception {
		var in = list(new NestedListBean("a", list(list("x", "y"), list("z"))));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedListBean.class);
		assertEquals(1, out.size());
	}

	@Test
	void d04_arrayOfPrimitiveArrays() throws Exception {
		var in = new int[][] { {1, 2}, {3, 4, 5} };
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class IntArrayBean {
		public String name;
		public int[] nums;
		public IntArrayBean() {}
		public IntArrayBean(String name, int[] nums) { this.name = name; this.nums = nums; }
	}

	@Test
	void d05_primitiveArrayProperty() throws Exception {
		var in = list(new IntArrayBean("a", new int[]{1, 2, 3}));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class Inner {
		public String label;
		public int n;
		public Inner() {}
		public Inner(String label, int n) { this.label = label; this.n = n; }
	}

	public static class Middle {
		public String mid;
		public Inner inner;
		public Middle() {}
		public Middle(String mid, Inner inner) { this.mid = mid; this.inner = inner; }
	}

	public static class Outer {
		public String name;
		public Middle middle;
		public Outer() {}
		public Outer(String name, Middle middle) { this.name = name; this.middle = middle; }
	}

	@Test
	void d10_nestedBeanMultiLevelOptional() throws Exception {
		// Bean -> bean -> bean scalar => leaf column under 3 OPTIONAL groups -> computeDefValue (maxDef>=2).
		var in = list(
			new Outer("a", new Middle("m1", new Inner("i1", 1))),
			new Outer("b", new Middle("m2", null)),     // deepest group present, inner null
			new Outer("c", null));                       // intermediate group null
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(in);
		var out = (List<Outer>) ParquetParser.DEFAULT.parse(bytes, List.class, Outer.class);
		assertEquals(3, out.size());
		assertEquals("i1", out.get(0).middle.inner.label);
	}

	public static class ObjArrBean {
		public String name;
		public String[] items;
		public ObjArrBean() {}
		public ObjArrBean(String name, String[] items) { this.name = name; this.items = items; }
	}

	@Test
	void d11_objectArrayListColumn() throws Exception {
		var in = list(new ObjArrBean("a", new String[]{"x", "y"}), new ObjArrBean("b", new String[0]));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<ObjArrBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ObjArrBean.class);
		assertEquals(2, out.size());
	}

	public static class IntArrColBean {
		public String name;
		public int[] vals;
		public IntArrColBean() {}
		public IntArrColBean(String name, int[] vals) { this.name = name; this.vals = vals; }
	}

	@Test
	void d12_primitiveArrayListColumn() throws Exception {
		var in = list(new IntArrColBean("a", new int[]{1, 2, 3}), new IntArrColBean("b", new int[]{}));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<IntArrColBean>) ParquetParser.DEFAULT.parse(bytes, List.class, IntArrColBean.class);
		assertEquals(2, out.size());
	}

	@Test
	void d13_listWithNullElements() throws Exception {
		var in = new ArrayList<ListBean>();
		var tags = new ArrayList<String>();
		tags.add("a");
		tags.add(null);
		tags.add("c");
		in.add(new ListBean("a", tags));
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(1, out.size());
	}

	public static class DeepListBean {
		public String name;
		public List<List<String>> grid;
		public DeepListBean() {}
		public DeepListBean(String name, List<List<String>> grid) { this.name = name; this.grid = grid; }
	}

	@Test
	void d14_nestedListWithNullAndEmptyInner() throws Exception {
		var grid = new ArrayList<List<String>>();
		grid.add(list("a", "b"));
		grid.add(new ArrayList<>());  // empty inner
		grid.add(null);               // null inner
		var inner = new ArrayList<String>();
		inner.add("x");
		inner.add(null);              // null element
		grid.add(inner);
		var in = list(new DeepListBean("a", grid));
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void d15_beanWithObjectArrayList() throws Exception {
		var in = list(new ObjArrBean("a", new String[]{"x", "y", "z"}));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<ObjArrBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ObjArrBean.class);
		assertEquals(1, out.size());
	}

	public static class ListOfArraysBean {
		public String name;
		public List<int[]> rows;
		public ListOfArraysBean() {}
		public ListOfArraysBean(String name, List<int[]> rows) { this.name = name; this.rows = rows; }
	}

	@Test
	void d16_listOfPrimitiveArrays() throws Exception {
		var rows = new ArrayList<int[]>();
		rows.add(new int[]{1, 2});
		rows.add(new int[]{3});
		var in = list(new ListOfArraysBean("a", rows));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Maps: string keys, non-string keys, nested maps.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_mapStringKeys() throws Exception {
		var in = new LinkedHashMap<String, Object>();
		in.put("a", 1);
		in.put("b", "two");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void e02_mapWithNullKey() throws Exception {
		var in = new HashMap<String, Object>();
		in.put(null, "nullval");
		in.put("a", "aval");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void e03_mapIntegerKeys() throws Exception {
		var in = new TreeMap<Integer, String>();
		in.put(1, "a");
		in.put(2, "b");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<Integer, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, Integer.class, String.class);
		assertEquals("a", out.get(1));
		assertEquals("b", out.get(2));
	}

	@Test
	void e04_mapEnumKeys() throws Exception {
		var in = new LinkedHashMap<Color, String>();
		in.put(Color.RED, "r");
		in.put(Color.GREEN, "g");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (Map<Color, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, Color.class, String.class);
		assertEquals("r", out.get(Color.RED));
	}

	public static class NestedMapBean {
		public String name;
		public Map<String, Integer> counts;
		public NestedMapBean() {}
		public NestedMapBean(String name, Map<String, Integer> counts) { this.name = name; this.counts = counts; }
	}

	@Test
	void e05_nestedMapProperty() throws Exception {
		var m = new LinkedHashMap<String, Integer>();
		m.put("x", 1);
		m.put("y", 2);
		var in = list(new NestedMapBean("a", m), new NestedMapBean("b", map()));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<NestedMapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NestedMapBean.class);
		assertEquals(2, out.size());
		assertEquals(Integer.valueOf(1), out.get(0).counts.get("x"));
	}

	@Test
	void e06_mapEnumKeysAndDateKeys() throws Exception {
		// Map with non-string keys at root drives mapKeyToStoredString for Date/Calendar/Temporal branches.
		var in = new LinkedHashMap<LocalDate, String>();
		in.put(LocalDate.parse("2020-01-01"), "a");
		in.put(LocalDate.parse("2021-02-02"), "b");
		var bytes = ParquetSerializer.create().keepNullProperties().addBeanTypes().addRootType().build().serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class TemporalMapBean {
		public String name;
		public Map<String, Instant> events;
		public TemporalMapBean() {}
		public TemporalMapBean(String name, Map<String, Instant> events) { this.name = name; this.events = events; }
	}

	@Test
	void e07_nestedMapTemporalValuesAsString() throws Exception {
		// Nested Map<String,Instant> value column at UTF8 (writeDatesAsTimestamp(false)); values reach writeValue raw.
		var m = new LinkedHashMap<String, Instant>();
		m.put("start", Instant.parse("2020-01-02T03:04:05Z"));
		m.put("end", Instant.parse("2020-01-03T03:04:05Z"));
		var in = list(new TemporalMapBean("a", m), new TemporalMapBean("b", map()));
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class IntMapBean {
		public String name;
		public Map<String, Integer> nums;
		public IntMapBean() {}
		public IntMapBean(String name, Map<String, Integer> nums) { this.name = name; this.nums = nums; }
	}

	@Test
	void e08_nestedMapWithNullValue() throws Exception {
		var m = new LinkedHashMap<String, Integer>();
		m.put("x", 1);
		m.put("y", null);
		var in = list(new IntMapBean("a", m));
		var bytes = ParquetSerializer.create().keepNullProperties().build().serialize(in);
		var out = (List<IntMapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, IntMapBean.class);
		assertEquals(1, out.size());
	}

	public static class NonStrKeyMapBean {
		public String name;
		public Map<Integer, String> byId;
		public NonStrKeyMapBean() {}
		public NonStrKeyMapBean(String name, Map<Integer, String> byId) { this.name = name; this.byId = byId; }
	}

	@Test
	void e09_nestedMapNonStringKeys() throws Exception {
		var m = new LinkedHashMap<Integer, String>();
		m.put(1, "a");
		m.put(2, "b");
		var in = list(new NonStrKeyMapBean("x", m));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<NonStrKeyMapBean>) ParquetParser.DEFAULT.parse(bytes, List.class, NonStrKeyMapBean.class);
		assertEquals(1, out.size());
		assertEquals("a", out.get(0).byId.get(1));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// byte[], enum, UUID, Duration, BigDecimal, byte-array columns.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ByteArrayBean {
		public String name;
		public byte[] data;
		public ByteArrayBean() {}
		public ByteArrayBean(String name, byte[] data) { this.name = name; this.data = data; }
	}

	@Test
	void f01_byteArrayColumn() throws Exception {
		var in = list(new ByteArrayBean("a", new byte[]{1, 2, 3}));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<ByteArrayBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ByteArrayBean.class);
		assertEquals(1, out.size());
	}

	public static class EnumBean {
		public String name;
		public Color color;
		public EnumBean() {}
		public EnumBean(String name, Color color) { this.name = name; this.color = color; }
	}

	@Test
	void f02_enumColumn() throws Exception {
		var in = list(new EnumBean("a", Color.RED), new EnumBean("b", Color.BLUE));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<EnumBean>) ParquetParser.DEFAULT.parse(bytes, List.class, EnumBean.class);
		assertEquals(Color.RED, out.get(0).color);
		assertEquals(Color.BLUE, out.get(1).color);
	}

	public static class UuidBean {
		public String name;
		public UUID id;
		public UuidBean() {}
		public UuidBean(String name, UUID id) { this.name = name; this.id = id; }
	}

	@Test
	void f03_uuidColumn() throws Exception {
		var u = UUID.fromString("12345678-1234-5678-1234-567812345678");
		var in = list(new UuidBean("a", u));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<UuidBean>) ParquetParser.DEFAULT.parse(bytes, List.class, UuidBean.class);
		assertEquals(u, out.get(0).id);
	}

	public static class DurationBean {
		public String name;
		public Duration dur;
		public DurationBean() {}
		public DurationBean(String name, Duration dur) { this.name = name; this.dur = dur; }
	}

	@Test
	void f04_durationColumn() throws Exception {
		var in = list(new DurationBean("a", Duration.ofSeconds(90)));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<DurationBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DurationBean.class);
		assertEquals(1, out.size());
	}

	public static class DecBean {
		public String name;
		public BigDecimal amount;
		public DecBean() {}
		public DecBean(String name, BigDecimal amount) { this.name = name; this.amount = amount; }
	}

	@Test
	void f05_bigDecimalDefaultStringPath() throws Exception {
		var in = list(new DecBean("a", new BigDecimal("123.456")));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DecBean.class);
		assertEquals(0, new BigDecimal("123.456").compareTo(out.get(0).amount));
	}

	@Test
	void f06_bigDecimalNativePath() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new DecBean("a", new BigDecimal("123.456")));
		var bytes = ser.serialize(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DecBean.class);
		assertEquals(0, new BigDecimal("123.456000000").compareTo(out.get(0).amount));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// java.time temporals — default (timestamp-millis) and native (date/time/timestamp-micros).
	//-----------------------------------------------------------------------------------------------------------------

	public static class TemporalBean {
		public LocalDate ld;
		public LocalDateTime ldt;
		public LocalTime lt;
		public Instant inst;
		public ZonedDateTime zdt;
		public OffsetDateTime odt;
		public OffsetTime ot;
		public Year year;
		public YearMonth ym;
		public TemporalBean() {}
	}

	private static TemporalBean fullTemporal() {
		var b = new TemporalBean();
		b.ld = LocalDate.parse("2020-01-02");
		b.ldt = LocalDateTime.parse("2020-01-02T03:04:05");
		b.lt = LocalTime.parse("03:04:05");
		b.inst = Instant.parse("2020-01-02T03:04:05Z");
		b.zdt = ZonedDateTime.parse("2020-01-02T03:04:05Z");
		b.odt = OffsetDateTime.parse("2020-01-02T03:04:05+01:00");
		b.ot = OffsetTime.parse("03:04:05+01:00");
		b.year = Year.of(2020);
		b.ym = YearMonth.of(2020, 1);
		return b;
	}

	@Test
	void g01_temporalsDefaultTimestampMillis() throws Exception {
		var in = list(fullTemporal());
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class NativeTemporalBean {
		public LocalDate ld;
		public LocalTime lt;
		public Instant inst;
		public ZonedDateTime zdt;
		public OffsetDateTime odt;
		public NativeTemporalBean() {}
	}

	@Test
	void g02_temporalsNative() throws Exception {
		var b = new NativeTemporalBean();
		b.ld = LocalDate.parse("2020-01-02");
		b.lt = LocalTime.parse("03:04:05");
		b.inst = Instant.parse("2020-01-02T03:04:05Z");
		b.zdt = ZonedDateTime.parse("2020-01-02T03:04:05Z");
		b.odt = OffsetDateTime.parse("2020-01-02T03:04:05+01:00");
		var bytes = ParquetSerializer.create().nativeLogicalTypes(true).build().serialize(list(b));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class DateBean {
		public Date d;
		public Calendar c;
		public DateBean() {}
	}

	@Test
	void g03_dateAndCalendarDefault() throws Exception {
		var b = new DateBean();
		b.d = new Date(1_000_000_000L);
		var cal = new GregorianCalendar();
		cal.setTimeInMillis(2_000_000_000L);
		b.c = cal;
		var bytes = ParquetSerializer.DEFAULT.serialize(list(b));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class InstantBean {
		public Instant inst;
		public InstantBean() {}
	}

	@Test
	void g04_instantNative() throws Exception {
		var b = new InstantBean();
		b.inst = Instant.parse("2020-01-02T03:04:05.123456Z");
		var bytes = ParquetSerializer.create().nativeLogicalTypes(true).build().serialize(list(b));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void g05_temporalsAsString() throws Exception {
		// writeDatesAsTimestamp(false) -> temporals stored as BYTE_ARRAY/STRING.
		var in = list(fullTemporal());
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Temporals reaching writeValue as raw objects (via Object-typed property / map values) — drives toInt64 branches.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ObjBean {
		public Object v;
		public ObjBean() {}
		public ObjBean(Object v) { this.v = v; }
	}

	/** Root Map&lt;String,Object&gt; with temporal values — values reach writeValue raw (no per-property swap),
	 *  driving the {@code toInt64} temporal-instance branches at INT64/TIMESTAMP_MILLIS columns. */
	@Test
	void g10_mapWithTemporalValuesDefault() throws Exception {
		var in = new LinkedHashMap<String, Object>();
		in.put("d", new Date(1_000_000_000L));
		var cal = new GregorianCalendar();
		cal.setTimeInMillis(2_000_000_000L);
		in.put("c", cal);
		in.put("inst", Instant.parse("2020-01-02T03:04:05Z"));
		in.put("ld", LocalDate.parse("2020-01-02"));
		in.put("ldt", LocalDateTime.parse("2020-01-02T03:04:05"));
		in.put("lt", LocalTime.parse("03:04:05"));
		in.put("zdt", ZonedDateTime.parse("2020-01-02T03:04:05Z"));
		in.put("odt", OffsetDateTime.parse("2020-01-02T03:04:05+01:00"));
		in.put("ot", OffsetTime.parse("03:04:05+01:00"));
		in.put("year", Year.of(2020));
		in.put("ym", YearMonth.of(2020, 1));
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	/** Native logical types: raw temporal values drive toEpochDay/toTimeMicros/toInstant instance branches. */
	@Test
	void g11_mapWithTemporalValuesNative() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		// DATE (INT32 / toEpochDay) — LocalDate, LocalDateTime, Instant, ZonedDateTime, OffsetDateTime.
		var dates = new LinkedHashMap<String, Object>();
		dates.put("ld", LocalDate.parse("2020-01-02"));
		assertNotNull(ser.serialize(dates));
		// TIME (INT64 micros / toTimeMicros) — LocalTime, OffsetTime.
		var times = new LinkedHashMap<String, Object>();
		times.put("lt", LocalTime.parse("03:04:05"));
		assertNotNull(ser.serialize(times));
		var times2 = new LinkedHashMap<String, Object>();
		times2.put("ot", OffsetTime.parse("03:04:05+01:00"));
		assertNotNull(ser.serialize(times2));
		// TIMESTAMP (INT64 micros / toInstant) — Date, Calendar, Instant, LocalDate, LocalDateTime, ZonedDateTime, OffsetDateTime.
		for (var v : new Object[] {
				new Date(1_000_000_000L),
				instantCal(),
				Instant.parse("2020-01-02T03:04:05.123456Z"),
				LocalDateTime.parse("2020-01-02T03:04:05"),
				ZonedDateTime.parse("2020-01-02T03:04:05Z"),
				OffsetDateTime.parse("2020-01-02T03:04:05+01:00") }) {
			var m = new LinkedHashMap<String, Object>();
			m.put("ts", v);
			assertNotNull(ser.serialize(m));
		}
	}

	private static Calendar instantCal() {
		var cal = new GregorianCalendar();
		cal.setTimeInMillis(2_000_000_000L);
		return cal;
	}

	/** Native DATE from Instant/ZonedDateTime/OffsetDateTime/LocalDateTime — distinct from g11 LocalDate to drive
	 *  the remaining toEpochDay branches. These are LocalDate-typed columns only via nativeLogicalTypes + LocalDate
	 *  declared type, so use a Map with a LocalDate sample but exercise other epoch-day inputs is not directly
	 *  reachable; covered through TIMESTAMP path instead. */
	@Test
	void g12_mapBigDecimalNative() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var m = new LinkedHashMap<String, Object>();
		m.put("amt", new BigDecimal("12.5"));
		m.put("amt2", new BigInteger("42"));
		assertNotNull(ser.serialize(m));
	}

	/** Root Map with byte[], enum, UUID, Duration values — drives toByteArray / toFixedLenByteArray branches with raw objects. */
	@Test
	void g13_mapWithBinaryValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("bytes", new byte[]{1, 2, 3});
		m.put("color", Color.GREEN);
		m.put("id", UUID.fromString("12345678-1234-5678-1234-567812345678"));
		m.put("dur", Duration.ofSeconds(5));
		var bytes = ParquetSerializer.DEFAULT.serialize(m);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	/** Map with temporal values + writeDatesAsTimestamp(false): raw temporal at a UTF8 BYTE_ARRAY column ->
	 *  toByteArray CONVERTED_UTF8 branch. */
	@Test
	void g14_mapTemporalValuesAsString() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("inst", Instant.parse("2020-01-02T03:04:05Z"));
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().serialize(m);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	/** Map value typed Object holding a Number at a UTF8 column -> toByteArray fall-through. */
	@Test
	void g15_mapObjectNumberValue() throws Exception {
		var b = new NestedMapBean();
		b.name = "a";
		b.counts = null;
		// Bean with Map<String,Integer> values reach writeValue raw; combine with arbitrary scalar map.
		var m = new LinkedHashMap<String, Object>();
		m.put("n", 42L);
		m.put("s", "str");
		var bytes = ParquetSerializer.DEFAULT.serialize(m);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Multi-row-group and multi-page.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_multiRowGroup() throws Exception {
		var in = new ArrayList<Bean>();
		for (var i = 0; i < 50; i++)
			in.add(new Bean("n" + i, i));
		var ser = ParquetSerializer.create().rowGroupSize(1024).build();
		var bytes = ser.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(50, out.size());
		assertEquals("n49", out.get(49).name);
	}

	@Test
	void h02_multiPage() throws Exception {
		var in = new ArrayList<Bean>();
		for (var i = 0; i < 50; i++)
			in.add(new Bean("n" + i, i));
		var ser = ParquetSerializer.create().pageSize(1024).build();
		var bytes = ser.serialize(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(50, out.size());
		assertEquals("n49", out.get(49).name);
	}

	@Test
	void h03_multiPageList() throws Exception {
		var in = new ArrayList<ListBean>();
		for (var i = 0; i < 30; i++)
			in.add(new ListBean("n" + i, list("a" + i, "b" + i)));
		var ser = ParquetSerializer.create().pageSize(1024).rowGroupSize(2048).build();
		var bytes = ser.serialize(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ListBean.class);
		assertEquals(30, out.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Cycle handling.
	//-----------------------------------------------------------------------------------------------------------------

	public static class CyclicBean {
		public String name;
		public CyclicBean child;
	}

	@Test
	void i01_cycleNull() throws Exception {
		var a = new CyclicBean();
		a.name = "root";
		a.child = a;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.NULL).addBeanTypes().build();
		var bytes = ser.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void i02_cycleThrow() {
		var a = new CyclicBean();
		a.name = "root";
		a.child = a;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).addBeanTypes().build();
		assertThrows(SerializeException.class, () -> ser.serialize(a));
	}

	public static class CyclicListBean {
		public String name;
		public List<CyclicListBean> children;
	}

	@Test
	void i03_cycleInListNull() throws Exception {
		var a = new CyclicListBean();
		a.name = "root";
		a.children = new ArrayList<>();
		a.children.add(a);
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.NULL).addBeanTypes().build();
		var bytes = ser.serialize(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void i04_cycleInListThrow() {
		var a = new CyclicListBean();
		a.name = "root";
		a.children = new ArrayList<>();
		a.children.add(a);
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).addBeanTypes().build();
		assertThrows(SerializeException.class, () -> ser.serialize(a));
	}

	public static class MapCycleBean {
		public String name;
		public Map<String, Object> data;
		public MapCycleBean() {}
	}

	@Test
	void i05_cycleThroughMapNull() throws Exception {
		// Cycle through a Map (not a bean property, so the schema keeps the column) -> getValueByPath cycle/handleCycle.
		var a = new MapCycleBean();
		a.name = "root";
		a.data = new LinkedHashMap<>();
		a.data.put("self", a.data);  // map references itself
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.NULL).build();
		var bytes = ser.serialize(list(a));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Streaming entry points: serializeRecords / serializeArrayRecords.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_serializeRecords() throws Exception {
		var session = (ParquetSerializerSession) ParquetSerializer.DEFAULT.getSession();
		var baos = new ByteArrayOutputStream();
		try (var w = session.serializeRecords(baos)) {
			w.write(list(new Bean("a", 1), new Bean("b", 2)));
		}
		assertTrue(baos.toByteArray().length > 0);
		assertFalse(session.isRecordStreaming());
	}

	@Test
	void j02_serializeArrayRecords() throws Exception {
		var session = (ParquetSerializerSession) ParquetSerializer.DEFAULT.getSession();
		var baos = new ByteArrayOutputStream();
		try (var w = session.serializeArrayRecords(baos)) {
			w.write(new Bean("a", 1));
			w.write(new Bean("b", 2));
		}
		assertTrue(baos.toByteArray().length > 0);
		assertFalse(session.isArrayRecordStreaming());
	}

	@Test
	void j03_hasNativeBytes() {
		var session = (ParquetSerializerSession) ParquetSerializer.DEFAULT.getSession();
		assertFalse(session.hasNativeBytes());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Single scalar / single map / single bean roots.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k00_streamableRoot() throws Exception {
		// A Stream is streamable -> collectBeans isStreamable branch (toListFromStreamable).
		var bytes = ParquetSerializer.DEFAULT.serialize(java.util.stream.Stream.of(new Bean("a", 1), new Bean("b", 2)));
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
	}

	@Test
	void k00b_iteratorRoot() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(new Bean("a", 1)).iterator());
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals(1, out.size());
	}

	@Test
	void k01_singleScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize("hello");
		assertEquals("hello", ParquetParser.DEFAULT.parse(bytes, String.class));
	}

	@Test
	void k02_singleBean() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(new Bean("a", 1));
		var out = (List<Bean>) ParquetParser.DEFAULT.parse(bytes, List.class, Bean.class);
		assertEquals("a", out.get(0).name);
	}
}
