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

import org.apache.juneau.commons.bean.*;

/**
 * The in-memory {@link BeanQueryContext}: runs {@link BeanQuery}s against an in-JVM collection of beans or maps.
 *
 * <p>
 * {@link #create(Class)} declares one column per readable Juneau bean property: getters ({@code getX()}, or
 * {@code isX()} for booleans), public fields, and record components, named and filtered by the same rules serializers
 * use ({@link BeanIgnore @BeanIgnore}, {@link BeanProp @BeanProp}, the property namer).  The value type is inferred
 * from the property type.  Use {@link #create(Class, BeanConfigContext)} or {@link #create(BeanMeta)} to apply a
 * specific bean configuration.  Nothing else is queryable: {@code class}, {@code toString} and other methods are not
 * columns.  Narrow the list with {@link Builder#exclude(String...) exclude} or
 * {@link Builder#columns(String...) columns}, add derived columns with {@link Builder#column(String, SearchType)
 * column} plus {@link Builder#accessor(String, Function) accessor}, and scope every session with a server-only
 * {@link Builder#guard(Predicate) guard}.  {@link #create()} is for {@code Map} rows: declare each column, and it is
 * read by key.
 *
 * <p>
 * The context holds no rows.  Open a session over a specific collection with {@link #getSession(Collection)} or
 * {@link #createSession()}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	InMemoryBeanQueryContext&lt;Person&gt; <jv>context</jv> = InMemoryBeanQueryContext
 * 		.<jsm>create</jsm>(Person.<jk>class</jk>)
 * 		.exclude(<js>"ssn"</js>)
 * 		.column(<js>"initial"</js>, SearchType.<jsf>TEXT</jsf>)
 * 		.accessor(<js>"initial"</js>, <jv>p</jv> -&gt; <jv>p</jv>.getName().substring(0, 1))
 * 		.guard(Person::isActive)
 * 		.build();
 *
 * 	<jk>try</jk> (InMemoryBeanQuerySession&lt;Person&gt; <jv>session</jv> = <jv>context</jv>.getSession(<jv>people</jv>)) {
 * 		Page&lt;Person&gt; <jv>page</jv> = <jv>session</jv>.find(BeanQuery.<jsm>create</jsm>().setSearch(<js>"age=$gt(21)"</js>).setSort(<js>"name"</js>));
 * 	}
 *
 * 	<jc>// The bean rules of a specific serializer configuration.</jc>
 * 	InMemoryBeanQueryContext&lt;Person&gt; <jv>dashed</jv> = InMemoryBeanQueryContext
 * 		.<jsm>create</jsm>(Person.<jk>class</jk>, BeanConfigContext.<jsm>create</jsm>().propertyNamer(PropertyNamerDLC.<jsf>INSTANCE</jsf>).build())
 * 		.build();
 *
 * 	<jc>// Map rows: declare the columns.</jc>
 * 	InMemoryBeanQueryContext&lt;Map&lt;String,Object&gt;&gt; <jv>maps</jv> = InMemoryBeanQueryContext
 * 		.&lt;Map&lt;String,Object&gt;&gt;<jsm>create</jsm>()
 * 		.column(<js>"name"</js>, SearchType.<jsf>TEXT</jsf>)
 * 		.column(<js>"age"</js>, SearchType.<jsf>NUMERIC</jsf>)
 * 		.build();
 * </p>
 *
 * @param <T> The row (bean) type.
 * @since 10.0.0
 */
public final class InMemoryBeanQueryContext<T> extends BeanQueryContext<T> {

	/**
	 * Builder for {@link InMemoryBeanQueryContext}.  See the class Javadoc for an example.
	 *
	 * @param <T> The row type.
	 */
	public static final class Builder<T> extends BeanQueryContext.Builder<T,Builder<T>> {

		private final Class<T> beanClass;
		private final QueryModel<T> model;  // Null for Map rows.
		private final Map<String,Function<? super T,?>> accessors = new LinkedHashMap<>();
		private Predicate<? super T> guard;

		Builder(QueryModel<T> model) {
			this.model = model;
			beanClass = model == null ? null : model.getBeanClass();
			if (model != null)
				model.getReadableProperties().forEach((name, p) -> settings.columns.put(name, p.getSearchType()));
		}

		Builder(InMemoryBeanQueryContext<T> copyFrom) {
			super(copyFrom);
			beanClass = copyFrom.beanClass;
			model = copyFrom.model;
			accessors.putAll(copyFrom.accessors);
			guard = copyFrom.guard;
		}

		/**
		 * Sets how a declared column's value is read, replacing the bean property (or map key).
		 *
		 * @param column The column name.  Must be declared.
		 * @param value The accessor.  Must not be <jk>null</jk>.  Exceptions it throws become
		 * 	{@link BeanQueryExecutionException}s.
		 * @return This object.
		 */
		public Builder<T> accessor(String column, Function<? super T,?> value) {
			accessors.put(declared(column), reqnn("value", value));
			return this;
		}

