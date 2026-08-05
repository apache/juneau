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
package org.apache.juneau.rest.client.mcp.auth.flow;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.*;

/**
 * Package-private helpers shared across all flow types.
 *
 * <p>
 * Relocated from {@code juneau-rest-server-auth-oauth} for client-side use &mdash; see this package's javadoc.
 * Centralizes the Nimbus {@code TokenRequest}-to-{@code OAuthToken} round-trip plus the standard error mapping.
 *
 * @since 10.0.0
 */
final class Flows {

	/** Default connect/read timeout applied to every token-endpoint request when the caller sets none. */
	static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);

	private Flows() {}

	/**
	 * Sends the supplied {@link TokenRequest} via Nimbus's HTTP client and maps the response to
	 * {@link OAuthToken}.
	 *
	 * <p>
	 * A finite connect/read timeout is applied BEFORE the caller's {@code httpConfigurator} runs, so an unresponsive
	 * IdP can never wedge the call indefinitely while still letting callers override the value.
	 *
	 * @param req The Nimbus {@code TokenRequest}.  Never {@code null}.
	 * @param timeout The connect/read timeout.  {@code null} selects {@link #DEFAULT_HTTP_TIMEOUT}.
	 * @param httpConfigurator Optional pre-send hook on the {@link HTTPRequest}.
	 * @return The acquired token.
	 * @throws OAuthFlowException If the IdP returns an error or the HTTP round-trip fails.
	 */
	static OAuthToken send(TokenRequest req, Duration timeout, Consumer<HTTPRequest> httpConfigurator) {
		var http = req.toHTTPRequest();
		applyTimeout(http, timeout);
		if (httpConfigurator != null)
			httpConfigurator.accept(http);
		HTTPResponse httpResp;
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
		if (!resp.indicatesSuccess()) {
			var err = resp.toErrorResponse().getErrorObject();
			var code = (err == null || err.getCode() == null) ? "unknown_error" : err.getCode();
			throw new OAuthFlowException("OAuth token endpoint error: " + code, code);
		}
		return toOAuthToken(resp.toSuccessResponse());
	}

	/**
	 * Applies a finite connect/read timeout to a Nimbus {@link HTTPRequest} (Nimbus defaults to infinite).
	 *
	 * @param http The request.  Never {@code null}.
	 * @param timeout The timeout.  {@code null} selects {@link #DEFAULT_HTTP_TIMEOUT}.
	 */
	static void applyTimeout(HTTPRequest http, Duration timeout) {
		var t = timeout == null ? DEFAULT_HTTP_TIMEOUT : timeout;
		var ms = (int) Math.min(t.toMillis(), Integer.MAX_VALUE);
		http.setConnectTimeout(ms);
		http.setReadTimeout(ms);
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
		var v = custom.get("id_token");
		if (v instanceof String v2)
			idToken = o(v2);
		return new OAuthToken(access.getValue(), tokenType, expiresAt, refreshToken, scope, idToken);
	}

	private static Instant computeExpiry(com.nimbusds.oauth2.sdk.token.AccessToken access) {
		var lifetime = access.getLifetime();
		if (lifetime <= 0L)
			return Instant.MAX;
		return Instant.now().plusSeconds(lifetime);
	}
}
