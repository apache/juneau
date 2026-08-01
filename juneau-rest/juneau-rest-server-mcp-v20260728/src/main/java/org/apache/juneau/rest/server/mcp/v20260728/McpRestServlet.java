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

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.tracing.TraceContextExtractor;

/**
 * Abstract MCP servlet bound to protocol revision {@code 2026-07-28}.
 *
 * <p>
 * Subclass this (rather than the neutral {@link org.apache.juneau.rest.server.mcp.McpRestServlet}) to
 * expose a {@code 2026-07-28} endpoint at {@code POST /}; the revision binding is a compile-time choice
 * made by which class you extend.
 *
 * <p>
 * URI and polymorphic-type serializer policy ({@code addBeanTypes} and {@code uriResolution="NONE"}) is
 * inherited centrally from the neutral {@link org.apache.juneau.rest.server.mcp.McpRestServlet}; the only
 * revision-specific behavior added here is notification empty-body processing, below.
 *
 * <p>
 * Registers {@link McpNotificationResponseProcessor} so a {@code null} dispatch result (a JSON-RPC
 * notification) renders as a genuinely empty HTTP body instead of the framework's default four-byte
 * {@code null} JSON literal.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/mcp")
 * public class MyMcpServlet extends McpRestServlet {
 *     @Override
 *     protected McpServerConfig createMcpConfig() {
 *         return new McpServerConfig().setName("my-server").setVersion("1.0.0").addTool(new MyEchoTool());
 *     }
 * }
 * </pre>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S2176", // Intentional: dated adapter binding classes are de-versioned and differentiated by package (see TODO-312).
	"java:S110" // Inherent to extending the RestServlet hierarchy.
})
@Rest(responseProcessors = McpNotificationResponseProcessor.class)
public abstract class McpRestServlet extends org.apache.juneau.rest.server.mcp.McpRestServlet {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({
		"java:S2226", // Lazily-published, per-servlet-instance cache config; cannot be static (per-instance) or final (assigned after construction).
		"java:S3077" // Volatile + idempotent createCacheConfig() makes concurrent first-access races safe: any racing writer publishes an equivalent value.
	})
	private transient volatile McpCacheConfig cacheConfig;

	@Override /* McpRestServlet */
	protected org.apache.juneau.rest.server.mcp.McpRevision revision() {
		return new McpRevision(capabilities(), getCacheConfig(), instructions());
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
	public TraceContextExtractor mcpTraceContextExtractor() {
		return McpRevision.TRACE_CONTEXT_EXTRACTOR;
	}

	/**
	 * Optional explicit capabilities advertisement for {@code server/discover}.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) leaves capabilities auto-derived from the registered
	 * tool / prompt / resource lists. Returning a non-<jk>null</jk> value bypasses auto-derivation
	 * entirely and advertises exactly what is returned — the way to advertise capabilities that are
	 * not derivable from a handler registry.
	 *
	 * <p>
	 * This hook is typed against this revision's wire beans on purpose: capabilities diverge between
	 * MCP revisions, so a neutral carrier in the core would accept data that is nonsense for the
	 * bound revision and fail only at serialization time.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive.
	 */
	protected ServerCapabilities capabilities() {
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
	protected String instructions() {
		return null;
	}

	/**
	 * Creates the cache configuration published by {@link #getCacheConfig()}.
	 *
	 * <p>
	 * Override to supply TTL/scope hints. The default returns an empty {@link McpCacheConfig} (no
	 * cache hints emitted on any result).
	 *
	 * <p>
	 * <b>Must be side-effect-free and idempotent.</b> Concurrent first access under {@link #getCacheConfig()}
	 * may invoke this hook more than once before the published value settles; every invocation must return
	 * an equivalent, independently valid configuration.
	 *
	 * @return The cache configuration. Must not be <jk>null</jk>.
	 */
	protected McpCacheConfig createCacheConfig() {
		return new McpCacheConfig();
	}

	/**
	 * Returns this servlet's lazily-published, binding-owned cache configuration.
	 *
	 * <p>
	 * The first successful call publishes the result of {@link #createCacheConfig()}; every later call
	 * returns the same instance. The published instance must not be mutated by callers.
	 *
	 * @return The cache configuration. Never <jk>null</jk>.
	 * @throws IllegalStateException If {@link #createCacheConfig()} returns <jk>null</jk>.
	 */
	public McpCacheConfig getCacheConfig() {
		var result = cacheConfig;
		if (result == null) {
			result = createCacheConfig();
			if (result == null)
				throw new IllegalStateException("createCacheConfig() returned null");
			cacheConfig = result;
		}
		return result;
	}
}
