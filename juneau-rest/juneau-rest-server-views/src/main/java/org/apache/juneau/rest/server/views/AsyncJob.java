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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.rest.server.views.ActionResult.*;

/**
 * A single in-memory, one-shot async job &mdash; the streamed-progress + terminal-result unit of a future async write
 * variant (design doc §6.3).
 *
 * <h5 class='section'>The stream URL IS the capability</h5>
 * <p>
 * A job is addressed by its {@link #id() id}, an unguessable {@value AsyncJobRegistry#CAPABILITY_BITS}-bit
 * {@link java.security.SecureRandom} token minted by the {@link AsyncJobRegistry}, matching
 * {@link org.apache.juneau.rest.server.filter.SynchronizerToken}'s bar.  Because a browser {@code EventSource}
 * constructor cannot set an {@code X-Csrf-Token} header and cookies are unsound on a loopback port, the SSE GET that
 * streams a job's progress is Host-checked at the boundary and <b>capability-gated by the unguessability of the
 * id</b> &mdash; there is no session cookie and no CSRF-on-GET (HIGH-4).  Consequently the id is a secret:
 * {@link #toString()} never renders it.
 *
 * <h5 class='section'>Server-side hard limits (MED-6, a Task-11 disclosure bound)</h5>
 * <p>
 * A job caps its own streamed output at {@link #maxOutputBytes} bytes and terminates itself when that is exceeded,
 * and it terminates itself once past its {@link #deadline() hard-timeout deadline}.  Both are enforced <b>here</b>,
 * server-side &mdash; a client cannot be trusted to cancel &mdash; and both are disclosure bounds, not only heap
 * bounds: they cap how long, and how much, customer-adjacent triage content is streamed.  The number of concurrent
 * subscribers is capped at {@link #maxSubscribers} (a browser reload plus one straggler), enforced through
 * {@link #acquireSubscriber()} / {@link #releaseSubscriber()}.
 *
 * <h5 class='section'>Terminal outcomes reuse the frozen {@link ActionResult} contract</h5>
 * <p>
 * A job settles exactly once, to an {@link ActionResult}: a producer-supplied {@link #complete(ActionResult)
 * success/failure/refusal/unknown}, a {@link #cancel() cancellation} (distinguishing {@link Outcome#CANCELLED
 * cancelled} from {@link Outcome#CANCELLED_AFTER_EFFECT cancelled-after-effect} via
 * {@link #markEffectStarted()}), an output-limit termination, or a timeout.  The reserved async outcomes are read
 * off the frozen {@link Outcome} enum via its public {@code outcome} field &mdash; this contract is reused, never
 * re-versioned.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread-safe: a producer thread appends progress and settles the job while
 * 		subscriber threads drain it through {@link #awaitUpdate(int, Duration)}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AsyncJobRegistry}
 * 	<li class='jc'>{@link AsyncJobsMixin}
 * 	<li class='jc'>{@link ActionResult}
 * </ul>
 *
 * @since 10.0.0
 */
public final class AsyncJob {

	private final String id;
	private final Instant createdAt;
	private final Duration timeout;
	private final long maxOutputBytes;
	private final int maxSubscribers;

	private final Object lock = new Object();
	private final List<String> events = new ArrayList<>();
	private long outputBytes;
	private boolean effectStarted;
	private int subscribers;
	private ActionResult result;   // Non-null iff terminal; the single settled outcome.

	/**
	 * Constructor.
	 *
	 * @param id The unguessable capability id.  Must not be <jk>null</jk> or blank.
	 * @param createdAt The instant the job was created (for the hard-timeout deadline).  Must not be <jk>null</jk>.
	 * @param timeout The hard timeout after which the job self-terminates.  Must not be <jk>null</jk>.
	 * @param maxOutputBytes The maximum number of streamed output bytes before the job self-terminates.
	 * @param maxSubscribers The maximum number of concurrent subscribers.
	 */
	AsyncJob(String id, Instant createdAt, Duration timeout, long maxOutputBytes, int maxSubscribers) {
		this.id = assertArgNotNull("id", id);
		this.createdAt = assertArgNotNull("createdAt", createdAt);
		this.timeout = assertArgNotNull("timeout", timeout);
		this.maxOutputBytes = maxOutputBytes;
		this.maxSubscribers = maxSubscribers;
	}

