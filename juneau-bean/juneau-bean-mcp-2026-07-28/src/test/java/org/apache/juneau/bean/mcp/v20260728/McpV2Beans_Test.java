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
package org.apache.juneau.bean.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Coverage for the MCP 2026-07-28 metadata, discovery, capability, tool-schema, and structured-content wire beans.
 */
class McpV2Beans_Test {

	@Test void a01_metadataAndDiscoveryRoundTrip() {
		var meta = new RequestMeta().setProtocolVersion("2026-07-28")
			.setClientInfo(new Implementation().setName("c").setVersion("1"))
			.setCapabilities(new ClientCapabilities());
		assertEquals(
			"{\"capabilities\":{},\"clientInfo\":{\"name\":\"c\",\"version\":\"1\"},\"protocolVersion\":\"2026-07-28\"}",
			JsonSerializer.DEFAULT.write(meta));
		var result = new ServerDiscoverResult()
			.setServerInfo(new Implementation().setName("s").setVersion("2"))
			.setCapabilities(new ServerCapabilities().setTools(new ToolCapability()));
		var json = JsonSerializer.DEFAULT.write(result);
		assertTrue(json.contains("\"serverInfo\""));
		assertFalse(json.contains("protocolVersion"));
		assertFalse(json.toLowerCase(Locale.ROOT).contains("session"));
	}

	@Test void a02_methodsAndProtocolConstants() {
		assertEquals("server/discover", McpMethods.SERVER_DISCOVER);
		assertEquals("2.0", McpProtocol.JSON_RPC_2_0);
		assertEquals("2026-07-28", McpProtocol.VERSION_2026_07_28);
		var methodFieldNames = Arrays.stream(McpMethods.class.getFields()).map(Field::getName).toList();
		assertFalse(methodFieldNames.contains("INITIALIZE"));
		var protocolFieldNames = Arrays.stream(McpProtocol.class.getFields()).map(Field::getName).toList();
		assertFalse(protocolFieldNames.contains("VERSION_2025_06_18"));
	}

	@Test void a03_noInitializeClasses() {
		for (var name : List.of("InitializeRequest", "InitializeResult")) {
			assertThrows(ClassNotFoundException.class,
				() -> Class.forName("org.apache.juneau.bean.mcp.v20260728." + name));
		}
	}

	@Test void a04_serverCapabilitiesRoundTrip() {
		var caps = new ServerCapabilities()
			.setTools(new ToolCapability().setListChanged(true))
			.setPrompts(new PromptCapability().setListChanged(false))
			.setResources(new ResourceCapability().setSubscribe(true))
			.setLogging(new LoggingCapability().setLevel("info"))
			.setExperimental(JsonMap.of("x", 1));
		var json = JsonSerializer.DEFAULT.write(caps);
		var copy = JsonParser.DEFAULT.read(json, ServerCapabilities.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
	}

	@Test void a05_serverDiscoverResultShapeIsIdentityAndCapabilitiesOnly() {
		var declaredFieldNames = Arrays.stream(ServerDiscoverResult.class.getDeclaredFields())
			.map(Field::getName).collect(Collectors.toSet());
		assertEquals(Set.of("serverInfo", "capabilities"), declaredFieldNames);
		var result = new ServerDiscoverResult()
			.setServerInfo(new Implementation().setName("s").setVersion("1"))
			.setCapabilities(new ServerCapabilities());
		var copy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(result), ServerDiscoverResult.class);
		assertEquals("s", copy.getServerInfo().getName());
		assertNotNull(copy.getCapabilities());
	}

	@ParameterizedTest
	@MethodSource("structuredValues")
	void b01_structuredContent_roundTripsAnyJsonValue(Object value) {
		var json = JsonSerializer.DEFAULT.write(new CallToolResult().setStructuredContent(value));
		var copy = JsonParser.DEFAULT.read(json, CallToolResult.class);
		assertEquals(JsonSerializer.DEFAULT.write(value), JsonSerializer.DEFAULT.write(copy.getStructuredContent()));
	}

	static Stream<Object> structuredValues() {
		return Stream.of(JsonMap.of("x", 1), JsonList.of(1, 2), "text", true, null);
	}

	@Test void b02_toolInputOutputSchema_areJsonSchemaType() throws NoSuchMethodException {
		assertEquals(JsonSchema.class, Tool.class.getMethod("getInputSchema").getReturnType());
		assertEquals(JsonSchema.class, Tool.class.getMethod("getOutputSchema").getReturnType());
		assertEquals(JsonSchema.class, Tool.class.getMethod("setInputSchema", JsonSchema.class).getParameterTypes()[0]);
		assertEquals(JsonSchema.class, Tool.class.getMethod("setOutputSchema", JsonSchema.class).getParameterTypes()[0]);
	}

