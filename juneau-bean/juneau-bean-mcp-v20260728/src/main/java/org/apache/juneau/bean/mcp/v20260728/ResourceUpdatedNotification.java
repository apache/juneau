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
 * Params for the {@code notifications/resources/updated} notification: a specific subscribed resource's
 * content changed.
 *
 * <p>
 * Only ever delivered inside a {@code subscriptions/listen} stream the client opened; {@code subscriptionId}
 * rides in the inherited {@code _meta} (see {@link RequestMeta#KEY_SUBSCRIPTION_ID}), not as an own field.
 */
@Marshalled
public class ResourceUpdatedNotification extends RequestParams<ResourceUpdatedNotification> {

	private String uri;

	/**
	 * The URI of the resource that changed.
	 *
	 * @return The URI, or {@code null} if unset.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the URI of the resource that changed.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceUpdatedNotification setUri(String value) {
		uri = value;
		return this;
	}
}
