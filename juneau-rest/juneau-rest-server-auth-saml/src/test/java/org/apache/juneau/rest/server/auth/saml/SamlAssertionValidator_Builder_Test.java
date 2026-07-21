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
import static org.mockito.Mockito.*;

import java.security.*;
import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.opensaml.security.credential.*;
import org.opensaml.xmlsec.signature.support.*;

/**
 * Builder validation tests for {@link SamlAssertionValidator}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class SamlAssertionValidator_Builder_Test extends TestBase {

	private static BasicCredential dummyCredential() throws Exception {
		var kp = KeyPairGenerator.getInstance("RSA");
		kp.initialize(2048);
		var pair = kp.generateKeyPair();
		return new BasicCredential(pair.getPublic(), pair.getPrivate());
	}

	@Test void a01_build_requiresSpEntityId() {
		assertThrows(IllegalStateException.class, () -> SamlAssertionValidator.create()
			.expectedIssuer("https://idp.example.com")
			.signingCredential(dummyCredential())
			.build());
	}

	@Test void a02_build_requiresExpectedIssuer() {
		assertThrows(IllegalStateException.class, () -> SamlAssertionValidator.create()
			.spEntityId("https://sp.example.com")
			.signingCredential(dummyCredential())
			.build());
	}

	@Test void a03_build_requiresCredentialOrMetadata() {
		assertThrows(IllegalStateException.class, () -> SamlAssertionValidator.create()
			.spEntityId("https://sp.example.com")
			.expectedIssuer("https://idp.example.com")
			.build());
	}

	@Test void a04_build_happyPath() throws Exception {
		var v = SamlAssertionValidator.create()
			.spEntityId("https://sp.example.com")
			.expectedIssuer("https://idp.example.com")
			.signingCredential(dummyCredential())
			.build();
		assertEquals("https://sp.example.com", v.getSpEntityId());
		assertEquals("https://idp.example.com", v.getExpectedIssuer());
		assertEquals(Duration.ofSeconds(60), v.getClockSkew());
		assertEquals(2, v.getAlgorithms().size());
		assertTrue(v.getAlgorithms().contains(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256));
		assertTrue(v.getAlgorithms().contains(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256));
	}

	@Test void b01_clockSkew_negativeRejected() {
		assertThrows(IllegalArgumentException.class, () -> SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp").signingCredential(dummyCredential())
			.clockSkew(Duration.ofSeconds(-1)));
	}

	@Test void b02_clockSkew_overFiveMinutesRejected() {
		assertThrows(IllegalArgumentException.class, () -> SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp").signingCredential(dummyCredential())
			.clockSkew(Duration.ofMinutes(6)));
	}

	@Test void b03_clockSkew_atFiveMinutesAccepted() throws Exception {
		var v = SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp").signingCredential(dummyCredential())
			.clockSkew(Duration.ofMinutes(5))
			.build();
		assertEquals(Duration.ofMinutes(5), v.getClockSkew());
	}

	@Test void c01_algorithms_sha1Rejected() {
		assertThrows(IllegalArgumentException.class, () -> SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp").signingCredential(dummyCredential())
			.algorithms(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));
	}

	@Test void c02_algorithms_emptyRejected() {
		assertThrows(IllegalArgumentException.class, () -> SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp").signingCredential(dummyCredential())
			.algorithms());
	}

	@Test void c03_algorithms_customAllowlist() throws Exception {
		var v = SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp").signingCredential(dummyCredential())
			.algorithms(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256)
			.build();
		assertEquals(1, v.getAlgorithms().size());
	}

	@Test void d01_credentialAndResolverMutuallyExclusive() throws Exception {
		var cred = dummyCredential();
		var meta = mock(org.opensaml.saml.metadata.resolver.MetadataResolver.class);
		assertThrows(IllegalStateException.class, () -> SamlAssertionValidator.create()
			.spEntityId("sp").expectedIssuer("idp")
			.signingCredential(cred)
			.metadataResolver(meta)
			.build());
	}
}
