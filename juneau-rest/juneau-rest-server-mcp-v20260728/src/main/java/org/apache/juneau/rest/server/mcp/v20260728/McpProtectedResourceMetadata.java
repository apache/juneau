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

import java.net.*;
import java.util.*;

import org.apache.juneau.commons.bean.BeanType;
import org.apache.juneau.commons.bean.PropertyNamerULC;

/**
 * Server-side representation of an <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a>
 * Protected Resource Metadata (PRM) document, emitted by the {@code 2026-07-28} MCP resource server from its
 * {@code .well-known/oauth-protected-resource} endpoint so a client can discover which authorization server(s) protect
 * this MCP server.
 *
 * <p>
 * The {@link BeanType#propertyNamer() property namer} maps the camel-case Java properties to the snake-case wire field
 * names RFC 9728 mandates ({@code resource}, {@code authorization_servers}, {@code scopes_supported},
 * {@code bearer_methods_supported}), so Juneau bean serialization produces JSON that parses cleanly on the client side
 * via {@code org.apache.juneau.rest.client.mcp.auth.McpProtectedResourceMetadata} /
 * {@code McpProtectedResourceMetadataClient}.
 *
 * <h5 class='section'>Q5 client/server split:</h5>
 * <p>
 * This intentionally duplicates the shape of the client-side
 * {@code org.apache.juneau.rest.client.mcp.auth.McpProtectedResourceMetadata} record (accepted per READY-312f Q5); the
 * two live in different modules with different dependency footprints (the client parses via Nimbus; the server emits via
 * Juneau marshalling).  Sub-project D later rationalizes a shared, dependency-neutral bean.
 *
 * @since 10.0.0
 */
@BeanType(propertyNamer = PropertyNamerULC.class)
public class McpProtectedResourceMetadata {

	private URI resource;
	private List<URI> authorizationServers;
	private Set<String> scopesSupported;
	private Set<String> bearerMethodsSupported;

	/**
	 * The {@code resource} field &mdash; this server's canonical resource identifier (RFC 9728 &sect;2).
	 *
	 * @return The resource identifier, or <jk>null</jk> if not set.
	 */
	public URI getResource() {
		return resource;
	}

	/**
	 * Sets the {@code resource} identifier.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	public McpProtectedResourceMetadata setResource(URI value) {
		resource = value;
		return this;
	}

	/**
	 * The {@code authorization_servers} field &mdash; the issuer URIs of the authorization servers that mint tokens for
	 * this resource (RFC 9728 &sect;2).
	 *
	 * @return The authorization-server issuer URIs, or <jk>null</jk> if not set.
	 */
	public List<URI> getAuthorizationServers() {
		return authorizationServers;
	}

	/**
	 * Sets the {@code authorization_servers} list.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	public McpProtectedResourceMetadata setAuthorizationServers(List<URI> value) {
		authorizationServers = value;
		return this;
	}

	/**
	 * The {@code scopes_supported} field &mdash; the OAuth scopes this resource understands (RFC 9728 &sect;2).
	 *
	 * @return The supported scopes, or <jk>null</jk> if not set.
	 */
	public Set<String> getScopesSupported() {
		return scopesSupported;
	}

	/**
	 * Sets the {@code scopes_supported} set.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	public McpProtectedResourceMetadata setScopesSupported(Set<String> value) {
		scopesSupported = value;
		return this;
	}

	/**
	 * The {@code bearer_methods_supported} field &mdash; the RFC 6750 methods by which a bearer token may be presented
	 * (RFC 9728 &sect;2).  This server accepts the {@code header} method only.
	 *
	 * @return The supported bearer methods, or <jk>null</jk> if not set.
	 */
	public Set<String> getBearerMethodsSupported() {
		return bearerMethodsSupported;
	}

	/**
	 * Sets the {@code bearer_methods_supported} set.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	public McpProtectedResourceMetadata setBearerMethodsSupported(Set<String> value) {
		bearerMethodsSupported = value;
		return this;
	}
}
