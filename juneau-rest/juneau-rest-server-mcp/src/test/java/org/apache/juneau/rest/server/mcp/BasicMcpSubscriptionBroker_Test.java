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
package org.apache.juneau.rest.server.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

class BasicMcpSubscriptionBroker_Test {

	@Test void a01_constructorRejectsNonPositiveQueueSize() {
		assertThrows(IllegalArgumentException.class, () -> new BasicMcpSubscriptionBroker(0));
		assertThrows(IllegalArgumentException.class, () -> new BasicMcpSubscriptionBroker(-1));
	}

	@Test void a02_registerAndActiveCount() {
		var broker = new BasicMcpSubscriptionBroker(4);
		assertEquals(0, broker.activeCount());
		try (var s1 = broker.register("s1", new McpSubscriptionFilter(true, true, true, Set.of()))) {
			assertEquals(1, broker.activeCount());
			try (var s2 = broker.register("s2", new McpSubscriptionFilter(true, true, true, Set.of()))) {
				assertEquals(2, broker.activeCount());
			}
		}
	}

	@Test void a03_unregister_closesAndRemoves() {
		var broker = new BasicMcpSubscriptionBroker(4);
		try (var sub = broker.register("s1", new McpSubscriptionFilter(true, true, true, Set.of()))) {
			broker.unregister("s1");
			assertEquals(0, broker.activeCount());
			assertTrue(sub.isClosed());
		}
	}

	@Test void a04_unregisterUnknownId_isNoOp() {
		var broker = new BasicMcpSubscriptionBroker(4);
		assertDoesNotThrow(() -> broker.unregister("nope"));
	}

	@Test void a04b_unregisterNullId_throws() {
		var broker = new BasicMcpSubscriptionBroker(4);
		assertThrows(IllegalArgumentException.class, () -> broker.unregister(null));
	}

	/**
	 * Blocks (via a bounded poll loop, not a fixed sleep) until the given thread has actually parked in a
	 * blocking call, so tests that exercise {@code close()}/{@code unregister()} racing a blocked
	 * {@code take()} are deterministic rather than timing-dependent.
	 */
	private static void awaitParked(Thread thread) throws InterruptedException {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		while (thread.getState() != Thread.State.WAITING && thread.getState() != Thread.State.TIMED_WAITING) {
			if (System.nanoTime() > deadline)
				fail("thread did not reach a parked state in time (state=" + thread.getState() + ")");
			Thread.sleep(5);
		}
	}

