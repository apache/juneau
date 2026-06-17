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
package org.apache.juneau.marshall.json5l;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Json5lSerializer}.
 */
class Json5l_Test extends TestBase {

	@BeanType(properties = "name,age")
	public static class Person {
		public String name;
		public int age;

		public Person() {}
		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}
	}

	// =================================================================================
	// A. Serialization — strict-per-line default (mirrors JSONL semantics)
	// =================================================================================

	@Test
	void a01_serializeCollectionOfBeans() throws Exception {
		var list = list(
			new Person("Alice", 30),
			new Person("Bob", 25),
			new Person("Carol", 35)
		);
		var json5l = Json5l.of(list);
		var lines = json5l.split("\n");
		assertEquals(3, lines.length);
		assertTrue(lines[0].contains("\"name\":\"Alice\""));
		assertTrue(lines[0].contains("\"age\":30"));
		assertTrue(lines[1].contains("\"name\":\"Bob\""));
		assertTrue(lines[2].contains("\"name\":\"Carol\""));
		assertFalse(json5l.contains("["));
		assertFalse(json5l.contains("]"));
	}

	@Test
	void a02_serializeArray() throws Exception {
		var arr = new Person[]{new Person("Alice", 30), new Person("Bob", 25)};
		var json5l = Json5l.of(arr);
		var lines = json5l.split("\n");
		assertEquals(2, lines.length);
		assertTrue(lines[0].contains("\"Alice\""));
		assertTrue(lines[1].contains("\"Bob\""));
	}

	@Test
	void a03_serializeSingleBean() throws Exception {
		var p = new Person("Alice", 30);
		var json5l = Json5l.of(p);
		assertEquals(1, json5l.split("\n").length);
		assertTrue(json5l.contains("\"name\":\"Alice\""));
		assertTrue(json5l.contains("\"age\":30"));
	}

	@Test
	void a04_serializeCollectionOfStrings() throws Exception {
		var list = list("foo", "bar", "baz");
		var json5l = Json5l.of(list);
		var lines = json5l.split("\n");
		assertEquals(3, lines.length);
		assertTrue(lines[0].contains("\"foo\""));
		assertTrue(lines[1].contains("\"bar\""));
		assertTrue(lines[2].contains("\"baz\""));
	}

	@Test
	void a05_serializeEmptyCollection() throws Exception {
		var json5l = Json5l.of(list());
		assertEquals("", json5l.trim());
	}

	@Test
	void a06_serializeNullValues() throws Exception {
		var list = list("a", null, "c");
		var json5l = Json5l.of(list);
		var lines = json5l.split("\n");
		assertEquals(3, lines.length);
		assertEquals("null", lines[1]);
	}

	@Test
	void a07_serializeNestedObjects() throws Exception {
		var outer = JsonMap.of("name", "Alice", "inner", JsonMap.of("x", 1, "y", 2));
		var json5l = Json5l.of(outer);
		assertTrue(json5l.contains("\"inner\":{\"x\":1,\"y\":2}"));
	}

	// =================================================================================
	// B. Default output is byte-identical to JSONL (sugar off)
	// =================================================================================

	@Test
	void b01_defaultIsByteIdenticalToJsonl() throws Exception {
		var list = list(
			new Person("Alice", 30),
			new Person("Bob", 25)
		);
		assertEquals(Jsonl.of(list), Json5l.of(list));
	}

	@Test
	void b02_defaultProducesStrictDoubleQuotes() throws Exception {
		var json5l = Json5l.of(JsonMap.of("name", "Alice"));
		assertTrue(json5l.contains("\"name\":\"Alice\""));
		assertFalse(json5l.contains("'"));
	}

	// =================================================================================
	// C. json5Sugar opt-in
	// =================================================================================

	@Test
	void c01_sugarUsesSingleQuotesAndUnquotedKeys() throws Exception {
		var s = Json5lSerializer.create().json5Sugar().build();
		var out = s.serialize(JsonMap.of("name", "Alice", "age", 30));
		assertTrue(out.contains("name:'Alice'"), "Expected unquoted key + single quotes: " + out);
		assertTrue(out.contains("age:30"), "Expected unquoted key: " + out);
		assertFalse(out.contains("\""), "Expected no double quotes: " + out);
	}

	@Test
	void c02_sugarStillOneLinePerRecord() throws Exception {
		var s = Json5lSerializer.create().json5Sugar().build();
		var out = s.serialize(list(new Person("Alice", 30), new Person("Bob", 25)));
		var lines = out.split("\n");
		assertEquals(2, lines.length);
		assertTrue(lines[0].contains("name:'Alice'"));
		assertTrue(lines[1].contains("name:'Bob'"));
	}

	@Test
	void c03_sugarFlagIsReflectedOnContext() {
		assertFalse(Json5lSerializer.DEFAULT.isJson5Sugar());
		assertTrue(Json5lSerializer.create().json5Sugar().build().isJson5Sugar());
	}

	@Test
	void c04_sugarAndStrictAreDistinctCachedInstances() {
		var strict = Json5lSerializer.create().build();
		var sugar = Json5lSerializer.create().json5Sugar().build();
		assertNotSame(strict, sugar);
		assertFalse(strict.isJson5Sugar());
		assertTrue(sugar.isJson5Sugar());
	}

	@Test
	void c05_copyPreservesSugar() {
		var sugar = Json5lSerializer.create().json5Sugar().build();
		assertTrue(sugar.copy().build().isJson5Sugar());
	}
}
