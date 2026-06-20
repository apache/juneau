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
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Json5lParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<Person>/List<JsonMap>/List<String> in tests
})
class Json5lParser_Test extends TestBase {

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
	// A. Strict JSONL input (JSON5 is a superset of JSON)
	// =================================================================================

	@Test
	void a01_parseStrictJsonlToListOfBeans() throws Exception {
		var in = "{\"name\":\"Alice\",\"age\":30}\n{\"name\":\"Bob\",\"age\":25}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void a02_parseStrictSingleLine() throws Exception {
		var p = Json5l.DEFAULT.to("{\"name\":\"Alice\",\"age\":30}", Person.class);
		assertBean(p, "name,age", "Alice,30");
	}

	// =================================================================================
	// B. JSON5 dialect, one document per line
	// =================================================================================

	@Test
	void b01_unquotedKeysAndSingleQuotes() throws Exception {
		var in = "{name:'Alice',age:30}\n{name:'Bob',age:25}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void b02_trailingCommas() throws Exception {
		var in = "{name:'Alice',age:30,}\n{name:'Bob',age:25,}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void b03_mixedStrictAndSugarLines() throws Exception {
		var in = "{name:'Alice',age:30}\n{\"name\":\"Bob\",\"age\":25}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	// =================================================================================
	// C. Comment handling
	// =================================================================================

	@Test
	void c01_commentOnlyLineSkipped() throws Exception {
		var in = "// header comment\n{name:'Alice',age:30}\n{name:'Bob',age:25}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name},1{name}", "{Alice},{Bob}");
	}

	@Test
	void c02_blockCommentOnlyLineSkipped() throws Exception {
		var in = "/* block comment */\n{name:'Alice',age:30}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name}", "{Alice}");
	}

	@Test
	void c03_inlineTrailingLineComment() throws Exception {
		var in = "{name:'Alice',age:30} // trailing\n{name:'Bob',age:25}";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name},1{name}", "{Alice},{Bob}");
	}

	@Test
	void c04_blankAndCommentLinesInterspersed() throws Exception {
		var in = "\n// one\n{name:'Alice'}\n\n/* two */\n{name:'Bob'}\n";
		var list = (List<Person>) Json5l.DEFAULT.to(in, List.class, Person.class);
		assertBean(list, "0{name},1{name}", "{Alice},{Bob}");
	}

	@Test
	void c05_commentOnlySingleObjectTarget() throws Exception {
		var in = "// nothing but a comment first\n{name:'Alice',age:30}";
		var p = Json5l.DEFAULT.to(in, Person.class);
		assertBean(p, "name,age", "Alice,30");
	}

	// =================================================================================
	// D. Empty / blank inputs
	// =================================================================================

	@Test
	void d01_parseEmptyInput() throws Exception {
		var list = (List<?>) Json5l.DEFAULT.to("", List.class, Person.class);
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	void d02_parseCommentOnlyInputToList() throws Exception {
		var list = (List<?>) Json5l.DEFAULT.to("// only a comment\n\n", List.class, Person.class);
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	void d03_parseToListOfStrings() throws Exception {
		var in = "'foo'\n'bar'\n'baz'";
		var list = (List<String>) Json5l.DEFAULT.to(in, List.class, String.class);
		assertEquals(list("foo", "bar", "baz"), list);
	}
}
