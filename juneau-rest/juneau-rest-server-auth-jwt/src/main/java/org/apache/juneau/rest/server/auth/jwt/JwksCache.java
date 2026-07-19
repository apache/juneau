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
package org.apache.juneau.rest.server.auth.jwt;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;

/**
 * Time-bounded cache of {@link JWKSet} keys with graceful-degradation on fetch failure
 * and optional eager refresh on a key-id ({@code kid}) cache miss.
 *
 * <p>
 * Wraps a {@link JWKSource} so that:
 * <ul>
 * 	<li>The full key set is fetched from the underlying source no more than once per {@code ttl}.
 * 	<li>If the underlying source fails (network down, JWKS endpoint returning 5xx), the cache
 * 		continues to return the last-known-good key set with a warning logged at {@code WARNING}
 * 		level. This avoids correlated auth outages caused by transient network blips.
 * 	<li>When {@code eagerRefreshOnKidMiss} is {@code true} (the default when constructed via
 * 		{@link JwtTokenValidator.Builder}), a fresh-cache selection that returns no matching keys
 * 		triggers a single out-of-band JWKS refresh before the TTL would normally expire. This
 * 		eliminates the rotation window where an IdP publishes a new signing key mid-TTL and clients
 * 		presenting tokens signed with that key would otherwise receive spurious 401 responses (up to
 * 		the full TTL = 5 minutes by default). The eager refresh is bounded by two guards:
 * 		<ul>
 * 			<li>A <em>per-cache cooldown</em> (default 10 seconds) that caps how often the JWKS
 * 				endpoint can be hit during a miss burst or an endpoint outage.
 * 			<li>A <em>shared in-flight {@link CompletableFuture}</em> so at most one network request
 * 				runs concurrently; all other threads experiencing the same miss wait for (and share)
 * 				the result of that single fetch.
 * 		</ul>
 * 	<li>This feature applies only when the cache is constructed via
 * 		{@link JwtTokenValidator.Builder#jwksUrl(java.net.URI) jwksUrl(...)}. A caller-supplied
 * 		{@link JwtTokenValidator.Builder#jwkSource(JWKSource) jwkSource(...)} is unaffected.
 * </ul>
 *
 * <p>
 * The cache is intentionally simple — single-slot, no LRU bounding, no key-rotation hints. JWKS
 * endpoints typically return a small (&lt;10) number of keys; caching the whole set is the standard
 * pattern. For production deployments that need fancier strategies (HSM-backed signers, multi-issuer
 * federation, etc.), implement {@link JWKSource} directly and pass it via
 * {@link JwtTokenValidator.Builder#jwkSource(JWKSource)}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link JwtTokenValidator}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 — JSON Web Key</a>
 * </ul>
 *
 * @since 10.0.0
 */
final class JwksCache implements JWKSource<SecurityContext> {

	/** Module logger. Warnings on JWKS fetch failures are intentionally loud per Resolved Decision #2. */
	private static final Logger LOG = Logger.getLogger(JwksCache.class.getName());

	/** Selector that pulls every key from the upstream source. */
	private static final JWKSelector SELECT_ALL = new JWKSelector(new JWKMatcher.Builder().build());

	/**
	 * Maximum wait for a loser thread joining an in-flight eager refresh.  Slightly longer than a
	 * typical HTTP read timeout so a misbehaving delegate can't pin a request thread indefinitely.
	 */
	private static final long IN_FLIGHT_TIMEOUT_SECONDS = 10;

	private final JWKSource<SecurityContext> delegate;
	private final Duration ttl;
	private final Clock clock;
	private final boolean eagerRefreshOnKidMiss;
	private final Duration eagerRefreshCooldown;
	private final AtomicReference<Entry> slot = new AtomicReference<>();

	/** Epoch of the most recent eager-refresh attempt; {@link Instant#MIN} initially (first attempt always allowed). */
	private final AtomicReference<Instant> lastEagerAttempt = new AtomicReference<>(Instant.MIN);

	/**
	 * In-flight eager-refresh future; {@code null} when idle.  Set via CAS to deduplicate concurrent
	 * miss requests: the CAS winner does the single network fetch; losers await this future.
	 */
	private final AtomicReference<CompletableFuture<JWKSet>> inFlight = new AtomicReference<>();

