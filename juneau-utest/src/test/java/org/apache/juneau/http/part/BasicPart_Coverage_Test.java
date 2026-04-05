// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                               *
// ***************************************************************************************************************************
package org.apache.juneau.http.part;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.function.*;

import org.junit.jupiter.api.*;

/**
 * Coverage tests for BasicLongPart, BasicUriPart, BasicStringPart, BasicCsvArrayPart.
 */
public class BasicPart_Coverage_Test {

	//------------------------------------------------------------------------------------------------------------------
	// BasicLongPart
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basicLongPart_factoryNullReturns() {
		assertNull(BasicLongPart.of("Foo", (Long)null));
		assertNull(BasicLongPart.of(null, 1L));
		assertNull(BasicLongPart.of("Foo", (Supplier<Long>)null));
		assertNull(BasicLongPart.of(null, (Supplier<Long>)() -> 1L));
	}

	@Test void a02_basicLongPart_constructors() {
		var p1 = new BasicLongPart("Foo", 42L);
		assertEquals("42", p1.getValue());
		assertEquals(42L, p1.toLong());
		assertEquals(42L, p1.orElse(0L));

		var p2 = new BasicLongPart("Foo", "99");
		assertEquals("99", p2.getValue());
		assertEquals(99L, p2.toLong());

		var p3 = new BasicLongPart("Foo", (String)null);
		assertNull(p3.toLong());
		assertNull(p3.orElse(null));

		var p4 = new BasicLongPart("Foo", (Supplier<Long>)() -> 77L);
		assertEquals("77", p4.getValue());
		assertEquals(77L, p4.toLong());
	}

	@Test void a03_basicLongPart_orElse_whenNull() {
		var p = new BasicLongPart("Foo", (String)null);
		assertEquals(5L, p.orElse(5L));
	}

	@Test void a04_basicLongPart_supplierNull() {
		var p = new BasicLongPart("Foo", (Supplier<Long>)() -> null);
		assertNull(p.toLong());
		assertEquals(3L, p.orElse(3L));
	}

	@Test void a05_basicLongPart_assertLong() {
		var p = new BasicLongPart("Foo", 42L);
		assertNotNull(p.assertLong());
		assertNotNull(p.asLong());
	}

