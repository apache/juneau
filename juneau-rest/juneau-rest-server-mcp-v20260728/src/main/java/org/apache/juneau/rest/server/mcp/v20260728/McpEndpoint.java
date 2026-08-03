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

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.rest.server.tracing.TraceContextExtractor;

/**
 * Mixin interface that exposes a {@code 2026-07-28} MCP endpoint at {@code POST /mcp} on any Juneau REST
 * resource.
 *
 * <p>
 * The mixin path and the servlet-subclass path ({@link McpRestServlet}) are at parity: both bind the
 * revision at compile time and both expose the same {@link #capabilities()} hook.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/api")
 * public class MyResource extends BasicRestServlet implements McpEndpoint {
 *     @Override
 *     public McpServerConfig getMcpConfig() {
 *         return new McpServerConfig().addTool(new MyEchoTool());
 *     }
 * }
 * </pre>
 */
@SuppressWarnings({
	"java:S2176" // Intentional: dated adapter binding classes are de-versioned and differentiated by package (see TODO-312).
})
public interface McpEndpoint extends org.apache.juneau.rest.server.mcp.McpEndpoint {

	@Override /* McpEndpoint */
	default org.apache.juneau.rest.server.mcp.McpRevision revision() {
		return new McpRevision(capabilities(), Objects.requireNonNull(cacheConfig(), "cacheConfig"), instructions(),
			Objects.requireNonNull(mrtrConfig(), "mrtrConfig"));
	}

	/**
	 * Publishes this revision's stable {@link TraceContextExtractor} so an active {@code TracerHook}
	 * (via {@code RestOpInvoker}) recognizes this endpoint's resolved {@code JsonRpcRequest} argument
	 * and its {@code params._meta} carrier before span creation.
	 *
	 * <p>
	 * A no-op when no {@code TracerHook} bean is registered &mdash; see the no-tracer fast-path
	 * contract documented on {@code TracerHook}.
	 *
	 * @return This revision's shared {@link TraceContextExtractor}. Never <jk>null</jk>.
	 */
	@Bean
	default TraceContextExtractor mcpTraceContextExtractor() {
		return McpRevision.TRACE_CONTEXT_EXTRACTOR;
	}

	/**
	 * Optional explicit capabilities advertisement for {@code server/discover}.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) leaves capabilities auto-derived from the registered
	 * tool / prompt / resource lists; a non-<jk>null</jk> value bypasses auto-derivation.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive.
	 */
	default ServerCapabilities capabilities() {
		return null;
	}

	/**
	 * Optional free-form {@code server/discover} usage instructions advertised to clients.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) omits the {@code instructions} member from the discovery
	 * result.
	 *
	 * @return The instructions, or <jk>null</jk> to omit them.
	 */
	default String instructions() {
		return null;
	}

	/**
	 * Cache configuration for this endpoint's bound revision.
	 *
	 * <p>
	 * The default returns an empty {@link McpCacheConfig} (no cache hints emitted on any result).
	 * Override to supply TTL/scope hints. An overriding implementation must return a stable instance
	 * that is not mutated after it is returned — this mixin does not lazily cache the result the way
	 * the servlet-subclass path does.
	 *
	 * @return The cache configuration. Must not be <jk>null</jk>.
	 */
	default McpCacheConfig cacheConfig() {
		return new McpCacheConfig();
	}

	/**
	 * MRTR (Multi-Round-Trip Request) configuration for this endpoint's bound revision.
	 *
	 * <p>
	 * The default returns a <b>per-process shared</b> {@link McpMrtrConfig} (AES-GCM ephemeral codec, 5-minute
	 * {@code requestState} TTL, 10-round cap) &mdash; a single JVM-wide instance holding one ephemeral AES key,
	 * lazily created on first use (see {@link SharedMrtrConfig}). Because {@link #revision()} constructs a fresh
	 * {@link McpRevision} on every request, returning that same shared instance from every call is what lets a
	 * {@code requestState} sealed on a PAUSE request be unsealed on the follow-up RESUME request; a fresh
	 * {@link McpMrtrConfig} per call would mint a fresh AES key per request and make every RESUME fail. So the
	 * common mixin case is resumable out of the box with no override.
	 *
	 * <p>
	 * Override to supply a custom {@link RequestStateCodec}, TTL, or max-rounds cap. <b>An overriding
	 * implementation must return a stable instance</b> (e.g. stored in a field) that is not mutated after it is
	 * returned — this mixin does not lazily cache an override the way the servlet-subclass path
	 * ({@link McpRestServlet#getMrtrConfig()}) does, and returning a fresh custom instance per call would
	 * reintroduce the per-request-key RESUME failure the default avoids.
	 *
	 * @return The MRTR configuration. Must not be <jk>null</jk>.
	 */
	default McpMrtrConfig mrtrConfig() {
		return SharedMrtrConfig.get();
	}
}
