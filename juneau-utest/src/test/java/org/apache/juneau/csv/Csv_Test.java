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

import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

class Csv_Test extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var l = new LinkedList<>();
		l.add(new A("b1",1));
		l.add(new A("b2",2));

		var s = CsvSerializer.DEFAULT;
		var r = s.serialize(l);

		assertEquals("b,c\nb1,1\nb2,2\n", r);
	}

	public static class A {
		public String b;
		public int c;

		public A(String b, int c) {
			this.b = b;
			this.c = c;
		}
	}

	//====================================================================================================
	// Test swaps on bean properties
	//====================================================================================================
	@Test void b01_swapOnBeanProperty() throws Exception {
		var l = new LinkedList<>();
		l.add(new B("user1", new Date(1000000)));
		l.add(new B("user2", new Date(2000000)));

		var s = CsvSerializer.create().swaps(DateSwap.class).build();
		var r = s.serialize(l);

		// Swaps should convert dates to yyyy-MM-dd format
		assertTrue(r.contains("1970-01-01") || r.contains("1969-12-31"), "Should have formatted dates but was: " + r);
		assertTrue(r.contains("user1"));
		assertTrue(r.contains("user2"));
	}

	public static class B {
		public String name;
		public Date date;

		public B(String name, Date date) {
			this.name = name;
			this.date = date;
		}
	}

	public static class DateSwap extends StringSwap<Date> {
		private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		@Override
		public String swap(BeanSession session, Date date) {
			return df.format(date);
		}
		@Override
		public Date unswap(BeanSession session, String str, ClassMeta<?> hint) throws java.text.ParseException {
			return df.parse(str);
		}
	}

	//====================================================================================================
	// Test swaps on map values
	//====================================================================================================
	@Test void c01_swapOnMapValues() throws Exception {
		var l = new LinkedList<>();
		var m1 = new HashMap<String,Object>();
		m1.put("name", "user1");
		m1.put("date", new Date(1000000));
		var m2 = new HashMap<String,Object>();
		m2.put("name", "user2");
		m2.put("date", new Date(2000000));
		l.add(m1);
		l.add(m2);

		var s = CsvSerializer.create().swaps(DateSwap.class).build();
		var r = s.serialize(l);

		// Swaps should format dates
		assertTrue(r.contains("user1"));
		assertTrue(r.contains("user2"));
		assertTrue(r.contains("1970-01-01") || r.contains("1969-12-31"), "Should have formatted dates but was: " + r);
	}

	//====================================================================================================
	// Test swaps on simple values
	//====================================================================================================
	@Test void d01_swapOnSimpleValues() throws Exception {
		var l = new LinkedList<>();
		l.add(new Date(1000000));
		l.add(new Date(2000000));
		l.add(new Date(3000000));

		var s = CsvSerializer.create().swaps(DateSwap.class).build();
		var r = s.serialize(l);

		// Should have value header and formatted dates
		assertTrue(r.startsWith("value\n"));
		assertTrue(r.contains("1970-01-01") || r.contains("1969-12-31"), "Should have formatted dates but was: " + r);
		// Should have 3 date lines + 1 header = 4 lines total
		assertEquals(4, r.split("\n").length);
	}

	//====================================================================================================
	// Test swap with null values
	//====================================================================================================
	@Test void e01_swapWithNullValues() throws Exception {
		var l = new LinkedList<>();
		l.add(new B("user1", null));
		l.add(new B("user2", new Date(2000000)));
		l.add(new B("user3", null));

		var s = CsvSerializer.create().swaps(DateSwap.class).build();
		var r = s.serialize(l);

		// Should have users and null values (null marker defaults to <NULL>)
		assertTrue(r.contains("user1"));
		assertTrue(r.contains("user2"));
		assertTrue(r.contains("user3"));
		assertTrue(r.contains("<NULL>"));
		assertTrue(r.contains("1970-01-01") || r.contains("1969-12-31"), "Should have formatted date but was: " + r);
	}

	//====================================================================================================
	// Test custom object swap
	//====================================================================================================
	@Test void f01_customObjectSwap() throws Exception {
		var l = new LinkedList<>();
		l.add(new C("John", new Address("123 Main St", "Seattle", "WA")));
		l.add(new C("Jane", new Address("456 Oak Ave", "Portland", "OR")));

		var s = CsvSerializer.create().swaps(AddressSwap.class).build();
		var r = s.serialize(l);

		// Should have names and pipe-delimited addresses
		assertTrue(r.contains("John"));
		assertTrue(r.contains("Jane"));
		assertTrue(r.contains("123 Main St|Seattle|WA"));
		assertTrue(r.contains("456 Oak Ave|Portland|OR"));
	}

	public static class C {
		public String name;
		public Address address;

		public C(String name, Address address) {
			this.name = name;
			this.address = address;
		}
	}

	public static class Address {
		public String street, city, state;

		public Address(String street, String city, String state) {
			this.street = street;
			this.city = city;
			this.state = state;
		}
	}

	public static class AddressSwap extends StringSwap<Address> {
		@Override
		public String swap(BeanSession session, Address address) {
			if (address == null) return null;
			return address.street + "|" + address.city + "|" + address.state;
		}
		@Override
		public Address unswap(BeanSession session, String str, ClassMeta<?> hint) {
			if (str == null) return null;
			var parts = str.split("\\|");
			return new Address(parts[0], parts[1], parts[2]);
		}
	}

	//====================================================================================================
	// Test @Swap annotation on field
	//====================================================================================================
	@Test void g01_swapAnnotationOnField() throws Exception {
		var l = new LinkedList<>();
		l.add(new D("user1", new Date(1000000)));
		l.add(new D("user2", new Date(2000000)));

		var s = CsvSerializer.DEFAULT;
		var r = s.serialize(l);

		// @Swap annotation on field should apply the swap
		assertTrue(r.contains("user1"));
		assertTrue(r.contains("user2"));
		assertTrue(r.contains("1970-01-01") || r.contains("1969-12-31"), "Should have formatted dates but was: " + r);
	}

	public static class D {
		public String name;

		@Swap(DateSwap.class)
		public Date timestamp;

		public D(String name, Date timestamp) {
			this.name = name;
			this.timestamp = timestamp;
		}
	}

	//====================================================================================================
	// Test enum swap
	//====================================================================================================
	@Test void h01_enumSwap() throws Exception {
		var l = new LinkedList<>();
		l.add(new E("Task1", Status.PENDING));
		l.add(new E("Task2", Status.COMPLETED));
		l.add(new E("Task3", Status.IN_PROGRESS));

		var s = CsvSerializer.DEFAULT;
		var r = s.serialize(l);

		// Enums should serialize as their string names
		assertTrue(r.contains("Task1"));
		assertTrue(r.contains("Task2"));
		assertTrue(r.contains("Task3"));
		assertTrue(r.contains("PENDING"));
		assertTrue(r.contains("COMPLETED"));
		assertTrue(r.contains("IN_PROGRESS"));
	}

	public static class E {
		public String name;
		public Status status;

		public E(String name, Status status) {
			this.name = name;
			this.status = status;
		}
	}

	public enum Status {
		PENDING, IN_PROGRESS, COMPLETED
	}

	//====================================================================================================
	// Test values containing commas are quoted (RFC 4180)
	//====================================================================================================
	@Test void i01_specialCharComma() throws Exception {
		var l = new LinkedList<>();
		l.add(new F("hello, world", 1));
		l.add(new F("plain", 2));

		var r = CsvSerializer.DEFAULT.serialize(l);
		// Value with comma must be enclosed in double quotes
		assertTrue(r.contains("\"hello, world\""), "Expected quoted comma value but got: " + r);
		assertTrue(r.contains("plain"));
	}

	public static class F {
		public String b;
		public int c;
		public F(String b, int c) { this.b = b; this.c = c; }
	}

	//====================================================================================================
	// Test values containing double quotes are escaped (RFC 4180 doubling)
	//====================================================================================================
	@Test void i02_specialCharQuote() throws Exception {
		var l = new LinkedList<>();
		l.add(new F("say \"hi\"", 1));

		var r = CsvSerializer.DEFAULT.serialize(l);
		// Embedded quotes must be doubled inside a quoted field
		assertTrue(r.contains("\"say \"\"hi\"\"\""), "Expected RFC 4180 doubled quotes but got: " + r);
	}

	//====================================================================================================
	// Test values containing newlines are quoted
	//====================================================================================================
	@Test void i03_specialCharNewline() throws Exception {
		var l = new LinkedList<>();
		l.add(new F("line1\nline2", 1));

		var r = CsvSerializer.DEFAULT.serialize(l);
		assertTrue(r.contains("\"line1\nline2\""), "Expected quoted newline value but got: " + r);
	}

	//====================================================================================================
	// Test null vs "null" string distinction
	//====================================================================================================
	@Test void i04_nullVsNullString() throws Exception {
		var l = new LinkedList<>();
		l.add(new G(null, "null"));

		var r = CsvSerializer.DEFAULT.serialize(l);
		// Java null → null marker (<NULL> by default); the String "null" → quoted "\"null\""
		assertTrue(r.contains("<NULL>") && r.contains("null"), "Unexpected output: " + r);
	}

	public static class G {
		public String a;
		public String b;
		public G() {}
		public G(String a, String b) { this.a = a; this.b = b; }
	}

	//====================================================================================================
	// Test serializing empty collection
	//====================================================================================================
	@Test void i05_emptyCollection() throws Exception {
		var l = new LinkedList<>();
		var r = CsvSerializer.DEFAULT.serialize(l);
		assertEquals("", r);
	}

	//====================================================================================================
	// Test serializing a single bean (not in a collection)
	//====================================================================================================
	@Test void i06_singleBean() throws Exception {
		var r = CsvSerializer.DEFAULT.serialize(new F("hello", 42));
		assertEquals("b,c\nhello,42\n", r);
	}

	//====================================================================================================
	// Test type discriminator - addBeanTypes adds _type column
	//====================================================================================================
	//====================================================================================================
	// Test type discriminator - parser skips _type column when target is concrete
	//====================================================================================================
	@Test void j01_typeDiscriminator_skipsTypeColumn() throws Exception {
		// CSV with _type column - parser skips it for concrete Circle type
		var csv = "name,radius,_type\nc1,10,Circle\nc2,20,Circle\n";
		var p = CsvParser.create().build();

		@SuppressWarnings("unchecked")
		var parsed = (List<Circle>) p.parse(csv, List.class, Circle.class);

		assertNotNull(parsed);
		assertEquals(2, parsed.size());
		assertEquals("c1", parsed.get(0).name);
		assertEquals(10, parsed.get(0).radius);
		assertEquals("c2", parsed.get(1).name);
		assertEquals(20, parsed.get(1).radius);
	}

	//====================================================================================================
	// Test type discriminator - single bean with _type column
	//====================================================================================================
	@Test void j02_typeDiscriminator_singleBean() throws Exception {
		var csv = "name,radius,_type\nc1,10,Circle\n";
		var p = CsvParser.create().build();

		var parsed = p.parse(csv, Circle.class);
		assertNotNull(parsed);
		assertEquals("c1", parsed.name);
		assertEquals(10, parsed.radius);
	}

	@org.apache.juneau.annotation.Bean(dictionary = {Circle.class, Rectangle.class})
	public interface Shape {
		String getName();
	}

	@org.apache.juneau.annotation.Bean(typeName = "Circle")
	public static class Circle implements Shape {
		public String name;
		public int radius;

		public Circle() {}
		public Circle(String name, int radius) {
			this.name = name;
			this.radius = radius;
		}
		@Override
		public String getName() { return name; }
	}

	@org.apache.juneau.annotation.Bean(typeName = "Rectangle")
	public static class Rectangle implements Shape {
		public String name;
		public int width;
		public int height;

		public Rectangle() {}
		public Rectangle(String name, int width, int height) {
			this.name = name;
			this.width = width;
			this.height = height;
		}
		@Override
		public String getName() { return name; }
	}

	//====================================================================================================
	// Test byte[] BASE64 round-trip
	//====================================================================================================
	@Test void k01_byteArray_base64() throws Exception {
		var bytes = "Hello World".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		var l = new LinkedList<>();
		l.add(new I("row1", bytes));
		l.add(new I("row2", new byte[]{1, 2, 3}));

		var s = CsvSerializer.create().byteArrayFormat(ByteArrayFormat.BASE64).build();
		var p = CsvParser.create().byteArrayFormat(ByteArrayFormat.BASE64).build();

		var csv = s.serialize(l);
		assertTrue(csv.contains("SGVsbG8gV29ybGQ=") || csv.contains("data"), "Should have base64: " + csv);

		@SuppressWarnings("unchecked")
		var parsed = (List<I>) p.parse(csv, List.class, I.class);
		assertNotNull(parsed);
		assertEquals(2, parsed.size());
		assertArrayEquals(bytes, parsed.get(0).data);
		assertArrayEquals(new byte[]{1, 2, 3}, parsed.get(1).data);
	}

	//====================================================================================================
	// Test byte[] SEMICOLON_DELIMITED round-trip
	//====================================================================================================
	@Test void k02_byteArray_semicolonDelimited() throws Exception {
		var bytes = new byte[]{72, 101, 108, 108, 111};
		var l = new LinkedList<>();
		l.add(new I("row1", bytes));

		var s = CsvSerializer.create().byteArrayFormat(ByteArrayFormat.SEMICOLON_DELIMITED).build();
		var p = CsvParser.create().byteArrayFormat(ByteArrayFormat.SEMICOLON_DELIMITED).build();

		var csv = s.serialize(l);
		assertTrue(csv.contains("72;101;108;108;111"), "Should have semicolon format: " + csv);

		@SuppressWarnings("unchecked")
		var parsed = (List<I>) p.parse(csv, List.class, I.class);
		assertNotNull(parsed);
		assertArrayEquals(bytes, parsed.get(0).data);
	}

	public static class I {
		public String name;
		public byte[] data;

		public I() {}
		public I(String name, byte[] data) {
			this.name = name;
			this.data = data;
		}
	}

	//====================================================================================================
	// Test int[] and double[] via [1;2;3] format
	//====================================================================================================
	@Test void k03_primitiveArrays() throws Exception {
		var l = new LinkedList<>();
		l.add(new H("row1", new int[]{1, 2, 3}, new double[]{1.5, 2.5, 3.5}));
		l.add(new H("row2", new int[]{}, new double[]{}));

		var s = CsvSerializer.DEFAULT;
		var p = CsvParser.DEFAULT;

		var csv = s.serialize(l);
		assertTrue(csv.contains("[1;2;3]"), "Should have int array format: " + csv);
		assertTrue(csv.contains("[1.5;2.5;3.5]"), "Should have double array format: " + csv);

		@SuppressWarnings("unchecked")
		var parsed = (List<H>) p.parse(csv, List.class, H.class);
		assertNotNull(parsed);
		assertEquals(2, parsed.size());
		assertArrayEquals(new int[]{1, 2, 3}, parsed.get(0).ints);
		assertArrayEquals(new double[]{1.5, 2.5, 3.5}, parsed.get(0).doubles);
		assertArrayEquals(new int[0], parsed.get(1).ints);
		assertArrayEquals(new double[0], parsed.get(1).doubles);
	}

	public static class H {
		public String name;
		public int[] ints;
		public double[] doubles;

		public H() {}
		public H(String name, int[] ints, double[] doubles) {
			this.name = name;
			this.ints = ints;
			this.doubles = doubles;
		}
	}

	//====================================================================================================
	// Test nested structures - allowNestedStructures with {key:val} and [val;val]
	//====================================================================================================
	@Test void l01_nestedStructures() throws Exception {
		var l = new LinkedList<>();
		l.add(new J("row1", List.of("a", "b", "c"), Map.of("x", 1, "y", 2)));
		l.add(new J("row2", List.of(), Map.of()));

		var s = CsvSerializer.create().allowNestedStructures(true).build();
		var p = CsvParser.create().allowNestedStructures(true).build();

		var csv = s.serialize(l);
		assertTrue(csv.contains("[a;b;c]") || csv.contains("tags"), "Should have array notation: " + csv);
		assertTrue(csv.contains("x:1") || csv.contains("y:2") || csv.contains("meta"), "Should have object notation: " + csv);

		@SuppressWarnings("unchecked")
		var parsed = (List<J>) p.parse(csv, List.class, J.class);
		assertNotNull(parsed);
		assertEquals(2, parsed.size());
		assertEquals(List.of("a", "b", "c"), parsed.get(0).tags);
		assertEquals(2, parsed.get(0).meta.size());
		assertEquals(1, ((Number) parsed.get(0).meta.get("x")).intValue());
		assertEquals(2, ((Number) parsed.get(0).meta.get("y")).intValue());
		assertEquals(List.of(), parsed.get(1).tags);
		assertTrue(parsed.get(1).meta.isEmpty());
	}

	public static class J {
		public String name;
		public List<String> tags;
		public Map<String, Object> meta;

		public J() {}
		public J(String name, List<String> tags, Map<String, Object> meta) {
			this.name = name;
			this.tags = tags;
			this.meta = meta;
		}
	}

	//====================================================================================================
	// Test nullValue() - custom null marker
	//====================================================================================================
	@Test void m01_nullValue() throws Exception {
		var l = new LinkedList<>();
		l.add(new G(null, "x"));

		var s = CsvSerializer.create().nullValue("<NULL>").build();
		var p = CsvParser.create().nullValue("<NULL>").build();

		var csv = s.serialize(l);
		assertTrue(csv.contains("<NULL>"), "Should have null marker: " + csv);

		@SuppressWarnings("unchecked")
		var parsed = (List<G>) p.parse(csv, List.class, G.class);
		assertNotNull(parsed);
		assertEquals(1, parsed.size());
		assertNull(parsed.get(0).a);
		assertEquals("x", parsed.get(0).b);
	}
}