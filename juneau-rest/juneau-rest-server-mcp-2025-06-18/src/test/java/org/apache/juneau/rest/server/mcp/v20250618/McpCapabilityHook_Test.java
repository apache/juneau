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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the explicit-capabilities hook on both integration paths.
 *
 * <p>
 * The hook replaces the pre-re-layering {@code McpServerConfig.setCapabilities(...)} setter. Both of
 * its states are real, distinct code paths: a {@code null} return auto-derives from the handler
 * registry (unchanged behavior), and a non-{@code null} return is advertised as-is.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class McpCapabilityHook_Test extends TestBase {

	private static McpToolHandler tool() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("t"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return new McpToolOutcome(); }
		};
	}

	private static ServerCapabilities explicit() {
		return new ServerCapabilities()
			.setLogging(new LoggingCapability().setLevel("debug"))
			.setResources(new ResourceCapability().setSubscribe(true))
			.setExperimental(JsonMap.of("x", 1));
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A_ServletDefault extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig().addTool(tool()); }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A_ServletOverride extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig().addTool(tool()); }
		@Override protected ServerCapabilities capabilities() { return explicit(); }
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	@org.apache.juneau.marshall.serializer.SerializerConfig(addBeanTypes = "true")
	public static class B_MixinDefault extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig().addTool(tool()); }
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	@org.apache.juneau.marshall.serializer.SerializerConfig(addBeanTypes = "true")
	public static class B_MixinOverride extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override public McpServerConfig getMcpConfig() { return new McpServerConfig().addTool(tool()); }
		// public here is required by the JLS (an interface method, default or not, is always public;
		// an overriding class member cannot narrow that) — it is not a departure from C8's "no public
		// hook" ruling, which is about McpRevision never having cross-package access to the hook.
		@Override public ServerCapabilities capabilities() { return explicit(); }
	}

	@Test
	void a01_servlet_defaultHook_autoDerives() throws Exception {
		var c = MockRestClient.create(A_ServletDefault.class).json().contentType("application/json").accept("application/json").build();
		var body = c.post("/").contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"tools\":{}"), body);
		assertFalse(body.contains("logging"), body);
		assertFalse(body.contains("subscribe"), body);
	}

	@Test
	void a02_servlet_overriddenHook_bypassesAutoDerivation() throws Exception {
		var c = MockRestClient.create(A_ServletOverride.class).json().contentType("application/json").accept("application/json").build();
		var body = c.post("/").contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"level\":\"debug\""), body);
		assertTrue(body.contains("\"subscribe\":true"), body);
		assertTrue(body.contains("\"x\":1"), body);
		assertFalse(body.contains("\"tools\":{}"), "an explicit override must bypass auto-derivation entirely: " + body);
	}

	@Test
	void b01_mixin_defaultHook_autoDerives() throws Exception {
		var c = MockRestClient.create(B_MixinDefault.class).json().contentType("application/json").accept("application/json").build();
		var body = c.post("/mcp").contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"tools\":{}"), body);
		assertFalse(body.contains("logging"), body);
	}

	@Test
	void b02_mixin_overriddenHook_bypassesAutoDerivation() throws Exception {
		var c = MockRestClient.create(B_MixinOverride.class).json().contentType("application/json").accept("application/json").build();
		var body = c.post("/mcp").contentString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"level\":\"debug\""), body);
		assertTrue(body.contains("\"subscribe\":true"), body);
		assertFalse(body.contains("\"tools\":{}"), body);
	}

	@Test
	void c01_defaultHookReturnsNullOnBothPaths() {
		assertNull(new A_ServletDefault().capabilities());
		assertNull(new B_MixinDefault().capabilities());
	}
}
