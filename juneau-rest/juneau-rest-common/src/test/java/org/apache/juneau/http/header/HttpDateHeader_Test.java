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
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpDateHeader_Test extends TestBase {

	private static final String NAME = "Date";
	private static final ZonedDateTime ZDT = ZonedDateTime.from(RFC_1123_DATE_TIME.parse("Sun, 06 Nov 1994 08:49:37 GMT"));
	private static final String WIRE = RFC_1123_DATE_TIME.format(ZDT);

	// Tiny subclass to expose the protected lazy constructor for testing both LAZY modes.
	private static final class Sub extends HttpDateHeader {
		Sub(String name, Supplier<?> supplier, int lazyMode) {
			super(name, supplier, lazyMode);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_wire_validValue() {
		var h = HttpDateHeader.of(NAME, WIRE);
		assertEquals(NAME, h.getName());
		assertEquals(WIRE, h.getValue());
		assertEquals(ZDT, h.toZonedDateTime());
		assertEquals(ZDT, h.asZonedDateTime().get());
	}

	@Test void a02_of_wire_nullValue() {
		var h = HttpDateHeader.of(NAME, (String)null);
		assertNull(h.toZonedDateTime());
		assertNull(h.getValue());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void a03_of_typed_value() {
		var h = HttpDateHeader.of(NAME, ZDT);
		assertEquals(WIRE, h.getValue());
		assertEquals(ZDT, h.toZonedDateTime());
	}

	@Test void a04_of_typed_null() {
		var h = HttpDateHeader.of(NAME, (ZonedDateTime)null);
		assertNull(h.toZonedDateTime());
		assertNull(h.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_orElse_present() {
		assertEquals(ZDT, HttpDateHeader.of(NAME, ZDT).orElse(ZDT.plusDays(1)));
	}

	@Test void b02_orElse_absent() {
		var other = ZDT.plusDays(1);
		assertEquals(other, HttpDateHeader.of(NAME, (ZonedDateTime)null).orElse(other));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy modes (exercise the protected ctor through a tiny subclass).
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lazy_wireString() {
		var h = new Sub(NAME, (Supplier<String>) () -> WIRE, HttpDateHeader.LAZY_WIRE_STRING);
		assertEquals(WIRE, h.getValue());
		assertEquals(ZDT, h.toZonedDateTime());
	}

	@Test void c02_lazy_zonedDateTime() {
		var h = new Sub(NAME, (Supplier<ZonedDateTime>) () -> ZDT, HttpDateHeader.LAZY_ZONED_DATE_TIME);
		assertEquals(WIRE, h.getValue());
		assertEquals(ZDT, h.toZonedDateTime());
	}

	@Test void c03_lazy_zonedDateTime_null() {
		var h = new Sub(NAME, (Supplier<ZonedDateTime>) () -> null, HttpDateHeader.LAZY_ZONED_DATE_TIME);
		assertNull(h.getValue());
		assertNull(h.toZonedDateTime());
	}
}
