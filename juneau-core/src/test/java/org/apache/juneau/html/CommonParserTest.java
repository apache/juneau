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
package org.apache.juneau.html;

import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.serializer.SerializerContext.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","serial","javadoc"})
public class CommonParserTest {

	//====================================================================================================
	// testFromSerializer
	//====================================================================================================
	@Test
	public void testFromSerializer() throws Exception {
		ReaderParser p = HtmlParser.DEFAULT.clone().setClassLoader(getClass().getClassLoader());
		Map m = null;
		String in;

		in = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>a</string></td><td><number>1</number></td></tr></table>";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));

		in = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>a</string></td><td><number>1</number></td></tr><tr><td><string>b</string></td><td><string>foo bar</string></td></tr></table>";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));

		in = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>a</string></td><td><number>1</number></td></tr><tr><td><string>b</string></td><td><string>foo bar</string></td></tr><tr><td><string>c</string></td><td><boolean>false</boolean></td></tr></table>";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals(false, m.get("c"));

		in = " <table type='object'> <tr> <th> <string> key </string> </th> <th> <string> value </string> </th> </tr> <tr> <td> <string> a </string> </td> <td> <number> 1 </number> </td> </tr> <tr> <td> <string> b </string> </td> <td> <string> foo </string> </td> </tr> <tr> <td> <string> c </string> </td> <td> <boolean> false </boolean> </td> </tr> </table> ";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo", m.get("b"));
		assertEquals(false, m.get("c"));

		in = "<table type='array'><tr><th>attribute</th></tr><tr><td><string>value</string></td></tr><tr><td><string>value</string></td></tr></table>";
		ObjectList jl = (ObjectList)p.parse(in, Object.class);
		assertEquals("value", jl.getObjectMap(0).getString("attribute"));
		assertEquals("value", jl.getObjectMap(1).getString("attribute"));

		A1 t1 = new A1();
		A2 t2 = new A2();
		t2.add(new A3("name0","value0"));
		t2.add(new A3("name1","value1"));
		t1.list = t2;
		in = new HtmlSerializer().setProperty(SERIALIZER_addClassAttrs, true).serialize(t1);
		t1 = (A1)p.parse(in, Object.class);
		assertEquals("value1", t1.list.get(1).value);

		in = HtmlSerializer.DEFAULT.serialize(t1);
		t1 = p.parse(in, A1.class);
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
	@Test
	public void testCorrectHandlingOfUnknownProperties() throws Exception {
		ReaderParser p = new HtmlParser().setProperty(BEAN_ignoreUnknownBeanProperties, true);
		B t;

		String in = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>a</string></td><td><number>1</number></td></tr><tr><td><string>unknown</string></td><td><number>1</number></td></tr><tr><td><string>b</string></td><td><number>2</number></td></tr></table>";
		t = p.parse(in, B.class);
		assertEquals(t.a, 1);
		assertEquals(t.b, 2);

		try {
			p = new HtmlParser();
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
		ReaderParser p = HtmlParser.DEFAULT;

		String in = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>ints</string></td><td><ul><li><number>1</number></li><li><number>2</number></li><li><number>3</number></li></ul></td></tr><tr><td><string>beans</string></td><td><table type='array'><tr><th>a</th><th>b</th></tr><tr><td><number>1</number></td><td><number>2</number></td></tr></table></td></tr></table>";
		C t = p.parse(in, C.class);
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
		HtmlParser p = new HtmlParser().setProperty(BEAN_ignoreUnknownBeanProperties, true);
		p.addListener(
			new ParserListener() {
				@Override /* ParserListener */
				public <T> void onUnknownProperty(String propertyName, Class<T> beanClass, T bean, int line, int col) {
					events.add(propertyName + "," + line + "," + col);
				}
			}
		);

		String in = "<table type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>a</string></td><td><number>1</number></td></tr><tr><td><string>unknown</string></td><td><string>/foo</string></td></tr><tr><td><string>b</string></td><td><number>2</number></td></tr></table>";
		p.parse(in, B.class);
		assertEquals(1, events.size());
		assertEquals("unknown,-1,-1", events.get(0));
	}

}
