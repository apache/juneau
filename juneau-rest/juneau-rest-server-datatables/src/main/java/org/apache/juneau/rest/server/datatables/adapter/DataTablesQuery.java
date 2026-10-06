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
package org.apache.juneau.rest.server.datatables.adapter;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.math.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.rest.server.datatables.*;

/**
 * Maps a DataTables {@link DataTablesRequest request bean} onto a {@link BeanQuery}, runs it against a
 * {@link BeanQuerySession}, and copies the result onto a {@link DataTablesResults} response bean (design §4).
 *
 * <p>
 * This adapter is the only place the two worlds meet: the DataTables JSON beans
 * ({@code org.apache.juneau.rest.server.datatables}) know nothing about the query engine, and the engine knows
 * nothing about DataTables JSON.  The mapping ({@link #toBeanQuery(DataTablesRequest, BeanQueryContext)}):
 * <ul class='spaced-list'>
 * 	<li>Each {@code columns[i].data} resolves against {@link BeanQueryContext#columns() context.columns()}:
 * 		a declared name is used as is, an all-digits string is the n-th declared column, and {@code null}/{@code ""}
 * 		is skipped &mdash; resolution only happens for a column something actually uses (a non-blank per-column
 * 		search, an {@code order[]} entry, or global-search eligibility), so a render-only column with an unresolvable
 * 		{@code data} never errors.
 * 	<li>Each searchable column's non-blank {@code search.value} becomes one raw {@code column=expression} condition
 * 		({@link org.apache.juneau.commons.beanquery.BeanQuery.Builder#search(String, String)}); two DataTables columns resolving to the same key merge
 * 		into one {@code $and}.
 * 	<li>A non-blank global {@code search.value} becomes a top-level {@code $or} of one leaf per eligible column
 * 		(searchable, resolvable), the leaf chosen by that column's {@link SearchType} (#2 §9's
 * 		leaf table): {@code $contains} for {@link SearchType#TEXT}/{@link SearchType#ID}, {@code $prefix} for
 * 		{@link SearchType#VERSION}, {@code $eqic} for {@link SearchType#ENUM}, and {@code $eq} for {@link
 * 		SearchType#NUMERIC}/{@link SearchType#TIMESTAMP}/{@link SearchType#BOOLEAN} when {@code search.value} parses
 * 		for that type &mdash; a parse failure excludes just that column from this request's {@code $or}, it never
 * 		fails the request. Resolved keys are de-duplicated in column order; one eligible column collapses to a
 * 		plain leaf (#2 unwraps a single-item group); an eligible key that also has a per-column search merges into
 * 		that column's {@code $and} (#2 D2).
 * 	<li>Each {@code order[i]} resolves through {@code columns[i]} to a {@link org.apache.juneau.commons.beanquery.BeanQuery.Builder#sort(String) sort} /
 * 		{@link org.apache.juneau.commons.beanquery.BeanQuery.Builder#sortDesc(String) sortDesc} entry; duplicate keys keep the first occurrence.
 * 	<li>{@code start}/{@code length} become the {@link org.apache.juneau.commons.beanquery.BeanQuery.Builder#page(int,int) page} window
 * 		({@code length=-1} means all rows; a negative {@code start} is clamped to {@code 0}).
 * 	<li>Counts are always requested ({@link CountRequest#BOTH}); the context's own {@code CountPolicy} still wins.
 * </ul>
 *
 * <p>
 * {@code columns.size()} and {@code order.size()} are capped by {@link BeanQueryContext#getMaxSearchClauses()} /
 * {@link BeanQueryContext#getMaxSortKeys()} <em>before</em> any column is resolved.  A caller mistake &mdash; an
 * unknown column, too many search clauses, too many sort keys, a malformed expression, a disallowed operator, a
 * restricted column &mdash; raises {@link BeanQuerySyntaxException}, which {@link #run(DataTablesRequest,
 * BeanQuerySession) run} / {@link #runArrays(DataTablesRequest, BeanQuerySession) runArrays} do <b>not</b> catch:
 * it propagates to {@code RestContext.convertThrowable}, which maps it to HTTP&nbsp;<b>400</b> with the server's message
 * in the {@code X-BeanQuery-Error} response header ({@code BeanQueryRequest.ERROR_HEADER}) &mdash; the same contract as
 * every other queryable endpoint.  (This overrides the original 200-plus-{@code error}-field design; 2026-10-05.)
 * The bundled {@code juneau-datatables.js} reads that header and shows the message through DataTables' {@code errMode}
 * instead of a generic "Ajax error".  A
 * {@code BeanQueryExecutionException} (a genuine server fault) propagates unchanged to the REST layer's 500 mapping.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// The §4.1 endpoint shape.</jc>
 * 	<ja>@Rest</ja>(mixins=DataTablesMixin.<jk>class</jk>)
 * 	<jk>public class</jk> ReleasesResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@RestPost</ja>(path=<js>"/query"</js>, parsers=JsonParser.<jk>class</jk>)
 * 		<jk>public</jk> DataTablesResults&lt;Release&gt; queryReleases(<ja>@Content</ja> DataTablesRequest <jv>request</jv>) {
 * 			<jk>try</jk> (var <jv>session</jv> = CONTEXT.getSession(<jv>releases</jv>)) {
 * 				<jk>return</jk> DataTablesQuery.<jsm>run</jsm>(<jv>request</jv>, <jv>session</jv>);
 * 			}
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * A caller that needs to add its own condition (e.g. a tenant guard) builds the query directly and merges it with
 * {@link BeanQuery#copy()}:
 * <p class='bjava'>
 * 	BeanQuery <jv>query</jv> = DataTablesQuery.<jsm>toBeanQuery</jsm>(<jv>request</jv>, <jv>session</jv>.getContext());
 * 	BeanQuery <jv>guarded</jv> = <jv>query</jv>.copy().eq(<js>"tenant"</js>, <jv>tenantId</jv>).build();
 * 	var <jv>page</jv> = <jv>session</jv>.find(<jv>guarded</jv>);
 * 	<jk>return</jk> DataTablesResults.&lt;Release&gt;create().setDraw(<jv>request</jv>.getDraw()).setData(<jv>page</jv>.rows());
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S3776" // Cognitive complexity acceptable: query translation walks each DataTables request section in one pass.
})
public class DataTablesQuery {

	private DataTablesQuery() {}

	/**
	 * Runs the DataTables request against the session and builds the response envelope.
	 *
	 * <p>
	 * The caller owns the session (open it in a try-with-resources block); this method neither opens nor closes it,
	 * so the adapter works identically over an in-memory or a SQL engine.  Rows come back as beans ({@code T}); use
	 * {@link #runArrays(DataTablesRequest, BeanQuerySession) runArrays} when {@code columns[i].data} are integers and
	 * the table expects array rows.
	 *
	 * @param <T> The row type.
	 * @param request The DataTables request bean.  Must not be <jk>null</jk>.
	 * @param session The open bean-query session.  Must not be <jk>null</jk>.
	 * @return The populated DataTables response envelope.
	 * @throws BeanQuerySyntaxException If {@code request} describes an invalid query.  Mapped centrally to HTTP 400
	 * 	with an {@code X-BeanQuery-Error} header.
	 */
	public static <T> DataTablesResults<T> run(DataTablesRequest request, BeanQuerySession<T> session) {
		var query = toBeanQuery(request, session.getContext());
		var page = session.find(query);
		var r = DataTablesResults.<T>create().setDraw(request.getDraw()).setData(page.rows());
		page.total().ifPresentOrElse(r::setRecordsTotal, () -> page.matched().ifPresent(r::setRecordsTotal));
		page.matched().ifPresent(r::setRecordsFiltered);
		return r;
	}

	/**
	 * Runs the DataTables request against the session and builds the response envelope with array rows.
	 *
	 * <p>
	 * For a table whose {@code columns[i].data} are integers (so there is no bean property to put named fields on):
	 * the view is set to every column in {@link BeanQueryContext#columns() context.columns()} order, rows come
	 * back from {@link BeanQuerySession#findValues(BeanQuery) findValues}, and each row {@code Map} is flattened to a
	 * {@code List} in that same order, so {@code data: n} and the n-th array cell refer to the same column.
	 *
	 * @param request The DataTables request bean.  Must not be <jk>null</jk>.
	 * @param session The open bean-query session.  Must not be <jk>null</jk>.
	 * @return The populated DataTables response envelope.
	 * @throws BeanQuerySyntaxException If {@code request} describes an invalid query (see {@link #run(DataTablesRequest,
	 * 	BeanQuerySession) run}).
	 */
	public static DataTablesResults<List<Object>> runArrays(DataTablesRequest request, BeanQuerySession<?> session) {
		var context = session.getContext();
		var columns = context.columns();
		var query = toBeanQuery(request, context).copy().view(columns.toArray(new String[0])).build();
		var page = session.findValues(query);
		var rows = page.rows().stream().map(row -> columns.stream().<Object>map(row::get).toList()).toList();
		var r = DataTablesResults.<List<Object>>create().setDraw(request.getDraw()).setData(rows);
		page.total().ifPresentOrElse(r::setRecordsTotal, () -> page.matched().ifPresent(r::setRecordsTotal));
		page.matched().ifPresent(r::setRecordsFiltered);
		return r;
	}

	/**
	 * Translates a DataTables request into an engine {@link BeanQuery} (search, sort, page window, counts) against
	 * the given context's declared columns (design §4.3&ndash;§4.4).
	 *
	 * @param request The DataTables request bean.  Must not be <jk>null</jk>.
	 * @param context The context the query will run against.  Must not be <jk>null</jk>.  Its {@link
	 * 	BeanQueryContext#columns() columns()} / {@link BeanQueryContext#getColumnType(String) getColumnType(String)}
	 * 	/ {@link BeanQueryContext#getMaxSearchClauses() getMaxSearchClauses()} / {@link
	 * 	BeanQueryContext#getMaxSortKeys() getMaxSortKeys()} drive every resolution and cap below.  A session that
	 * 	narrows its columns ({@code restrictColumns}) is not visible here &mdash; the #1
	 * 	pipeline still rejects a restricted column, reported through the same {@link BeanQuerySyntaxException} path.
	 * @return The equivalent query.
	 * @throws BeanQuerySyntaxException If {@code columns}/{@code order} exceed the context's caps, or a search/sort
	 * 	target names an unresolvable column.
	 */
	public static BeanQuery toBeanQuery(DataTablesRequest request, BeanQueryContext<?> context) {
		var columns = request.getColumns();
		var order = request.getOrder();
		var declared = context.columns();

		// D5: caps checked before any column is resolved.
		var columnCount = columns == null ? 0 : columns.size();
		var orderCount = order == null ? 0 : order.size();
		if (columnCount > context.getMaxSearchClauses())
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.TOO_MANY_CLAUSES, "Too many columns (max %s).", context.getMaxSearchClauses());
		if (orderCount > context.getMaxSortKeys())
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.TOO_MANY_SORT_KEYS, "Too many sort keys (max %s).", context.getMaxSortKeys());

		var b = BeanQuery.create();

		// Per-column search: a non-blank term on a searchable, resolved column becomes one raw condition.
		if (columns != null) {
			for (var c : columns) {
				var value = c.getSearch() == null ? null : c.getSearch().getValue();
				if (c.isSearchable() && ! ib(value)) {
					var key = resolveColumn(declared, c.getData(), true);
					if (key != null)
						b.search(key, value);
				}
			}
		}

		// Global search (D4): one $or of a per-SearchType leaf over every eligible column, per
		// #2's §9 leaf table ($contains for TEXT/ID, $prefix for VERSION, $eqic for ENUM, $eq for
		// NUMERIC/TIMESTAMP/BOOLEAN). TEXT/ID/ENUM/VERSION are always eligible; a NUMERIC/TIMESTAMP/BOOLEAN column
		// is eligible only when globalValue parses for that type (checked once per request, not per row) -- a
		// parse failure excludes just that column, it never fails the request. Resolved keys are de-duplicated in
		// column order first, same as the per-column search above. One eligible column becomes a plain top-level
		// leaf and none eligible adds nothing, so "none eligible" naturally means no search string. If an eligible key
		// also has a per-column search, the leaf merges into that column's $and (#2 D2).
		var globalValue = request.getSearch() == null ? null : request.getSearch().getValue();
		if (inb(globalValue)) {
			var keys = new LinkedHashSet<String>();
			if (columns != null) {
				for (var c : columns) {
					var key = c.isSearchable() ? resolveColumn(declared, c.getData(), false) : null;
					if (key != null)
						keys.add(key);
				}
			}
			var numericOk = looksNumeric(globalValue);
			var booleanOk = looksBoolean(globalValue);
			var timestampOk = looksLikeInstant(globalValue);
			var leaves = new ArrayList<String[]>();  // Each entry is a column key paired with its search operator.
			for (var key : keys) {
				switch (context.getColumnType(key)) {
					case TEXT, ID -> leaves.add(new String[]{key, "$contains"});
					case VERSION -> leaves.add(new String[]{key, "$prefix"});
					case ENUM -> leaves.add(new String[]{key, "$eqic"});
					case NUMERIC -> { if (numericOk) leaves.add(new String[]{key, "$eq"}); }
					case BOOLEAN -> { if (booleanOk) leaves.add(new String[]{key, "$eq"}); }
					case TIMESTAMP -> { if (timestampOk) leaves.add(new String[]{key, "$eq"}); }
					default -> { /* All SearchType values are handled above. */ }
				}
			}
			// WORKAROUND (revert once BeanQuery is fixed): a single leaf is added straight to the top level instead of
			// through b.or(...): the builder merges a top-level leaf with an earlier leaf on the same column into one
			// $and, but an unwrapped single-item
			// group bypasses that merge and would emit the same column twice (a DUPLICATE_COLUMN error).
			if (leaves.size() == 1)
				b.op(leaves.get(0)[0], leaves.get(0)[1], globalValue);
			else if (leaves.size() > 1)
				b.or(g -> leaves.forEach(l -> g.op(l[0], l[1], globalValue)));
		}

		// Ordering: order[i].column indexes columns[]; a non-orderable, blank-data, or duplicate key is skipped.
		if (order != null && columns != null) {
			var seen = new LinkedHashSet<String>();
			for (var o : order) {
				var key = resolveOrderKey(o, columns, declared);
				if (key != null && seen.add(key)) {
					if (eqic(o.getDir(), "desc"))
						b.sortDesc(key);
					else
						b.sort(key);
				}
			}
		}

		// Paging: length=-1 means all rows (passed through); a negative start is clamped rather than rejected.
		b.page(Math.max(request.getStart(), 0), request.getLength());

		// Counts: always requested; the context's own CountPolicy (NONE/MATCHED) still wins (#1 D3).
		b.counts(CountRequest.BOTH);

		return b.build();
	}

	/** Resolves an {@code order[]} entry to its column key, or <jk>null</jk> if the entry is out of range, non-orderable or blank. */
	private static String resolveOrderKey(DataTablesRequest.Order o, List<DataTablesRequest.Column> columns, List<String> declared) {
		var ci = o.getColumn();
		if (ci < 0 || ci >= columns.size())
			return null;
		var c = columns.get(ci);
		return c.isOrderable() ? resolveColumn(declared, c.getData(), true) : null;
	}

	/**
	 * Resolves one {@code columns[i].data} string to a declared column key, per design §4.3.
	 *
	 * @param declared The context's declared column names, in order.
	 * @param data The raw {@code data} string (may be <jk>null</jk>).
	 * @param used Whether this column is actually used (a non-blank search term or an order target).  When
	 * 	<jk>false</jk>, an unresolvable name is excluded (returns <jk>null</jk>) instead of raising &mdash; this is
	 * 	what lets global-search eligibility skip a render-only column instead of failing the whole request.
	 * @return The resolved key, or <jk>null</jk> if {@code data} is blank or (when {@code used} is <jk>false</jk>)
	 * 	unresolvable.
	 * @throws BeanQuerySyntaxException If {@code used} is <jk>true</jk> and {@code data} does not resolve.
	 */
	private static String resolveColumn(List<String> declared, String data, boolean used) {
		var trimmed = tr(data);
		if (ib(trimmed))
			return null;
		if (declared.contains(trimmed))
			return trimmed;
		if (isAllDigits(trimmed)) {
			try {
				var i = Integer.parseInt(trimmed);
				if (i < declared.size())
					return declared.get(i);
			} catch (NumberFormatException e) {
				// Too large for an int: cannot be a valid index, fall through to the unknown-column handling below.
			}
		}
		if (used)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_COLUMN, "Unknown column '%s'.", trimmed);
		return null;
	}

	/** Returns whether {@code s} is non-empty and every character is an ASCII digit (so {@code "-1"} is not). */
	private static boolean isAllDigits(String s) {
		return ! s.isEmpty() && s.chars().allMatch(ch -> ch >= '0' && ch <= '9');
	}

	/** Whether a global-search term is eligible for the global-search {@code NUMERIC} leaf (D4): only parseability
	 * gates eligibility &mdash; the leaf itself binds the raw, unparsed term.  Uses {@link BigDecimal}, the same
	 * parse the engine applies to a {@code NUMERIC} value, so Java-only {@code Double} forms ({@code 1d},
	 * {@code NaN}, {@code Infinity}, hex floats) are excluded instead of failing downstream. */
	private static boolean looksNumeric(String s) {
		try {
			// Constructed only for its NumberFormatException.
			Objects.requireNonNull(new BigDecimal(s.strip()));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/** Whether a global-search term is eligible for the global-search {@code BOOLEAN} leaf (D4): {@code true}/
	 * {@code false}, case-insensitively, nothing else. */
	private static boolean looksBoolean(String s) {
		var t = s.strip();
		return eqic(t, "true") || eqic(t, "false");
	}

	/** Whether a global-search term is eligible for the global-search {@code TIMESTAMP} leaf (D4): the same
	 * ISO-8601 instant form the engine's own value formatting emits. */
	private static boolean looksLikeInstant(String s) {
		try {
			Instant.parse(s.strip());
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}
}
