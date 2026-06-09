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
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end happy-path test for {@link SamlAssertionValidator} using a real OpenSAML-signed assertion.
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_HappyPath_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";

	@Test void a01_validResponse_returnsClaimsPrincipal() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var now = Instant.now();
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			now.minusSeconds(60), now.plusSeconds(300), Map.of("dept", "engineering"));

		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.build();
		var principal = validator.validate(xml);
		assertEquals("alice", principal.getName());
		var cp = (ClaimsPrincipal) principal;
		assertEquals("SAML", cp.getClaim(SamlAssertionValidator.ISSUER_TYPE_CLAIM, String.class).orElse(null));
		assertEquals("engineering", cp.getClaim("dept", String.class).orElse(null));
		assertEquals(ISSUER, cp.getClaim("iss", String.class).orElse(null));
		assertEquals(AUDIENCE, cp.getClaim("aud", String.class).orElse(null));
	}

	@Test void b01_expiredAssertion_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var past = Instant.now().minusSeconds(600);
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice",
			past.minusSeconds(60), past, Map.of());

		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void b02_wrongAudience_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var now = Instant.now();
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, "https://other.example.com", "alice",
			now.minusSeconds(60), now.plusSeconds(300), Map.of());

		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void b03_wrongIssuer_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var now = Instant.now();
		var xml = SamlTestSupport.buildSignedResponse(cred, "https://malicious.example.com", AUDIENCE, "alice",
			now.minusSeconds(60), now.plusSeconds(300), Map.of());

		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void b04_wrongSignatureCredential_rejected() throws Exception {
		var signing = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var verifyingDifferent = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var now = Instant.now();
		var xml = SamlTestSupport.buildSignedResponse(signing, ISSUER, AUDIENCE, "alice",
			now.minusSeconds(60), now.plusSeconds(300), Map.of());

		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(verifyingDifferent)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate(xml));
	}

	@Test void b05_malformedXml_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.build();
		assertThrows(AuthenticationException.class, () -> validator.validate("<not really saml>"));
	}
}
