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

	@Test void a10_getBoolean_acceptedTrueContent_returnsTrue() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of("confirm", true)));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertTrue(ElicitationResponses.getBoolean(ctx, "q1", "confirm"));
	}

	@Test void a11_getBoolean_acceptedFalseContent_returnsFalse() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of("confirm", false)));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertFalse(ElicitationResponses.getBoolean(ctx, "q1", "confirm"));
	}

	@Test void a12_getBoolean_declinedAction_returnsFalse() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "decline"));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertFalse(ElicitationResponses.getBoolean(ctx, "q1", "confirm"));
	}

	@Test void a13_getBoolean_missingId_returnsFalse() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		assertFalse(ElicitationResponses.getBoolean(ctx, "missing", "confirm"));
	}

	@Test void a14_getBoolean_missingField_returnsFalse() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of()));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertFalse(ElicitationResponses.getBoolean(ctx, "q1", "confirm"));
	}

	@Test void a15_getBoolean_nullFieldThrows() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.getBoolean(ctx, "q1", null));
		assertEquals("Argument 'field' cannot be null.", e.getMessage());
	}

	@Test void a16_getString_acceptedContent_returnsValue() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of("name", "al")));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertEquals("al", ElicitationResponses.getString(ctx, "q1", "name"));
	}

	@Test void a17_getString_declinedAction_returnsNull() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "decline"));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertNull(ElicitationResponses.getString(ctx, "q1", "name"));
	}

	@Test void a18_getString_missingId_returnsNull() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		assertNull(ElicitationResponses.getString(ctx, "missing", "name"));
	}

	@Test void a19_getString_nonStringValue_returnsNull() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of("name", 42)));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertNull(ElicitationResponses.getString(ctx, "q1", "name"));
	}

	@Test void a20_getString_nullFieldThrows() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.getString(ctx, "q1", null));
		assertEquals("Argument 'field' cannot be null.", e.getMessage());
	}

	@Test void a21_getString_acceptedMissingField_returnsNull() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of()));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertNull(ElicitationResponses.getString(ctx, "q1", "name"));
	}

	@Test void a22_getBoolean_nonBooleanStringValue_returnsFalseSafely() {
		// L-3: a string "true" is not Boolean.TRUE - getBoolean must not do a lenient/truthy coercion.
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "accept", "content", JsonMap.of("confirm", "true")));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertFalse(ElicitationResponses.getBoolean(ctx, "q1", "confirm"));
	}

	@Test void a23_getBoolean_cancelledAction_returnsFalse() {
		// L-3: cancel (not just decline) is also a non-ACCEPT action that must be treated as false.
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "cancel"));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertFalse(ElicitationResponses.getBoolean(ctx, "q1", "confirm"));
	}

	@Test void a24_getString_cancelledAction_returnsNull() {
		var responses = Map.<String,Object>of("q1", JsonMap.of("action", "cancel"));
		var ctx = new McpMrtrResumeContext(null, responses);
		assertNull(ElicitationResponses.getString(ctx, "q1", "name"));
	}

	@Test void a25_getBoolean_nullCtxBlamesCtxBeforeField() {
		// L-2: ctx/id must be validated before field, so an all-null call blames "ctx", not "field".
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.getBoolean(null, null, null));
		assertEquals("Argument 'ctx' cannot be null.", e.getMessage());
	}

	@Test void a26_getBoolean_nullIdBlamesIdBeforeField() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.getBoolean(ctx, null, null));
		assertEquals("Argument 'id' cannot be null.", e.getMessage());
	}

	@Test void a27_getString_nullCtxBlamesCtxBeforeField() {
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.getString(null, null, null));
		assertEquals("Argument 'ctx' cannot be null.", e.getMessage());
	}

	@Test void a28_getString_nullIdBlamesIdBeforeField() {
		var ctx = new McpMrtrResumeContext(null, Map.of());
		var e = assertThrows(IllegalArgumentException.class, () -> ElicitationResponses.getString(ctx, null, null));
		assertEquals("Argument 'id' cannot be null.", e.getMessage());
	}
}
