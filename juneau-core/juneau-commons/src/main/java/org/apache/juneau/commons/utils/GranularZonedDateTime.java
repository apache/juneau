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

import static org.apache.juneau.commons.utils.DateUtils.*;
import static org.apache.juneau.commons.utils.StateEnum.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
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
		// Try DateUtils.fromIso8601 first for consistency
		ZonedDateTime zdt = fromIso8601(seg);
		if (nn(zdt)) {
			// Determine precision based on the input string
			var precision = getPrecisionFromString(seg);
			return new GranularZonedDateTime(zdt, precision);
		}

		throw rex("Invalid date encountered: ''{0}''", seg);
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

		var state = S1;
		var precision = ChronoField.YEAR; // Track precision as we go

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
}
