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
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;

/**
 * The per-column value type that governs which search operators apply to a column and how a bare (no leading
 * {@code $}) pattern token is interpreted.
 *
 * <p>
 * A column's value type selects both the operator set offered for that column
 * ({@link SearchOperatorSet#forType(SearchType)}) and the leaf-comparison semantics a context applies.  The lowercase
 * {@link #wire() wire token} is the stable external spelling.  {@link #bareHelp()} is a fixed, one-sentence
 * explanation of bare-token behavior intended for display in a search-bar popover.
 *
 * <h5 class='section'>Bare-token behavior per type:</h5>
 * <ul>
 * 	<li>{@link #TEXT} / {@link #ID} &mdash; case-insensitive substring (same idea as {@code $contains}).
 * 	<li>{@link #VERSION} &mdash; dotted-tuple prefix.
 * 	<li>{@link #NUMERIC} / {@link #TIMESTAMP} / {@link #ENUM} / {@link #BOOLEAN} &mdash; exact equality.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>help</jv> = SearchType.VERSION.<jsm>bareHelp</jsm>();
 * 	<jc>// help == "Matches any version starting with this prefix, e.g. 250 matches 250.1 and 250.2.3."</jc>
 * </p>
 *
 * <h5 class='section'>Example (typing a literal):</h5>
 * <p class='bjava'>
 * 	<jc>// Type a literal the same way for every engine, so a bad value is BAD_VALUE (400) everywhere.</jc>
 * 	Object <jv>v</jv> = SearchType.<jsf>NUMERIC</jsf>.<jsm>parse</jsm>(<js>"42"</js>);  <jc>// BigDecimal("42")</jc>
 * </p>
 *
 * @since 10.0.0
 */
public enum SearchType {

	/** Free text.  Bare token is a case-insensitive substring match. */
	TEXT("text", "Matches any value containing this text (case-insensitive). Use $eq(...) for an exact, case-sensitive match."),

	/** An id-like string (identifiers, keys).  Bare token is a case-insensitive substring match, same as {@link #TEXT}. */
	ID("id", "Matches any value containing this text (case-insensitive). Use $eq(...) for an exact, case-sensitive match."),

	/** A number.  Bare token is exact equality; supports the ordered operators. */
	NUMERIC("numeric", "Matches a number exactly. Use $gt / $gte / $lt / $lte / $between for a range, or a duration such as -24h or -PT24H (= -86400000 ms)."),

	/** A dotted version tuple (e.g. {@code 10.0.0}).  Bare token is a dotted-tuple prefix; supports the ordered operators and {@code $prefix}. */
	VERSION("version", "Matches any version starting with this prefix, e.g. 250 matches 250.1 and 250.2.3."),

	/**
	 * An instant/timestamp.  Bare token is exact equality; supports the ordered operators.
	 *
	 * <p>
	 * <b>Offset advice:</b> a value with an explicit offset ({@code 2026-09-30T12:00:00Z},
	 * {@code 2026-09-30T12:00:00-04:00}) always denotes one absolute instant.  The local forms (a date-time without an
	 * offset, or a bare date) have no zone: in-memory matching treats them as UTC, while a SQL engine hands them to the
	 * database, which interprets them in the <i>session</i> time zone.  The two can therefore disagree, so prefer the
	 * offset forms (or epoch milliseconds, which are always UTC) in any search that must behave identically everywhere.
	 * See {@link #parse(String)}.
	 */
	TIMESTAMP("timestamp", "Matches an exact instant; a bare date means UTC midnight exactly, so for a whole day use $between(d, d+1). Also accepts a duration such as -24h or -PT24H, relative to the request time."),

	/** A one-of enumerated value.  Bare token is a case-insensitive exact match. */
	ENUM("enum", "Matches one value exactly, case-insensitive."),

	/** A boolean.  Bare token is exact equality; supports only {@code $eq}/{@code $ne}. */
	BOOLEAN("boolean", "Matches true or false exactly.");

	private final String wire;
	private final String bareHelp;

	SearchType(String wire, String bareHelp) {
		this.wire = wire;
		this.bareHelp = bareHelp;
	}

	/**
	 * Returns the lowercase wire token for this type (e.g. {@code "text"}).
	 *
	 * @return The wire token.
	 */
	public String wire() {
		return wire;
	}

	/**
	 * Returns a fixed, one-sentence explanation of what a bare (no leading {@code $}) token means for a column of
	 * this type, intended for display in a search-bar popover.
	 *
	 * @return The help string.  Never <jk>null</jk> or blank.
	 */
	public String bareHelp() {
		return bareHelp;
	}

	/**
	 * Resolves a wire token back to its {@link SearchType}.
	 *
	 * @param wire The lowercase wire token (case-insensitive).  Can be <jk>null</jk>.
	 * @return The matching type, or <jk>null</jk> if {@code wire} is <jk>null</jk>, blank, or unrecognized.
	 */
	public static SearchType fromWire(String wire) {
		if (wire == null)
			return null;
		var w = wire.trim();
		for (var t : values())
			if (eqic(t.wire, w))
				return t;
		return null;
	}

