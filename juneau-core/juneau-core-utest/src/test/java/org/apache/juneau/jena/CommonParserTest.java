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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.jena.RdfCommon.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","serial"})
@FixMethodOrder(NAME_ASCENDING)
public class CommonParserTest {

	private String wrap(String in) {
		return ""
			+ "<rdf:RDF"
			+ " xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'"
			+ " xmlns:j='http://www.apache.org/juneau/'"
			+ " xmlns:jp='http://www.apache.org/juneaubp/'>"
			+ in
			+ "</rdf:RDF>";
	}

	private String strip(String s) {
		return s.replaceFirst("<rdf:RDF[^>]+>\\s*", "").replaceAll("</rdf:RDF>$", "").trim().replaceAll("[\\r\\n]", "");
	}

	private RdfSerializerBuilder getBasicSerializer() {
		return RdfSerializer.create()
			.sq()
			.addLiteralTypes()
			.set(RDF_rdfxml_allowBadUris, true)
			.set(RDF_rdfxml_showDoctypeDeclaration, false)
			.set(RDF_rdfxml_showXmlDeclaration, false);
	}

	//====================================================================================================
	// testBasicFromSerializer
	//====================================================================================================
	@Test
	public void testFromSerializer() throws Exception {
		WriterSerializer s = getBasicSerializer().build();
		ReaderParser p = RdfParser.create().xml().trimWhitespace().build();
		Map m = null;
		String in;
		Integer one = Integer.valueOf(1);

		in = wrap("<rdf:Description><jp:a rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>1</jp:a></rdf:Description>");
		m = (Map)p.parse(in, Object.class);
		assertEquals(one, m.get("a"));

		in = wrap("<rdf:Description><jp:a rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>1</jp:a><jp:b>foo bar</jp:b><jp:c rdf:datatype='http://www.w3.org/2001/XMLSchema#boolean'>false</jp:c></rdf:Description>");
		m = (Map)p.parse(in, Object.class);
		assertEquals(one, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		in = wrap("<rdf:Description><jp:a rdf:datatype='http://www.w3.org/2001/XMLSchema#int'> 1 </jp:a><jp:b> foo bar </jp:b><jp:c rdf:datatype='http://www.w3.org/2001/XMLSchema#boolean'> false </jp:c></rdf:Description>");
		m = (Map)p.parse(in, Object.class);
		assertEquals(one, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		assertEquals(false, m.get("c"));

		in = wrap("<rdf:Description><jp:x>org.apache.juneau.test.Person</jp:x><jp:addresses><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:x>org.apache.juneau.test.Address</jp:x><jp:city>city A</jp:city><jp:state>state A</jp:state><jp:street>street A</jp:street><jp:zip rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>12345</jp:zip></rdf:li></rdf:Seq></jp:addresses></rdf:Description>");
		m = (Map)p.parse(in, Object.class);
		assertEquals("org.apache.juneau.test.Person", m.get("x"));
		List l = (List)m.get("addresses");
		assertNotNull(l);
		m = (Map)l.get(0);
		assertNotNull(m);
		assertEquals("org.apache.juneau.test.Address", m.get("x"));
		assertEquals("city A", m.get("city"));
		assertEquals("state A", m.get("state"));
		assertEquals("street A", m.get("street"));
		assertEquals(12345, m.get("zip"));

		in = wrap("<rdf:Seq><rdf:li rdf:parseType='Resource'><jp:attribute>value</jp:attribute></rdf:li><rdf:li rdf:parseType='Resource'><jp:attribute>value</jp:attribute></rdf:li></rdf:Seq>");
		OList jl = (OList)p.parse(in, Object.class);
		assertEquals("value", jl.getMap(0).getString("attribute"));
		assertEquals("value", jl.getMap(1).getString("attribute"));

		// Verify that all the following return null.
		assertNull(p.parse((CharSequence)null, Object.class));
		assertNull(p.parse(wrap(""), Object.class));
		assertNull(p.parse(wrap("   "), Object.class));
		assertNull(p.parse(wrap("   \t"), Object.class));
		assertNull(p.parse(wrap("   <!--foo-->"), Object.class));
		assertNull(p.parse(wrap("   <!--foo-->   "), Object.class));
		assertNull(p.parse(wrap("   //foo   "), Object.class));


		A1 t1 = new A1();
		A2 t2 = new A2();
		t2.add(new A3("name0","value0"));
		t2.add(new A3("name1","value1"));
		t1.list = t2;

		s = getBasicSerializer().addBeanTypes().addRootType().build();
		in = strip(s.serialize(t1));
		assertEquals("<rdf:Description><jp:_type>A1</jp:_type><jp:list><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:name>name0</jp:name><jp:value>value0</jp:value></rdf:li><rdf:li rdf:parseType='Resource'><jp:name>name1</jp:name><jp:value>value1</jp:value></rdf:li></rdf:Seq></jp:list></rdf:Description>", in);
		in = wrap(in);
		t1 = p.parse(in, A1.class);
		assertEquals("value1", t1.list.get(1).value);
	}

	@Bean(typeName="A1")
	public static class A1 {
		public A2 list;
	}

	public static class A2 extends LinkedList<A3> {
	}

	public static class A3 {
		public String name, value;
		public A3(){}
		public A3(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

	//====================================================================================================
	// Correct handling of unknown properties.
	//====================================================================================================
	@Test
	public void testCorrectHandlingOfUnknownProperties() throws Exception {
		ReaderParser p = RdfParser.create().xml().ignoreUnknownBeanProperties().build();
		B t;

		String in = wrap("<rdf:Description><jp:a rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>1</jp:a><jp:unknownProperty>foo</jp:unknownProperty><jp:b rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>2</jp:b></rdf:Description>");
		t = p.parse(in, B.class);
		assertEquals(t.a, 1);
		assertEquals(t.b, 2);

		assertThrown(()->{RdfParser.create().xml().build().parse(in, B.class);}).isType(ParseException.class);
	}

	public static class B {
		public int a, b;
	}

	//====================================================================================================
	// Writing to Collection properties with no setters.
	//====================================================================================================
	@Test
	public void testCollectionPropertiesWithNoSetters() throws Exception {
		RdfParser p = RdfParser.create().xml().build();
		String in = wrap("<rdf:Description><jp:ints><rdf:Seq><rdf:li>1</rdf:li><rdf:li>2</rdf:li></rdf:Seq></jp:ints><jp:beans><rdf:Seq><rdf:li rdf:parseType='Resource'><jp:a>1</jp:a><jp:b>2</jp:b></rdf:li></rdf:Seq></jp:beans></rdf:Description>");
		C t = p.parse(in, C.class);
		assertEquals(t.getInts().size(), 2);
		assertEquals(t.getBeans().get(0).b, 2);
	}

	public static class C {
		private Collection<Integer> ints = new LinkedList<>();
		private List<B> beans = new LinkedList<>();
		public Collection<Integer> getInts() {
			return ints;
		}
		public List<B> getBeans() {
			return beans;
		}
	}

	//====================================================================================================
	// Parser listeners.
	//====================================================================================================
	@Test
	public void testParserListeners() throws Exception {
		RdfParser p = RdfParser.create().xml().ignoreUnknownBeanProperties().listener(MyParserListener.class).build();

		String in = wrap("<rdf:Description><jp:a rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>1</jp:a><jp:unknownProperty>foo</jp:unknownProperty><jp:b rdf:datatype='http://www.w3.org/2001/XMLSchema#int'>2</jp:b></rdf:Description>");
		p.parse(in, B.class);
		assertEquals(1, MyParserListener.events.size());
		assertEquals("unknownProperty, line 1, column 0", MyParserListener.events.get(0));
	}

	public static class MyParserListener extends ParserListener {
		static final List<String> events = new LinkedList<>();

		@Override /* ParserListener */
		public <T> void onUnknownBeanProperty(ParserSession session, String propertyName, Class<T> beanClass, T bean) {
			events.add(propertyName + ", " + session.getPosition());
		}
	}
}
