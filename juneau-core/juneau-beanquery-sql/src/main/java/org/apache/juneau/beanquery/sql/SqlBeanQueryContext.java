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
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import javax.sql.*;

import org.apache.juneau.commons.beanquery.*;

/**
 * The shared JDBC {@link BeanQueryContext}: renders validated queries into SQL for a {@link SqlDialect} and runs
 * them on a connection from a configured source.
 *
 * <p>
 * The engine is dialect-agnostic.  Identifier quoting, the built-in operator predicates and pagination come from the
 * {@link SqlDialect} (Juneau ships {@code juneau-beanquery-postgres}).  Columns are always explicit: the class passed
 * to {@link #create(Class)} fixes {@code T} (and is returned by {@link #getType()}); it is introspected only to
 * choose the default row mapper, never to derive columns.
 *
 * <p>
 * If no {@link Builder#rowMapper(SqlRowMapper) rowMapper} is set, {@link Builder#build()} picks a default from the
 * row type {@code T}: a {@code Map} type that a {@link LinkedHashMap} is assignable to gets
 * {@link SqlRowMapper#map()}, and any other type that {@link SqlRowMapper#bean(Class)} can plan (a record, or a class
 * with a public no-arg constructor and setters) gets {@link SqlRowMapper#bean(Class)}.  {@code Object}, other
 * {@code Map} types (such as {@code TreeMap}) and unplannable types get no default; {@code find} and {@code stream}
 * then throw {@link IllegalStateException}, with the reason appended when there is one.  An explicit
 * {@code rowMapper(...)} always wins, and {@code findValues}, {@code streamValues} and {@code count} never need one.
 *
 * <p>
 * {@link #renderRows(ResolvedQuery)} and {@link #renderCount(ResolvedQuery, boolean)} open no connection, so the
 * generated SQL and binds can be inspected and unit-tested.  A {@link #guard} fragment is AND-ed, in parentheses,
 * into every statement.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	SqlBeanQueryContext&lt;Person&gt; <jv>context</jv> = SqlBeanQueryContext
 * 		.<jsm>create</jsm>(Person.<jk>class</jk>)
 * 		.dialect(PostgresDialect.<jsf>INSTANCE</jsf>)
 * 		.table(<js>"person"</js>)
 * 		.column(<js>"name"</js>, SearchType.<jsf>TEXT</jsf>)
 * 		.column(<js>"age"</js>, SearchType.<jsf>NUMERIC</jsf>)
 * 		.columnSql(<js>"name"</js>, <js>"full_name"</js>)
 * 		.guard(SqlFragment.<jsm>of</jsm>(<js>"\"deleted\" = false"</js>))
 * 		.rowMapper((<jv>rs</jv>, <jv>cols</jv>) -&gt; <jk>new</jk> Person(<jv>rs</jv>.getString(<js>"full_name"</js>), <jv>rs</jv>.getInt(<js>"age"</js>)))
 * 		.dataSource(<jv>dataSource</jv>)
 * 		.queryTimeout(Duration.<jsm>ofSeconds</jsm>(5))
 * 		.build();
 *
 * 	<jk>try</jk> (SqlBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.getSession()) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(<jv>query</jv>);
 * 	}
 *
 * 	<jc>// No rowMapper(...): Person is a record, so build() defaults to SqlRowMapper.bean(Person.class).</jc>
 * 	SqlBeanQueryContext&lt;Person&gt; <jv>defaulted</jv> = SqlBeanQueryContext.<jsm>create</jsm>(Person.<jk>class</jk>)
 * 		.dialect(PostgresDialect.<jsf>INSTANCE</jsf>)
 * 		.table(<js>"person"</js>)
 * 		.column(<js>"name"</js>, SearchType.<jsf>TEXT</jsf>)
 * 		.column(<js>"age"</js>, SearchType.<jsf>NUMERIC</jsf>)
 * 		.dataSource(<jv>dataSource</jv>)
 * 		.build();
 *
 * 	<jc>// A Map row type defaults to SqlRowMapper.map().</jc>
 * 	SqlBeanQueryContext&lt;Map&gt; <jv>maps</jv> = SqlBeanQueryContext.<jsm>create</jsm>(Map.<jk>class</jk>)
 * 		<jc>// ...dialect, table, columns, connection source...</jc>
 * 		.build();
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
public final class SqlBeanQueryContext<T> extends BeanQueryContext<T> implements ColumnResolver {

	// Applied only when a statement's search includes $regex and no explicit queryTimeout (below) was configured.
	// Unbounded SQL regex is a DoS risk -- the in-memory engine's RegexBudget cannot reach a
	// query that runs inside the database, so this is the SQL engine's own, separate bound.
	static final Duration DEFAULT_REGEX_TIMEOUT = Duration.ofSeconds(5);

	/**
	 * Builder for {@link SqlBeanQueryContext}.  See the class Javadoc for an example.
	 *
	 * @param <T> The row type.
	 */
	public static final class Builder<T> extends BeanQueryContext.Builder<T,Builder<T>> {

		private final Class<T> type;
		private SqlDialect dialect;
		private String table;
		private SqlRowMapper<T> rowMapper;
		private final Map<String,String> columnSql = new LinkedHashMap<>();
		private SqlFragment guard;
		private Supplier<Connection> connectionSupplier;
		private Consumer<Connection> connectionReleaser = SqlBeanQueryContext::closeConnection;
		private int fetchSize = 500;
		private Duration queryTimeout;

		Builder(Class<T> type) {
			this.type = type;
		}

		Builder(SqlBeanQueryContext<T> copyFrom) {
			super(copyFrom);
			type = copyFrom.type;
			dialect = copyFrom.dialect;
			table = copyFrom.table;
			rowMapper = copyFrom.rowMapper;
			columnSql.putAll(copyFrom.columnSql);
			guard = copyFrom.guard;
			connectionSupplier = copyFrom.connectionSupplier;
			connectionReleaser = copyFrom.connectionReleaser;
			fetchSize = copyFrom.fetchSize;
			queryTimeout = copyFrom.queryTimeout;
		}

		/**
		 * Sets the SQL dialect.  Required.
		 *
		 * @param value The dialect.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T> dialect(SqlDialect value) {
			dialect = reqnn("value", value);
			return this;
		}

		/**
		 * Sets the table (or view) to query.  Required.
		 *
		 * @param value The raw table name; the dialect quotes it.  Must not be blank.
		 * @return This object.
		 */
		public Builder<T> table(String value) {
			table = reqnb("value", value);
			return this;
		}

		/**
		 * Sets how {@link SqlBeanQuerySession#find(BeanQuery) find} and {@link SqlBeanQuerySession#stream(BeanQuery)
		 * stream} turn a row into a bean, overriding the default {@link #build()} would pick from the row type.  Not
		 * needed for {@code findValues}, {@code streamValues} or {@code count}.
		 *
		 * @param value The row mapper.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T> rowMapper(SqlRowMapper<T> value) {
			rowMapper = reqnn("value", value);
			return this;
		}

		/**
		 * Maps a declared column to a different raw SQL column name (otherwise the column name is used).
		 *
		 * @param column The column name.  Must be declared.
		 * @param sqlName The raw SQL column name; the dialect quotes it.  Must not be blank.
		 * @return This object.
		 */
		public Builder<T> columnSql(String column, String sqlName) {
			columnSql.put(declared(column), reqnb("sqlName", sqlName));
			return this;
		}

		/**
		 * Sets a server-only SQL predicate AND-ed into every statement.  A request can neither see nor remove it.
		 *
		 * @param value The guard, or <jk>null</jk> for none.
		 * @return This object.
		 */
		public Builder<T> guard(SqlFragment value) {
			guard = value;
			return this;
		}

		/**
		 * Sets where sessions get a connection when none is passed to
		 * {@link SqlBeanQuerySession.Builder#connection(Connection)}.  This or {@link #dataSource(DataSource)} is
		 * required.
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.  Exceptions it throws become
		 * 	{@link BeanQueryExecutionException}s.
		 * @return This object.
		 */
		public Builder<T> connectionSupplier(Supplier<Connection> value) {
			connectionSupplier = reqnn("value", value);
			return this;
		}

		/**
		 * Sets how a session gives back a connection it got from the supplier.  Default: {@link Connection#close()}.
		 *
		 * @param value The releaser.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T> connectionReleaser(Consumer<Connection> value) {
			connectionReleaser = reqnn("value", value);
			return this;
		}

		/**
		 * Shorthand for {@link #connectionSupplier(Supplier) connectionSupplier(ds::getConnection)}, with a
		 * {@link SQLException} wrapped as a {@link BeanQueryExecutionException}.
		 *
		 * @param value The data source.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T> dataSource(DataSource value) {
			reqnn("value", value);
			return connectionSupplier(() -> {
				try {
					return value.getConnection();
				} catch (SQLException e) {
					throw new BeanQueryExecutionException(e, "Failed to open a database connection.");
				}
			});
		}

		/**
		 * Sets the JDBC fetch size for every statement.  Default: 500.
		 *
		 * @param value The fetch size.  Must be positive.
		 * @return This object.
		 */
		public Builder<T> fetchSize(int value) {
			req(value > 0, "fetchSize must be positive: %s", value);
			fetchSize = value;
			return this;
		}

		/**
		 * Sets the JDBC query timeout for every statement, rounded up to whole seconds.  Default: none.
		 *
		 * <p>
		 * Leave unset and this still bounds any statement whose search includes {@code $regex}, at a shorter built-in
		 * default &mdash; set this explicitly to control that bound instead of accepting the default.
		 *
		 * @param value The timeout, or <jk>null</jk> for none.  Must be positive if set.
		 * @return This object.
		 */
		public Builder<T> queryTimeout(Duration value) {
			req(value == null || ! (value.isNegative() || value.isZero()), "queryTimeout must be positive: %s", value);
			queryTimeout = value;
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @throws IllegalStateException If the dialect, the table, the connection source or every column is missing.
		 */
		@Override /* BeanQueryContext.Builder */
		public SqlBeanQueryContext<T> build() {
			if (dialect == null)
				throw isex("SqlBeanQueryContext requires a dialect.");
			if (table == null)
				throw isex("SqlBeanQueryContext requires a table.");
			if (connectionSupplier == null)
				throw isex("SqlBeanQueryContext requires a connection source.");
			if (rowMapper != null)
				return new SqlBeanQueryContext<>(this, rowMapper, null);
			var selection = defaultRowMapper(type);
			return new SqlBeanQueryContext<>(this, selection.mapper(), selection.reason());
		}

		// Default row-mapper selection; runs only when no rowMapper(...) was set.
		@SuppressWarnings({
			"unchecked" // A LinkedHashMap row is assignable to every T the Map branch accepts.
		})
		private static <T> MapperSelection<T> defaultRowMapper(Class<T> type) {
			if (type == Object.class)
				return new MapperSelection<>(null, null);
			if (Map.class.isAssignableFrom(type)) {
				if (type.isAssignableFrom(LinkedHashMap.class))
					return new MapperSelection<>((SqlRowMapper<T>)SqlRowMapper.map(), null);
				return new MapperSelection<>(null, "rows are LinkedHashMap");
			}
			var plan = SqlRowMappers.tryBean(type);
			if (plan.reason() == null)
				return new MapperSelection<>(plan.mapper(), null);
			return new MapperSelection<>(null, format("cannot map rows to '%s': %s", type.getSimpleName(), plan.reason()));
		}

		private record MapperSelection<T>(SqlRowMapper<T> mapper, String reason) {}
	}

	/**
	 * Returns a new builder.
	 *
	 * @param <T> The row type.
	 * @param type The row type.  Fixes {@code T} and is returned by {@link #getType()}; it is introspected only to
	 * 	choose the default row mapper, never for columns.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <T> Builder<T> create(Class<T> type) {
		return new Builder<>(reqnn("type", type));
	}

	private final Class<T> type;
	private final SqlDialect dialect;
	final String table;
	private final SqlRowMapper<T> rowMapper;
	private final String rowMapperReason;  // Why no default mapper could be chosen, or null.
	private final Map<String,String> columnSql;
	private final SqlFragment guard;
	private final Supplier<Connection> connectionSupplier;
	private final Consumer<Connection> connectionReleaser;
	final int fetchSize;
	final Duration queryTimeout;

	SqlBeanQueryContext(Builder<T> builder, SqlRowMapper<T> rowMapper, String rowMapperReason) {
		super(builder);
		type = builder.type;
		dialect = builder.dialect;
		table = builder.table;
		this.rowMapper = rowMapper;
		this.rowMapperReason = rowMapperReason;
		columnSql = Map.copyOf(builder.columnSql);
		guard = builder.guard;
		connectionSupplier = builder.connectionSupplier;
		connectionReleaser = builder.connectionReleaser;
		fetchSize = builder.fetchSize;
		queryTimeout = builder.queryTimeout;
	}

	/**
	 * Returns the row type passed to {@link #create(Class)}.
	 *
	 * @return The row type.  Never <jk>null</jk>.
	 */
	public Class<T> getType() {
		return type;
	}

	@Override /* BeanQueryContext */
	public Builder<T> copy() {
		return new Builder<>(this);
	}

	@Override /* BeanQueryContext */
	public SqlBeanQuerySession.Builder<T> createSession() {
		return new SqlBeanQuerySession.Builder<>(this);
	}

	/**
	 * Opens a session on a connection from the configured source, with this context's settings as-is.
	 *
	 * @return A new session.  Close it to release the connection.
	 * @throws BeanQueryExecutionException If no connection can be obtained.
	 */
	public SqlBeanQuerySession<T> getSession() {
		return createSession().build();
	}

	@Override /* ColumnResolver */
	public String columnSql(String column) {
		return dialect.quote(columnSql.getOrDefault(column, column));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Statement rendering (no connection)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Renders the row-selection statement for a validated query.
	 *
	 * @param query The validated query, from {@link #resolve(BeanQuery)}.  Must not be <jk>null</jk>.
	 * @return The {@code SELECT} statement.
	 */
	public SqlStatement renderRows(ResolvedQuery query) {
		return renderRows(query, null);
	}

	SqlStatement renderRows(ResolvedQuery query, SqlFragment sessionGuard) {
		reqnn("query", query);
		checkOrigin(query);
		var cols = query.view();
		var sb = new StringBuilder("SELECT ");
		sb.append(cols.stream().map(this::columnSql).collect(Collectors.joining(", ")));
		var binds = new ArrayList<>();
		sb.append(" FROM ").append(dialect.quote(table));
		appendWhere(sb, binds, query, true, sessionGuard);
		appendOrderBy(sb, query);
		var limit = query.limit() == null ? null : Long.valueOf(query.limit());
		var offset = query.position() == 0 ? null : Long.valueOf(query.position());
		sb.append(dialect.renderLimitOffset(limit, offset));
		return new SqlStatement(sb.toString(), binds, cols, containsRegex(query.filter()));
	}

	/**
	 * Renders a count statement for a validated query.
	 *
	 * @param query The validated query, from {@link #resolve(BeanQuery)}.  Must not be <jk>null</jk>.
	 * @param applySearch <jk>true</jk> for the matched count (guards and search); <jk>false</jk> for the total count
	 * 	(guards only).
	 * @return The {@code COUNT} statement.
	 */
	public SqlStatement renderCount(ResolvedQuery query, boolean applySearch) {
		return renderCount(query, applySearch, null);
	}

	SqlStatement renderCount(ResolvedQuery query, boolean applySearch, SqlFragment sessionGuard) {
		reqnn("query", query);
		checkOrigin(query);
		var sb = new StringBuilder("SELECT count(*) FROM ").append(dialect.quote(table));
		var binds = new ArrayList<>();
		appendWhere(sb, binds, query, applySearch, sessionGuard);
		return new SqlStatement(sb.toString(), binds, List.of(), applySearch && containsRegex(query.filter()));
	}

	private void appendWhere(StringBuilder sb, List<Object> binds, ResolvedQuery query, boolean applySearch, SqlFragment sessionGuard) {
		var parts = new ArrayList<SqlFragment>();
		// guard/sessionGuard are raw, caller-supplied SQL this library cannot verify is internally safe to AND
		// unwrapped (e.g. it may itself be a top-level OR), so each is defensively parenthesized here.
		for (var g : new SqlFragment[] {guard, sessionGuard})
			if (g != null)
				parts.add(SqlFragment.of("(" + g.sql() + ")", g.binds()));  // Parenthesized so an OR cannot escape.
		if (applySearch) {
			var filter = query.filter();
			if (! (filter instanceof Filter.And and && and.items().isEmpty())) {
				// Unlike guard/sessionGuard, search comes from SqlSearchCompiler.compile(Filter), which already
				// guarantees its own top-level rendering is safe to AND with other WHERE parts (a multi-item
				// top-level And/Or parenthesizes itself; a top-level Not/Leaf is already one atomic, boundaried
				// unit) -- so wrapping it again here would just double-parenthesize it.
				parts.add(SqlSearchCompiler.create(dialect, this).compile(filter));
			}
		}
		if (parts.isEmpty())
			return;
		sb.append(" WHERE ");
		sb.append(parts.stream().map(SqlFragment::sql).collect(Collectors.joining(" AND ")));
		parts.forEach(p -> binds.addAll(p.binds()));
	}

	// True if any leaf anywhere in the filter uses the $regex operator. If filter contains a $regex leaf at all,
	// allowRegex was necessarily true when QueryResolver resolved it (checkRegex gates this earlier in the
	// pipeline), so there is nothing left to re-check here -- only to detect, for the statement-timeout fallback.
	private static boolean containsRegex(Filter filter) {
		if (filter instanceof Filter.Leaf leaf)
			return containsRegex(leaf.expression());
		if (filter instanceof Filter.Not not)
			return containsRegex(not.item());
		if (filter instanceof Filter.And and)
			return and.items().stream().anyMatch(SqlBeanQueryContext::containsRegex);
		if (filter instanceof Filter.Or or)
			return or.items().stream().anyMatch(SqlBeanQueryContext::containsRegex);
		throw new IllegalStateException("Unknown Filter type: " + filter);
	}

	private static boolean containsRegex(SearchExpression node) {
		if (node.isLiteral())
			return false;
		if ("$regex".equals(node.operator().name()))
			return true;
		return node.args().stream().anyMatch(SqlBeanQueryContext::containsRegex);
	}

	private void appendOrderBy(StringBuilder sb, ResolvedQuery query) {
		var keys = query.sort();
		if (keys.isEmpty())
			return;
		sb.append(" ORDER BY ");
		sb.append(keys.stream().map(k -> columnSql(k.column()) + (k.descending() ? " DESC" : " ASC")).collect(Collectors.joining(", ")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Session support
	//-----------------------------------------------------------------------------------------------------------------

	SqlRowMapper<T> rowMapper() {
		if (rowMapper == null) {
			if (rowMapperReason == null)
				throw isex("SqlBeanQueryContext requires a rowMapper for find and stream.");
			throw isex("SqlBeanQueryContext requires a rowMapper for find and stream: %s.", rowMapperReason);
		}
		return rowMapper;
	}

	Connection acquire() {
		Connection c;
		try {
			c = connectionSupplier.get();
		} catch (BeanQueryExecutionException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new BeanQueryExecutionException(e, "Failed to open a database connection.");
		}
		if (c == null)
			throw new BeanQueryExecutionException(null, "Failed to open a database connection.");
		return c;
	}

	void release(Connection c) {
		try {
			connectionReleaser.accept(c);
		} catch (BeanQueryExecutionException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new BeanQueryExecutionException(e, "Failed to release a database connection.");
		}
	}

	@SuppressWarnings({
		"java:S1144" // Used as a method reference by the nested Builder; the analyzer misses that.
	})
	private static void closeConnection(Connection c) {
		try {
			c.close();
		} catch (SQLException e) {
			throw new BeanQueryExecutionException(e, "Failed to release a database connection.");
		}
	}
}
