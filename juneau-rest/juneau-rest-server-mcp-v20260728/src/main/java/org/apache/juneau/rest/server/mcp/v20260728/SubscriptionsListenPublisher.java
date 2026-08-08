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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.marshall.sse.SseEvent;
import org.apache.juneau.rest.server.mcp.McpChangeEvent;
import org.apache.juneau.rest.server.mcp.McpSubscription;

/**
 * The {@code Flow.Publisher<SseEvent>} returned by v2 {@code McpRevision.dispatch(...)} for
 * {@code subscriptions/listen}. Emits the acknowledged frame first, then drains {@code subscription},
 * mapping each {@code McpChangeEvent} to its wire notification, until graceful close produces a terminal
 * {@code SubscriptionsListenResult} frame followed by {@code onComplete()}. Backpressure is one frame at a
 * time: exactly one {@code onNext} is emitted per unit of {@code request(n)} demand. A periodic named
 * {@code "ping"} heartbeat event (no data) is interleaved on the same demand-gated path at
 * {@code heartbeatIntervalMs} (disabled when {@code heartbeatIntervalMs <= 0}), standing in for
 * {@code SseHeartbeat} (see this task's design note: {@code SseHeartbeat} is {@link java.io.Writer}-based
 * and has no integration point with a {@code Flow.Publisher}), so a dead connection's failed write surfaces
 * identically to a failed write of any other frame.
 *
 * <p>
 * A separate idle watchdog bounds how long the stream may go without any outbound activity (a delivered
 * change event, the initial acknowledged frame, or a heartbeat) before it is terminated gracefully:
 * {@code idleTimeoutMs} (disabled when {@code idleTimeoutMs <= 0}, which blocks {@link McpSubscription#take()}
 * indefinitely as before). Every such activity resets the idle clock. Because a heartbeat counts as
 * activity, configuring {@code heartbeatIntervalMs > 0} with {@code heartbeatIntervalMs < idleTimeoutMs} keeps
 * an otherwise-quiet-but-healthy stream alive indefinitely; the idle timeout only bites once
 * {@code heartbeatIntervalMs} is disabled (or configured larger than {@code idleTimeoutMs}), where it is the
 * sole guard against a connection that receives no real change events. On expiry the watchdog closes
 * {@code subscription}, which interrupts (or short-circuits) the pump's blocked {@link McpSubscription#take()}
 * call, driving the same graceful {@code awaitDemandThenComplete()} shutdown as any other closed-subscription
 * completion. If the pump is instead blocked waiting on {@code request(n)} demand rather than on
 * {@code take()} at expiry, the watchdog interrupts that wait directly (see {@code Pump.checkIdle()}'s
 * javadoc) so it cannot outlive the subscription it was waiting to drain.
 *
 * <p>
 * Supports exactly one {@link #subscribe(Flow.Subscriber)} call; a second is rejected with
 * {@code onError(IllegalStateException)} rather than spinning a second {@code Pump} over the same shared
 * {@code subscription}.
 */
final class SubscriptionsListenPublisher implements Flow.Publisher<SseEvent> {

	private static final String HEARTBEAT_EVENT_NAME = "ping";
	/** Fallback bound for {@code Pump#awaitDemandThenComplete()} when no heartbeat interval is configured. */
	private static final long DEFAULT_COMPLETION_AWAIT_TIMEOUT_MS = 30_000L;
	private static final long COMPLETION_AWAIT_TIMEOUT_MULTIPLIER = 3L;

	/** A no-op {@code Flow.Subscription} handed to a rejected second subscriber, which never owns a real {@code Pump}. */
	private static final class NoopSubscription implements Flow.Subscription {
		static final NoopSubscription INSTANCE = new NoopSubscription();
		@Override public void request(long n) { /* rejected before any Pump existed; nothing to demand */ }
		@Override public void cancel() { /* rejected before any Pump existed; nothing to cancel */ }
	}

