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
package org.apache.juneau.rest.server.terminal.bus;

import static org.apache.juneau.rest.server.runreport.RunViewChecks.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.terminal.*;

/**
 * Tells browser terminal cards that a terminal has new bytes, so they fetch immediately instead of waiting for the
 * next poll.
 *
 * <p>
 * This is a {@link TerminalNudgePublisher}: hand it to {@link MemoryTerminalSource#nudges(String, TerminalNudgePublisher)} or
 * {@link TerminalProcess#nudges(String, TerminalNudgePublisher)}, or call {@link #nudge(String, long, boolean)} after a
 * write of your own.  Each call is coalesced to at most one frame per terminal per 250 ms, carrying the latest offset,
 * and is published on {@code term.nudge:<id>} as <c>{"end": &lt;offset&gt;}</c>, plus <c>"done": true</c> once no more
 * bytes will arrive.  A nudge is only a hint: the card still reads the bytes through the terminal's bytes endpoint,
 * and keeps polling as a fallback.
 *
 * <p>
 * Wiring.  The bus is built from the policy, so it is bound afterwards, once:
 * <p class='bjava'>
 * 	TerminalNudges <jv>nudges</jv> = TerminalNudges.<jsm>create</jsm>(<jv>access</jv>);
 * 	ServerBus <jv>bus</jv> = ServerBus.<jsm>create</jsm>(BusPolicy.<jsm>create</jsm>()
 * 		.downstream(<js>"term.nudge:*"</js>, <jk>false</jk>)
 * 		.build());
 * 	<jv>nudges</jv>.bind(<jv>bus</jv>);
 * </p>
 *
 * <p>
 * Access.  {@link TerminalAccess#canWatch(TerminalCaller, String)} is called for every delivery, which is up to four
 * times per second per terminal, for each connected session that holds the grant (up to 256 by default).  It must be
 * cheap.  Only sessions holding the exact {@code term.nudge:<id>} grant receive a nudge; a session granted only
 * {@code term.nudge:*} receives none.  A revocation that races a delivery can let that one frame through; the next is
 * denied.
 *
 * <p>
 * Delivery runs on one daemon thread named {@code juneau-terminal-nudges}.  {@link #nudge(String, long, boolean)} only
 * records state and schedules, so it never blocks the writer.
 */
public final class TerminalNudges implements TerminalNudgePublisher, AutoCloseable {

	/** The topic family; a terminal's topic is {@code term.nudge:<id>}. */
	public static final String FAMILY = "term.nudge";

	private static final Logger LOG = Logger.getLogger(TerminalNudges.class.getName());
	private static final long WINDOW_MS = 250;
	private static final long CLEANUP_MS = 5000;
	private static final long WARN_MS = 60_000;

	/** Runs delayed tasks; replaced in tests. */
	interface Scheduler {
		/** Starts the scheduler's thread, if it has one. */
		default void start() {}

		/** Runs a task after a delay. */
		void schedule(Runnable r, long delayMs);

		/** Stops the scheduler. */
		void close();
	}

	private static final class State {
		long end, lastSentAt = Long.MIN_VALUE, lastTouched;
		boolean done, scheduled, cleanupPending;
	}

	private final TerminalAccess access;
	private final Scheduler scheduler;
	private final LongSupplier clock;
	private final Map<String,State> states = new HashMap<>();
	private final Map<String,Long> lastWarned = new HashMap<>();
	private ServerBus bus;
	private boolean closed;

	TerminalNudges(TerminalAccess access, Scheduler scheduler, LongSupplier clock) {
		this.access = access;
		this.scheduler = scheduler;
		this.clock = clock;
	}

	/**
	 * Creates a publisher.
	 *
	 * @param access Who may watch.  Must not be <jk>null</jk>.
	 * @return A new, unbound publisher.
	 * @throws IllegalArgumentException If {@code access} is <jk>null</jk>.
	 */
	public static TerminalNudges create(TerminalAccess access) {
		if (access == null)
			throw new IllegalArgumentException("TerminalNudges.create: access must not be null");
		return new TerminalNudges(access, new ThreadScheduler(), () -> System.nanoTime() / 1_000_000);
	}

