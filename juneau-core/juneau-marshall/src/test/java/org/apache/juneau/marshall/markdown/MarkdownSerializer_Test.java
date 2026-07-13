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
package org.apache.juneau.marshall.markdown;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests for {@link MarkdownSerializer} and {@link MarkdownSerializerSession} (fragment mode).
 */
class MarkdownSerializer_Test {

	//====================================================================================================
	// a - Serialize bean as key/value table
	//====================================================================================================

	@Test void a01_serializeFlatBean() {
		var bean = new A();
		bean.name = "Alice";
		bean.age = 30;
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(bean);
		assertTrue(md.contains("| Property | Value |"), "Expected header row: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name row: " + md);
		assertTrue(md.contains("| age | 30 |"), "Expected age row: " + md);
	}

	@Test void a02_serializeBeanWithNullValue() {
		var bean = new A();
		bean.name = null;
		bean.age = 30;
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(bean);
		assertTrue(md.contains("*null*"), "Expected null marker in: " + md);
	}

	@Test void a03_serializeBeanWithCustomNullValue() {
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

	@Test void b01_serializeStringMap() {
		var m = new LinkedHashMap<String, String>();
		m.put("k1", "v1");
		m.put("k2", "v2");
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(m);
		assertTrue(md.contains("| Key | Value |"), "Expected header row: " + md);
		assertTrue(md.contains("| k1 | v1 |"), "Expected k1 row: " + md);
		assertTrue(md.contains("| k2 | v2 |"), "Expected k2 row: " + md);
	}

	//====================================================================================================
	// c - Serialize uniform collection as multi-column table
	//====================================================================================================

	@Test void c01_serializeBeanListAsMultiColumnTable() {
		var list = List.of(new B("Alice", 30), new B("Bob", 25));
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(list);
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

	@Test void d01_serializeStringListAsBullets() {
		var list = List.of("alpha", "beta", "gamma");
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(list);
		assertTrue(md.contains("- alpha"), "Expected bullet alpha: " + md);
		assertTrue(md.contains("- beta"), "Expected bullet beta: " + md);
		assertTrue(md.contains("- gamma"), "Expected bullet gamma: " + md);
	}

	@Test void d02_serializeIntListAsBullets() {
		var list = List.of(1, 2, 3);
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(list);
		assertTrue(md.contains("- 1"), "Expected bullet 1: " + md);
		assertTrue(md.contains("- 2"), "Expected bullet 2: " + md);
		assertTrue(md.contains("- 3"), "Expected bullet 3: " + md);
	}

	//====================================================================================================
	// e - Nested complex values rendered as inline JSON5
	//====================================================================================================

	@Test void e01_serializeNestedBeanAsInlineJson5() {
		var bean = new C();
		bean.name = "Alice";
		bean.nested = new A();
		bean.nested.name = "inner";
		bean.nested.age = 1;
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(bean);
		assertTrue(md.contains("`"), "Expected backtick wrapping for nested value: " + md);
	}

	public static class C {
		public String name;
		public A nested;
	}

	//====================================================================================================
	// f - Pipe character in values is escaped
	//====================================================================================================

	@Test void f01_pipeCharacterIsEscaped() {
		var m = Map.of("desc", "hello | world");
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(m);
		assertTrue(md.contains("\\|"), "Expected escaped pipe: " + md);
	}

	//====================================================================================================
	// g - showHeaders = false
	//====================================================================================================

	@Test void g01_noHeaders() {
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

	@Test void h01_serializeBooleanAndNumericProperties() {
		var bean = new D();
		bean.count = 42;
		bean.ratio = 3.14;
		bean.flag = true;
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(bean);
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

	@Test void i01_roundTripFlatBean() {
		var original = new B("Alice", 30);
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(original);
		var parsed = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.to(md, B.class);
		assertEquals("Alice", parsed.name);
		assertEquals(30, parsed.age);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void i02_roundTripBeanList() {
		var original = List.of(new B("Alice", 30), new B("Bob", 25));
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(original);
		var parsed = (List<B>) org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.to(md, List.class, B.class);
		assertEquals(2, parsed.size());
		assertEquals("Alice", parsed.get(0).name);
		assertEquals(30, parsed.get(0).age);
		assertEquals("Bob", parsed.get(1).name);
		assertEquals(25, parsed.get(1).age);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void i03_roundTripStringList() {
		var original = List.of("foo", "bar", "baz");
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(original);
		var parsed = (List<String>) org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.to(md, List.class, String.class);
		assertEquals(original, parsed);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void i04_roundTripMap() {
		var original = new LinkedHashMap<String, String>();
		original.put("k1", "v1");
		original.put("k2", "v2");
		var md = org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.of(original);
		var parsed = (Map<String, String>) org.apache.juneau.marshall.marshaller.Markdown.DEFAULT.to(md, Map.class, String.class, String.class);
		assertEquals(original, parsed);
	}

	//====================================================================================================
	// j — meta-provider methods: null-bpm guard + nullValue(null) guard
	//====================================================================================================

	@Test void j01_getMarkdownBeanPropertyMeta_null_returnsDefault() {
		var result = MarkdownSerializer.DEFAULT.getMarkdownBeanPropertyMeta(null);
		assertSame(org.apache.juneau.marshall.markdown.MarkdownBeanPropertyMeta.DEFAULT, result);
	}

	@Test void j02_nullValue_null_usesDefaultMarker() {
		var serializer = MarkdownSerializer.create().nullValue(null).build();
		assertNotNull(serializer.getNullValue());
	}

	//====================================================================================================
	// k - Date and temporal types in inline cell values
	//====================================================================================================

	@Test void k01_serializeDateInlineValue() {
		// java.util.Date property in a bean → serializeInlineValue Date path
		var bean = new K();
		bean.ts = new java.util.Date(0);
		bean.name = "test";
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		assertTrue(md.contains("| ts |"), "Expected ts property: " + md);
	}

	@Test void k02_serializeCalendarInlineValue() {
		// java.util.Calendar property → serializeInlineValue Calendar path
		var bean = new KCal();
		bean.cal = java.util.Calendar.getInstance();
		bean.label = "calTest";
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		assertTrue(md.contains("| cal |"), "Expected cal property: " + md);
	}

	@Test void k03_serializeTemporalInlineValue() {
		// java.time.Instant property → serializeInlineValue Temporal path
		var bean = new KTemporal();
		bean.instant = java.time.Instant.ofEpochMilli(0);
		bean.label = "temporalTest";
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		assertTrue(md.contains("| instant |"), "Expected instant property: " + md);
	}

	@Test void k04_serializeDurationInlineValue() {
		// java.time.Duration property → serializeInlineValue Duration path
		var bean = new KDuration();
		bean.dur = java.time.Duration.ofSeconds(60);
		bean.label = "durationTest";
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		assertTrue(md.contains("| dur |"), "Expected dur property: " + md);
	}

	@Test void k05_serializePeriodInlineValue() {
		// java.time.Period property → serializeInlineValue Period path
		var bean = new KPeriod();
		bean.period = java.time.Period.ofDays(7);
		bean.label = "periodTest";
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		assertTrue(md.contains("| period |"), "Expected period property: " + md);
	}

	@Test void k06_serializeOptionalEmptyInBean() {
		// Optional.empty() property → serializeAnything Optional.isEmpty() branch (null path)
		var bean = new KOptional();
		bean.name = oe();
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		// Empty Optional renders as null value (null marker or omitted key)
		assertTrue(md.contains("name") || md.contains("*null*"), "Expected name property in: " + md);
	}

	public static class K {
		public String name;
		public java.util.Date ts;
	}

	public static class KCal {
		public String label;
		public java.util.Calendar cal;
	}

	public static class KTemporal {
		public String label;
		public java.time.Instant instant;
	}

	public static class KDuration {
		public String label;
		public java.time.Duration dur;
	}

	public static class KPeriod {
		public String label;
		public java.time.Period period;
	}

	public static class KOptional {
		public java.util.Optional<String> name;
	}

	//====================================================================================================
	// l - Collection edge cases in serializeCollection
	//====================================================================================================

	@Test void l01_serializeEmptyCollection() {
		// Empty collection → serializeCollection returns immediately (empty branch)
		var md = MarkdownSerializer.DEFAULT.serialize(java.util.List.of());
		// Empty collection → empty output
		assertTrue(md == null || md.isEmpty() || md.isBlank(), "Expected empty output for empty collection: '" + md + "'");
	}

	@Test void l02_serializeCollectionWithAllNullItems() {
		// All-null collection → getTableHeaders returns null → bulleted list path
		var list = new java.util.ArrayList<>();
		list.add(null);
		list.add(null);
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		assertNotNull(md);
		// Should render as bulleted list with null markers
		assertTrue(md.contains("*null*"), "Expected null markers in bulleted list: " + md);
	}

	@Test void l03_serializeMapWithNullKey() {
		// Map with null key → serializeMap null-key branch
		var m = new java.util.HashMap<>();
		m.put(null, "value");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
		assertTrue(md.contains("*null*") || md.contains("value"), "Expected map output: " + md);
	}

	//====================================================================================================
	// m - escapeJson5String special character paths
	//====================================================================================================

	@Test void m01_serializeStringWithSingleQuoteAndBackslash() {
		// Single-quote and backslash in string → escapeJson5String special char branches
		var m = new java.util.LinkedHashMap<String, String>();
		m.put("val", "it's a \\test");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
		// The string should be wrapped in JSON5 backtick syntax (ambiguous due to backslash or single quote)
		// Round-trip should preserve the value
		@SuppressWarnings("unchecked")
		var parsed = (java.util.Map<String,String>) MarkdownParser.DEFAULT.parse(md, java.util.Map.class);
		assertEquals("it's a \\test", parsed.get("val"));
	}

	@Test void m02_serializeStringWithTabAndCR() {
		// Tab and CR in string → escapeJson5String \t and \r paths
		var m = new java.util.LinkedHashMap<String, String>();
		m.put("tab", "a\tb");
		m.put("cr", "a\rb");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
		// These should be wrapped in JSON5 backtick syntax (control chars trigger isAmbiguousString)
		assertTrue(md.contains("`'"), "Expected JSON5 wrapping for tab/CR: " + md);
	}

	//====================================================================================================
	// n - addBeanTypes with polymorphic beans
	//====================================================================================================

	@Marshalled(typeName="nbase")
	public static class NBase {
		public String type;
	}

	@Marshalled(typeName="nchild")
	public static class NChild extends NBase {
		public String extra;
	}

	@Test void n01_addBeanTypes_inKeyValueTable() {
		// addBeanTypes → getBeanTypeName may return non-null when declared type differs from actual type
		var s = MarkdownSerializer.create().addBeanTypes().beanDictionary(NBase.class, NChild.class).build();
		var child = new NChild();
		child.type = "test";
		child.extra = "extra";
		// Serialize as the declared parent type so addBeanTypes kicks in
		var md = s.serialize((NBase) child);
		assertNotNull(md);
		// Should produce some table output with bean properties
		assertTrue(md.contains("|"), "Expected table output: " + md);
	}

	//====================================================================================================
	// o - Date/Calendar/Temporal/Duration/Period as MAP values
	// These hit serializeInlineValue's isDate/isCalendar/isTemporal/isDuration/isPeriod branches
	// (lines 459-471). Bean properties miss these because applyContextFormats installs a per-property
	// swap that converts them to String before serializeInlineValue is called.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("o01_temporalTypeAsMapValueProvider")
	void o01_temporalTypeAsMapValue(String key, Object value) {
		var m = new LinkedHashMap<String, Object>();
		m.put(key, value);
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
		assertTrue(md.contains("| " + key + " |"), "Expected " + key + " key: " + md);
	}

	static Stream<Arguments> o01_temporalTypeAsMapValueProvider() {
		return Stream.of(
			Arguments.of("ts", new Date(0)),
			Arguments.of("cal", Calendar.getInstance()),
			Arguments.of("instant", Instant.ofEpochMilli(0)),
			Arguments.of("dur", Duration.ofSeconds(60)),
			Arguments.of("period", Period.ofDays(7))
		);
	}

	//====================================================================================================
	// p - Direct serializeAnything paths: null root, Optional
	//====================================================================================================

	@Test void p01_serializeNullRoot() {
		// null as root object → serializeAnything null branch at line 153
		var md = MarkdownSerializer.DEFAULT.serialize(null);
		assertNotNull(md);
		assertTrue(md.contains("*null*"), "Expected null marker: " + md);
	}

	@Test void p02_serializeOptionalPresent() {
		// Optional.of("hello") as root → serializeAnything isOptional=true, isEmpty=false (lines 172, 174 false)
		var md = MarkdownSerializer.DEFAULT.serialize(o("hello"));
		assertNotNull(md);
		assertTrue(md.contains("hello"), "Expected value in output: " + md);
	}

	@Test void p03_serializeOptionalEmpty() {
		// Optional.empty() as root → serializeAnything isOptional=true, isEmpty=true (lines 172, 174 true)
		var md = MarkdownSerializer.DEFAULT.serialize(oe());
		assertNotNull(md);
		assertTrue(md.contains("*null*"), "Expected null marker: " + md);
	}

	@Test void p04_serializeStreamable() {
		// Stream as root → serializeAnything isStreamable branch at line 191
		var stream = java.util.stream.Stream.of("alpha", "beta", "gamma");
		var md = MarkdownSerializer.DEFAULT.serialize(stream);
		assertNotNull(md);
		assertTrue(md.contains("alpha"), "Expected alpha in output: " + md);
	}

	//====================================================================================================
	// q - showHeaders=false for map serialization
	//====================================================================================================

	@Test void q01_noHeadersForMap() {
		// showHeaders=false with a map → skip header at line 257
		var s = MarkdownSerializer.create().showHeaders(false).build();
		var m = new java.util.LinkedHashMap<String, String>();
		m.put("k1", "v1");
		var md = s.serialize(m);
		assertFalse(md.contains("| Key |"), "Should not have header row: " + md);
		assertTrue(md.contains("| k1 | v1 |"), "Expected data row: " + md);
	}

	//====================================================================================================
	// r - Ambiguous map key (line 266 in serializeMap)
	//====================================================================================================

	@Test void r01_numericMapKey() {
		// Key that looks like a number → isAmbiguousString returns true for key at line 266
		var m = new java.util.LinkedHashMap<String, String>();
		m.put("42", "answer");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
		assertTrue(md.contains("42"), "Expected numeric key in output: " + md);
	}

	//====================================================================================================
	// s - Collection with null item in table mode (line 310)
	//====================================================================================================

	@Test void s01_beanCollectionWithNullItem() {
		// Collection of beans where one item is null → null row in table at line 310
		var list = new java.util.ArrayList<B>();
		list.add(new B("Alice", 30));
		list.add(null);
		list.add(new B("Bob", 25));
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		assertNotNull(md);
		assertTrue(md.contains("Alice"), "Expected Alice in table: " + md);
		assertTrue(md.contains("*null*"), "Expected null row: " + md);
	}

	//====================================================================================================
	// t - addBeanTypes + addRootType on a list of polymorphic beans (lines 326-335)
	//====================================================================================================

	@Test void t01_addRootType_beanCollection() {
		// addBeanTypes + addRootType on a list → isRoot() && isAddRootType() check at line 328
		var s = MarkdownSerializer.create().addBeanTypes().addRootType().beanDictionary(NBase.class, NChild.class).build();
		var list = java.util.List.of(new NChild(), new NChild());
		var md = s.serialize(list);
		assertNotNull(md);
		assertTrue(md.contains("|"), "Expected table output: " + md);
	}

	//====================================================================================================
	// u - isAmbiguousString edge cases in serializeInlineValue (lines 369-377)
	//====================================================================================================

	@Test void u01_emptyStringAsMapValue() {
		// Empty string value → isAmbiguousString returns true at line 369 (s.isEmpty())
		var m = new java.util.LinkedHashMap<String, String>();
		m.put("key", "");
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
		assertTrue(md.contains("key"), "Expected key in output: " + md);
	}

	@ParameterizedTest
	@MethodSource("u02_ambiguousMapEntryProducesOutputProvider")
	void u02_ambiguousMapEntryProducesOutput(String key, String value) {
		// Ambiguous map keys/values still produce non-null serialized output.
		var m = new java.util.LinkedHashMap<String, String>();
		m.put(key, value);
		var md = MarkdownSerializer.DEFAULT.serialize(m);
		assertNotNull(md);
	}

	static Stream<Arguments> u02_ambiguousMapEntryProducesOutputProvider() {
		return Stream.of(
			Arguments.of("key", "*null*"),     // value equal to nullValue → isAmbiguousString line 370
			Arguments.of("key", "a\u007fb"),   // char 127 (DEL) → isAmbiguousString line 373
			Arguments.of("*null*", "surprise") // key equal to nullValue → isAmbiguousString line 370
		);
	}
}
