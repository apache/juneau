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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.sse.SseEvent;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.Test;

class SubscriptionsListenPublisher_Test {

	/**
	 * Polls until no live thread named {@code name} remains (or the timeout elapses), used to confirm the
	 * worker/heartbeat threads (whose executors/handles are private to {@code Pump}) actually terminate
	 * rather than merely being asked to.
	 */
	private static boolean awaitNoThreadNamed(String name, Duration timeout) throws InterruptedException {
		return awaitTrue(() -> Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.getName().equals(name)), timeout);
	}

	/** Polls {@code condition} until it is {@code true} (or the timeout elapses), for state with no completion signal. */
	private static boolean awaitTrue(java.util.function.BooleanSupplier condition, Duration timeout) throws InterruptedException {
		var deadline = System.nanoTime() + timeout.toNanos();
		do {
			if (condition.getAsBoolean())
				return true;
			Thread.sleep(10);
		} while (System.nanoTime() < deadline);
		return false;
	}

	/** Manual demand control: {@link #nextWithin} issues exactly one {@code request(1)} then waits. */
	private static final class RecordingSubscriber implements Flow.Subscriber<SseEvent> {
		final BlockingQueue<SseEvent> events = new LinkedBlockingQueue<>();
		final CountDownLatch completedOrErrored = new CountDownLatch(1);
		volatile Throwable error;
		Flow.Subscription subscription;

		@Override public void onSubscribe(Flow.Subscription s) { subscription = s; }
		@Override public void onNext(SseEvent item) { events.add(item); }
		@Override public void onComplete() { completedOrErrored.countDown(); }
		@Override public void onError(Throwable t) { error = t; completedOrErrored.countDown(); }

		SseEvent nextWithin(Duration timeout) throws InterruptedException {
			subscription.request(1);
			var event = events.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
			assertNotNull(event, "expected a frame within " + timeout);
			return event;
		}
	}

	/**
	 * Real blocking queue + close()-interrupts-take() semantics, matching the {@code McpSubscription} contract.
	 *
	 * <p>
	 * Guards the "check closed / register taker" step in {@link #take()} and the "flip closed / read taker +
	 * interrupt" step in {@link #close()} with the same {@code lock}, exactly like {@code BasicMcpSubscription}
	 * (Phase 1): without this, a {@code close()} that lands between two {@code take()} calls (i.e. while
	 * {@code taker} is transiently {@code null}) would silently skip the interrupt and the pump thread would
	 * block forever in the next {@code queue.take()} with no further events ever offered — a lost-interrupt
	 * hang, not a rare flake, since nothing thereafter can wake it.
	 */
	private static final class FakeSubscription implements McpSubscription {
		private final BlockingQueue<McpChangeEvent> queue = new LinkedBlockingQueue<>();
		private final AtomicBoolean closed = new AtomicBoolean();
		private final Object lock = new Object();
		private volatile Thread taker;

		void offer(McpChangeEvent event) { queue.offer(event); }

		@Override public String getId() { return "99"; }
		@Override public McpSubscriptionFilter getFilter() { return new McpSubscriptionFilter(true, true, true, Set.of("file:///a")); }

		@Override
		public McpChangeEvent take() throws InterruptedException {
			synchronized (lock) {
				if (closed.get())
					throw new InterruptedException("subscription closed");
				taker = Thread.currentThread();
			}
			try {
				return queue.take();
			} finally {
				taker = null;
			}
		}

		@Override public boolean isClosed() { return closed.get(); }

		@Override
		public void close() {
			if (closed.compareAndSet(false, true)) {
				synchronized (lock) {
					var t = taker;
					if (t != null)
						t.interrupt();
				}
			}
		}
	}

	@Test
	void ackFirstThenEachChangeKindMapsToTheRightFrameThenTerminalOnClose() throws Exception {
		var honored = new SubscriptionFilter().setToolsListChanged(true).setPromptsListChanged(true)
			.setResourcesListChanged(true).setResourceSubscriptions(List.of("file:///a"));
		var subscription = new FakeSubscription();
		var publisher = new SubscriptionsListenPublisher(99, honored, subscription, 0L, 0L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		var ack = subscriber.nextWithin(Duration.ofSeconds(2));
		assertContains("\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\"", ack.getData());
		assertContains("\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":99", ack.getData());

		subscription.offer(new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, "file:///a"));
		var updated = subscriber.nextWithin(Duration.ofSeconds(2));
		assertContains("\"" + McpMethods.NOTIFICATIONS_RESOURCES_UPDATED + "\"", updated.getData());
		assertContains("file:///a", updated.getData());
		assertContains("\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":99", updated.getData());

		subscription.offer(new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null));
		var tools = subscriber.nextWithin(Duration.ofSeconds(2));
		assertContains("\"" + McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED + "\"", tools.getData());

		subscription.offer(new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null));
		var prompts = subscriber.nextWithin(Duration.ofSeconds(2));
		assertContains("\"" + McpMethods.NOTIFICATIONS_PROMPTS_LIST_CHANGED + "\"", prompts.getData());

		subscription.offer(new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null));
		var resources = subscriber.nextWithin(Duration.ofSeconds(2));
		assertContains("\"" + McpMethods.NOTIFICATIONS_RESOURCES_LIST_CHANGED + "\"", resources.getData());

		subscription.close();
		var terminal = subscriber.nextWithin(Duration.ofSeconds(2));
		assertContains("\"result\"", terminal.getData());
		assertContains("\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":99", terminal.getData());
		assertTrue(subscriber.completedOrErrored.await(2, TimeUnit.SECONDS));
	}

	@Test
	void cancelClosesTheUpstreamSubscriptionSoTheBrokerUnregistersIt() {
		var broker = new BasicMcpSubscriptionBroker(1024);
		var sub = broker.register("cancel-test", new McpSubscriptionFilter(false, false, false, Set.of()));
		var publisher = new SubscriptionsListenPublisher("cancel-test", new SubscriptionFilter(), sub, 0L, 0L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		subscriber.subscription.cancel();

		assertEquals(0, broker.activeCount(),
			"Pump.cancel() must close the upstream McpSubscription, which (Phase 1-3 contract) deregisters from the broker");
	}

	@Test
	void honoredFilterAccessorExposesTheSameFilterInstancePassedToTheConstructor() {
		var honored = new SubscriptionFilter().setToolsListChanged(true);
		var publisher = new SubscriptionsListenPublisher(1, honored, new FakeSubscription(), 0L, 0L);
		assertSame(honored, publisher.honoredFilter());
	}

	@Test
	void heartbeatFiresAtTheConfiguredIntervalWhenIdle() throws Exception {
		var subscription = new FakeSubscription(); // never offer()s an event; take() blocks forever
		var publisher = new SubscriptionsListenPublisher(500, new SubscriptionFilter(), subscription, 50L, 0L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		// Grant generous demand up front so heartbeat frames are never backpressure-starved.
		subscriber.subscription.request(10);

		var ack = subscriber.events.poll(2, TimeUnit.SECONDS);
		assertNotNull(ack);
		assertContains(McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED, ack.getData());

		var heartbeat = subscriber.events.poll(2, TimeUnit.SECONDS);
		assertNotNull(heartbeat, "expected a heartbeat frame within 2s of a 50ms heartbeatIntervalMs");
		assertEquals("ping", heartbeat.getEvent());
		assertNull(heartbeat.getData());

		subscriber.subscription.cancel();
	}

	/** Rejects every {@code onNext} to deterministically exercise C1's frame-construction/onNext failure path. */
	private static final class ThrowingSubscriber implements Flow.Subscriber<SseEvent> {
		final RuntimeException boom = new RuntimeException("boom");
		final CountDownLatch errored = new CountDownLatch(1);
		volatile Throwable error;
		Flow.Subscription subscription;

		@Override public void onSubscribe(Flow.Subscription s) { subscription = s; }
		@Override public void onNext(SseEvent item) { throw boom; }
		@Override public void onComplete() { /* not expected on this path */ }
		@Override public void onError(Throwable t) { error = t; errored.countDown(); }
	}

	// C1: an unexpected RuntimeException from frame construction/onNext must route to onError exactly once,
	// and must not leak the heartbeat executor or leave the upstream McpSubscription open.
	@Test
	void onNextThrowingRoutesToOnErrorAndCleansUpTheExecutorAndSubscription() throws Exception {
		var subscription = new FakeSubscription();
		var publisher = new SubscriptionsListenPublisher(321, new SubscriptionFilter(), subscription, 20L, 0L);
		var subscriber = new ThrowingSubscriber();
		publisher.subscribe(subscriber);

		subscriber.subscription.request(1); // triggers the ack frame, which onNext() immediately rejects

		assertTrue(subscriber.errored.await(2, TimeUnit.SECONDS), "expected onError after onNext threw");
		assertSame(subscriber.boom, subscriber.error);
		// terminateWithError()'s cleanup (subscription.close(), executor shutdown) runs just after onError is
		// delivered, not atomically with it, so poll briefly instead of asserting immediately on the latch.
		assertTrue(awaitTrue(subscription::isClosed, Duration.ofSeconds(2)),
			"the upstream McpSubscription must be closed on error");
		assertTrue(awaitNoThreadNamed("mcp-subscriptions-heartbeat-321", Duration.ofSeconds(2)),
			"the heartbeat executor's thread must not survive an onError termination");
		assertTrue(awaitNoThreadNamed("mcp-subscriptions-listen-321", Duration.ofSeconds(2)),
			"the worker thread must not survive an onError termination");
	}

	// C2: cancel() racing request()'s startup block must never leave the heartbeat executor running. Races
	// many iterations via a shared start latch to probe for the interleaving; the fix makes this hold
	// deterministically for every ordering, so this also serves as a permanent regression guard.
	//
	// I2 coverage gap: uses a positive idleTimeoutMs (large enough it can never itself fire during this
	// test) so idleWatchdogExecutor is also constructed inside request()'s startupLock section here,
	// exercising it under the exact same startup race the design exists for - the sibling race test below
	// keeps idleTimeoutMs at 0 (watchdog never constructed) so both configurations remain covered.
	@Test
	void cancelRacingRequestDuringStartupNeverLeaksTheHeartbeatExecutor() throws Exception {
		for (var i = 0; i < 50; i++) {
			var listenId = "race-" + i;
			var subscription = new FakeSubscription();
			var publisher = new SubscriptionsListenPublisher(listenId, new SubscriptionFilter(), subscription, 5L, 60_000L);
			var subscriber = new RecordingSubscriber();
			publisher.subscribe(subscriber);

			var start = new CountDownLatch(1);
			var requester = new Thread(() -> {
				await(start);
				subscriber.subscription.request(1);
			});
			var canceler = new Thread(() -> {
				await(start);
				subscriber.subscription.cancel();
			});
			requester.start();
			canceler.start();
			start.countDown();
			requester.join(2000);
			canceler.join(2000);

			assertTrue(awaitNoThreadNamed("mcp-subscriptions-heartbeat-" + listenId, Duration.ofSeconds(2)),
				"iteration " + i + ": heartbeat executor thread must not survive a cancel()/request() race");
			assertTrue(awaitNoThreadNamed("mcp-subscriptions-idle-" + listenId, Duration.ofSeconds(2)),
				"iteration " + i + ": idle watchdog executor thread must not survive a cancel()/request() race");
			assertTrue(awaitNoThreadNamed("mcp-subscriptions-listen-" + listenId, Duration.ofSeconds(2)),
				"iteration " + i + ": worker thread must not survive a cancel()/request() race");
		}
	}

	// C2 follow-up: terminateWithError() (added for C1) has the same startup-window TOCTOU as the original
	// C2 bug, just via a different trigger - racing request()'s startup block against a concurrent
	// request(0), which calls terminateWithError() from a second thread instead of cancel(). Same shape as
	// the race test above, over many iterations with unique listenIds.
	@Test
	void terminateWithErrorRacingRequestDuringStartupNeverLeaksTheHeartbeatExecutor() throws Exception {
		for (var i = 0; i < 50; i++) {
			var listenId = "term-race-" + i;
			var subscription = new FakeSubscription();
			var publisher = new SubscriptionsListenPublisher(listenId, new SubscriptionFilter(), subscription, 5L, 0L);
			var subscriber = new RecordingSubscriber();
			publisher.subscribe(subscriber);

			var start = new CountDownLatch(1);
			var requester = new Thread(() -> {
				await(start);
				subscriber.subscription.request(1);
			});
			var terminator = new Thread(() -> {
				await(start);
				subscriber.subscription.request(0); // invalid n: triggers terminateWithError() from a 2nd thread
			});
			requester.start();
			terminator.start();
			start.countDown();
			requester.join(2000);
			terminator.join(2000);

			assertTrue(awaitNoThreadNamed("mcp-subscriptions-heartbeat-" + listenId, Duration.ofSeconds(2)),
				"iteration " + i + ": heartbeat executor thread must not survive a terminateWithError()/request() startup race");
			assertTrue(awaitNoThreadNamed("mcp-subscriptions-listen-" + listenId, Duration.ofSeconds(2)),
				"iteration " + i + ": worker thread must not survive a terminateWithError()/request() startup race");
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// I4: request(n) with a non-positive n must deliver onError(IllegalArgumentException), not silently no-op.
	@Test
	void requestWithNonPositiveNDeliversIllegalArgumentExceptionOnce() throws Exception {
		var subscription = new FakeSubscription();
		var publisher = new SubscriptionsListenPublisher(501, new SubscriptionFilter(), subscription, 0L, 0L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		subscriber.subscription.request(0);

		assertTrue(subscriber.completedOrErrored.await(2, TimeUnit.SECONDS), "expected onError after request(0)");
		assertInstanceOf(IllegalArgumentException.class, subscriber.error);
		assertTrue(subscriber.events.isEmpty(), "no frames should ever be emitted after an invalid request(n)");

		// A second invalid call after termination must not deliver a second signal.
		subscriber.error = null;
		subscriber.subscription.request(-5);
		Thread.sleep(50);
		assertNull(subscriber.error, "onError must be delivered at most once, guarded like the other terminal paths");
	}

	// I5: if the client never grants further demand, awaitDemandThenComplete() must still terminate and
	// clean up within a bounded window instead of blocking forever - even though no terminal frame can be
	// delivered to a subscriber that stopped asking for one.
	@Test
	void gracefulCloseWithNoFurtherDemandStillTerminatesAndCleansUpWithinTheBound() throws Exception {
		var subscription = new FakeSubscription();
		// Small heartbeatIntervalMs so the derived completion-await bound is short and this test stays fast.
		var publisher = new SubscriptionsListenPublisher(502, new SubscriptionFilter(), subscription, 20L, 0L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		var ack = subscriber.nextWithin(Duration.ofSeconds(2)); // consumes the only demand ever granted
		assertContains(McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED, ack.getData());

		subscription.close(); // graceful producer-side close; subscriber never calls request(...) again

		assertTrue(awaitNoThreadNamed("mcp-subscriptions-listen-502", Duration.ofSeconds(5)),
			"the worker thread must terminate within the bound even with no further demand");
		assertTrue(awaitNoThreadNamed("mcp-subscriptions-heartbeat-502", Duration.ofSeconds(5)),
			"the heartbeat executor must be shut down within the bound even with no further demand");
	}

	// I3: a small idleTimeoutMs with no heartbeat and no change events must terminate the stream gracefully
	// (terminal frame + onComplete) once idleTimeoutMs elapses since the last activity, and must not leak
	// the worker thread or the idle-watchdog executor.
	@Test
	void idleTimeoutTerminatesTheStreamGracefullyWhenNoActivityOccurs() throws Exception {
		var subscription = new FakeSubscription(); // never offer()s an event; take() blocks forever
		var publisher = new SubscriptionsListenPublisher(504, new SubscriptionFilter(), subscription, 0L, 100L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		var ack = subscriber.nextWithin(Duration.ofSeconds(2)); // consumes the only demand ever granted
		assertContains(McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED, ack.getData());

		// Grant one more unit of demand up front so the terminal frame can be delivered once the idle
		// watchdog fires and closes the subscription.
		subscriber.subscription.request(1);

		var terminal = subscriber.events.poll(3, TimeUnit.SECONDS);
		assertNotNull(terminal, "expected a terminal frame within 3s of a 100ms idleTimeoutMs with no activity");
		assertContains("\"result\"", terminal.getData());
		assertTrue(subscriber.completedOrErrored.await(2, TimeUnit.SECONDS));
		assertTrue(subscription.isClosed(), "the idle watchdog must close the upstream McpSubscription");
		assertTrue(awaitNoThreadNamed("mcp-subscriptions-listen-504", Duration.ofSeconds(2)),
			"the worker thread must not survive an idle-timeout termination");
		// I2/I3 coverage gap: the idle-watchdog executor's own thread must also not survive - it is shut
		// down by the same awaitDemandThenComplete() cleanup (under startupLock) that shuts down
		// heartbeatExecutor, but nothing above asserted that until now.
		assertTrue(awaitNoThreadNamed("mcp-subscriptions-idle-504", Duration.ofSeconds(2)),
			"the idle watchdog executor thread must not survive an idle-timeout termination");
	}

	// I3: heartbeats count as activity, so a heartbeatIntervalMs shorter than idleTimeoutMs must keep an
	// otherwise-quiet stream alive past what idleTimeoutMs alone would allow.
	@Test
	void heartbeatsResetTheIdleClockSoAFasterHeartbeatKeepsTheStreamAlive() throws Exception {
		var subscription = new FakeSubscription(); // never offer()s an event; take() blocks forever
		// idleTimeoutMs (500ms) is generously wider than heartbeatIntervalMs (20ms) and the 400ms poll
		// window below, and request(200) grants far more demand credits than the ~20 heartbeats the poll
		// window could ever consume, so a transient scheduling stall can't flip the terminal-frame
		// assertion below (this test was previously timing-tight at request(20)/idleTimeoutMs=150).
		var publisher = new SubscriptionsListenPublisher(505, new SubscriptionFilter(), subscription, 20L, 500L);
		var subscriber = new RecordingSubscriber();
		publisher.subscribe(subscriber);

		subscriber.subscription.request(200); // generous demand so heartbeats are never backpressure-starved

		var ack = subscriber.events.poll(2, TimeUnit.SECONDS);
		assertNotNull(ack);

		// Poll for 400ms (well past the 150ms idleTimeoutMs, but heartbeats fire every 20ms) and confirm the
		// stream is still alive throughout: only "ping" heartbeats arrive, never a terminal frame.
		var deadline = System.nanoTime() + Duration.ofMillis(400).toNanos();
		while (System.nanoTime() < deadline) {
			var event = subscriber.events.poll(50, TimeUnit.MILLISECONDS);
			if (event != null)
				assertEquals("ping", event.getEvent(), "heartbeats must keep resetting the idle clock, not terminate the stream");
		}
		assertFalse(subscriber.completedOrErrored.await(0, TimeUnit.MILLISECONDS),
			"the stream must still be open: heartbeats faster than idleTimeoutMs must prevent idle termination");

		subscriber.subscription.cancel();
		// I2/I3 coverage gap: cancel() must shut down the idle-watchdog executor it started, exactly like
		// it already shuts down heartbeatExecutor - nothing above asserted that until now.
		assertTrue(awaitNoThreadNamed("mcp-subscriptions-idle-505", Duration.ofSeconds(2)),
			"cancel() must shut down the idle watchdog executor");
	}

	// M8: a second subscribe() must not spin a second Pump over the same McpSubscription; it must instead
	// be rejected with onError(IllegalStateException), leaving the first subscriber's stream untouched.
	@Test
	void secondSubscribeIsRejectedWithIllegalStateExceptionInsteadOfASecondPump() throws Exception {
		var subscription = new FakeSubscription();
		var publisher = new SubscriptionsListenPublisher(503, new SubscriptionFilter(), subscription, 0L, 0L);
		var first = new RecordingSubscriber();
		publisher.subscribe(first);

		var second = new RecordingSubscriber();
		publisher.subscribe(second);

		assertTrue(second.completedOrErrored.await(2, TimeUnit.SECONDS), "expected the second subscriber to be rejected");
		assertInstanceOf(IllegalStateException.class, second.error);
		assertTrue(second.events.isEmpty(), "the second subscriber must never receive any frames");

		// The first subscriber's stream must be unaffected by the rejected second subscribe() attempt.
		first.subscription.request(1);
		var ack = first.events.poll(2, TimeUnit.SECONDS);
		assertNotNull(ack, "the first subscriber's stream must still work normally");
		first.subscription.cancel(); // avoid leaking this test's worker thread into the rest of the suite
	}
}
