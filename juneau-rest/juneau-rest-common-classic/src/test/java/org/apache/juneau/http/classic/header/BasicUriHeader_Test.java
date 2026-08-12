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
package org.apache.juneau.http.classic.header;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicUriHeader}.
 */
class BasicUriHeader_Test extends TestBase {

	private static final String NAME = "Location";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wireString_validValue() {
		var h = BasicUriHeader.of(NAME, "http://foo.com");
		assertEquals(NAME, h.getName());
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}

	@Test void a02_of_wireString_null_returnsNull() {
		assertNull(BasicUriHeader.of(NAME, (String)null));
	}

	@Test void a03_of_typedValue() {
		var h = BasicUriHeader.of(NAME, URI.create("http://foo.com"));
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}

	@Test void a04_of_typedValue_null_returnsNull() {
		assertNull(BasicUriHeader.of(NAME, (URI)null));
	}

	@Test void a05_of_supplier_validValue() {
		var h = BasicUriHeader.of(NAME, () -> URI.create("http://foo.com"));
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}

	@Test void a06_of_supplier_null_returnsNull() {
		assertNull(BasicUriHeader.of(NAME, (java.util.function.Supplier<URI>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString_validValue() {
		var h = new BasicUriHeader(NAME, "http://foo.com");
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
	}

	@Test void b02_ctor_wireString_null() {
		var h = new BasicUriHeader(NAME, (String)null);
		assertNull(h.getValue());
		assertNull(h.toUri());
		assertTrue(h.asUri().isEmpty());
	}

	@Test void b03_ctor_wireString_empty() {
		var h = new BasicUriHeader(NAME, "");
		assertNull(h.toUri());
	}

	@Test void b04_ctor_typedValue() {
		var h = new BasicUriHeader(NAME, URI.create("http://foo.com"));
		assertEquals("http://foo.com", h.getValue());
		assertEquals(URI.create("http://foo.com"), h.toUri());
		assertEquals(URI.create("http://foo.com"), h.asUri().get());
	}

	@Test void b05_ctor_typedValue_null() {
		var h = new BasicUriHeader(NAME, (URI)null);
		assertNull(h.getValue());
		assertNull(h.toUri());
	}

	@Test void b06_ctor_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = new BasicUriHeader(NAME, () -> { calls[0]++; return URI.create("http://foo.com"); });
		assertEquals(0, calls[0]);
		assertEquals(URI.create("http://foo.com"), h.toUri());
		assertEquals(1, calls[0]);
		assertEquals("http://foo.com", h.getValue());
		assertEquals(2, calls[0]);
	}

	@Test void b07_ctor_nameNull_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicUriHeader(null, "http://foo.com"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_orElse_present() {
		var h = new BasicUriHeader(NAME, URI.create("http://foo.com"));
		assertEquals(URI.create("http://foo.com"), h.orElse(URI.create("http://bar.com")));
	}

	@Test void c02_orElse_absent() {
		var h = new BasicUriHeader(NAME, (URI)null);
		assertEquals(URI.create("http://bar.com"), h.orElse(URI.create("http://bar.com")));
	}

	@Test void c03_asUri_present() {
		var h = new BasicUriHeader(NAME, URI.create("http://foo.com"));
		assertEquals(URI.create("http://foo.com"), h.asUri().get());
	}

	@Test void c04_asUri_absent() {
		var h = new BasicUriHeader(NAME, (URI)null);
		assertTrue(h.asUri().isEmpty());
	}
}
