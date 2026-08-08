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

import org.junit.jupiter.api.Test;

class McpChangeEvent_Test {

	@Test void a01_resourceUpdated_carriesUri() {
		var event = new McpChangeEvent(McpChangeKind.RESOURCE_UPDATED, "file:///a.txt");
		assertEquals(McpChangeKind.RESOURCE_UPDATED, event.kind());
		assertEquals("file:///a.txt", event.resourceUri());
	}

	@Test void a02_listChangedKinds_uriIsNull() {
		assertNull(new McpChangeEvent(McpChangeKind.TOOLS_LIST_CHANGED, null).resourceUri());
		assertNull(new McpChangeEvent(McpChangeKind.PROMPTS_LIST_CHANGED, null).resourceUri());
		assertNull(new McpChangeEvent(McpChangeKind.RESOURCES_LIST_CHANGED, null).resourceUri());
	}

	@Test void a03_nullKindThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> new McpChangeEvent(null, null));
		assertEquals("kind must not be null", e.getMessage());
	}

	@Test void a04_fourDistinctKinds() {
		assertEquals(4, McpChangeKind.values().length);
	}
}
