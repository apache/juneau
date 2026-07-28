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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;

/**
 * The inbound JSON-RPC envelope plus request-header access, with no servlet or HTTP types attached.
 *
 * <p>
 * This is the sole argument a revision receives about the transport. It exists so a revision can
 * read protocol headers (later MCP revisions route on them) without the core or the revision
 * depending on a servlet API.
 */
public final class McpExchange {

	private final JsonRpcRequest request;
	private final Function<String,String> headers;

	/**
	 * Constructor.
	 *
	 * @param request The bound JSON-RPC request envelope. Can be <jk>null</jk> (an unparseable or
	 * 	absent body), which a revision must report as an invalid request.
	 * @param headers Header lookup by name, returning <jk>null</jk> for an absent header. Must not
	 * 	be <jk>null</jk>.
	 */
	public McpExchange(JsonRpcRequest request, Function<String,String> headers) {
		assertArgNotNull("headers", headers);
		this.request = request;
		this.headers = headers;
	}

	/**
	 * The inbound JSON-RPC request envelope.
	 *
	 * @return The envelope, or <jk>null</jk> if the body was absent or unparseable.
	 */
	public JsonRpcRequest request() {
		return request;
	}

	/**
	 * Looks up a request header.
	 *
	 * @param name The header name. Can be <jk>null</jk>.
	 * @return The header value, or <jk>null</jk> if absent.
	 */
	public String header(String name) {
		return headers.apply(name);
	}
}
