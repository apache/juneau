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
import static java.util.Calendar.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
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
	// Test getPrecisionFromString method (now in GranularZonedDateTime)
	//-----------------------------------------------------------------------------------------------------------------
	// Note: getPrecisionFromString has been moved to GranularZonedDateTime as a private method.
	// Tests for this functionality are now in GranularZonedDateTime_Test via the parse() method.

	//-----------------------------------------------------------------------------------------------------------------
	// toChronoUnit(ChronoField) tests (now in GranularZonedDateTime)
	//-----------------------------------------------------------------------------------------------------------------
	// Note: toChronoUnit has been moved to GranularZonedDateTime as a private method.
	// Tests for this functionality are now in GranularZonedDateTime_Test via the roll() method.

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

	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper method for converting Calendar to ISO8601 string
	//-----------------------------------------------------------------------------------------------------------------

	private static String toIso8601(Calendar c) {
		var sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		sdf.setTimeZone(c.getTimeZone());
		return sdf.format(c.getTime());
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
			String s = input.iso8601String;
			Calendar result = opt(s).filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.parse(s).getZonedDateTime()).map(GregorianCalendar::from).orElse(null);

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
			ZonedDateTime result = GranularZonedDateTime.parse(input.iso8601String).getZonedDateTime();

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
			ZonedDateTime result = GranularZonedDateTime.parse(input.iso8601String).getZonedDateTime();
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
		void l04_invalidFormat() {
			// These should throw DateTimeParseException
			assertThrows(Exception.class, () -> {
				opt("invalid-date").filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.parse("invalid-date").getZonedDateTime()).map(GregorianCalendar::from).orElse(null);
			});
			assertThrows(Exception.class, () -> {
				GranularZonedDateTime.parse("invalid-date").getZonedDateTime();
			});
		}

		@Test
		void l05_minimumDate() {
			Calendar cal = opt("0001-01-01T00:00:00Z").filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.parse("0001-01-01T00:00:00Z").getZonedDateTime()).map(GregorianCalendar::from).orElse(null);
			assertNotNull(cal);
			assertEquals(1, cal.get(Calendar.YEAR));
			assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
			assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = GranularZonedDateTime.parse("0001-01-01T00:00:00Z").getZonedDateTime();
			assertNotNull(zdt);
			assertEquals(1, zdt.getYear());
			assertEquals(1, zdt.getMonthValue());
			assertEquals(1, zdt.getDayOfMonth());
		}

		@Test
		void l06_maximumDate() {
			Calendar cal = opt("9999-12-31T23:59:59Z").filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.parse("9999-12-31T23:59:59Z").getZonedDateTime()).map(GregorianCalendar::from).orElse(null);
			assertNotNull(cal);
			assertEquals(9999, cal.get(Calendar.YEAR));
			assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
			assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = GranularZonedDateTime.parse("9999-12-31T23:59:59Z").getZonedDateTime();
			assertNotNull(zdt);
			assertEquals(9999, zdt.getYear());
			assertEquals(12, zdt.getMonthValue());
			assertEquals(31, zdt.getDayOfMonth());
		}

		@Test
		void l07_leapYear() {
			Calendar cal = opt("2024-02-29T12:00:00Z").filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.parse("2024-02-29T12:00:00Z").getZonedDateTime()).map(GregorianCalendar::from).orElse(null);
			assertNotNull(cal);
			assertEquals(2024, cal.get(Calendar.YEAR));
			assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
			assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = GranularZonedDateTime.parse("2024-02-29T12:00:00Z").getZonedDateTime();
			assertNotNull(zdt);
			assertEquals(2024, zdt.getYear());
			assertEquals(2, zdt.getMonthValue());
			assertEquals(29, zdt.getDayOfMonth());
		}

		@Test
		void l08_dstTransition() {
			// Test DST transition in America/New_York (Spring forward)
			Calendar cal = opt("2024-03-10T02:30:00-05:00").filter(x1 -> ! isBlank(x1)).map(x -> GranularZonedDateTime.parse("2024-03-10T02:30:00-05:00").getZonedDateTime()).map(GregorianCalendar::from).orElse(null);
			assertNotNull(cal);
			assertEquals(2024, cal.get(Calendar.YEAR));
			assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
			assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));

			ZonedDateTime zdt = GranularZonedDateTime.parse("2024-03-10T02:30:00-05:00").getZonedDateTime();
			assertNotNull(zdt);
			assertEquals(2024, zdt.getYear());
			assertEquals(3, zdt.getMonthValue());
			assertEquals(10, zdt.getDayOfMonth());
		}
	}
}