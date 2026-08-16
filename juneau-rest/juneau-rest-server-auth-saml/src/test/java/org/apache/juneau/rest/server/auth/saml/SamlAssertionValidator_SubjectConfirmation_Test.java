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
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.*;

/**
 * Tests for bearer {@code <SubjectConfirmation>} / {@code <SubjectConfirmationData>} validation in
 * {@link SamlAssertionValidator}, enabled by configuring the expected {@code Recipient} (ACS URL).
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_SubjectConfirmation_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final String ACS = "https://sp.example.com/saml/acs";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final Instant NBF = NOW.minusSeconds(60);
	private static final Instant NOA = NOW.plusSeconds(300);

	private static SamlAssertionValidator.Builder base(Credential cred) {
		return SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.clock(CLOCK);
	}

	private static String signedWithSubject(Credential cred, Subject sub) throws Exception {
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, NOA);
		return SamlTestSupport.signAndBuildResponse((BasicCredential) cred, ISSUER, assertion);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Happy path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_validBearerConfirmation_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var principal = base(cred).recipient(ACS).build().validate(xml);
		assertEquals("alice", principal.getName());
	}

	@Test void a02_recipientNotConfigured_confirmationNotEnforced() throws Exception {
		// Opt-in: with no recipient configured, an assertion lacking any SubjectConfirmation still validates.
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice", NBF, NOA, Map.of());
		assertEquals("alice", base(cred).build().validate(xml).getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Rejections
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_missingConfirmation_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice")); // no confirmations
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void b02_wrongRecipient_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation("https://evil.example.com/acs", null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void b03_expiredConfirmationNotOnOrAfter_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		// Confirmation NotOnOrAfter well in the past (outside the 60s skew).
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOW.minusSeconds(600), null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void b04_missingConfirmationNotOnOrAfter_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, null, null, null); // no NotOnOrAfter
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void b05_futureConfirmationNotBefore_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, NOW.plusSeconds(600), NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void b06_nonBearerMethodOnly_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var bf = SamlTestSupport.bf();
		// OpenSAML builder factory returns a wildcard-typed builder; cast is safe for the requested element QName.
		@SuppressWarnings("unchecked")
		var scb = (org.opensaml.saml.common.SAMLObjectBuilder<SubjectConfirmation>)
			bf.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
		var sc = scb.buildObject();
		sc.setMethod(SubjectConfirmation.METHOD_HOLDER_OF_KEY);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void b07_nullSubject_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedWithSubject(cred, null);
		var v = base(cred).recipient(ACS).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// InResponseTo binding
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_unsolicited_withInResponseTo_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-123", null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).build(); // no expectedInResponseTo → must be absent
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void c02_solicited_matchingInResponseTo_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-123", null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).expectedInResponseTo("_req-123").build();
		assertEquals("alice", v.validate(xml).getName());
	}

	@Test void c03_solicited_mismatchedInResponseTo_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-999", null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).expectedInResponseTo("_req-123").build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Address binding
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_matchingAddress_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, "203.0.113.7");
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).subjectAddress("203.0.113.7").build();
		assertEquals("alice", v.validate(xml).getName());
	}

	@Test void d02_mismatchedAddress_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, "203.0.113.7");
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).recipient(ACS).subjectAddress("198.51.100.1").build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Multiple confirmations: one bad, one good → accepted
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_multipleConfirmations_oneValid_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var bad = SamlTestSupport.bearerConfirmation("https://evil.example.com/acs", null, NOA, null, null);
		var good = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", bad, good));
		assertEquals("alice", base(cred).recipient(ACS).build().validate(xml).getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// validate(xml, recipient) overload — used by SamlAuthFilter to bind the actual per-request ACS URL
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_explicitRecipientOverload_matchingRecipient_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		// No recipient(...) configured on the builder — the two-arg overload must still enforce it.
		var v = base(cred).build();
		assertEquals("alice", v.validate(xml, ACS).getName());
	}

	@Test void f02_explicitRecipientOverload_mismatchedRecipient_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation("https://evil.example.com/acs", null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml, ACS));
	}

	@Test void f03_explicitRecipientOverload_overridesConfiguredBuilderRecipient() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		// Builder was configured with a DIFFERENT recipient; the per-call argument wins.
		var v = base(cred).recipient("https://other-sp.example.com/saml/acs").build();
		assertEquals("alice", v.validate(xml, ACS).getName());
	}

	@Test void f04_explicitRecipientOverload_nullRecipient_throws() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice", NBF, NOA, Map.of());
		var v = base(cred).build();
		assertThrows(IllegalArgumentException.class, () -> v.validate(xml, null));
	}

	@Test void f05_explicitRecipientOverload_blankRecipient_throws() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = SamlTestSupport.buildSignedResponse(cred, ISSUER, AUDIENCE, "alice", NBF, NOA, Map.of());
		var v = base(cred).build();
		assertThrows(IllegalArgumentException.class, () -> v.validate(xml, " "));
	}
}
