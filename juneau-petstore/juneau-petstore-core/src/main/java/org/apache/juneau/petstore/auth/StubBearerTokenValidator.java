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
package org.apache.juneau.petstore.auth;

import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.server.auth.*;

/**
 * Stub {@link TokenValidator} for the petstore demo — recognises an in-memory map of opaque bearer tokens.
 *
 * <p>
 * <b>NOT FOR PRODUCTION USE.</b>  This is a sample/demo validator that maps fixed token strings to fixed user
 * principals.  Real deployments should plug in {@code JwtTokenValidator} from {@code juneau-rest-server-auth-jwt}
 * (signed JWTs against a JWKS), an opaque-token store backed by a database, or RFC 7662 introspection.
 *
 * <p>
 * Default token table:
 * <ul>
 * 	<li>{@code petstore-user} → principal {@code "alice"}
 * 	<li>{@code petstore-admin} → principal {@code "admin"}
 * </ul>
 *
 * <p>
 * Other token strings throw {@link AuthenticationException} carrying a {@code WWW-Authenticate: Bearer realm="petstore"}
 * challenge — the {@code AuthFilterChain} in turn returns {@code 401}.
 */
public class StubBearerTokenValidator implements TokenValidator {

	/** Default in-memory token table. */
	public static final Map<String,String> DEFAULT_TOKENS = Map.of(
		"petstore-user", "alice",
		"petstore-admin", "admin"
	);

	private final Map<String,String> tokenToUser;

	/** Constructor using the {@link #DEFAULT_TOKENS} table. */
	public StubBearerTokenValidator() {
		this(DEFAULT_TOKENS);
	}

	/**
	 * Constructor.
	 *
	 * @param tokenToUser Map of token string to principal name.  Copied defensively.
	 */
	public StubBearerTokenValidator(Map<String,String> tokenToUser) {
		this.tokenToUser = new ConcurrentHashMap<>(Objects.requireNonNull(tokenToUser, "tokenToUser"));
	}

	@Override
	public Principal validate(String token) throws AuthenticationException {
		var name = tokenToUser.get(token);
		if (name == null)
			throw new AuthenticationException("Unknown bearer token").wwwAuthenticate("Bearer realm=\"petstore\"");
		return () -> name;
	}
}
