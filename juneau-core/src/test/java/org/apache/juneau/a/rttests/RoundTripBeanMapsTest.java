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
import static org.junit.Assert.*;

import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","hiding","serial","unused","javadoc"})
public class RoundTripBeanMapsTest extends RoundTripTest {

	public RoundTripBeanMapsTest(String label, Serializer s, Parser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	@Override /* RoundTripTest */
	public Map<Class<?>,Class<?>> getImplClasses() {
		Map<Class<?>,Class<?>> m = new HashMap<Class<?>,Class<?>>();
		m.put(IBean.class, CBean.class);
		return m;
	}

	//====================================================================================================
	// IBean/ABean/Bean
	//====================================================================================================
	@Test
	public void testImplClasses() throws Exception {
		IBean bean = new CBean();

		bean.setF1("bar");
		bean = roundTrip(bean, IBean.class);
		assertEquals("bar", bean.getF1());

		bean.setF1("baz");
		bean = roundTrip(bean, ABean.class);
		assertEquals("baz", bean.getF1());

		bean.setF1("bing");
		bean = roundTrip(bean, CBean.class);
		assertEquals("bing", bean.getF1());
	}

	//====================================================================================================
	// IBean[]/ABean[]/Bean[]
	//====================================================================================================
	@Test
	public void testImplArrayClasses() throws Exception {
		IBean[] bean = new CBean[]{new CBean()};

		bean[0].setF1("bar");
		bean = roundTrip(bean, IBean[].class);
		assertEquals("bar", bean[0].getF1());

		bean[0].setF1("baz");
		bean = roundTrip(bean, ABean[].class);
		assertEquals("baz", bean[0].getF1());

		bean[0].setF1("bing");
		bean = roundTrip(bean, CBean[].class);
		assertEquals("bing", bean[0].getF1());
	}

	//====================================================================================================
	// List<IBean/ABean/Bean>
	//====================================================================================================
	@Test
	public void testImplListClasses() throws Exception {
		List<IBean> l = new LinkedList<IBean>() {{
			add(new CBean());
		}};

		l.get(0).setF1("bar");
		l = roundTripCollection(l, List.class, IBean.class);
		assertEquals("bar", l.get(0).getF1());
		l = roundTripCollection(l, LinkedList.class, IBean.class);
		assertEquals("bar", l.get(0).getF1());

		l.get(0).setF1("baz");
		l = roundTripCollection(l, List.class, ABean.class);
		assertEquals("baz", l.get(0).getF1());
		l = roundTripCollection(l, LinkedList.class, ABean.class);
		assertEquals("baz", l.get(0).getF1());

		l.get(0).setF1("bing");
		l = roundTripCollection(l, List.class, CBean.class);
		assertEquals("bing", l.get(0).getF1());
		l = roundTripCollection(l, LinkedList.class, CBean.class);
		assertEquals("bing", l.get(0).getF1());
	}

	//====================================================================================================
	// Map<String,IBean/ABean/Bean>
	//====================================================================================================
	@Test
	public void testImplMap() throws Exception {
		Map<String,IBean> l = new LinkedHashMap<String,IBean>() {{
			put("foo", new CBean());
		}};

		l.get("foo").setF1("bar");
		l = roundTripMap(l, Map.class, String.class, IBean.class);
		assertEquals("bar", l.get("foo").getF1());
		l = roundTripMap(l, LinkedHashMap.class, String.class, IBean.class);
		assertEquals("bar", l.get("foo").getF1());

		l.get("foo").setF1("baz");
		l = roundTripMap(l, Map.class, String.class, ABean.class);
		assertEquals("baz", l.get("foo").getF1());
		l = roundTripMap(l, LinkedHashMap.class, String.class, ABean.class);
		assertEquals("baz", l.get("foo").getF1());

		l.get("foo").setF1("bing");
		l = roundTripMap(l, Map.class, String.class, CBean.class);
		assertEquals("bing", l.get("foo").getF1());
		l = roundTripMap(l, LinkedHashMap.class, String.class, CBean.class);
		assertEquals("bing", l.get("foo").getF1());
	}

	//====================================================================================================
	// Map<String,IBean/ABean/Bean>
	//====================================================================================================
	@Test
	public void testImplMap2() throws Exception {
		A b = new A(1);
		b = roundTrip(b);
		if (returnOriginalObject || p == null)
			return;
		assertEquals(0, b.f1);
		assertEquals(0, b.f2);
		assertEquals(1, b.f3);
		assertEquals(1, b.f4);
		assertEquals(0, b.getF5());
		assertEquals(1, b.getF6());
	}

	public static interface IBean {
		public String getF1();
		public void setF1(String f1);
	}

	public static abstract class ABean implements IBean {
		@Override /* IBean */
		public abstract String getF1();
		@Override /* IBean */
		public abstract void setF1(String f1);
	}

	public static class CBean extends ABean {
		private String f1 = "foo";
		@Override /* IBean */
		public String getF1() {
			return f1;
		}
		@Override /* IBean */
		public void setF1(String f1) {
			this.f1 = f1;
		}
	}

	public static class A {

		@BeanIgnore
		public int f1, f2;
		public int f3, f4;

		private int f5, f6;

		@BeanIgnore
		public int getF5() {
			return f5;
		}
		public void setF5(int f5) {
			this.f5 = f5;
		}

		public int getF6() {
			return f6;
		}
		public void setF6(int f6) {
			this.f6 = f6;
		}

		public A() {}

		public A(int v) {
			f1 = f2 = f3 = f4 = f5 = f6 = v;
		}
	}

	//====================================================================================================
	// Test @Bean(subTypes=xxx)
	//====================================================================================================
	@Test
	public void testSubTypesUsingAnnotation() throws Exception {
		JsonSerializer js = JsonSerializer.DEFAULT_LAX.clone().addPojoSwaps(XMLGregorianCalendarSwap.class);

		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer().clone().addPojoSwaps(XMLGregorianCalendarSwap.class);
		Parser p = getParser().clone().addPojoSwaps(XMLGregorianCalendarSwap.class);

		B1 b1 = B1.create();
		Object r = s.serialize(b1);
		B b = p.parse(r, B.class);
		assertTrue(b instanceof B1);
		assertObjectEquals("{subType:'B1',f0:'f0',f1:'f1'}", b, js);

		B2 b2 = B2.create();
		r = s.serialize(b2);
		b = p.parse(r, B.class);
		assertTrue(b instanceof B2);
		assertObjectEquals("{subType:'B2',f0:'f0',f2:1}", b, js);

		B3 b3 = B3.create();
		r = s.serialize(b3);
		b = p.parse(r, B.class);
		assertTrue(b instanceof B3);
		assertObjectEquals("{subType:'B3',f0:'f0',f3:'2001-01-01T12:34:56.789Z'}", b, js);
}

	@Bean(
		subTypeProperty="subType",
		subTypes={B1.class,B2.class,B3.class}
	)
	public abstract static class B {
		public String f0 = "f0";
	}

	@Bean(typeName="B1")
	public static class B1 extends B {
		public String f1;
		public static B1 create() {
			B1 b = new B1();
			b.f0 = "f0";
			b.f1 = "f1";
			return b;
		}
	}

	@Bean(typeName="B2")
	public static class B2 extends B {
		public int f2;
		public static B2 create() {
			B2 b = new B2();
			b.f0 = "f0";
			b.f2 = 1;
			return b;
		}
	}

	@Bean(typeName="B3")
	public static class B3 extends B {
		public XMLGregorianCalendar f3;
		public static B3 create() throws Exception {
			B3 b = new B3();
			b.f0 = "f0";
			b.f3 = DatatypeFactory.newInstance().newXMLGregorianCalendar("2001-01-01T12:34:56.789Z");
			return b;
		}
	}

	//====================================================================================================
	// Test @Bean(subTypes=xxx) using BeanFilter
	//====================================================================================================
	@Test
	public void testSubTypesUsingBeanFilter() throws Exception {
		JsonSerializer js = JsonSerializer.DEFAULT_LAX.clone().addPojoSwaps(XMLGregorianCalendarSwap.class);

		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer().clone().addBeanFilters(CFilter.class).addPojoSwaps(XMLGregorianCalendarSwap.class);
		Parser p = getParser().clone().addBeanFilters(CFilter.class).addPojoSwaps(XMLGregorianCalendarSwap.class);

		C1 c1 = C1.create();
		Object r = s.serialize(c1);
		C c = p.parse(r, C.class);
		assertTrue(c instanceof C1);
		assertObjectEquals("{f0:'f0',f1:'f1'}", c, js);

		C2 c2 = C2.create();
		r = s.serialize(c2);
		c = p.parse(r, C.class);
		assertTrue(c instanceof C2);
		assertObjectEquals("{f0:'f0',f2:1}", c, js);

		C3 c3 = C3.create();
		r = s.serialize(c3);
		c = p.parse(r, C.class);
		assertTrue(c instanceof C3);
		assertObjectEquals("{f0:'f0',f3:'2001-01-01T12:34:56.789Z'}", c, js);
	}

	public abstract static class C {
		public String f0;
	}

	public static class C1 extends C {
		public String f1;
		public static C1 create() {
			C1 c = new C1();
			c.f0 = "f0";
			c.f1 = "f1";
			return c;
		}
	}

	public static class C2 extends C {
		public int f2;
		public static C2 create() {
			C2 c = new C2();
			c.f0 = "f0";
			c.f2 = 1;
			return c;
		}
	}

	public static class C3 extends C {
		public XMLGregorianCalendar f3;
		public static C3 create() throws Exception {
			C3 c = new C3();
			c.f0 = "f0";
			c.f3 = DatatypeFactory.newInstance().newXMLGregorianCalendar("2001-01-01T12:34:56.789Z");
			return c;
		}
	}

	public static class CFilter extends BeanFilterBuilder {
		public CFilter() {
			super(C.class);
			setSubTypeProperty("subType");
			addSubType("C1", C1.class);
			addSubType("C2", C2.class);
			addSubType("C3", C3.class);
		}
	}

	//====================================================================================================
	// Test @Bean(subTypeProperty=xxx) with real bean property
	//====================================================================================================
	@Test
	public void testSubTypePropertyWithRealPropertyUsingAnnotation() throws Exception {
		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer();
		Parser p = getParser();

		BA1 ba1 = BA1.create();
		Object r = s.serialize(ba1);
		BA b = p.parse(r, BA.class);
		assertTrue(b instanceof BA1);
		assertEquals("BA1", b.subType);
		assertObjectEquals("{subType:'BA1',f0a:'f0a',f0b:'f0b',f1:'f1'}", b);
	}

	@Bean(
		subTypeProperty="subType",
		subTypes={BA1.class,BA2.class}
	)
	public abstract static class BA {
		public String f0a, subType, f0b;
	}

	@Bean(typeName="BA1")
	public static class BA1 extends BA {
		public String f1;
		public static BA1 create() {
			BA1 b = new BA1();
			b.f0a = "f0a";
			b.f0b = "f0b";
			b.f1 = "f1";
			b.subType = "xxx";// Should be ignored.
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
	@Test
	public void testSubTypePropertyWithRealPropertyUsingBeanFilter() throws Exception {
		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer().clone().addBeanFilters(CAFilter.class);
		Parser p = getParser().clone().addBeanFilters(CAFilter.class);

		CA1 c1 = CA1.create();
		Object r = s.serialize(c1);
		CA c = p.parse(r, CA.class);
		assertTrue(c instanceof CA1);
		assertEquals("CA1", c.subType);
		assertObjectEquals("{f0a:'f0a',subType:'CA1',f0b:'f0b',f1:'f1'}", c);
	}

	public abstract static class CA {
		public String f0a, subType, f0b;
	}

	public static class CA1 extends CA {
		public String f1;
		public static CA1 create() {
			CA1 c = new CA1();
			c.f0a = "f0a";
			c.f0b = "f0b";
			c.f1 = "f1";
			c.subType = "xxx";// Should be ignored.
			return c;
		}
	}

	public static class CA2 extends CA {
		public String f2;
	}

	public static class CAFilter extends BeanFilterBuilder {
		public CAFilter() {
			super(CA.class);
			setSubTypeProperty("subType");
			addSubType("CA1", CA1.class);
			addSubType("CA2", CA2.class);
		}
	}

	//====================================================================================================
	// Test @Bean(properties=xxx)
	//====================================================================================================
	@Test
	public void testPropertiesUsingAnnotation() throws Exception {
		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer();
		Parser p = getParser();

		D1 d = new D1().init();
		Object r = s.serialize(d);
		d = p.parse(r, D1.class);
		assertNull(d.f1);
		assertObjectEquals("{f3:'f3',f2:'f2'}", d);
	}

	@Bean(properties="f3,f2")
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
	// Test @Bean(properties=xxx) using BeanFilter
	//====================================================================================================
	@Test
	public void testPropertiesUsingBeanFilter() throws Exception {
		JsonSerializer js = JsonSerializer.DEFAULT_LAX.clone().addBeanFilters(D2Filter.class);

		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer().clone().addBeanFilters(D2Filter.class);
		Parser p = getParser().clone().addBeanFilters(D2Filter.class);

		D2 d = new D2().init();
		Object r = s.serialize(d);
		d = p.parse(r, D2.class);
		assertNull(d.f1);
		assertObjectEquals("{f3:'f3',f2:'f2'}", d, js);
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
	public static class D2Filter extends BeanFilterBuilder {
		public D2Filter() {
			super(D2.class);
			setProperties("f3,f2");
		}
	}

	//====================================================================================================
	// Test @Bean(excludeProperties=xxx)
	//====================================================================================================
	@Test
	public void testExcludePropertiesUsingAnnotation() throws Exception {
		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer();
		Parser p = getParser();

		E1 e = new E1().init();
		Object r = s.serialize(e);
		e = p.parse(r, E1.class);
		assertObjectEquals("{f1:'f1',f3:'f3'}", e);
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
	// Test @Bean(excludeProperties=xxx) using BeanFilter
	//====================================================================================================
	@Test
	public void testExcludePropertiesUsingBeanFilter() throws Exception {
		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer().clone().addBeanFilters(E2Filter.class);
		Parser p = getParser().clone().addBeanFilters(E2Filter.class);

		E2 e = new E2().init();
		Object r = s.serialize(e);
		e = p.parse(r, E2.class);
		assertObjectEquals("{f1:'f1',f3:'f3'}", e);
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
	public static class E2Filter extends BeanFilterBuilder {
		public E2Filter() {
			super(E2.class);
			setExcludeProperties("f2");
		}
	}

	//====================================================================================================
	// Test @Bean(interfaceClass=xxx)
	//====================================================================================================
	@Test
	public void testInterfaceClassUsingAnnotation() throws Exception {
		// Skip validation-only tests
		if (isValidationOnly())
			return;

		Serializer s = getSerializer();
		Parser p = getParser();

		FA2 t = new FA2().init();
		Object r = s.serialize(t);
		t = p.parse(r, FA2.class);
		assertObjectEquals("{f1:'f1'}", t);
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
	@Test
	public void testInterfaceClassUsingBeanFilter() throws Exception {
		Serializer s;
		Parser p;
		FB2 t;
		Object r;

		// Skip validation-only tests
		if (isValidationOnly())
			return;

		// --- BeanFilter defined on parent class ---
		s = getSerializer().clone().addBeanFilters(FB1Filter.class);
		p = getParser().clone().addBeanFilters(FB1Filter.class);

		t = new FB2().init();
		r = s.serialize(t);
		t = p.parse(r, FB2.class);
		assertObjectEquals("{f1:'f1'}", t);

		// --- BeanFilter defined on child class class ---
		s = getSerializer().clone().addBeanFilters(FB2Filter.class);
		p = getParser().clone().addBeanFilters(FB2Filter.class);

		t = new FB2().init();
		r = s.serialize(t);
		t = p.parse(r, FB2.class);
		assertObjectEquals("{f1:'f1'}", t);

		// --- BeanFilter defined as plain class ---
		s = getSerializer().clone().addBeanFilters(FB1.class);
		p = getParser().clone().addBeanFilters(FB1.class);

		t = new FB2().init();
		r = s.serialize(t);
		t = p.parse(r, FB2.class);
		assertObjectEquals("{f1:'f1'}", t);
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
	public static class FB1Filter extends InterfaceBeanFilterBuilder {
		public FB1Filter() {
			super(FB1.class);
		}
	}
	public static class FB2Filter extends InterfaceBeanFilterBuilder {
		public FB2Filter() {
			super(FB1.class);
		}
	}

	//====================================================================================================
	// testMemberClass
	//====================================================================================================
	@Test
	public void testMemberClass() throws Exception {
		G t = G.create();
		t = roundTrip(t, G.class);
	}

	public static class G {
		public int a1;
		public G1 g1;

		public static G create() {
			G g = new G();
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
	@Test
	public void testMemberClassWithMapClass() throws Exception {
		H t = H.create();
		t = roundTrip(t, H.class);
	}

	public static class H extends LinkedHashMap<String,H.H1> {

		static H create() {
			H h = new H();
			h.add("foo", 1, 2);
			return h;
		}

		H add(String key, int a2, int a3) {
			H1 h1 = new H1();
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
	@Test
	public void testMemberClassWithListClass() throws Exception {
		I t = I.create();
		t = roundTrip(t, I.class);
	}

	public static class I extends LinkedList<I.I1> {

		static I create() {
			I i = new I();
			i.add(1, 2);
			return i;
		}

		I add(int a2, int a3) {
			I1 i1 = new I1();
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
	@Test
	public void testMemberClassWithStringConstructor() throws Exception {
		J t = J.create();
		t = roundTrip(t, J.class);
	}

	public static class J {
		public J2 j2;

		static J create() {
			J j = new J();
			j.init();
			return j;
		}

		private void init() {
			j2 = new J2("2");
		}

		public class J2 {
			int a2;

			public J2(String arg) {
				this.a2 = Integer.parseInt(arg);
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
	@Test
	public void testBeanPropertyPrecedence() throws Exception {
		K t = K.create();
		t = roundTrip(t, K.class);
	}
	public static enum KEnum { FOO, BAR, BAZ }

	public static class K {
		private KEnum a, b, c;

		static K create() {
			K t = new K();
			t.a = KEnum.FOO;
			t.b = KEnum.BAR;
			t.c = KEnum.BAZ;
			return t;
		}

		@BeanIgnore
		public KEnum getA() {
			return KEnum.FOO;
		}

		@BeanProperty(name="a")
		public String getA2() {
			return a.toString();
		}

		// This method should not be interpreted as the setter for this
		// property because it doesn't match the getter return type above.
		public void setA(KEnum a) {
			throw new RuntimeException("Should not be called!");
		}

		public void setA(String a) {
			this.a = KEnum.valueOf(a);
		}

		public KEnum getB() {
			return b;
		}

		public void setB(String b) {
			throw new RuntimeException("Should not be called!");
		}

		public void setB(Object b) {
			throw new RuntimeException("Should not be called!");
		}

		public void setB(KEnum b) {
			this.b = b;
		}

		public KEnum getC() {
			return c;
		}

		public void setC(KEnum c) {
			this.c = c;
		}

		public void setC(String c) {
			throw new RuntimeException("Should not be called!");
		}

		public void setC(Object c) {
			throw new RuntimeException("Should not be called!");
		}
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnBean
	//====================================================================================================
	@Test
	public void testWrapperAttrAnnotationOnBean() throws Exception {
		L t = L.create();
		t = roundTrip(t, L.class);

		Map<String,L> m = new LinkedHashMap<String,L>();
		m.put("bar", L.create());
		roundTripMap(m, LinkedHashMap.class, String.class, L.class);
	}

	@Json(wrapperAttr="foo")
	public static class L {
		public int f1;

		static L create() {
			L l = new L();
			l.f1 = 1;
			return l;
		}
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnNonBean
	//====================================================================================================
	@Test
	public void testWrapperAttrAnnotationOnNonBean() throws Exception {
		M t = M.create();
		t = roundTrip(t, M.class);

		Map<String,M> m = new LinkedHashMap<String,M>();
		m.put("bar", M.create());
		roundTripMap(m, LinkedHashMap.class, String.class, M.class);
	}

	@Json(wrapperAttr="foo")
	public static class M {
		int f1;

		static M create() {
			M m = new M();
			m.f1 = 1;
			return m;
		}

		@Override /* Object */
		public String toString() {
			return String.valueOf(f1);
		}

		public static M valueOf(String s) {
			M m = new M();
			m.f1 = Integer.parseInt(s);
			return m;
		}
	}

	//====================================================================================================
	// Test parsing into top-level non-static inner classes with outer context.
	//====================================================================================================
	@Test
	public void testParsingIntoTopLevelNonStaticInnerClasses() throws Exception {
		N n = new N(1);

		if (returnOriginalObject)
			return;

		Serializer s = getSerializer();
		Parser p = getParser();

		Object r = s.serialize(n.n2);
		n = new N(2);
		ParserSession session = p.createSession(r, null, null, n, null, null);

		N.N2 n2 = p.parse(session, BeanContext.DEFAULT.getClassMeta(N.N2.class));

		// The inner N2.f1 field should be the value of the outer object passed in through the context.
		assertEquals(2, n2.f1);
	}

	public static class N {
		public int f1;
		public N2 n2;

		public N(int f1) {
			this.f1 = f1;
			n2 = new N2();
		}
		public class N2 {
			private int f1 = N.this.f1;
			public int dummy = 1;
		}
	}

}
