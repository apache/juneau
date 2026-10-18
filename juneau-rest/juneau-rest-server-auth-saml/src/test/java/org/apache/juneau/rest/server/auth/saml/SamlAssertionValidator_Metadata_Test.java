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

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * End-to-end tests for {@link SamlAssertionValidator} resolving the IdP signing certificate from real SAML
 * metadata (loaded through {@link SamlMetadataResolvers#file(Path)}), rather than from a pinned credential.
 *
 * @since 10.0.0
 */
class SamlAssertionValidator_Metadata_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";
	private static final Instant NOW = SamlTestKeys.VALID_AT;
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final Instant NBF = NOW.minusSeconds(60);
	private static final Instant NOA = NOW.plusSeconds(300);

	@TempDir
	Path tmp;

	private static String metadata(String certElements) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" entityID=\"" + ISSUER + "\">"
			+ "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
			+ "<KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:X509Data>" + certElements + "</ds:X509Data></ds:KeyInfo></KeyDescriptor>"
			+ "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://idp.example.com/sso\"/>"
			+ "</IDPSSODescriptor></EntityDescriptor>";
	}

	private static String certElement(String base64) {
		return "<ds:X509Certificate>" + base64 + "</ds:X509Certificate>";
	}

	private SamlAssertionValidator validator(String metadataXml, Clock clock) throws Exception {
		var file = tmp.resolve("metadata.xml");
		Files.writeString(file, metadataXml, StandardCharsets.UTF_8);
		return SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.metadataResolver(SamlMetadataResolvers.file(file))
			.clock(clock)
			.build();
	}

	private static String signedResponse() throws Exception {
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		return SamlTestSupport.signAndBuildResponse(SamlTestKeys.credential(), ISSUER, assertion);
	}

	@Test void a01_metadataCertificate_verifiesSignature() throws Exception {
		var v = validator(metadata(certElement(SamlTestKeys.CERT_BASE64)), CLOCK);
		assertEquals("alice", v.validate(signedResponse()).getName());
	}

	@Test void a02_malformedCertificateSkipped_nextCertificateUsed() throws Exception {
		// The first certificate is valid base64 but not a certificate; the loop moves on to the genuine one.
		var v = validator(metadata(certElement("AAAA") + certElement(SamlTestKeys.CERT_BASE64)), CLOCK);
		assertEquals("alice", v.validate(signedResponse()).getName());
	}

	@Test void a03_certificateNotYetValid_rejected() throws Exception {
		// The trust-anchor certificate is rejected outside its validity window, so no signing credential remains.
		var early = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
		var v = validator(metadata(certElement(SamlTestKeys.CERT_BASE64)), early);
		var xml = signedResponse();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("No signing X.509 certificate"), ex.getMessage());
	}

	@Test void a04_certificateExpired_rejected() throws Exception {
		var late = Clock.fixed(Instant.parse("2130-01-01T00:00:00Z"), ZoneOffset.UTC);
		var v = validator(metadata(certElement(SamlTestKeys.CERT_BASE64)), late);
		var xml = signedResponse();
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("No signing X.509 certificate"), ex.getMessage());
	}

	@Test void a05_metadataCertificateOfDifferentKey_signatureRejected() throws Exception {
		// Metadata carries the test certificate, but the assertion is signed with an unrelated key.
		var other = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var assertion = SamlTestSupport.buildMinimalAssertion(ISSUER, AUDIENCE, "alice", NBF, NOA);
		var xml = SamlTestSupport.signAndBuildResponse(other, ISSUER, assertion);
		var v = validator(metadata(certElement(SamlTestKeys.CERT_BASE64)), CLOCK);
		var ex = assertThrows(AuthenticationException.class, () -> v.validate(xml));
		assertTrue(ex.getMessage().contains("signature verification failed"), ex.getMessage());
	}

	@Test void b01_file_validMetadata_resolves() throws Exception {
		var file = tmp.resolve("ok.xml");
		Files.writeString(file, metadata(certElement(SamlTestKeys.CERT_BASE64)), StandardCharsets.UTF_8);
		assertNotNull(SamlMetadataResolvers.file(file));
		assertNotNull(SamlMetadataResolvers.file(file.toFile()));
	}

	@Test void b02_file_invalidXml_throwsIOException() throws Exception {
		var file = tmp.resolve("bad.xml");
		Files.writeString(file, "this is not metadata", StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> SamlMetadataResolvers.file(file));
	}
}