	private final Object listenId;
	private final SubscriptionFilter honoredWireFilter;
	@SuppressWarnings({
		"resource" // This publisher owns the subscription and closes it in every Pump terminal path (cancel(), terminateWithError(), checkIdle(), and run()'s completion); Eclipse JDT @Owning warning is by design.
	})
	private final McpSubscription subscription;
	private final long heartbeatIntervalMs;
	private final long idleTimeoutMs;
	private final AtomicBoolean subscribed = new AtomicBoolean();

	SubscriptionsListenPublisher(Object listenId, SubscriptionFilter honoredWireFilter, McpSubscription subscription, long heartbeatIntervalMs, long idleTimeoutMs) {
		this.listenId = listenId;
		this.honoredWireFilter = honoredWireFilter;
		this.subscription = subscription;
		this.heartbeatIntervalMs = heartbeatIntervalMs;
		this.idleTimeoutMs = idleTimeoutMs;
	}

	/**
	 * The honored wire filter this publisher echoes on its acknowledged frame.
	 *
	 * <p>
	 * Package-visible so tests can confirm capability gating happened before registration, without
	 * decoding the serialized acknowledged frame.
	 *
	 * @return The honored wire filter. Never <jk>null</jk>.
	 */
	SubscriptionFilter honoredFilter() {
		return honoredWireFilter;
	}

	@Override
	public void subscribe(Flow.Subscriber<? super SseEvent> subscriber) {
		if (!subscribed.compareAndSet(false, true)) {
			subscriber.onSubscribe(NoopSubscription.INSTANCE);
			subscriber.onError(new IllegalStateException(
				"SubscriptionsListenPublisher supports only one Subscriber; subscribe() was already called"));
			return;
		}
		subscriber.onSubscribe(new Pump(subscriber));
	}

	/**
	 * Bridges {@link McpSubscription#take()}'s blocking queue to {@code Flow.Subscriber} calls on a
	 * single dedicated daemon thread, honoring one-at-a-time backpressure via a {@link Semaphore} of
	 * outstanding {@code request(n)} credits, plus a second daemon-thread {@link ScheduledExecutorService}
	 * that periodically contends for the same demand credits to interleave heartbeat frames. A thread per
	 * active subscription is acceptable: {@code maxConcurrentSubscriptions} bounds the count. All
	 * {@code onNext} calls (main pump and heartbeat) are serialized through {@code emitLock} so they never
	 * race per the {@code Flow.Subscriber} contract.
	 *
	 * <p>
	 * Every terminal path (graceful completion, {@code cancel()}, an invalid {@code request(n)}, or an
	 * unexpected {@link RuntimeException}) is idempotency-guarded by the same {@code terminated} flag,
	 * closes {@code subscription}, and shuts down both {@code heartbeatExecutor} and
	 * {@code idleWatchdogExecutor} via the shared {@code shutdownExecutors()} helper (always called under
	 * {@code startupLock}). {@code run()} additionally calls it directly on every one of its own exits, not
	 * just via {@code awaitDemandThenComplete()}/{@code terminateWithError()} — belt-and-suspenders given
	 * {@code shutdownNow()}'s idempotence, so no path, present or future, can leak either executor.
	 */
	private final class Pump implements Flow.Subscription {
		private final Flow.Subscriber<? super SseEvent> subscriber;
		private final Semaphore demand = new Semaphore(0);
		private final AtomicBoolean started = new AtomicBoolean();
		private final Object emitLock = new Object();
		/**
		 * Serializes {@code request(n)}'s worker/heartbeat-executor startup against {@code cancel()}'s
		 * teardown, so {@code cancel()} can never observe the half-initialized window between
		 * {@code worker.start()} and {@code heartbeatExecutor}'s field assignment (which would otherwise let
		 * the just-created executor escape unshut-down once {@code request(n)}'s thread resumed and
		 * published it after {@code cancel()} had already looked and found nothing to shut down).
		 */
		private final Object startupLock = new Object();
		private volatile boolean terminated;
		private final AtomicReference<Thread> worker = new AtomicReference<>();
		private final AtomicReference<ScheduledExecutorService> heartbeatExecutor = new AtomicReference<>();
		private final AtomicReference<ScheduledExecutorService> idleWatchdogExecutor = new AtomicReference<>();
		/**
		 * Nanosecond-clock ({@link System#nanoTime()}) activity stamp, deliberately not wall-clock
		 * ({@link System#currentTimeMillis()}): a wall-clock stamp is vulnerable to NTP/clock-step
		 * adjustments — a backward step would defer an overdue idle timeout indefinitely, and a forward
		 * step would reap an otherwise-healthy stream early. {@link System#nanoTime()} has no such
		 * relation to wall-clock time and is immune to both.
		 */
		private final AtomicLong lastActivityAtNanos = new AtomicLong();
		private final long idleTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(idleTimeoutMs);
		/**
		 * {@code true} exactly while {@code worker} is blocked in {@code awaitDemandThenEmit}'s
		 * <b>unbounded</b> {@code demand.acquire()} — the one wait in this class with no timeout of its
		 * own. Lets {@code checkIdle()} tell that specific wait apart from {@code awaitDemandThenComplete}'s
		 * already-bounded {@code tryAcquire}, which needs no help from the watchdog since it always gives
		 * up on its own; see {@code checkIdle()}'s javadoc for why interrupting indiscriminately would be
		 * unsafe.
		 */
		private volatile boolean parkedOnDemand;

