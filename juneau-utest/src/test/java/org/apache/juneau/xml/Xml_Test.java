// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.xml;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.annotation.*;
import org.apache.juneau.xml.xml1a.*;
import org.apache.juneau.xml.xml1b.*;
import org.apache.juneau.xml.xml1c.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class Xml_Test extends SimpleTestBase {

	//====================================================================================================
	// Simple comparison test with JSON serializer
	//====================================================================================================
	@Test void a01_comparisonWithJson() throws Exception {

		var json1 = """
			{
				name: "John Smith",
				address: {
					streetAddress: "21 2nd Street",
					city: "New York",
					state: "NY",
					postalCode: 10021
				},
				phoneNumbers: [
					"212 555-1111",
					"212 555-2222"
				],
				additionalInfo: null,
				remote: false,
				height: 62.4,
				"fico score": " > 640"
			}""";

		var xml1 = """
			<object>
				<name>John Smith</name>
				<address _type='object'>
					<streetAddress>21 2nd Street</streetAddress>
					<city>New York</city>
					<state>NY</state>
					<postalCode _type='number'>10021</postalCode>
				</address>
				<phoneNumbers _type='array'>
					<string>212 555-1111</string>
					<string>212 555-2222</string>
				</phoneNumbers>
				<additionalInfo _type='null'/>
				<remote _type='boolean'>false</remote>
				<height _type='number'>62.4</height>
				<fico_x0020_score>_x0020_&gt; 640</fico_x0020_score>
			</object>
			""";

		var m = (JsonMap) XmlParser.DEFAULT.parse(xml1, Object.class);
		var json2 = JsonSerializer.create().simpleAttrs().ws().keepNullProperties().build().serialize(m);
		assertEquals(json1, json2);

		m = (JsonMap) JsonParser.DEFAULT.parse(json1, Object.class);
		var xml2 = XmlSerializer.create().sq().ws()
			.keepNullProperties()
			.build()
			.serialize(m);
		assertEquals(xml1, xml2);
	}

	//====================================================================================================
	// Test namespacing
	//====================================================================================================
	@Test void a02_namespaces() throws Exception {

		var json1 = """
			{
				name: "John Smith",
				address: {
					streetAddress: "21 2nd Street",
					city: "New York",
					state: "NY",
					postalCode: 10021
				},
				phoneNumbers: [
					"212 555-1111",
					"212 555-2222"
				],
				additionalInfo: null,
				remote: false,
				height: 62.4,
				"fico score": " > 640"
			}""";

		var xml1 = """
			<object xmlns='http://www.apache.org'>
				<name>John Smith</name>
				<address _type='object'>
					<streetAddress>21 2nd Street</streetAddress>
					<city>New York</city>
					<state>NY</state>
					<postalCode _type='number'>10021</postalCode>
				</address>
				<phoneNumbers _type='array'>
					<string>212 555-1111</string>
					<string>212 555-2222</string>
				</phoneNumbers>
				<additionalInfo _type='null'/>
				<remote _type='boolean'>false</remote>
				<height _type='number'>62.4</height>
				<fico_x0020_score>_x0020_&gt; 640</fico_x0020_score>
			</object>
			""";

		var m = (JsonMap) JsonParser.DEFAULT.parse(json1, Object.class);
		var r = XmlSerializer.create().ns().sq().ws()
			.addNamespaceUrisToRoot()
			.defaultNamespace(Namespace.of("http://www.apache.org"))
			.keepNullProperties()
			.build()
			.serialize(m);
		assertEquals(xml1, r);
	}

	//====================================================================================================
	// Test bean name annotation
	//====================================================================================================
	@Test void a03_beanNameAnnotation() throws Exception {
		var e = """
			<Person1>
				<name>John Smith</name>
				<age>123</age>
			</Person1>
			""";
		var r = XmlSerializer.DEFAULT_SQ_READABLE.serialize(new Person1("John Smith", 123));
		assertEquals(e, r);
	}

	/** Class with explicitly specified properties */
	@Bean(typeName="Person1", properties="name,age")
	public static class Person1 {
		public int age;
		protected Person1(String name, int age) {
			this.name = name;
			this.age = age;
		}

		private String name;
		public String getName() { return name; }
		public void setName(String v) { name = v; }
	}

	//====================================================================================================
	// Test trimNulls property.
	//====================================================================================================
	@Test void a04_trimNulls() throws Exception {
		var e = """
			<Person1>
				<age>123</age>
			</Person1>
			""";
		var r = XmlSerializer.DEFAULT_SQ_READABLE.serialize(new Person1(null, 123));
		assertEquals(e, r);
	}

	//====================================================================================================
	// Element name.
	//====================================================================================================
	@Test void a05_elementName() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new A();
		var r = s.serialize(t);
		assertEquals("<foo><f1>1</f1></foo>", r);
		validateXml(t);
	}

	@Bean(typeName="foo")
	public static class A {
		public int f1 = 1;
	}

	//====================================================================================================
	// Element name on superclass.
	//====================================================================================================
	@Test void a06_elementNameOnSuperclass() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new B2();
		var r = s.serialize(t);
		assertEquals("<foo><f1>1</f1></foo>", r);
		validateXml(t);
	}

	public static class B1 extends A {}
	public static class B2 extends B1 {}

	//====================================================================================================
	// Element name on interface.
	//====================================================================================================
	@Test void a07_elementNameOnInterface() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new C3();
		var r = s.serialize(t);
		assertEquals("<foo><f1>1</f1></foo>", r);
		validateXml(t);
	}

	@Bean(typeName="foo")
	public interface C1 {}
	public static class C2 implements C1 {}
	public static class C3 extends C2 {
		public int f1 = 1;
	}

	//====================================================================================================
	// Element name with invalid XML characters.
	//====================================================================================================
	@Test void a08_elementNameWithInvalidChars() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new D();
		var r = s.serialize(t);
		assertEquals("<_x007E__x0021__x0040__x0023__x0024__x0025__x005E__x0026__x002A__x0028__x0029___x002B__x0060_-_x003D__x007B__x007D__x007C__x005B__x005D__x005C__x003A__x0022__x003B__x0027__x003C__x003E__x003F__x002C_._x000A__x000D__x0009__x0008_><f1>1</f1></_x007E__x0021__x0040__x0023__x0024__x0025__x005E__x0026__x002A__x0028__x0029___x002B__x0060_-_x003D__x007B__x007D__x007C__x005B__x005D__x005C__x003A__x0022__x003B__x0027__x003C__x003E__x003F__x002C_._x000A__x000D__x0009__x0008_>", r);
		t = p.parse(r, D.class);
		validateXml(t);
	}

	@Bean(typeName="~!@#$%^&*()_+`-={}|[]\\:\";'<>?,.\n\r\t\b")
	public static class D {
		public int f1 = 1;
	}

	//====================================================================================================
	// Field of type collection with element name.
	// Element name should be ignored.
	//====================================================================================================
	@Test void a09_ignoreCollectionFieldWithElementName() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new G();
		t.f1.add("bar");
		var r = s.serialize(t);
		assertEquals("<bar><foo _name='f1'><string>bar</string></foo></bar>", r);
		t = p.parse(r, G.class);
		validateXml(t);
	}

	@Bean(typeName="foo")
	public static class F extends LinkedList<String>{}

	@Bean(typeName="bar")
	public static class G {
		public F f1 = new F();
	}

	//====================================================================================================
	// Element name on beans of a collection.
	//====================================================================================================
	@Test void a10_elementNameOnBeansOfCollection() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var o = new J1();
		var r = s.serialize(o);
		assertEquals("<foo><f1><bar><f2>2</f2></bar></f1></foo>", r);
	}

	@Bean(typeName="foo")
	public static class J1 {
		@Beanp(properties="f2") public List<J2> f1 = list(new J2());
	}

	@Bean(typeName="bar")
	public static class J2 {
		public int f2 = 2;
		public int f3 = 3;
	}

	//====================================================================================================
	// @Xml.ns without matching nsUri.
	//====================================================================================================
	@Test void a11_xmlNsWithoutMatchingNsUri() {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new K();
		assertThrowsWithMessage(SerializeException.class, "Found @Xml.prefix annotation with no matching URI.  prefix='foo'", ()->s.serialize(t));
	}

	@Xml(prefix="foo")
	public static class K {
		public int f1;
	}

	//====================================================================================================
	// @Xml.format=ATTR.
	//====================================================================================================
	@Test void a12_xmlFormatAttr() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new L();
		var r = s.serialize(t);
		assertEquals("<object f2='2'><f1>1</f1><f3>3</f3></object>", r);
		t.f1 = 4; t.f2 = 5; t.f3 = 6;
		t = p.parse(s.serialize(t), L.class);
		assertEquals(4, t.f1);
		assertEquals(5, t.f2);
		assertEquals(6, t.f3);
		validateXml(t);
	}

	public static class L {
		public int f1 = 1;
		@Xml(format=ATTR)
		public int f2 = 2;
		public int f3 = 3;
	}

	//====================================================================================================
	// @Xml.format=ATTR with namespaces.
	//====================================================================================================
	@Test void a13_xmlFormatAttrWithNs() throws Exception {
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;
		var t = new M();
		var r = n(String.class);
		r = s.build().serialize(t);
		assertEquals("<object f1='1' f2='2' f3='3'/>", r);
		s.enableNamespaces().addNamespaceUrisToRoot().keepNullProperties();
		t.f1 = 4; t.f2 = 5; t.f3 = 6;
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:bar='http://bar' xmlns:foo='http://foo' xmlns:baz='http://baz' bar:f1='4' foo:f2='5' baz:f3='6'/>", r);
		t = p.parse(r, M.class);
		assertEquals(4, t.f1);
		assertEquals(5, t.f2);
		assertEquals(6, t.f3);
		validateXml(t, s.build());
	}

	@Xml(prefix="bar", namespace="http://bar")
	public static class M {
		@Xml(format=ATTR)
		public int f1 = 1;
		@Xml(prefix="foo", format=ATTR, namespace="http://foo")
		public int f2 = 2;
		@Xml(prefix="baz", namespace="http://baz", format=ATTR)
		public int f3 = 3;
	}

	//====================================================================================================
	// _xXXXX_ notation.
	//====================================================================================================
	@Test void a14_xXXXNotation() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		String in, r;

		in = "\u0001";
		r = s.serialize(in);
		assertEquals("<string>_x0001_</string>", r);
		in = p.parse(r, String.class);
		assertEquals("\u0001", in);

		in = "_x0001_";
		r = s.serialize(in);
		assertEquals("<string>_x005F_x0001_</string>", r);
		in = p.parse(r, String.class);
		assertEquals("_x0001_", in);

		in = "_x001_";
		r = s.serialize(in);
		assertEquals("<string>_x001_</string>", r);
		in = p.parse(r, String.class);
		assertEquals("_x001_", in);

		in = "_x00001_";
		r = s.serialize(in);
		assertEquals("<string>_x00001_</string>", r);
		in = p.parse(r, String.class);
		assertEquals("_x00001_", in);

		in = "_xx001_";
		r = s.serialize(in);
		assertEquals("<string>_xx001_</string>", r);
		in = p.parse(r, String.class);
		assertEquals("_xx001_", in);
	}

	//====================================================================================================
	// @Bean.uri annotation formatted as element
	//====================================================================================================
	@Test void a15_beanUriAnnotationFormattedAsElement() throws Exception {
		var p = XmlParser.DEFAULT;
		var s = XmlSerializer.DEFAULT_SQ;

		var t = new N("http://foo",123, "bar");
		var r = s.serialize(t);
		assertEquals("<object><url>http://foo</url><id>123</id><name>bar</name></object>", r);

		t = p.parse(r, N.class);
		assertEquals("http://foo", t.url.toString());
		assertEquals(123, t.id);
		assertEquals("bar", t.name);

		validateXml(t, s);
	}

	@Bean(properties="url,id,name")
	public static class N {
		@Xml(format=ELEMENT) public URL url;
		public int id;
		public String name;
		public N() {}
		public N(String url, int id, String name) {
			this.url = url(url);
			this.id = id;
			this.name = name;
		}
	}

	//====================================================================================================
	// @Bean.uri as elements, overridden element names
	//====================================================================================================
	@Test void a16_overriddenBeanUriAsElementNames() throws Exception {
		var p = XmlParser.DEFAULT;
		var s = XmlSerializer.DEFAULT_SQ;

		var t = new O("http://foo", 123, "bar");
		var r = s.serialize(t);
		assertEquals("<object><url2>http://foo</url2><id2>123</id2><name>bar</name></object>", r);

		t = p.parse(r, O.class);
		assertEquals("http://foo", t.url.toString());
		assertEquals(123, t.id);
		assertEquals("bar", t.name);

		validateXml(t, s);
	}

	@Bean(properties="url2,id2,name")
	public static class O {
		@Beanp(name="url2") @Xml(format=ELEMENT) public URL url;
		@Beanp(name="id2") public int id;
		public String name;
		public O() {}
		public O(String url, int id, String name) {
			this.url = url(url);
			this.id = id;
			this.name = name;
		}
	}

	//====================================================================================================
	// @Bean.uri and @Bean.id annotations, overridden attribute names
	//====================================================================================================
	@Test void a17_overriddenBeanUriAndIdAnnotations() throws Exception {
		var p = XmlParser.DEFAULT;
		var s = XmlSerializer.DEFAULT_SQ;

		var t = new P("http://foo", 123, "bar");
		var r = s.serialize(t);
		assertEquals("<object url2='http://foo' id2='123'><name>bar</name></object>", r);

		t = p.parse(r, P.class);
		assertEquals("http://foo", t.url.toString());
		assertEquals(123, t.id);
		assertEquals("bar", t.name);

		validateXml(t, s);
	}

	@Bean(properties="url2,id2,name")
	public static class P {
		@Beanp(name="url2") @Xml(format=ATTR) public URL url;
		@Beanp(name="id2") @Xml(format=ATTR) public int id;
		public String name;
		public P() {}
		public P(String url, int id, String name) {
			this.url = url(url);
			this.id = id;
			this.name = name;
		}
	}

	//====================================================================================================
	// Namespace on class
	//====================================================================================================
	@Test void a18_nsOnClass() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T1();
		var r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T1.class)));

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T1.class)));
		validateXml(t, s.build());

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T1.class)));

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T1.class)));

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T1.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// Namespace on class with element name.
	//====================================================================================================
	@Test void a19_nsOnClassWithElementName() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T2();
		var r = s.build().serialize(t);
		assertEquals("<T2><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T2>", r);
		assertTrue(t.equals(p.parse(r, T2.class)));

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<foo:T2><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T2>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<foo:T2 xmlns='http://www.apache.org/2013/Juneau'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T2>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<foo:T2 xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T2>", r);
		assertTrue(t.equals(p.parse(r, T2.class)));
		validateXml(t, s.build());

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<T2><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T2>", r);

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<T2><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T2>", r);

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<foo:T2 xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T2>", r);
		assertTrue(t.equals(p.parse(r, T2.class)));
		validateXml(t, s.build());
	}


	//====================================================================================================
	// Namespace on package, no namespace on class.
	//====================================================================================================
	@Test void a20_nsOnPackageNoNsOnClass() throws Exception {
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;

		var t = new T3();
		var r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T3.class)));
		validateXml(t, s.build());

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></object>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot().disableAutoDetectNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau'><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></object>", r);

		// Manually set namespaces
		s.disableAutoDetectNamespaces();
		s.namespaces(
			Namespace.of("p1","http://p1"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:p1='http://p1' xmlns:bar='http://bar' xmlns:baz='http://baz'><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T3.class)));
		validateXml(t, s.build());

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:p1='http://p1' xmlns:bar='http://bar' xmlns:baz='http://baz'><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T3.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// Namespace on package, no namespace on class, element name on class.
	//====================================================================================================
	@Test void a21_nsOnPackageNoNsOnClassElementNameOnClass() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T4();
		var r = s.build().serialize(t);
		assertEquals("<T4><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T4>", r);
		assertTrue(t.equals(p.parse(r, T4.class)));

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<p1:T4><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></p1:T4>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<p1:T4 xmlns='http://www.apache.org/2013/Juneau'><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></p1:T4>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz"),
			Namespace.of("p1","http://p1")
		);
		r = s.build().serialize(t);
		assertEquals("<p1:T4 xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz' xmlns:p1='http://p1'><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></p1:T4>", r);
		assertTrue(t.equals(p.parse(r, T4.class)));
		validateXml(t, s.build());

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<T4><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T4>", r);

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<T4><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T4>", r);

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<p1:T4 xmlns='http://www.apache.org/2013/Juneau' xmlns:p1='http://p1' xmlns:bar='http://bar' xmlns:baz='http://baz'><p1:f1>1</p1:f1><bar:f2>2</bar:f2><p1:f3>3</p1:f3><baz:f4>4</baz:f4></p1:T4>", r);
		assertTrue(t.equals(p.parse(r, T4.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// Namespace on package, namespace on class, element name on class.
	//====================================================================================================
	@Test void a22_nsOnPackageNsOnClassElementNameOnClass() throws Exception {
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;

		var t = new T5();
		var r = s.build().serialize(t);
		assertEquals("<T5><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T5>", r);
		assertTrue(t.equals(p.parse(r, T5.class)));
		validateXml(t, s.build());

		s.ns().disableAutoDetectNamespaces();
		r = s.build().serialize(t);
		assertEquals("<foo:T5><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T5>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<foo:T5 xmlns='http://www.apache.org/2013/Juneau'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T5>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<foo:T5 xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T5>", r);
		assertTrue(t.equals(p.parse(r, T5.class)));
		validateXml(t, s.build());

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<T5><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T5>", r);
		validateXml(t, s.build());

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<T5><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></T5>", r);
		validateXml(t, s.build());

		s.ns();
		r = s.build().serialize(t);
		assertEquals("<foo:T5 xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></foo:T5>", r);
		assertTrue(t.equals(p.parse(r, T5.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// Namespace on package, namespace on class, no element name on class.
	//====================================================================================================
	@Test void a23_nsOnPackageNsOnClassNoElementNameOnClass() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T6();
		var r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T6.class)));

		s.ns();
		r = s.build().serialize(t);
		assertEquals("<object><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot().disableAutoDetectNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T6.class)));
		validateXml(t, s.build());

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		validateXml(t, s.build());

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		validateXml(t, s.build());

		s.ns();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><foo:f1>1</foo:f1><bar:f2>2</bar:f2><foo:f3>3</foo:f3><baz:f4>4</baz:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T6.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// Combination of namespaces and overridden bean property names.
	//====================================================================================================
	@Test void a24_comboOfNsAndOverriddenBeanPropertyNames() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T7();
		var r = s.build().serialize(t);
		assertEquals("<object><g1>1</g1><g2>2</g2><g3>3</g3><g4>4</g4></object>", r);
		assertTrue(t.equals(p.parse(r, T7.class)));

		s.enableNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object><p1:g1>1</p1:g1><bar:g2>2</bar:g2><p1:g3>3</p1:g3><baz:g4>4</baz:g4></object>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot().disableAutoDetectNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau'><p1:g1>1</p1:g1><bar:g2>2</bar:g2><p1:g3>3</p1:g3><baz:g4>4</baz:g4></object>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz"),
			Namespace.of("p1","http://p1")
		);
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz' xmlns:p1='http://p1'><p1:g1>1</p1:g1><bar:g2>2</bar:g2><p1:g3>3</p1:g3><baz:g4>4</baz:g4></object>", r);
		assertTrue(t.equals(p.parse(r, T7.class)));

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<object><g1>1</g1><g2>2</g2><g3>3</g3><g4>4</g4></object>", r);

		r = s.build().serialize(t);
		assertEquals("<object><g1>1</g1><g2>2</g2><g3>3</g3><g4>4</g4></object>", r);

		s.ns().addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:p1='http://p1' xmlns:bar='http://bar' xmlns:baz='http://baz'><p1:g1>1</p1:g1><bar:g2>2</bar:g2><p1:g3>3</p1:g3><baz:g4>4</baz:g4></object>", r);
		assertTrue(t.equals(p.parse(r, T7.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// @XmlNs annotation
	//====================================================================================================
	@Test void a25_xmlNsAnnotation() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T8();
		var r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T8.class)));

		s.ns().disableAutoDetectNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object><p2:f1>1</p2:f1><p1:f2>2</p1:f2><c1:f3>3</c1:f3><f1:f4>4</f1:f4></object>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau'><p2:f1>1</p2:f1><p1:f2>2</p1:f2><c1:f3>3</c1:f3><f1:f4>4</f1:f4></object>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><p2:f1>1</p2:f1><p1:f2>2</p1:f2><c1:f3>3</c1:f3><f1:f4>4</f1:f4></object>", r);

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		assertTrue(t.equals(p.parse(r, T8.class)));
		validateXml(t, s.build());

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1><f2>2</f2><f3>3</f3><f4>4</f4></object>", r);
		validateXml(t, s.build());

		s.ns();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:p2='http://p2' xmlns:p1='http://p1' xmlns:c1='http://c1' xmlns:f1='http://f1'><p2:f1>1</p2:f1><p1:f2>2</p1:f2><c1:f3>3</c1:f3><f1:f4>4</f1:f4></object>", r);
		assertTrue(t.equals(p.parse(r, T8.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// @Xml.ns on package, @Xml.nsUri not on package but in @XmlNs.
	//====================================================================================================
	@Test void a26_xmlNsOnPackageNsUriInXmlNs() throws Exception {
		var s = XmlSerializer.create().sq().disableAutoDetectNamespaces();
		var p = XmlParser.DEFAULT;

		var t = new T9();
		var r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1></object>", r);
		assertTrue(t.equals(p.parse(r, T9.class)));

		s.ns().disableAutoDetectNamespaces();
		r = s.build().serialize(t);
		assertEquals("<object><p1:f1>1</p1:f1></object>", r);

		// Add namespace URIs to root, but don't auto-detect.
		// Only xsi should be added to root.
		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau'><p1:f1>1</p1:f1></object>", r);

		// Manually set namespaces
		s.namespaces(
			Namespace.of("foo","http://foo"),
			Namespace.of("bar","http://bar"),
			Namespace.of("baz","http://baz")
		);
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:foo='http://foo' xmlns:bar='http://bar' xmlns:baz='http://baz'><p1:f1>1</p1:f1></object>", r);

		// Auto-detect namespaces.
		s = XmlSerializer.create().sq();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1></object>", r);
		assertTrue(t.equals(p.parse(r, T9.class)));
		validateXml(t, s.build());

		s.addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object><f1>1</f1></object>", r);
		validateXml(t, s.build());

		s.ns();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:p1='http://p1'><p1:f1>1</p1:f1></object>", r);
		assertTrue(t.equals(p.parse(r, T9.class)));
		validateXml(t, s.build());
	}

	//====================================================================================================
	// @Xml.format=ATTR
	//====================================================================================================
	@Test void a27_xmlAttrs() throws Exception {
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;

		var t = new Q();
		t.f1 = url("http://xf1");
		t.f2 = "xf2";
		t.f3 = "xf3";
		var r = s.build().serialize(t);
		assertEquals("<object f1='http://xf1' f2='xf2' x3='xf3'/>", r);
		t = p.parse(r, Q.class);
		assertEquals("http://xf1", t.f1.toString());
		assertEquals("xf2", t.f2);
		assertEquals("xf3", t.f3);

		s.ns().addNamespaceUrisToRoot();
		r = s.build().serialize(t);
		assertEquals("<object xmlns='http://www.apache.org/2013/Juneau' xmlns:ns='http://ns' xmlns:nsf1='http://nsf1' xmlns:nsf3='http://nsf3' nsf1:f1='http://xf1' ns:f2='xf2' nsf3:x3='xf3'/>", r);
		validateXml(t, s.build());

		t = p.parse(r, Q.class);
		assertEquals("http://xf1", t.f1.toString());
		assertEquals("xf2", t.f2);
		assertEquals("xf3", t.f3);
	}

	@Xml(prefix="ns", namespace="http://ns")
	public static class Q {

		@Xml(format=ATTR, prefix="nsf1", namespace="http://nsf1")
		public URL f1;

		@Xml(format=ATTR)
		public String f2;

		@Beanp(name="x3")
		@Xml(format=ATTR, prefix="nsf3", namespace="http://nsf3")
		public String f3;

		public Q() throws Exception {
			f1 = url("http://f1");
			f2 = "f2";
			f3 = "f3";
		}
	}
}