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
package org.apache.juneau.terminal.bus;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.bus.*;
import org.apache.juneau.terminal.region.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link TerminalNudges}: validation, per-session delivery, coalescing and lifecycle.
 */
class TerminalNudges_Test extends TestBase {

	private static final String T1 = TerminalNudges.topic("t1");
	private static final String T2 = TerminalNudges.topic("t2");

	/** Records every accepted frame. */
	static final class RecordingSink implements FrameSink {
		final List<String> frames = new ArrayList<>();

		@Override public boolean offer(String frame) {
			frames.add(frame);
			return true;
		}

		@Override public void close(int code, String reason) { /* no-op: the recorder keeps frames only */ }

		void reset() { frames.clear(); }

		/** The payload JSON of every frame received on a topic. */
		List<String> on(String topic) {
			var out = new ArrayList<String>();
			for (var f : frames) {
				try {
					var m = BusFrames.decode(f);
					if (topic.equals(m.get("topic")))
						out.add(Json.of(m.get("payload")));
				} catch (BusRefusal e) {
					throw new AssertionError(f, e);
				}
			}
			return out;
		}
	}

	/** One fake time source behind both the scheduler and the clock. */
	static final class Manual implements TerminalNudges.Scheduler {
		long now;
		boolean closed, armClockFailure, requireStart, started;
		Runnable onStart;
		private long seq;
		private final PriorityQueue<long[]> queue = new PriorityQueue<>((a, b) -> a[0] == b[0] ? Long.compare(a[1], b[1]) : Long.compare(a[0], b[0]));
		private final Map<Long,Runnable> tasks = new HashMap<>();

		@Override public void start() {
			if (onStart != null)
				onStart.run();
			started = true;
		}

		@Override public void schedule(Runnable r, long delayMs) {
			if (requireStart && ! started)
				return;
			var id = seq++;
			tasks.put(id, r);
			queue.add(new long[]{now + delayMs, id});
		}

		@Override public void close() { closed = true; }

		long clock() {
			if (armClockFailure) {
				armClockFailure = false;
				throw new IllegalStateException("clock failure");
			}
			return now;
		}

		int pending() { return queue.size(); }

		void advance(long ms) {
			now += ms;
			while (! queue.isEmpty() && queue.peek()[0] <= now)
				tasks.remove(queue.poll()[1]).run();
		}
	}

	private final Set<String> allowed = new HashSet<>(Set.of("alice"));
	private final Set<String> throwing = new HashSet<>();
	private boolean failHard;
	private Runnable onWatch;

	private final TerminalAccess access = new TerminalAccess() {
		@Override public boolean canWatch(TerminalCaller c, String id) {
			if (onWatch != null)
				onWatch.run();
			if (failHard)
				throw new AssertionError("hard failure");
			if (throwing.contains(c.principalName()))
				throw new IllegalStateException("access failure");
			return allowed.contains(c.principalName());
		}
		@Override public Optional<TerminalSink> inputSink(TerminalCaller c, String id) {
			return Optional.empty();
		}
	};

	private Manual manual;
	private TerminalNudges nudges;
	private ServerBus bus;

	@BeforeEach void setUp() {
		manual = new Manual();
		nudges = new TerminalNudges(access, manual, manual::clock);
		bus = bus(policy().build());
		nudges.bind(bus);
	}

	@AfterEach void tearDown() {
		bus.close();
	}

	private static BusPolicy.Builder policy() {
		return BusPolicy.create().downstream("term.nudge:*", false);
	}

	private static ServerBus bus(BusPolicy p) {
		var b = ServerBus.create(p);
		b.enableWebSocket("/juneau-bus/ws");
		return b;
	}

