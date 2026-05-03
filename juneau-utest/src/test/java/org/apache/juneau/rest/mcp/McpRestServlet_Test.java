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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end coverage for {@link McpRestServlet} via {@link MockRestClient}.
 */
class McpRestServlet_Test extends TestBase {

	private static final JsonParser PAR = JsonParser.create()
		.typePropertyName(Content.class, "type")
		.typePropertyName(ResourceContents.class, "type")
		.build();

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.setServerInfo(new Implementation().setName("test").setVersion("1.0.0"))
				.addTool(new McpToolHandler() {
					@Override
					public Tool descriptor() {
						return new Tool().setName("echo").setDescription("Echoes back");
					}

					@Override
					public CallToolResult call(Map<String, Object> arguments, BasicBeanStore ctx) {
						var ctr = new CallToolResult();
						ctr.setContent(List.of(new TextContent().setText(String.valueOf(arguments.get("text")))));
						return ctr;
					}
				});
		}
	}

	private MockRestClient client() {
		return MockRestClient.create(A.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void initialize_returnsServerInfo() throws Exception {
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.INITIALIZE);
		var resp = client().post("/", req).run().assertStatus(200).getContent().asString();
		var parsed = PAR.parse(resp, JsonRpcResponse.class);
		assertNotNull(parsed.getResult());
	}

	@Test void toolsList_returnsRegisteredTool() throws Exception {
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.TOOLS_LIST);
		var resp = client().post("/", req).run().assertStatus(200).getContent().asString();
		assertContains("echo", resp);
	}

	@Test void toolsCall_invokesHandler() throws Exception {
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(org.apache.juneau.collections.JsonMap.of("name", "echo", "arguments", org.apache.juneau.collections.JsonMap.of("text", "hello")));
		var resp = client().post("/", req).run().assertStatus(200).getContent().asString();
		assertContains("hello", resp);
		assertContains("\"type\":\"text\"", resp);
	}

	@Test void unknown_methodReturnsErrorEnvelope() throws Exception {
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod("no/such/method");
		var resp = client().post("/", req).run().assertStatus(200).getContent().asString();
		assertContains("error", resp);
		assertContains("-32601", resp);
	}

	// -------- McpEndpoint mixin --------

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	@org.apache.juneau.serializer.annotation.SerializerConfig(addBeanTypes = "true")
	public static class B extends org.apache.juneau.rest.servlet.BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;

		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(new McpToolHandler() {
				@Override
				public Tool descriptor() { return new Tool().setName("ping"); }

				@Override
				public CallToolResult call(Map<String, Object> arguments, BasicBeanStore ctx) {
					return new CallToolResult().setContent(List.of(new TextContent().setText("pong")));
				}
			});
		}
	}

	@Test void endpointMixin_dispatches() throws Exception {
		var c = MockRestClient.create(B.class).json().contentType("application/json").accept("application/json").build();
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(org.apache.juneau.collections.JsonMap.of("name", "ping"));
		var resp = c.post("/mcp", req).run().assertStatus(200).getContent().asString();
		assertContains("pong", resp);
	}

	// -------- failure modes --------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return null;
		}
	}

	@Test void servlet_getMcpConfig_cachesValue() {
		var s = new A();
		var c1 = s.getMcpConfig();
		var c2 = s.getMcpConfig();
		assertSame(c1, c2);
	}

	@Test void servlet_getMcpConfig_nullThrows() {
		var s = new C();
		assertThrows(IllegalStateException.class, s::getMcpConfig);
	}

	@Test void servlet_nullConfigCausesFailure() throws Exception {
		var c = MockRestClient.create(C.class).json().contentType("application/json").accept("application/json").ignoreErrors().build();
		var req = new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1).setMethod(McpMethods.INITIALIZE);
		c.post("/", req).run().assertStatus(500);
	}
}
