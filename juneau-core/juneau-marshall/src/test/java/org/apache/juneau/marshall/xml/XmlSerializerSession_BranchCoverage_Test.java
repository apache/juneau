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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for XML serializer/parser round-trips and edge cases.
 *
 * Uses XmlSerializer/XmlParser directly to avoid name-collision with the
 * {@link Xml} annotation in this package.
 */
class XmlSerializerSession_BranchCoverage_Test extends TestBase {

	private static final XmlSerializer SER = XmlSerializer.DEFAULT_SQ;
	private static final XmlParser     PAR = XmlParser.DEFAULT;

	// ------------------------------------------------------------------------------------------------------------------
	// a - Round-trip tests
	// ------------------------------------------------------------------------------------------------------------------

	/** Simple bean serializes to XML and round-trips back. */
	@Test void a01_simpleBean() {
		var bean = new SimpleBean();
		bean.name = "Alice";
		bean.age = 30;
		var xml = SER.serialize(bean);
		assertNotNull(xml);
		assertTrue(xml.contains("<name>Alice</name>"), "Expected name element in: " + xml);
		assertTrue(xml.contains("<age>30</age>"), "Expected age element in: " + xml);
		var parsed = PAR.parse(xml, SimpleBean.class);
		assertEquals("Alice", parsed.name);
		assertEquals(30, parsed.age);
	}

	/** Bean with null field round-trips correctly — null fields are omitted by default. */
	@Test void a02_nullField() {
		var bean = new SimpleBean();
		bean.name = null;
		bean.age = 5;
		var xml = SER.serialize(bean);
		assertNotNull(xml);
		var parsed = PAR.parse(xml, SimpleBean.class);
		assertNull(parsed.name);
		assertEquals(5, parsed.age);
	}

	/** Map serializes to XML and round-trips via JsonMap. */
	@Test void a03_map() {
		var m = new LinkedHashMap<String, Object>();
		m.put("k1", "v1");
		m.put("k2", 42);
		var xml = SER.serialize(m);
		assertNotNull(xml);
		assertTrue(xml.contains("k1"), "Expected key k1 in: " + xml);
		assertTrue(xml.contains("v1"), "Expected value v1 in: " + xml);
		var parsed = PAR.parse(xml, JsonMap.class);
		assertEquals("v1", parsed.get("k1"));
	}

	/** List serializes to XML and round-trips via JsonList. */
	@Test void a04_list() {
		var list = new ArrayList<>(Arrays.asList("alpha", "beta", "gamma"));
		var xml = SER.serialize(list);
		assertNotNull(xml);
		assertTrue(xml.contains("alpha"), "Expected alpha in: " + xml);
		var parsed = PAR.parse(xml, JsonList.class);
		assertEquals(3, parsed.size());
	}

	/** Empty bean serializes to an element with no children and round-trips. */
	@Test void a05_emptyBean() {
		var bean = new EmptyBean();
		var xml = SER.serialize(bean);
		assertNotNull(xml);
		assertNotNull(PAR.parse(xml, EmptyBean.class));
	}

	/** Nested bean serializes with child elements and round-trips. */
	@Test void a06_nestedBean() {
		var outer = new OuterBean();
		outer.label = "outer";
		outer.inner = new SimpleBean();
		outer.inner.name = "Bob";
		outer.inner.age = 25;
		var xml = SER.serialize(outer);
		assertNotNull(xml);
		assertTrue(xml.contains("Bob"), "Expected nested name in: " + xml);
		var parsed = PAR.parse(xml, OuterBean.class);
		assertEquals("outer", parsed.label);
		assertEquals("Bob", parsed.inner.name);
	}

	/** Array serializes as repeated elements and round-trips. */
	@Test void a07_array() {
		var arr = new int[]{1, 2, 3};
		var xml = SER.serialize(arr);
		assertNotNull(xml);
		var parsed = PAR.parse(xml, JsonList.class);
		assertEquals(3, parsed.size());
	}

	/** trimStrings=true strips whitespace around string values during serialization. */
	@Test void a08_trimStrings() {
		var s = XmlSerializer.create().trimStrings().build();
		var m = new LinkedHashMap<String, Object>();
		m.put("key", "  padded  ");
		var xml = s.serialize(m);
		assertNotNull(xml);
		assertTrue(xml.contains("padded"), "Expected trimmed value in: " + xml);
		assertFalse(xml.contains("  padded  "), "Expected padding removed in: " + xml);
	}

	/** addBeanTypesXml causes type information to be encoded when the declared type is abstract. */
	@Test void a09_addBeanTypes() {
		var s = XmlSerializer.create().addBeanTypesXml().build();
		var bean = new SimpleBean();
		bean.name = "typeTest";
		bean.age = 1;
		var xml = s.serialize(bean);
		assertNotNull(xml);
		assertTrue(xml.contains("name"), "Expected bean content in: " + xml);
	}

