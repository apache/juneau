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
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
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
 * End-to-end HTTP coverage for the {@code 2026-07-28} {@link McpRestServlet} and {@link McpEndpoint}
 * bindings and their typed {@code server/discover} capability hooks.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpBindings_Test extends TestBase {

	private static Object validMeta() {
		return JsonMap.of(
			"protocolVersion", "2026-07-28",
			"clientInfo", JsonMap.of("name", "fixture-client", "version", "1.0"),
			"capabilities", JsonMap.of());
	}

	private static String body(Object id, String method, Object params) {
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params).setMeta(validMeta()));
	}

	private static McpToolHandler echo() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo").setDescription("Echoes back"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text(String.valueOf(arguments.get("text"))); }
		};
	}

	// -------- servlet path (POST /) ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
	}

	private MockRestClient clientA() {
		return MockRestClient.create(A.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void a01_servlet_serverDiscover_autoDerivesToolCapability() throws Exception {
		var resp = clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"serverInfo\"", resp);
		assertContains("\"test\"", resp);
		assertContains("\"tools\"", resp);
	}

	@Test void a02_servlet_toolsCall_dispatches() throws Exception {
		var params = JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hello"));
		var resp = clientA().post("/").contentString(body(1, "tools/call", params))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("hello", resp);
	}

	// -------- servlet with explicit capability override ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class D extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("override").setVersion("1.0.0").addTool(echo());
		}
		@Override
		protected ServerCapabilities capabilities() {
			return new ServerCapabilities().setPrompts(new PromptCapability());
		}
	}

	@Test void a03_servlet_overrideCapabilities_advertisedAsIs() throws Exception {
		var c = MockRestClient.create(D.class).json().contentType("application/json").accept("application/json").build();
		var resp = c.post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"prompts\"", resp);
		assertFalse(resp.contains("\"tools\""), resp);  // a registered tool does NOT leak past an explicit override
	}

	// -------- endpoint mixin path (POST /mcp) ---------

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(new McpToolHandler() {
				@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("ping"); }
				@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text("pong"); }
			});
		}
	}

	private MockRestClient clientB() {
		return MockRestClient.create(B.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void b01_endpointMixin_serverDiscover_dispatches() throws Exception {
		var resp = clientB().post("/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"serverInfo\"", resp);
		assertContains("\"tools\"", resp);
	}

	@Test void b02_endpointMixin_toolsCall_dispatches() throws Exception {
		var resp = clientB().post("/mcp").contentString(body(1, "tools/call", JsonMap.of("name", "ping")))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ping")
			.run().assertStatus(200).getContent().asString();
		assertContains("pong", resp);
	}

	// -------- default null capability hooks ---------

	@Test void c01_servletCapabilityHook_defaultsToNull() {
		assertNull(new A().capabilities());
	}

	@Test void c02_endpointCapabilityHook_defaultsToNull() {
		assertNull(new B().capabilities());
	}

	// -------- cache-config lifecycle hooks ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class E extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		static final AtomicInteger calls = new AtomicInteger();
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override
		protected McpCacheConfig createCacheConfig() {
			calls.incrementAndGet();
			return new McpCacheConfig().setToolsList(new McpCacheHint().setTtlMs(21));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig();
		}
		@Override
		protected McpCacheConfig createCacheConfig() {
			return null;
		}
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class G extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override
		public McpCacheConfig cacheConfig() {
			return new McpCacheConfig().setToolsList(new McpCacheHint().setCacheScope(McpCacheScope.PRIVATE));
		}
	}

	private static MockRestClient client(Class<?> c, String basePath) {
		return MockRestClient.create(c).json().contentType("application/json").accept("application/json").build();
	}

	private MockRestClient clientBWithCache() {
		return MockRestClient.create(G.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void d01_servletCacheConfig_isLazilyCachedAndInjected() throws Exception {
		var servlet = new E();
		assertSame(servlet.getCacheConfig(), servlet.getCacheConfig());
		assertEquals(1, E.calls.get());
		var body = client(E.class, "/").post("/").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "").run().getContent().asString();
		assertContains("\"ttlMs\":21", body);
	}

	@Test void d02_servletNullFactoryFailsFast() {
		var e = assertThrows(IllegalStateException.class, () -> new F().getCacheConfig());
		assertEquals("createCacheConfig() returned null", e.getMessage());
	}

	@Test void d03_endpointDefaultIsEmptyAndOverrideIsInjected() throws Exception {
		assertNotNull(new B().cacheConfig());
		var body = clientBWithCache().post("/mcp").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "").run().getContent().asString();
		assertContains("\"cacheScope\":\"private\"", body);
	}
}
