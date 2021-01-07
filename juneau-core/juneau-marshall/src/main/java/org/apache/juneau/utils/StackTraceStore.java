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
package org.apache.juneau.utils;

import static org.apache.juneau.utils.StackTraceUtils.*;
import static org.apache.juneau.SystemProperties.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * An in-memory cache of stack traces.
 *
 * <p>
 * Used for preventing duplication of stack traces in log files and replacing them with small hashes.
 */
public class StackTraceStore {

	/** Default global stack trace store. */
	public static final StackTraceStore GLOBAL = new StackTraceStore();

	/**
	 * System property name for the default stack trace timeout value.
	 * <p>
	 * Can also use a <c>JUNEAU_STACKTRACECACHETIMEOUT</c> environment variable.
	 * <p>
	 * If not specified, the default is <js>"86400000"</js> (1 day).
	 */
	public static final String SP_stackTraceCacheTimeout = "juneau.stackTraceCacheTimeout";

	private final ConcurrentHashMap<Integer,StackTraceInfo> db = new ConcurrentHashMap<>();
	private final String stopClass;
	private final long cacheTimeout;

	/**
	 * Constructor.
	 */
	public StackTraceStore() {
		this(getProperty(Integer.class, SP_stackTraceCacheTimeout, 24*60*60*1000), null);
	}

	/**
	 * Constructor.
	 *
	 * @param cacheTimeout
	 * 	The amount of time in milliseconds to cache stack trace info in this database before discarding.
	 * 	<br>If <c>-1</c>, never discard.
	 * @param stopClass
	 * 	When this class is encountered in a stack trace, stop calculating the hash.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public StackTraceStore(long cacheTimeout, Class<?> stopClass) {
		this.stopClass = stopClass == null ? "" : stopClass.getName();
		this.cacheTimeout = cacheTimeout;
	}

	/**
	 * Adds the specified throwable to this database.
	 *
	 * @param e The exception to add.
	 * @return This object (for method chaining).
	 */
	public StackTraceStore add(Throwable e) {
		find(e).increment();
		return this;
	}

	/**
	 * Retrieves the stack trace information for the specified exception.
	 *
	 * @param e The exception.
	 * @return A clone of the stack trace info, never <jk>null</jk>.
	 */
	public StackTraceInfo getStackTraceInfo(Throwable e) {
		return find(e).clone();
	}

	/**
	 * Clears out the stack trace cache.
	 */
	public void reset() {
		db.clear();
	}

	/**
	 * Returns the list of all stack traces in this database.
	 *
	 * @return The list of all stack traces in this database, cloned and sorted by count descending.
	 */
	public List<StackTraceInfo> getClonedStackTraceInfos() {
		return db.values().stream().map(x -> x.clone()).sorted().collect(Collectors.toList());
	}

	private StackTraceInfo find(Throwable e) {
		int hash = hash(e, stopClass);
		StackTraceInfo stc = db.get(hash);
		long time = System.currentTimeMillis();

		if (stc != null && stc.timeout > time) {
			return stc;
		}

		String n = e == null ? null : e.getClass().getSimpleName();
		long t = cacheTimeout == -1 ? Long.MAX_VALUE : time + cacheTimeout;
		stc = new StackTraceInfo(n, t, hash);

		db.put(hash, stc);

		return db.get(hash);
	}
}
