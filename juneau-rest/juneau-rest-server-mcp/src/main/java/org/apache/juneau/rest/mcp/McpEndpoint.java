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
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin interface that exposes an MCP JSON-RPC endpoint at {@code POST /mcp} on any Juneau REST resource.
 *
 * <p>
 * Implementing classes provide their {@link McpServerConfig} by implementing {@link #getMcpConfig()}; the
 * default {@link #handleMcpRequest(JsonRpcRequest, RestRequest)} method dispatches incoming requests through
 * {@link Mcp#handle(JsonRpcRequest, McpServerConfig, BasicBeanStore)}.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/api")
 * public class MyResource extends BasicRestServlet implements McpEndpoint {
 *     @Override
 *     public McpServerConfig getMcpConfig() {
 *         return new McpServerConfig().addTool(new MyEchoTool());
 *     }
 * }
 * </pre>
 */
public interface McpEndpoint {

	/**
	 * Returns the {@link McpServerConfig} backing this endpoint.
	 *
	 * @return The config. Never {@code null}.
	 */
	McpServerConfig getMcpConfig();

	/**
	 * Default MCP JSON-RPC endpoint handler.
	 *
	 * <p>
	 * Implementations may override this method to customize routing (path / annotations) but must still
	 * call {@link Mcp#handle(JsonRpcRequest, McpServerConfig, BasicBeanStore)} to dispatch.
	 *
	 * @param req JSON-RPC request envelope.
	 * @param restReq The current REST request.
	 * @return The response, or {@code null} for notifications.
	 */
	@RestPost(path = "/mcp")
	default JsonRpcResponse handleMcpRequest(@Content JsonRpcRequest req, RestRequest restReq) {
		var bs = BasicBeanStore.of(restReq.getContext().getBeanStore())
			.addBean(RestRequest.class, restReq);
		return Mcp.handle(req, getMcpConfig(), bs);
	}
}
