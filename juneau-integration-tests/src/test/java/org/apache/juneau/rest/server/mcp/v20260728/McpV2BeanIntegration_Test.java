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

import java.util.stream.*;

import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-module coverage for the {@code juneau-bean-mcp-2026-07-28} wire beans, exercised from
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
}
