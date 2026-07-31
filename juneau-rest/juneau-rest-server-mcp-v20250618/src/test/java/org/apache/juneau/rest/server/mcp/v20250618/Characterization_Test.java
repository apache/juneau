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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Wire-compatibility characterization fixtures for the MCP JSON-RPC endpoint.
 *
 * <p>
 * These fixtures encode <em>current</em> behavior, including behavior known to be wrong (all four
 * "not found" kinds collapse to {@code -32601}). They are deliberately not named {@code golden/}.
 * A fixture body must never be edited to accommodate a code change: if replay fails, the code
 * change is wrong.
 *
 * <p>
 * Regenerate the {@code *.response.json} files (only ever against known-good code) with:
 * <p>
 * {@code mvn test -Drat.skip=true -pl juneau-rest/juneau-rest-server-mcp-v20250618 -Dtest=Characterization_Test -Djuneau.mcp.characterization.write=true}
 */
class Characterization_Test {

	private static final Path DIR = Paths.get("src/test/resources/characterization");
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
				.setName("characterization").setVersion("1.0.0")
				.setInstructions("Be concise.")
				.addTool(tool("echo", McpSchema.of(JsonMap.of("type", "object", "required", List.of("text"))), a -> McpToolOutcome.text(String.valueOf(a.get("text")))))
				.addTool(tool("mixed", null, a -> McpToolOutcome.of(
					McpContentBlock.text("t"),
					McpContentBlock.image("AAA=", "image/png"),
					McpContentBlock.resource(McpResourceContents.text("file:///e", "text/plain", "emb")))))
				.addTool(tool("failing", null, a -> McpToolOutcome.text("nope").setError(true)))
				.addPrompt(prompt("greet", a -> new McpPromptOutcome().setDescription("d").setMessages(List.of(
					new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text("hi " + a.get("who")))))))
				.addResource(resource("file:///a", u -> new McpResourceOutcome().setContents(List.of(
					McpResourceContents.text(u, "text/plain", "body")))));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Caps extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig();
		}

		@Override
		protected ServerCapabilities capabilities() {
			return new ServerCapabilities()
				.setLogging(new LoggingCapability().setLevel("info"))
				.setResources(new ResourceCapability().setSubscribe(true).setListChanged(true))
				.setExperimental(JsonMap.of("flag", 1));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Paged extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.setCursor(McpCursor.fixedSize(1))
				.addTool(tool("t1", null, a -> new McpToolOutcome()))
				.addTool(tool("t2", null, a -> new McpToolOutcome()));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Throw extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addTool(tool("mcpEx", null, a -> { throw new McpException(-32099, "nope", JsonMap.of("k", "v")); }))
				.addTool(tool("boom", null, a -> { throw new RuntimeException("boom"); }))
				.addTool(tool("silent", null, a -> { throw new IllegalStateException(); }));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Structured extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(new McpToolHandler() {
				@Override public McpToolSpec descriptor() {
					return new McpToolSpec().setName("structured").setDescription("desc:structured")
						.setInputSchema(McpSchema.of(JsonMap.of("type", "object")))
						.setOutputSchema(McpSchema.of(JsonMap.of(
							"type", "object",
							"properties", JsonMap.of("x", JsonMap.of("type", "integer")),
							"required", List.of("x"),
							"additionalProperties", false)));
				}
				@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
					var value = arguments.get("value");
					return new McpToolOutcome()
						.setStructuredContent(value)
						.setContent(List.of(McpContentBlock.text(Json.of(value))));
				}
			});
		}
	}

	/**
	 * Backs every {@code TEMPLATE-*} fixture: one exact resource that always outranks any template match,
	 * plus four resource-template registrations (in listing order) exercising decoded-scalar capture,
	 * reserved/unencoded capture, a listing-only non-winning two-variable template, and literal-prefix
	 * specificity. {@code TEMPLATE-read-unknown} deliberately reuses this template-registered servlet
	 * (rather than {@link F_Empty}) so its "no match" outcome proves a real miss against a populated
	 * registry, not a trivially empty one.
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

	private static Class<?> servletFor(String fixture) {
		var prefix = fixture.substring(0, fixture.indexOf('-'));
		return switch (prefix) {
			case "EMPTY" -> F_Empty.class;
			case "FULL" -> F_Full.class;
			case "CAPS" -> F_Caps.class;
			case "PAGED" -> F_Paged.class;
			case "THROW" -> F_Throw.class;
			case "STRUCTURED" -> F_Structured.class;
			case "TEMPLATE" -> F_Template.class;
			case "COMPLETE" -> F_Complete.class;
			default -> throw new IllegalArgumentException("Unknown fixture prefix: " + prefix);
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
		var requestBody = Files.readString(DIR.resolve(fixture + ".request.json")).strip();
		try (var client = MockRestClient.create(servletFor(fixture)).json()
				.contentType("application/json").accept("application/json").ignoreErrors().build()) {
			var req = client.post("/").contentString(requestBody);
			loadHeaders(fixture).forEach(req::header);
			var res = req.run();
			assertEquals(200, res.getStatusCode(), () -> fixture + ": HTTP status changed");
			var actual = res.getContent().asString();
			var expected = DIR.resolve(fixture + ".response.json");
			if (WRITE) {
				Files.writeString(expected, actual);
				return;
			}
			assertEquals(Files.readString(expected), actual,
				() -> fixture + ": WIRE FORMAT CHANGED. Do not update the fixture — fix the code.");
		}
	}

	private static Map<String,String> loadHeaders(String fixture) throws IOException {
		var m = new LinkedHashMap<String,String>();
		var path = DIR.resolve(fixture + ".headers.properties");
		if (! Files.exists(path))
			return m;
		var props = new Properties();
		try (var in = Files.newBufferedReader(path)) {
			props.load(in);
		}
		for (var name : props.stringPropertyNames())
			m.put(name, props.getProperty(name));
		return m;
	}
}
