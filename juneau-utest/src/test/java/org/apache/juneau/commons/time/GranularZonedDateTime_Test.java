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
package org.apache.juneau.commons.time;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.*;
import java.util.Date;

import org.apache.juneau.*;
import org.apache.juneau.utest.utils.FakeTimeProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

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
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		assertEquals(ZoneId.of("America/New_York"), gdt.zdt.getZone());
	}

	@Test
	void b03_constructorWithZonedDateTime_preservesPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MINUTE_OF_HOUR);
		assertEquals(ChronoField.MINUTE_OF_HOUR, gdt.precision);
	}

	@Test
	void b04_ofZonedDateTime() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = GranularZonedDateTime.of(zdt, ChronoField.HOUR_OF_DAY);
		assertEquals(zdt, gdt.zdt);
		assertEquals(ChronoField.HOUR_OF_DAY, gdt.precision);
	}

	@Test
	void b05_ofDate() {
		var date = new Date(1705322445000L); // 2024-01-15T12:30:45Z
		var gdt = GranularZonedDateTime.of(date, ChronoField.SECOND_OF_MINUTE);
		assertEquals(ChronoField.SECOND_OF_MINUTE, gdt.precision);
		// Verify the instant is correct (date uses system default timezone)
		assertEquals(date.toInstant(), gdt.zdt.toInstant());
	}

	@Test
	void b06_ofDateWithZoneId() {
		var date = new Date(1705322445000L); // 2024-01-15T12:30:45Z
		var zoneId = ZoneId.of("America/New_York");
		var gdt = GranularZonedDateTime.of(date, ChronoField.SECOND_OF_MINUTE, zoneId);
		assertEquals(ChronoField.SECOND_OF_MINUTE, gdt.precision);
		assertEquals(zoneId, gdt.zdt.getZone());
		assertEquals(2024, gdt.zdt.getYear());
		assertEquals(1, gdt.zdt.getMonthValue());
		assertEquals(15, gdt.zdt.getDayOfMonth());
	}

	//====================================================================================================
	// copy() tests
	//====================================================================================================

	@Test
	void c01_copy() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt1 = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		var gdt2 = gdt1.copy();
		assertNotSame(gdt1, gdt2);
		assertEquals(gdt1.zdt, gdt2.zdt);
		assertEquals(gdt1.precision, gdt2.precision);
	}

	//====================================================================================================
	// getZonedDateTime() tests
	//====================================================================================================

	@Test
	void d01_getZonedDateTime() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		assertEquals(zdt, gdt.getZonedDateTime());
	}

	@Test
	void d02_getPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.MINUTE_OF_HOUR);
		assertEquals(ChronoField.MINUTE_OF_HOUR, gdt.getPrecision());
	}

	//====================================================================================================
	// roll(ChronoField, int) tests
	//====================================================================================================

	@Test
	void e01_roll_withField() {
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

		var rolled8 = gdt.roll(ChronoField.NANO_OF_SECOND, 1);
		assertEquals(zdt.plus(1, ChronoUnit.NANOS), rolled8.zdt);
	}

	@Test
	void e02_roll_withNanoOfSecondPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 123456789, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.NANO_OF_SECOND);

		var rolled = gdt.roll(ChronoField.NANO_OF_SECOND, 100);
		assertEquals(zdt.plus(100, ChronoUnit.NANOS), rolled.zdt);
		assertEquals(ChronoField.NANO_OF_SECOND, rolled.precision);
	}

	@Test
	void e03_roll_withUnsupportedField() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);

		assertThrowsWithMessage(IllegalArgumentException.class,
			"Unsupported roll field: AmPmOfDay",
			() -> gdt.roll(ChronoField.AMPM_OF_DAY, 1));
	}

	//====================================================================================================
	// roll(int) tests
	//====================================================================================================

	@Test
	void f01_roll_withPrecision() {
		var zdt = ZonedDateTime.of(2024, 1, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		var gdt = new GranularZonedDateTime(zdt, ChronoField.HOUR_OF_DAY);
		var rolled = gdt.roll(2);
		assertEquals(zdt.plusHours(2), rolled.zdt);
		assertEquals(ChronoField.HOUR_OF_DAY, rolled.precision);
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================

	@Test
	void h01_rollAndof() {
		var gdt1 = GranularZonedDateTime.of("2011", FAKE_TIME_PROVIDER);
		var gdt2 = gdt1.roll(1);
		assertEquals(2012, gdt2.zdt.getYear());
		assertEquals(ChronoField.YEAR, gdt2.precision);
	}

	@Test
	void h02_rollMultipleTimes() {
		var gdt = GranularZonedDateTime.of("2011-01-15", FAKE_TIME_PROVIDER);
		var rolled1 = gdt.roll(1);
		var rolled2 = rolled1.roll(1);
		assertEquals(17, rolled2.zdt.getDayOfMonth());
	}

	@Test
	void h03_rollWithDifferentField() {
		var gdt = GranularZonedDateTime.of("2011-01-15T12:30Z", FAKE_TIME_PROVIDER);
		// Roll by hours even though precision is minutes
		var rolled = gdt.roll(ChronoField.HOUR_OF_DAY, 2);
		assertEquals(14, rolled.zdt.getHour());
		assertEquals(30, rolled.zdt.getMinute());
		assertEquals(ChronoField.MINUTE_OF_HOUR, rolled.precision); // Original precision preserved
	}

	@Test
	void h04_copyAndRoll() {
		var gdt1 = GranularZonedDateTime.of("2011-01-15", FAKE_TIME_PROVIDER);
		var gdt2 = gdt1.copy();
		var rolled = gdt2.roll(1);
		// Original should be unchanged
		assertEquals(15, gdt1.zdt.getDayOfMonth());
		assertEquals(16, rolled.zdt.getDayOfMonth());
	}

	//====================================================================================================
	// of(String) and of(String, ZoneId) tests - see J_parserTests nested class below
	//====================================================================================================

	private static final FakeTimeProvider FAKE_TIME_PROVIDER = new FakeTimeProvider();

	@Nested class J_parserTests extends TestBase {

		record ParseTest(int index, String name, String input, String expected, ZoneId defaultZoneId) {
			ParseTest(int index, String name, String input, String expected) {
				this(index, name, input, expected, null);
			}
		}

		static ParseTest[] parseTests() {
			return new ParseTest[] {
				// Date formats
				new ParseTest(1, "yearOnly", "2011", "2011-01-01T00:00:00Z(Year)"),
				new ParseTest(2, "yearMonth", "2011-01", "2011-01-01T00:00:00Z(MonthOfYear)"),
				new ParseTest(3, "date", "2011-01-15", "2011-01-15T00:00:00Z(DayOfMonth)"),

				// DateTime formats
				new ParseTest(4, "dateTime_hour", "2011-01-15T12", "2011-01-15T12:00:00Z(HourOfDay)"),
				new ParseTest(5, "dateTime_minute", "2011-01-15T12:30", "2011-01-15T12:30:00Z(MinuteOfHour)"),
				new ParseTest(6, "dateTime_second", "2011-01-15T12:30:45", "2011-01-15T12:30:45Z(SecondOfMinute)"),

				// With fractional seconds
				new ParseTest(7, "dateTime_millisecond_dot", "2011-01-15T12:30:45.123", "2011-01-15T12:30:45.123Z(MilliOfSecond)"),
				new ParseTest(8, "dateTime_millisecond_comma", "2011-01-15T12:30:45,123", "2011-01-15T12:30:45.123Z(MilliOfSecond)"),
				new ParseTest(9, "dateTime_nanosecond_1digit", "2011-01-15T12:30:45.1", "2011-01-15T12:30:45.1Z(MilliOfSecond)"),
				new ParseTest(10, "dateTime_nanosecond_2digits", "2011-01-15T12:30:45.12", "2011-01-15T12:30:45.12Z(MilliOfSecond)"),
				new ParseTest(11, "dateTime_nanosecond_3digits", "2011-01-15T12:30:45.123", "2011-01-15T12:30:45.123Z(MilliOfSecond)"),
				new ParseTest(12, "dateTime_nanosecond_4digits", "2011-01-15T12:30:45.1234", "2011-01-15T12:30:45.1234Z(NanoOfSecond)"),
				new ParseTest(13, "dateTime_nanosecond_5digits", "2011-01-15T12:30:45.12345", "2011-01-15T12:30:45.12345Z(NanoOfSecond)"),
				new ParseTest(14, "dateTime_nanosecond_6digits", "2011-01-15T12:30:45.123456", "2011-01-15T12:30:45.123456Z(NanoOfSecond)"),
				new ParseTest(15, "dateTime_nanosecond_7digits", "2011-01-15T12:30:45.1234567", "2011-01-15T12:30:45.1234567Z(NanoOfSecond)"),
				new ParseTest(16, "dateTime_nanosecond_8digits", "2011-01-15T12:30:45.12345678", "2011-01-15T12:30:45.12345678Z(NanoOfSecond)"),
				new ParseTest(17, "dateTime_nanosecond_9digits", "2011-01-15T12:30:45.123456789", "2011-01-15T12:30:45.123456789Z(NanoOfSecond)"),

				// Time-only formats (use fixed current time: 2000-01-01T12:00:00Z)
				new ParseTest(18, "timeOnly_hour", "T12", "2000-01-01T12:00:00Z(HourOfDay)"),
				new ParseTest(19, "timeOnly_minute", "T12:30", "2000-01-01T12:30:00Z(MinuteOfHour)"),
				new ParseTest(20, "timeOnly_second", "T12:30:45", "2000-01-01T12:30:45Z(SecondOfMinute)"),
				new ParseTest(21, "timeOnly_millisecond", "T12:30:45.123", "2000-01-01T12:30:45.123Z(MilliOfSecond)"),
				new ParseTest(22, "timeOnly_withZ", "T12:30:45Z", "2000-01-01T12:30:45Z(SecondOfMinute)"),
				new ParseTest(23, "timeOnly_withOffset", "T12:30:45+05:30", "2000-01-01T12:30:45+05:30(SecondOfMinute)"),

				// With UTC timezone
				new ParseTest(24, "withZ", "2011-01-15T12:30:45Z", "2011-01-15T12:30:45Z(SecondOfMinute)"),
				new ParseTest(25, "yearWithZ", "2011Z", "2011-01-01T00:00:00Z(Year)"),
				new ParseTest(26, "yearMonthWithZ", "2011-01Z", "2011-01-01T00:00:00Z(MonthOfYear)"),
				new ParseTest(27, "dateWithZ", "2011-01-15Z", "2011-01-15T00:00:00Z(DayOfMonth)"),
				new ParseTest(28, "hourWithZ", "2011-01-15T12Z", "2011-01-15T12:00:00Z(HourOfDay)"),
				new ParseTest(29, "minuteWithZ", "2011-01-15T12:30Z", "2011-01-15T12:30:00Z(MinuteOfHour)"),
				new ParseTest(30, "millisecondWithZ", "2011-01-15T12:30:45.123Z", "2011-01-15T12:30:45.123Z(MilliOfSecond)"),

				// With offset (hours only)
				new ParseTest(31, "offset_plusHH", "2011-01-15T12:30:45+05", "2011-01-15T12:30:45+05:00(SecondOfMinute)"),
				new ParseTest(32, "offset_minusHH", "2011-01-15T12:30:45-05", "2011-01-15T12:30:45-05:00(SecondOfMinute)"),

				// With offset (hours and minutes, compact)
				new ParseTest(33, "offset_plusHHMM", "2011-01-15T12:30:45+0530", "2011-01-15T12:30:45+05:30(SecondOfMinute)"),
				new ParseTest(34, "offset_minusHHMM", "2011-01-15T12:30:45-0530", "2011-01-15T12:30:45-05:30(SecondOfMinute)"),

				// With offset (hours and minutes, with colon)
				new ParseTest(35, "offset_plusHH_MM", "2011-01-15T12:30:45+05:30", "2011-01-15T12:30:45+05:30(SecondOfMinute)"),
				new ParseTest(36, "offset_minusHH_MM", "2011-01-15T12:30:45-05:30", "2011-01-15T12:30:45-05:30(SecondOfMinute)"),
				new ParseTest(37, "offset_minus12_30", "2011-01-15T12:30:45-12:30", "2011-01-15T12:30:45-12:30(SecondOfMinute)"),
				new ParseTest(38, "offset_plus09_00", "2011-01-15T12:30:45+09:00", "2011-01-15T12:30:45+09:00(SecondOfMinute)"),

				// Timezone after various components
				new ParseTest(39, "yearFollowedByT", "2011T12", "2011-01-01T12:00:00Z(HourOfDay)"),
				new ParseTest(40, "yearFollowedByPlus", "2011+05", "2011-01-01T00:00:00+05:00(Year)"),
				new ParseTest(41, "yearFollowedByMinus", "2011-01-15T12-05", "2011-01-15T12:00:00-05:00(HourOfDay)"),
				new ParseTest(42, "monthFollowedByZ", "2011-01Z", "2011-01-01T00:00:00Z(MonthOfYear)"),
				new ParseTest(43, "monthFollowedByPlus", "2011-01+05", "2011-01-01T00:00:00+05:00(MonthOfYear)"),
				new ParseTest(44, "monthFollowedByMinus", "2011-01-15T12:30-05", "2011-01-15T12:30:00-05:00(MinuteOfHour)"),
				new ParseTest(45, "dayFollowedByZ", "2011-01-15Z", "2011-01-15T00:00:00Z(DayOfMonth)"),
				new ParseTest(46, "dayFollowedByPlus", "2011-01-15+05", "2011-01-15T00:00:00+05:00(DayOfMonth)"),
				new ParseTest(47, "dayFollowedByMinus", "2011-01-15-05", "2011-01-15T00:00:00-05:00(DayOfMonth)"),
				new ParseTest(48, "hourFollowedByZ", "2011-01-15T12Z", "2011-01-15T12:00:00Z(HourOfDay)"),
				new ParseTest(49, "hourFollowedByPlus", "2011-01-15T12+05", "2011-01-15T12:00:00+05:00(HourOfDay)"),
				new ParseTest(50, "hourFollowedByMinus", "2011-01-15T12-05", "2011-01-15T12:00:00-05:00(HourOfDay)"),
				new ParseTest(51, "minuteFollowedByZ", "2011-01-15T12:30Z", "2011-01-15T12:30:00Z(MinuteOfHour)"),
				new ParseTest(52, "minuteFollowedByPlus", "2011-01-15T12:30+05", "2011-01-15T12:30:00+05:00(MinuteOfHour)"),
				new ParseTest(53, "minuteFollowedByMinus", "2011-01-15T12:30-05", "2011-01-15T12:30:00-05:00(MinuteOfHour)"),
				new ParseTest(54, "timezoneAfterT", "2011-01T+05:30", "2011-01-01T00:00:00+05:30(MonthOfYear)"),
				new ParseTest(55, "dateFollowedByTZ", "2011-01TZ", "2011-01-01T00:00:00Z(MonthOfYear)"),
				new ParseTest(56, "dateFollowedByTMinus", "2011-01T-05:30", "2011-01-01T00:00:00-05:30(MonthOfYear)"),
				new ParseTest(57, "TFollowedByZ", "TZ", "2000-01-01T00:00:00Z(HourOfDay)"),
				new ParseTest(58, "TFollowedByPlus", "T+05", "2000-01-01T00:00:00+05:00(HourOfDay)"),
				new ParseTest(59, "TFollowedByMinus", "T-05", "2000-01-01T00:00:00-05:00(HourOfDay)"),

				// Fractional separator followed by timezone
				new ParseTest(60, "fractionalSeparatorDotFollowedByZ", "2011-01-15T12:30:45.Z", "2011-01-15T12:30:45Z(SecondOfMinute)"),
				new ParseTest(61, "fractionalSeparatorCommaFollowedByZ", "2011-01-15T12:30:45,Z", "2011-01-15T12:30:45Z(SecondOfMinute)"),
				new ParseTest(62, "fractionalSeparatorDotFollowedByPlus", "2011-01-15T12:30:45.+05:00", "2011-01-15T12:30:45+05:00(SecondOfMinute)"),
				new ParseTest(63, "fractionalSeparatorCommaFollowedByPlus", "2011-01-15T12:30:45,+05:00", "2011-01-15T12:30:45+05:00(SecondOfMinute)"),
				new ParseTest(64, "fractionalSeparatorDotFollowedByMinus", "2011-01-15T12:30:45.-05:00", "2011-01-15T12:30:45-05:00(SecondOfMinute)"),
				new ParseTest(65, "fractionalSeparatorCommaFollowedByMinus", "2011-01-15T12:30:45,-05:00", "2011-01-15T12:30:45-05:00(SecondOfMinute)"),

				// Fractional seconds followed by timezone
				new ParseTest(66, "fractionalSecondsFollowedByZ", "2011-01-15T12:30:45.123Z", "2011-01-15T12:30:45.123Z(MilliOfSecond)"),
				new ParseTest(67, "fractionalSecondsFollowedByPlus", "2011-01-15T12:30:45.123+05:00", "2011-01-15T12:30:45.123+05:00(MilliOfSecond)"),
				new ParseTest(68, "fractionalSecondsFollowedByMinus", "2011-01-15T12:30:45.123-05:00", "2011-01-15T12:30:45.123-05:00(MilliOfSecond)"),
				new ParseTest(69, "fractionalSecondsCommaFollowedByZ", "2011-01-15T12:30:45,123Z", "2011-01-15T12:30:45.123Z(MilliOfSecond)"),
				new ParseTest(70, "fractionalSecondsCommaFollowedByPlus", "2011-01-15T12:30:45,123+05:00", "2011-01-15T12:30:45.123+05:00(MilliOfSecond)"),
				new ParseTest(71, "fractionalSecondsCommaFollowedByMinus", "2011-01-15T12:30:45,123-05:00", "2011-01-15T12:30:45.123-05:00(MilliOfSecond)"),

				// Nanoseconds (4+ digits) followed by timezone
				new ParseTest(72, "nanosecondsFollowedByZ", "2011-01-15T12:30:45.1234Z", "2011-01-15T12:30:45.1234Z(NanoOfSecond)"),
				new ParseTest(73, "nanosecondsFollowedByPlus", "2011-01-15T12:30:45.1234+05:00", "2011-01-15T12:30:45.1234+05:00(NanoOfSecond)"),
				new ParseTest(74, "nanosecondsFollowedByMinus", "2011-01-15T12:30:45.1234-05:00", "2011-01-15T12:30:45.1234-05:00(NanoOfSecond)"),

				// ISO8601 offset range validation (-18:00 ≤ offset ≤ +18:00)
				new ParseTest(75, "offsetBoundary_minus18_00", "2011-01-15T12:30:45-18:00", "2011-01-15T12:30:45-18:00(SecondOfMinute)"),
				new ParseTest(76, "offsetBoundary_plus18_00", "2011-01-15T12:30:45+18:00", "2011-01-15T12:30:45+18:00(SecondOfMinute)"),
				new ParseTest(77, "offsetBoundary_minus18_00_compact", "2011-01-15T12:30:45-1800", "2011-01-15T12:30:45-18:00(SecondOfMinute)"),
				new ParseTest(78, "offsetBoundary_plus18_00_compact", "2011-01-15T12:30:45+1800", "2011-01-15T12:30:45+18:00(SecondOfMinute)"),
				new ParseTest(79, "offsetBoundary_minus18_hoursOnly", "2011-01-15T12:30:45-18", "2011-01-15T12:30:45-18:00(SecondOfMinute)"),
				new ParseTest(80, "offsetBoundary_plus18_hoursOnly", "2011-01-15T12:30:45+18", "2011-01-15T12:30:45+18:00(SecondOfMinute)"),
				new ParseTest(81, "offsetValid_withinRange1", "2011-01-15T12:30:45-12:30", "2011-01-15T12:30:45-12:30(SecondOfMinute)"),
				new ParseTest(82, "offsetValid_withinRange2", "2011-01-15T12:30:45+12:30", "2011-01-15T12:30:45+12:30(SecondOfMinute)"),
				new ParseTest(83, "offsetValid_withinRange3", "2011-01-15T12:30:45-17:59", "2011-01-15T12:30:45-17:59(SecondOfMinute)"),
				new ParseTest(84, "offsetValid_withinRange4", "2011-01-15T12:30:45+17:59", "2011-01-15T12:30:45+17:59(SecondOfMinute)"),

				// Invalid offset range
				new ParseTest(85, "offsetInvalid_belowMinimum", "2011-01-15T12:30:45-19:00", "Invalid ISO8601 timestamp"),
				new ParseTest(86, "offsetInvalid_aboveMaximum", "2011-01-15T12:30:45+19:00", "Invalid ISO8601 timestamp"),
				new ParseTest(87, "offsetInvalid_belowMinimum_compact", "2011-01-15T12:30:45-1900", "Invalid ISO8601 timestamp"),
				new ParseTest(88, "offsetInvalid_aboveMaximum_compact", "2011-01-15T12:30:45+1900", "Invalid ISO8601 timestamp"),
				new ParseTest(89, "offsetInvalid_belowMinimum_hoursOnly", "2011-01-15T12:30:45-19", "Invalid ISO8601 timestamp"),
				new ParseTest(90, "offsetInvalid_aboveMaximum_hoursOnly", "2011-01-15T12:30:45+19", "Invalid ISO8601 timestamp"),

				// Invalid offset format
				new ParseTest(91, "offsetInvalid_1digit", "2011-01-15T12:30:45+1", "Invalid ISO8601 timestamp"),
				new ParseTest(92, "offsetInvalid_3digits", "2011-01-15T12:30:45+123", "Invalid ISO8601 timestamp"),
				new ParseTest(93, "offsetInvalid_5digits", "2011-01-15T12:30:45+12345", "Invalid ISO8601 timestamp"),

				// Invalid date/time values
				new ParseTest(94, "invalidYearLength", "123", "Invalid ISO8601 timestamp"),
				new ParseTest(95, "invalidMonth_00", "2011-00", "Invalid ISO8601 timestamp"),
				new ParseTest(96, "invalidMonth_13", "2011-13", "Invalid ISO8601 timestamp"),
				new ParseTest(97, "invalidMonth_99", "2011-99", "Invalid ISO8601 timestamp"),
				new ParseTest(98, "invalidDay_00", "2011-01-00", "Invalid ISO8601 timestamp"),
				new ParseTest(99, "invalidDay_32", "2011-01-32", "Invalid ISO8601 timestamp"),
				new ParseTest(100, "invalidDay_99", "2011-01-99", "Invalid ISO8601 timestamp"),
				new ParseTest(101, "invalidHour_24", "2011-01-15T24", "Invalid ISO8601 timestamp"),
				new ParseTest(102, "invalidHour_99", "2011-01-15T99", "Invalid ISO8601 timestamp"),
				new ParseTest(103, "invalidMinute_60", "2011-01-15T12:60", "Invalid ISO8601 timestamp"),
				new ParseTest(104, "invalidMinute_99", "2011-01-15T12:99", "Invalid ISO8601 timestamp"),
				new ParseTest(105, "invalidSecond_60", "2011-01-15T12:30:60", "Invalid ISO8601 timestamp"),
				new ParseTest(106, "invalidSecond_99", "2011-01-15T12:30:99", "Invalid ISO8601 timestamp"),

				// Invalid dates for specific months
				new ParseTest(107, "invalidDate_Nov31", "2011-11-31", "Invalid date 'NOVEMBER 31'"),
				new ParseTest(108, "invalidDate_Feb29_nonLeap", "2011-02-29", "Invalid date 'February 29' as '2011' is not a leap year"),
				new ParseTest(109, "invalidDate_Feb30", "2011-02-30", "Invalid date 'FEBRUARY 30'"),
				new ParseTest(110, "invalidDate_Apr31", "2011-04-31", "Invalid date 'APRIL 31'"),
				new ParseTest(111, "invalidDate_Jun31", "2011-06-31", "Invalid date 'JUNE 31'"),
				new ParseTest(112, "invalidDate_Sep31", "2011-09-31", "Invalid date 'SEPTEMBER 31'"),
				new ParseTest(113, "validDate_Feb29_leap", "2024-02-29", "2024-02-29T00:00:00Z(DayOfMonth)"),

				// Invalid characters in various states
				new ParseTest(114, "invalidCharAfterYear", "2011X", "Invalid ISO8601 timestamp"),
				new ParseTest(115, "invalidCharAfterYearDash", "2011-X", "Invalid ISO8601 timestamp"),
				new ParseTest(116, "invalidCharAfterMonth", "2011-01X", "Invalid ISO8601 timestamp"),
				new ParseTest(117, "invalidCharAfterMonthDash", "2011-01-X", "Invalid ISO8601 timestamp"),
				new ParseTest(118, "invalidCharAfterDay", "2011-01-15X", "Invalid ISO8601 timestamp"),
				new ParseTest(119, "invalidCharAfterT", "TX", "Invalid ISO8601 timestamp"),
				new ParseTest(120, "invalidCharAfterHour", "2011-01-15T12X", "Invalid ISO8601 timestamp"),
				new ParseTest(121, "invalidCharAfterHourColon", "2011-01-15T12:X", "Invalid ISO8601 timestamp"),
				new ParseTest(122, "invalidCharAfterMinute", "2011-01-15T12:30X", "Invalid ISO8601 timestamp"),
				new ParseTest(123, "invalidCharAfterMinuteColon", "2011-01-15T12:30:X", "Invalid ISO8601 timestamp"),
				new ParseTest(124, "invalidCharAfterSecond", "2011-01-15T12:30:45X", "Invalid ISO8601 timestamp"),
				new ParseTest(125, "invalidCharAfterFractionalSeparator", "2011-01-15T12:30:45.X", "Invalid ISO8601 timestamp"),
				new ParseTest(126, "invalidCharInFractionalSeconds", "2011-01-15T12:30:45.12X", "Invalid ISO8601 timestamp"),
				new ParseTest(127, "invalidCharAfterPlus", "2011-01-15T12:30:45+X", "Invalid ISO8601 timestamp"),
				new ParseTest(128, "invalidCharAfterMinus", "2011-01-15T12:30:45-X", "Invalid ISO8601 timestamp"),
				new ParseTest(129, "invalidCharInOffsetHours", "2011-01-15T12:30:45+05X", "Invalid ISO8601 timestamp"),
				new ParseTest(130, "invalidCharAfterOffsetColon", "2011-01-15T12:30:45+05:X", "Invalid ISO8601 timestamp"),
				new ParseTest(131, "invalidCharInOffsetMinutes", "2011-01-15T12:30:45+05:30X", "Invalid ISO8601 timestamp"),

				// Invalid character after Z
				new ParseTest(132, "invalidCharAfterZ_year", "2011Z5", "Invalid ISO8601 timestamp"),
				new ParseTest(133, "invalidCharAfterZ_month", "2011-01ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(134, "invalidCharAfterZ_day", "2011-01-15ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(135, "invalidCharAfterZ_T", "TZX", "Invalid ISO8601 timestamp"),
				new ParseTest(136, "invalidCharAfterZ_hour", "2011-01-15T12ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(137, "invalidCharAfterZ_minute", "2011-01-15T12:30ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(138, "invalidCharAfterZ_second", "2011-01-15T12:30:45ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(139, "invalidCharAfterZ_secondPlus", "2011-01-15T12:30:45Z+", "Invalid ISO8601 timestamp"),
				new ParseTest(140, "invalidCharAfterZ_secondMinus", "2011-01-15T12:30:45Z-", "Invalid ISO8601 timestamp"),
				new ParseTest(141, "invalidCharAfterZ_secondDigit", "2011-01-15T12:30:45Z5", "Invalid ISO8601 timestamp"),
				new ParseTest(142, "invalidCharAfterZ_secondSpace", "2011-01-15T12:30:45Z ", "Invalid ISO8601 timestamp"),
				new ParseTest(143, "invalidCharAfterZ_fractionalSeparator", "2011-01-15T12:30:45.ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(144, "invalidCharAfterZ_fractionalSeconds", "2011-01-15T12:30:45.123ZX", "Invalid ISO8601 timestamp"),
				new ParseTest(145, "invalidCharAfterZ_timeOnly", "T12:30:45ZX", "Invalid ISO8601 timestamp"),

				// Invalid nanosecond length (>9 digits)
				new ParseTest(146, "invalidNanos_10digits", "2011-01-15T12:30:45.1234567890", "Invalid ISO8601 timestamp"),

				// Edge cases with defaultZoneId
				new ParseTest(147, "withDefaultZoneId", "2011-01-15T12:30:45", "2011-01-15T12:30:45-05:00(SecondOfMinute)", ZoneId.of("America/New_York")),
				new ParseTest(148, "withDefaultZoneId_timeOnly", "T12:30:45", "2000-01-01T12:30:45-05:00(SecondOfMinute)", ZoneId.of("America/New_York")),
				new ParseTest(149, "withDefaultZoneId_ignoredWhenZoneInString", "2011-01-15T12:30:45Z", "2011-01-15T12:30:45Z(SecondOfMinute)", ZoneId.of("America/New_York")),

				// Invalid inputs (expect exceptions)
				new ParseTest(150, "null", null, "Argument 'value' cannot be null."),
				new ParseTest(151, "emptyString", "", "Invalid ISO8601 timestamp"),
				new ParseTest(152, "invalidString", "invalid-date", "Invalid ISO8601 timestamp"),
			};
		}

		@ParameterizedTest(name = "[{0}] {1}")
		@MethodSource("parseTests")
		void j01_parse(ParseTest test) {
			if (test.defaultZoneId == null) {
				testParse(test.expected, test.input);
			} else {
				testParse(test.expected, test.input, test.defaultZoneId);
			}
		}

		private void testParse(String expected, String in) {
			try {
				var x = GranularZonedDateTime.of(in, FAKE_TIME_PROVIDER);
				assertEquals(expected, x.toString(), "Failed for input: " + in);
			} catch (Exception e) {
				assertEquals(expected, e.getLocalizedMessage(), "Failed for input: " + in);
			}
		}

		private void testParse(String expected, String in, ZoneId zoneId) {
			try {
				var x = GranularZonedDateTime.of(in, zoneId, FAKE_TIME_PROVIDER);
				assertEquals(expected, x.toString(), "Failed for input: " + in + " with zoneId: " + zoneId);
			} catch (Exception e) {
				assertEquals(expected, e.getLocalizedMessage(), "Failed for input: " + in + " with zoneId: " + zoneId);
			}
		}
	}
}
