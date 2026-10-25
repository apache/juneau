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
package org.apache.juneau.examples.guides.secureanendpoint;

// region validators
import java.net.*;

import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.auth.jwt.*;
import org.apache.juneau.rest.server.auth.oauth.*;

import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;

/**
 * The token validators the endpoint can use.
 */
public final class SecureValidators {

	public static final String ISSUER = "https://issuer.example.com/";
	public static final String AUDIENCE = "https://api.example.com";

	private SecureValidators() {}

	/** Production: verify JWTs against the identity provider's published keys. */
	public static TokenValidator jwt(URI jwksUrl) {
		return JwtTokenValidator.create().jwksUrl(jwksUrl).issuer(ISSUER).audience(AUDIENCE).build();
	}

	/** Same checks, with the keys supplied directly (used by the test). */
	public static TokenValidator jwt(JWKSource<SecurityContext> keys) {
		return JwtTokenValidator.create().jwkSource(keys).issuer(ISSUER).audience(AUDIENCE).build();
	}

	/** Opaque OAuth 2.0 tokens: ask the authorization server whether the token is active (RFC 7662). */
	public static TokenValidator introspection(URI endpoint, String clientId, String clientSecret) {
		return OAuthIntrospectionValidator.create()
			.introspectionEndpoint(endpoint)
			.clientId(clientId)
			.clientSecret(clientSecret)
			.requiredScopes("orders:read")
			.build();
	}

	/** Accept either kind: a JWT has three dot-separated parts, an opaque token doesn't. */
	public static TokenValidator either(TokenValidator jwt, TokenValidator opaque) {
		return token -> token.split("\\.", -1).length == 3 ? jwt.validate(token) : opaque.validate(token);
	}
}
// endregion validators
