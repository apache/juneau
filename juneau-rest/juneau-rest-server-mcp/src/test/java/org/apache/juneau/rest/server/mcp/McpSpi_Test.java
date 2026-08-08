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
package org.apache.juneau.rest.server.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the revision SPI surface: {@link McpErrorKind}, {@link McpExchange},
 * {@link McpRevision}, {@link McpParamUtils}, and {@link McpCursor#cursorOf(Object)}.
 */
class McpSpi_Test {

	@Test
	void a01_errorKind_hasEightConstants() {
		assertEquals(8, McpErrorKind.values().length);
		assertNotNull(McpErrorKind.valueOf("INVALID_REQUEST"));
		assertNotNull(McpErrorKind.valueOf("UNKNOWN_METHOD"));
		assertNotNull(McpErrorKind.valueOf("TOOL_NOT_FOUND"));
		assertNotNull(McpErrorKind.valueOf("PROMPT_NOT_FOUND"));
		assertNotNull(McpErrorKind.valueOf("RESOURCE_NOT_FOUND"));
		assertNotNull(McpErrorKind.valueOf("INVALID_PARAMS"));
		assertNotNull(McpErrorKind.valueOf("INTERNAL_ERROR"));
		assertNotNull(McpErrorKind.valueOf("PARSE_ERROR"));
	}

	@Test
	void b01_exchange_exposesRequestAndHeaders() {
		var a = new JsonRpcRequest().setMethod("ping");
		var b = new McpExchange(a, n -> "Mcp-Method".equals(n) ? "tools/call" : null);
		assertSame(a, b.request());
		assertEquals("tools/call", b.header("Mcp-Method"));
		assertNull(b.header("Missing"));
	}

	@Test
	void b02_exchange_allowsNullRequest() {
		assertNull(new McpExchange(null, n -> null).request());
	}

	@Test
	void b03_exchange_rejectsNullHeaderLookup() {
		assertThrows(IllegalArgumentException.class, () -> new McpExchange(null, null));
	}

	@Test
	void c01_paramUtils_asMap() {
		assertTrue(McpParamUtils.asMap(null).isEmpty());
		assertEquals("v", McpParamUtils.asMap(JsonMap.of("k", "v")).get("k"));
		var e = assertThrows(McpException.class, () -> McpParamUtils.asMap("not-a-map"));
		assertEquals(-32602, e.getCode());
		assertEquals("Params must be an object", e.getMessage());
	}

	@Test
	void c02_paramUtils_strParam() {
		assertNull(McpParamUtils.strParam(Map.of(), "name"));
		assertEquals("x", McpParamUtils.strParam(Map.of("name", "x"), "name"));
		assertEquals("7", McpParamUtils.strParam(Map.of("name", 7), "name"));
	}

	@Test
	void c03_paramUtils_mapParam() {
		assertTrue(McpParamUtils.mapParam(Map.of(), "arguments").isEmpty());
		assertEquals("v", McpParamUtils.mapParam(Map.of("arguments", JsonMap.of("k", "v")), "arguments").get("k"));
		Map<String,Object> badParams = Map.of("arguments", "nope");
		var e = assertThrows(McpException.class, () -> McpParamUtils.mapParam(badParams, "arguments"));
		assertEquals(-32602, e.getCode());
		assertEquals("Param 'arguments' must be an object", e.getMessage());
	}

	@Test
	void c04a_paramUtils_strictStrParam() {
		assertNull(McpParamUtils.strictStrParam(Map.of(), "value"));
		assertEquals("x", McpParamUtils.strictStrParam(Map.of("value", "x"), "value"));
		Map<String,Object> numericValue = Map.of("value", 7);
		var e1 = assertThrows(McpException.class, () -> McpParamUtils.strictStrParam(numericValue, "value"));
		assertEquals(-32602, e1.getCode());
		assertEquals("Param 'value' must be a string", e1.getMessage());
		Map<String,Object> booleanValue = Map.of("value", true);
		var e2 = assertThrows(McpException.class, () -> McpParamUtils.strictStrParam(booleanValue, "value"));
		assertEquals(-32602, e2.getCode());
		assertEquals("Param 'value' must be a string", e2.getMessage());
	}

	@Test
	void c04b_paramUtils_strictStrMapParam() {
		assertTrue(McpParamUtils.strictStrMapParam(Map.of(), "arguments").isEmpty());
		var result = McpParamUtils.strictStrMapParam(Map.of("arguments", JsonMap.of("k", "v")), "arguments");
		assertEquals("v", result.get("k"));
		Map<String,Object> nonObjectArguments = Map.of("arguments", "nope");
		var e1 = assertThrows(McpException.class, () -> McpParamUtils.strictStrMapParam(nonObjectArguments, "arguments"));
		assertEquals(-32602, e1.getCode());
		assertEquals("Param 'arguments' must be an object", e1.getMessage());
		Map<String,Object> nonStringValues = Map.of("arguments", JsonMap.of("k", 7));
		var e2 = assertThrows(McpException.class,
			() -> McpParamUtils.strictStrMapParam(nonStringValues, "arguments"));
		assertEquals(-32602, e2.getCode());
		assertEquals("Param 'arguments' values must be strings", e2.getMessage());
	}

	@Test
	void c04_paramUtils_constructorIsPrivate() {
		assertDoesNotThrow(() -> {
			var ctor = McpParamUtils.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			assertNotNull(ctor.newInstance());
		});
	}

	@Test
	void d01_cursorOf_extractsCursorParam() {
		assertNull(McpCursor.cursorOf(null));
		assertNull(McpCursor.cursorOf(JsonMap.of("other", "x")));
		assertEquals("3", McpCursor.cursorOf(JsonMap.of("cursor", "3")));
		assertThrows(McpException.class, () -> McpCursor.cursorOf("not-a-map"));
	}
}
