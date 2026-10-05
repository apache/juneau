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
package org.apache.juneau.rest.server.datatables;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * The DataTables server-side-processing request envelope &mdash; the JSON <b>input</b> bean paired with the
 * {@link DataTablesResults} JSON <b>output</b> bean.
 *
 * <p>
 * This bean is a faithful model of the
 * <a class="doclink" href="https://datatables.net/manual/server-side">DataTables server-side request</a>; it carries
 * <b>no</b> query-engine types and knows nothing about {@code BeanQuery}.  The adapter in
 * {@code org.apache.juneau.rest.server.datatables.adapter} maps a parsed instance onto a
 * {@link org.apache.juneau.commons.beanquery.BeanQuery} (design §5.3).
 *
 * <p>
 * Shape (fields omitted from the request keep their defaults):
 * <p class='bjson'>
 * 	{
 * 		<jf>draw</jf>: 1,
 * 		<jf>start</jf>: 0,
 * 		<jf>length</jf>: 10,
 * 		<jf>search</jf>: { <jf>value</jf>: <js>""</js>, <jf>regex</jf>: <jk>false</jk>, <jf>fixed</jf>: [] },
 * 		<jf>columns</jf>: [ { <jf>data</jf>: <js>"name"</js>, <jf>name</jf>: <js>""</js>, <jf>searchable</jf>: <jk>true</jk>, <jf>orderable</jf>: <jk>true</jk>, <jf>search</jf>: { <jf>value</jf>: <js>""</js>, <jf>regex</jf>: <jk>false</jk>, <jf>fixed</jf>: [] } } ],
 * 		<jf>order</jf>: [ { <jf>column</jf>: 0, <jf>dir</jf>: <js>"asc"</js>, <jf>name</jf>: <js>""</js> } ]
 * 	}
 * </p>
 *
 * <p>
 * Every property DataTables 2.x core sends is modelled (including {@code search.fixed} and {@code order[].name}, both new
 * in 2.x), so the request parses under Juneau's default strict bean parsing, which rejects unknown properties.
 * Any OTHER top-level property - one an application merges in through {@code JuneauDataTables.ajax(url, {data:{...}})},
 * or one a DataTables extension adds (SearchBuilder's {@code searchBuilder}, SearchPanes' {@code searchPanes}) - is
 * collected into {@link #getExtra()} instead of failing the parse.  The adapter ignores it.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(<js>"/releases/query"</js>)
 * 	<jk>public</jk> DataTablesResults&lt;Release&gt; queryReleases(<ja>@Content</ja> DataTablesRequest <jv>request</jv>) {
 * 		<jk>try</jk> (var <jv>session</jv> = <jsf>CONTEXT</jsf>.getSession(<jv>releases</jv>)) {
 * 			<jk>return</jk> DataTablesQuery.<jsm>run</jsm>(<jv>request</jv>, <jv>session</jv>);
 * 		}
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public class DataTablesRequest {

	private int draw;
	private int start;
	private int length = -1;  // -1 (or an absent length) means "all rows".
	private Search search;
	private List<Column> columns;
	private List<Order> order;
	private Map<String,Object> extra = new LinkedHashMap<>();

	/**
	 * Returns the draw counter echoed back on the response.
	 *
	 * @return The draw counter.
	 */
	public int getDraw() {
		return draw;
	}

	/**
	 * Sets the draw counter.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesRequest setDraw(int value) {
		this.draw = value;
		return this;
	}

	/**
	 * Returns the 0-based index of the first record to return.
	 *
	 * @return The paging start index.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * Sets the 0-based index of the first record to return.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesRequest setStart(int value) {
		this.start = value;
		return this;
	}

	/**
	 * Returns the number of records the page should return ({@code -1} means all records, no paging).
	 *
	 * @return The page length.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Sets the number of records the page should return ({@code -1} means all records, no paging).
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesRequest setLength(int value) {
		this.length = value;
		return this;
	}

	/**
	 * Returns the global search descriptor.
	 *
	 * @return The global search, or <jk>null</jk> if none.
	 */
	public Search getSearch() {
		return search;
	}

	/**
	 * Sets the global search descriptor.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesRequest setSearch(Search value) {
		this.search = value;
		return this;
	}

	/**
	 * Returns the column descriptors.
	 *
	 * @return The columns, or <jk>null</jk> if none.
	 */
	public List<Column> getColumns() {
		return columns;
	}

	/**
	 * Sets the column descriptors.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesRequest setColumns(List<Column> value) {
		this.columns = value;
		return this;
	}

	/**
	 * Returns the ordering descriptors.
	 *
	 * @return The order entries, or <jk>null</jk> if none.
	 */
	public List<Order> getOrder() {
		return order;
	}

	/**
	 * Sets the ordering descriptors.
	 *
	 * @param value The value.
	 * @return This object.
	 */
	public DataTablesRequest setOrder(List<Order> value) {
		this.order = value;
		return this;
	}

	/**
	 * Returns every top-level request property that is not one of the modelled DataTables properties.
	 *
	 * <p>
	 * For example the {@code tenant} in {@code JuneauDataTables.ajax(url, {data:{tenant:'x'}})}, or an extension's
	 * own request key.  Lets a handler read application parameters that ride the JSON body.
	 *
	 * @return The extra properties, keyed by name.  Never <jk>null</jk>.
	 */
	@BeanProp(name="*")
	public Map<String,Object> getExtra() {
		return extra;
	}

	/**
	 * Records a top-level request property that is not one of the modelled DataTables properties.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 */
	@BeanProp(name="*")
	public void setExtra(String name, Object value) {
		extra.put(name, value);
	}

	/**
	 * A DataTables search descriptor ({@code {value, regex, fixed}}), used for both the global search and a column
	 * search.
	 */
	public static class Search {
		private String value;
		private boolean regex;
		private List<Fixed> fixed;

		/**
		 * Returns the DataTables 2.x fixed (named, API-applied) search terms.  Carried for wire fidelity &mdash; a
		 * strict parser would otherwise reject every DataTables 2.x request &mdash; and ignored by the adapter.
		 *
		 * @return The fixed search terms, or <jk>null</jk> if none.
		 */
		public List<Fixed> getFixed() {
			return fixed;
		}

		/**
		 * Sets the DataTables 2.x fixed search terms.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Search setFixed(List<Fixed> v) {
			this.fixed = v;
			return this;
		}

		/**
		 * Returns the raw search term.  For this design the term is a {@code $}-language expression (design §5.1).
		 *
		 * @return The search term, or <jk>null</jk> if none.
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Sets the raw search term.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Search setValue(String v) {
			this.value = v;
			return this;
		}

		/**
		 * Returns the DataTables regex flag.  Carried for wire fidelity; the adapter does not use it, since regex
		 * matching is expressed inline with the {@code $regex(...)} operator in {@link #getValue() value}.
		 *
		 * @return The regex flag.
		 */
		public boolean isRegex() {
			return regex;
		}

		/**
		 * Sets the DataTables regex flag.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Search setRegex(boolean v) {
			this.regex = v;
			return this;
		}
	}

	/**
	 * A DataTables 2.x fixed search term ({@code search.fixed[i]}): a named search applied through the DataTables API
	 * ({@code search.fixed(name, term)}) rather than typed by the user.
	 */
	public static class Fixed {
		private String name;
		private String term;

		/**
		 * Returns the fixed search's name.
		 *
		 * @return The name, or <jk>null</jk> if none.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the fixed search's name.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Fixed setName(String v) {
			this.name = v;
			return this;
		}

		/**
		 * Returns the fixed search's term, as DataTables stringified it (a function or regex arrives as its source).
		 *
		 * @return The term, or <jk>null</jk> if none.
		 */
		public String getTerm() {
			return term;
		}

		/**
		 * Sets the fixed search's term.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Fixed setTerm(String v) {
			this.term = v;
			return this;
		}
	}

	/**
	 * A DataTables column descriptor ({@code columns[i]}).
	 */
	public static class Column {
		private String data;
		private String name;
		private boolean searchable = true;
		private boolean orderable = true;
		private Search search;

		/**
		 * Returns the column's data source (the row-data key / bean-property name).
		 *
		 * @return The data source, or <jk>null</jk> if none.
		 */
		@BeanIgnore
		public String getData() {
			return data;
		}

		/**
		 * Bean-property view of {@link #getData() data} typed as {@link Object}, so the property type matches
		 * {@link #setData(Object)} and the parser accepts a bare JSON number as well as a string.
		 *
		 * @return The data source, or <jk>null</jk> if none.
		 */
		@BeanProp("data")
		public Object getDataProperty() {
			return getData();
		}

		/**
		 * Sets the column's data source.
		 *
		 * <p>
		 * DataTables sends {@code data} as a string (object source) <em>or</em> a bare JSON number (array source), so
		 * this accepts any value and normalizes it to its string form; {@link #getData()} always returns a string.
		 *
		 * @param v The value (a string or a number).
		 * @return This object.
		 */
		@BeanProp("data")
		public Column setData(Object v) {
			this.data = v == null ? null : String.valueOf(v);
			return this;
		}

		/**
		 * Returns the column's name (a fallback key when {@link #getData() data} is absent).
		 *
		 * @return The name, or <jk>null</jk> if none.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the column's name.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Column setName(String v) {
			this.name = v;
			return this;
		}

		/**
		 * Returns whether this column may be searched (default <jk>true</jk>).
		 *
		 * @return <jk>true</jk> if searchable.
		 */
		public boolean isSearchable() {
			return searchable;
		}

		/**
		 * Sets whether this column may be searched.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Column setSearchable(boolean v) {
			this.searchable = v;
			return this;
		}

		/**
		 * Returns whether this column may be ordered (default <jk>true</jk>).
		 *
		 * @return <jk>true</jk> if orderable.
		 */
		public boolean isOrderable() {
			return orderable;
		}

		/**
		 * Sets whether this column may be ordered.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Column setOrderable(boolean v) {
			this.orderable = v;
			return this;
		}

		/**
		 * Returns this column's search descriptor.
		 *
		 * @return The column search, or <jk>null</jk> if none.
		 */
		public Search getSearch() {
			return search;
		}

		/**
		 * Sets this column's search descriptor.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Column setSearch(Search v) {
			this.search = v;
			return this;
		}
	}

	/**
	 * A DataTables ordering descriptor ({@code order[i]}): the index of the ordered column plus its direction.
	 */
	public static class Order {
		private int column = -1;
		private String dir;
		private String name;

		/**
		 * Returns the index (into {@link DataTablesRequest#getColumns() columns}) of the column to order by.
		 *
		 * @return The column index, or {@code -1} if unset.
		 */
		public int getColumn() {
			return column;
		}

		/**
		 * Sets the index of the column to order by.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Order setColumn(int v) {
			this.column = v;
			return this;
		}

		/**
		 * Returns the sort direction ({@code "asc"} or {@code "desc"}).
		 *
		 * @return The direction, or <jk>null</jk> if none.
		 */
		public String getDir() {
			return dir;
		}

		/**
		 * Sets the sort direction.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Order setDir(String v) {
			this.dir = v;
			return this;
		}

		/**
		 * Returns the ordered column's {@code name} (DataTables 2.x echoes {@code columns[column].name} here).  Carried
		 * for wire fidelity; the adapter resolves the column through {@link #getColumn() column}.
		 *
		 * @return The name, or <jk>null</jk> if none.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the ordered column's name.
		 *
		 * @param v The value.
		 * @return This object.
		 */
		public Order setName(String v) {
			this.name = v;
			return this;
		}
	}
}
