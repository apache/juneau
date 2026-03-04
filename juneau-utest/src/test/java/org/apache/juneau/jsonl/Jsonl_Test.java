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
package org.apache.juneau.jsonl;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link JsonlSerializer}.
 */
class Jsonl_Test extends TestBase {

	@Bean(properties = "name,age")
	public static class Person {
		public String name;
		public int age;

		public Person() {}
		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}
	}

	@Test
	void a01_serializeCollectionOfBeans() throws Exception {
		var list = list(
			new Person("Alice", 30),
			new Person("Bob", 25),
			new Person("Carol", 35)
		);
		var jsonl = Jsonl.of(list);
		var lines = jsonl.split("\n");
		assertEquals(3, lines.length);
		assertTrue(lines[0].contains("\"name\":\"Alice\""));
		assertTrue(lines[0].contains("\"age\":30"));
		assertTrue(lines[1].contains("\"name\":\"Bob\""));
		assertTrue(lines[2].contains("\"name\":\"Carol\""));
		assertFalse(jsonl.contains("["));
		assertFalse(jsonl.contains("]"));
	}

	@Test
	void a02_serializeArray() throws Exception {
		var arr = new Person[]{new Person("Alice", 30), new Person("Bob", 25)};
		var jsonl = Jsonl.of(arr);
		var lines = jsonl.split("\n");
		assertEquals(2, lines.length);
		assertTrue(lines[0].contains("\"Alice\""));
		assertTrue(lines[1].contains("\"Bob\""));
	}

	@Test
	void a03_serializeSingleBean() throws Exception {
		var p = new Person("Alice", 30);
		var jsonl = Jsonl.of(p);
		assertEquals(1, jsonl.split("\n").length);
		assertTrue(jsonl.contains("\"name\":\"Alice\""));
		assertTrue(jsonl.contains("\"age\":30"));
	}

	@Test
	void a04_serializeCollectionOfMaps() throws Exception {
		var list = list(
			JsonMap.of("a", 1, "b", 2),
			JsonMap.of("x", "y")
		);
		var jsonl = Jsonl.of(list);
		var lines = jsonl.split("\n");
		assertEquals(2, lines.length);
		assertTrue(lines[0].contains("\"a\":1"));
		assertTrue(lines[1].contains("\"x\":\"y\""));
	}

	@Test
	void a05_serializeCollectionOfStrings() throws Exception {
		var list = list("foo", "bar", "baz");
		var jsonl = Jsonl.of(list);
		var lines = jsonl.split("\n");
		assertEquals(3, lines.length);
		assertTrue(lines[0].contains("\"foo\""));
		assertTrue(lines[1].contains("\"bar\""));
		assertTrue(lines[2].contains("\"baz\""));
	}

	@Test
	void a06_serializeCollectionOfNumbers() throws Exception {
		var list = list(1, 2, 3);
		var jsonl = Jsonl.of(list);
		var lines = jsonl.split("\n");
		assertEquals(3, lines.length);
		assertEquals("1", lines[0]);
		assertEquals("2", lines[1]);
		assertEquals("3", lines[2]);
	}

	@Test
	void a07_serializeEmptyCollection() throws Exception {
		var jsonl = Jsonl.of(list());
		assertEquals("", jsonl.trim());
	}

	@Test
	void a08_serializeNullValues() throws Exception {
		var list = list("a", null, "c");
		var jsonl = Jsonl.of(list);
		var lines = jsonl.split("\n");
		assertEquals(3, lines.length);
		assertEquals("null", lines[1]);
	}

	@Test
	void a09_serializeNestedObjects() throws Exception {
		var outer = JsonMap.of("name", "Alice", "inner", JsonMap.of("x", 1, "y", 2));
		var jsonl = Jsonl.of(outer);
		assertTrue(jsonl.contains("\"inner\":{\"x\":1,\"y\":2}"));
	}

	@Test
	void a10_noTrailingComma() throws Exception {
		var list = list(new Person("A", 1), new Person("B", 2));
		var jsonl = Jsonl.of(list);
		assertFalse(jsonl.contains(",,"));
		assertFalse(jsonl.contains("\n,"));
	}

	@Test
	void a11_noWhitespaceBetweenLines() throws Exception {
		var list = list(new Person("A", 1), new Person("B", 2));
		var jsonl = Jsonl.of(list);
		var lines = jsonl.split("\n");
		for (var line : lines) {
			if (!line.isEmpty()) {
				assertFalse(line.startsWith(" "), "Line should not start with space: " + line);
				assertFalse(line.contains("\n"), "Line should not contain newline");
			}
		}
	}

	@Test
	void a12_serializeWithSwaps() throws Exception {
		var s = (JsonlSerializer) JsonlSerializer.create().swaps(ByteArraySwap.Base64.class).build();
		var list = list(JsonMap.of("data", new byte[]{1, 2, 3}));
		var jsonl = s.serialize(list);
		assertTrue(jsonl.contains("AQID"));
	}
}
