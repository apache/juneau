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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpCsvHeader_Test extends TestBase {

	private static final String NAME = "Allow";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpCsvHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpCsvHeader.of(NAME, "GET, POST");
		assertEquals(NAME, h.getName());
		assertEquals("GET, POST", h.getValue());
		assertArrayEquals(new String[]{"GET","POST"}, h.toArray());
		assertEquals(java.util.List.of("GET","POST"), h.toList());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpCsvHeader.of(NAME, (String)null);
		assertNull(h.toArray());
		assertNull(h.getValue());
		assertTrue(h.asArray().isEmpty());
		assertTrue(h.asList().isEmpty());
	}

	@Test void a03_of_typed_values() {
		var h = HttpCsvHeader.of(NAME, "GET", "POST");
		assertEquals("GET, POST", h.getValue());
		assertArrayEquals(new String[]{"GET","POST"}, h.toArray());
		assertArrayEquals(new String[]{"GET","POST"}, h.asArray().get());
	}

	@Test void a04_of_typed_null() {
		var h = HttpCsvHeader.of(NAME, (String[])null);
		assertNull(h.toArray());
		assertNull(h.getValue());
	}

	@Test void a05_of_typed_empty() {
		var h = HttpCsvHeader.of(NAME);
		assertEquals("", h.getValue());
		assertArrayEquals(new String[0], h.toArray());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_contains() {
		var h = HttpCsvHeader.of(NAME, "GET", "POST");
		assertTrue(h.contains("GET"));
		assertFalse(h.contains("get"));
		assertFalse(h.contains("PUT"));
	}

	@Test void b02_contains_unset() {
		assertFalse(HttpCsvHeader.of(NAME, (String)null).contains("GET"));
	}

	@Test void b03_containsIgnoreCase() {
		var h = HttpCsvHeader.of(NAME, "GET", "POST");
		assertTrue(h.containsIgnoreCase("get"));
		assertFalse(h.containsIgnoreCase("put"));
	}

	@Test void b04_containsIgnoreCase_unset() {
		assertFalse(HttpCsvHeader.of(NAME, (String)null).containsIgnoreCase("GET"));
	}

	@Test void b05_orElse_present() {
		assertArrayEquals(new String[]{"GET"}, HttpCsvHeader.of(NAME, "GET").orElse(new String[]{"POST"}));
	}

	@Test void b06_orElse_absent() {
		assertArrayEquals(new String[]{"POST"}, HttpCsvHeader.of(NAME, (String)null).orElse(new String[]{"POST"}));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "GET, POST", HttpCsvHeader.LAZY_WIRE_STRING);
		assertEquals("GET, POST", h.getValue());
		assertArrayEquals(new String[]{"GET","POST"}, h.toArray());
	}

	@Test void c02_lazy_tokens() {
		var h = new Sub(NAME, (Supplier<String[]>) () -> new String[]{"GET","POST"}, HttpCsvHeader.LAZY_TOKENS);
		assertArrayEquals(new String[]{"GET","POST"}, h.toArray());
	}
}
