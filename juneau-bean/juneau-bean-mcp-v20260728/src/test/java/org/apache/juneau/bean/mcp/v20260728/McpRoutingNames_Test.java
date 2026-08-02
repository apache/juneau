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

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class McpRoutingNames_Test {

	@Test
	void a01_toolsCall_usesParamsName() {
		assertEquals("echo", McpRoutingNames.routingName(McpMethods.TOOLS_CALL, JsonMap.of("name", "echo")));
	}

	@Test
	void a02_promptsGet_usesParamsName() {
		assertEquals("draft", McpRoutingNames.routingName(McpMethods.PROMPTS_GET, JsonMap.of("name", "draft")));
	}

	@Test
	void a03_resourcesRead_usesParamsUri() {
		assertEquals("file:///a.txt", McpRoutingNames.routingName(McpMethods.RESOURCES_READ, JsonMap.of("uri", "file:///a.txt")));
	}

	@Test
	void a04_otherMethods_returnEmptyString() {
		assertEquals("", McpRoutingNames.routingName(McpMethods.SERVER_DISCOVER, JsonMap.of("name", "ignored")));
		assertEquals("", McpRoutingNames.routingName(McpMethods.COMPLETION_COMPLETE, JsonMap.of("ref", JsonMap.of("type", "ref/prompt", "name", "x"))));
	}

	@Test
	void a05_missingOrNullSourceField_returnsEmptyString() {
		assertEquals("", McpRoutingNames.routingName(McpMethods.TOOLS_CALL, JsonMap.of()));
		assertEquals("", McpRoutingNames.routingName(McpMethods.RESOURCES_READ, JsonMap.of("uri", null)));
	}

	@Test
	void a06_nonMapParams_returnsEmptyString() {
		assertEquals("", McpRoutingNames.routingName(McpMethods.TOOLS_CALL, "not-a-map"));
	}
}
