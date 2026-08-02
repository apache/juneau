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
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Built-in, process-local, in-memory default implementation of {@link McpResponseCache}.
 *
 * <p>
 * Entries are partitioned by {@code scope}, so the same {@code key} in two different scopes never collides.
 * A non-positive {@code ttlMs} passed to {@link #put(String, String, Object, long)} means the entry never expires.
 *
 * <p>
 * This class is thread-safe (backed by a {@link ConcurrentHashMap}).
 *
 * @since 10.0.0
 */
public class InMemoryMcpResponseCache implements McpResponseCache {

	// Argument name constants for assertArgNotNull
	private static final String ARG_scope = "scope";
	private static final String ARG_key = "key";
	private static final String ARG_value = "value";
	private static final String ARG_clock = "clock";

	private record Entry(Object value, long expiresAtMs) {}

	private final Map<String,Entry> entries = new ConcurrentHashMap<>();
	private final LongSupplier clock;

	/**
	 * Constructor. Uses the system clock ({@link System#currentTimeMillis()}) for TTL expiry.
	 */
	public InMemoryMcpResponseCache() {
		this(System::currentTimeMillis);
	}

	/**
	 * Constructor with an injectable clock, for deterministic TTL-expiry testing.
	 *
	 * @param clock Supplies the current time in milliseconds. Must not be <jk>null</jk>.
	 */
	InMemoryMcpResponseCache(LongSupplier clock) {
		this.clock = assertArgNotNull(ARG_clock, clock);
	}

	@Override /* Overridden from McpResponseCache */
	public Optional<Object> get(String scope, String key) {
		assertArgNotNull(ARG_scope, scope);
		assertArgNotNull(ARG_key, key);
		var partitionKey = partitionKey(scope, key);
		var entry = entries.get(partitionKey);
		if (entry == null)
			return Optional.empty();
		if (entry.expiresAtMs() > 0 && entry.expiresAtMs() <= clock.getAsLong()) {
			// Two-arg remove only evicts the entry we just read; a concurrent put() of a fresh entry for the
			// same partition key between our get() and this remove() is preserved instead of being dropped.
			entries.remove(partitionKey, entry);
			return Optional.empty();
		}
		return Optional.of(entry.value());
	}

	@Override /* Overridden from McpResponseCache */
	public void put(String scope, String key, Object value, long ttlMs) {
		assertArgNotNull(ARG_scope, scope);
		assertArgNotNull(ARG_key, key);
		assertArgNotNull(ARG_value, value);
		var expiresAtMs = ttlMs > 0 ? clock.getAsLong() + ttlMs : 0L;
		entries.put(partitionKey(scope, key), new Entry(value, expiresAtMs));
	}

	@Override /* Overridden from McpResponseCache */
	public void clear() {
		entries.clear();
	}

	private static String partitionKey(String scope, String key) {
		return scope + "\u0000" + key;
	}
}
