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
import java.util.stream.*;

/**
 * Supported wire formats for {@link BitSet} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#bitSetFormat(BitSetFormat)},
 * {@link Marshalled#bitSetFormat()},
 * {@link MarshalledProp#bitSetFormat()}, and
 * {@link MarshalledConfig#bitSetFormat()} to control how {@link BitSet} values
 * are written to text-based wire formats.
 *
 * <p>
 * The default is {@link #INDICES} which emits the ascending set-bit indices as a comma-delimited token.
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link MarshalledProp#bitSetFormat() @MarshalledProp(bitSetFormat=…)} on the bean property.
 * 	<li>{@link Marshalled#bitSetFormat() @Marshalled(bitSetFormat=…)} on the bean class.
 * 	<li>{@link MarshalledConfig#bitSetFormat() @MarshalledConfig(bitSetFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder#bitSetFormat(BitSetFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.bitSetFormat</c>.
 * 	<li>The default constant ({@link #INDICES}).
 * </ol>
 *
 * <h5 class='topic'>Parsing</h5>
 *
 * <p>
 * Parsing is directed by the configured constant — a serializer and parser sharing the same
 * {@code BitSetFormat} round-trip cleanly.  The three textual shapes are ambiguous with each other
 * (a bare digit string is a valid index list, bit string, and hex token), so the configured format is
 * honored rather than auto-detected.
 */
public enum BitSetFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Ascending set-bit indices as a comma-delimited token (the default).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	0,2,5
	 * </p>
	 *
	 * <p>
	 * An empty {@link BitSet} emits the empty string.
	 */
	INDICES,

	/**
	 * Little-endian bit string — character at position <c>i</c> is <c>'1'</c> when bit <c>i</c> is set.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	101001
	 * </p>
	 *
	 * <p>
	 * The string length equals {@link BitSet#length()}; an empty {@link BitSet} emits the empty string.
	 */
	BITS,

	/**
	 * Hex-encoded little-endian byte array ({@link BitSet#toByteArray()}).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	25
	 * </p>
	 *
	 * <p>
	 * An empty {@link BitSet} emits the empty string.
	 */
	HEX;

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	/**
	 * Formats the specified {@link BitSet} using this format.
	 *
	 * <p>
	 * {@link #NOT_SET} falls through to {@link #INDICES}.
	 *
	 * @param value The value to format.  Can be <jk>null</jk>.
	 * @param format The format to use.  Can be <jk>null</jk> (treated as {@link #INDICES}).
	 * @return The formatted wire representation, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static String format(BitSet value, BitSetFormat format) {
		if (value == null)
			return null;
		var fmt = format == null ? INDICES : format;
		return switch (fmt) {
			case NOT_SET, INDICES -> value.stream().mapToObj(Integer::toString).collect(Collectors.joining(","));
			case BITS -> formatBits(value);
			case HEX -> formatHex(value.toByteArray());
		};
	}

	private static String formatBits(BitSet value) {
		var len = value.length();
		var sb = new StringBuilder(len);
		for (var i = 0; i < len; i++)
			sb.append(value.get(i) ? '1' : '0');
		return sb.toString();
	}

	private static String formatHex(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 2);
		for (var b : bytes) {
			sb.append(HEX_CHARS[(b >> 4) & 0xF]);
			sb.append(HEX_CHARS[b & 0xF]);
		}
		return sb.toString();
	}

	/**
	 * Parses the specified wire value into a {@link BitSet}.
	 *
	 * <p>
	 * Parsing is directed by the {@code format} hint (defaulting to {@link #INDICES}); the three textual
	 * shapes are mutually ambiguous, so the configured format is honored rather than auto-detected.
	 *
	 * @param value The wire value.  Can be <jk>null</jk> or blank.
	 * @param format The configured format hint.  Can be <jk>null</jk> (treated as {@link #INDICES}).
	 * @return The parsed {@link BitSet}, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 * @throws IllegalArgumentException If the value does not match the configured textual shape.
	 */
	public static BitSet parse(String value, BitSetFormat format) {
		if (value == null)
			return null;
		var s = value.trim();
		var fmt = format == null ? INDICES : format;
		var bs = new BitSet();
		if (s.isEmpty())
			return bs;
		try {
			switch (fmt) {
				case NOT_SET, INDICES -> {
					for (var tok : s.split(","))
						bs.set(Integer.parseInt(tok.trim()));
				}
				case BITS -> {
					for (var i = 0; i < s.length(); i++)
						if (s.charAt(i) == '1')
							bs.set(i);
				}
				case HEX -> {
					return BitSet.valueOf(parseHex(s));
				}
			}
		} catch (IllegalArgumentException e) {
			throw iaex("Invalid BitSet value '%s' for format %s: %s", value, fmt, e.getMessage());
		}
		return bs;
	}

	private static byte[] parseHex(String s) {
		if ((s.length() & 1) != 0)
			throw iaex("Hex string must have an even number of characters");
		var out = new byte[s.length() / 2];
		for (var i = 0; i < out.length; i++) {
			var hi = Character.digit(s.charAt(i * 2), 16);
			var lo = Character.digit(s.charAt(i * 2 + 1), 16);
			if (hi < 0 || lo < 0)
				throw iaex("Invalid hex character");
			out[i] = (byte) ((hi << 4) | lo);
		}
		return out;
	}

	/**
	 * Returns <jk>true</jk> if this format is numeric on the wire.
	 *
	 * <p>
	 * Always <jk>false</jk> — every constant emits a textual representation.
	 *
	 * @return <jk>false</jk>.
	 */
	@SuppressWarnings({
		"static-method", // Kept as an instance method for polymorphic-by-convention symmetry with the other Format classes (BigNumberFormat, FloatFormat, DurationFormat, etc.) where isNumeric() depends on the enum constant.
		"java:S3400"     // Same rationale — must remain an instance method, not a constant, to match the cross-Format API contract.
	})
	public boolean isNumeric() {
		return false;
	}
}
