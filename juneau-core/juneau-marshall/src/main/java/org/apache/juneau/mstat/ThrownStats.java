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

import static org.apache.juneau.internal.CollectionUtils.*;
import static java.util.Optional.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.*;

/**
 * Represents an entry in {@link ThrownStore}.
 */
public class ThrownStats implements Cloneable {

	private final long guid;
	private final long hash;
	private final Class<?> thrownClass;
	private final String firstMessage;
	private final List<String> stackTrace;
	private final Optional<ThrownStats> causedBy;

	private final AtomicInteger count;
	private final AtomicLong firstOccurrence, lastOccurrence;

	/**
	 * Creator.
	 *
	 * @return A new builder for this object.
	 */
	public static ThrownStatsBuilder create() {
		return new ThrownStatsBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public ThrownStats(ThrownStatsBuilder builder) {
		this.guid = new Random().nextLong();
		this.thrownClass = builder.throwable.getClass();
		this.firstMessage = builder.throwable.getMessage();
		this.stackTrace = listBuilder(builder.stackTrace).copy().unmodifiable().build();
		this.causedBy = ofNullable(builder.causedBy);
		this.hash = builder.hash;
		this.count = new AtomicInteger(0);
		long ct = System.currentTimeMillis();
		this.firstOccurrence = new AtomicLong(ct);
		this.lastOccurrence = new AtomicLong(ct);
	}

	/**
	 * Copy constructor.
	 */
	private ThrownStats(ThrownStats x) {
		this.guid = x.guid;
		this.thrownClass = x.thrownClass;
		this.firstMessage = x.firstMessage;
		this.stackTrace = listBuilder(x.stackTrace).copy().unmodifiable().build();
		this.causedBy = Optional.ofNullable(x.causedBy.isPresent() ? x.causedBy.get().clone() : null);
		this.hash = x.hash;
		this.count = new AtomicInteger(x.count.get());
		this.firstOccurrence = new AtomicLong(x.firstOccurrence.get());
		this.lastOccurrence = new AtomicLong(x.lastOccurrence.get());
	}

	/**
	 * Returns a globally unique ID for this object.
	 *
	 * <p>
	 * A random long generated during the creation of this object.
	 * Allows this object to be differentiated from other similar objects in multi-node environments so that
	 * statistics can be reliably stored in a centralized location.
	 *
	 * @return The globally unique ID for this object.
	 */
	public long getGuid() {
		return guid;
	}

	/**
	 * Returns a hash of this exception that can typically be used to uniquely identify it.
	 *
	 * @return A hash of this exception.
	 */
	public long getHash() {
		return hash;
	}

	/**
	 * Returns the exception class.
	 *
	 * @return The exception class.
	 */
	public Class<?> getThrownClass() {
		return thrownClass;
	}

	/**
	 * Returns the number of times this exception occurred at a specific location in code.
	 *
	 * @return The number of times this exception occurred at a specific location in code.
	 */
	public int getCount() {
		return count.intValue();
	}

	/**
	 * Returns the UTC time of the first occurrence of this exception at a specific location in code.
	 *
	 * @return The UTC time of the first occurrence of this exception at a specific location in code.
	 */
	public long getFirstOccurrence() {
		return firstOccurrence.longValue();
	}

	/**
	 * Returns the UTC time of the last occurrence of this exception at a specific location in code.
	 *
	 * @return The UTC time of the last occurrence of this exception at a specific location in code.
	 */
	public long getLastOccurrence() {
		return lastOccurrence.longValue();
	}

	/**
	 * Returns the message of the first exception at a specific location in code.
	 *
	 * @return The message of the first exception at a specific location in code.
	 */
	public String getFirstMessage() {
		return firstMessage;
	}

	/**
	 * Returns the stack trace of the first exception at a specific location in code.
	 *
	 * @return The stack trace of the first exception at a specific location in code.
	 */
	public List<String> getStackTrace() {
		return stackTrace;
	}

	/**
	 * Returns the stats on the caused-by exception.
	 *
	 * @return The stats on the caused-by exception, never <jk>null</jk>.
	 */
	public Optional<ThrownStats> getCausedBy() {
		return causedBy;
	}

	/**
	 * Increments the occurrence count of this exception.
	 *
	 * @return This object (for method chaining).
	 */
	public ThrownStats increment() {
		count.incrementAndGet();
		lastOccurrence.set(System.currentTimeMillis());
		causedBy.ifPresent(ThrownStats::increment);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT.toString(this);
	}

	@Override /* Object */
	public ThrownStats clone() {
		return new ThrownStats(this);
	}
}
