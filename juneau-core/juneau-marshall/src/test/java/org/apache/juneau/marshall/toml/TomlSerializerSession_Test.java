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

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link TomlSerializerSession}.
 *
 * <p>Targets bean/nested-table serialization, root-level dispatch, array-of-tables,
 * key-quoting branches, swap/temporal/enum/date/duration paths, and inline-table heuristics.
 */
@SuppressWarnings({
	"java:S125",  // Commented-out code is retained as historical reference / future re-enable candidate.
	"java:S5976"  // Separate test methods preferred over parameterized for clarity and independent failure reporting.
})
class TomlSerializerSession_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Helper beans
	//-----------------------------------------------------------------------------------------------------------------

	public static class A_Simple {
		public String name;
		public int port;
		public boolean active;
	}

	public static class A_WithNested {
		public String name;
		public A_Simple inner;
	}

	public static class A_WithList {
		public String title;
		public List<A_Simple> servers;
	}

	public static class A_WithBeanArray {
		public String title;
		public A_Simple[] servers;
	}

	public static class A_WithMap {
		public String name;
		public Map<String, Object> props;
	}

	public static class A_WithSimpleList {
		public String name;
		public List<String> tags;
	}

	public static class A_WithArray {
		public String name;
		public int[] codes;
	}

	public static class A_WithStringArray {
		public String name;
		public String[] tags;
	}

	public static class A_WithEnum {
		public String name;
		public TimeUnit unit;
	}

	public static class A_AllSimple {
		public String s;
		public int i;
		public boolean b;
	}

	public static class A_FourFields {
		public String f1;
		public String f2;
		public String f3;
		public String f4;
	}

	public static class A_WithBeanInline {
		public String name;
		public A_AllSimple inner;
	}

	public static class A_NullProperty {
		public String name;
		public String missing;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a. Root-type dispatch
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_rootBean() throws Exception {
		var x = new A_Simple();
		x.name = "alpha";
		x.port = 80;
		x.active = true;
		var toml = TomlSerializer.DEFAULT.serialize(x);
		assertTrue(toml.contains("name = \"alpha\""));
		assertTrue(toml.contains("port = 80"));
		assertTrue(toml.contains("active = true"));
	}

	@Test
	void a02_rootMap() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("k", "v");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("k = \"v\""));
	}

	@Test
	void a03_rootMapWithNestedMap() throws Exception {
		var inner = new LinkedHashMap<String, Object>();
		inner.put("host", "localhost");
		inner.put("port", 5432);
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "myapp");
		m.put("database", inner);
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("[database]"));
		assertTrue(toml.contains("host = \"localhost\""));
	}

	@Test
	void a04_rootNullObject() throws Exception {
		// doSerialize returns immediately for null
		var toml = TomlSerializer.DEFAULT.serialize(null);
		assertNotNull(toml);
		assertEquals("", toml.replace("\r", "").replace("\n", ""));
	}

	@Test
	void a05_rootCollectionOfBeans() throws Exception {
		// Root collection of beans — at runtime, List.of() loses generic info and the element type
		// resolves to Object, so the simple-array branch fires (stringifies elements). This still
		// exercises the "elType.isBean() || elType.isMap()" false branch in serializeRoot for
		// collections, which is the coverage goal. The bean-element [[item]] path is exercised in
		// a11_rootArrayOfBeans where the runtime array type preserves component info.
		var a1 = new A_Simple();
		a1.name = "a";
		a1.port = 1;
		a1.active = true;
		var a2 = new A_Simple();
		a2.name = "b";
		a2.port = 2;
		a2.active = false;
		var toml = TomlSerializer.DEFAULT.serialize(List.of(a1, a2));
		assertNotNull(toml);
		assertTrue(toml.contains("_value"), () -> "Expected _value wrapper in output but got: " + toml);
	}

	@Test
	void a06_rootCollectionOfMaps() throws Exception {
		// Same erasure caveat as a05 — List.of(map, map) loses element type at runtime.
		var m1 = new LinkedHashMap<String, Object>();
		m1.put("k", "v1");
		var m2 = new LinkedHashMap<String, Object>();
		m2.put("k", "v2");
		var toml = TomlSerializer.DEFAULT.serialize(List.of(m1, m2));
		assertNotNull(toml);
	}

	@Test
	void a07_rootCollectionOfPrimitives() throws Exception {
		// Root collection of simples wraps in _value
		var toml = TomlSerializer.DEFAULT.serialize(List.of("a", "b", "c"));
		assertTrue(toml.contains("_value = ["));
		assertTrue(toml.contains("\"a\""));
	}

	@Test
	void a08_rootArrayOfPrimitives() throws Exception {
		// Root array (not collection) - hits eType.isArray() branch
		var toml = TomlSerializer.DEFAULT.serialize(new int[]{1, 2, 3});
		assertTrue(toml.contains("_value = ["));
		assertTrue(toml.contains("1"));
		assertTrue(toml.contains("2"));
		assertTrue(toml.contains("3"));
	}

	@Test
	void a09_rootPrimitive() throws Exception {
		// Root primitive wrapped in _value
		var toml = TomlSerializer.DEFAULT.serialize("hello");
		assertTrue(toml.contains("_value = \"hello\""));
	}

	@Test
	void a10_rootInteger() throws Exception {
		var toml = TomlSerializer.DEFAULT.serialize(42);
		assertTrue(toml.contains("_value = 42"));
	}

	@Test
	void a11_rootArrayOfBeans() throws Exception {
		// Bean array at root - hits eType.isArray() with bean elements
		var a1 = new A_Simple();
		a1.name = "x";
		a1.port = 1;
		var a2 = new A_Simple();
		a2.name = "y";
		a2.port = 2;
		var toml = TomlSerializer.DEFAULT.serialize(new A_Simple[]{a1, a2});
		assertTrue(toml.contains("[[item]]"));
		assertTrue(toml.contains("name = \"x\""));
		assertTrue(toml.contains("name = \"y\""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. Nested bean serialization (serializeBean complex pass)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_beanWithNestedBean() throws Exception {
		var x = new A_WithNested();
		x.name = "outer";
		x.inner = new A_Simple();
		x.inner.name = "inner";
		x.inner.port = 80;
		x.inner.active = true;
		// Force non-inline by using more than threshold and a sub-bean — easier: turn off inline tables
		var s = TomlSerializer.create().useInlineTables(false).build();
		var toml = s.serialize(x);
		assertTrue(toml.contains("[inner]"), () -> "Expected nested table header [inner] but got: " + toml);
		assertTrue(toml.contains("name = \"inner\""));
	}

	@Test
	void b02_beanWithListOfBeans() throws Exception {
		// Exercises the bean.complex pass for a List<Bean> property. The runtime list-element type
		// detection inside serializeBean's complex branch may resolve to Object and fall into the
		// writeKeyValue fallthrough at line 216 — this still drives the complex-pass dispatch.
		var x = new A_WithList();
		x.title = "store";
		var p1 = new A_Simple();
		p1.name = "Hammer";
		p1.port = 1;
		var p2 = new A_Simple();
		p2.name = "Nail";
		p2.port = 2;
		x.servers = List.of(p1, p2);
		var s = TomlSerializer.create().useInlineTables(false).build();
		var toml = s.serialize(x);
		assertTrue(toml.contains("title = \"store\""));
		assertTrue(toml.contains("servers"), () -> "Expected 'servers' key in output but got: " + toml);
	}

	@Test
	void b02b_beanWithBeanArray() throws Exception {
		// Bean property typed as A_Simple[] (array, not List). Component type info preserved at runtime.
		var p1 = new A_Simple();
		p1.name = "Hammer";
		p1.port = 1;
		p1.active = true;
		var p2 = new A_Simple();
		p2.name = "Nail";
		p2.port = 2;
		p2.active = false;
		var x = new A_WithBeanArray();
		x.title = "store";
		x.servers = new A_Simple[]{p1, p2};
		var s = TomlSerializer.create().useInlineTables(false).build();
		var toml = s.serialize(x);
		assertTrue(toml.contains("title = \"store\""));
		assertTrue(toml.contains("[[servers]]"), () -> "Expected [[servers]] header but got: " + toml);
		assertTrue(toml.contains("Hammer"));
		assertTrue(toml.contains("Nail"));
	}

	@Test
	void b03_beanWithNestedMap() throws Exception {
		var x = new A_WithMap();
		x.name = "outer";
		var inner = new LinkedHashMap<String, Object>();
		inner.put("k1", "v1");
		inner.put("k2", 42);
		x.props = inner;
		var s = TomlSerializer.create().useInlineTables(false).build();
		var toml = s.serialize(x);
		assertTrue(toml.contains("[props]"));
		assertTrue(toml.contains("k1 = \"v1\""));
		assertTrue(toml.contains("k2 = 42"));
	}

	@Test
	void b04_beanWithSimpleList() throws Exception {
		var x = new A_WithSimpleList();
		x.name = "outer";
		x.tags = List.of("red", "green", "blue");
		var toml = TomlSerializer.DEFAULT.serialize(x);
		assertTrue(toml.contains("name = \"outer\""));
		assertTrue(toml.contains("tags = ["));
		assertTrue(toml.contains("\"red\""));
	}

	@Test
	void b05_beanWithIntArray() throws Exception {
		var x = new A_WithArray();
		x.name = "outer";
		x.codes = new int[]{1, 2, 3};
		var toml = TomlSerializer.DEFAULT.serialize(x);
		assertTrue(toml.contains("codes = ["));
		assertTrue(toml.contains("1"));
		assertTrue(toml.contains("2"));
		assertTrue(toml.contains("3"));
	}

	@Test
	void b06_beanWithStringArray() throws Exception {
		var x = new A_WithStringArray();
		x.name = "outer";
		x.tags = new String[]{"a", "b"};
		var toml = TomlSerializer.DEFAULT.serialize(x);
		assertTrue(toml.contains("tags = ["));
		assertTrue(toml.contains("\"a\""));
		assertTrue(toml.contains("\"b\""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. Inline-table heuristic
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_inlineTableUsedWhenSmall() throws Exception {
		// useInlineTables=true (default) and threshold=3
		// A_Simple has 3 fields (all simple) — should serialize as inline table when nested
		var x = new A_WithBeanInline();
		x.name = "outer";
		x.inner = new A_AllSimple();
		x.inner.s = "hi";
		x.inner.i = 7;
		x.inner.b = true;
		var s = TomlSerializer.create().useInlineTables(true).inlineTableThreshold(5).build();
		var toml = s.serialize(x);
		// Inner becomes inline table: inner = {s = "hi", i = 7, b = true}
		assertTrue(toml.contains("inner = {"), () -> "Expected inline table for inner but got: " + toml);
	}

	@Test
	void c02_inlineTableSkippedWhenOverThreshold() throws Exception {
		// 4 fields > threshold=3 default → falls back to [table]
		var x = new A_WithBeanInline();
		x.name = "outer";
		x.inner = new A_AllSimple();
		x.inner.s = "hi";
		x.inner.i = 1;
		x.inner.b = true;
		// Set threshold to 2 to force overshoot
		var s = TomlSerializer.create().useInlineTables(true).inlineTableThreshold(2).build();
		var toml = s.serialize(x);
		// Inner has 3 fields > 2 threshold → should be a [table] not inline
		assertTrue(toml.contains("[inner]"), () -> "Expected [inner] table header but got: " + toml);
	}

	@Test
	void c03_inlineTableDisabled() throws Exception {
		var x = new A_WithBeanInline();
		x.name = "outer";
		x.inner = new A_AllSimple();
		x.inner.s = "hi";
		x.inner.i = 1;
		x.inner.b = true;
		var s = TomlSerializer.create().useInlineTables(false).build();
		var toml = s.serialize(x);
		assertTrue(toml.contains("[inner]"), () -> "Expected [inner] table header but got: " + toml);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. Key quoting (writeKey: bareKey vs quotedKey)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_keyWithDots() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("a.b.c", "val");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("\"a.b.c\""));
	}

	@Test
	void d02_keyWithSpaces() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("with spaces", "val");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("\"with spaces\""));
	}

	@Test
	void d03_emptyKeyQuoted() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("", "val");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// Empty key => quoted
		assertTrue(toml.contains("\"\" = \"val\""));
	}

	@Test
	void d04_bareKeyAlphanumDashUnderscore() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("my-key_1", "val");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// bare key: no quotes
		assertTrue(toml.contains("my-key_1 = \"val\""));
	}

	@Test
	void d05_nullKeyBecomesNullString() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put(null, "val");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// Null key serialized as "null"
		assertTrue(toml.contains("null = \"val\""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e. Type dispatch in writeValue
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_floatValue() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("pi", 3.14);
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("pi = 3.14"));
	}

	@Test
	void e02_floatValueWrappedFloat() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("f", Float.valueOf(1.5f));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("f = 1.5"));
	}

	@Test
	void e03_longValue() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("n", 1234567890123L);
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("n = 1234567890123"));
	}

	@Test
	void e04_enumValue() throws Exception {
		var x = new A_WithEnum();
		x.name = "outer";
		x.unit = TimeUnit.SECONDS;
		var toml = TomlSerializer.DEFAULT.serialize(x);
		assertTrue(toml.contains("unit = \"SECONDS\""));
	}

	@Test
	void e05_dateValue() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("when", new Date(0L));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// Date should serialize via serializeDate
		assertTrue(toml.contains("when ="));
	}

	@Test
	void e06_calendarValue() throws Exception {
		var c = Calendar.getInstance();
		c.setTimeInMillis(0L);
		var m = new LinkedHashMap<String, Object>();
		m.put("when", c);
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("when ="));
	}

	@Test
	void e07_temporalOffsetDateTime() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("ts", OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// OffsetDateTime should serialize as raw temporal (not quoted)
		assertTrue(toml.contains("ts = 2024-01-15"));
	}

	@Test
	void e08_temporalYear() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("y", Year.of(2024));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// Year should be string-quoted
		assertTrue(toml.contains("y = \"2024\""), () -> "Expected quoted Year but got: " + toml);
	}

	@Test
	void e09_temporalYearMonth() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("ym", YearMonth.of(2024, 6));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("ym = \"2024-06\""));
	}

	@Test
	void e10_durationValue() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("d", Duration.ofSeconds(30));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		// Duration should be string-quoted
		assertTrue(toml.contains("d = \""));
	}

	@Test
	void e11_periodValue() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("p", Period.ofDays(5));
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("p = \""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f. Null property handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_nullPropertyOmittedByDefault() throws Exception {
		var x = new A_NullProperty();
		x.name = "alpha";
		x.missing = null;
		var toml = TomlSerializer.DEFAULT.serialize(x);
		assertTrue(toml.contains("name = \"alpha\""));
		assertFalse(toml.contains("missing"));
	}

	@Test
	void f02_nullPropertyKeptWhenConfigured() throws Exception {
		var s = TomlSerializer.create().keepNullProperties().build();
		var x = new A_NullProperty();
		x.name = "alpha";
		x.missing = null;
		var toml = s.serialize(x);
		assertTrue(toml.contains("name = \"alpha\""));
		assertTrue(toml.contains("missing = \"<NULL>\""));
	}

	@Test
	void f03_nullElementInArray() throws Exception {
		var s = TomlSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String, Object>();
		var list = new ArrayList<String>();
		list.add("a");
		list.add(null);
		list.add("b");
		m.put("items", list);
		var toml = s.serialize(m);
		// Null element within array should appear as nullValue
		assertTrue(toml.contains("\"<NULL>\""), () -> "Expected nullValue for null element but got: " + toml);
	}

	@Test
	void f04_nullElementInPrimitiveArray() throws Exception {
		// Object array with null element via reflective Array path
		var s = TomlSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String, Object>();
		m.put("arr", new String[]{"a", null, "b"});
		var toml = s.serialize(m);
		assertTrue(toml.contains("\"<NULL>\""), () -> "Expected nullValue for null array element but got: " + toml);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g. sortKeys option
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_sortKeys() throws Exception {
		var s = TomlSerializer.create().sortKeys(true).useInlineTables(false).build();
		var x = new A_FourFields();
		x.f4 = "d";
		x.f2 = "b";
		x.f1 = "a";
		x.f3 = "c";
		var toml = s.serialize(x);
		// Verify order: f1 before f2 before f3 before f4
		var i1 = toml.indexOf("f1 ");
		var i2 = toml.indexOf("f2 ");
		var i3 = toml.indexOf("f3 ");
		var i4 = toml.indexOf("f4 ");
		assertTrue(i1 >= 0 && i1 < i2, "f1 should appear before f2");
		assertTrue(i2 < i3, "f2 should appear before f3");
		assertTrue(i3 < i4, "f3 should appear before f4");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h. Map-as-table key quoting
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_mapWithComplexKeysGetsQuoted() throws Exception {
		var inner = new LinkedHashMap<String, Object>();
		inner.put("a.dotted", "v");
		var m = new LinkedHashMap<String, Object>();
		m.put("section", inner);
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("[section]"));
		assertTrue(toml.contains("\"a.dotted\""));
	}

	@Test
	void h02_mapWithIntegerKeys() throws Exception {
		// Non-string keys go through toString() (line uses isMap branch)
		var m = new LinkedHashMap<>();
		m.put(1, "one");
		m.put(2, "two");
		var toml = TomlSerializer.DEFAULT.serialize(m);
		assertTrue(toml.contains("1 = \"one\""));
		assertTrue(toml.contains("2 = \"two\""));
	}
}
