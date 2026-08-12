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
package org.apache.juneau.http.classic.part;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link BasicDatePart}
 */
class BasicDatePart_Test extends TestBase {

	private static final String NAME = "X-Date";
	private static final ZonedDateTime DATE = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_value() {
		var p = BasicDatePart.of(NAME, DATE);
		assertEquals(NAME, p.getName());
		assertEquals(DATE, p.toZonedDateTime());
	}

	@Test void a02_of_nullValue_returnsNull() {
		assertNull(BasicDatePart.of(NAME, (ZonedDateTime)null));
	}

	@Test void a03_of_nullName_returnsNull() {
		assertNull(BasicDatePart.of(null, DATE));
	}

	@Test void a04_of_supplier() {
		var p = BasicDatePart.of(NAME, () -> DATE);
		assertEquals(DATE, p.toZonedDateTime());
	}

	@Test void a05_of_nullSupplier_returnsNull() {
		assertNull(BasicDatePart.of(NAME, (java.util.function.Supplier<ZonedDateTime>)null));
	}

	@Test void a06_of_nullName_supplier_returnsNull() {
		assertNull(BasicDatePart.of(null, () -> DATE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_zonedDateTimeValue() {
		var p = new BasicDatePart(NAME, DATE);
		assertEquals(DATE, p.toZonedDateTime());
		assertNotNull(p.getValue());
	}

	@Test void b02_ctor_zonedDateTimeValue_null() {
		var p = new BasicDatePart(NAME, (ZonedDateTime)null);
		assertNull(p.getValue());
		assertNull(p.toZonedDateTime());
	}

	@Test void b03_ctor_stringValue() {
		var p = new BasicDatePart(NAME, DATE.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME));
		assertEquals(DATE, p.toZonedDateTime());
	}

	@Test void b04_ctor_stringValue_empty() {
		assertNull(new BasicDatePart(NAME, "").toZonedDateTime());
	}

	@Test void b05_ctor_stringValue_null() {
		assertNull(new BasicDatePart(NAME, (String)null).toZonedDateTime());
	}

	@Test void b06_ctor_supplier() {
		assertEquals(DATE, new BasicDatePart(NAME, () -> DATE).toZonedDateTime());
	}

	@Test void b07_ctor_supplier_nullSupplied() {
		var p = new BasicDatePart(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toZonedDateTime());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asZonedDateTime_present() {
		assertEquals(DATE, BasicDatePart.of(NAME, DATE).asZonedDateTime().get());
	}

	@Test void c02_asZonedDateTime_absent() {
		assertTrue(new BasicDatePart(NAME, "").asZonedDateTime().isEmpty());
	}

	@Test void c03_orElse_present() {
		var other = DATE.plusDays(1);
		assertEquals(DATE, BasicDatePart.of(NAME, DATE).orElse(other));
	}

	@Test void c04_orElse_absent() {
		var other = DATE.plusDays(1);
		assertEquals(other, new BasicDatePart(NAME, (ZonedDateTime)null).orElse(other));
	}

	@Test void c05_assertZonedDateTime() {
		BasicDatePart.of(NAME, DATE).assertZonedDateTime().isExists();
	}
}
