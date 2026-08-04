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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * Wire filter for {@code subscriptions/listen}: which notification kinds and resource URIs a client wants
 * (see {@link SubscriptionsListenRequest#getNotifications()}), or the honored subset a server actually
 * granted (see {@link SubscriptionsAcknowledgedNotification#getNotifications()}).
 *
 * <p>
 * Distinct from the revision-neutral {@code org.apache.juneau.rest.server.mcp.McpSubscriptionFilter}: this is
 * the wire shape (nullable {@link Boolean}s, a plain {@link List}); the neutral type is the immutable,
 * always-non-null server-side carrier a v2 dispatch branch builds from this one after capability gating.
 */
@Marshalled
public class SubscriptionFilter {

	private Boolean toolsListChanged;
	private Boolean promptsListChanged;
	private Boolean resourcesListChanged;
	private List<String> resourceSubscriptions;

	/**
	 * Whether the tool list-changed signal is requested/honored.
	 *
	 * @return The value, or {@code null} if unset.
	 */
	public Boolean getToolsListChanged() {
		return toolsListChanged;
	}

	/**
	 * Sets whether the tool list-changed signal is requested/honored.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SubscriptionFilter setToolsListChanged(Boolean value) {
		toolsListChanged = value;
		return this;
	}

	/**
	 * Whether the prompt list-changed signal is requested/honored.
	 *
	 * @return The value, or {@code null} if unset.
	 */
	public Boolean getPromptsListChanged() {
		return promptsListChanged;
	}

	/**
	 * Sets whether the prompt list-changed signal is requested/honored.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SubscriptionFilter setPromptsListChanged(Boolean value) {
		promptsListChanged = value;
		return this;
	}

	/**
	 * Whether the resource list-changed signal is requested/honored.
	 *
	 * @return The value, or {@code null} if unset.
	 */
	public Boolean getResourcesListChanged() {
		return resourcesListChanged;
	}

	/**
	 * Sets whether the resource list-changed signal is requested/honored.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SubscriptionFilter setResourcesListChanged(Boolean value) {
		resourcesListChanged = value;
		return this;
	}

	/**
	 * The exact resource URIs requested/honored for {@code resourceUpdated} events.
	 *
	 * @return An unmodifiable view, or {@code null} if unset.
	 */
	public List<String> getResourceSubscriptions() {
		return u(resourceSubscriptions);
	}

	/**
	 * Sets the resource URIs requested/honored for {@code resourceUpdated} events.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SubscriptionFilter setResourceSubscriptions(List<String> value) {
		resourceSubscriptions = value;
		return this;
	}
}
