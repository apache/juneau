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
package org.apache.juneau.rest.server.auth.oauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link OAuthFilter} &mdash; happy path, missing header, validator failure, and scope-claim
 * role extraction (whitespace-split per RFC 6749 &sect;3.3).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice.
	"java:S5976" // Similar-shaped header/scope cases assert distinct authentication outcomes; parameterizing would obscure intent.
})
class OAuthFilter_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";

	private static HttpServletRequest req(String authHeader) {
		var r = mock(HttpServletRequest.class);
		when(r.getHeader("Authorization")).thenReturn(authHeader);
		return r;
	}

	private static OAuthFilter filter(TokenValidator v) {
		return OAuthFilter.create().validator(v).build();
	}

	@Test void a01_happyPath_returnsAuthResult() throws Exception {
		var f = filter(token -> ALICE);
		var result = f.authenticate(req("Bearer good-token"));
		assertTrue(result.isPresent());
		assertSame(ALICE, result.get().getPrincipal());
	}

	@Test void a02_missingHeader_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		assertTrue(f.authenticate(req(null)).isEmpty());
	}

	@Test void a03_wrongScheme_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		assertTrue(f.authenticate(req("Basic xyz")).isEmpty());
	}

	@Test void a04_blankToken_returnsEmpty() throws Exception {
		var f = filter(token -> ALICE);
		assertTrue(f.authenticate(req("Bearer    ")).isEmpty());
	}

	@Test void b01_validatorThrows_propagatesWithChallenge() {
		TokenValidator v = token -> { throw new AuthenticationException("bad token"); };
		var f = filter(v);
		var ex = assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer x")));
		assertTrue(ex.getHeaders().stream()
			.anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName())));
	}

	@Test void b02_validatorReturnsNull_throws() {
		TokenValidator v = token -> null;
		var f = filter(v);
		assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer x")));
	}

	@Test void c01_scopeClaim_splitOnWhitespace() throws Exception {
		Principal cp = new ClaimsPrincipal("alice", Map.of("scope", "read:orders write:orders admin"));
		TokenValidator v = token -> cp;
		var f = OAuthFilter.create().validator(v).build();
		var roles = f.authenticate(req("Bearer x")).get().getRoles();
		assertEquals(Set.of("read:orders", "write:orders", "admin"), roles);
	}

	@Test void c02_scopeClaim_asList_collected() throws Exception {
		Principal cp = new ClaimsPrincipal("alice", Map.of("scope", List.of("read", "write")));
		TokenValidator v = token -> cp;
		var f = OAuthFilter.create().validator(v).build();
		assertEquals(Set.of("read", "write"), f.authenticate(req("Bearer x")).get().getRoles());
	}

	@Test void c03_customRolesClaim() throws Exception {
		Principal cp = new ClaimsPrincipal("alice", Map.of("groups", List.of("admin", "user")));
		TokenValidator v = token -> cp;
		var f = OAuthFilter.create().validator(v).rolesClaim("groups").build();
		assertEquals(Set.of("admin", "user"), f.authenticate(req("Bearer x")).get().getRoles());
	}

	@Test void d01_builder_validatorRequired() {
		assertThrows(IllegalStateException.class, () -> OAuthFilter.create().build());
	}

	@Test void d02_builder_realmCustomization() throws Exception {
		TokenValidator v = token -> { throw new AuthenticationException("bad"); };
		var f = OAuthFilter.create().validator(v).realm("api2").build();
		var e = assertThrows(AuthenticationException.class, () -> f.authenticate(req("Bearer x")));
		var hdr = e.getHeaders().stream()
			.filter(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()))
			.findFirst().orElseThrow();
		assertTrue(hdr.getValue().contains("api2"));
	}
}
