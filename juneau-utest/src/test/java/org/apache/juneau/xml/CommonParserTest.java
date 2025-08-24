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

import static org.junit.Assert.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"rawtypes","serial"})
class CommonParserTest  extends SimpleTestBase{

	//====================================================================================================
	// testFromSerializer
	//====================================================================================================
	@Test void testFromSerializer() throws Exception {
		ReaderParser p = XmlParser.DEFAULT;

		Map m = null;
		m = (Map)p.parse("<object><a _type='number'>1</a></object>", Object.class);
		assertEquals(1, m.get("a"));
		m = (Map)p.parse("<object><a _type='number'>1</a><b _type='string'>foo bar</b></object>", Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		m = (Map)p.parse("<object><a _type='number'>1</a><b _type='string'>foo bar</b><c _type='boolean'>false</c></object>", Object.class);
		assertEquals(1, m.get("a"));
		assertEquals(false, m.get("c"));
		m = (Map)p.parse("   <object>	<a _type='number'>	1	</a>	<b _type='string'>	foo	</b>	<c _type='boolean'>	false 	</c>	</object>	", Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo", m.get("b"));
		assertEquals(false, m.get("c"));

		m = (Map)p.parse("<object><x _type='string'>org.apache.juneau.test.Person</x><addresses _type='array'><object><x _type='string'>org.apache.juneau.test.Address</x><city _type='string'>city A</city><state _type='string'>state A</state><street _type='string'>street A</street><zip _type='number'>12345</zip></object></addresses></object>", Object.class);
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

		JsonList jl = (JsonList)p.parse("<array><object><attribute _type='string'>value</attribute></object><object><attribute _type='string'>value</attribute></object></array>", Object.class);
		assertEquals("value", jl.getMap(0).getString("attribute"));
		assertEquals("value", jl.getMap(1).getString("attribute"));

		jl = (JsonList)p.parse("<array><object><attribute _type='string'>value</attribute></object><object><attribute _type='string'>value</attribute></object></array>", Object.class);
		assertEquals("value", jl.getMap(0).getString("attribute"));
		assertEquals("value", jl.getMap(1).getString("attribute"));

		var t1 = new A1();
		var t2 = new A2();
		t2.add(new A3("name0","value0"));
		t2.add(new A3("name1","value1"));
		t1.list = t2;
		String r = XmlSerializer.DEFAULT_NS.serialize(t1);
		t1 = p.parse(r, A1.class);
		assertEquals("value1", t1.list.get(1).value);

		r = XmlSerializer.DEFAULT_NS.serialize(t1);
		t1 = p.parse(r, A1.class);
		assertEquals("value1", t1.list.get(1).value);
	}

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
	@Test void testCorrectHandlingOfUnknownProperties() throws Exception {
		var p = XmlParser.create().ignoreUnknownBeanProperties().build();
		B t;

		String in =  "<object><a>1</a><unknown>foo</unknown><b>2</b></object>";
		t = p.parse(in, B.class);
		assertEquals(1, t.a);
		assertEquals(2, t.b);

		String in2 =  "<object><a>1</a><unknown><object><a _type='string'>foo</a></object></unknown><b>2</b></object>";
		t = p.parse(in2, B.class);
		assertEquals(1, t.a);
		assertEquals(2, t.b);

		assertThrows(ParseException.class, ()->XmlParser.DEFAULT.parse(in2, B.class));
	}

	public static class B {
		public int a, b;
	}

	//====================================================================================================
	// Writing to Collection properties with no setters.
	//====================================================================================================
	@Test void testCollectionPropertiesWithNoSetters() throws Exception {

		ReaderParser p = XmlParser.DEFAULT;

		String in = "<object><ints _type='array'><number>1</number><number>2</number><number>3</number></ints><beans _type='array'><object><a _type='number'>1</a><b _type='number'>2</b></object></beans></object>";
		C t = p.parse(in, C.class);
		assertEquals(3, t.getInts().size());
		assertEquals(2, t.getBeans().get(0).b);
	}

	public static class C {
		private Collection<Integer> ints = new LinkedList<>();
		public Collection<Integer> getInts() { return ints; }

		private List<B> beans = new LinkedList<>();
		public List<B> getBeans() { return beans; }
	}

	//====================================================================================================
	// Parser listeners.
	//====================================================================================================
	@Test void testParserListeners() throws Exception {
		var p = XmlParser.create().ignoreUnknownBeanProperties().listener(MyParserListener.class).build();

		String in = "<object><a _type='number'>1</a><unknownProperty _type='string'>foo</unknownProperty><b _type='number'>2</b></object>";
		p.parse(in, B.class);
		assertEquals(1, MyParserListener.events.size());
		// XML parser may or may not support line numbers.
		assertTrue(MyParserListener.events.get(0).startsWith("unknownProperty,"));
	}

	public static class MyParserListener extends ParserListener {
		static final List<String> events = new LinkedList<>();

		@Override /* ParserListener */
		public <T> void onUnknownBeanProperty(ParserSession session, String propertyName, Class<T> beanClass, T bean) {
			events.add(propertyName + ", " + session.getPosition());
		}
	}
}
