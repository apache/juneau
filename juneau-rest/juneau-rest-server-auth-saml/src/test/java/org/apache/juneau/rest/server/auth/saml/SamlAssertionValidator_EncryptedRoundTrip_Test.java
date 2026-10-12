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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@link SamlAssertionValidator}: a signed assertion encrypted to the SP's key is decrypted,
 * its signature verified, and the resulting principal returned; one signed by any other key is rejected after
 * decryption.
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_EncryptedRoundTrip_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test void a01_encryptedSignedAssertion_decryptedAndValidated() throws Exception {
		var idp = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sp = SamlTestKeys.credential();
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NOW.minusSeconds(60), NOW.plusSeconds(300));
		SamlTestSupport.addAttributeStatement(assertion, "dept", "eng");
		var xml = SamlTestSupport.signEncryptAndBuildResponse(idp, sp, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(idp)
			.decryptionCredential(sp)
			.clock(CLOCK)
			.build();
		var principal = (ClaimsPrincipal) v.validate(xml);
		assertEquals("alice", principal.getName());
		assertEquals("eng", principal.getClaim("dept", String.class).orElse(null));
	}

	@Test void a02_encryptedAssertionSignedByWrongKey_rejected() throws Exception {
		// Decryption succeeds (it is encrypted to the SP's key), but the signer is not the trusted IdP.
		var idp = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var attacker = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sp = SamlTestKeys.credential();
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "mallory", NOW.minusSeconds(60), NOW.plusSeconds(300));
		var xml = SamlTestSupport.signEncryptAndBuildResponse(attacker, sp, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(idp)
			.decryptionCredential(sp)
			.clock(CLOCK)
			.build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertEquals("SAML signature verification failed", ex.getMessage());
		var hdr = ex.getHeaders().stream()
			.filter(h -> eqic("WWW-Authenticate", h.getName()))
			.findFirst().orElseThrow();
		assertTrue(hdr.getValue().contains("signature_invalid"), hdr.getValue());
	}
}
