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
import static org.apache.juneau.internal.CollectionUtils.list;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;
import static org.junit.Assert.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class CommonTest extends SimpleTestBase {

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test void testTrimNullsFromBeans() throws Exception {
		XmlSerializer.Builder s = XmlSerializer.create().sq();
		XmlParser p = XmlParser.DEFAULT;
		A t1 = A.create(), t2;

		String r = s.build().serialize(t1);
		assertEquals("<object><s2>s2</s2></object>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t1));

		s.keepNullProperties();
		r = s.build().serialize(t1);
		assertEquals("<object><s1 _type='null'/><s2>s2</s2></object>", r);
		t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t1));
	}

	public static class A {
		public String s1, s2;

		public static A create() {
			A t = new A();
			t.s2 = "s2";
			return t;
		}
	}

	//====================================================================================================
	// Trim empty maps
	//====================================================================================================
	@Test void testTrimEmptyMaps() throws Exception {
		XmlSerializer.Builder s = XmlSerializer.create().sq();
		XmlParser p = XmlParser.DEFAULT;
		B t1 = B.create(), t2;
		String r;

		r = s.build().serialize(t1);
		assertEquals("<object><f1/><f2><f2a _type='null'/><f2b><s2>s2</s2></f2b></f2></object>", r);
		t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyMaps();
		r = s.build().serialize(t1);
		assertEquals("<object><f2><f2a _type='null'/><f2b><s2>s2</s2></f2b></f2></object>", r);
		t2 = p.parse(r, B.class);
		assertNull(t2.f1);
	}

	public static class B {
		public TreeMap<String,A> f1, f2;

		public static B create() {
			B t = new B();
			t.f1 = new TreeMap<>();
			t.f2 = new TreeMap<>(){{put("f2a",null);put("f2b",A.create());}};
			return t;
		}
	}

	//====================================================================================================
	// Trim empty lists
	//====================================================================================================
	@Test void testTrimEmptyLists() throws Exception {
		XmlSerializer.Builder s = XmlSerializer.create().sq();
		XmlParser p = XmlParser.DEFAULT;
		C t1 = C.create(), t2;
		String r;

		r = s.build().serialize(t1);
		assertEquals("<object><f1></f1><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		t2 = p.parse(r, C.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("<object><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			C t = new C();
			t.f1 = list();
			t.f2 = list(null,A.create());
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test void testTrimEmptyArrays() throws Exception {
		XmlSerializer.Builder s = XmlSerializer.create().sq();
		XmlParser p = XmlParser.DEFAULT;
		D t1 = D.create(), t2;
		String r;

		r = s.build().serialize(t1);
		assertEquals("<object><f1></f1><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		t2 = p.parse(r, D.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("<object><f2><null/><object><s2>s2</s2></object></f2></object>", r);
		t2 = p.parse(r, D.class);
		assertNull(t2.f1);
	}

	public static class D {
		public A[] f1, f2;

		public static D create() {
			D t = new D();
			t.f1 = new A[]{};
			t.f2 = new A[]{null, A.create()};
			return t;
		}
	}

	//====================================================================================================
	// @Beanp.bpi annotation.
	//====================================================================================================
	@Test void testBeanPropertyProperties() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		E1 t = new E1();
		String r = s.serialize(t);
		assertEquals(
			"<object>"
				+"<x1 f2='2'><f1>1</f1></x1>"
				+"<x2><f1>1</f1></x2>"
				+"<x3><object f2='2'><f1>1</f1></object></x3>"
				+"<x4><object f2='2'><f1>1</f1></object></x4>"
				+"<x5><object><f1 _type='number'>1</f1></object></x5>"
				+"<x6><object><f1 _type='number'>1</f1></object></x6>"
			+"</object>",
			r);
		validateXml(t);
	}

	public static class E1 {
		@Beanp(properties="f1,f2") public E2 x1 = new E2();
		@Beanp(properties="f1,f2") public Map<String,Integer> x2 = map("f1",1,"f3",3);
		@Beanp(properties="f1,f2") public E2[] x3 = {new E2()};
		@Beanp(properties="f1,f2") public List<E2> x4 = list(new E2());
		@Beanp(properties="f1") public JsonMap[] x5 = {JsonMap.of("f1",1,"f3",3)};
		@Beanp(properties="f1") public List<JsonMap> x6 = list(JsonMap.of("f1",1,"f3",3));
	}

	public static class E2 {
		public int f1 = 1;
		@Xml(format=ATTR) public int f2 = 2;
		public int f3 = 3;
		@Xml(format=ATTR) public int f4 = 4;
	}

	//====================================================================================================
	// @Beanp.bpi annotation on list of beans.
	//====================================================================================================
	@Test void testBeanPropertyPropertiesOnListOfBeans() throws Exception {
		XmlSerializer s = XmlSerializer.DEFAULT_SQ;
		List<Test7b> l = new LinkedList<>();
		Test7b t = new Test7b();
		t.x1.add(new Test7b());
		l.add(t);
		String xml = s.serialize(l);
		assertEquals("<array><object><x1><object><x2>2</x2></object></x1><x2>2</x2></object></array>", xml);
	}

	public static class Test7b {
		@Beanp(properties="x2") public List<Test7b> x1 = new LinkedList<>();
		public int x2 = 2;
	}

	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test void testRecursion() throws Exception {
		XmlSerializer.Builder s = XmlSerializer.create().maxDepth(Integer.MAX_VALUE);

		R1 r1 = new R1();
		R2 r2 = new R2();
		R3 r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		assertThrown(()->s.build().serialize(r1)).asMessage().isContains("It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.");

		// Recursion detection, no ignore
		s.detectRecursions();
		assertThrown(()->s.build().serialize(r1)).asMessage().isContains("[0] <noname>:org.apache.juneau.xml.CommonTest$R1", "->[1] r2:org.apache.juneau.xml.CommonTest$R2", "->[2] r3:org.apache.juneau.xml.CommonTest$R3", "->[3] r1:org.apache.juneau.xml.CommonTest$R1");

		s.ignoreRecursions();
		assertEquals("<object><name>foo</name><r2><name>bar</name><r3><name>baz</name></r3></r2></object>", s.build().serialize(r1));
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
}