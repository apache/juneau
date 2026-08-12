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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicMediaRangesHeader}.
 */
class BasicMediaRangesHeader_Test extends TestBase {

	private static final String NAME = "Accept";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_typedValue() {
		var h = BasicMediaRangesHeader.of(NAME, MediaRanges.of("text/json"));
		assertEquals(NAME, h.getName());
		assertEquals("text/json", h.getValue());
	}

	@Test void a02_of_typedValue_null_returnsNull() {
		assertNull(BasicMediaRangesHeader.of(NAME, (MediaRanges)null));
	}

	@Test void a03_of_wireString() {
		var h = BasicMediaRangesHeader.of(NAME, "text/json");
		assertEquals("text/json", h.getValue());
	}

	@Test void a04_of_wireString_null_returnsNull() {
		assertNull(BasicMediaRangesHeader.of(NAME, (String)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_typedValue() {
		var h = new BasicMediaRangesHeader(NAME, MediaRanges.of("text/json"));
		assertEquals("text/json", h.getValue());
		assertEquals("text/json", h.asMediaRanges().get().toString());
	}

	@Test void b02_ctor_typedValue_null() {
		var h = new BasicMediaRangesHeader(NAME, (MediaRanges)null);
		assertNull(h.getValue());
		assertTrue(h.asMediaRanges().isEmpty());
	}

	@Test void b03_ctor_wireString() {
		var h = new BasicMediaRangesHeader(NAME, "text/json;q=0.9,text/xml;q=0.1");
		assertEquals("text/json;q=0.9,text/xml;q=0.1", h.getValue());
	}

	@Test void b04_ctor_wireString_null() {
		// Unlike MediaRanges.of(String) (which maps null to EMPTY), the wire-string ctor's private parse() helper
		// short-circuits a null value straight to null (never calling MediaRanges.of()), so this ctor CAN produce
		// a truly unset header.
		var h = new BasicMediaRangesHeader(NAME, (String)null);
		assertNull(h.getValue());
		assertNull(h.toMediaRanges());
	}

	@Test void b05_ctor_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = new BasicMediaRangesHeader(NAME, () -> { calls[0]++; return MediaRanges.of("text/json"); });
		assertEquals(0, calls[0]);
		assertEquals("text/json", h.getValue());
		assertEquals(1, calls[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getRange_present() {
		var h = new BasicMediaRangesHeader(NAME, "text/json,text/xml");
		assertNotNull(h.getRange(0));
	}

	@Test void c02_getRange_unset() {
		var h = new BasicMediaRangesHeader(NAME, (MediaRanges)null);
		assertNull(h.getRange(0));
	}

	@Test void c03_hasSubtypePart_true() {
		var h = new BasicMediaRangesHeader(NAME, "text/json+activity");
		assertTrue(h.hasSubtypePart("activity"));
	}

	@Test void c04_hasSubtypePart_false() {
		var h = new BasicMediaRangesHeader(NAME, "text/json");
		assertFalse(h.hasSubtypePart("activity"));
	}

	@Test void c05_hasSubtypePart_unset() {
		var h = new BasicMediaRangesHeader(NAME, (MediaRanges)null);
		assertFalse(h.hasSubtypePart("activity"));
	}

	@Test void c06_match_found() {
		var h = new BasicMediaRangesHeader(NAME, "text/json");
		assertEquals(0, h.match(List.of(org.apache.juneau.commons.http.MediaType.of("text/json"))));
	}

	@Test void c07_match_unset() {
		var h = new BasicMediaRangesHeader(NAME, (MediaRanges)null);
		assertEquals(-1, h.match(List.of(org.apache.juneau.commons.http.MediaType.of("text/json"))));
	}

	@Test void c08_orElse_present() {
		var other = MediaRanges.of("text/xml");
		var h = new BasicMediaRangesHeader(NAME, MediaRanges.of("text/json"));
		assertNotSame(other, h.orElse(other));
	}

	@Test void c09_orElse_absent() {
		var other = MediaRanges.of("text/xml");
		assertSame(other, new BasicMediaRangesHeader(NAME, (MediaRanges)null).orElse(other));
	}

	@Test void c10_toMediaRanges() {
		var h = new BasicMediaRangesHeader(NAME, "text/json");
		assertEquals("text/json", h.toMediaRanges().toString());
	}
}
