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
package org.apache.juneau.json;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.json.JsonSerializerContext.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.annotation.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings({"serial","javadoc"})
public class JsonTest {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		Map<String,Object> m = new LinkedHashMap<String,Object>();
		List<Object> l = new LinkedList<Object>();

		WriterSerializer s1 = new JsonSerializer.Simple().setProperty(SERIALIZER_trimNullProperties, false);
		WriterSerializer s2 = new JsonSerializer.Simple().setProperty(SERIALIZER_trimNullProperties, false).setProperty(SERIALIZER_quoteChar, '"');
		String r;

		// Null keys and values
		m.clear();
		m.put(null, null);
		m.put("aaa", "bbb");
		assertEquals("A1", "{null:null,aaa:'bbb'}", s1.serialize(m));

		// Escapes.
		// String = ["]
		m.clear();
		m.put("x", "[\"]");
		assertEquals("{x:\"[\\\"]\"}", s2.serialize(m));
		// String = [\"]
		// JSON = {x:"\\\""}
		m.clear();
		m.put("x", "[\\\"]");
		assertEquals("{x:\"[\\\\\\\"]\"}", s2.serialize(m));

		// String = [\w[\w\-\.]{3,}\w]
		// JSON = {x:"\\w[\\w\\-\\.]{3,}\\w"}
		m.clear();
		r = "\\w[\\w\\-\\.]{3,}\\w";
		m.put("x", r);
		assertEquals("{x:\"\\\\w[\\\\w\\\\-\\\\.]{3,}\\\\w\"}", s2.serialize(m));
		assertEquals(r, new ObjectMap(s2.serialize(m)).getString("x"));

		// String = [foo\bar]
		// JSON = {x:"foo\\bar"}
		m.clear();
		m.put("x", "foo\\bar");
		assertEquals("{x:\"foo\\\\bar\"}", s2.serialize(m));

		m.clear();
		m.put("null", null);
		m.put("aaa", "bbb");
		assertEquals("A2", "{'null':null,aaa:'bbb'}", s1.serialize(m));

		m.clear();
		m.put(null, "null");
		m.put("aaa", "bbb");
		assertEquals("A3", "{null:'null',aaa:'bbb'}", s1.serialize(m));

