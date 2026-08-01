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

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Cross-module coverage proving resource-template reads and {@code completion/complete} dispatch on the
 * {@code 2026-07-28} adapter are consumable exactly as an external artifact consumer would use them, through
 * the published public API of {@code juneau-rest-server-mcp-v20260728} and real servlet HTTP dispatch.
 *
 * <p>
 * Full unit-level precedence/ranking/validation coverage lives in the module-local {@code McpResourceTemplate_Test}
 * and {@code McpCompletion_Test}; this class proves exact precedence, decoded variables, completion context
 * forwarding, empty-miss behavior, the auto-derived capability matrix, and explicit capability override all flow
 * through the real published servlet (with valid per-request {@code _meta} and {@code Mcp-Method}/{@code Mcp-Name}
 * headers), not module-internal test helpers.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class McpTemplateCompletionIntegration_Test {

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
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(withMeta(params)));
	}

	private static McpResourceHandler resource(String uri, String bodyText) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("res").setMimeType("text/plain"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) {
				return new McpResourceOutcome().setContents(List.of(McpResourceContents.text(u, "text/plain", bodyText)));
			}
		};
	}

	private static McpResourceTemplateHandler template(String uriTemplate) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return new McpResourceTemplateSpec().setUriTemplate(uriTemplate).setName("t").setMimeType("text/plain");
			}
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) {
				return new McpResourceOutcome().setContents(List.of(
					McpResourceContents.text(uri, "text/plain", "template:" + variables)));
			}
		};
	}

	private static McpPromptHandler promptWithContextCompleter() {
		var descriptor = new McpPromptSpec().setName("greet").setArguments(List.of(
			new McpPromptArgument().setName("style").setCompleter((request, ctx) -> {
				var greeting = request.getContextArguments().getOrDefault("greeting", "");
				return new McpCompletionResult().setValues(List.of(greeting + " Alice", greeting + " Bob"));
			})));
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return descriptor; }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
		};
	}

	private static McpPromptHandler promptWithCompleter() {
		var descriptor = new McpPromptSpec().setName("greet")
			.setArguments(List.of(new McpPromptArgument().setName("style").setCompleter((request, ctx) -> McpCompletionResult.empty())));
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return descriptor; }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
		};
	}

	private static McpResourceTemplateHandler templateWithCompleter() {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///{x}").setName("t"); }
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
			@Override public McpCompleter completer(String v) { return "x".equals(v) ? (request, ctx) -> McpCompletionResult.empty() : null; }
		};
	}

	private static ServerCapabilities explicit() {
		return new ServerCapabilities()
			.setLogging(new LoggingCapability().setLevel("debug"))
			.setResources(new ResourceCapability().setSubscribe(true));
	}

	// -------- exact precedence / decoded variables ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A_ExactBeatsTemplate extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addResource(resource("file:///a", "exact"))
				.addResourceTemplate(template("file:///{x}"));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A_TemplateOnly extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addResourceTemplate(template("file:///{name}"));
		}
	}

	@Test void a01_exactResourceBeatsMatchingTemplate_throughRealDispatch() throws Exception {
		var c = MockRestClient.create(A_ExactBeatsTemplate.class).json().contentType("application/json").accept("application/json").build();
		var resp = c.post("/").contentString(body(1, "resources/read", JsonMap.of("uri", "file:///a")))
			.header("Mcp-Method", "resources/read").header("Mcp-Name", "file:///a")
			.run().assertStatus(200).getContent().asString();
		assertTrue(resp.contains("\"text\":\"exact\""), resp);
	}

	@Test void a02_templateWinner_receivesDecodedVariables_throughRealDispatch() throws Exception {
		var c = MockRestClient.create(A_TemplateOnly.class).json().contentType("application/json").accept("application/json").build();
		var resp = c.post("/").contentString(body(1, "resources/read", JsonMap.of("uri", "file:///John%20Doe")))
			.header("Mcp-Method", "resources/read").header("Mcp-Name", "file:///John%20Doe")
			.run().assertStatus(200).getContent().asString();
		assertTrue(resp.contains("name=John Doe"), resp);
	}

	// -------- completion context forwarding / empty miss ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B_ContextCompleter extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addPrompt(promptWithContextCompleter());
		}
	}

	@Test void b01_completionContext_forwardedToCompleter_throughRealDispatch() throws Exception {
		var c = MockRestClient.create(B_ContextCompleter.class).json().contentType("application/json").accept("application/json").build();
		var params = JsonMap.of(
			"ref", JsonMap.of("type", "ref/prompt", "name", "greet"),
			"argument", JsonMap.of("name", "style", "value", ""),
			"context", JsonMap.of("arguments", JsonMap.of("greeting", "Hi")));
		var resp = c.post("/").contentString(body(1, "completion/complete", params))
			.header("Mcp-Method", "completion/complete").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertTrue(resp.contains("Hi Alice"), resp);
		assertTrue(resp.contains("Hi Bob"), resp);
	}

	@Test void b02_unknownReference_returnsSuccessfulEmptyCompletion_throughRealDispatch() throws Exception {
		var c = MockRestClient.create(B_ContextCompleter.class).json().contentType("application/json").accept("application/json").build();
		var params = JsonMap.of(
			"ref", JsonMap.of("type", "ref/prompt", "name", "ghost"),
			"argument", JsonMap.of("name", "style", "value", ""));
		var resp = c.post("/").contentString(body(1, "completion/complete", params))
			.header("Mcp-Method", "completion/complete").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertFalse(resp.contains("\"error\""), resp);
		assertTrue(resp.contains("\"values\":[]"), resp);
	}

	// -------- capability matrix / explicit override ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C_NoRegistrations extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig(); }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C_PromptCompleterOnly extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig().addPrompt(promptWithCompleter()); }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C_TemplateCompleterOnly extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig().addResourceTemplate(templateWithCompleter()); }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class C_OverrideWithCompleterRegistered extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig().addPrompt(promptWithCompleter()); }
		@Override protected ServerCapabilities capabilities() { return explicit(); }
	}

	private static String discover(Class<?> servlet) throws Exception {
		try (var c = MockRestClient.create(servlet).json().contentType("application/json").accept("application/json").build()) {
			return c.post("/").contentString(body(1, "server/discover", null))
				.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
				.run().assertStatus(200).getContent().asString();
		}
	}

	@Test void c01_capabilityMatrix_noRegistrations_neitherResourcesNorCompletions() throws Exception {
		var resp = discover(C_NoRegistrations.class);
		assertFalse(resp.contains("\"resources\""), resp);
		assertFalse(resp.contains("\"completions\""), resp);
	}

	@Test void c02_capabilityMatrix_promptCompleterOnly_derivesPromptsAndCompletions() throws Exception {
		var resp = discover(C_PromptCompleterOnly.class);
		assertTrue(resp.contains("\"prompts\":{}"), resp);
		assertTrue(resp.contains("\"completions\":{}"), resp);
		assertFalse(resp.contains("\"resources\""), resp);
	}

	@Test void c03_capabilityMatrix_templateCompleterOnly_derivesResourcesAndCompletions() throws Exception {
		var resp = discover(C_TemplateCompleterOnly.class);
		assertTrue(resp.contains("\"resources\":{}"), resp);
		assertTrue(resp.contains("\"completions\":{}"), resp);
		assertFalse(resp.contains("\"prompts\""), resp);
	}

	@Test void c04_explicitCapabilitiesOverride_notMergedWithAutoDerivedCompletions() throws Exception {
		var resp = discover(C_OverrideWithCompleterRegistered.class);
		assertTrue(resp.contains("\"level\":\"debug\""), resp);
		assertFalse(resp.contains("\"completions\""), "an explicit override must not merge auto-derived completions: " + resp);
	}
}
