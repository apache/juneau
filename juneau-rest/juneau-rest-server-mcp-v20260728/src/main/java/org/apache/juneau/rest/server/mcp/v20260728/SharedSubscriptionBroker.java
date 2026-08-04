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

import org.apache.juneau.rest.server.mcp.BasicMcpSubscriptionBroker;
import org.apache.juneau.rest.server.mcp.McpSubscriptionBroker;

/**
 * Holds the lazily-initialized, per-process shared default {@link McpSubscriptionBroker} returned by the
 * {@link McpEndpoint#subscriptionBroker()} mixin default.
 *
 * <p>
 * The mixin path has no per-endpoint-instance field to memoize a broker onto (it's an interface), and unlike
 * the knobs-only {@link McpSubscriptionsConfig}, a {@link McpSubscriptionBroker} holds live per-connection
 * subscription state that must be shared across every {@code subscriptions/listen} stream a given mixin
 * deployment serves — a fresh broker per call would silently split subscribers and publishers across
 * unrelated registries. Returning this single shared instance instead means the whole JVM shares one
 * subscription registry across every mixin deployment, mirroring {@link SharedMrtrConfig}'s reasoning for why
 * MRTR's per-process default exists.
 *
 * <p>
 * Uses the initialization-on-demand holder idiom: the instance is created the first time {@link #get()} is
 * called &mdash; i.e. the first mixin {@code subscriptionBroker()} call &mdash; and the JVM guarantees that
 * class initialization is thread-safe, so no explicit synchronization is needed.
 */
final class SharedSubscriptionBroker {

	private SharedSubscriptionBroker() {}

	private static final class Holder {
		static final McpSubscriptionBroker INSTANCE =
			new BasicMcpSubscriptionBroker(McpSubscriptionsConfig.DEFAULT_QUEUE_SIZE);
	}

	/**
	 * Returns the per-process shared default subscription broker, creating it on first access.
	 *
	 * @return The shared broker. Never <jk>null</jk>.
	 */
	static McpSubscriptionBroker get() {
		return Holder.INSTANCE;
	}
}
