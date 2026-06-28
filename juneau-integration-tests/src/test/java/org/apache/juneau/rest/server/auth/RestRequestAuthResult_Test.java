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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link RestRequest} authentication overrides ({@link RestRequest#getUserPrincipal()},
 * {@link RestRequest#isUserInRole(String)}, {@link RestRequest#getRemoteUser()}) backed by a stored
 * {@link AuthResult} via {@link RestRequest#setAuthResult(AuthResult)}.
 *
 * @since 10.0.0
 */
class RestRequestAuthResult_Test extends TestBase {

	public static class AliceAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return opt(AuthResult.of(() -> "alice", "admin", "user"));
		}
	}

	@Rest(authenticator=AliceAuth.class)
	public static class A_Authenticated extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/info") public String info(RestRequest req) {
			return req.getUserPrincipal().getName()
				+ "|" + req.getRemoteUser()
				+ "|" + req.isUserInRole("admin")
				+ "|" + req.isUserInRole("user")
				+ "|" + req.isUserInRole("nope");
		}
	}

	private static final MockRestClient A = MockRestClient.buildLax(A_Authenticated.class);

	@Test void a01_overridesReflectStoredResult() throws Exception {
		A.get("/info").run().assertStatus(200).assertContent("alice|alice|true|true|false");
	}

	// -----------------------------------------------------------------------------------------
	// No authenticator: overrides fall through to the underlying request (null principal/roles).
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class B_Anonymous extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/info") public String info(RestRequest req) {
			return (req.getUserPrincipal() == null) + "|" + (req.getRemoteUser() == null) + "|" + req.isUserInRole("admin");
		}
	}

	private static final MockRestClient B = MockRestClient.buildLax(B_Anonymous.class);

	@Test void b01_fallThroughWhenNoResult() throws Exception {
		B.get("/info").run().assertStatus(200).assertContent("true|true|false");
	}

	// -----------------------------------------------------------------------------------------
	// Roles-only result with no principal anywhere: the fold yields no identity (see
	// AuthResultAccumulator "no identity => not a successful auth"), so nothing is applied and the overrides
	// fall through exactly as for the anonymous case.
	// -----------------------------------------------------------------------------------------

	public static class RolesOnlyAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return opt(AuthResult.ofRoles("editor"));
		}
	}

	@Rest(authenticator=RolesOnlyAuth.class)
	public static class C_RolesOnly extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/info") public String info(RestRequest req) {
			return (req.getUserPrincipal() == null) + "|" + (req.getRemoteUser() == null) + "|" + req.isUserInRole("editor");
		}
	}

	private static final MockRestClient C = MockRestClient.buildLax(C_RolesOnly.class);

	@Test void c01_rolesOnlyNoPrincipalNotApplied() throws Exception {
		C.get("/info").run().assertStatus(200).assertContent("true|true|false");
	}

	@Test void c02_rolesOnlyAugmentsPreAuthenticatedPrincipal() throws Exception {
		// With an upstream principal present, the roles-only result augments it (spec §5.6): principal kept,
		// role applied — restoring the augment semantics for the seeded-identity case.
		C.get("/info").userPrincipal(() -> "dave").run().assertStatus(200).assertContent("false|false|true");
	}
}
