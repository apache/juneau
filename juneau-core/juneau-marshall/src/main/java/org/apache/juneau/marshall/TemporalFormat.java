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
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.time.*;
import java.time.chrono.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.swaps.*;

/**
 * Supported wire formats for {@link TemporalAccessor} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#temporalFormat(TemporalFormat)},
 * {@link Marshalled#temporalFormat()},
 * {@link MarshalledProp#temporalFormat()}, and
 * {@link MarshalledConfig#temporalFormat()} to control how
 * {@link TemporalAccessor} values ({@link Instant}, {@link LocalDate}, {@link LocalDateTime},
 * {@link LocalTime}, {@link OffsetDateTime}, {@link OffsetTime}, {@link ZonedDateTime}, {@link Year},
 * {@link YearMonth}, {@link MonthDay}, …) are written to and read from the wire.
 *
 * <p>
 * The default is {@link #DEFAULT} which uses the per-subtype default formatter
 * (e.g. {@link DateTimeFormatter#ISO_INSTANT} for {@link Instant}, {@link DateTimeFormatter#ISO_LOCAL_DATE}
 * for {@link LocalDate}, etc.) — preserving the historical wire output produced before this enum existed.
 *
 * <p>
 * Numeric formats ({@link #MILLIS}) emit native numeric wire types on binary serializers
 * (BSON / CBOR / MsgPack) and bare numbers on text formats. Text formats emit a quoted ISO 8601 string for
 * every other constant.
 *
 * <p>
 * Most subtypes implement {@link Temporal}, but {@link MonthDay} only implements {@link TemporalAccessor}.
 * Format/parse methods accept any {@link TemporalAccessor} so that {@link MonthDay} round-trips through the
 * same dispatch path as the {@link Temporal} subtypes.
 */
