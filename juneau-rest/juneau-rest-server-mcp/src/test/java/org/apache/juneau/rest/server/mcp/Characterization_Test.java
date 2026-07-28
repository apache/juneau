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

import java.nio.file.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
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
 * {@code mvn test -Drat.skip=true -pl juneau-rest/juneau-rest-server-mcp -Dtest=Characterization_Test -Djuneau.mcp.characterization.write=true}
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
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
				.setServerInfo(new Implementation().setName("characterization").setVersion("1.0.0"))
				.setInstructions("Be concise.")
				.addTool(tool("echo", new JsonSchema().setType("object").setRequired("text"), a -> new CallToolResult()
					.setContent(List.of(new TextContent().setText(String.valueOf(a.get("text")))))))
				.addTool(tool("mixed", null, a -> new CallToolResult().setContent(List.of(
					new TextContent().setText("t"),
					new ImageContent().setData("AAA=").setMimeType("image/png"),
					new EmbeddedResourceContent().setResource(
						new TextResourceContents().setUri("file:///e").setMimeType("text/plain").setText("emb"))))))
				.addTool(tool("failing", null, a -> new CallToolResult().setIsError(true)
					.setContent(List.of(new TextContent().setText("nope")))))
				.addPrompt(prompt("greet", a -> new GetPromptResult().setDescription("d").setMessages(List.of(
					new PromptMessage().setRole(Role.USER).setContent(new TextContent().setText("hi " + a.get("who")))))))
				.addResource(resource("file:///a", u -> new ReadResourceResult().setContents(List.of(
					new TextResourceContents().setUri(u).setMimeType("text/plain").setText("body")))));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Caps extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.setCapabilities(new ServerCapabilities()
					.setLogging(new LoggingCapability().setLevel("info"))
					.setResources(new ResourceCapability().setSubscribe(true).setListChanged(true))
					.setExperimental(JsonMap.of("flag", 1)));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Paged extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.setCursor(McpCursor.fixedSize(1))
				.addTool(tool("t1", null, a -> new CallToolResult()))
				.addTool(tool("t2", null, a -> new CallToolResult()));
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

	// --- fixture handler factories ---------------------------------------------------------

	private static McpToolHandler tool(String name, JsonSchema schema, Function<Map<String,Object>,CallToolResult> fn) {
		return new McpToolHandler() {
			@Override public Tool descriptor() { return new Tool().setName(name).setDescription("desc:" + name).setInputSchema(schema); }
			@Override public CallToolResult call(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpPromptHandler prompt(String name, Function<Map<String,Object>,GetPromptResult> fn) {
		return new McpPromptHandler() {
			@Override public Prompt descriptor() { return new Prompt().setName(name).setDescription("pd"); }
			@Override public GetPromptResult get(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpResourceHandler resource(String uri, Function<String,ReadResourceResult> fn) {
		return new McpResourceHandler() {
			@Override public Resource descriptor() { return new Resource().setUri(uri).setName("a").setMimeType("text/plain"); }
			@Override public ReadResourceResult read(String u, BeanStore ctx) { return fn.apply(u); }
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
		var client = MockRestClient.create(servletFor(fixture)).json()
			.contentType("application/json").accept("application/json").ignoreErrors().build();
		var res = client.post("/").contentString(requestBody).run();
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
