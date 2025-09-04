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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.annotation.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class Json_Test extends SimpleTestBase{

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		var l = new LinkedList<>();

		var s1 = JsonSerializer.create().json5().keepNullProperties().build();
		var s2 = JsonSerializer.create().simpleAttrs().keepNullProperties().build();

		// Null keys and values
		m.clear();
		m.put(null, null);
		m.put("aaa", "bbb");
		assertEquals("{null:null,aaa:'bbb'}", s1.serialize(m));

		// Escapes.
		// String = ["]
		m.clear();
		m.put("x", "[\"]");
		assertEquals("{x:\"[\\\"]\"}", s2.serialize(m));
		// String = [\"]
		// JSON = {x:"\\\""} - NOSONAR
		m.clear();
		m.put("x", "[\\\"]");
		assertEquals("{x:\"[\\\\\\\"]\"}", s2.serialize(m));

		// String = [\w[\w\-\.]{3,}\w]
		// JSON = {x:"\\w[\\w\\-\\.]{3,}\\w"} - NOSONAR
		m.clear();
		var r = "\\w[\\w\\-\\.]{3,}\\w";
		m.put("x", r);
		assertEquals("{x:\"\\\\w[\\\\w\\\\-\\\\.]{3,}\\\\w\"}", s2.serialize(m));
		assertEquals(r, JsonMap.ofJson(s2.serialize(m)).getString("x"));

		// String = [foo\bar]
		// JSON = {x:"foo\\bar"} - NOSONAR
		m.clear();
		m.put("x", "foo\\bar");
		assertEquals("{x:\"foo\\\\bar\"}", s2.serialize(m));

		m.clear();
		m.put("null", null);
		m.put("aaa", "bbb");
		assertEquals("{'null':null,aaa:'bbb'}", s1.serialize(m));

		m.clear();
		m.put(null, "null");
		m.put("aaa", "bbb");
		assertEquals("{null:'null',aaa:'bbb'}", s1.serialize(m));

		// Arrays
		m.clear();
		l.clear();
		m.put("J", "f1");
		m.put("B", "b");
		m.put("C", "c");
		l.add("1");
		l.add("2");
		l.add("3");
		var o = new Object[] { m, l };
		var o2 = new Object[] { o, "foo", "bar", Integer.valueOf(1), Boolean.valueOf(false), Float.valueOf(1.2f), null };
		assertEquals("[[{J:'f1',B:'b',C:'c'},['1','2','3']],'foo','bar',1,false,1.2,null]", s1.serialize(o2));
	}

	@Test void a02_reservedKeywordAttributes() {
		var m = new LinkedHashMap<String,Object>();

		// Keys with reserved names.
		for (var attr : new String[]{"","true","false","null","try","123","1x","-123",".123"}) {
			m.clear();
			m.put(attr,1);
			assertJson("{'"+attr+"':1}", m);
		}
	}

	//====================================================================================================
	// Validate various backslashes in strings.
	//====================================================================================================
	@Test void a03_backslashesInStrings() throws Exception {
		var s = JsonSerializer.create().simpleAttrs().keepNullProperties().build();

		// [\\]
		var r = "\\";
		var r2 = s.serialize(r);
		assertEquals("\"\\\\\"", r2);
		assertEquals(JsonParser.DEFAULT.parse(r2, Object.class), r);

		// [\b\f\n\t]
		r = "\b\f\n\t";
		r2 = s.serialize(r);
		assertEquals("\"\\b\\f\\n\\t\"", r2);
		assertEquals(r, JsonParser.DEFAULT.parse(r2, Object.class));

		// Special JSON case:  Forward slashes can OPTIONALLY be escaped.
		// [\/]
		assertEquals("/", JsonParser.DEFAULT.parse("\"\\/\"", Object.class));

		// Unicode
		r = "\u1234\u1ABC\u1abc";
		r2 = s.serialize(r);
		assertEquals("\"\u1234\u1ABC\u1abc\"", r2);

		assertEquals("\u1234", JsonParser.DEFAULT.parse("\"\\u1234\"", Object.class));
	}

	//====================================================================================================
	// Indentation
	//====================================================================================================
	@Test void a04_indentation() throws Exception {
		var m = JsonMap.ofJson("{J:{B:['c',{D:'e'},['f',{G:'h'},1,false]]},I:'j'}");
		var e = """
			{
				J: {
					B: [
						'c',
						{
							D: 'e'
						},
						[
							'f',
							{
								G: 'h'
							},
							1,
							false
						]
					]
				},
				I: 'j'
			}""";
		assertEquals(e, Json5Serializer.DEFAULT_READABLE.serialize(m));
	}

	//====================================================================================================
	// Escaping double quotes
	//====================================================================================================
	@Test void a05_escapingDoubleQuotes() throws Exception {
		var s = JsonSerializer.DEFAULT;
		var r = s.serialize(JsonMap.of("f1", "x'x\"x"));
		assertEquals("{\"f1\":\"x'x\\\"x\"}", r);
		var p = JsonParser.DEFAULT;
		assertEquals("x'x\"x", p.parse(r, JsonMap.class).getString("f1"));
	}

	//====================================================================================================
	// Escaping single quotes
	//====================================================================================================
	@Test void a06_escapingSingleQuotes() throws Exception {
		var s = Json5Serializer.DEFAULT;
		var r = s.serialize(JsonMap.of("f1", "x'x\"x"));
		assertEquals("{f1:'x\\'x\"x'}", r);
		var p = JsonParser.DEFAULT;
		assertEquals("x'x\"x", p.parse(r, JsonMap.class).getString("f1"));
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnBean
	//====================================================================================================
	@Test void a07_wrapperAttrAnnotationOnBean() throws Exception {
		var s = Json5Serializer.DEFAULT;
		var p = JsonParser.DEFAULT;

		var t = A.create();
		var r = s.serialize(t);
		assertEquals("{foo:{f1:1}}", r);
		t = p.parse(r, A.class);
		assertEquals(1, t.f1);

		var m = new LinkedHashMap<String,A>();
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
			var a = new A();
			a.f1 = 1;
			return a;
		}
	}

	@Test void a08_wrapperAttrAnnotationOnBean_usingConfig() throws Exception {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(A2Config.class).build();
		var p = JsonParser.DEFAULT.copy().applyAnnotations(A2Config.class).build();

		var t = A2.create();
		var r = s.serialize(t);
		assertEquals("{foo:{f1:1}}", r);
		t = p.parse(r, A2.class);
		assertEquals(1, t.f1);

		var m = new LinkedHashMap<String,A2>();
		m.put("bar", A2.create());
		r = s.serialize(m);
		assertEquals("{bar:{foo:{f1:1}}}", r);

		m = p.parse(r, LinkedHashMap.class, String.class, A2.class);
		assertEquals(1, m.get("bar").f1);
	}

	@Json(on="Dummy1",wrapperAttr="foo")
	@Json(on="A2",wrapperAttr="foo")
	@Json(on="Dummy2",wrapperAttr="foo")
	private static class A2Config {}

	public static class A2 {
		public int f1;

		static A2 create() {
			var a = new A2();
			a.f1 = 1;
			return a;
		}
	}

	//====================================================================================================
	// testWrapperAttrAnnotationOnNonBean
	//====================================================================================================
	@Test void a09_wrapperAttrAnnotationOnNonBean() throws Exception {
		var s = Json5Serializer.DEFAULT;
		var p = JsonParser.DEFAULT;

		var t = B.create();
		var r = s.serialize(t);
		assertEquals("{foo:'1'}", r);
		t = p.parse(r, B.class);
		assertEquals(1, t.f1);

		var m = new LinkedHashMap<String,B>();
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
			var b = new B();
			b.f1 = 1;
			return b;
		}

		@Override /* Object */
		public String toString() {
			return String.valueOf(f1);
		}

		public static B valueOf(String s) {
			var b = new B();
			b.f1 = Integer.parseInt(s);
			return b;
		}
	}

	@Test void a10_wrapperAttrAnnotationOnNonBean_usingConfig() throws Exception {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(B2Config.class).build();
		var p = JsonParser.DEFAULT.copy().applyAnnotations(B2Config.class).build();

		var t = B2.create();
		var r = s.serialize(t);
		assertEquals("{foo:'1'}", r);
		t = p.parse(r, B2.class);
		assertEquals(1, t.f1);

		var m = new LinkedHashMap<String,B2>();
		m.put("bar", B2.create());
		r = s.serialize(m);
		assertEquals("{bar:{foo:'1'}}", r);

		m = p.parse(r, LinkedHashMap.class, String.class, B2.class);
		assertEquals(1, m.get("bar").f1);
	}

	@Json(on="B2",wrapperAttr="foo")
	private static class B2Config {}

	public static class B2 {
		int f1;

		static B2 create() {
			var b = new B2();
			b.f1 = 1;
			return b;
		}

		@Override /* Object */
		public String toString() {
			return String.valueOf(f1);
		}

		public static B2 valueOf(String s) {
			var b = new B2();
			b.f1 = Integer.parseInt(s);
			return b;
		}
	}

	//====================================================================================================
	// testSubclassedList
	//====================================================================================================
	@Test void a11_subclassedList() throws Exception {
		var s = JsonSerializer.DEFAULT;
		var o = new HashMap<>();
		o.put("c", new C());
		assertEquals("{\"c\":[]}", s.serialize(o));
	}

	public static class C extends LinkedList<String> {
	}

	//====================================================================================================
	// testEscapeSolidus
	//====================================================================================================
	@Test void a12_escapeSolidus() throws Exception {
		var s = JsonSerializer.create().build();
		var r = s.serialize("foo/bar");
		assertEquals("\"foo/bar\"", r);
		r = JsonParser.DEFAULT.parse(r, String.class);
		assertEquals("foo/bar", r);

		s = JsonSerializer.create().escapeSolidus().build();
		r = s.serialize("foo/bar");
		assertEquals("\"foo\\/bar\"", r);
		r = JsonParser.DEFAULT.parse(r, String.class);
		assertEquals("foo/bar", r);

		s = JsonSerializer.create().escapeSolidus().build();
		r = s.serialize("foo/bar");
		assertEquals("\"foo\\/bar\"", r);
		r = JsonParser.DEFAULT.parse(r, String.class);
		assertEquals("foo/bar", r);
	}
}