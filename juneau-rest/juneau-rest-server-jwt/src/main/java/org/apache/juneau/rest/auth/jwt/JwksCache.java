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

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;

/**
 * Time-bounded cache of {@link JWKSet} keys with graceful-degradation on fetch failure.
 *
 * <p>
 * Wraps a {@link JWKSource} so that:
 * <ul>
 * 	<li>The full key set is fetched from the underlying source no more than once per {@code ttl}.
 * 	<li>If the underlying source fails (network down, JWKS endpoint returning 5xx), the cache
 * 		continues to return the last-known-good key set with a warning logged at {@code WARNING}
 * 		level. This avoids correlated auth outages caused by transient network blips.
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
 * @since 9.5.0
 */
final class JwksCache implements JWKSource<SecurityContext> {

	/** Module logger. Warnings on JWKS fetch failures are intentionally loud per Resolved Decision #2. */
	private static final Logger LOG = Logger.getLogger(JwksCache.class.getName());

	/** Selector that pulls every key from the upstream source. */
	private static final JWKSelector SELECT_ALL = new JWKSelector(new JWKMatcher.Builder().build());

	private final JWKSource<SecurityContext> delegate;
	private final Duration ttl;
	private final Clock clock;
	private final AtomicReference<Entry> slot = new AtomicReference<>();

	JwksCache(JWKSource<SecurityContext> delegate, Duration ttl, Clock clock) {
		this.delegate = delegate;
		this.ttl = ttl;
		this.clock = clock;
	}

	@Override /* Overridden from JWKSource */
	public List<JWK> get(JWKSelector selector, SecurityContext ctx) throws KeySourceException {
		var now = clock.instant();
		var current = slot.get();
		if (current != null && current.fetchedAt.plus(ttl).isAfter(now))
			return selector.select(current.jwkSet);
		try {
			var refreshed = new JWKSet(delegate.get(SELECT_ALL, ctx));
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

	private record Entry(JWKSet jwkSet, Instant fetchedAt) {}
}
