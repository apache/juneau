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
package org.apache.juneau.rest.server.auth.jwt;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.net.*;
import java.security.*;
import java.text.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.auth.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;
import com.nimbusds.jwt.proc.*;

/**
 * {@link TokenValidator} that validates signed JWTs against a JWKS-backed key source.
 *
 * <p>
 * Designed to be paired with {@link BearerTokenGuard} in
 * {@code juneau-rest-server}. It enforces a deliberately strict default policy so a misconfigured
 * call site can't accidentally accept dangerous tokens:
 *
 * <ul>
 * 	<li><b>Algorithm allowlisting</b> — defaults to {@code [RS256, ES256]}. {@code HS256} is rejected
 * 		unless the caller explicitly opts in via
 * 		{@link Builder#algorithms(JWSAlgorithm...) algorithms(...)}. {@code "none"} (unsigned JWTs) is
 * 		<i>permanently</i> rejected — there is no opt-in. This defeats the well-known
 * 		<i>algorithm confusion</i> attack where a JWT is signed with {@code HS256} using the RS256
 * 		public key as the HMAC secret.
 * 	<li><b>Mandatory claims</b> — {@code iss}, {@code aud}, {@code exp}, {@code nbf} are required by
 * 		default. A token missing any of them is rejected.
 * 	<li><b>Clock skew</b> — defaults to 60 seconds tolerance for {@code exp} and {@code nbf}.
 * 		Capped at 300 seconds by the builder.
 * 	<li><b>JWKS caching</b> — keys are fetched and cached for 5 minutes by default. On fetch failure
 * 		the cache continues serving past TTL with a warning logged (see {@link JwksCache}). When a
 * 		fresh-cache selection returns no keys (typically because an IdP rotated its signing key
 * 		mid-TTL), the cache performs one eager out-of-band refresh before failing the request. This
 * 		behavior is <b>enabled by default</b> ({@link Builder#jwksEagerRefreshOnKidMiss(boolean)
 * 		jwksEagerRefreshOnKidMiss(true)}) and bounded by a per-cache cooldown (default 10 seconds)
 * 		plus a single-in-flight guard. Opt out via
 * 		{@link Builder#jwksEagerRefreshOnKidMiss(boolean) jwksEagerRefreshOnKidMiss(false)}.
 * 		Applies only to {@link Builder#jwksUrl(URI) jwksUrl(...)}-backed caches; a
 * 		caller-supplied {@link Builder#jwkSource(JWKSource) jwkSource(...)} is unaffected.
 * </ul>
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> validator = JwtTokenValidator.<jsm>create</jsm>()
 * 		.jwksUrl(URI.<jsm>create</jsm>(<js>"https://issuer.example.com/.well-known/jwks.json"</js>))
 * 		.issuer(<js>"https://issuer.example.com/"</js>)
 * 		.audience(<js>"https://api.example.com"</js>)
 * 		.build();
 *
 * 	<jk>var</jk> guard = BearerTokenGuard.<jsm>create</jsm>()
 * 		.realm(<js>"api"</js>)
 * 		.validator(validator)
 * 		.build();
 * </p>
 *
 * <h5 class='topic'>Security notes</h5>
 *
 * <ul>
 * 	<li>Never disable {@code iss} / {@code aud} validation. The builder enforces them.
 * 	<li>Allowlist {@code HS256} only when both ends of the connection are services you control AND
 * 		you have a secure secret distribution mechanism. JWKS-published JWTs should be asymmetric.
 * 	<li>Rotate JWKS keys on a schedule. {@link JwksCache} picks up new keys after the configured TTL.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link TokenValidator}
 * 	<li class='jc'>{@link BearerTokenGuard}
 * 	<li class='jc'>{@link ClaimsPrincipal}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 — JSON Web Token</a>
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc7517">RFC 7517 — JSON Web Key</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerAuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are JWT claim names and algorithm identifiers; intentional
})
public class JwtTokenValidator implements TokenValidator {

	/** Maximum clock-skew tolerance the builder will accept. */
	private static final Duration MAX_CLOCK_SKEW = Duration.ofSeconds(300);

	/**
	 * Static creator.
	 *
	 * <p>
	 * Routes builder construction through {@link BeanInstantiator} so the
	 * {@link Builder#jwksCacheTtl jwksCacheTtl} default is resolved via {@link Value @Value} from the
	 * active {@link org.apache.juneau.commons.settings.Settings Settings} chain
	 * ({@code juneau.jwt.jwksCacheTtl} property &rarr; {@code PT5M}).
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return create(BasicBeanStore.INSTANCE);
	}

	/**
	 * Static creator with explicit bean store.
	 *
	 * @param beanStore The bean store to use for dependency injection.
	 * @return A new builder.
	 */
	public static Builder create(BeanStore beanStore) {
		return BeanInstantiator.of(Builder.class, beanStore).run();
	}

	/**
	 * Builder class for {@link JwtTokenValidator}.
	 */
	public static class Builder {

		private URI jwksUrl;
		private JWKSource<SecurityContext> jwkSource;
		private String issuer;
		private String audience;
		private Set<JWSAlgorithm> algorithms = new LinkedHashSet<>(Arrays.asList(JWSAlgorithm.RS256, JWSAlgorithm.ES256));
		private Duration clockSkew = Duration.ofSeconds(60);

		/**
		 * JWKS cache TTL; populated via {@link Value @Value} from
		 * {@code juneau.jwt.jwksCacheTtl} (ISO-8601 duration, default {@code PT5M} = 5 minutes).
		 */
		@Value("${juneau.jwt.jwksCacheTtl:PT5M}")
		Duration jwksCacheTtl;

		/**
		 * Whether to perform an eager JWKS refresh on a {@code kid} cache miss; populated via
		 * {@link Value @Value} from {@code juneau.jwt.jwksEagerRefreshOnKidMiss} (boolean, default
		 * {@code true}).
		 */
		@Value("${juneau.jwt.jwksEagerRefreshOnKidMiss:true}")
		boolean jwksEagerRefreshOnKidMiss;

		/**
		 * Minimum interval between eager JWKS refreshes; populated via {@link Value @Value} from
		 * {@code juneau.jwt.jwksEagerRefreshCooldown} (ISO-8601 duration, default {@code PT10S} = 10
		 * seconds).
		 */
		@Value("${juneau.jwt.jwksEagerRefreshCooldown:PT10S}")
		Duration jwksEagerRefreshCooldown;

		private Clock clock = Clock.systemUTC();

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the JWKS endpoint URL the validator fetches keys from.
		 *
		 * <p>
		 * Mutually exclusive with {@link #jwkSource(JWKSource)} — set one or the other.
		 *
		 * @param value The JWKS URL. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwksUrl(URI value) {
			jwksUrl = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets a custom {@link JWKSource} the validator fetches keys from.
		 *
		 * <p>
		 * Mutually exclusive with {@link #jwksUrl(URI)} — set one or the other. Useful for HSM-backed
		 * signers, multi-issuer federation, or unit tests that want to inject a deterministic key
		 * set without binding to a real HTTPS endpoint.
		 *
		 * @param value The JWK source. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwkSource(JWKSource<SecurityContext> value) {
			jwkSource = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the required {@code iss} (issuer) claim value.
		 *
		 * @param value The issuer. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder issuer(String value) {
			issuer = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the required {@code aud} (audience) claim value.
		 *
		 * @param value The audience. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder audience(String value) {
			audience = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the signing-algorithm allowlist.
		 *
		 * <p>
		 * Replaces (not adds to) the default allowlist. Callers wanting {@code HS256} support must
		 * include it explicitly. {@link JWSAlgorithm#NONE} is rejected at build time and at runtime
		 * regardless of what is passed here.
		 *
		 * @param values The allowed algorithms. Must contain at least one element; must not contain
		 * 	{@link JWSAlgorithm#NONE}.
		 * @return This object.
		 */
		public Builder algorithms(JWSAlgorithm...values) {
			assertArgNotNull("values", values);
			if (values.length == 0)
				throw iaex("algorithms allowlist must be non-empty");
			Set<JWSAlgorithm> next = new LinkedHashSet<>();
			for (var a : values) {
				assertArgNotNull("algorithm", a);
				if (Algorithm.NONE.equals(a))
					throw iaex("\"none\" algorithm is permanently rejected (RFC 7518 §3.6 unsafe)");
				next.add(a);
			}
			algorithms = next;
			return this;
		}

		/**
		 * Sets the clock-skew tolerance for {@code exp} / {@code nbf} validation.
		 *
		 * @param value The tolerance. Must be non-negative and not exceed 5 minutes.
		 * @return This object.
		 */
		public Builder clockSkew(Duration value) {
			assertArgNotNull("value", value);
			if (value.isNegative())
				throw iaex("clockSkew must be non-negative");
			if (value.compareTo(MAX_CLOCK_SKEW) > 0)
				throw iaex("clockSkew must not exceed 5 minutes (was %s)", value);
			clockSkew = value;
			return this;
		}

		/**
		 * Sets the JWKS cache TTL.
		 *
		 * @param value The TTL. Must be positive.
		 * @return This object.
		 */
		public Builder jwksCacheTtl(Duration value) {
			assertArgNotNull("value", value);
			if (value.isZero() || value.isNegative())
				throw iaex("jwksCacheTtl must be positive");
			jwksCacheTtl = value;
			return this;
		}

		/**
		 * Enables or disables the eager JWKS refresh that fires on a fresh-cache {@code kid} miss.
		 *
		 * <p>
		 * When {@code true} (the default), the first request that presents a token whose signing
		 * key is not in the current (still-fresh) cache triggers a single out-of-band JWKS refresh.
		 * This closes the rotation window where an IdP publishes a new key mid-TTL and clients would
		 * otherwise see up to {@code jwksCacheTtl} minutes of spurious 401 responses for perfectly
		 * valid tokens. The refresh is bounded by {@link #jwksEagerRefreshCooldown(Duration)} and a
		 * single-in-flight guard (no thundering herd).
		 *
		 * <p>
		 * Applies only to {@link #jwksUrl(URI) jwksUrl(...)}-backed caches. A
		 * caller-supplied {@link #jwkSource(JWKSource) jwkSource(...)} is unaffected.
		 *
		 * @param value {@code true} to enable (default); {@code false} to restore pre-10.0.0 behavior.
		 * @return This object.
		 */
		public Builder jwksEagerRefreshOnKidMiss(boolean value) {
			jwksEagerRefreshOnKidMiss = value;
			return this;
		}

		/**
		 * Sets the minimum spacing between consecutive eager JWKS refreshes.
		 *
		 * <p>
		 * A burst of unknown-{@code kid} tokens (e.g. misconfigured client, replay attack) or a
		 * JWKS-endpoint outage will trigger at most one eager refresh per cooldown window. This
		 * prevents the eager-refresh path from becoming a vector for amplified JWKS-endpoint load.
		 *
		 * <p>
		 * Must be positive, at most 60 seconds, and at most {@link #jwksCacheTtl(Duration)
		 * jwksCacheTtl} (validated at {@link #build()} time).
		 *
		 * @param value The cooldown. Must not be {@code null}.
		 * @return This object.
		 */
		public Builder jwksEagerRefreshCooldown(Duration value) {
			assertArgNotNull("value", value);
			if (value.isZero() || value.isNegative())
				throw iaex("jwksEagerRefreshCooldown must be positive");
			if (value.compareTo(Duration.ofSeconds(60)) > 0)
				throw iaex("jwksEagerRefreshCooldown must not exceed 60 seconds (was %s)", value);
			jwksEagerRefreshCooldown = value;
			return this;
		}

		/**
		 * Overrides the {@link Clock} used for {@code exp} / {@code nbf} comparisons and JWKS cache
		 * TTL. Useful in tests.
		 *
		 * @param value The clock. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the validator.
		 *
		 * @return A new {@link JwtTokenValidator}.
		 */
		public JwtTokenValidator build() {
			if (issuer == null)
				throw isex("JwtTokenValidator requires an issuer");
			if (audience == null)
				throw isex("JwtTokenValidator requires an audience");
			if (jwksUrl == null && jwkSource == null)
				throw isex("JwtTokenValidator requires either jwksUrl(...) or jwkSource(...)");
			if (jwksUrl != null && jwkSource != null)
				throw isex("JwtTokenValidator: jwksUrl(...) and jwkSource(...) are mutually exclusive");
			if (jwksEagerRefreshCooldown.compareTo(jwksCacheTtl) > 0)
				throw isex("jwksEagerRefreshCooldown (%s) must not exceed jwksCacheTtl (%s)", jwksEagerRefreshCooldown, jwksCacheTtl);
			return new JwtTokenValidator(this);
		}
	}

	private final String issuer;
	private final String audience;
	private final Set<JWSAlgorithm> algorithms;
	private final Duration clockSkew;
	private final Clock clock;
	private final JWKSource<SecurityContext> jwkSource;

	/**
	 * Constructor.
	 *
	 * @param builder The builder to read configuration from.
	 */
	protected JwtTokenValidator(Builder builder) {
		this.issuer = builder.issuer;
		this.audience = builder.audience;
		this.algorithms = Collections.unmodifiableSet(new LinkedHashSet<>(builder.algorithms));
		this.clockSkew = builder.clockSkew;
		this.clock = builder.clock;
		this.jwkSource = builder.jwkSource != null
			? builder.jwkSource
			: new JwksCache(createRemoteJwkSource(builder.jwksUrl), builder.jwksCacheTtl, builder.clock,
				builder.jwksEagerRefreshOnKidMiss, builder.jwksEagerRefreshCooldown);
	}

	/**
	 * Returns the issuer this validator requires on tokens.
	 *
	 * @return The configured {@code iss} value.
	 */
	public String getIssuer() {
		return issuer;
	}

	/**
	 * Returns the audience this validator requires on tokens.
	 *
	 * @return The configured {@code aud} value.
	 */
	public String getAudience() {
		return audience;
	}

	/**
	 * Returns the allowlist of signing algorithms.
	 *
	 * @return An unmodifiable copy of the allowlist.
	 */
	public Set<JWSAlgorithm> getAlgorithms() {
		return algorithms;
	}

	/**
	 * Returns the configured clock-skew tolerance.
	 *
	 * @return The clock skew duration.
	 */
	public Duration getClockSkew() {
		return clockSkew;
	}

	@Override /* Overridden from TokenValidator */
	public Principal validate(String token) throws AuthenticationException {
		assertArgNotNull("token", token);
		JWT jwt;
		try {
			jwt = JWTParser.parse(token);
		} catch (ParseException e) {
			throw new AuthenticationException(e, "JWT could not be parsed")
				.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"malformed JWT\"");
		}
		if (! (jwt instanceof SignedJWT jwt2))
			throw new AuthenticationException("JWT must be signed (alg=none and unsigned JWTs are rejected)")
				.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"unsigned JWT\"");
		var alg = jwt2.getHeader().getAlgorithm();
		if (alg == null || Algorithm.NONE.equals(alg))
			throw new AuthenticationException("JWT alg \"none\" is not permitted")
				.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"alg=none rejected\"");
		if (! algorithms.contains(alg))
			throw new AuthenticationException("JWT algorithm not allowlisted: " + alg)
				.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"algorithm not allowed\"");

		// Verify signature.
		var processor = new DefaultJWTProcessor<SecurityContext>();
		processor.setJWSKeySelector(new JWSVerificationKeySelector<>(algorithms, jwkSource));
		// Disable nimbus' internal claims verifier — we run our own below so we control the Clock.
		processor.setJWTClaimsSetVerifier((claims, ctx) -> { /* no-op */ });

		JWTClaimsSet claims;
		try {
			claims = processor.process(jwt2, null);
		} catch (BadJOSEException e) {
			throw new AuthenticationException(e, "JWT validation failed: " + e.getMessage())
				.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"" + sanitize(e.getMessage()) + "\"");
		} catch (JOSEException e) {
			throw new AuthenticationException(e, "JWT signature verification failed")
				.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"signature verification failed\"");
		}

		verifyClaims(claims);

		var subject = claims.getSubject();
		if (isBlank(subject))
			subject = "<no-sub>";
		return new ClaimsPrincipal(subject, claims.toJSONObject());
	}

	private void verifyClaims(JWTClaimsSet claims) throws AuthenticationException {
		// Required claims: iss, aud, exp, nbf are non-negotiable defaults.
		var iss = claims.getIssuer();
		if (isBlank(iss))
			throw reject("\"iss\" claim is required");
		if (! issuer.equals(iss))
			throw reject("\"iss\" claim does not match expected issuer");

		var aud = claims.getAudience();
		if (aud == null || aud.isEmpty())
			throw reject("\"aud\" claim is required");
		if (! aud.contains(audience))
			throw reject("\"aud\" claim does not include expected audience");

		var exp = claims.getExpirationTime();
		if (exp == null)
			throw reject("\"exp\" claim is required");
		var nbf = claims.getNotBeforeTime();
		if (nbf == null)
			throw reject("\"nbf\" claim is required");

		var now = clock.instant();
		if (exp.toInstant().plus(clockSkew).isBefore(now))
			throw reject("token has expired");
		if (nbf.toInstant().minus(clockSkew).isAfter(now))
			throw reject("token not yet valid");
	}

	private static AuthenticationException reject(String reason) {
		return new AuthenticationException("JWT validation failed: " + reason)
			.wwwAuthenticate("Bearer error=\"invalid_token\", error_description=\"" + sanitize(reason) + "\"");
	}

	private static JWKSource<SecurityContext> createRemoteJwkSource(URI jwksUrl) {
		try {
			return JWKSourceBuilder.create(jwksUrl.toURL()).build();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid JWKS URL: " + jwksUrl, e);
		}
	}

	private static String sanitize(String s) {
		if (s == null)
			return "";
		// Don't echo arbitrary characters into a WWW-Authenticate header value.
		return s.replaceAll("[\"\\r\\n]", " ");
	}
}
