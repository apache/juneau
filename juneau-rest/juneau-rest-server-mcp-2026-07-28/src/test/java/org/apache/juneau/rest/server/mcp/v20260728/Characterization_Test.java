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

import org.apache.juneau.bean.jsonrpc.*;
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
 * Per Resolution B1, the {@code STRUCTURED-*} fixtures cannot be produced through an actual HTTP
 * dispatch: the revision-neutral {@link org.apache.juneau.rest.server.mcp.McpToolOutcome} has no
 * {@code structuredContent} field for a handler to populate. Those five fixtures are instead
 * replayed as a direct {@link CallToolResult} bean round-trip wrapped in a hand-built
 * {@link JsonRpcResponse}, proving the v2 wire shape without pretending the neutral core produces it.
 *
 * <p>
 * Regenerate the {@code *.response.json} files (only ever against known-good code) with:
 * <p>
 * {@code mvn test -Drat.skip=true -pl juneau-rest/juneau-rest-server-mcp-2026-07-28 -Dtest=Characterization_Test -Djuneau.mcp.characterization.write=true}
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class Characterization_Test {

	private static final Path DIR = Paths.get("src/test/resources/characterization");
	private static final boolean WRITE = Boolean.getBoolean("juneau.mcp.characterization.write");

	private static final Set<String> STRUCTURED_DIRECT = Set.of(
		"STRUCTURED-object", "STRUCTURED-array", "STRUCTURED-string", "STRUCTURED-boolean", "STRUCTURED-null");

	/**
	 * The servlet path applies {@code @SerializerConfig(addBeanTypes = "true")} (see the core
	 * {@link org.apache.juneau.rest.server.mcp.McpRestServlet}) so polymorphic {@link Content} wire
	 * types are tagged with their {@code type} discriminator. The {@code STRUCTURED-*} direct bean
	 * round-trips bypass the servlet entirely, so they need the same serializer setting explicitly.
	 */
	private static final JsonSerializer DIRECT_SERIALIZER = JsonSerializer.create().addBeanTypes().build();

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
					McpResourceContents.text(u, "text/plain", "body")))));
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
				"FULL-resources-list", "FULL-resources-read", "HEADER-valid-named", "STATELESS-repeat" -> F_Full.class;
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
		var actual = STRUCTURED_DIRECT.contains(fixture) ? replayStructured(fixture) : replayHttp(fixture);
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

	/**
	 * Direct {@link CallToolResult} bean round-trip for the {@code STRUCTURED-*} fixtures (Resolution
	 * B1). These do not dispatch through the servlet: the neutral {@link McpToolOutcome} has no
	 * {@code structuredContent} field, so an actual HTTP call could never produce one. Instead the
	 * wire bean is built directly from the request's {@code arguments.value} and wrapped in a
	 * hand-built {@link JsonRpcResponse}, exercising exactly the wire contract the fixture encodes.
	 */
	private String replayStructured(String fixture) throws Exception {
		var requestBody = Files.readString(DIR.resolve(fixture + ".request.json")).strip();
		var top = JsonParser.DEFAULT.read(requestBody, JsonMap.class);
		var id = top.get("id");
		var params = (Map<?,?>) top.get("params");
		var arguments = (Map<?,?>) params.get("arguments");
		var value = arguments.get("value");
		var result = new CallToolResult().setContent(new TextContent().setText("ok")).setStructuredContent(value);
		return DIRECT_SERIALIZER.write(JsonRpcResponse.ok(id, result));
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
