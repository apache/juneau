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
package org.apache.juneau.rest.client.mcp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.function.*;

/**
 * Built-in, process-local, in-memory default implementation of {@link McpResponseCache}.
 *
 * <p>
 * Entries are partitioned by {@code scope}, so the same {@code key} in two different scopes never collides.
 * A non-positive {@code ttlMs} passed to {@link #put(String, String, Object, long)} means the entry never expires.
 *
 * <h5 class='section'>Size bound / eviction policy:</h5>
 * <p>
 * The cache is bounded at {@link #DEFAULT_MAX_ENTRIES} total entries (across all scopes combined) by default,
 * overridable via {@link #InMemoryMcpResponseCache(int)}. Once the cap is reached, inserting one more entry
 * evicts the single <b>least-recently-used</b> entry (an access via {@link #get(String, String)}, whether a hit
 * or a still-live entry, counts as a use; an expired-and-evicted-on-read entry does not) — a bounded default
 * that never grows unboundedly under sustained cache traffic, without needing every caller to reason about a
 * capacity setting themselves.
 *
 * <p>
 * This class is thread-safe (a synchronized {@link LinkedHashMap} in access-order/LRU mode).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // ARG_-prefixed assertion-param constants use the project's UPPER_camelCase convention (ARG_<param>).
})
public class InMemoryMcpResponseCache implements McpResponseCache {

	/**
	 * Default maximum number of entries (across all scopes combined) retained before the least-recently-used
	 * entry is evicted to make room for a new one. See the class javadoc's size-bound/eviction-policy note.
	 */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;

	// Argument name constants for assertArgNotNull
	private static final String ARG_scope = "scope";
	private static final String ARG_key = "key";
	private static final String ARG_value = "value";
	private static final String ARG_clock = "clock";

	private record CacheEntry(Object value, long expiresAtMs) {}

	private final Map<String,CacheEntry> entries;
	private final LongSupplier clock;

	/**
	 * Constructor. Uses the system clock ({@link System#currentTimeMillis()}) for TTL expiry and
	 * {@link #DEFAULT_MAX_ENTRIES} as the size cap.
	 */
	public InMemoryMcpResponseCache() {
		this(System::currentTimeMillis, DEFAULT_MAX_ENTRIES);
	}

	/**
	 * Constructor with an explicit size cap. Uses the system clock ({@link System#currentTimeMillis()}) for TTL
	 * expiry.
	 *
	 * @param maxEntries The maximum number of entries (across all scopes combined) retained before the
	 * 	least-recently-used entry is evicted. Must be positive.
	 */
	public InMemoryMcpResponseCache(int maxEntries) {
		this(System::currentTimeMillis, maxEntries);
	}

	/**
	 * Constructor with an injectable clock, for deterministic TTL-expiry testing. Uses {@link #DEFAULT_MAX_ENTRIES}
	 * as the size cap.
	 *
	 * @param clock Supplies the current time in milliseconds. Must not be <jk>null</jk>.
	 */
	InMemoryMcpResponseCache(LongSupplier clock) {
		this(clock, DEFAULT_MAX_ENTRIES);
	}

	/**
	 * Constructor with both an injectable clock and an explicit size cap, for deterministic testing of the
	 * eviction policy.
	 *
	 * @param clock Supplies the current time in milliseconds. Must not be <jk>null</jk>.
	 * @param maxEntries The maximum number of entries (across all scopes combined) retained before the
	 * 	least-recently-used entry is evicted. Must be positive.
	 */
	InMemoryMcpResponseCache(LongSupplier clock, int maxEntries) {
		this.clock = assertArgNotNull(ARG_clock, clock);
		if (maxEntries <= 0)
			throw new IllegalArgumentException("maxEntries must be positive: " + maxEntries);
		this.entries = Collections.synchronizedMap(new LinkedHashMap<String,CacheEntry>(16, 0.75f, true) {
			@Override protected boolean removeEldestEntry(Map.Entry<String,CacheEntry> eldest) {
				return size() > maxEntries;
			}
		});
	}

	@Override /* Overridden from McpResponseCache */
	public Optional<Object> get(String scope, String key) {
		assertArgNotNull(ARG_scope, scope);
		assertArgNotNull(ARG_key, key);
		var partitionKey = partitionKey(scope, key);
		synchronized (entries) {
			var entry = entries.get(partitionKey);
			if (entry == null)
				return Optional.empty();
			if (entry.expiresAtMs() > 0 && entry.expiresAtMs() <= clock.getAsLong()) {
				entries.remove(partitionKey);
				return Optional.empty();
			}
			return Optional.of(entry.value());
		}
	}

	@Override /* Overridden from McpResponseCache */
	public void put(String scope, String key, Object value, long ttlMs) {
		assertArgNotNull(ARG_scope, scope);
		assertArgNotNull(ARG_key, key);
		assertArgNotNull(ARG_value, value);
		var expiresAtMs = ttlMs > 0 ? clock.getAsLong() + ttlMs : 0L;
		entries.put(partitionKey(scope, key), new CacheEntry(value, expiresAtMs));
	}

	@Override /* Overridden from McpResponseCache */
	public void clear() {
		entries.clear();
	}

	private static String partitionKey(String scope, String key) {
		return scope + "\u0000" + key;
	}
}
