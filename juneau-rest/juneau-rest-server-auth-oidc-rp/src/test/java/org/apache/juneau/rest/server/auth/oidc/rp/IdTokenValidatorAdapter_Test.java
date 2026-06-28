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

import static org.apache.juneau.rest.server.auth.oidc.rp.OidcTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.*;

/**
 * Tests for {@link IdTokenValidatorAdapter} &mdash; OIDC ID-token signature + claim validation via Nimbus.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class IdTokenValidatorAdapter_Test extends TestBase {

	private static final String ISS = "https://stub-idp.example.com";
	private static final String CID = "web-app";

	private RSAKey key;
	private Instant now;

	@SuppressWarnings({
		"java:S8692" // Nimbus oauth2-oidc-sdk 11.37.2 exposes no clock hook on the ID-token path (IDTokenClaimsVerifier reads new Date() internally), so injecting a clock there would require cloning Nimbus internals; tokens are minted at real "now" and validated against the real system clock.
	})
	@BeforeEach void setup() throws Exception {
		key = generateRsa("k1");
		now = Instant.now();  // validation uses the real system clock, so anchor tokens to "now"
	}

	private IdTokenValidatorAdapter validator() {
		return IdTokenValidatorAdapter.create().issuer(ISS).clientId(CID).jwkSet(publicJwks(key)).build();
	}

	@Test void a01_validToken_projectsClaims() throws Exception {
		var jwt = signIdToken(key, ISS, CID, "alice", "sess-1", "nonce-1", now, Duration.ofMinutes(5), null);
		var p = validator().validate(jwt, "nonce-1");
		assertEquals("alice", p.getName());
		assertEquals("sess-1", p.getClaim("sid", String.class).orElse(null));
	}

	@Test void b01_wrongNonce_rejected() throws Exception {
		var jwt = signIdToken(key, ISS, CID, "alice", null, "nonce-1", now, Duration.ofMinutes(5), null);
		assertThrows(AuthenticationException.class, () -> validator().validate(jwt, "different-nonce"));
	}

	@Test void b02_wrongIssuer_rejected() throws Exception {
		var jwt = signIdToken(key, "https://evil.example.com", CID, "alice", null, "n", now, Duration.ofMinutes(5), null);
		assertThrows(AuthenticationException.class, () -> validator().validate(jwt, "n"));
	}

	@Test void b03_wrongAudience_rejected() throws Exception {
		var jwt = signIdToken(key, ISS, "some-other-client", "alice", null, "n", now, Duration.ofMinutes(5), null);
		assertThrows(AuthenticationException.class, () -> validator().validate(jwt, "n"));
	}

	@Test void b04_expiredToken_rejected() throws Exception {
		var past = now.minus(Duration.ofHours(1));
		var jwt = signIdToken(key, ISS, CID, "alice", null, "n", past, Duration.ofMinutes(5), null);
		assertThrows(AuthenticationException.class, () -> validator().validate(jwt, "n"));
	}

	@Test void b05_wrongSigningKey_rejected() throws Exception {
		var attacker = generateRsa("k1");  // same kid, different key material
		var jwt = signIdToken(attacker, ISS, CID, "alice", null, "n", now, Duration.ofMinutes(5), null);
		assertThrows(AuthenticationException.class, () -> validator().validate(jwt, "n"));
	}

	@Test void b06_garbageToken_rejected() {
		assertThrows(AuthenticationException.class, () -> validator().validate("not-a-jwt", "n"));
	}

	@Test void c01_noneAlgorithm_rejectedAtBuild() {
		assertThrows(IllegalArgumentException.class,
			() -> IdTokenValidatorAdapter.create().algorithms(new JWSAlgorithm("none")));
	}

	@Test void c02_requiresExactlyOneJwkSource() {
		assertThrows(IllegalStateException.class,
			() -> IdTokenValidatorAdapter.create().issuer(ISS).clientId(CID).build());
		assertThrows(IllegalStateException.class,
			() -> IdTokenValidatorAdapter.create().issuer(ISS).clientId(CID)
				.jwkSet(publicJwks(key)).jwksUri(URI.create("https://idp/jwks")).build());
	}

	@Test void c03_requiresIssuerAndClientId() {
		assertThrows(IllegalStateException.class,
			() -> IdTokenValidatorAdapter.create().clientId(CID).jwkSet(publicJwks(key)).build());
		assertThrows(IllegalStateException.class,
			() -> IdTokenValidatorAdapter.create().issuer(ISS).jwkSet(publicJwks(key)).build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: algorithms() guard branches not yet covered.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_algorithms_emptyArray_rejected() {
		// values.length == 0 → assertArg false branch (line 157).
		assertThrows(IllegalArgumentException.class,
			() -> IdTokenValidatorAdapter.create().algorithms());
	}

	@Test void d02_algorithms_nullElement_rejected() {
		// null element → assertArgNotNull false branch (line 159).
		assertThrows(IllegalArgumentException.class,
			() -> IdTokenValidatorAdapter.create().algorithms((JWSAlgorithm) null));
	}

	@Test void d03_algorithms_validAlgorithm_accepted() {
		// RS256 is not NONE → assertArg true branch (line 161 TRUE branch). Build also uses jwkSet.
		assertDoesNotThrow(() -> IdTokenValidatorAdapter.create()
			.issuer(ISS).clientId(CID).jwkSet(publicJwks(key))
			.algorithms(JWSAlgorithm.RS256)
			.build());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: maxClockSkewSeconds negative rejected (line 175 false branch).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_maxClockSkewSeconds_negativeRejected() {
		assertThrows(IllegalArgumentException.class,
			() -> IdTokenValidatorAdapter.create().maxClockSkewSeconds(-1));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: resolveSource — jwksUri path (line 217 false branch when jwkSet is null).
	// F: validate — null expectedNonce (line 246 false branch).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_jwksUri_path_buildSucceeds() {
		// jwkSet is null, jwksUri is set — resolveSource takes the jwksUri branch (line 217 false).
		assertDoesNotThrow(() -> IdTokenValidatorAdapter.create()
			.issuer(ISS).clientId(CID)
			.jwksUri(URI.create("https://stub-idp.example.com/.well-known/jwks.json"))
			.build());
	}

	@Test void f02_validate_nullNonce_passedThrough() throws Exception {
		// expectedNonce == null → ternary false branch (line 246 null → null Nonce).
		// Token is minted without nonce; validate() called with null nonce.
		var jwt = signIdToken(key, ISS, CID, "alice", null, null, now, Duration.ofMinutes(5), null);
		assertDoesNotThrow(() -> validator().validate(jwt, null));
	}
}
