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

import java.util.*;

/**
 * Supported wire formats for {@link Currency} values.
 *
 * <p>
 * Used by {@link MarshallingContext.Builder<?>#currencyFormat(CurrencyFormat)},
 * {@link org.apache.juneau.annotation.Marshalled#currencyFormat()},
 * {@link org.apache.juneau.annotation.MarshalledProp#currencyFormat()}, and
 * {@link org.apache.juneau.annotation.MarshalledConfig#currencyFormat()} to control how {@link Currency}
 * values are written to text-based wire formats.
 *
 * <p>
 * The default is {@link #ISO_CODE} which preserves the historical wire output ({@link Currency#toString()} /
 * {@link Currency#getCurrencyCode()}).
 *
 * <h5 class='topic'>Locale composability</h5>
 *
 * <p>
 * {@link #SYMBOL} and {@link #DISPLAY_NAME} are locale-sensitive — they consult the supplied {@link Locale}
 * (typically the {@link MarshallingContext#getLocale()} resolved through the {@link MarshallingSession}).
 * {@link #ISO_CODE} ignores the locale.
 *
 * <h5 class='topic'>Round-trip caveat</h5>
 *
 * <p>
 * <b>Round-trip is strictly guaranteed only for {@link #ISO_CODE}.</b>  {@code SYMBOL} parsing of a token
 * like {@code "$"} could resolve to any of USD / CAD / AUD / MXN / HKD / SGD / NZD depending on the
 * locale, and {@code DISPLAY_NAME} parsing is even less reliable ({@code "Dollar"} matches at least eight
 * ISO codes). For machine-to-machine wires, prefer {@link #ISO_CODE}.
 *
 * <h5 class='topic'>Precedence (highest to lowest)</h5>
 * <ol>
 * 	<li>{@link org.apache.juneau.annotation.MarshalledProp#currencyFormat() @MarshalledProp(currencyFormat=…)} on the bean property.
 * 	<li>{@link org.apache.juneau.annotation.Marshalled#currencyFormat() @Marshalled(currencyFormat=…)} on the bean class.
 * 	<li>{@link org.apache.juneau.annotation.MarshalledConfig#currencyFormat() @MarshalledConfig(currencyFormat=…)} on
 * 		<code><ja>@Rest</ja></code>-annotated classes / methods.
 * 	<li>Programmatic {@link MarshallingContext.Builder<?>#currencyFormat(CurrencyFormat)}.
 * 	<li>Environment variable <c>MarshallingContext.currencyFormat</c>.
 * 	<li>The default constant ({@link #ISO_CODE}).
 * </ol>
 *
 * <h5 class='topic'>Parser leniency</h5>
 *
 * <p>
 * Parsers SHALL try {@link Currency#getInstance(String)} first regardless of the parser-side
 * {@code CurrencyFormat} setting — this handles the {@link #ISO_CODE} shape unambiguously. For
 * {@link #SYMBOL} / {@link #DISPLAY_NAME}, the parser falls back to a best-effort iterate-and-match scan
 * against {@link Currency#getAvailableCurrencies()}.
 *
 * <h5 class='topic'>Binary serializers</h5>
 *
 * <p>
 * Binary serializers (BSON / CBOR / MsgPack / Proto / Parquet) emit a UTF-8 string regardless of this
 * setting — there is no native {@code Currency} wire type in any supported binary format. The choice of
 * {@code ISO_CODE} vs {@code SYMBOL} vs {@code DISPLAY_NAME} is purely about the textual content of that
 * string.
 */
public enum CurrencyFormat {

	/** Sentinel meaning "no value configured" — falls through to the next-higher precedence level. */
	NOT_SET,

	/**
	 * ISO 4217 currency code (the default).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode'>
	 * 	USD
	 * 	EUR
	 * 	JPY
	 * </p>
	 *
	 * <p>
	 * Locale-independent. Round-trips through {@link Currency#getInstance(String)}.
	 */
	ISO_CODE,

	/**
	 * Currency symbol per {@link Currency#getSymbol(Locale)}.
	 *
	 * <h5 class='figure'>Example (locale {@code en_US}):</h5>
	 * <p class='bcode'>
	 * 	$
	 * 	€
	 * 	¥
	 * </p>
	 *
	 * <p>
	 * <b>Locale-sensitive.</b> The symbol is resolved against the supplied locale.  Round-trip is
	 * best-effort — {@code "$"} is shared by USD / CAD / AUD / MXN / HKD / SGD / NZD; the parser prefers
	 * the locale's default currency on ambiguity and throws when no match is found.
	 */
	SYMBOL,

	/**
	 * Localized display name per {@link Currency#getDisplayName(Locale)}.
	 *
	 * <h5 class='figure'>Example (locale {@code en_US}):</h5>
	 * <p class='bcode'>
	 * 	US Dollar
	 * 	Euro
	 * 	Japanese Yen
	 * </p>
	 *
	 * <p>
	 * <b>Locale-sensitive and even less round-trip-safe than {@link #SYMBOL}.</b>  Use only for
	 * user-facing display contexts.
	 */
	DISPLAY_NAME;

