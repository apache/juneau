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
package org.apache.juneau.rest.server.sse;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.sse.*;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource") // 'sub' instances are short-lived in-memory test fixtures backed by a queue; nothing external to leak.
class SseSubscription_Test {

	private static SseEvent pollNoWait(SseSubscription sub) throws Exception {
		var holder = new AtomicReference<SseEvent>();
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

	@Test void a01_getIdAndIsClosed_reflectConstructorArgs() {
		try (var sub = new SseSubscription("s1", 4, id -> {})) {
			assertEquals("s1", sub.getId());
			assertFalse(sub.isClosed());
		}
	}

	@Test void a02_offerAndTake_fifoOrder() throws Exception {
		try (var sub = new SseSubscription("s1", 4, id -> {})) {
			var e1 = new SseEvent("a", "1");
			var e2 = new SseEvent("b", "2");
			assertFalse(sub.offer(e1));
			assertFalse(sub.offer(e2));
			assertSame(e1, sub.take());
			assertSame(e2, sub.take());
		}
	}

	@Test void a03_offerBeyondCapacityDropsOldest() throws Exception {
		try (var sub = new SseSubscription("s1", 2, id -> {})) {
			var e1 = new SseEvent("a", "1");
			var e2 = new SseEvent("b", "2");
			var e3 = new SseEvent("c", "3");
			assertFalse(sub.offer(e1));
			assertFalse(sub.offer(e2));
			assertTrue(sub.offer(e3), "offering beyond capacity must report a drop");
			assertSame(e2, sub.take());
			assertSame(e3, sub.take());
		}
	}

	@Test void a04_offerAfterCloseReturnsFalse() {
		var sub = new SseSubscription("s1", 4, id -> {});
		sub.close();
		assertFalse(sub.offer(new SseEvent("a", "1")));
	}

	/**
	 * Guards against a non-atomic drop-oldest retry loop in {@code offer()}: with a capacity-1 queue and many
	 * concurrent producers, the check-evict-insert sequence (poll-then-offer) must be serialized so the queue
	 * can never end up over capacity and no producer thread ever throws.
	 */
	@Test void a05_concurrentOffers_atCapacity_neverThrowsAndRespectsCapacity() throws Exception {
		var capacity = 1;
		var producerCount = 16;
		var sub = new SseSubscription("s1", capacity, id -> {});
		var errors = new ConcurrentLinkedQueue<Throwable>();
		var start = new CountDownLatch(1);
		var finished = new CountDownLatch(producerCount);
		var threads = new ArrayList<Thread>();
		for (var i = 0; i < producerCount; i++) {
			var data = String.valueOf(i);
			var t = new Thread(() -> {
				try {
					start.await();
					sub.offer(new SseEvent("e", data));
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

		var remaining = new ArrayList<SseEvent>();
		SseEvent e;
		while ((e = pollNoWait(sub)) != null)
			remaining.add(e);
		assertEquals(capacity, remaining.size(),
			"final queue size must equal the bounded capacity after " + producerCount + " concurrent producers");
	}
}
