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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for default {@code descriptor()} methods on the handler interfaces (which throw
 * {@link UnsupportedOperationException} unless overridden).
 */
class McpHandlerDefaults_Test {

	@Test
	void a01_toolHandler_defaultDescriptor_throws() {
		McpToolHandler h = (args, ctx) -> new CallToolResult();
		assertThrows(UnsupportedOperationException.class, h::descriptor);
	}

	@Test
	void a02_promptHandler_defaultDescriptor_throws() {
		McpPromptHandler h = (args, ctx) -> new GetPromptResult();
		assertThrows(UnsupportedOperationException.class, h::descriptor);
	}

	@Test
	void a03_resourceHandler_defaultDescriptor_throws() {
		McpResourceHandler h = (uri, ctx) -> new ReadResourceResult();
		assertThrows(UnsupportedOperationException.class, h::descriptor);
	}

	@Test
	void c01_typedHandlers_constructor_isPrivate() {
		// Sanity: the static façade class should not be instantiable. Reflection trick used to bump coverage on the
		// implicit private no-arg constructor.
		assertDoesNotThrow(() -> {
			var ctor = McpTypedHandlers.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			assertNotNull(ctor.newInstance());
		});
	}
}
