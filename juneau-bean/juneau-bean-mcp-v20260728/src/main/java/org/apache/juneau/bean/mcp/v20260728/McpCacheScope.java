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
 * SEP-2549 cache-hint authorization scope for a {@link CacheableResult} (MCP wire uses lowercase strings).
 *
 * <p>
 * {@link #PUBLIC} means the result is not user-specific and may be shared across authentication/authorization
 * contexts. {@link #PRIVATE} means the result is user- or authorization-specific and MUST NOT be shared across
 * authentication/authorization contexts.
 *
 * <p>
 * The protocol default for an absent {@code cacheScope} is {@code public}, but that is an interpretation rule for
 * consumers, not a serialization default: Juneau never materializes {@code "cacheScope":"public"} when the
 * property is unset.
 */
public enum McpCacheScope {

	/** Result is not user-specific; may be shared across authentication/authorization contexts. */
	PUBLIC("public"),

	/** Result is user- or authorization-specific; MUST NOT be shared across authentication/authorization contexts. */
	PRIVATE("private");

	private final String wire;

	McpCacheScope(String wire) {
		this.wire = wire;
	}

	/**
	 * Wire token for JSON payloads.
	 *
	 * @return Lowercase MCP cache-scope string.
	 */
	public String toWire() {
		return wire;
	}

	@Override /* Object */
	public String toString() {
		return wire;
	}
}
