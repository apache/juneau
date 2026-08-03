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

/**
 * Holds the lazily-initialized, per-process shared default {@link McpMrtrConfig} returned by the
 * {@link McpEndpoint#mrtrConfig()} mixin default.
 *
 * <p>
 * The mixin's {@code revision()} constructs a fresh {@link McpRevision} on every request, so a default that
 * returned a fresh {@link McpMrtrConfig} each call would mint a fresh {@link AeadRequestStateCodec} AES key
 * per request &mdash; a {@code requestState} sealed on the PAUSE request could never be unsealed on the
 * follow-up RESUME request. Returning this single shared instance instead means the whole JVM shares one
 * ephemeral AES key across every mixin deployment, which is exactly the "per-process ephemeral key" design
 * intent and makes RESUME work out of the box without an override.
 *
 * <p>
 * Uses the initialization-on-demand holder idiom: the instance (and its single per-JVM key) is created the
 * first time {@link #get()} is called &mdash; i.e. the first mixin {@code mrtrConfig()} call &mdash; and the
 * JVM guarantees that class initialization is thread-safe, so no explicit synchronization is needed.
 */
final class SharedMrtrConfig {

	private SharedMrtrConfig() {}

	private static final class Holder {
		static final McpMrtrConfig INSTANCE = new McpMrtrConfig();
	}

	/**
	 * Returns the per-process shared default MRTR configuration, creating it on first access.
	 *
	 * @return The shared configuration. Never <jk>null</jk>.
	 */
	static McpMrtrConfig get() {
		return Holder.INSTANCE;
	}
}
