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
package org.apache.juneau.rest.server.mcp.v20250618;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.McpCursor;
import org.apache.juneau.rest.server.mcp.McpErrorKind;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpParamUtils;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;

/**
 * {@link McpRevision} implementation for MCP revision {@code 2025-06-18}.
 *
 * <p>
 * Owns this revision's JSON-RPC method table and error-code table. Note the error-code table
 * deliberately reproduces a known-wrong mapping: {@link McpErrorKind#UNKNOWN_METHOD},
 * {@link McpErrorKind#TOOL_NOT_FOUND}, {@link McpErrorKind#PROMPT_NOT_FOUND} and
 * {@link McpErrorKind#RESOURCE_NOT_FOUND} all report {@code -32601}, which is only actually correct
 * for the first. Preserving that is intentional — the corrective fix is tracked separately and is
 * not part of this re-layering.
 *
 * <p>
 * <b>Constructed per binding, not shared as a singleton (Correction C8).</b> The bound servlet or
 * mixin builds a fresh instance on every {@code revision()} call, passing its own
 * {@code capabilities()} hook result into the constructor — see {@link McpRestServlet} and
 * {@link McpEndpoint}. That is how this revision learns which explicit capabilities (if any)
 * to advertise, with no channel back into core: this class is otherwise entirely stateless, so
 * constructing a new one per call costs nothing.
 */
public final class McpRevision implements org.apache.juneau.rest.server.mcp.McpRevision {

	private final ServerCapabilities capabilities;

