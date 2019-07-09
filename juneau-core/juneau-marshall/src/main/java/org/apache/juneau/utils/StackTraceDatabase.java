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

import java.util.concurrent.*;

/**
 * An in-memory cache of stack traces.
 *
 * <p>
 * Used for preventing duplication of stack traces in log files and replacing them with small hashes.
 */
public class StackTraceDatabase {

	private final ConcurrentHashMap<Integer,StackTraceInfo> DB = new ConcurrentHashMap<>();

	/**
	 * Retrieves the stack trace information for the specified exception.
	 *
	 * @param e The exception.
	 * @param timeout The timeout in milliseconds to cache the hash for this stack trace.
	 * @return The stack trace info, never <jk>null</jk>.
	 */
	public StackTraceInfo getStackTraceInfo(Throwable e, int timeout) {
		int hash = hash(e);
		StackTraceInfo stc = DB.get(hash);
		if (stc != null && stc.timeout > System.currentTimeMillis()) {
			stc.incrementAndClone();
			return stc.clone();
		}
		synchronized (DB) {
			stc = new StackTraceInfo(timeout, hash);
			DB.put(hash, stc);
			return stc.clone();
		}
	}

	private static int hash(Throwable t) {
		int i = 0;
		while (t != null) {
			for (StackTraceElement e : t.getStackTrace())
				i ^= e.hashCode();
			t = t.getCause();
		}
		return i;
	}

	/**
	 * Clears out the stack trace cache.
	 */
	public void reset() {
		DB.clear();
	}
}
