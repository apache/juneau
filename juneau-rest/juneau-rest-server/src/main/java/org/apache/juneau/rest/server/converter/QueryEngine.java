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

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.lang.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.rest.server.*;

/**
 * Protocol-agnostic driver for the shared {@link ObjectSearcher}/{@link ObjectSorter}/{@link ObjectViewer}/{@link ObjectPaginator}
 * query engine.
 *
 * <p>
 * Applies the normalized {@link QueryArgs} to a response POJO in the fixed order
 * <c>search &rarr; search-any (OR) &rarr; sort &rarr; view &rarr; page</c>, capturing the pre-paging counts a
 * {@link QueryProtocol} needs to build a response envelope.  The <c>search &rarr; sort &rarr; view &rarr; page</c>
 * ordering matches the historical {@code Queryable} behavior exactly, so the native protocol is byte-for-byte
 * back-compatible.
 *
 * @since 10.0.0
 */
final class QueryEngine {

	private QueryEngine() {}

	/**
	 * Runs the engine over the specified response object.
	 *
	 * @param req The incoming request (supplies the marshalling session).
	 * @param o The response object.  Must not be <jk>null</jk> (callers short-circuit <jk>null</jk> responses).
	 * @param args The normalized query arguments.
	 * @return The filtered/paged data plus the pre-paging counts.
	 */
	static QueryResult run(RestRequest req, Object o, QueryArgs args) {
		var bs = req.getMarshallingSession();

		var recordsTotal = count(o);

		var v = Holder.of(o);
		args.getSearch().ifPresent(x -> v.set(search(bs, v.get(), x)));
		args.getSearchAny().ifPresent(x -> v.set(searchAny(bs, v.get(), x)));

		// Captured immediately after the search stages: recordsFiltered is defined as the size after search filtering.
		// The sort/view stages that follow are a reorder/projection and don't change the row count today, but capturing
		// the count here keeps it correct if a future view stage ever drops rows.
		var recordsFiltered = count(v.get());

		args.getSort().ifPresent(x -> v.set(ObjectSorter.create().run(bs, v.get(), x)));
		args.getView().ifPresent(x -> v.set(ObjectViewer.create().run(bs, v.get(), x)));

		args.getPage().ifPresent(x -> v.set(ObjectPaginator.create().run(bs, v.get(), x)));

		return new QueryResult(v.get(), recordsTotal, recordsFiltered);
	}

	/**
	 * Per-column (AND) search stage.
	 *
	 * <p>
	 * A search pattern whose type can't match a column (e.g. a text term against a numeric column, which the matcher
	 * rejects with a {@link PatternException}) or a malformed client regex ({@link PatternSyntaxException}, only
	 * reachable when a protocol opts into regex search) is a client error, not a server fault &mdash; so it's mapped to
	 * an HTTP 400 rather than escaping as a 500.  This matters most for protocols (e.g. DataTables) that expose a
	 * per-column search box to end users, where typing text into a numeric column's box is a routine action.
	 */
	private static Object search(MarshallingSession bs, Object o, SearchArgs args) {
		try {
			return ObjectSearcher.create().run(bs, o, args);
		} catch (PatternException | PatternSyntaxException e) {
			throw new BadRequest(e, "Invalid search pattern: %s", e.getMessage());
		}
	}

	/**
	 * Cross-column OR filter: keeps rows (in original order) matching <b>any</b> of the specified single-column filters.
	 *
	 * <p>
	 * Each entry is run through {@link ObjectSearcher} independently against the whole input and the surviving rows
	 * are unioned by identity, so the semantics are a true OR across columns (which the engine's per-column AND model
	 * cannot express in a single pass).
	 */
	private static Object searchAny(MarshallingSession bs, Object o, Map<String,String> filters) {
		// A non-collection/array response can't be row-filtered, so it's returned as-is.  filters is guaranteed
		// non-empty here: searchAny() is only invoked via getSearchAny().ifPresent(...), and that Optional is present
		// only when the map has at least one entry.
		var rows = asList(o);
		if (rows == null)
			return o;

		Set<Object> matched = Collections.newSetFromMap(new IdentityHashMap<>());
		filters.forEach((col, pattern) -> {
			// A global (OR) term is applied to every searchable column, including columns whose type can't match
			// the term (e.g. a text term against a numeric/date column, which the matcher rejects with a
			// PatternException).  For an OR union that simply means "this column contributes no matches", so a
			// per-column type-mismatch failure is swallowed rather than failing the whole request.  A malformed
			// client regex (PatternSyntaxException, only reachable when the protocol opts into regex search) is a
			// client error, so it's surfaced as a 400 instead.
			try {
				// ObjectSearcher.run always yields a Collection for a Collection input (the filtered list, or the
				// input unchanged), so the union add is safe without an instanceof guard.
				var sub = (Collection<?>) ObjectSearcher.create().run(bs, rows, SearchArgs.create(List.of(col + "=" + pattern)));
				matched.addAll(sub);
			} catch (PatternSyntaxException e) {
				throw new BadRequest(e, "Invalid search pattern: %s", e.getMessage());
			} catch (PatternException e) {
				// Column type can't match this term - contributes nothing to the OR.
			}
		});

		var out = new ArrayList<>(matched.size());
		for (var row : rows)
			if (matched.contains(row))
				out.add(row);
		return out;
	}

	/**
	 * Returns the specified value as a {@code List} if it is a collection or array, else <jk>null</jk>.
	 *
	 * <p>
	 * {@code o} is always non-null: it originates from a non-null response ({@link ProtocolQueryable} short-circuits
	 * null responses) and every engine stage returns non-null for non-null input.
	 */
	private static List<Object> asList(Object o) {
		if (o instanceof Collection<?> c)
			return new ArrayList<>(c);
		if (o.getClass().isArray()) {
			var size = Array.getLength(o);
			var l = new ArrayList<>(size);
			for (var i = 0; i < size; i++)
				l.add(Array.get(o, i));
			return l;
		}
		return null;
	}

	/**
	 * Returns the number of rows in a collection/array, or <c>-1</c> for anything else.
	 *
	 * <p>
	 * {@code o} is always non-null (see {@link #asList(Object)}).
	 */
	private static int count(Object o) {
		if (o instanceof Collection<?> c)
			return c.size();
		if (o.getClass().isArray())
			return Array.getLength(o);
		return -1;
	}
}
