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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.math.*;
import java.time.*;
import java.time.format.*;
import java.util.regex.*;

/**
 * The parser backing {@link SearchType#parse(String)} - the inverse of {@link ValueFormat}.
 *
 * <p>
 * Package-private: applications call {@link SearchType#parse(String)}, never this class directly.
 *
 * <p>
 * A signed relative-duration literal ({@code [+-]<n>(ms|s|m|h|d)}, e.g. {@code -24h}, or a signed ISO-8601 duration
 * such as {@code -PT24H} or {@code P1D}; identical semantics) is a signed millisecond
 * {@link BigDecimal} for {@link SearchType#NUMERIC} and {@code requestTime} plus that amount (an
 * {@link OffsetDateTime} at UTC) for {@link SearchType#TIMESTAMP}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Object <jv>v</jv> = ValueParse.<jsm>parse</jsm>(SearchType.<jsf>NUMERIC</jsf>, <js>"42"</js>);  <jc>// BigDecimal("42")</jc>
 * </p>
 *
 * @since 10.0.0
 */
final class ValueParse {

	/** Largest accepted absolute {@link BigDecimal#scale() scale}; guards against absurd exponents such as {@code 1E+999999999}. */
	static final int MAX_SCALE = 1000;

	/** Largest accepted {@link BigDecimal#precision() precision} (significant digits). */
	static final int MAX_PRECISION = 1000;

	/** Matches a signed relative-duration literal such as {@code -24h} or {@code +90ms}; lowercase units only; anchored so a bare {@code -5} is a plain number. */
	private static final Pattern DURATION = Pattern.compile("^([+-])(\\d+)(ms|s|m|h|d)$");

	/** Matches the start of a signed ISO-8601 duration ({@code PT24H}, {@code -PT24H}, {@code +P1D}); case-insensitive like {@link Duration#parse(CharSequence)}. */
	private static final Pattern ISO_DURATION_START = Pattern.compile("^[+-]?[Pp]");

	private ValueParse() {}

	/**
	 * Parses {@code value} into {@code type}'s Java representation.
	 *
	 * @param type The target type.  Must not be <jk>null</jk>.
	 * @param value The literal text, or <jk>null</jk> (treated as empty).
	 * @return The parsed value (see {@link SearchType#parse(String)}).
	 * @throws BeanQuerySyntaxException If {@code value} does not parse for {@code type}.
	 */
	static Object parse(SearchType type, String value) {
		return parse(type, value, null, true);
	}

	/**
	 * Parses a <em>cell</em> value: like {@link #parse(SearchType, String)} but a relative-duration literal is never
	 * recognized on a {@link SearchType#TIMESTAMP} (a stored String such as {@code -24h} is unparseable, not "now minus a day").
	 *
	 * @param type The target type.  Must not be <jk>null</jk>.
	 * @param value The cell text, or <jk>null</jk> (treated as empty).
	 * @return The parsed value.
	 * @throws BeanQuerySyntaxException If {@code value} does not parse for {@code type}.
	 */
	static Object parseAbsolute(SearchType type, String value) {
		return parse(type, value, null, false);
	}

	/**
	 * Parses {@code value} into {@code type}'s Java representation, resolving a relative-duration literal (if any)
	 * against {@code requestTime}.
	 *
	 * @param type The target type.  Must not be <jk>null</jk>.
	 * @param value The literal text, or <jk>null</jk> (treated as empty).
	 * @param requestTime The instant a relative-duration {@link SearchType#TIMESTAMP} literal resolves against.
	 * 	Must not be <jk>null</jk>.
	 * @return The parsed value (see {@link SearchType#parse(String, Instant)}).
	 * @throws BeanQuerySyntaxException If {@code value} does not parse for {@code type}.
	 */
	static Object parse(SearchType type, String value, Instant requestTime) {
		return parse(type, value, requestTime, true);
	}

	private static Object parse(SearchType type, String value, Instant requestTime, boolean durations) {
		var v = value == null ? "" : value.strip();
		return switch (type) {
			case NUMERIC -> numeric(value, v);
			case BOOLEAN -> bool(value, v);
			case TIMESTAMP -> timestamp(value, v, requestTime, durations);
			default -> value == null ? "" : value;
		};
	}

