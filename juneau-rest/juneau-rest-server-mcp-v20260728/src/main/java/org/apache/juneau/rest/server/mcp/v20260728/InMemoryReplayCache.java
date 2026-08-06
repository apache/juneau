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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Built-in default {@link ReplayCache}: a per-process, {@link ConcurrentHashMap}-backed seen-{@code jti} set
 * with self-eviction bounded by each record's own {@code expiresAtMs}.
 *
 * <p>
 * <b>Not shareable across process instances by design</b> &mdash; mirrors {@link EphemeralKeyProvider}'s own
 * "per-process, not restart-durable" precedent exactly, just for replay tracking instead of key material. Two
 * separate JVMs (or two instances behind a load balancer) each holding their own {@link InMemoryReplayCache}
 * will not see each other's recorded {@code jti}s, so a token resumed against one instance and then replayed
 * against the other is <b>not</b> caught. Operators who need cross-node single-use enforcement must supply a
 * {@link ReplayCache} backed by a store shared across every node instead of relying on this default.
 *
 * <p>
 * <b>Memory bound (size honestly).</b> A {@code jti} is retained only until its own {@code expiresAtMs}, so the
 * map's steady-state size is bounded by the product of the <i>distinct-token submission rate</i> and the token
 * {@link McpMrtrConfig#getTtlMs() TTL} &mdash; i.e. roughly the number of distinct, still-unexpired tokens
 * outstanding at once, plus at most one TTL's worth of already-expired records awaiting the next sweep. Under a
 * high pause/resume rate with a long TTL this is not inherently small; operators should size accordingly, or
 * supply a store-backed {@link ReplayCache} with server-side expiry. There is deliberately <b>no</b> hard cap
 * that drops still-unexpired {@code jti}s: doing so would silently weaken replay protection by letting an
 * evicted-but-valid token be replayed.
 *
 * <p>
 * <b>Eviction is throttled.</b> Sweeping out already-expired records is an O(n) scan, so it runs at most once
 * per {@link #DEFAULT_SWEEP_INTERVAL_MS} rather than on every call (which would make {@link #checkAndRecord}
 * effectively O(n) per invocation under load). An already-expired token is separately rejected by the
 * dispatcher's own expiry check before a {@link ReplayCache} is ever consulted (see
 * {@code McpRevision#resolveMrtrContext}), so no correctness depends on the sweep running promptly &mdash; it
 * exists purely to bound memory.
 *
 * <p>
 * <b>Thread-safety.</b> {@link #checkAndRecord(String, long)} is safe for concurrent invocation: the
 * check-and-record step is a single {@link ConcurrentHashMap#putIfAbsent} call, so of any two concurrent calls
 * with the same {@code jti}, exactly one observes a first-seen (<jk>true</jk>) outcome. The throttled sweep is
 * guarded by an atomic compare-and-set so at most one thread sweeps per interval.
 *
 * @since 10.0.0
 */
public class InMemoryReplayCache implements ReplayCache {

	/** Default minimum interval, in milliseconds, between opportunistic expired-record sweeps. */
	public static final long DEFAULT_SWEEP_INTERVAL_MS = 1000L;

	private final ConcurrentHashMap<String,Long> seen = new ConcurrentHashMap<>();
	private final long sweepIntervalMs;
	private final AtomicLong nextSweepAtMs = new AtomicLong();

	/**
	 * Constructor using the {@link #DEFAULT_SWEEP_INTERVAL_MS default} sweep interval.
	 */
	public InMemoryReplayCache() {
		this(DEFAULT_SWEEP_INTERVAL_MS);
	}

	/**
	 * Constructor.
	 *
	 * @param sweepIntervalMs The minimum interval, in milliseconds, between opportunistic expired-record sweeps.
	 * 	{@code 0} sweeps on every call (useful in tests that assert eviction behavior). Must be {@code >= 0}.
	 */
	InMemoryReplayCache(long sweepIntervalMs) {
		if (sweepIntervalMs < 0)
			throw new IllegalArgumentException("sweepIntervalMs " + sweepIntervalMs + " must be >= 0");
		this.sweepIntervalMs = sweepIntervalMs;
	}

	@Override /* ReplayCache */
	public boolean checkAndRecord(String jti, long expiresAtMs) {
		assertArgNotNull("jti", jti);
		maybeEvictExpired();
		return seen.putIfAbsent(jti, expiresAtMs) == null;
	}

	/**
	 * Sweeps expired records at most once per {@link #sweepIntervalMs}. The atomic compare-and-set on
	 * {@code nextSweepAtMs} both throttles the sweep and ensures a single sweeper per window; a losing thread
	 * simply skips the sweep (its own {@code putIfAbsent} still runs, so no {@code jti} is ever missed).
	 */
	private void maybeEvictExpired() {
		var now = System.currentTimeMillis();
		var next = nextSweepAtMs.get();
		if (now < next)
			return;
		if (! nextSweepAtMs.compareAndSet(next, now + sweepIntervalMs))
			return;
		seen.values().removeIf(expiresAt -> expiresAt <= now);
	}
}
