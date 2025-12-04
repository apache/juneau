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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StateEnum.*;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.commons.utils.*;

/**
 * A ZonedDateTime with precision information for granular time operations.
 *
 * <p>
 * This class combines a {@link ZonedDateTime} with a {@link ChronoField} precision identifier,
 * allowing for granular time operations such as rolling by specific time units.
 *
 * <p>
 * The precision field indicates the granularity of the time value, which determines how the
 * {@link #roll(int)} method behaves. For example, a precision of {@link ChronoField#YEAR} means
 * rolling by 1 will advance the year, while a precision of {@link ChronoField#HOUR_OF_DAY} means
 * rolling by 1 will advance the hour.
 *
 * <h5 class='section'>ISO8601 Parsing:</h5>
 * <p>
 * The {@link #of(String)} method can parse various ISO8601 timestamp formats:
 * <ul>
 * 	<li>Date formats: <js>"2011"</js>, <js>"2011-01"</js>, <js>"2011-01-15"</js>
 * 	<li>DateTime formats: <js>"2011-01-15T12"</js>, <js>"2011-01-15T12:30"</js>, <js>"2011-01-15T12:30:45"</js>
 * 	<li>With fractional seconds: <js>"2011-01-15T12:30:45.123"</js>, <js>"2011-01-15T12:30:45,123"</js>
 * 	<li>Time-only formats: <js>"T12"</js>, <js>"T12:30"</js>, <js>"T12:30:45"</js>
 * 	<li>With timezone: <js>"2011-01-15T12:30:45Z"</js>, <js>"2011-01-15T12:30:45+05:30"</js>, <js>"2011-01-15T12:30:45-05:30"</js>
 * </ul>
 *
 * <p>
 * The precision is automatically determined from the input format. For example:
 * <ul>
 * 	<li><js>"2011"</js> → {@link ChronoField#YEAR}
 * 	<li><js>"2011-01"</js> → {@link ChronoField#MONTH_OF_YEAR}
 * 	<li><js>"2011-01-15"</js> → {@link ChronoField#DAY_OF_MONTH}
 * 	<li><js>"2011-01-15T12"</js> → {@link ChronoField#HOUR_OF_DAY}
 * 	<li><js>"2011-01-15T12:30"</js> → {@link ChronoField#MINUTE_OF_HOUR}
 * 	<li><js>"2011-01-15T12:30:45"</js> → {@link ChronoField#SECOND_OF_MINUTE}
 * 	<li><js>"2011-01-15T12:30:45.123"</js> → {@link ChronoField#MILLI_OF_SECOND} (1-3 digits)
 * 	<li><js>"2011-01-15T12:30:45.123456789"</js> → {@link ChronoField#NANO_OF_SECOND} (4-9 digits)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse an ISO8601 timestamp with year precision</jc>
 * 	GranularZonedDateTime <jv>gdt</jv> = GranularZonedDateTime.<jsm>of</jsm>(<js>"2011"</js>);
 * 	<jc>// Roll forward by one year</jc>
 * 	<jv>gdt</jv> = <jv>gdt</jv>.<jsm>roll</jsm>(1);
 * 	<jc>// Get the ZonedDateTime</jc>
 * 	ZonedDateTime <jv>zdt</jv> = <jv>gdt</jv>.<jsm>getZonedDateTime</jsm>();
 * 	<jc>// Result: 2012-01-01T00:00:00 with system default timezone</jc>
 * </p>
 *
 * <p class='bjava'>
 * 	<jc>// Parse a datetime with hour precision</jc>
 * 	GranularZonedDateTime <jv>gdt2</jv> = GranularZonedDateTime.<jsm>of</jsm>(<js>"2011-01-15T12Z"</js>);
 * 	<jc>// Roll forward by 2 hours</jc>
 * 	<jv>gdt2</jv> = <jv>gdt2</jv>.<jsm>roll</jsm>(<jv>ChronoField</jv>.<jf>HOUR_OF_DAY</jf>, 2);
 * 	<jc>// Result: 2011-01-15T14:00:00Z</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is immutable and thread-safe.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link ZonedDateTime}
 * 	<li class='jm'>{@link ChronoField}
 * 	<li class='jm'>{@link DateUtils}
 * </ul>
 */
public class GranularZonedDateTime {

