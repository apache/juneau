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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class DurationAndPeriodFormat_Test {

	@Test void a01_durationFormat_isoVariants() {
		var d = Duration.ofHours(26).plusMinutes(3);
		assertEquals("PT26H3M", DurationFormat.ISO_8601.format(d));
		assertEquals("PT26H3M", DurationFormat.NOT_SET.format(d));
		assertEquals("P1DT2H3M", DurationFormat.ISO_8601_WITH_DAYS.format(d));
		assertEquals(Duration.ofHours(-6), DurationFormat.ISO_8601.parse("PT-6H"));
		assertEquals(Duration.ofHours(-6), DurationFormat.ISO_8601.parse("-PT6H"));
	}

	@Test void a02_durationFormat_numericVariants() {
		var d = Duration.ofSeconds(45);
		assertEquals("45000000000", DurationFormat.NANOS.format(d));
		assertEquals("45000", DurationFormat.MILLIS.format(d));
		assertEquals("45.000000000", DurationFormat.SECONDS.format(d));

		assertEquals(Duration.ofNanos(45000000000L), DurationFormat.NANOS.parse("45000000000"));
		assertEquals(Duration.ofMillis(45000), DurationFormat.MILLIS.parse("45000"));
		assertEquals(Duration.ofSeconds(45), DurationFormat.SECONDS.parse("45.000000000"));
	}

	@Test void a03_durationFormat_hoconAndNumericFlag() {
		assertEquals("2h", DurationFormat.HOCON.format(Duration.ofHours(2)));
		assertEquals("500ms", DurationFormat.HOCON.format(Duration.ofMillis(500)));
		assertEquals(Duration.ofHours(2), DurationFormat.HOCON.parse("2h"));
		assertEquals(Duration.ofMillis(500), DurationFormat.HOCON.parse("500ms"));
		assertThrows(IllegalArgumentException.class, () -> DurationFormat.HOCON.parse("not-a-duration"));

		assertTrue(DurationFormat.NANOS.isNumeric());
		assertTrue(DurationFormat.MILLIS.isNumeric());
		assertTrue(DurationFormat.SECONDS.isNumeric());
		assertFalse(DurationFormat.ISO_8601.isNumeric());
		assertFalse(DurationFormat.ISO_8601_WITH_DAYS.isNumeric());
		assertFalse(DurationFormat.HOCON.isNumeric());
		assertFalse(DurationFormat.NOT_SET.isNumeric());
	}

	@Test void a04_durationFormat_nullAndBlankInputs() {
		assertNull(DurationFormat.ISO_8601.format(null));
		assertNull(DurationFormat.ISO_8601.parse(null));
		assertNull(DurationFormat.ISO_8601.parse("   "));
	}

	@Test void a05_durationFormat_isoWithDaysBranchCoverage() {
		assertEquals("PT0S", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ZERO));
		assertEquals("P1D", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ofDays(1)));
		assertEquals("P1DT1H", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ofDays(1).plusHours(1)));
		assertEquals("-PT6H", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ofHours(-6)));
		assertEquals("PT20.345S", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ofSeconds(20, 345_000_000)));
	}

	@Test void a06_durationFormat_hocon_parseAllUnits() {
		assertEquals(Duration.ofDays(2), DurationFormat.HOCON.parse("2d"));
		assertEquals(Duration.ofHours(3), DurationFormat.HOCON.parse("3h"));
		assertEquals(Duration.ofMinutes(4), DurationFormat.HOCON.parse("4m"));
		assertEquals(Duration.ofSeconds(5), DurationFormat.HOCON.parse("5s"));
		assertEquals(Duration.ofMillis(6), DurationFormat.HOCON.parse("6ms"));
		assertEquals(Duration.ofNanos(7_000), DurationFormat.HOCON.parse("7us"));
		assertEquals(Duration.ofNanos(8), DurationFormat.HOCON.parse("8ns"));
		assertEquals(Duration.ofHours(-2), DurationFormat.HOCON.parse("-2h"));
	}

	@Test void a07_durationFormat_hocon_zeroAndNegativeFormatting() {
		assertEquals("0s", DurationFormat.HOCON.format(Duration.ZERO));
		assertEquals("-1500ns", DurationFormat.HOCON.format(Duration.ofNanos(-1_500)));
	}

	@Test void a08_durationFormat_isoWithDays_minutesAndNanosOnly() {
		assertEquals("PT5M", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ofMinutes(5)));
		assertEquals("PT0.345S", DurationFormat.ISO_8601_WITH_DAYS.format(Duration.ofNanos(345_000_000)));
	}

	@Test void b01_periodFormat_allVariants() {
		var p = Period.of(1, 2, 3);
		assertEquals("P1Y2M3D", PeriodFormat.ISO_8601.format(p));
		assertEquals("P1Y2M3D", PeriodFormat.NOT_SET.format(p));
		assertEquals("428", PeriodFormat.DAYS.format(p));

		assertEquals(Period.of(1, 2, 3), PeriodFormat.ISO_8601.parse("P1Y2M3D"));
		assertEquals(Period.of(1, 2, 3), PeriodFormat.NOT_SET.parse("P1Y2M3D"));
		assertEquals(Period.ofDays(9), PeriodFormat.DAYS.parse("9"));
	}

	@Test void b02_periodFormat_nullAndBlankInputs() {
		assertNull(PeriodFormat.ISO_8601.format(null));
		assertNull(PeriodFormat.ISO_8601.parse(null));
		assertNull(PeriodFormat.ISO_8601.parse("  "));
	}
}
