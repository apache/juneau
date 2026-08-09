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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.client.mcp.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-client-real-server end-to-end coverage for {@link McpClient}, driving an inline v2
 * {@link org.apache.juneau.rest.server.mcp.v20260728.McpRestServlet} fixture over a genuine embedded
 * Jetty server to catch wire-format mismatches that transport-double tests cannot.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class McpClientV20260728_Integration_Test extends TestBase {

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Fixture extends org.apache.juneau.rest.server.mcp.v20260728.McpRestServlet {
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
		return McpClient.builder()
			.endpoint(fixture.getRootUrl() + "/")
			.clientCapabilities(new ClientCapabilities())
			.responseCache(new InMemoryMcpResponseCache());
	}

	@Test void a01_serverDiscover_and_callTool_roundTrip() throws Exception {
		try (var client = clientBuilder().build()) {
			var discover = client.serverDiscover();
			assertNotNull(discover);
			assertEquals("2026-07-28", discover.getSupportedVersions().get(0));

			var call = client.callTool("echo", Map.of("text", "hello"));
			assertNotNull(call);
			assertNotEquals(Boolean.TRUE, call.getIsError());
			assertEquals(1, call.getContent().size());
			assertEquals("hello", ((TextContent)call.getContent().get(0)).getText());
		}
	}
}
