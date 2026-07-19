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
package org.apache.juneau.bean.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Finiteness-invariant tests for the self-referential {@link JsonSchema} bean.
 *
 * <p>
 * {@link JsonSchema} is self-referential ({@code items} → {@code JsonSchema}; {@code properties}/{@code $defs}
 * are {@code Map<String,JsonSchema>}; {@code additionalProperties} is an opaque {@code Object}).  The MCP wire
 * contract treats it as an <b>acyclic-by-wire DAG</b>: schemas are assembled bottom-up (each child is fully
 * built before it is attached to a parent), so there is no supported way to manufacture a cycle over the wire.
 * These tests pin that invariant — {@code toString()} and JSON serialization over a deeply-nested schema
 * terminate finitely (no depth blow-up / infinite recursion).  Mirrors the {@code juneau-marshall} TODO-253
 * {@code HttpPartSchema} deep-nesting finiteness anchor.
 */
class JsonSchema_Finiteness_Test {

	@Test void a01_deepItemsNesting_toStringAndSerializeFinite() {
		var depth = 100;

		// Build an acyclic 'items' chain bottom-up: leaf is a string, each level wraps the previous in an array.
		var node = new JsonSchema().setType("string");
		for (var i = 0; i < depth; i++)
			node = new JsonSchema().setType("array").setItems(node);
		var root = node;

		var str = assertDoesNotThrow(root::toString);
		assertNotNull(str);
		assertFalse(str.isEmpty());

		var json = assertDoesNotThrow(() -> JsonSerializer.DEFAULT.write(root));
		assertNotNull(json);
		assertTrue(json.length() < 1_000_000, () -> "Output should be finite: length=" + json.length());
	}

	@Test void a02_deepPropertyNesting_serializeFinite() {
		var depth = 100;

		// Build an acyclic 'properties' chain bottom-up (each level nests the previous under a single key).
		var node = new JsonSchema().setType("object");
		for (var i = 0; i < depth; i++) {
			var child = node;
			var next = new JsonSchema().setType("object");
			next.setProperties(new LinkedHashMap<>(Map.of("child", child)));
			node = next;
		}
		var root = node;

		var json = assertDoesNotThrow(() -> JsonSerializer.DEFAULT.write(root));
		assertNotNull(json);
		assertTrue(json.length() < 5_000_000, () -> "Output should be finite: length=" + json.length());
	}
}
