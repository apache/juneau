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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.StateEnum.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

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
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class DateUtils {

	/**
	 * A factory for {@link SimpleDateFormat}s.
	 *
	 * <p>
	 * The instances are stored in a thread-local way because SimpleDateFormat is not thread-safe as noted in
	 * {@link SimpleDateFormat its javadoc}.
	 */
	static class DateFormatHolder {
		private static final ThreadLocal<SoftReference<Map<String,SimpleDateFormat>>> THREADLOCAL_FORMATS = new ThreadLocal<>() {
			@Override
			protected SoftReference<Map<String,SimpleDateFormat>> initialValue() {
				var m = new HashMap<String,SimpleDateFormat>();
				return new SoftReference<>(m);
			}
		};

		public static void clearThreadLocal() {
			THREADLOCAL_FORMATS.remove();
		}

		/**
		 * Creates a {@link SimpleDateFormat} for the requested format string.
		 *
		 * @param pattern
		 * 	A non-<c>null</c> format String according to {@link SimpleDateFormat}.
		 * 	The format is not checked against <c>null</c> since all paths go through {@link DateUtils}.
		 * @return
		 * 	The requested format.
		 * 	This simple date-format should not be used to {@link SimpleDateFormat#applyPattern(String) apply} to a
		 * 	different pattern.
		 */
		public static SimpleDateFormat formatFor(final String pattern) {
			final SoftReference<Map<String,SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
			Map<String,SimpleDateFormat> formats = ref.get();
			if (formats == null) {
				formats = new HashMap<>();
				THREADLOCAL_FORMATS.set(new SoftReference<>(formats));
			}
			SimpleDateFormat format = formats.get(pattern);
			if (format == null) {
				format = new SimpleDateFormat(pattern, Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				formats.put(pattern, format);
			}
			return format;
		}
	}

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1123 format.
	 */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1036 format.
	 */
	public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
	/**
	 * Date format pattern used to parse HTTP date headers in ANSI C <c>asctime()</c> format.
	 */
	public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	static {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(GMT);
		calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	/**
	 * Clears thread-local variable containing {@link java.text.DateFormat} cache.
	 */
	public static void clearThreadLocal() {
		DateFormatHolder.clearThreadLocal();
	}

	/**
	 * Formats the given date according to the specified pattern.
	 *
	 * <p>
	 * The pattern must conform to that used by the {@link SimpleDateFormat simple date format} class.
	 *
	 * @param date The date to format.
	 * @param pattern The pattern to use for formatting the date.
	 * @return A formatted date string.
	 * @throws IllegalArgumentException If the given date pattern is invalid.
	 * @see SimpleDateFormat
	 */
	public static String formatDate(final Date date, final String pattern) {
		final SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
		return formatter.format(date);
	}

	/**
	 * Returns a {@link DateTimeFormatter} using either a pattern or predefined pattern name.
	 *
	 * @param pattern The pattern (e.g. <js>"yyyy-MM-dd"</js>) or pattern name (e.g. <js>"ISO_INSTANT"</js>).
	 * @return The formatter.
	 */
	public static DateTimeFormatter getDateTimeFormatter(String pattern) {
		if (StringUtils.isEmpty(pattern))
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
	 * @see #toIso8601(Calendar)
	 */
	public static Calendar fromIso8601Calendar(String s) {
		if (StringUtils.isBlank(s))
			return null;
		return GregorianCalendar.from(fromIso8601(s));
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
	public static ZonedDateTime fromIso8601(String s) {
		if (StringUtils.isBlank(s))
			return null;
		String validDate = toValidIso8601DT(s);
		return ZonedDateTime.parse(validDate, DateTimeFormatter.ISO_DATE_TIME);
	}

	/**
	 * Converts a Calendar object to an ISO8601 formatted string.
	 *
	 * <p>
	 * This method formats a Calendar object into a standard ISO8601 date/time string
	 * with timezone information. The output format follows the pattern:
	 * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code>
	 *
	 * <p>
	 * The method preserves the timezone information from the Calendar object and
	 * formats it according to ISO8601 standards, including the timezone offset.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a Calendar with a specific timezone</jc>
	 * 	Calendar <jv>cal</jv> = Calendar.getInstance(TimeZone.getTimeZone(<js>"America/New_York"</js>));
	 * 	<jv>cal</jv>.set(2024, Calendar.JANUARY, 15, 14, 30, 45);
	 * 	<jv>cal</jv>.set(Calendar.MILLISECOND, 123);
	 *
	 * 	<jc>// Convert to ISO8601 string</jc>
	 * 	String <jv>iso8601</jv> = DateUtils.<jsm>toIso8601</jsm>(<jv>cal</jv>);
	 * 	<jc>// Result: "2024-01-15T14:30:45-05:00" (or -04:00 during DST)</jc>
	 *
	 * 	<jc>// UTC timezone example</jc>
	 * 	Calendar <jv>utcCal</jv> = Calendar.getInstance(TimeZone.getTimeZone(<js>"UTC"</js>));
	 * 	<jv>utcCal</jv>.set(2024, Calendar.JANUARY, 15, 19, 30, 45);
	 * 	String <jv>utcIso</jv> = DateUtils.<jsm>toIso8601</jsm>(<jv>utcCal</jv>);
	 * 	<jc>// Result: "2024-01-15T19:30:45Z"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Format Details:</h5>
	 * <ul>
	 * 	<li><c>yyyy</c> - 4-digit year
	 * 	<li><c>MM</c> - 2-digit month (01-12)
	 * 	<li><c>dd</c> - 2-digit day of month (01-31)
	 * 	<li><c>T</c> - Literal 'T' separator between date and time
	 * 	<li><c>HH</c> - 2-digit hour in 24-hour format (00-23)
	 * 	<li><c>mm</c> - 2-digit minute (00-59)
	 * 	<li><c>ss</c> - 2-digit second (00-59)
	 * 	<li><c>XXX</c> - Timezone offset (+HH:mm, -HH:mm, or Z for UTC)
	 * </ul>
	 *
	 * <h5 class='section'>Timezone Handling:</h5>
	 * <p>
	 * The method uses the Calendar's timezone to determine the appropriate offset.
	 * UTC timezones are represented as 'Z', while other timezones show their
	 * offset from UTC (e.g., -05:00 for EST, -04:00 for EDT).
	 * </p>
	 *
	 * See Also:  <a class="doclink" href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 - Wikipedia</a>
	 *
	 * @param c The Calendar object to convert (cannot be null)
	 * @return ISO8601 formatted string representation of the Calendar
	 * @throws NullPointerException if the Calendar parameter is null
	 * @see SimpleDateFormat
	 * @see Calendar#getTimeZone()
	 */
	public static String toIso8601(Calendar c) {
		var sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		sdf.setTimeZone(c.getTimeZone());
		return sdf.format(c.getTime());
	}

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
		boolean needsT = false;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
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
			} else if (state == S5) {
				if (c == ':')
					state = S6;
			} else if (state == S6) {
				if (c == 'Z' || c == '+' || c == '-')
					state = S7;
			}
		}

		if (needsT)
			in = in.replace(' ', 'T');

		String result = switch (state) {
			case S1 -> in + "-01-01T00:00:00";
			case S2 -> in + "-01T00:00:00";
			case S3 -> in + "T00:00:00";
			case S4 -> in + ":00:00";
			case S5 -> in + ":00";
			case S6 -> in;  // Complete time, no timezone
			case S7 -> in;  // Complete time with timezone
			default -> in;
		};

		if (state != S7)
			result += ZonedDateTime.now(ZoneId.systemDefault()).getOffset().toString();

		return result;
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
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Year precision</jc>
	 * 	ChronoField <jv>precision1</jv> = DateUtils.<jsm>getPrecisionFromString</jsm>(<js>"2011"</js>);
	 * 	<jc>// Returns ChronoField.YEAR</jc>
	 *
	 * 	<jc>// Month precision</jc>
	 * 	ChronoField <jv>precision2</jv> = DateUtils.<jsm>getPrecisionFromString</jsm>(<js>"2011-01"</js>);
	 * 	<jc>// Returns ChronoField.MONTH_OF_YEAR</jc>
	 *
	 * 	<jc>// Day precision</jc>
	 * 	ChronoField <jv>precision3</jv> = DateUtils.<jsm>getPrecisionFromString</jsm>(<js>"2011-01-01"</js>);
	 * 	<jc>// Returns ChronoField.DAY_OF_MONTH</jc>
	 *
	 * 	<jc>// Hour precision with timezone</jc>
	 * 	ChronoField <jv>precision4</jv> = DateUtils.<jsm>getPrecisionFromString</jsm>(<js>"2011-01-01T12Z"</js>);
	 * 	<jc>// Returns ChronoField.HOUR_OF_DAY</jc>
	 *
	 * 	<jc>// Millisecond precision</jc>
	 * 	ChronoField <jv>precision5</jv> = DateUtils.<jsm>getPrecisionFromString</jsm>(<js>"2011-01-01T12:30:45.123"</js>);
	 * 	<jc>// Returns ChronoField.MILLI_OF_SECOND</jc>
	 * </p>
	 *
	 * See Also: <a class="doclink" href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601 - Wikipedia</a>
	 *
	 * @param seg The date/time string to analyze (can be null or empty)
	 * @return The ChronoField representing the precision level, or {@link ChronoField#MILLI_OF_SECOND} for invalid/empty strings
	 * @see ChronoField
	 */
	public static ChronoField getPrecisionFromString(String seg) {
		if (StringUtils.isEmpty(seg))
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

		StateEnum state = S1;
		var precision = ChronoField.YEAR; // Track precision as we go

		for (int i = 0; i < seg.length(); i++) {
			char c = seg.charAt(i);

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

	// ================================================================================================================
	// ChronoField/ChronoUnit/Calendar conversion utilities
	// ================================================================================================================

	/**
	 * Converts a ChronoUnit to its corresponding ChronoField.
	 *
	 * <p>
	 * This method provides a mapping from time units to date/time fields.
	 * Not all ChronoUnit values have direct ChronoField equivalents.
	 *
	 * @param unit The ChronoUnit to convert
	 * @return The corresponding ChronoField, or null if no direct mapping exists
	 * @see ChronoUnit
	 * @see ChronoField
	 */
	public static ChronoField toChronoField(ChronoUnit unit) {
		return switch (unit) {
			case YEARS -> ChronoField.YEAR;
			case MONTHS -> ChronoField.MONTH_OF_YEAR;
			case DAYS -> ChronoField.DAY_OF_MONTH;
			case HOURS -> ChronoField.HOUR_OF_DAY;
			case MINUTES -> ChronoField.MINUTE_OF_HOUR;
			case SECONDS -> ChronoField.SECOND_OF_MINUTE;
			case MILLIS -> ChronoField.MILLI_OF_SECOND;
			default -> null;
		};
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
	 * @see ChronoField
	 * @see ChronoUnit
	 */
	public static ChronoUnit toChronoUnit(ChronoField field) {
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
	 * Converts a ChronoField to its corresponding Calendar field constant.
	 *
	 * <p>
	 * This method provides a mapping from modern ChronoField values to legacy
	 * Calendar field constants for use with Calendar.add() and similar methods.
	 *
	 * @param field The ChronoField to convert
	 * @return The corresponding Calendar field constant
	 * @see ChronoField
	 * @see Calendar
	 */
	public static int toCalendarField(ChronoField field) {
		return switch (field) {
			case YEAR -> Calendar.YEAR;
			case MONTH_OF_YEAR -> Calendar.MONTH;
			case DAY_OF_MONTH -> Calendar.DAY_OF_MONTH;
			case HOUR_OF_DAY -> Calendar.HOUR_OF_DAY;
			case MINUTE_OF_HOUR -> Calendar.MINUTE;
			case SECOND_OF_MINUTE -> Calendar.SECOND;
			case MILLI_OF_SECOND -> Calendar.MILLISECOND;
			default -> Calendar.MILLISECOND;
		};
	}

	/**
	 * Adds or subtracts a number of days from the specified calendar.
	 *
	 * <p>Creates a clone of the calendar before modifying it.
	 *
	 * @param c The calendar to modify.
	 * @param days The number of days to add (positive) or subtract (negative).
	 * @return A cloned calendar with the updated date, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static Calendar addSubtractDays(Calendar c, int days) {
		return opt(c)
			.map(x -> (Calendar)x.clone())
			.map(x -> add(x, Calendar.DATE, days))
			.orElse(null);
	}

	/**
	 * Adds to a field of a calendar.
	 *
	 * @param c The calendar to modify.
	 * @param field The calendar field to modify (e.g., {@link Calendar#DATE}, {@link Calendar#MONTH}).
	 * @param amount The amount to add.
	 * @return The same calendar with the field modified.
	 */
	public static Calendar add(Calendar c, int field, int amount) {
		c.add(field, amount);
		return c;
	}

	/**
	 * Converts a calendar to a {@link ZonedDateTime}.
	 *
	 * @param c The calendar to convert.
	 * @return An {@link Optional} containing the {@link ZonedDateTime}, or empty if the input was <jk>null</jk>.
	 */
	public static Optional<ZonedDateTime> toZonedDateTime(Calendar c) {
		return opt(c).map(GregorianCalendar.class::cast).map(GregorianCalendar::toZonedDateTime);
	}
}