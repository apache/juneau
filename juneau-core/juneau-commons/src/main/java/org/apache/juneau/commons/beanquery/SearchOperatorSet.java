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

import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * An immutable, ordered collection of {@link SearchOperator}s that governs which {@code $}-operators the parser
 * recognizes.
 *
 * <p>
 * {@link #standard()} is the built-in set (the confirmed operators with their arity and per-type applicability).
 * Applications extend it with {@link #with(SearchOperator)} (add a custom operator to a copy) or build a bespoke set
 * with {@link #of(SearchOperator...)}. A context uses one set as its default and may hold a per-column map; a
 * per-column set <b>replaces</b> the default for that column rather than merging with it.
 *
 * <p>
 * Juneau uses the spellings {@code $prefix} and {@code $blank} as first-class names; there are deliberately <b>no</b>
 * {@code $starts}/{@code $empty} aliases.
 *
 * <h5 class='section'>Case-insensitive names</h5>
 * <p>
 * Lookup ({@link #get(String)}, {@link #contains(String)}, {@link #without(String)}) is case-insensitive: {@code
 * $EQ}, {@code $Eq} and {@code $eq} all resolve to the same operator. {@link #of(SearchOperator...)} rejects two
 * operators whose names differ only in case; {@link #with(SearchOperator)} replaces a same-name-ignoring-case
 * operator, the same way it replaces an exact-name one. {@link #operators()} and {@link #get(String)} always expose
 * the operator's own canonical (registered) name, never the casing a caller looked it up with.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Hide $regex unless a caller opts in.</jc>
 * 	SearchOperatorSet <jv>restricted</jv> = SearchOperatorSet.<jsm>standard</jsm>().without(<js>"$regex"</js>);
 *
 * 	<jc>// $EQ resolves to the same operator as $eq.</jc>
 * 	SearchOperator <jv>eq</jv> = <jv>restricted</jv>.get(<js>"$EQ"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class SearchOperatorSet {

	private static final SearchOperatorSet STANDARD;
	static {
		var ops = new SearchOperator[] {
			SearchOperator.builtin("$eq", 1, 1, false,
				"Exact, case-sensitive match. Example: $eq(OPEN)",
				TEXT, ID, NUMERIC, VERSION, TIMESTAMP, ENUM, BOOLEAN),
			SearchOperator.builtin("$eqic", 1, 1, false,
				"Case-insensitive match. Example: $eqic(open)",
				TEXT, ID, ENUM),
			SearchOperator.builtin("$ne", 1, -1, false,
				"None of the given values, case-sensitive (1 or more). Blank cells are kept. Example: $ne(OPEN,CLOSED)",
				TEXT, ID, NUMERIC, VERSION, TIMESTAMP, ENUM, BOOLEAN),
			SearchOperator.builtin("$in", 1, -1, false,
				"Any of the given values, case-sensitive (1 or more). Example: $in(OPEN,CLOSED)",
				TEXT, ID, NUMERIC, VERSION, TIMESTAMP, ENUM),
			SearchOperator.builtin("$and", 2, -1, true,
				"All of the given sub-expressions match (2 or more). Example: $and($gt(1),$lt(9))"),
			SearchOperator.builtin("$or", 2, -1, true,
				"Any of the given sub-expressions matches (2 or more). Example: $or($eq(OPEN),$eq(CLOSED))"),
			SearchOperator.builtin("$not", 1, 1, true,
				"Negates a single sub-expression. Example: $not($eq(OPEN))"),
			SearchOperator.builtin("$contains", 1, 1, false,
				"Case-insensitive substring match. Example: $contains(err)",
				TEXT, ID),
			SearchOperator.builtin("$prefix", 1, 1, false,
				"Prefix match (string prefix, or dotted-tuple prefix for versions). Example: $prefix(10.0)",
				TEXT, ID, VERSION),
			SearchOperator.builtin("$regex", 1, 2, false,
				"Full-string regular expression, case-insensitive by default. Portable flags i/m/s. Example: $regex(err.*, flags=i)",
				TEXT, ID),
			SearchOperator.builtin("$blank", 0, 0, false,
				"Matches an empty or whitespace-only cell. Takes no arguments. Example: $blank()",
				TEXT, ID),
			SearchOperator.builtin("$gt", 1, 1, false,
				"Greater than. Example: $gt(100)",
				NUMERIC, VERSION, TIMESTAMP),
			SearchOperator.builtin("$gte", 1, 1, false,
				"Greater than or equal. Example: $gte(100)",
				NUMERIC, VERSION, TIMESTAMP),
			SearchOperator.builtin("$lt", 1, 1, false,
				"Less than. Example: $lt(100)",
				NUMERIC, VERSION, TIMESTAMP),
			SearchOperator.builtin("$lte", 1, 1, false,
				"Less than or equal. Example: $lte(100)",
				NUMERIC, VERSION, TIMESTAMP),
			SearchOperator.builtin("$between", 2, 2, false,
				"Inclusive range between two bounds. Example: $between(1, 100)",
				NUMERIC, VERSION, TIMESTAMP),
		};
		var m = new LinkedHashMap<String,SearchOperator>();
		for (var op : ops)
			put(m, op);
		STANDARD = new SearchOperatorSet(m);
	}

	private final Map<String,SearchOperator> operators;

	private SearchOperatorSet(Map<String,SearchOperator> operators) {
		this.operators = Collections.unmodifiableMap(new LinkedHashMap<>(operators));
	}

	/** Lower-cases a name for use as a map key (D6: lookup is case-insensitive; {@code Locale.ROOT}). */
	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	/**
	 * The built-in operator set.
	 *
	 * @return The standard set, never <jk>null</jk>.
	 */
	public static SearchOperatorSet standard() {
		return STANDARD;
	}

	/**
	 * Builds a set from exactly the specified operators (no built-ins are implied).
	 *
	 * @param operators The operators, in the order to expose them. Must not be <jk>null</jk>; names must be unique
	 * 	ignoring case.
	 * @return A new set.
	 */
	public static SearchOperatorSet of(SearchOperator...operators) {
		req(operators != null, "SearchOperatorSet.of operators must not be null.");
		var m = new LinkedHashMap<String,SearchOperator>();
		for (var op : operators)
			put(m, op);
		return new SearchOperatorSet(m);
	}

	/**
	 * Returns a copy of this set with the specified operator added, replacing a same-name-ignoring-case operator if
	 * one is already present.
	 *
	 * @param op The operator to add. Must not be <jk>null</jk>.
	 * @return A new set; this set is unchanged.
	 */
	public SearchOperatorSet with(SearchOperator op) {
		req(op != null, "SearchOperatorSet.with operator must not be null.");
		var m = new LinkedHashMap<>(operators);
		m.put(key(op.name()), op);
		return new SearchOperatorSet(m);
	}

	/**
	 * Returns a copy of this set without the named operator.
	 *
	 * @param name The operator name (for example {@code "$regex"}), matched ignoring case. Can be <jk>null</jk>.
	 * @return A new set, or this set if it has no operator with that name.
	 */
	public SearchOperatorSet without(String name) {
		if (! contains(name))
			return this;
		var m = new LinkedHashMap<>(operators);
		m.remove(key(name));
		return new SearchOperatorSet(m);
	}

	private static void put(Map<String,SearchOperator> m, SearchOperator op) {
		req(op != null, "SearchOperatorSet operator must not be null.");
		var key = key(op.name());
		var existing = m.get(key);
		if (existing != null) {
			if (eq(existing.name(), op.name()))
				req(false, "SearchOperatorSet contains a duplicate operator name: '%s'", op.name());
			req(false, "SearchOperatorSet contains operators whose names differ only in case: '%s' and '%s'.", existing.name(), op.name());
		}
		m.put(key, op);
	}

	/**
	 * Looks up an operator by name, ignoring case.
	 *
	 * @param name The operator name, including the leading {@code $} (e.g. {@code "$eq"} or {@code "$EQ"}). Can be
	 * 	<jk>null</jk>.
	 * @return The operator (with its canonical, registered-case name), or <jk>null</jk> if not in this set.
	 */
	public SearchOperator get(String name) {
		return name == null ? null : operators.get(key(name));
	}

	/**
	 * Whether this set contains an operator with the specified name, ignoring case.
	 *
	 * @param name The operator name. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if present.
	 */
	public boolean contains(String name) {
		return name != null && operators.containsKey(key(name));
	}

	/**
	 * All operators in this set, in canonical order.
	 *
	 * @return An unmodifiable list.
	 */
	public List<SearchOperator> operators() {
		return List.copyOf(operators.values());
	}

	/**
	 * The operators in this set offered for the specified value type, including the combinators (which apply to every
	 * type).
	 *
	 * @param type The column value type. Can be <jk>null</jk> (yields an empty list).
	 * @return An unmodifiable list of applicable operators, in canonical order.
	 */
	public List<SearchOperator> forType(SearchType type) {
		var l = new ArrayList<SearchOperator>();
		if (type != null)
			for (var op : operators.values())
				if (op.appliesTo(type))
					l.add(op);
		return List.copyOf(l);
	}
}
