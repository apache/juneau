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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.rest.server.*;
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
 *     @Override                                            // optional
 *     protected McpOptions createMcpOptions() {
 *         return new McpOptions()
 *             .setInstructions("Use tool 'echo' to test.")
 *             .cache(c -&gt; c.setToolsList(new McpCacheHint().setTtlMs(60_000)))
 *             .subscriptions(s -&gt; s.setQueueSize(2048));
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

	private final transient AtomicReference<McpOptions> mcpOptions = new AtomicReference<>();

	@Override /* AbstractMcpRestServlet */
	protected final org.apache.juneau.rest.server.mcp.McpRevision revision() {
		var o = getMcpOptions();
		return new McpRevision(o.getCapabilities(), o.getCache(), o.getInstructions(), o.getMrtr());
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
	public final TraceContextExtractor mcpTraceContextExtractor() {
		return McpRevision.TRACE_CONTEXT_EXTRACTOR;
	}

	/**
	 * Factory method for the behavior configuration published by {@link #getMcpOptions()}.
	 *
	 * <p>
	 * Override to customize any of capabilities/instructions/cache/mrtr/subscriptions/broker in one place. The
	 * default returns a plain {@link McpOptions} (all defaults).
	 *
	 * <p>
	 * <b>Must be side-effect-free and idempotent</b>, exactly like
	 * {@link org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet#createMcpConfig()}: {@link #getMcpOptions()}
	 * calls this hook at most once per servlet instance and publishes the result via a lock-free
	 * {@link java.util.concurrent.atomic.AtomicReference#compareAndSet}, so under a first-access race it may be
	 * invoked by more than one thread, but only one resulting instance is ever published and returned.
	 *
	 * @return A non-<jk>null</jk> options instance.
	 */
	protected McpOptions createMcpOptions() {
		return new McpOptions();
	}

	/**
	 * Returns this servlet's lazily-published, binding-owned {@link McpOptions}.
	 *
	 * <p>
	 * The first successful call publishes the result of {@link #createMcpOptions()}; every later call returns
	 * the same instance, mirroring exactly how
	 * {@link org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet#getMcpConfig() getMcpConfig()} memoizes.
	 * Also published as a {@code @Bean} so downstream plumbing ({@link #getSubscriptionBroker()},
	 * {@code subscriptions/listen} dispatch) can resolve it uniformly through the {@code BeanStore}. This
	 * {@code @Bean} publication is authoritative and takes precedence over any Spring {@code @Bean McpOptions}
	 * in the app context: a Spring user who wants their configuration honored must inject it INTO
	 * {@link #createMcpOptions()}'s override (for example an {@code @Autowired} field read from there), not
	 * publish a bare {@code @Bean McpOptions} in their Spring context and expect it to be picked up instead.
	 *
	 * @return The options. Never <jk>null</jk>.
	 * @throws IllegalStateException If {@link #createMcpOptions()} returns <jk>null</jk>.
	 */
	@Bean
	public final McpOptions getMcpOptions() {
		var o = mcpOptions.get();
		if (o == null) {
			var no = createMcpOptions();
			if (no == null)
				throw isex("createMcpOptions() returned null");
			o = mcpOptions.compareAndSet(null, no) ? no : mcpOptions.get(); // HTT: the CAS-loses branch requires a genuine concurrent first-access race; untestable deterministically.
		}
		return o;
	}

	/**
	 * Returns this servlet's lazily-published, binding-owned subscription broker.
	 *
	 * <p>
	 * Reads {@link #getMcpOptions()}'s memoized {@link McpOptions#getSubscriptionBroker()}: an explicitly
	 * configured broker is returned as-is; otherwise one is derived and memoized per-binding, sized from
	 * {@link McpOptions#getSubscriptions()}'s queue bound (see {@link McpOptions#resolveSubscriptionBroker()}).
	 *
	 * @return The subscription broker. Never <jk>null</jk>.
	 */
	@Override
	public final McpSubscriptionBroker getSubscriptionBroker() {
		return getMcpOptions().resolveSubscriptionBroker();
	}
}
