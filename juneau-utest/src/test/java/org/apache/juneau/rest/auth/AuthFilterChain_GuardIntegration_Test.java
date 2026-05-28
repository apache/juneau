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

import java.io.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * End-to-end integration test verifying the composition of {@link AuthFilterChain} with
 * {@link RoleBasedRestGuard} and {@link Auth @Auth Principal} op parameters.
 *
 * <h5 class='topic'>Note on MockRest and servlet filters</h5>
 *
 * <p>
 * {@code MockRest} / {@code MockRestClient} invokes the Juneau {@code RestServlet} directly without going through
 * the Jetty servlet container, so servlet filters are <b>not</b> executed in that path.  Full end-to-end filter +
 * servlet testing requires a live Jetty container (e.g. a {@code JettyMicroserviceTest}), which is deliberately
 * outside the fast unit-test tier.
 *
 * <p>
 * This test class verifies the integration in two ways:
 * <ol>
 * 	<li><b>Filter-layer assertions</b>: directly drives {@link AuthFilterChain#doFilter} and checks that the
 * 		resulting {@link AuthenticatedRequestWrapper} carries the correct principal, roles, and attributes.
 * 	<li><b>Juneau-layer assertions</b>: uses {@code MockRestClient} to verify that the Juneau stack
 * 		({@code RoleBasedRestGuard}, {@code @Auth Principal}) responds correctly when a request arrives with
 * 		credentials already stashed by an upstream filter — simulated here via {@link BearerTokenGuard} (the
 * 		FINISHED-69 op-level guard), which uses the same {@link RestServerConstants#PRINCIPAL_ATTR} stash key.
 * </ol>
 *
 * @since 9.5.0
 */
class AuthFilterChain_GuardIntegration_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Shared fixtures.
	// -----------------------------------------------------------------------------------------

	private static final Principal ALICE = () -> "alice";
	private static final Principal BOB = () -> "bob";

	/** TokenValidator: "bearer-user" → ClaimsPrincipal(alice, roles=[user]) */
	private static final TokenValidator BEARER_VALIDATOR = token -> {
		if ("bearer-user".equals(token))
			return new ClaimsPrincipal("alice", Map.of("roles", List.of("user"), "sub", "alice"));
		if ("bearer-admin".equals(token))
			return new ClaimsPrincipal("alice", Map.of("roles", List.of("admin"), "sub", "alice"));
		throw new AuthenticationException("Unknown bearer token").wwwAuthenticate("Bearer realm=\"api\"");
	};

	/** ApiKeyStore: "apikey-admin" → ClaimsPrincipal(bob, roles=[admin]) */
	private static final ApiKeyStore KEY_STORE = key -> {
		if ("apikey-admin".equals(key))
			return Optional.of(new ClaimsPrincipal("bob", Map.of("roles", List.of("admin"), "sub", "bob")));
		return Optional.empty();
	};

	private static AuthFilterChain buildChain() {
		return AuthFilterChain.create(null)
			.append(BearerTokenAuthFilter.create().validator(BEARER_VALIDATOR).build())
			.append(ApiKeyAuthFilter.create().store(KEY_STORE).build())
			.build();
	}

	// -----------------------------------------------------------------------------------------
	// Helper: drives chain.doFilter and returns the captured wrapped request (or null when 401).
	// -----------------------------------------------------------------------------------------

	private static class Result {
		final ServletRequest captured;
		final int status;

		Result(ServletRequest captured, int status) {
			this.captured = captured;
			this.status = status;
		}
	}

	private static Result runChain(AuthFilterChain chain, String authHeader, String apiKeyHeader) throws Exception {
		var req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/api/me");
		when(req.getContextPath()).thenReturn("");
		when(req.getHeader("Authorization")).thenReturn(authHeader);
		when(req.getHeader("X-API-Key")).thenReturn(apiKeyHeader);
		when(req.getCookies()).thenReturn(null);

		var resp = mock(HttpServletResponse.class);
		var sw = new StringWriter();
		when(resp.getWriter()).thenReturn(new PrintWriter(sw));

		var captured = new Object[1];
		var status = new int[]{0};
		FilterChain captureChain = (r, s) -> captured[0] = r;
		doAnswer(inv -> { status[0] = (int) inv.getArguments()[0]; return null; }).when(resp).setStatus(anyInt());

		chain.doFilter(req, resp, captureChain);
		return new Result((ServletRequest) captured[0], status[0]);
	}

	// -----------------------------------------------------------------------------------------
	// Filter-layer tests.
	// -----------------------------------------------------------------------------------------

	@Test void a01_validBearerToken_principalAndRoleCorrect() throws Exception {
		var r = runChain(buildChain(), "Bearer bearer-user", null);
		assertNotNull(r.captured, "Request must be forwarded on success");
		assertInstanceOf(AuthenticatedRequestWrapper.class, r.captured);
		var w = (AuthenticatedRequestWrapper) r.captured;
		assertEquals("alice", w.getUserPrincipal().getName());
		assertTrue(w.isUserInRole("user"));
		assertFalse(w.isUserInRole("admin"));
	}

	@Test void a02_validApiKey_principalAndRoleCorrect() throws Exception {
		var r = runChain(buildChain(), null, "apikey-admin");
		assertNotNull(r.captured);
		var w = (AuthenticatedRequestWrapper) r.captured;
		assertEquals("bob", w.getUserPrincipal().getName());
		assertTrue(w.isUserInRole("admin"));
		assertFalse(w.isUserInRole("user"));
	}

	@Test void a03_bothCredentials_bearerPrincipalWins_rolesUnion() throws Exception {
		// Both bearer-user (user role) and apikey-admin (admin role) present.
		// Bearer is registered first → alice wins for principal; roles = union.
		var r = runChain(buildChain(), "Bearer bearer-user", "apikey-admin");
		assertNotNull(r.captured);
		var w = (AuthenticatedRequestWrapper) r.captured;
		assertEquals("alice", w.getUserPrincipal().getName());
		// Union: user (from bearer) + admin (from api-key)
		assertTrue(w.isUserInRole("user"));
		assertTrue(w.isUserInRole("admin"));
	}

	@Test void a04_noCredentials_passThroughUnchanged() throws Exception {
		// Both filters return empty — no credentials at all — pass through.
		var r = runChain(buildChain(), null, null);
		// Captured request is the original mock (not an AuthenticatedRequestWrapper)
		assertNotNull(r.captured, "Request must pass through");
		assertFalse(r.captured instanceof AuthenticatedRequestWrapper,
			"No credentials: must pass through without wrapping");
	}

	@Test void a05_invalidBearerToken_returns401() throws Exception {
		var r = runChain(buildChain(), "Bearer BAD", null);
		assertNull(r.captured, "Request must NOT be forwarded on 401");
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, r.status);
	}

	@Test void a06_principalAttrStashedForAuthArgResolver() throws Exception {
		var r = runChain(buildChain(), "Bearer bearer-user", null);
		var w = (AuthenticatedRequestWrapper) r.captured;
		// @Auth arg resolver reads PRINCIPAL_ATTR
		var attr = w.getAttribute(RestServerConstants.PRINCIPAL_ATTR);
		assertNotNull(attr);
		assertInstanceOf(Principal.class, attr);
		assertEquals("alice", ((Principal) attr).getName());
	}

	// -----------------------------------------------------------------------------------------
	// Juneau-layer tests (MockRest): verify @Auth Principal injection reads PRINCIPAL_ATTR.
	//
	// Note: MockRest does not run servlet filters, so AuthenticatedRequestWrapper is not in the
	// request chain here.  These tests use BearerTokenGuard (FINISHED-69) as a stand-in to
	// pre-populate RestServerConstants.PRINCIPAL_ATTR, verifying that the Juneau stack reads
	// the same attribute key that AuthenticatedRequestWrapper writes.  Full
	// RoleBasedRestGuard + filter-produced isUserInRole() integration requires a live Jetty
	// container test.
	// -----------------------------------------------------------------------------------------

	private static final TokenValidator MOCK_VALIDATOR = token -> {
		if ("user-tok".equals(token))
			return new ClaimsPrincipal("alice", Map.of("roles", List.of("user"), "sub", "alice"));
		throw new AuthenticationException("bad token").wwwAuthenticate("Bearer realm=\"test\"");
	};

	@Rest
	public static class GuardedResource extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(MOCK_VALIDATOR).build())
				.build();
		}

		@RestGet(path="/me")
		public String me(@Auth Principal p) {
			return p == null ? "null" : p.getName();
		}
	}

	private static final MockRestClient GUARDED = MockRestClient.buildLax(GuardedResource.class);

	@Test void b01_juneauLayer_validToken_authArgInjected() throws Exception {
		// BearerTokenGuard sets PRINCIPAL_ATTR; @Auth reads the same key.
		// This confirms AuthenticatedRequestWrapper.getAttribute(PRINCIPAL_ATTR) and the @Auth
		// resolver share the same stash contract.
		GUARDED.get("/me").header("Authorization", "Bearer user-tok")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void b02_juneauLayer_noToken_returns401() throws Exception {
		GUARDED.get("/me")
			.run()
			.assertStatus(401);
	}

	@Test void b03_juneauLayer_badToken_returns401() throws Exception {
		GUARDED.get("/me").header("Authorization", "Bearer BAD")
			.run()
			.assertStatus(401);
	}
}
