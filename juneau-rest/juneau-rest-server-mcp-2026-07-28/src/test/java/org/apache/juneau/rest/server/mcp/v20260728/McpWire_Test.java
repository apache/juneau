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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpContentBlock;
import org.apache.juneau.rest.server.mcp.McpPromptArgument;
import org.apache.juneau.rest.server.mcp.McpPromptMessage;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceContents;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpRole;
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@code 2026-07-28} {@link McpWire} neutral-to-wire boundary.
 */
class McpWire_Test {

	// -------- opaque _meta -> RequestMeta ---------

	@Test
	void a01_requestMeta_opaqueMap_roundTrips() {
		var meta = McpWire.requestMeta(JsonMap.of(
			"protocolVersion", "2026-07-28",
			"clientInfo", JsonMap.of("name", "c", "version", "1"),
			"capabilities", JsonMap.of()));
		assertEquals("2026-07-28", meta.getProtocolVersion());
		assertEquals("c", meta.getClientInfo().getName());
		assertEquals("1", meta.getClientInfo().getVersion());
		assertNotNull(meta.getCapabilities());
	}

	@Test
	void a02_requestMeta_scalar_rejected() {
		var e = assertThrows(McpException.class, () -> McpWire.requestMeta("x"));
		assertEquals(McpRevision.CODE_INVALID_REQUEST, e.getCode());
		assertEquals("Request _meta must be an object", e.getMessage());
	}

	@Test
	void a03_requestMeta_array_rejected() {
		var e = assertThrows(McpException.class, () -> McpWire.requestMeta(JsonList.of(1, 2, 3)));
		assertEquals(McpRevision.CODE_INVALID_REQUEST, e.getCode());
		assertEquals("Request _meta must be an object", e.getMessage());
	}

	@Test
	void a04_requestMeta_null_rejected() {
		var e = assertThrows(McpException.class, () -> McpWire.requestMeta(null));
		assertEquals(McpRevision.CODE_INVALID_REQUEST, e.getCode());
	}

	// -------- server identity ---------

	@Test
	void b01_serverInfo_fallsBackToDefaults() {
		var info = McpWire.serverInfo(new McpServerConfig());
		assertEquals(McpRevision.DEFAULT_SERVER_NAME, info.getName());
		assertEquals("unknown", info.getVersion());
	}

	@Test
	void b02_serverInfo_usesExplicitIdentity() {
		var info = McpWire.serverInfo(new McpServerConfig().setName("srv").setVersion("9.9"));
		assertEquals("srv", info.getName());
		assertEquals("9.9", info.getVersion());
	}

	// -------- tool descriptor + schema ---------

	@Test
	void c01_toWire_toolSpec_mapsBothSchemasThroughWireSubtype() {
		var a = McpSchema.of(JsonMap.of("type", "object"));
		var b = McpSchema.of(JsonMap.of("$defs", JsonMap.of("X", JsonMap.of("type", "string")), "$id", "urn:test", "type", "object"));
		var c = McpWire.toWire(new McpToolSpec().setName("echo").setDescription("d").setInputSchema(a).setOutputSchema(b));
		assertNotNull(c.getInputSchema());
		assertNotNull(c.getOutputSchema());
		var d = Json.of(c.getOutputSchema());
		assertTrue(d.contains("\"$defs\""));
		assertTrue(d.contains("\"$id\""));
		assertFalse(d.contains("\"definitions\""));
		assertFalse(d.contains("\"id\""));
	}

	@Test
	void c02_toWire_toolSpec_null_returnsNull() {
		assertNull(McpWire.toWire((McpToolSpec) null));
	}

	@Test
	void c03_toWire_schema_mapsToJsonSchema_nullSafe() {
		assertNull(McpWire.toWire((McpSchema) null));
		var s = McpWire.toWire(McpSchema.of(JsonMap.of("type", "string")));
		assertNotNull(s);
		assertTrue(Json.of(s).contains("\"type\":\"string\""));
	}

	// -------- tool outcome ---------

	@Test
	void d01_toWire_toolOutcome_mapsStructuredContentIdentity() {
		var a = JsonMap.of("x", 1);
		var b = McpWire.toWire(new McpToolOutcome()
			.setContent(List.of(McpContentBlock.text("{\"x\":1}")))
			.setStructuredContent(a));
		assertSame(a, b.getStructuredContent());
		assertEquals(1, b.getContent().size());
	}

	@Test
	void d02_toWire_toolOutcome_nullAndEmptyPreserved() {
		assertNull(McpWire.toWire((McpToolOutcome) null));
		assertEquals("{}", Json.of(McpWire.toWire(new McpToolOutcome())));
	}

	// -------- content + resource-contents variants ---------

	@Test
	void e01_toWire_content_variants() {
		var text = (TextContent) McpWire.toWire(McpContentBlock.text("hi"));
		assertEquals("hi", text.getText());
		var image = (ImageContent) McpWire.toWire(McpContentBlock.image("ZGF0YQ==", "image/png"));
		assertEquals("ZGF0YQ==", image.getData());
		assertEquals("image/png", image.getMimeType());
		var embedded = (EmbeddedResourceContent) McpWire.toWire(McpContentBlock.resource(McpResourceContents.text("file://a", "text/plain", "body")));
		assertNotNull(embedded.getResource());
	}

	@Test
	void e02_toWire_resourceContents_variants() {
		var text = (TextResourceContents) McpWire.toWire(McpResourceContents.text("file://a", "text/plain", "body"));
		assertEquals("file://a", text.getUri());
		assertEquals("body", text.getText());
		var blob = (BlobResourceContents) McpWire.toWire(McpResourceContents.blob("file://b", "application/octet-stream", "QUJD"));
		assertEquals("file://b", blob.getUri());
		assertEquals("QUJD", blob.getBlob());
	}

	// -------- prompt family ---------

	@Test
	void f01_toWire_promptFamily() {
		var arg = new McpPromptArgument().setName("a").setDescription("ad").setRequired(true);
		var prompt = McpWire.toWire(new McpPromptSpec().setName("p").setDescription("pd").setArguments(List.of(arg)));
		assertEquals("p", prompt.getName());
		assertEquals(1, prompt.getArguments().size());
		assertEquals("a", prompt.getArguments().get(0).getName());

		var message = new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text("hey"));
		var outcome = McpWire.toWire(new McpPromptOutcome().setDescription("od").setMessages(List.of(message)));
		assertEquals("od", outcome.getDescription());
		assertEquals(1, outcome.getMessages().size());
		assertEquals(Role.USER, outcome.getMessages().get(0).getRole());
	}

	// -------- resource family ---------

	@Test
	void f02_toWire_resourceFamily() {
		var resource = McpWire.toWire(new McpResourceSpec().setUri("file://a").setName("res").setTitle("t").setDescription("d").setMimeType("text/plain"));
		assertEquals("file://a", resource.getUri());
		assertEquals("res", resource.getName());

		var outcome = McpWire.toWire(new McpResourceOutcome().setContents(List.of(McpResourceContents.text("file://a", "text/plain", "x"))));
		assertEquals(1, outcome.getContents().size());
	}

	// -------- discovery ---------

	@Test
	void g01_discover_buildsResult() {
		var config = new McpServerConfig().setName("srv").setVersion("2.0");
		var capabilities = new ServerCapabilities().setTools(new ToolCapability());
		var result = McpWire.discover(config, capabilities);
		assertEquals("srv", result.getServerInfo().getName());
		assertEquals("2.0", result.getServerInfo().getVersion());
		assertNotNull(result.getCapabilities().getTools());
	}
}
