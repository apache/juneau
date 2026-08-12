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

import java.net.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpUriHeader_Test extends TestBase {

	private static final String NAME = "Location";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpUriHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpUriHeader.of(NAME, "http://foo.com");
		assertEquals(NAME, h.getName());
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpUriHeader.of(NAME, (String)null);
		assertNull(h.toUri());
		assertNull(h.getValue());
		assertTrue(h.asUri().isEmpty());
	}

	@Test void a03_of_typed_value() {
		var h = HttpUriHeader.of(NAME, URI.create("http://foo.com"));
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
		assertEquals(URI.create("http://foo.com"), h.asUri().get());
	}

	@Test void a04_of_typed_null() {
		var h = HttpUriHeader.of(NAME, (URI)null);
		assertNull(h.toUri());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_orElse_present() {
		assertEquals(URI.create("http://foo.com"), HttpUriHeader.of(NAME, URI.create("http://foo.com")).orElse(URI.create("http://bar.com")));
	}

	@Test void b02_orElse_absent() {
		assertEquals(URI.create("http://bar.com"), HttpUriHeader.of(NAME, (URI)null).orElse(URI.create("http://bar.com")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "http://foo.com", HttpUriHeader.LAZY_WIRE_STRING);
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}

	@Test void c02_lazy_uri() {
		var h = new Sub(NAME, (Supplier<URI>) () -> URI.create("http://foo.com"), HttpUriHeader.LAZY_URI);
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}
}