	JwksCache(JWKSource<SecurityContext> delegate, Duration ttl, Clock clock,
			boolean eagerRefreshOnKidMiss, Duration eagerRefreshCooldown) {
		this.delegate = delegate;
		this.ttl = ttl;
		this.clock = clock;
		this.eagerRefreshOnKidMiss = eagerRefreshOnKidMiss;
		this.eagerRefreshCooldown = eagerRefreshCooldown;
	}

	@Override /* Overridden from JWKSource */
	public List<JWK> get(JWKSelector selector, SecurityContext ctx) throws KeySourceException {
		var now = clock.instant();
		var current = slot.get();
		if (current != null && current.fetchedAt.plus(ttl).isAfter(now)) {
			// Fresh-cache path: serve from cache unless we get an empty selection and eager is on.
			var hits = selector.select(current.jwkSet);
			if (!hits.isEmpty() || !eagerRefreshOnKidMiss)
				return hits;
			return tryEagerRefresh(selector, ctx, now);
		}
		// Expired / cold path — unchanged from pre-10.0.0 behavior.
		try {
			var refreshed = fetchFromDelegate(ctx);
			slot.set(new Entry(refreshed, now));
			return selector.select(refreshed);
		} catch (KeySourceException | RuntimeException e) {
			if (current != null) {
				LOG.log(Level.WARNING, e,
					() -> "JWKS refresh failed; serving cached keys past TTL.  Underlying error: " + e.getMessage());
				return selector.select(current.jwkSet);
			}
			throw e;
		}
	}

	/**
	 * Attempts a single eager refresh when the fresh cache produced no keys for the selector.
	 *
	 * <ul>
	 * 	<li>Cooldown gate: returns empty immediately if a prior attempt was within the cooldown window.
	 * 	<li>CAS winner: runs the one network fetch, updates the slot, completes the shared future.
	 * 	<li>CAS losers: wait for the winner's future (bounded timeout), then select from its result.
	 * 	<li>On outage: retains the last-known-good slot, logs WARNING, returns empty (→ 401).
	 * 	<li>{@code lastEagerAttempt} is always advanced in the {@code finally} block so a burst of
	 * 		distinct unknown kids cannot bypass the cooldown.
	 * </ul>
	 */
	private List<JWK> tryEagerRefresh(JWKSelector selector, SecurityContext ctx, Instant now) {
		if (now.isBefore(lastEagerAttempt.get().plus(eagerRefreshCooldown))) {
			LOG.fine(() -> "JWKS eager refresh skipped (within cooldown)");
			return Collections.emptyList();
		}
		var myFuture = new CompletableFuture<JWKSet>();
		var prior = inFlight.compareAndExchange(null, myFuture);
		if (prior != null)
			return awaitAndSelect(selector, prior);

		// CAS winner: run the single eager fetch.
		try {
			LOG.fine(() -> "JWKS eager refresh triggered by kid miss");
			var refreshed = fetchFromDelegate(ctx);
			slot.set(new Entry(refreshed, now));
			myFuture.complete(refreshed);
			return selector.select(refreshed);
		} catch (KeySourceException | RuntimeException e) {
			myFuture.completeExceptionally(e);
			if (slot.get() != null)
				LOG.log(Level.WARNING, e,
					() -> "JWKS eager refresh failed; retaining last-known-good set.  Underlying error: " + e.getMessage());
			return Collections.emptyList();
		} finally {
			// Advance cooldown on every attempt (success, still-missing, or outage) so a burst of
			// unknown kids cannot hammer the endpoint within one cooldown window.
			lastEagerAttempt.set(now);
			inFlight.compareAndSet(myFuture, null);
		}
	}

	/** Loser path: wait for the in-flight future and select from its result. */
	private static List<JWK> awaitAndSelect(JWKSelector selector, CompletableFuture<JWKSet> future) {
		try {
			return selector.select(future.get(IN_FLIGHT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		} catch (ExecutionException | TimeoutException e) {
			// Fail closed: the in-flight refresh errored or timed out, so this loser thread selects nothing
			// (the winner logs the underlying cause at WARNING).  A key miss surfaces as an auth failure upstream.
			LOG.log(Level.FINE, e, () -> "JWKS in-flight refresh did not complete for this waiter: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private JWKSet fetchFromDelegate(SecurityContext ctx) throws KeySourceException {
		return new JWKSet(delegate.get(SELECT_ALL, ctx));
	}

	private record Entry(JWKSet jwkSet, Instant fetchedAt) {}
}
