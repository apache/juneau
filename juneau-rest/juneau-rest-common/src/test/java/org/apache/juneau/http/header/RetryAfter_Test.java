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

import static java.time.format.DateTimeFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RetryAfter_Test extends TestBase {

	private static final ZonedDateTime ZDT =
		ZonedDateTime.from(RFC_1123_DATE_TIME.parse("Sun, 06 Nov 1994 08:49:37 GMT")).truncatedTo(ChronoUnit.SECONDS);
	private static final String WIRE_DATE = RFC_1123_DATE_TIME.format(ZDT);

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_integer() {
		var h = RetryAfter.of(120);
		assertEquals("Retry-After", h.getName());
		assertEquals(120, h.asInteger().get());
		assertEquals("120", h.getValue());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void a02_of_integer_null_returnsNull() {
		assertNull(RetryAfter.of((Integer)null));
	}

	@Test void a03_of_wireString_numericForm() {
		var h = RetryAfter.of("120");
		assertEquals(120, h.asInteger().get());
		assertEquals("120", h.getValue());
	}

	@Test void a04_of_wireString_httpDateForm() {
		var h = RetryAfter.of(WIRE_DATE);
		assertEquals(ZDT, h.asZonedDateTime().get());
		assertTrue(h.asInteger().isEmpty());
	}

	@Test void a05_ctor_wireString_null() {
		// Direct ctor (not the of() factory, which short-circuits null to null) to hit the constructor's own
		// null-handling branch.
		var h = new RetryAfter((String)null);
		assertNull(h.getValue());
		assertTrue(h.asInteger().isEmpty());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void a06_of_wireString_emptyString_isNotHttpDate() {
		// HTT: an empty string is neither numeric nor a parseable HTTP-date; pins the ie(value) branch in the
		// wire-string constructor (treated the same as a missing value rather than throwing a parse exception).
		var h = RetryAfter.of("");
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void a07_of_string_null_returnsNull() {
		assertNull(RetryAfter.of((String)null));
	}

	@Test void a08_of_zonedDateTime() {
		var h = RetryAfter.of(ZDT);
		assertEquals(WIRE_DATE, h.getValue());
		assertEquals(ZDT, h.asZonedDateTime().get());
	}

	@Test void a09_of_zonedDateTime_null_returnsNull() {
		assertNull(RetryAfter.of((ZonedDateTime)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy supplier
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_supplier_integer() {
		var h = RetryAfter.of((Supplier<Object>) () -> 30);
		assertEquals(30, h.asInteger().get());
		assertEquals("30", h.getValue());
	}

	@Test void b02_of_supplier_zonedDateTime() {
		var h = RetryAfter.of((Supplier<Object>) () -> ZDT);
		assertEquals(ZDT, h.asZonedDateTime().get());
		assertEquals(WIRE_DATE, h.getValue());
	}

	@Test void b03_of_supplier_null() {
		var h = RetryAfter.of((Supplier<Object>) () -> null);
		assertNull(h.getValue());
		assertTrue(h.asInteger().isEmpty());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void b04_of_supplier_invalidType_throws() {
		var h = RetryAfter.of((Supplier<Object>) () -> "not-a-valid-type");
		assertThrows(RuntimeException.class, h::getValue);
	}

	@Test void b05_of_supplier_wrongTypeRequested_returnsEmpty() {
		var h = RetryAfter.of((Supplier<Object>) () -> ZDT);
		assertTrue(h.asInteger().isEmpty());
	}
}
