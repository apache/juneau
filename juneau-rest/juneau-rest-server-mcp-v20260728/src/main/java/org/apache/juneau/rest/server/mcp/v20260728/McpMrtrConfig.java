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
 * Binding-owned MRTR (Multi-Round-Trip Request) configuration for a v2 {@link McpRevision}.
 *
 * <p>
 * Mirrors {@link McpCacheConfig}'s exact placement precedent (spec Open item #2): MRTR is as
 * {@code 2026-07-28}-only as cache hints, so its configuration lives here, on the v2 adapter, never on the
 * revision-neutral {@link org.apache.juneau.rest.server.mcp.McpServerConfig}.
 *
 * <p>
 * <b>Mutable during setup, effectively immutable once published.</b> This is a builder-less mutable-setup
 * bean: its fluent setters exist to configure it during construction, but a binding must fully configure it
 * <i>before</i> handing it to a {@link McpRevision} and must never mutate it afterward. The dispatcher reads
 * the codec/TTL/max-rounds on every request with no synchronization, so a post-publication mutation is a data
 * race; treat a published instance as read-only.
 *
 * <p>
 * Defaults: {@link AeadRequestStateCodec} (a fresh instance per {@link McpMrtrConfig}), a 5-minute
 * {@code requestState} TTL, a max-rounds cap of 10, and no {@link ReplayCache} (replay rejection is opt-in; see
 * {@link #setReplayCache(ReplayCache)}). Operators inject a shared/rotating-key {@link RequestStateCodec} here
 * for multi-instance or restart-durable resumption.
 *
 * <h5 class='section'>Trust model:</h5>
 * <ul>
 * 	<li><b>{@code requestState} is a bearer token.</b> Anyone holding a sealed {@code requestState} can resume
 * 		the paused operation until it expires; the token is not bound to a caller identity. It must be
 * 		transported only over authenticated TLS and <b>never logged</b> (it is opaque ciphertext, but logging
 * 		it hands a replayable credential to anyone with log access). See {@link RequestStateCodec}.
 * 	<li><b>Resume side effects must be idempotent by default.</b> A captured token can be replayed any number
 * 		of times within its {@link #getTtlMs() TTL} <i>unless</i> a {@link ReplayCache} is configured (see
 * 		{@link #setReplayCache(ReplayCache)}) to enforce single-use &mdash; the built-in
 * 		{@link InMemoryReplayCache} is per-process only, so cross-node single-use requires a shared
 * 		implementation. The {@link #getMaxRounds() max-rounds cap} bounds the <i>depth</i> of a single resume
 * 		chain, not the total number of times a given token can be re-submitted, so a handler must not treat a
 * 		resume as a once-only event unless it knows single-use is enforced.
 * </ul>
 */
public class McpMrtrConfig {

	/** Default {@code requestState} TTL in milliseconds: 5 minutes. */
	public static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

	/** Default max-rounds cap. */
	public static final int DEFAULT_MAX_ROUNDS = 10;

	private RequestStateCodec codec = new AeadRequestStateCodec();
	private long ttlMs = DEFAULT_TTL_MS;
	private int maxRounds = DEFAULT_MAX_ROUNDS;
	private ReplayCache replayCache;

	/**
	 * The codec used to seal/unseal {@code requestState} tokens.
	 *
	 * @return The codec. Never {@code null}.
	 */
	public RequestStateCodec getCodec() {
		return codec;
	}

	/**
	 * Sets the codec.
	 *
	 * @param value The new value. Must not be {@code null}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is {@code null}.
	 */
	public McpMrtrConfig setCodec(RequestStateCodec value) {
		if (value == null)
			throw new IllegalArgumentException("codec must not be null");
		codec = value;
		return this;
	}

	/**
	 * Sets the key provider, wrapping it in a fresh {@link AeadRequestStateCodec}.
	 *
	 * <p>
	 * Sugar for the common "keep AES-GCM, just supply my keys" case &mdash; equivalent to
	 * {@code setCodec(new AeadRequestStateCodec(value))}. {@link #setKeyProvider} and {@link #setCodec} are
	 * last-wins: both assign the same {@code codec} field, so whichever is called more recently determines the
	 * effective codec. There is no getter for the provider itself &mdash; {@link #getCodec()} remains the sole
	 * accessor, since the codec (not the provider) is the source of truth once either setter has been called.
	 *
	 * @param value The new value. Must not be {@code null}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is {@code null}.
	 */
	public McpMrtrConfig setKeyProvider(KeyProvider value) {
		if (value == null)
			throw new IllegalArgumentException("keyProvider must not be null");
		return setCodec(new AeadRequestStateCodec(value));
	}

	/**
	 * The {@code requestState} time-to-live in milliseconds.
	 *
	 * @return The TTL. Always {@code > 0}.
	 */
	public long getTtlMs() {
		return ttlMs;
	}

	/**
	 * Sets the TTL.
	 *
	 * @param value The new value. Must be {@code > 0}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is not {@code > 0}.
	 */
	public McpMrtrConfig setTtlMs(long value) {
		if (value <= 0)
			throw new IllegalArgumentException("ttlMs " + value + " must be > 0");
		ttlMs = value;
		return this;
	}

	/**
	 * The max-rounds cap.
	 *
	 * <p>
	 * Counts total handler invocations in a single resume chain, not just the resumes: the default of 10 permits
	 * 1 initial call plus 9 resumes before {@link McpRevision#CODE_MAX_ROUNDS_EXCEEDED} is returned.
	 *
	 * @return The cap. Always {@code >= 1}.
	 */
	public int getMaxRounds() {
		return maxRounds;
	}

	/**
	 * Sets the max-rounds cap.
	 *
	 * @param value The new value. Must be {@code >= 1}.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is not {@code >= 1}.
	 */
	public McpMrtrConfig setMaxRounds(int value) {
		if (value < 1)
			throw new IllegalArgumentException("maxRounds " + value + " must be >= 1");
		maxRounds = value;
		return this;
	}

	/**
	 * The {@link ReplayCache} used to reject reuse of a consumed {@code requestState} token, or <jk>null</jk> if
	 * none is configured.
	 *
	 * <p>
	 * <b>Unset (<jk>null</jk>) by default &mdash; replay rejection is opt-in.</b> A dispatcher only performs a
	 * replay check when this returns non-<jk>null</jk> (see {@code McpRevision#resolveMrtrContext}); with no
	 * {@link ReplayCache} configured, a {@code requestState} token remains multi-use within its TTL, exactly as
	 * documented on {@link RequestStateCodec}.
	 *
	 * @return The configured replay cache, or <jk>null</jk> if replay rejection is disabled (the default).
	 */
	public ReplayCache getReplayCache() {
		return replayCache;
	}

	/**
	 * Sets the replay cache, enabling replay rejection.
	 *
	 * <p>
	 * Passing <jk>null</jk> is allowed and explicitly reverts to the default, opt-out behavior (no replay check;
	 * a token stays multi-use within its TTL) &mdash; unlike {@link #setCodec(RequestStateCodec)}, there is no
	 * always-non-null invariant to preserve here, since "no replay cache" is itself the documented default.
	 *
	 * @param value The replay cache to use, or <jk>null</jk> to disable replay rejection.
	 * @return This object.
	 */
	public McpMrtrConfig setReplayCache(ReplayCache value) {
		replayCache = value;
		return this;
	}
}
