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
package org.apache.juneau.beanquery.sql;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.beanquery.*;

/**
 * Compiles a resolved {@link Filter} tree into a SQL {@code WHERE} predicate for a {@link SqlDialect}.
 *
 * <p>
 * This is the dialect-agnostic core of the shared JDBC engine.  The terms are already parsed, allow-listed and
 * type-checked.  The compiler walks each tree and:
 * <ul>
 * 	<li>combines the terms (one per column) with {@code AND};
 * 	<li>combines a column's {@code $and}/{@code $or}/{@code $not} sub-expressions itself;
 * 	<li>delegates every leaf to the {@link SqlDialect} (a built-in via {@link SqlDialect#builtinRenderer(String)}, a
 * 		bare pattern via {@link SqlDialect#renderBare}) or, for a custom operator, to that operator's
 * 		{@link SearchOperator#extension(Class) SearchSqlRenderer extension}.
 * 	<li>a {@link #compile(Filter)} caller gets the same per-leaf rendering across a resolved, cross-column tree.
 * </ul>
 *
 * <p>
 * <p>
 * The SQL result must match the in-memory result, so a built-in the dialect cannot faithfully express is rejected with a
 * {@link BeanQuerySyntaxException} whose code is {@link org.apache.juneau.commons.beanquery.BeanQuerySyntaxException.Code#BAD_VALUE BAD_VALUE} rather than
 * rendered differently: an operator for which {@link SqlDialect#builtinRenderer(String)} returns <jk>null</jk> (for
 * example {@code $regex} on a database without regex support), and a range/ordering operator ({@code $gt}, {@code $gte},
 * {@code $lt}, {@code $lte}, {@code $between}) on a {@code VERSION} column when
 * {@link SqlDialect#supportsVersionOrdering()} is <jk>false</jk>.  Equality on versions is unaffected.  A custom
 * operator with no usable renderer still raises {@link IllegalStateException}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ResolvedQuery <jv>q</jv> = <jv>context</jv>.resolve(BeanQuery.<jsm>create</jsm>().gt(<js>"age"</js>, 21).eq(<js>"name"</js>, <js>"Bob"</js>).build());
 * 	SqlFragment <jv>where</jv> = SqlSearchCompiler.<jsm>create</jsm>(<jv>dialect</jv>, <jv>context</jv>).compile(<jv>q</jv>.filter());
 * 	<jc>// where.sql():   "age" &gt; ? AND "name" = ?</jc>
 * 	<jc>// where.binds(): [21, Bob]</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class SqlSearchCompiler {

	/** Range/ordering built-ins; on a VERSION column these need the dialect to order dotted tuples semantically. */
	private static final Set<String> VERSION_ORDERING = Set.of("$gt", "$gte", "$lt", "$lte", "$between");

	private final SqlDialect dialect;
	private final ColumnResolver columns;

	private SqlSearchCompiler(SqlDialect dialect, ColumnResolver columns) {
		reqnn("dialect", dialect, "columns", columns);
		this.dialect = dialect;
		this.columns = columns;
	}

	/**
	 * Creates a compiler for a dialect and column resolver.
	 *
	 * @param dialect The target dialect.  Must not be <jk>null</jk>.
	 * @param columns Maps validated column names to quoted SQL references.
	 * @return A new compiler.
	 */
	public static SqlSearchCompiler create(SqlDialect dialect, ColumnResolver columns) {
		return new SqlSearchCompiler(dialect, columns);
	}

	/**
	 * Compiles an already-resolved {@link Filter} tree into a boolean {@code WHERE} predicate.
	 *
	 * <p>
	 * Renders the full cross-column {@code Filter.And}/{@code Filter.Or}/{@code Filter.Not} tree, so a top-level
	 * {@code $or} across <i>different</i> columns (for example DataTables' global search) compiles correctly.
	 * {@code Filter.And}/{@code Filter.Or} with no items render as always-true/always-false ({@code 1=1}/{@code 1=0})
	 * per their own contract &mdash; see {@code SqlBeanQueryContext.appendWhere}, which special-cases an empty
	 * top-level filter so an unfiltered query's {@code WHERE} clause does not pick up a spurious {@code (1=1)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SqlFragment <jv>where</jv> = SqlSearchCompiler.<jsm>create</jsm>(<jv>dialect</jv>, <jv>columns</jv>)
	 * 		.compile(<jv>filter</jv>);
	 * </p>
	 *
	 * @param filter The resolved filter, or <jk>null</jk> for no predicate.
	 * @return The predicate fragment, or <jk>null</jk> if {@code filter} is <jk>null</jk>.
	 * @throws BeanQuerySyntaxException (BAD_VALUE) If a built-in leaf is not supported by the dialect.
	 * @throws IllegalStateException If a custom-operator leaf cannot be rendered for the dialect.
	 */
	public SqlFragment compile(Filter filter) {
		return filter == null ? null : renderFilter(filter);
	}

	private SqlFragment renderFilter(Filter node) {
		if (node instanceof Filter.Leaf leaf)
			return render(leaf.expression(), columns.columnSql(leaf.column()), leaf.type());
		if (node instanceof Filter.Not not) {
			var child = renderFilter(not.item());
			return not(child);
		}
		if (node instanceof Filter.And and) {
			if (and.items().isEmpty())
				return SqlFragment.of("1=1");
			return join(and.items().stream().map(this::renderFilter).toList(), " AND ");
		}
		if (node instanceof Filter.Or or) {
			if (or.items().isEmpty())
				return SqlFragment.of("1=0");
			return join(or.items().stream().map(this::renderFilter).toList(), " OR ");
		}
		throw new IllegalStateException("Unknown Filter type: " + node);
	}

	private SqlFragment render(SearchExpression node, String columnSql, SearchType type) {
		if (node.isLiteral())
			return required(dialect.renderBare(columnSql, type, node.typedValue(), node.isQuoted()), "bare pattern", node);
		var op = node.operator();
		if (op.isCombinator())
			return renderCombinator(node, columnSql, type);
		if (op.isCustom()) {
			var r = op.extension(SearchSqlRenderer.class).orElse(null);
			if (r == null)
				throw new IllegalStateException("Custom operator '" + op.name() + "' has no SQL renderer; it cannot run against dialect '" + dialect.id() + "'.");
			return required(r.render(dialect, columnSql, type, op.typedArgs() ? node.typedArgs() : List.copyOf(node.literalArgs())), "custom operator '" + op.name() + "'", node);
		}
		if (type == SearchType.VERSION && VERSION_ORDERING.contains(op.name()) && ! dialect.supportsVersionOrdering())
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.BAD_VALUE,
				"Operator '%s' is not supported on version columns by the '%s' SQL dialect (version ordering is semantic in memory, so it is rejected rather than compared as text).", op.name(), dialect.id());
		var r = dialect.builtinRenderer(op.name());
		if (r == null)
			throw new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.BAD_VALUE,
				"Operator '%s' is not supported by the '%s' SQL dialect.", op.name(), dialect.id());
		return required(r.render(dialect, columnSql, type, node.typedArgs()), "operator '" + op.name() + "'", node);
	}

	private SqlFragment renderCombinator(SearchExpression node, String columnSql, SearchType type) {
		if ("$not".equals(node.name())) {
			return not(render(node.args().get(0), columnSql, type));
		}
		var children = node.args().stream().map(a -> render(a, columnSql, type)).toList();
		return join(children, "$or".equals(node.name()) ? " OR " : " AND ");
	}

	/**
	 * Negates a fragment with two-valued (strictly true/false) semantics.
	 *
	 * <p>
	 * The in-memory engine evaluates every leaf to true or false, so a negated leaf matches a row whose cell is
	 * {@code null}.  In SQL a predicate over a {@code NULL} cell is {@code NULL}, and {@code NOT NULL} is still
	 * {@code NULL}, which would drop the row.  Coalescing the inner predicate to {@code FALSE} first restores the
	 * in-memory result.
	 */
	private static SqlFragment not(SqlFragment child) {
		return SqlFragment.of("(NOT COALESCE(" + child.sql() + ", FALSE))", child.binds());
	}

	/** Joins fragments with a boolean operator, concatenating binds; the whole group is always parenthesized. */
	private static SqlFragment join(List<SqlFragment> frags, String sep) {
		var sb = new StringBuilder("(");
		var binds = new ArrayList<>();
		for (var i = 0; i < frags.size(); i++) {
			if (i > 0)
				sb.append(sep);
			sb.append(frags.get(i).sql());
			binds.addAll(frags.get(i).binds());
		}
		sb.append(')');
		return SqlFragment.of(sb.toString(), binds);
	}

	private SqlFragment required(SqlFragment f, String what, SearchExpression node) {
		if (f == null)
			throw new IllegalStateException("Dialect '" + dialect.id() + "' returned no SQL for " + what + " in '" + node + "'.");
		return f;
	}
}
