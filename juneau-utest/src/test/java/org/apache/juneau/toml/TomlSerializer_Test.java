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

import org.junit.jupiter.api.*;

class TomlSerializer_Test {

	@Test
	void a01_simpleBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("host", "localhost");
		m.put("port", 8080);
		m.put("debug", true);

		String toml = org.apache.juneau.toml.TomlSerializer.DEFAULT.serialize(m);
		assertNotNull(toml);
		assertTrue(toml.contains("host = \"localhost\""));
		assertTrue(toml.contains("port = 8080"));
		assertTrue(toml.contains("debug = true"));
	}

	@Test
	void a02_collectionOfStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("tags", List.of("web", "api", "rest"));

		String toml = org.apache.juneau.toml.TomlSerializer.DEFAULT.serialize(m);
		assertNotNull(toml);
		assertTrue(toml.contains("[\"web\", \"api\", \"rest\"]") || toml.contains("tags = ["));
	}

	@Test
	void a03_nestedBean() throws Exception {
		var db = new LinkedHashMap<String, Object>();
		db.put("host", "localhost");
		db.put("port", 5432);
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "myapp");
		config.put("database", db);

		String toml = org.apache.juneau.toml.TomlSerializer.DEFAULT.serialize(config);
		assertNotNull(toml);
		assertTrue(toml.contains("name = \"myapp\""));
		assertTrue(toml.contains("[database]"));
		assertTrue(toml.contains("host = \"localhost\""));
		assertTrue(toml.contains("port = 5432"));
	}

}
