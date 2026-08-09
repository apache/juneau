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
package org.apache.juneau.commons.concurrent;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.function.*;

/**
 * SPI for detecting reuse ("replay") of a one-time-use identifier.
 *
 * <p>
 * A consumer wires a {@link ReplayCache} to narrow a token/identifier that would otherwise be usable any number
 * of times within its lifetime down to single-use: the second and every subsequent submission of the same
 * identifier is reported as a replay.  The SPI is deliberately generic &mdash; it deals only in a {@link String}
 * identifier and a <c>long</c> absolute expiry, with no dependency on any particular protocol or transport.
 *
 * <p>
 * <b>Atomic check-and-record (no TOCTOU).</b> {@link #checkAndRecord(String, long)} both checks whether an
 * identifier has been seen before <i>and</i> records it as seen, as one indivisible operation.  A separate
 * "seen" followed by a separate "record" would leave a race window in which two near-simultaneous submissions of
 * the same identifier could both observe "not yet seen" and both proceed; implementations must guarantee that of
 * any two concurrent calls with the same identifier, at most one returns <jk>true</jk>.
 *
 * <p>
 * <b>Fail-mode is a consumer decision.</b> When the backing store cannot answer &mdash; for example a remote
 * store is unreachable &mdash; an implementation may either throw or return an outcome.  The consumer decides how
 * a thrown failure is resolved by passing a {@link FailMode} to
 * {@link #checkAndRecord(String, long, FailMode, Consumer)}:
 * <ul>
 * 	<li>{@link FailMode#FAIL_CLOSED} (the safe default) treats a store failure as a replay (reject).
 * 	<li>{@link FailMode#FAIL_OPEN} treats a store failure as first-seen (allow), degrading to the
 * 		multi-use-tolerant behavior a consumer would have with no {@link ReplayCache} wired at all.
 * </ul>
 * An implementation that wants to enforce its own outcome on store failure (rather than defer to the consumer's
 * {@link FailMode}) can catch its own failures internally and return <jk>true</jk>/<jk>false</jk> from
 * {@link #checkAndRecord(String, long)} instead of throwing.
 *
 * <p>
 * <b>The built-in default is per-process only.</b> The zero-config built-in implementation
 * ({@link InMemoryReplayCache}) is backed by process-local memory, so it only enforces single-use within one
 * process.  Cross-node single-use requires a consumer-supplied {@link ReplayCache} backed by a store shared
 * across every node.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link InMemoryReplayCache}
 * </ul>
 *
 * @since 10.0.0
 */
public interface ReplayCache {

	/**
	 * The policy applied by {@link ReplayCache#checkAndRecord(String, long, FailMode, Consumer)} when
	 * {@link ReplayCache#checkAndRecord(String, long)} throws (the backing store could not answer).
	 *
	 * @since 10.0.0
	 */
	enum FailMode {

		/** Treat a store failure as first-seen: allow the operation to proceed (multi-use-tolerant). */
		FAIL_OPEN,

		/** Treat a store failure as a replay: reject the operation (the safe default). */
		FAIL_CLOSED
	}

	/**
	 * Atomically checks whether the specified identifier has been observed before and records it as seen if not.
	 *
	 * <p>
	 * <b>Implementations own their own timeout.</b> A call that blocks indefinitely (for example a remote store
	 * with no client-side deadline) blocks the caller with it.  An implementation backed by a network store must
	 * bound its own call so a slow/unreachable backend surfaces promptly as a throw (which the caller resolves
	 * per its chosen {@link FailMode}) or a return, never an unbounded stall.
	 *
	 * @param id The identifier to check.  Never <jk>null</jk>.
	 * @param expiresAtMs The identifier's own absolute expiry, in epoch milliseconds.  An implementation may use
	 * 	this to self-evict a record once it can no longer matter.
	 * @return <jk>true</jk> if this is the first time the identifier has been observed (the caller should
	 * 	proceed); <jk>false</jk> if it has been observed before (the caller should reject the submission as a
	 * 	replay).
	 */
	boolean checkAndRecord(String id, long expiresAtMs);

	/**
	 * Invokes {@link #checkAndRecord(String, long)} applying the specified fail-mode policy on a thrown failure.
	 *
	 * <p>
	 * Equivalent to {@link #checkAndRecord(String, long, FailMode, Consumer) checkAndRecord(id, expiresAtMs,
	 * failMode, <jk>null</jk>)}.
	 *
	 * @param id The identifier to check.  Never <jk>null</jk>.
	 * @param expiresAtMs The identifier's own absolute expiry, in epoch milliseconds.
	 * @param failMode The policy to apply if {@link #checkAndRecord(String, long)} throws.  Must not be
	 * 	<jk>null</jk>.
	 * @return <jk>true</jk> if the caller should proceed; <jk>false</jk> if the submission should be rejected.
	 */
	default boolean checkAndRecord(String id, long expiresAtMs, FailMode failMode) {
		return checkAndRecord(id, expiresAtMs, failMode, null);
	}

	/**
	 * Invokes {@link #checkAndRecord(String, long)} applying the specified fail-mode policy on a thrown failure.
	 *
	 * <p>
	 * If {@link #checkAndRecord(String, long)} throws, the exception is passed to <jv>onFailure</jv> (when
	 * non-<jk>null</jk>) and then resolved to <jk>true</jk> for {@link FailMode#FAIL_OPEN} or <jk>false</jk> for
	 * {@link FailMode#FAIL_CLOSED}.
	 *
	 * @param id The identifier to check.  Never <jk>null</jk>.
	 * @param expiresAtMs The identifier's own absolute expiry, in epoch milliseconds.
	 * @param failMode The policy to apply if {@link #checkAndRecord(String, long)} throws.  Must not be
	 * 	<jk>null</jk>.
	 * @param onFailure A handler invoked with the thrown exception before the policy is applied.  Can be
	 * 	<jk>null</jk>.
	 * @return <jk>true</jk> if the caller should proceed; <jk>false</jk> if the submission should be rejected.
	 */
	default boolean checkAndRecord(String id, long expiresAtMs, FailMode failMode, Consumer<Exception> onFailure) {
		assertArgNotNull("failMode", failMode);
		try {
			return checkAndRecord(id, expiresAtMs);
		} catch (Exception e) {
			if (onFailure != null)
				onFailure.accept(e);
			return failMode == FailMode.FAIL_OPEN;
		}
	}
}
