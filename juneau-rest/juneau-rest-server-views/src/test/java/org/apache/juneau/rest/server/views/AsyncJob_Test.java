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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Unit test for the in-memory one-shot {@link AsyncJob}.
 *
 * <p>
 * Covers the server-side hard limits that are BOTH heap bounds and data-egress disclosure bounds:
 * the per-job output cap and the hard timeout; the reserved async terminal outcomes (cancelled vs
 * cancelled-after-effect); the subscriber cap; the progress buffering / await used by the SSE stream; and the
 * resume cursor the stream picks from a {@code Last-Event-ID} / {@code lastEventId} value.
 */
class AsyncJob_Test extends TestBase {

	private static AsyncJob job() {
		return job(AsyncJobRegistry.MAX_OUTPUT_BYTES, AsyncJobRegistry.MAX_SUBSCRIBERS_PER_JOB, Duration.ofSeconds(120));
	}

	private static AsyncJob job(long maxOutputBytes, int maxSubscribers, Duration timeout) {
		return new AsyncJob("cap-id", Instant.parse("2026-08-19T00:00:00Z"), timeout, maxOutputBytes, maxSubscribers);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Progress buffering + eventsFrom snapshot (the stream resumes via resumeCursor, section h)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_progressBuffersEvents() {
		var j = job();
		assertTrue(j.progress("one"));
		assertTrue(j.progress("two"));
		assertEquals(2, j.eventCount());
		assertEquals(java.util.List.of("one", "two"), j.eventsFrom(0));
		assertEquals(java.util.List.of("two"), j.eventsFrom(1));
		assertEquals(java.util.List.of(), j.eventsFrom(5));
	}

	@Test void a02_idAccessor() {
		assertEquals("cap-id", job().id());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Terminal settling (first settle wins) + reserved async outcomes
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_completeSettlesOnce() {
		var j = job();
		assertTrue(j.complete(ActionResult.success(null)));
		assertTrue(j.isTerminal());
		assertEquals("success", j.result().outcome);
		// A second settle is a no-op; the first outcome stands.
		assertFalse(j.complete(ActionResult.failure()));
		assertEquals("success", j.result().outcome);
	}

	@Test void b02_cancelBeforeEffectIsCancelled() {
		var j = job();
		assertTrue(j.cancel());
		assertEquals("cancelled", j.result().outcome);
	}

	@Test void b03_cancelAfterEffectIsCancelledAfterEffect() {
		// "cancelled" and "cancelled-after-effect" are DIFFERENT outcomes and must not be collapsed.
		var j = job();
		j.markEffectStarted();
		assertTrue(j.effectStarted());
		assertTrue(j.cancel());
		assertEquals("cancelled-after-effect", j.result().outcome);
	}

	@Test void b04_cancelAfterTerminalIsNoOp() {
		var j = job();
		j.complete(ActionResult.success(null));
		assertFalse(j.cancel());
		assertEquals("success", j.result().outcome);
	}

	@Test void b05_markEffectStartedReportsWhetherTheEffectMayRun() {
		// The mark and the check are one lock acquisition, so a producer cannot start its effect on a settled job.
		var running = job();
		assertTrue(running.markEffectStarted());
		assertTrue(running.markEffectStarted());           // idempotent while running
		var cancelled = job();
		cancelled.cancel();
		assertFalse(cancelled.markEffectStarted());
		assertFalse(cancelled.effectStarted());
		assertEquals("cancelled", cancelled.result().outcome);
		var timedOut = job();
		timedOut.enforceTimeout(timedOut.deadline().plusMillis(1));
		assertFalse(timedOut.markEffectStarted());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Output cap - a data-egress disclosure bound, enforced server-side
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_outputCapTerminatesJobAndDropsExcess() {
		var j = job(10, 2, Duration.ofSeconds(120));
		assertTrue(j.progress("12345"));           // 5 bytes -> ok
		assertFalse(j.progress("123456"));         // +6 -> 11 > 10 -> terminate, drop this event
		assertTrue(j.isTerminal());
		assertEquals("failure", j.result().outcome);
		assertTrue(j.result().message.contains("output-limit"), j.result().message);
		assertEquals(1, j.eventCount(), "the over-cap event must be dropped, not streamed");
		// Once terminal, further progress is a no-op.
		assertFalse(j.progress("x"));
		assertEquals(1, j.eventCount());
	}

	@Test void c02_outputBytesTracked() {
		var j = job();
		j.progress("abc");   // 3 bytes
		j.progress("de");    // 2 bytes
		assertEquals(5, j.outputBytes());
	}

	@Test void c03_emptyProgressEventsHitTheCapAndStopGrowing() {
		var j = job(100, 2, Duration.ofSeconds(120));
		for (var i = 0; i < 1000 && ! j.isTerminal(); i++)
			j.progress("");
		assertTrue(j.isTerminal(), "empty events must count toward the cap");
		assertEquals("failure", j.result().outcome);
		assertTrue(j.result().message.contains("output-limit"), j.result().message);
		var count = j.eventCount();
		assertTrue(count <= 100, "events must be bounded by the cap, was " + count);
		assertFalse(j.progress(""));
		assertEquals(count, j.eventCount());
	}

	@Test void c04_nonEmptyProgressBehaviorUnchanged() {
		var j = job(10, 2, Duration.ofSeconds(120));
		assertTrue(j.progress("1234"));
		assertEquals(4, j.outputBytes(), "outputBytes still reports payload bytes only");
		assertEquals(0, j.eventsFrom(0).indexOf("1234"));
		assertEquals(1, j.eventCount());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Hard timeout - a data-egress disclosure bound, enforced server-side
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_timeoutBeforeDeadlineIsNoOp() {
		var j = job(AsyncJobRegistry.MAX_OUTPUT_BYTES, 2, Duration.ofSeconds(120));
		assertFalse(j.enforceTimeout(j.createdAt().plusSeconds(119)));
		assertFalse(j.isTerminal());
	}

	@Test void d02_timeoutPastDeadlineSettlesUnknown() {
		var j = job(AsyncJobRegistry.MAX_OUTPUT_BYTES, 2, Duration.ofSeconds(120));
		assertTrue(j.enforceTimeout(j.createdAt().plusSeconds(121)));
		assertTrue(j.isTerminal());
		assertEquals("unknown", j.result().outcome);
		assertTrue(j.result().message.contains("hard-timeout"), j.result().message);
	}

	@Test void d03_deadlineIsCreatedAtPlusTimeout() {
		var j = job(AsyncJobRegistry.MAX_OUTPUT_BYTES, 2, Duration.ofSeconds(120));
		assertEquals(j.createdAt().plusSeconds(120), j.deadline());
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) awaitUpdate: drains a terminal job in one pass; blocks then wakes for a live event
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_awaitUpdateReturnsBufferedEventsAndResultForTerminalJob() {
		var j = job();
		j.progress("p1");
		j.progress("p2");
		j.complete(ActionResult.success(null));
		var u = j.awaitUpdate(0, Duration.ofMillis(10));
		assertEquals(java.util.List.of("p1", "p2"), u.events());
		assertNotNull(u.result());
		assertEquals("success", u.result().outcome);
	}

	@Test void e02_awaitUpdateBlocksThenWakesOnLiveEvent() {
		var j = job();
		var pool = Executors.newSingleThreadScheduledExecutor();
		try {
			pool.schedule(() -> j.progress("live"), 50, TimeUnit.MILLISECONDS);
			var u = j.awaitUpdate(0, Duration.ofSeconds(2));   // blocks until the scheduled progress arrives
			assertEquals(java.util.List.of("live"), u.events());
			assertNull(u.result());
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void e03_awaitUpdateTimesOutEmptyWhileRunning() {
		var j = job();
		var u = j.awaitUpdate(0, Duration.ofMillis(20));
		assertEquals(java.util.List.of(), u.events());
		assertNull(u.result());
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Subscriber cap
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_subscriberCapEnforced() {
		var j = job(AsyncJobRegistry.MAX_OUTPUT_BYTES, 2, Duration.ofSeconds(120));
		assertTrue(j.acquireSubscriber());
		assertTrue(j.acquireSubscriber());
		assertFalse(j.acquireSubscriber(), "3rd subscriber must be refused (cap=2)");
		assertEquals(2, j.subscriberCount());
		j.releaseSubscriber();
		assertTrue(j.acquireSubscriber(), "a freed slot can be re-acquired");
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) The id is a secret - toString must not leak it
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_toStringRedactsId() {
		var j = new AsyncJob("super-secret-capability", Instant.now(), Duration.ofSeconds(120), 1024, 2);
		var s = j.toString();
		assertFalse(s.contains("super-secret-capability"), s);
		assertTrue(s.contains("<redacted>"), s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) resumeCursor: the stream's start position from a Last-Event-ID / lastEventId value
	//------------------------------------------------------------------------------------------------------------------

	private static AsyncJob jobWith(int events) {
		var j = job();
		for (var i = 0; i < events; i++)
			j.progress("e" + i);
		return j;
	}

	@Test void h01_noValue_isLiveOnly_currentEventCount() {
		var j = jobWith(3);
		assertEquals(3, j.resumeCursor(null));
		assertEquals(3, j.resumeCursor(""));
		assertEquals(3, j.resumeCursor(" \t"));
		assertEquals(0, job().resumeCursor(null));
	}

	@Test void h02_validValues_returnTheCursor() {
		var j = jobWith(3);
		assertEquals(0, j.resumeCursor("0"));
		assertEquals(2, j.resumeCursor("2"));
		assertEquals(3, j.resumeCursor("3"));
		assertEquals(2, j.resumeCursor(" 2 "));
		assertEquals(2, j.resumeCursor("\t2\t"));
		assertEquals(7, jobWith(8).resumeCursor("007"));
		assertEquals(0, job().resumeCursor("0"));
	}

	@ParameterizedTest
	@ValueSource(strings={"4", "-1", "+1", "1.0", "1e2", "0x2", "abc", "\n2", "2\n", "12345678901", "2147483648", "9999999999", "1 2", "\u00a02"})
	void h03_badOrPastTheEndValues_throw(String value) {
		var j = jobWith(3);
		assertThrows(IllegalArgumentException.class, () -> j.resumeCursor(value), value);
	}

	@Test void h04_freshCursor_isLiveOnly_noThreads() {
		var j = jobWith(2);
		var cursor = j.resumeCursor(null);
		j.progress("x");
		var u = j.awaitUpdate(cursor, Duration.ZERO);
		assertEquals(java.util.List.of("x"), u.events());
		assertNull(u.result());
	}

	@Test void h05_resumeCursorOnEndedJob_stillBoundsByEventCount() {
		var j = jobWith(3);
		j.complete(ActionResult.success(null));
		assertEquals(3, j.resumeCursor(null));
		assertEquals(1, j.resumeCursor("1"));
		assertThrows(IllegalArgumentException.class, () -> j.resumeCursor("4"));
		var u = j.awaitUpdate(j.resumeCursor(null), Duration.ZERO);
		assertEquals(java.util.List.of(), u.events());
		assertNotNull(u.result());
	}

	@Test void h06_rejectionMessages_nameTheValueAndBufferSize() {
		var j = jobWith(3);
		var malformed = assertThrows(IllegalArgumentException.class, () -> j.resumeCursor("1e2"));
		assertTrue(malformed.getMessage().contains("1e2"), malformed.getMessage());
		var past = assertThrows(IllegalArgumentException.class, () -> j.resumeCursor("4"));
		assertTrue(past.getMessage().contains("4"), past.getMessage());
		assertTrue(past.getMessage().contains("3"), past.getMessage());
	}
}