	/**
	 * Creates a GranularZonedDateTime from a Date with the specified precision.
	 *
	 * <p>
	 * The date is converted to a ZonedDateTime using the system default timezone.
	 *
	 * @param date The date to convert.
	 * @param precision The precision of the time value.
	 * @return A new GranularZonedDateTime instance.
	 * @throws IllegalArgumentException if date or precision is null.
	 */
	public static GranularZonedDateTime of(Date date, ChronoField precision) {
		return of(date, precision, ZoneId.systemDefault());
	}

	/**
	 * Creates a GranularZonedDateTime from a Date with the specified precision and timezone.
	 *
	 * <p>
	 * The date is converted to a ZonedDateTime using the specified timezone.
	 *
	 * @param date The date to convert.
	 * @param precision The precision of the time value.
	 * @param zoneId The timezone to use.
	 * @return A new GranularZonedDateTime instance.
	 * @throws IllegalArgumentException if date, precision, or zoneId is null.
	 */
	public static GranularZonedDateTime of(Date date, ChronoField precision, ZoneId zoneId) {
		return of(date.toInstant().atZone(zoneId), precision);
	}

	/**
	 * Parses an ISO8601 timestamp string into a GranularZonedDateTime.
	 *
	 * <p>
	 * This method parses various ISO8601 formats and automatically determines the precision
	 * based on the format. If no timezone is specified in the string, the system default
	 * timezone is used.
	 *
	 * <h5 class='section'>Supported Formats:</h5>
	 * <ul>
	 * 	<li>Date: <js>"2011"</js>, <js>"2011-01"</js>, <js>"2011-01-15"</js>
	 * 	<li>DateTime: <js>"2011-01-15T12"</js>, <js>"2011-01-15T12:30"</js>, <js>"2011-01-15T12:30:45"</js>
	 * 	<li>With fractional seconds: <js>"2011-01-15T12:30:45.123"</js>, <js>"2011-01-15T12:30:45,123"</js>
	 * 	<li>Time-only: <js>"T12"</js>, <js>"T12:30"</js>, <js>"T12:30:45"</js> (uses current date)
	 * 	<li>With timezone: <js>"2011-01-15T12:30:45Z"</js>, <js>"2011-01-15T12:30:45+05:30"</js>, <js>"2011-01-15T12:30:45-05:30"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Timezone Handling:</h5>
	 * <ul>
	 * 	<li>If the string contains a timezone (Z, +HH:mm, -HH:mm, etc.), that timezone is used.
	 * 	<li>If no timezone is specified, the system default timezone is used.
	 * 	<li>For time-only formats (starting with "T"), the current date is used with the specified or default timezone.
	 * </ul>
	 *
	 * <h5 class='section'>Precision Detection:</h5>
	 * <p>
	 * The precision is automatically determined from the format:
	 * <ul>
	 * 	<li>Year only → {@link ChronoField#YEAR}
	 * 	<li>Year-Month → {@link ChronoField#MONTH_OF_YEAR}
	 * 	<li>Year-Month-Day → {@link ChronoField#DAY_OF_MONTH}
	 * 	<li>With hour → {@link ChronoField#HOUR_OF_DAY}
	 * 	<li>With minute → {@link ChronoField#MINUTE_OF_HOUR}
	 * 	<li>With second → {@link ChronoField#SECOND_OF_MINUTE}
	 * 	<li>With 1-3 fractional digits → {@link ChronoField#MILLI_OF_SECOND}
	 * 	<li>With 4-9 fractional digits → {@link ChronoField#NANO_OF_SECOND}
	 * </ul>
	 *
	 * @param value The ISO8601 timestamp string to parse.
	 * @return A new GranularZonedDateTime instance.
	 * @throws IllegalArgumentException if timestamp is null.
	 * @throws DateTimeParseException if the timestamp format is invalid.
	 */
	public static GranularZonedDateTime of(String value) {
		return of(value, null, null);
	}

