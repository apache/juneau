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

import org.apache.juneau.commons.inject.*;

/**
 * Handler for a single MCP resource.
 *
 * <p>
 * Implementations declare a {@link #descriptor() descriptor} (the {@link McpResourceSpec} returned by
 * {@code resources/list}) and a {@link #read(String, BeanStore) read} body invoked when the matching
 * {@code resources/read} method runs.
 */
@FunctionalInterface
public interface McpResourceHandler {

	/**
	 * Returns the static descriptor for this resource.
	 *
	 * <p>
	 * The {@link McpResourceSpec#getUri() uri} value is used by the bound {@link McpRevision} to route
	 * incoming {@code resources/read} requests, so each handler in an {@link McpServerConfig} must use
	 * a unique URI.
	 *
	 * @return The resource descriptor. Never {@code null}.
	 */
	default McpResourceSpec descriptor() {
		throw new UnsupportedOperationException("descriptor() must be implemented by McpResourceHandler subclasses.");
	}

	/**
	 * Reads the resource body.
	 *
	 * @param uri The URI from the {@code resources/read} request (matches {@link #descriptor()}.{@code uri}).
	 * @param ctx Per-request bean store. Never {@code null}.
	 * @return The resource contents. Never {@code null}.
	 */
	McpResourceOutcome read(String uri, BeanStore ctx);
}
