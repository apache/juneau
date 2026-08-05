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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;

/**
 * Immutable view of an RFC 9728 Protected Resource Metadata (PRM) document, as an MCP client consumes it to discover
 * which authorization server(s) protect a given MCP server.
 *
 * <p>
 * Only the fields the client needs are surfaced; unknown fields are preserved in {@link #extras()}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a>
 * </ul>
 *
 * @param resource The {@code resource} identifier (the canonical URI of the protected MCP server).  Never {@code null}.
 * @param authorizationServers The {@code authorization_servers} issuer URIs, in document order.  May be empty.
 * @param scopesSupported The {@code scopes_supported} values, in document order.  May be empty.
 * @param extras Unknown / server-specific fields.  Read-only map.
 * @since 10.0.0
 */
public record McpProtectedResourceMetadata(
		URI resource,
		List<URI> authorizationServers,
		Set<String> scopesSupported,
		Map<String,Object> extras) {

	/**
	 * Compact constructor enforcing a non-null {@code resource} and defensively copying the collections.
	 */
	public McpProtectedResourceMetadata {
		Objects.requireNonNull(resource, "resource");
		authorizationServers = authorizationServers == null ? List.of() : u(cp(authorizationServers));
		scopesSupported = scopesSupported == null ? Set.of() : u(cp(scopesSupported));
		extras = extras == null ? Map.of() : u(cp(extras));
	}

	/**
	 * Returns the first advertised authorization server, if any.
	 *
	 * <p>
	 * Per RFC 9728 &sect;7.6 the client selects the authorization server; selecting the first entry is compliant
	 * (READY-312f Q8) and no mandatory alternate-AS fallback is required.
	 *
	 * @return The first authorization-server issuer URI, or {@link Optional#empty()} if none are advertised.
	 */
	public Optional<URI> firstAuthorizationServer() {
		return authorizationServers.isEmpty() ? oe() : o(authorizationServers.get(0));
	}
}
