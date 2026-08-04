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

class ResourceUpdatedNotification_Test {

	@Test void a01_uriAndMetaRoundTrip() {
		var notif = new ResourceUpdatedNotification().setUri("file:///a.txt")
			.setMeta(new RequestMeta().setProtocolVersion("2026-07-28"));
		var json = JsonSerializer.DEFAULT.write(notif);
		assertTrue(json.contains("\"uri\":\"file:///a.txt\""));
		var copy = JsonParser.DEFAULT.read(json, ResourceUpdatedNotification.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals("file:///a.txt", copy.getUri());
	}

	@Test void a02_extendsRequestParams() {
		assertTrue(RequestParams.class.isAssignableFrom(ResourceUpdatedNotification.class));
	}

	@Test void a03_uriOmittedWhenUnset() {
		var json = JsonSerializer.DEFAULT.write(new ResourceUpdatedNotification());
		assertFalse(json.contains("uri"));
	}
}
