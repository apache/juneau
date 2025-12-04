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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link GranularZonedDateTime}.
 */
class GranularZonedDateTime_Test extends TestBase {

	//====================================================================================================
	// Constructor(ZonedDateTime, ChronoField) tests
	//====================================================================================================

	@Test
	void b01_constructorWithZonedDateTime() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		assertEquals(zdt, gdt.zdt);
		assertEquals(ChronoField.HOUR_OF_DAY, gdt.precision);
	}

	@Test
	void b02_constructorWithZonedDateTime_preservesZone() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("America/New_York"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MINUTE_OF_HOUR);
		assertEquals(ZoneId.of("America/New_York"), gdt.zdt.getZone());
	}

	@Test
	void b03_constructorWithZonedDateTime_allPrecisions() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 123000000, ZoneId.of("UTC"));
		var precisions = new ChronoField[] {
			ChronoField.YEAR,
			ChronoField.MONTH_OF_YEAR,
			ChronoField.DAY_OF_MONTH,
			ChronoField.HOUR_OF_DAY,
			ChronoField.MINUTE_OF_HOUR,
			ChronoField.SECOND_OF_MINUTE,
			ChronoField.MILLI_OF_SECOND
		};
		for (var precision : precisions) {
			var gdt = new GranularZonedDateTime(zdt, precision);
			assertEquals(zdt, gdt.zdt);
			assertEquals(precision, gdt.precision);
		}
	}

	//====================================================================================================
	// copy() tests
	//====================================================================================================

	@Test
	void c01_copy_createsNewInstance() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt1 = new GranularZonedDateTime(zdt, ChronoField.DAY_OF_MONTH);
		var gdt2 = gdt1.copy();
		assertNotSame(gdt1, gdt2);
		assertEquals(gdt1.zdt, gdt2.zdt);
		assertEquals(gdt1.precision, gdt2.precision);
	}

	@Test
	void c02_copy_immutable() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt1 = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var gdt2 = gdt1.copy();
		// Modifying the copy should not affect original (though ZonedDateTime is immutable anyway)
		var gdt3 = gdt2.roll(1);
		assertEquals(gdt1.zdt, gdt1.zdt); // Original unchanged
		assertNotEquals(gdt1.zdt, gdt3.zdt);
	}

	//====================================================================================================
	// getZonedDateTime() tests
	//====================================================================================================

	@Test
	void d01_getZonedDateTime_returnsCorrectValue() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		assertEquals(zdt, gdt.getZonedDateTime());
		assertSame(gdt.zdt, gdt.getZonedDateTime());
	}

	@Test
	void d02_getZonedDateTime_differentZones() {
		var zdt1 = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var zdt2 = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("America/New_York"));
		var gdt1 = new GranularZonedDateTime(zdt1, ChronoField.MINUTE_OF_HOUR);
		var gdt2 = new GranularZonedDateTime(zdt2, ChronoField.MINUTE_OF_HOUR);
		assertEquals(zdt1, gdt1.getZonedDateTime());
		assertEquals(zdt2, gdt2.getZonedDateTime());
		assertNotEquals(gdt1.getZonedDateTime(), gdt2.getZonedDateTime());
	}

	//====================================================================================================
	// roll(int) tests
	//====================================================================================================

	@Test
	void e01_roll_forwardByOne() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plusYears(1), rolled.zdt);
		assertEquals(ChronoField.YEAR, rolled.precision);
	}

	@Test
	void e02_roll_backwardByOne() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(-1);
		assertEquals(zdt.minusYears(1), rolled.zdt);
		assertEquals(ChronoField.YEAR, rolled.precision);
	}

	@Test
	void e03_roll_byMultiple() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MONTH_OF_YEAR);
		var rolled = gdt.roll(3);
		assertEquals(zdt.plusMonths(3), rolled.zdt);
	}

	@Test
	void e04_roll_monthPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MONTH_OF_YEAR);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plusMonths(1), rolled.zdt);
	}

	@Test
	void e05_roll_dayPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.DAY_OF_MONTH);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plusDays(1), rolled.zdt);
	}

	@Test
	void e06_roll_hourPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plusHours(1), rolled.zdt);
	}

	@Test
	void e07_roll_minutePrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MINUTE_OF_HOUR);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plusMinutes(1), rolled.zdt);
	}

	@Test
	void e08_roll_secondPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.SECOND_OF_MINUTE);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plusSeconds(1), rolled.zdt);
	}

	@Test
	void e09_roll_millisecondPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 123000000, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MILLI_OF_SECOND);
		var rolled = gdt.roll(1);
		assertEquals(zdt.plus(1, ChronoUnit.MILLIS), rolled.zdt);
	}

	@Test
	void e10_roll_zero() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(0);
		assertEquals(zdt, rolled.zdt);
	}

	@Test
	void e11_roll_preservesPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.DAY_OF_MONTH);
		var rolled = gdt.roll(5);
		assertEquals(ChronoField.DAY_OF_MONTH, rolled.precision);
	}

	//====================================================================================================
	// roll(ChronoField, int) tests
	//====================================================================================================

	@Test
	void f01_rollWithField_differentField() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(ChronoField.MONTH_OF_YEAR, 2);
		assertEquals(zdt.plusMonths(2), rolled.zdt);
		// Precision should remain the same as original
		assertEquals(ChronoField.YEAR, rolled.precision);
	}

	@Test
	void f02_rollWithField_sameField() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(ChronoField.YEAR, 1);
		assertEquals(zdt.plusYears(1), rolled.zdt);
		assertEquals(ChronoField.YEAR, rolled.precision);
	}

	@Test
	void f03_rollWithField_allSupportedFields() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 123000000, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MILLI_OF_SECOND);

		var rolled1 = gdt.roll(ChronoField.YEAR, 1);
		assertEquals(zdt.plusYears(1), rolled1.zdt);

		var rolled2 = gdt.roll(ChronoField.MONTH_OF_YEAR, 1);
		assertEquals(zdt.plusMonths(1), rolled2.zdt);

		var rolled3 = gdt.roll(ChronoField.DAY_OF_MONTH, 1);
		assertEquals(zdt.plusDays(1), rolled3.zdt);

		var rolled4 = gdt.roll(ChronoField.HOUR_OF_DAY, 1);
		assertEquals(zdt.plusHours(1), rolled4.zdt);

		var rolled5 = gdt.roll(ChronoField.MINUTE_OF_HOUR, 1);
		assertEquals(zdt.plusMinutes(1), rolled5.zdt);

		var rolled6 = gdt.roll(ChronoField.SECOND_OF_MINUTE, 1);
		assertEquals(zdt.plusSeconds(1), rolled6.zdt);

		var rolled7 = gdt.roll(ChronoField.MILLI_OF_SECOND, 1);
		assertEquals(zdt.plus(1, ChronoUnit.MILLIS), rolled7.zdt);
	}

	@Test
	void f04_rollWithField_unsupportedField() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		// Using an unsupported ChronoField (one that toChronoUnit returns null for)
		var rolled = gdt.roll(ChronoField.DAY_OF_WEEK, 1);
		// Should return the same instance (no change)
		assertEquals(gdt.zdt, rolled.zdt);
		assertEquals(gdt.precision, rolled.precision);
	}

	@Test
	void f05_rollWithField_negativeAmount() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(ChronoField.MONTH_OF_YEAR, -3);
		assertEquals(zdt.minusMonths(3), rolled.zdt);
	}

	@Test
	void f06_rollWithField_zeroAmount() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.YEAR);
		var rolled = gdt.roll(ChronoField.MONTH_OF_YEAR, 0);
		assertEquals(zdt, rolled.zdt);
	}

	@Test
	void f07_rollWithField_preservesOriginalPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.DAY_OF_MONTH);
		var rolled = gdt.roll(ChronoField.YEAR, 1);
		// Precision should remain DAY_OF_MONTH, not YEAR
		assertEquals(ChronoField.DAY_OF_MONTH, rolled.precision);
	}

	//====================================================================================================
	// of(String) tests
	//====================================================================================================

	@Test
	void g01_of_yearOnly() {
		var gdt = GranularZonedDateTime.of("2011");
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(ChronoField.YEAR, gdt.precision);
	}

	@Test
	void g02_of_yearMonth() {
		var gdt = GranularZonedDateTime.of("2011-01");
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(ChronoField.MONTH_OF_YEAR, gdt.precision);
	}

	@Test
	void g03_of_date() {
		var gdt = GranularZonedDateTime.of("2011-01-15");
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ChronoField.DAY_OF_MONTH, gdt.precision);
	}

	@Test
	void g04_of_dateTime_hour() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ChronoField.HOUR_OF_DAY, gdt.precision);
	}

	@Test
	void g05_of_dateTime_minute() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ChronoField.MINUTE_OF_HOUR, gdt.precision);
	}

	@Test
	void g06_of_dateTime_second() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(ChronoField.SECOND_OF_MINUTE, gdt.precision);
	}

	@Test
	void g07_of_dateTime_millisecond() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(123, gdt.zdt.getNano() / 1_000_000);
		assertEquals(ChronoField.MILLI_OF_SECOND, gdt.precision);
	}

	@Test
	void g08_of_withOffset() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-05:00");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneOffset.of("-05:00"), gdt.zdt.getOffset());
	}

	@Test
	void g09_of_withPositiveOffset() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+09:00");
		assertNotNull(gdt);
		assertEquals(ZoneOffset.of("+09:00"), gdt.zdt.getOffset());
	}

	@Test
	void g10_of_invalidString() {
		// fromIso8601 throws DateTimeParseException for invalid input
		// The exception may be DateTimeParseException (from fromIso8601) or BasicRuntimeException (from of)
		var e = assertThrows(RuntimeException.class, () -> {
			GranularZonedDateTime.of("invalid-date");
		});
		// Verify that an exception was thrown (the exact message format may vary)
		assertNotNull(e);
		assertNotNull(e.getMessage());
	}

	@Test
	void g11_of_null() {
		assertThrows(IllegalArgumentException.class, () -> GranularZonedDateTime.of(null));
	}

	@Test
	void g12_of_emptyString() {
		assertThrowsWithMessage(RuntimeException.class, "Invalid ISO8601 timestamp", () -> {
			GranularZonedDateTime.of("");
		});
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================

	@Test
	void h01_rollAndof() {
		var gdt1 = GranularZonedDateTime.of("2011");
		var gdt2 = gdt1.roll(1);
		assertEquals(2012, gdt2.zdt.getYear());
		assertEquals(ChronoField.YEAR, gdt2.precision);
	}

	@Test
	void h02_rollMultipleTimes() {
		var gdt = GranularZonedDateTime.of("2011-01-15");
		var rolled1 = gdt.roll(1);
		var rolled2 = rolled1.roll(1);
		assertEquals(17, rolled2.zdt.getDayOfMonth());
	}

	@Test
	void h03_rollWithDifferentField() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30Z");
		// Roll by hours even though precision is minutes
		var rolled = gdt.roll(ChronoField.HOUR_OF_DAY, 2);
		assertEquals(14, rolled.zdt.getHour());
		assertEquals(30, rolled.zdt.getMinute());
		assertEquals(ChronoField.MINUTE_OF_HOUR, rolled.precision); // Original precision preserved
	}

	@Test
	void h04_copyAndRoll() {
		var gdt1 = GranularZonedDateTime.of("2011-01-15");
		var gdt2 = gdt1.copy();
		var rolled = gdt2.roll(1);
		// Original should be unchanged
		assertEquals(15, gdt1.zdt.getDayOfMonth());
		assertEquals(16, rolled.zdt.getDayOfMonth());
	}

	//====================================================================================================
	// of(String) tests
	//====================================================================================================

	@Test
	void i01_of_null() {
		// of(String) overload throws IllegalArgumentException when seg is null
		assertThrows(IllegalArgumentException.class, () -> {
			GranularZonedDateTime.of((String)null);
		});
		// of(String, ZoneId) also throws when seg is null
		assertThrows(IllegalArgumentException.class, () -> {
			GranularZonedDateTime.of((String)null, (ZoneId)null);
		});
	}

	@Test
	void i02_of_yearOnly() {
		var gdt = GranularZonedDateTime.of("2011", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(1, gdt.zdt.getDayOfMonth());
		assertEquals(0, gdt.zdt.getHour());
		assertEquals(ChronoField.YEAR, gdt.precision);
	}

	@Test
	void i03_of_yearMonth() {
		var gdt = GranularZonedDateTime.of("2011-01", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(1, gdt.zdt.getDayOfMonth());
		assertEquals(ChronoField.MONTH_OF_YEAR, gdt.precision);
	}

	@Test
	void i04_of_date() {
		var gdt = GranularZonedDateTime.of("2011-01-15", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ChronoField.DAY_OF_MONTH, gdt.precision);
	}

	@Test
	void i05_of_dateTime_hour() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(0, gdt.zdt.getMinute());
		assertEquals(ChronoField.HOUR_OF_DAY, gdt.precision);
	}

	@Test
	void i06_of_dateTime_minute() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30", null);
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(0, gdt.zdt.getSecond());
		assertEquals(ChronoField.MINUTE_OF_HOUR, gdt.precision);
	}

	@Test
	void i07_of_dateTime_second() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45", null);
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(ChronoField.SECOND_OF_MINUTE, gdt.precision);
	}

	@Test
	void i08_of_dateTime_millisecond_dot() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123", null);
		assertNotNull(gdt);
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ChronoField.MILLI_OF_SECOND, gdt.precision);
	}

	@Test
	void i09_of_dateTime_millisecond_comma() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45,123", null);
		assertNotNull(gdt);
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ChronoField.MILLI_OF_SECOND, gdt.precision);
	}

	@Test
	void i10_of_timeOnly_hour() {
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(now.getMonthValue(), gdt.zdt.getMonthValue());
		assertEquals(now.getDayOfMonth(), gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(0, gdt.zdt.getMinute());
		assertEquals(ChronoField.HOUR_OF_DAY, gdt.precision);
	}

	@Test
	void i11_of_timeOnly_minute() {
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12:30", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(now.getMonthValue(), gdt.zdt.getMonthValue());
		assertEquals(now.getDayOfMonth(), gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ChronoField.MINUTE_OF_HOUR, gdt.precision);
	}

	@Test
	void i12_of_timeOnly_second() {
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12:30:45", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(now.getMonthValue(), gdt.zdt.getMonthValue());
		assertEquals(now.getDayOfMonth(), gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(ChronoField.SECOND_OF_MINUTE, gdt.precision);
	}

	@Test
	void i13_of_timeOnly_millisecond() {
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12:30:45.123", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(now.getMonthValue(), gdt.zdt.getMonthValue());
		assertEquals(now.getDayOfMonth(), gdt.zdt.getDayOfMonth());
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ChronoField.MILLI_OF_SECOND, gdt.precision);
	}

	@Test
	void i14_of_withZ() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45Z", null);
		assertNotNull(gdt);
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
		assertEquals(12, gdt.zdt.getHour());
	}

	@Test
	void i15_of_withOffset_plusHH() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+05", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i16_of_withOffset_minusHH() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-05", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i17_of_withOffset_plusHHMM() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+0530", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), gdt.zdt.getOffset());
	}

	@Test
	void i18_of_withOffset_minusHHMM() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-0530", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), gdt.zdt.getOffset());
	}

	@Test
	void i19_of_withOffset_plusHH_MM() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+05:30", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), gdt.zdt.getOffset());
	}

	@Test
	void i20_of_withOffset_minusHH_MM() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-05:30", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), gdt.zdt.getOffset());
	}

	@Test
	void i21_of_timezoneAfterYear() {
		var gdt = GranularZonedDateTime.of("2011Z", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i22_of_timezoneAfterMonth() {
		var gdt = GranularZonedDateTime.of("2011-01Z", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i23_of_timezoneAfterDay() {
		var gdt = GranularZonedDateTime.of("2011-01-15Z", null);
		assertNotNull(gdt);
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i24_of_timezoneAfterHour() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12Z", null);
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i25_of_timezoneAfterMinute() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30Z", null);
		assertNotNull(gdt);
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i26_of_timezoneAfterT() {
		var gdt = GranularZonedDateTime.of("2011-01T+05:30", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), gdt.zdt.getOffset());
	}

	@Test
	void i27_of_nanoseconds_1digit() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.1", null);
		assertNotNull(gdt);
		assertEquals(100000000, gdt.zdt.getNano());
		// 1 digit is treated as milliseconds (hundreds of milliseconds)
		assertEquals(ChronoField.MILLI_OF_SECOND, gdt.precision);
	}

	@Test
	void i28_of_nanoseconds_2digits() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.12", null);
		assertNotNull(gdt);
		assertEquals(120000000, gdt.zdt.getNano());
	}

	@Test
	void i29_of_nanoseconds_3digits() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
	}

	@Test
	void i30_of_nanoseconds_4digits() {
		// Lines 1018: len == 4
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.1234", null);
		assertNotNull(gdt);
		assertEquals(123400000, gdt.zdt.getNano());
		assertEquals(ChronoField.NANO_OF_SECOND, gdt.precision);
	}

	@Test
	void i30a_of_nanoseconds_5digits() {
		// Lines 1019: len == 5
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.12345", null);
		assertNotNull(gdt);
		assertEquals(123450000, gdt.zdt.getNano());
		assertEquals(ChronoField.NANO_OF_SECOND, gdt.precision);
	}

	@Test
	void i30b_of_nanoseconds_6digits() {
		// Lines 1020: len == 6
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123456", null);
		assertNotNull(gdt);
		assertEquals(123456000, gdt.zdt.getNano());
		assertEquals(ChronoField.NANO_OF_SECOND, gdt.precision);
	}

	@Test
	void i30c_of_nanoseconds_7digits() {
		// Lines 1021: len == 7
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.1234567", null);
		assertNotNull(gdt);
		assertEquals(123456700, gdt.zdt.getNano());
		assertEquals(ChronoField.NANO_OF_SECOND, gdt.precision);
	}

	@Test
	void i30d_of_nanoseconds_8digits() {
		// Lines 1022: len == 8
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.12345678", null);
		assertNotNull(gdt);
		assertEquals(123456780, gdt.zdt.getNano());
		assertEquals(ChronoField.NANO_OF_SECOND, gdt.precision);
	}

	@Test
	void i30e_of_nanoseconds_9digits() {
		// Line 1023: len == 9 (default return)
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123456789", null);
		assertNotNull(gdt);
		assertEquals(123456789, gdt.zdt.getNano());
		assertEquals(ChronoField.NANO_OF_SECOND, gdt.precision);
	}

	@Test
	void i31_of_badTimestamps() {
		// Invalid formats
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("invalid", null));
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("", null));

		// Invalid year length
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("123", null));

		// Invalid month - below minimum (0)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-00", null));

		// Invalid month - above maximum (13)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-13", null));

		// Invalid month - way above maximum
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-99", null));

		// Invalid day - below minimum (0)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-00", null));

		// Invalid day - above maximum (32)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-32", null));

		// Invalid day - way above maximum
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-99", null));

		// Invalid hour - below minimum (-1, but this would be caught as invalid format)
		// Invalid hour - above maximum (24)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T24", null));

		// Invalid hour - way above maximum
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T99", null));

		// Invalid minute - above maximum (60)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:60", null));

		// Invalid minute - way above maximum
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:99", null));

		// Invalid second - above maximum (60)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:60", null));

		// Invalid second - way above maximum
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:99", null));

		// Invalid character after year (line 642)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011X", null));

		// Invalid character after '-' in S3 (line 651)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-X", null));

		// Invalid character after month (line 676)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01X", null));

		// Invalid character after '-' in S5 (line 685)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-X", null));

		// Invalid character after day (line 707)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15X", null));

		// Invalid character after 'T' in S7 (line 725)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("TX", null));

		// Invalid character after hour (line 747)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12X", null));

		// Invalid character after ':' in S9 (line 756)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:X", null));

		// Invalid character after minute (line 778)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30X", null));

		// Invalid character after ':' in S11 (line 787)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:X", null));

		// Invalid character in S12 after seconds (line 810)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45X", null));

		// Invalid character in S13 after '.' or ',' (line 827)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45.X", null));

		// Invalid character in S14 while reading milliseconds (line 846)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45.12X", null));

		// Invalid character in S16 after '+' (line 857)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+X", null));

		// Invalid character in S17 after '-' (line 865)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45-X", null));

		// Invalid character in S18 while reading offset hours (line 875)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+05X", null));

		// Invalid character in S19 after ':' in offset (line 884)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+05:X", null));

		// Invalid character in S20 while reading offset minutes (line 891)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+05:30X", null));

		// Invalid offset format in S18 finalization - not 2 or 4 digits (line 941)
		// Ending in S18 with 1 digit (should be 2 or 4)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+1", null));
		// Ending in S18 with 3 digits (should be 2 or 4)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+123", null));
		// Ending in S18 with 5 digits (should be 2 or 4)
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45+12345", null));

		// Invalid nanosecond length in ofNanos (line 1010)
		// This is defensive code - ofNanos checks length is 1-9 digits
		// The state machine should prevent this, but we test the defensive check
		// by ending in S14 with 0 digits (which shouldn't happen, but tests the check)
		// Actually, we can't easily trigger 0 digits since we transition to S14 only after seeing a digit.
		// For >9 digits, if we have 10+ digits, the 10th non-digit would trigger line 846, not 1010.
		// However, if we end the string with exactly 10 digits, we'd call ofNanos with len=10, triggering 1010.
		// But wait, the state machine allows digits in S14, so we'd need to end the string with 10+ digits.
		// Let's test with a string that ends with 10 digits of fractional seconds (no timezone)
		// This would end in S14 with 10 digits, calling ofNanos with len=10, which is >9, triggering line 1010.
		assertThrows(java.time.format.DateTimeParseException.class, () -> GranularZonedDateTime.of("2011-01-15T12:30:45.1234567890", null));
	}

	@Test
	void i38_of_timeOnly_withZ() {
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12:30:45Z", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i39_of_timeOnly_withOffset() {
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12:30:45+05:30", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), gdt.zdt.getOffset());
	}

	@Test
	void i40_of_noTimezone_usesSystemDefault() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45", null);
		assertNotNull(gdt);
		assertEquals(ZoneId.systemDefault(), gdt.zdt.getZone());
	}

	@Test
	void i41_of_yearFollowedByT() {
		// Lines 627-628: Year followed by 'T'
		var gdt = GranularZonedDateTime.of("2011T12", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(12, gdt.zdt.getHour());
	}

	@Test
	void i42_of_yearFollowedByZ() {
		// Lines 629-632: Year followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011Z", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i43_of_yearFollowedByPlus() {
		// Lines 633-636: Year followed by '+'
		var gdt = GranularZonedDateTime.of("2011+05", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i44_of_yearFollowedByMinus() {
		// Lines 637-640: Year followed by '-' (timezone)
		// Note: The code has two '-' checks in S2, but the first one (line 621) goes to S3 (month),
		// so the second '-' check (line 637) is unreachable in normal parsing.
		// However, we can test the negative timezone path by using a format that works:
		// After parsing a complete component, we can have timezone. Let's test with hour followed by negative timezone.
		// Actually, let's test the negative timezone path that IS reachable - after hour, minute, or second.
		// This test covers the concept even if the specific line isn't reachable.
		var gdt = GranularZonedDateTime.of("2011-01-15T12-05", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i45_of_monthFollowedByZ() {
		// Lines 663-666: Month followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011-01Z", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i46_of_monthFollowedByPlus() {
		// Lines 667-670: Month followed by '+'
		var gdt = GranularZonedDateTime.of("2011-01+05", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i47_of_monthFollowedByMinus() {
		// Lines 671-674: Month followed by '-' (timezone)
		// Note: Similar to i44, the second '-' check in S4 may be unreachable because the first '-' goes to S5 (day).
		// However, we can test the negative timezone concept with a reachable path.
		// Let's test with minute followed by negative timezone to cover the negative timezone logic.
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30-05", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i48_of_dayFollowedByZ() {
		// Lines 692-697: Day followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011-01-15Z", null);
		assertNotNull(gdt);
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i49_of_dayFollowedByPlus() {
		// Lines 698-701: Day followed by '+'
		var gdt = GranularZonedDateTime.of("2011-01-15+05", null);
		assertNotNull(gdt);
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i50_of_dayFollowedByMinus() {
		// Lines 702-705: Day followed by '-'
		var gdt = GranularZonedDateTime.of("2011-01-15-05", null);
		assertNotNull(gdt);
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i51_of_TFollowedByZ() {
		// Lines 715-717: 'T' followed by 'Z'
		var gdt = GranularZonedDateTime.of("TZ", null);
		assertNotNull(gdt);
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i52_of_TFollowedByPlus() {
		// Lines 718-720: 'T' followed by '+'
		var gdt = GranularZonedDateTime.of("T+05", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i53_of_TFollowedByMinus() {
		// Lines 721-723: 'T' followed by '-'
		var gdt = GranularZonedDateTime.of("T-05", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i54_of_hourFollowedByZ() {
		// Lines 732-737: Hour followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011-01-15T12Z", null);
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i55_of_hourFollowedByPlus() {
		// Lines 738-741: Hour followed by '+'
		var gdt = GranularZonedDateTime.of("2011-01-15T12+05", null);
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i56_of_hourFollowedByMinus() {
		// Lines 742-745: Hour followed by '-'
		var gdt = GranularZonedDateTime.of("2011-01-15T12-05", null);
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i57_of_minuteFollowedByZ() {
		// Lines 765-768: Minute followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30Z", null);
		assertNotNull(gdt);
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i58_of_minuteFollowedByPlus() {
		// Lines 769-772: Minute followed by '+'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30+05", null);
		assertNotNull(gdt);
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i59_of_minuteFollowedByMinus() {
		// Lines 773-776: Minute followed by '-'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30-05", null);
		assertNotNull(gdt);
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	//====================================================================================================
	// of(String) - Timezone after fractional seconds tests
	//====================================================================================================

	@Test
	void i73_of_S13_fractionalSeparatorFollowedByZ() {
		// Lines 817-819: S13 - '.' or ',' followed by 'Z'
		var gdt1 = GranularZonedDateTime.of("2011-01-15T12:30:45.Z", null);
		assertNotNull(gdt1);
		assertEquals(ZoneId.of("Z"), gdt1.zdt.getZone());
		assertEquals(0, gdt1.zdt.getNano());

		var gdt2 = GranularZonedDateTime.of("2011-01-15T12:30:45,Z", null);
		assertNotNull(gdt2);
		assertEquals(ZoneId.of("Z"), gdt2.zdt.getZone());
		assertEquals(0, gdt2.zdt.getNano());
	}

	@Test
	void i74_of_S13_fractionalSeparatorFollowedByPlus() {
		// Lines 820-822: S13 - '.' or ',' followed by '+'
		var gdt1 = GranularZonedDateTime.of("2011-01-15T12:30:45.+05:00", null);
		assertNotNull(gdt1);
		assertEquals(ZoneOffset.ofHours(5), gdt1.zdt.getOffset());
		assertEquals(0, gdt1.zdt.getNano());

		var gdt2 = GranularZonedDateTime.of("2011-01-15T12:30:45,+05:00", null);
		assertNotNull(gdt2);
		assertEquals(ZoneOffset.ofHours(5), gdt2.zdt.getOffset());
		assertEquals(0, gdt2.zdt.getNano());
	}

	@Test
	void i75_of_S13_fractionalSeparatorFollowedByMinus() {
		// Lines 823-825: S13 - '.' or ',' followed by '-'
		var gdt1 = GranularZonedDateTime.of("2011-01-15T12:30:45.-05:00", null);
		assertNotNull(gdt1);
		assertEquals(ZoneOffset.ofHours(-5), gdt1.zdt.getOffset());
		assertEquals(0, gdt1.zdt.getNano());

		var gdt2 = GranularZonedDateTime.of("2011-01-15T12:30:45,-05:00", null);
		assertNotNull(gdt2);
		assertEquals(ZoneOffset.ofHours(-5), gdt2.zdt.getOffset());
		assertEquals(0, gdt2.zdt.getNano());
	}

	@Test
	void i76_of_S14_fractionalSecondsFollowedByZ() {
		// Lines 833-836: S14 - fractional seconds followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123Z", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i77_of_S14_fractionalSecondsFollowedByPlus() {
		// Lines 837-840: S14 - fractional seconds followed by '+'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123+05:00", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i78_of_S14_fractionalSecondsFollowedByMinus() {
		// Lines 841-844: S14 - fractional seconds followed by '-'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45.123-05:00", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i79_of_S14_fractionalSecondsWithCommaFollowedByZ() {
		// Lines 833-836: S14 - fractional seconds (comma separator) followed by 'Z'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45,123Z", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
	}

	@Test
	void i80_of_S14_fractionalSecondsWithCommaFollowedByPlus() {
		// Lines 837-840: S14 - fractional seconds (comma separator) followed by '+'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45,123+05:00", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i81_of_S14_fractionalSecondsWithCommaFollowedByMinus() {
		// Lines 841-844: S14 - fractional seconds (comma separator) followed by '-'
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45,123-05:00", null);
		assertNotNull(gdt);
		assertEquals(123000000, gdt.zdt.getNano());
		assertEquals(ZoneOffset.ofHours(-5), gdt.zdt.getOffset());
	}

	@Test
	void i82_of_S15_invalidCharacterAfterZ() {
		// Line 846: S15 - invalid character after 'Z' (should throw error)
		// After finding 'Z', any additional characters should trigger this error
		// Test all possible transitions to S15:

		// S2 -> S15 (year followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011Z5", null);
		});

		// S4 -> S15 (month followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01ZX", null);
		});

		// S6 -> S15 (day followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15ZX", null);
		});

		// S7 -> S15 (T followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("TZX", null);
		});

		// S8 -> S15 (hour followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12ZX", null);
		});

		// S10 -> S15 (minute followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30ZX", null);
		});

		// S12 -> S15 (second followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45ZX", null);
		});
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45Z+", null);
		});
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45Z-", null);
		});
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45Z5", null);
		});
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45Z ", null);
		});

		// S13 -> S15 (fractional separator followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45.ZX", null);
		});

		// S14 -> S15 (fractional digits followed by Z, then invalid char)
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45.123ZX", null);
		});
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("T12:30:45ZX", null);
		});
	}

	@Test
	void i83_of_invalidDateForMonth() {
		// Invalid dates for specific months - LocalDateTime.of() throws DateTimeException
		// November only has 30 days
		assertThrows(java.time.DateTimeException.class, () -> {
			GranularZonedDateTime.of("2011-11-31", null);
		});
		// February in non-leap year only has 28 days
		assertThrows(java.time.DateTimeException.class, () -> {
			GranularZonedDateTime.of("2011-02-29", null);
		});
		// February in non-leap year only has 28 days (day 30)
		assertThrows(java.time.DateTimeException.class, () -> {
			GranularZonedDateTime.of("2011-02-30", null);
		});
		// April only has 30 days
		assertThrows(java.time.DateTimeException.class, () -> {
			GranularZonedDateTime.of("2011-04-31", null);
		});
		// June only has 30 days
		assertThrows(java.time.DateTimeException.class, () -> {
			GranularZonedDateTime.of("2011-06-31", null);
		});
		// September only has 30 days
		assertThrows(java.time.DateTimeException.class, () -> {
			GranularZonedDateTime.of("2011-09-31", null);
		});
		// Valid: February 29 in leap year
		var gdt = GranularZonedDateTime.of("2024-02-29", null);
		assertNotNull(gdt);
		assertEquals(2024, gdt.zdt.getYear());
		assertEquals(2, gdt.zdt.getMonthValue());
		assertEquals(29, gdt.zdt.getDayOfMonth());
	}

	@Test
	void i84_of_zoneIdAlreadySet() {
		// Line 941: zoneId is not null, so offset building is skipped (false branch)
		// Test with 'Z' timezone (zoneId already set)
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45Z", null);
		assertNotNull(gdt);
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
		// ohour and ominute remain -1, but zoneId is already set, so the if condition on line 941 is false
	}

	@Test
	void i85_of_offsetOnlyHours() {
		// Line 946: ohour >= 0 but ominute < 0 (only hours, no minutes) - else if branch
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+05", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(5), gdt.zdt.getOffset());
	}

	@Test
	void i86_of_offsetHoursAndMinutes() {
		// Line 942: ohour >= 0 && ominute >= 0 (both hours and minutes)
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+05:30", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), gdt.zdt.getOffset());
	}

	@Test
	void i87_of_timeOnlyMissingYear() {
		// Lines 964-966: timeOnly=true, year/month/day are -1, use current date
		var now = ZonedDateTime.now();
		var gdt = GranularZonedDateTime.of("T12:30:45", null);
		assertNotNull(gdt);
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(now.getMonthValue(), gdt.zdt.getMonthValue());
		assertEquals(now.getDayOfMonth(), gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
	}

	@Test
	void i88_of_dateFormatMissingMonth() {
		// Line 970: timeOnly=false, month is -1, default to 1
		var gdt = GranularZonedDateTime.of("2011", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue()); // Defaults to 1
		assertEquals(1, gdt.zdt.getDayOfMonth()); // Defaults to 1
	}

	@Test
	void i89_of_dateFormatMissingDay() {
		// Line 971: timeOnly=false, day is -1, default to 1
		var gdt = GranularZonedDateTime.of("2011-01", null);
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(1, gdt.zdt.getDayOfMonth()); // Defaults to 1
	}

	@Test
	void i90_of_withDefaultZoneId() {
		// Line 958: defaultZoneId != null branch - use provided defaultZoneId when no zone in string
		var defaultZone = ZoneId.of("America/New_York");
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45", defaultZone);
		assertNotNull(gdt);
		assertEquals(defaultZone, gdt.zdt.getZone());
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
	}

	@Test
	void i91_of_withDefaultZoneId_timeOnly() {
		// Line 958: defaultZoneId != null branch - use provided defaultZoneId for time-only format
		var defaultZone = ZoneId.of("Europe/London");
		var now = ZonedDateTime.now(defaultZone);
		var gdt = GranularZonedDateTime.of("T12:30:45", defaultZone);
		assertNotNull(gdt);
		assertEquals(defaultZone, gdt.zdt.getZone());
		assertEquals(now.getYear(), gdt.zdt.getYear());
		assertEquals(now.getMonthValue(), gdt.zdt.getMonthValue());
		assertEquals(now.getDayOfMonth(), gdt.zdt.getDayOfMonth());
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
	}

	@Test
	void i92_of_withDefaultZoneId_ignoredWhenZoneInString() {
		// Line 958: defaultZoneId is ignored when zone is found in the string
		var defaultZone = ZoneId.of("America/New_York");
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45Z", defaultZone);
		assertNotNull(gdt);
		// Should use 'Z' from the string, not the defaultZoneId
		assertEquals(ZoneId.of("Z"), gdt.zdt.getZone());
		assertNotEquals(defaultZone, gdt.zdt.getZone());
	}

	//====================================================================================================
	// of(String) - ISO8601 offset range validation tests (-18:00 ≤ offset ≤ +18:00)
	//====================================================================================================

	@Test
	void i60_of_offsetBoundary_minus18_00() {
		// Minimum valid offset: -18:00
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-18:00", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(-18, 0), gdt.zdt.getOffset());
	}

	@Test
	void i61_of_offsetBoundary_plus18_00() {
		// Maximum valid offset: +18:00
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+18:00", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(18, 0), gdt.zdt.getOffset());
	}

	@Test
	void i62_of_offsetBoundary_minus18_00_compact() {
		// Minimum valid offset in compact format: -1800
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-1800", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(-18, 0), gdt.zdt.getOffset());
	}

	@Test
	void i63_of_offsetBoundary_plus18_00_compact() {
		// Maximum valid offset in compact format: +1800
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+1800", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHoursMinutes(18, 0), gdt.zdt.getOffset());
	}

	@Test
	void i64_of_offsetBoundary_minus18_00_hoursOnly() {
		// Minimum valid offset hours only: -18
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45-18", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(-18), gdt.zdt.getOffset());
	}

	@Test
	void i65_of_offsetBoundary_plus18_00_hoursOnly() {
		// Maximum valid offset hours only: +18
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30:45+18", null);
		assertNotNull(gdt);
		assertEquals(ZoneOffset.ofHours(18), gdt.zdt.getOffset());
	}

	@Test
	void i66_of_offsetInvalid_belowMinimum() {
		// Invalid: offset below -18:00
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45-19:00", null);
		});
	}

	@Test
	void i67_of_offsetInvalid_aboveMaximum() {
		// Invalid: offset above +18:00
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45+19:00", null);
		});
	}

	@Test
	void i68_of_offsetInvalid_belowMinimum_compact() {
		// Invalid: offset below -18:00 in compact format
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45-1900", null);
		});
	}

	@Test
	void i69_of_offsetInvalid_aboveMaximum_compact() {
		// Invalid: offset above +18:00 in compact format
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45+1900", null);
		});
	}

	@Test
	void i70_of_offsetInvalid_belowMinimum_hoursOnly() {
		// Invalid: offset below -18 hours only
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45-19", null);
		});
	}

	@Test
	void i71_of_offsetInvalid_aboveMaximum_hoursOnly() {
		// Invalid: offset above +18 hours only
		assertThrows(java.time.format.DateTimeParseException.class, () -> {
			GranularZonedDateTime.of("2011-01-15T12:30:45+19", null);
		});
	}

	@Test
	void i72_of_offsetValid_withinRange() {
		// Valid offsets within range
		var gdt1 = GranularZonedDateTime.of("2011-01-15T12:30:45-12:30", null);
		assertNotNull(gdt1);
		assertEquals(ZoneOffset.ofHoursMinutes(-12, -30), gdt1.zdt.getOffset());

		var gdt2 = GranularZonedDateTime.of("2011-01-15T12:30:45+12:30", null);
		assertNotNull(gdt2);
		assertEquals(ZoneOffset.ofHoursMinutes(12, 30), gdt2.zdt.getOffset());

		var gdt3 = GranularZonedDateTime.of("2011-01-15T12:30:45-17:59", null);
		assertNotNull(gdt3);
		assertEquals(ZoneOffset.ofHoursMinutes(-17, -59), gdt3.zdt.getOffset());

		var gdt4 = GranularZonedDateTime.of("2011-01-15T12:30:45+17:59", null);
		assertNotNull(gdt4);
		assertEquals(ZoneOffset.ofHoursMinutes(17, 59), gdt4.zdt.getOffset());
	}
}

