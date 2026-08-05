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
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link McpClientRegistrations} (B3: feeding DCR-issued credentials into F1's acquirer + token provider).
 *
 * @since 10.0.0
 */
class McpDcrWiring_Test extends TestBase {

	private static final URI ISSUER = URI.create("https://as.example.com");
	private static final URI AUTH = URI.create("https://as.example.com/auth");
	private static final URI TOKEN = URI.create("https://as.example.com/token");
	private static final URI RESOURCE = URI.create("https://mcp.example.com");

	private static McpClientRegistration confidential() {
		return new McpClientRegistration("dcr-client", Optional.of("dcr-secret"), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, List.of(URI.create("http://127.0.0.1:3000/cb"), URI.create("http://localhost:3000/cb")),
			McpApplicationType.NATIVE, Map.of());
	}

	private static McpClientRegistration publicClient() {
		return new McpClientRegistration("pub-client", Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, List.of(URI.create("http://127.0.0.1:3000/callback")), McpApplicationType.NATIVE, Map.of());
	}

	@Test void a01_feedsAuthorizationCodeAcquirer() {
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(AUTH).tokenEndpoint(TOKEN).resource(RESOURCE).expectedIssuer(ISSUER);
		var ret = McpClientRegistrations.configure(b, confidential());
		assertSame(b, ret);
		assertDoesNotThrow(ret::build);
	}

	@Test void a02_publicClientAcquirerHasNoSecret() {
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(AUTH).tokenEndpoint(TOKEN).resource(RESOURCE).expectedIssuer(ISSUER);
		assertDoesNotThrow(() -> McpClientRegistrations.configure(b, publicClient()).build());
	}

	@Test void a03_feedsClientCredentialsProvider() {
		var b = McpTokenProvider.clientCredentials().tokenEndpoint(TOKEN).resource(RESOURCE);
		McpClientRegistrations.configure(b, confidential());
		assertDoesNotThrow(b::build);
	}

	@Test void a04_feedsRefreshProvider() {
		var b = McpTokenProvider.refreshToken("rt").tokenEndpoint(TOKEN).resource(RESOURCE);
		McpClientRegistrations.configure(b, confidential());
		assertDoesNotThrow(b::build);
	}

	@Test void b01_resourceStillRequiredAfterWiring() {
		// SEP-2352 changes only credential provenance: the RFC 8707 resource indicator is still mandatory.
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(AUTH).tokenEndpoint(TOKEN).expectedIssuer(ISSUER);
		McpClientRegistrations.configure(b, confidential());
		assertThrows(IllegalStateException.class, b::build);
	}

	@Test void b02_expectedIssuerStillRequiredAfterWiring() {
		// SEP-2468 iss validation still applies to DCR-issued clients.
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(AUTH).tokenEndpoint(TOKEN).resource(RESOURCE);
		McpClientRegistrations.configure(b, confidential());
		assertThrows(IllegalStateException.class, b::build);
	}

	@Test void b03_clientCredentialsSecretRequiredWhenPublic() {
		// A public DCR client (no secret) can't drive client-credentials (which requires a secret) — build must fail.
		var b = McpTokenProvider.clientCredentials().tokenEndpoint(TOKEN).resource(RESOURCE);
		McpClientRegistrations.configure(b, publicClient());
		assertThrows(IllegalStateException.class, b::build);
	}

	@Test void c01_nullArgsRejected() {
		var reg = confidential();
		assertThrows(IllegalArgumentException.class, () -> McpClientRegistrations.configure((McpAuthorizationCodeAcquirer.Builder) null, reg));
		var b = McpAuthorizationCodeAcquirer.create();
		assertThrows(IllegalArgumentException.class, () -> McpClientRegistrations.configure(b, null));
	}

	// H2: a bind-first (forPort) registration carries its registered PORT through onto the acquirer so the receiver
	// binds exactly the port the AS was told (not a fresh ephemeral one).
	@Test void d01_configureCarriesRegisteredPort() {
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(AUTH).tokenEndpoint(TOKEN).resource(RESOURCE).expectedIssuer(ISSUER);
		var acquirer = McpClientRegistrations.configure(b, confidential()).build();
		assertEquals(3000, acquirer.redirectPort());
	}

	// H2: a port-agnostic registration (no port on the redirect URI) leaves the acquirer on its ephemeral default.
	@Test void d02_portAgnosticRegistrationLeavesEphemeralDefault() {
		var reg = new McpClientRegistration("pa-client", Optional.empty(), Optional.empty(), Optional.empty(),
			Optional.empty(), ISSUER, LoopbackRedirectUris.portAgnostic("/callback"), McpApplicationType.NATIVE, Map.of());
		var b = McpAuthorizationCodeAcquirer.create()
			.authorizationEndpoint(AUTH).tokenEndpoint(TOKEN).resource(RESOURCE).expectedIssuer(ISSUER);
		var acquirer = McpClientRegistrations.configure(b, reg).build();
		assertEquals(0, acquirer.redirectPort());
	}
}
