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

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link ElicitationResponses}.
 */
class ElicitationResponses_Test {

	@Test void a01_get_presentKey_acceptActionWithContent() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of("choice", "red")));
		var ctx = new McpMrtrResumeContext(null, responses);
		var a = ElicitationResponses.get(ctx, "q1");
		assertEquals(ElicitAction.ACCEPT, a.getAction());
		assertEquals("red", a.getContent().get("choice"));
	}

	@Test void a02_get_presentKey_declineActionNoContent() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "decline"));
		var ctx = new McpMrtrResumeContext(null, responses);
		var a = ElicitationResponses.get(ctx, "q1");
		assertEquals(ElicitAction.DECLINE, a.getAction());
		assertNull(a.getContent());
	}

	@Test void a03_get_missingKey_returnsNull() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		assertNull(ElicitationResponses.get(ctx, "missing"));
	}

	@Test void a04_get_nullCtxThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.get(null, "q1"));
		assertEquals("Argument 'ctx' cannot be null.", e.getMessage());
	}

	@Test void a05_get_nullIdThrows() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.get(ctx, null));
		assertEquals("Argument 'id' cannot be null.", e.getMessage());
	}

	@Test void a06_all_multipleKeys_returnsAllTyped() {
		var responses = new LinkedHashMap<String,Object>();
		responses.put("q1", JsonMap.of("action", "accept", "content", JsonMap.of("choice", "red")));
		responses.put("q2", JsonMap.of("action", "decline"));
		var ctx = new McpMrtrResumeContext(null, responses);
		var all = ElicitationResponses.all(ctx);
		assertEquals(2, all.size());
		assertEquals(ElicitAction.ACCEPT, all.get("q1").getAction());
		assertEquals("red", all.get("q1").getContent().get("choice"));
		assertEquals(ElicitAction.DECLINE, all.get("q2").getAction());
	}

	@Test void a07_all_emptyResponses_returnsEmptyMap() {
		var all = ElicitationResponses.all(new McpMrtrResumeContext(null, Map.of()));
		assertNotNull(all);
		assertTrue(all.isEmpty());
	}

	@Test void a08_all_nullCtxThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.all(null));
		assertEquals("Argument 'ctx' cannot be null.", e.getMessage());
	}

	@Test void a09_get_malformedShapeThrows() {
		// A JSON-array answer cannot be converted to a bean target: the marshaller throws (which the
		// dispatcher's generic branch surfaces as -32603). Mirrors McpInputRequiredSignal_Test's
		// b07_continuationAsShapeMismatchThrows for the continuation side.
		var responses = Map.<String,Object>of("q1", JsonList.of(1, 2));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertThrows(RuntimeException.class, () -> ElicitationResponses.get(ctx, "q1"));
	}
}
