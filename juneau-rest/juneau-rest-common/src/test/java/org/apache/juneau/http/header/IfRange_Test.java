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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class IfRange_Test extends TestBase {

	private static final ZonedDateTime ZDT =
		ZonedDateTime.from(RFC_1123_DATE_TIME.parse("Sun, 06 Nov 1994 08:49:37 GMT")).truncatedTo(ChronoUnit.SECONDS);
	private static final String WIRE_DATE = RFC_1123_DATE_TIME.format(ZDT);

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_entityTag() {
		var tag = EntityTag.of("\"abc\"");
		var h = IfRange.of(tag);
		assertEquals("If-Range", h.getName());
		assertEquals(tag, h.asEntityTag().get());
		assertEquals("\"abc\"", h.getValue());
	}

	@Test void a02_of_entityTag_null_returnsNull() {
		assertNull(IfRange.of((EntityTag)null));
	}

	@Test void a03_of_wireString_etagForm() {
		var h = IfRange.of("\"abc\"");
		assertEquals("\"abc\"", h.asEntityTag().get().toString());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void a04_of_wireString_weakEtagForm() {
		var h = IfRange.of("W/\"abc\"");
		assertEquals("W/\"abc\"", h.asEntityTag().get().toString());
	}

	@Test void a05_of_wireString_httpDateForm() {
		var h = IfRange.of(WIRE_DATE);
		assertEquals(ZDT, h.asZonedDateTime().get());
		assertTrue(h.asEntityTag().isEmpty());
	}

	@Test void a06_ctor_wireString_null() {
		// Direct ctor (not the of() factory, which short-circuits null to null) to hit the constructor's own
		// null-handling branch.
		var h = new IfRange((String)null);
		assertNull(h.getValue());
		assertTrue(h.asEntityTag().isEmpty());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void a07_of_wireString_emptyString_isNotHttpDate() {
		// HTT: an empty string is neither an entity-tag nor a parseable HTTP-date; pins the ie(value) branch in the
		// wire-string constructor (treated the same as a missing value rather than throwing a parse exception).
		var h = IfRange.of("");
		assertNull(h.asZonedDateTime().orElse(null));
	}

	@Test void a08_of_null_returnsNull() {
		assertNull(IfRange.of((String)null));
	}

	@Test void a09_of_zonedDateTime() {
		var h = IfRange.of(ZDT);
		assertEquals(WIRE_DATE, h.getValue());
		assertEquals(ZDT, h.asZonedDateTime().get());
	}

	@Test void a10_of_zonedDateTime_null_returnsNull() {
		assertNull(IfRange.of((ZonedDateTime)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy supplier
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_supplier_entityTag() {
		var tag = EntityTag.of("\"abc\"");
		var h = IfRange.of((java.util.function.Supplier<Object>) () -> tag);
		assertEquals(tag, h.asEntityTag().get());
		assertEquals("\"abc\"", h.getValue());
	}

	@Test void b02_of_supplier_zonedDateTime() {
		var h = IfRange.of((java.util.function.Supplier<Object>) () -> ZDT);
		assertEquals(ZDT, h.asZonedDateTime().get());
		assertEquals(WIRE_DATE, h.getValue());
	}

	@Test void b03_of_supplier_null() {
		var h = IfRange.of((java.util.function.Supplier<Object>) () -> null);
		assertNull(h.getValue());
		assertTrue(h.asEntityTag().isEmpty());
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void b04_of_supplier_invalidType_throws() {
		var h = IfRange.of((java.util.function.Supplier<Object>) () -> 123);
		assertThrows(RuntimeException.class, h::getValue);
	}

	@Test void b05_of_supplier_wrongTypeRequested_returnsEmpty() {
		var h = IfRange.of((java.util.function.Supplier<Object>) () -> ZDT);
		assertTrue(h.asEntityTag().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// White-box: the private isEtag(String) helper's own null-check is never reached via its sole call site
	// (the String ctor already gates on "value != null" before calling isEtag), so it's invoked directly via
	// reflection here to cover that defensive branch.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_isEtag_null_viaReflection_returnsFalse() throws Exception {
		var m = IfRange.class.getDeclaredMethod("isEtag", String.class);
		m.setAccessible(true);
		assertEquals(false, m.invoke(null, (String)null));
	}
}
