/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.json;

import static com.ibm.juno.core.BeanContextProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;

@SuppressWarnings({"rawtypes","serial"})
public class CT_CommonParser {

	//====================================================================================================
	// testFromSerializer
	//====================================================================================================
	@Test
	public void testFromSerializer() throws Exception {
		ReaderParser p = JsonParser.DEFAULT.clone().setClassLoader(getClass().getClassLoader());

		Map m = null;
		m = (Map)p.parse("{a:1}", Object.class);
		assertEquals(1, m.get("a"));
		m = (Map)p.parse("{a:1,b:\"foo bar\"}", Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo bar", m.get("b"));
		m = (Map)p.parse("{a:1,b:\"foo bar\",c:false}", Object.class);
		assertEquals(1, m.get("a"));
		assertEquals(false, m.get("c"));
		m = (Map)p.parse(" { a : 1 , b : 'foo' , c : false } ", Object.class);
		assertEquals(1, m.get("a"));
		assertEquals("foo", m.get("b"));
		assertEquals(false, m.get("c"));

		m = (Map)p.parse("{x:\"com.ibm.juno.core.test.Person\",addresses:[{x:\"com.ibm.juno.core.test.Address\",city:\"city A\",state:\"state A\",street:\"street A\",zip:12345}]}", Object.class);
		assertEquals("com.ibm.juno.core.test.Person", m.get("x"));
		List l = (List)m.get("addresses");
		assertNotNull(l);
		m = (Map)l.get(0);
		assertNotNull(m);
		assertEquals("com.ibm.juno.core.test.Address", m.get("x"));
		assertEquals("city A", m.get("city"));
		assertEquals("state A", m.get("state"));
		assertEquals("street A", m.get("street"));
		assertEquals(12345, m.get("zip"));

		ObjectList jl = (ObjectList)p.parse("[{attribute:'value'},{attribute:'value'}]", Object.class);
		assertEquals("value", jl.getObjectMap(0).getString("attribute"));
		assertEquals("value", jl.getObjectMap(1).getString("attribute"));

		// Verify that all the following return null.
		assertNull(p.parse((CharSequence)null, Object.class));
		assertNull(p.parse("", Object.class));
		assertNull(p.parse("   ", Object.class));
		assertNull(p.parse("   \t", Object.class));
		assertNull(p.parse("   /*foo*/", Object.class));
		assertNull(p.parse("   /*foo*/   ", Object.class));
		assertNull(p.parse("   //foo   ", Object.class));

		try {
			jl = (ObjectList)p.parse("[{attribute:'value'},{attribute:'value'}]", Object.class);
			assertEquals("value", jl.getObjectMap(0).getString("attribute"));
			assertEquals("value", jl.getObjectMap(1).getString("attribute"));
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}

		A1 b = new A1();
		A2 tl = new A2();
		tl.add(new A3("name0","value0"));
		tl.add(new A3("name1","value1"));
		b.list = tl;
		String json = new JsonSerializer().setProperty(SERIALIZER_addClassAttrs, true).serialize(b);
		b = (A1)p.parse(json, Object.class);
		assertEquals("value1", b.list.get(1).value);

		json = JsonSerializer.DEFAULT.serialize(b);
		b = p.parse(json, A1.class);
		assertEquals("value1", b.list.get(1).value);
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
		ReaderParser p = new JsonParser().setProperty(BEAN_ignoreUnknownBeanProperties, true);
		B b;

		String in =  "{a:1,unknown:3,b:2}";
		b = p.parse(in, B.class);
		assertEquals(b.a, 1);
		assertEquals(b.b, 2);

		try {
			p = new JsonParser();
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
		JsonParser p = JsonParser.DEFAULT;
		String json = "{ints:[1,2,3],beans:[{a:1,b:2}]}";
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
		JsonParser p = new JsonParser().setProperty(BEAN_ignoreUnknownBeanProperties, true);
		p.addListener(
			new ParserListener() {
				@Override /* ParserListener */
				public <T> void onUnknownProperty(String propertyName, Class<T> beanClass, T bean, int line, int col) {
					events.add(propertyName + "," + line + "," + col);
				}
			}
		);

		String json = "{a:1,unknownProperty:\"/foo\",b:2}";
		p.parse(json, B.class);
		assertEquals(1, events.size());
		assertEquals("unknownProperty,1,5", events.get(0));
	}
}
