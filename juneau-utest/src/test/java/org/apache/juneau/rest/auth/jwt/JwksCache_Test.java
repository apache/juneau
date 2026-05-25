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

	private static JwksCache newCache(JWKSource<SecurityContext> src, Duration ttl, Clock clock) throws Exception {
		var ctor = JwksCache.class.getDeclaredConstructor(JWKSource.class, Duration.class, Clock.class);
		ctor.setAccessible(true);
		return (JwksCache) ctor.newInstance(src, ttl, clock);
	}

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

		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(invokeCacheCtor(src, Duration.ofMinutes(1), clock))
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

	private static JWKSource<SecurityContext> invokeCacheCtor(JWKSource<SecurityContext> src, Duration ttl, Clock clock) throws Exception {
		Constructor<?> ctor = JwksCache.class.getDeclaredConstructor(JWKSource.class, Duration.class, Clock.class);
		ctor.setAccessible(true);
		@SuppressWarnings("unchecked")
		var cast = (JWKSource<SecurityContext>) ctor.newInstance(src, ttl, clock);
		return cast;
	}
}
