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

import java.security.*;

/**
 * SPI for verifying an opaque bearer token and producing the associated {@link Principal}.
 *
 * <p>
 * {@link BearerTokenGuard} extracts the token from the {@code Authorization: Bearer <token>} header
 * and delegates verification to a {@code TokenValidator} the host supplies. The validator is the
 * authoritative source of "is this token still good?" &mdash; signature, expiry, audience, issuer,
 * revocation, etc.
 *
 * <p>
 * Implementations:
 * <ul>
 * 	<li><b>JWT</b> &mdash; the optional {@code juneau-rest-server-auth-jwt} sub-module ships
 * 		{@code JwtTokenValidator}, which performs JWKS-backed signature verification with full
 * 		{@code iss} / {@code aud} / {@code exp} / {@code nbf} / algorithm-allowlist enforcement.
 * 	<li><b>Opaque token</b> &mdash; e.g. a 32-byte random token stored in a database; the validator
 * 		looks the token up in the store and returns the associated user record wrapped as a
 * 		{@link Principal}.
 * 	<li><b>Introspection</b> &mdash; e.g. RFC 7662 token introspection over HTTP against the issuing
 * 		IdP; the validator caches successful responses for a short TTL.
 * </ul>
 *
 * <h5 class='topic'>Contract</h5>
 *
 * <p>
 * On success, return a non-null {@link Principal}. On failure, throw {@link AuthenticationException}
 * with a {@code WWW-Authenticate} challenge string set via
 * {@link AuthenticationException#wwwAuthenticate(String)}. Returning <jk>null</jk> is treated as failure
 * by the guard but produces a generic challenge &mdash; throwing is preferred so the failure reason
 * surfaces in the response body.
 *
 * <p>
 * The supplied {@code token} string is exactly what followed {@code "Bearer "} in the {@code Authorization}
 * header &mdash; the guard does not pre-trim or unquote. Validators should treat it as untrusted input
 * (no logging of full tokens, no exception messages echoing the token, etc.).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BearerTokenGuard}
 * 	<li class='jc'>{@link AuthenticationException}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 &mdash; Bearer Token Usage</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface TokenValidator {

	/**
	 * Validates the supplied bearer token and returns the associated {@link Principal}.
	 *
	 * @param token The raw token string from {@code Authorization: Bearer <token>}. Never <jk>null</jk>.
	 * @return The authenticated principal. Must not be <jk>null</jk> on success.
	 * @throws AuthenticationException If the token is invalid, expired, revoked, or otherwise unacceptable.
	 */
	Principal validate(String token) throws AuthenticationException;
}
