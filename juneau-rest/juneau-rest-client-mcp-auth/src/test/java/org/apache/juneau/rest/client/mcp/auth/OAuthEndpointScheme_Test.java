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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.client.mcp.auth.flow.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that the OAuth flow builders reject a plaintext {@code http} endpoint aimed at a non-loopback host,
 * while accepting {@code https} and loopback endpoints, and that {@link McpDynamicClientRegistrar} shares the
 * same scheme check.
 */
class OAuthEndpointScheme_Test extends TestBase {

	private static final URI PLAINTEXT_REMOTE = URI.create("http://as.example.com/oauth");
	private static final URI HTTPS_REMOTE = URI.create("https://as.example.com/oauth");
	private static final URI LOOPBACK_HOST = URI.create("http://localhost:8080/oauth");
	private static final URI LOOPBACK_IP = URI.create("http://127.0.0.1:8080/oauth");

	// -----------------------------------------------------------------------------------------------------------------
	// A: OAuthAuthorizationCodeFlow
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_authorizationCodeFlow_authorizationEndpoint_rejectsPlaintextRemote() {
		var builder = OAuthAuthorizationCodeFlow.create();
		assertThrows(IllegalArgumentException.class, () -> builder.authorizationEndpoint(PLAINTEXT_REMOTE));
	}

	@Test void a02_authorizationCodeFlow_tokenEndpoint_rejectsPlaintextRemote() {
		var builder = OAuthAuthorizationCodeFlow.create();
		assertThrows(IllegalArgumentException.class, () -> builder.tokenEndpoint(PLAINTEXT_REMOTE));
	}

	@Test void a03_authorizationCodeFlow_acceptsHttpsAndLoopback() {
		assertDoesNotThrow(() -> OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(HTTPS_REMOTE)
			.tokenEndpoint(LOOPBACK_HOST)
			.authorizationEndpoint(LOOPBACK_IP));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: OAuthClientCredentialsFlow
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_clientCredentialsFlow_tokenEndpoint_rejectsPlaintextRemote() {
		var builder = OAuthClientCredentialsFlow.create();
		assertThrows(IllegalArgumentException.class, () -> builder.tokenEndpoint(PLAINTEXT_REMOTE));
	}

	@Test void b02_clientCredentialsFlow_acceptsHttpsAndLoopback() {
		assertDoesNotThrow(() -> OAuthClientCredentialsFlow.create().tokenEndpoint(HTTPS_REMOTE));
		assertDoesNotThrow(() -> OAuthClientCredentialsFlow.create().tokenEndpoint(LOOPBACK_IP));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: OAuthRefreshTokenFlow
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_refreshTokenFlow_tokenEndpoint_rejectsPlaintextRemote() {
		var builder = OAuthRefreshTokenFlow.create();
		assertThrows(IllegalArgumentException.class, () -> builder.tokenEndpoint(PLAINTEXT_REMOTE));
	}

	@Test void c02_refreshTokenFlow_acceptsHttpsAndLoopback() {
		assertDoesNotThrow(() -> OAuthRefreshTokenFlow.create().tokenEndpoint(HTTPS_REMOTE));
		assertDoesNotThrow(() -> OAuthRefreshTokenFlow.create().tokenEndpoint(LOOPBACK_HOST));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: McpAuthorizationCodeAcquirer
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_acquirer_authorizationEndpoint_rejectsPlaintextRemote() {
		var builder = McpAuthorizationCodeAcquirer.create();
		assertThrows(IllegalArgumentException.class, () -> builder.authorizationEndpoint(PLAINTEXT_REMOTE));
	}

	@Test void d02_acquirer_tokenEndpoint_rejectsPlaintextRemote() {
		var builder = McpAuthorizationCodeAcquirer.create();
		assertThrows(IllegalArgumentException.class, () -> builder.tokenEndpoint(PLAINTEXT_REMOTE));
	}

	@Test void d03_acquirer_acceptsHttpsAndLoopback() {
		assertDoesNotThrow(() -> McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(HTTPS_REMOTE)
			.tokenEndpoint(LOOPBACK_IP));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: McpDynamicClientRegistrar shares the same check
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_registrar_isSecureOrLoopback() {
		assertFalse(McpDynamicClientRegistrar.isSecureOrLoopback(PLAINTEXT_REMOTE));
		assertTrue(McpDynamicClientRegistrar.isSecureOrLoopback(HTTPS_REMOTE));
		assertTrue(McpDynamicClientRegistrar.isSecureOrLoopback(LOOPBACK_HOST));
		assertTrue(McpDynamicClientRegistrar.isSecureOrLoopback(LOOPBACK_IP));
	}
}
