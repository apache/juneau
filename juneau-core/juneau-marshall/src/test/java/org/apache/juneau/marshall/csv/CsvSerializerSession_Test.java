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
package org.apache.juneau.marshall.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link CsvSerializerSession} targeting branches not exercised
 * by {@link Csv_Test} or {@link CsvCellSerializer_Test}.
 *
 * <p>
 * Focuses on builder paths (property dispatch, nullValue/byteArrayFormat null branches),
 * applySwap special-type branches (Date/Calendar/Temporal/Duration/Period),
 * formatIfDateOrDuration variants, type-discriminator (_type column) for bean/map/simple paths,
 * primitive array variants (float/short/boolean/char), and prepareForInlineValue branches.
 */
class CsvSerializerSession_Test extends TestBase {

	//====================================================================================================
	// a - Builder configuration (byteArrayFormat null no-op, property dispatch, null key)
	//====================================================================================================

	@Test void a01_byteArrayFormat_nullValueIsNoOp() throws Exception {
		// CsvSerializerSession.Builder.byteArrayFormat(null) should not change current value
		// (line 120 nn(value)=false branch). Builder is on the session, not the serializer.
		var ctx = CsvSerializer.create().byteArrayFormat(CsvByteArrayCellFormat.SEMICOLON_DELIMITED).build();
		var session = ctx.createSession().byteArrayFormat(null).build();
		assertNotNull(session);
		// Value should still be SEMICOLON_DELIMITED.
		var l = new LinkedList<>();
		l.add(new BytesBean("a", new byte[]{1, 2, 3}));
		var csv = ctx.write(l);
		assertTrue(csv.contains("1;2;3"), "Expected semicolon format retained: " + csv);
	}

	@Test void a02_nullValue_explicitlyNull_fallsBackToDefault() throws Exception {
		// Builder.nullValue(null) -> session.nullValue defaults to "<NULL>" (line 177 false branch).
		var s = CsvSerializer.create().nullValue(null).build();
		var l = new LinkedList<>();
		l.add(new SimpleBean(null, 1));
		var csv = s.write(l);
		assertTrue(csv.contains("<NULL>"), "Expected default null marker: " + csv);
	}

	@Test void a03_property_byteArrayFormat() {
		// Hits Builder.property() short property name dispatch for byteArrayFormat.
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("byteArrayFormat", CsvByteArrayCellFormat.SEMICOLON_DELIMITED)
			.build();
		assertNotNull(session);
	}

	@Test void a04_property_byteArrayFormat_qualifiedName() {
		// Hits Builder.property() qualified name dispatch.
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("CsvSerializerSession.byteArrayFormat", CsvByteArrayCellFormat.BASE64)
			.build();
		assertNotNull(session);
	}

	@Test void a05_property_allowNestedStructures() {
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("allowNestedStructures", true)
			.build();
		assertNotNull(session);
	}

	@Test void a06_property_allowNestedStructures_qualifiedName() {
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("CsvSerializerSession.allowNestedStructures", false)
			.build();
		assertNotNull(session);
	}

	@Test void a07_property_nullValue() {
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("nullValue", "NIL")
			.build();
		assertNotNull(session);
	}

	@Test void a08_property_nullValue_qualifiedName() {
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("CsvSerializerSession.nullValue", "<EMPTY>")
			.build();
		assertNotNull(session);
	}

	@Test void a09_property_unknownKey_delegatesToSuper() {
		// Default branch in Builder.property() switch.
		var session = CsvSerializer.DEFAULT
			.createSession()
			.property("someUnknownKey", "value")
			.build();
		assertNotNull(session);
	}

	@Test void a10_property_nullKey_throws() {
		// Null key delegates to super (line 138-141), which throws.
		var b = CsvSerializer.DEFAULT.createSession();
		assertThrows(IllegalArgumentException.class, () -> b.property(null, "x"));
	}

	@Test void a11_create_nullCtx_throws() {
		// Static create(ctx) with null ctx -> assertArgNotNull throws.
		CsvSerializer ctx = null;
		assertThrows(IllegalArgumentException.class, () -> CsvSerializerSession.create(ctx));
	}

