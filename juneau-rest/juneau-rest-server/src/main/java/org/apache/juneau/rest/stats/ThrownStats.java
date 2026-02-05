/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.stats;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.cp.*;

/**
 * Represents an entry in {@link ThrownStore}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ExecutionStatistics">REST method execution statistics</a>
 * </ul>
 */
public class ThrownStats implements Cloneable {

	// Property name constants
	private static final String PROP_causedBy = "causedBy";
	private static final String PROP_count = "count";
	private static final String PROP_firstMessage = "firstMessage";
	private static final String PROP_firstOccurrence = "firstOccurrence";
	private static final String PROP_guid = "guid";
	private static final String PROP_hash = "hash";
	private static final String PROP_lastOccurrence = "lastOccurrence";
	private static final String PROP_stackTrace = "stackTrace";
	private static final String PROP_thrownClass = "thrownClass";

	/**
	 * Builder class.
	 */
	public static class Builder {

		final BasicBeanStore beanStore;
		Throwable throwable;
		long hash;
		List<String> stackTrace;
		ThrownStats causedBy;

		BeanCreator<ThrownStats> creator;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BasicBeanStore beanStore) {
			this.beanStore = beanStore;
			this.creator = BeanCreator.of(ThrownStats.class, beanStore).builder(Builder.class, this);
		}

		/**
		 * Create a new {@link ThrownStats} using this builder.
		 *
		 * @return A new {@link ThrownStats}
		 */
		public ThrownStats build() {
			return creator.run();
		}

		/**
		 * Specifies the caused-by exception.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder causedBy(ThrownStats value) {
			causedBy = value;
			return this;
		}

		/**
		 * Specifies the calculated hash.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder hash(long value) {
			hash = value;
			return this;
		}

		/**
		 * Specifies the normalized stacktrace.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder stackTrace(List<String> value) {
			stackTrace = value;
			return this;
		}

		/**
		 * Specifies the thrown exception.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder throwable(Throwable value) {
			throwable = value;
			return this;
		}

		/**
		 * Specifies a subclass of {@link ThrownStats} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public Builder type(Class<? extends ThrownStats> value) {
			creator.type(value == null ? ThrownStats.class : value);
			return this;
		}
	}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BasicBeanStore beanStore) {
		return new Builder(beanStore);
	}

	private final long guid;
	private final long hash;
	private final Class<?> thrownClass;
	private final String firstMessage;
	private final List<String> stackTrace;
	private final Optional<ThrownStats> causedBy;

	private final AtomicInteger count;
	private final AtomicLong firstOccurrence;
	private final AtomicLong lastOccurrence;

	/**
	 * Copy constructor.
	 */
	private ThrownStats(ThrownStats x) {
		this.guid = x.guid;
		this.thrownClass = x.thrownClass;
		this.firstMessage = x.firstMessage;
		this.stackTrace = u(copyOf(x.stackTrace));
		this.causedBy = opt(x.causedBy.isPresent() ? x.causedBy.get().clone() : null);
		this.hash = x.hash;
		this.count = new AtomicInteger(x.count.get());
		this.firstOccurrence = new AtomicLong(x.firstOccurrence.get());
		this.lastOccurrence = new AtomicLong(x.lastOccurrence.get());
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected ThrownStats(Builder builder) {
		this.guid = new Random().nextLong();
		this.thrownClass = builder.throwable.getClass();
		this.firstMessage = builder.throwable.getMessage();
		this.stackTrace = u(copyOf(builder.stackTrace));
		this.causedBy = opt(builder.causedBy);
		this.hash = builder.hash;
		this.count = new AtomicInteger(0);
		long ct = System.currentTimeMillis();
		this.firstOccurrence = new AtomicLong(ct);
		this.lastOccurrence = new AtomicLong(ct);
	}

	@Override /* Overridden from Object */
	public ThrownStats clone() {
		try {
			super.clone(); // Satisfy SonarQube requirement
			// Since all fields are final, we cannot modify them after super.clone().
			// Use copy constructor to properly deep-copy mutable fields (AtomicInteger, AtomicLong, List, Optional).
			return new ThrownStats(this);
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Returns the stats on the caused-by exception.
	 *
	 * @return The stats on the caused-by exception, never <jk>null</jk>.
	 */
	public Optional<ThrownStats> getCausedBy() { return causedBy; }

	/**
	 * Returns the number of times this exception occurred at a specific location in code.
	 *
	 * @return The number of times this exception occurred at a specific location in code.
	 */
	public int getCount() { return count.intValue(); }

	/**
	 * Returns the message of the first exception at a specific location in code.
	 *
	 * @return The message of the first exception at a specific location in code.
	 */
	public String getFirstMessage() { return firstMessage; }

	/**
	 * Returns the UTC time of the first occurrence of this exception at a specific location in code.
	 *
	 * @return The UTC time of the first occurrence of this exception at a specific location in code.
	 */
	public long getFirstOccurrence() { return firstOccurrence.longValue(); }

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
	public long getGuid() { return guid; }

	/**
	 * Returns a hash of this exception that can typically be used to uniquely identify it.
	 *
	 * @return A hash of this exception.
	 */
	public long getHash() { return hash; }

	/**
	 * Returns the UTC time of the last occurrence of this exception at a specific location in code.
	 *
	 * @return The UTC time of the last occurrence of this exception at a specific location in code.
	 */
	public long getLastOccurrence() { return lastOccurrence.longValue(); }

	/**
	 * Returns the stack trace of the first exception at a specific location in code.
	 *
	 * @return The stack trace of the first exception at a specific location in code.
	 */
	public List<String> getStackTrace() { return stackTrace; }

	/**
	 * Returns the exception class.
	 *
	 * @return The exception class.
	 */
	public Class<?> getThrownClass() { return thrownClass; }

	/**
	 * Increments the occurrence count of this exception.
	 *
	 * @return This object.
	 */
	public ThrownStats increment() {
		count.incrementAndGet();
		lastOccurrence.set(System.currentTimeMillis());
		causedBy.ifPresent(ThrownStats::increment);
		return this;
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a(PROP_causedBy, causedBy.orElse(null))
			.a(PROP_count, getCount())
			.a(PROP_firstMessage, firstMessage)
			.a(PROP_firstOccurrence, getFirstOccurrence())
			.a(PROP_guid, guid)
			.a(PROP_hash, hash)
			.a(PROP_lastOccurrence, getLastOccurrence())
			.a(PROP_stackTrace, stackTrace)
			.a(PROP_thrownClass, thrownClass);
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}
}