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
package org.apache.juneau.rest.server.auth.oauth.flow;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.server.auth.oauth.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.token.*;

/**
 * Package-private helpers shared across all flow types.
 *
 * <p>
 * Centralizes the Nimbus {@code TokenRequest}-to-{@code OAuthToken} round-trip plus the standard error
 * mapping.  All flow classes delegate here so the wire-shape handling lives in one place.
 *
 * @since 10.0.0
 */
final class Flows {

	private Flows() {}

	/**
	 * Sends the supplied {@link TokenRequest} via Nimbus's HTTP client and maps the response to
	 * {@link OAuthToken}.
	 *
	 * @param req The Nimbus {@code TokenRequest}.  Never {@code null}.
	 * @param httpConfigurator Optional pre-send hook on the {@link HTTPRequest}.
	 * @return The acquired token.
	 * @throws OAuthFlowException If the IdP returns an error or the HTTP round-trip fails.
	 */
	static OAuthToken send(TokenRequest req, Consumer<HTTPRequest> httpConfigurator) {
		var http = req.toHTTPRequest();
		if (httpConfigurator != null)
			httpConfigurator.accept(http);
		com.nimbusds.oauth2.sdk.http.HTTPResponse httpResp;
		try {
			httpResp = http.send();
		} catch (IOException e) {
			throw new OAuthFlowException("OAuth token endpoint HTTP call failed", e);
		}
		TokenResponse resp;
		try {
			resp = TokenResponse.parse(httpResp);
		} catch (ParseException e) {
			throw new OAuthFlowException("OAuth token-endpoint response could not be parsed", e);
		}
		if (!resp.indicatesSuccess())
			throw new OAuthFlowException("OAuth token endpoint error: "
				+ resp.toErrorResponse().getErrorObject().getCode());
		return toOAuthToken(resp.toSuccessResponse());
	}

	private static OAuthToken toOAuthToken(AccessTokenResponse success) {
		var tokens = success.getTokens();
		var access = tokens.getAccessToken();
		String tokenType = access.getType() != null ? access.getType().getValue() : "Bearer"; // HTT: null branch unreachable; Nimbus AccessToken always has a non-null AccessTokenType
		Instant expiresAt = computeExpiry(access);
		Optional<String> refreshToken = tokens.getRefreshToken() != null
			? o(tokens.getRefreshToken().getValue())
			: oe();
		Optional<Set<String>> scope = access.getScope() != null
			? o(new LinkedHashSet<>(access.getScope().toStringList()))
			: oe();
		Optional<String> idToken = oe();
		var custom = success.getCustomParameters();
		if (custom != null) { // HTT: null branch unreachable; Nimbus returns an empty map (never null) for standard responses
			var v = custom.get("id_token");
			if (v instanceof String v2)
				idToken = o(v2);
		}
		return new OAuthToken(access.getValue(), tokenType, expiresAt, refreshToken, scope, idToken);
	}

	private static Instant computeExpiry(com.nimbusds.oauth2.sdk.token.AccessToken access) {
		var lifetime = access.getLifetime();
		if (lifetime <= 0L)
			return Instant.MAX;
		return Instant.now().plusSeconds(lifetime);
	}

	/**
	 * Helper to coerce Nimbus's {@code BearerAccessToken} cast safely when needed.
	 *
	 * @param token The token.
	 * @return The cast token, or {@code null}.
	 */
	static BearerAccessToken asBearer(com.nimbusds.oauth2.sdk.token.AccessToken token) {
		return token instanceof BearerAccessToken token2 ? token2 : null; // HTT: null return unreachable in current callers; all token-endpoint responses produce BearerAccessToken
	}
}
