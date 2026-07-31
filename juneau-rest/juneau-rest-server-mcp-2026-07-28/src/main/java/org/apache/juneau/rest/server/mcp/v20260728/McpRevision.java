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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.McpCursor;
import org.apache.juneau.rest.server.mcp.McpErrorKind;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpJsonValueSafety;
import org.apache.juneau.rest.server.mcp.McpParamUtils;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
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
 * fresh instance on every {@code revision()} call, passing its own {@code capabilities()} hook
 * result and its published {@link McpCacheConfig} into the constructor — see {@link McpRestServlet}
 * and {@link McpEndpoint}. A {@code null} capabilities override auto-derives {@code server/discover}
 * capabilities from the registered tool/prompt/resource/resource-template lists; a non-{@code null}
 * override is advertised as-is. The cache config is binding-owned: it is supplied once at
 * construction, is never {@code null}, and is treated as static and request-independent for the
 * lifetime of this instance — every page of a given list method resolves to identical cache values.
 * The instance retains no request-derived state.
 */
public final class McpRevision implements org.apache.juneau.rest.server.mcp.McpRevision {

	private final ServerCapabilities capabilities;
	private final McpCacheConfig cacheConfig;

	/**
	 * Constructor. Uses an empty {@link McpCacheConfig} (no cache hints emitted on any result).
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code server/discover}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists.
	 */
	public McpRevision(ServerCapabilities capabilities) {
		this(capabilities, new McpCacheConfig());
	}

