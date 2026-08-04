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
import org.apache.juneau.rest.server.mcp.BasicMcpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.apache.juneau.rest.server.tracing.TraceContextExtractor;

/**
 * Abstract MCP servlet bound to protocol revision {@code 2026-07-28}.
 *
 * <p>
 * Subclass this (rather than the neutral {@link org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet}) to
 * expose a {@code 2026-07-28} endpoint at {@code POST /}; the revision binding is a compile-time choice
 * made by which class you extend.
 *
 * <p>
 * URI and polymorphic-type serializer policy ({@code addBeanTypes} and {@code uriResolution="NONE"}) is
 * inherited centrally from the neutral {@link org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet}; the only
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
	"java:S110" // Inherent to extending the RestServlet hierarchy.
})
@Rest(responseProcessors = McpNotificationResponseProcessor.class)
public abstract class McpRestServlet extends org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({
		"java:S2226", // Lazily-published, per-servlet-instance cache config; cannot be static (per-instance) or final (assigned after construction).
		"java:S3077" // Volatile + idempotent createCacheConfig() makes concurrent first-access races safe: any racing writer publishes an equivalent value.
	})
	private transient volatile McpCacheConfig cacheConfig;

	@SuppressWarnings({
		"java:S2226", // Lazily-published, per-servlet-instance MRTR config; cannot be static (per-instance) or final (assigned after construction).
		"java:S3077" // Volatile is required for the correct double-checked locking in getMrtrConfig(): unlike the cache config, two McpMrtrConfig instances are NOT equivalent (each holds a different random AES key), so exactly one config/key must ever be created and published or a requestState sealed against one key could never be unsealed against another.
	})
	private transient volatile McpMrtrConfig mrtrConfig;

	@SuppressWarnings({
		"java:S2226", // Lazily-published, per-servlet-instance subscriptions config; cannot be static (per-instance) or final (assigned after construction).
	})
	private transient volatile McpSubscriptionsConfig subscriptionsConfig;

	@SuppressWarnings({
		"java:S2226", // Lazily-published, per-servlet-instance subscription broker; cannot be static (per-instance) or final (assigned after construction).
		"java:S3077" // Volatile is required for correct double-checked locking in getSubscriptionBroker(): unlike the plain-read cache config, two BasicMcpSubscriptionBroker instances are NOT equivalent (each holds distinct live per-connection subscription state), so exactly one broker must ever be created and published or a subscription registered against one instance would never be visible to a publish call against the other.
	})
	private transient volatile McpSubscriptionBroker subscriptionBroker;

	@Override /* AbstractMcpRestServlet */
	protected org.apache.juneau.rest.server.mcp.McpRevision revision() {
		return new McpRevision(capabilities(), getCacheConfig(), instructions(), getMrtrConfig());
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

	/**
	 * Creates the MRTR (Multi-Round-Trip Request) configuration published by {@link #getMrtrConfig()}.
	 *
	 * <p>
	 * Override to supply a custom {@link RequestStateCodec}, TTL, or max-rounds cap. The default returns a
	 * new {@link McpMrtrConfig} (AES-GCM ephemeral codec, 5-minute {@code requestState} TTL, 10-round cap).
	 *
	 * <p>
	 * <b>Must be side-effect-free.</b> {@link #getMrtrConfig()} calls this hook <b>exactly once</b> per servlet
	 * instance under a lock, so &mdash; unlike {@link #createCacheConfig()} &mdash; the returned instance (and its
	 * single random AES key) is the one and only config ever published; it must not depend on being called more
	 * than once.
	 *
	 * @return The MRTR configuration. Must not be <jk>null</jk>.
	 */
	protected McpMrtrConfig createMrtrConfig() {
		return new McpMrtrConfig();
	}

	/**
	 * Returns this servlet's lazily-published, binding-owned MRTR configuration.
	 *
	 * <p>
	 * The first call publishes the result of {@link #createMrtrConfig()} under a lock; every later call returns
	 * that same instance. Publication is done with double-checked locking (not the plain lazy read
	 * {@link #getCacheConfig()} uses) on purpose: two {@link McpMrtrConfig} instances are <b>not</b> equivalent
	 * &mdash; each {@link AeadRequestStateCodec} generates a distinct random AES key at construction time &mdash;
	 * so a benign publication race that let two threads each create and publish a config would leave a
	 * {@code requestState} sealed against one key impossible to unseal against the other, failing RESUME. The lock
	 * guarantees exactly one config/key is ever created and published. The published instance must not be mutated
	 * by callers.
	 *
	 * @return The MRTR configuration. Never <jk>null</jk>.
	 * @throws IllegalStateException If {@link #createMrtrConfig()} returns <jk>null</jk>.
	 */
	public McpMrtrConfig getMrtrConfig() {
		var result = mrtrConfig;
		if (result == null) {
			synchronized (this) {
				result = mrtrConfig;
				if (result == null) {
					result = createMrtrConfig();
					if (result == null)
						throw new IllegalStateException("createMrtrConfig() returned null");
					mrtrConfig = result;
				}
			}
		}
		return result;
	}

	/**
	 * Creates the subscriptions configuration published by {@link #getSubscriptionsConfig()}.
	 *
	 * <p>
	 * Override to supply custom concurrency/queue/heartbeat/idle-timeout knobs. The default returns a plain
	 * {@link McpSubscriptionsConfig} (all defaults).
	 *
	 * @return The subscriptions configuration. Must not be <jk>null</jk>.
	 */
	protected McpSubscriptionsConfig createSubscriptionsConfig() {
		return new McpSubscriptionsConfig();
	}

	/**
	 * Returns this servlet's lazily-published, binding-owned subscriptions configuration.
	 *
	 * <p>
	 * Knobs-only (no live/broker state), so — unlike {@link #getSubscriptionBroker()} — this uses the same
	 * plain lazy-read strategy as {@link #getCacheConfig()}: a benign publication race is harmless because any
	 * two {@link McpSubscriptionsConfig} instances built by an idempotent {@link #createSubscriptionsConfig()}
	 * are equivalent.
	 *
	 * @return The subscriptions configuration. Never <jk>null</jk>.
	 * @throws IllegalStateException If {@link #createSubscriptionsConfig()} returns <jk>null</jk>.
	 */
	public McpSubscriptionsConfig getSubscriptionsConfig() {
		var result = subscriptionsConfig;
		if (result == null) {
			result = createSubscriptionsConfig();
			if (result == null)
				throw new IllegalStateException("createSubscriptionsConfig() returned null");
			subscriptionsConfig = result;
		}
		return result;
	}

	/**
	 * Publishes {@link #getSubscriptionsConfig()} into this servlet's {@code RestContext} bean store
	 * (the parent store {@link org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet#handleMcp} wraps its
	 * per-request {@code BeanStore} with), the same {@code @Bean}-method mechanism already used by
	 * {@link #mcpTraceContextExtractor()}.
	 *
	 * <p>
	 * This is what lets {@code McpRevision.dispatch(...)}'s {@code subscriptions/listen} branch resolve
	 * the real, possibly-overridden config via {@code ctx.getBean(McpSubscriptionsConfig.class)} instead
	 * of always falling through to that call's defensive {@code new McpSubscriptionsConfig()} default —
	 * the neutral core cannot add this bean itself ({@code McpSubscriptionsConfig} is a v2 type, and the
	 * neutral module must not import it), so this v2-side {@code @Bean} method is the wiring's only seam.
	 *
	 * @return This servlet's subscriptions configuration. Never <jk>null</jk>.
	 */
	@Bean
	public McpSubscriptionsConfig subscriptionsConfigBean() {
		return getSubscriptionsConfig();
	}

	/**
	 * Creates the subscription broker published by {@link #getSubscriptionBroker()}.
	 *
	 * <p>
	 * Override to supply a custom {@link McpSubscriptionBroker} implementation. The default returns a new
	 * {@link BasicMcpSubscriptionBroker} sized from {@link #getSubscriptionsConfig()}'s queue bound.
	 *
	 * <p>
	 * <b>Must be side-effect-free.</b> {@link #getSubscriptionBroker()} calls this hook <b>exactly once</b>
	 * per servlet instance under a lock: the returned instance is the one and only broker (and its live
	 * subscription registry) ever published.
	 *
	 * @return The subscription broker. Must not be <jk>null</jk>.
	 */
	protected McpSubscriptionBroker createSubscriptionBroker() {
		return new BasicMcpSubscriptionBroker(getSubscriptionsConfig().getQueueSize());
	}

	/**
	 * Returns this servlet's lazily-published, binding-owned subscription broker.
	 *
	 * <p>
	 * The first call publishes the result of {@link #createSubscriptionBroker()} under a lock; every later
	 * call returns that same instance. Publication is double-checked locking (MRTR-grade, not the plain lazy
	 * read {@link #getSubscriptionsConfig()} uses) on purpose: two {@link BasicMcpSubscriptionBroker}
	 * instances are <b>not</b> equivalent — each holds distinct, live per-connection subscription state — so a
	 * benign publication race would silently split subscribers and publishers across two unrelated registries.
	 *
	 * @return The subscription broker. Never <jk>null</jk>.
	 * @throws IllegalStateException If {@link #createSubscriptionBroker()} returns <jk>null</jk>.
	 */
	@Override
	public McpSubscriptionBroker getSubscriptionBroker() {
		var result = subscriptionBroker;
		if (result == null) {
			synchronized (this) {
				result = subscriptionBroker;
				if (result == null) {
					result = createSubscriptionBroker();
					if (result == null)
						throw new IllegalStateException("createSubscriptionBroker() returned null");
					subscriptionBroker = result;
				}
			}
		}
		return result;
	}
}
