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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpIntegerHeader_Test extends TestBase {

	private static final String NAME = "X-Count";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpIntegerHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpIntegerHeader.of(NAME, "42");
		assertEquals(NAME, h.getName());
		assertEquals("42", h.getValue());
		assertEquals(Integer.valueOf(42), h.toInteger());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpIntegerHeader.of(NAME, (String)null);
		assertNull(h.toInteger());
		assertNull(h.getValue());
	}

	@Test void a03_of_wire_badValue_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Value 'abc' could not be parsed as an integer.", () -> HttpIntegerHeader.of(NAME, "abc"));
	}

	@Test void a04_of_typed_value() {
		var h = HttpIntegerHeader.of(NAME, Integer.valueOf(7));
		assertEquals("7", h.getValue());
		assertEquals(Integer.valueOf(7), h.toInteger());
	}

	@Test void a05_of_typed_null() {
		var h = HttpIntegerHeader.of(NAME, (Integer)null);
		assertNull(h.toInteger());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asInteger_present() {
		assertEquals(Integer.valueOf(5), HttpIntegerHeader.of(NAME, 5).asInteger().get());
	}

	@Test void b02_asInteger_absent() {
		assertTrue(HttpIntegerHeader.of(NAME, (Integer)null).asInteger().isEmpty());
	}

	@Test void b03_orElse_present() {
		assertEquals(Integer.valueOf(5), HttpIntegerHeader.of(NAME, 5).orElse(99));
	}

	@Test void b04_orElse_absent() {
		assertEquals(Integer.valueOf(99), HttpIntegerHeader.of(NAME, (Integer)null).orElse(99));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "42", HttpIntegerHeader.LAZY_WIRE_STRING);
		assertEquals("42", h.getValue());
		assertEquals(Integer.valueOf(42), h.toInteger());
	}

	@Test void c02_lazy_integer() {
		var h = new Sub(NAME, (Supplier<Integer>) () -> 42, HttpIntegerHeader.LAZY_INTEGER);
		assertEquals(Integer.valueOf(42), h.toInteger());
	}

	@Test void c03_lazy_wireString_badValue_throws() {
		var h = new Sub(NAME, (Supplier<String>) () -> "abc", HttpIntegerHeader.LAZY_WIRE_STRING);
		assertThrowsWithMessage(IllegalArgumentException.class, "Value 'abc' could not be parsed as an integer.", h::toInteger);
	}
}
