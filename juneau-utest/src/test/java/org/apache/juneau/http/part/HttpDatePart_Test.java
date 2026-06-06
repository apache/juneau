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

import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.format.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpDatePart_Test extends TestBase {

	private static final String NAME = "X-Date";
	private static final ZonedDateTime ZDT = ZonedDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.UTC);
	private static final String ZDT_WIRE = ISO_DATE_TIME.format(ZDT);

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_zonedDateTimeValue() {
		var p = HttpDatePart.of(NAME, ZDT);
		assertEquals(NAME, p.getName());
		assertEquals(ZDT_WIRE, p.getValue());
		assertEquals(ZDT, p.toZonedDateTime());
	}

	@Test void a02_of_nullDate() {
		var p = HttpDatePart.of(NAME, (ZonedDateTime)null);
		assertEquals(NAME, p.getName());
		assertNull(p.getValue());
		assertNull(p.toZonedDateTime());
	}

	@Test void a03_ofString_validWire() {
		var p = HttpDatePart.ofString(NAME, ZDT_WIRE);
		assertEquals(NAME, p.getName());
		assertEquals(ZDT_WIRE, p.getValue());
		// Truncated to seconds, so should equal original (which already had 0 nanos).
		assertEquals(ZDT.truncatedTo(SECONDS), p.toZonedDateTime());
	}

	@Test void a04_ofString_emptyWire() {
		var p = HttpDatePart.ofString(NAME, "");
		assertNull(p.toZonedDateTime());
	}

	@Test void a05_ofString_nullWire() {
		var p = HttpDatePart.ofString(NAME, (String)null);
		assertNull(p.toZonedDateTime());
	}

	@Test void a06_ofString_badWire_throws() {
		assertThrows(DateTimeParseException.class, () -> HttpDatePart.ofString(NAME, "not-a-date"));
	}

	@Test void a07_ofLazy_present() {
		var p = HttpDatePart.ofLazy(NAME, () -> ZDT);
		assertEquals(NAME, p.getName());
		assertEquals(ZDT_WIRE, p.getValue());
		assertEquals(ZDT, p.toZonedDateTime());
	}

	@Test void a08_ofLazy_nullSupplied() {
		var p = HttpDatePart.ofLazy(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toZonedDateTime());
	}

	@Test void a09_ofString_truncatesSubSecondPrecision() {
		var withNanos = ZDT.withNano(123_456_789);
		var wire = ISO_DATE_TIME.format(withNanos);
		var p = HttpDatePart.ofString(NAME, wire);
		// toZonedDateTime() should be truncated to seconds.
		assertEquals(withNanos.truncatedTo(SECONDS), p.toZonedDateTime());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asZonedDateTime_present() {
		assertEquals(ZDT, HttpDatePart.of(NAME, ZDT).asZonedDateTime().get());
	}

	@Test void b02_asZonedDateTime_absent() {
		assertTrue(HttpDatePart.of(NAME, (ZonedDateTime)null).asZonedDateTime().isEmpty());
	}

	@Test void b03_orElse_present() {
		var fallback = ZonedDateTime.of(1999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertEquals(ZDT, HttpDatePart.of(NAME, ZDT).orElse(fallback));
	}

	@Test void b04_orElse_absent() {
		var fallback = ZonedDateTime.of(1999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertEquals(fallback, HttpDatePart.of(NAME, (ZonedDateTime)null).orElse(fallback));
	}

	@Test void b05_orElse_lazyAbsent() {
		var fallback = ZonedDateTime.of(1999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertEquals(fallback, HttpDatePart.ofLazy(NAME, () -> null).orElse(fallback));
	}
}
