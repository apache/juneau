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

import java.math.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Canonical {@code Object} &rarr; wire-literal formatting for {@link BeanQuery.Builder} /
 * {@link BeanQuery.GroupBuilder} condition values (design §6).
 *
 * <p>
 * One conversion table serves every leaf-condition method on the builders: a value is formatted to the same wire
 * text a session parses back, so a builder-built query and a hand-written one behave identically.  The SQL engine
 * applies the inverse of this table for typed binds.  There is no {@code toString()} fallback for an
 * unrecognized type &mdash; formatting a value this table does not cover throws rather than silently embedding a
 * Java-specific representation.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>wire1</jv> = ValueFormat.<jsm>format</jsm>(<jk>new</jk> BigDecimal(<js>"1E+3"</js>));  <jc>// "1000"</jc>
 * 	String <jv>wire2</jv> = ValueFormat.<jsm>format</jsm>(Instant.<jsm>now</jsm>());  <jc>// "2026-09-30T12:00:00Z"</jc>
 * </p>
 *
 * @since 10.0.0
 */
final class ValueFormat {

	private ValueFormat() {}

	/**
	 * Formats a value to its canonical wire literal.
	 *
	 * @param v The value.  Must not be <jk>null</jk>.
	 * @return The wire literal, never <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code v} is <jk>null</jk>, of an unsupported type, or a non-finite /
	 * 	unparseable number.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for a straight-line type dispatch table.
	})
	static String format(Object v) {
		req(v != null, "Unsupported search value type '%s'.", "null");
		if (v instanceof CharSequence cs)
			return cs.toString();
		if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte || v instanceof BigInteger)
			return v.toString();
		if (v instanceof Boolean b)
			return b.toString();
		if (v instanceof Enum<?> e)
			return e.name();
		if (v instanceof Double d) {
			req(! d.isNaN() && ! d.isInfinite(), "Search value '%s' is not a finite number.", d);
			return decimal(v);
		}
		if (v instanceof Float f) {
			req(! f.isNaN() && ! f.isInfinite(), "Search value '%s' is not a finite number.", f);
			return decimal(v);
		}
		if (v instanceof BigDecimal || v instanceof Number)
			return decimal(v);
		if (v instanceof Instant i)
			return DateTimeFormatter.ISO_INSTANT.format(i);
		if (v instanceof OffsetDateTime o)
			return DateTimeFormatter.ISO_INSTANT.format(o.toInstant());
		if (v instanceof ZonedDateTime z)
			return DateTimeFormatter.ISO_INSTANT.format(z.toInstant());
		if (v instanceof Date d)
			return DateTimeFormatter.ISO_INSTANT.format(d.toInstant());
		if (v instanceof Calendar c)
			return DateTimeFormatter.ISO_INSTANT.format(c.toInstant());
		if (v instanceof LocalDate ld)
			return DateTimeFormatter.ISO_LOCAL_DATE.format(ld);
		if (v instanceof LocalDateTime ldt)
			return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ldt);
		if (v instanceof RelativeDuration rd)
			return (rd.amount() >= 0 ? "+" : "") + rd.amount() + rd.unit().token();
		return fail(v);
	}

	/** Renders a {@code Double}/{@code Float}/{@code BigDecimal}/other {@code Number} as a plain decimal string. */
	private static String decimal(Object v) {
		try {
			return new BigDecimal(v.toString()).stripTrailingZeros().toPlainString();
		} catch (NumberFormatException e) {
			return fail(v);
		}
	}

	private static String fail(Object v) {
		throw iaex("Unsupported search value type '%s'.", v.getClass().getName());
	}
}
