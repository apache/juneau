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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.oauth.oidc.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Live-stub tests for {@link OidcDiscoveryClient}: exercises the full {@code discover()} path via an
 * in-JVM {@code com.sun.net.httpserver.HttpServer} serving a minimal OIDC discovery document.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // HttpServer held as test fixture; lifecycle managed by @AfterEach
})
class OidcDiscoveryClient_Live_Test extends TestBase {

	private HttpServer server;
	private int port;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		port = server.getAddress().getPort();
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private URI issuerUri() {
		return URI.create("http://127.0.0.1:" + port);
	}

	private void serveDiscovery(String json) {
		server.createContext("/.well-known/openid-configuration", ex -> {
			var body = json.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
	}

	private String minimalDiscovery(boolean withScopes, boolean withExtras) {
		var base = issuerUri().toString();
		var sb = new StringBuilder("{")
			.append("\"issuer\":\"").append(base).append("\",")
			.append("\"authorization_endpoint\":\"").append(base).append("/authorize\",")
			.append("\"token_endpoint\":\"").append(base).append("/token\",")
			.append("\"jwks_uri\":\"").append(base).append("/jwks\",")
			.append("\"response_types_supported\":[\"code\"],")
			.append("\"subject_types_supported\":[\"public\"],")
			.append("\"id_token_signing_alg_values_supported\":[\"RS256\"]");
		if (withScopes)
			sb.append(",\"scopes_supported\":[\"openid\",\"profile\"]");
		if (withExtras)
			sb.append(",\"custom_field\":\"custom_value\"");
		sb.append("}");
		return sb.toString();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: discover() — no configurator (line 138 false), with scopes (line 166 false), no extras (line 148 false)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_discover_noConfigurator_withScopes() throws Exception {
		serveDiscovery(minimalDiscovery(true, false));
		var c = OidcDiscoveryClient.create().issuer(issuerUri()).build();
		var md = c.discover();
		assertEquals(issuerUri(), md.issuer());
		assertTrue(md.supportedScopes().contains("openid")); // scopes_supported present → populated
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: discover() — with httpRequestConfigurator (line 138 true), no scopes (line 166 true → empty), with extras
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_discover_withConfigurator_noScopes_withExtras() throws Exception {
		serveDiscovery(minimalDiscovery(false, true));
		var c = OidcDiscoveryClient.create()
			.issuer(issuerUri())
			.httpRequestConfigurator(req -> {}) // non-null → hits line 138 true branch and line 139
			.build();
		var md = c.discover();
		assertEquals(issuerUri(), md.issuer());
		assertTrue(md.supportedScopes().isEmpty()); // line 166 true → returns emptySet
		assertEquals("custom_value", md.extras().get("custom_field")); // line 148 true (non-standard field added)
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: discover() — GeneralException path (parse error → OidcDiscoveryException, line 142)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_discover_badJson_throws() {
		server.createContext("/.well-known/openid-configuration", ex -> {
			var body = "not-json".getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		var c = OidcDiscoveryClient.create().issuer(issuerUri()).build();
		assertThrows(OidcDiscoveryException.class, c::discover);
	}
}
