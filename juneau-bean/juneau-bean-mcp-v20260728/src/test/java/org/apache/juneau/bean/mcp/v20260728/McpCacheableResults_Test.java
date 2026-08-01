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

import java.util.stream.*;

import org.apache.juneau.marshall.json.JsonParser;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Coverage for the {@code 2026-07-28} SEP-2549 cache-hint carriers: {@link McpCacheScope} and the
 * {@link CacheableResult} base shared by every cacheable v2 list/read result.
 */
class McpCacheableResults_Test {

	private static Stream<CacheableResult<?>> results() {
		return Stream.of(new ListToolsResult(), new ListPromptsResult(),
			new ListResourcesResult(), new ReadResourceResult(), new ListResourceTemplatesResult());
	}

	@ParameterizedTest @MethodSource("results")
	void a01_defaultsOmitBothFields(CacheableResult<?> value) {
		assertNull(value.getTtlMs());
		assertNull(value.getCacheScope());
		assertFalse(Json.of(value).contains("ttlMs"));
		assertFalse(Json.of(value).contains("cacheScope"));
	}

	@ParameterizedTest @MethodSource("results")
	void a02_inheritedFieldsRoundTrip(CacheableResult<?> value) {
		value.setTtlMs(0).setCacheScope(McpCacheScope.PRIVATE);
		var json = Json.of(value);
		assertTrue(json.contains("\"ttlMs\":0"));
		assertTrue(json.contains("\"cacheScope\":\"private\""));
		var copy = JsonParser.DEFAULT.read(json, value.getClass());
		assertEquals(0, ((CacheableResult<?>)copy).getTtlMs());
		assertEquals(McpCacheScope.PRIVATE, ((CacheableResult<?>)copy).getCacheScope());
	}

	@Test void a03_scopeUsesExactLowercaseWireTokens() {
		assertEquals("\"public\"", Json.of(McpCacheScope.PUBLIC));
		assertEquals("\"private\"", Json.of(McpCacheScope.PRIVATE));
		assertEquals(McpCacheScope.PUBLIC, JsonParser.DEFAULT.read("\"public\"", McpCacheScope.class));
		assertEquals(McpCacheScope.PRIVATE, JsonParser.DEFAULT.read("\"private\"", McpCacheScope.class));
		assertThrows(Exception.class, () -> JsonParser.DEFAULT.read("\"bogus\"", McpCacheScope.class));
		assertArrayEquals(new McpCacheScope[] { McpCacheScope.PUBLIC, McpCacheScope.PRIVATE }, McpCacheScope.values());
	}

	@Test void a04_concreteFluentChainsCompileAndPreservePayload() {
		var tools = new ListToolsResult().setTtlMs(5).setCacheScope(McpCacheScope.PUBLIC)
			.setTools(new Tool().setName("t")).setNextCursor("n");
		var read = new ReadResourceResult().setCacheScope(McpCacheScope.PRIVATE).setTtlMs(7)
			.setContents(new TextResourceContents().setUri("file:///a").setText("x"));
		assertEquals("t", tools.getTools().get(0).getName());
		assertEquals("n", tools.getNextCursor());
		assertEquals("x", ((TextResourceContents)read.getContents().get(0)).getText());
	}

	@Test void a05_wireBeanPreservesNegativeParsedTtl() {
		var value = JsonParser.DEFAULT.read("{\"ttlMs\":-1,\"tools\":[]}", ListToolsResult.class);
		assertEquals(-1, value.getTtlMs());
		assertEquals("complete", value.getResultType());
		assertEquals("{\"resultType\":\"complete\",\"tools\":[],\"ttlMs\":-1}", Json.of(value));
	}

	@Test void a06_payloadPlusInheritedResultType_listToolsResult() {
		var value = new ListToolsResult().setTools(new Tool().setName("t"));
		var json = Json.of(value);
		assertEquals("{\"resultType\":\"complete\",\"tools\":[{\"name\":\"t\"}]}", json);
		var copy = JsonParser.DEFAULT.read(json, ListToolsResult.class);
		assertEquals(json, Json.of(copy));
		assertFalse(json.contains("ttlMs"));
		assertFalse(json.contains("cacheScope"));
	}

	@Test void a07_payloadPlusInheritedResultType_listPromptsResult() {
		var value = new ListPromptsResult().setPrompts(new Prompt().setName("p"));
		var json = Json.of(value);
		assertEquals("{\"prompts\":[{\"name\":\"p\"}],\"resultType\":\"complete\"}", json);
		var copy = JsonParser.DEFAULT.read(json, ListPromptsResult.class);
		assertEquals(json, Json.of(copy));
		assertFalse(json.contains("ttlMs"));
		assertFalse(json.contains("cacheScope"));
	}

	@Test void a08_payloadPlusInheritedResultType_listResourcesResult() {
		var value = new ListResourcesResult().setResources(new Resource().setUri("file:///a"));
		var json = Json.of(value);
		assertEquals("{\"resources\":[{\"uri\":\"file:///a\"}],\"resultType\":\"complete\"}", json);
		var copy = JsonParser.DEFAULT.read(json, ListResourcesResult.class);
		assertEquals(json, Json.of(copy));
		assertFalse(json.contains("ttlMs"));
		assertFalse(json.contains("cacheScope"));
	}

	@Test void a09_payloadPlusInheritedResultType_readResourceResult() {
		var value = new ReadResourceResult().setContents(new TextResourceContents().setUri("file:///a").setText("x"));
		var json = Json.of(value);
		assertEquals("{\"contents\":[{\"text\":\"x\",\"uri\":\"file:///a\"}],\"resultType\":\"complete\"}", json);
		assertFalse(json.contains("ttlMs"));
		assertFalse(json.contains("cacheScope"));
	}

	@ParameterizedTest @MethodSource("results")
	void a10_cacheableResults_extendResult(CacheableResult<?> value) {
		assertTrue(Result.class.isAssignableFrom(value.getClass()));
		assertEquals("complete", value.getResultType());
	}

	@Test void a11_cacheableResult_classExtendsResult() {
		assertTrue(Result.class.isAssignableFrom(CacheableResult.class));
	}

	@Test void a12_fluentChain_mixesOwnCacheAndInheritedResultSetters() {
		var value = new ListToolsResult()
			.setTools(new Tool().setName("t"))
			.setTtlMs(5)
			.setCacheScope(McpCacheScope.PUBLIC)
			.setResultType("complete")
			.setMeta(new ResultMeta().setServerInfo(new Implementation().setName("s").setVersion("1")));
		assertEquals("t", value.getTools().get(0).getName());
		assertEquals(5, value.getTtlMs());
		assertEquals(McpCacheScope.PUBLIC, value.getCacheScope());
		assertEquals("complete", value.getResultType());
		assertEquals("s", value.getMeta().getServerInfo().getName());
	}
}
