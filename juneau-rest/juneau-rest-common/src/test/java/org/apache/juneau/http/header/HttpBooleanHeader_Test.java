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

class HttpBooleanHeader_Test extends TestBase {

	private static final String NAME = "X-Flag";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpBooleanHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_true() {
		var h = HttpBooleanHeader.of(NAME, "true");
		assertEquals(NAME, h.getName());
		assertEquals("true", h.getValue());
		assertTrue(h.isTrue());
		assertEquals(Boolean.TRUE, h.asBoolean().get());
	}

	@Test void a02_of_wire_false() {
		var h = HttpBooleanHeader.of(NAME, "false");
		assertEquals("false", h.getValue());
		assertFalse(h.isTrue());
	}

	@Test void a03_of_wire_nullValue() {
		var h = HttpBooleanHeader.of(NAME, (String)null);
		assertNull(h.toBoolean());
		assertNull(h.getValue());
		assertFalse(h.isTrue());
		assertTrue(h.asBoolean().isEmpty());
	}

	@Test void a04_of_typed_value() {
		var h = HttpBooleanHeader.of(NAME, Boolean.TRUE);
		assertEquals("true", h.getValue());
		assertEquals(Boolean.TRUE, h.toBoolean());
	}

	@Test void a05_of_typed_null() {
		var h = HttpBooleanHeader.of(NAME, (Boolean)null);
		assertNull(h.toBoolean());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_orElse_present() {
		assertEquals(Boolean.TRUE, HttpBooleanHeader.of(NAME, Boolean.TRUE).orElse(Boolean.FALSE));
	}

	@Test void b02_orElse_absent() {
		assertEquals(Boolean.FALSE, HttpBooleanHeader.of(NAME, (Boolean)null).orElse(Boolean.FALSE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "true", HttpBooleanHeader.LAZY_WIRE_STRING);
		assertEquals("true", h.getValue());
		assertTrue(h.isTrue());
	}

	@Test void c02_lazy_boolean() {
		var h = new Sub(NAME, (Supplier<Boolean>) () -> Boolean.TRUE, HttpBooleanHeader.LAZY_BOOLEAN);
		assertEquals(Boolean.TRUE, h.toBoolean());
	}
}
