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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.Map;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.html5.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"serial"})
class RoundTripBeanMaps_Test extends TestBase {

	private static RoundTrip_Tester[] TESTERS = {
		tester(1, "Json - default")
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(2, "Json - lax")
			.serializer(JsonSerializer.create().json5().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(3, "Json - lax, readable")
			.serializer(JsonSerializer.create().json5().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(4, "Xml - namespaces, validation, readable")
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester(5, "Xml - no namespaces, validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(6, "Html - default")
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(7, "Html - readable")
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(8, "Html - with key/value headers")
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(9, "Uon - default")
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(10, "Uon - readable")
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(11, "Uon - encoded")
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding())
			.build(),
		tester(12, "UrlEncoding - default")
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(13, "UrlEncoding - readable")
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(14, "UrlEncoding - expanded params")
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().expandedParams())
			.build(),
		tester(15, "MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MsgPackParser.create())
			.build(),
		tester(16, "Json schema")
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.returnOriginalObject()
			.build(),
	};

	static RoundTrip_Tester[]  testers() {
		return TESTERS;
	}

	protected static RoundTrip_Tester.Builder tester(int index, String label) {
		return RoundTrip_Tester.create(index, label).annotatedClasses(L2Config.class, M2Config.class).implClasses(Map.of(IBean.class, CBean.class));
	}

	//====================================================================================================
	// IBean/ABean/Bean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_implClasses(RoundTrip_Tester t) throws Exception {
		var bean = new CBean();

		bean.setF1("bar");
		bean = t.roundTrip(bean, IBean.class);
		assertEquals("bar", bean.getF1());

		bean.setF1("bing");
		bean = t.roundTrip(bean, CBean.class);
		assertEquals("bing", bean.getF1());
	}

	//====================================================================================================
	// IBean[]/ABean[]/Bean[]
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_implArrayClasses(RoundTrip_Tester t) throws Exception {
		var bean = (IBean[])new CBean[]{new CBean()};

		bean[0].setF1("bar");
		bean = t.roundTrip(bean, IBean[].class);
		assertEquals("bar", bean[0].getF1());

		bean[0].setF1("bing");
		bean = t.roundTrip(bean, CBean[].class);
		assertEquals("bing", bean[0].getF1());
	}

	//====================================================================================================
	// List<IBean/ABean/Bean>
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_implListClasses(RoundTrip_Tester t) throws Exception {
		var l = alist(new CBean());

		l.get(0).setF1("bar");
		l = t.roundTrip(l, List.class, IBean.class);
		assertBean(l.get(0), "f1", "bar");
		l = t.roundTrip(l, LinkedList.class, IBean.class);
		assertBean(l.get(0), "f1", "bar");

		l.get(0).setF1("bing");
		l = t.roundTrip(l, List.class, CBean.class);
		assertBean(l.get(0), "f1", "bing");
		l = t.roundTrip(l, LinkedList.class, CBean.class);
		assertBean(l.get(0), "f1", "bing");
	}

	//====================================================================================================
	// Map<String,IBean/ABean/Bean>
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a04_implMap(RoundTrip_Tester t) throws Exception {
		var l = CollectionUtils.map("foo",new CBean());

		l.get("foo").setF1("bar");
		l = t.roundTrip(l, Map.class, String.class, IBean.class);
		assertBean(l.get("foo"), "f1", "bar");
		l = t.roundTrip(l, LinkedHashMap.class, String.class, IBean.class);
		assertBean(l.get("foo"), "f1", "bar");

		l.get("foo").setF1("bing");
		l = t.roundTrip(l, Map.class, String.class, CBean.class);
		assertBean(l.get("foo"), "f1", "bing");
		l = t.roundTrip(l, LinkedHashMap.class, String.class, CBean.class);
		assertBean(l.get("foo"), "f1", "bing");
	}

	//====================================================================================================
	// Map<String,IBean/ABean/Bean>
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a05_implMap2(RoundTrip_Tester t) throws Exception {
		var b = new A(1);
		b = t.roundTrip(b);
		if (t.returnOriginalObject || t.getParser() == null)
			return;
		assertBean(b, "f1,f2,f3,f4,f5,f6", "0,0,1,1,0,1");
	}

	public interface IBean {
		String getF1();
		void setF1(String v);
	}

	public abstract static class ABean implements IBean {
		@Override /* IBean */
		public abstract String getF1();
		@Override /* IBean */
		public abstract void setF1(String v);
	}

	public static class CBean extends ABean {
		private String f1 = "foo";
		@Override /* IBean */
		public String getF1() { return f1; }
		@Override /* IBean */
		public void setF1(String v) { f1 = v; }
	}

	public static class A {

		@BeanIgnore
		public int f1, f2;
		public int f3, f4;

		private int f5, f6;

		@BeanIgnore
		public int getF5() { return f5; }
		public void setF5(int v) { f5 = v; }

		public int getF6() { return f6; }
		public A withF6(int v) { f6 = v; return this; }

		public A() {}

		public A(int v) {
			f1 = f2 = f3 = f4 = f5 = f6 = v;
		}
	}

	//====================================================================================================
	// Test @Bean(subTypes=xxx)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a06_subTypesUsingAnnotation(RoundTrip_Tester t) throws Exception {
		var js = JsonSerializer.create().json5().addBeanTypes().addRootType().build();

		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t. getParser();

		var b1 = B1.create();
		var r = s.serialize(b1);
		var b = p.parse(r, B.class);
		assertTrue(b instanceof B1);
		assertSerialized(b, js, "{_type:'B1',f0:'f0',f1:'f1'}");

		var b2 = B2.create();
		r = s.serialize(b2);
		b = p.parse(r, B.class);
		assertTrue(b instanceof B2);
		assertSerialized(b, js, "{_type:'B2',f0:'f0',f2:1}");

		var b3 = B3.create();
		r = s.serialize(b3);
		b = p.parse(r, B.class);
		assertTrue(b instanceof B3);
		assertSerialized(b, js, "{_type:'B3',f0:'f0',f3:'2001-01-01T12:34:56.789Z'}");
	}