	private static McpChangeEvent pollNoWait(McpSubscription sub) throws Exception {
		var holder = new AtomicReference<McpChangeEvent>();
		var thread = new Thread(() -> {
			try {
				holder.set(sub.take());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		thread.start();
		thread.join(200);
		if (thread.isAlive()) {
			thread.interrupt();
			thread.join(200);
			return null;
		}
		return holder.get();
	}

	@Test void a05_filteredFanOut_onlyMatchingSubsReceive() throws Exception {
		var broker = new BasicMcpSubscriptionBroker(4);
		try (var toolsOnly = broker.register("s1", new McpSubscriptionFilter(true, false, false, Set.of()));
				var uriOnly = broker.register("s2", new McpSubscriptionFilter(false, false, false, Set.of("file:///a.txt")));
				var everything = broker.register("s3", new McpSubscriptionFilter(true, true, true, Set.of("file:///a.txt")))) {

			broker.toolsListChanged();
			broker.resourceUpdated("file:///a.txt");
			broker.resourceUpdated("file:///b.txt");
			broker.promptsListChanged();

			assertEquals(McpChangeKind.TOOLS_LIST_CHANGED, toolsOnly.take().getKind());
			assertNull(pollNoWait(toolsOnly), "toolsOnly must not receive resourceUpdated or promptsListChanged");

			assertEquals("file:///a.txt", uriOnly.take().getResourceUri());
			assertNull(pollNoWait(uriOnly), "uriOnly must not receive the non-matching URI or list-changed events");

			assertEquals(McpChangeKind.TOOLS_LIST_CHANGED, everything.take().getKind());
			assertEquals("file:///a.txt", everything.take().getResourceUri());
			assertEquals(McpChangeKind.PROMPTS_LIST_CHANGED, everything.take().getKind());
		}
	}

	@Test void a06_publishDropsOldestWhenSubscriberIsSlow() throws Exception {
		var broker = new BasicMcpSubscriptionBroker(1);
		try (var sub = broker.register("s1", new McpSubscriptionFilter(true, true, true, Set.of()))) {
			broker.toolsListChanged();
			broker.promptsListChanged();
			assertEquals(McpChangeKind.PROMPTS_LIST_CHANGED, sub.take().getKind());
		}
	}

	@Test void a07_take_blocksThenUnblocksOnPublish() throws Exception {
		var broker = new BasicMcpSubscriptionBroker(4);
		try (var sub = broker.register("s1", new McpSubscriptionFilter(true, true, true, Set.of()))) {
			var received = new AtomicReference<McpChangeEvent>();
			var started = new CountDownLatch(1);
			var thread = new Thread(() -> {
				started.countDown();
				try {
					received.set(sub.take());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			thread.start();
			assertTrue(started.await(2, TimeUnit.SECONDS));
			Thread.sleep(100);
			broker.resourcesListChanged();
			thread.join(2000);
			assertFalse(thread.isAlive());
			assertEquals(McpChangeKind.RESOURCES_LIST_CHANGED, received.get().getKind());
		}
	}

	@Test void a08_close_interruptsBlockedTake() throws Exception {
		var broker = new BasicMcpSubscriptionBroker(4);
		// broker.unregister("s1") below closes 'sub' as part of the behavior under test; the
		// try-with-resources close() at the end of this block is a harmless idempotent no-op (see
		// BasicMcpSubscription.close()) that satisfies the resource-leak analysis without disturbing
		// the interrupt-timing assertions.
		try (var sub = broker.register("s1", new McpSubscriptionFilter(true, true, true, Set.of()))) {
			var interrupted = new AtomicBoolean(false);
			var started = new CountDownLatch(1);
			var thread = new Thread(() -> {
				started.countDown();
				try {
					sub.take();
				} catch (InterruptedException e) {
					interrupted.set(true);
				}
			});
			thread.start();
			assertTrue(started.await(2, TimeUnit.SECONDS));
			awaitParked(thread);
			broker.unregister("s1");
			thread.join(2000);
			assertFalse(thread.isAlive());
			assertTrue(interrupted.get());
			assertEquals(0, broker.activeCount());
		}
	}

	@Test void a09_registerSameIdTwice_closesThePreviousSubscription() {
		var broker = new BasicMcpSubscriptionBroker(4);
		// 'first' is already closed by the re-registration below (asserted); the try-with-resources
		// close() is a harmless idempotent no-op for 'first' and closes 'second' only after both
		// assertions on its state have already run.
		try (var first = broker.register("s1", new McpSubscriptionFilter(true, true, true, Set.of()));
				var second = broker.register("s1", new McpSubscriptionFilter(false, false, false, Set.of()))) {
			assertTrue(first.isClosed());
			assertFalse(second.isClosed());
			assertEquals(1, broker.activeCount());
		}
	}

	@Test void a10_resourceUpdated_nullUri_throws() {
		var broker = new BasicMcpSubscriptionBroker(4);
		assertThrows(IllegalArgumentException.class, () -> broker.resourceUpdated(null));
	}

	@Test void a11_registerIfUnder_underCap_registersAndReturnsPresent() {
		var broker = new BasicMcpSubscriptionBroker(4);
		var result = broker.registerIfUnder(2, "s1", new McpSubscriptionFilter(true, true, true, Set.of()));
		try {
			assertTrue(result.isPresent());
			assertEquals(1, broker.activeCount());
			assertFalse(result.get().isClosed());
		} finally {
			result.ifPresent(McpSubscription::close);
		}
	}

	@Test void a12_registerIfUnder_atCap_rejectsAndReturnsEmpty() {
		var broker = new BasicMcpSubscriptionBroker(4);
		broker.registerIfUnder(1, "s1", new McpSubscriptionFilter(true, true, true, Set.of()));
		var result = broker.registerIfUnder(1, "s2", new McpSubscriptionFilter(true, true, true, Set.of()));
		assertTrue(result.isEmpty());
		assertEquals(1, broker.activeCount());
	}

	@Test void a13_registerIfUnder_reRegisteringSameId_isAllowedEvenAtCap_andClosesThePrevious() {
		var broker = new BasicMcpSubscriptionBroker(4);
		// 'first' is already closed by the re-registration below (asserted); the close() in the outer
		// finally is a harmless idempotent no-op.
		var first = broker.registerIfUnder(1, "s1", new McpSubscriptionFilter(true, true, true, Set.of())).orElseThrow();
		try {
			// At cap (1 active, max 1): re-registering the SAME id must still succeed since it replaces, not grows.
			var second = broker.registerIfUnder(1, "s1", new McpSubscriptionFilter(false, false, false, Set.of()));
			try {
				assertTrue(second.isPresent());
				assertTrue(first.isClosed());
				assertFalse(second.get().isClosed());
				assertEquals(1, broker.activeCount());
			} finally {
				second.ifPresent(McpSubscription::close);
			}
		} finally {
			first.close();
		}
	}

	@Test void a14_registerIfUnder_concurrentRace_neverExceedsCapAndRejectsExactlyTheRightCount() throws Exception {
		final var max = 10;
		final var seeded = max - 1;
		var broker = new BasicMcpSubscriptionBroker(4);
		for (var i = 0; i < seeded; i++)
			broker.registerIfUnder(max, "seed-" + i, new McpSubscriptionFilter(true, true, true, Set.of()));
		assertEquals(seeded, broker.activeCount());

		final var threadCount = 50;
		var ready = new CountDownLatch(threadCount);
		var start = new CountDownLatch(1);
		var done = new CountDownLatch(threadCount);
		var accepted = new AtomicInteger();
		var rejected = new AtomicInteger();
		var threads = new ArrayList<Thread>();
		for (var i = 0; i < threadCount; i++) {
			final var id = "racer-" + i;
			var t = new Thread(() -> {
				ready.countDown();
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				var result = broker.registerIfUnder(max, id, new McpSubscriptionFilter(true, true, true, Set.of()));
				(result.isPresent() ? accepted : rejected).incrementAndGet();
				done.countDown();
			});
			threads.add(t);
			t.start();
		}
		assertTrue(ready.await(2, TimeUnit.SECONDS), "threads did not all reach the start gate in time");
		start.countDown();
		assertTrue(done.await(5, TimeUnit.SECONDS), "threads did not all finish in time");
		for (var t : threads)
			t.join(1000);

		assertEquals(max, broker.activeCount(), "active count must never exceed the cap");
		assertEquals(1, accepted.get(), "exactly one racer should have been admitted to fill the last slot");
		assertEquals(threadCount - 1, rejected.get(), "every other racer must be rejected, not silently over-admitted");
	}
}
