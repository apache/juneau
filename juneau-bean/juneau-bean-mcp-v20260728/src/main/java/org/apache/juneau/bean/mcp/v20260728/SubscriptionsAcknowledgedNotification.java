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

import org.apache.juneau.marshall.*;

/**
 * Mandatory first frame on every accepted {@code subscriptions/listen} stream: echoes the honored subset of
 * the client's requested {@link SubscriptionFilter} (after capability gating).
 *
 * <p>
 * Extends {@link RequestParams} (not {@link Result}) because this is a JSON-RPC <i>notification</i>, not a
 * response: {@code subscriptionId} rides in the inherited {@code _meta} via {@code RequestMeta}, set to the
 * listen request's own JSON-RPC id (see {@link RequestMeta#KEY_SUBSCRIPTION_ID}).
 */
@Marshalled
public class SubscriptionsAcknowledgedNotification extends RequestParams<SubscriptionsAcknowledgedNotification> {

	private SubscriptionFilter notifications;

	/**
	 * The honored subset of the requested notification filter.
	 *
	 * @return The filter, or {@code null} if unset.
	 */
	public SubscriptionFilter getNotifications() {
		return notifications;
	}

	/**
	 * Sets the honored subset of the requested notification filter.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SubscriptionsAcknowledgedNotification setNotifications(SubscriptionFilter value) {
		notifications = value;
		return this;
	}
}
