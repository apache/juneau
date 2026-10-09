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
package org.apache.juneau.rest.server.bus;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.sse.*;
import org.junit.jupiter.api.*;

/** Tests for {@link SseFrameSink}, the SSE transport's {@link FrameSink} over an {@link SseSubscription}. */
@SuppressWarnings({
	"resource" // Each test's broadcaster and subscriptions are in-memory queues; nothing external to leak.
})
class SseFrameSink_Test extends TestBase {

	private static final Duration SHORT = Duration.ofMillis(20);
	private static final String A = BusFrames.pub("ops.jobs", "{\"n\":1}", true, 1);
	private static final String B = BusFrames.pub("ops.jobs", "{\"n\":2}", true, 2);
	private static final String C = BusFrames.pub("ops.jobs", "{\"n\":3}", true, 3);

	private final SseBroadcaster streams = new SseBroadcaster(3);

	/** A sink bounded at 2 frames over a queue of 3, as ServerBus sizes them. */
	private SseFrameSink sink() {
		return new SseFrameSink(streams.subscribe("k"), 2);
	}

	private static String data(SseEvent e) {
		assertNotNull(e, "expected an event");
		return e.getData();
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Test void a01_fifoAndBounded() throws Exception {
		var k = sink();
		assertTrue(k.offer(A));
		assertTrue(k.offer(B));
		assertFalse(k.offer(C), "full at max, before the queue's spare slot");
		assertEquals(A, data(k.poll(SHORT)));
		assertTrue(k.offer(C));
		assertEquals(B, data(k.poll(SHORT)));
		assertEquals(C, data(k.poll(SHORT)));
		assertNull(k.poll(SHORT), "times out when empty");
		assertFalse(k.isDone(), "empty is not done until closed");
	}

	@Test void a02_eachFrameIsOneBusEventWithItsSeqAsId() throws Exception {
		var k = sink();
		k.offer(B);
		k.offer(BusFrames.ping());
		var e = k.poll(SHORT);
		assertEquals(BusFrames.EVENT, e.getEvent());
		assertEquals("2", e.getId());
		assertNull(k.poll(SHORT).getId(), "a frame without a seq has no id");
	}

	@Test void a03_anOrdinaryCloseDrainsThenEnds() throws Exception {
		var k = sink();
		k.offer(A);
		k.offer(B);
		k.close(1001, "bus:closed");
		assertFalse(k.offer(C), "closed");
		assertFalse(k.isDone());
		assertEquals(A, data(k.poll(SHORT)));
		assertEquals(B, data(k.poll(SHORT)));
		assertNull(k.poll(Duration.ofSeconds(10)), "returns at once once closed and drained");
		assertTrue(k.isDone());
	}

	@Test void a04_aSlowConsumerCloseLeavesOnlyTheErrorFrame() throws Exception {
		var k = sink();
		k.offer(A);
		k.offer(B);
		assertFalse(k.offer(C));
		k.close(4429, "bus:slow-consumer");
		assertEquals(BusFrames.error("bus:slow-consumer", "the connection fell behind", null), data(k.poll(SHORT)));
		assertNull(k.poll(SHORT));
		assertTrue(k.isDone());
	}

	@Test void a05_aReplacedCloseDropsTheBacklog() throws Exception {
		var k = sink();
		k.offer(A);
		k.close(4409, "bus:replaced");
		assertNull(k.poll(SHORT));
		assertTrue(k.isDone());
	}

	@Test void a06_theFirstCloseWins() throws Exception {
		var k = sink();
		k.close(4429, "bus:slow-consumer");
		k.close(1001, "bus:closed");
		assertNotNull(k.poll(SHORT), "the second close does not drop the error frame");
		assertTrue(k.isDone());
	}

	@Test void a07_pollWakesOnOfferAndOnClose() throws Exception {
		var k = sink();
		var t = new Thread(() -> {
			sleep(50);
			k.offer(A);
			sleep(50);
			k.close(1001, "bus:closed");
		});
		t.start();
		assertEquals(A, data(k.poll(Duration.ofSeconds(10))));
		var start = System.nanoTime();
		assertNull(k.poll(Duration.ofSeconds(10)));
		assertTrue(System.nanoTime() - start < 5_000_000_000L, "woke on the close, not on the timeout");
		assertTrue(k.isDone());
		t.join();
	}

	@Test void a08_reSubscribingTheKeyEndsTheOldSinkQuietly() throws Exception {
		var old = sink();
		old.offer(A);
		var replacement = sink();
		assertFalse(old.offer(B), "the old subscription was replaced");
		old.close(4429, "bus:slow-consumer");   // What ServerBus does after a refused offer.
		assertNull(old.poll(SHORT), "a replaced stream does not get the slow-consumer frame");
		assertTrue(old.isDone());
		assertTrue(replacement.offer(C));
		assertEquals(C, data(replacement.poll(SHORT)));
	}

	@Test void a09_releaseEndsTheSink() throws Exception {
		var k = sink();
		k.offer(A);
		k.release();
		assertFalse(k.offer(B));
		assertNull(k.poll(SHORT));
		assertTrue(k.isDone());
		k.close(1001, "bus:closed");   // The bus may still close it afterwards; harmless.
	}
}
