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

import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.*;
import static java.util.Calendar.*;
import static org.apache.juneau.commons.utils.DateUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class DateUtils_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Constructor (line 42)
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	void a00_constructor() {
		// Test line 42: class instantiation
		// DateUtils has an implicit public no-arg constructor
		var instance = new DateUtils();
		assertNotNull(instance);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test getPrecisionFromString method (state machine implementation)
	//-----------------------------------------------------------------------------------------------------------------

	static class A_getPrecisionFromString {

		private static final Input[] INPUT = {
			/* 01 */ input(1, "2011", ChronoField.YEAR),
			/* 02 */ input(2, "2024", ChronoField.YEAR),
			/* 03 */ input(3, "1999", ChronoField.YEAR),
			/* 04 */ input(4, "2011-01", MONTH_OF_YEAR),
			/* 05 */ input(5, "2024-12", MONTH_OF_YEAR),
			/* 06 */ input(6, "1999-06", MONTH_OF_YEAR),
			/* 07 */ input(7, "2011-01-01", ChronoField.DAY_OF_MONTH),
			/* 08 */ input(8, "2024-12-31", ChronoField.DAY_OF_MONTH),
			/* 09 */ input(9, "1999-06-15", ChronoField.DAY_OF_MONTH),
			/* 10 */ input(10, "2011-01-01T12", ChronoField.HOUR_OF_DAY),
			/* 11 */ input(11, "2024-12-31T23", ChronoField.HOUR_OF_DAY),
			/* 12 */ input(12, "1999-06-15T00", ChronoField.HOUR_OF_DAY),
			/* 13 */ input(13, "2011-01-01T12:30", MINUTE_OF_HOUR),
			/* 14 */ input(14, "2024-12-31T23:59", MINUTE_OF_HOUR),
			/* 15 */ input(15, "1999-06-15T00:00", MINUTE_OF_HOUR),
			/* 16 */ input(16, "2011-01-01T12:30:45", SECOND_OF_MINUTE),
			/* 17 */ input(17, "2024-12-31T23:59:59", SECOND_OF_MINUTE),
			/* 18 */ input(18, "1999-06-15T00:00:00", SECOND_OF_MINUTE),
			/* 19 */ input(19, "2011-01-01T12:30:45.123", MILLI_OF_SECOND),
			/* 20 */ input(20, "2024-12-31T23:59:59.999", MILLI_OF_SECOND),
			/* 21 */ input(21, "1999-06-15T00:00:00.000", MILLI_OF_SECOND),
			/* 22 */ input(22, "0000", ChronoField.YEAR),
			/* 23 */ input(23, "9999", ChronoField.YEAR),
			/* 24 */ input(24, "0000-01", MONTH_OF_YEAR),
			/* 25 */ input(25, "9999-12", MONTH_OF_YEAR),
			/* 26 */ input(26, "0000-01-01", ChronoField.DAY_OF_MONTH),
			/* 27 */ input(27, "9999-12-31", ChronoField.DAY_OF_MONTH),
			/* 28 */ input(28, "", MILLI_OF_SECOND),
			/* 35 */ input(35, "2011Z", ChronoField.YEAR),
			/* 36 */ input(36, "2011-01Z", MONTH_OF_YEAR),
			/* 37 */ input(37, "2011-01-01Z", ChronoField.DAY_OF_MONTH),
			/* 38 */ input(38, "2011-01-01T12Z", ChronoField.HOUR_OF_DAY),
			/* 39 */ input(39, "2011-01-01T12:30Z", MINUTE_OF_HOUR),
			/* 40 */ input(40, "2011-01-01T12:30:45Z", SECOND_OF_MINUTE),
			/* 41 */ input(41, "2011-01-01T12:30:45.123Z", MILLI_OF_SECOND)
		};

		private static Input input(int index, String dateString, ChronoField expectedPrecision) {
			return new Input(index, dateString, expectedPrecision);
		}

		private static class Input {
			final String dateString;
			final ChronoField expectedPrecision;

			public Input(int index, String dateString, ChronoField expectedPrecision) {
				this.dateString = dateString;
				this.expectedPrecision = expectedPrecision;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void a01_basic(Input input) {
			assertEquals(input.expectedPrecision, getPrecisionFromString(input.dateString));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toChronoField(ChronoUnit) tests
	//-----------------------------------------------------------------------------------------------------------------

	static class D_toChronoField {

		private static final Input[] INPUT = {
			/* 01 */ input(1, YEARS, ChronoField.YEAR),
			/* 02 */ input(2, MONTHS, MONTH_OF_YEAR),
			/* 03 */ input(3, DAYS, ChronoField.DAY_OF_MONTH),
			/* 04 */ input(4, HOURS, ChronoField.HOUR_OF_DAY),
			/* 05 */ input(5, MINUTES, MINUTE_OF_HOUR),
			/* 06 */ input(6, SECONDS, SECOND_OF_MINUTE),
			/* 07 */ input(7, MILLIS, MILLI_OF_SECOND),
			/* 08 */ input(8, NANOS, null),
			/* 09 */ input(9, MICROS, null),
			/* 10 */ input(10, WEEKS, null),
			/* 11 */ input(11, DECADES, null),
			/* 12 */ input(12, CENTURIES, null),
			/* 13 */ input(13, MILLENNIA, null),
			/* 14 */ input(14, ERAS, null)
		};

		private static Input input(int index, ChronoUnit unit, ChronoField expectedField) {
			return new Input(index, unit, expectedField);
		}

		private static class Input {
			final int index;
			final ChronoUnit unit;
			final ChronoField expectedField;

			public Input(int index, ChronoUnit unit, ChronoField expectedField) {
				this.index = index;
				this.unit = unit;
				this.expectedField = expectedField;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void d01_toChronoField(Input input) {
			ChronoField result = toChronoField(input.unit);
			assertEquals(input.expectedField, result, "Test " + input.index + ": " + input.unit);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toChronoUnit(ChronoField) tests
	//-----------------------------------------------------------------------------------------------------------------

	static class E_toChronoUnit {

		private static final Input[] INPUT = {
			/* 01 */ input(1, ChronoField.YEAR, YEARS),
			/* 02 */ input(2, MONTH_OF_YEAR, MONTHS),
			/* 03 */ input(3, ChronoField.DAY_OF_MONTH, DAYS),
			/* 04 */ input(4, ChronoField.HOUR_OF_DAY, HOURS),
			/* 05 */ input(5, MINUTE_OF_HOUR, MINUTES),
			/* 06 */ input(6, SECOND_OF_MINUTE, SECONDS),
			/* 07 */ input(7, MILLI_OF_SECOND, MILLIS),
			/* 08 */ input(8, ChronoField.DAY_OF_WEEK, null),
			/* 09 */ input(9, ChronoField.DAY_OF_YEAR, null),
			/* 11 */ input(11, ALIGNED_DAY_OF_WEEK_IN_MONTH, null),
			/* 12 */ input(12, ALIGNED_WEEK_OF_MONTH, null),
			/* 13 */ input(13, ALIGNED_WEEK_OF_YEAR, null),
			/* 14 */ input(14, NANO_OF_SECOND, null),
			/* 15 */ input(15, MICRO_OF_SECOND, null)
		};

		private static Input input(int index, ChronoField field, ChronoUnit expectedUnit) {
			return new Input(index, field, expectedUnit);
		}

		private static class Input {
			final int index;
			final ChronoField field;
			final ChronoUnit expectedUnit;

			public Input(int index, ChronoField field, ChronoUnit expectedUnit) {
				this.index = index;
				this.field = field;
				this.expectedUnit = expectedUnit;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void e01_toChronoUnit(Input input) {
			ChronoUnit result = toChronoUnit(input.field);
			assertEquals(input.expectedUnit, result, "Test " + input.index + ": " + input.field);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toCalendarField(ChronoField) tests
	//-----------------------------------------------------------------------------------------------------------------

	static class F_toCalendarField {

		private static final Input[] INPUT = {
			/* 01 */ input(1, ChronoField.YEAR, Calendar.YEAR),
			/* 02 */ input(2, MONTH_OF_YEAR, MONTH),
			/* 03 */ input(3, ChronoField.DAY_OF_MONTH, Calendar.DAY_OF_MONTH),
			/* 04 */ input(4, ChronoField.HOUR_OF_DAY, Calendar.HOUR_OF_DAY),
			/* 05 */ input(5, MINUTE_OF_HOUR, MINUTE),
			/* 06 */ input(6, SECOND_OF_MINUTE, SECOND),
			/* 07 */ input(7, MILLI_OF_SECOND, MILLISECOND),
			/* 08 */ input(8, ChronoField.DAY_OF_WEEK, MILLISECOND), // Should default to MILLISECOND
			/* 09 */ input(9, ChronoField.DAY_OF_YEAR, MILLISECOND), // Should default to MILLISECOND
			/* 11 */ input(11, NANO_OF_SECOND, MILLISECOND), // Should default to MILLISECOND
			/* 12 */ input(12, MICRO_OF_SECOND, MILLISECOND) // Should default to MILLISECOND
		};

		private static Input input(int index, ChronoField field, int expectedCalendarField) {
			return new Input(index, field, expectedCalendarField);
		}

		private static class Input {
			final int index;
			final ChronoField field;
			final int expectedCalendarField;

			public Input(int index, ChronoField field, int expectedCalendarField) {
				this.index = index;
				this.field = field;
				this.expectedCalendarField = expectedCalendarField;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void f01_toCalendarField(Input input) {
			int result = toCalendarField(input.field);
			assertEquals(input.expectedCalendarField, result, "Test " + input.index + ": " + input.field);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Round-trip conversion tests
	//-----------------------------------------------------------------------------------------------------------------

	static class G_roundTripConversions {

		private static final Input[] INPUT = {
			/* 01 */ input(1, ChronoField.YEAR),
			/* 02 */ input(2, MONTH_OF_YEAR),
			/* 03 */ input(3, ChronoField.DAY_OF_MONTH),
			/* 04 */ input(4, ChronoField.HOUR_OF_DAY),
			/* 05 */ input(5, MINUTE_OF_HOUR),
			/* 06 */ input(6, SECOND_OF_MINUTE),
			/* 07 */ input(7, MILLI_OF_SECOND)
		};

		private static Input input(int index, ChronoField field) {
			return new Input(index, field);
		}

		private static class Input {
			final int index;
			final ChronoField field;

			public Input(int index, ChronoField field) {
				this.index = index;
				this.field = field;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void g01_chronoFieldToChronoUnitToChronoField(Input input) {
			// ChronoField -> ChronoUnit -> ChronoField should be idempotent
			ChronoUnit unit = toChronoUnit(input.field);
			if (unit != null) {
				ChronoField result = toChronoField(unit);
				assertEquals(input.field, result, "Test " + input.index + ": " + input.field + " -> " + unit + " -> " + result);
			}
		}

		@ParameterizedTest
		@MethodSource("input")
		void g02_chronoFieldToCalendarField(Input input) {
			// ChronoField -> Calendar field should always work
			int calendarField = toCalendarField(input.field);
			assertTrue(calendarField >= 0, "Test " + input.index + ": Calendar field should be non-negative");
			assertTrue(calendarField <= 18, "Test " + input.index + ": Calendar field should be valid Calendar constant");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toIso8601(Calendar) tests
	//-----------------------------------------------------------------------------------------------------------------

	static class H_toIso8601 {

		private static final Input[] INPUT = {
			/* 01 */ input(1, 2024, JANUARY, 15, 14, 30, 45, 123, "UTC", "2024-01-15T14:30:45Z"),
			/* 02 */ input(2, 2024, JANUARY, 15, 14, 30, 45, 123, "America/New_York", "2024-01-15T14:30:45-05:00"),
			/* 03 */ input(3, 2024, JANUARY, 15, 14, 30, 45, 123, "America/Los_Angeles", "2024-01-15T14:30:45-08:00"),
			/* 04 */ input(4, 2024, JANUARY, 15, 14, 30, 45, 123, "Europe/London", "2024-01-15T14:30:45Z"),
			/* 05 */ input(5, 2024, JANUARY, 15, 14, 30, 45, 123, "Asia/Tokyo", "2024-01-15T14:30:45+09:00"),
			/* 06 */ input(6, 2024, JULY, 15, 14, 30, 45, 123, "America/New_York", "2024-07-15T14:30:45-04:00"), // DST
			/* 07 */ input(7, 2024, JULY, 15, 14, 30, 45, 123, "America/Los_Angeles", "2024-07-15T14:30:45-07:00"), // DST
			/* 08 */ input(8, 2024, JULY, 15, 14, 30, 45, 123, "Europe/London", "2024-07-15T14:30:45+01:00"), // DST
			/* 09 */ input(9, 2000, FEBRUARY, 29, 12, 0, 0, 0, "UTC", "2000-02-29T12:00:00Z"), // Leap year
			/* 10 */ input(10, 2024, DECEMBER, 31, 23, 59, 59, 999, "UTC", "2024-12-31T23:59:59Z"), // End of year
			/* 11 */ input(11, 2024, JANUARY, 1, 0, 0, 0, 0, "UTC", "2024-01-01T00:00:00Z"), // Start of year
			/* 12 */ input(12, 2024, JANUARY, 15, 0, 0, 0, 0, "UTC", "2024-01-15T00:00:00Z"), // Midnight
			/* 13 */ input(13, 2024, JANUARY, 15, 23, 59, 59, 999, "UTC", "2024-01-15T23:59:59Z"), // End of day
			/* 14 */ input(14, 2024, JANUARY, 15, 12, 0, 0, 0, "GMT+05:30", "2024-01-15T12:00:00+05:30"), // Custom offset
			/* 15 */ input(15, 2024, JANUARY, 15, 12, 0, 0, 0, "GMT-05:30", "2024-01-15T12:00:00-05:30") // Custom offset
		};

		private static Input input(int index, int year, int month, int day, int hour, int minute, int second, int millisecond, String timezone, String expectedIso8601) {
			return new Input(index, year, month, day, hour, minute, second, millisecond, timezone, expectedIso8601);
		}

		private static class Input {
			final int index;
			final int year;
			final int month;
			final int day;
			final int hour;
			final int minute;
			final int second;
			final int millisecond;
			final String timezone;
			final String expectedIso8601;

			public Input(int index, int year, int month, int day, int hour, int minute, int second, int millisecond, String timezone, String expectedIso8601) {
				this.index = index;
				this.year = year;
				this.month = month;
				this.day = day;
				this.hour = hour;
				this.minute = minute;
				this.second = second;
				this.millisecond = millisecond;
				this.timezone = timezone;
				this.expectedIso8601 = expectedIso8601;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void h01_toIso8601(Input input) {
			// Create Calendar with specified timezone and date/time
			var cal = Calendar.getInstance(TimeZone.getTimeZone(input.timezone));
			cal.set(input.year, input.month, input.day, input.hour, input.minute, input.second);
			cal.set(Calendar.MILLISECOND, input.millisecond);

			// Convert to ISO8601 string
			String result = toIso8601(cal);

			// Verify the result matches expected format
			assertEquals(input.expectedIso8601, result, "Test " + input.index + ": " + input.year + "-" + (input.month + 1) + "-" + input.day + " " + input.timezone);
		}

		@ParameterizedTest
		@MethodSource("input")
		void h02_toIso8601_formatValidation(Input input) {
			// Create Calendar with specified timezone and date/time
			var cal = Calendar.getInstance(TimeZone.getTimeZone(input.timezone));
			cal.set(input.year, input.month, input.day, input.hour, input.minute, input.second);
			cal.set(Calendar.MILLISECOND, input.millisecond);

			// Convert to ISO8601 string
			String result = toIso8601(cal);

			// Validate format structure
			assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}|\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
				"Test " + input.index + ": Result should match ISO8601 format: " + result);

			// Validate timezone format
			if (result.endsWith("Z")) {
				// UTC timezone
				assertTrue(result.endsWith("Z"), "Test " + input.index + ": UTC timezone should end with 'Z'");
			} else {
				// Offset timezone
				assertTrue(result.matches(".*[+-]\\d{2}:\\d{2}$"), "Test " + input.index + ": Offset timezone should end with +/-HH:MM");
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// toIso8601(Calendar) edge cases and error handling
	//-----------------------------------------------------------------------------------------------------------------

	static class I_toIso8601_edgeCases {

		@Test
		void i01_nullCalendar() {
			assertThrows(NullPointerException.class, () -> {
				toIso8601(null);
			});
		}

		@Test
		void i02_minimumDate() {
			var cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.set(1, Calendar.JANUARY, 1, 0, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);

			String result = toIso8601(cal);
			assertEquals("0001-01-01T00:00:00Z", result);
		}

		@Test
		void i03_maximumDate() {
			var cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.set(9999, Calendar.DECEMBER, 31, 23, 59, 59);
			cal.set(Calendar.MILLISECOND, 999);

			String result = toIso8601(cal);
			assertEquals("9999-12-31T23:59:59Z", result);
		}

		@Test
		void i04_leapYear() {
			var cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.set(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);

			String result = toIso8601(cal);
			assertEquals("2024-02-29T12:00:00Z", result);
		}

		@Test
		void i05_nonLeapYear() {
			var cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.set(2023, Calendar.FEBRUARY, 28, 12, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);

			String result = toIso8601(cal);
			assertEquals("2023-02-28T12:00:00Z", result);
		}

		@Test
		void i06_dstTransition() {
			// Test DST transition in America/New_York (Spring forward)
			var cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
			cal.set(2024, Calendar.MARCH, 10, 2, 30, 0); // 2:30 AM on DST transition day
			cal.set(Calendar.MILLISECOND, 0);

			String result = toIso8601(cal);
			// The exact result depends on how Java handles the DST transition
			assertTrue(result.contains("2024-03-10T"), "Should contain the date");
			assertTrue(result.contains(":30:00"), "Should contain the time");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// fromIso8601Calendar(String) tests
	//-----------------------------------------------------------------------------------------------------------------

	static class J_fromIso8601Calendar {

		private static final Input[] INPUT = {
			/* 01 */ input(1, "2024-01-15T14:30:45Z", "UTC", 2024, JANUARY, 15, 14, 30, 45),
			/* 02 */ input(2, "2024-01-15T14:30:45-05:00", "GMT-05:00", 2024, JANUARY, 15, 14, 30, 45),
			/* 03 */ input(3, "2024-01-15T14:30:45+09:00", "GMT+09:00", 2024, JANUARY, 15, 14, 30, 45),
			/* 04 */ input(4, "2024-01-15", "System", 2024, JANUARY, 15, 0, 0, 0),
			/* 05 */ input(5, "2024-01-15T14:30", "System", 2024, JANUARY, 15, 14, 30, 0),
			/* 06 */ input(6, "2024-01-15T14:30:45.123Z", "UTC", 2024, JANUARY, 15, 14, 30, 45),
			/* 07 */ input(7, "2024-07-15T14:30:45-04:00", "GMT-04:00", 2024, JULY, 15, 14, 30, 45), // DST
			/* 08 */ input(8, "2024-07-15T14:30:45-07:00", "GMT-07:00", 2024, JULY, 15, 14, 30, 45), // DST
			/* 09 */ input(9, "2024-02-29T12:00:00Z", "UTC", 2024, FEBRUARY, 29, 12, 0, 0), // Leap year
			/* 10 */ input(10, "2024-12-31T23:59:59Z", "UTC", 2024, DECEMBER, 31, 23, 59, 59), // End of year
			/* 11 */ input(11, "2024-01-01T00:00:00Z", "UTC", 2024, JANUARY, 1, 0, 0, 0), // Start of year
			/* 12 */ input(12, "2024-01-15T00:00:00Z", "UTC", 2024, JANUARY, 15, 0, 0, 0), // Midnight
			/* 13 */ input(13, "2024-01-15T23:59:59Z", "UTC", 2024, JANUARY, 15, 23, 59, 59), // End of day
			/* 14 */ input(14, "2024-01-15T12:00:00+05:30", "GMT+05:30", 2024, JANUARY, 15, 12, 0, 0), // Custom offset
			/* 15 */ input(15, "2024-01-15T12:00:00-05:30", "GMT-05:30", 2024, JANUARY, 15, 12, 0, 0) // Custom offset
		};

		private static Input input(int index, String iso8601String, String expectedTimezone, int year, int month, int day, int hour, int minute, int second) {
			return new Input(index, iso8601String, expectedTimezone, year, month, day, hour, minute, second);
		}

		private static class Input {
			final int index;
			final String iso8601String;
			final String expectedTimezone;
			final int year;
			final int month;
			final int day;
			final int hour;
			final int minute;
			final int second;

			public Input(int index, String iso8601String, String expectedTimezone, int year, int month, int day, int hour, int minute, int second) {
				this.index = index;
				this.iso8601String = iso8601String;
				this.expectedTimezone = expectedTimezone;
				this.year = year;
				this.month = month;
				this.day = day;
				this.hour = hour;
				this.minute = minute;
				this.second = second;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void j01_fromIso8601Calendar(Input input) {
			// Parse the ISO8601 string
			Calendar result = fromIso8601Calendar(input.iso8601String);

			// Verify the result is not null
			assertNotNull(result, "Test " + input.index + ": Result should not be null");

			// Verify date components
			assertEquals(input.year, result.get(Calendar.YEAR), "Test " + input.index + ": Year should match");
			assertEquals(input.month, result.get(Calendar.MONTH), "Test " + input.index + ": Month should match");
			assertEquals(input.day, result.get(Calendar.DAY_OF_MONTH), "Test " + input.index + ": Day should match");
			assertEquals(input.hour, result.get(Calendar.HOUR_OF_DAY), "Test " + input.index + ": Hour should match");
			assertEquals(input.minute, result.get(Calendar.MINUTE), "Test " + input.index + ": Minute should match");
			assertEquals(input.second, result.get(Calendar.SECOND), "Test " + input.index + ": Second should match");

			// Verify timezone (for non-system timezones)
			if (!"System".equals(input.expectedTimezone)) {
				var expectedTz = TimeZone.getTimeZone(input.expectedTimezone);
				assertEquals(expectedTz.getID(), result.getTimeZone().getID(), "Test " + input.index + ": Timezone should match");
			}
		}

		@ParameterizedTest
		@MethodSource("input")
		void j02_fromIso8601Calendar_roundTrip(Input input) {
			// Parse the ISO8601 string
			Calendar cal = fromIso8601Calendar(input.iso8601String);
			assertNotNull(cal, "Test " + input.index + ": Calendar should not be null");

			// Convert back to ISO8601
			String result = toIso8601(cal);

			// The result should be a valid ISO8601 string
			assertNotNull(result, "Test " + input.index + ": Result should not be null");
			assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}|\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
				"Test " + input.index + ": Result should be valid ISO8601 format: " + result);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// fromIso8601(String) tests
	//-----------------------------------------------------------------------------------------------------------------

	static class K_fromIso8601 {

		private static final Input[] INPUT = {
			/* 01 */ input(1, "2024-01-15T14:30:45Z", "Z", 2024, 1, 15, 14, 30, 45),
			/* 02 */ input(2, "2024-01-15T14:30:45-05:00", "-05:00", 2024, 1, 15, 14, 30, 45),
			/* 03 */ input(3, "2024-01-15T14:30:45+09:00", "+09:00", 2024, 1, 15, 14, 30, 45),
			/* 04 */ input(4, "2024-01-15", "System", 2024, 1, 15, 0, 0, 0),
			/* 05 */ input(5, "2024-01-15T14:30", "System", 2024, 1, 15, 14, 30, 0),
			/* 06 */ input(6, "2024-01-15T14:30:45.123Z", "Z", 2024, 1, 15, 14, 30, 45),
			/* 07 */ input(7, "2024-07-15T14:30:45-04:00", "-04:00", 2024, 7, 15, 14, 30, 45), // DST
			/* 08 */ input(8, "2024-07-15T14:30:45-07:00", "-07:00", 2024, 7, 15, 14, 30, 45), // DST
			/* 09 */ input(9, "2024-02-29T12:00:00Z", "Z", 2024, 2, 29, 12, 0, 0), // Leap year
			/* 10 */ input(10, "2024-12-31T23:59:59Z", "Z", 2024, 12, 31, 23, 59, 59), // End of year
			/* 11 */ input(11, "2024-01-01T00:00:00Z", "Z", 2024, 1, 1, 0, 0, 0), // Start of year
			/* 12 */ input(12, "2024-01-15T00:00:00Z", "Z", 2024, 1, 15, 0, 0, 0), // Midnight
			/* 13 */ input(13, "2024-01-15T23:59:59Z", "Z", 2024, 1, 15, 23, 59, 59), // End of day
			/* 14 */ input(14, "2024-01-15T12:00:00+05:30", "+05:30", 2024, 1, 15, 12, 0, 0), // Custom offset
			/* 15 */ input(15, "2024-01-15T12:00:00-05:30", "-05:30", 2024, 1, 15, 12, 0, 0) // Custom offset
		};

		private static Input input(int index, String iso8601String, String expectedTimezone, int year, int month, int day, int hour, int minute, int second) {
			return new Input(index, iso8601String, expectedTimezone, year, month, day, hour, minute, second);
		}

		private static class Input {
			final int index;
			final String iso8601String;
			final String expectedTimezone;
			final int year;
			final int month;
			final int day;
			final int hour;
			final int minute;
			final int second;

			public Input(int index, String iso8601String, String expectedTimezone, int year, int month, int day, int hour, int minute, int second) {
				this.index = index;
				this.iso8601String = iso8601String;
				this.expectedTimezone = expectedTimezone;
				this.year = year;
				this.month = month;
				this.day = day;
				this.hour = hour;
				this.minute = minute;
				this.second = second;
			}
		}

		static Input[] input() {
			return INPUT;
		}

		@ParameterizedTest
		@MethodSource("input")
		void k01_fromIso8601(Input input) {
			// Parse the ISO8601 string
			ZonedDateTime result = fromIso8601(input.iso8601String);

			// Verify the result is not null
			assertNotNull(result, "Test " + input.index + ": Result should not be null");

			// Verify date components
			assertEquals(input.year, result.getYear(), "Test " + input.index + ": Year should match");
			assertEquals(input.month, result.getMonthValue(), "Test " + input.index + ": Month should match");
			assertEquals(input.day, result.getDayOfMonth(), "Test " + input.index + ": Day should match");
			assertEquals(input.hour, result.getHour(), "Test " + input.index + ": Hour should match");
			assertEquals(input.minute, result.getMinute(), "Test " + input.index + ": Minute should match");
			assertEquals(input.second, result.getSecond(), "Test " + input.index + ": Second should match");

			// Verify timezone (for non-system timezones)
			if (!"System".equals(input.expectedTimezone)) {
				var expectedZone = ZoneId.of(input.expectedTimezone);
				assertEquals(expectedZone, result.getZone(), "Test " + input.index + ": Timezone should match");
			}
		}

		@ParameterizedTest
		@MethodSource("input")
		void k02_fromIso8601_immutability(Input input) {
			// Parse the ISO8601 string
			ZonedDateTime result = fromIso8601(input.iso8601String);
			assertNotNull(result, "Test " + input.index + ": Result should not be null");

			// Verify immutability - operations should return new instances
			ZonedDateTime plusOneDay = result.plusDays(1);
			assertNotSame(result, plusOneDay, "Test " + input.index + ": Plus operation should return new instance");
			assertNotEquals(result, plusOneDay, "Test " + input.index + ": Plus operation should change the value");

			ZonedDateTime minusOneDay = result.minusDays(1);
			assertNotSame(result, minusOneDay, "Test " + input.index + ": Minus operation should return new instance");
			assertNotEquals(result, minusOneDay, "Test " + input.index + ": Minus operation should change the value");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// fromIso8601Calendar and fromIso8601 edge cases and error handling
	//-----------------------------------------------------------------------------------------------------------------

	static class L_fromIso8601_edgeCases {

		@Test
		void l01_nullInput() {
			assertNull(fromIso8601Calendar(null));
			assertNull(fromIso8601(null));
		}

		@Test
		void l02_emptyInput() {
			assertNull(fromIso8601Calendar(""));
			assertNull(fromIso8601(""));
		}

		@Test
		void l03_whitespaceInput() {
			assertNull(fromIso8601Calendar("   "));
			assertNull(fromIso8601("   "));
		}

		@Test
		void l04_invalidFormat() {
			// These should throw DateTimeParseException
			assertThrows(Exception.class, () -> {
				fromIso8601Calendar("invalid-date");
			});
			assertThrows(Exception.class, () -> {
				fromIso8601("invalid-date");
			});
		}

		@Test
		void l05_minimumDate() {
			Calendar cal = fromIso8601Calendar("0001-01-01T00:00:00Z");
			assertNotNull(cal);
			assertEquals(1, cal.get(Calendar.YEAR));
			assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
			assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = fromIso8601("0001-01-01T00:00:00Z");
			assertNotNull(zdt);
			assertEquals(1, zdt.getYear());
			assertEquals(1, zdt.getMonthValue());
			assertEquals(1, zdt.getDayOfMonth());
		}

		@Test
		void l06_maximumDate() {
			Calendar cal = fromIso8601Calendar("9999-12-31T23:59:59Z");
			assertNotNull(cal);
			assertEquals(9999, cal.get(Calendar.YEAR));
			assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
			assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = fromIso8601("9999-12-31T23:59:59Z");
			assertNotNull(zdt);
			assertEquals(9999, zdt.getYear());
			assertEquals(12, zdt.getMonthValue());
			assertEquals(31, zdt.getDayOfMonth());
		}

		@Test
		void l07_leapYear() {
			Calendar cal = fromIso8601Calendar("2024-02-29T12:00:00Z");
			assertNotNull(cal);
			assertEquals(2024, cal.get(Calendar.YEAR));
			assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
			assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = fromIso8601("2024-02-29T12:00:00Z");
			assertNotNull(zdt);
			assertEquals(2024, zdt.getYear());
			assertEquals(2, zdt.getMonthValue());
			assertEquals(29, zdt.getDayOfMonth());
		}

		@Test
		void l08_dstTransition() {
			// Test DST transition in America/New_York (Spring forward)
			Calendar cal = fromIso8601Calendar("2024-03-10T02:30:00-05:00");
			assertNotNull(cal);
			assertEquals(2024, cal.get(Calendar.YEAR));
			assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
			assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = fromIso8601("2024-03-10T02:30:00-05:00");
			assertNotNull(zdt);
			assertEquals(2024, zdt.getYear());
			assertEquals(3, zdt.getMonthValue());
			assertEquals(10, zdt.getDayOfMonth());
		}
	}

	//====================================================================================================
	// addSubtractDays / add / toZonedDateTime
	//====================================================================================================
	@Test
	void test_addSubtractDays() {
		var cal = Calendar.getInstance();
		cal.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);

		Calendar result = addSubtractDays(cal, 10);
		assertNotNull(result);
		assertNotSame(cal, result); // Should be a clone
		assertEquals(25, result.get(Calendar.DAY_OF_MONTH));

		result = addSubtractDays(cal, -5);
		assertNotNull(result);
		assertEquals(10, result.get(Calendar.DAY_OF_MONTH));

		// Null calendar
		assertNull(addSubtractDays(null, 10));
	}

	@Test
	void test_add() {
		var cal = Calendar.getInstance();
		cal.set(2024, Calendar.JANUARY, 15, 12, 30, 45);
		cal.set(Calendar.MILLISECOND, 0);

		// Add days
		Calendar result = add(cal, Calendar.DAY_OF_MONTH, 5);
		assertSame(cal, result); // Returns same instance
		assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));

		// Add months
		cal.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
		add(cal, Calendar.MONTH, 2);
		assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));

		// Add hours
		cal.set(2024, Calendar.JANUARY, 15, 10, 0, 0);
		add(cal, Calendar.HOUR_OF_DAY, 5);
		assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
	}

	@Test
	void test_toZonedDateTime() {
		var cal = new GregorianCalendar(2024, Calendar.JANUARY, 15, 12, 30, 45);

		Optional<ZonedDateTime> result = toZonedDateTime(cal);
		assertTrue(result.isPresent());

		ZonedDateTime zdt = result.get();
		assertEquals(2024, zdt.getYear());
		assertEquals(1, zdt.getMonthValue());
		assertEquals(15, zdt.getDayOfMonth());
		assertEquals(12, zdt.getHour());
		assertEquals(30, zdt.getMinute());
		assertEquals(45, zdt.getSecond());

		// Null calendar
		assertFalse(toZonedDateTime(null).isPresent());
	}

	@Test
	void test_toZonedDateTime_preservesTimezone() {
		var tz = TimeZone.getTimeZone("America/New_York");
		Calendar cal = new GregorianCalendar(tz);
		cal.set(2024, Calendar.JANUARY, 15, 12, 30, 45);

		Optional<ZonedDateTime> result = toZonedDateTime(cal);
		assertTrue(result.isPresent());

		ZonedDateTime zdt = result.get();
		assertEquals(tz.toZoneId(), zdt.getZone());
	}
}