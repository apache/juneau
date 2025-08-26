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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class Common_UrlEncodingTest extends SimpleTestBase {
	UrlEncodingParser p = UrlEncodingParser.DEFAULT;

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test void a01_trimNullsFromBeans() throws Exception {
		var s = UrlEncodingSerializer.create();
		var t1 = A.create();

		var r = s.build().serialize(t1);
		assertEquals("s2=s2", r);
		var t2 = p.parse(r, A.class);
		assertEquals(json(t2), json(t1));

		s.keepNullProperties();
		r = s.build().serialize(t1);
		assertEquals("s1=null&s2=s2", r);
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
		var s = UrlEncodingSerializer.create();
		var t1 = B.create();
		var r = s.build().serialize(t1);

		assertEquals("f1=()&f2=(f2a=null,f2b=(s2=s2))", r);
		var t2 = p.parse(r, B.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyMaps();
		r = s.build().serialize(t1);
		assertEquals("f2=(f2a=null,f2b=(s2=s2))", r);
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
		var s = UrlEncodingSerializer.create();
		var t1 = C.create();
		var r = s.build().serialize(t1);

		assertEquals("f1=@()&f2=@(null,(s2=s2))", r);
		var t2 = p.parse(r, C.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("f2=@(null,(s2=s2))", r);
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			var t = new C();
			t.f1 = list();
			t.f2 = list(null,A.create());
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test void a04_trimEmptyArrays() throws Exception {
		var s = UrlEncodingSerializer.create();
		var t1 = D.create();
		var r = s.build().serialize(t1);

		assertEquals("f1=@()&f2=@(null,(s2=s2))", r);
		var t2 = p.parse(r, D.class);
		assertEquals(json(t2), json(t1));

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("f2=@(null,(s2=s2))", r);
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
	@Test void a05_beanPropertyProperies() throws Exception {
		var s = UrlEncodingSerializer.DEFAULT;
		var ue = s.serialize(new E1());
		assertEquals("x1=(f1=1)&x2=(f1=1)&x3=@((f1=1))&x4=@((f1=1))&x5=@((f1=1))&x6=@((f1=1))", ue);
	}

	public static class E1 {
		@Beanp(properties="f1") public E2 x1 = new E2();
		@Beanp(properties="f1") public Map<String,Integer> x2 = map("f1",1,"f2",2);
		@Beanp(properties="f1") public E2[] x3 = {new E2()};
		@Beanp(properties="f1") public List<E2> x4 = list(new E2());
		@Beanp(properties="f1") public JsonMap[] x5 = {JsonMap.of("f1",1,"f2",2)};
		@Beanp(properties="f1") public List<JsonMap> x6 = list(JsonMap.of("f1",1,"f2",2));
	}

	public static class E2 {
		public int f1 = 1;
		public int f2 = 2;
	}

	//====================================================================================================
	// @Beanp.bpi annotation on list of beans.
	//====================================================================================================
	@Test void a06_beanPropertyPropertiesOnListOfBeans() throws Exception {
		var s = UrlEncodingSerializer.DEFAULT;
		var l = new LinkedList<>();
		var t = new F();
		t.x1.add(new F());
		l.add(t);
		var m = JsonMap.of("t", l);
		var xml = s.serialize(m);
		assertEquals("t=@((x1=@((x2=2)),x2=2))", xml);
		xml = s.serialize(l);
		assertEquals("0=(x1=@((x2=2)),x2=2)", xml);
	}

	public static class F {
		@Beanp(properties="x2") public List<F> x1 = new LinkedList<>();
		public int x2 = 2;
	}

	//====================================================================================================
	// Test URIAttr - Test that URLs and URIs are serialized and parsed correctly.
	//====================================================================================================
	@Test void a07_uRIAttr() throws Exception {
		var s = UrlEncodingSerializer.DEFAULT;
		var p2 = UrlEncodingParser.DEFAULT;

		var t = new G();
		t.uri = new URI("http://uri");
		t.f1 = new URI("http://f1");
		t.f2 = url("http://f2");

		var r = s.serialize(t);
		t = p2.parse(r, G.class);
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
		var s = UrlEncodingSerializer.create().maxDepth(Integer.MAX_VALUE);

		var r1 = new R1();
		var r2 = new R2();
		var r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		assertThrowsWithMessage(Exception.class, "It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.", ()->s.build().serialize(r1));

		// Recursion detection, no ignore
		s.detectRecursions();
		assertThrowsWithMessage(Exception.class, list("[0] root:org.apache.juneau.urlencoding.Common_UrlEncodingTest$R1", "->[1] r2:org.apache.juneau.urlencoding.Common_UrlEncodingTest$R2", "->[2] r3:org.apache.juneau.urlencoding.Common_UrlEncodingTest$R3", "->[3] r1:org.apache.juneau.urlencoding.Common_UrlEncodingTest$R1"), ()->s.build().serialize(r1));

		s.ignoreRecursions();
		assertEquals("name=foo&r2=(name=bar,r3=(name=baz))", s.build().serialize(r1));
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
