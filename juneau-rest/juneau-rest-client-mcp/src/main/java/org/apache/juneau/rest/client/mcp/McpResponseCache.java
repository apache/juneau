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

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;

/**
 * An opt-in, off-by-default response cache for MCP client results.
 *
 * <p>
 * Keyed by an opaque {@code scope} (a partition key — a revision-specific adapter defines its own scope wire
 * vocabulary, e.g. the {@code 2026-07-28} adapter's {@code org.apache.juneau.bean.mcp.v20260728.McpCacheScope}
 * {@code "private"}/{@code "public"} cache-hint values; this neutral core treats the string as fully opaque and
 * defines no scope constants of its own) plus a {@code key} (typically method + canonical params, computed by
 * the caller).
 *
 * <p>
 * <b>Stored value shape:</b> this SPI makes no assumption about what a cached {@code value} is — a caller may
 * store the raw JSON-RPC {@code result} (e.g. via {@link #putResult(String, String, JsonRpcResponse, long)}) or
 * a typed result bean it has already re-marshaled (e.g. the {@code 2026-07-28} client adapter, which caches the
 * typed bean returned to its caller). Whatever shape a caller stores under a given {@code key} is exactly the
 * shape it must expect back from {@link #get(String, String)} for that same {@code key} — this interface does
 * not normalize or re-marshal on either side.
 *
 * <p>
 * <b>Identity:</b> implementations must return the exact {@code value} instance passed to {@link #put(String,
 * String, Object, long)} from a subsequent {@link #get(String, String)} for the same partition/key (process-local
 * implementations naturally satisfy this; an external/serializing store would not, and a caller layering one in
 * must re-marshal on read to preserve this contract). Cached values may be handed out to more than one caller
 * concurrently; callers must not mutate a returned value.
 *
 * <p>
 * Implementations are free to be process-local ({@link InMemoryMcpResponseCache}, the built-in default) or backed
 * by an external store; the SPI itself makes no assumption about storage.
 *
 * @since 10.0.0
 */
public interface McpResponseCache {

	/**
	 * Looks up a cached value.
	 *
	 * @param scope The cache-scope partition key. Must not be <jk>null</jk>.
	 * @param key The cache key (typically method + canonical params). Must not be <jk>null</jk>.
	 * @return The cached value, or {@link Optional#empty()} if absent or expired.
	 */
	Optional<Object> get(String scope, String key);

	/**
	 * Stores a value.
	 *
	 * @param scope The cache-scope partition key. Must not be <jk>null</jk>.
	 * @param key The cache key (typically method + canonical params). Must not be <jk>null</jk>.
	 * @param value The value to cache. Must not be <jk>null</jk>.
	 * @param ttlMs The time-to-live in milliseconds. A value {@code <= 0} means the entry never expires.
	 */
	void put(String scope, String key, Object value, long ttlMs);

	/**
	 * Convenience entry point for an adapter that wants to cache the raw JSON-RPC {@code result} straight off a
	 * {@link JsonRpcResponse}: stores it, or does nothing if the response is <jk>null</jk>, carries a JSON-RPC
	 * {@code error}, or has a <jk>null</jk> {@code result}.
	 *
	 * <p>
	 * Callers never need to check for an error response themselves before caching, since this method never
	 * forwards one to {@link #put(String, String, Object, long)}. Note that this caches the raw wire
	 * {@code result} (e.g. a {@code Map}), not a typed result bean — an adapter that instead caches an
	 * already-re-marshaled typed bean (as the {@code 2026-07-28} client adapter does) should call
	 * {@link #put(String, String, Object, long)} directly instead of this method.
	 *
	 * @param scope The cache-scope partition key. Must not be <jk>null</jk>.
	 * @param key The cache key (typically method + canonical params). Must not be <jk>null</jk>.
	 * @param response The JSON-RPC response to conditionally cache. May be <jk>null</jk>.
	 * @param ttlMs The time-to-live in milliseconds. A value {@code <= 0} means the entry never expires.
	 */
	default void putResult(String scope, String key, JsonRpcResponse response, long ttlMs) {
		if (response == null || response.getError() != null)
			return;
		var result = response.getResult();
		if (result == null)
			return;
		put(scope, key, result, ttlMs);
	}

	/**
	 * Removes all cached entries across all scopes.
	 */
	void clear();
}
