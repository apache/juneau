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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.time.*;
import java.util.*;

/**
 * Single-use, TTL-bounded store for the per-authorization {@code state} &rarr; {@code (codeVerifier, expectedIssuer)}
 * association created when an interactive authorization-code + PKCE flow is started and consumed when the loopback
 * redirect arrives.
 *
 * <p>
 * Relocation of the {@code EphemeralStore} pattern from {@code juneau-rest-server-auth-oidc-rp} (Q3) for headless
 * (CLI/desktop) use.  The browser-session {@code redirectTarget}/{@code nonce} shape is dropped; this headless variant
 * stores the PKCE {@code code_verifier} and the discovered {@code expectedIssuer} used for the SEP-2468 {@code iss}
 * check on the callback.
 *
 * <p>
 * Security properties:
 * <ul>
 * 	<li><b>Single-use</b> &mdash; {@link #consume(String)} atomically removes the entry, so a replayed callback with the
 * 		same {@code state} fails.
 * 	<li><b>TTL-bounded</b> &mdash; entries older than the configured TTL are treated as a miss and swept.
 * 	<li><b>Size-bounded</b> &mdash; an LRU cap (default 1000) prevents unbounded growth from abandoned attempts.
 * </ul>
 *
 * <p>
 * Thread-safe.
 *
 * @since 10.0.0
 */
public class EphemeralStore {

	/** Default maximum number of in-flight authorization attempts retained. */
	public static final int DEFAULT_MAX_ENTRIES = 1_000;

	/** Default time-to-live for a pending authorization (the user has at most a few minutes at the IdP). */
	public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

	/** Hard cap on the configurable TTL. */
	static final Duration MAX_TTL = Duration.ofMinutes(30);

	/**
	 * A pending-authorization association awaiting the IdP loopback callback.
	 *
	 * @param codeVerifier The PKCE {@code code_verifier} string.
	 * @param expectedIssuer The discovered issuer the callback {@code iss} parameter is validated against (SEP-2468).
	 * 	May be {@code null} when no issuer was discovered.
	 * @param createdAt The instant the entry was stored.
	 */
	public record PendingAuthorization(String codeVerifier, URI expectedIssuer, Instant createdAt) {

		/**
		 * Redacts the PKCE {@code codeVerifier} so it never reaches logs via this record's {@code toString()}
		 * (mirrors the {@code OAuthToken} / {@code McpTokenProvider} redaction discipline).
		 *
		 * @return A redacted string form disclosing only non-secret metadata.
		 */
		@Override
		public String toString() {
			return "PendingAuthorization[codeVerifier=<redacted>, expectedIssuer=" + expectedIssuer
				+ ", createdAt=" + createdAt + "]";
		}
	}

	private final int maxEntries;
	private final Duration ttl;
	private final Clock clock;
	private final Map<String,PendingAuthorization> entries;
	private final Object lock = new Object();

	/**
	 * Creates a store with the default TTL, size cap, and system UTC clock.
	 *
	 * @return A new store.
	 */
	public static EphemeralStore create() {
		return new EphemeralStore(DEFAULT_TTL, DEFAULT_MAX_ENTRIES, Clock.systemUTC());
	}

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
		assertArg(ttl.compareTo(MAX_TTL) <= 0, "ttl must not exceed 30 minutes (was %s)", ttl);
		assertArg(maxEntries > 0, "maxEntries must be positive (was %s)", maxEntries);
		this.ttl = ttl;
		this.maxEntries = maxEntries;
		this.clock = assertArgNotNull("clock", clock);
		this.entries = new LinkedHashMap<>(16, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<String,PendingAuthorization> eldest) {
				return size() > EphemeralStore.this.maxEntries;
			}
		};
	}

	/**
	 * Stores a pending-authorization association keyed by {@code state}.
	 *
	 * @param state The opaque {@code state} value.  Must not be <jk>null</jk> or blank.
	 * @param codeVerifier The PKCE {@code code_verifier}.  Must not be <jk>null</jk> or blank.
	 * @param expectedIssuer The discovered issuer for the SEP-2468 {@code iss} check.  May be <jk>null</jk>.
	 */
	public void store(String state, String codeVerifier, URI expectedIssuer) {
		assertArgNotNullOrBlank("state", state);
		assertArgNotNullOrBlank("codeVerifier", codeVerifier);
		var now = clock.instant();
		synchronized (lock) {
			sweepExpired(now);
			entries.put(state, new PendingAuthorization(codeVerifier, expectedIssuer, now));
		}
	}

	/**
	 * Atomically removes and returns the pending-authorization association for {@code state}, if present and
	 * not expired.
	 *
	 * @param state The {@code state} value from the callback.  Must not be <jk>null</jk>.
	 * @return The association, or {@link Optional#empty()} if absent or expired.
	 */
	public Optional<PendingAuthorization> consume(String state) {
		if (state == null)
			return oe();
		var now = clock.instant();
		synchronized (lock) {
			var p = entries.remove(state);
			if (p == null)
				return oe();
			if (isExpired(p, now))
				return oe();
			return o(p);
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

	private boolean isExpired(PendingAuthorization p, Instant now) {
		return !now.isBefore(p.createdAt().plus(ttl));
	}

	private void sweepExpired(Instant now) {
		entries.values().removeIf(p -> isExpired(p, now));
	}
}
