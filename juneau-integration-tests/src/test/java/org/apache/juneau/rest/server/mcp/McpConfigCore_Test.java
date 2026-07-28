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

import java.util.*;

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
	void defaults() {
		var a = new McpServerConfig();
		assertNull(a.getName());
		assertNull(a.getVersion());
		assertNull(a.getInstructions());
		assertNotNull(a.getTools());
		assertNotNull(a.getPrompts());
		assertNotNull(a.getResources());
		assertSame(McpCursor.SINGLE_PAGE, a.getCursor());
	}

	@Test
	void setters_and_addCalls() {
		var a = new McpServerConfig()
			.setName("x")
			.setVersion("1")
			.setInstructions("hello")
			.setCursor(McpCursor.fixedSize(5))
			.addTool(dummyTool("t1"), dummyTool("t2"))
			.addPrompt(dummyPrompt("p1"))
			.addResource(dummyResource("r://a"));

		assertString("x", a.getName());
		assertString("1", a.getVersion());
		assertString("hello", a.getInstructions());
		assertNotSame(McpCursor.SINGLE_PAGE, a.getCursor());
		assertSize(2, a.getTools());
		assertSize(1, a.getPrompts());
		assertSize(1, a.getResources());
	}

	@Test
	void setLists_replacingAndNullClears() {
		var a = new McpServerConfig().addTool(dummyTool("t")).addPrompt(dummyPrompt("p")).addResource(dummyResource("r"));
		a.setTools(null);
		a.setPrompts(null);
		a.setResources(null);
		assertEmpty(a.getTools());
		assertEmpty(a.getPrompts());
		assertEmpty(a.getResources());

		a.setTools(List.of(dummyTool("a")));
		a.setPrompts(List.of(dummyPrompt("a")));
		a.setResources(List.of(dummyResource("a")));
		assertSize(1, a.getTools());
		assertSize(1, a.getPrompts());
		assertSize(1, a.getResources());
	}

	@Test
	void setCursor_nullResets() {
		var a = new McpServerConfig().setCursor(McpCursor.fixedSize(2));
		a.setCursor(null);
		assertSame(McpCursor.SINGLE_PAGE, a.getCursor());
	}

	@Test
	void noRevisionSpecificSurfaceRemains() {
		var m = Arrays.stream(McpServerConfig.class.getMethods()).map(java.lang.reflect.Method::getName).toList();
		assertFalse(m.contains("getCapabilities"), "capabilities are revision-owned; core must hold none");
		assertFalse(m.contains("setCapabilities"), "capabilities are revision-owned; core must hold none");
		assertFalse(m.contains("getProtocolVersion"), "the protocol version is owned by McpRevision.protocolVersion()");
		assertFalse(m.contains("setProtocolVersion"), "the protocol version is owned by McpRevision.protocolVersion()");
		assertFalse(m.contains("getServerInfo"), "server identity is plain name/version on the neutral config");
	}

	@Test
	void nullListElementsAreDistinctFromEmpty() {
		assertNull(new McpToolOutcome().getContent());
		assertNull(new McpPromptOutcome().getMessages());
		assertNull(new McpResourceOutcome().getContents());
	}
}