	/**
	 * Parses an ISO8601 timestamp string into a GranularZonedDateTime with a custom time provider.
	 *
	 * <p>
	 * This method is similar to {@link #of(String)}, but allows you to specify a custom
	 * {@link TimeProvider} to use for obtaining the system default timezone and current time.
	 * This is useful for testing or when you need deterministic time behavior.
	 *
	 * <p>
	 * The time provider is used when:
	 * <ul>
	 * 	<li>No timezone is specified in the string - uses {@link TimeProvider#getSystemDefaultZoneId()}
	 * 	<li>Time-only formats (starting with "T") - uses {@link TimeProvider#now(ZoneId)} to get the current date
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse with custom time provider for testing</jc>
	 * 	<jk>var</jk> <jv>timeProvider</jv> = <jk>new</jk> FakeTimeProvider();
	 * 	GranularZonedDateTime <jv>gdt</jv> = GranularZonedDateTime.<jsm>of</jsm>(
	 * 		<js>"T12:30:45"</js>,
	 * 		<jv>timeProvider</jv>
	 * 	);
	 * 	<jc>// Result uses the time provider's current date and timezone</jc>
	 * </p>
	 *
	 * @param value The ISO8601 timestamp string to parse.
	 * @param timeProvider The time provider to use for system default timezone and current time.
	 * 	If null, {@link TimeProvider#INSTANCE} is used.
	 * @return A new GranularZonedDateTime instance.
	 * @throws IllegalArgumentException if value is null.
	 * @throws DateTimeParseException if the timestamp format is invalid.
	 */
	public static GranularZonedDateTime of(String value, TimeProvider timeProvider) {
		return of(value, null, timeProvider);
	}


