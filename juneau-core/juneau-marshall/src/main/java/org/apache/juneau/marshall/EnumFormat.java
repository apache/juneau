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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Supported wire formats for {@link Enum} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#enumFormat(EnumFormat)},
 * {@link org.apache.juneau.marshall.Marshalled#enumFormat()},
 * {@link org.apache.juneau.marshall.MarshalledProp#enumFormat()}, and
 * {@link org.apache.juneau.marshall.MarshalledConfig#enumFormat()} to control how
 * {@link Enum} values are written to and read from the wire.
 *
 * <p>
 * The default is {@link #TO_STRING} which preserves the historical wire output produced by
 * {@code Object.toString()} (the previous default before this enum existed). New APIs SHOULD prefer
 * {@link #NAME} for stability against {@code toString()} overrides.
 *
 * <p>
 * Numeric formats ({@link #ORDINAL}) emit native numeric wire types on binary serializers
 * (BSON / CBOR / MsgPack) and a bare integer literal on text formats. All other constants emit a
 * UTF-8 string.
 *
 * <p>
 * Parsers SHALL accept any wire shape that the corresponding serializer can produce, regardless of
 * the parser-side {@code EnumFormat} setting. The parser-side setting is consulted only for genuinely
 * ambiguous input.
 */
public enum EnumFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/** Calls {@link Object#toString()} on the enum value (the default). */
	TO_STRING,

	/** Calls {@link Enum#name()} on the enum value. */
	NAME,

	/** Lower-case hyphenated form: {@code MY_ENUM_VALUE} → {@code "my-enum-value"}. */
	LOWER_HYPHEN,

	/** Upper-case hyphenated form: {@code MY_ENUM_VALUE} → {@code "MY-ENUM-VALUE"}. */
	UPPER_HYPHEN,

	/** Lower-case underscored form: {@code MY_ENUM_VALUE} → {@code "my_enum_value"}. */
	LOWER_UNDERSCORE,

	/** Lower-case form using {@link Locale#ROOT}: {@code MY_ENUM_VALUE} → {@code "my_enum_value"}. */
	LOWER,

	/** Upper-case form using {@link Locale#ROOT}: equivalent to {@link Enum#name()} for canonical UPPER_SNAKE enum names. */
	UPPER,

	/** {@link Enum#ordinal()} as a numeric value. */
	ORDINAL;

	/**
	 * Formats the specified enum value using this format.
	 *
	 * @param value The enum value.  Can be <jk>null</jk>.
	 * @return The formatted wire representation, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public String format(Enum<?> value) {
		if (value == null)
			return null;
		return switch (this) {
			case NOT_SET, TO_STRING -> value.toString();
			case NAME, UPPER -> value.name();
			case LOWER_HYPHEN -> value.name().toLowerCase(Locale.ROOT).replace('_', '-');
			case UPPER_HYPHEN -> value.name().replace('_', '-');
			case LOWER_UNDERSCORE, LOWER -> value.name().toLowerCase(Locale.ROOT);
			case ORDINAL -> Integer.toString(value.ordinal());
		};
	}

	/**
	 * Parses the specified wire value into an enum constant of the specified class.
	 *
	 * <p>
	 * Parsing is format-agnostic: every constant accepts any of the wire shapes produced by
	 * {@link #format(Enum)} regardless of the configured constant.  Lookup order:
	 * <ol>
	 * 	<li>Numeric input → resolved as {@link #ORDINAL}.
	 * 	<li>Exact match against {@link Enum#name()}.
	 * 	<li>Exact match against {@link Object#toString()}.
	 * 	<li>Reconstruct UPPER_SNAKE form (replace {@code -} with {@code _}, upper-case) and re-try {@code name()}.
	 * 	<li>Case-insensitive match against {@code name()} and {@code toString()}.
	 * </ol>
	 *
	 * @param <E> The enum class type.
	 * @param value The wire value.  Can be <jk>null</jk> or blank.
	 * @param enumClass The enum class.  Must not be <jk>null</jk>.
	 * @return The parsed enum constant, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value does not match any enum constant.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for enum format parsing dispatch
	})
	public static <E extends Enum<E>> E parse(String value, Class<E> enumClass) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		// Numeric input: try ORDINAL first.
		if (isAllDigits(s)) {
			try {
				var ord = Integer.parseInt(s);
				var consts = enumClass.getEnumConstants();
				if (ord >= 0 && ord < consts.length)
					return consts[ord];
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				// Fall through to string-based matching.
			}
		}
		// Exact name match.
		for (var e : enumClass.getEnumConstants())
			if (e.name().equals(s))
				return e;
		// Exact toString() match.
		for (var e : enumClass.getEnumConstants())
			if (s.equals(e.toString()))
				return e;
		// Reconstruct UPPER_SNAKE form (handles LOWER_HYPHEN / UPPER_HYPHEN / LOWER / LOWER_UNDERSCORE).
		var snake = s.replace('-', '_').toUpperCase(Locale.ROOT);
		for (var e : enumClass.getEnumConstants())
			if (e.name().equals(snake))
				return e;
		// Case-insensitive fallback.
		for (var e : enumClass.getEnumConstants())
			if (e.name().equalsIgnoreCase(s) || s.equalsIgnoreCase(e.toString()))
				return e;
		throw iaex("Could not resolve enum value ''{0}'' on class ''{1}''", value, enumClass.getName());
	}

	/**
	 * Returns <jk>true</jk> if this format is numeric on the wire.
	 *
	 * @return <jk>true</jk> if this format emits a numeric wire value.
	 */
	public boolean isNumeric() {
		return this == ORDINAL;
	}

	private static boolean isAllDigits(String s) {
		if (s.isEmpty())
			return false;
		var i = s.charAt(0) == '-' || s.charAt(0) == '+' ? 1 : 0;
		if (i == s.length())
			return false;
		for (; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c < '0' || c > '9')
				return false;
		}
		return true;
	}
}
