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

/**
 * Verifies that a successful {@link JwtTokenValidator#validate(String) validation} produces a
 * {@link ClaimsPrincipal} whose claims are accessible via {@code getClaim(String, Class)}.
 *
 * @since 10.0.0
 */
class JwtTokenValidator_ClaimsPrincipal_Test extends TestBase {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

	@Test void a01_claimsPrincipalExposesStandardClaims() throws Exception {
		var rsa = generateRsa("kid-1");
		var claims = defaultClaims(CLOCK)
			.claim("scope", "read write")
			.claim("groups", new String[]{"admins", "billing"})
			.build();
		var token = signRsa(rsa, claims);
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(CLOCK)
			.build();
		var principal = validator.validate(token);
		assertInstanceOf(ClaimsPrincipal.class, principal);
		var cp = (ClaimsPrincipal) principal;

		assertEquals("alice", cp.getName());
		assertEquals(DEFAULT_ISSUER, cp.getClaim("iss", String.class).orElse(null));
		assertEquals("read write", cp.getClaim("scope", String.class).orElse(null));
		assertNotNull(cp.getClaim("exp", Object.class).orElse(null));
		assertNotNull(cp.getClaim("nbf", Object.class).orElse(null));
	}

	@Test void a02_missingClaimReturnsEmpty() throws Exception {
		var rsa = generateRsa("kid-1");
		var token = signRsa(rsa, defaultClaims(CLOCK).build());
		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(CLOCK)
			.build();
		var cp = (ClaimsPrincipal) validator.validate(token);
		assertTrue(cp.getClaim("not_in_token", String.class).isEmpty());
	}
}
