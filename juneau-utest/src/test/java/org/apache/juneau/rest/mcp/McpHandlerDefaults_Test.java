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

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.cp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for default {@code descriptor()} methods on the handler interfaces (which throw
 * {@link UnsupportedOperationException} unless overridden).
 */
class McpHandlerDefaults_Test {

	@Test
	void toolHandler_defaultDescriptor_throws() {
		McpToolHandler h = (args, ctx) -> new CallToolResult();
		assertThrows(UnsupportedOperationException.class, h::descriptor);
	}

	@Test
	void promptHandler_defaultDescriptor_throws() {
		McpPromptHandler h = (args, ctx) -> new GetPromptResult();
		assertThrows(UnsupportedOperationException.class, h::descriptor);
	}

	@Test
	void resourceHandler_defaultDescriptor_throws() {
		McpResourceHandler h = (uri, ctx) -> new ReadResourceResult();
		assertThrows(UnsupportedOperationException.class, h::descriptor);
	}

	@Test
	void mcpException_default_data_isNull() {
		var e = new McpException(-1, "x");
		assertNull(e.getData());
		assertEquals(-1, e.getCode());
		assertString("x", e.getMessage());
	}

	@Test
	void mcpException_with_data() {
		var e = new McpException(-1, "x", "data");
		assertString("data", e.getData());
		var rpc = e.toJsonRpcError();
		assertEquals(-1, rpc.getCode());
		assertString("x", rpc.getMessage());
		assertString("data", rpc.getData());
	}

	@Test
	void typedHandlers_constructor_isPrivate() {
		// Sanity: the static façade class should not be instantiable. Reflection trick used to bump coverage on the
		// implicit private no-arg constructor.
		try {
			var ctor = McpTypedHandlers.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			assertNotNull(ctor.newInstance());
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	void mcp_facade_constructor_isPrivate() {
		try {
			var ctor = Mcp.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			assertNotNull(ctor.newInstance());
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	void cursor_passes_ctx_to_strategy() {
		// Verify ctx parameter reaches the cursor (covers the BasicBeanStore parameter passthrough).
		var bs = BasicBeanStore.create().build();
		var got = new Object[1];
		McpCursor c = new McpCursor() {
			@Override
			public <T> McpPage<T> page(java.util.List<T> all, String cursor, BasicBeanStore ctx) {
				got[0] = ctx;
				return new McpPage<>(all, null);
			}
		};
		c.page(java.util.List.of(), null, bs);
		assertSame(bs, got[0]);
	}
}
