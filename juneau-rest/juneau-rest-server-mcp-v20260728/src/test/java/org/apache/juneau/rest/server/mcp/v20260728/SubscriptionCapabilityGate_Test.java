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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.junit.jupiter.api.Test;

class SubscriptionCapabilityGate_Test {

	@Test
	void dropsFieldsTheServerNeverAdvertised() {
		var requested = new SubscriptionFilter()
			.setToolsListChanged(true)
			.setPromptsListChanged(true)
			.setResourcesListChanged(true)
			.setResourceSubscriptions(List.of("file:///a"));
		// Only tools.listChanged is advertised; everything else must be dropped.
		var caps = new ServerCapabilities().setTools(new ToolCapability().setListChanged(true));

		var honored = SubscriptionCapabilityGate.honor(requested, caps);

		assertTrue(honored.isToolsListChanged());
		assertFalse(honored.isPromptsListChanged());
		assertFalse(honored.isResourcesListChanged());
		assertTrue(honored.getResourceUris().isEmpty());
	}

	@Test
	void honorsEveryAdvertisedFieldThatWasAlsoRequested() {
		var requested = new SubscriptionFilter()
			.setToolsListChanged(true)
			.setPromptsListChanged(true)
			.setResourcesListChanged(true)
			.setResourceSubscriptions(List.of("file:///a", "file:///b"));
		var caps = new ServerCapabilities()
			.setTools(new ToolCapability().setListChanged(true))
			.setPrompts(new PromptCapability().setListChanged(true))
			.setResources(new ResourceCapability().setListChanged(true).setSubscribe(true));

		var honored = SubscriptionCapabilityGate.honor(requested, caps);

		assertTrue(honored.isToolsListChanged());
		assertTrue(honored.isPromptsListChanged());
		assertTrue(honored.isResourcesListChanged());
		assertEquals(Set.of("file:///a", "file:///b"), honored.getResourceUris());
	}

	@Test
	void resourceSubscriptionsAreDroppedWhenSubscribeCapabilityIsNotAdvertised() {
		// resources.listChanged is advertised but resources.subscribe is not: URIs must be dropped
		// even though the resources capability object itself is present.
		var requested = new SubscriptionFilter().setResourceSubscriptions(List.of("file:///a"));
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setListChanged(true));

		var honored = SubscriptionCapabilityGate.honor(requested, caps);

		assertTrue(honored.getResourceUris().isEmpty());
	}

	@Test
	void nullCapabilitiesHonorNothing() {
		var requested = new SubscriptionFilter().setToolsListChanged(true).setResourceSubscriptions(List.of("file:///a"));
		var honored = SubscriptionCapabilityGate.honor(requested, null);
		assertFalse(honored.isToolsListChanged());
		assertFalse(honored.isPromptsListChanged());
		assertFalse(honored.isResourcesListChanged());
		assertTrue(honored.getResourceUris().isEmpty());
	}

	@Test
	void nullRequestedFilterHonorsNothing() {
		var caps = new ServerCapabilities().setTools(new ToolCapability().setListChanged(true));
		var honored = SubscriptionCapabilityGate.honor(null, caps);
		assertFalse(honored.isToolsListChanged());
		assertTrue(honored.getResourceUris().isEmpty());
	}

	@Test
	void toWireFilterRoundTripsTheHonoredSubset() {
		var honored = new org.apache.juneau.rest.server.mcp.McpSubscriptionFilter(true, false, true, Set.of("file:///a"));
		var wire = SubscriptionCapabilityGate.toWireFilter(honored);
		assertEquals(Boolean.TRUE, wire.getToolsListChanged());
		assertEquals(Boolean.FALSE, wire.getPromptsListChanged());
		assertEquals(Boolean.TRUE, wire.getResourcesListChanged());
		assertEquals(List.of("file:///a"), wire.getResourceSubscriptions());
	}
}
