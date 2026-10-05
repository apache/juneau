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

import org.apache.juneau.commons.utils.StringUtils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * A protocol-agnostic query request against a collection of beans: what to search, which columns to return, how to
 * sort, and which page to return (design §5.1, §5.3).
 *
 * <p>
 * Every field is a plain <b>raw value</b> &mdash; there are no HTTP-binding annotations, no {@link java.util.Optional},
 * no search map, no page string, and no parsed operators on this bean.  A <jk>null</jk> field means <b>absent</b>.  The
 * grammar for {@link #getSearch() search} and {@link #getOpts() opts} lives <b>inside</b> those single strings and is
 * decoded by a {@link BeanQuerySession session} when the query runs, not by this bean.
 *
 * <h5 class='section'>Fields</h5>
 * <ul>
 * 	<li><b>search</b> ({@link String}) &mdash; the one search string.  Top-level commas (outside parentheses) separate
 * 		clauses; the first unescaped {@code =} separates a column from its raw {@code $}-expression
 * 		({@link SearchExpressionParser}); backslash escapes {@code \,} / {@code \=} / {@code \\} are literals.  Columns
 * 		combine with {@code and}.  See {@link ClauseParser}.
 * 	<li><b>view</b> ({@link String}) &mdash; a comma-separated list of column names, in output order.
 * 	<li><b>sort</b> ({@link String}) &mdash; a comma-separated list of {@code column} (ascending) or {@code column:desc}
 * 		tokens (a trailing {@code -}/{@code +} is also accepted).
 * 	<li><b>position</b> ({@link Integer}) &mdash; the 0-based first row of the page, or <jk>null</jk> for the start.
 * 	<li><b>limit</b> ({@link Integer}) &mdash; the page size, or <jk>null</jk> for all rows.
 * 	<li><b>opts</b> ({@link String}) &mdash; an open bag of {@code key=value} clauses (same grammar as {@code search}).
 * 		The session reads {@code counts} and ignores unknown keys.
 * </ul>
 *
 * <p>
 * Construct with {@link #create()}, which returns a {@link Builder} that assembles these raw strings fluently:
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	BeanQuery <jv>q</jv> = BeanQuery.<jsm>create</jsm>()
 * 		.eq(<js>"status"</js>, <js>"active"</js>)
 * 		.or(<jv>g</jv> -&gt; <jv>g</jv>.contains(<js>"name"</js>, <js>"bob"</js>).contains(<js>"email"</js>, <js>"bob"</js>))
 * 		.sort(<js>"name"</js>)
 * 		.limit(50)
 * 		.build();
 * </p>
 *
 * <p>
 * The HTTP-binding {@code BeanQueryRequest} ({@code juneau-rest-common}) extends this class and adds the
 * {@code @Query} bindings.
 *
 * @since 10.0.0
 */
public class BeanQuery {

	private String search;
	private String view;
	private String sort;
	private Integer position;
	private Integer limit;
	private String opts;

	/**
	 * Creates a new {@link Builder}.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * The one search string.
	 *
	 * @return The search string, or <jk>null</jk> if absent.
	 */
	public String getSearch() {
		return search;
	}

	/**
	 * Sets the one search string.
	 *
	 * @param value The search string.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BeanQuery setSearch(String value) {
		search = value;
		return this;
	}

	/**
	 * The raw view (column-selection) directive.
	 *
	 * @return The view string, or <jk>null</jk> if absent.
	 */
	public String getView() {
		return view;
	}

	/**
	 * Sets the raw view (column-selection) directive.
	 *
	 * @param value The view string.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BeanQuery setView(String value) {
		view = value;
		return this;
	}

	/**
	 * The raw sort directive.
	 *
	 * @return The sort string, or <jk>null</jk> if absent.
	 */
	public String getSort() {
		return sort;
	}

	/**
	 * Sets the raw sort directive.
	 *
	 * @param value The sort string.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BeanQuery setSort(String value) {
		sort = value;
		return this;
	}

	/**
	 * The 0-based first row of the page.
	 *
	 * @return The position, or <jk>null</jk> for the start of the results.
	 */
	public Integer getPosition() {
		return position;
	}

	/**
	 * Sets the 0-based first row of the page.
	 *
	 * @param value The position.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BeanQuery setPosition(Integer value) {
		position = value;
		return this;
	}

	/**
	 * The page size.
	 *
	 * @return The limit, or <jk>null</jk> for all rows.
	 */
	public Integer getLimit() {
		return limit;
	}

	/**
	 * Sets the page size.
	 *
	 * @param value The limit.  Can be <jk>null</jk> to unset (all rows).
	 * @return This object.
	 */
	public BeanQuery setLimit(Integer value) {
		limit = value;
		return this;
	}

	/**
	 * The one opts string (an open bag of {@code key=value} clauses).
	 *
	 * @return The opts string, or <jk>null</jk> if absent.
	 */
	public String getOpts() {
		return opts;
	}

	/**
	 * Sets the one opts string.
	 *
	 * @param value The opts string.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BeanQuery setOpts(String value) {
		opts = value;
		return this;
	}

	/**
	 * Creates a {@link Builder} pre-populated with this query's current state, so individual settings can be
	 * changed or added without disturbing the rest. {@link #getSearch()} is re-parsed into the builder's item
	 * tree, so a new condition on an existing top-level column merges through <c>$and(...)</c> rather than being
	 * dropped or duplicated.
	 *
	 * @return A new builder seeded from this query.
	 */
	public Builder copy() {
		var b = new Builder();
		if (search != null)
			b.group.items.seed(SearchParser.parse(search));
		if (view != null)
			b.view.addAll(splitNonBlank(view));
		if (sort != null)
			b.sort.addAll(splitNonBlank(sort));
		b.position = position;
		b.limit = limit;
		if (opts != null)
			b.opts.putAll(ClauseParser.parse(opts));
		return b;
	}

	/** Splits a comma-delimited list, trimming each token and dropping blank ones. */
	private static List<String> splitNonBlank(String s) {
		var l = new ArrayList<>(StringUtils.split(s));
		l.removeIf(String::isEmpty);
		return l;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof BeanQuery other))
			return false;
		return eq(search, other.search) && eq(view, other.view)
			&& eq(sort, other.sort) && eq(position, other.position)
			&& eq(limit, other.limit) && eq(opts, other.opts);
	}

	@Override
	public int hashCode() {
		return h(search, view, sort, position, limit, opts);
	}

	@Override
	public String toString() {
		return "BeanQuery[search=" + search + ", view=" + view + ", sort=" + sort + ", position=" + position
			+ ", limit=" + limit + ", opts=" + opts + "]";
	}

	/**
	 * A group of cross-column conditions built with {@link BeanQuery.Builder#or(Consumer)},
	 * {@link BeanQuery.Builder#and(Consumer)}, or {@link BeanQuery.Builder#not(Consumer)}.
	 *
	 * <p>Exposes the same condition methods as {@link Builder}, minus paging/view/sort/opts, since a group only
	 * ever contributes conditions to its enclosing level.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BeanQuery.<jsm>create</jsm>().or(<jv>g</jv> -&gt; <jv>g</jv>.eq(<js>"a"</js>, 1).eq(<js>"b"</js>, 2));
	 * 	<jc>// -&gt; "$or(a=$eq(\"1\"),b=$eq(\"2\"))"</jc>
	 * </p>
	 *
	 * @since 10.0.0
	 */
	public static final class GroupBuilder {

		final SearchItemBuilder items = new SearchItemBuilder();

		GroupBuilder() {}

		/**
		 * Adds an equals condition.
		 *
		 * @param column The column name.
		 * @param value The value to compare against.
		 * @return This object.
		 */
		public GroupBuilder eq(String column, Object value) {
			return emit(column, "$eq", value);
		}

		/**
		 * Adds a not-equals condition, true if the column matches none of {@code values}.
		 *
		 * @param column The column name.
		 * @param values The values to compare against.
		 * @return This object.
		 */
		public GroupBuilder ne(String column, Object...values) {
			return emit(column, "$ne", values);
		}

		/**
		 * Adds an in-list condition, true if the column matches any of {@code values}.
		 *
		 * @param column The column name.
		 * @param values The values to compare against.
		 * @return This object.
		 */
		public GroupBuilder in(String column, Object...values) {
			return emit(column, "$in", values);
		}

		/**
		 * Adds a substring-match condition.
		 *
		 * @param column The column name.
		 * @param value The substring to search for.
		 * @return This object.
		 */
		public GroupBuilder contains(String column, Object value) {
			return emit(column, "$contains", value);
		}

		/**
		 * Adds a prefix-match condition.
		 *
		 * @param column The column name.
		 * @param value The prefix to match.
		 * @return This object.
		 */
		public GroupBuilder prefix(String column, Object value) {
			return emit(column, "$prefix", value);
		}

		/**
		 * Adds a blank-value condition.
		 *
		 * @param column The column name.
		 * @return This object.
		 */
		public GroupBuilder blank(String column) {
			reqnb("column", column);
			items.addLeaf(column, "$blank()");
			return this;
		}

		/**
		 * Adds a not-blank condition.
		 *
		 * @param column The column name.
		 * @return This object.
		 */
		public GroupBuilder notBlank(String column) {
			reqnb("column", column);
			items.addLeaf(column, "$not($blank())");
			return this;
		}

		/**
		 * Adds a greater-than condition.
		 *
		 * @param column The column name.
		 * @param value The value to compare against.
		 * @return This object.
		 */
		public GroupBuilder gt(String column, Object value) {
			return emit(column, "$gt", value);
		}

		/**
		 * Adds a greater-than-or-equal condition.
		 *
		 * @param column The column name.
		 * @param value The value to compare against.
		 * @return This object.
		 */
		public GroupBuilder gte(String column, Object value) {
			return emit(column, "$gte", value);
		}

		/**
		 * Adds a less-than condition.
		 *
		 * @param column The column name.
		 * @param value The value to compare against.
		 * @return This object.
		 */
		public GroupBuilder lt(String column, Object value) {
			return emit(column, "$lt", value);
		}

		/**
		 * Adds a less-than-or-equal condition.
		 *
		 * @param column The column name.
		 * @param value The value to compare against.
		 * @return This object.
		 */
		public GroupBuilder lte(String column, Object value) {
			return emit(column, "$lte", value);
		}

		/**
		 * Adds a between condition, inclusive of both bounds.
		 *
		 * @param column The column name.
		 * @param lower The lower bound.
		 * @param upper The upper bound.
		 * @return This object.
		 */
		public GroupBuilder between(String column, Object lower, Object upper) {
			return emit(column, "$between", lower, upper);
		}

		/**
		 * Adds a regex-match condition. The session still rejects this unless regex search is enabled
		 * (context/session design D7).
		 *
		 * @param column The column name.
		 * @param pattern The regular expression.
		 * @return This object.
		 */
		public GroupBuilder regex(String column, String pattern) {
			return emit(column, "$regex", pattern);
		}

		/**
		 * Adds a regex-match condition with flags. The session still rejects this unless regex search is enabled
		 * (context/session design D7).
		 *
		 * @param column The column name.
		 * @param pattern The regular expression.
		 * @param flags The regex flags (engine-specific, e.g. {@code "i"} for case-insensitive).
		 * @return This object.
		 */
		public GroupBuilder regex(String column, String pattern, String flags) {
			return emit(column, "$regex", pattern, flags);
		}

		/**
		 * Escape hatch for an operator not exposed by a dedicated method above. {@code name} must start with
		 * {@code "$"}.
		 *
		 * @param column The column name.
		 * @param name The operator name, starting with {@code "$"}.
		 * @param values The operator's arguments.
		 * @return This object.
		 */
		public GroupBuilder op(String column, String name, Object...values) {
			req(name != null && name.startsWith("$"),
				"BeanQuery.GroupBuilder.op requires an operator name starting with '$': '%s'.", name);
			return emit(column, name, values);
		}

		/**
		 * Escape hatch for a raw, already-formatted expression. Unlike every other condition method, the
		 * expression is used as-is and is not quoted.
		 *
		 * @param column The column name.
		 * @param expression The raw expression, e.g. {@code "$eq(\"bob\")"}.
		 * @return This object.
		 */
		public GroupBuilder search(String column, String expression) {
			reqnb("column", column);
			reqnn("expression", expression);
			items.addLeaf(column, expression);
			return this;
		}

		/**
		 * Adds a cross-column <c>$or(...)</c> group. A block that adds no items adds nothing; a block that adds
		 * exactly one item emits that item unwrapped (no <c>$or(...)</c> wrapper).
		 *
		 * @param block Populates the group's conditions on a fresh {@link GroupBuilder}.
		 * @return This object.
		 */
		public GroupBuilder or(Consumer<GroupBuilder> block) {
			items.addGroup(group(block, SearchItem.Or::new));
			return this;
		}

		/**
		 * Adds a cross-column <c>$and(...)</c> group. A block that adds no items adds nothing; a block that adds
		 * exactly one item emits that item unwrapped (no <c>$and(...)</c> wrapper).
		 *
		 * @param block Populates the group's conditions on a fresh {@link GroupBuilder}.
		 * @return This object.
		 */
		public GroupBuilder and(Consumer<GroupBuilder> block) {
			items.addGroup(group(block, SearchItem.And::new));
			return this;
		}

		/**
		 * Adds a <c>$not(...)</c> group. The block must add exactly one item (a leaf or a nested group); any
		 * other count throws {@link IllegalArgumentException}.
		 *
		 * @param block Populates the single negated condition on a fresh {@link GroupBuilder}.
		 * @return This object.
		 * @throws IllegalArgumentException If the block adds a number of items other than exactly one.
		 */
		public GroupBuilder not(Consumer<GroupBuilder> block) {
			reqnn("block", block);
			var inner = new GroupBuilder();
			block.accept(inner);
			var innerItems = inner.items.items();
			req(innerItems.size() == 1,
				"BeanQuery.GroupBuilder.not(...) requires exactly one condition, found %s.", innerItems.size());
			items.addGroup(new SearchItem.Not(innerItems.get(0)));
			return this;
		}

		private static SearchItem group(Consumer<GroupBuilder> block, Function<List<SearchItem>,SearchItem> wrap) {
			reqnn("block", block);
			var inner = new GroupBuilder();
			block.accept(inner);
			var innerItems = inner.items.items();
			if (innerItems.isEmpty())
				return null;
			if (innerItems.size() == 1)
				return innerItems.get(0);
			return wrap.apply(innerItems);
		}

		private GroupBuilder emit(String column, String name, Object...values) {
			reqnb("column", column);
			req(values != null && values.length > 0,
				"Operator '%s' on column '%s' requires at least one value.", name, column);
			var body = Arrays.stream(values).map(GroupBuilder::quote).collect(Collectors.joining(","));
			items.addLeaf(column, name + "(" + body + ")");
			return this;
		}

		private static String quote(Object value) {
			var s = ValueFormat.format(value);
			// Escape a literal '$' by doubling it, in addition to (not instead of) the quote-escaping below:
			// decodeLiteral unconditionally collapses $$ -> $ as its last decode step, so an un-escaped
			// literal "$$" here would wrongly round-trip to "$". (The two replace() calls operate on
			// disjoint alphabets, so the order between them doesn't matter - only that both happen.)
			return "\"" + s.replace("$", "$$").replace("\"", "\"\"") + "\"";
		}
	}

	/**
	 * Builds an immutable {@link BeanQuery}. Create with {@link BeanQuery#create()}; derive one from an existing
	 * query with {@link BeanQuery#copy()}.
	 *
	 * <p>Exposes every {@link GroupBuilder} condition method (they add top-level conditions here) plus
	 * view/sort/paging/opts/counts and {@link #build()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BeanQuery <jv>q</jv> = BeanQuery.<jsm>create</jsm>().eq(<js>"status"</js>, <js>"active"</js>).build();
	 * </p>
	 *
	 * @since 10.0.0
	 */
	public static final class Builder {

		private final GroupBuilder group = new GroupBuilder();
		private final List<String> view = new ArrayList<>();
		private final List<String> sort = new ArrayList<>();
		private final Map<String,String> opts = new LinkedHashMap<>();
		private Integer position;
		private Integer limit;

		Builder() {}

		/** Same as {@link GroupBuilder#eq(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder eq(String column, Object value) { group.eq(column, value); return this; }

		/** Same as {@link GroupBuilder#ne(String,Object...)}, adding a top-level condition. @param column The column name. @param values The values. @return This object. */
		public Builder ne(String column, Object...values) { group.ne(column, values); return this; }

		/** Same as {@link GroupBuilder#in(String,Object...)}, adding a top-level condition. @param column The column name. @param values The values. @return This object. */
		public Builder in(String column, Object...values) { group.in(column, values); return this; }

		/** Same as {@link GroupBuilder#contains(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder contains(String column, Object value) { group.contains(column, value); return this; }

		/** Same as {@link GroupBuilder#prefix(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder prefix(String column, Object value) { group.prefix(column, value); return this; }

		/** Same as {@link GroupBuilder#blank(String)}, adding a top-level condition. @param column The column name. @return This object. */
		public Builder blank(String column) { group.blank(column); return this; }

		/** Same as {@link GroupBuilder#notBlank(String)}, adding a top-level condition. @param column The column name. @return This object. */
		public Builder notBlank(String column) { group.notBlank(column); return this; }

		/** Same as {@link GroupBuilder#gt(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder gt(String column, Object value) { group.gt(column, value); return this; }

		/** Same as {@link GroupBuilder#gte(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder gte(String column, Object value) { group.gte(column, value); return this; }

		/** Same as {@link GroupBuilder#lt(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder lt(String column, Object value) { group.lt(column, value); return this; }

		/** Same as {@link GroupBuilder#lte(String,Object)}, adding a top-level condition. @param column The column name. @param value The value. @return This object. */
		public Builder lte(String column, Object value) { group.lte(column, value); return this; }

		/** Same as {@link GroupBuilder#between(String,Object,Object)}, adding a top-level condition. @param column The column name. @param lower The lower bound. @param upper The upper bound. @return This object. */
		public Builder between(String column, Object lower, Object upper) { group.between(column, lower, upper); return this; }

		/** Same as {@link GroupBuilder#regex(String,String)}, adding a top-level condition. @param column The column name. @param pattern The regular expression. @return This object. */
		public Builder regex(String column, String pattern) { group.regex(column, pattern); return this; }

		/** Same as {@link GroupBuilder#regex(String,String,String)}, adding a top-level condition. @param column The column name. @param pattern The regular expression. @param flags The regex flags. @return This object. */
		public Builder regex(String column, String pattern, String flags) { group.regex(column, pattern, flags); return this; }

		/** Same as {@link GroupBuilder#op(String,String,Object...)}, adding a top-level condition. @param column The column name. @param name The operator name, starting with {@code "$"}. @param values The operator's arguments. @return This object. */
		public Builder op(String column, String name, Object...values) { group.op(column, name, values); return this; }

		/** Same as {@link GroupBuilder#search(String,String)}, adding a top-level condition. @param column The column name. @param expression The raw expression. @return This object. */
		public Builder search(String column, String expression) { group.search(column, expression); return this; }

		/** Same as {@link GroupBuilder#or(Consumer)}, adding a top-level group. @param block Populates the group. @return This object. */
		public Builder or(Consumer<GroupBuilder> block) { group.or(block); return this; }

		/** Same as {@link GroupBuilder#and(Consumer)}, adding a top-level group. @param block Populates the group. @return This object. */
		public Builder and(Consumer<GroupBuilder> block) { group.and(block); return this; }

		/** Same as {@link GroupBuilder#not(Consumer)}, adding a top-level group. @param block Populates the negated condition. @return This object. @throws IllegalArgumentException If the block adds a number of items other than exactly one. */
		public Builder not(Consumer<GroupBuilder> block) { group.not(block); return this; }

		/**
		 * Appends columns to the view list; does not replace columns already added.
		 *
		 * @param columns The columns to append.  Must not be <jk>null</jk>, nor contain a <jk>null</jk> element.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code columns} is <jk>null</jk> or contains a <jk>null</jk> element.
		 */
		public Builder view(String...columns) {
			reqnns("columns", columns);
			Collections.addAll(view, columns);
			return this;
		}

		/**
		 * Appends an ascending sort key; does not replace keys already added.
		 *
		 * @param column The column to sort by.
		 * @return This object.
		 */
		public Builder sort(String column) {
			reqnb("column", column);
			sort.add(column);
			return this;
		}

		/**
		 * Appends a descending sort key ({@code column + ":desc"}); does not replace keys already added.
		 *
		 * @param column The column to sort by, descending.
		 * @return This object.
		 */
		public Builder sortDesc(String column) {
			reqnb("column", column);
			sort.add(column + ":desc");
			return this;
		}

		/**
		 * Sets the zero-based row offset.
		 *
		 * @param position The row offset; must not be negative.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code position} is negative.
		 */
		public Builder position(int position) {
			req(position >= 0, "BeanQuery.Builder.position must not be negative: %s.", position);
			this.position = position;
			return this;
		}

		/**
		 * Sets the maximum number of rows to return. A negative value is clamped to the literal {@code -1}
		 * (meaning "as many rows as the session allows"), never dropped to {@code null}.
		 *
		 * @param limit The row limit.
		 * @return This object.
		 */
		public Builder limit(int limit) {
			this.limit = limit < 0 ? -1 : limit;
			return this;
		}

		/**
		 * Sets {@link #position(int)} and {@link #limit(int)} together.
		 *
		 * @param start The zero-based row offset; must not be negative.
		 * @param length The maximum number of rows to return; a negative value means "as many as allowed".
		 * @return This object.
		 * @throws IllegalArgumentException If {@code start} is negative.
		 */
		public Builder page(int start, int length) {
			position(start);
			return limit(length);
		}

		/**
		 * Sets a free-form option, overwriting any earlier value for the same key.
		 *
		 * @param key The option name.  Must not be <jk>null</jk> or blank.
		 * @param value The option value.  Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code key} is <jk>null</jk> or blank, or {@code value} is <jk>null</jk>.
		 */
		public Builder opt(String key, String value) {
			reqnb("key", key);
			reqnn("value", value);
			opts.put(key, value);
			return this;
		}

		/**
		 * Sets the {@code counts} option from a {@link CountRequest}. {@link CountRequest#NONE} removes the
		 * {@code counts} option entirely (rather than writing a {@code "none"} value).
		 *
		 * @param request The requested counts.
		 * @return This object.
		 */
		public Builder counts(CountRequest request) {
			reqnn("request", request);
			if (request == CountRequest.NONE) {
				opts.remove("counts");
				return this;
			}
			return opt("counts", request.name().toLowerCase(Locale.ROOT));
		}

		/**
		 * Builds a new, immutable {@link BeanQuery} from this builder's current state. The builder remains usable
		 * afterward; later calls do not affect the {@link BeanQuery} already built.
		 *
		 * @return A new {@link BeanQuery}.
		 */
		public BeanQuery build() {
			var q = new BeanQuery();
			var items = group.items.items();
			if (!items.isEmpty())
				q.setSearch(SearchParser.render(items));
			if (!view.isEmpty())
				q.setView(String.join(",", view));
			if (!sort.isEmpty())
				q.setSort(String.join(",", sort));
			q.setPosition(position);
			q.setLimit(limit);
			if (!opts.isEmpty())
				q.setOpts(opts.entrySet().stream()
					.map(e -> ClauseParser.escape(e.getKey()) + "=" + ClauseParser.escape(e.getValue()))
					.collect(Collectors.joining(",")));
			return q;
		}
	}
}
