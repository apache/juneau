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
package org.apache.juneau.toml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.Toml;
import org.junit.jupiter.api.*;

class Toml_Test {

	@Test
	void a01_of() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", "1");
		m.put("b", 2);

		String toml = Toml.of(m);
		assertNotNull(toml);

		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("1", parsed.getString("a"));
		assertEquals(2, parsed.getInt("b"));
	}

	@Test
	void a02_roundTripString() throws Exception {
		var m = JsonMap.of("s", "hello");
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("hello", parsed.getString("s"));
	}

	@Test
	void a03_roundTripNumber() throws Exception {
		var m = JsonMap.of("n", 42);
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(42, parsed.getInt("n"));
	}

	@Test
	void a04_roundTripBoolean() throws Exception {
		var m = JsonMap.of("b", true);
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertTrue(parsed.getBoolean("b"));
	}

	@Test
	void a05_roundTripList() throws Exception {
		var m = JsonMap.of("tags", List.of("a", "b", "c"));
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var list = parsed.getList("tags", String.class, List.of());
		assertEquals(3, list.size());
		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));
	}

	@Test
	void a06_roundTripNested() throws Exception {
		var db = new LinkedHashMap<String, Object>();
		db.put("host", "localhost");
		db.put("port", 5432);
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "myapp");
		config.put("database", db);

		String toml = Toml.of(config);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("myapp", parsed.getString("name"));
		JsonMap dbParsed = parsed.getMap("database");
		assertNotNull(dbParsed);
		assertEquals("localhost", dbParsed.get("host"));
		assertEquals(5432L, dbParsed.get("port"));
	}
}
