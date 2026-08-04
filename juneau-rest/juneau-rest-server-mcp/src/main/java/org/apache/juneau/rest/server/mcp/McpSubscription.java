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

/**
 * Per-subscription queue handle minted by an {@link McpSubscriptionBroker}.
 *
 * <p>
 * Deliberately named differently from the client-facing {@code McpSubscriptionHandle} (a distinct type in a
 * separate client module/package): this is the server-side, broker-internal handle a v2 dispatch branch
 * drains via {@link #take()} to serialize change events onto its own stream.
 *
 * <p>
 * Owns its own bounded, drop-oldest queue — it does <b>not</b> reuse
 * {@code org.apache.juneau.rest.server.sse.SseSubscription}, whose constructor and {@code offer(...)} method
 * are package-private to a different package. Only one thread should call {@link #take()} concurrently on a
 * given instance (matching the production single-pump-per-subscription usage): {@link #close()}'s contract
 * of interrupting a blocked {@link #take()} tracks a single in-flight taker thread.
 */
public interface McpSubscription extends AutoCloseable {

	/**
	 * The subscription identifier (the originating {@code subscriptions/listen} request's JSON-RPC id).
	 *
	 * @return The identifier. Never {@code null} or empty.
	 */
	String getId();

	/**
	 * The honored filter this subscription was registered with.
	 *
	 * @return The filter. Never {@code null}.
	 */
	McpSubscriptionFilter getFilter();

	/**
	 * Blocks until the next matching change event is available.
	 *
	 * <p>
	 * Calling this on a subscription that is already closed throws {@link InterruptedException} immediately
	 * rather than blocking — load-bearing behavior for a pump's graceful-completion path, which relies on a
	 * closed-before-called {@link #take()} failing fast instead of hanging forever waiting for an event that
	 * will never arrive.
	 *
	 * @return The next event.
	 * @throws InterruptedException If the wait was interrupted, including by a concurrent {@link #close()},
	 * 	or if this subscription was already closed before this call.
	 */
	McpChangeEvent take() throws InterruptedException;

	/**
	 * Returns whether this subscription has been closed.
	 *
	 * @return {@code true} if closed.
	 */
	boolean isClosed();

	/**
	 * Closes this subscription: idempotent, deregisters from the broker that minted it, and interrupts a
	 * thread currently blocked in {@link #take()}.
	 */
	@Override
	void close();
}
