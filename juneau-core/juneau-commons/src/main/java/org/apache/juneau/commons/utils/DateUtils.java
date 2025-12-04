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

import java.lang.ref.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and other headers.
 *
 * <p>
 * This class handles dates as defined by RFC 2616 section 3.3.1 as well as some other common non-standard formats.
 *
 * <p>
 * This class was copied from HttpClient 4.3.
 *
 */
public class DateUtils {

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
	public static ZonedDateTime fromIso8601(String s) {
		if (StringUtils.isBlank(s))
			return null;
		String validDate = toValidIso8601DT(s);
		return ZonedDateTime.parse(validDate, DateTimeFormatter.ISO_DATE_TIME);
	}

	/**
	 * Parses an ISO8601 date string into a Calendar object.
	 *
	 * <p>
	 * This method converts an ISO8601 formatted date/time string into a Calendar object,
	 * preserving timezone information and handling various ISO8601 formats. The method
	 * automatically normalizes the input string to ensure it can be parsed correctly.
	 *
	 * <p>
	 * The method supports the following ISO8601 formats:
	 * <ul>
	 * 	<li><js>"2024-01-15T14:30:45Z"</js> - UTC timezone
	 * 	<li><js>"2024-01-15T14:30:45-05:00"</js> - Offset timezone
	 * 	<li><js>"2024-01-15T14:30:45+09:00"</js> - Positive offset timezone
	 * 	<li><js>"2024-01-15"</js> - Date only (time defaults to 00:00:00)
	 * 	<li><js>"2024-01-15T14:30"</js> - Date and time (seconds default to 00)
	 * 	<li><js>"2024-01-15T14:30:45.123"</js> - With milliseconds
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse UTC timezone</jc>
	 * 	Calendar <jv>utcCal</jv> = DateUtils.<jsm>fromIso8601Calendar</jsm>(<js>"2024-01-15T14:30:45Z"</js>);
	 * 	<jc>// Result: Calendar with UTC timezone</jc>
	 *
	 * 	<jc>// Parse offset timezone</jc>
	 * 	Calendar <jv>estCal</jv> = DateUtils.<jsm>fromIso8601Calendar</jsm>(<js>"2024-01-15T14:30:45-05:00"</js>);
	 * 	<jc>// Result: Calendar with EST timezone (-05:00)</jc>
	 *
	 * 	<jc>// Parse date only</jc>
	 * 	Calendar <jv>dateCal</jv> = DateUtils.<jsm>fromIso8601Calendar</jsm>(<js>"2024-01-15"</js>);
	 * 	<jc>// Result: Calendar with time set to 00:00:00 in system timezone</jc>
	 *
	 * 	<jc>// Parse with milliseconds</jc>
	 * 	Calendar <jv>msCal</jv> = DateUtils.<jsm>fromIso8601Calendar</jsm>(<js>"2024-01-15T14:30:45.123Z"</js>);
	 * 	<jc>// Result: Calendar with 123 milliseconds</jc>
	 * </p>
	 *
	 * <h5 class='section'>Timezone Handling:</h5>
	 * <p>
	 * The method preserves the original timezone information from the ISO8601 string.
	 * If no timezone is specified, the system's default timezone is used. The resulting
	 * Calendar object will have the appropriate timezone set.
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
	 * @return Calendar object representing the parsed date/time, or null if input is null/empty
	 * @throws DateTimeParseException if the string cannot be parsed as a valid ISO8601 date
	 * @see #fromIso8601(String)
	 */
	public static Calendar fromIso8601Calendar(String s) {
		if (isBlank(s))
			return null;
		return GregorianCalendar.from(fromIso8601(s));
	}

	/**
	 * Returns a {@link DateTimeFormatter} using either a pattern or predefined pattern name.
	 *
	 * @param pattern The pattern (e.g. <js>"yyyy-MM-dd"</js>) or pattern name (e.g. <js>"ISO_INSTANT"</js>).
	 * @return The formatter.
	 */
	public static DateTimeFormatter getDateTimeFormatter(String pattern) {
		if (isEmpty(pattern))
			return DateTimeFormatter.ISO_INSTANT;
		try {
			for (var f : DateTimeFormatter.class.getFields()) {
				if (f.getName().equals(pattern)) {
					return (DateTimeFormatter)f.get(null);
				}
			}
			return DateTimeFormatter.ofPattern(pattern);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw toRex(e);
		}
	}