	private static HttpServletRequest req(String user) {
		Principal principal = user == null ? null : () -> user;
		return (HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "getUserPrincipal" -> principal;
				case "toString" -> "FakeRequest[" + user + "]";
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				default -> method.getReturnType() == boolean.class ? (Object)false : method.getReturnType() == int.class ? (Object)0 : null;
			});
	}

	private static BusSession open(ServerBus bus, String user, String...topics) throws BusRefusal {
		return bus.openSession(req(user), JsonMap.of("v", 1, "bridge", "term", "transport", "websocket",
			"downstream", List.of(topics), "upstream", List.of()));
	}

	private static RecordingSink connect(ServerBus bus, BusSession s) {
		var sink = new RecordingSink();
		bus.connect(s, sink);
		sink.reset();
		return sink;
	}

	private RecordingSink join(String user, String...topics) throws BusRefusal {
		return connect(bus, open(bus, user, topics));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_createNull() {
		assertThrows(IllegalArgumentException.class, () -> TerminalNudges.create(null));
	}

	@Test void a02_bindTwice() {
		assertThrows(IllegalStateException.class, () -> nudges.bind(bus));
		try (var fresh = new TerminalNudges(access, new Manual(), () -> 0L)) {
			assertThrows(IllegalArgumentException.class, () -> fresh.bind(null));
		}
	}

	@Test void a03_bindPolicy() {
		var none = bus(BusPolicy.create().downstream("other.topic", false).build());
		var retained = bus(BusPolicy.create().downstream("term.nudge:*", true).build());
		try {
			try (var n1 = new TerminalNudges(access, new Manual(), () -> 0L); var n2 = new TerminalNudges(access, new Manual(), () -> 0L)) {
				assertThrows(IllegalStateException.class, () -> n1.bind(none));
				assertThrows(IllegalStateException.class, () -> n2.bind(retained));
			}
		} finally {
			none.close();
			retained.close();
		}
	}

	@Test void a04_topic() {
		assertEquals("term.nudge:t1", TerminalNudges.topic("t1"));
		assertEquals("term.nudge", TerminalNudges.FAMILY);
		assertThrows(IllegalArgumentException.class, () -> TerminalNudges.topic("bad id"));
		assertThrows(IllegalArgumentException.class, () -> nudges.nudge("bad id", 1, false));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Delivery
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_allowedOnly() throws Exception {
		var a = join("alice", T1);
		var b = join("bob", T1);
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":10}"), a.on(T1));
		assertEquals(List.of(), b.on(T1));
	}

	@Test void b02_wildcardGrantSkipped() throws Exception {
		var a = join("alice", "term.nudge:*");
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		assertTrue(a.frames.isEmpty());
	}

	@Test void b02b_exactPlusWildcardDelivered() throws Exception {
		var a = join("alice", T1, "term.nudge:*");
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":10}"), a.on(T1));
	}

	@Test void b03_otherTerminalSkipped() throws Exception {
		var a = join("alice", T2);
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		assertTrue(a.frames.isEmpty());
	}

	@Test void b04_disconnectedSkipped() throws Exception {
		var notConnected = open(bus, "alice", T1);
		var late = new RecordingSink();
		var s = open(bus, "alice", T1);
		var sink = new RecordingSink();
		bus.connect(s, sink);
		bus.disconnect(s, sink);
		sink.reset();
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		assertTrue(sink.frames.isEmpty());
		bus.connect(notConnected, late);
		late.reset();
		manual.advance(1000);
		assertTrue(late.frames.isEmpty(), "a nudge sent while not connected is not replayed");
	}

	@Test void b05_canWatchPerDelivery() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		allowed.clear();
		manual.advance(250);
		nudges.nudge("t1", 20, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":10}"), a.on(T1));
	}

	@Test void b06_canWatchThrows() throws Exception {
		allowed.add("bob");
		throwing.add("alice");
		var a = join("alice", T1);
		var b = join("bob", T1);
		nudges.nudge("t1", 10, false);
		LogRecordCapture.quietly(TerminalNudges.class, () -> manual.advance(0));
		assertTrue(a.frames.isEmpty());
		assertEquals(List.of("{\"end\":10}"), b.on(T1));
	}

	@Test void b07_payloadDone() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 7, true);
		manual.advance(0);
		assertEquals(List.of("{\"end\":7,\"done\":true}"), a.on(T1));
	}

	@Test void b08_taskThrowsSchedulerSurvives() throws Exception {
		var a = join("alice", T1, T2);
		nudges.nudge("t1", 10, false);
		manual.armClockFailure = true;
		LogRecordCapture.quietly(TerminalNudges.class, () -> manual.advance(0));
		assertTrue(a.frames.isEmpty());
		nudges.nudge("t2", 5, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":5}"), a.on(T2));
		nudges.nudge("t1", 11, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":11}"), a.on(T1));
	}

	@Test void b09_doneKeptWhenDeliveryThrowsAndFollowUpQueued() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 7, true);
		failHard = true;
		// A nudge that arrives while the delivery runs is the follow-up.
		onWatch = () -> {
			onWatch = null;
			nudges.nudge("t1", 8, false);
		};
		assertThrows(AssertionError.class, () -> manual.advance(0));
		failHard = false;
		manual.advance(250);
		assertEquals(List.of("{\"end\":8,\"done\":true}"), a.on(T1));
	}

	@Test void b09b_stateDroppedWhenDeliveryThrowsWithoutFollowUp() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 7, true);
		failHard = true;
		assertThrows(AssertionError.class, () -> manual.advance(0));
		failHard = false;
		assertEquals(0, nudges.pendingTerminals());
		assertTrue(a.frames.isEmpty());
		nudges.nudge("t1", 8, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":8}"), a.on(T1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Coalescing
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_leadingEdge() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 5, false);
		assertTrue(a.frames.isEmpty(), "delivery never happens inline");
		manual.advance(0);
		assertEquals(List.of("{\"end\":5}"), a.on(T1));
	}

	@Test void c02_trailingCarriesMax() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 5, false);
		manual.advance(0);
		manual.advance(10);
		nudges.nudge("t1", 8, false);
		manual.advance(10);
		nudges.nudge("t1", 12, false);
		manual.advance(10);
		nudges.nudge("t1", 9, false);
		manual.advance(219);
		assertEquals(List.of("{\"end\":5}"), a.on(T1));
		manual.advance(1);
		assertEquals(List.of("{\"end\":5}", "{\"end\":12}"), a.on(T1));
	}

	@Test void c03_doneSticky() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 5, false);
		manual.advance(0);
		manual.advance(10);
		nudges.nudge("t1", 20, true);
		manual.advance(10);
		nudges.nudge("t1", 21, false);
		manual.advance(230);
		assertEquals(List.of("{\"end\":5}", "{\"end\":21,\"done\":true}"), a.on(T1));
	}

	@Test void c03b_doneNotResurrected() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 7, true);
		manual.advance(0);
		nudges.nudge("t1", 30, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":7,\"done\":true}", "{\"end\":30}"), a.on(T1));
	}

	@Test void c04_oneScheduledPerBurst() {
		nudges.nudge("t1", 1, false);
		manual.advance(0);
		manual.advance(5);
		var before = manual.pending();
		for (var i = 0; i < 100; i++)
			nudges.nudge("t1", 10 + i, false);
		assertEquals(before + 1, manual.pending());
	}

	@Test void c05_stateCleanup() {
		nudges.nudge("t1", 5, true);
		manual.advance(0);
		assertEquals(0, nudges.pendingTerminals());
		nudges.nudge("t2", 5, false);
		manual.advance(0);
		assertEquals(1, nudges.pendingTerminals());
		manual.advance(4999);
		assertEquals(1, nudges.pendingTerminals());
		manual.advance(1);
		assertEquals(0, nudges.pendingTerminals());
	}

	@Test void c06_perTerminalIndependent() throws Exception {
		var a = join("alice", T1, T2);
		nudges.nudge("t1", 5, false);
		nudges.nudge("t2", 6, false);
		manual.advance(0);
		assertEquals(List.of("{\"end\":5}"), a.on(T1));
		assertEquals(List.of("{\"end\":6}"), a.on(T2));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lifecycle
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_beforeBindDropped() throws Exception {
		var m = new Manual();
		try (var fresh = new TerminalNudges(access, m, m::clock)) {
			var a = join("alice", T1);
			fresh.nudge("t1", 10, false);
			fresh.bind(bus);
			m.advance(0);
			m.advance(1000);
			assertTrue(a.frames.isEmpty());
			assertEquals(0, m.pending());
		}
	}

	@Test void d02_afterCloseDropped() throws Exception {
		var a = join("alice", T1);
		nudges.close();
		nudges.close();
		assertTrue(manual.closed);
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		assertTrue(a.frames.isEmpty());
	}

	@Test void d02b_closeDropsQueuedFlush() throws Exception {
		throwing.add("alice");
		var a = join("alice", T1);
		nudges.nudge("t1", 5, false);
		LogRecordCapture.quietly(TerminalNudges.class, () -> manual.advance(0));
		assertEquals(1, nudges.warnedTerminals());
		nudges.nudge("t1", 10, false);
		nudges.close();
		manual.advance(250);
		assertTrue(a.frames.isEmpty());
		assertEquals(0, nudges.pendingTerminals());
		assertEquals(0, nudges.warnedTerminals());
	}

	@Test void d02c_closeDropsPendingTrailingWindow() throws Exception {
		var a = join("alice", T1);
		nudges.nudge("t1", 10, false);
		manual.advance(0);
		nudges.nudge("t1", 20, false);
		nudges.close();
		manual.advance(250);
		assertEquals(List.of("{\"end\":10}"), a.on(T1));
		assertEquals(0, nudges.pendingTerminals());
	}

	@Test void d04_nudgeDuringBindIsNotLost() throws Exception {
		var m = new Manual();
		m.requireStart = true;
		try (var fresh = new TerminalNudges(access, m, m::clock)) {
			var a = join("alice", T1);
			var racer = new Thread(() -> fresh.nudge("t1", 10, false));
			m.onStart = () -> {
				racer.start();
				try {
					racer.join(300);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			};
			fresh.bind(bus);
			racer.join();
			m.advance(0);
			assertEquals(List.of("{\"end\":10}"), a.on(T1));
		}
	}

	@Test void d03_defaultSchedulerThread() throws Exception {
		var real = bus(policy().build());
		try {
			var n = TerminalNudges.create(access);
			n.bind(real);
			var t = find();
			assertNotNull(t);
			assertTrue(t.isDaemon());
			n.close();
			t.join(2000);
			assertFalse(t.isAlive());
		} finally {
			real.close();
		}
	}

	private static Thread find() {
		return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().equals("juneau-terminal-nudges") && t.isAlive()).findFirst().orElse(null);
	}
}
