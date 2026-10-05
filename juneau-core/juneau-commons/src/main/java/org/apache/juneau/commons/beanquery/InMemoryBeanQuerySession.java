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

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * A {@link BeanQuerySession} over one in-memory collection.
 *
 * <p>
 * The rows are filtered once, when the session is built, through the context guard <b>and</b> the session guard.
 * Every call then searches, sorts and pages that scoped list.  All {@code $regex} matching in one call shares one
 * deadline ({@link BeanQueryContext.Builder#regexTimeout(java.time.Duration) regexTimeout}).  {@link #close()} is a
 * no-op, but use try-with-resources anyway so the code does not change if the engine does.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.createSession()
 * 			.rows(<jv>people</jv>)
 * 			.guard(<jv>p</jv> -&gt; <jv>p</jv>.getTenant().equals(<jv>tenant</jv>))   <jc>// This request's tenant only.</jc>
 * 			.build()) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(<jv>query</jv>);
 * 		Counts <jv>counts</jv> = <jv>session</jv>.count(<jv>query</jv>);
 * 	}
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
public final class InMemoryBeanQuerySession<T> extends BeanQuerySession<T> {

	/**
	 * Builder for {@link InMemoryBeanQuerySession}.  See the class Javadoc for an example.
	 *
	 * @param <T> The row type.
	 */
	public static final class Builder<T> extends BeanQuerySession.Builder<T,Builder<T>> {

		private final InMemoryBeanQueryContext<T> context;
		private Collection<? extends T> rows;
		private Predicate<? super T> guard;

		Builder(InMemoryBeanQueryContext<T> context) {
			super(context);
			this.context = context;
		}

		/**
		 * Sets the rows to query.  Required.
		 *
		 * @param value The rows.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder<T> rows(Collection<? extends T> value) {
			rows = reqnn("value", value);
			return this;
		}

		/**
		 * Sets a per-request filter, applied together with the context guard.
		 *
		 * @param value The guard, or <jk>null</jk> for none.
		 * @return This object.
		 */
		public Builder<T> guard(Predicate<? super T> value) {
			guard = value;
			return this;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @throws IllegalStateException If no rows were set.
		 */
		@Override /* BeanQuerySession.Builder */
		@SuppressWarnings({
			"resource" // Caller takes ownership of the returned session
		})
		public InMemoryBeanQuerySession<T> build() {
			if (rows == null)
				throw isex("InMemoryBeanQuerySession requires rows.");
			return new InMemoryBeanQuerySession<>(this);
		}
	}

	private final InMemoryBeanQueryContext<T> context;
	private final List<T> scoped;

	@SuppressWarnings({
		"java:S9391" // Per-row try/catch that re-wraps guard failures; a stream lambda would obscure it.
	})
	InMemoryBeanQuerySession(Builder<T> builder) {
		super(builder);
		context = builder.context;
		var g = builder.guard;
		var l = new ArrayList<T>();
		for (T r : builder.rows) {
			try {
				if (context.inScope(r) && (g == null || g.test(r)))
					l.add(r);
			} catch (BeanQueryExecutionException | BeanQuerySyntaxException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new BeanQueryExecutionException(e, "Failed to apply guard.");
			}
		}
		scoped = Collections.unmodifiableList(l);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return The context that created this session.
	 */
	@Override /* BeanQuerySession */
	public InMemoryBeanQueryContext<T> getContext() {
		return context;
	}

	@Override /* BeanQuerySession */
	protected Page<T> doFind(ResolvedQuery query) {
		var m = matched(query);
		return page(slice(sorted(m, query), query), query, scoped::size, m::size);
	}

	@Override /* BeanQuerySession */
	protected Page<Map<String,Object>> doFindValues(ResolvedQuery query) {
		var m = matched(query);
		return page(values(slice(sorted(m, query), query), query), query, scoped::size, m::size);
	}

	@Override /* BeanQuerySession */
	protected Counts doCount(ResolvedQuery query) {
		return Counts.of(scoped.size(), matched(query).size());
	}

	@Override /* BeanQuerySession */
	protected Stream<T> doStream(ResolvedQuery query) {
		return slice(sorted(matched(query), query), query).stream();
	}

	@Override /* BeanQuerySession */
	protected Stream<Map<String,Object>> doStreamValues(ResolvedQuery query) {
		return values(slice(sorted(matched(query), query), query), query).stream();
	}

	@Override /* BeanQuerySession */
	public void close() {
		// Nothing to release: the rows belong to the caller.
	}

	private List<T> matched(ResolvedQuery query) {
		var filter = query.filter();
		if (filter instanceof Filter.And and && and.items().isEmpty())
			return scoped;  // No search: every scoped row matches, without constructing a RegexBudget for nothing.
		var budget = new RegexBudget(settings.regexTimeout);  // One deadline for the whole call.
		var l = new ArrayList<T>();
		for (var r : scoped)
			if (FilterEvaluator.matches(filter, col -> context.cell(r, col), budget))
				l.add(r);
		return l;
	}

	private List<T> sorted(List<T> rows, ResolvedQuery query) {
		Comparator<T> c = null;
		for (var k : query.sort()) {
			Comparator<T> next = (a, b) -> InMemoryMatch.compareCells(context.cell(a, k.column()), context.cell(b, k.column()), k.type());
			if (k.descending())
				next = next.reversed();
			c = c == null ? next : c.thenComparing(next);
		}
		if (c == null)
			return rows;
		var l = new ArrayList<>(rows);
		l.sort(c);
		return l;
	}

	private static <R> List<R> slice(List<R> rows, ResolvedQuery query) {
		var from = Math.min(query.position(), rows.size());
		var limit = query.limit();
		var to = limit == null ? rows.size() : (int)Math.min((long)from + limit, rows.size());
		return rows.subList(from, to);
	}

	private List<Map<String,Object>> values(List<T> rows, ResolvedQuery query) {
		var l = new ArrayList<Map<String,Object>>(rows.size());
		for (var r : rows) {
			var m = new LinkedHashMap<String,Object>();
			for (var c : query.view())
				m.put(c, context.cell(r, c));
			l.add(Collections.unmodifiableMap(m));
		}
		return l;
	}
}
