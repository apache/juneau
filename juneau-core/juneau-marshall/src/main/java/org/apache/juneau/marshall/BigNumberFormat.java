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

import java.math.*;

/**
 * Supported wire formats for {@link BigInteger} and {@link BigDecimal} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#bigNumberFormat(BigNumberFormat)},
 * {@link Marshalled#bigNumberFormat()},
 * {@link MarshalledProp#bigNumberFormat()}, and
 * {@link MarshalledConfig#bigNumberFormat()} to control how
 * {@link BigInteger} / {@link BigDecimal} values are written to text-based wire formats.
 *
 * <p>
 * The default is {@link #NUMBER} which preserves the historical wire output (a bare numeric token).
 *
 * <h5 class='topic'>JS-interop motivation</h5>
 *
 * <p>
 * JavaScript's {@code Number.MAX_SAFE_INTEGER} is {@code 2^53 − 1} (= {@code 9007199254740991}).
 * Values outside that range silently lose precision when parsed by a JavaScript client. {@link #STRING}
 * forces a quoted-string representation that round-trips losslessly. {@link #AUTO} picks {@link #NUMBER}
 * for values that are JS-safe and {@link #STRING} for everything else — the typical "safe default" for
 * services that may be consumed by both JVM and browser clients.
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link MarshalledProp#bigNumberFormat() @MarshalledProp(bigNumberFormat=…)} on the bean property.
 * 	<li>{@link Marshalled#bigNumberFormat() @Marshalled(bigNumberFormat=…)} on the bean class.
 * 	<li>{@link MarshalledConfig#bigNumberFormat() @MarshalledConfig(bigNumberFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder#bigNumberFormat(BigNumberFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.bigNumberFormat</c>.
 * 	<li>The default constant ({@link #NUMBER}).
 * </ol>
 *
 * <h5 class='topic'>Parser leniency</h5>
 *
 * <p>
 * Parsers SHALL accept both bare numeric and quoted-string input regardless of the parser-side
 * {@code BigNumberFormat} setting. The configured constant is informational only.
 *
 * <h5 class='topic'>Binary serializers</h5>
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Prototext) emit native numeric types regardless of the
 * configured {@code BigNumberFormat}; this setting only affects text-based serializers. BSON has
 * {@code decimal128} for exact decimal values; CBOR / MsgPack handle big-integer values via
 * tag&nbsp;2 / tag&nbsp;3 with string fallback.
 */
