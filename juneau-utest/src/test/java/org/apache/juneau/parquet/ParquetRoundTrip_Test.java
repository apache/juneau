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
package org.apache.juneau.parquet;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for Parquet serialization.
 */
@SuppressWarnings({
	"unchecked" // Parser returns raw types; explicit casts required for typed assertions
})
class ParquetRoundTrip_Test extends TestBase {

	private enum TestEnum { FOO, BAR }

	@Test
	void a01_simpleBeanRoundTrip() throws Exception {
		var a = new ParquetSerializer_Test.SimpleBean();
		a.name = "Alice";
		a.age = 30;
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		var b = (List<ParquetSerializer_Test.SimpleBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.SimpleBean.class);
		assertBeans(b, "name,age", "Alice,30");
	}

	@Test
	void a02_collectionRoundTrip() throws Exception {
		var a = new ParquetSerializer_Test.SimpleBean();
		a.name = "a";
		a.age = 1;
		var b = new ParquetSerializer_Test.SimpleBean();
		b.name = "b";
		b.age = 2;
		var list = list(a, b);
		var bytes = ParquetSerializer.DEFAULT.serialize(list);
		var parsed = (List<ParquetSerializer_Test.SimpleBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.SimpleBean.class);
		assertBeans(parsed, "name,age", "a,1", "b,2");
	}