	/** Parsing invalid XML throws a ParseException. */
	@Test void a10_parseError() {
		assertThrows(ParseException.class, () -> PAR.parse("<<not valid xml>>", SimpleBean.class));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// b - Builder.property() method coverage (lines 164-186)
	// ------------------------------------------------------------------------------------------------------------------

	/** textNodeDelimiter(null) → sets empty string (line 164 true branch). */
	@Test void b01_textNodeDelimiter_null() {
		var b = XmlSerializer.create();
		b.textNodeDelimiter(null);
		var s = b.build();
		assertNotNull(s.serialize("hello"));
	}

	/** property(null, value) → null-key branch at line 170. */
	@Test void b02_property_nullKey() {
		var b = XmlSerializer.DEFAULT.createSession();
		assertThrows(IllegalArgumentException.class, () -> b.property(null, "value"));
	}

	/** property() with addNamespaceUrisToRoot key → switch case at line 172. */
	@Test void b03_property_addNamespaceUrisToRoot() {
		var s = XmlSerializer.DEFAULT.createSession().property("addNamespaceUrisToRoot", true).build();
		var xml = s.serialize(new SimpleBean());
		assertNotNull(xml);
	}

	/** property() with textNodeDelimiter key → switch case at line 180. */
	@Test void b04_property_textNodeDelimiter() {
		var s = XmlSerializer.DEFAULT.createSession().property("textNodeDelimiter", " | ").build();
		var xml = s.serialize(new SimpleBean());
		assertNotNull(xml);
	}

	/** property() with defaultNamespace key → switch case at line 176. */
	@Test void b05_property_defaultNamespace() {
		var s = XmlSerializer.DEFAULT.createSession().property("defaultNamespace", "http://example.com").build();
		assertNotNull(s);
	}

	/** property() with unknown key → default case at line 182. */
	@Test void b06_property_unknownKey() {
		var b = XmlSerializer.DEFAULT.createSession();
		b.property("some.unknown.key", "value");
		assertNotNull(b);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// c - Namespace-related serialization (lines 346-352, 695-709)
	// ------------------------------------------------------------------------------------------------------------------

	/** Bean with namespace enabled → namespace-aware serialization. */
	@Test void c01_enableNamespaces() {
		var s = XmlSerializer.create().enableNamespaces().addNamespaceUrisToRoot().build();
		var xml = s.serialize(new SimpleBean());
		assertNotNull(xml);
	}

	/** List with namespace enabled → collection namespace serialization. */
	@Test void c02_enableNamespaces_collection() {
		var s = XmlSerializer.create().enableNamespaces().build();
		var list = new ArrayList<>(List.of("alpha", "beta"));
		var xml = s.serialize(list);
		assertNotNull(xml);
	}

	/** Null root object serializes to XML. */
	@Test void c03_nullRoot() {
		var xml = SER.serialize(null);
		assertNotNull(xml);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// d - Type-specific serialization (Date, Temporal, Duration, Period, Char, Boolean, Number)
	// ------------------------------------------------------------------------------------------------------------------

	/** Date serializes without error. */
	@Test void d01_date() {
		var xml = SER.serialize(new Date(0));
		assertNotNull(xml);
	}

	/** java.time.Instant serializes without error. */
	@Test void d02_instant() {
		var xml = SER.serialize(java.time.Instant.EPOCH);
		assertNotNull(xml);
	}

	/** java.time.Duration serializes without error. */
	@Test void d03_duration() {
		var xml = SER.serialize(java.time.Duration.ofSeconds(42));
		assertNotNull(xml);
	}

	/** java.time.Period serializes without error. */
	@Test void d04_period() {
		var xml = SER.serialize(java.time.Period.of(1, 2, 3));
		assertNotNull(xml);
	}

	/** Boolean serializes to XML. */
	@Test void d05_booleanValue() {
		var xml = SER.serialize(true);
		assertNotNull(xml);
	}

	/** Number serializes to XML. */
	@Test void d06_number() {
		var xml = SER.serialize(42);
		assertNotNull(xml);
	}

	/** char value zero serializes to XML. */
	@Test void d07_charZero() {
		var xml = SER.serialize('\0');
		assertNotNull(xml);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// e - Complex collection serialization (lines 500-532)
	// ------------------------------------------------------------------------------------------------------------------

	/** Bean with list property annotated as XML elements format. */
	public static class BeanWithListProp {
		@Xml(format=XmlFormat.ELEMENTS)
		public List<String> items = List.of("a", "b", "c");
	}

	@Test void e01_beanWithListProperty() {
		var xml = SER.serialize(new BeanWithListProp());
		assertNotNull(xml);
		assertTrue(xml.contains("a"), "Expected items in: " + xml);
	}

	/** Stream serialized as collection. */
	@Test void e02_streamAsCollection() {
		var stream = java.util.stream.Stream.of("x", "y", "z");
		var xml = SER.serialize(stream);
		assertNotNull(xml);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// f - DelegateList serialization (lines 693-709)
	// ------------------------------------------------------------------------------------------------------------------

	@Test @SuppressWarnings({"unchecked","rawtypes"}) void f01_delegateList() {
		var ctx = XmlSerializer.DEFAULT.getMarshallingContext();
		var cm = (org.apache.juneau.marshall.ClassMeta) ctx.getClassMeta(List.class, String.class);
		var dl = new org.apache.juneau.marshall.internal.DelegateList<>(cm);
		dl.add("alpha");
		dl.add("beta");
		var xml = SER.serialize(dl);
		assertNotNull(xml);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// g - Auto-detect namespaces (line 334)
	// ------------------------------------------------------------------------------------------------------------------

	@Test void g01_autoDetectNamespaces() {
		// autoDetectNamespaces is on by default (disable method is disableAutoDetectNamespaces)
		var s = XmlSerializer.create().enableNamespaces(true).addNamespaceUrisToRoot().build();
		var xml = s.serialize(new SimpleBean());
		assertNotNull(xml);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Helper bean classes
	// ------------------------------------------------------------------------------------------------------------------

	public static class SimpleBean {
		public String name;
		public int age;
	}

	public static class EmptyBean {
		public String placeholder;
	}

	public static class OuterBean {
		public String label;
		public SimpleBean inner;
	}
}
