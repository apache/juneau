// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.stats;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static java.util.Comparator.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * An in-memory cache of thrown exceptions.
 *
 * <p>
 * Used for preventing duplication of stack traces in log files and replacing them with small hashes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.ExecutionStatistics">REST method execution statistics</a>
 * </ul>
 */
public class ThrownStore {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Identifies a single global store for the entire JVM. */
	public static final ThrownStore GLOBAL = new ThrownStore();

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder(BeanStore.INSTANCE);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<ThrownStore> {

		ThrownStore parent;
		Class<? extends ThrownStats> statsImplClass;
		Set<Class<?>> ignoreClasses;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(ThrownStore.class, beanStore);
		}

		@Override /* BeanBuilder */
		protected ThrownStore buildDefault() {
			return new ThrownStore(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Specifies a subclass of {@link ThrownStats} to use for individual method statistics.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder statsImplClass(Class<? extends ThrownStats> value) {
			this.statsImplClass = value;
			return this;
		}

		/**
		 * Specifies the parent store of this store.
		 *
		 * <p>
		 * Parent stores are used for aggregating statistics across multiple child stores.
		 * <br>The {@link ThrownStore#GLOBAL} store can be used for aggregating all thrown exceptions in a single JVM.
		 *
		 * @param value The parent store.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder parent(ThrownStore value) {
			this.parent = value;
			return this;
		}

		/**
		 * Specifies the list of classes to ignore when calculating stack traces.
		 *
		 * <p>
		 * Stack trace elements that are the specified class will be ignored.
		 *
		 * @param value The list of classes to ignore.
		 * @return This object.
		 */
		public Builder ignoreClasses(Class<?>...value) {
			this.ignoreClasses = set(value);
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ConcurrentHashMap<Long,ThrownStats> db = new ConcurrentHashMap<>();
	private final Optional<ThrownStore> parent;
	private final BeanStore beanStore;
	private final Class<? extends ThrownStats> statsImplClass;
	private final Set<String> ignoreClasses;

	/**
	 * Constructor.
	 */
	public ThrownStore() {
		this(create(BeanStore.INSTANCE));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public ThrownStore(Builder builder) {
		this.parent = optional(builder.parent);
		this.beanStore = builder.beanStore();

		this.statsImplClass = firstNonNull(builder.statsImplClass, parent.isPresent() ? parent.get().statsImplClass : null, null);

		Set<String> s = null;
		if (builder.ignoreClasses != null)
			s = builder.ignoreClasses.stream().map(x->x.getName()).collect(toSet());
		if (s == null && parent.isPresent())
			s = parent.get().ignoreClasses;
		if (s == null)
			s = Collections.emptySet();
		this.ignoreClasses = unmodifiable(s);
	}


	/**
	 * Adds the specified thrown exception to this database.
	 *
	 * @param e The exception to add.
	 * @return This object.
	 */
	public ThrownStats add(Throwable e) {
		ThrownStats s = find(e);
		s.increment();
		parent.ifPresent(x->x.add(e));
		return s;
	}

	/**
	 * Retrieves the stats for the specified thrown exception.
	 *
	 * @param e The exception.
	 * @return A clone of the stats, never <jk>null</jk>.
	 */
	public Optional<ThrownStats> getStats(Throwable e) {
		return getStats(hash(e));
	}

	/**
	 * Retrieves the stack trace information for the exception with the specified hash as calculated by {@link #hash(Throwable)}.
	 *
	 * @param hash The hash of the exception.
	 * @return A clone of the stack trace info, never <jk>null</jk>.
	 */
	public Optional<ThrownStats> getStats(long hash) {
		ThrownStats s = db.get(hash);
		return optional(s == null ? null : s.clone());
	}

	/**
	 * Returns the list of all stack traces in this database.
	 *
	 * @return The list of all stack traces in this database, cloned and sorted by count descending.
	 */
	public List<ThrownStats> getStats() {
		return db.values().stream().map(x -> x.clone()).sorted(comparingInt(ThrownStats::getCount).reversed()).collect(toList());
	}

	/**
	 * Clears out the stack trace cache.
	 */
	public void reset() {
		db.clear();
	}

	/**
	 * Calculates a 32-bit hash for the specified throwable based on the stack trace generated by {@link #createStackTrace(Throwable)}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 *
	 * @param t The throwable to calculate the stack trace on.
	 * @return A calculated hash.
	 */
	protected long hash(Throwable t) {
		long h = 1125899906842597L; // prime
		for (String s : createStackTrace(t)) {
			int len = s.length();
			for (int i = 0; i < len; i++)
				h = 31*h + s.charAt(i);
		}
		return h;
	}

	/**
	 * Converts the stack trace for the specified throwable into a simple list of strings.
	 *
	 * <p>
	 * The stack trace elements for the throwable are sent through {@link #normalize(StackTraceElement)} to convert
	 * them to simple strings.
	 *
	 *
	 * @param t The throwable to create the stack trace for.
	 * @return A modifiable list of strings.
	 */
	protected List<String> createStackTrace(Throwable t) {
		return alist(t.getStackTrace()).stream().filter(x -> include(x)).map(x -> normalize(x)).collect(toList());
	}

	/**
	 * Returns <jk>true</jk> if the specified stack trace element should be included in {@link #createStackTrace(Throwable)}.
	 *
	 * @param e The stack trace element.
	 * @return <jk>true</jk> if the specified stack trace element should be included in {@link #createStackTrace(Throwable)}.
	 */
	protected boolean include(StackTraceElement e) {
		return true;
	}

	/**
	 * Converts the specified stack trace element into a normalized string.
	 *
	 * <p>
	 * The default implementation simply replaces <js>"\\$.*"</js> with <js>"..."</js> which should take care of stuff like stack
	 * trace elements of lambda expressions.
	 *
	 * @param e The stack trace element to convert.
	 * @return The converted stack trace element.
	 */
	protected String normalize(StackTraceElement e) {
		if (ignoreClasses.contains(e.getClassName()))
			return "<ignored>";
		String s = e.toString();
		int i = s.indexOf('$');
		if (i == -1)
			return s;
		int j = s.indexOf('(', i);
		if (j == -1)
			return s;  // Probably can't happen.
		String s2 = s.substring(0, i), s3 = s.substring(j);
		if (ignoreClasses.contains(s2))
			return "<ignored>";
		return s2 + "..." + s3;
	}

	private ThrownStats find(final Throwable t) {

		if (t == null)
			return null;

		long hash = hash(t);

		ThrownStats stc = db.get(hash);
		if (stc == null) {
			stc = ThrownStats
				.create(beanStore)
				.type(statsImplClass)
				.throwable(t)
				.hash(hash)
				.stackTrace(createStackTrace(t))
				.causedBy(find(t.getCause()))
				.build();

			db.putIfAbsent(hash, stc);
			stc = db.get(hash);
		}

		return stc;
	}
}
