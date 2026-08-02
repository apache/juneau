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
package org.apache.juneau.rest.client.mcp.v20250618;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-client-real-server end-to-end coverage for {@link McpClient}, driving an inline v1
 * {@link org.apache.juneau.rest.server.mcp.v20250618.McpRestServlet} fixture (mirroring {@code McpRestServlet_Test}'s
 * inline-fixture-per-test-class pattern) over a genuine embedded Jetty server instead of the classic in-process
 * mock, catching wire-format mismatches a stubbed-transport unit test cannot.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class McpClient_JettyMicroservice_Test extends TestBase {

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Fixture extends org.apache.juneau.rest.server.mcp.v20250618.McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.setName("it-fixture").setVersion("1.0.0")
				.addTool(new McpToolHandler() {
					@Override
					public McpToolSpec descriptor() {
						return new McpToolSpec().setName("echo").setDescription("Echoes back");
					}

					@Override
					public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
						return McpToolOutcome.text(String.valueOf(arguments.get("text")));
					}
				});
		}
	}

	@Configuration
	public static class FixtureConfig {
		@Bean public Servlet mcpServlet() { return new Fixture(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(FixtureConfig.class);

	private static McpClient.Builder clientBuilder() {
		return McpClient.builder().endpoint(fixture.getRootUrl() + "/");
	}

	@Test void a01_initialize_returnsServerInfoOverRealHttp() throws Exception {
		try (var client = clientBuilder().build()) {
			var result = client.initialize();
			assertEquals("it-fixture", result.getServerInfo().getName());
			assertEquals("1.0.0", result.getServerInfo().getVersion());
		}
	}

	@Test void a02_ping_succeedsOverRealHttp() throws Exception {
		try (var client = clientBuilder().build()) {
			assertDoesNotThrow(client::ping);
		}
	}

	@Test void b01_listTools_returnsRegisteredToolOverRealHttp() throws Exception {
		try (var client = clientBuilder().build()) {
			var result = client.listTools();
			assertEquals(1, result.getTools().size());
			assertEquals("echo", result.getTools().get(0).getName());
		}
	}

	@Test void b02_callTool_invokesHandlerOverRealHttp() throws Exception {
		try (var client = clientBuilder().build()) {
			var result = client.callTool("echo", Map.of("text", "hello"));
			assertEquals(1, result.getContent().size());
			assertEquals("hello", ((org.apache.juneau.bean.mcp.v20250618.TextContent) result.getContent().get(0)).getText());
		}
	}

	@Test void c01_callTool_unknownTool_throwsMcpExceptionOverRealHttp() throws Exception {
		try (var client = clientBuilder().build()) {
			var e = assertThrows(McpException.class, () -> client.callTool("no-such-tool", Map.of()));
			assertEquals(-32601, e.getCode());
			assertEquals("Tool not found: no-such-tool", e.getMessage());
		}
	}
}