	@Bean(
		dictionary={B1.class,B2.class,B3.class}
	)
	public abstract static class B {
		public String f0 = "f0";
	}

	@Bean(typeName="B1")
	public static class B1 extends B {
		public String f1;
		public static B1 create() {
			var b = new B1();
			b.f0 = "f0";
			b.f1 = "f1";
			return b;
		}
	}

	@Bean(typeName="B2")
	public static class B2 extends B {
		public int f2;
		public static B2 create() {
			var b = new B2();
			b.f0 = "f0";
			b.f2 = 1;
			return b;
		}
	}

	@Bean(typeName="B3")
	public static class B3 extends B {
		public XMLGregorianCalendar f3;
		public static B3 create() throws Exception {
			var b = new B3();
			b.f0 = "f0";
			b.f3 = DatatypeFactory.newInstance().newXMLGregorianCalendar("2001-01-01T12:34:56.789Z");
			return b;
		}
	}

	//====================================================================================================
	// Test @Bean(subTypes=xxx) using BeanFilter
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a07_subTypesUsingBeanFilter(RoundTrip_Tester t) throws Exception {
		var js = JsonSerializer.create().json5().build();

		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer().copy().dictionaryOn(C.class, CFilterDictionaryMap.class).build();
		var p = t.getParser().copy().dictionaryOn(C.class, CFilterDictionaryMap.class).build();

		var c1 = C1.create();
		var r = s.serialize(c1);
		var c = p.parse(r, C.class);
		assertTrue(c instanceof C1);
		assertSerialized(c, js, "{f0:'f0',f1:'f1'}");

		var c2 = C2.create();
		r = s.serialize(c2);
		c = p.parse(r, C.class);
		assertTrue(c instanceof C2);
		assertSerialized(c, js, "{f0:'f0',f2:1}");

		var c3 = C3.create();
		r = s.serialize(c3);
		c = p.parse(r, C.class);
		assertTrue(c instanceof C3);
		assertSerialized(c, js, "{f0:'f0',f3:'2001-01-01T12:34:56.789Z'}");
	}

