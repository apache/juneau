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

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the revision-neutral value types: {@link McpSchema}, {@link McpRole},
 * {@link McpContentBlock}, {@link McpResourceContents}.
 */
class McpNeutralValues_Test {

	@Test
	void a01_schema_carriesRawMapThrough() {
		var a = JsonMap.of("type", "object");
		var b = McpSchema.of(a);
		assertSame(a, b.toJsonMap());
	}

	@Test
	void a02_schema_nullBecomesEmptyMap() {
		assertTrue(McpSchema.of(null).toJsonMap().isEmpty());
	}

	@Test
	void b01_role_hasFourValuesWithLowercaseWire() {
		assertEquals(4, McpRole.values().length);
		assertEquals("user", McpRole.USER.toWire());
		assertEquals("assistant", McpRole.ASSISTANT.toWire());
		assertEquals("system", McpRole.SYSTEM.toWire());
		assertEquals("tool", McpRole.TOOL.toWire());
		for (var a : McpRole.values())
			assertEquals(a.toWire(), a.toString());
	}

	@Test
	void c01_resourceContents_text() {
		var a = McpResourceContents.text("file:///a", "text/plain", "body");
		assertEquals(McpResourceContents.Kind.TEXT, a.kind());
		assertEquals("file:///a", a.uri());
		assertEquals("text/plain", a.mimeType());
		assertEquals("body", a.text());
		assertNull(a.blob());
	}

	@Test
	void c02_resourceContents_blob() {
		var a = McpResourceContents.blob("file:///b", "application/octet-stream", "Qk09");
		assertEquals(McpResourceContents.Kind.BLOB, a.kind());
		assertEquals("Qk09", a.blob());
		assertNull(a.text());
	}

	@Test
	void d01_contentBlock_text() {
		var a = McpContentBlock.text("hi");
		assertEquals(McpContentBlock.Kind.TEXT, a.kind());
		assertEquals("hi", a.text());
		assertNull(a.data());
		assertNull(a.mimeType());
		assertNull(a.resource());
	}

	@Test
	void d02_contentBlock_image() {
		var a = McpContentBlock.image("AAA=", "image/png");
		assertEquals(McpContentBlock.Kind.IMAGE, a.kind());
		assertEquals("AAA=", a.data());
		assertEquals("image/png", a.mimeType());
		assertNull(a.text());
	}

	@Test
	void d03_contentBlock_resource() {
		var a = McpResourceContents.text("file:///a", null, "x");
		var b = McpContentBlock.resource(a);
		assertEquals(McpContentBlock.Kind.RESOURCE, b.kind());
		assertSame(a, b.resource());
		assertNull(b.text());
	}
}
