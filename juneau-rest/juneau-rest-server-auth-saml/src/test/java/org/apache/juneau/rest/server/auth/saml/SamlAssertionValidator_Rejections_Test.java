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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.*;
import org.opensaml.xmlsec.signature.support.*;

/**
 * Rejection-path tests for {@link SamlAssertionValidator}: non-success response status, structurally invalid or
 * tampered signatures, reference-digest allowlisting, bearer-confirmation edge cases, and missing assertion IDs.
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_Rejections_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final String ACS = "https://sp.example.com/saml/acs";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final Instant NBF = NOW.minusSeconds(60);
	private static final Instant NOA = NOW.plusSeconds(300);

	private static SamlAssertionValidator.Builder base(BasicCredential cred) {
		return SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.clock(CLOCK);
	}

	private static BasicCredential cred() throws Exception {
		return SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
	}

	private static String challenge(AuthenticationException ex) {
		return ex.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.findFirst().orElseThrow().getValue();
	}

	private static String signed(BasicCredential cred) throws Exception {
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		return SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
	}

	private static String response(String statusXml) {
		return "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
			+ " ID=\"_r1\" Version=\"2.0\" IssueInstant=\"2026-01-01T00:00:00Z\">" + statusXml + "</samlp:Response>";
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: response status
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_statusNotSuccess_rejected() throws Exception {
		var xml = response("<samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Responder\"/></samlp:Status>");
		var ex = assertThrows(AuthenticationException.class, () -> base(cred()).build().validate(xml));
		assertTrue(ex.getMessage().contains("not Success"), ex.getMessage());
		assertTrue(challenge(ex).contains("invalid_response"));
	}

	@Test void a02_noStatusElement_rejected() throws Exception {
		var ex = assertThrows(AuthenticationException.class, () -> base(cred()).build().validate(response("")));
		assertTrue(ex.getMessage().contains("not Success"), ex.getMessage());
	}

	@Test void a03_statusWithoutStatusCode_rejected() throws Exception {
		var ex = assertThrows(AuthenticationException.class, () -> base(cred()).build().validate(response("<samlp:Status/>")));
		assertTrue(ex.getMessage().contains("not Success"), ex.getMessage());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: signature structure and digest allowlist
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_signatureWithoutSignatureMethod_rejectedAtParse() throws Exception {
		var cred = cred();
		var xml = signed(cred).replaceAll("<(\\w+:)?SignatureMethod[^>]*/>", "");
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml));
		assertTrue(ex.getMessage().contains("could not be parsed"), ex.getMessage());
	}

	@Test void b02_sha1ReferenceDigest_rejected() throws Exception {
		var cred = cred();
		var xml = signed(cred).replace(SignatureConstants.ALGO_ID_DIGEST_SHA256, SignatureConstants.ALGO_ID_DIGEST_SHA1);
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml));
		assertTrue(ex.getMessage().contains("digest algorithm not allowlisted"), ex.getMessage());
		assertTrue(challenge(ex).contains("signature_algorithm_rejected"));
	}

	@Test void b03_emptyReferenceDigest_rejected() throws Exception {
		var cred = cred();
		var xml = signed(cred).replace("Algorithm=\"" + SignatureConstants.ALGO_ID_DIGEST_SHA256 + "\"", "Algorithm=\"\"");
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml));
		assertTrue(ex.getMessage().contains("digest algorithm not allowlisted"), ex.getMessage());
	}

	@Test void b04_referenceDigestOutsideCustomAllowlist_rejected() throws Exception {
		var cred = cred();
		var xml = signed(cred);
		var v = base(cred).digestAlgorithms(SignatureConstants.ALGO_ID_DIGEST_SHA384).build();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("digest algorithm not allowlisted"), ex.getMessage());
	}

	@Test void b05_referenceDigestInsideCustomAllowlist_accepted() throws Exception {
		var cred = cred();
		var xml = signed(cred);
		var v = base(cred).digestAlgorithms(SignatureConstants.ALGO_ID_DIGEST_SHA256).build();
		assertEquals("alice", v.validate(xml).getName());
	}

	@Test void b06_referenceNotPointingAtAssertion_profileValidationFails() throws Exception {
		var cred = cred();
		var xml = signed(cred).replaceAll("<(\\w+:)?Reference URI=\"#[^\"]*\"", "<ds:Reference URI=\"#_other\"");
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml));
		assertTrue(ex.getMessage().contains("signature profile validation failed"), ex.getMessage());
		assertTrue(challenge(ex).contains("signature_invalid"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: digestAlgorithms(...) builder validation
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_digestAlgorithms_emptyRejected() throws Exception {
		var b = base(cred());
		assertThrows(IllegalArgumentException.class, b::digestAlgorithms);
	}

	@Test void c02_digestAlgorithms_nullArrayRejected() throws Exception {
		var b = base(cred());
		assertThrows(IllegalArgumentException.class, () -> b.digestAlgorithms((String[]) null));
	}

	@Test void c03_digestAlgorithms_nullEntryRejected() throws Exception {
		var b = base(cred());
		assertThrows(IllegalArgumentException.class, () -> b.digestAlgorithms((String) null));
	}

	@Test void c04_digestAlgorithms_sha1Rejected() throws Exception {
		var b = base(cred());
		assertThrows(IllegalArgumentException.class, () -> b.digestAlgorithms(SignatureConstants.ALGO_ID_DIGEST_SHA1));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: bearer subject confirmation edge cases
	// -----------------------------------------------------------------------------------------------------------------

	private static String signedWithConfirmation(BasicCredential cred, SubjectConfirmation sc) throws Exception {
		var sub = SamlTestSupport.subjectWithConfirmations("alice", sc);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, NOA);
		return SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
	}

	@Test void d01_bearerWithoutConfirmationData_rejected() throws Exception {
		var cred = cred();
		@SuppressWarnings("unchecked")
		var scb = (org.opensaml.saml.common.SAMLObjectBuilder<SubjectConfirmation>) SamlTestSupport.bf().getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
		var sc = scb.buildObject();
		sc.setMethod(SubjectConfirmation.METHOD_BEARER);
		var xml = signedWithConfirmation(cred, sc);
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml, ACS));
		assertTrue(ex.getMessage().contains("SubjectConfirmationData> is missing"), ex.getMessage());
	}

	@Test void d02_confirmationNotYetValid_rejected() throws Exception {
		var cred = cred();
		var xml = signedWithConfirmation(cred, SamlTestSupport.bearerConfirmation(ACS, NOW.plusSeconds(600), NOA, null, null));
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml, ACS));
		assertTrue(ex.getMessage().contains("not yet valid"), ex.getMessage());
	}

	@Test void d03_unsolicitedConfirmationWithInResponseTo_rejected() throws Exception {
		var cred = cred();
		var xml = signedWithConfirmation(cred, SamlTestSupport.bearerConfirmation(ACS, null, NOA, "_req1", null));
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml, ACS));
		assertTrue(ex.getMessage().contains("unsolicited"), ex.getMessage());
	}

	@Test void d04_confirmationNotBeforeInWindow_accepted() throws Exception {
		var cred = cred();
		var xml = signedWithConfirmation(cred, SamlTestSupport.bearerConfirmation(ACS, NBF, NOA, null, null));
		assertEquals("alice", base(cred).build().validate(xml, ACS).getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: assertion ID (OpenSAML trims an empty ID to null, so empty and absent are the same path)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_missingAssertionId_rejected() throws Exception {
		var cred = cred();
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		assertion.setID(null);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var ex = assertThrows(AuthenticationException.class, () -> base(cred).build().validate(xml));
		assertTrue(ex.getMessage().contains("no ID"), ex.getMessage());
	}
}
