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
package org.apache.juneau.rest.sse;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.sse.*;

/**
 * In-memory server-side SSE broadcaster.
 */
public class SseBroadcaster {

	private static final Logger LOGGER = Logger.getLogger(SseBroadcaster.class.getName());
	private static final int DEFAULT_QUEUE_SIZE = 1024;

	/**
	 * Creates a broadcaster with the default queue size.
	 *
	 * @return A new broadcaster.
	 */
	public static SseBroadcaster create() {
		return new SseBroadcaster(DEFAULT_QUEUE_SIZE);
	}

	private final int queueSize;
	private final ConcurrentMap<String,SseSubscription> subscriptions;

	/**
	 * Constructor.
	 *
	 * @param queueSize The per-subscriber queue size.
	 */
	public SseBroadcaster(int queueSize) {
		if (queueSize <= 0)
			throw illegalArg("queueSize must be greater than 0.");
		this.queueSize = queueSize;
		subscriptions = new ConcurrentHashMap<>();
	}

	/**
	 * Creates or replaces a subscriber.
	 *
	 * @param id The subscriber identifier.
	 * @return A new subscription.
	 */
	@SuppressWarnings({
		"resource" // Returned subscription is caller-owned and closed by the caller/framework.
	})
	public SseSubscription subscribe(String id) {
		if (isEmpty(id))
			throw illegalArg("id cannot be null or empty.");
		var subscription = new SseSubscription(id, queueSize, this::removeSubscriber);
		var previous = subscriptions.put(id, subscription);
		if (previous != null)
			previous.close();
		return subscription;
	}

	/**
	 * Publishes an event to all active subscribers.
	 *
	 * @param event The event to publish.
	 */
	public void publish(SseEvent event) {
		if (event == null)
			return;
		subscriptions.values().forEach(x -> {
			if (x.offer(event))
				LOGGER.fine(() -> "SSE queue overflow for subscriber " + x.getId() + ", dropped oldest event.");
		});
	}

	void removeSubscriber(String id) {
		var removed = subscriptions.remove(id);
		if (removed != null && ! removed.isClosed())
			removed.close();
	}
}
