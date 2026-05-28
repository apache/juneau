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
package org.apache.juneau.rest.auth.oauth;

import java.security.Principal;
import java.time.*;
import java.util.Optional;

/**
 * SPI for caching the result of an OAuth introspection or token-acquisition call.
 *
 * <p>
 * Two value shapes are supported via dedicated entry types: validator-side caches store
 * {@link CachedPrincipal} (used by {@link OAuthIntrospectionValidator}); flow-side caches store
 * {@link OAuthToken}.  Implementations are not required to support both surfaces &mdash; a Redis-backed
 * impl can serialize either via JSON, an in-memory impl can hold both.  The default impl
 * {@link BoundedLruTokenCache} supports both.
 *
 * <p>
 * Implementations must be thread-safe.
 *
 * @since 9.5.0
 */
public interface TokenCache {

	/**
	 * Stores or replaces a principal entry.
	 *
	 * @param key The cache key (typically the opaque token string).  Never {@code null}.
	 * @param principal The principal to cache.  Never {@code null}.
	 * @param ttl The maximum cache lifetime relative to "now".  Must be positive.
	 */
	void putPrincipal(String key, Principal principal, Duration ttl);

	/**
	 * Retrieves a non-expired principal entry.
	 *
	 * @param key The cache key.
	 * @return The cached principal, or {@link Optional#empty()} if absent or expired.
	 */
	Optional<Principal> getPrincipal(String key);

	/**
	 * Stores or replaces a token entry.
	 *
	 * @param key The cache key (typically a stable key derived from the flow parameters).
	 * @param token The token to cache.  Never {@code null}.
	 */
	void putToken(String key, OAuthToken token);

	/**
	 * Retrieves a non-expired token entry.
	 *
	 * @param key The cache key.
	 * @param now The reference instant for expiry comparison.
	 * @param skew Skew tolerance subtracted from the entry's expiry.  Must be non-negative.
	 * @return The cached token, or {@link Optional#empty()} if absent or expired.
	 */
	Optional<OAuthToken> getToken(String key, Instant now, Duration skew);

	/**
	 * Removes the entry under the supplied key.  No-op if absent.
	 *
	 * @param key The cache key.
	 */
	void invalidate(String key);

	/**
	 * Returns the current entry count.  Primarily for tests and metrics.
	 *
	 * @return The entry count.
	 */
	int size();
}
