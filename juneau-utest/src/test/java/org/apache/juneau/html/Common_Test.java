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
package org.apache.juneau.html;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.junit.jupiter.api.*;

class Common_Test extends TestBase {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test void a01_trimNullsFromBeans() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders();
		var p = HtmlParser.DEFAULT;
		var t1 = A.create();

		s.keepNullProperties();
		var r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>s1</td><td><null/></td></tr><tr><td>s2</td><td>s2</td></tr></table>", r);
		var t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t1));

		s = HtmlSerializer.create().sq().addKeyValueTableHeaders();
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>s2</td><td>s2</td></tr></table>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t1));
	}

	public static class A {
		public String s1, s2;

		public static A create() {
			var t = new A();
			t.s2 = "s2";
			return t;
		}
	}

	//====================================================================================================
	// Trim empty maps
	//====================================================================================================
	@Test void a02_trimEmptyMaps() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders();
		var p = HtmlParser.DEFAULT;
		var t1 = B.create();
		var r = s.build().serialize(t1);

		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><table><tr><th>key</th><th>value</th></tr></table></td></tr><tr><td>f2</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>f2a</td><td><null/></td></tr><tr><td>f2b</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>s2</td><td>s2</td></tr></table></td></tr></table></td></tr></table>", r);
		var t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyMaps();
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f2</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>f2a</td><td><null/></td></tr><tr><td>f2b</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>s2</td><td>s2</td></tr></table></td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, B.class);
		assertNull(t2.f1);
	}

	public static class B {
		public TreeMap<String,A> f1, f2;

		public static B create() {
			var t = new B();
			t.f1 = new TreeMap<>();
			t.f2 = new TreeMap<>(){{put("f2a",null);put("f2b",A.create());}};
			return t;
		}
	}

	//====================================================================================================
	// Trim empty lists
	//====================================================================================================
	@Test void a03_trimEmptyLists() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders();
		var p = HtmlParser.DEFAULT;
		var t1 = C.create();
		var r = s.build().serialize(t1);

		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f1</td><td><ul></ul></td></tr><tr><td>f2</td><td><table _type='array'><tr><th>s1</th><th>s2</th></tr><tr><null/></tr><tr><td><null/></td><td>s2</td></tr></table></td></tr></table>", r);
		var t2 = p.parse(r, C.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("<table><tr><th>key</th><th>value</th></tr><tr><td>f2</td><td><table _type='array'><tr><th>s1</th><th>s2</th></tr><tr><null/></tr><tr><td><null/></td><td>s2</td></tr></table></td></tr></table>", r);
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			var t = new C();
			t.f1 = alist();
			t.f2 = alist(null,A.create());
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test void a04_trimEmptyArrays() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders();
		var p = HtmlParser.DEFAULT;
		var t1 = D.create();
		var r = s.build().serialize(t1);

		assertEquals(
			"<table>"
				+"<tr><th>key</th><th>value</th></tr>"
				+"<tr>"
					+"<td>f1</td>"
					+"<td><ul></ul></td>"
				+"</tr>"
				+"<tr>"
					+"<td>f2</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>s1</th><th>s2</th></tr>"
							+"<tr><null/></tr>"
							+"<tr><td><null/></td><td>s2</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
			+"</table>",
			r);

		var t2 = p.parse(r, D.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals(
			"<table>"
				+"<tr><th>key</th><th>value</th></tr>"
				+"<tr>"
					+"<td>f2</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>s1</th><th>s2</th></tr>"
							+"<tr><null/></tr>"
							+"<tr><td><null/></td><td>s2</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
			+"</table>",
			r);
		t2 = p.parse(r, D.class);
		assertNull(t2.f1);
	}

	public static class D {
		public A[] f1, f2;

		public static D create() {
			var t = new D();
			t.f1 = new A[]{};
			t.f2 = new A[]{null, A.create()};
			return t;
		}
	}

	//====================================================================================================
	// @Beanp.bpi annotation.
	//====================================================================================================
	@Test void a05_beanPropertyProperties() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().build();
		var t = new E1();
		var r = s.serialize(t);

		assertEquals(
			"<table>"
				+"<tr>"
					+"<th>key</th>"
					+"<th>value</th>"
				+"</tr>"
				+"<tr>"
					+"<td>x1</td>"
					+"<td>"
						+"<table>"
							+"<tr><th>key</th><th>value</th></tr>"
							+"<tr><td>f1</td><td>1</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x2</td>"
					+"<td>"
						+"<table>"
							+"<tr><th>key</th><th>value</th></tr>"
							+"<tr><td>f1</td><td>3</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x3</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td>1</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x4</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td>1</td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x5</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td><number>5</number></td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
				+"<tr>"
					+"<td>x6</td>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>f1</th></tr>"
							+"<tr><td><number>7</number></td></tr>"
						+"</table>"
					+"</td>"
				+"</tr>"
			+"</table>",
		r);
		r = s.getSchemaSerializer().serialize(new E1());
		assertEquals(r.indexOf("f2"), -1);
	}

	public static class E1 {
		@Beanp(properties="f1") public E2 x1 = new E2();
		@Beanp(properties="f1") public Map<String,Integer> x2 = map("f1",3,"f2",4);
		@Beanp(properties="f1") public E2[] x3 = {new E2()};
		@Beanp(properties="f1") public List<E2> x4 = alist(new E2());
		@Beanp(properties="f1") public JsonMap[] x5 = {JsonMap.of("f1",5,"f2",6)};
		@Beanp(properties="f1") public List<JsonMap> x6 = alist(JsonMap.of("f1",7,"f2",8));
	}

	public static class E2 {
		public int f1 = 1;
		public int f2 = 2;
	}

	//====================================================================================================
	// @Beanp.bpi annotation on list of beans.
	//====================================================================================================
	@Test void a06_beanPropertyPropertiesOnListOfBeans() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;
		var l = new LinkedList<>();
		var t = new F();
		t.x1.add(new F());
		l.add(t);
		var html = s.serialize(l);
		assertEquals(
			"<table _type='array'>"
				+"<tr><th>x1</th><th>x2</th></tr>"
				+"<tr>"
					+"<td>"
						+"<table _type='array'>"
							+"<tr><th>x2</th></tr>"
							+"<tr><td>2</td></tr>"
						+"</table>"
					+"</td>"
					+"<td>2</td>"
				+"</tr>"
			+"</table>", html);

	}

	public static class F {
		@Beanp(properties="x2") public List<F> x1 = new LinkedList<>();
		public int x2 = 2;
	}

	//====================================================================================================
	// Test that URLs and URIs are serialized and parsed correctly.
	//====================================================================================================
	@Test void a07_uRIAttr() throws Exception {
		var s = HtmlSerializer.DEFAULT_SQ;
		var p = HtmlParser.DEFAULT;

		var t = new G();
		t.uri = new URI("http://uri");
		t.f1 = new URI("http://f1");
		t.f2 = url("http://f2");

		var html = s.serialize(t);
		t = p.parse(html, G.class);
		assertEquals("http://uri", t.uri.toString());
		assertEquals("http://f1", t.f1.toString());
		assertEquals("http://f2", t.f2.toString());
	}

	public static class G {
		public URI uri;
		public URI f1;
		public URL f2;
	}

	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test void a08_recursion() throws Exception {
		var s = HtmlSerializer.create().sq().addKeyValueTableHeaders().maxDepth(Integer.MAX_VALUE);

		var r1 = new R1();
		var r2 = new R2();
		var r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		assertThrowsWithMessage(Exception.class, "It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.", ()->s.build().serialize(r1));
		assertThrowsWithMessage(Exception.class, "It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.", ()->s.build().serialize(r1));

		// Recursion detection, no ignore
		s.detectRecursions();
		assertThrowsWithMessage(Exception.class, Utils.list("[0] <noname>:org.apache.juneau.html.Common_Test$R1", "->[1] r2:org.apache.juneau.html.Common_Test$R2", "->[2] r3:org.apache.juneau.html.Common_Test$R3", "->[3] r1:org.apache.juneau.html.Common_Test$R1"), ()->s.build().serialize(r1));

		s.ignoreRecursions();
		assertEquals(
			"<table><tr><th>key</th><th>value</th></tr><tr><td>name</td><td>foo</td></tr><tr><td>r2</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>name</td><td>bar</td></tr><tr><td>r3</td><td><table><tr><th>key</th><th>value</th></tr><tr><td>name</td><td>baz</td></tr></table></td></tr></table></td></tr></table>",
			s.build().serialize(r1)
		);

		// Make sure this doesn't blow up.
		s.build().getSchemaSerializer().serialize(r1);
	}

	public static class R1 {
		public String name = "foo";
		public R2 r2;
	}
	public static class R2 {
		public String name = "bar";
		public R3 r3;
	}
	public static class R3 {
		public String name = "baz";
		public R1 r1;
	}

	//====================================================================================================
	// Basic bean
	//====================================================================================================
	@Test void a09_basicBean() throws Exception {
		var s = HtmlSerializer.create().sq().keepNullProperties().sortProperties().addKeyValueTableHeaders().build();

		var a = new J();
		a.setF1("J");
		a.setF2(100);
		a.setF3(true);
		assertEquals(
			"<table>"
				+"<tr><th>key</th><th>value</th></tr>"
				+"<tr><td>f1</td><td>J</td></tr>"
				+"<tr><td>f2</td><td>100</td></tr>"
				+"<tr><td>f3</td><td>true</td></tr>"
			+"</table>",
			s.serialize(a));
	}

	public static class J {
		private String f1;
		public String getF1() { return f1; }
		public void setF1(String v) { f1 = v; }

		private int f2 = -1;
		public int getF2() { return f2; }
		public void setF2(int v) { f2 = v; }

		private boolean f3;
		public boolean isF3() { return f3; }
		public void setF3(boolean v) { f3 = v; }

		@Override /* Overridden from Object */
		public String toString() {
			return ("J(f1: " + this.getF1() + ", f2: " + this.getF2() + ")");
		}
	}

	//====================================================================================================
	// @Beanp(format) tests
	//====================================================================================================

	@Test
	void a10_beanpFormat() throws Exception {
		var s = HtmlSerializer.create().sq().sortProperties().addKeyValueTableHeaders().build();

		var bean = new K();
		var html = s.serialize(bean);

		// Verify all formatted fields are serialized correctly
		assertTrue(html.contains("<td>doubleField</td><td>2.718</td>"));
		assertTrue(html.contains("<td>floatField</td><td>3.14</td>"));
		assertTrue(html.contains("<td>floatWithCurrency</td><td>$19.99</td>"));
		assertTrue(html.contains("<td>hexField</td><td>0xFF00FF</td>"));
		assertTrue(html.contains("<td>intField</td><td>00042</td>"));
		assertTrue(html.contains("<td>longField</td><td>0000001234567890</td>"));
		assertTrue(html.contains("<td>name</td><td>Test</td>"));
		assertTrue(html.contains("<td>privateField</td><td>9.88</td>"));
		assertTrue(html.contains("<td>scientificField</td><td>6.022e+23</td>") || html.contains("<td>scientificField</td><td>6.022E+23</td>"));
	}

	public static class K {
		@Beanp(format="%.2f")
		public float floatField = 3.14159f;

		@Beanp(format="$%.2f")
		public float floatWithCurrency = 19.987f;

		@Beanp(format="%.3f")
		public double doubleField = 2.71828;

		@Beanp(format="%05d")
		public int intField = 42;

		@Beanp(format="%016d")
		public long longField = 1234567890L;

		@Beanp(format="0x%06X")
		public int hexField = 0xFF00FF;

		@Beanp(format="%.3e")
		public double scientificField = 6.02214076e23;

		public String name = "Test";

		@Beanp(format="%.2f")
		private float privateField = 9.876f;

		public float getPrivateField() { return privateField; }
		public void setPrivateField(float f) { privateField = f; }
	}
}