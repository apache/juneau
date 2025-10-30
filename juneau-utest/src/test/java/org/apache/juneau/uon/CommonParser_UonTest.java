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
package org.apache.juneau.uon;

import static org.apache.juneau.junit.bct.BctAssertions.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"rawtypes","serial"})
class CommonParser_UonTest extends TestBase {

	ReaderParser p = UonParser.create().beanDictionary(A1.class).build();
	ReaderParser pe = UonParser.DEFAULT_DECODING;

	//====================================================================================================
	// testFromSerializer
	//====================================================================================================
	@Test void a01_fromSerializer() throws Exception {
		var in = "(a=1)";

		var m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));

		in = "(a=1,b='foo+bar')";
		m = (Map)p.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo+bar", m.get("b"));
		m = (Map)pe.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));

		in = "(a=1,b='foo+bar',c=false)";
		m = (Map)pe.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		assertEquals(false, m.get("c"));

		in = "(a=1,b='foo%20bar',c=false)";
		m = (Map)pe.parse(in, Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		assertEquals(false, m.get("c"));

		var jl = (JsonList)p.parse("@((attribute=value),(attribute=~'value~'))", Object.class);
		assertEquals("value", jl.getMap(0).getString("attribute"));
		assertEquals("'value'", jl.getMap(1).getString("attribute"));

		var b = new A1();
		var tl = new A2();
		tl.add(new A3("name0","value0"));
		tl.add(new A3("name1","value1"));
		b.list = tl;

		in = UonSerializer.create().addBeanTypes().addRootType().build().serialize(b);
		b = (A1)p.parse(in, Object.class);
		assertEquals("value1", b.list.get(1).value);

		in = UonSerializer.DEFAULT.serialize(b);
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
	@Test void a02_correctHandlingOfUnknownProperties() throws Exception {
		var p2 = UonParser.create().ignoreUnknownBeanProperties().build();
		var in = "(a=1,unknown=3,b=2)";
		var t = p2.parse(in, B.class);
		assertEquals(1, t.a);
		assertEquals(2, t.b);

		assertThrows(ParseException.class, ()->UonParser.DEFAULT.parse(in, B.class));
	}

	public static class B {
		public int a, b;
	}

	//====================================================================================================
	// Writing to Collection properties with no setters.
	//====================================================================================================
	@Test void a03_collectionPropertiesWithNoSetters() throws Exception {
		var p2 = UonParser.DEFAULT;
		var json = "(ints=@(1,2,3),beans=@((a=1,b=2)))";
		var t = p2.parse(json, C.class);
		assertSize(3, t.getInts());
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
	@Test void a04_parserListeners() throws Exception {
		var p2 = UonParser.create().ignoreUnknownBeanProperties().listener(MyParserListener.class).build();
		var in = "(a=1,unknownProperty=foo,b=2)";
		p2.parse(in, B.class);
		assertSize(1, MyParserListener.events);
		assertEquals("unknownProperty, line 1, column 5", MyParserListener.events.get(0));
	}

	public static class MyParserListener extends ParserListener {
		static final List<String> events = new LinkedList<>();

		@Override /* ParserListener */
		public <T> void onUnknownBeanProperty(ParserSession session, String propertyName, Class<T> beanClass, T bean) {
			events.add(propertyName + ", " + session.getPosition());
		}
	}
}