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
package org.apache.juneau.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CsvParser} and {@link CsvParserSession}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class CsvParser_Test extends TestBase {

	/** Convenience: parse CSV into List&lt;T&gt;. */
	private static <T> List<T> parseList(String csv, Class<T> elementType) throws Exception {
		return (List<T>) CsvParser.DEFAULT.parse(csv, List.class, elementType);
	}

	//====================================================================================================
	// a - Parse into collection of beans
	//====================================================================================================

	@Test void a01_parseBeanCollection() throws Exception {
		var csv = "b,c\nb1,1\nb2,2\n";
		var r = parseList(csv, A.class);
		assertEquals(2, r.size());
		assertEquals("b1", r.get(0).b);
		assertEquals(1, r.get(0).c);
		assertEquals("b2", r.get(1).b);
		assertEquals(2, r.get(1).c);
	}

	@Test void a02_parseSingleBean() throws Exception {
		var csv = "b,c\nhello,42\n";
		var r = CsvParser.DEFAULT.parse(csv, A.class);
		assertEquals("hello", r.b);
		assertEquals(42, r.c);
	}

	@Test void a03_parseBeanArray() throws Exception {
		var csv = "b,c\nfoo,10\nbar,20\n";
		var r = CsvParser.DEFAULT.parse(csv, A[].class);
		assertEquals(2, r.length);
		assertEquals("foo", r[0].b);
		assertEquals(10, r[0].c);
		assertEquals("bar", r[1].b);
		assertEquals(20, r[1].c);
	}

	public static class A {
		public String b;
		public int c;
	}

	//====================================================================================================
	// b - Parse into collection of maps
	//====================================================================================================

	@Test void b01_parseMapCollection() throws Exception {
		var csv = "name,value\nfoo,1\nbar,2\n";
		var r = parseList(csv, Map.class);
		assertEquals(2, r.size());
		assertEquals("foo", r.get(0).get("name"));
		assertEquals("1", r.get(0).get("value"));
		assertEquals("bar", r.get(1).get("name"));
		assertEquals("2", r.get(1).get("value"));
	}

	@Test void b02_parseSingleMap() throws Exception {
		var csv = "k1,k2\nv1,v2\n";
		var r = (Map<?, ?>) CsvParser.DEFAULT.parse(csv, Map.class);
		assertEquals("v1", r.get("k1"));
		assertEquals("v2", r.get("k2"));
	}

	//====================================================================================================
	// c - Parse simple value collections (single "value" column)
	//====================================================================================================

	@Test void c01_parseStringList() throws Exception {
		var csv = "value\nalpha\nbeta\ngamma\n";
		var r = parseList(csv, String.class);
		assertEquals(List.of("alpha", "beta", "gamma"), r);
	}

	@Test void c02_parseIntegerList() throws Exception {
		var csv = "value\n1\n2\n3\n";
		var r = parseList(csv, Integer.class);
		assertEquals(List.of(1, 2, 3), r);
	}

	@Test void c03_parseBooleanList() throws Exception {
		var csv = "value\ntrue\nfalse\ntrue\n";
		var r = parseList(csv, Boolean.class);
		assertEquals(List.of(true, false, true), r);
	}

	//====================================================================================================
	// d - Null and empty value handling
	//====================================================================================================

	@Test void d01_parseNullValues() throws Exception {
		var csv = "b,c\n<NULL>,1\nb2,<NULL>\n";
		var r = parseList(csv, B.class);
		assertEquals(2, r.size());
		assertNull(r.get(0).b);
		assertEquals(1, (int) r.get(0).c);
		assertEquals("b2", r.get(1).b);
		assertNull(r.get(1).c);
	}

	public static class B {
		public String b;
		public Integer c;
	}

	@Test void d02_parseEmptyInput() throws Exception {
		var r1 = CsvParser.DEFAULT.parse("", A.class);
		assertNull(r1);
	}

	@Test void d03_parseHeaderOnly() throws Exception {
		var csv = "b,c\n";
		var r = parseList(csv, A.class);
		assertTrue(r.isEmpty());
	}

	//====================================================================================================
	// e - Quoted field handling (RFC 4180)
	//====================================================================================================

	@Test void e01_parseQuotedComma() throws Exception {
		var csv = "value\n\"hello, world\"\n";
		var r = parseList(csv, String.class);
		assertEquals("hello, world", r.get(0));
	}

	@Test void e02_parseQuotedNewline() throws Exception {
		var csv = "value\n\"line1\nline2\"\n";
		var r = parseList(csv, String.class);
		assertEquals("line1\nline2", r.get(0));
	}

	@Test void e03_parseDoubledQuote() throws Exception {
		var csv = "value\n\"say \"\"hello\"\"\"\n";
		var r = parseList(csv, String.class);
		assertEquals("say \"hello\"", r.get(0));
	}

	//====================================================================================================
	// f - Enum values
	//====================================================================================================

	@Test void f01_parseEnumValues() throws Exception {
		var csv = "name,status\nTask1,PENDING\nTask2,COMPLETED\n";
		var r = parseList(csv, C.class);
		assertEquals(2, r.size());
		assertEquals("Task1", r.get(0).name);
		assertEquals(Status.PENDING, r.get(0).status);
		assertEquals("Task2", r.get(1).name);
		assertEquals(Status.COMPLETED, r.get(1).status);
	}

	public static class C {
		public String name;
		public Status status;
	}

	public enum Status { PENDING, IN_PROGRESS, COMPLETED }

	//====================================================================================================
	// g - Object (untyped) parsing
	//====================================================================================================

	@Test void g01_parseAsObject_multipleRows() throws Exception {
		var csv = "a,b\n1,2\n3,4\n";
		var r = CsvParser.DEFAULT.parse(csv, Object.class);
		assertInstanceOf(JsonList.class, r);
		assertEquals(2, ((JsonList) r).size());
	}

	@Test void g02_parseAsObject_singleRow() throws Exception {
		var csv = "a,b\n1,2\n";
		var r = CsvParser.DEFAULT.parse(csv, Object.class);
		assertInstanceOf(JsonMap.class, r);
		var m = (JsonMap) r;
		assertEquals("1", m.get("a"));
		assertEquals("2", m.get("b"));
	}

	//====================================================================================================
	// h - Mismatch: fewer fields than headers
	//====================================================================================================

	@Test void h01_fewerFieldsThanHeaders() throws Exception {
		// Row has fewer columns than header; missing fields are treated as null/default.
		var csv = "b,c\nhello\n";
		var r = parseList(csv, A.class);
		assertEquals(1, r.size());
		assertEquals("hello", r.get(0).b);
		assertEquals(0, r.get(0).c);
	}

	//====================================================================================================
	// i - Round-trip: serialize then parse
	//====================================================================================================

	@Test void i01_roundTripBeanList() throws Exception {
		var original = List.of(new D("alice", 30), new D("bob", 25));
		var csv = CsvSerializer.DEFAULT.serialize(original);
		var parsed = parseList(csv, D.class);
		assertEquals(2, parsed.size());
		assertEquals("alice", parsed.get(0).name);
		assertEquals(30, parsed.get(0).age);
		assertEquals("bob", parsed.get(1).name);
		assertEquals(25, parsed.get(1).age);
	}

	@Test void i02_roundTripStringList() throws Exception {
		var original = List.of("foo", "bar", "baz");
		var csv = CsvSerializer.DEFAULT.serialize(original);
		var parsed = parseList(csv, String.class);
		assertEquals(original, parsed);
	}

	@Test void i03_roundTripIntList() throws Exception {
		var original = List.of(1, 2, 3);
		var csv = CsvSerializer.DEFAULT.serialize(original);
		var parsed = parseList(csv, Integer.class);
		assertEquals(original, parsed);
	}

	public static class D {
		public String name;
		public int age;
		public D() {}
		public D(String name, int age) { this.name = name; this.age = age; }
	}

	//====================================================================================================
	// j - Bean annotations
	//====================================================================================================

	@Test void j01_parseWithBeanAnnotations() throws Exception {
		var csv = "full_name,years\nJohn,35\n";
		var r = parseList(csv, E.class);
		assertEquals(1, r.size());
		assertEquals("John", r.get(0).name);
		assertEquals(35, r.get(0).age);
	}

	public static class E {
		@Beanp(name = "full_name")
		public String name;

		@Beanp(name = "years")
		public int age;
	}

	//====================================================================================================
	// k - CRLF line endings
	//====================================================================================================

	@Test void k01_parseCrlfLineEndings() throws Exception {
		var csv = "b,c\r\nhello,1\r\nworld,2\r\n";
		var r = parseList(csv, A.class);
		assertEquals(2, r.size());
		assertEquals("hello", r.get(0).b);
		assertEquals("world", r.get(1).b);
	}

}
