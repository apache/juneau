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
package org.apache.juneau.console.data;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/** Value conversions shared by {@link SeriesSource} and {@link SeriesAggregator}. */
final class Values {

	private Values() {}

	/** True for the value types written as an ISO-8601 UTC instant. */
	static boolean isTime(Object v) {
		return v instanceof Instant || v instanceof ZonedDateTime || v instanceof OffsetDateTime
			|| v instanceof LocalDate || v instanceof Date || v instanceof Calendar || v instanceof LocalDateTime;
	}

	/** The value as an {@link Instant}; LocalDate is the start of that day in UTC, and LocalDateTime is read as UTC. */
	static Instant toInstant(Object v) {
		if (v instanceof Instant x) return x;
		if (v instanceof ZonedDateTime x) return x.toInstant();
		if (v instanceof OffsetDateTime x) return x.toInstant();
		if (v instanceof LocalDate x) return x.atStartOfDay(ZoneOffset.UTC).toInstant();
		// Not toInstant(): java.sql.Date and Time throw.
		if (v instanceof Date x) return Instant.ofEpochMilli(x.getTime());
		if (v instanceof Calendar x) return x.toInstant();
		if (v instanceof LocalDateTime x) return x.toInstant(ZoneOffset.UTC);
		if (v instanceof Number x) return Instant.ofEpochMilli(x.longValue());
		if (v instanceof CharSequence x) return parseInstant(x.toString());
		return null;
	}

	/** Parses an ISO-8601 instant or a digits-only epoch-millisecond string; null when neither. */
	static Instant parseInstant(String s) {
		if (ie(s)) return null;
		try {
			if (s.chars().allMatch(Character::isDigit)) return Instant.ofEpochMilli(Long.parseLong(s));
			return Instant.parse(s);
		} catch (NumberFormatException | DateTimeException e) {
			return null;
		}
	}

	/** ISO-8601 UTC text, e.g. <c>2026-10-11T01:02:03Z</c>. */
	static String iso(Instant i) {
		return DateTimeFormatter.ISO_INSTANT.format(i.truncatedTo(ChronoUnit.MILLIS));
	}

	/** True when the number has no finite double value (NaN or an infinity). */
	static boolean nonFinite(Number n) {
		return (n instanceof Double || n instanceof Float) && !Double.isFinite(n.doubleValue());
	}
}
