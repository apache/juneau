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
import java.util.concurrent.atomic.*;

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
		return SamlAssertionValidator.create().recipient(SamlTestSupport.ACS).allowIdpInitiated()
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
		@SuppressWarnings({
			"unchecked" // OpenSAML builder factory returns a wildcard-typed builder; cast is safe for the requested element QName.
		})
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
	// Mandatory recipient, request binding, IdP-initiated policy
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_recipientIsRequired() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var b = SamlAssertionValidator.create().spEntityId(AUDIENCE).expectedIssuer(ISSUER).signingCredential(cred);
		assertThrows(IllegalStateException.class, b::build);
	}

	@Test void f02_registeredRequestId_acceptedOnce() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-7", null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).build();
		v.rememberAuthnRequest("_req-7");
		assertEquals("alice", v.validate(xml).getName());
	}

	@Test void f03_registeredRequestId_consumedByFirstUse() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var v = base(cred).build();
		v.rememberAuthnRequest("_req-8");
		var xml1 = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-8", null)));
		assertEquals("alice", v.validate(xml1).getName());
		var xml2 = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-8", null)));
		assertThrows(AuthenticationException.class, () -> v.validate(xml2));
	}

	@Test void f04_unregisteredRequestId_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-unknown", null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = base(cred).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void f05_idpInitiated_rejectedByDefault() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var v = SamlAssertionValidator.create().recipient(ACS).spEntityId(AUDIENCE).expectedIssuer(ISSUER)
			.signingCredential(cred).clock(CLOCK).build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("IdP-initiated"), ex.getMessage());
	}

	@Test void f06_idpInitiated_acceptedWhenAllowed() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		assertEquals("alice", base(cred).build().validate(xml).getName());
	}

	@Test void f07_requestIdExpires() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-9", null);
		var xml = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc));
		var clock = new AtomicReference<>(NOW);
		var v = base(cred).authnRequestTtl(Duration.ofSeconds(30)).clock(new Clock() {
			@Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
			@Override public Clock withZone(ZoneId z) { return this; }
			@Override public Instant instant() { return clock.get(); }
		}).build();
		v.rememberAuthnRequest("_req-9");
		clock.set(NOW.plusSeconds(31));
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void f08_customAuthnRequestStore_usedAndSingleUse() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var remembered = new ArrayList<String>();
		var consumed = new ArrayList<String>();
		var ids = new HashSet<String>();
		AuthnRequestStore store = new AuthnRequestStore() {
			@Override public void remember(String id, long expiresAtMs, long nowMs) {
				remembered.add(id);
				ids.add(id);
			}
			@Override public boolean consume(String id, long nowMs) {
				consumed.add(id);
				return ids.remove(id);
			}
		};
		var v = base(cred).authnRequestStore(store).build();
		v.rememberAuthnRequest("_req-10");
		assertEquals(List.of("_req-10"), remembered);
		var xml1 = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-10", null)));
		assertEquals("alice", v.validate(xml1).getName());
		var xml2 = signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req-10", null)));
		assertThrows(AuthenticationException.class, () -> v.validate(xml2));
		assertEquals(List.of("_req-10", "_req-10"), consumed);
	}

	@Test void f09_authnRequestStore_nullRejected() {
		var b = SamlAssertionValidator.create();
		assertThrows(IllegalArgumentException.class, () -> b.authnRequestStore(null));
	}

	@Test void f10_inMemoryStore_singleUseExpiryAndCap() {
		var store = new InMemoryAuthnRequestStore(2);
		store.remember("a", 1000, 0);
		store.remember("b", 50, 0);
		assertThrows(IllegalStateException.class, () -> store.remember("c", 1000, 10));
		store.remember("c", 1000, 60);
		assertFalse(store.consume("b", 60));
		assertTrue(store.consume("a", 60));
		assertFalse(store.consume("a", 60));
		assertThrows(IllegalArgumentException.class, () -> new InMemoryAuthnRequestStore(0));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Destination, subject, attribute handling
	// -----------------------------------------------------------------------------------------------------------------

	private static String withDestination(String xml, String destination) {
		return xml.replaceFirst("<(\\w+:)?Response ", "$0Destination=\"" + destination + "\" ");
	}

	@Test void g01_destinationMismatch_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = withDestination(signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc)), "https://other.example.com/acs");
		var v = base(cred).build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("Destination"), ex.getMessage());
	}

	@Test void g02_destinationMatch_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var xml = withDestination(signedWithSubject(cred, SamlTestSupport.subjectWithConfirmations("alice", sc)), ACS);
		assertEquals("alice", base(cred).build().validate(xml).getName());
	}

	@Test void g03_missingNameId_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var sc = SamlTestSupport.bearerConfirmation(ACS, null, NOA, null, null);
		var sub = SamlTestSupport.subjectWithNoNameId();
		sub.getSubjectConfirmations().add(sc);
		var xml = signedWithSubject(cred, sub);
		var v = base(cred).build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("NameID"), ex.getMessage());
	}

	@Test void g04_attributesCannotReplaceValidatorClaims() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		SamlTestSupport.addAttributeStatement(assertion, "iss", "https://evil.example.com");
		SamlTestSupport.addAttributeStatement(assertion, "aud", "other");
		SamlTestSupport.addAttributeStatement(assertion, "issuerType", "JWT");
		SamlTestSupport.addAttributeStatement(assertion, "dept", "eng");
		var xml = SamlTestSupport.signAndBuildResponse((BasicCredential) cred, ISSUER, assertion);
		var p = (ClaimsPrincipal) base(cred).build().validate(xml);
		assertEquals(ISSUER, p.getClaims().get("iss"));
		assertEquals(AUDIENCE, p.getClaims().get("aud"));
		assertEquals("SAML", p.getClaims().get("issuerType"));
		assertEquals("eng", p.getClaims().get("dept"));
	}
}
