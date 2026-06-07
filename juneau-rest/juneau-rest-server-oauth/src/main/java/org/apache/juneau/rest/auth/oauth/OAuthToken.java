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
package org.apache.juneau.rest.auth.oauth;

import java.time.*;
import java.util.*;

/**
 * Immutable record returned by every OAuth 2.0 flow helper on a successful token acquisition.
 *
 * @param accessToken The opaque or JWT access-token string.  Never {@code null}.
 * @param tokenType The token type per RFC 6749 &sect;5.1 (typically {@code "Bearer"}).  Never {@code null}.
 * @param expiresAt The instant at which the access token expires.  May be {@link Instant#MAX} when the
 * 	IdP returned no {@code expires_in}.
 * @param refreshToken The refresh token, if the IdP issued one.
 * @param scope The granted scopes, if the IdP returned a {@code scope} field.
 * @param idToken The OIDC ID token (a JWT), if the flow scope included {@code openid}.
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
		scope = scope.map(s -> Collections.unmodifiableSet(new LinkedHashSet<>(s)));
	}

	/**
	 * Returns whether the token is past its expiration instant relative to the supplied clock.
	 *
	 * @param now The reference instant (typically {@code Clock.instant()}).
	 * @param skew Skew tolerance (subtracted from {@link #expiresAt}).  Must be non-negative.
	 * @return <jk>true</jk> if {@code now &gt;= expiresAt - skew}.
	 */
	public boolean isExpired(Instant now, java.time.Duration skew) {
		Objects.requireNonNull(now, "now");
		Objects.requireNonNull(skew, "skew");
		return !now.isBefore(expiresAt.minus(skew));
	}
}
