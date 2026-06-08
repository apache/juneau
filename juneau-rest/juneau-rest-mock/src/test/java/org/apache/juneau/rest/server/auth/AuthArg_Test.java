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

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Validates {@link AuthArg} &mdash; resolves {@code @Auth Principal}, bare {@code Principal}
 * (no annotation, type-driven), and {@code ClaimsPrincipal} subtype parameters.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Static MockRestClient fields are a common test pattern; resources are managed by the mock framework.
})
class AuthArg_Test extends TestBase {

	private static final Principal ALICE = () -> "alice";
	private static final ClaimsPrincipal ALICE_C = new ClaimsPrincipal("alice", Map.of("scope", "read"));

	// -----------------------------------------------------------------------------------------
	// Resource A: validator that injects ALICE (a bare Principal).
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(t -> ALICE).build())
				.build();
		}

		@RestGet(path="/annotated")
		public String annotated(@Auth Principal p) { return p == null ? "null" : p.getName(); }

		@RestGet(path="/bare")
		public String bare(Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_atAuthPrincipalResolved() throws Exception {
		ca.get("/annotated").header("Authorization", "Bearer x")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void a02_barePrincipalResolved() throws Exception {
		// No @Auth annotation - type-driven resolution should still kick in.
		ca.get("/bare").header("Authorization", "Bearer x")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	// -----------------------------------------------------------------------------------------
	// Resource B: validator that injects ClaimsPrincipal.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(t -> ALICE_C).build())
				.build();
		}

		@RestGet(path="/claims")
		public String claims(@Auth ClaimsPrincipal p) {
			return p == null ? "null" : p.getClaim("scope", String.class).orElse("none");
		}

		@RestGet(path="/bare-claims")
		public String bareClaims(ClaimsPrincipal p) {
			return p == null ? "null" : p.getName();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_claimsPrincipalSubtypeResolved() throws Exception {
		cb.get("/claims").header("Authorization", "Bearer x")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("read");
	}

	@Test void b02_bareClaimsPrincipalResolved() throws Exception {
		cb.get("/bare-claims").header("Authorization", "Bearer x")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	// -----------------------------------------------------------------------------------------
	// Resource C: no guard - AuthArg returns null since no principal is stashed.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/me")
		public String me(@Auth Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_unprotectedPathInjectsNull() throws Exception {
		// Without a guard, AuthArg has no principal to pull -> returns null.
		// The guard chain is the contract that guarantees non-null; this op is documenting
		// the resolver's "no stash" behavior.
		cc.get("/me").run().assertStatus(200).assertContent().asString().is("null");
	}

	// -----------------------------------------------------------------------------------------
	// Resource D: stash an incompatible value to confirm type-mismatch path returns null.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class D extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestStartCall
		public void stashWrongType(HttpServletRequest req) {
			req.setAttribute(RestServerConstants.PRINCIPAL_ATTR, "not-a-principal");
		}

		@RestGet(path="/me")
		public String me(@Auth Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient cd = MockRestClient.buildLax(D.class);

	@Test void d01_incompatibleStashedTypeInjectsNull() throws Exception {
		cd.get("/me").run().assertStatus(200).assertContent().asString().is("null");
	}
}
