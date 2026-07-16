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

import java.util.Locale;

/**
 * Supported wire formats for non-finite {@link Float} / {@link Double} values
 * ({@link Double#NaN NaN}, {@link Double#POSITIVE_INFINITY +Infinity}, {@link Double#NEGATIVE_INFINITY -Infinity}).
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#floatFormat(FloatFormat)},
 * {@link Marshalled#floatFormat()},
 * {@link MarshalledProp#floatFormat()}, and
 * {@link MarshalledConfig#floatFormat()} to control how non-finite
 * floating-point values are written to text-based wire formats.  Finite values are unaffected and ride
 * the natural bare-numeric-token wire form for the underlying serializer.
 *
 * <p>
 * The default is {@link #NaN_AS_NULL} which is JSON-spec-compliant per RFC 8259 §6 (non-finite floats
 * are not valid JSON tokens; emitting {@code null} is the closest valid representation).
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link MarshalledProp#floatFormat() @MarshalledProp(floatFormat=…)} on the bean property.
 * 	<li>{@link Marshalled#floatFormat() @Marshalled(floatFormat=…)} on the bean class.
 * 	<li>{@link MarshalledConfig#floatFormat() @MarshalledConfig(floatFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder#floatFormat(FloatFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.floatFormat</c>.
 * 	<li>The default constant ({@link #NaN_AS_NULL}).
 * </ol>
 *
 * <h5 class='topic'>Parser leniency</h5>
 *
 * <p>
 * Parsers SHALL accept all of the following wire shapes regardless of the parser-side
 * {@code FloatFormat} setting:
 * <ul>
 * 	<li>Bare numeric tokens including the non-standard {@code NaN} / {@code Infinity} / {@code -Infinity} tokens
 * 		(Juneau's lenient parser accepts these by default).
 * 	<li>Quoted-string forms — {@code "NaN"}, {@code "Infinity"}, {@code "-Infinity"} (case-insensitive).
 * 	<li>{@code null} — reconstructed as {@link Double#NaN} when reading into a primitive {@code double} / {@code float};
 * 		preserved as {@code null} when reading into a boxed {@link Double} / {@link Float}.
 * </ul>
 *
 * <h5 class='topic'>JSON-spec compliance</h5>
 *
 * <p>
 * {@link #NaN_AS_NUMBER} is <b>non-standard JSON</b> per RFC 8259 §6 — JSON does not permit {@code NaN} /
 * {@code Infinity} / {@code -Infinity} tokens. Juneau's own JSON parser accepts these in lenient mode,
 * but strict-mode consumers (e.g. some browser {@code JSON.parse} pipelines) will reject them. Use only
 * when you control both ends of the wire.
 *
 * <h5 class='topic'>Binary serializers</h5>
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Prototext / Parquet) emit native IEEE-754 floats regardless
 * of this setting; native non-finite representations exist in every supported binary format (BSON
 * {@code double} / {@code decimal128}; CBOR's tag 22 / 23 / 24 for special floats; MsgPack {@code float64}).
 * This setting only affects text-based serializers.
 */
