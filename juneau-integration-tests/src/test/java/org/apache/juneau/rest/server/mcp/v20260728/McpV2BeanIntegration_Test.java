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
import java.util.stream.*;

import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.JsonParser;
import org.apache.juneau.marshall.json.JsonSerializer;
import org.apache.juneau.marshall.marshaller.Json;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-module coverage for the {@code juneau-bean-mcp-v20260728} wire beans, exercised from
 * {@code juneau-integration-tests} exactly as an external consumer would: through the published
 * public API of the artifact, with no access to module-internal test helpers.
 *
 * <p>
 * Mirrors the acceptance surface of the module-local {@code McpV2Beans_Test}: opaque per-request
 * {@link RequestMeta}, {@link ServerDiscoverResult}, the five Draft-2020-12 special schema keys
 * ({@code $comment}/{@code $defs}/{@code $id}/{@code $ref}/{@code $schema}), both {@link Tool}
 * schemas, and all five {@link CallToolResult#getStructuredContent()} JSON categories.
 */
class McpV2BeanIntegration_Test {

	private static final JsonSerializer MCP_JSON =
		JsonSerializer.create()
			.addBeanTypes()
			.typePropertyName(CompletionReference.class, "type")
			.build();

	private static final JsonParser MCP_JSON_PARSER =
		JsonParser.create()
			.typePropertyName(CompletionReference.class, "type")
			.build();

	private static void assertJsonRoundTrip(Object bean, Class<?> type) {
		var j1 = MCP_JSON.write(bean);
		var copy = MCP_JSON_PARSER.read(j1, type);
		var j2 = MCP_JSON.write(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	@Test
	void a01_metadataAndDiscoveryRoundTrip() {
		var meta = new RequestMeta().setProtocolVersion("2026-07-28")
			.setClientInfo(new Implementation().setName("c").setVersion("1"))
			.setCapabilities(new ClientCapabilities());
		var metaCopy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(meta), RequestMeta.class);
		assertEquals("2026-07-28", metaCopy.getProtocolVersion());
		assertEquals("c", metaCopy.getClientInfo().getName());
		assertNotNull(metaCopy.getCapabilities());

		var result = new ServerDiscoverResult()
			.setServerInfo(new Implementation().setName("s").setVersion("2"))
			.setCapabilities(new ServerCapabilities().setTools(new ToolCapability()));
		var resultCopy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(result), ServerDiscoverResult.class);
		assertEquals("s", resultCopy.getServerInfo().getName());
		assertNotNull(resultCopy.getCapabilities().getTools());
	}

	@Test
	void a02_toolInputAndOutputSchema_roundTripAsJsonSchema() {
		var input = new JsonSchema<>().setType(JsonType.OBJECT)
			.addProperties(new JsonSchemaProperty<>("q", JsonType.STRING)).addRequired("q");
		var output = new JsonSchema<>().setType(JsonType.STRING);
		var tool = new Tool().setName("t1").setDescription("d1").setInputSchema(input).setOutputSchema(output);
		var json = JsonSerializer.DEFAULT.write(tool);
		var copy = JsonParser.DEFAULT.read(json, Tool.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals(JsonType.OBJECT, copy.getInputSchema().getTypeAsJsonType());
		assertEquals(JsonType.STRING, copy.getOutputSchema().getTypeAsJsonType());
	}

	@Test
	void a03_schemaExactKeyNames_liveRoundTrip() {
		var schema = new JsonSchema<>()
			.setComment("note")
			.setIdUri("https://example.com/s.json")
			.setSchemaVersionUri("https://json-schema.org/draft/2020-12/schema")
			.addDef("Id", new JsonSchema<>().setType(JsonType.STRING))
			.addProperties(new JsonSchemaProperty<>("id").setRef("#/$defs/Id"));
		var json = JsonSerializer.DEFAULT.write(schema);
		assertTrue(json.contains("\"$comment\":\"note\""));
		assertTrue(json.contains("\"$id\":\"https://example.com/s.json\""));
		assertTrue(json.contains("\"$schema\":\"https://json-schema.org/draft/2020-12/schema\""));
		assertTrue(json.contains("\"$defs\":"));
		assertTrue(json.contains("\"$ref\":\"#/$defs/Id\""));
		var copy = JsonParser.DEFAULT.read(json, JsonSchema.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
	}

	@ParameterizedTest
	@MethodSource("structuredValues")
	void a04_structuredContent_roundTripsAnyJsonValue(Object value) {
		var json = JsonSerializer.DEFAULT.write(new CallToolResult().setStructuredContent(value));
		var copy = JsonParser.DEFAULT.read(json, CallToolResult.class);
		assertEquals(JsonSerializer.DEFAULT.write(value), JsonSerializer.DEFAULT.write(copy.getStructuredContent()));
	}

	static Stream<Object> structuredValues() {
		return Stream.of(JsonMap.of("x", 1), JsonList.of(1, 2), "text", true, null);
	}

	@Test void b01_cacheScopeAndFiveCarriers_roundTrip() {
		assertArrayEquals(new McpCacheScope[] { McpCacheScope.PUBLIC, McpCacheScope.PRIVATE }, McpCacheScope.values());
		assertEquals("\"public\"", Json.of(McpCacheScope.PUBLIC));
		assertEquals("\"private\"", Json.of(McpCacheScope.PRIVATE));
		for (var value : List.<CacheableResult<?>>of(new ListToolsResult(), new ListPromptsResult(),
			new ListResourcesResult(), new ListResourceTemplatesResult(), new ReadResourceResult())) {
			assertFalse(Json.of(value).contains("ttlMs"));
			assertFalse(Json.of(value).contains("cacheScope"));
			value.setTtlMs(0).setCacheScope(McpCacheScope.PRIVATE);
			assertTrue(Json.of(value).contains("\"ttlMs\":0"));
			assertTrue(Json.of(value).contains("\"cacheScope\":\"private\""));
			var copy = (CacheableResult<?>)JsonParser.DEFAULT.read(Json.of(value), value.getClass());
			assertEquals(0, copy.getTtlMs());
			assertEquals(McpCacheScope.PRIVATE, copy.getCacheScope());
		}
	}

	@Test void b02_resourceTemplateAndList_publicContract() {
		var template = new ResourceTemplate().setUriTemplate("file:///{name}").setName("n")
			.setTitle("t").setDescription("d").setMimeType("text/plain");
		var result = new ListResourceTemplatesResult().setResourceTemplates(template)
			.setNextCursor("1").setTtlMs(5).setCacheScope(McpCacheScope.PUBLIC);
		var copy = JsonParser.DEFAULT.read(Json.of(result), ListResourceTemplatesResult.class);
		assertEquals("file:///{name}", copy.getResourceTemplates().get(0).getUriTemplate());
		assertEquals("n", copy.getResourceTemplates().get(0).getName());
		assertEquals("t", copy.getResourceTemplates().get(0).getTitle());
		assertEquals("d", copy.getResourceTemplates().get(0).getDescription());
		assertEquals("text/plain", copy.getResourceTemplates().get(0).getMimeType());
		assertEquals("1", copy.getNextCursor());
		assertThrows(UnsupportedOperationException.class,
			() -> copy.getResourceTemplates().add(template));
		assertEquals("resources/templates/list", McpMethods.RESOURCES_TEMPLATES_LIST);
	}

	@Test void b03_resourceTemplateAndListResult_cacheBearingRoundTrip_reassertsC2Precedence() {
		// Reasserts, from the consumed artifact, that C2's cache-bearing carrier contract still holds for
		// the C4 template-list result: ttlMs/cacheScope round-trip identically to every other CacheableResult.
		var template = new ResourceTemplate().setUriTemplate("file:///{name}").setName("n").setMimeType("text/plain");
		var result = new ListResourceTemplatesResult().setResourceTemplates(template)
			.setNextCursor("2").setTtlMs(60000).setCacheScope(McpCacheScope.PRIVATE);
		var json = Json.of(result);
		assertTrue(json.contains("\"ttlMs\":60000"));
		assertTrue(json.contains("\"cacheScope\":\"private\""));
		var copy = JsonParser.DEFAULT.read(json, ListResourceTemplatesResult.class);
		assertEquals(60000, copy.getTtlMs());
		assertEquals(McpCacheScope.PRIVATE, copy.getCacheScope());
		assertEquals("file:///{name}", copy.getResourceTemplates().get(0).getUriTemplate());
		assertEquals("2", copy.getNextCursor());
	}

	// -------- completion request/result/reference/capability shapes ---------

	@Test void c01_completeRequest_promptReferenceRoundTrip() {
		var req = new CompleteRequest()
			.setRef(new PromptReference().setName("greet"))
			.setArgument(new CompletionArgument().setName("name").setValue("al"));
		assertJsonRoundTrip(req, CompleteRequest.class);
	}

	@Test void c02_completeRequest_resourceTemplateReferenceWithContextRoundTrip() {
		var req = new CompleteRequest()
			.setRef(new ResourceTemplateReference().setUri("file:///{name}"))
			.setArgument(new CompletionArgument().setName("name").setValue("a"))
			.setContext(new CompletionContext().putArgument("scope", "public"));
		assertJsonRoundTrip(req, CompleteRequest.class);
	}

	@Test void c03_completionReference_exactDiscriminators() {
		var promptJson = MCP_JSON.write(
			new CompleteRequest().setRef(new PromptReference().setName("greet"))
				.setArgument(new CompletionArgument().setName("n").setValue("v")));
		assertTrue(promptJson.contains("\"type\":\"ref/prompt\""));
		assertTrue(promptJson.contains("\"name\":\"greet\""));
		var resourceJson = MCP_JSON.write(
			new CompleteRequest().setRef(new ResourceTemplateReference().setUri("file:///{name}"))
				.setArgument(new CompletionArgument().setName("n").setValue("v")));
		assertTrue(resourceJson.contains("\"type\":\"ref/resource\""));
		assertTrue(resourceJson.contains("\"uri\":\"file:///{name}\""));
	}

	@Test void c04_completionReference_closedDictionaryHasNoFallbackSubtype() {
		assertEquals(Set.of(PromptReference.class, ResourceTemplateReference.class),
			Set.of(CompletionReference.class.getAnnotation(org.apache.juneau.marshall.Marshalled.class).dictionary()));
	}

	@Test void c05_completionArgumentAndContext_fieldsRoundTrip() {
		var arg = new CompletionArgument().setName("n").setValue("v");
		var copy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(arg), CompletionArgument.class);
		assertEquals(JsonSerializer.DEFAULT.write(arg), JsonSerializer.DEFAULT.write(copy));
		var ctx = new CompletionContext().setArguments(Map.of("k", "v"));
		var ctxCopy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(ctx), CompletionContext.class);
		assertEquals(Map.of("k", "v"), ctxCopy.getArguments());
	}

	@Test void c06_completeResult_emptyValuesRoundTrip() {
		var result = new CompleteResult().setCompletion(new Completion().setValues(List.of()));
		assertEquals("{\"completion\":{\"values\":[]}}", JsonSerializer.DEFAULT.write(result));
		var copy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(result), CompleteResult.class);
		assertEquals(JsonSerializer.DEFAULT.write(result), JsonSerializer.DEFAULT.write(copy));
	}

	@Test void c07_completeResult_cappedValuesWithTotalAndHasMoreRoundTrip() {
		var values = new ArrayList<String>();
		for (var i = 0; i < 100; i++)
			values.add("v" + i);
		var completion = new Completion().setValues(values).setTotal(150).setHasMore(true);
		var result = new CompleteResult().setCompletion(completion);
		var copy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(result), CompleteResult.class);
		assertEquals(100, copy.getCompletion().getValues().size());
		assertEquals(150, copy.getCompletion().getTotal());
		assertEquals(true, copy.getCompletion().getHasMore());
	}

	@Test void c08_completion_optionalFieldsOmittedFromJson() {
		var completion = new Completion().setValues(List.of("a"));
		assertEquals("{\"values\":[\"a\"]}", JsonSerializer.DEFAULT.write(completion));
	}

	@Test void c09_completionCapability_emptySerializationAndNullableByDefault() {
		assertNull(new ServerCapabilities().getCompletions());
		var caps = new ServerCapabilities().setCompletions(new CompletionCapability());
		assertTrue(JsonSerializer.DEFAULT.write(caps).contains("\"completions\":{}"));
	}

	@Test void c10_completionCompleteMethodConstant() {
		assertEquals("completion/complete", McpMethods.COMPLETION_COMPLETE);
	}
}
