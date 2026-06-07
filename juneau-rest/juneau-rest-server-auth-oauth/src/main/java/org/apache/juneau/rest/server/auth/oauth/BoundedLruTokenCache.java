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
package org.apache.juneau.rest.server.auth.oauth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.security.*;
import java.time.*;
import java.util.*;

/**
 * Thread-safe bounded LRU implementation of {@link TokenCache}.
 *
 * <p>
 * Backed by a {@link LinkedHashMap} configured with {@code accessOrder=true} so iteration order tracks
 * most-recently-used.  Eviction policy is the conjunction of:
 *
 * <ul>
 * 	<li><b>Size cap</b> &mdash; when the entry count would exceed {@code maxEntries}, the least-recently-used
 * 		entry is removed before the new entry is inserted.  Default {@code maxEntries=1000}.
 * 	<li><b>TTL eviction</b> &mdash; on lookup, an entry whose stored expiry instant has passed (relative to
 * 		the supplied {@code now} clock) is removed and treated as a miss.
 * </ul>
 *
 * @since 10.0.0
 */
public class BoundedLruTokenCache implements TokenCache {

	/** Default maximum entry count. */
	public static final int DEFAULT_MAX_ENTRIES = 1000;

	/**
	 * Static creator with the default max-entries.
	 *
	 * @return A new cache.
	 */
	public static BoundedLruTokenCache create() {
		return new BoundedLruTokenCache(DEFAULT_MAX_ENTRIES);
	}

	/**
	 * Static creator with a custom max-entries.
	 *
	 * @param maxEntries The maximum entry count.  Must be positive.
	 * @return A new cache.
	 */
	public static BoundedLruTokenCache create(int maxEntries) {
		if (maxEntries <= 0)
			throw new IllegalArgumentException("maxEntries must be positive (was " + maxEntries + ")");
		return new BoundedLruTokenCache(maxEntries);
	}

	private final int maxEntries;
	private final Map<String,Object> entries;
	private final Object lock = new Object();

	/**
	 * Constructor.
	 *
	 * @param maxEntries The maximum entry count.
	 */
	protected BoundedLruTokenCache(int maxEntries) {
		this.maxEntries = maxEntries;
		this.entries = new LinkedHashMap<>(16, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<String,Object> eldest) {
				return size() > BoundedLruTokenCache.this.maxEntries;
			}
		};
	}

	/**
	 * Returns the configured maximum entry count.
	 *
	 * @return The maximum entry count.
	 */
	public int getMaxEntries() {
		return maxEntries;
	}

	@Override /* Overridden from TokenCache */
	public void putPrincipal(String key, Principal principal, Duration ttl) {
		assertArgNotNullOrBlank("key", key);
		assertArgNotNull("principal", principal);
		assertArgNotNull("ttl", ttl);
		if (ttl.isZero() || ttl.isNegative())
			throw new IllegalArgumentException("ttl must be positive (was " + ttl + ")");
		var expiresAt = Instant.now().plus(ttl);
		synchronized (lock) {
			entries.put(key, new CachedPrincipal(principal, expiresAt));
		}
	}

	@Override /* Overridden from TokenCache */
	public Optional<Principal> getPrincipal(String key) {
		assertArgNotNullOrBlank("key", key);
		synchronized (lock) {
			var v = entries.get(key);
			if (!(v instanceof CachedPrincipal cp))
				return opte();
			if (!Instant.now().isBefore(cp.expiresAt())) {
				entries.remove(key);
				return opte();
			}
			return opt(cp.principal());
		}
	}

	@Override /* Overridden from TokenCache */
	public void putToken(String key, OAuthToken token) {
		assertArgNotNullOrBlank("key", key);
		assertArgNotNull("token", token);
		synchronized (lock) {
			entries.put(key, token);
		}
	}

	@Override /* Overridden from TokenCache */
	public Optional<OAuthToken> getToken(String key, Instant now, Duration skew) {
		assertArgNotNullOrBlank("key", key);
		assertArgNotNull("now", now);
		assertArgNotNull("skew", skew);
		if (skew.isNegative())
			throw new IllegalArgumentException("skew must be non-negative (was " + skew + ")");
		synchronized (lock) {
			var v = entries.get(key);
			if (!(v instanceof OAuthToken t))
				return opte();
			if (t.isExpired(now, skew)) {
				entries.remove(key);
				return opte();
			}
			return opt(t);
		}
	}

	@Override /* Overridden from TokenCache */
	public void invalidate(String key) {
		assertArgNotNullOrBlank("key", key);
		synchronized (lock) {
			entries.remove(key);
		}
	}

	@Override /* Overridden from TokenCache */
	public int size() {
		synchronized (lock) {
			return entries.size();
		}
	}
}
