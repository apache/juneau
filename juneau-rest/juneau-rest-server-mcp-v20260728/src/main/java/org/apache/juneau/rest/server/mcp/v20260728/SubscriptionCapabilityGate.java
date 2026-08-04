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

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.server.mcp.McpSubscriptionFilter;

/**
 * Capability-gates a client-requested {@link SubscriptionFilter} against the advertised
 * {@link ServerCapabilities}, producing both the neutral honored filter used to register with the
 * broker and the wire filter echoed back on the {@code notifications/subscriptions/acknowledged} frame.
 *
 * <p>
 * A requested field is honored only when the server actually advertises the matching capability:
 * {@code toolsListChanged} requires {@code tools.listChanged}, {@code promptsListChanged} requires
 * {@code prompts.listChanged}, {@code resourcesListChanged} requires {@code resources.listChanged}, and
 * {@code resourceSubscriptions} requires {@code resources.subscribe}. Everything else is dropped.
 */
final class SubscriptionCapabilityGate {

	private SubscriptionCapabilityGate() {}

	/**
	 * Computes the honored neutral filter for a client-requested wire filter.
	 *
	 * @param requested The client-requested filter. Can be <jk>null</jk> (treated as an all-false request).
	 * @param caps The advertised capabilities. Can be <jk>null</jk> (nothing is honored).
	 * @return The honored filter. Never <jk>null</jk>.
	 */
	static McpSubscriptionFilter honor(SubscriptionFilter requested, ServerCapabilities caps) {
		var req = requested == null ? new SubscriptionFilter() : requested;
		var resources = caps == null ? null : caps.getResources();
		var tools = caps == null ? null : caps.getTools();
		var prompts = caps == null ? null : caps.getPrompts();

		var toolsListChanged = truthy(req.getToolsListChanged()) && tools != null && truthy(tools.getListChanged());
		var promptsListChanged = truthy(req.getPromptsListChanged()) && prompts != null && truthy(prompts.getListChanged());
		var resourcesListChanged = truthy(req.getResourcesListChanged()) && resources != null && truthy(resources.getListChanged());

		Set<String> resourceUris = new LinkedHashSet<>();
		if (resources != null && truthy(resources.getSubscribe()) && req.getResourceSubscriptions() != null)
			resourceUris.addAll(req.getResourceSubscriptions());

		return new McpSubscriptionFilter(toolsListChanged, promptsListChanged, resourcesListChanged, resourceUris);
	}

	/**
	 * Builds the wire filter echoed on the {@code notifications/subscriptions/acknowledged} frame from
	 * the honored neutral filter.
	 *
	 * @param honored The honored neutral filter. Must not be <jk>null</jk>.
	 * @return The wire filter. Never <jk>null</jk>.
	 */
	static SubscriptionFilter toWireFilter(McpSubscriptionFilter honored) {
		return new SubscriptionFilter()
			.setToolsListChanged(honored.isToolsListChanged())
			.setPromptsListChanged(honored.isPromptsListChanged())
			.setResourcesListChanged(honored.isResourcesListChanged())
			.setResourceSubscriptions(new ArrayList<>(honored.getResourceUris()));
	}

	private static boolean truthy(Boolean value) {
		return Boolean.TRUE.equals(value);
	}
}
