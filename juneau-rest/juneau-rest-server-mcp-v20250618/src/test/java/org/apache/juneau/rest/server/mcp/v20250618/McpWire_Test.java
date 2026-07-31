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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@code 2025-06-18} wire mapping of the structured output fields in both directions.
 */
class McpWire_Test {

	@Test void a01_toolSpecNeutralToWireMapsBothSchemas() {
		var a = McpSchema.of(JsonMap.of("type", "object"));
		var b = McpSchema.of(JsonMap.of("type", "object", "properties", JsonMap.of("x", JsonMap.of("type", "integer"))));
		var c = McpWire.toWire(new McpToolSpec().setName("t").setDescription("d").setInputSchema(a).setOutputSchema(b));
		assertEquals("t", c.getName());
		// Compare as parsed maps: the neutral JsonMap preserves insertion order while the v1 JsonSchema bean
		// serializes properties alphabetically, so an order-sensitive string compare would spuriously fail.
		assertEquals(Json.to(Json.of(a.toJsonMap()), JsonMap.class), Json.to(Json.of(c.getInputSchema()), JsonMap.class));
		assertEquals(Json.to(Json.of(b.toJsonMap()), JsonMap.class), Json.to(Json.of(c.getOutputSchema()), JsonMap.class));
	}

	@Test void a02_toolSpecWireToNeutralMapsBothSchemas() {
		var a = new JsonSchema().setType("object");
		var b = new JsonSchema().setType("object").addProperty("x", new JsonSchema().setType("integer"));
		var c = McpWire.toNeutral(new Tool().setName("t").setInputSchema(a).setOutputSchema(b));
		assertEquals(Json.of(a), Json.of(c.getInputSchema().toJsonMap()));
		assertEquals(Json.of(b), Json.of(c.getOutputSchema().toJsonMap()));
	}

	@Test void b01_outcomeNeutralToWireMapsStructuredIdentity() {
		var a = JsonMap.of("x", 1);
		var b = McpWire.toWire(new McpToolOutcome().setStructuredContent(a).setError(true)
			.setContent(List.of(McpContentBlock.text("{\"x\":1}"))));
		assertSame(a, b.getStructuredContent());
		assertEquals(Boolean.TRUE, b.getIsError());
		assertEquals(1, b.getContent().size());
	}

	@Test void b02_outcomeWireToNeutralMapsStructuredIdentity() {
		var a = JsonMap.of("x", 1);
		var b = McpWire.toNeutral(new CallToolResult().setStructuredContent(a).setIsError(true)
			.setContent(new TextContent().setText("{\"x\":1}")));
		assertSame(a, b.getStructuredContent());
		assertEquals(Boolean.TRUE, b.getError());
		assertEquals(1, b.getContent().size());
	}

	@Test void c01_resourceTemplate_mapsAllFieldsBothWaysAndNull() {
		var neutral = new McpResourceTemplateSpec().setUriTemplate("file:///{name}").setName("n")
			.setTitle("t").setDescription("d").setMimeType("text/plain");
		var wire = McpWire.toWire(neutral);
		assertEquals("file:///{name}", wire.getUriTemplate());
		assertEquals("n", wire.getName());
		assertEquals("t", wire.getTitle());
		assertEquals("d", wire.getDescription());
		assertEquals("text/plain", wire.getMimeType());
		var copy = McpWire.toNeutral(wire);
		assertEquals(neutral.getUriTemplate(), copy.getUriTemplate());
		assertEquals(neutral.getName(), copy.getName());
		assertEquals(neutral.getTitle(), copy.getTitle());
		assertEquals(neutral.getDescription(), copy.getDescription());
		assertEquals(neutral.getMimeType(), copy.getMimeType());
		assertNull(McpWire.toWire((McpResourceTemplateSpec) null));
		assertNull(McpWire.toNeutral((ResourceTemplate) null));
	}

	@Test void c02_promptArgument_wireBeanHasNoCompleterField() {
		var fields = Arrays.stream(PromptArgument.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList();
		assertFalse(fields.stream().anyMatch(n -> n.toLowerCase().contains("completer")), fields.toString());
	}

	@Test void c03_promptArgument_completerNeverSerializedOnWire() {
		var neutral = new McpPromptArgument().setName("a").setDescription("ad").setRequired(true)
			.setCompleter((request, ctx) -> McpCompletionResult.empty());
		var wire = McpWire.toWire(neutral);
		assertEquals("a", wire.getName());
		assertEquals("ad", wire.getDescription());
		assertEquals(Boolean.TRUE, wire.getRequired());
		var json = Json.of(wire);
		assertFalse(json.toLowerCase().contains("completer"), json);
	}

	@Test void d01_completionResult_mapsValuesTotalAndHasMore() {
		var normalized = McpCompletionResult.normalize(new McpCompletionResult().setValues(List.of("a", "b")).setTotal(5).setHasMore(true));
		var wire = McpWire.toWire(normalized);
		assertEquals(List.of("a", "b"), wire.getCompletion().getValues());
		assertEquals(5, wire.getCompletion().getTotal());
		assertEquals(Boolean.TRUE, wire.getCompletion().getHasMore());
	}

	@Test void d02_completionResult_emptyOmitsTotalAndHasMore() {
		var normalized = McpCompletionResult.normalize(McpCompletionResult.empty());
		var wire = McpWire.toWire(normalized);
		assertEquals(List.of(), wire.getCompletion().getValues());
		assertNull(wire.getCompletion().getTotal());
		assertNull(wire.getCompletion().getHasMore());
	}
}
