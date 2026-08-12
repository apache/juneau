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

import static java.time.format.DateTimeFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link IfRange}.
 */
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
	}

	@Test void a04_of_wireString_weakEtagForm() {
		var h = IfRange.of("W/\"abc\"");
		assertEquals("W/\"abc\"", h.asEntityTag().get().toString());
	}

	@Test void a05_of_wireString_httpDateForm() {
		var h = IfRange.of(WIRE_DATE);
		assertEquals(WIRE_DATE, h.getValue());
	}

	@Test void a06_of_wireString_null_returnsNull() {
		assertNull(IfRange.of((String)null));
	}

	@Test void a07_ctor_wireString_null_throwsNpe() {
		// Unlike the common-module IfRange (which null-checks before dispatching to isEtag()), the
		// classic IfRange(String) ctor's javadoc explicitly documents "must not be null (a null argument throws
		// NullPointerException)" -- so this is documented/intended behavior here, not a bug. Pinning it.
		assertThrows(NullPointerException.class, () -> new IfRange((String)null));
	}

	@Test void a08_of_zonedDateTime() {
		var h = IfRange.of(ZDT);
		assertEquals(WIRE_DATE, h.getValue());
	}

	@Test void a09_of_zonedDateTime_null_returnsNull() {
		assertNull(IfRange.of((ZonedDateTime)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lazy supplier
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_supplier_entityTag() {
		var tag = EntityTag.of("\"abc\"");
		var h = IfRange.of((Supplier<Object>) () -> tag);
		assertEquals(tag, h.asEntityTag().get());
		assertEquals("\"abc\"", h.getValue());
	}

	@Test void b02_of_supplier_zonedDateTime_getValue() {
		var h = IfRange.of((Supplier<Object>) () -> ZDT);
		assertEquals(WIRE_DATE, h.getValue());
	}

	@Test void b03_of_supplier_zonedDateTime_asZonedDateTime_notWired() {
		// Unlike asEntityTag(), asZonedDateTime() is inherited from BasicDateHeader and reads that
		// class's own private value/supplier fields, which the IfRange(Supplier) ctor never populates (it always
		// passes a null String to the super ctor). So a ZonedDateTime-returning supplier is visible via getValue()
		// (IfRange's own override) but not via the inherited asZonedDateTime() accessor. Pinning the current gap.
		var h = IfRange.of((Supplier<Object>) () -> ZDT);
		assertTrue(h.asZonedDateTime().isEmpty());
	}

	@Test void b04_of_supplier_null_value() {
		var h = IfRange.of((Supplier<Object>) () -> null);
		assertNull(h.getValue());
		assertTrue(h.asEntityTag().isEmpty());
	}

	@Test void b05_of_supplier_null_returnsNull() {
		assertNull(IfRange.of((Supplier<?>)null));
	}

	@Test void b06_of_supplier_invalidType_getValue_throws() {
		var h = IfRange.of((Supplier<Object>) () -> 123);
		assertThrows(RuntimeException.class, h::getValue);
	}

	@Test void b07_of_supplier_wrongTypeRequested_asEntityTag_returnsEmpty() {
		var h = IfRange.of((Supplier<Object>) () -> ZDT);
		assertTrue(h.asEntityTag().isEmpty());
	}
}
