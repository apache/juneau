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

import org.apache.juneau.commons.inject.Bean;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestStartCall;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.apache.juneau.rest.server.tracing.TraceContextExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Mixin interface that exposes a {@code 2026-07-28} MCP endpoint at {@code POST /mcp} on any Juneau REST
 * resource.
 *
 * <p>
 * The mixin path and the servlet-subclass path ({@link McpRestServlet}) are at parity: both bind the revision
 * at compile time and both read the same {@link McpOptions} shape for behavior configuration
 * (capabilities/instructions/cache/mrtr/subscriptions/broker) via a single override.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/api")
 * public class MyResource extends BasicRestServlet implements McpEndpoint {
 *     @Override
 *     public McpServerConfig getMcpConfig() {
 *         return new McpServerConfig().addTool(new MyEchoTool());
 *     }
 *     @Override                                        // optional
 *     public McpOptions getMcpOptions() {
 *         return new McpOptions().mrtr(m -&gt; m.setTtlMs(600_000));
 *     }
 * }
 * </pre>
 */
public interface McpEndpoint extends org.apache.juneau.rest.server.mcp.McpEndpointMixin {

	@Override /* McpEndpointMixin */
	default org.apache.juneau.rest.server.mcp.McpRevision revision() {
		var o = McpEndpointOptionsCache.resolve(this);
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
	default TraceContextExtractor mcpTraceContextExtractor() {
		return McpRevision.TRACE_CONTEXT_EXTRACTOR;
	}

	/**
	 * This endpoint's behavior configuration &mdash; capabilities, instructions, cache hints, MRTR, subscriptions,
	 * and the subscription broker, all in one place.
	 *
	 * <p>
	 * The default returns a per-binding-fresh {@link McpOptions} (all defaults; no live-state sharing across
	 * endpoint instances). Override to customize any of the six concepts.
	 *
	 * <p>
	 * <b>This method is invoked exactly once per endpoint instance</b> and the result is memoized for that
	 * instance's lifetime by the framework internals that read it ({@link #revision()},
	 * {@link #subscriptionBroker()}, the {@code @Bean}-published accessor) &mdash; so an override may freely
	 * construct and configure a fresh instance on every call, exactly as shown above; it never needs to cache the
	 * result itself, and returning a fresh instance per call cannot reintroduce the "must return a stable
	 * instance" footgun the pre-consolidation per-concept hooks warned about.
	 *
	 * @return This endpoint's options. Must not be <jk>null</jk>.
	 */
	default McpOptions getMcpOptions() {
		return new McpOptions();
	}

	/**
	 * Publishes this endpoint's memoized {@link McpOptions} into this resource's {@code RestContext} bean store,
	 * the same {@code @Bean}-method mechanism already used by {@link #mcpTraceContextExtractor()}.
	 *
	 * <p>
	 * This is what lets {@code McpRevision.dispatch(...)}'s {@code subscriptions/listen} branch resolve the
	 * real, possibly-overridden subscriptions configuration via {@code ctx.getBean(McpOptions.class)} instead of
	 * always falling through to that call's defensive {@code new McpSubscriptionsConfig()} default &mdash; the
	 * neutral core cannot add this bean itself ({@code McpOptions} is a v2 type, and the neutral module must not
	 * import it), so this v2-side {@code @Bean} method is the wiring's only seam.
	 *
	 * @return This endpoint's memoized options. Never <jk>null</jk>.
	 */
	@Bean
	default McpOptions mcpOptionsBean() {
		return McpEndpointOptionsCache.resolve(this);
	}

	/**
	 * Subscription broker for this endpoint's bound revision.
	 *
	 * <p>
	 * Reads {@link #getMcpOptions()}'s memoized {@link McpOptions#getSubscriptionBroker()}: an explicitly
	 * configured broker is returned as-is; otherwise one is derived and memoized per-binding, sized from
	 * {@link McpOptions#getSubscriptions()}'s queue bound (see {@link McpOptions#resolveSubscriptionBroker()}).
	 * Because {@link McpOptions} itself is memoized once per endpoint instance ({@link McpEndpointOptionsCache}),
	 * two distinct endpoint instances resolve to two distinct broker registries by default &mdash; there is no
	 * accidental JVM-wide sharing the way the pre-consolidation mixin default had.
	 *
	 * @return The subscription broker. Never <jk>null</jk>.
	 */
	@Override
	default McpSubscriptionBroker subscriptionBroker() {
		return McpEndpointOptionsCache.resolve(this).resolveSubscriptionBroker();
	}

	/**
	 * OAuth 2.1 resource-server bearer gate for the mixin's MCP {@code POST /mcp} endpoint (READY-312f F2).
	 *
	 * <p>
	 * A no-op unless {@link McpResourceServerConfig#isEnabled() RS auth is enabled}; when enabled, a request that
	 * <b>the router resolves</b> to the MCP {@code POST /mcp} dispatch operation must present a valid bearer token or it is
	 * rejected with {@code 401} / {@code 403} and a {@code WWW-Authenticate: Bearer ...} challenge.  The gate is scoped to
	 * the MCP dispatch <i>operation</i> (identified by resolved-operation identity, or by that operation's own router
	 * matchers &mdash; see {@link McpResourceServerSupport#resolvesToMcpDispatch}) so other {@code POST} operations on the
	 * host resource are unaffected, and it cannot drift from the router: a trailing slash, percent-encoding, a
	 * {@code ?method=}/{@code X-Method} override, or a host-chosen {@code @Mixin(path/paths)} re-mount cannot slip past it
	 * (B1/H3).  The well-known {@code GET} routes are left unauthenticated for PRM discovery.
	 *
	 * @param ctx The host {@link org.apache.juneau.rest.server.RestContext} handling this request.
	 * @param req The raw HTTP request.
	 * @param res The raw HTTP response (used to emit the challenge header on rejection).
	 */
	@RestStartCall
	default void mcpResourceServerAuth(org.apache.juneau.rest.server.RestContext ctx, HttpServletRequest req, HttpServletResponse res) {
		McpResourceServerSupport.gateMcpEndpoint(McpEndpointOptionsCache.resolve(this).getResourceServer(), ctx, req, res);
	}

	/**
	 * Serves the RFC 9728 Protected Resource Metadata document at the root well-known location (SEP-2351 fallback).
	 *
	 * <p>
	 * This route (and its path-inserted sibling) is registered unconditionally, but returns {@code 404} whenever RS auth
	 * is {@link McpResourceServerConfig#isEnabled() disabled} &mdash; so a non-opted-in endpoint claims the well-known
	 * namespace but exposes no observable metadata (READY-312f F2, M7).
	 *
	 * @param req The REST request.
	 * @return The PRM document.
	 */
	@RestGet(path = "/.well-known/oauth-protected-resource")
	default McpProtectedResourceMetadata mcpProtectedResourceMetadataRoot(org.apache.juneau.rest.server.RestRequest req) {
		return McpResourceServerSupport.metadataForWellKnownRoot(McpEndpointOptionsCache.resolve(this).getResourceServer(), req);
	}

	/**
	 * Serves the RFC 9728 Protected Resource Metadata document at the path-inserted well-known location (SEP-2351), but
	 * only when the requested suffix matches this resource's expected well-known path; otherwise {@code 404} (M5).
	 *
	 * @param req The REST request.
	 * @return The PRM document.
	 */
	@RestGet(path = "/.well-known/oauth-protected-resource/*")
	default McpProtectedResourceMetadata mcpProtectedResourceMetadataPathInserted(org.apache.juneau.rest.server.RestRequest req) {
		return McpResourceServerSupport.metadataForWellKnownPathInserted(McpEndpointOptionsCache.resolve(this).getResourceServer(), req);
	}
}
