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
 * The two neutral row counts a session can report: {@link #total()} (the rows in scope before the query's search) and
 * {@link #matched()} (the rows that survive the search, before any page limit).
 *
 * <p>
 * These names are transport-neutral on purpose; a request-facing adapter maps them to its own vocabulary (for example
 * a DataTables adapter maps {@code total}&rarr;{@code recordsTotal} and {@code matched}&rarr;{@code recordsFiltered}).
 *
 * @since 10.0.0
 */
public final class Counts {

	private final long total;
	private final long matched;

	private Counts(long total, long matched) {
		this.total = total;
		this.matched = matched;
	}

	/**
	 * Creates a counts result.
	 *
	 * @param total The rows in scope before the search.
	 * @param matched The rows that survive the search, before any page limit.
	 * @return A new counts result.
	 */
	public static Counts of(long total, long matched) {
		return new Counts(total, matched);
	}

	/**
	 * The number of rows in scope before the query's search (after any server-only guard).
	 *
	 * @return The total row count.
	 */
	public long total() {
		return total;
	}

	/**
	 * The number of rows that survive the search, before any page limit.
	 *
	 * @return The matched row count.
	 */
	public long matched() {
		return matched;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return this == o || (o instanceof Counts c && total == c.total && matched == c.matched);
	}

	@Override /* Object */
	public int hashCode() {
		return h(total, matched);
	}

	@Override /* Object */
	public String toString() {
		return "Counts(total=" + total + ", matched=" + matched + ")";
	}
}
