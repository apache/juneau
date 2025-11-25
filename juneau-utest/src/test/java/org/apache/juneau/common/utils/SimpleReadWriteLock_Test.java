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
package org.apache.juneau.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link SimpleReadWriteLock}.
 */
class SimpleReadWriteLock_Test extends TestBase {

	//====================================================================================================
	// NO_OP static field tests
	//====================================================================================================

	@Test
	void a01_noOp_exists() {
		assertNotNull(SimpleReadWriteLock.NO_OP);
	}

	@Test
	void a02_noOp_readReturnsNoOp() {
		var lock = SimpleReadWriteLock.NO_OP.read();
		assertSame(SimpleLock.NO_OP, lock);
	}

	@Test
	void a03_noOp_writeReturnsNoOp() {
		var lock = SimpleReadWriteLock.NO_OP.write();
		assertSame(SimpleLock.NO_OP, lock);
	}

	@Test
	void a04_noOp_closeDoesNotThrow() throws Exception {
		var lock = SimpleReadWriteLock.NO_OP.read();
		// Should not throw when closing NO_OP lock
		assertDoesNotThrow(() -> lock.close());
	}

	//====================================================================================================
	// Constructor tests
	//====================================================================================================

	@Test
	void b01_constructor_default() {
		var lock = new SimpleReadWriteLock();
		assertNotNull(lock);
		assertFalse(lock.isFair());
	}

	@Test
	void b02_constructor_fair() {
		var lock = new SimpleReadWriteLock(true);
		assertNotNull(lock);
		assertTrue(lock.isFair());
	}

	@Test
	void b03_constructor_unfair() {
		var lock = new SimpleReadWriteLock(false);
		assertNotNull(lock);
		assertFalse(lock.isFair());
	}

	//====================================================================================================
	// read() tests
	//====================================================================================================

	@Test
	void c01_read_returnsSimpleLock() {
		var rwLock = new SimpleReadWriteLock();
		var lock = rwLock.read();
		assertNotNull(lock);
		assertInstanceOf(SimpleLock.class, lock);
	}

	@Test
	void c02_read_returnsNewInstance() {
		var rwLock = new SimpleReadWriteLock();
		var lock1 = rwLock.read();
		var lock2 = rwLock.read();
		assertNotSame(lock1, lock2);
	}

	@Test
	void c03_read_canClose() throws Exception {
		var rwLock = new SimpleReadWriteLock();
		var lock = rwLock.read();
		// Should not throw when closing
		assertDoesNotThrow(() -> lock.close());
	}

	@Test
	void c04_read_multipleCalls() {
		var rwLock = new SimpleReadWriteLock();
		var lock1 = rwLock.read();
		var lock2 = rwLock.read();
		var lock3 = rwLock.read();
		assertNotNull(lock1);
		assertNotNull(lock2);
		assertNotNull(lock3);
	}

	//====================================================================================================
	// write() tests
	//====================================================================================================

	@Test
	void d01_write_returnsSimpleLock() {
		var rwLock = new SimpleReadWriteLock();
		var lock = rwLock.write();
		assertNotNull(lock);
		assertInstanceOf(SimpleLock.class, lock);
	}

	@Test
	void d02_write_returnsNewInstance() {
		var rwLock = new SimpleReadWriteLock();
		var lock1 = rwLock.write();
		var lock2 = rwLock.write();
		assertNotSame(lock1, lock2);
	}

	@Test
	void d03_write_canClose() throws Exception {
		var rwLock = new SimpleReadWriteLock();
		var lock = rwLock.write();
		// Should not throw when closing
		assertDoesNotThrow(() -> lock.close());
	}

	@Test
	void d04_write_multipleCalls() {
		var rwLock = new SimpleReadWriteLock();
		var lock1 = rwLock.write();
		var lock2 = rwLock.write();
		var lock3 = rwLock.write();
		assertNotNull(lock1);
		assertNotNull(lock2);
		assertNotNull(lock3);
	}

	//====================================================================================================
	// read() and write() interaction tests
	//====================================================================================================

	@Test
	void e01_readAndWrite_differentInstances() throws Exception {
		var rwLock = new SimpleReadWriteLock();
		var readLock = rwLock.read();
		readLock.close();  // Release read lock before acquiring write lock
		var writeLock = rwLock.write();
		assertNotSame(readLock, writeLock);
		writeLock.close();  // Clean up
	}

	@Test
	void e02_readAndWrite_bothCanBeCreated() throws Exception {
		var rwLock = new SimpleReadWriteLock();
		var readLock = rwLock.read();
		readLock.close();  // Release read lock before acquiring write lock
		var writeLock = rwLock.write();
		assertNotNull(readLock);
		assertNotNull(writeLock);
		writeLock.close();  // Clean up
	}

	@Test
	void e03_readAndWrite_bothCanBeClosed() throws Exception {
		var rwLock = new SimpleReadWriteLock();
		var readLock = rwLock.read();
		readLock.close();  // Release read lock before acquiring write lock
		var writeLock = rwLock.write();
		assertDoesNotThrow(() -> writeLock.close());
		// Both locks have been closed successfully
	}

	//====================================================================================================
	// Inherited ReentrantReadWriteLock methods tests
	//====================================================================================================

	@Test
	void f01_isFair_default() {
		var lock = new SimpleReadWriteLock();
		assertFalse(lock.isFair());
	}

	@Test
	void f02_isFair_explicit() {
		var fairLock = new SimpleReadWriteLock(true);
		var unfairLock = new SimpleReadWriteLock(false);
		assertTrue(fairLock.isFair());
		assertFalse(unfairLock.isFair());
	}

