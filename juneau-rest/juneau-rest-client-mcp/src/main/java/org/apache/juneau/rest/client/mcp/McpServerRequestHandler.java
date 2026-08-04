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

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;

/**
 * Callback invoked by {@link McpDuplexDispatcher} to handle a single server-initiated JSON-RPC request
 * delivered over an MCP client's duplex event-stream channel (for example {@code sampling/createMessage};
 * illustrative only, as sampling is deprecated per SEP-2577).
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface McpServerRequestHandler {

	/**
	 * Handles a single server-initiated request.
	 *
	 * @param request The inbound request, with its {@code params} left as the generic, revision-neutral wire
	 * 	representation. Never <jk>null</jk>.
	 * @param ctx The request-scoped bean store. Never <jk>null</jk>.
	 * @return The result to return to the server, marshaled as the {@code result} of the JSON-RPC response
	 * 	{@link McpDuplexDispatcher} posts back. Can be <jk>null</jk>.
	 * @throws Exception If handling fails. An {@link McpException} thrown here propagates to the caller as-is;
	 * 	any other exception is wrapped by {@link McpDuplexDispatcher#dispatch(JsonRpcRequest, BeanStore)} as a
	 * 	{@code -32603} internal-error {@link McpException}.
	 */
	Object handle(JsonRpcRequest request, BeanStore ctx) throws Exception;
}
