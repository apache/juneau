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
package org.apache.juneau.marshall.jsonl;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link JsonlParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<Person>/List<JsonMap>/List<String> in tests
})
class JsonlParser_Test extends TestBase {

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

	@Test
	void a01_parseToListOfBeans() throws Exception {
		var jsonl = "{\"name\":\"Alice\",\"age\":30}\n{\"name\":\"Bob\",\"age\":25}\n{\"name\":\"Carol\",\"age\":35}";
		var list = (List<Person>) Jsonl.DEFAULT.to(jsonl, List.class, Person.class);
		assertBean(list, "0{name,age},1{name,age},2{name,age}", "{Alice,30},{Bob,25},{Carol,35}");
	}

	@Test
	void a02_parseToArray() throws Exception {
		var jsonl = "{\"name\":\"Alice\",\"age\":30}\n{\"name\":\"Bob\",\"age\":25}";
		var arr = Jsonl.DEFAULT.to(jsonl, Person[].class);
		assertBean(arr, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void a03_parseSingleLine() throws Exception {
		var jsonl = "{\"name\":\"Alice\",\"age\":30}";
		var p = Jsonl.DEFAULT.to(jsonl, Person.class);
		assertBean(p, "name,age", "Alice,30");
	}

	@Test
	void a04_parseWithEmptyLines() throws Exception {
		var jsonl = "{\"name\":\"Alice\"}\n\n\n{\"name\":\"Bob\"}";
		var list = (List<Person>) Jsonl.DEFAULT.to(jsonl, List.class, Person.class);
		assertBean(list, "0{name},1{name}", "{Alice},{Bob}");
	}

	@Test
	void a05_parseToListOfMaps() throws Exception {
		var jsonl = "{\"a\":1,\"b\":2}\n{\"x\":\"y\"}";
		var list = (List<JsonMap>) Jsonl.DEFAULT.to(jsonl, List.class, JsonMap.class);
		assertBean(list, "0{a,b},1{x}", "{1,2},{y}");
	}

	@Test
	void a06_parseToListOfStrings() throws Exception {
		var jsonl = "\"foo\"\n\"bar\"\n\"baz\"";
		var list = (List<String>) Jsonl.DEFAULT.to(jsonl, List.class, String.class);
		assertEquals(list("foo", "bar", "baz"), list);
	}

	@Test
	void a07_parseEmptyInput() throws Exception {
		var list = (List<?>) Jsonl.DEFAULT.to("", List.class, Person.class);
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	void a08_parseWithTrailingNewline() throws Exception {
		var jsonl = "{\"name\":\"Alice\"}\n";
		var list = (List<Person>) Jsonl.DEFAULT.to(jsonl, List.class, Person.class);
		assertBean(list, "0{name}", "{Alice}");
	}

	@Test
	void a09_parseNestedObjects() throws Exception {
		var jsonl = "{\"name\":\"Alice\",\"addr\":{\"city\":\"Boston\",\"zip\":\"02101\"}}";
		var list = (List<JsonMap>) Jsonl.DEFAULT.to(jsonl, List.class, JsonMap.class);
		assertBean(list, "0{name,addr{city,zip}}", "{Alice,{Boston,02101}}");
	}

	@Test
	void a10_parseWithSwaps() throws Exception {
		var p = (JsonlParser) JsonlParser.create().binaryFormat(BinaryFormat.BASE64).build();
		var jsonl = "{\"data\":\"AQID\"}\n{\"data\":\"BAUG\"}";
		var list = (List<JsonMap>) p.parse(jsonl, List.class, JsonMap.class);
		assertBean(list, "0{data},1{data}", "{AQID},{BAUG}");
	}

	@Test
	void a11_parseMalformedLine() {
		assertThrowsWithMessage(ParseException.class, "invalid", () ->
			Jsonl.DEFAULT.to("{\"a\":1}\n{invalid json}", List.class, JsonMap.class));
	}
}
