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

class BasicStringRangesHeader_Test extends TestBase {

	private static final String NAME = "Accept-Encoding";

	//------------------------------------------------------------------------------------------------------------------
	// Factories -- of(String,String) / of(String,StringRanges) / of(String,Supplier)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = BasicStringRangesHeader.of(NAME, "gzip;q=0.5, identity");
		assertEquals(NAME, h.getName());
		assertEquals("gzip;q=0.5, identity", h.getValue());
		assertEquals(StringRanges.of("gzip;q=0.5, identity").toString(), h.toStringRanges().toString());
		assertTrue(h.asStringRanges().isPresent());
	}

	@Test void a02_of_wire_nullValue_returnsNull() {
		assertNull(BasicStringRangesHeader.of(NAME, (String)null));
	}

	@Test void a03_of_typed_value() {
		var v = StringRanges.of("gzip");
		var h = BasicStringRangesHeader.of(NAME, v);
		assertEquals("gzip", h.getValue());
		assertSame(v, h.toStringRanges());
	}

	@Test void a04_of_typed_null_returnsNull() {
		assertNull(BasicStringRangesHeader.of(NAME, (StringRanges)null));
	}

	@Test void a05_of_supplier_null_returnsNull() {
		assertNull(BasicStringRangesHeader.of(NAME, (java.util.function.Supplier<StringRanges>)null));
	}

	@Test void a06_of_supplier_evaluatedLazily() {
		var v = StringRanges.of("gzip");
		var h = BasicStringRangesHeader.of(NAME, () -> v);
		assertSame(v, h.toStringRanges());
		assertEquals("gzip", h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors -- null-value handling (bypasses the null-forwarding factories above)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a07_ctor_wireString_null() {
		// StringRanges.of(null) resolves to StringRanges.EMPTY (never null), so a null wire string still
		// produces a non-null (empty) typed value; only the typed-value ctor (a08) can produce a truly unset header.
		var h = new BasicStringRangesHeader(NAME, (String)null);
		assertEquals("", h.getValue());
		assertNotNull(h.toStringRanges());
		assertEquals("", h.toStringRanges().toString());
	}

	@Test void a08_ctor_typed_null() {
		var h = new BasicStringRangesHeader(NAME, (StringRanges)null);
		assertNull(h.getValue());
		assertNull(h.toStringRanges());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Delegating accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getRange() {
		var h = BasicStringRangesHeader.of(NAME, "gzip, identity");
		assertNotNull(h.getRange(0));
	}

	@Test void b02_getRange_unset() {
		var h = new BasicStringRangesHeader(NAME, (StringRanges)null);
		assertNull(h.getRange(0));
	}

	@Test void b03_match() {
		var h = BasicStringRangesHeader.of(NAME, "gzip");
		assertEquals(0, h.match(List.of("gzip")));
	}

	@Test void b04_match_unset() {
		var h = new BasicStringRangesHeader(NAME, (StringRanges)null);
		assertEquals(-1, h.match(List.of("gzip")));
	}

	@Test void b05_orElse_present() {
		var v = StringRanges.of("gzip");
		assertSame(v, BasicStringRangesHeader.of(NAME, v).orElse(StringRanges.of("identity")));
	}

	@Test void b06_orElse_absent() {
		var other = StringRanges.of("identity");
		assertSame(other, new BasicStringRangesHeader(NAME, (StringRanges)null).orElse(other));
	}
}