	@Test void a06_basicLongPart_factory() {
		var p1 = BasicLongPart.of("Foo", 42L);
		assertNotNull(p1);
		assertEquals("42", p1.getValue());

		var p2 = BasicLongPart.of("Foo", (Supplier<Long>)() -> 10L);
		assertNotNull(p2);
		assertEquals("10", p2.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// BasicUriPart
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_basicUriPart_factoryNullReturns() {
		assertNull(BasicUriPart.of("Foo", (URI)null));
		assertNull(BasicUriPart.of(null, URI.create("http://example.com")));
		assertNull(BasicUriPart.of("Foo", (Supplier<URI>)null));
		assertNull(BasicUriPart.of(null, (Supplier<URI>)() -> URI.create("http://example.com")));
	}

	@Test void b02_basicUriPart_constructors() throws Exception {
		var uri = URI.create("http://example.com");
		var p1 = new BasicUriPart("Foo", uri);
		assertEquals("http://example.com", p1.getValue());
		assertEquals(uri, p1.toUri());
		assertEquals(uri, p1.orElse(null));

		var p2 = new BasicUriPart("Foo", "http://example.com");
		assertEquals("http://example.com", p2.getValue());
		assertEquals(uri, p2.toUri());

		var p3 = new BasicUriPart("Foo", (String)null);
		assertNull(p3.toUri());
		assertNull(p3.orElse(null));

		var p4 = new BasicUriPart("Foo", (Supplier<URI>)() -> uri);
		assertEquals("http://example.com", p4.getValue());
	}

	@Test void b03_basicUriPart_orElse_whenNull() {
		var p = new BasicUriPart("Foo", (String)null);
		var fallback = URI.create("http://fallback.com");
		assertEquals(fallback, p.orElse(fallback));
	}

	@Test void b04_basicUriPart_supplierNull() {
		var p = new BasicUriPart("Foo", (Supplier<URI>)() -> null);
		assertNull(p.toUri());
		var fallback = URI.create("http://fallback.com");
		assertEquals(fallback, p.orElse(fallback));
	}

	@Test void b05_basicUriPart_asUri() {
		var uri = URI.create("http://example.com");
		var p = new BasicUriPart("Foo", uri);
		assertTrue(p.asUri().isPresent());
		assertEquals(uri, p.asUri().get());
	}

	@Test void b06_basicUriPart_factory() {
		var uri = URI.create("http://example.com");
		var p1 = BasicUriPart.of("Foo", uri);
		assertNotNull(p1);
		assertEquals(uri, p1.toUri());

		var p2 = BasicUriPart.of("Foo", (Supplier<URI>)() -> uri);
		assertNotNull(p2);
		assertEquals(uri, p2.toUri());
	}

	//------------------------------------------------------------------------------------------------------------------
	// BasicStringPart
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_basicStringPart_factoryNullReturns() {
		assertNull(BasicStringPart.of("Foo", (String)null));
		assertNull(BasicStringPart.of(null, "bar"));
		assertNull(BasicStringPart.of("Foo", (Supplier<String>)null));
		assertNull(BasicStringPart.of(null, (Supplier<String>)() -> "bar"));
	}

	@Test void c02_basicStringPart_orElse_whenNull() {
		var p = BasicStringPart.of("Foo", (String)null);
		assertNull(p); // null returned, so we construct directly
		var p2 = new BasicStringPart("Foo", (String)null);
		assertEquals("fallback", p2.orElse("fallback"));
	}

	@Test void c03_basicStringPart_supplier() {
		var p = BasicStringPart.of("Foo", (Supplier<String>)() -> "hello");
		assertNotNull(p);
		assertEquals("hello", p.getValue());
	}

	@Test void c04_basicStringPart_supplierNull() {
		var p = new BasicStringPart("Foo", (Supplier<String>)() -> null);
		assertNull(p.getValue());
		assertEquals("fallback", p.orElse("fallback"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// BasicCsvArrayPart
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_basicCsvArrayPart_factoryNullReturns() {
		assertNull(BasicCsvArrayPart.of("Foo", (String[])null));
		assertNull(BasicCsvArrayPart.of(null, new String[]{"a"}));
		assertNull(BasicCsvArrayPart.of("Foo", (Supplier<String[]>)null));
		assertNull(BasicCsvArrayPart.of(null, (Supplier<String[]>)() -> new String[]{"a"}));
	}

	@Test void d02_basicCsvArrayPart_factory() {
		var p = BasicCsvArrayPart.of("Foo", "a", "b", "c");
		assertNotNull(p);
		assertEquals("a,b,c", p.getValue());
		assertTrue(p.contains("a"));
		assertTrue(p.containsIgnoreCase("B"));
		assertFalse(p.contains("x"));
	}

	@Test void d03_basicCsvArrayPart_orElse() {
		var p = BasicCsvArrayPart.of("Foo", (Supplier<String[]>)() -> null);
		assertNotNull(p);
		// When supplier returns null, value() returns EMPTY array, not null, so orElse returns EMPTY
		assertArrayEquals(new String[]{}, p.orElse(new String[]{"x", "y"}));
	}

	@Test void d04_basicCsvArrayPart_supplierNonNull() {
		var p = BasicCsvArrayPart.of("Foo", (Supplier<String[]>)() -> new String[]{"a", "b"});
		assertNotNull(p);
		assertEquals("a,b", p.getValue());
		assertArrayEquals(new String[]{"a", "b"}, p.orElse(null));
	}

	@Test void d05_basicCsvArrayPart_constructors() {
		var p1 = new BasicCsvArrayPart("Foo", "a,b,c");
		assertEquals("a,b,c", p1.getValue());

		var p2 = new BasicCsvArrayPart("Foo", (String)null);
		assertNull(p2.getValue());

		var p3 = new BasicCsvArrayPart("Foo", new String[]{"x", "y"});
		assertEquals("x,y", p3.getValue());
	}
}
