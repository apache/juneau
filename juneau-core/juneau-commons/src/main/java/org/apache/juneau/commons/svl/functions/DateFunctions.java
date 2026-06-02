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
package org.apache.juneau.commons.svl.functions;

import java.time.*;
import java.time.format.*;

import org.apache.juneau.commons.svl.*;

/**
 * Date/time functions for the {@code #{...}} script catalog.
 *
 * <p>
 * All functions exchange epoch milliseconds as the integration currency. Formatting and parsing
 * use {@link java.time.format.DateTimeFormatter} patterns for the explicit-pattern variants;
 * the no-pattern variants use ISO-8601 ({@code Instant.toString()} on the way out, the ISO
 * detection chain on the way in).
 */
public final class DateFunctions {

	private DateFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Now.class, ParseDate.class, FormatDate.class
	};

	/** {@code #{now()}} — current epoch milliseconds (UTC). */
	public static class Now extends TypedFunction {
		@Override public String name() { return "now"; }
		public String invoke() { return String.valueOf(System.currentTimeMillis()); }
	}

	/**
	 * {@code #{parseDate(s)}} / {@code #{parseDate(s, format)}} — parses an ISO-8601 string or
	 * a {@link DateTimeFormatter}-pattern string to epoch milliseconds.
	 *
	 * <p>
	 * The single-arg form tries, in order: {@link Instant#parse}, {@link OffsetDateTime#parse},
	 * {@link ZonedDateTime#parse}, {@link LocalDateTime#parse} (assumed UTC),
	 * {@link LocalDate#parse} (start-of-day UTC).
	 */
	public static class ParseDate extends TypedFunction {
		@Override public String name() { return "parseDate"; }

		public String invoke(String s) {
			if (s == null || s.isEmpty()) return "0";
			return String.valueOf(parseIso(s));
		}

		public String invoke(String s, String format) {
			if (s == null || s.isEmpty()) return "0";
			if (format == null || format.isEmpty()) return invoke(s);
			var fmt = DateTimeFormatter.ofPattern(format);
			try {
				return String.valueOf(LocalDateTime.parse(s, fmt).toInstant(ZoneOffset.UTC).toEpochMilli());
			} catch (@SuppressWarnings("unused") DateTimeParseException e) {
				// Try as a date-only pattern.
			}
			return String.valueOf(LocalDate.parse(s, fmt).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
		}

		@SuppressWarnings({
			"java:S3776", // Cognitive complexity: small ISO-format dispatch.
			"java:S1166", // Exceptions swallowed intentionally — this is a try-each-format chain.
		})
		private static long parseIso(String s) {
			try { return Instant.parse(s).toEpochMilli(); } catch (@SuppressWarnings("unused") DateTimeParseException e) { /* fall through */ }
			try { return OffsetDateTime.parse(s).toInstant().toEpochMilli(); } catch (@SuppressWarnings("unused") DateTimeParseException e) { /* fall through */ }
			try { return ZonedDateTime.parse(s).toInstant().toEpochMilli(); } catch (@SuppressWarnings("unused") DateTimeParseException e) { /* fall through */ }
			try { return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli(); } catch (@SuppressWarnings("unused") DateTimeParseException e) { /* fall through */ }
			return LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		}
	}

	/**
	 * {@code #{formatDate(epoch)}} — renders {@code epoch} as ISO-8601 ({@link Instant#toString()}).
	 *
	 * <p>
	 * {@code #{formatDate(epoch, format)}} — renders {@code epoch} per a
	 * {@link DateTimeFormatter} pattern in UTC.
	 */
	public static class FormatDate extends TypedFunction {
		@Override public String name() { return "formatDate"; }

		public String invoke(long epoch) {
			return Instant.ofEpochMilli(epoch).toString();
		}

		public String invoke(long epoch, String format) {
			if (format == null || format.isEmpty()) return invoke(epoch);
			var fmt = DateTimeFormatter.ofPattern(format).withZone(ZoneOffset.UTC);
			return fmt.format(Instant.ofEpochMilli(epoch));
		}
	}
}
