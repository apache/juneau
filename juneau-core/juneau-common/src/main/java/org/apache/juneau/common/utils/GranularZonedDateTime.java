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

import static org.apache.juneau.common.utils.DateUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

/**
 * A ZonedDateTime with precision information for granular time operations.
 *
 * <p>
 * This class combines a {@link ZonedDateTime} with a {@link ChronoField} precision identifier,
 * allowing for granular time operations such as rolling by specific time units.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create with year precision</jc>
 * 	GranularZonedDateTime <jv>gdt</jv> = GranularZonedDateTime.parse(<js>"2011"</js>);
 * 	<jc>// Roll forward by one year</jc>
 * 	<jv>gdt</jv>.roll(1);
 * 	<jc>// Get the ZonedDateTime</jc>
 * 	ZonedDateTime <jv>zdt</jv> = <jv>gdt</jv>.getZonedDateTime();
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is immutable and thread-safe.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link ZonedDateTime}
 * 	<li class='jm'>{@link ChronoField}
 * 	<li class='jm'>{@link DateUtils}
 * </ul>
 */
public class GranularZonedDateTime {

	/** The ZonedDateTime value */
	public final ZonedDateTime zdt;

	/** The precision of this time value */
	public final ChronoField precision;

	/**
	 * Constructor.
	 *
	 * @param date The date to wrap.
	 * @param precision The precision of this time value.
	 */
	public GranularZonedDateTime(Date date, ChronoField precision) {
		this.zdt = date.toInstant().atZone(ZoneId.systemDefault());
		this.precision = precision;
	}

	/**
	 * Constructor.
	 *
	 * @param zdt The ZonedDateTime value.
	 * @param precision The precision of this time value.
	 */
	public GranularZonedDateTime(ZonedDateTime zdt, ChronoField precision) {
		this.zdt = zdt;
		this.precision = precision;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A new GranularZonedDateTime with the same values.
	 */
	public GranularZonedDateTime copy() {
		return new GranularZonedDateTime(zdt, precision);
	}

	/**
	 * Returns the ZonedDateTime value.
	 *
	 * @return The ZonedDateTime value.
	 */
	public ZonedDateTime getZonedDateTime() { return zdt; }

	/**
	 * Rolls this time value by the specified amount using the current precision.
	 *
	 * @param amount The amount to roll by.
	 * @return A new GranularZonedDateTime with the rolled value.
	 */
	public GranularZonedDateTime roll(int amount) {
		return roll(precision, amount);
	}

	/**
	 * Rolls this time value by the specified amount using the specified field.
	 *
	 * @param field The field to roll by.
	 * @param amount The amount to roll by.
	 * @return A new GranularZonedDateTime with the rolled value.
	 */
	public GranularZonedDateTime roll(ChronoField field, int amount) {
		// Use DateUtils utility method to convert ChronoField to ChronoUnit
		ChronoUnit unit = toChronoUnit(field);
		if (nn(unit)) {
			ZonedDateTime newZdt = zdt.plus(amount, unit);
			return new GranularZonedDateTime(newZdt, precision);
		}
		return this;
	}

	/**
	 * Parses a timestamp string and returns a GranularZonedDateTime.
	 *
	 * <p>
	 * This method uses {@link DateUtils#fromIso8601(String)} for parsing and
	 * {@link DateUtils#getPrecisionFromString(String)} for determining precision.
	 *
	 * @param seg The string segment to parse.
	 * @return A GranularZonedDateTime representing the parsed timestamp.
	 * @throws BasicRuntimeException If the string cannot be parsed as a valid timestamp.
	 */
	public static GranularZonedDateTime parse(String seg) {
		// Try DateUtils.fromIso8601 first for consistency
		ZonedDateTime zdt = fromIso8601(seg);
		if (nn(zdt)) {
			// Determine precision based on the input string
			var precision = getPrecisionFromString(seg);
			return new GranularZonedDateTime(zdt, precision);
		}

		throw rex("Invalid date encountered: ''{0}''", seg);
	}
}
