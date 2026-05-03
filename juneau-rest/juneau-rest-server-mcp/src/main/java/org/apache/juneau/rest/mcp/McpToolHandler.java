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
package org.apache.juneau.rest.mcp;

import java.util.*;

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.cp.*;

/**
 * Handler for a single MCP tool.
 *
 * <p>
 * Implementations declare a {@link #descriptor() descriptor} (the {@link Tool} returned by {@code tools/list})
 * and a {@link #call(Map, BasicBeanStore) call} body invoked when the matching {@code tools/call} method runs.
 *
 * <p>
 * The {@link BasicBeanStore} argument is the per-request bean store, allowing handlers to look up additional
 * services (or the underlying {@code RestRequest}) without making this interface depend on REST runtime types.
 */
@FunctionalInterface
public interface McpToolHandler {

	/**
	 * Returns the static descriptor for this tool.
	 *
	 * <p>
	 * The {@link Tool#getName() name} value is used by {@link McpDispatcher} to route incoming
	 * {@code tools/call} requests, so each handler in an {@link McpServerConfig} must use a unique name.
	 *
	 * @return The tool descriptor. Never {@code null}.
	 */
	default Tool descriptor() {
		throw new UnsupportedOperationException("descriptor() must be implemented by McpToolHandler subclasses.");
	}

	/**
	 * Invokes the tool.
	 *
	 * @param arguments The arguments object passed in the JSON-RPC params (never {@code null}; empty map when omitted).
	 * @param ctx Per-request bean store. Never {@code null}.
	 * @return The call result. Never {@code null}.
	 */
	CallToolResult call(Map<String, Object> arguments, BasicBeanStore ctx);
}
