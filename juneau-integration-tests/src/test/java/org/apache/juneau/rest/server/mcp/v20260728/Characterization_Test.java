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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Wire-compatibility characterization fixtures for the {@code 2026-07-28} MCP JSON-RPC endpoint.
 *
 * <p>
 * These fixtures encode <em>current</em> behavior. A fixture body must never be edited to
 * accommodate a code change: if replay fails, the code change is wrong.
 *
 * <p>
 * Regenerate the {@code *.response.json} files (only ever against known-good code) with:
 * <p>
 * {@code mvn test -Drat.skip=true -pl juneau-rest/juneau-rest-server-mcp-v20260728 -Dtest=Characterization_Test -Djuneau.mcp.characterization.write=true}
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class Characterization_Test {

	private static final Path DIR = Paths.get("src/test/resources/mcp/v20260728/characterization");
	private static final boolean WRITE = Boolean.getBoolean("juneau.mcp.characterization.write");

	// --- fixture servlets -------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Empty extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig(); }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Full extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addTool(tool("echo", McpSchema.of(JsonMap.of("type", "object", "required", List.of("text"))), a -> McpToolOutcome.text(String.valueOf(a.get("text")))))
				.addPrompt(prompt("greet", a -> new McpPromptOutcome().setDescription("d").setMessages(List.of(
					new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text("hi " + a.get("who")))))))
				.addResource(resource("file:///a", u -> new McpResourceOutcome().setContents(List.of(
					McpResourceContents.text(u, "text/plain", "body")))))
				.addResourceTemplate(new McpResourceTemplateSpec()
					.setUriTemplate("file:///{name}")
					.setName("templated")
					.setTitle("Template title")
					.setDescription("template description")
					.setMimeType("text/plain"));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Cache extends F_Full {
		private static final long serialVersionUID = 1L;

		@Override protected McpCacheConfig createCacheConfig() {
			return new McpCacheConfig()
				.setDefaultHint(new McpCacheHint().setTtlMs(30000))
				.setToolsList(new McpCacheHint().setTtlMs(5000).setCacheScope(McpCacheScope.PRIVATE))
				.setPromptsList(new McpCacheHint().setTtlMs(0).setCacheScope(McpCacheScope.PUBLIC))
				.setResourceTemplatesList(new McpCacheHint().setTtlMs(60000).setCacheScope(McpCacheScope.PRIVATE))
				.setResourcesRead(new McpCacheHint().setTtlMs(2000))
				.addResourceReadOverride("file:///a",
					new McpCacheHint().setTtlMs(1000).setCacheScope(McpCacheScope.PRIVATE));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Throw extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addTool(tool("echo", null, a -> { throw new RuntimeException("handler failed"); }));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Schema extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			var schema = McpSchema.of(JsonMap.of(
				"type", "object",
				"required", List.of("text"),
				"properties", JsonMap.of("text", JsonMap.of("$ref", "#/$defs/text")),
				"$defs", JsonMap.of("text", JsonMap.of("type", "string")),
				"$id", "https://example.com/schemas/echo-input",
				"$schema", "https://json-schema.org/draft/2020-12/schema",
				"$comment", "input schema",
				"allOf", List.of(JsonMap.of("type", "object")),
				"oneOf", List.of(JsonMap.of("required", List.of("text"))),
				"if", JsonMap.of("properties", JsonMap.of("text", JsonMap.of("type", "string"))),
				"else", JsonMap.of()));
			return new McpServerConfig().addTool(tool("echo", schema, a -> new McpToolOutcome()));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Structured extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(new McpTypedToolHandler<JsonMap,Object>() {
				@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo"); }
				@Override public java.lang.reflect.Type argumentType() { return JsonMap.class; }
				@Override public java.lang.reflect.Type resultType() { return Object.class; }
				@Override public Object call(JsonMap arguments, BeanStore ctx) { return arguments.get("value"); }
			});
		}
	}

	/**
	 * Backs every {@code TEMPLATE-*} fixture: one exact resource that always outranks any template match,
	 * plus resource-template registrations (in listing order) exercising decoded-scalar capture,
	 * reserved/unencoded capture, a listing-only non-winning two-variable template, and literal-prefix
	 * specificity. {@code TEMPLATE-read-unknown} deliberately reuses this template-registered servlet
	 * (rather than {@link F_Empty}) so its "no match" outcome proves a real miss against a populated
	 * registry, not a trivially empty one. No cache config is set here, matching every other
	 * cache-config-free fixture servlet.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Template extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addResource(resource("file:///a", u -> new McpResourceOutcome().setContents(List.of(
					McpResourceContents.text(u, "text/plain", "exact-a")))))
				.addResourceTemplate(
					template("file:///{name}", "simple", "Simple template", "Captures one decoded path segment",
						(uri, vars) -> new McpResourceOutcome().setContents(List.of(
							McpResourceContents.text(uri, "text/plain", "name=" + vars.get("name"))))),
					template("file:///r/{+name}", "reserved", "Reserved template", "Captures unencoded path segments",
						(uri, vars) -> new McpResourceOutcome().setContents(List.of(
							McpResourceContents.text(uri, "text/plain", "name=" + vars.get("name"))))))
				.addResourceTemplate(new McpResourceTemplateSpec()
					.setUriTemplate("file:///{a}/{b}").setName("twovar").setTitle("Two variable template")
					.setDescription("Two single-segment variables").setMimeType("text/plain"))
				.addResourceTemplate(
					template("file:///fixed/{name}", "fixed", "Fixed-prefix template", "Fixed literal prefix with one variable",
						(uri, vars) -> new McpResourceOutcome().setContents(List.of(
							McpResourceContents.text(uri, "text/plain", "fixed name=" + vars.get("name"))))));
		}
	}

	/**
	 * Backs every {@code COMPLETE-*} fixture: a prompt whose two declared arguments each carry a
	 * deterministic completer (one ignoring context, one consuming it), and a resource-template variable
	 * completer whose current-value branches deterministically reproduce the small and the 101-value/capped
	 * cases from one registration.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Complete extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			var greet = new McpPromptSpec().setName("greet").setDescription("pd")
				.setArguments(List.of(
					new McpPromptArgument().setName("who")
						.setCompleter((request, ctx) -> new McpCompletionResult().setValues(List.of("Bob", "Alice", "Bob"))),
					new McpPromptArgument().setName("style")
						.setCompleter((request, ctx) -> {
							var greeting = request.getContextArguments().getOrDefault("greeting", "");
							return new McpCompletionResult().setValues(List.of(greeting + " Alice", greeting + " Bob"));
						})));
			return new McpServerConfig()
				.addPrompt(new McpPromptHandler() {
					@Override public McpPromptSpec descriptor() { return greet; }
					@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
				})
				.addResourceTemplate(new McpResourceTemplateHandler() {
					@Override public McpResourceTemplateSpec descriptor() {
						return new McpResourceTemplateSpec().setUriTemplate("file:///{name}").setName("simple").setMimeType("text/plain");
					}
					@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
					@Override public McpCompleter completer(String variableName) {
						if (! "name".equals(variableName))
							return null;
						return (request, ctx) -> {
							if ("cap".equals(request.getValue())) {
								var values = new ArrayList<String>();
								for (var i = 0; i < 101; i++)
									values.add("item" + i);
								return new McpCompletionResult().setValues(values).setTotal(101);
							}
							return new McpCompletionResult().setValues(List.of("alpha", "beta"));
						};
					}
				});
		}
	}

	// --- fixture handler factories ---------------------------------------------------------

	private static McpToolHandler tool(String name, McpSchema schema, Function<Map<String,Object>,McpToolOutcome> fn) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name).setDescription("desc:" + name).setInputSchema(schema); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpPromptHandler prompt(String name, Function<Map<String,Object>,McpPromptOutcome> fn) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name).setDescription("pd"); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpResourceHandler resource(String uri, Function<String,McpResourceOutcome> fn) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("a").setMimeType("text/plain"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return fn.apply(u); }
		};
	}

	private static McpResourceTemplateHandler template(String uriTemplate, String name, String title, String description,
			BiFunction<String,Map<String,String>,McpResourceOutcome> fn) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return new McpResourceTemplateSpec().setUriTemplate(uriTemplate).setName(name).setTitle(title)
					.setDescription(description).setMimeType("text/plain");
			}
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return fn.apply(uri, variables); }
		};
	}

	/**
	 * Maps each fixture to the servlet whose tool/prompt/resource registrations it needs.
	 *
	 * <p>
	 * Matched per literal fixture name, not by prefix: {@code ERROR-missing-tool} needs {@code echo}
	 * <em>unregistered</em> while {@code ERROR-handler-failure} needs it registered and throwing, so
	 * both share the {@code ERROR-} prefix but resolve to different servlets.
	 */
	private static Class<?> servletFor(String fixture) {
		return switch (fixture) {
			case "ERROR-handler-failure" -> F_Throw.class;
			case "SCHEMA-draft-2020-12" -> F_Schema.class;
			case "FULL-tools-list", "FULL-tools-call", "FULL-prompts-list", "FULL-prompts-get",
				"FULL-resources-list", "FULL-resources-read", "FULL-resource-templates-list",
				"HEADER-valid-named", "STATELESS-repeat" -> F_Full.class;
			case "CACHE-tools-list", "CACHE-prompts-list", "CACHE-resources-list",
				"CACHE-resource-templates-list", "CACHE-resources-read" -> F_Cache.class;
			case "STRUCTURED-object", "STRUCTURED-array", "STRUCTURED-string", "STRUCTURED-boolean", "STRUCTURED-null" -> F_Structured.class;
			case "TEMPLATE-simple-read", "TEMPLATE-reserved-read", "TEMPLATE-exact-precedence",
				"TEMPLATE-most-specific", "TEMPLATE-read-unknown" -> F_Template.class;
			case "COMPLETE-prompt", "COMPLETE-resource-template", "COMPLETE-context",
				"COMPLETE-empty-unknown", "COMPLETE-capped" -> F_Complete.class;
			default -> F_Empty.class;
		};
	}

	// --- replay ----------------------------------------------------------------------------

	static List<String> fixtures() throws Exception {
		try (var s = Files.list(DIR)) {
			return s.map(x -> x.getFileName().toString())
				.filter(x -> x.endsWith(".request.json"))
				.map(x -> x.substring(0, x.length() - ".request.json".length()))
				.sorted()
				.toList();
		}
	}

	@ParameterizedTest
	@MethodSource("fixtures")
	void a01_wireIsUnchanged(String fixture) throws Exception {
		var actual = replayHttp(fixture);
		var expected = DIR.resolve(fixture + ".response.json");
		if (WRITE) {
			Files.writeString(expected, actual);
			return;
		}
		assertEquals(Files.readString(expected), actual,
			() -> fixture + ": WIRE FORMAT CHANGED. Do not update the fixture — fix the code.");
	}

	private String replayHttp(String fixture) throws Exception {
		var requestBody = Files.readString(DIR.resolve(fixture + ".request.json")).strip();
		var headers = loadHeaders(fixture);
		var client = MockRestClient.create(servletFor(fixture)).json()
			.contentType("application/json").accept("application/json").ignoreErrors().build();
		var req = client.post("/").contentString(requestBody);
		headers.forEach(req::header);
		var res = req.run();
		assertEquals(200, res.getStatusCode(), () -> fixture + ": HTTP status changed");
		return res.getContent().asString();
	}

	private static Map<String,String> loadHeaders(String fixture) throws IOException {
		var props = new Properties();
		try (var in = Files.newBufferedReader(DIR.resolve(fixture + ".headers.properties"))) {
			props.load(in);
		}
		var m = new LinkedHashMap<String,String>();
		for (var name : props.stringPropertyNames())
			m.put(name, props.getProperty(name));
		return m;
	}
}