	public abstract static class C {
		public String f0;
	}

	public static class C1 extends C {
		public String f1;
		public static C1 create() {
			var c = new C1();
			c.f0 = "f0";
			c.f1 = "f1";
			return c;
		}
	}

	public static class C2 extends C {
		public int f2;
		public static C2 create() {
			var c = new C2();
			c.f0 = "f0";
			c.f2 = 1;
			return c;
		}
	}

	public static class C3 extends C {
		public XMLGregorianCalendar f3;
		public static C3 create() throws Exception {
			var c = new C3();
			c.f0 = "f0";
			c.f3 = DatatypeFactory.newInstance().newXMLGregorianCalendar("2001-01-01T12:34:56.789Z");
			return c;
		}
	}

	public static class CFilterDictionaryMap extends BeanDictionaryMap {
		public CFilterDictionaryMap() {
			append("C1", C1.class);
			append("C2", C2.class);
			append("C3", C3.class);
		}
	}

	//====================================================================================================
	// Test @Bean(subTypeProperty=xxx) with real bean property
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a08_subTypePropertyWithRealPropertyUsingAnnotation(RoundTrip_Tester t) throws Exception {
		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		var ba1 = BA1.create();
		var r = s.serialize(ba1);
		var b = p.parse(r, BA.class);
		assertBean(b, "class{simpleName},f0a,f0b,f1", "{BA1},f0a,f0b,f1");
	}

	@Bean(dictionary={BA1.class,BA2.class})
	public abstract static class BA {
		public String f0a, f0b;
	}

	@Bean(typeName="BA1")
	public static class BA1 extends BA {
		public String f1;
		public static BA1 create() {
			var b = new BA1();
			b.f0a = "f0a";
			b.f0b = "f0b";
			b.f1 = "f1";
			return b;
		}
	}

	@Bean(typeName="BA2")
	public static class BA2 extends BA {
		public String f2;
	}

	//====================================================================================================
	// Test @Bean(subTypes=xxx) with real bean property using BeanFilter
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a09_subTypePropertyWithRealPropertyUsingBeanFilter(RoundTrip_Tester t) throws Exception {
		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer().copy().annotations(BeanAnnotation.create(CA.class).dictionary(CAFilterDictionaryMap.class).build()).build();
		var p = t.getParser().copy().annotations(BeanAnnotation.create(CA.class).dictionary(CAFilterDictionaryMap.class).build()).build();

		var c1 = CA1.create();
		var r = s.serialize(c1);
		var c = p.parse(r, CA.class);
		assertBean(c, "class{simpleName},f0a,f0b,f1", "{CA1},f0a,f0b,f1");
	}

	public abstract static class CA {
		public String f0a, f0b;
	}

	public static class CA1 extends CA {
		public String f1;
		public static CA1 create() {
			var c = new CA1();
			c.f0a = "f0a";
			c.f0b = "f0b";
			c.f1 = "f1";
			return c;
		}
	}

	public static class CA2 extends CA {
		public String f2;
	}

	public static class CAFilterDictionaryMap extends BeanDictionaryMap {
		public CAFilterDictionaryMap() {
			append("CA1", CA1.class);
			append("CA2", CA2.class);
		}
	}

