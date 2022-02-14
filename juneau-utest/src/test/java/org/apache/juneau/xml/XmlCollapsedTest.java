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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.testutils.XmlUtils;
import org.apache.juneau.xml.annotation.*;
import org.junit.*;

@SuppressWarnings({"serial"})
@FixMethodOrder(NAME_ASCENDING)
public class XmlCollapsedTest {

	//====================================================================================================
	// testBasic - @Xml.format=COLLAPSED
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		A t = new A();

		t.f1 = list("f1a","f1b");
		t.f2 = new String[]{"f2a","f2b"};
		t.f3 = list("f3a","f3b");
		t.f4 = new String[]{"f4a","f4b"};

		String xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><f1>f1b</f1><f2>f2a</f2><f2>f2b</f2><xf3>f3a</xf3><xf3>f3b</xf3><xf4>f4a</xf4><xf4>f4b</xf4></object>", xml);
		t = p.parse(xml, A.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f4a", t.f4[0]);

		XmlUtils.validateXml(t, s);
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
	@Test
	public void testUninitializedFields() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		B t = new B();

		t.f1 = list("f1a","f1b");
		t.f2 = new String[]{"f2a","f2b"};
		t.f3 = list("f3a","f3b");
		t.f4 = new String[]{"f4a","f4b"};

		String xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><f1>f1b</f1><f2>f2a</f2><f2>f2b</f2><xf3>f3a</xf3><xf3>f3b</xf3><xf4>f4a</xf4><xf4>f4b</xf4></object>", xml);
		t = p.parse(xml, B.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f4a", t.f4[0]);

		XmlUtils.validateXml(t, s);
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
	@Test
	public void testInitializedFields() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		C t = new C();

		t.f1 = list("f1b");
		t.f2 = new String[]{"f2b"};
		t.f3 = list("f3b");
		t.f4 = new String[]{"f4b"};

		String xml = s.serialize(t);
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

		XmlUtils.validateXml(t, s);
	}

	public static class C {

		@Xml(format=COLLAPSED)
		public List<String> f1 = list("f1a");

		@Xml(format=COLLAPSED)
		public String[] f2 = {"f2a"};

		@Xml(format=COLLAPSED,childName="xf3")
		public List<String> f3 = list("f3a");

		@Xml(format=COLLAPSED,childName="xf4")
		public String[] f4 = {"f4a"};
	}

	//====================================================================================================
	// testGetters - @Xml.format=COLLAPSED, getters.
	//====================================================================================================
	@Test
	public void testGetters() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		D t = new D();

		t.f1 = list("f1a");
		t.f2 = new String[]{"f2a"};
		t.f3 = list("f3a");
		t.f4 = new String[]{"f4a"};

		String xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><f2>f2a</f2><xf3>f3a</xf3><xf4>f4a</xf4></object>", xml);

		// Note that existing fields should be reused and appended to.
		t = p.parse(xml, D.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2[0]);
		assertEquals("f3a", t.f3.get(0));
		assertEquals("f4a", t.f4[0]);

		XmlUtils.validateXml(t, s);
	}

	@Bean(properties="f1,f2,f3,f4")
	public static class D {

		private List<String> f1 = new LinkedList<>(), f3 = new LinkedList<>();
		private String[] f2, f4;

		@Xml(format=COLLAPSED)
		public List<String> getF1() {
			return f1;
		}

		@Xml(format=COLLAPSED)
		public String[] getF2() {
			return f2;
		}
		public void setF2(String[] f2) {
			this.f2 = f2;
		}

		@Xml(format=COLLAPSED,childName="xf3")
		public List<String> getF3() {
			return f3;
		}

		@Xml(format=COLLAPSED,childName="xf4")
		public String[] getF4() {
			return f4;
		}
		public void setF4(String[] f4) {
			this.f4 = f4;
		}
	}

	//====================================================================================================
	// testNullConstructibleCollectionFields - @Xml.format=COLLAPSED, null constructible collection fields.
	//====================================================================================================
	@Test
	public void testNullConstructibleCollectionFields() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		E t = new E();

		t.f1 = list("f1a");
		t.f2 = list("f2a");

		String xml = s.serialize(t);
		assertEquals("<object><f1>f1a</f1><xf2>f2a</xf2></object>", xml);

		// Note that existing fields should be reused and appended to.
		t = p.parse(xml, E.class);
		assertEquals("f1a", t.f1.get(0));
		assertEquals("f2a", t.f2.get(0));