	/**
	 * The nudge topic of a terminal.
	 *
	 * @param terminalId The terminal id.
	 * @return {@code term.nudge:<id>}.
	 * @throws IllegalArgumentException If the id is not a valid run id.
	 */
	public static String topic(String terminalId) {
		if (terminalId == null || ! isRunId(terminalId))
			throw new IllegalArgumentException("TerminalNudges: '" + terminalId + "' is not a valid terminal id");
		return FAMILY + ":" + terminalId;
	}

	/**
	 * Binds the bus the nudges are published on, and starts the scheduler.  May be called once.
	 *
	 * @param bus The bus.  Its policy must allow {@code term.nudge:*} downstream without retention.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code bus} is <jk>null</jk>.
	 * @throws IllegalStateException If a bus is already bound, or the policy does not allow {@code term.nudge:*}
	 * 	downstream unretained.
	 */
	public TerminalNudges bind(ServerBus bus) {
		if (bus == null)
			throw new IllegalArgumentException("TerminalNudges.bind: bus must not be null");
		synchronized (this) {
			if (this.bus != null)
				throw new IllegalStateException("TerminalNudges.bind: already bound");
			var retain = bus.policy().downstreamRetain(FAMILY + ":*");
			if (retain.isEmpty() || retain.get())
				throw new IllegalStateException("TerminalNudges.bind: the bus policy must allow '" + FAMILY + ":*' downstream without retention");
			// Started before the bus is visible: a nudge that sees the bus must find a running scheduler.
			scheduler.start();
			this.bus = bus;
		}
		return this;
	}

	/**
	 * Records that a terminal's bytes now end at an offset; delivery is coalesced and happens on the scheduler thread.
	 *
	 * <p>
	 * Dropped silently before {@link #bind(ServerBus)} and after {@link #close()}.
	 *
	 * @param terminalId The terminal id.
	 * @param end The offset one past the last byte.
	 * @param done <jk>true</jk> if no more bytes will be appended.
	 * @throws IllegalArgumentException If the id is not a valid run id.
	 */
	@Override /* TerminalNudgePublisher */
	public void nudge(String terminalId, long end, boolean done) {
		topic(terminalId);
		synchronized (this) {
			if (bus == null || closed)
				return;
			var now = clock.getAsLong();
			var st = states.computeIfAbsent(terminalId, k -> new State());
			st.end = Math.max(st.end, end);
			st.done |= done;
			st.lastTouched = now;
			if (! st.scheduled) {
				st.scheduled = true;
				var delay = st.lastSentAt == Long.MIN_VALUE ? 0 : Math.max(0, st.lastSentAt + WINDOW_MS - now);
				scheduler.schedule(guarded(() -> flush(terminalId)), delay);
			}
		}
	}

	/**
	 * Stops the scheduler.  Flushes already queued, and later nudges, deliver nothing.  Idempotent.
	 */
	@Override /* AutoCloseable */
	public void close() {
		synchronized (this) {
			if (closed)
				return;
			closed = true;
			states.clear();
			lastWarned.clear();
		}
		scheduler.close();
	}

	/** The number of terminals with state held; for tests. */
	synchronized int pendingTerminals() {
		return states.size();
	}

	/** The number of terminals with a rate-limited warning held; for tests. */
	synchronized int warnedTerminals() {
		return lastWarned.size();
	}

	private Runnable guarded(Runnable r) {
		return () -> {
			try {
				r.run();
			} catch (RuntimeException e) {
				LOG.log(Level.WARNING, e, () -> "Terminal nudge task failed.");
			}
		};
	}

