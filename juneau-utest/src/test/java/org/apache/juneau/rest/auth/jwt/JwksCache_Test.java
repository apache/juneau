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
package org.apache.juneau.rest.auth.jwt;

import static org.apache.juneau.rest.auth.jwt.JwtTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;

/**
 * Cache + graceful-degradation behavior for {@link JwksCache}, exercised end-to-end via
 * {@link JwtTokenValidator}.
 *
 * @since 9.5.0
 */
class JwksCache_Test extends TestBase {

	/** Counting JWK source: returns the supplied keys and records every fetch call. */
	private static final class CountingSource implements JWKSource<SecurityContext> {
		final AtomicInteger fetches = new AtomicInteger();
		final AtomicReference<JWKSet> set;
		volatile boolean fail;

		CountingSource(JWKSet initial) { this.set = new AtomicReference<>(initial); }

		@Override /* Overridden from JWKSource */
		public List<JWK> get(JWKSelector selector, SecurityContext ctx) throws KeySourceException {
			fetches.incrementAndGet();
			if (fail)
				throw new KeySourceException("simulated network failure");
			return selector.select(set.get());
		}
	}

	// -----------------------------------------------------------------------------------------
	// Reflective helpers — updated to the 5-arg JwksCache constructor (OQ-10).
	// Default overloads pass eager=true / cooldown=10s matching the Builder defaults.
	// -----------------------------------------------------------------------------------------

	private static JwksCache newCache(JWKSource<SecurityContext> src, Duration ttl, Clock clock) throws Exception {
		return newCache(src, ttl, clock, true, Duration.ofSeconds(10));
	}

	private static JwksCache newCache(JWKSource<SecurityContext> src, Duration ttl, Clock clock,
			boolean eager, Duration cooldown) throws Exception {
		var ctor = JwksCache.class.getDeclaredConstructor(
			JWKSource.class, Duration.class, Clock.class, boolean.class, Duration.class);
		ctor.setAccessible(true);
		return (JwksCache) ctor.newInstance(src, ttl, clock, eager, cooldown);
	}

	private static JWKSource<SecurityContext> invokeCacheCtor(JWKSource<SecurityContext> src, Duration ttl, Clock clock) throws Exception {
		return invokeCacheCtor(src, ttl, clock, true, Duration.ofSeconds(10));
	}

	private static JWKSource<SecurityContext> invokeCacheCtor(JWKSource<SecurityContext> src, Duration ttl, Clock clock,
			boolean eager, Duration cooldown) throws Exception {
		Constructor<?> ctor = JwksCache.class.getDeclaredConstructor(
			JWKSource.class, Duration.class, Clock.class, boolean.class, Duration.class);
		ctor.setAccessible(true);
		@SuppressWarnings("unchecked")
		var cast = (JWKSource<SecurityContext>) ctor.newInstance(src, ttl, clock, eager, cooldown);
		return cast;
	}

	// -----------------------------------------------------------------------------------------
	// a-series: basic TTL / cache-hit / cache-miss mechanics.
	// -----------------------------------------------------------------------------------------

	@Test void a01_cacheHitWithinTtl_skipsUnderlyingFetch() throws Exception {
		var rsa = generateRsa("kid-1");
		var src = new CountingSource(new JWKSet(rsa.toPublicJWK()));
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

		var cache = newCache(src, Duration.ofMinutes(5), clock);
		var sel = new JWKSelector(new JWKMatcher.Builder().build());
		cache.get(sel, null);
		cache.get(sel, null);
		cache.get(sel, null);

		assertEquals(1, src.fetches.get(), "subsequent lookups within TTL must serve from cache");
	}

	@Test void a02_cacheMissAfterTtl_refetches() throws Exception {
		var rsa = generateRsa("kid-1");
		var src = new CountingSource(new JWKSet(rsa.toPublicJWK()));
		var t0 = Instant.parse("2026-01-01T00:00:00Z");
		var clockHolder = new AtomicReference<>(Clock.fixed(t0, ZoneOffset.UTC));
		var clock = new Clock() {
			@Override public ZoneId getZone() { return clockHolder.get().getZone(); }
			@Override public Clock withZone(ZoneId z) { return clockHolder.get().withZone(z); }
			@Override public Instant instant() { return clockHolder.get().instant(); }
		};

		var cache = newCache(src, Duration.ofMinutes(1), clock);
		var sel = new JWKSelector(new JWKMatcher.Builder().build());
		cache.get(sel, null);
		clockHolder.set(Clock.fixed(t0.plus(Duration.ofMinutes(2)), ZoneOffset.UTC));
		cache.get(sel, null);

		assertEquals(2, src.fetches.get(), "lookup past TTL must re-fetch from upstream");
	}

	// -----------------------------------------------------------------------------------------
	// b-series: graceful-degradation on JWKS-endpoint outage.
	// -----------------------------------------------------------------------------------------

