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
package org.apache.juneau.bean.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class McpMethodsSubscriptions_Test {

	@Test void a01_subscriptionsListenConstant() {
		assertEquals("subscriptions/listen", McpMethods.SUBSCRIPTIONS_LISTEN);
	}

	@Test void a02_notificationMethodConstants() {
		assertEquals("notifications/resources/updated", McpMethods.NOTIFICATIONS_RESOURCES_UPDATED);
		assertEquals("notifications/resources/list_changed", McpMethods.NOTIFICATIONS_RESOURCES_LIST_CHANGED);
		assertEquals("notifications/tools/list_changed", McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED);
		assertEquals("notifications/prompts/list_changed", McpMethods.NOTIFICATIONS_PROMPTS_LIST_CHANGED);
		assertEquals("notifications/subscriptions/acknowledged", McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED);
	}
}
