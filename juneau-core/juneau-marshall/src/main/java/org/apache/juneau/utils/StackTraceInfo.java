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

import java.util.concurrent.atomic.*;

/**
 * Represents an entry in {@link StackTraceDatabase}.
 */
public class StackTraceInfo {
	AtomicInteger count;
	long timeout;
	String hash;

	StackTraceInfo(long timeout, int hash) {
		this.count = new AtomicInteger(1);
		this.timeout = System.currentTimeMillis() + timeout;
		this.hash = Integer.toHexString(hash);
	}

	private StackTraceInfo(int count, long timeout, String hash) {
		this.count = new AtomicInteger(count);
		this.timeout = timeout;
		this.hash = hash;
	}

	@Override
	public StackTraceInfo clone() {
		return new StackTraceInfo(count.intValue(), timeout, hash);
	}

	/**
	 * Returns the number of times this stack trace was encountered.
	 *
	 * @return The number of times this stack trace was encountered.
	 */
	public int getCount() {
		return count.intValue();
	}

	/**
	 * Returns an 8-byte hash of the stack trace.
	 *
	 * @return An 8-byte hash of the stack trace.
	 */
	public String getHash() {
		return hash;
	}

	StackTraceInfo incrementAndClone() {
		return new StackTraceInfo(count.incrementAndGet(), timeout, hash);
	}
}
