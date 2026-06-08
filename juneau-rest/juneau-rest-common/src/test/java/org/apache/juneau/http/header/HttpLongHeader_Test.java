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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpLongHeader_Test extends TestBase {

	private static final String NAME = "X-Size";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpLongHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpLongHeader.of(NAME, "9999999999");
		assertEquals(NAME, h.getName());
		assertEquals("9999999999", h.getValue());
		assertEquals(Long.valueOf(9999999999L), h.toLong());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpLongHeader.of(NAME, (String)null);
		assertNull(h.toLong());
		assertNull(h.getValue());
	}

	@Test void a03_of_wire_badValue_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Value 'abc' could not be parsed as a long.", () -> HttpLongHeader.of(NAME, "abc"));
	}

	@Test void a04_of_typed_value() {
		var h = HttpLongHeader.of(NAME, Long.valueOf(7L));
		assertEquals("7", h.getValue());
		assertEquals(Long.valueOf(7L), h.toLong());
	}

	@Test void a05_of_typed_null() {
		var h = HttpLongHeader.of(NAME, (Long)null);
		assertNull(h.toLong());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asLong_present() {
		assertEquals(Long.valueOf(5L), HttpLongHeader.of(NAME, 5L).asLong().get());
	}

	@Test void b02_asLong_absent() {
		assertTrue(HttpLongHeader.of(NAME, (Long)null).asLong().isEmpty());
	}

	@Test void b03_orElse_present() {
		assertEquals(Long.valueOf(5L), HttpLongHeader.of(NAME, 5L).orElse(99L));
	}

	@Test void b04_orElse_absent() {
		assertEquals(Long.valueOf(99L), HttpLongHeader.of(NAME, (Long)null).orElse(99L));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "42", HttpLongHeader.LAZY_WIRE_STRING);
		assertEquals("42", h.getValue());
		assertEquals(Long.valueOf(42L), h.toLong());
	}

	@Test void c02_lazy_long() {
		var h = new Sub(NAME, (Supplier<Long>) () -> 42L, HttpLongHeader.LAZY_LONG);
		assertEquals(Long.valueOf(42L), h.toLong());
	}

	@Test void c03_lazy_wireString_badValue_throws() {
		var h = new Sub(NAME, (Supplier<String>) () -> "abc", HttpLongHeader.LAZY_WIRE_STRING);
		assertThrowsWithMessage(IllegalArgumentException.class, "Value 'abc' could not be parsed as a long.", h::toLong);
	}
}
