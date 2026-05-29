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
package org.apache.juneau.rest.auth.oidc.rp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.net.*;
import java.text.ParseException;
import java.util.*;

import org.apache.juneau.rest.auth.AuthenticationException;
import org.apache.juneau.rest.auth.ClaimsPrincipal;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;
import com.nimbusds.oauth2.sdk.id.*;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

/**
 * Wraps Nimbus's {@link IDTokenValidator} to validate an OpenID Connect ID token's signature and claims
 * and project the verified claims onto a Juneau {@link ClaimsPrincipal}.
 *
 * <p>
 * Nimbus's {@code IDTokenValidator} performs the full OIDC ID-token validation in one call: signature
 * verification against the IdP JWKS, exact {@code iss} match, {@code aud} contains the client id,
 * {@code azp} when there are multiple audiences, {@code exp} (with clock skew), required {@code iat},
 * and &mdash; when supplied &mdash; {@code nonce} match.  This adapter owns only the configuration and
 * the projection to {@link ClaimsPrincipal}; no bespoke crypto.
 *
 * <p>
 * The signing-algorithm allowlist defaults to {@code [RS256, ES256]} (both SHA-256 based).  The unsafe
 * {@code "none"} algorithm is permanently rejected (a token signed with {@code none} has no key and so
 * fails the key selector), and SHA-1-family algorithms are excluded by virtue of not being on the
 * allowlist &mdash; consistent with the {@code juneau-rest-server-jwt} strict-default stance.
 *
 * <p>
 * For production, configure {@link Builder#jwksUri(URI)} (Nimbus builds a cached remote key source).
 * For tests / HSM-backed signers / multi-issuer setups, inject a {@link Builder#jwkSet(JWKSet)} or
 * {@link Builder#jwkSource(JWKSource)} directly.
 *
 * @since 9.5.0
 */
public class IdTokenValidatorAdapter {

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder.
	 */
	public static class Builder {
		private String issuer;
		private String clientId;
		private URI jwksUri;
		private JWKSet jwkSet;
		private JWKSource<SecurityContext> jwkSource;
		private Set<JWSAlgorithm> algorithms = new LinkedHashSet<>(Arrays.asList(JWSAlgorithm.RS256, JWSAlgorithm.ES256));
		private int maxClockSkewSeconds = 60;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the expected {@code iss} (issuer).  Required.
		 *
		 * @param value The issuer.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder issuer(String value) {
			issuer = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client id (expected {@code aud}).  Required.
		 *
		 * @param value The client id.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the JWKS endpoint URI the validator fetches signing keys from.  Mutually exclusive with
		 * {@link #jwkSet(JWKSet)} / {@link #jwkSource(JWKSource)}.
		 *
		 * @param value The JWKS URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwksUri(URI value) {
			jwksUri = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets a fixed JWK set (useful for tests / static keys).  Mutually exclusive with
		 * {@link #jwksUri(URI)} / {@link #jwkSource(JWKSource)}.
		 *
		 * @param value The JWK set.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwkSet(JWKSet value) {
			jwkSet = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets a custom {@link JWKSource}.  Mutually exclusive with {@link #jwksUri(URI)} /
		 * {@link #jwkSet(JWKSet)}.
		 *
		 * @param value The JWK source.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder jwkSource(JWKSource<SecurityContext> value) {
			jwkSource = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the signing-algorithm allowlist.  Replaces the default {@code [RS256, ES256]}.
		 *
		 * @param values The allowed algorithms.  Must be non-empty and must not contain
		 * 	{@link JWSAlgorithm#NONE}.
		 * @return This object.
		 */
		public Builder algorithms(JWSAlgorithm...values) {
			assertArgNotNull("values", values);
			assertArg(values.length > 0, "algorithms allowlist must be non-empty");
			var next = new LinkedHashSet<JWSAlgorithm>();
			for (var a : values) {
				assertArgNotNull("algorithm", a);
				assertArg(! JWSAlgorithm.NONE.equals(a), "\"none\" algorithm is permanently rejected");
				next.add(a);
			}
			algorithms = next;
			return this;
		}

		/**
		 * Sets the clock-skew tolerance (in seconds) for {@code exp} / {@code iat} validation.
		 *
		 * @param value The tolerance in seconds.  Must be non-negative.
		 * @return This object.
		 */
		public Builder maxClockSkewSeconds(int value) {
			assertArg(value >= 0, "maxClockSkewSeconds must be non-negative (was {0})", value);
			maxClockSkewSeconds = value;
			return this;
		}

		/**
		 * Builds the adapter.
		 *
		 * @return A new {@link IdTokenValidatorAdapter}.
		 */
		public IdTokenValidatorAdapter build() {
			if (issuer == null)
				throw new IllegalStateException("IdTokenValidatorAdapter requires issuer(...)");
			if (clientId == null)
				throw new IllegalStateException("IdTokenValidatorAdapter requires clientId(...)");
			var sources = 0;
			if (jwksUri != null) sources++;
			if (jwkSet != null) sources++;
			if (jwkSource != null) sources++;
			if (sources != 1)
				throw new IllegalStateException("IdTokenValidatorAdapter requires exactly one of jwksUri(...), jwkSet(...), or jwkSource(...)");
			return new IdTokenValidatorAdapter(this);
		}
	}

	private final IDTokenValidator validator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected IdTokenValidatorAdapter(Builder b) {
		JWKSource<SecurityContext> source = resolveSource(b);
		var keySelector = new JWSVerificationKeySelector<SecurityContext>(b.algorithms, source);
		this.validator = new IDTokenValidator(new Issuer(b.issuer), new ClientID(b.clientId), keySelector, null);
		this.validator.setMaxClockSkew(b.maxClockSkewSeconds);
	}

	private static JWKSource<SecurityContext> resolveSource(Builder b) {
		if (b.jwkSource != null)
			return b.jwkSource;
		if (b.jwkSet != null)
			return new ImmutableJWKSet<>(b.jwkSet);
		try {
			return JWKSourceBuilder.create(b.jwksUri.toURL()).build();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid JWKS URI: " + b.jwksUri, e);
		}
	}

	/**
	 * Validates an ID token and projects the verified claims onto a {@link ClaimsPrincipal}.
	 *
	 * @param idToken The raw (compact) ID token JWT.  Must not be <jk>null</jk> or blank.
	 * @param expectedNonce The {@code nonce} the relying party stored before the redirect; the ID
	 * 	token's {@code nonce} claim must match.  May be <jk>null</jk> only if the flow did not use a
	 * 	nonce (the RP always supplies one).
	 * @return The verified claims as a {@link ClaimsPrincipal} whose name is the {@code sub} claim.
	 * @throws AuthenticationException If parsing, signature verification, or any claim check fails.
	 */
	public ClaimsPrincipal validate(String idToken, String expectedNonce) throws AuthenticationException {
		assertArgNotNullOrBlank("idToken", idToken);
		JWT jwt;
		try {
			jwt = JWTParser.parse(idToken);
		} catch (ParseException e) {
			throw new AuthenticationException(e, "ID token could not be parsed");
		}
		IDTokenClaimsSet claims;
		try {
			claims = validator.validate(jwt, expectedNonce == null ? null : new Nonce(expectedNonce));
		} catch (BadJOSEException e) {
			throw new AuthenticationException(e, "ID token validation failed: {0}", e.getMessage());
		} catch (JOSEException e) {
			throw new AuthenticationException(e, "ID token signature verification failed");
		}
		var sub = claims.getSubject().getValue();
		return new ClaimsPrincipal(sub, claims.toJSONObject());
	}
}
