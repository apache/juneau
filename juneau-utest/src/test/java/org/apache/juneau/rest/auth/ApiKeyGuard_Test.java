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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link ApiKeyGuard} &mdash; header / query / cookie source configuration, the
 * {@code WWW-Authenticate: ApiKey realm="..."} challenge, principal stashing, store-throws path,
 * and builder validation.
 *
 * @since 10.0.0
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class ApiKeyGuard_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Shared store: keys "alice-key" -> alice, "bob-key" -> bob.
	// -----------------------------------------------------------------------------------------

	private static final Map<String,Principal> KEYS = Map.of(
		"alice-key", () -> "alice",
		"bob-key",   () -> "bob"
	);

	private static final ApiKeyStore STORE = key -> Optional.ofNullable(KEYS.get(key));

	private static final ApiKeyStore THROWING_STORE = key -> {
		throw new IllegalStateException("store boom");
	};

	// -----------------------------------------------------------------------------------------
	// Resource A: default header source ("X-API-Key").
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(ApiKeyGuard.create().store(STORE).build())
				.build();
		}

		@RestGet(path="/me")
		public String me(@Auth Principal p) {
			return p == null ? "null" : p.getName();
		}
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_happyPath_defaultHeader() throws Exception {
		ca.get("/me").header("X-API-Key", "alice-key")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void a02_missingHeader_returns401WithChallenge() throws Exception {
		ca.get("/me")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("ApiKey realm=\"api\"");
	}

	@Test void a03_unknownKey_returns401() throws Exception {
		ca.get("/me").header("X-API-Key", "no-such-key")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("ApiKey realm=\"api\"")
			.assertContent().asString().isContains("API key not recognized");
	}

	@Test void a04_blankHeader_returns401() throws Exception {
		ca.get("/me").header("X-API-Key", "")
			.run()
			.assertStatus(401);
	}

	// -----------------------------------------------------------------------------------------
	// Resource B: custom header name + custom realm.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(ApiKeyGuard.create()
					.store(STORE)
					.fromHeader("X-Acme-Token")
					.realm("acme")
					.build())
				.build();
		}

		@RestGet(path="/me") public String me(@Auth Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_customHeader_happyPath() throws Exception {
		cb.get("/me").header("X-Acme-Token", "bob-key")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("bob");
	}

	@Test void b02_customRealmInChallenge() throws Exception {
		cb.get("/me")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("ApiKey realm=\"acme\"");
	}

	@Test void b03_defaultHeaderIgnoredWhenCustomConfigured() throws Exception {
		cb.get("/me").header("X-API-Key", "alice-key")
			.run()
			.assertStatus(401);
	}

	// -----------------------------------------------------------------------------------------
	// Resource C: query-param source.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(ApiKeyGuard.create().store(STORE).fromQuery("apiKey").build())
				.build();
		}

		@RestGet(path="/me") public String me(@Auth Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_queryParam_happyPath() throws Exception {
		cc.get("/me?apiKey=alice-key")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void c02_missingQueryParam_returns401() throws Exception {
		cc.get("/me")
			.run()
			.assertStatus(401);
	}

	@Test void c03_blankQueryParam_returns401() throws Exception {
		cc.get("/me?apiKey=")
			.run()
			.assertStatus(401);
	}

	// -----------------------------------------------------------------------------------------
	// Resource D: cookie source.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class D extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(ApiKeyGuard.create().store(STORE).fromCookie("api_key").build())
				.build();
		}

		@RestGet(path="/me") public String me(@Auth Principal p) { return p == null ? "null" : p.getName(); }
	}

	private static final MockRestClient cd = MockRestClient.buildLax(D.class);

	@Test void d01_cookie_happyPath() throws Exception {
		cd.get("/me").header("Cookie", "api_key=alice-key")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	@Test void d02_missingCookie_returns401() throws Exception {
		cd.get("/me")
			.run()
			.assertStatus(401);
	}

	@Test void d03_otherCookiesIgnored() throws Exception {
		cd.get("/me").header("Cookie", "session=abc; other=xyz")
			.run()
			.assertStatus(401);
	}

	@Test void d04_correctCookieAmongOthers() throws Exception {
		cd.get("/me").header("Cookie", "session=abc; api_key=bob-key; other=xyz")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("bob");
	}

	@Test void d05_malformedCookiePairSkipped() throws Exception {
		// A bare token with no '=' should be silently skipped, not crash the parser.
		cd.get("/me").header("Cookie", "flagonly; api_key=alice-key; other")
			.run()
			.assertStatus(200)
			.assertContent().asString().is("alice");
	}

	// -----------------------------------------------------------------------------------------
	// Resource E: throwing store wraps as AuthenticationException.
	// -----------------------------------------------------------------------------------------

	@Rest
	public static class E extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(ApiKeyGuard.create().store(THROWING_STORE).build())
				.build();
		}

		@RestGet(path="/me") public String me(@Auth Principal p) { return "ok"; }
	}

	private static final MockRestClient ce = MockRestClient.buildLax(E.class);

	@Test void e01_throwingStoreWrapsAs401() throws Exception {
		ce.get("/me").header("X-API-Key", "alice-key")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("ApiKey realm=\"api\"");
	}

	// -----------------------------------------------------------------------------------------
	// Builder validation.
	// -----------------------------------------------------------------------------------------

	@Test void f01_buildWithoutStoreRejected() {
		Assertions.assertThrows(IllegalStateException.class,
			() -> ApiKeyGuard.create().build());
	}

	@Test void f02_nullStoreRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ApiKeyGuard.create().store(null));
	}

	@Test void f03_blankHeaderNameRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ApiKeyGuard.create().fromHeader("   "));
	}

	@Test void f04_blankQueryNameRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ApiKeyGuard.create().fromQuery(""));
	}

	@Test void f05_blankCookieNameRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ApiKeyGuard.create().fromCookie(null));
	}

	@Test void f06_blankRealmRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> ApiKeyGuard.create().realm(""));
	}

	@Test void f07_convenienceConstructor() {
		var g = new ApiKeyGuard(STORE);
		Assertions.assertNotNull(g);
	}

	@Test void f08_isRequestAllowedReturnsFalse() {
		var g = ApiKeyGuard.create().store(STORE).build();
		Assertions.assertFalse(g.isRequestAllowed(null));
	}
}
