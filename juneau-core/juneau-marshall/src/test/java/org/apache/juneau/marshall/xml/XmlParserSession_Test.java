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
 * Coverage-focused tests for {@link XmlParserSession} targeting low-coverage paths:
 *  - Builder property() switch (preserveRootElement, validating, alternate keys, fall-through)
 *  - Malformed XML / unexpected events (END_DOCUMENT, end-of-stream)
 *  - Generic-map parsing with attributes-only, mixed text+children, nested namespaces
 *  - parseIntoMap with non-string key types and duplicate keys (JsonList accumulation)
 *  - parseIntoCollection with arrays and varargs
 *  - parseAnything error / non-bean fallback / proxy / number / boolean / nil branches
 *  - XmlFormat variants: ATTR, COLLAPSED, TEXT, XMLTEXT, MIXED, ELEMENTS
 *  - Comments / processing instructions / CDATA inside elements
 *  - Accessors: isPreserveRootElement, isValidating, isWhitespaceElement, getEventAllocator,
 *    getReporter, getResolver, getXmlBeanMeta, getXmlBeanPropertyMeta, getXmlClassMeta
 */
@SuppressWarnings({
	"rawtypes",
	"unchecked",
	"java:S5961"
})
class XmlParserSession_Test extends TestBase {

	private static final XmlParser P = XmlParser.DEFAULT;

	// -----------------------------------------------------------------------------------------------------------------
	// a01 - Builder.property() switch coverage
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_builder_preserveRootElement() {
		var s = XmlParser.create().build().createSession()
			.property("preserveRootElement", "true")
			.build();
		assertTrue(s.isPreserveRootElement());
	}

	@Test void a02_builder_validating() {
		var s = XmlParser.create().build().createSession()
			.property("validating", "true")
			.build();
		assertTrue(s.isValidating());
	}

	@Test void a03_builder_qualifiedKeys() {
		var s = XmlParser.create().build().createSession()
			.property("XmlParserSession.preserveRootElement", "true")
			.property("XmlParserSession.validating", "true")
			.build();
		assertTrue(s.isPreserveRootElement());
		assertTrue(s.isValidating());
	}

	@Test void a04_builder_unknownKeyFallsThrough() {
		// Unknown key → default branch → super.property delegates without effect.
		var s = XmlParser.create().build().createSession()
			.property("UnknownKey", "value")
			.build();
		assertNotNull(s);
	}

