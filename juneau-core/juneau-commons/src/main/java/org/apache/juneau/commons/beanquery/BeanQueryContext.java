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
 * The reusable, immutable, server-side half of a BeanQuery engine: which columns may be searched, sorted and viewed,
 * which operators apply, and the limits every session enforces.
 *
 * <p>
 * Build a context once, at startup, from its engine's {@code create(...)} factory.  It holds no rows and no
 * connections, so it can be shared across threads.  Open a {@link BeanQuerySession} per request with the engine's
 * {@code getSession(...)} shortcut or with {@link #createSession()}.  Call {@link #copy()} to derive a variant.
 *
 * <p>
 * The base class owns every engine-independent check (spec §5): the column allow-list, operator/type checks, the
 * regex gate, the size caps, limit clamping and the count policy.  Engines see only a {@link ResolvedQuery}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Once, at startup.</jc>
 * 	InMemoryBeanQueryContext&lt;Person&gt; <jv>people</jv> = InMemoryBeanQueryContext
 * 		.<jsm>create</jsm>(Person.<jk>class</jk>)       <jc>// Columns: every getter on Person.</jc>
 * 		.exclude(<js>"passwordHash"</js>)            <jc>// Never searchable, sortable or visible.</jc>
 * 		.column(<js>"version"</js>, SearchType.<jsf>VERSION</jsf>)
 * 		.maxLimit(500)
 * 		.build();
 *
 * 	<jc>// A stricter variant for a public page.</jc>
 * 	InMemoryBeanQueryContext&lt;Person&gt; <jv>guest</jv> = <jv>people</jv>.copy().allowRegex(<jk>false</jk>).defaultLimit(50).maxLimit(50).build();
 *
 * 	<jc>// Per request.</jc>
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>people</jv>.getSession(<jv>rows</jv>)) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(<jv>query</jv>);
 * 	}
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1452" // Public self-typed builder API; the concrete SELF is unknowable to callers.
})
public abstract class BeanQueryContext<T> {

	/**
	 * The settings every engine's context builder shares.
	 *
	 * <p>
	 * Single-argument problems throw {@link IllegalArgumentException} from the setter.  Missing or conflicting settings
	 * throw {@link IllegalStateException} from {@link #build()}.
	 *
	 * @param <T> The row type.
	 * @param <SELF> The concrete builder type, returned by every setter.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<T,SELF extends Builder<T,SELF>> {

		final QuerySettings settings;
		private boolean defaultLimitExplicit;  // True once defaultLimit(int) is called on THIS builder.

		/** Constructor for a new, empty builder. */
		protected Builder() {
			settings = new QuerySettings();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The context whose settings are copied.  Must not be <jk>null</jk>.
		 */
		protected Builder(BeanQueryContext<T> copyFrom) {
			settings = reqnn("copyFrom", copyFrom).settings.copy();
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
		 * Declares a column, or changes the type of a declared one (keeping its position).
		 *
		 * @param name The column name.  Must not be blank.
		 * @param type The column's value type.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF column(String name, SearchType type) {
			reqnb("name", name);
			settings.columns.put(name, reqnn("type", type));
			return self();
		}

		/**
		 * Keeps only the specified declared columns, in the specified order.
		 *
		 * @param names The column names.  Each must already be declared.
		 * @return This object.
		 */
		public SELF columns(String...names) {
			var m = new LinkedHashMap<String,SearchType>();
			for (var n : reqnns("names", names))
				m.put(n, settings.columns.get(declared(n)));
			settings.columns.clear();
			settings.columns.putAll(m);
			settings.columnOperators.keySet().retainAll(m.keySet());
			return self();
		}

		/**
		 * Removes declared columns (and any operator override on them).
		 *
		 * @param names The column names.  Each must be declared.
		 * @return This object.
		 */
		public SELF exclude(String...names) {
			for (var n : reqnns("names", names)) {
				settings.columns.remove(declared(n));
				settings.columnOperators.remove(n);
			}
			return self();
		}

		/**
		 * Checks that a column is declared.
		 *
		 * @param name The column name.
		 * @return The same name.
		 * @throws IllegalArgumentException If the column is not declared.
		 */
		protected final String declared(String name) {
			if (! settings.columns.containsKey(name))
				throw iaex("Column '%s' is not declared.", name);
			return name;
		}

		/**
		 * Sets the default operator set (for columns without an override).  Default: {@link SearchOperatorSet#standard()}.
		 *
		 * @param value The operator set.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF operators(SearchOperatorSet value) {
			settings.defaultOperators = reqnn("value", value);
			return self();
		}

		/**
		 * Replaces the operator set for one declared column.
		 *
		 * @param column The column name.  Must be declared.
		 * @param value The operator set.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF columnOperators(String column, SearchOperatorSet value) {
			settings.columnOperators.put(declared(column), reqnn("value", value));
			return self();
		}

		/**
		 * Allows the {@code $regex} operator.  Default: <jk>true</jk>; {@code allowRegex(false)} disables it.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public SELF allowRegex(boolean value) {
			settings.allowRegex = value;
			return self();
		}

		/**
		 * Sets the time budget for all {@code $regex} matching in one session call.  Default: 50 ms.
		 *
		 * @param value The budget.  Must be positive.
		 * @return This object.
		 */
		public SELF regexTimeout(Duration value) {
			settings.regexTimeout = QuerySettings.positive("regexTimeout", value);
			return self();
		}

		/**
		 * Sets which counts {@code find} / {@code findValues} compute.  Default: {@link CountPolicy#IF_REQUESTED}.
		 *
		 * @param value The policy.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF countPolicy(CountPolicy value) {
			settings.countPolicy = reqnn("value", value);
			return self();
		}

		/**
		 * Sets the limit used when a query has none.  Default: 100.
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
		 * Sets the largest limit a query can ask for; larger limits and {@code limit=-1} are clamped to it.
		 * Default: 1000.
		 *
		 * <p>
		 * If this builder's {@code defaultLimit} was never explicitly set (it is still {@code QuerySettings}'s own
		 * baseline default, or — for a builder from {@link #copy()} — still whatever the source context had),
		 * narrowing {@code maxLimit} below it auto-narrows {@code defaultLimit} to match, rather than leaving the two
		 * in conflict. An explicitly-chosen {@code defaultLimit} is never silently changed by this setter —
		 * a resulting conflict is left for {@link #build()} to reject.
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
		 * Sets the largest number of search clauses.  Default: 64.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxSearchClauses(int value) {
			settings.maxSearchClauses = QuerySettings.positive("maxSearchClauses", value);
			return self();
		}

		/**
		 * Sets the largest number of sort keys.  Default: 8.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxSortKeys(int value) {
			settings.maxSortKeys = QuerySettings.positive("maxSortKeys", value);
			return self();
		}

		/**
		 * Sets the longest search, sort, view or opts string.  Default: 4096.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxSearchLength(int value) {
			settings.maxSearchLength = QuerySettings.positive("maxSearchLength", value);
			return self();
		}

		/**
		 * Sets the deepest allowed expression nesting.  Default: 16.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public SELF maxExpressionDepth(int value) {
			settings.maxExpressionDepth = QuerySettings.positive("maxExpressionDepth", value);
			return self();
		}

		/**
		 * Builds the immutable context.
		 *
		 * @return A new context.
		 * @throws IllegalStateException If a required setting is missing or two settings conflict.
		 */
		public abstract BeanQueryContext<T> build();
	}