	/**
	 * Parses a decoded literal (quotes stripped, escapes undone) into this type's Java representation.
	 *
	 * <p>
	 * {@link #NUMERIC} parses to {@link java.math.BigDecimal}; {@link #BOOLEAN} to {@link Boolean}
	 * (case-insensitive {@code true}/{@code false}); {@link #TIMESTAMP} to {@link java.time.OffsetDateTime} (has an
	 * offset), {@link java.time.LocalDateTime} (no offset), {@link java.time.LocalDate} (date only), or, for a bare
	 * integer, an {@link java.time.OffsetDateTime} at UTC built from epoch milliseconds (optional leading {@code -}; no
	 * {@code +}).  The local forms carry no zone - see the offset advice on {@link #TIMESTAMP}.  {@link #TEXT}, {@link #ID},
	 * {@link #ENUM} and {@link #VERSION} are returned unchanged (never <jk>null</jk>; a <jk>null</jk> {@code value}
	 * becomes {@code ""}).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BigDecimal <jv>n</jv> = (BigDecimal)SearchType.<jsf>NUMERIC</jsf>.<jsm>parse</jsm>(<js>"-3.5"</js>);
	 * </p>
	 *
	 * @param value The decoded literal text, or <jk>null</jk> (treated as empty).
	 * @return The parsed value.
	 * @throws BeanQuerySyntaxException (code {@link BeanQuerySyntaxException.Code#BAD_VALUE BAD_VALUE}) If
	 * 	{@code value} does not parse for this type ({@link #NUMERIC}, {@link #BOOLEAN} or {@link #TIMESTAMP} only).
	 * @since 10.0.0
	 */
	public Object parse(String value) {
		return ValueParse.parse(this, value);
	}

	/**
	 * Parses a decoded literal like {@link #parse(String)}, resolving a relative-duration literal against
	 * {@code requestTime} instead of the current instant.
	 *
	 * <p>
	 * A relative-duration literal is {@code [+-]<n>(ms|s|m|h|d)} (lowercase units only), e.g. {@code -24h}, or a signed ISO-8601 duration ({@code PT24H}, {@code -PT24H},
	 * {@code +PT30M}, {@code P1D}) with identical semantics (only one leading sign; {@code PT-1H}, years, months and
	 * weeks are {@code BAD_VALUE}).  For
	 * {@link #NUMERIC} it parses to the signed amount in milliseconds (a {@link java.math.BigDecimal}; "now" is never
	 * involved).  For {@link #TIMESTAMP} it parses to {@code requestTime} plus the signed amount, as an
	 * {@link java.time.OffsetDateTime} at UTC.  A bare signed number such as {@code -5} is never a duration, and a total that overflows a {@code long} (for
	 * {@link #TIMESTAMP}, after adding {@code requestTime}'s epoch milliseconds) is {@code BAD_VALUE}.  On
	 * {@link #BOOLEAN} a duration is {@code BAD_VALUE}; {@link #TEXT}, {@link #ID}, {@link #ENUM} and {@link #VERSION}
	 * are untyped and keep the text literally.  Every
	 * other literal shape parses exactly as in {@link #parse(String)}; {@link #parse(String)} itself delegates here
	 * with {@link Instant#now()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Instant <jv>now</jv> = Instant.<jsm>parse</jsm>(<js>"2026-09-30T12:00:00Z"</js>);
	 * 	Object <jv>yesterday</jv> = SearchType.<jsf>TIMESTAMP</jsf>.<jsm>parse</jsm>(<js>"-24h"</js>, <jv>now</jv>);  <jc>// 2026-09-29T12:00Z</jc>
	 * 	Object <jv>ms</jv> = SearchType.<jsf>NUMERIC</jsf>.<jsm>parse</jsm>(<js>"-24h"</js>, <jv>now</jv>);  <jc>// BigDecimal("-86400000")</jc>
	 * </p>
	 *
	 * @param value The decoded literal text, or <jk>null</jk> (treated as empty).
	 * @param requestTime The instant a relative-duration {@link #TIMESTAMP} literal resolves against.  Must not be <jk>null</jk>.
	 * @return The parsed value.
	 * @throws BeanQuerySyntaxException (code {@link BeanQuerySyntaxException.Code#BAD_VALUE BAD_VALUE}) If
	 * 	{@code value} does not parse for this type ({@link #NUMERIC}, {@link #BOOLEAN} or {@link #TIMESTAMP} only).
	 * @since 10.0.0
	 */
	public Object parse(String value, Instant requestTime) {
		return ValueParse.parse(this, value, reqnn("requestTime", requestTime));
	}
}