	@Test
	void f03_getReadLockCount_initial() {
		var lock = new SimpleReadWriteLock();
		assertEquals(0, lock.getReadLockCount());
	}

	@Test
	void f04_getWriteHoldCount_initial() {
		var lock = new SimpleReadWriteLock();
		assertEquals(0, lock.getWriteHoldCount());
	}

	@Test
	void f05_isWriteLocked_initial() {
		var lock = new SimpleReadWriteLock();
		assertFalse(lock.isWriteLocked());
	}

	@Test
	void f06_isWriteLockedByCurrentThread_initial() {
		var lock = new SimpleReadWriteLock();
		assertFalse(lock.isWriteLockedByCurrentThread());
	}

	@Test
	void f07_getReadHoldCount_initial() {
		var lock = new SimpleReadWriteLock();
		assertEquals(0, lock.getReadHoldCount());
	}

	@Test
	void f08_getQueueLength_initial() {
		var lock = new SimpleReadWriteLock();
		assertEquals(0, lock.getQueueLength());
	}

	@Test
	void f09_hasQueuedThreads_initial() {
		var lock = new SimpleReadWriteLock();
		assertFalse(lock.hasQueuedThreads());
	}

	@Test
	void f10_hasQueuedThread_currentThread() {
		var lock = new SimpleReadWriteLock();
		assertFalse(lock.hasQueuedThread(Thread.currentThread()));
	}

	@Test
	void f11_getReadLockCount_afterRead() throws Exception {
		var lock = new SimpleReadWriteLock();
		var readLock = lock.read();
		// After acquiring read lock, count should be > 0
		assertTrue(lock.getReadLockCount() > 0);
		readLock.close();
	}

	@Test
	void f12_isWriteLocked_afterWrite() throws Exception {
		var lock = new SimpleReadWriteLock();
		var writeLock = lock.write();
		// After acquiring write lock, should be write locked
		assertTrue(lock.isWriteLocked());
		writeLock.close();
	}

	@Test
	void f13_isWriteLockedByCurrentThread_afterWrite() throws Exception {
		var lock = new SimpleReadWriteLock();
		var writeLock = lock.write();
		// After acquiring write lock, current thread should hold it
		assertTrue(lock.isWriteLockedByCurrentThread());
		writeLock.close();
	}

	@Test
	void f14_getReadHoldCount_afterRead() throws Exception {
		var lock = new SimpleReadWriteLock();
		var readLock = lock.read();
		// After acquiring read lock, hold count should be > 0
		assertTrue(lock.getReadHoldCount() > 0);
		readLock.close();
	}

	@Test
	void f15_getWriteHoldCount_afterWrite() throws Exception {
		var lock = new SimpleReadWriteLock();
		var writeLock = lock.write();
		// After acquiring write lock, hold count should be > 0
		assertTrue(lock.getWriteHoldCount() > 0);
		writeLock.close();
	}

	//====================================================================================================
	// try-with-resources tests
	//====================================================================================================

	@Test
	void g01_tryWithResources_read() throws Exception {
		var lock = new SimpleReadWriteLock();
		try (var readLock = lock.read()) {
			assertNotNull(readLock);
			assertTrue(lock.getReadLockCount() > 0);
		}
		// After try-with-resources, lock should be released
		assertEquals(0, lock.getReadLockCount());
	}

	@Test
	void g02_tryWithResources_write() throws Exception {
		var lock = new SimpleReadWriteLock();
		try (var writeLock = lock.write()) {
			assertNotNull(writeLock);
			assertTrue(lock.isWriteLocked());
		}
		// After try-with-resources, lock should be released
		assertFalse(lock.isWriteLocked());
	}

	@Test
	void g03_tryWithResources_readAndWrite() throws Exception {
		var lock = new SimpleReadWriteLock();
		try (var readLock = lock.read()) {
			assertNotNull(readLock);
		}
		// Read lock is released, now we can acquire write lock
		try (var writeLock = lock.write()) {
			assertNotNull(writeLock);
		}
		// After try-with-resources, locks should be released
		assertEquals(0, lock.getReadLockCount());
		assertFalse(lock.isWriteLocked());
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void h01_edgeCase_multipleReadLocks() throws Exception {
		var lock = new SimpleReadWriteLock();
		var readLock1 = lock.read();
		var readLock2 = lock.read();
		var readLock3 = lock.read();
		assertNotNull(readLock1);
		assertNotNull(readLock2);
		assertNotNull(readLock3);
		readLock1.close();
		readLock2.close();
		readLock3.close();
	}

	@Test
	void h02_edgeCase_multipleWriteLocks() throws Exception {
		var lock = new SimpleReadWriteLock();
		var writeLock1 = lock.write();
		writeLock1.close();
		var writeLock2 = lock.write();
		writeLock2.close();
		var writeLock3 = lock.write();
		writeLock3.close();
		// All should work
		assertFalse(lock.isWriteLocked());
	}

	@Test
	void h03_edgeCase_fairLock() throws Exception {
		var lock = new SimpleReadWriteLock(true);
		assertTrue(lock.isFair());
		var readLock = lock.read();
		assertNotNull(readLock);
		readLock.close();  // Release read lock before acquiring write lock
		var writeLock = lock.write();
		assertNotNull(writeLock);
		writeLock.close();
	}

	@Test
	void h04_edgeCase_unfairLock() throws Exception {
		var lock = new SimpleReadWriteLock(false);
		assertFalse(lock.isFair());
		var readLock = lock.read();
		assertNotNull(readLock);
		readLock.close();  // Release read lock before acquiring write lock
		var writeLock = lock.write();
		assertNotNull(writeLock);
		writeLock.close();
	}
}