	/**
	 * The unguessable capability id &mdash; the whole of the job's access control (see the class javadoc).
	 *
	 * @return The id.  Never <jk>null</jk> or blank.
	 */
	public String id() { return id; }

	/** @return The instant this job was created. */
	public Instant createdAt() { return createdAt; }

	/** @return The instant past which this job self-terminates on the next timeout check. */
	public Instant deadline() { return createdAt.plus(timeout); }

	/** @return The total number of streamed output bytes so far. */
	public long outputBytes() { synchronized (lock) { return outputBytes; } }

	/** @return The maximum streamed output bytes before self-termination. */
	public long maxOutputBytes() { return maxOutputBytes; }

	/** @return The maximum number of concurrent subscribers. */
	public int maxSubscribers() { return maxSubscribers; }

	/**
	 * Appends a progress event.
	 *
	 * <p>
	 * A no-op once the job is terminal.  Enforces the server-side output cap: if this event would push the total
	 * over {@link #maxOutputBytes}, the job is instead terminated with a {@link Outcome#FAILURE failure} carrying an
	 * {@code app:output-limit-exceeded} message and the event is dropped &mdash; the cap is a data-egress bound, so
	 * the excess content is never streamed.
	 *
	 * @param text The progress text.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the event was appended; <jk>false</jk> if the job was already terminal or the output
	 * 	cap was hit (which itself terminates the job).
	 */
	public boolean progress(String text) {
		assertArgNotNull("text", text);
		synchronized (lock) {
			if (result != null)
				return false;
			var add = (long) text.getBytes(UTF_8).length;
			if (outputBytes + add > maxOutputBytes) {
				settle(ActionResult.failure().message("app:output-limit-exceeded"));
				return false;
			}
			outputBytes += add;
			events.add(text);
			lock.notifyAll();
			return true;
		}
	}

	/**
	 * Marks that this job's outbound effect has begun, so a subsequent {@link #cancel()} settles to
	 * {@link Outcome#CANCELLED_AFTER_EFFECT cancelled-after-effect} rather than {@link Outcome#CANCELLED cancelled}
	 * (Q4: the two are different outcomes and must not be collapsed).  A no-op once terminal.
	 */
	public void markEffectStarted() {
		synchronized (lock) {
			if (result == null)
				effectStarted = true;
		}
	}

	/** @return Whether the outbound effect has begun (see {@link #markEffectStarted()}). */
	public boolean effectStarted() { synchronized (lock) { return effectStarted; } }

	/**
	 * Settles the job to a producer-supplied terminal result (success/failure/refusal/unknown).
	 *
	 * @param value The terminal result.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if this call settled the job; <jk>false</jk> if it was already terminal (first settle
	 * 	wins).
	 */
	public boolean complete(ActionResult value) {
		assertArgNotNull("value", value);
		synchronized (lock) {
			if (result != null)
				return false;
			settle(value);
			return true;
		}
	}

	/**
	 * Cancels the job, settling it to {@link Outcome#CANCELLED cancelled} &mdash; or
	 * {@link Outcome#CANCELLED_AFTER_EFFECT cancelled-after-effect} when {@link #markEffectStarted()} was called
	 * first.  Enforced server-side; a client cannot be trusted to stop the work itself.
	 *
	 * @return <jk>true</jk> if this call settled the job; <jk>false</jk> if it was already terminal.
	 */
	public boolean cancel() {
		synchronized (lock) {
			if (result != null)
				return false;
			settle(cancelledResult(effectStarted));
			return true;
		}
	}

	/**
	 * Terminates the job if it is past its {@link #deadline() hard-timeout deadline}, settling it to a non-optimistic
	 * {@link Outcome#UNKNOWN unknown} &mdash; the effect may or may not have completed in the time cut off.  This is
	 * the server-side timeout enforcement (HIGH-4/MED-6): it bounds both heap and, as a Task-11 disclosure bound, how
	 * long triage keeps streaming customer-adjacent content.
	 *
	 * @param now The current instant.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if this call timed the job out; <jk>false</jk> if it was already terminal or not yet past
	 * 	its deadline.
	 */
	public boolean enforceTimeout(Instant now) {
		assertArgNotNull("now", now);
		synchronized (lock) {
			if (result != null || ! now.isAfter(deadline()))
				return false;
			settle(ActionResult.unknown().message("app:hard-timeout-exceeded"));
			return true;
		}
	}

	/** @return Whether the job has settled to a terminal result. */
	public boolean isTerminal() { synchronized (lock) { return result != null; } }

