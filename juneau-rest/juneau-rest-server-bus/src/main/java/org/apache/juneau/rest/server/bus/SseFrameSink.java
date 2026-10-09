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

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.sse.*;

/**
 * The SSE transport's {@link FrameSink}: an adapter over the session's {@link SseSubscription} (from
 * {@link ServerBus#subscribeSse(BusSession)}), drained by {@link BusEventsMixin#streamBus} on the request thread.
 *
 * <p>
 * Each frame becomes one {@code event: bus} {@link SseEvent}, with the frame's {@code seq} (when it has one) as its
 * id.  The sink refuses a frame once {@code max} are pending, so the bus closes it as a slow consumer.  The
 * subscription's queue is one larger than {@code max}, so the subscription's own drop-oldest overflow never runs and a
 * retained update is never silently lost.
 *
 * <p>
 * {@code 4429} (slow consumer), {@code 4409} (replaced) and {@link #release()} close the subscription, which drops
 * the backlog.  After {@code 4429} the stream writes one {@code error bus:slow-consumer} frame as its last event (spec
 * §11.6).  {@code 1001} (bus closed) and {@code 4401} (expired) let pending frames drain first.  When another stream
 * re-subscribes the session (one connection per session), this sink ends quietly, as if replaced.
 */
final class SseFrameSink implements FrameSink {

	/** Ends a draining close.  Compared by identity and never written. */
	private static final SseEvent END = new SseEvent(BusFrames.EVENT, "");

	private static final String SLOW_CONSUMER = BusFrames.error("bus:slow-consumer", "the connection fell behind", null);

	private final SseSubscription subscription;
	private final int max;
	private final AtomicInteger pending = new AtomicInteger();
	private final AtomicInteger closeCode = new AtomicInteger();
	private boolean done;   // Consumer thread only.

	/**
	 * @param subscription The session's subscription.  Its queue must hold at least {@code max + 1} events.
	 * @param max The most frames that may be pending ({@link BusPolicy#maxQueuedFrames()}).
	 */
	SseFrameSink(SseSubscription subscription, int max) {
		this.subscription = Objects.requireNonNull(subscription, "subscription");
		this.max = max;
	}

	@Override /* FrameSink */
	public boolean offer(String frame) {
		if (closeCode.get() != 0)
			return false;
		if (subscription.isClosed()) {
			// Another stream re-subscribed this session: end quietly, as a replaced connection does.
			closeCode.compareAndSet(0, 4409);
			return false;
		}
		if (pending.incrementAndGet() > max) {
			pending.decrementAndGet();
			return false;
		}
		subscription.offer(toEvent(frame));
		return true;
	}

	@Override /* FrameSink */
	public void close(int code, String reason) {
		if (! closeCode.compareAndSet(0, code))
			return;
		if (code == 1001 || code == 4401)
			subscription.offer(END);
		else
			subscription.close();
	}

	/**
	 * Waits for the next event to write.
	 *
	 * @param timeout How long to wait.
	 * @return The event, or <jk>null</jk> on timeout or once the sink has ended (see {@link #isDone()}).  After a
	 * 	{@code 4429} close, the one {@code error bus:slow-consumer} event comes first.
	 * @throws InterruptedException If interrupted while waiting.
	 */
	SseEvent poll(Duration timeout) throws InterruptedException {
		if (done)
			return null;
		var e = subscription.poll(timeout);
		if (e != null && e != END) {
			pending.decrementAndGet();
			return e;
		}
		if (e == null && ! subscription.isClosed())
			return null;
		done = true;
		subscription.close();
		return closeCode.get() == 4429 ? toEvent(SLOW_CONSUMER) : null;
	}

	/** @return <jk>true</jk> once the sink has ended and its last event has been taken. */
	boolean isDone() {
		return done;
	}

	/** Ends the sink from the stream's side (the response ended).  Safe to call more than once. */
	void release() {
		closeCode.compareAndSet(0, 1000);
		subscription.close();
	}

	private static SseEvent toEvent(String frame) {
		var event = new SseEvent(BusFrames.EVENT, frame);
		var seq = BusFrames.seq(frame);
		if (seq.isPresent())
			event.setId(String.valueOf(seq.getAsLong()));
		return event;
	}
}
