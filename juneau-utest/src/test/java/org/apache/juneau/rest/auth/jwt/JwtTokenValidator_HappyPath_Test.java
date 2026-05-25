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
 * Happy-path validation for {@link JwtTokenValidator}: an RS256-signed JWT with all mandatory
 * claims is accepted and produces a populated {@link ClaimsPrincipal}.
 *
 * @since 9.5.0
 */
class JwtTokenValidator_HappyPath_Test extends TestBase {

	@Test void a01_validRsaJwt_isAccepted() throws Exception {
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var rsa = generateRsa("kid-1");
		var token = signRsa(rsa, defaultClaims(clock).build());

		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(rsa))
			.clock(clock)
			.build();

		var principal = validator.validate(token);
		assertNotNull(principal);
		assertEquals("alice", principal.getName());
		assertInstanceOf(ClaimsPrincipal.class, principal);
	}

	@Test void a02_validEcJwt_isAccepted() throws Exception {
		var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
		var ec = generateEc("kid-2");
		var token = signEc(ec, defaultClaims(clock).build());

		var validator = JwtTokenValidator.create()
			.issuer(DEFAULT_ISSUER)
			.audience(DEFAULT_AUDIENCE)
			.jwkSource(fixed(ec))
			.clock(clock)
			.build();

		var principal = validator.validate(token);
		assertEquals("alice", principal.getName());
	}
}
