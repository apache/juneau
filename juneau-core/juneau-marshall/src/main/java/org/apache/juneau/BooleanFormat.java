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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

/**
 * Supported wire formats for {@link Boolean} / <code><jk>boolean</jk></code> values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder<?>#booleanFormat(BooleanFormat)},
 * {@link org.apache.juneau.annotation.Marshalled#booleanFormat()},
 * {@link org.apache.juneau.annotation.MarshalledProp#booleanFormat()}, and
 * {@link org.apache.juneau.annotation.MarshalledConfig#booleanFormat()} to control how boolean values
 * are written to text-based wire formats.
 *
 * <p>
 * The default is {@link #TRUE_FALSE} which preserves the historical wire output (the bare
 * <c>true</c> / <c>false</c> tokens).
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link org.apache.juneau.annotation.MarshalledProp#booleanFormat() @MarshalledProp(booleanFormat=…)} on the bean property.
 * 	<li>{@link org.apache.juneau.annotation.Marshalled#booleanFormat() @Marshalled(booleanFormat=…)} on the bean class.
 * 	<li>{@link org.apache.juneau.annotation.MarshalledConfig#booleanFormat() @MarshalledConfig(booleanFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder<?>#booleanFormat(BooleanFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.booleanFormat</c>.
 * 	<li>The default constant ({@link #TRUE_FALSE}).
 * </ol>
 *
 * <h5 class='topic'>Parser leniency</h5>
 *
 * <p>
 * Parsers SHALL accept any of the textual shapes ({@code true}/{@code false}, {@code 1}/{@code 0},
 * {@code yes}/{@code no}, {@code y}/{@code n}, {@code on}/{@code off}) regardless of the parser-side
 * {@code BooleanFormat} setting. Matching is case-insensitive. The configured constant is informational only.
 *
 * <h5 class='topic'>Binary serializers</h5>
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Proto / Parquet) emit a native boolean wire type regardless
 * of this setting; the setting only affects text-based serializers (JSON / XML / CSV / HOCON / TOML / etc.).
 * For {@link #ZERO_ONE}, JSON / JSON5 emit the bare numeric token <c>0</c> / <c>1</c> (not the string
 * <c>"0"</c> / <c>"1"</c>) so the wire shape matches an <code><jk>int</jk></code> field.
 */
public enum BooleanFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Standard boolean tokens (the default).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	true
	 * 	false
	 * </p>
	 *
	 * <p>
	 * Round-trips through {@link Boolean#toString()} / {@link Boolean#parseBoolean(String)}.
	 */
	TRUE_FALSE,

	/**
	 * Numeric tokens — <c>0</c> for {@code false} and <c>1</c> for {@code true}.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	1
	 * 	0
	 * </p>
	 *
	 * <p>
	 * JSON / JSON5 emit the bare numeric token <c>0</c> / <c>1</c> (not the quoted string <c>"0"</c> /
	 * <c>"1"</c>) so the wire shape matches an <code><jk>int</jk></code> field. Useful for CSV-shaped
	 * JSON consumers and SQL-style payloads.
	 */
	ZERO_ONE,

	/**
	 * English long-form yes / no.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	yes
	 * 	no
	 * </p>
	 *
	 * <p>
	 * Common in human-friendly CSV and form-encoded payloads.
	 */
	YES_NO,

	/**
	 * Compact single-character yes / no.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	Y
	 * 	N
	 * </p>
	 *
	 * <p>
	 * Compact single-character form, common in DB and CSV exports.
	 */
	Y_N,

	/**
	 * On / off.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	on
	 * 	off
	 * </p>
	 *
	 * <p>
	 * Common in config files (HOCON / INI / TOML) and HTML form checkboxes.
	 */
	ON_OFF;

	/**
	 * Formats the specified boolean using this format.
	 *
	 * <p>
	 * The return type varies per constant:
	 * <ul>
	 * 	<li>{@link #TRUE_FALSE} → {@link Boolean} (so JSON / XML emit the bare {@code true} / {@code false} token).
	 * 	<li>{@link #ZERO_ONE} → {@link Integer} (so JSON emits the bare numeric token {@code 0} / {@code 1}).
	 * 	<li>{@link #YES_NO} / {@link #Y_N} / {@link #ON_OFF} → {@link String}.
	 * </ul>
	 *
	 * <p>
	 * {@link #NOT_SET} falls through to {@link #TRUE_FALSE}.
	 *
	 * @param value The value to format.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #TRUE_FALSE}).
	 * @return The formatted wire value — a {@link Boolean}, {@link Integer}, or {@link String} depending on the format.
	 */
	public static Object format(boolean value, BooleanFormat format) {
		var fmt = format == null ? TRUE_FALSE : format;
		return switch (fmt) {
			case NOT_SET, TRUE_FALSE -> Boolean.valueOf(value);
			case ZERO_ONE -> Integer.valueOf(value ? 1 : 0);
			case YES_NO -> value ? "yes" : "no";
			case Y_N -> value ? "Y" : "N";
			case ON_OFF -> value ? "on" : "off";
		};
	}

	/**
	 * Parses the specified wire value into a {@link Boolean}.
	 *
	 * <p>
	 * Lenient parsing — accepts any of the textual shapes ({@code true}/{@code false}, {@code 1}/{@code 0},
	 * {@code yes}/{@code no}, {@code y}/{@code n}, {@code on}/{@code off}) regardless of the {@code format}
	 * hint, case-insensitive. The hint is informational only.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @param format The configured format hint (informational only — parsing is format-agnostic). Can be <jk>null</jk>.
	 * @return The parsed boolean, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value does not match any supported boolean textual shape.
	 */
	public static Boolean parse(String value, BooleanFormat format) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		return switch (s.toLowerCase()) {
			case "true", "1", "yes", "y", "on" -> Boolean.TRUE;
			case "false", "0", "no", "n", "off" -> Boolean.FALSE;
			default -> throw illegalArg("Invalid boolean value ''{0}'' for format {1}", value, format);
		};
	}

	/**
	 * Returns <jk>true</jk> if this format always emits a numeric wire token.
	 *
	 * <p>
	 * {@link #ZERO_ONE} returns <jk>true</jk>; every other constant returns <jk>false</jk>.
	 * Note that {@link #TRUE_FALSE} emits a native boolean token (not a string) in JSON / XML — that is
	 * considered a boolean-typed wire value, not a numeric one.
	 *
	 * @return <jk>true</jk> if this format emits a bare numeric wire token.
	 */
	public boolean isNumeric() {
		return this == ZERO_ONE;
	}
}
