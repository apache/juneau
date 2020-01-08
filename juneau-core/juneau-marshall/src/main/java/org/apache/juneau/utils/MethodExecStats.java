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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.marshall.*;

/**
 * Basic timing information.
 *
 * Keeps track of number of starts/finishes on tasks and keeps an average run time.
 */
@Bean(bpi="method,runs,running,errors,minTime,maxTime,avgTime,totalTime,exceptions")
public class MethodExecStats implements Comparable<MethodExecStats> {

	private String method;
	private WeightedAverage avgTime = new WeightedAverage();
	private volatile int minTime = -1, maxTime;

	private AtomicInteger
		starts = new AtomicInteger(),
		finishes = new AtomicInteger(),
		errors = new AtomicInteger();

	private AtomicLong
		totalTime = new AtomicLong();

	private StackTraceDatabase stackTraceDb;

	/**
	 * Constructor.
	 *
	 * @param method Arbitrary label.  Should be kept to less than 50 characters.
	 * @param stackTraceStopClass Don't calculate stack traces when this class is encountered.
	 */
	public MethodExecStats(Method method, Class<?> stackTraceStopClass) {
		this.method = method.getDeclaringClass().getSimpleName() + "." + method.getName();
		this.stackTraceDb = new StackTraceDatabase(-1, stackTraceStopClass);
	}

	/**
	 * Constructor.
	 *
	 * @param method Arbitrary label.  Should be kept to less than 50 characters.
	 */
	public MethodExecStats(Method method) {
		this(method, MethodInvoker.class);
	}

	/**
	 * Call when task is started.
	 */
	public void started() {
		starts.incrementAndGet();
	}

	/**
	 * Call when task is finished.
	 * @param nanoTime The execution time of the task in nanoseconds.
	 */
	public void finished(long nanoTime) {
		finishes.incrementAndGet();
		int milliTime = (int)(nanoTime/1_000_000);
		totalTime.addAndGet(nanoTime);
		avgTime.add(1, nanoTime);
		minTime = minTime == -1 ? milliTime : Math.min(minTime, milliTime);
		maxTime = Math.max(maxTime, milliTime);
	}

	/**
	 * Call when an error occurs.
	 * @param e The exception thrown.  Can be <jk>null</jk>.
	 */
	public void error(Throwable e) {
		errors.incrementAndGet();
		stackTraceDb.add(e);
	}

	/**
	 * Returns the method name of these stats.
	 *
	 * @return The method name of these stats.
	 */
	public String getMethod() {
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
		return (int)avgTime.getValue() / 1_000_000;
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
	public List<StackTraceInfo> getExceptions() {
		return stackTraceDb.getClonedStackTraceInfos();
	}

	@Override /* Object */
	public String toString() {
		return SimpleJson.DEFAULT.toString(this);
	}

	@Override /* Comparable */
	public int compareTo(MethodExecStats o) {
		return Long.compare(o.getTotalTime(), getTotalTime());
	}
}