	/**
	 * Parses an ISO8601 timestamp string into a GranularZonedDateTime with a default timezone and custom time provider.
	 *
	 * <p>
	 * This method is similar to {@link #of(String)}, but allows you to specify both a default
	 * timezone and a custom {@link TimeProvider} to use when no timezone is present in the timestamp string.
	 *
	 * <p>
	 * If the timestamp string contains a timezone (Z, +HH:mm, -HH:mm, etc.), that timezone
	 * takes precedence over the defaultZoneId parameter. The defaultZoneId is only used when
	 * no timezone is specified in the string.
	 *
	 * <p>
	 * The time provider is used when:
	 * <ul>
	 * 	<li>No timezone is specified and defaultZoneId is null - uses {@link TimeProvider#getSystemDefaultZoneId()}
	 * 	<li>Time-only formats (starting with "T") - uses {@link TimeProvider#now(ZoneId)} to get the current date
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse with default timezone and custom time provider</jc>
	 * 	<jk>var</jk> <jv>timeProvider</jv> = <jk>new</jk> FakeTimeProvider();
	 * 	GranularZonedDateTime <jv>gdt1</jv> = GranularZonedDateTime.<jsm>of</jsm>(
	 * 		<js>"2011-01-15T12:30:45"</js>,
	 * 		<jv>ZoneId</jv>.<jsm>of</jsm>(<js>"America/New_York"</js>),
	 * 		<jv>timeProvider</jv>
	 * 	);
	 * 	<jc>// Result uses America/New_York timezone</jc>
	 *
	 * 	<jc>// Parse with timezone in string (defaultZoneId is ignored)</jc>
	 * 	GranularZonedDateTime <jv>gdt2</jv> = GranularZonedDateTime.<jsm>of</jsm>(
	 * 		<js>"2011-01-15T12:30:45Z"</js>,
	 * 		<jv>ZoneId</jv>.<jsm>of</jsm>(<js>"America/New_York"</js>),
	 * 		<jv>timeProvider</jv>
	 * 	);
	 * 	<jc>// Result uses UTC (Z), not America/New_York</jc>
	 * </p>
	 *
	 * @param value The ISO8601 timestamp string to parse.
	 * @param defaultZoneId The default timezone to use if no timezone is specified in the string.
	 * 	If null, the time provider's system default timezone is used.
	 * @param timeProvider The time provider to use for system default timezone and current time.
	 * 	If null, {@link TimeProvider#INSTANCE} is used.
	 * @return A new GranularZonedDateTime instance.
	 * @throws IllegalArgumentException if value is null.
	 * @throws DateTimeParseException if the timestamp format is invalid.
	 */
	public static GranularZonedDateTime of(String value, ZoneId defaultZoneId, TimeProvider timeProvider) {
		assertArgNotNull("value", value);
		var digit = StringUtils.DIGIT;
		timeProvider = timeProvider == null ? TimeProvider.INSTANCE : timeProvider;

		// States:
		// S01: Looking for Y(S02) or T(S07).
		// S02: Found Y, looking for Y(S02)/-(S03)/T(S07).
		// S03: Found -, looking for M(S04).
		// S04: Found M, looking for M(S04)/-(S05)/T(S07).
		// S05: Found -, looking for D(S10).
		// S06  Found D, looking for D(S06)/T(S07).
		// S07: Found T, looking for h(S08)/Z(S15)/+(S16)/-(S17).
		// S08: Found h, looking for h(S08)/:(S09)/Z(S15)/+(S16)/-(S17).
		// S09: Found :, looking for m(S10).
		// S10: Found m, looking for m(S10)/:(S11)/Z(S15)/+(S16)/-(S17).
		// S11: Found :, looking for s(S12).
		// S12: Found s, looking for s(S12)/.(S13)/Z(S15)/+(S16)/-(S17).
		// S13: Found ., looking for S(S14)/Z(S15)/+(S16)/-(S17).
		// S14: Found S, looking for S(S14)/Z(S15)/+(S16)/-(S17).
		// S15: Found Z.
		// S16: Found +, looking for oh(S18).
		// S17: Found -, looking for oh(S18).
		// S18: Found oh, looking for oh(S18)/:(S19).
		// S19: Found :, looking for om(S20).
		// S20: Found om, looking for om(S20).


		int year = 1, month = 1, day = 1, hour = 0, minute = 0, second = 0, nanos = 0, ohour = -1, ominute = -1;
		boolean nego = false; // negative offset
		boolean timeOnly = false; // Track if format started with "T" (time-only)
		ZoneId zoneId = null;
		var state = S1;
		var mark = 0;
		ChronoField precision = ChronoField.YEAR; // Track precision as we go

		for (var i = 0; i < value.length(); i++) {
			var c = value.charAt(i);

			if (state == S1) {
				// S01: Looking for Y(S02) or T(S07)
				if (digit.contains(c)) {
					mark = i;
					state = S2;
				} else if (c == 'T') {
					timeOnly = true; // Mark as time-only format
					state = S7;
				} else {
					throw bad(value, i);
				}
			} else if (state == S2) {
				// S02: Found Y, looking for Y(S02)/-(S03)/T(S07)
				if (digit.contains(c)) {
					// Stay in S2
				} else if (c == '-') {
					year = parse(value, 4, mark, i, 0, 9999);
					state = S3;
				} else if (c == 'T') {
					year = parse(value, 4, mark, i, 0, 9999);
					state = S7;
				} else if (c == 'Z') {
					zoneId = ZoneId.of("Z");
					year = parse(value, 4, mark, i, 0, 9999);
					state = S15;
				} else if (c == '+') {
					year = parse(value, 4, mark, i, 0, 9999);
					nego = false;
					state = S16;
				} else {
					throw bad(value, i);
				}
			} else if (state == S3) {
				// S03: Found -, looking for M(S04)
				if (digit.contains(c)) {
					mark = i;
					state = S4;
					precision = ChronoField.MONTH_OF_YEAR;
				} else {
					throw bad(value, i);
				}
			} else if (state == S4) {
				// S04: Found M, looking for M(S04)/-(S05)/T(S07)
				if (digit.contains(c)) {
					// Stay in S4
				} else if (c == '-') {
					month = parse(value, 2, mark, i, 1, 12);
					state = S5;
				} else if (c == 'T') {
					month = parse(value, 2, mark, i, 1, 12);
					state = S7;
				} else if (c == 'Z') {
					month = parse(value, 2, mark, i, 1, 12);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					month = parse(value, 2, mark, i, 1, 12);
					nego = false;
					state = S16;
				} else {
					throw bad(value, i);
				}
			} else if (state == S5) {
				// S05: Found -, looking for D(S06)
				if (digit.contains(c)) {
					mark = i;
					state = S6;
					precision = ChronoField.DAY_OF_MONTH;
				} else {
					throw bad(value, i);
				}
			} else if (state == S6) {
				// S06: Found D, looking for D(S06)/T(S07)
				if (digit.contains(c)) {
					// Stay in S6
				} else if (c == 'T') {
					day = parse(value, 2, mark, i, 1, 31);
					state = S7;
				} else if (c == 'Z') {
					day = parse(value, 2, mark, i, 1, 31);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					day = parse(value, 2, mark, i, 1, 31);
					nego = false;
					state = S16;
				} else if (c == '-') {
					day = parse(value, 2, mark, i, 1, 31);
					nego = true;
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S7) {
				// S07: Found T, looking for h(S08)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					mark = i;
					state = S8;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z') {
					zoneId = ZoneId.of("Z");
					if (timeOnly) {
						precision = ChronoField.HOUR_OF_DAY;
					}
					state = S15;
				} else if (c == '+') {
					nego = false;
					if (timeOnly) {
						precision = ChronoField.HOUR_OF_DAY;
					}
					state = S16;
				} else if (c == '-') {
					nego = true;
					if (timeOnly) {
						precision = ChronoField.HOUR_OF_DAY;
					}
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S8) {
				// S08: Found h, looking for h(S08)/:(S09)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S8
				} else if (c == ':') {
					hour = parse(value, 2, mark, i, 0, 23);
					state = S9;
				} else if (c == 'Z') {
					hour = parse(value, 2, mark, i, 0, 23);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					hour = parse(value, 2, mark, i, 0, 23);
					nego = false;
					state = S16;
				} else if (c == '-') {
					hour = parse(value, 2, mark, i, 0, 23);
					nego = true;
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S9) {
				// S09: Found :, looking for m(S10)
				if (digit.contains(c)) {
					mark = i;
					state = S10;
					precision = ChronoField.MINUTE_OF_HOUR;
				} else {
					throw bad(value, i);
				}
			} else if (state == S10) {
				// S10: Found m, looking for m(S10)/:(S11)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S10
				} else if (c == ':') {
					minute = parse(value, 2, mark, i, 0, 59);
					state = S11;
				} else if (c == 'Z') {
					minute = parse(value, 2, mark, i, 0, 59);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					minute = parse(value, 2, mark, i, 0, 59);
					nego = false;
					state = S16;
				} else if (c == '-') {
					minute = parse(value, 2, mark, i, 0, 59);
					nego = true;
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S11) {
				// S11: Found :, looking for s(S12)
				if (digit.contains(c)) {
					mark = i;
					state = S12;
					precision = ChronoField.SECOND_OF_MINUTE;
				} else {
					throw bad(value, i);
				}
			} else if (state == S12) {
				// S12: Found s, looking for s(S12)/.(S13)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S12
			} else if (c == '.' || c == ',') {
				second = parse(value, 2, mark, i, 0, 59);
				state = S13;
				// Precision will be set based on number of fractional digits
				} else if (c == 'Z') {
					second = parse(value, 2, mark, i, 0, 59);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					second = parse(value, 2, mark, i, 0, 59);
					nego = false;
					state = S16;
				} else if (c == '-') {
					second = parse(value, 2, mark, i, 0, 59);
					nego = true;
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S13) {
				// S13: Found . or ,, looking for S(S14)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					mark = i;
					state = S14;
				} else if (c == 'Z') {
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					nego = false;
					state = S16;
				} else if (c == '-') {
					nego = true;
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S14) {
				// S14: Found S, looking for S(S14)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S14
				} else if (c == 'Z') {
					nanos = parseNanos(value, mark, i);
					zoneId = ZoneId.of("Z");
					// Set precision based on number of fractional digits: 1-3 = milliseconds, 4-9 = nanoseconds
					var digitCount = i - mark;
					precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
					state = S15;
				} else if (c == '+') {
					nanos = parseNanos(value, mark, i);
					// Set precision based on number of fractional digits: 1-3 = milliseconds, 4-9 = nanoseconds
					var digitCount = i - mark;
					precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
					nego = false;
					state = S16;
				} else if (c == '-') {
					nanos = parseNanos(value, mark, i);
					// Set precision based on number of fractional digits: 1-3 = milliseconds, 4-9 = nanoseconds
					var digitCount = i - mark;
					precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
					nego = true;
					state = S17;
				} else {
					throw bad(value, i);
				}
			} else if (state == S15) {
				// Shouldn't find anything after Z
				throw bad(value, i);
			} else if (state == S16) {
				// S16: Found +, looking for oh(S18)
				if (digit.contains(c)) {
					mark = i;
					state = S18;
				} else {
					throw bad(value, i);
				}
			} else if (state == S17) {
				// S17: Found -, looking for oh(S18)
				if (digit.contains(c)) {
					mark = i;
					state = S18;
				} else {
					throw bad(value, i);
				}
			} else if (state == S18) {
				// S18: Found oh, looking for oh(S18)/:(S19)/end
				if (digit.contains(c)) {
					// Stay in S18
				} else if (c == ':') {
					ohour = parse(value, 2, mark, i, 0, 18);
					state = S19;
				} else {
					throw bad(value, i);
				}
				// If we reach end of string, ohour is complete (2 digits)
			} else if (state == S19) {
				// S19: Found :, looking for om(S20)
				if (digit.contains(c)) {
					mark = i;
					state = S20;
				} else {
					throw bad(value, i);
				}
			} else /* (state == S20) */ {
				// S20: Found om, looking for om(S20)
				if (digit.contains(c)) {
					// Stay in S20
				} else {
					throw bad(value, i);
				}
			}
		}

		var end = value.length(); // end is exclusive (one past last character)
		if (state.isAny(S1, S3, S5, S7, S9, S11, S13, S16, S17, S19)) {
			throw bad(value, end - 1);
		} else if (state == S2) {
			// S02: Found Y, looking for Y(S02)/-(S03)/T(S07).
			year = parse(value, 4, mark, end, 0, 9999);
			precision = ChronoField.YEAR;
		} else if (state == S4) {
			// S04: Found M, looking for M(S04)/-(S05)/T(S07).
			month = parse(value, 2, mark, end, 1, 12);
			precision = ChronoField.MONTH_OF_YEAR;
		} else if (state == S6) {
			// S06  Found D, looking for D(S06)/T(S07).
			day = parse(value, 2, mark, end, 1, 31);
			precision = ChronoField.DAY_OF_MONTH;
		} else if (state == S8) {
			// S08: Found h, looking for h(S08)/:(S09)/Z(S15)/+(S16)/-(S17).
			hour = parse(value, 2, mark, end, 0, 23);
			precision = ChronoField.HOUR_OF_DAY;
		} else if (state == S10) {
			// S10: Found m, looking for m(S10)/:(S11)/Z(S15)/+(S16)/-(S17).
			minute = parse(value, 2, mark, end, 0, 59);
			precision = ChronoField.MINUTE_OF_HOUR;
		} else if (state == S12) {
			// S12: Found s, looking for s(S12)/.(S13)/Z(S15)/+(S16)/-(S17).
			second = parse(value, 2, mark, end, 0, 59);
			precision = ChronoField.SECOND_OF_MINUTE;
		} else if (state == S14) {
			// S14: Found S, looking for S(S14)/Z(S15)/+(S16)/-(S17).
			nanos = parseNanos(value, mark, end);
			// Set precision based on number of digits: 1-3 = milliseconds, 4-9 = nanoseconds
			var digitCount = end - mark;
			precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
		} else if (state == S15) {
			// S15: Found Z.
		} else if (state == S18) {
			// S18: Found oh, looking for oh(S18)/:(S19).
			// Check if we have 2 digits (+hh) or 4 digits (+hhmm)
			if (end - mark == 2) {
				ohour = parse(value, 2, mark, end, 0, 18);
			} else if (end - mark == 4) {
				// +hhmm format: parse hours from mark to mark+2, minutes from mark+2 to end
				ohour = parse(value, 2, mark, mark + 2, 0, 18);
				ominute = parse(value, 2, mark + 2, end, 0, 59);
			} else {
				throw bad(value, mark);
			}
		} else /* (state == S20) */ {
			// S20: Found om, looking for om(S20).
			ominute = parse(value, 2, mark, end, 0, 59);
		}

		// Build ZoneId if we have offset information
		if (zoneId == null) {
			if (ohour >= 0) {
				if (ominute >= 0) {
					// If negative offset, both hours and minutes must be negative
					var offset = ZoneOffset.ofHoursMinutes(nego ? -ohour : ohour, nego ? -ominute : ominute);
					zoneId = offset;
				} else {
					var offset = ZoneOffset.ofHours(nego ? -ohour : ohour);
					zoneId = offset;
				}
			}
		}

		// Use provided default zone if no zone specified, otherwise use system default
		if (zoneId == null) {
			zoneId = defaultZoneId != null ? defaultZoneId : timeProvider.getSystemDefaultZoneId();
		}

		// Construct ZonedDateTime from parsed values
		// Default values for missing components
		// For time-only formats (started with "T"), use current date
		// For date formats, default to 1/1/1
		if (timeOnly) {
			// Time-only format: use current year/month/day
			var now = timeProvider.now(zoneId);
			year = now.getYear();
			month = now.getMonthValue();
			day = now.getDayOfMonth();
		}

		var localDateTime = LocalDateTime.of(year, month, day, hour, minute, second);
		if (nanos > 0) {
			localDateTime = localDateTime.plusNanos(nanos);
		}

		var zdt = ZonedDateTime.of(localDateTime, zoneId);

		// Return GranularZonedDateTime with the determined precision
		return new GranularZonedDateTime(zdt, precision);
	}

