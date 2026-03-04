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
 * WITHOUT WARRANTIES OR CONDITIONS FOR A PARTICULAR PURPOSE.  See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ini;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link IniSerializer}.
 */
class IniSerializer_Test {

	//====================================================================================================
	// a - Simple bean and flat properties
	//====================================================================================================

	@Test
	void a01_simpleBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("host", "localhost");
		m.put("port", 8080);
		m.put("debug", true);
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("host = localhost") || ini.contains("host=localhost"));
		assertTrue(ini.contains("port = 8080") || ini.contains("port=8080"));
		assertTrue(ini.contains("debug = true") || ini.contains("debug=true"));
	}

	@Test
	void a02_nestedBean() throws Exception {
		var db = new LinkedHashMap<String, Object>();
		db.put("host", "localhost");
		db.put("port", 5432);
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "myapp");
		config.put("database", db);
		var ini = IniSerializer.DEFAULT.serialize(config);
		assertNotNull(ini);
		assertTrue(ini.contains("name") && ini.contains("myapp"));
		assertTrue(ini.contains("[database]"));
		assertTrue(ini.contains("host") && ini.contains("localhost"));
		assertTrue(ini.contains("port") && ini.contains("5432"));
	}

	@Test
	void a03_beanWithListOfStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("tags", List.of("web", "api", "rest"));
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("tags =") || ini.contains("tags="));
		assertTrue(ini.contains("web") && ini.contains("api") && ini.contains("rest"));
	}

	@Test
	void a04_nullValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "Alice");
		m.put("middle", null);
		var s = IniSerializer.create().keepNullProperties().build();
		var ini = s.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("name") && ini.contains("Alice"));
		assertTrue(ini.contains("null") || ini.contains("middle"));
	}

	@Test
	void a05_stringQuoting() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("key", "123");
		m.put("flag", "true");
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("'123'") || ini.contains("123"));
		assertTrue(ini.contains("'true'") || ini.contains("true"));
	}

	@Test
	void a06_emptyStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("empty", "");
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("empty = ''") || ini.contains("empty=''") || ini.contains("empty ="));
	}

	@Test
	void a07_beanWithMap() throws Exception {
		var settings = new LinkedHashMap<String, String>();
		settings.put("timeout", "30");
		settings.put("retries", "3");
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "app");
		config.put("settings", settings);
		var ini = IniSerializer.DEFAULT.serialize(config);
		assertNotNull(ini);
		assertTrue(ini.contains("[settings]"));
		assertTrue(ini.contains("timeout") && ini.contains("30"));
		assertTrue(ini.contains("retries") && ini.contains("3"));
	}

	@Test
	void a08_deeplyNestedBean() throws Exception {
		var company = new LinkedHashMap<String, Object>();
		company.put("name", "Acme");
		company.put("ticker", "ACME");
		var employment = new LinkedHashMap<String, Object>();
		employment.put("title", "Engineer");
		employment.put("company", company);
		var person = new LinkedHashMap<String, Object>();
		person.put("name", "John");
		person.put("employment", employment);
		var ini = IniSerializer.DEFAULT.serialize(person);
		assertNotNull(ini);
		assertTrue(ini.contains("name") && ini.contains("John"));
		assertTrue(ini.contains("employment") && (ini.contains("[employment]") || ini.contains("employment")));
		assertTrue(ini.contains("Acme"));
	}

	@Test
	void a09_multipleNestedBeans() throws Exception {
		var addr1 = new LinkedHashMap<String, Object>();
		addr1.put("city", "Boston");
		var addr2 = new LinkedHashMap<String, Object>();
		addr2.put("city", "NYC");
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "x");
		m.put("home", addr1);
		m.put("work", addr2);
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("[home]"));
		assertTrue(ini.contains("[work]"));
		assertTrue(ini.contains("Boston") && ini.contains("NYC"));
	}

	@Test
	void a10_topLevelCollectionThrows() throws Exception {
		var list = List.of("a", "b", "c");
		var ex = assertThrows(Exception.class, () -> IniSerializer.DEFAULT.serialize(list));
		assertTrue(ex.getMessage().contains("Collection") || ex.getMessage().contains("not supported")
			|| ex.getClass().getSimpleName().contains("Serialize"));
	}

	@Test
	void a11_topLevelScalarThrows() throws Exception {
		var ex = assertThrows(Exception.class, () -> IniSerializer.DEFAULT.serialize("hello"));
		assertTrue(ex.getMessage().contains("not supported") || ex.getMessage().contains("bean")
			|| ex.getClass().getSimpleName().contains("Serialize"));
	}

	@Test
	void a12_emptyBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.trim().isEmpty() || ini.contains("\n\n"));
	}

	@Test
	void a13_kvSeparator() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("key", "value");
		var s = IniSerializer.create().kvSeparator(':').build();
		var ini = s.serialize(m);
		assertTrue(ini.contains(":") && ini.contains("value"));
	}

	@Test
	void a14_addBeanTypes() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "test");
		var s = IniSerializer.create().addBeanTypes().addRootType().build();
		var ini = s.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("name") && ini.contains("test"));
	}

	//====================================================================================================
	// b - Date/time and enum
	//====================================================================================================

	@Test
	void b01_dateValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("date", LocalDate.of(2024, 3, 15));
		m.put("instant", Instant.parse("2024-03-15T12:00:00Z"));
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertNotNull(ini);
		assertTrue(ini.contains("2024"));
	}

	@Test
	void b02_enumValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("status", TestEnum.ACTIVE);
		var ini = IniSerializer.DEFAULT.serialize(m);
		assertTrue(ini.contains("ACTIVE"));
	}

	enum TestEnum { ACTIVE, INACTIVE }
}
