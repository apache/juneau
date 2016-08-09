/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.jena;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testbeans.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings({"unchecked","serial","javadoc"})
public class CommonTest {

	private RdfSerializer getBasicSerializer() {
		return new RdfSerializer()
			.setProperty(SERIALIZER_quoteChar, '\'')
			.setProperty(SERIALIZER_useIndentation, false)
			.setProperty(RDF_rdfxml_allowBadUris, true)
			.setProperty(RDF_rdfxml_showDoctypeDeclaration, false)
			.setProperty(RDF_rdfxml_showXmlDeclaration, false);
	}

	private String strip(String s) {
		return s.replaceFirst("<rdf:RDF[^>]+>\\s*", "").replaceAll("</rdf:RDF>$", "").trim().replaceAll("[\\r\\n]", "");
	}

	//====================================================================================================
	// Trim nulls from beans
	//====================================================================================================
	@Test
	public void testTrimNullsFromBeans() throws Exception {
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		A t1 = A.create(), t2;

		s.setProperty(SERIALIZER_trimNullProperties, false);
		String r = s.serialize(t1);
		assertEquals("<rdf:Description><jp:s1 rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><jp:s2>s2</jp:s2></rdf:Description>", strip(r));
		t2 = p.parse(r, A.class);
		assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimNullProperties, true);
		r = s.serialize(t1);
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
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		B t1 = B.create(), t2;
		String r;

		s.setProperty(SERIALIZER_trimEmptyMaps, false);
		r = s.serialize(t1);
		assertEquals("<rdf:Description><jp:f1 rdf:parseType='Resource'></jp:f1><jp:f2 rdf:parseType='Resource'><jp:f2a rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><jp:f2b rdf:parseType='Resource'><jp:s2>s2</jp:s2></jp:f2b></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, B.class);
		assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimEmptyMaps, true);
		r = s.serialize(t1);
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
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		C t1 = C.create(), t2;
		String r;

		s.setProperty(SERIALIZER_trimEmptyLists, false);
		r = s.serialize(t1);
		assertEquals("<rdf:Description><jp:f1><rdf:Seq/></jp:f1><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, C.class);
		assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimEmptyLists, true);
		r = s.serialize(t1);
		assertEquals("<rdf:Description><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, C.class);
		assertNull(t2.f1);
		t2 = p.parse(r, C.class);
	}

	public static class C {
		public List<A> f1, f2;

		public static C create() {
			C t = new C();
			t.f1 = new LinkedList<A>();
			t.f2 = new LinkedList<A>(){{add(null);add(A.create());}};
			return t;
		}
	}

	//====================================================================================================
	// Trim empty arrays
	//====================================================================================================
	@Test
	public void testTrimEmptyArrays() throws Exception {
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		D t1 = D.create(), t2;
		String r;

		s.setProperty(SERIALIZER_trimEmptyLists, false);
		r = s.serialize(t1);
		assertEquals("<rdf:Description><jp:f1><rdf:Seq/></jp:f1><jp:f2><rdf:Seq><rdf:li rdf:resource='http://www.w3.org/1999/02/22-rdf-syntax-ns#nil'/><rdf:li rdf:parseType='Resource'><jp:s2>s2</jp:s2></rdf:li></rdf:Seq></jp:f2></rdf:Description>", strip(r));
		t2 = p.parse(r, D.class);
		assertEqualObjects(t1, t2);

		s.setProperty(SERIALIZER_trimEmptyLists, true);
		r = s.serialize(t1);
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
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		E1 t1 = E1.create(), t2;
		String r;

		r = s.serialize(t1);
		assertEquals("<rdf:Description><jp:x1 rdf:parseType='Resource'><jp:f1>1</jp:f1></jp:x1><jp:x2 rdf:parseType='Resource'><jp:f1>1</jp:f1></jp:x2><jp:x3><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x3><jp:x4><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x4><jp:x5><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x5><jp:x6><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:f1>1</jp:f1></rdf:li></rdf:Seq></jp:x6></rdf:Description>", strip(r));
		t2 = p.parse(r, E1.class);
		assertEqualObjects(t1, t2);
	}

	public static class E1 {
		@BeanProperty(properties={"f1"}) public E2 x1;
		@BeanProperty(properties={"f1"}) public Map<String,Integer> x2;
		@BeanProperty(properties={"f1"}) public E2[] x3;
		@BeanProperty(properties={"f1"}) public List<E2> x4;
		@BeanProperty(properties={"f1"}) public ObjectMap[] x5;
		@BeanProperty(properties={"f1"}) public List<ObjectMap> x6;

