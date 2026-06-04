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
package org.apache.juneau.rest.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link BearerTokenAuthFilter} — happy path, missing header, malformed header,
 * validator throws, and {@link ClaimsPrincipal} role extraction.
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
	"java:S5976" // Similar-shaped header cases assert distinct authentication outcomes; parameterizing would obscure intent.
})
class BearerTokenAuthFilter_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";

	private static HttpServletRequest req(String authHeader) {
		var r = mock(HttpServletRequest.class);
		when(r.getHeader("Authorization")).thenReturn(authHeader);
		return r;
	}

	private static BearerTokenAuthFilter filter(TokenValidator v) {
		return BearerTokenAuthFilter.create().validator(v).build();
	}

	@Test void a01_happyPath_returnsAuthResult() throws Exception {
		var f = filter(token -> ALICE);
		var result = f.authenticate(req("Bearer good-token"));
		assertTrue(result.isPresent());
		assertSame(ALICE, result.get().getPrincipal());
	}

	@Test void a02_missingHeader_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		var result = f.authenticate(req(null));
		assertTrue(result.isEmpty());
	}

	@Test void a03_blankHeader_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		var result = f.authenticate(req("   "));
		assertTrue(result.isEmpty());
	}

	@Test void a04_wrongScheme_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		var result = f.authenticate(req("Basic dXNlcjpwYXNz"));
		assertTrue(result.isEmpty());
	}

	@Test void a05_blankToken_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		var result = f.authenticate(req("Bearer   "));
		assertTrue(result.isEmpty());
	}

	@Test void a06_validatorThrows_rethrowsWithChallenge() {
		TokenValidator bad = token -> { throw new AuthenticationException("expired"); };
		var f = filter(bad);
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer tok")));
		assertNotNull(ex.getMessage());
		// Must carry a WWW-Authenticate challenge
		assertTrue(ex.getHeaders().stream().anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName())));
	}

	@Test void a07_validatorThrowsWithChallenge_preservesExistingChallenge() {
		TokenValidator detailed = token -> {
			throw new AuthenticationException("expired").wwwAuthenticate("Bearer realm=\"detail\", error=\"invalid_token\"");
		};
		var f = filter(detailed);
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer tok")));
		var challenges = ex.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.map(h -> h.getValue())
			.toList();
		assertEquals(1, challenges.size());
		assertTrue(challenges.get(0).contains("error=\"invalid_token\""));
	}

	@Test void a08_validatorReturnsNull_throws() {
		TokenValidator nullReturner = token -> null;
		var f = filter(nullReturner);
		assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer tok")));
	}

	@Test void a09_validatorThrowsRuntime_wrappedAsAuthException() {
		TokenValidator boom = token -> { throw new IllegalStateException("boom"); };
		var f = filter(boom);
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer tok")));
		assertNotNull(ex.getCause());
	}

	@Test void a10_claimsPrincipal_rolesFlowToAuthResult() throws Exception {
		var claims = Map.<String, Object>of("roles", List.of("user", "admin"), "sub", "alice");
		var cp = new ClaimsPrincipal("alice", claims);
		var f = filter(token -> cp);
		var result = f.authenticate(req("Bearer tok"));
		assertTrue(result.isPresent());
		assertEquals(Set.of("user", "admin"), result.get().getRoles());
	}

	@Test void a11_claimsPrincipal_noRolesClaim_emptyRoles() throws Exception {
		var cp = new ClaimsPrincipal("alice", Map.of("sub", "alice"));
		var f = filter(token -> cp);
		var result = f.authenticate(req("Bearer tok"));
		assertTrue(result.isPresent());
		assertTrue(result.get().getRoles().isEmpty());
	}

	@Test void a12_customRolesClaim() throws Exception {
		var claims = Map.<String, Object>of("groups", List.of("ops"), "sub", "alice");
		var cp = new ClaimsPrincipal("alice", claims);
		var f = BearerTokenAuthFilter.create().validator(token -> cp).rolesClaim("groups").build();
		var result = f.authenticate(req("Bearer tok"));
		assertTrue(result.isPresent());
		assertEquals(Set.of("ops"), result.get().getRoles());
	}

	@Test void a13_buildWithoutValidator_throws() {
		assertThrows(IllegalStateException.class, () -> BearerTokenAuthFilter.create().build());
	}
}
