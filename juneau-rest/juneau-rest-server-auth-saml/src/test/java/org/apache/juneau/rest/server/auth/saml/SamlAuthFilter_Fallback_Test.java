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
import static org.mockito.Mockito.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;
import org.opensaml.security.credential.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link SamlAuthFilter}: the builder's {@code consumerPath}, {@code rolesClaim} and {@code realm}
 * settings, and the ACS-recipient reconstruction used when the container returns no request URL.
 *
 * @since 10.0.0
 */
class SamlAuthFilter_Fallback_Test extends TestBase {

	private static final String B64 = Base64.getEncoder().encodeToString("<x/>".getBytes(StandardCharsets.UTF_8));

	/** A validator that records the recipient it was asked to bind and returns a principal carrying {@code claims}. */
	private static SamlAssertionValidator capturing(String[] recipient, Map<String,Object> claims) throws Exception {
		var kp = KeyPairGenerator.getInstance("RSA");
		kp.initialize(2048);
		var pair = kp.generateKeyPair();
		return new SamlAssertionValidator(
				SamlAssertionValidator.create()
					.spEntityId("https://sp.example.com")
					.expectedIssuer("https://idp.example.com")
					.signingCredential(new BasicCredential(pair.getPublic(), pair.getPrivate()))) {
			@Override public Principal validate(String xml, String r) {
				recipient[0] = r;
				return new ClaimsPrincipal("alice", claims);
			}
		};
	}

	/** A request for {@code /saml/acs} whose {@code getRequestURL()} is null, forcing reconstruction. */
	private static HttpServletRequest noUrlReq(String scheme, String host, int port, String uri) {
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn("/saml/acs");
		when(r.getParameter("SAMLResponse")).thenReturn(B64);
		when(r.getRequestURL()).thenReturn(null);
		when(r.getScheme()).thenReturn(scheme);
		when(r.getServerName()).thenReturn(host);
		when(r.getServerPort()).thenReturn(port);
		when(r.getRequestURI()).thenReturn(uri);
		return r;
	}

	private static String reconstructed(String scheme, String host, int port, String uri) throws Exception {
		var recipient = new String[1];
		var f = SamlAuthFilter.create().validator(capturing(recipient, Map.of())).build();
		assertTrue(f.authenticate(noUrlReq(scheme, host, port, uri)).isPresent());
		return recipient[0];
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: recipient reconstruction when getRequestURL() is null
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_httpsDefaultPort_omitted() throws Exception {
		assertEquals("https://sp.example.com/saml/acs", reconstructed("https", "sp.example.com", 443, "/saml/acs"));
	}

	@Test void a02_httpDefaultPort_omitted() throws Exception {
		assertEquals("http://sp.example.com/saml/acs", reconstructed("http", "sp.example.com", 80, "/saml/acs"));
	}

	@Test void a03_nonDefaultPort_included() throws Exception {
		assertEquals("https://sp.example.com:8443/saml/acs", reconstructed("https", "sp.example.com", 8443, "/saml/acs"));
	}

	@Test void a04_port80OnHttps_included() throws Exception {
		assertEquals("https://sp.example.com:80/saml/acs", reconstructed("https", "sp.example.com", 80, "/saml/acs"));
	}

	@Test void a05_port443OnHttp_included() throws Exception {
		assertEquals("http://sp.example.com:443/saml/acs", reconstructed("http", "sp.example.com", 443, "/saml/acs"));
	}

	@Test void a06_unknownPort_omitted() throws Exception {
		assertEquals("https://sp.example.com/saml/acs", reconstructed("https", "sp.example.com", -1, "/saml/acs"));
	}

	@Test void a07_blankSchemeHostOrUri_rejectedWithChallenge() throws Exception {
		var f = SamlAuthFilter.create().validator(capturing(new String[1], Map.of())).build();
		for (var r : List.of(
				noUrlReq(null, "sp.example.com", 443, "/saml/acs"),
				noUrlReq("https", " ", 443, "/saml/acs"),
				noUrlReq("https", "sp.example.com", 443, null))) {
			var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(r));
			assertTrue(ex.getMessage().contains("Unable to derive SAML ACS recipient"), ex.getMessage());
			assertTrue(ex.getHeaders().stream().anyMatch(h -> eqic("WWW-Authenticate", h.getName())));
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: builder settings
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_consumerPath_customPathMatches_defaultNoLongerDoes() throws Exception {
		var f = SamlAuthFilter.create().validator(capturing(new String[1], Map.of())).consumerPath("/sso/post").build();
		var custom = noUrlReq("https", "sp.example.com", 443, "/sso/post");
		when(custom.getPathInfo()).thenReturn("/sso/post");
		assertTrue(f.authenticate(custom).isPresent());
		assertTrue(f.authenticate(noUrlReq("https", "sp.example.com", 443, "/saml/acs")).isEmpty());
	}

	@Test void b02_rolesClaim_customClaimNameUsed() throws Exception {
		var claims = Map.<String,Object>of("roles", List.of("ignored"), "groups", List.of("admin", "ops"));
		var f = SamlAuthFilter.create().validator(capturing(new String[1], claims)).rolesClaim("groups").build();
		var result = f.authenticate(noUrlReq("https", "sp.example.com", 443, "/saml/acs"));
		assertEquals(Set.of("admin", "ops"), result.orElseThrow().getRoles());
	}

	@Test void b03_realm_usedInChallenge() throws Exception {
		var f = SamlAuthFilter.create().validator(capturing(new String[1], Map.of())).realm("corp").build();
		var r = noUrlReq("https", null, 443, "/saml/acs");
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(r));
		var header = ex.getHeaders().stream().filter(h -> eqic("WWW-Authenticate", h.getName())).findFirst().orElseThrow();
		assertEquals("SAML realm=\"corp\"", header.getValue());
	}
}