		public static E1 create() {
			E1 t = new E1();
			t.x1 = new E2();
			t.x2 = new LinkedHashMap<String,Integer>() {{ put("f1",1); put("f2",2); }};
			t.x3 = new E2[] {new E2()};
			t.x4 = new LinkedList<E2>() {{ add(new E2()); }};
			t.x5 = new ObjectMap[] {new ObjectMap().append("f1","1").append("f2","2")};
			t.x6 = new LinkedList<ObjectMap>() {{ add(new ObjectMap().append("f1","1").append("f2","2")); }};
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
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;
		List<F> l1 = new LinkedList<F>(), l2;
		F t = F.create();
		t.x1.add(F.create());
		l1.add(t);

		String r = s.serialize(l1);
		assertEquals("<rdf:Seq><rdf:li rdf:parseType='Resource'><jp:x1><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:x2>2</jp:x2></rdf:li></rdf:Seq></jp:x1><jp:x2>2</jp:x2></rdf:li></rdf:Seq>", strip(r));
		ClassMeta<LinkedList<F>> cm = p.getBeanContext().getCollectionClassMeta(LinkedList.class, F.class);
		l2 = p.parse(r, cm);
		assertEqualObjects(l1, l2);
	}

	public static class F {
		@BeanProperty(properties={"x2"}) public List<F> x1;
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
		RdfSerializer s = getBasicSerializer();
		RdfParser p = RdfParser.DEFAULT_XML;

		G t = new G();
		t.uri = new URI("http://uri");
		t.f1 = new URI("http://f1");
		t.f2 = new URL("http://f2");

		String xml = s.serialize(t);
		t = p.parse(xml, G.class);
		assertEquals("http://uri", t.uri.toString());
		assertEquals("http://f1", t.f1.toString());
		assertEquals("http://f2", t.f2.toString());
	}

	public static class G {
		@BeanProperty(beanUri=true) public URI uri;
		public URI f1;
		public URL f2;
	}

