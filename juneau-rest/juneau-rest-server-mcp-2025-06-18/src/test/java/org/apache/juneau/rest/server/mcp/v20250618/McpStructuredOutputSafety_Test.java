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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Coverage for post-handler {@code structuredContent} validation in {@link McpRevision#callTool}: a
 * tool's returned structured result is checked after the handler returns and before wire conversion,
 * raising {@code -32603} (internal error) rather than the {@code -32602} (invalid params) used for
 * input failures. This revision additionally requires the structured result to be a JSON object
 * (the {@code 2025-06-18} wire {@code CallToolResult.structuredContent} is object-shaped), checked
 * before the shared depth/node/deadline bounds.
 */
class McpStructuredOutputSafety_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpServerConfig configReturning(Object value) {
		return new McpServerConfig().addTool(new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("out"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				return McpToolOutcome.text("ok").setStructuredContent(value);
			}
		});
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params);
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r) {
		return new McpRevision(null).dispatch(new McpExchange(r, n -> null), config, ctx);
	}

	private static JsonRpcRequest requestWithId(Object id) {
		return req(id, McpMethods.TOOLS_CALL, JsonMap.of("name", "out", "arguments", JsonMap.of()));
	}

	private static JsonRpcRequest notificationWithoutId() {
		return requestWithId(null);
	}

	/** Builds a map nested {@code depth} levels deep. */
	private static Map<String,Object> nest(int depth) {
		Map<String,Object> node = new LinkedHashMap<>();
		for (var i = 1; i < depth; i++) {
			var parent = new LinkedHashMap<String,Object>();
			parent.put("child", node);
			node = parent;
		}
		return node;
	}

	/** Builds a list nested {@code depth} levels deep. */
	private static List<Object> nestList(int depth) {
		List<Object> node = new ArrayList<>();
		for (var i = 1; i < depth; i++) {
			var parent = new ArrayList<Object>();
			parent.add(node);
			node = parent;
		}
		return node;
	}

	private static Map<String,Object> flat(int nodeCount) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < nodeCount - 1; i++)  // root map (1) + (nodeCount-1) scalar entries == nodeCount nodes
			m.put("k" + i, i);
		return m;
	}

	// -------- failure cases: structuredContent violating the shared bounds ---------

	private void assertStructuredOutputRejected(Object value) {
		var a = send(configReturning(value), requestWithId(1));
		assertEquals(-32603, a.getError().getCode());
		assertContains("Tool structuredContent", a.getError().getMessage());
		assertFalse(a.getError().getMessage().contains("input"));
		assertNull(send(configReturning(value), notificationWithoutId()));
	}

	@Test
	void a01_depth65Object_rejected() {
		assertStructuredOutputRejected(nest(65));
	}

	@Test
	void a02_depth65Array_rejected() {
		// Not a Map at all: this revision's object-shape guard rejects it before the depth check runs.
		assertStructuredOutputRejected(nestList(65));
	}

	@Test
	void a03_nodes10001_rejected() {
		assertStructuredOutputRejected(flat(10_001));
	}

	@Test
	void a04_nonJsonLeaf_rejected() {
		assertStructuredOutputRejected(JsonMap.of("x", new Object()));
	}

	// -------- revision-specific object-shape guard ---------

	@Test
	void a05_arrayStructuredContent_rejected_notObject() {
		var a = send(configReturning(JsonList.of(1, 2)), requestWithId(1));
		assertEquals(-32603, a.getError().getCode());
		assertContains("2025-06-18", a.getError().getMessage());
		assertContains("JSON object", a.getError().getMessage());
	}

	@Test
	void a06_stringStructuredContent_rejected_notObject() {
		var a = send(configReturning("text"), requestWithId(1));
		assertEquals(-32603, a.getError().getCode());
		assertContains("2025-06-18", a.getError().getMessage());
		assertContains("JSON object", a.getError().getMessage());
	}

	// -------- accepted cases: structuredContent within bounds ---------

	@Test
	void b01_normalObject_accepted() {
		var resp = send(configReturning(JsonMap.of("a", 1, "b", JsonMap.of("c", 2))), requestWithId(1));
		assertNull(resp.getError());
		assertInstanceOf(CallToolResult.class, resp.getResult());
	}

	@Test
	void b02_sharedDiamond_acyclic_terminatesCleanly() {
		// A diamond: two parent keys point at the same shared sub-map (no cycle back to an ancestor).
		// Shared references are pruned by identity, so this cannot inflate the node count or loop.
		var shared = JsonMap.of("s", 1);
		var root = JsonMap.of("left", shared, "right", shared);
		assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
			var resp = send(configReturning(root), requestWithId(1));
			assertNull(resp.getError());
			assertInstanceOf(CallToolResult.class, resp.getResult());
		});
	}
}
