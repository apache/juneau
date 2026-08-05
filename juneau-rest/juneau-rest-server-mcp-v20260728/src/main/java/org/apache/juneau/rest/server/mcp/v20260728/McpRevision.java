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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.utils.JsonValueSafety;
import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.tracing.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.mcp.McpCompletionRef;
import org.apache.juneau.rest.server.mcp.McpCompletionRequest;
import org.apache.juneau.rest.server.mcp.McpCompletionResult;
import org.apache.juneau.rest.server.mcp.McpCursor;
import org.apache.juneau.rest.server.mcp.McpErrorKind;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpParamUtils;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpResourceTemplateHandler;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;

/**
 * {@link org.apache.juneau.rest.server.mcp.McpRevision} implementation for MCP revision {@code 2026-07-28}.
 *
 * <p>
 * Owns this revision's stateless JSON-RPC method table, per-request {@code _meta} negotiation,
 * SEP-2243 {@code Mcp-Method}/{@code Mcp-Name} header agreement checks, {@code server/discover}
 * dispatch, and error-code table. Every request is independently negotiated from its own
 * {@code _meta}; no handshake, session, or prior-request state exists.
 *
 * <p>
 * <b>Constructed per binding, not shared as a singleton.</b> The bound servlet or mixin builds a
 * fresh instance on every {@code revision()} call, passing its {@link McpOptions#getCapabilities()}
 * result and its published {@link McpCacheConfig} into the constructor — see {@link McpRestServlet}
 * and {@link McpEndpoint}. A {@code null} capabilities override auto-derives {@code server/discover}
 * capabilities from the registered tool/prompt/resource/resource-template lists; a non-{@code null}
 * override is advertised as-is. The cache config is binding-owned: it is supplied once at
 * construction, is never {@code null}, and is treated as static and request-independent for the
 * lifetime of this instance — every page of a given list method resolves to identical cache values.
 * The instance retains no request-derived state.
 */
@SuppressWarnings({
	"java:S2176" // Intentional: dated adapter binding classes are de-versioned and differentiated by package (see TODO-312).
})
public final class McpRevision implements org.apache.juneau.rest.server.mcp.McpRevision {

	private final ServerCapabilities capabilities;
	private final McpCacheConfig cacheConfig;
	private final String instructions;
	private final McpMrtrConfig mrtrConfig;

	/**
	 * Constructor. Uses an empty {@link McpCacheConfig} (no cache hints emitted on any result) and no
	 * discovery instructions.
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code server/discover}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists.
	 */
	public McpRevision(ServerCapabilities capabilities) {
		this(capabilities, new McpCacheConfig());
	}

	/**
	 * Constructor. Advertises no discovery instructions.
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code server/discover}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists.
	 * @param cacheConfig Binding-owned cache configuration. Must not be <jk>null</jk>.
	 */
	public McpRevision(ServerCapabilities capabilities, McpCacheConfig cacheConfig) {
		this(capabilities, cacheConfig, null);
	}

	/**
	 * Constructor. Uses a default {@link McpMrtrConfig} (AES-GCM ephemeral codec, 5-minute
	 * {@code requestState} TTL, 10-round cap).
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code server/discover}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists.
	 * @param cacheConfig Binding-owned cache configuration. Must not be <jk>null</jk>.
	 * @param instructions Optional free-form {@code server/discover} usage instructions, or <jk>null</jk>
	 * 	to omit them.
	 */
	public McpRevision(ServerCapabilities capabilities, McpCacheConfig cacheConfig, String instructions) {
		this(capabilities, cacheConfig, instructions, new McpMrtrConfig());
	}

	/**
	 * Constructor.
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code server/discover}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists.
	 * @param cacheConfig Binding-owned cache configuration. Must not be <jk>null</jk>.
	 * @param instructions Optional free-form {@code server/discover} usage instructions, or <jk>null</jk>
	 * 	to omit them.
	 * @param mrtrConfig Binding-owned MRTR (Multi-Round-Trip Request) configuration. Must not be <jk>null</jk>.
	 */
	public McpRevision(ServerCapabilities capabilities, McpCacheConfig cacheConfig, String instructions, McpMrtrConfig mrtrConfig) {
		this.capabilities = capabilities;
		this.cacheConfig = Objects.requireNonNull(cacheConfig, "cacheConfig");
		this.instructions = instructions;
		this.mrtrConfig = Objects.requireNonNull(mrtrConfig, "mrtrConfig");
	}

	/** JSON-RPC error code: parse error. Never reported by this revision (see {@link McpErrorKind#PARSE_ERROR}). */
	public static final int CODE_PARSE_ERROR = -32700;

	/** JSON-RPC error code: invalid request. */
	public static final int CODE_INVALID_REQUEST = -32600;

	/** JSON-RPC error code: method not found. */
	public static final int CODE_METHOD_NOT_FOUND = -32601;

	/** JSON-RPC error code: invalid params. */
	public static final int CODE_INVALID_PARAMS = -32602;

	/** JSON-RPC error code: internal error. */
	public static final int CODE_INTERNAL_ERROR = -32603;

	/**
	 * SEP-2322 error code: the client did not advertise a capability ({@code elicitation}) required to receive an
	 * {@code input_required} result. Pinned directly by the SEP-2322 schema (spec Open item #3).
	 */
	public static final int CODE_MISSING_REQUIRED_CLIENT_CAPABILITY = -32021;

	/**
	 * MRTR error code: the echoed {@code requestState} has expired (past its sealed {@code expiresAtMs}). This
	 * plan's own pin, chosen from the JSON-RPC-reserved {@code -32000}..{@code -32099} server-error range adjacent
	 * to the schema-pinned {@code -32021}; not a SEP-2322-sourced value.
	 */
	public static final int CODE_REQUEST_STATE_EXPIRED = -32022;

	/**
	 * MRTR error code: the echoed {@code requestState}'s embedded round counter is at or above the configured
	 * max-rounds cap. This plan's own pin (same range/rationale as {@link #CODE_REQUEST_STATE_EXPIRED}).
	 */
	public static final int CODE_MAX_ROUNDS_EXCEEDED = -32023;

	/**
	 * C8 error code: the server already has {@code maxConcurrentSubscriptions} live {@code
	 * subscriptions/listen} streams. This plan's own pin (same {@code -32000}..{@code -32099}
	 * server-error range and rationale as {@link #CODE_REQUEST_STATE_EXPIRED} / {@link #CODE_MAX_ROUNDS_EXCEEDED});
	 * not sourced from SEP-2575.
	 */
	public static final int CODE_TOO_MANY_SUBSCRIPTIONS = -32024;