		// Arrays
		m.clear();
		l.clear();
		m.put("J", "f1");
		m.put("B", "b");
		m.put("C", "c");
		l.add("1");
		l.add("2");
		l.add("3");
		Object o = new Object[] { m, l };
		Object o2 = new Object[] { o, "foo", "bar", new Integer(1), new Boolean(false), new Float(1.2), null };
		assertEquals("K1", "[[{J:'f1',B:'b',C:'c'},['1','2','3']],'foo','bar',1,false,1.2,null]", s1.serialize(o2));
	}

	@Test
	public void testReservedKeywordAttributes() throws Exception {
		Map<String,Object> m = new LinkedHashMap<String,Object>();

		// Keys with reserved names.
		for (String attr : new String[]{"","true","false","null","try","123","1x","-123",".123"}) {
			m.clear();
			m.put(attr,1);
			assertObjectEquals("{'"+attr+"':1}", m);
		}
	}

	//====================================================================================================
	// Validate various backslashes in strings.
	//====================================================================================================
	@Test
	public void testBackslashesInStrings() throws Exception {
		JsonSerializer s = new JsonSerializer.Simple().setProperty(SERIALIZER_trimNullProperties, false).setProperty(SERIALIZER_quoteChar, '"');
		String r, r2;

		// [\\]
		r = "\\";
		r2 = s.serialize(r);
		assertEquals(r2, "\"\\\\\"");
		assertEquals(JsonParser.DEFAULT.parse(r2, Object.class), r);

		// [\b\f\n\t]
		r = "\b\f\n\t";
		r2 = s.serialize(r);
		assertEquals("\"\\b\\f\\n\\t\"", r2);
		assertEquals(r, JsonParser.DEFAULT.parse(r2, Object.class));

		// Special JSON case:  Forward slashes can OPTIONALLY be escaped.
		// [\/]
		assertEquals(JsonParser.DEFAULT.parse("\"\\/\"", Object.class), "/");

		// Unicode
		r = "\u1234\u1ABC\u1abc";
		r2 = s.serialize(r);
		assertEquals("\"\u1234\u1ABC\u1abc\"", r2);

		assertEquals("\u1234", JsonParser.DEFAULT.parse("\"\\u1234\"", Object.class));
	}

	//====================================================================================================
	// Indentation
	//====================================================================================================
	@Test
	public void testIndentation() throws Exception {
		ObjectMap m = new ObjectMap("{J:{B:['c',{D:'e'},['f',{G:'h'},1,false]]},I:'j'}");
		String e = ""
			+ "{"
			+ "\n	J: {"
			+ "\n		B: ["
			+ "\n			'c', "
			+ "\n			{"
			+ "\n				D: 'e'"
			+ "\n			}, "
			+ "\n			["
			+ "\n				'f', "
			+ "\n				{"
			+ "\n					G: 'h'"
			+ "\n				}, "
			+ "\n				1, "
			+ "\n				false"
			+ "\n			]"
			+ "\n		]"
			+ "\n	}, "
			+ "\n	I: 'j'"
			+ "\n}";
		assertEquals(e, JsonSerializer.DEFAULT_LAX_READABLE.serialize(m));
	}

	//====================================================================================================
	// Escaping double quotes
	//====================================================================================================
	@Test
	public void testEscapingDoubleQuotes() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT;
		String r = s.serialize(new ObjectMap().append("f1", "x'x\"x"));
		assertEquals("{\"f1\":\"x'x\\\"x\"}", r);
		JsonParser p = JsonParser.DEFAULT;
		assertEquals("x'x\"x", p.parse(r, ObjectMap.class).getString("f1"));
	}

	//====================================================================================================
	// Escaping single quotes
	//====================================================================================================
	@Test
	public void testEscapingSingleQuotes() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		String r = s.serialize(new ObjectMap().append("f1", "x'x\"x"));
		assertEquals("{f1:'x\\'x\"x'}", r);
		JsonParser p = JsonParser.DEFAULT;
		assertEquals("x'x\"x", p.parse(r, ObjectMap.class).getString("f1"));
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnBean
	//====================================================================================================
	@Test
	public void testWrapperAttrAnnotationOnBean() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		JsonParser p = JsonParser.DEFAULT;
		String r;

		A t = A.create();
		r = s.serialize(t);
		assertEquals("{foo:{f1:1}}", r);
		t = p.parse(r, A.class);
		assertEquals(1, t.f1);

		Map<String,A> m = new LinkedHashMap<String,A>();
		m.put("bar", A.create());
		r = s.serialize(m);
		assertEquals("{bar:{foo:{f1:1}}}", r);

		m = p.parse(r, LinkedHashMap.class, String.class, A.class);
		assertEquals(1, m.get("bar").f1);
	}

	@Json(wrapperAttr="foo")
	public static class A {
		public int f1;

		static A create() {
			A a = new A();
			a.f1 = 1;
			return a;
		}
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnNonBean
	//====================================================================================================
	@Test
	public void testWrapperAttrAnnotationOnNonBean() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		JsonParser p = JsonParser.DEFAULT;
		String r;

		B t = B.create();
		r = s.serialize(t);
		assertEquals("{foo:'1'}", r);
		t = p.parse(r, B.class);
		assertEquals(1, t.f1);

		Map<String,B> m = new LinkedHashMap<String,B>();
		m.put("bar", B.create());
		r = s.serialize(m);
		assertEquals("{bar:{foo:'1'}}", r);

		m = p.parse(r, LinkedHashMap.class, String.class, B.class);
		assertEquals(1, m.get("bar").f1);
	}

	@Json(wrapperAttr="foo")
	public static class B {
		int f1;

		static B create() {
			B b = new B();
			b.f1 = 1;
			return b;
		}

		@Override /* Object */
		public String toString() {
			return String.valueOf(f1);
		}

		public static B valueOf(String s) {
			B b = new B();
			b.f1 = Integer.parseInt(s);
			return b;
		}
	}

	//====================================================================================================
	// testSubclassedList
	//====================================================================================================
	@Test
	public void testSubclassedList() throws Exception {
		JsonSerializer s = new JsonSerializer();
		Map<String,Object> o = new HashMap<String,Object>();
		o.put("c", new C());
		assertEquals("{\"c\":[]}", s.serialize(o));
	}

	public static class C extends LinkedList<String> {
	}

	//====================================================================================================
	// testEscapeSolidus
	//====================================================================================================
	@Test
	public void testEscapeSolidus() throws Exception {
		JsonSerializer s = new JsonSerializer().setProperty(JSON_escapeSolidus, false);
		String r = s.serialize("foo/bar");
		assertEquals("\"foo/bar\"", r);
		r = JsonParser.DEFAULT.parse(r, String.class);
		assertEquals("foo/bar", r);

		s = new JsonSerializer().setProperty(JSON_escapeSolidus, true);
		r = s.serialize("foo/bar");
		assertEquals("\"foo\\/bar\"", r);
		r = JsonParser.DEFAULT.parse(r, String.class);
		assertEquals("foo/bar", r);
	}
}