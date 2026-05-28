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
package org.apache.juneau.rest.auth.oauth;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.auth.oauth.flow.*;
import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.pkce.*;

/**
 * Builder validation tests for the four flow helpers in {@link org.apache.juneau.rest.auth.oauth.flow}.
 * Network round-trips are not exercised here; those need a stub IdP fixture.
 *
 * @since 9.5.0
 */
class OAuthFlowBuilder_Test extends TestBase {

	@Test void a01_clientCredentials_requiredFields() {
		assertThrows(IllegalStateException.class, () -> OAuthClientCredentialsFlow.create().build());
		assertThrows(IllegalStateException.class, () -> OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token")).build());
		assertThrows(IllegalStateException.class, () -> OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id").build());
	}

	@Test void a02_clientCredentials_buildsWithMinimum() {
		var f = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.clientSecret("secret")
			.scope("read")
			.build();
		assertNotNull(f);
	}

	@Test void b01_authCode_requiredFields() {
		assertThrows(IllegalStateException.class, () -> OAuthAuthorizationCodeFlow.create().build());
	}

	@Test void b02_authCode_buildsAndGeneratesAuthUrl() {
		var f = OAuthAuthorizationCodeFlow.create()
			.authorizationEndpoint(URI.create("https://x.example.com/authorize"))
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.redirectUri(URI.create("https://app.example.com/cb"))
			.scope("openid", "profile")
			.build();
		var verifier = new CodeVerifier();
		var challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
		var url = f.buildAuthorizationUrl("state-123", challenge);
		assertNotNull(url);
		var q = url.getQuery();
		assertTrue(q.contains("response_type=code"));
		assertTrue(q.contains("state=state-123"));
		assertTrue(q.contains("code_challenge_method=S256"));
		assertTrue(q.contains("code_challenge="));
	}

	@Test void c01_resourceOwner_requiredFields() {
		assertThrows(IllegalStateException.class, () -> OAuthResourceOwnerFlow.create().build());
		assertThrows(IllegalStateException.class, () -> OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id").clientSecret("secret").username("alice").build());
	}

	@Test void c02_resourceOwner_buildsWithMinimum() {
		var f = OAuthResourceOwnerFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.clientSecret("secret")
			.username("alice")
			.password("p")
			.build();
		assertNotNull(f);
	}

	@Test void d01_refresh_requiredFields() {
		assertThrows(IllegalStateException.class, () -> OAuthRefreshTokenFlow.create().build());
	}

	@Test void d02_refresh_buildsWithMinimum() {
		var f = OAuthRefreshTokenFlow.create()
			.tokenEndpoint(URI.create("https://x.example.com/token"))
			.clientId("id")
			.refreshToken("rt-xyz")
			.build();
		assertNotNull(f);
	}
}
