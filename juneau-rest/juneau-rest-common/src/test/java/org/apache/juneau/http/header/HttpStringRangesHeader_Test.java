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
import org.junit.jupiter.api.*;

class HttpStringRangesHeader_Test extends TestBase {

	private static final String NAME = "Accept-Encoding";

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpStringRangesHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpStringRangesHeader.of(NAME, "gzip;q=0.5, identity");
		assertEquals(NAME, h.getName());
		assertEquals("gzip;q=0.5, identity", h.getValue());
		assertEquals(StringRanges.of("gzip;q=0.5, identity").toString(), h.toStringRanges().toString());
		assertTrue(h.asStringRanges().isPresent());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpStringRangesHeader.of(NAME, (String)null);
		assertNull(h.toStringRanges());
		assertNull(h.getValue());
		assertTrue(h.asStringRanges().isEmpty());
	}

	@Test void a03_of_typed_value() {
		var v = StringRanges.of("gzip");
		var h = HttpStringRangesHeader.of(NAME, v);
		assertEquals("gzip", h.getValue());
		assertSame(v, h.toStringRanges());
	}

	@Test void a04_of_typed_null() {
		var h = HttpStringRangesHeader.of(NAME, (StringRanges)null);
		assertNull(h.toStringRanges());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Delegating accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getRange() {
		var h = HttpStringRangesHeader.of(NAME, "gzip, identity");
		assertNotNull(h.getRange(0));
	}

	@Test void b02_getRange_unset() {
		var h = HttpStringRangesHeader.of(NAME, (String)null);
		assertNull(h.getRange(0));
	}

	@Test void b03_match() {
		var h = HttpStringRangesHeader.of(NAME, "gzip");
		assertEquals(0, h.match(java.util.List.of("gzip")));
	}

	@Test void b04_match_unset() {
		var h = HttpStringRangesHeader.of(NAME, (String)null);
		assertEquals(-1, h.match(java.util.List.of("gzip")));
	}

	@Test void b05_orElse_present() {
		var v = StringRanges.of("gzip");
		assertSame(v, HttpStringRangesHeader.of(NAME, v).orElse(StringRanges.of("identity")));
	}

	@Test void b06_orElse_absent() {
		var other = StringRanges.of("identity");
		assertSame(other, HttpStringRangesHeader.of(NAME, (String)null).orElse(other));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> "gzip", HttpStringRangesHeader.LAZY_WIRE_STRING);
		assertEquals("gzip", h.getValue());
		assertEquals("gzip", h.toStringRanges().toString());
	}

	@Test void c02_lazy_stringRanges() {
		var v = StringRanges.of("gzip");
		var h = new Sub(NAME, (Supplier<StringRanges>) () -> v, HttpStringRangesHeader.LAZY_STRING_RANGES);
		assertSame(v, h.toStringRanges());
	}
}
