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

import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
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
		assertSize(1, b.getContent());
	}

	@Test void b02_outcomeWireToNeutralMapsStructuredIdentity() {
		var a = JsonMap.of("x", 1);
		var b = McpWire.toNeutral(new CallToolResult().setStructuredContent(a).setIsError(true)
			.setContent(new TextContent().setText("{\"x\":1}")));
		assertSame(a, b.getStructuredContent());
		assertEquals(Boolean.TRUE, b.getError());
		assertSize(1, b.getContent());
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

	@Test void e01_nullInputs_mapToNullBothDirections() {
		assertNull(McpWire.toWire((McpToolSpec) null));
		assertNull(McpWire.toWire((McpSchema) null));
		assertNull(McpWire.toWire((McpToolOutcome) null));
		assertNull(McpWire.toWire((McpContentBlock) null));
		assertNull(McpWire.toWire((McpResourceContents) null));
		assertNull(McpWire.toWire((McpPromptSpec) null));
		assertNull(McpWire.toWire((McpPromptArgument) null));
		assertNull(McpWire.toWire((McpPromptOutcome) null));
		assertNull(McpWire.toWire((McpPromptMessage) null));
		assertNull(McpWire.toWire((McpRole) null));
		assertNull(McpWire.toWire((McpResourceSpec) null));
		assertNull(McpWire.toWire((McpResourceOutcome) null));
		assertNull(McpWire.toNeutral((Tool) null));
		assertNull(McpWire.toNeutral((JsonSchema) null));
		assertNull(McpWire.toNeutral((CallToolResult) null));
		assertNull(McpWire.toNeutral((Content) null));
		assertNull(McpWire.toNeutral((ResourceContents) null));
		assertNull(McpWire.toNeutral((Prompt) null));
		assertNull(McpWire.toNeutral((PromptArgument) null));
		assertNull(McpWire.toNeutral((GetPromptResult) null));
		assertNull(McpWire.toNeutral((PromptMessage) null));
		assertNull(McpWire.toNeutral((Role) null));
	}

	@Test void e02_contentBlocks_allKindsBothDirections() {
		var text = McpWire.toWire(McpContentBlock.text("t"));
		assertEquals("t", ((TextContent) text).getText());
		var image = (ImageContent) McpWire.toWire(McpContentBlock.image("ZGF0YQ==", "image/png"));
		assertEquals("ZGF0YQ==", image.getData());
		assertEquals("image/png", image.getMimeType());
		var embedded = (EmbeddedResourceContent) McpWire.toWire(McpContentBlock.resource(McpResourceContents.text("u:1", "text/plain", "body")));
		assertEquals("u:1", ((TextResourceContents) embedded.getResource()).getUri());

		assertEquals("t", McpWire.toNeutral(text).text());
		var image2 = McpWire.toNeutral(image);
		assertEquals(McpContentBlock.Kind.IMAGE, image2.kind());
		assertEquals("ZGF0YQ==", image2.data());
		assertEquals("image/png", image2.mimeType());
		var embedded2 = McpWire.toNeutral(embedded);
		assertEquals(McpContentBlock.Kind.RESOURCE, embedded2.kind());
		assertEquals("body", embedded2.resource().text());
	}

	@Test void e03_resourceContents_textAndBlobBothDirections() {
		var text = (TextResourceContents) McpWire.toWire(McpResourceContents.text("u:1", "text/plain", "body"));
		assertEquals("u:1", text.getUri());
		assertEquals("text/plain", text.getMimeType());
		assertEquals("body", text.getText());
		var blob = (BlobResourceContents) McpWire.toWire(McpResourceContents.blob("u:2", "application/octet-stream", "AAEC"));
		assertEquals("u:2", blob.getUri());
		assertEquals("AAEC", blob.getBlob());

		var text2 = McpWire.toNeutral(text);
		assertEquals(McpResourceContents.Kind.TEXT, text2.kind());
		assertEquals("body", text2.text());
		var blob2 = McpWire.toNeutral(blob);
		assertEquals(McpResourceContents.Kind.BLOB, blob2.kind());
		assertEquals("AAEC", blob2.blob());
		assertEquals("application/octet-stream", blob2.mimeType());
	}

	@Test void e04_resourceSpecAndOutcome_mapFields() {
		var spec = McpWire.toWire(new McpResourceSpec().setUri("u:1").setName("n").setTitle("t")
			.setDescription("d").setMimeType("text/plain").setSize(7L));
		assertEquals("u:1", spec.getUri());
		assertEquals("n", spec.getName());
		assertEquals("t", spec.getTitle());
		assertEquals("d", spec.getDescription());
		assertEquals("text/plain", spec.getMimeType());
		assertEquals(7L, spec.getSize());

		var outcome = McpWire.toWire(new McpResourceOutcome().setContents(List.of(McpResourceContents.text("u:1", "text/plain", "body"))));
		assertSize(1, outcome.getContents());
		assertNull(McpWire.toWire(new McpResourceOutcome()).getContents());
	}

	@Test void e05_promptSpecAndOutcome_nullAndPopulatedCollections() {
		var bare = McpWire.toWire(new McpPromptSpec().setName("p"));
		assertNull(bare.getArguments());
		var full = McpWire.toWire(new McpPromptSpec().setName("p").setDescription("d")
			.setArguments(List.of(new McpPromptArgument().setName("a").setRequired(true))));
		assertSize(1, full.getArguments());
		assertEquals("a", full.getArguments().get(0).getName());

		assertNull(McpWire.toNeutral(new Prompt().setName("p")).getArguments());
		var neutral = McpWire.toNeutral(new Prompt().setName("p").setArguments(List.of(new PromptArgument().setName("a"))));
		assertEquals("a", neutral.getArguments().get(0).getName());

		assertNull(McpWire.toWire(new McpPromptOutcome().setDescription("d")).getMessages());
		assertNull(McpWire.toNeutral(new GetPromptResult().setDescription("d")).getMessages());
		var msg = new McpPromptMessage().setRole(McpRole.ASSISTANT).setContent(McpContentBlock.text("hi"));
		var wire = McpWire.toWire(new McpPromptOutcome().setMessages(List.of(msg)));
		assertEquals(Role.ASSISTANT, wire.getMessages().get(0).getRole());
		var back = McpWire.toNeutral(wire);
		assertEquals(McpRole.ASSISTANT, back.getMessages().get(0).getRole());
		assertEquals("hi", back.getMessages().get(0).getContent().text());
	}

	@Test void e06_roles_allValuesBothDirections() {
		for (var r : McpRole.values())
			assertEquals(r, McpWire.toNeutral(McpWire.toWire(r)));
	}

	@Test void e07_toolOutcome_withoutContentKeepsNull() {
		assertNull(McpWire.toWire(new McpToolOutcome()).getContent());
		assertNull(McpWire.toNeutral(new CallToolResult()).getContent());
	}

	@Test void e08_serverInfo_defaultsAndPartialIdentity() {
		var dflt = McpWire.serverInfo(new McpServerConfig());
		assertEquals(McpRevision.DEFAULT_SERVER_NAME, dflt.getName());
		assertEquals("unknown", dflt.getVersion());
		var nameOnly = McpWire.serverInfo(new McpServerConfig().setName("n"));
		assertEquals("n", nameOnly.getName());
		assertNull(nameOnly.getVersion());
		var versionOnly = McpWire.serverInfo(new McpServerConfig().setVersion("1"));
		assertNull(versionOnly.getName());
		assertEquals("1", versionOnly.getVersion());
	}

	@Test void e09_customContentImplementation_internalError() {
		var content = new Content() {};
		var e = assertThrows(McpException.class, () -> McpWire.toNeutral(content));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, e.getCode());
		assertTrue(e.getMessage().startsWith("Unsupported content type: "), e.getMessage());
	}

	@Test void e10_customResourceContentsImplementation_internalError() {
		var content = new EmbeddedResourceContent().setResource(new ResourceContents() {});
		var e = assertThrows(McpException.class, () -> McpWire.toNeutral(content));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, e.getCode());
		assertTrue(e.getMessage().startsWith("Unsupported resource contents type: "), e.getMessage());
	}
}
