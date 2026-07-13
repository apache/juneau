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
 * Supported wire formats for {@link UUID} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#uuidFormat(UuidFormat)},
 * {@link org.apache.juneau.marshall.Marshalled#uuidFormat()},
 * {@link org.apache.juneau.marshall.MarshalledProp#uuidFormat()}, and
 * {@link org.apache.juneau.marshall.MarshalledConfig#uuidFormat()} to control how {@link UUID} values
 * are written to text-based wire formats.
 *
 * <p>
 * The default is {@link #STANDARD} which preserves the historical wire output produced by
 * {@link UUID#toString()} (8-4-4-4-12 hyphenated form).
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link org.apache.juneau.marshall.MarshalledProp#uuidFormat() @MarshalledProp(uuidFormat=…)} on the bean property.
 * 	<li>{@link org.apache.juneau.marshall.Marshalled#uuidFormat() @Marshalled(uuidFormat=…)} on the bean class.
 * 	<li>{@link org.apache.juneau.marshall.MarshalledConfig#uuidFormat() @MarshalledConfig(uuidFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder#uuidFormat(UuidFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.uuidFormat</c>.
 * 	<li>The default constant ({@link #STANDARD}).
 * </ol>
 *
 * <h5 class='topic'>Parser leniency</h5>
 *
 * <p>
 * Parsers SHALL accept any wire shape that the corresponding serializer can produce, regardless of the
 * parser-side {@code UuidFormat} setting. Each constant accepts all three textual shapes
 * ({@link #STANDARD} / {@link #NO_DASHES} / {@link #URN}) — the configured constant is informational and
 * is consulted only as a hint.
 *
 * <h5 class='topic'>Binary serializers</h5>
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Prototext) emit native 16-byte binary regardless of the
 * configured {@code UuidFormat}; this setting only affects text-based serializers. BSON in particular
 * uses {@code binData} subtype 4 (UUID).
 */
public enum UuidFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * 8-4-4-4-12 hyphenated form (the default).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	550e8400-e29b-41d4-a716-446655440000
	 * </p>
	 *
	 * <p>
	 * Round-trips through {@link UUID#toString()} / {@link UUID#fromString(String)}.
	 */
	STANDARD,

	/**
	 * Compact 32-hex-character form (no hyphens).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	550e8400e29b41d4a716446655440000
	 * </p>
	 *
	 * <p>
	 * Useful for URL paths, database column keys, and other compact-token use cases where the standard
	 * hyphens add noise.
	 */
	NO_DASHES,

	/**
	 * RFC 4122 URN namespace form.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	urn:uuid:550e8400-e29b-41d4-a716-446655440000
	 * </p>
	 *
	 * <p>
	 * Surfaces in JSON-LD, SOAP, and some semantic-web payloads.
	 */
	URN;

	private static final String URN_PREFIX = "urn:uuid:";

	/**
	 * Formats the specified {@link UUID} using this format.
	 *
	 * <p>
	 * {@link #NOT_SET} falls through to {@link #STANDARD}.
	 *
	 * @param value The value to format.  Can be <jk>null</jk>.
	 * @return The formatted wire representation, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static String format(UUID value, UuidFormat format) {
		if (value == null)
			return null;
		var fmt = format == null ? STANDARD : format;
		return switch (fmt) {
			case NOT_SET, STANDARD -> value.toString();
			case NO_DASHES -> value.toString().replace("-", "");
			case URN -> URN_PREFIX + value;
		};
	}

	/**
	 * Parses the specified wire value into a {@link UUID}.
	 *
	 * <p>
	 * Lenient parsing — accepts any of the three textual shapes ({@link #STANDARD} / {@link #NO_DASHES} /
	 * {@link #URN}) regardless of the {@code format} hint.  The hint is informational only.
	 *
	 * @param value The wire value.  Can be <jk>null</jk> or blank.
	 * @param format The configured format hint (informational only — parsing is format-agnostic).  Can be <jk>null</jk>.
	 * @return The parsed {@link UUID}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value does not match any supported UUID textual shape.
	 */
	public static UUID parse(String value, UuidFormat format) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		// Strip URN prefix when present (case-insensitive per RFC 4122).
		if (s.length() > URN_PREFIX.length() && s.regionMatches(true, 0, URN_PREFIX, 0, URN_PREFIX.length()))
			s = s.substring(URN_PREFIX.length());
		// Insert dashes if the input is the 32-hex-character compact form.
		if (s.length() == 32 && s.indexOf('-') < 0)
			s = s.substring(0, 8) + '-' + s.substring(8, 12) + '-' + s.substring(12, 16) + '-' + s.substring(16, 20) + '-' + s.substring(20);
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			throw iaex("Invalid UUID value ''{0}'' for format {1}: {2}", value, format, e.getMessage());
		}
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
