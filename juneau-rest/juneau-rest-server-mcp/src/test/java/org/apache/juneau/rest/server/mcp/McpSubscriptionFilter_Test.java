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

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class McpSubscriptionFilter_Test {

	record Case(McpSubscriptionFilter filter, McpChangeEvent event, boolean expected) {}

	static List<Case> matrix() {
		var allFilter = new McpSubscriptionFilter(true, true, true, Set.of("file:///a.txt"));
		var noneFilter = new McpSubscriptionFilter(false, false, false, Set.of());
		var uriFilter = new McpSubscriptionFilter(false, false, false, Set.of("file:///a.txt"));
		return List.of(
			new Case(allFilter, new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null), true),
			new Case(allFilter, new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null), true),
			new Case(allFilter, new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null), true),
			new Case(allFilter, new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, "file:///a.txt"), true),
			new Case(allFilter, new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, "file:///b.txt"), false),
			new Case(noneFilter, new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null), false),
			new Case(noneFilter, new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null), false),
			new Case(noneFilter, new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null), false),
			new Case(uriFilter, new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, "file:///a.txt"), true),
			new Case(uriFilter, new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, "file:///other.txt"), false),
			new Case(uriFilter, new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null), false));
	}

	@ParameterizedTest
	@MethodSource("matrix")
	void a01_matchesMatrix(Case c) {
		assertEquals(c.expected(), c.filter().matches(c.event()));
	}

	@Test void a02_matchesNullEventReturnsFalse() {
		assertFalse(new McpSubscriptionFilter(true, true, true, Set.of("x")).matches(null));
	}

	@Test void a03_getResourceUris_isUnmodifiableAndDefaultsEmpty() {
		var filter = new McpSubscriptionFilter(false, false, false, null);
		assertTrue(filter.getResourceUris().isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> filter.getResourceUris().add("x"));
	}

	@Test void a04_gettersReflectConstructorArgs() {
		var filter = new McpSubscriptionFilter(true, false, true, Set.of("u1"));
		assertTrue(filter.isToolsListChanged());
		assertFalse(filter.isPromptsListChanged());
		assertTrue(filter.isResourcesListChanged());
		assertEquals(Set.of("u1"), filter.getResourceUris());
	}
}
