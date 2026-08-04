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
package org.apache.juneau.rest.server.mcp.v20260728;

/**
 * Binding-owned MCP {@code subscriptions/listen} (SEP-2575) configuration for a v2 {@link McpRevision}.
 *
 * <p>
 * Mirrors {@link McpMrtrConfig}/{@link McpCacheConfig}'s exact placement precedent: {@code subscriptions/listen}
 * is a {@code 2026-07-28}-only feature, so its configuration lives here, on the v2 adapter, never on the
 * revision-neutral {@code org.apache.juneau.rest.server.mcp.McpServerConfig}.
 *
 * <p>
 * <b>Mutable during setup, effectively immutable once published.</b> Builder-less mutable-setup bean,
 * following the same contract as {@link McpMrtrConfig}: configure fully before handing to a binding, never
 * mutate afterward.
 *
 * <p>
 * Unlike the {@code org.apache.juneau.rest.server.mcp.McpSubscriptionBroker} singleton (which holds live
 * per-connection state), this type is knobs-only — every field is a tunable numeric limit with no embedded
 * broker reference — so a servlet binding may memoize it with a plain lazy read (see
 * {@link McpRestServlet#getSubscriptionsConfig()}) rather than the MRTR-grade double-checked lock the broker
 * itself requires (see {@link McpRestServlet#getSubscriptionBroker()}).
 */
public class McpSubscriptionsConfig {

	/** Default cap on concurrently open {@code subscriptions/listen} streams per broker. */
	public static final int DEFAULT_MAX_CONCURRENT_SUBSCRIPTIONS = 256;

	/** Default per-subscription drop-oldest queue bound; matches {@code SseBroadcaster.DEFAULT_QUEUE_SIZE}. */
	public static final int DEFAULT_QUEUE_SIZE = 1024;

	/** Default heartbeat/keepalive interval in milliseconds. */
	public static final long DEFAULT_HEARTBEAT_INTERVAL_MS = 15_000L;

	/** Default idle timeout in milliseconds; {@code 0} disables idle-timeout reaping. */
	public static final long DEFAULT_IDLE_TIMEOUT_MS = 0L;

	private int maxConcurrentSubscriptions = DEFAULT_MAX_CONCURRENT_SUBSCRIPTIONS;
	private int queueSize = DEFAULT_QUEUE_SIZE;
	private long heartbeatIntervalMs = DEFAULT_HEARTBEAT_INTERVAL_MS;
	private long idleTimeoutMs = DEFAULT_IDLE_TIMEOUT_MS;

	/**
	 * The cap on concurrently open {@code subscriptions/listen} streams per broker.
	 *
	 * @return The cap. Always {@code > 0}.
	 */
	public int getMaxConcurrentSubscriptions() {
		return maxConcurrentSubscriptions;
	}

	/**
	 * Sets the cap on concurrently open {@code subscriptions/listen} streams per broker.
	 *
	 * @param value The new value. Must be {@code > 0}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is not {@code > 0}.
	 */
	public McpSubscriptionsConfig setMaxConcurrentSubscriptions(int value) {
		if (value <= 0)
			throw new IllegalArgumentException("maxConcurrentSubscriptions " + value + " must be > 0");
		maxConcurrentSubscriptions = value;
		return this;
	}

	/**
	 * The per-subscription drop-oldest queue bound.
	 *
	 * @return The bound. Always {@code > 0}.
	 */
	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * Sets the per-subscription drop-oldest queue bound.
	 *
	 * @param value The new value. Must be {@code > 0}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is not {@code > 0}.
	 */
	public McpSubscriptionsConfig setQueueSize(int value) {
		if (value <= 0)
			throw new IllegalArgumentException("queueSize " + value + " must be > 0");
		queueSize = value;
		return this;
	}

	/**
	 * The heartbeat/keepalive interval in milliseconds.
	 *
	 * @return The interval. Always {@code > 0}.
	 */
	public long getHeartbeatIntervalMs() {
		return heartbeatIntervalMs;
	}

	/**
	 * Sets the heartbeat/keepalive interval.
	 *
	 * @param value The new value. Must be {@code > 0}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is not {@code > 0}.
	 */
	public McpSubscriptionsConfig setHeartbeatIntervalMs(long value) {
		if (value <= 0)
			throw new IllegalArgumentException("heartbeatIntervalMs " + value + " must be > 0");
		heartbeatIntervalMs = value;
		return this;
	}

	/**
	 * The idle timeout in milliseconds.
	 *
	 * @return The timeout. Always {@code >= 0} ({@code 0} means disabled).
	 */
	public long getIdleTimeoutMs() {
		return idleTimeoutMs;
	}

	/**
	 * Sets the idle timeout.
	 *
	 * <p>
	 * <b>Keep this greater than {@link #getHeartbeatIntervalMs() heartbeatIntervalMs}.</b> A heartbeat
	 * counts as activity, so as long as {@code heartbeatIntervalMs < idleTimeoutMs}, heartbeats alone keep
	 * an otherwise-quiet stream alive indefinitely. Configuring {@code idleTimeoutMs} at or below
	 * {@code heartbeatIntervalMs} defeats that: the watchdog can fire between two heartbeats and silently
	 * reap a perfectly healthy connection. This setter accepts any non-negative value without checking
	 * against the currently-configured heartbeat interval (the two may be set in either order), so this is
	 * a configuration contract, not one this class enforces for you.
	 *
	 * @param value The new value. Must be {@code >= 0} ({@code 0} disables idle-timeout reaping).
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is negative.
	 */
	public McpSubscriptionsConfig setIdleTimeoutMs(long value) {
		if (value < 0)
			throw new IllegalArgumentException("idleTimeoutMs " + value + " must be >= 0");
		idleTimeoutMs = value;
		return this;
	}
}