	/**
	 * Returns the signed milliseconds of a relative-duration literal, or <jk>null</jk> if {@code v} is not shaped
	 * like one.  A duration whose millisecond total overflows a {@code long} is {@code BAD_VALUE}.
	 */
	private static Long durationMillis(SearchType type, String raw, String v) {
		if (ISO_DURATION_START.matcher(v).find())
			return isoDurationMillis(type, raw, v);
		var m = DURATION.matcher(v);
		if (! m.matches())
			return null;
		try {
			var n = Math.multiplyExact(Long.parseLong(m.group(2)), unitMillis(m.group(3)));
			return "-".equals(m.group(1)) ? Math.negateExact(n) : n;
		} catch (ArithmeticException | NumberFormatException e) {
			throw badValue(type, raw);
		}
	}

	/**
	 * Parses a signed ISO-8601 duration via {@link Duration#parse(CharSequence)} into signed milliseconds (fractions
	 * of a millisecond are floored).  Only the single leading {@code +}/{@code -} is accepted: Java's per-component
	 * signs ({@code PT-1H}, {@code -PT-1H}) are {@code BAD_VALUE}, as is anything {@code Duration.parse} rejects
	 * (years, months, weeks, junk) or whose millisecond total overflows a {@code long}.
	 */
	private static long isoDurationMillis(SearchType type, String raw, String v) {
		if (v.indexOf('-', 1) >= 0 || v.indexOf('+', 1) >= 0)
			throw badValue(type, raw);
		try {
			return Duration.parse(v).toMillis();
		} catch (DateTimeParseException | ArithmeticException e) {
			throw badValue(type, raw);
		}
	}

	private static long unitMillis(String unit) {
		return switch (unit) {
			case "ms" -> 1L;
			case "s" -> 1_000L;
			case "m" -> 60_000L;
			case "h" -> 3_600_000L;
			default -> 86_400_000L;  // "d" - the pattern admits nothing else.
		};
	}

	private static Object numeric(String raw, String v) {
		var millis = durationMillis(SearchType.NUMERIC, raw, v);
		if (millis != null)
			return BigDecimal.valueOf(millis);
		BigDecimal bd;
		try {
			bd = new BigDecimal(v);
		} catch (NumberFormatException e) {
			throw badValue(SearchType.NUMERIC, raw);
		}
		if (Math.abs((long)bd.scale()) > MAX_SCALE || bd.precision() > MAX_PRECISION)
			throw badValue(SearchType.NUMERIC, raw);
		return bd;
	}

	private static Object bool(String raw, String v) {
		if (eqic(v, "true"))
			return Boolean.TRUE;
		if (eqic(v, "false"))
			return Boolean.FALSE;
		throw badValue(SearchType.BOOLEAN, raw);
	}

	private static Object timestamp(String raw, String v, Instant requestTime, boolean durations) {
		var millis = durations ? durationMillis(SearchType.TIMESTAMP, raw, v) : null;
		if (millis != null) {
			try {
				// The clock is read only here, and only when the caller did not supply a request time.
				var base = requestTime == null ? Instant.now() : requestTime;
				return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Math.addExact(base.toEpochMilli(), millis)), ZoneOffset.UTC);
			} catch (DateTimeException | ArithmeticException e) {
				throw badValue(SearchType.TIMESTAMP, raw);
			}
		}
		try {
			return OffsetDateTime.parse(v, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		} catch (DateTimeParseException e1) {
			// Fall through to the next accepted shape.
		}
		try {
			return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} catch (DateTimeParseException e2) {
			// Fall through to the next accepted shape.
		}
		try {
			return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e3) {
			// Fall through to epoch milliseconds.
		}
		// Epoch milliseconds (every long is a valid Instant/OffsetDateTime, so only NumberFormatException can occur): optional leading '-' only (Long.parseLong would also accept '+').
		if (v.startsWith("+"))
			throw badValue(SearchType.TIMESTAMP, raw);
		try {
			return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(v)), ZoneOffset.UTC);
		} catch (NumberFormatException e4) {
			throw badValue(SearchType.TIMESTAMP, raw);
		}
	}

	private static BeanQuerySyntaxException badValue(SearchType type, String raw) {
		return new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.BAD_VALUE, "Value '%s' is not a valid %s.", raw == null ? "" : raw, type.wire());
	}
}
