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

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.cp.*;

/**
 * Static façade over {@link McpDispatcher} for embedders that want a single-line dispatch call.
 *
 * <p>
 * Use this from inside a custom Juneau REST resource that wants to expose an MCP endpoint without
 * subclassing {@link McpRestServlet}:
 *
 * <pre>
 * @Rest(path="/api")
 * public class MyResource extends BasicRestServlet {
 *     @RestPost(path="/mcp")
 *     public JsonRpcResponse mcp(JsonRpcRequest req, RestRequest rr) {
 *         return Mcp.handle(req, getMcpConfig(), rr.getContext().getBeanStore());
 *     }
 *
 *     private McpServerConfig getMcpConfig() { ... }
 * }
 * </pre>
 */
public final class Mcp {

	private static final McpDispatcher DISPATCHER = new McpDispatcher();

	private Mcp() {}

	/**
	 * Dispatches a JSON-RPC request through the shared {@link McpDispatcher} instance.
	 *
	 * @param req JSON-RPC request envelope.
	 * @param config Server config.
	 * @param ctx Per-request bean store.
	 * @return The response, or {@code null} for notification requests.
	 */
	public static JsonRpcResponse handle(JsonRpcRequest req, McpServerConfig config, BasicBeanStore ctx) {
		return DISPATCHER.dispatch(req, config, ctx);
	}
}
