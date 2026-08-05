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
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

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
}