	@Test void b01_gracefulDegradation_servesCachedKeysOnFetchFailure() throws Exception {
		var rsa = generateRsa("kid-1");
		var src = new CountingSource(new JWKSet(rsa.toPublicJWK()));
		var t0 = Instant.parse("2026-01-01T00:00:00Z");
		var clockHolder = new AtomicReference<>(Clock.fixed(t0, ZoneOffset.UTC));
		var clock = new Clock() {
			@Override public ZoneId getZone() { return clockHolder.get().getZone(); }
			@Override public Clock withZone(ZoneId z) { return clockHolder.get().withZone(z); }
			@Override public Instant instant() { return clockHolder.get().instant(); }
		};

		var cache = newCache(src, Duration.ofMinutes(1), clock);
		var sel = new JWKSelector(new JWKMatcher.Builder().build());
		var first = cache.get(sel, null);
		assertFalse(first.isEmpty(), "warm-up fetch must succeed");

		// Past TTL — but the upstream is now failing.
		clockHolder.set(Clock.fixed(t0.plus(Duration.ofMinutes(2)), ZoneOffset.UTC));
		src.fail = true;
		var second = cache.get(sel, null);

		assertFalse(second.isEmpty(), "must serve stale cached keys when upstream fetch fails past TTL");
		assertEquals(2, src.fetches.get(), "the failing refresh attempt must still be observed");
	}

	@Test void b02_failureWithoutPriorFetch_rethrows() throws Exception {
		var rsa = generateRsa("kid-1");
		var src = new CountingSource(new JWKSet(rsa.toPublicJWK()));
		src.fail = true;
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

		var cache = newCache(src, Duration.ofMinutes(1), clock);
		var sel = new JWKSelector(new JWKMatcher.Builder().build());

		assertThrows(KeySourceException.class, () -> cache.get(sel, null));
	}

	// -----------------------------------------------------------------------------------------
	// c-series: key-rotation end-to-end through JwtTokenValidator.
	// c01 — legacy/feature-off regression (eager disabled): mid-TTL kid miss must throw.
	// c02 — feature-on: mid-TTL kid miss succeeds after eager refresh.
	// -----------------------------------------------------------------------------------------

	@Test void c01_jwksRotation_picksUpNewKeysAfterTtl() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var t0 = Instant.parse("2026-01-01T00:00:00Z");
		var clockHolder = new AtomicReference<>(Clock.fixed(t0, ZoneOffset.UTC));
		var clock = new Clock() {
			@Override public ZoneId getZone() { return clockHolder.get().getZone(); }
			@Override public Clock withZone(ZoneId z) { return clockHolder.get().withZone(z); }
			@Override public Instant instant() { return clockHolder.get().instant(); }
		};

		// Eager refresh disabled — preserves pre-9.5.0 behavior for regression coverage.
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(invokeCacheCtor(src, Duration.ofMinutes(1), clock, false, Duration.ofSeconds(10)))
			.clock(clock)
			.build();

		// Sign with old key — accepted (only kid-old is in the JWKS).
		var oldToken = signRsa(oldKey, defaultClaims(clock).build());
		var p1 = validator.validate(oldToken);
		assertEquals("alice", p1.getName());

		// Rotate the JWKS to publish the new key only.
		src.set.set(new JWKSet(newKey.toPublicJWK()));

		// Sign with the new key — within TTL, the cache still has the old key, so it fails.
		var newTokenEarly = signRsa(newKey, defaultClaims(clock).build());
		assertThrows(AuthenticationException.class, () -> validator.validate(newTokenEarly));

