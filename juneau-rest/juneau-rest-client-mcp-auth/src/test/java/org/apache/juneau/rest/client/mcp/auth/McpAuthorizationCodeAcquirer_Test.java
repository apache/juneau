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
package org.apache.juneau.rest.client.mcp.auth;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.auth.oauth.flow.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.pkce.*;
import com.sun.net.httpserver.*;

/**
 * Tests for {@link McpAuthorizationCodeAcquirer}: the SEP-2468 {@code iss} / CSRF validation logic and a fully-stubbed
 * interactive authorization-code + PKCE acquisition (loopback receiver + simulated IdP + token exchange).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // HttpServer held as test fixture; lifecycle managed by @AfterEach
})
class McpAuthorizationCodeAcquirer_Test extends TestBase {

	private static final URI ISSUER = URI.create("https://as.example.com");
	private static final URI RESOURCE = URI.create("https://mcp.example.com/api");

	// -----------------------------------------------------------------------------------------------------------------
	// A: validateAuthorizationResponse (pure SEP-2468 / CSRF checks)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_happyReturnsCode() {
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st1&iss=" + enc(ISSUER.toString()));
		assertEquals("the-code", McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
	}

	@Test void a02_issAbsentIsAccepted() {
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st1");
		assertEquals("the-code", McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
	}

	@Test void b01_stateMismatchRejected() {
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=WRONG");
		assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
	}

	@Test void b02_issMismatchRejected() {
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st1&iss=" + enc("https://evil.example.com"));
		var e = assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
		assertTrue(e.getMessage().contains("SEP-2468"), e::getMessage);
	}

	@Test void b03_errorResponseRejected() {
		var cb = URI.create("http://127.0.0.1:1/callback?error=access_denied&state=st1");
		assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
	}

	@Test void b05_issMismatchGatesErrorResponse() {
		// An error response with a mismatched iss must be reported as an issuer mismatch (SEP-2468), NOT a denial
		// — the client MUST NOT act on the error/error_description of a response whose issuer doesn't match.
		var cb = URI.create("http://127.0.0.1:1/callback?error=access_denied&state=st1&iss=" + enc("https://evil.example.com"));
		var e = assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
		assertTrue(e.getMessage().contains("SEP-2468"), e::getMessage);
		assertFalse(e.getMessage().contains("denied"), e::getMessage);
	}

	@Test void b06_issAbsentRejectedWhenRequired() {
		// When the AS advertises authorization_response_iss_parameter_supported, a missing iss must be rejected.
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st1");
		var e = assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER, true));
		assertTrue(e.getMessage().contains("iss"), e::getMessage);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: full interactive acquisition (stubbed IdP)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_interactiveAcquireHappy() throws IOException {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		var tokenReqBody = new AtomicReference<String>();
		server.createContext("/token", ex -> {
			try (var is = ex.getRequestBody()) { tokenReqBody.set(new String(is.readAllBytes(), UTF_8)); }
			var body = "{\"access_token\":\"acq-at\",\"token_type\":\"Bearer\",\"expires_in\":3600}".getBytes(UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) { os.write(body); }
		});
		server.start();
		try {
			var tokenEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
			var authorizeEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/authorize");
			var seen = new AtomicReference<AuthorizationRequest>();

			var acquirer = McpAuthorizationCodeAcquirer.create()
				.authorizationEndpoint(authorizeEndpoint)
				.tokenEndpoint(tokenEndpoint)
				.clientId("cli-app")
				.resource(RESOURCE)
				.expectedIssuer(ISSUER)
				.offlineAccess(true)
				.browserLauncher(authUrl -> simulateIdp(authUrl, seen))
				.build();

			var token = acquirer.acquire(Duration.ofSeconds(5));
			assertEquals("acq-at", token.accessToken());

			// Verify the authorization request was well-formed: PKCE S256, offline_access, and RFC 8707 resource.
			var ar = seen.get();
			assertNotNull(ar, "browser launcher was not invoked");
			assertEquals(CodeChallengeMethod.S256, ar.getCodeChallengeMethod());
			assertNotNull(ar.getCodeChallenge());
			assertTrue(ar.getScope().contains("offline_access"), () -> "offline_access missing: " + ar.getScope());
			assertTrue(ar.getResources().contains(RESOURCE), () -> "resource indicator missing: " + ar.getResources());

			// The code-exchange token request must also carry the RFC 8707 resource indicator.
			var body = tokenReqBody.get();
			assertNotNull(body, "token endpoint was not called");
			assertTrue(body.contains("resource="), () -> "resource indicator missing from token request: " + body);
			assertTrue(body.contains("mcp.example.com"), () -> "resource value missing from token request: " + body);
		} finally {
			server.stop(0);
		}
	}

	@Test void c02_acquireTimesOut() {
		var acquirer = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("cli-app")
			.resource(RESOURCE)
			.expectedIssuer(ISSUER)
			.browserLauncher(authUrl -> { /* never completes the callback */ })
			.build();
		var timeout = Duration.ofMillis(100);
		assertThrows(McpAuthException.class, () -> acquirer.acquire(timeout));
	}

	/** Calling handleCallback twice with the same state must reject the second (state + PKCE verifier single-use). */
	@Test void b04_stateReplayRejected() throws IOException {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/token", ex -> {
			var body = "{\"access_token\":\"acq-at\",\"token_type\":\"Bearer\",\"expires_in\":3600}".getBytes(UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) { os.write(body); }
		});
		server.start();
		try {
			var tokenEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
			var authorizeEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/authorize");
			var store = EphemeralStore.create();

			var acquirer = McpAuthorizationCodeAcquirer.create()
				.authorizationEndpoint(authorizeEndpoint)
				.tokenEndpoint(tokenEndpoint)
				.clientId("cli-app")
				.resource(RESOURCE)
				.expectedIssuer(ISSUER)
				.store(store)
				.build();

			var flow = OAuthAuthorizationCodeFlow.create()
				.authorizationEndpoint(authorizeEndpoint)
				.tokenEndpoint(tokenEndpoint)
				.clientId("cli-app")
				.redirectUri(URI.create("http://127.0.0.1:1/callback"))
				.resource(RESOURCE)
				.build();

			var verifier = new CodeVerifier();
			store.store("st-1", verifier.getValue(), ISSUER);
			var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st-1&iss=" + enc(ISSUER.toString()));

			// First consumption succeeds; the state + PKCE verifier are then burned.
			assertEquals("acq-at", acquirer.handleCallback(flow, cb).accessToken());
			// Replay of the same state (and thus the same verifier) must be rejected.
			assertThrows(McpAuthException.class, () -> acquirer.handleCallback(flow, cb));
		} finally {
			server.stop(0);
		}
	}

	/** Simulates the IdP: parses the authorization request, then redirects to the loopback callback with a code. */
	private static void simulateIdp(URI authUrl, AtomicReference<AuthorizationRequest> seen) {
		try {
			var ar = AuthorizationRequest.parse(authUrl);
			seen.set(ar);
			var state = ar.getState().getValue();
			var cb = URI.create(ar.getRedirectionURI() + "?code=the-code&state=" + enc(state) + "&iss=" + enc(ISSUER.toString()));
			var c = (HttpURLConnection) cb.toURL().openConnection();
			c.setRequestMethod("GET");
			c.getResponseCode();
			c.getInputStream().readAllBytes();
			c.disconnect();
		} catch (ParseException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder validation
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_buildRequiresResource() {
		// Resource() (RFC 8707) is mandatory on the MCP-facing entry point.
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("cli-app")
			.expectedIssuer(ISSUER);
		var e = assertThrows(IllegalStateException.class, b::build);
		assertTrue(e.getMessage().contains("resource"), e::getMessage);
	}

	@Test void d02_buildRequiresIssuerByDefault() {
		// ExpectedIssuer() is required unless explicitly opted out.
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("cli-app")
			.resource(RESOURCE);
		var e = assertThrows(IllegalStateException.class, b::build);
		assertTrue(e.getMessage().contains("expectedIssuer"), e::getMessage);
	}

	@Test void d03_skipIssuerValidationAllowsBuild() {
		// The explicit, greppable opt-out lets build() succeed without an issuer.
		var acquirer = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("cli-app")
			.resource(RESOURCE)
			.skipIssuerValidation(true)
			.build();
		assertNotNull(acquirer);
	}

	@Test void d04_redirectPathMustStartWithSlash() {
		// A redirect path that doesn't start with '/' is rejected at configuration time.
		var b = McpAuthorizationCodeAcquirer.create();
		assertThrows(IllegalArgumentException.class, () -> b.redirectPath("callback"));
	}

	@Test void d05_redirectPortDefaultsToEphemeral() {
		// The default (unset) redirect port is 0 (ephemeral / port-agnostic).
		var acquirer = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("cli-app").resource(RESOURCE).skipIssuerValidation(true).build();
		assertEquals(0, acquirer.redirectPort());
	}

	@Test void d06_redirectPortOutOfRangeRejected() {
		// A negative or >65535 fixed port is rejected at configuration time.
		var b = McpAuthorizationCodeAcquirer.create();
		assertThrows(IllegalArgumentException.class, () -> b.redirectPort(-1));
		assertThrows(IllegalArgumentException.class, () -> b.redirectPort(70000));
	}

	// End-to-end — with a fixed redirectPort, the receiver binds exactly that port and the authorization request's
	// redirect_uri carries it, so the whole interactive flow completes on the caller-chosen port (bind-first).
	@Test void c03_interactiveAcquireHonorsRedirectPort() throws IOException {
		var fixedPort = freePort();
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/token", ex -> {
			var body = "{\"access_token\":\"acq-at\",\"token_type\":\"Bearer\",\"expires_in\":3600}".getBytes(UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) { os.write(body); }
		});
		server.start();
		try {
			var tokenEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
			var authorizeEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/authorize");
			var seen = new AtomicReference<AuthorizationRequest>();

			var acquirer = McpAuthorizationCodeAcquirer.create()
				.authorizationEndpoint(authorizeEndpoint)
				.tokenEndpoint(tokenEndpoint)
				.clientId("cli-app")
				.resource(RESOURCE)
				.expectedIssuer(ISSUER)
				.redirectPort(fixedPort)
				.browserLauncher(authUrl -> simulateIdp(authUrl, seen))
				.build();

			var token = acquirer.acquire(Duration.ofSeconds(5));
			assertEquals("acq-at", token.accessToken());
			// The receiver bound the fixed port, so the redirect_uri the acquirer generated carries exactly it.
			assertEquals(fixedPort, seen.get().getRedirectionURI().getPort());
		} finally {
			server.stop(0);
		}
	}

	private static int freePort() throws IOException {
		try (var s = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			return s.getLocalPort();
		}
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, UTF_8);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder options, callback edge cases, and failure modes
	// -----------------------------------------------------------------------------------------------------------------

	private static McpAuthorizationCodeAcquirer.Builder minimal() {
		return McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token"))
			.clientId("cli-app")
			.resource(RESOURCE)
			.expectedIssuer(ISSUER);
	}

	private static OAuthAuthorizationCodeFlow flowFor(URI tokenEndpoint) {
		return OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(tokenEndpoint)
			.clientId("cli-app")
			.redirectUri(URI.create("http://127.0.0.1:1/callback"))
			.resource(RESOURCE)
			.build();
	}

	@Test void e01_interactiveAcquireWithSecretScopesTimeoutAndConfigurator() throws IOException {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		var tokenReqBody = new AtomicReference<String>();
		var tokenReqAuth = new AtomicReference<String>();
		server.createContext("/token", ex -> {
			tokenReqAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
			try (var is = ex.getRequestBody()) { tokenReqBody.set(new String(is.readAllBytes(), UTF_8)); }
			var body = "{\"access_token\":\"acq-at\",\"token_type\":\"Bearer\",\"expires_in\":3600}".getBytes(UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) { os.write(body); }
		});
		server.start();
		try {
			var port = server.getAddress().getPort();
			var seen = new AtomicReference<AuthorizationRequest>();
			var configured = new AtomicInteger();

			var acquirer = McpAuthorizationCodeAcquirer.create()
				.authorizationEndpoint(URI.create("http://127.0.0.1:" + port + "/authorize"))
				.tokenEndpoint(URI.create("http://127.0.0.1:" + port + "/token"))
				.clientId("cli-app")
				.clientSecret("s3cret")
				.scope("mcp:read", "mcp:write")
				.resource(RESOURCE)
				.expectedIssuer(ISSUER)
				.requireIssuerResponseParameter(true)
				.redirectPath("/cb")
				.httpTimeout(Duration.ofSeconds(5))
				.httpRequestConfigurator(r -> configured.incrementAndGet())
				.store(EphemeralStore.create())
				.browserLauncher(authUrl -> simulateIdp(authUrl, seen))
				.build();

			assertEquals("acq-at", acquirer.acquire(Duration.ofSeconds(5)).accessToken());

			var ar = seen.get();
			assertTrue(ar.getScope().contains("mcp:read"), () -> "scope missing: " + ar.getScope());
			assertTrue(ar.getScope().contains("mcp:write"), () -> "scope missing: " + ar.getScope());
			assertEquals("/cb", ar.getRedirectionURI().getPath());
			assertEquals(1, configured.get(), "httpRequestConfigurator must be applied to the token exchange");
			assertNotNull(tokenReqAuth.get(), "confidential client must authenticate the token exchange");
			assertTrue(tokenReqAuth.get().startsWith("Basic "), tokenReqAuth::get);
		} finally {
			server.stop(0);
		}
	}

	@Test void e02_requireIssuerParameterRejectsCallbackWithoutIss() {
		// With requireIssuerResponseParameter(true) the end-to-end flow must reject a callback that omits iss.
		var acquirer = minimal()
			.requireIssuerResponseParameter(true)
			.browserLauncher(authUrl -> {
				try {
					var ar = AuthorizationRequest.parse(authUrl);
					var cb = URI.create(ar.getRedirectionURI() + "?code=the-code&state=" + enc(ar.getState().getValue()));
					var c = (HttpURLConnection) cb.toURL().openConnection();
					c.getResponseCode();
					c.disconnect();
				} catch (ParseException | IOException e) {
					throw new RuntimeException(e);
				}
			})
			.build();
		var timeout = Duration.ofSeconds(5);
		var e = assertThrows(McpAuthException.class, () -> acquirer.acquire(timeout));
		assertTrue(e.getMessage().contains("missing the required iss"), e::getMessage);
	}

	@Test void e03_acquireFailsWhenFixedPortInUse() throws IOException {
		try (var occupied = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			var acquirer = minimal().redirectPort(occupied.getLocalPort()).build();
			var timeout = Duration.ofMillis(100);
			var e = assertThrows(McpAuthException.class, () -> acquirer.acquire(timeout));
			assertTrue(e.getMessage().contains("loopback redirect listener"), e::getMessage);
		}
	}

	@Test void e04_defaultBrowserLauncherPrintsAuthorizationUrl() {
		var captured = new ByteArrayOutputStream();
		var original = System.out;
		try (var ps = new PrintStream(captured, true, UTF_8)) {
			System.setOut(ps);
			var acquirer = McpAuthorizationCodeAcquirer.create()
				.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
				.tokenEndpoint(URI.create("https://idp.example.com/token"))
				.clientId("cli-app").resource(RESOURCE).expectedIssuer(ISSUER).build();
			var timeout = Duration.ofMillis(100);
			assertThrows(McpAuthException.class, () -> acquirer.acquire(timeout));
		} finally {
			System.setOut(original);
		}
		var out = captured.toString(UTF_8);
		assertTrue(out.contains("Open the following URL in a browser to authorize"), out);
		assertTrue(out.contains("https://idp.example.com/authorize"), out);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"http://127.0.0.1:1/callback?code=the-code                    | missing the state",
		"http://127.0.0.1:1/callback?code=the-code&state=never-issued | unknown, expired, or replayed",
		"http://127.0.0.1:1/callback                                  | could not be parsed"  // No parameters at all.
	})
	void e05_handleCallbackRejected(String callback, String expectedMessage) {
		var acquirer = minimal().build();
		var flow = flowFor(URI.create("https://idp.example.com/token"));
		var cb = URI.create(callback);
		var e = assertThrows(McpAuthException.class, () -> acquirer.handleCallback(flow, cb));
		assertTrue(e.getMessage().contains(expectedMessage), e::getMessage);
	}

	@Test void e08_validateMissingStateParamIsCsrfMismatch() {
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code");
		var e = assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
		assertTrue(e.getMessage().contains("state mismatch"), e::getMessage);
	}

	@Test void e15_validateErrorWithIllegalCodeRejectedAsUnparseable() {
		var cb = URI.create("http://127.0.0.1:1/callback?error=%22bad&state=st1&iss=" + enc(ISSUER.toString()));
		var e = assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
		// Nimbus nulls an illegal error code and then rejects the response ("Missing error code"), so a null code never reaches the denial path.
		assertTrue(e.getMessage().contains("could not be parsed"), e::getMessage);
	}

	@Test void e09_validateErrorResponseReportsDenialCode() {
		var cb = URI.create("http://127.0.0.1:1/callback?error=access_denied&state=st1&iss=" + enc(ISSUER.toString()));
		var e = assertThrows(McpAuthException.class,
			() -> McpAuthorizationCodeAcquirer.validateAuthorizationResponse(cb, "st1", ISSUER));
		assertTrue(e.getMessage().contains("denied: access_denied"), e::getMessage);
	}

	@Test void e10_issuerRecordedAsNullRejectsAnyIss() {
		// A pending entry recorded with no issuer cannot vouch for any iss, so a callback carrying iss is a mismatch.
		var store = EphemeralStore.create();
		var acquirer = minimal().store(store).build();
		var flow = flowFor(URI.create("https://idp.example.com/token"));
		store.store("st-1", new CodeVerifier().getValue(), null);
		var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st-1&iss=" + enc(ISSUER.toString()));
		var e = assertThrows(McpAuthException.class, () -> acquirer.handleCallback(flow, cb));
		assertTrue(e.getMessage().contains("SEP-2468"), e::getMessage);
	}

	@Test void e11_skipIssuerValidationAcceptsAnyIss() throws IOException {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/token", ex -> {
			var body = "{\"access_token\":\"acq-at\",\"token_type\":\"Bearer\",\"expires_in\":3600}".getBytes(UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) { os.write(body); }
		});
		server.start();
		try {
			var tokenEndpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
			var store = EphemeralStore.create();
			var acquirer = McpAuthorizationCodeAcquirer.create()
				.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
				.tokenEndpoint(tokenEndpoint)
				.clientId("cli-app").resource(RESOURCE)
				.skipIssuerValidation(true).store(store).build();
			store.store("st-1", new CodeVerifier().getValue(), null);
			var cb = URI.create("http://127.0.0.1:1/callback?code=the-code&state=st-1&iss=" + enc("https://anything.example.com"));
			assertEquals("acq-at", acquirer.handleCallback(flowFor(tokenEndpoint), cb).accessToken());
		} finally {
			server.stop(0);
		}
	}

	@Test void e12_builderRequiresEndpointsAndClientId() {
		var noAuthz = McpAuthorizationCodeAcquirer.create()
			.tokenEndpoint(URI.create("https://idp.example.com/token")).clientId("c").resource(RESOURCE).expectedIssuer(ISSUER);
		assertTrue(assertThrows(IllegalStateException.class, noAuthz::build).getMessage().contains("authorizationEndpoint"));
		var noToken = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize")).clientId("c").resource(RESOURCE).expectedIssuer(ISSUER);
		assertTrue(assertThrows(IllegalStateException.class, noToken::build).getMessage().contains("tokenEndpoint"));
		var noClient = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(URI.create("https://idp.example.com/authorize"))
			.tokenEndpoint(URI.create("https://idp.example.com/token")).resource(RESOURCE).expectedIssuer(ISSUER);
		assertTrue(assertThrows(IllegalStateException.class, noClient::build).getMessage().contains("clientId"));
	}

	@Test void e13_builderArgumentValidation() {
		var b = McpAuthorizationCodeAcquirer.create();
		assertThrows(IllegalArgumentException.class, () -> b.clientSecret(" "));
		assertThrows(IllegalArgumentException.class, () -> b.scope(" "));
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(Duration.ZERO));
		var negative = Duration.ofSeconds(-1);
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(negative));
	}

	@Test void e14_validFixedRedirectPortAccepted() {
		assertEquals(8080, minimal().redirectPort(8080).build().redirectPort());
		assertEquals(1, minimal().redirectPort(1).build().redirectPort());
		assertEquals(0, minimal().redirectPort(0).build().redirectPort());
		assertEquals(65535, minimal().redirectPort(65535).build().redirectPort());
	}
}
