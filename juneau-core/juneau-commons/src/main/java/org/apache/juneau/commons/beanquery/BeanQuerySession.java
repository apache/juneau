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
import java.util.function.*;
import java.util.stream.*;

/**
 * One unit of query work against a {@link BeanQueryContext}: the rows (or connection) to query, plus optional
 * per-request narrowing of the context's settings.
 *
 * <p>
 * Open a session per request and close it with try-with-resources.  A session is not thread-safe.  Its public methods
 * are {@code final}: each validates the {@link BeanQuery} into a {@link ResolvedQuery} (throwing
 * {@link BeanQuerySyntaxException} for bad input) and then calls the engine's {@code doXxx} hook.  Engine failures
 * throw {@link BeanQueryExecutionException}.
 *
 * <p>
 * {@link Builder#restrictColumns(String...)} only narrows the context's columns.  The limit, cap and count-policy
 * overrides replace the context's values for this session (so they can also raise or remove a cap); they are for
 * trusted server-side callers only, never request data.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Shortcut: the context's settings as-is.</jc>
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.getSession(<jv>rows</jv>)) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(<jv>query</jv>);
 * 	}
 *
 * 	<jc>// Narrowed for a guest user.</jc>
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.createSession()
 * 			.rows(<jv>rows</jv>)
 * 			.restrictColumns(<js>"name"</js>, <js>"city"</js>)
 * 			.defaultLimit(20)
 * 			.maxLimit(20)
 * 			.build()) {
 * 		List&lt;Map&lt;String,Object&gt;&gt; <jv>values</jv> = <jv>session</jv>.findValues(<jv>query</jv>).rows();
 * 	}
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
public abstract class BeanQuerySession<T> implements AutoCloseable {

	/**
	 * The settings every engine's session builder shares.  Each starts from the context's value.
	 *
	 * @param <T> The row type.
	 * @param <SELF> The concrete builder type, returned by every setter.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<T,SELF extends Builder<T,SELF>> {

		private final BeanQueryContext<T> context;
		final QuerySettings settings;
		private boolean defaultLimitExplicit;  // True once defaultLimit(int) is called on THIS builder.

		/**
		 * Constructor.
		 *
		 * @param context The context the session runs against.  Must not be <jk>null</jk>.
		 */
		protected Builder(BeanQueryContext<T> context) {
			this.context = reqnn("context", context);
			settings = this.context.settings.copy();
		}

		/**
		 * Returns this builder as its concrete type.
		 *
		 * @return This object.
		 */
		@SuppressWarnings({
			"unchecked" // CRTP self-type cast is safe by construction.
		})
		protected final SELF self() {
			return (SELF)this;
		}

		/**
		 * Keeps only the specified columns for this session.  The context's column order is kept.
		 *
		 * @param names The column names.  Each must be a column of the context (or of an earlier restriction).
		 * @return This object.
		 * @throws IllegalArgumentException If a name is not a current column.
		 */
		public SELF restrictColumns(String...names) {
			var keep = new HashSet<String>();
			for (var n : reqnns("names", names)) {
				if (! settings.columns.containsKey(n))
					throw iaex("Column '%s' is not a column of this context; a session can only narrow the columns.", n);
				keep.add(n);
			}
			settings.columns.keySet().retainAll(keep);
			settings.columnOperators.keySet().retainAll(keep);
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#regexTimeout(Duration)} for this session.
		 *
		 * @param value The budget.  Must be positive.
		 * @return This object.
		 */
		public SELF regexTimeout(Duration value) {
			settings.regexTimeout = QuerySettings.positive("regexTimeout", value);
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#countPolicy(CountPolicy)} for this session.
		 *
		 * @param value The policy.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF countPolicy(CountPolicy value) {
			settings.countPolicy = reqnn("value", value);
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#defaultLimit(int)} for this session.
		 *
		 * @param value The limit.  Must be positive.
		 * @return This object.
		 */
		public SELF defaultLimit(int value) {
			settings.defaultLimit = QuerySettings.positive("defaultLimit", value);
			defaultLimitExplicit = true;
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#maxLimit(Integer)} for this session.
		 *
		 * <p>
		 * If this session's {@code defaultLimit} was never explicitly set on this builder (it is still the value
		 * inherited from the context), narrowing {@code maxLimit} below it auto-narrows {@code defaultLimit} to match,
		 * rather than leaving the two in conflict. An explicitly-chosen {@code defaultLimit} is never
		 * silently changed by this setter — a resulting conflict is left for {@link #build()} to reject.
		 *
		 * @param value The cap, or <jk>null</jk> for no cap.  Must be positive if set.
		 * @return This object.
		 */
		public SELF maxLimit(Integer value) {
			settings.maxLimit = QuerySettings.positiveOrNull("maxLimit", value);
			if (! defaultLimitExplicit && settings.maxLimit != null && settings.defaultLimit > settings.maxLimit)
				settings.defaultLimit = settings.maxLimit;
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#maxSearchClauses(int)} for this session.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxSearchClauses(int value) {
			settings.maxSearchClauses = QuerySettings.positive("maxSearchClauses", value);
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#maxSortKeys(int)} for this session.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxSortKeys(int value) {
			settings.maxSortKeys = QuerySettings.positive("maxSortKeys", value);
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#maxSearchLength(int)} for this session.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxSearchLength(int value) {
			settings.maxSearchLength = QuerySettings.positive("maxSearchLength", value);
			return self();
		}

		/**
		 * Overrides {@link BeanQueryContext.Builder#maxExpressionDepth(int)} for this session.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxExpressionDepth(int value) {
			settings.maxExpressionDepth = QuerySettings.positive("maxExpressionDepth", value);
			return self();
		}

		/**
		 * Builds the session.
		 *
		 * @return A new session.  Close it when done.
		 * @throws IllegalStateException If a required setting is missing or two settings conflict.
		 */
		public abstract BeanQuerySession<T> build();
	}

	private final BeanQueryContext<T> context;
	final QuerySettings settings;

	/**
	 * Constructor.  Takes a private copy of the builder's settings and validates it.
	 *
	 * @param builder The builder.  Must not be <jk>null</jk>.
	 * @throws IllegalStateException If the settings are invalid.
	 */
	protected BeanQuerySession(Builder<T,?> builder) {
		context = reqnn("builder", builder).context;
		settings = builder.settings.copy();
		settings.validate();
	}

	/**
	 * Returns this session's columns, in order (the context's columns after any {@link Builder#restrictColumns
	 * restriction}).
	 *
	 * @return An unmodifiable list.
	 */
	public final List<String> columns() {
		return List.copyOf(settings.columns.keySet());
	}

	/**
	 * Returns the context that created this session (through {@link BeanQueryContext#createSession()} or an
	 * engine's {@code getSession(...)} shortcut).
	 *
	 * <p>
	 * Engines whose session builder is given a narrower context type (for example,
	 * {@code InMemoryBeanQuerySession}) override this with a covariant return type.
	 *
	 * @return The context.  Never <jk>null</jk>.
	 */
	public BeanQueryContext<T> getContext() {
		return context;
	}

	/**
	 * Returns the largest number of search clauses a query may use in this session: the context's cap, or this
	 * session's {@link Builder#maxSearchClauses(int) override}.
	 *
	 * @return The cap.
	 */
	public final int getMaxSearchClauses() {
		return settings.maxSearchClauses;
	}

	/**
	 * Returns the largest number of sort keys a query may use in this session: the context's cap, or this session's
	 * {@link Builder#maxSortKeys(int) override}.
	 *
	 * @return The cap.
	 */
	public final int getMaxSortKeys() {
		return settings.maxSortKeys;
	}

	/**
	 * Validates a query against this session's rules without running it.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The resolved query.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 */
	public final ResolvedQuery resolve(BeanQuery query) {
		return QueryResolver.resolve(reqnn("query", query), settings, context);
	}

	/**
	 * Returns one page of matching rows, plus the counts the {@link CountPolicy} selects.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The page.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 * @throws BeanQueryExecutionException If the engine fails.
	 */
	public final Page<T> find(BeanQuery query) {
		return doFind(resolve(query));
	}

	/**
	 * Like {@link #find(BeanQuery)}, but each row is a map of the viewed columns, in view order.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The page.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 * @throws BeanQueryExecutionException If the engine fails.
	 */
	public final Page<Map<String,Object>> findValues(BeanQuery query) {
		return doFindValues(resolve(query));
	}

	/**
	 * Returns both counts, whatever the {@link CountPolicy}.  Sort, view and paging are ignored.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The counts.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 * @throws BeanQueryExecutionException If the engine fails.
	 */
	public final Counts count(BeanQuery query) {
		return doCount(resolve(query));
	}

	/**
	 * Streams one page of matching rows.  Close the stream (or the session) to release engine resources.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The rows.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 * @throws BeanQueryExecutionException If the engine fails.
	 */
	public final Stream<T> stream(BeanQuery query) {
		return doStream(resolve(query));
	}

	/**
	 * Like {@link #stream(BeanQuery)}, but each row is a map of the viewed columns, in view order.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The rows.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 * @throws BeanQueryExecutionException If the engine fails.
	 */
	public final Stream<Map<String,Object>> streamValues(BeanQuery query) {
		return doStreamValues(resolve(query));
	}

	/**
	 * Engine hook for {@link #find(BeanQuery)}.
	 *
	 * @param query The validated query.
	 * @return The page.  Build it with {@link #page(List, ResolvedQuery, LongSupplier, LongSupplier)}.
	 */
	protected abstract Page<T> doFind(ResolvedQuery query);

	/**
	 * Engine hook for {@link #findValues(BeanQuery)}.
	 *
	 * @param query The validated query.
	 * @return The page.
	 */
	protected abstract Page<Map<String,Object>> doFindValues(ResolvedQuery query);

	/**
	 * Engine hook for {@link #count(BeanQuery)}.
	 *
	 * @param query The validated query.
	 * @return The counts.
	 */
	protected abstract Counts doCount(ResolvedQuery query);

	/**
	 * Engine hook for {@link #stream(BeanQuery)}.
	 *
	 * @param query The validated query.
	 * @return The rows.
	 */
	protected abstract Stream<T> doStream(ResolvedQuery query);

	/**
	 * Engine hook for {@link #streamValues(BeanQuery)}.
	 *
	 * @param query The validated query.
	 * @return The rows.
	 */
	protected abstract Stream<Map<String,Object>> doStreamValues(ResolvedQuery query);

	/**
	 * Builds a page, computing only the counts the query's {@link ResolvedQuery#counts() policy} selects.
	 *
	 * @param <R> The row type.
	 * @param rows The page's rows.
	 * @param query The validated query.
	 * @param total Computes the total count.  Called only under {@link CountPolicy#BOTH}.
	 * @param matched Computes the matched count.  Called only under {@link CountPolicy#MATCHED} or {@link CountPolicy#BOTH}.
	 * @return The page.
	 */
	protected static <R> Page<R> page(List<R> rows, ResolvedQuery query, LongSupplier total, LongSupplier matched) {
		switch (query.counts()) {
			case BOTH:    return Page.of(rows, total.getAsLong(), matched.getAsLong());
			case MATCHED: return Page.ofMatched(rows, matched.getAsLong());
			default:      return Page.of(rows);
		}
	}

	/**
	 * Releases whatever the session acquired.  Never throws a checked exception.
	 */
	@Override /* AutoCloseable */
	public abstract void close();
}
