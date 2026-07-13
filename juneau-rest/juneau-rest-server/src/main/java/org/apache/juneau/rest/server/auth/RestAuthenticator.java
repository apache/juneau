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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.rest.server.*;

import jakarta.servlet.http.*;

/**
 * Resource-level authentication SPI for {@link org.apache.juneau.rest.server.Rest @Rest} resources.
 *
 * <p>
 * Resolves a request's {@link java.security.Principal} and roles, reusing the same {@link Authenticator}
 * implementations (SAML/OAuth/JWT/API-key) used by the servlet-filter layer.  Same three-state contract as
 * {@link Authenticator}:
 * <ul>
 * 	<li>{@link Optional#empty()} &mdash; does not apply (anonymous passthrough).
 * 	<li>{@link Optional#of(Object) Optional.of(AuthResult)} &mdash; authentication succeeded.
 * 	<li>throw {@link AuthenticationException} &mdash; credentials present but invalid.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Authenticator}
 * 	<li class='jc'>{@link AuthResult}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerAuthenticator">REST Authenticator</a>
 * </ul>
 *
 * @since 10.0.0
 */
public abstract class RestAuthenticator {

	/**
	 * Resolves identity + roles for the request.
	 *
	 * @param req The REST request. Never <jk>null</jk>.
	 * @return The auth result, or {@link Optional#empty()} if this authenticator does not apply.
	 * @throws AuthenticationException If credentials are present but invalid.
	 */
	public abstract Optional<AuthResult> authenticate(RestRequest req) throws AuthenticationException;

	/**
	 * Framework entry point.
	 *
	 * <p>
	 * The default implementation delegates to {@link #authenticate(RestRequest)}.  The framework always passes a
	 * {@link RestRequest}; the separate signature lets adapters operate on a raw {@link HttpServletRequest}.
	 *
	 * @param req The HTTP request (always a {@link RestRequest} in framework wiring).
	 * @return The auth result, or {@link Optional#empty()} if this authenticator does not apply.
	 * @throws AuthenticationException If credentials are present but invalid.
	 */
	Optional<AuthResult> authenticateRaw(HttpServletRequest req) throws AuthenticationException {
		return authenticate((RestRequest) req);
	}

	/**
	 * Adapts any {@link Authenticator} (e.g. an {@link AuthFilter} or {@link AuthFilterChain} bean) into a
	 * {@link RestAuthenticator}.
	 *
	 * @param a The authenticator to adapt. Must not be <jk>null</jk>.
	 * @return A new {@link RestAuthenticator}.
	 */
	public static RestAuthenticator of(Authenticator a) {
		return new RestAuthenticator() {
			@Override public Optional<AuthResult> authenticate(RestRequest req) throws AuthenticationException {
				return a.authenticate(req);
			}
			@Override Optional<AuthResult> authenticateRaw(HttpServletRequest req) throws AuthenticationException {
				return a.authenticate(req);
			}
		};
	}

	/**
	 * Sentinel "unset" type for {@link org.apache.juneau.rest.server.Rest#authenticator() @Rest(authenticator=...)};
	 * never authenticates.
	 */
	public static final class Null extends RestAuthenticator {
		@Override public Optional<AuthResult> authenticate(RestRequest req) {
			return oe();
		}
		@Override Optional<AuthResult> authenticateRaw(HttpServletRequest req) {
			return oe();
		}
	}
}
