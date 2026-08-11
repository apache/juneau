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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.junit.jupiter.api.*;

class McpClient_Surface_Test extends TestBase {

	@Test
	void a01_surface_hasServerDiscoverAndNoInitialize() throws Exception {
		var type = McpClient.class;
		assertNotNull(type.getMethod("serverDiscover"));
		assertNotNull(type.getMethod("ping"));
		assertNotNull(type.getMethod("listTools"));
		assertNotNull(type.getMethod("callTool", String.class, java.util.Map.class));
		assertNotNull(type.getMethod("listPrompts"));
		assertNotNull(type.getMethod("getPrompt", String.class, java.util.Map.class));
		assertNotNull(type.getMethod("listResources"));
		assertNotNull(type.getMethod("readResource", String.class));
		assertNotNull(type.getMethod("listResourceTemplates"));
		assertNotNull(type.getMethod("complete", CompletionReference.class, String.class, String.class, java.util.Map.class));

		assertThrows(NoSuchMethodException.class, () -> type.getMethod("initialize"));
	}

	@Test
	void a02_builderFactoryPresent() {
		assertNotNull(McpClient.builder());
	}
}