	//====================================================================================================
	// Test URIs with URI_CONTEXT and URI_AUTHORITY
	//====================================================================================================
	@Test
	public void testUris() throws Exception {
		WriterSerializer s = getBasicSerializer();
		TestURI t = new TestURI();
		String r;
		String expected = "";

		s.setProperty(SERIALIZER_relativeUriBase, null);
		r = stripAndSort(s.serialize(t));
		expected = ""
			+"</rdf:Description>>"
			+"\n<<rdf:Description rdf:about='f0/x0'>"
			+"\n<jp:f1 rdf:resource='f1/x1'/>"
			+"\n<jp:f2 rdf:resource='/f2/x2'/>"
			+"\n<jp:f3 rdf:resource='http://www.ibm.com/f3/x3'/>"
			+"\n<jp:f4 rdf:resource='f4/x4'/>"
			+"\n<jp:f5 rdf:resource='/f5/x5'/>"
			+"\n<jp:f6 rdf:resource='http://www.ibm.com/f6/x6'/>"
			+"\n<jp:f7 rdf:resource='http://www.ibm.com/f7/x7'/>"
			+"\n<jp:f8 rdf:resource='f8/x8'/>"
			+"\n<jp:f9 rdf:resource='f9/x9'/>"
			+"\n<jp:fa>http://www.ibm.com/fa/xa#MY_LABEL</jp:fa>"
			+"\n<jp:fb>http://www.ibm.com/fb/xb?label=MY_LABEL&amp;foo=bar</jp:fb>"
			+"\n<jp:fc>http://www.ibm.com/fc/xc?foo=bar&amp;label=MY_LABEL</jp:fc>"
			+"\n<jp:fd>http://www.ibm.com/fd/xd?label2=MY_LABEL&amp;foo=bar</jp:fd>"
			+"\n<jp:fe>http://www.ibm.com/fe/xe?foo=bar&amp;label2=MY_LABEL</jp:fe>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "");  // Same as null.
		r = stripAndSort(s.serialize(t));
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "/cr");
		r = stripAndSort(s.serialize(t));
		expected = ""
			+"</rdf:Description>>"
			+"\n<<rdf:Description rdf:about='/cr/f0/x0'>"
			+"\n<jp:f1 rdf:resource='/cr/f1/x1'/>"
			+"\n<jp:f2 rdf:resource='/f2/x2'/>"
			+"\n<jp:f3 rdf:resource='http://www.ibm.com/f3/x3'/>"
			+"\n<jp:f4 rdf:resource='/cr/f4/x4'/>"
			+"\n<jp:f5 rdf:resource='/f5/x5'/>"
			+"\n<jp:f6 rdf:resource='http://www.ibm.com/f6/x6'/>"
			+"\n<jp:f7 rdf:resource='http://www.ibm.com/f7/x7'/>"
			+"\n<jp:f8 rdf:resource='/cr/f8/x8'/>"
			+"\n<jp:f9 rdf:resource='/cr/f9/x9'/>"
			+"\n<jp:fa>http://www.ibm.com/fa/xa#MY_LABEL</jp:fa>"
			+"\n<jp:fb>http://www.ibm.com/fb/xb?label=MY_LABEL&amp;foo=bar</jp:fb>"
			+"\n<jp:fc>http://www.ibm.com/fc/xc?foo=bar&amp;label=MY_LABEL</jp:fc>"
			+"\n<jp:fd>http://www.ibm.com/fd/xd?label2=MY_LABEL&amp;foo=bar</jp:fd>"
			+"\n<jp:fe>http://www.ibm.com/fe/xe?foo=bar&amp;label2=MY_LABEL</jp:fe>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "/cr/");  // Same as above
		r = stripAndSort(s.serialize(t));
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, "/");
		r = stripAndSort(s.serialize(t));
		expected = ""
			+"</rdf:Description>>"
			+"\n<<rdf:Description rdf:about='/f0/x0'>"
			+"\n<jp:f1 rdf:resource='/f1/x1'/>"
			+"\n<jp:f2 rdf:resource='/f2/x2'/>"
			+"\n<jp:f3 rdf:resource='http://www.ibm.com/f3/x3'/>"
			+"\n<jp:f4 rdf:resource='/f4/x4'/>"
			+"\n<jp:f5 rdf:resource='/f5/x5'/>"
			+"\n<jp:f6 rdf:resource='http://www.ibm.com/f6/x6'/>"
			+"\n<jp:f7 rdf:resource='http://www.ibm.com/f7/x7'/>"
			+"\n<jp:f8 rdf:resource='/f8/x8'/>"
			+"\n<jp:f9 rdf:resource='/f9/x9'/>"
			+"\n<jp:fa>http://www.ibm.com/fa/xa#MY_LABEL</jp:fa>"
			+"\n<jp:fb>http://www.ibm.com/fb/xb?label=MY_LABEL&amp;foo=bar</jp:fb>"
			+"\n<jp:fc>http://www.ibm.com/fc/xc?foo=bar&amp;label=MY_LABEL</jp:fc>"
			+"\n<jp:fd>http://www.ibm.com/fd/xd?label2=MY_LABEL&amp;foo=bar</jp:fd>"
			+"\n<jp:fe>http://www.ibm.com/fe/xe?foo=bar&amp;label2=MY_LABEL</jp:fe>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_relativeUriBase, null);