	//====================================================================================================
	// b - applySwap special-type branches (Date/Calendar/Temporal/Duration/Period)
	//====================================================================================================

	@Test void b01_simpleValue_dateElement() throws Exception {
		// Simple value path -> applySwap(value) -> isDate() branch (line 203).
		var l = new LinkedList<Date>();
		l.add(new Date(0));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.startsWith("value\n"), "Expected value header but was: " + csv);
		assertEquals(2, csv.split("\n").length, "Expected header + 1 row: " + csv);
	}

	@Test void b02_simpleValue_calendarElement() throws Exception {
		// applySwap -> isCalendar() branch (line 205).
		var l = new LinkedList<Calendar>();
		var c = GregorianCalendar.from(Instant.EPOCH.atZone(ZoneOffset.UTC));
		l.add(c);
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.startsWith("value\n"), "Expected value header: " + csv);
	}

	@Test void b03_simpleValue_temporalElement() throws Exception {
		// applySwap -> isTemporal() branch (line 207).
		var l = new LinkedList<Instant>();
		l.add(Instant.ofEpochSecond(0));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.startsWith("value\n"), "Expected value header: " + csv);
		assertTrue(csv.contains("1970"), "Expected ISO instant: " + csv);
	}

	@Test void b04_simpleValue_durationElement() throws Exception {
		// applySwap -> isDuration() branch (line 209).
		var l = new LinkedList<Duration>();
		l.add(Duration.ofSeconds(60));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.startsWith("value\n"), "Expected value header: " + csv);
		assertTrue(csv.contains("PT1M") || csv.contains("60"), "Expected duration: " + csv);
	}

	@Test void b05_simpleValue_periodElement() throws Exception {
		// applySwap -> isPeriod() branch (line 211).
		var l = new LinkedList<Period>();
		l.add(Period.ofDays(7));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.startsWith("value\n"), "Expected value header: " + csv);
		assertTrue(csv.contains("P7D") || csv.contains("7"), "Expected period: " + csv);
	}

	//====================================================================================================
	// c - formatIfDateOrDuration / bean property dispatch for special types
	//====================================================================================================

	@Test void c01_beanProperty_date() throws Exception {
		// Bean path: formatIfDateOrDuration on Date property (line 406).
		var l = new LinkedList<>();
		l.add(new DateBean("evt1", new Date(0)));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("evt1"));
		assertTrue(csv.contains("1970") || csv.contains("1969"), "Expected formatted date: " + csv);
	}

	@Test void c02_beanProperty_calendar() throws Exception {
		// formatIfDateOrDuration on Calendar property (line 408).
		var l = new LinkedList<>();
		var c = GregorianCalendar.from(Instant.EPOCH.atZone(ZoneOffset.UTC));
		l.add(new CalendarBean("evt1", c));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("evt1"));
	}

	@Test void c03_beanProperty_temporal() throws Exception {
		// formatIfDateOrDuration on Temporal property (line 410).
		var l = new LinkedList<>();
		l.add(new InstantBean("evt1", Instant.ofEpochSecond(0)));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("evt1"));
		assertTrue(csv.contains("1970"), "Expected ISO instant: " + csv);
	}

	@Test void c04_beanProperty_duration() throws Exception {
		// formatIfDateOrDuration on Duration property (line 412).
		var l = new LinkedList<>();
		l.add(new DurationBean("evt1", Duration.ofSeconds(60)));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("evt1"));
		assertTrue(csv.contains("PT1M"), "Expected ISO duration: " + csv);
	}

	@Test void c05_beanProperty_xmlGregorianCalendar() throws Exception {
		// formatIfDateOrDuration covers XMLGregorianCalendar (line 408 second condition).
		var l = new LinkedList<>();
		var dtf = javax.xml.datatype.DatatypeFactory.newInstance();
		var gc = new GregorianCalendar();
		gc.setTimeInMillis(0);
		var xmlGc = dtf.newXMLGregorianCalendar(gc);
		l.add(new XmlCalBean("evt1", xmlGc));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("evt1"));
	}

	@Test void c06_beanProperty_nullDate() throws Exception {
		// formatIfDateOrDuration value == null branch (line 403-404).
		var l = new LinkedList<>();
		l.add(new DateBean("evt1", null));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("<NULL>"), "Expected null marker: " + csv);
	}

	@Test void c07_beanProperty_misc_scalars() throws Exception {
		// Exercises bean property dispatch for varied scalar types.
		var l = new LinkedList<>();
		l.add(new ScalarBean(1, 2L, true, 'c', new BigDecimal("1.23")));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("1"));
		assertTrue(csv.contains("2"));
		assertTrue(csv.contains("true"));
		assertTrue(csv.contains("c"));
		assertTrue(csv.contains("1.23"));
	}

	//====================================================================================================
	// d - addBeanTypes / addRootType (_type column) for bean / map / simple paths
	//====================================================================================================

	@Test void d01_addBeanTypes_beanPath() throws Exception {
		// Bean path with addBeanTypes: lines 269,270,284-287,318-322.
		var s = CsvSerializer.create().addBeanTypes().addRootType().build();
		var l = new LinkedList<>();
		l.add(new SimpleBean("x", 1));
		l.add(new SimpleBean("y", 2));
		var csv = s.write(l);
		// Header should include _type column.
		var lines = csv.split("\n");
		assertTrue(lines[0].contains("_type"), "Expected _type column header: " + csv);
	}

	@Test void d02_addBeanTypes_beanPath_withNullEntry() throws Exception {
		// Bean path null entry + addTypeColumn (lines 292-301).
		var s = CsvSerializer.create().addBeanTypes().addRootType().build();
		var l = new LinkedList<SimpleBean>();
		l.add(new SimpleBean("x", 1));
		l.add(null);
		var csv = s.write(l);
		assertTrue(csv.contains("_type"), "Expected _type column: " + csv);
		assertTrue(csv.contains("<NULL>"), "Expected null cells for null row: " + csv);
	}

	@Test void d03_addBeanTypes_mapPath() throws Exception {
		// Map path with addBeanTypes (lines 342-345, 360-364).
		var s = CsvSerializer.create().addBeanTypes().addRootType().build();
		var l = new LinkedList<Map<String,Object>>();
		var m1 = new LinkedHashMap<String,Object>();
		m1.put("a", 1);
		m1.put("b", "x");
		l.add(m1);
		var csv = s.write(l);
		assertTrue(csv.contains("_type"), "Expected _type column: " + csv);
	}

	@Test void d04_addBeanTypes_simplePath_withNullElement() throws Exception {
		// Simple path with addBeanTypes, including null entry handling (lines 370-389).
		var s = CsvSerializer.create().addBeanTypes().addRootType().build();
		var l = new LinkedList<String>();
		l.add("hello");
		l.add(null);
		var csv = s.write(l);
		assertTrue(csv.startsWith("value"), "Expected value header: " + csv);
		assertTrue(csv.contains("hello"));
		assertTrue(csv.contains("<NULL>"), "Expected null marker: " + csv);
	}

	@Test void d05_addBeanTypes_simplePath_nonNull() throws Exception {
		// Simple path addBeanTypes -> aType branch (lines 380-385).
		var s = CsvSerializer.create().addBeanTypes().addRootType().build();
		var l = new LinkedList<Integer>();
		l.add(1);
		l.add(2);
		var csv = s.write(l);
		assertTrue(csv.startsWith("value"), "Expected value header: " + csv);
	}

	//====================================================================================================
	// e - Empty collection / single-row variants
	//====================================================================================================

	@Test void e01_emptyCollection_returnsEmpty() throws Exception {
		// Hits ne(l) false (line 247) - empty collection produces empty output.
		var csv = CsvSerializer.DEFAULT.write(new ArrayList<>());
		assertEquals("", csv);
	}

	@Test void e02_singletonCollection_singleBean() throws Exception {
		var csv = CsvSerializer.DEFAULT.write(List.of(new SimpleBean("x", 1)));
		assertEquals("b,c\nx,1\n", csv);
	}

	@Test void e03_singleBean_directObject() throws Exception {
		// cm is bean -> wrapped in singleton (line 244).
		var csv = CsvSerializer.DEFAULT.write(new SimpleBean("hello", 42));
		assertEquals("b,c\nhello,42\n", csv);
	}

	@Test void e04_objectArray() throws Exception {
		// cm.isArray() && componentType not primitive -> Object[] path (line 237).
		var arr = new SimpleBean[]{new SimpleBean("x", 1), new SimpleBean("y", 2)};
		var csv = CsvSerializer.DEFAULT.write(arr);
		assertTrue(csv.startsWith("b,c\n"), "Expected bean header: " + csv);
		assertTrue(csv.contains("x,1"));
		assertTrue(csv.contains("y,2"));
	}

	@Test void e05_primitiveArray_intArray() throws Exception {
		// cm.isArray() && componentType is primitive -> singletonList (line 235).
		var csv = CsvSerializer.DEFAULT.write(new int[]{1, 2, 3});
		assertTrue(csv.startsWith("value\n"), "Expected value header: " + csv);
		assertTrue(csv.contains("[1;2;3]"), "Expected formatted int array: " + csv);
	}

	@Test void e06_streamable() throws Exception {
		// cm.isStreamable() -> toListFromStreamable (line 242).
		var stream = java.util.stream.Stream.of(new SimpleBean("a", 1), new SimpleBean("b", 2));
		var csv = CsvSerializer.DEFAULT.write(stream);
		assertTrue(csv.startsWith("b,c\n"), "Expected bean header: " + csv);
	}

	//====================================================================================================
	// f - Special characters in cells (comma, quote, newline, tab)
	//====================================================================================================

	@Test void f01_cellWithComma_isQuoted() throws Exception {
		var csv = CsvSerializer.DEFAULT.write(List.of(new SimpleBean("a,b", 1)));
		assertTrue(csv.contains("\"a,b\""), "Expected quoted comma value: " + csv);
	}

	@Test void f02_cellWithQuote_isEscapedAndQuoted() throws Exception {
		var csv = CsvSerializer.DEFAULT.write(List.of(new SimpleBean("a\"b", 1)));
		assertTrue(csv.contains("\"a\"\"b\""), "Expected RFC 4180 doubled quotes: " + csv);
	}

	@Test void f03_cellWithNewline_isQuoted() throws Exception {
		var csv = CsvSerializer.DEFAULT.write(List.of(new SimpleBean("a\nb", 1)));
		assertTrue(csv.contains("\"a\nb\""), "Expected newline value quoted: " + csv);
	}

	@Test void f04_cellWithTab_passes() throws Exception {
		// Tab is not a CSV special char; should pass through unquoted.
		var csv = CsvSerializer.DEFAULT.write(List.of(new SimpleBean("a\tb", 1)));
		assertTrue(csv.contains("a\tb"), "Expected tab in value: " + csv);
	}

	//====================================================================================================
	// g - formatForCsvCell primitive array variants (float[], short[], boolean[], char[])
	//====================================================================================================

	@Test void g01_floatArrayProperty() throws Exception {
		// Bean property of float[] -> formatForCsvCell float[] branch (line 464).
		var l = new LinkedList<>();
		l.add(new FloatArrBean("a", new float[]{1.5f, 2.5f}));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("[1.5;2.5]"), "Expected float[] format: " + csv);
	}

	@Test void g02_shortArrayProperty() throws Exception {
		// Bean property of short[] -> formatForCsvCell short[] branch (line 474).
		var l = new LinkedList<>();
		l.add(new ShortArrBean("a", new short[]{1, 2, 3}));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("[1;2;3]"), "Expected short[] format: " + csv);
	}

	@Test void g03_booleanArrayProperty() throws Exception {
		// Bean property of boolean[] -> formatForCsvCell boolean[] branch (line 484).
		var l = new LinkedList<>();
		l.add(new BoolArrBean("a", new boolean[]{true, false, true}));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("[true;false;true]"), "Expected boolean[] format: " + csv);
	}

	@Test void g04_charArrayProperty() throws Exception {
		// Bean property of char[] -> formatForCsvCell char[] branch (line 494).
		var l = new LinkedList<>();
		l.add(new CharArrBean("a", new char[]{'a', 'b'}));
		var csv = CsvSerializer.DEFAULT.write(l);
		// char[] becomes [97;98] (numeric values).
		assertTrue(csv.contains("[97;98]"), "Expected char[] numeric format: " + csv);
	}

	@Test void g05_longArrayProperty() throws Exception {
		var l = new LinkedList<>();
		l.add(new LongArrBean("a", new long[]{10L, 20L}));
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("[10;20]"), "Expected long[] format: " + csv);
	}

	//====================================================================================================
	// h - prepareForInlineValue branches (allowNestedStructures with Date/Calendar/Temporal/Duration)
	//====================================================================================================

	@Test void h01_inline_nestedBean_invokesPrepareForInlineValue() throws Exception {
		// allowNestedStructures + nested bean -> prepareForInlineValue isBean branch (line 525).
		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var l = new LinkedList<>();
		l.add(new ParentBean("p", new SimpleBean("c", 1)));
		var csv = s.write(l);
		assertTrue(csv.contains("{") && csv.contains("}"), "Expected nested object syntax: " + csv);
	}

	@Test void h02_inline_nestedListOfDates() throws Exception {
		// allowNestedStructures + List<Date> -> CsvCellSerializer recurses into prepareForInlineValue
		// for non-primitive elements (Date) -> isDate() branch (line 527).
		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var l = new LinkedList<>();
		l.add(new DateListBean("p", List.of(new Date(0))));
		var csv = s.write(l);
		assertTrue(csv.contains("["), "Expected list syntax: " + csv);
	}

	@Test void h03_inline_nestedListOfCalendars() throws Exception {
		// prepareForInlineValue isCalendar (line 529).
		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var c = GregorianCalendar.from(Instant.EPOCH.atZone(ZoneOffset.UTC));
		var l = new LinkedList<>();
		l.add(new CalListBean("p", List.of(c)));
		var csv = s.write(l);
		assertTrue(csv.contains("["), "Expected list syntax: " + csv);
	}

	@Test void h04_inline_nestedListOfTemporals() throws Exception {
		// prepareForInlineValue isTemporal (line 531).
		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var l = new LinkedList<>();
		l.add(new InstantListBean("p", List.of(Instant.ofEpochSecond(0))));
		var csv = s.write(l);
		assertTrue(csv.contains("["), "Expected list syntax: " + csv);
		assertTrue(csv.contains("1970"), "Expected ISO date: " + csv);
	}

	@Test void h05_inline_nestedListOfDurations() throws Exception {
		// prepareForInlineValue Duration (line 533).
		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var l = new LinkedList<>();
		l.add(new DurationListBean("p", List.of(Duration.ofSeconds(60))));
		var csv = s.write(l);
		assertTrue(csv.contains("["));
		assertTrue(csv.contains("PT1M"));
	}

	@Test void h06_inline_nullValue() throws Exception {
		// prepareForInlineValue null branch (line 521).
		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var l = new LinkedList<>();
		l.add(new ParentBean("p", null));
		var csv = s.write(l);
		assertTrue(csv.contains("<NULL>"), "Expected null marker: " + csv);
	}

	//====================================================================================================
	// i - Map-of-maps and map with null values / null keys (header detection paths)
	//====================================================================================================

	@Test void i01_mapPath_withNullKeys() throws Exception {
		// Map path: null keys in header (line 334-335).
		var l = new LinkedList<Map<Object,Object>>();
		var m = new LinkedHashMap<>();
		m.put(null, "v1");
		m.put("k", "v2");
		l.add(m);
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("<NULL>"), "Expected null marker for null key: " + csv);
		assertTrue(csv.contains("k"));
	}

	@Test void i02_mapPath_withNullValues() throws Exception {
		// Map path null values (line 358).
		var l = new LinkedList<Map<String,Object>>();
		var m = new LinkedHashMap<String,Object>();
		m.put("a", null);
		m.put("b", "x");
		l.add(m);
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("<NULL>"), "Expected null marker: " + csv);
		assertTrue(csv.contains("x"));
	}

	@Test void i03_simpleValue_nullElement() throws Exception {
		// Simple path with null element (line 379 nullValue branch).
		// First element must be non-null because first(l).isPresent()==false aborts the row loop.
		var l = new LinkedList<String>();
		l.add("y");
		l.add(null);
		var csv = CsvSerializer.DEFAULT.write(l);
		assertTrue(csv.contains("<NULL>"), "Expected null marker: " + csv);
		assertTrue(csv.contains("y"));
	}

	@Test void i04_customNullValue_render() throws Exception {
		// Custom null marker (line 177 true branch with non-null value).
		var s = CsvSerializer.create().nullValue("NIL").build();
		var l = new LinkedList<String>();
		l.add("first");
		l.add(null);
		var csv = s.write(l);
		assertTrue(csv.contains("NIL"), "Expected custom null marker: " + csv);
	}

	//====================================================================================================
	// Bean classes
	//====================================================================================================

	public static class SimpleBean {
		public String b;
		public int c;
		public SimpleBean() {}
		public SimpleBean(String b, int c) { this.b = b; this.c = c; }
	}

	public static class BytesBean {
		public String name;
		public byte[] data;
		public BytesBean() {}
		public BytesBean(String name, byte[] data) { this.name = name; this.data = data; }
	}

	public static class DateBean {
		public String name;
		public Date d;
		public DateBean() {}
		public DateBean(String name, Date d) { this.name = name; this.d = d; }
	}

	public static class CalendarBean {
		public String name;
		public Calendar c;
		public CalendarBean() {}
		public CalendarBean(String name, Calendar c) { this.name = name; this.c = c; }
	}

	public static class InstantBean {
		public String name;
		public Instant t;
		public InstantBean() {}
		public InstantBean(String name, Instant t) { this.name = name; this.t = t; }
	}

	public static class DurationBean {
		public String name;
		public Duration d;
		public DurationBean() {}
		public DurationBean(String name, Duration d) { this.name = name; this.d = d; }
	}

	public static class XmlCalBean {
		public String name;
		public javax.xml.datatype.XMLGregorianCalendar c;
		public XmlCalBean() {}
		public XmlCalBean(String name, javax.xml.datatype.XMLGregorianCalendar c) {
			this.name = name; this.c = c;
		}
	}

	public static class ScalarBean {
		public int i;
		public long l;
		public boolean b;
		public char ch;
		public BigDecimal bd;
		public ScalarBean() {}
		public ScalarBean(int i, long l, boolean b, char ch, BigDecimal bd) {
			this.i = i; this.l = l; this.b = b; this.ch = ch; this.bd = bd;
		}
	}

	public static class FloatArrBean {
		public String name;
		public float[] data;
		public FloatArrBean() {}
		public FloatArrBean(String name, float[] data) { this.name = name; this.data = data; }
	}

	public static class ShortArrBean {
		public String name;
		public short[] data;
		public ShortArrBean() {}
		public ShortArrBean(String name, short[] data) { this.name = name; this.data = data; }
	}

	public static class BoolArrBean {
		public String name;
		public boolean[] data;
		public BoolArrBean() {}
		public BoolArrBean(String name, boolean[] data) { this.name = name; this.data = data; }
	}

	public static class CharArrBean {
		public String name;
		public char[] data;
		public CharArrBean() {}
		public CharArrBean(String name, char[] data) { this.name = name; this.data = data; }
	}

	public static class LongArrBean {
		public String name;
		public long[] data;
		public LongArrBean() {}
		public LongArrBean(String name, long[] data) { this.name = name; this.data = data; }
	}

	public static class ParentBean {
		public String name;
		public SimpleBean child;
		public ParentBean() {}
		public ParentBean(String name, SimpleBean child) { this.name = name; this.child = child; }
	}

	public static class DateListBean {
		public String name;
		public List<Date> dates;
		public DateListBean() {}
		public DateListBean(String name, List<Date> dates) { this.name = name; this.dates = dates; }
	}

	public static class CalListBean {
		public String name;
		public List<Calendar> cals;
		public CalListBean() {}
		public CalListBean(String name, List<Calendar> cals) { this.name = name; this.cals = cals; }
	}

	public static class InstantListBean {
		public String name;
		public List<Instant> ts;
		public InstantListBean() {}
		public InstantListBean(String name, List<Instant> ts) { this.name = name; this.ts = ts; }
	}

	public static class DurationListBean {
		public String name;
		public List<Duration> ds;
		public DurationListBean() {}
		public DurationListBean(String name, List<Duration> ds) { this.name = name; this.ds = ds; }
	}
}
