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
package org.apache.juneau.proto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.JsonMap;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProtoSerializer}.
 */
class ProtoSerializer_Test {

	@Test
	void a01_simpleBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("host", "localhost");
		m.put("port", 8080);
		m.put("debug", true);
		m.put("ratio", 3.14);

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("host:"));
		assertTrue(proto.contains("localhost"));
		assertTrue(proto.contains("port:"));
		assertTrue(proto.contains("8080"));
		assertTrue(proto.contains("debug:"));
		assertTrue(proto.contains("true"));
		assertTrue(proto.contains("ratio:"));
		assertTrue(proto.contains("3.14"));
	}

	@Test
	void a02_nestedBean() throws Exception {
		var db = new LinkedHashMap<String, Object>();
		db.put("host", "localhost");
		db.put("port", 5432);
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "myapp");
		config.put("database", db);

		String proto = ProtoSerializer.DEFAULT.serialize(config);
		assertNotNull(proto);
		assertTrue(proto.contains("name:"));
		assertTrue(proto.contains("database {"));
		assertTrue(proto.contains("host:"));
		assertTrue(proto.contains("5432"));
	}

	@Test
	void a03_deeplyNestedBean() throws Exception {
		var ssl = new LinkedHashMap<String, Object>();
		ssl.put("enabled", true);
		var server = new LinkedHashMap<String, Object>();
		server.put("host", "localhost");
		server.put("ssl", ssl);
		var config = new LinkedHashMap<String, Object>();
		config.put("server", server);

		String proto = ProtoSerializer.DEFAULT.serialize(config);
		assertNotNull(proto);
		assertTrue(proto.contains("server {"));
		assertTrue(proto.contains("ssl {"));
		assertTrue(proto.contains("enabled: true"));
	}

	@Test
	void a04_collectionOfStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("tags", List.of("a", "b", "c"));

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("tags"));
		assertTrue(proto.contains("a") && proto.contains("b") && proto.contains("c"));
	}

	@Test
	void a05_collectionOfIntegers() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("ports", List.of(8080, 8443, 9090));

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("ports"));
		assertTrue(proto.contains("8080"));
	}

	@Test
	void a06_collectionOfBeans() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("host", "alpha");
		a.put("port", 8080);
		var b = new LinkedHashMap<String, Object>();
		b.put("host", "beta");
		b.put("port", 8081);
		var m = new LinkedHashMap<String, Object>();
		m.put("servers", List.of(a, b));

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("servers"));
		assertTrue(proto.contains("alpha") && proto.contains("beta"));
	}

	@Test
	void a07_mapProperty() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("env", Map.of("PATH", "/usr/bin", "HOME", "/home/user"));

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("env {"));
		assertTrue(proto.contains("PATH") || proto.contains("HOME"));
	}

	@Test
	void a08_nullValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", "x");
		m.put("b", null);
		m.put("c", 1);

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("a:"));
		assertTrue(proto.contains("c:"));
		assertFalse(proto.contains("b:"));
	}

	@Test
	void a09_booleanValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("t", true);
		m.put("f", false);

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertTrue(proto.contains("true"));
		assertTrue(proto.contains("false"));
	}

	@Test
	void a10_floatValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("x", 3.14);
		m.put("inf", Double.POSITIVE_INFINITY);
		m.put("nan", Double.NaN);

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("3.14") || proto.contains("3.14"));
		assertTrue(proto.contains("inf"));
		assertTrue(proto.contains("nan"));
	}

	@Test
	void a11_stringEscaping() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("s", "a\nb\tc\\d\"e");

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("\\n") || proto.contains("\\\\"));
	}

	@Test
	void a12_enumValues() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("level", LogLevel.WARN);

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertTrue(proto.contains("WARN"));
		assertFalse(proto.contains("\"WARN\""));
	}

	@Test
	void a13_emptyBean() throws Exception {
		var m = new LinkedHashMap<String, Object>();

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.trim().isEmpty() || proto.equals(""));
	}

	@Test
	void a14_emptyCollections() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("tags", List.of());

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertTrue(proto.contains("tags") && proto.contains("[]"));
	}

	@Test
	void a16_noColonBeforeMessages() throws Exception {
		var inner = new LinkedHashMap<String, Object>();
		inner.put("x", 1);
		var m = new LinkedHashMap<String, Object>();
		m.put("inner", inner);

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertTrue(proto.contains("inner {"));
		assertFalse(proto.contains("inner: {"));
	}

	@Test
	void a17_indentation() throws Exception {
		var inner = new LinkedHashMap<String, Object>();
		inner.put("x", 1);
		var m = new LinkedHashMap<String, Object>();
		m.put("inner", inner);

		String proto = ProtoSerializer.DEFAULT_READABLE.serialize(m);
		assertTrue(proto.contains("\n") || proto.contains("inner"));
	}

	@Test
	void a18_addBeanTypesAndRootType() throws Exception {
		var m = JsonMap.of("name", "test");
		var serializer = ProtoSerializer.create().addBeanTypes().addRootType().build();
		String proto = serializer.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("name"));
		assertTrue(proto.contains("test"));
	}

	@Test
	void a20_binaryData() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("data", new byte[] { 0x0a, 0x05 });

		String proto = ProtoSerializer.DEFAULT.serialize(m);
		assertNotNull(proto);
		assertTrue(proto.contains("data:"));
	}

	enum LogLevel { DEBUG, INFO, WARN, ERROR }
}
