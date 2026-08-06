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
package org.apache.juneau.examples.mcp.secured;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.marshaller.Json;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.*;

/**
 * A minimal, self-contained, in-process stand-in for a real OAuth 2.1 authorization server (AS), so the
 * {@code secured} MCP example (and its end-to-end test) run entirely offline &mdash; no external IdP, no
 * network access, no pre-shared secrets checked into source control.
 *
 * <p><b>DEMO ONLY &mdash; NOT SUITABLE FOR PRODUCTION USE.</b></p>
 *
 * <p>
 * This is deliberately NOT a general-purpose or spec-complete authorization server. It exists purely to make
 * {@link SecuredExampleMcpServer}'s {@code JwtTokenValidator}-backed resource-server gate and
 * {@link SecuredExampleClient}'s {@code McpTokenProvider}-backed client-credentials flow both have something
 * real to talk to. Concretely, on {@link #start()} it:
 *
 * <ol>
 * 	<li>Generates a fresh RSA-2048 signing key (a new key every run &mdash; nothing is persisted, so restarting
 * 		the example invalidates any previously-issued token, which is exactly the offline-demo property we want).
 * 	<li>Publishes only the <b>public</b> half of that key as a {@link JWKSource}, which
 * 		{@link SecuredExampleMcpServer} feeds directly to {@code JwtTokenValidator.jwkSource(...)} &mdash; no JWKS
 * 		HTTP endpoint is needed because the server and "AS" share this JVM.
 * 	<li>Starts a tiny {@link HttpServer} exposing two routes:
 * 		<ul>
 * 			<li>{@code POST /token} &mdash; the RFC 6749 &sect;4.4 client-credentials grant against one fixed,
 * 				randomly-generated demo client id/secret pair (see {@link #clientId()} / {@link #clientSecret()}).
 * 				This is a REAL HTTP round trip: the client-side {@code OAuthClientCredentialsFlow} used by
 * 				{@code McpTokenProvider} talks to it exactly as it would talk to a production IdP's token
 * 				endpoint, just on {@code localhost}.
 * 			<li>{@code GET /.well-known/oauth-authorization-server} &mdash; a minimal RFC 8414 Authorization
 * 				Server Metadata document ({@code issuer} + {@code token_endpoint} only), so a real client can
 * 				perform genuine discovery against {@link #issuerUri()} instead of being handed the token
 * 				endpoint out of band. See {@link SecuredExampleClient} for the client half of that handshake.
 * 		</ul>
 * </ol>
 *
 * <p>
 * <b>What this class intentionally does NOT implement</b> (called out here and in the README so nobody mistakes
 * this demo for a template for a real deployment):
 * <ul>
 * 	<li>No real scope-authorization decision &mdash; {@link #handleToken} only checks the requested scope
 * 		against a fixed allowlist ({@code mcp.read}/{@code mcp.write}); it never asks a resource owner or policy
 * 		engine whether the client is ALLOWED that scope, which is what a real AS's consent/policy step does.
 * 	<li>No refresh tokens, no consent screen, no user authentication of any kind &mdash; only the
 * 		machine-to-machine client-credentials grant, which is all an MCP server-to-server OAuth demo needs.
 * 	<li>No persistence, replay protection beyond the JWT {@code exp}/{@code nbf} window, or key rotation. The
 * 		RFC 8414 document above is likewise minimal: no {@code jwks_uri}, no {@code scopes_supported}, no
 * 		dynamic client registration &mdash; this AS's public key is handed directly to the validator in-process
 * 		(see {@link #jwkSource()}) rather than published for a real JWKS fetch.
 * </ul>
 *
 * <p>
 * <b>TLS caveat:</b> this class and {@link SecuredExampleServer} talk plain {@code http} only because both
 * ends are loopback-only ({@code 127.0.0.1}/{@code localhost}). OAuth 2.1 / RFC 9728 require {@code https} for
 * any non-loopback resource identifier or token endpoint; a bearer token sent over plaintext to a non-loopback
 * host is exposed to anyone on the network path.
 *
 * <p>
 * A real deployment would point {@code JwtTokenValidator} at an actual IdP's JWKS URL and issuer, and would
 * never generate or hand out client secrets like {@link #start()} does here.
 *
 * @since 10.0.0
 */
