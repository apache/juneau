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
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
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
public interface McpEndpoint extends org.apache.juneau.rest.server.mcp.McpEndpointMixin {

	@Override /* McpEndpointMixin */
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

	/**
	 * Subscriptions configuration for this endpoint's bound revision.
	 *
	 * <p>
	 * The default returns a fresh {@link McpSubscriptionsConfig} (all defaults) on every call — knobs-only, so
	 * unlike {@link #subscriptionBroker()} there is no live state a fresh instance could split, matching
	 * {@link #cacheConfig()}'s precedent exactly. Override to supply custom
	 * concurrency/queue/heartbeat/idle-timeout knobs.
	 *
	 * <p>
	 * <b>Warning:</b> overriding {@link McpSubscriptionsConfig#getQueueSize() queueSize} here has <b>no
	 * effect</b> on the JVM-wide default shared broker {@link #subscriptionBroker()} returns — that broker is
	 * already sized from {@link McpSubscriptionsConfig#DEFAULT_QUEUE_SIZE} the first time it is lazily created
	 * (see {@link SharedSubscriptionBroker}), before this method's returned queue size could ever be read. A
	 * non-default queue size only takes effect if {@link #subscriptionBroker()} is also overridden to size a
	 * broker from it.
	 *
	 * @return The subscriptions configuration. Must not be <jk>null</jk>.
	 */
	default McpSubscriptionsConfig subscriptionsConfig() {
		return new McpSubscriptionsConfig();
	}

	/**
	 * Publishes {@link #subscriptionsConfig()} into this resource's {@code RestContext} bean store, the
	 * same {@code @Bean}-method mechanism already used by {@link #mcpTraceContextExtractor()}.
	 *
	 * <p>
	 * This is what lets {@code McpRevision.dispatch(...)}'s {@code subscriptions/listen} branch resolve
	 * the real, possibly-overridden config via {@code ctx.getBean(McpSubscriptionsConfig.class)} instead
	 * of always falling through to that call's defensive {@code new McpSubscriptionsConfig()} default —
	 * the neutral core cannot add this bean itself ({@code McpSubscriptionsConfig} is a v2 type, and the
	 * neutral module must not import it), so this v2-side {@code @Bean} method is the wiring's only seam.
	 *
	 * @return This endpoint's subscriptions configuration. Never <jk>null</jk>.
	 */
	@Bean
	default McpSubscriptionsConfig subscriptionsConfigBean() {
		return subscriptionsConfig();
	}

	/**
	 * Subscription broker for this endpoint's bound revision.
	 *
	 * <p>
	 * The default returns a <b>per-process shared</b> {@link McpSubscriptionBroker} (see
	 * {@link SharedSubscriptionBroker}) — a single JVM-wide registry, lazily created on first use — since a
	 * fresh broker per call (unlike the knobs-only {@link #subscriptionsConfig()}) would silently split live
	 * subscribers and publishers across unrelated registries. Override to supply a custom broker; an
	 * overriding implementation must return a stable instance (this mixin does not lazily cache an override
	 * the way {@link McpRestServlet#getSubscriptionBroker()} does).
	 *
	 * <p>
	 * Because this registry is shared JVM-wide, every live subscription still shares one namespace across
	 * however many mixin resources bind this same broker — but {@code subscriptions/listen}'s dispatch
	 * branch registers each stream under a fresh server-minted key (a random {@code UUID}, not the
	 * client-supplied JSON-RPC request id), so distinct listens can never collide and evict each other's
	 * stream merely because two unrelated clients (or the same client, twice) happened to reuse the same
	 * id. The only thing still shared process-wide is capacity: {@code maxConcurrentSubscriptions} is
	 * enforced against this one registry's total active count, mirroring the same per-process-sharing
	 * trade-off {@link #mrtrConfig()} documents for its shared AES key.
	 *
	 * @return The subscription broker. Must not be <jk>null</jk>.
	 */
	@Override
	default McpSubscriptionBroker subscriptionBroker() {
		return SharedSubscriptionBroker.get();
	}
}
