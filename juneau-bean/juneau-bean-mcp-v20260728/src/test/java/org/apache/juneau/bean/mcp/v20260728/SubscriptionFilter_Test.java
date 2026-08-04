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

import java.util.*;

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

class SubscriptionFilter_Test {

	@Test void a01_allFieldsRoundTrip() {
		var filter = new SubscriptionFilter()
			.setToolsListChanged(true)
			.setPromptsListChanged(false)
			.setResourcesListChanged(true)
			.setResourceSubscriptions(List.of("file:///a.txt", "file:///b.txt"));
		var json = JsonSerializer.DEFAULT.write(filter);
		var copy = JsonParser.DEFAULT.read(json, SubscriptionFilter.class);
		assertEquals(json, JsonSerializer.DEFAULT.write(copy));
		assertEquals(true, copy.getToolsListChanged());
		assertEquals(false, copy.getPromptsListChanged());
		assertEquals(true, copy.getResourcesListChanged());
		assertEquals(List.of("file:///a.txt", "file:///b.txt"), copy.getResourceSubscriptions());
	}

	@Test void a02_unsetFieldsOmittedFromJson() {
		var json = JsonSerializer.DEFAULT.write(new SubscriptionFilter());
		assertEquals("{}", json);
	}

	@Test void a03_getResourceSubscriptions_isUnmodifiableView() {
		var filter = new SubscriptionFilter().setResourceSubscriptions(new ArrayList<>(List.of("x")));
		var view = filter.getResourceSubscriptions();
		assertThrows(UnsupportedOperationException.class, () -> view.add("y"));
	}

	@Test void a04_getResourceSubscriptions_nullWhenUnset() {
		assertNull(new SubscriptionFilter().getResourceSubscriptions());
	}
}
