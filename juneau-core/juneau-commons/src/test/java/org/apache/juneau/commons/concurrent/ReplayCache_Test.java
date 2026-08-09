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

import static org.apache.juneau.commons.concurrent.ReplayCache.FailMode.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@link ReplayCache} fail-mode policy applied by
 * {@link ReplayCache#checkAndRecord(String, long, ReplayCache.FailMode, java.util.function.Consumer)}.
 */
class ReplayCache_Test extends TestBase {

	/** A cache whose backing "store" is unavailable: every {@code checkAndRecord} throws (after counting the call). */
	private static final class ThrowingReplayCache implements ReplayCache {
		final AtomicInteger calls = new AtomicInteger();

		@Override public boolean checkAndRecord(String id, long expiresAtMs) {
			calls.incrementAndGet();
			throw new IllegalStateException("store unavailable");
		}
	}

	@Test void a01_failOpen_allowsWhenStoreUnavailable() {
		// A thrown failure resolved with FAIL_OPEN is treated as first-seen (allow).
		var cache = new ThrowingReplayCache();
		assertTrue(cache.checkAndRecord("id-1", 0L, FAIL_OPEN));
		assertEquals(1, cache.calls.get());  // the cache WAS consulted -- not vacuous
	}

	@Test void a02_failClosed_rejectsWhenStoreUnavailable() {
		// A thrown failure resolved with FAIL_CLOSED is treated as a replay (reject).
		var cache = new ThrowingReplayCache();
		assertFalse(cache.checkAndRecord("id-1", 0L, FAIL_CLOSED));
		assertEquals(1, cache.calls.get());
	}

	@Test void a03_onFailureHandlerReceivesException() {
		var cache = new ThrowingReplayCache();
		var captured = new AtomicReference<Exception>();
		assertTrue(cache.checkAndRecord("id-1", 0L, FAIL_OPEN, captured::set));
		assertNotNull(captured.get());
		assertEquals("store unavailable", captured.get().getMessage());
	}

	@Test void a04_onFailureNotInvokedWhenNoThrow() {
		var cache = new InMemoryReplayCache();
		var invoked = new AtomicInteger();
		assertTrue(cache.checkAndRecord("id-1", System.currentTimeMillis() + 60_000L, FAIL_CLOSED, e -> invoked.incrementAndGet()));
		assertEquals(0, invoked.get());
	}

	@Test void a05_normalOutcomePassesThroughUnderEitherMode() {
		// When the delegate does not throw, the fail-mode is irrelevant: the real first-seen/replay outcome wins.
		var cache = new InMemoryReplayCache();
		var expiresAtMs = System.currentTimeMillis() + 60_000L;
		assertTrue(cache.checkAndRecord("id-1", expiresAtMs, FAIL_CLOSED));
		assertFalse(cache.checkAndRecord("id-1", expiresAtMs, FAIL_OPEN));
	}

	@Test void a06_nullFailModeRejected() {
		var cache = new InMemoryReplayCache();
		assertThrows(IllegalArgumentException.class, () -> cache.checkAndRecord("id-1", 0L, null));
	}
}
