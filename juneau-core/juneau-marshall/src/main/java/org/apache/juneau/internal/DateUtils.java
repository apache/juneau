// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.ref.*;
import java.text.*;
import java.time.*;
import java.time.chrono.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import javax.xml.bind.*;

import org.apache.juneau.reflect.*;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and other headers.
 *
 * <p>
 * This class handles dates as defined by RFC 2616 section 3.3.1 as well as some other common non-standard formats.
 *
 * <p>
 * This class was copied from HttpClient 4.3.
 */
public final class DateUtils {

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
	private static final String[] DEFAULT_PATTERNS = new String[] { PATTERN_RFC1123, PATTERN_RFC1036, PATTERN_ASCTIME };
	private static final Date DEFAULT_TWO_DIGIT_YEAR_START;
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	static {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(GMT);
		calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
	}

	/**
	 * Parses a date value.
	 *
	 * <p>
	 * The formats used for parsing the date value are retrieved from the default http params.
	 *
	 * @param dateValue the date value to parse
	 * @return the parsed date or null if input could not be parsed
	 */
	public static Date parseDate(final String dateValue) {
		return parseDate(dateValue, null, null);
	}

	/**
	 * Parses the date value using the given date formats.
	 *
	 * @param dateValue the date value to parse
	 * @param dateFormats the date formats to use
	 * @return the parsed date or null if input could not be parsed
	 */
	public static Date parseDate(final String dateValue, final String[] dateFormats) {
		return parseDate(dateValue, dateFormats, null);
	}

