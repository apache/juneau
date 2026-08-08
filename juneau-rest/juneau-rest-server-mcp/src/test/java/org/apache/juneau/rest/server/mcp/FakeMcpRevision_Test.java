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
package org.apache.juneau.rest.server.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * The architecture acceptance test for the revision-neutral core.
 *
 * <p>
 * A revision implementation written entirely from scratch, inside the core module's own test tree,
 * with no access to any revision-specific module, must be able to serve a full MCP request through
 * both integration paths. If this ever stops compiling or stops passing, some revision-specific
 * assumption has leaked into the core — which is precisely the regression this test exists to catch.
 * Keep it permanently.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class FakeMcpRevision_Test extends TestBase {

	/** Records what it received and returns a canned response. */
	static class FakeRevision implements McpRevision {
		McpExchange lastExchange;
		McpServerConfig lastConfig;
		BeanStore lastCtx;
		int calls;

		@Override
		public String protocolVersion() {
			return "0000-00-00";
		}

		@Override
		public McpDispatchResult dispatch(McpExchange exchange, McpServerConfig config, BeanStore ctx) {
			calls++;
			lastExchange = exchange;
			lastConfig = config;
			lastCtx = ctx;
			return new McpResponseResult(JsonRpcResponse.ok(exchange.request().getId(), Map.of("fake", protocolVersion())));
		}

		@Override
		public int errorCode(McpErrorKind kind) {
			return -1;
		}
	}

	static final FakeRevision SERVLET_REVISION = new FakeRevision();
	static final FakeRevision MIXIN_REVISION = new FakeRevision();

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends AbstractMcpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig().setInstructions("fake"); }
		@Override protected McpRevision revision() { return SERVLET_REVISION; }
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B extends BasicRestServlet implements McpEndpointMixin {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig().setInstructions("fake-mixin"); }
		@Override public McpRevision revision() { return MIXIN_REVISION; }
	}

	@Test
	void a01_servletPath_reachesTheFakeRevision() throws Exception {
		var c = MockRestClient.create(A.class).json().contentType("application/json").accept("application/json").build();
		var res = c.post("/").contentString("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"anything\"}").run()
			.assertStatus(200).getContent().asString();
		assertTrue(res.contains("0000-00-00"), res);
		assertEquals(1, SERVLET_REVISION.calls);
		assertEquals("anything", SERVLET_REVISION.lastExchange.request().getMethod());
		assertEquals("fake", SERVLET_REVISION.lastConfig.getInstructions());
		assertTrue(SERVLET_REVISION.lastCtx.getBean(RestRequest.class).isPresent());
	}

	@Test
	void a02_mixinPath_reachesTheFakeRevision() throws Exception {
		var c = MockRestClient.create(B.class).json().contentType("application/json").accept("application/json").build();
		var res = c.post("/mcp").contentString("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"anything\"}").run()
			.assertStatus(200).getContent().asString();
		assertTrue(res.contains("0000-00-00"), res);
		assertEquals(1, MIXIN_REVISION.calls);
		assertEquals("fake-mixin", MIXIN_REVISION.lastConfig.getInstructions());
	}

	@Test
	void a03_headerAccessorReachesTheHttpHeaders() throws Exception {
		var c = MockRestClient.create(A.class).json().contentType("application/json").accept("application/json").build();
		c.post("/").contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"anything\"}").header("Mcp-Method", "tools/call").run().assertStatus(200);
		assertEquals("tools/call", SERVLET_REVISION.lastExchange.header("Mcp-Method"));
		assertNull(SERVLET_REVISION.lastExchange.header("Absent-Header"));
	}

	@Test
	void a04_coreTestTreeSeesNoRevisionModule() {
		assertThrows(ClassNotFoundException.class,
			() -> Class.forName("org.apache.juneau.rest.server.mcp.v20250618.McpRevision"),
			"the core module's test classpath must not contain any revision adapter");
		assertThrows(ClassNotFoundException.class,
			() -> Class.forName("org.apache.juneau.bean.mcp.v20250618.Tool"),
			"the core module's test classpath must not contain any revision's wire beans");
	}
}
