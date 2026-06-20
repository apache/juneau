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

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.rest.server.auth.oauth.oidc.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.*;

/**
 * Test helpers + an in-JVM stub OpenID Connect provider for the {@code OidcRelyingParty} end-to-end
 * tests.  Lives in the test tree (never compiled into production), in {@code juneau-integration-tests} to match the
 * centralized-tests convention.
 *
 * <p>
 * The stub serves a real {@code /token} (and {@code /userinfo}) endpoint over loopback so the Nimbus
 * token exchange in {@code OAuthAuthorizationCodeFlow#exchange} performs a genuine HTTP round-trip.
 * Discovery and JWKS are short-circuited by injecting {@link OidcMetadata} and a public {@link JWKSet}
 * directly on the relying party (so no second listener is needed and the tests stay deterministic).
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
final class OidcTestSupport {

	static final String EVENT_BACKCHANNEL_LOGOUT = "http://schemas.openid.net/event/backchannel-logout";

	private OidcTestSupport() {}

	/** Generates a fresh RSA-2048 signing key with the given key id. */
	static RSAKey generateRsa(String kid) throws JOSEException {
		return new RSAKeyGenerator(2048).keyID(kid).algorithm(JWSAlgorithm.RS256).generate();
	}

	/** Public-only JWK set for the supplied key(s) (what an IdP would publish at its JWKS endpoint). */
	static JWKSet publicJwks(JWK...keys) {
		return new JWKSet(Arrays.asList(keys)).toPublicJWKSet();
	}

	/** Signs an OIDC ID token (RS256) with the standard claims plus an optional {@code sid}. */
	static String signIdToken(RSAKey key, String issuer, String clientId, String subject, String sid, String nonce,
			Instant now, Duration ttl, Map<String,Object> extraClaims) throws JOSEException {
		var cb = new JWTClaimsSet.Builder()
			.issuer(issuer)
			.subject(subject)
			.audience(clientId)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plus(ttl)))
			.claim("nonce", nonce);
		if (sid != null)
			cb.claim("sid", sid);
		if (extraClaims != null)
			extraClaims.forEach(cb::claim);
		return sign(key, JWSAlgorithm.RS256, null, cb.build());
	}

	/** Signs an OIDC back-channel {@code logout_token} (RS256) targeting a {@code sub} and/or {@code sid}. */
	static String signLogoutToken(RSAKey key, String issuer, String clientId, String subject, String sid, Instant now)
			throws JOSEException {
		var events = new LinkedHashMap<String,Object>();
		events.put(EVENT_BACKCHANNEL_LOGOUT, new LinkedHashMap<>());
		var cb = new JWTClaimsSet.Builder()
			.issuer(issuer)
			.audience(clientId)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plus(Duration.ofMinutes(2))))
			.jwtID(UUID.randomUUID().toString())
			.claim("events", events);
		if (subject != null)
			cb.subject(subject);
		if (sid != null)
			cb.claim("sid", sid);
		return sign(key, JWSAlgorithm.RS256, new JOSEObjectType("logout+jwt"), cb.build());
	}

	private static String sign(RSAKey key, JWSAlgorithm alg, JOSEObjectType typ, JWTClaimsSet claims)
			throws JOSEException {
		var hb = new JWSHeader.Builder(alg).keyID(key.getKeyID());
		if (typ != null)
			hb.type(typ);
		var jwt = new SignedJWT(hb.build(), claims);
		jwt.sign(new RSASSASigner(key));
		return jwt.serialize();
	}

	/** Extracts a single query parameter value from a URL string (URL-decoded). */
	static String queryParam(String url, String name) {
		var q = URI.create(url).getQuery();
		if (q == null)
			return null;
		for (var pair : q.split("&")) {
			var i = pair.indexOf('=');
			var k = i < 0 ? pair : pair.substring(0, i);
			if (k.equals(name))
				return i < 0 ? "" : URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
		}
		return null;
	}

	/** Parses the cookie value out of a {@code Set-Cookie} header (the bit between {@code =} and the first {@code ;}). */
	static String cookieValue(String setCookieHeader) {
		if (setCookieHeader == null)
			return null;
		var eq = setCookieHeader.indexOf('=');
		var semi = setCookieHeader.indexOf(';');
		return setCookieHeader.substring(eq + 1, semi < 0 ? setCookieHeader.length() : semi);
	}

	/**
	 * In-JVM stub IdP exposing a {@code /token} and {@code /userinfo} endpoint over loopback.
	 *
	 * <p>
	 * The {@code /token} response is driven by mutable fields the test sets between flow steps (the ID
	 * token must echo the nonce the relying party generated, which is only known after {@code startLogin}).
	 */
	static final class StubIdp implements AutoCloseable {

		private final HttpServer server;
		final String issuer = "https://stub-idp.example.com";

		/** The ID token returned by the next {@code /token} call. */
		volatile String idToken;
		/** The refresh token returned by an authorization-code {@code /token} call. */
		volatile String refreshToken = "rt-1";
		/** The refresh token returned by a refresh-grant {@code /token} call (rotation). */
		volatile String rotatedRefreshToken = "rt-2";
		/** When true, the {@code /token} endpoint returns an error (simulates revoked / reused refresh token). */
		volatile boolean failToken;
		/** When true, the {@code /userinfo} endpoint returns 401 (simulates a non-success UserInfo response). */
		volatile boolean userInfoFail;
		/** Extra claims served by {@code /userinfo}. */
		volatile Map<String,Object> userInfo = new LinkedHashMap<>();

		StubIdp() throws IOException {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			server.createContext("/token", this::handleToken);
			server.createContext("/userinfo", this::handleUserInfo);
			server.start();
		}

		private void handleToken(HttpExchange ex) throws IOException {
			var body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			if (failToken) {
				writeJson(ex, 400, "{\"error\":\"invalid_grant\"}");
				return;
			}
			var isRefresh = body.contains("grant_type=refresh_token");
			var rt = isRefresh ? rotatedRefreshToken : refreshToken;
			var json = new StringBuilder("{")
				.append("\"access_token\":\"at-").append(isRefresh ? "refreshed" : "initial").append("\",")
				.append("\"token_type\":\"Bearer\",")
				.append("\"expires_in\":3600,");
			if (rt != null)
				json.append("\"refresh_token\":\"").append(rt).append("\",");
			if (idToken != null)
				json.append("\"id_token\":\"").append(idToken).append("\",");
			json.append("\"scope\":\"openid profile\"}");
			writeJson(ex, 200, json.toString());
		}

		private void handleUserInfo(HttpExchange ex) throws IOException {
			if (userInfoFail) {
				writeJson(ex, 401, "{\"error\":\"invalid_token\"}");
				return;
			}
			var sb = new StringBuilder("{\"sub\":\"alice\"");
			userInfo.forEach((k, v) -> sb.append(",\"").append(k).append("\":\"").append(v).append("\""));
			sb.append("}");
			writeJson(ex, 200, sb.toString());
		}

		private static void writeJson(HttpExchange ex, int status, String body) throws IOException {
			var bytes = body.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(status, bytes.length);
			try (var os = ex.getResponseBody()) {
				os.write(bytes);
			}
		}

		int port() {
			return server.getAddress().getPort();
		}

		URI baseUri() {
			return URI.create("http://127.0.0.1:" + port());
		}

		URI tokenEndpoint() {
			return URI.create(baseUri() + "/token");
		}

		URI userinfoEndpoint() {
			return URI.create(baseUri() + "/userinfo");
		}

		/** Builds an {@link OidcMetadata} record pointing the token/userinfo endpoints at this stub. */
		OidcMetadata metadata(URI authorizationEndpoint, URI endSessionEndpoint) {
			return new OidcMetadata(
				URI.create(issuer),
				tokenEndpoint(),
				authorizationEndpoint,
				null,
				URI.create(baseUri() + "/jwks"),
				userinfoEndpoint(),
				endSessionEndpoint,
				Set.of("openid", "profile", "email"),
				Map.of());
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}
}
