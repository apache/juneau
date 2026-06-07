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
package org.apache.juneau.rest.server.auth.oidc.rp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.time.*;
import java.util.*;

/**
 * Single-use, TTL-bounded store for the per-login {@code state} &rarr; {@code (nonce, codeVerifier,
 * redirectTarget)} association created during {@link OidcRelyingParty#startLogin} and consumed during
 * {@link OidcRelyingParty#completeLogin}.
 *
 * <p>
 * Security properties (see OpenID Connect Core &sect;3.1.2.1 and the RP charter):
 * <ul>
 * 	<li><b>Single-use</b> &mdash; {@link #consume(String)} atomically removes the entry, so a replayed
 * 		callback with the same {@code state} fails.
 * 	<li><b>TTL-bounded</b> &mdash; entries older than the configured TTL are treated as a miss and swept.
 * 	<li><b>Size-bounded</b> &mdash; an LRU cap (default 10 000) prevents unbounded growth from abandoned
 * 		login attempts; eviction mirrors the {@code BoundedLruTokenCache} shape.
 * </ul>
 *
 * <p>
 * Thread-safe.
 *
 * @since 10.0.0
 */
public class EphemeralStore {

	/** Default maximum number of in-flight login attempts retained. */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;

	/** Default time-to-live for a pending login (the user has at most a few minutes at the IdP). */
	public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

	/** Hard cap on the configurable TTL. */
	static final Duration MAX_TTL = Duration.ofMinutes(30);

	/**
	 * A pending-login association awaiting the IdP callback.
	 *
	 * @param nonce The OIDC {@code nonce} value bound into the authorization request.
	 * @param codeVerifier The PKCE {@code code_verifier} string.
	 * @param redirectTarget The application URL to redirect to after a successful login.  May be
	 * 	{@code null}.
	 * @param createdAt The instant the entry was stored.
	 */
	public record PendingLogin(String nonce, String codeVerifier, String redirectTarget, Instant createdAt) {}

	private final int maxEntries;
	private final Duration ttl;
	private final Clock clock;
	private final Map<String,PendingLogin> entries;
	private final Object lock = new Object();

	/**
	 * Constructor.
	 *
	 * @param ttl The single-use TTL.  Must be positive and not exceed 30 minutes.
	 * @param maxEntries The LRU size cap.  Must be positive.
	 * @param clock The clock for TTL comparisons.  Must not be <jk>null</jk>.
	 */
	public EphemeralStore(Duration ttl, int maxEntries, Clock clock) {
		assertArgNotNull("ttl", ttl);
		assertArg(!ttl.isZero() && !ttl.isNegative(), "ttl must be positive");
		assertArg(ttl.compareTo(MAX_TTL) <= 0, "ttl must not exceed 30 minutes (was {0})", ttl);
		assertArg(maxEntries > 0, "maxEntries must be positive (was {0})", maxEntries);
		this.ttl = ttl;
		this.maxEntries = maxEntries;
		this.clock = assertArgNotNull("clock", clock);
		this.entries = new LinkedHashMap<>(16, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<String,PendingLogin> eldest) {
				return size() > EphemeralStore.this.maxEntries;
			}
		};
	}

	/**
	 * Stores a pending-login association keyed by {@code state}.
	 *
	 * @param state The opaque {@code state} value.  Must not be <jk>null</jk> or blank.
	 * @param nonce The OIDC {@code nonce}.  Must not be <jk>null</jk> or blank.
	 * @param codeVerifier The PKCE {@code code_verifier}.  Must not be <jk>null</jk> or blank.
	 * @param redirectTarget The post-login redirect target.  May be <jk>null</jk>.
	 */
	public void store(String state, String nonce, String codeVerifier, String redirectTarget) {
		assertArgNotNullOrBlank("state", state);
		assertArgNotNullOrBlank("nonce", nonce);
		assertArgNotNullOrBlank("codeVerifier", codeVerifier);
		var now = clock.instant();
		synchronized (lock) {
			sweepExpired(now);
			entries.put(state, new PendingLogin(nonce, codeVerifier, redirectTarget, now));
		}
	}

	/**
	 * Atomically removes and returns the pending-login association for {@code state}, if present and
	 * not expired.
	 *
	 * @param state The {@code state} value from the callback.  Must not be <jk>null</jk>.
	 * @return The association, or {@link Optional#empty()} if absent or expired.
	 */
	public Optional<PendingLogin> consume(String state) {
		if (state == null)
			return opte();
		var now = clock.instant();
		synchronized (lock) {
			var p = entries.remove(state);
			if (p == null)
				return opte();
			if (isExpired(p, now))
				return opte();
			return opt(p);
		}
	}

	/**
	 * Returns the current entry count.  Primarily for tests and metrics.
	 *
	 * @return The entry count.
	 */
	public int size() {
		synchronized (lock) {
			return entries.size();
		}
	}

	private boolean isExpired(PendingLogin p, Instant now) {
		return !now.isBefore(p.createdAt().plus(ttl));
	}

	private void sweepExpired(Instant now) {
		entries.values().removeIf(p -> isExpired(p, now));
	}
}
