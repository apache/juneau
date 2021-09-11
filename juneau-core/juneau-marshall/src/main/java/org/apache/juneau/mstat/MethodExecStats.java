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
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Method execution statistics.
 *
 * Keeps track of number of starts/finishes on tasks and keeps an average run time.
 */
public class MethodExecStats {

	private final long guid;
	private final Method method;
	private final ThrownStore thrownStore;

	private volatile int minTime = -1, maxTime;

	private AtomicInteger
		starts = new AtomicInteger(),
		finishes = new AtomicInteger(),
		errors = new AtomicInteger();

	private AtomicLong
		totalTime = new AtomicLong();

	/**
	 * Static creator.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MethodExecStats(Builder builder) {
		this.guid = new Random().nextLong();
		this.method = builder.method;
		this.thrownStore = ofNullable(builder.thrownStore).orElseGet(ThrownStore::new);
	}

	/**
	 * Builder for this object.
	 */
	@FluentSetters
	public static class Builder {

		Method method;
		ThrownStore thrownStore;

		Class<? extends MethodExecStats> implClass;
		BeanStore beanStore;

		/**
		 * Create a new {@link MethodExecStats} using this builder.
		 *
		 * @return A new {@link ThrownStats}
		 */
		public MethodExecStats build() {
			try {
				Class<? extends MethodExecStats> ic = isConcrete(implClass) ? implClass : MethodExecStats.class;
				return BeanStore.of(beanStore).addBeans(Builder.class, this).createBean(ic);
			} catch (ExecutableException e) {
				throw runtimeException(e);
			}
		}

		/**
		 * Specifies the bean store to use for instantiating the {@link MethodExecStats} object.
		 *
		 * <p>
		 * Can be used to instantiate {@link MethodExecStats} implementations with injected constructor argument beans.
		 *
		 * @param value The new value for this setting.
		 * @return  This object (for method chaining).
		 */
		@FluentSetter
		public Builder beanStore(BeanStore value) {
			beanStore = value;
			return this;
		}

		/**
		 * Specifies a subclass of {@link MethodExecStats} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return  This object (for method chaining).
		 */
		@FluentSetter
		public Builder implClass(Class<? extends MethodExecStats> value) {
			implClass = value;
			return this;
		}

		/**
		 * Specifies the Java method.
		 *
		 * @param value The new value for this setting.
		 * @return  This object (for method chaining).
		 */
		@FluentSetter
		public Builder method(Method value) {
			method = value;
			return this;
		}

		/**
		 * Specifies the thrown store for tracking exceptions.
		 *
		 * @param value The new value for this setting.
		 * @return  This object (for method chaining).
		 */
		@FluentSetter
		public Builder thrownStore(ThrownStore value) {
			thrownStore = value;
			return this;
		}
	}


	/**
	 * Call when task is started.
	 *
	 * @return This object (for method chaining).
	 */
	public MethodExecStats started() {
		starts.incrementAndGet();
		return this;
	}

	/**
	 * Call when task is finished.
	 *
	 * @param nanoTime The execution time of the task in nanoseconds.
	 * @return This object (for method chaining).
	 */
	public MethodExecStats finished(long nanoTime) {
		finishes.incrementAndGet();
		int milliTime = (int)(nanoTime/1_000_000);
		totalTime.addAndGet(nanoTime);
		minTime = minTime == -1 ? milliTime : Math.min(minTime, milliTime);
		maxTime = Math.max(maxTime, milliTime);
		return this;
	}

	/**
	 * Call when an error occurs.
	 *
	 * @param e The exception thrown.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public MethodExecStats error(Throwable e) {
		errors.incrementAndGet();
		thrownStore.add(e);
		return this;
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
	 * Returns the method name of these stats.
	 *
	 * @return The method name of these stats.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Returns the number of times the {@link #started()} method was called.
	 *
	 * @return The number of times the {@link #started()} method was called.
	 */
	public int getRuns() {
		return starts.get();
	}

	/**
	 * Returns the number currently running method invocations.
	 *
	 * @return The number of currently running method invocations.
	 */
	public int getRunning() {
		return starts.get() - finishes.get();
	}

	/**
	 * Returns the number of times the {@link #error(Throwable)} method was called.
	 *
	 * @return The number of times the {@link #error(Throwable)} method was called.
	 */
	public int getErrors() {
		return errors.get();
	}

	/**
	 * Returns the max execution time.
	 *
	 * @return The average execution time in milliseconds.
	 */
	public int getMinTime() {
		return minTime == -1 ? 0 : minTime;
	}

	/**
	 * Returns the max execution time.
	 *
	 * @return The average execution time in milliseconds.
	 */
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * Returns the average execution time.
	 *
	 * @return The average execution time in milliseconds.
	 */
	public int getAvgTime() {
		int runs = finishes.get();
		return runs == 0 ? 0 : (int)(getTotalTime() / runs);
	}

	/**
	 * Returns the total execution time.
	 *
	 * @return The total execution time in milliseconds.
	 */
	public long getTotalTime() {
		return totalTime.get() / 1_000_000;
	}

	/**
	 * Returns information on all stack traces of all exceptions encountered.
	 *
	 * @return Information on all stack traces of all exceptions encountered.
	 */
	public ThrownStore getThrownStore() {
		return thrownStore;
	}

	@Override /* Object */
	public String toString() {
		return json(this);
	}
}