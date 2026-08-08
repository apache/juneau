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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@code 2025-06-18} dual-role schema-capability check.
 *
 * <p>
 * This revision's wire schema bean supports exactly six keywords and requires an object root. A
 * neutral {@link McpSchema} can carry anything, so a config whose input OR output schema uses a
 * keyword this revision cannot express — or whose root is not an object — is rejected on first
 * dispatch rather than silently dropping the keyword on the wire.
 */
class McpSchemaCheck_Test {

	private final BeanStore ctx = new BasicBeanStore();

	private static McpToolHandler tool(String name, JsonMap input, JsonMap output) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName(name)
					.setInputSchema(input == null ? null : McpSchema.of(input))
					.setOutputSchema(output == null ? null : McpSchema.of(output));
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx2) {
				return new McpToolOutcome();
			}
		};
	}

	private static McpExchange ping() {
		return new McpExchange(new org.apache.juneau.bean.jsonrpc.JsonRpcRequest().setId(1).setMethod("ping"), n -> null);
	}

	/** Builds a map nested {@code depth} levels deep through supported keys. */
	private static JsonMap nested(int depth) {
		var root = new JsonMap();
		var cur = root;
		for (var i = 1; i < depth; i++) {
			var next = new JsonMap();
			cur.put("x", next);
			cur = next;
		}
		return root;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Unsupported keyword — input role.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_unsupportedTopLevelKeyword_isRejectedNamingToolAndKeyword() {
		var config = new McpServerConfig().addTool(tool("risky", JsonMap.of("type", "object", "oneOf", List.of()), null));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("risky"), e.getMessage());
		assertTrue(e.getMessage().contains("inputSchema"), e.getMessage());
		assertTrue(e.getMessage().contains("oneOf"), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	@Test
	void a02_unsupportedNestedKeyword_isRejected() {
		var nested = JsonMap.of("type", "object", "properties", JsonMap.of("a", JsonMap.of("$ref", "#/$defs/X")));
		var config = new McpServerConfig().addTool(tool("nested", nested, null));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("nested"), e.getMessage());
		assertTrue(e.getMessage().contains("$ref"), e.getMessage());
	}

	@Test
	void a03_everySupportedKeyword_startsClean() {
		var schema = JsonMap.of(
			"type", "object",
			"required", List.of("id"),
			"properties", JsonMap.of("id", JsonMap.of("type", "string")),
			"items", JsonMap.of("type", "string"),
			"additionalProperties", false,
			"$defs", JsonMap.of("IdString", JsonMap.of("type", "string")));
		var config = new McpServerConfig().addTool(tool("ok", schema, null));
		assertNotNull(new McpRevision(null).dispatch(ping(), config, ctx));
	}

	@Test
	void a04_nullSchemasValid_emptySchemaRejected() {
		var okConfig = new McpServerConfig().addTool(tool("noSchema", null, null));
		assertNotNull(new McpRevision(null).dispatch(ping(), okConfig, ctx));

		var badConfig = new McpServerConfig().addTool(tool("emptySchema", new JsonMap(), null));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, badConfig, ctx));
		assertTrue(e.getMessage().contains("emptySchema"), e.getMessage());
		assertTrue(e.getMessage().contains("inputSchema"), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	@Test
	void a05_checkRunsOncePerConfigInstance() {
		// Deliberately two distinct McpRevision instances (C8: no shared INSTANCE anymore) —
		// this also proves VALIDATED is keyed by config identity, not by revision instance identity.
		var config = new McpServerConfig().addTool(tool("ok", JsonMap.of("type", "object"), null));
		assertNotNull(new McpRevision(null).dispatch(ping(), config, ctx));
		assertNotNull(new McpRevision(null).dispatch(ping(), config, ctx));
	}

	@Test
	void a06_validateSchemas_isDirectlyCallable() {
		var bad = new McpServerConfig().addTool(tool("risky", JsonMap.of("type", "object", "enum", List.of("a")), null));
		assertThrows(IllegalArgumentException.class, () -> McpRevision.validateSchemas(bad));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Non-object roots — both roles.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_inputRootNotObject_rejected() {
		var config = new McpServerConfig().addTool(tool("risky", JsonMap.of("type", "string"), null));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("risky"), e.getMessage());
		assertTrue(e.getMessage().contains("inputSchema"), e.getMessage());
		assertTrue(e.getMessage().contains("string"), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	@Test
	void b02_outputRootNotObject_rejected() {
		var config = new McpServerConfig().addTool(tool("risky", JsonMap.of("type", "object"), JsonMap.of("type", "array")));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("risky"), e.getMessage());
		assertTrue(e.getMessage().contains("outputSchema"), e.getMessage());
		assertTrue(e.getMessage().contains("array"), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Complete walk — unsupported keyword reached through every container shape (output role).
	// -----------------------------------------------------------------------------------------------------------------

	private void assertOutputRejects(JsonMap output, String keyword) {
		var config = new McpServerConfig().addTool(tool("risky", JsonMap.of("type", "object"), output));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("risky"), e.getMessage());
		assertTrue(e.getMessage().contains("outputSchema"), e.getMessage());
		assertTrue(e.getMessage().contains(keyword), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	@Test
	void c01_output_root_oneOf() {
		assertOutputRejects(JsonMap.of("type", "object", "oneOf", List.of()), "oneOf");
	}

	@Test
	void c02_output_properties_pattern() {
		assertOutputRejects(JsonMap.of("type", "object", "properties", JsonMap.of("x", JsonMap.of("pattern", "^a$"))), "pattern");
	}

	@Test
	void c03_output_items_minItems() {
		assertOutputRejects(JsonMap.of("type", "object", "items", JsonMap.of("minItems", 1)), "minItems");
	}

	@Test
	void c04_output_defs_ref() {
		assertOutputRejects(JsonMap.of("type", "object", "$defs", JsonMap.of("X", JsonMap.of("$ref", "#/$defs/Y"))), "$ref");
	}

	@Test
	void c05_output_collectionValued_enum() {
		assertOutputRejects(JsonMap.of("type", "object", "items", List.of(JsonMap.of("enum", List.of("a")))), "enum");
	}

	@Test
	void c06_output_additionalProperties_const() {
		assertOutputRejects(JsonMap.of("type", "object", "additionalProperties", JsonMap.of("const", 1)), "const");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Structural safety — cycles, depth, node count.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_cyclicGraph_terminatesCleanly() {
		var root = JsonMap.of("type", "object");
		var props = new JsonMap();
		root.put("properties", props);
		props.put("x", root); // cycle back to root through supported keywords only
		var config = new McpServerConfig().addTool(tool("ok", JsonMap.of("type", "object"), root));
		assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),
			() -> assertNotNull(new McpRevision(null).dispatch(ping(), config, ctx)));
	}

	@Test
	void d02_depth65_rejected() {
		var config = new McpServerConfig().addTool(tool("deep", JsonMap.of("type", "object"), nested(65)));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("nesting depth"), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	@Test
	void d03_nodeCount_rejected() {
		var props = new JsonMap();
		for (var i = 0; i < 10001; i++)
			props.put("x" + i, JsonMap.of("type", "string"));
		var config = new McpServerConfig().addTool(tool("wide", JsonMap.of("type", "object"), JsonMap.of("type", "object", "properties", props)));
		var revision = new McpRevision(null);
		var exchange = ping();
		var e = assertThrows(IllegalArgumentException.class,
			() -> revision.dispatch(exchange, config, ctx));
		assertTrue(e.getMessage().contains("node count"), e.getMessage());
	}

	@Test
	void d04_cleanDualObject_startsClean() {
		var input = JsonMap.of(
			"type", "object",
			"required", List.of("id"),
			"properties", JsonMap.of("id", JsonMap.of("type", "string")),
			"items", JsonMap.of("type", "string"),
			"additionalProperties", false,
			"$defs", JsonMap.of("IdString", JsonMap.of("type", "string")));
		var output = JsonMap.of("type", "object", "properties", JsonMap.of("x", JsonMap.of("type", "integer")));
		var config = new McpServerConfig().addTool(tool("ok", input, output));
		assertNotNull(new McpRevision(null).dispatch(ping(), config, ctx));
	}
}
