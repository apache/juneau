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

import java.io.*;
import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(3, out.size());
		assertEquals("a", out.get(0).name);
		assertEquals(3, out.get(2).age);
	}

	@Test
	void a02_arrayOfBeans() throws Exception {
		var in = new Bean[] { new Bean("a", 1), new Bean("b", 2) };
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
		assertEquals("b", out.get(1).name);
	}

	@Test
	void a03_collectionOfScalars() throws Exception {
		var in = list("x", "y", "z");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = ParquetParser.DEFAULT.read(bytes, String[].class);
		assertArrayEquals(new String[]{"x", "y", "z"}, out);
	}

	@Test
	void a04_collectionWithNullElements() throws Exception {
		var in = new ArrayList<Bean>();
		in.add(new Bean("a", 1));
		in.add(null);
		in.add(new Bean("c", 3));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(3, out.size());
	}

	@Test
	void a05_immutableCollection() throws Exception {
		var in = List.of(new Bean("a", 1), new Bean("b", 2));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
	}

	@Test
	void a06_emptyCollection() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list());
		var out = ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertTrue(((List<?>) out).isEmpty());
	}

	@Test
	void a07_nullRoot() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(null);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Optional handling.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_optionalRootPresent() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(o(new Bean("a", 1)));
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(1, out.size());
		assertEquals("a", out.get(0).name);
	}

	@Test
	void b02_optionalRootEmpty() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(oe());
		assertNotNull(bytes);
		var out = ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
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
		var in = list(new OptBean("a", o("aa")), new OptBean("b", oe()));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<OptBean>) ParquetParser.DEFAULT.read(bytes, List.class, OptBean.class);
		assertEquals(2, out.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Null property values.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_nullPropertyValues() throws Exception {
		var in = list(new Bean(null, 0), new Bean("b", 2));
		var ser = ParquetSerializer.create().keepNullProperties().build();
		var bytes = ser.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.read(bytes, List.class, ListBean.class);
		assertEquals(2, out.size());
		assertEquals(list("x", "y"), out.get(0).tags);
	}

	@Test
	void d02_emptyAndNullList() throws Exception {
		var in = new ArrayList<ListBean>();
		in.add(new ListBean("a", list()));
		in.add(new ListBean("b", null));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.read(bytes, List.class, ListBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<NestedListBean>) ParquetParser.DEFAULT.read(bytes, List.class, NestedListBean.class);
		assertEquals(1, out.size());
	}

	@Test
	void d04_arrayOfPrimitiveArrays() throws Exception {
		var in = new int[][] { {1, 2}, {3, 4, 5} };
		var bytes = ParquetSerializer.DEFAULT.write(in);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
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
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		var out = (List<Outer>) ParquetParser.DEFAULT.read(bytes, List.class, Outer.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<ObjArrBean>) ParquetParser.DEFAULT.read(bytes, List.class, ObjArrBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<IntArrColBean>) ParquetParser.DEFAULT.read(bytes, List.class, IntArrColBean.class);
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
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.read(bytes, List.class, ListBean.class);
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
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void d15_beanWithObjectArrayList() throws Exception {
		var in = list(new ObjArrBean("a", new String[]{"x", "y", "z"}));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<ObjArrBean>) ParquetParser.DEFAULT.read(bytes, List.class, ObjArrBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Maps: string keys, non-string keys, nested maps.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_mapStringKeys() throws Exception {
		var in = new LinkedHashMap<String,Object>();
		in.put("a", 1);
		in.put("b", "two");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void e02_mapWithNullKey() throws Exception {
		var in = new HashMap<String,Object>();
		in.put(null, "nullval");
		in.put("a", "aval");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void e03_mapIntegerKeys() throws Exception {
		var in = new TreeMap<Integer,String>();
		in.put(1, "a");
		in.put(2, "b");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<Integer,String>) ParquetParser.DEFAULT.read(bytes, Map.class, Integer.class, String.class);
		assertEquals("a", out.get(1));
		assertEquals("b", out.get(2));
	}

	@Test
	void e04_mapEnumKeys() throws Exception {
		var in = new LinkedHashMap<Color,String>();
		in.put(Color.RED, "r");
		in.put(Color.GREEN, "g");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (Map<Color,String>) ParquetParser.DEFAULT.read(bytes, Map.class, Color.class, String.class);
		assertEquals("r", out.get(Color.RED));
	}

	public static class NestedMapBean {
		public String name;
		public Map<String,Integer> counts;
		public NestedMapBean() {}
		public NestedMapBean(String name, Map<String,Integer> counts) { this.name = name; this.counts = counts; }
	}

	@Test
	void e05_nestedMapProperty() throws Exception {
		var m = new LinkedHashMap<String,Integer>();
		m.put("x", 1);
		m.put("y", 2);
		var in = list(new NestedMapBean("a", m), new NestedMapBean("b", map()));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<NestedMapBean>) ParquetParser.DEFAULT.read(bytes, List.class, NestedMapBean.class);
		assertEquals(2, out.size());
		assertEquals(Integer.valueOf(1), out.get(0).counts.get("x"));
	}

	@Test
	void e06_mapEnumKeysAndDateKeys() throws Exception {
		// Map with non-string keys at root drives mapKeyToStoredString for Date/Calendar/Temporal branches.
		var in = new LinkedHashMap<LocalDate,String>();
		in.put(LocalDate.parse("2020-01-01"), "a");
		in.put(LocalDate.parse("2021-02-02"), "b");
		var bytes = ParquetSerializer.create().keepNullProperties().addBeanTypes().addRootType().build().write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class TemporalMapBean {
		public String name;
		public Map<String,Instant> events;
		public TemporalMapBean() {}
		public TemporalMapBean(String name, Map<String,Instant> events) { this.name = name; this.events = events; }
	}

	@Test
	void e07_nestedMapTemporalValuesAsString() throws Exception {
		// Nested Map<String,Instant> value column at UTF8 (writeDatesAsTimestamp(false)); values reach writeValue raw.
		var m = new LinkedHashMap<String,Instant>();
		m.put("start", Instant.parse("2020-01-02T03:04:05Z"));
		m.put("end", Instant.parse("2020-01-03T03:04:05Z"));
		var in = list(new TemporalMapBean("a", m), new TemporalMapBean("b", map()));
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class IntMapBean {
		public String name;
		public Map<String,Integer> nums;
		public IntMapBean() {}
		public IntMapBean(String name, Map<String,Integer> nums) { this.name = name; this.nums = nums; }
	}

	@Test
	void e08_nestedMapWithNullValue() throws Exception {
		var m = new LinkedHashMap<String,Integer>();
		m.put("x", 1);
		m.put("y", null);
		var in = list(new IntMapBean("a", m));
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		var out = (List<IntMapBean>) ParquetParser.DEFAULT.read(bytes, List.class, IntMapBean.class);
		assertEquals(1, out.size());
	}

	public static class NonStrKeyMapBean {
		public String name;
		public Map<Integer,String> byId;
		public NonStrKeyMapBean() {}
		public NonStrKeyMapBean(String name, Map<Integer,String> byId) { this.name = name; this.byId = byId; }
	}

	@Test
	void e09_nestedMapNonStringKeys() throws Exception {
		var m = new LinkedHashMap<Integer,String>();
		m.put(1, "a");
		m.put(2, "b");
		var in = list(new NonStrKeyMapBean("x", m));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<NonStrKeyMapBean>) ParquetParser.DEFAULT.read(bytes, List.class, NonStrKeyMapBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<ByteArrayBean>) ParquetParser.DEFAULT.read(bytes, List.class, ByteArrayBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<EnumBean>) ParquetParser.DEFAULT.read(bytes, List.class, EnumBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<UuidBean>) ParquetParser.DEFAULT.read(bytes, List.class, UuidBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<DurationBean>) ParquetParser.DEFAULT.read(bytes, List.class, DurationBean.class);
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
		var bytes = ParquetSerializer.DEFAULT.write(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.read(bytes, List.class, DecBean.class);
		assertEquals(0, new BigDecimal("123.456").compareTo(out.get(0).amount));
	}

	@Test
	void f06_bigDecimalNativePath() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new DecBean("a", new BigDecimal("123.456")));
		var bytes = ser.write(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.read(bytes, List.class, DecBean.class);
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
		public TemporalBean() { /* no-op */ }
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
		b.ym = YearMonth.of(2020, Month.JANUARY);
		return b;
	}

	@Test
	void g01_temporalsDefaultTimestampMillis() throws Exception {
		var in = list(fullTemporal());
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class NativeTemporalBean {
		public LocalDate ld;
		public LocalTime lt;
		public Instant inst;
		public ZonedDateTime zdt;
		public OffsetDateTime odt;
		public NativeTemporalBean() { /* no-op */ }
	}

	@Test
	void g02_temporalsNative() throws Exception {
		var b = new NativeTemporalBean();
		b.ld = LocalDate.parse("2020-01-02");
		b.lt = LocalTime.parse("03:04:05");
		b.inst = Instant.parse("2020-01-02T03:04:05Z");
		b.zdt = ZonedDateTime.parse("2020-01-02T03:04:05Z");
		b.odt = OffsetDateTime.parse("2020-01-02T03:04:05+01:00");
		var bytes = ParquetSerializer.create().nativeLogicalTypes(true).build().write(list(b));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class DateBean {
		public Date d;
		public Calendar c;
		public DateBean() { /* no-op */ }
	}

	@Test
	void g03_dateAndCalendarDefault() throws Exception {
		var b = new DateBean();
		b.d = new Date(1_000_000_000L);
		var cal = new GregorianCalendar();
		cal.setTimeInMillis(2_000_000_000L);
		b.c = cal;
		var bytes = ParquetSerializer.DEFAULT.write(list(b));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class InstantBean {
		public Instant inst;
		public InstantBean() { /* no-op */ }
	}

	@Test
	void g04_instantNative() throws Exception {
		var b = new InstantBean();
		b.inst = Instant.parse("2020-01-02T03:04:05.123456Z");
		var bytes = ParquetSerializer.create().nativeLogicalTypes(true).build().write(list(b));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void g05_temporalsAsString() throws Exception {
		// writeDatesAsTimestamp(false) -> temporals stored as BYTE_ARRAY/STRING.
		var in = list(fullTemporal());
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().write(in);
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
		var in = new LinkedHashMap<String,Object>();
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
		in.put("ym", YearMonth.of(2020, Month.JANUARY));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	/** Native logical types: raw temporal values drive toEpochDay/toTimeMicros/toInstant instance branches. */
	@Test
	void g11_mapWithTemporalValuesNative() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		// DATE (INT32 / toEpochDay) — LocalDate, LocalDateTime, Instant, ZonedDateTime, OffsetDateTime.
		var dates = new LinkedHashMap<String,Object>();
		dates.put("ld", LocalDate.parse("2020-01-02"));
		assertNotNull(ser.write(dates));
		// TIME (INT64 micros / toTimeMicros) — LocalTime, OffsetTime.
		var times = new LinkedHashMap<String,Object>();
		times.put("lt", LocalTime.parse("03:04:05"));
		assertNotNull(ser.write(times));
		var times2 = new LinkedHashMap<String,Object>();
		times2.put("ot", OffsetTime.parse("03:04:05+01:00"));
		assertNotNull(ser.write(times2));
		// TIMESTAMP (INT64 micros / toInstant) — Date, Calendar, Instant, LocalDate, LocalDateTime, ZonedDateTime, OffsetDateTime.
		for (var v : new Object[] {
				new Date(1_000_000_000L),
				instantCal(),
				Instant.parse("2020-01-02T03:04:05.123456Z"),
				LocalDateTime.parse("2020-01-02T03:04:05"),
				ZonedDateTime.parse("2020-01-02T03:04:05Z"),
				OffsetDateTime.parse("2020-01-02T03:04:05+01:00") }) {
			var m = new LinkedHashMap<String,Object>();
			m.put("ts", v);
			assertNotNull(ser.write(m));
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
		var m = new LinkedHashMap<String,Object>();
		m.put("amt", new BigDecimal("12.5"));
		m.put("amt2", new BigInteger("42"));
		assertNotNull(ser.write(m));
	}

	/** Root Map with byte[], enum, UUID, Duration values — drives toByteArray / toFixedLenByteArray branches with raw objects. */
	@Test
	void g13_mapWithBinaryValues() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		m.put("bytes", new byte[]{1, 2, 3});
		m.put("color", Color.GREEN);
		m.put("id", UUID.fromString("12345678-1234-5678-1234-567812345678"));
		m.put("dur", Duration.ofSeconds(5));
		var bytes = ParquetSerializer.DEFAULT.write(m);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	/** Map with temporal values + writeDatesAsTimestamp(false): raw temporal at a UTF8 BYTE_ARRAY column ->
	 *  toByteArray CONVERTED_UTF8 branch. */
	@Test
	void g14_mapTemporalValuesAsString() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		m.put("inst", Instant.parse("2020-01-02T03:04:05Z"));
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().write(m);
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
		var m = new LinkedHashMap<String,Object>();
		m.put("n", 42L);
		m.put("s", "str");
		var bytes = ParquetSerializer.DEFAULT.write(m);
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
		var bytes = ser.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(50, out.size());
		assertEquals("n49", out.get(49).name);
	}

	@Test
	void h02_multiPage() throws Exception {
		var in = new ArrayList<Bean>();
		for (var i = 0; i < 50; i++)
			in.add(new Bean("n" + i, i));
		var ser = ParquetSerializer.create().pageSize(1024).build();
		var bytes = ser.write(in);
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(50, out.size());
		assertEquals("n49", out.get(49).name);
	}

	@Test
	void h03_multiPageList() throws Exception {
		var in = new ArrayList<ListBean>();
		for (var i = 0; i < 30; i++)
			in.add(new ListBean("n" + i, list("a" + i, "b" + i)));
		var ser = ParquetSerializer.create().pageSize(1024).rowGroupSize(2048).build();
		var bytes = ser.write(in);
		var out = (List<ListBean>) ParquetParser.DEFAULT.read(bytes, List.class, ListBean.class);
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
		var bytes = ser.write(a);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void i02_cycleThrow() {
		var a = new CyclicBean();
		a.name = "root";
		a.child = a;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).addBeanTypes().build();
		assertThrows(SerializeException.class, () -> ser.write(a));
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
		var bytes = ser.write(a);
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
		assertThrows(SerializeException.class, () -> ser.write(a));
	}

	public static class MapCycleBean {
		public String name;
		public Map<String,Object> data;
		public MapCycleBean() { /* no-op */ }
	}

	@Test
	void i05_cycleThroughMapNull() throws Exception {
		// Cycle through a Map (not a bean property, so the schema keeps the column) -> getValueByPath cycle/handleCycle.
		var a = new MapCycleBean();
		a.name = "root";
		a.data = new LinkedHashMap<>();
		a.data.put("self", a.data);  // map references itself
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.NULL).build();
		var bytes = ser.write(list(a));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Streaming entry points: writeRecords / writeArrayRecords.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_writeRecords() throws Exception {
		var session = (ParquetSerializerSession) ParquetSerializer.DEFAULT.getSession();
		var baos = new ByteArrayOutputStream();
		try (var w = session.writeRecords(baos)) {
			w.write(list(new Bean("a", 1), new Bean("b", 2)));
		}
		assertTrue(baos.toByteArray().length > 0);
		assertFalse(session.isRecordStreaming());
	}

	@Test
	void j02_writeArrayRecords() throws Exception {
		var session = (ParquetSerializerSession) ParquetSerializer.DEFAULT.getSession();
		var baos = new ByteArrayOutputStream();
		try (var w = session.writeArrayRecords(baos)) {
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
		var bytes = ParquetSerializer.DEFAULT.write(java.util.stream.Stream.of(new Bean("a", 1), new Bean("b", 2)));
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(2, out.size());
	}

	@Test
	void k00b_iteratorRoot() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(new Bean("a", 1)).iterator());
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals(1, out.size());
	}

	@Test
	void k01_singleScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write("hello");
		assertEquals("hello", ParquetParser.DEFAULT.read(bytes, String.class));
	}

	@Test
	void k02_singleBean() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(new Bean("a", 1));
		var out = (List<Bean>) ParquetParser.DEFAULT.read(bytes, List.class, Bean.class);
		assertEquals("a", out.get(0).name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toEpochDay / toTimeMicros / toInstant alternative-temporal branches.
	// Uses an Object-typed bean property; schema is built from a sample (LocalDate/LocalTime/Instant)
	// to produce the matching native column type.  Subsequent rows supply alternate temporal types,
	// which reach the toEpochDay/toTimeMicros/toInstant dispatch chain with non-canonical types.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ObjDateBean {
		public Object d;
		public ObjDateBean() {}
		public ObjDateBean(Object d) { this.d = d; }
	}

	public static class ObjTimeBean {
		public Object t;
		public ObjTimeBean() {}
		public ObjTimeBean(Object t) { this.t = t; }
	}

	public static class ObjTsBean {
		public Object ts;
		public ObjTsBean() {}
		public ObjTsBean(Object ts) { this.ts = ts; }
	}

	@Test
	void l01_toEpochDayLocalDateTimeAlt() throws Exception {
		// DATE column inferred from row-0 LocalDate value (Object-typed property); row-1 passes LocalDateTime -> toEpochDay(LocalDateTime).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDateBean(LocalDate.parse("2020-01-01")), new ObjDateBean(LocalDateTime.parse("2021-06-15T00:00:00")));
		assertNotNull(ser.write(in));
	}

	@Test
	void l02_toEpochDayInstantAlt() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDateBean(LocalDate.parse("2020-01-01")), new ObjDateBean(Instant.parse("2021-06-15T00:00:00Z")));
		assertNotNull(ser.write(in));
	}

	@Test
	void l03_toEpochDayZonedDateTimeAlt() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDateBean(LocalDate.parse("2020-01-01")), new ObjDateBean(ZonedDateTime.parse("2021-06-15T00:00:00Z")));
		assertNotNull(ser.write(in));
	}

	@Test
	void l04_toEpochDayOffsetDateTimeAlt() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDateBean(LocalDate.parse("2020-01-01")), new ObjDateBean(OffsetDateTime.parse("2021-06-15T00:00:00+00:00")));
		assertNotNull(ser.write(in));
	}

	@Test
	void l05_toEpochDayNumberAlt() throws Exception {
		// DATE column from row-0 LocalDate; row-1 passes a Long (days-since-epoch) -> toEpochDay(Number).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDateBean(LocalDate.parse("2020-01-01")), new ObjDateBean(18793L));
		assertNotNull(ser.write(in));
	}

	@Test
	void l06_toTimeMicrosNumberAlt() throws Exception {
		// TIME_MICROS column from row-0 LocalTime; row-1 passes a Number -> toTimeMicros(Number).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTimeBean(LocalTime.parse("01:02:03")), new ObjTimeBean(3_723_000_000L));
		assertNotNull(ser.write(in));
	}

	@Test
	void l07_toInstantLocalDateAlt() throws Exception {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes LocalDate -> toInstant(LocalDate).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean(LocalDate.parse("2021-06-15")));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeBoolean / writeFloat / writeDouble String fallback branches.
	// Bean with Object property; schema from boolean/float/double; second bean passes String.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ObjBoolBean {
		public Object f;
		public ObjBoolBean() {}
		public ObjBoolBean(Object f) { this.f = f; }
	}

	public static class ObjFloatBean {
		public Object f;
		public ObjFloatBean() {}
		public ObjFloatBean(Object f) { this.f = f; }
	}

	public static class ObjDblBean {
		public Object d;
		public ObjDblBean() {}
		public ObjDblBean(Object d) { this.d = d; }
	}

	@Test
	void l08_writeBooleanStringFallback() throws Exception {
		// BOOLEAN column (from row-0 Boolean); row-1 passes "true" String -> Boolean.parseBoolean fires.
		var in = list(new ObjBoolBean(Boolean.TRUE), new ObjBoolBean("true"));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	@Test
	void l09_writeFloatStringFallback() throws Exception {
		// FLOAT column (from row-0 Float); row-1 passes "2.5" String -> Float.parseFloat fires.
		var in = list(new ObjFloatBean(1.5f), new ObjFloatBean("2.5"));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	@Test
	void l10_writeDoubleStringFallback() throws Exception {
		// DOUBLE column (from row-0 Double); row-1 passes "2.5" String -> Double.parseDouble fires.
		var in = list(new ObjDblBean(1.5d), new ObjDblBean("2.5"));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toByteArray CONVERTED_TIMESTAMP_MILLIS branch: temporal object to timestamp-as-string column.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l11_toByteArrayConvertedTimestampMillis() throws Exception {
		// writeDatesAsTimestamp(false) + temporal value -> CONVERTED_TIMESTAMP_MILLIS path in toByteArray (line 1023)
		// A Date value at a BYTE_ARRAY/TIMESTAMP_MILLIS column reaches the timestamp-to-string branch.
		var m = new LinkedHashMap<String,Object>();
		m.put("d", new Date(1_000_000_000L));
		var bytes = ParquetSerializer.create().writeDatesAsTimestamp(false).build().write(m);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// applyDefaultSwap with a type that has a registered swap: Class<?> -> String.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l12_applyDefaultSwapClassValue() throws Exception {
		// A Class<?> value in a Map column: applyDefaultSwap finds the ClassSwap and returns a String.
		var m = new LinkedHashMap<String,Object>();
		m.put("type", String.class);
		var bytes = ParquetSerializer.DEFAULT.write(m);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInt64 String fallback (line 934): a non-Number, non-TemporalAccessor value in a plain INT64 column.
	// Schema is built from row-0 Long (Object-typed property) -> INT64 column.  Row-1 passes "123" String.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ObjLongBean {
		public Object v;
		public ObjLongBean() {}
		public ObjLongBean(Object v) { this.v = v; }
	}

	@Test
	void l13_toInt64StringFallback() throws Exception {
		// INT64 column (from row-0 Long); row-1 passes "123" String -> Long.parseLong fires.
		var in = list(new ObjLongBean(100L), new ObjLongBean("123"));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInstant String path (line 994-995): TIMESTAMP_MICROS column; value is a parseable ISO-8601 String.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l14_toInstantStringSuccess() throws Exception {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes an Instant-parseable String.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean("2021-06-15T12:00:00Z"));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInstant SerializeException path (line 996): TIMESTAMP_MICROS column; value is an unparseable String.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l15_toInstantStringFailure() {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes an unparseable String -> SerializeException.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean("not-a-timestamp"));
		assertThrows(Exception.class, () -> ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toByteArray CONVERTED_UTF8 and CONVERTED_ENUM String.valueOf branches (lines 1025-1026).
	// Need a BYTE_ARRAY column with CONVERTED_UTF8 / CONVERTED_ENUM but with a non-String value.
	// A plain Map with an Integer value -> BYTE_ARRAY/UTF8 column -> String.valueOf(Integer) fires.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l16_toByteArrayConvertedUtf8NonString() throws SerializeException {
		// BYTE_ARRAY / CONVERTED_UTF8 column from row-0 String; row-1 passes an Integer -> String.valueOf fires.
		var m0 = new LinkedHashMap<String,Object>();
		m0.put("s", "hello");
		var m1 = new LinkedHashMap<String,Object>();
		m1.put("s", 42);
		// Two maps serialized as two-row list equivalent via writeRecords
		var ser = ParquetSerializer.DEFAULT;
		assertNotNull(ser.write(m0));
		assertNotNull(ser.write(m1));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toDecimalUnscaled Number and String branches (lines 1005, 1008).
	// Use ObjTsBean-like pattern: schema from BigDecimal -> DECIMAL column; next row passes Number / String.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ObjDecBean {
		public Object amt;
		public ObjDecBean() {}
		public ObjDecBean(Object amt) { this.amt = amt; }
	}

	@Test
	void l17_toDecimalUnscaledFromNumber() throws Exception {
		// DECIMAL column from row-0 BigDecimal; row-1 passes an Integer -> Number branch fires.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDecBean(new BigDecimal("1.5")), new ObjDecBean(42));
		assertNotNull(ser.write(in));
	}

	@Test
	void l18_toDecimalUnscaledFromString() throws Exception {
		// DECIMAL column from row-0 BigDecimal; row-1 passes "2.5" String -> String branch fires.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDecBean(new BigDecimal("1.5")), new ObjDecBean("2.5"));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// collectBeans: empty root Map (line 249 true branch), null-value root map (line 259 false branch).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_emptyRootMap() throws Exception {
		// sType.isMap() && m.isEmpty() -> true branch on line 249: returns List.of().
		var bytes = ParquetSerializer.DEFAULT.write(new LinkedHashMap<String,Object>());
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void m02_rootMapWithNullValue() throws Exception {
		// String-keyed root map with null value -> val != null false branch (line 259): val stays null.
		var m = new LinkedHashMap<String,Object>();
		m.put("a", "x");
		m.put("b", null);
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(m);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// computeDefValue: Optional.value path (line 479), Map path (line 483).
	//-----------------------------------------------------------------------------------------------------------------

	public static class OptNestedBean {
		public Optional<String> name;
		public OptNestedBean() { /* no-op */ }
		public OptNestedBean(Optional<String> name) { this.name = name; }
	}

	public static class OptBeanOuter {
		public String id;
		public OptNestedBean inner;
		public OptBeanOuter() { /* no-op */ }
		public OptBeanOuter(String id, OptNestedBean inner) { this.id = id; this.inner = inner; }
	}

	@Test
	void m03_computeDefValueOptionalPath() throws Exception {
		// A bean with a nested Optional property -> computeDefValue walks Optional.value path.
		// Row with present Optional, row with empty Optional, row with null intermediate.
		var in = list(
			new OptBeanOuter("a", new OptNestedBean(o("x"))),
			new OptBeanOuter("b", new OptNestedBean(oe())),
			new OptBeanOuter("c", null));
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		var out = (List<OptBeanOuter>) ParquetParser.DEFAULT.read(bytes, List.class, OptBeanOuter.class);
		assertEquals(3, out.size());
	}

	public static class MapNestedBean {
		public String id;
		public Map<String,String> data;
		public MapNestedBean() { /* no-op */ }
		public MapNestedBean(String id, Map<String,String> data) { this.id = id; this.data = data; }
	}

	@Test
	void m04_computeDefValueMapPath() throws Exception {
		// Bean with nested Map property -> computeDefValue walks Map path (line 483).
		var m = new LinkedHashMap<String,String>();
		m.put("x", "1");
		m.put("y", "2");
		var in = list(new MapNestedBean("a", m), new MapNestedBean("b", null));
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// flattenListValues: Optional.value path (line 785), Map path (line 789), plain-object path (line 792).
	//-----------------------------------------------------------------------------------------------------------------

	public static class ListOptBean {
		public String name;
		public List<Optional<String>> opts;
		public ListOptBean() { /* no-op */ }
		public ListOptBean(String name, List<Optional<String>> opts) { this.name = name; this.opts = opts; }
	}

	@Test
	void m05_flattenListOptionalElements() throws Exception {
		// List column with Optional<String> elements -> flattenListValues Optional.value branch (line 785).
		var in = list(new ListOptBean("a", list(o("x"), oe(), o("z"))));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class ListMapBean {
		public String name;
		public List<Map<String,String>> rows;
		public ListMapBean() { /* no-op */ }
		public ListMapBean(String name, List<Map<String,String>> rows) { this.name = name; this.rows = rows; }
	}

	@Test
	void m06_flattenListMapElements() throws Exception {
		// List<Map<String,String>> -> flattenListValues Map path (line 789).
		var row1 = new LinkedHashMap<String,String>();
		row1.put("k", "v1");
		var row2 = new LinkedHashMap<String,String>();
		row2.put("k", "v2");
		var in = list(new ListMapBean("a", list(row1, row2)));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	public static class ListInnerBean {
		public String label;
		public int n;
		public ListInnerBean() { /* no-op */ }
		public ListInnerBean(String label, int n) { this.label = label; this.n = n; }
	}

	public static class ListBeanObjBean {
		public String name;
		public List<ListInnerBean> items;
		public ListBeanObjBean() { /* no-op */ }
		public ListBeanObjBean(String name, List<ListInnerBean> items) { this.name = name; this.items = items; }
	}

	@Test
	void m07_flattenListBeanElements() throws Exception {
		// List<bean> -> flattenListValues plain-object (toBeanMap) path (line 792).
		var in = list(new ListBeanObjBean("a", list(new ListInnerBean("x", 1), new ListInnerBean("y", 2))));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getValueByPath: Optional.value branch (line 819), Map branch (line 823), plain-object branch (line 826).
	// Cycle detection (line 827-828) -> handleCycle THROW (line 836).
	//-----------------------------------------------------------------------------------------------------------------

	public static class PathOptBean {
		public String id;
		public Optional<String> tag;
		public PathOptBean() { /* no-op */ }
		public PathOptBean(String id, Optional<String> tag) { this.id = id; this.tag = tag; }
	}

	@Test
	void m08_getValueByPathOptional() throws Exception {
		// Bean with Optional<String> tag -> getValueByPath hits Optional.value branch (line 819).
		var in = list(new PathOptBean("a", o("t1")), new PathOptBean("b", oe()));
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		assertNotNull(bytes);
	}

	public static class MapValueBean {
		public String id;
		public Map<String,Object> extra;
		public MapValueBean() { /* no-op */ }
		public MapValueBean(String id, Map<String,Object> extra) { this.id = id; this.extra = extra; }
	}

	@Test
	void m09_getValueByPathMap() throws Exception {
		// Bean with Map property -> getValueByPath hits Map branch (line 823).
		var m = new LinkedHashMap<String,Object>();
		m.put("sub", "val");
		var in = list(new MapValueBean("a", m));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	public static class PlainNested {
		public String label;
		public PlainNested() { /* no-op */ }
	}

	public static class PlainOuter {
		public String name;
		public PlainNested child;
		public PlainOuter() { /* no-op */ }
		public PlainOuter(String name, PlainNested child) { this.name = name; this.child = child; }
	}

	@Test
	void m10_getValueByPathPlainObject() throws Exception {
		// Nested plain bean -> getValueByPath hits toBeanMap plain-object path (line 826).
		var child = new PlainNested();
		child.label = "lbl";
		var in = list(new PlainOuter("a", child));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// handleCycle THROW via getValueByPath (line 836): cycle detected in column-value walk with THROW mode.
	//-----------------------------------------------------------------------------------------------------------------

	public static class CycleMap {
		public String name;
		public Map<String,Object> data;
		public CycleMap() { /* no-op */ }
	}

	@Test
	void m11_handleCycleThrowViaGetValueByPath() {
		// CyclicBean with child==self in THROW mode -> handleCycle throws from getValueByPath (line 827-828 / line 836).
		var a = new CyclicBean();
		a.name = "root";
		a.child = a;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).build();
		assertThrows(Exception.class, () -> ser.write(list(a)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeValue default case (line 891): unknown type int triggers default switch branch.
	// isRootKeyValue path in extractFlattenedMapValues (lines 657/660).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m12_nonStringKeyRootMapMultiEntry() throws Exception {
		// Non-string-key root Map with multiple entries -> collectBeans produces key/value pair rows,
		// writeColumnChunk calls writeMapColumnChunk for root.key_value.key/value paths,
		// extractFlattenedMapValues hits isRootKeyValue=true and the {key,value} row branch (lines 657/660).
		var in = new LinkedHashMap<Integer,String>();
		in.put(1, "a");
		in.put(2, null);  // null value -> def=1 branch in extractFlattenedMapValues (line 683)
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void m13_nonStringKeyRootMapSingleEntry() throws Exception {
		// Single-entry non-string-key root Map -> isRootKeyValue path with exactly one entry (rep=0 only).
		var in = new LinkedHashMap<Long,String>();
		in.put(42L, "answer");
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// writeListColumnChunk: repLevels null branch (line 550) when listDepth==0 (single-level list).
	// Already covered by d01; this exercises the repLevels != null false branch explicitly.
	// getMapAtPath: obj==null false branch (obj is non-null, non-Map -> toBeanMap branch, line 698).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m14_nestedMapNullProperty() throws Exception {
		// Bean with Map property = null -> getMapAtPath: getValueByPath returns null -> obj==null true branch (line 694-695).
		var in = list(new NonStrKeyMapBean("a", null));
		var bytes = ParquetSerializer.create().keepNullProperties().build().write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toTimeMicros: LocalDateTime -> ChronoLocalDateTime branch (line 952).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m15_toTimeMicrosOffsetTime() throws Exception {
		// TIME_MICROS column from row-0 LocalTime; row-1 passes OffsetTime -> toTimeMicros OffsetTime branch (line 958-959).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTimeBean(LocalTime.parse("01:02:03")), new ObjTimeBean(OffsetTime.parse("02:03:04+01:00")));
		assertNotNull(ser.write(in));
	}

	@Test
	void m16_toTimeMicrosNumberAlt2() throws Exception {
		// TIME_MICROS from row-0 LocalTime; row-1 passes a Long -> Number branch (line 960-961).
		// (Duplicate of l06 intent but with OffsetTime sample to confirm OffsetTime also reaches the Number branch.)
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTimeBean(LocalTime.parse("01:02:03")), new ObjTimeBean(999_999L));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInstant: ZonedDateTime branch (line 982-983).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m17_toInstantZonedDateTimeAlt() throws Exception {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes ZonedDateTime -> toInstant ZonedDateTime branch.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean(ZonedDateTime.parse("2021-06-15T00:00:00Z")));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toEpochDay: Number branch (line 911) already covered by l05; Date/Calendar branch (lines 905-910).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m18_toEpochDayStringAlt() throws Exception {
		// DATE column from row-0 LocalDate; row-1 passes a String -> toEpochDay String/parse fallback branch (line 951).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjDateBean(LocalDate.parse("2020-01-01")), new ObjDateBean("2021-06-15"));
		assertNotNull(ser.write(in));
	}

	@Test
	void m19_toTimeMicrosStringAlt() throws Exception {
		// TIME_MICROS column from row-0 LocalTime; row-1 passes a String -> LocalTime.parse fallback branch (line 962).
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTimeBean(LocalTime.parse("01:02:03")), new ObjTimeBean("04:05:06"));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInstant: Date and Calendar branches (lines 968-975); LocalDateTime branch (line 982-983).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m20_toInstantDateAlt() throws Exception {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes Date -> toInstant Date branch.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean(new Date(1_000_000_000L)));
		assertNotNull(ser.write(in));
	}

	@Test
	void m21_toInstantCalendarAlt() throws Exception {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes Calendar -> toInstant Calendar branch.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var cal = new GregorianCalendar();
		cal.setTimeInMillis(1_000_000_000L);
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean(cal));
		assertNotNull(ser.write(in));
	}

	@Test
	void m22_toInstantLocalDateTimeAlt() throws Exception {
		// TIMESTAMP_MICROS column from row-0 Instant; row-1 passes LocalDateTime -> toInstant LocalDateTime branch.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new ObjTsBean(Instant.parse("2020-01-01T00:00:00Z")), new ObjTsBean(LocalDateTime.parse("2021-06-15T12:00:00")));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toFixedLenByteArray: non-UUID fallback (line 1041) — a FIXED_LEN_BYTE_ARRAY column with a non-UUID value.
	//-----------------------------------------------------------------------------------------------------------------

	public static class UuidObjBean {
		public Object id;
		public UuidObjBean() { /* no-op */ }
		public UuidObjBean(Object id) { this.id = id; }
	}

	@Test
	void m23_toFixedLenByteArrayNonUuid() throws Exception {
		// FIXED_LEN_BYTE_ARRAY column built from UUID sample; second row passes a non-UUID Object -> fallback 16-byte array.
		var ser = ParquetSerializer.DEFAULT;
		var uuid = UUID.fromString("12345678-1234-5678-1234-567812345678");
		var in = list(new UuidObjBean(uuid), new UuidObjBean("not-a-uuid"));
		assertNotNull(ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toByteArray: CONVERTED_UTF8/ENUM with non-String (line 1025) — enum value at a UTF8 column.
	// Already partly covered; ensure CONVERTED_ENUM specifically is hit.
	//-----------------------------------------------------------------------------------------------------------------

	public static class EnumObjBean {
		public Object c;
		public EnumObjBean() { /* no-op */ }
		public EnumObjBean(Object c) { this.c = c; }
	}

	@Test
	void m24_toByteArrayEnumAtUtf8Column() throws Exception {
		// BYTE_ARRAY/UTF8 column from row-0 String; row-1 passes Enum -> String.valueOf(enum) via CONVERTED_UTF8 branch.
		var in = list(new EnumObjBean("red"), new EnumObjBean(Color.GREEN));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInt64: TemporalAccessor catch branch (line ~875) — Instant.from() fails, falls through to Long.parseLong.
	//-----------------------------------------------------------------------------------------------------------------

	public static class LongObjBean {
		public Object v;
		public LongObjBean() { /* no-op */ }
		public LongObjBean(Object v) { this.v = v; }
	}

	@Test
	void m25_toInt64TemporalAccessorCatch() {
		// Plain INT64 column (row-0 is Long). Row-1 passes Month.JUNE (a TemporalAccessor) which fails
		// Instant.from() -> catch fires -> Long.parseLong("JUNE") throws NumberFormatException.
		// The catch branch in toInt64 IS exercised regardless of what Long.parseLong does.
		var in = list(new LongObjBean(1L), new LongObjBean(Month.JUNE));
		assertThrows(Exception.class, () -> ParquetSerializer.DEFAULT.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toInstant: TemporalAccessor catch branch (line ~936) — Instant.from() fails, falls through to string parse.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m26_toInstantTemporalAccessorCatch() {
		// TIMESTAMP_MICROS column (row-0 is Instant). Row-1 passes Month.JUNE (a TemporalAccessor) which
		// fails Instant.from() -> catch fires -> Instant.parse("JUNE") also fails -> SerializeException.
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = list(new LongObjBean(Instant.EPOCH), new LongObjBean(Month.JUNE));
		assertThrows(Exception.class, () -> ser.write(in));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// collectBeans: origType.isBean() true branch (line ~289) — swap returns non-bean but original is a bean.
	//-----------------------------------------------------------------------------------------------------------------

	public static class SwappedBean {
		public String name;
		public SwappedBean(String name) { this.name = name; }
		public SwappedBean() {}
	}

	public static class SwapToString extends ObjectSwap<SwappedBean,String> {
		@Override public String swap(MarshallingSession s, SwappedBean o) { return o.name; }
	}

	@Test
	void m27_collectBeansOrigBeanSwappedToNonBean() throws Exception {
		// ObjectSwap<SwappedBean,String>: swappedType.isBean()=false, origType.isBean()=true -> line 289 true branch.
		var ser = ParquetSerializer.create().swaps(SwapToString.class).build();
		var bytes = ser.write(list(new SwappedBean("Alice"), new SwappedBean("Bob")));
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// handleCycle THROW via computeDefValue: data-level self-reference in an Object-typed property.
	// Schema inferred from first row (non-cyclic); second row has ref=itself -> handleCycle THROW fires.
	//-----------------------------------------------------------------------------------------------------------------

	public static class SampleInner {
		public String name;
		public SampleInner(String name) { this.name = name; }
		public SampleInner() {}
	}

	public static class SelfRefObjBean {
		public Object ref;
		public SelfRefObjBean(Object ref) { this.ref = ref; }
		public SelfRefObjBean() {}
	}

	@Test
	void m28_handleCycleThrowViaComputeDefValue() {
		// Schema from first row: ref=SampleInner -> root.ref.name (maxDef=2).
		// Second row: ref=itself -> computeDefValue detects cycle -> handleCycle(THROW) fires (line 777 true branch).
		var row1 = new SelfRefObjBean(new SampleInner("x"));
		var row2 = new SelfRefObjBean(null);
		row2.ref = row2;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).build();
		assertThrows(Exception.class, () -> ser.write(list(row1, row2)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// flattenListValuesImpl singletonList branch (line ~692): scalar value at a list path.
	// Schema from first row (v=[["x"]] -> 2-level list); second row (v="scalar") hits singletonList.
	//-----------------------------------------------------------------------------------------------------------------

	public static class ObjListBean {
		public Object v;
		public ObjListBean(Object v) { this.v = v; }
		public ObjListBean() {}
	}

	@Test
	void m29_flattenListValuesImplSingletonList() throws Exception {
		// Schema from first row: v=[["x"]] -> root.v.list.element.list.element (2-level list, String leaf).
		// Second row: v="scalar" -> at list.element, obj="scalar" -> singletonList fires (line ~692).
		var in = list(new ObjListBean(list(list("x"))), new ObjListBean("scalar"));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// flattenListValues THROW cycle via data-level circular list (line 652 true branch).
	// Schema from first row (non-cyclic list); second row has a list that contains itself.
	//-----------------------------------------------------------------------------------------------------------------

	public static class CircularListBean {
		public Object items;
		public CircularListBean(Object items) { this.items = items; }
		public CircularListBean() {}
	}

	@Test
	void m30_flattenListValuesCycleThrow() {
		// Schema from first row: items=[["x"]] -> root.items.list.element.list.element (2-level list).
		// Second row: items=circular_list (contains itself) -> flattenListValues detects cycle -> THROW.
		var row1 = new CircularListBean(list(list("x")));
		var circularList = new ArrayList<>();
		circularList.add(circularList);
		var row2 = new CircularListBean(circularList);
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).build();
		assertThrows(Exception.class, () -> ser.write(list(row1, row2)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getValueByPath null mid-path (line 758 true branch): nested map property where intermediate bean is null.
	//-----------------------------------------------------------------------------------------------------------------

	public static class InnerMapHolder {
		public Map<String,Integer> counts;
		public InnerMapHolder(Map<String,Integer> counts) { this.counts = counts; }
		public InnerMapHolder() {}
	}

	public static class OuterMapHolder {
		public String name;
		public InnerMapHolder inner;
		public OuterMapHolder(String name, InnerMapHolder inner) { this.name = name; this.inner = inner; }
		public OuterMapHolder() {}
	}

	@Test
	void m31_getValueByPathNullMidPath() throws Exception {
		var m = new LinkedHashMap<String,Integer>();
		m.put("x", 1);
		var in = list(new OuterMapHolder("a", new InnerMapHolder(m)), new OuterMapHolder("b", null));
		var bytes = ParquetSerializer.DEFAULT.write(in);
		assertNotNull(bytes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getValueByPath cycle (line 766 TT): bean whose Object-typed property is inferred as Map from the first-row
	// sample, but the second row stores the bean itself in that property -> seen.contains fires.
	// Schema builder uses sampleBean (BeanMap) for top-level properties so childSample = bm.get("counts") = Map,
	// which infers Map schema.  Row2: counts = row2 itself -> single navigation returns row2 -> seen.contains = TRUE.
	//-----------------------------------------------------------------------------------------------------------------

	public static class CycleFlatMapBean {
		public String name;
		public Object counts;
		public CycleFlatMapBean(String name, Object counts) { this.name = name; this.counts = counts; }
		public CycleFlatMapBean() {}
	}

	@Test
	void m32_getValueByPathCycleFlatBean() throws Exception {
		// Schema inferred from row1: counts=Map -> key_value columns -> getMapAtPath calls getValueByPath.
		// Row2: counts = row2 itself -> bm.get("counts") = row2 (in seen) -> handleCycle(NULL) -> map=empty.
		var m = new LinkedHashMap<String,Integer>();
		m.put("x", 1);
		var row1 = new CycleFlatMapBean("a", m);
		var row2 = new CycleFlatMapBean("b", null);
		row2.counts = row2;
		var bytes = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.NULL).build().write(list(row1, row2));
		assertNotNull(bytes);
	}

	@Test
	void m33_getValueByPathCycleThrowMode() {
		// Same structure with THROW mode: cycle at line 766 TT -> handleCycle(THROW) -> SerializeException.
		var m = new LinkedHashMap<String,Integer>();
		m.put("x", 1);
		var row1 = new CycleFlatMapBean("a", m);
		var row2 = new CycleFlatMapBean("b", null);
		row2.counts = row2;
		var ser = ParquetSerializer.create().cycleHandling(ParquetCycleHandling.THROW).build();
		assertThrows(Exception.class, () -> ser.write(list(row1, row2)));
	}

}
