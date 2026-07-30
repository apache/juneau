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

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the revision-neutral tool / prompt / resource spec and outcome types.
 */
class McpNeutralModel_Test {

	@Test
	void a01_toolSpec_fluentSetters() {
		var a = McpSchema.of(JsonMap.of("type", "object"));
		var b = McpSchema.of(JsonMap.of("type", "string"));
		var c = new McpToolSpec().setName("echo").setDescription("d").setInputSchema(a).setOutputSchema(b);
		assertEquals("echo", c.getName());
		assertEquals("d", c.getDescription());
		assertSame(a, c.getInputSchema());
		assertSame(b, c.getOutputSchema());
	}

	@Test
	void a02_toolSpec_defaultsAreNull() {
		var a = new McpToolSpec();
		assertNull(a.getName());
		assertNull(a.getDescription());
		assertNull(a.getInputSchema());
		assertNull(a.getOutputSchema());
	}

	@Test
	void b01_toolOutcome_textFactory() {
		var a = McpToolOutcome.text("hi");
		assertEquals(1, a.getContent().size());
		assertEquals("hi", a.getContent().get(0).text());
		assertNull(a.getError());
	}

	@Test
	void b02_toolOutcome_ofFactory() {
		var a = McpToolOutcome.of(McpContentBlock.text("a"), McpContentBlock.image("AAA=", "image/png"));
		assertEquals(2, a.getContent().size());
	}

	@Test
	void b03_toolOutcome_defaultsAreNull_notEmpty() {
		var a = new McpToolOutcome();
		assertNull(a.getContent(), "null content must stay null: an empty list would change the wire bytes");
		assertNull(a.getError());
		assertNull(a.getStructuredContent());
	}

	@Test
	void b04_toolOutcome_errorFlag() {
		assertEquals(Boolean.TRUE, new McpToolOutcome().setError(true).getError());
	}

	@Test
	void b05_toolOutcome_structuredContent_preservesIdentityAndNull() {
		var a = JsonMap.of("x", 1);
		var b = new McpToolOutcome().setStructuredContent(a);
		assertSame(a, b.getStructuredContent());
		assertNull(new McpToolOutcome().getStructuredContent());
	}

	@Test
	void c01_promptFamily_fluentSetters() {
		var a = new McpPromptArgument().setName("who").setDescription("d").setRequired(true);
		var b = new McpPromptSpec().setName("greet").setDescription("pd").setArguments(List.of(a));
		assertEquals("greet", b.getName());
		assertEquals(1, b.getArguments().size());
		assertEquals("who", b.getArguments().get(0).getName());
		assertEquals(Boolean.TRUE, a.getRequired());
	}

	@Test
	void c02_promptOutcome_fluentSetters() {
		var a = new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text("hi"));
		var b = new McpPromptOutcome().setDescription("d").setMessages(List.of(a));
		assertEquals("d", b.getDescription());
		assertEquals(McpRole.USER, b.getMessages().get(0).getRole());
	}

	@Test
	void c03_promptOutcome_defaultsAreNull() {
		var a = new McpPromptOutcome();
		assertNull(a.getDescription());
		assertNull(a.getMessages());
	}

	@Test
	void d01_resourceSpec_hasAllSixFields() {
		var a = new McpResourceSpec().setUri("file:///a").setName("n").setTitle("t")
			.setDescription("d").setMimeType("text/plain").setSize(99L);
		assertEquals("file:///a", a.getUri());
		assertEquals("n", a.getName());
		assertEquals("t", a.getTitle());
		assertEquals("d", a.getDescription());
		assertEquals("text/plain", a.getMimeType());
		assertEquals(Long.valueOf(99L), a.getSize());
	}

	@Test
	void d02_resourceOutcome_fluentSetters() {
		var a = new McpResourceOutcome().setContents(List.of(McpResourceContents.text("u", null, "x")));
		assertEquals(1, a.getContents().size());
		assertNull(new McpResourceOutcome().getContents());
	}
}
