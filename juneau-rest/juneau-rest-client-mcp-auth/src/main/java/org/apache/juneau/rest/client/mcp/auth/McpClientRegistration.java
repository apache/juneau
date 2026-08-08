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
import java.time.*;
import java.util.*;

/**
 * Immutable result of a Dynamic Client Registration (SEP-837) round-trip &mdash; the client credentials and metadata an
 * authorization server issued, keyed by the AS {@code issuer} for SEP-2352 credential binding.
 *
 * <p>
 * <b>Never log {@code clientSecret} / {@code registrationAccessToken}.</b>  {@link #toString()} redacts both
 * secret-bearing fields (mirroring the {@code OAuthToken} / {@code KeyedSecret} redaction discipline) so credential
 * material never reaches logs via this record's auto-generated form.
 *
 * @param clientId The registered {@code client_id}.  Never {@code null}.
 * @param clientSecret The issued {@code client_secret}, if the AS issued one (absent for a public/native client).
 * @param clientSecretExpiresAt The instant the {@code client_secret} expires, if the AS returned a non-zero
 * 	{@code client_secret_expires_at}.
 * @param registrationAccessToken The RFC 7592 registration access token, if the AS issued one.
 * @param registrationClientUri The RFC 7592 registration client URI, if the AS returned one.
 * @param issuer The authorization server {@code issuer} these credentials were registered with (the SEP-2352 key).
 * 	Never {@code null}.
 * @param redirectUris The redirect URIs that were registered, in registration order.  Never {@code null}.
 * @param applicationType The declared {@code application_type}.  Never {@code null}.
 * @param extras Any non-standard fields the AS returned.  Read-only map.  Never {@code null}.
 * @since 10.0.0
 */
public record McpClientRegistration(
		String clientId,
		Optional<String> clientSecret,
		Optional<Instant> clientSecretExpiresAt,
		Optional<String> registrationAccessToken,
		Optional<URI> registrationClientUri,
		URI issuer,
		List<URI> redirectUris,
		McpApplicationType applicationType,
		Map<String,Object> extras) {

	/** Placeholder shown for an absent secret-bearing / optional field in {@link #toString()}. */
	private static final String NONE = "<none>";

	/**
	 * Compact constructor enforcing non-null fields and defensively copying the collections.
	 */
	public McpClientRegistration {
		Objects.requireNonNull(clientId, "clientId");
		Objects.requireNonNull(clientSecret, "clientSecret");
		Objects.requireNonNull(clientSecretExpiresAt, "clientSecretExpiresAt");
		Objects.requireNonNull(registrationAccessToken, "registrationAccessToken");
		Objects.requireNonNull(registrationClientUri, "registrationClientUri");
		Objects.requireNonNull(issuer, "issuer");
		Objects.requireNonNull(applicationType, "applicationType");
		redirectUris = redirectUris == null ? List.of() : u(cp(redirectUris));
		extras = extras == null ? Map.of() : u(cp(extras));
	}

	/**
	 * Whether these credentials include a client secret (a confidential client) versus a public/native client that
	 * authenticates with PKCE alone.
	 *
	 * @return <jk>true</jk> if a {@code client_secret} was issued.
	 */
	public boolean isConfidential() {
		return clientSecret.isPresent();
	}

	/**
	 * Redacts the secret-bearing fields so credential material never reaches logs via this record's
	 * {@code toString()}.
	 *
	 * @return A redacted string form disclosing only non-secret metadata.
	 */
	@Override
	public String toString() {
		return "McpClientRegistration[clientId=" + clientId
			+ ", issuer=" + issuer
			+ ", applicationType=" + applicationType
			+ ", redirectUris=" + redirectUris
			+ ", clientSecret=" + (clientSecret.isPresent() ? "<redacted>" : NONE)
			+ ", clientSecretExpiresAt=" + clientSecretExpiresAt.map(Object::toString).orElse(NONE)
			+ ", registrationAccessToken=" + (registrationAccessToken.isPresent() ? "<redacted>" : NONE)
			+ ", registrationClientUri=" + registrationClientUri.map(Object::toString).orElse(NONE)
			+ ", extras=" + extras
			+ "]";
	}
}
