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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.utils.JsonValueSafety;
import org.apache.juneau.marshall.collections.*;
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
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;

/**
 * {@link McpRevision} implementation for MCP revision {@code 2025-06-18}.
 *
 * <p>
 * Owns this revision's JSON-RPC method table and error-code table. Note the error-code table
 * deliberately reproduces a known-wrong mapping: {@link McpErrorKind#UNKNOWN_METHOD},
 * {@link McpErrorKind#TOOL_NOT_FOUND} and {@link McpErrorKind#PROMPT_NOT_FOUND} all report
 * {@code -32601}, which is only actually correct for the first. Preserving that is intentional —
 * the corrective fix for those two is tracked separately and is not part of this re-layering.
 * {@link McpErrorKind#RESOURCE_NOT_FOUND} is not part of that known-wrong grouping: it correctly
 * reports {@code -32002}, the {@code 2025-06-18} spec's dedicated missing-resource error code.
 *
 * <p>
 * <b>Constructed per binding, not shared as a singleton (Correction C8).</b> The bound servlet or
 * mixin builds a fresh instance on every {@code revision()} call, passing its own
 * {@code capabilities()} hook result into the constructor — see {@link McpRestServlet} and
 * {@link McpEndpoint}. That is how this revision learns which explicit capabilities (if any)
 * to advertise, with no channel back into core: this class is otherwise entirely stateless, so
 * constructing a new one per call costs nothing.
 */
