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
package org.apache.juneau.rest.mcp;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;

/**
 * Transport-agnostic JSON-RPC dispatcher for the MCP wire protocol.
 *
 * <p>
 * The dispatcher contains <em>zero</em> HTTP plumbing; it accepts a parsed {@link JsonRpcRequest},
 * routes by {@code method}, and returns a {@link JsonRpcResponse}. {@link McpRestServlet} is a thin
 * adapter that wraps this dispatcher in a Juneau REST {@code @RestPost} method, but embedders are
 * free to call {@link #dispatch(JsonRpcRequest, McpServerConfig, BasicBeanStore)} directly from
 * tests or from a non-Juneau servlet.
 *
 * <p>
 * Notification requests (those with a {@code null} {@link JsonRpcRequest#getId() id}) return
 * {@code null}; HTTP adapters should map this to {@code 204 No Content}.
 */
public class McpDispatcher {

	/** JSON-RPC error code: parse error. */
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
	 * Default server name used when {@link McpServerConfig#getServerInfo()} is {@code null}.
	 */
	public static final String DEFAULT_SERVER_NAME = "juneau-rest-server-mcp";

	/**
	 * Dispatch the supplied JSON-RPC request against the supplied configuration.
	 *
	 * @param req JSON-RPC request envelope. Never {@code null}.
	 * @param config Server config (handler registry, capabilities, cursor strategy). Never {@code null}.
	 * @param ctx Per-request bean store (passed through to handlers). Never {@code null}.
	 * @return The response, or {@code null} for notification requests.
	 */
	public JsonRpcResponse dispatch(JsonRpcRequest req, McpServerConfig config, BasicBeanStore ctx) {
		if (req == null)
			return errorResponse(null, CODE_INVALID_REQUEST, "Request envelope is null", null);

		var id = req.getId();
		var method = req.getMethod();

		if (method == null || method.isEmpty())
			return notification(id) ? null : errorResponse(id, CODE_INVALID_REQUEST, "Missing method", null);

		try {
			var result = invoke(method, req.getParams(), config, ctx);
			return notification(id) ? null : ok(id, result);
		} catch (McpException e) {
			return notification(id) ? null : new JsonRpcResponse()
				.setJsonrpc(McpProtocol.JSON_RPC_2_0)
				.setId(id)
				.setError(e.toJsonRpcError());
		} catch (Exception e) {
			return notification(id) ? null : errorResponse(id, CODE_INTERNAL_ERROR, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), JsonMap.of("type", e.getClass().getName()));
		}
	}

	private static boolean notification(Object id) {
		return id == null;
	}

