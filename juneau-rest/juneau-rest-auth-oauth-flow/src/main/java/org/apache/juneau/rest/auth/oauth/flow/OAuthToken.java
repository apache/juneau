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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;

/**
 * Immutable record returned by every OAuth 2.0 flow helper on a successful token acquisition.
 *
 * <p>
 * <b>Never log {@code accessToken} / {@code refreshToken} / {@code idToken}.</b>  {@link #toString()} redacts the
 * secret-bearing fields so token material never reaches logs via this record's auto-generated form.
 *
 * @param accessToken The opaque or JWT access-token string.  Never {@code null}.
 * @param tokenType The token type per RFC 6749 &sect;5.1 (typically {@code "Bearer"}).  Never {@code null}.
 * @param expiresAt The instant at which the access token expires.  May be {@link Instant#MAX} when the
 * 	IdP returned no {@code expires_in}.
 * @param refreshToken The refresh token, if the IdP issued one.
 * @param scope The granted scopes, if the IdP returned a {@code scope} field.
 * @param idToken The OIDC ID token (a JWT), if the flow scope included {@code openid}.  <b>Not validated</b>
 * 	&mdash; see {@link #idToken()}.
 * @since 10.0.0
 */
public record OAuthToken(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		Optional<String> refreshToken,
		Optional<Set<String>> scope,
		Optional<String> idToken) {

	/**
	 * Compact constructor enforcing non-null fields.
	 */
	public OAuthToken {
		Objects.requireNonNull(accessToken, "accessToken");
		Objects.requireNonNull(tokenType, "tokenType");
		Objects.requireNonNull(expiresAt, "expiresAt");
		Objects.requireNonNull(refreshToken, "refreshToken");
		Objects.requireNonNull(scope, "scope");
		Objects.requireNonNull(idToken, "idToken");
		scope = scope.map(s -> u(cp(s)));
	}

	/**
	 * Returns whether the token is past its expiration instant relative to the supplied clock.
	 *
	 * @param now The reference instant (typically {@code Clock.instant()}).
	 * @param skew Skew tolerance (subtracted from {@link #expiresAt}).  Must be non-negative.
	 * @return <jk>true</jk> if {@code now &gt;= expiresAt - skew}.
	 */
	public boolean isExpired(Instant now, Duration skew) {
		Objects.requireNonNull(now, "now");
		Objects.requireNonNull(skew, "skew");
		if (skew.isNegative())
			throw iaex("skew must be non-negative (was %s)", skew);
		return !now.isBefore(expiresAt.minus(skew));
	}

	/**
	 * The OIDC ID token (a JWT), if the flow scope included {@code openid}.
	 *
	 * <p>
	 * <b>SECURITY CAVEAT:</b> this ID token is returned verbatim and is <b>NOT validated</b> by the flow helpers
	 * &mdash; its signature and its {@code iss} / {@code aud} / {@code exp} / {@code nonce} claims are not checked.
	 * Callers MUST NOT trust its claims for authentication or authorization decisions without performing full
	 * {@code IDTokenValidator}-based verification first.
	 *
	 * @return The raw ID token, or an empty {@link Optional} if the IdP issued none.
	 */
	@Override
	public Optional<String> idToken() {
		return idToken;
	}

	/**
	 * Redacts the secret-bearing fields so token material never reaches logs via this record's {@code toString()}.
	 *
	 * @return A redacted string form disclosing only non-secret metadata.
	 */
	@Override
	public String toString() {
		return "OAuthToken[tokenType=" + tokenType
			+ ", expiresAt=" + expiresAt
			+ ", accessToken=<redacted>"
			+ ", refreshToken=" + (refreshToken.isPresent() ? "<redacted>" : "<none>")
			+ ", idToken=" + (idToken.isPresent() ? "<redacted>" : "<none>")
			+ ", scope=" + scope.orElse(Set.of())
			+ "]";
	}
}
