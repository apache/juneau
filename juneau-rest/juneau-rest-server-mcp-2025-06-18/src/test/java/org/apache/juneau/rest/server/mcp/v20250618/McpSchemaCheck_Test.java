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
 * Coverage for the {@code 2025-06-18} schema-capability check.
 *
 * <p>
 * This revision's wire schema bean supports exactly six keywords. A neutral {@link McpSchema} can
 * carry anything, so a config using a keyword this revision cannot express is rejected on first
 * dispatch rather than silently dropping the keyword on the wire.
 */
class McpSchemaCheck_Test {

	private final BeanStore ctx = new BasicBeanStore();

	private static McpToolHandler tool(String name, JsonMap schema) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName(name).setInputSchema(schema == null ? null : McpSchema.of(schema));
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx2) { return new McpToolOutcome(); }
		};
	}

	private static McpExchange ping() {
		return new McpExchange(new org.apache.juneau.bean.jsonrpc.JsonRpcRequest().setId(1).setMethod("ping"), n -> null);
	}

	@Test
	void a01_unsupportedTopLevelKeyword_isRejectedNamingToolAndKeyword() {
		var config = new McpServerConfig().addTool(tool("risky", JsonMap.of("type", "object", "oneOf", List.of())));
		var e = assertThrows(IllegalArgumentException.class,
			() -> new Mcp20250618Revision(null).dispatch(ping(), config, ctx));
		assertTrue(e.getMessage().contains("risky"), e.getMessage());
		assertTrue(e.getMessage().contains("oneOf"), e.getMessage());
		assertTrue(e.getMessage().contains("2025-06-18"), e.getMessage());
	}

	@Test
	void a02_unsupportedNestedKeyword_isRejected() {
		var nested = JsonMap.of("type", "object", "properties", JsonMap.of("a", JsonMap.of("$ref", "#/$defs/X")));
		var config = new McpServerConfig().addTool(tool("nested", nested));
		var e = assertThrows(IllegalArgumentException.class,
			() -> new Mcp20250618Revision(null).dispatch(ping(), config, ctx));
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
		var config = new McpServerConfig().addTool(tool("ok", schema));
		assertNotNull(new Mcp20250618Revision(null).dispatch(ping(), config, ctx));
	}

	@Test
	void a04_nullAndEmptySchemas_startClean() {
		var config = new McpServerConfig().addTool(tool("noSchema", null)).addTool(tool("emptySchema", new JsonMap()));
		assertNotNull(new Mcp20250618Revision(null).dispatch(ping(), config, ctx));
	}

	@Test
	void a05_checkRunsOncePerConfigInstance() {
		// Deliberately two distinct Mcp20250618Revision instances (C8: no shared INSTANCE anymore) —
		// this also proves VALIDATED is keyed by config identity, not by revision instance identity.
		var config = new McpServerConfig().addTool(tool("ok", JsonMap.of("type", "object")));
		assertNotNull(new Mcp20250618Revision(null).dispatch(ping(), config, ctx));
		assertNotNull(new Mcp20250618Revision(null).dispatch(ping(), config, ctx));
	}

	@Test
	void a06_validateSchemas_isDirectlyCallable() {
		var bad = new McpServerConfig().addTool(tool("risky", JsonMap.of("enum", List.of("a"))));
		assertThrows(IllegalArgumentException.class, () -> Mcp20250618Revision.validateSchemas(bad));
	}
}
