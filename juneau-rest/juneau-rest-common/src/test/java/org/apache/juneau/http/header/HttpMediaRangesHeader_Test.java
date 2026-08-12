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
import org.apache.juneau.commons.http.*;
import org.apache.juneau.commons.http.MediaType;
import org.junit.jupiter.api.*;

class HttpMediaRangesHeader_Test extends TestBase {

	private static final String NAME = "Accept";

	// Package-private access exposes the protected constructors/lazy ctor for direct testing.
	private static final class Sub extends HttpMediaRangesHeader {
		Sub(String name, String value) { super(name, value); }
		Sub(String name, MediaRanges value) { super(name, value); }
		Sub(String name, Supplier<?> supplier, int lazyMode) { super(name, supplier, lazyMode); }
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_wire_validValue() {
		var h = new Sub(NAME, "text/plain, text/json");
		assertEquals(NAME, h.getName());
		assertEquals("text/plain, text/json", h.getValue());
		assertEquals(MediaRanges.of("text/plain, text/json"), h.toMediaRanges());
		assertEquals(MediaRanges.of("text/plain, text/json"), h.asMediaRanges().get());
	}

	@Test void a02_wire_nullValue() {
		var h = new Sub(NAME, (String)null);
		assertNull(h.toMediaRanges());
		assertNull(h.getValue());
		assertTrue(h.asMediaRanges().isEmpty());
	}

	@Test void a03_typed_value() {
		var v = MediaRanges.of("text/plain");
		var h = new Sub(NAME, v);
		assertEquals("text/plain", h.getValue());
		assertEquals(v, h.toMediaRanges());
	}

	@Test void a04_typed_null() {
		var h = new Sub(NAME, (MediaRanges)null);
		assertNull(h.toMediaRanges());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Delegating accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getRange() {
		var h = new Sub(NAME, "text/plain, text/json");
		assertNotNull(h.getRange(0));
	}

	@Test void b02_getRange_unset() {
		var h = new Sub(NAME, (String)null);
		assertNull(h.getRange(0));
	}

	@Test void b03_hasSubtypePart() {
		var h = new Sub(NAME, "text/json+activity");
		assertTrue(h.hasSubtypePart("activity"));
		assertFalse(h.hasSubtypePart("bogus"));
	}

	@Test void b04_hasSubtypePart_unset() {
		var h = new Sub(NAME, (String)null);
		assertFalse(h.hasSubtypePart("activity"));
	}

	@Test void b05_match() {
		var h = new Sub(NAME, "text/plain");
		assertEquals(0, h.match(java.util.List.of(MediaType.of("text/plain"))));
	}

	@Test void b06_match_unset() {
		var h = new Sub(NAME, (String)null);
		assertEquals(-1, h.match(java.util.List.of(MediaType.of("text/plain"))));
	}

	@Test void b07_orElse_present() {
		var v = MediaRanges.of("text/plain");
		assertEquals(v, new Sub(NAME, v).orElse(MediaRanges.of("text/json")));
	}

	@Test void b08_orElse_absent() {
		var other = MediaRanges.of("text/json");
		assertEquals(other, new Sub(NAME, (String)null).orElse(other));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "text/plain", HttpMediaRangesHeader.LAZY_WIRE_STRING);
		assertEquals("text/plain", h.getValue());
		assertEquals(MediaRanges.of("text/plain"), h.toMediaRanges());
	}

	@Test void c02_lazy_mediaRanges() {
		var v = MediaRanges.of("text/plain");
		var h = new Sub(NAME, (Supplier<MediaRanges>) () -> v, HttpMediaRangesHeader.LAZY_MEDIA_RANGES);
		assertEquals(v, h.toMediaRanges());
	}
}