		/**
		 * Sets a server-only filter applied to every session before the search.  Rows it rejects are not counted in
		 * {@link Page#total()}.
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
		 * @throws IllegalArgumentException If a bean column has neither a readable property nor an accessor.
		 */
		@Override /* BeanQueryContext.Builder */
		public InMemoryBeanQueryContext<T> build() {
			return new InMemoryBeanQueryContext<>(this);
		}
	}

	/**
	 * Returns a builder whose columns are the bean's readable properties, per {@link BeanConfigContext#DEFAULT}.
	 *
	 * @param <T> The bean type.
	 * @param beanClass The bean class.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <T> Builder<T> create(Class<T> beanClass) {
		return new Builder<>(QueryModel.of(reqnn("beanClass", beanClass)));
	}

	/**
	 * Returns a builder whose columns are the bean's readable properties under the given bean configuration.
	 *
	 * <p>
	 * Pass the configuration your serializer uses so column names match the property names it emits.
	 *
	 * @param <T> The bean type.
	 * @param beanClass The bean class.  Must not be <jk>null</jk>.
	 * @param config The bean configuration.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <T> Builder<T> create(Class<T> beanClass, BeanConfigContext config) {
		return new Builder<>(QueryModel.of(reqnn("beanClass", beanClass), reqnn("config", config)));
	}

	/**
	 * Returns a builder whose columns are the readable properties of existing bean metadata.
	 *
	 * @param <T> The bean type.
	 * @param beanMeta The bean metadata.  Must not be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static <T> Builder<T> create(BeanMeta<T> beanMeta) {
		return new Builder<>(QueryModel.of(reqnn("beanMeta", beanMeta)));
	}

	/**
	 * Returns a builder for {@code Map} rows.  It has no columns until you declare them.
	 *
	 * @param <T> The row type, usually {@code Map<String,Object>}.
	 * @return A new builder.
	 */
	public static <T> Builder<T> create() {
		return new Builder<>((QueryModel<T>)null);
	}

	private final Class<T> beanClass;
	private final QueryModel<T> model;
	private final Map<String,Function<? super T,?>> accessors;
	private final Map<String,Function<? super T,?>> readers;
	private final Predicate<? super T> guard;

	InMemoryBeanQueryContext(Builder<T> builder) {
		super(builder);
		beanClass = builder.beanClass;
		model = builder.model;
		accessors = Collections.unmodifiableMap(new LinkedHashMap<>(builder.accessors));
		guard = builder.guard;
		var m = new LinkedHashMap<String,Function<? super T,?>>();
		for (var c : settings.columns.keySet())
			m.put(c, reader(c));
		readers = Collections.unmodifiableMap(m);
	}

	private Function<? super T,?> reader(String column) {
		var a = accessors.get(column);
		if (a != null)
			return a;
		var p = model == null ? null : model.getReadableProperty(column);
		if (p != null)
			return p::read;
		if (beanClass == null)
			return row -> ((Map<?,?>)row).get(column);
		throw iaex("Column '%s' is not a readable bean property of %s; declare an accessor.", column, beanClass.getName());
	}

	/**
	 * Reads one cell.
	 *
	 * @param row The row.
	 * @param column A declared column.
	 * @return The value.  Can be <jk>null</jk>.
	 * @throws BeanQueryExecutionException If the accessor or property read fails.
	 */
	Object cell(T row, String column) {
		try {
			return readers.get(column).apply(row);
		} catch (BeanQueryExecutionException | BeanQuerySyntaxException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new BeanQueryExecutionException(e, "Failed to read column '%s'.", column);
		}
	}

	/**
	 * Tests the context guard.
	 *
	 * @param row The row.
	 * @return <jk>true</jk> if there is no guard or the guard accepts the row.
	 */
	boolean inScope(T row) {
		return guard == null || guard.test(row);
	}

	@Override /* BeanQueryContext */
	public Builder<T> copy() {
		return new Builder<>(this);
	}

	@Override /* BeanQueryContext */
	public InMemoryBeanQuerySession.Builder<T> createSession() {
		return new InMemoryBeanQuerySession.Builder<>(this);
	}

	/**
	 * Opens a session over a collection with this context's settings as-is.
	 *
	 * @param rows The rows.  Must not be <jk>null</jk>.
	 * @return A new session.
	 */
	@SuppressWarnings({
		"resource" // Caller takes ownership of the returned session
	})
	public InMemoryBeanQuerySession<T> getSession(Collection<? extends T> rows) {
		return createSession().rows(rows).build();
	}
}
