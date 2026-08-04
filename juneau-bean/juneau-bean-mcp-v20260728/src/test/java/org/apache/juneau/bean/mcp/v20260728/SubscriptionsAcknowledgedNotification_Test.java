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

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

class SubscriptionsAcknowledgedNotification_Test {

	@Test void a01_notificationsAndMetaRoundTrip() {
		var notif = new SubscriptionsAcknowledgedNotification()
			.setNotifications(new SubscriptionFilter().setToolsListChanged(true).setResourcesListChanged(false))
			.setMeta(new RequestMeta().setProtocolVersion("2026-07-28"));
		var json = JsonSerializer.DEFAULT.write(notif);
		assertTrue(json.contains("\"notifications\":{"));
		assertTrue(json.contains("\"_meta\":{"));
		var copy = JsonParser.DEFAULT.read(json, SubscriptionsAcknowledgedNotification.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals(true, copy.getNotifications().getToolsListChanged());
		assertEquals(false, copy.getNotifications().getResourcesListChanged());
	}

	@Test void a02_extendsRequestParams() {
		assertTrue(RequestParams.class.isAssignableFrom(SubscriptionsAcknowledgedNotification.class));
	}

	@Test void a03_declaresOnlyNotificationsBeyondInheritedMeta() {
		var names = Arrays.stream(SubscriptionsAcknowledgedNotification.class.getDeclaredFields())
			.map(Field::getName).collect(Collectors.toSet());
		assertEquals(Set.of("notifications"), names);
	}
}
