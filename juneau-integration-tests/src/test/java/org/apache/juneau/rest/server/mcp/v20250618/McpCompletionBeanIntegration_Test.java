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

import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Cross-module coverage for the {@code juneau-bean-mcp-v20250618} template-backfill and completion wire
 * beans, exercised from {@code juneau-integration-tests} exactly as an external consumer would: through the
 * published public API of the artifact, with no access to module-internal test helpers.
 *
 * <p>
 * Mirrors the acceptance surface of the module-local {@code McpBeans_RoundTrip_Test}: the V1 template-list
 * backfill ({@link ResourceTemplate}, {@link ListResourceTemplatesResult}), and the shared completion shapes
 * ({@link CompleteRequest}, {@link CompleteResult}, {@link Completion}, {@link PromptReference},
 * {@link ResourceTemplateReference}, {@link CompletionReference}, {@link CompletionArgument},
 * {@link CompletionContext}, {@link CompletionCapability}).
 */
class McpCompletionBeanIntegration_Test {

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

	// -------- V1 template-list backfill ---------

	@Test void a01_resourceTemplate_allFieldsRoundTrip() {
		var t = new ResourceTemplate()
			.setUriTemplate("file:///{name}")
			.setName("n")
			.setTitle("t")
			.setDescription("desc")
			.setMimeType("text/plain");
		var json = JsonSerializer.DEFAULT.write(t);
		var copy = JsonParser.DEFAULT.read(json, ResourceTemplate.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("file:///{name}", copy.getUriTemplate());
		assertEquals("n", copy.getName());
		assertEquals("t", copy.getTitle());
		assertEquals("desc", copy.getDescription());
		assertEquals("text/plain", copy.getMimeType());
	}

	@Test void a02_listResourceTemplatesResult_roundTrip() {
		var t = new ResourceTemplate().setUriTemplate("file:///{name}").setName("n");
		var lr = new ListResourceTemplatesResult().setResourceTemplates(List.of(t)).setNextCursor("c1");
		var json = JsonSerializer.DEFAULT.write(lr);
		var copy = JsonParser.DEFAULT.read(json, ListResourceTemplatesResult.class);
		assertEquals("file:///{name}", copy.getResourceTemplates().get(0).getUriTemplate());
		assertEquals("c1", copy.getNextCursor());
	}

	@Test void a03_resourceTemplatesListMethodConstant() {
		assertEquals("resources/templates/list", McpMethods.RESOURCES_TEMPLATES_LIST);
	}

	// -------- completion request/result/reference/capability shapes ---------

	@Test void b01_completeRequest_promptReferenceRoundTrip() {
		var req = new CompleteRequest()
			.setRef(new PromptReference().setName("greet"))
			.setArgument(new CompletionArgument().setName("name").setValue("al"));
		assertJsonRoundTrip(req, CompleteRequest.class);
	}

	@Test void b02_completeRequest_resourceTemplateReferenceWithContextRoundTrip() {
		var req = new CompleteRequest()
			.setRef(new ResourceTemplateReference().setUri("file:///{name}"))
			.setArgument(new CompletionArgument().setName("name").setValue("a"))
			.setContext(new CompletionContext().putArgument("scope", "public"));
		assertJsonRoundTrip(req, CompleteRequest.class);
	}

	@Test void b03_completionReference_exactDiscriminators() {
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

	@Test void b04_completionReference_closedDictionaryHasNoFallbackSubtype() {
		assertEquals(Set.of(PromptReference.class, ResourceTemplateReference.class),
			Set.of(CompletionReference.class.getAnnotation(org.apache.juneau.marshall.Marshalled.class).dictionary()));
	}

	@Test void b05_completionArgumentAndContext_fieldsRoundTrip() {
		var arg = new CompletionArgument().setName("n").setValue("v");
		var argCopy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(arg), CompletionArgument.class);
		assertEquals(JsonSerializer.DEFAULT.write(arg), JsonSerializer.DEFAULT.write(argCopy));
		var ctx = new CompletionContext().setArguments(Map.of("k", "v"));
		var ctxCopy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(ctx), CompletionContext.class);
		assertEquals(Map.of("k", "v"), ctxCopy.getArguments());
	}

	@Test void b06_completeResult_emptyValuesRoundTrip() {
		var result = new CompleteResult().setCompletion(new Completion().setValues(List.of()));
		assertEquals("{\"completion\":{\"values\":[]}}", JsonSerializer.DEFAULT.write(result));
		var copy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(result), CompleteResult.class);
		assertEquals(JsonSerializer.DEFAULT.write(result), JsonSerializer.DEFAULT.write(copy));
	}

	@Test void b07_completeResult_cappedValuesWithTotalAndHasMoreRoundTrip() {
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

	@Test void b08_completion_optionalFieldsOmittedFromJson() {
		var completion = new Completion().setValues(List.of("a"));
		assertEquals("{\"values\":[\"a\"]}", JsonSerializer.DEFAULT.write(completion));
	}

	@Test void b09_completionCapability_emptySerializationAndNullableByDefault() {
		assertNull(new ServerCapabilities().getCompletions());
		var caps = new ServerCapabilities().setCompletions(new CompletionCapability());
		assertTrue(JsonSerializer.DEFAULT.write(caps).contains("\"completions\":{}"));
	}

	@Test void b10_completionCompleteMethodConstant() {
		assertEquals("completion/complete", McpMethods.COMPLETION_COMPLETE);
	}
}