	//====================================================================================================
	// Test @Bean(bpi=xxx)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a10_propertiesUsingAnnotation(RoundTrip_Tester t) throws Exception {
		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		var d = new D1().init();
		var r = s.serialize(d);
		d = p.parse(r, D1.class);
		assertNull(d.f1);
		assertBean(d, "f3,f2", "f3,f2");
	}

	@Bean(p="f3,f2")
	public static class D1 {
		public String f1, f2, f3;
		public D1 init() {
			f1 = "f1";
			f2 = "f2";
			f3 = "f3";
			return this;
		}
	}

	//====================================================================================================
	// Test @Bean(bpi=xxx) using BeanFilter
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a11_propertiesUsingBeanFilter(RoundTrip_Tester t) throws Exception {
		var js = JsonSerializer.create().json5().beanProperties(D2.class, "f3,f2").build();

		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer().copy().beanProperties(D2.class, "f3,f2").build();
		var p = t.getParser().copy().beanProperties(D2.class, "f3,f2").build();

		var d = new D2().init();
		var r = s.serialize(d);
		d = p.parse(r, D2.class);
		assertNull(d.f1);
		assertSerialized(d, js, "{f3:'f3',f2:'f2'}");
	}

	public static class D2 {
		public String f1, f2, f3;
		public D2 init() {
			f1 = "f1";
			f2 = "f2";
			f3 = "f3";
			return this;
		}
	}

	//====================================================================================================
	// Test @Bean(bpx=xxx)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a12_excludePropertiesUsingAnnotation(RoundTrip_Tester t) throws Exception {
		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		var e = new E1().init();
		var r = s.serialize(e);
		e = p.parse(r, E1.class);
		assertBean(e, "f1,f3", "f1,f3");
	}

	@Bean(excludeProperties="f2")
	public static class E1 {
		public String f1, f2, f3;
		public E1 init() {
			f1 = "f1";
			f2 = "f2";
			f3 = "f3";
			return this;
		}
	}

	//====================================================================================================
	// Test @Bean(bpx=xxx) using BeanFilter
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a13_excludePropertiesUsingBeanFilter(RoundTrip_Tester t) throws Exception {
		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer().copy().beanPropertiesExcludes(E2.class, "f2").build();
		var p = t.getParser().copy().beanPropertiesExcludes(E2.class, "f2").build();

		var e = new E2().init();
		var r = s.serialize(e);
		e = p.parse(r, E2.class);
		assertBean(e, "f1,f3", "f1,f3");
	}

	public static class E2 {
		public String f1, f2, f3;
		public E2 init() {
			f1 = "f1";
			f2 = "f2";
			f3 = "f3";
			return this;
		}
	}

	//====================================================================================================
	// Test @Bean(interfaceClass=xxx)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a14_interfaceClassUsingAnnotation(RoundTrip_Tester t) throws Exception {
		// Skip validation-only tests
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		var x = new FA2().init();
		var r = s.serialize(x);
		x = p.parse(r, FA2.class);
		assertBean(x, "f1", "f1");
	}

	@Bean(interfaceClass=FA1.class)
	public static class FA1 {
		public String f1;
	}

	public static class FA2 extends FA1 {
		public String f2;
		public FA2 init() {
			f1 = "f1";
			f2 = "f2";
			return this;
		}
	}

	//====================================================================================================
	// Test @Bean(interfaceClass=xxx) using BeanFilter
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a15_interfaceClassUsingBeanFilter(RoundTrip_Tester t) throws Exception {
		var s = t.getSerializer().copy();
		var p = t.getParser() == null ? null : t.getParser().copy();
		FB2 x;
		Object r;

		// Skip validation-only tests
		if (t.isValidationOnly() || p == null)
			return;

		// --- BeanFilter defined on parent class ---
		s.interfaces(FB1.class);
		p.interfaces(FB1.class);

		x = new FB2().init();
		r = s.build().serialize(x);
		x = p.build().parse(r, FB2.class);
		assertBean(x, "f1", "f1");

		// --- BeanFilter defined on child class class ---
		s.interfaces(FB1.class);
		p.interfaces(FB1.class);

		x = new FB2().init();
		r = s.build().serialize(x);
		x = p.build().parse(r, FB2.class);
		assertBean(x, "f1", "f1");

		// --- BeanFilter defined as plain class ---
		s.interfaces(FB1.class);
		p.interfaces(FB1.class);

		x = new FB2().init();
		r = s.build().serialize(x);
		x = p.build().parse(r, FB2.class);
		assertBean(x, "f1", "f1");
	}

	public static class FB1 {
		public String f1;
	}

	public static class FB2 extends FB1 {
		public String f2;
		public FB2 init() {
			f1 = "f1";
			f2 = "f2";
			return this;
		}
	}

	//====================================================================================================
	// testMemberClass
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a16_memberClass(RoundTrip_Tester t) {
		var x = G.create();
		assertDoesNotThrow(()->t.roundTrip(x, G.class));
	}

	public static class G {
		public int a1;
		public G1 g1;

		public static G create() {
			var g = new G();
			g.a1 = 1;
			g.g1.a2 = 2;
			g.g1.g2.a3 = 3;
			return g;
		}

		public G() {
			g1 = new G1();
		}

		public class G1 {
			public int a2;
			public G2 g2;

			public G1() {
				g2 = new G2();
			}

			public class G2 {
				public int a3;
			}
		}
	}

	//====================================================================================================
	// testMemberClassWithMapClass
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a17_memberClassWithMapClass(RoundTrip_Tester t) {
		var x = H.create();
		assertDoesNotThrow(()->t.roundTrip(x, H.class));
	}

	public static class H extends LinkedHashMap<String,H.H1> {

		static H create() {
			var h = new H();
			h.add("foo", 1, 2);
			return h;
		}

		H add(String key, int a2, int a3) {
			var h1 = new H1();
			h1.a2 = a2;
			h1.h2.a3 = a3;
			put(key, h1);
			return this;
		}

		public class H1 {
			public int a2;
			public H2 h2;

			public H1() {
				h2 = new H2();
			}

			public class H2 {
				public int a3;
			}
		}
	}

	//====================================================================================================
	// testMemberClassWithListClass
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a18_memberClassWithListClass(RoundTrip_Tester t) {
		var x = I.create();
		assertDoesNotThrow(()->t.roundTrip(x, I.class));
	}

	public static class I extends LinkedList<I.I1> {

		static I create() {
			var i = new I();
			i.add(1, 2);
			return i;
		}

		I add(int a2, int a3) {
			var i1 = new I1();
			i1.a2 = a2;
			i1.i2.a3 = a3;
			super.add(i1);
			return this;
		}

		public class I1 {
			public int a2;
			public I2 i2;

			public I1() {
				i2 = new I2();
			}

			public class I2 {
				public int a3;
			}
		}
	}

	//====================================================================================================
	// testMemberClassWithStringConstructor
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a19_memberClassWithStringConstructor(RoundTrip_Tester t) {
		var x = J.create();
		assertDoesNotThrow(()->t.roundTrip(x, J.class));
	}

	public static class J {
		public J2 j2;

		static J create() {
			var j = new J();
			j.init();
			return j;
		}

		private void init() {
			j2 = new J2("2");
		}

		public class J2 {
			int a2;

			public J2(String v) {
				a2 = Integer.parseInt(v);
			}

			@Override /* Object */
			public String toString() {
				return String.valueOf(a2);
			}
		}
	}

	//====================================================================================================
	// testBeanPropertyPrecedence
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a20_beanPropertyPrecedence(RoundTrip_Tester t) {
		var x = K.create();
		assertDoesNotThrow(()->t.roundTrip(x, K.class));
	}
	public enum KEnum { FOO, BAR, BAZ }

	public static class K {
		private KEnum a, b, c;

		static K create() {
			var t = new K();
			t.a = KEnum.FOO;
			t.b = KEnum.BAR;
			t.c = KEnum.BAZ;
			return t;
		}

		@BeanIgnore public KEnum getA() { return KEnum.FOO; }
		@Beanp(name="a") public String getA2() { return a.toString(); }
		public void setA(KEnum v) {
			// This method should not be interpreted as the setter for this
			// property because it doesn't match the getter return type above.
			throw new IllegalCallerException("Should not be called!");
		}
		public void setA(String v) { a = KEnum.valueOf(v); }

		public KEnum getB() { return b; }
		public void setB(String v) { throw new IllegalCallerException("Should not be called!"); }
		public void setB(Object v) { throw new IllegalCallerException("Should not be called!"); }
		public void setB(KEnum v) { b = v;}

		public KEnum getC() { return c; }
		public void setC(KEnum v) { c = v; }
		public void setC(String v) { throw new IllegalCallerException("Should not be called!"); }
		public void setC(Object v) { throw new IllegalCallerException("Should not be called!"); }
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a21_wrapperAttrAnnotationOnBean(RoundTrip_Tester t) {
		var x = L.create();
		assertDoesNotThrow(()->t.roundTrip(x, L.class));

		var x2 = new LinkedHashMap<String,L>();
		x2.put("bar", L.create());
		assertDoesNotThrow(()->t.roundTrip(x2, LinkedHashMap.class, String.class, L.class));
	}

	@Json(wrapperAttr="foo")
	public static class L {
		public int f1;

		static L create() {
			var l = new L();
			l.f1 = 1;
			return l;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a22_wrapperAttrAnnotationOnBean_usingConfig(RoundTrip_Tester t) {
		var x = L2.create();
		assertDoesNotThrow(()->t.roundTrip(x, L2.class));

		var x2 = new LinkedHashMap<String,L2>();
		x2.put("bar", L2.create());
		assertDoesNotThrow(()->t.roundTrip(x2, LinkedHashMap.class, String.class, L2.class));
	}

	@Json(on="L2",wrapperAttr="foo")
	private static class L2Config {}

	public static class L2 {
		public int f1;

		static L2 create() {
			var l = new L2();
			l.f1 = 1;
			return l;
		}
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnNonBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a23_wrapperAttrAnnotationOnNonBean(RoundTrip_Tester t) {
		var x = M.create();
		assertDoesNotThrow(()->t.roundTrip(x, M.class));

		var x2 = new LinkedHashMap<String,M>();
		x2.put("bar", M.create());
		assertDoesNotThrow(()->t.roundTrip(x2, LinkedHashMap.class, String.class, M.class));
	}

	@Json(wrapperAttr="foo")
	public static class M {
		int f1;

		static M create() {
			var m = new M();
			m.f1 = 1;
			return m;
		}

		@Override /* Object */
		public String toString() {
			return String.valueOf(f1);
		}

		public static M valueOf(String s) {
			var m = new M();
			m.f1 = Integer.parseInt(s);
			return m;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a24_WrapperAttrAnnotationOnNonBean_usingConfig(RoundTrip_Tester t) {
		var x = M2.create();
		assertDoesNotThrow(()->t.roundTrip(x, M2.class));

		var x2 = new LinkedHashMap<String,M2>();
		x2.put("bar", M2.create());
		assertDoesNotThrow(()->t.roundTrip(x2, LinkedHashMap.class, String.class, M2.class));
	}

	@Json(on="M2",wrapperAttr="foo")
	private static class M2Config {}

	public static class M2 {
		int f1;

		static M2 create() {
			var m = new M2();
			m.f1 = 1;
			return m;
		}

		@Override /* Object */
		public String toString() {
			return String.valueOf(f1);
		}

		public static M2 valueOf(String s) {
			var m = new M2();
			m.f1 = Integer.parseInt(s);
			return m;
		}
	}

	//====================================================================================================
	// testBeanPropertyWithBeanWithAttrsField
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a25_BeanPropertyWithBeanWithAttrsField(RoundTrip_Tester t) throws Exception {
		var x = N.create();
		x = t.roundTrip(x, N.class);

		x.f1.type("foo");
		x = t.roundTrip(x, N.class);

		x.f1.attr("foo", "bar").attrUri("href", "http://foo");
		t.roundTrip(x, N.class);

		var x2 = new Head().child(new Style());
		assertDoesNotThrow(()->t.roundTrip(x2, Head.class));
	}

	public static class N {
		public Style f1;

		static N create() {
			var n = new N();
			n.f1 = new Style();
			return n;
		}
	}
}