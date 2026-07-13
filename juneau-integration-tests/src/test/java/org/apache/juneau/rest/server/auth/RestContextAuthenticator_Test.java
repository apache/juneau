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
 * Tests for {@link RestContext} resolution of a {@link RestAuthenticator} across the precedence rungs
 * (registered bean / {@code @Rest(authenticator=)} annotation / {@code @Bean} factory method /
 * {@link RestServlet#createAuthenticator(BeanStore)}), and the per-request fold driving {@code roleGuard} and
 * {@code @Auth}.
 *
 * @since 10.0.0
 */
class RestContextAuthenticator_Test extends TestBase {

	public static class AliceAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return o(AuthResult.of(() -> "alice", "admin"));
		}
	}

	// -----------------------------------------------------------------------------------------
	// (a) @Rest(authenticator=...) annotation.
	// -----------------------------------------------------------------------------------------

	@Rest(authenticator=AliceAuth.class)
	public static class A_Annotated extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
		@RestGet(path="/admin", roleGuard="admin") public String admin() { return "ok"; }
	}

	private static final MockRestClient A = MockRestClient.buildLax(A_Annotated.class);

	@Test void a01_annotationResolvesPrincipal() throws Exception {
		A.get("/whoami").run().assertStatus(200).assertContent("alice");
	}

	@Test void a02_annotationRoleGuardPasses() throws Exception {
		A.get("/admin").run().assertStatus(200).assertContent("ok");
	}

	// -----------------------------------------------------------------------------------------
	// (b) @Bean factory method.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class B_BeanMethod extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestAuthenticator authenticator() { return new AliceAuth(); }
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	private static final MockRestClient B = MockRestClient.buildLax(B_BeanMethod.class);

	@Test void b01_beanMethodResolves() throws Exception {
		B.get("/whoami").run().assertStatus(200).assertContent("alice");
	}

	// -----------------------------------------------------------------------------------------
	// (c) createAuthenticator override.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class C_Override extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Override public RestAuthenticator createAuthenticator(BeanStore beanStore) { return new AliceAuth(); }
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	private static final MockRestClient C = MockRestClient.buildLax(C_Override.class);

	@Test void c01_createAuthenticatorResolves() throws Exception {
		C.get("/whoami").run().assertStatus(200).assertContent("alice");
	}

	// -----------------------------------------------------------------------------------------
	// (d) No authenticator — anonymous passthrough; roleGuard denies.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class D_None extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
		@RestGet(path="/admin", roleGuard="admin") public String admin() { return "ok"; }
	}

	private static final MockRestClient D = MockRestClient.buildLax(D_None.class);

	@Test void d01_noAuthenticatorAnonymous() throws Exception {
		D.get("/whoami").run().assertStatus(200).assertContent("anon");
	}

	@Test void d02_noAuthenticatorRoleGuardDenies() throws Exception {
		D.get("/admin").run().assertStatus(403);
	}

	// -----------------------------------------------------------------------------------------
	// (e) Annotation override via class inheritance: the most-derived @Rest(authenticator=) wins.
	// -----------------------------------------------------------------------------------------

	public static class BobAuth extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return o(AuthResult.of(() -> "bob", "admin"));
		}
	}

	@Rest(authenticator=AliceAuth.class)
	public abstract static class E_Base extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet("/whoami") public String whoami(@Auth Principal p) { return p == null ? "anon" : p.getName(); }
	}

	@Rest(authenticator=BobAuth.class)
	public static class E_Derived extends E_Base {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient E = MockRestClient.buildLax(E_Derived.class);

	@Test void e01_derivedAnnotationOverridesBase() throws Exception {
		E.get("/whoami").run().assertStatus(200).assertContent("bob");
	}
}
