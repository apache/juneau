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

import java.util.*;

/**
 * A {@link BeanQuery} after validation: the engine-neutral input to every engine hook.
 *
 * <p>
 * Produced only by {@link BeanQueryContext#resolve(BeanQuery)} and {@link BeanQuerySession#resolve(BeanQuery)}.
 * Every column in it is allowed, every expression is parsed, depth-checked and type-checked, the limit is clamped,
 * and {@link #counts()} is never {@link CountPolicy#IF_REQUESTED}.  Engines never see raw request strings.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Validate without executing, e.g. to fail fast or to inspect what a request will do.</jc>
 * 	ResolvedQuery <jv>q</jv> = <jv>context</jv>.resolve(BeanQuery.<jsm>create</jsm>().gt(<js>"age"</js>, 21).sortDesc(<js>"name"</js>).build());
 * 	<jk>if</jk> (<jv>q</jv>.filter() <jk>instanceof</jk> Filter.Leaf <jv>leaf</jv>)
 * 		<jv>log</jv>.fine(<jv>leaf</jv>.column() + <js>" ("</js> + <jv>leaf</jv>.type() + <js>"): "</js> + <jv>leaf</jv>.expression());
 * 	<jk>boolean</jk> <jv>newestFirst</jv> = <jv>q</jv>.sort().get(0).descending();
 * </p>
 *
 * <p>
 * The nested {@link SortKey} type is covered by this example.
 *
 * @since 10.0.0
 */
public final class ResolvedQuery {

	/** One sort key: its column, the column's value type, and the direction. */
	public static final class SortKey {
		private final String column;
		private final SearchType type;
		private final boolean descending;

		SortKey(String column, SearchType type, boolean descending) {
			this.column = column;
			this.type = type;
			this.descending = descending;
		}

		/** @return The column name. */
		public String column() {
			return column;
		}

		/** @return The column's value type. */
		public SearchType type() {
			return type;
		}

		/** @return <jk>true</jk> for descending order. */
		public boolean descending() {
			return descending;
		}

		@Override /* Object */
		public String toString() {
			return column + (descending ? ":desc" : "");
		}
	}

	private final Filter filter;
	private final List<SortKey> sort;
	private final List<String> view;
	private final int position;
	private final Integer limit;
	private final CountPolicy counts;
	private final BeanQueryContext<?> origin;

	ResolvedQuery(Filter filter, List<SortKey> sort, List<String> view, int position, Integer limit, CountPolicy counts, BeanQueryContext<?> origin) {
		this.filter = filter;
		this.sort = List.copyOf(sort);
		this.view = List.copyOf(view);
		this.position = position;
		this.limit = limit;
		this.counts = counts;
		this.origin = origin;
	}

	/**
	 * @return The resolved search tree, AND-ed and OR-ed across columns as the request's search expressed it;
	 * 	never <jk>null</jk> &mdash; an empty search resolves to an empty {@link Filter.And}, which matches every row.
	 */
	public Filter filter() {
		return filter;
	}

	/** @return The sort keys, most significant first (empty for source order). */
	public List<SortKey> sort() {
		return sort;
	}

	/** @return The view columns; never empty (defaults to every allowed column in declaration order). */
	public List<String> view() {
		return view;
	}

	/** @return The zero-based offset of the first row (never negative). */
	public int position() {
		return position;
	}

	/** @return The maximum number of rows, or <jk>null</jk> for all rows (only when {@code maxLimit} is unset). */
	public Integer limit() {
		return limit;
	}

	/** @return The effective count policy: {@link CountPolicy#NONE}, {@link CountPolicy#MATCHED} or {@link CountPolicy#BOTH}. */
	public CountPolicy counts() {
		return counts;
	}

	@Override /* Object */
	public String toString() {
		return "ResolvedQuery(filter=" + filter + ", sort=" + sort + ", view=" + view + ", position=" + position + ", limit=" + limit + ", counts=" + counts + ")";
	}

	/**
	 * @param context The context to check.
	 * @return <jk>true</jk> if the given context is the one that resolved this query (directly, or through a session built
	 * 	from it).  Not public: an engine's rendering/execution code uses this only to confirm a caller-supplied query
	 * 	matches the context it is about to be used against; it is not part of this type's engine-neutral public contract.
	 */
	boolean isResolvedBy(BeanQueryContext<?> context) {
		return origin == context;
	}
}
