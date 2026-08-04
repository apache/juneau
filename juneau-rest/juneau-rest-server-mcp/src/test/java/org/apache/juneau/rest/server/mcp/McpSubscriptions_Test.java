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

class McpSubscriptions_Test {

	@Test void a01_implementationReceivesAllFourPublishCalls() {
		var calls = new ArrayList<String>();
		McpSubscriptions subs = new McpSubscriptions() {
			@Override public void resourceUpdated(String uri) { calls.add("resourceUpdated:" + uri); }
			@Override public void toolsListChanged() { calls.add("toolsListChanged"); }
			@Override public void promptsListChanged() { calls.add("promptsListChanged"); }
			@Override public void resourcesListChanged() { calls.add("resourcesListChanged"); }
		};
		subs.resourceUpdated("file:///a.txt");
		subs.toolsListChanged();
		subs.promptsListChanged();
		subs.resourcesListChanged();
		assertEquals(
			List.of("resourceUpdated:file:///a.txt", "toolsListChanged", "promptsListChanged", "resourcesListChanged"),
			calls);
	}
}
