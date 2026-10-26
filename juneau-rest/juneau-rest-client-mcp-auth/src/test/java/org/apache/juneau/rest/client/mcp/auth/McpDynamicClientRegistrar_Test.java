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

import static org.apache.juneau.BasicTestUtils.assertThrowsWithMessage;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.nimbusds.common.contenttype.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.openid.connect.sdk.rp.*;

/**
 * Tests for {@link McpDynamicClientRegistrar} (SEP-837 RFC 7591 registration round-trip).
 *
 * @since 10.0.0
 */
class McpDynamicClientRegistrar_Test extends TestBase {

	private static final URI REG = URI.create("https://as.example.com/register");
	private static final URI ISSUER = URI.create("https://as.example.com");

	private static McpDynamicClientRegistrar.Builder base() {
		return McpDynamicClientRegistrar.create()
			.registrationEndpoint(REG)
			.issuer(ISSUER)
			.applicationType(McpApplicationType.NATIVE)
			.addRedirectUris(LoopbackRedirectUris.forPort(3000, "/callback"));
	}

	// Builder validation

	@Test void a01_requiresRegistrationEndpoint() {
		assertThrowsWithMessage(IllegalStateException.class, "registrationEndpoint", () ->
			McpDynamicClientRegistrar.create().issuer(ISSUER).applicationType(McpApplicationType.NATIVE)
				.addRedirectUri(URI.create("http://127.0.0.1/callback")).build());
	}

	@Test void a02_requiresApplicationType() {
		assertThrowsWithMessage(IllegalStateException.class, "applicationType", () ->
			McpDynamicClientRegistrar.create().registrationEndpoint(REG).issuer(ISSUER)
				.addRedirectUri(URI.create("http://127.0.0.1/callback")).build());
	}

	@Test void a03_requiresRedirectUri() {
		assertThrowsWithMessage(IllegalStateException.class, "redirectUri", () ->
			McpDynamicClientRegistrar.create().registrationEndpoint(REG).issuer(ISSUER)
				.applicationType(McpApplicationType.NATIVE).build());
	}

	@Test void a04_requiresHttpsEndpoint() {
		assertThrowsWithMessage(IllegalStateException.class, "https", () ->
			McpDynamicClientRegistrar.create().registrationEndpoint(URI.create("http://as.example.com/register"))
				.issuer(ISSUER).applicationType(McpApplicationType.NATIVE)
				.addRedirectUri(URI.create("http://127.0.0.1/callback")).build());
	}

	@Test void a05_loopbackEndpointExemptFromHttps() {
		assertDoesNotThrow(() -> McpDynamicClientRegistrar.create()
			.registrationEndpoint(URI.create("http://127.0.0.1:9000/register"))
			.issuer(ISSUER).applicationType(McpApplicationType.NATIVE)
			.addRedirectUri(URI.create("http://127.0.0.1/callback")).build());
	}

	@Test void a06_noRequireSecureEscapeHatch() {
		var hasRequireSecure = Arrays.stream(McpDynamicClientRegistrar.Builder.class.getMethods())
			.anyMatch(m -> eq(m.getName(), "requireSecure"));
		assertFalse(hasRequireSecure, "Builder must not expose a public requireSecure(...) escape hatch");
	}

	// Request construction

	@Test void b01_emitsApplicationTypeNative() {
		var req = base().build().buildRegistrationRequest();
		assertEquals(ApplicationType.NATIVE, req.getOIDCClientMetadata().getApplicationType());
	}

	@Test void b02_emitsApplicationTypeWeb() {
		var req = base().applicationType(McpApplicationType.WEB).build().buildRegistrationRequest();
		assertEquals(ApplicationType.WEB, req.getOIDCClientMetadata().getApplicationType());
	}

	@Test void b03_publicClientUsesAuthMethodNone() {
		var req = base().build().buildRegistrationRequest();
		assertEquals(ClientAuthenticationMethod.NONE, req.getOIDCClientMetadata().getTokenEndpointAuthMethod());
	}