public final class OfflineAuthorizationServer implements AutoCloseable {

	/** The scope minted onto a token when the client's request omits one. */
	public static final String DEFAULT_SCOPE = "mcp.read";

	/**
	 * The fixed allowlist of scopes this offline AS will grant (H2): a client requesting anything outside this
	 * set is rejected with RFC 6749 &sect;5.2 {@code invalid_scope}, rather than being handed back whatever it
	 * asked for.
	 */
	public static final Set<String> GRANTABLE_SCOPES = Set.of("mcp.read", "mcp.write");

	/** How long a minted access token is valid for. */
	private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

	/** RFC 8414 Authorization Server Metadata well-known path. */
	private static final String WELL_KNOWN_AUTHORIZATION_SERVER = "/.well-known/oauth-authorization-server";

	private final RSAKey signingKey;
	private final String clientId;
	private final String clientSecret;
	private final HttpServer httpServer;
	private final URI issuer;
	private final AtomicInteger tokenRequestCount = new AtomicInteger();

	private OfflineAuthorizationServer(RSAKey signingKey, String clientId, String clientSecret, HttpServer httpServer, URI issuer) {
		this.signingKey = signingKey;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.httpServer = httpServer;
		this.issuer = issuer;
	}

	/**
	 * Generates a fresh signing key and demo client credentials, and starts the {@code /token} and RFC 8414
	 * discovery endpoints on an OS-assigned loopback-only port.
	 *
	 * @return A running instance. Close it (or call {@link #close()}) to stop the HTTP endpoint.
	 * @throws Exception If key generation or the HTTP endpoint fails to start.
	 */
	public static OfflineAuthorizationServer start() throws Exception {
		var signingKey = new RSAKeyGenerator(2048).keyID("demo-key-1").algorithm(JWSAlgorithm.RS256).generate();
		var clientId = "demo-client";
		var clientSecret = randomSecret();

		var httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		// H4/M10: the issuer is THIS instance's own loopback base URL, known as soon as the (possibly
		// OS-assigned) listen socket is bound - a constructor-supplied instance field, not a shared constant,
		// so two independently-started instances naturally have two different issuers (see
		// SecuredExampleMcpEndToEnd_Test's wrong-issuer test).
		var issuer = URI.create("http://127.0.0.1:" + httpServer.getAddress().getPort());
		var self = new OfflineAuthorizationServer(signingKey, clientId, clientSecret, httpServer, issuer);
		httpServer.createContext("/token", self::handleToken);
		httpServer.createContext(WELL_KNOWN_AUTHORIZATION_SERVER, self::handleAuthorizationServerMetadata);
		httpServer.start();
		return self;
	}