	/** @return The terminal result, or <jk>null</jk> if the job is still running. */
	public ActionResult result() { synchronized (lock) { return result; } }

	/** @return The number of progress events buffered so far. */
	public int eventCount() { synchronized (lock) { return events.size(); } }

	/**
	 * Snapshots the progress events at or after {@code fromIndex} (for a re-attaching subscriber that replays
	 * buffered progress on page reload, per Q3).
	 *
	 * @param fromIndex The zero-based index to start from.
	 * @return A copy of the events from {@code fromIndex} onward (empty if none/out of range).
	 */
	public List<String> eventsFrom(int fromIndex) {
		synchronized (lock) {
			if (fromIndex < 0 || fromIndex >= events.size())
				return List.of();
			return List.copyOf(events.subList(fromIndex, events.size()));
		}
	}

	/**
	 * Blocks until there is a progress event at or after {@code fromIndex}, or the job is terminal, or {@code timeout}
	 * elapses &mdash; then returns whichever is available.
	 *
	 * <p>
	 * A terminal job returns immediately with any remaining buffered events plus its terminal result, so a single
	 * subscriber loop drains a completed (or re-attached) job in one pass.
	 *
	 * @param fromIndex The zero-based index the caller has already consumed up to.
	 * @param timeout The maximum time to wait for a new event.  Must not be <jk>null</jk>.
	 * @return The new events (possibly empty) and the terminal result (<jk>null</jk> while still running).
	 */
	public Update awaitUpdate(int fromIndex, Duration timeout) {
		assertArgNotNull("timeout", timeout);
		var deadlineNanos = System.nanoTime() + Math.max(0L, timeout.toNanos());
		synchronized (lock) {
			while (true) {
				if (fromIndex < events.size() || result != null) {
					var newEvents = fromIndex < events.size()
						? List.copyOf(events.subList(Math.max(0, fromIndex), events.size()))
						: List.<String>of();
					return new Update(newEvents, result);
				}
				var remaining = deadlineNanos - System.nanoTime();
				if (remaining <= 0L)
					return new Update(List.of(), null);
				try {
					lock.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return new Update(List.of(), result);
				}
			}
		}
	}

	/**
	 * Attempts to register a subscriber, enforcing the {@link #maxSubscribers} cap.
	 *
	 * @return <jk>true</jk> if a subscriber slot was acquired; <jk>false</jk> if the cap is already reached.
	 */
	public boolean acquireSubscriber() {
		synchronized (lock) {
			if (subscribers >= maxSubscribers)
				return false;
			subscribers++;
			return true;
		}
	}

	/** Releases a subscriber slot previously acquired via {@link #acquireSubscriber()}. */
	public void releaseSubscriber() {
		synchronized (lock) {
			if (subscribers > 0)
				subscribers--;
		}
	}

	/** @return The current number of registered subscribers. */
	public int subscriberCount() { synchronized (lock) { return subscribers; } }

	/** Settles the terminal result and wakes every waiting subscriber.  Caller must hold {@link #lock}. */
	private void settle(ActionResult value) {
		synchronized (lock) {
			result = value;
			lock.notifyAll();
		}
	}

	/** Builds a terminal {@link ActionResult} for a cancellation, reusing the frozen contract's reserved outcomes. */
	private static ActionResult cancelledResult(boolean afterEffect) {
		var r = new ActionResult();
		r.outcome = (afterEffect ? Outcome.CANCELLED_AFTER_EFFECT : Outcome.CANCELLED).wire();
		return r;
	}

	/**
	 * Returns a description that does <b>not</b> include the capability id.
	 *
	 * <p>
	 * The id is the stream's whole access control (see the class javadoc); a bean-dumping logger or debug view that
	 * stringified it would write a live capability somewhere it can be read back and used to attach to the stream.
	 *
	 * @return A value-free description.
	 */
	@Override /* Object */
	public String toString() {
		return "AsyncJob(id=<redacted>,terminal=" + isTerminal() + ")";
	}

	/**
	 * The result of {@link AsyncJob#awaitUpdate(int, Duration)}: the newly-available progress events and the terminal
	 * result (<jk>null</jk> while the job is still running).
	 *
	 * @param events The new progress events since the caller's cursor.  Never <jk>null</jk>.
	 * @param result The terminal result, or <jk>null</jk> if the job is still running.
	 */
	public record Update(List<String> events, ActionResult result) {}
}
