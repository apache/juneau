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

		// Should have users and null values
		assertTrue(r.contains("user1"));
		assertTrue(r.contains("user2"));
		assertTrue(r.contains("user3"));
		assertTrue(r.contains("null"));
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
}