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

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * An in-memory cache of stack traces.
 *
 * <p>
 * Used for preventing duplication of stack traces in log files and replacing them with small hashes.
 */
public class ExceptionStore {

	private final ConcurrentHashMap<Integer,ExceptionStats> sdb = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer,ExceptionInfo> idb = new ConcurrentHashMap<>();
	private final ExceptionHasher hasher;
	private final long cacheTimeout;

	/**
	 * Constructor.
	 */
	public ExceptionStore() {
		this(-1, null);
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
	public ExceptionStore(long cacheTimeout, Class<?> stopClass) {
		this.hasher = new ExceptionHasher(stopClass);
		this.cacheTimeout = cacheTimeout;
	}

	/**
	 * Adds the specified throwable to this database.
	 *
	 * @param e The exception to add.
	 * @return This object (for method chaining).
	 */
	public ExceptionStore add(Throwable e) {
		find(e).increment();
		return this;
	}

	/**
	 * Retrieves the stack trace information for the specified exception.
	 *
	 * @param e The exception.
	 * @return A clone of the stack trace info, never <jk>null</jk>.
	 */
	public ExceptionStats getStackTraceInfo(Throwable e) {
		return find(e).clone();
	}

	/**
	 * Clears out the stack trace cache.
	 */
	public void reset() {
		sdb.clear();
	}

	/**
	 * Returns the list of all stack traces in this database.
	 *
	 * @return The list of all stack traces in this database, cloned and sorted by count descending.
	 */
	public List<ExceptionStats> getClonedStats() {
		return sdb.values().stream().map(x -> x.clone()).sorted().collect(Collectors.toList());
	}

	private ExceptionStats find(Throwable e) {

		if (e == null)
			return null;

		int hash = hasher.hash(e);

		ExceptionStats stc = sdb.get(hash);
		if (stc != null && ! stc.isExpired())
			return stc;

		stc = ExceptionStats
			.create()
			.hash(Integer.toHexString(hash))
			.timeout(cacheTimeout == -1 ? Long.MAX_VALUE : System.currentTimeMillis() + cacheTimeout)
			.message(e.getMessage())
			.exceptionClass(e.getClass().getName())
			.stackTrace(hasher.getStackTrace(e))
			.causedBy(e.getCause() == null ? null : findInfo(e.getCause()));

		sdb.put(hash, stc);

		return sdb.get(hash);
	}

	private ExceptionInfo findInfo(Throwable e) {

		int hash = hasher.hash(e);

		ExceptionInfo ei = sdb.get(hash);
		if (ei == null) {
			ei = ExceptionInfo
				.create()
				.hash(Integer.toHexString(hash))
				.message(e.getMessage())
				.exceptionClass(e.getClass().getName())
				.stackTrace(hasher.getStackTrace(e))
				.causedBy(e.getCause() == null ? null : findInfo(e.getCause()));
			idb.put(hash, ei);
		}

		return ei;
	}
}
