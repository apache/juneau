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
import java.util.stream.*;

/**
 * Parses and renders the {@link BeanQuery#getSearch() search} string grammar (design §4):
 *
 * <pre>
 *   search = item ("," item)*
 *   item   = group | leaf
 *   group  = "$or(" item ("," item)+ ")" | "$and(" item ("," item)+ ")" | "$not(" item ")"
 *   leaf   = key "=" expression
 *   key    = backslash-unescaped column name (a backslash before any character is accepted on parse;
 *            {@link ClauseParser#escape(String)} is the narrower inverse used on render, only ever escaping
 *            "\", ",", "=", and a leading "$")
 * </pre>
 *
 * <p>
 * A leaf's {@code expression} is left unparsed here — it is the per-column {@code $}-expression grammar, unchanged
 * by this design; {@code SearchExpressionParser} parses it later, once the column and its operator set are known.
 * Group-vs-leaf disambiguation is an exact text match on {@code $or(}/{@code $and(}/{@code $not(} at the start of
 * an item. Structural errors ({@code $or}/{@code $and} needing 2+ items, {@code $not} needing exactly 1, unbalanced
 * group parens) are caught here; per-expression errors (unterminated quote, unknown operator, ...) surface later
 * from {@code SearchExpressionParser} / {@code QueryResolver}. A blank leaf ({@code key=} with an empty expression)
 * is preserved here, not dropped, as a {@link SearchItem.Leaf} with an empty expression &mdash; so {@code
 * QueryResolver} can validate its column first (an unknown column still throws, even with a blank value) and only
 * then drop the blank leaf during resolution (design §7), via {@code FilterResolver}'s null-as-drop-signal
 * convention. A group left with no items after that drop is dropped in turn.
 *
 * <p>
 * {@link #parse(String)} also rejects a column repeated among the returned <em>top-level</em> items (design D2) —
 * the same column repeated inside a group, or once at top level and again inside a top-level group, is allowed.
 * Both {@code QueryResolver} (resolving a request's search) and {@code BeanQuery.copy()} (re-parsing an existing
 * query's search into a builder) go through this one check.
 */
final class SearchParser {

	private final String s;
	private final int len;
	private int pos;

	private SearchParser(String s) {
		this.s = s;
		this.len = s.length();
	}

	/**
	 * Parses a whole search string into its top-level items, in order.
	 *
	 * @param search The raw search string.  <jk>null</jk> or blank yields an empty list (matches every row).
	 * @return The top-level items (AND-ed together), never <jk>null</jk>.
	 * @throws BeanQuerySyntaxException On unbalanced group parens, a group arity violation, or a column repeated
	 * 	among the top-level items.
	 */
	static List<SearchItem> parse(String search) {
		if (search == null || search.isBlank())
			return List.of();
		var p = new SearchParser(search);
		List<SearchItem> items = new ArrayList<>();
		p.skipWs();
		while (! p.atEnd()) {
			SearchItem item = p.parseItem();
			if (item != null)
				items.add(item);
			p.skipWs();
			if (p.atEnd())
				break;
			p.expect(',');
			p.skipWs();
		}
		checkNoDuplicateTopLevelColumns(items);
		return items;
	}