	@Test
	void a03_primitiveTypesRoundTrip() throws Exception {
		var a = new ParquetSerializer_Test.PrimitiveBean();
		a.i = -1;
		a.l = 999999L;
		a.d = 2.718;
		a.b = false;
		a.s = "world";
		var bytes = ParquetSerializer.DEFAULT.serialize(a);
		var parsed = (List<ParquetSerializer_Test.PrimitiveBean>) ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.PrimitiveBean.class);
		assertBeans(parsed, "i,l,d,b,s", "-1,999999,2.718,false,world");
	}

	@Test
	void a04_emptyRoundTrip() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var parsed = ParquetParser.DEFAULT.parse(bytes, List.class, ParquetSerializer_Test.SimpleBean.class);
		assertBeans(parsed, "name,age");
	}

	/** Bean with List&lt;String&gt; for nested list support. */
	public static class BeanWithList {
		public String name;
		public List<String> tags;
	}

	@Test
	void a05_beanWithListSerialization() throws Exception {
		var a = new BeanWithList();
		a.name = "Alice";
		a.tags = list("a", "b", "c");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(a));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a06_beanWithListRoundTrip() throws Exception {
		var a = new BeanWithList();
		a.name = "Alice";
		a.tags = list("a", "b", "c");
		var bytes = ParquetSerializer.DEFAULT.serialize(list(a));
		var parsed = (List<BeanWithList>) ParquetParser.DEFAULT.parse(bytes, List.class, BeanWithList.class);
		assertBeans(parsed, "name,tags", "Alice,[a,b,c]");
	}

	/** Record with List of beans for a03 parity. */
	public record Person(String name, int age) {}

	/** Record with List&lt;Person&gt; for a03 parity. */
	public record Team(String name, List<Person> members) {}

	@Test
	void a07a_schemaWithSampleForListOfBean() {
		var members = List.of(new Person("Alice", 30), new Person("Bob", 25));
		var in = new Team("devs", members);
		var bc = BeanContext.DEFAULT;
		var beanMap = bc.toBeanMap(in);
		var schema = new ParquetSchemaBuilder(bc, false, ParquetCycleHandling.NULL).buildSchema(bc.getClassMeta(Team.class), beanMap);
		var leaves = ParquetSchemaBuilder.getLeafColumns(schema);
		var leafPaths = leaves.stream().map(e -> e.path).sorted().toList();
		// With sample, expect root.name, root.members.list.element.name, root.members.list.element.age
		assertEquals(List.of("root.members.list.element.age", "root.members.list.element.name", "root.name"), leafPaths);
	}

	@Test
	void a07_recordWithListRoundTrip() throws Exception {
		var members = List.of(new Person("Alice", 30), new Person("Bob", 25));
		var in = new Team("devs", members);
		var ser = ParquetSerializer.create().addBeanTypes().build();
		var bytes = ser.serialize(in);
		var out = (Team) ParquetParser.DEFAULT.parse(bytes, Team.class);
		assertBean(out, "name,members{0{name,age},1{name,age}}", "devs,{{Alice,30},{Bob,25}}");
	}

	@Test
	void a08_mapWithNonStringKeysRoundTrip() throws Exception {
		var in = new java.util.TreeMap<Integer, String>();
		in.put(1, "a");
		in.put(2, "b");
		in.put(3, "c");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (java.util.Map<Integer, String>) ParquetParser.DEFAULT.parse(bytes, java.util.Map.class, Integer.class, String.class);
		assertEquals(3, out.size());
		assertEquals("a", out.get(1));
		assertEquals("b", out.get(2));
		assertEquals("c", out.get(3));
	}

	@Test
	void a09_scalarAndArrayUnwrap() throws Exception {
		// 2.1: Top-level scalar/array - parser unwraps {value: X} to X
		assertEquals("foobar", ParquetParser.DEFAULT.parse(ParquetSerializer.DEFAULT.serialize("foobar"), String.class));
		assertEquals(123, ParquetParser.DEFAULT.parse(ParquetSerializer.DEFAULT.serialize(123), Integer.class));
		assertEquals(123, ParquetParser.DEFAULT.parse(ParquetSerializer.DEFAULT.serialize(123), int.class).intValue());
		assertEquals(true, ParquetParser.DEFAULT.parse(ParquetSerializer.DEFAULT.serialize(true), Boolean.class));
		var intArr = ParquetParser.DEFAULT.parse(ParquetSerializer.DEFAULT.serialize(new int[]{1, 2, 3}), int[].class);
		assertArrayEquals(new int[]{1, 2, 3}, intArr);
		var strArr = ParquetParser.DEFAULT.parse(ParquetSerializer.DEFAULT.serialize(new String[]{"a", "b"}), String[].class);
		assertArrayEquals(new String[]{"a", "b"}, strArr);
	}

	@Test
	void a10_stringArrayWithNull() throws Exception {
		// 2.1: Arrays with null - ["foo", null, "null", ""]
		var in = new String[]{"foo", null, "null", ""};
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = ParquetParser.DEFAULT.parse(bytes, String[].class);
		assertArrayEquals(in, out);
	}

	@Test
	void a11_mapWithEnumKeysRoundTrip() throws Exception {
		// 2.7: Map<Enum,?> at root - enum keys serialized as name(), parser converts string back to enum
		var in = new java.util.LinkedHashMap<TestEnum, String>();
		in.put(TestEnum.FOO, "x");
		in.put(TestEnum.BAR, "y");
		var bytes = ParquetSerializer.DEFAULT.serialize(in);
		var out = (java.util.Map<TestEnum, String>) ParquetParser.DEFAULT.parse(bytes,
			java.util.Map.class, TestEnum.class, String.class);
		assertEquals(2, out.size());
		assertEquals("x", out.get(TestEnum.FOO));
		assertEquals("y", out.get(TestEnum.BAR));
	}

	/** Bean with nested Map&lt;Enum,Enum&gt; - 2.1 nested map key_value support. */
	public static class BeanWithNestedMap {
		public String name;
		public Map<TestEnum, TestEnum> statusMap;
	}

	@Test
	void a11_mapIntegerStringRoundTrip() throws Exception {
		// 2.2: Root Map with non-String keys
		var x = new TreeMap<Integer, String>();
		x.put(1, "a");
		x.put(2, null);
		var bytes = ParquetSerializer.DEFAULT.serialize(x);
		var parsed = (TreeMap<Integer, String>) ParquetParser.DEFAULT.parse(bytes, TreeMap.class, Integer.class, String.class);
		assertEquals("a", parsed.get(1));
		assertNull(parsed.get(2));
		assertEquals(2, parsed.size());
	}

	@Test
	void a11b_mapIntegerStringWithAddRootTypeRoundTrip() throws Exception {
		// 2.2: Root Map with addRootType (ValueHolder unwrap)
		var s = ParquetSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = ParquetParser.create().build();
		var x = new TreeMap<Integer, String>();
		x.put(1, "a");
		x.put(2, null);
		var bytes = s.serialize(x);
		var parsed = (TreeMap<Integer, String>) p.parse(bytes, TreeMap.class, Integer.class, String.class);
		assertEquals("a", parsed.get(1));
		assertNull(parsed.get(2));
		assertEquals(2, parsed.size());
	}

	@Test
	void a11c_mapDateStringWithRoundTripMapsConfig() throws Exception {
		// 2.1: Map<Date,String> with RoundTripMaps config (addBeanTypes, addRootType)
		var s = ParquetSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = ParquetParser.create().build();
		var xd1 = java.util.GregorianCalendar.from(java.time.ZonedDateTime.of(1901, 3, 3, 4, 5, 6, 0, java.time.ZoneId.systemDefault())).getTime();
		var xd2 = java.util.GregorianCalendar.from(java.time.ZonedDateTime.of(1902, 4, 4, 5, 6, 7, 0, java.time.ZoneId.systemDefault())).getTime();
		var x = new java.util.TreeMap<java.util.Date, String>();
		x.put(xd1, "a");
		x.put(xd2, null);
		var bytes = s.serialize(x);
		var parsed = (java.util.TreeMap<java.util.Date, String>) p.parse(bytes, java.util.TreeMap.class, java.util.Date.class, String.class);
		assertEquals("a", parsed.get(xd1));
		assertNull(parsed.get(xd2));
		assertEquals(2, parsed.size());
	}

	@Test
	void a11d_mapCalendarStringWithRoundTripMapsConfig() throws Exception {
		// 2.1: Map<Calendar,String> with RoundTripMaps config
		var s = ParquetSerializer.create().keepNullProperties().addBeanTypes().addRootType().build();
		var p = ParquetParser.create().build();
		var xc1 = java.util.GregorianCalendar.from(java.time.ZonedDateTime.parse("2012-12-21T12:34:56Z"));
		var xc2 = java.util.GregorianCalendar.from(java.time.ZonedDateTime.parse("2012-12-21T12:34:57Z"));
		var x = new java.util.TreeMap<java.util.Calendar, String>();
		x.put(xc1, "a");
		x.put(xc2, null);
		var bytes = s.serialize(x);
		var parsed = (java.util.TreeMap<java.util.Calendar, String>) p.parse(bytes, java.util.TreeMap.class, java.util.GregorianCalendar.class, String.class);
		assertEquals("a", parsed.get(xc1));
		assertNull(parsed.get(xc2));
		assertEquals(2, parsed.size());
	}

	@Test
	void a12_beanWithNestedMapRoundTrip() throws Exception {
		// 2.1: Nested Map in beans - key_value repeated group
		var empty = new BeanWithNestedMap();
		empty.name = "empty";
		empty.statusMap = map();
		var withEntries = new BeanWithNestedMap();
		withEntries.name = "withEntries";
		withEntries.statusMap = new java.util.LinkedHashMap<>();
		withEntries.statusMap.put(TestEnum.FOO, TestEnum.BAR);
		withEntries.statusMap.put(TestEnum.BAR, TestEnum.FOO);

		var bytesEmpty = ParquetSerializer.DEFAULT.serialize(list(empty));
		var parsedEmpty = (List<BeanWithNestedMap>) ParquetParser.DEFAULT.parse(bytesEmpty, List.class, BeanWithNestedMap.class);
		assertBeans(parsedEmpty, "name,statusMap", "empty,{}");

		var bytesWithEntries = ParquetSerializer.DEFAULT.serialize(list(withEntries));
		var parsedWithEntries = (List<BeanWithNestedMap>) ParquetParser.DEFAULT.parse(bytesWithEntries, List.class, BeanWithNestedMap.class);
		assertEquals(1, parsedWithEntries.size());
		assertEquals("withEntries", parsedWithEntries.get(0).name);
		var m = parsedWithEntries.get(0).statusMap;
		assertEquals(2, m.size());
		assertEquals(TestEnum.BAR, m.get(TestEnum.FOO));
		assertEquals(TestEnum.FOO, m.get(TestEnum.BAR));
	}
}