public enum TemporalFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Per-subtype default formatter (the historical wire output).
	 *
	 * <p>
	 * Uses the ISO formatter most appropriate for each {@link Temporal} subtype:
	 * {@link DateTimeFormatter#ISO_INSTANT} for {@link Instant}, {@link DateTimeFormatter#ISO_LOCAL_DATE}
	 * for {@link LocalDate}, {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} for {@link ZonedDateTime} /
	 * {@link OffsetDateTime}, etc.
	 */
	DEFAULT,

	/** {@link DateTimeFormatter#BASIC_ISO_DATE}: <js>"20111203"</js>. */
	BASIC_ISO_DATE,

	/** {@link DateTimeFormatter#ISO_DATE}: <js>"2011-12-03+01:00"</js> or <js>"2011-12-03"</js>. */
	ISO_DATE,

	/** {@link DateTimeFormatter#ISO_DATE_TIME}: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>. */
	ISO_DATE_TIME,

	/** {@link DateTimeFormatter#ISO_INSTANT}: <js>"2011-12-03T10:15:30Z"</js>. */
	ISO_INSTANT,

	/** {@link DateTimeFormatter#ISO_LOCAL_DATE}: <js>"2011-12-03"</js>. */
	ISO_LOCAL_DATE,

	/** {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}: <js>"2011-12-03T10:15:30"</js>. */
	ISO_LOCAL_DATE_TIME,

	/** {@link DateTimeFormatter#ISO_LOCAL_TIME}: <js>"10:15:30"</js>. */
	ISO_LOCAL_TIME,

	/** {@link DateTimeFormatter#ISO_OFFSET_DATE}: <js>"2011-12-03+01:00"</js>. */
	ISO_OFFSET_DATE,

	/** {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}: <js>"2011-12-03T10:15:30+01:00"</js>. */
	ISO_OFFSET_DATE_TIME,

	/** {@link DateTimeFormatter#ISO_OFFSET_TIME}: <js>"10:15:30+01:00"</js>. */
	ISO_OFFSET_TIME,

	/** {@link DateTimeFormatter#ISO_ORDINAL_DATE}: <js>"2012-337"</js>. */
	ISO_ORDINAL_DATE,

	/** {@link DateTimeFormatter#ISO_TIME}: <js>"10:15:30+01:00"</js> or <js>"10:15:30"</js>. */
	ISO_TIME,

	/** {@link DateTimeFormatter#ISO_WEEK_DATE}: <js>"2012-W48-6"</js>. */
	ISO_WEEK_DATE,

	/** Year only — pattern {@code "uuuu"}: <js>"2012"</js>. */
	ISO_YEAR,

	/** Year and month — pattern {@code "uuuu-MM"}: <js>"2012-12"</js>. */
	ISO_YEAR_MONTH,

	/** {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}: <js>"2011-12-03T10:15:30+01:00[Europe/Paris]"</js>. */
	ISO_ZONED_DATE_TIME,

	/** {@link DateTimeFormatter#RFC_1123_DATE_TIME}: <js>"Tue, 3 Jun 2008 11:05:30 GMT"</js>. */
	RFC_1123_DATE_TIME,

	/**
	 * Epoch milliseconds as a numeric value.
	 *
	 * <p>
	 * Emitted as a native int64 by binary serializers (BSON / CBOR / MsgPack) and as a bare integer
	 * literal by text serializers.
	 *
	 * <p>
	 * <b>Per-subtype semantics (midnight UTC for non-instant subtypes):</b>
	 * <ul>
	 *   <li>{@link Instant} — {@code instant.toEpochMilli()}.
	 *   <li>{@link OffsetDateTime}, {@link ZonedDateTime} — {@code value.toInstant().toEpochMilli()}.
	 *   <li>{@link LocalDateTime} — {@code ldt.toInstant(ZoneOffset.UTC).toEpochMilli()}.
	 *   <li>{@link LocalDate} — {@code ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()} (midnight UTC).
	 *   <li>{@link YearMonth} — first day of the month at midnight UTC.
	 *   <li>{@link Year} — January 1 at midnight UTC.
	 *   <li>{@link LocalTime}, {@link OffsetTime}, {@link MonthDay} — fall back to the type's {@link #DEFAULT}
	 *       (ISO string form) because no defensible epoch-millis interpretation exists without an associated
	 *       date/year.  See {@link #isMillisNumeric(Class)} for the canonical decision predicate.
	 * </ul>
	 *
	 * <p>
	 * <b>The asymmetry is intentional:</b> a configured {@code MILLIS} setting silently produces a string
	 * for {@link LocalTime} / {@link OffsetTime} / {@link MonthDay} and a number for every other subtype.
	 */
	MILLIS;

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

	private static final Map<Class<? extends TemporalAccessor>, Method> FROM_METHODS = new ConcurrentHashMap<>();

	/**
	 * Returns <jk>true</jk> when {@link #MILLIS} produces a numeric wire string for the specified subtype.
	 *
	 * <p>
	 * Single source of truth for the {@code MILLIS} wire-type decision shared by {@link #format} and the
	 * marshalling-side {@code temporalSwap} / {@code temporalAccessorSwap} factories.  The helper exists so
	 * the swap-side {@code Long.valueOf(s)} coercion only fires when {@link #format} actually emitted a
	 * numeric string — a mismatch on either side is a {@link NumberFormatException} waiting to happen.
	 *
	 * <p>
	 * Returns <jk>false</jk> for {@link LocalTime}, {@link OffsetTime}, and {@link MonthDay} — these
	 * subtypes have no defensible epoch-millis interpretation, so {@link #format}'s {@code MILLIS} branch
	 * falls back to {@link #DEFAULT} (an ISO string) for them and the swap must keep the value as a string.
	 *
	 * @param c The {@link TemporalAccessor} subtype to query.
	 * @return <jk>true</jk> if {@link #format} emits a {@link Long}-parseable numeric string for {@code c}
	 *         under {@link #MILLIS}; <jk>false</jk> if the {@code MILLIS} branch falls back to ISO string.
	 */
	public static boolean isMillisNumeric(Class<? extends TemporalAccessor> c) {
		return ! (LocalTime.class.equals(c) || OffsetTime.class.equals(c) || MonthDay.class.equals(c));
	}

	/**
	 * Formats the specified value using this format.
	 *
	 * @param value The value to format. Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @param zoneId The default zone id used for time-only / unzoned values.
	 * @return The formatted string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 *         For {@link #MILLIS} this is a numeric string for every subtype except {@link LocalTime},
	 *         {@link OffsetTime}, and {@link MonthDay} (which fall back to their {@link #DEFAULT} ISO
	 *         string form).
	 */
	@SuppressWarnings({
		"java:S3776" // Per-subtype MILLIS coercion logic is intentionally explicit; flattening hurts readability
	})
	public String format(TemporalAccessor value, ZoneId zoneId) {
		if (value == null)
			return null;
		// MonthDay only implements TemporalAccessor (not Temporal) and has no DateTimeFormatter that
		// matches its --MM-DD wire form natively.  Use its own toString/parse pair regardless of the
		// configured format — every other TemporalFormat value is structurally meaningless without a year.
		if (value instanceof MonthDay value2)
			return value2.toString();
		if (this == MILLIS) {
			Long millis = toEpochMillis(value);
			if (millis != null)
				return Long.toString(millis);
			return DEFAULT.format(value, zoneId);
		}
		var zone = zoneId == null ? ZoneId.systemDefault() : zoneId;
		var tc = value.getClass();
		if (!DEFAULT_FORMATTERS.containsKey(tc) && (this == DEFAULT || this == NOT_SET))
			return DateTimeFormatter.ISO_INSTANT.format(toInstant(value, zone));
		var coerced = convertForFormatter(value, zone);
		return formatter(tc).format(coerced);
	}

	private static Instant toInstant(TemporalAccessor value, ZoneId zone) {
		if (value instanceof Instant value2)
			return value2;
		try {
			return Instant.from(value);
		} catch (Exception e) {
			if (value instanceof ChronoLocalDate value2)
				return value2.atTime(LocalTime.MIDNIGHT).atZone(zone).toInstant();
			if (value instanceof ChronoLocalDateTime<?> value2)
				return value2.atZone(zone).toInstant();
			if (value instanceof ChronoZonedDateTime<?> value2)
				return value2.toInstant();
			throw e;
		}
	}

	/**
	 * Parses the specified wire value using this format.
	 *
	 * @param <T> The temporal subtype.
	 * @param value The wire value. Can be <jk>null</jk> or blank (returns <jk>null</jk>).
	 * @param targetType The target temporal subtype.  Must be either a {@link Temporal} subtype or
	 *                   {@link MonthDay} (the only non-{@link Temporal} {@link TemporalAccessor} the JDK
	 *                   currently exposes).
	 * @param zoneId The default zone id used for time-only / unzoned values.
	 * @return The parsed value, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure requires unchecked cast on reflective from() result
		"java:S3776" // Per-subtype MILLIS dispatch is intentionally explicit; flattening hurts readability
	})
	public <T extends TemporalAccessor> T parse(String value, Class<T> targetType, ZoneId zoneId) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		var zone = zoneId == null ? ZoneId.systemDefault() : zoneId;
		if (this == MILLIS) {
			// MonthDay reverses what format() emits at MILLIS — its native --MM-DD shape; numeric input
			// at MILLIS×MonthDay is unreachable on the round-trip path because format() emits a string.
			if (MonthDay.class.equals(targetType))
				return (T) MonthDay.parse(s);
			// LocalTime never had a defensible epoch-millis interpretation; legacy callers get DEFAULT.parse.
			if (LocalTime.class.equals(targetType))
				return DEFAULT.parse(s, targetType, zone);
			// OffsetTime: format() falls back to DEFAULT (ISO_OFFSET_TIME string).  Be lenient on parse —
			// route numeric input through fromEpochMillis (legacy lenient support) and route the
			// ISO_OFFSET_TIME wire form back through DEFAULT.parse so the round-trip closes.
			if (OffsetTime.class.equals(targetType) && ! isAllDigits(s))
				return DEFAULT.parse(s, targetType, zone);
			return fromEpochMillis(Long.parseLong(s), targetType, zone);
		}
		// MonthDay's --MM-DD wire shape is its only stable representation; route to its native parser
		// regardless of the configured format.
		if (MonthDay.class.equals(targetType))
			return (T) MonthDay.parse(s);
		// Lenient: epoch-millis numeric strings are accepted for DEFAULT/NOT_SET on instant-like subtypes only.
		// Date-only subtypes (Year, YearMonth, LocalDate, MonthDay, LocalTime) use ISO patterns that are themselves
		// numeric ("2012", "2012-12") and must not be reinterpreted as epoch millis.
		if ((this == DEFAULT || this == NOT_SET) && isAllDigits(s) && isInstantLike(targetType))
			return fromEpochMillis(Long.parseLong(s), targetType, zone);
		var offset = (targetType == Instant.class) ? Z : zone;
		var ta = new DefaultingTemporalAccessor(formatter(targetType).parse(s), offset);
		return (T) invokeFrom(targetType, ta);
	}

	private static boolean isInstantLike(Class<?> targetType) {
		return Instant.class.equals(targetType)
			|| ZonedDateTime.class.equals(targetType)
			|| OffsetDateTime.class.equals(targetType)
			|| LocalDateTime.class.equals(targetType);
	}

	private static boolean isAllDigits(String s) {
		var n = s.length();
		if (n == 0)
			return false;
		var i = 0;
		if (s.charAt(0) == '-' || s.charAt(0) == '+') {
			if (n == 1)
				return false;
			i = 1;
		}
		for (; i < n; i++)
			if (!Character.isDigit(s.charAt(i)))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this format is numeric on the wire.
	 *
	 * @return <jk>true</jk> if this format emits a numeric wire value.
	 */
	public boolean isNumeric() {
		return this == MILLIS;
	}

	private static DateTimeFormatter defaultFormatter(Class<?> tc) {
		var f = DEFAULT_FORMATTERS.get(tc);
		return or(f, DateTimeFormatter.ISO_INSTANT);
	}

	@SuppressWarnings({
		"java:S1301" // switch over enum: explicit case constants keep the per-format mapping legible
	})
	private DateTimeFormatter formatter(Class<?> tc) {
		return switch (this) {
			case NOT_SET, DEFAULT -> defaultFormatter(tc);
			case BASIC_ISO_DATE -> DateTimeFormatter.BASIC_ISO_DATE;
			case ISO_DATE -> DateTimeFormatter.ISO_DATE;
			case ISO_DATE_TIME -> DateTimeFormatter.ISO_DATE_TIME;
			case ISO_INSTANT -> DateTimeFormatter.ISO_INSTANT;
			case ISO_LOCAL_DATE -> DateTimeFormatter.ISO_LOCAL_DATE;
			case ISO_LOCAL_DATE_TIME -> DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			case ISO_LOCAL_TIME -> DateTimeFormatter.ISO_LOCAL_TIME;
			case ISO_OFFSET_DATE -> DateTimeFormatter.ISO_OFFSET_DATE;
			case ISO_OFFSET_DATE_TIME -> DateTimeFormatter.ISO_OFFSET_DATE_TIME;
			case ISO_OFFSET_TIME -> DateTimeFormatter.ISO_OFFSET_TIME;
			case ISO_ORDINAL_DATE -> DateTimeFormatter.ISO_ORDINAL_DATE;
			case ISO_TIME -> DateTimeFormatter.ISO_TIME;
			case ISO_WEEK_DATE -> DateTimeFormatter.ISO_WEEK_DATE;
			case ISO_YEAR -> YEAR_FORMATTER;
			case ISO_YEAR_MONTH -> YEAR_MONTH_FORMATTER;
			case ISO_ZONED_DATE_TIME -> DateTimeFormatter.ISO_ZONED_DATE_TIME;
			case RFC_1123_DATE_TIME -> DateTimeFormatter.RFC_1123_DATE_TIME;
			case MILLIS -> throw new IllegalStateException("MILLIS does not use a DateTimeFormatter");
		};
	}

	/**
	 * Returns whether the configured formatter accepts a missing zone, mirroring the historical
	 * {@code TemporalSwap.zoneOptional()} flag.
	 */
	@SuppressWarnings({
		"java:S6541" // exhaustive switch keeps the per-format zone-optional table explicit
	})
	private boolean zoneOptional() {
		return switch (this) {
			case BASIC_ISO_DATE, ISO_DATE, ISO_DATE_TIME, ISO_LOCAL_DATE, ISO_LOCAL_DATE_TIME, ISO_LOCAL_TIME,
				ISO_ORDINAL_DATE, ISO_TIME, ISO_WEEK_DATE, ISO_YEAR, ISO_YEAR_MONTH -> true;
			default -> false;
		};
	}

	@SuppressWarnings({
		"java:S3776" // Per-subtype coercion table mirrors the legacy TemporalSwap.convertToSerializable path
	})
	private Temporal convertForFormatter(TemporalAccessor value, ZoneId zoneId) {
		if (value instanceof Temporal value2 && (this == DEFAULT || this == NOT_SET))
			return value2;
		var tc = value.getClass();
		// Instant always serializes in GMT.
		if (tc == Instant.class)
			return ZonedDateTime.from(new DefaultingTemporalAccessor(value, Z));
		// Zoned / offset variants are already serializable by any pattern.
		if (value instanceof Temporal value2 && (tc == ZonedDateTime.class || tc == OffsetDateTime.class))
			return value2;
		if (zoneOptional()) {
			if (value instanceof LocalDateTime value2)
				return value2;
			if (tc == OffsetTime.class)
				return ZonedDateTime.from(new DefaultingTemporalAccessor(value, zoneId));
			return LocalDateTime.from(new DefaultingTemporalAccessor(value, zoneId));
		}
		return ZonedDateTime.from(new DefaultingTemporalAccessor(value, zoneId));
	}

	@SuppressWarnings({
		"java:S3776" // Per-subtype epoch-millis coercion is intentionally explicit
	})
	private static Long toEpochMillis(TemporalAccessor value) {
		if (value instanceof Instant value2)
			return value2.toEpochMilli();
		if (value instanceof OffsetDateTime value2)
			return value2.toInstant().toEpochMilli();
		if (value instanceof ZonedDateTime value2)
			return value2.toInstant().toEpochMilli();
		if (value instanceof LocalDateTime value2)
			return value2.toInstant(ZoneOffset.UTC).toEpochMilli();
		if (value instanceof LocalDate value2)
			return value2.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		if (value instanceof YearMonth value2)
			return value2.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		if (value instanceof Year value2)
			return value2.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		return null;
	}

	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts for per-subtype factory dispatch
	})
	private static <T extends TemporalAccessor> T fromEpochMillis(long millis, Class<T> targetType, ZoneId zoneId) {
		var instant = Instant.ofEpochMilli(millis);
		if (targetType == Instant.class) return (T) instant;
		if (targetType == ZonedDateTime.class) return (T) instant.atZone(zoneId);
		if (targetType == OffsetDateTime.class) return (T) instant.atZone(zoneId).toOffsetDateTime();
		if (targetType == LocalDateTime.class) return (T) instant.atZone(ZoneOffset.UTC).toLocalDateTime();
		if (targetType == LocalDate.class) return (T) instant.atZone(ZoneOffset.UTC).toLocalDate();
		if (targetType == OffsetTime.class) return (T) instant.atZone(zoneId).toOffsetDateTime().toOffsetTime();
		if (targetType == YearMonth.class) return (T) YearMonth.from(instant.atZone(ZoneOffset.UTC));
		if (targetType == Year.class) return (T) Year.from(instant.atZone(ZoneOffset.UTC));
		throw iaex("Cannot convert epoch millis to %s", targetType.getName());
	}

	private static Method findFromMethod(Class<? extends TemporalAccessor> tc) {
		// @formatter:off
		return FROM_METHODS.computeIfAbsent(tc, k -> info(tc).getPublicMethod(
			x -> x.isStatic()
				&& x.isNotDeprecated()
				&& x.hasName("from")
				&& x.hasReturnType(tc)
				&& x.hasParameterTypes(TemporalAccessor.class)
			)
			.map(MethodInfo::inner)
			.orElseThrow(() -> iaex("No static from(TemporalAccessor) on %s", tc.getName())));
		// @formatter:on
	}

	private static Object invokeFrom(Class<? extends TemporalAccessor> tc, TemporalAccessor ta) {
		try {
			return findFromMethod(tc).invoke(null, ta);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw toRex(e);
		}
	}
}
