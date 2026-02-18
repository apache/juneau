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

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.concurrent.locks.*;

/**
 * A simple auto-closeable wrapper around a {@link Lock} that automatically acquires the lock
 * when created and releases it when closed.
 *
 * <p>
 * This class wraps any {@link Lock} implementation and makes it work seamlessly with try-with-resources
 * statements. The lock is automatically acquired in the constructor and released when {@link #close()}
 * is called (either explicitly or automatically via try-with-resources).
 *
 * <p>
 * This class is typically used indirectly through {@link SimpleReadWriteLock#read()} and
 * {@link SimpleReadWriteLock#write()}, but can also be used directly with any {@link Lock} instance.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Using with a ReentrantLock</jc>
 * 	<jk>var</jk> <jv>lock</jv> = <jk>new</jk> ReentrantLock();
 * 	<jk>try</jk> (<jk>var</jk> <jv>simpleLock</jv> = <jk>new</jk> SimpleLock(<jv>lock</jv>)) {
 * 		<jc>// Lock is automatically held - perform critical section operations</jc>
 * 		<jv>sharedResource</jv>.modify();
 * 	}
 * 	<jc>// Lock is automatically released when exiting try block</jc>
 *
 * 	<jc>// Using with SimpleReadWriteLock</jc>
 * 	<jk>var</jk> <jv>rwLock</jv> = <jk>new</jk> SimpleReadWriteLock();
 * 	<jk>try</jk> (<jk>var</jk> <jv>readLock</jv> = <jv>rwLock</jv>.read()) {
 * 		<jc>// Read lock is automatically held</jc>
 * 		<jv>sharedResource</jv>.read();
 * 	}
 * 	<jc>// Read lock is automatically released</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe itself. Each instance wraps a single {@link Lock} and should be used
 * by a single thread. The underlying lock's thread-safety guarantees apply to the wrapped lock.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>The lock is automatically acquired in the constructor and released in {@link #close()}.
 * 	<li class='note'>If <jk>null</jk> is passed to the constructor, no locking operations are performed
 * 		(useful for creating no-op locks).
 * 	<li class='note'>This class implements {@link AutoCloseable}, making it ideal for use with
 * 		try-with-resources statements.
 * 	<li class='note'>Multiple calls to {@link #close()} are safe - subsequent calls have no effect if
 * 		the lock has already been released.
 * 	<li class='note'>The underlying lock's reentrancy behavior is preserved - if the lock supports
 * 		reentrant locking, the same thread can acquire it multiple times.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SimpleReadWriteLock} - A read-write lock that returns {@link SimpleLock} instances
 * 	<li class='jc'>{@link Lock} - The underlying lock interface
 * </ul>
 */
public class SimpleLock implements AutoCloseable {

	/**
	 * A no-op lock instance that performs no actual locking operations.
	 *
	 * <p>
	 * This instance can be used when locking is conditionally disabled or when you need a
	 * {@link SimpleLock} instance but don't want any actual synchronization overhead.
	 * All lock operations on this instance are no-ops.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Use NO_OP when locking is disabled</jc>
	 * 	<jk>var</jk> <jv>lock</jv> = isLockingEnabled() ? <jk>new</jk> SimpleLock(<jk>new</jk> ReentrantLock()) : SimpleLock.NO_OP;
	 * 	<jk>try</jk> (<jk>var</jk> <jv>simpleLock</jv> = <jv>lock</jv>) {
	 * 		<jc>// Code works the same whether locking is enabled or not</jc>
	 * 		<jv>sharedResource</jv>.modify();
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Calling {@link #close()} on this instance has no effect.
	 * 	<li class='note'>This instance is thread-safe and can be shared across threads.
	 * 	<li class='note'>This instance wraps a <jk>null</jk> lock, so no actual locking occurs.
	 * </ul>
	 */
	public static final SimpleLock NO_OP = new SimpleLock(null);

	private final Lock lock;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new {@link SimpleLock} wrapper around the specified {@link Lock}. The lock is
	 * automatically acquired during construction. If <jk>null</jk> is passed, no locking operations
	 * are performed (useful for creating no-op locks).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Wrap a ReentrantLock</jc>
	 * 	<jk>var</jk> <jv>underlyingLock</jv> = <jk>new</jk> ReentrantLock();
	 * 	<jk>var</jk> <jv>simpleLock</jv> = <jk>new</jk> SimpleLock(<jv>underlyingLock</jv>);
	 * 	<jc>// Lock is now held - use try-with-resources to ensure release</jc>
	 * 	<jk>try</jk> (<jv>simpleLock</jv>) {
	 * 		<jc>// Critical section</jc>
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The lock is acquired immediately in the constructor. This method may block
	 * 		if the lock is not available.
	 * 	<li class='note'>If <jk>null</jk> is passed, no lock is acquired and {@link #close()} will have no effect.
	 * 	<li class='note'>The underlying lock's behavior (fairness, reentrancy, etc.) is preserved.
	 * </ul>
	 *
	 * @param lock The {@link Lock} being wrapped. Can be <jk>null</jk> to create a no-op lock.
	 */
	public SimpleLock(Lock lock) {
		this.lock = lock;
		if (nn(lock))
			lock.lock();
	}

	/**
	 * Releases the lock.
	 *
	 * <p>
	 * This method is called automatically when used in a try-with-resources statement, or can be
	 * called explicitly to release the lock early. If the lock was <jk>null</jk> (no-op lock),
	 * this method has no effect.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>lock</jv> = <jk>new</jk> SimpleLock(<jk>new</jk> ReentrantLock());
	 * 	<jc>// Lock is held</jc>
	 * 	<jv>lock</jv>.close();  <jc>// Explicitly release the lock</jc>
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method is called automatically when exiting a try-with-resources block.
	 * 	<li class='note'>Multiple calls to this method are safe - subsequent calls have no effect if
	 * 		the lock has already been released.
	 * 	<li class='note'>If the lock was <jk>null</jk> (no-op lock), this method returns immediately
	 * 		without performing any operations.
	 * 	<li class='note'>The underlying lock's unlock behavior is preserved (e.g., reentrant locks
	 * 		must be unlocked the same number of times they were locked).
	 * </ul>
	 *
	 * @throws IllegalMonitorStateException If the current thread does not hold this lock
	 * 	(only applies to certain lock implementations).
	 */
	@Override
	public void close() {
		if (nn(lock)) {
			try {
				// Lock release in finally ensures unlock on all execution paths
			} finally {
				lock.unlock();
			}
		}
	}
}