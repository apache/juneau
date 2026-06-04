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
package org.apache.juneau.rest.auth.saml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.auth.AuthenticationException;
import org.junit.jupiter.api.Test;

/**
 * Encrypted-assertion handling tests for {@link SamlAssertionValidator} (per OQA Q5).
 *
 * <p>
 * The full encrypt-decrypt-validate round-trip needs an explicit OpenSAML encrypter pipeline (which is
 * non-trivial); for v1 we cover the two negative paths called out by the OQA decision:
 *
 * <ol>
 * 	<li><b>Missing decryption credential</b> &mdash; when an envelope carries {@code <EncryptedAssertion>}
 * 		but the validator has no {@code decryptionCredential(...)} configured, validation fails fast with
 * 		an {@code AuthenticationException} bearing the {@code decryption_required} challenge.
 * 	<li><b>Decrypt fails &mdash; then validation fails</b> &mdash; when the wrong decryption key is
 * 		configured, validation fails with {@code decryption_failed} rather than silently producing a bogus
 * 		principal.
 * </ol>
 *
 * <p>
 * The full happy-path is covered indirectly by {@link SamlAssertionValidator_HappyPath_Test} using
 * unencrypted assertions; encrypt-path full coverage is deferred to integration testing against a real IdP
 * once the integration-test harness lands (separate task).
 *
 * @since 9.5.0
 */
class SamlAssertionValidator_EncryptedAssertion_Test extends TestBase {

	private static final String ISSUER = "https://idp.example.com";
	private static final String AUDIENCE = "https://sp.example.com";

	private static final String ENCRYPTED_ENVELOPE_TEMPLATE =
		"<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
		+ " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\""
		+ " xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\""
		+ " ID=\"_r1\" Version=\"2.0\" IssueInstant=\"2030-01-01T00:00:00Z\">"
		+ "  <saml:Issuer>" + ISSUER + "</saml:Issuer>"
		+ "  <samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status>"
		+ "  <saml:EncryptedAssertion>"
		+ "    <xenc:EncryptedData Type=\"http://www.w3.org/2001/04/xmlenc#Element\">"
		+ "      <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2009/xmlenc11#aes128-gcm\"/>"
		+ "      <xenc:CipherData><xenc:CipherValue>SGVsbG8=</xenc:CipherValue></xenc:CipherData>"
		+ "    </xenc:EncryptedData>"
		+ "  </saml:EncryptedAssertion>"
		+ "</samlp:Response>";

	@Test void a01_missingDecryptionCredential_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.build();
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(ENCRYPTED_ENVELOPE_TEMPLATE));
		var hdr = ex.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.findFirst().orElseThrow();
		assertTrue(hdr.getValue().contains("decryption_required"),
			"Expected WWW-Authenticate to contain 'decryption_required', got: " + hdr.getValue());
	}

	@Test void a02_wrongDecryptionCredential_rejected() throws Exception {
		var signing = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var decryption = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(signing)
			.decryptionCredential(decryption)
			.build();
		var ex = assertThrows(AuthenticationException.class, () -> validator.validate(ENCRYPTED_ENVELOPE_TEMPLATE));
		var hdr = ex.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.findFirst().orElseThrow();
		assertTrue(hdr.getValue().contains("decryption_failed"),
			"Expected WWW-Authenticate to contain 'decryption_failed', got: " + hdr.getValue());
	}
}
