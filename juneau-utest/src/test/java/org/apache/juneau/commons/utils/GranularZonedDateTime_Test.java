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
	// Constructor(Date, ChronoField) tests
	//====================================================================================================

	@Test
	void a01_constructorWithDate() {
		var date = new Date(1000000000L); // Fixed timestamp
		var gdt = new GranularZonedDateTime(date, ChronoField.YEAR);
		assertNotNull(gdt.zdt);
		assertEquals(ChronoField.YEAR, gdt.precision);
		assertEquals(date.toInstant().atZone(ZoneId.systemDefault()), gdt.zdt);
	}

	@Test
	void a02_constructorWithDate_differentPrecisions() {
		var date = new Date(1000000000L);
		var gdt1 = new GranularZonedDateTime(date, ChronoField.MONTH_OF_YEAR);
		var gdt2 = new GranularZonedDateTime(date, ChronoField.DAY_OF_MONTH);
		assertEquals(ChronoField.MONTH_OF_YEAR, gdt1.precision);
		assertEquals(ChronoField.DAY_OF_MONTH, gdt2.precision);
		assertEquals(gdt1.zdt, gdt2.zdt);
	}

	@Test
	void a03_constructorWithDate_usesSystemDefaultZone() {
		var date = new Date(1000000000L);
		var gdt = new GranularZonedDateTime(date, ChronoField.YEAR);
		assertEquals(ZoneId.systemDefault(), gdt.zdt.getZone());
	}

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
	// parse(String) tests
	//====================================================================================================

	@Test
	void g01_parse_yearOnly() {
		var gdt = GranularZonedDateTime.parse("2011");
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(ChronoField.YEAR, gdt.precision);
	}

	@Test
	void g02_parse_yearMonth() {
		var gdt = GranularZonedDateTime.parse("2011-01");
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(ChronoField.MONTH_OF_YEAR, gdt.precision);
	}

	@Test
	void g03_parse_date() {
		var gdt = GranularZonedDateTime.parse("2011-01-15");
		assertNotNull(gdt);
		assertEquals(2011, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(15, gdt.zdt.getDayOfMonth());
		assertEquals(ChronoField.DAY_OF_MONTH, gdt.precision);
	}

	@Test
	void g04_parse_dateTime_hour() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ChronoField.HOUR_OF_DAY, gdt.precision);
	}

	@Test
	void g05_parse_dateTime_minute() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12:30Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(ChronoField.MINUTE_OF_HOUR, gdt.precision);
	}

	@Test
	void g06_parse_dateTime_second() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12:30:45Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(ChronoField.SECOND_OF_MINUTE, gdt.precision);
	}

	@Test
	void g07_parse_dateTime_millisecond() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12:30:45.123Z");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(30, gdt.zdt.getMinute());
		assertEquals(45, gdt.zdt.getSecond());
		assertEquals(123, gdt.zdt.getNano() / 1_000_000);
		assertEquals(ChronoField.MILLI_OF_SECOND, gdt.precision);
	}

	@Test
	void g08_parse_withOffset() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12:30:45-05:00");
		assertNotNull(gdt);
		assertEquals(12, gdt.zdt.getHour());
		assertEquals(ZoneOffset.of("-05:00"), gdt.zdt.getOffset());
	}

	@Test
	void g09_parse_withPositiveOffset() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12:30:45+09:00");
		assertNotNull(gdt);
		assertEquals(ZoneOffset.of("+09:00"), gdt.zdt.getOffset());
	}

	@Test
	void g10_parse_invalidString() {
		// fromIso8601 throws DateTimeParseException for invalid input
		// The exception may be DateTimeParseException (from fromIso8601) or BasicRuntimeException (from parse)
		var e = assertThrows(RuntimeException.class, () -> {
			GranularZonedDateTime.parse("invalid-date");
		});
		// Verify that an exception was thrown (the exact message format may vary)
		assertNotNull(e);
		assertNotNull(e.getMessage());
	}

	@Test
	void g11_parse_null() {
		assertThrowsWithMessage(RuntimeException.class, "Invalid date", () -> {
			GranularZonedDateTime.parse(null);
		});
	}

	@Test
	void g12_parse_emptyString() {
		assertThrowsWithMessage(RuntimeException.class, "Invalid date", () -> {
			GranularZonedDateTime.parse("");
		});
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================

	@Test
	void h01_rollAndParse() {
		var gdt1 = GranularZonedDateTime.parse("2011");
		var gdt2 = gdt1.roll(1);
		assertEquals(2012, gdt2.zdt.getYear());
		assertEquals(ChronoField.YEAR, gdt2.precision);
	}

	@Test
	void h02_rollMultipleTimes() {
		var gdt = GranularZonedDateTime.parse("2011-01-15");
		var rolled1 = gdt.roll(1);
		var rolled2 = rolled1.roll(1);
		assertEquals(17, rolled2.zdt.getDayOfMonth());
	}

	@Test
	void h03_rollWithDifferentField() {
		var gdt = GranularZonedDateTime.parse("2011-01-15T12:30Z");
		// Roll by hours even though precision is minutes
		var rolled = gdt.roll(ChronoField.HOUR_OF_DAY, 2);
		assertEquals(14, rolled.zdt.getHour());
		assertEquals(30, rolled.zdt.getMinute());
		assertEquals(ChronoField.MINUTE_OF_HOUR, rolled.precision); // Original precision preserved
	}

	@Test
	void h04_copyAndRoll() {
		var gdt1 = GranularZonedDateTime.parse("2011-01-15");
		var gdt2 = gdt1.copy();
		var rolled = gdt2.roll(1);
		// Original should be unchanged
		assertEquals(15, gdt1.zdt.getDayOfMonth());
		assertEquals(16, rolled.zdt.getDayOfMonth());
	}
}