	@Test void b04_confidentialClientUsesSecretBasic() {
		var req = base().confidential(true).build().buildRegistrationRequest();
		assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, req.getOIDCClientMetadata().getTokenEndpointAuthMethod());
	}

	@Test void b05_registersLoopbackRedirectUris() {
		var req = base().build().buildRegistrationRequest();
		var uris = req.getOIDCClientMetadata().getRedirectionURIs();
		assertTrue(uris.contains(URI.create("http://127.0.0.1:3000/callback")));
		assertTrue(uris.contains(URI.create("http://localhost:3000/callback")));
	}

	@Test void b06_applicationTypeSerializedInBody() {
		var http = base().build().buildRegistrationRequest().toHTTPRequest();
		assertTrue(http.getBody().contains("\"application_type\":\"native\""), http.getBody());
	}

	@Test void b07_initialAccessTokenAttached() {
		var http = base().initialAccessToken("init-tok").build().buildRegistrationRequest().toHTTPRequest();
		assertEquals("Bearer init-tok", http.getAuthorization());
	}

	// Response mapping

	private static HTTPResponse jsonResponse(int status, String body) {
		var r = new HTTPResponse(status);
		r.setEntityContentType(ContentType.APPLICATION_JSON);
		r.setBody(body);
		return r;
	}

	@Test void c01_parsesSuccessfulRegistration() {
		var body = "{\"client_id\":\"abc\",\"client_secret\":\"sec\",\"client_secret_expires_at\":0,"
			+ "\"registration_access_token\":\"rat\",\"registration_client_uri\":\"https://as.example.com/register/abc\","
			+ "\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"],\"application_type\":\"native\","
			+ "\"token_endpoint_auth_method\":\"none\"}";
		var reg = base().build().parseRegistrationResponse(jsonResponse(201, body));
		assertEquals("abc", reg.clientId());
		assertEquals("sec", reg.clientSecret().orElseThrow());
		assertEquals(ISSUER, reg.issuer());
		assertEquals("rat", reg.registrationAccessToken().orElseThrow());
		assertEquals(URI.create("https://as.example.com/register/abc"), reg.registrationClientUri().orElseThrow());
	}

	@Test void c02_publicClientHasNoSecret() {
		var body = "{\"client_id\":\"pub\",\"redirect_uris\":[\"http://127.0.0.1/callback\"],"
			+ "\"application_type\":\"native\",\"token_endpoint_auth_method\":\"none\"}";
		var reg = base().build().parseRegistrationResponse(jsonResponse(201, body));
		assertEquals("pub", reg.clientId());
		assertTrue(reg.clientSecret().isEmpty());
		assertFalse(reg.isConfidential());
	}

	@Test void c03_registrationErrorSurfacesAsMcpAuthException() {
		var body = "{\"error\":\"invalid_redirect_uri\",\"error_description\":\"redirect not allowed\"}";
		assertThrowsWithMessage(McpAuthException.class, "invalid_redirect_uri", () ->
			base().build().parseRegistrationResponse(jsonResponse(400, body)));
	}

	@Test void c04_stampedIssuerIsBuilderIssuer() {
		var body = "{\"client_id\":\"abc\",\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"],"
			+ "\"application_type\":\"native\",\"token_endpoint_auth_method\":\"none\"}";
		var reg = McpDynamicClientRegistrar.create().registrationEndpoint(REG)
			.issuer(URI.create("https://other.example.com")).applicationType(McpApplicationType.NATIVE)
			.addRedirectUri(URI.create("http://127.0.0.1:3000/callback")).build()
			.parseRegistrationResponse(jsonResponse(201, body));
		assertEquals(URI.create("https://other.example.com"), reg.issuer());
	}

	// LOW: any non-registered custom field the AS returns in the registration response is surfaced as the
	// registration's extras (rather than always Map.of()).
	@Test void c05_customFieldsSurfacedAsExtras() {
		var body = "{\"client_id\":\"abc\",\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"],"
			+ "\"application_type\":\"native\",\"token_endpoint_auth_method\":\"none\",\"vendor_flag\":\"xyz\"}";
		var reg = base().build().parseRegistrationResponse(jsonResponse(201, body));
		assertEquals("xyz", reg.extras().get("vendor_flag"));
	}

	@Test void d01_toStringRedactsInitialAccessToken() {
		var s = base().initialAccessToken("init-tok").build().toString();
		assertFalse(s.contains("init-tok"));
		assertTrue(s.contains("<redacted>"));
	}

	// Builder options + request contents

	@Test void e01_requiresIssuer() {
		assertThrowsWithMessage(IllegalStateException.class, "issuer", () ->
			McpDynamicClientRegistrar.create().registrationEndpoint(REG).applicationType(McpApplicationType.NATIVE)
				.addRedirectUri(URI.create("http://127.0.0.1/callback")).build());
	}

	@Test void e02_optionalBuilderFieldsReachTheRequest() {
		var req = base()
			.scope("mcp:read", "mcp:write")
			.clientName("Demo Client")
			.addGrantType("authorization_code")
			.addResponseType("code")
			.addRedirectUri(URI.create("http://127.0.0.1:3001/other"))
			.httpTimeout(Duration.ofSeconds(5))
			.build().buildRegistrationRequest();
		var md = req.getOIDCClientMetadata();
		assertEquals("Demo Client", md.getName());
		assertEquals("mcp:read mcp:write", md.getScope().toString());
		assertSize(1, md.getGrantTypes());
		assertSize(1, md.getResponseTypes());
		assertTrue(md.getRedirectionURIs().contains(URI.create("http://127.0.0.1:3001/other")));
	}

	@Test void e03_defaultGrantAndResponseTypes() {
		var md = base().build().buildRegistrationRequest().getOIDCClientMetadata();
		assertSize(() -> "authorization_code + refresh_token", 2, md.getGrantTypes());
		assertSize(() -> "code", 1, md.getResponseTypes());
	}

	@Test void e04_builderArgumentValidation() {
		var b = McpDynamicClientRegistrar.create();
		assertThrows(IllegalArgumentException.class, () -> b.scope(" "));
		assertThrows(IllegalArgumentException.class, () -> b.addGrantType(" "));
		assertThrows(IllegalArgumentException.class, () -> b.addResponseType(" "));
		assertThrows(IllegalArgumentException.class, () -> b.clientName(" "));
		assertThrows(IllegalArgumentException.class, () -> b.initialAccessToken(" "));
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(Duration.ZERO));
		var negative = Duration.ofSeconds(-1);
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(negative));
	}

	@Test void e05_toStringWithoutInitialAccessToken() {
		var s = base().build().toString();
		assertTrue(s.contains("initialAccessToken=<none>"), s);
	}

	// Response mapping edge cases

	@Test void f01_secretExpiryAndEmptyExtrasMapped() {
		var body = "{\"client_id\":\"abc\",\"client_secret\":\"sec\",\"client_secret_expires_at\":1893456000,"
			+ "\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"],\"application_type\":\"native\"}";
		var reg = base().build().parseRegistrationResponse(jsonResponse(201, body));
		assertEquals(Instant.ofEpochSecond(1893456000L), reg.clientSecretExpiresAt().orElseThrow());
		assertTrue(reg.registrationAccessToken().isEmpty());
		assertTrue(reg.registrationClientUri().isEmpty());
	}

	@Test void f02_unparseableSuccessBodyThrowsMcpAuthException() {
		assertThrowsWithMessage(McpAuthException.class, "could not be parsed", () ->
			base().build().parseRegistrationResponse(jsonResponse(201, "not json")));
	}

	@Test void f03_errorWithoutCodeReportedAsUnknown() {
		var r = new HTTPResponse(500);
		assertThrowsWithMessage(McpAuthException.class, "unknown_error", () ->
			base().build().parseRegistrationResponse(r));
	}

	@Test void f04_errorDescriptionIncludedWhenPresent() {
		var body = "{\"error\":\"invalid_redirect_uri\",\"error_description\":\"redirect not allowed\"}";
		assertThrowsWithMessage(McpAuthException.class, "(redirect not allowed)", () ->
			base().build().parseRegistrationResponse(jsonResponse(400, body)));
	}

	@Test void f05_errorWithoutDescriptionOmitsParenthetical() {
		var body = "{\"error\":\"invalid_client_metadata\"}";
		var registrar = base().build();
		var response = jsonResponse(400, body);
		var e = assertThrows(McpAuthException.class, () -> registrar.parseRegistrationResponse(response));
		assertFalse(e.getMessage().contains("("), e::getMessage);
	}

	// Live round-trip against an in-JVM loopback stub

	@Test void g01_registerRoundTripAppliesConfiguratorAndAuthorization() throws Exception {
		var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		var auth = new AtomicReference<String>();
		server.createContext("/register", ex -> {
			auth.set(ex.getRequestHeaders().getFirst("Authorization"));
			try (var in = ex.getRequestBody()) { in.readAllBytes(); }
			var b = ("{\"client_id\":\"live\",\"redirect_uris\":[\"http://127.0.0.1:3000/callback\"],"
				+ "\"application_type\":\"native\"}").getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(201, b.length);
			try (var os = ex.getResponseBody()) { os.write(b); }
		});
		server.start();
		try {
			var configured = new AtomicInteger();
			var reg = McpDynamicClientRegistrar.create()
				.registrationEndpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/register"))
				.issuer(ISSUER).applicationType(McpApplicationType.NATIVE)
				.addRedirectUris(List.of(URI.create("http://127.0.0.1:3000/callback")))
				.initialAccessToken("init-tok")
				.httpTimeout(Duration.ofSeconds(5))
				.httpRequestConfigurator(r -> configured.incrementAndGet())
				.build().register();
			assertEquals("live", reg.clientId());
			assertEquals(1, configured.get());
			assertEquals("Bearer init-tok", auth.get());
		} finally {
			server.stop(0);
		}
	}

	@Test void g02_registerHttpFailureThrowsMcpAuthException() throws Exception {
		int port;
		try (var s = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			port = s.getLocalPort();
		}
		var registrar = McpDynamicClientRegistrar.create()
			.registrationEndpoint(URI.create("http://127.0.0.1:" + port + "/register"))
			.issuer(ISSUER).applicationType(McpApplicationType.NATIVE)
			.addRedirectUri(URI.create("http://127.0.0.1:3000/callback")).build();
		assertThrowsWithMessage(McpAuthException.class, "HTTP call failed", registrar::register);
	}
}
