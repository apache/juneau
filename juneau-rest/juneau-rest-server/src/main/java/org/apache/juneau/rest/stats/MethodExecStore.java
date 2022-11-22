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
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Method execution statistics database.
 *
 * <p>
 * Used for tracking basic call statistics on Java methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.ExecutionStatistics">REST method execution statistics</a>
 * </ul>
 */
public class MethodExecStore {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
	public static class Builder extends BeanBuilder<MethodExecStore> {

		ThrownStore thrownStore;
		Class<? extends MethodExecStats> statsImplClass;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(MethodExecStore.class, beanStore);
		}

		@Override /* BeanBuilder */
		protected MethodExecStore buildDefault() {
			return new MethodExecStore(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Specifies a subclass of {@link MethodExecStats} to use for individual method statistics.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder statsImplClass(Class<? extends MethodExecStats> value) {
			statsImplClass = value;
			return this;
		}

		/**
		 * Specifies the store to use for gathering statistics on thrown exceptions.
		 *
		 * <p>
		 * Can be used to capture thrown exception stats across multiple {@link MethodExecStore} objects.
		 *
		 * <p>
		 * If not specified, one will be created by default for the {@link MethodExecStore} object.
		 *
		 * @param value The store to use for gathering statistics on thrown exceptions.
		 * @return This object.
		 */
		public Builder thrownStore(ThrownStore value) {
			thrownStore = value;
			return this;
		}

		/**
		 * Same as {@link #thrownStore(ThrownStore)} but only sets the new value if the current value is <jk>null</jk>.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder thrownStoreOnce(ThrownStore value) {
			if (thrownStore == null)
				thrownStore = value;
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

	private final ThrownStore thrownStore;
	private final BeanStore beanStore;
	private final Class<? extends MethodExecStats> statsImplClass;
	private final ConcurrentHashMap<Method,MethodExecStats> db = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The store to use for storing thrown exception statistics.
	 */
	protected MethodExecStore(Builder builder) {
		this.beanStore = builder.beanStore();
		this.thrownStore = builder.thrownStore != null ? builder.thrownStore : beanStore.getBean(ThrownStore.class).orElseGet(ThrownStore::new);
		this.statsImplClass = builder.statsImplClass;
	}

	/**
	 * Returns the statistics for the specified method.
	 *
	 * <p>
	 * Creates a new stats object if one has not already been created.
	 *
	 * @param m The method to return the statistics for.
	 * @return The statistics for the specified method.  Never <jk>null</jk>.
	 */
	public MethodExecStats getStats(Method m) {
		MethodExecStats stats = db.get(m);
		if (stats == null) {
			stats = MethodExecStats
				.create(beanStore)
				.type(statsImplClass)
				.method(m)
				.thrownStore(ThrownStore.create(beanStore).parent(thrownStore).build())
				.build();
			db.putIfAbsent(m, stats);
			stats = db.get(m);
		}
		return stats;
	}

	/**
	 * Returns all the statistics in this store.
	 *
	 * @return All the statistics in this store.
	 */
	public Collection<MethodExecStats> getStats() {
		return db.values();
	}

	/**
	 * Returns timing information on all method executions on this class.
	 *
	 * @return A list of timing statistics ordered by average execution time descending.
	 */
	public List<MethodExecStats> getStatsByTotalTime() {
		return getStats().stream().sorted(Comparator.comparingLong(MethodExecStats::getTotalTime).reversed()).collect(toList());
	}

	/**
	 * Returns the timing information returned by {@link #getStatsByTotalTime()} in a readable format.
	 *
	 * @return A report of all method execution times ordered by .
	 */
	public String getReport() {
		StringBuilder sb = new StringBuilder()
			.append(" Method                         Runs      Running   Errors   Avg          Total     \n")
			.append("------------------------------ --------- --------- -------- ------------ -----------\n");
		getStatsByTotalTime()
			.stream()
			.sorted(Comparator.comparingDouble(MethodExecStats::getTotalTime).reversed())
			.forEach(x -> sb.append(String.format("%30s %9d %9d %9d %10dms %10dms\n", x.getMethod(), x.getRuns(), x.getRunning(), x.getErrors(), x.getAvgTime(), x.getTotalTime())));
		return sb.toString();

	}

	/**
	 * Returns the thrown exception store being used by this store.
	 *
	 * @return The thrown exception store being used by this store.
	 */
	public ThrownStore getThrownStore() {
		return thrownStore;
	}
}
