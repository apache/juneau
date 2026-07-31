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
package org.apache.juneau.rest.server.mcp.v20260728;

import org.apache.juneau.bean.mcp.v20260728.McpCacheScope;

/**
 * A single {@code ttlMs}/{@code cacheScope} configuration pair applied to a v2 cacheable result.
 *
 * <p>
 * Both fields are nullable. A {@code null} {@link #getTtlMs()} means no TTL hint is emitted on the wire.
 * A {@code null} {@link #getCacheScope()} means the wire omits {@code cacheScope} entirely, which per
 * SEP-2549 is equivalent to {@code "public"} (public-by-absence).
 */
public class McpCacheHint {

	private Integer ttlMs;
	private McpCacheScope cacheScope;

	/**
	 * The cache TTL in milliseconds.
	 *
	 * @return The TTL, or {@code null} if no TTL hint is configured.
	 */
	public Integer getTtlMs() {
		return ttlMs;
	}

	/**
	 * Sets the cache TTL in milliseconds.
	 *
	 * @param value The new value. Can be {@code null} to unset the property. Must not be negative.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is negative.
	 */
	public McpCacheHint setTtlMs(Integer value) {
		if (value != null && value < 0)
			throw new IllegalArgumentException("ttlMs " + value + " is below minimum 0");
		ttlMs = value;
		return this;
	}

	/**
	 * The cache scope.
	 *
	 * @return The scope, or {@code null} if not set (public-by-absence).
	 */
	public McpCacheScope getCacheScope() {
		return cacheScope;
	}

	/**
	 * Sets the cache scope.
	 *
	 * @param value The new value. Can be {@code null} to unset the property (public-by-absence).
	 * @return This object.
	 */
	public McpCacheHint setCacheScope(McpCacheScope value) {
		cacheScope = value;
		return this;
	}
}
