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

class McpSubscriptionListener_Test extends TestBase {

	@Test
	void a01_defaultMethods_areNoOpsAndDoNotThrow() {
		McpSubscriptionListener listener = new McpSubscriptionListener() {};
		var filter = new SubscriptionFilter();
		var error = new RuntimeException("boom");
		assertDoesNotThrow(() -> listener.onAcknowledged(filter));
		assertDoesNotThrow(() -> listener.onResourceUpdated("file:///a.txt"));
		assertDoesNotThrow(() -> listener.onListChanged(McpListChangedKind.TOOLS));
		assertDoesNotThrow(listener::onComplete);
		assertDoesNotThrow(() -> listener.onError(error));
	}

	@Test
	void a02_listChangedKind_hasThreeValuesInDeclarationOrder() {
		assertArrayEquals(
			new McpListChangedKind[]{McpListChangedKind.TOOLS, McpListChangedKind.PROMPTS, McpListChangedKind.RESOURCES},
			McpListChangedKind.values());
	}
}
