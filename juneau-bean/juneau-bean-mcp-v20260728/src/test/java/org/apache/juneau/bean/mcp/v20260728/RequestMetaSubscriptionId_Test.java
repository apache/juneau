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

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

class RequestMetaSubscriptionId_Test {

	@Test void a01_keyConstantExactValue() {
		assertEquals("io.modelcontextprotocol/subscriptionId", RequestMeta.KEY_SUBSCRIPTION_ID);
	}

	@Test void a02_setAndGetViaExtensionTriplet_roundTripsThroughJson() {
		var meta = new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-123");
		var json = JsonSerializer.DEFAULT.write(meta);
		assertTrue(json.contains("\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":\"sub-123\""));
		var copy = JsonParser.DEFAULT.read(json, RequestMeta.class);
		assertEquals("sub-123", copy.get(RequestMeta.KEY_SUBSCRIPTION_ID));
	}

	@Test void a03_subscriptionId_readableFromRequestParamsCarrier() {
		var req = new SubscriptionsAcknowledgedNotification()
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-456"));
		var json = JsonSerializer.DEFAULT.write(req);
		assertTrue(json.contains("\"_meta\":{\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":\"sub-456\"}"));
		var copy = JsonParser.DEFAULT.read(json, SubscriptionsAcknowledgedNotification.class);
		assertEquals("sub-456", copy.getMeta().get(RequestMeta.KEY_SUBSCRIPTION_ID));
	}
}
