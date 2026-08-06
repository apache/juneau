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
 * SPI for detecting reuse ("replay") of a sealed MCP MRTR {@code requestState} resume token (see
 * {@code McpRevision#resolveMrtrContext}).
 *
 * <p>
 * <b>Opt-in.</b> A {@code requestState} token is, by default, a multi-use bearer credential: it may be resumed
 * any number of times within its TTL (see {@link RequestStateCodec}). Wiring a {@link ReplayCache} via
 * {@link McpMrtrConfig#setReplayCache(ReplayCache)} narrows that default to single-use &mdash; the second and
 * every subsequent submission of the same token is rejected. No {@link ReplayCache} is wired by default (see
 * {@link McpMrtrConfig#getReplayCache()}).
 *
 * <p>
 * <b>Atomic check-and-record (no TOCTOU).</b> {@link #checkAndRecord(String, long)} both checks whether
 * {@code jti} has been seen before <i>and</i> records it as seen, as one indivisible operation. A separate
 * {@code seen(jti)} followed by a separate {@code record(jti)} would leave a race window in which two
 * near-simultaneous submissions of the same token could both observe "not yet seen" and both proceed;
 * implementations must guarantee that of any two concurrent calls with the same {@code jti}, at most one
 * returns <jk>true</jk>.
 *
 * <p>
 * <b>Fail-mode split (deliberate).</b> The dispatcher and this SPI split responsibility for what happens when
 * the backing store cannot answer the question:
 * <ul>
 * 	<li><b>A thrown exception is fail-open.</b> If {@link #checkAndRecord(String, long)} throws, the dispatcher
 * 		catches it, logs it, and treats the token as first-seen &mdash; the resume proceeds exactly as it would
 * 		with no {@link ReplayCache} wired at all. This degrades a transient store outage to today's
 * 		already-documented multi-use-tolerant behavior rather than rejecting all MRTR resume traffic.
 * 	<li><b>An operator who wants fail-closed must return <jk>false</jk>, not throw.</b> An implementation backed
 * 		by a store that is unreachable, and whose operator prefers to reject rather than degrade, must catch its
 * 		own I/O failures internally and return <jk>false</jk> (a "replay" outcome) rather than let the exception
 * 		propagate &mdash; the framework itself applies no fail-closed policy and offers no config toggle for one.
 * </ul>
 *
 * <p>
 * <b>Built-in default is per-process only.</b> The zero-config built-in implementation
 * ({@link InMemoryReplayCache}) is backed by process-local memory: it is not shareable across process
 * instances, so it only enforces single-use within one process. Cross-node (horizontally-scaled) single-use
 * requires an operator-supplied {@link ReplayCache} backed by a store shared across every node (for example
 * Redis), mirroring {@link KeyProvider}'s own SPI-plus-non-shareable-default precedent.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link InMemoryReplayCache}
 * 	<li class='jc'>{@link McpMrtrConfig#setReplayCache(ReplayCache)}
 * </ul>
 *
 * @since 10.0.0
 */
public interface ReplayCache {

	/**
	 * Atomically checks whether {@code jti} has been observed before and records it as seen if not.
	 *
	 * <p>
	 * <b>Implementations own their own timeout.</b> The framework's fail-open safety net covers a thrown
	 * exception (see the class Javadoc), not a hang: a call that blocks indefinitely (for example a remote store
	 * with no client-side deadline) blocks the resume with it. An implementation backed by a network store must
	 * bound its own call so a slow/unreachable backend surfaces as a prompt throw (fail-open) or return
	 * (operator-chosen fail-closed), never an unbounded stall.
	 *
	 * @param jti The token identifier read from the sealed {@code requestState} plaintext (never the
	 * 	client-supplied token string itself). Never <jk>null</jk>.
	 * @param expiresAtMs The sealed token's own absolute expiry, in epoch milliseconds. An implementation may
	 * 	use this to self-evict the record once it can no longer matter &mdash; an already-expired token is
	 * 	separately rejected by the dispatcher's own expiry check regardless of what this method returns, so a
	 * 	record never needs to outlive its token's own TTL.
	 * @return <jk>true</jk> if this is the first time {@code jti} has been observed (the caller should proceed);
	 * 	<jk>false</jk> if {@code jti} has been observed before (the caller should reject the resume as a
	 * 	replay).
	 */
	boolean checkAndRecord(String jti, long expiresAtMs);
}
