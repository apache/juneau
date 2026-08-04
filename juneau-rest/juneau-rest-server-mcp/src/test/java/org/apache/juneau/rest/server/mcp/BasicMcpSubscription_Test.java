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

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

class BasicMcpSubscription_Test {

	private static McpSubscriptionFilter allFilter() {
		return new McpSubscriptionFilter(true, true, true, Set.of());
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

	@Test void a01_getIdAndFilter_reflectConstructorArgs() {
		var filter = allFilter();
		try (McpSubscription sub = new BasicMcpSubscription("s1", filter, 4, s -> {})) {
			assertEquals("s1", sub.getId());
			assertSame(filter, sub.getFilter());
			assertFalse(sub.isClosed());
		}
	}

	@Test void a02_constructorRejectsNullOrEmptyId() {
		assertThrows(IllegalArgumentException.class, () -> new BasicMcpSubscription(null, allFilter(), 4, s -> {}));
		assertThrows(IllegalArgumentException.class, () -> new BasicMcpSubscription("", allFilter(), 4, s -> {}));
	}

	@Test void a03_offerAndTake_fifoOrder() throws Exception {
		try (var sub = new BasicMcpSubscription("s1", allFilter(), 4, s -> {})) {
			var e1 = new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null);
			var e2 = new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null);
			assertFalse(sub.offer(e1));
			assertFalse(sub.offer(e2));
			assertSame(e1, sub.take());
			assertSame(e2, sub.take());
		}
	}

	@Test void a04_offerBeyondCapacityDropsOldest() throws Exception {
		try (var sub = new BasicMcpSubscription("s1", allFilter(), 2, s -> {})) {
			var e1 = new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null);
			var e2 = new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null);
			var e3 = new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null);
			assertFalse(sub.offer(e1));
			assertFalse(sub.offer(e2));
			assertTrue(sub.offer(e3), "offering beyond capacity must report a drop");
			assertSame(e2, sub.take());
			assertSame(e3, sub.take());
		}
	}

	@Test void a05_offerAfterCloseReturnsFalse() {
		var sub = new BasicMcpSubscription("s1", allFilter(), 4, s -> {});
		sub.close();
		assertFalse(sub.offer(new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null)));
	}

	@Test void a06_take_blocksThenUnblocksOnOffer() throws Exception {
		try (var sub = new BasicMcpSubscription("s1", allFilter(), 4, s -> {})) {
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
			var event = new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null);
			sub.offer(event);
			thread.join(2000);
			assertFalse(thread.isAlive(), "taker thread must unblock once an event is offered");
			assertSame(event, received.get());
		}
	}

	@Test void a07_close_interruptsBlockedTake() throws Exception {
		var sub = new BasicMcpSubscription("s1", allFilter(), 4, s -> {});
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
		sub.close();
		thread.join(2000);
		assertFalse(thread.isAlive(), "taker thread must unblock when close() is called");
		assertTrue(interrupted.get(), "close() must interrupt a thread blocked in take()");
	}

	@Test void a08_close_isIdempotentAndInvokesCloseCallbackOnce() {
		var calls = new AtomicInteger();
		var sub = new BasicMcpSubscription("s1", allFilter(), 4, s -> calls.incrementAndGet());
		sub.close();
		sub.close();
		assertTrue(sub.isClosed());
		assertEquals(1, calls.get());
	}

	@Test void a09_take_afterAlreadyClosed_throwsImmediately() {
		var sub = new BasicMcpSubscription("s1", allFilter(), 4, s -> {});
		sub.close();
		assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertThrows(InterruptedException.class, sub::take));
	}

	@Test void a10_constructorRejectsNonPositiveQueueSize() {
		assertThrows(IllegalArgumentException.class, () -> new BasicMcpSubscription("s1", allFilter(), 0, s -> {}));
		assertThrows(IllegalArgumentException.class, () -> new BasicMcpSubscription("s1", allFilter(), -1, s -> {}));
	}

	@Test void a11_concurrentOffers_atCapacity_neverThrowsAndRespectsCapacity() throws Exception {
		var capacity = 1;
		var producerCount = 16;
		var sub = new BasicMcpSubscription("s1", allFilter(), capacity, s -> {});
		var errors = new ConcurrentLinkedQueue<Throwable>();
		var start = new CountDownLatch(1);
		var finished = new CountDownLatch(producerCount);
		var threads = new ArrayList<Thread>();
		for (var i = 0; i < producerCount; i++) {
			var uri = "file:///" + i + ".txt";
			var t = new Thread(() -> {
				try {
					start.await();
					sub.offer(new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, uri));
				} catch (Throwable e) {
					errors.add(e);
				} finally {
					finished.countDown();
				}
			});
			threads.add(t);
			t.start();
		}
		start.countDown();
		assertTrue(finished.await(5, TimeUnit.SECONDS), "all producer threads must finish");
		for (var t : threads)
			t.join(1000);
		assertTrue(errors.isEmpty(), "no producer thread should throw: " + errors);

		var remaining = new ArrayList<McpChangeEvent>();
		McpChangeEvent e;
		while ((e = pollNoWait(sub)) != null)
			remaining.add(e);
		assertEquals(capacity, remaining.size(),
			"final queue size must equal the bounded capacity after " + producerCount + " concurrent producers");
	}
}
