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
package org.apache.juneau.rest.server.auth.oidc.rp;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Builder-guard tests for {@link OidcRelyingParty} — covers the missing-required-field branches
 * and the validator branches on setters that accept bad values.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class OidcRelyingPartyBuilder_Test extends TestBase {

	// Minimal valid base builder — all required fields supplied.
	private static OidcRelyingParty.Builder base() {
		return OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: build() missing required fields (line 408 guard branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_build_requiresIssuerOrMetadata() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create())
			.build());
	}

	@Test void a02_build_requiresClientId() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.redirectUri(URI.create("https://app.example.com/cb"))
			.sessionStore(InMemorySessionStore.create())
			.build());
	}

	@Test void a03_build_requiresRedirectUri() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.clientId("app")
			.sessionStore(InMemorySessionStore.create())
			.build());
	}

	@Test void a04_build_requiresSessionStore() {
		assertThrows(IllegalStateException.class, () -> OidcRelyingParty.create()
			.issuer(URI.create("https://idp.example.com"))
			.clientId("app")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: sessionTtl — zero and negative are rejected (line 316 branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_sessionTtl_zeroRejected() {
		assertThrows(Exception.class, () -> base().sessionTtl(Duration.ZERO));
	}

	@Test void b02_sessionTtl_negativeRejected() {
		assertThrows(Exception.class, () -> base().sessionTtl(Duration.ofHours(-1)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: clockSkewSeconds — negative is rejected (line 408 branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_clockSkewSeconds_negativeRejected() {
		assertThrows(Exception.class, () -> base().clockSkewSeconds(-1));
	}

	@Test void c02_clockSkewSeconds_zeroAccepted() {
		assertDoesNotThrow(() -> base().clockSkewSeconds(0));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: userInfoClaims — blank and null values are rejected (line 448 branches).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_userInfoClaims_blankRejected() {
		assertThrows(Exception.class, () -> base().userInfoClaims("openid", ""));
	}

	@Test void d02_userInfoClaims_validAccumulates() {
		assertDoesNotThrow(() -> base().userInfoClaims("email", "name"));
	}
}
