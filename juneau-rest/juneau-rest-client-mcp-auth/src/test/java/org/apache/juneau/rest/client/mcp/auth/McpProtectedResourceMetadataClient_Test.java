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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@link McpProtectedResourceMetadataClient}: PRM parse/fetch and authorization-server discovery + issuer
 * validation, exercised against an in-JVM stub.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // HttpServer held as test fixture; lifecycle managed by @AfterEach
})
class McpProtectedResourceMetadataClient_Test extends TestBase {

	private HttpServer server;
	private volatile String issuerInDoc;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/.well-known/openid-configuration", ex -> respond(ex, oidcMetadataJson()));
		server.createContext("/.well-known/oauth-protected-resource", ex -> respond(ex, prmJson()));
		server.start();
		issuerInDoc = base();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private static void respond(HttpExchange ex, String body) throws IOException {
		var b = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().add("Content-Type", "application/json");
		ex.sendResponseHeaders(200, b.length);
		try (var os = ex.getResponseBody()) {
			os.write(b);
		}
	}

	private String base() {
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	private String prmJson() {
		return "{\"resource\":\"" + base() + "\","
			+ "\"authorization_servers\":[\"" + base() + "\"],"
			+ "\"scopes_supported\":[\"mcp:read\",\"mcp:write\"]}";
	}

	private String oidcMetadataJson() {
		return "{\"issuer\":\"" + issuerInDoc + "\","
			+ "\"authorization_endpoint\":\"" + base() + "/authorize\","
			+ "\"token_endpoint\":\"" + base() + "/token\","
			+ "\"jwks_uri\":\"" + base() + "/jwks\","
			+ "\"response_types_supported\":[\"code\"],"
			+ "\"subject_types_supported\":[\"public\"],"
			+ "\"id_token_signing_alg_values_supported\":[\"RS256\"]}";
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: parse
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_parseHappy() {
		var prm = McpProtectedResourceMetadataClient.create().build().parse(prmJson(), null);
		assertEquals(URI.create(base()), prm.resource());
		assertEquals(URI.create(base()), prm.firstAuthorizationServer().orElseThrow());
		assertTrue(prm.scopesSupported().contains("mcp:read"));
	}

	@Test void a02_parseMissingResourceThrows() {
		var client = McpProtectedResourceMetadataClient.create().build();
		assertThrows(McpAuthException.class, () -> client.parse("{\"authorization_servers\":[\"https://as\"]}", null));
	}

	@Test void a03_parseMalformedJsonThrows() {
		var client = McpProtectedResourceMetadataClient.create().build();
		assertThrows(McpAuthException.class, () -> client.parse("not json", null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: fetch
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_fetchHappy() {
		var prm = McpProtectedResourceMetadataClient.create().build()
			.fetch(URI.create(base() + "/.well-known/oauth-protected-resource"));
		assertEquals(URI.create(base()), prm.resource());
	}

	@Test void b02_fetch404Throws() {
		var client = McpProtectedResourceMetadataClient.create().build();
		var url = URI.create(base() + "/does-not-exist");
		assertThrows(McpAuthException.class, () -> client.fetch(url));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: authorization-server discovery + issuer validation
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_discoverAuthorizationServer() {
		var client = McpProtectedResourceMetadataClient.create().build();
		var prm = client.fetch(URI.create(base() + "/.well-known/oauth-protected-resource"));
		var md = client.discoverAuthorizationServer(prm);
		assertEquals(URI.create(base()), md.issuer());
		assertEquals(URI.create(base() + "/token"), md.tokenEndpoint());
	}

	@Test void c02_issuerMismatchRejected() {
		// AS metadata advertises a different issuer than requested -> RFC 8414 issuer check fails.
		issuerInDoc = "https://evil.example.com";
		var client = McpProtectedResourceMetadataClient.create().build();
		var prm = client.parse(prmJson(), null);
		assertThrows(McpAuthException.class, () -> client.discoverAuthorizationServer(prm));
	}

	@Test void c03_noAuthorizationServersThrows() {
		var client = McpProtectedResourceMetadataClient.create().build();
		var prm = client.parse("{\"resource\":\"" + base() + "\"}", null);
		assertThrows(McpAuthException.class, () -> client.discoverAuthorizationServer(prm));
	}

	@Test void c04_resourceMismatchRejected() {
		// B1 (RFC 9728 3.3): the PRM document's resource field must match the resource the client was talking to.
		var client = McpProtectedResourceMetadataClient.create()
			.expectedResource(URI.create("https://different.example.com")).build();
		var e = assertThrows(McpAuthException.class, () -> client.parse(prmJson(), null));
		assertTrue(e.getMessage().contains("9728"), e::getMessage);
	}

	@Test void c05_resourceMatchAccepted() {
		// B1 positive: a matching resource identity passes.
		var client = McpProtectedResourceMetadataClient.create()
			.expectedResource(URI.create(base())).build();
		var prm = client.parse(prmJson(), null);
		assertEquals(URI.create(base()), prm.resource());
	}

	@Test void c06_discoverViaRfc8414Only() {
		// H4: with only the RFC 8414 oauth-authorization-server endpoint available (no OIDC openid-configuration),
		// discovery must still succeed via the AuthorizationServerMetadata path.
		server.removeContext("/.well-known/openid-configuration");
		server.createContext("/.well-known/oauth-authorization-server", ex -> respond(ex, oidcMetadataJson()));
		var client = McpProtectedResourceMetadataClient.create().build();
		var prm = client.parse(prmJson(), null);
		var md = client.discoverAuthorizationServer(prm);
		assertEquals(URI.create(base()), md.issuer());
		assertEquals(URI.create(base() + "/token"), md.tokenEndpoint());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: https scheme enforcement (M8)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_insecureNonLoopbackPrmUrlRejected() {
		// M8: a non-loopback plain-http PRM URL is rejected before any network I/O.
		var client = McpProtectedResourceMetadataClient.create().build();
		var url = URI.create("http://example.com/.well-known/oauth-protected-resource");
		var e = assertThrows(McpAuthException.class, () -> client.fetch(url));
		assertTrue(e.getMessage().contains("https"), e::getMessage);
	}

	@Test void d02_allowInsecureHttpOptOut() {
		// M8: the opt-out disables the scheme check (loopback http already works; this asserts the flag path builds
		// and fetches over the loopback stub).
		var prm = McpProtectedResourceMetadataClient.create().allowInsecureHttp(true).build()
			.fetch(URI.create(base() + "/.well-known/oauth-protected-resource"));
		assertEquals(URI.create(base()), prm.resource());
	}
}
