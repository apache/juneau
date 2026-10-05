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
import java.util.function.*;

/**
 * Folds a parsed {@link SearchItem} tree (as produced by {@link SearchParser#parse(String)}) into one
 * {@link Filter}, resolving each leaf's raw {@code (column, expression)} pair with a caller-supplied resolver.
 * {@code QueryResolver} is expected to call {@link #resolve} once per query, after the raw {@code search} string
 * has been parsed and before engine dispatch; {@link #countLeaves} supports the {@code maxSearchClauses} check at
 * any nesting depth.
 */
final class FilterResolver {

	private FilterResolver() {}

	/**
	 * Resolves {@code items} into one {@link Filter}. More than one top-level item folds into a {@link Filter.And};
	 * exactly one item is returned unwrapped; an empty list resolves to an empty {@link Filter.And}, never
	 * <jk>null</jk>, which matches every row.
	 *
	 * @param items The parsed top-level items.
	 * @param leafResolver Resolves one leaf's {@code (column, expression)} pair into a {@link Filter.Leaf} (parsing
	 * 	the expression with the column's operator set and attaching the column's {@link SearchType}), or returns
	 * 	<jk>null</jk> to signal the leaf should be dropped (e.g. a blank expression on an otherwise-valid column).
	 * 	A dropped leaf propagates: an {@code $and}/{@code $or} group left with no resolved children is itself
	 * 	dropped, and {@code $not} of a dropped item is dropped.
	 * @return The resolved filter.
	 */
	static Filter resolve(List<SearchItem> items, BiFunction<String,String,Filter.Leaf> leafResolver) {
		var resolved = resolveAll(items, leafResolver);
		if (resolved.isEmpty())
			return new Filter.And(List.of());
		if (resolved.size() == 1)
			return resolved.get(0);
		return new Filter.And(resolved);
	}

	/** Counts {@link SearchItem.Leaf}s at any nesting depth (groups do not themselves count). */
	static int countLeaves(List<SearchItem> items) {
		var n = 0;
		for (var item : items)
			n += countLeaves(item);
		return n;
	}

	private static List<Filter> resolveAll(List<SearchItem> items, BiFunction<String,String,Filter.Leaf> leafResolver) {
		var out = new ArrayList<Filter>();
		for (var item : items) {
			var resolved = resolveOne(item, leafResolver);
			if (resolved != null)
				out.add(resolved);
		}
		return out;
	}

	/** Resolves one item, or <jk>null</jk> if it (or everything inside it) was dropped &mdash; a blank leaf on a
	 * known column, or a group left with no items after its own children were dropped. */
	private static Filter resolveOne(SearchItem item, BiFunction<String,String,Filter.Leaf> leafResolver) {
		if (item instanceof SearchItem.Leaf leaf)
			return leafResolver.apply(leaf.column(), leaf.expression());
		if (item instanceof SearchItem.And and) {
			var resolved = resolveAll(and.items(), leafResolver);
			return resolved.isEmpty() ? null : new Filter.And(resolved);
		}
		if (item instanceof SearchItem.Or or) {
			var resolved = resolveAll(or.items(), leafResolver);
			return resolved.isEmpty() ? null : new Filter.Or(resolved);
		}
		if (item instanceof SearchItem.Not not) {
			var resolved = resolveOne(not.item(), leafResolver);
			return resolved == null ? null : new Filter.Not(resolved);
		}
		throw new IllegalStateException("Unknown SearchItem type: " + item);
	}

	private static int countLeaves(SearchItem item) {
		if (item instanceof SearchItem.Leaf)
			return 1;
		if (item instanceof SearchItem.And and)
			return countLeaves(and.items());
		if (item instanceof SearchItem.Or or)
			return countLeaves(or.items());
		if (item instanceof SearchItem.Not not)
			return countLeaves(not.item());
		throw new IllegalStateException("Unknown SearchItem type: " + item);
	}
}
