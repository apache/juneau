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
package org.apache.juneau;

import java.time.*;
import java.util.*;

/**
 * Supported wire formats for {@link TimeZone} and {@link ZoneId} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#timeZoneFormat(TimeZoneFormat)},
 * {@link org.apache.juneau.annotation.Marshalled#timeZoneFormat()},
 * {@link org.apache.juneau.annotation.MarshalledProp#timeZoneFormat()}, and
 * {@link org.apache.juneau.annotation.MarshalledConfig#timeZoneFormat()} to control how time-zone
 * identifiers are written to and read from the wire.
 *
 * <p>
 * The default is {@link #ID} which preserves the historical wire output produced before this enum existed
 * ({@code TimeZone.getID()} / {@code ZoneId.getId()}).
 */
public enum TimeZoneFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/** The IANA zone id (the default): <js>"America/Los_Angeles"</js>, <js>"UTC"</js>, <js>"GMT-08:00"</js>. */
	ID,

	/** Normalized to a {@link ZoneOffset} where possible: <js>"-08:00"</js>; falls back to {@link #ID} for region zones. */
	OFFSET,

	/**
	 * Default long display name in {@link Locale#ROOT} (formatting only; not always round-trippable):
	 * <js>"Pacific Standard Time"</js>.
	 *
	 * <p>
	 * Parsing accepts any value that {@link ZoneId#of(String)} understands; localized display names are
	 * <b>not</b> reverse-parsed.
	 */
	NAME_LONG,

	/**
	 * Default short display name in {@link Locale#ROOT} (formatting only; not always round-trippable):
	 * <js>"PST"</js>.
	 *
	 * <p>
	 * Parsing accepts any value that {@link ZoneId#of(String)} understands; localized display names are
	 * <b>not</b> reverse-parsed.
	 */
	NAME_SHORT;

	/**
	 * Formats the specified {@link TimeZone} using this format.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @return The formatted string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public String format(TimeZone value) {
		if (value == null)
			return null;
		return switch (this) {
			case NAME_LONG -> value.getDisplayName(false, TimeZone.LONG, Locale.ROOT);
			case NAME_SHORT -> value.getDisplayName(false, TimeZone.SHORT, Locale.ROOT);
			case OFFSET -> formatOffset(value.toZoneId());
			case NOT_SET, ID -> value.getID();
		};
	}

	/**
	 * Formats the specified {@link ZoneId} using this format.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @return The formatted string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public String format(ZoneId value) {
		if (value == null)
			return null;
		return switch (this) {
			case NAME_LONG -> TimeZone.getTimeZone(value).getDisplayName(false, TimeZone.LONG, Locale.ROOT);
			case NAME_SHORT -> TimeZone.getTimeZone(value).getDisplayName(false, TimeZone.SHORT, Locale.ROOT);
			case OFFSET -> formatOffset(value);
			case NOT_SET, ID -> value.getId();
		};
	}

	/**
	 * Parses the specified wire value as a {@link TimeZone}.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @return The parsed {@link TimeZone}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 */
	public static TimeZone parseTimeZone(String value) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		return TimeZone.getTimeZone(s);
	}

	/**
	 * Parses the specified wire value as a {@link ZoneId}.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @return The parsed {@link ZoneId}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 */
	public static ZoneId parseZoneId(String value) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		return ZoneId.of(s, ZoneId.SHORT_IDS);
	}

	private static String formatOffset(ZoneId zoneId) {
		try {
			return ZoneOffset.from(zoneId.getRules().getOffset(Instant.now())).getId();
		} catch (@SuppressWarnings("unused") Exception e) {  // NOSONAR: ZoneOffset.from throws DateTimeException; fall back to ID
			return zoneId.getId();
		}
	}
}