	private void flush(String id) {
		ServerBus b;
		long end;
		boolean done;
		synchronized (this) {
			if (closed)
				return;
			var st = states.get(id);
			if (st == null)
				return;
			// Reset first, so a failure below cannot leave the terminal marked as scheduled forever.
			st.scheduled = false;
			st.lastSentAt = clock.getAsLong();
			b = bus;
			end = st.end;
			done = st.done;
			st.done = false;
		}
		var delivered = false;
		try {
			deliver(b, id, end, done);
			delivered = true;
		} finally {
			synchronized (this) {
				var st = states.get(id);
				if (st != null && ! closed) {
					if (! delivered) {
						if (st.scheduled) {
							// A later nudge is already queued: it carries the done report.
							st.done |= done;
						} else {
							// No follow-up: drop the state; polling finishes the card.
							states.remove(id);
							lastWarned.remove(id);
						}
					} else if (done) {
						// A nudge right after a done delivery starts a fresh state and skips the 250 ms window (allowed).
						if (! st.scheduled) {
							states.remove(id);
							lastWarned.remove(id);
						}
					} else if (! st.cleanupPending) {
						st.cleanupPending = true;
						scheduler.schedule(guarded(() -> cleanup(id)), CLEANUP_MS);
					}
				}
			}
		}
	}

	private void cleanup(String id) {
		synchronized (this) {
			var st = states.get(id);
			if (st == null || closed)
				return;
			st.cleanupPending = false;
			var age = clock.getAsLong() - st.lastTouched;
			if (st.scheduled)
				return;
			if (age >= CLEANUP_MS) {
				states.remove(id);
				lastWarned.remove(id);
			} else {
				st.cleanupPending = true;
				scheduler.schedule(guarded(() -> cleanup(id)), CLEANUP_MS - age);
			}
		}
	}

	private void deliver(ServerBus b, String id, long end, boolean done) {
		var topic = topic(id);
		var allowed = Collections.newSetFromMap(new IdentityHashMap<BusSession,Boolean>());
		for (var s : b.sessions()) {
			if (! s.isConnected() || ! s.downstream().contains(topic))
				continue;
			try {
				if (access.canWatch(new SessionCaller(s), id))
					allowed.add(s);
			} catch (RuntimeException e) {
				warnDenied(id, e);
			}
		}
		if (allowed.isEmpty())
			return;
		var payload = done ? JsonMap.of("end", end, "done", true) : JsonMap.of("end", end);
		try {
			b.publishTo(allowed::contains, topic, payload);
		} catch (IllegalArgumentException e) {
			LOG.log(Level.WARNING, e, () -> "Terminal nudge for '" + id + "' was refused by the bus.");
		}
	}

	private void warnDenied(String id, RuntimeException e) {
		var now = clock.getAsLong();
		synchronized (this) {
			var last = lastWarned.get(id);
			if (last != null && now - last < WARN_MS)
				return;
			lastWarned.put(id, now);
		}
		LOG.log(Level.WARNING, e, () -> "TerminalAccess.canWatch failed for '" + id + "'; the session gets no nudge.");
	}

	/** The default scheduler: one daemon thread. */
	private static final class ThreadScheduler implements Scheduler {
		private ScheduledExecutorService executor;

		@Override public synchronized void start() {
			if (executor != null)
				return;
			var ex = new ScheduledThreadPoolExecutor(1, r -> {
				var t = new Thread(r, "juneau-terminal-nudges");
				t.setDaemon(true);
				return t;
			});
			ex.setRemoveOnCancelPolicy(true);
			ex.prestartAllCoreThreads();
			executor = ex;
		}

		@Override public synchronized void schedule(Runnable r, long delayMs) {
			if (executor == null)
				return;
			try {
				executor.schedule(r, delayMs, TimeUnit.MILLISECONDS);
			} catch (RejectedExecutionException e) {
				// Closed concurrently: the nudge is dropped.
			}
		}

		@Override public synchronized void close() {
			if (executor != null)
				executor.shutdownNow();
		}
	}
}