	@Test void b03_toolInputSchema_objectRootRoundTrip() {
		var schema = new JsonSchema<>()
			.setType(JsonType.OBJECT)
			.addProperties(new JsonSchemaProperty("q", JsonType.STRING))
			.addRequired("q");
		var tool = new Tool().setName("t1").setDescription("d1").setInputSchema(schema);
		var json = JsonSerializer.DEFAULT.write(tool);
		var copy = JsonParser.DEFAULT.read(json, Tool.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals(JsonType.OBJECT, copy.getInputSchema().getTypeAsJsonType());
	}

	@Test void b04_schemaComposition_and_conditional_roundTrip() {
		var schema = new JsonSchema<>()
			.setAllOf(new JsonSchema<>().setType(JsonType.OBJECT))
			.setOneOf(new JsonSchema<>().setType(JsonType.STRING), new JsonSchema<>().setType(JsonType.NUMBER))
			.setIf(new JsonSchema<>().addProperties(new JsonSchemaProperty("country").setConst("USA")))
			.setThen(new JsonSchema<>().addProperties(new JsonSchemaProperty("postalCode").setPattern("^[0-9]{5}$")))
			.setElse(new JsonSchema<>().addProperties(new JsonSchemaProperty("postalCode").setType(JsonType.STRING)));
		var json = JsonSerializer.DEFAULT.write(schema);
		assertTrue(json.contains("\"allOf\""));
		assertTrue(json.contains("\"oneOf\""));
		assertTrue(json.contains("\"if\""));
		assertTrue(json.contains("\"then\""));
		assertTrue(json.contains("\"else\""));
		var copy = JsonParser.DEFAULT.read(json, JsonSchema.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
	}

	@Test void b05_schemaExactKeyNames_liveRoundTrip() {
		var schema = new JsonSchema<>()
			.setComment("note")
			.setIdUri("https://example.com/s.json")
			.setSchemaVersionUri("https://json-schema.org/draft/2020-12/schema")
			.addDef("Id", new JsonSchema<>().setType(JsonType.STRING))
			.addProperties(new JsonSchemaProperty("id").setRef("#/$defs/Id"));
		var json = JsonSerializer.DEFAULT.write(schema);
		assertTrue(json.contains("\"$comment\":\"note\""));
		assertTrue(json.contains("\"$id\":\"https://example.com/s.json\""));
		assertTrue(json.contains("\"$schema\":\"https://json-schema.org/draft/2020-12/schema\""));
		assertTrue(json.contains("\"$defs\":"));
		assertTrue(json.contains("\"$ref\":\"#/$defs/Id\""));
		var copy = JsonParser.DEFAULT.read(json, JsonSchema.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
	}

	@Test void b06_resourceTemplate_exactShapeAndOmission() {
		var value = new ResourceTemplate().setUriTemplate("file:///{name}").setName("n");
		assertEquals("{\"name\":\"n\",\"uriTemplate\":\"file:///{name}\"}", org.apache.juneau.marshall.marshaller.Json.of(value));
		var all = value.setTitle("t").setDescription("d").setMimeType("text/plain");
		var copy = JsonParser.DEFAULT.read(org.apache.juneau.marshall.marshaller.Json.of(all), ResourceTemplate.class);
		assertEquals("file:///{name}", copy.getUriTemplate());
		assertEquals("n", copy.getName());
		assertEquals("t", copy.getTitle());
		assertEquals("d", copy.getDescription());
		assertEquals("text/plain", copy.getMimeType());
		assertEquals(Set.of("uriTemplate", "name", "title", "description", "mimeType"),
			Arrays.stream(ResourceTemplate.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet()));
	}

	@Test void b07_listTemplateResult_contract() {
		var a = new ResourceTemplate().setUriTemplate("a").setName("a");
		var b = new ResourceTemplate().setUriTemplate("b").setName("b");
		var result = new ListResourceTemplatesResult().setResourceTemplates(a).addResourceTemplates(b)
			.setNextCursor("2").setTtlMs(0).setCacheScope(McpCacheScope.PUBLIC);
		assertEquals(2, result.getResourceTemplates().size());
		assertThrows(UnsupportedOperationException.class, () -> result.getResourceTemplates().add(a));
		assertEquals("2", result.getNextCursor());
		assertTrue(org.apache.juneau.marshall.marshaller.Json.of(result).contains("\"resourceTemplates\""));
		assertFalse(org.apache.juneau.marshall.marshaller.Json.of(result).contains("\"templates\""));
		var copy = JsonParser.DEFAULT.read(org.apache.juneau.marshall.marshaller.Json.of(result), ListResourceTemplatesResult.class);
		assertEquals(org.apache.juneau.marshall.marshaller.Json.of(result), org.apache.juneau.marshall.marshaller.Json.of(copy));
	}

	@Test void b08_methodConstant() {
		assertEquals("resources/templates/list", McpMethods.RESOURCES_TEMPLATES_LIST);
	}
}
