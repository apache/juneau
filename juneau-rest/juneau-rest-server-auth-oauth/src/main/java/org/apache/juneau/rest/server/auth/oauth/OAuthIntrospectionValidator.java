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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.commons.utils.*;
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
 * <h2 class='topic'>Usage</h2>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> validator = OAuthIntrospectionValidator.<jsm>create</jsm>()
 * 		.introspectionEndpoint(URI.<jsm>create</jsm>(<js>"https://idp.example.com/oauth2/introspect"</js>))
 * 		.clientId(<js>"api-server"</js>)
 * 		.clientSecret(<js>"..."</js>)
 * 		.audience(<js>"https://api.example.com"</js>)
 * 		.requiredScopes(<js>"read:orders"</js>)
 * 		.build();
 * </p>
 *
 * <h2 class='topic'>Defaults</h2>
 * <ul>
 * 	<li>Audience: required.  {@link Builder#build()} fails unless {@link Builder#audience(String...)} (or
 * 		{@link Builder#resource(String...)}) or an explicit {@link Builder#anyAudience()} is set.
 * 	<li>HTTP timeouts: 5 seconds connect and read.
 * 	<li>Cache TTL: 5 minutes, capped at 1 hour.
 * 	<li>Cache size: 1000 entries.
 * 	<li>Negative cache: inactive or rejected tokens are remembered for 30 seconds.
 * 	<li>Required scopes: none (scope enforcement lives on the validator, not the filter).
 * </ul>
 *
 * <h2 class='section'>See Also:</h2>
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

	/** Default connect/read timeout for the introspection call. */
	public static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(5);

	/** Default time an inactive or rejected token is remembered. */
	public static final Duration DEFAULT_NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);

	private static final int MAX_NEGATIVE_ENTRIES = 1000;

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
		private final Set<String> requiredScopes = st();
		private final Set<String> expectedAudiences = st();
		private Clock clock = Clock.systemUTC();
		private Consumer<HTTPRequest> httpRequestConfigurator;
		private String issuer;
		private boolean anyAudience;
		private Duration httpTimeout = DEFAULT_HTTP_TIMEOUT;
		private Duration negativeCacheTtl = DEFAULT_NEGATIVE_CACHE_TTL;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the introspection endpoint URL.  Required.
		 *
		 * <p>
		 * Each introspection call transmits the client secret (HTTP Basic) and the access token under
		 * validation to this endpoint, so the URL must use <js>"https"</js> or target a loopback host; a
		 * plaintext transport to a non-loopback host is rejected.
		 *
		 * @param value The endpoint URL.  Must not be <jk>null</jk> and must use HTTPS or target a loopback host.
		 * @return This object.
		 */
		public Builder introspectionEndpoint(URI value) {
			introspectionEndpoint = UriUtils.assertSecureOrLoopback(reqnn("value", value));
			return this;
		}

		/**
		 * Sets the OAuth client ID used for HTTP Basic auth against the introspection endpoint.
		 *
		 * @param value The client ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = reqnb("value", value);
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
			reqnb("value", value);
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
			clientSecretSupplier = reqnn("value", value);
			return this;
		}

		/**
		 * Sets a custom token cache.  Defaults to a {@link BoundedLruTokenCache} with 1000 entries.
		 *
		 * @param value The cache.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder tokenCache(TokenCache value) {
			tokenCache = reqnn("value", value);
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
			reqnn("value", value);
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
			reqnn("scopes", scopes);
			for (var s : scopes) {
				reqnb("scope", s);
				requiredScopes.add(s);
			}
			return this;
		}

		/**
		 * Adds expected audience values (RFC 7662 {@code aud}) that a token must carry to be accepted.
		 *
		 * <p>
		 * When at least one value is configured here (via this method or {@link #resource(String...)}), the
		 * introspection response's {@code aud} claim must contain <b>at least one</b> matching value, or the
		 * token is rejected as {@code invalid_token} &mdash; even when {@code active=true} and required scopes
		 * match.  This binds the token to the resource server it was minted for, so a token issued for a
		 * different API cannot be replayed against this one.
		 *
		 * <p>
		 * An audience is required: {@link #build()} fails unless at least one value is configured here or
		 * {@link #anyAudience()} is called.
		 *
		 * @param values The expected audience values.  Must contain at least one non-blank entry.
		 * @return This object.
		 */
		public Builder audience(String... values) {
			reqnn("values", values);
			for (var v : values) {
				reqnb("audience", v);
				expectedAudiences.add(v);
			}
			return this;
		}

		/**
		 * Explicitly disables the audience check.
		 *
		 * <p>
		 * Use only for authorization servers that cannot emit an {@code aud} claim.  Without this call (or an
		 * {@link #audience(String...)} value), {@link #build()} fails.
		 *
		 * @return This object.
		 */
		public Builder anyAudience() {
			anyAudience = true;
			return this;
		}

		/**
		 * Sets the expected token issuer.
		 *
		 * <p>
		 * When set, the introspection response's {@code iss} claim must equal this value or the token is rejected.
		 *
		 * @param value The expected issuer.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder issuer(String value) {
			issuer = reqnb("value", value);
			return this;
		}

		/**
		 * Sets the connect/read timeout of the introspection call.  Defaults to {@link #DEFAULT_HTTP_TIMEOUT 5 seconds}.
		 *
		 * <p>
		 * Applied before {@link #httpRequestConfigurator(Consumer)} runs, so the configurator can override it.
		 *
		 * @param value The timeout.  Must be positive.
		 * @return This object.
		 */
		public Builder httpTimeout(Duration value) {
			reqnn("value", value);
			if (value.isZero() || value.isNegative())
				throw iaex("httpTimeout must be positive");
			httpTimeout = value;
			return this;
		}

		/**
		 * Sets how long an inactive or rejected token is remembered so repeated presentations do not each trigger
		 * an introspection call.  Defaults to {@link #DEFAULT_NEGATIVE_CACHE_TTL 30 seconds}; {@link Duration#ZERO} disables it.
		 *
		 * @param value The TTL.  Must not be negative.
		 * @return This object.
		 */
		public Builder negativeCacheTtl(Duration value) {
			reqnn("value", value);
			if (value.isNegative())
				throw iaex("negativeCacheTtl must not be negative");
			negativeCacheTtl = value;
			return this;
		}

		/**
		 * Adds expected resource-indicator values (RFC 8707).
		 *
		 * <p>
		 * Alias for {@link #audience(String...)}: RFC 8707 resource indicators are echoed back by
		 * conformant authorization servers in the same introspection {@code aud} claim as RFC 7662
		 * audiences, so both methods add to the same expected-value set.
		 *
		 * @param values The expected resource values.  Must contain at least one non-blank entry.
		 * @return This object.
		 */
		public Builder resource(String... values) {
			return audience(values);
		}

		/**
		 * Overrides the {@link Clock} used for cache expiry.  Useful in tests.
		 *
		 * @param value The clock.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = reqnn("value", value);
			return this;
		}

		/**
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} before it is sent.
		 *
		 * <p>
		 * This is the escape hatch for custom HTTP behavior (proxies, TLS overrides,
		 * timeout overrides).  The callback is invoked on every introspection call.
		 *
		 * @param value The configurator callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = reqnn("value", value);
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
			if (expectedAudiences.isEmpty() && ! anyAudience)
				throw new IllegalStateException("OAuthIntrospectionValidator requires audience(...) or an explicit anyAudience()");
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
	private final Set<String> expectedAudiences;
	private final Clock clock;
	private final Consumer<HTTPRequest> httpRequestConfigurator;
	private final String issuer;
	private final Duration httpTimeout;
	private final Duration negativeCacheTtl;
	private final Map<String,NegativeEntry> negativeCache = new LinkedHashMap<>(16, 0.75f, true) {
		private static final long serialVersionUID = 1L;
		@Override
		protected boolean removeEldestEntry(Map.Entry<String,NegativeEntry> eldest) {
			return size() > MAX_NEGATIVE_ENTRIES;
		}
	};

	/** Raw introspection result as stored in the token cache; carries no per-validator decisions. */
	private record RawIntrospection(TokenIntrospectionSuccessResponse response) implements Principal {
		@Override /* Overridden from Principal */
		public String getName() {
			return response.getSubject() == null ? "" : response.getSubject().getValue();
		}
	}

	private record NegativeEntry(String message, String wwwAuthenticate, Instant expires) {}

	private static final class Rejection extends Exception {
		private static final long serialVersionUID = 1L;
		private final String challenge;
		Rejection(String message, String challenge) {
			super(message);
			this.challenge = challenge;
		}
		AuthenticationException toException() {
			return new AuthenticationException(getMessage()).wwwAuthenticate(challenge);
		}
	}

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
		this.requiredScopes = u(cp(b.requiredScopes));
		this.expectedAudiences = u(cp(b.expectedAudiences));
		this.clock = b.clock;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
		this.issuer = b.issuer;
		this.httpTimeout = b.httpTimeout;
		this.negativeCacheTtl = b.negativeCacheTtl;
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
	 * Returns the configured expected audience / resource-indicator set.
	 *
	 * @return An unmodifiable view.  Empty only when {@link Builder#anyAudience()} was used.
	 */
	public Set<String> getExpectedAudiences() {
		return expectedAudiences;
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
		reqnb("token", token);
		var cached = tokenCache.getPrincipal(token);
		if (cached.isPresent() && cached.get() instanceof RawIntrospection raw) {
			// The cache holds only the raw introspection result, so a cache shared with a differently
			// configured validator can never skip this validator's own checks.
			try {
				enforceNotExpired(raw.response());
				return toPrincipal(raw.response());
			} catch (Rejection e) {
				throw e.toException();
			}
		}
		checkNegativeCache(token);
		return introspect(token);
	}

	private static String negativeKey(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private void checkNegativeCache(String token) throws AuthenticationException {
		if (negativeCacheTtl.isZero())
			return;
		NegativeEntry e;
		synchronized (negativeCache) {
			var key = negativeKey(token);
			e = negativeCache.get(key);
			if (e != null && ! e.expires().isAfter(clock.instant())) {
				negativeCache.remove(key);
				e = null;
			}
		}
		if (e != null)
			throw new AuthenticationException(e.message()).wwwAuthenticate(e.wwwAuthenticate());
	}

	private AuthenticationException remember(String token, Rejection r) {
		if (! negativeCacheTtl.isZero()) {
			synchronized (negativeCache) {
				negativeCache.put(negativeKey(token), new NegativeEntry(r.getMessage(), r.challenge, clock.instant().plus(negativeCacheTtl)));
			}
		}
		return r.toException();
	}

	private Principal introspect(String token) throws AuthenticationException {
		var clientAuth = new ClientSecretBasic(new ClientID(clientId),
			new com.nimbusds.oauth2.sdk.auth.Secret(clientSecretSupplier.get()));
		AccessToken accessToken = new BearerAccessToken(token);
		var req = new TokenIntrospectionRequest(introspectionEndpoint, clientAuth, accessToken);
		var http = req.toHTTPRequest();
		var timeoutMs = (int)Math.min(httpTimeout.toMillis(), Integer.MAX_VALUE);
		http.setConnectTimeout(timeoutMs);
		http.setReadTimeout(timeoutMs);
		http.setFollowRedirects(false);
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
			throw remember(token, new Rejection("OAuth introspection error: " + safeErrorCode(resp.toErrorResponse().getErrorObject().getCode()),
				bearerError("invalid_token", safeErrorCode(resp.toErrorResponse().getErrorObject().getCode()))));
		var success = resp.toSuccessResponse();
		if (!success.isActive())
			throw remember(token, new Rejection("OAuth token inactive", bearerError("invalid_token", "token inactive")));
		Principal principal;
		try {
			principal = toPrincipal(success);
		} catch (Rejection e) {
			throw remember(token, e);
		}
		tokenCache.putPrincipal(token, new RawIntrospection(success), resolveTtl(success));
		return principal;
	}

	private Principal toPrincipal(TokenIntrospectionSuccessResponse success) throws Rejection {
		enforceIssuer(success);
		enforceAudience(success);
		var scopes = extractScopes(success);
		enforceRequiredScopes(scopes);
		var claims = buildClaims(success, scopes);
		if (success.getSubject() == null || success.getSubject().getValue().isBlank())
			throw new Rejection("OAuth token has no subject", bearerError("invalid_token", "subject missing"));
		var subject = success.getSubject().getValue();
		return new ClaimsPrincipal(subject, claims);
	}

	private void enforceNotExpired(TokenIntrospectionSuccessResponse success) throws Rejection {
		var exp = success.getExpirationTime();
		if (exp != null && ! exp.toInstant().isAfter(clock.instant()))
			throw new Rejection("OAuth token expired", bearerError("invalid_token", "token expired"));
	}

	private void enforceIssuer(TokenIntrospectionSuccessResponse success) throws Rejection {
		if (issuer == null)
			return;
		var iss = success.getIssuer();
		if (iss == null || ! issuer.equals(iss.getValue()))
			throw new Rejection("OAuth token issuer mismatch", bearerError("invalid_token", "issuer mismatch"));
	}

	private Set<String> extractScopes(TokenIntrospectionSuccessResponse success) {
		var s = success.getScope();
		if (s == null)
			return Collections.emptySet();
		return s.toStringList().stream()
			.filter(v -> v != null && !v.isBlank()) // HTT: v==null branch unreachable; Nimbus Scope.toStringList() never returns null entries
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Rejects the token when an expected audience/resource set is configured and the introspection
	 * response's {@code aud} claim contains none of the expected values.  No-op when
	 * {@link #expectedAudiences} is empty (explicit {@code anyAudience()}).
	 */
	private void enforceAudience(TokenIntrospectionSuccessResponse success) throws Rejection {
		if (expectedAudiences.isEmpty())
			return;
		var tokenAudiences = success.getAudience();
		var matched = tokenAudiences != null && tokenAudiences.stream()
			.map(Audience::getValue)
			.anyMatch(expectedAudiences::contains);
		if (!matched)
			throw new Rejection("OAuth token audience/resource mismatch", bearerError("invalid_token", "audience mismatch"));
	}

	private void enforceRequiredScopes(Set<String> tokenScopes) throws Rejection {
		for (var req : requiredScopes) {
			if (!tokenScopes.contains(req))
				throw new Rejection("Required scope missing: " + req, bearerError("insufficient_scope", "scope " + req + " missing"));
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

	private static String safeErrorCode(String code) {
		if (code == null || code.isEmpty() || ! code.matches("[\\x20\\x21\\x23-\\x5B\\x5D-\\x7E]+"))
			return "invalid_token";
		return code;
	}
}
