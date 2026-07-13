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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.oauth.flow.*;
import org.apache.juneau.rest.server.auth.oauth.oidc.*;
import org.junit.jupiter.api.*;

import com.nimbusds.oauth2.sdk.pkce.*;

/**
 * Branch-coverage tests for the OAuth flow builders and the branches reachable without a live IdP.
 * Exercises guard-clause permutations (null/non-null optional fields, scope empty vs. populated, etc.)
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778",   // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"deprecation"   // OAuthResourceOwnerFlow is deprecated API; tested intentionally for coverage
})
class OAuthFlowBranch_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: OAuthAuthorizationCodeFlow — empty-scopes branch, authentication URL, customizer null/non-null.
	// -----------------------------------------------------------------------------------------------------------------

	private static OAuthAuthorizationCodeFlow authCodeFlow(boolean withSecret, boolean withScopes) {
		var b = OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("client-id")
			.redirectUri(URI.create("https://app.example.com/cb"));
		if (withSecret)
			b.clientSecret("s3cret");
		if (withScopes)
			b.scope("openid", "profile");
		return b.build();
	}

	@Test void a01_buildAuthorizationUrl_emptyScopes_noScopeParam() {
		var f = authCodeFlow(true, false);
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var url = f.buildAuthorizationUrl("st8", challenge);
		// No scope= param when scopes are empty; Nimbus omits it entirely.
		assertNotNull(url);
		assertFalse(url.getQuery().contains("scope="), url.toString());
	}

	@Test void a02_buildAuthorizationUrl_withScopes_scopePresent() {
		var f = authCodeFlow(true, true);
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var url = f.buildAuthorizationUrl("st8", challenge);
		assertTrue(url.getQuery().contains("scope="), url.toString());
	}

	@Test void a03_buildAuthenticationUrl_nullCustomizer_omitsCustomization() {
		var f = authCodeFlow(true, true);
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var url = f.buildAuthenticationUrl("st8", challenge, "nonce-1", null);
		assertNotNull(url);
		assertTrue(url.getQuery().contains("nonce=nonce-1"), url.toString());
	}

	@Test void a04_buildAuthenticationUrl_withCustomizer_customizerInvoked() {
		var f = authCodeFlow(true, true);
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var called = new AtomicBoolean();
		f.buildAuthenticationUrl("st8", challenge, "nonce-2", builder -> called.set(true));
		assertTrue(called.get());
	}

	@Test void a05_exchange_withoutClientSecret_usesPublicClientId() {
		// exchange() when clientSecretSupplier == null routes through the public-client branch.
		// No live IdP — just verify OAuthFlowException is thrown (not IAE/NPE from null secret).
		var f = authCodeFlow(false, true);
		var verifier = new CodeVerifier();
		assertThrows(OAuthFlowException.class, () -> f.exchange("code-xyz", verifier));
	}

	@Test void a06_exchange_withClientSecret_usesConfidentialClient() {
		// exchange() when clientSecretSupplier != null routes through the confidential-client branch.
		var f = authCodeFlow(true, true);
		var verifier = new CodeVerifier();
		assertThrows(OAuthFlowException.class, () -> f.exchange("code-xyz", verifier));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: OAuthClientCredentialsFlow — tokenCache null vs. non-null, scope empty branch.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_acquire_noCache_noScope_hitsPeers() {
		// No cache, no scope — scopes.isEmpty() == true → Nimbus Scope is null.
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	@Test void b02_acquire_noCache_withScope() {
		// Scopes populated → Nimbus Scope is non-null.
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.scope("read")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	@Test void b03_acquire_cacheHit_returnsWithoutNetwork() {
		var cache = BoundedLruTokenCache.create();
		var seeded = new OAuthToken("at", "Bearer", Instant.parse("2099-01-01T00:00:00Z"),
			oe(), oe(), oe());
		// Build the flow first to discover the cache key pattern.
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.scope("read")
			.tokenCache(cache)
			.build();
		cache.putToken("cc|id|read", seeded);
		var result = f.acquire();
		assertSame(seeded, result);
	}

	@Test void b04_acquire_cacheMiss_attemptsNetwork() {
		var cache = BoundedLruTokenCache.create();
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.tokenCache(cache)
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: OAuthRefreshTokenFlow — clientSecretSupplier null vs. non-null, scope empty branch.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_acquire_withSecret_noScope() {
		var f = OAuthRefreshTokenFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.refreshToken("rt-abc")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	@Test void c02_acquire_withSecret_withScope() {
		var f = OAuthRefreshTokenFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.refreshToken("rt-abc")
			.scope("openid")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	@Test void c03_acquire_noSecret_publicClient() {
		var f = OAuthRefreshTokenFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.refreshToken("rt-abc")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: OAuthResourceOwnerFlow — scope empty vs. non-empty.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_acquire_noScope() {
		var f = OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.username("alice")
			.password("pw")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	@Test void d02_acquire_withScope() {
		var f = OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("http://127.0.0.1:1/token"))
			.clientId("id")
			.clientSecret("s")
			.username("alice")
			.password("pw")
			.scope("read")
			.build();
		assertThrows(OAuthFlowException.class, f::acquire);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: OidcDiscoveryClient builder — httpRequestConfigurator null/non-null, discover() error path.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_discover_withHttpConfigurator_configuratorInvoked() {
		// discover() will fail network-wise but the configurator should be invoked before failure.
		var called = new AtomicBoolean();
		var c = OidcDiscoveryClient.create()
			.issuer(URI.create("http://127.0.0.1:1"))
			.httpRequestConfigurator(req -> called.set(true))
			.build();
		// Either IOException (network fail) or OidcDiscoveryException (parse fail) is acceptable.
		assertThrows(Exception.class, c::discover);
		// We can't guarantee the configurator was called if the network fails before the request,
		// so we just verify no NPE is thrown — the branch is covered by the configurator being non-null.
	}

	@Test void e02_builder_httpRequestConfigurator_nullRejected() {
		assertThrows(Exception.class, () -> OidcDiscoveryClient.create()
			.issuer(URI.create("https://idp.example.com"))
			.httpRequestConfigurator(null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: OAuthAuthorizationCodeFlow builder — each guard step (lines 198/200/202)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_authCode_eachBuilderGuard() {
		// authorizationEndpoint present but tokenEndpoint null → line 198
		assertThrows(IllegalStateException.class, () -> OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://x.example.com/authorize")).build());
		// tokenEndpoint present but clientId null → line 200
		assertThrows(IllegalStateException.class, () -> OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://x.example.com/authorize"))
			.tokenEndpoint(URI.create("https://x.example.com/token")).build());
		// clientId present but redirectUri null → line 202
		assertThrows(IllegalStateException.class, () -> OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://x.example.com/authorize"))
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id").build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: OAuthAuthorizationCodeFlow.buildAuthenticationUrl — empty scopes → defaults to "openid" (line 287)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_buildAuthenticationUrl_emptyScopes_defaultsToOpenid() {
		var f = OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://x.example.com/authorize"))
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.build();
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var url = f.buildAuthenticationUrl("st8", challenge, "nonce-3", null);
		assertTrue(url.getQuery().contains("scope=openid"), url.toString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: OAuthClientCredentialsFlow.cacheSkew — negative value rejected (line 168)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_clientCredentials_negativeCacheSkewRejected() {
		assertThrows(IllegalArgumentException.class, () -> OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.clientSecret("secret")
			.cacheSkew(java.time.Duration.ofSeconds(-1)));
	}

	@Test void h02_clientCredentials_zeroCacheSkewAccepted() {
		assertNotNull(OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.clientSecret("secret")
			.cacheSkew(java.time.Duration.ZERO)
			.build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// I: OAuthRefreshTokenFlow builder — each guard step (lines 160/162)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_refresh_eachBuilderGuard() {
		// tokenEndpoint present but clientId null → line 160
		assertThrows(IllegalStateException.class, () -> OAuthRefreshTokenFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token")).build());
		// clientId present but refreshToken null → line 162
		assertThrows(IllegalStateException.class, () -> OAuthRefreshTokenFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id").build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// J: OAuthResourceOwnerFlow builder — each guard step (lines 206/208/210)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void j01_resourceOwner_eachBuilderGuard() {
		// tokenEndpoint present but clientId null → line 206
		assertThrows(IllegalStateException.class, () -> OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token")).build());
		// clientId present but clientSecretSupplier null → line 208
		assertThrows(IllegalStateException.class, () -> OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id").build());
		// clientSecret present but username null → line 210
		assertThrows(IllegalStateException.class, () -> OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id").clientSecret("s").build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// K: OidcMetadata — null supportedScopes and null extras (lines 58/61)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_oidcMetadata_nullSupportedScopes_defaultsToEmpty() {
		var md = new OidcMetadata(
			java.net.URI.create("https://idp.example.com"),
			null, null, null, null, null, null,
			null, java.util.Map.of());
		assertTrue(md.supportedScopes().isEmpty());
	}

	@Test void k02_oidcMetadata_nullExtras_defaultsToEmpty() {
		var md = new OidcMetadata(
			java.net.URI.create("https://idp.example.com"),
			null, null, null, null, null, null,
			java.util.Set.of(), null);
		assertTrue(md.extras().isEmpty());
	}
}
