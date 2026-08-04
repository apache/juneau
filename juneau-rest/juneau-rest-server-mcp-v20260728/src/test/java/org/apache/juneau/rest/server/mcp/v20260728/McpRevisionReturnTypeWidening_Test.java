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

import org.apache.juneau.bean.jsonrpc.JsonRpcRequest;
import org.apache.juneau.bean.mcp.v20260728.RequestMeta;
import org.apache.juneau.commons.inject.BeanStore;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.marshall.json.JsonParser;
import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.rest.mock.classic.MockRestClient;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet;
import org.apache.juneau.rest.server.mcp.McpEndpointMixin;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.Test;

/**
 * Confirms the C8 return-type widening ({@code JsonRpcResponse} to {@code Object}) landed on all four
 * pinned signatures, and that it does not disturb serialization of a normal (non-streaming) dispatch.
 */
@SuppressWarnings({"resource"})
class McpRevisionReturnTypeWidening_Test {

	@Test
	void neutralInterfaceDispatchReturnsObject() throws NoSuchMethodException {
		var m = org.apache.juneau.rest.server.mcp.McpRevision.class
			.getMethod("dispatch", McpExchange.class, McpServerConfig.class, BeanStore.class);
		assertEquals(Object.class, m.getReturnType());
	}

	@Test
	void v2RevisionDispatchReturnsObject() throws NoSuchMethodException {
		var m = McpRevision.class.getMethod("dispatch", McpExchange.class, McpServerConfig.class, BeanStore.class);
		assertEquals(Object.class, m.getReturnType());
	}

	@Test
	void handleMcpReturnsObject() throws NoSuchMethodException {
		var m = AbstractMcpRestServlet.class.getMethod("handleMcp", JsonRpcRequest.class, RestRequest.class);
		assertEquals(Object.class, m.getReturnType());
	}

	@Test
	void handleMcpRequestReturnsObject() throws NoSuchMethodException {
		var m = McpEndpointMixin.class.getMethod("handleMcpRequest", JsonRpcRequest.class, RestRequest.class);
		assertEquals(Object.class, m.getReturnType());
	}

	// -------- regression: a normal method's JSON is unaffected by the widening --------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Fixture extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("widening-fixture").setVersion("1.0.0").addTool(echo());
		}
		private static McpToolHandler echo() {
			return new McpToolHandler() {
				@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo"); }
				@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text(String.valueOf(arguments.get("text"))); }
			};
		}
	}

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static String body(Object id, String method, Object params) {
		var p = params instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest()
			.setJsonrpc(org.apache.juneau.bean.mcp.v20260728.McpProtocol.JSON_RPC_2_0)
			.setId(id).setMethod(method).setParams(p));
	}

	@Test
	void toolsCallJsonUnchangedAfterWidening() throws Exception {
		var client = MockRestClient.create(Fixture.class).json().contentType("application/json").accept("application/json").build();
		var params = JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hello"));
		var resp = client.post("/").contentString(body(1, "tools/call", params))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertTrue(resp.contains("hello"), resp);
		assertFalse(resp.contains("\"error\""), resp);
	}
}
