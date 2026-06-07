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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.sse.*;

/**
 * Subscriber queue for an {@link SseBroadcaster}.
 */
public class SseSubscription implements AutoCloseable, Iterable<SseEvent> {

	private final String id;
	private final LinkedBlockingDeque<SseEvent> queue;
	private final AtomicBoolean closed;
	private final Consumer<String> closeCallback;

	SseSubscription(String id, int queueSize, Consumer<String> closeCallback) {
		if (isEmpty(id))
			throw illegalArg("id cannot be null or empty.");
		this.id = id;
		this.queue = new LinkedBlockingDeque<>(queueSize);
		this.closeCallback = assertArgNotNull("closeCallback", closeCallback);
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

	boolean offer(SseEvent event) {
		if (isClosed())
			return false;
		var dropped = false;
		while (! queue.offerLast(event)) {
			queue.pollFirst();
			dropped = true;
		}
		return dropped;
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
