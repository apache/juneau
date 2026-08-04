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
package org.apache.juneau.rest.client.mcp.v20260728;

import org.apache.juneau.bean.mcp.v20260728.*;

/**
 * Typed callback sink for {@link McpClient#listen(SubscriptionFilter, McpSubscriptionListener)} frames.
 *
 * <p>
 * Every method is a no-op {@code default} so a caller only overrides the callbacks it cares about.
 *
 * @since 10.0.0
 */
public interface McpSubscriptionListener {

	/**
	 * First frame, always: echoes the server-honored (capability-gated) subset of the requested filter.
	 *
	 * @param honoredFilter The honored filter. Never <jk>null</jk>.
	 */
	default void onAcknowledged(SubscriptionFilter honoredFilter) {}

	/**
	 * {@code notifications/resources/updated}.
	 *
	 * @param uri The updated resource URI. Never <jk>null</jk>.
	 */
	default void onResourceUpdated(String uri) {}

	/**
	 * {@code notifications/resources/list_changed} / {@code notifications/tools/list_changed} /
	 * {@code notifications/prompts/list_changed} — {@code kind} discriminates which.
	 *
	 * @param kind Which list changed. Never <jk>null</jk>.
	 */
	default void onListChanged(McpListChangedKind kind) {}

	/**
	 * Graceful server close: the terminal {@code SubscriptionsListenResult} ({@code resultType:"complete"}) was
	 * received.
	 */
	default void onComplete() {}

	/**
	 * Abrupt drop, decode failure, or transport error.
	 *
	 * @param t The failure. Never <jk>null</jk>.
	 */
	default void onError(Throwable t) {}
}