	/**
	 * Parses the date value using the given date formats.
	 *
	 * @param dateValue the date value to parse
	 * @param dateFormats the date formats to use
	 * @param startDate
	 * 	During parsing, two digit years will be placed in the range <c>startDate</c> to
	 * 	<c>startDate + 100 years</c>. This value may be <c>null</c>. When
	 * 	<c>null</c> is given as a parameter, year <c>2000</c> will be used.
	 * @return the parsed date or null if input could not be parsed
	 */
	public static Date parseDate(final String dateValue, final String[] dateFormats, final Date startDate) {
		final String[] localDateFormats = dateFormats != null ? dateFormats : DEFAULT_PATTERNS;
		final Date localStartDate = startDate != null ? startDate : DEFAULT_TWO_DIGIT_YEAR_START;
		String v = dateValue;
		// trim single quotes around date if present
		// see issue #5279
		if (v.length() > 1 && v.startsWith("'") && v.endsWith("'")) {
			v = v.substring(1, v.length() - 1);
		}
		for (final String dateFormat : localDateFormats) {
			final SimpleDateFormat dateParser = DateFormatHolder.formatFor(dateFormat);
			dateParser.set2DigitYearStart(localStartDate);
			final ParsePosition pos = new ParsePosition(0);
			final Date result = dateParser.parse(v, pos);
			if (pos.getIndex() != 0) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Parses an ISO8601 string and converts it to a {@link Calendar}.
	 *
	 * @param s The string to parse.
	 * @return The parsed value, or <jk>null</jk> if the string was <jk>null</jk> or empty.
	 */
	public static Calendar parseISO8601Calendar(String s) {
		if (isEmpty(s))
			return null;
		return DatatypeConverter.parseDateTime(toValidISO8601DT(s));
	}

	/**
	 * Parses an ISO8601 string and converts it to a {@link Date}.
	 *
	 * @param s The string to parse.
	 * @return The parsed value, or <jk>null</jk> if the string was <jk>null</jk> or empty.
	 */
	public static Date parseISO8601(String s) {
		if (isEmpty(s))
			return null;
		return DatatypeConverter.parseDateTime(toValidISO8601DT(s)).getTime();
	}

	/**
	 * Formats the given date according to the RFC 1123 pattern.
	 *
	 * @param date The date to format.
	 * @return An RFC 1123 formatted date string.
	 * @see #PATTERN_RFC1123
	 */
	public static String formatDate(final Date date) {
		return formatDate(date, PATTERN_RFC1123);
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
	 * Clears thread-local variable containing {@link java.text.DateFormat} cache.
	 */
	public static void clearThreadLocal() {
		DateFormatHolder.clearThreadLocal();
	}

	/**
	 * A factory for {@link SimpleDateFormat}s.
	 *
	 * <p>
	 * The instances are stored in a thread-local way because SimpleDateFormat is not thread-safe as noted in
	 * {@link SimpleDateFormat its javadoc}.
	 */
	static final class DateFormatHolder {
		private static final ThreadLocal<SoftReference<Map<String,SimpleDateFormat>>> THREADLOCAL_FORMATS =
				new ThreadLocal<SoftReference<Map<String,SimpleDateFormat>>>() {
			@Override
			protected SoftReference<Map<String,SimpleDateFormat>> initialValue() {
				Map<String,SimpleDateFormat> m = new HashMap<>();
				return new SoftReference<>(m);
			}
		};

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

		public static void clearThreadLocal() {
			THREADLOCAL_FORMATS.remove();
		}
	}

	/**
	 * Pads out an ISO8601 string so that it can be parsed using {@link DatatypeConverter#parseDateTime(String)}.
	 *
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js> --&gt; <js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js> --&gt; <js>"2001-07-04T15:30:45Z"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1Z"</js> --&gt; <js>"2001-07-04T15:30:45.1Z"</js>
	 * 	<li><js>"2001-07-04T15:30Z"</js> --&gt; <js>"2001-07-04T15:30:00Z"</js>
	 * 	<li><js>"2001-07-04T15:30"</js> --&gt; <js>"2001-07-04T15:30:00"</js>
	 * 	<li><js>"2001-07-04"</js> --&gt; <li><js>"2001-07-04T00:00:00"</js>
	 * 	<li><js>"2001-07"</js> --&gt; <js>"2001-07-01T00:00:00"</js>
	 * 	<li><js>"2001"</js> --&gt; <js>"2001-01-01T00:00:00"</js>
	 * </ul>
	 *
	 * @param in The string to pad.
	 * @return The padded string.
	 */
	public static final String toValidISO8601DT(String in) {

		// "2001-07-04T15:30:45Z"
		final int
			S1 = 1, // Looking for -
			S2 = 2, // Found -, looking for -
			S3 = 3, // Found -, looking for T
			S4 = 4, // Found T, looking for :
			S5 = 5, // Found :, looking for :
			S6 = 6; // Found :

		int state = 1;
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
			}
		}

		if (needsT)
			in = in.replace(' ', 'T');
		switch(state) {
			case S1: return in + "-01-01T00:00:00";
			case S2: return in + "-01T00:00:00";
			case S3: return in + "T00:00:00";
			case S4: return in + ":00:00";
			case S5: return in + ":00";
			default: return in;
		}
	}


	/**
	 * Returns a {@link DateTimeFormatter} using either a pattern or predefined pattern name.
	 *
	 * @param pattern The pattern (e.g. <js>"yyyy-MM-dd"</js>) or pattern name (e.g. <js>"ISO_INSTANT"</js>).
	 * @return The formatter.
	 */
	public static DateTimeFormatter getFormatter(String pattern) {
		if (isEmpty(pattern))
			return DateTimeFormatter.ISO_INSTANT;
		try {
			FieldInfo fi = ClassInfo.of(DateTimeFormatter.class).getStaticPublicField(pattern);
			if (fi != null)
				return (DateTimeFormatter)fi.inner().get(null);
			return DateTimeFormatter.ofPattern(pattern);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the specified {@link Temporal} object to an {@link Instant} object.
	 *
	 * @param t The temporal object to convert.
	 * @param zoneId The time zone if not already included in temporal.
	 * @return The timestamp.
	 */
	public static Instant toInstant(Temporal t, ZoneId zoneId) {
		return toTemporal(t, Instant.class, zoneId);
	}

	/**
	 * Returns the specified {@link Temporal} object to a {@link ZonedDateTime} object.
	 *
	 * @param t The temporal object to convert.
	 * @param zoneId The time zone if not already included in temporal.
	 * @return The timestamp.
	 */
	public static ZonedDateTime toZonedDateTime(Temporal t, ZoneId zoneId) {
		return toTemporal(t, ZonedDateTime.class, zoneId);
	}

	/**
	 * Returns the specified {@link Temporal} object to a {@link Date} object.
	 *
	 * @param t The temporal object to convert.
	 * @param zoneId The time zone if not already included in temporal.
	 * @return The date.
	 */
	public static Date toDate(Temporal t, ZoneId zoneId) {
		return Date.from(toInstant(t, zoneId));
	}

	/**
	 * Returns the specified {@link Temporal} object to a {@link Calendar} object.
	 *
	 * @param t The temporal object to convert.
	 * @param zoneId The time zone if not already included in temporal.
	 * @return The date.
	 */
	public static Calendar toCalendar(Temporal t, ZoneId zoneId) {
		return GregorianCalendar.from(toZonedDateTime(t, zoneId));
	}

	private static final LocalDate ZERO_DATE = LocalDate.ofEpochDay(0);
	private static final MonthDay ZERO_MONTHDAY = MonthDay.of(1, 1);
	private static final LocalTime ZERO_TIME = LocalTime.MIDNIGHT;

	/**
	 * Converts an arbitrary {@link Temporal} to another arbitrary {@link Temporal}
	 *
	 * @param o The temporal to convert.
	 * @param tc The target temporal class.
	 * @param zoneId The time zone if not already included in temporal.
	 * @return The converted temporal.
	 * @param <T> The target temporal class.
	 */
	public static <T> T toTemporal(Temporal o, Class<T> tc, ZoneId zoneId) {
		if (o == null)
			return null;
		T t = toTemporal2(o, tc, zoneId);
		if (t.getClass() != tc)
			throw new RuntimeException("Temporal=["+o.getClass().getName()+"], wanted=["+tc.getName()+"], actual=["+t.getClass().getName()+"]");
		return t;
	}

	@SuppressWarnings("unchecked")
	private static <T> T toTemporal2(Temporal o, Class<T> tc, ZoneId zoneId) {

		if (o == null)
			return null;

		Class<? extends Temporal> oc = o.getClass();

		if (oc == tc)
			return (T)o;

		if (zoneId == null)
			zoneId = ZoneId.systemDefault();

		if (oc == Instant.class) {
			Instant t = (Instant)o;
			if (tc == LocalDate.class) {
				return (T)t.atZone(zoneId).toLocalDate();
			} else if (tc == LocalDateTime.class) {
				return (T)t.atZone(zoneId).toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)t.atZone(zoneId).toLocalTime();
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atZone(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atZone(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atZone(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atZone(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atZone(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atZone(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atZone(zoneId));
			}
		} else if (oc == LocalDate.class) {
			LocalDate t = (LocalDate)o;
			if (tc == Instant.class) {
				return (T)t.atStartOfDay(zoneId).toInstant();
			} else if (tc == LocalDateTime.class) {
				return (T)t.atStartOfDay(zoneId).toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)t.atStartOfDay(zoneId).toLocalTime();
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atStartOfDay(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atStartOfDay(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atStartOfDay(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atStartOfDay(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atStartOfDay(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atStartOfDay(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atStartOfDay(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atStartOfDay(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atStartOfDay(zoneId));
			}
		} else if (oc == LocalDateTime.class) {
			LocalDateTime t = (LocalDateTime)o;
			if (tc == Instant.class) {
				return (T)t.atZone(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)t.toLocalDate();
			} else if (tc == LocalTime.class) {
				return (T)t.toLocalTime();
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atZone(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atZone(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atZone(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atZone(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atZone(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atZone(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atZone(zoneId));
			}
		} else if (oc == LocalTime.class) {
			LocalTime t = (LocalTime)o;
			if (tc == Instant.class) {
				return (T)t.atDate(ZERO_DATE).atZone(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)LocalDate.ofEpochDay(0);
			} else if (tc == LocalDateTime.class) {
				return (T)t.atDate(ZERO_DATE);
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atDate(ZERO_DATE).atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atDate(ZERO_DATE).atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.of(1970);
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.of(1970, 1);
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atDate(ZERO_DATE).atZone(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(ZERO_DATE.atStartOfDay(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(ZERO_DATE.atStartOfDay(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(ZERO_DATE.atStartOfDay(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(ZERO_DATE.atStartOfDay(zoneId));
			}
		} else if (oc == OffsetDateTime.class) {
			OffsetDateTime t = (OffsetDateTime)o;
			if (tc == Instant.class) {
				return (T)t.toInstant();
			} else if (tc == LocalDate.class) {
				return (T)t.toLocalDate();
			} else if (tc == LocalDateTime.class) {
				return (T)t.toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)t.toLocalTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t);
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t);
			} else if (tc == ZonedDateTime.class) {
				return (T)t.toZonedDateTime();
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t);
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t);
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t);
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t);
			}
		} else if (oc == OffsetTime.class) {
			OffsetTime t = (OffsetTime)o;
			if (tc == Instant.class) {
				return (T)t.atDate(ZERO_DATE).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)LocalDate.ofEpochDay(0);
			} else if (tc == LocalDateTime.class) {
				return (T)t.atDate(ZERO_DATE).toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)t.atDate(ZERO_DATE).toLocalTime();
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atDate(ZERO_DATE);
			} else if (tc == Year.class) {
				return (T)Year.from(t.atDate(ZERO_DATE));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atDate(ZERO_DATE));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atDate(ZERO_DATE).toZonedDateTime();
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atDate(ZERO_DATE));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atDate(ZERO_DATE));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atDate(ZERO_DATE));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atDate(ZERO_DATE));
			}
		} else if (oc == Year.class) {
			Year t = (Year)o;
			if (tc == Instant.class) {
				return (T)t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)t.atMonthDay(ZERO_MONTHDAY);
			} else if (tc == LocalDateTime.class) {
				return (T)t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId).toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)ZERO_TIME;
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == YearMonth.class) {
				return (T)t.atMonth(1);
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atMonthDay(ZERO_MONTHDAY).atStartOfDay(zoneId));
			}
		} else if (oc == YearMonth.class) {
			YearMonth t = (YearMonth)o;
			if (tc == Instant.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId).toLocalDate();
			} else if (tc == LocalDateTime.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId).toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId).toLocalTime();
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t);
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atDay(1).atStartOfDay(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atDay(1).atStartOfDay(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atDay(1).atStartOfDay(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atDay(1).atStartOfDay(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atDay(1).atStartOfDay(zoneId));
			}
		} else if (oc == ZonedDateTime.class) {
			ZonedDateTime t = (ZonedDateTime)o;
			if (tc == Instant.class) {
				return (T)t.toInstant();
			} else if (tc == LocalDate.class) {
				return (T)t.toLocalDate();
			} else if (tc == LocalDateTime.class) {
				return (T)t.toLocalDateTime();
			} else if (tc == LocalTime.class) {
				return (T)t.toLocalTime();
			} else if (tc == OffsetDateTime.class) {
				return (T)t.toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t);
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t);
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t);
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t);
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t);
			}
		} else if (oc == HijrahDate.class) {
			HijrahDate t = (HijrahDate)o;
			if (tc == Instant.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay());
			} else if (tc == LocalDateTime.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay()).atTime(ZERO_TIME);
			} else if (tc == LocalTime.class) {
				return (T)ZERO_TIME;
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId);
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			}
		} else if (oc == JapaneseDate.class) {
			JapaneseDate t = (JapaneseDate)o;
			if (tc == Instant.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay());
			} else if (tc == LocalDateTime.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay()).atTime(ZERO_TIME);
			} else if (tc == LocalTime.class) {
				return (T)ZERO_TIME;
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			}
		} else if (oc == MinguoDate.class) {
			MinguoDate t = (MinguoDate)o;
			if (tc == Instant.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay());
			} else if (tc == LocalDateTime.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay()).atTime(ZERO_TIME);
			} else if (tc == LocalTime.class) {
				return (T)ZERO_TIME;
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ThaiBuddhistDate.class) {
				return (T)ThaiBuddhistDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			}
		} else if (oc == ThaiBuddhistDate.class) {
			ThaiBuddhistDate t = (ThaiBuddhistDate)o;
			if (tc == Instant.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant();
			} else if (tc == LocalDate.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay());
			} else if (tc == LocalDateTime.class) {
				return (T)LocalDate.ofEpochDay(t.toEpochDay()).atTime(ZERO_TIME);
			} else if (tc == LocalTime.class) {
				return (T)ZERO_TIME;
			} else if (tc == OffsetDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime();
			} else if (tc == OffsetTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId).toOffsetDateTime().toOffsetTime();
			} else if (tc == Year.class) {
				return (T)Year.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == YearMonth.class) {
				return (T)YearMonth.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == ZonedDateTime.class) {
				return (T)t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId);
			} else if (tc == HijrahDate.class) {
				return (T)HijrahDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == JapaneseDate.class) {
				return (T)JapaneseDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			} else if (tc == MinguoDate.class) {
				return (T)MinguoDate.from(t.atTime(ZERO_TIME).atZone(zoneId).toInstant().atZone(zoneId));
			}
		}

		// Last chance...try to use a static from(TemporalAccessor) method if present.
		ClassInfo ci = ClassInfo.of(tc);
		MethodInfo mi = ci.getStaticPublicMethod("from", tc, TemporalAccessor.class);
		if (mi != null) {
			try {
				return (T)mi.inner().invoke(null, o);
			} catch (Exception e) {}
		}

		throw new RuntimeException("Temporal type '"+o.getClass().getName()+"' cannot be converted to type '"+tc.getName()+"'.");
	}
}