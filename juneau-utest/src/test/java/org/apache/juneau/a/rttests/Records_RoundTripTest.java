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
package org.apache.juneau.a.rttests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class Records_RoundTripTest extends RoundTripTest_Base {

	//-----------------------------------------------------------------------------------------------------------------
	// Record definitions
	//-----------------------------------------------------------------------------------------------------------------

	public record Person(String name, int age) {}

	public record Team(String name, List<Person> members) {}

	public record Config(String id, Map<String,Object> settings) {}

	public record NullableRecord(String name, String nickname) {}

	public record EmptyRecord() {}

	public record WithArray(String label, int[] values) {}

	public enum Priority { LOW, MEDIUM, HIGH }

	public record WithEnum(String name, Priority priority) {}

	@Bean(properties="age,name")
	public record AnnotatedOrder(String name, int age) {}

	public record WithBeanp(@Beanp(name="fullName") String name, int age) {}

	public record WithCompactConstructor(String name, int age) {
		public WithCompactConstructor {
			if (name == null) name = "unknown";
			if (age < 0) age = 0;
		}
	}

	public record Nested(Person person, String role) {}

	public record WithNullValues(String required, String optional) {}

	@Bean(properties="name")
	public record WithBeanc(String name, int age) {
		@Beanc(properties="name")
		public WithBeanc(String name) {
			this(name, 0);
		}
	}

	public record Wrapper<T>(T value, String label) {}

	public interface Identifiable {
		String id();
	}

	public record IdentifiableRecord(String id, String data) implements Identifiable {}

	@Bean(dictionary={DogRecord.class, CatRecord.class})
	public interface AnimalRecord {}

	@Bean(typeName="dog")
	public record DogRecord(String name, String breed) implements AnimalRecord {}

	@Bean(typeName="cat")
	public record CatRecord(String name, int lives) implements AnimalRecord {}

	public record AnimalHolder(AnimalRecord animal) {}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic round-trip tests
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	void a01_simpleRecord(RoundTrip_Tester t) throws Exception {
		var in = new Person("John", 30);
		var out = t.roundTrip(in, Person.class);
		assertEquals("John", out.name());
		assertEquals(30, out.age());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_nestedRecord(RoundTrip_Tester t) throws Exception {
		var person = new Person("Jane", 25);
		var in = new Nested(person, "developer");
		var out = t.roundTrip(in, Nested.class);
		assertEquals("Jane", out.person().name());
		assertEquals(25, out.person().age());
		assertEquals("developer", out.role());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a03_recordWithList(RoundTrip_Tester t) throws Exception {
		var members = List.of(new Person("Alice", 30), new Person("Bob", 25));
		var in = new Team("devs", members);
		var out = t.roundTrip(in, Team.class);
		assertEquals("devs", out.name());
		assertEquals(2, out.members().size());
		assertEquals("Alice", out.members().get(0).name());
		assertEquals("Bob", out.members().get(1).name());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_recordWithMap(RoundTrip_Tester t) throws Exception {
		var settings = Map.<String,Object>of("key1", "val1", "key2", 42);
		var in = new Config("cfg1", settings);
		var out = t.roundTrip(in, Config.class);
		assertEquals("cfg1", out.id());
		assertEquals("val1", out.settings().get("key1"));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a05_recordWithNulls(RoundTrip_Tester t) throws Exception {
		var in = new NullableRecord("John", null);
		var out = t.roundTrip(in, NullableRecord.class);
		assertEquals("John", out.name());
		assertNull(out.nickname());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a06_emptyRecord(RoundTrip_Tester t) throws Exception {
		var in = new EmptyRecord();
		var out = t.roundTrip(in, EmptyRecord.class);
		assertNotNull(out);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a07_recordWithEnum(RoundTrip_Tester t) throws Exception {
		var in = new WithEnum("task1", Priority.HIGH);
		var out = t.roundTrip(in, WithEnum.class);
		assertEquals("task1", out.name());
		assertEquals(Priority.HIGH, out.priority());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation integration tests
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	void b01_beanPropertyOrder(RoundTrip_Tester t) throws Exception {
		var in = new AnnotatedOrder("John", 30);
		var out = t.roundTrip(in, AnnotatedOrder.class);
		assertEquals("John", out.name());
		assertEquals(30, out.age());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b02_beanpRename(RoundTrip_Tester t) throws Exception {
		var in = new WithBeanp("John", 30);
		var out = t.roundTrip(in, WithBeanp.class);
		assertEquals("John", out.name());
		assertEquals(30, out.age());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b03_beancCustomConstructor(RoundTrip_Tester t) throws Exception {
		var in = new WithBeanc("John", 0);
		var out = t.roundTrip(in, WithBeanc.class);
		assertEquals("John", out.name());
		assertEquals(0, out.age());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Type complexity tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b05_genericRecord() throws Exception {
		var in = new Wrapper<>("hello", "greeting");
		var json = Json5Serializer.DEFAULT.serialize(in);
		assertTrue(json.contains("hello"));
		assertTrue(json.contains("greeting"));
	}

	@Test
	void b06_recordWithArraySerialization() throws Exception {
		var in = new WithArray("data", new int[]{1, 2, 3});
		var json = Json5Serializer.DEFAULT.serialize(in);
		assertEquals("{label:'data',values:[1,2,3]}", json);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void b07_recordImplementingInterface(RoundTrip_Tester t) throws Exception {
		var in = new IdentifiableRecord("id1", "some data");
		var out = t.roundTrip(in, IdentifiableRecord.class);
		assertEquals("id1", out.id());
		assertEquals("some data", out.data());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Edge case tests
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("testers")
	void c01_compactConstructor(RoundTrip_Tester t) throws Exception {
		var in = new WithCompactConstructor("John", 30);
		var out = t.roundTrip(in, WithCompactConstructor.class);
		assertEquals("John", out.name());
		assertEquals(30, out.age());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void c02_recordInCollection(RoundTrip_Tester t) throws Exception {
		var in = List.of(new Person("Alice", 30), new Person("Bob", 25));
		var out = t.roundTrip(in);
		assertNotNull(out);
	}

	@Test
	void c03_recordAsMapValue() throws Exception {
		var map = Map.of("p1", new Person("Alice", 30), "p2", new Person("Bob", 25));
		var json = Json5Serializer.DEFAULT.serialize(map);
		assertTrue(json.contains("Alice"));
		assertTrue(json.contains("Bob"));
	}

	@Test
	void c04_beancNonCanonicalConstructor() throws Exception {
		var in = new WithBeanc("Jane", 0);
		var json = Json5Serializer.DEFAULT.serialize(in);
		assertTrue(json.contains("Jane"));
		var out = JsonParser.DEFAULT.parse("{\"name\":\"Jane\"}", WithBeanc.class);
		assertEquals("Jane", out.name());
		assertEquals(0, out.age());
	}

	@Test
	void c05_polymorphicRecordWithTypeName() throws Exception {
		var dog = new DogRecord("Rex", "Labrador");
		var s = Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build();
		var json = s.serialize(dog);
		assertTrue(json.contains("dog"));
		assertTrue(json.contains("Rex"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// JSON-specific serialization tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_jsonSerialization() throws Exception {
		var p = new Person("John", 30);
		var json = Json5Serializer.DEFAULT.serialize(p);
		assertEquals("{age:30,name:'John'}", json);
	}

	@Test
	void d02_jsonSerializationNested() throws Exception {
		var person = new Person("Jane", 25);
		var nested = new Nested(person, "developer");
		var json = Json5Serializer.DEFAULT.serialize(nested);
		assertEquals("{person:{age:25,name:'Jane'},role:'developer'}", json);
	}

	@Test
	void d03_jsonSerializationEmpty() throws Exception {
		var empty = new EmptyRecord();
		var json = Json5Serializer.DEFAULT.serialize(empty);
		assertEquals("{}", json);
	}

	@Test
	void d04_jsonSerializationWithEnum() throws Exception {
		var status = new WithEnum("task1", Priority.HIGH);
		var json = Json5Serializer.DEFAULT.serialize(status);
		assertEquals("{name:'task1',priority:'HIGH'}", json);
	}

	@Test
	void d05_jsonSerializationBeanpRename() throws Exception {
		var p = new WithBeanp("John", 30);
		var json = Json5Serializer.DEFAULT.serialize(p);
		assertEquals("{age:30,fullName:'John'}", json);
	}

	@Test
	void d06_jsonSerializationPropertyOrder() throws Exception {
		var p = new AnnotatedOrder("John", 30);
		var json = Json5Serializer.DEFAULT.serialize(p);
		assertEquals("{age:30,name:'John'}", json);
	}

	@Test
	void d07_jsonParsingRenamedProperty() throws Exception {
		var parsed = JsonParser.DEFAULT.parse("{\"fullName\":\"John\",\"age\":30}", WithBeanp.class);
		assertEquals("John", parsed.name());
		assertEquals(30, parsed.age());
	}
}
