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

/**
 * A signed, relative-to-request-time duration value: wire literal {@code [+-]<n>(ms|s|m|h|d)} (IRS parity, design
 * review §3 Gap 2).
 *
 * <p>
 * Pass one to any {@link BeanQuery.Builder}/{@link BeanQuery.GroupBuilder} comparison method ({@code eq}, {@code ne},
 * {@code gt}, {@code gte}, {@code lt}, {@code lte}, {@code between}) on a {@code TIMESTAMP} or numeric-duration
 * column to mean "relative to the time the request is evaluated" &mdash; for example {@code -24} {@link Unit#H H}
 * for "24 hours before now." {@link ValueFormat} renders this to the exact wire literal below; the search parser
 * reads that literal back out of a hand-written or stored search string (on a {@code BOOLEAN} column it is
 * {@code BAD_VALUE}; on {@code TEXT}, {@code ID}, {@code ENUM} and {@code VERSION} columns the text is kept
 * literally), and the query engine resolves it against one request-time instant
 * captured once per query (not {@code now()}/{@code interval} in generated SQL): a {@code TIMESTAMP} column's
 * literal becomes {@code instant &plusmn; duration}, a numeric-duration column's becomes a plain signed-milliseconds
 * number. This type is the shared, stable wire format the builder, parser, and engines meet at.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// matches rows whose lastSeen is within the last 24 hours</jc>
 * 	BeanQuery <jv>q</jv> = BeanQuery.<jsm>create</jsm>()
 * 		.gte(<js>"lastSeen"</js>, RelativeDuration.<jsm>ofHours</jsm>(-24))
 * 		.build();
 * 	<jc>// q.getSearch() == "lastSeen=$gte(\"-24h\")"</jc>
 * </p>
 *
 * @param amount The signed magnitude; negative is in the past, positive is in the future, relative to request time.
 * @param unit The time unit. Must not be <jk>null</jk>.
 * @since 10.0.0
 */
public record RelativeDuration(long amount, Unit unit) {

	/**
	 * Compact constructor &mdash; validates <c>unit</c> is non-<jk>null</jk>.
	 *
	 * @param amount The signed magnitude.
	 * @param unit The time unit. Must not be <jk>null</jk>.
	 */
	public RelativeDuration {
		reqnn("unit", unit);
	}

	/**
	 * Creates a relative-duration literal.
	 *
	 * @param amount The signed magnitude; negative is in the past, positive is in the future.
	 * @param unit The time unit. Must not be <jk>null</jk>.
	 * @return The new literal.
	 */
	public static RelativeDuration of(long amount, Unit unit) {
		return new RelativeDuration(amount, unit);
	}

	/**
	 * Shorthand for {@code of(amount, Unit.MS)}.
	 *
	 * @param amount The signed magnitude.
	 * @return The new literal.
	 */
	public static RelativeDuration ofMillis(long amount) {
		return of(amount, Unit.MS);
	}

	/**
	 * Shorthand for {@code of(amount, Unit.S)}.
	 *
	 * @param amount The signed magnitude.
	 * @return The new literal.
	 */
	public static RelativeDuration ofSeconds(long amount) {
		return of(amount, Unit.S);
	}

	/**
	 * Shorthand for {@code of(amount, Unit.M)}.
	 *
	 * @param amount The signed magnitude.
	 * @return The new literal.
	 */
	public static RelativeDuration ofMinutes(long amount) {
		return of(amount, Unit.M);
	}

	/**
	 * Shorthand for {@code of(amount, Unit.H)}.
	 *
	 * @param amount The signed magnitude.
	 * @return The new literal.
	 */
	public static RelativeDuration ofHours(long amount) {
		return of(amount, Unit.H);
	}

	/**
	 * Shorthand for {@code of(amount, Unit.D)}.
	 *
	 * @param amount The signed magnitude.
	 * @return The new literal.
	 */
	public static RelativeDuration ofDays(long amount) {
		return of(amount, Unit.D);
	}

	/** The duration unit, with its lowercase wire token (design review §3 Gap 2). */
	public enum Unit {

		/** Milliseconds. */
		MS("ms"),

		/** Seconds. */
		S("s"),

		/** Minutes. */
		M("m"),

		/** Hours. */
		H("h"),

		/** Days. */
		D("d");

		private final String token;

		Unit(String token) {
			this.token = token;
		}

		/**
		 * The lowercase wire token for this unit (e.g. {@code "h"}).
		 *
		 * @return The wire token.
		 */
		public String token() {
			return token;
		}
	}
}