		Pump(Flow.Subscriber<? super SseEvent> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void request(long n) {
			if (n <= 0) {
				terminateWithError(new IllegalArgumentException("request(n) must be positive, but was " + n));
				return;
			}
			if (terminated)
				return;
			// Clamps a single request(n) call to Integer.MAX_VALUE permits, but does not defend against
			// cumulative overflow across repeated request() calls (Semaphore.release() would itself throw
			// first if outstanding permits ever approached Integer.MAX_VALUE). Unreachable under this
			// stream's own driver: Pump consumes one permit per frame/heartbeat, and
			// ReactiveResponseProcessor's StreamingSubscriber only ever requests one at a time.
			demand.release(n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n);
			if (started.compareAndSet(false, true)) {
				synchronized (startupLock) {
					if (terminated)
						return; // cancel() already ran (holding the same lock) and found nothing to shut down
					lastActivityAtNanos.set(System.nanoTime());
					var w = new Thread(this::run, "mcp-subscriptions-listen-" + listenId);
					w.setDaemon(true);
					worker.set(w);
					w.start();
					if (heartbeatIntervalMs > 0) {
						var executor = Executors.newSingleThreadScheduledExecutor(r -> {
							var t = new Thread(r, "mcp-subscriptions-heartbeat-" + listenId);
							t.setDaemon(true);
							return t;
						});
						executor.scheduleAtFixedRate(this::sendHeartbeat, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
						heartbeatExecutor.set(executor);
					}
					if (idleTimeoutMs > 0) {
						var checkPeriodMs = Math.max(idleTimeoutMs / 4, 10L);
						var executor = Executors.newSingleThreadScheduledExecutor(r -> {
							var t = new Thread(r, "mcp-subscriptions-idle-" + listenId);
							t.setDaemon(true);
							return t;
						});
						executor.scheduleAtFixedRate(this::checkIdle, checkPeriodMs, checkPeriodMs, TimeUnit.MILLISECONDS);
						idleWatchdogExecutor.set(executor);
					}
				}
			}
		}

		/**
		 * Polled by {@code idleWatchdogExecutor}: if no activity (the initial acknowledged frame, a change
		 * event, or a heartbeat) has been emitted within {@code idleTimeoutMs}, closes {@code subscription}
		 * to drive the pump's blocked (or next) {@link McpSubscription#take()} into the existing graceful
		 * {@code awaitDemandThenComplete()} shutdown path. A no-op once already terminated.
		 *
		 * <p>
		 * {@code subscription.close()} alone only wakes {@code worker} if it is currently blocked inside
		 * {@link McpSubscription#take()}. It does nothing for a {@code worker} instead parked in
		 * {@code awaitDemandThenEmit}'s unbounded {@code demand.acquire()} (already holding a taken event,
		 * but never granted the {@code request(n)} needed to emit it) — that wait would otherwise block
		 * forever past an idle timeout. This method additionally interrupts {@code worker} directly, but
		 * <b>only</b> when {@code parkedOnDemand} is set, i.e. only for that specific unbounded wait.
		 * Deliberately never interrupts unconditionally: an interrupt landing during
		 * {@code awaitDemandThenComplete}'s own (already-bounded, and by then not {@code parkedOnDemand})
		 * {@code tryAcquire} would reintroduce exactly the lost-terminal-frame hazard documented on
		 * {@code run()}'s {@code catch (InterruptedException e)} block — that bounded wait needs no help
		 * from this watchdog, since it always gives up on its own within {@code completionAwaitTimeoutMs()}.
		 */
		private void checkIdle() {
			if (terminated)
				return;
			if (System.nanoTime() - lastActivityAtNanos.get() < idleTimeoutNanos)
				return;
			subscription.close();
			if (parkedOnDemand) {
				var w = worker.get();
				if (w != null)
					w.interrupt();
			}
		}

		/**
		 * Shuts down both {@code heartbeatExecutor} and {@code idleWatchdogExecutor}, guarded by
		 * {@code startupLock} so this can never observe the half-initialized window between
		 * {@code worker.start()} and either executor field's assignment (see {@code startupLock}'s
		 * javadoc). Called from every terminal path ({@code cancel()}, {@code terminateWithError()}, and
		 * every {@code run()} exit via {@code awaitDemandThenComplete()} or directly) so none of them can
		 * leak either executor. {@link ScheduledExecutorService#shutdownNow()} on an already-shut-down
		 * executor is a documented no-op, so calling this more than once for the same {@code Pump} (e.g.
		 * once from an early {@code run()} exit and again from whichever terminal path drove it there) is
		 * always safe.
		 */
		private void shutdownExecutors() {
			synchronized (startupLock) {
				var executor = heartbeatExecutor.get();
				if (executor != null)
					executor.shutdownNow();
				var idle = idleWatchdogExecutor.get();
				if (idle != null)
					idle.shutdownNow();
			}
		}

		@Override
		public void cancel() {
			// shutdownExecutors() re-acquires startupLock, which is safe: Java's intrinsic locks are
			// reentrant, so nesting it inside this already-held block keeps the flag write and the
			// executor read-and-shutdown in one atomic critical section, exactly as before this method
			// was extracted.
			synchronized (startupLock) {
				terminated = true;
				shutdownExecutors();
			}
			var w = worker.get();
			if (w != null)
				w.interrupt();
			subscription.close();
		}

		/**
		 * Idempotently routes an unexpected failure (frame construction or a misbehaving
		 * {@code subscriber.onNext(...)}) to a single {@code onError}, then performs the same cleanup as
		 * every other terminal path.
		 *
	 * <p>
	 * The {@code worker} interrupt and {@code shutdownExecutors()} call are its own {@code startupLock}
	 * critical section — mirroring {@code cancel()} — so it can never observe the same half-initialized
	 * startup window {@code cancel()} was fixed against: without this, a failure landing between
	 * {@code worker.start()} and either executor field's assignment would read a still-{@code null}
	 * executor here, then have {@code request(n)}'s startup block publish it moments later with nothing left
	 * to ever shut it down. That section is deliberately NOT nested inside the {@code emitLock} section
	 * above (each is acquired and released independently) to avoid pairing with {@code
	 * request()}/{@code cancel()}'s {@code startupLock}-then-{@code emitLock} risk in the opposite order
	 * (neither of which actually nests {@code emitLock} today, but keeping this method's two sections
	 * separate keeps that true regardless).
	 */
		private void terminateWithError(Throwable t) {
			synchronized (emitLock) {
				if (terminated)
					return;
				terminated = true;
				subscriber.onError(t);
			}
			synchronized (startupLock) {
				var w = worker.get();
				if (w != null && w != Thread.currentThread())
					w.interrupt();
				shutdownExecutors();
			}
			subscription.close();
		}

		private void sendHeartbeat() {
			if (terminated || !demand.tryAcquire())
				return;
			Throwable failure = null;
			synchronized (emitLock) {
				if (terminated) {
					demand.release();
					return;
				}
				try {
					// A fresh SseEvent per send: this is never mutated afterward, so sharing one instance
					// across sends would in fact be safe too, but constructing a new one keeps that
					// invariant from ever needing to be relied upon.
					subscriber.onNext(new SseEvent().setEvent(HEARTBEAT_EVENT_NAME));
					lastActivityAtNanos.set(System.nanoTime());
				} catch (RuntimeException e) {
					failure = e;
				}
			}
			// Deliberately called after releasing emitLock above: terminateWithError() takes emitLock itself,
			// then separately takes startupLock, and must never do so while still nested inside a caller's
			// own held emitLock (see terminateWithError()'s javadoc on lock ordering).
			if (failure != null)
				terminateWithError(failure);
		}

		/**
		 * Every exit from this method (including both {@code awaitDemandThenEmit} early returns below)
		 * calls {@code shutdownExecutors()} directly, in addition to whatever cleanup
		 * {@code awaitDemandThenComplete}/{@code terminateWithError} already perform on their own paths —
		 * redundant on the two in-tree {@link McpSubscription} implementations today (every real path here
		 * already reaches one of those two methods, or a {@code cancel()} that shut down first), but
		 * hardens {@code run()} itself against ever adding a future exit path that forgets to.
		 * {@code shutdownExecutors()}'s {@code shutdownNow()} calls are idempotent, so the redundancy is free.
		 */
		private void run() {
			try {
				if (!awaitDemandThenEmit(this::acknowledgedFrame)) {
					shutdownExecutors();
					return;
				}
				while (!terminated) {
					var event = takeNextEvent();
					if (event == null)
						return; // interrupted; takeNextEvent() already ran awaitDemandThenComplete() and restored the interrupt flag
					if (subscription.isClosed()) {
						awaitDemandThenComplete();
						return;
					}
					if (!awaitDemandThenEmit(() -> changeFrame(event))) {
						shutdownExecutors();
						return;
					}
				}
			} catch (RuntimeException e) {
				// Catches anything awaitDemandThenEmit() doesn't handle itself (e.g. an unexpected throw from
				// subscription.take()/isClosed()), in addition to propagating frame-construction/onNext
				// failures from awaitDemandThenEmit's own emitLock block once its monitor unwinds.
				terminateWithError(e);
			}
		}

		/**
		 * Drains one event from {@code subscription}, or performs the on-interrupt shutdown sequence and
		 * returns <jk>null</jk> if the wait was interrupted (including by a concurrent {@code close()}).
		 *
		 * <p>
		 * Deliberately does NOT restore the interrupt flag before calling {@code awaitDemandThenComplete()}:
		 * that call still needs to perform a legitimate blocking wait on this same thread to await permission
		 * to emit the terminal frame, and a pre-set interrupt flag would make that wait return immediately
		 * regardless of real demand, silently dropping the terminal frame. The flag is restored afterward,
		 * just before this method returns to the (dying) {@link #run()} caller.
		 */
		private McpChangeEvent takeNextEvent() {
			try {
				return subscription.take();
			} catch (InterruptedException e) {
				awaitDemandThenComplete();
				Thread.currentThread().interrupt();
				return null;
			}
		}

		private boolean awaitDemandThenEmit(Supplier<SseEvent> frame) {
			try {
				parkedOnDemand = true;
				try {
					demand.acquire();
				} finally {
					parkedOnDemand = false;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
			synchronized (emitLock) {
				if (terminated)
					return false;
				subscriber.onNext(frame.get());
			}
			lastActivityAtNanos.set(System.nanoTime());
			return true;
		}

		/**
		 * Bounds the wait for demand to emit the terminal frame, instead of blocking forever if the client
		 * stalls (stops calling {@code request(n)}). On timeout (or interruption), resource release wins
		 * over guaranteeing the terminal frame's delivery: cleanup still runs, but the subscriber may never
		 * see {@code onComplete()}.
		 */
		private void awaitDemandThenComplete() {
			if (terminated) {
				// Another terminal path (cancel()/terminateWithError(), or a concurrent call into this same
				// method) already won the race to set `terminated`; that path is responsible for its own
				// shutdownExecutors() call, but this redundant one is free (see run()'s javadoc) and closes
				// off any future path that might reach `terminated == true` here without having done so.
				shutdownExecutors();
				return;
			}
			boolean acquired;
			try {
				acquired = demand.tryAcquire(completionAwaitTimeoutMs(), TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				acquired = false;
			}
			synchronized (emitLock) {
				if (terminated) {
					shutdownExecutors();
					return;
				}
				terminated = true;
				if (acquired) {
					try {
						subscriber.onNext(terminalFrame());
						subscriber.onComplete();
					} catch (RuntimeException e) {
						// Best effort only: `terminated` is already set above, and the Subscriber contract
						// forbids following an attempted onComplete with onError, so there is no further
						// signal we can deliver here - but the cleanup below must still run regardless.
					}
				}
				// else: a stalled client (no request(n) within the bound) or this thread was interrupted
				// while waiting. No Subscriber signal is sent on this path either.
			}
			// Mirrors cancel()/terminateWithError(): reading and shutting down the executors must happen
			// under startupLock too, not just under emitLock above, so this path can never observe the same
			// half-initialized window between worker.start() and heartbeatExecutor/idleWatchdogExecutor's
			// field assignment (see cancel()'s startupLock javadoc). Without this, a subscription closed and
			// immediately drained (demand already available) racing a request(n) call still inside its own
			// startupLock section could see both executor fields still null here, then have request(n)
			// publish one or both moments later with nothing left to ever shut them down.
			shutdownExecutors();
		}

		private long completionAwaitTimeoutMs() {
			return heartbeatIntervalMs > 0 ? heartbeatIntervalMs * COMPLETION_AWAIT_TIMEOUT_MULTIPLIER : DEFAULT_COMPLETION_AWAIT_TIMEOUT_MS;
		}

		/**
		 * Builds the initial acknowledged-subscription frame. Used only from {@link #run()}, so it lives
		 * here rather than on the enclosing publisher.
		 */
		private SseEvent acknowledgedFrame() {
			var notification = new SubscriptionsAcknowledgedNotification()
				.setNotifications(honoredWireFilter)
				.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, listenId));
			var frame = new JsonRpcRequest()
				.setJsonrpc(McpProtocol.JSON_RPC_2_0)
				.setId(null)
				.setMethod(McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED)
				.setParams(notification);
			return new SseEvent().setData(Json.of(frame));
		}

		/**
		 * Builds the terminal completion frame. Used only from {@link #awaitDemandThenComplete()}, so it
		 * lives here rather than on the enclosing publisher.
		 */
		private SseEvent terminalFrame() {
			var result = new SubscriptionsListenResult()
				.setMeta(new ResultMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, listenId));
			return new SseEvent().setData(Json.of(JsonRpcResponse.ok(listenId, result)));
		}

		/**
		 * Maps a drained {@link McpChangeEvent} to its wire notification frame. Used only from {@link #run()},
		 * so it lives here rather than on the enclosing publisher.
		 */
		private SseEvent changeFrame(McpChangeEvent event) {
			var meta = new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, listenId);
			String method;
			Object params;
			switch (event.kind()) {
				case RESOURCE_UPDATED -> {
					method = McpMethods.NOTIFICATIONS_RESOURCES_UPDATED;
					params = new ResourceUpdatedNotification().setUri(event.resourceUri()).setMeta(meta);
				}
				case TOOLS_LIST_CHANGED -> {
					method = McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED;
					params = new ToolsListChangedNotification().setMeta(meta);
				}
				case PROMPTS_LIST_CHANGED -> {
					method = McpMethods.NOTIFICATIONS_PROMPTS_LIST_CHANGED;
					params = new PromptsListChangedNotification().setMeta(meta);
				}
				case RESOURCES_LIST_CHANGED -> {
					method = McpMethods.NOTIFICATIONS_RESOURCES_LIST_CHANGED;
					params = new ResourcesListChangedNotification().setMeta(meta);
				}
				default -> throw new IllegalStateException("Unhandled McpChangeKind: " + event.kind());
			}
			var frame = new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(null).setMethod(method).setParams(params);
			return new SseEvent().setData(Json.of(frame));
		}
	}
}
