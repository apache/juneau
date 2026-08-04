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
 * Request params for {@code subscriptions/listen} (SEP-2575): the requested notification filter.
 *
 * <p>
 * Unlike every other request in this package, the terminal response to this request is withheld while the
 * server streams notifications (see {@link SubscriptionsListenResult}); {@code _meta.subscriptionId}
 * (see {@link RequestMeta#KEY_SUBSCRIPTION_ID}) tags every frame on that stream with this request's JSON-RPC
 * id.
 */
@Marshalled
public class SubscriptionsListenRequest extends RequestParams<SubscriptionsListenRequest> {

	private SubscriptionFilter notifications;

	/**
	 * The requested notification filter.
	 *
	 * @return The filter, or {@code null} if unset.
	 */
	public SubscriptionFilter getNotifications() {
		return notifications;
	}

	/**
	 * Sets the requested notification filter.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SubscriptionsListenRequest setNotifications(SubscriptionFilter value) {
		notifications = value;
		return this;
	}
}
