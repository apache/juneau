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
package org.apache.juneau.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarkdownDocParser} and {@link MarkdownDocParserSession}.
 */
class MarkdownDocParser_Test {

	//====================================================================================================
	// a - Parse simple flat document to bean
	//====================================================================================================

	@Test void a01_parseSimpleFlatDoc_toBean() throws Exception {
		var md = """
			# Person

			| Property | Value |
			|---|---|
			| name | Alice |
			| age | 30 |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, A.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	@Test void a02_parseFlatDocNoTitle_toBean() throws Exception {
		// Document without a top-level heading should still parse the table
		var md = "| Property | Value |\n|---|---|\n| name | Bob |\n| age | 25 |";
		var r = MarkdownDocParser.DEFAULT.parse(md, A.class);
		assertEquals("Bob", r.name);
		assertEquals(25, r.age);
	}

	public static class A {
		public String name;
		public int age;
	}

	//====================================================================================================
	// b - Parse nested bean via sub-heading
	//====================================================================================================

	@Test void b01_parseNestedBeanViaSubHeading() throws Exception {
		var md = """
			# Person

			| Property | Value |
			|---|---|
			| name | Alice |
			| age | 30 |

			## address

			| Property | Value |
			|---|---|
			| city | Boston |
			| state | MA |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, B.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
		assertNotNull(r.address);
		assertEquals("Boston", r.address.city);
		assertEquals("MA", r.address.state);
	}

	public static class B {
		public String name;
		public int age;
		public Address address;
	}

	public static class Address {
		public String city;
		public String state;
	}

	//====================================================================================================
	// c - Parse collection under sub-heading
	//====================================================================================================

	@Test void c01_parseListOfStringsViaSubHeading() throws Exception {
		var md = """
			# Report

			| Property | Value |
			|---|---|
			| name | Summary |

			## tags

			- alpha
			- beta
			- gamma
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, C.class);
		assertEquals("Summary", r.name);
		assertNotNull(r.tags);
		assertEquals(List.of("alpha", "beta", "gamma"), r.tags);
	}

	@Test void c02_parseMultiColumnTableViaSubHeading() throws Exception {
		var md = """
			# Container

			## items

			| name | age |
			|---|---|
			| Alice | 30 |
			| Bob | 25 |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, D.class);
		assertNotNull(r.items);
		assertEquals(2, r.items.size());
		assertEquals("Alice", r.items.get(0).name);
		assertEquals(30, r.items.get(0).age);
		assertEquals("Bob", r.items.get(1).name);
		assertEquals(25, r.items.get(1).age);
	}

	public static class C {
		public String name;
		public List<String> tags;
	}

	public static class D {
		public List<A> items;
	}

	//====================================================================================================
	// d - Parse map document
	//====================================================================================================

	@Test
	@SuppressWarnings("unchecked")
	void d01_parseFlatDocToMap() throws Exception {
		var md = """
			# Data

			| Key | Value |
			|---|---|
			| k1 | v1 |
			| k2 | v2 |
			""";
		var r = (Map<String, String>) MarkdownDocParser.DEFAULT.parse(md, Map.class, String.class, String.class);
		assertEquals("v1", r.get("k1"));
		assertEquals("v2", r.get("k2"));
	}

	//====================================================================================================
	// e - Null handling
	//====================================================================================================

	@Test void e01_parseNullPropertyValue() throws Exception {
		var md = """
			# Data

			| Property | Value |
			|---|---|
			| name | *null* |
			| age | 30 |
			""";
		var r = MarkdownDocParser.DEFAULT.parse(md, E.class);
		assertNull(r.name);
		assertEquals(30, r.age);
	}

	public static class E {
		public String name;
		public Integer age;
	}

	//====================================================================================================
	// f - Custom heading level
	//====================================================================================================

	@Test void f01_customHeadingLevel() throws Exception {
		var md = """
			## Person

			| Property | Value |
			|---|---|
			| name | Alice |
			| age | 30 |
			""";
		var p = MarkdownDocParser.create().headingLevel(2).build();
		var r = p.parse(md, A.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// g - Round-trip: serialize then parse
	//====================================================================================================

	@Test void g01_roundTripFlatBean() throws Exception {
		var original = new A();
		original.name = "Alice";
		original.age = 30;
		var md = MarkdownDocSerializer.DEFAULT.serialize(original);
		var parsed = MarkdownDocParser.DEFAULT.parse(md, A.class);
		assertEquals("Alice", parsed.name);
		assertEquals(30, parsed.age);
	}

	@Test void g02_roundTripNestedBean() throws Exception {
		var original = new B();
		original.name = "Alice";
		original.age = 30;
		original.address = new Address();
		original.address.city = "Boston";
		original.address.state = "MA";
		var md = MarkdownDocSerializer.DEFAULT.serialize(original);
		var parsed = MarkdownDocParser.DEFAULT.parse(md, B.class);
		assertEquals("Alice", parsed.name);
		assertEquals(30, parsed.age);
		assertNotNull(parsed.address);
		assertEquals("Boston", parsed.address.city);
		assertEquals("MA", parsed.address.state);
	}

	@Test void g03_roundTripNestedBeanWithTitle() throws Exception {
		var original = new B();
		original.name = "Bob";
		original.age = 25;
		original.address = new Address();
		original.address.city = "Seattle";
		original.address.state = "WA";
		var s = MarkdownDocSerializer.create().title("Employee Report").build();
		var md = s.serialize(original);
		var parsed = MarkdownDocParser.DEFAULT.parse(md, B.class);
		assertEquals("Bob", parsed.name);
		assertEquals(25, parsed.age);
		assertNotNull(parsed.address);
		assertEquals("Seattle", parsed.address.city);
		assertEquals("WA", parsed.address.state);
	}
}