	/** Default server name reported by {@code server/discover} when the config supplies no server identity. */
	public static final String DEFAULT_SERVER_NAME = "juneau-rest-server-mcp";

	private static final String META_KEY = "_meta";
	private static final String PARAM_ARGUMENTS = "arguments";

	/**
	 * This revision instance's binding-owned MRTR (Multi-Round-Trip Request) configuration, as supplied at
	 * construction time.
	 *
	 * <p>
	 * Package-visible so tests can confirm the binding-level memoization contract documented on
	 * {@link McpRestServlet#getMcpOptions()} / {@link McpEndpoint#getMcpOptions()}: two {@link McpRevision}
	 * instances built from the same binding must share the same {@link McpMrtrConfig} (and therefore the
	 * same {@link RequestStateCodec}), even though each dispatched request constructs its own
	 * {@link McpRevision}.
	 *
	 * @return The MRTR configuration. Never <jk>null</jk>.
	 */
	McpMrtrConfig mrtrConfig() {
		return mrtrConfig;
	}

	@Override /* McpRevision */
	public String protocolVersion() {
		return McpProtocol.VERSION_2026_07_28;
	}

	@Override /* McpRevision */
	public int errorCode(McpErrorKind kind) {
		return switch (kind) {
			case INVALID_REQUEST -> CODE_INVALID_REQUEST;
			case UNKNOWN_METHOD -> CODE_METHOD_NOT_FOUND;
			case TOOL_NOT_FOUND, PROMPT_NOT_FOUND, RESOURCE_NOT_FOUND, INVALID_PARAMS -> CODE_INVALID_PARAMS;
			case INTERNAL_ERROR -> CODE_INTERNAL_ERROR;
			case PARSE_ERROR -> CODE_PARSE_ERROR;
		};
	}

	@Override /* McpRevision */
	public Object dispatch(McpExchange exchange, McpServerConfig config, BeanStore ctx) {
		assertArgNotNull("exchange", exchange);
		assertArgNotNull("config", config);
		assertArgNotNull("ctx", ctx);

		var req = exchange.request();
		if (req == null)
			return reportError(ctx, null, errorCode(McpErrorKind.INVALID_REQUEST), "Request envelope is null");

		var id = req.getId();
		var method = req.getMethod();

		if (isEmpty(method))
			return JsonRpcResponse.notification(id) ? null
				: reportError(ctx, id, errorCode(McpErrorKind.INVALID_REQUEST), "Missing method");

		// SEP-2350 per-operation step-up enforcement at the POST-parse point (method now known).  Deliberately OUTSIDE
		// the try below: an insufficient-scope 403 (with its WWW-Authenticate step-up challenge) must propagate as a real
		// HTTP 403 through the response processors, not be caught and folded into a JSON-RPC error over HTTP 200.
		enforceStepUpScopes(method, req.getParams(), ctx);

		try {
			validateHeaders(exchange, req);
			validateMeta(req.getParams());
			if (McpMethods.SUBSCRIPTIONS_LISTEN.equals(method))
				return JsonRpcResponse.notification(id) ? null : dispatchSubscriptionsListen(exchange, id, req.getParams(), config, ctx);
			var result = finalizeResult(invoke(method, req.getParams(), config, ctx), config, ctx);
			return JsonRpcResponse.notification(id) ? null : JsonRpcResponse.ok(id, result);
		} catch (McpException e) {
			if (JsonRpcResponse.notification(id))
				return null;
			recordRpcError(ctx, e.getCode(), e.getMessage());
			return new JsonRpcResponse()
				.setJsonrpc(McpProtocol.JSON_RPC_2_0)
				.setId(id)
				.setError(e.toJsonRpcError());
		} catch (Exception e) {
			if (JsonRpcResponse.notification(id))
				return null;
			var message = e.getMessage() == null ? cns(e) : e.getMessage();
			var code = errorCode(McpErrorKind.INTERNAL_ERROR);
			recordRpcError(ctx, code, message);
			return JsonRpcResponse.errorResponse(id, code, message, JsonMap.of("type", cn(e)));
		}
	}

