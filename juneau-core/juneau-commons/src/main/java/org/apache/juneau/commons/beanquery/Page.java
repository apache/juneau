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

import java.util.*;

/**
 * One page of query results, plus the optional counts selected by the session's {@link CountPolicy}.
 *
 * <ul>
 * 	<li>{@link #total()}: rows that pass the guards (before the search).
 * 	<li>{@link #matched()}: rows that pass the guards <b>and</b> the search.
 * </ul>
 *
 * <p>
 * Under {@link CountPolicy#NONE} neither count is present.  Under {@link CountPolicy#MATCHED} only
 * {@link #matched()} is.  Under {@link CountPolicy#BOTH} both are.  Counts are opt-in and independent of each
 * other: a caller that did not ask for a count gets an empty {@link OptionalLong} back rather than an exception, so
 * "was a count computed" and "read the count" are the same call.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(BeanQuery.<jsm>create</jsm>().counts(CountRequest.<jsf>BOTH</jsf>).build());
 * 	<jk>for</jk> (Person <jv>p</jv> : <jv>page</jv>.rows())
 * 		<jsm>render</jsm>(<jv>p</jv>);
 * 	<jv>page</jv>.total().ifPresent(<jv>total</jv> -&gt; <jsm>renderFooter</jsm>(<jv>page</jv>.matched().getAsLong() + <js>" of "</js> + <jv>total</jv>));
 * </p>
 *
 * @param <T> The row type.
 * @since 10.0.0
 */
public final class Page<T> {

	private final List<T> rows;
	private final Long total;
	private final Long matched;

	private Page(List<T> rows, Long total, Long matched) {
		this.rows = copyRows(rows);
		this.total = total;
		this.matched = matched;
	}

	// List.copyOf throws a bare, message-less NullPointerException on a null element; give it a clear cause instead.
	private static <T> List<T> copyRows(List<T> rows) {
		for (var i = 0; i < rows.size(); i++)
			if (rows.get(i) == null)
				throw new BeanQueryExecutionException(null, "Row %s is null.", i);
		return List.copyOf(rows);
	}

	/**
	 * Creates a page without counts.
	 *
	 * @param <T> The row type.
	 * @param rows The rows.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @return A new page.
	 */
	public static <T> Page<T> of(List<T> rows) {
		return new Page<>(rows, null, null);
	}

	/**
	 * Creates a page with both counts.
	 *
	 * @param <T> The row type.
	 * @param rows The rows.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @param total The number of rows that pass the guards.
	 * @param matched The number of rows that pass the guards and the search.
	 * @return A new page.
	 */
	public static <T> Page<T> of(List<T> rows, long total, long matched) {
		return new Page<>(rows, total, matched);
	}

	/**
	 * Creates a page with only the matched count.
	 *
	 * @param <T> The row type.
	 * @param rows The rows.  Must not be <jk>null</jk> or contain <jk>null</jk>.
	 * @param matched The number of rows that pass the guards and the search.
	 * @return A new page.
	 */
	public static <T> Page<T> ofMatched(List<T> rows, long matched) {
		return new Page<>(rows, null, matched);
	}

	/**
	 * Returns the rows of this page.
	 *
	 * @return An unmodifiable list (never <jk>null</jk>).
	 */
	public List<T> rows() {
		return rows;
	}

	/**
	 * The number of rows in scope before the query's search (after any server-only guard).
	 *
	 * @return The total row count, or empty if it was not requested.
	 */
	public OptionalLong total() {
		return total == null ? OptionalLong.empty() : OptionalLong.of(total);
	}

	/**
	 * The number of rows that survive the search, before any page limit.
	 *
	 * @return The matched row count, or empty if it was not requested.
	 */
	public OptionalLong matched() {
		return matched == null ? OptionalLong.empty() : OptionalLong.of(matched);
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return this == o || (o instanceof Page<?> p && eq(rows, p.rows) && eq(total, p.total) && eq(matched, p.matched));
	}

	@Override /* Object */
	public int hashCode() {
		return h(rows, total, matched);
	}

	@Override /* Object */
	public String toString() {
		var sb = new StringBuilder("Page[rows=").append(rows.size());
		if (total != null)
			sb.append(", total=").append(total);
		if (matched != null)
			sb.append(", matched=").append(matched);
		return sb.append(']').toString();
	}
}
