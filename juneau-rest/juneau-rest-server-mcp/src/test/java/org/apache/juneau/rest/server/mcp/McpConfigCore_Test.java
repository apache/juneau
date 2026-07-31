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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the revision-neutral {@link McpServerConfig}.
 */
class McpConfigCore_Test {

	private static McpToolHandler dummyTool(String name) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return new McpToolOutcome(); }
		};
	}

	private static McpPromptHandler dummyPrompt(String name) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
		};
	}

	private static McpResourceHandler dummyResource(String uri) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return new McpResourceOutcome(); }
		};
	}

	@Test
	void a01_defaults() {
		var a = new McpServerConfig();
		assertNull(a.getName());
		assertNull(a.getVersion());
		assertNull(a.getInstructions());
		assertNotNull(a.getTools());
		assertNotNull(a.getPrompts());
		assertNotNull(a.getResources());
		assertNotNull(a.getResourceTemplates());
		assertSame(McpCursor.SINGLE_PAGE, a.getCursor());
	}

	@Test
	void a02_setters_and_addCalls() {
		var a = new McpServerConfig()
			.setName("x")
			.setVersion("1")
			.setInstructions("hello")
			.setCursor(McpCursor.fixedSize(5))
			.addTool(dummyTool("t1"), dummyTool("t2"))
			.addPrompt(dummyPrompt("p1"))
			.addResource(dummyResource("r://a"))
			.addResourceTemplate(new McpResourceTemplateSpec().setName("rt1"));

		assertString("x", a.getName());
		assertString("1", a.getVersion());
		assertString("hello", a.getInstructions());
		assertNotSame(McpCursor.SINGLE_PAGE, a.getCursor());
		assertSize(2, a.getTools());
		assertSize(1, a.getPrompts());
		assertSize(1, a.getResources());
		assertSize(1, a.getResourceTemplates());
	}

	@Test
	void a03_setLists_replacingAndNullClears() {
		var a = new McpServerConfig().addTool(dummyTool("t")).addPrompt(dummyPrompt("p")).addResource(dummyResource("r"))
			.addResourceTemplate(new McpResourceTemplateSpec().setName("rt"));
		a.setTools(null);
		a.setPrompts(null);
		a.setResources(null);
		a.setResourceTemplates(null);
		assertEmpty(a.getTools());
		assertEmpty(a.getPrompts());
		assertEmpty(a.getResources());
		assertEmpty(a.getResourceTemplates());

		a.setTools(List.of(dummyTool("a")));
		a.setPrompts(List.of(dummyPrompt("a")));
		a.setResources(List.of(dummyResource("a")));
		a.setResourceTemplates(List.of(new McpResourceTemplateSpec().setName("a")));
		assertSize(1, a.getTools());
		assertSize(1, a.getPrompts());
		assertSize(1, a.getResources());
		assertSize(1, a.getResourceTemplates());
	}

	@Test
	void a04_setCursor_nullResets() {
		var a = new McpServerConfig().setCursor(McpCursor.fixedSize(2));
		a.setCursor(null);
		assertSame(McpCursor.SINGLE_PAGE, a.getCursor());
	}

	@Test
	void a05_noRevisionSpecificSurfaceRemains() {
		var m = Arrays.stream(McpServerConfig.class.getMethods()).map(java.lang.reflect.Method::getName).toList();
		assertFalse(m.contains("getCapabilities"), "capabilities are revision-owned; core must hold none");
		assertFalse(m.contains("setCapabilities"), "capabilities are revision-owned; core must hold none");
		assertFalse(m.contains("getProtocolVersion"), "the protocol version is owned by McpRevision.protocolVersion()");
		assertFalse(m.contains("setProtocolVersion"), "the protocol version is owned by McpRevision.protocolVersion()");
		assertFalse(m.contains("getServerInfo"), "server identity is plain name/version on the neutral config");
	}

	@Test
	void a06_nullListElementsAreDistinctFromEmpty() {
		assertNull(new McpToolOutcome().getContent());
		assertNull(new McpPromptOutcome().getMessages());
		assertNull(new McpResourceOutcome().getContents());
	}

	@Test void a07_resourceTemplateSpec() {
		var a = new McpResourceTemplateSpec().setUriTemplate("file:///{name}").setName("n")
			.setTitle("t").setDescription("d").setMimeType("text/plain");
		assertEquals("file:///{name}", a.getUriTemplate());
		assertEquals("n", a.getName());
		assertEquals("t", a.getTitle());
		assertEquals("d", a.getDescription());
		assertEquals("text/plain", a.getMimeType());
	}

	@Test void a08_templateRegistrationOrderCopyAndNull() {
		var a = new McpResourceTemplateSpec().setName("a");
		var b = new McpResourceTemplateSpec().setName("b");
		var source = new ArrayList<>(List.of(a));
		var config = new McpServerConfig().setResourceTemplates(source).addResourceTemplate(b);
		source.clear();
		assertEquals(List.of(a, b), config.getResourceTemplates());
		config.setResourceTemplates(null);
		assertTrue(config.getResourceTemplates().isEmpty());
		config.getResourceTemplates().add(a);
		assertEquals(1, config.getResourceTemplates().size());
	}

	@Test void a09_templateSpecHasNoPolicySurface() {
		var names = Arrays.stream(McpResourceTemplateSpec.class.getMethods())
			.map(Method::getName).collect(Collectors.toSet());
		assertFalse(names.stream().anyMatch(x -> x.contains("Cache") || x.contains("Ttl") || x.contains("Read")));
		assertEquals(Set.of("uriTemplate", "name", "title", "description", "mimeType"),
			Arrays.stream(McpResourceTemplateSpec.class.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet()));
	}
}
