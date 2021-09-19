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
package org.apache.juneau.mstat;

import static java.util.Optional.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;

/**
 * Method execution statistics database.
 *
 * <p>
 * Used for tracking basic call statistics on Java methods.
 */
public class MethodExecStore {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends BeanBuilder<MethodExecStore> {

		ThrownStore thrownStore;
		Class<? extends MethodExecStats> statsImplClass;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(MethodExecStore.class);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			thrownStore = copyFrom.thrownStore;
			statsImplClass = copyFrom.statsImplClass;
		}

		@Override /* BeanBuilder */
		protected MethodExecStore buildDefault() {
			return new MethodExecStore(this);
		}

		@Override /* BeanBuilder */
		public Builder copy() {
			return new Builder(this);
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

		@Override /* BeanBuilder */
		public Builder type(Class<? extends MethodExecStore> value) {
			super.type(value);
			return this;
		}

		@Override /* BeanBuilder */
		public Builder impl(MethodExecStore value) {
			super.impl(value);
			return this;
		}

		@Override /* BeanBuilder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* BeanBuilder */
		public Builder beanStore(BeanStore value) {
			super.beanStore(value);
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
		this.beanStore = builder.beanStore().orElseGet(()->BeanStore.create().build());
		this.thrownStore = ofNullable(builder.thrownStore).orElse(beanStore.getBean(ThrownStore.class).orElseGet(ThrownStore::new));
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
				.create()
				.beanStore(beanStore)
				.type(statsImplClass)
				.method(m)
				.thrownStore(ThrownStore.create().parent(thrownStore).build())
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
	 * Returns the thrown exception store being used by this store.
	 *
	 * @return The thrown exception store being used by this store.
	 */
	public ThrownStore getThrownStore() {
		return thrownStore;
	}
}
