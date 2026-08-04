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

/**
 * App-facing, revision-neutral change-publish SPI for MCP {@code subscriptions/listen}.
 *
 * <p>
 * Injected into tool/resource/prompt handlers via their {@link org.apache.juneau.commons.inject.BeanStore}
 * argument (the same seam {@code RestRequest} and other request-scoped beans use), so application code can
 * announce a change without any dependency on the v2 wire beans or transport. A {@link McpSubscriptionBroker}
 * is the production implementation of this interface.
 */
public interface McpSubscriptions {

	/**
	 * Announces that a specific subscribed resource's content changed.
	 *
	 * @param uri The exact resource URI that changed. Must not be {@code null}.
	 */
	void resourceUpdated(String uri);

	/** Announces that the tool list changed. */
	void toolsListChanged();

	/** Announces that the prompt list changed. */
	void promptsListChanged();

	/** Announces that the resource list changed. */
	void resourcesListChanged();
}
