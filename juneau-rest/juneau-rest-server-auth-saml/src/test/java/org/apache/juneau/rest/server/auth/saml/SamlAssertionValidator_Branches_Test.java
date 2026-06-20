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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.opensaml.saml.common.xml.*;
import org.opensaml.saml.metadata.resolver.*;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.security.credential.*;

import net.shibboleth.shared.resolver.*;

/**
 * Branch-coverage tests for {@link SamlAssertionValidator} — exercises paths that the happy-path
 * and encrypted-assertion tests do not reach.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class SamlAssertionValidator_Branches_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final Instant NBF = NOW.minusSeconds(60);
	private static final Instant NOA = NOW.plusSeconds(300);

	private static SamlAssertionValidator validator(BasicCredential cred) {
		return SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.clock(CLOCK)
			.build();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: null <Conditions> → AuthenticationException (validateConditions line 556)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullConditions_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildAssertionNoConditions(ISSUER, "alice");
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: nbf == null and noa == null → timestamps not checked (lines 561–564 both false branches)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_noNbf_noNoa_accepted() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		// conditions() with null notBefore and null notOnOrAfter → neither timestamp check fires
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", null, null);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var principal = validator(cred).validate(xml);
		assertEquals("alice", principal.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: response <Issuer> non-null but wrong → rejected (validateIssuer line 584 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_responseIssuerMismatch_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		// signAndBuildResponse with responseIssuer="https://evil.example.com" != expectedIssuer
		var xml = SamlTestSupport.signAndBuildResponse(cred, "https://evil.example.com", assertion);
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	@Test void c02_responseIssuerNull_accepted() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		// null responseIssuer → the response-level issuer check is skipped (respIssuer==null branch)
		var xml = SamlTestSupport.signAndBuildResponse(cred, null, assertion);
		var principal = validator(cred).validate(xml);
		assertEquals("alice", principal.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: nameId() — null subject → "<no-subject>"; NameID with null value → "<no-subject>"
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_nullSubject_nameIsNoSubject() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, null, NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var principal = validator(cred).validate(xml);
		assertEquals("<no-subject>", principal.getName());
	}

	@Test void d02_nullNameIdValue_nameIsNoSubject() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var sub = SamlTestSupport.subjectWithNullNameIdValue();
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var principal = validator(cred).validate(xml);
		assertEquals("<no-subject>", principal.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: buildClaims — multi-value attribute (values.size() > 1 branch, line 546)
	//                  attribute with null name (skip branch, line 539)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_multiValueAttribute_storesList() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		SamlTestSupport.addAttributeStatement(assertion, "groups", "admin", "user", "viewer");
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var principal = (ClaimsPrincipal) validator(cred).validate(xml);
		var groups = principal.getClaim("groups", Object.class).orElse(null);
		assertInstanceOf(List.class, groups);
		assertEquals(3, ((List<?>) groups).size());
	}

	@Test void e02_nullAttributeName_skipped() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		SamlTestSupport.addAttributeStatement(assertion, null, "should-be-ignored");
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		// Validation should succeed; the null-name attribute is silently skipped
		var principal = validator(cred).validate(xml);
		assertEquals("alice", principal.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: no assertions at all → AuthenticationException (extractAndDecryptAssertion line 393)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_noAssertions_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		// Build an empty Response (no assertions, no encrypted assertions)
		var emptyXml = buildEmptyResponse();
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(emptyXml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: metadata-resolver path — MetadataResolver returns EntityDescriptor; also ResolverException path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_metadataResolverException_wrapped() throws Exception {
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any(CriteriaSet.class))).thenThrow(new ResolverException("test"));
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.metadataResolver(resolver)
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void g02_metadataResolverReturnsNull_rejected() throws Exception {
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any(CriteriaSet.class))).thenReturn(null);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.metadataResolver(resolver)
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void g03_metadataResolverNoIdpSsoDescriptor_rejected() throws Exception {
		// EntityDescriptor exists but has no IDPSSODescriptor for the SAML20P namespace
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(null);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any(CriteriaSet.class))).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.metadataResolver(resolver)
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void g04_metadataResolverNoKeyDescriptors_rejected() throws Exception {
		// IDPSSODescriptor exists but has no KeyDescriptors → extractSigningCredential returns null
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(Collections.emptyList());
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any(CriteriaSet.class))).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.metadataResolver(resolver)
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: verifySignature — unsigned assertion (signature==null) already in h01
	//    also: assertion issuer null → rejected (validateIssuer line 581 left branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_unsignedAssertion_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		// buildUnsignedResponse omits the Signature element entirely
		var xml = SamlTestSupport.buildUnsignedResponse(ISSUER, assertion);
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// I: validateConditions — nbf in the future → rejected (line 561 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_futureNbf_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		// nbf is 10 minutes in the future relative to NOW (well outside 60s clockSkew)
		var futureNbf = NOW.plusSeconds(600);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", futureNbf, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// J: validateIssuer — assertion issuer is null → rejected (line 581 left-hand branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void j01_assertionIssuerNull_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		assertion.setIssuer(null);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// K: nameId() — subject with no NameID element → "<no-subject>" (line 602 null branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_subjectWithNoNameId_nameIsNoSubject() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var sub = SamlTestSupport.subjectWithNoNameId();
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var principal = validator(cred).validate(xml);
		assertEquals("<no-subject>", principal.getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// L: buildClaims — attribute with 0 values (values.isEmpty() true → not stored, line 546 false)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void l01_emptyValueAttribute_notStored() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		SamlTestSupport.addAttributeStatement(assertion, "empty-attr"); // zero values
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var principal = (ClaimsPrincipal) validator(cred).validate(xml);
		assertTrue(principal.getClaim("empty-attr", Object.class).isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// M: parseResponse — unknown XML element → AuthenticationException (line 376 true branch)
	//    parseResponse — parsed object is not a Response → rejected (line 380 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void m01_unknownXmlElement_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		// An XML element that OpenSAML has no registered unmarshaller for
		var xml = "<foo:Unknown xmlns:foo=\"urn:foo:bar\"><foo:Child/></foo:Unknown>";
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	@Test void m02_knownElementNotResponse_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		// A bare <saml:Assertion> is a known SAML element but is NOT a <samlp:Response> →
		// unmarshaller is non-null but obj instanceof Response is false
		var xml = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\""
			+ " ID=\"_a1\" Version=\"2.0\" IssueInstant=\"2026-01-01T00:00:00Z\">"
			+ "<saml:Issuer>https://idp.example.com</saml:Issuer>"
			+ "</saml:Assertion>";
		assertThrows(AuthenticationException.class, () -> validator(cred).validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// N: metadata path — KeyDescriptor with non-signing use → skipped (credentialFromKeyDescriptor line 487)
	//    KeyDescriptor with null KeyInfo → skipped (line 490)
	//    KeyDescriptor with KeyInfo but empty X509Datas → no credential (line 492 empty loop)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void n01_keyDescriptor_nonSigningUse_skipped() throws Exception {
		// A KeyDescriptor with UsageType.ENCRYPTION is not a signing key → skipped → null credential → rejected
		var kd = mock(KeyDescriptor.class);
		when(kd.getUse()).thenReturn(UsageType.ENCRYPTION);
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(List.of(kd));
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any())).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE).expectedIssuer(ISSUER).metadataResolver(resolver).clock(CLOCK).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void n02_keyDescriptor_nullKeyInfo_skipped() throws Exception {
		var kd = mock(KeyDescriptor.class);
		when(kd.getUse()).thenReturn(null); // signing
		when(kd.getKeyInfo()).thenReturn(null);
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(List.of(kd));
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any())).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE).expectedIssuer(ISSUER).metadataResolver(resolver).clock(CLOCK).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void n03_keyDescriptor_emptyX509Datas_skipped() throws Exception {
		// KeyInfo is non-null but has no X509Datas → credentialFromKeyDescriptor returns null → rejected
		var keyInfo = mock(org.opensaml.xmlsec.signature.KeyInfo.class);
		when(keyInfo.getX509Datas()).thenReturn(Collections.emptyList());
		var kd = mock(KeyDescriptor.class);
		when(kd.getUse()).thenReturn(null); // signing
		when(kd.getKeyInfo()).thenReturn(keyInfo);
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(List.of(kd));
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any())).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE).expectedIssuer(ISSUER).metadataResolver(resolver).clock(CLOCK).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void n04_keyDescriptor_unspecifiedUse_treatedAsSigning() throws Exception {
		// UsageType.UNSPECIFIED should be treated as a signing key (isSigningKey line 513 UNSPECIFIED branch)
		// KeyInfo non-null but empty X509Datas → still rejected (no credential found)
		var keyInfo = mock(org.opensaml.xmlsec.signature.KeyInfo.class);
		when(keyInfo.getX509Datas()).thenReturn(Collections.emptyList());
		var kd = mock(KeyDescriptor.class);
		when(kd.getUse()).thenReturn(UsageType.UNSPECIFIED);
		when(kd.getKeyInfo()).thenReturn(keyInfo);
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(List.of(kd));
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any())).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE).expectedIssuer(ISSUER).metadataResolver(resolver).clock(CLOCK).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void n05_keyDescriptor_x509DataWithNullCertValue_skipped() throws Exception {
		// X509Data has a certificate whose getValue() returns null → parseX509Credential returns null → skipped
		var cert = mock(org.opensaml.xmlsec.signature.X509Certificate.class);
		when(cert.getValue()).thenReturn(null);
		var x509 = mock(org.opensaml.xmlsec.signature.X509Data.class);
		when(x509.getX509Certificates()).thenReturn(List.of(cert));
		var keyInfo = mock(org.opensaml.xmlsec.signature.KeyInfo.class);
		when(keyInfo.getX509Datas()).thenReturn(List.of(x509));
		var kd = mock(KeyDescriptor.class);
		when(kd.getUse()).thenReturn(UsageType.SIGNING);
		when(kd.getKeyInfo()).thenReturn(keyInfo);
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(List.of(kd));
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any())).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE).expectedIssuer(ISSUER).metadataResolver(resolver).clock(CLOCK).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	@Test void n06_keyDescriptor_x509DataWithInvalidBase64_skipped() throws Exception {
		// getValue() returns invalid base64 → parseX509Credential catches exception and returns null → skipped
		var cert = mock(org.opensaml.xmlsec.signature.X509Certificate.class);
		when(cert.getValue()).thenReturn("!!!not-valid-base64-cert!!!");
		var x509 = mock(org.opensaml.xmlsec.signature.X509Data.class);
		when(x509.getX509Certificates()).thenReturn(List.of(cert));
		var keyInfo = mock(org.opensaml.xmlsec.signature.KeyInfo.class);
		when(keyInfo.getX509Datas()).thenReturn(List.of(x509));
		var kd = mock(KeyDescriptor.class);
		when(kd.getUse()).thenReturn(UsageType.SIGNING);
		when(kd.getKeyInfo()).thenReturn(keyInfo);
		var idp = mock(IDPSSODescriptor.class);
		when(idp.getKeyDescriptors()).thenReturn(List.of(kd));
		var ed = mock(EntityDescriptor.class);
		when(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)).thenReturn(idp);
		var resolver = mock(MetadataResolver.class);
		when(resolver.resolveSingle(any())).thenReturn(ed);
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE).expectedIssuer(ISSUER).metadataResolver(resolver).clock(CLOCK).build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// O: verifySignature — algorithm not in allowlist → rejected (line 424 !algorithms.contains(alg) true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void o01_algorithmNotInAllowlist_rejected() throws Exception {
		var pair = SamlTestSupport.generateRsaKeyPair();
		var cred = SamlTestSupport.credential(pair);
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(cred, ISSUER, assertion);
		// The assertion was signed with RSA-SHA256. Configure the validator to allow ONLY ECDSA-SHA256 →
		// RSA-SHA256 is not in the allowlist → rejected at the algorithm check (line 424 true branch).
		var v = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.algorithms("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256")
			.clock(CLOCK)
			.build();
		assertThrows(AuthenticationException.class, () -> v.validate(xml));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static String buildEmptyResponse() {
		return "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
			+ " ID=\"_empty\" Version=\"2.0\" IssueInstant=\"2026-01-01T00:00:00Z\">"
			+ "<samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status>"
			+ "</samlp:Response>";
	}
}
