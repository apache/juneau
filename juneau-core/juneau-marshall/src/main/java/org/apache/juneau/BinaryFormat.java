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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

/**
 * Supported wire formats for <code><jk>byte</jk>[]</code> values and stream-based serialization output.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder#binaryFormat(BinaryFormat)},
 * {@link org.apache.juneau.annotation.Marshalled#binaryFormat()},
 * {@link org.apache.juneau.annotation.MarshalledProp#binaryFormat()}, and
 * {@link org.apache.juneau.annotation.MarshalledConfig#binaryFormat()} to control how
 * <code><jk>byte</jk>[]</code> values are written to and read from the wire on text formats, as well as
 * the encoding used by stream-based serializers when converting their byte output to a string.
 *
 * <p>
 * The default is {@link #NOT_SET} — leaves {@code byte[]} handling to the surrounding serializer / parser
 * (e.g. textual serializers fall back to the natural language-level array representation, and binary
 * serializers emit native bytes).  Callers wanting a textual encoding on the wire should set this to
 * {@link #HEX}, {@link #BASE64}, {@link #SPACED_HEX}, or {@link #BASE64_URL}.
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Proto) emit native bytes regardless of the configured
 * {@code BinaryFormat}; this setting only affects text-based serializers and the
 * {@code OutputStreamSerializerSession#serializeToString(Object)} stream-to-string convenience.
 */
public enum BinaryFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * Spaced-hex.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	12 34 56 78 90 AB CD EF
	 * </p>
	 */
	SPACED_HEX,

	/**
	 * Hex.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	1234567890ABCDEF
	 * </p>
	 */
	HEX,

	/** Standard {@link Base64} encoding. */
	BASE64,

	/**
	 * URL-safe {@link Base64} encoding (RFC 4648 §5).
	 *
	 * <p>
	 * Uses {@code -} and {@code _} in place of {@code +} and {@code /}. Padding ({@code =}) is omitted on
	 * the wire but accepted on input.
	 */
	BASE64_URL;

	/**
	 * Formats the specified value using this format.
	 *
	 * @param value The value to format.  Can be <jk>null</jk>.
	 * @return The formatted string, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public String format(byte[] value) {
		if (value == null)
			return null;
		return switch (this) {
			case NOT_SET, BASE64 -> base64Encode(value);
			case BASE64_URL -> Base64.getUrlEncoder().withoutPadding().encodeToString(value);
			case HEX -> toHex(value);
			case SPACED_HEX -> toSpacedHex(value);
		};
	}

	/**
	 * Parses the specified wire value using this format.
	 *
	 * <p>
	 * The parser accepts any of the formats produced by {@link #format(byte[])} regardless of the
	 * configured constant, mirroring the format-agnostic parser convention used by other {@code <type>Format}
	 * enums in this package.  The configured format is consulted only as a hint for ambiguous input.
	 *
	 * @param value The wire value.  Can be <jk>null</jk> or blank.
	 * @return The parsed bytes, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 */
	public byte[] parse(String value) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return new byte[0];
		// Honor the configured format constant for BASE64_URL: the URL-safe encoder emits without padding,
		// so a non-3-aligned payload that happens not to contain `-`/`_` (because the encoded chars all fall
		// in the base64-alphabet intersection) would otherwise be misrouted to the standard decoder, which
		// rejects missing padding.  The URL-safe decoder accepts both with-padding and without-padding input.
		if (this == BASE64_URL) {
			try {
				return Base64.getUrlDecoder().decode(s);
			} catch (IllegalArgumentException e) {
				throw illegalArg("Invalid binary value ''{0}'' for format {1}: {2}", s, this, e.getMessage());
			}
		}
		// Format-agnostic parsing: sniff the wire shape and decode accordingly, regardless of the
		// configured format on `this`.  A space anywhere implies spaced-hex; a `-`/`_` implies url-safe
		// base64; otherwise pure hex shape (even-length, hex-only chars) implies hex.  Anything left
		// is standard base64.
		if (s.indexOf(' ') >= 0)
			return fromSpacedHex(s);
		if (s.indexOf('-') >= 0 || s.indexOf('_') >= 0)
			return Base64.getUrlDecoder().decode(s);
		if (isHex(s))
			return fromHex(s);
		try {
			return base64Decode(s);
		} catch (IllegalArgumentException e) {
			throw illegalArg("Invalid binary value ''{0}'' for format {1}: {2}", s, this, e.getMessage());
		}
	}

	private static boolean isHex(String s) {
		if (s.isEmpty() || (s.length() & 1) == 1)
			return false;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (! ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
				return false;
		}
		return true;
	}
}
