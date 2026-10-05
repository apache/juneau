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

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

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
import org.apache.juneau.rest.server.mcp.McpTypedHandlers;
import org.apache.juneau.rest.server.mcp.McpTypedToolHandler;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Byte-parity coverage proving the {@code 2026-07-28} {@link McpRestServlet} (servlet, {@code POST /}) and
 * {@link McpEndpoint} (mixin, {@code POST /mcp}) apply identical serializer policy: {@code addBeanTypes="true"}
 * and {@code uriResolution="NONE"}.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpSerializerParity_Test extends TestBase {

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

	//-----------------------------------------------------------------------------------------------------------------
	// Neutral typed tool, registered identically on both fixtures
	//-----------------------------------------------------------------------------------------------------------------

	public static class Nested {
		public String value;
		public Nested() {}
		public Nested(String v) { this.value = v; }
	}

	public static class Result {
		public URI uri = URI.create("https://example.com/a#fragment");
		public String ref = "#/$defs/X";
		public Nested nested = new Nested("value");
	}

	/**
	 * Typed tool adapted then re-exposed <em>without</em> an input schema.
	 *
	 * <p>
	 * {@link Result} still drives output-schema {@code $defs}/{@code $ref} and the structured-content
	 * URI/fragment fields under test. Input schema is deliberately omitted: {@code tools/call} runs
	 * {@code McpSchemaSafety.validateInput}, which shares a 100&nbsp;ms wall-clock
	 * {@link org.apache.juneau.commons.utils.JsonValueSafety} deadline across the schema and argument walks.
	 * Under CI load ({@code forkCount=8}) one of the two sequential servlet/mixin calls can trip
	 * {@code Tool input schema traversal exceeded 100 ms} while the other succeeds, breaking byte-identity.
	 * A null input schema makes {@code validateInput} a no-op so both sides stay on the success path;
	 * serializer policy under test does not depend on input-schema validation.
	 *
	 * <p>
	 * Argument type remains {@code Map.class} (not a bean {@code Args}) so the typed adapter's schema
	 * derivation never materializes a {@link URI} leaf that {@code JsonValueSafety} would reject as a
	 * non-JSON value — a pre-existing, out-of-scope defect for bean-shaped tool arguments.
	 */
	private static McpToolHandler xTool() {
		var adapted = McpTypedHandlers.adaptTool(new McpTypedToolHandler<Map<String,Object>,Result>() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("x"); }
			@Override public Type argumentType() { return Map.class; }
			@Override public Type resultType() { return Result.class; }
			@Override public Result call(Map<String,Object> arguments, BeanStore ctx) { return new Result(); }
		});
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec()
					.setName(adapted.descriptor().getName())
					.setOutputSchema(adapted.descriptor().getOutputSchema());
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				return adapted.call(arguments, ctx);
			}
		};
	}

	// -------- servlet path (POST /) ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Servlet extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(xTool());
		}
	}

	private MockRestClient clientServlet() {
		return MockRestClient.create(Servlet.class).json().contentType("application/json").accept("application/json").build();
	}

	// -------- endpoint mixin path (POST /mcp) ---------

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Mixin extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override
		public McpServerConfig getMcpConfig() {
			// Same server identity as Servlet's config: this fixture proves serializer-policy parity, not
			// server-identity parity, and every success result now carries its own server identity per
			// common result finalization.
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(xTool());
		}
	}

	private MockRestClient clientMixin() {
		return MockRestClient.create(Mixin.class).json().contentType("application/json").accept("application/json").build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parity tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_toolsList_servletAndMixinAreByteIdentical() throws Exception {
		var servletBody = clientServlet().post("/").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		var mixinBody = clientMixin().post("/mcp").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertEquals(servletBody, mixinBody);
	}

	@Test void a02_toolsCall_servletAndMixinAreByteIdentical() throws Exception {
		var params = JsonMap.of("name", "x", "arguments", JsonMap.of());
		var servletBody = clientServlet().post("/").contentString(body(1, "tools/call", params))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "x")
			.run().assertStatus(200).getContent().asString();
		var mixinBody = clientMixin().post("/mcp").contentString(body(1, "tools/call", params))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "x")
			.run().assertStatus(200).getContent().asString();
		assertEquals(servletBody, mixinBody);
		assertContains("\"type\":\"text\"", servletBody);
		assertFalse(servletBody.contains("_type"));
		assertContains("#/$defs/X", servletBody);
		assertContains("https://example.com/a#fragment", servletBody);
	}
}
