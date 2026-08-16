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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;

/**
 * The shipped, single-node default {@link LoginStateStore}: a single-use, TTL-bounded, size-bounded
 * in-memory store for the per-login {@code state} &rarr; {@link LoginStateStore.PendingLogin} association
 * created during {@link OidcRelyingParty#startLogin} and consumed during {@link OidcRelyingParty#completeLogin}.
 *
 * <p>
 * Security properties (see OpenID Connect Core &sect;3.1.2.1 and the RP charter):
 * <ul>
 * 	<li><b>Single-use</b> &mdash; {@link #consume(String)} atomically removes the entry under a monitor, so a
 * 		replayed callback with the same {@code state} fails.  Cross-node single-use requires a shared store
 * 		with an atomic consume primitive (see {@link LoginStateStore#consume(String)}); this impl only
 * 		guarantees single-use within one JVM.
 * 	<li><b>TTL-bounded</b> &mdash; entries at or past their framework-minted {@code expiresAt} are treated as a
 * 		miss and swept.
 * 	<li><b>Size-bounded</b> &mdash; an LRU cap (default 10 000) prevents unbounded growth from abandoned
 * 		login attempts; eviction mirrors the {@code BoundedLruTokenCache} shape.
 * </ul>
 *
 * <p>
 * The framework mints {@code createdAt}/{@code expiresAt} on the {@link LoginStateStore.PendingLogin} record;
 * this impl persists them verbatim and does not stamp timestamps of its own.  The {@code ttl} argument here
 * only bounds sweep/GC of abandoned entries and asserts the framework's {@code (0, MAX_TTL]} ceiling for the
 * default path; the relying party re-checks {@code expiresAt} independently on consume.
 *
 * <p>
 * Thread-safe.
 *
 * @since 10.0.0
 */
public class InMemoryLoginStateStore implements LoginStateStore {

	/** Default maximum number of in-flight login attempts retained. */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;

	/** Default time-to-live for a pending login (the user has at most a few minutes at the IdP). */
	public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

	private final int maxEntries;
	private final Clock clock;
	private final Map<String,PendingLogin> entries;
	private final Object lock = new Object();

	/**
	 * Constructor.
	 *
	 * @param ttl The single-use TTL.  Must be positive and not exceed {@link LoginStateStore#MAX_TTL}.  Bounds
	 * 	sweep of abandoned entries and asserts the framework ceiling for the default path; per-entry expiry is
	 * 	driven by the framework-minted {@link LoginStateStore.PendingLogin#expiresAt()}.
	 * @param maxEntries The LRU size cap.  Must be positive.
	 * @param clock The clock for TTL sweeping.  Must not be <jk>null</jk>.
	 */
	public InMemoryLoginStateStore(Duration ttl, int maxEntries, Clock clock) {
		assertArgNotNull("ttl", ttl);
		assertArg(!ttl.isZero() && !ttl.isNegative(), "ttl must be positive");
		assertArg(ttl.compareTo(MAX_TTL) <= 0, "ttl must not exceed 30 minutes (was %s)", ttl);
		assertArg(maxEntries > 0, "maxEntries must be positive (was %s)", maxEntries);
		this.maxEntries = maxEntries;
		this.clock = assertArgNotNull("clock", clock);
		this.entries = new LinkedHashMap<>(16, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<String,PendingLogin> eldest) {
				return size() > InMemoryLoginStateStore.this.maxEntries;
			}
		};
	}

	@Override /* LoginStateStore */
	public void store(String state, PendingLogin pending) {
		assertArgNotNullOrBlank("state", state);
		assertArgNotNull("pending", pending);
		var now = clock.instant();
		synchronized (lock) {
			sweepExpired(now);
			entries.put(state, pending);
		}
	}

	@Override /* LoginStateStore */
	public Optional<PendingLogin> consume(String state) {
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

	private boolean isExpired(PendingLogin p, Instant now) {
		var exp = p.expiresAt();
		if (exp == null)
			return true;
		return !now.isBefore(exp);
	}

	private void sweepExpired(Instant now) {
		entries.values().removeIf(p -> isExpired(p, now));
	}
}
