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
package org.apache.juneau.utils;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.swaps.*;

/**
 * Centralized ISO 8601 formatting and parsing utility for date/time and Duration types.
 *
 * <p>
 * Provides the built-in serialization format for {@link java.time.temporal.Temporal}, {@link Calendar},
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
	 * @param value The value to format.
	 * @param type The class metadata for the value (used for selecting the appropriate formatter for Temporal types).
	 * @param timeZone The session time zone (used when the value lacks zone info).
	 * @return The ISO 8601 string representation.
	 */
	@SuppressWarnings({
		"java:S1172" // type kept for API compatibility; callers pass ClassMeta context for potential future use
	})
	public static String format(Object value, ClassMeta<?> type, TimeZone timeZone) {
		if (value instanceof Duration d)
			return d.toString();
		if (value instanceof Calendar c)
			return formatCalendar(c);
		if (value instanceof Date d)
			return formatDate(d, timeZone);
		if (value instanceof XMLGregorianCalendar x)
			return x.toXMLFormat();
		if (value instanceof Temporal t)
			return formatTemporal(t, timeZone);
		return value.toString();
	}

	/**
	 * Formats a Calendar to ISO 8601 using ISO_OFFSET_DATE_TIME.
	 */
	private static String formatCalendar(Calendar c) {
		ZonedDateTime zdt;
		if (c instanceof GregorianCalendar gc)
			zdt = gc.toZonedDateTime();
		else
			zdt = c.toInstant().atZone(c.getTimeZone().toZoneId());
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zdt);
	}

	/**
	 * Formats a Date to ISO 8601 using ISO_LOCAL_DATE_TIME.
	 */
	private static String formatDate(Date d, TimeZone tz) {
		ZoneId zoneId = tz != null ? tz.toZoneId() : ZoneId.systemDefault();
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(d.toInstant().atZone(zoneId));
	}

	/**
	 * Formats a Temporal to ISO 8601 using the appropriate default formatter for the concrete type.
	 */
	private static String formatTemporal(Temporal t, TimeZone tz) {
		ZoneId zoneId = tz != null ? tz.toZoneId() : ZoneId.systemDefault();
		Class<?> tc = t.getClass();

		if (tc == Instant.class)
			return DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.from(new DefaultingTemporalAccessor(t, Z)));

		if (tc == ZonedDateTime.class || tc == OffsetDateTime.class || tc == OffsetTime.class)
			return DEFAULT_FORMATTERS.getOrDefault(tc, DateTimeFormatter.ISO_OFFSET_DATE_TIME).format(t);

		if (tc == LocalDate.class || tc == LocalDateTime.class || tc == LocalTime.class
				|| tc == Year.class || tc == YearMonth.class)
			return DEFAULT_FORMATTERS.getOrDefault(tc, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(t);

		return DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.from(new DefaultingTemporalAccessor(t, zoneId)));
	}

	/**
	 * Formats a date/time value as an ISO date (date-only, for OpenAPI 'date' format).
	 *
	 * @param value The value to format.
	 * @param type The class metadata.
	 * @param timeZone The session time zone.
	 * @return The ISO date string.
	 */
	@SuppressWarnings({
		"java:S1172" // type kept for API compatibility; callers pass ClassMeta context for potential future use
	})
	public static String formatAsDate(Object value, ClassMeta<?> type, TimeZone timeZone) {
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		if (value instanceof Calendar c) {
			ZonedDateTime zdt = (c instanceof GregorianCalendar gc) ? gc.toZonedDateTime() : c.toInstant().atZone(c.getTimeZone().toZoneId());
			return DateTimeFormatter.ISO_DATE.format(zdt);
		}
		if (value instanceof Date d)
			return DateTimeFormatter.ISO_DATE.format(d.toInstant().atZone(zoneId));
		if (value instanceof Temporal t)
			return DateTimeFormatter.ISO_DATE.format(ZonedDateTime.from(new DefaultingTemporalAccessor(t, zoneId)));
		return value.toString();
	}

	/**
	 * Formats a date/time value as an ISO date-time (for OpenAPI 'date-time' format).
	 *
	 * @param value The value to format.
	 * @param type The class metadata.
	 * @param timeZone The session time zone.
	 * @return The ISO date-time string.
	 */
	@SuppressWarnings({
		"java:S1172" // type kept for API compatibility; callers pass ClassMeta context for potential future use
	})
	public static String formatAsDateTime(Object value, ClassMeta<?> type, TimeZone timeZone) {
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
		if (value instanceof Calendar c) {
			ZonedDateTime zdt = (c instanceof GregorianCalendar gc) ? gc.toZonedDateTime() : c.toInstant().atZone(c.getTimeZone().toZoneId());
			return DateTimeFormatter.ISO_INSTANT.format(zdt.toInstant());
		}
		if (value instanceof Date d)
			return DateTimeFormatter.ISO_INSTANT.format(d.toInstant());
		if (value instanceof Temporal t) {
			ZonedDateTime zdt = ZonedDateTime.from(new DefaultingTemporalAccessor(t, zoneId));
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
	 * @param <T> The target type.
	 * @param iso8601 The ISO 8601 string to parse.
	 * @param targetType The target class metadata.
	 * @param timeZone The session time zone (used for types that need a default zone).
	 * @return The parsed object.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for date/time type conversion
	})
	public static <T> T parse(String iso8601, ClassMeta<T> targetType, TimeZone timeZone) {
		if (iso8601 == null)
			return null;

		Class<T> tc = targetType.inner();
		ZoneId zoneId = timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();

		if (tc == Duration.class)
			return (T) parseDuration(iso8601);

		if (Calendar.class.isAssignableFrom(tc))
			return (T) parseCalendar(iso8601, zoneId);

		if (tc == Date.class)
			return (T) parseDate(iso8601, zoneId);

		if (XMLGregorianCalendar.class.isAssignableFrom(tc))
			return (T) datatypeFactory.newXMLGregorianCalendar(iso8601);

		if (Temporal.class.isAssignableFrom(tc))
			return (T) parseTemporal(iso8601, (Class<? extends Temporal>) tc, zoneId);

		return null;
	}

	/** Normalizes PT-Nx to -PTNx and parses ISO-8601 duration, with fallback for edge cases. */
	private static Duration parseDuration(String iso8601) {
		if (iso8601 == null || iso8601.isEmpty())
			return null;
		String s = iso8601.trim();
		// Strip optional surrounding quotes
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
			s = s.substring(1, s.length() - 1).trim();
		if (s.startsWith("PT-"))
			s = "-PT" + s.substring(3);
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

	private static Duration parseDurationManual(String s) {
		if (s == null || s.length() < 3)
			return null;
		boolean neg = s.startsWith("-");
		if (neg)
			s = s.substring(1);
		if (!s.startsWith("PT") && !s.startsWith("pt"))
			return null;
		s = s.substring(2);
		long totalNanos = 0;
		boolean found = false;
		var p = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([HhMmSs])");
		var m = p.matcher(s);
		while (m.find()) {
			found = true;
			double v = Double.parseDouble(m.group(1));
			long nanos = (long)(v * 1_000_000_000);
			char unit = Character.toUpperCase(m.group(2).charAt(0));
			switch (unit) {
				case 'H': totalNanos += nanos * 3600; break;
				case 'M': totalNanos += nanos * 60; break;
				case 'S': totalNanos += nanos; break;
				default: break;
			}
		}
		if (!found)
			return null;
		Duration d = Duration.ofNanos(totalNanos);
		return neg ? d.negated() : d;
	}

	private static Calendar parseCalendar(String iso8601, ZoneId zoneId) {
		var formatter = selectParserFormatter(iso8601);
		var ta = new DefaultingTemporalAccessor(formatter.parse(iso8601), zoneId);
		return GregorianCalendar.from(ZonedDateTime.from(ta));
	}

	private static Date parseDate(String iso8601, ZoneId zoneId) {
		var formatter = selectParserFormatter(iso8601);
		var ta = new DefaultingTemporalAccessor(formatter.parse(iso8601), zoneId);
		return Date.from(ZonedDateTime.from(ta).toInstant());
	}

	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for Temporal.from() reflection
	})
	private static <T extends Temporal> T parseTemporal(String iso8601, Class<T> tc, ZoneId zoneId) {
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

		throw new IllegalArgumentException("Cannot parse ISO 8601 string into type: " + tc.getName());
	}

	private static DateTimeFormatter getFormatterForType(Class<?> tc) {
		var f = DEFAULT_FORMATTERS.get(tc);
		return f != null ? f : DateTimeFormatter.ISO_INSTANT;
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
	 * @return The converted date/time object.
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
