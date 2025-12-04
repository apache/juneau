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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StateEnum.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * A ZonedDateTime with precision information for granular time operations.
 *
 * <p>
 * This class combines a {@link ZonedDateTime} with a {@link ChronoField} precision identifier,
 * allowing for granular time operations such as rolling by specific time units.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create with year precision</jc>
 * 	GranularZonedDateTime <jv>gdt</jv> = GranularZonedDateTime.parse(<js>"2011"</js>);
 * 	<jc>// Roll forward by one year</jc>
 * 	<jv>gdt</jv>.roll(1);
 * 	<jc>// Get the ZonedDateTime</jc>
 * 	ZonedDateTime <jv>zdt</jv> = <jv>gdt</jv>.getZonedDateTime();
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
	 * Parses a timestamp string and returns a GranularZonedDateTime.
	 *
	 * <p>
	 * This method uses {@link DateUtils#fromIso8601(String)} for parsing and
	 * determines precision based on the input string format.
	 *
	 * @param seg The string segment to parse.
	 * @return A GranularZonedDateTime representing the parsed timestamp.
	 * @throws BasicRuntimeException If the string cannot be parsed as a valid timestamp.
	 */
	public static GranularZonedDateTime parse(String seg) {
		return parse2(seg);
	}

	/** The ZonedDateTime value */
	public final ZonedDateTime zdt;

	/** The precision of this time value */
	public final ChronoField precision;

	/**
	 * Constructor.
	 *
	 * @param date The date to wrap.
	 * @param precision The precision of this time value.
	 */
	public GranularZonedDateTime(Date date, ChronoField precision) {
		this.zdt = date.toInstant().atZone(ZoneId.systemDefault());
		this.precision = precision;
	}

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
	 * Rolls this time value by the specified amount using the specified field.
	 *
	 * @param field The field to roll by.
	 * @param amount The amount to roll by.
	 * @return A new GranularZonedDateTime with the rolled value.
	 */
	public GranularZonedDateTime roll(ChronoField field, int amount) {
		ChronoUnit unit = toChronoUnit(field);
		if (nn(unit)) {
			ZonedDateTime newZdt = zdt.plus(amount, unit);
			return new GranularZonedDateTime(newZdt, precision);
		}
		return this;
	}

	/**
	 * Rolls this time value by the specified amount using the current precision.
	 *
	 * @param amount The amount to roll by.
	 * @return A new GranularZonedDateTime with the rolled value.
	 */
	public GranularZonedDateTime roll(int amount) {
		return roll(precision, amount);
	}

	/**
	 * Converts a ChronoField to its corresponding ChronoUnit.
	 *
	 * <p>
	 * This method provides a mapping from date/time fields to time units.
	 * Not all ChronoField values have direct ChronoUnit equivalents.
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
			default -> null;
		};
	}

	/**
	 * Determines the precision level of an ISO8601 date/time string using a state machine.
	 *
	 * <p>
	 * This method analyzes the structure of a date/time string to determine the finest level of precision
	 * represented. It uses a state machine to parse the string character by character, tracking the precision
	 * level as it encounters different components.
	 *
	 * <p>
	 * The method supports the following ISO8601 formats:
	 * <ul>
	 * 	<li><js>"YYYY"</js> → {@link ChronoField#YEAR}
	 * 	<li><js>"YYYY-MM"</js> → {@link ChronoField#MONTH_OF_YEAR}
	 * 	<li><js>"YYYY-MM-DD"</js> → {@link ChronoField#DAY_OF_MONTH}
	 * 	<li><js>"YYYY-MM-DDTHH"</js> → {@link ChronoField#HOUR_OF_DAY}
	 * 	<li><js>"YYYY-MM-DDTHH:MM"</js> → {@link ChronoField#MINUTE_OF_HOUR}
	 * 	<li><js>"YYYY-MM-DDTHH:MM:SS"</js> → {@link ChronoField#SECOND_OF_MINUTE}
	 * 	<li><js>"YYYY-MM-DDTHH:MM:SS.SSS"</js> → {@link ChronoField#MILLI_OF_SECOND}
	 * </ul>
	 *
	 * <p>
	 * Timezone information (Z, +HH:mm, -HH:mm) is preserved but doesn't affect the precision level.
	 * Invalid or unrecognized formats default to {@link ChronoField#MILLI_OF_SECOND}.
	 *
	 * @param seg The date/time string to analyze (can be null or empty)
	 * @return The ChronoField representing the precision level, or {@link ChronoField#MILLI_OF_SECOND} for invalid/empty strings
	 */
	private static ChronoField getPrecisionFromString(String seg) {
		if (isEmpty(seg))
			return ChronoField.MILLI_OF_SECOND;

		// States:
		// S1: Looking for year digits (YYYY)
		// S2: Found year, looking for - or T or end (YYYY)
		// S3: Found -, looking for month digits (YYYY-MM)
		// S4: Found month, looking for - or T or end (YYYY-MM)
		// S5: Found -, looking for day digits (YYYY-MM-DD)
		// S6: Found day, looking for T or end (YYYY-MM-DD)
		// S7: Found T, looking for hour digits (YYYY-MM-DDTHH)
		// S8: Found hour, looking for : or end (YYYY-MM-DDTHH)
		// S9: Found :, looking for minute digits (YYYY-MM-DDTHH:MM)
		// S10: Found minute, looking for : or end (YYYY-MM-DDTHH:MM)
		// S11: Found :, looking for second digits (YYYY-MM-DDTHH:MM:SS)
		// S12: Found second, looking for . or end (YYYY-MM-DDTHH:MM:SS)
		// S13: Found ., looking for millisecond digits (YYYY-MM-DDTHH:MM:SS.SSS)
		// S14: Found timezone (Z, +HH:mm, -HH:mm)

		seg = seg.replace(' ', 'T').replace(',', '.');

		var state = S1;
		var precision = ChronoField.YEAR; // Track precision as we go

		int year, month, day, hour, minute, second, ms;

		for (var i = 0; i < seg.length(); i++) {
			var c = seg.charAt(i);

			if (state == S1) {
				// S1: Looking for year digits (YYYY)
				if (Character.isDigit(c)) {
					state = S2;
				} else if (c == '-') {
					state = S3;
					precision = ChronoField.MONTH_OF_YEAR;
				} else if (c == 'T') {
					state = S7;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (YEAR)
				}
			} else if (state == S2) {
				// S2: Found year, looking for - or T or end (YYYY)
				if (c == '-') {
					state = S3;
					precision = ChronoField.MONTH_OF_YEAR;
				} else if (c == 'T') {
					state = S7;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (YEAR)
				}
			} else if (state == S3) {
				// S3: Found -, looking for month digits (YYYY-MM)
				if (Character.isDigit(c)) {
					state = S4;
				} else if (c == '-') {
					state = S5;
					precision = ChronoField.DAY_OF_MONTH;
				} else if (c == 'T') {
					state = S7;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (MONTH_OF_YEAR)
				}
			} else if (state == S4) {
				// S4: Found month, looking for - or T or end (YYYY-MM)
				if (c == '-') {
					state = S5;
					precision = ChronoField.DAY_OF_MONTH;
				} else if (c == 'T') {
					state = S7;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (MONTH_OF_YEAR)
				}
			} else if (state == S5) {
				// S5: Found -, looking for day digits (YYYY-MM-DD)
				if (Character.isDigit(c)) {
					state = S6;
				} else if (c == 'T') {
					state = S7;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (DAY_OF_MONTH)
				}
			} else if (state == S6) {
				// S6: Found day, looking for T or end (YYYY-MM-DD)
				if (c == 'T') {
					state = S7;
					precision = ChronoField.HOUR_OF_DAY;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (DAY_OF_MONTH)
				}
			} else if (state == S7) {
				// S7: Found T, looking for hour digits (YYYY-MM-DDTHH)
				if (Character.isDigit(c)) {
					state = S8;
				} else if (c == ':') {
					state = S9;
					precision = ChronoField.MINUTE_OF_HOUR;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (HOUR_OF_DAY)
				}
			} else if (state == S8) {
				// S8: Found hour, looking for : or end (YYYY-MM-DDTHH)
				if (c == ':') {
					state = S9;
					precision = ChronoField.MINUTE_OF_HOUR;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (HOUR_OF_DAY)
				}
			} else if (state == S9) {
				// S9: Found :, looking for minute digits (YYYY-MM-DDTHH:MM)
				if (Character.isDigit(c)) {
					state = S10;
				} else if (c == ':') {
					state = S11;
					precision = ChronoField.SECOND_OF_MINUTE;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (MINUTE_OF_HOUR)
				}
			} else if (state == S10) {
				// S10: Found minute, looking for : or end (YYYY-MM-DDTHH:MM)
				if (c == ':') {
					state = S11;
					precision = ChronoField.SECOND_OF_MINUTE;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (MINUTE_OF_HOUR)
				}
			} else if (state == S11) {
				// S11: Found :, looking for second digits (YYYY-MM-DDTHH:MM:SS)
				if (Character.isDigit(c)) {
					state = S12;
				} else if (c == '.') {
					state = S13;
					precision = ChronoField.MILLI_OF_SECOND;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (SECOND_OF_MINUTE)
				}
			} else if (state == S12) {
				// S12: Found second, looking for . or end (YYYY-MM-DDTHH:MM:SS)
				if (c == '.') {
					state = S13;
					precision = ChronoField.MILLI_OF_SECOND;
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (SECOND_OF_MINUTE)
				}
			} else if (state == S13) {
				// S13: Found ., looking for millisecond digits (YYYY-MM-DDTHH:MM:SS.SSS)
				if (Character.isDigit(c)) {
					// Continue reading millisecond digits
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S14;
					// Keep current precision (MILLI_OF_SECOND)
				}
			} else if (state == S14) {
				// S14: Found timezone (Z, +HH:mm, -HH:mm) - precision already determined
				// Just continue reading timezone characters
			}
		}

		return precision;
	}

	/**
	 * Parses an ISO8601 date string into a ZonedDateTime object.
	 *
	 * <p>
	 * This method converts an ISO8601 formatted date/time string into a ZonedDateTime object,
	 * which provides full timezone context including DST handling. The method automatically
	 * normalizes the input string to ensure it can be parsed correctly.
	 *
	 * <p>
	 * The method supports the same ISO8601 formats as {@link #fromIso8601Calendar(String)},
	 * but returns a modern {@link ZonedDateTime} instead of a legacy {@link Calendar}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse UTC timezone</jc>
	 * 	ZonedDateTime <jv>utcZdt</jv> = DateUtils.<jsm>fromIso8601</jsm>(<js>"2024-01-15T14:30:45Z"</js>);
	 * 	<jc>// Result: ZonedDateTime with UTC timezone</jc>
	 *
	 * 	<jc>// Parse offset timezone</jc>
	 * 	ZonedDateTime <jv>estZdt</jv> = DateUtils.<jsm>fromIso8601</jsm>(<js>"2024-01-15T14:30:45-05:00"</js>);
	 * 	<jc>// Result: ZonedDateTime with EST timezone (-05:00)</jc>
	 *
	 * 	<jc>// Parse date only</jc>
	 * 	ZonedDateTime <jv>dateZdt</jv> = DateUtils.<jsm>fromIso8601</jsm>(<js>"2024-01-15"</js>);
	 * 	<jc>// Result: ZonedDateTime with time set to 00:00:00 in system timezone</jc>
	 * </p>
	 *
	 * <h5 class='section'>Advantages over Calendar:</h5>
	 * <ul>
	 * 	<li><c>Immutable</c> - Thread-safe by design
	 * 	<li><c>DST Aware</c> - Automatic Daylight Saving Time handling
	 * 	<li><c>Modern API</c> - Part of Java 8+ time package
	 * 	<li><c>Better Performance</c> - Optimized for modern JVMs
	 * 	<li><c>Type Safety</c> - Compile-time validation of operations
	 * </ul>
	 *
	 * <h5 class='section'>Timezone Handling:</h5>
	 * <p>
	 * The method preserves the original timezone information from the ISO8601 string.
	 * If no timezone is specified, the system's default timezone is used. The resulting
	 * ZonedDateTime object will have the appropriate timezone set and will automatically
	 * handle DST transitions.
	 * </p>
	 *
	 * <h5 class='section'>Input Normalization:</h5>
	 * <p>
	 * The method automatically normalizes incomplete ISO8601 strings by:
	 * <ul>
	 * 	<li>Adding missing time components (defaults to 00:00:00)
	 * 	<li>Adding timezone information if missing (uses system default)
	 * 	<li>Ensuring proper format compliance
	 * </ul>
	 * </p>
	 *
	 * See Also:  <a class="doclink" href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 - Wikipedia</a>
	 *
	 * @param s The ISO8601 formatted string to parse (can be null or empty)
	 * @return ZonedDateTime object representing the parsed date/time, or null if input is null/empty
	 * @throws DateTimeParseException if the string cannot be parsed as a valid ISO8601 date
	 * @see #fromIso8601Calendar(String)
	 * @see ZonedDateTime
	 */
	private static ZonedDateTime fromIso8601(String s) {
		if (StringUtils.isBlank(s))
			return null;

		// Inline toValidIso8601DT logic
		var in = s.trim();

		// "2001-07-04T15:30:45Z"
		// S1: Looking for -
		// S2: Found -, looking for -
		// S3: Found -, looking for T
		// S4: Found T, looking for :
		// S5: Found :, looking for :
		// S6: Found :, looking for Z or - or + or . or ,
		// S7: Found time zone
		// S8: Found . or , after seconds, skipping milliseconds digits

		var state = S1;
		var needsT = false;
		var timezoneAfterHour = false;  // Track if timezone found after hour (S4)
		var timezoneAfterMinute = false;  // Track if timezone found after minute (S5)
		var millisCommaIndex = -1;  // Track where comma-separated milliseconds start (to convert to dot)

		for (var i = 0; i < in.length(); i++) {
			var c = in.charAt(i);
			if (state == S1) {
				if (c == '-')
					state = S2;
			} else if (state == S2) {
				if (c == '-')
					state = S3;
			} else if (state == S3) {
				if (c == 'T')
					state = S4;
				if (c == ' ') {
					state = S4;
					needsT = true;
				}
			} else if (state == S4) {
				if (c == ':')
					state = S5;
				else if (c == 'Z' || c == '+' || c == '-') {
					state = S7;  // Timezone immediately after hour (e.g., "2011-01-15T12Z")
					timezoneAfterHour = true;
				}
			} else if (state == S5) {
				if (c == ':')
					state = S6;
				else if (c == 'Z' || c == '+' || c == '-') {
					state = S7;  // Timezone immediately after minute (e.g., "2011-01-15T12:30Z")
					timezoneAfterMinute = true;
				}
			} else if (state == S6) {
				if (c == '.' || c == ',') {
					state = S8;  // Found milliseconds separator
					if (c == ',') {
						millisCommaIndex = i;  // Track comma to convert to dot
					}
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S7;
				}
			} else if (state == S8) {
				// S8: Reading milliseconds digits, looking for timezone
				if (Character.isDigit(c)) {
					// Continue reading milliseconds
				} else if (c == 'Z' || c == '+' || c == '-') {
					state = S7;
				}
			}
		}

		// Convert comma-separated milliseconds to dot-separated (ISO_DATE_TIME expects dots)
		if (millisCommaIndex >= 0) {
			var chars = in.toCharArray();
			chars[millisCommaIndex] = '.';
			in = new String(chars);
		}

		if (needsT)
			in = in.replace(' ', 'T');

		var validDate = switch (state) {
			case S1 -> in + "-01-01T00:00:00";
			case S2 -> in + "-01T00:00:00";
			case S3 -> in + "T00:00:00";
			case S4 -> in + ":00:00";
			case S5 -> in + ":00";
			case S6 -> in;  // Complete time, no timezone
			case S7 -> {
				// Complete time with timezone, but may need to add missing components
				if (timezoneAfterHour) {
					// Timezone found after hour, need to add :00:00 before timezone
					var tzIndex = in.length();
					for (var i = in.length() - 1; i >= 0; i--) {
						var ch = in.charAt(i);
						if (ch == 'Z' || ch == '+' || ch == '-') {
							tzIndex = i;
							break;
						}
					}
					yield in.substring(0, tzIndex) + ":00:00" + in.substring(tzIndex);
				} else if (timezoneAfterMinute) {
					// Timezone found after minute, need to add :00 before timezone
					var tzIndex = in.length();
					for (var i = in.length() - 1; i >= 0; i--) {
						var ch = in.charAt(i);
						if (ch == 'Z' || ch == '+' || ch == '-') {
							tzIndex = i;
							break;
						}
					}
					yield in.substring(0, tzIndex) + ":00" + in.substring(tzIndex);
				} else {
					yield in;  // Complete time with timezone (already has seconds)
				}
			}
			default -> in;
		};


		if (state != S7)
			validDate += ZonedDateTime.now(ZoneId.systemDefault()).getOffset().toString();

		return ZonedDateTime.parse(validDate, DateTimeFormatter.ISO_DATE_TIME);
	}

	public static GranularZonedDateTime parse2(String seg) {
		return parse2(seg, null);
	}

	public static GranularZonedDateTime parse2(String seg, ZoneId defaultZoneId) {
		assertArgNotNull("seg", seg);
		var digit = StringUtils.DIGIT;

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

		for (var i = 0; i < seg.length(); i++) {
			var c = seg.charAt(i);

			if (state == S1) {
				// S01: Looking for Y(S02) or T(S07)
				if (digit.contains(c)) {
					mark = i;
					state = S2;
				} else if (c == 'T') {
					timeOnly = true; // Mark as time-only format
					state = S7;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S2) {
				// S02: Found Y, looking for Y(S02)/-(S03)/T(S07)
				if (digit.contains(c)) {
					// Stay in S2
				} else if (c == '-') {
					year = parse(seg, 4, mark, i, 0, 9999);
					state = S3;
				} else if (c == 'T') {
					year = parse(seg, 4, mark, i, 0, 9999);
					state = S7;
				} else if (c == 'Z') {
					zoneId = ZoneId.of("Z");
					year = parse(seg, 4, mark, i, 0, 9999);
					state = S15;
				} else if (c == '+') {
					year = parse(seg, 4, mark, i, 0, 9999);
					nego = false;
					state = S16;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S3) {
				// S03: Found -, looking for M(S04)
				if (digit.contains(c)) {
					mark = i;
					state = S4;
					precision = ChronoField.MONTH_OF_YEAR;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S4) {
				// S04: Found M, looking for M(S04)/-(S05)/T(S07)
				if (digit.contains(c)) {
					// Stay in S4
				} else if (c == '-') {
					month = parse(seg, 2, mark, i, 1, 12);
					state = S5;
				} else if (c == 'T') {
					month = parse(seg, 2, mark, i, 1, 12);
					state = S7;
				} else if (c == 'Z') {
					month = parse(seg, 2, mark, i, 1, 12);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					month = parse(seg, 2, mark, i, 1, 12);
					nego = false;
					state = S16;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S5) {
				// S05: Found -, looking for D(S06)
				if (digit.contains(c)) {
					mark = i;
					state = S6;
					precision = ChronoField.DAY_OF_MONTH;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S6) {
				// S06: Found D, looking for D(S06)/T(S07)
				if (digit.contains(c)) {
					// Stay in S6
				} else if (c == 'T') {
					day = parse(seg, 2, mark, i, 1, 31);
					state = S7;
				} else if (c == 'Z') {
					day = parse(seg, 2, mark, i, 1, 31);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					day = parse(seg, 2, mark, i, 1, 31);
					nego = false;
					state = S16;
				} else if (c == '-') {
					day = parse(seg, 2, mark, i, 1, 31);
					nego = true;
					state = S17;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S7) {
				// S07: Found T, looking for h(S08)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					mark = i;
					state = S8;
					precision = ChronoField.HOUR_OF_DAY;
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
					throw bad(seg, i);
				}
			} else if (state == S8) {
				// S08: Found h, looking for h(S08)/:(S09)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S8
				} else if (c == ':') {
					hour = parse(seg, 2, mark, i, 0, 23);
					state = S9;
				} else if (c == 'Z') {
					hour = parse(seg, 2, mark, i, 0, 23);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					hour = parse(seg, 2, mark, i, 0, 23);
					nego = false;
					state = S16;
				} else if (c == '-') {
					hour = parse(seg, 2, mark, i, 0, 23);
					nego = true;
					state = S17;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S9) {
				// S09: Found :, looking for m(S10)
				if (digit.contains(c)) {
					mark = i;
					state = S10;
					precision = ChronoField.MINUTE_OF_HOUR;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S10) {
				// S10: Found m, looking for m(S10)/:(S11)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S10
				} else if (c == ':') {
					minute = parse(seg, 2, mark, i, 0, 59);
					state = S11;
				} else if (c == 'Z') {
					minute = parse(seg, 2, mark, i, 0, 59);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					minute = parse(seg, 2, mark, i, 0, 59);
					nego = false;
					state = S16;
				} else if (c == '-') {
					minute = parse(seg, 2, mark, i, 0, 59);
					nego = true;
					state = S17;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S11) {
				// S11: Found :, looking for s(S12)
				if (digit.contains(c)) {
					mark = i;
					state = S12;
					precision = ChronoField.SECOND_OF_MINUTE;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S12) {
				// S12: Found s, looking for s(S12)/.(S13)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S12
			} else if (c == '.' || c == ',') {
				second = parse(seg, 2, mark, i, 0, 59);
				state = S13;
				// Precision will be set based on number of fractional digits
				} else if (c == 'Z') {
					second = parse(seg, 2, mark, i, 0, 59);
					zoneId = ZoneId.of("Z");
					state = S15;
				} else if (c == '+') {
					second = parse(seg, 2, mark, i, 0, 59);
					nego = false;
					state = S16;
				} else if (c == '-') {
					second = parse(seg, 2, mark, i, 0, 59);
					nego = true;
					state = S17;
				} else {
					throw bad(seg, i);
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
					throw bad(seg, i);
				}
			} else if (state == S14) {
				// S14: Found S, looking for S(S14)/Z(S15)/+(S16)/-(S17)
				if (digit.contains(c)) {
					// Stay in S14
				} else if (c == 'Z') {
					nanos = parseNanos(seg, mark, i);
					zoneId = ZoneId.of("Z");
					// Set precision based on number of fractional digits: 1-3 = milliseconds, 4-9 = nanoseconds
					var digitCount = i - mark;
					precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
					state = S15;
				} else if (c == '+') {
					nanos = parseNanos(seg, mark, i);
					// Set precision based on number of fractional digits: 1-3 = milliseconds, 4-9 = nanoseconds
					var digitCount = i - mark;
					precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
					nego = false;
					state = S16;
				} else if (c == '-') {
					nanos = parseNanos(seg, mark, i);
					// Set precision based on number of fractional digits: 1-3 = milliseconds, 4-9 = nanoseconds
					var digitCount = i - mark;
					precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
					nego = true;
					state = S17;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S15) {
				// Shouldn't find anything after Z
				throw bad(seg, i);
			} else if (state == S16) {
				// S16: Found +, looking for oh(S18)
				if (digit.contains(c)) {
					mark = i;
					state = S18;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S17) {
				// S17: Found -, looking for oh(S18)
				if (digit.contains(c)) {
					mark = i;
					state = S18;
				} else {
					throw bad(seg, i);
				}
			} else if (state == S18) {
				// S18: Found oh, looking for oh(S18)/:(S19)/end
				if (digit.contains(c)) {
					// Stay in S18
				} else if (c == ':') {
					ohour = parse(seg, 2, mark, i, 0, 18);
					state = S19;
				} else {
					throw bad(seg, i);
				}
				// If we reach end of string, ohour is complete (2 digits)
			} else if (state == S19) {
				// S19: Found :, looking for om(S20)
				if (digit.contains(c)) {
					mark = i;
					state = S20;
				} else {
					throw bad(seg, i);
				}
			} else /* (state == S20) */ {
				// S20: Found om, looking for om(S20)
				if (digit.contains(c)) {
					// Stay in S20
				} else {
					throw bad(seg, i);
				}
			}
		}

		var end = seg.length(); // end is exclusive (one past last character)
		if (state.isAny(S1, S3, S5, S7, S9, S11, S13, S16, S17, S19)) {
			throw bad(seg, end - 1);
		} else if (state == S2) {
			// S02: Found Y, looking for Y(S02)/-(S03)/T(S07).
			year = parse(seg, 4, mark, end, 0, 9999);
			precision = ChronoField.YEAR;
		} else if (state == S4) {
			// S04: Found M, looking for M(S04)/-(S05)/T(S07).
			month = parse(seg, 2, mark, end, 1, 12);
			precision = ChronoField.MONTH_OF_YEAR;
		} else if (state == S6) {
			// S06  Found D, looking for D(S06)/T(S07).
			day = parse(seg, 2, mark, end, 1, 31);
			precision = ChronoField.DAY_OF_MONTH;
		} else if (state == S8) {
			// S08: Found h, looking for h(S08)/:(S09)/Z(S15)/+(S16)/-(S17).
			hour = parse(seg, 2, mark, end, 0, 23);
			precision = ChronoField.HOUR_OF_DAY;
		} else if (state == S10) {
			// S10: Found m, looking for m(S10)/:(S11)/Z(S15)/+(S16)/-(S17).
			minute = parse(seg, 2, mark, end, 0, 59);
			precision = ChronoField.MINUTE_OF_HOUR;
		} else if (state == S12) {
			// S12: Found s, looking for s(S12)/.(S13)/Z(S15)/+(S16)/-(S17).
			second = parse(seg, 2, mark, end, 0, 59);
			precision = ChronoField.SECOND_OF_MINUTE;
		} else if (state == S14) {
			// S14: Found S, looking for S(S14)/Z(S15)/+(S16)/-(S17).
			nanos = parseNanos(seg, mark, end);
			// Set precision based on number of digits: 1-3 = milliseconds, 4-9 = nanoseconds
			var digitCount = end - mark;
			precision = (digitCount <= 3) ? ChronoField.MILLI_OF_SECOND : ChronoField.NANO_OF_SECOND;
		} else if (state == S15) {
			// S15: Found Z.
		} else if (state == S18) {
			// S18: Found oh, looking for oh(S18)/:(S19).
			// Check if we have 2 digits (+hh) or 4 digits (+hhmm)
			if (end - mark == 2) {
				ohour = parse(seg, 2, mark, end, 0, 18);
			} else if (end - mark == 4) {
				// +hhmm format: parse hours from mark to mark+2, minutes from mark+2 to end
				ohour = parse(seg, 2, mark, mark + 2, 0, 18);
				ominute = parse(seg, 2, mark + 2, end, 0, 59);
			} else {
				throw bad(seg, mark);
			}
		} else /* (state == S20) */ {
			// S20: Found om, looking for om(S20).
			ominute = parse(seg, 2, mark, end, 0, 59);
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
			zoneId = defaultZoneId != null ? defaultZoneId : ZoneId.systemDefault();
		}

		// Construct ZonedDateTime from parsed values
		// Default values for missing components
		// For time-only formats (started with "T"), use current date
		// For date formats, default to 1/1/1
		if (timeOnly) {
			// Time-only format: use current year/month/day
			var now = ZonedDateTime.now(zoneId);
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
}