public enum BigNumberFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Bare numeric token (the default).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	12345678901234567890
	 * </p>
	 *
	 * <p>
	 * This is the historical wire output and the natural form for server-to-server JVM clients.
	 */
	NUMBER,

	/**
	 * Quoted-string form for lossless precision.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	"12345678901234567890"
	 * </p>
	 *
	 * <p>
	 * Round-trips losslessly through any JSON / YAML / etc. parser, including JavaScript clients which
	 * would otherwise truncate to a 64-bit double.
	 */
	STRING,

	/**
	 * Hybrid — emit {@link #NUMBER} when the value is JS-safe (fits in {@code ±(2^53 − 1)}), otherwise
	 * emit {@link #STRING}.
	 *
	 * <p>
	 * For {@link BigDecimal} values, any non-zero scale forces {@link #STRING} (a fractional value cannot
	 * be safely represented in a JS double regardless of magnitude).
	 *
	 * <p>
	 * The "safe default" for services that may be consumed by both JVM and browser clients.
	 */
	AUTO;

	/** {@link Number#MAX_SAFE_INTEGER} equivalent — largest integer that round-trips through a JS double. */
	private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;
	private static final BigInteger BIG_JS_MAX_SAFE = BigInteger.valueOf(JS_MAX_SAFE_INTEGER);
	private static final BigInteger BIG_JS_MIN_SAFE = BigInteger.valueOf(-JS_MAX_SAFE_INTEGER);

	/**
	 * Formats the specified {@link BigInteger} using this format.
	 *
	 * <p>
	 * Returns either a {@link Number} (when {@link #NUMBER} or {@link #AUTO} produces a JS-safe value) or
	 * a {@link String} (when {@link #STRING} or {@link #AUTO} produces an out-of-range value). Callers
	 * should branch on the runtime type:
	 * <ul>
	 * 	<li>{@link Number} → emit as a bare numeric wire token.
	 * 	<li>{@link String} → emit as a quoted string.
	 * </ul>
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #NUMBER}).
	 * @return The formatted value (a {@link BigInteger} for numeric output, a {@link String} for string output),
	 *   or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static Object format(BigInteger value, BigNumberFormat format) {
		if (value == null)
			return null;
		var fmt = format == null ? NUMBER : format;
		return switch (fmt) {
			case NOT_SET, NUMBER -> value;
			case STRING -> value.toString();
			case AUTO -> isJsSafe(value) ? (Object) value : value.toString();
		};
	}

	/**
	 * Formats the specified {@link BigDecimal} using this format.
	 *
	 * <p>
	 * Returns either a {@link Number} (when {@link #NUMBER} or {@link #AUTO} produces a JS-safe value) or
	 * a {@link String} (when {@link #STRING} or {@link #AUTO} produces an out-of-range value).
	 *
	 * <p>
	 * For {@link #AUTO}: any non-zero-scale {@link BigDecimal} (i.e. has a fractional part) forces
	 * {@link #STRING} since fractional values cannot be safely represented in a JS double regardless of
	 * magnitude.  Zero-scale values are tested against the integer {@code ±(2^53 − 1)} bound.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #NUMBER}).
	 * @return The formatted value (a {@link BigDecimal} for numeric output, a {@link String} for string output),
	 *   or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static Object format(BigDecimal value, BigNumberFormat format) {
		if (value == null)
			return null;
		var fmt = format == null ? NUMBER : format;
		return switch (fmt) {
			case NOT_SET, NUMBER -> value;
			case STRING -> value.toPlainString();
			case AUTO -> {
				if (value.scale() > 0)
					yield value.toPlainString();
				yield isJsSafe(value.toBigInteger()) ? (Object) value : value.toPlainString();
			}
		};
	}

	/**
	 * Parses the specified wire value into a {@link BigInteger} or {@link BigDecimal}.
	 *
	 * <p>
	 * Accepts both bare numeric and quoted-string input regardless of the {@code format} hint. The hint is
	 * informational only.
	 *
	 * @param <T> The target type ({@link BigInteger} or {@link BigDecimal}).
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @param format The configured format (informational only — parsing is format-agnostic). Can be <jk>null</jk>.
	 * @param targetType The desired return type — {@link BigInteger} or {@link BigDecimal}.
	 * @return The parsed value, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value cannot be parsed as a number or {@code targetType} is unsupported.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: parameterized by caller.
	})
	public static <T extends Number> T parse(String value, BigNumberFormat format, Class<T> targetType) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		// Strip surrounding quotes if present (lenient parsing — quoted-string and bare-numeric both accepted).
		if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0))
			s = s.substring(1, s.length() - 1).trim();
		try {
			if (BigInteger.class.equals(targetType))
				return (T) new BigInteger(s);
			if (BigDecimal.class.equals(targetType))
				return (T) new BigDecimal(s);
			throw iaex("Unsupported BigNumberFormat target type: %s", targetType.getName());
		} catch (NumberFormatException e) {
			throw iaex("Invalid big-number value '%s' for format %s: %s", value, format, e.getMessage());
		}
	}

	/**
	 * Returns <jk>true</jk> if this format always emits a numeric wire value.
	 *
	 * <p>
	 * {@link #NUMBER} returns <jk>true</jk>; {@link #STRING} returns <jk>false</jk>. {@link #AUTO}
	 * returns <jk>false</jk> conservatively — its {@code format(...)} return type varies per value, so
	 * serializers should branch on the actual runtime type returned by
	 * {@link #format(BigInteger, BigNumberFormat)} / {@link #format(BigDecimal, BigNumberFormat)} rather
	 * than this hint.
	 *
	 * @return <jk>true</jk> if this format emits a bare numeric wire value.
	 */
	public boolean isNumeric() {
		return this == NUMBER;
	}

	private static boolean isJsSafe(BigInteger value) {
		return value.compareTo(BIG_JS_MIN_SAFE) >= 0 && value.compareTo(BIG_JS_MAX_SAFE) <= 0;
	}
}
