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

import java.util.*;

/**
 * A revision-neutral, immutable subscription filter: the set of change kinds and resource URIs one
 * {@link McpSubscription} is interested in.
 *
 * <p>
 * Concrete (not an interface) on purpose: a v2 adapter reads the honored values back off this type to
 * build the {@code notifications/subscriptions/acknowledged} frame, so it needs field-level accessors, not
 * just the {@link #matches(McpChangeEvent)} predicate.
 *
 * @param toolsListChanged Whether the tool list-changed signal is honored.
 * @param promptsListChanged Whether the prompt list-changed signal is honored.
 * @param resourcesListChanged Whether the resource list-changed signal is honored.
 * @param resourceUris The exact resource URIs subscribed to for {@code resourceUpdated} events. Can be
 * 	{@code null} (treated as empty).
 */
public record McpSubscriptionFilter(boolean toolsListChanged, boolean promptsListChanged, boolean resourcesListChanged,
		Set<String> resourceUris) {

	/**
	 * Canonical constructor.
	 *
	 * <p>
	 * Normalizes {@code resourceUris} to an unmodifiable, insertion-ordered copy (empty when {@code null}).
	 */
	public McpSubscriptionFilter {
		resourceUris = resourceUris == null ? Collections.emptySet()
			: Collections.unmodifiableSet(new LinkedHashSet<>(resourceUris));
	}

	/**
	 * Returns whether this filter opted into the given event's change kind (and, for
	 * {@link McpChangeKind#RESOURCE_UPDATED}, its exact resource URI).
	 *
	 * @param event The event to test. A {@code null} event never matches.
	 * @return {@code true} if this filter honors the event.
	 */
	public boolean matches(McpChangeEvent event) {
		if (event == null)
			return false;
		return switch (event.kind()) {
			case TOOLS_LIST_CHANGED -> toolsListChanged;
			case PROMPTS_LIST_CHANGED -> promptsListChanged;
			case RESOURCES_LIST_CHANGED -> resourcesListChanged;
			case RESOURCE_UPDATED -> resourceUris.contains(event.resourceUri());
		};
	}
}
