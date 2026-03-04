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
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@link JsonlSerializer} and {@link JsonlParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<Person>/List<ComplexPerson>/List<JsonMap>/List<Object>/List<String> in tests
})
class JsonlRoundTrip_Test extends TestBase {

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

	@Bean(properties = "name,address,tags")
	public static class ComplexPerson {
		public String name;
		public Address address;
		public List<String> tags;

		public ComplexPerson() {}
		public ComplexPerson(String name, Address address, List<String> tags) {
			this.name = name;
			this.address = address;
			this.tags = tags;
		}
	}

	@Bean(properties = "street,city")
	public static class Address {
		public String street;
		public String city;

		public Address() {}
		public Address(String street, String city) {
			this.street = street;
			this.city = city;
		}
	}

	@Test
	void a01_roundTripBeanCollection() throws Exception {
		var a = list(
			new Person("Alice", 30),
			new Person("Bob", 25),
			new Person("Carol", 35)
		);
		var jsonl = Jsonl.of(a);
		var b = (List<Person>) Jsonl.to(jsonl, List.class, Person.class);
		assertBean(b, "0{name,age},1{name,age},2{name,age}", "{Alice,30},{Bob,25},{Carol,35}");
	}

	@Test
	void a02_roundTripBeanArray() throws Exception {
		var a = new Person[]{new Person("Alice", 30), new Person("Bob", 25)};
		var jsonl = Jsonl.of(a);
		var b = Jsonl.to(jsonl, Person[].class);
		assertBean(b, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void a03_roundTripComplexBeans() throws Exception {
		var a = list(
			new ComplexPerson("Alice", new Address("123 Main", "Boston"), list("a", "b", "c")),
			new ComplexPerson("Bob", new Address("456 Oak", "Portland"), list("d", "e"))
		);
		var jsonl = Jsonl.of(a);
		var b = (List<ComplexPerson>) Jsonl.to(jsonl, List.class, ComplexPerson.class);
		assertBean(b, "0{name,address{street,city},tags},1{name,address{street,city},tags}",
			"{Alice,{123 Main,Boston},[a,b,c]},{Bob,{456 Oak,Portland},[d,e]}");
	}

	@Test
	void a04_roundTripWithNulls() throws Exception {
		var a = list("a", null, "c");
		var jsonl = Jsonl.of(a);
		var b = (List<String>) Jsonl.to(jsonl, List.class, String.class);
		assertBean(b, "0,1,2", "a,<null>,c");
	}

	@Test
	void a05_roundTripMixedTypes() throws Exception {
		var a = list(1, "two", true, null, 3.5);
		var jsonl = Jsonl.of(a);
		var b = (List<Object>) Jsonl.to(jsonl, List.class, Object.class);
		assertBean(b, "0,1,2,3,4", "1,two,true,<null>,3.5");
	}

	@Test
	void a06_roundTripWithSwaps() throws Exception {
		var s = (JsonlSerializer) JsonlSerializer.create().swaps(ByteArraySwap.Base64.class).build();
		var p = (JsonlParser) JsonlParser.create().swaps(ByteArraySwap.Base64.class).build();
		var m = new Jsonl(s, p);
		var a = list(
			JsonMap.of("name", "Alice", "data", new byte[]{1, 2, 3}),
			JsonMap.of("name", "Bob", "data", new byte[]{4, 5, 6})
		);
		var jsonl = m.write(a);
		var b = (List<JsonMap>) m.read(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{name},1{name}", "{Alice},{Bob}");
		assertTrue(jsonl.contains("AQID"));
		assertTrue(jsonl.contains("BAUG"));
	}

	@Test
	void a07_roundTripSingleObject() throws Exception {
		var a = new Person("Alice", 30);
		var jsonl = Jsonl.of(a);
		var b = Jsonl.to(jsonl, Person.class);
		assertBean(b, "name,age", "Alice,30");
	}

	@Test
	void a08_roundTripEmptyCollection() throws Exception {
		var a = list();
		var jsonl = Jsonl.of(a);
		var b = (List<?>) Jsonl.to(jsonl, List.class, Person.class);
		assertNotNull(b);
		assertTrue(b.isEmpty());
	}
}
