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
 *
 * <p>
 * Named {@code *Mixin} (not {@code Abstract*}) because this is an interface, not an abstract class -
 * {@code Abstract*} is this repo's naming convention for abstract classes only. {@link McpRevision}, the
 * other core type dated subclasses de-versioning-shadow, deliberately keeps its plain name: end-users
 * never implement it directly (only a bound revision's own subclass does), so it never faces the
 * same-simple-name collision this mixin interface was renamed to avoid.
 */
public interface McpEndpointMixin {

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
	 * Optional subscription broker backing this endpoint's {@code subscriptions/listen} support.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) means this endpoint does not support subscriptions: no
	 * {@link McpSubscriptionBroker} or {@link McpSubscriptions} bean is added to the request-scoped
	 * {@link BeanStore} that {@link #handleMcpRequest} builds.
	 *
	 * <p>
	 * A revision binding that implements {@code subscriptions/listen} (for example
	 * {@code org.apache.juneau.rest.server.mcp.v20260728.McpEndpoint}) overrides this to return a
	 * binding-owned broker instance (its own per-binding vs. shared-instance semantics are that
	 * revision's own choice — see its Javadoc).
	 *
	 * @return The subscription broker, or <jk>null</jk> if this endpoint does not support subscriptions.
	 */
	default McpSubscriptionBroker subscriptionBroker() {
		return null;
	}

	/**
	 * Default MCP JSON-RPC endpoint handler.
	 *
	 * <p>
	 * Implementations may override this method to customize routing (path / annotations) but must
	 * still dispatch through {@link #revision()}.
	 *
	 * <p>
	 * Applies the same serializer policy as {@link AbstractMcpRestServlet} ({@code addBeanTypes} and
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
	default Object handleMcpRequest(@Content JsonRpcRequest req, RestRequest restReq) {
		var bs = new BasicBeanStore(restReq.getContext().getBeanStore())
			.addBean(RestRequest.class, restReq);
		var broker = subscriptionBroker();
		if (broker != null)
			bs.addBean(McpSubscriptionBroker.class, broker).addBean(McpSubscriptions.class, broker);
		var exchange = new McpExchange(req, n -> restReq.getHeaderParam(n).asString().orElse(null));
		var result = revision().dispatch(exchange, getMcpConfig(), bs);
		if (result instanceof McpResponseResult r)
			return r.response();
		if (result instanceof McpStreamResult r)
			return r.stream();
		return null;
	}
}
