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
package org.apache.juneau.rest.server.bus.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import jakarta.websocket.*;

/**
 * {@link WsFrameSink}: one send in flight, a bounded FIFO behind it, and a close that is handed to another thread.
 */
class WsFrameSink_Test extends TestBase {

	private Session ws;
	private RemoteEndpoint.Async remote;
	private final Queue<Runnable> closer = new ArrayDeque<>();
	private WsFrameSink sink;

	@BeforeEach void setUp() {
		ws = mock(Session.class);
		remote = mock(RemoteEndpoint.Async.class);
		when(ws.getAsyncRemote()).thenReturn(remote);
		sink = new WsFrameSink(ws, 2, closer::add);
	}

	private SendHandler sentHandler(String frame) {
		var h = ArgumentCaptor.forClass(SendHandler.class);
		verify(remote).sendText(eq(frame), h.capture());
		return h.getValue();
	}

	private void runCloser() {
		var r = closer.poll();
		assertNotNull(r, "a close was expected to be handed off");
		r.run();
	}

	private CloseReason closedWith() throws IOException {
		var c = ArgumentCaptor.forClass(CloseReason.class);
		verify(ws).close(c.capture());
		return c.getValue();
	}

	@Test void a01_offerSendsImmediatelyWhenIdle() {
		assertTrue(sink.offer("f1"));
		verify(remote).sendText(eq("f1"), any(SendHandler.class));
	}

	@Test void a02_offersQueueBehindTheInFlightSend_inOrder() {
		assertTrue(sink.offer("f1"));
		assertTrue(sink.offer("f2"));
		assertTrue(sink.offer("f3"));
		verify(remote, never()).sendText(eq("f2"), any(SendHandler.class));
		sentHandler("f1").onResult(new SendResult(ws));
		verify(remote, never()).sendText(eq("f3"), any(SendHandler.class));
		sentHandler("f2").onResult(new SendResult(ws));
		verify(remote).sendText(eq("f3"), any(SendHandler.class));
	}

	@Test void a03_fullQueueRefusesTheOffer() {
		assertTrue(sink.offer("f1"));    // in flight
		assertTrue(sink.offer("f2"));    // queued 1 of 2
		assertTrue(sink.offer("f3"));    // queued 2 of 2
		assertFalse(sink.offer("f4"), "offer must not block; false means the queue is full (the bus closes 4429)");
		verify(remote, never()).sendText(eq("f4"), any(SendHandler.class));
	}

	@Test void a04_aCompletedSendFreesTheSlot() {
		sink.offer("f1");
		sentHandler("f1").onResult(new SendResult(ws));
		assertTrue(sink.offer("f2"));
		verify(remote).sendText(eq("f2"), any(SendHandler.class));
	}

	@Test void b01_closeIsHandedOff_notRunInline() throws Exception {
		sink.close(4429, "bus:slow-consumer");
		verify(ws, never()).close(any(CloseReason.class));
		assertEquals(1, closer.size(), "close must not block the caller: the bus holds its lock");
		runCloser();
		var r = closedWith();
		assertEquals(4429, r.getCloseCode().getCode());
		assertEquals("bus:slow-consumer", r.getReasonPhrase());
	}

	@Test void b02_closeStopsFurtherOffers_andDropsTheQueue() {
		sink.offer("f1");
		sink.offer("f2");
		sink.close(4409, "bus:replaced");
		assertFalse(sink.offer("late"));
		sentHandler("f1").onResult(new SendResult(ws));
		verify(remote, never()).sendText(eq("f2"), any(SendHandler.class));
	}

