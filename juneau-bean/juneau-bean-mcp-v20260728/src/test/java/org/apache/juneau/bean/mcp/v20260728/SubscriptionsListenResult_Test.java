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

class SubscriptionsListenResult_Test {

	@Test void a01_defaultsResultTypeToComplete() {
		var result = new SubscriptionsListenResult();
		assertEquals("complete", result.getResultType());
		assertEquals("{\"resultType\":\"complete\"}", JsonSerializer.DEFAULT.write(result));
	}

	@Test void a02_metaRoundTripsAsResultMeta() {
		var result = new SubscriptionsListenResult()
			.setMeta(new ResultMeta().setServerInfo(new Implementation().setName("s").setVersion("1")));
		var json = JsonSerializer.DEFAULT.write(result);
		var copy = JsonParser.DEFAULT.read(json, SubscriptionsListenResult.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("s", copy.getMeta().getServerInfo().getName());
	}

	@Test void a03_extendsResult() {
		assertTrue(Result.class.isAssignableFrom(SubscriptionsListenResult.class));
	}

	@Test void a04_declaresNoOwnMembers() {
		assertEquals(0, SubscriptionsListenResult.class.getDeclaredFields().length);
	}

	@Test void a05_subscriptionIdOnResultMeta_roundTripsThroughMeta() {
		var result = new SubscriptionsListenResult()
			.setMeta(new ResultMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-789"));
		var json = JsonSerializer.DEFAULT.write(result);
		assertTrue(json.contains("\"_meta\":{\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":\"sub-789\"}"));
		var copy = JsonParser.DEFAULT.read(json, SubscriptionsListenResult.class);
		assertEquals("sub-789", copy.getMeta().get(RequestMeta.KEY_SUBSCRIPTION_ID));
	}
}
