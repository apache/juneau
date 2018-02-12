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
package org.apache.juneau.jena;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.jena.RdfCommon.*;
import static org.junit.Assert.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@SuppressWarnings({"serial"})
public class CommonTest {

	private RdfSerializerBuilder getBasicSerializer() {
		return RdfSerializer.create()
			.sq()
			.useWhitespace(false)
			.set(RDF_rdfxml_allowBadUris, true)
			.set(RDF_rdfxml_showDoctypeDeclaration, false)
			.set(RDF_rdfxml_showXmlDeclaration, false);
	}

	private String strip(String s) {
		return s.replaceFirst("<rdf:RDF[^>]+>\\s*", "").replaceAll("</rdf:RDF>$", "").trim().replaceAll("[\\r\\n]", "");
	}

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test
	public void testTrimNullsFromBeans() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		A t1 = A.create(), t2;

		s.trimNullProperties(false);
		String r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:s1 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><jp:s2>s2</jp:s2></rdf:Description>", strip(r));
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);

		s.trimNullProperties(true);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:s2>s2</jp:s2></rdf:Description>", strip(r));
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);
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
	@Test
	public void testTrimEmptyMaps() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		B t1 = B.create(), t2;
		String r;

		s.trimEmptyMaps(false);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f1 rdf:parseType='Resource'></jp:f1><jp:f2 rdf:parseType='Resource'><jp:f2a rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><jp:f2b rdf:parseType='Resource'><jp:s2>s2</jp:s2></jp:f2b></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, B.class);
		assertEqualObjects(t1, t2);

		s.trimEmptyMaps(true);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f2 rdf:parseType='Resource'><jp:f2a rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><jp:f2b rdf:parseType='Resource'><jp:s2>s2</jp:s2></jp:f2b></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, B.class);
		assertNull(t2.f1);

		s.trimEmptyMaps();
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f2 rdf:parseType='Resource'><jp:f2a rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><jp:f2b rdf:parseType='Resource'><jp:s2>s2</jp:s2></jp:f2b></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, B.class);
		assertNull(t2.f1);
	}

	public static class B {
		public TreeMap<String,A> f1, f2;

		public static B create() {
			B t = new B();
			t.f1 = new TreeMap<String,A>();
			t.f2 = new TreeMap<String,A>(){{put("f2a",null);put("f2b",A.create());}};
			return t;
		}
	}

	//====================================================================================================
	// Trim empty lists
	//====================================================================================================
	@Test
	public void testTrimEmptyLists() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		C t1 = C.create(), t2;
		String r;

		s.trimEmptyCollections(false);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f1><rdf:Seq/></jp:f1><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, C.class);
		assertEqualObjects(t1, t2);

		s.trimEmptyCollections(true);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
		t2 = p.parse(r, C.class);

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
		t2 = p.parse(r, C.class);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			C t = new C();
			t.f1 = new AList<A>();
			t.f2 = new AList<A>().append(null).append(A.create());
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test
	public void testTrimEmptyArrays() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		D t1 = D.create(), t2;
		String r;

		s.trimEmptyCollections(false);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f1><rdf:Seq/></jp:f1><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, D.class);
		assertEqualObjects(t1, t2);

		s.trimEmptyCollections(true);
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, D.class);
		assertNull(t2.f1);

		s.trimEmptyCollections();
		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
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
	// @BeanProperty.properties annotation.
	//====================================================================================================
	@Test
	public void testBeanPropertyProperties() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		E1 t1 = E1.create(), t2;
		String r;

		r = s.build().serialize(t1);
		assertEquals("<rdf:Description><jp:x1 rdf:parseType='Resource'><jp:f1>1</jp:f1></jp:x1><jp:x2 rdf:parseType='Resource'><jp:f1>1</jp:f1></jp:x2><jp:x3><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x3><jp:x4><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x4><jp:x5><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x5><jp:x6><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x6></rdf:Description>", strip(r));
		t2 = p.parse(r, E1.class);
		assertEqualObjects(t1, t2);
	}

	public static class E1 {
		@BeanProperty(properties="f1") public E2 x1;
		@BeanProperty(properties="f1") public Map<String,Integer> x2;
		@BeanProperty(properties="f1") public E2[] x3;
		@BeanProperty(properties="f1") public List<E2> x4;
		@BeanProperty(properties="f1") public ObjectMap[] x5;
		@BeanProperty(properties="f1") public List<ObjectMap> x6;

		public static E1 create() {
			E1 t = new E1();
			t.x1 = new E2();
			t.x2 = new AMap<String,Integer>().append("f1",1).append("f2",2);
			t.x3 = new E2[] {new E2()};
			t.x4 = new AList<E2>().append(new E2());
			t.x5 = new ObjectMap[] {new ObjectMap().append("f1","1").append("f2","2")};
			t.x6 = new AList<ObjectMap>().append(new ObjectMap().append("f1","1").append("f2","2"));
			return t;
		}
	}

	public static class E2 {
		public int f1 = 1;
		public int f2 = 2;
	}

	//====================================================================================================
	// @BeanProperty.properties annotation on list of beans.
	//====================================================================================================
	@Test
	public void testBeanPropertyProperiesOnListOfBeans() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		List<F> l1 = new LinkedList<F>(), l2;
		F t = F.create();
		t.x1.add(F.create());
		l1.add(t);

		String r = s.build().serialize(l1);
		assertEquals("<rdf:Seq><rdf:li rdf:parseType='Resource'><jp:x1><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:x2>2</jp:x2></rdf:li></rdf:Seq></jp:x1><jp:x2>2</jp:x2></rdf:li></rdf:Seq>", strip(r));
		l2 = p.parse(r, LinkedList.class, F.class);
		assertEqualObjects(l1, l2);
	}

	public static class F {
		@BeanProperty(properties="x2") public List<F> x1;
		public int x2;

		public static F create() {
			F t = new F();
			t.x1 = new LinkedList<F>();
			t.x2 = 2;
			return t;
		}
	}

	//====================================================================================================
	// Test URIAttr - Test that URLs and URIs are serialized and parsed correctly.
	//====================================================================================================
	@Test
	public void testURIAttr() throws Exception {
		RdfSerializerBuilder s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;

		G t = new G();
		t.uri = new URI("http://uri");
		t.f1 = new URI("http://f1");
		t.f2 = new URL("http://f2");

		String xml = s.build().serialize(t);
		t = p.parse(xml, G.class);
		assertEquals("http://uri", t.uri.toString());
		assertEquals("http://f1", t.f1.toString());
		assertEquals("http://f2", t.f2.toString());
	}

	public static class G {
		@Rdf(beanUri=true) public URI uri;
		public URI f1;
		public URL f2;
	}


	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test
	public void testRecursion() throws Exception {
		RdfSerializerBuilder s = RdfSerializer.create().xmlabbrev().sq();

		R1 r1 = new R1();
		R2 r2 = new R2();
		R3 r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		try {
			s.build().serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("It's recommended you use the Serializer.SERIALIZER_detectRecursions setting to help locate the loop."));
		}

		// Recursion detection, no ignore
		s.detectRecursions();
		try {
			s.build().serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("[0]root:org.apache.juneau.jena.CommonTest$R1"));
			assertTrue(msg.contains("->[1]r2:org.apache.juneau.jena.CommonTest$R2"));
			assertTrue(msg.contains("->[2]r3:org.apache.juneau.jena.CommonTest$R3"));
			assertTrue(msg.contains("->[3]r1:org.apache.juneau.jena.CommonTest$R1"));
		}

		s.ignoreRecursions();
		String r = s.build().serialize(r1).replace("\r", "");
		// Note...the order of the namespaces is not always the same depending on the JVM.
		// The Jena libraries appear to use a hashmap for these.
		assertTrue(r.contains(
			"<rdf:Description>\n"+
			"<jp:name>foo</jp:name>\n"+
			"<jp:r2 rdf:parseType='Resource'>\n"+
			"<jp:name>bar</jp:name>\n"+
			"<jp:r3 rdf:parseType='Resource'>\n"+
			"<jp:name>baz</jp:name>\n"+
			"</jp:r3>\n"+
			"</jp:r2>\n"+
			"</rdf:Description>\n"+
			"</rdf:RDF>\n"
		));
		assertTrue(r.contains("xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
		assertTrue(r.contains("xmlns:j='http://www.apache.org/juneau/"));
		assertTrue(r.contains("xmlns:jp='http://www.apache.org/juneaubp/"));
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
