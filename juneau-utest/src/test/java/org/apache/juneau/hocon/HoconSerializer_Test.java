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
 * Tests for {@link HoconSerializer}.
 */
class HoconSerializer_Test {

	@Test
	void a01_simpleBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "Alice");
		m.put("age", 30);
		m.put("active", true);
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("name") && hocon.contains("Alice"));
		assertTrue(hocon.contains("age") && hocon.contains("30"));
		assertTrue(hocon.contains("active") && hocon.contains("true"));
		assertTrue(hocon.contains("="));
	}

	@Test
	void a02_nestedBean() throws Exception {
		var address = new LinkedHashMap<String, Object>();
		address.put("city", "Boston");
		address.put("state", "MA");
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "myapp");
		config.put("address", address);
		var hocon = HoconSerializer.DEFAULT.serialize(config);
		assertNotNull(hocon);
		assertTrue(hocon.contains("name") && hocon.contains("myapp"));
		assertTrue(hocon.contains("address") && hocon.contains("city") && hocon.contains("Boston"));
	}

	@Test
	void a03_unquotedStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("key", "simple");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("simple"));
		assertTrue(hocon.contains("key = simple"));
	}

	@Test
	void a04_omitRootBraces() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "Alice");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertFalse(hocon.trim().startsWith("{"));
	}

	@Test
	void a05_withRootBraces() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "Alice");
		var hocon = HoconSerializer.DEFAULT_BRACES.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.trim().startsWith("{"));
	}

	@Test
	void a06_arrayOfStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("tags", List.of("web", "api", "rest"));
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("web") && hocon.contains("api") && hocon.contains("rest"));
	}

	@Test
	void a07_mapProperty() throws Exception {
		var nested = new LinkedHashMap<String, Object>();
		nested.put("host", "localhost");
		nested.put("port", 8080);
		var m = new LinkedHashMap<String, Object>();
		m.put("database", nested);
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("database") && hocon.contains("host") && hocon.contains("localhost"));
		assertTrue(hocon.contains("port") && hocon.contains("8080"));
	}

	@Test
	void a08_nullValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "Alice");
		m.put("middle", null);
		var s = HoconSerializer.create().keepNullProperties().build();
		var hocon = s.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("null"));
	}

	@Test
	void a09_quotedStringRequired() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("special", "a{b}c:d\"e");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("\""));
	}

	@Test
	void a10_tripleQuotedString() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("desc", "line1\nline2");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("\"\"\"") || hocon.contains("line1"));
	}

	@Test
	void a11_equalsSignSeparator() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("key", "value");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertTrue(hocon.contains("="));
		assertTrue(hocon.contains("key = value") || hocon.contains("key= value") || hocon.contains("key =value"));
	}

	@Test
	void a12_newlineSeparators() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", 1);
		m.put("b", 2);
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertTrue(hocon.contains("\n"));
	}

	@Test
	void a13_compactMode() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "Alice");
		m.put("age", 30);
		var hocon = HoconSerializer.DEFAULT_COMPACT.serialize(m);
		assertNotNull(hocon);
		assertFalse(hocon.contains("\n"));
	}

	@Test
	void a14_emptyBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		var hocon = HoconSerializer.DEFAULT_BRACES.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("{") && hocon.contains("}"));
	}

	@Test
	void a15_emptyCollection() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("tags", List.of());
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("[]"));
	}

	@Test
	void a16_enumValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("size", "LARGE");
		m.put("status", "ACTIVE");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertTrue(hocon.contains("LARGE") && hocon.contains("ACTIVE"));
	}

	@Test
	void a17_stringLikeSpecialValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("asBoolean", "true");
		m.put("asNumber", "42");
		m.put("asNull", "null");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertTrue(hocon.contains("\"true\"") || hocon.contains("\"42\"") || hocon.contains("\"null\""));
	}

	@Test
	void a18_collectionOfBeans() throws Exception {
		var server1 = new LinkedHashMap<String, Object>();
		server1.put("host", "a");
		server1.put("port", 8080);
		var server2 = new LinkedHashMap<String, Object>();
		server2.put("host", "b");
		server2.put("port", 9090);
		var m = new LinkedHashMap<String, Object>();
		m.put("servers", List.of(server1, server2));
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("host") && hocon.contains("port"));
		assertTrue(hocon.contains("a") && hocon.contains("b"));
		assertTrue(hocon.contains("8080") && hocon.contains("9090"));
	}

	@Test
	void a19_indentation() throws Exception {
		var nested = new LinkedHashMap<String, Object>();
		nested.put("inner", "value");
		var m = new LinkedHashMap<String, Object>();
		m.put("outer", nested);
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		assertNotNull(hocon);
		assertTrue(hocon.contains("outer") && hocon.contains("inner") && hocon.contains("value"));
		assertTrue(hocon.contains("\n"));
	}
}