	/**
	 * Enforces SEP-2350 per-operation step-up scopes for a dispatched request (the POST-parse enforcement point).
	 *
	 * <p>
	 * A no-op unless an {@link McpOptions} bean is bound and its {@link McpResourceServerConfig} is
	 * {@link McpResourceServerConfig#isEnabled() enabled} with a per-operation scope configured for this operation.  The
	 * MCP method &mdash; and thus the operation identity ({@link McpRoutingNames#routingName routing name}) &mdash; is
	 * known here (unlike the pre-parse {@code @RestStartCall} bearer gate), so a scoped {@code 403 insufficient_scope}
	 * step-up challenge naming exactly this operation's required scopes can be emitted.  Delegates the actual
	 * granted-vs-required comparison and challenge construction to {@link McpResourceServerSupport#enforceOperationScopes}.
	 *
	 * @param method The resolved JSON-RPC method.  Never <jk>null</jk>/empty at the call site.
	 * @param params The raw JSON-RPC request params (used to derive the operation name and passed through verbatim as
	 * 	the {@link McpOperationContext#params() resolver seam}).
	 * @param ctx The per-request bean store (carries the bound {@link McpOptions} and {@link RestRequest}).
	 */
	private static void enforceStepUpScopes(String method, Object params, BeanStore ctx) {
		var opt = ctx.getBean(McpOptions.class).orElse(null);
		if (opt == null)
			return;
		var cfg = opt.getResourceServer();
		if (!cfg.isEnabled())
			return;
		var granted = McpResourceServerSupport.grantedScopes(
			ctx.getBean(RestRequest.class).map(RestRequest::getHttpServletRequest).orElse(null));
		var rn = McpRoutingNames.routingName(method, params);
		var name = isEmpty(rn) ? null : rn;
		var rawParams = params instanceof Map<?,?> m ? castParams(m) : Map.<String,Object>of();
		McpResourceServerSupport.enforceOperationScopes(cfg, granted, new McpOperationContext(method, name, rawParams));
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> castParams(Map<?,?> m) {
		return (Map<String,Object>)m;
	}

	/**
	 * Capability-gates, limit-checks, registers, and constructs the streaming publisher for {@code
	 * subscriptions/listen}. Bypasses {@link #finalizeResult} / {@link JsonRpcResponse#ok}: the successful
	 * result is a {@code Flow.Publisher<SseEvent>}, not a JSON-RPC envelope. A thrown {@link McpException}
	 * (for example the over-limit case) is caught by {@link #dispatch}'s existing {@code McpException}
	 * handler, which builds the normal JSON-RPC error envelope.
	 *
	 * @param exchange The transport-neutral request/header accessor, used to enforce the SSE {@code Accept}
	 * 	gate below before any broker registration happens.
	 * @param id The JSON-RPC request id. Never <jk>null</jk> (this method is never called for a notification).
	 * @param params The opaque request params. Must decode to a {@code SubscriptionsListenRequest}.
	 * @param config The neutral handler registry, used to auto-derive capabilities when none were set explicitly.
	 * @param ctx The per-request bean store, expected to carry a bound {@link McpSubscriptionBroker}
	 * 	(Phase 1-3 wiring) and a bound {@link McpOptions}. In production the options bean is normally present —
	 * 	{@code McpRestServlet#getMcpOptions()} / {@code McpEndpoint#mcpOptionsBean()} (TODO-330) publish it into
	 * 	the {@code RestContext} bean store that this request-scoped {@code ctx} wraps as its parent, since the
	 * 	neutral core cannot add it directly ({@code McpOptions} is a v2 type). The
	 * 	{@code orElseGet(McpSubscriptionsConfig::new)} fallback below is defense-in-depth only (e.g. a
	 * 	hand-built {@code ctx} in a direct-dispatch test that omits it).
	 * <p>
	 * <b>SSE negotiation gate (checked first, before any broker registration):</b> every MCP servlet binding
	 * defaults an unqualified request to {@code Accept: application/json}. A {@code subscriptions/listen}
	 * call that never negotiated {@code text/event-stream} would still register with the broker and hand
	 * back a real {@code Flow.Publisher}, but {@code ReactiveResponseProcessor} would then render it in its
	 * BUFFER shape (unbounded {@code request(Long.MAX_VALUE)} demand, collected into a list that is only
	 * flushed once the publisher completes — which, for a live subscription, is never, until the connection
	 * itself drops). Because this dead-client cleanup is write-failure-driven (see the class javadoc), a
	 * BUFFER-shape stream never even attempts a write, so the broker slot, the pump thread, and the
	 * heartbeat executor all leak permanently, and any caller can trigger this for free. Checking {@code
	 * exchange.header("Accept")} here, before {@link McpSubscriptionBroker#registerIfUnder} is ever called,
	 * closes that hole at zero cost to a well-behaved caller (see {@code McpClient#listen} /
	 * {@code AbstractMcpClient#openEventStream}, which already always negotiate SSE on this call).
	 *
	 * <p>
	 * {@code maxConcurrentSubscriptions} is enforced as a hard cap, not an advisory one: the admission check
	 * and the registration are one atomic step via {@link McpSubscriptionBroker#registerIfUnder}, so
	 * concurrent {@code subscriptions/listen} calls against the same broker (e.g. a custom broker explicitly
	 * shared across multiple bindings via {@link McpOptions#setSubscriptionBroker}) cannot all observe room
	 * under the cap and all register, which a separate {@code activeCount()} check followed by a separate
	 * {@code register(...)} call would allow.
	 *
	 * <p>
	 * The broker registers under a fresh {@link UUID}, never under the client-supplied JSON-RPC request
	 * {@code id}: the broker's registry is process-wide (see {@link McpEndpoint#subscriptionBroker()}), so
	 * two unrelated concurrent listens that happen to share the same client id would otherwise evict each
	 * other's stream (the second {@code register(...)} call under a shared key closes whichever
	 * subscription is already registered there). The client's original {@code id} is passed to
	 * {@link SubscriptionsListenPublisher} unchanged and is still what every ack/notification frame on that
	 * client's own stream carries as its {@code _meta.subscriptionId} — this only decouples the *internal*
	 * registry key from the *wire* id, which stays exactly as the spec expects.
	 *
	 * @return The {@code Flow.Publisher<SseEvent>} for the newly registered subscription.
	 * @throws McpException If the caller never negotiated {@code text/event-stream}, the broker bean is
	 * 	missing, or {@code maxConcurrentSubscriptions} is reached.
	 */
	private Object dispatchSubscriptionsListen(McpExchange exchange, Object id, Object params, McpServerConfig config, BeanStore ctx) {
		requireEventStreamAccept(exchange);
		var request = Json.to(Json.of(params), SubscriptionsListenRequest.class);
		var caps = discoverCapabilities(config);
		var honoredFilter = SubscriptionCapabilityGate.honor(request.getNotifications(), caps);

		var broker = ctx.getBean(McpSubscriptionBroker.class)
			.orElseThrow(() -> new McpException(CODE_INTERNAL_ERROR, "McpSubscriptionBroker not bound in BeanStore"));
		var subscriptionsConfig = ctx.getBean(McpOptions.class).map(McpOptions::getSubscriptions).orElseGet(McpSubscriptionsConfig::new);
		// registerIfUnder admission-checks and registers as one atomic step (hard cap, not advisory) — a
		// separate activeCount() check followed by a separate register() call would leave a TOCTOU window
		// open for concurrent listen requests racing the same per-binding broker (see
		// McpOptions#resolveSubscriptionBroker()). The registry key is a server-minted UUID, deliberately
		// NOT String.valueOf(id): see the javadoc above.
		var subscription = broker.registerIfUnder(subscriptionsConfig.getMaxConcurrentSubscriptions(), UUID.randomUUID().toString(), honoredFilter)
			.orElseThrow(() -> new McpException(CODE_TOO_MANY_SUBSCRIPTIONS, "Maximum concurrent subscriptions exceeded"));
		var honoredWireFilter = SubscriptionCapabilityGate.toWireFilter(honoredFilter);
		return new SubscriptionsListenPublisher(id, honoredWireFilter, subscription, subscriptionsConfig.getHeartbeatIntervalMs(), subscriptionsConfig.getIdleTimeoutMs());
	}

	/**
	 * C1: rejects a {@code subscriptions/listen} call that never negotiated {@code text/event-stream},
	 * before any broker registration happens (see {@link #dispatchSubscriptionsListen}'s javadoc).
	 *
	 * <p>
	 * A bare {@code Accept: * / *} (every media type acceptable) is rejected exactly like an absent
	 * {@code Accept} header: this endpoint requires the caller to have explicitly negotiated
	 * {@code text/event-stream}, not merely to be willing to accept it among everything else. Without this,
	 * a wildcard-accepting request would fall into {@code ReactiveResponseProcessor}'s non-streaming BUFFER
	 * shape (the same permanent-leak hole C1 closes for a missing header), rather than the STREAM shape
	 * this method exists to require. A well-behaved caller is unaffected: both {@code McpClient#listen} and
	 * the underlying {@code AbstractMcpClient#openEventStream} already always send the explicit
	 * {@code text/event-stream} value on this call.
	 *
	 * @param exchange The transport-neutral request/header accessor. Must not be <jk>null</jk>.
	 * @throws McpException If the {@code Accept} header is absent or does not contain {@code text/event-stream}
	 * 	(case-insensitive, substring match — a real client's {@code Accept} header is commonly a
	 * 	comma-separated list of media-type ranges, not the bare value alone).
	 */
	private static void requireEventStreamAccept(McpExchange exchange) {
		var accept = exchange.header("Accept");
		if (accept == null || ! accept.toLowerCase(Locale.ROOT).contains("text/event-stream"))
			throw new McpException(CODE_INVALID_REQUEST, "subscriptions/listen requires Accept: text/event-stream");
	}

	/**
	 * Builds a JSON-RPC error response, first giving an active request-scoped tracing observation a
	 * chance to record the outcome via {@link #recordRpcError(BeanStore, int, String)} &mdash; every
	 * dated dispatch returns JSON-RPC errors over HTTP {@code 200}, so this is the only place such an
	 * outcome reaches an active span.
	 *
	 * @param ctx The request-scoped bean store. Must not be <jk>null</jk>.
	 * @param id The JSON-RPC request id, or <jk>null</jk> when the envelope itself could not be read.
	 * @param code The JSON-RPC error code.
	 * @param message The JSON-RPC error message.
	 * @return The error response. Never <jk>null</jk>.
	 */
	private static JsonRpcResponse reportError(BeanStore ctx, Object id, int code, String message) {
		recordRpcError(ctx, code, message);
		return JsonRpcResponse.errorResponse(id, code, message);
	}

	/**
	 * Records a JSON-RPC error outcome on the active request-scoped {@link Scope}, if any.
	 *
	 * <p>
	 * A neutral no-op &mdash; no OpenTelemetry import involved &mdash; when no tracer is active for
	 * this request (no {@link Scope} was stashed under {@link TraceContextResponseProcessor#ATTR_SCOPE}
	 * by {@code RestOpInvoker}) or when the request-scoped {@link RestRequest} itself is unavailable
	 * (for example, a unit test dispatching directly against a bare {@link BeanStore}).
	 *
	 * @param ctx The request-scoped bean store. Must not be <jk>null</jk>.
	 * @param code The JSON-RPC error code.
	 * @param message The JSON-RPC error message.
	 */
	@SuppressWarnings({
		"resource" // scope is owned/closed by RestOpInvoker's finally block after the handler returns; reading it here must not close it early.
	})
	private static void recordRpcError(BeanStore ctx, int code, String message) {
		ctx.getBean(RestRequest.class).ifPresent(request -> {
			var scope = request.getAttribute(TraceContextResponseProcessor.ATTR_SCOPE).as(Scope.class).orElse(null);
			if (scope != null)
				scope.recordRpcError(code, message);
		});
	}

	// --- SEP-2243 header/body agreement -----------------------------------------------------

	private static void validateHeaders(McpExchange exchange, JsonRpcRequest req) {
		var headerMethod = exchange.header("Mcp-Method");
		if (headerMethod == null)
			throw new McpException(CODE_INVALID_REQUEST, "Missing required header: Mcp-Method");
		var headerName = exchange.header("Mcp-Name");
		if (headerName == null)
			throw new McpException(CODE_INVALID_REQUEST, "Missing required header: Mcp-Name");
		var method = req.getMethod();
		if (! headerMethod.equals(method))
			throw new McpException(CODE_INVALID_REQUEST, "Mcp-Method header '" + headerMethod + "' does not match request method '" + method + "'");
		var name = McpRoutingNames.routingName(method, req.getParams());
		if (! headerName.equals(name))
			throw new McpException(CODE_INVALID_REQUEST, "Mcp-Name header '" + headerName + "' does not match request name '" + name + "'");
	}

	// --- per-request params._meta negotiation -----------------------------------------------

	/**
	 * Validates the opaque {@code params._meta} shape directly.
	 *
	 * <p>
	 * The parsed {@link RequestMeta} bean cannot distinguish a malformed {@code clientCapabilities} (a
	 * scalar/array) from an absent one — parsing either into {@code ClientCapabilities} raises a
	 * generic parse error rather than a validation failure — so metadata shape is checked against
	 * the opaque map to produce the exact per-field {@code -32600} messages naming the exact wire path.
	 *
	 * @param params The opaque request params (the JSON-RPC {@code params} member). Can be <jk>null</jk>.
	 */
	private static void validateMeta(Object params) {
		if (! (params instanceof Map<?,?> p))
			throw new McpException(CODE_INVALID_REQUEST, "Request params must be an object");
		var meta = p.get(META_KEY);
		if (! (meta instanceof Map<?,?> m))
			throw new McpException(CODE_INVALID_REQUEST, "Request params._meta must be an object");
		var protocolVersion = str(m.get(RequestMeta.KEY_PROTOCOL_VERSION));
		if (isEmpty(protocolVersion))
			throw new McpException(CODE_INVALID_REQUEST, "Missing required params._meta." + RequestMeta.KEY_PROTOCOL_VERSION);
		if (! McpProtocol.VERSION_2026_07_28.equals(protocolVersion))
			throw new McpException(CODE_INVALID_REQUEST, "Unsupported protocol version: " + protocolVersion);
		var clientInfo = m.get(RequestMeta.KEY_CLIENT_INFO);
		if (clientInfo != null && (! (clientInfo instanceof Map<?,?> ci) || isEmpty(str(ci.get("name"))) || isEmpty(str(ci.get("version")))))
			throw new McpException(CODE_INVALID_REQUEST, "Malformed params._meta." + RequestMeta.KEY_CLIENT_INFO);
		if (! m.containsKey(RequestMeta.KEY_CLIENT_CAPABILITIES) || m.get(RequestMeta.KEY_CLIENT_CAPABILITIES) == null)
			throw new McpException(CODE_INVALID_REQUEST, "Missing required params._meta." + RequestMeta.KEY_CLIENT_CAPABILITIES);
		if (! (m.get(RequestMeta.KEY_CLIENT_CAPABILITIES) instanceof Map<?,?>))
			throw new McpException(CODE_INVALID_REQUEST, "params._meta." + RequestMeta.KEY_CLIENT_CAPABILITIES + " must be an object");
	}

	private static String str(Object value) {
		return value == null ? null : value.toString();
	}

	/**
	 * Attaches the common success-result finalization to every dispatched result: a {@code "complete"}
	 * {@code resultType} (already the {@link Result} default) and {@link ResultMeta#getServerInfo() server
	 * identity} under {@code _meta}, without disturbing cache hints or payload fields a method already set.
	 *
	 * @param result The neutral-to-wire result returned by {@link #invoke}. Can be any wire result type.
	 * @param config The server configuration supplying server identity.
	 * @return The same {@code result} instance, finalized in place.
	 */
	private static Object finalizeResult(Object result, McpServerConfig config, BeanStore ctx) {
		if (result instanceof Result<?> r) {
			var meta = r.getMeta();
			if (meta == null)
				meta = new ResultMeta();
			meta.setServerInfo(McpWire.serverInfo(config));
			echoTraceContext(meta, ctx);
			r.setMeta(meta);
		}
		return result;
	}

	/**
	 * Echoes the request-scoped W3C trace context into a successful result's {@code _meta}.
	 *
	 * <p>
	 * At span-start time, an active {@code TracerHook} bridge (for example {@code OtelTracerHook})
	 * stashes the rendered {@code traceparent} / {@code tracestate} / {@code baggage} values as
	 * {@link RestRequest} attributes under {@link TraceContextResponseProcessor#ATTR_TRACEPARENT} /
	 * {@code ATTR_TRACESTATE} / {@code ATTR_BAGGAGE}. This copies those same values &mdash; already
	 * neutral strings, read here with no OpenTelemetry import &mdash; into <c>meta</c> so a caller can
	 * read the server-started trace identifiers off the JSON-RPC result body, in addition to the HTTP
	 * response headers {@link TraceContextResponseProcessor} emits from the same captured values.
	 *
	 * <p>
	 * A no-op &mdash; no trace keys added &mdash; when no tracer is active (no {@code ATTR_TRACEPARENT}
	 * was stashed) or when the request-scoped {@link RestRequest} itself is unavailable.
	 *
	 * @param meta The result metadata to enrich in place. Must not be <jk>null</jk>.
	 * @param ctx The request-scoped bean store. Must not be <jk>null</jk>.
	 */
	private static void echoTraceContext(ResultMeta meta, BeanStore ctx) {
		ctx.getBean(RestRequest.class).ifPresent(request -> {
			var traceparent = request.getAttribute(TraceContextResponseProcessor.ATTR_TRACEPARENT).as(String.class).orElse(null);
			if (isEmpty(traceparent))
				return;
			meta.setTraceparent(traceparent);
			var tracestate = request.getAttribute(TraceContextResponseProcessor.ATTR_TRACESTATE).as(String.class).orElse(null);
			if (! isEmpty(tracestate))
				meta.setTracestate(tracestate);
			var baggage = request.getAttribute(TraceContextResponseProcessor.ATTR_BAGGAGE).as(String.class).orElse(null);
			if (! isEmpty(baggage))
				meta.setBaggage(baggage);
		});
	}

	// --- method table -----------------------------------------------------------------------

	private Object invoke(String method, Object params, McpServerConfig config, BeanStore ctx) {
		return switch (method) {
			case McpMethods.SERVER_DISCOVER -> discover(config);
			case McpMethods.PING -> new PingResult();
			case McpMethods.TOOLS_LIST -> listTools(config, params, ctx);
			case McpMethods.TOOLS_CALL -> callTool(config, params, ctx);
			case McpMethods.PROMPTS_LIST -> listPrompts(config, params, ctx);
			case McpMethods.PROMPTS_GET -> getPrompt(config, params, ctx);
			case McpMethods.RESOURCES_LIST -> listResources(config, params, ctx);
			case McpMethods.RESOURCES_READ -> readResource(config, params, ctx);
			case McpMethods.RESOURCES_TEMPLATES_LIST -> listResourceTemplates(config, params, ctx);
			case McpMethods.COMPLETION_COMPLETE -> complete(config, params, ctx);
			default -> throw new McpException(errorCode(McpErrorKind.UNKNOWN_METHOD), "Method not found: " + method);
		};
	}

	// --- cache-hint precedence ---------------------------------------------------------------

	private static McpCacheHint first(McpCacheHint... values) {
		for (var value : values)
			if (value != null)
				return value;
		return null;
	}

	private static <T extends CacheableResult<T>> T applyCache(T result, McpCacheHint hint) {
		if (hint != null) {
			result.setTtlMs(hint.getTtlMs());
			result.setCacheScope(hint.getCacheScope());
		}
		return result;
	}

	private McpCacheHint readHint(String uri) {
		return first(cacheConfig.getResourceReadOverrides().get(uri),
			cacheConfig.getResourcesRead(), cacheConfig.getDefaultHint());
	}

	private ServerDiscoverResult discover(McpServerConfig config) {
		var result = McpWire.discover(discoverCapabilities(config), protocolVersion(), instructions);
		return applyCache(result, first(cacheConfig.getServerDiscover(), cacheConfig.getDefaultHint()));
	}

	private ServerCapabilities discoverCapabilities(McpServerConfig config) {
		var caps = this.capabilities;
		if (caps == null) {
			caps = new ServerCapabilities();
			if (! config.getTools().isEmpty())
				caps.setTools(new ToolCapability());
			if (! config.getPrompts().isEmpty())
				caps.setPrompts(new PromptCapability());
			if (! config.getResources().isEmpty() || ! config.getResourceTemplates().isEmpty())
				caps.setResources(new ResourceCapability());
			if (config.hasAnyCompleter())
				caps.setCompletions(new CompletionCapability());
		}
		return caps;
	}

	private ListToolsResult listTools(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getTools().stream().map(McpToolHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return applyCache(new ListToolsResult()
			.setTools(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor()),
			first(cacheConfig.getToolsList(), cacheConfig.getDefaultHint()));
	}

	private Object callTool(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var name = McpParamUtils.strParam(p, "name");
		if (name == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing tool name");
		var handler = config.getTools().stream()
			.filter(h -> name.equals(h.descriptor().getName()))
			.findFirst()
			.orElseThrow(() -> new McpException(errorCode(McpErrorKind.TOOL_NOT_FOUND), "Tool not found: " + name));
		var args = McpParamUtils.mapParam(p, PARAM_ARGUMENTS);
		McpSchemaSafety.validateInput(handler.descriptor().getInputSchema(), args);
		var mrtr = resolveMrtrContext(McpMethods.TOOLS_CALL, p, ctx);
		try (var store = mrtr.store()) {
			var outcome = handler.call(args, store);
			validateStructuredOutput(outcome);
			return McpWire.toWire(outcome);
		} catch (McpInputRequiredSignal signal) {
			return pause(signal, McpMethods.TOOLS_CALL, p, mrtr.currentRound());
		}
	}

	private static void validateStructuredOutput(McpToolOutcome outcome) {
		if (outcome == null || outcome.getStructuredContent() == null)
			return;
		try {
			JsonValueSafety.check(outcome.getStructuredContent(), "Tool structuredContent");
		} catch (IllegalArgumentException e) {
			throw new McpException(CODE_INTERNAL_ERROR, e.getMessage());
		}
	}

	private ListPromptsResult listPrompts(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getPrompts().stream().map(McpPromptHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return applyCache(new ListPromptsResult()
			.setPrompts(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor()),
			first(cacheConfig.getPromptsList(), cacheConfig.getDefaultHint()));
	}

	private Object getPrompt(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var name = McpParamUtils.strParam(p, "name");
		if (name == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing prompt name");
		var handler = config.getPrompts().stream()
			.filter(h -> name.equals(h.descriptor().getName()))
			.findFirst()
			.orElseThrow(() -> new McpException(errorCode(McpErrorKind.PROMPT_NOT_FOUND), "Prompt not found: " + name));
		var args = McpParamUtils.mapParam(p, PARAM_ARGUMENTS);
		var mrtr = resolveMrtrContext(McpMethods.PROMPTS_GET, p, ctx);
		try (var store = mrtr.store()) {
			return McpWire.toWire(handler.get(args, store));
		} catch (McpInputRequiredSignal signal) {
			return pause(signal, McpMethods.PROMPTS_GET, p, mrtr.currentRound());
		}
	}

	private ListResourcesResult listResources(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getResources().stream().map(McpResourceHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return applyCache(new ListResourcesResult()
			.setResources(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor()),
			first(cacheConfig.getResourcesList(), cacheConfig.getDefaultHint()));
	}

	private Object readResource(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var uri = McpParamUtils.strParam(p, "uri");
		if (uri == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing resource uri");
		var exact = config.getResources().stream()
			.filter(h -> uri.equals(h.descriptor().getUri()))
			.findFirst();
		if (exact.isPresent()) {
			// MRTR PAUSE/RESUME applies only to the exact-resource path; C4's resource-template branch below is
			// untouched (spec rule 5). A PAUSE returns before applyCache(...), so a pending result never gets a
			// cache hint.
			var mrtr = resolveMrtrContext(McpMethods.RESOURCES_READ, p, ctx);
			try (var store = mrtr.store()) {
				return applyCache(McpWire.toWire(exact.get().read(uri, store)), readHint(uri));
			} catch (McpInputRequiredSignal signal) {
				return pause(signal, McpMethods.RESOURCES_READ, p, mrtr.currentRound());
			}
		}
		var match = config.resolveResourceTemplate(uri);
		var outcome = match == null ? null : match.handler().read(uri, match.variables(), ctx);
		if (outcome == null)
			throw new McpException(errorCode(McpErrorKind.RESOURCE_NOT_FOUND), "Resource not found: " + uri);
		return applyCache(McpWire.toWire(outcome), readHint(uri));
	}

	private ListResourceTemplatesResult listResourceTemplates(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getResourceTemplates().stream().map(McpResourceTemplateHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return applyCache(new ListResourceTemplatesResult()
			.setResourceTemplates(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor()),
			first(cacheConfig.getResourceTemplatesList(), cacheConfig.getDefaultHint()));
	}

	private CompleteResult complete(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var ref = completionRef(McpParamUtils.mapParam(p, "ref"));
		var argument = McpParamUtils.mapParam(p, "argument");
		var argumentName = McpParamUtils.strParam(argument, "name");
		if (isEmpty(argumentName))
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing argument.name");
		var value = McpParamUtils.strictStrParam(argument, "value");
		if (value == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing or non-string argument.value");
		var contextArguments = McpParamUtils.strictStrMapParam(McpParamUtils.mapParam(p, "context"), PARAM_ARGUMENTS);

		var completer = config.completer(ref, argumentName);
		McpCompletionResult raw;
		if (completer == null) {
			raw = McpCompletionResult.empty();
		} else {
			var request = new McpCompletionRequest()
				.setRef(ref)
				.setArgumentName(argumentName)
				.setValue(value)
				.setContextArguments(contextArguments);
			raw = completer.complete(request, ctx);
		}
		return McpWire.toWire(McpCompletionResult.normalize(raw));
	}

	private McpCompletionRef completionRef(Map<String,Object> refMap) {
		var type = McpParamUtils.strParam(refMap, "type");
		if ("ref/prompt".equals(type)) {
			var name = McpParamUtils.strParam(refMap, "name");
			if (isEmpty(name))
				throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing ref.name for ref/prompt");
			return McpCompletionRef.prompt(name);
		}
		if ("ref/resource".equals(type)) {
			var uri = McpParamUtils.strParam(refMap, "uri");
			if (isEmpty(uri))
				throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing ref.uri for ref/resource");
			return McpCompletionRef.resource(uri);
		}
		throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Invalid or missing ref.type: " + type);
	}

	// --- MRTR (Multi-Round-Trip Request) pause/resume ---------------------------------------

	/**
	 * The per-request MRTR resolution result: the {@link WritableBeanStore} a handler is invoked with (always
	 * carrying {@link McpMrtrCapabilityContext}, and on a RESUME also {@link McpMrtrResumeContext}) plus the
	 * current round decoded from the echoed {@code requestState} (0 on a first-round call), threaded to
	 * {@link #pause} so it can increment without a second unseal.
	 *
	 * <p>
	 * {@code store} is typed {@link WritableBeanStore} (rather than the narrower {@link BeanStore}) specifically
	 * so call sites can close it in a try-with-resources block: any bean a handler resolves through it is tracked
	 * for {@code @PreDestroy} cleanup on <i>this</i> store, not the caller-owned {@code ctx} it wraps (see
	 * {@link BasicBeanStore#close()}), so it must be closed once the handler invocation completes.
	 */
	private record MrtrContext(WritableBeanStore store, int currentRound) {}

	/**
	 * Builds the {@link BeanStore} an in-scope handler receives and, on a RESUME, validates the echoed
	 * {@code requestState} before the handler runs.
	 *
	 * <p>
	 * Always wraps {@code ctx} with a {@link McpMrtrCapabilityContext} so a handler can pre-check the client's
	 * advertised {@code elicitation} capability (spec &sect;4). When the request carries a {@code requestState}
	 * (a RESUME), it is unsealed under the canonical AAD and validated for integrity, method agreement, expiry,
	 * and the max-rounds cap &mdash; any failure raises the mapped {@link McpException} <i>before</i> the handler
	 * is re-invoked &mdash; then the decoded continuation and the client's {@code inputResponses} are exposed via
	 * {@link McpMrtrResumeContext}.
	 *
	 * @param method The in-scope JSON-RPC method. Must not be <jk>null</jk>.
	 * @param params The request params map. Must not be <jk>null</jk>.
	 * @param ctx The request-scoped bean store to wrap. Must not be <jk>null</jk>.
	 * @return The resolved MRTR context. Never <jk>null</jk>.
	 */
	private MrtrContext resolveMrtrContext(String method, Map<String,Object> params, BeanStore ctx) {
		var requestState = McpParamUtils.strParam(params, "requestState");
		if (requestState == null) {
			@SuppressWarnings({
				"resource" // Ownership transfers to the returned MrtrContext; the caller closes it via mrtr.store() in try-with-resources (see MrtrContext's javadoc above). Eclipse JDT @Owning warning is by design.
			})
			var wrapped = new BasicBeanStore(ctx)
				.addBean(McpMrtrCapabilityContext.class, new McpMrtrCapabilityContext(clientElicitationSupported(params)));
			return new MrtrContext(wrapped, 0);
		}
		// Validate the echoed requestState before constructing the BasicBeanStore below: every failure here
		// throws, and a BasicBeanStore built earlier would never reach a caller's try-with-resources to be
		// closed (java:S2095). Deferring construction until validation succeeds means no path leaks it.
		var sealed = mrtrConfig.getCodec().unseal(requestState, aad(method))
			.orElseThrow(() -> new McpException(CODE_INVALID_PARAMS, "Invalid or tampered requestState"));
		if (! method.equals(sealed.method()))
			throw new McpException(CODE_INVALID_PARAMS, "requestState method mismatch");
		if (sealed.expiresAtMs() <= System.currentTimeMillis())
			throw new McpException(CODE_REQUEST_STATE_EXPIRED, "requestState has expired");
		if (sealed.round() >= mrtrConfig.getMaxRounds())
			// Client-facing message deliberately omits the configured cap value so server config is not leaked.
			throw new McpException(CODE_MAX_ROUNDS_EXCEEDED, "Max MRTR rounds exceeded");
		var inputResponses = McpParamUtils.mapParam(params, "inputResponses");
		@SuppressWarnings({
			"resource" // Ownership transfers to the returned MrtrContext; the caller closes it via mrtr.store() in try-with-resources (see MrtrContext's javadoc above). Eclipse JDT @Owning warning is by design.
		})
		var wrapped = new BasicBeanStore(ctx)
			.addBean(McpMrtrCapabilityContext.class, new McpMrtrCapabilityContext(clientElicitationSupported(params)))
			.addBean(McpMrtrResumeContext.class, new McpMrtrResumeContext(sealed.continuation(), inputResponses));
		return new MrtrContext(wrapped, sealed.round());
	}

	/**
	 * Turns a caught {@link McpInputRequiredSignal} into a wire {@link InputRequiredResult}: capability-gates the
	 * client, seals a fresh {@code requestState} carrying the incremented round counter, and assembles the
	 * requested-inputs map.
	 *
	 * <p>
	 * The capability gate runs <i>before</i> any token is minted &mdash; a client that never advertised
	 * {@code elicitation} causes no seal at all, only a {@link #CODE_MISSING_REQUIRED_CLIENT_CAPABILITY} error.
	 *
	 * @param signal The signal thrown by the handler. Must not be <jk>null</jk>.
	 * @param method The in-scope JSON-RPC method this pause is for. Must not be <jk>null</jk>.
	 * @param params The request params map. Must not be <jk>null</jk>.
	 * @param currentRound The round decoded on RESUME (0 on a first-round pause), incremented into the new token.
	 * @return The assembled, validated {@code input_required} result. Never <jk>null</jk>.
	 */
	private InputRequiredResult pause(McpInputRequiredSignal signal, String method, Map<String,Object> params, int currentRound) {
		if (! clientElicitationSupported(params))
			throw new McpException(CODE_MISSING_REQUIRED_CLIENT_CAPABILITY,
				"Client does not advertise the elicitation capability required for input_required");
		var state = new McpRequestState(signal.getContinuation(), method, currentRound + 1, System.currentTimeMillis() + mrtrConfig.getTtlMs());
		var result = new InputRequiredResult().setRequestState(mrtrConfig.getCodec().seal(state, aad(method)));
		// Each inputRequests value is carried to the wire byte-for-byte as a raw sub-request object (see
		// McpInputRequiredSignal). The pinned schema models every value as an object; a non-map handler value is a
		// programming error that surfaces (as a ClassCastException here) via dispatch's -32603 fail-safe.
		signal.getInputRequests().forEach((id, raw) -> result.putInputRequest(id, new JsonMap((Map<?,?>) raw)));
		result.validate();
		return result;
	}

	/**
	 * The canonical MRTR AAD binding a sealed {@code requestState} to the request that produced it:
	 * {@code method + '\u0000' + protocolVersion} (NUL-separated; see {@link RequestStateCodec}). NUL cannot
	 * appear in a method name or protocol-version literal, so the concatenation is unambiguous.
	 *
	 * @param method The in-scope JSON-RPC method. Must not be <jk>null</jk>.
	 * @return The AAD string. Never <jk>null</jk>.
	 */
	private String aad(String method) {
		return method + '\u0000' + protocolVersion();
	}

	/**
	 * Reads whether the request advertised the client {@code elicitation} capability, from the opaque
	 * {@code _meta.clientCapabilities} map directly (mirroring {@link #validateMeta}'s opaque-map style rather than
	 * re-parsing a full {@code RequestMeta}/{@code ClientCapabilities} bean for one key).
	 *
	 * @param params The request params map. Must not be <jk>null</jk>.
	 * <p>
	 * {@link #validateMeta} has already run for every dispatched request and rejects a missing or non-{@code Map}
	 * {@code clientCapabilities} with {@link #CODE_INVALID_REQUEST}, so {@code caps} is always a non-<jk>null</jk>
	 * {@code Map} here &mdash; no defensive type guard is needed.
	 *
	 * @return <jk>true</jk> if {@code _meta.clientCapabilities.elicitation} is present and non-<jk>null</jk>.
	 */
	private static boolean clientElicitationSupported(Map<String,Object> params) {
		var caps = (Map<?,?>) McpWire.metaMapOrEmpty(params).get(RequestMeta.KEY_CLIENT_CAPABILITIES);
		return caps.get("elicitation") != null;
	}

	// --- trace-context extraction (Part B / trace-context propagation) ----------------------

	/**
	 * This revision's stable, stateless {@link TraceContextExtractor}, published as a {@code @Bean} by
	 * both {@link McpRestServlet} and {@link McpEndpoint} so an active {@code TracerHook} can recognize
	 * this revision's resolved {@link JsonRpcRequest} argument and its {@code params._meta} carrier
	 * before span creation.
	 *
	 * <p>
	 * Imports only the neutral {@code juneau-rest-server} tracing seam ({@link TraceContextExtractor},
	 * {@link TraceContextCarrier}, {@link TraceOperation}) &mdash; never OpenTelemetry.
	 */
	static final TraceContextExtractor TRACE_CONTEXT_EXTRACTOR = new V2TraceContextExtractor();

	/**
	 * {@link TraceContextExtractor} implementation for revision {@code 2026-07-28}.
	 *
	 * <p>
	 * Recognizes the resolved {@link JsonRpcRequest} among {@code @RestOp}-resolved arguments, reads
	 * its opaque {@code params._meta} (via {@link McpWire#metaMapOrEmpty(Object)}, since extraction
	 * runs before {@link #validateMeta(Object)} enforces the per-request contract on a request that
	 * has not yet been validated), and derives a composite carrier plus a low-cardinality
	 * {@link TraceOperation}.
	 */
	private static final class V2TraceContextExtractor implements TraceContextExtractor {

		@Override /* TraceContextExtractor */
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			var req = findRequest(resolvedArguments);
			if (req == null)
				return Optional.empty();
			var meta = new LinkedHashMap<>(McpWire.metaMapOrEmpty(req.getParams()));
			return Optional.of(new MetaCarrier(meta, request));
		}

		@Override /* TraceContextExtractor */
		public TraceOperation operation(RestRequest request, Object[] resolvedArguments) {
			var req = findRequest(resolvedArguments);
			return req == null ? TraceOperation.DEFAULT : buildOperation(req);
		}

		private static JsonRpcRequest findRequest(Object[] resolvedArguments) {
			for (var arg : resolvedArguments)
				if (arg instanceof JsonRpcRequest r)
					return r;
			return null;
		}

		/**
		 * Derives method, protocol version, request id, and tool/prompt/resource target into a
		 * {@link TraceOperation}, following the exact pinned MCP/GenAI attribute and span-name mapping:
		 * {@code "tools/call "+name}, {@code "prompts/get "+name}, bare {@code "resources/read"} (the
		 * URI stays an attribute, never a span-name suffix), or the exact method name otherwise.
		 */
		private static TraceOperation buildOperation(JsonRpcRequest req) {
			var method = req.getMethod();
			if (isEmpty(method))
				return TraceOperation.DEFAULT;
			var meta = McpWire.metaMapOrEmpty(req.getParams());
			var attrs = new LinkedHashMap<String,String>();
			attrs.put(TraceOperation.ATTR_MCP_METHOD_NAME, method);
			var protocolVersion = str(meta.get(RequestMeta.KEY_PROTOCOL_VERSION));
			if (! isEmpty(protocolVersion))
				attrs.put(TraceOperation.ATTR_MCP_PROTOCOL_VERSION, protocolVersion);
			if (nn(req.getId()))
				attrs.put(TraceOperation.ATTR_JSONRPC_REQUEST_ID, String.valueOf(req.getId()));

			var spanName = method;
			switch (method) {
				case McpMethods.TOOLS_CALL -> {
					var name = McpRoutingNames.routingName(method, req.getParams());
					if (! isEmpty(name)) {
						attrs.put(TraceOperation.ATTR_GEN_AI_TOOL_NAME, name);
						spanName = method + " " + name;
					}
					attrs.put(TraceOperation.ATTR_GEN_AI_OPERATION_NAME, "execute_tool");
				}
				case McpMethods.PROMPTS_GET -> {
					var name = McpRoutingNames.routingName(method, req.getParams());
					if (! isEmpty(name)) {
						attrs.put(TraceOperation.ATTR_GEN_AI_PROMPT_NAME, name);
						spanName = method + " " + name;
					}
				}
				case McpMethods.RESOURCES_READ -> {
					var uri = McpRoutingNames.routingName(method, req.getParams());
					if (! isEmpty(uri))
						attrs.put(TraceOperation.ATTR_MCP_RESOURCE_URI, uri);
				}
				default -> {
					// Exact method name; no method-specific attributes.
				}
			}
			return TraceOperation.of(spanName, attrs);
		}
	}

	/**
	 * Composite, per-request {@link TraceContextCarrier} giving an explicit {@code params._meta} value
	 * precedence over the equivalent HTTP header, for any key &mdash; in practice the bare W3C
	 * {@code traceparent} / {@code tracestate} / {@code baggage} keys, since those are the only names
	 * both sides share: {@code lookup(key) = params._meta[key] if present, otherwise the HTTP header of
	 * the same name}.
	 */
	private static final class MetaCarrier implements TraceContextCarrier {
		private final Map<String,Object> meta;
		private final RestRequest request;

		MetaCarrier(Map<String,Object> meta, RestRequest request) {
			this.meta = meta;
			this.request = request;
		}

		@Override /* TraceContextCarrier */
		public String get(String key) {
			var value = meta.get(key);
			if (nn(value))
				return value.toString();
			return request.getHeaderParam(key).orElse(null);
		}

		@Override /* TraceContextCarrier */
		public Iterable<String> keys() {
			var result = new LinkedHashSet<String>(meta.keySet());
			result.addAll(request.getHeaders().getNames());
			return result;
		}

		@Override /* TraceContextCarrier */
		public void set(String key, String value) {
			meta.put(key, value);
		}
	}
}
