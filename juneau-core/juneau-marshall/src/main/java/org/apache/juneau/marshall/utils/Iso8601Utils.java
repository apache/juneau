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
package org.apache.juneau.marshall.utils;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.time.*;
import java.time.Duration;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swaps.*;

/**
 * Centralized ISO 8601 formatting and parsing utility for date/time and Duration types.
 *
 * <p>
 * Provides the built-in serialization format for {@link Temporal}, {@link Calendar},
 * {@link Date}, {@link XMLGregorianCalendar}, and {@link Duration} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
 * </ul>
 */
public final class Iso8601Utils {

	private Iso8601Utils() {}

	private static final ZoneId Z = ZoneId.of("Z");

	private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("uuuu");
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM");

	private static final Map<Class<?>, DateTimeFormatter> DEFAULT_FORMATTERS = Map.ofEntries(
		Map.entry(Instant.class, DateTimeFormatter.ISO_INSTANT),
		Map.entry(ZonedDateTime.class, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
		Map.entry(OffsetDateTime.class, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
		Map.entry(LocalDate.class, DateTimeFormatter.ISO_LOCAL_DATE),
		Map.entry(LocalDateTime.class, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
		Map.entry(LocalTime.class, DateTimeFormatter.ISO_LOCAL_TIME),
		Map.entry(OffsetTime.class, DateTimeFormatter.ISO_OFFSET_TIME),
		Map.entry(Year.class, YEAR_FORMATTER),
		Map.entry(YearMonth.class, YEAR_MONTH_FORMATTER)
	);

	private static DatatypeFactory datatypeFactory;
	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw toRex(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Formatting
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Formats a date/time or Duration value to its ISO 8601 string representation.
	 *
	 * <p>
	 * Convenience dispatcher that uses default per-type wire formats; callers that need format-aware
	 * output should use the per-type helpers directly:
	 * {@link #formatDate}, {@link #formatCalendar}, {@link #formatTemporal},
	 * {@link #formatDuration}, {@link #formatPeriod}.
	 *
	 * @param value The value to format.  <jk>null</jk> returns <jk>null</jk> (consistent with the per-type helpers).
	 * @param type The class metadata for the value (used for selecting the appropriate formatter for Temporal types).
	 * @param timeZone The session time zone (used when the value lacks zone info).
	 * @return The ISO 8601 string representation, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // type kept for API compatibility; future callers may use it for per-type dispatch hints
	})
	public static String format(Object value, ClassMeta<?> type, TimeZone timeZone) {
		if (value == null)
			return null;
		if (value instanceof Duration value2)
			return formatDuration(value2, DurationFormat.ISO_8601_WITH_DAYS);
		if (value instanceof Period value2)
			return formatPeriod(value2, PeriodFormat.ISO_8601);
		if (value instanceof Calendar || value instanceof XMLGregorianCalendar)
			return formatCalendar(value, type, CalendarFormat.ISO_OFFSET_DATE_TIME, timeZone);
		if (value instanceof Date value2)
			return formatDate(value2, type, DateFormat.ISO_LOCAL_DATE_TIME, timeZone);
		if (value instanceof TemporalAccessor value2)
			return formatTemporal(value2, type, TemporalFormat.DEFAULT, timeZone);
		return value.toString();
	}

	/**
	 * Formats a {@link Duration} value using the supplied wire format.
	 *
	 * @param value The value to format. <jk>null</jk> returns <jk>null</jk>.
	 * @param format The duration wire format. May be <jk>null</jk> (defaults to {@link DurationFormat#ISO_8601_WITH_DAYS}).
	 * @return The formatted value, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static String formatDuration(Duration value, DurationFormat format) {
		if (value == null)
			return null;
		return (format == null ? DurationFormat.ISO_8601_WITH_DAYS : format).format(value);
	}

	/**
	 * Formats a {@link Period} value using the supplied wire format.
	 *
	 * @param value The value to format. <jk>null</jk> returns <jk>null</jk>.
	 * @param format The period wire format. May be <jk>null</jk> (defaults to {@link PeriodFormat#ISO_8601}).
	 * @return The formatted value, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static String formatPeriod(Period value, PeriodFormat format) {
		if (value == null)
			return null;
		return (format == null ? PeriodFormat.ISO_8601 : format).format(value);
	}

	/**
	 * Formats a {@link Date} value using the supplied wire format.
	 *
	 * <p>
	 * {@code java.sql.Date}, {@code java.sql.Time}, and {@code java.sql.Timestamp} subclasses all dispatch
	 * through {@link DateFormat#format(Date, ZoneId)} just like {@code Date}.
	 *
	 * @param value The value to format. <jk>null</jk> returns <jk>null</jk>.
	 * @param sourceType The class metadata for the value (currently unused, kept for symmetry with the parse helper).
	 * @param format The date wire format. May be <jk>null</jk> (defaults to {@link DateFormat#ISO_LOCAL_DATE_TIME}).
	 * @param timeZone The session time zone (used as the default zone).
	 * @return The formatted value, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // sourceType kept for API symmetry with parseDate; callers pass ClassMeta context for potential future use
	})
	public static String formatDate(Date value, ClassMeta<?> sourceType, DateFormat format, TimeZone timeZone) {
		if (value == null)
			return null;
		ZoneId zone = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		return (format == null ? DateFormat.ISO_LOCAL_DATE_TIME : format).format(value, zone);
	}

	/**
	 * Formats a {@link Calendar} or {@link XMLGregorianCalendar} value using the supplied wire format.
	 *
	 * <p>
	 * {@link XMLGregorianCalendar} values always emit {@link XMLGregorianCalendar#toXMLFormat()} regardless
	 * of the configured {@code format} — mirroring the parse-side rule and the historical wire output.
	 * {@link Calendar} (including {@link GregorianCalendar}) values are formatted using the supplied
	 * {@link CalendarFormat}.
	 *
	 * @param value The value to format. Must be a {@link Calendar} or {@link XMLGregorianCalendar}.
	 * 	<jk>null</jk> returns <jk>null</jk>.
	 * @param sourceType The class metadata for the value (currently unused, kept for symmetry with the parse helper).
	 * @param format The calendar wire format. May be <jk>null</jk> (defaults to {@link CalendarFormat#ISO_OFFSET_DATE_TIME}).
	 * @param timeZone The session time zone (used as the default zone when the value lacks zone info).
	 * @return The formatted value, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // sourceType kept for API symmetry with parseCalendar; callers pass ClassMeta context for potential future use
	})
	public static String formatCalendar(Object value, ClassMeta<?> sourceType, CalendarFormat format, TimeZone timeZone) {
		if (value == null)
			return null;
		if (value instanceof XMLGregorianCalendar value2)
			return value2.toXMLFormat();
		ZoneId zone = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		return (format == null ? CalendarFormat.ISO_OFFSET_DATE_TIME : format).format((Calendar)value, zone);
	}

	/**
	 * Formats a {@link TemporalAccessor} value using the supplied wire format.
	 *
	 * @param value The value to format. Must be a {@link Temporal} subtype. <jk>null</jk> returns <jk>null</jk>.
	 * @param sourceType The class metadata for the value (currently unused, kept for symmetry with the parse helper).
	 * @param format The temporal wire format. May be <jk>null</jk> (defaults to {@link TemporalFormat#DEFAULT}).
	 * @param timeZone The session time zone (used as the default zone when the value lacks zone info).
	 * @return The formatted value, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // sourceType kept for API symmetry with parseTemporal; callers pass ClassMeta context for potential future use
	})
	public static String formatTemporal(TemporalAccessor value, ClassMeta<?> sourceType, TemporalFormat format, TimeZone timeZone) {
		if (value == null)
			return null;
		ZoneId zone = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		return (format == null ? TemporalFormat.DEFAULT : format).format(value, zone);
	}

	/**
	 * Formats a date/time value as an ISO date (date-only, for OpenAPI 'date' format).
	 *
	 * @param value The value to format.  <jk>null</jk> returns <jk>null</jk>.
	 * @param type The class metadata.
	 * @param timeZone The session time zone.
	 * @return The ISO date string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // type kept for API compatibility; callers pass ClassMeta context for potential future use
	})
	public static String formatAsDate(Object value, ClassMeta<?> type, TimeZone timeZone) {
		if (value == null)
			return null;
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		if (value instanceof Calendar value2) {
			ZonedDateTime zdt = (value2 instanceof GregorianCalendar value3) ? value3.toZonedDateTime() : value2.toInstant().atZone(value2.getTimeZone().toZoneId());
			return DateTimeFormatter.ISO_DATE.format(zdt);
		}
		if (value instanceof Date value2)
			return DateTimeFormatter.ISO_DATE.format(value2.toInstant().atZone(zoneId));
		if (value instanceof Temporal value2)
			return DateTimeFormatter.ISO_DATE.format(ZonedDateTime.from(new DefaultingTemporalAccessor(value2, zoneId)));
		return value.toString();
	}

	/**
	 * Formats a date/time value as an ISO date-time (for OpenAPI 'date-time' format).
	 *
	 * @param value The value to format.  <jk>null</jk> returns <jk>null</jk>.
	 * @param type The class metadata.
	 * @param timeZone The session time zone.
	 * @return The ISO date-time string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1172" // type kept for API compatibility; callers pass ClassMeta context for potential future use
	})
	public static String formatAsDateTime(Object value, ClassMeta<?> type, TimeZone timeZone) {
		if (value == null)
			return null;
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		if (value instanceof Calendar value2) {
			ZonedDateTime zdt = (value2 instanceof GregorianCalendar value3) ? value3.toZonedDateTime() : value2.toInstant().atZone(value2.getTimeZone().toZoneId());
			return DateTimeFormatter.ISO_INSTANT.format(zdt.toInstant());
		}
		if (value instanceof Date value2)
			return DateTimeFormatter.ISO_INSTANT.format(value2.toInstant());
		if (value instanceof Temporal value2) {
			ZonedDateTime zdt = ZonedDateTime.from(new DefaultingTemporalAccessor(value2, zoneId));
			return DateTimeFormatter.ISO_INSTANT.format(zdt.toInstant());
		}
		return value.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parsing
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Parses an ISO 8601 string into the specified date/time target type.
	 *
	 * <p>
	 * Dispatches to the appropriate per-type parser based on the target's {@link ClassMeta} category:
	 * {@link #parseDate parseDate}, {@link #parseCalendar parseCalendar}, {@link #parseTemporal parseTemporal},
	 * {@link #parseDuration parseDuration}, or {@link #parsePeriod parsePeriod} — each using its default
	 * wire format.
	 *
	 * @param <T> The target type.
	 * @param iso8601 The ISO 8601 string to parse. <jk>null</jk> returns <jk>null</jk>.
	 * @param targetType The target class metadata.
	 * @param timeZone The session time zone (used for types that need a default zone).
	 * @return The parsed object, or <jk>null</jk> if {@code iso8601} is <jk>null</jk> or the target type is
	 * 	not a recognized date/time/duration/period type.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for date/time type dispatch
	})
	public static <T> T parse(String iso8601, ClassMeta<T> targetType, TimeZone timeZone) {
		if (iso8601 == null)
			return null;
		if (targetType.isDate())
			return parseDate(iso8601, targetType, DateFormat.ISO_LOCAL_DATE_TIME, timeZone);
		if (targetType.isCalendar())
			return parseCalendar(iso8601, targetType, CalendarFormat.ISO_OFFSET_DATE_TIME, timeZone);
		if (targetType.isTemporal())
			return (T) parseTemporal(iso8601, (ClassMeta<? extends Temporal>)(ClassMeta<?>)targetType, TemporalFormat.DEFAULT, timeZone);
		if (targetType.isDuration())
			return (T) parseDuration(iso8601, DurationFormat.MILLIS);
		if (targetType.isPeriod())
			return (T) parsePeriod(iso8601, PeriodFormat.ISO_8601);
		return null;
	}

	/**
	 * Parses an ISO 8601 / numeric wire value into a {@link Duration}.
	 *
	 * <p>
	 * Handles ISO 8601 forms ({@code "PT1H"}, {@code "-PT30M"}, {@code "PT-6H"}, {@code "PT1.5S"}, …) as well
	 * as the bare-number wire forms produced by {@link DurationFormat#MILLIS}, {@link DurationFormat#SECONDS},
	 * {@link DurationFormat#NANOS}, and {@link DurationFormat#HOCON} (e.g. {@code "500ms"}, {@code "1.5s"}).
	 *
	 * <p>
	 * The {@code formatHint} disambiguates bare integer strings that could be milliseconds, nanos, or seconds —
	 * for all other numeric or ISO forms the input is self-describing and the hint is ignored.
	 *
	 * @param iso8601 The wire value to parse. <jk>null</jk> or empty returns <jk>null</jk>.
	 * @param formatHint The duration wire format used to disambiguate bare integer inputs. May be <jk>null</jk>
	 * 	(defaults to {@link DurationFormat#MILLIS}).
	 * @return The parsed {@link Duration}, or <jk>null</jk> if {@code iso8601} is <jk>null</jk> or empty.
	 */
	public static Duration parseDuration(String iso8601, DurationFormat formatHint) {
		if (iso8601 == null || iso8601.isEmpty())
			return null;
		String s = iso8601.trim();
		// Strip optional surrounding quotes
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
			s = s.substring(1, s.length() - 1).trim();
		if (s.startsWith("PT-"))
			s = "-PT" + s.substring(3);
		// Number sniffing and explicit format hints.
		if (isIntegerLiteral(s))
			return (formatHint == null ? DurationFormat.MILLIS : formatHint).parse(s);
		if (isDecimalLiteral(s))
			return DurationFormat.SECONDS.parse(s);
		if (isHoconLiteral(s))
			return DurationFormat.HOCON.parse(s);
		// Use manual parser for full control (handles PTnH, PTnM, PTn.nS, -PTnH, PT-6H, etc.)
		Duration d = parseDurationManual(s);
		if (d != null)
			return d;
		// Fallback to standard parser
		try {
			return Duration.parse(s);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * Parses an ISO 8601 / numeric wire value into a {@link Period}.
	 *
	 * <p>
	 * Handles standard ISO 8601 period strings ({@code "P1Y2M3D"}, …) as well as the bare-integer wire form
	 * produced by {@link PeriodFormat#DAYS}.
	 *
	 * @param value The wire value to parse. <jk>null</jk> or empty returns <jk>null</jk>.
	 * @param formatHint The period wire format used to interpret bare integers. May be <jk>null</jk>
	 * 	(defaults to {@link PeriodFormat#DAYS}).
	 * @return The parsed {@link Period}, or <jk>null</jk> if {@code value} is <jk>null</jk> or empty.
	 */
	public static Period parsePeriod(String value, PeriodFormat formatHint) {
		if (value == null || value.isEmpty())
			return null;
		String s = value.trim();
		if (isIntegerLiteral(s))
			return (formatHint == null ? PeriodFormat.DAYS : formatHint).parse(s);
		return Period.parse(s);
	}

	/**
	 * Parses an ISO 8601 wire value into a {@link Calendar} or {@link XMLGregorianCalendar}.
	 *
	 * <p>
	 * Dispatches by target type:
	 * <ul>
	 * 	<li>{@link XMLGregorianCalendar} targets always use {@link DatatypeFactory#newXMLGregorianCalendar(String)}
	 * 		and ignore {@code formatHint}.
	 * 	<li>{@link Calendar} / {@link GregorianCalendar} targets honor {@code formatHint} when set to anything
	 * 		other than {@link CalendarFormat#NOT_SET} or {@link CalendarFormat#ISO_OFFSET_DATE_TIME}; the default
	 * 		path uses content-based formatter autodetection.
	 * </ul>
	 *
	 * @param <T> The target type.
	 * @param iso8601 The wire value to parse. <jk>null</jk> returns <jk>null</jk>.
	 * @param targetType The target class metadata. Must represent a {@link Calendar} or {@link XMLGregorianCalendar}.
	 * @param formatHint The calendar wire format. May be <jk>null</jk> (treated as {@link CalendarFormat#NOT_SET}).
	 * @param timeZone The session time zone (used as the default zone when the wire value lacks zone info).
	 * @return The parsed value, or <jk>null</jk> if {@code iso8601} is <jk>null</jk> or the target is not a
	 * 	recognized calendar type.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for Calendar/XMLGregorianCalendar dispatch
	})
	public static <T> T parseCalendar(String iso8601, ClassMeta<T> targetType, CalendarFormat formatHint, TimeZone timeZone) {
		if (iso8601 == null)
			return null;
		Class<T> tc = targetType.inner();
		if (XMLGregorianCalendar.class.isAssignableFrom(tc))
			return (T) datatypeFactory.newXMLGregorianCalendar(iso8601);
		if (! Calendar.class.isAssignableFrom(tc))
			return null;
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		if (formatHint != null && formatHint != CalendarFormat.NOT_SET && formatHint != CalendarFormat.ISO_OFFSET_DATE_TIME)
			return (T) formatHint.parse(iso8601, zoneId);
		return (T) parseCalendarDefault(iso8601, zoneId);
	}

	/**
	 * Parses an ISO 8601 wire value into a {@link Date}.
	 *
	 * <p>
	 * Honors {@code formatHint} when set to anything other than {@link DateFormat#NOT_SET} or
	 * {@link DateFormat#ISO_LOCAL_DATE_TIME}; the default path uses content-based formatter autodetection.
	 *
	 * <p>
	 * Only direct {@link Date} targets are produced — {@code java.sql.*} subclasses fall through to
	 * <jk>null</jk>, matching the original dispatch.
	 *
	 * @param <T> The target type.
	 * @param iso8601 The wire value to parse. <jk>null</jk> returns <jk>null</jk>.
	 * @param targetType The target class metadata. Must represent {@link Date}.
	 * @param formatHint The date wire format. May be <jk>null</jk> (treated as {@link DateFormat#NOT_SET}).
	 * @param timeZone The session time zone (used as the default zone when the wire value lacks zone info).
	 * @return The parsed {@link Date}, or <jk>null</jk> if {@code iso8601} is <jk>null</jk> or the target is
	 * 	not exactly {@code Date}.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for Date dispatch
	})
	public static <T> T parseDate(String iso8601, ClassMeta<T> targetType, DateFormat formatHint, TimeZone timeZone) {
		if (iso8601 == null)
			return null;
		if (targetType.inner() != Date.class)
			return null;
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		if (formatHint != null && formatHint != DateFormat.NOT_SET && formatHint != DateFormat.ISO_LOCAL_DATE_TIME)
			return (T) formatHint.parse(iso8601, zoneId);
		return (T) parseDateDefault(iso8601, zoneId);
	}

	/**
	 * Parses an ISO 8601 wire value into the specified {@link Temporal} subtype.
	 *
	 * <p>
	 * Honors {@code formatHint} when set to anything other than {@link TemporalFormat#NOT_SET} or
	 * {@link TemporalFormat#DEFAULT}; the default path uses the per-subtype default formatter
	 * (e.g. {@link DateTimeFormatter#ISO_INSTANT} for {@link Instant}, {@link DateTimeFormatter#ISO_LOCAL_DATE}
	 * for {@link LocalDate}, …).
	 *
	 * @param <T> The target temporal subtype.
	 * @param iso8601 The wire value to parse. <jk>null</jk> returns <jk>null</jk>.
	 * @param targetType The target class metadata. Must represent a {@link Temporal} subtype.
	 * @param formatHint The temporal wire format. May be <jk>null</jk> (treated as {@link TemporalFormat#NOT_SET}).
	 * @param timeZone The session time zone (used as the default zone when the wire value lacks zone info).
	 * @return The parsed value, or <jk>null</jk> if {@code iso8601} is <jk>null</jk> or the target is not a
	 * 	{@link Temporal} subtype.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for per-subtype Temporal.from() dispatch
	})
	public static <T> T parseTemporal(String iso8601, ClassMeta<T> targetType, TemporalFormat formatHint, TimeZone timeZone) {
		if (iso8601 == null)
			return null;
		Class<T> tc = targetType.inner();
		if (! Temporal.class.isAssignableFrom(tc))
			return null;
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		Class<? extends Temporal> ttc = (Class<? extends Temporal>) tc;
		if (formatHint != null && formatHint != TemporalFormat.NOT_SET && formatHint != TemporalFormat.DEFAULT)
			return (T) formatHint.parse(iso8601, ttc, zoneId);
		return (T) parseTemporalDefault(iso8601, ttc, zoneId);
	}

	/**
	 * Hand-written state machine that parses the {@code PT}-prefixed body as a strict, gapless sequence of
	 * {@code number[whitespace]unit} components ({@code H}/{@code M}/{@code S}, case-insensitive).
	 *
	 * <p>
	 * Whitespace is permitted between a number and its unit and between/around components, but every
	 * non-whitespace character must belong to a valid component. The parser rejects (by returning <jk>null</jk>,
	 * the same signal used for any input it cannot handle) when it encounters stray characters, a malformed
	 * number (e.g. more than one decimal point, or a trailing dot with no fraction), or a number without a
	 * trailing unit. On rejection {@link #parseDuration} falls through to {@link Duration#parse(CharSequence)},
	 * which throws for genuinely invalid values. Numeric conversion uses the floating-point expression
	 * {@code (long)(value * 1_000_000_000)} to preserve historical rounding for well-formed inputs.
	 *
	 * @param s The candidate duration string (already trimmed/dequoted/PT-normalized by {@link #parseDuration}).
	 * @return The parsed {@link Duration}, or <jk>null</jk> if the string is too short, lacks a {@code PT}/{@code pt}
	 * 	prefix, contains no component, or contains any stray/malformed content.
	 */
	@SuppressWarnings({
		"java:S3776" // Hand-written ISO-8601 duration lexer/state-machine; decomposing the char-by-char scan would obscure the parse invariants without changing behavior.
	})
	private static Duration parseDurationManual(String s) {
		if (s == null || s.length() < 3)
			return null;
		boolean neg = s.startsWith("-");
		String s2 = neg ? s.substring(1) : s;
		if (!s2.startsWith("PT") && !s2.startsWith("pt"))
			return null;
		s2 = s2.substring(2);
		long totalNanos = 0;
		boolean found = false;
		int len = s2.length();
		int i = 0;
		while (i < len) {
			// Whitespace between/around components is skipped; any other non-component character is rejected.
			if (isRegexWhitespace(s2.charAt(i))) {
				i++;
				continue;
			}
			// A component must begin with a digit; any other character is stray junk → reject.
			if (!isAsciiDigit(s2.charAt(i)))
				return null;
			int start = i;
			while (i < len && isAsciiDigit(s2.charAt(i)))  // Integer part.
				i++;
			// Optional fraction: a single '.' that MUST be followed by at least one digit.
			if (i < len && s2.charAt(i) == '.') {
				i++;
				if (i >= len || !isAsciiDigit(s2.charAt(i)))
					return null;  // Trailing dot with no fraction → malformed.
				while (i < len && isAsciiDigit(s2.charAt(i)))
					i++;
			}
			int numEnd = i;
			while (i < len && isRegexWhitespace(s2.charAt(i)))  // Optional whitespace between number and unit.
				i++;
			// A unit designator is required; a number without one (or followed by stray chars) is malformed.
			if (i >= len || !isDurationUnit(s2.charAt(i)))
				return null;
			found = true;
			double v = Double.parseDouble(s2.substring(start, numEnd));
			long nanos = (long)(v * 1_000_000_000);
			char unit = Character.toUpperCase(s2.charAt(i));
			switch (unit) {
				case 'H': totalNanos += nanos * 3600; break;
				case 'M': totalNanos += nanos * 60; break;
				case 'S': totalNanos += nanos; break;
				default: break;  // HTT: isDurationUnit guarantees H/M/S
			}
			i++;  // Consume the unit.
		}
		if (!found)
			return null;
		Duration d = Duration.ofNanos(totalNanos);
		return neg ? d.negated() : d;
	}

	private static boolean isAsciiDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Matches the regex {@code \s} character class (without the {@code UNICODE_CHARACTER_CLASS} flag).
	 */
	private static boolean isRegexWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
	}

	private static boolean isDurationUnit(char c) {
		return c == 'H' || c == 'h' || c == 'M' || c == 'm' || c == 'S' || c == 's';
	}

	/**
	 * State-machine equivalent of {@code ^[+-]?\d++$} — an optional sign followed by one or more ASCII digits.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if the entire string is an optionally-signed integer literal.
	 */
	private static boolean isIntegerLiteral(String s) {
		int len = s.length();
		int i = 0;
		if (i < len && (s.charAt(i) == '+' || s.charAt(i) == '-'))
			i++;
		if (i == len)
			return false;
		for (int k = i; k < len; k++)
			if (!isAsciiDigit(s.charAt(k)))
				return false;
		return true;
	}

	/**
	 * State-machine equivalent of {@code ^[+-]?\d++\.\d++$} — an optionally-signed decimal with digits on both
	 * sides of a single dot.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if the entire string is an optionally-signed decimal literal.
	 */
	private static boolean isDecimalLiteral(String s) {
		int len = s.length();
		int i = 0;
		if (i < len && (s.charAt(i) == '+' || s.charAt(i) == '-'))
			i++;
		int intStart = i;
		while (i < len && isAsciiDigit(s.charAt(i)))
			i++;
		if (i == intStart)
			return false;
		if (i >= len || s.charAt(i) != '.')
			return false;
		i++;
		int fracStart = i;
		while (i < len && isAsciiDigit(s.charAt(i)))
			i++;
		if (i == fracStart)
			return false;
		return i == len;
	}

	/**
	 * State-machine equivalent of {@code ^[+-]?\d++(?:\.\d++)?(?:ns|us|ms|s|m|h|d)$} — an optionally-signed number
	 * (with optional fraction) followed by a HOCON time unit.
	 *
	 * @param s The string to test.
	 * @return <jk>true</jk> if the entire string is an optionally-signed HOCON duration literal.
	 */
	private static boolean isHoconLiteral(String s) {
		int len = s.length();
		int i = 0;
		if (i < len && (s.charAt(i) == '+' || s.charAt(i) == '-'))
			i++;
		int intStart = i;
		while (i < len && isAsciiDigit(s.charAt(i)))
			i++;
		if (i == intStart)
			return false;
		// Optional fraction consumed only when '.' is followed by at least one digit; otherwise left for the unit.
		if (i < len && s.charAt(i) == '.' && i + 1 < len && isAsciiDigit(s.charAt(i + 1))) {
			i++;
			while (i < len && isAsciiDigit(s.charAt(i)))
				i++;
		}
		String unit = s.substring(i);
		return unit.equals("ns") || unit.equals("us") || unit.equals("ms")
			|| unit.equals("s") || unit.equals("m") || unit.equals("h") || unit.equals("d");
	}

	private static Calendar parseCalendarDefault(String iso8601, ZoneId zoneId) {
		var formatter = selectParserFormatter(iso8601);
		var ta = new DefaultingTemporalAccessor(formatter.parse(iso8601), zoneId);
		return GregorianCalendar.from(ZonedDateTime.from(ta));
	}

	private static Date parseDateDefault(String iso8601, ZoneId zoneId) {
		var formatter = selectParserFormatter(iso8601);
		var ta = new DefaultingTemporalAccessor(formatter.parse(iso8601), zoneId);
		return Date.from(ZonedDateTime.from(ta).toInstant());
	}

	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for Temporal.from() reflection
	})
	private static <T extends Temporal> T parseTemporalDefault(String iso8601, Class<T> tc, ZoneId zoneId) {
		ZoneId offset = (tc == Instant.class) ? Z : zoneId;
		var formatter = getFormatterForType(tc);
		var ta = new DefaultingTemporalAccessor(formatter.parse(iso8601), offset);

		try {
			var parseMethod = info(tc).getPublicMethod(
				x -> x.isStatic()
					&& x.isNotDeprecated()
					&& x.hasName("from")
					&& x.hasReturnType(tc)
					&& x.hasParameterTypes(TemporalAccessor.class)
			).map(MethodInfo::inner).orElse(null);

			if (parseMethod != null)
				return (T) parseMethod.invoke(null, ta);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw toRex(e);
		}

		throw iaex("Cannot parse ISO 8601 string into type: %s", tc.getName());
	}

	private static DateTimeFormatter getFormatterForType(Class<?> tc) {
		var f = DEFAULT_FORMATTERS.get(tc);
		return or(f, DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Auto-detects the appropriate parser formatter based on the string content.
	 */
	private static DateTimeFormatter selectParserFormatter(String iso8601) {
		boolean hasTime = iso8601.contains("T");
		boolean hasZone = iso8601.endsWith("Z") || iso8601.contains("+")
			|| (iso8601.length() > 10 && iso8601.lastIndexOf('-') > 10);
		if (hasTime && hasZone)
			return DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		if (hasTime)
			return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		if (hasZone)
			return DateTimeFormatter.ISO_DATE;
		return DateTimeFormatter.ISO_LOCAL_DATE;
	}

	/**
	 * Converts epoch milliseconds to the target date/time type.
	 *
	 * @param <T> The target type.
	 * @param epochMillis The epoch milliseconds value.
	 * @param targetType The target class metadata.
	 * @param timeZone The session time zone.
	 * @return The converted date/time object, or <jk>null</jk> if the target type is not a recognized date/time type.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for date/time type conversion
	})
	public static <T> T fromEpochMillis(long epochMillis, ClassMeta<T> targetType, TimeZone timeZone) {
		Class<T> tc = targetType.inner();
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		Instant instant = Instant.ofEpochMilli(epochMillis);

		if (tc == Instant.class) return (T) instant;
		if (tc == ZonedDateTime.class) return (T) instant.atZone(zoneId);
		if (tc == OffsetDateTime.class) return (T) instant.atZone(zoneId).toOffsetDateTime();
		if (tc == LocalDateTime.class) return (T) instant.atZone(zoneId).toLocalDateTime();
		if (tc == LocalDate.class) return (T) instant.atZone(zoneId).toLocalDate();
		if (tc == LocalTime.class) return (T) instant.atZone(zoneId).toLocalTime();
		if (tc == OffsetTime.class) return (T) instant.atZone(zoneId).toOffsetDateTime().toOffsetTime();
		if (tc == Date.class) return (T) Date.from(instant);
		if (Calendar.class.isAssignableFrom(tc)) {
			var cal = GregorianCalendar.from(instant.atZone(zoneId));
			return (T) cal;
		}
		if (XMLGregorianCalendar.class.isAssignableFrom(tc)) {
			var gc = GregorianCalendar.from(instant.atZone(zoneId));
			return (T) datatypeFactory.newXMLGregorianCalendar(gc);
		}

		return null;
	}
}
