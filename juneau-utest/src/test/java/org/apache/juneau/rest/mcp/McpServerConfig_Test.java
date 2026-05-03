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
package org.apache.juneau.rest.mcp;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpServerConfig}.
 */
class McpServerConfig_Test {

	private static McpToolHandler dummyTool(String name) {
		return new McpToolHandler() {
			@Override
			public Tool descriptor() {
				return new Tool().setName(name);
			}

			@Override
			public CallToolResult call(Map<String, Object> arguments, org.apache.juneau.cp.BasicBeanStore ctx) {
				return new CallToolResult();
			}
		};
	}

	private static McpPromptHandler dummyPrompt(String name) {
		return new McpPromptHandler() {
			@Override
			public Prompt descriptor() {
				return new Prompt().setName(name);
			}

			@Override
			public GetPromptResult get(Map<String, Object> arguments, org.apache.juneau.cp.BasicBeanStore ctx) {
				return new GetPromptResult();
			}
		};
	}

	private static McpResourceHandler dummyResource(String uri) {
		return new McpResourceHandler() {
			@Override
			public Resource descriptor() {
				return new Resource().setUri(uri);
			}

			@Override
			public ReadResourceResult read(String u, org.apache.juneau.cp.BasicBeanStore ctx) {
				return new ReadResourceResult();
			}
		};
	}

	@Test
	void defaults() {
		var c = new McpServerConfig();
		assertNull(c.getServerInfo());
		assertString(McpProtocol.VERSION_2025_06_18, c.getProtocolVersion());
		assertNull(c.getInstructions());
		assertNotNull(c.getTools());
		assertNotNull(c.getPrompts());
		assertNotNull(c.getResources());
		assertNull(c.getCapabilities());
		assertSame(McpCursor.SINGLE_PAGE, c.getCursor());
	}

	@Test
	void setters_and_addCalls() {
		var info = new Implementation().setName("x").setVersion("1");
		var caps = new ServerCapabilities().setLogging(new LoggingCapability());
		var c = new McpServerConfig()
			.setServerInfo(info)
			.setProtocolVersion("2024-11-05")
			.setInstructions("hello")
			.setCapabilities(caps)
			.setCursor(McpCursor.fixedSize(5))
			.addTool(dummyTool("t1"), dummyTool("t2"))
			.addPrompt(dummyPrompt("p1"))
			.addResource(dummyResource("r://a"));

		assertSame(info, c.getServerInfo());
		assertString("2024-11-05", c.getProtocolVersion());
		assertString("hello", c.getInstructions());
		assertSame(caps, c.getCapabilities());
		assertNotSame(McpCursor.SINGLE_PAGE, c.getCursor());
		assertSize(2, c.getTools());
		assertSize(1, c.getPrompts());
		assertSize(1, c.getResources());
	}

	@Test
	void setLists_replacingAndNullClears() {
		var c = new McpServerConfig().addTool(dummyTool("t")).addPrompt(dummyPrompt("p")).addResource(dummyResource("r"));
		c.setTools(null);
		c.setPrompts(null);
		c.setResources(null);
		assertEmpty(c.getTools());
		assertEmpty(c.getPrompts());
		assertEmpty(c.getResources());

		c.setTools(List.of(dummyTool("a")));
		c.setPrompts(List.of(dummyPrompt("a")));
		c.setResources(List.of(dummyResource("a")));
		assertSize(1, c.getTools());
		assertSize(1, c.getPrompts());
		assertSize(1, c.getResources());
	}

	@Test
	void setCursor_nullResets() {
		var c = new McpServerConfig().setCursor(McpCursor.fixedSize(2));
		c.setCursor(null);
		assertSame(McpCursor.SINGLE_PAGE, c.getCursor());
	}
}
