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
package org.apache.juneau.hocon;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link HoconParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to Map<String,Object> when parsing with Map.class, String.class, Object.class
})
class HoconParser_Test {

	@Test
	void b01_equalsSignSeparator() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("name = Alice", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertEquals("Alice", m.get("name"));
	}

	@Test
	void b02_colonSeparator() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("name: Alice", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertEquals("Alice", m.get("name"));
	}

	@Test
	void b03_unquotedValues() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("key = hello", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertEquals("hello", m.get("key"));
	}

	@Test
	void b04_quotedValues() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("key = \"hello world\"", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertEquals("hello world", m.get("key"));
	}

	@Test
	void b05_pathExpressions() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.b.c = 10", Map.class, String.class, Object.class);
		assertNotNull(m);
		var a = m.get("a");
		assertNotNull(a);
		assertTrue(a instanceof Map);
		var b = ((Map<?, ?>) a).get("b");
		assertNotNull(b);
		assertTrue(b instanceof Map);
		var c = ((Map<?, ?>) b).get("c");
		assertEquals(10, ((Number) c).intValue());
	}

	@Test
	void b06_rootBraceless() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("name = myapp\nport = 8080", Map.class, String.class, Object.class);
		assertNotNull(m);
		assertEquals("myapp", m.get("name"));
		assertEquals(8080, ((Number) m.get("port")).intValue());
	}

	@Test
	void b07_nestedObjects() throws Exception {
		var hocon = "database {\n  host = localhost\n  port = 5432\n}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertNotNull(m);
		var db = m.get("database");
		assertNotNull(db);
		assertTrue(db instanceof Map);
		assertEquals("localhost", ((Map<?, ?>) db).get("host"));
		assertEquals(5432, ((Number) ((Map<?, ?>) db).get("port")).intValue());
	}

	@Test
	void b08_pathExpressions() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.b.c = 10", Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		var b = (Map<String, Object>) a.get("b");
		assertEquals(10, ((Number) b.get("c")).intValue());
	}

	@Test
	void b09_objectMerging() throws Exception {
		var hocon = "a { x = 1 }\na { y = 2 }";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		assertEquals(1, ((Number) a.get("x")).intValue());
		assertEquals(2, ((Number) a.get("y")).intValue());
	}

	@Test
	void b10_substitutions() throws Exception {
		var hocon = "x = hello\nval = ${x}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("hello", m.get("val"));
	}

	@Test
	void b11_optionalSubstitutions() throws Exception {
		var hocon = "val = ${?missing}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertNull(m.get("val"));
	}

	@Test
	void b12_tripleQuotedValues() throws Exception {
		var hocon = "key = \"\"\"line1\nline2\"\"\"";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("line1\nline2", m.get("key"));
	}

	@Test
	void b13_hashComments() throws Exception {
		var hocon = "a = 1 # comment\nb = 2";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(1, ((Number) m.get("a")).intValue());
		assertEquals(2, ((Number) m.get("b")).intValue());
	}

	@Test
	void b14_slashComments() throws Exception {
		var hocon = "a = 1\n// comment\nb = 2";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(1, ((Number) m.get("a")).intValue());
		assertEquals(2, ((Number) m.get("b")).intValue());
	}

	@Test
	void b15_trailingCommas() throws Exception {
		var hocon = "arr = [1, 2, 3,]";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var arr = (List<?>) m.get("arr");
		assertEquals(3, arr.size());
		assertEquals(1, ((Number) arr.get(0)).intValue());
		assertEquals(2, ((Number) arr.get(1)).intValue());
		assertEquals(3, ((Number) arr.get(2)).intValue());
	}

	@Test
	void b16_newlineSeparators() throws Exception {
		var hocon = "a = 1\nb = 2\nc = 3";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(1, ((Number) m.get("a")).intValue());
		assertEquals(2, ((Number) m.get("b")).intValue());
		assertEquals(3, ((Number) m.get("c")).intValue());
	}

	@Test
	void b17_nestedObjectsDeep() throws Exception {
		var hocon = "a { b { c = 42 } }";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		var b = (Map<String, Object>) a.get("b");
		assertEquals(42, ((Number) b.get("c")).intValue());
	}

	@Test
	void b18_arrays() throws Exception {
		var hocon = "nums = [1, 2, 3]";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var arr = (List<?>) m.get("nums");
		assertEquals(3, arr.size());
		assertEquals(1, ((Number) arr.get(0)).intValue());
		assertEquals(2, ((Number) arr.get(1)).intValue());
		assertEquals(3, ((Number) arr.get(2)).intValue());
	}

	@Test
	void b19_quotedPathComponent() throws Exception {
		var hocon = "\"a.b\".c = 1";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var ab = (Map<String, Object>) m.get("a.b");
		assertEquals(1, ((Number) ab.get("c")).intValue());
	}

	@Test
	void b20_numberValues() throws Exception {
		var hocon = "int = 42\nfloat = 3.14";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(42, ((Number) m.get("int")).intValue());
		assertEquals(3.14, ((Number) m.get("float")).doubleValue(), 0.001);
	}

	@Test
	void b21_booleanNullValues() throws Exception {
		var hocon = "t = true\nf = false\nn = null";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(true, m.get("t"));
		assertEquals(false, m.get("f"));
		assertNull(m.get("n"));
	}

	@Test
	void b22_plusEquals() throws Exception {
		var hocon = "list = [a, b]\nlist += c";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var list = (List<?>) m.get("list");
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));
	}

	@Test
	void b23_selfReferentialSubstitution() throws Exception {
		var hocon = "path = /usr\npath = ${path}\"/bin\"";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("/usr/bin", m.get("path"));
	}

	@Test
	void b24_valueConcatenation() throws Exception {
		var hocon = "path = /usr \"/local\"";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("/usr/local", m.get("path"));
	}

	@Test
	void b25_arrayConcatenation() throws Exception {
		var hocon = "nums = [1, 2] [3, 4]";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var arr = (List<?>) m.get("nums");
		assertNotNull(arr);
		assertEquals(4, arr.size());
		assertEquals(1, ((Number) arr.get(0)).intValue());
		assertEquals(2, ((Number) arr.get(1)).intValue());
		assertEquals(3, ((Number) arr.get(2)).intValue());
		assertEquals(4, ((Number) arr.get(3)).intValue());
	}

	@Test
	void b26_objectConcatenation() throws Exception {
		var hocon = "obj = { a = 1 } { b = 2 }";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var obj = (Map<String, Object>) m.get("obj");
		assertNotNull(obj);
		assertEquals(1, ((Number) obj.get("a")).intValue());
		assertEquals(2, ((Number) obj.get("b")).intValue());
	}
}
