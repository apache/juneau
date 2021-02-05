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

import org.apache.juneau.cp.*;

/**
 * Method execution statistics database.
 * 
 * <p>
 * Used for tracking basic call statistics on Java methods.
 */
public class MethodExecStore {

	private final ThrownStore thrownStore;
	private final BeanFactory beanFactory;
	private final Class<? extends MethodExecStats> statsImplClass;
	private final ConcurrentHashMap<Method,MethodExecStats> db = new ConcurrentHashMap<>();

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static MethodExecStoreBuilder create() {
		return new MethodExecStoreBuilder();
	}

	/**
	 * Constructor.
	 */
	public MethodExecStore() {
		this(create());
	}

	/**
	 * Constructor.
	 *
	 * @param builder The store to use for storing thrown exception statistics.
	 */
	public MethodExecStore(MethodExecStoreBuilder builder) {
		this.beanFactory = ofNullable(builder.beanFactory).orElseGet(BeanFactory::new);
		this.thrownStore = ofNullable(builder.thrownStore).orElse(beanFactory.getBean(ThrownStore.class).orElseGet(ThrownStore::new));
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
				.beanFactory(beanFactory)
				.implClass(statsImplClass)
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
