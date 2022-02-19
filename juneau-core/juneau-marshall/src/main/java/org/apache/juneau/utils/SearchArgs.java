// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Encapsulates arguments for basic search/view/sort/position/limit functionality.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class SearchArgs {

	/**
	 * Default search args.
	 */
	public static SearchArgs DEFAULT = SearchArgs.create().build();

	private final Map<String,String> search;
	private final List<String> view;
	private final Map<String,Boolean> sort;
	private final int position, limit;
	private final boolean ignoreCase;

	SearchArgs(Builder b) {
		this.search = unmodifiable(copyOf(b.search));
		this.view = unmodifiable(copyOf(b.view));
		this.sort = unmodifiable(copyOf(b.sort));
		this.position = b.position;
		this.limit = b.limit;
		this.ignoreCase = b.ignoreCase;
	}

	/**
	 * Creates a new builder for {@link SearchArgs}
	 *
	 * @return A new builder for {@link SearchArgs}
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder for {@link SearchArgs} class.
	 */
	public static final class Builder {
		Map<String,String> search = map();
		List<String> view = list();
		Map<String,Boolean> sort = map();
		int position, limit;
		boolean ignoreCase;

		/**
		 * Adds search terms to this builder.
		 *
		 * <p>
		 * The search terms are a comma-delimited list of key/value pairs of column-names and search tokens.
		 *
		 * <p>
		 * For example:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.search(<js>"column1=foo*, column2=bar baz"</js>);
		 * </p>
		 *
		 * <p>
		 * It's up to implementers to decide the syntax and meaning of the search terms.
		 *
		 * <p>
		 * Whitespace is trimmed from column names and search tokens.
		 *
		 * @param searchTerms
		 * 	The search terms string.
		 * 	Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder search(String searchTerms) {
			split(searchTerms, x -> {
				int i = indexOf(x, '=', '>', '<');
				if (i == -1)
					throw runtimeException("Invalid search terms: ''{0}''", searchTerms);
				char c = x.charAt(i);
				search(x.substring(0, i).trim(), x.substring(c == '=' ? i+1 : i).trim());
			});
			return this;
		}

		/**
		 * Adds a search term to this builder.
		 *
		 * <p>
		 * It's up to implementers to decide the syntax and meaning of the search term.
		 *
		 * @param column The column being searched.
		 * @param searchTerm The search term.
		 * @return This object.
		 */
		public Builder search(String column, String searchTerm) {
			this.search.put(column, searchTerm);
			return this;
		}

		/**
		 * Specifies the list of columns to view.
		 *
		 * <p>
		 * The columns argument is a simple comma-delimited list of column names.
		 *
		 * <p>
		 * For example:
		 * <p class='bjava'>
		 * 	<jv>builder</jv>.view(<js>"column1, column2"</js>);
		 * </p>
		 *
		 * <p>
		 * Whitespace is trimmed from column names.
		 *
		 * <p>
		 * Empty view columns imply view all columns.
		 *
		 * @param columns
		 * 	The columns being viewed.
		 * 	Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder view(String columns) {
			if (columns != null)
				Collections.addAll(this.view, StringUtils.split(columns));
			return this;
		}

		/**
		 * Specifies the list of columns to view.
		 *
		 * <p>
		 * Empty view columns imply view all columns.
		 *
		 * @param columns The columns being viewed.
		 * @return This object.
		 */
		public Builder view(Collection<String> columns) {
			this.view.addAll(columns);
			return this;
		}

		/**
		 * Specifies the sort arguments.
		 *
		 * <p>
		 * The sort argument is a simple comma-delimited list of column names.
		 * <br>Column names can be suffixed with <js>'+'</js> or <js>'-'</js> to indicate ascending or descending order.
		 * <br>No suffix implies ascending order.
		 *
		 * <p>
		 * For example:
		 * <p class='bjava'>
		 * 	<jc>// Order by column1 ascending, then column2 descending.</jc>
		 * 	<jv>builder</jv>.sort(<js>"column1, column2-"</js>);
		 * </p>
		 *
		 * <p>
		 * Note that the order of the order arguments is important.
		 *
		 * <p>
		 * Whitespace is trimmed from column names.
		 *
		 * @param sortArgs
		 * 	The columns to sort by.
		 * 	Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder sort(String sortArgs) {
			if (sortArgs != null)
				sort(Arrays.asList(StringUtils.split(sortArgs)));
			return this;
		}

		/**
		 * Specifies the sort arguments.
		 *
		 * <p>
		 * Column names can be suffixed with <js>'+'</js> or <js>'-'</js> to indicate ascending or descending order.
		 * <br>No suffix implies ascending order.
		 *
		 * <p>
		 * Note that the order of the sort is important.
		 *
		 * @param sortArgs
		 * 	The columns to sort by.
		 * 	Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder sort(Collection<String> sortArgs) {
			for (String s : sortArgs) {
				boolean isDesc = false;
				if (endsWith(s, '-', '+')) {
					isDesc = endsWith(s, '-');
					s = s.substring(0, s.length()-1);
				}
				this.sort.put(s, isDesc);
			}
			return this;
		}

		/**
		 * Specifies the starting line number.
		 *
		 * @param position The zero-indexed position.
		 * @return This object.
		 */
		public Builder position(int position) {
			this.position = position;
			return this;
		}

		/**
		 * Specifies the number of rows to return.
		 *
		 * @param limit
		 * 	The number of rows to return.
		 * 	If <c>&lt;=0</c>, all rows should be returned.
		 * @return This object.
		 */
		public Builder limit(int limit) {
			this.limit = limit;
			return this;
		}

		/**
		 * Specifies whether case-insensitive search should be used.
		 *
		 * <p>
		 * The default is <jk>false</jk>.
		 *
		 * @param value The ignore-case flag value.
		 * @return This object.
		 */
		public Builder ignoreCase(boolean value) {
			this.ignoreCase = value;
			return this;
		}

		/**
		 * Construct the {@link SearchArgs} object.
		 *
		 * <p>
		 * This method can be called multiple times to construct new objects.
		 *
		 * @return A new {@link SearchArgs} object initialized with values in this builder.
		 */
		public SearchArgs build() {
			return new SearchArgs(this);
		}
	}

	/**
	 * The query search terms.
	 *
	 * <p>
	 * The search terms are key/value pairs consisting of column-names and search tokens.
	 *
	 * <p>
	 * It's up to implementers to decide the syntax and meaning of the search term.
	 *
	 * @return An unmodifiable map of query search terms.
	 */
	public Map<String,String> getSearch() {
		return search;
	}

	/**
	 * The view columns.
	 *
	 * <p>
	 * The view columns are the list of columns that should be displayed.
	 * An empty list implies all columns should be displayed.
	 *
	 * @return An unmodifiable list of columns to view.
	 */
	public List<String> getView() {
		return view;
	}

	/**
	 * The sort columns.
	 *
	 * <p>
	 * The sort columns are key/value pairs consisting of column-names and direction flags
	 * (<jk>false</jk> = ascending, <jk>true</jk> = descending).
	 *
	 * @return An unmodifiable ordered map of sort columns and directions.
	 */
	public Map<String,Boolean> getSort() {
		return sort;
	}

	/**
	 * The first-row position.
	 *
	 * @return
	 * 	The zero-indexed row number of the first row to display.
	 * 	Default is <c>0</c>
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * The number of rows to return.
	 *
	 * @return
	 * 	The number of rows to return in the result.
	 * 	Default is <c>0</c> which means return all rows.
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * The ignore-case flag.
	 *
	 * <p>
	 * Used in conjunction with {@link #getSearch()} to specify whether case-insensitive searches should be performed.
	 *
	 * @return
	 * 	The number of rows to return in the result.
	 * 	Default is <jk>false</jk>.
	 */
	public boolean isIgnoreCase() {
		return ignoreCase;
	}
}
