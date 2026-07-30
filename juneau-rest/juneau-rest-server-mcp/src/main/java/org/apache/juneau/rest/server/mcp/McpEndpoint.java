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

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Content;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin interface that exposes an MCP JSON-RPC endpoint at {@code POST /mcp} on any Juneau REST resource.
 *
 * <p>
 * Implementing classes supply their {@link McpServerConfig} via {@link #getMcpConfig()} and their
 * protocol revision via {@link #revision()}. In practice a consumer implements a revision-specific
 * sub-interface (for example {@code org.apache.juneau.rest.server.mcp.v20250618.McpEndpoint}), which supplies {@link #revision()} for them.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/api")
 * public class MyResource extends BasicRestServlet implements org.apache.juneau.rest.server.mcp.v20250618.McpEndpoint {
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
	 * The MCP protocol revision this endpoint speaks.
	 *
	 * @return The bound revision. Never {@code null}.
	 */
	McpRevision revision();

	/**
	 * Default MCP JSON-RPC endpoint handler.
	 *
	 * <p>
	 * Implementations may override this method to customize routing (path / annotations) but must
	 * still dispatch through {@link #revision()}.
	 *
	 * <p>
	 * Applies the same serializer policy as {@link McpRestServlet} ({@code addBeanTypes} and
	 * {@code uriResolution="NONE"}), so the two neutral HTTP entrypoints stay at parity.
	 *
	 * @param req JSON-RPC request envelope.
	 * @param restReq The current REST request.
	 * @return The response, or {@code null} for notifications.
	 */
	@SuppressWarnings({
		"resource" // Request-scoped scratch BasicBeanStore; lifetime is bounded by this handler invocation, no foreign resources are captured.
	})
	@SerializerConfig(addBeanTypes = "true", uriResolution = "NONE")
	@RestPost(path = "/mcp")
	default JsonRpcResponse handleMcpRequest(@Content JsonRpcRequest req, RestRequest restReq) {
		var bs = new BasicBeanStore(restReq.getContext().getBeanStore())
			.addBean(RestRequest.class, restReq);
		var exchange = new McpExchange(req, n -> restReq.getHeaderParam(n).asString().orElse(null));
		return revision().dispatch(exchange, getMcpConfig(), bs);
	}
}