	/**
	 * Constructor.
	 *
	 * @param capabilities Explicit capabilities to advertise on {@code initialize}, or <jk>null</jk>
	 * 	to auto-derive from the registered tool/prompt/resource lists (today's default behavior).
	 */
	public McpRevision(ServerCapabilities capabilities) {
		this.capabilities = capabilities;
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

	/** Default server name reported by {@code initialize} when the config supplies no server identity. */
	public static final String DEFAULT_SERVER_NAME = "juneau-rest-server-mcp";

	private static final Set<String> SUPPORTED_SCHEMA_KEYWORDS =
		Set.of("type", "properties", "required", "additionalProperties", "items", "$defs");

	private static final Set<McpServerConfig> VALIDATED =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	/**
	 * Verifies that every registered tool's input schema is expressible in this revision's wire
	 * schema type.
	 *
	 * <p>
	 * The neutral {@link McpSchema} is an unconstrained JSON object carrier, but this revision's
	 * {@code JsonSchema} bean supports exactly six keywords. Rather than silently dropping an
	 * unsupported keyword on the wire, a config carrying one is rejected, naming both the tool and
	 * the keyword.
	 *
	 * <p>
	 * Note this cannot be a complete guarantee against every possible wire shape: {@code JsonSchema}
	 * is {@code public} and non-{@code final}, and Juneau's reflection-based marshalling will
	 * serialize a getter added by a subclass. No such subclass exists in this codebase, and none can
	 * be constructed through the public MCP API, but the check covers what a caller can express
	 * through {@link McpSchema}, not what reflection can reach.
	 *
	 * @param config The config to validate. Never {@code null}.
	 * @throws IllegalArgumentException If any tool's schema uses an unsupported keyword.
	 */
	public static void validateSchemas(McpServerConfig config) {
		config.getTools().forEach(x -> {
			var spec = x.descriptor();
			if (spec != null && spec.getInputSchema() != null)
				checkKeywords(spec.getName(), spec.getInputSchema().toJsonMap());
		});
	}

	private static void checkKeywords(String toolName, Map<String,Object> schema) {
		schema.forEach((k, v) -> {
			if (! SUPPORTED_SCHEMA_KEYWORDS.contains(k))
				throw iaex("Tool ''%s'' declares JSON Schema keyword ''%s'', which MCP revision 2025-06-18 cannot represent.", toolName, k);
			if (v instanceof Map<?,?> v2)
				v2.forEach((k2, v3) -> {
					if (v3 instanceof Map<?,?> v4)
						checkKeywords(toolName, asStringKeyed(v4));
				});
		});
	}

	@SuppressWarnings({
		"unchecked" // Cast is safe: JSON object keys are always strings.
	})
	private static Map<String,Object> asStringKeyed(Map<?,?> x) {
		return (Map<String,Object>) x;
	}

	@Override /* McpRevision */
	public String protocolVersion() {
		return McpProtocol.VERSION_2025_06_18;
	}

	@Override /* McpRevision */
	public int errorCode(McpErrorKind kind) {
		return switch (kind) {
			case INVALID_REQUEST -> CODE_INVALID_REQUEST;
			case UNKNOWN_METHOD, TOOL_NOT_FOUND, PROMPT_NOT_FOUND, RESOURCE_NOT_FOUND -> CODE_METHOD_NOT_FOUND;
			case INVALID_PARAMS -> CODE_INVALID_PARAMS;
			case INTERNAL_ERROR -> CODE_INTERNAL_ERROR;
			case PARSE_ERROR -> CODE_PARSE_ERROR;
		};
	}

	@Override /* McpRevision */
	public JsonRpcResponse dispatch(McpExchange exchange, McpServerConfig config, BeanStore ctx) {
		assertArgNotNull("exchange", exchange);
		assertArgNotNull("config", config);
		assertArgNotNull("ctx", ctx);

		if (VALIDATED.add(config))
			validateSchemas(config);

		var req = exchange.request();
		if (req == null)
			return JsonRpcResponse.errorResponse(null, errorCode(McpErrorKind.INVALID_REQUEST), "Request envelope is null");

		var id = req.getId();
		var method = req.getMethod();

		if (isEmpty(method))
			return JsonRpcResponse.notification(id) ? null
				: JsonRpcResponse.errorResponse(id, errorCode(McpErrorKind.INVALID_REQUEST), "Missing method");

		try {
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

	private Object invoke(String method, Object params, McpServerConfig config, BeanStore ctx) {
		switch (method) {
			case McpMethods.INITIALIZE:
				return initialize(config);
			case McpMethods.PING:
				return new JsonMap();
			case McpMethods.TOOLS_LIST:
				return listTools(config, params, ctx);
			case McpMethods.TOOLS_CALL:
				return callTool(config, params, ctx);
			case McpMethods.PROMPTS_LIST:
				return listPrompts(config, params, ctx);
			case McpMethods.PROMPTS_GET:
				return getPrompt(config, params, ctx);
			case McpMethods.RESOURCES_LIST:
				return listResources(config, params, ctx);
			case McpMethods.RESOURCES_READ:
				return readResource(config, params, ctx);
			default:
				throw new McpException(errorCode(McpErrorKind.UNKNOWN_METHOD), "Method not found: " + method);
		}
	}

	private InitializeResult initialize(McpServerConfig config) {
		var caps = this.capabilities;
		if (caps == null) {
			caps = new ServerCapabilities();
			if (! config.getTools().isEmpty())
				caps.setTools(new ToolCapability());
			if (! config.getPrompts().isEmpty())
				caps.setPrompts(new PromptCapability());
			if (! config.getResources().isEmpty())
				caps.setResources(new ResourceCapability());
		}
		return new InitializeResult()
			.setProtocolVersion(protocolVersion())
			.setCapabilities(caps)
			.setServerInfo(McpWire.serverInfo(config))
			.setInstructions(config.getInstructions());
	}

	private static ListToolsResult listTools(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getTools().stream().map(McpToolHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return new ListToolsResult()
			.setTools(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor());
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
		return McpWire.toWire(handler.call(args, ctx));
	}

	private static ListPromptsResult listPrompts(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getPrompts().stream().map(McpPromptHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return new ListPromptsResult()
			.setPrompts(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor());
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

	private static ListResourcesResult listResources(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getResources().stream().map(McpResourceHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return new ListResourcesResult()
			.setResources(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor());
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
		return McpWire.toWire(handler.read(uri, ctx));
	}
}
