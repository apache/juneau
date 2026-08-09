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

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpCursor} bean-store passthrough.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class McpCursorCtx_Test {

	@Test
	void d01_cursor_passes_ctx_to_strategy() {
		// Verify ctx parameter reaches the cursor (covers the BeanStore parameter passthrough).
		var bs = new BasicBeanStore();
		var got = new Object[1];
		McpCursor c = new McpCursor() {
			@Override
			public <T> McpPage<T> page(java.util.List<T> all, String cursor, BeanStore ctx) {
				got[0] = ctx;
				return new McpPage<>(all, null);
			}
		};
		c.page(java.util.List.of(), null, bs);
		assertSame(bs, got[0]);
	}
}