	/**
	 * Creates a GranularZonedDateTime from a ZonedDateTime with the specified precision.
	 *
	 * <p>
	 * This is the most direct way to create a GranularZonedDateTime when you already have
	 * a ZonedDateTime and want to specify its precision.
	 *
	 * @param date The ZonedDateTime value.
	 * @param precision The precision of the time value.
	 * @return A new GranularZonedDateTime instance.
	 * @throws IllegalArgumentException if date or precision is null.
	 */
	public static GranularZonedDateTime of(ZonedDateTime date, ChronoField precision) {
		return new GranularZonedDateTime(date, precision);
	}


	private static DateTimeParseException bad(String s, int pos) {
		return new DateTimeParseException("Invalid ISO8601 timestamp", s, pos);
	}

	private static int parse(String s, int chars, int pos, int end, int min, int max) {
		if (end-pos != chars) throw bad(s, pos);
		var i = Integer.parseInt(s, pos, end, 10);
		if (i < min || i > max) throw bad(s, pos);
		return i;
	}

	private static int parseNanos(String s, int pos, int end) {
		var len = end - pos; // Length of the substring being parsed
		if (len > 9) {
			throw bad(s, pos);
		}
		var n = Integer.parseInt(s, pos, end, 10);
		// Convert to nanoseconds based on number of digits
		// 1 digit = hundreds of milliseconds, 2 = tens, 3 = milliseconds, etc.
		if (len == 1) return n * 100000000;
		if (len == 2) return n * 10000000;
		if (len == 3) return n * 1000000;
		if (len == 4) return n * 100000;
		if (len == 5) return n * 10000;
		if (len == 6) return n * 1000;
		if (len == 7) return n * 100;
		if (len == 8) return n * 10;
		return n;
	}

