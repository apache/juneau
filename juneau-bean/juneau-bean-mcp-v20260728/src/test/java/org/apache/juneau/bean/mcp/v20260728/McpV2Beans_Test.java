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

	@Test void a01_metadataAndDiscoveryRoundTrip() {
		var meta = new RequestMeta().setProtocolVersion("2026-07-28")
			.setClientInfo(new Implementation().setName("c").setVersion("1"))
			.setClientCapabilities(new ClientCapabilities());
		var json = JsonSerializer.DEFAULT.write(meta);
		assertTrue(json.contains("\"" + RequestMeta.KEY_PROTOCOL_VERSION + "\":\"2026-07-28\""));
		assertTrue(json.contains("\"" + RequestMeta.KEY_CLIENT_INFO + "\":{\"name\":\"c\",\"version\":\"1\"}"));
		assertTrue(json.contains("\"" + RequestMeta.KEY_CLIENT_CAPABILITIES + "\":{}"));
		assertFalse(json.contains("\"protocolVersion\":"));
		assertFalse(json.contains("\"clientInfo\":"));
		assertFalse(json.contains("\"capabilities\":"));
		var result = new ServerDiscoverResult()
			.setMeta(new ResultMeta().setServerInfo(new Implementation().setName("s").setVersion("2")))
			.setCapabilities(new ServerCapabilities().setTools(new ToolCapability()));
		var discoverJson = JsonSerializer.DEFAULT.write(result);
		assertTrue(discoverJson.contains("\"" + ResultMeta.KEY_SERVER_INFO + "\""));
		assertFalse(discoverJson.contains("protocolVersion"));
		assertFalse(discoverJson.toLowerCase(Locale.ROOT).contains("session"));
	}

	@Test void a02_methodsAndProtocolConstants() {
		assertEquals("server/discover", McpMethods.SERVER_DISCOVER);
		assertEquals("2.0", McpProtocol.JSON_RPC_2_0);
		assertEquals("2026-07-28", McpProtocol.VERSION_2026_07_28);
		assertEquals("completion/complete", McpMethods.COMPLETION_COMPLETE);
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

	@Test void a04b_completionCapability_nullableAndEmptySerialization() {
		assertNull(new ServerCapabilities().getCompletions());
		var caps = new ServerCapabilities().setCompletions(new CompletionCapability());
		assertTrue(JsonSerializer.DEFAULT.write(caps).contains("\"completions\":{}"));
	}

	@Test void a05_serverDiscoverResultShapeIsIdentityCapabilitiesAndDiscoveryFields() {
		var declaredFieldNames = Arrays.stream(ServerDiscoverResult.class.getDeclaredFields())
			.map(Field::getName).collect(Collectors.toSet());
		assertEquals(Set.of("supportedVersions", "capabilities", "instructions"), declaredFieldNames);
		var result = new ServerDiscoverResult()
			.setMeta(new ResultMeta().setServerInfo(new Implementation().setName("s").setVersion("1")))
			.setCapabilities(new ServerCapabilities())
			.setSupportedVersions(McpProtocol.VERSION_2026_07_28)
			.setInstructions("call tools/list first");
		var copy = JsonParser.DEFAULT.read(JsonSerializer.DEFAULT.write(result), ServerDiscoverResult.class);
		assertEquals("s", copy.getMeta().getServerInfo().getName());
		assertNotNull(copy.getCapabilities());
		assertEquals(List.of(McpProtocol.VERSION_2026_07_28), copy.getSupportedVersions());
		assertEquals("call tools/list first", copy.getInstructions());
	}

	@Test void a05b_serverDiscoverResult_extendsCacheableResult() {
		assertTrue(CacheableResult.class.isAssignableFrom(ServerDiscoverResult.class));
		assertTrue(Result.class.isAssignableFrom(ServerDiscoverResult.class));
		var result = new ServerDiscoverResult().setSupportedVersions(McpProtocol.VERSION_2026_07_28)
			.setTtlMs(0).setCacheScope(McpCacheScope.PUBLIC);
		assertEquals(0, result.getTtlMs());
		assertEquals(McpCacheScope.PUBLIC, result.getCacheScope());
		assertEquals("complete", result.getResultType());
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
			.addProperties(new JsonSchemaProperty<>("q", JsonType.STRING))
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
			.setIf(new JsonSchema<>().addProperties(new JsonSchemaProperty<>("country").setConst("USA")))
			.setThen(new JsonSchema<>().addProperties(new JsonSchemaProperty<>("postalCode").setPattern("^[0-9]{5}$")))
			.setElse(new JsonSchema<>().addProperties(new JsonSchemaProperty<>("postalCode").setType(JsonType.STRING)));
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

	@Test void b09_resourceTemplateAndListResultRemainSingularAndCacheCapable() {
		assertEquals(Set.of("uriTemplate", "name", "title", "description", "mimeType"),
			Arrays.stream(ResourceTemplate.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet()));
		assertTrue(CacheableResult.class.isAssignableFrom(ListResourceTemplatesResult.class));
	}

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
		assertEquals("{\"completion\":{\"values\":[]},\"resultType\":\"complete\"}", JsonSerializer.DEFAULT.write(result));
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

	@Test void c09_completionCompleteMethodConstant() {
		assertEquals("completion/complete", McpMethods.COMPLETION_COMPLETE);
	}

	@Test void c10_completeRequest_doesNotDuplicateEnvelopeMeta() {
		assertEquals(Set.of("ref", "argument", "context"),
			Arrays.stream(CompleteRequest.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet()));
	}

	static Stream<Class<?>> concreteParamsBeanTypes() {
		return Stream.of(CallToolRequest.class, GetPromptRequest.class, ReadResourceRequest.class,
			CompleteRequest.class, RequestParamsOnly.class);
	}

	@ParameterizedTest
	@MethodSource("concreteParamsBeanTypes")
	void d01_paramsBeans_extendRequestParams(Class<?> type) {
		assertTrue(RequestParams.class.isAssignableFrom(type), () -> type.getName() + " must extend RequestParams");
	}

	@Test void d02_requestParams_metaSetterReturnsConcreteCrtpTypeAtCompileTime() {
		// Compile-time proof: each assignment below only type-checks because setMeta(...) is CRTP-typed to
		// return the concrete subclass, not the RequestParams base type.
		CallToolRequest ctr = new CallToolRequest().setMeta(new RequestMeta());
		GetPromptRequest gpr = new GetPromptRequest().setMeta(new RequestMeta());
		ReadResourceRequest rrr = new ReadResourceRequest().setMeta(new RequestMeta());
		CompleteRequest cr = new CompleteRequest().setMeta(new RequestMeta());
		RequestParamsOnly rpo = new RequestParamsOnly().setMeta(new RequestMeta());
		assertNotNull(ctr.getMeta());
		assertNotNull(gpr.getMeta());
		assertNotNull(rrr.getMeta());
		assertNotNull(cr.getMeta());
		assertNotNull(rpo.getMeta());
	}

	@ParameterizedTest
	@MethodSource("concreteParamsBeanTypes")
	void d03_paramsBeans_nestMetaUnderParamsAsMeta(Class<?> type) throws Exception {
		var meta = new RequestMeta().setProtocolVersion("2026-07-28").setClientCapabilities(new ClientCapabilities());
		var bean = type.getDeclaredConstructor().newInstance();
		type.getMethod("setMeta", RequestMeta.class).invoke(bean, meta);
		var json = JsonSerializer.DEFAULT.write(bean);
		assertTrue(json.contains("\"_meta\":{"), () -> type.getSimpleName() + " must nest metadata under _meta: " + json);
		var copy = JsonParser.DEFAULT.read(json, type);
		var copyMeta = (RequestMeta)type.getMethod("getMeta").invoke(copy);
		assertEquals("2026-07-28", copyMeta.getProtocolVersion());
	}

	@Test void d04_requestParamsOnly_declaresNoMembersBeyondInheritedMeta() {
		assertEquals(Set.of(),
			Arrays.stream(RequestParamsOnly.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet()));
		assertJsonRoundTrip(new RequestParamsOnly().setMeta(new RequestMeta().setProtocolVersion("2026-07-28")),
			RequestParamsOnly.class);
	}

	@Test void d05_callToolRequest_fluentApiUnaffectedByRetrofit() {
		var req = new CallToolRequest().setName("echo").putArgument("x", 1)
			.setMeta(new RequestMeta().setProtocolVersion("2026-07-28").setClientCapabilities(new ClientCapabilities()));
		assertEquals("echo", req.getName());
		assertEquals(Map.of("x", 1), req.getArguments());
		assertEquals("2026-07-28", req.getMeta().getProtocolVersion());
		assertJsonRoundTrip(req, CallToolRequest.class);
	}

	@Test void d06_requestMeta_exactPrefixedKeysAndW3cValuesRoundTrip() {
		var meta = new RequestMeta()
			.setProtocolVersion("2026-07-28")
			.setClientInfo(new Implementation().setName("c").setVersion("1"))
			.setClientCapabilities(new ClientCapabilities())
			.setLogLevel("info")
			.setTraceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
			.setTracestate("congo=t61rcWkgMzE")
			.setBaggage("userId=alice")
			.set("io.example/custom", "y");
		var json = JsonSerializer.DEFAULT.write(meta);
		assertTrue(json.contains("\"" + RequestMeta.KEY_PROTOCOL_VERSION + "\":\"2026-07-28\""));
		assertTrue(json.contains("\"" + RequestMeta.KEY_CLIENT_INFO + "\":"));
		assertTrue(json.contains("\"" + RequestMeta.KEY_CLIENT_CAPABILITIES + "\":{}"));
		assertTrue(json.contains("\"" + RequestMeta.KEY_LOG_LEVEL + "\":\"info\""));
		assertTrue(json.contains("\"traceparent\":\"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01\""));
		assertTrue(json.contains("\"tracestate\":\"congo=t61rcWkgMzE\""));
		assertTrue(json.contains("\"baggage\":\"userId=alice\""));
		assertTrue(json.contains("\"io.example/custom\":\"y\""));
		assertFalse(json.contains("\"protocolVersion\":"));
		assertFalse(json.contains("\"clientInfo\":"));
		assertFalse(json.contains("\"capabilities\":"));
		assertFalse(json.contains("\"logLevel\":"));
		var copy = JsonParser.DEFAULT.read(json, RequestMeta.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("y", copy.get("io.example/custom"));
		assertEquals(Set.of("io.example/custom"), copy.extraKeys());
	}

	@Test void d08_requestMeta_extensionTriplet_noExtensionsAndRepeatedSet() {
		var empty = new RequestMeta();
		assertNull(empty.get("anything"));
		assertEquals(Set.of(), empty.extraKeys());
		var meta = new RequestMeta().set("a", 1).set("b", 2);
		assertEquals(1, meta.get("a"));
		assertEquals(2, meta.get("b"));
		assertEquals(Set.of("a", "b"), meta.extraKeys());
	}

	@Test void d07_requestMeta_clientInfoOptionalAndCapabilitiesRenamed() throws NoSuchMethodException {
		assertNull(new RequestMeta().getClientInfo());
		var method = RequestMeta.class.getMethod("getClientCapabilities");
		assertEquals(ClientCapabilities.class, method.getReturnType());
		assertThrows(NoSuchMethodException.class, () -> RequestMeta.class.getMethod("getCapabilities"));
		assertThrows(NoSuchMethodException.class,
			() -> RequestMeta.class.getMethod("setCapabilities", ClientCapabilities.class));
	}

	@Test void d09_requestMeta_noLongerDeclaresServerInfoKey() {
		assertThrows(NoSuchFieldException.class, () -> RequestMeta.class.getField("KEY_SERVER_INFO"));
	}

	static Stream<Result<?>> tenResultPaths() {
		return Stream.of(
			new ServerDiscoverResult(), new ListToolsResult(), new CallToolResult(), new ListPromptsResult(),
			new GetPromptResult(), new ListResourcesResult(), new ReadResourceResult(),
			new ListResourceTemplatesResult(), new CompleteResult(), new PingResult());
	}

	@ParameterizedTest
	@MethodSource("tenResultPaths")
	void e01_everyResultPath_defaultsToCompleteResultType(Result<?> value) {
		assertEquals("complete", value.getResultType());
		assertTrue(JsonSerializer.DEFAULT.write(value).contains("\"resultType\":\"complete\""),
			() -> value.getClass().getSimpleName() + " must emit resultType:\"complete\"");
	}

	@ParameterizedTest
	@MethodSource("tenResultPaths")
	void e02_everyResultPath_metaRoundTripsAsResultMeta(Result<?> value) throws Exception {
		var setMeta = value.getClass().getMethod("setMeta", ResultMeta.class);
		var getMeta = value.getClass().getMethod("getMeta");
		assertEquals(ResultMeta.class, getMeta.getReturnType());
		setMeta.invoke(value, new ResultMeta().setServerInfo(new Implementation().setName("s").setVersion("1")));
		var json = JsonSerializer.DEFAULT.write(value);
		assertTrue(json.contains("\"_meta\":{\"" + ResultMeta.KEY_SERVER_INFO + "\":"),
			() -> value.getClass().getSimpleName() + " must nest result metadata under _meta: " + json);
		var copy = JsonParser.DEFAULT.read(json, value.getClass());
		var copyMeta = (ResultMeta)getMeta.invoke(copy);
		assertEquals("s", copyMeta.getServerInfo().getName());
	}

	@Test void e03_result_parsesArbitraryResultTypeStringLosslessly() {
		var copy = JsonParser.DEFAULT.read("{\"resultType\":\"input_required\"}", PingResult.class);
		assertEquals("input_required", copy.getResultType());
		assertEquals("{\"resultType\":\"input_required\"}", JsonSerializer.DEFAULT.write(copy));
	}

	@Test void e04_pingResult_isTypedAndDeclaresNoOwnMembers() {
		assertTrue(Result.class.isAssignableFrom(PingResult.class));
		assertEquals(Set.of(),
			Arrays.stream(PingResult.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet()));
		assertEquals("{\"resultType\":\"complete\"}", JsonSerializer.DEFAULT.write(new PingResult()));
	}

	@Test void e05_cacheableResult_extendsResult() {
		assertTrue(Result.class.isAssignableFrom(CacheableResult.class));
	}

	@Test void e06_fluentChains_ownAndInheritedResultSettersCompileAndPreservePayload() {
		var callTool = new CallToolResult().setContent(new TextContent().setText("hi")).setIsError(false)
			.setResultType("complete").setMeta(new ResultMeta());
		assertEquals("hi", ((TextContent)callTool.getContent().get(0)).getText());
		assertNotNull(callTool.getMeta());

		var getPrompt = new GetPromptResult().setDescription("d")
			.setMessages(new PromptMessage().setRole(Role.USER).setContent(new TextContent().setText("hi")))
			.setMeta(new ResultMeta().setServerInfo(new Implementation().setName("s").setVersion("1")));
		assertEquals("d", getPrompt.getDescription());
		assertEquals("s", getPrompt.getMeta().getServerInfo().getName());

		var complete = new CompleteResult().setCompletion(new Completion().setValues(List.of("a")))
			.setResultType("complete");
		assertEquals(List.of("a"), complete.getCompletion().getValues());

		var ping = new PingResult().setMeta(new ResultMeta().setTraceparent("00-tp"));
		assertEquals("00-tp", ping.getMeta().getTraceparent());
	}

	@Test void e07_resultMeta_exactServerInfoKeyAndW3cValuesRoundTrip() {
		var meta = new ResultMeta()
			.setServerInfo(new Implementation().setName("s").setVersion("1"))
			.setTraceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
			.setTracestate("congo=t61rcWkgMzE")
			.setBaggage("userId=alice")
			.set("io.example/custom", "y");
		var json = JsonSerializer.DEFAULT.write(meta);
		assertTrue(json.contains("\"" + ResultMeta.KEY_SERVER_INFO + "\":"));
		assertTrue(json.contains("\"traceparent\":\"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01\""));
		assertTrue(json.contains("\"tracestate\":\"congo=t61rcWkgMzE\""));
		assertTrue(json.contains("\"baggage\":\"userId=alice\""));
		assertTrue(json.contains("\"io.example/custom\":\"y\""));
		assertFalse(json.contains("\"serverInfo\":"));
		var copy = JsonParser.DEFAULT.read(json, ResultMeta.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("y", copy.get("io.example/custom"));
		assertEquals(Set.of("io.example/custom"), copy.extraKeys());
	}

	@Test void e08_resultMeta_extensionTriplet_noExtensionsAndRepeatedSet() {
		var empty = new ResultMeta();
		assertNull(empty.get("anything"));
		assertEquals(Set.of(), empty.extraKeys());
		var meta = new ResultMeta().set("a", 1).set("b", 2);
		assertEquals(1, meta.get("a"));
		assertEquals(2, meta.get("b"));
		assertEquals(Set.of("a", "b"), meta.extraKeys());
	}

	@Test void e09_resultMeta_serverInfoKeyIsExactAndCentralized() {
		assertEquals("io.modelcontextprotocol/serverInfo", ResultMeta.KEY_SERVER_INFO);
	}

	@ParameterizedTest
	@ValueSource(classes = { ClientCapabilities.class, ServerCapabilities.class })
	void e10_capabilities_extensionsRoundTripOrdered(Class<?> type) throws Exception {
		var bean = type.getDeclaredConstructor().newInstance();
		var put = type.getMethod("putExtensions", String.class, Object.class);
		put.invoke(bean, "b", 2);
		put.invoke(bean, "a", 1);
		var json = JsonSerializer.DEFAULT.write(bean);
		assertTrue(json.contains("\"extensions\":{\"b\":2,\"a\":1}"),
			() -> type.getSimpleName() + " must preserve extension insertion order: " + json);
		var copy = JsonParser.DEFAULT.read(json, type);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
	}

	@Test void e11_clientCapabilities_setExtensionsWholesale() {
		var caps = new ClientCapabilities().setExtensions(Map.of("x", 1));
		assertEquals(Map.of("x", 1), caps.getExtensions());
	}

	@Test void e12_serverCapabilities_setExtensionsWholesale() {
		var caps = new ServerCapabilities().setExtensions(Map.of("x", 1));
		assertEquals(Map.of("x", 1), caps.getExtensions());
	}

	@Test void e13_implementation_titleDescriptionIconsRoundTrip() {
		var impl = new Implementation().setName("n").setVersion("1").setTitle("t").setDescription("d")
			.setIcons(new Icon().setSrc("https://example.com/a.png"))
			.addIcons(new Icon().setSrc("https://example.com/b.png"));
		assertEquals(2, impl.getIcons().size());
		assertThrows(UnsupportedOperationException.class, () -> impl.getIcons().add(new Icon().setSrc("x")));
		var json = JsonSerializer.DEFAULT.write(impl);
		var copy = JsonParser.DEFAULT.read(json, Implementation.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("t", copy.getTitle());
		assertEquals("d", copy.getDescription());
		assertEquals("https://example.com/a.png", copy.getIcons().get(0).getSrc());
		assertEquals("https://example.com/b.png", copy.getIcons().get(1).getSrc());
	}

	@Test void e14_implementation_optionalCompletenessFieldsOmittedWhenUnset() {
		var json = JsonSerializer.DEFAULT.write(new Implementation().setName("n").setVersion("1"));
		assertEquals("{\"name\":\"n\",\"version\":\"1\"}", json);
	}

	@Test void e15_icon_allFieldsRoundTrip() {
		var icon = new Icon().setSrc("https://example.com/a.svg").setMimeType("image/svg+xml")
			.setSizes("48x48", "96x96").setTheme(Icon.Theme.DARK);
		var json = JsonSerializer.DEFAULT.write(icon);
		var copy = JsonParser.DEFAULT.read(json, Icon.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("https://example.com/a.svg", copy.getSrc());
		assertEquals("image/svg+xml", copy.getMimeType());
		assertEquals(List.of("48x48", "96x96"), copy.getSizes());
		assertEquals(Icon.Theme.DARK, copy.getTheme());
		assertTrue(json.contains("\"theme\":\"dark\""));
	}

	@Test void e16_icon_themeAddSizesAndRequiredSrcOnly() {
		var icon = new Icon().setSrc("s").addSizes("any").addSizes(List.of("16x16"));
		assertEquals(List.of("any", "16x16"), icon.getSizes());
		assertEquals("{\"sizes\":[\"any\",\"16x16\"],\"src\":\"s\"}", JsonSerializer.DEFAULT.write(icon));
	}

	@Test void e17_icon_themeRestrictedToLightOrDark() {
		assertEquals(Icon.Theme.LIGHT, JsonParser.DEFAULT.read("\"light\"", Icon.Theme.class));
		assertEquals(Icon.Theme.DARK, JsonParser.DEFAULT.read("\"dark\"", Icon.Theme.class));
		assertThrows(Exception.class, () -> JsonParser.DEFAULT.read("\"bogus\"", Icon.Theme.class));
		assertArrayEquals(new Icon.Theme[] { Icon.Theme.LIGHT, Icon.Theme.DARK }, Icon.Theme.values());
	}
}