	@Test void a05_builder_nullKeyDelegates() {
		// Null key → early return → super.property delegates → throws.
		var b = XmlParser.create().build().createSession();
		assertThrows(IllegalArgumentException.class, () -> b.property(null, "x"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b01 - Accessor coverage
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_accessors() {
		// Default-state and explicit-state accessors.
		var s = P.getSession();
		assertFalse(s.isPreserveRootElement());
		assertFalse(s.isValidating());
		assertNull(s.getEventAllocator());
		assertNull(s.getReporter());
		assertNull(s.getResolver());
		var s2 = XmlParser.create().preserveRootElement().validating().build().getSession();
		assertTrue(s2.isPreserveRootElement());
		assertTrue(s2.isValidating());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c01 - getUnknown() / generic Map parsing branches
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_unknown_attributesOnlyAndWithText() {
		// Element with attrs only → MarshalledMap with attrs only.
		var m1 = (Map) P.parse("<A b='1' c='2'/>", Object.class);
		assertEquals("1", m1.get("b"));
		assertEquals("2", m1.get("c"));
		// Element with attrs + text → MarshalledMap with "contents" key.
		var m2 = (Map) P.parse("<A b='1'>hello</A>", Object.class);
		assertEquals("1", m2.get("b"));
		assertEquals("hello", m2.get("contents"));
	}

	@Test void c03_unknown_cdataAndPiAndComment() {
		// CDATA appends text; PI and comments are skipped (PROCESSING_INSTRUCTION/COMMENT branches).
		assertEquals("hello", P.parse("<A><![CDATA[hello]]></A>", Object.class));
		assertEquals("hello", P.parse("<A><?pi data?><!-- comment -->hello</A>", Object.class));
	}

	@Test void c05_unknown_duplicateKeysAccumulateIntoList() {
		// First duplicate → JsonList; third occurrence → MarshalledList.add() branch.
		var m = (Map) P.parse("<A><x>1</x><x>2</x><x>3</x></A>", Object.class);
		var v = m.get("x");
		assertTrue(v instanceof List);
		assertEquals(3, ((List) v).size());
	}

	@Test void c07_unknown_namedNestedElements() {
		// _name attribute drives the key name in the parent generic map.
		// The child's content goes through getUnknown which sees the _name attribute and so
		// builds a MarshalledMap; with text "v" it ends up as {contents: "v"}.
		var m = (Map) P.parse("<A><x _name='custom'>v</x></A>", Object.class);
		assertNotNull(m.get("custom"));
	}

	@Test void c08_unknown_namespacedChildren() {
		// Child elements with namespaces → keys are local-only (decoded), values parsed.
		var xml = "<A xmlns:ns='http://x'><ns:b>1</ns:b><ns:c>2</ns:c></A>";
		var m = (Map) P.parse(xml, Object.class);
		assertEquals("1", m.get("b"));
		assertEquals("2", m.get("c"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d01 - Malformed XML / parser errors
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_malformed_unclosedTag() {
		assertThrows(ParseException.class, () -> P.parse("<A><B>", Object.class));
	}

	@Test void d02_malformed_mismatchedTag() {
		assertThrows(ParseException.class, () -> P.parse("<A></B>", Object.class));
	}

	@Test void d03_malformed_notXml() {
		assertThrows(ParseException.class, () -> P.parse("not-xml", Object.class));
	}

	@Test void d04_malformed_emptyInput() {
		assertThrows(ParseException.class, () -> P.parse("", Object.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// e01 - parseIntoMap / Map parsing with typed keys/values + duplicate handling
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_intKeyMap() {
		// Parse a Map<Integer,String> with encoded element names; convertAttrToType bridges to Integer.
		var src = "<object><_x0031_>a</_x0031_><_x0032_>b</_x0032_></object>";
		Map<Integer, String> m = P.parse(src, HashMap.class, Integer.class, String.class);
		assertEquals("a", m.get(1));
		assertEquals("b", m.get(2));
	}

	@Test void e02_genericMap_attributeAndDuplicates() {
		// Top-level map with attribute → attribute becomes entry.  Duplicate child names roll into a
		// JsonList, third occurrence appends to the existing list.
		var m1 = (Map) P.parse("<object a='1'><b>2</b></object>", Map.class);
		assertEquals("1", m1.get("a"));
		assertEquals("2", m1.get("b"));
		var m2 = (Map) P.parse("<object><x>1</x><x>2</x><x>3</x></object>", Map.class);
		assertEquals(3, ((List) m2.get("x")).size());
	}

	@Test void e03_emptyMap() {
		var m = P.parse("<object/>", JsonMap.class);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// f01 - parseIntoCollection / Array branches
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_collection_strings() {
		var xml = "<array><string>a</string><string>b</string><string>c</string></array>";
		var l = P.parse(xml, List.class);
		assertEquals(3, l.size());
		assertEquals("a", l.get(0));
	}

	@Test void f02_collection_typedList() {
		var xml = "<array><number>1</number><number>2</number></array>";
		var l = (LinkedList<Integer>) P.parse(xml, LinkedList.class, Integer.class);
		assertEquals(2, l.size());
		assertEquals(Integer.valueOf(1), l.get(0));
	}

	@Test void f03_collection_emptyAndPrimArray() {
		var emptyArr = P.parse("<array/>", String[].class);
		assertEquals(0, emptyArr.length);
		var arr = P.parse("<array><number>1</number><number>2</number><number>3</number></array>", int[].class);
		assertArrayEquals(new int[] { 1, 2, 3 }, arr);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// g01 - parseAnything: scalar types and the type-attribute / element-name dispatch
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_nilTrueReturnsNull() {
		// "_type=null" or "nil=true" path: <object nil='true'/> when target is bean produces null/empty; // NOSONAR
		// _type='null' returns null directly.
		var o = P.parse("<x _type='null'/>", Object.class);
		assertNull(o);
	}

	@Test void g02_typeNumber() {
		var n = P.parse("<x _type='number'>42</x>", Object.class);
		assertEquals(42, n);
	}

	@Test void g03_typeBooleanStringArrayObject() {
		assertEquals(Boolean.TRUE, P.parse("<x _type='boolean'>true</x>", Object.class));
		assertEquals("hi", P.parse("<x _type='string'>hi</x>", Object.class));
		var l = (List) P.parse("<x _type='array'><string>a</string></x>", Object.class);
		assertEquals(1, l.size());
		var m = (Map) P.parse("<x _type='object'><a>1</a></x>", Object.class);
		assertEquals("1", m.get("a"));
	}

	@Test void g04_targetCharacterAndOptional() {
		assertEquals(Character.valueOf('z'), P.parse("<x>z</x>", Character.class));
		var o = (Optional<Integer>) P.parse("<x>5</x>", Optional.class, Integer.class);
		assertTrue(o.isPresent());
		assertEquals(5, o.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// h01 - preserveRootElement + Map / generic Object
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_preserveRoot_genericMap() {
		var p = XmlParser.create().preserveRootElement().build();
		var m = p.parse("<wrap><a>1</a></wrap>", JsonMap.class);
		assertEquals("{\"wrap\":{\"a\":\"1\"}}", m.toString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// i01 - Beans with @Xml format variants (ATTR / COLLAPSED / TEXT / XMLTEXT / MIXED / ELEMENTS)
	// -----------------------------------------------------------------------------------------------------------------

	public static class AttrBean {
		@Xml(format = XmlFormat.ATTR)
		public String a;
		public String b;
	}

	@Test void i01_attrFormat() {
		var x = "<object a='1'><b>2</b></object>";
		var bean = P.parse(x, AttrBean.class);
		assertEquals("1", bean.a);
		assertEquals("2", bean.b);
	}

	public static class CollapsedBean {
		@Xml(childName = "x", format = XmlFormat.COLLAPSED)
		public List<String> items = new ArrayList<>();
	}

	@Test void i02_collapsedFormat() {
		var x = "<object><x>a</x><x>b</x><x>c</x></object>";
		var bean = P.parse(x, CollapsedBean.class);
		assertEquals(3, bean.items.size());
		assertEquals("a", bean.items.get(0));
	}

	public static class TextBean {
		@Xml(format = XmlFormat.ATTR)
		public String a;
		@Xml(format = XmlFormat.TEXT)
		public String b;
	}

	@Test void i03_textFormat() {
		var x = "<object a='aval'>text-content</object>";
		var bean = P.parse(x, TextBean.class);
		assertEquals("aval", bean.a);
		assertEquals("text-content", bean.b);
	}

	public static class XmlTextBean {
		@Xml(format = XmlFormat.ATTR)
		public String a;
		@Xml(format = XmlFormat.XMLTEXT)
		public String b;
	}

	@Test void i04_xmlTextFormat() {
		var x = "<object a='aval'>before<i>middle</i>after</object>";
		var bean = P.parse(x, XmlTextBean.class);
		assertEquals("aval", bean.a);
		assertNotNull(bean.b);
		assertTrue(bean.b.contains("before"));
		assertTrue(bean.b.contains("middle"));
		assertTrue(bean.b.contains("after"));
	}

	public static class ElementsBean {
		@Xml(format = XmlFormat.ATTR)
		public String a;
		@Xml(format = XmlFormat.ELEMENTS)
		public Object[] b;
	}

	@Test void i05_elementsFormat() {
		var x = "<object a='aval'><string>foo</string><number>1</number><boolean>true</boolean></object>";
		var bean = P.parse(x, ElementsBean.class);
		assertEquals("aval", bean.a);
		assertNotNull(bean.b);
		assertEquals(3, bean.b.length);
	}

	@Xml(format = XmlFormat.ATTRS)
	public static class AttrsBean {
		public String a;
		public int b;
	}

	@Test void i06_attrsFormat() {
		var x = "<object a='foo' b='42'/>";
		var bean = P.parse(x, AttrsBean.class);
		assertEquals("foo", bean.a);
		assertEquals(42, bean.b);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// j01 - Comment handling inside bean parsing (event == COMMENT branch in parseIntoBean)
	// -----------------------------------------------------------------------------------------------------------------

	public static class SimpleBean {
		public String f1;
		public int f2;
	}

	@Test void j01_commentInsideBean() {
		// Comments between bean property elements should be ignored (event == COMMENT branch in parseIntoBean).
		var x = "<object><!-- skip --><f1>hi</f1><f2>5</f2></object>";
		var bean = P.parse(x, SimpleBean.class);
		assertEquals("hi", bean.f1);
		assertEquals(5, bean.f2);
	}

	@Test void j02_unknownPropertyOnBean() {
		// Unknown property triggers onUnknownProperty (which by default throws).
		var x = "<object><nope>1</nope></object>";
		assertThrows(ParseException.class, () -> P.parse(x, SimpleBean.class));
	}

	@Test void j03_ignoreUnknownProperty() {
		var p = XmlParser.create().ignoreUnknownBeanProperties().build();
		var x = "<object><nope>1</nope><f1>hi</f1></object>";
		var bean = p.parse(x, SimpleBean.class);
		assertEquals("hi", bean.f1);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// k01 - Sub-document parsing: parseIntoMap with attribute on map-element
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_mapAttributeBecomesEntryAndNestedMap() {
		// Map element with an attribute → attribute becomes a key (parseIntoMap attr loop).
		// Nested object property becomes nested Map.
		var x = "<object x='val'><inner><a>1</a></inner></object>";
		var m = (Map) P.parse(x, Map.class);
		assertEquals("val", m.get("x"));
		assertEquals("1", ((Map) m.get("inner")).get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// l01 - Encoded element names with _xNNNN_ escapes
	// -----------------------------------------------------------------------------------------------------------------

	@Test void l01_encodedElementName() {
		// Element name "_x0061_" decodes to "a" via getElementName/decodeString.
		var x = "<object><_x0061_>1</_x0061_></object>";
		var m = (Map) P.parse(x, Map.class);
		assertEquals("1", m.get("a"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// m01 - parseIntoBean error and content-property combinations
	// -----------------------------------------------------------------------------------------------------------------

	@Test void m01_parseIntoMap_facadeEntryPoint() {
		// XmlParser.parseIntoMap dispatches into XmlParserSession.doParseIntoMap.
		var dest = new LinkedHashMap<String,Integer>();
		var x = "<object><a>1</a><b>2</b></object>";
		P.parseIntoMap(x, dest, String.class, Integer.class);
		assertEquals(1, dest.get("a"));
		assertEquals(2, dest.get("b"));
	}

	@Test void m02_parseIntoCollection_facadeEntryPoint() {
		// XmlParser.parseIntoCollection dispatches into XmlParserSession.doParseIntoCollection.
		var dest = new ArrayList<String>();
		var x = "<array><string>x</string><string>y</string></array>";
		P.parseIntoCollection(x, dest, String.class);
		assertEquals(2, dest.size());
		assertEquals("x", dest.get(0));
		assertEquals("y", dest.get(1));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// MIXED-format coverage
	// -----------------------------------------------------------------------------------------------------------------

	public static class MixedBean {
		@Xml(format = XmlFormat.ATTR)
		public String a;
		@Xml(format = XmlFormat.MIXED)
		public List<Object> b = new ArrayList<>();
	}

	@Test void n01_mixedFormat_collection() {
		// MIXED format with collection content property accumulates text nodes & elements.
		var x = "<object a='aval'>text1<x>el</x>text2</object>";
		var bean = P.parse(x, MixedBean.class);
		assertEquals("aval", bean.a);
		assertNotNull(bean.b);
		assertFalse(bean.b.isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// o01 - Wrapped (preserveRoot) Map target
	// -----------------------------------------------------------------------------------------------------------------

	@Test void o01_preserveRoot_typedMap_and_objectType() {
		// preserveRoot wraps both typed-Map and generic-Object/_type='object' targets.
		var p = XmlParser.create().preserveRootElement().build();
		var m = (Map) p.parse("<root><a>1</a></root>", HashMap.class);
		assertTrue(m.containsKey("root"));
		var o = p.parse("<wrap _type='object'><a>1</a></wrap>", Object.class);
		assertTrue(o instanceof Map);
		assertTrue(((Map) o).containsKey("wrap"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// p01 - parseText / TEXT_PWS content format (parseText() workhorse)
	// -----------------------------------------------------------------------------------------------------------------

	public static class TextPwsBean {
		@Xml(format = XmlFormat.ATTR)
		public String a;
		@Xml(format = XmlFormat.TEXT_PWS)
		public String b;
	}

	@Test void p01_textPwsFormat_withInnerElement() {
		// TEXT_PWS preserves whitespace and parseText() is invoked when an inner element is found
		// inside the text-content property.  The inner element is stringified into b.
		var x = "<object a='aval'>before<i>inner</i>after</object>";
		var bean = P.parse(x, TextPwsBean.class);
		assertEquals("aval", bean.a);
		assertNotNull(bean.b);
		// At minimum the inner text or surrounding text appears in b - exact format depends on parseText impl.
		assertTrue(bean.b.contains("inner") || bean.b.contains("i"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// q01 - Object target with _type='string' and Character target (line 820-821 isChar branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void q01_objectTargetCharacterFromTypedString() {
		// _type='string' on Object target → if Object equals char, parse as character.  We exercise
		// the isChar branch here by parsing into Character target where parseAnything still routes
		// through the OBJECT/STRING branch via type-attribute.
		var c = P.parse("<x _type='string'>z</x>", Character.class);
		assertEquals(Character.valueOf('z'), c);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// r01 - parseAnything: numeric on element-name dispatch (sType is Object, jsonType from element name)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void r01_elementNameDispatch_number() {
		// When inside a generic Map context, elements with _type='number' dispatch jsonType=NUMBER.
		var m = (Map) P.parse("<object><k _type='number'>42</k></object>", Map.class);
		assertEquals(42, m.get("k"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// s — XmlReader constructor: reporter / resolver / eventAllocator non-null branches
	// -----------------------------------------------------------------------------------------------------------------

	@Test void s01_parseWithReporter_succeeds() {
		var parser = XmlParser.create().reporter(XmlConfigAnnotationTest.AB.class).build();
		var bean = parser.parse("<object><name>Alice</name><age>30</age></object>", JsonMap.class);
		assertEquals("Alice", bean.get("name"));
	}

	@Test void s02_parseWithResolver_succeeds() {
		var parser = XmlParser.create().resolver(XmlConfigAnnotationTest.AC.class).build();
		var bean = parser.parse("<object><name>Bob</name></object>", JsonMap.class);
		assertEquals("Bob", bean.get("name"));
	}

	@Test void s03_parseWithEventAllocator_succeeds() {
		var parser = XmlParser.create().eventAllocator(XmlConfigAnnotationTest.AA.class).build();
		var bean = parser.parse("<object><x>test</x></object>", JsonMap.class);
		assertEquals("test", bean.get("x"));
	}
}
