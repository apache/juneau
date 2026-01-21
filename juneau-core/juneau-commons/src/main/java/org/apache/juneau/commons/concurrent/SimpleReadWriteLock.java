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
package org.apache.juneau.commons.concurrent;

import java.util.concurrent.locks.*;

/**
 * An extension of {@link ReentrantReadWriteLock} with convenience methods for creating
 * auto-closeable locks that work seamlessly with try-with-resources statements.
 *
 * <p>
 * This class provides a simpler API than the standard {@link ReentrantReadWriteLock} by wrapping
 * read and write locks in {@link SimpleLock} instances that implement {@link AutoCloseable}.
 * This allows locks to be automatically released when used in try-with-resources blocks, reducing
 * the risk of forgetting to unlock and preventing deadlocks.
 *
 * <p>
 * The returned {@link SimpleLock} instances automatically acquire the lock when created and release
 * it when {@link SimpleLock#close()} is called (either explicitly or automatically via try-with-resources).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Using read lock with try-with-resources</jc>
 * 	<jk>var</jk> <jv>lock</jv> = <jk>new</jk> SimpleReadWriteLock();
 * 	<jk>try</jk> (<jk>var</jk> <jv>readLock</jv> = <jv>lock</jv>.read()) {
 * 		<jc>// Read operations here - lock is automatically held</jc>
 * 		<jv>sharedResource</jv>.read();
 * 	}
 * 	<jc>// Lock is automatically released when exiting try block</jc>
 *
 * 	<jc>// Using write lock with try-with-resources</jc>
 * 	<jk>try</jk> (<jk>var</jk> <jv>writeLock</jv> = <jv>lock</jv>.write()) {
 * 		<jc>// Write operations here - lock is automatically held</jc>
 * 		<jv>sharedResource</jv>.write(<js>"new value"</js>);
 * 	}
 * 	<jc>// Lock is automatically released when exiting try block</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class inherits all thread-safety guarantees from {@link ReentrantReadWriteLock}.
 * Multiple threads can hold read locks simultaneously, but write locks are exclusive.
 * Read and write locks cannot be held simultaneously by different threads.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>The returned {@link SimpleLock} instances are not thread-safe themselves and should
 * 		not be shared between threads. Each thread should call {@link #read()} or {@link #write()} separately.
 * 	<li class='note'>Each call to {@link #read()} or {@link #write()} returns a new {@link SimpleLock} instance.
 * 	<li class='note'>The lock is automatically acquired when {@link SimpleLock} is created and released when
 * 		{@link SimpleLock#close()} is called.
 * 	<li class='note'>This class supports reentrant locking - the same thread can acquire multiple read locks
 * 		or upgrade from read to write lock (if no other threads hold read locks).
 * 	<li class='note'>For fair ordering, use {@link #SimpleReadWriteLock(boolean)} with <jk>true</jk>.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SimpleLock} - The auto-closeable lock wrapper
 * 	<li class='jc'>{@link ReentrantReadWriteLock} - The underlying lock implementation
 * </ul>
 */
public class SimpleReadWriteLock extends ReentrantReadWriteLock {
	private static final long serialVersionUID = 1L;

