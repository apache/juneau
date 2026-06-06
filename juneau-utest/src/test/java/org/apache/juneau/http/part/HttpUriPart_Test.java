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
package org.apache.juneau.http.part;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpUriPart_Test extends TestBase {

	private static final String NAME = "X-Location";
	private static final URI URI_VALUE = URI.create("http://example.com/foo");
	private static final String URI_WIRE = "http://example.com/foo";

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_uriValue() {
		var p = HttpUriPart.of(NAME, URI_VALUE);
		assertEquals(NAME, p.getName());
		assertEquals(URI_WIRE, p.getValue());
		assertEquals(URI_VALUE, p.toUri());
	}

	@Test void a02_of_nullUri() {
		var p = HttpUriPart.of(NAME, (URI)null);
		assertEquals(NAME, p.getName());
		assertNull(p.getValue());
		assertNull(p.toUri());
	}

	@Test void a03_ofString_validWire() {
		var p = HttpUriPart.ofString(NAME, URI_WIRE);
		assertEquals(NAME, p.getName());
		assertEquals(URI_WIRE, p.getValue());
		assertEquals(URI_VALUE, p.toUri());
	}

	@Test void a04_ofString_emptyWire() {
		var p = HttpUriPart.ofString(NAME, "");
		assertNull(p.toUri());
	}

	@Test void a05_ofString_nullWire() {
		var p = HttpUriPart.ofString(NAME, (String)null);
		assertNull(p.toUri());
	}

	@Test void a06_ofString_badWire_throws() {
		assertThrows(IllegalArgumentException.class, () -> HttpUriPart.ofString(NAME, "http://example.com/ has space"));
	}

	@Test void a07_ofLazy_present() {
		var p = HttpUriPart.ofLazy(NAME, () -> URI_VALUE);
		assertEquals(NAME, p.getName());
		assertEquals(URI_WIRE, p.getValue());
		assertEquals(URI_VALUE, p.toUri());
	}

	@Test void a08_ofLazy_nullSupplied() {
		var p = HttpUriPart.ofLazy(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toUri());
	}

	@Test void a09_of_relativeUri() {
		var rel = URI.create("/path/to/resource?q=1");
		var p = HttpUriPart.of(NAME, rel);
		assertEquals("/path/to/resource?q=1", p.getValue());
		assertEquals(rel, p.toUri());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asUri_present() {
		assertEquals(URI_VALUE, HttpUriPart.of(NAME, URI_VALUE).asUri().get());
	}

	@Test void b02_asUri_absent() {
		assertTrue(HttpUriPart.of(NAME, (URI)null).asUri().isEmpty());
	}

	@Test void b03_orElse_present() {
		var fallback = URI.create("http://default.com/");
		assertEquals(URI_VALUE, HttpUriPart.of(NAME, URI_VALUE).orElse(fallback));
	}

	@Test void b04_orElse_absent() {
		var fallback = URI.create("http://default.com/");
		assertEquals(fallback, HttpUriPart.of(NAME, (URI)null).orElse(fallback));
	}

	@Test void b05_orElse_lazyAbsent() {
		var fallback = URI.create("http://default.com/");
		assertEquals(fallback, HttpUriPart.ofLazy(NAME, () -> null).orElse(fallback));
	}
}
