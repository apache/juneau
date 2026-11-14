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


import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the admission checks applied to MCP POST requests.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpRequestPolicy_Test extends TestBase {

	private static final String PING = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json", allowedOrigins = "https://app.example.com")
	public static class Open extends McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("t").setVersion("1");
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Public extends McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("t").setVersion("1").allowUnauthenticated(true);
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Secured extends McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("t").setVersion("1").setTokenValidator(t -> {
				if (! "good".equals(t))
					throw new AuthenticationException("bad");
				return () -> "user";
			});
		}
	}

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).accept("application/json").build();
	}

	@Test void a01_wrongContentType_is415() throws Exception {
		client(Open.class).post("/", PING).contentType("text/plain").ignoreErrors().run().assertStatus(415);
	}

	@Test void a02_sameHostOrigin_allowed() throws Exception {
		client(Open.class).post("/", PING).contentType("application/json").header("Host", "localhost:8080").header("Origin", "http://localhost:8080").ignoreErrors().run().assertStatus(200);
	}

	@Test void a03_foreignOrigin_isRefused() throws Exception {
		client(Open.class).post("/", PING).contentType("application/json").header("Host", "localhost:8080").header("Origin", "http://other.example.com").ignoreErrors().run().assertStatus(403);
	}

	@Test void a04_originListedOnRest_allowed() throws Exception {
		client(Open.class).post("/", PING).contentType("application/json").header("Host", "localhost:8080").header("Origin", "https://app.example.com").ignoreErrors().run().assertStatus(200);
	}

	@Test void a05_noOrigin_allowed() throws Exception {
		client(Open.class).post("/", PING).contentType("application/json").ignoreErrors().run().assertStatus(200);
	}

	@Test void a06_unauthenticated_foreignHost_is403() throws Exception {
		client(Open.class).post("/", PING).contentType("application/json").header("Host", "rebound.example.com").ignoreErrors().run().assertStatus(403);
	}

	@Test void a07_allowUnauthenticated_servesAnyHost() throws Exception {
		client(Public.class).post("/", PING).contentType("application/json").header("Host", "rebound.example.com").ignoreErrors().run().assertStatus(200);
	}

	@Test void a08_tokenValidator_requiresBearer() throws Exception {
		client(Secured.class).post("/", PING).contentType("application/json").ignoreErrors().run().assertStatus(401);
		client(Secured.class).post("/", PING).contentType("application/json").header("Authorization", "Bearer bad").ignoreErrors().run().assertStatus(401);
		client(Secured.class).post("/", PING).contentType("application/json").header("Authorization", "Bearer good").ignoreErrors().run().assertStatus(200);
	}
}