	/**
	 * A no-op lock instance that performs no actual locking operations.
	 *
	 * <p>
	 * This instance can be used when locking is conditionally disabled or when you need a
	 * {@link SimpleReadWriteLock} instance but don't want any actual synchronization overhead.
	 * All lock operations on this instance are no-ops and return {@link SimpleLock#NO_OP}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Use NO_OP when locking is disabled</jc>
	 * 	<jk>var</jk> <jv>lock</jv> = isLockingEnabled() ? <jk>new</jk> SimpleReadWriteLock() : SimpleReadWriteLock.NO_OP;
	 * 	<jk>try</jk> (<jk>var</jk> <jv>readLock</jv> = <jv>lock</jv>.read()) {
	 * 		<jc>// Code works the same whether locking is enabled or not</jc>
	 * 		<jv>sharedResource</jv>.read();
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Calling {@link #read()} or {@link #write()} on this instance returns {@link SimpleLock#NO_OP}.
	 * 	<li class='note'>Closing the returned {@link SimpleLock} has no effect.
	 * 	<li class='note'>This instance is thread-safe and can be shared across threads.
	 * </ul>
	 */
	public static SimpleReadWriteLock NO_OP = new SimpleReadWriteLock() {
		private static final long serialVersionUID = 1L;

		@Override
		public SimpleLock read() {
			return SimpleLock.NO_OP;
		}

		@Override
		public SimpleLock write() {
			return SimpleLock.NO_OP;
		}
	};

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new read-write lock with an unfair ordering policy. Unfair locks generally
	 * provide better throughput under high contention but may not guarantee fairness.
	 */
	public SimpleReadWriteLock() {}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new read-write lock with the specified fairness policy.
	 *
	 * <h5 class='section'>Fair vs Unfair:</h5><ul>
	 * 	<li class='note'><b>Fair locks</b> (<jk>true</jk>): Threads acquire locks in approximately
	 * 		FIFO order. Provides fairness but may have lower throughput under high contention.
	 * 	<li class='note'><b>Unfair locks</b> (<jk>false</jk>): No ordering guarantee. Generally provides
	 * 		better throughput but may starve some threads under high contention.
	 * </ul>
	 *
	 * @param fair <jk>true</jk> if this lock should use a fair ordering policy, <jk>false</jk> for unfair.
	 */
	public SimpleReadWriteLock(boolean fair) {
		super(fair);
	}

	/**
	 * Acquires and returns a read lock.
	 *
	 * <p>
	 * The returned {@link SimpleLock} automatically acquires the read lock when created and releases
	 * it when {@link SimpleLock#close()} is called. Multiple threads can hold read locks simultaneously,
	 * but a write lock cannot be acquired while any read locks are held.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>lock</jv> = <jk>new</jk> SimpleReadWriteLock();
	 * 	<jk>try</jk> (<jk>var</jk> <jv>readLock</jv> = <jv>lock</jv>.read()) {
	 * 		<jc>// Multiple threads can hold read locks simultaneously</jc>
	 * 		<jv>sharedResource</jv>.read();
	 * 	}
	 * 	<jc>// Lock is automatically released</jc>
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method blocks until the read lock can be acquired.
	 * 	<li class='note'>Multiple threads can hold read locks concurrently.
	 * 	<li class='note'>Read locks cannot be acquired while a write lock is held by another thread.
	 * 	<li class='note'>The same thread can acquire multiple read locks (reentrant).
	 * 	<li class='note'>Each call returns a new {@link SimpleLock} instance.
	 * </ul>
	 *
	 * @return A new {@link SimpleLock} that holds the read lock. The lock is automatically acquired.
	 */
	public SimpleLock read() {
		return new SimpleLock(readLock());
	}

	/**
	 * Acquires and returns a write lock.
	 *
	 * <p>
	 * The returned {@link SimpleLock} automatically acquires the write lock when created and releases
	 * it when {@link SimpleLock#close()} is called. Write locks are exclusive - only one thread can
	 * hold a write lock at a time, and no read locks can be held while a write lock is active.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>lock</jv> = <jk>new</jk> SimpleReadWriteLock();
	 * 	<jk>try</jk> (<jk>var</jk> <jv>writeLock</jv> = <jv>lock</jv>.write()) {
	 * 		<jc>// Only one thread can hold a write lock at a time</jc>
	 * 		<jv>sharedResource</jv>.write(<js>"new value"</js>);
	 * 	}
	 * 	<jc>// Lock is automatically released</jc>
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method blocks until the write lock can be acquired.
	 * 	<li class='note'>Write locks are exclusive - only one thread can hold a write lock at a time.
	 * 	<li class='note'>Write locks cannot be acquired while any read locks are held by other threads.
	 * 	<li class='note'>Read locks cannot be acquired while a write lock is held.
	 * 	<li class='note'>The same thread can acquire multiple write locks (reentrant).
	 * 	<li class='note'>Each call returns a new {@link SimpleLock} instance.
	 * </ul>
	 *
	 * @return A new {@link SimpleLock} that holds the write lock. The lock is automatically acquired.
	 */
	public SimpleLock write() {
		return new SimpleLock(writeLock());
	}
}