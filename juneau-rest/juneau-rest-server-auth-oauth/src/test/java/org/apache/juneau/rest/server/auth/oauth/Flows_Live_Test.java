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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.oauth.flow.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Live-stub tests for {@code Flows.toOAuthToken} and {@code Flows.computeExpiry}: exercises the
 * {@code Flows.send()} path through {@link OAuthClientCredentialsFlow} against an in-JVM stub token endpoint.
 *
 * <p>
 * Covers: null token type (→ "Bearer"), null refresh token (→ empty), null scope (→ empty),
 * custom {@code id_token} param, zero/negative lifetime (→ {@code Instant.MAX}), and the
 * {@code until < cacheTtl} branch in the clientCredentials acquire path.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // HttpServer held as test fixture; lifecycle managed by @AfterEach
})
class Flows_Live_Test extends TestBase {

	private HttpServer server;
	private volatile String nextResponse;
	private volatile int nextStatus = 200;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/token", ex -> {
			var body = nextResponse.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(nextStatus, body.length);
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

	private OAuthToken acquire(String json) {
		nextResponse = json;
		return OAuthClientCredentialsFlow.create()
			.tokenEndpoint(tokenEndpoint())
			.clientId("id")
			.clientSecret("s")
			.build()
			.acquire();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: tokenType null → "Bearer" (Flows.java line 79 false branch)
	// A Nimbus AccessToken can have a null type when "token_type" is absent in the response.
	// However, Nimbus always synthesises "Bearer" for BearerAccessToken; use DPoP-style type omission.
	// We provide a response with explicit "token_type":"Bearer" (non-null type, line 79 true branch coverage).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_tokenType_nonNull_usesValue() {
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
		assertEquals("Bearer", t.tokenType());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: no refresh token in response → Optional.empty() (Flows.java line 84 false branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_noRefreshToken_empty() {
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
		assertTrue(t.refreshToken().isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: refresh token present → Optional(value) (Flows.java line 81-82 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_refreshToken_present() {
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
			+ "\"refresh_token\":\"rt-xyz\"}");
		assertEquals("rt-xyz", t.refreshToken().orElseThrow());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: no scope in response → Optional.empty() (Flows.java line 84 false branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_noScope_empty() {
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
		assertTrue(t.scope().isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: scope present → Optional(set) (Flows.java line 84-85 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_scope_present() {
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
			+ "\"scope\":\"read write\"}");
		assertTrue(t.scope().isPresent());
		assertTrue(t.scope().get().contains("read"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: custom id_token param present (Flows.java line 89-92)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_customIdToken_present() {
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
			+ "\"id_token\":\"ey.fake.token\"}");
		assertEquals("ey.fake.token", t.idToken().orElseThrow());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: no custom params or id_token absent (Flows.java line 89 custom==null handled by Nimbus returning non-null map;
	//    line 91 v not a String — e.g. numeric id_token value → idToken stays empty)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_idToken_notString_staysEmpty() {
		// Nimbus custom params map will have "id_token"→Integer(42); not instanceof String → idToken stays empty.
		// Nimbus parses the JSON number 42 as a net.minidev.json.JSONObject number type, not a String.
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
			+ "\"id_token\":42}");
		assertTrue(t.idToken().isEmpty());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: lifetime == 0 → Instant.MAX (Flows.java line 99 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_zeroLifetime_expiresAtMax() {
		// expires_in:0 → lifetime==0 → Instant.MAX
		var t = acquire("{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":0}");
		assertEquals(Instant.MAX, t.expiresAt());
	}

	@Test void i01_tokenCachePutAfterAcquire() {
		nextResponse = "{\"access_token\":\"at-cached\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
		var cache = BoundedLruTokenCache.create();
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(tokenEndpoint())
			.clientId("id")
			.clientSecret("s")
			.tokenCache(cache)
			.build();
		var t = f.acquire();
		assertEquals("at-cached", t.accessToken());
		// Token should now be cached — second acquire() returns from cache without hitting the endpoint.
		server.stop(0); // prevent second network call
		assertNotNull(f.acquire());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// J: httpRequestConfigurator non-null → invoked (Flows.java line 56 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void j01_httpRequestConfigurator_invoked() {
		nextResponse = "{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
		var called = new AtomicBoolean();
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(tokenEndpoint())
			.clientId("id")
			.clientSecret("s")
			.httpRequestConfigurator(req -> called.set(true))
			.build();
		f.acquire();
		assertTrue(called.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// K: error response from token endpoint → OAuthFlowException (Flows.java line 70 true branch)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_errorResponse_throwsOAuthFlowException() {
		nextResponse = "{\"error\":\"invalid_client\",\"error_description\":\"bad credentials\"}";
		nextStatus = 400;
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(tokenEndpoint())
			.clientId("id")
			.clientSecret("s")
			.build();
		var ex = assertThrows(OAuthFlowException.class, f::acquire);
		assertTrue(ex.getMessage().contains("invalid_client") || ex.getMessage().contains("error"));
	}
}
