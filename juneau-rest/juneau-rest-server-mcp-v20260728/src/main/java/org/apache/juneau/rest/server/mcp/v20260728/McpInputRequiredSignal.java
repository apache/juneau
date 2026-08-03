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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Thrown by a handler's ordinary {@code call}/{@code get}/{@code read} body to signal that it needs more input
 * before it can complete (MCP {@code 2026-07-28} SEP-2322 Multi-Round-Trip Requests).
 *
 * <p>
 * Caught (and turned into an {@code input_required} result) <b>only</b> on the three MRTR-wired in-scope paths:
 * {@link McpRevision}'s {@code callTool}/{@code getPrompt}/{@code readResource}-exact-resource branches. Any other
 * handler path is <b>not</b> MRTR-wired &mdash; the resource-<i>template</i> handler, a {@code completion/complete}
 * completer, and the list/discover/ping paths all run outside those try/catch blocks. A signal thrown from one of
 * them is not caught here; it propagates to {@link McpRevision#dispatch}'s generic error branch and surfaces as the
 * {@code -32603} internal-error fail-safe (a misuse outcome, not an {@code input_required} result). This is the
 * spec's Open item #4: MRTR engages by construction only for the three scoped seams, so an out-of-scope pause is a
 * handler bug the fail-safe contains rather than a supported flow.
 *
 * <p>
 * Deliberately <b>not</b> an {@link org.apache.juneau.bean.jsonrpc.McpException}: it is not itself a JSON-RPC
 * error, it is an internal dispatch-control signal that gets turned into a successful {@code input_required}
 * result.
 */
public class McpInputRequiredSignal extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient Map<String,Object> inputRequests;
	private final transient Object continuation;

	/**
	 * Constructor.
	 *
	 * @param inputRequests Server-assigned-id-keyed map of requested inputs. Must not be <jk>null</jk> or empty.
	 * 	Each value must be a sub-request object ({@code Map}, per the pinned schema); it is carried to the wire
	 * 	byte-for-byte (a non-map value is a handler bug the dispatcher's {@code -32603} fail-safe contains).
	 * @param continuation Handler-opaque continuation value. Can be <jk>null</jk> if the handler needs no
	 * 	state carried between rounds beyond the original request itself.
	 * @throws IllegalArgumentException If {@code inputRequests} is <jk>null</jk> or empty.
	 */
	public McpInputRequiredSignal(Map<String,Object> inputRequests, Object continuation) {
		super("Handler requested more input");
		if (inputRequests == null || inputRequests.isEmpty())
			throw iaex("inputRequests must not be null or empty");
		this.inputRequests = inputRequests;
		this.continuation = continuation;
	}

	/**
	 * The requested inputs.
	 *
	 * @return The map. Never <jk>null</jk> or empty.
	 */
	public Map<String,Object> getInputRequests() {
		return inputRequests;
	}

	/**
	 * The handler-opaque continuation.
	 *
	 * @return The continuation, or <jk>null</jk> if the handler supplied none.
	 */
	public Object getContinuation() {
		return continuation;
	}
}
