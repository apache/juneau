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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.function.Consumer;

import org.apache.juneau.bean.mcp.v20260728.ServerCapabilities;
import org.apache.juneau.rest.server.mcp.BasicMcpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;

/**
 * Binding-owned behavior configuration ("how" a {@code 2026-07-28} MCP endpoint behaves) &mdash; the single
 * v2-only aggregate consolidating every per-binding tunable {@link McpEndpoint}/{@link McpRestServlet} previously
 * exposed as six separate hooks: {@code capabilities}, {@code instructions}, {@code cache}, {@code mrtr},
 * {@code subscriptions}, and {@code subscriptionBroker}.
 *
 * <p>
 * <b>Mutable during setup, effectively immutable once published.</b> Same builder-less mutable-setup idiom as
 * {@link McpCacheConfig}/{@link McpMrtrConfig}/{@link McpSubscriptionsConfig} and the neutral
 * {@code org.apache.juneau.rest.server.mcp.McpServerConfig} it sits next to: configure fully via the single
 * {@code createMcpOptions()} (servlet-subclass path) / {@code getMcpOptions()} (mixin path) override, then never
 * mutate it afterward. The framework memoizes exactly one instance per binding (see {@link McpRestServlet#getMcpOptions()}
 * and {@link McpEndpoint#getMcpOptions()}), so a request-time reader sees a stable, request-independent snapshot.
 *
 * <p>
 * The three nested configs ({@link #getCache() cache}, {@link #getMrtr() mrtr}, {@link #getSubscriptions()
 * subscriptions}) are framework-owned instances created once, here, at construction time. Use the
 * {@link #cache(Consumer)}/{@link #mrtr(Consumer)}/{@link #subscriptions(Consumer)} configure-blocks to mutate
 * them in place &mdash; callers never {@code new} a replacement nested config, so the "must return a stable
 * instance" footgun the pre-consolidation hooks warned about cannot recur.
 *
 * <h5 class='section'>The subscription broker default:</h5>
 * <p>
 * {@link #getSubscriptionBroker()} defaults to <jk>null</jk>, meaning "framework-derived": the binding lazily
 * derives a {@link BasicMcpSubscriptionBroker} sized from {@link #getSubscriptions()}'s
 * {@link McpSubscriptionsConfig#getQueueSize() queueSize} the first time a broker is needed, and memoizes that
 * one derived instance for the lifetime of the binding (a fresh broker per call would silently split live
 * subscribers and publishers across unrelated registries). Call {@link #setSubscriptionBroker(McpSubscriptionBroker)}
 * to supply a custom broker instead (for example one shared across multiple bindings on purpose).
 *
 * <h5 class='section'>The idleTimeoutMs / heartbeatIntervalMs caveat:</h5>
 * <p>
 * When configuring {@link #subscriptions(Consumer)}, keep
 * {@link McpSubscriptionsConfig#getHeartbeatIntervalMs() heartbeatIntervalMs} below
 * {@link McpSubscriptionsConfig#getIdleTimeoutMs() idleTimeoutMs} (see that method's Javadoc) &mdash; otherwise
 * the idle-timeout watchdog can reap a perfectly healthy connection between two heartbeats.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * McpOptions <jv>options</jv> = <jk>new</jk> McpOptions()
 *     .setInstructions(<js>"Use tool 'echo' to test."</js>)
 *     .cache(<jv>c</jv> -&gt; <jv>c</jv>.setToolsList(<jk>new</jk> McpCacheHint().setTtlMs(60_000)))
 *     .subscriptions(<jv>s</jv> -&gt; <jv>s</jv>.setQueueSize(2048));
 * </pre>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., MSG_consumerMustNotBeNull)
})
public class McpOptions {

	// Error message constant (reused across the cache/mrtr/subscriptions/resourceServer configure-blocks below).
	private static final String MSG_consumerMustNotBeNull = "consumer must not be null";

	private ServerCapabilities capabilities;
	private String instructions;
	private McpCacheConfig cache = new McpCacheConfig();
	private McpMrtrConfig mrtr = new McpMrtrConfig();
	private McpResourceServerConfig resourceServer = new McpResourceServerConfig();
	private McpSubscriptionsConfig subscriptions = new McpSubscriptionsConfig();
	private McpSubscriptionBroker subscriptionBroker;

	@SuppressWarnings({
		"java:S3077" // Volatile is required for correct double-checked locking in resolveSubscriptionBroker(): unlike a plain lazy read, two BasicMcpSubscriptionBroker instances are NOT equivalent (each holds distinct live per-connection subscription state), so exactly one derived broker must ever be created and published or a subscription registered against one instance would never be visible to a publish call against the other.
	})
	private volatile McpSubscriptionBroker derivedBroker;

	private final Object lock = new Object();

	/**
	 * Optional explicit capabilities advertisement for {@code server/discover}.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive from the registered tool / prompt /
	 * 	resource lists (the default).
	 */
	public ServerCapabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets the explicit {@code server/discover} capabilities advertisement.
	 *
	 * <p>
	 * A non-<jk>null</jk> value bypasses auto-derivation entirely and advertises exactly what is supplied.
	 *
	 * @param value The new value. Can be <jk>null</jk> to auto-derive.
	 * @return This object (for method chaining).
	 */
	public McpOptions setCapabilities(ServerCapabilities value) {
		capabilities = value;
		return this;
	}

	/**
	 * Optional free-form {@code server/discover} usage instructions advertised to clients.
	 *
	 * @return The instructions, or <jk>null</jk> to omit them from the discovery result (the default).
	 */
	public String getInstructions() {
		return instructions;
	}

	/**
	 * Sets the {@code server/discover} usage instructions.
	 *
	 * @param value The new value. Can be <jk>null</jk> to omit them.
	 * @return This object (for method chaining).
	 */
	public McpOptions setInstructions(String value) {
		instructions = value;
		return this;
	}

	/**
	 * This binding's cache-hint configuration.
	 *
	 * @return The framework-owned cache configuration. Never <jk>null</jk>.
	 */
	public McpCacheConfig getCache() {
		return cache;
	}

	/**
	 * Replaces this binding's cache configuration outright.
	 *
	 * @param value The new value. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public McpOptions setCache(McpCacheConfig value) {
		if (value == null)
			throw iaex("cache must not be null");
		cache = value;
		return this;
	}

	/**
	 * Mutates this binding's cache configuration in place.
	 *
	 * <p>
	 * Preferred over {@link #setCache(McpCacheConfig)} for incremental configuration since it operates on the
	 * framework-owned instance rather than requiring the caller to construct a replacement.
	 *
	 * @param consumer Callback invoked with the framework-owned {@link McpCacheConfig}. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code consumer} is <jk>null</jk>.
	 */
	public McpOptions cache(Consumer<McpCacheConfig> consumer) {
		if (consumer == null)
			throw iaex(MSG_consumerMustNotBeNull);
		consumer.accept(cache);
		return this;
	}

	/**
	 * This binding's MRTR (Multi-Round-Trip Request) configuration.
	 *
	 * @return The framework-owned MRTR configuration. Never <jk>null</jk>.
	 */
	public McpMrtrConfig getMrtr() {
		return mrtr;
	}

	/**
	 * Replaces this binding's MRTR configuration outright.
	 *
	 * @param value The new value. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public McpOptions setMrtr(McpMrtrConfig value) {
		if (value == null)
			throw iaex("mrtr must not be null");
		mrtr = value;
		return this;
	}

	/**
	 * Mutates this binding's MRTR configuration in place.
	 *
	 * @param consumer Callback invoked with the framework-owned {@link McpMrtrConfig}. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code consumer} is <jk>null</jk>.
	 */
	public McpOptions mrtr(Consumer<McpMrtrConfig> consumer) {
		if (consumer == null)
			throw iaex(MSG_consumerMustNotBeNull);
		consumer.accept(mrtr);
		return this;
	}

	/**
	 * This binding's OAuth 2.1 resource-server (RS) configuration (READY-312f F2).
	 *
	 * @return The framework-owned RS configuration. Never <jk>null</jk>.  Disabled by default.
	 */
	public McpResourceServerConfig getResourceServer() {
		return resourceServer;
	}

	/**
	 * Replaces this binding's resource-server configuration outright.
	 *
	 * @param value The new value. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public McpOptions setResourceServer(McpResourceServerConfig value) {
		if (value == null)
			throw iaex("resourceServer must not be null");
		resourceServer = value;
		return this;
	}

	/**
	 * Mutates this binding's resource-server configuration in place.
	 *
	 * @param consumer Callback invoked with the framework-owned {@link McpResourceServerConfig}. Must not be
	 * 	<jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code consumer} is <jk>null</jk>.
	 */
	public McpOptions resourceServer(Consumer<McpResourceServerConfig> consumer) {
		if (consumer == null)
			throw iaex(MSG_consumerMustNotBeNull);
		consumer.accept(resourceServer);
		return this;
	}

	/**
	 * This binding's {@code subscriptions/listen} configuration.
	 *
	 * @return The framework-owned subscriptions configuration. Never <jk>null</jk>.
	 */
	public McpSubscriptionsConfig getSubscriptions() {
		return subscriptions;
	}

	/**
	 * Replaces this binding's subscriptions configuration outright.
	 *
	 * @param value The new value. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public McpOptions setSubscriptions(McpSubscriptionsConfig value) {
		if (value == null)
			throw iaex("subscriptions must not be null");
		subscriptions = value;
		return this;
	}

	/**
	 * Mutates this binding's subscriptions configuration in place.
	 *
	 * @param consumer Callback invoked with the framework-owned {@link McpSubscriptionsConfig}. Must not be
	 * 	<jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code consumer} is <jk>null</jk>.
	 */
	public McpOptions subscriptions(Consumer<McpSubscriptionsConfig> consumer) {
		if (consumer == null)
			throw iaex(MSG_consumerMustNotBeNull);
		consumer.accept(subscriptions);
		return this;
	}

	/**
	 * The explicit subscription broker for this binding, if one was supplied.
	 *
	 * @return The explicit broker, or <jk>null</jk> if none was supplied (the default) &mdash; in which case the
	 * 	binding lazily derives and memoizes one (see {@link #resolveSubscriptionBroker()}).
	 */
	public McpSubscriptionBroker getSubscriptionBroker() {
		return subscriptionBroker;
	}

	/**
	 * Sets an explicit subscription broker, bypassing the framework-derived default.
	 *
	 * <p>
	 * Setting this after {@link #resolveSubscriptionBroker()} has already derived and published a default
	 * broker for this instance does not retroactively clear or replace that already-published derived
	 * broker &mdash; {@link #resolveSubscriptionBroker()} always prefers an explicit value set here over a
	 * previously-derived one, but under the never-mutate-after-setup contract this method should only be
	 * called during configuration, before the binding is handed to the framework.
	 *
	 * @param value The new value. Can be <jk>null</jk> to revert to the framework-derived default.
	 * @return This object (for method chaining).
	 */
	public McpOptions setSubscriptionBroker(McpSubscriptionBroker value) {
		subscriptionBroker = value;
		return this;
	}

	/**
	 * Returns {@link #getSubscriptionBroker()} if explicitly set, otherwise lazily derives and memoizes a
	 * {@link BasicMcpSubscriptionBroker} sized from {@link #getSubscriptions()}'s queue bound.
	 *
	 * <p>
	 * This is the <b>effective</b> broker accessor: unlike {@link #getSubscriptionBroker()} (which returns
	 * <jk>null</jk> by default), this method never returns <jk>null</jk>, deriving a default the first time
	 * one is needed. Internal plumbing for {@link McpEndpoint#subscriptionBroker()} /
	 * {@link McpRestServlet#getSubscriptionBroker()}. Since this {@link McpOptions} instance is itself
	 * memoized once per binding, the derived broker this method publishes is likewise stable for the
	 * binding's lifetime &mdash; publication uses double-checked locking (not a plain lazy read) because two
	 * {@link BasicMcpSubscriptionBroker} instances are <b>not</b> equivalent: each holds distinct live
	 * per-connection subscription state, so a benign publication race would silently split subscribers and
	 * publishers across two unrelated registries.
	 *
	 * @return The subscription broker to use. Never <jk>null</jk>.
	 */
	public McpSubscriptionBroker resolveSubscriptionBroker() {
		var explicit = subscriptionBroker;
		if (explicit != null)
			return explicit;
		var result = derivedBroker;
		if (result == null) {
			synchronized (lock) {
				result = derivedBroker;
				if (result == null) {
					result = new BasicMcpSubscriptionBroker(subscriptions.getQueueSize());
					derivedBroker = result;
				}
			}
		}
		return result;
	}
}