@SuppressWarnings({
	"java:S2176" // Intentional: dated adapter binding classes are de-versioned and differentiated by package (see TODO-312).
})
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

	/** JSON-RPC error code: resource not found (MCP-specific, per the {@code 2025-06-18} spec). */
	public static final int CODE_RESOURCE_NOT_FOUND = -32002;

	/** Default server name reported by {@code initialize} when the config supplies no server identity. */
	public static final String DEFAULT_SERVER_NAME = "juneau-rest-server-mcp";

	private static final String PARAM_ARGUMENTS = "arguments";

	private static final Set<String> SUPPORTED_SCHEMA_KEYWORDS =
		Set.of("type", "properties", "required", "additionalProperties", "items", "$defs");

	private static final Set<McpServerConfig> VALIDATED =
		Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	/**
	 * Verifies that every registered tool's input <em>and</em> output schema is expressible in this
	 * revision's wire schema type.
	 *
	 * <p>
	 * The neutral {@link McpSchema} is an unconstrained JSON object carrier, but this revision's
	 * {@code JsonSchema} bean supports exactly six keywords and models only object schemas. Each
	 * schema role is therefore checked twice: its root must declare {@code "type":"object"}, and the
	 * complete graph reachable from that root may use only this revision's six keywords. Rather than
	 * silently dropping an unsupported keyword — or an incompatible root shape — on the wire, a config
	 * carrying one is rejected, naming the tool, the offending role ({@code inputSchema} /
	 * {@code outputSchema}), and the offending shape or keyword.
	 *
	 * <p>
	 * Before walking, each schema is passed through {@link JsonValueSafety} to bound nesting depth,
	 * node count, traversal time, and cyclic/shared references, so a pathological config cannot stall
	 * or exhaust the walk.
	 *
	 * <p>
	 * Note this cannot be a complete guarantee against every possible wire shape: {@code JsonSchema}
	 * is {@code public} and non-{@code final}, and Juneau's reflection-based marshalling will
	 * serialize a getter added by a subclass. No such subclass exists in this codebase, and none can
	 * be constructed through the public MCP API, but the check covers what a caller can express
	 * through {@link McpSchema}, not what reflection can reach.
	 *
	 * @param config The config to validate. Never {@code null}.
	 * @throws IllegalArgumentException If any tool's input or output schema has a non-object root or uses an unsupported keyword.
	 */
	public static void validateSchemas(McpServerConfig config) {
		config.getTools().forEach(handler -> {
			var spec = handler.descriptor();
			if (spec != null) {
				validateSchema(spec.getName(), "inputSchema", spec.getInputSchema());
				validateSchema(spec.getName(), "outputSchema", spec.getOutputSchema());
			}
		});
	}

	private static void validateSchema(String toolName, String role, McpSchema schema) {
		if (schema == null)
			return;
		var raw = schema.toJsonMap();
		JsonValueSafety.check(raw, "Tool '" + toolName + "' " + role + " for MCP revision 2025-06-18");
		if (! "object".equals(raw.get("type")))
			throw iaex("Tool ''%s'' %s root type ''%s'' is not object; MCP revision 2025-06-18 requires object schemas.",
				toolName, role, raw.get("type"));
		checkKeywords(toolName, role, raw);
	}

	private static void checkKeywords(String toolName, String role, Object root) {
		var seen = Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
		var stack = new ArrayDeque<SchemaValue>();
		stack.push(new SchemaValue(root));
		while (! stack.isEmpty()) {
			var framed = stack.pop();
			var value = framed.value();
			if ((value instanceof Map<?,?> || value instanceof Collection<?> || value != null && value.getClass().isArray())
				&& ! seen.add(value))
				continue;
			if (value instanceof Map<?,?> value2) {
				for (var entry : value2.entrySet()) {
					var keyword = String.valueOf(entry.getKey());
					if (! SUPPORTED_SCHEMA_KEYWORDS.contains(keyword))
						throw iaex("Tool ''%s'' %s declares JSON Schema keyword ''%s'', which MCP revision 2025-06-18 cannot represent.",
							toolName, role, keyword);
					var child = entry.getValue();
					if (("properties".equals(keyword) || "$defs".equals(keyword)) && child instanceof Map<?,?> child2) {
						for (var schema : child2.values())
							stack.push(new SchemaValue(schema));
					} else {
						stack.push(new SchemaValue(child));
					}
				}
			} else if (value instanceof Collection<?> value2)
				value2.forEach(child -> stack.push(new SchemaValue(child)));
			else if (value != null && value.getClass().isArray())
				for (var i = 0; i < Array.getLength(value); i++)
					stack.push(new SchemaValue(Array.get(value, i)));
		}
	}

	private record SchemaValue(Object value) {}

	@Override /* McpRevision */
	public String protocolVersion() {
		return McpProtocol.VERSION_2025_06_18;
	}

	@Override /* McpRevision */
	public int errorCode(McpErrorKind kind) {
		return switch (kind) {
			case INVALID_REQUEST -> CODE_INVALID_REQUEST;
			case UNKNOWN_METHOD, TOOL_NOT_FOUND, PROMPT_NOT_FOUND -> CODE_METHOD_NOT_FOUND;
			case RESOURCE_NOT_FOUND -> CODE_RESOURCE_NOT_FOUND;
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
			case McpMethods.RESOURCES_TEMPLATES_LIST:
				return listResourceTemplates(config, params, ctx);
			case McpMethods.COMPLETION_COMPLETE:
				return complete(config, params, ctx);
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
			if (! config.getResources().isEmpty() || ! config.getResourceTemplates().isEmpty())
				caps.setResources(new ResourceCapability());
			if (config.hasAnyCompleter())
				caps.setCompletions(new CompletionCapability());
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
		var args = McpParamUtils.mapParam(p, PARAM_ARGUMENTS);
		var outcome = handler.call(args, ctx);
		validateStructuredOutput(outcome);
		return McpWire.toWire(outcome);
	}

	private static void validateStructuredOutput(McpToolOutcome outcome) {
		if (outcome == null || outcome.getStructuredContent() == null)
			return;
		if (! (outcome.getStructuredContent() instanceof Map<?,?>))
			throw new McpException(CODE_INTERNAL_ERROR,
				"Tool structuredContent must be a JSON object for MCP revision 2025-06-18");
		try {
			JsonValueSafety.check(outcome.getStructuredContent(), "Tool structuredContent");
		} catch (IllegalArgumentException e) {
			throw new McpException(CODE_INTERNAL_ERROR, e.getMessage());
		}
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
		var args = McpParamUtils.mapParam(p, PARAM_ARGUMENTS);
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
		var exact = config.getResources().stream()
			.filter(h -> uri.equals(h.descriptor().getUri()))
			.findFirst();
		if (exact.isPresent())
			return McpWire.toWire(exact.get().read(uri, ctx));
		var match = config.resolveResourceTemplate(uri);
		var outcome = match == null ? null : match.handler().read(uri, match.variables(), ctx);
		if (outcome == null)
			throw new McpException(errorCode(McpErrorKind.RESOURCE_NOT_FOUND), "Resource not found: " + uri);
		return McpWire.toWire(outcome);
	}

	private static ListResourceTemplatesResult listResourceTemplates(McpServerConfig config, Object params, BeanStore ctx) {
		var descriptors = config.getResourceTemplates().stream().map(McpResourceTemplateHandler::descriptor).toList();
		var page = config.getCursor().page(descriptors, McpCursor.cursorOf(params), ctx);
		return new ListResourceTemplatesResult()
			.setResourceTemplates(page.items().stream().map(McpWire::toWire).toList())
			.setNextCursor(page.nextCursor());
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
}
