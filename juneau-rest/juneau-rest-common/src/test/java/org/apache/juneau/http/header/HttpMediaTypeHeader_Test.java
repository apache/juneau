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
import org.apache.juneau.commons.http.MediaType;
import org.junit.jupiter.api.*;

class HttpMediaTypeHeader_Test extends TestBase {

	private static final String NAME = "Content-Type";

	// Package-private access exposes the protected constructors/lazy ctor for direct testing.
	private static final class Sub extends HttpMediaTypeHeader {
		Sub(String name, String value) { super(name, value); }
		Sub(String name, MediaType value) { super(name, value); }
		Sub(String name, Supplier<?> supplier, int lazyMode) { super(name, supplier, lazyMode); }
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_wire_validValue() {
		var h = new Sub(NAME, "text/plain");
		assertEquals(NAME, h.getName());
		assertEquals("text/plain", h.getValue());
		assertEquals(MediaType.of("text/plain"), h.toMediaType());
		assertEquals(MediaType.of("text/plain"), h.asMediaType().get());
	}

	@Test void a02_wire_nullValue() {
		var h = new Sub(NAME, (String)null);
		assertNull(h.toMediaType());
		assertNull(h.getValue());
		assertTrue(h.asMediaType().isEmpty());
	}

	@Test void a03_typed_value() {
		var h = new Sub(NAME, MediaType.of("text/plain"));
		assertEquals("text/plain", h.getValue());
		assertEquals(MediaType.of("text/plain"), h.toMediaType());
	}

	@Test void a04_typed_null() {
		var h = new Sub(NAME, (MediaType)null);
		assertNull(h.toMediaType());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Delegating accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_accessors() {
		var h = new Sub(NAME, "text/plain;charset=utf-8");
		assertEquals("text", h.getType());
		assertEquals("plain", h.getSubType());
		assertEquals(java.util.List.of("plain"), h.getSubTypes());
		assertFalse(h.isMetaSubtype());
		assertFalse(h.hasSubType("json"));
		assertEquals("utf-8", h.getParameter("charset"));
		assertFalse(h.getParameters().isEmpty());
		assertTrue(h.match(MediaType.of("text/plain"), true) > 0);
	}

	@Test void b02_orElse_present() {
		assertEquals(MediaType.of("text/plain"), new Sub(NAME, "text/plain").orElse(MediaType.EMPTY));
	}

	@Test void b03_orElse_absent() {
		assertSame(MediaType.EMPTY, new Sub(NAME, (String)null).orElse(MediaType.EMPTY));
	}

	@Test void b04_match_list() {
		var h = new Sub(NAME, "text/plain");
		var list = java.util.List.of(MediaType.of("text/json"), MediaType.of("text/plain"));
		assertEquals(1, h.match(list));
	}

	@Test void b05_match_list_noMatch() {
		var h = new Sub(NAME, (String)null);
		var list = java.util.List.of(MediaType.of("text/json"));
		assertEquals(-1, h.match(list));
	}

	@Test void b06_wire_withLeadingQualityPrefix() {
		// parseMediaType() strips off any leading comma-separated segment.
		var h = new Sub(NAME, "q=0.5,text/plain");
		assertEquals(MediaType.of("text/plain"), h.toMediaType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "text/plain", HttpMediaTypeHeader.LAZY_WIRE_STRING);
		assertEquals("text/plain", h.getValue());
		assertEquals(MediaType.of("text/plain"), h.toMediaType());
	}

	@Test void c02_lazy_mediaType() {
		var mt = MediaType.of("text/plain");
		var h = new Sub(NAME, (Supplier<MediaType>) () -> mt, HttpMediaTypeHeader.LAZY_MEDIA_TYPE);
		assertEquals(mt, h.toMediaType());
	}
}
