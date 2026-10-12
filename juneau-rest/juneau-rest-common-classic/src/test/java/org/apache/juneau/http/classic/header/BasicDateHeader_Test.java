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

import java.time.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicDateHeader}.
 */
class BasicDateHeader_Test extends TestBase {

	private static final String NAME = "Date";
	private static final String WIRE = "Wed, 16 Nov 1994 08:49:37 GMT";
	private static final ZonedDateTime DATE = ZonedDateTime.of(1994, 11, 16, 8, 49, 37, 0, ZoneOffset.UTC);

	@Test void a01_of_typedValue() {
		var h = BasicDateHeader.of(NAME, DATE);
		assertEquals(NAME, h.getName());
		assertEquals(WIRE, h.getValue());
		assertNull(BasicDateHeader.of(NAME, (ZonedDateTime)null));
	}

	@Test void a02_of_wireString_parsesAndTruncatesToSeconds() {
		var h = BasicDateHeader.of(NAME, WIRE);
		assertEquals(DATE, h.toZonedDateTime());
		assertEquals(WIRE, h.getValue());
		assertNull(BasicDateHeader.of(NAME, (String)null));
	}

	@Test void a03_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicDateHeader.of(NAME, () -> { calls[0]++; return DATE; });
		assertEquals(0, calls[0]);
		assertEquals(WIRE, h.getValue());
		assertEquals(1, calls[0]);
		assertNull(BasicDateHeader.of(NAME, (Supplier<ZonedDateTime>)null));
	}

	@Test void a04_wireString_emptyIsAbsent_invalidThrows() {
		assertTrue(new BasicDateHeader(NAME, "").asZonedDateTime().isEmpty());
		assertThrows(java.time.format.DateTimeParseException.class, () -> new BasicDateHeader(NAME, "not a date"));
	}

	@Test void b01_asZonedDateTime_orElse_toZonedDateTime() {
		var h = new BasicDateHeader(NAME, DATE);
		assertEquals(DATE, h.asZonedDateTime().get());
		assertEquals(DATE, h.orElse((ZonedDateTime)null));
		assertEquals(DATE, h.toZonedDateTime());
		var none = new BasicDateHeader(NAME, (Supplier<ZonedDateTime>)() -> null);
		assertTrue(none.asZonedDateTime().isEmpty());
		assertEquals(DATE, none.orElse(DATE));
		assertNull(none.toZonedDateTime());
		assertNull(none.getValue());
	}

	@Test void b02_assertZonedDateTime() {
		new BasicDateHeader(NAME, DATE).assertZonedDateTime().is(DATE);
		assertThrows(AssertionError.class, () -> new BasicDateHeader(NAME, DATE).assertZonedDateTime().is(DATE.plusDays(1)));
	}
}