	/** Generates a random, never-checked-in demo client secret (32 bytes, URL-safe base64). */
	private static String randomSecret() {
		var bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/**
	 * Returns a {@link JWKSource} publishing only the public half of this server's signing key, ready to hand
	 * straight to {@code JwtTokenValidator.jwkSource(...)}.
	 *
	 * @return The public-key JWK source. Never <jk>null</jk>.
	 */
	public JWKSource<SecurityContext> jwkSource() {
		return new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()));
	}

	/**
	 * Returns this authorization server's issuer identity (its own loopback base URL) &mdash; used as the JWT
	 * {@code iss} claim, the PRM document's {@code authorization_servers} entry, and the RFC 8414 discovery
	 * document's {@code issuer} field.
	 *
	 * @return The issuer URI. Never <jk>null</jk>.
	 */
	public URI issuerUri() {
		return issuer;
	}

	/**
	 * Returns the loopback URL of the {@code /token} client-credentials endpoint.
	 *
	 * @return The token endpoint URL. Never <jk>null</jk>.
	 */
	public URI tokenEndpoint() {
		return URI.create(issuer + "/token");
	}

	/**
	 * Returns the fixed demo client id a caller must present (via HTTP Basic) to acquire a token.
	 *
	 * @return The client id. Never <jk>null</jk>.
	 */
	public String clientId() {
		return clientId;
	}

	/**
	 * Returns the randomly-generated demo client secret a caller must present (via HTTP Basic) to acquire a
	 * token. Generated fresh on every {@link #start()}; never persisted anywhere &mdash; {@link SecuredExampleServer#main}
	 * prints it to the console purely so a reader running the demo by hand has something to copy/paste.
	 *
	 * @return The client secret. Never <jk>null</jk>.
	 */
	public String clientSecret() {
		return clientSecret;
	}

	/**
	 * Returns how many HTTP requests have hit the {@code /token} endpoint so far (M7): a test seam proving a
	 * caching {@code McpTokenProvider} genuinely reuses an acquired token across multiple dispatches instead of
	 * silently re-requesting one per call.
	 *
	 * @return The number of {@code /token} requests handled since {@link #start()}.
	 */
	public int tokenRequestCount() {
		return tokenRequestCount.get();
	}

	@Override
	public void close() {
		httpServer.stop(0);
	}

	/**
	 * Implements RFC 6749 &sect;4.4 (client-credentials grant) against the fixed demo client id/secret,
	 * minting a signed JWT whose {@code aud} claim is exactly the RFC 8707 {@code resource} indicator the
	 * caller supplied &mdash; this is what lets {@code JwtTokenValidator} enforce the audience check on the
	 * receiving end.
	 */
	private void handleToken(HttpExchange exchange) throws IOException {
		// DEMO ONLY: see class javadoc - not a production AS.
		tokenRequestCount.incrementAndGet();
		try {
			if (! "POST".equals(exchange.getRequestMethod())) {
				exchange.getResponseHeaders().add("Allow", "POST");
				sendJson(exchange, 405, error("invalid_request", "must POST to /token"));
				return;
			}
			if (! authenticateClient(exchange)) {
				exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"offline-authorization-server\"");
				sendJson(exchange, 401, error("invalid_client", "unknown client id or secret"));
				return;
			}
			var form = parseForm(exchange.getRequestBody());
			if (! "client_credentials".equals(form.get("grant_type"))) {
				sendJson(exchange, 400, error("unsupported_grant_type", "only client_credentials is supported"));
				return;
			}
			var resource = form.get("resource");
			if (resource == null || resource.isBlank()) {
				sendJson(exchange, 400, error("invalid_target", "a resource indicator (RFC 8707) is required"));
				return;
			}
			var scope = form.getOrDefault("scope", DEFAULT_SCOPE);
			if (! isGrantable(scope)) {
				sendJson(exchange, 400, error("invalid_scope", "requested scope must be a subset of " + GRANTABLE_SCOPES));
				return;
			}
			var token = mintAccessToken(resource, scope, Instant.now(), TOKEN_TTL);
			exchange.getResponseHeaders().add("Cache-Control", "no-store");
			exchange.getResponseHeaders().add("Pragma", "no-cache");
			sendJson(exchange, 200, Json.of(Map.<String,Object>of(
				"access_token", token,
				"token_type", "Bearer",
				"expires_in", TOKEN_TTL.toSeconds(),
				"scope", scope)));
		} catch (RuntimeException | JOSEException e) {
			sendJson(exchange, 500, error("server_error", e.getMessage() == null ? "internal error" : e.getMessage()));
		}
	}

	/** Serves a minimal RFC 8414 Authorization Server Metadata document (M10): {@code issuer} + {@code token_endpoint} only. */
	private void handleAuthorizationServerMetadata(HttpExchange exchange) throws IOException {
		if (! "GET".equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().add("Allow", "GET");
			exchange.sendResponseHeaders(405, -1);
			return;
		}
		sendJson(exchange, 200, Json.of(Map.of(
			"issuer", issuer.toString(),
			"token_endpoint", tokenEndpoint().toString())));
	}

	/** Returns whether every space-delimited token in {@code scope} is in the {@link #GRANTABLE_SCOPES} allowlist (H2). */
	private static boolean isGrantable(String scope) {
		for (var s : scope.split("\\s+"))
			if (! s.isBlank() && ! GRANTABLE_SCOPES.contains(s))
				return false;
		return true;
	}

	/** Validates the RFC 6749 HTTP Basic client-authentication header against the fixed demo credentials. */
	private boolean authenticateClient(HttpExchange exchange) {
		// DEMO ONLY: see class javadoc - not a production AS.
		var header = exchange.getRequestHeaders().getFirst("Authorization");
		if (header == null || ! header.startsWith("Basic "))
			return false;
		byte[] decodedBytes;
		try {
			decodedBytes = Base64.getDecoder().decode(header.substring("Basic ".length()));
		} catch (IllegalArgumentException e) { // L1: malformed base64 -> a clean 401, not a 500
			return false;
		}
		var decoded = new String(decodedBytes, StandardCharsets.UTF_8);
		var sep = decoded.indexOf(':');
		if (sep < 0)
			return false;
		var presentedId = URLDecoder.decode(decoded.substring(0, sep), StandardCharsets.UTF_8);
		var presentedSecret = URLDecoder.decode(decoded.substring(sep + 1), StandardCharsets.UTF_8);
		// M6: constant-time comparison (both operands, combined with '&' rather than '&&') so neither the
		// id nor the secret check can leak timing information about how many leading bytes matched.
		var idMatches = MessageDigest.isEqual(clientId.getBytes(StandardCharsets.UTF_8), presentedId.getBytes(StandardCharsets.UTF_8));
		var secretMatches = MessageDigest.isEqual(clientSecret.getBytes(StandardCharsets.UTF_8), presentedSecret.getBytes(StandardCharsets.UTF_8));
		return idMatches & secretMatches;
	}

	/**
	 * Signs a fresh RS256 access token carrying the mandatory claims {@code JwtTokenValidator} requires.
	 *
	 * <p>
	 * Package-private test seam (H4): exposing {@code issuedAt}/{@code ttl} lets
	 * {@link SecuredExampleMcpEndToEnd_Test} mint an already-expired token directly, without needing a real
	 * clock to actually elapse.
	 *
	 * @param audience The RFC 8707 {@code resource} indicator to mint the token's {@code aud} claim for.
	 * @param scope The (space-delimited) {@code scope} claim.
	 * @param issuedAt The token's {@code iat}/{@code nbf} instant.
	 * @param ttl How long after {@code issuedAt} the token expires.
	 * @return The signed, serialized JWT.
	 * @throws JOSEException If signing fails.
	 */
	String mintAccessToken(String audience, String scope, Instant issuedAt, Duration ttl) throws JOSEException {
		var claims = new JWTClaimsSet.Builder()
			// M4: a client-credentials grant has no resource owner - the subject IS the client, not a
			// placeholder end user.
			.subject(clientId)
			.issuer(issuer.toString())
			.audience(audience)
			.claim("scope", scope)
			.issueTime(Date.from(issuedAt))
			.notBeforeTime(Date.from(issuedAt))
			.expirationTime(Date.from(issuedAt.plus(ttl)))
			.build();
		var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build();
		var jwt = new SignedJWT(header, claims);
		jwt.sign(new RSASSASigner(signingKey));
		return jwt.serialize();
	}

	/** Parses an {@code application/x-www-form-urlencoded} request body into a key/value map. */
	private static Map<String,String> parseForm(InputStream body) throws IOException {
		var raw = new String(body.readAllBytes(), StandardCharsets.UTF_8);
		var out = new LinkedHashMap<String,String>();
		for (var pair : raw.split("&")) {
			if (pair.isEmpty())
				continue;
			var eq = pair.indexOf('=');
			var key = eq < 0 ? pair : pair.substring(0, eq);
			var value = eq < 0 ? "" : pair.substring(eq + 1);
			out.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
		}
		return out;
	}

	/**
	 * Builds an RFC 6749 &sect;5.2 JSON error body.
	 *
	 * <p>
	 * M2/M3: built as a {@link Map} and serialized via {@link Json#of(Object)} (the same marshaller
	 * {@link SecuredExampleClient} already uses) instead of hand-concatenating strings, so a client-controlled
	 * {@code error_description} containing a quote or backslash can no longer reshape the JSON structure or
	 * produce invalid JSON.
	 */
	private static String error(String code, String description) {
		return Json.of(Map.of("error", code, "error_description", description));
	}

	private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
		var bytes = json.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (var os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}
}