	/**
	 * Converts a ChronoField to its corresponding ChronoUnit.
	 *
	 * <p>
	 * This method provides a mapping from date/time fields to time units.
	 * Not all ChronoField values have direct ChronoUnit equivalents.
	 *
	 * <p>
	 * Supported fields:
	 * <ul>
	 * 	<li>{@link ChronoField#YEAR} → {@link ChronoUnit#YEARS}
	 * 	<li>{@link ChronoField#MONTH_OF_YEAR} → {@link ChronoUnit#MONTHS}
	 * 	<li>{@link ChronoField#DAY_OF_MONTH} → {@link ChronoUnit#DAYS}
	 * 	<li>{@link ChronoField#HOUR_OF_DAY} → {@link ChronoUnit#HOURS}
	 * 	<li>{@link ChronoField#MINUTE_OF_HOUR} → {@link ChronoUnit#MINUTES}
	 * 	<li>{@link ChronoField#SECOND_OF_MINUTE} → {@link ChronoUnit#SECONDS}
	 * 	<li>{@link ChronoField#MILLI_OF_SECOND} → {@link ChronoUnit#MILLIS}
	 * 	<li>{@link ChronoField#NANO_OF_SECOND} → {@link ChronoUnit#NANOS}
	 * </ul>
	 *
	 * @param field The ChronoField to convert
	 * @return The corresponding ChronoUnit, or null if no direct mapping exists
	 */
	private static ChronoUnit toChronoUnit(ChronoField field) {
		return switch (field) {
			case YEAR -> ChronoUnit.YEARS;
			case MONTH_OF_YEAR -> ChronoUnit.MONTHS;
			case DAY_OF_MONTH -> ChronoUnit.DAYS;
			case HOUR_OF_DAY -> ChronoUnit.HOURS;
			case MINUTE_OF_HOUR -> ChronoUnit.MINUTES;
			case SECOND_OF_MINUTE -> ChronoUnit.SECONDS;
			case MILLI_OF_SECOND -> ChronoUnit.MILLIS;
			case NANO_OF_SECOND -> ChronoUnit.NANOS;
			default -> null;
		};
	}

