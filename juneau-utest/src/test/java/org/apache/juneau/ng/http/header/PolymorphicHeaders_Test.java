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
package org.apache.juneau.ng.http.header;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

/**
 * Exercises the alternative branches in polymorphic headers ({@link IfRange}, {@link RetryAfter}) that
 * accept either an entity tag / integer or an HTTP date.
 *
 * <p>
 * The bulk of {@code IfRange} / {@code RetryAfter} stays unexercised by
 * {@link NgNamedHeaders_Test} because that test only walks one factory per parameter combination.
 * Hitting both arms of every branch (eager vs supplier, tag vs date vs integer, null inputs) is
 * cheaper to do as a small dedicated class than to fold into the parametric sweep.
 */
class PolymorphicHeaders_Test extends TestBase {

	private static final ZonedDateTime DATE = ZonedDateTime.parse("2024-01-15T08:30:00Z");
	private static final String DATE_WIRE = "Mon, 15 Jan 2024 08:30:00 GMT";

	//------------------------------------------------------------------------------------------------------------------
	// IfRange — entity tag / HTTP date / lazy supplier paths.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ifRange_eagerEntityTag() {
		var h = IfRange.of(EntityTag.of("\"foo\""));
		assertEquals("If-Range: \"foo\"", h.toString());
		assertEquals("\"foo\"", h.getValue());
		assertEquals(EntityTag.of("\"foo\""), h.asEntityTag().orElse(null));
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void a02_ifRange_eagerHttpDate() {
		var h = IfRange.of(DATE);
		assertEquals(DATE_WIRE, h.getValue());
		assertEquals(DATE.toInstant(), h.asZonedDateTime().orElseThrow().toInstant());
		assertNull(h.asEntityTag().orElse(null));
	}

	@Test void a03_ifRange_wireString_entityTag() {
		var h = new IfRange("\"foo\"");
		assertEquals("\"foo\"", h.getValue());
		assertEquals(EntityTag.of("\"foo\""), h.asEntityTag().orElse(null));
	}

	@Test void a04_ifRange_wireString_weakEntityTag() {
		var h = new IfRange("W/\"foo\"");
		assertEquals("W/\"foo\"", h.getValue());
		assertEquals(EntityTag.of("W/\"foo\""), h.asEntityTag().orElse(null));
	}

	@Test void a05_ifRange_wireString_httpDate() {
		var h = new IfRange(DATE_WIRE);
		assertNotNull(h.asZonedDateTime().orElse(null));
		assertNull(h.asEntityTag().orElse(null));
	}

	@Test void a06_ifRange_wireString_null() {
		var h = new IfRange((String) null);
		assertNull(h.getValue());
		assertNull(h.asEntityTag().orElse(null));
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void a07_ifRange_supplier_entityTag() {
		var h = IfRange.of(() -> EntityTag.of("\"bar\""));
		assertEquals("\"bar\"", h.getValue());
		assertEquals(EntityTag.of("\"bar\""), h.asEntityTag().orElse(null));
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void a08_ifRange_supplier_httpDate() {
		var h = IfRange.of(() -> DATE);
		assertEquals(DATE_WIRE, h.getValue());
		assertEquals(DATE.toInstant(), h.asZonedDateTime().orElseThrow().toInstant());
		assertNull(h.asEntityTag().orElse(null));
	}

	@Test void a09_ifRange_supplier_null() {
		var h = IfRange.of(() -> null);
		assertNull(h.getValue());
		assertNull(h.asEntityTag().orElse(null));
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void a10_ifRange_factories_returnNullForNullInput() {
		assertNull(IfRange.of((EntityTag) null));
		assertNull(IfRange.of((String) null));
		assertNull(IfRange.of((ZonedDateTime) null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// RetryAfter — delay-seconds / HTTP date / lazy supplier paths.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_retryAfter_eagerInteger() {
		var h = RetryAfter.of(Integer.valueOf(120));
		assertEquals("Retry-After: 120", h.toString());
		assertEquals("120", h.getValue());
		assertEquals(120, h.asInteger().orElseThrow());
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void b02_retryAfter_eagerHttpDate() {
		var h = RetryAfter.of(DATE);
		assertEquals(DATE_WIRE, h.getValue());
		assertEquals(DATE.toInstant(), h.asZonedDateTime().orElseThrow().toInstant());
		assertNull(h.asInteger().orElse(null));
	}

	@Test void b03_retryAfter_wireString_numeric() {
		var h = new RetryAfter("60");
		assertEquals("60", h.getValue());
		assertEquals(60, h.asInteger().orElseThrow());
	}

	@Test void b04_retryAfter_wireString_httpDate() {
		var h = new RetryAfter(DATE_WIRE);
		assertNotNull(h.asZonedDateTime().orElse(null));
		assertNull(h.asInteger().orElse(null));
	}

	@Test void b05_retryAfter_wireString_null() {
		var h = new RetryAfter((String) null);
		assertNull(h.getValue());
		assertNull(h.asInteger().orElse(null));
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void b06_retryAfter_supplier_integer() {
		var h = RetryAfter.of(() -> Integer.valueOf(90));
		assertEquals("90", h.getValue());
		assertEquals(90, h.asInteger().orElseThrow());
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void b07_retryAfter_supplier_httpDate() {
		var h = RetryAfter.of(() -> DATE);
		assertEquals(DATE_WIRE, h.getValue());
		assertEquals(DATE.toInstant(), h.asZonedDateTime().orElseThrow().toInstant());
		assertNull(h.asInteger().orElse(null));
	}

	@Test void b08_retryAfter_supplier_null() {
		var h = RetryAfter.of(() -> null);
		assertNull(h.getValue());
		assertNull(h.asInteger().orElse(null));
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void b09_retryAfter_factories_returnNullForNullInput() {
		assertNull(RetryAfter.of((Integer) null));
		assertNull(RetryAfter.of((String) null));
		assertNull(RetryAfter.of((ZonedDateTime) null));
	}
}
