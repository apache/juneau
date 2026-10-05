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

import java.time.*;
import java.util.*;

/**
 * The validation pipeline (spec §5): turns a raw {@link BeanQuery} into a {@link ResolvedQuery}, or throws
 * {@link BeanQuerySyntaxException} with a caller-facing message.  Checks run in the spec's order.
 */
final class QueryResolver {

	private QueryResolver() {}

	/** Built-in leaf operators whose arguments are typed by column type (design #4 section 5). */
	private static final Set<String> VALUE_OPS = Set.of("$eq", "$ne", "$in", "$gt", "$gte", "$lt", "$lte", "$between");

	static ResolvedQuery resolve(BeanQuery query, QuerySettings s, BeanQueryContext<?> origin) {
		return resolve(query, s, origin, Clock.systemUTC());
	}

	// The clock is read exactly once, here; everything below takes the captured Instant so every relative-duration
	// literal in one query (e.g. two leaves, or both ends of $between(-7d,-1d)) resolves against the identical instant.
	static ResolvedQuery resolve(BeanQuery query, QuerySettings s, BeanQueryContext<?> origin, Clock clock) {
		reqnn("query", query);
		reqnn("clock", clock);
		checkLength(BeanQuerySyntaxException.Code.SEARCH_TOO_LONG, "Search", query.getSearch(), s.maxSearchLength);
		checkLength(BeanQuerySyntaxException.Code.SEARCH_TOO_LONG, "Sort", query.getSort(), s.maxSearchLength);
		checkLength(BeanQuerySyntaxException.Code.SEARCH_TOO_LONG, "View", query.getView(), s.maxSearchLength);
		checkLength(BeanQuerySyntaxException.Code.OPTS_TOO_LONG, "Options", query.getOpts(), s.maxSearchLength);
		var requestTime = clock.instant();
		var filter = filter(query.getSearch(), s, requestTime);
		var sort = sort(query.getSort(), s);
		var view = view(query.getView(), s);
		var position = query.getPosition();
		if (position != null && position < 0)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.NEGATIVE_POSITION, "Position must not be negative.");
		return new ResolvedQuery(filter, sort, view, position == null ? 0 : position, limit(query.getLimit(), s), counts(query.getOpts(), s), origin);
	}

	private static void checkLength(BeanQuerySyntaxException.Code code, String what, String value, int max) {
		if (value != null && value.length() > max)
			throw new BeanQuerySyntaxException(code, "%s string exceeds %s characters.", what, max);
	}

	// Rows 2-6: clause count, allowed column, parse + depth, regex gate, operator/type.
	private static Filter filter(String search, QuerySettings s, Instant requestTime) {
		var items = SearchParser.parse(search);
		if (FilterResolver.countLeaves(items) > s.maxSearchClauses)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.TOO_MANY_CLAUSES, "Too many search clauses (max %s).", s.maxSearchClauses);
		var groupDepths = new ArrayList<Integer>();
		collectGroupDepths(items, 0, groupDepths);
		var leafIndex = new int[] {0};
		return FilterResolver.resolve(items, (column, expression) -> {
			var type = s.columns.get(column);
			if (type == null)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_COLUMN, "Unknown column '%s' in search.", column);
			var depthIdx = leafIndex[0]++;
			if (expression.isBlank())
				return null;
			var tree = SearchExpressionParser.parse(expression, s.operators(column));
			var totalDepth = groupDepths.get(depthIdx) + depth(tree);
			if (totalDepth > s.maxExpressionDepth)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.TOO_DEEP, "Search expression nested too deeply (max %s).", s.maxExpressionDepth);
			checkRegex(tree, s.allowRegex);
			checkTypes(tree, type, column);
			if (type == SearchType.NUMERIC || type == SearchType.BOOLEAN || type == SearchType.TIMESTAMP)
				tree = tree.isLiteral() ? typeLiteral(tree, type, column, requestTime) : typeTree(tree, type, column, requestTime);
			return new Filter.Leaf(column, tree, type);
		});
	}

	// Depth = group nesting + expression nesting (design §7 row 4). Walks the raw SearchItem tree, in the same
	// left-to-right pre-order FilterResolver.resolve visits leaves, recording each leaf's group-nesting depth so the
	// leafResolver callback above can look its own leaf's depth up by position once it has parsed the expression.
	private static void collectGroupDepths(List<SearchItem> items, int groupDepth, List<Integer> out) {
		for (var item : items) {
			if (item instanceof SearchItem.Leaf)
				out.add(groupDepth);
			else if (item instanceof SearchItem.And and)
				collectGroupDepths(and.items(), groupDepth + 1, out);
			else if (item instanceof SearchItem.Or or)
				collectGroupDepths(or.items(), groupDepth + 1, out);
			else if (item instanceof SearchItem.Not not)
				collectGroupDepths(List.of(not.item()), groupDepth + 1, out);
		}
	}

	private static int depth(SearchExpression node) {
		if (node.isLiteral())
			return 0;
		var d = 0;
		for (var a : node.args())
			d = Math.max(d, depth(a));
		return d + 1;
	}

	private static void checkRegex(SearchExpression node, boolean allowRegex) {
		if (node.isLiteral())
			return;
		if ("$regex".equals(node.name())) {
			if (! allowRegex)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.REGEX_DISABLED, "Regex search is not enabled for this list.");
			if (node.literalArgs().get(0).length() > QuerySettings.MAX_REGEX_LENGTH)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.REGEX_TOO_LONG, "Regex pattern exceeds %s characters.", QuerySettings.MAX_REGEX_LENGTH);
		}
		for (var a : node.args())
			checkRegex(a, allowRegex);
	}

	private static void checkTypes(SearchExpression node, SearchType type, String column) {
		if (node.isLiteral())
			return;
		var op = node.operator();
		if (! op.appliesTo(type))
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.OPERATOR_TYPE, "Operator %s does not apply to %s column '%s'.", op.name(), type.wire(), column);
		for (var a : node.args())
			checkTypes(a, type, column);
	}

	// Types the leaves design #4 section 5 says to type: bare literals (called directly on a literal tree) and every
	// literal argument of a value operator or a typedArgs(true) custom operator.  Runs after checkTypes so
	// OPERATOR_TYPE wins over BAD_VALUE.  Everything else ($contains/$prefix/$regex/$eqic/$blank arguments, a custom
	// operator that did not opt in) is returned unchanged and stays text.
	private static SearchExpression typeTree(SearchExpression node, SearchType type, String column, Instant requestTime) {
		var op = node.operator();
		if (op.isCombinator()) {
			var newArgs = node.args().stream().map(a -> a.isLiteral() ? typeLiteral(a, type, column, requestTime) : typeTree(a, type, column, requestTime)).toList();
			return SearchExpression.func(op, newArgs);
		}
		if (VALUE_OPS.contains(op.name()) || (op.isCustom() && op.typedArgs())) {
			var newArgs = node.args().stream().map(a -> a.isLiteral() ? typeLiteral(a, type, column, requestTime) : a).toList();
			return SearchExpression.func(op, newArgs);
		}
		return node;
	}

	private static SearchExpression typeLiteral(SearchExpression literal, SearchType type, String column, Instant requestTime) {
		try {
			return literal.withTypedValue(type.parse(literal.value(), requestTime));
		} catch (BeanQuerySyntaxException e) {
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.BAD_VALUE, "Value '%s' is not a valid %s for column '%s'.", literal.value(), type.wire(), column);
		}
	}

	// Row 7.
	private static List<ResolvedQuery.SortKey> sort(String sort, QuerySettings s) {
		var keys = new ArrayList<ResolvedQuery.SortKey>();
		if (ib(sort))
			return keys;
		for (var raw : sort.split(",")) {
			var t = raw.strip();  // An empty key passes through the suffix checks unchanged and is skipped below.
			var desc = false;
			if (t.regionMatches(true, t.length() - 5, ":desc", 0, 5)) {
				desc = true;
				t = t.substring(0, t.length() - 5).strip();
			} else if (t.regionMatches(true, t.length() - 4, ":asc", 0, 4)) {
				t = t.substring(0, t.length() - 4).strip();
			} else if (t.endsWith("-")) {
				desc = true;
				t = t.substring(0, t.length() - 1).strip();
			} else if (t.endsWith("+")) {
				t = t.substring(0, t.length() - 1).strip();
			}
			if (t.isEmpty())
				continue;
			if (keys.size() == s.maxSortKeys)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.TOO_MANY_SORT_KEYS, "Too many sort keys (max %s).", s.maxSortKeys);
			var type = s.columns.get(t);
			if (type == null)
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_COLUMN, "Unknown sort column '%s'.", t);
			keys.add(new ResolvedQuery.SortKey(t, type, desc));
		}
		return keys;
	}

	// Row 8.
	private static List<String> view(String view, QuerySettings s) {
		var cols = new LinkedHashSet<String>();
		if (inb(view)) {
			for (var raw : view.split(",")) {
				var t = raw.strip();
				if (t.isEmpty())
					continue;
				if (! s.columns.containsKey(t))
					throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_COLUMN, "Unknown view column '%s'.", t);
				cols.add(t);
			}
		}
		return cols.isEmpty() ? List.copyOf(s.columns.keySet()) : List.copyOf(cols);
	}

	// Row 9 (limit half): missing -> default; negative -> cap; above cap -> cap; null only when uncapped.
	private static Integer limit(Integer limit, QuerySettings s) {
		if (limit == null)
			return s.defaultLimit;
		if (limit < 0)
			return s.maxLimit;
		return s.maxLimit == null ? limit : Math.min(limit, s.maxLimit);
	}

	// Row 10.
	private static CountPolicy counts(String opts, QuerySettings s) {
		if (s.countPolicy != CountPolicy.IF_REQUESTED)
			return s.countPolicy;
		var v = ClauseParser.parse(opts).get("counts");
		if (v != null)
			v = v.strip();
		if (eqic(v, "matched"))
			return CountPolicy.MATCHED;
		if (eqic(v, "both") || eqic(v, "true"))
			return CountPolicy.BOTH;
		return CountPolicy.NONE;
	}
}
