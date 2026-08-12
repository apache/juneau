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

class HttpEntityTagsHeader_Test extends TestBase {

	private static final String NAME = "If-Match";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpEntityTagsHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpEntityTagsHeader.of(NAME, "\"foo\", \"bar\"");
		assertEquals(NAME, h.getName());
		assertEquals("\"foo\", \"bar\"", h.getValue());
		assertEquals(EntityTags.of("\"foo\", \"bar\""), h.toEntityTags());
		assertEquals(EntityTags.of("\"foo\", \"bar\""), h.asEntityTags().get());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpEntityTagsHeader.of(NAME, (String)null);
		assertNull(h.toEntityTags());
		assertNull(h.getValue());
		assertTrue(h.asEntityTags().isEmpty());
	}

	@Test void a03_of_typed_value() {
		var v = EntityTags.of("\"foo\"");
		var h = HttpEntityTagsHeader.of(NAME, v);
		assertEquals("\"foo\"", h.getValue());
		assertEquals(v, h.toEntityTags());
	}

	@Test void a04_of_typed_null() {
		var h = HttpEntityTagsHeader.of(NAME, (EntityTags)null);
		assertNull(h.toEntityTags());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_orElse_present() {
		var v1 = EntityTags.of("\"foo\"");
		var v2 = EntityTags.of("\"bar\"");
		assertEquals(v1, HttpEntityTagsHeader.of(NAME, v1).orElse(v2));
	}

	@Test void b02_orElse_absent() {
		var v2 = EntityTags.of("\"bar\"");
		assertEquals(v2, HttpEntityTagsHeader.of(NAME, (EntityTags)null).orElse(v2));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "\"foo\"", HttpEntityTagsHeader.LAZY_WIRE_STRING);
		assertEquals("\"foo\"", h.getValue());
		assertEquals(EntityTags.of("\"foo\""), h.toEntityTags());
	}

	@Test void c02_lazy_entityTags() {
		var v1 = EntityTags.of("\"foo\"");
		var h = new Sub(NAME, (Supplier<EntityTags>) () -> v1, HttpEntityTagsHeader.LAZY_ENTITY_TAGS);
		assertEquals(v1, h.toEntityTags());
	}
}
