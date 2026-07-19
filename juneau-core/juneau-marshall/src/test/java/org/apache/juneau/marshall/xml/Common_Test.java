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

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class Common_Test extends TestBase {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test void a01_trimNullsFromBeans() throws Exception {
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;
		var t1 = A.create();

		var r = s.build().write(t1);
		assertEquals("<object><s2>s2</s2></object>", r);
		var t2 = p.read(r, A.class);
		assertEquals(json(t2), json(t1));

		s.keepNullProperties();
		r = s.build().write(t1);
		assertEquals("<object><s1 _type='null'/><s2>s2</s2></object>", r);
		t2 = p.read(r, A.class);
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
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;
		var t1 = B.create();
		var r = s.build().write(t1);

		assertEquals("<object><f1/><f2><f2a _type='null'/><f2b><s2>s2</s2></f2b></f2></object>", r);
		var t2 = p.read(r, B.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyMaps();
		r = s.build().write(t1);
		assertEquals("<object><f2><f2a _type='null'/><f2b><s2>s2</s2></f2b></f2></object>", r);
		t2 = p.read(r, B.class);
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
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;
		var t1 = C.create();
		var r = s.build().write(t1);

		assertEquals("<object><f1></f1><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		var t2 = p.read(r, C.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().write(t1);
		assertEquals("<object><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		t2 = p.read(r, C.class);
		assertNull(t2.f1);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			var t = new C();
			t.f1 = l();
			t.f2 = l(null,A.create());
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test void a04_trimEmptyArrays() throws Exception {
		var s = XmlSerializer.create().sq();
		var p = XmlParser.DEFAULT;
		var t1 = D.create();
		var r = s.build().write(t1);

		assertEquals("<object><f1></f1><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		var t2 = p.read(r, D.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().write(t1);
		assertEquals("<object><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		t2 = p.read(r, D.class);
		assertNull(t2.f1);
	}

	public static class D {
		public A[] f1, f2;

		public static D create() {
			var t = new D();
			t.f1 = a();
			t.f2 = a(null, A.create());
			return t;
		}
	}

	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test void a07_recursion() throws Exception {
		var s = XmlSerializer.create().maxDepth(Integer.MAX_VALUE);

		var r1 = new R1();
		var r2 = new R2();
		var r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		assertThrowsWithMessage(Exception.class, "It's recommended you use the MarshallingTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.", ()->s.build().write(r1));

		// Recursion detection, no ignore
		s.detectRecursions();
		assertThrowsWithMessage(
			Exception.class,
			l("[0] <noname>:org.apache.juneau.marshall.xml.Common_Test$R1", "->[1] r2:org.apache.juneau.marshall.xml.Common_Test$R2", "->[2] r3:org.apache.juneau.marshall.xml.Common_Test$R3", "->[3] r1:org.apache.juneau.marshall.xml.Common_Test$R1"),
			()->s.build().write(r1)
		);

		s.ignoreRecursions();
		assertEquals("<object><name>foo</name><r2><name>bar</name><r3><name>baz</name></r3></r2></object>", s.build().write(r1));
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
	// @MarshalledProp(format) tests
	//====================================================================================================

	@Test
	void a08_beanpFormat() throws Exception {
		var s = XmlSerializer.create().sq().build();

		var bean = new K();
		var xml = s.write(bean);

		// Verify all formatted fields are serialized correctly
		assertTrue(xml.contains("<doubleField>2.718</doubleField>"));
		assertTrue(xml.contains("<floatField>3.14</floatField>"));
		assertTrue(xml.contains("<floatWithCurrency>$19.99</floatWithCurrency>"));
		assertTrue(xml.contains("<hexField>0xFF00FF</hexField>"));
		assertTrue(xml.contains("<intField>00042</intField>"));
		assertTrue(xml.contains("<longField>0000001234567890</longField>"));
		assertTrue(xml.contains("<name>Test</name>"));
		assertTrue(xml.contains("<privateField>9.88</privateField>"));
		assertTrue(xml.contains("<scientificField>6.022e+23</scientificField>") || xml.contains("<scientificField>6.022E+23</scientificField>"));
	}

	public static class K {
		@MarshalledProp(format="%.2f")
		public float floatField = 3.14159f;

		@MarshalledProp(format="$%.2f")
		public float floatWithCurrency = 19.987f;

		@MarshalledProp(format="%.3f")
		public double doubleField = 2.71828;

		@MarshalledProp(format="%05d")
		public int intField = 42;

		@MarshalledProp(format="%016d")
		public long longField = 1234567890L;

		@MarshalledProp(format="0x%06X")
		public int hexField = 0xFF00FF;

		@MarshalledProp(format="%.3e")
		public double scientificField = 6.02214076e23;

		public String name = "Test";

		@MarshalledProp(format="%.2f")
		private float privateField = 9.876f;

		public float getPrivateField() { return privateField; }
		public void setPrivateField(float f) { privateField = f; }
	}
}