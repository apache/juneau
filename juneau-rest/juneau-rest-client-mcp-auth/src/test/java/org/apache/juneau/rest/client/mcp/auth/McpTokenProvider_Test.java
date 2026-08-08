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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.client.mcp.auth.flow.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@link McpTokenProvider}: static / client-credentials / refresh-token modes, RFC 8707 resource-indicator
 * emission, caching + expiry re-acquisition, rotated-refresh-token capture (SEP-2207), builder validation, and
 * secret redaction.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // HttpServer held as test fixture; lifecycle managed by @AfterEach
})
class McpTokenProvider_Test extends TestBase {

	private static final URI RES = URI.create("https://mcp.example.com/api");

	private HttpServer server;
	private final List<String> requestBodies = new CopyOnWriteArrayList<>();
	private final BlockingQueue<String> responses = new LinkedBlockingQueue<>();

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/token", ex -> {
			try (var is = ex.getRequestBody()) {
				requestBodies.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
			}
			var body = Objects.requireNonNull(responses.poll(), "no stubbed response").getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private URI tokenEndpoint() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
	}

	private static String tokenJson(String at, int expiresIn) {
		return "{\"access_token\":\"" + at + "\",\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn + "}";
	}

	private static String tokenJsonWithRefresh(String at, String rt, int expiresIn) {
		return "{\"access_token\":\"" + at + "\",\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn
			+ ",\"refresh_token\":\"" + rt + "\"}";
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: static / pre-provisioned
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_staticTokenAlwaysReturned() {
		var p = McpTokenProvider.ofStaticToken("preprovisioned");
		assertEquals("preprovisioned", p.get());
		assertEquals("preprovisioned", p.get());
	}

	@Test void a02_staticTokenBlankRejected() {
		assertThrows(IllegalArgumentException.class, () -> McpTokenProvider.ofStaticToken("  "));
		assertThrows(IllegalArgumentException.class, () -> McpTokenProvider.ofStaticToken(null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: client-credentials
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_clientCredentialsAcquires() {
		responses.add(tokenJson("cc-at", 3600));
		var p = McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint()).clientId("id").clientSecret("s").resource(RES).scope("mcp:read").build();
		assertEquals("cc-at", p.get());
	}

	@Test void b02_resourceIndicatorSentOnTokenRequest() {
		responses.add(tokenJson("cc-at", 3600));
		var p = McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint()).clientId("id").clientSecret("s")
			.resource(URI.create("https://mcp.example.com/api")).build();
		p.get();
		var body = requestBodies.get(0);
		assertTrue(body.contains("resource="), () -> "resource indicator missing from token request: " + body);
		assertTrue(body.contains("mcp.example.com"), () -> "resource value missing: " + body);
	}

	@Test void b03_cachedWhileValid() {
		responses.add(tokenJson("cc-at", 3600));
		var p = McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint()).clientId("id").clientSecret("s").resource(RES).build();
		assertEquals("cc-at", p.get());
		// Second call should be served from cache without a second token-endpoint request.
		assertEquals("cc-at", p.get());
		assertEquals(1, requestBodies.size());
	}

	@Test void b04_reacquiresWhenExpired() {
		// Large skew forces the freshly-acquired token to be treated as already expired -> re-acquire each call.
		responses.add(tokenJson("cc-at-1", 3600));
		responses.add(tokenJson("cc-at-2", 3600));
		var p = McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint()).clientId("id").clientSecret("s").resource(RES)
			.expirySkew(Duration.ofHours(2)).build();
		assertEquals("cc-at-1", p.get());
		assertEquals("cc-at-2", p.get());
		assertEquals(2, requestBodies.size());
	}

	@Test void b05_errorResponsePropagates() {
		// Non-2xx from the token endpoint surfaces as OAuthFlowException from the underlying flow.
		server.removeContext("/token");
		server.createContext("/token", ex -> {
			var b = "{\"error\":\"invalid_client\"}".getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(400, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); }
		});
		var p = McpTokenProvider.clientCredentials()
			.tokenEndpoint(tokenEndpoint()).clientId("id").clientSecret("s").resource(RES).build();
		assertThrows(OAuthFlowException.class, p::get);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: refresh-token (SEP-2207)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_refreshAcquiresAndSendsRefreshToken() {
		responses.add(tokenJson("refreshed-at", 3600));
		var p = McpTokenProvider.refreshToken("rt-initial")
			.tokenEndpoint(tokenEndpoint()).clientId("id").resource(RES).build();
		assertEquals("refreshed-at", p.get());
		assertTrue(requestBodies.get(0).contains("refresh_token=rt-initial"));
	}

	@Test void c02_rotatedRefreshTokenCapturedAndReused() {
		responses.add(tokenJsonWithRefresh("at-1", "rt-2", 3600));
		responses.add(tokenJsonWithRefresh("at-2", "rt-3", 3600));
		var p = McpTokenProvider.refreshToken("rt-1")
			.tokenEndpoint(tokenEndpoint()).clientId("id").resource(RES)
			.expirySkew(Duration.ofHours(2)).build(); // force re-acquire each call
		assertEquals("at-1", p.get());
		assertEquals("rt-2", p.currentRefreshToken().orElseThrow());
		assertEquals("at-2", p.get());
		assertEquals("rt-3", p.currentRefreshToken().orElseThrow());
		// The second refresh must have used the rotated token rt-2, not the original rt-1.
		assertTrue(requestBodies.get(1).contains("refresh_token=rt-2"), () -> requestBodies.get(1));
	}

	@Test void c03_resourceIndicatorSentOnRefreshRequest() {
		// H6: the refresh-flow token request must carry the RFC 8707 resource indicator too.
		responses.add(tokenJson("refreshed-at", 3600));
		var p = McpTokenProvider.refreshToken("rt-initial")
			.tokenEndpoint(tokenEndpoint()).clientId("id").resource(RES).build();
		p.get();
		var body = requestBodies.get(0);
		assertTrue(body.contains("resource="), () -> "resource indicator missing from refresh request: " + body);
		assertTrue(body.contains("mcp.example.com"), () -> "resource value missing: " + body);
	}

	@Test void c04_refreshInvalidGrantLatchesTerminal() {
		// M7: an invalid_grant on refresh latches a terminal state; subsequent get() throws McpAuthException
		// without re-hitting the IdP with the dead refresh token.
		server.removeContext("/token");
		var hits = new AtomicInteger();
		server.createContext("/token", ex -> {
			hits.incrementAndGet();
			var b = "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(400, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); }
		});
		var p = McpTokenProvider.refreshToken("rt-dead")
			.tokenEndpoint(tokenEndpoint()).clientId("id").resource(RES).build();
		var e1 = assertThrows(McpAuthException.class, p::get);
		assertTrue(e1.getMessage().contains("invalid_grant"), e1::getMessage);
		var e2 = assertThrows(McpAuthException.class, p::get);
		assertTrue(e2.getMessage().contains("re-authorization required"), e2::getMessage);
		assertEquals(1, hits.get(), "terminal state must not re-hit the IdP");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: builder validation + redaction
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_clientCredentialsRequiredFields() {
		var b1 = McpTokenProvider.clientCredentials();
		assertThrows(IllegalStateException.class, b1::build);
		var b2 = McpTokenProvider.clientCredentials().tokenEndpoint(tokenEndpoint());
		assertThrows(IllegalStateException.class, b2::build);
		var b3 = McpTokenProvider.clientCredentials().tokenEndpoint(tokenEndpoint()).clientId("id");
		assertThrows(IllegalStateException.class, b3::build);
		// H3: resource() is mandatory on the MCP-facing entry point.
		var b4 = McpTokenProvider.clientCredentials().tokenEndpoint(tokenEndpoint()).clientId("id").clientSecret("s");
		assertThrows(IllegalStateException.class, b4::build);
	}

	@Test void d02_refreshRequiredFields() {
		assertThrows(IllegalArgumentException.class, () -> McpTokenProvider.refreshToken(" "));
		var b = McpTokenProvider.refreshToken("rt");
		assertThrows(IllegalStateException.class, b::build);
	}

	@Test void d03_negativeSkewRejected() {
		var b = McpTokenProvider.clientCredentials();
		var skew = Duration.ofSeconds(-1);
		assertThrows(IllegalArgumentException.class, () -> b.expirySkew(skew));
	}

	@Test void d04_toStringRedactsSecrets() {
		var p = McpTokenProvider.ofStaticToken("super-secret-token");
		assertFalse(p.toString().contains("super-secret-token"), p::toString);
		assertTrue(p.toString().contains("<redacted>"));
	}

	@Test void d05_interceptorFactory() {
		var p = McpTokenProvider.ofStaticToken("t");
		assertNotNull(p.interceptor());
	}
}
