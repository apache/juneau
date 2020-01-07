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

import org.apache.juneau.annotation.*;

/**
 * Represents an entry in {@link StackTraceDatabase}.
 */
@Bean(bpi="exception,hash,count")
public class StackTraceInfo implements Comparable<StackTraceInfo> {
	final AtomicInteger count;
	final long timeout;
	final String hash;
	final String exception;

	/**
	 * Constructor.
	 *
	 * @param exception The simple name of the exception class.  Can be <jk>null</jk>.
	 * @param timeout The time in UTC milliseconds at which point this stack trace info should be discarded from caches.
	 * @param hash The hash id of this stack trace.
	 */
	public StackTraceInfo(String exception, long timeout, int hash) {
		this.count = new AtomicInteger(0);
		this.timeout = timeout;
		this.hash = Integer.toHexString(hash);
		this.exception = exception;
	}

	private StackTraceInfo(String exception, int count, long timeout, String hash) {
		this.count = new AtomicInteger(count);
		this.timeout = timeout;
		this.hash = hash;
		this.exception = exception;
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

	/**
	 * Returns the simple class name of the exception.
	 *
	 * @return The simple class name of the exception, or <jk>null</jk> if not specified.
	 */
	public String getException() {
		return exception;
	}

	/**
	 * Increments the occurrence count of this exception.
	 *
	 * @return This object (for method chaining).
	 */
	public StackTraceInfo increment() {
		count.incrementAndGet();
		return this;
	}

	@Override /* Comparable */
	public int compareTo(StackTraceInfo o) {
		return Integer.compare(o.getCount(), getCount());
	}

	@Override /* Object */
	public StackTraceInfo clone() {
		return new StackTraceInfo(exception, count.intValue(), timeout, hash);
	}
}