	// ================================================================================================================
	// ChronoField/ChronoUnit/Calendar conversion utilities
	// ================================================================================================================


	/**
	 * Pads out an ISO8601 string so that it can be parsed using {@link DatatypeConverter#parseDateTime(String)}.
	 *
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js> -&gt; <js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js> -&gt; <js>"2001-07-04T15:30:45Z"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1Z"</js> -&gt; <js>"2001-07-04T15:30:45.1Z"</js>
	 * 	<li><js>"2001-07-04T15:30Z"</js> -&gt; <js>"2001-07-04T15:30:00Z"</js>
	 * 	<li><js>"2001-07-04T15:30"</js> -&gt; <js>"2001-07-04T15:30:00"</js>
	 * 	<li><js>"2001-07-04"</js> -&gt; <li><js>"2001-07-04T00:00:00"</js>
	 * 	<li><js>"2001-07"</js> -&gt; <js>"2001-07-01T00:00:00"</js>
	 * 	<li><js>"2001"</js> -&gt; <js>"2001-01-01T00:00:00"</js>
	 * </ul>
	 *
	 * @param in The string to pad.
	 * @return The padded string.
	 */
	public static String toValidIso8601DT(String in) {
		assertArgNotNull("in", in);

		in = in.trim();

		// "2001-07-04T15:30:45Z"
		// S1: Looking for -
		// S2: Found -, looking for -
		// S3: Found -, looking for T
		// S4: Found T, looking for :
		// S5: Found :, looking for :
		// S6: Found :, looking for Z or - or +
		// S7: Found time zone

		var state = S1;
		var needsT = false;
		var timezoneAfterHour = false;  // Track if timezone found after hour (S4)
		var timezoneAfterMinute = false;  // Track if timezone found after minute (S5)
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
				if (c == 'Z' || c == '+' || c == '-')
					state = S7;
			}
		}

		if (needsT)
			in = in.replace(' ', 'T');

		var result = switch (state) {
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
			result += ZonedDateTime.now(ZoneId.systemDefault()).getOffset().toString();

		return result;
	}

	/**
	 * Parses an ISO8601 string into a calendar.
	 *
	 * <p>
	 * TODO-90: Investigate whether this helper can be removed in favor of java.time parsing (see TODO.md).
	 *
	 * <p>
	 * Supports any of the following formats:
	 * <br><c>yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS</c>
	 *
	 * @param date The date string.
	 * @return The parsed calendar.
	 * @throws IllegalArgumentException Value was not a valid date.
	 */
	public static Calendar parseIsoCalendar(String date) throws IllegalArgumentException {
		if (StringUtils.isEmpty(date))
			return null;
		date = date.trim().replace(' ', 'T');  // Convert to 'standard' ISO8601
		if (date.indexOf(',') != -1)  // Trim milliseconds
			date = date.substring(0, date.indexOf(','));
		if (date.matches("\\d{4}"))
			date += "-01-01T00:00:00";
		else if (date.matches("\\d{4}\\-\\d{2}"))
			date += "-01T00:00:00";
		else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}"))
			date += "T00:00:00";
		else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}"))
			date += ":00:00";
		else if (date.matches("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}\\:\\d{2}"))
			date += ":00";
		return fromIso8601Calendar(date);
	}

	/**
	 * Parses an ISO8601 string into a date.
	 *
	 * <p>
	 * Supports any of the following formats:
	 * <br><c>yyyy, yyyy-MM, yyyy-MM-dd, yyyy-MM-ddThh, yyyy-MM-ddThh:mm, yyyy-MM-ddThh:mm:ss, yyyy-MM-ddThh:mm:ss.SSS</c>
	 *
	 * @param date The date string.
	 * @return The parsed date.
	 * @throws IllegalArgumentException Value was not a valid date.
	 */
	public static Date parseIsoDate(String date) throws IllegalArgumentException {
		if (StringUtils.isEmpty(date))
			return null;
		return parseIsoCalendar(date).getTime();  // NOSONAR - NPE not possible.
	}

}