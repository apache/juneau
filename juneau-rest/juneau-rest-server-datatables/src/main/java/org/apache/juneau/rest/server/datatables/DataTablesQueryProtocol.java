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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.converter.*;

/**
 * A {@link QueryProtocol} implementing the
 * <a class="doclink" href="https://datatables.net/manual/server-side">DataTables server-side processing</a> wire contract
 * over Juneau's shared query engine.
 *
 * <p>
 * Maps the DataTables request parameters onto normalized {@link QueryArgs} and wraps the engine result in a
 * {@link DataTablesResults} envelope:
 *
 * <table class='styled'>
 * 	<tr><th>DataTables request</th><th>Mapped to</th></tr>
 * 	<tr><td><c>start</c> / <c>length</c></td><td>{@link PageArgs} ({@code length=-1} means "all", i.e. no paging)</td></tr>
 * 	<tr><td><c>order[i][column]</c> / <c>order[i][dir]</c></td><td>{@link SortArgs} (resolved through <c>columns[i][data]</c>)</td></tr>
 * 	<tr><td><c>columns[i][search][value]</c></td><td>per-column {@link SearchArgs} (AND across columns)</td></tr>
 * 	<tr><td><c>search[value]</c></td><td>cross-column OR search across all searchable columns ({@link QueryArgs.Builder#searchAny})</td></tr>
 * </table>
 *
 * <h5 class='section'>Search-term mapping:</h5>
 * <p>
 * A DataTables search value {@code "Bill"} is handed to the engine as the <b>quoted</b> contains-style pattern
 * {@code "'*Bill*'"}.  The single-quoted form makes the match <b>case-insensitive</b> (what DataTables users expect
 * of a search box) and treats internal whitespace <b>literally</b>, so a multi-word term such as {@code "Bill Smith"}
 * is matched as a literal substring rather than as Juneau's OR-within-column tokens.  Any single-quote in the term is
 * escaped so it can't break out of the quoting.
 *
 * <p>
 * Regular-expression search ({@code search[regex]=true} / {@code columns[i][search][regex]=true}) is <b>opt-in</b> and
 * gated behind {@link QueryableSettings#allowRegexSearch()} (default <jk>false</jk>) to close the ReDoS/CPU-burn vector
 * of compiling and running arbitrary caller regexes against every row &times; column.  When the flag is off, a
 * {@code regex=true} term is treated as the same literal case-insensitive substring search.  When the flag is on, the
 * term is handed to the engine as a slash-delimited regex ({@code "/Bill/"}) and a malformed pattern yields an HTTP 400
 * (not a 500).
 *
 * <h5 class='section'>Column-key resolution:</h5>
 * <p>
 * Each column's key is resolved from {@code columns[i][data]} (a non-numeric value) or, failing that,
 * {@code columns[i][name]}.  DataTables' <b>array data source</b> instead sends the numeric array index as
 * {@code columns[i][data]=0,1,2…} with no {@code name} &mdash; a shape that carries no bean-property information.  When
 * this protocol is constructed with a row type ({@link #DataTablesQueryProtocol(Class)}), such a numeric index is
 * mapped <b>positionally</b> to the Nth readable bean property (bean-property order, matching {@link DataTablesColumns}).
 * Without a row type, an array-index column has no resolvable key and is simply ignored rather than silently disabling
 * all filtering &mdash; so prefer object (named) data sources, or supply the row type, when using this protocol.
 *
 * <h5 class='section'>Parse limits:</h5>
 * <p>
 * The number of parsed {@code columns[i]} / {@code order[i]} descriptors is capped by
 * {@link QueryableSettings#maxColumns()} / {@link QueryableSettings#maxOrderColumns()} (defaults 64 / 8) to bound the
 * O(columns &times; params) parse cost against a linear-scan parameter list, and identical global-search columns are
 * de-duplicated before the per-column engine scan.
 *
 * <h5 class='section'>Versioning (OQ3a):</h5>
 * <p>
 * The DataTables server-side wire contract is small and has stayed backward-compatible across DataTables 1.10 → 2.x,
 * so this adapter versions at the <b>class level</b>, not the Maven-module level.  {@code DataTablesQueryProtocol} is
 * the current protocol; should DataTables ever ship a breaking wire change, a sibling class (e.g. {@code DataTables2QueryProtocol})
 * would be added to this same module rather than a new {@code -vNNNN} artifact.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=ProtocolQueryable.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> QueryableSettings queryableSettings() {
 * 			<jk>return</jk> QueryableSettings.<jsm>create</jsm>().protocol(<jk>new</jk> DataTablesQueryProtocol()).build();
 * 		}
 *
 * 		<ja>@RestGet</ja>
 * 		<jk>public</jk> List&lt;MyRow&gt; getRows() {...}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DataTablesResults}
 * 	<li class='jc'>{@link QueryProtocol}
 * 	<li class='jc'>{@link ProtocolQueryable}
 * 	<li class='jc'>{@link QueryableSettings}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class DataTablesQueryProtocol implements QueryProtocol {

	private final Class<?> rowType;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Array-index (numeric {@code columns[i][data]}) column sources with no {@code name} cannot be mapped to a bean
	 * property and are ignored; use {@link #DataTablesQueryProtocol(Class)} to enable positional resolution.
	 */
	public DataTablesQueryProtocol() {
		this(null);
	}

	/**
	 * Constructor that enables positional resolution of array-index column sources.
	 *
	 * @param rowType
	 * 	The row bean type whose readable-property order resolves a numeric {@code columns[i][data]} index (an array
	 * 	data source) to a bean property.  May be <jk>null</jk> to disable positional resolution.
	 */
	public DataTablesQueryProtocol(Class<?> rowType) {
		this.rowType = rowType;
	}

	@Override /* Overridden from QueryProtocol */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable: linear translation of the DataTables request contract.
	})
	public QueryArgs parse(RestRequest req) {
		var p = req.getQueryParams();
		var b = QueryArgs.create();
		var settings = settings(req);
		var allowRegex = settings.allowRegexSearch();
		var positionalKeys = positionalKeys();

		// Column descriptors (columns[i][data|name|searchable|orderable|search[value]|search[regex]]), capped.
		var cols = new ArrayList<Column>();
		for (var i = 0; i < settings.maxColumns() && p.contains("columns[" + i + "][data]"); i++) {
			var data = p.get("columns[" + i + "][data]").asString().orElse("");
			var name = p.get("columns[" + i + "][name]").asString().orElse("");
			var key = resolveKey(data, name, positionalKeys);
			var searchable = p.get("columns[" + i + "][searchable]").asString().map(Boolean::parseBoolean).orElse(Boolean.TRUE);
			var orderable = p.get("columns[" + i + "][orderable]").asString().map(Boolean::parseBoolean).orElse(Boolean.TRUE);
			var colSearch = p.get("columns[" + i + "][search][value]").asString().orElse("");
			var colRegex = p.get("columns[" + i + "][search][regex]").asString().map(Boolean::parseBoolean).orElse(Boolean.FALSE);
			cols.add(new Column(key, searchable, orderable, colSearch, colRegex));
		}

		// Per-column (AND) search.
		var and = new ArrayList<String>();
		for (var c : cols)
			if (c.searchable && !c.key.isEmpty() && !c.search.isEmpty())
				and.add(c.key + "=" + pattern(c.search, c.regex, allowRegex));
		if (! and.isEmpty())
			b.search(SearchArgs.create(and));

		// Global (OR) search across searchable columns (de-duplicated by key so a repeated column isn't scanned twice).
		var global = p.get("search[value]").asString().orElse("");
		var globalRegex = p.get("search[regex]").asString().map(Boolean::parseBoolean).orElse(Boolean.FALSE);
		if (! global.isEmpty()) {
			var seen = new HashSet<String>();
			for (var c : cols)
				if (c.searchable && ! c.key.isEmpty() && seen.add(c.key))
					b.searchAny(c.key, pattern(global, globalRegex, allowRegex));
		}

		// Ordering (capped; an order referencing a non-orderable or empty-key column is skipped).
		var sort = new ArrayList<String>();
		for (var j = 0; j < settings.maxOrderColumns() && p.contains("order[" + j + "][column]"); j++) {
			var ci = p.get("order[" + j + "][column]").asInteger().orElse(-1);
			var dir = p.get("order[" + j + "][dir]").asString().orElse("asc");
			if (ci >= 0 && ci < cols.size()) {
				var c = cols.get(ci);
				if (c.orderable && ! c.key.isEmpty())
					sort.add(c.key + ("desc".equalsIgnoreCase(dir) ? "-" : "+"));
			}
		}
		if (! sort.isEmpty())
			b.sort(SortArgs.create(sort));

		// Paging (length=-1 => all rows; absent => no paging).
		var length = p.get("length").asInteger().orElse(null);
		var start = p.get("start").asInteger().orElse(0);
		if (length != null && length >= 0)
			b.page(PageArgs.create(start, length));

		return b.build();
	}

	@Override /* Overridden from QueryProtocol */
	public Object wrap(RestRequest req, QueryResult result) {
		var draw = req.getQueryParams().get("draw").asInteger().orElse(0);
		var data = toList(result.getData());
		var total = result.getRecordsTotal() < 0 ? data.size() : result.getRecordsTotal();
		var filtered = result.getRecordsFiltered() < 0 ? data.size() : result.getRecordsFiltered();
		return DataTablesResults.create()
			.setDraw(draw)
			.setRecordsTotal(total)
			.setRecordsFiltered(filtered)
			.setData(data);
	}

	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this only borrows a bean and must not close it.
	})
	private static QueryableSettings settings(RestRequest req) {
		return req.getContext().getBeanStore().getBean(QueryableSettings.class).orElse(QueryableSettings.DEFAULT);
	}

	/** The readable bean-property names of {@link #rowType} (in bean order), or <jk>null</jk> if no row type was supplied. */
	private List<String> positionalKeys() {
		if (rowType == null)
			return null;
		var out = new ArrayList<String>();
		for (var col : DataTablesColumns.of(rowType))
			out.add((String) col.get("data"));
		return out;
	}

	/**
	 * Resolves a column key from its {@code data} / {@code name} descriptors.
	 *
	 * <p>
	 * A non-numeric {@code data} wins; else a non-empty {@code name}; else (for an array-index numeric {@code data})
	 * the Nth readable bean property when a row type is known; else empty (the column is ignored).
	 */
	private static String resolveKey(String data, String name, List<String> positionalKeys) {
		if (! data.isEmpty() && ! isNumeric(data))
			return data;
		if (! name.isEmpty())
			return name;
		if (! data.isEmpty() && positionalKeys != null) {
			// data is all-digits here (non-numeric data returned above), so the parsed index is always >= 0.
			var index = Integer.parseInt(data);
			if (index < positionalKeys.size())
				return positionalKeys.get(index);
		}
		return "";
	}

	/**
	 * Wraps a DataTables search value as a Juneau {@link ObjectSearcher} pattern.
	 *
	 * <p>
	 * A non-regex term (or a regex term when {@code allowRegex} is off) becomes a quoted {@code '*term*'} pattern:
	 * case-insensitive, whitespace-literal, with any embedded single-quote escaped.  A regex term with
	 * {@code allowRegex} on becomes a slash-delimited {@code /term/} full-match regex.
	 *
	 * <p>
	 * Package-private for direct unit testing of all flavors.
	 */
	static String pattern(String value, boolean regex, boolean allowRegex) {
		if (regex && allowRegex)
			return "/" + value + "/";
		return "'*" + value.replace("'", "\\'") + "*'";
	}

	// Only ever called with a non-empty string, so no empty guard.
	private static boolean isNumeric(String s) {
		return s.chars().allMatch(Character::isDigit);
	}

	/**
	 * Normalizes the engine result into a {@code List} for the envelope's <c>data</c> array.
	 *
	 * <p>
	 * Package-private for direct unit testing of the collection/array/scalar/null branches.
	 */
	static List<Object> toList(Object o) {
		if (o == null)
			return new ArrayList<>(0);
		if (o instanceof List<?> l) {
			@SuppressWarnings("unchecked")
			var l2 = (List<Object>)l;
			return l2;
		}
		if (o instanceof Collection<?> c)
			return new ArrayList<>(c);
		if (o.getClass().isArray()) {
			var size = Array.getLength(o);
			var l = new ArrayList<>(size);
			for (var i = 0; i < size; i++)
				l.add(Array.get(o, i));
			return l;
		}
		return new ArrayList<>(List.of(o));
	}

	/**
	 * Immutable column descriptor parsed from the {@code columns[i][...]} request parameters.
	 */
	private static final class Column {
		final String key;
		final boolean searchable;
		final boolean orderable;
		final String search;
		final boolean regex;

		Column(String key, boolean searchable, boolean orderable, String search, boolean regex) {
			this.key = key;
			this.searchable = searchable;
			this.orderable = orderable;
			this.search = search;
			this.regex = regex;
		}
	}
}
