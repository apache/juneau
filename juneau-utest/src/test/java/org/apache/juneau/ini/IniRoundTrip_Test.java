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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for {@link IniSerializer} and {@link IniParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to Map/bean in tests
})
class IniRoundTrip_Test extends TestBase {

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

	//====================================================================================================
	// a - Simple bean round-trip
	//====================================================================================================

	@Test
	void a01_simpleBeanRoundTrip() throws Exception {
		var a = new Person("Alice", 30);
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = IniParser.DEFAULT.parse(ini, Person.class);
		assertBean(b, "name,age", "Alice,30");
	}

	@Test
	void a02_nestedBeanRoundTrip() throws Exception {
		var a = new ComplexPerson("Alice", new Address("123 Main", "Boston"), list("a", "b", "c"));
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = IniParser.DEFAULT.parse(ini, ComplexPerson.class);
		assertBean(b, "name,address{street,city},tags", "Alice,{123 Main,Boston},[a,b,c]");
	}

	@Test
	void a03_mapRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "test");
		a.put("count", 42);
		a.put("flag", true);
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,count,flag", "test,42,true");
	}

	@Test
	void a04_nestedMapRoundTrip() throws Exception {
		var nested = new LinkedHashMap<String, Object>();
		nested.put("host", "localhost");
		nested.put("port", 8080);
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "app");
		a.put("database", nested);
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,database{host,port}", "app,{localhost,8080}");
	}

	@Test
	void a05_nullPropertiesRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "Alice");
		a.put("middle", null);
		var s = IniSerializer.create().keepNullProperties().build();
		var ini = s.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,middle", "Alice,<null>");
	}

	@Test
	void a06_collectionsRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "x");
		a.put("tags", list("a", "b", "c"));
		a.put("counts", list(1, 2, 3));
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,tags,counts", "x,[a,b,c],[1,2,3]");
	}

	@Test
	void a07_deepNestingRoundTrip() throws Exception {
		var company = new LinkedHashMap<String, Object>();
		company.put("name", "Acme");
		company.put("ticker", "ACME");
		var employment = new LinkedHashMap<String, Object>();
		employment.put("title", "Engineer");
		employment.put("company", company);
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "John");
		a.put("employment", employment);
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,employment{title,company{name,ticker}}", "John,{Engineer,{Acme,ACME}}");
	}

	@Test
	void a08_emptyCollectionsRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "x");
		a.put("tags", list());
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,tags", "x,[]");
	}

	@Test
	void a09_stringEdgeCases() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("empty", "");
		a.put("numeric", "123");
		a.put("boolean", "true");
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "empty,numeric,boolean", ",123,true");
	}

	@Test
	void a10_unicodeRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "José");
		a.put("emoji", "Hello \uD83D\uDE00");
		var ini = IniSerializer.DEFAULT.serialize(a);
		var b = (Map<String, Object>) IniParser.DEFAULT.parse(ini, Map.class, String.class, Object.class);
		assertBean(b, "name,emoji", "José,Hello \uD83D\uDE00");
	}
}
