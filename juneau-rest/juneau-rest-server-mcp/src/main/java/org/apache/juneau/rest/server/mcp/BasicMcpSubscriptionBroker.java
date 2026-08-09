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

import java.util.Optional;
import java.util.concurrent.*;

/**
 * Default {@link McpSubscriptionBroker}: a {@link ConcurrentHashMap}-backed registry doing filtered fan-out.
 *
 * <p>
 * Publish methods ({@link #resourceUpdated}, {@link #toolsListChanged}, {@link #promptsListChanged},
 * {@link #resourcesListChanged}) build one {@link McpChangeEvent} and iterate the live registry, offering the
 * event only to subscriptions whose {@link McpSubscriptionFilter#matches(McpChangeEvent)} returns
 * {@code true} — this never broadcasts unconditionally the way
 * {@code org.apache.juneau.rest.server.sse.SseBroadcaster#publish} does.
 */
public class BasicMcpSubscriptionBroker implements McpSubscriptionBroker {

	private final int queueSize;
	private final ConcurrentMap<String,BasicMcpSubscription> subscriptions = new ConcurrentHashMap<>();
	// Guards the check-then-mutate window in registerIfUnder only; plain register() is intentionally
	// uncapped and does not synchronize on this (see the interface javadoc for the split rationale).
	private final Object admissionLock = new Object();

	/**
	 * Creates a new broker.
	 *
	 * @param queueSize The per-subscription drop-oldest queue bound. Must be {@code > 0}.
	 * @throws IllegalArgumentException If {@code queueSize} is not {@code > 0}.
	 */
	public BasicMcpSubscriptionBroker(int queueSize) {
		if (queueSize <= 0)
			throw iaex("queueSize %s must be > 0", queueSize);
		this.queueSize = queueSize;
	}

	@Override
	@SuppressWarnings({
		"resource" // Returned subscription is caller-owned and closed by the caller/framework; Eclipse JDT @Owning warning is by design.
	})
	public McpSubscription register(String subscriptionId, McpSubscriptionFilter honoredFilter) {
		if (isEmpty(subscriptionId))
			throw iaex("subscriptionId must not be null or empty");
		if (honoredFilter == null)
			throw iaex("honoredFilter must not be null");
		return doRegister(subscriptionId, honoredFilter);
	}

	@Override
	public Optional<McpSubscription> registerIfUnder(int max, String subscriptionId, McpSubscriptionFilter honoredFilter) {
		if (isEmpty(subscriptionId))
			throw iaex("subscriptionId must not be null or empty");
		if (honoredFilter == null)
			throw iaex("honoredFilter must not be null");
		// The size check and the put must be indivisible from the perspective of concurrent registerIfUnder
		// callers, or two racers can each observe activeCount() < max and both register, exceeding the cap.
		synchronized (admissionLock) {
			if (! subscriptions.containsKey(subscriptionId) && subscriptions.size() >= max)
				return Optional.empty();
			@SuppressWarnings({
				"resource" // Returned subscription is caller-owned and closed by the caller/framework; Eclipse JDT @Owning warning is by design.
			})
			var sub = doRegister(subscriptionId, honoredFilter);
			return Optional.of(sub);
		}
	}

	@SuppressWarnings({
		"resource" // Returned subscription is caller-owned and closed by the caller/framework; Eclipse JDT @Owning warning is by design.
	})
	private McpSubscription doRegister(String subscriptionId, McpSubscriptionFilter honoredFilter) {
		// The close callback removes this exact instance only (via the two-arg conditional ConcurrentMap.remove),
		// so closing a stale/replaced subscription can never evict the fresher one that has since replaced it.
		var sub = new BasicMcpSubscription(subscriptionId, honoredFilter, queueSize,
			closed -> subscriptions.remove(subscriptionId, closed));
		var previous = subscriptions.put(subscriptionId, sub);
		if (previous != null)
			previous.close();
		return sub;
	}

	@Override
	public void unregister(String subscriptionId) {
		if (subscriptionId == null)
			throw iaex("subscriptionId must not be null");
		var sub = subscriptions.remove(subscriptionId);
		if (sub != null)
			sub.close();
	}

	@Override
	public int activeCount() {
		return subscriptions.size();
	}

	@Override
	public void resourceUpdated(String uri) {
		if (uri == null)
			throw iaex("uri must not be null");
		publish(new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, uri));
	}

	@Override
	public void toolsListChanged() {
		publish(new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null));
	}

	@Override
	public void promptsListChanged() {
		publish(new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null));
	}

	@Override
	public void resourcesListChanged() {
		publish(new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null));
	}

	private void publish(McpChangeEvent event) {
		for (var sub : subscriptions.values())
			if (sub.getFilter().matches(event))
				sub.offer(event);
	}
}
