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

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.MediaRanges;
import org.apache.juneau.commons.http.MediaType;
import org.junit.jupiter.api.*;

/**
 * Validates {@link Accept}.
 */
class Accept_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_mediaRanges() {
		var h = Accept.of(MediaRanges.of("text/json"));
		assertEquals("Accept", h.getName());
		assertEquals("text/json", h.getValue());
	}

	@Test void a02_of_mediaRanges_null_returnsNull() {
		assertNull(Accept.of((MediaRanges)null));
	}

	@Test void a03_of_mediaType() {
		var h = Accept.of(MediaType.of("text/json"));
		assertEquals("text/json", h.getValue());
	}

	@Test void a04_of_mediaType_null_returnsNull() {
		assertNull(Accept.of((MediaType)null));
	}

	@Test void a05_of_wireString() {
		var h = Accept.of("text/json");
		assertEquals("text/json", h.getValue());
	}

	@Test void a06_of_wireString_null_returnsNull() {
		assertNull(Accept.of((String)null));
	}

	@Test void a07_of_wireString_cached_returnsSameInstanceForSameValue() {
		var h1 = Accept.of("application/vnd.foo+json");
		var h2 = Accept.of("application/vnd.foo+json");
		assertSame(h1, h2);
	}

	@Test void a08_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = Accept.of((Supplier<MediaRanges>) () -> { calls[0]++; return MediaRanges.of("text/json"); });
		assertEquals(0, calls[0]);
		assertEquals("text/json", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a09_of_supplier_null_returnsNull() {
		assertNull(Accept.of((Supplier<MediaRanges>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_mediaRanges() {
		var h = new Accept(MediaRanges.of("text/json"));
		assertEquals("text/json", h.getValue());
	}

	@Test void b02_ctor_wireString() {
		var h = new Accept("text/json");
		assertEquals("text/json", h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constants
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_constant_null() {
		assertNull(Accept.NULL.getValue());
	}

	@Test void c02_constant_wildcard() {
		assertEquals("*/*", Accept.WILDCARD.getValue());
	}

	@Test void c03_constant_applicationJson() {
		assertEquals("application/json", Accept.APPLICATION_JSON.getValue());
	}
}