	/**
	 * Formats the specified {@link Currency} using this format.
	 *
	 * <p>
	 * {@link #NOT_SET} falls through to {@link #ISO_CODE}. {@code null} locale is treated as
	 * {@link Locale#ROOT}.
	 *
	 * @param value The value to format. Can be <jk>null</jk>.
	 * @param format The configured format. Can be <jk>null</jk> (treated as {@link #ISO_CODE}).
	 * @param locale The locale used by {@link #SYMBOL} / {@link #DISPLAY_NAME}. Can be <jk>null</jk>
	 *   (treated as {@link Locale#ROOT}).
	 * @return The formatted wire representation, or <jk>null</jk> if {@code value} is <jk>null</jk>.
	 */
	public static String format(Currency value, CurrencyFormat format, Locale locale) {
		if (value == null)
			return null;
		var fmt = format == null ? ISO_CODE : format;
		var loc = locale == null ? Locale.ROOT : locale;
		return switch (fmt) {
			case NOT_SET, ISO_CODE -> value.getCurrencyCode();
			case SYMBOL -> value.getSymbol(loc);
			case DISPLAY_NAME -> value.getDisplayName(loc);
		};
	}

	/**
	 * Parses the specified wire value into a {@link Currency}.
	 *
	 * <p>
	 * Lenient parsing — always tries {@link Currency#getInstance(String)} first (handles {@link #ISO_CODE}
	 * unambiguously).  For {@link #SYMBOL} / {@link #DISPLAY_NAME} the parser falls back to a best-effort
	 * iterate-and-match scan against {@link Currency#getAvailableCurrencies()}; when multiple currencies
	 * match a symbol, prefers the locale's default currency.
	 *
	 * @param value The wire value. Can be <jk>null</jk> or blank.
	 * @param format The configured format hint. Can be <jk>null</jk> (treated as {@link #ISO_CODE}).
	 * @param locale The locale used to disambiguate {@link #SYMBOL} / {@link #DISPLAY_NAME} matches.
	 *   Can be <jk>null</jk> (treated as {@link Locale#ROOT}).
	 * @return The parsed {@link Currency}, or <jk>null</jk> if {@code value} is <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the value cannot be resolved to a unique currency.
	 */
	public static Currency parse(String value, CurrencyFormat format, Locale locale) {
		if (value == null)
			return null;
		var s = value.trim();
		if (s.isEmpty())
			return null;
		var loc = locale == null ? Locale.ROOT : locale;
		var fmt = format == null ? ISO_CODE : format;
		// Try ISO code first — works unambiguously for the ISO_CODE shape and many SYMBOL / DISPLAY_NAME inputs.
		try {
			return Currency.getInstance(s);
		} catch (@SuppressWarnings("unused") IllegalArgumentException ignored) {
			// Fall through to symbol / display-name scan.
		}
		return switch (fmt) {
			case NOT_SET, ISO_CODE -> throw illegalArg("Invalid currency code ''{0}''", value);
			case SYMBOL -> findBySymbol(s, loc, value);
			case DISPLAY_NAME -> findByDisplayName(s, loc, value);
		};
	}

	private static Currency findBySymbol(String token, Locale loc, String original) {
		var matches = new ArrayList<Currency>();
		for (var c : Currency.getAvailableCurrencies())
			if (token.equals(c.getSymbol(loc)))
				matches.add(c);
		return resolveAmbiguousMatches(matches, loc, original, "symbol");
	}

	private static Currency findByDisplayName(String token, Locale loc, String original) {
		var matches = new ArrayList<Currency>();
		for (var c : Currency.getAvailableCurrencies())
			if (token.equals(c.getDisplayName(loc)))
				matches.add(c);
		return resolveAmbiguousMatches(matches, loc, original, "display name");
	}

	private static Currency resolveAmbiguousMatches(List<Currency> matches, Locale loc, String original, String kind) {
		if (matches.isEmpty())
			throw illegalArg("Could not resolve currency {0} ''{1}'' in locale {2}", kind, original, loc);
		if (matches.size() == 1)
			return matches.get(0);
		// Prefer the locale's default currency when the locale has one.
		try {
			var preferred = Currency.getInstance(loc);
			if (matches.contains(preferred))
				return preferred;
		} catch (@SuppressWarnings("unused") IllegalArgumentException ignored) {
			// Locale has no associated currency (e.g. Locale.ROOT, Locale.ENGLISH, Locale.JAPAN may or may not).
		}
		throw illegalArg("Ambiguous currency {0} ''{1}'' in locale {2}: matches {3}", kind, original, loc, matches);
	}

	/**
	 * Returns <jk>true</jk> if this format emits a numeric wire value.
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
