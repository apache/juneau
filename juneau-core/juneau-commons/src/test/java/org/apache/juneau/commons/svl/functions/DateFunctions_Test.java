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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/** Tests for {@link DateFunctions}. */
class DateFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(DateFunctions.ALL).build();

	@Test void now_isANumber() {
		var s = vr.resolve("#{now()}");
		var n = Long.parseLong(s);
		assertTrue(n > 1_700_000_000_000L, "now should produce a recent epoch milli value, got: " + s);
	}

	@Test void parseDate_isoInstant() {
		assertEquals("0", vr.resolve("#{parseDate(\"1970-01-01T00:00:00Z\")}"));
	}

	@Test void parseDate_isoDate() {
		// 2026-05-25 UTC midnight via Java time:
		var expected = String.valueOf(java.time.LocalDate.of(2026, 5, 25)
			.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());
		assertEquals(expected, vr.resolve("#{parseDate(\"2026-05-25\")}"));
	}

	@Test void parseDate_pattern() {
		var expected = String.valueOf(java.time.LocalDate.of(2026, 5, 25)
			.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());
		assertEquals(expected, vr.resolve("#{parseDate(\"2026-05-25\", \"yyyy-MM-dd\")}"));
	}

	@Test void formatDate_iso() {
		assertEquals("1970-01-01T00:00:00Z", vr.resolve("#{formatDate(0)}"));
	}

	@Test void formatDate_pattern() {
		var epoch = java.time.LocalDate.of(2026, 5, 25)
			.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
		assertEquals("2026-05-25", vr.resolve("#{formatDate(" + epoch + ", \"yyyy-MM-dd\")}"));
	}

	@Test void roundTrip() {
		// parseDate then formatDate should preserve the value.
		var ts = vr.resolve("#{parseDate(\"2026-05-25T12:34:56Z\")}");
		assertEquals("2026-05-25T12:34:56Z", vr.resolve("#{formatDate(" + ts + ")}"));
	}
}