	/**
	 * Constructor.
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code server/discover}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists.
	 * @param cacheConfig Binding-owned cache configuration. Must not be <jk>null</jk>.
	 */
	public McpRevision(ServerCapabilities capabilities, McpCacheConfig cacheConfig) {
		this.capabilities = capabilities;
		this.cacheConfig = Objects.requireNonNull(cacheConfig, "cacheConfig");
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

	/** Default server name reported by {@code server/discover} when the config supplies no server identity. */
	public static final String DEFAULT_SERVER_NAME = "juneau-rest-server-mcp";

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
	public JsonRpcResponse dispatch(McpExchange exchange, McpServerConfig config, BeanStore ctx) {
		assertArgNotNull("exchange", exchange);
		assertArgNotNull("config", config);
		assertArgNotNull("ctx", ctx);

		var req = exchange.request();
		if (req == null)
			return JsonRpcResponse.errorResponse(null, errorCode(McpErrorKind.INVALID_REQUEST), "Request envelope is null");

		var id = req.getId();
		var method = req.getMethod();

		if (isEmpty(method))
			return JsonRpcResponse.notification(id) ? null
				: JsonRpcResponse.errorResponse(id, errorCode(McpErrorKind.INVALID_REQUEST), "Missing method");

		try {
			validateHeaders(exchange, req);
			validateMeta(req.getMeta());
			var result = invoke(method, req.getParams(), config, ctx);
			return JsonRpcResponse.notification(id) ? null : JsonRpcResponse.ok(id, result);
		} catch (McpException e) {
			return JsonRpcResponse.notification(id) ? null : new JsonRpcResponse()
				.setJsonrpc(McpProtocol.JSON_RPC_2_0)
				.setId(id)
				.setError(e.toJsonRpcError());
		} catch (Exception e) {
			if (JsonRpcResponse.notification(id))
				return null;
			var message = e.getMessage() == null ? cns(e) : e.getMessage();
			return JsonRpcResponse.errorResponse(id, errorCode(McpErrorKind.INTERNAL_ERROR), message, JsonMap.of("type", cn(e)));
		}
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
		var name = routingName(method, req.getParams());
		if (! headerName.equals(name))
			throw new McpException(CODE_INVALID_REQUEST, "Mcp-Name header '" + headerName + "' does not match request name '" + name + "'");
	}

	/**
	 * The routing name a request's {@code Mcp-Name} header must equal (SEP-2243 / Resolution B3):
	 * {@code params.name} for {@code tools/call} and {@code prompts/get}, {@code params.uri} for
	 * {@code resources/read}, and the empty string for every other method.
	 */
	private static String routingName(String method, Object params) {
		var name = switch (method) {
			case McpMethods.TOOLS_CALL, McpMethods.PROMPTS_GET -> paramValue(params, "name");
			case McpMethods.RESOURCES_READ -> paramValue(params, "uri");
			default -> "";
		};
		return name == null ? "" : name;
	}

	private static String paramValue(Object params, String key) {
		if (params instanceof Map<?,?> m) {
			var v = m.get(key);
			return v == null ? null : v.toString();
		}
		return null;
	}

	// --- per-request _meta negotiation ------------------------------------------------------

	/**
	 * Validates the opaque {@code _meta} shape directly.
	 *
	 * <p>
	 * The parsed {@link RequestMeta} bean cannot distinguish a malformed {@code capabilities} (a
	 * scalar/array) from an absent one — parsing either into {@code ClientCapabilities} raises a
	 * generic parse error rather than a validation failure — so metadata shape is checked against
	 * the opaque map to produce the exact per-field {@code -32600} messages.
	 */
	private static void validateMeta(Object meta) {
		if (! (meta instanceof Map<?,?> m))
			throw new McpException(CODE_INVALID_REQUEST, "Request _meta must be an object");
		var protocolVersion = str(m.get("protocolVersion"));
		if (isEmpty(protocolVersion))
			throw new McpException(CODE_INVALID_REQUEST, "Missing required _meta.protocolVersion");
		if (! McpProtocol.VERSION_2026_07_28.equals(protocolVersion))
			throw new McpException(CODE_INVALID_REQUEST, "Unsupported protocol version: " + protocolVersion);
		var clientInfo = m.get("clientInfo");
		if (! (clientInfo instanceof Map<?,?> ci) || isEmpty(str(ci.get("name"))) || isEmpty(str(ci.get("version"))))
			throw new McpException(CODE_INVALID_REQUEST, "Missing required _meta.clientInfo");
		if (! m.containsKey("capabilities") || m.get("capabilities") == null)
			throw new McpException(CODE_INVALID_REQUEST, "Missing required _meta.capabilities");
		if (! (m.get("capabilities") instanceof Map<?,?>))
			throw new McpException(CODE_INVALID_REQUEST, "_meta.capabilities must be an object");
	}

	private static String str(Object value) {
		return value == null ? null : value.toString();
	}

	// --- method table -----------------------------------------------------------------------

	private Object invoke(String method, Object params, McpServerConfig config, BeanStore ctx) {
		return switch (method) {
			case McpMethods.SERVER_DISCOVER -> McpWire.discover(config, discoverCapabilities(config));
			case McpMethods.PING -> new JsonMap();
			case McpMethods.TOOLS_LIST -> listTools(config, params, ctx);
			case McpMethods.TOOLS_CALL -> callTool(config, params, ctx);
			case McpMethods.PROMPTS_LIST -> listPrompts(config, params, ctx);
			case McpMethods.PROMPTS_GET -> getPrompt(config, params, ctx);
			case McpMethods.RESOURCES_LIST -> listResources(config, params, ctx);
			case McpMethods.RESOURCES_READ -> readResource(config, params, ctx);
			case McpMethods.RESOURCES_TEMPLATES_LIST -> listResourceTemplates(config, params, ctx);
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

	private CallToolResult callTool(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var name = McpParamUtils.strParam(p, "name");
		if (name == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing tool name");
		var handler = config.getTools().stream()
			.filter(h -> name.equals(h.descriptor().getName()))
			.findFirst()
			.orElseThrow(() -> new McpException(errorCode(McpErrorKind.TOOL_NOT_FOUND), "Tool not found: " + name));
		var args = McpParamUtils.mapParam(p, "arguments");
		McpSchemaSafety.validateInput(handler.descriptor().getInputSchema(), args);
		var outcome = handler.call(args, ctx);
		validateStructuredOutput(outcome);
		return McpWire.toWire(outcome);
	}

	private static void validateStructuredOutput(McpToolOutcome outcome) {
		if (outcome == null || outcome.getStructuredContent() == null)
			return;
		try {
			McpJsonValueSafety.check(outcome.getStructuredContent(), "Tool structuredContent");
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

	private GetPromptResult getPrompt(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var name = McpParamUtils.strParam(p, "name");
		if (name == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing prompt name");
		var handler = config.getPrompts().stream()
			.filter(h -> name.equals(h.descriptor().getName()))
			.findFirst()
			.orElseThrow(() -> new McpException(errorCode(McpErrorKind.PROMPT_NOT_FOUND), "Prompt not found: " + name));
		var args = McpParamUtils.mapParam(p, "arguments");
		return McpWire.toWire(handler.get(args, ctx));
	}

	private ListResourcesResult listResources(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getResources().stream().map(McpResourceHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return applyCache(new ListResourcesResult()
			.setResources(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor()),
			first(cacheConfig.getResourcesList(), cacheConfig.getDefaultHint()));
	}

	private ReadResourceResult readResource(McpServerConfig config, Object params, BeanStore ctx) {
		var p = McpParamUtils.asMap(params);
		var uri = McpParamUtils.strParam(p, "uri");
		if (uri == null)
			throw new McpException(errorCode(McpErrorKind.INVALID_PARAMS), "Missing resource uri");
		var handler = config.getResources().stream()
			.filter(h -> uri.equals(h.descriptor().getUri()))
			.findFirst()
			.orElseThrow(() -> new McpException(errorCode(McpErrorKind.RESOURCE_NOT_FOUND), "Resource not found: " + uri));
		return applyCache(McpWire.toWire(handler.read(uri, ctx)), readHint(uri));
	}

	private ListResourceTemplatesResult listResourceTemplates(McpServerConfig config, Object params, BeanStore ctx) {
		var page = config.getCursor().page(config.getResourceTemplates(), McpCursor.cursorOf(params), ctx);
		return applyCache(new ListResourceTemplatesResult()
			.setResourceTemplates(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor()),
			first(cacheConfig.getResourceTemplatesList(), cacheConfig.getDefaultHint()));
	}
}