		s.setProperty(SERIALIZER_absolutePathUriBase, "http://foo");
		r = stripAndSort(s.serialize(t));
		expected = ""
			+"</rdf:Description>>"
			+"\n<<rdf:Description rdf:about='f0/x0'>"
			+"\n<jp:f1 rdf:resource='f1/x1'/>"
			+"\n<jp:f2 rdf:resource='http://foo/f2/x2'/>"
			+"\n<jp:f3 rdf:resource='http://www.ibm.com/f3/x3'/>"
			+"\n<jp:f4 rdf:resource='f4/x4'/>"
			+"\n<jp:f5 rdf:resource='http://foo/f5/x5'/>"
			+"\n<jp:f6 rdf:resource='http://www.ibm.com/f6/x6'/>"
			+"\n<jp:f7 rdf:resource='http://www.ibm.com/f7/x7'/>"
			+"\n<jp:f8 rdf:resource='f8/x8'/>"
			+"\n<jp:f9 rdf:resource='f9/x9'/>"
			+"\n<jp:fa>http://www.ibm.com/fa/xa#MY_LABEL</jp:fa>"
			+"\n<jp:fb>http://www.ibm.com/fb/xb?label=MY_LABEL&amp;foo=bar</jp:fb>"
			+"\n<jp:fc>http://www.ibm.com/fc/xc?foo=bar&amp;label=MY_LABEL</jp:fc>"
			+"\n<jp:fd>http://www.ibm.com/fd/xd?label2=MY_LABEL&amp;foo=bar</jp:fd>"
			+"\n<jp:fe>http://www.ibm.com/fe/xe?foo=bar&amp;label2=MY_LABEL</jp:fe>"
		;
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_absolutePathUriBase, "http://foo/");
		r = stripAndSort(s.serialize(t));
		assertEquals(expected, r);

		s.setProperty(SERIALIZER_absolutePathUriBase, "");  // Same as null.
		r = stripAndSort(s.serialize(t));
		expected = ""
			+"</rdf:Description>>"
			+"\n<<rdf:Description rdf:about='f0/x0'>"
			+"\n<jp:f1 rdf:resource='f1/x1'/>"
			+"\n<jp:f2 rdf:resource='/f2/x2'/>"
			+"\n<jp:f3 rdf:resource='http://www.ibm.com/f3/x3'/>"
			+"\n<jp:f4 rdf:resource='f4/x4'/>"
			+"\n<jp:f5 rdf:resource='/f5/x5'/>"
			+"\n<jp:f6 rdf:resource='http://www.ibm.com/f6/x6'/>"
			+"\n<jp:f7 rdf:resource='http://www.ibm.com/f7/x7'/>"
			+"\n<jp:f8 rdf:resource='f8/x8'/>"
			+"\n<jp:f9 rdf:resource='f9/x9'/>"
			+"\n<jp:fa>http://www.ibm.com/fa/xa#MY_LABEL</jp:fa>"
			+"\n<jp:fb>http://www.ibm.com/fb/xb?label=MY_LABEL&amp;foo=bar</jp:fb>"
			+"\n<jp:fc>http://www.ibm.com/fc/xc?foo=bar&amp;label=MY_LABEL</jp:fc>"
			+"\n<jp:fd>http://www.ibm.com/fd/xd?label2=MY_LABEL&amp;foo=bar</jp:fd>"
			+"\n<jp:fe>http://www.ibm.com/fe/xe?foo=bar&amp;label2=MY_LABEL</jp:fe>"
		;
		assertEquals(expected, r);
	}

	private String stripAndSort(String s) {
		s = strip(s);
		Set<String> set = new TreeSet<String>();
		for (String s2 : s.split("><"))
			set.add('<' + s2 + '>');
		return StringUtils.join(set, "\n");
	}

	//====================================================================================================
	// Validate that you cannot update properties on locked serializer.
	//====================================================================================================
	@Test
	public void testLockedSerializer() throws Exception {
		RdfSerializer s = getBasicSerializer().lock();
		try {
			s.setProperty(XmlSerializerContext.XML_enableNamespaces, true);
			fail("Locked exception not thrown");
		} catch (LockedException e) {}
		try {
			s.setProperty(SerializerContext.SERIALIZER_addClassAttrs, true);
			fail("Locked exception not thrown");
		} catch (LockedException e) {}
		try {
			s.setProperty(BeanContext.BEAN_beanMapPutReturnsOldValue, true);
			fail("Locked exception not thrown");
		} catch (LockedException e) {}
	}

	//====================================================================================================
	// Recursion
	//====================================================================================================
	@Test
	public void testRecursion() throws Exception {
		WriterSerializer s = new RdfSerializer.XmlAbbrev().setProperty(SERIALIZER_quoteChar, '\'');

		R1 r1 = new R1();
		R2 r2 = new R2();
		R3 r3 = new R3();
		r1.r2 = r2;
		r2.r3 = r3;
		r3.r1 = r1;

		// No recursion detection
		try {
			s.serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("It's recommended you use the SerializerContext.SERIALIZER_detectRecursions setting to help locate the loop."));
		}

		// Recursion detection, no ignore
		s.setProperty(SERIALIZER_detectRecursions, true);
		try {
			s.serialize(r1);
			fail("Exception expected!");
		} catch (Exception e) {
			String msg = e.getLocalizedMessage();
			assertTrue(msg.contains("[0]root:org.apache.juneau.jena.CommonTest$R1"));
			assertTrue(msg.contains("->[1]r2:org.apache.juneau.jena.CommonTest$R2"));
			assertTrue(msg.contains("->[2]r3:org.apache.juneau.jena.CommonTest$R3"));
			assertTrue(msg.contains("->[3]r1:org.apache.juneau.jena.CommonTest$R1"));
		}

		s.setProperty(SERIALIZER_ignoreRecursions, true);
		String r = s.serialize(r1).replace("\r", "");
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
		assertTrue(r.contains("xmlns:j='http://www.ibm.com/juneau/"));
		assertTrue(r.contains("xmlns:jp='http://www.ibm.com/juneaubp/"));
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
