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

import java.security.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BearerTokenGuard} &mdash; happy path, missing-header / malformed-header / blank-token
 * rejection paths, validator-throws path, the {@code WWW-Authenticate: Bearer realm="..."} challenge,
 * principal stashing, and builder validation.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class BearerTokenGuard_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Shared validators.
	// -----------------------------------------------------------------------------------------

	private static final Principal ALICE = () -> "alice";

	/** Accepts only the literal token "good"; throws for everything else. */
	private static final TokenValidator V_GOOD = token -> {
		if ("good".equals(token))
			return ALICE;
		throw new AuthenticationException("Bad token");
	};

	/** Validator that returns null (treated as failure). */
	private static final TokenValidator V_NULL = token -> null;

	/** Validator that throws a non-Authentication runtime exception. */
	private static final TokenValidator V_BOOM = token -> {
		throw new IllegalStateException("validator boom");
	};

	/** Validator that throws an AuthenticationException pre-set with its own richer challenge. */
	private static final TokenValidator V_DETAIL = token ->
		{ throw new AuthenticationException("Token expired").wwwAuthenticate("Bearer realm=\"detail\", error=\"invalid_token\""); };

	// -----------------------------------------------------------------------------------------
	// Resource A: V_GOOD validator, default realm.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(V_GOOD).build())
				.build();
		}

		@RestGet(path="/me")
		public String me(@Auth Principal p) {
			return p == null ? "null" : p.getName();
		}
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_happyPath_principalInjected() throws Exception {
		ca.get("/me").header("Authorization", "Bearer good")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void a02_missingAuthorization_returns401WithChallenge() throws Exception {
		ca.get("/me")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"api\"");
	}

	@Test void a03_emptyAuthorization_returns401() throws Exception {
		ca.get("/me").header("Authorization", "")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"api\"");
	}

	@Test void a04_basicSchemeRejected() throws Exception {
		ca.get("/me").header("Authorization", "Basic dXNlcjpwYXNz")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"api\"");
	}

	@Test void a05_bearerWithoutTokenRejected() throws Exception {
		ca.get("/me").header("Authorization", "Bearer ")
			.run()
			.assertStatus(401);
	}

	@Test void a06_bearerWithBlankTokenRejected() throws Exception {
		ca.get("/me").header("Authorization", "Bearer    ")
			.run()
			.assertStatus(401);
	}

	@Test void a07_bearerSchemeCaseInsensitive() throws Exception {
		ca.get("/me").header("Authorization", "bEaReR good")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void a08_validatorRejects_returns401WithReason() throws Exception {
		ca.get("/me").header("Authorization", "Bearer wrong")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"api\"")
			.assertContent().asString().isContains("Bad token");
	}

	// -----------------------------------------------------------------------------------------
	// Resource B: V_NULL validator (returns null) + custom realm.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(V_NULL).realm("custom-realm").build())
				.build();
		}

		@RestGet(path="/me")
		public String me(@Auth Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_nullPrincipalTreatedAsFailure() throws Exception {
		cb.get("/me").header("Authorization", "Bearer xxx")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"custom-realm\"");
	}

	// -----------------------------------------------------------------------------------------
	// Resource C: V_BOOM validator (runtime exception) wraps as AuthenticationException.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(V_BOOM).build())
				.build();
		}

		@RestGet(path="/me") public String me(@Auth Principal p) { return "ok"; }
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_runtimeExceptionWrappedAs401() throws Exception {
		cc.get("/me").header("Authorization", "Bearer x")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"api\"");
	}

	// -----------------------------------------------------------------------------------------
	// Resource D: validator sets its own challenge - guard preserves it.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class D extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().validator(V_DETAIL).build())
				.build();
		}

		@RestGet(path="/me") public String me(@Auth Principal p) { return "ok"; }
	}

	private static final MockRestClient cd = MockRestClient.buildLax(D.class);

	@Test void d01_validatorSuppliedChallengePreserved() throws Exception {
		cd.get("/me").header("Authorization", "Bearer x")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"detail\", error=\"invalid_token\"");
	}

	// -----------------------------------------------------------------------------------------
	// Builder validation.
	// -----------------------------------------------------------------------------------------

	@Test void e01_buildWithoutValidatorRejected() {
		Assertions.assertThrows(IllegalStateException.class,
			() -> BearerTokenGuard.create().build());
	}

	@Test void e02_nullValidatorRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BearerTokenGuard.create().validator(null));
	}

	@Test void e03_blankRealmRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BearerTokenGuard.create().realm("  "));
	}

	@Test void e04_convenienceConstructor() {
		var g = new BearerTokenGuard(V_GOOD);
		Assertions.assertNotNull(g);
	}

	@Test void e05_isRequestAllowedReturnsFalse() {
		// Sanity: the abstract method returns false; the guard overrides guard(req,res) directly.
		var g = BearerTokenGuard.create().validator(V_GOOD).build();
		Assertions.assertFalse(g.isRequestAllowed(null));
	}
}
