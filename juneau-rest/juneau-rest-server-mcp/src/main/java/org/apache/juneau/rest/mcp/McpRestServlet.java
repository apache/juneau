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
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.serializer.annotation.*;

/**
 * Drop-in {@link BasicRestServlet} subclass that exposes a single MCP JSON-RPC endpoint at {@code POST /}.
 *
 * <p>
 * Subclasses provide their {@link McpServerConfig} by overriding {@link #createMcpConfig()} (called once at
 * {@link #init() init} time), or by registering an {@link McpServerConfig} bean in their {@code RestContext}
 * bean store.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/mcp")
 * public class MyMcpServlet extends McpRestServlet {
 *     @Override
 *     protected McpServerConfig createMcpConfig() {
 *         return new McpServerConfig()
 *             .setServerInfo(new Implementation().setName("my-server").setVersion("1.0.0"))
 *             .addTool(new MyEchoTool());
 *     }
 * }
 * </pre>
 *
 * <p>
 * The servlet enables {@code addBeanTypes} on its serializer so {@link Content} and {@link ResourceContents}
 * polymorphic types are tagged with their {@code type} discriminator on the wire.
 *
 * @serial exclude
 */
@Rest
@SerializerConfig(addBeanTypes = "true")
public abstract class McpRestServlet extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	private McpServerConfig config;

	/**
	 * Returns the {@link McpServerConfig} backing this servlet.
	 *
	 * <p>
	 * The default implementation lazily caches the value of {@link #createMcpConfig()}; subclasses needing
	 * per-request reconfiguration may override.
	 *
	 * @return The config. Never {@code null}.
	 */
	public synchronized McpServerConfig getMcpConfig() {
		if (config == null) {
			config = createMcpConfig();
			if (config == null)
				throw new IllegalStateException("createMcpConfig() returned null");
		}
		return config;
	}

	/**
	 * Factory method for the {@link McpServerConfig} backing this servlet.
	 *
	 * <p>
	 * Subclasses must override this method to declare their tools, prompts, and resources.
	 *
	 * @return A non-{@code null} server config.
	 */
	protected abstract McpServerConfig createMcpConfig();

	/**
	 * MCP JSON-RPC endpoint.
	 *
	 * @param req The parsed JSON-RPC request envelope.
	 * @param restReq The HTTP request (used to surface the bean store to handlers).
	 * @return The JSON-RPC response, or {@code null} for notifications (which the framework renders as an
	 *         empty body).
	 */
	@RestPost(path = "/")
	public JsonRpcResponse handleMcp(@Content JsonRpcRequest req, RestRequest restReq) {
		var bs = BasicBeanStore.of(restReq.getContext().getBeanStore())
			.addBean(RestRequest.class, restReq);
		return Mcp.handle(req, getMcpConfig(), bs);
	}
}
