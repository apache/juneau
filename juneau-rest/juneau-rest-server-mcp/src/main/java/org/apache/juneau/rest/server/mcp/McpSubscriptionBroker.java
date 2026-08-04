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

import java.util.Optional;

/**
 * Revision-neutral registry + filtered fan-out publisher for MCP {@code subscriptions/listen}.
 *
 * <p>
 * Extends {@link McpSubscriptions} (the publish half) and adds registration/lifecycle: a v2 dispatch branch
 * calls {@link #registerIfUnder(int, String, McpSubscriptionFilter)} once per accepted
 * {@code subscriptions/listen} request. Close (graceful or abrupt) does <b>not</b> flow back through this
 * interface: the returned {@link McpSubscription#close()} instead runs a close-callback wired at
 * registration time, which removes the entry directly from the registry the returned handle's key was
 * minted under. {@link #unregister(String)} is a registry-completeness API for that same by-id removal —
 * it has no current production caller, since the close-callback path never has to look the id back up.
 * Application handlers reach this same instance only through the narrower {@link McpSubscriptions} view.
 *
 * <p>
 * Plain {@link #register(String, McpSubscriptionFilter)} is uncapped — it never rejects, so a caller wanting
 * an enforced {@code maxConcurrentSubscriptions} ceiling must use {@link #registerIfUnder(int, String,
 * McpSubscriptionFilter)} instead, which admission-checks and mutates the registry as one atomic step. A
 * separate "check {@link #activeCount()}, then call {@code register}" pair is exactly the TOCTOU race this
 * capped variant exists to close: two concurrent callers can both observe a count under the cap and both
 * register before either mutation is visible to the other.
 */
public interface McpSubscriptionBroker extends McpSubscriptions {

	/**
	 * Registers a new subscription and returns its queue handle. Uncapped: never rejects.
	 *
	 * @param subscriptionId The subscription identifier (the listen request's JSON-RPC id). Must not be
	 * 	{@code null} or empty.
	 * @param honoredFilter The capability-gated filter this subscription honors. Must not be {@code null}.
	 * @return The new subscription handle. Never {@code null}.
	 */
	McpSubscription register(String subscriptionId, McpSubscriptionFilter honoredFilter);

	/**
	 * Atomically admission-checks against {@code max} and registers, closing the TOCTOU window a separate
	 * {@link #activeCount()} check plus {@link #register} call would leave open under concurrent callers
	 * (in particular against a JVM-wide shared broker instance, where unrelated callers race each other).
	 *
	 * <p>
	 * Re-registering an id that is already present is always allowed and never counted against {@code max},
	 * matching {@link #register}'s replace-on-same-id semantics (the previous subscription with that id is
	 * closed) — a replacement does not grow {@link #activeCount()}, so it cannot be what pushes the registry
	 * over the cap.
	 *
	 * @param max The maximum number of concurrently active subscriptions to admit under (exclusive: a
	 * 	request is admitted only while {@link #activeCount()} is strictly less than this, except for the
	 * 	same-id replacement case above).
	 * @param subscriptionId The subscription identifier (the listen request's JSON-RPC id). Must not be
	 * 	{@code null} or empty.
	 * @param honoredFilter The capability-gated filter this subscription honors. Must not be {@code null}.
	 * @return The new subscription handle, or {@link Optional#empty()} if the cap was reached and this was
	 * 	not a same-id replacement.
	 */
	Optional<McpSubscription> registerIfUnder(int max, String subscriptionId, McpSubscriptionFilter honoredFilter);

	/**
	 * Deregisters and closes the subscription with the given id, if present.
	 *
	 * @param subscriptionId The subscription identifier. A missing id is a no-op.
	 */
	void unregister(String subscriptionId);

	/**
	 * The number of currently registered (not yet closed) subscriptions.
	 *
	 * @return The active count.
	 */
	int activeCount();
}
