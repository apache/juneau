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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end integration tests for the {@link RestAuthenticator} request lifecycle: child-resource inheritance,
 * {@code ADD} role-augmentation vs {@code REPLACE} principal-override merge semantics, {@code noInherit} subtree
 * opt-out, anonymous passthrough, {@code 401} on failure, and dependency-injected authenticators (constructor +
 * {@code @Bean} factory).
 *
 * @since 10.0.0
 */
class RestAuthenticatorE2E_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Authenticators.
	// -----------------------------------------------------------------------------------------

	/** Root: principal=alice, roles=[root]. */
	public static class RootAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return o(AuthResult.of(() -> "alice", "root"));
		}
	}

	/** ADD: roles-only, augments accumulated principal/roles. */
	public static class AddRolesAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return o(AuthResult.ofRoles("extra"));
		}
	}

	/** REPLACE: discards accumulated, sets principal=bob, roles=[admin]. */
	public static class ReplaceAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return o(AuthResult.replacing(() -> "bob", "admin"));
		}
	}

	/** Always fails. */
	public static class ThrowAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			throw new AuthenticationException("nope").wwwAuthenticate("Bearer realm=\"test\"");
		}
	}

	// -----------------------------------------------------------------------------------------
	// Child resources.
	// -----------------------------------------------------------------------------------------

	@Rest(path="/child")
	public static class InheritChild {
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	@Rest(path="/childAdd", authenticator=AddRolesAuth.class)
	public static class AddChild {
		@RestGet(path="/needsExtra", roleGuard="extra") public String needsExtra(@Auth Principal p) { return p.getName(); }
	}

	@Rest(path="/childReplace", authenticator=ReplaceAuth.class)
	public static class ReplaceChild {
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	@Rest(path="/public", noInherit="authenticator")
	public static class PublicChild {
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	@Rest(
		path="/root",
		authenticator=RootAuth.class,
		children={InheritChild.class, AddChild.class, ReplaceChild.class, PublicChild.class}
	)
	public static class Root extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	private static final MockRestClient ROOT = MockRestClient.buildLax(Root.class);

	@Test void a01_rootResolvesPrincipal() throws Exception {
		ROOT.get("/whoami").run().assertStatus(200).assertContent("alice");
	}

	@Test void a02_childInheritsRootAuthenticator() throws Exception {
		ROOT.get("/child/whoami").run().assertStatus(200).assertContent("alice");
	}

	@Test void a03_addChildAugmentsRoles_principalUnchanged() throws Exception {
		// Root sets alice+[root]; child ADD adds role 'extra'.  roleGuard 'extra' passes; principal stays alice.
		ROOT.get("/childAdd/needsExtra").run().assertStatus(200).assertContent("alice");
	}

	@Test void a04_replaceChildOverridesPrincipal() throws Exception {
		// Root sets alice; child REPLACE swaps to bob.
		ROOT.get("/childReplace/whoami").run().assertStatus(200).assertContent("bob");
	}

	@Test void a05_noInheritSubtreeOptOut_anonymous() throws Exception {
		ROOT.get("/public/whoami").run().assertStatus(200).assertContent("anon");
	}

	// -----------------------------------------------------------------------------------------
	// 401 on failure.
	// -----------------------------------------------------------------------------------------

	@Rest(authenticator=ThrowAuth.class)
	public static class Failing extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	private static final MockRestClient FAILING = MockRestClient.buildLax(Failing.class);

	@Test void b01_failureReturns401() throws Exception {
		FAILING.get("/whoami").run().assertStatus(401);
	}

	// -----------------------------------------------------------------------------------------
	// DI injection: @Bean RestAuthenticator authenticator(Authenticator inner) where inner is itself a @Bean.
	// Proves constructor/bean injection at both levels via RestAuthenticator.of(inner).
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class Injected extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public Authenticator inner() {
			return req -> o(AuthResult.of(() -> "carol", "admin"));
		}

		@Bean public RestAuthenticator authenticator(Authenticator inner) {
			return RestAuthenticator.of(inner);
		}

		@RestGet(path="/whoami", roleGuard="admin") public String whoami(@Auth Principal p) { return p.getName(); }
	}

	private static final MockRestClient INJECTED = MockRestClient.buildLax(Injected.class);

	@Test void c01_diInjectedAuthenticatorChain() throws Exception {
		INJECTED.get("/whoami").run().assertStatus(200).assertContent("carol");
	}

	// -----------------------------------------------------------------------------------------
	// Cross-layer augment/replace (spec §5.6/§5.7): a request already authenticated upstream (container security
	// or a servlet-layer AuthFilterChain) arrives with a non-null getUserPrincipal.  A roles-only resource
	// authenticator (ADD) must augment it (keep the principal, union roles) rather than drop the roles; a
	// REPLACE result must still override the pre-existing identity.
	// -----------------------------------------------------------------------------------------

	@Rest(authenticator=AddRolesAuth.class)
	public static class PreAuthAugment extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(RestRequest req) {
			return req.getUserPrincipal().getName() + "|" + req.isUserInRole("base") + "|" + req.isUserInRole("extra");
		}
	}

	private static final MockRestClient PREAUTH_AUGMENT = MockRestClient.buildLax(PreAuthAugment.class);

	@Test void d01_crossLayerRolesOnlyAugmentsExistingPrincipal() throws Exception {
		// Pre-authenticated upstream: principal=alice, container role=base.  Resource adds role 'extra' (roles-only ADD).
		PREAUTH_AUGMENT.get("/whoami").userPrincipal(() -> "alice").roles("base").run()
			.assertStatus(200).assertContent("alice|true|true");
	}

	@Rest(authenticator=ReplaceAuth.class)
	public static class PreAuthReplace extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(RestRequest req) {
			return req.getUserPrincipal().getName() + "|" + req.isUserInRole("admin");
		}
	}

	private static final MockRestClient PREAUTH_REPLACE = MockRestClient.buildLax(PreAuthReplace.class);

	@Test void d02_crossLayerReplaceOverridesExistingPrincipal() throws Exception {
		// Pre-authenticated alice; resource REPLACE swaps identity to bob with role admin.
		PREAUTH_REPLACE.get("/whoami").userPrincipal(() -> "alice").roles("base").run()
			.assertStatus(200).assertContent("bob|true");
	}
}
