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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.McpContentBlock;
import org.apache.juneau.rest.server.mcp.McpErrorKind;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpResponseResult;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@code 2026-07-28} {@link McpRevision} {@code params._meta} negotiation, SEP-2243 header
 * validation, and discovery dispatch.
 */
class McpRevisionValidation_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpToolHandler tool(String name, Function<Map<String,Object>,McpToolOutcome> fn) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpPromptHandler prompt(String name) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
		};
	}

	private static McpResourceHandler resource(String uri) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("res"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return new McpResourceOutcome(); }
		};
	}

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	/** Merges {@code _meta} into a copy of {@code baseParams} (an object params member, or {@code null}). */
	private static Object withMeta(Object baseParams, Object meta) {
		var p = baseParams instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", meta);
		return p;
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params);
	}

	private static Map<String,String> hdrs(String method, String name) {
		var m = new LinkedHashMap<String,String>();
		if (method != null)
			m.put("Mcp-Method", method);
		if (name != null)
			m.put("Mcp-Name", name);
		return m;
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r, Map<String,String> headers) {
		var result = new McpRevision(null).dispatch(new McpExchange(r, headers::get), config, ctx);
		return result instanceof McpResponseResult mrr ? mrr.response() : null;
	}

	// -------- protocol + error table + envelope ---------

	@Test
	void a01_protocolVersion_is_2026_07_28() {
		assertEquals("2026-07-28", new McpRevision(null).protocolVersion());
	}

	@Test
	void a02_errorTable_isComplete() {
		var r = new McpRevision(null);
		assertEquals(-32600, r.errorCode(McpErrorKind.INVALID_REQUEST));
		assertEquals(-32601, r.errorCode(McpErrorKind.UNKNOWN_METHOD));
		assertEquals(-32602, r.errorCode(McpErrorKind.TOOL_NOT_FOUND));
		assertEquals(-32602, r.errorCode(McpErrorKind.PROMPT_NOT_FOUND));
		assertEquals(-32602, r.errorCode(McpErrorKind.RESOURCE_NOT_FOUND));
		assertEquals(-32602, r.errorCode(McpErrorKind.INVALID_PARAMS));
		assertEquals(-32603, r.errorCode(McpErrorKind.INTERNAL_ERROR));
		assertEquals(-32700, r.errorCode(McpErrorKind.PARSE_ERROR));
	}

	@Test
	void a03_nullEnvelope_invalidRequest() {
		var result = new McpRevision(null).dispatch(new McpExchange(null, n -> null), new McpServerConfig(), ctx);
		var resp = result instanceof McpResponseResult mrr ? mrr.response() : null;
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	@Test
	void a04_missingMethodBody_invalidRequest() {
		var resp = send(new McpServerConfig(), req(1, null, withMeta(null, validMeta())), hdrs(null, null));
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
		assertEquals("Missing method", resp.getError().getMessage());
	}

	// -------- params shape validation (absent/non-object params) ---------

	@Test
	void b01_params_null_invalidRequest() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", null), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Request params must be an object", resp.getError().getMessage());
	}

	@Test
	void b02_params_scalar_invalidRequest() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", "x"), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Request params must be an object", resp.getError().getMessage());
	}

	@Test
	void b03_params_array_invalidRequest() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", JsonList.of(1, 2, 3)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Request params must be an object", resp.getError().getMessage());
	}

	// -------- params._meta shape validation (object params, absent/non-object _meta) ---------

	@Test
	void b04_meta_missing_mustBeObject() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", JsonMap.of()), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Request params._meta must be an object", resp.getError().getMessage());
	}

	@Test
	void b05_meta_scalar_mustBeObject() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", JsonMap.of("_meta", "x")), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Request params._meta must be an object", resp.getError().getMessage());
	}

	@Test
	void b06_meta_array_mustBeObject() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", JsonMap.of("_meta", JsonList.of(1, 2, 3))), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Request params._meta must be an object", resp.getError().getMessage());
	}

	// -------- prefixed negotiation key validation ---------

	@Test
	void c01_meta_missingProtocolVersion() {
		var meta = JsonMap.of(RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "c", "version", "1"), RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Missing required params._meta." + RequestMeta.KEY_PROTOCOL_VERSION, resp.getError().getMessage());
	}

	@Test
	void c02_meta_unsupportedProtocolVersion() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2025-06-18", RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "c", "version", "1"), RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Unsupported protocol version: 2025-06-18", resp.getError().getMessage());
	}

	@Test
	void c03_meta_missingClientInfo_isOptionalAndSucceeds() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28", RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertNull(resp.getError());
		assertInstanceOf(ServerDiscoverResult.class, resp.getResult());
	}

	@Test
	void c04_meta_malformedClientInfo_rejected() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28", RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "c"), RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Malformed params._meta." + RequestMeta.KEY_CLIENT_INFO, resp.getError().getMessage());
	}

	@Test
	void c05_meta_missingClientCapabilities() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28", RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "c", "version", "1"));
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Missing required params._meta." + RequestMeta.KEY_CLIENT_CAPABILITIES, resp.getError().getMessage());
	}

	@Test
	void c06_meta_clientCapabilitiesScalar() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28", RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "c", "version", "1"), RequestMeta.KEY_CLIENT_CAPABILITIES, true);
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("params._meta." + RequestMeta.KEY_CLIENT_CAPABILITIES + " must be an object", resp.getError().getMessage());
	}

	@Test
	void c07_meta_logLevel_acceptedAndIgnored() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28", RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of(), RequestMeta.KEY_LOG_LEVEL, "debug");
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertNull(resp.getError());
	}

	@Test
	void c08_meta_traceKeys_optionalAndAccepted() {
		var meta = JsonMap.of(RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28", RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of(),
			RequestMeta.KEY_TRACEPARENT, "00-a-b-01", RequestMeta.KEY_TRACESTATE, "vendor=x", RequestMeta.KEY_BAGGAGE, "k=v");
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertNull(resp.getError());
	}

	@Test
	void c09_meta_oldBareKeys_stillRejectedAsMissing() {
		var meta = JsonMap.of("protocolVersion", "2026-07-28", "clientInfo", JsonMap.of("name", "c", "version", "1"), "capabilities", JsonMap.of());
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, meta)), hdrs("server/discover", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Missing required params._meta." + RequestMeta.KEY_PROTOCOL_VERSION, resp.getError().getMessage());
	}

	// -------- header validation (valid metadata) ---------

	@Test
	void d01_header_missingMethod() {
		var resp = send(new McpServerConfig(), req(1, "ping", withMeta(null, validMeta())), hdrs(null, ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Missing required header: Mcp-Method", resp.getError().getMessage());
	}

	@Test
	void d02_header_missingName() {
		var resp = send(new McpServerConfig(), req(1, "ping", withMeta(null, validMeta())), hdrs("ping", null));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Missing required header: Mcp-Name", resp.getError().getMessage());
	}

	@Test
	void d03_header_methodMismatch() {
		var resp = send(new McpServerConfig(), req(1, "ping", withMeta(null, validMeta())), hdrs("pong", ""));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Mcp-Method header 'pong' does not match request method 'ping'", resp.getError().getMessage());
	}

	@Test
	void d04_header_nameMismatch() {
		var params = JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi"));
		var resp = send(new McpServerConfig(), req(1, "tools/call", withMeta(params, validMeta())), hdrs("tools/call", "wrong"));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals("Mcp-Name header 'wrong' does not match request name 'echo'", resp.getError().getMessage());
	}

	@Test
	void d05_header_validNamed_dispatches() {
		var config = new McpServerConfig().addTool(tool("echo", a -> new McpToolOutcome().setContent(List.of(McpContentBlock.text(String.valueOf(a.get("text")))))));
		var params = JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi"));
		var resp = send(config, req(1, "tools/call", withMeta(params, validMeta())), hdrs("tools/call", "echo"));
		assertNull(resp.getError());
		assertInstanceOf(CallToolResult.class, resp.getResult());
	}

	@Test
	void d06_header_validNameless_dispatches() {
		var resp = send(new McpServerConfig(), req(1, "ping", withMeta(null, validMeta())), hdrs("ping", ""));
		assertNull(resp.getError());
		assertInstanceOf(PingResult.class, resp.getResult());
	}

	// -------- discovery + method table ---------

	@Test
	void e01_serverDiscover_returnsIdentityAndCapabilities() {
		var resp = send(new McpServerConfig(), req(1, "server/discover", withMeta(null, validMeta())), hdrs("server/discover", ""));
		var result = (ServerDiscoverResult) resp.getResult();
		assertEquals("complete", result.getResultType());
		assertEquals(McpRevision.DEFAULT_SERVER_NAME, result.getMeta().getServerInfo().getName());
		assertEquals("unknown", result.getMeta().getServerInfo().getVersion());
		assertNotNull(result.getCapabilities());
		assertNull(result.getCapabilities().getTools());
	}

	@Test
	void e02_serverDiscover_derivesCapabilitiesFromRegistry() {
		var config = new McpServerConfig().addTool(tool("echo", a -> new McpToolOutcome())).addPrompt(prompt("p")).addResource(resource("file://a"));
		var resp = send(config, req(1, "server/discover", withMeta(null, validMeta())), hdrs("server/discover", ""));
		var result = (ServerDiscoverResult) resp.getResult();
		assertNotNull(result.getCapabilities().getTools());
		assertNotNull(result.getCapabilities().getPrompts());
		assertNotNull(result.getCapabilities().getResources());
	}

	@Test
	void e03_serverDiscover_explicitCapabilitiesReturnedAsIs() {
		var explicit = new ServerCapabilities().setPrompts(new PromptCapability());
		var rev = new McpRevision(explicit);
		var dispatchResult = rev.dispatch(new McpExchange(req(1, "server/discover", withMeta(null, validMeta())), hdrs("server/discover", "")::get), new McpServerConfig(), ctx);
		var resp = dispatchResult instanceof McpResponseResult mrr ? mrr.response() : null;
		var result = (ServerDiscoverResult) resp.getResult();
		assertNotNull(result.getCapabilities().getPrompts());
		assertNull(result.getCapabilities().getTools());
	}

	@Test
	void e04_ping_returnsFinalizedPingResult() {
		var resp = send(new McpServerConfig(), req(1, "ping", withMeta(null, validMeta())), hdrs("ping", ""));
		assertInstanceOf(PingResult.class, resp.getResult());
		var result = (PingResult) resp.getResult();
		assertEquals("complete", result.getResultType());
		assertNotNull(result.getMeta().getServerInfo());
	}

	@Test
	void e05_unknownMethod_methodNotFound() {
		var resp = send(new McpServerConfig(), req(1, "no/such/method", withMeta(null, validMeta())), hdrs("no/such/method", ""));
		assertEquals(McpRevision.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
		assertEquals("Method not found: no/such/method", resp.getError().getMessage());
	}

	@Test
	void e06_initialize_isUnknownMethod() {
		var resp = send(new McpServerConfig(), req(1, "initialize", withMeta(null, validMeta())), hdrs("initialize", ""));
		assertEquals(McpRevision.CODE_METHOD_NOT_FOUND, resp.getError().getCode());
		assertEquals("Method not found: initialize", resp.getError().getMessage());
	}

	@Test
	void e07_notification_returnsNullResponse() {
		var resp = send(new McpServerConfig(), req(null, "ping", withMeta(null, validMeta())), hdrs("ping", ""));
		assertNull(resp);
	}

	// -------- no handler invocation on validation failure ---------

	@Test
	void f01_noHandlerInvocation_onHeaderFailure() {
		var counter = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("echo", a -> { counter.incrementAndGet(); return new McpToolOutcome(); }));
		var params = JsonMap.of("name", "echo");
		var resp = send(config, req(1, "tools/call", withMeta(params, validMeta())), hdrs("pong", "echo"));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals(0, counter.get());
	}

	@Test
	void f02_noHandlerInvocation_onMetaFailure() {
		var counter = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("echo", a -> { counter.incrementAndGet(); return new McpToolOutcome(); }));
		var params = JsonMap.of("name", "echo");
		var badMeta = JsonMap.of(RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "c", "version", "1"), RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
		var resp = send(config, req(1, "tools/call", withMeta(params, badMeta)), hdrs("tools/call", "echo"));
		assertEquals(-32600, resp.getError().getCode());
		assertEquals(0, counter.get());
	}
}
