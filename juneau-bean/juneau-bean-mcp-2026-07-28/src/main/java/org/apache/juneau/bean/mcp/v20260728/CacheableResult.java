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
package org.apache.juneau.bean.mcp.v20260728;

/**
 * Shared SEP-2549 cache-hint base for the five cacheable {@code 2026-07-28} result carriers
 * ({@code tools/list}, {@code prompts/list}, {@code resources/list}, {@code resources/templates/list}, and
 * {@code resources/read}).
 *
 * <p>
 * Owns two nullable, independent, top-level wire properties that are peers of each subclass's own payload
 * properties:
 * <ul>
 * 	<li>{@code ttlMs} - Freshness duration in milliseconds ({@code Cache-Control: max-age} semantics).
 * 		{@code 0} means immediately stale.
 * 	<li>{@code cacheScope} - {@link McpCacheScope#PUBLIC} or {@link McpCacheScope#PRIVATE}. An absent value is
 * 		interpreted by consumers as {@code public}, but that interpretation is never materialized on the wire.
 * </ul>
 *
 * <p>
 * This base is a lossless wire carrier only: it performs no {@code ttlMs} validation and does not synthesize a
 * default {@code cacheScope}. Wire beans must be able to parse and round-trip arbitrary protocol payloads
 * (including a negative parsed {@code ttlMs}) independently of any server-side configuration policy. TTL
 * validation for values the server itself emits is owned by the {@code 2026-07-28} adapter's cache
 * configuration, not by this bean.
 *
 * <p>
 * The CRTP type parameter lets each concrete subclass's cache setters return its own concrete type for fluent
 * chaining, without duplicating the two cache accessors in every subclass.
 *
 * @param <T> The concrete subclass, for fluent-setter self-typing.
 */
public abstract class CacheableResult<T extends CacheableResult<T>> {

	private Integer ttlMs;
	private McpCacheScope cacheScope;

	/**
	 * Cache freshness duration in milliseconds.
	 *
	 * @return The TTL, or {@code null} if not set.
	 */
	public Integer getTtlMs() {
		return ttlMs;
	}

	/**
	 * Sets the cache freshness duration in milliseconds.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property. {@code 0} is a valid, meaningful
	 * 	value meaning immediately stale.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({
		"unchecked" // CRTP subclasses bind T to their own concrete type.
	})
	public T setTtlMs(Integer value) {
		ttlMs = value;
		return (T)this;
	}

	/**
	 * Cache-hint authorization scope.
	 *
	 * @return The scope, or {@code null} if not set.
	 */
	public McpCacheScope getCacheScope() {
		return cacheScope;
	}

	/**
	 * Sets the cache-hint authorization scope.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({
		"unchecked" // CRTP subclasses bind T to their own concrete type.
	})
	public T setCacheScope(McpCacheScope value) {
		cacheScope = value;
		return (T)this;
	}
}
