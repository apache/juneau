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
package org.apache.juneau.rest.client.mcp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;

/**
 * Dispatches server-initiated JSON-RPC requests delivered over an MCP client's duplex event-stream channel to a
 * single registered {@link McpServerRequestHandler}.
 *
 * <p>
 * Requests are handed to the registered handler untouched, with their {@code params} left as the generic,
 * revision-neutral wire representation - never projected into a typed request bean - since the set of
 * server-initiated methods (for example {@code sampling/createMessage}; illustrative only, as sampling is
 * deprecated per SEP-2577) is revision- and deployment-specific and this dispatcher has no knowledge of it.
 *
 * @since 10.0.0
 */
public class McpDuplexDispatcher {

	// Argument name constants for assertArgNotNull
	private static final String ARG_REQUEST = "request";

	private final AtomicReference<McpServerRequestHandler> handler = new AtomicReference<>();

	/**
	 * Registers (or de-registers) the handler invoked by {@link #dispatch(JsonRpcRequest, BeanStore)}.
	 *
	 * @param value The handler to register. Passing <jk>null</jk> de-registers any previously-registered handler -
	 * 	a subsequent {@link #dispatch(JsonRpcRequest, BeanStore)} call then behaves as if no handler was ever
	 * 	registered (see the "no handler" behavior documented there).
	 * @return This object.
	 */
	public McpDuplexDispatcher register(McpServerRequestHandler value) {
		handler.set(value);
		return this;
	}

	/**
	 * Dispatches a single server-initiated request to the registered handler.
	 *
	 * <p>
	 * When no handler is registered: a notification (a <jk>null</jk> {@link JsonRpcRequest#getId() id}) is
	 * silently dropped (returns <jk>null</jk>); a request is rejected with a {@code -32601} {@link McpException}.
	 *
	 * @param request The inbound request. Must not be <jk>null</jk>.
	 * @param ctx The request-scoped bean store to hand to the handler. Can be <jk>null</jk>, in which case
	 * 	{@link BasicBeanStore#INSTANCE} is used instead.
	 * @return The handler's result, or <jk>null</jk> for a notification with no registered handler.
	 * @throws McpException If no handler is registered and {@code request} is not a notification, or if the
	 * 	handler itself throws (an {@link McpException} thrown by the handler propagates as-is; any other
	 * 	exception is wrapped as a {@code -32603} internal-error {@link McpException} with the original
	 * 	exception preserved as its cause).
	 */
	public Object dispatch(JsonRpcRequest request, BeanStore ctx) {
		assertArgNotNull(ARG_REQUEST, request);
		var h = handler.get();
		if (h == null) {
			if (JsonRpcResponse.notification(request.getId()))
				return null;
			throw new McpException(-32601, String.format("No server-request handler registered for method '%s'", request.getMethod()));
		}
		try {
			return h.handle(request, ctx == null ? BasicBeanStore.INSTANCE : ctx);
		} catch (McpException e) {
			throw e;
		} catch (Exception e) {
			var e2 = new McpException(-32603, "Internal error handling server request '" + request.getMethod() + "'");
			e2.initCause(e);
			throw e2;
		}
	}
}
