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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class TimeZoneFormat_Test {

	private static final TimeZone PST = TimeZone.getTimeZone("America/Los_Angeles");
	private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

	@Test void a01_idVariant() {
		assertEquals("America/Los_Angeles", TimeZoneFormat.ID.format(PST));
		assertEquals("America/Los_Angeles", TimeZoneFormat.NOT_SET.format(PST));
		assertEquals("America/Los_Angeles", TimeZoneFormat.ID.format(LA));
	}

	@Test void a02_offsetVariant() {
		var off = TimeZoneFormat.OFFSET.format(PST);
		assertNotNull(off);
		assertTrue(off.startsWith("-") || off.equals("Z"), "Unexpected offset: " + off);
	}

	@Test void a03_nameVariants() {
		assertNotNull(TimeZoneFormat.NAME_LONG.format(PST));
		assertNotNull(TimeZoneFormat.NAME_SHORT.format(PST));
		assertNotNull(TimeZoneFormat.NAME_LONG.format(LA));
		assertNotNull(TimeZoneFormat.NAME_SHORT.format(LA));
	}

	@Test void a04_roundTrip_id() {
		var s = TimeZoneFormat.ID.format(PST);
		assertEquals(PST.getID(), TimeZoneFormat.parseTimeZone(s).getID());
		assertEquals(LA, TimeZoneFormat.parseZoneId(s));
	}

	@Test void a05_nullAndBlank() {
		assertNull(TimeZoneFormat.ID.format((TimeZone)null));
		assertNull(TimeZoneFormat.ID.format((ZoneId)null));
		assertNull(TimeZoneFormat.parseTimeZone(null));
		assertNull(TimeZoneFormat.parseTimeZone("  "));
		assertNull(TimeZoneFormat.parseZoneId(null));
		assertNull(TimeZoneFormat.parseZoneId("  "));
	}

	@Test void a06_zoneIdParsing_handlesShortIds() {
		assertNotNull(TimeZoneFormat.parseZoneId("PST"));
		assertNotNull(TimeZoneFormat.parseZoneId("UTC"));
	}
}
