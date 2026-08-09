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
package org.apache.juneau.rest.server.auth.saml;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.concurrent.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the one-time-use (replay) enforcement in {@link SamlAssertionValidator} &mdash; a validly-signed
 * assertion succeeds once and is rejected on any subsequent presentation, and the check is fail-closed.
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_SingleUse_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private static SamlAssertionValidator.Builder base(org.opensaml.security.credential.Credential cred) {
		return SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.clock(CLOCK);
	}

	@Test void a01_firstPresentationSucceeds_secondRejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());
		var validator = base(cred).build();

		assertEquals("alice", validator.validate(xml).getName());
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void a02_separateValidatorInstances_haveIndependentCaches() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());

		// The default in-memory cache is per-instance, so a fresh validator accepts the same assertion.
		assertEquals("alice", base(cred).build().validate(xml).getName());
		assertEquals("alice", base(cred).build().validate(xml).getName());
	}

	@Test void a03_injectedSharedCache_rejectsAcrossValidators() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());
		var shared = new InMemoryReplayCache();

		assertEquals("alice", base(cred).replayCache(shared).build().validate(xml).getName());
		// A different validator instance sharing the same cache sees the assertion as already consumed.
		var v2 = base(cred).replayCache(shared).build();
		assertThrows(AuthenticationException.class, () -> v2.validate(xml));
	}

	@Test void b01_cacheUnavailable_failsClosed() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());
		// A cache that cannot answer (throws) must cause the assertion to be rejected, not accepted.
		ReplayCache throwing = (id, expiresAtMs) -> { throw new IllegalStateException("store down"); };
		var validator = base(cred).replayCache(throwing).build();
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void b02_distinctAssertions_bothAccepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var validator = base(cred).build();
		// Two independently-minted assertions carry distinct IDs, so neither is a replay of the other.
		var xml1 = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());
		var xml2 = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "bob",
			NOW.minusSeconds(60), NOW.plusSeconds(300), Map.of());
		assertEquals("alice", validator.validate(xml1).getName());
		assertEquals("bob", validator.validate(xml2).getName());
	}
}
