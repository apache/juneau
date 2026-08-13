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
package org.apache.juneau.rest.server.converter;

import java.util.*;

import org.apache.juneau.marshall.objecttools.*;

/**
 * Normalized, protocol-agnostic query arguments produced by a {@link QueryProtocol} and consumed by the shared
 * query engine.
 *
 * <p>
 * Wraps the existing engine argument types ({@link SearchArgs}, {@link ViewArgs}, {@link SortArgs},
 * {@link PageArgs}) plus a cross-column "match-any" (OR) search map:
 * <ul class='spaced-list'>
 * 	<li>{@link #getSearch() search} &mdash; per-column filters, <b>AND</b>-ed across columns (the engine's native model).
 * 	<li>{@link #getSearchAny() searchAny} &mdash; per-column filters, <b>OR</b>-ed across columns (a row matches if
 * 		<i>any</i> entry matches).  This models a protocol's "global search box" (e.g. DataTables' {@code search[value]})
 * 		which the engine's per-column AND model cannot otherwise express.  Entry values are raw
 * 		{@link SearchArgs}-syntax patterns (e.g. {@code "*Bill*"}).
 * 	<li>{@link #getView() view} &mdash; the columns to project.
 * 	<li>{@link #getSort() sort} &mdash; the sort columns and directions.
 * 	<li>{@link #getPage() page} &mdash; the position/limit window.
 * </ul>
 *
 * <p>
 * Any facet may be absent (its accessor returns {@link Optional#empty()}), in which case the engine skips that stage.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link QueryProtocol}
 * 	<li class='jc'>{@link QueryResult}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class QueryArgs {

	/** An empty set of query arguments (all facets absent). */
	public static final QueryArgs EMPTY = create().build();

	private final SearchArgs search;
	private final Map<String,String> searchAny;
	private final ViewArgs view;
	private final SortArgs sort;
	private final PageArgs page;

	private QueryArgs(Builder b) {
		this.search = b.search;
		// The builder only ever allocates the map when adding a (non-null) entry, so a non-null map is always non-empty.
		this.searchAny = (b.searchAny == null) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(b.searchAny));
		this.view = b.view;
		this.sort = b.sort;
		this.page = b.page;
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * The per-column search filters, AND-ed across columns.
	 *
	 * @return The search args, or {@link Optional#empty()} if none.
	 */
	public Optional<SearchArgs> getSearch() {
		return Optional.ofNullable(search);
	}

	/**
	 * The cross-column "match-any" (OR) search filters.
	 *
	 * <p>
	 * A row matches if <b>any</b> entry matches.  Entry keys are column names and values are raw
	 * {@link SearchArgs}-syntax patterns.
	 *
	 * @return An unmodifiable map, or {@link Optional#empty()} if none.
	 */
	public Optional<Map<String,String>> getSearchAny() {
		return Optional.ofNullable(searchAny);
	}

	/**
	 * The columns to project.
	 *
	 * @return The view args, or {@link Optional#empty()} if none.
	 */
	public Optional<ViewArgs> getView() {
		return Optional.ofNullable(view);
	}

	/**
	 * The sort columns and directions.
	 *
	 * @return The sort args, or {@link Optional#empty()} if none.
	 */
	public Optional<SortArgs> getSort() {
		return Optional.ofNullable(sort);
	}

	/**
	 * The position/limit window.
	 *
	 * @return The page args, or {@link Optional#empty()} if none.
	 */
	public Optional<PageArgs> getPage() {
		return Optional.ofNullable(page);
	}

	/**
	 * Builder for {@link QueryArgs}.
	 */
	public static class Builder {
		private SearchArgs search;
		private Map<String,String> searchAny;
		private ViewArgs view;
		private SortArgs sort;
		private PageArgs page;

		/**
		 * Sets the per-column (AND) search filters.
		 *
		 * @param value The value.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder search(SearchArgs value) {
			this.search = value;
			return this;
		}

		/**
		 * Adds a cross-column "match-any" (OR) search filter.
		 *
		 * @param column The column name.  Ignored if <jk>null</jk>.
		 * @param pattern The raw {@link SearchArgs}-syntax pattern.  Ignored if <jk>null</jk>.
		 * @return This object.
		 */
		public Builder searchAny(String column, String pattern) {
			if (column != null && pattern != null) {
				if (searchAny == null)
					searchAny = new LinkedHashMap<>();
				searchAny.put(column, pattern);
			}
			return this;
		}

		/**
		 * Sets the columns to project.
		 *
		 * @param value The value.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder view(ViewArgs value) {
			this.view = value;
			return this;
		}

		/**
		 * Sets the sort columns and directions.
		 *
		 * @param value The value.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder sort(SortArgs value) {
			this.sort = value;
			return this;
		}

		/**
		 * Sets the position/limit window.
		 *
		 * @param value The value.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder page(PageArgs value) {
			this.page = value;
			return this;
		}

		/**
		 * Builds the query arguments.
		 *
		 * @return A new {@link QueryArgs}.
		 */
		public QueryArgs build() {
			return new QueryArgs(this);
		}
	}
}