	final QuerySettings settings;

	/**
	 * Constructor.  Takes a private copy of the builder's settings and validates it.
	 *
	 * @param builder The builder.  Must not be <jk>null</jk>.
	 * @throws IllegalStateException If the settings are invalid.
	 */
	protected BeanQueryContext(Builder<T,?> builder) {
		settings = reqnn("builder", builder).settings.copy();
		settings.validate();
	}

	/**
	 * Returns the declared columns, in order: the allow-list for search, sort and view, and the default view.
	 *
	 * @return An unmodifiable list.
	 */
	public final List<String> columns() {
		return List.copyOf(settings.columns.keySet());
	}

	/**
	 * Returns the default operator set a client may use.  {@code $regex} is absent if regex is disallowed.
	 *
	 * @return The operator set.
	 */
	public final SearchOperatorSet operatorSet() {
		return visible(settings.defaultOperators);
	}

	/**
	 * Returns the operator set a client may use on one column.  {@code $regex} is absent if regex is disallowed.
	 *
	 * @param column The column name.
	 * @return The column's operator set, else the default set.
	 */
	public final SearchOperatorSet operatorSet(String column) {
		return visible(settings.operators(column));
	}

	private SearchOperatorSet visible(SearchOperatorSet s) {
		return settings.allowRegex ? s : s.without("$regex");
	}

	/**
	 * Returns the type stored for a declared column: the type given to {@link Builder#column(String, SearchType)
	 * column}, or the type inferred for it (for example, {@link InMemoryBeanQueryContext#create(Class)
	 * InMemoryBeanQueryContext.create(Class)} infers one per bean getter).
	 *
	 * @param name The column name.
	 * @return The column's type.  Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the column is not declared.
	 */
	public final SearchType getColumnType(String name) {
		if (! settings.columns.containsKey(name))
			throw iaex("Unknown column '%s'.", name);
		return settings.columns.get(name);
	}

	/**
	 * Returns the largest number of search clauses a query may use.
	 *
	 * @return The cap.
	 */
	public final int getMaxSearchClauses() {
		return settings.maxSearchClauses;
	}

	/**
	 * Returns the largest number of sort keys a query may use.
	 *
	 * @return The cap.
	 */
	public final int getMaxSortKeys() {
		return settings.maxSortKeys;
	}

	/**
	 * Validates a query against this context's rules without running it.
	 *
	 * @param query The query.  Must not be <jk>null</jk>.
	 * @return The resolved query.
	 * @throws BeanQuerySyntaxException If the query breaks a rule.
	 */
	public final ResolvedQuery resolve(BeanQuery query) {
		return QueryResolver.resolve(reqnn("query", query), settings, this);
	}

	/**
	 * Confirms that {@code query} was produced by resolving against this context (directly, or through a session
	 * built from it) before an engine uses it.  An engine's rendering/execution code that accepts a caller-supplied
	 * {@link ResolvedQuery} &mdash; rather than one it just resolved itself &mdash; calls this first, so a query
	 * resolved against a different context's columns is rejected with a clear message instead of silently rendering
	 * (or running) against the wrong schema.
	 *
	 * @param query The query to check.  Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code query} was not produced by this context.
	 */
	protected final void checkOrigin(ResolvedQuery query) {
		req(query.isResolvedBy(this), "This ResolvedQuery was not produced by this context (resolve it via this context, or a session built from it).");
	}

	/**
	 * Returns a builder initialized from this context.  Building it leaves this context unchanged.
	 *
	 * @return A new builder.
	 */
	public abstract Builder<T,?> copy();

	/**
	 * Returns a builder for a session over this context.
	 *
	 * @return A new session builder.
	 */
	public abstract BeanQuerySession.Builder<T,?> createSession();
}
