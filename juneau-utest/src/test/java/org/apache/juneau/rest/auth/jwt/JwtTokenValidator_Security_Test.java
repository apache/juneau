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

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.*;

/**
 * Security-critical tests for {@link JwtTokenValidator}: algorithm-confusion attacks, the
 * "none" algorithm, and the HS256 opt-in path.
 *
 * @since 9.5.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class JwtTokenValidator_Security_Test extends TestBase {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

	@Test void a01_noneAlgorithm_isPermanentlyRejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var unsigned = unsignedToken(defaultClaims(CLOCK).build());
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(CLOCK)
			.build();
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(unsigned));
		assertTrue(ex.getMessage().toLowerCase().contains("alg") || ex.getMessage().toLowerCase().contains("sign"));
	}

	@Test void a02_noneInBuilderAllowlist_isRefused() throws Exception {
		// JWSAlgorithm has no static `NONE` constant typed as JWSAlgorithm (nimbus exposes it on the
		// shared Algorithm parent, returned as Algorithm).  Re-creating it by name lets us assert
		// the builder refuses the deny-listed value via the JWSAlgorithm path callers actually use.
		var none = new JWSAlgorithm("none");
		assertThrows(IllegalArgumentException.class, () ->
			JwtTokenValidator.create()
				.issuer(DEFAULT_ISSUER)
				.audience(DEFAULT_AUDIENCE)
				.algorithms(none)
		);
	}

	@Test void b01_algorithmConfusion_rejected() throws Exception {
		// Classic algorithm-confusion attack:
		//  - Validator is configured for RS256 only, with an RSA public key.
		//  - Attacker grabs the (public) RSA modulus, encodes it, and uses it as the HS256 HMAC secret.
		//  - Attacker signs a JWT with HS256 using that secret and sends it.
		//  - If the validator naively re-used the same key for any algorithm, it would pass.
		// JwtTokenValidator must reject because HS256 is not in the allowlist.
		var rsa = generateRsa("kid-1");
		var fakeSecret = rsa.toPublicJWK().toJSONString().getBytes();
		var token = signHmacRaw(fakeSecret, "kid-1", defaultClaims(CLOCK).build());

		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(CLOCK)
			.build();

		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("algorithm"));
	}

	@Test void c01_hs256OptIn_isAccepted_whenAllowlisted() throws Exception {
		var hmac = generateHmac("kid-hmac");
		var token = signHmac(hmac, defaultClaims(CLOCK).build());
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.algorithms(JWSAlgorithm.HS256)
			.jwkSource(fixedRaw(hmac))
			.clock(CLOCK)
			.build();
		var principal = validator.validate(token);
		assertEquals("alice", principal.getName());
	}

	@Test void c02_hs256_rejectedByDefault() throws Exception {
		var hmac = generateHmac("kid-hmac");
		var token = signHmac(hmac, defaultClaims(CLOCK).build());
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixedRaw(hmac))
			.clock(CLOCK)
			.build();
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(token));
		assertTrue(ex.getMessage().toLowerCase().contains("allowlist") || ex.getMessage().toLowerCase().contains("not"));
	}

	@Test void d01_garbageToken_rejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate("not.a.jwt"));
	}

	@Test void d02_emptyToken_rejected() throws Exception {
		var rsa = generateRsa("kid-1");
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate(""));
	}
}