		XmlUtils.validateXml(t, s);
	}

	@Bean(properties="f1,f2")
	public static class E {

		private ArrayList<String> f1, f2;

		@Xml(format=COLLAPSED)
		public ArrayList<String> getF1() {
			return f1;
		}
		public void setF1(ArrayList<String> f1) {
			this.f1 = f1;
		}

		@Xml(format=COLLAPSED,childName="xf2")
		public ArrayList<String> getF2() {
			return f2;
		}
		public void setF2(ArrayList<String> f2) {
			this.f2 = f2;
		}
	}


	//====================================================================================================
	// testElementNameOnElementClass - @Xml.format=COLLAPSED, element name defined on element class.
	//====================================================================================================
	@Test
	public void testElementNameOnElementClass() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		Object t1 = FA.newInstance(), t2;
		String r;

		r = s.serialize(t1);
		assertEquals("<object><xf1>x1</xf1><xf1>x2</xf1></object>", r);
		t2 = p.parse(r, FA.class);
		assertObject(t1).isSameJsonAs(t2);
		XmlUtils.validateXml(t1, s);

		t1 = FB.newInstance();
		r = s.serialize(t1);
		assertEquals("<object><xf1>x1</xf1><xf1>x2</xf1></object>", r);
		t2 = p.parse(r, FB.class);
		assertObject(t1).isSameJsonAs(t2);
		XmlUtils.validateXml(t1, s);
	}

	public static class FA {

		@Xml(format=COLLAPSED)
		public List<F1> f1;

		public static FA newInstance() {
			FA t = new FA();
			t.f1 = new LinkedList<>();
			t.f1.add(F1.newInstance("x1"));
			t.f1.add(F1.newInstance("x2"));
			return t;
		}
	}

	public static class FB {
		@Xml(format=COLLAPSED)
		public F1[] f1;

		public static FB newInstance() {
			FB t = new FB();
			t.f1 = new F1[]{
				F1.newInstance("x1"),
				F1.newInstance("x2")
			};
			return t;
		}
	}

	@Bean(typeName="xf1")
	public static class F1 {

		@Xml(format=TEXT)
		public String text;

		public static F1 newInstance(String text) {
			F1 t = new F1();
			t.text = text;
			return t;
		}
	}


	//====================================================================================================
	// testElementNameOnElementClassOverridden - @Xml.format=COLLAPSED, element name defined on element class,
	//	but overridden by @Xml.childName on property.
	//====================================================================================================
	@Test
	public void testElementNameOnElementClassOverridden() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		G t = G.newInstance(), t2;

		String xml = s.serialize(t);
		assertEquals("<object><yf1>x1</yf1><yf1>x2</yf1></object>", xml);

		// Note that existing fields should be reused and appended to.
		t2 = p.parse(xml, G.class);
		assertObject(t).isSameJsonAs(t2);

		XmlUtils.validateXml(t, s);
	}

	public static class G {

		@Xml(format=COLLAPSED, childName="yf1")
		public List<F1> f1;

		public static G newInstance() {
			G t = new G();
			t.f1 = new LinkedList<>();
			t.f1.add(F1.newInstance("x1"));
			t.f1.add(F1.newInstance("x2"));
			return t;
		}
	}


	//====================================================================================================
	// testElementNameOnCollectionClass - @Xml.format=COLLAPSED, element name defined on bean class.
	//====================================================================================================
	@Test
	public void testElementNameOnCollectionClass() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		H t = H.newInstance(), t2;

		String xml = s.serialize(t);
		assertEquals("<object><xf1>x1</xf1><xf1>x2</xf1></object>", xml);

		// Note that existing fields should be reused and appended to.
		t2 = p.parse(xml, H.class);
		assertObject(t).isSameJsonAs(t2);

		XmlUtils.validateXml(t, s);
	}

	public static class H {

		@Xml(format=COLLAPSED)
		public H1 f1;

		public static H newInstance() {
			H t = new H();
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
	@Test
	public void testElementNameOnCollectionClassOverridden() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		XmlParser p = XmlParser.DEFAULT;
		G t = G.newInstance(), t2;

		String xml = s.serialize(t);
		assertEquals("<object><yf1>x1</yf1><yf1>x2</yf1></object>", xml);

		// Note that existing fields should be reused and appended to.
		t2 = p.parse(xml, G.class);
		assertObject(t).isSameJsonAs(t2);

		XmlUtils.validateXml(t, s);
	}

	public static class I {

		@Xml(format=COLLAPSED, childName="yf1")
		public H1 f1;

		public static I newInstance() {
			I t = new I();
			t.f1 = new H1();
			t.f1.add("x1");
			t.f1.add("x2");
			return t;
		}
	}
}
