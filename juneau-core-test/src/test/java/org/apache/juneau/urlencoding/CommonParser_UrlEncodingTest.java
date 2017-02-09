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
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","serial","javadoc"})
public class CommonParser_UrlEncodingTest {

	ReaderParser p = UrlEncodingParser.DEFAULT.clone().addToBeanDictionary(A1.class);

	//====================================================================================================
	// testFromSerializer
	//====================================================================================================
	@Test
	public void testFromSerializer() throws Exception {
		Map m = null;
		String in;

		in = "a=$n(1)";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));

		in = "a=$n(1)&b=foo+bar";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));

		in = "a=$n(1)&b=foo+bar&c=$b(false)";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		assertEquals(false, m.get("c"));

		in = "a=$n(1)&b=foo%20bar&c=$b(false)";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		assertEquals(false, m.get("c"));

		ObjectMap jm = (ObjectMap)p.parse("x=$a($o(attribute=value),$o(attribute='value'))", Object.class);
		assertEquals("value", jm.getObjectList("x").getObjectMap(0).getString("attribute"));
		assertEquals("'value'", jm.getObjectList("x").getObjectMap(1).getString("attribute"));

		ObjectList jl = (ObjectList)p.parse("_value=$a($o(attribute=value),$o(attribute='value'))", Object.class);
		assertEquals("value", jl.getObjectMap(0).getString("attribute"));
		assertEquals("'value'", jl.getObjectMap(1).getString("attribute"));

		A1 b = new A1();
		A2 tl = new A2();
		tl.add(new A3("name0","value0"));
		tl.add(new A3("name1","value1"));
		b.list = tl;

		in = new UrlEncodingSerializer().setAddBeanTypeProperties(true).serialize(b);
		b = (A1)p.parse(in, Object.class);
		assertEquals("value1", b.list.get(1).value);

		in = UrlEncodingSerializer.DEFAULT.serialize(b);
		b = p.parse(in, A1.class);
		assertEquals("value1", b.list.get(1).value);
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
		ReaderParser p = new UrlEncodingParser().setIgnoreUnknownBeanProperties(true);
		B t;

		String in =  "a=1&unknown=3&b=2";
		t = p.parse(in, B.class);
		assertEquals(t.a, 1);
		assertEquals(t.b, 2);

		try {
			p = new UrlEncodingParser();
			p.parse(in, B.class);
			fail("Exception expected");
		} catch (ParseException e) {}
	}

	public static class B {
		public int a, b;
	}

	//====================================================================================================
	// Writing to Collection properties with no setters.
	//====================================================================================================
	@Test
	public void testCollectionPropertiesWithNoSetters() throws Exception {

		ReaderParser p = UrlEncodingParser.DEFAULT;

		String json = "ints=(1,2,3)&beans=((a=1,b=2))";
		C t = p.parse(json, C.class);
		assertEquals(t.getInts().size(), 3);
		assertEquals(t.getBeans().get(0).b, 2);
	}

	public static class C {
		private Collection<Integer> ints = new LinkedList<Integer>();
		private List<B> beans = new LinkedList<B>();
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
		final List<String> events = new LinkedList<String>();
		UonParser p = new UrlEncodingParser().setIgnoreUnknownBeanProperties(true);
		p.addListener(
			new ParserListener() {
				@Override /* ParserListener */
				public <T> void onUnknownProperty(String propertyName, Class<T> beanClass, T bean, int line, int col) {
					events.add(propertyName + "," + line + "," + col);
				}
			}
		);

		String in = "a=1&unknownProperty=foo&b=2";
		p.parse(in, B.class);
		assertEquals(1, events.size());
		assertEquals("unknownProperty,1,4", events.get(0));
	}

	@Test
	public void testCollections() throws Exception {
		WriterSerializer s = new UrlEncodingSerializer().setSimpleMode(true);
		ReaderParser p = new UrlEncodingParser();

		List l = new ObjectList("foo","bar");
		assertEquals("0=foo&1=bar", s.serialize(l));

		String in =  "0=foo&1=bar";
		l = p.parse(in, LinkedList.class, String.class);
		assertObjectEquals("['foo','bar']",l);
	}
}
