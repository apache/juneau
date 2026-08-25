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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link McpOperationContext}'s compact constructor &mdash; non-null method enforcement and the
 * null-vs-populated {@code params} defensive-copy branches.
 */
class McpOperationContext_Test {

	@Test void a01_nullParamsDefaultsToEmptyMap() {
		var ctx = new McpOperationContext("tools/call", "echo", null);
		assertEquals(Map.of(), ctx.params());
	}

	@Test void a02_populatedParamsDefensivelyCopied() {
		var src = new LinkedHashMap<String,Object>();
		src.put("name", "echo");
		var ctx = new McpOperationContext("tools/call", "echo", src);
		assertEquals("echo", ctx.params().get("name"));
		src.put("name", "changed");
		assertEquals("echo", ctx.params().get("name"), "params must be defensively copied, not a live view");
	}

	@Test void a03_paramsViewIsUnmodifiable() {
		var ctx = new McpOperationContext("tools/call", "echo", Map.of("k", "v"));
		var params = ctx.params();
		assertThrows(UnsupportedOperationException.class, () -> params.put("x", "y"));
	}

	@Test void b01_nullMethodRejected() {
		assertThrows(NullPointerException.class, () -> new McpOperationContext(null, "echo", Map.of()));
	}
}
