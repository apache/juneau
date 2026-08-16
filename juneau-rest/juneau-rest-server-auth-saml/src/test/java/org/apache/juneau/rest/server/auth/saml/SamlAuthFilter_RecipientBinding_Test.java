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

import java.nio.charset.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.opensaml.security.credential.*;

import jakarta.servlet.http.*;

/**
 * Filter-level tests proving {@link SamlAuthFilter} binds every assertion to the actual ACS URL of the
 * current request &mdash; regardless of whether the {@link SamlAssertionValidator} it wraps was itself built
 * with {@link SamlAssertionValidator.Builder#recipient(String) recipient(...)} configured.
 *
 * @since 10.0.0
 */
class SamlAuthFilter_RecipientBinding_Test extends TestBase {

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

	/** Builds a signed response whose bearer confirmation names {@code confirmedRecipient} as its Recipient. */
	private static String signedResponseWithRecipient(Credential cred, String confirmedRecipient) throws Exception {
		var sc = SamlTestSupport.bearerConfirmation(confirmedRecipient, null, NOA, null, null);
		var sub = SamlTestSupport.subjectWithConfirmations("alice", sc);
		var assertion = SamlTestSupport.buildMinimalAssertionWithSubject(ISSUER, AUDIENCE, sub, NBF, NOA);
		return SamlTestSupport.signAndBuildResponse((BasicCredential) cred, ISSUER, assertion);
	}

	/** Mocks a request that was actually delivered to {@code deliveredToAcs}, carrying {@code responseXml}. */
	private static HttpServletRequest req(String deliveredToAcs, String responseXml) {
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn("/saml/acs");
		when(r.getServletPath()).thenReturn("/saml/acs");
		when(r.getParameter("SAMLResponse")).thenReturn(
			Base64.getEncoder().encodeToString(responseXml.getBytes(StandardCharsets.UTF_8)));
		when(r.getRequestURL()).thenReturn(new StringBuffer(deliveredToAcs));
		return r;
	}

	@Test void a01_matchingAcsRecipient_accepted() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedResponseWithRecipient(cred, ACS);
		// No recipient(...) configured on the validator — the filter must derive and bind it anyway.
		var validator = base(cred).build();
		var f = SamlAuthFilter.create().validator(validator).build();
		var result = f.authenticate(req(ACS, xml));
		assertTrue(result.isPresent());
		assertEquals("alice", result.get().getPrincipal().getName());
	}

	@Test void a02_differentAcsRecipient_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		// The assertion's bearer confirmation names a different ACS than the one this request actually hit.
		var xml = signedResponseWithRecipient(cred, "https://other-sp.example.com/saml/acs");
		var validator = base(cred).build();
		var f = SamlAuthFilter.create().validator(validator).build();
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req(ACS, xml)));
		assertTrue(ex.getHeaders().stream().anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName())));
	}

	@Test void a03_publishedExampleRecipe_stillBindsRecipientOnFilterPath() throws Exception {
		// Mirrors SamlAuthFilter's javadoc "Usage" example exactly: validator built with no recipient(...).
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedResponseWithRecipient(cred, "https://other-sp.example.com/saml/acs");
		var validator = SamlAssertionValidator.create()
			.spEntityId(AUDIENCE)
			.expectedIssuer(ISSUER)
			.signingCredential(cred)
			.clock(CLOCK)
			.build();
		var f = SamlAuthFilter.create().validator(validator).build();
		assertThrows(AuthenticationException.class, () -> f.authenticate(req(ACS, xml)));
	}

	@Test void a04_standaloneValidator_recipientOptedInDirectly_unaffectedByFilterOverride() throws Exception {
		// Sanity check: calling the validator directly (not through the filter) with recipient(...) configured
		// still enforces the builder-level recipient, independent of the filter's per-call override.
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedResponseWithRecipient(cred, ACS);
		var validator = base(cred).recipient(ACS).build();
		assertEquals("alice", validator.validate(xml).getName());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: getRequestURL() returns null — the recipient is reconstructed from scheme/host/port/path, or the
	// request is rejected via AuthenticationException (never an NPE) when reconstruction is impossible.
	// -----------------------------------------------------------------------------------------------------------------

	/** Mocks a request with a null getRequestURL(), delivered to the given scheme/host/port/uri. */
	private static HttpServletRequest reqNullRequestUrl(String scheme, String host, int port, String uri, String responseXml) {
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn("/saml/acs");
		when(r.getServletPath()).thenReturn("/saml/acs");
		when(r.getParameter("SAMLResponse")).thenReturn(
			Base64.getEncoder().encodeToString(responseXml.getBytes(StandardCharsets.UTF_8)));
		when(r.getRequestURL()).thenReturn(null);
		when(r.getScheme()).thenReturn(scheme);
		when(r.getServerName()).thenReturn(host);
		when(r.getServerPort()).thenReturn(port);
		when(r.getRequestURI()).thenReturn(uri);
		return r;
	}

	@Test void b01_nullRequestUrl_reconstructedAtDefaultPort_acceptsMatchingRecipient() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedResponseWithRecipient(cred, ACS);
		var validator = base(cred).build();
		var f = SamlAuthFilter.create().validator(validator).build();
		var result = f.authenticate(reqNullRequestUrl("https", "sp.example.com", 443, "/saml/acs", xml));
		assertTrue(result.isPresent());
		assertEquals("alice", result.get().getPrincipal().getName());
	}

	@Test void b02_nullRequestUrl_reconstructedAtNonDefaultPort_includesPortInRecipient() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedResponseWithRecipient(cred, "https://sp.example.com:8443/saml/acs");
		var validator = base(cred).build();
		var f = SamlAuthFilter.create().validator(validator).build();
		var result = f.authenticate(reqNullRequestUrl("https", "sp.example.com", 8443, "/saml/acs", xml));
		assertTrue(result.isPresent());
	}

	@Test void b03_nullRequestUrl_reconstructedRecipientMismatch_rejected() throws Exception {
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		// The assertion's bearer confirmation names a different ACS than the one this request actually hit.
		var xml = signedResponseWithRecipient(cred, "https://other-sp.example.com/saml/acs");
		var validator = base(cred).build();
		var f = SamlAuthFilter.create().validator(validator).build();
		var ex = assertThrows(AuthenticationException.class,
			() -> f.authenticate(reqNullRequestUrl("https", "sp.example.com", 443, "/saml/acs", xml)));
		assertTrue(ex.getHeaders().stream().anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName())));
	}

	@Test void b04_nullRequestUrl_andNullScheme_failsClosedNotNpe() throws Exception {
		// Neither getRequestURL() nor the scheme/host/uri fallback getters are usable — must fail closed with
		// an AuthenticationException, never propagate an NPE.
		var cred = SamlTestSupport.credential(SamlTestSupport.generateRsaKeyPair());
		var xml = signedResponseWithRecipient(cred, ACS);
		var validator = base(cred).build();
		var f = SamlAuthFilter.create().validator(validator).build();
		var ex = assertThrows(AuthenticationException.class,
			() -> f.authenticate(reqNullRequestUrl(null, null, 0, null, xml)));
		assertTrue(ex.getHeaders().stream().anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName())));
	}
}
