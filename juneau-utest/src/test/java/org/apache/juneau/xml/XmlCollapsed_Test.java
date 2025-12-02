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
package org.apache.juneau.xml;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class XmlCollapsed_Test extends TestBase {

	//====================================================================================================
	// testBasic - @Xml.format=COLLAPSED
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new A();

		t.f1 = l("f1a","f1b");
		t.f2 = a("f2a","f2b");
		t.f3 = l("f3a","f3b");
		t.f4 = a("f4a","f4b");

		var xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><f1>f1b</f1><f2>f2a</f2><f2>f2b</f2><xf3>f3a</xf3><xf3>f3b</xf3><xf4>f4a</xf4><xf4>f4b</xf4></object>", xml);
		t = p.parse(xml, A.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f4a", t.f4[0]);

		validateXml(t, s);
	}

	public static class A {

		@Xml(format=COLLAPSED)
		public List<String> f1 = new LinkedList<>();

		@Xml(format=COLLAPSED)
		public String[] f2 = {};

		@Xml(format=COLLAPSED,childName="xf3")
		public List<String> f3 = new LinkedList<>();

		@Xml(format=COLLAPSED,childName="xf4")
		public String[] f4 =  {};
	}

	//====================================================================================================
	// testUninitializedFields - @Xml.format=COLLAPSED, uninitialized fields.
	//====================================================================================================
	@Test void a02_uninitializedFields() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new B();

		t.f1 = l("f1a","f1b");
		t.f2 = a("f2a","f2b");
		t.f3 = l("f3a","f3b");
		t.f4 = a("f4a","f4b");

		var xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><f1>f1b</f1><f2>f2a</f2><f2>f2b</f2><xf3>f3a</xf3><xf3>f3b</xf3><xf4>f4a</xf4><xf4>f4b</xf4></object>", xml);
		t = p.parse(xml, B.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f4a", t.f4[0]);

		validateXml(t, s);
	}

	public static class B {

		@Xml(format=COLLAPSED)
		public List<String> f1;

		@Xml(format=COLLAPSED)
		public String[] f2;

		@Xml(format=COLLAPSED,childName="xf3")
		public List<String> f3;

		@Xml(format=COLLAPSED,childName="xf4")
		public String[] f4;
	}

	//====================================================================================================
	// testInitializedFields - @Xml.format=COLLAPSED, initialized fields.
	//====================================================================================================
	@Test void a03_initializedFields() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new C();

		t.f1 = list("f1b");
		t.f2 = a("f2b");
		t.f3 = list("f3b");
		t.f4 = a("f4b");

		var xml = s.serialize(t);
		assertEquals("<object><f1>f1b</f1><f2>f2b</f2><xf3>f3b</xf3><xf4>f4b</xf4></object>", xml);

		// Note that existing fields should be reused and appended to.
		t = p.parse(xml, C.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f1b", t.f1.get(1));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f2b", t.f2[1]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f3b", t.f3.get(1));
		assertEquals("f4a", t.f4[0]);
		assertEquals("f4b", t.f4[1]);

		validateXml(t, s);
	}

	public static class C {

		@Xml(format=COLLAPSED)
		public List<String> f1 = l("f1a");

		@Xml(format=COLLAPSED)
		public String[] f2 = {"f2a"};

		@Xml(format=COLLAPSED,childName="xf3")
		public List<String> f3 = l("f3a");

		@Xml(format=COLLAPSED,childName="xf4")
		public String[] f4 = {"f4a"};
	}

	//====================================================================================================
	// testGetters - @Xml.format=COLLAPSED, getters.
	//====================================================================================================
	@Test void a04_getters() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new D();

		t.f1 = l("f1a");
		t.f2 = a("f2a");
		t.f3 = l("f3a");
		t.f4 = a("f4a");

		var xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><f2>f2a</f2><xf3>f3a</xf3><xf4>f4a</xf4></object>", xml);

		// Note that existing fields should be reused and appended to.
		t = p.parse(xml, D.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f4a", t.f4[0]);

		validateXml(t, s);
	}

	@Bean(properties="f1,f2,f3,f4")
	public static class D {
		private List<String> f1 = new LinkedList<>();
		@Xml(format=COLLAPSED) public List<String> getF1() { return f1; }

		private String[] f2;
		@Xml(format=COLLAPSED) public String[] getF2() { return f2;}
		public void setF2(String[] v) { f2 = v; }

		private List<String> f3 = new LinkedList<>();
		@Xml(format=COLLAPSED,childName="xf3") public List<String> getF3() { return f3; }

		private String[] f4;
		@Xml(format=COLLAPSED,childName="xf4") public String[] getF4() { return f4; }
		public void setF4(String[] v) { f4 = v; }
	}

	//====================================================================================================
	// testNullConstructibleCollectionFields - @Xml.format=COLLAPSED, null constructible collection fields.
	//====================================================================================================
	@Test void a05_nullConstructibleCollectionFields() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new E();

		t.f1 = (ArrayList<String>)list("f1a");
		t.f2 = (ArrayList<String>)list("f2a");

		var xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><xf2>f2a</xf2></object>", xml);

		// Note that existing fields should be reused and appended to.
		t = p.parse(xml, E.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2.get(0));

		validateXml(t, s);
	}

	@Bean(properties="f1,f2")
	public static class E {
		private ArrayList<String> f1;
		@Xml(format=COLLAPSED) public ArrayList<String> getF1() { return f1; }
		public void setF1(ArrayList<String> v) { f1 = v; }

		private ArrayList<String> f2;
		@Xml(format=COLLAPSED,childName="xf2") public ArrayList<String> getF2() { return f2; }
		public void setF2(ArrayList<String> v) { f2 = v; }
	}

	//====================================================================================================
	// testElementNameOnElementClass - @Xml.format=COLLAPSED, element name defined on element class.
	//====================================================================================================
	@Test void a06_elementNameOnElementClass() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t1 = (Object)FA.newInstance();
		var r = s.serialize(t1);

		assertEquals("<object><xf1>x1</xf1><xf1>x2</xf1></object>", r);
		var t2 = (Object)p.parse(r, FA.class);
		assertEquals(json(t2), json(t1));
		validateXml(t1, s);

		t1 = FB.newInstance();
		r = s.serialize(t1);
		assertEquals("<object><xf1>x1</xf1><xf1>x2</xf1></object>", r);
		t2 = p.parse(r, FB.class);
		assertEquals(json(t2), json(t1));
		validateXml(t1, s);
	}

	public static class FA {
		@Xml(format=COLLAPSED) public List<F1> f1;

		public static FA newInstance() {
			var t = new FA();
			t.f1 = new LinkedList<>();
			t.f1.add(F1.newInstance("x1"));
			t.f1.add(F1.newInstance("x2"));
			return t;
		}
	}

	public static class FB {
		@Xml(format=COLLAPSED) public F1[] f1;

		public static FB newInstance() {
			var t = new FB();
			t.f1 = a(
				F1.newInstance("x1"),
				F1.newInstance("x2")
			);
			return t;
		}
	}

	@Bean(typeName="xf1")
	public static class F1 {
		@Xml(format=TEXT) public String text;

		public static F1 newInstance(String text) {
			var t = new F1();
			t.text = text;
			return t;
		}
	}

	//====================================================================================================
	// testElementNameOnElementClassOverridden - @Xml.format=COLLAPSED, element name defined on element class,
	//	but overridden by @Xml.childName on property.
	//====================================================================================================
	@Test void a07_elementNameOnElementClassOverridden() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = G.newInstance();

		var xml = s.serialize(t);
		assertEquals("<object><yf1>x1</yf1><yf1>x2</yf1></object>", xml);

		// Note that existing fields should be reused and appended to.
		var t2 = p.parse(xml, G.class);
		assertEquals(json(t2), json(t));

		validateXml(t, s);
	}

	public static class G {
		@Xml(format=COLLAPSED, childName="yf1") public List<F1> f1;

		public static G newInstance() {
			var t = new G();
			t.f1 = new LinkedList<>();
			t.f1.add(F1.newInstance("x1"));
			t.f1.add(F1.newInstance("x2"));
			return t;
		}
	}

	//====================================================================================================
	// testElementNameOnCollectionClass - @Xml.format=COLLAPSED, element name defined on bean class.
	//====================================================================================================
	@Test void a08_elementNameOnCollectionClass() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = H.newInstance();

		var xml = s.serialize(t);
		assertEquals("<object><xf1>x1</xf1><xf1>x2</xf1></object>", xml);

		// Note that existing fields should be reused and appended to.
		var t2 = p.parse(xml, H.class);
		assertEquals(json(t2), json(t));

		validateXml(t, s);
	}

	public static class H {
		@Xml(format=COLLAPSED) public H1 f1;

		public static H newInstance() {
			var t = new H();
			t.f1 = new H1();
			t.f1.add("x1");
			t.f1.add("x2");
			return t;
		}
	}

	@Xml(childName="xf1")
	public static class H1 extends LinkedList<String> {
	}

	//====================================================================================================
	// testElementNameOnCollectionClassOverridden - @Xml.format=COLLAPSED, element name defined on element class,
	//	but overridden by @Xml.childName on property.
	//====================================================================================================
	@Test void a09_elementNameOnCollectionClassOverridden() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = G.newInstance();

		var xml = s.serialize(t);
		assertEquals("<object><yf1>x1</yf1><yf1>x2</yf1></object>", xml);

		// Note that existing fields should be reused and appended to.
		var t2 = p.parse(xml, G.class);
		assertEquals(json(t2), json(t));

		validateXml(t, s);
	}

	public static class I {
		@Xml(format=COLLAPSED, childName="yf1") public H1 f1;

		public static I newInstance() {
			var t = new I();
			t.f1 = new H1();
			t.f1.add("x1");
			t.f1.add("x2");
			return t;
		}
	}
}