@SuppressWarnings({
	"java:S115" // NaN_AS_* enum constants use the IEEE 754 abbreviation 'NaN' which includes lowercase letters; intentional
})
public enum FloatFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Emit {@code null} for {@code NaN} / {@code ±Infinity} (the default).
	 *
	 * <p>
	 * <b>JSON-spec compliant.</b> Loses the ability to distinguish {@code NaN} from
	 * {@code +Infinity} from {@code -Infinity} on the wire — all three become {@code null}. Round-trips
	 * back to {@link Double#NaN} when reading into a primitive {@code double}; into a boxed {@link Double}
	 * field, {@code null} stays {@code null}.
	 */
	NaN_AS_NULL,

	/**
	 * Emit the quoted string {@code "NaN"} / {@code "Infinity"} / {@code "-Infinity"}.
	 *
	 * <p>
	 * <b>Round-trip preserving.</b> The parser unambiguously decodes the string back to the original
	 * non-finite value.
	 */
	NaN_AS_STRING,

	/**
	 * Emit the bare {@code NaN} / {@code Infinity} / {@code -Infinity} token.
	 *
	 * <p>
	 * <b>Non-standard JSON</b> per RFC 8259 §6. Accepted by Juneau's lenient parser and V8 / several
	 * Python libs; rejected by strict consumers. Use only when you control both ends of the wire.
	 */
	NaN_AS_NUMBER,

	/**
	 * Throw {@link IllegalArgumentException} when a non-finite value is encountered.
	 *
	 * <p>
	 * <b>Fail-fast</b> for data-cleanliness pipelines where a {@code NaN} indicates upstream corruption.
	 */
	NaN_AS_ERROR;

	/**
	 * Formats the specified {@code double} using this format.
	 *
	 * <p>
	 * Finite values always return the boxed {@link Double} so text serializers emit the bare numeric
	 * token. Non-finite values are dispatched per the constant:
	 * <ul>
	 * 	<li>{@link #NaN_AS_NULL} → {@code null}.
	 * 	<li>{@link #NaN_AS_STRING} → {@code "NaN"} / {@code "Infinity"} / {@code "-Infinity"}.
	 * 	<li>{@link #NaN_AS_NUMBER} → boxed {@link Double} (non-finite); text serializers emit the bare token.
	 * 	<li>{@link #NaN_AS_ERROR} → throws {@link IllegalArgumentException}.
	 * </ul>
	 *
	 * <p>
	 * {@link #NOT_SET} / {@code null} format are treated as {@link #NaN_AS_NULL}.
	 *
	 * @param value The value to format.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #NaN_AS_NULL}).
	 * @return The formatted value — a {@link Double} for finite or {@code NaN_AS_NUMBER}, a {@link String}
	 *   for {@code NaN_AS_STRING}, or {@code null} for non-finite under {@code NaN_AS_NULL}.
	 * @throws IllegalArgumentException If {@code format} is {@link #NaN_AS_ERROR} and the value is non-finite.
	 */
	public static Object format(double value, FloatFormat format) {
		if (Double.isFinite(value))
			return Double.valueOf(value);
		return formatNonFinite(value, format);
	}

	/**
	 * Formats the specified {@code float} using this format.
	 *
	 * <p>
	 * Finite values return the boxed {@link Float}. Non-finite values follow the same rules as
	 * {@link #format(double, FloatFormat)} except that {@link #NaN_AS_NUMBER} returns the boxed
	 * {@link Float} (not {@link Double}).
	 *
	 * @param value The value to format.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #NaN_AS_NULL}).
	 * @return The formatted value — a {@link Float} for finite or {@code NaN_AS_NUMBER}, a {@link String}
	 *   for {@code NaN_AS_STRING}, or {@code null} for non-finite under {@code NaN_AS_NULL}.
	 * @throws IllegalArgumentException If {@code format} is {@link #NaN_AS_ERROR} and the value is non-finite.
	 */
	public static Object format(float value, FloatFormat format) {
		if (Float.isFinite(value))
			return Float.valueOf(value);
		var fmt = format == null ? NaN_AS_NULL : format;
		return switch (fmt) {
			case NOT_SET, NaN_AS_NULL -> null;
			case NaN_AS_STRING -> nonFiniteTokenFloat(value);
			case NaN_AS_NUMBER -> Float.valueOf(value);
			case NaN_AS_ERROR -> throw iaex("Non-finite float value %s rejected by FloatFormat.NaN_AS_ERROR", value);
		};
	}

	private static Object formatNonFinite(double value, FloatFormat format) {
		var fmt = format == null ? NaN_AS_NULL : format;
		return switch (fmt) {
			case NOT_SET, NaN_AS_NULL -> null;
			case NaN_AS_STRING -> nonFiniteToken(value);
			case NaN_AS_NUMBER -> Double.valueOf(value);
			case NaN_AS_ERROR -> throw iaex("Non-finite double value %s rejected by FloatFormat.NaN_AS_ERROR", value);
		};
	}

	private static String nonFiniteToken(double value) {
		if (Double.isNaN(value))
			return "NaN";
		return value > 0 ? "Infinity" : "-Infinity";
	}

	private static String nonFiniteTokenFloat(float value) {
		if (Float.isNaN(value))
			return "NaN";
		return value > 0 ? "Infinity" : "-Infinity";
	}

	/**
	 * Parses the specified wire value into a non-finite-aware {@link Double} or {@link Float}.
	 *
	 * <p>
	 * Lenient parsing — accepts bare numeric, quoted-string {@code "NaN"} / {@code "Infinity"} /
	 * {@code "-Infinity"} (case-insensitive), and unquoted {@code NaN} / {@code Infinity} / {@code -Infinity}
	 * tokens regardless of the {@code format} hint. The hint is informational only.
	 *
	 * @param <T> The target type ({@link Double} or {@link Float}).
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @param format The configured format hint (informational only — parsing is format-agnostic). Can be <jk>null</jk>.
	 * @param targetType The desired return type — {@link Double} or {@link Float}.
	 * @return The parsed value, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value cannot be parsed or {@code targetType} is unsupported.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: parameterized by caller.
	})
	public static <T extends Number> T parse(String value, FloatFormat format, Class<T> targetType) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		// Strip surrounding quotes if present (lenient parsing — quoted-string and bare-numeric both accepted).
		if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0))
			s = s.substring(1, s.length() - 1).trim();
		try {
			if (Double.class.equals(targetType) || double.class.equals(targetType))
				return (T) Double.valueOf(parseDouble(s));
			if (Float.class.equals(targetType) || float.class.equals(targetType))
				return (T) Float.valueOf(parseFloat(s));
			throw iaex("Unsupported FloatFormat target type: %s", targetType.getName());
		} catch (NumberFormatException e) {
			throw iaex("Invalid float value '%s' for format %s: %s", value, format, e.getMessage());
		}
	}

	private static double parseDouble(String s) {
		var lower = s.toLowerCase(Locale.ROOT);
		return switch (lower) {
			case "nan" -> Double.NaN;
			case "infinity", "+infinity", "inf", "+inf" -> Double.POSITIVE_INFINITY;
			case "-infinity", "-inf" -> Double.NEGATIVE_INFINITY;
			default -> Double.parseDouble(s);
		};
	}

	private static float parseFloat(String s) {
		var lower = s.toLowerCase(Locale.ROOT);
		return switch (lower) {
			case "nan" -> Float.NaN;
			case "infinity", "+infinity", "inf", "+inf" -> Float.POSITIVE_INFINITY;
			case "-infinity", "-inf" -> Float.NEGATIVE_INFINITY;
			default -> Float.parseFloat(s);
		};
	}

	/**
	 * Returns <jk>true</jk> if this format always emits a bare numeric wire token for non-finite values.
	 *
	 * <p>
	 * Only {@link #NaN_AS_NUMBER} returns <jk>true</jk>. {@link #NaN_AS_NULL} may emit {@code null};
	 * {@link #NaN_AS_STRING} emits a quoted string; {@link #NaN_AS_ERROR} throws. Finite values are
	 * always emitted as bare numeric tokens regardless of this setting.
	 *
	 * @return <jk>true</jk> if this format emits a bare numeric wire token for non-finite values.
	 */
	public boolean isNumeric() {
		return this == NaN_AS_NUMBER;
	}
}
