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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.marshall.sse.*;

/**
 * Subscriber queue for an {@link SseBroadcaster}.
 */
public class SseSubscription implements AutoCloseable, Iterable<SseEvent> {

	private final String id;
	private final LinkedBlockingDeque<SseEvent> queue;
	private final AtomicBoolean closed;
	private final Consumer<String> closeCallback;
	// Serializes the drop-oldest retry loop in offer() across concurrent producers, so one producer's
	// pollFirst()/offerLast() retry can't interleave with another's and evict a just-inserted event instead
	// of the true oldest one.
	private final Object offerLock = new Object();

	/** How often {@link #poll(Duration)} re-checks {@link #isClosed()} while it waits. */
	private static final long CLOSE_CHECK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

	SseSubscription(String id, int queueSize, Consumer<String> closeCallback) {
		if (isEmpty(id))
			throw iaex("id cannot be null or empty.");
		this.id = id;
		this.queue = new LinkedBlockingDeque<>(queueSize);
		this.closeCallback = reqnn("closeCallback", closeCallback);
		closed = new AtomicBoolean(false);
	}

	/**
	 * The subscriber identifier.
	 *
	 * @return The subscriber identifier.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns whether this subscription has been closed.
	 *
	 * @return {@code true} if this subscription has been closed.
	 */
	public boolean isClosed() {
		return closed.get();
	}

	/**
	 * Queues an event, dropping the oldest queued event when the queue is full.
	 *
	 * <p>
	 * Does nothing once the subscription is closed.  {@link SseBroadcaster#publish(SseEvent)} calls this for every
	 * subscriber; call it directly to address a single subscriber.
	 *
	 * @param event The event.  Must not be <jk>null</jk>.
	 * @return {@code true} if the oldest queued event was dropped to make room.
	 */
	public boolean offer(SseEvent event) {
		if (isClosed())
			return false;
		synchronized (offerLock) {
			var dropped = false;
			while (! queue.offerLast(event)) {
				queue.pollFirst();
				dropped = true;
			}
			return dropped;
		}
	}

	/**
	 * Blocks until the next event is available.
	 *
	 * @return The next event.
	 * @throws InterruptedException If the wait was interrupted.
	 */
	public SseEvent take() throws InterruptedException {
		return queue.takeFirst();
	}

	/**
	 * Waits up to a timeout for the next event.
	 *
	 * <p>
	 * Unlike {@link #take()}, returns once the subscription is closed (within about 50 ms of the close), so a caller
	 * whose subscription was replaced or closed from another thread never hangs.  The 50 ms bound applies to a waiting
	 * {@code poll}; {@link #take()} is not woken by {@link #close()}.
	 *
	 * @param timeout How long to wait.  Must not be <jk>null</jk>.
	 * @return The next event, or <jk>null</jk> on timeout or once the subscription is closed.
	 * @throws InterruptedException If the wait was interrupted.
	 */
	public SseEvent poll(Duration timeout) throws InterruptedException {
		reqnn("timeout", timeout);
		var deadline = System.nanoTime() + timeout.toNanos();
		while (! isClosed()) {
			var left = deadline - System.nanoTime();
			if (left <= 0)
				return null;
			var event = queue.pollFirst(Math.min(left, CLOSE_CHECK_NANOS), TimeUnit.NANOSECONDS);
			if (event != null)
				return event;
		}
		return null;
	}

	@Override /* Iterable */
	public Iterator<SseEvent> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return ! isClosed();
			}

			@Override
			public SseEvent next() {
				try {
					return take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new NoSuchElementException("Interrupted while waiting for SSE event.");
				}
			}
		};
	}

	@Override /* AutoCloseable */
	public void close() {
		if (closed.compareAndSet(false, true)) {
			queue.clear();
			closeCallback.accept(id);
		}
	}
}
