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

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.stream.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.commons.collections.*;

/**
 * A {@link BeanQuerySession} on one JDBC connection.
 *
 * <p>
 * The connection is either passed in with {@link Builder#connection(Connection)} (the caller owns it and this session
 * never closes it) or taken from the context's connection source (this session gives it back on {@link #close()}).
 * {@link #stream(BeanQuery)} and {@link #streamValues(BeanQuery)} read rows on demand; closing the stream, or the
 * session, closes the cursor.  Every statement gets the context's fetch size and query timeout, and, for a
 * statement whose search includes {@code $regex}, a bounded default timeout if no explicit {@code queryTimeout} was
 * configured.  A closed session rejects further queries with {@link IllegalStateException}.
 *
 * <p>
 * The counts from {@link #find(BeanQuery)} and {@link #count(BeanQuery)} come from separate statements.  They are a
 * consistent snapshot only if the caller's connection runs them in one suitably isolated transaction.
 *
 * <p>
 * A failing statement becomes a {@link BeanQueryExecutionException} naming only the table; the SQL text (with
 * {@code ?} placeholders) is logged at {@link Level#WARNING} with the driver's exception, whose message may include
 * values.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Caller-owned connection and transaction; a per-request tenant guard.</jc>
 * 	<jk>try</jk> (SqlBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.createSession()
 * 			.connection(<jv>connection</jv>)
 * 			.guard(SqlFragment.<jsm>of</jsm>(<js>"\"tenant\" = ?"</js>, <jv>tenant</jv>))
 * 			.maxLimit(200)
 * 			.build()) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(<jv>query</jv>);
 * 		<jk>try</jk> (Stream&lt;Map&lt;String,Object&gt;&gt; <jv>rows</jv> = <jv>session</jv>.streamValues(<jv>exportQuery</jv>)) {
 * 			<jv>rows</jv>.forEach(<jv>csv</jv>::write);
 * 		}
 * 	}
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
public final class SqlBeanQuerySession<T> extends BeanQuerySession<T> {

	private static final Logger LOG = Logger.getLogger(SqlBeanQuerySession.class.getName());

	// Ceiling for the timeout seconds handed to PreparedStatement.setQueryTimeout(int).  Clamped below
	// Integer.MAX_VALUE, not at it: at least one JDBC driver (H2 2.3.232) computes seconds*1000 as a 32-bit int
	// inside its own setQueryTimeout(int), which overflows near Integer.MAX_VALUE seconds -- this bound keeps that
	// downstream multiplication safe too, while still being functionally "no timeout" for any real use (~24.8 days).
	private static final int MAX_QUERY_TIMEOUT_SECONDS = Integer.MAX_VALUE / 1000;

	/**
	 * Builder for {@link SqlBeanQuerySession}.  See the class Javadoc for an example.
	 *
	 * @param <T> The row type.
	 */
	public static final class Builder<T> extends BeanQuerySession.Builder<T,Builder<T>> {

		private final SqlBeanQueryContext<T> context;
		private Connection connection;
		private SqlFragment guard;

		Builder(SqlBeanQueryContext<T> context) {
			super(context);
			this.context = context;
		}

		/**
		 * Runs this session on a caller-owned connection instead of one from the context's connection source.  The
		 * session never closes it.
		 *
		 * @param value The connection.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T> connection(Connection value) {
			connection = reqnn("value", value);
			return this;
		}

		/**
		 * Sets a per-request SQL predicate, AND-ed (in parentheses) with the context guard into every statement.
		 *
		 * @param value The guard, or <jk>null</jk> for none.
		 * @return This object.
		 */
		public Builder<T> guard(SqlFragment value) {
			guard = value;
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @throws BeanQueryExecutionException If no connection was given and none can be obtained.
		 */
		@Override /* BeanQuerySession.Builder */
		public SqlBeanQuerySession<T> build() {
			return new SqlBeanQuerySession<>(this);
		}
	}

	private final SqlBeanQueryContext<T> context;
	private final SqlFragment guard;
	private final Connection connection;
	private final boolean owned;
	private final List<Cursor<?>> open = new ArrayList<>();
	private boolean closed;

	SqlBeanQuerySession(Builder<T> builder) {
		super(builder);  // Validates the settings before a connection is taken.
		context = builder.context;
		guard = builder.guard;
		owned = builder.connection == null;
		connection = owned ? context.acquire() : builder.connection;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The context that created this session.
	 */
	@Override /* BeanQuerySession */
	public SqlBeanQueryContext<T> getContext() {
		return context;
	}

	@Override /* BeanQuerySession */
	protected Page<T> doFind(ResolvedQuery query) {
		var mapper = context.rowMapper();
		var stmt = context.renderRows(query, guard);
		return page(list(stmt, rs -> mapper.map(rs, stmt.columns())), query, () -> countRows(query, false), () -> countRows(query, true));
	}

	@Override /* BeanQuerySession */
	protected Page<Map<String,Object>> doFindValues(ResolvedQuery query) {
		var stmt = context.renderRows(query, guard);
		return page(list(stmt, rs -> valueRow(rs, stmt.columns())), query, () -> countRows(query, false), () -> countRows(query, true));
	}

	@Override /* BeanQuerySession */
	protected Counts doCount(ResolvedQuery query) {
		return Counts.of(countRows(query, false), countRows(query, true));
	}

	@Override /* BeanQuerySession */
	protected Stream<T> doStream(ResolvedQuery query) {
		var mapper = context.rowMapper();
		var stmt = context.renderRows(query, guard);
		return stream(new Cursor<>(stmt, rs -> mapper.map(rs, stmt.columns())));
	}

	@Override /* BeanQuerySession */
	protected Stream<Map<String,Object>> doStreamValues(ResolvedQuery query) {
		var stmt = context.renderRows(query, guard);
		return stream(new Cursor<>(stmt, rs -> valueRow(rs, stmt.columns())));
	}

	/**
	 * Closes any open streams, then gives back the connection if this session took it from the context.  Calling it
	 * again does nothing.  After this, every query method throws {@link IllegalStateException}.
	 *
	 * @throws BeanQueryExecutionException If the connection cannot be released.
	 */
	@Override /* BeanQuerySession */
	public void close() {
		if (closed)
			return;
		closed = true;
		for (var c : new ArrayList<>(open))
			c.closeBySession();
		if (owned)
			context.release(connection);
	}

	private long countRows(ResolvedQuery query, boolean applySearch) {
		var out = list(context.renderCount(query, applySearch, guard), rs -> rs.getLong(1));
		return out.isEmpty() ? 0L : out.get(0);
	}

	private <R> List<R> list(SqlStatement stmt, RowReader<R> reader) {
		assertOpen();  // Outside the try: a closed-session IllegalStateException must surface unwrapped.
		try (var ps = prepare(stmt); var rs = ps.executeQuery()) {
			var out = new ArrayList<R>();
			while (rs.next())
				out.add(reader.read(rs));
			return out;
		} catch (BeanQueryExecutionException | BeanQuerySyntaxException e) {
			throw e;
		} catch (SQLException | RuntimeException e) {  // Includes the rowMapper throwing.
			throw failed(stmt, e);
		}
	}

	private void assertOpen() {
		if (closed)
			throw isex("SqlBeanQuerySession is closed.");
	}

	private PreparedStatement prepare(SqlStatement stmt) throws SQLException {
		assertOpen();
		var ps = connection.prepareStatement(stmt.sql());
		try {
			ps.setFetchSize(context.fetchSize);
			var timeout = effectiveTimeout(stmt);
			if (timeout != null)
				ps.setQueryTimeout((int)Math.min(MAX_QUERY_TIMEOUT_SECONDS, Math.max(1, queryTimeoutSeconds(timeout))));
			var binds = stmt.binds();
			for (var i = 0; i < binds.size(); i++)
				ps.setObject(i + 1, binds.get(i));
			return ps;
		} catch (SQLException | RuntimeException e) {
			closeQuietly(ps);
			throw e;
		}
	}

	private Duration effectiveTimeout(SqlStatement stmt) {
		if (context.queryTimeout != null)
			return context.queryTimeout;
		return stmt.hasRegex() ? SqlBeanQueryContext.DEFAULT_REGEX_TIMEOUT : null;
	}

	// Rounds up to the next whole second without ever computing total milliseconds: Duration.toMillis() can throw
	// ArithmeticException for a Duration whose millis would overflow long; getSeconds()/getNano() cannot.
	// The "seconds < Long.MAX_VALUE" guard exists specifically so the "+ 1" itself cannot overflow when
	// getSeconds() is already at Long.MAX_VALUE (reachable via the public builder, e.g.
	// Duration.ofSeconds(Long.MAX_VALUE, 999_999_999)) -- without it, Long.MAX_VALUE + 1 wraps to Long.MIN_VALUE,
	// which would silently survive the caller's Math.max(1, ...) clamp as 1, producing a 1-second timeout instead
	// of the intended effectively-no-timeout behavior.
	private static long queryTimeoutSeconds(Duration d) {
		var seconds = d.getSeconds();
		return (d.getNano() > 0 && seconds < Long.MAX_VALUE) ? seconds + 1 : seconds;
	}

	private BeanQueryExecutionException failed(SqlStatement stmt, Throwable e) {
		LOG.log(Level.WARNING, e, () -> "Bean query failed: " + stmt.sql());  // SQL text with placeholders; the driver's message may include values.
		return new BeanQueryExecutionException(e, "Query execution failed for table '%s'.", context.table);
	}

	private <R> Stream<R> stream(Cursor<R> cursor) {  // Not static: Cursor is an inner class.
		return StreamSupport.stream(cursor, false).onClose(cursor::close);
	}

	private static Map<String,Object> valueRow(ResultSet rs, List<String> columns) throws SQLException {
		var keys = columns.toArray(new String[0]);
		var vals = new Object[keys.length];
		for (var i = 0; i < keys.length; i++)
			vals[i] = rs.getObject(i + 1);
		return new SimpleMap<>(keys, vals);
	}

	private static void closeQuietly(AutoCloseable c) {
		try {
			c.close();
		} catch (Exception e) {  // NOSONAR - A close failure must not hide the real result or error.
			LOG.log(Level.FINE, "Failed to close a JDBC resource.", e);
		}
	}

	@FunctionalInterface
	private interface RowReader<R> {
		R read(ResultSet rs) throws SQLException;
	}

	/** An open statement and result set, read one row per {@link #tryAdvance(Consumer)}. */
	private final class Cursor<R> extends Spliterators.AbstractSpliterator<R> implements AutoCloseable {

		private final SqlStatement stmt;
		private final RowReader<R> reader;
		private PreparedStatement ps;
		private ResultSet rs;
		private boolean done;
		private boolean closedBySession;  // Set only by closeBySession() -- distinguishes a forced shutdown from EOF.

		Cursor(SqlStatement stmt, RowReader<R> reader) {
			super(Long.MAX_VALUE, ORDERED);
			this.stmt = stmt;
			this.reader = reader;
			assertOpen();  // Outside the try: a closed-session IllegalStateException must surface unwrapped.
			try {
				ps = prepare(stmt);
				rs = ps.executeQuery();
			} catch (BeanQueryExecutionException | BeanQuerySyntaxException e) {
				close();
				throw e;
			} catch (SQLException | RuntimeException e) {
				close();
				throw failed(stmt, e);
			}
			open.add(this);
		}

		@Override /* Spliterator */
		public boolean tryAdvance(Consumer<? super R> action) {
			if (done) {
				if (closedBySession)
					throw isex("SqlBeanQuerySession was closed while a stream from it was still open.");
				return false;
			}
			R row;
			try {
				if (! rs.next()) {
					close();
					return false;
				}
				row = reader.read(rs);
			} catch (BeanQueryExecutionException | BeanQuerySyntaxException e) {
				close();
				throw e;
			} catch (SQLException | RuntimeException e) {  // Includes the rowMapper throwing.
				close();
				throw failed(stmt, e);
			}
			// action.accept() runs outside the try: it's the caller's stream-pipeline code (e.g. a downstream
			// Collectors.toMap duplicate-key check), not our SQL/row-mapping logic, so its exceptions must propagate
			// unwrapped rather than being misreported as a query failure.
			action.accept(row);
			return true;
		}

		@Override /* AutoCloseable */
		public void close() {
			if (done)
				return;
			done = true;
			open.remove(this);
			if (rs != null)
				closeQuietly(rs);
			if (ps != null)
				closeQuietly(ps);
		}

		/**
		 * Closes this cursor because the owning session is closing, not because the caller is done with it. A later
		 * {@link #tryAdvance} throws {@link IllegalStateException} instead of looking like the stream simply ran out
		 *. Called only from {@code SqlBeanQuerySession.close()}'s force-close loop over still-open cursors.
		 */
		void closeBySession() {
			closedBySession = true;
			close();
		}
	}
}
