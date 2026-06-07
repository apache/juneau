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
package org.apache.juneau.rest.server.auth.oauth.oidc;

import java.net.*;
import java.util.*;

/**
 * Immutable record produced by {@link OidcDiscoveryClient} carrying the metadata fields a Juneau OAuth /
 * OIDC consumer typically needs.
 *
 * <p>
 * Unknown fields (IdP-specific extensions) are preserved in {@link #extras()} as a read-only
 * {@code Map<String, Object>}.
 *
 * @param issuer The {@code issuer} URI.  Never {@code null}.
 * @param tokenEndpoint The {@code token_endpoint} URI.
 * @param authorizationEndpoint The {@code authorization_endpoint} URI.  May be {@code null} for pure
 * 	machine-to-machine IdPs.
 * @param introspectionEndpoint The RFC 7662 introspection endpoint URI.  May be {@code null}.
 * @param jwksUri The {@code jwks_uri} URI.
 * @param userinfoEndpoint The OIDC {@code userinfo_endpoint} URI.  May be {@code null}.
 * @param endSessionEndpoint The OIDC end-session endpoint URI.  May be {@code null}.
 * @param supportedScopes Scopes the IdP advertises support for.  May be empty.
 * @param extras Unknown / IdP-specific fields.  Read-only map.
 * @since 10.0.0
 */
public record OidcMetadata(
		URI issuer,
		URI tokenEndpoint,
		URI authorizationEndpoint,
		URI introspectionEndpoint,
		URI jwksUri,
		URI userinfoEndpoint,
		URI endSessionEndpoint,
		Set<String> supportedScopes,
		Map<String,Object> extras) {

	/**
	 * Compact constructor that defensively copies the collections.
	 */
	public OidcMetadata {
		Objects.requireNonNull(issuer, "issuer");
		supportedScopes = supportedScopes == null
			? Collections.emptySet()
			: Collections.unmodifiableSet(new LinkedHashSet<>(supportedScopes));
		extras = extras == null
			? Collections.emptyMap()
			: Collections.unmodifiableMap(new LinkedHashMap<>(extras));
	}
}
