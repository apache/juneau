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

import java.util.*;

/**
 * Supported wire formats for {@link Locale} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#localeFormat(LocaleFormat)},
 * {@link org.apache.juneau.annotation.Marshalled#localeFormat()},
 * {@link org.apache.juneau.annotation.MarshalledProp#localeFormat()}, and
 * {@link org.apache.juneau.annotation.MarshalledConfig#localeFormat()} to control how {@link Locale}
 * values are written to and read from the wire.
 *
 * <p>
 * The default is {@link #BCP_47} which preserves the historical wire output produced before this enum existed
 * ({@code Locale.toLanguageTag()} / {@code Locale.forLanguageTag(String)}).
 */
public enum LocaleFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * BCP 47 language tag (the default): <js>"en-US"</js>, <js>"zh-Hant-TW"</js>.
	 *
	 * <p>
	 * Round-trips through {@link Locale#toLanguageTag()} / {@link Locale#forLanguageTag(String)}.
	 */
	BCP_47,

	/**
	 * Underscore-separated form: <js>"en_US"</js>, <js>"zh_TW"</js>.
	 *
	 * <p>
	 * Round-trips through {@link Locale#toString()} on the format side and a hand-rolled parser
	 * (language + country + variant tokens) on the parse side. Script subtags are not representable.
	 */
	UNDERSCORE;

	/**
	 * Formats the specified value using this format.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @return The formatted string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public String format(Locale value) {
		if (value == null)
			return null;
		return switch (this) {
			case UNDERSCORE -> value.toString();
			case NOT_SET, BCP_47 -> value.toLanguageTag();
		};
	}

	/**
	 * Parses the specified wire value using this format.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @return The parsed {@link Locale}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 */
	public Locale parse(String value) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		return switch (this) {
			case UNDERSCORE -> parseUnderscore(s);
			case NOT_SET, BCP_47 -> Locale.forLanguageTag(s);
		};
	}

	private static Locale parseUnderscore(String s) {
		var parts = s.split("_", -1);
		return switch (parts.length) {
			case 1 -> new Locale(parts[0]);  // NOSONAR: legacy Locale constructor required for underscore form
			case 2 -> new Locale(parts[0], parts[1]);  // NOSONAR: legacy Locale constructor required for underscore form
			default -> new Locale(parts[0], parts[1], parts[2]);  // NOSONAR: legacy Locale constructor required for underscore form
		};
	}
}
