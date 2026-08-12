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
import org.apache.juneau.commons.http.MediaType;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicMediaTypeHeader}.
 */
class BasicMediaTypeHeader_Test extends TestBase {

	private static final String NAME = "Content-Type";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_typedValue() {
		var h = BasicMediaTypeHeader.of(NAME, MediaType.of("text/json"));
		assertEquals("text/json", h.getValue());
	}

	@Test void a02_of_typedValue_null_returnsNull() {
		assertNull(BasicMediaTypeHeader.of(NAME, (MediaType)null));
	}

	@Test void a03_of_wireString() {
		var h = BasicMediaTypeHeader.of(NAME, "text/json");
		assertEquals("text/json", h.getValue());
	}

	@Test void a04_of_wireString_null_returnsNull() {
		assertNull(BasicMediaTypeHeader.of(NAME, (String)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString_multiValue_usesLastValue() {
		var h = new BasicMediaTypeHeader(NAME, "text/json,text/xml");
		assertEquals("text/xml", h.getValue());
	}

	@Test void b02_ctor_wireString_null() {
		var h = new BasicMediaTypeHeader(NAME, (String)null);
		assertNull(h.toMediaType());
	}

	@Test void b03_ctor_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = new BasicMediaTypeHeader(NAME, () -> { calls[0]++; return MediaType.of("text/json"); });
		assertEquals(0, calls[0]);
		assertEquals("text/json", h.getValue());
		assertEquals(1, calls[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asMediaType_present() {
		assertEquals("text/json", new BasicMediaTypeHeader(NAME, "text/json").asMediaType().get().toString());
	}

	@Test void c02_asMediaType_absent() {
		assertTrue(new BasicMediaTypeHeader(NAME, (MediaType)null).asMediaType().isEmpty());
	}

	@Test void c03_getParameter() {
		var h = new BasicMediaTypeHeader(NAME, "text/html;level=1");
		assertEquals("1", h.getParameter("level"));
	}

	@Test void c04_getParameters() {
		var h = new BasicMediaTypeHeader(NAME, "text/html;level=1");
		assertEquals(1, h.getParameters().size());
	}

	@Test void c05_getSubType() {
		assertEquals("json", new BasicMediaTypeHeader(NAME, "text/json").getSubType());
	}

	@Test void c06_getSubTypes() {
		assertEquals(List.of("json", "foo"), new BasicMediaTypeHeader(NAME, "text/json+foo").getSubTypes());
	}

	@Test void c07_getType() {
		assertEquals("text", new BasicMediaTypeHeader(NAME, "text/json").getType());
	}

	@Test void c08_hasSubType() {
		assertTrue(new BasicMediaTypeHeader(NAME, "text/json+activity").hasSubType("activity"));
	}

	@Test void c09_isMetaSubtype_true() {
		assertTrue(new BasicMediaTypeHeader(NAME, "text/*").isMetaSubtype());
	}

	@Test void c10_isMetaSubtype_false() {
		assertFalse(new BasicMediaTypeHeader(NAME, "text/json").isMetaSubtype());
	}

	@Test void c11_match_list_found() {
		var h = new BasicMediaTypeHeader(NAME, "text/json");
		assertEquals(0, h.match(List.of(MediaType.of("text/json"))));
	}

	@Test void c12_match_list_absent() {
		var h = new BasicMediaTypeHeader(NAME, (MediaType)null);
		assertEquals(-1, h.match(List.of(MediaType.of("text/json"))));
	}

	@Test void c13_match_single() {
		var h = new BasicMediaTypeHeader(NAME, "text/json");
		assertTrue(h.match(MediaType.of("text/json"), false) > 0);
	}

	@Test void c14_orElse_present() {
		var other = MediaType.of("text/xml");
		assertNotSame(other, new BasicMediaTypeHeader(NAME, "text/json").orElse(other));
	}

	@Test void c15_orElse_absent() {
		var other = MediaType.of("text/xml");
		assertSame(other, new BasicMediaTypeHeader(NAME, (MediaType)null).orElse(other));
	}

	@Test void c16_toMediaType() {
		assertEquals("text/json", new BasicMediaTypeHeader(NAME, "text/json").toMediaType().toString());
	}
}