	private static void checkNoDuplicateTopLevelColumns(List<SearchItem> items) {
		Set<String> seen = new HashSet<>();
		for (SearchItem item : items)
			if (item instanceof SearchItem.Leaf leaf && ! seen.add(leaf.column()))
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.DUPLICATE_COLUMN, "Column '%s' appears more than once in search; combine with $and(...).", leaf.column());
	}

	/** Renders a top-level item list back to its wire search string (used by {@code BeanQuery.copy()}/{@code build()}). */
	static String render(List<SearchItem> items) {
		return items.stream().map(SearchParser::renderItem).collect(Collectors.joining(","));
	}

	private static String renderItem(SearchItem item) {
		if (item instanceof SearchItem.Leaf l)
			return ClauseParser.escape(l.column()) + "=" + l.expression();
		if (item instanceof SearchItem.And a)
			return AND_OPEN + a.items().stream().map(SearchParser::renderItem).collect(Collectors.joining(",")) + ")";
		if (item instanceof SearchItem.Or o)
			return OR_OPEN + o.items().stream().map(SearchParser::renderItem).collect(Collectors.joining(",")) + ")";
		if (item instanceof SearchItem.Not n)
			return NOT_OPEN + renderItem(n.item()) + ")";
		throw new IllegalStateException("Unreachable: " + item.getClass());
	}

	// -- parsing ------------------------------------------------------------------------------------------------

	private static final String AND_OPEN = "$and(";
	private static final String OR_OPEN = "$or(";
	private static final String NOT_OPEN = "$not(";

	private enum GroupKind { AND, OR, NOT }

	/** Parses one item slot. Returns <jk>null</jk> only for a syntactically-missing slot (e.g. a bare comma with no
	 * {@code key=} at all); a blank-valued leaf still comes back as a {@link SearchItem.Leaf} with an empty
	 * expression. */
	private SearchItem parseItem() {
		skipWs();
		if (atEnd() || peek() == ',' || peek() == ')')
			return null;
		if (startsWithAt(OR_OPEN))
			return parseGroup(OR_OPEN, GroupKind.OR);
		if (startsWithAt(AND_OPEN))
			return parseGroup(AND_OPEN, GroupKind.AND);
		if (startsWithAt(NOT_OPEN))
			return parseGroup(NOT_OPEN, GroupKind.NOT);
		return parseLeaf();
	}

	private SearchItem parseGroup(String prefix, GroupKind kind) {
		pos += prefix.length();
		skipWs();
		List<SearchItem> children = new ArrayList<>();
		int rawCount = 0;
		if (! atEnd() && peek() == ')') {
			pos++;
			validateArity(kind, 0);
			return null;
		}
		while (true) {
			rawCount++;
			SearchItem child = parseItem();
			if (child != null)
				children.add(child);
			skipWs();
			if (atEnd())
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNBALANCED, "Unbalanced group in search.");
			char c = peek();
			if (c == ')') {
				pos++;
				break;
			}
			if (c != ',')
				throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNBALANCED, "Unbalanced group in search.");
			pos++;
			skipWs();
		}
		validateArity(kind, rawCount);
		if (children.isEmpty())
			return null;
		return switch (kind) {
			case AND -> new SearchItem.And(children);
			case OR -> new SearchItem.Or(children);
			case NOT -> new SearchItem.Not(children.get(0));
		};
	}

	private void validateArity(GroupKind kind, int rawCount) {
		switch (kind) {
			case AND -> { if (rawCount < 2) throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.GROUP_ARITY, "$and takes at least two items."); }
			case OR -> { if (rawCount < 2) throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.GROUP_ARITY, "$or takes at least two items."); }
			case NOT -> { if (rawCount != 1) throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.GROUP_ARITY, "$not takes exactly one item."); }
		}
	}

	private SearchItem parseLeaf() {
		String column = parseKey();
		String expression = scanBalancedUntilDelimiter();
		return new SearchItem.Leaf(column, expression);
	}

	/** Reads a backslash-escaped key up to (and consuming) the first unescaped {@code '='}. */
	private String parseKey() {
		var sb = new StringBuilder();
		while (! atEnd()) {
			char c = s.charAt(pos);
			if (c == '\\' && pos + 1 < len) {
				sb.append(s.charAt(pos + 1));
				pos += 2;
				continue;
			}
			if (c == '=') {
				pos++;
				return sb.toString();
			}
			sb.append(c);
			pos++;
		}
		throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNBALANCED, "Missing '=' after column '%s' in search.", sb);
	}

	/**
	 * Reads raw expression text up to (not consuming) the next depth-0, non-quoted {@code ','} or {@code ')'}, or
	 * end of string — tracking paren depth and quote state (with design D5 doubled-quote-is-literal handling) only
	 * well enough to find this leaf's boundary.  It does not itself validate that the expression's own quotes and
	 * parens balance; an unterminated quote inside one leaf's expression is caught later by
	 * {@code SearchExpressionParser} when the expression is actually parsed.
	 */
	@SuppressWarnings({
		"java:S3776" // Straight-line character scanner; splitting it would obscure the single quote/paren state machine.
	})
	private String scanBalancedUntilDelimiter() {
		int start = pos;
		int depth = 0;
		Character quote = null;
		while (! atEnd()) {
			char c = s.charAt(pos);
			if (quote == null && depth == 0 && (c == ')' || c == ','))
				break;
			if (quote != null) {
				if (c != quote)
					pos++;
				else if (pos + 1 < len && s.charAt(pos + 1) == quote)
					pos += 2;
				else {
					quote = null;
					pos++;
				}
			} else if (c == '"' || c == '\'') {
				quote = c;
				pos++;
			} else {
				if (c == '(')
					depth++;
				else if (c == ')')
					depth--;
				pos++;
			}
		}
		return s.substring(start, pos).strip();
	}

	// -- cursor helpers -------------------------------------------------------------------------------------------

	private boolean atEnd() {
		return pos >= len;
	}

	private char peek() {
		return s.charAt(pos);
	}

	private void skipWs() {
		while (! atEnd() && Character.isWhitespace(peek()))
			pos++;
	}

	private void expect(char c) {
		if (atEnd())
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNBALANCED, "Unexpected end of search string; expected '%s'.", c);
		if (peek() != c)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNBALANCED, "Expected '%s' in search but found '%s'.", c, peek());
		pos++;
	}

	private boolean startsWithAt(String literal) {
		return s.regionMatches(pos, literal, 0, literal.length());
	}
}