	@Test void b03_aCloseThatBlocksDoesNotBlockTheCaller() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		doAnswer(i -> { entered.countDown(); release.await(10, TimeUnit.SECONDS); return null; }).when(ws).close(any(CloseReason.class));
		var real = new WsFrameSink(ws, 2);
		var done = new CountDownLatch(1);
		new Thread(() -> { real.close(1001, "bus:closed"); done.countDown(); }).start();
		try {
			assertTrue(done.await(5, TimeUnit.SECONDS), "close() returned without waiting for the socket close");
			assertTrue(entered.await(5, TimeUnit.SECONDS), "the close still ran, on another thread");
		} finally {
			release.countDown();
		}
	}

	@Test void c01_failedSendClosesWith1011_handedOff() throws Exception {
		sink.offer("f1");
		sentHandler("f1").onResult(new SendResult(ws, new IOException("broken pipe")));
		verify(ws, never()).close(any(CloseReason.class));
		runCloser();
		var r = closedWith();
		assertEquals(1011, r.getCloseCode().getCode());
		assertEquals("bus:send-failed", r.getReasonPhrase());
		assertFalse(sink.offer("f2"));
	}

	@Test void c02_aSendThatThrowsClosesAsynchronously() throws Exception {
		doThrow(new IllegalStateException("closed")).when(remote).sendText(anyString(), any(SendHandler.class));
		assertTrue(sink.offer("f1"), "the frame was accepted; the failure surfaces as a close");
		verify(ws, never()).close(any(CloseReason.class));
		runCloser();
		assertEquals(1011, closedWith().getCloseCode().getCode());
		assertFalse(sink.offer("f2"));
	}

	@Test void d01_markClosedStopsOffersWithoutClosing() {
		sink.markClosed();
		assertFalse(sink.offer("late"));
		assertTrue(closer.isEmpty());
		verifyNoInteractions(remote);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) re-entrancy, close-once and the default closer
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_sendsThatCompleteInline_doNotGrowTheStack_andStayInOrder() {
		var big = new WsFrameSink(ws, 2000, closer::add);
		var order = new ArrayList<String>();
		var inFlight = new int[] {0, 0};  // current, max
		var first = new SendHandler[1];
		var inline = new boolean[1];
		doAnswer(i -> {
			var h = i.<SendHandler>getArgument(1);
			order.add(i.getArgument(0));
			inFlight[0]++;
			inFlight[1] = Math.max(inFlight[1], inFlight[0]);
			if (inline[0]) {
				inFlight[0]--;
				h.onResult(new SendResult(ws));
			} else {
				first[0] = h;
			}
			return null;
		}).when(remote).sendText(anyString(), any(SendHandler.class));
		assertTrue(big.offer("f0"));
		for (var n = 1; n <= 1000; n++)
			assertTrue(big.offer("f" + n));
		inline[0] = true;
		inFlight[0]--;
		assertDoesNotThrow(() -> first[0].onResult(new SendResult(ws)));
		assertEquals(1001, order.size());
		for (var n = 0; n <= 1000; n++)
			assertEquals("f" + n, order.get(n));
		assertEquals(1, inFlight[1], "exactly one send was ever in flight");
		assertTrue(big.offer("after"), "the sink is idle again");
		assertEquals("after", order.get(1001));
	}

	@Test void e02_aFailedSendAfterAClose_doesNotCloseAgain() throws Exception {
		sink.offer("f1");
		var h = sentHandler("f1");
		sink.close(1001, "bus:closed");
		h.onResult(new SendResult(ws, new IOException("gone")));
		assertEquals(1, closer.size(), "only the close that was asked for");
	}

	@Test void e03_theDefaultCloserRunsOnABoundedDaemonThreadWithNoContextClassLoader() throws Exception {
		var seen = new CompletableFuture<Thread>();
		var loader = new ClassLoader[1];
		doAnswer(i -> {
			loader[0] = Thread.currentThread().getContextClassLoader();
			seen.complete(Thread.currentThread());
			return null;
		}).when(ws).close(any(CloseReason.class));
		new WsFrameSink(ws, 2).close(1001, "bus:closed");
		var t = seen.get(10, TimeUnit.SECONDS);
		assertTrue(t.isDaemon());
		assertTrue(t.getName().startsWith("juneau-bus-ws-close-"), t.getName());
		assertNull(loader[0]);
	}
}
