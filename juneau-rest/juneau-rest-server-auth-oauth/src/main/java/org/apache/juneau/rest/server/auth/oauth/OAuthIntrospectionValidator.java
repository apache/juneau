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
package org.apache.juneau.rest.server.auth.oauth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.server.auth.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.oauth2.sdk.token.*;

/**
 * {@link TokenValidator} that validates opaque OAuth 2.0 access tokens via the
 * <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7662">RFC 7662</a> Token Introspection
 * endpoint.
 *
 * <p>
 * Wraps Nimbus's {@code TokenIntrospectionRequest} / {@code TokenIntrospectionResponse} for the HTTP
 * round-trip and the JSON parsing.  A {@link TokenCache} (default: bounded LRU cache) short-circuits
 * the round-trip on repeated lookups within the configured TTL window.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> validator = OAuthIntrospectionValidator.<jsm>create</jsm>()
 * 		.introspectionEndpoint(URI.<jsm>create</jsm>(<js>"https://idp.example.com/oauth2/introspect"</js>))
 * 		.clientId(<js>"api-server"</js>)
 * 		.clientSecret(<js>"..."</js>)
 * 		.requiredScopes(<js>"read:orders"</js>)
 * 		.build();
 * </p>
 *
 * <h5 class='topic'>Defaults</h5>
 * <ul>
 * 	<li>Cache TTL: 5 minutes, capped at 1 hour.
 * 	<li>Cache size: 1000 entries.
 * 	<li>Required scopes: none (scope enforcement lives on the validator, not the filter).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link OAuthFilter}
 * 	<li class='jc'>{@link TokenValidator}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7662">RFC 7662 &mdash; OAuth 2.0 Token Introspection</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are OAuth introspection response field names; intentional
})
public class OAuthIntrospectionValidator implements TokenValidator {

	/** Default cache TTL. */
	public static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(5);

	/** Maximum cache TTL. */
	public static final Duration MAX_CACHE_TTL = Duration.ofHours(1);

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder for {@link OAuthIntrospectionValidator}.
	 */
	public static class Builder {
		private URI introspectionEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private TokenCache tokenCache;
		private Duration cacheTtl = DEFAULT_CACHE_TTL;
		private Set<String> requiredScopes = new LinkedHashSet<>();
		private Clock clock = Clock.systemUTC();
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the introspection endpoint URL.  Required.
		 *
		 * @param value The endpoint URL.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder introspectionEndpoint(URI value) {
			introspectionEndpoint = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client ID used for HTTP Basic auth against the introspection endpoint.
		 *
		 * @param value The client ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client secret as a literal string.
		 *
		 * <p>
		 * Mutually exclusive with {@link #clientSecretSupplier(Supplier)}.
		 *
		 * @param value The client secret.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientSecret(String value) {
			assertArgNotNullOrBlank("value", value);
			clientSecretSupplier = () -> value;
			return this;
		}

		/**
		 * Sets the OAuth client secret via a supplier (useful for rotating secrets).
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets a custom token cache.  Defaults to a {@link BoundedLruTokenCache} with 1000 entries.
		 *
		 * @param value The cache.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder tokenCache(TokenCache value) {
			tokenCache = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the cache TTL.  Defaults to {@link #DEFAULT_CACHE_TTL 5 minutes}; capped at
		 * {@link #MAX_CACHE_TTL 1 hour}.  Capped by the token's own {@code exp} when shorter.
		 *
		 * @param value The TTL.  Must be positive and not exceed 1 hour.
		 * @return This object.
		 */
		public Builder cacheTtl(Duration value) {
			assertArgNotNull("value", value);
			if (value.isZero() || value.isNegative())
				throw iaex("cacheTtl must be positive");
			if (value.compareTo(MAX_CACHE_TTL) > 0)
				throw iaex("cacheTtl must not exceed 1 hour (was %s)", value);
			cacheTtl = value;
			return this;
		}

		/**
		 * Adds required scopes (scope enforcement lives on the validator, not the filter).
		 *
		 * <p>
		 * If any required scope is missing from the introspection response's {@code scope} claim, the
		 * validator throws {@link AuthenticationException} with
		 * {@code WWW-Authenticate: Bearer error="insufficient_scope"}.
		 *
		 * @param scopes The required scopes.  Must contain at least one non-blank entry.
		 * @return This object.
		 */
		public Builder requiredScopes(String... scopes) {
			assertArgNotNull("scopes", scopes);
			for (var s : scopes) {
				assertArgNotNullOrBlank("scope", s);
				requiredScopes.add(s);
			}
			return this;
		}

		/**
		 * Overrides the {@link Clock} used for cache expiry.  Useful in tests.
		 *
		 * @param value The clock.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} before it is sent.
		 *
		 * <p>
		 * This is the escape hatch for custom HTTP behavior (proxies, TLS overrides,
		 * timeouts).  The callback is invoked on every introspection call.
		 *
		 * @param value The configurator callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the validator.
		 *
		 * @return A new {@link OAuthIntrospectionValidator}.
		 */
		public OAuthIntrospectionValidator build() {
			if (introspectionEndpoint == null)
				throw new IllegalStateException("OAuthIntrospectionValidator requires introspectionEndpoint(...)");
			if (clientId == null)
				throw new IllegalStateException("OAuthIntrospectionValidator requires clientId(...)");
			if (clientSecretSupplier == null)
				throw new IllegalStateException("OAuthIntrospectionValidator requires clientSecret(...) or clientSecretSupplier(...)");
			if (tokenCache == null)
				tokenCache = BoundedLruTokenCache.create();
			return new OAuthIntrospectionValidator(this);
		}
	}

	private final URI introspectionEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final TokenCache tokenCache;
	private final Duration cacheTtl;
	private final Set<String> requiredScopes;
	private final Clock clock;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OAuthIntrospectionValidator(Builder b) {
		this.introspectionEndpoint = b.introspectionEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.tokenCache = b.tokenCache;
		this.cacheTtl = b.cacheTtl;
		this.requiredScopes = Collections.unmodifiableSet(new LinkedHashSet<>(b.requiredScopes));
		this.clock = b.clock;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Returns the configured introspection endpoint URL.
	 *
	 * @return The endpoint.
	 */
	public URI getIntrospectionEndpoint() {
		return introspectionEndpoint;
	}

	/**
	 * Returns the configured OAuth client ID.
	 *
	 * @return The client ID.
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Returns the cache TTL.
	 *
	 * @return The TTL.
	 */
	public Duration getCacheTtl() {
		return cacheTtl;
	}

	/**
	 * Returns the required-scope set.
	 *
	 * @return An unmodifiable view.
	 */
	public Set<String> getRequiredScopes() {
		return requiredScopes;
	}

	/**
	 * Returns the underlying token cache.
	 *
	 * @return The token cache.
	 */
	public TokenCache getTokenCache() {
		return tokenCache;
	}

	@Override /* Overridden from TokenValidator */
	public Principal validate(String token) throws AuthenticationException {
		assertArgNotNullOrBlank("token", token);
		var cached = tokenCache.getPrincipal(token);
		if (cached.isPresent())
			return cached.get();
		return introspect(token);
	}

	private Principal introspect(String token) throws AuthenticationException {
		var clientAuth = new ClientSecretBasic(new ClientID(clientId),
			new com.nimbusds.oauth2.sdk.auth.Secret(clientSecretSupplier.get()));
		AccessToken accessToken = new BearerAccessToken(token);
		var req = new TokenIntrospectionRequest(introspectionEndpoint, clientAuth, accessToken);
		var http = req.toHTTPRequest();
		if (httpRequestConfigurator != null)
			httpRequestConfigurator.accept(http);

		com.nimbusds.oauth2.sdk.http.HTTPResponse httpResp;
		try {
			httpResp = http.send();
		} catch (IOException e) {
			throw new AuthenticationException(e, "OAuth introspection HTTP call failed")
				.wwwAuthenticate(bearerError("invalid_token", "introspection unreachable"));
		}
		TokenIntrospectionResponse resp;
		try {
			resp = TokenIntrospectionResponse.parse(httpResp);
		} catch (ParseException e) {
			throw new AuthenticationException(e, "OAuth introspection response could not be parsed")
				.wwwAuthenticate(bearerError("invalid_token", "introspection parse failed"));
		}
		if (!resp.indicatesSuccess())
			throw new AuthenticationException("OAuth introspection error: " + resp.toErrorResponse().getErrorObject().getCode())
				.wwwAuthenticate(bearerError("invalid_token", resp.toErrorResponse().getErrorObject().getCode()));
		var success = resp.toSuccessResponse();
		if (!success.isActive())
			throw new AuthenticationException("OAuth token inactive")
				.wwwAuthenticate(bearerError("invalid_token", "token inactive"));
		var scopes = extractScopes(success);
		enforceRequiredScopes(scopes);
		var claims = buildClaims(success, scopes);
		var subject = success.getSubject() != null ? success.getSubject().getValue() : "<no-sub>";
		var principal = new ClaimsPrincipal(subject, claims);
		var ttl = resolveTtl(success);
		tokenCache.putPrincipal(token, principal, ttl);
		return principal;
	}

	private Set<String> extractScopes(TokenIntrospectionSuccessResponse success) {
		var s = success.getScope();
		if (s == null)
			return Collections.emptySet();
		var out = new LinkedHashSet<String>();
		for (var v : s.toStringList())
			if (v != null && !v.isBlank()) // HTT: v==null branch unreachable; Nimbus Scope.toStringList() never returns null entries
				out.add(v);
		return out;
	}

	private void enforceRequiredScopes(Set<String> tokenScopes) throws AuthenticationException {
		for (var req : requiredScopes) {
			if (!tokenScopes.contains(req))
				throw new AuthenticationException("Required scope missing: " + req)
					.wwwAuthenticate(bearerError("insufficient_scope", "scope " + req + " missing"));
		}
	}

	private Map<String,Object> buildClaims(TokenIntrospectionSuccessResponse success, Set<String> scopes) {
		var claims = new LinkedHashMap<String,Object>(success.toJSONObject());
		if (!scopes.isEmpty())
			claims.put("scope", new ArrayList<>(scopes));
		return claims;
	}

	private Duration resolveTtl(TokenIntrospectionSuccessResponse success) {
		var ttl = cacheTtl;
		var exp = success.getExpirationTime();
		if (exp != null) {
			var until = Duration.between(clock.instant(), exp.toInstant());
			if (until.isNegative() || until.isZero())
				until = Duration.ofSeconds(1);
			if (until.compareTo(ttl) < 0)
				ttl = until;
		}
		return ttl;
	}

	private static String bearerError(String code, String description) {
		return "Bearer error=\"" + code + "\", error_description=\"" + description.replace('"', ' ') + "\"";
	}
}
