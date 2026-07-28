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

/**
 * Service-provider interface implemented once per MCP protocol revision.
 *
 * <p>
 * A consumer binds exactly one revision at compile time, by extending that revision's abstract
 * servlet or composing its endpoint mixin. There is deliberately no {@code ServiceLoader} wiring
 * and no {@code priority()}/{@code isAvailable()} pair: unlike the transport-provider precedent in
 * {@code juneau-rest-client-apache-httpclient-45}, there is never a set of runtime candidates to
 * choose between.
 *
 * <p>
 * <b>This interface has exactly three methods and must stay that way.</b> {@code default} methods
 * here are reserved for genuinely additive future protocol growth — a hook a later revision needs
 * that earlier revisions can no-op. They are <em>not</em> for utility plumbing: a revision-neutral
 * helper belongs on whichever neutral type already owns the concept (an envelope bean,
 * {@link McpCursor}, {@link McpParamUtils}), never here.
 */
public interface McpRevision {

	/**
	 * The MCP protocol revision string this implementation speaks.
	 *
	 * @return The revision token (e.g. {@code "2025-06-18"}). Never <jk>null</jk>.
	 */
	String protocolVersion();

	/**
	 * Dispatches one JSON-RPC request.
	 *
	 * @param exchange The inbound envelope plus header access. Never <jk>null</jk>.
	 * @param config The neutral handler registry and pagination strategy. Never <jk>null</jk>.
	 * @param ctx The per-request bean store, passed through to handlers. Never <jk>null</jk>.
	 * @return The response, or <jk>null</jk> for a notification request (which the HTTP layer renders
	 * 	as an empty body).
	 */
	JsonRpcResponse dispatch(McpExchange exchange, McpServerConfig config, BeanStore ctx);

	/**
	 * Maps a neutral failure classification to this revision's JSON-RPC error code.
	 *
	 * @param kind The failure classification. Never <jk>null</jk>.
	 * @return The JSON-RPC error code this revision reports for that kind.
	 */
	int errorCode(McpErrorKind kind);
}