	/** The ZonedDateTime value */
	public final ZonedDateTime zdt;

	/** The precision of this time value */
	public final ChronoField precision;

	/**
	 * Constructor.
	 *
	 * @param zdt The ZonedDateTime value.
	 * @param precision The precision of this time value.
	 */
	public GranularZonedDateTime(ZonedDateTime zdt, ChronoField precision) {
		this.zdt = zdt;
		this.precision = precision;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A new GranularZonedDateTime with the same values.
	 */
	public GranularZonedDateTime copy() {
		return new GranularZonedDateTime(zdt, precision);
	}

	/**
	 * Returns the ZonedDateTime value.
	 *
	 * @return The ZonedDateTime value.
	 */
	public ZonedDateTime getZonedDateTime() { return zdt; }

	/**
	 * Returns the precision of this time value.
	 *
	 * <p>
	 * The precision indicates the finest granularity of the time value, which determines
	 * how the value was parsed or created. For example:
	 * <ul>
	 * 	<li>{@link ChronoField#YEAR} - Year precision (e.g., "2011")
	 * 	<li>{@link ChronoField#MONTH_OF_YEAR} - Month precision (e.g., "2011-01")
	 * 	<li>{@link ChronoField#DAY_OF_MONTH} - Day precision (e.g., "2011-01-15")
	 * 	<li>{@link ChronoField#HOUR_OF_DAY} - Hour precision (e.g., "2011-01-15T12")
	 * 	<li>{@link ChronoField#MINUTE_OF_HOUR} - Minute precision (e.g., "2011-01-15T12:30")
	 * 	<li>{@link ChronoField#SECOND_OF_MINUTE} - Second precision (e.g., "2011-01-15T12:30:45")
	 * 	<li>{@link ChronoField#MILLI_OF_SECOND} - Millisecond precision (e.g., "2011-01-15T12:30:45.123")
	 * 	<li>{@link ChronoField#NANO_OF_SECOND} - Nanosecond precision (e.g., "2011-01-15T12:30:45.123456789")
	 * </ul>
	 *
	 * @return The precision of this time value.
	 */
	public ChronoField getPrecision() { return precision; }

	@Override
	public String toString() {
		return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "(" + precision + ")";
	}

	/**
	 * Rolls this time value by the specified amount using the specified field.
	 *
	 * <p>
	 * This method creates a new GranularZonedDateTime by adding the specified amount to the
	 * specified field. The precision of the returned object remains the same as this object.
	 *
	 * <h5 class='section'>Supported Fields:</h5>
	 * <ul>
	 * 	<li>{@link ChronoField#YEAR}
	 * 	<li>{@link ChronoField#MONTH_OF_YEAR}
	 * 	<li>{@link ChronoField#DAY_OF_MONTH}
	 * 	<li>{@link ChronoField#HOUR_OF_DAY}
	 * 	<li>{@link ChronoField#MINUTE_OF_HOUR}
	 * 	<li>{@link ChronoField#SECOND_OF_MINUTE}
	 * 	<li>{@link ChronoField#MILLI_OF_SECOND}
	 * 	<li>{@link ChronoField#NANO_OF_SECOND}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a datetime with hour precision</jc>
	 * 	GranularZonedDateTime <jv>gdt</jv> = GranularZonedDateTime.<jsm>of</jsm>(<js>"2011-01-15T12Z"</js>);
	 * 	<jc>// Roll forward by 2 hours</jc>
	 * 	<jv>gdt</jv> = <jv>gdt</jv>.<jsm>roll</jsm>(<jv>ChronoField</jv>.<jf>HOUR_OF_DAY</jf>, 2);
	 * 	<jc>// Result: 2011-01-15T14:00:00Z (precision still HOUR_OF_DAY)</jc>
	 * </p>
	 *
	 * @param field The field to roll by. Must be one of the supported fields listed above.
	 * @param amount The amount to roll by. Positive values roll forward, negative values roll backward.
	 * @return A new GranularZonedDateTime with the rolled value.
	 * @throws IllegalArgumentException If the field is not supported.
	 */
	public GranularZonedDateTime roll(ChronoField field, int amount) {
		var unit = toChronoUnit(field);
		assertArg(unit != null, "Unsupported roll field: {0}", field);
		var newZdt = zdt.plus(amount, unit);
		return new GranularZonedDateTime(newZdt, precision);
	}

	/**
	 * Rolls this time value by the specified amount using the current precision.
	 *
	 * <p>
	 * This is a convenience method that calls {@link #roll(ChronoField, int)} using this
	 * object's precision field.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a datetime with year precision</jc>
	 * 	GranularZonedDateTime <jv>gdt</jv> = GranularZonedDateTime.<jsm>of</jsm>(<js>"2011"</js>);
	 * 	<jc>// Roll forward by 1 (using YEAR precision)</jc>
	 * 	<jv>gdt</jv> = <jv>gdt</jv>.<jsm>roll</jsm>(1);
	 * 	<jc>// Result: 2012-01-01T00:00:00 (precision still YEAR)</jc>
	 * </p>
	 *
	 * @param amount The amount to roll by. Positive values roll forward, negative values roll backward.
	 * @return A new GranularZonedDateTime with the rolled value.
	 */
	public GranularZonedDateTime roll(int amount) {
		return roll(precision, amount);
	}
}
