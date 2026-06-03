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
package org.apache.juneau.rest.auth.jwt;

import static org.apache.juneau.rest.auth.jwt.JwtTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.*;

/**
 * Validates {@link JwtTokenValidator.Builder} input rules and {@link JwtTokenValidator}'s read-only
 * getters &mdash; argument validation, mutual-exclusion of {@code jwksUrl(...)} / {@code jwkSource(...)},
 * and the configured-getters round-trip.
 *
 * @since 9.5.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class JwtTokenValidator_Builder_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Required-field rejection on build().
	// -----------------------------------------------------------------------------------------

	@Test void a01_missingIssuer_rejectedAtBuild() throws Exception {
		var rsa = generateRsa("kid-1");
		assertThrows(IllegalStateException.class, () ->
			JwtTokenValidator.create()
				.audience(DEFAULT_AUDIENCE)
				.jwkSource(fixed(rsa))
				.build()
		);
	}

	@Test void a02_missingAudience_rejectedAtBuild() throws Exception {
		var rsa = generateRsa("kid-1");
		assertThrows(IllegalStateException.class, () ->
			JwtTokenValidator.create()
				.issuer(DEFAULT_ISSUER)
				.jwkSource(fixed(rsa))
				.build()
		);
	}

	@Test void a03_missingKeySource_rejectedAtBuild() {
		assertThrows(IllegalStateException.class, () ->
			JwtTokenValidator.create()
				.issuer(DEFAULT_ISSUER)
				.audience(DEFAULT_AUDIENCE)
				.build()
		);
	}

	@Test void a04_bothJwksUrlAndJwkSource_rejectedAtBuild() throws Exception {
		var rsa = generateRsa("kid-1");
		assertThrows(IllegalStateException.class, () ->
			JwtTokenValidator.create()
				.issuer(DEFAULT_ISSUER)
				.audience(DEFAULT_AUDIENCE)
				.jwksUrl(URI.create("https://issuer.example.com/.well-known/jwks.json"))
				.jwkSource(fixed(rsa))
				.build()
		);
	}

	@Test void a05_jwksUrlOnlyPath_builds() {
		// Exercises the "wrap jwksUrl in a JwksCache" branch of the constructor without making a
		// network call (no validate() is invoked).
		var v = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwksUrl(URI.create("https://issuer.example.com/.well-known/jwks.json"))
			.build();
		assertNotNull(v);
	}

	// -----------------------------------------------------------------------------------------
	// Builder argument validation.
	// -----------------------------------------------------------------------------------------

	@Test void b01_emptyAlgorithmsList_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().algorithms(new JWSAlgorithm[0])
		);
	}

	@Test void b02_negativeClockSkew_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().clockSkew(Duration.ofSeconds(-1))
		);
	}

	@Test void b03_clockSkewOverMax_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().clockSkew(Duration.ofMinutes(6))
		);
	}

	@Test void b04_zeroJwksCacheTtl_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().jwksCacheTtl(Duration.ZERO)
		);
	}

	@Test void b05_negativeJwksCacheTtl_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().jwksCacheTtl(Duration.ofSeconds(-1))
		);
	}

	@Test void b06_zeroEagerRefreshCooldown_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().jwksEagerRefreshCooldown(Duration.ZERO)
		);
	}

	@Test void b07_negativeEagerRefreshCooldown_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().jwksEagerRefreshCooldown(Duration.ofSeconds(-1))
		);
	}

	@Test void b08_eagerRefreshCooldownOverSixtySeconds_rejected() {
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create().jwksEagerRefreshCooldown(Duration.ofSeconds(61))
		);
	}

	@Test void b09_eagerRefreshCooldownExceedsTtl_rejectedAtBuild() throws Exception {
		var rsa = generateRsa("kid-1");
		// ttl = 20s, cooldown = 30s (≤ 60s but > ttl) → rejected at build().
		assertThrows(IllegalStateException.class, () ->
			JwtTokenValidator.create()
				.issuer(DEFAULT_ISSUER)
				.audience(DEFAULT_AUDIENCE)
				.jwkSource(fixed(rsa))
				.jwksCacheTtl(Duration.ofSeconds(20))
				.jwksEagerRefreshCooldown(Duration.ofSeconds(30))
				.build()
		);
	}

	@Test void b10_eagerRefreshOnKidMiss_setterDisablesFeature() throws Exception {
		var rsa = generateRsa("kid-1");
		// Should build successfully with eager disabled.
		var v = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.jwksEagerRefreshOnKidMiss(false)
			.build();
		assertNotNull(v);
	}

	// -----------------------------------------------------------------------------------------
	// Getters expose the configured state.
	// -----------------------------------------------------------------------------------------

	@Test void c01_gettersReturnConfiguredValues() throws Exception {
		var rsa = generateRsa("kid-1");
		var skew = Duration.ofSeconds(30);
		var v = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.algorithms(JWSAlgorithm.RS256)
			.clockSkew(skew)
			.build();
		assertEquals(DEFAULT_ISSUER, v.getIssuer());
		assertEquals(DEFAULT_AUDIENCE, v.getAudience());
		assertEquals(1, v.getAlgorithms().size());
		assertTrue(v.getAlgorithms().contains(JWSAlgorithm.RS256));
		assertEquals(skew, v.getClockSkew());
	}

	@Test void c02_algorithmsCopyIsUnmodifiable() throws Exception {
		var rsa = generateRsa("kid-1");
		var v = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.build();
		assertThrows(UnsupportedOperationException.class, () -> v.getAlgorithms().clear());
	}
}
