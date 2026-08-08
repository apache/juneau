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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * A single revision-neutral change event published through {@link McpSubscriptions} and fanned out by an
 * {@link McpSubscriptionBroker} to every matching {@link McpSubscription}.
 *
 * <p>
 * Immutable value carrier. {@link #resourceUri()} is only ever non-{@code null} when {@link #kind()} is
 * {@link McpChangeKind#RESOURCE_UPDATED}; this is a documented contract of the four {@link McpSubscriptions}
 * publish methods, not an invariant enforced by this constructor.
 *
 * @param kind The change kind. Never {@code null}.
 * @param resourceUri The affected resource URI, or {@code null} for a list-changed kind.
 */
public record McpChangeEvent(McpChangeKind kind, String resourceUri) {

	/**
	 * Canonical constructor.
	 *
	 * @throws IllegalArgumentException If {@code kind} is {@code null}.
	 */
	public McpChangeEvent {
		if (kind == null)
			throw iaex("kind must not be null");
	}
}
