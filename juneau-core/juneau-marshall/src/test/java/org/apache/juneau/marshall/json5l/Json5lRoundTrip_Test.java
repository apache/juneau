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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@link Json5lSerializer} / {@link Json5lParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<Person>/List<JsonMap> in tests
})
class Json5lRoundTrip_Test extends TestBase {

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
	void a01_strictRoundTrip() throws Exception {
		var in = list(new Person("Alice", 30), new Person("Bob", 25));
		var out = Json5lSerializer.DEFAULT.write(in);
		var back = (List<Person>) Json5lParser.DEFAULT.read(out, List.class, Person.class);
		assertBean(back, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void a02_sugarRoundTrip() throws Exception {
		var s = Json5lSerializer.create().json5Sugar().build();
		var in = list(new Person("Alice", 30), new Person("Bob", 25));
		var out = s.write(in);
		// Sugar output is single-quoted / unquoted-key; the parser reads it back fine.
		assertTrue(out.contains("name:'Alice'"));
		var back = (List<Person>) Json5lParser.DEFAULT.read(out, List.class, Person.class);
		assertBean(back, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void a03_mapRoundTrip() throws Exception {
		var in = list(JsonMap.of("x", 1), JsonMap.of("y", 2));
		var out = Json5lSerializer.DEFAULT.write(in);
		var back = (List<JsonMap>) Json5lParser.DEFAULT.read(out, List.class, JsonMap.class);
		assertEquals(2, back.size());
		assertEquals(1, back.get(0).getInt("x"));
		assertEquals(2, back.get(1).getInt("y"));
	}
}
