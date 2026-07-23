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
package org.apache.juneau.marshall.hjson;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link HjsonSerializer}.
 */
class HjsonSerializer_Test {

	@Test
	void a01_simpleBean() {
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "Alice");
		m.put("age", 30);
		m.put("active", true);
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("name") && hjson.contains("Alice"));
		assertTrue(hjson.contains("age") && hjson.contains("30"));
		assertTrue(hjson.contains("active") && hjson.contains("true"));
	}

	@Test
	void a02_nestedBean() {
		var address = new LinkedHashMap<String,Object>();
		address.put("city", "Boston");
		address.put("state", "MA");
		var config = new LinkedHashMap<String,Object>();
		config.put("name", "myapp");
		config.put("address", address);
		var hjson = HjsonSerializer.DEFAULT.write(config);
		assertNotNull(hjson);
		assertTrue(hjson.contains("name") && hjson.contains("myapp"));
		assertTrue(hjson.contains("address") && hjson.contains("city") && hjson.contains("Boston"));
	}

	@Test
	void a03_quotelessStrings() {
		var m = new LinkedHashMap<String,Object>();
		m.put("key", "simple");
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("simple"));
		assertFalse(hjson.contains("\"simple\""));
	}

	@Test
	void a04_compactMode() {
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "Alice");
		m.put("age", 30);
		var hjson = HjsonSerializer.DEFAULT_COMPACT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("name:Alice") || hjson.contains("name: Alice"));
		assertFalse(hjson.contains("\n"));
	}

	@Test
	void a05_nullValue() {
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "Alice");
		m.put("middle", null);
		var s = HjsonSerializer.create().keepNullProperties().build();
		var hjson = s.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("null"));
	}

	@Test
	void a06_arrayOfStrings() {
		var m = new LinkedHashMap<String,Object>();
		m.put("tags", List.of("web", "api", "rest"));
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("web") && hjson.contains("api") && hjson.contains("rest"));
	}

	@Test
	void a07_quotedStringRequired() {
		var m = new LinkedHashMap<String,Object>();
		m.put("special", "a{b}c:d\"e");
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("\""));
	}

	@Test
	void a08_multilineString() {
		var m = new LinkedHashMap<String,Object>();
		m.put("desc", "line1\nline2");
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("'''") || hjson.contains("line1"));
	}

	@Test
	void a09_emptyBean() {
		var m = new LinkedHashMap<String,Object>();
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("{") && hjson.contains("}"));
	}

	@Test
	void a10_emptyCollection() {
		var m = new LinkedHashMap<String,Object>();
		m.put("tags", List.of());
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("[]"));
	}

	@Test
	void a11_stringLikeBoolean() {
		var m = new LinkedHashMap<String,Object>();
		m.put("s", "true");
		var hjson = HjsonSerializer.DEFAULT.write(m);
		assertNotNull(hjson);
		assertTrue(hjson.contains("\"") && hjson.contains("true"));
	}

	@Test
	void a12_omitRootBraces() {
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "x");
		var s = HjsonSerializer.create().omitRootBraces(true).build();
		var hjson = s.write(m);
		assertNotNull(hjson);
		assertFalse(hjson.trim().startsWith("{"));
	}
}
