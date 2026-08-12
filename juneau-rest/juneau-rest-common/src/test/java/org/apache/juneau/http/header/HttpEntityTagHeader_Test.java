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

class HttpEntityTagHeader_Test extends TestBase {

	private static final String NAME = "ETag";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpEntityTagHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpEntityTagHeader.of(NAME, "\"foo\"");
		assertEquals(NAME, h.getName());
		assertEquals("\"foo\"", h.getValue());
		assertEquals(new EntityTag("\"foo\""), h.toEntityTag());
		assertEquals(new EntityTag("\"foo\""), h.asEntityTag().get());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpEntityTagHeader.of(NAME, (String)null);
		assertNull(h.toEntityTag());
		assertNull(h.getValue());
		assertTrue(h.asEntityTag().isEmpty());
	}

	@Test void a03_of_typed_value() {
		var h = HttpEntityTagHeader.of(NAME, new EntityTag("\"foo\""));
		assertEquals("\"foo\"", h.getValue());
		assertEquals(new EntityTag("\"foo\""), h.toEntityTag());
	}

	@Test void a04_of_typed_null() {
		var h = HttpEntityTagHeader.of(NAME, (EntityTag)null);
		assertNull(h.toEntityTag());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_orElse_present() {
		var t1 = new EntityTag("\"foo\"");
		var t2 = new EntityTag("\"bar\"");
		assertEquals(t1, HttpEntityTagHeader.of(NAME, t1).orElse(t2));
	}

	@Test void b02_orElse_absent() {
		var t2 = new EntityTag("\"bar\"");
		assertEquals(t2, HttpEntityTagHeader.of(NAME, (EntityTag)null).orElse(t2));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "\"foo\"", HttpEntityTagHeader.LAZY_WIRE_STRING);
		assertEquals("\"foo\"", h.getValue());
		assertEquals(new EntityTag("\"foo\""), h.toEntityTag());
	}

	@Test void c02_lazy_entityTag() {
		var t1 = new EntityTag("\"foo\"");
		var h = new Sub(NAME, (Supplier<EntityTag>) () -> t1, HttpEntityTagHeader.LAZY_ENTITY_TAG);
		assertEquals(t1, h.toEntityTag());
	}
}
