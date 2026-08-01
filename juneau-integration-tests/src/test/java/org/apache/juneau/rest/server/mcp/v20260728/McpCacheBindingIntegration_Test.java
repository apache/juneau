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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Cross-module coverage proving the servlet-subclass and endpoint-mixin cache-config lifecycle hooks
 * are consumable exactly as an external artifact consumer would use them, through the published
 * public API of {@code juneau-rest-server-mcp-v20260728}.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpCacheBindingIntegration_Test {

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static Object withMeta(Object baseParams) {
		var p = baseParams instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return p;
	}

	private static String body(Object id, String method, Object params) {
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(withMeta(params)));
	}

	private static McpToolHandler echo() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text("ok"); }
		};
	}

	// -------- servlet-subclass path ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class ServletWithCache extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override protected McpCacheConfig createCacheConfig() {
			return new McpCacheConfig().setToolsList(
				new McpCacheHint().setTtlMs(77).setCacheScope(McpCacheScope.PRIVATE));
		}
	}

	// -------- endpoint-mixin path ---------

	@Rest(path = "/mcp", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class EndpointWithCache extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override public McpCacheConfig cacheConfig() {
			return new McpCacheConfig().setToolsList(
				new McpCacheHint().setTtlMs(77).setCacheScope(McpCacheScope.PRIVATE));
		}
	}

	@Test void a01_servletAndEndpointCacheHooks_produceByteIdenticalTopLevelHints() throws Exception {
		var servletResp = MockRestClient.create(ServletWithCache.class).json()
			.contentType("application/json").accept("application/json").build()
			.post("/").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		var endpointResp = MockRestClient.create(EndpointWithCache.class).json()
			.contentType("application/json").accept("application/json").build()
			.post("/mcp").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertEquals(servletResp, endpointResp);
		assertContains("\"ttlMs\":77", servletResp);
		assertContains("\"cacheScope\":\"private\"", servletResp);
	}
}
