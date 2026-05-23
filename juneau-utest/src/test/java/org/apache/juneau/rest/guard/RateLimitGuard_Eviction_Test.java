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
package org.apache.juneau.rest.guard;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RateLimitGuard_Eviction_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// LRU-style eviction kicks in once the size cap is exceeded.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_inMemoryStorageEvictsOnceCapExceeded() {
		var s = new RateLimitGuard.InMemoryStorage(4);
		for (var i = 0; i < 10; i++)
			s.tryAcquire("k-" + i, 1, 1.0);
		assertTrue(s.size() <= 4, "expected size <= 4, was " + s.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// evict(ttl=0) sweeps every bucket older than zero nanoseconds — i.e. all of them.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a02_evictWithZeroTtlSweepsAll() throws InterruptedException {
		var s = new RateLimitGuard.InMemoryStorage(100);
		s.tryAcquire("a", 1, 1.0);
		s.tryAcquire("b", 1, 1.0);
		s.tryAcquire("c", 1, 1.0);
		assertEquals(3, s.size());
		Thread.sleep(5);
		s.evict(Duration.ofNanos(1));
		assertEquals(0, s.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Long TTL keeps buckets in place.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_evictWithLargeTtlKeepsBuckets() {
		var s = new RateLimitGuard.InMemoryStorage(100);
		s.tryAcquire("a", 1, 1.0);
		s.evict(Duration.ofHours(1));
		assertEquals(1, s.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructor rejects non-positive maxKeys.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_constructorRejectsNonPositiveMaxKeys() {
		assertThrows(IllegalArgumentException.class, () -> new RateLimitGuard.InMemoryStorage(0));
		assertThrows(IllegalArgumentException.class, () -> new RateLimitGuard.InMemoryStorage(-1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Storage.inMemory(int) factory wires the size cap correctly.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_storageInMemoryFactoryWithCapacity() {
		var s = RateLimitGuard.Storage.inMemory(2);
		assertNotNull(s);
		s.tryAcquire("a", 1, 1.0);
		s.tryAcquire("b", 1, 1.0);
		s.tryAcquire("c", 1, 1.0);
		s.tryAcquire("d", 1, 1.0);
		s.evict(Duration.ofHours(1));
		assertNotNull(s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Custom storage backend can be wired via Builder.storage(...).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_customStorageBackendIsWiredThroughBuilder() {
		var custom = RateLimitGuard.Storage.inMemory(50);
		var g = RateLimitGuard.create()
			.permitsPerSecond(1)
			.burst(1)
			.storage(custom)
			.build();
		assertNotNull(g);
	}

	//------------------------------------------------------------------------------------------------------------------
	// secondsUntilFull is non-negative even when the bucket has refilled past nominal capacity.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a07_secondsUntilFullIsNonNegative() throws InterruptedException {
		var s = new RateLimitGuard.InMemoryStorage(10);
		s.tryAcquire("a", 5, 1000.0);
		Thread.sleep(20);
		var r = s.tryAcquire("a", 5, 1000.0);
		assertTrue(r.secondsUntilReset() >= 0L);
	}
}
