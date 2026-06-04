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

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.*;
import org.junit.jupiter.api.*;

/**
 * Mandatory-claim validation for {@link JwtTokenValidator}: {@code iss}, {@code aud},
 * {@code exp}, {@code nbf} are required by default; mismatches and absences are rejected.
 *
 * @since 10.0.0
 */
class JwtTokenValidator_ClaimValidation_Test extends TestBase {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private static JwtTokenValidator newValidator(com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> src) {
		return JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(src)
			.clock(CLOCK)
			.build();
	}

	@Test void a01_expiredJwt_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		// Use a value well beyond the default 60s clock-skew tolerance so the rejection isn't on the edge.
		var claims = defaultClaims(CLOCK)
			.expirationTime(Date.from(NOW.minus(Duration.ofMinutes(10))))
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("expired"));
	}

	@Test void a02_notYetValidJwt_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = defaultClaims(CLOCK)
			.notBeforeTime(Date.from(NOW.plus(Duration.ofMinutes(5))))
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("not yet"));
	}

	@Test void a03_audienceMismatch_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = defaultClaims(CLOCK)
			.audience("https://other.example.com")
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("aud"));
	}

	@Test void a04_issuerMismatch_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = defaultClaims(CLOCK)
			.issuer("https://other-issuer.example.com/")
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("iss"));
	}

	@Test void a05_missingExp_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
			.subject("alice")
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.notBeforeTime(Date.from(NOW))
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("\"exp\""));
	}

	@Test void a06_missingNbf_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
			.subject("alice")
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.expirationTime(Date.from(NOW.plus(Duration.ofMinutes(5))))
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("\"nbf\""));
	}

	@Test void a07_missingIss_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
			.subject("alice")
			.audience(DEFAULT_AUDIENCE)
			.notBeforeTime(Date.from(NOW))
			.expirationTime(Date.from(NOW.plus(Duration.ofMinutes(5))))
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("\"iss\""));
	}

	@Test void a08_missingAud_isRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
			.subject("alice")
			.issuer(DEFAULT_ISSUER)
			.notBeforeTime(Date.from(NOW))
			.expirationTime(Date.from(NOW.plus(Duration.ofMinutes(5))))
			.build();
		var token = signRsa(rsa, claims);
		var validator = newValidator(fixed(rsa));
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("\"aud\""));
	}

	@Test void a09_blankSubject_yieldsPlaceholderName() throws Exception {
		// A signed, otherwise-valid JWT with no "sub" still validates; the resulting principal
		// carries the documented "<no-sub>" placeholder.
		var rsa = generateRsa("kid-1");
		var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.notBeforeTime(Date.from(NOW))
			.expirationTime(Date.from(NOW.plus(Duration.ofMinutes(5))))
			.build();
		var token = signRsa(rsa, claims);
		var principal = newValidator(fixed(rsa)).validate(token);
		assertEquals("<no-sub>", principal.getName());
	}

	@Test void b01_clockSkew_acceptsRecentlyExpiredJwtWithinSkew() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = defaultClaims(CLOCK)
			.expirationTime(Date.from(NOW.minus(Duration.ofSeconds(30))))
			.build();
		var token = signRsa(rsa, claims);
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clockSkew(Duration.ofSeconds(120))
			.clock(CLOCK)
			.build();
		var principal = validator.validate(token);
		assertEquals("alice", principal.getName());
	}
}