	private Object invoke(String method, Object params, McpServerConfig config, BasicBeanStore ctx) {
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
				throw new McpException(CODE_METHOD_NOT_FOUND, "Method not found: " + method);
		}
	}

	// -------------------------------------------------------------------------------------------
	// initialize / capabilities
	// -------------------------------------------------------------------------------------------

	private static InitializeResult initialize(McpServerConfig config) {
		var caps = config.getCapabilities();
		if (caps == null) {
			caps = new ServerCapabilities();
			if (! config.getTools().isEmpty())
				caps.setTools(new ToolCapability());
			if (! config.getPrompts().isEmpty())
				caps.setPrompts(new PromptCapability());
			if (! config.getResources().isEmpty())
				caps.setResources(new ResourceCapability());
		}
		var info = config.getServerInfo();
		if (info == null)
			info = new Implementation().setName(DEFAULT_SERVER_NAME).setVersion("unknown");
		return new InitializeResult()
			.setProtocolVersion(config.getProtocolVersion())
			.setCapabilities(caps)
			.setServerInfo(info)
			.setInstructions(config.getInstructions());
	}

	// -------------------------------------------------------------------------------------------
	// tools
	// -------------------------------------------------------------------------------------------

	private static ListToolsResult listTools(McpServerConfig config, Object params, BasicBeanStore ctx) {
		var descriptors = config.getTools().stream().map(McpToolHandler::descriptor).collect(Collectors.toList());
		var page = config.getCursor().page(descriptors, cursorOf(params), ctx);
		return new ListToolsResult().setTools(page.items()).setNextCursor(page.nextCursor());
	}

	private static CallToolResult callTool(McpServerConfig config, Object params, BasicBeanStore ctx) {
		var p = asMap(params);
		var name = strParam(p, "name");
		if (name == null)
			throw new McpException(CODE_INVALID_PARAMS, "Missing tool name");
		var handler = config.getTools().stream()
			.filter(h -> name.equals(h.descriptor().getName()))
			.findFirst()
			.orElseThrow(() -> new McpException(CODE_METHOD_NOT_FOUND, "Tool not found: " + name));
		var args = mapParam(p, "arguments");
		return handler.call(args, ctx);
	}

	// -------------------------------------------------------------------------------------------
	// prompts
	// -------------------------------------------------------------------------------------------

	private static ListPromptsResult listPrompts(McpServerConfig config, Object params, BasicBeanStore ctx) {
		var descriptors = config.getPrompts().stream().map(McpPromptHandler::descriptor).collect(Collectors.toList());
		var page = config.getCursor().page(descriptors, cursorOf(params), ctx);
		return new ListPromptsResult().setPrompts(page.items()).setNextCursor(page.nextCursor());
	}

	private static GetPromptResult getPrompt(McpServerConfig config, Object params, BasicBeanStore ctx) {
		var p = asMap(params);
		var name = strParam(p, "name");
		if (name == null)
			throw new McpException(CODE_INVALID_PARAMS, "Missing prompt name");
		var handler = config.getPrompts().stream()
			.filter(h -> name.equals(h.descriptor().getName()))
			.findFirst()
			.orElseThrow(() -> new McpException(CODE_METHOD_NOT_FOUND, "Prompt not found: " + name));
		var args = mapParam(p, "arguments");
		return handler.get(args, ctx);
	}

	// -------------------------------------------------------------------------------------------
	// resources
	// -------------------------------------------------------------------------------------------

	private static ListResourcesResult listResources(McpServerConfig config, Object params, BasicBeanStore ctx) {
		var descriptors = config.getResources().stream().map(McpResourceHandler::descriptor).collect(Collectors.toList());
		var page = config.getCursor().page(descriptors, cursorOf(params), ctx);
		return new ListResourcesResult().setResources(page.items()).setNextCursor(page.nextCursor());
	}

	private static ReadResourceResult readResource(McpServerConfig config, Object params, BasicBeanStore ctx) {
		var p = asMap(params);
		var uri = strParam(p, "uri");
		if (uri == null)
			throw new McpException(CODE_INVALID_PARAMS, "Missing resource uri");
		var handler = config.getResources().stream()
			.filter(h -> uri.equals(h.descriptor().getUri()))
			.findFirst()
			.orElseThrow(() -> new McpException(CODE_METHOD_NOT_FOUND, "Resource not found: " + uri));
		return handler.read(uri, ctx);
	}

	// -------------------------------------------------------------------------------------------
	// helpers
	// -------------------------------------------------------------------------------------------

	private static String cursorOf(Object params) {
		return strParam(asMap(params), "cursor");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object params) {
		if (params == null)
			return Map.of();
		if (params instanceof Map)
			return (Map<String, Object>) params;
		throw new McpException(CODE_INVALID_PARAMS, "Params must be an object");
	}

	private static String strParam(Map<String, Object> p, String key) {
		var v = p.get(key);
		return v == null ? null : v.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> mapParam(Map<String, Object> p, String key) {
		var v = p.get(key);
		if (v == null)
			return Map.of();
		if (v instanceof Map)
			return (Map<String, Object>) v;
		throw new McpException(CODE_INVALID_PARAMS, "Param '" + key + "' must be an object");
	}

	private static JsonRpcResponse ok(Object id, Object result) {
		return new JsonRpcResponse()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(id)
			.setResult(result);
	}

	private static JsonRpcResponse errorResponse(Object id, int code, String message, Object data) {
		return new JsonRpcResponse()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(id)
			.setError(new JsonRpcError().setCode(code).setMessage(message).setData(data));
	}
}
