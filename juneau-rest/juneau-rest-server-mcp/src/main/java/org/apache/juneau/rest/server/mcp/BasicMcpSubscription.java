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
package org.apache.juneau.rest.server.mcp;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Default {@link McpSubscription} implementation: a bounded, drop-oldest queue backed by a
 * {@link LinkedBlockingDeque}, replicating {@code SseSubscription}'s exact drop-oldest semantics
 * (package-private there, so re-implemented here rather than reused).
 */
final class BasicMcpSubscription implements McpSubscription {

	private final String id;
	private final McpSubscriptionFilter filter;
	private final LinkedBlockingDeque<McpChangeEvent> queue;
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final AtomicReference<Thread> waitingThread = new AtomicReference<>();
	private final Consumer<BasicMcpSubscription> closeCallback;
	// Guards the "check-closed / register-waiter" step in take() and the "flip-closed / read-waiter +
	// interrupt" step in close() as a single atomic unit, so a close() can never land in the window between
	// take()'s closed-check and its waitingThread registration and interrupt nobody (lost-interrupt hang).
	private final Object lock = new Object();
	// Serializes the drop-oldest retry loop in offer() across concurrent producers, so one producer's
	// pollFirst()/offerLast() retry can't interleave with another's and evict a just-inserted event instead
	// of the true oldest one. Deliberately does not also guard take()/takeFirst() - only producers contend here.
	private final Object offerLock = new Object();

	BasicMcpSubscription(String id, McpSubscriptionFilter filter, int queueSize,
			Consumer<BasicMcpSubscription> closeCallback) {
		if (isEmpty(id))
			throw iaex("id must not be null or empty");
		if (filter == null)
			throw iaex("filter must not be null");
		if (closeCallback == null)
			throw iaex("closeCallback must not be null");
		if (queueSize <= 0)
			throw iaex("queueSize %s must be > 0", queueSize);
		this.id = id;
		this.filter = filter;
		this.queue = new LinkedBlockingDeque<>(queueSize);
		this.closeCallback = closeCallback;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public McpSubscriptionFilter getFilter() {
		return filter;
	}

	/**
	 * Offers an event to this subscription's queue, dropping the oldest queued event if full.
	 *
	 * @param event The event to offer.
	 * @return {@code true} if an existing queued event was dropped to make room; {@code false} if the queue
	 * 	had room, or if this subscription is already closed (in which case the event is discarded).
	 */
	boolean offer(McpChangeEvent event) {
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

	@Override
	public McpChangeEvent take() throws InterruptedException {
		synchronized (lock) {
			if (closed.get())
				throw new InterruptedException("subscription closed");
			waitingThread.set(Thread.currentThread());
		}
		try {
			return queue.takeFirst();
		} finally {
			waitingThread.compareAndSet(Thread.currentThread(), null);
		}
	}

	@Override
	public boolean isClosed() {
		return closed.get();
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			synchronized (lock) {
				queue.clear();
				var t = waitingThread.get();
				if (t != null)
					t.interrupt();
			}
			closeCallback.accept(this);
		}
	}
}