		// Advance past TTL — cache refreshes, new-key tokens now accepted.
		clockHolder.set(Clock.fixed(t0.plus(Duration.ofMinutes(2)), ZoneOffset.UTC));
		var newTokenAfterRotation = signRsa(newKey, defaultClaims(clockHolder.get()).build());
		var p2 = validator.validate(newTokenAfterRotation);
		assertEquals("alice", p2.getName());
	}

	@Test void c02_eagerRefresh_freshCacheKidMiss_succeeds() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

		// Eager refresh enabled (default).
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(invokeCacheCtor(src, Duration.ofMinutes(5), clock))
			.clock(clock)
			.build();

		// Warm the cache with kid-old.
		var oldToken = signRsa(oldKey, defaultClaims(clock).build());
		assertEquals("alice", validator.validate(oldToken).getName());
		assertEquals(1, src.fetches.get());

		// IdP rotates: publishes {kid-old, kid-new}.
		src.set.set(new JWKSet(List.of(oldKey.toPublicJWK(), newKey.toPublicJWK())));

		// kid-new token presented within TTL — eager refresh fires, token validates.
		var newToken = signRsa(newKey, defaultClaims(clock).build());
		var p = validator.validate(newToken);
		assertEquals("alice", p.getName(), "kid-new token must validate after eager refresh");
		assertEquals(2, src.fetches.get(), "exactly one eager fetch");
	}

	// -----------------------------------------------------------------------------------------
	// d-series: JwksCache unit tests for the eager-refresh path.
	// -----------------------------------------------------------------------------------------

	/** Helper: mutable clock driven by a holder. */
	private static Clock holderClock(AtomicReference<Clock> holder) {
		return new Clock() {
			@Override public ZoneId getZone() { return holder.get().getZone(); }
			@Override public Clock withZone(ZoneId z) { return holder.get().withZone(z); }
			@Override public Instant instant() { return holder.get().instant(); }
		};
	}

	@Test void d01_eagerRefresh_freshCacheKidMiss_refetchesOnce() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var cache = newCache(src, Duration.ofMinutes(5), clock);

		// Warm with kid-old.
		var allSel = new JWKSelector(new JWKMatcher.Builder().build());
		var warm = cache.get(allSel, null);
		assertFalse(warm.isEmpty());
		assertEquals(1, src.fetches.get());

		// IdP rotates: publishes kid-new.
		src.set.set(new JWKSet(newKey.toPublicJWK()));

		// kid-new selector within TTL: should trigger eager refresh.
		var kidNewSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-new").build());
		var hits = cache.get(kidNewSel, null);

		assertFalse(hits.isEmpty(), "kid-new must be returned after eager refresh");
		assertEquals(2, src.fetches.get(), "exactly one eager fetch");
	}

	@Test void d02_eagerRefreshDisabled_preservesLegacyBehavior() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var cache = newCache(src, Duration.ofMinutes(5), clock, false, Duration.ofSeconds(10));

		var allSel = new JWKSelector(new JWKMatcher.Builder().build());
		cache.get(allSel, null);
		src.set.set(new JWKSet(newKey.toPublicJWK()));

		var kidNewSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-new").build());
		var hits = cache.get(kidNewSel, null);

		assertTrue(hits.isEmpty(), "kid-new must remain absent within TTL when eager is disabled");
		assertEquals(1, src.fetches.get(), "no extra fetch when eager is disabled");
	}

	@Test void d03_eagerRefresh_cooldownThrottlesRepeatedMisses() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var t0 = Instant.parse("2026-01-01T00:00:00Z");
		var clockHolder = new AtomicReference<>(Clock.fixed(t0, ZoneOffset.UTC));
		var clock = holderClock(clockHolder);
		var cooldown = Duration.ofSeconds(30);
		var cache = newCache(src, Duration.ofMinutes(5), clock, true, cooldown);

		// Warm with kid-old.
		cache.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
		assertEquals(1, src.fetches.get());

		// Rotate: only kid-new published.
		src.set.set(new JWKSet(newKey.toPublicJWK()));

		var kidMissSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-miss").build());

		// First miss within TTL: eager refresh fires (fetches == 2).
		cache.get(kidMissSel, null);
		assertEquals(2, src.fetches.get(), "first eager refresh must fire");

		// Second miss immediately (within cooldown): must be skipped.
		cache.get(kidMissSel, null);
		assertEquals(2, src.fetches.get(), "second miss within cooldown must not trigger another fetch");

		// Advance past cooldown: third miss should trigger another eager refresh.
		clockHolder.set(Clock.fixed(t0.plus(cooldown).plusSeconds(1), ZoneOffset.UTC));
		cache.get(kidMissSel, null);
		assertEquals(3, src.fetches.get(), "miss after cooldown expiry must trigger another eager fetch");
	}

	@Test void d04_eagerRefresh_endpointOutageRetainsLastKnownGood() throws Exception {
		var oldKey = generateRsa("kid-old");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var cache = newCache(src, Duration.ofMinutes(5), clock, true, Duration.ofSeconds(10));

		// Warm with kid-old.
		var oldSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-old").build());
		var allSel = new JWKSelector(new JWKMatcher.Builder().build());
		cache.get(allSel, null);
		assertEquals(1, src.fetches.get());

		// Make the endpoint fail.
		src.fail = true;

		// kid-miss selector within TTL: eager refresh fires but fails; last-known-good retained.
		var kidMissSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-unknown").build());
		var missing = cache.get(kidMissSel, null);
		assertTrue(missing.isEmpty(), "unknown kid must return empty when eager refresh fails");
		assertEquals(2, src.fetches.get(), "one failed eager refresh attempt");

		// Old key must still be available from the retained cache.
		var retained = cache.get(oldSel, null);
		assertFalse(retained.isEmpty(), "last-known-good keys must still be served after a failed eager refresh");

		// Immediate repeat miss must be throttled by cooldown (no additional fetch).
		cache.get(kidMissSel, null);
		assertEquals(2, src.fetches.get(), "cooldown must prevent a second eager fetch immediately after failure");
	}

	@Test void d05_eagerRefresh_stillMissingAfterRefresh_noDoubleAttempt() throws Exception {
		var oldKey = generateRsa("kid-old");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var cache = newCache(src, Duration.ofMinutes(5), clock, true, Duration.ofSeconds(10));

		// Warm with kid-old.
		cache.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
		assertEquals(1, src.fetches.get());

		// Rotate: source keeps kid-old only (simulates IdP not yet publishing kid-new).
		var kidNewSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-new").build());

		// First miss: eager refresh fires, returns kid-old set, but kid-new still absent → empty.
		var first = cache.get(kidNewSel, null);
		assertTrue(first.isEmpty(), "still-missing kid must produce empty after one refresh");
		assertEquals(2, src.fetches.get(), "one eager fetch");

		// Immediate repeat: must be throttled by cooldown.
		cache.get(kidNewSel, null);
		assertEquals(2, src.fetches.get(), "no extra fetch within cooldown after still-missing refresh");
	}

	@Test void d06_eagerRefresh_successResetsTtlWindow() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");
		var src = new CountingSource(new JWKSet(oldKey.toPublicJWK()));
		var t0 = Instant.parse("2026-01-01T00:00:00Z");
		var ttl = Duration.ofMinutes(5);
		var clockHolder = new AtomicReference<>(Clock.fixed(t0, ZoneOffset.UTC));
		var clock = holderClock(clockHolder);
		var cache = newCache(src, ttl, clock, true, Duration.ofSeconds(10));

		// Warm at t0.
		var allSel = new JWKSelector(new JWKMatcher.Builder().build());
		cache.get(allSel, null);
		assertEquals(1, src.fetches.get());

		// Rotate at t0+1min: IdP publishes {kid-old, kid-new}.
		clockHolder.set(Clock.fixed(t0.plusSeconds(60), ZoneOffset.UTC));
		src.set.set(new JWKSet(List.of(oldKey.toPublicJWK(), newKey.toPublicJWK())));

		// Eager refresh fires at t0+1min (still within original TTL window).
		var kidNewSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-new").build());
		var hits = cache.get(kidNewSel, null);
		assertFalse(hits.isEmpty(), "kid-new must be found after eager refresh");
		assertEquals(2, src.fetches.get());

		// Move to t0+4min (just before the ORIGINAL TTL boundary, but well within the RESET window).
		// If the TTL was reset at t0+1min, the next periodic refresh is due at t0+6min.
		clockHolder.set(Clock.fixed(t0.plusSeconds(240), ZoneOffset.UTC));
		cache.get(allSel, null);
		assertEquals(2, src.fetches.get(), "TTL window must be reset by the eager refresh; no extra fetch at t0+4min");
	}

	@Test void d07_eagerRefresh_singleInFlight_noThunderingHerd() throws Exception {
		var oldKey = generateRsa("kid-old");
		var newKey = generateRsa("kid-new");

		// Latch that holds the eager-refresh delegate call until we release it.
		var fetchLatch = new CountDownLatch(1);
		var warmupDone = new AtomicBoolean(false);
		var fetchCount = new AtomicInteger();

		JWKSource<SecurityContext> src = (selector, ctx) -> {
			fetchCount.incrementAndGet();
			if (!warmupDone.get())
				return selector.select(new JWKSet(oldKey.toPublicJWK()));
			// Eager-refresh fetch: block until the test releases the latch.
			try { fetchLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			return selector.select(new JWKSet(List.of(oldKey.toPublicJWK(), newKey.toPublicJWK())));
		};

		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var cache = newCache(src, Duration.ofMinutes(5), clock, true, Duration.ofSeconds(10));

		// Warm the cache.
		cache.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
		assertEquals(1, fetchCount.get());
		warmupDone.set(true);

		// N threads simultaneously present a kid-new miss.
		int n = 6;
		var barrier = new CyclicBarrier(n);
		var executor = Executors.newFixedThreadPool(n);
		var kidNewSel = new JWKSelector(new JWKMatcher.Builder().keyID("kid-new").build());
		var futures = new ArrayList<Future<List<JWK>>>();
		for (int i = 0; i < n; i++) {
			futures.add(executor.submit(() -> {
				barrier.await();  // all threads start simultaneously
				return cache.get(kidNewSel, null);
			}));
		}

		// Allow threads to queue up on the blocked fetch, then release.
		Thread.sleep(250);
		fetchLatch.countDown();

		executor.shutdown();
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

		int successCount = 0;
		for (var f : futures) {
			if (!f.get().isEmpty())
				successCount++;
		}

		assertEquals(2, fetchCount.get(), "exactly one eager fetch regardless of concurrent misses");
		assertEquals(n, successCount, "all concurrent threads must see the refreshed key");
	}
}
